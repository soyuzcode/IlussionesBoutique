import { useState, type FormEvent } from 'react';
import type { Appointment } from '../../types';
import { appointmentsApi } from '../../api/appointmentsApi';
import { ApiError } from '../../api/httpClient';
import { APPOINTMENT_REASONS } from '../../utils/constants';
import { formatDateOnly, isPastSlot, slotKey, to12h, to24h, todayISO, type TimeSlot } from '../../utils/dates';
import TimeSlotPicker from '../common/TimeSlotPicker';

interface AppointmentModalProps {
  appointment: Appointment;
  onClose: () => void;
  onSaved: () => Promise<void> | void;
}

const STATUS_LABELS = {
  SCHEDULED: 'Programada',
  COMPLETED: 'Completada',
  CANCELLED: 'Cancelada',
} as const;

const inputClass =
  'w-full p-2.5 border border-gray-200 rounded-xl bg-gray-50 outline-none focus:border-pink-500';

export function AppointmentModal({ appointment, onClose, onSaved }: AppointmentModalProps) {
  const originalSlot = to12h(appointment.appointmentTime);

  const [isEditing, setIsEditing] = useState(false);
  const [reason, setReason] = useState(appointment.reason);
  const [date, setDate] = useState(appointment.appointmentDate);
  const [slot, setSlot] = useState<TimeSlot>(originalSlot);
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  const reasons = APPOINTMENT_REASONS.includes(appointment.reason)
    ? APPOINTMENT_REASONS
    : [appointment.reason, ...APPOINTMENT_REASONS];

  const cancelEdit = () => {
    setReason(appointment.reason);
    setDate(appointment.appointmentDate);
    setSlot(originalSlot);
    setError('');
    setIsEditing(false);
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError('');

    const dateChanged = date !== appointment.appointmentDate;
    const timeChanged = slotKey(slot) !== slotKey(originalSlot);

    if (!reason.trim()) {
      setError('El motivo es obligatorio.');
      return;
    }
    if (!date) {
      setError('Elige la fecha de la cita.');
      return;
    }
    if (dateChanged && date < todayISO()) {
      setError('La fecha no puede ser anterior a hoy.');
      return;
    }
    if ((dateChanged || timeChanged) && isPastSlot(slot, date)) {
      setError('Elige una hora que aún no haya pasado.');
      return;
    }

    setSaving(true);
    try {
      await appointmentsApi.update(appointment.id, {
        reason: reason.trim(),
        appointmentDate: date,
        appointmentTime: to24h(slot.time, slot.period),
      });
      await onSaved();
      onClose();
    } catch (err) {
      setError(
        err instanceof ApiError && err.status === 405
          ? 'El backend aún no tiene PUT /api/appointment/{id}.'
          : 'No se pudo guardar la cita.',
      );
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/50 backdrop-blur-xs flex items-center justify-center p-4 z-50">
      <form
        onSubmit={handleSubmit}
        className="bg-white w-full max-w-sm rounded-2xl p-5 space-y-4 shadow-xl max-h-[90vh] overflow-y-auto"
      >
        <div className="flex justify-between items-center border-b border-gray-100 pb-2">
          <h3 className="font-bold text-gray-800 text-xs uppercase tracking-wider">
            {isEditing ? 'Editar Cita' : 'Detalle de la Cita'}
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
          <div>
            <span className="text-gray-400 text-[10px] block uppercase font-bold">Cliente</span>
            <p className="font-bold text-gray-800 text-sm">{appointment.customer.name}</p>
            {appointment.customer.phone && <p className="text-gray-500">{appointment.customer.phone}</p>}
          </div>

          {isEditing ? (
            <>
              <div>
                <label className="text-gray-400 text-[10px] block uppercase font-bold mb-1">Servicio / Motivo</label>
                <select
                  value={reason}
                  onChange={(e) => setReason(e.target.value)}
                  className={`${inputClass} cursor-pointer`}
                >
                  {reasons.map((item) => (
                    <option key={item}>{item}</option>
                  ))}
                </select>
              </div>

              <div>
                <label className="text-gray-400 text-[10px] block uppercase font-bold mb-1">Día</label>
                <input
                  type="date"
                  min={todayISO()}
                  value={date}
                  onChange={(e) => setDate(e.target.value)}
                  className={inputClass}
                />
              </div>

              <div>
                <span className="text-gray-400 text-[10px] block uppercase font-bold mb-1">Hora</span>
                <TimeSlotPicker date={date} value={slot} onChange={setSlot} />
              </div>
            </>
          ) : (
            <>
              <div className="flex justify-between items-center bg-gray-50 p-2.5 rounded-xl border border-gray-100">
                <div>
                  <span className="text-gray-400 text-[10px] block uppercase font-bold">Horario</span>
                  <p className="text-gray-700 font-semibold">
                    {formatDateOnly(appointment.appointmentDate)} • {originalSlot.time} {originalSlot.period}
                  </p>
                </div>
                <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-pink-100 text-pink-700">
                  {STATUS_LABELS[appointment.status] ?? appointment.status}
                </span>
              </div>

              <div>
                <span className="text-gray-400 text-[10px] block uppercase font-bold">Servicio / Motivo</span>
                <p className="text-gray-600 bg-gray-50 p-2.5 rounded-xl border border-gray-100 mt-1">
                  {appointment.reason}
                </p>
              </div>
            </>
          )}
        </div>

        {error && (
          <p role="alert" className="bg-red-50 border border-red-200 text-red-600 text-xs font-semibold p-2.5 rounded-xl">
            {error}
          </p>
        )}

        {isEditing ? (
          <div className="flex gap-2">
            <button
              type="button"
              onClick={cancelEdit}
              className="flex-1 py-2.5 bg-gray-100 hover:bg-gray-200 text-gray-700 rounded-xl text-xs font-bold cursor-pointer"
            >
              Cancelar
            </button>
            <button
              type="submit"
              disabled={saving}
              className="flex-1 py-2.5 bg-pink-600 hover:bg-pink-700 text-white rounded-xl text-xs font-bold cursor-pointer disabled:opacity-50"
            >
              {saving ? 'Guardando...' : 'Guardar'}
            </button>
          </div>
        ) : (
          <div className="flex gap-2">
            <button
              type="button"
              onClick={() => setIsEditing(true)}
              className="flex-1 py-2.5 bg-pink-50 hover:bg-pink-100 text-pink-600 rounded-xl text-xs font-bold cursor-pointer"
            >
              ✏️ Editar
            </button>
            <button
              type="button"
              onClick={onClose}
              className="flex-1 py-2.5 bg-pink-600 hover:bg-pink-700 text-white rounded-xl text-xs font-bold cursor-pointer"
            >
              Cerrar
            </button>
          </div>
        )}
      </form>
    </div>
  );
}

export default AppointmentModal;