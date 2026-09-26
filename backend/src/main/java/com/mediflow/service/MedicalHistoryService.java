package com.mediflow.service;

import com.mediflow.dto.MedicalHistoryRequest;
import com.mediflow.model.MedicalHistoryRecord;
import com.mediflow.model.Patient;
import com.mediflow.repository.MedicalHistoryRepository;
import com.mediflow.repository.PatientRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class MedicalHistoryService {
    private final MedicalHistoryRepository records; private final PatientRepository patients;
    public MedicalHistoryService(MedicalHistoryRepository records, PatientRepository patients){this.records=records;this.patients=patients;}
    public List<MedicalHistoryRecord> list(Long patientId){return records.findByPatientIdOrderByRecordedAtDesc(patientId);}
    public MedicalHistoryRecord add(Long patientId, MedicalHistoryRequest req){
        Patient p=patients.findById(patientId).orElseThrow(()->new IllegalArgumentException("Patient not found."));
        MedicalHistoryRecord r=new MedicalHistoryRecord(); r.setPatient(p); r.setCategory(req.getCategory().trim()); r.setDetails(req.getDetails().trim()); return records.save(r);
    }
    public void delete(Long patientId, Long id){
        MedicalHistoryRecord r=records.findById(id).orElseThrow(()->new IllegalArgumentException("Record not found."));
        if(!r.getPatient().getId().equals(patientId)) throw new IllegalArgumentException("You can only delete your own records.");
        records.delete(r);
    }
}
