package com.mediflow.security;

import com.mediflow.model.Patient;
import com.mediflow.repository.PatientRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class PatientUserDetailsService implements UserDetailsService {

    private final PatientRepository patientRepository;

    public PatientUserDetailsService(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Patient patient = patientRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("No patient found with email: " + email));
        return new PatientUserDetails(patient);
    }
}
