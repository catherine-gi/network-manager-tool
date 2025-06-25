package com.cath.heartbeat_monitor.model;

import com.cath.heartbeat_monitor.model.Alert;
import com.cath.heartbeat_monitor.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    private final JavaMailSender mailSender;
    private final AlertRepository alertRepository;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async
    public void notifyAdmins(Alert alert) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(alert.getEmail()); // In real app, this would come from user preferences
            message.setSubject("Network Alert: " + alert.getType());
            message.setText(alert.getMessage());

            mailSender.send(message);

            // Mark as notified
            alert.setNotified(true);
            alertRepository.save(alert);

            log.info("Notification sent for alert: {}", alert.getId());
        } catch (Exception e) {
            log.error("Failed to send notification for alert: {}", alert.getId(), e);
        }
    }
}