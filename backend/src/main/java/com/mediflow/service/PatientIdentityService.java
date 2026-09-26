package com.mediflow.service;

import com.mediflow.model.Patient;
import com.mediflow.repository.PatientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.UUID;

/**
 * Single source of truth for permanent MediFlow Patient IDs.
 * Format: MFP-YYYY-XXXXXXXX
 */
@Service
public class PatientIdentityService {
    private final PatientRepository patients;

    public PatientIdentityService(PatientRepository patients) {
        this.patients = patients;
    }

    @Transactional
    public String ensurePatientCode(Patient patient) {
        if (patient.getPatientCode() != null && !patient.getPatientCode().isBlank()) {
            String normalized = patient.getPatientCode().trim().toUpperCase();
            if (!normalized.equals(patient.getPatientCode())) {
                patient.setPatientCode(normalized);
                patients.save(patient);
            }
            return normalized;
        }

        String code;
        do {
            code = generateCandidate();
        } while (patients.existsByPatientCode(code));

        patient.setPatientCode(code);
        patients.save(patient);
        return code;
    }

    public String generateUniqueCode() {
        String code;
        do {
            code = generateCandidate();
        } while (patients.existsByPatientCode(code));
        return code;
    }

    private String generateCandidate() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "MFP-" + Year.now().getValue() + "-" + suffix;
    }
}
