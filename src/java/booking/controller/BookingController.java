package com.islandtrails.booking.controller;

import com.islandtrails.auth.entity.User;
import com.islandtrails.auth.service.PermissionService;
import com.islandtrails.booking.dto.BookingFormDTO;
import com.islandtrails.booking.entity.Booking;
import com.islandtrails.booking.service.BookingService;
import com.islandtrails.catalog.entity.Package;
import com.islandtrails.catalog.service.PackageService;
import com.islandtrails.common.exception.ResourceConflictException;
import com.islandtrails.common.exception.ValidationException;
import com.islandtrails.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
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

    public BookingController(BookingService bookingService,
                             PackageService packageService,
                             PaymentService paymentService,
                             PermissionService permissionService) {
        this.bookingService = bookingService;
        this.packageService = packageService;
        this.paymentService = paymentService;
        this.permissionService = permissionService;
    }

    @GetMapping("/customer/booking/new")
    public String newBookingForm(@RequestParam("packageId") Long packageId, Model model) {
        Package pkg = packageService.getPackageById(packageId);
        BookingFormDTO dto = new BookingFormDTO();
        dto.setPackageId(pkg.getId());
        dto.setStartDate(LocalDate.now().plusDays(7));
        dto.setEndDate(LocalDate.now().plusDays(7 + (pkg.getDurationDays() != null ? pkg.getDurationDays() : 3)));

        model.addAttribute("pkg", pkg);
        model.addAttribute("bookingDTO", dto);
        return "booking/create-booking";
    }

    @PostMapping("/customer/booking")
    public String createBooking(@Valid @ModelAttribute("bookingDTO") BookingFormDTO dto,
                                BindingResult bindingResult,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        Package pkg = dto.getPackageId() != null ? packageService.getPackageById(dto.getPackageId()) : null;

        if (dto.getStartDate() != null && dto.getEndDate() != null) {
            if (dto.getEndDate().isBefore(dto.getStartDate())) {
                bindingResult.rejectValue("endDate", "error.endDate", "End date must be on or after start date.");
            }
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("pkg", pkg);
            return "booking/create-booking";
        }

        User customer = permissionService.getCurrentUser().orElseThrow();
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

            redirectAttributes.addFlashAttribute("successMessage", "Booking created successfully! Please complete payment to confirm your reservation.");
            return "redirect:/customer/booking/" + booking.getId();
        } catch (ResourceConflictException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("pkg", pkg);
            return "booking/create-booking";
        } catch (ValidationException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("pkg", pkg);
            return "booking/create-booking";
        }
    }

    @GetMapping("/customer/booking/{id}")
    public String bookingDetail(@PathVariable("id") Long id, Model model) {
        Booking booking = bookingService.getBookingById(id);
        Package pkg = packageService.getPackageById(booking.getPackageId());
        model.addAttribute("booking", booking);
        model.addAttribute("pkg", pkg);
        model.addAttribute("payment", paymentService.getPaymentByBooking(id).orElse(null));
        return "booking/booking-detail";
    }

    @GetMapping("/customer/bookings")
    public String customerBookings(Model model) {
        User customer = permissionService.getCurrentUser().orElseThrow();
        List<Booking> bookings = bookingService.getBookingsByCustomer(customer.getId());
        model.addAttribute("bookings", bookings);
        return "booking/customer-bookings";
    }

    @PostMapping("/customer/booking/{id}/cancel")
    public String cancelBooking(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        bookingService.cancelBooking(id);
        redirectAttributes.addFlashAttribute("infoMessage", "Booking has been cancelled and reserved resources released.");
        return "redirect:/customer/bookings";
    }

    @PostMapping("/customer/booking/{id}/modify")
    public String modifyBooking(@PathVariable("id") Long id,
                                @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                @RequestParam(value = "numberOfTravelers", required = false) Integer travelers,
                                @RequestParam(value = "specialRequests", required = false) String specialRequests,
                                RedirectAttributes redirectAttributes) {
        try {
            bookingService.modifyBooking(id, startDate, endDate, travelers, specialRequests);
            redirectAttributes.addFlashAttribute("successMessage", "Booking modified successfully.");
        } catch (ResourceConflictException | ValidationException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return "redirect:/customer/booking/" + id;
    }
}
