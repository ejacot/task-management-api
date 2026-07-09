package com.ejacot.taskmanagement.hotel;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, Long> {
    boolean existsByEndpoint(String endpoint);
    List<PushSubscription> findAllByUserId(Long userId);
}
