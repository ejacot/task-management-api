package com.ejacot.taskmanagement.hotel;

import com.ejacot.taskmanagement.user.UserAccount;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;

public final class HotelDtos {
    private HotelDtos() {}
    public record Bootstrap(Me me, HotelView hotel, List<WorkTypeView> workTypes,
                            List<PlanView> plans, List<LogView> logs, List<NotificationView> notifications, Metrics metrics) {}
    public record Me(Long id, String username,String firstName,String lastName, String email, String phone,String address,Integer steuerClass, UserRole role, BigDecimal hourlyRate) {
        static Me from(UserAccount u) { return new Me(u.getId(), u.getUsername(),u.getFirstName(),u.getLastName(), u.getEmail(), u.getPhone(),u.getAddress(),u.getSteuerClass(), u.getRole(), u.getHourlyRate()); }
    }
    public record HotelView(Long id, String name, String city,BigDecimal normalRoomsPerHour,BigDecimal juniorRoomsPerHour,BigDecimal presidentRoomsPerHour) {}
    public record WorkTypeView(Long id, String code, String name, WorkUnit unit, BigDecimal roomsPerHour, String color,LocalTime defaultStartTime,LocalTime defaultEndTime,int defaultBreakMinutes) {
        static WorkTypeView from(WorkType t) { return new WorkTypeView(t.getId(), t.getCode(), t.getName(), t.getUnit(), t.getRoomsPerHour(), t.getColor(),t.getDefaultStartTime(),t.getDefaultEndTime(),t.getDefaultBreakMinutes()); }
    }
    public record PlanView(Long id, Long employeeId,String employee, LocalDate date, LocalTime startTime, LocalTime endTime,
                           String workType, String color, ShiftKind kind, PlanStatus status, String notes) {
        static PlanView from(ShiftPlan p) { String name=java.util.stream.Stream.of(p.getEmployee().getFirstName(),p.getEmployee().getLastName()).filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.joining(" "));return new PlanView(p.getId(),p.getEmployee().getId(),name.isBlank()?p.getEmployee().getUsername():name,p.getWorkDate(), p.getStartTime(), p.getEndTime(), p.getWorkType()==null?null:p.getWorkType().getName(), p.getWorkType()==null?"#9aa29f":p.getWorkType().getColor(),p.getKind(), p.getStatus(), p.getNotes()); }
    }
    public record LogView(Long id,String employee, LocalDate date, LocalTime startTime, LocalTime endTime, int breakMinutes,
                          String workType, WorkUnit unit, RoomType roomType, Integer quantity,
                          int normalRooms,int juniorRooms,int presidentRooms,boolean hasAttachment,String attachmentName,
                          BigDecimal hours, LogStatus status, String rejectionReason,String notes,Long shiftPlanId) {
        static LogView from(WorkLog l) { return new LogView(l.getId(),l.getEmployee().getUsername(),l.getWorkDate(),l.getStartTime(),l.getEndTime(),l.getBreakMinutes(),l.getWorkType().getName(),l.getWorkType().getUnit(),l.getRoomType(),l.getQuantity(),l.getNormalRooms(),l.getJuniorRooms(),l.getPresidentRooms(),l.hasAttachment(),l.getAttachmentName(),l.getCalculatedHours(),l.getStatus(),l.getRejectionReason(),l.getNotes(),l.getShiftPlan()==null?null:l.getShiftPlan().getId()); }
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
            @Min(0) Integer normalRooms,
            @Min(0) Integer juniorRooms,
            @Min(0) Integer presidentRooms,
            @Size(max=255) String attachmentName,
            String attachmentData,
            @Size(max = 500) String notes) {}
    public record CorrectPlannedLog(@NotNull LocalTime startTime,@NotNull LocalTime endTime,
                                    @Min(0) @Max(180) Integer breakMinutes,
                                    @NotBlank @Size(max=500) String reason) {}
    public record NotificationView(Long id,String title,String message,String link,boolean read,Instant createdAt){static NotificationView from(Notification n){return new NotificationView(n.getId(),n.getTitle(),n.getMessage(),n.getLink(),n.isRead(),n.getCreatedAt());}}
}

