package com.mediflow.dto;

import jakarta.validation.constraints.NotBlank;

public class PatientLoginCompatibilityRequest {
    @NotBlank private String identifier;
    @NotBlank private String pin;

    public String getIdentifier() { return identifier; }
    public void setIdentifier(String identifier) { this.identifier = identifier; }
    public String getPin() { return pin; }
    public void setPin(String pin) { this.pin = pin; }
}
