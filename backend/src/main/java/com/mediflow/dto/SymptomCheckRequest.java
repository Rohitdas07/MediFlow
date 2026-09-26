package com.mediflow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SymptomCheckRequest {
    @NotBlank
    private String symptomText;

    // Optional - patient's current location, used if condition turns out critical
    private Double latitude;
    private Double longitude;
	public String getSymptomText() {
		return symptomText;
	}
	public void setSymptomText(String symptomText) {
		this.symptomText = symptomText;
	}
	public Double getLatitude() {
		return latitude;
	}
	public void setLatitude(Double latitude) {
		this.latitude = latitude;
	}
	public Double getLongitude() {
		return longitude;
	}
	public void setLongitude(Double longitude) {
		this.longitude = longitude;
	}
    
    
    
}
