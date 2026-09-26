package com.mediflow.service;

import com.mediflow.dto.NearbyHospitalResponse;
import com.mediflow.model.Hospital;
import com.mediflow.repository.HospitalRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class HospitalService {

    private final HospitalRepository hospitalRepository;

    public HospitalService(HospitalRepository hospitalRepository) {
        this.hospitalRepository = hospitalRepository;
    }

    public List<NearbyHospitalResponse> findNearby(double lat, double lon, int limit) {
        List<Hospital> hospitals = hospitalRepository.findByEmergencyAvailableTrue();

        return hospitals.stream()
                .map(h -> new NearbyHospitalResponse(
                        h,
                        Math.round(GeoUtils.distanceKm(lat, lon, h.getLatitude(), h.getLongitude()) * 100.0) / 100.0,
                        GeoUtils.googleMapsDirectionsUrl(h.getLatitude(), h.getLongitude())
                ))
                .sorted(Comparator.comparingDouble(NearbyHospitalResponse::getDistanceKm))
                .limit(limit)
                .toList();
    }
}
