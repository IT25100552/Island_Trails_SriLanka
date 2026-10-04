package com.islandtrails.tripplanning.service;

import com.islandtrails.common.exception.ResourceNotFoundException;
import com.islandtrails.common.exception.ValidationException;
import com.islandtrails.tripplanning.entity.TripRequest;
import com.islandtrails.tripplanning.entity.TripRequestStatus;
import com.islandtrails.tripplanning.repository.TripRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class TripRequestService {

    private final TripRequestRepository tripRequestRepository;

    public TripRequestService(TripRequestRepository tripRequestRepository) {
        this.tripRequestRepository = tripRequestRepository;
    }

    public List<TripRequest> getAllTripRequests() {
        return tripRequestRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<TripRequest> getPendingReviewRequests() {
        return tripRequestRepository.findByStatusOrderByCreatedAtDesc(TripRequestStatus.SUBMITTED);
    }

    public List<TripRequest> getRequestsByCustomer(Long customerId) {
        return tripRequestRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    public TripRequest getTripRequestById(Long id) {
        return tripRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trip request not found with id: " + id));
    }

    @Transactional
    public TripRequest submitTripRequest(Long customerId, String customerName, BigDecimal budget, LocalDate startDate, LocalDate endDate, String interests, String specialRequirements, Integer numberOfTravelers) {
        if (budget == null || budget.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Budget must be greater than zero.");
        }
        if (startDate == null || endDate == null) {
            throw new ValidationException("Start and End travel dates must be specified.");
        }
        if (endDate.isBefore(startDate)) {
            throw new ValidationException("End date cannot be before start date.");
        }

        TripRequest request = new TripRequest(customerId, customerName, budget, startDate, endDate, interests, specialRequirements, numberOfTravelers);
        return tripRequestRepository.save(request);
    }

    @Transactional
    public TripRequest updateStatus(Long id, TripRequestStatus status) {
        TripRequest request = getTripRequestById(id);
        request.setStatus(status);
        return tripRequestRepository.save(request);
    }
}
