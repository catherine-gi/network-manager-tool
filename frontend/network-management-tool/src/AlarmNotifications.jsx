import React, { useState, useEffect } from 'react';
import { AlertTriangle, CheckCircle, Clock, Mail, Settings } from 'lucide-react';

const AlarmNotifications = () => {
  const [alerts, setAlerts] = useState([]);
  const [connectionStatus, setConnectionStatus] = useState('disconnected');
  const [emailConfig, setEmailConfig] = useState('');
  const [showEmailConfig, setShowEmailConfig] = useState(false);
  const [emailSaving, setEmailSaving] = useState(false);
  const [wsInstance, setWsInstance] = useState(null);

  useEffect(() => {
    const connectWebSocket = () => {
      const ws = new WebSocket('ws://localhost:8084/alerts');

      ws.onopen = () => setConnectionStatus('connected');

      ws.onmessage = (event) => {
        try {
          const alert = JSON.parse(event.data);
          setAlerts(prev => [alert, ...prev.slice(0, 49)]); // Keep last 50 alerts
        } catch (e) {
          console.error('Error parsing alert:', e);
        }
      };

      ws.onerror = () => setConnectionStatus('error');

      ws.onclose = () => {
        setConnectionStatus('disconnected');
        setTimeout(connectWebSocket, 5000);
      };

      setWsInstance(ws);
      return ws;
    };

    const ws = connectWebSocket();

    fetch('http://localhost:8084/email-config')
      .then(res => res.json())
      .then(data => setEmailConfig(data.recipientEmail || ''))
      .catch(console.error);

    return () => {
      if (ws && ws.readyState === WebSocket.OPEN) ws.close();
    };
  }, []);

  const saveEmailConfig = async () => {
    if (!emailConfig.trim()) return;
    setEmailSaving(true);
    try {
      const response = await fetch('http://localhost:8084/email-config', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ recipientEmail: emailConfig }),
      });

      if (response.ok) {
        setShowEmailConfig(false);
        alert('Email configuration updated successfully!');
      } else {
        alert('Failed to update email configuration');
      }
    } catch (error) {
      console.error(error);
      alert('Error updating email configuration');
    } finally {
      setEmailSaving(false);
    }
  };

  const clearAlerts = () => {
    setAlerts([]);
  };

  const getStatusColor = (status) => {
    switch (status) {
      case 'connected': return 'text-green-500';
      case 'disconnected': return 'text-yellow-500';
      case 'error': return 'text-red-500';
      default: return 'text-gray-500';
    }
  };

  const getStatusIcon = (status) => {
    switch (status) {
      case 'connected': return <CheckCircle className="w-4 h-4" />;
      case 'disconnected': return <Clock className="w-4 h-4" />;
      case 'error': return <AlertTriangle className="w-4 h-4" />;
      default: return <Clock className="w-4 h-4" />;
    }
  };

  return (
    <div style={{
      height: '100%',
      display: 'flex',
      flexDirection: 'column',
      gap: 12,
      fontFamily: 'Inter, sans-serif',
      fontSize: 14,
      color: '#1f2937'
    }}>
      {/* Header */}
      <div style={{
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center'
      }}>
        <h1 style={{
          fontSize: '1.125rem',
          fontWeight: 700,
          display: 'flex',
          alignItems: 'center',
          gap: 8
        }}>
          <AlertTriangle style={{ color: '#ef4444', width: 20, height: 20 }} />
          Alarm Dashboard
        </h1>

        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <button
            onClick={() => setShowEmailConfig(!showEmailConfig)}
            style={{
              padding: '4px 10px',
              fontSize: 13,
              backgroundColor: '#3b82f6',
              color: 'white',
              borderRadius: 6,
              border: 'none',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: 6,
              fontWeight: 500
            }}
            aria-label="Toggle Email Configuration"
          >
            <Settings style={{ width: 16, height: 16 }} />
            Email Config
          </button>
          <div style={{
            display: 'flex',
            alignItems: 'center',
            gap: 6,
            padding: '4px 12px',
            borderRadius: 9999,
            fontWeight: 500,
            fontSize: 13,
            color:
              getStatusColor(connectionStatus) === 'text-green-500'
                ? '#10b981'
                : getStatusColor(connectionStatus) === 'text-yellow-500'
                  ? '#f59e0b'
                  : getStatusColor(connectionStatus) === 'text-red-500'
                    ? '#ef4444'
                    : '#6b7280',
            userSelect: 'none'
          }}>
            {getStatusIcon(connectionStatus)}
            <span>
              {connectionStatus.charAt(0).toUpperCase() + connectionStatus.slice(1)}
            </span>
          </div>
        </div>
      </div>

      {/* Email Configuration Section */}
      {showEmailConfig && (
        <div style={{
          backgroundColor: '#f9fafb',
          borderRadius: 8,
          padding: 12,
          border: '1px solid #d1d5db',
          marginBottom: 12
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 8 }}>
            <Mail style={{ width: 18, height: 18, color: '#3b82f6' }} />
            <h2 style={{ fontSize: '1rem', fontWeight: 600, margin: 0 }}>
              Email Configuration
            </h2>
          </div>
          <label style={{ fontWeight: 600, fontSize: 13, marginBottom: 4, display: 'block', color: '#374151' }}>
            Alert Recipient Email
          </label>
          <input
            type="email"
            value={emailConfig}
            onChange={(e) => setEmailConfig(e.target.value)}
            placeholder="Enter recipient email"
            style={{
              width: '100%',
              padding: 6,
              fontSize: 14,
              borderRadius: 6,
              border: '1px solid #d1d5db',
              marginBottom: 8
            }}
            aria-label="Alert Recipient Email"
          />
          <div style={{ display: 'flex', gap: 8 }}>
            <button
              onClick={saveEmailConfig}
              disabled={emailSaving || !emailConfig.trim()}
              style={{
                flex: 1,
                backgroundColor: emailSaving || !emailConfig.trim() ? '#9ca3af' : '#10b981',
                border: 'none',
                borderRadius: 6,
                padding: '8px',
                color: 'white',
                fontWeight: 600,
                cursor: emailSaving || !emailConfig.trim() ? 'not-allowed' : 'pointer'
              }}
              aria-label="Save Email Configuration"
            >
              {emailSaving ? 'Saving...' : 'Save'}
            </button>
            <button
              onClick={() => setShowEmailConfig(false)}
              style={{
                flex: 1,
                backgroundColor: '#6b7280',
                border: 'none',
                borderRadius: 6,
                padding: '8px',
                color: 'white',
                fontWeight: 600,
                cursor: 'pointer'
              }}
              aria-label="Cancel Email Configuration"
            >
              Cancel
            </button>
          </div>
          <p style={{ fontSize: 12, color: '#6b7280', marginTop: 8, userSelect: 'none' }}>
            Current recipient: {emailConfig || 'Using default email'}
          </p>
        </div>
      )}

      {/* Stats */}
      <div style={{
        display: 'flex',
        gap: 12
      }}>
        <div style={{
          backgroundColor: '#ffffff',
          borderRadius: 8,
          padding: 10,
          flex: 1,
          boxShadow: '0 1px 3px rgb(0 0 0 / 0.1)'
        }}>
          <p style={{ fontSize: 12, fontWeight: 600, color: '#6b7280', marginBottom: 2 }}>Total Alerts</p>
          <p style={{ fontSize: 20, fontWeight: 700, margin: 0, color: '#1f2937' }}>{alerts.length}</p>
        </div>

        <div style={{
          backgroundColor: '#ffffff',
          borderRadius: 8,
          padding: 10,
          flex: 1,
          boxShadow: '0 1px 3px rgb(0 0 0 / 0.1)'
        }}>
          <p style={{ fontSize: 12, fontWeight: 600, color: '#6b7280', marginBottom: 2 }}>Active Failures</p>
          <p style={{ fontSize: 20, fontWeight: 700, margin: 0, color: '#ef4444' }}>
            {alerts.filter(alert => new Date(alert.timestamp) > new Date(Date.now() - 5 * 60 * 1000)).length}
          </p>
        </div>

        <div style={{
          backgroundColor: '#ffffff',
          borderRadius: 8,
          padding: 10,
          flex: 1,
          boxShadow: '0 1px 3px rgb(0 0 0 / 0.1)'
        }}>
          <p style={{ fontSize: 12, fontWeight: 600, color: '#6b7280', marginBottom: 2 }}>Email Status</p>
          <p style={{ fontSize: 20, fontWeight: 700, margin: 0, color: '#3b82f6' }}>
            {emailConfig ? 'Configured' : 'Default'}
          </p>
        </div>
      </div>

      {/* Alerts List */}
      <div style={{
        flex: 1,
        backgroundColor: '#fff',
        borderRadius: 8,
        marginTop: 8,
        overflowY: 'auto',
        border: '1px solid #e5e7eb',
        display: 'flex',
        flexDirection: 'column'
      }}>
        <div style={{
          padding: '12px 16px',
          borderBottom: '1.5px solid #e5e7eb',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          fontWeight: 600,
          color: '#1f2937',
          fontSize: 15,
          userSelect: 'none'
        }}>
          <span>Recent Alerts</span>
          {alerts.length > 0 && (
            <button
              onClick={clearAlerts}
              style={{
                padding: '4px 12px',
                backgroundColor: '#6b7280',
                borderRadius: 6,
                color: '#fff',
                border: 'none',
                cursor: 'pointer',
                fontSize: 13,
                fontWeight: 600
              }}
              aria-label="Clear all alerts"
            >
              Clear All
            </button>
          )}
        </div>

        <div style={{ padding: 16, flex: 1, overflowY: 'auto' }}>
          {alerts.length === 0 ? (
            <div style={{
              textAlign: 'center',
              color: '#6b7280',
              userSelect: 'none'
            }}>
              <CheckCircle style={{ width: 36, height: 36, color: '#10b981', marginBottom: 12 }} />
              <p style={{ fontWeight: 600, fontSize: 16, marginBottom: 6 }}>No alerts</p>
              <p style={{ fontSize: 13 }}>All nodes are running normally</p>
            </div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 12, maxHeight: '100%' }}>
              {alerts.map((alert, index) => (
                <div key={index} style={{
                  display: 'flex',
                  gap: 12,
                  padding: 12,
                  borderRadius: 6,
                  backgroundColor: '#f9fafb',
                  borderLeft: '4px solid #ef4444',
                  userSelect: 'text'
                }}>
                  <AlertTriangle style={{ width: 18, height: 18, color: '#ef4444', flexShrink: 0, marginTop: 2 }} />
                  <div style={{ flex: 1 }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', fontWeight: 600, color: '#1f2937' }}>
                      <span>Node {alert.nodeId} Failed</span>
                      <time style={{ fontSize: 12, color: '#6b7280' }} dateTime={alert.timestamp}>
                        {new Date(alert.timestamp).toLocaleString()}
                      </time>
                    </div>
                    <p style={{ marginTop: 4, fontSize: 13, color: '#6b7280' }}>{alert.message}</p>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Connection Status */}
      <div style={{
        backgroundColor: '#eff6ff',
        border: '1px solid #bfdbfe',
        borderRadius: 8,
        padding: 12,
        color: '#1e40af',
        fontSize: 13,
        fontWeight: 500,
        userSelect: 'none'
      }}>
        <h3 style={{ margin: '0 0 8px 0', fontSize: 14 }}>Service Status</h3>
        <p style={{ margin: 0 }}>
          {connectionStatus === 'connected'
            ? 'Successfully connected to alarm service. Monitoring for node failures...'
            : connectionStatus === 'error'
              ? 'Connection error. Attempting to reconnect...'
              : 'Disconnected from alarm service. Attempting to reconnect...'}
        </p>
      </div>
    </div>
  );
};

export default AlarmNotifications;