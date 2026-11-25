import { useEffect, useState, type FormEvent } from 'react';
import type { Customer, Product } from '../../types';
import { LocalSaveError, ordersApi } from '../../api/ordersApi';
import { productsApi } from '../../api/productsApi';
import { ApiError } from '../../api/httpClient';
import { todayISO } from '../../utils/dates';

interface NewOrderModalProps {
  customer: Customer;
  onClose: () => void;
  onCreated: () => Promise<void> | void;
}

const inputClass = 'w-full p-2.5 border border-gray-200 rounded-xl bg-gray-50 outline-none focus:border-pink-500';

const generateOrderId = () => `PED-${Date.now().toString(36).toUpperCase()}`;

export function NewOrderModal({ customer, onClose, onCreated }: NewOrderModalProps) {
  const [eventType, setEventType] = useState('');
  const [deliveryDate, setDeliveryDate] = useState('');
  const [products, setProducts] = useState<Product[]>([]);
  const [selected, setSelected] = useState<string[]>([]);
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);
  const [savedOffline, setSavedOffline] = useState(false);

  useEffect(() => {
    productsApi
      .getAll()
      .then((all) => setProducts(all.filter((p) => p.status === 'AVAILABLE' && (p.stock ?? 0) > 0)))
      .catch(() => setProducts([]));
  }, []);

  const toggle = (id: string) =>
    setSelected((current) => (current.includes(id) ? current.filter((x) => x !== id) : [...current, id]));

  const total = products
    .filter((p) => selected.includes(p.productId))
    .reduce((sum, p) => sum + Number(p.price ?? 0), 0);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (eventType.trim().length < 2) return setError('Indica el tipo de evento.');
    if (!deliveryDate) return setError('Indica la fecha de entrega.');
    if (deliveryDate < todayISO()) return setError('La fecha de entrega no puede estar en el pasado.');

    setSaving(true);
    setError('');
    try {
      const order = await ordersApi.create({
        orderId: generateOrderId(),
        customerId: customer.id,
        eventType: eventType.trim(),
        deliveryDate,
        productIds: selected,
        customer: { id: customer.id, name: customer.name },
        products: products.filter((p) => selected.includes(p.productId)),
      });
      await onCreated();
      if (order.localOnly) {
        setSavedOffline(true);
        setSaving(false);
        return;
      }
      onClose();
    } catch (err) {
      const reason = err instanceof ApiError ? (err.details as { reason?: string } | undefined)?.reason : undefined;
      setError(
        err instanceof LocalSaveError
          ? 'Sin conexión con el servidor y no se pudo guardar el pedido en este dispositivo.'
          : reason === 'insufficient_stock'
            ? 'Uno de los productos ya no tiene stock.'
            : 'No se pudo crear el pedido.',
      );
      setSaving(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/50 backdrop-blur-xs flex items-center justify-center p-4 z-[60]">
      <form onSubmit={handleSubmit} noValidate className="bg-white w-full max-w-md rounded-2xl p-5 space-y-3 shadow-xl text-xs max-h-[90vh] overflow-y-auto">
        <div className="flex justify-between items-center border-b border-gray-100 pb-2">
          <h3 className="font-bold text-gray-800 uppercase tracking-wider">Nuevo pedido · {customer.name}</h3>
          <button type="button" onClick={onClose} className="text-gray-400 hover:text-gray-600 font-bold p-1 cursor-pointer">✕</button>
        </div>
        <label className="block">
          <span className="text-gray-400 text-[10px] uppercase font-bold">Tipo de evento</span>
          <input value={eventType} onChange={(e) => setEventType(e.target.value)} placeholder="Boda, XV años..." className={inputClass} />
        </label>
        <label className="block">
          <span className="text-gray-400 text-[10px] uppercase font-bold">Fecha de entrega</span>
          <input type="date" min={todayISO()} value={deliveryDate} onChange={(e) => setDeliveryDate(e.target.value)} className={inputClass} />
        </label>
        <div>
          <span className="text-gray-400 text-[10px] block uppercase font-bold mb-1">Productos del inventario (opcional)</span>
          <div className="bg-gray-50 border border-gray-100 rounded-xl divide-y divide-gray-100 max-h-40 overflow-y-auto">
            {products.length === 0 && <p className="p-2.5 text-gray-400">No hay productos disponibles.</p>}
            {products.map((p) => (
              <label key={p.productId} className="p-2.5 flex items-center gap-2 cursor-pointer">
                <input type="checkbox" checked={selected.includes(p.productId)} onChange={() => toggle(p.productId)} />
                <span className="flex-1 text-gray-700">{p.name}</span>
                <span className="text-gray-400">stock {p.stock}</span>
                <span className="font-semibold text-gray-700">${Number(p.price).toFixed(2)}</span>
              </label>
            ))}
          </div>
          {selected.length > 0 && <p className="text-right mt-1 font-bold text-gray-700">Total: ${total.toFixed(2)}</p>}
        </div>
        {error && <p role="alert" className="bg-red-50 border border-red-200 text-red-600 font-semibold p-2.5 rounded-xl">{error}</p>}
        {savedOffline ? (
          <>
            <p role="alert" className="bg-amber-50 border border-amber-200 text-amber-800 font-semibold p-2.5 rounded-xl">
              Sin conexión: el pedido se guardó solo en este dispositivo. Aparece como «Sin sincronizar» y no se envió al servidor.
            </p>
            <button type="button" onClick={onClose} className="w-full py-2.5 bg-pink-600 hover:bg-pink-700 text-white rounded-xl font-bold cursor-pointer">
              Entendido
            </button>
          </>
        ) : (
        <div className="flex gap-2">
          <button type="button" onClick={onClose} className="flex-1 py-2.5 bg-gray-100 hover:bg-gray-200 text-gray-700 rounded-xl font-bold cursor-pointer">Cancelar</button>
          <button type="submit" disabled={saving} className="flex-1 py-2.5 bg-pink-600 hover:bg-pink-700 text-white rounded-xl font-bold cursor-pointer disabled:opacity-50">
            {saving ? 'Guardando...' : 'Crear pedido'}
          </button>
        </div>
        )}
      </form>
    </div>
  );
}

export default NewOrderModal;
