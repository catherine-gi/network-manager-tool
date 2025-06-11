// Service/TopologyService.java
package com.cath.topology_service.service;

import com.cath.topology_service.model.HeartbeatLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.cath.topology_service.model.NetworkNode;
import com.cath.topology_service.model.NetworkTopology;
import com.cath.topology_service.repository.HeartbeatLogRepository;
import com.cath.topology_service.repository.NetworkNodeRepository;
import com.cath.topology_service.repository.NetworkTopologyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class TopologyService {
    private static final Logger logger = LoggerFactory.getLogger(TopologyService.class);

    @Autowired
    private HeartbeatLogRepository heartbeatLogRepository;

    @Autowired
    private NetworkNodeRepository nodeRepository;

    @Autowired
    private NetworkTopologyRepository topologyRepository;

    @Autowired
    private KafkaProducerService kafkaProducerService;

    public void processTopologyInit(Map<String, Object> topologyData) {

        Integer nodeCount = (Integer) topologyData.get("nodeCount");
        String fromNode = (String) topologyData.get("fromNode");
        String toNode = (String) topologyData.get("toNode");
        String topologyType = (String) topologyData.get("topologyType");
        List<String> nodes = (List<String>) topologyData.get("nodes");

        // Create and save topology
        NetworkTopology topology = new NetworkTopology(nodeCount, fromNode, toNode, topologyType, nodes);

        // Generate adjacency list for MESH topology
        if ("MESH".equals(topologyType)) {
            topology.setAdjacencyList(generateMeshAdjacencyList(nodes));
        }

        // In TopologyService.java, inside processTopologyInit
// Delete existing topology of the same type before saving new one
        topologyRepository.deleteByTopologyType(topologyType);
        topologyRepository.save(topology);

        // Initialize nodes
        for (String nodeId : nodes) {
            NetworkNode node = new NetworkNode(nodeId, 0, true, topologyType);
            nodeRepository.save(node);
        }
    }

    public void processHeartbeat(Map<String, Object> heartbeatData) {
        Map<String, Integer> nodeLatencies = (Map<String, Integer>) heartbeatData.get("nodes");

        List<String> failedNodes = new ArrayList<>();
        heartbeatLogRepository.save(
                new HeartbeatLog(LocalDateTime.now(), nodeLatencies)
        );
        for (Map.Entry<String, Integer> entry : nodeLatencies.entrySet()) {
            String nodeId = entry.getKey();
            Integer latency = entry.getValue();

            Optional<NetworkNode> existingNode = nodeRepository.findByNodeId(nodeId);

            if (existingNode.isPresent()) {
                NetworkNode node = existingNode.get();

                if (latency == null) {
                    // Node failure detected
                    if (node.getIsActive()) {
                        node.setIsActive(false);
                        failedNodes.add(nodeId);
                    }
                } else {
                    // Node is active
                    node.setIsActive(true);
                    node.setLatency(latency);
                }

                node.setLastSeen(LocalDateTime.now());
                nodeRepository.save(node);
                logger.info("Saved node to MongoDB: {}", node.getNodeId());

            }
        }

        // Notify path service about failures
        if (!failedNodes.isEmpty()) {
            Map<String, Object> failureNotification = new HashMap<>();
            failureNotification.put("failedNodes", failedNodes);
            failureNotification.put("timestamp", LocalDateTime.now());

            kafkaProducerService.sendMessage("node-failure-topic", failureNotification);
        }
    }

    /**
     * Generates a full mesh adjacency list where every node is connected to every other node
     */
    private Map<String, List<String>> generateMeshAdjacencyList(List<String> nodes) {
        Map<String, List<String>> adjacencyList = new HashMap<>();

        for (String node : nodes) {
            List<String> connections = new ArrayList<>();

            // Add all other nodes as connections (full mesh)
            for (String otherNode : nodes) {
                if (!node.equals(otherNode)) {
                    connections.add(otherNode);
                }
            }

            adjacencyList.put(node, connections);
        }

        return adjacencyList;
    }

    public List<List<String>> findPaths(String fromNode, String toNode, String topologyType) {
        Optional<NetworkTopology> topologyOpt = topologyRepository.findByTopologyType(topologyType);

        if (topologyOpt.isEmpty()) {
            throw new IllegalArgumentException("No topology found for type: " + topologyType);
            //return new ArrayList<>();
        }

        NetworkTopology topology = topologyOpt.get();
        if (!topology.getNodes().contains(fromNode) || !topology.getNodes().contains(toNode)) {
            throw new IllegalArgumentException(
                    String.format("Nodes not found in topology: %s or %s", fromNode, toNode));
        }
        Map<String, List<String>> adjacencyList = topology.getAdjacencyList();

        // Get active nodes
        List<NetworkNode> activeNodes = nodeRepository.findByIsActive(true);
        Set<String> activeNodeIds = new HashSet<>();
        for (NetworkNode node : activeNodes) {
            activeNodeIds.add(node.getNodeId());
        }

        // Find all paths using DFS
        List<List<String>> allPaths = new ArrayList<>();
        List<String> currentPath = new ArrayList<>();
        Set<String> visited = new HashSet<>();

        findAllPaths(fromNode, toNode, adjacencyList, activeNodeIds, currentPath, visited, allPaths);

        return allPaths;
    }

    private void findAllPaths(String current, String destination,
                              Map<String, List<String>> adjacencyList,
                              Set<String> activeNodes, List<String> currentPath,
                              Set<String> visited, List<List<String>> allPaths) {

        if (!activeNodes.contains(current)) {
            return; // Skip inactive nodes
        }

        currentPath.add(current);
        visited.add(current);

        if (current.equals(destination)) {
            allPaths.add(new ArrayList<>(currentPath));
        } else {
            List<String> neighbors = adjacencyList.getOrDefault(current, new ArrayList<>());
            for (String neighbor : neighbors) {
                if (!visited.contains(neighbor)) {
                    findAllPaths(neighbor, destination, adjacencyList, activeNodes,
                            currentPath, visited, allPaths);
                }
            }
        }

        // Backtrack
        currentPath.remove(currentPath.size() - 1);
        visited.remove(current);
    }

    // Additional utility methods
    public List<NetworkNode> getAllNodes() {
        return nodeRepository.findAll();
    }

    public List<NetworkNode> getActiveNodes() {
        return nodeRepository.findByIsActive(true);
    }

    public Optional<NetworkNode> getNodeById(String nodeId) {
        return nodeRepository.findByNodeId(nodeId);
    }

    public Optional<NetworkTopology> getTopologyByType(String topologyType) {
        return topologyRepository.findByTopologyType(topologyType);
    }

    public void updateNodeStatus(String nodeId, boolean isActive) {
        Optional<NetworkNode> nodeOpt = nodeRepository.findByNodeId(nodeId);
        if (nodeOpt.isPresent()) {
            NetworkNode node = nodeOpt.get();
            node.setIsActive(isActive);
            node.setLastSeen(LocalDateTime.now());
            nodeRepository.save(node);
        }
    }
}