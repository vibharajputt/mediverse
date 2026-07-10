package com.mediverse.controller;

import com.mediverse.dto.ApiResponse;
import com.mediverse.dto.PharmacyPriceRequest;
import com.mediverse.model.PharmacyMedicine;
import com.mediverse.model.User;
import com.mediverse.security.JwtTokenProvider;
import com.mediverse.service.AuthService;
import com.mediverse.service.PharmacyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/pharmacy")
public class PharmacyController {

    @Autowired
    private PharmacyService pharmacyService;

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @PostMapping("/prices")
    public ResponseEntity<?> setPrices(
            @RequestBody PharmacyPriceRequest request,
            @RequestHeader("Authorization") String authHeader) {
        try {
            Long pharmacyId = getUserIdFromToken(authHeader);
            List<PharmacyMedicine> medicines = pharmacyService.setPrices(request, pharmacyId);
            return ResponseEntity.ok(ApiResponse.success("Prices set successfully", 
                medicines.stream().map(this::toMap).collect(Collectors.toList())));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/medicines")
    public ResponseEntity<?> getPharmacyMedicines(@RequestHeader("Authorization") String authHeader) {
        try {
            Long pharmacyId = getUserIdFromToken(authHeader);
            List<PharmacyMedicine> medicines = pharmacyService.getPharmacyMedicines(pharmacyId);
            return ResponseEntity.ok(medicines.stream().map(this::toMap).collect(Collectors.toList()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/prescription/{prescriptionId}")
    public ResponseEntity<?> getPharmaciesForPrescription(@PathVariable Long prescriptionId) {
        try {
            Map<String, Object> result = pharmacyService.getPharmaciesForPrescription(prescriptionId);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllPharmacies() {
        List<User> pharmacies = pharmacyService.getAllPharmacies();
        return ResponseEntity.ok(pharmacies.stream().map(p -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", p.getId());
            map.put("name", p.getName());
            map.put("email", p.getEmail());
            map.put("phone", p.getPhone());
            map.put("address", p.getAddress());
            map.put("city", p.getCity());
            map.put("licenseNo", p.getLicenseNo());
            return map;
        }).collect(Collectors.toList()));
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(
            @RequestBody Map<String, String> profileData,
            @RequestHeader("Authorization") String authHeader) {
        try {
            Long pharmacyId = getUserIdFromToken(authHeader);
            User pharmacy = pharmacyService.updatePharmacyProfile(pharmacyId, profileData);
            return ResponseEntity.ok(ApiResponse.success("Profile updated successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    private Map<String, Object> toMap(PharmacyMedicine pm) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", pm.getId());
        map.put("medicineName", pm.getMedicineName());
        map.put("sellingPrice", pm.getSellingPrice());
        map.put("available", pm.getAvailable());
        map.put("prescriptionId", pm.getPrescription().getId());
        return map;
    }

    private Long getUserIdFromToken(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String email = tokenProvider.getEmailFromToken(token);
        User user = authService.getUserByEmail(email);
        return user.getId();
    }
}
