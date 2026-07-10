package com.mediverse.controller;

import com.mediverse.dto.ApiResponse;
import com.mediverse.dto.PrescriptionRequest;
import com.mediverse.dto.PrescriptionAnalysisRequest;
import com.mediverse.model.Prescription;
import com.mediverse.model.User;
import com.mediverse.security.JwtTokenProvider;
import com.mediverse.service.AuthService;
import com.mediverse.service.PrescriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/prescriptions")
public class PrescriptionController {

    @Autowired
    private PrescriptionService prescriptionService;

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @PostMapping
    public ResponseEntity<?> createPrescription(
            @RequestBody PrescriptionRequest request,
            @RequestHeader("Authorization") String authHeader) {
        try {
            Long doctorId = getUserIdFromToken(authHeader);
            Prescription prescription = prescriptionService.createPrescription(request, doctorId);

            Map<String, Object> response = new HashMap<>();
            response.put("id", prescription.getId());
            response.put("diagnosis", prescription.getDiagnosis());
            response.put("hasTests", prescription.getHasTests());
            response.put("routeType", prescription.getRouteType());
            response.put("status", prescription.getStatus());

            return ResponseEntity.ok(ApiResponse.success("Prescription created and analyzed!", response));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/patient")
    public ResponseEntity<?> getPatientPrescriptions(
            @RequestParam(required = false) Long familyMemberId,
            @RequestHeader("Authorization") String authHeader) {
        try {
            Long patientId = getUserIdFromToken(authHeader);
            List<Prescription> prescriptions = prescriptionService.getPatientPrescriptions(patientId, familyMemberId);
            return ResponseEntity.ok(prescriptions.stream().map(this::toMap).collect(Collectors.toList()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/doctor")
    public ResponseEntity<?> getDoctorPrescriptions(@RequestHeader("Authorization") String authHeader) {
        try {
            Long doctorId = getUserIdFromToken(authHeader);
            List<Prescription> prescriptions = prescriptionService.getDoctorPrescriptions(doctorId);
            return ResponseEntity.ok(prescriptions.stream().map(this::toMap).collect(Collectors.toList()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getPrescription(@PathVariable Long id) {
        try {
            Prescription prescription = prescriptionService.getPrescriptionById(id);
            return ResponseEntity.ok(toMap(prescription));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{id}/analyze")
    public ResponseEntity<?> analyzePrescription(@PathVariable Long id) {
        try {
            Map<String, Object> analysis = prescriptionService.analyzePrescription(id);
            return ResponseEntity.ok(ApiResponse.success("AI Analysis Complete", analysis));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/analyze-raw")
    public ResponseEntity<?> analyzeRaw(@RequestBody PrescriptionAnalysisRequest request) {
        try {
            Map<String, Object> result = prescriptionService.analyzeRaw(
                    request.getSymptoms(),
                    request.getMedicine(),
                    request.getPreviousPrescription());
            return ResponseEntity.ok(ApiResponse.success("AI Safety Analysis completed!", result));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/analyze-document")
    public ResponseEntity<?> analyzeDocument(@RequestBody Map<String, String> request) {
        try {
            String fileUrl = request.get("fileUrl");
            String newMedicine = request.get("newMedicine");
            Map<String, Object> result = prescriptionService.analyzeDocument(fileUrl, newMedicine);
            return ResponseEntity.ok(ApiResponse.success("Document safety analysis completed!", result));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/pharmacy-queue")
    public ResponseEntity<?> getPharmacyPrescriptions() {
        try {
            List<Prescription> prescriptions = prescriptionService.getPharmacyPrescriptions();
            return ResponseEntity.ok(prescriptions.stream().map(this::toMap).collect(Collectors.toList()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    private Map<String, Object> toMap(Prescription p) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", p.getId());
        map.put("diagnosis", p.getDiagnosis());
        map.put("medicines", p.getMedicines());
        map.put("tests", p.getTests());
        map.put("notes", p.getNotes());
        map.put("hasTests", p.getHasTests());
        map.put("routeType", p.getRouteType());
        map.put("status", p.getStatus());
        map.put("patientId", p.getPatient().getId());
        map.put("patientName", p.getPatient().getName());
        if (p.getFamilyMember() != null) {
            map.put("familyMemberId", p.getFamilyMember().getId());
            map.put("familyMemberName", p.getFamilyMember().getName());
        }
        map.put("doctorId", p.getDoctor().getId());
        map.put("doctorName", p.getDoctor().getName());
        map.put("createdAt", p.getCreatedAt());
        if (p.getBooking() != null) {
            map.put("bookingId", p.getBooking().getId());
        }
        map.put("reportUrls", p.getReportUrls());
        map.put("aiSummary", p.getAiSummary());
        return map;
    }

    @PutMapping("/{id}/upload-report")
    public ResponseEntity<?> uploadReport(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload) {
        try {
            String reportUrl = payload.get("reportUrl");
            Prescription prescription = prescriptionService.uploadReport(id, reportUrl);
            return ResponseEntity.ok(ApiResponse.success("Report uploaded successfully", toMap(prescription)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    private Long getUserIdFromToken(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String email = tokenProvider.getEmailFromToken(token);
        User user = authService.getUserByEmail(email);
        return user.getId();
    }
}
