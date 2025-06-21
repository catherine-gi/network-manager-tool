package com.cath.path_service.model;


import java.util.List;
import java.util.Map;

public class MultiplePathRequest {
    private Map<String, List<String>> pathRequests;
    private Map<String, Double> weights;

    // Getters and setters
    public Map<String, List<String>> getPathRequests() {
        return pathRequests;
    }

    public void setPathRequests(Map<String, List<String>> pathRequests) {
        this.pathRequests = pathRequests;
    }

    public Map<String, Double> getWeights() {
        return weights;
    }

    public void setWeights(Map<String, Double> weights) {
        this.weights = weights;
    }
}