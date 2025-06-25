// src/main/java/com/cath/alarm_service/model/HeartbeatMessage.java
package com.cath.alarm_service.model;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public class HeartbeatMessage {
    @JsonProperty("nodeStatuses")
    private Map<String, NodeStatus> nodeStatuses;

    @JsonProperty("failedEdges")
    private List<Object> failedEdges; // Use appropriate type if known

    @JsonProperty("timestamp")
    private String timestamp;

    public Map<String, NodeStatus> getNodeStatuses() {
        return nodeStatuses;
    }

    public void setNodeStatuses(Map<String, NodeStatus> nodeStatuses) {
        this.nodeStatuses = nodeStatuses;
    }

    public List<Object> getFailedEdges() {
        return failedEdges;
    }

    public void setFailedEdges(List<Object> failedEdges) {
        this.failedEdges = failedEdges;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}