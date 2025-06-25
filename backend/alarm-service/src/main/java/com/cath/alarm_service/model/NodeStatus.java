// src/main/java/com/cath/alarm_service/model/NodeStatus.java
package com.cath.alarm_service.model;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class NodeStatus {
    @JsonProperty("status")
    private String status;

    @JsonProperty("cpu")
    private int cpu;

    @JsonProperty("latency")
    private int latency;

    @JsonProperty("outgoingConnections")
    private List<String> outgoingConnections;

    @JsonProperty("incomingConnections")
    private List<String> incomingConnections;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getCpu() {
        return cpu;
    }

    public void setCpu(int cpu) {
        this.cpu = cpu;
    }

    public int getLatency() {
        return latency;
    }

    public void setLatency(int latency) {
        this.latency = latency;
    }

    public List<String> getOutgoingConnections() {
        return outgoingConnections;
    }

    public void setOutgoingConnections(List<String> outgoingConnections) {
        this.outgoingConnections = outgoingConnections;
    }

    public List<String> getIncomingConnections() {
        return incomingConnections;
    }

    public void setIncomingConnections(List<String> incomingConnections) {
        this.incomingConnections = incomingConnections;
    }
}