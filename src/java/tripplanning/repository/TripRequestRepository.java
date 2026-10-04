package com.islandtrails.tripplanning.repository;

import com.islandtrails.tripplanning.entity.TripRequest;
import com.islandtrails.tripplanning.entity.TripRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TripRequestRepository extends JpaRepository<TripRequest, Long> {
    List<TripRequest> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
    List<TripRequest> findByStatusOrderByCreatedAtDesc(TripRequestStatus status);
    List<TripRequest> findAllByOrderByCreatedAtDesc();
}
