package com.cath.producer_service.model;

import lombok.Data;

@Data
public class TopologyEdge {
    private String id;
    private String source;
    private String target;
    private String label;
    private String status;
}