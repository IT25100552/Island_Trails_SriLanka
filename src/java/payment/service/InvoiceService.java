package com.islandtrails.payment.service;

import com.islandtrails.booking.entity.Booking;
import com.islandtrails.booking.repository.BookingRepository;
import com.islandtrails.common.exception.ResourceNotFoundException;
import com.islandtrails.payment.entity.Invoice;
import com.islandtrails.payment.entity.InvoiceStatus;
import com.islandtrails.payment.repository.InvoiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Service for creating, finding, and voiding customer invoices
@Service
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final BookingRepository bookingRepository;

    // Injects invoice and booking repositories
    public InvoiceService(InvoiceRepository invoiceRepository, BookingRepository bookingRepository) {
        this.invoiceRepository = invoiceRepository;
        this.bookingRepository = bookingRepository;
    }

    // Returns all invoices ordered by newest first
    public List<Invoice> getAllInvoices() {
        return invoiceRepository.findAllByOrderByCreatedAtDesc();
    }

    // Finds an invoice for a specific booking
    public Optional<Invoice> getInvoiceByBooking(Long bookingId) {
        return invoiceRepository.findByBookingId(bookingId);
    }

    // Finds an invoice by its ID or throws an error if not found
    public Invoice getInvoiceById(Long id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + id));
    }

    // Generates and saves a PAID invoice for a confirmed booking (or returns existing one)
    @Transactional
    public Invoice generateInvoice(Long bookingId) {
        // Step 1: Return existing invoice if one was already created for this booking
        Optional<Invoice> existing = invoiceRepository.findByBookingId(bookingId);
        if (existing.isPresent()) {
            Invoice inv = existing.get();
            if (inv.getStatus() != InvoiceStatus.VOID) {
                return inv;
            }
            // If previously voided, reactivate with a fresh invoice number and current booking details
            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));
            inv.setInvoiceNumber("INV-" + LocalDate.now().getYear() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            inv.setIssueDate(LocalDate.now());
            inv.setDueDate(LocalDate.now().plusDays(7));
            inv.setTotalAmount(booking.getTotalPrice());
            inv.setStatus(InvoiceStatus.PAID);
            inv.setCustomerName(booking.getCustomerName());
            inv.setPackageName(booking.getPackageName());
            return invoiceRepository.save(inv);
        }

        // Step 2: Look up the booking to get customer and price details
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));

        // Step 3: Create a unique invoice number with year and random ID
        String invoiceNumber = "INV-" + LocalDate.now().getYear() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // Step 4: Build the invoice object marked as PAID
        Invoice invoice = new Invoice(
                booking.getId(),
                invoiceNumber,
                LocalDate.now(),
                booking.getTotalPrice(),
                InvoiceStatus.PAID,
                booking.getCustomerName(),
                null,
                booking.getPackageName()
        );

        // Step 5: Save and return the invoice
        return invoiceRepository.save(invoice);
    }

    // Marks an existing invoice as VOID
    @Transactional
    public Invoice voidInvoice(Long invoiceId) {
        // Step 1: Find the invoice by ID
        Invoice invoice = getInvoiceById(invoiceId);

        // Step 2: Update status to VOID
        invoice.setStatus(InvoiceStatus.VOID);

        // Step 3: Save and return the updated invoice
        return invoiceRepository.save(invoice);
    }
}
