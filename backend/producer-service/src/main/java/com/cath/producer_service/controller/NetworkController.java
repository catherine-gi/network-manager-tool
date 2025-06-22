package com.cath.producer_service.controller;

import com.cath.producer_service.model.*;
import com.cath.producer_service.service.NetworkSimulationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/network")
@CrossOrigin(origins = "http://localhost:5173")
public class NetworkController {

    @Autowired
    private NetworkSimulationService networkSimulationService;

    @PostMapping("/initialize")
    public ResponseEntity<Map<String, String>> initializeTopology(@Valid @RequestBody TopologyRequest request) {
        try {
            networkSimulationService.initializeTopology(request);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Topology details sent successfully");
            response.put("status", "success");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Failed to initialize topology: " + e.getMessage());
            errorResponse.put("status", "error");

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    @PostMapping("/update-node")
    public ResponseEntity<Map<String, String>> updateNodeProperties(
            @Valid @RequestBody NodeUpdateRequest request) {
        try {
            networkSimulationService.updateNodeProperties(
                    request.getNodeId(),
                    request.getCpu(),
                    request.getLatency(),
                    request.getStatus()
            );

            Map<String, String> response = new HashMap<>();
            response.put("message", "Node properties updated successfully");
            response.put("status", "success");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Failed to update node: " + e.getMessage());
            errorResponse.put("status", "error");
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/down")
    public ResponseEntity<Map<String, String>> simulateNodeFailure(@Valid @RequestBody NodeFailureRequest request) {
        try {
            networkSimulationService.simulateNodeFailure(request.getNodeId());

            Map<String, String> response = new HashMap<>();
            response.put("message", "Node " + request.getNodeId() + " marked as failed");
            response.put("status", "success");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Failed to simulate node failure: " + e.getMessage());
            errorResponse.put("status", "error");

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/restore")
    public ResponseEntity<Map<String, String>> restoreNode(@Valid @RequestBody NodeFailureRequest request) {
        try {
            networkSimulationService.restoreNode(request.getNodeId());

            Map<String, String> response = new HashMap<>();
            response.put("message", "Node " + request.getNodeId() + " restored");
            response.put("status", "success");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Failed to restore node: " + e.getMessage());
            errorResponse.put("status", "error");

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getNetworkStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("activeNodes", networkSimulationService.getActiveNodes());
        status.put("failedNodes", networkSimulationService.getFailedNodes());
        status.put("status", "success");

        return ResponseEntity.ok(status);
    }

    @PostMapping("/stop")
    public ResponseEntity<Map<String, String>> stopSimulation() {
        networkSimulationService.stopSimulation();

        Map<String, String> response = new HashMap<>();
        response.put("message", "Simulation stopped");
        response.put("status", "success");

        return ResponseEntity.ok(response);
    }
    @PostMapping("/update-edge")
    public ResponseEntity<Map<String, String>> updateEdgeProperties(
            @Valid @RequestBody EdgeUpdateRequest request) {
        try {
            networkSimulationService.updateEdgeProperties(
                    request.getEdgeId(),
                    request.getSource(),
                    request.getTarget(),
                    request.getStatus(),
                    request.getLatency()
            );

            Map<String, String> response = new HashMap<>();
            response.put("message", "Edge properties updated successfully");
            response.put("status", "success");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Failed to update edge: " + e.getMessage());
            errorResponse.put("status", "error");
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/edge-down")
    public ResponseEntity<Map<String, String>> simulateEdgeFailure(@Valid @RequestBody EdgeFailureRequest request) {
        try {
            networkSimulationService.simulateEdgeFailure(
                    request.getEdgeId(),
                    request.getSource(),
                    request.getTarget()
            );

            Map<String, String> response = new HashMap<>();
            response.put("message", "Edge " + request.getSource() + "-" + request.getTarget() + " marked as failed");
            response.put("status", "success");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Failed to simulate edge failure: " + e.getMessage());
            errorResponse.put("status", "error");
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/edge-restore")
    public ResponseEntity<Map<String, String>> restoreEdge(@Valid @RequestBody EdgeFailureRequest request) {
        try {
            networkSimulationService.restoreEdge(
                    request.getEdgeId(),
                    request.getSource(),
                    request.getTarget()
            );

            Map<String, String> response = new HashMap<>();
            response.put("message", "Edge " + request.getSource() + "-" + request.getTarget() + " restored");
            response.put("status", "success");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Failed to restore edge: " + e.getMessage());
            errorResponse.put("status", "error");
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    @PostMapping("/add-node")
    public ResponseEntity<Map<String, String>> addNode(@Valid @RequestBody NodeAddRequest request) {
        try {
            networkSimulationService.addNode(request);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Node added successfully");
            response.put("status", "success");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Failed to add node: " + e.getMessage());
            errorResponse.put("status", "error");
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/delete-node")
    public ResponseEntity<Map<String, String>> deleteNode(@Valid @RequestBody NodeDeleteRequest request) {
        try {
            networkSimulationService.deleteNode(request.getNodeId());

            Map<String, String> response = new HashMap<>();
            response.put("message", "Node deleted successfully");
            response.put("status", "success");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Failed to delete node: " + e.getMessage());
            errorResponse.put("status", "error");
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/add-edge")
    public ResponseEntity<Map<String, String>> addEdge(@Valid @RequestBody EdgeAddRequest request) {
        try {
            networkSimulationService.addEdge(request);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Edge added successfully");
            response.put("status", "success");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Failed to add edge: " + e.getMessage());
            errorResponse.put("status", "error");
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/delete-edge")
    public ResponseEntity<Map<String, String>> deleteEdge(@Valid @RequestBody EdgeDeleteRequest request) {
        try {
            networkSimulationService.deleteEdge(request.getEdgeId());

            Map<String, String> response = new HashMap<>();
            response.put("message", "Edge deleted successfully");
            response.put("status", "success");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Failed to delete edge: " + e.getMessage());
            errorResponse.put("status", "error");
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}