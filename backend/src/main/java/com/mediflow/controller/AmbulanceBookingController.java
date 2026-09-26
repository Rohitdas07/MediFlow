package com.mediflow.controller;

import com.mediflow.dto.AmbulanceBookingRequest;
import com.mediflow.dto.AmbulanceBookingResponse;
import com.mediflow.security.PatientUserDetails;
import com.mediflow.service.AmbulanceBookingService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ambulances/bookings")
public class AmbulanceBookingController {
    private final AmbulanceBookingService service;

    public AmbulanceBookingController(AmbulanceBookingService service) { this.service = service; }

    @GetMapping
    public List<AmbulanceBookingResponse> mine(@AuthenticationPrincipal PatientUserDetails user) {
        return service.mine(user.getPatient().getId()).stream().map(AmbulanceBookingResponse::from).toList();
    }

    @PostMapping
    public AmbulanceBookingResponse book(@AuthenticationPrincipal PatientUserDetails user, @Valid @RequestBody AmbulanceBookingRequest request) {
        return AmbulanceBookingResponse.from(service.book(user.getPatient().getId(), request));
    }

    @DeleteMapping("/{id}")
    public void cancel(@AuthenticationPrincipal PatientUserDetails user, @PathVariable Long id) {
        service.cancel(user.getPatient().getId(), id);
    }
}
