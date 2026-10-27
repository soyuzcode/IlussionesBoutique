import { request } from './httpClient';
import type { Customer } from '../types';

const BASE = '/api/customer';

export const customersApi = {
  getAll: () => request<Customer[]>(BASE),
  create: (customer: Partial<Customer>) =>
    request<Customer>(BASE, { method: 'POST', body: JSON.stringify(customer) }),
  update: (id: string, customer: Partial<Customer>) =>
    request<Customer>(`${BASE}/${id}`, { method: 'PUT', body: JSON.stringify(customer) }),
  remove: (id: string) => request<void>(`${BASE}/${id}`, { method: 'DELETE' }),
};