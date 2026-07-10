package com.mediverse.controller;

import com.mediverse.dto.ApiResponse;
import com.mediverse.model.LabBooking;
import com.mediverse.model.User;
import com.mediverse.security.JwtTokenProvider;
import com.mediverse.service.AuthService;
import com.mediverse.service.LabBookingService;
import com.mediverse.repository.PrescriptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/labs")
public class LabController {

    @Autowired
    private LabBookingService labBookingService;

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @GetMapping("/all")
    public ResponseEntity<?> getAllLabs() {
        List<User> labs = labBookingService.getAllLabs();
        return ResponseEntity.ok(labs.stream().map(l -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", l.getId());
            map.put("name", l.getName());
            map.put("email", l.getEmail());
            map.put("phone", l.getPhone());
            map.put("address", l.getAddress());
            map.put("city", l.getCity());
            map.put("licenseNo", l.getLicenseNo());
            return map;
        }).collect(Collectors.toList()));
    }

    @PostMapping("/bookings")
    public ResponseEntity<?> createBooking(
            @RequestBody Map<String, Object> payload,
            @RequestHeader("Authorization") String authHeader) {
        try {
            Long patientId = getUserIdFromToken(authHeader);
            String labName = (String) payload.get("labName");
            String testsJson = (String) payload.get("testsJson");
            String deliveryAddress = (String) payload.get("deliveryAddress");

            Double testAmount = payload.get("testAmount") != null
                    ? ((Number) payload.get("testAmount")).doubleValue() : null;
            Double collectionCharges = payload.get("collectionCharges") != null
                    ? ((Number) payload.get("collectionCharges")).doubleValue() : null;
            Double totalAmount = payload.get("totalAmount") != null
                    ? ((Number) payload.get("totalAmount")).doubleValue() : null;
            Long prescriptionId = payload.get("prescriptionId") != null
                    ? ((Number) payload.get("prescriptionId")).longValue() : null;

            LabBooking booking = labBookingService.createBooking(patientId, labName, testsJson, deliveryAddress,
                    testAmount, collectionCharges, totalAmount, prescriptionId);
            return ResponseEntity.ok(ApiResponse.success("Booking created successfully", toMap(booking)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/bookings/lab")
    public ResponseEntity<?> getLabBookings(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            String email = tokenProvider.getEmailFromToken(token);
            User labUser = authService.getUserByEmail(email);
            
            List<LabBooking> bookings = labBookingService.getBookingsByLab(labUser.getName());
            return ResponseEntity.ok(bookings.stream().map(this::toMap).collect(Collectors.toList()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/bookings/patient")
    public ResponseEntity<?> getPatientBookings(@RequestHeader("Authorization") String authHeader) {
        try {
            Long patientId = getUserIdFromToken(authHeader);
            List<LabBooking> bookings = labBookingService.getBookingsByPatient(patientId);
            return ResponseEntity.ok(bookings.stream().map(this::toMap).collect(Collectors.toList()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/bookings/{id}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String status = payload.get("status");
            LabBooking booking = labBookingService.updateStatus(id, status);
            return ResponseEntity.ok(ApiResponse.success("Booking status updated", toMap(booking)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    private Map<String, Object> toMap(LabBooking booking) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", booking.getId());
        map.put("prescriptionId", booking.getPrescriptionId());
        map.put("patientId", booking.getPatient().getId());
        map.put("patientName", booking.getPatient().getName());
        map.put("labName", booking.getLabName());
        map.put("tests", booking.getTestsJson());
        map.put("status", booking.getStatus());
        map.put("deliveryAddress", booking.getDeliveryAddress());
        map.put("createdAt", booking.getCreatedAt().toString());
        map.put("testAmount", booking.getTestAmount());
        map.put("collectionCharges", booking.getCollectionCharges());
        map.put("totalAmount", booking.getTotalAmount());
        
        if (booking.getPrescriptionId() != null) {
            prescriptionRepository.findById(booking.getPrescriptionId()).ifPresent(p -> {
                if (p.getDoctor() != null) {
                    map.put("doctorName", p.getDoctor().getName());
                }
                map.put("reportUrls", p.getReportUrls());
                map.put("aiSummary", p.getAiSummary());
            });
        }
        return map;
    }

    private Long getUserIdFromToken(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String email = tokenProvider.getEmailFromToken(token);
        User user = authService.getUserByEmail(email);
        return user.getId();
    }
}
