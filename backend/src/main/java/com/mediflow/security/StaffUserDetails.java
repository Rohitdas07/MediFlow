package com.mediflow.security;

import com.mediflow.model.HospitalStaff;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.Locale;

/** UserDetails adapter for hospital clinical staff accounts. */
public class StaffUserDetails implements UserDetails {
    private final HospitalStaff staff;

    public StaffUserDetails(HospitalStaff staff) {
        this.staff = staff;
    }

    public HospitalStaff getStaff() {
        return staff;
    }

    @Override
    public List<? extends GrantedAuthority> getAuthorities() {
        String role = staff.getRole() == null ? "staff" : staff.getRole().toLowerCase(Locale.ROOT);
        String authority = switch (role) {
            case "doctor" -> "ROLE_DOCTOR";
            case "medical_officer" -> "ROLE_MEDICAL_OFFICER";
            case "triage_nurse", "nurse" -> "ROLE_TRIAGE_NURSE";
            default -> "ROLE_STAFF";
        };
        return List.of(new SimpleGrantedAuthority(authority));
    }

    @Override public String getPassword() { return staff.getPinHash(); }
    @Override public String getUsername() { return staff.getStaffId(); }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() {
        return staff.getStatus() == null || "active".equalsIgnoreCase(staff.getStatus());
    }
}
