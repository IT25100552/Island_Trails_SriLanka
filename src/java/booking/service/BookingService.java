package com.islandtrails.booking.service;

import com.islandtrails.booking.entity.Booking;
import com.islandtrails.booking.entity.BookingStatus;
import com.islandtrails.booking.repository.BookingRepository;
import com.islandtrails.catalog.entity.Package;
import com.islandtrails.catalog.repository.PackageRepository;
import com.islandtrails.common.exception.ResourceConflictException;
import com.islandtrails.common.exception.ResourceNotFoundException;
import com.islandtrails.common.exception.ValidationException;
import com.islandtrails.resource.entity.ResourceAssignment;
import com.islandtrails.resource.repository.ResourceAssignmentRepository;
import com.islandtrails.resource.service.AvailabilityService;
import com.islandtrails.resource.service.ConflictDetectionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Scheduled;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final PackageRepository packageRepository;
    private final ResourceAssignmentRepository resourceAssignmentRepository;
    private final ConflictDetectionService conflictDetectionService;
    private final AvailabilityService availabilityService;

    public BookingService(BookingRepository bookingRepository,
                          PackageRepository packageRepository,
                          ResourceAssignmentRepository resourceAssignmentRepository,
                          ConflictDetectionService conflictDetectionService,
                          AvailabilityService availabilityService) {
        this.bookingRepository = bookingRepository;
        this.packageRepository = packageRepository;
        this.resourceAssignmentRepository = resourceAssignmentRepository;
        this.conflictDetectionService = conflictDetectionService;
        this.availabilityService = availabilityService;
    }

    public List<Booking> getAllBookings() {
        return bookingRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<Booking> getBookingsByCustomer(Long customerId) {
        return bookingRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    public List<Booking> getBookingsByStatus(BookingStatus status) {
        return bookingRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    public Booking getBookingById(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));
    }

    @Transactional
    public Booking createBooking(Long customerId, String customerName, Long packageId, LocalDate startDate, LocalDate endDate, Integer numberOfTravelers, String specialRequests) {
        Package pkg = packageRepository.findById(packageId)
                .orElseThrow(() -> new ResourceNotFoundException("Package not found with id: " + packageId));

        if (startDate == null || endDate == null) {
            throw new ValidationException("Start and end dates must be provided.");
        }
        if (endDate.isBefore(startDate)) {
            throw new ValidationException("End date cannot be before start date.");
        }
        if (numberOfTravelers == null || numberOfTravelers < 1) {
            throw new ValidationException("Number of travelers must be at least 1.");
        }

        // Check for conflicts on any resources statically assigned to this package
        List<ResourceAssignment> packageAssignments = resourceAssignmentRepository.findByPackageId(packageId);
        for (ResourceAssignment ra : packageAssignments) {
            if (conflictDetectionService.hasConflict(ra.getResourceId(), startDate, endDate)) {
                throw new ResourceConflictException("Resource conflict detected for selected travel dates (" + startDate + " to " + endDate + "). Please select alternative dates.");
            }
        }

        BigDecimal totalPrice = pkg.getBasePrice().multiply(new BigDecimal(numberOfTravelers));

        Booking booking = new Booking(
                customerId,
                customerName,
                packageId,
                pkg.getName(),
                startDate,
                endDate,
                totalPrice,
                numberOfTravelers,
                specialRequests
        );
        booking.setStatus(BookingStatus.PENDING_PAYMENT);
        Booking saved = bookingRepository.save(booking);

        // Reserve package resources for this booking
        for (ResourceAssignment ra : packageAssignments) {
            availabilityService.reserveResource(ra.getResourceId(), packageId, saved.getId(), startDate, endDate);
        }

        return saved;
    }

    @Transactional
    public Booking modifyBooking(Long bookingId, LocalDate startDate, LocalDate endDate, Integer numberOfTravelers, String specialRequests) {
        Booking booking = getBookingById(bookingId);

        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT && booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new ValidationException("Cannot modify booking in " + booking.getStatus() + " status.");
        }

        Package pkg = packageRepository.findById(booking.getPackageId())
                .orElseThrow(() -> new ResourceNotFoundException("Package not found"));

        if (startDate != null && endDate != null) {
            if (endDate.isBefore(startDate)) {
                throw new ValidationException("End date cannot be before start date.");
            }

            // Release tentative and check new dates
            availabilityService.releaseAssignmentsForBooking(bookingId);

            List<ResourceAssignment> packageAssignments = resourceAssignmentRepository.findByPackageId(booking.getPackageId());
            for (ResourceAssignment ra : packageAssignments) {
                if (conflictDetectionService.hasConflict(ra.getResourceId(), startDate, endDate)) {
                    throw new ResourceConflictException("Resource conflict detected for new travel dates (" + startDate + " to " + endDate + ").");
                }
            }

            booking.setStartDate(startDate);
            booking.setEndDate(endDate);

            for (ResourceAssignment ra : packageAssignments) {
                availabilityService.reserveResource(ra.getResourceId(), booking.getPackageId(), booking.getId(), startDate, endDate);
            }
        }

        if (numberOfTravelers != null && numberOfTravelers >= 1) {
            booking.setNumberOfTravelers(numberOfTravelers);
            booking.setTotalPrice(pkg.getBasePrice().multiply(new BigDecimal(numberOfTravelers)));
        }

        if (specialRequests != null) {
            booking.setSpecialRequests(specialRequests);
        }

        return bookingRepository.save(booking);
    }

    @Transactional
    public Booking confirmBooking(Long bookingId) {
        Booking booking = getBookingById(bookingId);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setConfirmedAt(LocalDateTime.now());

        availabilityService.confirmAssignmentsForBooking(bookingId);
        return bookingRepository.save(booking);
    }

    @Scheduled(cron = "0 0 * * * ?")
    public void autoCancelStaleBookings() {
        // Find all bookings in PENDING_PAYMENT older than 24 hours and cancel them
        List<Booking> pending = bookingRepository.findByStatusOrderByCreatedAtDesc(BookingStatus.PENDING_PAYMENT);
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        for (Booking b : pending) {
            if (b.getCreatedAt().isBefore(cutoff)) {
                cancelBooking(b.getId());
            }
        }
    }

    @Transactional
    public void cancelBooking(Long bookingId) {
        Booking booking = getBookingById(bookingId);
        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new ValidationException("Can only cancel bookings pending payment.");
        }
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(LocalDateTime.now());
        availabilityService.releaseAssignmentsForBooking(bookingId);
        bookingRepository.save(booking);
    }

    @Transactional
    public Booking updateStatus(Long bookingId, BookingStatus newStatus) {
        Booking booking = getBookingById(bookingId);
        booking.setStatus(newStatus);
        if (newStatus == BookingStatus.CONFIRMED && booking.getConfirmedAt() == null) {
            booking.setConfirmedAt(LocalDateTime.now());
            availabilityService.confirmAssignmentsForBooking(bookingId);
        } else if (newStatus == BookingStatus.CANCELLED && booking.getCancelledAt() == null) {
            booking.setCancelledAt(LocalDateTime.now());
            availabilityService.releaseAssignmentsForBooking(bookingId);
        }
        return bookingRepository.save(booking);
    }
}
