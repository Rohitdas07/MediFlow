package com.mediflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class MedicalHistoryRequest {
    @NotBlank @Size(max = 120) private String category;
    @NotBlank @Size(max = 2000) private String details;
    public String getCategory(){return category;} public void setCategory(String v){category=v;}
    public String getDetails(){return details;} public void setDetails(String v){details=v;}
}
