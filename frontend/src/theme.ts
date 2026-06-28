// Material Design 3 Theme System
export interface ThemeColors {
  primary: string;
  onPrimary: string;
  primaryContainer: string;
  onPrimaryContainer: string;
  secondary: string;
  onSecondary: string;
  secondaryContainer: string;
  onSecondaryContainer: string;
  tertiary: string;
  onTertiary: string;
  tertiaryContainer: string;
  onTertiaryContainer: string;
  error: string;
  onError: string;
  errorContainer: string;
  onErrorContainer: string;
  background: string;
  onBackground: string;
  surface: string;
  onSurface: string;
  surfaceVariant: string;
  onSurfaceVariant: string;
  outline: string;
  outlineVariant: string;
  surfaceContainerLowest: string;
  surfaceContainerLow: string;
  surfaceContainer: string;
  surfaceContainerHigh: string;
  surfaceContainerHighest: string;
}

export interface ThemeTokens {
  colors: ThemeColors;
  elevation: {
    1: string;
    2: string;
    3: string;
    4: string;
    5: string;
  };
  shape: {
    xs: string;
    sm: string;
    md: string;
    lg: string;
    xl: string;
    full: string;
  };
  state: {
    hover: string;
    focus: string;
    pressed: string;
    dragged: string;
  };
  typography: {
    displayLarge: React.CSSProperties;
    displayMedium: React.CSSProperties;
    displaySmall: React.CSSProperties;
    headlineLarge: React.CSSProperties;
    headlineMedium: React.CSSProperties;
    headlineSmall: React.CSSProperties;
    titleLarge: React.CSSProperties;
    titleMedium: React.CSSProperties;
    titleSmall: React.CSSProperties;
    bodyLarge: React.CSSProperties;
    bodyMedium: React.CSSProperties;
    bodySmall: React.CSSProperties;
    labelLarge: React.CSSProperties;
    labelMedium: React.CSSProperties;
    labelSmall: React.CSSProperties;
  };
}

export const lightTheme: ThemeTokens = {
  colors: {
    primary: '#6750A4',
    onPrimary: '#FFFFFF',
    primaryContainer: '#EADDFF',
    onPrimaryContainer: '#21005D',
    secondary: '#625B71',
    onSecondary: '#FFFFFF',
    secondaryContainer: '#E8DEF8',
    onSecondaryContainer: '#1D192B',
    tertiary: '#7D5260',
    onTertiary: '#FFFFFF',
    tertiaryContainer: '#FFD8E4',
    onTertiaryContainer: '#31111D',
    error: '#BA1A1A',
    onError: '#FFFFFF',
    errorContainer: '#FFDAD6',
    onErrorContainer: '#410002',
    background: '#FEF7FF',
    onBackground: '#1D1B20',
    surface: '#FEF7FF',
    onSurface: '#1D1B20',
    surfaceVariant: '#E7E0EC',
    onSurfaceVariant: '#49454F',
    outline: '#79747E',
    outlineVariant: '#CAC4D0',
    surfaceContainerLowest: '#FFFFFF',
    surfaceContainerLow: '#F7F2FA',
    surfaceContainer: '#F3EDF7',
    surfaceContainerHigh: '#ECE6F0',
    surfaceContainerHighest: '#E6E0E9',
  },
  elevation: {
    1: '0 1px 2px 0 rgba(0,0,0,0.3), 0 1px 3px 1px rgba(0,0,0,0.15)',
    2: '0 1px 2px 0 rgba(0,0,0,0.3), 0 2px 6px 2px rgba(0,0,0,0.15)',
    3: '0 1px 3px 0 rgba(0,0,0,0.3), 0 4px 8px 3px rgba(0,0,0,0.15)',
    4: '0 2px 3px 0 rgba(0,0,0,0.3), 0 6px 10px 4px rgba(0,0,0,0.15)',
    5: '0 4px 4px 0 rgba(0,0,0,0.3), 0 8px 12px 6px rgba(0,0,0,0.15)',
  },
  shape: {
    xs: '4px',
    sm: '8px',
    md: '12px',
    lg: '16px',
    xl: '28px',
    full: '999px',
  },
  state: {
    hover: 'rgba(0,0,0,0.08)',
    focus: 'rgba(0,0,0,0.12)',
    pressed: 'rgba(0,0,0,0.12)',
    dragged: 'rgba(0,0,0,0.16)',
  },
  typography: {
    displayLarge: { fontFamily: 'Inter, sans-serif', fontSize: '57px', fontWeight: 400, lineHeight: '64px', letterSpacing: '-0.25px' },
    displayMedium: { fontFamily: 'Inter, sans-serif', fontSize: '45px', fontWeight: 400, lineHeight: '52px', letterSpacing: '0' },
    displaySmall: { fontFamily: 'Inter, sans-serif', fontSize: '36px', fontWeight: 400, lineHeight: '44px', letterSpacing: '0' },
    headlineLarge: { fontFamily: 'Inter, sans-serif', fontSize: '32px', fontWeight: 400, lineHeight: '40px', letterSpacing: '0' },
    headlineMedium: { fontFamily: 'Inter, sans-serif', fontSize: '28px', fontWeight: 400, lineHeight: '36px', letterSpacing: '0' },
    headlineSmall: { fontFamily: 'Inter, sans-serif', fontSize: '24px', fontWeight: 400, lineHeight: '32px', letterSpacing: '0' },
    titleLarge: { fontFamily: 'Inter, sans-serif', fontSize: '22px', fontWeight: 400, lineHeight: '28px', letterSpacing: '0' },
    titleMedium: { fontFamily: 'Inter, sans-serif', fontSize: '16px', fontWeight: 500, lineHeight: '24px', letterSpacing: '0.15px' },
    titleSmall: { fontFamily: 'Inter, sans-serif', fontSize: '14px', fontWeight: 500, lineHeight: '20px', letterSpacing: '0.1px' },
    bodyLarge: { fontFamily: 'Inter, sans-serif', fontSize: '16px', fontWeight: 400, lineHeight: '24px', letterSpacing: '0.5px' },
    bodyMedium: { fontFamily: 'Inter, sans-serif', fontSize: '14px', fontWeight: 400, lineHeight: '20px', letterSpacing: '0.25px' },
    bodySmall: { fontFamily: 'Inter, sans-serif', fontSize: '12px', fontWeight: 400, lineHeight: '16px', letterSpacing: '0.4px' },
    labelLarge: { fontFamily: 'Inter, sans-serif', fontSize: '14px', fontWeight: 500, lineHeight: '20px', letterSpacing: '0.1px' },
    labelMedium: { fontFamily: 'Inter, sans-serif', fontSize: '12px', fontWeight: 500, lineHeight: '16px', letterSpacing: '0.5px' },
    labelSmall: { fontFamily: 'Inter, sans-serif', fontSize: '11px', fontWeight: 500, lineHeight: '16px', letterSpacing: '0.5px' },
  },
};

