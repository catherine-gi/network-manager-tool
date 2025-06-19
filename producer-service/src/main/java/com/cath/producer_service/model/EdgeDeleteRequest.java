package com.cath.producer_service.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EdgeDeleteRequest {
    @NotBlank(message = "Edge ID is required")
    private String edgeId;
}