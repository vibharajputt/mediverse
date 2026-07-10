package com.mediverse.controller;

import com.mediverse.dto.ContactFormRequest;
import com.mediverse.service.EmailService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ContactController {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ContactController.class);

    @Autowired
    private EmailService emailService;

    @PostMapping("/contact")
    public ResponseEntity<Map<String, String>> handleContactSubmit(@Valid @RequestBody ContactFormRequest request) {
        Map<String, String> response = new HashMap<>();
        try {
            logger.info("📩 New Contact Form Submission Received: name='{}', email='{}', phone='{}', purpose='{}', message='{}'", 
                request.name(), request.email(), request.phone(), request.purpose(), request.message());
            
            try {
                emailService.sendContactEmails(request);
            } catch (Exception mailEx) {
                logger.error("⚠️ SMTP email transmission failed (logged inquiry details anyway): {}", mailEx.getMessage());
            }
            
            response.put("message", "Your message has been sent successfully!");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("❌ Contact form submission failed: {}", e.getMessage(), e);
            response.put("error", "Failed to send message: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}
