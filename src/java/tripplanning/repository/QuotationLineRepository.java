package com.islandtrails.tripplanning.repository;

import com.islandtrails.tripplanning.entity.QuotationLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuotationLineRepository extends JpaRepository<QuotationLine, Long> {
    List<QuotationLine> findByQuotationIdOrderBySequenceAsc(Long quotationId);
}
