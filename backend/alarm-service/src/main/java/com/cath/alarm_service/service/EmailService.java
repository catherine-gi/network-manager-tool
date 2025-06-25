package com.cath.alarm_service.service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${alert.email.to:admin@example.com}")
    private String defaultAlertEmail;

    private String currentRecipientEmail;

    public void sendAlert(String nodeId, String message) {
        try {
            SimpleMailMessage email = new SimpleMailMessage();
            String recipient = currentRecipientEmail != null ? currentRecipientEmail : defaultAlertEmail;
            email.setTo(recipient);
            email.setSubject("Node Failure Alert - " + nodeId);
            email.setText(message + "\n\nTimestamp: " + java.time.LocalDateTime.now());
            mailSender.send(email);
        } catch (Exception e) {
            System.err.println("Failed to send email: " + e.getMessage());
        }
    }

    public void updateRecipientEmail(String email) {
        this.currentRecipientEmail = email;
    }

    public String getCurrentRecipientEmail() {
        return currentRecipientEmail != null ? currentRecipientEmail : defaultAlertEmail;
    }
}