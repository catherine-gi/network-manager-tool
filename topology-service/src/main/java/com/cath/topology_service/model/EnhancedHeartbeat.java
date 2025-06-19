package com.cath.topology_service.model;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class EnhancedHeartbeat {
    private Map<String, NodeStatus> nodeStatuses;
    private Date timestamp;
    private List<String> failedEdges;

    // Getters and setters
    public Map<String, NodeStatus> getNodeStatuses() {
        return nodeStatuses;
    }

    public void setNodeStatuses(Map<String, NodeStatus> nodeStatuses) {
        this.nodeStatuses = nodeStatuses;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public List<String> getFailedEdges() {
        return failedEdges;
    }

    public void setFailedEdges(List<String> failedEdges) {
        this.failedEdges = failedEdges;
    }
}

