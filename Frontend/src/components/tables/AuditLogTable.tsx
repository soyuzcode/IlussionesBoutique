import { useCallback, useEffect, useMemo, useState } from 'react';
import { API_BASE_URL } from '../../lib/config';

type AuditLog = {
  id: string;
  occurredAt: string;
  eventType: string;
  action: string;
  actorType: string;
  actorId?: string | null;
  entityType: string;
  entityId?: string | null;
  eventSource: string;
  outcome: string;
  sessionId?: string | null;
  requestId: string;
  metadata: Record<string, unknown>;
};

type PageResponse = {
  content: AuditLog[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
};

type Filters = {
  from: string;
  to: string;
  eventType: string;
  eventSource: string;
  outcome: string;
  q: string;
};

const DEFAULT_FILTERS: Filters = {
  from: '',
  to: '',
  eventType: '',
  eventSource: '',
  outcome: '',
  q: '',
};

function buildQuery(filters: Filters, page: number, size: number): string {
  const params = new URLSearchParams();
  if (filters.from) {
    // datetime-local gives local time; send as-is if it has seconds
    params.set('from', new Date(filters.from).toISOString());
  }
  if (filters.to) {
    params.set('to', new Date(filters.to).toISOString());
  }
  if (filters.eventType) {
    params.set('eventType', filters.eventType);
  }
  if (filters.eventSource) {
    params.set('eventSource', filters.eventSource);
  }
  if (filters.outcome) {
    params.set('outcome', filters.outcome);
  }
  if (filters.q) {
    params.set('q', filters.q);
  }
  params.set('page', String(page));
  params.set('size', String(size));
  return params.toString();
}

export function AuditLogTable() {
  const [filters, setFilters] = useState<Filters>(DEFAULT_FILTERS);
  const [page, setPage] = useState(0);
  const [size] = useState(50);
  const [data, setData] = useState<PageResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const query = useMemo(() => buildQuery(filters, page, size), [filters, page, size]);

  const fetchData = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const url = `${API_BASE_URL}/api/audit/logs?${query}`;
      const res = await fetch(url, {
        method: 'GET',
        headers: {
          Accept: 'application/json',
        },
      });
      if (!res.ok) {
        throw new Error(`HTTP ${res.status}`);
      }
      const json = (await res.json()) as PageResponse;
      setData(json);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Error al cargar auditoría');
    } finally {
      setLoading(false);
    }
  }, [query]);

  useEffect(() => {
    let ignore = false;
    const run = async () => {
      setLoading(true);
      setError(null);
      try {
        const url = `${API_BASE_URL}/api/audit/logs?${query}`;
        const res = await fetch(url, {
          method: 'GET',
          headers: {
            Accept: 'application/json',
          },
        });
        if (!res.ok) {
          throw new Error(`HTTP ${res.status}`);
        }
        const json = (await res.json()) as PageResponse;
        if (!ignore) {
          setData(json);
        }
      } catch (err) {
        if (!ignore) {
          setError(err instanceof Error ? err.message : 'Error al cargar auditoría');
        }
      } finally {
        if (!ignore) {
          setLoading(false);
        }
      }
    };
    void run();
    return () => {
      ignore = true;
    };
  }, [query]);

  const applyFilters = () => {
    setPage(0);
    fetchData();
  };

  const clearFilters = () => {
    setFilters(DEFAULT_FILTERS);
    setPage(0);
  };

  const rows = data?.content ?? [];

  return (
    <div className="space-y-4 w-full">
      <div className="flex justify-between items-center">
        <h2 className="text-base sm:text-lg font-bold text-gray-800">Auditoría</h2>
        <p className="text-xs text-gray-500">
          {data ? `${data.totalElements} registros` : '—'}
        </p>
      </div>

      <div className="bg-white border border-gray-100 rounded-2xl p-4 shadow-xs space-y-3">
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
          <div className="flex flex-col gap-1">
            <label className="text-[10px] uppercase font-bold text-gray-400">Desde</label>
            <input
              type="datetime-local"
              value={filters.from}
              onChange={(e) => setFilters((f) => ({ ...f, from: e.target.value }))}
              className="p-2 text-xs border border-gray-200 rounded-xl bg-gray-50"
            />
          </div>
          <div className="flex flex-col gap-1">
            <label className="text-[10px] uppercase font-bold text-gray-400">Hasta</label>
            <input
              type="datetime-local"
              value={filters.to}
              onChange={(e) => setFilters((f) => ({ ...f, to: e.target.value }))}
              className="p-2 text-xs border border-gray-200 rounded-xl bg-gray-50"
            />
          </div>
          <div className="flex flex-col gap-1">
            <label className="text-[10px] uppercase font-bold text-gray-400">Buscar (id/request/sesión)</label>
            <input
              type="text"
              placeholder="Buscar..."
              value={filters.q}
              onChange={(e) => setFilters((f) => ({ ...f, q: e.target.value }))}
              className="p-2 text-xs border border-gray-200 rounded-xl bg-gray-50"
            />
          </div>
          <div className="flex flex-col gap-1">
            <label className="text-[10px] uppercase font-bold text-gray-400">Tipo de evento</label>
            <input
              type="text"
              placeholder="UI_LOGIN_SUBMITTED"
              value={filters.eventType}
              onChange={(e) => setFilters((f) => ({ ...f, eventType: e.target.value }))}
              className="p-2 text-xs border border-gray-200 rounded-xl bg-gray-50"
            />
          </div>
          <div className="flex flex-col gap-1">
            <label className="text-[10px] uppercase font-bold text-gray-400">Origen</label>
            <select
              value={filters.eventSource}
              onChange={(e) => setFilters((f) => ({ ...f, eventSource: e.target.value }))}
              className="p-2 text-xs border border-gray-200 rounded-xl bg-gray-50"
            >
              <option value="">Todos</option>
              <option value="FRONTEND">FRONTEND</option>
              <option value="BACKEND">BACKEND</option>
            </select>
          </div>
          <div className="flex flex-col gap-1">
            <label className="text-[10px] uppercase font-bold text-gray-400">Resultado</label>
            <select
              value={filters.outcome}
              onChange={(e) => setFilters((f) => ({ ...f, outcome: e.target.value }))}
              className="p-2 text-xs border border-gray-200 rounded-xl bg-gray-50"
            >
              <option value="">Todos</option>
              <option value="INFO">INFO</option>
              <option value="SUCCESS">SUCCESS</option>
              <option value="FAILURE">FAILURE</option>
              <option value="REJECTED">REJECTED</option>
            </select>
          </div>
        </div>
        <div className="flex gap-2">
          <button
            onClick={applyFilters}
            className="px-3 py-1.5 bg-pink-600 text-white rounded-xl text-xs font-bold hover:bg-pink-700 transition"
          >
            Aplicar filtros
          </button>
          <button
            onClick={clearFilters}
            className="px-3 py-1.5 bg-gray-100 text-gray-700 rounded-xl text-xs font-bold hover:bg-gray-200 transition"
          >
            Limpiar
          </button>
        </div>
      </div>

      {loading && <div className="text-center py-6 text-xs text-gray-400">Cargando...</div>}
      {error && <div className="text-center py-6 text-xs text-red-500">Error: {error}</div>}
      {!loading && !error && rows.length === 0 && (
        <div className="text-center py-6 text-xs text-gray-400">Sin registros</div>
      )}

      {!loading && !error && rows.length > 0 && (
        <div className="bg-white border border-gray-100 rounded-2xl shadow-xs overflow-x-auto">
          <table className="w-full text-left text-xs border-collapse">
            <thead>
              <tr className="bg-gray-50 border-b border-gray-100 text-gray-700 font-semibold">
                <th className="p-2.5">Fecha/Hora</th>
                <th className="p-2.5">Evento</th>
                <th className="p-2.5">Acción</th>
                <th className="p-2.5">Actor</th>
                <th className="p-2.5">Entidad</th>
                <th className="p-2.5">Origen</th>
                <th className="p-2.5">Resultado</th>
                <th className="p-2.5">Request ID</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {rows.map((row) => (
                <tr key={row.id} className="hover:bg-pink-50/40 transition">
                  <td className="p-2.5 text-gray-600 whitespace-nowrap">
                    {new Date(row.occurredAt).toLocaleString()}
                  </td>
                  <td className="p-2.5 font-bold text-gray-800">{row.eventType}</td>
                  <td className="p-2.5 text-gray-600">{row.action}</td>
                  <td className="p-2.5 text-gray-600">
                    {row.actorType}
                    {row.actorId ? ` / ${row.actorId}` : ''}
                  </td>
                  <td className="p-2.5 text-gray-600">
                    {row.entityType}
                    {row.entityId ? ` / ${row.entityId}` : ''}
                  </td>
                  <td className="p-2.5 text-gray-600">{row.eventSource}</td>
                  <td className="p-2.5">
                    <span
                      className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${
                        row.outcome === 'SUCCESS'
                          ? 'bg-green-100 text-green-700'
                          : row.outcome === 'FAILURE'
                            ? 'bg-red-100 text-red-700'
                            : row.outcome === 'REJECTED'
                              ? 'bg-orange-100 text-orange-700'
                              : 'bg-gray-100 text-gray-700'
                      }`}
                    >
                      {row.outcome}
                    </span>
                  </td>
                  <td className="p-2.5 text-gray-500 font-mono text-[10px]">{row.requestId}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {data && data.totalPages > 1 && (
        <div className="flex justify-between items-center">
          <button
            disabled={page === 0}
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            className="px-3 py-1.5 bg-gray-100 text-gray-700 rounded-xl text-xs font-bold disabled:opacity-50"
          >
            Anterior
          </button>
          <span className="text-xs text-gray-500">
            Página {data.number + 1} de {data.totalPages}
          </span>
          <button
            disabled={page >= data.totalPages - 1}
            onClick={() => setPage((p) => p + 1)}
            className="px-3 py-1.5 bg-gray-100 text-gray-700 rounded-xl text-xs font-bold disabled:opacity-50"
          >
            Siguiente
          </button>
        </div>
      )}
    </div>
  );
}

export default AuditLogTable;
