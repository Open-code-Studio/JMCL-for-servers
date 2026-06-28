import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { useUIStore } from './stores/uiStore';
import { lightTheme, darkTheme } from './theme';
import Sidebar from './components/Sidebar';
import Snackbar from './components/Snackbar';
import DashboardPage from './pages/DashboardPage';
import ServerListPage from './pages/ServerListPage';
import ServerConfigPage from './pages/ServerConfigPage';
import DownloadPage from './pages/DownloadPage';
import LogsPage from './pages/LogsPage';

function applyTheme(theme: typeof lightTheme) {
  const root = document.documentElement;
  const { colors } = theme;
  root.style.setProperty('--md-primary', colors.primary);
  root.style.setProperty('--md-on-primary', colors.onPrimary);
  root.style.setProperty('--md-primary-container', colors.primaryContainer);
  root.style.setProperty('--md-on-primary-container', colors.onPrimaryContainer);
  root.style.setProperty('--md-secondary', colors.secondary);
  root.style.setProperty('--md-on-secondary', colors.onSecondary);
  root.style.setProperty('--md-secondary-container', colors.secondaryContainer);
  root.style.setProperty('--md-on-secondary-container', colors.onSecondaryContainer);
  root.style.setProperty('--md-tertiary', colors.tertiary);
  root.style.setProperty('--md-error', colors.error);
  root.style.setProperty('--md-on-error', colors.onError);
  root.style.setProperty('--md-error-container', colors.errorContainer);
  root.style.setProperty('--md-bg', colors.background);
  root.style.setProperty('--md-on-bg', colors.onBackground);
  root.style.setProperty('--md-surface', colors.surface);
  root.style.setProperty('--md-on-surface', colors.onSurface);
  root.style.setProperty('--md-surface-variant', colors.surfaceVariant);
  root.style.setProperty('--md-on-surface-variant', colors.onSurfaceVariant);
  root.style.setProperty('--md-outline', colors.outline);
  root.style.setProperty('--md-outline-variant', colors.outlineVariant);
  root.style.setProperty('--md-surface-container', colors.surfaceContainer);
  root.style.setProperty('--md-surface-container-low', colors.surfaceContainerLow);
  root.style.setProperty('--md-surface-container-high', colors.surfaceContainerHigh);
  root.style.setProperty('--md-surface-container-highest', colors.surfaceContainerHighest);
  root.style.setProperty('--md-shadow-1', theme.elevation['1']);
  root.style.setProperty('--md-radius-sm', theme.shape.sm);
  root.style.setProperty('--md-radius-md', theme.shape.md);
  root.style.setProperty('--md-radius-lg', theme.shape.lg);
  root.style.setProperty('--md-radius-xl', theme.shape.xl);
}

const App: React.FC = () => {
  const theme = useUIStore((s) => s.theme);
  const snackbar = useUIStore((s) => s.snackbar);
  const clearSnackbar = useUIStore((s) => s.clearSnackbar);

  React.useEffect(() => {
    applyTheme(theme === 'dark' ? darkTheme : lightTheme);
  }, [theme]);

  return (
    <div style={{ display: 'flex', height: '100vh', width: '100vw' }}>
      <Sidebar />
      <main style={{ flex: 1, overflow: 'auto', padding: 0 }}>
        <Routes>
          <Route path="/" element={<DashboardPage />} />
          <Route path="/servers" element={<ServerListPage />} />
          <Route path="/servers/:id/config" element={<ServerConfigPage />} />
          <Route path="/download" element={<DownloadPage />} />
          <Route path="/logs/:id" element={<LogsPage />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
      {snackbar && (
        <Snackbar
          message={snackbar.message}
          type={snackbar.type}
          onClose={clearSnackbar}
        />
      )}
    </div>
  );
};

export default App;
