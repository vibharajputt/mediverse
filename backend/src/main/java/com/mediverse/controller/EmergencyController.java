package com.mediverse.controller;

import com.mediverse.dto.ApiResponse;
import com.mediverse.model.PatientProfile;
import com.mediverse.model.User;
import com.mediverse.security.JwtTokenProvider;
import com.mediverse.service.AuthService;
import com.mediverse.service.TwilioSmsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class EmergencyController {

    @Autowired
    private AuthService authService;

    @Autowired
    private TwilioSmsService twilioSmsService;

    @Autowired
    private JwtTokenProvider tokenProvider;

    private final java.util.concurrent.ScheduledExecutorService scheduler = java.util.concurrent.Executors.newScheduledThreadPool(1);

    @PostMapping("/sos")
    public ResponseEntity<?> triggerSOS(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody SosRequest request) {
        try {
            String token = authHeader.replace("Bearer ", "");
            String email = tokenProvider.getEmailFromToken(token);
            User user = authService.getUserByEmail(email);

            PatientProfile profile = authService.getPatientProfile(user.getId());
            String emergencyContact = (profile != null) ? profile.getEmergencyNumber() : null;

            if (emergencyContact == null || emergencyContact.trim().isEmpty()) {
                emergencyContact = user.getPhone();
            }

            String trackingLink = request.getTrackingLink();
            if (trackingLink != null) {
                String localIp = getLocalIpAddress();
                trackingLink = trackingLink.replace("localhost", localIp).replace("127.0.0.1", localIp);
            }

            String messageBody = String.format(
                    "MedAstraX SOS: %s. Hosp: %s. Link: %s",
                    user.getName(),
                    request.getHospitalName(),
                    trackingLink
            );

            // Trigger Twilio SMS
            if (emergencyContact != null && !emergencyContact.trim().isEmpty()) {
                twilioSmsService.sendSms(emergencyContact, messageBody);

                // Trigger Twilio Voice Call to wake them up
                try {
                    String voiceMessage = String.format(
                            "Emergency alert from MedAstraX. %s needs immediate help. Dispatched to %s. Please check your text messages immediately for the live ambulance tracking link.",
                            user.getName(),
                            request.getHospitalName()
                    );
                    twilioSmsService.makeVoiceCall(emergencyContact, voiceMessage);
                } catch (Exception ex) {
                    System.err.println("❌ Failed to initiate Twilio Voice Call: " + ex.getMessage());
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("sentTo", emergencyContact);
            result.put("messageBody", messageBody);
            result.put("status", "SUCCESS");

            return ResponseEntity.ok(ApiResponse.success("SOS Alert dispatched successfully!", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to trigger SOS alert: " + e.getMessage()));
        }
    }

    private String getLocalIpAddress() {
        try {
            java.util.Enumeration<java.net.NetworkInterface> interfaces = java.net.NetworkInterface.getNetworkInterfaces();
            String fallbackIp = null;
            while (interfaces.hasMoreElements()) {
                java.net.NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) continue;

                String displayName = iface.getDisplayName().toLowerCase();
                String name = iface.getName().toLowerCase();
                
                boolean isVirtual = false;
                for (String keyword : new String[]{"virtual", "switch", "wsl", "vmware", "virtualbox", "host-only", "docker", "vbox", "vpn", "bluetooth"}) {
                    if (displayName.contains(keyword) || name.contains(keyword)) {
                        isVirtual = true;
                        break;
                    }
                }

                java.util.Enumeration<java.net.InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    java.net.InetAddress addr = addresses.nextElement();
                    if (addr instanceof java.net.Inet4Address) {
                        String ip = addr.getHostAddress();
                        if (isVirtual) {
                            if (fallbackIp == null) {
                                fallbackIp = ip;
                            }
                        } else {
                            if (ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172.")) {
                                return ip;
                            }
                        }
                    }
                }
            }
            if (fallbackIp != null) return fallbackIp;
            return java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }

    public static class SosRequest {
        private String hospitalName;
        private String hospitalPhone;
        private String hospitalAddress;
        private Double userLatitude;
        private Double userLongitude;
        private String trackingLink;

        public SosRequest() {}

        public String getHospitalName() { return hospitalName; }
        public void setHospitalName(String hospitalName) { this.hospitalName = hospitalName; }

        public String getHospitalPhone() { return hospitalPhone; }
        public void setHospitalPhone(String hospitalPhone) { this.hospitalPhone = hospitalPhone; }

        public String getHospitalAddress() { return hospitalAddress; }
        public void setHospitalAddress(String hospitalAddress) { this.hospitalAddress = hospitalAddress; }

        public Double getUserLatitude() { return userLatitude; }
        public void setUserLatitude(Double userLatitude) { this.userLatitude = userLatitude; }

        public Double getUserLongitude() { return userLongitude; }
        public void setUserLongitude(Double userLongitude) { this.userLongitude = userLongitude; }

        public String getTrackingLink() { return trackingLink; }
        public void setTrackingLink(String trackingLink) { this.trackingLink = trackingLink; }
    }
}
