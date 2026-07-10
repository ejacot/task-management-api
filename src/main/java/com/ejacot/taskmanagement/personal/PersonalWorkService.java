package com.ejacot.taskmanagement.personal;

import com.ejacot.taskmanagement.hotel.WorkLog;
import com.ejacot.taskmanagement.hotel.WorkLogRepository;
import com.ejacot.taskmanagement.hotel.WorkType;
import com.ejacot.taskmanagement.hotel.WorkTypeRepository;
import com.ejacot.taskmanagement.user.UserAccount;
import com.ejacot.taskmanagement.user.UserAccountRepository;
import com.ejacot.taskmanagement.user.UserProfile;
import com.ejacot.taskmanagement.user.UserProfileRepository;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class PersonalWorkService {
    private final UserAccountRepository users;
    private final UserProfileRepository profiles;
    private final WorkTypeRepository workTypes;
    private final WorkLogRepository logs;

    public PersonalWorkService(UserAccountRepository users, UserProfileRepository profiles, WorkTypeRepository workTypes, WorkLogRepository logs) {
        this.users = users;
        this.profiles = profiles;
        this.workTypes = workTypes;
        this.logs = logs;
    }

    @Transactional(readOnly = true)
    public PersonalDtos.AppBootstrap bootstrap(String username, LocalDate today) {
        UserAccount user = user(username);
        UserProfile profile = profile(user);
        YearMonth month = YearMonth.from(today);
        List<WorkLog> monthLogs = listEntries(user, month.atDay(1), month.atEndOfMonth(), null);
        return new PersonalDtos.AppBootstrap(
                me(user, profile),
                summaryFrom(monthLogs),
                dayEntries(username, today),
                workTypes(username),
                calendar(username, month.getYear(), month.getMonthValue()),
                defaultComparisonPreset(today)
        );
    }

    @Transactional(readOnly = true)
    public PersonalDtos.DayEntries dayEntries(String username, LocalDate date) {
        UserAccount user = user(username);
        List<WorkLog> dayLogs = listEntries(user, date, date, null);
        return new PersonalDtos.DayEntries(date, dayLogs.stream().map(PersonalDtos.WorkEntryView::from).toList(), hours(dayLogs), gross(dayLogs));
    }

    @Transactional(readOnly = true)
    public PersonalDtos.EntryList entries(String username, Integer year, Integer month, LocalDate from, LocalDate to, Long workTypeId) {
        UserAccount user = user(username);
        DateRange range = resolveRange(year, month, from, to);
        List<WorkLog> found = listEntries(user, range.from(), range.to(), workTypeId);
        return new PersonalDtos.EntryList(found.stream().map(PersonalDtos.WorkEntryView::from).toList(), summaryFrom(found));
    }

    @Transactional(readOnly = true)
    public PersonalDtos.CalendarMonth calendar(String username, int year, int month) {
        UserAccount user = user(username);
        YearMonth yearMonth = YearMonth.of(year, month);
        List<WorkLog> found = listEntries(user, yearMonth.atDay(1), yearMonth.atEndOfMonth(), null);
        Map<LocalDate, List<WorkLog>> byDate = found.stream().collect(Collectors.groupingBy(WorkLog::getWorkDate, TreeMap::new, Collectors.toList()));
        List<PersonalDtos.CalendarDay> days = new ArrayList<>();
        for (LocalDate date = yearMonth.atDay(1); !date.isAfter(yearMonth.atEndOfMonth()); date = date.plusDays(1)) {
            List<WorkLog> items = byDate.getOrDefault(date, List.of());
            days.add(new PersonalDtos.CalendarDay(
                    date,
                    hours(items),
                    gross(items),
                    items.stream().map(log -> new PersonalDtos.CalendarEntry(
                            log.getId(),
                            log.getWorkType().getCode(),
                            log.getWorkType().getName(),
                            log.getWorkType().getColor(),
                            formatRange(log.getStartTime(), log.getEndTime()),
                            log.getCalculatedHours(),
                            log.getGrossAmount()
                    )).toList()
            ));
        }
        return new PersonalDtos.CalendarMonth(year, month, days);
    }

    public PersonalDtos.WorkEntryView createEntry(String username, PersonalDtos.WorkEntryRequest request) {
        UserAccount user = user(username);
        UserProfile profile = profile(user);
        WorkType workType = ownedWorkType(user, request.workTypeId());
        Calculation calculation = calculate(request.startTime(), request.endTime(), request.breakMinutes() == null ? profile.getDefaultBreakMinutes() : request.breakMinutes(), effectiveRate(profile, workType));
        WorkLog log = new WorkLog(user, null, workType, request.date(), request.startTime(), request.endTime(), calculation.breakMinutes(), null, null, calculation.hoursWorked(), trimToNull(request.notes()));
        log.record(calculation.hourlyRate(), calculation.grossAmount(), trimToNull(request.notes()));
        return PersonalDtos.WorkEntryView.from(logs.save(log));
    }

    public PersonalDtos.WorkEntryView updateEntry(String username, Long id, PersonalDtos.WorkEntryRequest request) {
        UserAccount user = user(username);
        UserProfile profile = profile(user);
        WorkLog log = logs.findByIdAndEmployeeUsername(id, username).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Activitatea nu există"));
        WorkType workType = ownedWorkType(user, request.workTypeId());
        Calculation calculation = calculate(request.startTime(), request.endTime(), request.breakMinutes() == null ? profile.getDefaultBreakMinutes() : request.breakMinutes(), effectiveRate(profile, workType));
        log.updateEntry(workType, request.date(), request.startTime(), request.endTime(), calculation.breakMinutes(), calculation.hoursWorked(), calculation.hourlyRate(), calculation.grossAmount(), trimToNull(request.notes()));
        return PersonalDtos.WorkEntryView.from(log);
    }

    public void deleteEntry(String username, Long id) {
        WorkLog log = logs.findByIdAndEmployeeUsername(id, username).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Activitatea nu există"));
        logs.delete(log);
    }

    public PersonalDtos.WorkEntryView duplicateEntry(String username, Long id, LocalDate date) {
        WorkLog source = logs.findByIdAndEmployeeUsername(id, username).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Activitatea nu există"));
        PersonalDtos.WorkEntryRequest request = new PersonalDtos.WorkEntryRequest(date, source.getWorkType().getId(), source.getStartTime(), source.getEndTime(), source.getBreakMinutes(), source.getNotes());
        return createEntry(username, request);
    }

    @Transactional(readOnly = true)
    public List<PersonalDtos.WorkTypeView> workTypes(String username) {
        UserAccount user = user(username);
        return workTypes.findAllByOwnerIdAndActiveTrueOrderByName(user.getId()).stream().map(PersonalDtos.WorkTypeView::from).toList();
    }

    public PersonalDtos.WorkTypeView createWorkType(String username, PersonalDtos.WorkTypeRequest request) {
        UserAccount user = user(username);
        WorkType workType = new WorkType(user, request.code().trim().toUpperCase(), request.name().trim(), com.ejacot.taskmanagement.hotel.WorkUnit.HOURLY, null, request.color().trim());
        workType.updatePersonal(request.code().trim().toUpperCase(), request.name().trim(), request.color().trim(), request.active(), request.customHourlyRate(), request.defaultBreakMinutes() == null ? 30 : request.defaultBreakMinutes());
        return PersonalDtos.WorkTypeView.from(workTypes.save(workType));
    }

    public PersonalDtos.WorkTypeView updateWorkType(String username, Long id, PersonalDtos.WorkTypeRequest request) {
        UserAccount user = user(username);
        WorkType workType = ownedWorkType(user, id);
        workType.updatePersonal(request.code().trim().toUpperCase(), request.name().trim(), request.color().trim(), request.active(), request.customHourlyRate(), request.defaultBreakMinutes() == null ? 30 : request.defaultBreakMinutes());
        return PersonalDtos.WorkTypeView.from(workType);
    }

    public void deactivateWorkType(String username, Long id) {
        UserAccount user = user(username);
        WorkType workType = ownedWorkType(user, id);
        workType.updatePersonal(workType.getCode(), workType.getName(), workType.getColor(), false, workType.getCustomHourlyRate(), workType.getDefaultBreakMinutes());
    }

    @Transactional(readOnly = true)
    public PersonalDtos.PeriodSummary summary(String username, Integer year, Integer month, LocalDate from, LocalDate to, Long workTypeId) {
        UserAccount user = user(username);
        DateRange range = resolveRange(year, month, from, to);
        List<WorkLog> found = listEntries(user, range.from(), range.to(), workTypeId);
        BigDecimal totalHours = hours(found);
        int totalDays = (int) found.stream().map(WorkLog::getWorkDate).distinct().count();
        BigDecimal totalGross = gross(found);
        BigDecimal average = totalDays == 0 ? BigDecimal.ZERO : totalHours.divide(BigDecimal.valueOf(totalDays), 2, RoundingMode.HALF_UP);
        return new PersonalDtos.PeriodSummary(totalHours, totalDays, totalGross, average, found.size(), breakdown(found, true), breakdown(found, false));
    }

    @Transactional(readOnly = true)
    public PersonalDtos.MonthlySeries monthly(String username, int year, String metric, Long workTypeId) {
        UserAccount user = user(username);
        List<PersonalDtos.MonthlyPoint> points = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            YearMonth ym = YearMonth.of(year, month);
            List<WorkLog> found = listEntries(user, ym.atDay(1), ym.atEndOfMonth(), workTypeId);
            points.add(new PersonalDtos.MonthlyPoint(month, metricValue(found, metric)));
        }
        return new PersonalDtos.MonthlySeries(year, metric, points);
    }

    @Transactional(readOnly = true)
    public PersonalDtos.Comparison compare(String username, LocalDate fromA, LocalDate toA, LocalDate fromB, LocalDate toB, String metric, Long workTypeId) {
        UserAccount user = user(username);
        List<WorkLog> a = listEntries(user, fromA, toA, workTypeId);
        List<WorkLog> b = listEntries(user, fromB, toB, workTypeId);
        BigDecimal totalA = metricValue(a, metric);
        BigDecimal totalB = metricValue(b, metric);
        BigDecimal diff = totalB.subtract(totalA).setScale(2, RoundingMode.HALF_UP);
        BigDecimal percent = totalA.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO : diff.multiply(BigDecimal.valueOf(100)).divide(totalA, 2, RoundingMode.HALF_UP);
        int monthsA = monthSpan(fromA, toA);
        int monthsB = monthSpan(fromB, toB);
        BigDecimal avgA = monthsA == 0 ? BigDecimal.ZERO : totalA.divide(BigDecimal.valueOf(monthsA), 2, RoundingMode.HALF_UP);
        BigDecimal avgB = monthsB == 0 ? BigDecimal.ZERO : totalB.divide(BigDecimal.valueOf(monthsB), 2, RoundingMode.HALF_UP);
        int betterMonths = compareMonths(user, fromA, toA, fromB, toB, metric, workTypeId);
        return new PersonalDtos.Comparison(metric, totalA, totalB, diff, percent, avgA, avgB, betterMonths);
    }

    @Transactional(readOnly = true)
    public byte[] exportCsv(String username, Integer year, Integer month, LocalDate from, LocalDate to) {
        UserAccount user = user(username);
        DateRange range = resolveRange(year, month, from, to);
        List<WorkLog> found = listEntries(user, range.from(), range.to(), null);
        StringBuilder csv = new StringBuilder("date,workType,start,end,breakMinutes,hours,hourlyRate,gross,notes\n");
        for (WorkLog log : found) {
            csv.append(log.getWorkDate()).append(',')
                    .append(escape(log.getWorkType().getName())).append(',')
                    .append(log.getStartTime()).append(',')
                    .append(log.getEndTime()).append(',')
                    .append(log.getBreakMinutes()).append(',')
                    .append(log.getCalculatedHours()).append(',')
                    .append(log.getHourlyRateSnapshot()).append(',')
                    .append(log.getGrossAmount()).append(',')
                    .append(escape(log.getNotes())).append('\n');
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Transactional(readOnly = true)
    public byte[] exportExcel(String username, Integer year, Integer month, LocalDate from, LocalDate to) {
        UserAccount user = user(username);
        DateRange range = resolveRange(year, month, from, to);
        List<WorkLog> found = listEntries(user, range.from(), range.to(), null);
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Work entries");
            var header = sheet.createRow(0);
            String[] columns = {"Date", "Work type", "Start", "End", "Break", "Hours", "Hourly rate", "Gross", "Notes"};
            for (int i = 0; i < columns.length; i++) header.createCell(i).setCellValue(columns[i]);
            int rowIndex = 1;
            for (WorkLog log : found) {
                var row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(log.getWorkDate().toString());
                row.createCell(1).setCellValue(log.getWorkType().getName());
                row.createCell(2).setCellValue(log.getStartTime().toString());
                row.createCell(3).setCellValue(log.getEndTime().toString());
                row.createCell(4).setCellValue(log.getBreakMinutes());
                row.createCell(5).setCellValue(log.getCalculatedHours().doubleValue());
                row.createCell(6).setCellValue(log.getHourlyRateSnapshot() == null ? 0 : log.getHourlyRateSnapshot().doubleValue());
                row.createCell(7).setCellValue(log.getGrossAmount() == null ? 0 : log.getGrossAmount().doubleValue());
                row.createCell(8).setCellValue(log.getNotes() == null ? "" : log.getNotes());
            }
            workbook.write(output);
            return output.toByteArray();
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Exportul Excel a eșuat");
        }
    }

    private PersonalDtos.Me me(UserAccount user, UserProfile profile) {
        return new PersonalDtos.Me(user.getId(), user.getEmail(), user.isEmailVerified(), profile.getFirstName(), profile.getLastName(), profile.getDefaultHourlyRate(), profile.getCurrency(), profile.getDefaultBreakMinutes(), profile.getLanguage(), profile.isCompleted());
    }

    private UserAccount user(String username) {
        return users.findByUsername(username).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    private UserProfile profile(UserAccount user) {
        return profiles.findByUserId(user.getId()).orElseGet(() -> profiles.save(new UserProfile(user)));
    }

    private WorkType ownedWorkType(UserAccount user, Long id) {
        return workTypes.findById(id)
                .filter(type -> type.getOwner() != null && type.getOwner().getId().equals(user.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tipul de activitate nu există"));
    }

    private DateRange resolveRange(Integer year, Integer month, LocalDate from, LocalDate to) {
        if (from != null || to != null) {
            if (from == null || to == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Perioada personalizată trebuie completă");
            return new DateRange(from, to);
        }
        if (year != null && month != null) {
            YearMonth ym = YearMonth.of(year, month);
            return new DateRange(ym.atDay(1), ym.atEndOfMonth());
        }
        if (year != null) {
            return new DateRange(LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31));
        }
        LocalDate today = LocalDate.now();
        YearMonth ym = YearMonth.from(today);
        return new DateRange(ym.atDay(1), ym.atEndOfMonth());
    }

    private List<WorkLog> listEntries(UserAccount user, LocalDate from, LocalDate to, Long workTypeId) {
        return logs.findAllByEmployeeUsernameAndWorkDateBetweenOrderByWorkDateDesc(user.getUsername(), from, to).stream()
                .filter(log -> workTypeId == null || Objects.equals(log.getWorkType().getId(), workTypeId))
                .filter(log -> log.getWorkType().getOwner() != null && log.getWorkType().getOwner().getId().equals(user.getId()))
                .sorted(Comparator.comparing(WorkLog::getWorkDate).reversed().thenComparing(WorkLog::getStartTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private Calculation calculate(LocalTime start, LocalTime end, int breakMinutes, BigDecimal hourlyRate) {
        long minutes = ChronoUnit.MINUTES.between(start, end);
        if (minutes <= 0) minutes += 24 * 60;
        long paidMinutes = minutes - breakMinutes;
        if (paidMinutes <= 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pauza este mai mare decât durata activității");
        BigDecimal hours = BigDecimal.valueOf(paidMinutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        BigDecimal gross = hours.multiply(hourlyRate).setScale(2, RoundingMode.HALF_UP);
        return new Calculation(breakMinutes, hours, hourlyRate, gross);
    }

    private BigDecimal effectiveRate(UserProfile profile, WorkType workType) {
        return Optional.ofNullable(workType.getCustomHourlyRate()).orElse(profile.getDefaultHourlyRate()).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal hours(List<WorkLog> logs) {
        return logs.stream().map(WorkLog::getCalculatedHours).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal gross(List<WorkLog> logs) {
        return logs.stream().map(log -> log.getGrossAmount() == null ? BigDecimal.ZERO : log.getGrossAmount()).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
    }

    private PersonalDtos.MonthSummary summaryFrom(List<WorkLog> logs) {
        return new PersonalDtos.MonthSummary(hours(logs), (int) logs.stream().map(WorkLog::getWorkDate).distinct().count(), gross(logs), logs.size());
    }

    private List<PersonalDtos.BreakdownItem> breakdown(List<WorkLog> logs, boolean hours) {
        Map<WorkType, BigDecimal> values = new LinkedHashMap<>();
        for (WorkLog log : logs) {
            BigDecimal increment = hours ? log.getCalculatedHours() : BigDecimal.ONE;
            values.merge(log.getWorkType(), increment, BigDecimal::add);
        }
        return values.entrySet().stream()
                .map(entry -> new PersonalDtos.BreakdownItem(entry.getKey().getCode(), entry.getKey().getName(), entry.getKey().getColor(), entry.getValue().setScale(2, RoundingMode.HALF_UP)))
                .toList();
    }

    private BigDecimal metricValue(List<WorkLog> logs, String metric) {
        return switch (metric) {
            case "days" -> BigDecimal.valueOf(logs.stream().map(WorkLog::getWorkDate).distinct().count());
            case "gross" -> gross(logs);
            default -> hours(logs);
        };
    }

    private PersonalDtos.ComparisonPreset defaultComparisonPreset(LocalDate today) {
        YearMonth current = YearMonth.from(today);
        YearMonth previousYear = current.minusYears(1);
        return new PersonalDtos.ComparisonPreset(previousYear.atDay(1), previousYear.atEndOfMonth(), current.atDay(1), current.atEndOfMonth());
    }

    private int monthSpan(LocalDate from, LocalDate to) {
        YearMonth start = YearMonth.from(from);
        YearMonth end = YearMonth.from(to);
        return (int) ChronoUnit.MONTHS.between(start, end) + 1;
    }

    private int compareMonths(UserAccount user, LocalDate fromA, LocalDate toA, LocalDate fromB, LocalDate toB, String metric, Long workTypeId) {
        int span = Math.min(monthSpan(fromA, toA), monthSpan(fromB, toB));
        int better = 0;
        YearMonth startA = YearMonth.from(fromA);
        YearMonth startB = YearMonth.from(fromB);
        for (int i = 0; i < span; i++) {
            YearMonth monthA = startA.plusMonths(i);
            YearMonth monthB = startB.plusMonths(i);
            BigDecimal valueA = metricValue(listEntries(user, monthA.atDay(1), monthA.atEndOfMonth(), workTypeId), metric);
            BigDecimal valueB = metricValue(listEntries(user, monthB.atDay(1), monthB.atEndOfMonth(), workTypeId), metric);
            if (valueB.compareTo(valueA) > 0) better++;
        }
        return better;
    }

    private String escape(String value) {
        if (value == null) return "";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private String formatRange(LocalTime start, LocalTime end) {
        return start.toString() + "–" + end.toString();
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record Calculation(int breakMinutes, BigDecimal hoursWorked, BigDecimal hourlyRate, BigDecimal grossAmount) {}
    private record DateRange(LocalDate from, LocalDate to) {}
}
