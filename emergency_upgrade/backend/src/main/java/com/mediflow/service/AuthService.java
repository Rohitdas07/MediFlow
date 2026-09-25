package com.mediflow.service;

import com.mediflow.dto.AuthResponse;
import com.mediflow.dto.LoginRequest;
import com.mediflow.dto.RegisterRequest;
import com.mediflow.model.Patient;
import com.mediflow.repository.PatientRepository;
import com.mediflow.security.JwtService;
import com.mediflow.security.PatientUserDetails;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final PatientRepository patientRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthService(PatientRepository patientRepository,
                        PasswordEncoder passwordEncoder,
                        JwtService jwtService,
                        AuthenticationManager authenticationManager) {
        this.patientRepository = patientRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    public AuthResponse register(RegisterRequest request) {
        if (patientRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("An account with this email already exists.");
        }

        Patient patient = new Patient();
        patient.setPatientCode(generatePatientCode());
        patient.setName(request.getName());
        patient.setEmail(request.getEmail());
        patient.setPassword(passwordEncoder.encode(request.getPassword()));
        patient.setPhone(request.getPhone());
        patient.setDob(request.getDob());
        patient.setGender(request.getGender());

        Patient saved = patientRepository.save(patient);
        String token = jwtService.generateToken(new PatientUserDetails(saved));

        return new AuthResponse(token, saved.getId(), saved.getName(), saved.getEmail());
    }

    private String generatePatientCode() {
        String code;
        do {
            code = "MFP-" + java.time.Year.now().getValue() + "-" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
            final String candidate = code;
            if (!patientRepository.existsByPatientCode(candidate)) return code;
        } while (true);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        Patient patient = patientRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        String token = jwtService.generateToken(new PatientUserDetails(patient));
        return new AuthResponse(token, patient.getId(), patient.getName(), patient.getEmail());
    }
}