export const darkTheme: ThemeTokens = {
  colors: {
    primary: '#D0BCFF',
    onPrimary: '#381E72',
    primaryContainer: '#4F378B',
    onPrimaryContainer: '#EADDFF',
    secondary: '#CCC2DC',
    onSecondary: '#332D41',
    secondaryContainer: '#4A4458',
    onSecondaryContainer: '#E8DEF8',
    tertiary: '#EFB8C8',
    onTertiary: '#492532',
    tertiaryContainer: '#633B48',
    onTertiaryContainer: '#FFD8E4',
    error: '#FFB4AB',
    onError: '#690005',
    errorContainer: '#93000A',
    onErrorContainer: '#FFDAD6',
    background: '#141218',
    onBackground: '#E6E0E9',
    surface: '#141218',
    onSurface: '#E6E0E9',
    surfaceVariant: '#49454F',
    onSurfaceVariant: '#CAC4D0',
    outline: '#938F99',
    outlineVariant: '#49454F',
    surfaceContainerLowest: '#0F0D13',
    surfaceContainerLow: '#1D1B20',
    surfaceContainer: '#211F26',
    surfaceContainerHigh: '#2B2930',
    surfaceContainerHighest: '#36343B',
  },
  elevation: {
    1: '0 1px 2px 0 rgba(0,0,0,0.3), 0 1px 3px 1px rgba(0,0,0,0.15)',
    2: '0 1px 2px 0 rgba(0,0,0,0.3), 0 2px 6px 2px rgba(0,0,0,0.15)',
    3: '0 1px 3px 0 rgba(0,0,0,0.3), 0 4px 8px 3px rgba(0,0,0,0.15)',
    4: '0 2px 3px 0 rgba(0,0,0,0.3), 0 6px 10px 4px rgba(0,0,0,0.15)',
    5: '0 4px 4px 0 rgba(0,0,0,0.3), 0 8px 12px 6px rgba(0,0,0,0.15)',
  },
  shape: {
    xs: '4px',
    sm: '8px',
    md: '12px',
    lg: '16px',
    xl: '28px',
    full: '999px',
  },
  state: {
    hover: 'rgba(255,255,255,0.08)',
    focus: 'rgba(255,255,255,0.12)',
    pressed: 'rgba(255,255,255,0.12)',
    dragged: 'rgba(255,255,255,0.16)',
  },
  typography: lightTheme.typography,
};
