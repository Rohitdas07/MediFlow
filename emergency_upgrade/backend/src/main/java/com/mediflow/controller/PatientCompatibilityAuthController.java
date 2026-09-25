package com.mediflow.controller;

import com.mediflow.dto.PatientCompatibilityResponse;
import com.mediflow.dto.PatientLoginCompatibilityRequest;
import com.mediflow.dto.PatientRegisterCompatibilityRequest;
import com.mediflow.service.PatientCompatibilityService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class PatientCompatibilityAuthController {
    private final PatientCompatibilityService service;
    public PatientCompatibilityAuthController(PatientCompatibilityService service) { this.service = service; }

    @PostMapping("/patient-register")
    public ResponseEntity<PatientCompatibilityResponse> register(@Valid @RequestBody PatientRegisterCompatibilityRequest request) {
        return ResponseEntity.ok(service.register(request.getPatient(), request.getPin()));
    }

    @PostMapping("/patient-login")
    public ResponseEntity<PatientCompatibilityResponse> login(@Valid @RequestBody PatientLoginCompatibilityRequest request) {
        return ResponseEntity.ok(service.login(request.getIdentifier(), request.getPin()));
    }
}
