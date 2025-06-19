package com.cath.producer_service.model;

import lombok.Data;
import java.util.List;

@Data
public class TopologyRequest {
    private List<TopologyNode> nodes;
    private List<TopologyEdge> edges;

}