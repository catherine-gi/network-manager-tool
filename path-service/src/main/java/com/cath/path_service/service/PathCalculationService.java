package com.cath.path_service.service;

import com.cath.path_service.model.NetworkPath;
import com.cath.path_service.repository.NetworkPathRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PathCalculationService {
    private static final Logger logger = LoggerFactory.getLogger(PathCalculationService.class);

    @Autowired
    private NetworkPathRepository pathRepository;

    @Autowired
    private WebClient.Builder webClientBuilder;

    @Value("${topology.service.url}")
    private String topologyServiceUrl;

    public List<NetworkPath> calculatePaths(String fromNode, String toNode) {
        // Clear existing paths
        pathRepository.deleteByFromNodeAndToNode(fromNode, toNode);

        try {
            Map<String, Object> response = webClientBuilder.build()
                    .post()
                    .uri(topologyServiceUrl + "/api/topology/paths")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of(
                            "fromNode", fromNode,
                            "toNode", toNode,
                            "topologyType", "MESH"
                    ))
                    .retrieve()
                    .onStatus(
                            status -> status.is4xxClientError(),
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .flatMap(body -> Mono.error(new RuntimeException("Client error: " + body)))
                    )
                    .onStatus(
                            status -> status.is5xxServerError(),
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .flatMap(body -> Mono.error(new RuntimeException("Server error: " + body)))
                    )
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

            if (response != null && "success".equals(response.get("status"))) {
                @SuppressWarnings("unchecked")
                List<List<String>> pathsList = (List<List<String>>) response.get("paths");

                if (pathsList == null || pathsList.isEmpty()) {
                    return Collections.emptyList();
                }

                List<NetworkPath> networkPaths = new ArrayList<>();
                pathsList.sort(Comparator.comparingInt(List::size));

                for (int i = 0; i < pathsList.size(); i++) {
                    List<String> path = pathsList.get(i);
                    String pathType = i == 0 ? "PRIMARY" : "BACKUP";
                    NetworkPath networkPath = new NetworkPath(fromNode, toNode, path, pathType);
                    networkPaths.add(pathRepository.save(networkPath));
                }

                return networkPaths;
            }
        } catch (Exception e) {
            logger.error("Path calculation failed for {} to {}: {}", fromNode, toNode, e.getMessage());
        }
        return Collections.emptyList();
    }

    public void handleNodeFailure(List<String> failedNodes) {
        List<NetworkPath> activePaths = pathRepository.findByIsActive(true);

        for (NetworkPath path : activePaths) {
            boolean pathAffected = path.getPath().stream()
                    .anyMatch(failedNodes::contains);

            if (pathAffected) {
                path.setIsActive(false);
                pathRepository.save(path);
                calculatePaths(path.getFromNode(), path.getToNode());
            }
        }
    }

    public List<NetworkPath> getActivePaths(String fromNode, String toNode) {
        return pathRepository.findByFromNodeAndToNodeAndIsActive(fromNode, toNode, true);
    }

    public List<NetworkPath> getAllActivePaths() {
        return pathRepository.findByIsActive(true);
    }

    public NetworkPath getPrimaryPath(String fromNode, String toNode) {
        return getActivePaths(fromNode, toNode).stream()
                .filter(path -> "PRIMARY".equals(path.getPathType()))
                .findFirst()
                .orElse(null);
    }

    public List<NetworkPath> getBackupPaths(String fromNode, String toNode) {
        return getActivePaths(fromNode, toNode).stream()
                .filter(path -> "BACKUP".equals(path.getPathType()))
                .collect(Collectors.toList());
    }
}
