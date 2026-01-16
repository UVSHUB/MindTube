package com.nutzycraft.pilotai.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users", schema = "public")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    // Note: In Supabase, this ID comes from auth.users, not auto-generated
    // For local dev, we can use UUID generation, but in production it should match auth.uid()
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "full_name")
    private String fullName;
    
    @Column(name = "avatar_url", columnDefinition = "TEXT")
    private String avatarUrl;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // Note: These fields need to be added to the Supabase schema
    // Run: ALTER TABLE public.users ADD COLUMN email VARCHAR(255) UNIQUE;
    // Run: ALTER TABLE public.users ADD COLUMN password VARCHAR(255);
    // Run: ALTER TABLE public.users ADD COLUMN verification_code VARCHAR(255);
    // Run: ALTER TABLE public.users ADD COLUMN is_verified BOOLEAN DEFAULT false;
    @Column(name = "email", unique = true)
    private String email;

    @Column(name = "password")
    private String password;

    @Column(name = "verification_code")
    private String verificationCode;
    
    @Column(name = "is_verified")
    private boolean isVerified = false;

    // Helper constructor for registration
    public User(String email, String password, String fullName) {
        this.email = email;
        this.password = password;
        this.fullName = fullName;
        this.createdAt = LocalDateTime.now();
    }
}
