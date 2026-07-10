package com.mediverse.controller;

import com.mediverse.dto.ApiResponse;
import com.mediverse.model.PatientProfile;
import com.mediverse.model.User;
import com.mediverse.repository.PatientProfileRepository;
import com.mediverse.security.JwtTokenProvider;
import com.mediverse.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rewards")
public class PatientRewardsController {

    @Autowired
    private PatientProfileRepository patientProfileRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @PostMapping("/checklist")
    public ResponseEntity<?> updateChecklist(
            @RequestBody Map<String, Boolean> payload,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            String email = tokenProvider.getEmailFromToken(token);
            User user = authService.getUserByEmail(email);

            PatientProfile profile = authService.getPatientProfile(user.getId());
            if (profile == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Patient profile not found"));
            }

            LocalDate today = LocalDate.now();
            LocalDate lastDate = profile.getLastChecklistDate();

            // Reset checklist if it's a new day
            if (lastDate == null || !lastDate.equals(today)) {
                // If it is a completely new day, we reset checkbox state in database before applying updates
                profile.setMedsChecked(false);
                profile.setDietChecked(false);
                profile.setExerciseChecked(false);
            }

            boolean reqMeds = payload.getOrDefault("medsChecked", false);
            boolean reqDiet = payload.getOrDefault("dietChecked", false);
            boolean reqExercise = payload.getOrDefault("exerciseChecked", false);

            int expDiff = 0;

            // Medicine: +10 EXP
            if (reqMeds && !profile.getMedsChecked()) {
                expDiff += 10;
            } else if (!reqMeds && profile.getMedsChecked()) {
                expDiff -= 10;
            }

            // Diet: +15 EXP
            if (reqDiet && !profile.getDietChecked()) {
                expDiff += 15;
            } else if (!reqDiet && profile.getDietChecked()) {
                expDiff -= 15;
            }

            // Exercise: +20 EXP
            if (reqExercise && !profile.getExerciseChecked()) {
                expDiff += 20;
            } else if (!reqExercise && profile.getExerciseChecked()) {
                expDiff -= 20;
            }

            // Apply updates
            profile.setMedsChecked(reqMeds);
            profile.setDietChecked(reqDiet);
            profile.setExerciseChecked(reqExercise);
            profile.setExpPoints(Math.max(0, profile.getExpPoints() + expDiff));

            // Check if all are completed to handle streaks
            boolean allChecked = reqMeds && reqDiet && reqExercise;
            boolean wasAllChecked = lastDate != null && lastDate.equals(today) && 
                                   profile.getMedsChecked() && profile.getDietChecked() && profile.getExerciseChecked();

            if (allChecked && !wasAllChecked) {
                if (lastDate == null) {
                    profile.setStreakDays(1);
                } else if (lastDate.equals(today.minusDays(1))) {
                    profile.setStreakDays(profile.getStreakDays() + 1);
                    // 7-day streak bonus: +100 EXP
                    if (profile.getStreakDays() % 7 == 0) {
                        profile.setExpPoints(profile.getExpPoints() + 100);
                    }
                } else if (!lastDate.equals(today)) {
                    profile.setStreakDays(1);
                }
                profile.setLastChecklistDate(today);
            } else if (lastDate == null || !lastDate.equals(today)) {
                profile.setLastChecklistDate(today);
            }

            patientProfileRepository.save(profile);

            Map<String, Object> response = new HashMap<>();
            response.put("expPoints", profile.getExpPoints());
            response.put("streakDays", profile.getStreakDays());
            response.put("medsChecked", profile.getMedsChecked());
            response.put("dietChecked", profile.getDietChecked());
            response.put("exerciseChecked", profile.getExerciseChecked());
            response.put("lastChecklistDate", profile.getLastChecklistDate().toString());

            return ResponseEntity.ok(ApiResponse.success("Daily checklist updated!", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<?> getLeaderboard(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            String email = tokenProvider.getEmailFromToken(token);
            User currentUser = authService.getUserByEmail(email);

            List<PatientProfile> profiles = patientProfileRepository.findTop10ByOrderByExpPointsDesc();
            List<Map<String, Object>> leaderboard = new ArrayList<>();

            for (PatientProfile profile : profiles) {
                if (profile.getUser() == null) continue;
                
                Map<String, Object> entry = new HashMap<>();
                entry.put("name", profile.getUser().getName());
                entry.put("exp", profile.getExpPoints());
                entry.put("badge", profile.getHealthBadge() != null ? profile.getHealthBadge() : "STABLE");
                entry.put("isCurrentUser", profile.getUser().getId().equals(currentUser.getId()));
                leaderboard.add(entry);
            }

            return ResponseEntity.ok(ApiResponse.success("Leaderboard retrieved successfully", leaderboard));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
