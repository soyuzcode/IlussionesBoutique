import { useState, type FormEvent } from 'react';
import type { Order } from '../../types';
import { ApiError } from '../../api/httpClient';
import { formatDate } from '../../utils/dates';
import { describeProduct, formatMoney, orderTotal } from '../../utils/quote';

interface OrderModalProps {
  order: Order;
  mode: 'view' | 'edit';
  onClose: () => void;
  onSave: (data: { eventType: string; deliveryDate: string }) => Promise<void>;
  onPrint: () => void;
}

const inputClass =
  'w-full p-2.5 border border-gray-200 rounded-xl bg-gray-50 outline-none focus:border-pink-500';

export function OrderModal({ order, mode, onClose, onSave, onPrint }: OrderModalProps) {
  const [eventType, setEventType] = useState(order.eventType ?? '');
  const [deliveryDate, setDeliveryDate] = useState(
    order.deliveryDate ? new Date(order.deliveryDate).toLocaleDateString('sv-SE') : '',
  );
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);
  const isEditing = mode === 'edit';

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError('');
    if (!eventType.trim()) {
      setError('El tipo de evento es obligatorio.');
      return;
    }
    if (!deliveryDate) {
      setError('La fecha de entrega es obligatoria.');
      return;
    }

    setSaving(true);
    try {
      await onSave({
        eventType: eventType.trim(),
        deliveryDate: new Date(`${deliveryDate}T12:00:00`).toISOString(),
      });
    } catch (err) {
      setError(
        err instanceof ApiError && err.status === 405
          ? 'El backend aún no tiene PUT /api/order/{id}.'
          : 'No se pudo guardar el pedido.',
      );
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50">
      <form onSubmit={handleSubmit} className="bg-white w-full max-w-md rounded-2xl p-5 space-y-4 shadow-xl">
        <div className="flex justify-between items-center border-b border-gray-100 pb-2">
          <h3 className="font-bold text-gray-800 text-xs uppercase tracking-wider">
            {isEditing ? 'Editar pedido' : 'Detalle del pedido'}
          </h3>
          <button
            type="button"
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600 text-xs font-bold p-1 cursor-pointer"
          >
            ✕
          </button>
        </div>

        <div className="space-y-3 text-xs">
          {order.customer && (
            <div>
              <span className="text-gray-400 text-[10px] block uppercase font-bold">Cliente</span>
              <p className="font-bold text-gray-800 text-sm">{order.customer.name}</p>
            </div>
          )}

          <div>
            <label className="text-gray-400 text-[10px] block uppercase font-bold mb-1">Evento</label>
            {isEditing ? (
              <input value={eventType} onChange={(e) => setEventType(e.target.value)} className={inputClass} />
            ) : (
              <p className="text-gray-700 font-semibold">{order.eventType}</p>
            )}
          </div>

          <div>
            <label className="text-gray-400 text-[10px] block uppercase font-bold mb-1">Entrega</label>
            {isEditing ? (
              <input
                type="date"
                value={deliveryDate}
                onChange={(e) => setDeliveryDate(e.target.value)}
                className={inputClass}
              />
            ) : (
              <p className="text-gray-700 font-semibold">{formatDate(order.deliveryDate)}</p>
            )}
          </div>

          <div>
            <span className="text-gray-400 text-[10px] block uppercase font-bold mb-1">Productos</span>
            <div className="bg-gray-50 border border-gray-100 rounded-xl divide-y divide-gray-100">
              {(order.products ?? []).length === 0 && <p className="p-2.5 text-gray-400">Sin productos</p>}
              {(order.products ?? []).map((product) => (
                <div key={product.productId} className="p-2.5 flex justify-between">
                  <span className="text-gray-700">{describeProduct(product)}</span>
                  <span className="font-semibold">{formatMoney(product.price)}</span>
                </div>
              ))}
            </div>
            <p className="text-right font-bold text-gray-800 mt-2 text-sm">
              Total: {formatMoney(orderTotal(order))}
            </p>
          </div>
        </div>

        {error && (
          <p role="alert" className="bg-red-50 border border-red-200 text-red-600 text-xs font-semibold p-2.5 rounded-xl">
            {error}
          </p>
        )}

        <div className="flex gap-2">
          <button
            type="button"
            onClick={onPrint}
            className="flex-1 py-2.5 bg-gray-100 hover:bg-gray-200 text-gray-700 rounded-xl text-xs font-bold cursor-pointer"
          >
            🖨️ Cotización
          </button>
          {isEditing ? (
            <button
              type="submit"
              disabled={saving}
              className="flex-1 py-2.5 bg-pink-600 hover:bg-pink-700 text-white rounded-xl text-xs font-bold cursor-pointer disabled:opacity-50"
            >
              {saving ? 'Guardando...' : 'Guardar'}
            </button>
          ) : (
            <button
              type="button"
              onClick={onClose}
              className="flex-1 py-2.5 bg-pink-600 hover:bg-pink-700 text-white rounded-xl text-xs font-bold cursor-pointer"
            >
              Cerrar
            </button>
          )}
        </div>
      </form>
    </div>
  );
}

export default OrderModal;