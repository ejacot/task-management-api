package com.ejacot.taskmanagement.task;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {
    Page<Task> findAllByOwnerUsername(String username, Pageable pageable);
    Page<Task> findAllByOwnerUsernameAndStatus(String username, TaskStatus status, Pageable pageable);
    Optional<Task> findByIdAndOwnerUsername(Long id, String username);
}

