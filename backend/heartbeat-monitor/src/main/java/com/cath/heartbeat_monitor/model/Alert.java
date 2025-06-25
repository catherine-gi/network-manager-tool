package com.cath.heartbeat_monitor.model;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "alerts")
public class Alert {
    @Id
    private String id;
    private String nodeId;
    private String type; // "FAILURE" or "LATENCY"
    private String message;
    private LocalDateTime timestamp;
    private boolean notified;
    private String email;
}