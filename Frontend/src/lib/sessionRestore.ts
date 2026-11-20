import { ApiError } from '../api/httpClient';
import { authApi, type LoginResult } from '../api/authApi';

export type RestoreOutcome =
  | { status: 'restored'; result: LoginResult }
  | { status: 'signed-out' }
  | { status: 'unreachable' };

/** Tiempo máximo de cada intento: el backend gratuito puede tardar en despertar. */
export const ATTEMPT_TIMEOUT_MS = 20_000;

const WAKING_STATUSES = [502, 503, 504];

/** Espera entre intentos: 2 s, 4 s, 8 s y luego cada 10 s. */
export const retryDelay = (attempt: number) => Math.min(10_000, 2_000 * 2 ** attempt);

/**
 * Un intento de restaurar la sesión. Un fallo de red, el timeout o un 502/503/504
 * del proxy mientras arranca el contenedor cuentan como "unreachable"; cualquier
 * otra respuesta significa que el servidor ya contestó.
 */
export async function restoreSession(timeoutMs = ATTEMPT_TIMEOUT_MS): Promise<RestoreOutcome> {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);
  try {
    return { status: 'restored', result: await authApi.session(controller.signal) };
  } catch (err) {
    const waking = !(err instanceof ApiError) || WAKING_STATUSES.includes(err.status);
    return waking ? { status: 'unreachable' } : { status: 'signed-out' };
  } finally {
    clearTimeout(timer);
  }
}
