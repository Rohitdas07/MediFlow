package com.mediflow.controller;

import com.mediflow.dto.*;
import com.mediflow.security.PatientUserDetails;
import com.mediflow.service.AppointmentService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

@RestController @RequestMapping("/api/appointments")
public class AppointmentController {
    private final AppointmentService service;
    public AppointmentController(AppointmentService service){this.service=service;}
    @GetMapping public List<AppointmentResponse> mine(@AuthenticationPrincipal PatientUserDetails user){
        return service.forPatient(user.getPatient().getId()).stream().map(AppointmentResponse::from).toList();
    }
    @GetMapping("/slots")
    public List<SlotResponse> slots(@RequestParam Long doctorId, @RequestParam LocalDate date) {
        var doctor = service.doctor(doctorId);
        boolean works = doctor.getAvailability().stream().anyMatch(a -> a.getDayOfWeek() == date.getDayOfWeek());
        if (!works) return List.of();
        var slots = new java.util.ArrayList<SlotResponse>();
        for (var a : doctor.getAvailability()) {
            if (a.getDayOfWeek() != date.getDayOfWeek()) continue;
            LocalTime t = a.getStartTime();
            while (t.isBefore(a.getEndTime())) {
                boolean booked = service.isBooked(doctorId, date, t);
                slots.add(new SlotResponse(t, booked));
                t = t.plusMinutes(30);
            }
        }
        return slots.stream().distinct().sorted(java.util.Comparator.comparing(SlotResponse::timeSlot)).toList();
    }

    @PostMapping public AppointmentResponse book(@AuthenticationPrincipal PatientUserDetails user, @Valid @RequestBody AppointmentRequest request){
        return AppointmentResponse.from(service.book(user.getPatient().getId(), request));
    }
    @DeleteMapping("/{id}") public void cancel(@AuthenticationPrincipal PatientUserDetails user,@PathVariable Long id){service.cancel(user.getPatient().getId(), id);}
}
