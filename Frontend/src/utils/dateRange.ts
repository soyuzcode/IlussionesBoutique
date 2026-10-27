import { formatDateOnly } from './dates';

export type RangePreset = 'all' | 'today' | 'week' | 'month' | 'custom';
export type FilterBy = 'registered' | 'appointments';

export interface DateRange {
  start: string; // "YYYY-MM-DD", inclusivo
  end: string;   // "YYYY-MM-DD", inclusivo
}

const toISO = (date: Date) => date.toLocaleDateString('sv-SE');

// Convierte una fecha con hora del backend a la fecha local "YYYY-MM-DD"
export function toLocalDate(iso?: string | null): string | null {
  if (!iso) return null;
  const date = new Date(iso);
  return Number.isNaN(date.getTime()) ? null : toISO(date);
}

function getPresetRange(preset: 'today' | 'week' | 'month'): DateRange {
  const now = new Date();

  if (preset === 'today') {
    const today = toISO(now);
    return { start: today, end: today };
  }

  if (preset === 'week') {
    const daysSinceMonday = (now.getDay() + 6) % 7; // lunes = 0
    const monday = new Date(now.getFullYear(), now.getMonth(), now.getDate() - daysSinceMonday);
    const sunday = new Date(now.getFullYear(), now.getMonth(), now.getDate() - daysSinceMonday + 6);
    return { start: toISO(monday), end: toISO(sunday) };
  }

  const firstDay = new Date(now.getFullYear(), now.getMonth(), 1);
  const lastDay = new Date(now.getFullYear(), now.getMonth() + 1, 0);
  return { start: toISO(firstDay), end: toISO(lastDay) };
}

// Devuelve el rango a aplicar (o null si no hay filtro) y un error si el rango personalizado es inválido
export function resolveRange(
  preset: RangePreset,
  from: string,
  to: string,
): { range: DateRange | null; error: string } {
  if (preset === 'all') return { range: null, error: '' };
  if (preset !== 'custom') return { range: getPresetRange(preset), error: '' };

  if (!from && !to) return { range: null, error: '' };
  if (from && to && from > to) {
    return { range: null, error: 'La fecha inicial no puede ser posterior a la final.' };
  }
  return { range: { start: from || '0000-01-01', end: to || '9999-12-31' }, error: '' };
}

export const isInRange = (date: string | null, range: DateRange) =>
  date !== null && date >= range.start && date <= range.end;

export function rangeLabel(range: DateRange): string {
  const isOpen = (date: string) => date.startsWith('0000') || date.startsWith('9999');
  if (isOpen(range.start)) return `Hasta el ${formatDateOnly(range.end)}`;
  if (isOpen(range.end)) return `Desde el ${formatDateOnly(range.start)}`;
  if (range.start === range.end) return formatDateOnly(range.start);
  return `Del ${formatDateOnly(range.start)} al ${formatDateOnly(range.end)}`;
}