package com.islandtrails.tripplanning.service;

import com.islandtrails.auth.service.SystemConfigService;
import com.islandtrails.booking.entity.Booking;
import com.islandtrails.booking.entity.BookingStatus;
import com.islandtrails.booking.repository.BookingRepository;
import com.islandtrails.catalog.entity.Package;
import com.islandtrails.catalog.entity.PackageOrigin;
import com.islandtrails.catalog.entity.PackageStatus;
import com.islandtrails.catalog.repository.PackageRepository;
import com.islandtrails.common.exception.ResourceNotFoundException;
import com.islandtrails.common.exception.ValidationException;
import com.islandtrails.common.util.CostCalculationUtil;
import com.islandtrails.tripplanning.entity.*;
import com.islandtrails.tripplanning.repository.QuotationLineRepository;
import com.islandtrails.tripplanning.repository.QuotationRepository;
import com.islandtrails.tripplanning.repository.TripRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
public class QuotationService {

    private final QuotationRepository quotationRepository;
    private final QuotationLineRepository quotationLineRepository;
    private final TripRequestRepository tripRequestRepository;
    private final PackageRepository packageRepository;
    private final BookingRepository bookingRepository;
    private final SystemConfigService systemConfigService;

    public QuotationService(QuotationRepository quotationRepository,
                            QuotationLineRepository quotationLineRepository,
                            TripRequestRepository tripRequestRepository,
                            PackageRepository packageRepository,
                            BookingRepository bookingRepository,
                            SystemConfigService systemConfigService) {
        this.quotationRepository = quotationRepository;
        this.quotationLineRepository = quotationLineRepository;
        this.tripRequestRepository = tripRequestRepository;
        this.packageRepository = packageRepository;
        this.bookingRepository = bookingRepository;
        this.systemConfigService = systemConfigService;
    }

    public List<Quotation> getQuotationsForTripRequest(Long tripRequestId) {
        return quotationRepository.findByTripRequestIdOrderByQuotationVersionDesc(tripRequestId);
    }

    public Optional<Quotation> getLatestQuotation(Long tripRequestId) {
        return quotationRepository.findFirstByTripRequestIdOrderByQuotationVersionDesc(tripRequestId);
    }

    public Quotation getQuotationById(Long id) {
        return quotationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Quotation not found with id: " + id));
    }

    public List<Quotation> getQuotationsByConsultant(Long consultantId) {
        return quotationRepository.findByConsultantIdOrderByCreatedAtDesc(consultantId);
    }

