import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import Card from '../components/Card';
import Button from '../components/Button';
import TextField from '../components/TextField';
import Terminal from '../components/Terminal';
import Spinner from '../components/Spinner';
import Dialog from '../components/Dialog';
import { useUIStore } from '../stores/uiStore';

interface ServerInstance {
  id: string;
  name: string;
  type: string;
  mcVersion: string;
  status: string;
  port: number;
  maxRam: number;
  minRam: number;
  javaArgs: string;
  serverJar: string;
  dataDir: string;
  properties: Record<string, string>;
}

interface LogLine {
  timestamp: string;
  level: string;
  message: string;
}

const API = '/api';

const ServerConfigPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const showSnackbar = useUIStore((s) => s.showSnackbar);

  const [server, setServer] = useState<ServerInstance | null>(null);
  const [loading, setLoading] = useState(true);
  const [logs, setLogs] = useState<LogLine[]>([]);
  const [command, setCommand] = useState('');
  const [showDeleteDialog, setShowDeleteDialog] = useState(false);
  const [saving, setSaving] = useState(false);

  // Form state
  const [name, setName] = useState('');
  const [port, setPort] = useState(25565);
  const [maxRam, setMaxRam] = useState(2048);
  const [minRam, setMinRam] = useState(1024);
  const [props, setProps] = useState<Record<string, string>>({});

  const fetchServer = async () => {
    try {
      const [serverRes, logsRes] = await Promise.all([
        fetch(`${API}/servers/${id}`),
        fetch(`${API}/servers/${id}/logs?tail=200`),
      ]);
      const serverData = await serverRes.json();
      const logsData = await logsRes.json();

      setServer(serverData);
      setName(serverData.name || '');
      setPort(serverData.port || 25565);
      setMaxRam(serverData.maxRam || 2048);
      setMinRam(serverData.minRam || 1024);
      setProps(serverData.properties || {});
      setLogs(logsData || []);
    } catch (e: any) {
      showSnackbar('Failed to fetch server data', 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchServer();
    const interval = setInterval(fetchServer, 3000);
    return () => clearInterval(interval);
  }, [id]);

  const handleAction = async (action: 'start' | 'stop' | 'restart') => {
    try {
      const res = await fetch(`${API}/servers/${id}/${action}`, { method: 'POST' });
      if (!res.ok) {
        const data = await res.json();
        showSnackbar(data.error || `${action} failed`, 'error');
        return;
      }
      showSnackbar(`Server ${action} initiated`, 'success');
      setTimeout(fetchServer, 2000);
    } catch (e: any) {
      showSnackbar(e.message, 'error');
    }
  };

  const handleSendCommand = async () => {
    if (!command.trim()) return;
    try {
      await fetch(`${API}/servers/${id}/command`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ command }),
      });
      setCommand('');
      showSnackbar('Command sent', 'success');
    } catch (e: any) {
      showSnackbar(e.message, 'error');
    }
  };

  const handleSaveConfig = async () => {
    setSaving(true);
    try {
      const res = await fetch(`${API}/config/${id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          serverId: id,
          properties: props,
        }),
      });
      if (!res.ok) {
        const data = await res.json();
        showSnackbar(data.error || 'Save failed', 'error');
        return;
      }
      // Also update instance settings
      await fetch(`${API}/config/${id}/instance`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          name,
          port,
          maxRam,
          minRam,
        }),
      });
      showSnackbar('Configuration saved', 'success');
    } catch (e: any) {
      showSnackbar(e.message, 'error');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    try {
      await fetch(`${API}/servers/${id}`, { method: 'DELETE' });
      showSnackbar('Server deleted', 'success');
      navigate('/');
    } catch (e: any) {
      showSnackbar(e.message, 'error');
    }
  };

  if (loading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100%' }}>
        <Spinner size={36} />
      </div>
    );
  }

  if (!server) {
    return (
      <div style={{ padding: 32, textAlign: 'center' }}>
        <p>Server not found</p>
        <Button onClick={() => navigate('/')}>Go Back</Button>
      </div>
    );
  }

  const statusColor = server.status === 'RUNNING' ? '#22C55E' : server.status === 'ERROR' ? '#EF4444' : '#6E7681';

  return (
    <div style={{ padding: 32, maxWidth: 960, margin: '0 auto', paddingBottom: 120 }}>
      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 24 }}>
        <div>
          <h1 style={{ fontSize: 28, fontWeight: 600, color: 'var(--md-on-bg)' }}>{server.name}</h1>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginTop: 4 }}>
            <span
              style={{
                width: 8,
                height: 8,
                borderRadius: '50%',
                background: statusColor,
              }}
            />
            <span style={{ fontSize: 14, color: 'var(--md-on-surface-variant)' }}>
              {server.status} \u2022 {server.type} \u2022 MC {server.mcVersion}
            </span>
          </div>
        </div>
        <div style={{ display: 'flex', gap: 8 }}>
          {server.status === 'STOPPED' || server.status === 'ERROR' ? (
            <Button onClick={() => handleAction('start')}>
              {`\u25B6`} Start
            </Button>
          ) : (
            <>
              <Button color="error" variant="tonal" onClick={() => handleAction('stop')}>
                {`\u25A0`} Stop
              </Button>
              <Button variant="outlined" onClick={() => handleAction('restart')}>
                {`\u21BB`} Restart
              </Button>
            </>
          )}
        </div>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 }}>
        {/* Settings Column */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          <Card>
            <h3 style={{ fontSize: 16, fontWeight: 500, marginBottom: 16, color: 'var(--md-on-surface)' }}>
              Instance Settings
            </h3>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
              <TextField
                label="Server Name"
                value={name}
                onChange={(e) => setName(e.target.value)}
                fullWidth
              />
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
                <TextField
                  label="Port"
                  type="number"
                  value={String(port)}
                  onChange={(e) => setPort(Number(e.target.value))}
                />
                <div />
                <TextField
                  label="Min RAM (MB)"
                  type="number"
                  value={String(minRam)}
                  onChange={(e) => setMinRam(Number(e.target.value))}
                />
                <TextField
                  label="Max RAM (MB)"
                  type="number"
                  value={String(maxRam)}
                  onChange={(e) => setMaxRam(Number(e.target.value))}
                />
              </div>
            </div>
          </Card>

          <Card>
            <h3 style={{ fontSize: 16, fontWeight: 500, marginBottom: 16, color: 'var(--md-on-surface)' }}>
              server.properties
            </h3>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 8, maxHeight: 400, overflow: 'auto' }}>
              {Object.entries(props).map(([key, value]) => (
                <div key={key} style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
                  <label
                    style={{
                      fontSize: 12,
                      color: 'var(--md-on-surface-variant)',
                      width: 160,
                      flexShrink: 0,
                      overflow: 'hidden',
                      textOverflow: 'ellipsis',
                      whiteSpace: 'nowrap',
                    }}
                    title={key}
                  >
                    {key}
                  </label>
                  <input
                    value={value}
                    onChange={(e) => setProps({ ...props, [key]: e.target.value })}
                    style={{
                      flex: 1,
                      height: 32,
                      padding: '0 8px',
                      border: '1px solid var(--md-outline-variant)',
                      borderRadius: 4,
                      background: 'var(--md-surface-container-highest)',
                      color: 'var(--md-on-surface)',
                      fontSize: 13,
                      fontFamily: 'inherit',
                    }}
                  />
                </div>
              ))}
            </div>
          </Card>

          <div style={{ display: 'flex', gap: 8, justifyContent: 'space-between' }}>
            <Button variant="outlined" color="error" onClick={() => setShowDeleteDialog(true)}>
              Delete Server
            </Button>
            <Button onClick={handleSaveConfig} disabled={saving}>
              {saving ? 'Saving...' : 'Save Configuration'}
            </Button>
          </div>
        </div>

        {/* Console Column */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          <Card style={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
            <h3 style={{ fontSize: 16, fontWeight: 500, marginBottom: 12, color: 'var(--md-on-surface)' }}>
              Console
            </h3>
            <div style={{ flex: 1 }}>
              <Terminal lines={logs} maxHeight={500} />
            </div>
          </Card>

          {server.status === 'RUNNING' && (
            <Card>
              <div style={{ display: 'flex', gap: 8 }}>
                <input
                  value={command}
                  onChange={(e) => setCommand(e.target.value)}
                  onKeyDown={(e) => { if (e.key === 'Enter') handleSendCommand(); }}
                  placeholder="Type a server command..."
                  style={{
                    flex: 1,
                    height: 40,
                    padding: '0 12px',
                    border: '1px solid var(--md-outline-variant)',
                    borderRadius: 'var(--md-radius-xl)',
                    background: 'var(--md-surface-container-highest)',
                    color: 'var(--md-on-surface)',
                    fontSize: 14,
                    fontFamily: 'inherit',
                  }}
                />
                <Button onClick={handleSendCommand}>Send</Button>
              </div>
            </Card>
          )}
        </div>
      </div>

      {/* Delete Dialog */}
      <Dialog
        open={showDeleteDialog}
        onClose={() => setShowDeleteDialog(false)}
        title="Delete Server"
        actions={
          <>
            <Button variant="outlined" onClick={() => setShowDeleteDialog(false)}>
              Cancel
            </Button>
            <Button color="error" onClick={handleDelete}>
              Delete
            </Button>
          </>
        }
      >
        <p style={{ color: 'var(--md-on-surface-variant)', fontSize: 14 }}>
          Are you sure you want to delete "{server.name}"? This will remove all server data including worlds.
        </p>
      </Dialog>
    </div>
  );
};

export default ServerConfigPage;
