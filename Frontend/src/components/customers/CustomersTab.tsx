import { useState } from 'react';
import type { Customer } from '../../types';
import { customersApi } from '../../api/customersApi';
import { ApiError } from '../../api/httpClient';

interface CustomersTabProps {
  customers: Customer[];
  onNew: () => void;
  onRefresh: () => Promise<void> | void;
}

export function CustomersTab({ customers, onNew, onRefresh }: CustomersTabProps) {
  const [search, setSearch] = useState('');
  const [error, setError] = useState('');
  const [deletingId, setDeletingId] = useState<string | null>(null);

  const handleDelete = async (customer: Customer) => {
    if (!window.confirm(`¿Eliminar a ${customer.name}? Esta acción no se puede deshacer.`)) return;
    setError('');
    setDeletingId(customer.id);
    try {
      await customersApi.remove(customer.id);
      await onRefresh();
    } catch (err) {
      setError(
        err instanceof ApiError && err.status === 404
          ? 'La clienta ya no existe.'
          : `No se pudo eliminar a ${customer.name}. Si tiene citas o pedidos asociados, elimínalos primero.`,
      );
    } finally {
      setDeletingId(null);
    }
  };

  const filtered = customers.filter((customer) =>
    customer.name.toLowerCase().includes(search.toLowerCase()),
  );

  return (
    <div className="space-y-4 w-full">
      <div className="flex justify-between items-center">
        <h2 className="text-base sm:text-lg font-bold text-gray-800">Gestión de Clientes</h2>
        <button
          onClick={onNew}
          className="px-4 py-2 bg-pink-600 text-white rounded-xl text-xs sm:text-sm font-bold cursor-pointer hover:bg-pink-700 transition"
        >
          + Nuevo Cliente
        </button>
      </div>

      <input
        type="text"
        placeholder="Buscar cliente por nombre..."
        value={search}
        onChange={(e) => setSearch(e.target.value)}
        className="w-full p-3.5 text-xs sm:text-sm border border-gray-200 rounded-xl bg-gray-50 outline-none focus:border-pink-500"
      />

      {error && (
        <p role="alert" className="bg-red-50 border border-red-200 text-red-600 text-xs font-semibold p-3 rounded-xl">
          {error}
        </p>
      )}

      <div className="bg-white border border-gray-100 rounded-2xl p-4 shadow-xs divide-y divide-gray-100">
        {filtered.length === 0 && <p className="py-4 text-xs text-gray-400 text-center">No hay clientes.</p>}
        {filtered.map((customer) => (
          <div key={customer.id} className="py-3 flex justify-between items-center text-xs sm:text-sm">
            <div>
              <p className="font-bold text-gray-800">{customer.name}</p>
              <p className="text-gray-500">{customer.phone || 'Sin teléfono'}</p>
            </div>
            <button
              onClick={() => handleDelete(customer)}
              disabled={deletingId === customer.id}
              className="px-3 py-1.5 bg-red-50 hover:bg-red-100 text-red-600 rounded-lg text-xs font-bold cursor-pointer disabled:opacity-50"
            >
              {deletingId === customer.id ? 'Eliminando...' : 'Eliminar'}
            </button>
          </div>
        ))}
      </div>
    </div>
  );
}

export default CustomersTab;