package com.islandtrails.tripplanning.entity;

import com.islandtrails.common.entity.BaseEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "quotation_lines")
public class QuotationLine extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quotation_id", nullable = false)
    private Quotation quotation;

    @Column(name = "resource_id", nullable = false)
    private Long resourceId;

    @Column(name = "resource_name")
    private String resourceName;

    @Column(name = "resource_type", length = 30)
    private String resourceType;

    @Column(nullable = false)
    private Integer quantity = 1;

    @Column(name = "unit_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitCost;

    @Column(name = "line_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal lineCost;

    @Column(nullable = false)
    private Integer sequence = 1; // day 1, day 2, etc.

    @Column(name = "activity_description", length = 255)
    private String activityDescription;

    public QuotationLine() {
    }

    public QuotationLine(Long resourceId, String resourceName, String resourceType, Integer quantity, BigDecimal unitCost, Integer sequence, String activityDescription) {
        this.resourceId = resourceId;
        this.resourceName = resourceName;
        this.resourceType = resourceType;
        this.quantity = quantity != null ? quantity : 1;
        this.unitCost = unitCost != null ? unitCost : BigDecimal.ZERO;
        this.lineCost = this.unitCost.multiply(new BigDecimal(this.quantity));
        this.sequence = sequence != null ? sequence : 1;
        this.activityDescription = activityDescription;
    }

    public Quotation getQuotation() {
        return quotation;
    }

    public void setQuotation(Quotation quotation) {
        this.quotation = quotation;
    }

    public Long getResourceId() {
        return resourceId;
    }

    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
        if (this.unitCost != null && this.quantity != null) {
            this.lineCost = this.unitCost.multiply(new BigDecimal(this.quantity));
        }
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    public void setUnitCost(BigDecimal unitCost) {
        this.unitCost = unitCost;
        if (this.unitCost != null && this.quantity != null) {
            this.lineCost = this.unitCost.multiply(new BigDecimal(this.quantity));
        }
    }

    public BigDecimal getLineCost() {
        return lineCost;
    }

    public void setLineCost(BigDecimal lineCost) {
        this.lineCost = lineCost;
    }

    public Integer getSequence() {
        return sequence;
    }

    public void setSequence(Integer sequence) {
        this.sequence = sequence;
    }

    public String getActivityDescription() {
        return activityDescription;
    }

    public void setActivityDescription(String activityDescription) {
        this.activityDescription = activityDescription;
    }
}
