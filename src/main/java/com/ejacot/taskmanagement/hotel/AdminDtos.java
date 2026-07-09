package com.ejacot.taskmanagement.hotel;

import com.ejacot.taskmanagement.user.UserAccount;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;

public final class AdminDtos {
    private AdminDtos() {}
    public record EmployeeAdminView(Long id,String username,String firstName,String lastName,String displayName,String email,String phone,String address,Integer steuerClass,UserRole role,BigDecimal hourlyRate,boolean active,String teamName,Long hotelId,String hotel,Instant createdAt,Instant deactivatedAt,String invitationLink) {
        static EmployeeAdminView from(UserAccount u,String invitationLink){String display=java.util.stream.Stream.of(u.getFirstName(),u.getLastName()).filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.joining(" "));return new EmployeeAdminView(u.getId(),u.getUsername(),u.getFirstName(),u.getLastName(),display.isBlank()?u.getUsername():display,u.getEmail(),u.getPhone(),u.getAddress(),u.getSteuerClass(),u.getRole(),u.getHourlyRate(),u.isActive(),u.getTeamName(),u.getHotel()==null?null:u.getHotel().getId(),u.getHotel()==null?null:u.getHotel().getName(),u.getCreatedAt(),u.getDeactivatedAt(),invitationLink);}
    }
    public record EmployeeUpsert(@NotBlank @Size(min=3,max=50) String username,@Size(max=80) String firstName,@Size(max=80) String lastName,@Email @Size(max=150) String email,@Size(max=30) String phone,@Size(max=255) String address,@Min(1) @Max(6) Integer steuerClass,@NotNull UserRole role,@NotNull @DecimalMin("0.00") BigDecimal hourlyRate,@Size(max=120) String teamName,String temporaryPassword) {}
    public record EmployeeList(List<EmployeeAdminView> employees) {}
    public record InvitationView(Long employeeId,String invitationLink,Instant expiresAt) {}
    public record ActiveRequest(boolean active) {}
    public record HotelSettings(@NotBlank @Size(max=120) String name,@NotBlank @Size(max=120) String city,@NotNull @DecimalMin("0.01") BigDecimal normalRoomsPerHour,@NotNull @DecimalMin("0.01") BigDecimal juniorRoomsPerHour,@NotNull @DecimalMin("0.01") BigDecimal presidentRoomsPerHour,@NotNull @DecimalMin("0.00") BigDecimal sundayPremiumPercent,@NotNull @DecimalMin("0.00") BigDecimal nightPremiumPercent,@NotNull @DecimalMin("0.00") BigDecimal holidayPremiumPercent,@Min(0) @Max(180) int defaultBreakMinutes) {
        static HotelSettings from(Hotel h){return new HotelSettings(h.getName(),h.getCity(),h.getNormalRoomsPerHour(),h.getJuniorRoomsPerHour(),h.getPresidentRoomsPerHour(),h.getSundayPremiumPercent(),h.getNightPremiumPercent(),h.getHolidayPremiumPercent(),h.getDefaultBreakMinutes());}
    }
    public record WorkTypeUpdate(@NotBlank @Size(max=30) String code,@NotBlank @Size(max=100) String name,@NotNull WorkUnit unit,@DecimalMin("0.01") BigDecimal roomsPerHour,@Pattern(regexp="^#[0-9A-Fa-f]{6}$") String color,boolean active,LocalTime defaultStartTime,LocalTime defaultEndTime,@Min(0) @Max(180) int defaultBreakMinutes) {}
    public record MonthlyClose(int year,int month,BigDecimal hours,BigDecimal gross,BigDecimal estimatedNet,int approvedLogs) {}
    public record ImportResult(int employees,int plansCreated,int plansSkipped) {}
}
