package com.ejacot.taskmanagement.hotel;

import com.ejacot.taskmanagement.user.UserAccount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class DeliveryService {
    private static final Logger log = LoggerFactory.getLogger(DeliveryService.class);

    private final NotificationRepository notifications;
    private final OutboundMessageRepository outbound;
    private final PushSubscriptionRepository pushSubscriptions;
    private final JavaMailSender mailSender;
    private final boolean mailEnabled;
    private final String mailFrom;

    public DeliveryService(NotificationRepository notifications,
                           OutboundMessageRepository outbound,
                           PushSubscriptionRepository pushSubscriptions,
                           ObjectProvider<JavaMailSender> mailSender,
                           @Value("${roomly.mail.enabled:false}") boolean mailEnabled,
                           @Value("${roomly.mail.from:no-reply@roomly.local}") String mailFrom) {
        this.notifications = notifications;
        this.outbound = outbound;
        this.pushSubscriptions = pushSubscriptions;
        this.mailSender = mailSender.getIfAvailable();
        this.mailEnabled = mailEnabled;
        this.mailFrom = mailFrom;
    }

    public void notifyInApp(UserAccount user, String title, String body, String link) {
        notifications.save(new Notification(user, title, body, link));
        queuePush(user, title, body);
    }

    public void queueEmail(UserAccount user, String subject, String body) {
        deliverEmail(user, subject, body, false);
    }

    public void sendRequiredEmail(UserAccount user, String subject, String body) {
        deliverEmail(user, subject, body, true);
    }

    public void queueSms(UserAccount user, String body) {
        if (user.getPhone() != null && !user.getPhone().isBlank()) {
            outbound.save(new OutboundMessage(user.getPhone(), "SMS", "Roomly Work", body));
        }
    }

    public void queuePush(UserAccount user, String title, String body) {
        pushSubscriptions.findAllByUserId(user.getId())
                .forEach(subscription -> outbound.save(new OutboundMessage(subscription.getEndpoint(), "PUSH", title, body)));
    }

    private void deliverEmail(UserAccount user, String subject, String body, boolean failOnError) {
        String recipient = resolveRecipient(user);
        OutboundMessage outboundMessage = outbound.save(new OutboundMessage(recipient, "EMAIL", subject, body));

        if (!mailEnabled) {
            log.info("Mail delivery is disabled. Email for {} was stored in outbound_messages only.", recipient);
            return;
        }

        if (!recipient.contains("@")) {
            handleFailure(outboundMessage, "Recipient email address is missing or invalid", null, failOnError,
                    "Adresa de email este invalidă.");
            return;
        }

        if (mailSender == null) {
            handleFailure(outboundMessage, "JavaMailSender bean is not available", null, failOnError,
                    "Serviciul de email nu este configurat.");
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(recipient);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            outboundMessage.sent();
            outbound.save(outboundMessage);
        } catch (MailException exception) {
            handleFailure(outboundMessage,
                    "Mail server rejected or could not deliver the message",
                    exception,
                    failOnError,
                    "Nu am putut trimite emailul. Verifică setările SMTP și încearcă din nou.");
        }
    }

    private String resolveRecipient(UserAccount user) {
        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            return user.getEmail();
        }
        return user.getUsername();
    }

    private void handleFailure(OutboundMessage outboundMessage,
                               String message,
                               Exception exception,
                               boolean failOnError,
                               String publicMessage) {
        String error = exception == null ? message : message + ": " + exception.getMessage();
        log.error("Email delivery failed: {}", error, exception);
        outboundMessage.failed(trim(error));
        outbound.save(outboundMessage);
        if (failOnError) {
            throw new EmailDeliveryException(publicMessage, exception);
        }
    }

    private String trim(String value) {
        if (value == null || value.length() <= 500) {
            return value;
        }
        return value.substring(0, 500);
    }
}
