package com.cath.producer_service.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import java.util.List;

public class TopologyRequest {
    @NotNull
    @Min(2)
    private Integer nodeCount;

    @NotNull
    private String fromNode;

    @NotNull
    private String toNode;

    @NotNull
    private String topologyType;

    private List<String> nodes;

    // Constructors
    public TopologyRequest() {}

    public TopologyRequest(Integer nodeCount, String fromNode, String toNode, String topologyType) {
        this.nodeCount = nodeCount;
        this.fromNode = fromNode;
        this.toNode = toNode;
        this.topologyType = topologyType;
    }

    // Getters and Setters
    public Integer getNodeCount() { return nodeCount; }
    public void setNodeCount(Integer nodeCount) { this.nodeCount = nodeCount; }

    public String getFromNode() { return fromNode; }
    public void setFromNode(String fromNode) { this.fromNode = fromNode; }

    public String getToNode() { return toNode; }
    public void setToNode(String toNode) { this.toNode = toNode; }

    public String getTopologyType() { return topologyType; }
    public void setTopologyType(String topologyType) { this.topologyType = topologyType; }

    public List<String> getNodes() { return nodes; }
    public void setNodes(List<String> nodes) { this.nodes = nodes; }
}