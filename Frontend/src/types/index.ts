export interface Customer {
  id: string;
  name: string;
  phone?: string;
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
  googleEventId?: string | null;
  microsoftEventId?: string | null;
}

export type ProductType = 'DRESS' | 'CARD' | 'ALBUM' | 'CUP' | 'PLUSH';
export type ProductStatus = 'AVAILABLE' | 'OUT_OF_STOCK' | 'DISCONTINUED';

export interface Product {
  productId: string;
  name?: string;
  type?: ProductType;
  price: number;
  photo?: string | null;
  materials?: string | null;
  status?: ProductStatus;
  stock?: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface ProductInput {
  name: string;
  type: ProductType;
  price: number;
  stock: number;
  materials?: string;
  photo?: string;
  status?: ProductStatus;
}

export type OrderStatus = 'PENDING' | 'IN_PROGRESS' | 'READY' | 'DELIVERED' | 'CANCELLED';

export type CalendarProvider = 'GOOGLE' | 'MICROSOFT';

export interface CalendarProviderStatus {
  provider: CalendarProvider;
  configured: boolean;
  connected: boolean;
  accountEmail: string | null;
  connectedAt: string | null;
}

/** Medidas corporales en cm, tal como se escriben. */
export interface BodyMeasurements {
  shoulder?: string | null;
  bust?: string | null;
  hip?: string | null;
  waist?: string | null;
  skirtLength?: string | null;
  blouseLength?: string | null;
  neckline?: string | null;
}

/** Datos del vestido personalizado de un pedido. */
export interface CustomOrderDetails {
  customization: string;
  customizationDescription?: string | null;
  customPrice: number;
  /** Abono entregado; se resta del total. */
  deposit: number;
  measurements: BodyMeasurements;
}

export interface Order {
  orderId: string;
  eventType: string;
  deliveryDate: string;
  products: Product[];
  customer?: Customer | null;
  status?: OrderStatus | null;
  customOrder?: boolean | null;
  customization?: string | null;
  customizationDescription?: string | null;
  customPrice?: number | null;
  deposit?: number | null;
  measurements?: BodyMeasurements | null;
  /** Calculados por el backend. */
  total?: number | null;
  balanceDue?: number | null;
  /** Guardado solo en este dispositivo porque el backend no respondió. */
  localOnly?: boolean;
}

/** Pedido que se crea junto con la cita, antes de conocer el id del cliente. */
export interface AppointmentOrderData {
  orderId: string;
  eventType: string;
  deliveryDate: string;
  productIds: string[];
  products: Product[];
  custom?: CustomOrderDetails;
}

export interface NewAppointmentData {
  customerId?: string; // si es un cliente existente
  customerName: string;
  phone: string;
  reason: string;
  date: string;   // "2026-10-29"
  time: string;   // "10:00"
  period: string; // "AM" | "PM"
  order?: AppointmentOrderData;
}
export interface AuthUser {
  name: string;
  email: string;
  role?: string;
}