import {
  getAuditSessionId,
  recordApiFailure,
} from './audit';
import type {
  AuditEntityType,
  AuditErrorCode,
  AuditOperation,
} from './audit';
import { API_BASE_URL } from './config';

export interface AuditApiContext {
  entityType: AuditEntityType;
  operation: AuditOperation;
}

export class ApiError extends Error {
  readonly code: AuditErrorCode;
  readonly status?: number;

  constructor(code: AuditErrorCode, status?: number) {
    super(status === undefined ? code : `${code} (HTTP ${status})`);
    this.name = 'ApiError';
    this.code = code;
    this.status = status;
  }
}

const REQUEST_TIMEOUT_MS = 12_000;

function buildUrl(path: string): string {
  return `${API_BASE_URL}${path.startsWith('/') ? path : `/${path}`}`;
}

function isAuditEndpoint(path: string): boolean {
  return path.startsWith('/audit');
}

async function parseJson<T>(response: Response): Promise<T> {
  if (response.status === 204) {
    return undefined as T;
  }

  const text = await response.text();
  if (!text) {
    return undefined as T;
  }

  return JSON.parse(text) as T;
}

/**
 * API wrapper that records transport/HTTP failures as frontend audit events.
 *
 * <p>It never records success: completed business operations are audited by
 * the backend. It never reads or logs response bodies, so database errors or
 * customer data cannot leak into audit metadata.</p>
 */
export async function apiRequest<T>(
  path: string,
  init: RequestInit = {},
  auditContext?: AuditApiContext
): Promise<T> {
  const skipAudit = isAuditEndpoint(path);
  const controller = new AbortController();
  const timeout = window.setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS);

  const headers = new Headers(init.headers);
  if (!headers.has('Content-Type') && init.body !== undefined) {
    headers.set('Content-Type', 'application/json');
  }
  headers.set('X-Audit-Session-Id', getAuditSessionId());

  try {
    const response = await fetch(buildUrl(path), {
      ...init,
      headers,
      signal: controller.signal,
    });

    if (!response.ok) {
      if (!skipAudit && auditContext) {
        recordApiFailure(
          auditContext.entityType,
          auditContext.operation,
          'HTTP_ERROR',
          response.status
        );
      }
      throw new ApiError('HTTP_ERROR', response.status);
    }

    try {
      return await parseJson<T>(response);
    } catch {
      if (!skipAudit && auditContext) {
        recordApiFailure(
          auditContext.entityType,
          auditContext.operation,
          'INVALID_RESPONSE',
          response.status
        );
      }
      throw new ApiError('INVALID_RESPONSE', response.status);
    }
  } catch (error) {
    if (error instanceof ApiError) {
      throw error;
    }

    const isTimeout = error instanceof Error && error.name === 'AbortError';
    const code: AuditErrorCode = isTimeout ? 'TIMEOUT' : 'NETWORK_ERROR';

    if (!skipAudit && auditContext) {
      recordApiFailure(
        auditContext.entityType,
        auditContext.operation,
        code
      );
    }

    throw new ApiError(code);
  } finally {
    window.clearTimeout(timeout);
  }
}
