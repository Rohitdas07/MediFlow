package com.mediflow.service;

import com.mediflow.dto.NearbyAmbulanceResponse;
import com.mediflow.model.Ambulance;
import com.mediflow.repository.AmbulanceRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class AmbulanceService {

    private final AmbulanceRepository ambulanceRepository;

    public AmbulanceService(AmbulanceRepository ambulanceRepository) {
        this.ambulanceRepository = ambulanceRepository;
    }

    public List<NearbyAmbulanceResponse> findAvailableNearby(double lat, double lon, int limit) {
        List<Ambulance> ambulances = ambulanceRepository.findByAvailableTrue();

        return ambulances.stream()
                .map(a -> new NearbyAmbulanceResponse(
                        a,
                        Math.round(GeoUtils.distanceKm(lat, lon, a.getLatitude(), a.getLongitude()) * 100.0) / 100.0,
                        "tel:" + a.getContactNumber()
                ))
                .sorted(Comparator.comparingDouble(NearbyAmbulanceResponse::getDistanceKm))
                .limit(limit)
                .toList();
    }
}
