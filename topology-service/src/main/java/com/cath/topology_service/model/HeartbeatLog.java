package com.cath.topology_service.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Document(collection = "heartbeat_logs")
public class HeartbeatLog {
    @Id
    private String id;
    private LocalDateTime timestamp;
    private Map<String, Integer> nodeLatencies;

    public HeartbeatLog(LocalDateTime timestamp, Map<String, Integer> nodeLatencies) {
        this.timestamp = timestamp;
        this.nodeLatencies = nodeLatencies;
    }
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, Integer> getNodeLatencies() {
        return nodeLatencies;
    }

    public void setNodeLatencies(Map<String, Integer> nodeLatencies) {
        this.nodeLatencies = nodeLatencies;
    }


}