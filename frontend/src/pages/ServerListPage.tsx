import React from 'react';
import { useNavigate } from 'react-router-dom';
import Card from '../components/Card';
import Button from '../components/Button';
import Spinner from '../components/Spinner';

interface ServerInfo {
  id: string;
  name: string;
  type: string;
  mcVersion: string;
  status: string;
  port: number;
  maxRam: number;
  minRam: number;
  serverJar: string;
}

const API = '/api';

const ServerListPage: React.FC = () => {
  const navigate = useNavigate();
  const [servers, setServers] = React.useState<ServerInfo[]>([]);
  const [loading, setLoading] = React.useState(true);

  React.useEffect(() => {
    fetch(`${API}/servers`)
      .then((r) => r.json())
      .then(setServers)
      .finally(() => setLoading(false));
  }, []);

  if (loading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100%' }}>
        <Spinner size={36} />
      </div>
    );
  }

  return (
    <div style={{ padding: 32, maxWidth: 960, margin: '0 auto' }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 24 }}>
        <h1 style={{ fontSize: 28, fontWeight: 600, color: 'var(--md-on-bg)' }}>Servers</h1>
        <Button onClick={() => navigate('/download')}>
          + Add Server
        </Button>
      </div>

      {servers.length === 0 ? (
        <Card style={{ textAlign: 'center', padding: 48 }}>
          <p style={{ color: 'var(--md-on-surface-variant)', marginBottom: 16 }}>
            No server instances configured
          </p>
          <Button onClick={() => navigate('/download')}>
            Download Server
          </Button>
        </Card>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
          {servers.map((server) => (
            <Card key={server.id} hoverable onClick={() => navigate(`/servers/${server.id}/config`)}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                <div>
                  <h3 style={{ fontSize: 16, fontWeight: 500, color: 'var(--md-on-surface)' }}>
                    {server.name}
                  </h3>
                  <div style={{ fontSize: 12, color: 'var(--md-on-surface-variant)', marginTop: 4 }}>
                    {server.type} \u2022 MC {server.mcVersion} \u2022 Port: {server.port}
                  </div>
                  <div style={{ fontSize: 12, color: 'var(--md-on-surface-variant)', marginTop: 2 }}>
                    RAM: {server.minRam}MB - {server.maxRam}MB
                  </div>
                </div>
                <span
                  style={{
                    padding: '4px 12px',
                    borderRadius: 'var(--md-radius-sm)',
                    fontSize: 12,
                    fontWeight: 500,
                    background: server.status === 'RUNNING'
                      ? 'rgba(34,197,94,0.2)'
                      : server.status === 'ERROR'
                      ? 'rgba(239,68,68,0.2)'
                      : 'var(--md-surface-container-highest)',
                    color: server.status === 'RUNNING'
                      ? '#22C55E'
                      : server.status === 'ERROR'
                      ? '#EF4444'
                      : 'var(--md-on-surface-variant)',
                  }}
                >
                  {server.status}
                </span>
              </div>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
};

export default ServerListPage;
