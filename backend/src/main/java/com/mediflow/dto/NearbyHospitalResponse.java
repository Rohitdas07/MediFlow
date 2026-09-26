package com.mediflow.dto;

import com.mediflow.model.Hospital;
public class NearbyHospitalResponse {
    private Hospital hospital;
    private double distanceKm;
    private String directionsUrl;
     
    
	public NearbyHospitalResponse(Hospital hospital, double distanceKm, String directionsUrl) {
		super();
		this.hospital = hospital;
		this.distanceKm = distanceKm;
		this.directionsUrl = directionsUrl;
	}
	
	public Hospital getHospital() {
		return hospital;
	}
	public void setHospital(Hospital hospital) {
		this.hospital = hospital;
	}
	public double getDistanceKm() {
		return distanceKm;
	}
	public void setDistanceKm(double distanceKm) {
		this.distanceKm = distanceKm;
	}
	public String getDirectionsUrl() {
		return directionsUrl;
	}
	public void setDirectionsUrl(String directionsUrl) {
		this.directionsUrl = directionsUrl;
	}
    
    
}
