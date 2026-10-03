package com.islandtrails.auth.repository;

import com.islandtrails.auth.entity.User;
import com.islandtrails.auth.entity.UserRole;
import com.islandtrails.auth.entity.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByRole(UserRole role);
    List<User> findByStatus(UserStatus status);
    long countByRole(UserRole role);
}
