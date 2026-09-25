package com.mediflow.repository;

import com.mediflow.model.MedicalHistoryRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MedicalHistoryRepository extends JpaRepository<MedicalHistoryRecord, Long> {
    List<MedicalHistoryRecord> findByPatientIdOrderByRecordedAtDesc(Long patientId);
}
