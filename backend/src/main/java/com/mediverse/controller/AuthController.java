package com.mediverse.controller;

import com.mediverse.dto.ApiResponse;
import com.mediverse.dto.AuthResponse;
import com.mediverse.dto.LoginRequest;
import com.mediverse.dto.SignupRequest;
import com.mediverse.model.PatientProfile;
import com.mediverse.model.User;
import com.mediverse.security.JwtTokenProvider;
import com.mediverse.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private com.mediverse.service.LicenseVerificationService licenseVerificationService;

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody SignupRequest request) {
        try {
            AuthResponse response = authService.signup(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid email or password"));
        }
    }

    @GetMapping("/verify-license")
    public ResponseEntity<?> verifyLicense(@RequestParam String licenseNo) {
        try {
            boolean valid = licenseVerificationService.verifyLicense(licenseNo);
            if (valid) {
                return ResponseEntity.ok(ApiResponse.success("License verified"));
            }
            return ResponseEntity.badRequest().body(ApiResponse.error("License number not recognized."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("License verification failed."));
        }
    }

    @GetMapping("/verify-pharmacy-license")
    public ResponseEntity<?> verifyPharmacyLicense(@RequestParam String licenseNo) {
        try {
            boolean valid = licenseVerificationService.verifyPharmacyLicense(licenseNo);
            if (valid) {
                return ResponseEntity.ok(ApiResponse.success("Pharmacy license verified"));
            }
            return ResponseEntity.badRequest().body(ApiResponse.error("Pharmacy license number not recognized."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Pharmacy license verification failed."));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader("Authorization") String token) {
        try {
            // This endpoint is protected by JWT filter
            return ResponseEntity.ok(ApiResponse.success("Authenticated"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            String email = tokenProvider.getEmailFromToken(token);
            User user = authService.getUserByEmail(email);

            Map<String, Object> response = new HashMap<>();
            response.put("id", user.getId());
            response.put("name", user.getName());
            response.put("email", user.getEmail());
            response.put("phone", user.getPhone());
            response.put("role", user.getRole().name());
            response.put("city", user.getCity());
            response.put("address", user.getAddress());
            response.put("avatarUrl", user.getAvatarUrl());
            response.put("hospitalId", user.getHospitalId());

            if (user.getRole() == User.Role.PATIENT) {
                PatientProfile profile = authService.getPatientProfile(user.getId());
                if (profile != null) {
                    response.put("dob", profile.getDob());
                    response.put("age", profile.getAge());
                    response.put("gender", profile.getGender());
                    response.put("bloodGroup", profile.getBloodGroup());
                    response.put("emergencyNumber", profile.getEmergencyNumber());
                    response.put("preferredLanguage", profile.getPreferredLanguage());
                    response.put("existingMedicalCondition", profile.getExistingMedicalCondition());
                    response.put("idProof", profile.getIdProof());
                    response.put("currentMedication", profile.getCurrentMedication());
                    response.put("allergies", profile.getAllergies());
                    response.put("isFirstTimeUser", profile.getIsFirstTimeUser());
                    response.put("prescriptionReportUrl", profile.getPrescriptionReportUrl());
                    response.put("expPoints", profile.getExpPoints());
                    response.put("streakDays", profile.getStreakDays());
                    response.put("healthBadge", profile.getHealthBadge());
                    response.put("lastAnalysis", profile.getLastAnalysis());
                    response.put("carePlan", profile.getCarePlan());
                    response.put("lastChecklistDate", profile.getLastChecklistDate() != null ? profile.getLastChecklistDate().toString() : null);
                    response.put("medsChecked", profile.getMedsChecked());
                    response.put("dietChecked", profile.getDietChecked());
                    response.put("exerciseChecked", profile.getExerciseChecked());
                }
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/patient/{id}")
    public ResponseEntity<?> getPatientProfileForDoctor(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        try {
            User user = authService.getUserById(id);
            Map<String, Object> response = new HashMap<>();
            response.put("id", user.getId());
            response.put("name", user.getName());
            response.put("email", user.getEmail());
            response.put("phone", user.getPhone());
            response.put("role", user.getRole().name());
            response.put("city", user.getCity());
            response.put("address", user.getAddress());

            PatientProfile profile = authService.getPatientProfile(user.getId());
            if (profile != null) {
                response.put("dob", profile.getDob());
                response.put("age", profile.getAge());
                response.put("gender", profile.getGender());
                response.put("bloodGroup", profile.getBloodGroup());
                response.put("emergencyNumber", profile.getEmergencyNumber());
                response.put("preferredLanguage", profile.getPreferredLanguage());
                response.put("existingMedicalCondition", profile.getExistingMedicalCondition());
                response.put("currentMedication", profile.getCurrentMedication());
                response.put("allergies", profile.getAllergies());
                response.put("healthBadge", profile.getHealthBadge());
                response.put("lastAnalysis", profile.getLastAnalysis());
                response.put("carePlan", profile.getCarePlan());
            } else {
                response.put("dob", "");
                response.put("age", 30);
                response.put("gender", "Not Specified");
                response.put("bloodGroup", "O+ (Confirmed)");
                response.put("emergencyNumber", "+91 98765 43210");
                response.put("preferredLanguage", "English");
                response.put("existingMedicalCondition", "None");
                response.put("currentMedication", "None");
                response.put("allergies", "None");
                response.put("healthBadge", null);
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }


    @PutMapping("/profile/avatar")
    public ResponseEntity<?> updateAvatar(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> request) {
        try {
            String token = authHeader.replace("Bearer ", "");
            String email = tokenProvider.getEmailFromToken(token);
            String avatarUrl = request.get("avatarUrl");
            authService.updateAvatarUrl(email, avatarUrl);
            return ResponseEntity.ok(ApiResponse.success("Avatar updated successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> request) {
        try {
            String token = authHeader.replace("Bearer ", "");
            String email = tokenProvider.getEmailFromToken(token);
            User user = authService.getUserByEmail(email);

            authService.updatePatientProfile(user.getId(), request);
            return ResponseEntity.ok(ApiResponse.success("Profile updated successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/verify-upi")
    public ResponseEntity<?> verifyUpi(@RequestParam String upiId) {
        try {
            java.util.Map<String, Object> result = authService.verifyUpiId(upiId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/google-login")
    public ResponseEntity<?> googleLogin(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Email is required"));
            }
            AuthResponse response = authService.googleLogin(email);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            String newPassword = request.get("newPassword");
            if (email == null || newPassword == null || newPassword.length() < 6) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Email and a password of at least 6 characters are required."));
            }
            authService.resetPassword(email, newPassword);
            return ResponseEntity.ok(ApiResponse.success("Password reset successfully!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/doctors")
    public ResponseEntity<?> getAllDoctors() {
        try {
            return ResponseEntity.ok(authService.getUsersByRole(User.Role.DOCTOR));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/patients")
    public ResponseEntity<?> getAllPatients() {
        try {
            return ResponseEntity.ok(authService.getUsersByRole(User.Role.PATIENT));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/bookings")
    public ResponseEntity<?> getAllBookings() {
        try {
            return ResponseEntity.ok(authService.getAllBookings());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/orders")
    public ResponseEntity<?> getAllOrders() {
        try {
            return ResponseEntity.ok(authService.getAllOrders());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/lab-bookings")
    public ResponseEntity<?> getAllLabBookings() {
        try {
            return ResponseEntity.ok(authService.getAllLabBookings());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
