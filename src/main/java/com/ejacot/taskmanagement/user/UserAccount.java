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

    @Column(name="first_name",length=80) private String firstName;
    @Column(name="last_name",length=80) private String lastName;
    @Column(length=255) private String address;
    @Column(name="steuer_class") private Integer steuerClass;

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
    public String getFirstName(){return firstName;} public String getLastName(){return lastName;} public String getAddress(){return address;} public Integer getSteuerClass(){return steuerClass;}
    public UserRole getRole() { return role; }
    public BigDecimal getHourlyRate() { return hourlyRate; }
    public boolean isActive() { return active; }
    public Hotel getHotel() { return hotel; }
    public Instant getCreatedAt() { return createdAt; }
    public void configureProfile(String firstName,String lastName,String address,Integer steuerClass){this.firstName=firstName;this.lastName=lastName;this.address=address;this.steuerClass=steuerClass;}
}
