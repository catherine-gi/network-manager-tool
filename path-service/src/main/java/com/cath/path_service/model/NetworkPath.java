package com.cath.path_service.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "paths")
public class NetworkPath {
    @Id
    private String id;
    private String fromNode;
    private String toNode;
    private List<String> path;
    private Integer hopCount;
    private Integer totalLatency;
    private Boolean isActive;
    private LocalDateTime calculatedAt;
    private String pathType; // PRIMARY, BACKUP, etc.

    public NetworkPath() {}

    public NetworkPath(String fromNode, String toNode, List<String> path, String pathType) {
        this.fromNode = fromNode;
        this.toNode = toNode;
        this.path = path;
        this.hopCount = path.size() - 1;
        this.pathType = pathType;
        this.isActive = true;
        this.calculatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFromNode() { return fromNode; }
    public void setFromNode(String fromNode) { this.fromNode = fromNode; }

    public String getToNode() { return toNode; }
    public void setToNode(String toNode) { this.toNode = toNode; }

    public List<String> getPath() { return path; }
    public void setPath(List<String> path) {
        this.path = path;
        this.hopCount = path != null ? path.size() - 1 : 0;
    }

    public Integer getHopCount() { return hopCount; }
    public void setHopCount(Integer hopCount) { this.hopCount = hopCount; }

    public Integer getTotalLatency() { return totalLatency; }
    public void setTotalLatency(Integer totalLatency) { this.totalLatency = totalLatency; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public LocalDateTime getCalculatedAt() { return calculatedAt; }
    public void setCalculatedAt(LocalDateTime calculatedAt) { this.calculatedAt = calculatedAt; }

    public String getPathType() { return pathType; }
    public void setPathType(String pathType) { this.pathType = pathType; }
}