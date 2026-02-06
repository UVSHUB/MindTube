package com.nutzycraft.pilotai.service;

import com.sendgrid.*;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class EmailService {

    @Value("${SENDGRID_API_KEY:}")
    private String sendGridApiKey;

    public void sendVerificationEmail(String toEmail, String code) throws IOException {
        Email from = new Email("nutzycraft@gmail.com");
        Email to = new Email(toEmail);
        String subject = "Your Verification Code - Nutzy Craft PilotAI";
        Content content = new Content("text/plain", 
            "Welcome to PilotAI!\n\nYour verification code is: " + code
            + "\n\nPlease enter this code on the verification page to activate your account.\n\nBest,\nThe Nutzy Craft Team");
        
        Mail mail = new Mail(from, subject, to, content);
        
        SendGrid sg = new SendGrid(sendGridApiKey);
        Request request = new Request();
        
        request.setMethod(Method.POST);
        request.setEndpoint("mail/send");
        request.setBody(mail.build());
        
        Response response = sg.api(request);
        
        if (response.getStatusCode() >= 400) {
            throw new IOException("SendGrid API error: " + response.getStatusCode() + " - " + response.getBody());
        }
        
        System.out.println("✅ Email sent successfully via SendGrid to: " + toEmail);
    }

    public void sendPasswordResetEmail(String toEmail, String resetLink) throws IOException {
        Email from = new Email("nutzycraft@gmail.com");
        Email to = new Email(toEmail);
        String subject = "Password Reset Request - Nutzy Craft PilotAI";
        Content content = new Content("text/plain",
            "Hello,\n\nWe received a request to reset your password. Click the link below to set a new password:\n\n"
            + resetLink
            + "\n\nIf you didn't request this, you can safely ignore this email.\n\nBest,\nThe Nutzy Craft Team");
        
        Mail mail = new Mail(from, subject, to, content);
        
        SendGrid sg = new SendGrid(sendGridApiKey);
        Request request = new Request();
        
        request.setMethod(Method.POST);
        request.setEndpoint("mail/send");
        request.setBody(mail.build());
        
        Response response = sg.api(request);
        
        if (response.getStatusCode() >= 400) {
            throw new IOException("SendGrid API error: " + response.getStatusCode() + " - " + response.getBody());
        }
        
        System.out.println("✅ Password reset email sent successfully via SendGrid to: " + toEmail);
    }
}
