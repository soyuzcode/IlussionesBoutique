import type { FilterBy, RangePreset } from '../../utils/dateRange';

interface CustomerFiltersProps {
  filterBy: FilterBy;
  onFilterByChange: (value: FilterBy) => void;
  preset: RangePreset;
  onPresetChange: (value: RangePreset) => void;
  from: string;
  to: string;
  onFromChange: (value: string) => void;
  onToChange: (value: string) => void;
  error: string;
}

const PRESETS: { id: RangePreset; label: string }[] = [
  { id: 'all', label: 'Todos' },
  { id: 'today', label: 'Hoy' },
  { id: 'week', label: 'Esta semana' },
  { id: 'month', label: 'Este mes' },
];

const FILTER_BY: { id: FilterBy; label: string }[] = [
  { id: 'registered', label: 'Fecha de registro' },
  { id: 'appointments', label: 'Fecha de cita' },
];

const chip = (active: boolean) =>
  `px-3 py-1.5 rounded-lg text-xs font-bold cursor-pointer transition border ${
    active
      ? 'bg-pink-600 text-white border-pink-600'
      : 'bg-gray-50 text-gray-600 border-gray-200 hover:border-pink-400'
  }`;

const dateInputClass =
  'w-full p-2.5 border border-gray-200 rounded-xl bg-white text-xs sm:text-sm outline-none focus:border-pink-500';

export function CustomerFilters({
  filterBy,
  onFilterByChange,
  preset,
  onPresetChange,
  from,
  to,
  onFromChange,
  onToChange,
  error,
}: CustomerFiltersProps) {
  return (
    <div className="bg-gray-50 border border-gray-100 rounded-2xl p-4 space-y-3">
      <div className="flex flex-wrap items-center gap-2">
        <span className="text-[10px] font-bold uppercase text-gray-400 mr-1">Filtrar por</span>
        {FILTER_BY.map((option) => (
          <button
            key={option.id}
            type="button"
            onClick={() => onFilterByChange(option.id)}
            className={chip(filterBy === option.id)}
          >
            {option.label}
          </button>
        ))}
      </div>

      <div className="flex flex-wrap items-center gap-2">
        <span className="text-[10px] font-bold uppercase text-gray-400 mr-1">Período</span>
        {PRESETS.map((option) => (
          <button
            key={option.id}
            type="button"
            onClick={() => onPresetChange(option.id)}
            className={chip(preset === option.id)}
          >
            {option.label}
          </button>
        ))}
        <button
          type="button"
          onClick={() => onPresetChange(preset === 'custom' ? 'all' : 'custom')}
          className={chip(preset === 'custom')}
        >
          ⚙️ Ajuste avanzado
        </button>
      </div>

      {preset === 'custom' && (
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 pt-1">
          <div>
            <label className="block text-[10px] font-bold uppercase text-gray-400 mb-1">Desde</label>
            <input
              type="date"
              value={from}
              max={to || undefined}
              onChange={(e) => onFromChange(e.target.value)}
              className={dateInputClass}
            />
          </div>
          <div>
            <label className="block text-[10px] font-bold uppercase text-gray-400 mb-1">Hasta</label>
            <input
              type="date"
              value={to}
              min={from || undefined}
              onChange={(e) => onToChange(e.target.value)}
              className={dateInputClass}
            />
          </div>
        </div>
      )}

      {error && (
        <p role="alert" className="bg-red-50 border border-red-200 text-red-600 text-xs font-semibold p-2.5 rounded-xl">
          {error}
        </p>
      )}
    </div>
  );
}

export default CustomerFilters;