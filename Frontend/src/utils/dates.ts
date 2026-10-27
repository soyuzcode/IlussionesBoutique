export const todayISO = () => new Date().toLocaleDateString('sv-SE'); // "2026-10-26"

export interface TimeSlot {
  time: string; // "10:00"
  period: 'AM' | 'PM';
}

export function to24h(time: string, period: string): string {
  const [hours, minutes] = time.split(':').map(Number);
  let h = hours;
  if (period === 'PM' && h < 12) h += 12;
  if (period === 'AM' && h === 12) h = 0;
  return `${String(h).padStart(2, '0')}:${String(minutes ?? 0).padStart(2, '0')}:00`;
}

export function to12h(time24: string): TimeSlot {
  const [hours, minutes] = time24.split(':').map(Number);
  const h12 = hours % 12 === 0 ? 12 : hours % 12;
  return {
    time: `${String(h12).padStart(2, '0')}:${String(minutes).padStart(2, '0')}`,
    period: hours >= 12 ? 'PM' : 'AM',
  };
}

// Horarios de atención: de 8:00 AM a 7:00 PM cada 30 minutos (ajústalo si la boutique tiene otro horario)
export const TIME_SLOTS: TimeSlot[] = [];
for (let minutes = 8 * 60; minutes <= 19 * 60; minutes += 30) {
  TIME_SLOTS.push(to12h(`${Math.floor(minutes / 60)}:${minutes % 60}`));
}

export const slotKey = (slot: TimeSlot) => `${slot.time} ${slot.period}`;

export function isPastSlot(slot: TimeSlot, date: string): boolean {
  if (date !== todayISO()) return false;
  const [h, m] = to24h(slot.time, slot.period).split(':').map(Number);
  const now = new Date();
  return h * 60 + m <= now.getHours() * 60 + now.getMinutes();
}

export function daysUntil(iso: string): number {
  const target = new Date(iso);
  const today = new Date();
  target.setHours(0, 0, 0, 0);
  today.setHours(0, 0, 0, 0);
  return Math.round((target.getTime() - today.getTime()) / 86400000);
}

// Para fechas con hora (pedidos)
export const formatDate = (iso: string) =>
  new Date(iso).toLocaleDateString('es-SV', { day: 'numeric', month: 'long', year: 'numeric' });

// Para fechas sin hora, como "2026-10-29" (citas). Sin esto se mostraría un día antes.
export const formatDateOnly = (date: string) =>
  new Date(`${date}T00:00:00`).toLocaleDateString('es-SV', { day: 'numeric', month: 'long', year: 'numeric' });

// Fecha del header: "Lunes, 26 de octubre"
export function formatHeaderDate(): string {
  const text = new Date().toLocaleDateString('es-SV', {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
  });
  return text.charAt(0).toUpperCase() + text.slice(1);
}