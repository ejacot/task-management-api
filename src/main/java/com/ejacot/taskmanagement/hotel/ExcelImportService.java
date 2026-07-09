package com.ejacot.taskmanagement.hotel;

import com.ejacot.taskmanagement.user.*;
import org.apache.poi.ss.usermodel.*;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.regex.*;

@Service
@Transactional
public class ExcelImportService {
    private static final Pattern TIME = Pattern.compile("(\\d{1,2}:\\d{2})\\s*[-–]\\s*(\\d{1,2}:\\d{2})");
    private final UserAccountRepository users; private final WorkTypeRepository types; private final ShiftPlanRepository plans; private final PlanLogService planLogs; private final PasswordEncoder encoder; private final NotificationRepository notifications;
    public ExcelImportService(UserAccountRepository users, WorkTypeRepository types, ShiftPlanRepository plans, PlanLogService planLogs, PasswordEncoder encoder, NotificationRepository notifications) {
        this.users = users; this.types = types; this.plans = plans; this.planLogs = planLogs; this.encoder = encoder; this.notifications = notifications;
    }
    @Transactional(readOnly = true)
    public AdminDtos.ImportPreview preview(String username, MultipartFile file) {
        UserAccount actor = users.findByUsername(username).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (actor.getRole() != UserRole.MANAGER && actor.getRole() != UserRole.EMPLOYER) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            Row header = sheet.getRow(0);
            if (header == null || header.getLastCellNum() < 2) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Excelul trebuie sa aiba zilele pe primul rand.");
            List<String> days = new ArrayList<>();
            List<String> warnings = new ArrayList<>();
            for (int column = 1; column < header.getLastCellNum(); column++) {
                try {
                    days.add(day(header.getCell(column)).toString());
                } catch (ResponseStatusException e) {
                    days.add("coloana " + column);
                    warnings.add("Nu pot citi data din coloana " + (column + 1) + ": " + text(header.getCell(column)));
                }
            }
            List<AdminDtos.ImportPreviewRow> rows = new ArrayList<>();
            int planCells = 0;
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) continue;
                String name = text(row.getCell(0));
                if (name.isBlank()) continue;
                List<String> values = new ArrayList<>();
                for (int column = 1; column <= days.size(); column++) {
                    String value = text(row.getCell(column));
                    values.add(value);
                    if (!value.isBlank()) planCells++;
                }
                rows.add(new AdminDtos.ImportPreviewRow(name, values));
            }
            if (rows.isEmpty()) warnings.add("Nu am gasit angajati in prima foaie Excel.");
            return new AdminDtos.ImportPreview(file.getOriginalFilename(), workbook.getNumberOfSheets(), rows.size(), planCells, days, rows.stream().limit(30).toList(), warnings);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nu am putut citi Excelul: " + e.getMessage());
        }
    }
    public AdminDtos.ImportResult importPlan(String username, MultipartFile file, boolean overwrite) {
        UserAccount actor = users.findByUsername(username).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (actor.getRole() != UserRole.MANAGER && actor.getRole() != UserRole.EMPLOYER) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            Row header = sheet.getRow(0);
            if (header == null || header.getLastCellNum() < 2) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Excelul trebuie să aibă zilele pe primul rând.");
            List<LocalDate> days = new ArrayList<>();
            for (int column = 1; column < header.getLastCellNum(); column++) days.add(day(header.getCell(column)));
            int employeeCount = 0, created = 0, skipped = 0;
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) continue;
                String name = text(row.getCell(0));
                if (name.isBlank()) continue;
                UserAccount employee = findOrCreate(actor, name);
                employeeCount++;
                for (int column = 1; column <= days.size(); column++) {
                    String value = text(row.getCell(column));
                    if (value.isBlank()) continue;
                    LocalDate date = days.get(column - 1);
                    List<ShiftPlan> existing = plans.findAllByEmployeeIdAndWorkDate(employee.getId(), date);
                    if (!existing.isEmpty() && !overwrite) { skipped++; continue; }
                    if (overwrite) existing.forEach(plans::delete);
                    ShiftPlan plan = plans.save(parse(actor, employee, date, value));
                    planLogs.ensureFor(plan);
                    notifications.save(new Notification(employee, "Plan importat din Excel", date + " · " + value, "plan"));
                    created++;
                }
            }
            return new AdminDtos.ImportResult(employeeCount, created, skipped);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nu am putut citi Excelul: " + e.getMessage());
        }
    }
    private ShiftPlan parse(UserAccount actor, UserAccount employee, LocalDate date, String raw) {
        String value = raw.trim();
        ShiftKind kind = switch (value.toUpperCase(Locale.ROOT)) { case "F" -> ShiftKind.FREE; case "U" -> ShiftKind.VACATION; case "K" -> ShiftKind.SICK; default -> ShiftKind.WORK; };
        if (kind != ShiftKind.WORK) return new ShiftPlan(employee, actor.getHotel(), null, date, null, null, value, kind);
        Matcher matcher = TIME.matcher(value);
        LocalTime start = null, end = null;
        if (matcher.find()) { start = LocalTime.parse(fix(matcher.group(1))); end = LocalTime.parse(fix(matcher.group(2))); }
        String code = value.split("\\s+|/")[0].replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
        if (code.isBlank()) code = "TASK";
        String finalCode = code;
        WorkType type = types.findByHotelIdAndCode(actor.getHotel().getId(), finalCode).orElseGet(() -> types.save(new WorkType(actor.getHotel(), finalCode, finalCode, WorkUnit.HOURLY, null, "#667085")));
        return new ShiftPlan(employee, actor.getHotel(), type, date, start, end, value);
    }
    private UserAccount findOrCreate(UserAccount actor, String fullName) {
        String[] parts = fullName.trim().split("\\s+");
        String username = String.join(".", Arrays.stream(parts).map(part -> part.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "")).filter(part -> !part.isBlank()).toList());
        if (username.isBlank()) username = "employee" + System.nanoTime();
        String finalUsername = username;
        return users.findByUsername(finalUsername).orElseGet(() -> {
            UserAccount employee = new UserAccount(finalUsername, encoder.encode("demo1234"), finalUsername + "@example.com", null, UserRole.EMPLOYEE, BigDecimal.ZERO, actor.getHotel());
            employee.configureProfile(parts.length > 1 ? parts[1] : parts[0], parts[0], actor.getHotel().getCity(), 1);
            return users.save(employee);
        });
    }
    private LocalDate day(Cell cell) {
        if (cell == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lipsește o dată în header.");
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) return cell.getLocalDateTimeCellValue().toLocalDate();
        String raw = text(cell).replace('.', '-').replace('/', '-').trim();
        try { return LocalDate.parse(raw); } catch (Exception ignored) {}
        Matcher matcher = Pattern.compile("(\\d{1,2})-(\\d{1,2})-(\\d{2,4})").matcher(raw);
        if (matcher.find()) {
            int year = Integer.parseInt(matcher.group(3));
            if (year < 100) year += 2000;
            return LocalDate.of(year, Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(1)));
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nu pot citi data: " + raw);
    }
    private String text(Cell cell) { return cell == null ? "" : new DataFormatter(Locale.GERMANY).formatCellValue(cell).trim(); }
    private String fix(String value) { String[] parts = value.split(":"); return String.format("%02d:%s", Integer.parseInt(parts[0]), parts[1]); }
}
