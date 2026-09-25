package com.mediflow.controller;

import com.mediflow.dto.NearbyHospitalResponse;
import com.mediflow.service.HospitalService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hospitals")
public class HospitalController {

    private final HospitalService hospitalService;

    public HospitalController(HospitalService hospitalService) {
        this.hospitalService = hospitalService;
    }

    @GetMapping("/nearby")
    public List<NearbyHospitalResponse> nearby(@RequestParam double lat,
                                                @RequestParam double lng,
                                                @RequestParam(defaultValue = "5") int limit) {
        return hospitalService.findNearby(lat, lng, limit);
    }
}
