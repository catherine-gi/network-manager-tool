package com.cath.topology_service.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class PathRequest {
    @NotNull(message = "fromNode cannot be null")
    @NotBlank(message = "fromNode cannot be blank")
    private String fromNode;

    @NotNull(message = "toNode cannot be null")
    @NotBlank(message = "toNode cannot be blank")
    private String toNode;

    // Constructors
    public PathRequest() {}

    public PathRequest(String fromNode, String toNode) {
        this.fromNode = fromNode;
        this.toNode = toNode;
    }

    // Getters and Setters
    public String getFromNode() {
        return fromNode;
    }

    public void setFromNode(String fromNode) {
        this.fromNode = fromNode;
    }

    public String getToNode() {
        return toNode;
    }

    public void setToNode(String toNode) {
        this.toNode = toNode;
    }
}