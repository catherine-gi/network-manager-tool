package com.cath.producer_service.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EdgeAddRequest {
    @NotBlank(message = "Edge ID is required")
    private String edgeId;

    @NotBlank(message = "Source node is required")
    private String source;

    @NotBlank(message = "Target node is required")
    private String target;

    @NotNull(message = "Status is required")
    private String status; // "active", "failed"
}
