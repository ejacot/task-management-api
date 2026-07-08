package com.ejacot.taskmanagement.hotel;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface ShiftPlanRepository extends JpaRepository<ShiftPlan, Long> {
    List<ShiftPlan> findAllByEmployeeUsernameAndWorkDateBetweenOrderByWorkDateAscStartTimeAsc(
            String username, LocalDate from, LocalDate to);
    List<ShiftPlan> findAllByHotelIdAndWorkDateBetweenOrderByWorkDateAscStartTimeAsc(Long hotelId,LocalDate from,LocalDate to);
    java.util.Optional<ShiftPlan> findByIdAndHotelId(Long id,Long hotelId);
    List<ShiftPlan> findAllByEmployeeIdAndWorkDate(Long employeeId,LocalDate workDate);
}

