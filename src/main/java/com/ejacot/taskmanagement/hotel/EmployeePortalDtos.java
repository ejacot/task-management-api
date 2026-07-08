package com.ejacot.taskmanagement.hotel;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;

public final class EmployeePortalDtos {
 private EmployeePortalDtos(){}
 public record CreateRequest(@NotNull EmployeeRequestType type,@NotNull LocalDate startDate,@NotNull LocalDate endDate,@Size(max=500) String message){}
 public record ReviewRequest(boolean approved,@Size(max=500) String response){}
 public record RequestView(Long id,String employee,EmployeeRequestType type,LocalDate startDate,LocalDate endDate,String message,EmployeeRequestStatus status,String managerResponse,Instant createdAt){static RequestView from(EmployeeRequest r){return new RequestView(r.getId(),r.getEmployee().getUsername(),r.getType(),r.getStartDate(),r.getEndDate(),r.getMessage(),r.getStatus(),r.getManagerResponse(),r.getCreatedAt());}}
 public record ProfileUpdate(@NotBlank @Size(max=80) String firstName,@NotBlank @Size(max=80) String lastName,@Email @Size(max=150) String email,@Size(max=30) String phone,@Size(max=255) String address,@Min(1) @Max(6) Integer steuerClass){}
 public record PasswordChange(@NotBlank String currentPassword,@NotBlank @Size(min=8,max=72) String newPassword){}
 public record PayrollMonth(int year,int month,BigDecimal regularHours,BigDecimal sundayHours,BigDecimal nightHours,BigDecimal baseGross,BigDecimal premiums,BigDecimal gross,BigDecimal estimatedNet){}
 public record PayrollYear(int year,List<PayrollMonth> months,BigDecimal totalHours,BigDecimal totalGross,BigDecimal estimatedNet){}
}
