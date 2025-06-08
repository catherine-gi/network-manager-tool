package com.cath.producer_service.controller;

import com.cath.producer_service.model.NodeFailureRequest;
import com.cath.producer_service.model.TopologyRequest;
import com.cath.producer_service.service.NetworkSimulationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/network")
@CrossOrigin(origins = "http://localhost:3000")
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
}