package com.mediflow.controller;

import com.mediflow.dto.*;
import com.mediflow.security.PatientUserDetails;
import com.mediflow.service.MedicalHistoryService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/medical-history")
public class MedicalHistoryController {
    private final MedicalHistoryService service;
    public MedicalHistoryController(MedicalHistoryService service){this.service=service;}
    @GetMapping public List<MedicalHistoryResponse> mine(@AuthenticationPrincipal PatientUserDetails user){return service.list(user.getPatient().getId()).stream().map(MedicalHistoryResponse::from).toList();}
    @PostMapping public MedicalHistoryResponse add(@AuthenticationPrincipal PatientUserDetails user,@Valid @RequestBody MedicalHistoryRequest req){return MedicalHistoryResponse.from(service.add(user.getPatient().getId(),req));}
    @DeleteMapping("/{id}") public void delete(@AuthenticationPrincipal PatientUserDetails user,@PathVariable Long id){service.delete(user.getPatient().getId(),id);}
}
