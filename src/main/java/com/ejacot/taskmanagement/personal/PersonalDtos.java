package com.ejacot.taskmanagement.personal;

import com.ejacot.taskmanagement.hotel.WorkLog;
import com.ejacot.taskmanagement.hotel.WorkType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public final class PersonalDtos {
    private PersonalDtos() {}

    public record AppBootstrap(Me me, MonthSummary currentMonth, DayEntries today, List<WorkTypeView> workTypes, CalendarMonth calendar, ComparisonPreset comparisonPreset) {}
    public record Me(Long userId, String email, boolean emailVerified, String firstName, String lastName, BigDecimal hourlyRate, String currency, int defaultBreakMinutes, String language, boolean onboardingComplete) {}
    public record MonthSummary(BigDecimal hoursWorked, int daysWorked, BigDecimal grossEstimated, long activitiesCount) {}
    public record DayEntries(LocalDate date, List<WorkEntryView> entries, BigDecimal totalHours, BigDecimal totalGross) {}
    public record EntryList(List<WorkEntryView> entries, MonthSummary summary) {}
    public record CalendarMonth(int year, int month, List<CalendarDay> days) {}
    public record CalendarDay(LocalDate date, BigDecimal totalHours, BigDecimal totalGross, List<CalendarEntry> entries) {}
    public record CalendarEntry(Long id, String workTypeCode, String workTypeName, String color, String timeRange, BigDecimal hours, BigDecimal gross) {}
    public record ComparisonPreset(LocalDate fromA, LocalDate toA, LocalDate fromB, LocalDate toB) {}

    public record WorkEntryRequest(
            @NotNull LocalDate date,
            @NotNull Long workTypeId,
            @NotNull LocalTime startTime,
            @NotNull LocalTime endTime,
            @Min(0) @Max(180) Integer breakMinutes,
            @Size(max = 500) String notes
    ) {}

    public record DuplicateRequest(@NotNull LocalDate date) {}

    public record WorkEntryView(
            Long id,
            LocalDate date,
            String workTypeCode,
            String workTypeName,
            String color,
            LocalTime startTime,
            LocalTime endTime,
            int breakMinutes,
            BigDecimal hoursWorked,
            BigDecimal hourlyRate,
            BigDecimal grossAmount,
            String notes
    ) {
        static WorkEntryView from(WorkLog log) {
            return new WorkEntryView(
                    log.getId(),
                    log.getWorkDate(),
                    log.getWorkType().getCode(),
                    log.getWorkType().getName(),
                    log.getWorkType().getColor(),
                    log.getStartTime(),
                    log.getEndTime(),
                    log.getBreakMinutes(),
                    log.getCalculatedHours(),
                    log.getHourlyRateSnapshot(),
                    log.getGrossAmount(),
                    log.getNotes()
            );
        }
    }

    public record WorkTypeRequest(
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Size(max = 30) String code,
            @NotBlank @Pattern(regexp = "^#([A-Fa-f0-9]{6})$") String color,
            boolean active,
            @DecimalMin(value = "0.00") BigDecimal customHourlyRate,
            @Min(0) @Max(180) Integer defaultBreakMinutes
    ) {}

    public record WorkTypeView(Long id, String name, String code, String color, boolean active, BigDecimal customHourlyRate, int defaultBreakMinutes) {
        static WorkTypeView from(WorkType workType) {
            return new WorkTypeView(workType.getId(), workType.getName(), workType.getCode(), workType.getColor(), workType.isActive(), workType.getCustomHourlyRate(), workType.getDefaultBreakMinutes());
        }
    }

    public record PeriodSummary(BigDecimal totalHoursWorked, int totalDaysWorked, BigDecimal totalGrossEstimated, BigDecimal averageHoursPerDay, long activitiesCount, List<BreakdownItem> hoursByType, List<BreakdownItem> daysByType) {}
    public record BreakdownItem(String workTypeCode, String workTypeName, String color, BigDecimal value) {}
    public record MonthlySeries(int year, String metric, List<MonthlyPoint> points) {}
    public record MonthlyPoint(int month, BigDecimal value) {}
    public record Comparison(String metric, BigDecimal totalA, BigDecimal totalB, BigDecimal differenceAbsolute, BigDecimal differencePercent, BigDecimal averagePerMonthA, BigDecimal averagePerMonthB, int betterMonthsInB) {}
}
