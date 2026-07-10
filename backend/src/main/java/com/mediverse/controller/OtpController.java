package com.mediverse.controller;

import com.mediverse.dto.ApiResponse;
import com.mediverse.dto.OtpRequest;
import com.mediverse.model.OtpVerification;
import com.mediverse.service.OtpService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/otp")
public class OtpController {

    @Autowired
    private OtpService otpService;

    /**
     * Send OTP to email or phone
     */
    @PostMapping("/send")
    public ResponseEntity<?> sendOtp(@Valid @RequestBody OtpRequest request) {
        try {
            OtpVerification.OtpType otpType = OtpVerification.OtpType.valueOf(request.getType().toUpperCase());
            otpService.sendOtp(request.getIdentifier(), otpType);
            return ResponseEntity.ok(ApiResponse.success(
                    "OTP sent successfully to " + maskIdentifier(request.getIdentifier(), otpType)
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid OTP type. Use EMAIL or PHONE."));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to send OTP: " + e.getMessage()));
        }
    }

    /**
     * Verify OTP
     */
    @PostMapping("/verify")
    public ResponseEntity<?> verifyOtp(@Valid @RequestBody OtpRequest request) {
        try {
            if (request.getOtp() == null || request.getOtp().isBlank()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("OTP is required for verification."));
            }

            OtpVerification.OtpType otpType = OtpVerification.OtpType.valueOf(request.getType().toUpperCase());
            boolean verified = otpService.verifyOtp(request.getIdentifier(), request.getOtp(), otpType);

            if (verified) {
                return ResponseEntity.ok(ApiResponse.success("OTP verified successfully! ✅"));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Invalid or expired OTP. Please try again."));
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid OTP type. Use EMAIL or PHONE."));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Verification failed: " + e.getMessage()));
        }
    }

    /**
     * Check verification status
     */
    @GetMapping("/status")
    public ResponseEntity<?> checkStatus(
            @RequestParam String identifier,
            @RequestParam String type) {
        try {
            OtpVerification.OtpType otpType = OtpVerification.OtpType.valueOf(type.toUpperCase());
            boolean verified = otpService.isVerified(identifier, otpType);
            return ResponseEntity.ok(ApiResponse.success(
                    verified ? "Verified" : "Not verified",
                    verified
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Mask identifier for privacy in response messages
     */
    private String maskIdentifier(String identifier, OtpVerification.OtpType type) {
        if (type == OtpVerification.OtpType.EMAIL) {
            int atIndex = identifier.indexOf('@');
            if (atIndex > 2) {
                return identifier.substring(0, 2) + "***" + identifier.substring(atIndex);
            }
            return "***" + identifier.substring(atIndex);
        } else {
            if (identifier.length() > 4) {
                return "***" + identifier.substring(identifier.length() - 4);
            }
            return "***" + identifier;
        }
    }
}
