import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Card from '../components/Card';
import Button from '../components/Button';
import Spinner from '../components/Spinner';

interface ServerInfo {
  id: string;
  name: string;
  type: string;
  mcVersion: string;
  status: 'STOPPED' | 'STARTING' | 'RUNNING' | 'STOPPING' | 'ERROR';
  port: number;
  maxRam: number;
}

const API = '/api';

const statusColors: Record<string, string> = {
  STOPPED: '#6E7681',
  STARTING: '#F59E0B',
  RUNNING: '#22C55E',
  STOPPING: '#F59E0B',
  ERROR: '#EF4444',
};

const statusLabels: Record<string, string> = {
  STOPPED: 'Stopped',
  STARTING: 'Starting...',
  RUNNING: 'Running',
  STOPPING: 'Stopping...',
  ERROR: 'Error',
};

const DashboardPage: React.FC = () => {
  const navigate = useNavigate();
  const [servers, setServers] = useState<ServerInfo[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchServers = async () => {
    try {
      const res = await fetch(`${API}/servers`);
      if (!res.ok) throw new Error('Failed to fetch servers');
      const data = await res.json();
      setServers(data);
    } catch (e: any) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchServers();
    const interval = setInterval(fetchServers, 5000);
    return () => clearInterval(interval);
  }, []);

  const handleAction = async (id: string, action: 'start' | 'stop' | 'restart') => {
    try {
      await fetch(`${API}/servers/${id}/${action}`, { method: 'POST' });
      await fetchServers();
    } catch (e: any) {
      setError(e.message);
    }
  };

  const runningCount = servers.filter((s) => s.status === 'RUNNING').length;
  const totalRam = servers.reduce((sum, s) => sum + s.maxRam, 0) / 1024;

  return (
    <div style={{ padding: 32, maxWidth: 960, margin: '0 auto' }}>
      <h1 style={{ fontSize: 28, fontWeight: 600, marginBottom: 8, color: 'var(--md-on-bg)' }}>
        Dashboard
      </h1>
      <p style={{ color: 'var(--md-on-surface-variant)', marginBottom: 32, fontSize: 14 }}>
        JMCL Server Manager
      </p>

      {/* Stats */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: 16, marginBottom: 32 }}>
        <Card>
          <div style={{ fontSize: 13, color: 'var(--md-on-surface-variant)', marginBottom: 4 }}>
            Total Servers
          </div>
          <div style={{ fontSize: 32, fontWeight: 700, color: 'var(--md-primary)' }}>
            {servers.length}
          </div>
        </Card>
        <Card>
          <div style={{ fontSize: 13, color: 'var(--md-on-surface-variant)', marginBottom: 4 }}>
            Running
          </div>
          <div style={{ fontSize: 32, fontWeight: 700, color: '#22C55E' }}>
            {runningCount}
          </div>
        </Card>
        <Card>
          <div style={{ fontSize: 13, color: 'var(--md-on-surface-variant)', marginBottom: 4 }}>
            Total RAM
          </div>
          <div style={{ fontSize: 32, fontWeight: 700, color: 'var(--md-tertiary)' }}>
            {totalRam.toFixed(1)} GB
          </div>
        </Card>
      </div>

      {/* Server List */}
      <div style={{ marginBottom: 24 }}>
        <h2 style={{ fontSize: 18, fontWeight: 500, marginBottom: 16, color: 'var(--md-on-bg)' }}>
          Your Servers
        </h2>

        {loading && (
          <div style={{ display: 'flex', justifyContent: 'center', padding: 48 }}>
            <Spinner size={32} />
          </div>
        )}

        {error && (
          <div style={{ padding: 16, background: 'var(--md-error-container)', color: 'var(--md-on-error-container)', borderRadius: 'var(--md-radius-md)' }}>
            {error}
          </div>
        )}

        {!loading && servers.length === 0 && (
          <Card style={{ textAlign: 'center', padding: 48 }}>
            <div style={{ fontSize: 48, marginBottom: 12, opacity: 0.4 }}>{'\u26F0'}</div>
            <p style={{ color: 'var(--md-on-surface-variant)', marginBottom: 16 }}>
              No servers yet. Download one to get started!
            </p>
            <Button onClick={() => navigate('/download')}>
              Download Server
            </Button>
          </Card>
        )}

        <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
          {servers.map((server) => (
            <Card key={server.id} hoverable onClick={() => navigate(`/servers/${server.id}/config`)}>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 12 }}>
                <div style={{ flex: 1, minWidth: 200 }}>
                  <div style={{ fontSize: 16, fontWeight: 500, color: 'var(--md-on-surface)' }}>
                    {server.name}
                  </div>
                  <div style={{ fontSize: 12, color: 'var(--md-on-surface-variant)', marginTop: 4 }}>
                    {server.type} \u2022 MC {server.mcVersion} \u2022 Port {server.port} \u2022 {server.maxRam}MB RAM
                  </div>
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                  <span
                    style={{
                      width: 8,
                      height: 8,
                      borderRadius: '50%',
                      background: statusColors[server.status],
                      flexShrink: 0,
                    }}
                  />
                  <span style={{ fontSize: 13, color: 'var(--md-on-surface-variant)', marginRight: 8 }}>
                    {statusLabels[server.status]}
                  </span>
                  {server.status === 'STOPPED' || server.status === 'ERROR' ? (
                    <Button
                      variant="tonal"
                      color="primary"
                      onClick={(e) => { e.stopPropagation(); handleAction(server.id, 'start'); }}
                    >
                      Start
                    </Button>
                  ) : server.status === 'RUNNING' ? (
                    <>
                      <Button
                        variant="tonal"
                        color="error"
                        onClick={(e) => { e.stopPropagation(); handleAction(server.id, 'stop'); }}
                      >
                        Stop
                      </Button>
                      <Button
                        variant="outlined"
                        color="primary"
                        onClick={(e) => { e.stopPropagation(); handleAction(server.id, 'restart'); }}
                      >
                        Restart
                      </Button>
                    </>
                  ) : (
                    <Spinner size={16} />
                  )}
                </div>
              </div>
            </Card>
          ))}
        </div>
      </div>
    </div>
  );
};

export default DashboardPage;
