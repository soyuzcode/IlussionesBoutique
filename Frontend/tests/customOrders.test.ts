import { afterEach, describe, expect, it, vi } from 'vitest';

import type { Order, Product } from '../src/types';
import { formatPhone, isValidPhone } from '../src/utils/phone';
import { orderBalance, orderDeposit, orderTotal } from '../src/utils/quote';
import { emptyOrderDraft, toOrderData, validateOrderDraft } from '../src/components/orders/orderDraft';

const tiara: Product = { productId: 'p1', name: 'Tiara', price: 30, type: 'CARD' };
const tomorrow = () => new Date(Date.now() + 86_400_000).toLocaleDateString('sv-SE');

describe('phone', () => {
  it('keeps eight digits split in blocks of four', () => {
    expect(formatPhone('7a0001234999')).toBe('7000-1234');
    expect(formatPhone('700')).toBe('700');
    expect(formatPhone('70001')).toBe('7000-1');
  });

  it('requires exactly eight digits', () => {
    expect(isValidPhone('7000-1234')).toBe(true);
    expect(isValidPhone('70001234')).toBe(true);
    expect(isValidPhone('7000-123')).toBe(false);
    expect(isValidPhone('')).toBe(false);
  });
});

describe('order totals', () => {
  const custom: Order = {
    orderId: 'PED-1',
    eventType: 'Boda',
    deliveryDate: '2026-12-20',
    products: [tiara],
    customOrder: true,
    customization: 'Sirena',
    customPrice: 200,
    deposit: 80,
  };

  it('adds the dress price and subtracts the deposit', () => {
    expect(orderTotal(custom)).toBe(230);
    expect(orderDeposit(custom)).toBe(80);
    expect(orderBalance(custom)).toBe(150);
  });

  it('ignores custom amounts on regular orders', () => {
    const regular = { ...custom, customOrder: false };
    expect(orderTotal(regular)).toBe(30);
    expect(orderBalance(regular)).toBe(30);
  });
});

describe('order draft', () => {
  afterEach(() => vi.useRealTimers());

  it('defaults to today and a regular order', () => {
    const draft = emptyOrderDraft();
    expect(draft.deliveryDate).toBe(new Date().toLocaleDateString('sv-SE'));
    expect(draft.customOrder).toBe(false);
  });

  it('validates the custom dress and deposit', () => {
    const draft = {
      ...emptyOrderDraft(),
      eventType: 'XV años',
      deliveryDate: tomorrow(),
      productIds: ['p1'],
      customOrder: true,
      customization: 'Sirena',
      customPrice: '200',
      deposit: '80',
    };
    expect(validateOrderDraft(draft, [tiara])).toBe(null);
    expect(validateOrderDraft({ ...draft, deposit: '231' }, [tiara])).toMatch(/abono/);
    expect(validateOrderDraft({ ...draft, customization: ' ' }, [tiara])).toMatch(/personalización/);
    expect(validateOrderDraft({ ...draft, deliveryDate: emptyOrderDraft().deliveryDate }, [tiara])).toMatch(/programada/);

    const data = toOrderData({ ...draft, measurements: { ...draft.measurements, bust: ' 90 ' } }, [tiara]);
    expect(data.productIds).toEqual(['p1']);
    expect(data.custom).toMatchObject({ customization: 'Sirena', customPrice: 200, deposit: 80 });
    expect(data.custom?.measurements.bust).toBe('90');
    expect(data.custom?.measurements.hip).toBeNull();
  });

  it('requires products for regular orders', () => {
    expect(validateOrderDraft({ ...emptyOrderDraft(), eventType: 'Boda' }, [tiara])).toMatch(/producto/);
  });
});
