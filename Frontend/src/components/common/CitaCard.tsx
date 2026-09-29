import React from 'react';

// ¡Asegúrate de que tenga "export"!
export interface Cita {
  id: number | string;
  hora: string;
  periodo?: string;
  cliente: string;
  tipo: string;
  estado?: 'activa' | 'completada' | string;
  dia?: string;
}

interface CitaCardProps {
  cita: Cita;
  esVistaSemanal?: boolean;
  onVerDetalle?: (id: number | string) => void;
}

export function CitaCard({ cita, esVistaSemanal = false, onVerDetalle }: CitaCardProps) {
  if (esVistaSemanal) {
    return (
      <div className="p-3 flex items-center justify-between border-b border-gray-100 last:border-b-0 hover:bg-gray-50/50 transition">
        <div>
          <span className="text-[9px] font-bold text-pink-600 bg-pink-50 px-1.5 py-0.5 rounded inline-block mb-1">
            {cita.dia} • {cita.hora}
          </span>
          <p className="font-bold text-gray-800 text-xs">{cita.cliente}</p>
          <p className="text-[10px] text-gray-500">{cita.tipo}</p>
        </div>
        <button
          onClick={() => onVerDetalle && onVerDetalle(cita.id)}
          className="text-pink-600 bg-pink-50 hover:bg-pink-100 px-2.5 py-1 rounded text-[10px] font-semibold cursor-pointer transition active:scale-95"
        >
          Ver
        </button>
      </div>
    );
  }

  return (
    <div
      className={`p-3 flex items-center justify-between border-b border-gray-100 last:border-b-0 transition ${
        cita.estado === 'completada' ? 'opacity-60 bg-gray-50/30' : 'hover:bg-pink-50/20'
      }`}
    >
      <div className="flex items-center gap-3">
        <div className="text-center">
          <span className="block text-xs font-bold text-gray-800">{cita.hora}</span>
          {cita.periodo && <span className="block text-[10px] text-gray-400">{cita.periodo}</span>}
        </div>
        <div
          className={`w-1 h-8 rounded-full ${
            cita.estado === 'activa' ? 'bg-pink-500' : 'bg-gray-300'
          }`}
        ></div>
        <div>
          <p className="font-bold text-gray-800 text-xs">{cita.cliente}</p>
          <p className="text-[10px] text-gray-500">{cita.tipo}</p>
        </div>
      </div>
      <button
        onClick={() => onVerDetalle && onVerDetalle(cita.id)}
        className={`px-2.5 py-1 rounded text-[10px] font-semibold cursor-pointer transition active:scale-95 ${
          cita.estado === 'activa'
            ? 'text-pink-600 bg-pink-50 hover:bg-pink-100'
            : 'text-gray-600 bg-gray-100 hover:bg-gray-200'
        }`}
      >
        Ver
      </button>
    </div>
  );
}

export default CitaCard;