package com.cath.producer_service.model;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EdgeFailureRequest {
    @NotNull
    private String edgeId;

    @NotNull
    private String source;

    @NotNull
    private String target;
}