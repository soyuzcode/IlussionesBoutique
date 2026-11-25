import type { AppointmentOrderData, BodyMeasurements, Product } from '../../types';
import { todayISO } from '../../utils/dates';

export interface OrderDraft {
  eventType: string;
  /** Por defecto la fecha del registro (hoy); otra fecha activa el pedido personalizado. */
  deliveryDate: string;
  productIds: string[];
  customOrder: boolean;
  customization: string;
  customizationDescription: string;
  customPrice: string;
  deposit: string;
  measurements: Record<keyof BodyMeasurements, string>;
}

export const emptyOrderDraft = (): OrderDraft => ({
  eventType: '',
  deliveryDate: todayISO(),
  productIds: [],
  customOrder: false,
  customization: '',
  customizationDescription: '',
  customPrice: '',
  deposit: '',
  measurements: { shoulder: '', bust: '', hip: '', waist: '', skirtLength: '', blouseLength: '', neckline: '' },
});

const AMOUNT = /^\d+(\.\d{1,2})?$/;
const toAmount = (value: string) => (AMOUNT.test(value.trim()) ? Number(value.trim()) : NaN);

export const generateOrderId = () => `PED-${Date.now().toString(36).toUpperCase()}`;

export function draftTotals(draft: OrderDraft, products: Product[]) {
  const selected = products.filter((p) => draft.productIds.includes(p.productId));
  const productsTotal = selected.reduce((sum, p) => sum + Number(p.price ?? 0), 0);
  const customPrice = draft.customOrder ? toAmount(draft.customPrice) || 0 : 0;
  const deposit = draft.customOrder ? toAmount(draft.deposit) || 0 : 0;
  const total = productsTotal + customPrice;
  return { selected, total, deposit, balance: Math.max(total - deposit, 0) };
}

/** Devuelve el mensaje de error o null si el pedido es válido. */
export function validateOrderDraft(draft: OrderDraft, products: Product[]): string | null {
  const today = todayISO();
  if (draft.eventType.trim().length < 2) return 'Indica el tipo de evento.';
  if (!draft.deliveryDate) return 'Indica la fecha de entrega.';
  if (draft.deliveryDate < today) return 'La fecha de entrega no puede estar en el pasado.';
  if (!draft.customOrder) {
    return draft.productIds.length === 0 ? 'Elige al menos un producto o activa el pedido personalizado.' : null;
  }
  if (draft.deliveryDate === today) return 'Elige la fecha de entrega programada del vestido.';
  if (!draft.customization.trim()) return 'Indica la personalización del vestido.';
  if (draft.customization.trim().length > 120) return 'La personalización admite máximo 120 caracteres.';
  if (draft.customizationDescription.trim().length > 1000) return 'La descripción admite máximo 1000 caracteres.';
  if (Number.isNaN(toAmount(draft.customPrice))) return 'Indica el precio del vestido (ej. 150.00).';
  if (draft.deposit.trim() && Number.isNaN(toAmount(draft.deposit))) return 'El abono no es válido (ej. 50.00).';
  if (Object.values(draft.measurements).some((value) => value.trim().length > 20)) {
    return 'Cada medida admite máximo 20 caracteres.';
  }
  const { total, deposit } = draftTotals(draft, products);
  if (deposit > total) return 'El abono no puede ser mayor que el total del pedido.';
  return null;
}

const blankToNull = (value: string) => value.trim() || null;

export function toOrderData(draft: OrderDraft, products: Product[]): AppointmentOrderData {
  const { selected } = draftTotals(draft, products);
  return {
    orderId: generateOrderId(),
    eventType: draft.eventType.trim(),
    deliveryDate: draft.deliveryDate,
    productIds: selected.map((p) => p.productId),
    products: selected,
    custom: draft.customOrder
      ? {
          customization: draft.customization.trim(),
          customizationDescription: blankToNull(draft.customizationDescription),
          customPrice: toAmount(draft.customPrice),
          deposit: toAmount(draft.deposit) || 0,
          measurements: Object.fromEntries(
            Object.entries(draft.measurements).map(([key, value]) => [key, blankToNull(value)]),
          ) as BodyMeasurements,
        }
      : undefined,
  };
}

export const orderErrorMessage = (reason: string | undefined) =>
  ({
    insufficient_stock: 'Uno de los productos ya no tiene stock.',
    deposit_exceeds_total: 'El abono no puede ser mayor que el total del pedido.',
    customization_required: 'Indica la personalización del vestido.',
    custom_price_required: 'Indica el precio del vestido.',
    delivery_date_required: 'Indica la fecha de entrega programada.',
  })[reason ?? ''] ?? 'No se pudo crear el pedido.';
