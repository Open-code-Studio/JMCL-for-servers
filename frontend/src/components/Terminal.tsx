import React, { useRef, useEffect } from 'react';

interface TerminalProps {
  lines: Array<{ message: string; level: string }>;
  maxHeight?: number;
  autoScroll?: boolean;
}

const Terminal: React.FC<TerminalProps> = ({ lines, maxHeight = 400, autoScroll = true }) => {
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (autoScroll && ref.current) {
      ref.current.scrollTop = ref.current.scrollHeight;
    }
  }, [lines, autoScroll]);

  const getColor = (level: string) => {
    switch (level.toUpperCase()) {
      case 'ERROR':
      case 'FATAL':
        return '#EF4444';
      case 'WARN':
        return '#F59E0B';
      case 'INFO':
        return '#8B5CF6';
      default:
        return 'var(--md-on-surface)';
    }
  };

  return (
    <div
      ref={ref}
      style={{
        background: '#0D1117',
        color: '#C9D1D9',
        fontFamily: "'JetBrains Mono', 'Fira Code', monospace",
        fontSize: 12,
        lineHeight: 1.6,
        padding: '12px 16px',
        borderRadius: 'var(--md-radius-md)',
        overflow: 'auto',
        maxHeight,
        whiteSpace: 'pre-wrap',
        wordBreak: 'break-all',
      }}
    >
      {lines.length === 0 ? (
        <span style={{ color: '#6E7681', fontStyle: 'italic' }}>
          Server output will appear here...
        </span>
      ) : (
        lines.map((line, i) => (
          <div key={i} style={{ display: 'flex', gap: 8 }}>
            <span style={{ color: '#6E7681', flexShrink: 0 }}>
              {i + 1}
            </span>
            <span style={{ color: getColor(line.level) }}>
              {line.message}
            </span>
          </div>
        ))
      )}
    </div>
  );
};

export default Terminal;
