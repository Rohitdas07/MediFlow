package com.mediflow.dto;

public class AuthResponse {
    private String token;
    private Long patientId;
    private String name;
    private String email;
	public String getToken() {
		return token;
	}
	public void setToken(String token) {
		this.token = token;
	}
	public Long getPatientId() {
		return patientId;
	}
	public void setPatientId(Long patientId) {
		this.patientId = patientId;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public AuthResponse(String token, Long patientId, String name, String email) {
		super();
		this.token = token;
		this.patientId = patientId;
		this.name = name;
		this.email = email;
	}
    
	
	
    
}
