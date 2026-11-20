import type { AuthUser } from '../types';

const TOKEN_KEY = 'ilussiones.auth.token';
const USER_KEY = 'ilussiones.auth.user';

/**
 * La sesión se guarda en `sessionStorage`: se borra al cerrar la pestaña y no
 * se comparte entre pestañas. El token lo emite el backend; el frontend nunca
 * decide por sí mismo si unas credenciales son válidas.
 */
function storage(): Storage | null {
  try {
    return typeof window === 'undefined' ? null : window.sessionStorage;
  } catch {
    return null;
  }
}

export function saveSession(token: string, user: AuthUser): void {
  const store = storage();
  if (!store) return;

  try {
    store.setItem(TOKEN_KEY, token);
    store.setItem(USER_KEY, JSON.stringify(user));
  } catch {
    // Si el almacenamiento no está disponible, la sesión vive solo en memoria.
  }
}

export function clearSession(): void {
  const store = storage();
  if (!store) return;

  try {
    store.removeItem(TOKEN_KEY);
    store.removeItem(USER_KEY);
  } catch {
    // Nada que limpiar.
  }
}

export function getAuthToken(): string | null {
  const store = storage();
  if (!store) return null;

  try {
    return store.getItem(TOKEN_KEY);
  } catch {
    return null;
  }
}

export function getStoredUser(): AuthUser | null {
  const store = storage();
  if (!store) return null;

  try {
    const raw = store.getItem(USER_KEY);
    if (!raw) return null;

    const parsed: unknown = JSON.parse(raw);
    if (
      typeof parsed !== 'object' ||
      parsed === null ||
      typeof (parsed as AuthUser).name !== 'string' ||
      typeof (parsed as AuthUser).email !== 'string'
    ) {
      return null;
    }

    return parsed as AuthUser;
  } catch {
    return null;
  }
}

export function authHeaders(): Record<string, string> {
  const token = getAuthToken();
  return token ? { Authorization: `Bearer ${token}` } : {};
}

const SIGNED_OUT_KEY = 'ilussiones.auth.signedOut';

/**
 * Marca persistente de "cerré sesión": si el logout no llega al servidor, la
 * cookie HttpOnly sigue viva y JavaScript no puede borrarla. Con esta marca la
 * app no la usa para reabrir la sesión y reintenta el logout al arrancar.
 */
export function markSignedOut(): void {
  try {
    localStorage.setItem(SIGNED_OUT_KEY, '1');
  } catch {
    // Sin almacenamiento persistente no hay nada que marcar.
  }
}

export function clearSignedOut(): void {
  try {
    localStorage.removeItem(SIGNED_OUT_KEY);
  } catch {
    // Nada que limpiar.
  }
}

export function isSignedOut(): boolean {
  try {
    return localStorage.getItem(SIGNED_OUT_KEY) === '1';
  } catch {
    return false;
  }
}
