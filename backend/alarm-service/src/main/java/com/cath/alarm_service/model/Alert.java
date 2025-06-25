package com.cath.alarm_service.model;
public class Alert {
    private String nodeId;
    private String message;
    private String timestamp;

    public Alert(String nodeId, String message, String timestamp) {
        this.nodeId = nodeId;
        this.message = message;
        this.timestamp = timestamp;
    }

    public String getNodeId() {
        return nodeId;
    }

    public String getMessage() {
        return message;
    }

    public String getTimestamp() {
        return timestamp;
    }
}