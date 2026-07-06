package com.ejacot.taskmanagement.hotel;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.*;

public interface WorkLogRepository extends JpaRepository<WorkLog, Long> {
    List<WorkLog> findAllByEmployeeUsernameAndWorkDateBetweenOrderByWorkDateDesc(String username, LocalDate from, LocalDate to);
    Optional<WorkLog> findByIdAndEmployeeUsername(Long id, String username);
}
