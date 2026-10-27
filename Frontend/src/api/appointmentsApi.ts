import { request } from './httpClient';
import { to12h } from '../utils/dates';
import type { Appointment } from '../types';
import type { Cita } from '../components/common/CitaCard';

const BASE = '/api/appointment';

export const appointmentsApi = {
  getAll: () => request<Appointment[]>(BASE),
  create: (data: {
    customerId: string;
    reason: string;
    appointmentDate: string; // "2026-10-29"
    appointmentTime: string; // "10:00:00"
  }) =>
    request<Appointment>(BASE, {
      method: 'POST',
      body: JSON.stringify({
        customer: { id: data.customerId },
        reason: data.reason,
        appointmentDate: data.appointmentDate,
        appointmentTime: data.appointmentTime,
        status: 'SCHEDULED',
      }),
    }),
  update: (id: string, data: { reason: string; appointmentDate: string; appointmentTime: string }) =>
    request<Appointment>(`${BASE}/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
};

// Convierte una cita del backend al formato que usa CitaCard
export function appointmentToCita(appointment: Appointment): Cita {
  const { time, period } = to12h(appointment.appointmentTime);
  return {
    id: appointment.id,
    client: appointment.customer.name,
    type: appointment.reason,
    time,
    period,
    status: appointment.status === 'SCHEDULED' ? 'active' : appointment.status.toLowerCase(),
  };
}