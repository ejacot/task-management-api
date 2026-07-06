package com.ejacot.taskmanagement.hotel;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "hotels")
public class Hotel {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 120) private String name;
    @Column(nullable = false, length = 120) private String city;
    @Column(nullable = false) private boolean active = true;
    @Column(name="normal_rooms_per_hour",nullable=false) private BigDecimal normalRoomsPerHour=new BigDecimal("2.40");
    @Column(name="junior_rooms_per_hour",nullable=false) private BigDecimal juniorRoomsPerHour=new BigDecimal("1.60");
    @Column(name="president_rooms_per_hour",nullable=false) private BigDecimal presidentRoomsPerHour=new BigDecimal("1.20");
    @Column(name="sunday_premium_percent",nullable=false) private BigDecimal sundayPremiumPercent=new BigDecimal("50");
    @Column(name="night_premium_percent",nullable=false) private BigDecimal nightPremiumPercent=new BigDecimal("25");
    @Column(name="holiday_premium_percent",nullable=false) private BigDecimal holidayPremiumPercent=new BigDecimal("100");

    protected Hotel() {}
    public Hotel(String name, String city) { this.name = name; this.city = city; }
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getCity() { return city; }
    public boolean isActive() { return active; }
    public BigDecimal getNormalRoomsPerHour(){return normalRoomsPerHour;} public BigDecimal getJuniorRoomsPerHour(){return juniorRoomsPerHour;}
    public BigDecimal getPresidentRoomsPerHour(){return presidentRoomsPerHour;} public BigDecimal getSundayPremiumPercent(){return sundayPremiumPercent;}
    public BigDecimal getNightPremiumPercent(){return nightPremiumPercent;} public BigDecimal getHolidayPremiumPercent(){return holidayPremiumPercent;}
}

