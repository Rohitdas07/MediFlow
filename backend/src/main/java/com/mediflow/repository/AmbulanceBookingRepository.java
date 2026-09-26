package com.mediflow.repository;

import com.mediflow.model.AmbulanceBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AmbulanceBookingRepository extends JpaRepository<AmbulanceBooking, Long> {
    List<AmbulanceBooking> findByPatientIdOrderByCreatedAtDesc(Long patientId);
    boolean existsByAmbulanceIdAndStatus(Long ambulanceId, String status);
}
