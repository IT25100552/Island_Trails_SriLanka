package com.islandtrails.booking.service;

import com.islandtrails.booking.entity.Booking;
import com.islandtrails.booking.entity.BookingStatus;
import com.islandtrails.booking.repository.BookingRepository;
import com.islandtrails.catalog.entity.Package;
import com.islandtrails.catalog.repository.PackageRepository;
import com.islandtrails.common.exception.ResourceConflictException;
import com.islandtrails.common.exception.ResourceNotFoundException;
import com.islandtrails.common.exception.ValidationException;
import com.islandtrails.payment.repository.PaymentRepository;
import com.islandtrails.resource.entity.ResourceAssignment;
import com.islandtrails.resource.repository.ResourceAssignmentRepository;
import com.islandtrails.resource.service.AvailabilityService;
import com.islandtrails.resource.service.ConflictDetectionService;
import com.islandtrails.tripplanning.entity.TripRequestStatus;
import com.islandtrails.tripplanning.repository.TripRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Scheduled;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final PackageRepository packageRepository;
    private final ResourceAssignmentRepository resourceAssignmentRepository;
    private final ConflictDetectionService conflictDetectionService;
    private final AvailabilityService availabilityService;
    private final PaymentRepository paymentRepository;
    private final TripRequestRepository tripRequestRepository;

    // Injects required repositories and services for handling bookings
    public BookingService(BookingRepository bookingRepository,
                          PackageRepository packageRepository,
                          ResourceAssignmentRepository resourceAssignmentRepository,
                          ConflictDetectionService conflictDetectionService,
                          AvailabilityService availabilityService,
                          PaymentRepository paymentRepository,
                          TripRequestRepository tripRequestRepository) {
        this.bookingRepository = bookingRepository;
        this.packageRepository = packageRepository;
        this.resourceAssignmentRepository = resourceAssignmentRepository;
        this.conflictDetectionService = conflictDetectionService;
        this.availabilityService = availabilityService;
        this.paymentRepository = paymentRepository;
        this.tripRequestRepository = tripRequestRepository;
    }

    // Returns all bookings sorted from newest to oldest
    public List<Booking> getAllBookings() {
        return bookingRepository.findAllByOrderByCreatedAtDesc();
    }

    // Returns all non-archived bookings placed by a specific customer (for "My Bookings" view)
    public List<Booking> getBookingsByCustomer(Long customerId) {
        return bookingRepository.findByCustomerIdAndArchivedFalseOrderByCreatedAtDesc(customerId);
    }

    // Returns all bookings placed by a specific customer, including archived (for staff/admin use)
    public List<Booking> getAllBookingsByCustomer(Long customerId) {
        return bookingRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    // Returns all bookings matching a specific status
    public List<Booking> getBookingsByStatus(BookingStatus status) {
        return bookingRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    // Checks if a booking exists by its ID
    public boolean bookingExists(Long id) {
        return bookingRepository.existsById(id);
    }

    // Finds a booking by ID or throws an error if not found
    public Booking getBookingById(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));
    }

    // Creates a new booking, checks for resource conflicts, and tentatively holds resources
    @Transactional
    public Booking createBooking(Long customerId, String customerName, Long packageId, LocalDate startDate, LocalDate endDate, Integer numberOfTravelers, String specialRequests) {
        // Step 1: Find the tour package or throw an error if not found
        Package pkg = packageRepository.findById(packageId)
                .orElseThrow(() -> new ResourceNotFoundException("Package not found with id: " + packageId));

        // Step 2: Validate travel dates and ensure traveler count is at least 1
        if (startDate == null || endDate == null) {
            throw new ValidationException("Start and end dates must be provided.");
        }
        if (endDate.isBefore(startDate)) {
            throw new ValidationException("End date cannot be before start date.");
        }
        if (numberOfTravelers == null || numberOfTravelers < 1) {
            throw new ValidationException("Number of travelers must be at least 1.");
        }

        // Step 3: Find distinct template resources configured for this package
        List<ResourceAssignment> packageAssignments = resourceAssignmentRepository.findByPackageIdAndBookingIdIsNull(packageId);
        List<Long> resourceIds = packageAssignments.stream()
                .map(ResourceAssignment::getResourceId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();

        // Step 3b: Check if any assigned vehicle, guide, or room is already booked on these dates
        for (Long rId : resourceIds) {
            if (conflictDetectionService.hasConflict(rId, startDate, endDate)) {
                throw new ResourceConflictException("Resource conflict detected for selected travel dates (" + startDate + " to " + endDate + "). Please select alternative dates.");
            }
        }

        // Step 4: Calculate total price based on number of travelers
        BigDecimal totalPrice = pkg.getBasePrice().multiply(new BigDecimal(numberOfTravelers));

        // Step 5: Save the booking with PENDING_PAYMENT status
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

        // Step 6: Reserve each distinct resource for this booking
        for (Long rId : resourceIds) {
            availabilityService.reserveResource(rId, packageId, saved.getId(), startDate, endDate);
        }

        // Step 7: Return the saved booking
        return saved;
    }

    // Modifies dates, travelers, or requests for an existing booking and updates resource reservations
    @Transactional
    public Booking modifyBooking(Long bookingId, LocalDate startDate, LocalDate endDate, Integer numberOfTravelers, String specialRequests) {
        // Step 1: Find the booking and make sure it can still be modified
        Booking booking = getBookingById(bookingId);

        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT && booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new ValidationException("Cannot modify booking in " + booking.getStatus() + " status.");
        }

        // Step 2: Load the tour package to get pricing details
        Package pkg = packageRepository.findById(booking.getPackageId())
                .orElseThrow(() -> new ResourceNotFoundException("Package not found"));

        // Step 3: If dates changed, check for conflicts on the new dates
        if (startDate != null && endDate != null) {
            if (endDate.isBefore(startDate)) {
                throw new ValidationException("End date cannot be before start date.");
            }

            // Step 3a: Release current resource hold so it does not conflict with itself
            availabilityService.releaseAssignmentsForBooking(bookingId);

            // Step 3b: Verify no other reservations conflict with the new dates
            List<ResourceAssignment> packageAssignments = resourceAssignmentRepository.findByPackageIdAndBookingIdIsNull(booking.getPackageId());
            List<Long> resourceIds = packageAssignments.stream()
                    .map(ResourceAssignment::getResourceId)
                    .filter(java.util.Objects::nonNull)
                    .distinct()
                    .toList();

            for (Long rId : resourceIds) {
                if (conflictDetectionService.hasConflict(rId, startDate, endDate)) {
                    throw new ResourceConflictException("Resource conflict detected for new travel dates (" + startDate + " to " + endDate + ").");
                }
            }

            // Step 3c: Update booking with the new dates
            booking.setStartDate(startDate);
            booking.setEndDate(endDate);

            // Step 3d: Re-reserve resources for the new dates
            for (Long rId : resourceIds) {
                availabilityService.reserveResource(rId, booking.getPackageId(), booking.getId(), startDate, endDate);
            }
        }

        // Step 4: Update traveler count and recalculate total price if provided (guard against price drift on confirmed bookings)
        if (numberOfTravelers != null && !numberOfTravelers.equals(booking.getNumberOfTravelers())) {
            if (booking.getStatus() == BookingStatus.CONFIRMED) {
                throw new ValidationException("Traveler count cannot be modified on a paid/confirmed booking. Please contact staff.");
            }
            if (numberOfTravelers < 1) {
                throw new ValidationException("Number of travelers must be at least 1.");
            }
            booking.setNumberOfTravelers(numberOfTravelers);
            booking.setTotalPrice(pkg.getBasePrice().multiply(new BigDecimal(numberOfTravelers)));
        }

        // Step 5: Update special requests if provided
        if (specialRequests != null) {
            booking.setSpecialRequests(specialRequests);
        }

        // Step 6: Save and return the updated booking
        return bookingRepository.save(booking);
    }

    // Confirms a booking upon payment and locks in resource reservations
    @Transactional
    public Booking confirmBooking(Long bookingId) {
        // Step 1: Find the booking by ID
        Booking booking = getBookingById(bookingId);

        // Step 2: Set status to CONFIRMED and record confirmation time
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setConfirmedAt(LocalDateTime.now());

        // Step 3: Promote tentative resource reservations to confirmed
        availabilityService.confirmAssignmentsForBooking(bookingId);

        // Step 4: Save and return the confirmed booking
        return bookingRepository.save(booking);
    }

    // Transitions a confirmed booking to ONGOING when the tour commences
    @Transactional
    public Booking startTour(Long bookingId) {
        Booking booking = getBookingById(bookingId);
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new ValidationException("Only CONFIRMED bookings can be started.");
        }
        booking.setStatus(BookingStatus.ONGOING);
        return bookingRepository.save(booking);
    }

    // Completes an ongoing or confirmed tour, releases allocated resources, and unlocks customer review eligibility
    @Transactional
    public Booking completeBooking(Long bookingId) {
        Booking booking = getBookingById(bookingId);
        if (booking.getStatus() != BookingStatus.CONFIRMED && booking.getStatus() != BookingStatus.ONGOING) {
            throw new ValidationException("Only CONFIRMED or ONGOING bookings can be marked as COMPLETED.");
        }
        booking.setStatus(BookingStatus.COMPLETED);
        availabilityService.releaseAssignmentsForBooking(bookingId);
        return bookingRepository.save(booking);
    }

    // Automatically cancels pending bookings older than 24 hours to free up locked resources
    @Scheduled(cron = "0 0 * * * ?")
    public void autoCancelStaleBookings() {
        // Step 1: Find all bookings waiting for payment
        List<Booking> pending = bookingRepository.findByStatusOrderByCreatedAtDesc(BookingStatus.PENDING_PAYMENT);
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);

        // Step 2: Cancel bookings older than 24 hours
        for (Booking b : pending) {
            if (b.getCreatedAt().isBefore(cutoff)) {
                cancelBooking(b.getId());
            }
        }
    }

    // Cancels a booking, releases assigned resources, and either hard-deletes pending bookings or marks others as CANCELLED
    @Transactional
    public void cancelBooking(Long bookingId) {
        // Step 1: Find booking; exit if not found
        Optional<Booking> bookingOpt = bookingRepository.findById(bookingId);
        if (bookingOpt.isEmpty()) {
            return;
        }
        Booking booking = bookingOpt.get();

        // Step 2: Return early if already cancelled (idempotent)
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            return;
        }

        // Step 3: Do not allow cancelling an already completed booking
        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new ValidationException("Cannot cancel a completed booking.");
        }

        // Step 4: Release all reserved resources back to the pool
        availabilityService.releaseAssignmentsForBooking(bookingId);

        // Step 5: If booking is PENDING_PAYMENT, purge preliminary payment, revert trip request, and hard-delete booking
        if (booking.getStatus() == BookingStatus.PENDING_PAYMENT) {
            if (paymentRepository != null) {
                paymentRepository.findByBookingId(bookingId).ifPresent(paymentRepository::delete);
            }
            if (tripRequestRepository != null) {
                tripRequestRepository.findByBookingId(bookingId).ifPresent(req -> {
                    req.setStatus(TripRequestStatus.ACCEPTED);
                    tripRequestRepository.save(req);
                });
            }
            bookingRepository.delete(booking);
            return;
        }

        // Step 6: If booking is not in PENDING_PAYMENT (e.g. cancelled during a finance refund process for confirmed bookings):
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(LocalDateTime.now());
        bookingRepository.save(booking);

        // Step 7: If this confirmed booking was linked to a custom trip request, transition it to CANCELLED
        if (tripRequestRepository != null) {
            tripRequestRepository.findByBookingId(bookingId).ifPresent(req -> {
                req.setStatus(TripRequestStatus.CANCELLED);
                tripRequestRepository.save(req);
            });
        }
    }

    // Updates booking status and confirms or releases resources based on the new status
    @Transactional
    public Booking updateStatus(Long bookingId, BookingStatus newStatus) {
        // Step 1: Find booking by ID
        Booking booking = getBookingById(bookingId);

        // Step 2: Update the status
        booking.setStatus(newStatus);

        // Step 3: Handle status change side effects
        if (newStatus == BookingStatus.CONFIRMED && booking.getConfirmedAt() == null) {
            booking.setConfirmedAt(LocalDateTime.now());
            availabilityService.confirmAssignmentsForBooking(bookingId);
        } else if (newStatus == BookingStatus.CANCELLED && booking.getCancelledAt() == null) {
            booking.setCancelledAt(LocalDateTime.now());
            availabilityService.releaseAssignmentsForBooking(bookingId);
        }

        // Step 4: Save and return the updated booking
        return bookingRepository.save(booking);
    }

    // Permanently deletes a booking (only allowed if the booking is already cancelled)
    @Transactional
    public void deleteBooking(Long bookingId) {
        // Step 1: Find booking; exit if not found
        Optional<Booking> bookingOpt = bookingRepository.findById(bookingId);
        if (bookingOpt.isEmpty()) {
            return;
        }
        Booking booking = bookingOpt.get();

        // Step 2: Ensure only cancelled bookings can be deleted
        if (booking.getStatus() != BookingStatus.CANCELLED) {
            throw new ValidationException("Cannot delete active booking. Booking must be cancelled first.");
        }

        // Step 3: Release any remaining resource assignments
        availabilityService.releaseAssignmentsForBooking(bookingId);

        // Step 4: Remove any associated payment records
        if (paymentRepository != null) {
            paymentRepository.findByBookingId(bookingId).ifPresent(paymentRepository::delete);
        }

        // Step 5: Permanently delete the booking from the database
        bookingRepository.delete(booking);
    }

    // Archives a booking, hiding it from the customer's active booking history
    @Transactional
    public void archiveBooking(Long bookingId) {
        Booking booking = getBookingById(bookingId);
        booking.setArchived(true);
        booking.setArchivedAt(LocalDateTime.now());
        bookingRepository.save(booking);
    }
}
