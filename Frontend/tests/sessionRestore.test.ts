import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { request } from '../src/api/httpClient';
import { getAuthToken, saveSession } from '../src/lib/authSession';
import { restoreSession, retryDelay } from '../src/lib/sessionRestore';

const USER = { name: 'Admin', email: 'admin@ilussiones.com', role: 'ADMIN' };
const json = (status: number, body: unknown) =>
  new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } });

let fetchMock: ReturnType<typeof vi.fn<typeof fetch>>;

describe('session restore', () => {
  beforeEach(() => {
    sessionStorage.clear();
    fetchMock = vi.fn<typeof fetch>();
    vi.stubGlobal('fetch', fetchMock);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('restores the session from the cookie and stores the new token', async () => {
    fetchMock.mockResolvedValueOnce(json(200, { token: 'fresh', tokenType: 'Bearer', expiresIn: 3600, user: USER }));

    const outcome = await restoreSession();

    expect(outcome.status).toBe('restored');
    expect(getAuthToken()).toBe('fresh');
    const [url, init] = fetchMock.mock.calls[0];
    expect(String(url)).toMatch(/\/api\/auth\/session$/);
    expect(init?.credentials).toBe('include');
  });

  it('treats 401 as signed out, but network errors and proxy 503 as a sleeping server', async () => {
    fetchMock.mockResolvedValueOnce(json(401, { error: 'no_session' }));
    expect((await restoreSession()).status).toBe('signed-out');

    fetchMock.mockRejectedValueOnce(new TypeError('Failed to fetch'));
    expect((await restoreSession()).status).toBe('unreachable');

    fetchMock.mockResolvedValueOnce(json(503, {}));
    expect((await restoreSession()).status).toBe('unreachable');
  });

  it('gives up an attempt after the timeout', async () => {
    fetchMock.mockImplementationOnce((_url, init) =>
      new Promise((_resolve, reject) => init?.signal?.addEventListener('abort', () => reject(new DOMException('', 'AbortError')))),
    );
    expect((await restoreSession(10)).status).toBe('unreachable');
  });

  it('backs off between attempts up to 10 seconds', () => {
    expect([0, 1, 2, 3, 6].map(retryDelay)).toEqual([2000, 4000, 8000, 10000, 10000]);
  });

  it('renews an expired access token through the cookie and retries once', async () => {
    saveSession('expired', USER);
    fetchMock
      .mockResolvedValueOnce(json(401, { error: 'unauthorized' }))
      .mockResolvedValueOnce(json(200, { token: 'fresh', tokenType: 'Bearer', expiresIn: 3600, user: USER }))
      .mockResolvedValueOnce(json(200, [{ id: '1' }]));

    await expect(request('/api/customer')).resolves.toEqual([{ id: '1' }]);

    const sessionHeaders = fetchMock.mock.calls[1][1]?.headers as Record<string, string>;
    expect(sessionHeaders.Authorization).toBeUndefined();
    const retryHeaders = fetchMock.mock.calls[2][1]?.headers as Record<string, string>;
    expect(retryHeaders.Authorization).toBe('Bearer fresh');
    expect(fetchMock.mock.calls[0][1]?.credentials).toBeUndefined();
  });
});
