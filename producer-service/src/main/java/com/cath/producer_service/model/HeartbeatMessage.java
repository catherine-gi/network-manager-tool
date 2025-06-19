package com.cath.producer_service.model;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

@Data
public class HeartbeatMessage {
    private Map<String, NodeStatus> nodes;
    private LocalDateTime timestamp;

    public HeartbeatMessage() {
        this.timestamp = LocalDateTime.now();
    }

    public HeartbeatMessage(Map<String, NodeStatus> nodes) {
        this();
        this.nodes = nodes;
    }

    public Map<String, NodeStatus> getNodes() {
        return nodes;
    }

    public void setNodes(Map<String, NodeStatus> nodes) {
        this.nodes = nodes;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}

