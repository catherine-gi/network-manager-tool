package com.cath.producer_service.service;



import com.cath.producer_service.model.HeartbeatMessage;
import com.cath.producer_service.model.TopologyRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class NetworkSimulationService {

    @Autowired
    private KafkaProducerService kafkaProducerService;

    private Set<String> activeNodes = new HashSet<>();
    private Set<String> failedNodes = new HashSet<>();
    private boolean simulationActive = false;

    public void initializeTopology(TopologyRequest request) {
        // Generate nodes based on count
        List<String> nodes = new ArrayList<>();
        for (int i = 1; i <= request.getNodeCount(); i++) {
            nodes.add("N" + i);
        }
        request.setNodes(nodes);

        // Store active nodes
        activeNodes.clear();
        activeNodes.addAll(nodes);
        failedNodes.clear();

        // Send topology init message
        kafkaProducerService.sendMessage("topology-init-topic", request);

        // Start heartbeat simulation
        simulationActive = true;
    }

    @Scheduled(fixedRate = 5000) // Every 5 seconds
    public void sendHeartbeats() {
        if (!simulationActive || (activeNodes.isEmpty() && failedNodes.isEmpty())) {
            return;
        }

        Map<String, Integer> nodeLatencies = new HashMap<>();

        // Include all nodes (active and failed)
        Set<String> allNodes = new HashSet<>();
        allNodes.addAll(activeNodes);
        allNodes.addAll(failedNodes);

        for (String node : allNodes) {
            if (failedNodes.contains(node)) {
                nodeLatencies.put(node, null); // Failed node
            } else {
                int latency = ThreadLocalRandom.current().nextInt(20, 100);
                nodeLatencies.put(node, latency);
            }
        }

        HeartbeatMessage heartbeat = new HeartbeatMessage(nodeLatencies);
        kafkaProducerService.sendMessage("heartbeat-topic", heartbeat);
    }

    public void simulateNodeFailure(String nodeId) {
        if (activeNodes.contains(nodeId)) {
            activeNodes.remove(nodeId);
            failedNodes.add(nodeId);

            // Send failure notification
            Map<String, Object> failureMsg = new HashMap<>();
            failureMsg.put("failedNode", nodeId);
            failureMsg.put("timestamp", new Date());
            failureMsg.put("topologyType", "MESH");

            //kafkaProducerService.sendMessage("node-failure-topic", failureMsg);
        }
    }

    public void restoreNode(String nodeId) {

        if (failedNodes.contains(nodeId)) {
            failedNodes.remove(nodeId);
            activeNodes.add(nodeId);

            // Notify path service to recalculate paths
            Map<String, Object> restoreMsg = new HashMap<>();
            restoreMsg.put("restoredNodes", Collections.singletonList(nodeId));
            restoreMsg.put("event", "NODE_UP");
            restoreMsg.put("timestamp", new Date());

            //kafkaProducerService.sendMessage("node-failure-topic", restoreMsg);
        }
    }

    public void stopSimulation() {
        simulationActive = false;
        activeNodes.clear();
        failedNodes.clear();
    }

    public Set<String> getActiveNodes() {
        return new HashSet<>(activeNodes);
    }

    public Set<String> getFailedNodes() {
        return new HashSet<>(failedNodes);
    }
}