package com.ejacot.taskmanagement.hotel;

import com.ejacot.taskmanagement.user.UserAccount;
import jakarta.persistence.*;
import java.time.*;

@Entity
@Table(name = "shift_plans")
public class ShiftPlan {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "employee_id") private UserAccount employee;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "hotel_id") private Hotel hotel;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "work_type_id") private WorkType workType;
    @Column(name = "work_date", nullable = false) private LocalDate workDate;
    @Column(name = "start_time") private LocalTime startTime;
    @Column(name = "end_time") private LocalTime endTime;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private PlanStatus status;
    @Column(length = 500) private String notes;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected ShiftPlan() {}
    public ShiftPlan(UserAccount employee, Hotel hotel, WorkType workType, LocalDate workDate,
                     LocalTime startTime, LocalTime endTime, String notes) {
        this.employee = employee; this.hotel = hotel; this.workType = workType; this.workDate = workDate;
        this.startTime = startTime; this.endTime = endTime; this.notes = notes;
        this.status = PlanStatus.PLANNED; this.updatedAt = Instant.now();
    }
    public Long getId() { return id; }
    public UserAccount getEmployee() { return employee; }
    public Hotel getHotel() { return hotel; }
    public WorkType getWorkType() { return workType; }
    public LocalDate getWorkDate() { return workDate; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public PlanStatus getStatus() { return status; }
    public String getNotes() { return notes; }
}

