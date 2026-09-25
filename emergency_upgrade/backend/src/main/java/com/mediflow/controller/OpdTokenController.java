package com.mediflow.controller;

import com.mediflow.model.OpdToken;
import com.mediflow.service.OpdTokenService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/opd")
public class OpdTokenController {
    private final OpdTokenService service;

    public OpdTokenController(OpdTokenService service) {
        this.service = service;
    }

    @PostMapping("/token")
    public ResponseEntity<Map<String, Object>> issue(@RequestBody(required = false) Map<String, String> req) {
        String stream = req == null ? "allopathy" : req.getOrDefault("careStream", "allopathy");
        String patientCode = req == null ? "" : req.getOrDefault("patientId", "");
        try {
            OpdToken token = service.issue(stream, patientCode);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "tokenNumber", token.getTokenNumber(),
                "issuedAt", token.getIssuedAt().toString(),
                "tokenDate", token.getTokenDate().toString(),
                "careStream", token.getCareStream(),
                "patientCode", token.getPatientCode() == null ? "" : token.getPatientCode()
            ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", ex.getMessage() == null ? "Unable to generate OPD token." : ex.getMessage()
            ));
        }
    }
}
