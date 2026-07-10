package com.ejacot.taskmanagement.hotel;

import com.ejacot.taskmanagement.user.UserAccount;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalTime;

@Entity
@Table(name = "work_types")
public class WorkType {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "hotel_id") private Hotel hotel;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "owner_id") private UserAccount owner;
    @Column(nullable = false, length = 30) private String code;
    @Column(nullable = false, length = 100) private String name;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private WorkUnit unit;
    @Column(name = "rooms_per_hour", precision = 8, scale = 2) private BigDecimal roomsPerHour;
    @Column(nullable = false, length = 20) private String color;
    @Column(nullable = false) private boolean active = true;
    @Column(name="custom_hourly_rate", precision = 10, scale = 2) private BigDecimal customHourlyRate;
    @Column(name="default_start_time") private LocalTime defaultStartTime;
    @Column(name="default_end_time") private LocalTime defaultEndTime;
    @Column(name="default_break_minutes",nullable=false) private int defaultBreakMinutes;

    protected WorkType() {}
    public WorkType(Hotel hotel, String code, String name, WorkUnit unit, BigDecimal roomsPerHour, String color) {
        this.hotel = hotel; this.code = code; this.name = name; this.unit = unit;
        this.roomsPerHour = roomsPerHour; this.color = color;
    }
    public WorkType(UserAccount owner, String code, String name, WorkUnit unit, BigDecimal roomsPerHour, String color) {
        this.owner = owner; this.code = code; this.name = name; this.unit = unit;
        this.roomsPerHour = roomsPerHour; this.color = color;
    }
    public Long getId() { return id; }
    public Hotel getHotel() { return hotel; }
    public UserAccount getOwner() { return owner; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public WorkUnit getUnit() { return unit; }
    public BigDecimal getRoomsPerHour() { return roomsPerHour; }
    public String getColor() { return color; }
    public boolean isActive() { return active; }
    public BigDecimal getCustomHourlyRate() { return customHourlyRate; }
    public LocalTime getDefaultStartTime(){return defaultStartTime;} public LocalTime getDefaultEndTime(){return defaultEndTime;} public int getDefaultBreakMinutes(){return defaultBreakMinutes;}
    public void configureDefaults(LocalTime start,LocalTime end,int breakMinutes){this.defaultStartTime=start;this.defaultEndTime=end;this.defaultBreakMinutes=breakMinutes;}
    public void update(String code,String name,WorkUnit unit,BigDecimal roomsPerHour,String color,boolean active,LocalTime start,LocalTime end,int breakMinutes){this.code=code;this.name=name;this.unit=unit;this.roomsPerHour=roomsPerHour;this.color=color;this.active=active;this.defaultStartTime=start;this.defaultEndTime=end;this.defaultBreakMinutes=breakMinutes;}
    public void updatePersonal(String code,String name,String color,boolean active,BigDecimal customHourlyRate,int breakMinutes){this.code=code;this.name=name;this.color=color;this.active=active;this.customHourlyRate=customHourlyRate;this.defaultBreakMinutes=breakMinutes;this.unit=WorkUnit.HOURLY;this.roomsPerHour=null;}
}