    public List<Quotation> getQuotationsByStatus(QuotationStatus status) {
        return quotationRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    @Transactional
    public Quotation createQuotation(Long tripRequestId, Long consultantId, String consultantName, BigDecimal profitMarginPercent, List<QuotationLine> lines, String itineraryNotes) {
        TripRequest request = tripRequestRepository.findById(tripRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip request not found with id: " + tripRequestId));

        BigDecimal margin = profitMarginPercent != null ? profitMarginPercent : systemConfigService.getProfitMarginPercentage();
        Quotation quotation = new Quotation(tripRequestId, consultantId, consultantName, 1, margin);
        quotation.setItineraryNotes(itineraryNotes);

        BigDecimal baseSubtotal = BigDecimal.ZERO;
        if (lines != null) {
            for (QuotationLine line : lines) {
                quotation.addLine(line);
                baseSubtotal = baseSubtotal.add(line.getLineCost());
            }
        }

        quotation.setBaseSubtotal(baseSubtotal);
        quotation.setTotalCost(CostCalculationUtil.calculateTotalCost(baseSubtotal, margin));
        quotation.setStatus(QuotationStatus.SENT);

        request.setStatus(TripRequestStatus.QUOTED);
        tripRequestRepository.save(request);

        return quotationRepository.save(quotation);
    }

    @Transactional
    public Quotation reviseQuotation(Long previousQuotationId, Long consultantId, String consultantName, BigDecimal profitMarginPercent, List<QuotationLine> newLines, String itineraryNotes) {
        Quotation previous = getQuotationById(previousQuotationId);
        previous.setStatus(QuotationStatus.SUPERSEDED);
        quotationRepository.save(previous);

        int nextVersion = previous.getQuotationVersion() + 1;
        BigDecimal margin = profitMarginPercent != null ? profitMarginPercent : previous.getProfitMarginPercent();

        Quotation revision = new Quotation(previous.getTripRequestId(), consultantId, consultantName, nextVersion, margin);
        revision.setItineraryNotes(itineraryNotes != null ? itineraryNotes : previous.getItineraryNotes());

        BigDecimal baseSubtotal = BigDecimal.ZERO;
        if (newLines != null) {
            for (QuotationLine line : newLines) {
                revision.addLine(line);
                baseSubtotal = baseSubtotal.add(line.getLineCost());
            }
        }

        revision.setBaseSubtotal(baseSubtotal);
        revision.setTotalCost(CostCalculationUtil.calculateTotalCost(baseSubtotal, margin));
        revision.setStatus(QuotationStatus.SENT);

        return quotationRepository.save(revision);
    }

    @Transactional
    public Quotation acceptQuotation(Long quotationId) {
        Quotation quotation = getQuotationById(quotationId);
        if (quotation.getStatus() != QuotationStatus.SENT) {
            throw new ValidationException("Only SENT quotations can be accepted.");
        }

        quotation.setStatus(QuotationStatus.APPROVED);
        Quotation saved = quotationRepository.save(quotation);

        TripRequest request = tripRequestRepository.findById(quotation.getTripRequestId())
                .orElseThrow(() -> new ResourceNotFoundException("Trip request not found"));
        request.setStatus(TripRequestStatus.ACCEPTED);
        tripRequestRepository.save(request);

        return saved;
    }

    @Transactional
    public Quotation rejectQuotation(Long quotationId, String reason) {
        Quotation quotation = getQuotationById(quotationId);
        quotation.setStatus(QuotationStatus.REJECTED);
        return quotationRepository.save(quotation);
    }

    @Transactional
    public Booking convertQuotationToBooking(Long quotationId) {
        Quotation quotation = getQuotationById(quotationId);
        if (quotation.getStatus() != QuotationStatus.APPROVED) {
            throw new ValidationException("Only APPROVED quotations can be converted to bookings.");
        }

        TripRequest request = tripRequestRepository.findById(quotation.getTripRequestId())
                .orElseThrow(() -> new ResourceNotFoundException("Trip request not found"));

        int durationDays = (int) ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1;
        if (durationDays <= 0) durationDays = 1;

        // Step 1: Auto-create Package with origin = FROM_QUOTATION
        String packageName = "Custom Tour: " + request.getCustomerName() + " (" + request.getStartDate() + ")";
        Package pkg = new Package(
                packageName,
                request.getInterests() != null && !request.getInterests().isBlank() ? request.getInterests() : "Sri Lanka Custom Tour",
                quotation.getItineraryNotes() != null ? quotation.getItineraryNotes() : "Customized quotation itinerary.",
                quotation.getTotalCost(),
                durationDays,
                PackageStatus.PUBLISHED,
                PackageOrigin.FROM_QUOTATION,
                quotation.getConsultantId()
        );
        Package savedPkg = packageRepository.save(pkg);

        // Step 2: Create Booking in PENDING_PAYMENT status
        Booking booking = new Booking(
                request.getCustomerId(),
                request.getCustomerName(),
                savedPkg.getId(),
                savedPkg.getName(),
                request.getStartDate(),
                request.getEndDate(),
                quotation.getTotalCost(),
                request.getNumberOfTravelers(),
                request.getSpecialRequirements()
        );
        booking.setStatus(BookingStatus.PENDING_PAYMENT);
        Booking savedBooking = bookingRepository.save(booking);

        // Step 3: Update Quotation and TripRequest statuses
        quotation.setStatus(QuotationStatus.CONVERTED);
        quotationRepository.save(quotation);

        request.setStatus(TripRequestStatus.CONVERTED_TO_BOOKING);
        tripRequestRepository.save(request);

        return savedBooking;
    }
}
