package com.ejacot.taskmanagement.hotel;

import com.ejacot.taskmanagement.user.UserAccount;
import jakarta.persistence.*;
import java.time.*;

@Entity @Table(name="employee_requests")
public class EmployeeRequest {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="employee_id") private UserAccount employee;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="hotel_id") private Hotel hotel;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private EmployeeRequestType type;
 @Column(name="start_date",nullable=false) private LocalDate startDate;
 @Column(name="end_date",nullable=false) private LocalDate endDate;
 @Column(length=500) private String message;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private EmployeeRequestStatus status=EmployeeRequestStatus.PENDING;
 @Column(name="manager_response",length=500) private String managerResponse;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="reviewed_by") private UserAccount reviewedBy;
 @Column(name="reviewed_at") private Instant reviewedAt;
 @Column(name="created_at",nullable=false) private Instant createdAt=Instant.now();
 protected EmployeeRequest(){}
 public EmployeeRequest(UserAccount employee,Hotel hotel,EmployeeRequestType type,LocalDate start,LocalDate end,String message){this.employee=employee;this.hotel=hotel;this.type=type;this.startDate=start;this.endDate=end;this.message=message;}
 public void review(UserAccount reviewer,boolean approved,String response){status=approved?EmployeeRequestStatus.APPROVED:EmployeeRequestStatus.REJECTED;reviewedBy=reviewer;reviewedAt=Instant.now();managerResponse=response;}
 public Long getId(){return id;} public UserAccount getEmployee(){return employee;} public EmployeeRequestType getType(){return type;} public LocalDate getStartDate(){return startDate;} public LocalDate getEndDate(){return endDate;} public String getMessage(){return message;} public EmployeeRequestStatus getStatus(){return status;} public String getManagerResponse(){return managerResponse;} public Instant getCreatedAt(){return createdAt;}
}
