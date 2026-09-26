package com.mediflow.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "hospital_staff", uniqueConstraints = {
        @UniqueConstraint(name = "uk_staff_staff_id", columnNames = "staff_id"),
        @UniqueConstraint(name = "uk_staff_email", columnNames = "email")
})
@Getter @Setter @NoArgsConstructor
public class HospitalStaff {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "staff_id", nullable = false, unique = true, length = 40)
    private String staffId;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(nullable = false, length = 30)
    private String role;

    private String roleTitle;
    private String department;
    private String specialization;
    private String registrationNumber;

    @Column(name = "nmc_registration_number", length = 100)
    private String nmcRegistrationNumber;

    @Column(name = "nmc_council", length = 200)
    private String nmcCouncil;

    @Column(name = "nmc_verified", nullable = false)
    private boolean nmcVerified = false;

    @Column(name = "nmc_verification_source", length = 500)
    private String nmcVerificationSource;

    @Column(name = "nmc_verified_at")
    private LocalDateTime nmcVerifiedAt;

    @Column(name = "nmc_registry_name", length = 255)
    private String nmcRegistryName;

    @Column(name = "nmc_registry_qualification", length = 255)
    private String nmcRegistryQualification;
    private String employeeCode;
    private String mobile;

    @Column(unique = true)
    private String email;

    private String qualification;
    private LocalDate joiningDate;
    private String roomNumber;
    private String opdTimings;
    private BigDecimal consultationFee;
    @Column(length = 1000) private String availableDays;
    @Column(length = 4000) private String bio;
    @Column(nullable = false, length = 20) private String status = "active";
    private String statusReason;
    private LocalDateTime lastLoginAt;
    @Column(nullable = false, updatable = false) private LocalDateTime createdAt = LocalDateTime.now();
    @Column(nullable = false) private LocalDateTime updatedAt = LocalDateTime.now();
    @Column(nullable = false) private String pinHash;
}
