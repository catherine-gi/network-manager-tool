package com.cath.path_service.service;

import com.cath.path_service.model.NetworkPath;
import com.cath.path_service.repository.NetworkPathRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PathCalculationService {
    private static final Logger logger = LoggerFactory.getLogger(PathCalculationService.class);
    private static final int MAX_PATHS_TO_CALCULATE = 10;

    @Autowired
    private NetworkPathRepository pathRepository;

    @Autowired
    private WebClient.Builder webClientBuilder;

    @Value("${topology.service.url}")
    private String topologyServiceUrl;

    private Map<String, Map<String, Object>> nodeMap;

    public List<NetworkPath> calculatePaths(String fromNode, String toNode, Map<String, Double> weights) {
        weights = normalizeWeights(weights);
        logger.info("Calculating paths from {} to {} with weights: {}", fromNode, toNode, weights);

        pathRepository.deleteByFromNodeAndToNode(fromNode, toNode);

        try {
            Map<String, Object> topologyResponse = webClientBuilder.build()
                    .get()
                    .uri(topologyServiceUrl + "/api/topology/complete")
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> nodes = (List<Map<String, Object>>) topologyResponse.get("nodes");
            @SuppressWarnings("unchecked")
            List<List<String>> edges = (List<List<String>>) topologyResponse.get("edges");

            Map<String, Map<String, Double>> graph = buildWeightedGraph(
                    nodes, edges,
                    weights.getOrDefault("hops", 0.0),
                    weights.getOrDefault("cpu", 0.0),
                    weights.getOrDefault("latency", 0.0)
            );

            if (!graph.containsKey(fromNode) || !graph.containsKey(toNode)) {
                logger.warn("Source or destination node not found in topology");
                return Collections.emptyList();
            }

            List<PathWithWeight> allPaths = findKShortestPaths(graph, fromNode, toNode, MAX_PATHS_TO_CALCULATE);

            List<NetworkPath> networkPaths = new ArrayList<>();
            for (int i = 0; i < allPaths.size(); i++) {
                PathWithWeight p = allPaths.get(i);
                String pathType = i == 0 ? "PRIMARY" : "BACKUP";
                NetworkPath path = createNetworkPath(fromNode, toNode, weights, p, pathType);
                pathRepository.save(path);
                logger.debug("Saved path: {}", path);
                networkPaths.add(path);
            }
            return networkPaths;

        } catch (Exception e) {
            logger.error("Path calculation failed", e);
            return Collections.emptyList();
        }
    }

    public Map<String, List<NetworkPath>> calculateMultiplePaths(
            Map<String, List<String>> pathRequests,
            Map<String, Double> weights) {

        weights = normalizeWeights(weights);
        logger.info("Calculating multiple paths with weights: {}", weights);

        // Get topology once for all calculations
        Map<String, Object> topologyResponse = webClientBuilder.build()
                .get()
                .uri(topologyServiceUrl + "/api/topology/complete")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> nodes = (List<Map<String, Object>>) topologyResponse.get("nodes");
        @SuppressWarnings("unchecked")
        List<List<String>> edges = (List<List<String>>) topologyResponse.get("edges");

        Map<String, Map<String, Double>> graph = buildWeightedGraph(
                nodes, edges,
                weights.getOrDefault("hops", 0.0),
                weights.getOrDefault("cpu", 0.0),
                weights.getOrDefault("latency", 0.0)
        );

        Map<String, List<NetworkPath>> results = new HashMap<>();

        for (Map.Entry<String, List<String>> entry : pathRequests.entrySet()) {
            String fromNode = entry.getKey();
            for (String toNode : entry.getValue()) {
                if (!graph.containsKey(fromNode) || !graph.containsKey(toNode)) {
                    logger.warn("Source {} or destination {} not found in topology", fromNode, toNode);
                    results.put(fromNode + "->" + toNode, Collections.emptyList());
                    continue;
                }

                // Delete existing paths for this pair
                pathRepository.deleteByFromNodeAndToNode(fromNode, toNode);

                List<PathWithWeight> allPaths = findKShortestPaths(graph, fromNode, toNode, MAX_PATHS_TO_CALCULATE);

                List<NetworkPath> networkPaths = new ArrayList<>();
                for (int i = 0; i < allPaths.size(); i++) {
                    PathWithWeight p = allPaths.get(i);
                    String pathType = i == 0 ? "PRIMARY" : "BACKUP";
                    NetworkPath path = createNetworkPath(fromNode, toNode, weights, p, pathType);
                    pathRepository.save(path);
                    networkPaths.add(path);
                }
                results.put(fromNode + "->" + toNode, networkPaths);
            }
        }

        return results;
    }

    private Map<String, Double> normalizeWeights(Map<String, Double> weights) {
        double total = weights.values().stream().mapToDouble(Double::doubleValue).sum();
        if (total <= 0) return Map.of("hops", 0.5, "cpu", 0.25, "latency", 0.25);

        return weights.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue() / total
                ));
    }

    private Map<String, Map<String, Double>> buildWeightedGraph(
            List<Map<String, Object>> nodes,
            List<List<String>> edges,
            double hopsWeight,
            double cpuWeight,
            double latencyWeight) {

        Map<String, Map<String, Double>> graph = new HashMap<>();
        this.nodeMap = nodes.stream()
                .collect(Collectors.toMap(
                        n -> (String) n.get("_id"),
                        n -> n
                ));
        for (Map<String, Object> node : nodes) {
            String nodeId = (String) node.get("_id");
            graph.putIfAbsent(nodeId, new HashMap<>());
        }

        for (List<String> edge : edges) {
            if (edge.size() != 2) continue;

            String source = edge.get(0);
            String target = edge.get(1);

            Map<String, Object> sourceNode = nodeMap.get(source);
            Map<String, Object> targetNode = nodeMap.get(target);

            if (sourceNode == null || targetNode == null) continue;

            double weight = calculateCompositeWeight(sourceNode, targetNode, hopsWeight, cpuWeight, latencyWeight);
            graph.get(source).put(target, weight);

//            graph.computeIfAbsent(source, k -> new HashMap<>()).put(target, weight);
//            graph.computeIfAbsent(target, k -> new HashMap<>()).put(source, weight);
        }

        return graph;
    }

    private double calculateCompositeWeight(
            Map<String, Object> sourceNode,
            Map<String, Object> targetNode,
            double hopsWeight,
            double cpuWeight,
            double latencyWeight) {

        double hopComponent = 1.0 * hopsWeight;

        int sourceCpu = (int) sourceNode.get("cpu");
        int targetCpu = (int) targetNode.get("cpu");
        double cpuComponent = (1.0 / ((sourceCpu + targetCpu) / 2.0)) * cpuWeight;

        int sourceLatency = (int) sourceNode.get("latency");
        int targetLatency = (int) targetNode.get("latency");
        double latencyComponent = ((sourceLatency + targetLatency) / 2.0 / 100.0) * latencyWeight;

        return hopComponent + cpuComponent + latencyComponent;
    }

    private List<PathWithWeight> findKShortestPaths(
            Map<String, Map<String, Double>> graph,
            String source,
            String target,
            int k) {

        List<PathWithWeight> paths = new ArrayList<>();
        PriorityQueue<PathWithWeight> queue = new PriorityQueue<>();

        PathWithWeight shortestPath = dijkstraShortestPath(graph, source, target);
        if (shortestPath == null) return paths;

        paths.add(shortestPath);
        queue.add(new PathWithWeight(shortestPath.path, shortestPath.weight));

        for (int i = 1; i < k; i++) {
            if (queue.isEmpty()) break;

            PathWithWeight currentPath = queue.poll();

            for (int spurNodeIndex = 0; spurNodeIndex < currentPath.path.size() - 1; spurNodeIndex++) {
                String spurNode = currentPath.path.get(spurNodeIndex);
                List<String> rootPath = currentPath.path.subList(0, spurNodeIndex + 1);

                Map<String, Map<String, Double>> modifiedGraph = createModifiedGraph(graph, paths, spurNode, rootPath);

                PathWithWeight spurPath = dijkstraShortestPath(modifiedGraph, spurNode, target);
                if (spurPath == null) continue;

                List<String> totalPath = new ArrayList<>(rootPath);
                totalPath.addAll(spurPath.path.subList(1, spurPath.path.size()));
                double totalWeight = calculatePathWeight(totalPath, graph);

                if (!containsPath(paths, totalPath) && !containsPath(queue, totalPath)) {
                    queue.add(new PathWithWeight(totalPath, totalWeight));
                }
            }
        }

        while (paths.size() < k && !queue.isEmpty()) {
            paths.add(queue.poll());
        }

        return paths;
    }

    private PathWithWeight dijkstraShortestPath(
            Map<String, Map<String, Double>> graph,
            String source,
            String target) {

        PriorityQueue<PathNode> queue = new PriorityQueue<>();
        Map<String, Double> distances = new HashMap<>();
        Map<String, List<String>> paths = new HashMap<>();

        for (String node : graph.keySet()) {
            distances.put(node, Double.MAX_VALUE);
            paths.put(node, new ArrayList<>());
        }
        distances.put(source, 0.0);
        paths.get(source).add(source);
        queue.add(new PathNode(source, 0.0));

        while (!queue.isEmpty()) {
            PathNode current = queue.poll();

            if (current.node.equals(target)) {
                return new PathWithWeight(paths.get(current.node), current.distance);
            }

            if (current.distance > distances.get(current.node)) continue;

            for (Map.Entry<String, Double> neighbor : graph.get(current.node).entrySet()) {
                double newDistance = current.distance + neighbor.getValue();
                if (newDistance < distances.get(neighbor.getKey())) {
                    distances.put(neighbor.getKey(), newDistance);
                    List<String> newPath = new ArrayList<>(paths.get(current.node));
                    newPath.add(neighbor.getKey());
                    paths.put(neighbor.getKey(), newPath);
                    queue.add(new PathNode(neighbor.getKey(), newDistance));
                }
            }
        }

        return null;
    }

    private Map<String, Map<String, Double>> createModifiedGraph(
            Map<String, Map<String, Double>> originalGraph,
            List<PathWithWeight> existingPaths,
            String spurNode,
            List<String> rootPath) {

        Map<String, Map<String, Double>> modifiedGraph = new HashMap<>();
        originalGraph.forEach((node, neighbors) -> {
            modifiedGraph.put(node, new HashMap<>(neighbors));
        });

        for (PathWithWeight path : existingPaths) {
            if (path.path.size() > rootPath.size() &&
                    path.path.subList(0, rootPath.size()).equals(rootPath)) {

                String node1 = path.path.get(rootPath.size() - 1);
                String node2 = path.path.get(rootPath.size());

                modifiedGraph.get(node1).remove(node2);
                modifiedGraph.get(node2).remove(node1);
            }
        }

        for (String node : rootPath) {
            if (!node.equals(spurNode)) {
                modifiedGraph.remove(node);
                modifiedGraph.values().forEach(neighbors -> neighbors.remove(node));
            }
        }

        return modifiedGraph;
    }

    private double calculatePathWeight(List<String> path, Map<String, Map<String, Double>> graph) {
        double totalWeight = 0.0;
        for (int i = 0; i < path.size() - 1; i++) {
            String current = path.get(i);
            String next = path.get(i + 1);
            totalWeight += graph.get(current).get(next);
        }
        return totalWeight;
    }

    private boolean containsPath(Collection<PathWithWeight> pathCollection, List<String> path) {
        return pathCollection.stream().anyMatch(p -> p.path.equals(path));
    }

    private NetworkPath createNetworkPath(
            String fromNode,
            String toNode,
            Map<String, Double> weights,
            PathWithWeight pathWithWeight,
            String pathType) {

        NetworkPath path = new NetworkPath(fromNode, toNode, pathWithWeight.path, pathType);
        path.setWeight(pathWithWeight.weight);
        path.setHopCount(pathWithWeight.path.size() - 1);
        path.setTotalLatency(calculatePathLatency(pathWithWeight.path));
        path.setIsActive(true);
        path.setCalculatedAt(LocalDateTime.now());
        return path;
    }

    private int calculatePathLatency(List<String> path) {
        if (path.size() < 2 || nodeMap == null) return 0;

        int totalLatency = 0;

        for (int i = 0; i < path.size() - 1; i++) {
            String currentNode = path.get(i);
            String nextNode = path.get(i + 1);

            Map<String, Object> currentNodeData = nodeMap.get(currentNode);
            Map<String, Object> nextNodeData = nodeMap.get(nextNode);

            if (currentNodeData != null && currentNodeData.containsKey("latency")) {
                totalLatency += (int) currentNodeData.get("latency");
            }
            if (nextNodeData != null && nextNodeData.containsKey("latency")) {
                totalLatency += (int) nextNodeData.get("latency");
            }
        }

        return totalLatency / (path.size() - 1);
    }

    public void handleNodeFailure(List<String> failedNodes) {
        logger.info("Handling node failure for nodes: {}", failedNodes);
        // Find all paths that include any of the failed nodes and deactivate them
        List<NetworkPath> affectedPaths = pathRepository.findAll().stream()
                .filter(path -> path.getPath().stream().anyMatch(failedNodes::contains))
                .collect(Collectors.toList());

        for (NetworkPath path : affectedPaths) {
            path.setIsActive(false);
            pathRepository.save(path);
            logger.info("Deactivated path due to node failure: {}", path.getPath());
        }
    }

    private static class PathNode implements Comparable<PathNode> {
        String node;
        double distance;

        public PathNode(String node, double distance) {
            this.node = node;
            this.distance = distance;
        }

        @Override
        public int compareTo(PathNode other) {
            return Double.compare(this.distance, other.distance);
        }
    }

    private static class PathWithWeight implements Comparable<PathWithWeight> {
        List<String> path;
        double weight;

        public PathWithWeight(List<String> path, double weight) {
            this.path = path;
            this.weight = weight;
        }

        @Override
        public int compareTo(PathWithWeight other) {
            return Double.compare(this.weight, other.weight);
        }
    }
}