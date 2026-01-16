package com.nutzycraft.pilotai.controller;

import com.nutzycraft.pilotai.entity.User;
import com.nutzycraft.pilotai.repository.UserRepository;
import com.nutzycraft.pilotai.service.CloudinaryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserRepository userRepository;
    private final CloudinaryService cloudinaryService;

    public UserController(UserRepository userRepository, CloudinaryService cloudinaryService) {
        this.userRepository = userRepository;
        this.cloudinaryService = cloudinaryService;
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
            String avatarUrl = (String) payload.get("avatarUrl");
            System.out.println("Received avatarUrl in payload: " + (avatarUrl != null ? (avatarUrl.length() > 100 ? avatarUrl.substring(0, 100) + "..." : avatarUrl) : "null"));
            
            // If avatarUrl is a base64 image (starts with "data:image"), upload to Cloudinary
            if (avatarUrl != null && avatarUrl.startsWith("data:image")) {
                System.out.println("Detected base64 image, uploading to Cloudinary...");
                try {
                    // Delete old avatar if it exists and is from Cloudinary
                    if (user.getAvatarUrl() != null && user.getAvatarUrl().contains("cloudinary.com")) {
                        System.out.println("Deleting old Cloudinary avatar: " + user.getAvatarUrl());
                        cloudinaryService.deleteImage(user.getAvatarUrl());
                    }
                    // Upload base64 image to Cloudinary
                    String cloudinaryUrl = cloudinaryService.uploadBase64Image(avatarUrl);
                    System.out.println("Successfully uploaded to Cloudinary: " + cloudinaryUrl);
                    user.setAvatarUrl(cloudinaryUrl);
                } catch (IOException e) {
                    System.err.println("Failed to upload avatar to Cloudinary: " + e.getMessage());
                    e.printStackTrace();
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("success", false);
                    errorResponse.put("message", "Failed to upload avatar image: " + e.getMessage());
                    return ResponseEntity.status(500).body(errorResponse);
                } catch (Exception e) {
                    System.err.println("Unexpected error uploading avatar: " + e.getMessage());
                    e.printStackTrace();
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("success", false);
                    errorResponse.put("message", "Failed to upload avatar image: " + e.getMessage());
                    return ResponseEntity.status(500).body(errorResponse);
                }
            } else if (avatarUrl != null && !avatarUrl.isEmpty()) {
                // If it's already a URL (e.g., from Google profile), just set it
                System.out.println("Setting avatar URL directly: " + avatarUrl);
                user.setAvatarUrl(avatarUrl);
            } else {
                // If empty string, delete the avatar
                System.out.println("Removing avatar");
                if (user.getAvatarUrl() != null && user.getAvatarUrl().contains("cloudinary.com")) {
                    cloudinaryService.deleteImage(user.getAvatarUrl());
                }
                user.setAvatarUrl(null);
            }
        } else {
            System.out.println("No avatarUrl in payload, skipping avatar update");
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
        response.put("avatarUrl", user.getAvatarUrl());
        return ResponseEntity.ok(response);
    }

    /**
     * Upload avatar image file directly to Cloudinary (multipart/form-data)
     * This is the primary method for avatar uploads - more efficient than base64
     */
    @PostMapping("/me/avatar")
    public ResponseEntity<Map<String, Object>> uploadAvatar(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") String userId) {
        
        Map<String, Object> response = new HashMap<>();
        
        System.out.println("Received avatar upload request. File: " + file.getOriginalFilename() + ", Size: " + file.getSize() + " bytes");
        
        if (file.isEmpty()) {
            response.put("success", false);
            response.put("message", "No file provided.");
            return ResponseEntity.badRequest().body(response);
        }

        // Validate file type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            response.put("success", false);
            response.put("message", "File must be an image.");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            UUID userUuid = UUID.fromString(userId);
            User user = userRepository.findById(userUuid).orElse(null);

            if (user == null) {
                response.put("success", false);
                response.put("message", "User not found.");
                return ResponseEntity.status(404).body(response);
            }

            // Delete old avatar if it exists and is from Cloudinary
            if (user.getAvatarUrl() != null && user.getAvatarUrl().contains("cloudinary.com")) {
                System.out.println("Deleting old Cloudinary avatar: " + user.getAvatarUrl());
                cloudinaryService.deleteImage(user.getAvatarUrl());
            }

            // Upload to Cloudinary
            System.out.println("Uploading file directly to Cloudinary...");
            String cloudinaryUrl = cloudinaryService.uploadFile(file);
            System.out.println("Successfully uploaded to Cloudinary: " + cloudinaryUrl);
            
            user.setAvatarUrl(cloudinaryUrl);
            userRepository.save(user);

            response.put("success", true);
            response.put("message", "Avatar uploaded successfully.");
            response.put("avatarUrl", cloudinaryUrl);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", "Invalid user ID format.");
            return ResponseEntity.badRequest().body(response);
        } catch (IOException e) {
            response.put("success", false);
            response.put("message", "Failed to upload avatar: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}
