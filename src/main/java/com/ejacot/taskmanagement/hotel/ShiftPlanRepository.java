package com.ejacot.taskmanagement.hotel;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface ShiftPlanRepository extends JpaRepository<ShiftPlan, Long> {
    List<ShiftPlan> findAllByEmployeeUsernameAndWorkDateBetweenOrderByWorkDateAscStartTimeAsc(
            String username, LocalDate from, LocalDate to);
}

