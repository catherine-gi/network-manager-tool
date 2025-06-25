package com.cath.heartbeat_monitor.model;
import com.network.heartbeat.model.Alert;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface AlertRepository extends MongoRepository<Alert, String> {
    List<Alert> findByNotifiedFalse();
    List<Alert> findByNodeId(String nodeId);
}