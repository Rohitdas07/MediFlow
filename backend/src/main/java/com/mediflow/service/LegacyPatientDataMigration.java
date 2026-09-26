package com.mediflow.service;

import com.mediflow.model.OpdToken;
import com.mediflow.model.Patient;
import com.mediflow.repository.OpdTokenRepository;
import com.mediflow.repository.PatientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * One-time/idempotent startup migration for legacy records created before
 * permanent MediFlow Patient IDs were introduced.
 *
 * It backfills patients.patient_code and converts numeric OPD patient references
 * (e.g. "1") to the patient's permanent MFP-YYYY-XXXXXXXX code.
 */
@Component
public class LegacyPatientDataMigration implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(LegacyPatientDataMigration.class);

    private final PatientRepository patients;
    private final OpdTokenRepository opdTokens;
    private final PatientIdentityService identityService;

    public LegacyPatientDataMigration(PatientRepository patients,
                                      OpdTokenRepository opdTokens,
                                      PatientIdentityService identityService) {
        this.patients = patients;
        this.opdTokens = opdTokens;
        this.identityService = identityService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int patientsUpdated = backfillPatients();
        int opdUpdated = migrateLegacyOpdReferences();
        if (patientsUpdated > 0 || opdUpdated > 0) {
            log.info("MediFlow identity migration completed: {} patient IDs backfilled, {} OPD references migrated.",
                    patientsUpdated, opdUpdated);
        }
    }

    private int backfillPatients() {
        int count = 0;
        List<Patient> all = patients.findAll();
        for (Patient patient : all) {
            if (patient.getPatientCode() == null || patient.getPatientCode().isBlank()) {
                identityService.ensurePatientCode(patient);
                count++;
            }
        }
        return count;
    }

    private int migrateLegacyOpdReferences() {
        int count = 0;
        List<OpdToken> tokens = opdTokens.findAll();
        for (OpdToken token : tokens) {
            String raw = token.getPatientCode();
            if (raw == null || raw.isBlank() || !raw.trim().matches("\\d+")) {
                continue;
            }

            try {
                Long patientId = Long.parseLong(raw.trim());
                Patient patient = patients.findById(patientId).orElse(null);
                if (patient != null && patient.getPatientCode() != null && !patient.getPatientCode().isBlank()) {
                    token.setPatientCode(patient.getPatientCode());
                    opdTokens.save(token);
                    count++;
                }
            } catch (NumberFormatException ignored) {
                // Keep malformed legacy values untouched rather than guessing.
            }
        }
        return count;
    }
}
