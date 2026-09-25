package com.mediflow.dto;

import com.mediflow.model.Appointment;
import java.time.LocalDate;
import java.time.LocalTime;

public record AppointmentResponse(Long id, Long doctorId, String doctorName, String department,
                                  LocalDate appointmentDate, LocalTime timeSlot, String chiefComplaint,
                                  String status) {
    public static AppointmentResponse from(Appointment a) {
        return new AppointmentResponse(a.getId(), a.getDoctor().getId(), a.getDoctor().getName(),
                a.getDoctor().getDepartment().getName(), a.getAppointmentDate(), a.getTimeSlot(),
                a.getChiefComplaint(), a.getStatus());
    }
}
