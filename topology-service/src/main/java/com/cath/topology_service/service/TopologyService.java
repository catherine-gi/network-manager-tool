package com.cath.topology_service.service;

import com.cath.topology_service.model.Edge;
import com.cath.topology_service.model.HeartbeatLog;
import com.cath.topology_service.model.NetworkNode;
import com.cath.topology_service.model.NetworkTopology;
import com.cath.topology_service.repository.HeartbeatLogRepository;
import com.cath.topology_service.repository.NetworkNodeRepository;
import com.cath.topology_service.repository.NetworkTopologyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

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

    // Stores failed edges as normalized "source-target" strings
    private final Set<String> failedEdges = new HashSet<>();

    public void processTopologyInit(Map<String, Object> topologyData) {
        try {
            List<Map<String, Object>> nodes = (List<Map<String, Object>>) topologyData.get("nodes");
            List<Map<String, Object>> edges = (List<Map<String, Object>>) topologyData.get("edges");

            Map<String, List<String>> adjacencyList = createAdjacencyList(edges);
            logger.debug("Created adjacency list: {}", adjacencyList);

            NetworkTopology topology = new NetworkTopology();
            topology.setNodes(nodes.stream()
                    .map(n -> (String) n.get("id"))
                    .collect(Collectors.toList()));
            topology.setAdjacencyList(adjacencyList);

            topologyRepository.deleteAll();
            nodeRepository.deleteAll();
            failedEdges.clear();

            topologyRepository.save(topology);
            initializeNodes(nodes);

            logger.info("Topology initialized with {} nodes and {} edges", nodes.size(), edges.size());
        } catch (Exception e) {
            logger.error("Error processing topology initialization", e);
            throw new RuntimeException("Failed to initialize topology", e);
        }
    }

    public void processHeartbeat(Map<String, Object> heartbeatData) {
        try {
            Map<String, Map<String, Object>> nodeStatuses =
                    (Map<String, Map<String, Object>>) heartbeatData.get("nodeStatuses");
            List<String> newFailedEdges = (List<String>) heartbeatData.get("failedEdges");
            Object timestampObj = heartbeatData.get("timestamp");
            Date timestamp;

            if (timestampObj instanceof Date) {
                timestamp = (Date) timestampObj;
            } else if (timestampObj instanceof String) {
                try {
                    timestamp = Date.from(java.time.Instant.parse((String) timestampObj));
                } catch (Exception e) {
                    logger.warn("Failed to parse timestamp string: {}, using current time", timestampObj);
                    timestamp = new Date();
                }
            } else if (timestampObj instanceof Long) {
                timestamp = new Date((Long) timestampObj);
            } else {
                logger.warn("Unknown timestamp format: {}, using current time", timestampObj);
                timestamp = new Date();
            }

            logger.debug("Processing heartbeat at {} with {} nodes and {} edges",
                    timestamp, nodeStatuses.size(), newFailedEdges != null ? newFailedEdges.size() : 0);

            logger.info("=== HEARTBEAT DEBUG ===");
            logger.info("Node Statuses: {}", nodeStatuses);
            logger.info("Failed Edges: {}", newFailedEdges);
            logger.info("Current Failed Edges Set: {}", failedEdges);
            logger.info("Timestamp: {} ({})", timestamp, timestampObj.getClass().getSimpleName());
            logger.info("========================");

            logHeartbeatLatency(nodeStatuses);

            List<String> failedNodes = processNodeStatuses(nodeStatuses, timestamp);

            boolean edgeStatusChanged = processEdgeStatusChanges(newFailedEdges);
            debugEdgeState();

            if (!failedNodes.isEmpty() || edgeStatusChanged) {
                logger.info("Topology changes detected - failed nodes: {}, failed edges: {}",
                        failedNodes, failedEdges);
                notifyFailureAndRecalculatePaths(failedNodes, new ArrayList<>(failedEdges));
            }
        } catch (Exception e) {
            logger.error("Error processing heartbeat", e);
        }
    }

    private boolean processEdgeStatusChanges(List<String> newFailedEdges) {
        boolean edgeStatusChanged = false;
        if (newFailedEdges != null) {
            logger.info("Processing new failed edges: {}", newFailedEdges);

            for (String edgeKey : newFailedEdges) {
                String normalizedKey = normalizeEdgeKey(edgeKey);
                logger.info("Original edge key: '{}', Normalized key: '{}'", edgeKey, normalizedKey);

                if (!failedEdges.contains(normalizedKey)) {
                    failedEdges.add(normalizedKey);
                    edgeStatusChanged = true;
                    logger.info("Edge {} marked as failed. Current failed edges: {}", normalizedKey, failedEdges);
                } else {
                    logger.info("Edge {} was already marked as failed", normalizedKey);
                }
            }
        }
        return edgeStatusChanged;
    }

    private String normalizeEdgeKey(String edgeKey) {
        String normalized = edgeKey.replace("reactflow__edge-", "");
        logger.debug("Normalizing edge key: '{}' -> '{}'", edgeKey, normalized);
        return normalized;
    }

    private boolean isEdgeFailed(String source, String target) {
        String edgeKey = source + "-" + target;
        boolean isFailed = failedEdges.contains(edgeKey);
        logger.info("Checking edge '{}': failed={}, failedEdges contains: {}", edgeKey, isFailed, failedEdges);
        return isFailed;
    }

    public void debugEdgeState() {
        logger.info("=== EDGE STATE DEBUG ===");
        logger.info("Failed Edges Set: {}", failedEdges);
        logger.info("Failed Edges Size: {}", failedEdges.size());
        failedEdges.forEach(edge -> logger.info("Failed Edge: '{}'", edge));
        logger.info("========================");
    }

    private Map<String, List<String>> createAdjacencyList(List<Map<String, Object>> edges) {
        Map<String, List<String>> adjacencyList = new HashMap<>();

        for (Map<String, Object> edge : edges) {
            String source = (String) edge.get("source");
            String target = (String) edge.get("target");

            if (source == null || target == null) {
                logger.warn("Skipping edge with null source or target");
                continue;
            }

            adjacencyList.computeIfAbsent(source, k -> new ArrayList<>()).add(target);
        }
        return adjacencyList;
    }

    private void initializeNodes(List<Map<String, Object>> nodes) {
        for (Map<String, Object> node : nodes) {
            String nodeId = (String) node.get("id");
            if (nodeId == null) {
                logger.warn("Skipping node with null ID");
                continue;
            }

            NetworkNode networkNode = new NetworkNode();
            networkNode.setId(nodeId);
            networkNode.setIsActive(!"failed".equalsIgnoreCase((String) node.get("status")));
            networkNode.setLastSeen(LocalDateTime.now());
            networkNode.setCpu((Integer) node.get("cpu"));
            networkNode.setLatency((Integer) node.get("latency"));
            nodeRepository.save(networkNode);
        }
    }

    public List<List<String>> findPaths(String fromNode, String toNode) {
        Optional<NetworkTopology> topologyOpt = topologyRepository.findAll().stream().findFirst();
        if (topologyOpt.isEmpty()) {
            throw new IllegalArgumentException("No topology found");
        }

        NetworkTopology topology = topologyOpt.get();
        validateNodesInTopology(fromNode, toNode, topology);

        Map<String, List<String>> adjacencyList = topology.getAdjacencyList();
        Set<String> activeNodeIds = getActiveNodeIds();

        logger.debug("Finding paths from {} to {} with active nodes: {}",
                fromNode, toNode, activeNodeIds);
        logger.debug("Current failed edges: {}", failedEdges);

        Map<String, List<String>> filteredAdjacencyList = createFilteredAdjacencyList(adjacencyList, activeNodeIds);

        logger.debug("Filtered adjacency list: {}", filteredAdjacencyList);

        List<List<String>> allPaths = new ArrayList<>();
        findAllPaths(fromNode, toNode, filteredAdjacencyList, activeNodeIds,
                new ArrayList<>(), new HashSet<>(), allPaths);

        logger.debug("Found {} paths from {} to {}", allPaths.size(), fromNode, toNode);
        return allPaths;
    }

    private Map<String, List<String>> createFilteredAdjacencyList(
            Map<String, List<String>> adjacencyList, Set<String> activeNodeIds) {
        Map<String, List<String>> filteredAdjacencyList = new HashMap<>();

        logger.info("=== FILTERING ADJACENCY LIST ===");
        logger.info("Active nodes: {}", activeNodeIds);
        logger.info("Failed edges: {}", failedEdges);
        logger.info("Original adjacency list: {}", adjacencyList);

        for (Map.Entry<String, List<String>> entry : adjacencyList.entrySet()) {
            String source = entry.getKey();
            if (!activeNodeIds.contains(source)) {
                logger.info("Skipping inactive source node: {}", source);
                continue;
            }

            List<String> filteredTargets = entry.getValue().stream()
                    .peek(target -> logger.debug("Processing edge: {} -> {}", source, target))
                    .filter(target -> {
                        if (!activeNodeIds.contains(target)) {
                            logger.info("Skipping inactive target node: {} from source: {}", target, source);
                            return false;
                        }
                        return true;
                    })
                    .filter(target -> {
                        if (isEdgeFailed(source, target)) {
                            logger.info("Skipping failed edge: {} -> {}", source, target);
                            return false;
                        }
                        return true;
                    })
                    .peek(target -> logger.info("Including active edge: {} -> {}", source, target))
                    .collect(Collectors.toList());

            if (!filteredTargets.isEmpty()) {
                filteredAdjacencyList.put(source, filteredTargets);
            }
        }

        logger.info("Filtered adjacency list: {}", filteredAdjacencyList);
        logger.info("================================");

        return filteredAdjacencyList;
    }

    private void logHeartbeatLatency(Map<String, Map<String, Object>> nodeStatuses) {
        Map<String, Integer> latencyMap = nodeStatuses.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> (Integer) e.getValue().get("latency")
                ));
        heartbeatLogRepository.save(new HeartbeatLog(LocalDateTime.now(), latencyMap));
    }

    private List<String> processNodeStatuses(Map<String, Map<String, Object>> nodeStatuses, Date timestamp) {
        List<String> failedNodes = new ArrayList<>();

        for (Map.Entry<String, Map<String, Object>> entry : nodeStatuses.entrySet()) {
            String nodeId = entry.getKey();
            Map<String, Object> status = entry.getValue();

            Optional<NetworkNode> nodeOpt = nodeRepository.findById(nodeId);
            if (nodeOpt.isPresent()) {
                NetworkNode node = nodeOpt.get();

                boolean isActive = "active".equalsIgnoreCase((String) status.get("status"));
                boolean wasActive = node.getIsActive();
                node.setIsActive(isActive);
                node.setLastSeen(LocalDateTime.now());

                if (!isActive && wasActive) {
                    failedNodes.add(nodeId);
                    logger.info("Node {} failed at {}", nodeId, timestamp);
                } else if (isActive && !wasActive) {
                    logger.info("Node {} recovered at {}", nodeId, timestamp);
                }

                nodeRepository.save(node);
            }
        }
        return failedNodes;
    }

    private void notifyFailureAndRecalculatePaths(List<String> failedNodes, List<String> failedEdges) {
        Map<String, Object> failureNotification = new HashMap<>();
        failureNotification.put("failedNodes", failedNodes);
        failureNotification.put("failedEdges", failedEdges);
        failureNotification.put("timestamp", LocalDateTime.now());

        kafkaProducerService.sendMessage("failure-notification-topic", failureNotification);
        logger.info("Detected topology changes - Nodes: {}, Edges: {}", failedNodes, failedEdges);

        recalculatePathsForTopology();
    }

    private void recalculatePathsForTopology() {
        Optional<NetworkTopology> topologyOpt = topologyRepository.findAll().stream().findFirst();
        if (topologyOpt.isPresent()) {
            NetworkTopology topology = topologyOpt.get();
            List<String> nodes = topology.getNodes();
            if (nodes.size() >= 2) {
                String fromNode = nodes.get(0);
                String toNode = nodes.get(1);

                List<List<String>> paths = findPaths(fromNode, toNode);

                Map<String, Object> pathData = new HashMap<>();
                pathData.put("fromNode", fromNode);
                pathData.put("toNode", toNode);
                pathData.put("paths", paths);
                pathData.put("timestamp", LocalDateTime.now());

                kafkaProducerService.sendMessage("path-update-topic", pathData);
                logger.info("Recalculated paths for topology");
            }
        }
    }

    private void validateNodesInTopology(String fromNode, String toNode, NetworkTopology topology) {
        if (!topology.getNodes().contains(fromNode)) {
            throw new IllegalArgumentException("Source node not found in topology: " + fromNode);
        }
        if (!topology.getNodes().contains(toNode)) {
            throw new IllegalArgumentException("Destination node not found in topology: " + toNode);
        }
    }

    private Set<String> getActiveNodeIds() {
        return nodeRepository.findByIsActive(true).stream()
                .map(NetworkNode::getNodeId)
                .collect(Collectors.toSet());
    }

    private void findAllPaths(String current, String destination,
                              Map<String, List<String>> adjacencyList,
                              Set<String> activeNodes, List<String> currentPath,
                              Set<String> visited, List<List<String>> allPaths) {
        if (!activeNodes.contains(current)) {
            return;
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

        currentPath.remove(currentPath.size() - 1);
        visited.remove(current);
    }
    // In TopologyService

    public List<Map<String, Object>> getAllNodes() {
        try {
            List<NetworkNode> nodes = nodeRepository.findAll();

            return nodes.stream()
                    .map(node -> {
                        Map<String, Object> nodeMap = new LinkedHashMap<>();
                        nodeMap.put("_id", node.getId());
                        nodeMap.put("latency", node.getLatency() != null ? node.getLatency() : 0);
                        nodeMap.put("status", Boolean.TRUE.equals(node.getIsActive()) ? "active" : "failed");
                        nodeMap.put("lastSeen", node.getLastSeen() != null ? node.getLastSeen().toString() : "");
                        nodeMap.put("cpu", node.getCpu() != null ? node.getCpu() : 0);
                        return nodeMap;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Failed to get nodes", e);
            return Collections.emptyList();
        }
    }


    public List<List<String>> getAllEdges() {
        Optional<NetworkTopology> topologyOpt = topologyRepository.findAll().stream().findFirst();
        if (topologyOpt.isEmpty()) {
            return Collections.emptyList();
        }

        NetworkTopology topology = topologyOpt.get();
        List<List<String>> edges = new ArrayList<>();

        topology.getAdjacencyList().forEach((source, targets) -> {
            targets.forEach(target -> {
                edges.add(List.of(source, target));
            });
        });

        return edges;
    }

    public List<NetworkNode> getFailedNodes() {
        return nodeRepository.findByIsActive(false);
    }

    public List<NetworkNode> getActiveNodes() {
        return nodeRepository.findByIsActive(true);
    }
//    public List<Edge> getAllFailedEdges() {
//        return edges.values().stream()
//                .filter(edge -> "FAILED".equals(edge.getStatus()))
//                .collect(Collectors.toList());
//    }
//    public Map<String, Edge> getAllEdges() {
//        return new HashMap<>(edges);
//    }

    public Optional<NetworkNode> getNodeById(String nodeId) {
        return nodeRepository.findById(nodeId);
    }

    public Optional<NetworkTopology> getTopology() {
        return topologyRepository.findAll().stream().findFirst();
    }

    public void updateNodeStatus(String nodeId, boolean isActive) {
        Optional<NetworkNode> nodeOpt = nodeRepository.findById(nodeId);
        if (nodeOpt.isPresent()) {
            NetworkNode node = nodeOpt.get();
            node.setIsActive(isActive);
            node.setLastSeen(LocalDateTime.now());
            nodeRepository.save(node);
        }
    }

    public List<List<String>> getAllFailedEdges() {
        return failedEdges.stream()
                .map(edge -> Arrays.asList(edge.split("-")))
                .collect(Collectors.toList());
    }


    public void debugTopologyState() {
        Optional<NetworkTopology> topologyOpt = topologyRepository.findAll().stream().findFirst();
        if (topologyOpt.isPresent()) {
            NetworkTopology topology = topologyOpt.get();
            logger.info("=== TOPOLOGY STATE DEBUG ===");
            logger.info("Nodes: {}", topology.getNodes());
            logger.info("Adjacency List: {}", topology.getAdjacencyList());
            logger.info("Failed Edges: {}", failedEdges);
            logger.info("Active Nodes: {}", getActiveNodeIds());
            logger.info("============================");
        } else {
            logger.info("No topology found in database");
        }
    }

}
