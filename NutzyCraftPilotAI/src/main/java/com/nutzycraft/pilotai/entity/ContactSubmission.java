package com.nutzycraft.pilotai.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "contact_submissions", schema = "public")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContactSubmission {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String name;
    private String email;
    private String subject;
    private String message;

    // Note: If using Hibernate to auto-generate timestamp, use @CreationTimestamp.
    // If relying on DB default, might need @Column(insertable=false,
    // updatable=false) or similar.
    // For simplicity, let's set it in existing application logic or use a listener.
    private LocalDateTime createdAt = LocalDateTime.now();
}
