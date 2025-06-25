package com.cath.alarm_service.service;

import com.cath.alarm_service.model.Alert;
import com.cath.alarm_service.model.HeartbeatMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class HeartbeatListener {

    @Autowired
    private EmailService emailService;

    @Autowired
    private AlertService alertService;

    private ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "heartbeat-topic", groupId = "alarm-service")
    public void listen(String message) {
        try {
            HeartbeatMessage heartbeat = objectMapper.readValue(message, HeartbeatMessage.class);
            checkForFailedNodes(heartbeat);
        } catch (Exception e) {
            System.err.println("Error processing heartbeat: " + e.getMessage());
        }
    }

    private void checkForFailedNodes(HeartbeatMessage heartbeat) {
        if (heartbeat.getNodeStatuses() != null) {
            for (Map.Entry<String, com.cath.alarm_service.model.NodeStatus> entry : heartbeat.getNodeStatuses().entrySet()) {
                String nodeId = entry.getKey();
                com.cath.alarm_service.model.NodeStatus status = entry.getValue();

                if ("failed".equals(status.getStatus())) {
                    String alertMessage = "Node " + nodeId + " has failed!";
                    Alert alert = new Alert(nodeId, alertMessage, LocalDateTime.now().toString());

                    // Send to frontend via WebSocket using custom handler
                    alertService.triggerAlert(alert);

                    // Send email alert
                    emailService.sendAlert(nodeId, alertMessage);
                }
            }
        }
    }
}