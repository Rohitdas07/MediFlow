package com.mediflow.controller;

import com.mediflow.model.HospitalStaff;
import com.mediflow.repository.HospitalStaffRepository;
import com.mediflow.security.StaffUserDetails;
import com.mediflow.service.StaffAuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class StaffAuthController {
    private final HospitalStaffRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final StaffAuthService staffAuthService;

    public StaffAuthController(HospitalStaffRepository repository,
                               PasswordEncoder passwordEncoder,
                               StaffAuthService staffAuthService) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.staffAuthService = staffAuthService;
    }

    @PostMapping("/staff-login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> request) {
        String staffId = value(request, "staffId");
        String pin = value(request, "pin");

        if (staffId.isBlank() || pin.isBlank()) {
            return error(400, "Staff ID and PIN are required.");
        }

        HospitalStaff staff = repository.findByStaffIdIgnoreCase(staffId).orElse(null);
        if (staff == null) {
            return error(401, "Authentication failed. Please verify your Staff ID and PIN.");
        }
        if (staff.getStatus() != null && !"active".equalsIgnoreCase(staff.getStatus())) {
            return error(403, "This staff account is currently " + staff.getStatus() + ". Please contact the HIS Administrator.");
        }
        if (isDoctorRole(staff.getRole()) && !staff.isNmcVerified()) {
            return error(403, "Doctor authentication blocked: NMC/IMR verification is required before clinical access.");
        }
        if (staff.getPinHash() == null || !passwordEncoder.matches(pin, staff.getPinHash())) {
            return error(401, "Authentication failed. Please verify your Staff ID and PIN.");
        }

        staff.setLastLoginAt(LocalDateTime.now());
        staff.setUpdatedAt(LocalDateTime.now());
        repository.save(staff);

        StaffUserDetails userDetails = new StaffUserDetails(staff);
        String token = staffAuthService.createToken(userDetails);
        String role = normalizeRole(staff.getRole());
        String targetView = "triage_nurse".equals(role) ? "triage" : "doctor";

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Staff authentication successful.",
                "session", Map.ofEntries(
                        Map.entry("token", token),
                        Map.entry("userId", staff.getStaffId()),
                        Map.entry("userName", staff.getFullName()),
                        Map.entry("role", role),
                        Map.entry("roleTitle", blankToDefault(staff.getRoleTitle(), roleTitleFor(role))),
                        Map.entry("department", blankToDefault(staff.getDepartment(), "Hospital Clinical Services")),
                        Map.entry("staffCode", staff.getStaffId()),
                        Map.entry("targetView", targetView),
                        Map.entry("nmcVerified", staff.isNmcVerified()),
                        Map.entry("nmcRegistrationNumber", blankToDefault(staff.getNmcRegistrationNumber(), "")),
                        Map.entry("nmcCouncil", blankToDefault(staff.getNmcCouncil(), "")),
                        Map.entry("issuedAt", Instant.now().toString())
                ),
                "staff", staff
        ));
    }

    @PostMapping("/staff-reset-pin")
    public ResponseEntity<Map<String, Object>> resetPin(@RequestBody Map<String, String> request) {
        String staffId = value(request, "staffId");
        String currentPin = value(request, "currentPin");
        String newPin = value(request, "newPin");
        String confirmNewPin = value(request, "confirmNewPin");

        if (staffId.isBlank() || currentPin.isBlank() || newPin.isBlank() || confirmNewPin.isBlank()) {
            return error(400, "Staff ID, current PIN, new PIN and confirmation are required.");
        }
        if (newPin.length() < 4) return error(400, "New PIN must be at least 4 characters.");
        if (!newPin.equals(confirmNewPin)) return error(400, "New PIN and confirmation do not match.");

        HospitalStaff staff = repository.findByStaffIdIgnoreCase(staffId).orElse(null);
        if (staff == null || staff.getPinHash() == null || !passwordEncoder.matches(currentPin, staff.getPinHash())) {
            return error(401, "Current PIN is incorrect. If this is the first login, use the PIN issued by the HIS Administrator.");
        }
        if (staff.getStatus() != null && !"active".equalsIgnoreCase(staff.getStatus())) {
            return error(403, "This staff account is not active.");
        }

        staff.setPinHash(passwordEncoder.encode(newPin));
        staff.setUpdatedAt(LocalDateTime.now());
        repository.save(staff);
        return ResponseEntity.ok(Map.of("success", true, "message", "Security PIN updated successfully. You can now sign in with your new PIN."));
    }

    private static String value(Map<String, String> map, String key) {
        String v = map == null ? null : map.get(key);
        return v == null ? "" : v.trim();
    }

    private static String normalizeRole(String role) {
        String r = role == null ? "staff" : role.trim().toLowerCase(Locale.ROOT);
        if (r.equals("medical officer") || r.equals("medical-officer") || r.equals("medicalofficer")) return "medical_officer";
        if (r.equals("nurse") || r.equals("triage nurse") || r.equals("triage-nurse")) return "triage_nurse";
        if (r.equals("doctor")) return "doctor";
        return r;
    }

    private static boolean isDoctorRole(String role) {
        return "doctor".equalsIgnoreCase(role) || "medical_officer".equalsIgnoreCase(role) || "medical officer".equalsIgnoreCase(role);
    }

    private static String roleTitleFor(String role) {
        return switch (role) {
            case "doctor" -> "Doctor";
            case "medical_officer" -> "Medical Officer";
            case "triage_nurse" -> "Triage Nurse";
            default -> "Clinical Staff";
        };
    }

    private static String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static ResponseEntity<Map<String, Object>> error(int status, String message) {
        return ResponseEntity.status(status).body(Map.of("success", false, "error", message));
    }
}
