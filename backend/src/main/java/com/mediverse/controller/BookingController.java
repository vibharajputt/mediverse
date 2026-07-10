package com.mediverse.controller;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

import com.mediverse.dto.ApiResponse;
import com.mediverse.dto.BookingRequest;
import com.mediverse.model.Booking;
import com.mediverse.model.User;
import com.mediverse.model.PatientProfile;
import com.mediverse.security.JwtTokenProvider;
import com.mediverse.service.AuthService;
import com.mediverse.service.BookingService;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @PostMapping
    public ResponseEntity<?> createBooking(
            @Valid @RequestBody BookingRequest request,
            @RequestHeader("Authorization") String authHeader) {
        try {
            Long patientId = getUserIdFromToken(authHeader);
            Booking booking = bookingService.createBooking(request, patientId);

            Map<String, Object> response = new HashMap<>();
            response.put("id", booking.getId());
            response.put("bookingDate", booking.getBookingDate());
            response.put("timeSlot", booking.getTimeSlot());
            response.put("type", booking.getType());
            response.put("status", booking.getStatus());
            response.put("hospitalName", booking.getHospital().getName());
            response.put("doctorName", booking.getDoctor().getName());
            response.put("patientName", booking.getPatientName());

            return ResponseEntity.ok(ApiResponse.success("Booking created successfully! Confirmation email sent.", response));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getBookingById(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        try {
            Booking booking = bookingService.getBookingById(id);
            return ResponseEntity.ok(toMap(booking));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/patient")
    public ResponseEntity<?> getPatientBookings(
            @RequestParam(required = false) Long familyMemberId,
            @RequestHeader("Authorization") String authHeader) {
        try {
            Long patientId = getUserIdFromToken(authHeader);
            List<Booking> bookings = bookingService.getPatientBookings(patientId, familyMemberId);
            return ResponseEntity.ok(bookings.stream().map(this::toMap).collect(Collectors.toList()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/doctor")
    public ResponseEntity<?> getDoctorBookings(@RequestHeader("Authorization") String authHeader) {
        try {
            Long doctorId = getUserIdFromToken(authHeader);
            List<Booking> bookings = bookingService.getDoctorBookings(doctorId);
            return ResponseEntity.ok(bookings.stream().map(this::toMap).collect(Collectors.toList()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestHeader("Authorization") String authHeader) {
        try {
            Long userId = getUserIdFromToken(authHeader);
            Booking booking = bookingService.updateBookingStatus(id, status, userId);
            return ResponseEntity.ok(ApiResponse.success("Booking status updated to " + status));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}/reschedule")
    public ResponseEntity<?> rescheduleBooking(
            @PathVariable Long id,
            @RequestParam String date,
            @RequestParam String timeSlot,
            @RequestHeader("Authorization") String authHeader) {
        try {
            Long userId = getUserIdFromToken(authHeader);
            LocalDate newDate = LocalDate.parse(date);
            Booking booking = bookingService.rescheduleBooking(id, newDate, timeSlot, userId);
            return ResponseEntity.ok(ApiResponse.success("Booking rescheduled successfully!", toMap(booking)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}/meeting-link")
    public ResponseEntity<?> updateMeetingLink(
            @PathVariable Long id,
            @RequestParam String meetingLink,
            @RequestHeader("Authorization") String authHeader) {
        try {
            Booking booking = bookingService.updateMeetingLink(id, meetingLink);
            return ResponseEntity.ok(ApiResponse.success("Meeting link updated successfully!", toMap(booking)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}/ai-report")
    public ResponseEntity<?> updateAiReport(
            @PathVariable Long id,
            @RequestBody Map<String, String> requestBody,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String aiReport = requestBody.getOrDefault("aiReport", "").trim();
            Booking booking = bookingService.updateAiReport(id, aiReport);
            return ResponseEntity.ok(ApiResponse.success("AI Report updated successfully!", toMap(booking)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/slots")
    public ResponseEntity<?> getAvailableSlots(
            @RequestParam Long doctorId,
            @RequestParam String date) {
        try {
            LocalDate bookingDate = LocalDate.parse(date);
            List<String> slots = bookingService.getAvailableTimeSlots(doctorId, bookingDate);
            return ResponseEntity.ok(slots);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    private Map<String, Object> toMap(Booking booking) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", booking.getId());
        map.put("patientId", booking.getPatient().getId());
        map.put("bookingDate", booking.getBookingDate());
        map.put("timeSlot", booking.getTimeSlot());
        map.put("type", booking.getType());
        map.put("status", booking.getStatus());
        map.put("notes", booking.getNotes());
        map.put("patientName", booking.getPatientName());
        map.put("patientPhone", booking.getPatientPhone());
        map.put("age", booking.getAge());
        map.put("gender", booking.getGender());
        if (booking.getFamilyMember() != null) {
            map.put("familyMemberId", booking.getFamilyMember().getId());
            map.put("familyMemberName", booking.getFamilyMember().getName());
        }
        map.put("symptoms", booking.getSymptoms());
        map.put("paymentMethod", booking.getPaymentMethod());
        map.put("paymentStatus", booking.getPaymentStatus());
        map.put("hospitalId", booking.getHospital().getId());
        map.put("hospitalName", booking.getHospital().getName());
        map.put("doctorId", booking.getDoctor().getId());
        map.put("doctorName", booking.getDoctor().getName());
        map.put("createdAt", booking.getCreatedAt());
        String meetingLink = booking.getMeetingLink();
        if (meetingLink != null && meetingLink.contains("meet.jit.si")) {
            meetingLink = null;
        }
        map.put("meetingLink", meetingLink);
        map.put("aiReport", booking.getAiReport());
        map.put("conditionBadge", booking.getConditionBadge());
        map.put("conditionBadgeReason", booking.getConditionBadgeReason());
        map.put("previousPrescriptionSummary", booking.getPreviousPrescriptionSummary());
        
        PatientProfile pProfile = authService.getPatientProfile(booking.getPatient().getId());
        map.put("healthBadge", pProfile != null ? pProfile.getHealthBadge() : null);
        return map;
    }

    private Long getUserIdFromToken(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String email = tokenProvider.getEmailFromToken(token);
        User user = authService.getUserByEmail(email);
        return user.getId();
    }
}
