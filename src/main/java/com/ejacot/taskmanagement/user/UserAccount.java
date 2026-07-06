package com.ejacot.taskmanagement.user;

import com.ejacot.taskmanagement.hotel.Hotel;
import com.ejacot.taskmanagement.hotel.UserRole;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "app_users")
public class UserAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, length = 100)
    private String password;

    @Column(unique = true, length = 150)
    private String email;

    @Column(unique = true, length = 30)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserRole role = UserRole.EMPLOYEE;

    @Column(name = "hourly_rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal hourlyRate = BigDecimal.ZERO;

    @Column(nullable = false)
    private boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id")
    private Hotel hotel;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected UserAccount() {}

    public UserAccount(String username, String password) {
        this.username = username;
        this.password = password;
        this.createdAt = Instant.now();
    }

    public UserAccount(String username, String password, String email, String phone,
                       UserRole role, BigDecimal hourlyRate, Hotel hotel) {
        this(username, password);
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.hourlyRate = hourlyRate;
        this.hotel = hotel;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public UserRole getRole() { return role; }
    public BigDecimal getHourlyRate() { return hourlyRate; }
    public boolean isActive() { return active; }
    public Hotel getHotel() { return hotel; }
    public Instant getCreatedAt() { return createdAt; }
}
