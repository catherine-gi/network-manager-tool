package com.cath.path_service.repository;


import com.cath.path_service.model.NetworkPath;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NetworkPathRepository extends MongoRepository<NetworkPath, String> {
    List<NetworkPath> findByFromNodeAndToNode(String fromNode, String toNode);
    List<NetworkPath> findByFromNodeAndToNodeAndIsActive(String fromNode, String toNode, Boolean isActive);
    List<NetworkPath> findByIsActive(Boolean isActive);
    void deleteByFromNodeAndToNode(String fromNode, String toNode);
}
