package com.nutzycraft.pilotai.controller;

import com.nutzycraft.pilotai.entity.User;
import com.nutzycraft.pilotai.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getMyProfile(@RequestParam(required = false) String userId,
            @RequestParam(required = false) String email) {
        // In a real app, get ID from Session/Principal.
        // Here, we trust the client for "dev mode" simplicity, or use query param.

        User user = null;
        if (userId != null) {
            user = userRepository.findById(UUID.fromString(userId)).orElse(null);
        } else if (email != null) {
            user = userRepository.findByEmail(email).orElse(null);
        }

        if (user == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "User not found or not logged in.");
            return ResponseEntity.status(401).body(error);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("id", user.getId());
        response.put("fullName", user.getFullName());
        response.put("email", user.getEmail());
        response.put("avatarUrl", user.getAvatarUrl());
        response.put("isVerified", user.isVerified());
        response.put("createdAt", user.getCreatedAt().toString());

        return ResponseEntity.ok(response);
    }

    @PutMapping("/me")
    public ResponseEntity<Map<String, Object>> updateMyProfile(@RequestBody Map<String, Object> payload) {
        // Again, trusting client ID for MVP dev mode
        UUID userId = payload.get("id") != null ? UUID.fromString(payload.get("id").toString()) : null;
        String emailObj = (String) payload.get("emailQuery"); // Alternative if ID missing

        User user = null;
        if (userId != null) {
            user = userRepository.findById(userId).orElse(null);
        } else if (emailObj != null) {
            user = userRepository.findByEmail(emailObj).orElse(null);
        }

        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        if (payload.containsKey("fullName")) {
            user.setFullName((String) payload.get("fullName"));
        }
        if (payload.containsKey("avatarUrl")) {
            user.setAvatarUrl((String) payload.get("avatarUrl"));
        }
        // Email update is trickier (needs re-verification), let's skip for simple MVP
        // or allow it
        if (payload.containsKey("email")) {
            // Check collision
            String newEmail = (String) payload.get("email");
            if (!newEmail.equals(user.getEmail()) && !userRepository.existsByEmail(newEmail)) {
                user.setEmail(newEmail);
                // maybe reset verification?
                // user.setVerified(false);
            }
        }

        userRepository.save(user);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Profile updated successfully.");
        return ResponseEntity.ok(response);
    }
}
