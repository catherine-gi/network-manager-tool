package com.cath.producer_service.model;

import lombok.Data;

@Data
public class NodeStatus {
    private Integer latency;
    private Integer cpu;
    private String status;
}