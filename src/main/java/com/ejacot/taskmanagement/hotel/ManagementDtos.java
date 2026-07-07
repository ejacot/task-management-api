package com.ejacot.taskmanagement.hotel;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;

public final class ManagementDtos{
 private ManagementDtos(){}
 public record EmployeeView(Long id,String username,String displayName,String role){ }
 public record Overview(List<EmployeeView> employees,List<HotelDtos.PlanView> plans,List<HotelDtos.LogView> pendingLogs){}
 public record PlanRequest(@NotEmpty List<Long> employeeIds,Long workTypeId,@NotNull LocalDate date,LocalTime startTime,LocalTime endTime,@NotNull ShiftKind kind,@Size(max=500) String notes){}
 public record UpdatePlanRequest(Long workTypeId,@NotNull LocalDate date,LocalTime startTime,LocalTime endTime,@NotNull ShiftKind kind,@Size(max=500) String notes){}
 public record ReviewRequest(boolean approved,@Size(max=500) String reason){}
 public record WorkTypeRequest(@NotBlank @Size(max=30) String code,@NotBlank @Size(max=100) String name,@NotNull WorkUnit unit,@DecimalMin("0.01") BigDecimal roomsPerHour,@Pattern(regexp="^#[0-9A-Fa-f]{6}$") String color){}
 public record PayRateRequest(@NotNull Long employeeId,@NotNull @DecimalMin("0.01") BigDecimal hourlyRate,@NotNull LocalDate effectiveFrom){}
 public record AttachmentView(String name,String data){}
}
