package com.cath.path_service.consumer;

import com.cath.path_service.service.PathCalculationService;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class PathConsumer {

    @Autowired
    private PathCalculationService pathCalculationService;

    @Autowired
    private ObjectMapper objectMapper;

    @KafkaListener(topics = "node-failure-topic", groupId = "path-service-group")
    public void handleNodeFailure(String message) {
        try {
            Map<String, Object> failureData = objectMapper.readValue(message, Map.class);

            if (failureData.containsKey("failedNodes")) {
                List<String> failedNodes = (List<String>) failureData.get("failedNodes");
                pathCalculationService.handleNodeFailure(failedNodes);
                System.out.println("Processed node failure: " + failedNodes);
            } else if (failureData.containsKey("failedNode")) {
                String failedNode = (String) failureData.get("failedNode");
                pathCalculationService.handleNodeFailure(List.of(failedNode));
                System.out.println("Processed node failure: " + failedNode);
            }
        } catch (Exception e) {
            System.err.println("Error processing node failure: " + e.getMessage());
        }
    }
}