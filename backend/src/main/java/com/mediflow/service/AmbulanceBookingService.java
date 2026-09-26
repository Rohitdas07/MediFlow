package com.mediflow.service;

import com.mediflow.dto.AmbulanceBookingRequest;
import com.mediflow.model.AmbulanceBooking;
import com.mediflow.repository.AmbulanceBookingRepository;
import com.mediflow.repository.AmbulanceRepository;
import com.mediflow.repository.PatientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AmbulanceBookingService {
    private final AmbulanceBookingRepository bookings;
    private final AmbulanceRepository ambulances;
    private final PatientRepository patients;

    public AmbulanceBookingService(AmbulanceBookingRepository bookings, AmbulanceRepository ambulances, PatientRepository patients) {
        this.bookings = bookings;
        this.ambulances = ambulances;
        this.patients = patients;
    }

    @Transactional
    public AmbulanceBooking book(Long patientId, AmbulanceBookingRequest request) {
        AmbulanceBooking active = bookings.findByPatientIdOrderByCreatedAtDesc(patientId).stream()
                .filter(b -> "REQUESTED".equals(b.getStatus()) || "ACCEPTED".equals(b.getStatus()))
                .findFirst().orElse(null);
        if (active != null) throw new IllegalArgumentException("You already have an active ambulance request.");

        var ambulance = ambulances.findById(request.getAmbulanceId())
                .orElseThrow(() -> new IllegalArgumentException("Ambulance not found."));
        if (!ambulance.isAvailable() || bookings.existsByAmbulanceIdAndStatus(ambulance.getId(), "REQUESTED") || bookings.existsByAmbulanceIdAndStatus(ambulance.getId(), "ACCEPTED"))
            throw new IllegalArgumentException("That ambulance is no longer available.");

        var patient = patients.findById(patientId).orElseThrow(() -> new IllegalArgumentException("Patient not found."));
        var booking = new AmbulanceBooking();
        booking.setPatient(patient);
        booking.setAmbulance(ambulance);
        booking.setPickupLatitude(request.getPickupLatitude());
        booking.setPickupLongitude(request.getPickupLongitude());
        booking.setNote(request.getNote());
        booking.setStatus("REQUESTED");
        ambulance.setAvailable(false);
        ambulances.save(ambulance);
        return bookings.save(booking);
    }

    public List<AmbulanceBooking> mine(Long patientId) { return bookings.findByPatientIdOrderByCreatedAtDesc(patientId); }

    @Transactional
    public void cancel(Long patientId, Long bookingId) {
        var booking = bookings.findById(bookingId).orElseThrow(() -> new IllegalArgumentException("Ambulance request not found."));
        if (!booking.getPatient().getId().equals(patientId)) throw new IllegalArgumentException("You can only cancel your own ambulance request.");
        if (!("REQUESTED".equals(booking.getStatus()) || "ACCEPTED".equals(booking.getStatus())))
            throw new IllegalArgumentException("Ambulance request is already " + booking.getStatus().toLowerCase() + ".");
        booking.setStatus("CANCELLED");
        booking.getAmbulance().setAvailable(true);
        ambulances.save(booking.getAmbulance());
        bookings.save(booking);
    }
}
