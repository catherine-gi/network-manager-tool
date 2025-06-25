package com.network.heartbeat.service;
import com.network.heartbeat.model.Alert;
import com.network.heartbeat.model.Heartbeat;
import com.network.heartbeat.model.NodeStatus;
import com.network.heartbeat.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class HeartbeatMonitorService {
    private final AlertRepository alertRepository;
    private final NotificationService notificationService;

    @Value("${heartbeat.latency.threshold}")
    private int latencyThreshold;

    @KafkaListener(topics = "heartbeat-topic", groupId = "heartbeat-monitor-group")
    public void monitorHeartbeat(Heartbeat heartbeat) {
        log.info("Received heartbeat: {}", heartbeat);

        Map<String, NodeStatus> nodeStatuses = heartbeat.getNodeStatuses();
        if (nodeStatuses != null) {
            nodeStatuses.forEach((nodeId, status) -> {
                // Check for node failure
                if ("failed".equalsIgnoreCase(status.getStatus())) {
                    handleNodeFailure(nodeId, status);
                }

                // Check for high latency
                if (status.getLatency() > latencyThreshold) {
                    handleHighLatency(nodeId, status);
                }
            });
        }
    }

    private void handleNodeFailure(String nodeId, NodeStatus status) {
        String message = String.format("Node %s has failed. Last status: %s", nodeId, status);
        createAndNotifyAlert(nodeId, "FAILURE", message);
    }

    private void handleHighLatency(String nodeId, NodeStatus status) {
        String message = String.format("Node %s has high latency: %dms (threshold: %dms)",
                nodeId, status.getLatency(), latencyThreshold);
        createAndNotifyAlert(nodeId, "LATENCY", message);
    }

    private void createAndNotifyAlert(String nodeId, String type, String message) {
        // Check if similar alert already exists and wasn't notified yet
        boolean alertExists = alertRepository.findByNodeId(nodeId)
                .stream()
                .anyMatch(a -> a.getType().equals(type) && !a.isNotified());

        if (!alertExists) {
            Alert alert = new Alert();
            alert.setNodeId(nodeId);
            alert.setType(type);
            alert.setMessage(message);
            alert.setTimestamp(LocalDateTime.now());
            alert.setNotified(false);

            alertRepository.save(alert);

            // Notify via email (async)
            notificationService.notifyAdmins(alert);
        }
    }
}