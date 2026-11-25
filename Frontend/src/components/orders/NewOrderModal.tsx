import { useState, type FormEvent } from 'react';
import type { Customer } from '../../types';
import { LocalSaveError, ordersApi } from '../../api/ordersApi';
import { customersApi } from '../../api/customersApi';
import { ApiError } from '../../api/httpClient';
import { formatPhone, isValidPhone } from '../../utils/phone';
import OrderFormFields from './OrderFormFields';
import { useAvailableProducts } from './useAvailableProducts';
import { emptyOrderDraft, orderErrorMessage, toOrderData, validateOrderDraft } from './orderDraft';

interface NewOrderModalProps {
  /** Cliente fijo (detalle de cliente). Si no se indica, se elige o agrega en el formulario. */
  customer?: Customer;
  customers?: Customer[];
  onClose: () => void;
  onCreated: () => Promise<void> | void;
  onCustomerCreated?: () => Promise<void> | void;
}

const inputClass = 'w-full p-2.5 border border-gray-200 rounded-xl bg-gray-50 outline-none focus:border-pink-500';
const labelClass = 'text-gray-400 text-[10px] block uppercase font-bold mb-1';

export function NewOrderModal({ customer, customers = [], onClose, onCreated, onCustomerCreated }: NewOrderModalProps) {
  const products = useAvailableProducts();
  const [draft, setDraft] = useState(emptyOrderDraft);
  const [customerId, setCustomerId] = useState(customer?.id ?? '');
  const [newCustomerName, setNewCustomerName] = useState('');
  const [newCustomerPhone, setNewCustomerPhone] = useState('');
  // Si el pedido falla después de crear al cliente, se reutiliza en el siguiente intento.
  const [createdCustomer, setCreatedCustomer] = useState<Customer | null>(null);
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);
  const [savedOffline, setSavedOffline] = useState(false);

  const addingCustomer = !customer && !customerId && !createdCustomer;

  const validateCustomer = (): string | null => {
    if (customer || customerId || createdCustomer) return null;
    const name = newCustomerName.trim();
    if (name.length < 2) return 'Elige un cliente o escribe el nombre del cliente a agregar.';
    if (customers.some((c) => c.name.trim().toLowerCase() === name.toLowerCase())) {
      return 'Ya existe un cliente con ese nombre. Selecciónalo en la lista.';
    }
    if (!isValidPhone(newCustomerPhone)) return 'El teléfono debe tener 8 dígitos (0000-0000).';
    return null;
  };

  const resolveCustomer = async (): Promise<Customer> => {
    if (customer) return customer;
    if (createdCustomer) return createdCustomer;
    const existing = customers.find((c) => c.id === customerId);
    if (existing) return existing;
    const created = await customersApi.create({ name: newCustomerName.trim(), phone: newCustomerPhone });
    setCreatedCustomer(created);
    await onCustomerCreated?.();
    return created;
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    const invalid = validateCustomer() ?? validateOrderDraft(draft, products);
    if (invalid) return setError(invalid);

    setSaving(true);
    setError('');
    let owner: Customer;
    try {
      owner = await resolveCustomer();
    } catch {
      setError('No se pudo agregar el cliente.');
      setSaving(false);
      return;
    }
    try {
      const order = await ordersApi.create({
        ...toOrderData(draft, products),
        customerId: owner.id,
        customer: { id: owner.id, name: owner.name },
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
          : orderErrorMessage(reason),
      );
      setSaving(false);
    }
  };

  const title = customer?.name ?? createdCustomer?.name;

  return (
    <div className="fixed inset-0 bg-black/50 backdrop-blur-xs flex items-center justify-center p-4 z-[60]">
      <form onSubmit={handleSubmit} noValidate className="bg-white w-full max-w-md rounded-2xl p-5 space-y-3 shadow-xl text-xs max-h-[90vh] overflow-y-auto">
        <div className="flex justify-between items-center border-b border-gray-100 pb-2">
          <h3 className="font-bold text-gray-800 uppercase tracking-wider">Agregar pedido{title ? ` · ${title}` : ''}</h3>
          <button type="button" onClick={onClose} className="text-gray-400 hover:text-gray-600 font-bold p-1 cursor-pointer">✕</button>
        </div>

        {!customer && !createdCustomer && (
          <>
            <label className="block">
              <span className={labelClass}>Cliente</span>
              <select value={customerId} onChange={(e) => setCustomerId(e.target.value)} className={`${inputClass} cursor-pointer`}>
                <option value="">➕ Agregar cliente</option>
                {customers.map((c) => (
                  <option key={c.id} value={c.id}>{c.name}</option>
                ))}
              </select>
            </label>
            {addingCustomer && (
              <div className="grid grid-cols-2 gap-2">
                <label className="block">
                  <span className={labelClass}>Nombre del cliente</span>
                  <input value={newCustomerName} onChange={(e) => setNewCustomerName(e.target.value)} placeholder="Ej. Lucía Gómez" className={inputClass} />
                </label>
                <label className="block">
                  <span className={labelClass}>Teléfono</span>
                  <input type="tel" inputMode="numeric" value={newCustomerPhone} onChange={(e) => setNewCustomerPhone(formatPhone(e.target.value))} maxLength={9} placeholder="7000-0000" className={inputClass} />
                </label>
              </div>
            )}
          </>
        )}

        <OrderFormFields draft={draft} products={products} onChange={setDraft} />

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
              {saving ? 'Guardando...' : 'Agregar pedido'}
            </button>
          </div>
        )}
      </form>
    </div>
  );
}

export default NewOrderModal;
