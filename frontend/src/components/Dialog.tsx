import React from 'react';
import Button from './Button';

interface DialogProps {
  open: boolean;
  onClose: () => void;
  title: string;
  children: React.ReactNode;
  actions?: React.ReactNode;
  maxWidth?: number;
}

const Dialog: React.FC<DialogProps> = ({
  open,
  onClose,
  title,
  children,
  actions,
  maxWidth = 480,
}) => {
  if (!open) return null;

  return (
    <>
      {/* Backdrop */}
      <div
        onClick={onClose}
        style={{
          position: 'fixed',
          inset: 0,
          background: 'rgba(0,0,0,0.5)',
          zIndex: 1000,
          animation: 'fadeIn 0.2s ease',
        }}
      />
      {/* Dialog */}
      <div
        style={{
          position: 'fixed',
          top: '50%',
          left: '50%',
          transform: 'translate(-50%, -50%)',
          background: 'var(--md-surface-container-high)',
          borderRadius: 'var(--md-radius-xl)',
          padding: 24,
          width: `calc(100% - 48px)`,
          maxWidth,
          zIndex: 1001,
          boxShadow: 'var(--md-shadow-5)',
          animation: 'scaleIn 0.2s ease',
        }}
      >
        <h2
          style={{
            fontSize: 22,
            fontWeight: 500,
            color: 'var(--md-on-surface)',
            marginBottom: 16,
          }}
        >
          {title}
        </h2>
        <div>{children}</div>
        {actions && (
          <div
            style={{
              display: 'flex',
              justifyContent: 'flex-end',
              gap: 8,
              marginTop: 24,
            }}
          >
            {actions}
          </div>
        )}
      </div>
    </>
  );
};

export default Dialog;
