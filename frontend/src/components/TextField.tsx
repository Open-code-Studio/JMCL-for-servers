import React from 'react';

interface TextFieldProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  helperText?: string;
  error?: string;
  fullWidth?: boolean;
  type?: string;
}

const TextField: React.FC<TextFieldProps> = ({
  label,
  helperText,
  error,
  fullWidth,
  style,
  id,
  value,
  ...props
}) => {
  const inputId = id || label?.replace(/\s+/g, '-').toLowerCase();
  const hasValue = value !== undefined && value !== '';

  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        gap: 4,
        width: fullWidth ? '100%' : undefined,
        ...style,
      }}
    >
      <div style={{ position: 'relative' }}>
        <input
          id={inputId}
          value={value}
          style={{
            width: '100%',
            height: 56,
            padding: '24px 16px 8px 16px',
            border: `1px solid ${error ? 'var(--md-error)' : 'var(--md-outline)'}`,
            borderRadius: '4px 4px 0 0',
            background: 'var(--md-surface-container-highest)',
            color: 'var(--md-on-surface)',
            fontSize: 16,
            outline: 'none',
            transition: 'border-color 0.2s ease',
            fontFamily: 'inherit',
          }}
          placeholder=" "
          {...props}
        />
        {label && (
          <label
            htmlFor={inputId}
            style={{
              position: 'absolute',
              left: 16,
              top: hasValue ? 8 : 18,
              fontSize: hasValue ? 12 : 16,
              color: error ? 'var(--md-error)' : 'var(--md-on-surface-variant)',
              pointerEvents: 'none',
              transition: 'all 0.2s ease',
            }}
          >
            {label}
          </label>
        )}
      </div>
      {(helperText || error) && (
        <span
          style={{
            fontSize: 12,
            color: error ? 'var(--md-error)' : 'var(--md-on-surface-variant)',
            padding: '0 16px',
          }}
        >
          {error || helperText}
        </span>
      )}
    </div>
  );
};

export default TextField;
