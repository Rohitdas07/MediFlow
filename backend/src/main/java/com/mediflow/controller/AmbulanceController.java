package com.mediflow.controller;

import com.mediflow.dto.NearbyAmbulanceResponse;
import com.mediflow.service.AmbulanceService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ambulances")
public class AmbulanceController {

    private final AmbulanceService ambulanceService;

    public AmbulanceController(AmbulanceService ambulanceService) {
        this.ambulanceService = ambulanceService;
    }

    @GetMapping("/available")
    public List<NearbyAmbulanceResponse> available(@RequestParam double lat,
                                                     @RequestParam double lng,
                                                     @RequestParam(defaultValue = "5") int limit) {
        return ambulanceService.findAvailableNearby(lat, lng, limit);
    }
}
