package com.ejacot.taskmanagement.hotel;

import com.ejacot.taskmanagement.user.UserAccount;
import org.springframework.stereotype.Service;

@Service
public class DeliveryService {
    private final NotificationRepository notifications; private final OutboundMessageRepository outbound; private final PushSubscriptionRepository pushSubscriptions;
    public DeliveryService(NotificationRepository notifications, OutboundMessageRepository outbound, PushSubscriptionRepository pushSubscriptions) { this.notifications = notifications; this.outbound = outbound; this.pushSubscriptions = pushSubscriptions; }
    public void notifyInApp(UserAccount user, String title, String body, String link) {
        notifications.save(new Notification(user, title, body, link));
        queuePush(user, title, body);
    }
    public void queueEmail(UserAccount user, String subject, String body) {
        String recipient = user.getEmail() == null || user.getEmail().isBlank() ? user.getUsername() : user.getEmail();
        outbound.save(new OutboundMessage(recipient, "EMAIL", subject, body));
    }
    public void queueSms(UserAccount user, String body) {
        if (user.getPhone() != null && !user.getPhone().isBlank()) outbound.save(new OutboundMessage(user.getPhone(), "SMS", "Roomly Work", body));
    }
    public void queuePush(UserAccount user, String title, String body) {
        pushSubscriptions.findAllByUserId(user.getId()).forEach(subscription -> outbound.save(new OutboundMessage(subscription.getEndpoint(), "PUSH", title, body)));
    }
}
