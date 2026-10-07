import React, { useState, useEffect } from 'react';
import Login from './components/common/Login';
import ProductTable from './components/tables/ProductTable';
import CitaCard from './components/common/CitaCard';
import NuevoClienteModal from './components/common/NewClientModel';
import type { Cita } from './components/common/CitaCard';

const mockToday: Cita[] = [
  { id: 101, time: '09:00', period: 'AM', client: 'Carla Mendoza', type: 'Prueba de Vestido', status: 'active' },
  { id: 102, time: '11:30', period: 'AM', client: 'Andrea Gómez', type: 'Toma de Medidas', status: 'active' },
  { id: 103, time: '14:00', period: 'PM', client: 'Sofía Alvarado', type: 'Primera toma de medidas', status: 'active' },
];

const mockWeek: Cita[] = [
  { id: 201, day: 'Wednesday 30', time: '10:00 AM', client: 'Gabriela Alas', type: 'Ajuste de Vestido Quinceañera' },
  { id: 202, day: 'Thursday 01', time: '03:00 PM', client: 'Lucía Fernández', type: 'Consulta de Catálogo / Cotización' },
  { id: 203, day: 'Friday 02', time: '11:30 AM', client: 'Camila Rivas', type: 'Segunda Prueba de Vestido' },
];

const mockPedidos = [
  { id: 'P-101', client: 'María López', item: 'Vestido de Gala Rojo', status: 'Pendiente Entrega', deliveryDate: '3 días' },
  { id: 'P-102', client: 'Elena Martínez', item: 'Vestido Novia Seda', status: 'En Confección', deliveryDate: '10 días' },
  { id: 'P-103', client: 'Gabriela Alas', item: 'Vestido XV Años Rosa', status: 'Listo', deliveryDate: 'Mañana' },
];

const API_URL = 'http://ilussionesboutique-production.up.railway.app/api';

interface BackendCustomer {
  id: string;
  name: string;
  phone: string;
  createdAt?: string;
  updatedAt?: string;
}

interface BackendAppointment {
  id: string;
  customer: BackendCustomer;
  appointmentDate: string;
  appointmentTime: string;
  status: 'SCHEDULED' | 'COMPLETED' | 'CANCELLED';
  googleEventId?: string | null;
  createdAt?: string;
  updatedAt?: string;
}

const mapAppointmentToCita = (appointment: BackendAppointment): Cita => {
  const [hourString, minute] = appointment.appointmentTime.split(':');
  const hour = Number(hourString);

  const period = hour >= 12 ? 'PM' : 'AM';

  const displayHour = hour % 12 || 12;

  return {
    id: appointment.id,
    time: `${String(displayHour).padStart(2, '0')}:${minute}`,
    period,
    client: appointment.customer.name,
    type: 'Cita',
    status:
      appointment.status === 'SCHEDULED'
        ? 'active'
        : appointment.status.toLowerCase(),
    day: appointment.appointmentDate,
  };
};

