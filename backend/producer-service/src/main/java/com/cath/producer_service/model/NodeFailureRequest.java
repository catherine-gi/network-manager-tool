package com.cath.producer_service.model;

import jakarta.validation.constraints.NotNull;

public class NodeFailureRequest {
    @NotNull
    private String nodeId;

    public NodeFailureRequest() {}

    public NodeFailureRequest(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }
}
