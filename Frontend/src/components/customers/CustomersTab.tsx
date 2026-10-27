import { useEffect, useMemo, useState } from 'react';
import type { Appointment, Customer } from '../../types';
import { customersApi } from '../../api/customersApi';
import { appointmentsApi } from '../../api/appointmentsApi';
import { ApiError } from '../../api/httpClient';
import { formatDateOnly, to12h, todayISO } from '../../utils/dates';
import {
  isInRange,
  rangeLabel,
  resolveRange,
  toLocalDate,
  type FilterBy,
  type RangePreset,
} from '../../utils/dateRange';
import CustomerFilters from './CustomerFilters';
import CustomerModal from './CustomerModal';

interface CustomersTabProps {
  customers: Customer[];
  onNew: () => void;
  onRefresh: () => Promise<void> | void;
}

export function CustomersTab({ customers, onNew, onRefresh }: CustomersTabProps) {
  const [search, setSearch] = useState('');
  const [filterBy, setFilterBy] = useState<FilterBy>('registered');
  const [preset, setPreset] = useState<RangePreset>('all');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');

  const [appointments, setAppointments] = useState<Appointment[]>([]);
  const [selected, setSelected] = useState<Customer | null>(null);
  const [error, setError] = useState('');
  const [deletingId, setDeletingId] = useState<string | null>(null);

  // Se vuelven a cargar las citas cada vez que cambia la lista de clientes
  useEffect(() => {
    let cancelled = false;
    appointmentsApi
      .getAll()
      .then((data) => {
        if (!cancelled) setAppointments(data);
      })
      .catch(() => {
        if (!cancelled) setAppointments([]);
      });
    return () => {
      cancelled = true;
    };
  }, [customers]);

  // Citas agrupadas por clienta, de la más reciente a la más antigua
  const appointmentsByCustomer = useMemo(() => {
    const map = new Map<string, Appointment[]>();
    for (const appointment of appointments) {
      const list = map.get(appointment.customer.id) ?? [];
      list.push(appointment);
      map.set(appointment.customer.id, list);
    }
    for (const list of map.values()) {
      list.sort((a, b) =>
        `${b.appointmentDate} ${b.appointmentTime}`.localeCompare(`${a.appointmentDate} ${a.appointmentTime}`),
      );
    }
    return map;
  }, [appointments]);

  const { range, error: rangeError } = resolveRange(preset, from, to);

  const visibleCustomers = customers
    .filter((customer) => customer.name.toLowerCase().includes(search.trim().toLowerCase()))
    .filter((customer) => {
      if (!range) return true;
      if (filterBy === 'registered') return isInRange(toLocalDate(customer.createdAt), range);
      return (appointmentsByCustomer.get(customer.id) ?? []).some((a) => isInRange(a.appointmentDate, range));
    })
    .sort((a, b) => (b.createdAt ?? '').localeCompare(a.createdAt ?? '') || a.name.localeCompare(b.name));

  const hasActiveFilters = search.trim() !== '' || preset !== 'all';

  const clearFilters = () => {
    setSearch('');
    setPreset('all');
    setFrom('');
    setTo('');
  };

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

  const nextAppointment = (list: Appointment[]) => {
    const today = todayISO();
    const upcoming = list.filter((a) => a.status === 'SCHEDULED' && a.appointmentDate >= today);
    return upcoming[upcoming.length - 1]; // la lista va de reciente a antigua, la última es la más cercana
  };

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

      <CustomerFilters
        filterBy={filterBy}
        onFilterByChange={setFilterBy}
        preset={preset}
        onPresetChange={setPreset}
        from={from}
        to={to}
        onFromChange={setFrom}
        onToChange={setTo}
        error={rangeError}
      />

      <div className="flex flex-wrap items-center justify-between gap-2 text-xs text-gray-500">
        <p>
          Mostrando <b>{visibleCustomers.length}</b> de <b>{customers.length}</b> clientes
          {range && (
            <>
              {' · '}
              {filterBy === 'registered' ? 'registradas' : 'con cita'}: {rangeLabel(range)}
            </>
          )}
        </p>
        {hasActiveFilters && (
          <button onClick={clearFilters} className="font-bold text-pink-600 hover:underline cursor-pointer">
            Limpiar filtros
          </button>
        )}
      </div>

      {error && (
        <p role="alert" className="bg-red-50 border border-red-200 text-red-600 text-xs font-semibold p-3 rounded-xl">
          {error}
        </p>
      )}

      <div className="bg-white border border-gray-100 rounded-2xl p-4 shadow-xs divide-y divide-gray-100">
        {visibleCustomers.length === 0 && (
          <p className="py-4 text-xs text-gray-400 text-center">No hay clientes que coincidan.</p>
        )}

        {visibleCustomers.map((customer) => {
          const customerAppointments = appointmentsByCustomer.get(customer.id) ?? [];
          const next = nextAppointment(customerAppointments);
          const registeredOn = toLocalDate(customer.createdAt);

          return (
            <div
              key={customer.id}
              className="py-3 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 text-xs sm:text-sm"
            >
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 shrink-0 rounded-full bg-pink-100 text-pink-600 font-bold flex items-center justify-center">
                  {customer.name.charAt(0).toUpperCase()}
                </div>
                <div>
                  <p className="font-bold text-gray-800">{customer.name}</p>
                  <p className="text-gray-500">{customer.phone || 'Sin teléfono'}</p>
                  <p className="text-[11px] text-gray-400">
                    {registeredOn ? `Registrada: ${formatDateOnly(registeredOn)}` : 'Sin fecha de registro'} ·{' '}
                    {customerAppointments.length} cita(s)
                  </p>
                  {next && (
                    <p className="text-[11px] text-pink-600 font-semibold">
                      Próxima cita: {formatDateOnly(next.appointmentDate)} · {to12h(next.appointmentTime).time}{' '}
                      {to12h(next.appointmentTime).period}
                    </p>
                  )}
                </div>
              </div>

              <div className="flex gap-2">
                <button
                  onClick={() => setSelected(customer)}
                  className="px-3 py-1.5 bg-pink-50 hover:bg-pink-100 text-pink-600 rounded-lg text-xs font-bold cursor-pointer"
                >
                  Ver detalles
                </button>
                <button
                  onClick={() => handleDelete(customer)}
                  disabled={deletingId === customer.id}
                  className="px-3 py-1.5 bg-red-50 hover:bg-red-100 text-red-600 rounded-lg text-xs font-bold cursor-pointer disabled:opacity-50"
                >
                  {deletingId === customer.id ? 'Eliminando...' : 'Eliminar'}
                </button>
              </div>
            </div>
          );
        })}
      </div>

      {selected && (
        <CustomerModal
          key={selected.id}
          customer={selected}
          customers={customers}
          appointments={appointmentsByCustomer.get(selected.id) ?? []}
          onClose={() => setSelected(null)}
          onSaved={onRefresh}
        />
      )}
    </div>
  );
}

export default CustomersTab;