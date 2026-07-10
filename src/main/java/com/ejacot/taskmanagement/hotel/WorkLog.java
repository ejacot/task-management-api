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
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "hotel_id") private Hotel hotel;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "work_type_id") private WorkType workType;
    @OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "shift_plan_id") private ShiftPlan shiftPlan;
    @Column(name = "work_date", nullable = false) private LocalDate workDate;
    @Column(name = "start_time") private LocalTime startTime;
    @Column(name = "end_time") private LocalTime endTime;
    @Column(name = "break_minutes", nullable = false) private int breakMinutes;
    @Enumerated(EnumType.STRING) @Column(name = "room_type", length = 30) private RoomType roomType;
    private Integer quantity;
    @Column(name="normal_rooms",nullable=false) private int normalRooms;
    @Column(name="junior_rooms",nullable=false) private int juniorRooms;
    @Column(name="president_rooms",nullable=false) private int presidentRooms;
    @Column(name="attachment_name",length=255) private String attachmentName;
    @Column(name="attachment_data",columnDefinition="TEXT") private String attachmentData;
    @Column(name="rejection_reason",length=500) private String rejectionReason;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="reviewed_by") private UserAccount reviewedBy;
    @Column(name="reviewed_at") private Instant reviewedAt;
    @Column(name = "calculated_hours", nullable = false, precision = 8, scale = 2) private BigDecimal calculatedHours;
    @Column(name = "hourly_rate_snapshot", precision = 10, scale = 2) private BigDecimal hourlyRateSnapshot;
    @Column(name = "gross_amount", precision = 10, scale = 2) private BigDecimal grossAmount;
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
    public WorkLog(ShiftPlan plan, BigDecimal calculatedHours, int breakMinutes) {
        this(plan.getEmployee(), plan.getHotel(), plan.getWorkType(), plan.getWorkDate(), plan.getStartTime(),
                plan.getEndTime(), breakMinutes, null, null, calculatedHours, "Generat automat din plan");
        this.shiftPlan = plan;
        this.status = LogStatus.APPROVED;
    }
    public void submit() { this.status = LogStatus.SUBMITTED; this.updatedAt = Instant.now(); }
    public void setRoomBreakdown(int normal,int junior,int president,String attachmentName,String attachmentData){this.normalRooms=normal;this.juniorRooms=junior;this.presidentRooms=president;this.quantity=normal+junior+president;this.attachmentName=attachmentName;this.attachmentData=attachmentData;}
    public void review(UserAccount reviewer,boolean approved,String reason){this.status=approved?LogStatus.APPROVED:LogStatus.REJECTED;this.reviewedBy=reviewer;this.reviewedAt=Instant.now();this.rejectionReason=approved?null:reason;this.updatedAt=Instant.now();}
    public void syncFromPlan(BigDecimal hours,int breakMinutes){if(shiftPlan==null)return;this.workType=shiftPlan.getWorkType();this.workDate=shiftPlan.getWorkDate();this.startTime=shiftPlan.getStartTime();this.endTime=shiftPlan.getEndTime();this.breakMinutes=breakMinutes;this.calculatedHours=hours;this.status=LogStatus.APPROVED;this.notes="Generat automat din plan";this.rejectionReason=null;this.updatedAt=Instant.now();}
    public void requestCorrection(LocalTime start,LocalTime end,int breakMinutes,BigDecimal hours,String reason){if(shiftPlan==null)throw new IllegalStateException("Pontajul nu este legat de plan");this.startTime=start;this.endTime=end;this.breakMinutes=breakMinutes;this.calculatedHours=hours;this.notes=reason;this.status=LogStatus.SUBMITTED;this.rejectionReason=null;this.updatedAt=Instant.now();}
    public void rejectCorrection(UserAccount reviewer,String reason,BigDecimal plannedHours,int plannedBreak){syncFromPlan(plannedHours,plannedBreak);this.reviewedBy=reviewer;this.reviewedAt=Instant.now();this.rejectionReason=reason;this.updatedAt=Instant.now();}
    public void record(BigDecimal hourlyRateSnapshot, BigDecimal grossAmount, String notes){
        this.hourlyRateSnapshot=hourlyRateSnapshot;
        this.grossAmount=grossAmount;
        this.notes=notes;
        this.status=LogStatus.APPROVED;
        this.updatedAt=Instant.now();
        this.rejectionReason=null;
    }
    public void updateEntry(WorkType workType, LocalDate workDate, LocalTime startTime, LocalTime endTime, int breakMinutes, BigDecimal calculatedHours, BigDecimal hourlyRateSnapshot, BigDecimal grossAmount, String notes){
        this.workType=workType;
        this.workDate=workDate;
        this.startTime=startTime;
        this.endTime=endTime;
        this.breakMinutes=breakMinutes;
        this.calculatedHours=calculatedHours;
        this.hourlyRateSnapshot=hourlyRateSnapshot;
        this.grossAmount=grossAmount;
        this.notes=notes;
        this.status=LogStatus.APPROVED;
        this.updatedAt=Instant.now();
        this.rejectionReason=null;
    }
    public Long getId() { return id; }
    public UserAccount getEmployee() { return employee; }
    public ShiftPlan getShiftPlan(){return shiftPlan;}
    public Hotel getHotel() { return hotel; }
    public WorkType getWorkType() { return workType; }
    public LocalDate getWorkDate() { return workDate; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public int getBreakMinutes() { return breakMinutes; }
    public RoomType getRoomType() { return roomType; }
    public Integer getQuantity() { return quantity; }
    public BigDecimal getCalculatedHours() { return calculatedHours; }
    public BigDecimal getHourlyRateSnapshot() { return hourlyRateSnapshot; }
    public BigDecimal getGrossAmount() { return grossAmount; }
    public LogStatus getStatus() { return status; }
    public String getNotes() { return notes; }
    public int getNormalRooms(){return normalRooms;} public int getJuniorRooms(){return juniorRooms;} public int getPresidentRooms(){return presidentRooms;}
    public String getAttachmentName(){return attachmentName;} public boolean hasAttachment(){return attachmentData!=null&&!attachmentData.isBlank();}
    public String getAttachmentData(){return attachmentData;}
    public String getRejectionReason(){return rejectionReason;}
}

