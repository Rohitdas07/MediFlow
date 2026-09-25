package com.mediflow.dto;

import com.mediflow.model.AmbulanceBooking;
import java.time.LocalDateTime;

public record AmbulanceBookingResponse(
        Long id, Long ambulanceId, String vehicleNumber, String driverName, String contactNumber,
        String hospitalName, Double pickupLatitude, Double pickupLongitude, String note,
        String status, LocalDateTime createdAt) {
    public static AmbulanceBookingResponse from(AmbulanceBooking b) {
        var a = b.getAmbulance();
        return new AmbulanceBookingResponse(
                b.getId(), a.getId(), a.getVehicleNumber(), a.getDriverName(), a.getContactNumber(),
                a.getHospital() == null ? null : a.getHospital().getName(),
                b.getPickupLatitude(), b.getPickupLongitude(), b.getNote(), b.getStatus(), b.getCreatedAt());
    }
}
