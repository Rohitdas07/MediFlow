package com.mediflow.dto;

import com.mediflow.model.MedicalHistoryRecord;
import java.time.LocalDateTime;

public record MedicalHistoryResponse(Long id, String category, String details, LocalDateTime recordedAt) {
    public static MedicalHistoryResponse from(MedicalHistoryRecord r) {
        return new MedicalHistoryResponse(r.getId(), r.getCategory(), r.getDetails(), r.getRecordedAt());
    }
}
