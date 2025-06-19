package com.cath.topology_service.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Document(collection = "topology")
public class NetworkTopology {
    @Id
    private String id;
    private List<String> nodes;
    private Map<String, List<String>> adjacencyList;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public NetworkTopology() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public List<String> getNodes() { return nodes; }
    public void setNodes(List<String> nodes) { this.nodes = nodes; }

    public Map<String, List<String>> getAdjacencyList() { return adjacencyList; }
    public void setAdjacencyList(Map<String, List<String>> adjacencyList) {
        this.adjacencyList = adjacencyList;
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}