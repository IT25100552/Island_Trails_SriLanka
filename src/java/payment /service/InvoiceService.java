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

@Service
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final BookingRepository bookingRepository;

    public InvoiceService(InvoiceRepository invoiceRepository, BookingRepository bookingRepository) {
        this.invoiceRepository = invoiceRepository;
        this.bookingRepository = bookingRepository;
    }

    public List<Invoice> getAllInvoices() {
        return invoiceRepository.findAllByOrderByCreatedAtDesc();
    }

    public Optional<Invoice> getInvoiceByBooking(Long bookingId) {
        return invoiceRepository.findByBookingId(bookingId);
    }

    public Invoice getInvoiceById(Long id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + id));
    }

    @Transactional
    public Invoice generateInvoice(Long bookingId) {
        Optional<Invoice> existing = invoiceRepository.findByBookingId(bookingId);
        if (existing.isPresent()) {
            return existing.get();
        }

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));

        String invoiceNumber = "INV-" + LocalDate.now().getYear() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

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

        return invoiceRepository.save(invoice);
    }

    @Transactional
    public Invoice voidInvoice(Long invoiceId) {
        Invoice invoice = getInvoiceById(invoiceId);
        invoice.setStatus(InvoiceStatus.VOID);
        return invoiceRepository.save(invoice);
    }
}
