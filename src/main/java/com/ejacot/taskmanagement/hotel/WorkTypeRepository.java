package com.ejacot.taskmanagement.hotel;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface WorkTypeRepository extends JpaRepository<WorkType, Long> {
    List<WorkType> findAllByHotelIdAndActiveTrueOrderByName(Long hotelId);
    Optional<WorkType> findByHotelIdAndCode(Long hotelId,String code);
    List<WorkType> findAllByOwnerIdAndActiveTrueOrderByName(Long ownerId);

    @Query("""
            select w from WorkType w
            where w.active = true and (
                w.owner.id = :userId or (:hotelId is not null and w.hotel.id = :hotelId)
            )
            order by w.name
            """)
    List<WorkType> findAvailableForUser(@Param("userId") Long userId, @Param("hotelId") Long hotelId);
}
