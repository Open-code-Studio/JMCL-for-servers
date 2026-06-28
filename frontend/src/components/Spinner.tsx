import React from 'react';

interface SpinnerProps {
  size?: number;
  color?: string;
}

const Spinner: React.FC<SpinnerProps> = ({ size = 24, color = 'var(--md-primary)' }) => {
  return (
    <div
      style={{
        width: size,
        height: size,
        border: `3px solid var(--md-surface-container-highest)`,
        borderTopColor: color,
        borderRadius: '50%',
        animation: 'spin 0.8s linear infinite',
      }}
    />
  );
};

export default Spinner;
