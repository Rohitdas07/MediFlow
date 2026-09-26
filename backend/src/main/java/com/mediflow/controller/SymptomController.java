package com.mediflow.controller;

import com.mediflow.dto.SymptomCheckRequest;
import com.mediflow.dto.SymptomCheckResponse;
import com.mediflow.service.SymptomService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/symptoms")
public class SymptomController {

    private final SymptomService symptomService;

    public SymptomController(SymptomService symptomService) {
        this.symptomService = symptomService;
    }

    @PostMapping("/check")
    public ResponseEntity<SymptomCheckResponse> check(@Valid @RequestBody SymptomCheckRequest request) {
        return ResponseEntity.ok(symptomService.check(request.getSymptomText()));
    }
}
