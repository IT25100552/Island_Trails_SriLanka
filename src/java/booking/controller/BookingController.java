package com.islandtrails.booking.controller;

import com.islandtrails.auth.entity.User;
import com.islandtrails.auth.entity.UserRole;
import com.islandtrails.auth.service.PermissionService;
import com.islandtrails.booking.dto.BookingFormDTO;
import com.islandtrails.booking.entity.Booking;
import com.islandtrails.booking.entity.BookingStatus;
import com.islandtrails.booking.service.BookingService;
import com.islandtrails.catalog.entity.Package;
import com.islandtrails.catalog.service.PackageService;
import com.islandtrails.common.exception.ResourceConflictException;
import com.islandtrails.common.exception.ValidationException;
import com.islandtrails.payment.entity.PaymentMethod;
import com.islandtrails.payment.entity.RefundStatus;
import com.islandtrails.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
public class BookingController {

    private final BookingService bookingService;
    private final PackageService packageService;
    private final PaymentService paymentService;
    private final PermissionService permissionService;
    private final com.islandtrails.payment.service.RefundService refundService;

    // Injects required services for booking flows
    public BookingController(BookingService bookingService,
                             PackageService packageService,
                             PaymentService paymentService,
                             PermissionService permissionService,
                             com.islandtrails.payment.service.RefundService refundService) {
        this.bookingService = bookingService;
        this.packageService = packageService;
        this.paymentService = paymentService;
        this.permissionService = permissionService;
        this.refundService = refundService;
    }

    // Displays the booking form with pre-filled default dates for the selected package
    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping("/customer/booking/new")
    public String newBookingForm(@RequestParam("packageId") Long packageId, Model model) {
        // Step 1: Retrieve package details by ID
        Package pkg = packageService.getPackageById(packageId);

        // Step 2: Initialize DTO with default dates (starting 7 days from now)
        BookingFormDTO dto = new BookingFormDTO();
        dto.setPackageId(pkg.getId());
        dto.setStartDate(LocalDate.now().plusDays(7));
        dto.setEndDate(LocalDate.now().plusDays(7 + (pkg.getDurationDays() != null ? pkg.getDurationDays() : 3)));

        // Step 3: Populate model and show booking creation view
        model.addAttribute("pkg", pkg);
        model.addAttribute("bookingDTO", dto);
        return "booking/create-booking";
    }

    // Handles form submission, validates dates, creates the booking, and redirects to payment
    @PreAuthorize("hasRole('CUSTOMER')")
    @PostMapping("/customer/booking")
    public String createBooking(@Valid @ModelAttribute("bookingDTO") BookingFormDTO dto,
                                BindingResult bindingResult,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        // Step 1: Look up package details for potential error view re-rendering
        Package pkg = dto.getPackageId() != null ? packageService.getPackageById(dto.getPackageId()) : null;

        // Step 2: Check that end date is not before start date
        if (dto.getStartDate() != null && dto.getEndDate() != null) {
            if (dto.getEndDate().isBefore(dto.getStartDate())) {
                bindingResult.rejectValue("endDate", "error.endDate", "End date must be on or after start date.");
            }
        }

        // Step 3: If validation errors exist, stay on the form
        if (bindingResult.hasErrors()) {
            model.addAttribute("pkg", pkg);
            return "booking/create-booking";
        }

        // Step 4: Get authenticated customer
        User customer = permissionService.getCurrentUser().orElseThrow();

        // Step 5: Create booking and catch conflict or validation errors
        try {
            Booking booking = bookingService.createBooking(
                    customer.getId(),
                    customer.getName(),
                    dto.getPackageId(),
                    dto.getStartDate(),
                    dto.getEndDate(),
                    dto.getNumberOfTravelers(),
                    dto.getSpecialRequests()
            );

            // Step 6: Redirect to payment checkout page
            redirectAttributes.addFlashAttribute("successMessage", "Booking created successfully! Please complete payment to confirm your reservation.");
            return "redirect:/customer/booking/" + booking.getId() + "/payment";
        } catch (ResourceConflictException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("pkg", pkg);
            return "booking/create-booking";
        } catch (ValidationException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("pkg", pkg);
            return "booking/create-booking";
        } catch (DataIntegrityViolationException ex) {
            String msg = "A scheduling conflict occurred: One or more selected resources are already reserved for these dates. Please choose different travel dates.";
            if (ex.getMessage() != null && ex.getMessage().contains("Scheduling Conflict")) {
                msg = "Scheduling Conflict: The specified resource is already booked for overlapping dates. Please select alternative dates.";
            }
            model.addAttribute("errorMessage", msg);
            model.addAttribute("pkg", pkg);
            return "booking/create-booking";
        }
    }

