package com.mediflow.service;

import com.mediflow.security.JwtService;
import com.mediflow.security.StaffUserDetails;
import org.springframework.stereotype.Service;

@Service
public class StaffAuthService {
    private final JwtService jwtService;

    public StaffAuthService(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    public String createToken(StaffUserDetails userDetails) {
        return jwtService.generateToken(userDetails);
    }
}
