import { useState } from 'react';
import type { Order } from '../../types';
import { ordersApi } from '../../api/ordersApi';
import { formatDate } from '../../utils/dates';
import { formatMoney, orderTotal, printQuote } from '../../utils/quote';
import OrderModal from './OrderModal';

interface OrdersTabProps {
  orders: Order[];
  onRefresh: () => Promise<void> | void;
}

export function OrdersTab({ orders, onRefresh }: OrdersTabProps) {
  const [selected, setSelected] = useState<{ order: Order; mode: 'view' | 'edit' } | null>(null);
  const [error, setError] = useState('');

  const handleDelete = async (order: Order) => {
    if (!window.confirm('¿Eliminar este pedido? Esta acción no se puede deshacer.')) return;
    setError('');
    try {
      await ordersApi.remove(order.orderId);
      await onRefresh();
    } catch {
      setError('No se pudo eliminar el pedido.');
    }
  };

  const handleSave = async (data: { eventType: string; deliveryDate: string }) => {
    if (!selected) return;
    await ordersApi.update(selected.order.orderId, data); // si falla, el modal muestra el error
    await onRefresh();
    setSelected(null);
  };

  return (
    <div className="space-y-4 w-full">
      <h2 className="text-base sm:text-lg font-bold text-gray-800">Seguimiento de Pedidos</h2>

      {error && (
        <p role="alert" className="bg-red-50 border border-red-200 text-red-600 text-xs font-semibold p-3 rounded-xl">
          {error}
        </p>
      )}
      {orders.length === 0 && (
        <p className="text-xs text-gray-400 py-6 text-center">No hay pedidos registrados.</p>
      )}

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
        {orders.map((order) => (
          <div key={order.orderId} className="bg-white border border-gray-100 rounded-2xl p-4 shadow-xs space-y-2">
            <div className="flex justify-between items-center">
              <span className="text-xs font-bold text-pink-600">#{String(order.orderId).slice(0, 8)}</span>
              {order.status && (
                <span className="text-[10px] font-bold px-2 py-0.5 bg-pink-50 text-pink-700 rounded-full">
                  {order.status}
                </span>
              )}
            </div>
            <h3 className="font-bold text-gray-800 text-sm">{order.customer?.name ?? order.eventType}</h3>
            {order.customer && <p className="text-xs text-gray-500">{order.eventType}</p>}
            <p className="text-[11px] text-gray-400 font-medium">Entrega: {formatDate(order.deliveryDate)}</p>
            <p className="text-xs font-semibold text-gray-700">
              {(order.products ?? []).length} producto(s) · {formatMoney(orderTotal(order))}
            </p>

            <div className="grid grid-cols-2 gap-2 pt-2">
              <button
                onClick={() => setSelected({ order, mode: 'view' })}
                className="py-1.5 bg-gray-100 hover:bg-gray-200 rounded-lg text-xs font-bold text-gray-700 cursor-pointer"
              >
                Ver
              </button>
              <button
                onClick={() => setSelected({ order, mode: 'edit' })}
                className="py-1.5 bg-pink-50 hover:bg-pink-100 rounded-lg text-xs font-bold text-pink-600 cursor-pointer"
              >
                Editar
              </button>
              <button
                onClick={() => printQuote(order)}
                className="py-1.5 bg-gray-100 hover:bg-gray-200 rounded-lg text-xs font-bold text-gray-700 cursor-pointer"
              >
                🖨️ Cotización
              </button>
              <button
                onClick={() => handleDelete(order)}
                className="py-1.5 bg-red-50 hover:bg-red-100 rounded-lg text-xs font-bold text-red-600 cursor-pointer"
              >
                Eliminar
              </button>
            </div>
          </div>
        ))}
      </div>

      {selected && (
        <OrderModal
          key={`${selected.order.orderId}-${selected.mode}`}
          order={selected.order}
          mode={selected.mode}
          onClose={() => setSelected(null)}
          onSave={handleSave}
          onPrint={() => printQuote(selected.order)}
        />
      )}
    </div>
  );
}

export default OrdersTab;