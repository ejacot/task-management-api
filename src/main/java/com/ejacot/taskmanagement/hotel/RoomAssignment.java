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
 @Column(name="created_at",nullable=false) private Instant createdAt=Instant.now();
 protected RoomAssignment(){}
 public RoomAssignment(Hotel hotel,UserAccount employee,LocalDate date,String room,String category){this.hotel=hotel;this.employee=employee;this.workDate=date;this.roomNumber=room;this.category=category;}
 public Long getId(){return id;} public UserAccount getEmployee(){return employee;} public LocalDate getWorkDate(){return workDate;} public String getRoomNumber(){return roomNumber;} public String getCategory(){return category;}
}
