package com.cath.topology_service.repository;


import com.cath.topology_service.model.NetworkNode;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NetworkNodeRepository extends MongoRepository<NetworkNode, String> {
    Optional<NetworkNode> findByNodeId(String nodeId);
    List<NetworkNode> findByIsActive(Boolean isActive);
    List<NetworkNode> findByTopologyType(String topologyType);
}