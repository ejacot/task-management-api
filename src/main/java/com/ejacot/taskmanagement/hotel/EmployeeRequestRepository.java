package com.ejacot.taskmanagement.hotel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface EmployeeRequestRepository extends JpaRepository<EmployeeRequest,Long>{
 List<EmployeeRequest> findAllByEmployeeUsernameOrderByCreatedAtDesc(String username);
 List<EmployeeRequest> findAllByHotelIdAndStatusOrderByCreatedAtDesc(Long hotelId,EmployeeRequestStatus status);
}
