package com.mediflow.dto;

import com.mediflow.model.Ambulance;
public class NearbyAmbulanceResponse {
    private Ambulance ambulance;
    private double distanceKm;
    private String callUrl;
	public Ambulance getAmbulance() {
		return ambulance;
	}
	public void setAmbulance(Ambulance ambulance) {
		this.ambulance = ambulance;
	}
	public double getDistanceKm() {
		return distanceKm;
	}
	public void setDistanceKm(double distanceKm) {
		this.distanceKm = distanceKm;
	}
	public String getCallUrl() {
		return callUrl;
	}
	public void setCallUrl(String callUrl) {
		this.callUrl = callUrl;
	}
	public NearbyAmbulanceResponse(Ambulance ambulance, double distanceKm, String callUrl) {
		super();
		this.ambulance = ambulance;
		this.distanceKm = distanceKm;
		this.callUrl = callUrl;
	}
    
	
    
}
