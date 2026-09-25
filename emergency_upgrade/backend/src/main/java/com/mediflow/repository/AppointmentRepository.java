package com.mediflow.repository;

import com.mediflow.model.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByPatientIdOrderByAppointmentDateDescTimeSlotDesc(Long patientId);
    boolean existsByDoctorIdAndAppointmentDateAndTimeSlotAndStatus(Long doctorId, LocalDate date, LocalTime timeSlot, String status);
}
