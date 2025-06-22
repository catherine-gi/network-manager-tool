package com.cath.producer_service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.cath.producer_service.model.*;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class NetworkSimulationService {

    @Autowired
    private KafkaProducerService kafkaProducerService;
    private static final Logger logger = LoggerFactory.getLogger(NetworkSimulationService.class);
    private Set<String> activeNodes = new HashSet<>();
    private Set<String> failedNodes = new HashSet<>();
    private Map<String, List<String>> adjacencyList = new HashMap<>();
    private Map<String, NodeProperties> nodeProperties = new HashMap<>();
    private boolean simulationActive = false;

    // Edge-related state
    private Map<String, EdgeProperties> edgeProperties = new HashMap<>();  // key: source-target, value: EdgeProperties
    private Set<String> failedEdges = new HashSet<>();

    @Data
    private static class NodeProperties {
        private int cpu;
        private int baseLatency;
        private String status;
    }

    @Data
    private static class EdgeProperties {
        private String status;
        private int latency;
    }

    public void initializeTopology(TopologyRequest request) {
        // Clear existing state
        activeNodes.clear();
        failedNodes.clear();
        nodeProperties.clear();
        adjacencyList.clear();
        edgeProperties.clear();
        failedEdges.clear();

        // Deduplicate nodes by ID
        List<TopologyNode> uniqueNodes = request.getNodes().stream()
                .collect(Collectors.toMap(
                        TopologyNode::getId,
                        n -> n,
                        (existing, replacement) -> existing)) // Keep first occurrence
                .values()
                .stream()
                .collect(Collectors.toList());

        // Deduplicate edges by source-target pair
        List<TopologyEdge> uniqueEdges = request.getEdges().stream()
                .collect(Collectors.toMap(
                        e -> e.getSource() + "-" + e.getTarget(), // Use source-target as unique key
                        e -> e,
                        (existing, replacement) -> existing))
                .values()
                .stream()
                .collect(Collectors.toList());

        // Validate we have at least one node
        if (uniqueNodes.isEmpty()) {
            logger.warn("Attempted to initialize topology with no valid nodes");
            return;
        }

        // Build UNIDIRECTIONAL adjacency list from deduplicated edges
        uniqueEdges.forEach(edge -> {
            // Only add the outgoing connection (source -> target)
            adjacencyList.computeIfAbsent(edge.getSource(), k -> new ArrayList<>()).add(edge.getTarget());
            // Do NOT add the reverse connection (target -> source)
        });

        // Store node properties from deduplicated nodes
        uniqueNodes.forEach(node -> {
            NodeProperties props = new NodeProperties();
            props.setCpu(node.getCpu());
            props.setBaseLatency(node.getLatency());
            props.setStatus(node.getStatus());
            nodeProperties.put(node.getId(), props);

            if ("failed".equalsIgnoreCase(node.getStatus())) {
                failedNodes.add(node.getId());
            } else {
                activeNodes.add(node.getId());
            }
        });

        // Store edge properties from deduplicated edges
        uniqueEdges.forEach(edge -> {
            String edgeKey = edge.getSource() + "-" + edge.getTarget();
            EdgeProperties props = new EdgeProperties();
            props.setStatus(edge.getStatus() != null ? edge.getStatus() : "active");
            props.setLatency(edge.getLatency());
            edgeProperties.put(edgeKey, props);
            if ("failed".equalsIgnoreCase(props.getStatus())) {
                failedEdges.add(edgeKey);
            }
        });

        // Prepare topology data with validation
        Map<String, Object> topologyData = new HashMap<>();
        topologyData.put("nodes", uniqueNodes.stream()
                .map(n -> {
                    Map<String, Object> nodeMap = new HashMap<>();
                    nodeMap.put("id", n.getId());
                    nodeMap.put("cpu", n.getCpu());
                    nodeMap.put("latency", n.getLatency());
                    nodeMap.put("status", n.getStatus());
                    return nodeMap;
                })
                .collect(Collectors.toList()));

        topologyData.put("edges", uniqueEdges.stream()
                .map(e -> {
                    Map<String, Object> edgeMap = new HashMap<>();
                    edgeMap.put("id", e.getId());
                    edgeMap.put("source", e.getSource());
                    edgeMap.put("target", e.getTarget());
                    edgeMap.put("status", e.getStatus() != null ? e.getStatus() : "active");
                    edgeMap.put("latency", e.getLatency());
                    return edgeMap;
                })
                .collect(Collectors.toList()));

        topologyData.put("timestamp", new Date());

        // Only send if we have valid nodes
        if (!uniqueNodes.isEmpty()) {
            kafkaProducerService.sendMessage("topology-init-topic", topologyData);
            simulationActive = true;
            logger.info("Topology initialized with {} nodes and {} edges (UNIDIRECTIONAL)",
                    uniqueNodes.size(), uniqueEdges.size());
        }
    }

    @Scheduled(fixedRate = 5000)
    public void sendHeartbeats() {
        if (!simulationActive || nodeProperties.isEmpty()) return;

        Map<String, Object> heartbeat = new HashMap<>();
        Map<String, Object> nodeStatuses = new HashMap<>();

        nodeProperties.forEach((nodeId, props) -> {
            Map<String, Object> status = new HashMap<>();
            status.put("cpu", props.getCpu());
            status.put("latency", props.getBaseLatency());
            status.put("status", props.getStatus());

            // Outgoing connections: include all, no filtering
            if (adjacencyList.containsKey(nodeId)) {
                status.put("outgoingConnections", new ArrayList<>(adjacencyList.get(nodeId)));
            } else {
                status.put("outgoingConnections", new ArrayList<>());
            }

            // Incoming connections: include all, no filtering
            List<String> incomingConnections = adjacencyList.entrySet().stream()
                    .filter(entry -> entry.getValue().contains(nodeId))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
            status.put("incomingConnections", incomingConnections);

            nodeStatuses.put(nodeId, status);
        });

        heartbeat.put("nodeStatuses", nodeStatuses);
        heartbeat.put("failedEdges", new ArrayList<>(failedEdges));  // Edge failure information
        heartbeat.put("timestamp", new Date());

        kafkaProducerService.sendMessage("heartbeat-topic", heartbeat);
    }

    public void updateNodeProperties(String nodeId, int cpu, int latency, String status) {
        if (nodeProperties.containsKey(nodeId)) {
            NodeProperties props = nodeProperties.get(nodeId);
            props.setCpu(cpu);
            props.setBaseLatency(latency);
            props.setStatus(status);

            if ("failed".equalsIgnoreCase(status)) {
                activeNodes.remove(nodeId);
                failedNodes.add(nodeId);
            } else {
                failedNodes.remove(nodeId);
                activeNodes.add(nodeId);
            }

            Map<String, Object> updateMsg = new HashMap<>();
            updateMsg.put("nodeId", nodeId);
            updateMsg.put("cpu", cpu);
            updateMsg.put("latency", latency);
            updateMsg.put("status", status);
            updateMsg.put("timestamp", new Date());

            kafkaProducerService.sendMessage("topology-init-topic", updateMsg);
        }
    }

    public void simulateNodeFailure(String nodeId) {
        if (activeNodes.contains(nodeId)) {
            activeNodes.remove(nodeId);
            failedNodes.add(nodeId);
            nodeProperties.get(nodeId).setStatus("failed");

            Map<String, Object> failureMsg = new HashMap<>();
            failureMsg.put("failedNode", nodeId);
            failureMsg.put("timestamp", new Date());
            //kafkaProducerService.sendMessage("node-failure-topic", failureMsg);
        }
    }

    public void restoreNode(String nodeId) {
        if (failedNodes.contains(nodeId)) {
            failedNodes.remove(nodeId);
            activeNodes.add(nodeId);

            NodeProperties props = nodeProperties.get(nodeId);
            props.setCpu(4); // default
            props.setBaseLatency(50); // default
            props.setStatus("active");
        }
    }

    public void updateEdgeProperties(String edgeId, String source, String target, String status, int latency) {
        String edgeKey = source + "-" + target;
        EdgeProperties props = edgeProperties.getOrDefault(edgeKey, new EdgeProperties());
        props.setStatus(status);
        props.setLatency(latency);
        edgeProperties.put(edgeKey, props);

        if ("failed".equalsIgnoreCase(status)) {
            failedEdges.add(edgeKey);
        } else {
            failedEdges.remove(edgeKey);
        }

        Map<String, Object> updateMsg = new HashMap<>();
        updateMsg.put("edgeId", edgeId);
        updateMsg.put("source", source);
        updateMsg.put("target", target);
        updateMsg.put("status", status);
        updateMsg.put("latency", latency);
        updateMsg.put("timestamp", new Date());

        kafkaProducerService.sendMessage("topology-init-topic", updateMsg);
    }

    public void simulateEdgeFailure(String edgeId, String source, String target) {
        String edgeKey = source + "-" + target;
        EdgeProperties props = edgeProperties.getOrDefault(edgeKey, new EdgeProperties());
        props.setStatus("failed");
        edgeProperties.put(edgeKey, props);
        failedEdges.add(edgeKey);

        Map<String, Object> failureMsg = new HashMap<>();
        failureMsg.put("edgeId", edgeId);
        failureMsg.put("source", source);
        failureMsg.put("target", target);
        failureMsg.put("timestamp", new Date());

        //kafkaProducerService.sendMessage("node-failure-topic", failureMsg);
    }

    public void restoreEdge(String edgeId, String source, String target) {
        String edgeKey = source + "-" + target;
        EdgeProperties props = edgeProperties.getOrDefault(edgeKey, new EdgeProperties());
        props.setStatus("active");
        edgeProperties.put(edgeKey, props);
        failedEdges.remove(edgeKey);

        Map<String, Object> restoreMsg = new HashMap<>();
        restoreMsg.put("edgeId", edgeId);
        restoreMsg.put("source", source);
        restoreMsg.put("target", target);
        restoreMsg.put("timestamp", new Date());

        kafkaProducerService.sendMessage("topology-init-topic", restoreMsg);
    }

    public void addNode(NodeAddRequest request) {
        NodeProperties props = new NodeProperties();
        props.setCpu(request.getCpu());
        props.setBaseLatency(request.getLatency());
        props.setStatus(request.getStatus());

        nodeProperties.put(request.getNodeId(), props);
        activeNodes.add(request.getNodeId());

        // Send full topology update
        sendFullTopologyUpdate();
    }

    public void deleteNode(String nodeId) {
        nodeProperties.remove(nodeId);
        activeNodes.remove(nodeId);
        failedNodes.remove(nodeId);

        // Remove node from adjacency list (outgoing connections)
        adjacencyList.remove(nodeId);

        // Remove all edges where this node is the target (incoming connections)
        adjacencyList.values().forEach(targets -> targets.remove(nodeId));

        // Remove all edges connected to this node from edge properties
        List<String> edgesToRemove = new ArrayList<>();
        for (String edgeKey : edgeProperties.keySet()) {
            if (edgeKey.startsWith(nodeId + "-") || edgeKey.endsWith("-" + nodeId)) {
                edgesToRemove.add(edgeKey);
            }
        }
        edgesToRemove.forEach(edgeKey -> {
            edgeProperties.remove(edgeKey);
            failedEdges.remove(edgeKey);
        });

        // Send full topology update
        sendFullTopologyUpdate();
    }

    public void addEdge(EdgeAddRequest request) {
        String edgeKey = request.getSource() + "-" + request.getTarget();
        EdgeProperties props = new EdgeProperties();
        props.setStatus(request.getStatus());
        props.setLatency(request.getLatency());
        edgeProperties.put(edgeKey, props);

        // Update adjacency list (UNIDIRECTIONAL)
        adjacencyList.computeIfAbsent(request.getSource(), k -> new ArrayList<>()).add(request.getTarget());

        // Send full topology update
        sendFullTopologyUpdate();
    }

    public void deleteEdge(String edgeId) {
        // Assuming edgeId is in format "source-target"
        edgeProperties.remove(edgeId);
        failedEdges.remove(edgeId);

        // Update adjacency list (UNIDIRECTIONAL)
        String[] nodes = edgeId.split("-");
        if (nodes.length == 2) {
            String source = nodes[0];
            String target = nodes[1];

            if (adjacencyList.containsKey(source)) {
                adjacencyList.get(source).remove(target);
            }
        }

        // Send full topology update
        sendFullTopologyUpdate();
    }

    private void sendFullTopologyUpdate() {
        List<Map<String, Object>> nodes = new ArrayList<>();
        nodeProperties.forEach((nodeId, props) -> {
            Map<String, Object> nodeMap = new HashMap<>();
            nodeMap.put("id", nodeId);
            nodeMap.put("cpu", props.getCpu());
            nodeMap.put("latency", props.getBaseLatency());
            nodeMap.put("status", props.getStatus());
            nodes.add(nodeMap);
        });

        List<Map<String, Object>> edges = new ArrayList<>();
        edgeProperties.forEach((edgeKey, props) -> {
            String[] nodePair = edgeKey.split("-");
            if (nodePair.length == 2) {
                Map<String, Object> edgeMap = new HashMap<>();
                edgeMap.put("id", edgeKey);
                edgeMap.put("source", nodePair[0]);
                edgeMap.put("target", nodePair[1]);
                edgeMap.put("status", props.getStatus());
                edgeMap.put("latency", props.getLatency());
                edges.add(edgeMap);
            }
        });

        Map<String, Object> topologyData = new HashMap<>();
        topologyData.put("nodes", nodes);
        topologyData.put("edges", edges);
        topologyData.put("timestamp", new Date());

        kafkaProducerService.sendMessage("topology-init-topic", topologyData);
    }

    public void stopSimulation() {
        simulationActive = false;
        activeNodes.clear();
        failedNodes.clear();
        nodeProperties.clear();
        adjacencyList.clear();
        edgeProperties.clear();
        failedEdges.clear();
    }

    public Set<String> getActiveNodes() {
        return new HashSet<>(activeNodes);
    }

    public Set<String> getFailedNodes() {
        return new HashSet<>(failedNodes);
    }

    public Map<String, NodeProperties> getNodeProperties() {
        return new HashMap<>(nodeProperties);
    }

    public Map<String, EdgeProperties> getEdgeProperties() {
        return new HashMap<>(edgeProperties);
    }

    public Set<String> getFailedEdges() {
        return new HashSet<>(failedEdges);
    }
}