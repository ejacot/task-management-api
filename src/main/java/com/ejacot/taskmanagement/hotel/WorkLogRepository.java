package com.ejacot.taskmanagement.hotel;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.*;

public interface WorkLogRepository extends JpaRepository<WorkLog, Long> {
    List<WorkLog> findAllByEmployeeUsernameAndWorkDateBetweenOrderByWorkDateDesc(String username, LocalDate from, LocalDate to);
    List<WorkLog> findAllByEmployeeUsernameOrderByWorkDateDesc(String username);
    boolean existsByEmployeeUsernameAndNotesContaining(String username, String marker);
    Optional<WorkLog> findByIdAndEmployeeUsername(Long id, String username);
    List<WorkLog> findAllByHotelIdAndStatusOrderByWorkDateDesc(Long hotelId,LogStatus status);
    Optional<WorkLog> findByIdAndHotelId(Long id,Long hotelId);
    Optional<WorkLog> findByShiftPlanId(Long shiftPlanId);
}
