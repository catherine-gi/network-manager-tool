package com.cath.topology_service.repository;


import com.cath.topology_service.model.NetworkTopology;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NetworkTopologyRepository extends MongoRepository<NetworkTopology, String> {
    Optional<NetworkTopology> findByTopologyType(String topologyType);
    Optional<NetworkTopology> findByFromNodeAndToNode(String fromNode, String toNode);
    void deleteByTopologyType(String topologyType);
}
