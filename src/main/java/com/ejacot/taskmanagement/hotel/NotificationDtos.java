package com.ejacot.taskmanagement.hotel;

import jakarta.validation.constraints.*;

public final class NotificationDtos {
    private NotificationDtos() {}
    public record PushSubscriptionRequest(@NotBlank @Size(max = 1000) String endpoint,
                                          @Size(max = 255) String p256dh,
                                          @Size(max = 255) String auth) {}
}
