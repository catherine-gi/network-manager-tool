package com.cath.topology_service.model;



import jakarta.validation.constraints.NotNull;

public class PathRequest {
    @NotNull
    private String fromNode;

    @NotNull
    private String toNode;

    private String topologyType = "MESH"; // Default value

    // Getters and Setters
    public String getFromNode() { return fromNode; }
    public void setFromNode(String fromNode) { this.fromNode = fromNode; }

    public String getToNode() { return toNode; }
    public void setToNode(String toNode) { this.toNode = toNode; }

    public String getTopologyType() { return topologyType; }
    public void setTopologyType(String topologyType) {
        this.topologyType = topologyType;
    }
}