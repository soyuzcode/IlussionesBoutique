import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import type { AuditMetadata } from '../src/lib/audit';

type AuditModule = typeof import('../src/lib/audit');

let audit: AuditModule;
let fetchMock: ReturnType<typeof vi.fn<typeof fetch>>;

function sentPayload(callIndex = 0): Record<string, unknown> {
  const call = fetchMock.mock.calls[callIndex];
  const init = call?.[1] as RequestInit | undefined;
  return JSON.parse(String(init?.body)) as Record<string, unknown>;
}

describe('audit client', () => {
  beforeEach(async () => {
    vi.resetModules();
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2026-01-01T00:00:00.000Z'));
    window.sessionStorage.clear();

    fetchMock = vi.fn<typeof fetch>();
    fetchMock.mockResolvedValue({ ok: true, status: 202 } as Response);
    vi.stubGlobal('fetch', fetchMock);

    audit = await import('../src/lib/audit');
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  it('sends only allowlisted metadata and never credentials or PII', () => {
    audit.auditEvent({
      eventType: 'UI_LOGIN_SUBMITTED',
      entityType: 'AUTH',
      metadata: {
        AUTH_METHOD: 'PASSWORD',
        EMAIL: 'customer@example.com',
      } as unknown as AuditMetadata,
    });

    expect(fetchMock).toHaveBeenCalledTimes(1);

    const body = sentPayload();
    expect(body.metadata).toEqual({ AUTH_METHOD: 'PASSWORD' });
    expect(JSON.stringify(body)).not.toContain('customer@example.com');
    expect(body).not.toHaveProperty('sessionId');

    const init = fetchMock.mock.calls[0]?.[1] as RequestInit;
    const headers = init.headers as Record<string, string>;
    expect(headers['X-Audit-Session-Id']).toMatch(/^[A-Za-z0-9_-]{8,64}$/);
    expect(init.credentials).toBe('omit');
    expect(init.keepalive).toBe(true);
  });

  it('drops entity ids that do not match the safe identifier shape', () => {
    audit.auditEvent({
      eventType: 'UI_APPOINTMENT_DETAIL_VIEWED',
      entityType: 'APPOINTMENT',
      entityId: 'id with spaces',
    });

    expect(sentPayload()).not.toHaveProperty('entityId');
  });

  it('deduplicates identical events inside the dedupe window', () => {
    const input = {
      eventType: 'UI_SESSION_STARTED',
      entityType: 'AUTH',
    } as const;

    audit.auditEvent(input);
    audit.auditEvent(input);
    audit.auditEvent(input);

    expect(fetchMock).toHaveBeenCalledTimes(1);

    vi.setSystemTime(new Date('2026-01-01T00:00:01.001Z'));
    audit.auditEvent(input);

    expect(fetchMock).toHaveBeenCalledTimes(2);
  });

  it('does not collapse failures that have different allowlisted metadata', () => {
    audit.recordApiFailure('CUSTOMER', 'LOAD_CUSTOMERS', 'HTTP_ERROR', 500);
    audit.recordApiFailure('APPOINTMENT', 'CREATE_APPOINTMENT', 'NETWORK_ERROR');

    expect(fetchMock).toHaveBeenCalledTimes(2);
    expect(sentPayload(0).metadata).toEqual({
      OPERATION: 'LOAD_CUSTOMERS',
      ERROR_CODE: 'HTTP_ERROR',
      HTTP_STATUS: 500,
    });
    expect(sentPayload(1).metadata).toEqual({
      OPERATION: 'CREATE_APPOINTMENT',
      ERROR_CODE: 'NETWORK_ERROR',
    });
  });

  it('stops sending after the per-minute client rate limit', () => {
    for (let index = 0; index < 41; index++) {
      audit.auditEvent({
        eventType: 'UI_APPOINTMENT_DETAIL_VIEWED',
        entityType: 'APPOINTMENT',
        entityId: `appointment-${index}`,
      });
    }

    expect(fetchMock).toHaveBeenCalledTimes(40);
  });

  it('never throws or recursively audits when the endpoint is unavailable', async () => {
    const debugSpy = vi.spyOn(console, 'debug').mockImplementation(() => {});
    fetchMock.mockRejectedValue(new Error('network down'));

    expect(() =>
      audit.auditEvent({ eventType: 'UI_SESSION_STARTED', entityType: 'AUTH' })
    ).not.toThrow();
    expect(() =>
      audit.auditEvent({ eventType: 'UI_LOGOUT_CLICKED', entityType: 'AUTH' })
    ).not.toThrow();

    await Promise.resolve();
    await Promise.resolve();
    await Promise.resolve();

    expect(fetchMock).toHaveBeenCalledTimes(2);
    expect(debugSpy).toHaveBeenCalledTimes(1);
  });
});
