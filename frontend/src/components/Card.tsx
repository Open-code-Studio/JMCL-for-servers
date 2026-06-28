import React from 'react';

interface CardProps {
  children: React.ReactNode;
  style?: React.CSSProperties;
  onClick?: () => void;
  hoverable?: boolean;
}

const Card: React.FC<CardProps> = ({ children, style, onClick, hoverable }) => {
  return (
    <div
      onClick={onClick}
      style={{
        background: 'var(--md-surface-container)',
        borderRadius: 'var(--md-radius-md)',
        padding: 16,
        boxShadow: hoverable ? 'var(--md-shadow-1)' : 'none',
        border: '1px solid var(--md-outline-variant)',
        cursor: onClick ? 'pointer' : undefined,
        transition: 'box-shadow 0.15s ease, border-color 0.15s ease',
        ...style,
      }}
      onMouseEnter={(e) => {
        if (hoverable) {
          e.currentTarget.style.boxShadow = 'var(--md-shadow-2)';
          e.currentTarget.style.borderColor = 'var(--md-outline)';
        }
      }}
      onMouseLeave={(e) => {
        if (hoverable) {
          e.currentTarget.style.boxShadow = 'none';
          e.currentTarget.style.borderColor = 'var(--md-outline-variant)';
        }
      }}
    >
      {children}
    </div>
  );
};

export default Card;
