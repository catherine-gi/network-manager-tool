package com.cath.producer_service.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
@Data
public class NodeDeleteRequest {
    @NotBlank(message = "Node ID is required")
    private String nodeId;
}