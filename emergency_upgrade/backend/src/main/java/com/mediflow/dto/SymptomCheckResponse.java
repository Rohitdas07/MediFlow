package com.mediflow.dto;

import com.mediflow.model.Department;
import java.util.List;

public class SymptomCheckResponse {
    private boolean critical;
    private Department suggestedDepartment;
    private List<String> matchedKeywords;
    private String message;
	public boolean isCritical() {
		return critical;
	}
	public void setCritical(boolean critical) {
		this.critical = critical;
	}
	public Department getSuggestedDepartment() {
		return suggestedDepartment;
	}
	public void setSuggestedDepartment(Department suggestedDepartment) {
		this.suggestedDepartment = suggestedDepartment;
	}
	public List<String> getMatchedKeywords() {
		return matchedKeywords;
	}
	public void setMatchedKeywords(List<String> matchedKeywords) {
		this.matchedKeywords = matchedKeywords;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public SymptomCheckResponse(boolean critical, Department suggestedDepartment, List<String> matchedKeywords,
			String message) {
		super();
		this.critical = critical;
		this.suggestedDepartment = suggestedDepartment;
		this.matchedKeywords = matchedKeywords;
		this.message = message;
	}
    
    
    
}
