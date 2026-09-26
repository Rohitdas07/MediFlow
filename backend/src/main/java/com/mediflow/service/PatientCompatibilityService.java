package com.mediflow.service;

import com.mediflow.dto.PatientCompatibilityResponse;
import com.mediflow.model.Patient;
import com.mediflow.repository.PatientRepository;
import com.mediflow.security.PatientUserDetails;
import com.mediflow.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class PatientCompatibilityService {
    private final PatientRepository repository;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;
    private final PatientIdentityService identityService;

    public PatientCompatibilityService(PatientRepository repository, PasswordEncoder encoder, JwtService jwtService, PatientIdentityService identityService) {
        this.repository = repository; this.encoder = encoder; this.jwtService = jwtService; this.identityService = identityService;
    }

    public PatientCompatibilityResponse register(Map<String,Object> rawPatient, String pin) {
        if (rawPatient == null) throw new IllegalArgumentException("Patient details are required.");
        String name = string(rawPatient.get("name"), "New Patient");
        String email = string(rawPatient.get("email"), "").trim().toLowerCase();
        if (email.isBlank()) throw new IllegalArgumentException("Email is required.");
        if (repository.existsByEmail(email)) throw new IllegalArgumentException("An account with this email already exists.");

        Patient p = new Patient();
        p.setPatientCode(identityService.generateUniqueCode());
        p.setName(name);
        p.setEmail(email);
        p.setPassword(encoder.encode(pin));
        p.setPhone(string(rawPatient.get("mobile"), string(rawPatient.get("phone"), "")));
        p.setGender(string(rawPatient.get("gender"), "other"));
        Object age = rawPatient.get("age");
        if (age != null) {
            try { p.setDob(java.time.LocalDate.now().minusYears(Long.parseLong(String.valueOf(age)))); } catch (Exception ignored) {}
        }
        Patient saved = repository.save(p);
        String token = jwtService.generateToken(new PatientUserDetails(saved));
        return success(saved, token);
    }

    public PatientCompatibilityResponse login(String identifier, String pin) {
        String value = identifier == null ? "" : identifier.trim();
        Patient p = repository.findByEmail(value.toLowerCase()).orElseGet(() -> repository.findByPatientCodeIgnoreCase(value).orElse(null));
        if (p == null || !encoder.matches(pin, p.getPassword())) {
            throw new IllegalArgumentException("Invalid email or PIN.");
        }
        // Legacy accounts may predate permanent Patient IDs. Backfill on login too
        // so a user can never receive a response containing a null Patient ID.
        identityService.ensurePatientCode(p);
        String token = jwtService.generateToken(new PatientUserDetails(p));
        return success(p, token);
    }

    private PatientCompatibilityResponse success(Patient p, String token) {
        Map<String,Object> patient = new LinkedHashMap<>();
        patient.put("id", String.valueOf(p.getId()));
        patient.put("patient_id", p.getPatientCode());
        patient.put("patientCode", p.getPatientCode());
        patient.put("name", p.getName());
        patient.put("email", p.getEmail());
        patient.put("phone", p.getPhone());
        patient.put("gender", p.getGender());
        if (p.getDob() != null) patient.put("dob", p.getDob().toString());
        patient.put("language", "en");
        patient.put("care_stream", "allopathy");
        patient.put("department", "General Medicine OPD");
        patient.put("consent_given", true);
        patient.put("created_at", p.getCreatedAt() == null ? LocalDateTime.now().toString() : p.getCreatedAt().toString());
        var session = new PatientCompatibilityResponse.Session(p.getId(), p.getId(), p.getName(), token, LocalDateTime.now().toString());
        return new PatientCompatibilityResponse(true, patient, session, null);
    }


    private static String string(Object value, String fallback) {
        return value == null ? fallback : String.valueOf(value);
    }
}
