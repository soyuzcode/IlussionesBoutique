import { request } from './httpClient';
import type { Customer } from '../types';

const BASE = '/api/customer';

export const customersApi = {
  getAll: () => request<Customer[]>(BASE),
  create: (customer: Pick<Customer, 'name' | 'phone'>) =>
    request<Customer>(BASE, { method: 'POST', body: JSON.stringify(customer) }),
  update: (id: string, data: { name: string; phone?: string }) =>
    request<Customer>(`${BASE}/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
  remove: (id: string) => request<void>(`${BASE}/${id}`, { method: 'DELETE' }),
};