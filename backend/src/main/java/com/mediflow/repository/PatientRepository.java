package com.mediflow.repository;

import com.mediflow.model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {
    Optional<Patient> findByEmail(String email);
    Optional<Patient> findByPatientCodeIgnoreCase(String patientCode);
    boolean existsByEmail(String email);
    boolean existsByPatientCode(String patientCode);
}
