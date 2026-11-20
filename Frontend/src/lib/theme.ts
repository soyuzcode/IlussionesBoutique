export type ThemeMode = 'light' | 'dark' | 'system';
export type ThemeAccent = 'pink' | 'violet' | 'emerald' | 'blue';

export interface ThemePreferences {
  mode: ThemeMode;
  accent: ThemeAccent;
  highContrast: boolean;
}

// Keep the key and validation in sync with the pre-paint script in index.html.
export const THEME_STORAGE_KEY = 'ilussiones-theme';
export const THEME_MODES: ThemeMode[] = ['light', 'dark', 'system'];
export const THEME_ACCENTS: ThemeAccent[] = ['pink', 'violet', 'emerald', 'blue'];
export const DEFAULT_THEME: ThemePreferences = { mode: 'system', accent: 'pink', highContrast: false };

const DARK_QUERY = '(prefers-color-scheme: dark)';

export function parseTheme(raw: string | null): ThemePreferences {
  try {
    const value = JSON.parse(raw ?? '{}') as Partial<ThemePreferences>;
    return {
      mode: THEME_MODES.includes(value.mode as ThemeMode) ? (value.mode as ThemeMode) : DEFAULT_THEME.mode,
      accent: THEME_ACCENTS.includes(value.accent as ThemeAccent) ? (value.accent as ThemeAccent) : DEFAULT_THEME.accent,
      highContrast: value.highContrast === true,
    };
  } catch {
    return DEFAULT_THEME;
  }
}

export function loadTheme(): ThemePreferences {
  try {
    return parseTheme(localStorage.getItem(THEME_STORAGE_KEY));
  } catch {
    return DEFAULT_THEME;
  }
}

export function saveTheme(prefs: ThemePreferences) {
  try {
    localStorage.setItem(THEME_STORAGE_KEY, JSON.stringify(prefs));
  } catch {
    // Storage may be unavailable (private mode); the theme still applies for this visit.
  }
}

const systemPrefersDark = () => typeof window.matchMedia === 'function' && window.matchMedia(DARK_QUERY).matches;

export function resolveMode(mode: ThemeMode, prefersDark: boolean): 'light' | 'dark' {
  if (mode === 'system') return prefersDark ? 'dark' : 'light';
  return mode;
}

export function applyTheme(prefs: ThemePreferences, prefersDark = systemPrefersDark()) {
  const root = document.documentElement;
  const mode = resolveMode(prefs.mode, prefersDark);
  root.dataset.mode = mode;
  root.dataset.accent = prefs.accent;
  root.dataset.contrast = prefs.highContrast ? 'high' : 'normal';
  root.style.colorScheme = mode;
}

/** Applies the stored theme and follows OS changes while the mode is "system". */
export function initTheme() {
  applyTheme(loadTheme());
  if (typeof window.matchMedia !== 'function') return;
  window.matchMedia(DARK_QUERY).addEventListener('change', (event) => {
    const prefs = loadTheme();
    if (prefs.mode === 'system') applyTheme(prefs, event.matches);
  });
}
