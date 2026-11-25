import type { BodyMeasurements, Product } from '../../types';
import { todayISO } from '../../utils/dates';
import { MEASUREMENT_FIELDS, formatMoney } from '../../utils/quote';
import { draftTotals, type OrderDraft } from './orderDraft';

const inputClass = 'w-full p-2.5 border border-gray-200 rounded-xl bg-gray-50 outline-none focus:border-pink-500';
const labelClass = 'text-gray-400 text-[10px] block uppercase font-bold mb-1';

interface OrderFormFieldsProps {
  draft: OrderDraft;
  products: Product[];
  onChange: (draft: OrderDraft) => void;
}

/** Campos del pedido compartidos por detalle de cliente, nueva cita y la pestaña Pedidos. */
export function OrderFormFields({ draft, products, onChange }: OrderFormFieldsProps) {
  const set = <K extends keyof OrderDraft>(key: K, value: OrderDraft[K]) => onChange({ ...draft, [key]: value });
  const setMeasurement = (key: keyof BodyMeasurements, value: string) =>
    onChange({ ...draft, measurements: { ...draft.measurements, [key]: value } });
  const toggleProduct = (id: string) =>
    set('productIds', draft.productIds.includes(id) ? draft.productIds.filter((x) => x !== id) : [...draft.productIds, id]);
  // Entregar otro día que el del registro implica un vestido personalizado.
  const changeDate = (deliveryDate: string) =>
    onChange({ ...draft, deliveryDate, customOrder: deliveryDate !== '' && deliveryDate !== todayISO() });

  const { total, deposit, balance } = draftTotals(draft, products);

  return (
    <div className="space-y-3">
      <label className="block">
        <span className={labelClass}>Tipo de evento</span>
        <input value={draft.eventType} onChange={(e) => set('eventType', e.target.value)} placeholder="Boda, XV años..." className={inputClass} />
      </label>
      <label className="block">
        <span className={labelClass}>{draft.customOrder ? 'Fecha de entrega programada' : 'Fecha de entrega'}</span>
        <input type="date" min={todayISO()} value={draft.deliveryDate} onChange={(e) => changeDate(e.target.value)} className={inputClass} />
        <span className="text-[10px] text-gray-400">
          Por defecto es la fecha del registro. Si eliges otra fecha se activa el pedido personalizado.
        </span>
      </label>

      <div>
        <span className={labelClass}>Productos terminados del inventario</span>
        <div className="bg-gray-50 border border-gray-100 rounded-xl divide-y divide-gray-100 max-h-40 overflow-y-auto">
          {products.length === 0 && <p className="p-2.5 text-gray-400">No hay productos disponibles.</p>}
          {products.map((p) => (
            <label key={p.productId} className="p-2.5 flex items-center gap-2 cursor-pointer">
              <input type="checkbox" checked={draft.productIds.includes(p.productId)} onChange={() => toggleProduct(p.productId)} />
              <span className="flex-1 text-gray-700">{p.name}</span>
              <span className="text-gray-400">stock {p.stock}</span>
              <span className="font-semibold text-gray-700">{formatMoney(p.price)}</span>
            </label>
          ))}
        </div>
      </div>

      <label className="flex items-center gap-2 font-bold text-gray-700 cursor-pointer">
        <input type="checkbox" checked={draft.customOrder} onChange={(e) => set('customOrder', e.target.checked)} />
        Pedido personalizado (vestido)
      </label>

      {draft.customOrder && (
        <fieldset className="border border-pink-100 bg-pink-50/40 rounded-xl p-3 space-y-3">
          <legend className="px-1 text-[10px] uppercase font-bold text-pink-600">Personalización del vestido</legend>
          <label className="block">
            <span className={labelClass}>Personalización</span>
            <input value={draft.customization} onChange={(e) => set('customization', e.target.value)} maxLength={120} placeholder="Ej. Vestido sirena con encaje" className={inputClass} />
          </label>
          <label className="block">
            <span className={labelClass}>Descripción</span>
            <textarea value={draft.customizationDescription} onChange={(e) => set('customizationDescription', e.target.value)} maxLength={1000} rows={3} placeholder="Detalles, color, tela..." className={inputClass} />
          </label>
          <div>
            <span className={labelClass}>Medidas corporales (cm)</span>
            <div className="grid grid-cols-2 gap-2">
              {MEASUREMENT_FIELDS.map(({ key, label }) => (
                <label key={key} className="block">
                  <span className="text-[10px] text-gray-500 font-semibold">{label} (cm)</span>
                  <input type="text" inputMode="decimal" value={draft.measurements[key]} onChange={(e) => setMeasurement(key, e.target.value)} maxLength={20} className={inputClass} />
                </label>
              ))}
            </div>
          </div>
          <div className="grid grid-cols-2 gap-2">
            <label className="block">
              <span className={labelClass}>Precio del vestido ($)</span>
              <input type="text" inputMode="decimal" value={draft.customPrice} onChange={(e) => set('customPrice', e.target.value)} placeholder="0.00" className={inputClass} />
            </label>
            <label className="block">
              <span className={labelClass}>Abono ($)</span>
              <input type="text" inputMode="decimal" value={draft.deposit} onChange={(e) => set('deposit', e.target.value)} placeholder="0.00" className={inputClass} />
            </label>
          </div>
        </fieldset>
      )}

      <div className="bg-gray-50 border border-gray-100 rounded-xl p-2.5 space-y-1 font-semibold text-gray-700">
        <div className="flex justify-between"><span>Total</span><span>{formatMoney(total)}</span></div>
        {draft.customOrder && (
          <>
            <div className="flex justify-between text-gray-500"><span>Abono</span><span>-{formatMoney(deposit)}</span></div>
            <div className="flex justify-between font-bold text-pink-700"><span>Saldo pendiente</span><span>{formatMoney(balance)}</span></div>
          </>
        )}
      </div>
    </div>
  );
}

export default OrderFormFields;
