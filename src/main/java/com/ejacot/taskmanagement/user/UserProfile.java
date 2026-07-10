package com.ejacot.taskmanagement.user;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "user_profiles")
public class UserProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserAccount user;

    @Column(name = "first_name", length = 80)
    private String firstName;

    @Column(name = "last_name", length = 80)
    private String lastName;

    @Column(name = "default_hourly_rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal defaultHourlyRate = BigDecimal.ZERO;

    @Column(nullable = false, length = 3)
    private String currency = "EUR";

    @Column(name = "default_break_minutes", nullable = false)
    private int defaultBreakMinutes = 30;

    @Column(nullable = false, length = 10)
    private String language = "ro";

    protected UserProfile() {}

    public UserProfile(UserAccount user) {
        this.user = user;
    }

    public Long getId() { return id; }
    public UserAccount getUser() { return user; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public BigDecimal getDefaultHourlyRate() { return defaultHourlyRate; }
    public String getCurrency() { return currency; }
    public int getDefaultBreakMinutes() { return defaultBreakMinutes; }
    public String getLanguage() { return language; }

    public boolean isCompleted() {
        return firstName != null && !firstName.isBlank()
                && lastName != null && !lastName.isBlank()
                && defaultHourlyRate != null
                && currency != null && !currency.isBlank()
                && language != null && !language.isBlank();
    }

    public void update(String firstName, String lastName, BigDecimal defaultHourlyRate, String currency, int defaultBreakMinutes, String language) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.defaultHourlyRate = defaultHourlyRate;
        this.currency = currency;
        this.defaultBreakMinutes = defaultBreakMinutes;
        this.language = language;
    }
}
