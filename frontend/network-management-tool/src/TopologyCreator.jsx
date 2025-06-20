import React, { useState, useCallback, useRef,useEffect } from 'react';
import ReactFlow, {
  addEdge,
  Background,
  Controls,
  MiniMap,
  useNodesState,
  useEdgesState,
  MarkerType
} from 'reactflow';
import 'reactflow/dist/style.css';
import axios from 'axios';

const initialNodes = [];
const initialEdges = [];

const TopologyCreator = () => {
  const [nodes, setNodes, onNodesChange] = useNodesState(initialNodes);
  const [edges, setEdges, onEdgesChange] = useEdgesState(initialEdges);
  const [topologyType, setTopologyType] = useState('MESH');
  const [nodeName, setNodeName] = useState('');
  const [nodeCpu, setNodeCpu] = useState(4);
  const [nodeLatency, setNodeLatency] = useState(50);
  const [nodeStatus, setNodeStatus] = useState('active');
  const [nodeCount, setNodeCount] = useState(0);
  const [message, setMessage] = useState('');
  const [selectedNode, setSelectedNode] = useState(null);
  const [selectedEdge, setSelectedEdge] = useState(null);
  const [editPanelPosition, setEditPanelPosition] = useState({ x: 0, y: 0 });
  const [paths, setPaths] = useState([]);
  const [selectedPathIndex, setSelectedPathIndex] = useState(0);
  const [fromNode, setFromNode] = useState('');
  const [toNode, setToNode] = useState('');
  const [weights, setWeights] = useState({
    hops: 50,
    cpu: 25,
    latency: 25
  });
  const reactFlowWrapper = useRef(null);
    const isEdgeInPath = (edge, path) => {
    for (let i = 0; i < path.length - 1; i++) {
      if (
        (edge.source === path[i] && edge.target === path[i + 1]) ||
        (edge.source === path[i + 1] && edge.target === path[i])
      ) {
        return true;
      }
    }
    return false;
  };
  useEffect(() => {
    if (paths.length > 0) {
      const selectedPath = paths[selectedPathIndex];

      // Highlight nodes
      const updatedNodes = nodes.map((node) => ({
        ...node,
        style: {
          ...node.style,
          backgroundColor: selectedPath.includes(node.id) ? '#ffcc00' : '#fff',
          borderColor: selectedPath.includes(node.id) ? '#ff9900' : '#eee',
          borderWidth: selectedPath.includes(node.id) ? 2 : 1
        }
      }));

      // Highlight edges
      const updatedEdges = edges.map((edge) => {
        const isInPath = isEdgeInPath(edge, selectedPath);
        return {
          ...edge,
          animated: isInPath,
          style: {
            ...edge.style,
            stroke: isInPath ? '#ff9900' : edge.style?.stroke || '#b1b1b7',
            strokeWidth: isInPath ? 3 : edge.style?.strokeWidth || 2
          }
        };
      });

      setNodes(updatedNodes);
      setEdges(updatedEdges);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedPathIndex, paths]);


  // Weight distribution handler
  const handleWeightChange = (key, value) => {
    const newWeights = { ...weights };
    newWeights[key] = parseInt(value) || 0;
    
    // Ensure total is 100 by adjusting the other weights proportionally
    const total = newWeights.hops + newWeights.cpu + newWeights.latency;
    if (total !== 100) {
      const remaining = 100 - newWeights[key];
      const otherKeys = Object.keys(newWeights).filter(k => k !== key);
      const sumOther = otherKeys.reduce((sum, k) => sum + newWeights[k], 0);
      
      otherKeys.forEach(k => {
        newWeights[k] = Math.round((newWeights[k] / sumOther) * remaining);
      });
      
      // Fix any rounding errors
      const finalTotal = Object.values(newWeights).reduce((sum, val) => sum + val, 0);
      if (finalTotal !== 100) {
        newWeights[otherKeys[0]] += 100 - finalTotal;
      }
    }
    
    setWeights(newWeights);
  };

  // Path calculation function
  const calculateAndDisplayPaths = async () => {
  if (!fromNode || !toNode) {
    setMessage('Please select both source and destination nodes');
    return;
  }

  try {
    const response = await axios.post(
      `http://localhost:8083/api/paths/calculate`,
      {
        fromNode,
        toNode,
        weights: {
          hops: weights.hops / 100,
          cpu: weights.cpu / 100,
          latency: weights.latency / 100
        }
      }
    );
    setPaths(response.data.paths);
    setSelectedPathIndex(0);
    setMessage(`Found ${response.data.pathCount} paths from ${fromNode} to ${toNode}`);
  } catch (error) {
    setMessage(`Error calculating paths: ${error.response?.data?.message || error.message}`);
  }
};

  // Existing topology functions
  const createTopology = async () => {
    try {
      const topologyData = {
        nodes: nodes.map(node => ({
          id: node.id,
          position: node.position,
          cpu: node.data?.cpu || 4,
          latency: node.data?.latency || 50,
          status: node.data?.status || 'active'
        })),
        edges: edges.map(edge => ({
          id: edge.id,
          source: edge.source,
          target: edge.target,
          status: edge.data?.status || 'active'
        })),
      };

      await axios.post('http://localhost:8081/api/network/initialize', topologyData);
      setMessage('Topology created successfully!');
    } catch (error) {
      setMessage(`Error: ${error.response?.data?.message || error.message}`);
    }
  };

  const deleteNode = async (nodeId) => {
    try {
      await axios.post('http://localhost:8081/api/network/delete-node', { nodeId });
      setNodes((nds) => nds.filter((node) => node.id !== nodeId));
      setEdges((eds) => eds.filter((edge) => edge.source !== nodeId && edge.target !== nodeId));
      setMessage(`Node ${nodeId} deleted successfully`);
      await createTopology();
    } catch (error) {
      setMessage(`Error deleting node: ${error.response?.data?.message || error.message}`);
    }
  };

  const deleteEdge = async (edgeId) => {
    try {
      await axios.post('http://localhost:8081/api/network/delete-edge', { edgeId });
      setEdges((eds) => eds.filter((edge) => edge.id !== edgeId));
      setMessage(`Edge deleted successfully`);
      await createTopology();
    } catch (error) {
      setMessage(`Error deleting edge: ${error.response?.data?.message || error.message}`);
    }
  };

  const onConnect = useCallback(
    (params) => {
      setEdges((eds) => addEdge(
        { 
          ...params, 
          markerEnd: { type: MarkerType.ArrowClosed },
          type: 'smoothstep',
          data: { status: 'active' },
          style: { stroke: '#b1b1b7', strokeWidth: 2 }
        }, 
        eds
      ));
      createTopology();
    },
    [setEdges]
  );

  const onNodeClick = useCallback((event, node) => {
    setSelectedNode(node);
    setSelectedEdge(null);
    const wrapperBounds = reactFlowWrapper.current.getBoundingClientRect();
    setEditPanelPosition({
      x: node.position.x + wrapperBounds.left + 20,
      y: node.position.y + wrapperBounds.top
    });
  }, []);

  const onEdgeClick = useCallback((event, edge) => {
    event.stopPropagation();
    setSelectedEdge(edge);
    setSelectedNode(null);
    const wrapperBounds = reactFlowWrapper.current.getBoundingClientRect();
    setEditPanelPosition({
      x: wrapperBounds.left + 400,
      y: wrapperBounds.top + 200
    });
  }, []);

  const onPaneClick = useCallback(() => {
    setSelectedNode(null);
    setSelectedEdge(null);
  }, []);

  const updateNodeProperties = useCallback((nodeId, newProperties) => {
    setNodes((nds) => nds.map((node) => {
      if (node.id === nodeId) {
        const updatedProperties = {
          cpu: newProperties.cpu !== undefined ? newProperties.cpu : node.data.cpu,
          latency: newProperties.latency !== undefined ? newProperties.latency : node.data.latency,
          status: newProperties.status || node.data.status
        };

        axios.post('http://localhost:8081/api/network/update-node', {
          nodeId: nodeId,
          cpu: updatedProperties.cpu,
          latency: updatedProperties.latency,
          status: updatedProperties.status
        })
        .then(() => {
          setMessage(`Node ${nodeId} updated successfully`);
        })
        .catch(error => {
          setMessage(`Error updating node: ${error.response?.data?.message || error.message}`);
        });

        return {
          ...node,
          data: {
            ...node.data,
            ...updatedProperties,
            label: `${node.id} (CPU: ${updatedProperties.cpu}, Latency: ${updatedProperties.latency})`
          }
        };
      }
      return node;
    }));
  }, [setNodes]);

  const updateEdgeProperties = useCallback((edgeId, newProperties) => {
    setEdges((eds) => eds.map((edge) => {
      if (edge.id === edgeId) {
        const updatedStatus = newProperties.status || edge.data?.status || 'active';
        
        axios.post('http://localhost:8081/api/network/update-edge', {
          edgeId: edgeId,
          source: edge.source,
          target: edge.target,
          status: updatedStatus
        })
        .then(() => {
          setMessage(`Edge ${edge.source} -> ${edge.target} updated successfully`);
        })
        .catch(error => {
          setMessage(`Error updating edge: ${error.response?.data?.message || error.message}`);
        });

        return {
          ...edge,
          data: {
            ...edge.data,
            status: updatedStatus
          },
          style: {
            ...edge.style,
            stroke: updatedStatus === 'failed' ? '#ff4444' : '#b1b1b7',
            strokeWidth: updatedStatus === 'failed' ? 3 : 2,
            strokeDasharray: updatedStatus === 'failed' ? '5,5' : 'none'
          }
        };
      }
      return edge;
    }));
  }, [setEdges]);

  const addNode = () => {
    if (!nodeName) return;
    
    const newNode = {
      id: nodeName,
      position: { x: Math.random() * 500, y: Math.random() * 500 },
      data: { 
        label: `${nodeName} (CPU: ${nodeCpu}, Latency: ${nodeLatency})`,
        cpu: nodeCpu,
        latency: nodeLatency,
        status: nodeStatus
      },
      type: 'default'
    };

    setNodes((nds) => [...nds, newNode]);
    setNodeCount(nodeCount + 1);
    setNodeName('');
    setNodeCpu(4);
    setNodeLatency(50);
    setNodeStatus('active');
    createTopology();
  };

  const simulateNodeFailure = async (nodeId) => {
    try {
      await axios.post('http://localhost:8081/api/network/down', { nodeId });
      updateNodeProperties(nodeId, { status: 'failed' });
      setMessage(`Node ${nodeId} marked as failed`);
    } catch (error) {
      setMessage(`Error: ${error.response?.data?.message || error.message}`);
    }
  };

  const restoreNode = async (nodeId) => {
    try {
      await axios.post('http://localhost:8081/api/network/restore', { nodeId });
      updateNodeProperties(nodeId, { status: 'active' });
      setMessage(`Node ${nodeId} restored`);
    } catch (error) {
      setMessage(`Error: ${error.response?.data?.message || error.message}`);
    }
  };

  const simulateEdgeFailure = async (edgeId, source, target) => {
    try {
      await axios.post('http://localhost:8081/api/network/edge-down', { 
        edgeId, 
        source, 
        target 
      });
      updateEdgeProperties(edgeId, { status: 'failed' });
      setMessage(`Edge ${source} -> ${target} marked as failed`);
    } catch (error) {
      setMessage(`Error: ${error.response?.data?.message || error.message}`);
    }
  };

  const restoreEdge = async (edgeId, source, target) => {
    try {
      await axios.post('http://localhost:8081/api/network/edge-restore', { 
        edgeId, 
        source, 
        target 
      });
      updateEdgeProperties(edgeId, { status: 'active' });
      setMessage(`Edge ${source} -> ${target} restored`);
    } catch (error) {
      setMessage(`Error: ${error.response?.data?.message || error.message}`);
    }
  };

  const stopSimulation = async () => {
    try {
      await axios.post('http://localhost:8081/api/network/stop');
      setMessage('Simulation stopped');
    } catch (error) {
      setMessage(`Error: ${error.response?.data?.message || error.message}`);
    }
  };
  

  return (
    <div style={{ height: '100vh', width: '100vw' }} ref={reactFlowWrapper}>
      <div style={{ 
        position: 'absolute', 
        zIndex: 10, 
        padding: '15px', 
        background: 'white',
        borderRadius: '8px',
        boxShadow: '0 2px 10px rgba(0,0,0,0.1)',
        margin: '10px',
        width: '300px'
      }}>
        <div style={{ marginBottom: '15px' }}>
          <h3 style={{ marginBottom: '10px', color: '#333' }}>Network Topology Builder</h3>
          
          <div style={{ marginBottom: '10px',margin: '10px' }}>
            <label style={{ display: 'block', marginBottom: '5px', fontWeight: '500' }}>Node ID:</label>
            <input 
              value={nodeName}
              onChange={(e) => setNodeName(e.target.value)}
              placeholder="Enter node ID"
              style={{
                width: '100%',
                padding: '8px',
                borderRadius: '4px',
                border: '1px solid #ddd'
              }}
            />
          </div>

          <div style={{ display: 'flex', gap: '10px', marginBottom: '10px', margin: '10px' }}>
            <div style={{ flex: 1 ,margin: '0 10px 0 0'}}>
              <label style={{ display: 'block', marginBottom: '5px', fontWeight: '500', color:'grey' }}>CPU</label>
              <input 
                type="number"
                value={nodeCpu}
                onChange={(e) => setNodeCpu(parseInt(e.target.value) || 4)}
                min="1"
                max="64"
                style={{
                  width: '100%',
                  padding: '8px',
                  borderRadius: '4px',
                  border: '1px solid #ddd'
                }}
              />
            </div>
            <div style={{ flex: 1 ,margin: '0 0 0 10px'}}>
              <label style={{ display: 'block', marginBottom: '5px', fontWeight: '500',color:'grey' }}>Latency:</label>
              <input 
                type="number"
                value={nodeLatency}
                onChange={(e) => setNodeLatency(parseInt(e.target.value) || 50)}
                min="1"
                max="1000"
                style={{
                  width: '100%',
                  padding: '8px',
                  borderRadius: '4px',
                  border: '1px solid #ddd'
                }}
              />
            </div>
          </div>

          {/* <div style={{ marginBottom: '15px' }}>
            <label style={{ display: 'block', marginBottom: '5px', fontWeight: '500' }}>Status:</label>
            <select 
              value={nodeStatus}
              onChange={(e) => setNodeStatus(e.target.value)}
              style={{
                width: '100%',
                padding: '8px',
                borderRadius: '4px',
                border: '1px solid #ddd',
                backgroundColor: 'white'
              }}
            >
              <option value="active">Active</option>
              <option value="standby">Standby</option>
              <option value="maintenance">Maintenance</option>
              <option value="failed">Failed</option>
            </select>
          </div> */}

          <button 
            onClick={addNode}
            style={{
              width: '100%',
              padding: '10px',
              backgroundColor: '#2f302f',
              color: 'white',
              border: 'none',
              borderRadius: '4px',
              cursor: 'pointer',
              fontWeight: 'bold',
              marginBottom: '15px'
            }}
          >
            Add Node
          </button>
        </div>

        <div style={{ marginBottom: '15px' }}>
          <label style={{ display: 'block', marginBottom: '5px', fontWeight: '500' }}>Topology Type:</label>
          {/* <select 
            value={topologyType}
            onChange={(e) => setTopologyType(e.target.value)}
            style={{
              width: '100%',
              padding: '8px',
              borderRadius: '4px',
              border: '1px solid #ddd',
              backgroundColor: 'white',
              marginBottom: '15px'
            }}
          >
            <option value="MESH">Mesh</option>
            <option value="STAR">Star</option>
            <option value="RING">Ring</option>
            <option value="BUS">Bus</option>
          </select> */}

          <div style={{ display: 'flex', gap: '10px', marginBottom: '10px' }}>
            <button 
              onClick={createTopology}
              style={{
                flex: 1,
                padding: '10px',
                backgroundColor: '#2f302f',
                color: 'white',
                border: 'none',
                borderRadius: '4px',
                cursor: 'pointer',
                fontWeight: 'bold'
              }}
            >
              Create
            </button>
          </div>

          {/* <button 
            onClick={stopSimulation}
            style={{
              width: '100%',
              padding: '10px',
              backgroundColor: '#f44336',
              color: 'white',
              border: 'none',
              borderRadius: '4px',
              cursor: 'pointer',
              fontWeight: 'bold',
              marginBottom: '15px'
            }}
          >
            Stop Simulation
          </button> */}
        </div>

        {/* Path Calculation Section */}
        <div style={{ marginTop: '15px', padding: '10px', borderTop: '1px solid #eee' }}>
          <h4 style={{ marginBottom: '10px', color: '#333' }}>Path Calculation</h4>
          
          <div style={{ marginBottom: '10px' }}>
            <label style={{ display: 'block', marginBottom: '5px', fontWeight: '500' }}>From Node:</label>
            <input 
              value={fromNode}
              onChange={(e) => setFromNode(e.target.value)}
              placeholder="Source node ID"
              style={{
                width: '100%',
                padding: '8px',
                borderRadius: '4px',
                border: '1px solid #ddd'
              }}
            />
          </div>

          <div style={{ marginBottom: '10px' }}>
            <label style={{ display: 'block', marginBottom: '5px', fontWeight: '500' }}>To Node:</label>
            <input 
              value={toNode}
              onChange={(e) => setToNode(e.target.value)}
              placeholder="Destination node ID"
              style={{
                width: '100%',
                padding: '8px',
                borderRadius: '4px',
                border: '1px solid #ddd'
              }}
            />
          </div>

          {/* Weight Distribution Controls */}
          <div style={{ marginBottom: '15px' }}>
            <h5 style={{ marginBottom: '8px', color: '#555' }}>Weight Distribution</h5>
            
            <div style={{ marginBottom: '8px' }}>
              <label style={{ display: 'block', marginBottom: '3px' }}>Hops: {weights.hops}%</label>
              <input
                type="range"
                min="0"
                max="100"
                value={weights.hops}
                onChange={(e) => handleWeightChange('hops', e.target.value)}
                style={{ width: '100%' }}
              />
            </div>
            
            <div style={{ marginBottom: '8px' }}>
              <label style={{ display: 'block', marginBottom: '3px' }}>CPU: {weights.cpu}%</label>
              <input
                type="range"
                min="0"
                max="100"
                value={weights.cpu}
                onChange={(e) => handleWeightChange('cpu', e.target.value)}
                style={{ width: '100%' }}
              />
            </div>
            
            <div style={{ marginBottom: '8px' }}>
              <label style={{ display: 'block', marginBottom: '3px' }}>Latency: {weights.latency}%</label>
              <input
                type="range"
                min="0"
                max="100"
                value={weights.latency}
                onChange={(e) => handleWeightChange('latency', e.target.value)}
                style={{ width: '100%' }}
              />
            </div>
            
            <div style={{ 
              display: 'flex', 
              justifyContent: 'space-between',
              fontSize: '12px',
              color: '#666',
              marginBottom: '10px'
            }}>
              <span>Total: {weights.hops + weights.cpu + weights.latency}%</span>
              <button 
                onClick={() => setWeights({ hops: 50, cpu: 25, latency: 25 })}
                style={{
                  padding: '2px 8px',
                  backgroundColor: '#2f302f',
                  border: '1px solid #ddd',
                  borderRadius: '4px',
                  cursor: 'pointer'
                }}
              >
                Reset Defaults
              </button>
            </div>
          </div>

          <button 
            onClick={calculateAndDisplayPaths}
            style={{
              width: '100%',
              padding: '10px',
              backgroundColor: '#2f302f',
              color: 'white',
              border: 'none',
              borderRadius: '4px',
              cursor: 'pointer',
              fontWeight: 'bold',
              marginBottom: '10px'
            }}
          >
            Calculate Paths
          </button>

          {paths.length > 0 && (
            <div style={{ marginTop: '10px' }}>
              <h5 style={{ marginBottom: '5px', color: '#555' }}>Found Paths:</h5>
              <select
                value={selectedPathIndex}
                onChange={(e) => setSelectedPathIndex(parseInt(e.target.value))}
                style={{
                  width: '100%',
                  padding: '8px',
                  borderRadius: '4px',
                  border: '1px solid #ddd',
                  marginBottom: '10px'
                }}
              >
                {paths.map((path, index) => (
                  <option key={index} value={index}>
                    Path {index + 1}: {path.join(' → ')}
                  </option>
                ))}
              </select>
            </div>
          )}
        </div>

        {message && (
          <div style={{
            padding: '10px',
            borderRadius: '4px',
            backgroundColor: message.startsWith('Error') ? '#ffebee' : '#e8f5e9',
            color: message.startsWith('Error') ? '#c62828' : '#2e7d32',
            marginBottom: '15px'
          }}>
            {message}
          </div>
        )}

        <div style={{
          display: 'flex',
          justifyContent: 'space-between',
          color: '#666',
          fontSize: '14px'
        }}>
          <span>Nodes: {nodeCount}</span>
          <span>Edges: {edges.length}</span>
        </div>
      </div>
      
      <ReactFlow
        nodes={nodes}
        edges={edges}
        onNodesChange={onNodesChange}
        onEdgesChange={onEdgesChange}
        onConnect={onConnect}
        onNodeClick={onNodeClick}
        onEdgeClick={onEdgeClick}
        onPaneClick={onPaneClick}
        fitView
      >
        <Background />
        <Controls />
        <MiniMap />
      </ReactFlow>

      {selectedNode && (
        <div style={{
          position: 'absolute',
          left: editPanelPosition.x,
          top: editPanelPosition.y,
          zIndex: 10,
          background: 'white',
          padding: '15px',
          border: '1px solid #ddd',
          borderRadius: '8px',
          boxShadow: '0 2px 10px rgba(0,0,0,0.1)',
          width: '250px'
        }}>
          <div style={{ 
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            marginBottom: '10px'
          }}>
            <h4 style={{ margin: 0 }}>Edit Node: {selectedNode.id}</h4>
            <button 
              onClick={() => setSelectedNode(null)}
              style={{
                background: 'none',
                border: 'none',
                cursor: 'pointer',
                fontSize: '16px',
                color: '#666'
              }}
            >
              ×
            </button>
          </div>

          <div style={{ marginBottom: '10px' }}>
            <label style={{ display: 'block', marginBottom: '5px', fontWeight: '500' }}>CPU:</label>
            <input 
              type="number"
              value={selectedNode.data.cpu}
              onChange={(e) => updateNodeProperties(selectedNode.id, { cpu: parseInt(e.target.value) })}
              min="1"
              max="64"
              style={{
                width: '100%',
                padding: '8px',
                borderRadius: '4px',
                border: '1px solid #ddd'
              }}
            />
          </div>

          <div style={{ marginBottom: '10px' }}>
            <label style={{ display: 'block', marginBottom: '5px', fontWeight: '500' }}>Latency:</label>
            <input 
              type="number"
              value={selectedNode.data.latency}
              onChange={(e) => updateNodeProperties(selectedNode.id, { latency: parseInt(e.target.value) })}
              min="1"
              max="1000"
              style={{
                width: '100%',
                padding: '8px',
                borderRadius: '4px',
                border: '1px solid #ddd'
              }}
            />
          </div>

          <div style={{ marginBottom: '15px' }}>
            <label style={{ display: 'block', marginBottom: '5px', fontWeight: '500' }}>Status:</label>
            <select 
              value={selectedNode.data.status}
              onChange={(e) => updateNodeProperties(selectedNode.id, { status: e.target.value })}
              style={{
                width: '100%',
                padding: '8px',
                borderRadius: '4px',
                border: '1px solid #ddd',
                backgroundColor: 'white'
              }}
            >
              <option value="active">Active</option>
              <option value="standby">Standby</option>
              <option value="maintenance">Maintenance</option>
              <option value="failed">Failed</option>
            </select>
          </div>

          <div style={{ display: 'flex', gap: '10px', marginBottom: '10px' }}>
            <button 
              onClick={() => simulateNodeFailure(selectedNode.id)}
              style={{
                flex: 1,
                padding: '8px',
                backgroundColor: '#f44336',
                color: 'white',
                border: 'none',
                borderRadius: '4px',
                cursor: 'pointer'
              }}
            >
              Fail Node
            </button>
            <button 
              onClick={() => restoreNode(selectedNode.id)}
              style={{
                flex: 1,
                padding: '8px',
                backgroundColor: '#4CAF50',
                color: 'white',
                border: 'none',
                borderRadius: '4px',
                cursor: 'pointer'
              }}
            >
              Restore
            </button>
          </div>

          <button
            onClick={() => { deleteNode(selectedNode.id); setSelectedNode(null); }}
            style={{
              width: '100%',
              padding: '8px',
              backgroundColor: '#e53935',
              color: 'white',
              border: 'none',
              borderRadius: '4px',
              cursor: 'pointer',
              marginTop: '10px'
            }}
          >
            Delete Node
          </button>
        </div>
      )}

      {selectedEdge && (
        <div style={{
          position: 'absolute',
          left: editPanelPosition.x,
          top: editPanelPosition.y,
          zIndex: 10,
          background: 'white',
          padding: '15px',
          border: '1px solid #ddd',
          borderRadius: '8px',
          boxShadow: '0 2px 10px rgba(0,0,0,0.1)',
          width: '250px'
        }}>
          <div style={{ 
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            marginBottom: '10px'
          }}>
            <h4 style={{ margin: 0 }}>Edit Edge</h4>
            <button 
              onClick={() => setSelectedEdge(null)}
              style={{
                background: 'none',
                border: 'none',
                cursor: 'pointer',
                fontSize: '16px',
                color: '#666'
              }}
            >
              ×
            </button>
          </div>

          <div style={{ marginBottom: '15px' }}>
            <p style={{ margin: '5px 0', color: '#666' }}>
              <strong>Connection:</strong> {selectedEdge.source} → {selectedEdge.target}
            </p>
            <p style={{ margin: '5px 0', color: '#666' }}>
              <strong>Status:</strong> 
              <span style={{
                color: selectedEdge.data?.status === 'failed' ? '#f44336' : '#4CAF50',
                fontWeight: 'bold',
                marginLeft: '5px'
              }}>
                {selectedEdge.data?.status || 'active'}
              </span>
            </p>
          </div>

          <div style={{ display: 'flex', gap: '10px', marginBottom: '10px' }}>
            <button 
              onClick={() => simulateEdgeFailure(selectedEdge.id, selectedEdge.source, selectedEdge.target)}
              style={{
                flex: 1,
                padding: '8px',
                backgroundColor: '#f44336',
                color: 'white',
                border: 'none',
                borderRadius: '4px',
                cursor: 'pointer'
              }}
            >
              Fail Edge
            </button>
            <button 
              onClick={() => restoreEdge(selectedEdge.id, selectedEdge.source, selectedEdge.target)}
              style={{
                flex: 1,
                padding: '8px',
                backgroundColor: '#4CAF50',
                color: 'white',
                border: 'none',
                borderRadius: '4px',
                cursor: 'pointer'
              }}
            >
              Restore Edge
            </button>
          </div>

          <button
            onClick={() => { deleteEdge(selectedEdge.id); setSelectedEdge(null); }}
            style={{
              width: '100%',
              padding: '8px',
              backgroundColor: '#e53935',
              color: 'white',
              border: 'none',
              borderRadius: '4px',
              cursor: 'pointer',
              marginTop: '10px'
            }}
          >
            Delete Edge
          </button>
        </div>
      )}
    </div>
  );
};

export default TopologyCreator;