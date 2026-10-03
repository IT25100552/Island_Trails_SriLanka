package com.islandtrails.auth.repository;

import com.islandtrails.auth.entity.UserAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserActionRepository extends JpaRepository<UserAction, Long> {
    List<UserAction> findAllByOrderByTimestampDesc();
    List<UserAction> findByUserIdOrderByTimestampDesc(Long userId);
    List<UserAction> findTop50ByOrderByTimestampDesc();
}
