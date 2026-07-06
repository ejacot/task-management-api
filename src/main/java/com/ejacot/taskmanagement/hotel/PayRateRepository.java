package com.ejacot.taskmanagement.hotel;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.Optional;

public interface PayRateRepository extends JpaRepository<PayRate,Long>{
    Optional<PayRate> findFirstByEmployeeUsernameAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(String username, LocalDate date);
}
