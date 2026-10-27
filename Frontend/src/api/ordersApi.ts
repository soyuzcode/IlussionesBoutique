import { request } from './httpClient';
import type { Order } from '../types';

const BASE = '/api/order';

export const ordersApi = {
  getAll: () => request<Order[]>(BASE),
  update: (id: string, data: { eventType: string; deliveryDate: string }) =>
    request<Order>(`${BASE}/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
  remove: (id: string) => request<void>(`${BASE}/${id}`, { method: 'DELETE' }),
};