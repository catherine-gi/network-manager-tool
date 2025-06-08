package com.cath.producer_service.model;

import java.time.LocalDateTime;
import java.util.Map;

public class HeartbeatMessage {
    private Map<String, Integer> nodes;
    private LocalDateTime timestamp;

    public HeartbeatMessage() {}

    public HeartbeatMessage(Map<String, Integer> nodes) {
        this.nodes = nodes;
        this.timestamp = LocalDateTime.now();
    }

    public Map<String, Integer> getNodes() { return nodes; }
    public void setNodes(Map<String, Integer> nodes) { this.nodes = nodes; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
