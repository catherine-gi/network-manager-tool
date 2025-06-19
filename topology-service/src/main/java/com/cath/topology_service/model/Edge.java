package com.cath.topology_service.model;

import java.util.Objects;

public class Edge {
    private String id;
    private String source;
    private String target;
    private String status;
    private double bandwidth;

    public Edge() {}

    public Edge(String id, String source, String target, String status, double bandwidth) {
        this.id = id;
        this.source = source;
        this.target = target;
        this.status = status;
        this.bandwidth = bandwidth;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public double getBandwidth() { return bandwidth; }
    public void setBandwidth(double bandwidth) { this.bandwidth = bandwidth; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Edge edge = (Edge) o;
        return Objects.equals(id, edge.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}