import React, { useState, useEffect } from 'react';
import axios from 'axios';

const colors = {
  textPrimary: '#111827',
  textSecondary: '#6b7280',
  buttonBg: '#111827',
  border: '#ddd',
  lightBg: '#f9fafb',
  navBg: '#f3f4f6',
  errorBg: '#fdecea',
  errorText: '#b91c1c',
  textLight: '#9ca3af',
};

const inputStyle = {
  width: '100%',
  padding: '8px 10px',
  borderRadius: 6,
  border: `1.5px solid ${colors.border}`,
  fontSize: 14,
  boxSizing: 'border-box',
  fontFamily: 'Inter, sans-serif',
  color: colors.textPrimary,
  backgroundColor: 'white',
};

const labelStyle = {
  marginBottom: 6,
  fontWeight: 600,
  color: colors.textPrimary,
  fontSize: 14,
  fontFamily: 'Inter, sans-serif',
  display: 'block',
};

const buttonPrimary = {
  padding: '10px 16px',
  backgroundColor: colors.buttonBg,
  color: 'white',
  border: 'none',
  borderRadius: 8,
  cursor: 'pointer',
  fontWeight: 700,
  fontSize: 15,
  fontFamily: 'Inter, sans-serif',
  userSelect: 'none',
  transition: 'background-color 0.25s ease',
};

const eventTypes = [
  { label: 'All Event Types', value: '' },
  { label: 'TOPOLOGY_INITIALIZATION', value: 'TOPOLOGY_INITIALIZATION' },
  { label: 'NODE_FAILURE', value: 'NODE_FAILURE' },
  { label: 'NODE_RECOVERY', value: 'NODE_RECOVERY' },
];

