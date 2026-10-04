package com.islandtrails.tripplanning.entity;

import com.islandtrails.common.entity.BaseEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quotations")
public class Quotation extends BaseEntity {

    @Column(name = "trip_request_id", nullable = false)
    private Long tripRequestId;

    @Column(name = "consultant_id", nullable = false)
    private Long consultantId;

    @Column(name = "consultant_name")
    private String consultantName;

    @Column(name = "quotation_version", nullable = false)
    private Integer quotationVersion = 1;

    @Column(name = "base_subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal baseSubtotal = BigDecimal.ZERO;

    @Column(name = "profit_margin_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal profitMarginPercent = new BigDecimal("15.00");

    @Column(name = "total_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalCost = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private QuotationStatus status = QuotationStatus.DRAFT;

    @Column(name = "itinerary_notes", columnDefinition = "TEXT")
    private String itineraryNotes;

    @OneToMany(mappedBy = "quotation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuotationLine> lines = new ArrayList<>();

    public Quotation() {
    }

    public Quotation(Long tripRequestId, Long consultantId, String consultantName, Integer quotationVersion, BigDecimal profitMarginPercent) {
        this.tripRequestId = tripRequestId;
        this.consultantId = consultantId;
        this.consultantName = consultantName;
        this.quotationVersion = quotationVersion != null ? quotationVersion : 1;
        this.profitMarginPercent = profitMarginPercent != null ? profitMarginPercent : new BigDecimal("15.00");
        this.status = QuotationStatus.DRAFT;
    }

    public Long getTripRequestId() {
        return tripRequestId;
    }

    public void setTripRequestId(Long tripRequestId) {
        this.tripRequestId = tripRequestId;
    }

    public Long getConsultantId() {
        return consultantId;
    }

    public void setConsultantId(Long consultantId) {
        this.consultantId = consultantId;
    }

    public String getConsultantName() {
        return consultantName;
    }

    public void setConsultantName(String consultantName) {
        this.consultantName = consultantName;
    }

    public Integer getQuotationVersion() {
        return quotationVersion;
    }

    public void setQuotationVersion(Integer quotationVersion) {
        this.quotationVersion = quotationVersion;
    }

    public BigDecimal getBaseSubtotal() {
        return baseSubtotal;
    }

    public void setBaseSubtotal(BigDecimal baseSubtotal) {
        this.baseSubtotal = baseSubtotal;
    }

    public BigDecimal getProfitMarginPercent() {
        return profitMarginPercent;
    }

    public void setProfitMarginPercent(BigDecimal profitMarginPercent) {
        this.profitMarginPercent = profitMarginPercent;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(BigDecimal totalCost) {
        this.totalCost = totalCost;
    }

    public QuotationStatus getStatus() {
        return status;
    }

    public void setStatus(QuotationStatus status) {
        this.status = status;
    }

    public String getItineraryNotes() {
        return itineraryNotes;
    }

    public void setItineraryNotes(String itineraryNotes) {
        this.itineraryNotes = itineraryNotes;
    }

    public List<QuotationLine> getLines() {
        return lines;
    }

    public void setLines(List<QuotationLine> lines) {
        this.lines = lines;
    }

    public void addLine(QuotationLine line) {
        lines.add(line);
        line.setQuotation(this);
    }

    public void removeLine(QuotationLine line) {
        lines.remove(line);
        line.setQuotation(null);
    }
}
