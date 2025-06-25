package com.cath.heartbeat_monitor.model;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class NodeStatus {
    @JsonProperty("outgoingConnections")
    private String[] outgoingConnections;

    @JsonProperty("latency")
    private int latency;

    @JsonProperty("cpu")
    private int cpu;

    @JsonProperty("status")
    private String status;

    @JsonProperty("incomingConnections")
    private String[] incomingConnections;
}