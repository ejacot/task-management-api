package com.ejacot.taskmanagement.hotel;

import com.ejacot.taskmanagement.user.UserAccount;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "notifications")
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "recipient_id") private UserAccount recipient;
    @Column(nullable = false, length = 150) private String title;
    @Column(nullable = false, length = 500) private String message;
    @Column(length = 200) private String link;
    @Column(name = "is_read", nullable = false) private boolean read;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    protected Notification() {}
    public Notification(UserAccount recipient, String title, String message, String link) {
        this.recipient=recipient;this.title=title;this.message=message;this.link=link;this.createdAt=Instant.now();
    }
    public void markRead(){read=true;}
    public Long getId(){return id;} public String getTitle(){return title;} public String getMessage(){return message;}
    public String getLink(){return link;} public boolean isRead(){return read;} public Instant getCreatedAt(){return createdAt;}
}

