package com.cath.topology_service.controller;



import com.cath.topology_service.model.PathRequest;
import com.cath.topology_service.service.TopologyService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/topology")
@CrossOrigin(origins = "http://localhost:3000")
public class TopologyController {

    @Autowired
    private TopologyService topologyService;

    @PostMapping("/paths")
    public ResponseEntity<Map<String, Object>> findPathsBetweenNodes(
            @RequestBody @Valid PathRequest request) {

        if (request.getFromNode() == null || request.getToNode() == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "fromNode and toNode are required"
            ));
        }

        try {

            List<List<String>> paths = topologyService.findPaths(
                    request.getFromNode(),
                    request.getToNode(),
                    request.getTopologyType());

            return ResponseEntity.ok(Map.of(
                    "paths", paths,
                    "status", "success"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

}