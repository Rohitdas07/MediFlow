package com.mediflow.service;

import com.mediflow.model.OpdDailySequence;
import com.mediflow.model.OpdToken;
import com.mediflow.repository.OpdDailySequenceRepository;
import com.mediflow.repository.OpdTokenRepository;
import com.mediflow.repository.PatientRepository;
import com.mediflow.model.Patient;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class OpdTokenService {
    private final OpdTokenRepository tokenRepository;
    private final OpdDailySequenceRepository sequenceRepository;
    private final PatientRepository patientRepository;

    public OpdTokenService(OpdTokenRepository tokenRepository, OpdDailySequenceRepository sequenceRepository, PatientRepository patientRepository) {
        this.tokenRepository = tokenRepository;
        this.sequenceRepository = sequenceRepository;
        this.patientRepository = patientRepository;
    }

    @Transactional
    public OpdToken issue(String requestedStream, String patientCode) {
        LocalDate today = LocalDate.now();
        String stream = normalizeStream(requestedStream);
        String patient = resolvePatientCode(patientCode);

        if (!patient.isBlank()) {
            Optional<OpdToken> existing = tokenRepository.findByTokenDateAndPatientCode(today, patient);
            if (existing.isPresent()) return existing.get();
        }

        OpdDailySequence sequence;
        try {
            sequence = sequenceRepository.findForUpdate(today, stream).orElseGet(() -> {
                OpdDailySequence created = new OpdDailySequence();
                created.setSequenceDate(today);
                created.setCareStream(stream);
                created.setLastNumber(0L);
                return sequenceRepository.saveAndFlush(created);
            });
        } catch (DataIntegrityViolationException race) {
            sequence = sequenceRepository.findForUpdate(today, stream)
                    .orElseThrow(() -> new IllegalStateException("Unable to initialize OPD sequence."));
        }

        long next = sequence.getLastNumber() + 1L;
        sequence.setLastNumber(next);
        sequenceRepository.save(sequence);

        String token = prefixFor(stream) + "-" + String.format("%03d", next);
        while (tokenRepository.existsByTokenDateAndTokenNumber(today, token)) {
            next++;
            sequence.setLastNumber(next);
            sequenceRepository.save(sequence);
            token = prefixFor(stream) + "-" + String.format("%03d", next);
        }

        OpdToken tokenEntity = new OpdToken();
        tokenEntity.setTokenDate(today);
        tokenEntity.setTokenNumber(token);
        tokenEntity.setCareStream(stream);
        tokenEntity.setPatientCode(patient.isBlank() ? null : patient);
        tokenEntity.setIssuedAt(LocalDateTime.now());
        return tokenRepository.save(tokenEntity);
    }


    private String resolvePatientCode(String value) {
        String raw = value == null ? "" : value.trim();
        if (raw.isBlank()) return "";

        // Legacy kiosk sessions may still send the numeric MySQL primary key.
        // Always translate that internal ID to the permanent MFP Patient ID.
        if (raw.matches("\\d+")) {
            try {
                Long id = Long.parseLong(raw);
                return patientRepository.findById(id)
                        .map(Patient::getPatientCode)
                        .filter(code -> code != null && !code.isBlank())
                        .orElseThrow(() -> new IllegalArgumentException("Patient record has no permanent Patient ID."));
            } catch (NumberFormatException ignored) {
                throw new IllegalArgumentException("Invalid patient identifier.");
            }
        }

        // A permanent code must belong to an actual MySQL patient. Never store
        // a browser-generated/local ID in opd_tokens.
        return patientRepository.findByPatientCodeIgnoreCase(raw)
                .map(Patient::getPatientCode)
                .filter(code -> code != null && !code.isBlank())
                .map(String::toUpperCase)
                .orElseThrow(() -> new IllegalArgumentException("Patient not found. Please register the patient before generating an OPD token."));
    }

    private String normalizeStream(String value) {
        String v = value == null ? "allopathy" : value.trim().toLowerCase();
        if (v.contains("ayur")) return "ayurveda";
        if (v.contains("integrated")) return "integrated";
        return "allopathy";
    }

    private String prefixFor(String stream) {
        return switch (stream) {
            case "ayurveda" -> "AYUSH";
            case "integrated" -> "INT";
            default -> "OPD";
        };
    }
}
