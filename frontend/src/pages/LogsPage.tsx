import React, { useEffect, useState, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import Button from '../components/Button';
import Terminal from '../components/Terminal';
import Spinner from '../components/Spinner';

interface LogLine {
  timestamp: string;
  level: string;
  message: string;
}

const API = '/api';

const LogsPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [logs, setLogs] = useState<LogLine[]>([]);
  const [loading, setLoading] = useState(true);
  const [autoScroll, setAutoScroll] = useState(true);
  const [serverName, setServerName] = useState('');

  useEffect(() => {
    fetch(`${API}/servers/${id}`)
      .then((r) => r.json())
      .then((d) => setServerName(d.name || id || ''))
      .catch(() => {});

    const fetchLogs = () => {
      fetch(`${API}/servers/${id}/logs?tail=500`)
        .then((r) => r.json())
        .then((data) => {
          setLogs(data || []);
          setLoading(false);
        })
        .catch(() => setLoading(false));
    };

    fetchLogs();
    const interval = setInterval(fetchLogs, 2000);
    return () => clearInterval(interval);
  }, [id]);

  return (
    <div style={{ padding: 32, maxWidth: 960, margin: '0 auto', height: '100%', display: 'flex', flexDirection: 'column' }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 16 }}>
        <div>
          <h1 style={{ fontSize: 28, fontWeight: 600, color: 'var(--md-on-bg)' }}>
            {serverName || 'Server Logs'}
          </h1>
          <p style={{ fontSize: 13, color: 'var(--md-on-surface-variant)', marginTop: 4 }}>
            Live console output
          </p>
        </div>
        <div style={{ display: 'flex', gap: 8 }}>
          <Button
            variant={autoScroll ? 'filled' : 'outlined'}
            onClick={() => setAutoScroll(!autoScroll)}
          >
            Auto-scroll {autoScroll ? 'ON' : 'OFF'}
          </Button>
          <Button
            variant="text"
            onClick={() => navigate(`/servers/${id}/config`)}
          >
            Back to Config
          </Button>
        </div>
      </div>

      {loading ? (
        <div style={{ display: 'flex', justifyContent: 'center', padding: 48 }}>
          <Spinner size={32} />
        </div>
      ) : (
        <div style={{ flex: 1, minHeight: 0 }}>
          <Terminal lines={logs} maxHeight={Math.max(600, window.innerHeight - 250)} autoScroll={autoScroll} />
        </div>
      )}
    </div>
  );
};

export default LogsPage;
