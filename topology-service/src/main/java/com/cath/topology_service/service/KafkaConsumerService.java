package com.cath.topology_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import java.util.Map;

@Service
public class KafkaConsumerService {

    @Autowired
    private TopologyService topologyService;

    @Autowired
    private ObjectMapper objectMapper;

    @KafkaListener(topics = "topology-init-topic", groupId = "topology-service-group")
    public void consumeTopologyInit(String message) {
        try {
            Map<String, Object> topologyData = objectMapper.readValue(message, Map.class);
            topologyService.processTopologyInit(topologyData);
        } catch (Exception e) {
            // Log or handle error
        }
    }

    @KafkaListener(topics = "heartbeat-topic", groupId = "topology-service-group")
    public void consumeHeartbeat(String message) {
        try {
            Map<String, Object> heartbeatData = objectMapper.readValue(message, Map.class);
            topologyService.processHeartbeat(heartbeatData);
        } catch (Exception e) {
            // Log or handle error
        }
    }
}