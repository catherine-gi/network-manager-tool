package com.cath.path_service.controller;

import com.cath.path_service.model.NetworkPath;
import com.cath.path_service.service.PathCalculationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/paths")
@CrossOrigin(origins = "http://localhost:5173")
public class PathController {

    @Autowired
    private PathCalculationService pathCalculationService;

    @PostMapping("/calculate")
    public ResponseEntity<Map<String, Object>> calculatePaths(@RequestBody Map<String, Object> request) {
        try {
            String fromNode = (String) request.get("fromNode");
            String toNode = (String) request.get("toNode");

            @SuppressWarnings("unchecked")
            Map<String, Double> weights = (Map<String, Double>) request.get("weights");

            if (fromNode == null || toNode == null) {
                throw new IllegalArgumentException("Both fromNode and toNode must be provided");
            }

            List<NetworkPath> paths = pathCalculationService.calculatePaths(
                    fromNode,
                    toNode,
                    weights != null ? weights : Map.of(
                            "hops", 0.5,
                            "cpu", 0.25,
                            "latency", 0.25
                    )
            );

            Map<String, Object> response = new HashMap<>();
            response.put("paths", paths.stream().map(NetworkPath::getPath).collect(Collectors.toList()));
            response.put("pathCount", paths.size());
            response.put("status", "success");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "Failed to calculate paths: " + e.getMessage());
            errorResponse.put("status", "error");

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

//    @GetMapping("/active")
//    public ResponseEntity<Map<String, Object>> getActivePaths(
//            @RequestParam(required = false) String fromNode,
//            @RequestParam(required = false) String toNode) {
//
//        try {
//            List<NetworkPath> paths;
//
//            if (fromNode != null && toNode != null) {
//                paths = pathCalculationService.getActivePaths(fromNode, toNode);
//            } else {
//                paths = pathCalculationService.getAllActivePaths();
//            }
//
//            Map<String, Object> response = new HashMap<>();
//            response.put("paths", paths);
//            response.put("pathCount", paths.size());
//            response.put("status", "success");
//
//            return ResponseEntity.ok(response);
//        } catch (Exception e) {
//            Map<String, Object> errorResponse = new HashMap<>();
//            errorResponse.put("message", "Failed to get active paths: " + e.getMessage());
//            errorResponse.put("status", "error");
//
//            return ResponseEntity.badRequest().body(errorResponse);
//        }
//    }
//
//    @GetMapping("/primary")
//    public ResponseEntity<Map<String, Object>> getPrimaryPath(
//            @RequestParam String fromNode,
//            @RequestParam String toNode) {
//
//        try {
//            NetworkPath primaryPath = pathCalculationService.getPrimaryPath(fromNode, toNode);
//
//            Map<String, Object> response = new HashMap<>();
//            response.put("primaryPath", primaryPath);
//            response.put("status", "success");
//
//            return ResponseEntity.ok(response);
//        } catch (Exception e) {
//            Map<String, Object> errorResponse = new HashMap<>();
//            errorResponse.put("message", "Failed to get primary path: " + e.getMessage());
//            errorResponse.put("status", "error");
//
//            return ResponseEntity.badRequest().body(errorResponse);
//        }
//    }
//
//    @GetMapping("/backup")
//    public ResponseEntity<Map<String, Object>> getBackupPaths(
//            @RequestParam String fromNode,
//            @RequestParam String toNode) {
//
//        try {
//            List<NetworkPath> backupPaths = pathCalculationService.getBackupPaths(fromNode, toNode);
//
//            Map<String, Object> response = new HashMap<>();
//            response.put("backupPaths", backupPaths);
//            response.put("pathCount", backupPaths.size());
//            response.put("status", "success");
//
//            return ResponseEntity.ok(response);
//        } catch (Exception e) {
//            Map<String, Object> errorResponse = new HashMap<>();
//            errorResponse.put("message", "Failed to get backup paths: " + e.getMessage());
//            errorResponse.put("status", "error");
//
//            return ResponseEntity.badRequest().body(errorResponse);
//        }
//    }
}