package com.ejacot.taskmanagement.hotel;

import com.ejacot.taskmanagement.user.UserAccount;
import jakarta.persistence.*;
import java.math.*;
import java.time.*;

@Entity
@Table(name = "work_logs")
public class WorkLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "employee_id") private UserAccount employee;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "hotel_id") private Hotel hotel;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "work_type_id") private WorkType workType;
    @Column(name = "work_date", nullable = false) private LocalDate workDate;
    @Column(name = "start_time") private LocalTime startTime;
    @Column(name = "end_time") private LocalTime endTime;
    @Column(name = "break_minutes", nullable = false) private int breakMinutes;
    @Enumerated(EnumType.STRING) @Column(name = "room_type", length = 30) private RoomType roomType;
    private Integer quantity;
    @Column(name = "calculated_hours", nullable = false, precision = 8, scale = 2) private BigDecimal calculatedHours;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private LogStatus status;
    @Column(length = 500) private String notes;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected WorkLog() {}
    public WorkLog(UserAccount employee, Hotel hotel, WorkType workType, LocalDate workDate,
                   LocalTime startTime, LocalTime endTime, int breakMinutes, RoomType roomType,
                   Integer quantity, BigDecimal calculatedHours, String notes) {
        this.employee = employee; this.hotel = hotel; this.workType = workType; this.workDate = workDate;
        this.startTime = startTime; this.endTime = endTime; this.breakMinutes = breakMinutes;
        this.roomType = roomType; this.quantity = quantity; this.calculatedHours = calculatedHours;
        this.notes = notes; this.status = LogStatus.DRAFT;
        this.createdAt = Instant.now(); this.updatedAt = this.createdAt;
    }
    public void submit() { this.status = LogStatus.SUBMITTED; this.updatedAt = Instant.now(); }
    public Long getId() { return id; }
    public UserAccount getEmployee() { return employee; }
    public WorkType getWorkType() { return workType; }
    public LocalDate getWorkDate() { return workDate; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public int getBreakMinutes() { return breakMinutes; }
    public RoomType getRoomType() { return roomType; }
    public Integer getQuantity() { return quantity; }
    public BigDecimal getCalculatedHours() { return calculatedHours; }
    public LogStatus getStatus() { return status; }
    public String getNotes() { return notes; }
}

