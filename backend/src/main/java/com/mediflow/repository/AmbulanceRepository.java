package com.mediflow.repository;

import com.mediflow.model.Ambulance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AmbulanceRepository extends JpaRepository<Ambulance, Long> {
    List<Ambulance> findByAvailableTrue();
}
