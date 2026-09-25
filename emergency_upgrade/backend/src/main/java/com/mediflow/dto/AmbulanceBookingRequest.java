package com.mediflow.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class AmbulanceBookingRequest {
    @NotNull private Long ambulanceId;
    @NotNull private Double pickupLatitude;
    @NotNull private Double pickupLongitude;
    @Size(max = 500) private String note;

    public Long getAmbulanceId() { return ambulanceId; }
    public void setAmbulanceId(Long ambulanceId) { this.ambulanceId = ambulanceId; }
    public Double getPickupLatitude() { return pickupLatitude; }
    public void setPickupLatitude(Double pickupLatitude) { this.pickupLatitude = pickupLatitude; }
    public Double getPickupLongitude() { return pickupLongitude; }
    public void setPickupLongitude(Double pickupLongitude) { this.pickupLongitude = pickupLongitude; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
