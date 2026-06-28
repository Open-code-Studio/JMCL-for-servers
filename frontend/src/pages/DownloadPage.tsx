import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Card from '../components/Card';
import Button from '../components/Button';
import TextField from '../components/TextField';
import Spinner from '../components/Spinner';
import { useUIStore } from '../stores/uiStore';

interface ServerType {
  id: string;
  name: string;
  category: string;
  description: string;
}

const API = '/api';

const categoryLabels: Record<string, string> = {
  VANILLA: 'Vanilla',
  HYBRID: 'Plugin Hybrid',
  MODDED: 'Modded',
  PROXY: 'Proxy',
};

const DownloadPage: React.FC = () => {
  const navigate = useNavigate();
  const showSnackbar = useUIStore((s) => s.showSnackbar);

  const [types, setTypes] = useState<ServerType[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedType, setSelectedType] = useState<string | null>(null);
  const [mcVersion, setMcVersion] = useState('1.20.4');
  const [serverName, setServerName] = useState('');
  const [downloading, setDownloading] = useState(false);

  useEffect(() => {
    fetch(`${API}/download/types`)
      .then((r) => r.json())
      .then(setTypes)
      .finally(() => setLoading(false));
  }, []);

  const handleDownload = async () => {
    if (!selectedType) {
      showSnackbar('Please select a server type', 'error');
      return;
    }
    if (!mcVersion.trim()) {
      showSnackbar('Please enter a Minecraft version', 'error');
      return;
    }

    setDownloading(true);
    try {
      const res = await fetch(`${API}/download/start`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          type: selectedType,
          mcVersion: mcVersion.trim(),
          serverName: serverName.trim() || undefined,
        }),
      });
      const data = await res.json();
      showSnackbar('Download started! Creating server instance...', 'success');
      setTimeout(() => navigate('/'), 3000);
    } catch (e: any) {
      showSnackbar(e.message, 'error');
    } finally {
      setDownloading(false);
    }
  };

  if (loading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100%' }}>
        <Spinner size={36} />
      </div>
    );
  }

  // Group by category
  const grouped = types.reduce((acc: Record<string, ServerType[]>, t) => {
    if (!acc[t.category]) acc[t.category] = [];
    acc[t.category].push(t);
    return acc;
  }, {});

  return (
    <div style={{ padding: 32, maxWidth: 960, margin: '0 auto' }}>
      <h1 style={{ fontSize: 28, fontWeight: 600, marginBottom: 8, color: 'var(--md-on-bg)' }}>
        Download Server
      </h1>
      <p style={{ color: 'var(--md-on-surface-variant)', marginBottom: 32, fontSize: 14 }}>
        Choose a server type and version to download
      </p>

      <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: 24 }}>
        {/* Server types */}
        <div>
          {Object.entries(grouped).map(([category, serverTypes]) => (
            <div key={category} style={{ marginBottom: 24 }}>
              <h3
                style={{
                  fontSize: 14,
                  fontWeight: 600,
                  color: 'var(--md-on-surface-variant)',
                  textTransform: 'uppercase',
                  marginBottom: 8,
                }}
              >
                {categoryLabels[category] || category}
              </h3>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                {serverTypes.map((type) => (
                  <Card
                    key={type.id}
                    hoverable
                    onClick={() => setSelectedType(type.id)}
                    style={{
                      borderColor: selectedType === type.id
                        ? 'var(--md-primary)'
                        : 'var(--md-outline-variant)',
                      borderWidth: selectedType === type.id ? 2 : 1,
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                      <div
                        style={{
                          width: 40,
                          height: 40,
                          borderRadius: 'var(--md-radius-md)',
                          background: selectedType === type.id
                            ? 'var(--md-primary-container)'
                            : 'var(--md-surface-container-highest)',
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                          fontSize: 18,
                          color: selectedType === type.id
                            ? 'var(--md-on-primary-container)'
                            : 'var(--md-on-surface-variant)',
                          flexShrink: 0,
                        }}
                      >
                        {type.id === 'VANILLA' ? '\u26F0' :
                         type.id.includes('FORGE') ? '\u2699' :
                         type.id === 'VELOCITY' ? '\u2206' : '\u26A1'}
                      </div>
                      <div>
                        <div style={{ fontSize: 14, fontWeight: 500, color: 'var(--md-on-surface)' }}>
                          {type.name}
                        </div>
                        <div style={{ fontSize: 12, color: 'var(--md-on-surface-variant)', marginTop: 2 }}>
                          {type.description}
                        </div>
                      </div>
                    </div>
                  </Card>
                ))}
              </div>
            </div>
          ))}
        </div>

        {/* Download form */}
        <div>
          <Card style={{ position: 'sticky', top: 32 }}>
            <h3 style={{ fontSize: 18, fontWeight: 500, marginBottom: 16, color: 'var(--md-on-surface)' }}>
              Download Settings
            </h3>

            <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
              <div>
                <label style={{ fontSize: 12, color: 'var(--md-on-surface-variant)', marginBottom: 4, display: 'block' }}>
                  Selected Type
                </label>
                <div
                  style={{
                    padding: '8px 12px',
                    background: 'var(--md-surface-container-high)',
                    borderRadius: 'var(--md-radius-sm)',
                    fontSize: 14,
                    color: selectedType ? 'var(--md-on-surface)' : 'var(--md-on-surface-variant)',
                  }}
                >
                  {selectedType
                    ? types.find((t) => t.id === selectedType)?.name || selectedType
                    : 'None selected'}
                </div>
              </div>

              <TextField
                label="Minecraft Version"
                value={mcVersion}
                onChange={(e) => setMcVersion(e.target.value)}
                fullWidth
                helperText="e.g., 1.20.4, 1.21"
              />

              <TextField
                label="Server Name (optional)"
                value={serverName}
                onChange={(e) => setServerName(e.target.value)}
                fullWidth
                helperText="Leave blank for auto-generated name"
              />

              <Button
                onClick={handleDownload}
                disabled={downloading || !selectedType}
                style={{ width: '100%', marginTop: 8 }}
              >
                {downloading ? 'Downloading...' : 'Download & Create Server'}
              </Button>
            </div>
          </Card>
        </div>
      </div>
    </div>
  );
};

export default DownloadPage;
