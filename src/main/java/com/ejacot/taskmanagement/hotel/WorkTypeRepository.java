package com.ejacot.taskmanagement.hotel;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface WorkTypeRepository extends JpaRepository<WorkType, Long> {
    List<WorkType> findAllByHotelIdAndActiveTrueOrderByName(Long hotelId);
    Optional<WorkType> findByHotelIdAndCode(Long hotelId,String code);
}
