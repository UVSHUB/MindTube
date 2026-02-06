package com.nutzycraft.pilotai.controller;

import com.nutzycraft.pilotai.entity.User;
import com.nutzycraft.pilotai.repository.UserRepository;
import com.nutzycraft.pilotai.service.EmailService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final EmailService emailService;

    public AuthController(UserRepository userRepository, EmailService emailService) {
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, String> payload) {
        Map<String, Object> response = new HashMap<>();
        String fullName = payload.get("fullName");
        if (fullName == null || fullName.trim().isEmpty())
            fullName = "User " + System.currentTimeMillis();
        String email = payload.get("email");
        String password = payload.get("password");

        if (email == null || password == null) {
            response.put("success", false);
            response.put("message", "Email and password are required.");
            return ResponseEntity.badRequest().body(response);
        }

        if (userRepository.existsByEmail(email)) {
            response.put("success", false);
            response.put("message", "Email already exists.");
            return ResponseEntity.badRequest().body(response);
        }

        // Generate 6-digit code
        String code = String.valueOf((int) (Math.random() * 900000) + 100000);

        // In a real app, encrypt password here! e.g., BCrypt
        User newUser = new User(email, password, fullName);
        newUser.setVerificationCode(code);
        newUser.setVerified(false);
        userRepository.save(newUser);

        // Send Real Email asynchronously to prevent blocking
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                emailService.sendVerificationEmail(email, code);
                System.out.println("✅ Verification email sent successfully to: " + email);
            } catch (Exception e) {
                System.err.println("❌ Failed to send verification email to " + email + ": " + e.getMessage());
                e.printStackTrace();
            }
        });

        response.put("success", true);
        response.put("message", "Account created. Please check your email for the verification code.");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> payload) {
        Map<String, Object> response = new HashMap<>();
        String email = payload.get("email");
        String password = payload.get("password");

        // Simple validation
        User user = userRepository.findByEmail(email).orElse(null);

        if (user != null && user.getPassword().equals(password)) {
            if (!user.isVerified()) {
                response.put("success", false);
                response.put("message", "Please verify your email first.");
                return ResponseEntity.status(403).body(response);
            }

            response.put("success", true);
            response.put("message", "Login successful.");
            response.put("userId", user.getId());
            response.put("fullName", user.getFullName());
            return ResponseEntity.ok(response);
        } else {
            response.put("success", false);
            response.put("message", "Invalid email or password.");
            return ResponseEntity.status(401).body(response);
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, Object>> forgotPassword(@RequestBody Map<String, String> payload) {
        Map<String, Object> response = new HashMap<>();
        String email = payload.get("email");

        User user = userRepository.findByEmail(email).orElse(null);

        if (user != null) {
            // Generate UUID token instead of 6-digit code
            String token = UUID.randomUUID().toString();
            user.setVerificationCode(token); // Reusing verificationCode field for reset token
            userRepository.save(user);

            try {
                // Construct the reset link (assuming frontend runs on same host/port or
                // configured base URL)
                // For local dev: http://localhost:8080 or wherever the frontend is served.
                // Since this is a simple setup, we'll assume relative path or hardcode
                // localhost for this sprint if base url isn't config'd.
                // Better: send just the link or constructed full URL.
                String resetLink = "https://mind-tube-gilt.vercel.app/reset-password.html?token=" + token;
                emailService.sendPasswordResetEmail(email, resetLink);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Always return success to prevent email enumeration
        response.put("success", true);
        response.put("message", "If an account exists with that email, a password reset link has been sent.");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/resend-code")
    public ResponseEntity<Map<String, Object>> resendCode(@RequestBody Map<String, String> payload) {
        Map<String, Object> response = new HashMap<>();
        String email = payload.get("email");

        User user = userRepository.findByEmail(email).orElse(null);

        if (user != null) {
            String code = user.getVerificationCode();
            if (code == null) {
                code = String.valueOf((int) (Math.random() * 900000) + 100000);
                user.setVerificationCode(code);
                userRepository.save(user);
            }

            try {
                emailService.sendVerificationEmail(email, code);
            } catch (Exception e) {
                e.printStackTrace();
                response.put("success", false);
                response.put("message", "Failed to send email. Please try again.");
                return ResponseEntity.internalServerError().body(response);
            }
        }

        response.put("success", true);
        response.put("message", "Code resent successfully.");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(@RequestBody Map<String, String> payload) {
        Map<String, Object> response = new HashMap<>();
        String token = payload.get("token");
        String newPassword = payload.get("newPassword");

        if (token == null || newPassword == null) {
            response.put("success", false);
            response.put("message", "Token and new password are required.");
            return ResponseEntity.badRequest().body(response);
        }

        // Find user by verification code (token)
        User user = userRepository.findByVerificationCode(token).orElse(null);

        if (user != null) {
            user.setPassword(newPassword); // In real app, hash this!
            user.setVerificationCode(null);
            user.setVerified(true); // Auto-verify on successful password reset
            userRepository.save(user);

            response.put("success", true);
            response.put("message", "Password reset successful. Please login.");
            return ResponseEntity.ok(response);
        } else {
            response.put("success", false);
            response.put("message", "Invalid or expired reset token.");
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyCode(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        String code = payload.get("code");
        Map<String, Object> response = new HashMap<>();

        if (email == null || code == null) {
            response.put("success", false);
            response.put("message", "Email and code are required.");
            return ResponseEntity.badRequest().body(response);
        }

        User user = userRepository.findByEmail(email).orElse(null);

        if (user != null && code.equals(user.getVerificationCode())) {
            user.setVerified(true);
            user.setVerificationCode(null);
            userRepository.save(user);

            response.put("success", true);
            response.put("message", "Verification successful.");
            response.put("userId", user.getId());
            response.put("fullName", user.getFullName());
            return ResponseEntity.ok(response);
        } else {
            response.put("success", false);
            response.put("message", "Invalid verification code.");
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/google")
    public ResponseEntity<Map<String, Object>> googleLogin(@RequestBody Map<String, String> payload) {
        String token = payload.get("token");
        Map<String, Object> response = new HashMap<>();

        if (token == null) {
            response.put("success", false);
            response.put("message", "Token is required.");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            // Verify token with Google
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://oauth2.googleapis.com/tokeninfo?id_token=" + token))
                    .GET()
                    .build();

            HttpResponse<String> googleResponse = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (googleResponse.statusCode() == 200) {
                String body = googleResponse.body();
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(body);

                String email = root.get("email").asText();
                String name = root.has("name") ? root.get("name").asText() : "Google User";
                String picture = root.has("picture") ? root.get("picture").asText() : null;

                User user = userRepository.findByEmail(email).orElse(null);

                if (user == null) {
                    user = new User(email, "GOOGLE_AUTH_PLACEHOLDER", name);
                    user.setVerified(true);
                    user.setAvatarUrl(picture);
                    userRepository.save(user);
                } else if (!user.isVerified()) {
                    user.setVerified(true);
                    userRepository.save(user);
                }

                response.put("success", true);
                response.put("message", "Login successful.");
                response.put("userId", user.getId());
                response.put("fullName", user.getFullName());
                return ResponseEntity.ok(response);

            } else {
                response.put("success", false);
                response.put("message", "Invalid Google Token.");
                return ResponseEntity.status(401).body(response);
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Google verification failed.");
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
