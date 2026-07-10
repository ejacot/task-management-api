package com.ejacot.taskmanagement.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
    Optional<UserProfile> findByUserUsername(String username);
    Optional<UserProfile> findByUserId(Long userId);
}
