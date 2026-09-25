package com.mediflow.service;

import com.mediflow.dto.SymptomCheckResponse;
import com.mediflow.model.SymptomDepartmentMap;
import com.mediflow.repository.SymptomDepartmentMapRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class SymptomService {

    private final SymptomDepartmentMapRepository mapRepository;

    public SymptomService(SymptomDepartmentMapRepository mapRepository) {
        this.mapRepository = mapRepository;
    }

    /**
     * Very simple keyword-based triage:
     * 1. Lower-case the patient's free-text description.
     * 2. Find every known symptom keyword contained in the text.
     * 3. If any matched keyword is flagged critical -> mark the whole check critical
     *    and suggest Emergency Medicine regardless of the specific department matches.
     * 4. Otherwise, suggest the department of the longest (most specific) matching keyword.
     *
     * This can later be swapped for a call to a real NLP / LLM classification service
     * without changing the controller contract.
     */
    public SymptomCheckResponse check(String symptomText) {
        String normalized = symptomText.toLowerCase(Locale.ROOT).trim();

        List<SymptomDepartmentMap> all = mapRepository.findAll();
        List<SymptomDepartmentMap> matches = new ArrayList<>();

        for (SymptomDepartmentMap entry : all) {
            if (normalized.contains(entry.getKeyword().toLowerCase(Locale.ROOT))) {
                matches.add(entry);
            }
        }

        if (matches.isEmpty()) {
            return new SymptomCheckResponse(
                    false,
                    null,
                    List.of(),
                    "We couldn't confidently match your symptoms. Please consult General Medicine, " +
                            "or describe your symptoms with more detail."
            );
        }

        boolean anyCritical = matches.stream().anyMatch(SymptomDepartmentMap::isCritical);

        SymptomDepartmentMap best = matches.stream()
                .max((a, b) -> Integer.compare(a.getKeyword().length(), b.getKeyword().length()))
                .orElseThrow();

        List<String> matchedKeywords = matches.stream().map(SymptomDepartmentMap::getKeyword).toList();

        if (anyCritical) {
            SymptomDepartmentMap criticalMatch = matches.stream()
                    .filter(SymptomDepartmentMap::isCritical)
                    .max((a, b) -> Integer.compare(a.getKeyword().length(), b.getKeyword().length()))
                    .orElseThrow();

            return new SymptomCheckResponse(
                    true,
                    criticalMatch.getDepartment(),
                    matchedKeywords,
                    "Your symptoms may indicate a serious/critical condition. Please seek emergency care " +
                            "immediately - nearby hospitals and available ambulances are shown below."
            );
        }

        return new SymptomCheckResponse(
                false,
                best.getDepartment(),
                matchedKeywords,
                "Based on your symptoms, we recommend booking a doctor in " + best.getDepartment().getName() + "."
        );
    }
}