const EventLog = () => {
  const [allEvents, setAllEvents] = useState([]);
  const [filteredEvents, setFilteredEvents] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [filters, setFilters] = useState({
    eventType: '',
    startDate: null,
    endDate: null,
  });

  // Fetch data on mount
  useEffect(() => {
    const fetchEvents = async () => {
      try {
        setLoading(true);
        const response = await axios.get('http://localhost:8082/api/events');
        setAllEvents(response.data || []);
        setError('');
      } catch {
        setError('Failed to fetch events');
        setAllEvents([]);
      } finally {
        setLoading(false);
      }
    };
    fetchEvents();
  }, []);

  // Apply filtering when filters or allEvents change
  useEffect(() => {
    const filtered = allEvents.filter(event => {
      // Event type matches filter description or all
      if (filters.eventType && event.description !== filters.eventType) {
        return false;
      }

      const eventTimestamp = Number(event.epoch);
      const startTimestamp = filters.startDate ? filters.startDate.getTime() : null;
      const endTimestamp = filters.endDate ? filters.endDate.getTime() : null;

      if (startTimestamp && eventTimestamp < startTimestamp) return false;
      if (endTimestamp && eventTimestamp > endTimestamp) return false;

      return true;
    });
    setFilteredEvents(filtered);
  }, [allEvents, filters]);

  const handleFilterChange = (key, value) => {
    setFilters(prev => ({ ...prev, [key]: value }));
  };

  const clearFilters = () => {
    setFilters({ eventType: '', startDate: null, endDate: null });
  };

  const formatDate = (epoch) => {
    if (!epoch || epoch === 0) return 'N/A';
    try {
      return new Date(epoch).toLocaleString('en-US', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
        hour12: false
      });
    } catch {
      return 'Invalid Date';
    }
  };

  const formatDateTimeLocal = (date) => {
    if (!date) return '';
    const year = date.getFullYear();
    const month = (date.getMonth() + 1).toString().padStart(2, '0');
    const day = date.getDate().toString().padStart(2, '0');
    const hours = date.getHours().toString().padStart(2, '0');
    const minutes = date.getMinutes().toString().padStart(2, '0');
    return `${year}-${month}-${day}T${hours}:${minutes}`;
  };

  return (
    <div style={{ marginTop: 20, fontFamily: 'Inter, sans-serif', color: colors.textPrimary, maxWidth: 900, marginLeft: 'auto', marginRight: 'auto' }}>
      <h3 style={{ marginBottom: 16, fontWeight: 700 }}>System Event Log</h3>

      <div style={{
        display: 'flex',
        flexWrap: 'wrap',
        gap: 12,
        alignItems: 'flex-end',
        marginBottom: 20,
      }}>
        <div style={{ flex: '1 1 220px', minWidth: 220 }}>
          <label htmlFor="eventType" style={labelStyle}>Event Type</label>
          <select
            id="eventType"
            style={inputStyle}
            value={filters.eventType}
            onChange={e => handleFilterChange('eventType', e.target.value)}
          >
            {eventTypes.map(({ label, value }) => (
              <option key={value} value={value}>{label}</option>
            ))}
          </select>
        </div>

        <div style={{ flex: '1 1 180px', minWidth: 160 }}>
          <label htmlFor="startDate" style={labelStyle}>Start Date and Time</label>
          <input
            id="startDate"
            type="datetime-local"
            style={inputStyle}
            value={formatDateTimeLocal(filters.startDate)}
            onChange={e => handleFilterChange('startDate', e.target.value ? new Date(e.target.value) : null)}
            aria-label="Start date and time"
          />
        </div>

        <div style={{ flex: '1 1 180px', minWidth: 160 }}>
          <label htmlFor="endDate" style={labelStyle}>End Date and Time</label>
          <input
            id="endDate"
            type="datetime-local"
            style={inputStyle}
            value={formatDateTimeLocal(filters.endDate)}
            onChange={e => handleFilterChange('endDate', e.target.value ? new Date(e.target.value) : null)}
            aria-label="End date and time"
          />
        </div>

        <button
          type="button"
          onClick={clearFilters}
          style={{
            ...buttonPrimary,
            backgroundColor: 'transparent',
            border: `1.5px solid ${colors.buttonBg}`,
            color: colors.buttonBg,
            flex: '0 0 auto',
            height: 40,
            padding: '8px 20px',
            fontWeight: 600,
            marginLeft: 'auto',
            userSelect: 'none',
          }}
          onMouseEnter={e => {
            e.currentTarget.style.backgroundColor = colors.buttonBg;
            e.currentTarget.style.color = 'white';
          }}
          onMouseLeave={e => {
            e.currentTarget.style.backgroundColor = 'transparent';
            e.currentTarget.style.color = colors.buttonBg;
          }}
          aria-label="Clear filters"
        >
          Clear Filters
        </button>
      </div>

      {error && (
        <div role="alert" style={{
          padding: 12,
          borderRadius: 8,
          backgroundColor: colors.errorBg,
          color: colors.errorText,
          fontSize: 14,
          marginBottom: 16,
          fontWeight: 600,
        }}>
          {error}
        </div>
      )}

      <div style={{ maxHeight: 'calc(100vh - 300px)', overflowY: 'auto', border: `1.5px solid ${colors.border}`, borderRadius: 8, boxShadow: 'rgba(0,0,0,0.05) 0px 1px 3px' }}>
        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 14 }} role="grid" aria-label="Event log table">
          <thead style={{ backgroundColor: colors.navBg, position: 'sticky', top: 0, zIndex: 10, userSelect: 'none' }}>
            <tr>
              <th style={{ padding: '14px 16px', textAlign: 'left', borderBottom: `1px solid ${colors.border}`, whiteSpace: 'nowrap' }}>Timestamp</th>
              <th style={{ padding: '14px 16px', textAlign: 'left', borderBottom: `1px solid ${colors.border}` }}>Event Description</th>
              <th style={{ padding: '14px 16px', textAlign: 'left', borderBottom: `1px solid ${colors.border}` }}>Event Type</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan={3} style={{ padding: 24, textAlign: 'center', color: colors.textSecondary }}>
                  Loading events...
                </td>
              </tr>
            ) : filteredEvents.length === 0 ? (
              <tr>
                <td colSpan={3} style={{ padding: 24, textAlign: 'center', color: colors.textSecondary }}>
                  No events found matching your filters.
                </td>
              </tr>
            ) : (
              filteredEvents.map(event => (
                <tr key={event.id} tabIndex={0} style={{ borderBottom: `1px solid ${colors.border}`, cursor: 'default', transition: 'background-color 0.1s ease' }}
                  onMouseEnter={e => (e.currentTarget.style.backgroundColor = colors.lightBg)}
                  onMouseLeave={e => (e.currentTarget.style.backgroundColor = 'transparent')}
                  aria-label={`Event ${event.description} occurred at ${formatDate(event.epoch)}`}>
                  <td style={{ padding: '12px 16px', whiteSpace: 'nowrap' }}>{formatDate(event.epoch)}</td>
                  <td style={{ padding: '12px 16px', fontWeight: 600, textTransform: 'uppercase' }}>{event.description}</td>
                  <td style={{ padding: '12px 16px' }}>{event.eventType}</td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default EventLog;