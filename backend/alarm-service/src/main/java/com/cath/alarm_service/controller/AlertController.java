package com.cath.alarm_service.controller;

import com.cath.alarm_service.config.EmailConfig;
import com.cath.alarm_service.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
public class AlertController {

    @Autowired
    private EmailService emailService;

    @GetMapping("/health")
    public String health() {
        return "Alarm service is running";
    }

    @PostMapping("/email-config")
    public String updateEmailConfig(@RequestBody EmailConfig config) {
        emailService.updateRecipientEmail(config.getRecipientEmail());
        return "Email recipient updated successfully";
    }

    @GetMapping("/email-config")
    public EmailConfig getEmailConfig() {
        return new EmailConfig(emailService.getCurrentRecipientEmail());
    }
}