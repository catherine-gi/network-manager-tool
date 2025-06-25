package com.cath.alarm_service.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class EmailConfig {
    private String recipientEmail;

    public EmailConfig() {}

    public EmailConfig(String recipientEmail) {
        this.recipientEmail = recipientEmail;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public void setRecipientEmail(String recipientEmail) {
        this.recipientEmail = recipientEmail;
    }
}