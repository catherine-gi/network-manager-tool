package com.cath.topology_service.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "nodes")
public class NetworkNode {

    @Id
    private String id; // MongoDB _id field (also used as nodeId)

    private Integer latency;
    private Boolean isActive;
    private LocalDateTime lastSeen;
    private Integer cpu;

    // === Constructors ===

    public NetworkNode() {
        this.lastSeen = LocalDateTime.now();
    }

    public NetworkNode(String id, Integer latency, Boolean isActive, Integer cpu) {
        this.id = id;
        this.latency = latency;
        this.isActive = isActive;
        this.cpu = cpu;
        this.lastSeen = LocalDateTime.now();
    }

    // === Getters and Setters ===

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    // Optional alias
    public String getNodeId() {
        return id;
    }

    public Integer getLatency() {
        return latency;
    }

    public void setLatency(Integer latency) {
        this.latency = latency;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
        this.lastSeen = LocalDateTime.now(); // auto-update last seen
    }

    public LocalDateTime getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(LocalDateTime lastSeen) {
        this.lastSeen = lastSeen;
    }

    public Integer getCpu() {
        return cpu;
    }

    public void setCpu(Integer cpu) {
        this.cpu = cpu;
    }

    // === Utility Methods ===

    public void markAsFailed() {
        this.isActive = false;
        this.lastSeen = LocalDateTime.now();
    }

    public void restore() {
        this.isActive = true;
        this.lastSeen = LocalDateTime.now();
    }
}
