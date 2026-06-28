import { create } from 'zustand';

interface UIState {
  theme: 'light' | 'dark';
  snackbar: { message: string; type: 'info' | 'error' | 'success' } | null;
  sidebarCollapsed: boolean;

  toggleTheme: () => void;
  showSnackbar: (message: string, type?: 'info' | 'error' | 'success') => void;
  clearSnackbar: () => void;
  toggleSidebar: () => void;
}

export const useUIStore = create<UIState>((set) => ({
  theme: (localStorage.getItem('jmcl-theme') as 'light' | 'dark') || 'dark',
  snackbar: null,
  sidebarCollapsed: false,

  toggleTheme: () => set((s) => {
    const next = s.theme === 'light' ? 'dark' : 'light';
    localStorage.setItem('jmcl-theme', next);
    return { theme: next };
  }),

  showSnackbar: (message, type = 'info') => set({ snackbar: { message, type } }),
  clearSnackbar: () => set({ snackbar: null }),

  toggleSidebar: () => set((s) => ({ sidebarCollapsed: !s.sidebarCollapsed })),
}));
