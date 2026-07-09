package com.ejacot.taskmanagement.hotel;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "outbound_messages")
public class OutboundMessage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 255) private String recipient;
    @Column(nullable = false, length = 30) private String channel;
    @Column(nullable = false, length = 255) private String subject;
    @Column(nullable = false, columnDefinition = "TEXT") private String body;
    @Column(nullable = false, length = 30) private String status = "QUEUED";
    @Column(name = "created_at", nullable = false) private Instant createdAt = Instant.now();
    protected OutboundMessage() {}
    public OutboundMessage(String recipient, String channel, String subject, String body) {
        this.recipient = recipient; this.channel = channel; this.subject = subject; this.body = body;
    }
}