    // Displays booking details, package info, and payment status for an authorized customer
    @GetMapping({"/customer/booking/{id}", "/booking/{id}"})
    public String bookingDetail(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        // Step 1: Fetch booking by ID
        Booking booking = bookingService.getBookingById(id);

        // Step 2: Ensure a customer can only view their own booking
        User user = permissionService.getCurrentUser().orElseThrow();
        if (user.getRole() == UserRole.CUSTOMER && !booking.getCustomerId().equals(user.getId())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Access denied: You do not have permission to view this booking.");
            return "redirect:/customer/bookings";
        }

        // Step 3: Check package details and refund request state
        Package pkg = booking.getPackageId() != null ? packageService.getPackageById(booking.getPackageId()) : null;
        var customerRefunds = refundService.getRefundsByCustomer(booking.getCustomerId());
        boolean refundRequested = customerRefunds.stream()
                .anyMatch(r -> r.getBookingId().equals(id) && r.getStatus() == RefundStatus.REQUESTED);
        boolean refundApproved = customerRefunds.stream()
                .anyMatch(r -> r.getBookingId().equals(id) && r.getStatus() == RefundStatus.APPROVED);
        boolean refundRejected = customerRefunds.stream()
                .anyMatch(r -> r.getBookingId().equals(id) && r.getStatus() == RefundStatus.REJECTED && !refundRequested && !refundApproved);

        // Step 4: Populate model and render detail view
        model.addAttribute("booking", booking);
        model.addAttribute("pkg", pkg);
        model.addAttribute("payment", paymentService.getPaymentByBooking(id).orElse(null));
        model.addAttribute("paymentMethods", PaymentMethod.values());
        model.addAttribute("refundRequested", refundRequested);
        model.addAttribute("refundApproved", refundApproved);
        model.addAttribute("refundRejected", refundRejected);
        return "booking/booking-detail";
    }

    // Displays all bookings placed by the currently logged-in customer
    @GetMapping("/customer/bookings")
    public String customerBookings(Model model) {
        // Step 1: Identify current authenticated customer
        User customer = permissionService.getCurrentUser().orElseThrow();

        // Step 2: Query bookings belonging to customer
        List<Booking> bookings = bookingService.getBookingsByCustomer(customer.getId());

        // Step 3: Populate model and render bookings list view
        model.addAttribute("bookings", bookings);
        return "booking/customer-bookings";
    }

    // Cancels a booking and releases reserved resources
    @PostMapping({"/customer/booking/{id}/cancel", "/booking/{id}/cancel"})
    public String cancelBooking(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        // Step 1: Ensure customer owns this booking before cancelling (only customer can cancel)
        Booking booking = bookingService.getBookingById(id);
        User user = permissionService.getCurrentUser().orElseThrow();
        if (user.getRole() != UserRole.CUSTOMER || !booking.getCustomerId().equals(user.getId())) {
            if (user.getRole() != UserRole.CUSTOMER) {
                redirectAttributes.addFlashAttribute("errorMessage", "Access denied: Only the customer who placed this booking can cancel it.");
                return "redirect:/dashboard";
            }
            redirectAttributes.addFlashAttribute("errorMessage", "Access denied: You do not have permission to cancel this booking.");
            return "redirect:/customer/bookings";
        }

        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            redirectAttributes.addFlashAttribute("errorMessage", "Confirmed bookings cannot be directly cancelled. Please request cancellation and refund via the Payment tab.");
            return "redirect:/customer/bookings";
        }

        // Step 2: Cancel booking and release resources
        bookingService.cancelBooking(id);
        redirectAttributes.addFlashAttribute("infoMessage", "Booking #" + id + " has been cancelled and removed.");

