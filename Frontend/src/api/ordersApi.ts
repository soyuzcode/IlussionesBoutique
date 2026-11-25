import { ApiError, request } from './httpClient';
import type { Customer, Order, OrderStatus, Product } from '../types';

const BASE = '/api/order';
export const LOCAL_ORDERS_KEY = 'demo_orders_ilussiones';

export interface NewOrderData {
  orderId: string;
  customerId: string;
  eventType: string;
  deliveryDate: string; // "2026-10-29"
  productIds: string[];
  /** Copia que solo se usa si el pedido se guarda en este dispositivo. */
  customer?: Customer;
  products?: Product[];
}

export interface OrdersListing {
  orders: Order[];
  /** true cuando el backend no respondió y solo se muestran pedidos locales. */
  offline: boolean;
}

/** No hay conexión con el backend y tampoco se pudo guardar el pedido en este dispositivo. */
export class LocalSaveError extends Error {
  constructor(cause?: unknown) {
    super('local_save_failed', { cause });
    this.name = 'LocalSaveError';
  }
}

// Respaldo sin conexión: solo se usa si el backend no responde (error de red o CORS).
// Los errores HTTP (validación, stock insuficiente, sesión...) siempre se muestran.
const isOffline = (err: unknown) => !(err instanceof ApiError);

// Midday avoids the stored instant shifting to the previous day in UTC-6.
const toDeliveryInstant = (date: string) => `${date}T12:00:00`;

const readLocal = (): Order[] => {
  try {
    const stored = JSON.parse(localStorage.getItem(LOCAL_ORDERS_KEY) || '[]') as Order[];
    return Array.isArray(stored) ? stored.map((order) => ({ ...order, localOnly: true })) : [];
  } catch {
    return [];
  }
};

const writeLocal = (orders: Order[]) => {
  try {
    localStorage.setItem(LOCAL_ORDERS_KEY, JSON.stringify(orders));
  } catch (err) {
    throw new LocalSaveError(err);
  }
};

const findLocal = (id: string) => readLocal().find((order) => order.orderId === id);

const forCustomer = (orders: Order[], customerId: string) =>
  orders.filter((order) => order.customer?.id === customerId);

async function withLocal(load: () => Promise<Order[]>, local: Order[]): Promise<OrdersListing> {
  try {
    return { orders: [...local, ...(await load())], offline: false };
  } catch (err) {
    if (isOffline(err)) return { orders: local, offline: true };
    throw err;
  }
}

/** Borra los pedidos guardados solo en este navegador (se llama al cerrar sesión). */
export function clearLocalOrders(): void {
  try {
    localStorage.removeItem(LOCAL_ORDERS_KEY);
  } catch {
    // Sin almacenamiento no hay nada que borrar.
  }
}

export const ordersApi = {
  /** Pedidos del servidor más los guardados solo en este dispositivo (`localOnly`). */
  list: () => withLocal(() => request<Order[]>(BASE), readLocal()),
  listByCustomer: (customerId: string) =>
    withLocal(() => request<Order[]>(`${BASE}/customer/${customerId}`), forCustomer(readLocal(), customerId)),
  getAll: async (): Promise<Order[]> => (await ordersApi.list()).orders,
  getByCustomer: async (customerId: string): Promise<Order[]> =>
    (await ordersApi.listByCustomer(customerId)).orders,

  /** Si el backend no responde, el pedido devuelto tiene `localOnly: true`. */
  create: async (data: NewOrderData): Promise<Order> => {
    try {
      return await request<Order>(BASE, {
        method: 'POST',
        body: JSON.stringify({
          orderId: data.orderId,
          eventType: data.eventType,
          deliveryDate: toDeliveryInstant(data.deliveryDate),
          customer: { id: data.customerId },
          products: data.productIds.map((productId) => ({ productId })),
        }),
      });
    } catch (err) {
      if (!isOffline(err)) throw err;
      const order: Order = {
        orderId: data.orderId,
        eventType: data.eventType,
        deliveryDate: toDeliveryInstant(data.deliveryDate),
        customer: data.customer ?? { id: data.customerId, name: '' },
        products: data.products ?? [],
        status: 'PENDING',
        localOnly: true,
      };
      writeLocal([order, ...readLocal()]);
      return order;
    }
  },

  // Los pedidos locales no existen en el servidor, así que se editan y borran aquí.
  update: async (id: string, data: { eventType: string; deliveryDate: string; status?: OrderStatus }) => {
    if (findLocal(id)) {
      const updated = readLocal().map((order) => (order.orderId === id ? { ...order, ...data } : order));
      writeLocal(updated);
      return updated.find((order) => order.orderId === id) as Order;
    }
    return request<Order>(`${BASE}/${id}`, { method: 'PUT', body: JSON.stringify(data) });
  },
  remove: async (id: string): Promise<void> => {
    if (findLocal(id)) {
      writeLocal(readLocal().filter((order) => order.orderId !== id));
      return;
    }
    await request<void>(`${BASE}/${id}`, { method: 'DELETE' });
  },
};
