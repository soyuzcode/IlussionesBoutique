import React, { useState } from 'react';

interface NuevoClienteModalProps {
  isOpen: boolean;
  onClose: () => void;
  onAgregarCita: (nuevaCita: { client: string; type: string; time: string; period: string }) => void;
}

export const NuevoClienteModal: React.FC<NuevoClienteModalProps> = ({ isOpen, onClose, onAgregarCita }) => {
  const [client, setClient] = useState('');
  const [type, setType] = useState('Primera toma de medidas');
  const [time, setTime] = useState('10:00');
  const [period, setPeriod] = useState('AM');

  if (!isOpen) return null;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!client.trim()) return;

    onAgregarCita({ client, type, time, period });
    setClient('');
    onClose();
  };

  return (
    <div className="fixed inset-0 bg-black/50 backdrop-blur-xs flex items-center justify-center p-4 z-50">
      <div className="bg-white w-full max-w-md rounded-2xl p-6 space-y-5 shadow-2xl">
        <div className="flex justify-between items-center border-b border-gray-100 pb-3">
          <h3 className="font-bold text-gray-800 text-sm uppercase tracking-wider">Agendar Nueva Cita</h3>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600 text-sm font-bold p-1 cursor-pointer">
            ✕
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4 text-xs sm:text-sm">
          <div>
            <label className="block font-bold text-gray-700 mb-1">Nombre de la Clienta</label>
            <input
              type="text"
              required
              placeholder="Ej. Lucía Gómez"
              value={client}
              onChange={(e) => setClient(e.target.value)}
              className="w-full p-3 border border-gray-200 rounded-xl bg-gray-50 focus:bg-white focus:border-pink-500 outline-none"
            />
          </div>

          <div>
            <label className="block font-bold text-gray-700 mb-1">Tipo de Servicio / Motivo</label>
            <select
              value={type}
              onChange={(e) => setType(e.target.value)}
              className="w-full p-3 border border-gray-200 rounded-xl bg-gray-50 focus:bg-white focus:border-pink-500 outline-none cursor-pointer"
            >
              <option value="Primera toma de medidas">Primera toma de medidas</option>
              <option value="Prueba de Vestido">Prueba de Vestido</option>
              <option value="Ajustes y Confección">Ajustes y Confección</option>
              <option value="Entrega de Pedido">Entrega de Pedido</option>
            </select>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block font-bold text-gray-700 mb-1">Hora</label>
              <input
                type="text"
                placeholder="10:00"
                value={time}
                onChange={(e) => setTime(e.target.value)}
                className="w-full p-3 border border-gray-200 rounded-xl bg-gray-50 focus:bg-white focus:border-pink-500 outline-none"
              />
            </div>

            <div>
              <label className="block font-bold text-gray-700 mb-1">Horario</label>
              <select
                value={period}
                onChange={(e) => setPeriod(e.target.value)}
                className="w-full p-3 border border-gray-200 rounded-xl bg-gray-50 focus:bg-white focus:border-pink-500 outline-none cursor-pointer"
              >
                <option value="AM">AM</option>
                <option value="PM">PM</option>
              </select>
            </div>
          </div>

          <div className="flex gap-3 pt-2">
            <button
              type="button"
              onClick={onClose}
              className="w-1/2 py-3 bg-gray-100 hover:bg-gray-200 text-gray-700 rounded-xl font-bold cursor-pointer transition"
            >
              Cancelar
            </button>
            <button
              type="submit"
              className="w-1/2 py-3 bg-pink-600 hover:bg-pink-700 text-white rounded-xl font-bold cursor-pointer shadow-md transition"
            >
              Guardar Cita
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default NuevoClienteModal;