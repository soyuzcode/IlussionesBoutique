import { useEffect, useState } from 'react';
import type { FC, FormEvent } from 'react';

import { auditEvent } from '../../lib/audit';
import type { Customer, NewAppointmentData } from '../../types';
import { APPOINTMENT_REASONS } from '../../utils/constants';
import { TIME_SLOTS, isPastSlot, slotKey, todayISO, type TimeSlot } from '../../utils/dates';
import TimeSlotPicker from './TimeSlotPicker';

interface NewClientModalProps {
  isOpen: boolean;
  onClose: () => void;
  customers: Customer[];
  onAddAppointment: (data: NewAppointmentData) => Promise<void>;
}

const DEFAULT_SLOT = TIME_SLOTS.find((s) => slotKey(s) === '10:00 AM') ?? TIME_SLOTS[0];

const inputClass =
  'w-full p-3 border border-gray-200 rounded-xl bg-gray-50 focus:bg-white focus:border-pink-500 outline-none';

export const NewClientModal: FC<NewClientModalProps> = ({ isOpen, onClose, customers, onAddAppointment }) => {
  const [customerId, setCustomerId] = useState('');
  const [customerName, setCustomerName] = useState('');
  const [phone, setPhone] = useState('');
  const [reason, setReason] = useState(APPOINTMENT_REASONS[0]);
  const [date, setDate] = useState(todayISO());
  const [slot, setSlot] = useState<TimeSlot>(DEFAULT_SLOT);
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (isOpen) {
      auditEvent({
        eventType: 'UI_APPOINTMENT_FORM_OPENED',
        entityType: 'APPOINTMENT',
      });
    }
  }, [isOpen]);

  if (!isOpen) return null;

  const handleClose = () => {
    setError('');
    setCustomerId('');
    setCustomerName('');
    setPhone('');
    setDate(todayISO());
    setSlot(DEFAULT_SLOT);
    setSaving(false);
    onClose();
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError('');

    if (!customerId) {
      if (!customerName.trim()) {
        setError('Escribe el nombre de la clienta o elige una existente.');
        auditEvent({
          eventType: 'UI_APPOINTMENT_FORM_VALIDATION_FAILED',
          entityType: 'APPOINTMENT',
          metadata: { FIELD: 'CLIENT', REASON: 'REQUIRED' },
        });
        return;
      }
      if (customers.some((c) => c.name.trim().toLowerCase() === customerName.trim().toLowerCase())) {
        setError('Ya existe una clienta con ese nombre. Selecciónala en la lista.');
        auditEvent({
          eventType: 'UI_APPOINTMENT_FORM_VALIDATION_FAILED',
          entityType: 'APPOINTMENT',
          metadata: { FIELD: 'CLIENT', REASON: 'DUPLICATE' },
        });
        return;
      }
      if (phone && !/^[0-9+\-\s]{8,15}$/.test(phone)) {
        setError('El teléfono no es válido.');
        auditEvent({
          eventType: 'UI_APPOINTMENT_FORM_VALIDATION_FAILED',
          entityType: 'APPOINTMENT',
          metadata: { FIELD: 'CLIENT', REASON: 'INVALID_DATA' },
        });
        return;
      }
    }
    if (!date) {
      setError('Elige la fecha de la cita.');
      auditEvent({
        eventType: 'UI_APPOINTMENT_FORM_VALIDATION_FAILED',
        entityType: 'APPOINTMENT',
        metadata: { FIELD: 'TIME', REASON: 'REQUIRED' },
      });
      return;
    }
    if (date < todayISO()) {
      setError('La fecha no puede ser anterior a hoy.');
      auditEvent({
        eventType: 'UI_APPOINTMENT_FORM_VALIDATION_FAILED',
        entityType: 'APPOINTMENT',
        metadata: { FIELD: 'TIME', REASON: 'INVALID_DATA' },
      });
      return;
    }
    if (isPastSlot(slot, date)) {
      setError('Elige una hora que aún no haya pasado.');
      auditEvent({
        eventType: 'UI_APPOINTMENT_FORM_VALIDATION_FAILED',
        entityType: 'APPOINTMENT',
        metadata: { FIELD: 'TIME', REASON: 'INVALID_DATA' },
      });
      return;
    }

    setSaving(true);
    try {
      auditEvent({
        eventType: 'UI_APPOINTMENT_FORM_SUBMITTED',
        entityType: 'APPOINTMENT',
      });

      await onAddAppointment({
        customerId: customerId || undefined,
        customerName,
        phone,
        reason,
        date,
        time: slot.time,
        period: slot.period,
      });
      
      setCustomerId('');
      setCustomerName('');
      setPhone('');
      setDate(todayISO());
      setSlot(DEFAULT_SLOT);
      onClose();
    } catch {
      setError('No se pudo guardar la cita. Verifica que el servidor esté activo.');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/50 backdrop-blur-xs flex items-center justify-center p-4 z-50">
      <div className="bg-white w-full max-w-md rounded-2xl p-6 space-y-5 shadow-2xl max-h-[90vh] overflow-y-auto">
        <div className="flex justify-between items-center border-b border-gray-100 pb-3">
          <h3 className="font-bold text-gray-800 text-sm uppercase tracking-wider">Agendar Nueva Cita</h3>
          <button onClick={handleClose} className="text-gray-400 hover:text-gray-600 text-sm font-bold p-1 cursor-pointer">
            ✕
          </button>
        </div>

        <form onSubmit={handleSubmit} noValidate className="space-y-4 text-xs sm:text-sm">
          <div>
            <label className="block font-bold text-gray-700 mb-1">Clienta</label>
            <select
              value={customerId}
              onChange={(e) => setCustomerId(e.target.value)}
              className={`${inputClass} cursor-pointer`}
            >
              <option value="">➕ Nueva clienta</option>
              {customers.map((customer) => (
                <option key={customer.id} value={customer.id}>
                  {customer.name}
                </option>
              ))}
            </select>
          </div>

          {!customerId && (
            <>
              <div>
                <label className="block font-bold text-gray-700 mb-1">Nombre de la Clienta</label>
                <input
                  type="text"
                  placeholder="Ej. Lucía Gómez"
                  value={customerName}
                  onChange={(e) => setCustomerName(e.target.value)}
                  className={inputClass}
                />
              </div>
              <div>
                <label className="block font-bold text-gray-700 mb-1">Teléfono (opcional)</label>
                <input
                  type="tel"
                  placeholder="7000-0000"
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                  className={inputClass}
                />
              </div>
            </>
          )}

          <div>
            <label className="block font-bold text-gray-700 mb-1">Tipo de Servicio / Motivo</label>
            <select
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              className={`${inputClass} cursor-pointer`}
            >
              {APPOINTMENT_REASONS.map((item) => (
                <option key={item}>{item}</option>
              ))}
            </select>
          </div>

          <div>
            <label className="block font-bold text-gray-700 mb-1">Día de la cita</label>
            <input
              type="date"
              min={todayISO()}
              value={date}
              onChange={(e) => setDate(e.target.value)}
              className={inputClass}
            />
          </div>

          <div>
            <label className="block font-bold text-gray-700 mb-1">Hora</label>
            <TimeSlotPicker date={date} value={slot} onChange={setSlot} />
          </div>

          {error && (
            <p role="alert" className="bg-red-50 border border-red-200 text-red-600 text-xs font-semibold p-3 rounded-xl">
              {error}
            </p>
          )}

          <div className="flex gap-3 pt-2">
            <button
              type="button"
              onClick={handleClose}
              className="w-1/2 py-3 bg-gray-100 hover:bg-gray-200 text-gray-700 rounded-xl font-bold cursor-pointer transition"
            >
              Cancelar
            </button>
            <button
              type="submit"
              disabled={saving}
              className="w-1/2 py-3 bg-pink-600 hover:bg-pink-700 text-white rounded-xl font-bold cursor-pointer shadow-md transition disabled:opacity-50"
            >
              {saving ? 'Guardando...' : 'Guardar Cita'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export { NewClientModal as NuevoClienteModal };
export default NewClientModal;
