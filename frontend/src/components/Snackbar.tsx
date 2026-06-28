import React from 'react';

interface SnackbarProps {
  message: string;
  type: 'info' | 'error' | 'success';
  onClose: () => void;
  duration?: number;
}

const bgMap = {
  info: '#333333',
  error: '#BA1A1A',
  success: '#2E7D32',
};

const Snackbar: React.FC<SnackbarProps> = ({ message, type, onClose, duration = 4000 }) => {
  React.useEffect(() => {
    const timer = setTimeout(onClose, duration);
    return () => clearTimeout(timer);
  }, [duration, onClose]);

  return (
    <div
      style={{
        position: 'fixed',
        bottom: 24,
        left: '50%',
        transform: 'translateX(-50%)',
        background: bgMap[type],
        color: '#FFFFFF',
        padding: '12px 24px',
        borderRadius: 'var(--md-radius-sm)',
        fontSize: 14,
        fontWeight: 500,
        boxShadow: 'var(--md-shadow-3)',
        zIndex: 2000,
        display: 'flex',
        alignItems: 'center',
        gap: 16,
        animation: 'slideUp 0.3s ease',
        maxWidth: 'calc(100vw - 48px)',
      }}
    >
      <span>{message}</span>
      <button
        onClick={onClose}
        style={{
          background: 'none',
          border: 'none',
          color: 'white',
          cursor: 'pointer',
          fontSize: 16,
          padding: 4,
          opacity: 0.8,
        }}
      >
        \u2715
      </button>
    </div>
  );
};

export default Snackbar;
