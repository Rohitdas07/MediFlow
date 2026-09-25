package com.mediflow.service;

import com.mediflow.dto.AppointmentRequest;
import com.mediflow.model.Appointment;
import com.mediflow.model.Doctor;
import com.mediflow.model.DoctorAvailability;
import com.mediflow.model.Patient;
import com.mediflow.repository.AppointmentRepository;
import com.mediflow.repository.DoctorRepository;
import com.mediflow.repository.PatientRepository;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.util.List;

@Service
public class AppointmentService {
    private final AppointmentRepository appointments;
    private final DoctorRepository doctors;
    private final PatientRepository patients;

    public AppointmentService(AppointmentRepository appointments, DoctorRepository doctors, PatientRepository patients) {
        this.appointments = appointments; this.doctors = doctors; this.patients = patients;
    }

    public Doctor doctor(Long doctorId) { return doctors.findById(doctorId).orElseThrow(() -> new IllegalArgumentException("Doctor not found.")); }
    public boolean isBooked(Long doctorId, java.time.LocalDate date, java.time.LocalTime time) { return appointments.existsByDoctorIdAndAppointmentDateAndTimeSlotAndStatus(doctorId, date, time, "BOOKED"); }

    public List<Appointment> forPatient(Long patientId) { return appointments.findByPatientIdOrderByAppointmentDateDescTimeSlotDesc(patientId); }

    public Appointment book(Long patientId, AppointmentRequest request) {
        if (request.getAppointmentDate().isBefore(java.time.LocalDate.now())) throw new IllegalArgumentException("Appointment date cannot be in the past.");
        Doctor doctor = doctors.findById(request.getDoctorId()).orElseThrow(() -> new IllegalArgumentException("Doctor not found."));
        boolean works = doctor.getAvailability().stream().anyMatch(a -> a.getDayOfWeek() == request.getAppointmentDate().getDayOfWeek()
                && !request.getTimeSlot().isBefore(a.getStartTime()) && request.getTimeSlot().isBefore(a.getEndTime()));
        if (!works) throw new IllegalArgumentException("Doctor is not available at that time.");
        if (appointments.existsByDoctorIdAndAppointmentDateAndTimeSlotAndStatus(doctor.getId(), request.getAppointmentDate(), request.getTimeSlot(), "BOOKED"))
            throw new IllegalArgumentException("That appointment slot is already booked.");
        Patient patient = patients.findById(patientId).orElseThrow(() -> new IllegalArgumentException("Patient not found."));
        Appointment a = new Appointment(); a.setPatient(patient); a.setDoctor(doctor); a.setAppointmentDate(request.getAppointmentDate());
        a.setTimeSlot(request.getTimeSlot()); a.setChiefComplaint(request.getChiefComplaint()); a.setStatus("BOOKED");
        return appointments.save(a);
    }

    public void cancel(Long patientId, Long appointmentId) {
        Appointment a = appointments.findById(appointmentId).orElseThrow(() -> new IllegalArgumentException("Appointment not found."));
        if (!a.getPatient().getId().equals(patientId)) throw new IllegalArgumentException("You can only cancel your own appointments.");
        if (!"BOOKED".equals(a.getStatus())) throw new IllegalArgumentException("Appointment is already " + a.getStatus().toLowerCase() + ".");
        a.setStatus("CANCELLED"); appointments.save(a);
    }
}