export function App() {
  const [isAuthenticated, setIsAuthenticated] = useState<boolean>(false);
  const [activeTab, setActiveTab] = useState<string>('Inicio');
  const [vistaCitas, setVistaCitas] = useState<'hoy' | 'semana'>('hoy');

  const [citasHoy, setCitasHoy] = useState<Cita[]>([]);
  const [citasSemanales, setCitasSemanales] = useState<Cita[]>([]);
  const [cargandoCitas, setCargandoCitas] = useState<boolean>(true);

  const [citaSeleccionada, setCitaSeleccionada] = useState<Cita | null>(null);
  const [isNuevoClienteOpen, setIsNuevoClienteOpen] = useState<boolean>(false);
  const [busquedaCliente, setBusquedaCliente] = useState<string>('');

  useEffect(() => {
    const cargarCitas = async () => {
      setCargandoCitas(true);

      try {
        const response = await fetch(`${API_URL}/appointment`);

        if (!response.ok) {
          throw new Error(`Error HTTP: ${response.status}`);
        }

        const appointments: BackendAppointment[] = await response.json();

        const citas = appointments.map(mapAppointmentToCita);

        setCitasHoy(citas);
        setCitasSemanales(citas);
      } catch (error) {
        console.error('Error cargando citas:', error);

        setCitasHoy([]);
        setCitasSemanales([]);
      } finally {
        setCargandoCitas(false);
      }
    };

    if (isAuthenticated) {
      cargarCitas();
    }
  }, [isAuthenticated]);

  const handleVerDetalleCita = (id: number | string) => {
    const citaEncontrada = [...citasHoy, ...citasSemanales].find((c) => c.id === id);
    if (citaEncontrada) {
      setCitaSeleccionada(citaEncontrada);
    }
  };

  const handleAgregarNuevaCita = async (nuevaCitaData: {
    client: string;
    type: string;
    time: string;
    period: string;
  }) => {
    try {
      /*
       * 1. Obtener los clientes existentes
       */
      const customersResponse = await fetch(`${API_URL}/customer`);

      if (!customersResponse.ok) {
        throw new Error('No se pudieron obtener los clientes');
      }

      const customers: BackendCustomer[] =
        await customersResponse.json();

      /*
       * 2. Buscar si el cliente ya existe
       */
      let customer = customers.find(
        (c) =>
          c.name.toLowerCase() ===
          nuevaCitaData.client.toLowerCase()
      );

      /*
       * 3. Si no existe, crearlo
       */
      if (!customer) {
        const customerResponse = await fetch(
          `${API_URL}/customer`,
          {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json',
            },
            body: JSON.stringify({
              name: nuevaCitaData.client,
              phone: '',
            }),
          }
        );

        if (!customerResponse.ok) {
          throw new Error('No se pudo crear el cliente');
        }

        customer = await customerResponse.json();
      }

      /*
       * 4. Convertir hora AM/PM → HH:mm:ss
       */
      let hour = Number(nuevaCitaData.time.split(':')[0]);
      const minute = nuevaCitaData.time.split(':')[1];

      if (nuevaCitaData.period === 'PM' && hour !== 12) {
        hour += 12;
      }

      if (nuevaCitaData.period === 'AM' && hour === 12) {
        hour = 0;
      }

      const appointmentTime =
        `${String(hour).padStart(2, '0')}:${minute}:00`;

      /*
       * 5. Crear la cita
       */
      const appointmentResponse = await fetch(
        `${API_URL}/appointment`,
        {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({
            customer: {
              id: customer.id,
            },
            appointmentDate: new Date()
              .toISOString()
              .split('T')[0],
            appointmentTime,
            status: 'SCHEDULED',
          }),
        }
      );

      if (!appointmentResponse.ok) {
        const errorText = await appointmentResponse.text();

        throw new Error(
          `No se pudo crear la cita: ${errorText}`
        );
      }

      /*
       * 6. Recargar citas desde el backend
       */
      const appointmentsResponse = await fetch(
        `${API_URL}/appointment`
      );

      if (!appointmentsResponse.ok) {
        throw new Error('No se pudieron recargar las citas');
      }

      const appointments: BackendAppointment[] =
        await appointmentsResponse.json();

      const citas = appointments.map(mapAppointmentToCita);

      setCitasHoy(citas);
      setCitasSemanales(citas);

      /*
       * 7. Cerrar modal
       */
      setIsNuevoClienteOpen(false);

      console.log('✅ Cita creada correctamente');
    } catch (error) {
      console.error('❌ Error creando cita:', error);
      alert('No se pudo crear la cita.');
    }
  };

  return (
    <div className="min-h-screen w-full bg-white font-sans text-gray-800 flex flex-col">
      {!isAuthenticated ? (
        <Login onLoginSuccess={() => setIsAuthenticated(true)} />
      ) : (
        <div className="flex-1 flex flex-col w-full min-h-screen relative pb-20">

          {/* Header Responsivo */}
          <header className="bg-pink-600 text-white p-4 sm:px-8 sm:py-6 shadow-md flex justify-between items-center w-full">
            <div>
              <h1 className="text-xl sm:text-2xl font-bold tracking-tight">Ilussiones Boutique</h1>
              <p className="text-pink-200 text-xs sm:text-sm mt-0.5">Martes, 29 de Septiembre</p>
            </div>
            <div className="flex items-center gap-3">
              <div className="w-9 h-9 sm:w-10 sm:h-10 bg-white rounded-full flex items-center justify-center text-pink-600 font-bold shadow text-xs sm:text-sm">
                A
              </div>
              <button
                onClick={() => setIsAuthenticated(false)}
                className="text-xs bg-pink-700 hover:bg-pink-800 text-white px-3 py-1.5 rounded-lg transition cursor-pointer font-medium"
              >
                Salir
              </button>
            </div>
          </header>

          {/* Contenido Principal */}
          <main className="flex-1 w-full p-4 sm:p-8 space-y-6 max-w-7xl mx-auto">
            {activeTab === 'Inicio' && (
              <>
                {/* Alerta de notificación interactiva */}
                <div className="bg-red-50 border-l-4 border-red-500 p-4 rounded-r-xl shadow-xs flex items-start justify-between w-full">
                  <div className="flex items-start">
                    <span className="text-red-500 text-lg sm:text-xl mr-3">⚠️</span>
                    <div>
                      <h3 className="text-red-800 font-bold text-xs sm:text-sm">Actualización Pendiente</h3>
                      <p className="text-red-600 text-xs sm:text-sm mt-0.5">
                        El vestido de "María López" se entrega en 3 días. Confirma su estado.
                      </p>
                    </div>
                  </div>
                  <button
                    onClick={() => setActiveTab('Pedidos')}
                    className="text-xs font-bold text-red-700 hover:underline cursor-pointer ml-2 whitespace-nowrap"
                  >
                    Ver Pedidos
                  </button>
                </div>

                {/* Accesos Rápidos Interactivos */}
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 w-full">
                  <button
                    onClick={() => setIsNuevoClienteOpen(true)}
                    className="bg-gray-50 hover:bg-pink-50 p-5 rounded-2xl border border-gray-100 flex items-center justify-center gap-3 transition active:scale-98 cursor-pointer shadow-xs"
                  >
                    <div className="bg-pink-100 p-3 rounded-full text-pink-600 text-2xl">📝</div>
                    <span className="font-semibold text-gray-700 text-sm sm:text-base">Nuevo Cliente / Cita</span>
                  </button>

                  <button
                    onClick={() => setActiveTab('Inventario')}
                    className="bg-gray-50 hover:bg-pink-50 p-5 rounded-2xl border border-gray-100 flex items-center justify-center gap-3 transition active:scale-98 cursor-pointer shadow-xs"
                  >
                    <div className="bg-purple-100 p-3 rounded-full text-purple-600 text-2xl">👗</div>
                    <span className="font-semibold text-gray-700 text-sm sm:text-base">Ver Catálogo</span>
                  </button>
                </div>

                {/* Agenda de Citas */}
                <section className="w-full">
                  <div className="flex justify-between items-center mb-3">
                    <h2 className="text-sm sm:text-base font-bold text-gray-800 flex items-center gap-2">
                      📅 Agenda de Citas
                    </h2>
                    <div className="flex bg-gray-100 p-1 rounded-xl">
                      <button
                        onClick={() => setVistaCitas('hoy')}
                        className={`px-3 py-1 rounded-lg text-xs sm:text-sm font-bold cursor-pointer transition ${vistaCitas === 'hoy' ? 'bg-white text-pink-600 shadow-xs' : 'text-gray-500'
                          }`}
                      >
                        Hoy
                      </button>
                      <button
                        onClick={() => setVistaCitas('semana')}
                        className={`px-3 py-1 rounded-lg text-xs sm:text-sm font-bold cursor-pointer transition ${vistaCitas === 'semana' ? 'bg-white text-pink-600 shadow-xs' : 'text-gray-500'
                          }`}
                      >
                        Esta Semana
                      </button>
                    </div>
                  </div>

                  {cargandoCitas ? (
                    <div className="text-center py-8 text-xs text-gray-400">Cargando agenda...</div>
                  ) : (
                    <div className="bg-white rounded-2xl border border-gray-100 shadow-xs overflow-hidden divide-y divide-gray-100 w-full">
                      {vistaCitas === 'hoy' &&
                        citasHoy.map((cita) => (
                          <CitaCard key={cita.id} cita={cita} onVerDetalle={handleVerDetalleCita} />
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
              </>
            )}

            {activeTab === 'Clientes' && (
              <div className="space-y-4 w-full">
                <div className="flex justify-between items-center">
                  <h2 className="text-base sm:text-lg font-bold text-gray-800">Gestión de Clientes</h2>
                  <button
                    onClick={() => setIsNuevoClienteOpen(true)}
                    className="px-4 py-2 bg-pink-600 text-white rounded-xl text-xs sm:text-sm font-bold cursor-pointer hover:bg-pink-700 transition"
                  >
                    + Nuevo Cliente
                  </button>
                </div>
                <input
                  type="text"
                  placeholder="Buscar cliente por nombre..."
                  value={busquedaCliente}
                  onChange={(e) => setBusquedaCliente(e.target.value)}
                  className="w-full p-3.5 text-xs sm:text-sm border border-gray-200 rounded-xl bg-gray-50 outline-none focus:border-pink-500"
                />

                <div className="bg-white border border-gray-100 rounded-2xl p-4 shadow-xs divide-y divide-gray-100">
                  {citasHoy
                    .filter((c) => c.client.toLowerCase().includes(busquedaCliente.toLowerCase()))
                    .map((c) => (
                      <div key={c.id} className="py-3 flex justify-between items-center text-xs sm:text-sm">
                        <div>
                          <p className="font-bold text-gray-800">{c.client}</p>
                          <p className="text-gray-500">{c.type}</p>
                        </div>
                        <span className="px-2.5 py-1 bg-gray-100 text-gray-600 rounded-lg text-xs font-semibold">
                          {c.time} {c.period || ''}
                        </span>
                      </div>
                    ))}
                </div>
              </div>
            )}

            {activeTab === 'Pedidos' && (
              <div className="space-y-4 w-full">
                <h2 className="text-base sm:text-lg font-bold text-gray-800">Seguimiento de Pedidos</h2>
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
                  {mockPedidos.map((ped) => (
                    <div key={ped.id} className="bg-white border border-gray-100 rounded-2xl p-4 shadow-xs space-y-2">
                      <div className="flex justify-between items-center">
                        <span className="text-xs font-bold text-pink-600">{ped.id}</span>
                        <span className="text-[10px] font-bold px-2 py-0.5 bg-pink-50 text-pink-700 rounded-full">
                          {ped.status}
                        </span>
                      </div>
                      <h3 className="font-bold text-gray-800 text-sm">{ped.client}</h3>
                      <p className="text-xs text-gray-500">{ped.item}</p>
                      <p className="text-[11px] text-gray-400 font-medium">Entrega estimada: {ped.deliveryDate}</p>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {activeTab === 'Inventario' && <ProductTable />}
          </main>

          {/* Modal para Crear Cita / Cliente */}
          <NuevoClienteModal
            isOpen={isNuevoClienteOpen}
            onClose={() => setIsNuevoClienteOpen(false)}
            onAgregarCita={handleAgregarNuevaCita}
          />

          {/* Modal de Detalle de Cita */}
          {citaSeleccionada && (
            <div className="fixed inset-0 bg-black/50 backdrop-blur-xs flex items-center justify-center p-4 z-50">
              <div className="bg-white w-full max-w-sm rounded-2xl p-5 space-y-4 shadow-xl">
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
                    <p className="font-bold text-gray-800 text-sm">{citaSeleccionada.client}</p>
                  </div>

                  <div className="flex justify-between items-center bg-gray-50 p-2.5 rounded-xl border border-gray-100">
                    <div>
                      <span className="text-gray-400 text-[10px] block uppercase font-bold">Horario</span>
                      <p className="text-gray-700 font-semibold">
                        {citaSeleccionada.day ? `${citaSeleccionada.day} • ` : ''}
                        {citaSeleccionada.time} {citaSeleccionada.period || ''}
                      </p>
                    </div>
                    <div>
                      <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-pink-100 text-pink-700">
                        {citaSeleccionada.status || 'active'}
                      </span>
                    </div>
                  </div>

                  <div>
                    <span className="text-gray-400 text-[10px] block uppercase font-bold">Servicio / Motivo</span>
                    <p className="text-gray-600 bg-gray-50 p-2.5 rounded-xl border border-gray-100 mt-1">
                      {citaSeleccionada.type}
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

          {/* Navegación Fija Inferior */}
          <nav className="fixed bottom-0 left-0 right-0 w-full bg-white border-t border-gray-200 flex justify-around py-3 z-40 shadow-lg">
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
                  className={`flex flex-col items-center gap-0.5 bg-transparent border-none cursor-pointer text-[10px] sm:text-xs font-semibold transition ${isActive ? 'text-pink-600 font-bold' : 'text-gray-400 hover:text-pink-600'
                    }`}
                >
                  <span className="text-lg sm:text-xl">{item.icon}</span>
                  {item.label}
                </button>
              );
            })}
          </nav>
        </div>
      )}
    </div>
  );
}

export default App;