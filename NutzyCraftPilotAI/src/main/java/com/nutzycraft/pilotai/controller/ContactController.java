package com.nutzycraft.pilotai.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api")
public class ContactController {

    private final com.nutzycraft.pilotai.repository.ContactRepository contactRepository;

    public ContactController(com.nutzycraft.pilotai.repository.ContactRepository contactRepository) {
        this.contactRepository = contactRepository;
    }

    @PostMapping("/contact")
    public ResponseEntity<Map<String, Object>> handleContact(@RequestBody Map<String, String> payload) {
        String name = payload.get("name");
        String email = payload.get("email");
        String subject = payload.get("subject");
        String message = payload.get("message");

        Map<String, Object> response = new HashMap<>();

        if (name == null || email == null || message == null) {
            response.put("success", false);
            response.put("message", "Missing required fields");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            com.nutzycraft.pilotai.entity.ContactSubmission submission = new com.nutzycraft.pilotai.entity.ContactSubmission();
            submission.setName(name);
            submission.setEmail(email);
            submission.setSubject(subject != null ? subject : "General Inquiry");
            submission.setMessage(message);

            contactRepository.save(submission);

            System.out.println("Contact Form Saved: " + name + " (" + email + ")");

            response.put("success", true);
            response.put("message", "Thank you! Your message has been received.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "An error occurred while saving your message.");
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
