package com.islandtrails.payment.repository;

import com.islandtrails.payment.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// Repository for managing invoice database records
@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    // Find invoice by booking ID
    Optional<Invoice> findByBookingId(Long bookingId);

    // Find invoice by unique invoice number
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    // Get all invoices ordered by creation date (newest first)
    List<Invoice> findAllByOrderByCreatedAtDesc();
}
