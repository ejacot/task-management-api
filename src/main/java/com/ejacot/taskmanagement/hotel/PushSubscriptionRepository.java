package com.ejacot.taskmanagement.hotel;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, Long> {
    boolean existsByEndpoint(String endpoint);
}
