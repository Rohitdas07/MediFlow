package com.mediflow.repository;

import com.mediflow.model.SymptomDepartmentMap;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SymptomDepartmentMapRepository extends JpaRepository<SymptomDepartmentMap, Long> {
    List<SymptomDepartmentMap> findAll();
}
