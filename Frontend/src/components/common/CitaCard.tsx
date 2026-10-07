import React from 'react';

export interface Cita {
  id: number | string;
  time: string;
  period?: string;
  client: string;
  type: string;
  status?: string;
  day?: string;
}

interface CitaCardProps {
  cita: Cita;
  esVistaSemanal?: boolean;
  onVerDetalle?: (id: number | string) => void;
  onEditarCita?: (cita: Cita) => void;
}

export const CitaCard: React.FC<CitaCardProps> = ({
  cita,
  esVistaSemanal = false,
  onVerDetalle,
  onEditarCita,
}) => {
  return (
    <div className="p-3 hover:bg-gray-50 transition flex items-center justify-between border-b border-gray-100 last:border-b-0">
      <div className="flex items-center gap-3">
        <div className="text-center min-w-[50px]">
          <span className="text-xs font-bold text-gray-800 block">{cita.time}</span>
          {cita.period && <span className="text-[10px] text-gray-400 font-medium">{cita.period}</span>}
          {esVistaSemanal && cita.day && (
            <span className="text-[9px] text-pink-600 block font-semibold">{cita.day}</span>
          )}
        </div>

        <div className="h-8 w-1 bg-pink-500 rounded-full"></div>

        <div>
          <h4 className="text-xs font-bold text-gray-800">{cita.client}</h4>
          <p className="text-[11px] text-gray-500">{cita.type}</p>
        </div>
      </div>

      <div className="flex items-center gap-1.5">
        {onEditarCita && (
          <button
            onClick={() => onEditarCita(cita)}
            className="px-2.5 py-1 bg-pink-50 text-pink-600 hover:bg-pink-100 text-xs font-semibold rounded-lg transition active:scale-95 cursor-pointer"
          >
            ✏️ Editar
          </button>
        )}
        <button
          onClick={() => onVerDetalle && onVerDetalle(cita.id)}
          className="px-3 py-1 bg-gray-100 text-gray-600 hover:bg-gray-200 text-xs font-semibold rounded-lg transition active:scale-95 cursor-pointer"
        >
          Ver
        </button>
      </div>
    </div>
  );
};

export default CitaCard;