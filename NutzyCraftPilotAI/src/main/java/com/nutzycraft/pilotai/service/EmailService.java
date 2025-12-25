package com.nutzycraft.pilotai.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendVerificationEmail(String toEmail, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("noreply@nutzycraft.com");
        message.setTo(toEmail);
        message.setSubject("Your Verification Code - Nutzy Craft PilotAI");
        message.setText("Welcome to PilotAI!\n\nYour verification code is: " + code
                + "\n\nPlease enter this code on the verification page to activate your account.\n\nBest,\nThe Nutzy Craft Team");

        mailSender.send(message);
    }

    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("noreply@nutzycraft.com");
        message.setTo(toEmail);
        message.setSubject("Password Reset Request - Nutzy Craft PilotAI");
        message.setText(
                "Hello,\n\nWe received a request to reset your password. Click the link below to set a new password:\n\n"
                        + resetLink
                        + "\n\nIf you didn't request this, you can safely ignore this email.\n\nBest,\nThe Nutzy Craft Team");

        mailSender.send(message);
    }
}
