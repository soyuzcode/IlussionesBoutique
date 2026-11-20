import { describe, expect, it } from 'vitest';
import { applyTheme, DEFAULT_THEME, parseTheme, resolveMode } from '../src/lib/theme';

describe('theme preferences', () => {
  it('falls back to defaults for missing or invalid values', () => {
    expect(parseTheme(null)).toEqual(DEFAULT_THEME);
    expect(parseTheme('not json')).toEqual(DEFAULT_THEME);
    expect(parseTheme('{"mode":"neon","accent":"gold","highContrast":"yes"}')).toEqual(DEFAULT_THEME);
  });

  it('keeps valid stored values', () => {
    expect(parseTheme('{"mode":"dark","accent":"emerald","highContrast":true}')).toEqual({
      mode: 'dark',
      accent: 'emerald',
      highContrast: true,
    });
  });

  it('resolves the system mode from the OS preference', () => {
    expect(resolveMode('system', true)).toBe('dark');
    expect(resolveMode('system', false)).toBe('light');
    expect(resolveMode('light', true)).toBe('light');
  });

  it('writes the theme to the root element', () => {
    applyTheme({ mode: 'system', accent: 'violet', highContrast: true }, true);
    const root = document.documentElement;
    expect(root.dataset.mode).toBe('dark');
    expect(root.dataset.accent).toBe('violet');
    expect(root.dataset.contrast).toBe('high');
    expect(root.style.colorScheme).toBe('dark');
  });
});
