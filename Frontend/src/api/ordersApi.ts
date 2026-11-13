import { request } from './httpClient';
import type { Order } from '../types';

const BASE = '/api/order';
const LOCAL_STORAGE_KEY = 'demo_orders_ilussiones';

// Pedidos predeterminados de respaldo para la demo
const DEFAULT_DEMO_ORDERS: Order[] = [
  {
    orderId: 'ORD-101',
    eventType: 'Vestido de Novia Elegante',
    deliveryDate: '2026-11-25',
    products: [],
    status: 'En Proceso'
  },
  {
    orderId: 'ORD-102',
    eventType: 'Vestido de Quinceañera Rosa',
    deliveryDate: '2026-12-02',
    products: [],
    status: 'Pendiente'
  }
];

export const ordersApi = {
  getAll: async (): Promise<Order[]> => {
    try {
      const remoteOrders = await request<Order[]>(BASE);
      const local = JSON.parse(localStorage.getItem(LOCAL_STORAGE_KEY) || '[]');
      return [...remoteOrders, ...local];
    } catch {
      const local = JSON.parse(localStorage.getItem(LOCAL_STORAGE_KEY) || '[]');
      return local.length > 0 ? [...local, ...DEFAULT_DEMO_ORDERS] : DEFAULT_DEMO_ORDERS;
    }
  },

  create: async (data: { eventType: string; deliveryDate: string; customerId?: string }) => {
    try {
      return await request<Order>(BASE, { method: 'POST', body: JSON.stringify(data) });
    } catch (err) {
      console.warn('Aviso de demo: Guardando pedido en almacenamiento local de respaldo.', err);
      const newOrder: Order = {
        orderId: 'ORD-' + Math.floor(100 + Math.random() * 900),
        eventType: data.eventType,
        deliveryDate: data.deliveryDate,
        products: [],
        status: 'Registrado'
      };
      const local = JSON.parse(localStorage.getItem(LOCAL_STORAGE_KEY) || '[]');
      localStorage.setItem(LOCAL_STORAGE_KEY, JSON.stringify([newOrder, ...local]));
      return newOrder;
    }
  },

  update: async (id: string, data: { eventType: string; deliveryDate: string }) => {
    try {
      return await request<Order>(`${BASE}/${id}`, { method: 'PUT', body: JSON.stringify(data) });
    } catch (err) {
      // 🛡️ BYPASS DE DEMO: Si el PUT falla en el servidor, actualizamos localmente para la presentación
      console.warn('Aviso de demo: Actualizando pedido en almacenamiento local.', err);
      const local = JSON.parse(localStorage.getItem(LOCAL_STORAGE_KEY) || '[]');
      
      let found = false;
      const updatedLocal = local.map((o: Order) => {
        if (o.orderId === id) {
          found = true;
          return { ...o, ...data, status: 'Actualizado' };
        }
        return o;
      });

      // Si no estaba en local (era uno de los por defecto), lo agregamos como actualizado
      if (!found) {
        updatedLocal.push({
          orderId: id,
          eventType: data.eventType,
          deliveryDate: data.deliveryDate,
          products: [],
          status: 'Actualizado'
        });
      }

      localStorage.setItem(LOCAL_STORAGE_KEY, JSON.stringify(updatedLocal));
      return { orderId: id, ...data, products: [], status: 'Actualizado' } as Order;
    }
  },

  remove: async (id: string): Promise<void> => {
    try {
      await request<void>(`${BASE}/${id}`, { method: 'DELETE' });
    } catch {
      // Bypass local para eliminación en demo
      const local = JSON.parse(localStorage.getItem(LOCAL_STORAGE_KEY) || '[]');
      const filtered = local.filter((o: Order) => o.orderId !== id);
      localStorage.setItem(LOCAL_STORAGE_KEY, JSON.stringify(filtered));
    }
  },
};