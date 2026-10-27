import { useState } from 'react';
import type { Customer, CustomerStatus } from '../../types';
import { customersApi } from '../../api/customersApi';
import { ApiError } from '../../api/httpClient';

interface CustomersTabProps {
  customers: Customer[];
  onNew: () => void;
  onRefresh: () => Promise<void> | void;
}

export function CustomersTab({ customers, onNew, onRefresh }: CustomersTabProps) {
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState<CustomerStatus | ''>('');
  const [error, setError] = useState('');
  const [deletingId, setDeletingId] = useState<string | null>(null);
  const [updatingId, setUpdatingId] = useState<string | null>(null);

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

  const handleStatusChange = async (customer: Customer, status: CustomerStatus) => {
    if (customer.status === status) return;
    setError('');
    setUpdatingId(customer.id);
    try {
      await customersApi.update(customer.id, { status });
      await onRefresh();
    } catch (err) {
      setError(
        err instanceof ApiError
          ? `No se pudo actualizar el estado. Error ${err.status}`
          : 'No se pudo actualizar el estado.',
      );
    } finally {
      setUpdatingId(null);
    }
  };

  const filtered = customers.filter((customer) => {
    const matchesSearch = customer.name.toLowerCase().includes(search.toLowerCase());
    const matchesStatus = statusFilter ? customer.status === statusFilter : true;
    return matchesSearch && matchesStatus;
  });

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

      <div className="flex flex-col sm:flex-row gap-2">
        <input
          type="text"
          placeholder="Buscar cliente por nombre..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="flex-1 p-3.5 text-xs sm:text-sm border border-gray-200 rounded-xl bg-gray-50 outline-none focus:border-pink-500"
        />
        <select
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value as CustomerStatus | '')}
          className="p-3.5 text-xs sm:text-sm border border-gray-200 rounded-xl bg-gray-50 outline-none focus:border-pink-500 cursor-pointer"
        >
          <option value="">Todos los estados</option>
          <option value="ACTIVO">ACTIVO</option>
          <option value="FINALIZADO">FINALIZADO</option>
        </select>
      </div>

      {error && (
        <p role="alert" className="bg-red-50 border border-red-200 text-red-600 text-xs font-semibold p-3 rounded-xl">
          {error}
        </p>
      )}

      <div className="bg-white border border-gray-100 rounded-2xl p-4 shadow-xs divide-y divide-gray-100">
        {filtered.length === 0 && <p className="py-4 text-xs text-gray-400 text-center">No hay clientes.</p>}
        {filtered.map((customer) => (
          <div key={customer.id} className="py-3 flex flex-col sm:flex-row sm:justify-between sm:items-center gap-2 text-xs sm:text-sm">
            <div>
              <p className="font-bold text-gray-800">{customer.name}</p>
              <p className="text-gray-500">{customer.phone || 'Sin teléfono'}</p>
              <span
                className={`inline-block mt-1 px-2 py-0.5 rounded-full text-[10px] font-bold ${
                  customer.status === 'FINALIZADO'
                    ? 'bg-gray-100 text-gray-700'
                    : 'bg-green-100 text-green-700'
                }`}
              >
                {customer.status || 'ACTIVO'}
              </span>
            </div>
            <div className="flex items-center gap-2">
              <select
                value={customer.status || 'ACTIVO'}
                onChange={(e) => handleStatusChange(customer, e.target.value as CustomerStatus)}
                disabled={updatingId === customer.id}
                className="px-2 py-1.5 border border-gray-200 rounded-lg bg-white text-xs font-semibold cursor-pointer disabled:opacity-50"
              >
                <option value="ACTIVO">ACTIVO</option>
                <option value="FINALIZADO">FINALIZADO</option>
              </select>
              <button
                onClick={() => handleDelete(customer)}
                disabled={deletingId === customer.id}
                className="px-3 py-1.5 bg-red-50 hover:bg-red-100 text-red-600 rounded-lg text-xs font-bold cursor-pointer disabled:opacity-50"
              >
                {deletingId === customer.id ? 'Eliminando...' : 'Eliminar'}
              </button>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

export default CustomersTab;