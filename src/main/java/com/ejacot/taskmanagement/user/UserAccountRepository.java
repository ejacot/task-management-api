package com.ejacot.taskmanagement.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.List;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByUsername(String username);
    boolean existsByUsername(String username);

    @Query("select u from UserAccount u where lower(u.username) = lower(:login) or lower(u.email) = lower(:login) or u.phone = :login")
    Optional<UserAccount> findByLogin(@Param("login") String login);
    List<UserAccount> findAllByHotelIdAndActiveTrueOrderByUsername(Long hotelId);
}
