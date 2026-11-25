import type { BodyMeasurements, Order, Product } from '../types';
import { formatDate } from './dates';

const PRODUCT_NAMES: Record<string, string> = {
  dress: 'Vestido',
  album: 'Álbum',
  card: 'Tarjeta',
  cup: 'Taza',
  plush: 'Peluche',
};

export function describeProduct(product: Product): string {
  const type = String(product.type ?? '').toLowerCase();
  if (PRODUCT_NAMES[type]) return PRODUCT_NAMES[type];
  if ('cutType' in product) return 'Vestido';
  if ('coverMaterial' in product) return 'Álbum';
  if ('capacity' in product) return 'Taza';
  if ('model' in product) return 'Peluche';
  if ('design' in product) return 'Tarjeta';
  return 'Producto';
}

export const formatMoney = (amount: number) =>
  new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(Number(amount) || 0);

const productsTotal = (order: Order) =>
  (order.products ?? []).reduce((sum, product) => sum + Number(product.price || 0), 0);

/** Productos del inventario más el precio del vestido personalizado. */
export const orderTotal = (order: Order) =>
  productsTotal(order) + (order.customOrder ? Number(order.customPrice || 0) : 0);

/** Abono recibido; solo aplica a pedidos personalizados. */
export const orderDeposit = (order: Order) => (order.customOrder ? Number(order.deposit || 0) : 0);

/** Saldo pendiente: total menos abono. */
export const orderBalance = (order: Order) => Math.max(orderTotal(order) - orderDeposit(order), 0);

export const MEASUREMENT_FIELDS: { key: keyof BodyMeasurements; label: string }[] = [
  { key: 'shoulder', label: 'Hombro' },
  { key: 'bust', label: 'Busto' },
  { key: 'hip', label: 'Cadera' },
  { key: 'waist', label: 'Cintura' },
  { key: 'skirtLength', label: 'Alto de falda' },
  { key: 'blouseLength', label: 'Alto de blusa' },
  { key: 'neckline', label: 'Escote' },
];

const HTML_ESCAPES: Record<string, string> = {
  '&': '&amp;',
  '<': '&lt;',
  '>': '&gt;',
  '"': '&quot;',
  "'": '&#39;',
};

const escapeHtml = (value: unknown) =>
  String(value ?? '').replace(/[&<>"']/g, (char) => HTML_ESCAPES[char]);

export function printQuote(order: Order) {
  const rows = (order.products ?? [])
    .map(
      (product) => `<tr>
        <td>${escapeHtml(describeProduct(product))}${product.materials ? ` · ${escapeHtml(product.materials)}` : ''}</td>
        <td class="r">${formatMoney(product.price)}</td>
      </tr>`,
    )
    .join('');
  const customRow = order.customOrder
    ? `<tr>
        <td>Vestido personalizado · ${escapeHtml(order.customization)}${
          order.customizationDescription ? `<br><small>${escapeHtml(order.customizationDescription)}</small>` : ''
        }</td>
        <td class="r">${formatMoney(Number(order.customPrice || 0))}</td>
      </tr>`
    : '';
  const measurements = order.customOrder
    ? MEASUREMENT_FIELDS.filter(({ key }) => order.measurements?.[key])
        .map(({ key, label }) => `${label}: ${escapeHtml(order.measurements?.[key])} cm`)
        .join(' · ')
    : '';
  const paymentRows = order.customOrder
    ? `<tr><td>Abono</td><td class="r">-${formatMoney(orderDeposit(order))}</td></tr>
       <tr class="total"><td>Saldo pendiente</td><td class="r">${formatMoney(orderBalance(order))}</td></tr>`
    : '';

  const printWindow = window.open('', '_blank', 'width=800,height=900');
  if (!printWindow) {
    alert('Permite las ventanas emergentes para poder imprimir la cotización.');
    return;
  }

  printWindow.document.write(`<!doctype html><html lang="es"><head><meta charset="utf-8">
    <title>Cotización</title>
    <style>
      body{font-family:system-ui,sans-serif;color:#1f2937;padding:40px;max-width:700px;margin:auto}
      h1{color:#db2777;margin:0} .sub{color:#6b7280;font-size:13px;margin-bottom:24px}
      table{width:100%;border-collapse:collapse;margin-top:16px}
      th,td{padding:10px 8px;border-bottom:1px solid #e5e7eb;text-align:left;font-size:14px}
      .r{text-align:right} .total{font-weight:bold;font-size:16px}
      .meta p{margin:4px 0;font-size:14px}
    </style></head><body>
    <h1>Ilussiones Boutique</h1>
    <div class="sub">Cotización · ${escapeHtml(formatDate(new Date().toISOString()))}</div>
    <div class="meta">
      ${order.customer ? `<p><b>Cliente:</b> ${escapeHtml(order.customer.name)}</p>` : ''}
      <p><b>Evento:</b> ${escapeHtml(order.eventType)}</p>
      <p><b>${order.customOrder ? 'Fecha de entrega programada' : 'Fecha de entrega'}:</b> ${escapeHtml(formatDate(order.deliveryDate))}</p>
      ${measurements ? `<p><b>Medidas:</b> ${measurements}</p>` : ''}
    </div>
    <table>
      <thead><tr><th>Producto</th><th class="r">Precio</th></tr></thead>
      <tbody>${rows + customRow || '<tr><td colspan="2">Sin productos</td></tr>'}</tbody>
      <tfoot><tr class="total"><td>Total</td><td class="r">${formatMoney(orderTotal(order))}</td></tr>${paymentRows}</tfoot>
    </table></body></html>`);
  printWindow.document.close();
  printWindow.focus();
  setTimeout(() => printWindow.print(), 300);
}