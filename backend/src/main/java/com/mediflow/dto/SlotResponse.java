package com.mediflow.dto;
import java.time.LocalTime;
public record SlotResponse(LocalTime timeSlot, boolean booked) {}
