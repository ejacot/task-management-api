package com.ejacot.taskmanagement.hotel;

import com.ejacot.taskmanagement.user.UserAccount;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;

@Service
public class DeliveryService {
    private final NotificationRepository notifications; private final OutboundMessageRepository outbound; private final PushSubscriptionRepository pushSubscriptions;
    private final JavaMailSender mailSender;
    private final boolean mailEnabled;
    private final String mailFrom;
    private final String mailFromName;

    public DeliveryService(NotificationRepository notifications, OutboundMessageRepository outbound, PushSubscriptionRepository pushSubscriptions,
                           JavaMailSender mailSender,
                           @Value("${roomly.mail.enabled:false}") boolean mailEnabled,
                           @Value("${roomly.mail.from:no-reply@roomly.local}") String mailFrom,
                           @Value("${roomly.mail.from-name:Roomly Work}") String mailFromName) {
        this.notifications = notifications;
        this.outbound = outbound;
        this.pushSubscriptions = pushSubscriptions;
        this.mailSender = mailSender;
        this.mailEnabled = mailEnabled;
        this.mailFrom = mailFrom;
        this.mailFromName = mailFromName;
    }
    public void notifyInApp(UserAccount user, String title, String body, String link) {
        notifications.save(new Notification(user, title, body, link));
        queuePush(user, title, body);
    }
    public void queueEmail(UserAccount user, String subject, String body) {
        String recipient = user.getEmail() == null || user.getEmail().isBlank() ? user.getUsername() : user.getEmail();
        OutboundMessage message = outbound.save(new OutboundMessage(recipient, "EMAIL", subject, body));
        if (mailEnabled && recipient.contains("@")) {
            sendEmail(message, recipient, subject, body);
        }
    }
    public void queueSms(UserAccount user, String body) {
        if (user.getPhone() != null && !user.getPhone().isBlank()) outbound.save(new OutboundMessage(user.getPhone(), "SMS", "Roomly Work", body));
    }
    public void queuePush(UserAccount user, String title, String body) {
        pushSubscriptions.findAllByUserId(user.getId()).forEach(subscription -> outbound.save(new OutboundMessage(subscription.getEndpoint(), "PUSH", title, body)));
    }

    private void sendEmail(OutboundMessage outboundMessage, String recipient, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(recipient);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            outboundMessage.sent();
        } catch (MailException ignored) {
            outboundMessage.failed();
            // The message is still stored in outbound_messages for later retry/debugging.
        }
    }
}
