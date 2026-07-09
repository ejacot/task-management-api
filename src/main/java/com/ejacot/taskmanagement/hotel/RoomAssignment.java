package com.ejacot.taskmanagement.hotel;

import com.ejacot.taskmanagement.user.UserAccount;
import jakarta.persistence.*;
import java.time.*;

@Entity @Table(name="room_assignments")
public class RoomAssignment {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="hotel_id") private Hotel hotel;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="employee_id") private UserAccount employee;
 @Column(name="work_date",nullable=false) private LocalDate workDate;
 @Column(name="room_number",nullable=false,length=20) private String roomNumber;
 @Column(nullable=false,length=20) private String category;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private RoomAssignmentStatus status=RoomAssignmentStatus.ASSIGNED;
 @Column(length=500) private String notes;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="checked_by") private UserAccount checkedBy;
 @Column(name="checked_at") private Instant checkedAt;
 @Column(name="defect_description",length=500) private String defectDescription;
 @Column(name="completed_at") private Instant completedAt;
 @Enumerated(EnumType.STRING) @Column(name="defect_status",length=30) private DefectStatus defectStatus;
 @Column(name="defect_resolved_at") private Instant defectResolvedAt;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="defect_resolved_by") private UserAccount defectResolvedBy;
 @Column(name="defect_photo_name",length=255) private String defectPhotoName;
 @Column(name="defect_photo_data",columnDefinition="TEXT") private String defectPhotoData;
 @Column(name="created_at",nullable=false) private Instant createdAt=Instant.now();
 protected RoomAssignment(){}
 public RoomAssignment(Hotel hotel,UserAccount employee,LocalDate date,String room,String category){this.hotel=hotel;this.employee=employee;this.workDate=date;this.roomNumber=room;this.category=category;}
 public void complete(String notes){this.status=RoomAssignmentStatus.COMPLETED;this.notes=notes;this.completedAt=Instant.now();}
 public void review(UserAccount checker,RoomAssignmentStatus status,String notes,String defectDescription){this.checkedBy=checker;this.checkedAt=Instant.now();this.status=status;this.notes=notes;this.defectDescription=defectDescription;if(status==RoomAssignmentStatus.DEFECT)this.defectStatus=DefectStatus.OPEN;}
 public void startDefect(UserAccount actor,String notes){this.defectStatus=DefectStatus.IN_PROGRESS;this.notes=notes;this.checkedBy=actor;this.checkedAt=Instant.now();}
 public void resolveDefect(UserAccount actor,String notes){this.defectStatus=DefectStatus.RESOLVED;this.defectResolvedBy=actor;this.defectResolvedAt=Instant.now();this.status=RoomAssignmentStatus.CHECKED;this.notes=notes;}
 public void attachDefectPhoto(String name,String data){this.defectPhotoName=name;this.defectPhotoData=data;}
 public Long getId(){return id;} public Hotel getHotel(){return hotel;} public UserAccount getEmployee(){return employee;} public LocalDate getWorkDate(){return workDate;} public String getRoomNumber(){return roomNumber;} public String getCategory(){return category;} public RoomAssignmentStatus getStatus(){return status;} public String getNotes(){return notes;} public UserAccount getCheckedBy(){return checkedBy;} public Instant getCheckedAt(){return checkedAt;} public String getDefectDescription(){return defectDescription;} public Instant getCompletedAt(){return completedAt;} public DefectStatus getDefectStatus(){return defectStatus;} public Instant getDefectResolvedAt(){return defectResolvedAt;} public UserAccount getDefectResolvedBy(){return defectResolvedBy;} public String getDefectPhotoName(){return defectPhotoName;} public boolean hasDefectPhoto(){return defectPhotoData!=null&&!defectPhotoData.isBlank();}
}
