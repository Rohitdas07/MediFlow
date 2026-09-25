package com.mediflow.security;

import com.mediflow.model.HospitalStaff;
import com.mediflow.repository.HospitalStaffRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class StaffUserDetailsService implements UserDetailsService {
    private final HospitalStaffRepository repository;

    public StaffUserDetailsService(HospitalStaffRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserDetails loadUserByUsername(String staffId) throws UsernameNotFoundException {
        HospitalStaff staff = repository.findByStaffIdIgnoreCase(staffId == null ? "" : staffId.trim())
                .orElseThrow(() -> new UsernameNotFoundException("Staff account not found: " + staffId));
        return new StaffUserDetails(staff);
    }
}
