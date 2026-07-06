package com.ejacot.taskmanagement.hotel;

import jakarta.persistence.*;

@Entity
@Table(name = "hotels")
public class Hotel {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 120) private String name;
    @Column(nullable = false, length = 120) private String city;
    @Column(nullable = false) private boolean active = true;

    protected Hotel() {}
    public Hotel(String name, String city) { this.name = name; this.city = city; }
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getCity() { return city; }
    public boolean isActive() { return active; }
}

