export type CustomerStatus = 'ACTIVO' | 'FINALIZADO';

export interface Customer {
  id: string;
  name: string;
  phone?: string;
  status?: CustomerStatus;
  createdAt?: string;
  updatedAt?: string;
}

export type AppointmentStatus = 'SCHEDULED' | 'COMPLETED' | 'CANCELLED';

export interface Appointment {
  id: string;
  customer: Customer;
  reason: string;
  appointmentDate: string; // "2026-10-29"
  appointmentTime: string; // "10:00:00"
  status: AppointmentStatus;
}

export interface Product {
  productId: string;
  price: number;
  photo?: string;
  materials?: string;
  status?: string;
  type?: string;
  [field: string]: unknown;
}

export interface Order {
  orderId: string;
  eventType: string;
  deliveryDate: string;
  products: Product[];
  customer?: Customer; // llegará cuando el backend lo agregue
  status?: string;     // idem
}

export interface NewAppointmentData {
  customerId?: string; // si es una clienta existente
  customerName: string;
  phone: string;
  reason: string;
  date: string;   // "2026-10-29"
  time: string;   // "10:00"
  period: string; // "AM" | "PM"
}
export interface AuthUser {
  name: string;
  email: string;
}