package com.cath.topology_service.model;

import java.util.Objects;

public class Node {
    private String id;
    private String status;
    private double cpu;
    private double latency;

    public Node() {}

    public Node(String id, String status, double cpu, double latency) {
        this.id = id;
        this.status = status;
        this.cpu = cpu;
        this.latency = latency;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public double getCpu() { return cpu; }
    public void setCpu(double cpu) { this.cpu = cpu; }

    public double getLatency() { return latency; }
    public void setLatency(double latency) { this.latency = latency; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Node node = (Node) o;
        return Objects.equals(id, node.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}