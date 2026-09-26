package com.mediflow.controller;

import com.mediflow.model.HospitalStaff;
import com.mediflow.repository.HospitalStaffRepository;
import com.mediflow.repository.DoctorRepository;
import com.mediflow.repository.DepartmentRepository;
import com.mediflow.model.Doctor;
import com.mediflow.model.Department;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/admin/staff")
public class AdminStaffController {
    private final HospitalStaffRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final DoctorRepository doctorRepository;
    private final DepartmentRepository departmentRepository;

    public AdminStaffController(HospitalStaffRepository repository, PasswordEncoder passwordEncoder, DoctorRepository doctorRepository, DepartmentRepository departmentRepository) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.doctorRepository = doctorRepository;
        this.departmentRepository = departmentRepository;
    }

    @GetMapping
    public Map<String,Object> list() {
        return Map.of("success", true, "staff", repository.findAll());
    }

    @PostMapping
    public ResponseEntity<Map<String,Object>> create(@RequestBody Map<String,Object> p) {
        try {
            String name = str(p, "fullName");
            if (name.isBlank()) return error(400, "Full name is required.");
            String role = strOr(p, "role", "doctor").toLowerCase();
            if (!Set.of("doctor", "medical_officer", "triage_nurse", "nurse").contains(role)) {
                return error(400, "Unsupported staff role: " + role);
            }
            String staffId = str(p, "staffId");
            if (staffId.isBlank()) staffId = generateStaffId(role, name);
            if (repository.existsByStaffIdIgnoreCase(staffId)) return error(409, "Staff/Doctor ID already exists: " + staffId);

            String email = str(p, "email");
            if (!email.isBlank() && repository.existsByEmailIgnoreCase(email)) return error(409, "Email already exists.");

            HospitalStaff s = new HospitalStaff();
            s.setStaffId(staffId);
            s.setFullName(name);
            s.setRole(role);
            s.setRoleTitle(str(p,"roleTitle"));
            s.setDepartment(str(p,"department"));
            s.setSpecialization(str(p,"specialization"));
            s.setRegistrationNumber(str(p,"registrationNumber"));
            s.setNmcRegistrationNumber(strOr(p, "nmcRegistrationNumber", str(p,"registrationNumber")));
            s.setNmcCouncil(str(p,"nmcCouncil"));
            s.setNmcVerified(false);
            s.setEmployeeCode(str(p,"employeeCode"));
            s.setMobile(str(p,"mobile"));
            s.setEmail(email.isBlank() ? null : email);
            s.setQualification(str(p,"qualification"));
            s.setJoiningDate(parseDate(str(p,"joiningDate")));
            s.setRoomNumber(str(p,"roomNumber"));
            s.setOpdTimings(str(p,"opdTimings"));
            s.setConsultationFee(parseDecimal(p.get("consultationFee")));
            s.setAvailableDays(str(p,"availableDays"));
            s.setBio(str(p,"bio"));
            s.setStatus("active");
            String pin = str(p,"initialPin");
            if (pin.isBlank()) pin = staffId;
            if (pin.length() < 4) return error(400, "Initial PIN must contain at least 4 characters.");
            s.setPinHash(passwordEncoder.encode(pin));
            s.setCreatedAt(LocalDateTime.now());
            s.setUpdatedAt(LocalDateTime.now());
            HospitalStaff saved = repository.save(s);
            if ("doctor".equalsIgnoreCase(role) || "medical_officer".equalsIgnoreCase(role)) {
                upsertDoctor(saved);
            }
            return ResponseEntity.ok(Map.of("success", true, "message", "Doctor/Staff saved successfully with ID " + saved.getStaffId(), "staff", saved));
        } catch (Exception e) {
            return error(500, "Failed to save doctor/staff: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String,Object>> update(@PathVariable String id, @RequestBody Map<String,Object> p) {
        try {
            HospitalStaff s = find(id);
            if (s == null) return error(404, "Staff member not found: " + id);
            if (p.containsKey("fullName")) s.setFullName(str(p,"fullName"));
            if (p.containsKey("roleTitle")) s.setRoleTitle(str(p,"roleTitle"));
            if (p.containsKey("department")) s.setDepartment(str(p,"department"));
            if (p.containsKey("specialization")) s.setSpecialization(str(p,"specialization"));
            if (p.containsKey("registrationNumber")) {
                String newRegistration = str(p,"registrationNumber");
                if (!newRegistration.equalsIgnoreCase(s.getRegistrationNumber() == null ? "" : s.getRegistrationNumber()) && isDoctorRole(s.getRole())) {
                    s.setNmcVerified(false);
                    s.setNmcVerifiedAt(null);
                }
                s.setRegistrationNumber(newRegistration);
                if (isDoctorRole(s.getRole())) s.setNmcRegistrationNumber(newRegistration);
            }
            if (p.containsKey("mobile")) s.setMobile(str(p,"mobile"));
            if (p.containsKey("email")) s.setEmail(str(p,"email"));
            if (p.containsKey("qualification")) s.setQualification(str(p,"qualification"));
            if (p.containsKey("roomNumber")) s.setRoomNumber(str(p,"roomNumber"));
            if (p.containsKey("opdTimings")) s.setOpdTimings(str(p,"opdTimings"));
            if (p.containsKey("consultationFee")) s.setConsultationFee(parseDecimal(p.get("consultationFee")));
            if (p.containsKey("availableDays")) s.setAvailableDays(str(p,"availableDays"));
            if (p.containsKey("bio")) s.setBio(str(p,"bio"));
            s.setUpdatedAt(LocalDateTime.now());
            HospitalStaff saved = repository.save(s);
            return ResponseEntity.ok(Map.of("success", true, "message", "Staff details saved.", "staff", saved));
        } catch (Exception e) { return error(500, e.getMessage()); }
    }

    @PostMapping("/{id}/nmc-verification")
    public ResponseEntity<Map<String,Object>> verifyNmc(@PathVariable String id, @RequestBody Map<String,Object> p) {
        try {
            HospitalStaff s = find(id);
            if (s == null) return error(404, "Staff member not found.");
            if (!isDoctorRole(s.getRole())) return error(400, "NMC verification is required only for doctors and medical officers.");

            String registration = strOr(p, "nmcRegistrationNumber", s.getRegistrationNumber());
            String council = str(p, "nmcCouncil");
            String registryName = str(p, "nmcRegistryName");
            String registryQualification = str(p, "nmcRegistryQualification");
            String source = strOr(p, "nmcVerificationSource", "NMC Indian Medical Register (manual external verification)");

            if (registration.isBlank()) return error(400, "NMC/IMR registration number is required.");
            if (council.isBlank()) return error(400, "State Medical Council is required.");
            if (registryName.isBlank()) return error(400, "Enter the doctor name exactly as displayed in the NMC/IMR result.");

            s.setRegistrationNumber(registration);
            s.setNmcRegistrationNumber(registration);
            s.setNmcCouncil(council);
            s.setNmcRegistryName(registryName);
            s.setNmcRegistryQualification(registryQualification);
            s.setNmcVerificationSource(source);
            s.setNmcVerified(true);
            s.setNmcVerifiedAt(LocalDateTime.now());
            s.setUpdatedAt(LocalDateTime.now());
            HospitalStaff saved = repository.save(s);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "NMC/IMR verification recorded. Doctor account is now eligible for MediFlow clinical login.",
                "staff", saved,
                "verification", Map.of(
                    "status", "VERIFIED",
                    "registrationNumber", registration,
                    "council", council,
                    "registryName", registryName,
                    "source", source,
                    "verifiedAt", saved.getNmcVerifiedAt().toString()
                )
            ));
        } catch (Exception e) {
            return error(500, "Failed to record NMC verification: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/nmc-unverify")
    public ResponseEntity<Map<String,Object>> unverifyNmc(@PathVariable String id) {
        HospitalStaff s = find(id);
        if (s == null) return error(404, "Staff member not found.");
        s.setNmcVerified(false);
        s.setNmcVerifiedAt(null);
        s.setUpdatedAt(LocalDateTime.now());
        repository.save(s);
        return ResponseEntity.ok(Map.of("success", true, "message", "NMC verification removed. Doctor clinical login is blocked until re-verified.", "staff", s));
    }

    @PostMapping("/reset-pin")
    public ResponseEntity<Map<String,Object>> resetPin(@RequestBody Map<String,Object> p) {
        HospitalStaff s = find(str(p,"staffId"));
        if (s == null) return error(404, "Staff member not found.");
        String pin = str(p,"newPin");
        if (Boolean.TRUE.equals(p.get("resetToStaffId")) || pin.isBlank()) pin = s.getStaffId();
        s.setPinHash(passwordEncoder.encode(pin));
        s.setUpdatedAt(LocalDateTime.now());
        repository.save(s);
        return ResponseEntity.ok(Map.of("success", true, "message", "PIN updated successfully.", "newPin", pin, "isStaffIdPin", pin.equals(s.getStaffId())));
    }

    @PostMapping("/status")
    public ResponseEntity<Map<String,Object>> status(@RequestBody Map<String,Object> p) {
        HospitalStaff s = find(str(p,"staffId"));
        if (s == null) return error(404, "Staff member not found.");
        s.setStatus(strOr(p,"status","active"));
        s.setStatusReason(str(p,"reason"));
        s.setUpdatedAt(LocalDateTime.now());
        repository.save(s);
        return ResponseEntity.ok(Map.of("success", true, "message", "Account status updated.", "staff", s));
    }

    private static boolean isDoctorRole(String role) {
        return "doctor".equalsIgnoreCase(role) || "medical_officer".equalsIgnoreCase(role) || "medical officer".equalsIgnoreCase(role);
    }

    private HospitalStaff find(String id) {
        if (id == null || id.isBlank()) return null;
        try { return repository.findById(Long.valueOf(id)).orElse(null); } catch (NumberFormatException ignored) {}
        return repository.findByStaffIdIgnoreCase(id).orElse(null);
    }


    private void upsertDoctor(HospitalStaff staff) {
        Department department = resolveDepartment(staff.getDepartment());
        if (department == null) return;
        Doctor doctor = doctorRepository.findAll().stream()
                .filter(d -> d.getName().equalsIgnoreCase(staff.getFullName()))
                .findFirst().orElseGet(() -> {
                    Doctor d = new Doctor();
                    long nextId = doctorRepository.findAll().stream().mapToLong(x -> x.getId() == null ? 0L : x.getId()).max().orElse(0L) + 1L;
                    d.setId(nextId);
                    return d;
                });
        doctor.setName(staff.getFullName());
        doctor.setDepartment(department);
        doctor.setQualification(staff.getQualification());
        doctor.setExperienceYears(0);
        doctor.setFee(staff.getConsultationFee() == null ? BigDecimal.ZERO : staff.getConsultationFee());
        doctorRepository.save(doctor);
    }

    private Department resolveDepartment(String requested) {
        String target = requested == null ? "" : requested.toLowerCase().replace("opd", "").trim();
        return departmentRepository.findAll().stream().filter(d -> {
            String n = d.getName().toLowerCase();
            return n.equals(target) || n.contains(target) || target.contains(n);
        }).findFirst().orElseGet(() -> departmentRepository.findById(4L).orElse(null));
    }

    private String generateStaffId(String role, String name) {
        String prefix = role.contains("doctor") ? "DOC" : role.contains("medical") ? "MO" : "NURSE";
        String base = name.replaceAll("[^A-Za-z]", "").toUpperCase();
        if (base.length() > 6) base = base.substring(0,6);
        if (base.isBlank()) base = "STAFF";
        int n = 1;
        String id;
        do { id = prefix + "-" + base + "-" + String.format("%02d", n++); } while (repository.existsByStaffIdIgnoreCase(id));
        return id;
    }

    private static String str(Map<String,Object> p, String key) { Object v=p.get(key); return v==null?"":String.valueOf(v).trim(); }
    private static String strOr(Map<String,Object> p, String key, String fallback) { String v=str(p,key); return v.isBlank()?fallback:v; }
    private static LocalDate parseDate(String s) { try { return s.isBlank()?null:LocalDate.parse(s); } catch(Exception e){ return null; } }
    private static BigDecimal parseDecimal(Object v) { try { return v==null?BigDecimal.ZERO:new BigDecimal(String.valueOf(v)); } catch(Exception e){ return BigDecimal.ZERO; } }
    private static ResponseEntity<Map<String,Object>> error(int status, String message) { return ResponseEntity.status(status).body(Map.of("success", false, "error", message)); }
}
