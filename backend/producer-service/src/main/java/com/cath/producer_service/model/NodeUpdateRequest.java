package com.cath.producer_service.model;

import lombok.Data;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@Data
public class NodeUpdateRequest {
    @NotBlank
    private String nodeId;

    @Min(1)
    private int cpu;

    @Min(1)
    private int latency;

    @NotBlank
    private String status;
}