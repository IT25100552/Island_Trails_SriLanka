package com.islandtrails.resource.dto;

import com.islandtrails.resource.entity.Resource;
import com.islandtrails.resource.entity.ResourceStatus;
import com.islandtrails.resource.entity.ResourceType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Data Transfer Object for creating and editing operational resources
 * with Bean Validation rules.
 */
public class ResourceFormDTO {

    private Long id;

    @NotBlank(message = "Resource name is required.")
    @Size(max = 150, message = "Name cannot exceed 150 characters.")
    private String name;

    @NotNull(message = "Resource type is required.")
    private ResourceType resourceType;

    @NotNull(message = "Unit cost is required.")
    @DecimalMin(value = "0.0", inclusive = true, message = "Unit cost cannot be negative.")
    private BigDecimal unitCost;

    @Min(value = 1, message = "Capacity must be at least 1.")
    private Integer capacity = 1;

    private String plateNumber;
    private String vehicleModel;
    private String languages;
    private String certification;
    private String contactNumber;
    private String hotelName;
    private String roomNumber;
    private String status = "ACTIVE";

    public ResourceFormDTO() {
    }

    public Resource toEntity() {
        Resource r = new Resource();
        r.setId(this.id);
        r.setName(this.name);
        r.setResourceType(this.resourceType);
        r.setUnitCost(this.unitCost);
        if (this.status != null) {
            try {
                r.setStatus(ResourceStatus.valueOf(this.status));
            } catch (IllegalArgumentException e) {
                r.setStatus(ResourceStatus.ACTIVE);
            }
        } else {
            r.setStatus(ResourceStatus.ACTIVE);
        }
        r.setPlateNumber(this.plateNumber);
        r.setVehicleModel(this.vehicleModel);
        r.setLanguages(this.languages);
        r.setCertification(this.certification);
        r.setContactNumber(this.contactNumber);
        r.setHotelName(this.hotelName);
        r.setRoomNumber(this.roomNumber);
        if (this.resourceType == ResourceType.VEHICLE) {
            r.setSeatCapacity(this.capacity);
        } else if (this.resourceType == ResourceType.HOTEL_ROOM) {
            r.setBedCapacity(this.capacity);
        }
        return r;
    }

    public static ResourceFormDTO fromEntity(Resource r) {
        if (r == null) {
            return null;
        }
        ResourceFormDTO dto = new ResourceFormDTO();
        dto.setId(r.getId());
        dto.setName(r.getName());
        dto.setResourceType(r.getResourceType());
        dto.setUnitCost(r.getUnitCost());
        dto.setStatus(r.getStatus() != null ? r.getStatus().name() : "ACTIVE");
        dto.setPlateNumber(r.getPlateNumber());
        dto.setVehicleModel(r.getVehicleModel());
        dto.setLanguages(r.getLanguages());
        dto.setCertification(r.getCertification());
        dto.setContactNumber(r.getContactNumber());
        dto.setHotelName(r.getHotelName());
        dto.setRoomNumber(r.getRoomNumber());
        if (r.getSeatCapacity() != null) {
            dto.setCapacity(r.getSeatCapacity());
        } else if (r.getBedCapacity() != null) {
            dto.setCapacity(r.getBedCapacity());
        }
        return dto;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    public void setUnitCost(BigDecimal unitCost) {
        this.unitCost = unitCost;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public Integer getSeatCapacity() {
        return capacity;
    }

    public void setSeatCapacity(Integer seatCapacity) {
        if (seatCapacity != null) {
            this.capacity = seatCapacity;
        }
    }

    public Integer getBedCapacity() {
        return capacity;
    }

    public void setBedCapacity(Integer bedCapacity) {
        if (bedCapacity != null) {
            this.capacity = bedCapacity;
        }
    }

    public String getPlateNumber() {
        return plateNumber;
    }

    public void setPlateNumber(String plateNumber) {
        this.plateNumber = plateNumber;
    }

    public String getVehicleModel() {
        return vehicleModel;
    }

    public void setVehicleModel(String vehicleModel) {
        this.vehicleModel = vehicleModel;
    }

    public String getLanguages() {
        return languages;
    }

    public void setLanguages(String languages) {
        this.languages = languages;
    }

    public String getCertification() {
        return certification;
    }

    public void setCertification(String certification) {
        this.certification = certification;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getHotelName() {
        return hotelName;
    }

    public void setHotelName(String hotelName) {
        this.hotelName = hotelName;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
