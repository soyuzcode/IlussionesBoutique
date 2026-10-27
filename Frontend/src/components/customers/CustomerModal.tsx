import { useState, type FormEvent } from 'react';
import type { Appointment, Customer } from '../../types';
import { customersApi } from '../../api/customersApi';
import { ApiError } from '../../api/httpClient';
import { formatDate, formatDateOnly, to12h, todayISO } from '../../utils/dates';
import AppointmentModal from '../appointments/AppointmentModal';

interface CustomerModalProps {
  customer: Customer;
  customers: Customer[];
  appointments: Appointment[];
  onClose: () => void;
  onSaved: () => Promise<void> | void;
}

const STATUS_LABELS: Record<string, string> = {
  SCHEDULED: 'Programada',
  COMPLETED: 'Completada',
  CANCELLED: 'Cancelada',
};

const inputClass = 'w-full p-2.5 border border-gray-200 rounded-xl bg-gray-50 outline-none focus:border-pink-500';

export function CustomerModal({ customer, customers, appointments, onClose, onSaved }: CustomerModalProps) {
  const [isEditing, setIsEditing] = useState(false);
  const [name, setName] = useState(customer.name);
  const [phone, setPhone] = useState(customer.phone ?? '');
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);
  
  // Estado para editar la cita activa/próxima de la clienta
  const [editingAppointment, setEditingAppointment] = useState<Appointment | null>(null);

  // Buscar la próxima cita o la más reciente de esta clienta
  const today = todayISO();
  const activeAppointment = appointments.find(
    (a) => a.status === 'SCHEDULED' && a.appointmentDate >= today
  ) ?? appointments[0];

  const cancelEdit = () => {
    setName(customer.name);
    setPhone(customer.phone ?? '');
    setError('');
    setIsEditing(false);
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError('');
    const cleanName = name.trim();
    const cleanPhone = phone.trim();

    if (cleanName.length < 2) {
      setError('El nombre debe tener al menos 2 caracteres.');
      return;
    }
    if (customers.some((c) => c.id !== customer.id && c.name.trim().toLowerCase() === cleanName.toLowerCase())) {
      setError('Ya existe otra clienta con ese nombre.');
      return;
    }
    if (cleanPhone && cleanPhone !== (customer.phone ?? '') && !/^[0-9+\-\s]{8,15}$/.test(cleanPhone)) {
      setError('El teléfono no es válido.');
      return;
    }
    if (cleanName === customer.name && cleanPhone === (customer.phone ?? '')) {
      setIsEditing(false);
      return;
    }

    setSaving(true);
    try {
      await customersApi.update(customer.id, { name: cleanName, phone: cleanPhone || undefined });
      await onSaved();
      onClose();
    } catch (err) {
      setError(
        err instanceof ApiError && err.status === 405
          ? 'El backend aún no tiene PUT /api/customer/{id}.'
          : 'No se pudo guardar la clienta.',
      );
    } finally {
      setSaving(false);
    }
  };

  const slot = activeAppointment ? to12h(activeAppointment.appointmentTime) : null;

  return (
    <div className="fixed inset-0 bg-black/50 backdrop-blur-xs flex items-center justify-center p-4 z-50">
      <form onSubmit={handleSubmit} noValidate className="bg-white w-full max-w-md rounded-2xl p-5 space-y-4 shadow-xl max-h-[90vh] overflow-y-auto">
        <div className="flex justify-between items-center border-b border-gray-100 pb-2">
          <h3 className="font-bold text-gray-800 text-xs uppercase tracking-wider">
            {isEditing ? 'Editar clienta' : 'Detalle de la clienta'}
          </h3>
          <button type="button" onClick={onClose} className="text-gray-400 hover:text-gray-600 text-xs font-bold p-1 cursor-pointer">
            ✕
          </button>
        </div>

        <div className="flex items-center gap-3">
          <div className="w-14 h-14 shrink-0 rounded-full bg-pink-100 text-pink-600 font-bold text-xl flex items-center justify-center">
            {customer.name.charAt(0).toUpperCase()}
          </div>
          {!isEditing && (
            <div>
              <p className="font-bold text-gray-800 text-base">{customer.name}</p>
              <p className="text-xs text-gray-500">{customer.phone || 'Sin teléfono'}</p>
            </div>
          )}
        </div>

        {isEditing ? (
          <div className="space-y-3 text-xs">
            <div>
              <label className="text-gray-400 text-[10px] block uppercase font-bold mb-1">Nombre</label>
              <input value={name} onChange={(e) => setName(e.target.value)} className={inputClass} />
            </div>
            <div>
              <label className="text-gray-400 text-[10px] block uppercase font-bold mb-1">Teléfono (opcional)</label>
              <input type="tel" value={phone} onChange={(e) => setPhone(e.target.value)} className={inputClass} />
            </div>
          </div>
        ) : (
          <div className="space-y-3 text-xs">
            <div className="grid grid-cols-2 gap-2">
              <div className="bg-gray-50 border border-gray-100 rounded-xl p-2.5">
                <span className="text-gray-400 text-[10px] block uppercase font-bold">Registro</span>
                <p className="text-gray-700 font-semibold">{customer.createdAt ? formatDate(customer.createdAt) : 'Sin dato'}</p>
              </div>
              <div className="bg-gray-50 border border-gray-100 rounded-xl p-2.5">
                <span className="text-gray-400 text-[10px] block uppercase font-bold">Última actualización</span>
                <p className="text-gray-700 font-semibold">{customer.updatedAt ? formatDate(customer.updatedAt) : 'Sin dato'}</p>
              </div>
            </div>

            {/* Apartado limpio y directo para ver y editar la cita activa sin mostrar todo un historial */}
            <div>
              <span className="text-gray-400 text-[10px] block uppercase font-bold mb-1">Próxima Cita / Cita Activa</span>
              {activeAppointment && slot ? (
                <div className="bg-gray-50 border border-gray-100 rounded-xl p-3 flex justify-between items-center gap-2">
                  <div>
                    <p className="font-semibold text-gray-700">{activeAppointment.reason}</p>
                    <p className="text-gray-500 text-[11px]">
                      {formatDateOnly(activeAppointment.appointmentDate)} · {slot.time} {slot.period}
                    </p>
                  </div>
                  <div className="flex items-center gap-2">
                    <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-pink-100 text-pink-700 whitespace-nowrap">
                      {STATUS_LABELS[activeAppointment.status] ?? activeAppointment.status}
                    </span>
                    <button
                      type="button"
                      onClick={() => setEditingAppointment(activeAppointment)}
                      className="px-2.5 py-1 bg-pink-50 hover:bg-pink-100 text-pink-600 rounded-lg text-xs font-bold cursor-pointer transition flex items-center gap-1"
                      title="Editar cita"
                    >
                      ✏️ Editar
                    </button>
                  </div>
                </div>
              ) : (
                <p className="p-2.5 bg-gray-50 border border-gray-100 rounded-xl text-gray-400">Sin citas pendientes o activas.</p>
              )}
            </div>
          </div>
        )}

        {error && <p role="alert" className="bg-red-50 border border-red-200 text-red-600 text-xs font-semibold p-2.5 rounded-xl">{error}</p>}

        {isEditing ? (
          <div className="flex gap-2">
            <button type="button" onClick={cancelEdit} className="flex-1 py-2.5 bg-gray-100 hover:bg-gray-200 text-gray-700 rounded-xl text-xs font-bold cursor-pointer">Cancelar</button>
            <button type="submit" disabled={saving} className="flex-1 py-2.5 bg-pink-600 hover:bg-pink-700 text-white rounded-xl text-xs font-bold cursor-pointer disabled:opacity-50">{saving ? 'Guardando...' : 'Guardar'}</button>
          </div>
        ) : (
          <div className="flex gap-2">
            <button type="button" onClick={() => setIsEditing(true)} className="flex-1 py-2.5 bg-pink-50 hover:bg-pink-100 text-pink-600 rounded-xl text-xs font-bold cursor-pointer">✏️ Editar Datos</button>
            <button type="button" onClick={onClose} className="flex-1 py-2.5 bg-pink-600 hover:bg-pink-700 text-white rounded-xl text-xs font-bold cursor-pointer">Cerrar</button>
          </div>
        )}
      </form>

      {/* Sub-modal para editar la cita directamente */}
      {editingAppointment && (
        <AppointmentModal
          key={editingAppointment.id}
          appointment={editingAppointment}
          onClose={() => setEditingAppointment(null)}
          onSaved={async () => {
            setEditingAppointment(null);
            await onSaved();
          }}
        />
      )}
    </div>
  );
}

export default CustomerModal;