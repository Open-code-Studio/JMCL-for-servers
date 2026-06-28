import React from 'react';

type ButtonVariant = 'filled' | 'tonal' | 'outlined' | 'text';
type ButtonColor = 'primary' | 'secondary' | 'error';

interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant;
  color?: ButtonColor;
  icon?: string;
}

const colorMap: Record<ButtonColor, Record<string, string>> = {
  primary: {
    bg: 'var(--md-primary)',
    fg: 'var(--md-on-primary)',
    tonalBg: 'var(--md-primary-container)',
    tonalFg: 'var(--md-on-primary-container)',
    border: 'var(--md-outline)',
  },
  secondary: {
    bg: 'var(--md-secondary)',
    fg: 'var(--md-on-secondary)',
    tonalBg: 'var(--md-secondary-container)',
    tonalFg: 'var(--md-on-secondary-container)',
    border: 'var(--md-outline)',
  },
  error: {
    bg: 'var(--md-error)',
    fg: 'var(--md-on-error)',
    tonalBg: 'var(--md-error-container)',
    tonalFg: 'var(--md-on-error-container)',
    border: 'var(--md-error)',
  },
};

const Button: React.FC<ButtonProps> = ({
  variant = 'filled',
  color = 'primary',
  icon,
  children,
  style,
  disabled,
  ...props
}) => {
  const c = colorMap[color];
  const styles: React.CSSProperties = {
    display: 'inline-flex',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
    height: 40,
    padding: '0 24px',
    border: 'none',
    borderRadius: 'var(--md-radius-xl)',
    fontSize: 14,
    fontWeight: 500,
    cursor: disabled ? 'not-allowed' : 'pointer',
    opacity: disabled ? 0.6 : 1,
    transition: 'all 0.15s ease',
    position: 'relative',
    overflow: 'hidden',
    ...style,
  };

  switch (variant) {
    case 'filled':
      styles.background = c.bg;
      styles.color = c.fg;
      styles.boxShadow = 'var(--md-shadow-1)';
      break;
    case 'tonal':
      styles.background = c.tonalBg;
      styles.color = c.tonalFg;
      break;
    case 'outlined':
      styles.background = 'transparent';
      styles.color = c.bg;
      styles.border = `1px solid ${c.border}`;
      break;
    case 'text':
      styles.background = 'transparent';
      styles.color = c.bg;
      styles.padding = '0 12px';
      break;
  }

  return (
    <button style={styles} disabled={disabled} {...props}>
      {icon && <span style={{ fontSize: 18 }}>{icon}</span>}
      {children}
    </button>
  );
};

export default Button;
