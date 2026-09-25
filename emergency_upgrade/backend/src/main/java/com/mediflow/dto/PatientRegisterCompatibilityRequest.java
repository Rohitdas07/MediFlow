package com.mediflow.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public class PatientRegisterCompatibilityRequest {
    @NotBlank private String pin;
    private Map<String, Object> patient;

    public String getPin() { return pin; }
    public void setPin(String pin) { this.pin = pin; }
    public Map<String, Object> getPatient() { return patient; }
    public void setPatient(Map<String, Object> patient) { this.patient = patient; }
}
