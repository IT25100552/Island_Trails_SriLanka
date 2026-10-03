package com.islandtrails.auth.repository;

import com.islandtrails.auth.entity.SystemConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SystemConfigRepository extends JpaRepository<SystemConfig, String> {
    Optional<SystemConfig> findByKey(String key);
}
