//package com.cath.topology_service.model;
//
//
//
//import org.springframework.data.annotation.Id;
//import org.springframework.data.mongodb.core.mapping.Document;
//
//import java.time.LocalDateTime;
//
//@Document(collection = "nodes")
//public class NetworkNode {
//    @Id
//    private String id;
//    private String nodeId;
//    private Integer latency;
//    private Boolean isActive;
//    private LocalDateTime lastSeen;
//    private String topologyType;
//
//    public NetworkNode() {}
//
//    public NetworkNode(String nodeId, Integer latency, Boolean isActive, String topologyType) {
//        this.nodeId = nodeId;
//        this.latency = latency;
//        this.isActive = isActive;
//        this.topologyType = topologyType;
//        this.lastSeen = LocalDateTime.now();
//    }
//
//    // Getters and Setters
//    public String getId() { return id; }
//    public void setId(String id) { this.id = id; }
//
//    public String getNodeId() { return nodeId; }
//    public void setNodeId(String nodeId) { this.nodeId = nodeId; }
//
//    public Integer getLatency() { return latency; }
//    public void setLatency(Integer latency) { this.latency = latency; }
//
//    public Boolean getIsActive() { return isActive; }
//    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
//
//    public LocalDateTime getLastSeen() { return lastSeen; }
//    public void setLastSeen(LocalDateTime lastSeen) { this.lastSeen = lastSeen; }
//
//    public String getTopologyType() { return topologyType; }
//    public void setTopologyType(String topologyType) { this.topologyType = topologyType; }
//}
package com.cath.topology_service.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "nodes")
public class NetworkNode {
    @Id
    private String nodeId; // Use nodeId as the MongoDB _id
    private Integer latency;
    private Boolean isActive;
    private LocalDateTime lastSeen;
    private String topologyType;

    public NetworkNode() {}

    public NetworkNode(String nodeId, Integer latency, Boolean isActive, String topologyType) {
        this.nodeId = nodeId;
        this.latency = latency;
        this.isActive = isActive;
        this.topologyType = topologyType;
        this.lastSeen = LocalDateTime.now();
    }

    // Getters and Setters
    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }

    public Integer getLatency() { return latency; }
    public void setLatency(Integer latency) { this.latency = latency; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public LocalDateTime getLastSeen() { return lastSeen; }
    public void setLastSeen(LocalDateTime lastSeen) { this.lastSeen = lastSeen; }

    public String getTopologyType() { return topologyType; }
    public void setTopologyType(String topologyType) { this.topologyType = topologyType; }
}