        // Step 3: Return to customer bookings list
        return "redirect:/customer/bookings";
    }

    // Modifies travel dates, passenger count, or special requests of an active booking (only customer can modify)
    @PostMapping({"/customer/booking/{id}/modify", "/customer/booking/{id}/edit", "/booking/{id}/modify", "/booking/{id}/edit"})
    public String modifyBooking(@PathVariable("id") Long id,
                                @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                @RequestParam(value = "numberOfTravelers", required = false) Integer travelers,
                                @RequestParam(value = "specialRequests", required = false) String specialRequests,
                                RedirectAttributes redirectAttributes) {
        // Step 1: Verify customer owns the booking
        Booking booking = bookingService.getBookingById(id);
        User user = permissionService.getCurrentUser().orElseThrow();
        if (user.getRole() != UserRole.CUSTOMER || !booking.getCustomerId().equals(user.getId())) {
            if (user.getRole() != UserRole.CUSTOMER) {
                redirectAttributes.addFlashAttribute("errorMessage", "Access denied: Only the customer who placed this booking can modify it.");
                return "redirect:/dashboard";
            }
            redirectAttributes.addFlashAttribute("errorMessage", "Access denied: You do not have permission to modify this booking.");
            return "redirect:/customer/bookings";
        }

        // Step 2: Apply modifications through service
        try {
            bookingService.modifyBooking(id, startDate, endDate, travelers, specialRequests);
            redirectAttributes.addFlashAttribute("successMessage", "Booking modified successfully.");
        } catch (ResourceConflictException | ValidationException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (DataIntegrityViolationException ex) {
            String msg = "A scheduling conflict occurred: One or more selected resources are already reserved for these dates. Please choose different travel dates.";
            if (ex.getMessage() != null && ex.getMessage().contains("Scheduling Conflict")) {
                msg = "Scheduling Conflict: The specified resource is already booked for overlapping dates. Please select alternative dates.";
            }
            redirectAttributes.addFlashAttribute("errorMessage", msg);
        }

        // Step 3: Redirect to booking detail page
        return "redirect:/customer/booking/" + id;
    }

    // Displays the booking modification form populated with current values (only customer can edit)
    @GetMapping({"/customer/booking/{id}/edit", "/booking/{id}/edit"})
    public String editBookingForm(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        // Step 1: Verify customer owns the booking
        Booking booking = bookingService.getBookingById(id);
        User user = permissionService.getCurrentUser().orElseThrow();
        if (user.getRole() != UserRole.CUSTOMER || !booking.getCustomerId().equals(user.getId())) {
            if (user.getRole() != UserRole.CUSTOMER) {
                redirectAttributes.addFlashAttribute("errorMessage", "Access denied: Only the customer who placed this booking can edit it.");
                return "redirect:/dashboard";
            }
            redirectAttributes.addFlashAttribute("errorMessage", "Access denied: You do not have permission to edit this booking.");
            return "redirect:/customer/bookings";
        }

        // Step 2: Fetch associated package information
        Package pkg = booking.getPackageId() != null ? packageService.getPackageById(booking.getPackageId()) : null;

        // Step 3: Populate model and render edit form
        model.addAttribute("booking", booking);
        model.addAttribute("pkg", pkg);
        return "booking/edit-booking";
    }

    // Permanently deletes a cancelled booking (staff or customer owner)
    @PostMapping("/booking/{id}/delete")
    public String deleteBooking(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        // Step 1: Identify current user and fetch booking
        User user = permissionService.getCurrentUser().orElseThrow();
        Booking booking = bookingService.getBookingById(id);

        // Step 2: Check role permissions (staff or customer owner)
        boolean isStaff = user.getRole() == UserRole.TOUR_OPS_MANAGER || user.getRole() == UserRole.IT_SYSTEMS_OFFICER;
        boolean isOwnerCustomer = user.getRole() == UserRole.CUSTOMER && booking.getCustomerId().equals(user.getId());

        if (!isStaff && !isOwnerCustomer) {
            redirectAttributes.addFlashAttribute("errorMessage", "Access denied: You do not have permission to delete this booking.");
            if (user.getRole() == UserRole.CUSTOMER) {
                return "redirect:/customer/bookings";
            }
            return "redirect:/dashboard";
        }

        // Step 3: Execute permanent deletion via service
        try {
            bookingService.deleteBooking(id);
            redirectAttributes.addFlashAttribute("successMessage", "Booking #" + id + " has been permanently deleted.");
        } catch (ValidationException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        // Step 4: Route user to appropriate destination
        if (isStaff) {
            return "redirect:/staff/tour-ops/all-bookings";
        }
        return "redirect:/customer/bookings";
    }

    // Archives a cancelled booking (customer owner or staff)
    @PostMapping({"/customer/booking/{id}/archive", "/booking/{id}/archive"})
    public String archiveBooking(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        User user = permissionService.getCurrentUser().orElseThrow();
        Booking booking = bookingService.getBookingById(id);

        boolean isStaff = user.getRole() == UserRole.TOUR_OPS_MANAGER || user.getRole() == UserRole.IT_SYSTEMS_OFFICER;
        boolean isOwnerCustomer = user.getRole() == UserRole.CUSTOMER && booking.getCustomerId().equals(user.getId());

        if (!isStaff && !isOwnerCustomer) {
            redirectAttributes.addFlashAttribute("errorMessage", "Access denied: You do not have permission to archive this booking.");
            if (user.getRole() == UserRole.CUSTOMER) {
                return "redirect:/customer/bookings";
            }
            return "redirect:/dashboard";
        }

        bookingService.archiveBooking(id);
        redirectAttributes.addFlashAttribute("successMessage", "Booking #" + id + " has been archived.");

        if (isStaff) {
            return "redirect:/staff/tour-ops/all-bookings";
        }
        return "redirect:/customer/bookings";
    }

    // Starts tour operations for a confirmed booking
    @PostMapping("/staff/tour-ops/booking/{id}/start")
    public String startTour(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        bookingService.startTour(id);
        redirectAttributes.addFlashAttribute("successMessage", "Booking #" + id + " tour started (ONGOING).");
        return "redirect:/staff/tour-ops/all-bookings";
    }

    // Marks an ongoing or confirmed booking as completed, unlocking customer reviews
    @PostMapping("/staff/tour-ops/booking/{id}/complete")
    public String completeBooking(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        bookingService.completeBooking(id);
        redirectAttributes.addFlashAttribute("successMessage", "Booking #" + id + " marked as COMPLETED. Customer review unlocked.");
        return "redirect:/staff/tour-ops/all-bookings";
    }
}
