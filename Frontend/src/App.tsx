import React, { useState, useEffect } from 'react';
import Login from './components/common/Login';
import ProductTable from './components/tables/ProductTable';
import CitaCard from './components/common/CitaCard';
import type { Cita } from './components/common/CitaCard';

// Datos de respaldo en caso de que aún no exista public/data/citas.json
const mockHoy: Cita[] = [
  { id: 101, hora: '14:00', periodo: 'PM', cliente: 'Sofía Alvarado', tipo: 'Primera toma de medidas', estado: 'activa' },
  { id: 102, hora: '16:30', periodo: 'PM', cliente: 'Elena Martínez', tipo: 'Prueba final (Boda)', estado: 'completada' },
];

const mockSemana: Cita[] = [
  { id: 201, dia: 'Miércoles 30', hora: '10:00 AM', cliente: 'Gabriela Alas', tipo: 'Ajuste de Vestido Quinceañera' },
  { id: 202, dia: 'Jueves 01', hora: '03:00 PM', cliente: 'Lucía Fernández', tipo: 'Consulta de Catálogo / Cotización' },
  { id: 203, dia: 'Viernes 02', hora: '11:30 AM', cliente: 'Camila Rivas', tipo: 'Segunda Prueba de Vestido' },
];

export function App() {
  const [isAuthenticated, setIsAuthenticated] = useState<boolean>(false);
  const [activeTab, setActiveTab] = useState<string>('Inicio');
  const [vistaCitas, setVistaCitas] = useState<'hoy' | 'semana'>('hoy');

  // Estados para lectura de JSON / API en tiempo real
  const [citasHoy, setCitasHoy] = useState<Cita[]>([]);
  const [citasSemanales, setCitasSemanales] = useState<Cita[]>([]);
  const [cargandoCitas, setCargandoCitas] = useState<boolean>(true);

  // Estado para controlar el Modal de detalle
  const [citaSeleccionada, setCitaSeleccionada] = useState<Cita | null>(null);

  // Efecto para cargar citas en tiempo real desde public/data/citas.json
  useEffect(() => {
    const cargarCitas = async () => {
      setCargandoCitas(true);
      try {
        const res = await fetch('/data/citas.json');
        if (!res.ok) throw new Error('Archivo citas.json no encontrado, cargando datos de respaldo.');
        const data = await res.json();
        setCitasHoy(data.hoy || []);
        setCitasSemanales(data.semana || []);
      } catch {
        setCitasHoy(mockHoy);
        setCitasSemanales(mockSemana);
      } finally {
        setCargandoCitas(false);
      }
    };

    if (isAuthenticated) {
      cargarCitas();
    }
  }, [isAuthenticated]);

  // Abrir detalle en el Modal en lugar de un alert
  const handleVerDetalleCita = (id: number | string) => {
    const citaEncontrada = [...citasHoy, ...citasSemanales].find((c) => c.id === id);
    if (citaEncontrada) {
      setCitaSeleccionada(citaEncontrada);
    }
  };

  return (
    <div className="bg-gray-200 min-h-screen flex justify-center items-center py-5 font-sans">
      {/* Marco del Celular */}
      <div className="w-[360px] h-[680px] bg-white rounded-[32px] shadow-2xl border-[8px] border-gray-700 relative flex flex-col overflow-hidden">
        {!isAuthenticated ? (
          <Login onLoginSuccess={() => setIsAuthenticated(true)} />
        ) : (
          <>
            {/* Contenido Dinámico */}
            <div className="flex-1 overflow-y-auto pb-16 bg-gray-50 text-gray-800">
              
              {/* VISTA: INICIO */}
              {activeTab === 'Inicio' && (
                <div>
                  {/* Header / Topbar */}
                  <header className="bg-pink-600 text-white p-5 rounded-b-3xl shadow-md relative">
                    <button
                      onClick={() => setIsAuthenticated(false)}
                      className="absolute top-3 right-4 text-[10px] text-pink-200 underline cursor-pointer hover:text-white transition"
                    >
                      Salir
                    </button>
                    <div className="flex justify-between items-center">
                      <div>
                        <h1 className="text-xl font-bold tracking-tight">Ilussiones</h1>
                        <p className="text-pink-200 text-xs mt-0.5">Martes, 29 de Septiembre</p>
                      </div>
                      <div className="w-9 h-9 bg-white rounded-full flex items-center justify-center text-pink-600 font-bold shadow text-sm">
                        A
                      </div>
                    </div>
                  </header>

                  {/* Contenido Principal */}
                  <main className="p-4 space-y-4">
                    {/* Alertas del Sistema */}
                    <div className="bg-red-50 border-l-4 border-red-500 p-3 rounded shadow-sm flex items-start">
                      <span className="text-red-500 text-lg mr-2.5">⚠️</span>
                      <div>
                        <h3 className="text-red-800 font-bold text-xs">Actualización Pendiente</h3>
                        <p className="text-red-600 text-[11px] mt-0.5">
                          El vestido de "María López" se entrega en 3 días. Confirma su estado.
                        </p>
                      </div>
                    </div>

                    {/* Acciones Rápidas */}
                    <div className="grid grid-cols-2 gap-3">
                      <button
                        onClick={() => setActiveTab('Clientes')}
                        className="bg-white p-4 rounded-2xl shadow-sm border border-gray-100 flex flex-col items-center justify-center gap-2 hover:bg-pink-50 transition active:scale-95 cursor-pointer"
                      >
                        <div className="bg-pink-100 p-2.5 rounded-full text-pink-600 text-xl">📝</div>
                        <span className="font-semibold text-gray-700 text-xs text-center">Nuevo Cliente / Cita</span>
                      </button>

                      <button
                        onClick={() => setActiveTab('Inventario')}
                        className="bg-white p-4 rounded-2xl shadow-sm border border-gray-100 flex flex-col items-center justify-center gap-2 hover:bg-pink-50 transition active:scale-95 cursor-pointer"
                      >
                        <div className="bg-purple-100 p-2.5 rounded-full text-purple-600 text-xl">👗</div>
                        <span className="font-semibold text-gray-700 text-xs text-center">Ver Catálogo</span>
                      </button>
                    </div>

                    {/* Agenda de Citas modulada con CitaCard */}
                    <section>
                      <div className="flex justify-between items-center mb-2.5">
                        <h2 className="text-xs font-bold text-gray-800 flex items-center gap-1">
                          📅 Agenda de Citas
                        </h2>
                        <div className="flex bg-gray-200 p-0.5 rounded-lg">
                          <button
                            onClick={() => setVistaCitas('hoy')}
                            className={`px-2 py-1 rounded-md text-[10px] font-bold cursor-pointer transition ${
                              vistaCitas === 'hoy' ? 'bg-white text-pink-600 shadow-sm' : 'text-gray-500'
                            }`}
                          >
                            Hoy
                          </button>
                          <button
                            onClick={() => setVistaCitas('semana')}
                            className={`px-2 py-1 rounded-md text-[10px] font-bold cursor-pointer transition ${
                              vistaCitas === 'semana' ? 'bg-white text-pink-600 shadow-sm' : 'text-gray-500'
                            }`}
                          >
                            Esta Semana
                          </button>
                        </div>
                      </div>

                      {cargandoCitas ? (
                        <div className="text-center py-6 text-xs text-gray-400">Cargando agenda...</div>
                      ) : (
                        <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden divide-y divide-gray-100">
                          {vistaCitas === 'hoy' &&
                            citasHoy.map((cita) => (
                              <CitaCard
                                key={cita.id}
                                cita={cita}
                                onVerDetalle={handleVerDetalleCita}
                              />
                            ))}

                          {vistaCitas === 'semana' &&
                            citasSemanales.map((cita) => (
                              <CitaCard
                                key={cita.id}
                                cita={cita}
                                esVistaSemanal={true}
                                onVerDetalle={handleVerDetalleCita}
                              />
                            ))}
                        </div>
                      )}
                    </section>
                  </main>
                </div>
              )}

              {/* VISTA: CLIENTES */}
              {activeTab === 'Clientes' && (
                <div className="p-4 space-y-3">
                  <div>
                    <h2 className="text-base font-bold text-gray-800">Gestión de Clientes</h2>
                    <p className="text-xs text-gray-500">Registro y agendamiento de citas.</p>
                  </div>
                  <input
                    type="text"
                    placeholder="Buscar cliente por nombre o teléfono..."
                    className="w-full p-2.5 text-xs border border-gray-200 rounded-xl bg-white outline-none focus:border-pink-500 shadow-sm transition"
                  />
                  <div className="bg-white p-3 rounded-2xl border border-gray-100 text-xs text-gray-500 text-center py-6 shadow-sm">
                    Selecciona o busca un cliente para gestionar sus medidas y citas.
                  </div>
                </div>
              )}

              {/* VISTA: PEDIDOS */}
              {activeTab === 'Pedidos' && (
                <div className="p-4 space-y-3">
                  <div>
                    <h2 className="text-base font-bold text-gray-800">Pedidos en Taller</h2>
                    <p className="text-xs text-gray-500">Estado de confección y entregas.</p>
                  </div>
                  <div className="bg-white p-3 rounded-2xl border border-gray-100 text-xs text-gray-500 text-center py-6 shadow-sm">
                    Seguimiento de pedidos activos en confección.
                  </div>
                </div>
              )}

              {/* VISTA: INVENTARIO */}
              {activeTab === 'Inventario' && (
                <div className="p-4 space-y-3">
                  <div>
                    <h2 className="text-base font-bold text-gray-800 mb-1">Inventario de Prendas</h2>
                    <p className="text-xs text-gray-500 mb-2">Catálogo general de vestidos y accesorios.</p>
                  </div>
                  <ProductTable />
                </div>
              )}
            </div>

            {/* Modal Desplegable para Detalle de Cita */}
            {citaSeleccionada && (
              <div className="absolute inset-0 bg-black/50 backdrop-blur-xs flex items-end justify-center z-50">
                <div className="bg-white w-full rounded-t-3xl p-5 space-y-4">
                  <div className="flex justify-between items-center border-b border-gray-100 pb-2">
                    <h3 className="font-bold text-gray-800 text-xs uppercase tracking-wider">Detalle de la Cita</h3>
                    <button
                      onClick={() => setCitaSeleccionada(null)}
                      className="text-gray-400 hover:text-gray-600 text-xs font-bold p-1 cursor-pointer"
                    >
                      ✕
                    </button>
                  </div>

                  <div className="space-y-3 text-xs">
                    <div>
                      <span className="text-gray-400 text-[10px] block uppercase font-bold">Cliente</span>
                      <p className="font-bold text-gray-800 text-sm">{citaSeleccionada.cliente}</p>
                    </div>

                    <div className="flex justify-between items-center bg-gray-50 p-2.5 rounded-xl border border-gray-100">
                      <div>
                        <span className="text-gray-400 text-[10px] block uppercase font-bold">Horario</span>
                        <p className="text-gray-700 font-semibold">
                          {citaSeleccionada.dia ? `${citaSeleccionada.dia} • ` : ''}
                          {citaSeleccionada.hora} {citaSeleccionada.periodo || ''}
                        </p>
                      </div>
                      <div>
                        <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-pink-100 text-pink-700">
                          {citaSeleccionada.estado || 'Programada'}
                        </span>
                      </div>
                    </div>

                    <div>
                      <span className="text-gray-400 text-[10px] block uppercase font-bold">Servicio / Motivo</span>
                      <p className="text-gray-600 bg-gray-50 p-2.5 rounded-xl border border-gray-100 mt-1">
                        {citaSeleccionada.tipo}
                      </p>
                    </div>
                  </div>

                  <button
                    onClick={() => setCitaSeleccionada(null)}
                    className="w-full py-2.5 bg-pink-600 text-white rounded-xl text-xs font-bold cursor-pointer hover:bg-pink-700 transition active:scale-95 shadow-md"
                  >
                    Cerrar
                  </button>
                </div>
              </div>
            )}

            {/* Bottom Navbar (Navegación Fija) */}
            <nav className="absolute bottom-0 left-0 right-0 bg-white border-t border-gray-200 flex justify-around py-2.5 z-40 rounded-b-[24px]">
              {[
                { id: 'Inicio', label: 'Inicio', icon: '🏠' },
                { id: 'Clientes', label: 'Clientes', icon: '👥' },
                { id: 'Pedidos', label: 'Pedidos', icon: '🧵' },
                { id: 'Inventario', label: 'Inventario', icon: '📦' },
              ].map((item) => {
                const isActive = activeTab === item.id;
                return (
                  <button
                    key={item.id}
                    onClick={() => setActiveTab(item.id)}
                    className={`flex flex-col items-center gap-0.5 bg-transparent border-none cursor-pointer text-[10px] font-semibold transition ${
                      isActive ? 'text-pink-600 font-bold' : 'text-gray-400 hover:text-pink-600'
                    }`}
                  >
                    <span className="text-xl">{item.icon}</span>
                    {item.label}
                  </button>
                );
              })}
            </nav>
          </>
        )}
      </div>
    </div>
  );
}

export default App;