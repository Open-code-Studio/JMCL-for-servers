import React from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useUIStore } from '../stores/uiStore';

interface NavItem {
  path: string;
  label: string;
  icon: string;
}

const navItems: NavItem[] = [
  { path: '/', label: 'Dashboard', icon: 'dashboard' },
  { path: '/servers', label: 'Servers', icon: 'dns' },
  { path: '/download', label: 'Download', icon: 'download' },
];

const icons: Record<string, string> = {
  dashboard: '\u2316',
  dns: '\u22A1',
  download: '\u21E9',
};

const Sidebar: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const collapsed = useUIStore((s) => s.sidebarCollapsed);
  const toggleCollapsed = useUIStore((s) => s.toggleSidebar);
  const theme = useUIStore((s) => s.theme);
  const toggleTheme = useUIStore((s) => s.toggleTheme);

  return (
    <nav
      style={{
        width: collapsed ? 64 : 240,
        minWidth: collapsed ? 64 : 240,
        height: '100vh',
        background: 'var(--md-surface-container-low)',
        display: 'flex',
        flexDirection: 'column',
        transition: 'width 0.2s ease',
        borderRight: '1px solid var(--md-outline-variant)',
        userSelect: 'none',
      }}
    >
      {/* Header */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          padding: '0 16px',
          height: 64,
          gap: 12,
          cursor: 'pointer',
          borderBottom: '1px solid var(--md-outline-variant)',
        }}
        onClick={toggleCollapsed}
      >
        <div
          style={{
            width: 36,
            height: 36,
            borderRadius: 'var(--md-radius-md)',
            background: 'var(--md-primary)',
            color: 'var(--md-on-primary)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontSize: 18,
            fontWeight: 700,
            flexShrink: 0,
          }}
        >
          M
        </div>
        {!collapsed && (
          <span
            style={{
              fontSize: 16,
              fontWeight: 600,
              color: 'var(--md-on-surface)',
              whiteSpace: 'nowrap',
              overflow: 'hidden',
            }}
          >
            JMCL Server
          </span>
        )}
      </div>

      {/* Nav items */}
      <div style={{ flex: 1, padding: '8px 0' }}>
        {navItems.map((item) => {
          const active = location.pathname === item.path ||
            (item.path !== '/' && location.pathname.startsWith(item.path));
          return (
            <div
              key={item.path}
              onClick={() => navigate(item.path)}
              style={{
                display: 'flex',
                alignItems: 'center',
                padding: '0 16px',
                height: 48,
                gap: 12,
                cursor: 'pointer',
                background: active ? 'var(--md-secondary-container)' : 'transparent',
                color: active ? 'var(--md-on-secondary-container)' : 'var(--md-on-surface-variant)',
                borderRadius: collapsed ? '0 var(--md-radius-xl) var(--md-radius-xl) 0' : '0 var(--md-radius-xl) var(--md-radius-xl) 0',
                margin: '2px 8px',
                transition: 'background 0.15s ease',
                position: 'relative',
              }}
            >
              <span style={{ fontSize: 20, flexShrink: 0, width: 24, textAlign: 'center' }}>
                {icons[item.icon] || '\u25CB'}
              </span>
              {!collapsed && (
                <span style={{ fontWeight: 500, whiteSpace: 'nowrap' }}>
                  {item.label}
                </span>
              )}
              {active && (
                <div
                  style={{
                    position: 'absolute',
                    left: 0,
                    top: 8,
                    bottom: 8,
                    width: 3,
                    borderRadius: '0 3px 3px 0',
                    background: 'var(--md-primary)',
                  }}
                />
              )}
            </div>
          );
        })}
      </div>

      {/* Footer - Theme toggle */}
      <div
        style={{
          padding: collapsed ? '8px' : '8px 16px',
          borderTop: '1px solid var(--md-outline-variant)',
          display: 'flex',
          alignItems: 'center',
          gap: 12,
          cursor: 'pointer',
        }}
        onClick={toggleTheme}
      >
        <span style={{ fontSize: 20, flexShrink: 0 }}>
          {theme === 'dark' ? '\u2600' : '\u263D'}
        </span>
        {!collapsed && (
          <span style={{ fontSize: 13, color: 'var(--md-on-surface-variant)' }}>
            {theme === 'dark' ? 'Light Mode' : 'Dark Mode'}
          </span>
        )}
      </div>
    </nav>
  );
};

export default Sidebar;
