package com.cath.heartbeat_monitor.model;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Heartbeat {
    @JsonProperty("nodeStatuses")
    private Map<String, NodeStatus> nodeStatuses;

    @JsonProperty("failedEdges")
    private String[] failedEdges;

    @JsonProperty("timestamp")
    private String timestamp;
}