package com.ejacot.taskmanagement.hotel;

import com.ejacot.taskmanagement.user.UserAccount;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "push_subscriptions")
public class PushSubscription {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id") private UserAccount user;
    @Column(nullable = false, length = 1000) private String endpoint;
    @Column(length = 255) private String p256dh;
    @Column(length = 255) private String auth;
    @Column(name = "created_at", nullable = false) private Instant createdAt = Instant.now();
    protected PushSubscription() {}
    public PushSubscription(UserAccount user, String endpoint, String p256dh, String auth) {
        this.user = user; this.endpoint = endpoint; this.p256dh = p256dh; this.auth = auth;
    }
    public String getEndpoint() { return endpoint; }
}
