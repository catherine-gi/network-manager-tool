import React, { useState, useCallback, useRef, useEffect } from 'react';
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

// Path colors for visualization
const PATH_COLORS = [
  '#FF6B6B', // Red
  '#4ECDC4', // Teal
  '#45B7D1', // Blue
  '#FFBE0B', // Yellow
  '#FB5607', // Orange
  '#8338EC', // Purple
  '#3A86FF', // Bright Blue
  '#FF006E', // Pink
  '#A5DD9B', // Light Green
  '#F9C74F'  // Gold
];

const initialNodes = [];
const initialEdges = [];

const colors = {
  lightBg: '#f9fafb',
  paneBg: '#ffffff',
  border: '#ddd',
  textPrimary: '#222',
  textSecondary: '#555',
  textLight: '#777',
  buttonBg: '#222',
  buttonHoverBg: '#444',
  inputBorder: '#ccc',
  inputBg: '#fff',
  successBg: '#edf7ed',
  successText: '#276749',
  errorBg: '#fdecea',
  errorText: '#b91c1c',
  navBg: '#f3f4f6',
  navText: '#444',
  navTextActive: '#222',
  navBorderBottom: '#ddd',
  tagBgNodes: '#e2e8f0',
  tagBgEdges: '#d1fae5',
  tagTextNodes: '#475569',
  tagTextEdges: '#276749'
};

const inputStyle = {
  width: '100%',
  padding: '8px 10px',
  borderRadius: 6,
  border: `1.5px solid ${colors.inputBorder}`,
  fontSize: 14,
  boxSizing: 'border-box',
  backgroundColor: colors.inputBg,
  color: colors.textPrimary,
  outlineOffset: '2px',
  outlineColor: 'transparent',
  transition: 'outline-color 0.15s ease-in-out',
  fontFamily: 'Inter, sans-serif'
};

const inputFocusStyle = {
  outlineColor: colors.buttonBg
};

const labelStyle = {
  marginBottom: 6,
  fontWeight: 600,
  color: colors.textPrimary,
  fontSize: 14,
  fontFamily: 'Inter, sans-serif'
};

const buttonPrimary = {
  width: '100%',
  padding: '11px 0',
  backgroundColor: colors.buttonBg,
  color: '#fff',
  border: 'none',
  borderRadius: 8,
  cursor: 'pointer',
  fontWeight: '700',
  fontSize: '15px',
  fontFamily: 'Inter, sans-serif',
  userSelect: 'none',
  transition: 'background-color 0.2s ease'
};

const buttonPrimaryHover = {
  backgroundColor: colors.buttonHoverBg
};

const paneStyle = {
  position: 'absolute',
  top: 0,
  left: 0,
  zIndex: 10,
  background: colors.paneBg,
  padding: '20px 25px',
  border: `1px solid ${colors.border}`,
  borderRadius: 0,
  boxShadow: '0 6px 15px rgb(0 0 0 / 0.07)',
  width: 350,
  fontFamily: 'Inter, sans-serif',
  color: colors.textPrimary,
  height: '100%',
  overflowY: 'auto'
};

const messageBoxStyle = (isError) => ({
  padding: '12px',
  borderRadius: 8,
  backgroundColor: isError ? colors.errorBg : colors.successBg,
  color: isError ? colors.errorText : colors.successText,
  fontSize: 14,
  marginBottom: 16,
  fontWeight: 600,
  fontFamily: 'Inter, sans-serif'
});

const tagStyle = (bg, color) => ({
  display: 'inline-block',
  padding: '1.5px 8px',
  fontSize: 12,
  fontWeight: 600,
  borderRadius: 9999,
  color,
  backgroundColor: bg,
  userSelect: 'none',
  marginRight: 6,
  fontFamily: 'Inter, sans-serif'
});

const navStyle = {
  width: '100%',
  padding: '10px 25px',
  backgroundColor: colors.navBg,
  borderBottom: `1px solid ${colors.navBorderBottom}`,
  display: 'flex',
  gap: 30,
  fontWeight: 600,
  fontSize: 15,
  color: colors.navText,
  fontFamily: 'Inter, sans-serif',
  userSelect: 'none',
  boxSizing: 'border-box',
  position: 'sticky',
  top: 0,
  zIndex: 1000
};

const navItemStyle = (active) => ({
  cursor: 'pointer',
  paddingBottom: 8,
  borderBottom: active ? `3px solid ${colors.buttonBg}` : '3px solid transparent',
  color: active ? colors.navTextActive : colors.navText,
  transition: 'color 0.2s ease, border-color 0.2s ease',
  whiteSpace: 'nowrap'
});

