package com.islandtrails.payment.repository;

import com.islandtrails.payment.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    Optional<Invoice> findByBookingId(Long bookingId);
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);
    List<Invoice> findAllByOrderByCreatedAtDesc();
}
