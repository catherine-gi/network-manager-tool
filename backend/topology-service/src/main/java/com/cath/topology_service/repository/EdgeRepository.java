package com.cath.topology_service.repository;

import com.cath.topology_service.model.Edge;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface EdgeRepository extends MongoRepository<Edge, String> {
    List<Edge> findBySource(String source);
    List<Edge> findByTarget(String target);
    List<Edge> findByStatus(String status);
}