const TopologyCreator = () => {
  const [nodes, setNodes, onNodesChange] = useNodesState(initialNodes);
  const [edges, setEdges, onEdgesChange] = useEdgesState(initialEdges);
  const [nodeName, setNodeName] = useState('');
  const [nodeCpu, setNodeCpu] = useState(4);
  const [nodeLatency, setNodeLatency] = useState(50);
  const [nodeStatus, setNodeStatus] = useState('active');
  const [message, setMessage] = useState('');
  const [selectedNode, setSelectedNode] = useState(null);
  const [selectedEdge, setSelectedEdge] = useState(null);
  const [editPanelPosition, setEditPanelPosition] = useState({ x: 0, y: 0 });
  const [paths, setPaths] = useState([]);
  const [selectedPathIndex, setSelectedPathIndex] = useState(0);
  const [weights, setWeights] = useState({
    hops: 50,
    cpu: 25,
    latency: 25
  });
  const [currentNav, setCurrentNav] = useState('create_topology');
  const reactFlowWrapper = useRef(null);
  
  // State for multiple path requests and visualization
  const [pathRequests, setPathRequests] = useState([{ from: '', to: '' }]);
  const [batchResults, setBatchResults] = useState({});
  const [showMultiplePaths, setShowMultiplePaths] = useState(false);
  const [selectedPaths, setSelectedPaths] = useState([]);

  // Viewport dimensions
  const [viewportDimensions, setViewportDimensions] = useState({
    width: window.innerWidth,
    height: window.innerHeight
  });

  // Update viewport dimensions on resize
  useEffect(() => {
    const handleResize = () => {
      setViewportDimensions({
        width: window.innerWidth,
        height: window.innerHeight
      });
    };

    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

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
      if (showMultiplePaths && selectedPaths.length > 0) {
        // Handle multiple path visualization
        const updatedNodes = nodes.map((node) => {
          let backgroundColor = colors.paneBg;
          let borderColor = colors.border;
          let borderWidth = 1;
          let fontWeight = 'normal';
          
          // Find if node is in any selected path
          selectedPaths.forEach((path, pathIndex) => {
            if (path.includes(node.id)) {
              backgroundColor = PATH_COLORS[pathIndex % PATH_COLORS.length] + '33'; // Add transparency
              borderColor = PATH_COLORS[pathIndex % PATH_COLORS.length];
              borderWidth = 2;
              fontWeight = '600';
            }
          });
          
          return {
            ...node,
            style: {
              ...node.style,
              backgroundColor,
              borderColor,
              borderWidth,
              fontWeight,
              color: colors.textPrimary
            }
          };
        });

        const updatedEdges = edges.map((edge) => {
          let stroke = '#bbb';
          let strokeWidth = 2;
          let animated = false;
          
          // Check if edge is in any selected path
          selectedPaths.forEach((path, pathIndex) => {
            if (isEdgeInPath(edge, path)) {
              stroke = PATH_COLORS[pathIndex % PATH_COLORS.length];
              strokeWidth = 3;
              animated = true;
            }
          });
          
          return {
            ...edge,
            animated,
            style: {
              ...edge.style,
              stroke,
              strokeWidth
            }
          };
        });

        setNodes(updatedNodes);
        setEdges(updatedEdges);
      } else {
        // Handle single path visualization
        const selectedPath = paths[selectedPathIndex];
        const updatedNodes = nodes.map((node) => ({
          ...node,
          style: {
            ...node.style,
            backgroundColor: selectedPath.includes(node.id) ? '#ffd54f' : colors.paneBg,
            borderColor: selectedPath.includes(node.id) ? '#ffb300' : colors.border,
            borderWidth: selectedPath.includes(node.id) ? 2 : 1,
            fontWeight: selectedPath.includes(node.id) ? '600' : 'normal',
            color: colors.textPrimary
          }
        }));

        const updatedEdges = edges.map((edge) => {
          const isInPath = isEdgeInPath(edge, selectedPath);
          return {
            ...edge,
            animated: isInPath,
            style: {
              ...edge.style,
              stroke: isInPath ? '#ffb300' : edge.style?.stroke || '#bbb',
              strokeWidth: isInPath ? 3 : edge.style?.strokeWidth || 2
            }
          };
        });

        setNodes(updatedNodes);
        setEdges(updatedEdges);
      }
    }
  }, [selectedPathIndex, paths, showMultiplePaths, selectedPaths]);

  const adjustPanelPosition = (position) => {
    const panelWidth = 300;
    const panelHeight = 500; // Approximate height
    const margin = 20;

    let adjustedX = position.x;
    let adjustedY = position.y;

    // Adjust X position if panel would go off right side
    if (position.x + panelWidth > viewportDimensions.width) {
      adjustedX = viewportDimensions.width - panelWidth - margin;
    }

    // Adjust Y position if panel would go off bottom
    if (position.y + panelHeight > viewportDimensions.height) {
      adjustedY = viewportDimensions.height - panelHeight - margin;
    }

    return { x: Math.max(margin, adjustedX), y: Math.max(margin, adjustedY) };
  };

  const handleWeightChange = (key, value) => {
    const newWeights = { ...weights };
    newWeights[key] = parseInt(value) || 0;

    const total = newWeights.hops + newWeights.cpu + newWeights.latency;
    if (total !== 100) {
      const remaining = 100 - newWeights[key];
      const otherKeys = Object.keys(newWeights).filter(k => k !== key);
      const sumOther = otherKeys.reduce((sum, k) => sum + newWeights[k], 0);

      otherKeys.forEach(k => {
        newWeights[k] = Math.round((newWeights[k] / sumOther) * remaining);
      });

      const finalTotal = Object.values(newWeights).reduce((sum, val) => sum + val, 0);
      if (finalTotal !== 100) {
        newWeights[otherKeys[0]] += 100 - finalTotal;
      }
    }

    setWeights(newWeights);
  };

  const addPathRequest = () => {
    setPathRequests([...pathRequests, { from: '', to: '' }]);
  };

  const removePathRequest = (index) => {
    if (pathRequests.length <= 1) return;
    const updated = [...pathRequests];
    updated.splice(index, 1);
    setPathRequests(updated);
  };

  const updatePathRequest = (index, field, value) => {
    const updated = [...pathRequests];
    updated[index][field] = value;
    setPathRequests(updated);
  };

  const calculateAndDisplayPaths = async () => {
    const invalidRequests = pathRequests.filter(req => !req.from.trim() || !req.to.trim());
    if (invalidRequests.length > 0) {
      setMessage('Please enter both source and destination nodes for all paths');
      return;
    }

    try {
      const requestsMap = {};
      pathRequests.forEach(req => {
        if (!requestsMap[req.from]) {
          requestsMap[req.from] = [];
        }
        requestsMap[req.from].push(req.to);
      });

      const response = await axios.post(
        `http://localhost:8083/api/paths/calculate-multiple`,
        {
          pathRequests: requestsMap,
          weights: {
            hops: weights.hops / 100,
            cpu: weights.cpu / 100,
            latency: weights.latency / 100
          }
        }
      );
      
      setBatchResults(response.data);
      setMessage(`Calculated paths for ${Object.keys(response.data).length} node pairs`);
      
      // Reset multiple paths display when new paths are calculated
      setShowMultiplePaths(false);
      setSelectedPaths([]);
    } catch (error) {
      setMessage(`Error calculating paths: ${error.response?.data?.message || error.message}`);
    }
  };

  const togglePathSelection = (pathKey, pathIndex) => {
    const path = batchResults[pathKey][pathIndex].path;
    if (showMultiplePaths) {
      setSelectedPaths(prev => {
        const pathStr = JSON.stringify(path);
        const isSelected = prev.some(p => JSON.stringify(p) === pathStr);
        return isSelected 
          ? prev.filter(p => JSON.stringify(p) !== pathStr)
          : [...prev, path];
      });
    } else {
      setPaths([path]);
      setSelectedPathIndex(0);
    }
  };

  const createTopology = async () => {
    try {
      const topologyData = {
        nodes: nodes.map(node => ({
          id: node.id,
          position: node.position,
          cpu: node.data?.cpu ?? 4,
          latency: node.data?.latency ?? 50,
          status: node.data?.status ?? 'active'
        })),
        edges: edges.map(edge => ({
          id: edge.id,
          source: edge.source,
          target: edge.target,
          status: edge.data?.status ?? 'active'
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
      setEdges((eds) =>
        addEdge(
          {
            ...params,
            markerEnd: { type: MarkerType.ArrowClosed },
            type: 'smoothstep',
            data: { status: 'active' },
            style: { stroke: '#bbb', strokeWidth: 2 }
          },
          eds
        )
      );
      createTopology();
    },
    [setEdges]
  );

  const onNodeClick = useCallback((event, node) => {
    setSelectedNode(node);
    setSelectedEdge(null);
    const wrapperBounds = reactFlowWrapper.current.getBoundingClientRect();
    const position = adjustPanelPosition({
      x: node.position.x + wrapperBounds.left + 20,
      y: node.position.y + wrapperBounds.top
    });
    setEditPanelPosition(position);
  }, []);

  const onEdgeClick = useCallback((event, edge) => {
    event.stopPropagation();
    setSelectedEdge(edge);
    setSelectedNode(null);
    const wrapperBounds = reactFlowWrapper.current.getBoundingClientRect();
    const position = adjustPanelPosition({
      x: wrapperBounds.left + 400,
      y: wrapperBounds.top + 200
    });
    setEditPanelPosition(position);
  }, []);

  const onPaneClick = useCallback(() => {
    setSelectedNode(null);
    setSelectedEdge(null);
  }, []);

  const updateNodeProperties = useCallback(
    (nodeId, newProperties) => {
      setNodes((nds) =>
        nds.map((node) => {
          if (node.id === nodeId) {
            const updatedProperties = {
              cpu: newProperties.cpu !== undefined ? newProperties.cpu : node.data.cpu,
              latency: newProperties.latency !== undefined ? newProperties.latency : node.data.latency,
              status: newProperties.status || node.data.status
            };

            axios
              .post('http://localhost:8081/api/network/update-node', {
                nodeId: nodeId,
                cpu: updatedProperties.cpu,
                latency: updatedProperties.latency,
                status: updatedProperties.status
              })
              .then(() => {
                setMessage(`Node ${nodeId} updated successfully`);
              })
              .catch((error) => {
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
        })
      );
    },
    [setNodes]
  );

  const updateEdgeProperties = useCallback(
    (edgeId, newProperties) => {
      setEdges((eds) =>
        eds.map((edge) => {
          if (edge.id === edgeId) {
            const updatedStatus = newProperties.status || edge.data?.status || 'active';

            axios
              .post('http://localhost:8081/api/network/update-edge', {
                edgeId: edgeId,
                source: edge.source,
                target: edge.target,
                status: updatedStatus
              })
              .then(() => {
                setMessage(`Edge ${edge.source} → ${edge.target} updated successfully`);
              })
              .catch((error) => {
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
                stroke: updatedStatus === 'failed' ? '#e53e3e' : '#bbb',
                strokeWidth: updatedStatus === 'failed' ? 3 : 2,
                strokeDasharray: updatedStatus === 'failed' ? '5 5' : 'none'
              }
            };
          }
          return edge;
        })
      );
    },
    [setEdges]
  );

  const addNode = () => {
    if (!nodeName.trim()) return;

    if (nodes.find((node) => node.id === nodeName.trim())) {
      setMessage(`Node "${nodeName.trim()}" already exists.`);
      return;
    }

    const newNode = {
      id: nodeName.trim(),
      position: { x: Math.random() * 600 + 20, y: Math.random() * 400 + 20 },
      data: {
        label: `${nodeName.trim()} (CPU: ${nodeCpu}, Latency: ${nodeLatency})`,
        cpu: nodeCpu,
        latency: nodeLatency,
        status: nodeStatus
      },
      type: 'default'
    };

    setNodes((nds) => [...nds, newNode]);
    setNodeName('');
    setNodeCpu(4);
    setNodeLatency(50);
    setNodeStatus('active');
    createTopology();
    setMessage('');
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
      setMessage(`Edge ${source} → ${target} marked as failed`);
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
      setMessage(`Edge ${source} → ${target} restored`);
    } catch (error) {
      setMessage(`Error: ${error.response?.data?.message || error.message}`);
    }
  };

  const renderCreateTopologyContent = () => (
    <>
      <h2 style={{ marginTop: 0, marginBottom: 14, color: colors.textPrimary, fontWeight: 700, fontSize: 20 }}>
        Network Builder
      </h2>

      <div style={{ marginBottom: 14 }}>
        <span style={tagStyle(colors.tagBgNodes, colors.tagTextNodes)}>{nodes.length} nodes</span>
        <span style={tagStyle(colors.tagBgEdges, colors.tagTextEdges)}>{edges.length} edges</span>
      </div>

      <hr style={{ borderColor: colors.border, marginBottom: 20 }} />

      <div style={{ marginBottom: 24 }}>
        <label htmlFor="nodeId" style={labelStyle}>
          Node ID
        </label>
        <input
          id="nodeId"
          type="text"
          value={nodeName}
          onChange={(e) => setNodeName(e.target.value)}
          placeholder="Unique identifier"
          style={inputStyle}
          autoComplete="off"
          onFocus={e => (e.currentTarget.style.outlineColor = colors.buttonBg)}
          onBlur={e => (e.currentTarget.style.outlineColor = 'transparent')}
        />
      </div>

      <div style={{ display: 'flex', gap: '12px', marginBottom: 20 }}>
        <div style={{ flex: 1 }}>
          <label htmlFor="nodeCpu" style={labelStyle}>
            CPU Cores
          </label>
          <input
            id="nodeCpu"
            type="number"
            min={1}
            max={64}
            value={nodeCpu}
            onChange={(e) => setNodeCpu(Math.min(64, Math.max(1, Number(e.target.value || 4))))}
            style={inputStyle}
            onFocus={e => (e.currentTarget.style.outlineColor = colors.buttonBg)}
            onBlur={e => (e.currentTarget.style.outlineColor = 'transparent')}
          />
        </div>
        <div style={{ flex: 1 }}>
          <label htmlFor="nodeLatency" style={labelStyle}>
            Latency (ms)
          </label>
          <input
            id="nodeLatency"
            type="number"
            min={1}
            max={1000}
            value={nodeLatency}
            onChange={(e) => setNodeLatency(Math.min(1000, Math.max(1, Number(e.target.value || 50))))}
            style={inputStyle}
            onFocus={e => (e.currentTarget.style.outlineColor = colors.buttonBg)}
            onBlur={e => (e.currentTarget.style.outlineColor = 'transparent')}
          />
        </div>
      </div>

      <div style={{ marginBottom: 28 }}>
        <label htmlFor="nodeStatus" style={labelStyle}>
          Status
        </label>
        <select
          id="nodeStatus"
          value={nodeStatus}
          onChange={(e) => setNodeStatus(e.target.value)}
          style={{
            ...inputStyle,
            cursor: 'pointer',
            paddingRight: 30,
            appearance: 'none',
            WebkitAppearance: 'none',
            MozAppearance: 'none',
            backgroundImage:
              'url("data:image/svg+xml,%3csvg xmlns=\'http://www.w3.org/2000/svg\' fill=\'%23444\' viewBox=\'0 0 20 20\'%3e%3cpath d=\'M7.293 7.293a1 1 0 011.414 0L10 8.586l1.293-1.293a1 1 0 111.414 1.414L10 11.414 7.293 8.707a1 1 0 010-1.414z\'/%3e%3c/svg%3e")',
            backgroundRepeat: 'no-repeat',
            backgroundPosition: 'right 10px center',
            backgroundSize: '14px 14px'
          }}
          onFocus={e => (e.currentTarget.style.outlineColor = colors.buttonBg)}
          onBlur={e => (e.currentTarget.style.outlineColor = 'transparent')}
        >
          <option value="active">Active</option>
          <option value="standby">Standby</option>
          <option value="maintenance">Maintenance</option>
          <option value="failed">Failed</option>
        </select>
      </div>

      <button
        onClick={addNode}
        style={buttonPrimary}
        onMouseOver={e => (e.currentTarget.style.backgroundColor = colors.buttonHoverBg)}
        onMouseOut={e => (e.currentTarget.style.backgroundColor = colors.buttonBg)}
        aria-label="Add node"
        type="button"
      >
        Add Node
      </button>

      <hr style={{ borderColor: colors.border, margin: '30px 0' }} />

      <button
        onClick={createTopology}
        style={{
          ...buttonPrimary,
          backgroundColor: 'transparent',
          border: `1.8px solid ${colors.buttonBg}`,
          color: colors.buttonBg
        }}
        onMouseOver={e => {
          e.currentTarget.style.backgroundColor = colors.buttonBg;
          e.currentTarget.style.color = '#fff';
        }}
        onMouseOut={e => {
          e.currentTarget.style.backgroundColor = 'transparent';
          e.currentTarget.style.color = colors.buttonBg;
        }}
        aria-label="Create topology"
        type="button"
      >
        Create Topology
      </button>
    </>
  );

  const renderFindPathsContent = () => (
    <>
      <h2 style={{ marginTop: 0, marginBottom: 14, color: colors.textPrimary, fontWeight: 700, fontSize: 20 }}>
        Path Calculation
      </h2>

      <div style={{ marginBottom: 24 }}>
        {pathRequests.map((req, index) => (
          <div key={index} style={{ 
            marginBottom: 16,
            padding: '16px',
            borderRadius: 8,
            border: `1px solid ${colors.border}`,
            position: 'relative'
          }}>
            {pathRequests.length > 1 && (
              <button
                onClick={() => removePathRequest(index)}
                style={{
                  position: 'absolute',
                  top: 8,
                  right: 8,
                  background: 'none',
                  border: 'none',
                  color: colors.errorText,
                  cursor: 'pointer',
                  fontSize: 16,
                  fontWeight: 'bold'
                }}
                aria-label="Remove path request"
              >
                ×
              </button>
            )}
            
            <div style={{ marginBottom: 12 }}>
              <label htmlFor={`fromNode-${index}`} style={labelStyle}>
                From Node
              </label>
              <input
                id={`fromNode-${index}`}
                type="text"
                placeholder="Source node ID"
                value={req.from}
                onChange={(e) => updatePathRequest(index, 'from', e.target.value)}
                style={inputStyle}
                autoComplete="off"
              />
            </div>

            <div style={{ marginBottom: 12 }}>
              <label htmlFor={`toNode-${index}`} style={labelStyle}>
                To Node
              </label>
              <input
                id={`toNode-${index}`}
                type="text"
                placeholder="Destination node ID"
                value={req.to}
                onChange={(e) => updatePathRequest(index, 'to', e.target.value)}
                style={inputStyle}
                autoComplete="off"
              />
            </div>
          </div>
        ))}

        <button
          onClick={addPathRequest}
          style={{
            ...buttonPrimary,
            backgroundColor: 'transparent',
            border: `1.5px solid ${colors.buttonBg}`,
            color: colors.buttonBg,
            marginBottom: 20
          }}
          aria-label="Add another path request"
        >
          + Add Another Path
        </button>
      </div>

      <h3 style={{ fontWeight: 700, fontSize: 18, marginBottom: 14, color: colors.textPrimary }}>
        Weight Distribution
      </h3>

      {['hops', 'cpu', 'latency'].map((key) => (
        <div key={key} style={{ marginBottom: 16 }}>
          <label htmlFor={`${key}Range`} style={{ ...labelStyle, fontWeight: 600 }}>
            {key.charAt(0).toUpperCase() + key.slice(1)}: {weights[key]}%
          </label>
          <input
            id={`${key}Range`}
            type="range"
            min={0}
            max={100}
            value={weights[key]}
            onChange={(e) => handleWeightChange(key, e.target.value)}
            style={{ width: '100%', cursor: 'pointer' }}
          />
        </div>
      ))}

      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          fontSize: 12,
          color: colors.textLight,
          marginBottom: 20,
          userSelect: 'none'
        }}
      >
        <span>Total: {weights.hops + weights.cpu + weights.latency}%</span>
        <button
          onClick={() => setWeights({ hops: 50, cpu: 25, latency: 25 })}
          style={{
            padding: '5px 12px',
            border: `1.2px solid ${colors.buttonBg}`,
            borderRadius: 6,
            cursor: 'pointer',
            fontSize: 13,
            backgroundColor: 'transparent',
            color: colors.buttonBg,
            userSelect: 'none'
          }}
          type="button"
          aria-label="Reset weight distribution"
        >
          Reset
        </button>
      </div>

      <button
        onClick={calculateAndDisplayPaths}
        style={buttonPrimary}
        onMouseOver={e => (e.currentTarget.style.backgroundColor = colors.buttonHoverBg)}
        onMouseOut={e => (e.currentTarget.style.backgroundColor = colors.buttonBg)}
        aria-label="Calculate paths"
        type="button"
      >
        Calculate Paths
      </button>

      {Object.keys(batchResults).length > 0 && (
        <div style={{ marginTop: 30 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 14 }}>
            <h3 style={{ fontWeight: 700, fontSize: 18, color: colors.textPrimary }}>
              Calculated Paths
            </h3>
            <button
              onClick={() => {
                setShowMultiplePaths(!showMultiplePaths);
                if (!showMultiplePaths) {
                  setSelectedPaths([]);
                }
              }}
              style={{
                padding: '6px 12px',
                backgroundColor: showMultiplePaths ? colors.buttonBg : 'transparent',
                color: showMultiplePaths ? '#fff' : colors.buttonBg,
                border: `1.5px solid ${colors.buttonBg}`,
                borderRadius: 6,
                cursor: 'pointer',
                fontWeight: 600,
                fontSize: 14
              }}
            >
              {showMultiplePaths ? 'Single Path View' : 'Multi Path View'}
            </button>
          </div>

          {Object.entries(batchResults).map(([pathKey, paths]) => (
            <div key={pathKey} style={{ 
              marginBottom: 20,
              padding: '16px',
              borderRadius: 8,
              border: `1px solid ${colors.border}`
            }}>
              <h4 style={{ marginTop: 0, marginBottom: 12, color: colors.textPrimary }}>
                {pathKey.replace('->', ' → ')}
              </h4>
              {paths.map((path, idx) => {
                const isSelected = selectedPaths.some(p => JSON.stringify(p) === JSON.stringify(path.path));
                return (
                  <div 
                    key={idx} 
                    style={{ 
                      padding: '8px',
                      backgroundColor: isSelected 
                        ? PATH_COLORS[idx % PATH_COLORS.length] + '33' 
                        : idx === 0 ? colors.successBg : colors.lightBg,
                      borderRadius: 6,
                      marginBottom: 8,
                      cursor: 'pointer',
                      border: `1px solid ${isSelected ? PATH_COLORS[idx % PATH_COLORS.length] : 'transparent'}`
                    }}
                    onClick={() => togglePathSelection(pathKey, idx)}
                  >
                    <div style={{ display: 'flex', alignItems: 'center' }}>
                      {showMultiplePaths && (
                        <div 
                          style={{
                            width: 16,
                            height: 16,
                            borderRadius: '50%',
                            backgroundColor: PATH_COLORS[idx % PATH_COLORS.length],
                            marginRight: 8,
                            border: `1px solid ${colors.border}`
                          }}
                        />
                      )}
                      <div>
                        <strong>Path {idx + 1}:</strong> {path.path.join(' → ')}
                        <div style={{ fontSize: 12, color: colors.textLight }}>
                          Weight: {path.weight.toFixed(2)} | Hops: {path.hopCount} | Latency: {path.totalLatency}ms
                        </div>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          ))}
        </div>
      )}
    </>
  );

  const renderEventLogContent = () => (
    <div style={{ color: colors.textSecondary, fontStyle: 'italic', marginTop: 20, fontSize: 14 }}>
      Events log coming soon...
    </div>
  );

  const renderAlarmNotificationsContent = () => (
    <div style={{ color: colors.textSecondary, fontStyle: 'italic', marginTop: 20, fontSize: 14 }}>
      Alarm notifications coming soon...
    </div>
  );

  return (
    <div style={{ 
      height: '100vh', 
      width: '100vw', 
      backgroundColor: colors.lightBg, 
      position: 'relative', 
      overflow: 'hidden',
      display: 'flex',
      flexDirection: 'column'
    }}>
      {/* NAVBAR */}
      <nav style={navStyle} role="navigation" aria-label="Main navigation">
        <div
          onClick={() => setCurrentNav('create_topology')}
          style={navItemStyle(currentNav === 'create_topology')}
          tabIndex={0}
          role="button"
          onKeyPress={e => { if(e.key === 'Enter' || e.key === ' ') setCurrentNav('create_topology'); }}
          aria-current={currentNav === 'create_topology' ? 'page' : undefined}
          aria-label="Create Topology tab"
        >
          Create Topology
        </div>
        <div
          onClick={() => setCurrentNav('find_paths')}
          style={navItemStyle(currentNav === 'find_paths')}
          tabIndex={0}
          role="button"
          onKeyPress={e => { if(e.key === 'Enter' || e.key === ' ') setCurrentNav('find_paths'); }}
          aria-current={currentNav === 'find_paths' ? 'page' : undefined}
          aria-label="Find Paths tab"
        >
          Find Paths
        </div>
        <div
          onClick={() => setCurrentNav('events_log')}
          style={navItemStyle(currentNav === 'events_log')}
          tabIndex={0}
          role="button"
          onKeyPress={e => { if(e.key === 'Enter' || e.key === ' ') setCurrentNav('events_log'); }}
          aria-current={currentNav === 'events_log' ? 'page' : undefined}
          aria-label="Events Log tab"
        >
          Events Log
        </div>
        <div
          onClick={() => setCurrentNav('alarm_notifications')}
          style={navItemStyle(currentNav === 'alarm_notifications')}
          tabIndex={0}
          role="button"
          onKeyPress={e => { if(e.key === 'Enter' || e.key === ' ') setCurrentNav('alarm_notifications'); }}
          aria-current={currentNav === 'alarm_notifications' ? 'page' : undefined}
          aria-label="Alarm Notifications tab"
        >
          Alarm Notifications
        </div>
      </nav>

      {/* MAIN CONTENT AREA */}
      <div style={{ 
        display: 'flex', 
        flex: 1, 
        overflow: 'hidden',
        position: 'relative'
      }}>
        {/* LEFT PANE */}
        <aside style={paneStyle}>
          {message && (
            <div role="alert" style={messageBoxStyle(message.toLowerCase().startsWith('error') || message.toLowerCase().startsWith('please'))}>
              {message}
            </div>
          )}

          {currentNav === 'create_topology' && renderCreateTopologyContent()}
          {currentNav === 'find_paths' && renderFindPathsContent()}
          {currentNav === 'events_log' && renderEventLogContent()}
          {currentNav === 'alarm_notifications' && renderAlarmNotificationsContent()}
        </aside>

        {/* MAIN FLOW AREA */}
        <div style={{ 
          flex: 1, 
          height: '100%',
          position: 'relative',
          overflow: 'hidden'
        }} ref={reactFlowWrapper}>
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
            style={{ 
              height: '100%', 
              width: '100%', 
              backgroundColor: colors.paneBg 
            }}
          >
            <Background />
            <Controls />
            <MiniMap />
          </ReactFlow>
        </div>
      </div>

      {/* NODE EDIT PANEL */}
      {selectedNode && (
        <aside
          style={{
            ...paneStyle,
            width: 300,
            left: Math.min(editPanelPosition.x, viewportDimensions.width - 320),
            top: Math.min(editPanelPosition.y, viewportDimensions.height - 500),
            boxShadow: '0 12px 30px rgb(0 0 0 / 0.12)',
            position: 'fixed',
            zIndex: 1001
          }}
          role="dialog"
          aria-modal="true"
          aria-labelledby="editNodeTitle"
        >
          <header
            style={{
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              marginBottom: 18
            }}
          >
            <h3 id="editNodeTitle" style={{ margin: 0, fontWeight: 700, fontSize: 20, color: colors.textPrimary }}>
              Edit Node: {selectedNode.id}
            </h3>
            <button
              onClick={() => setSelectedNode(null)}
              aria-label="Close edit node panel"
              style={{
                border: 'none',
                background: 'transparent',
                fontSize: 22,
                lineHeight: '1',
                cursor: 'pointer',
                color: colors.textLight,
                userSelect: 'none'
              }}
              type="button"
            >
              ×
            </button>
          </header>

          <div style={{ marginBottom: 16 }}>
            <label htmlFor="editCpu" style={labelStyle}>
              CPU Cores
            </label>
            <input
              id="editCpu"
              type="number"
              min={1}
              max={64}
              value={selectedNode.data.cpu}
              onChange={(e) => updateNodeProperties(selectedNode.id, { cpu: Math.min(64, Math.max(1, Number(e.target.value || 4)))})}
              style={inputStyle}
              onFocus={e => (e.currentTarget.style.outlineColor = colors.buttonBg)}
              onBlur={e => (e.currentTarget.style.outlineColor = 'transparent')}
              aria-describedby="nodeCpuHelp"
            />
          </div>

          <div style={{ marginBottom: 16 }}>
            <label htmlFor="editLatency" style={labelStyle}>
              Latency (ms)
            </label>
            <input
              id="editLatency"
              type="number"
              min={1}
              max={1000}
              value={selectedNode.data.latency}
              onChange={(e) => updateNodeProperties(selectedNode.id, { latency: Math.min(1000, Math.max(1, Number(e.target.value || 50)))})}
              style={inputStyle}
              onFocus={e => (e.currentTarget.style.outlineColor = colors.buttonBg)}
              onBlur={e => (e.currentTarget.style.outlineColor = 'transparent')}
              aria-describedby="nodeLatencyHelp"
            />
          </div>

          <div style={{ marginBottom: 24 }}>
            <label htmlFor="editStatus" style={labelStyle}>
              Status
            </label>
            <select
              id="editStatus"
              value={selectedNode.data.status}
              onChange={(e) => updateNodeProperties(selectedNode.id, { status: e.target.value })}
              style={{
                ...inputStyle,
                cursor: 'pointer',
                paddingRight: 30,
                appearance: 'none',
                WebkitAppearance: 'none',
                MozAppearance: 'none',
                backgroundImage:
                  'url("data:image/svg+xml,%3csvg xmlns=\'http://www.w3.org/2000/svg\' fill=\'%23444\' viewBox=\'0 0 20 20\'%3e%3cpath d=\'M7.293 7.293a1 1 0 011.414 0L10 8.586l1.293-1.293a1 1 0 111.414 1.414L10 11.414 7.293 8.707a1 1 0 010-1.414z\'/%3e%3c/svg%3e")',
                backgroundRepeat: 'no-repeat',
                backgroundPosition: 'right 10px center',
                backgroundSize: '14px 14px'
              }}
              onFocus={e => (e.currentTarget.style.outlineColor = colors.buttonBg)}
              onBlur={e => (e.currentTarget.style.outlineColor = 'transparent')}
            >
              <option value="active">Active</option>
              <option value="standby">Standby</option>
              <option value="maintenance">Maintenance</option>
              <option value="failed">Failed</option>
            </select>
          </div>

          <div style={{ display: 'flex', gap: '10px' }}>
            <button
              type="button"
              onClick={() => simulateNodeFailure(selectedNode.id)}
              style={{
                flex: 1,
                padding: 11,
                backgroundColor: '#e53e3e',
                color: '#fff',
                border: 'none',
                borderRadius: 6,
                cursor: 'pointer',
                fontWeight: 700,
                fontSize: 15,
                userSelect: 'none'
              }}
              aria-label={`Fail node ${selectedNode.id}`}
            >
              Fail Node
            </button>

            <button
              type="button"
              onClick={() => restoreNode(selectedNode.id)}
              style={{
                flex: 1,
                padding: 11,
                backgroundColor: '#48bb78',
                color: '#fff',
                border: 'none',
                borderRadius: 6,
                cursor: 'pointer',
                fontWeight: 700,
                fontSize: 15,
                userSelect: 'none'
              }}
              aria-label={`Restore node ${selectedNode.id}`}
            >
              Restore
            </button>
          </div>

          <button
            type="button"
            onClick={() => {
              deleteNode(selectedNode.id);
              setSelectedNode(null);
            }}
            style={{
              marginTop: 20,
              width: '100%',
              padding: 11,
              backgroundColor: '#c53030',
              color: '#fff',
              border: 'none',
              borderRadius: 6,
              cursor: 'pointer',
              fontWeight: 700,
              fontSize: 15,
              userSelect: 'none'
            }}
            aria-label={`Delete node ${selectedNode.id}`}
          >
            Delete Node
          </button>
        </aside>
      )}

      {/* EDGE EDIT PANEL */}
      {selectedEdge && (
        <aside
          style={{
            ...paneStyle,
            width: 320,
            left: Math.min(editPanelPosition.x, viewportDimensions.width - 340),
            top: Math.min(editPanelPosition.y, viewportDimensions.height - 500),
            boxShadow: '0 12px 30px rgb(0 0 0 / 0.12)',
            position: 'fixed',
            zIndex: 1001
          }}
          role="dialog"
          aria-modal="true"
          aria-labelledby="editEdgeTitle"
        >
          <header
            style={{
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              marginBottom: 18
            }}
          >
            <h3 id="editEdgeTitle" style={{ margin: 0, fontWeight: 700, fontSize: 20, color: colors.textPrimary }}>
              Edit Edge
            </h3>
            <button
              onClick={() => setSelectedEdge(null)}
              aria-label="Close edit edge panel"
              style={{
                border: 'none',
                background: 'transparent',
                fontSize: 22,
                lineHeight: '1',
                cursor: 'pointer',
                color: colors.textLight,
                userSelect: 'none'
              }}
              type="button"
            >
              ×
            </button>
          </header>

          <p style={{ color: colors.textSecondary, marginTop: 0, marginBottom: 10, fontSize: 15 }}>
            <strong>Connection:</strong> {selectedEdge.source} → {selectedEdge.target}
          </p>

          <p style={{ color: colors.textSecondary, marginTop: 0, marginBottom: 28, fontSize: 15 }}>
            <strong>Status:</strong>{' '}
            <span
              style={{
                color: selectedEdge.data?.status === 'failed' ? '#e53e3e' : '#48bb78',
                fontWeight: '700'
              }}
            >
              {selectedEdge.data?.status || 'active'}
            </span>
          </p>

          <div style={{ display: 'flex', gap: '10px' }}>
            <button
              type="button"
              onClick={() => simulateEdgeFailure(selectedEdge.id, selectedEdge.source, selectedEdge.target)}
              style={{
                flex: 1,
                padding: 11,
                backgroundColor: '#e53e3e',
                color: '#fff',
                border: 'none',
                borderRadius: 6,
                cursor: 'pointer',
                fontWeight: 700,
                fontSize: 15,
                userSelect: 'none'
              }}
              aria-label={`Fail edge from ${selectedEdge.source} to ${selectedEdge.target}`}
            >
              Fail Edge
            </button>

            <button
              type="button"
              onClick={() => restoreEdge(selectedEdge.id, selectedEdge.source, selectedEdge.target)}
              style={{
                flex: 1,
                padding: 11,
                backgroundColor: '#48bb78',
                color: '#fff',
                border: 'none',
                borderRadius: 6,
                cursor: 'pointer',
                fontWeight: 700,
                fontSize: 15,
                userSelect: 'none'
              }}
              aria-label={`Restore edge from ${selectedEdge.source} to ${selectedEdge.target}`}
            >
              Restore Edge
            </button>
          </div>

          <button
            type="button"
            onClick={() => {
              deleteEdge(selectedEdge.id);
              setSelectedEdge(null);
            }}
            style={{
              marginTop: 20,
              width: '100%',
              padding: 11,
              backgroundColor: '#c53030',
              color: '#fff',
              border: 'none',
              borderRadius: 6,
              cursor: 'pointer',
              fontWeight: 700,
              fontSize: 15,
              userSelect: 'none'
            }}
            aria-label={`Delete edge from ${selectedEdge.source} to ${selectedEdge.target}`}
          >
            Delete Edge
          </button>
        </aside>
      )}
    </div>
  );
};

export default TopologyCreator;