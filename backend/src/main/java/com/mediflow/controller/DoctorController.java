package com.mediflow.controller;

import com.mediflow.model.Doctor;
import com.mediflow.repository.DoctorRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/doctors")
public class DoctorController {

    private final DoctorRepository doctorRepository;

    public DoctorController(DoctorRepository doctorRepository) {
        this.doctorRepository = doctorRepository;
    }

    @GetMapping
    public List<Doctor> getDoctors(@RequestParam(required = false) Long departmentId) {
        if (departmentId != null) {
            return doctorRepository.findByDepartmentId(departmentId);
        }
        return doctorRepository.findAll();
    }

    @GetMapping("/{id}")
    public Doctor getDoctor(@PathVariable Long id) {
        return doctorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found: " + id));
    }
}
