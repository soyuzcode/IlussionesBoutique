import { useState } from 'react';
import {
  applyTheme,
  loadTheme,
  saveTheme,
  type ThemeAccent,
  type ThemeMode,
  type ThemePreferences,
} from '../../lib/theme';

const MODE_OPTIONS: { id: ThemeMode; label: string; icon: string }[] = [
  { id: 'light', label: 'Claro', icon: '☀️' },
  { id: 'dark', label: 'Oscuro', icon: '🌙' },
  { id: 'system', label: 'Sistema', icon: '💻' },
];

const ACCENT_OPTIONS: { id: ThemeAccent; label: string; swatch: string }[] = [
  { id: 'pink', label: 'Rosa', swatch: '#db2777' },
  { id: 'violet', label: 'Violeta', swatch: '#7c3aed' },
  { id: 'emerald', label: 'Esmeralda', swatch: '#059669' },
  { id: 'blue', label: 'Azul', swatch: '#2563eb' },
];

const optionClass = (active: boolean) =>
  `flex-1 min-w-[90px] py-2.5 px-3 rounded-xl border text-xs font-bold cursor-pointer transition flex items-center justify-center gap-1.5 ${
    active
      ? 'border-pink-600 bg-pink-50 text-pink-700'
      : 'border-gray-200 bg-white text-gray-600 hover:border-pink-400'
  }`;

export function ThemeSettings() {
  const [prefs, setPrefs] = useState<ThemePreferences>(loadTheme);

  const update = (patch: Partial<ThemePreferences>) => {
    const next = { ...prefs, ...patch };
    setPrefs(next);
    saveTheme(next);
    applyTheme(next);
  };

  return (
    <section className="bg-white border border-gray-100 rounded-2xl p-4 sm:p-6 shadow-xs space-y-5">
      <div>
        <h2 className="text-base sm:text-lg font-bold text-gray-800">Apariencia</h2>
        <p className="text-xs text-gray-500 mt-0.5">Se guarda en este dispositivo.</p>
      </div>

      <fieldset className="space-y-2">
        <legend className="text-[10px] uppercase font-bold text-gray-400 mb-2">Modo</legend>
        <div className="flex flex-wrap gap-2">
          {MODE_OPTIONS.map((option) => (
            <button
              key={option.id}
              type="button"
              aria-pressed={prefs.mode === option.id}
              onClick={() => update({ mode: option.id })}
              className={optionClass(prefs.mode === option.id)}
            >
              <span aria-hidden="true">{option.icon}</span>
              {option.label}
            </button>
          ))}
        </div>
      </fieldset>

      <fieldset className="space-y-2">
        <legend className="text-[10px] uppercase font-bold text-gray-400 mb-2">Color</legend>
        <div className="flex flex-wrap gap-2">
          {ACCENT_OPTIONS.map((option) => (
            <button
              key={option.id}
              type="button"
              aria-pressed={prefs.accent === option.id}
              onClick={() => update({ accent: option.id })}
              className={optionClass(prefs.accent === option.id)}
            >
              <span aria-hidden="true" className="w-3.5 h-3.5 rounded-full" style={{ backgroundColor: option.swatch }} />
              {option.label}
            </button>
          ))}
        </div>
      </fieldset>

      <label className="flex items-center justify-between gap-3 p-3 rounded-xl border border-gray-200 cursor-pointer">
        <span>
          <span className="block text-xs font-bold text-gray-800">Alto contraste</span>
          <span className="block text-[11px] text-gray-500">Texto y bordes más marcados para mejorar la legibilidad.</span>
        </span>
        <input
          type="checkbox"
          checked={prefs.highContrast}
          onChange={(event) => update({ highContrast: event.target.checked })}
          className="w-5 h-5 accent-pink-600 cursor-pointer"
        />
      </label>
    </section>
  );
}

export default ThemeSettings;
