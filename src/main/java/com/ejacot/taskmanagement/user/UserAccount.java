package com.ejacot.taskmanagement.user;

import jakarta.persistence.*;
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

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected UserAccount() {}

    public UserAccount(String username, String password) {
        this.username = username;
        this.password = password;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public Instant getCreatedAt() { return createdAt; }
}

