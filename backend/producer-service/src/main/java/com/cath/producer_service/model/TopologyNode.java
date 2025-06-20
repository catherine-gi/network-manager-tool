package com.cath.producer_service.model;

import lombok.Data;

@Data
public class TopologyNode {
    private String id;

    private Position position;
    private String data;
    private int cpu;        // CPU capacity
    private int latency;    // Base latency
    private String status;  // Initial status
}