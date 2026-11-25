export const PHONE_DIGITS = 8;

/** Deja solo dígitos (máximo 8) y los separa en bloques de 4: "7000-0000". */
export function formatPhone(value: string): string {
  const digits = value.replace(/\D/g, '').slice(0, PHONE_DIGITS);
  return digits.length > 4 ? `${digits.slice(0, 4)}-${digits.slice(4)}` : digits;
}

/** El teléfono debe tener exactamente 8 dígitos. */
export const isValidPhone = (value: string) => value.replace(/\D/g, '').length === PHONE_DIGITS && /^[\d\s-]+$/.test(value.trim());
