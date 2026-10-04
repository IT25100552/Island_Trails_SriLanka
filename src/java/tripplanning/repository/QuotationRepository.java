package com.islandtrails.tripplanning.repository;

import com.islandtrails.tripplanning.entity.Quotation;
import com.islandtrails.tripplanning.entity.QuotationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuotationRepository extends JpaRepository<Quotation, Long> {
    List<Quotation> findByTripRequestIdOrderByQuotationVersionDesc(Long tripRequestId);
    Optional<Quotation> findFirstByTripRequestIdOrderByQuotationVersionDesc(Long tripRequestId);
    List<Quotation> findByConsultantIdOrderByCreatedAtDesc(Long consultantId);
    List<Quotation> findByStatusOrderByCreatedAtDesc(QuotationStatus status);
}
