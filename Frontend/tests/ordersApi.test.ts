import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import type { Order, Product } from '../src/types';

type OrdersModule = typeof import('../src/api/ordersApi');

let api: OrdersModule;
let fetchMock: ReturnType<typeof vi.fn<typeof fetch>>;

const json = (status: number, body: unknown) =>
  new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } });
const networkError = () => Promise.reject(new TypeError('Failed to fetch'));

const dress: Product = { productId: 'p1', name: 'Vestido rosa', price: 120, type: 'DRESS' };
const newOrder = {
  orderId: 'PED-1',
  customerId: 'c1',
  eventType: 'Boda',
  deliveryDate: '2026-12-20',
  productIds: ['p1'],
  customer: { id: 'c1', name: 'Ana López' },
  products: [dress],
};
const serverOrder: Order = {
  orderId: 'SRV-1',
  eventType: 'XV años',
  deliveryDate: '2026-12-01T18:00:00Z',
  products: [],
  customer: { id: 'c1', name: 'Ana López' },
  status: 'PENDING',
};
const stored = () => JSON.parse(localStorage.getItem('demo_orders_ilussiones') ?? '[]') as Order[];

describe('ordersApi offline fallback', () => {
  beforeEach(async () => {
    vi.resetModules();
    localStorage.clear();
    sessionStorage.clear();
    fetchMock = vi.fn<typeof fetch>();
    vi.stubGlobal('fetch', fetchMock);
    api = await import('../src/api/ordersApi');
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  it('sends the order to the backend when it is reachable', async () => {
    fetchMock.mockResolvedValue(json(200, serverOrder));
    const order = await api.ordersApi.create(newOrder);

    expect(order.localOnly).toBeUndefined();
    const body = JSON.parse(String(fetchMock.mock.calls[0]?.[1]?.body));
    expect(body).toEqual({
      orderId: 'PED-1',
      eventType: 'Boda',
      deliveryDate: '2026-12-20T12:00:00',
      customer: { id: 'c1' },
      products: [{ productId: 'p1' }],
    });
    expect(stored()).toEqual([]);
  });

  it('saves the full order locally and flags it when the backend is unreachable', async () => {
    fetchMock.mockImplementation(networkError);
    const order = await api.ordersApi.create(newOrder);

    expect(order).toMatchObject({
      localOnly: true,
      status: 'PENDING',
      deliveryDate: '2026-12-20T12:00:00',
      customer: { id: 'c1', name: 'Ana López' },
      products: [dress],
    });
    expect(new Date(order.deliveryDate).getDate()).toBe(20);
    expect(stored()).toHaveLength(1);
  });

  it.each([400, 401, 409, 500, 502])('surfaces HTTP %i errors and never saves locally', async (status) => {
    fetchMock.mockResolvedValue(json(status, { reason: 'insufficient_stock' }));

    await expect(api.ordersApi.create(newOrder)).rejects.toMatchObject({ status });
    expect(stored()).toEqual([]);
  });

  it('reports a storage failure instead of pretending the order was saved', async () => {
    fetchMock.mockImplementation(networkError);
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new DOMException('full', 'QuotaExceededError');
    });

    await expect(api.ordersApi.create(newOrder)).rejects.toBeInstanceOf(api.LocalSaveError);
  });

  it('flags the listing as offline and shows only local orders', async () => {
    fetchMock.mockImplementation(networkError);
    await api.ordersApi.create(newOrder);

    const listing = await api.ordersApi.list();
    expect(listing.offline).toBe(true);
    expect(listing.orders.map((o) => o.orderId)).toEqual(['PED-1']);
  });

  it('keeps unsynced orders visible next to server orders after the backend recovers', async () => {
    fetchMock.mockImplementationOnce(networkError);
    await api.ordersApi.create(newOrder);
    fetchMock.mockResolvedValue(json(200, [serverOrder]));

    const listing = await api.ordersApi.list();
    expect(listing.offline).toBe(false);
    expect(listing.orders.map((o) => [o.orderId, o.localOnly ?? false])).toEqual([
      ['PED-1', true],
      ['SRV-1', false],
    ]);
  });

  it('includes the customer local orders in the client detail, online and offline', async () => {
    fetchMock.mockImplementationOnce(networkError);
    await api.ordersApi.create(newOrder);
    await api.ordersApi.create({ ...newOrder, orderId: 'PED-2', customerId: 'c2', customer: { id: 'c2', name: 'Eva' } }).catch(() => undefined);

    fetchMock.mockResolvedValueOnce(json(200, [serverOrder]));
    const online = await api.ordersApi.listByCustomer('c1');
    expect(online.orders.map((o) => o.orderId)).toEqual(['PED-1', 'SRV-1']);

    fetchMock.mockImplementation(networkError);
    const offline = await api.ordersApi.listByCustomer('c1');
    expect(offline).toMatchObject({ offline: true, orders: [{ orderId: 'PED-1' }] });
  });

  it('edits and deletes local orders without calling the backend', async () => {
    fetchMock.mockImplementationOnce(networkError);
    await api.ordersApi.create(newOrder);
    fetchMock.mockClear();

    const updated = await api.ordersApi.update('PED-1', { eventType: 'Boda civil', deliveryDate: '2026-12-21T18:00:00.000Z' });
    expect(updated).toMatchObject({ eventType: 'Boda civil', products: [dress], localOnly: true });
    await api.ordersApi.remove('PED-1');

    expect(fetchMock).not.toHaveBeenCalled();
    expect(stored()).toEqual([]);
  });

  it('does not fake edits or deletes of server orders while offline', async () => {
    fetchMock.mockImplementation(networkError);

    await expect(api.ordersApi.update('SRV-1', { eventType: 'x', deliveryDate: '2026-12-01' })).rejects.toBeInstanceOf(TypeError);
    await expect(api.ordersApi.remove('SRV-1')).rejects.toBeInstanceOf(TypeError);
  });

  it('clears local orders on logout', async () => {
    fetchMock.mockImplementation(networkError);
    await api.ordersApi.create(newOrder);

    api.clearLocalOrders();
    expect(localStorage.getItem('demo_orders_ilussiones')).toBeNull();
    expect((await api.ordersApi.list()).orders).toEqual([]);
  });
});
