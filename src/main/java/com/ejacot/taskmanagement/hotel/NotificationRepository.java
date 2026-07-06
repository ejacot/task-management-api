package com.ejacot.taskmanagement.hotel;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification,Long>{
    List<Notification> findTop20ByRecipientUsernameOrderByCreatedAtDesc(String username);
}

