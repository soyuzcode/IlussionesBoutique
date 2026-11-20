import { ApiError, request } from './httpClient';
import type { Order, OrderStatus } from '../types';

const BASE = '/api/order';
const LOCAL_STORAGE_KEY = 'demo_orders_ilussiones';

export interface NewOrderData {
  orderId: string;
  customerId: string;
  eventType: string;
  deliveryDate: string;
  productIds: string[];
}

// Demo fallback: only used when the backend is unreachable (network error).
// HTTP errors such as validation or insufficient stock are always surfaced.
const isOffline = (err: unknown) => !(err instanceof ApiError);

const readLocal = (): Order[] => {
  try {
    return JSON.parse(localStorage.getItem(LOCAL_STORAGE_KEY) || '[]') as Order[];
  } catch {
    return [];
  }
};

const writeLocal = (orders: Order[]) => localStorage.setItem(LOCAL_STORAGE_KEY, JSON.stringify(orders));

export const ordersApi = {
  getAll: async (): Promise<Order[]> => {
    try {
      return await request<Order[]>(BASE);
    } catch (err) {
      if (isOffline(err)) return readLocal();
      throw err;
    }
  },
  getByCustomer: (customerId: string) => request<Order[]>(`${BASE}/customer/${customerId}`),
  create: async (data: NewOrderData): Promise<Order> => {
    try {
      return await request<Order>(BASE, {
        method: 'POST',
        body: JSON.stringify({
          orderId: data.orderId,
          eventType: data.eventType,
          // Midday avoids the stored instant shifting to the previous day in UTC-6.
          deliveryDate: `${data.deliveryDate}T12:00:00`,
          customer: { id: data.customerId },
          products: data.productIds.map((productId) => ({ productId })),
        }),
      });
    } catch (err) {
      if (!isOffline(err)) throw err;
      const order: Order = {
        orderId: data.orderId,
        eventType: data.eventType,
        deliveryDate: data.deliveryDate,
        products: [],
        status: 'PENDING',
      };
      writeLocal([order, ...readLocal()]);
      return order;
    }
  },
  update: async (id: string, data: { eventType: string; deliveryDate: string; status?: OrderStatus }) => {
    try {
      return await request<Order>(`${BASE}/${id}`, { method: 'PUT', body: JSON.stringify(data) });
    } catch (err) {
      const local = readLocal();
      if (!isOffline(err) || !local.some((o) => o.orderId === id)) throw err;
      const updated = local.map((o) => (o.orderId === id ? { ...o, ...data } : o));
      writeLocal(updated);
      return updated.find((o) => o.orderId === id) as Order;
    }
  },
  remove: async (id: string): Promise<void> => {
    try {
      await request<void>(`${BASE}/${id}`, { method: 'DELETE' });
    } catch (err) {
      const local = readLocal();
      if (!isOffline(err) || !local.some((o) => o.orderId === id)) throw err;
      writeLocal(local.filter((o) => o.orderId !== id));
    }
  },
};
