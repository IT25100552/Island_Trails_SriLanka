package com.islandtrails.catalog.dto;

import java.math.BigDecimal;

public class PackageSearchDTO {

    private String keyword;
    private String destination;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Integer maxDuration;

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public BigDecimal getMinPrice() { return minPrice; }
    public void setMinPrice(BigDecimal minPrice) { this.minPrice = minPrice; }

    public BigDecimal getMaxPrice() { return maxPrice; }
    public void setMaxPrice(BigDecimal maxPrice) { this.maxPrice = maxPrice; }

    public Integer getMaxDuration() { return maxDuration; }
    public void setMaxDuration(Integer maxDuration) { this.maxDuration = maxDuration; }
}
