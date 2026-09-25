package com.mediflow.repository;

import com.mediflow.model.HospitalStaff;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface HospitalStaffRepository extends JpaRepository<HospitalStaff, Long> {
    Optional<HospitalStaff> findByStaffIdIgnoreCase(String staffId);
    boolean existsByStaffIdIgnoreCase(String staffId);
    boolean existsByEmailIgnoreCase(String email);
}
