import { request } from './httpClient';
import type { Customer } from '../types';

const BASE = '/api/customer';

export const customersApi = {
  getAll: () => request<Customer[]>(BASE),
  create: (customer: Pick<Customer, 'name' | 'phone'>) =>
    request<Customer>(BASE, { method: 'POST', body: JSON.stringify(customer) }),
  remove: (id: string) => request<void>(`${BASE}/${id}`, { method: 'DELETE' }),
};