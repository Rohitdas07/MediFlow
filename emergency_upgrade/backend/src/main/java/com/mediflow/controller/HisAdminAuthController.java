package com.mediflow.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import com.mediflow.security.AdminUserDetails;
import com.mediflow.security.JwtService;

import java.time.Instant;
import java.util.Map;

/**
 * Single HIS master administrator login for the MediFlow deployment.
 * Credentials are intentionally fixed for the local/demo hospital installation.
 */
@RestController
@RequestMapping("/api/auth")
public class HisAdminAuthController {

    @Value("${app.his-admin.id:HIS-01234}")
    private String adminId;

    @Value("${app.his-admin.pin:01234}")
    private String adminPin;

    private final JwtService jwtService;

    public HisAdminAuthController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @PostMapping("/his-admin-login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> request) {
        String adminId = request.getOrDefault("adminId", "").trim();
        String pin = request.getOrDefault("pin", "").trim();

        if (!this.adminId.equalsIgnoreCase(adminId) || !this.adminPin.equals(pin)) {
            return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "error", "Invalid HIS Master Administrator credentials."
            ));
        }

        String token = jwtService.generateToken(new AdminUserDetails(this.adminId, this.adminPin));
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "HIS Master Administrator verified.",
                "session", Map.of(
                        "token", token,
                        "userId", this.adminId,
                        "userName", "HIS Master Administrator",
                        "role", "admin",
                        "roleTitle", "HIS Master Administrator",
                        "department", "Hospital Information System",
                        "staffCode", this.adminId,
                        "targetView", "admin",
                        "isMasterAdmin", true,
                        "issuedAt", Instant.now().toString()
                )
        ));
    }
}
