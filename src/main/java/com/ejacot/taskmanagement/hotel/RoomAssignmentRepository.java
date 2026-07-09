package com.ejacot.taskmanagement.hotel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
public interface RoomAssignmentRepository extends JpaRepository<RoomAssignment,Long>{
 List<RoomAssignment> findAllByHotelIdAndWorkDateBetweenOrderByWorkDateAscRoomNumberAsc(Long hotel,LocalDate from,LocalDate to);
 List<RoomAssignment> findAllByEmployeeUsernameAndWorkDateBetweenOrderByWorkDateAscRoomNumberAsc(String username,LocalDate from,LocalDate to);
 List<RoomAssignment> findAllByHotelIdAndWorkDateOrderByRoomNumberAsc(Long hotel,LocalDate date);
 java.util.Optional<RoomAssignment> findByIdAndHotelId(Long id,Long hotel);
 java.util.Optional<RoomAssignment> findByIdAndEmployeeUsername(Long id,String username);
 void deleteAllByEmployeeIdAndWorkDate(Long employeeId,LocalDate date);
}
