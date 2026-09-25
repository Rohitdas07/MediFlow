package com.mediflow.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

public class AppointmentRequest {
    @NotNull private Long doctorId;
    @NotNull private LocalDate appointmentDate;
    @NotNull private LocalTime timeSlot;
    @Size(max = 1000) private String chiefComplaint;
    public Long getDoctorId(){return doctorId;} public void setDoctorId(Long v){doctorId=v;}
    public LocalDate getAppointmentDate(){return appointmentDate;} public void setAppointmentDate(LocalDate v){appointmentDate=v;}
    public LocalTime getTimeSlot(){return timeSlot;} public void setTimeSlot(LocalTime v){timeSlot=v;}
    public String getChiefComplaint(){return chiefComplaint;} public void setChiefComplaint(String v){chiefComplaint=v;}
}
