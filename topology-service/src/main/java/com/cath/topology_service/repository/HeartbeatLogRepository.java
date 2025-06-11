package com.cath.topology_service.repository;

import com.cath.topology_service.model.HeartbeatLog;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface HeartbeatLogRepository extends MongoRepository<HeartbeatLog, String> {
}