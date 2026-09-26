package com.mediflow.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provider-neutral toll-free emergency gateway.
 *
 * A telephony provider can POST its inbound call webhook to /api/toll-free/calls/inbound.
 * The provider-specific adapter can then translate its payload into the same call
 * session contract used by the MediFlow web experience.
 */
@RestController
@RequestMapping("/api/toll-free")
public class TollFreeController {

    private final Map<String, Map<String, Object>> calls = new ConcurrentHashMap<>();

    @PostMapping(value = "/calls", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> receiveCallSession(@RequestBody Map<String, Object> session) {
        String callId = Objects.toString(session.get("callId"), "TF-" + UUID.randomUUID());
        Map<String, Object> stored = new LinkedHashMap<>(session);
        stored.put("callId", callId);
        stored.putIfAbsent("receivedAt", Instant.now().toString());
        calls.put(callId, stored);
        return stored;
    }

    @PostMapping("/calls/inbound")
    public Map<String, Object> inboundProviderCall(@RequestParam(required = false) String callId,
                                                     @RequestParam(required = false) String callerNumber,
                                                     @RequestParam(required = false) String provider) {
        String id = callId == null || callId.isBlank() ? "TF-" + UUID.randomUUID() : callId;
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("callId", id);
        response.put("status", "CONNECTED");
        response.put("provider", provider == null ? "telephony-provider" : provider);
        response.put("callerNumber", callerNumber == null ? "unknown" : callerNumber);
        response.put("nextStep", "IVR_AI_ASSISTANT");
        response.put("workflow", List.of(
                "collect patient name",
                "collect location",
                "collect symptoms",
                "collect emergency details",
                "collect available vital signs",
                "classify LOW/MODERATE/HIGH",
                "route hospital/ambulance/doctor",
                "create digital patient summary"
        ));
        response.put("receivedAt", Instant.now().toString());
        calls.put(id, response);
        return response;
    }

    @GetMapping("/calls/{callId}")
    public Map<String, Object> getCall(@PathVariable String callId) {
        Map<String, Object> result = calls.get(callId);
        if (result == null) {
            throw new NoSuchElementException("Toll-free call not found: " + callId);
        }
        return result;
    }

    @GetMapping("/calls")
    public Collection<Map<String, Object>> listCalls() {
        return calls.values();
    }
}
