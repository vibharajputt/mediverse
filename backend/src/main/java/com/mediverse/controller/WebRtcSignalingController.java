package com.mediverse.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/webrtc")
public class WebRtcSignalingController {

    // Map of bookingId -> participantRole -> signalData
    private final Map<String, Map<String, Object>> signals = new ConcurrentHashMap<>();
    
    // Map of bookingId -> participantRole -> list of ICE candidates
    private final Map<String, Map<String, List<Object>>> iceCandidates = new ConcurrentHashMap<>();

    @PostMapping("/{bookingId}/signal")
    public ResponseEntity<?> sendSignal(
            @PathVariable String bookingId,
            @RequestParam String role,
            @RequestBody Object signalData) {
        signals.computeIfAbsent(bookingId, k -> new ConcurrentHashMap<>()).put(role, signalData);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{bookingId}/signal")
    public ResponseEntity<?> getSignal(
            @PathVariable String bookingId,
            @RequestParam String otherRole) {
        Map<String, Object> bookingSignals = signals.get(bookingId);
        if (bookingSignals != null && bookingSignals.containsKey(otherRole)) {
            return ResponseEntity.ok(bookingSignals.get(otherRole));
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/{bookingId}/ice")
    public ResponseEntity<?> sendIceCandidate(
            @PathVariable String bookingId,
            @RequestParam String role,
            @RequestBody Object candidate) {
        iceCandidates.computeIfAbsent(bookingId, k -> new ConcurrentHashMap<>())
                     .computeIfAbsent(role, k -> new ArrayList<>())
                     .add(candidate);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{bookingId}/ice")
    public ResponseEntity<?> getIceCandidates(
            @PathVariable String bookingId,
            @RequestParam String otherRole) {
        Map<String, List<Object>> bookingCandidates = iceCandidates.get(bookingId);
        if (bookingCandidates != null && bookingCandidates.containsKey(otherRole)) {
            List<Object> candidates = new ArrayList<>(bookingCandidates.get(otherRole));
            return ResponseEntity.ok(candidates);
        }
        return ResponseEntity.ok(new ArrayList<>());
    }

    @DeleteMapping("/{bookingId}/clear")
    public ResponseEntity<?> clearSession(@PathVariable String bookingId) {
        signals.remove(bookingId);
        iceCandidates.remove(bookingId);
        return ResponseEntity.ok().build();
    }
}
