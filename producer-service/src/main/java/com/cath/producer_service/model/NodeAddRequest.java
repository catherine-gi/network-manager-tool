package com.cath.producer_service.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class NodeAddRequest {
    @NotBlank(message = "Node ID is required")
    private String nodeId;

    @Min(value = 1, message = "CPU must be at least 1")
    private int cpu;

    @Min(value = 1, message = "Latency must be at least 1")
    private int latency;

    @NotNull(message = "Status is required")
    private String status; // "active", "standby", "maintenance", "failed"
}