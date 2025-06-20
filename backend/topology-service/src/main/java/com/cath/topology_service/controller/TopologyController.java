package com.cath.topology_service.controller;

import com.cath.topology_service.model.NetworkNode;
import com.cath.topology_service.model.PathRequest;
import com.cath.topology_service.repository.NetworkNodeRepository;
import com.cath.topology_service.service.TopologyService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/topology")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class TopologyController {

    @Autowired
    private TopologyService topologyService;
    @Autowired
    private NetworkNodeRepository nodeRepository;

    @PostMapping("/paths")
    public ResponseEntity<Map<String, Object>> findPathsBetweenNodes(
            @RequestBody @Valid PathRequest request) {

        try {
            List<List<String>> paths = topologyService.findPaths(
                    request.getFromNode(),
                    request.getToNode());

            return ResponseEntity.ok(Map.of(
                    "paths", paths,
                    "status", "success",
                    "pathCount", paths.size(),
                    "fromNode", request.getFromNode(),
                    "toNode", request.getToNode()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", "Internal server error: " + e.getMessage()
            ));
        }
    }
//    @PostMapping("/paths/{from}/{to}")
//    public ResponseEntity<Map<String, Object>> findPathsBetweenNodesGet(
//            @PathVariable String from,
//            @PathVariable String to,
//            @RequestBody(required = false) Map<String, Object> requestBody) {
//
//        if (from == null || to == null) {
//            return ResponseEntity.badRequest().body(Map.of(
//                    "status", "error",
//                    "message", "from and to are required"
//            ));
//        }
//
//        try {
//            // Extract weights from request body or use defaults
//            Map<String, Double> weights = new HashMap<>();
//            if (requestBody != null && requestBody.containsKey("weights")) {
//                @SuppressWarnings("unchecked")
//                Map<String, Double> requestWeights = (Map<String, Double>) requestBody.get("weights");
//                weights.putAll(requestWeights);
//            } else {
//                weights.put("hops", 0.5);
//                weights.put("cpu", 0.25);
//                weights.put("latency", 0.25);
//            }
//
//            List<List<String>> paths = topologyService.findPaths(from, to, weights);
//
//            return ResponseEntity.ok(Map.of(
//                    "paths", paths,
//                    "status", "success",
//                    "pathCount", paths.size()
//            ));
//        } catch (Exception e) {
//            return ResponseEntity.internalServerError().body(Map.of(
//                    "status", "error",
//                    "message", e.getMessage()
//            ));
//        }
//    }


    @GetMapping("/nodes/active")
    public ResponseEntity<Map<String, Object>> getActiveNodes() {
        try {
            return ResponseEntity.ok(Map.of(
                    "nodes", topologyService.getActiveNodes(),
                    "status", "success"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/nodes/failed")
    public ResponseEntity<Map<String, Object>> getFailedNodes() {
        try {
            return ResponseEntity.ok(Map.of(
                    "nodes", topologyService.getFailedNodes(),
                    "status", "success"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/edges/failed")
    public ResponseEntity<Map<String, Object>> getFailedEdges() {
        try {
            return ResponseEntity.ok(Map.of(
                    "edges", topologyService.getAllFailedEdges(),
                    "status", "success"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "topology-service",
                "totalNodes", topologyService.getAllNodes().size(),
                "activeNodes", topologyService.getActiveNodes().size()
                //"totalEdges", topologyService.getAllEdges().size()
        ));
    }
    // Add these to your existing TopologyController

    @GetMapping("/nodes")
    public ResponseEntity<Map<String, Object>> getAllNodes() {
        try {
            return ResponseEntity.ok(Map.of(
                    "nodes", topologyService.getAllNodes(),
                    "status", "success"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/edges")
    public ResponseEntity<Map<String, Object>> getAllEdges() {
        try {
            return ResponseEntity.ok(Map.of(
                    "edges", topologyService.getAllEdges(),
                    "status", "success"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }
    @GetMapping("/debug/nodes/raw")
    public ResponseEntity<List<NetworkNode>> getRawNodes() {

        return ResponseEntity.ok(nodeRepository.findAll());
    }

    // Example: TopologyController.java
    @GetMapping("/complete")
    public Map<String, Object> getCompleteTopology() {
        List<Map<String, Object>> nodes = topologyService.getAllNodes();
        List<List<String>> edges = topologyService.getAllEdges();
        List<List<String>> failedEdges = topologyService.getAllFailedEdges();

        // Remove failed edges from the edge list
        Set<String> failedEdgeSet = failedEdges.stream()
                .map(edge -> edge.get(0) + "-" + edge.get(1))
                .collect(Collectors.toSet());

        List<List<String>> filteredEdges = edges.stream()
                .filter(edge -> !failedEdgeSet.contains(edge.get(0) + "-" + edge.get(1)))
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("nodes", nodes);
        result.put("edges", filteredEdges);
        result.put("failedEdges", failedEdges);
        return result;
    }
}