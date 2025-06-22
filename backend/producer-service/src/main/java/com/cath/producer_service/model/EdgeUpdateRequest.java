package com.cath.producer_service.model;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EdgeUpdateRequest {
    @NotNull
    private String edgeId;

    @NotNull
    private String source;

    @NotNull
    private String target;

    @NotNull
    private String status;
    @NotNull
    private int latency;
}