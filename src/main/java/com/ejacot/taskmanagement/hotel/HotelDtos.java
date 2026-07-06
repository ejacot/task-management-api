package com.ejacot.taskmanagement.hotel;

import com.ejacot.taskmanagement.user.UserAccount;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;

public final class HotelDtos {
    private HotelDtos() {}
    public record Bootstrap(Me me, HotelView hotel, List<WorkTypeView> workTypes,
                            List<PlanView> plans, List<LogView> logs, Metrics metrics) {}
    public record Me(Long id, String username, String email, String phone, UserRole role, BigDecimal hourlyRate) {
        static Me from(UserAccount u) { return new Me(u.getId(), u.getUsername(), u.getEmail(), u.getPhone(), u.getRole(), u.getHourlyRate()); }
    }
    public record HotelView(Long id, String name, String city) {}
    public record WorkTypeView(Long id, String code, String name, WorkUnit unit, BigDecimal roomsPerHour, String color) {
        static WorkTypeView from(WorkType t) { return new WorkTypeView(t.getId(), t.getCode(), t.getName(), t.getUnit(), t.getRoomsPerHour(), t.getColor()); }
    }
    public record PlanView(Long id, LocalDate date, LocalTime startTime, LocalTime endTime,
                           String workType, String color, PlanStatus status, String notes) {
        static PlanView from(ShiftPlan p) { return new PlanView(p.getId(), p.getWorkDate(), p.getStartTime(), p.getEndTime(), p.getWorkType().getName(), p.getWorkType().getColor(), p.getStatus(), p.getNotes()); }
    }
    public record LogView(Long id, LocalDate date, LocalTime startTime, LocalTime endTime, int breakMinutes,
                          String workType, WorkUnit unit, RoomType roomType, Integer quantity,
                          BigDecimal hours, LogStatus status, String notes) {
        static LogView from(WorkLog l) { return new LogView(l.getId(), l.getWorkDate(), l.getStartTime(), l.getEndTime(), l.getBreakMinutes(), l.getWorkType().getName(), l.getWorkType().getUnit(), l.getRoomType(), l.getQuantity(), l.getCalculatedHours(), l.getStatus(), l.getNotes()); }
    }
    public record Metrics(BigDecimal monthHours, BigDecimal gross, BigDecimal estimatedNet, int rooms) {}
    public record CreateLog(
            @NotNull Long workTypeId,
            @NotNull LocalDate workDate,
            LocalTime startTime,
            LocalTime endTime,
            @Min(0) @Max(180) Integer breakMinutes,
            RoomType roomType,
            @Min(1) Integer quantity,
            @Size(max = 500) String notes) {}
}

