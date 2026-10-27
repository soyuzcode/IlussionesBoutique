import { useState, useEffect } from 'react';

import Login from './components/common/Login';
import ProductTable from './components/tables/ProductTable';
import AuditLogTable from './components/tables/AuditLogTable';
import CitaCard from './components/common/CitaCard';
import NuevoClienteModal from './components/common/NewClientModel';
import { auditEvent, sanitiseErrorName } from './lib/audit';
import { ApiError, apiRequest } from './lib/api';

import type { Cita } from './components/common/CitaCard';

const SECTION_BY_TAB: Record<
  string,
  'INICIO' | 'CLIENTES' | 'PEDIDOS' | 'INVENTARIO' | 'AUDITORIA'
> = {
  Inicio: 'INICIO',
  Clientes: 'CLIENTES',
  Pedidos: 'PEDIDOS',
  Inventario: 'INVENTARIO',
  Auditoría: 'AUDITORIA',
};

// ============================================================
// TIPOS DEL BACKEND
// ============================================================

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

// ============================================================
// MOCKS
// ============================================================

const mockPedidos = [
  {
    id: 'P-101',
    client: 'María López',
    item: 'Vestido de Gala Rojo',
    status: 'Pendiente Entrega',
    deliveryDate: '3 días',
  },
  {
    id: 'P-102',
    client: 'Elena Martínez',
    item: 'Vestido Novia Seda',
    status: 'En Confección',
    deliveryDate: '10 días',
  },
  {
    id: 'P-103',
    client: 'Gabriela Alas',
    item: 'Vestido XV Años Rosa',
    status: 'Listo',
    deliveryDate: 'Mañana',
  },
];

// ============================================================
// CONVERSIÓN BACKEND → FRONTEND
// ============================================================

const mapAppointmentToCita = (
  appointment: BackendAppointment
): Cita => {
  const [hourString, minute] =
    appointment.appointmentTime.split(':');

  const hour = Number(hourString);

  const period = hour >= 12 ? 'PM' : 'AM';

  const displayHour = hour % 12 || 12;

  return {
    id: appointment.id,

    time: `${String(displayHour).padStart(2, '0')}:${minute}`,

    period,

    client: appointment.customer.name,

    // Por ahora el backend todavía no guarda el tipo de servicio.
    type: 'Cita',

    status:
      appointment.status === 'SCHEDULED'
        ? 'active'
        : appointment.status.toLowerCase(),

    day: appointment.appointmentDate,
  };
};

// ============================================================
// APP
// ============================================================

import NewClientModal from './components/common/NewClientModel';
import AppointmentModal from './components/appointments/AppointmentModal';
import CustomersTab from './components/customers/CustomersTab';
import OrdersTab from './components/orders/OrdersTab';
import { appointmentsApi, appointmentToCita } from './api/appointmentsApi';
import { customersApi } from './api/customersApi';
import { ordersApi } from './api/ordersApi';
import { daysUntil, formatHeaderDate, to24h, todayISO } from './utils/dates';
import type { Appointment, AuthUser, Customer, NewAppointmentData, Order } from './types';

type Tab = 'home' | 'customers' | 'orders' | 'inventory';

const NAV_ITEMS: { id: Tab; label: string; icon: string }[] = [
  { id: 'home', label: 'Inicio', icon: '🏠' },
  { id: 'customers', label: 'Clientes', icon: '👥' },
  { id: 'orders', label: 'Pedidos', icon: '🧵' },
  { id: 'inventory', label: 'Inventario', icon: '📦' },
];

export function App() {
  const [currentUser, setCurrentUser] = useState<AuthUser | null>(null);
  const [activeTab, setActiveTab] = useState<Tab>('home');
  const [appointmentView, setAppointmentView] = useState<'today' | 'week'>('today');

  const [appointments, setAppointments] = useState<Appointment[]>([]);
  const [loadingAppointments, setLoadingAppointments] = useState<boolean>(true);
  const [appointmentsError, setAppointmentsError] = useState('');
  const [selectedAppointment, setSelectedAppointment] = useState<Appointment | null>(null);

  const [customers, setCustomers] = useState<Customer[]>([]);
  const [orders, setOrders] = useState<Order[]>([]);
  const [isNewClientOpen, setIsNewClientOpen] = useState<boolean>(false);

  const isAuthenticated = currentUser !== null;

  const loadAppointments = async () => {
    setLoadingAppointments(true);
    setAppointmentsError('');
    try {
      setAppointments(await appointmentsApi.getAll());
    } catch {
      setAppointments([]);
      setAppointmentsError('No se pudo cargar la agenda. Verifica que el servidor esté activo.');
    } finally {
      setLoadingAppointments(false);
    }
  };

  const loadCustomers = async () => {
    try {
      setCustomers(await customersApi.getAll());
    } catch {
      setCustomers([]);
    }
  };

  // ==========================================================
  // ERRORES NO CONTROLADOS
  // ==========================================================

  useEffect(() => {
    const handleWindowError = (event: ErrorEvent) => {
      auditEvent({
        eventType: 'UI_UNEXPECTED_ERROR',
        entityType: 'APPLICATION',
        metadata: {
          ERROR_NAME: sanitiseErrorName(event.error),
        },
      });
    };

    const handleUnhandledRejection = (
      event: PromiseRejectionEvent
    ) => {
      auditEvent({
        eventType: 'UI_UNEXPECTED_ERROR',
        entityType: 'APPLICATION',
        metadata: {
          ERROR_NAME: sanitiseErrorName(event.reason),
        },
      });
    };

    window.addEventListener('error', handleWindowError);
    window.addEventListener(
      'unhandledrejection',
      handleUnhandledRejection
    );

    return () => {
      window.removeEventListener('error', handleWindowError);
      window.removeEventListener(
        'unhandledrejection',
        handleUnhandledRejection
      );
    };
  }, []);

  // ==========================================================
  // AUTENTICACIÓN DE INTERFAZ / NAVEGACIÓN
  // ==========================================================

  const handleLoginSuccess = () => {
    setIsAuthenticated(true);

    auditEvent({
      eventType: 'UI_SESSION_STARTED',
      entityType: 'AUTH',
    });

    auditEvent({
      eventType: 'UI_SECTION_VIEWED',
      entityType: 'APPLICATION',
      metadata: { SECTION: 'INICIO' },
    });
  };

  const handleLogout = () => {
    auditEvent({
      eventType: 'UI_LOGOUT_CLICKED',
      entityType: 'AUTH',
    });
    setIsAuthenticated(false);
  };

  const handleTabChange = (tab: string) => {
    if (tab === activeTab) {
      return;
    }

    setActiveTab(tab);

    const section = SECTION_BY_TAB[tab];
    if (!section) {
      return;
    }

    if (tab === 'Inventario') {
      auditEvent({
        eventType: 'UI_PRODUCT_CATALOG_VIEWED',
        entityType: 'PRODUCT',
        metadata: { SECTION: section },
      });
      return;
    }

    auditEvent({
      eventType: 'UI_SECTION_VIEWED',
      entityType: 'APPLICATION',
      metadata: { SECTION: section },
    });
  };

  // ==========================================================
  // CARGAR CITAS DESDE SPRING
  // ==========================================================

  useEffect(() => {
    const cargarCitas = async () => {
      setCargandoCitas(true);

      try {
        const appointments =
          await apiRequest<BackendAppointment[]>(
            '/appointment',
            {},
            {
              entityType: 'APPOINTMENT',
              operation: 'LOAD_APPOINTMENTS',
            }
          );

        const citas = (appointments ?? []).map(
          mapAppointmentToCita
        );

        setCitasHoy(citas);
        setCitasSemanales(citas);
      } catch {
        // The API wrapper already recorded a UI_API_REQUEST_FAILED event.
        setCitasHoy([]);
        setCitasSemanales([]);
      } finally {
        setCargandoCitas(false);
      }
    };

  const loadOrders = async () => {
    try {
      setOrders(await ordersApi.getAll());
    } catch {
      setOrders([]);
    }
  };

  useEffect(() => {
    if (isAuthenticated) {
      loadAppointments();
      loadCustomers();
      loadOrders();
    }
  }, [isAuthenticated]);

  // ==========================================================
  // VER DETALLE DE UNA CITA
  // ==========================================================

  const handleVerDetalleCita = (
    id: number | string
  ) => {
    const citaEncontrada = [
      ...citasHoy,
      ...citasSemanales,
    ].find((c) => c.id === id);

    if (citaEncontrada) {
      setCitaSeleccionada(citaEncontrada);

      auditEvent({
        eventType: 'UI_APPOINTMENT_DETAIL_VIEWED',
        entityType: 'APPOINTMENT',
        entityId: String(id),
      });
    }
  };

  // ==========================================================
  // CREAR NUEVA CITA
  // ==========================================================

  const handleAgregarNuevaCita = async (
    nuevaCitaData: {
      client: string;
      type: string;
      time: string;
      period: string;
    }
  ) => {
    let appointmentCreated = false;

    try {
      // ------------------------------------------------------
      // 1. OBTENER CLIENTES
      // ------------------------------------------------------

      const customers = await apiRequest<BackendCustomer[]>(
        '/customer',
        {},
        {
          entityType: 'CUSTOMER',
          operation: 'LOAD_CUSTOMERS',
        }
      );

      // ------------------------------------------------------
      // 2. BUSCAR CLIENTE EXISTENTE
      // ------------------------------------------------------

      let customer = (customers ?? []).find(
        (c) =>
          c.name.trim().toLowerCase() ===
          nuevaCitaData.client.trim().toLowerCase()
      );

      // ------------------------------------------------------
      // 3. CREAR CLIENTE SI NO EXISTE
      // ------------------------------------------------------

      if (!customer) {
        customer = await apiRequest<BackendCustomer>(
          '/customer',
          {
            method: 'POST',
            body: JSON.stringify({
              name: nuevaCitaData.client.trim(),
              phone: '',
            }),
          },
          {
            entityType: 'CUSTOMER',
            operation: 'CREATE_CUSTOMER',
          }
        );
      }

      // ------------------------------------------------------
      // SEGURIDAD: VERIFICAR QUE TENEMOS CUSTOMER
      // ------------------------------------------------------

      if (!customer?.id) {
        throw new Error(
          'El cliente no tiene un ID válido.'
        );
      }

      // ------------------------------------------------------
      // 4. CONVERTIR HORA
      // ------------------------------------------------------

      let hour = Number(
        nuevaCitaData.time.split(':')[0]
      );

      const minute =
        nuevaCitaData.time.split(':')[1];

      if (
        nuevaCitaData.period === 'PM' &&
        hour !== 12
      ) {
        hour += 12;
      }

      if (
        nuevaCitaData.period === 'AM' &&
        hour === 12
      ) {
        hour = 0;
      }

      const appointmentTime =
        `${String(hour).padStart(2, '0')}:${minute}:00`;

      // ------------------------------------------------------
      // 5. FECHA ACTUAL
      // ------------------------------------------------------

      const appointmentDate =
        new Date()
          .toISOString()
          .split('T')[0];

      // ------------------------------------------------------
      // 6. CREAR APPOINTMENT
      // ------------------------------------------------------

      const appointmentBody = {
        customer: {
          id: customer.id,
        },

        appointmentDate,

        appointmentTime,

        status: 'SCHEDULED',
      };

      await apiRequest<BackendAppointment>(
        '/appointment',
        {
          method: 'POST',
          body: JSON.stringify(appointmentBody),
        },
        {
          entityType: 'APPOINTMENT',
          operation: 'CREATE_APPOINTMENT',
        }
      );

      appointmentCreated = true;

      // ------------------------------------------------------
      // 7. RECARGAR CITAS
      // ------------------------------------------------------

      const appointments =
        await apiRequest<BackendAppointment[]>(
          '/appointment',
          {},
          {
            entityType: 'APPOINTMENT',
            operation: 'RELOAD_APPOINTMENTS',
          }
        );

      const citas = (appointments ?? []).map(
        mapAppointmentToCita
      );

      setCitasHoy(citas);
      setCitasSemanales(citas);

      // ------------------------------------------------------
      // 8. CERRAR MODAL
      // ------------------------------------------------------

      setIsNuevoClienteOpen(false);
    } catch (error) {
      // Transport/HTTP failures are already audited by apiRequest. Only log a
      // workflow-level failure for non-API errors (for example a missing id).
      if (!(error instanceof ApiError)) {
        auditEvent({
          eventType: 'UI_APPOINTMENT_CREATE_FAILED',
          entityType: 'APPOINTMENT',
          metadata: { REASON: 'INVALID_DATA' },
        });
      }

      if (appointmentCreated) {
        alert(
          'La cita se creó, pero no se pudo actualizar la lista. Recarga la página.'
        );
      } else {
        alert(
          error instanceof ApiError
            ? 'No se pudo guardar la cita. Intenta de nuevo.'
            : 'No se pudo guardar la cita.'
        );
      }
    }
  const handleViewAppointment = (id: number | string) => {
    setSelectedAppointment(appointments.find((a) => String(a.id) === String(id)) ?? null);
  };

  const handleAddAppointment = async (data: NewAppointmentData) => {
    const customerId =
      data.customerId ??
      (await customersApi.create({ name: data.customerName.trim(), phone: data.phone.trim() || undefined })).id;

    await appointmentsApi.create({
      customerId,
      reason: data.reason,
      appointmentDate: data.date,
      appointmentTime: to24h(data.time, data.period),
    });
    await Promise.all([loadAppointments(), loadCustomers()]);
  };

  // Citas de hoy y de los próximos 7 días, ordenadas por fecha y hora
  const today = todayISO();
  const weekLimit = new Date(Date.now() + 7 * 86400000).toLocaleDateString('sv-SE');
  const sortedAppointments = [...appointments].sort((a, b) =>
    `${a.appointmentDate} ${a.appointmentTime}`.localeCompare(`${b.appointmentDate} ${b.appointmentTime}`),
  );
  const todayAppointments = sortedAppointments
    .filter((a) => a.appointmentDate === today)
    .map(appointmentToCita);
  const weekAppointments = sortedAppointments
    .filter((a) => a.appointmentDate > today && a.appointmentDate <= weekLimit)
    .map((a) => ({
      ...appointmentToCita(a),
      day: new Date(`${a.appointmentDate}T00:00:00`).toLocaleDateString('es-SV', {
        weekday: 'long',
        day: 'numeric',
      }),
    }));

  // Pedido más cercano a entregarse en los próximos 3 días (para la alerta)
  const urgentOrder = orders
    .filter((order) => order.status !== 'DELIVERED')
    .map((order) => ({ order, days: daysUntil(order.deliveryDate) }))
    .filter((item) => item.days >= 0 && item.days <= 3)
    .sort((a, b) => a.days - b.days)[0];

  return (
    <div className="min-h-screen w-full bg-white font-sans text-gray-800 flex flex-col">
      {!isAuthenticated ? (
        <Login onLoginSuccess={handleLoginSuccess} />
        <Login onLoginSuccess={(user) => setCurrentUser(user)} />
      ) : (
        <div className="flex-1 flex flex-col w-full min-h-screen relative pb-20">
          {/* Header */}
          <header className="bg-pink-600 text-white p-4 sm:px-8 sm:py-6 shadow-md flex justify-between items-center w-full">
            <div>
              <h1 className="text-xl sm:text-2xl font-bold tracking-tight">Ilussiones Boutique</h1>
              <p className="text-pink-200 text-xs sm:text-sm mt-0.5">{formatHeaderDate()}</p>
            </div>
            <div className="flex items-center gap-3">
              <div
                title={currentUser?.email || currentUser?.name}
                className="w-9 h-9 sm:w-10 sm:h-10 bg-white rounded-full flex items-center justify-center text-pink-600 font-bold shadow text-xs sm:text-sm"
              >
                {currentUser?.name.charAt(0).toUpperCase() || '?'}
              </div>
              <button
                onClick={handleLogout}
                onClick={() => setCurrentUser(null)}
                className="text-xs bg-pink-700 hover:bg-pink-800 text-white px-3 py-1.5 rounded-lg transition cursor-pointer font-medium"
              >
                Salir
              </button>
            </div>
          </header>

          {/* Main content */}
          <main className="flex-1 w-full p-4 sm:p-8 space-y-6 max-w-7xl mx-auto">
            {activeTab === 'home' && (
              <>
                {urgentOrder && (
                  <div className="bg-red-50 border-l-4 border-red-500 p-4 rounded-r-xl shadow-xs flex items-start justify-between w-full">
                    <div className="flex items-start">
                      <span className="text-red-500 text-lg sm:text-xl mr-3">⚠️</span>
                      <div>
                        <h3 className="text-red-800 font-bold text-xs sm:text-sm">Actualización Pendiente</h3>
                        <p className="text-red-600 text-xs sm:text-sm mt-0.5">
                          El pedido "{urgentOrder.order.eventType}"
                          {urgentOrder.order.customer ? ` de ${urgentOrder.order.customer.name}` : ''} se entrega{' '}
                          {urgentOrder.days === 0
                            ? 'hoy'
                            : urgentOrder.days === 1
                              ? 'mañana'
                              : `en ${urgentOrder.days} días`}
                          . Confirma su estado.
                        </p>
                      </div>
                    </div>
                    <button
                      onClick={() => setActiveTab('orders')}
                      className="text-xs font-bold text-red-700 hover:underline cursor-pointer ml-2 whitespace-nowrap"
                    >
                      Ver Pedidos
                    </button>
                  </div>
                )}

                  <button
                    onClick={() =>
                      handleTabChange('Pedidos')
                    }
                    className="text-xs font-bold text-red-700 hover:underline cursor-pointer ml-2 whitespace-nowrap"
                  >
                    Ver Pedidos
                  </button>

                </div>

                {/* ACCESOS RÁPIDOS */}

                {/* Quick actions */}
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 w-full">
                  <button
                    onClick={() => setIsNewClientOpen(true)}
                    className="bg-gray-50 hover:bg-pink-50 p-5 rounded-2xl border border-gray-100 flex items-center justify-center gap-3 transition active:scale-98 cursor-pointer shadow-xs"
                  >
                    <div className="bg-pink-100 p-3 rounded-full text-pink-600 text-2xl">📝</div>
                    <span className="font-semibold text-gray-700 text-sm sm:text-base">Nuevo Cliente / Cita</span>
                  </button>

                  <button
                    onClick={() =>
                      handleTabChange('Inventario')
                    }
                    onClick={() => setActiveTab('inventory')}
                    className="bg-gray-50 hover:bg-pink-50 p-5 rounded-2xl border border-gray-100 flex items-center justify-center gap-3 transition active:scale-98 cursor-pointer shadow-xs"
                  >
                    <div className="bg-purple-100 p-3 rounded-full text-purple-600 text-2xl">👗</div>
                    <span className="font-semibold text-gray-700 text-sm sm:text-base">Ver Catálogo</span>
                  </button>
                </div>

                {/* Appointments */}
                <section className="w-full">
                  <div className="flex justify-between items-center mb-3">
                    <h2 className="text-sm sm:text-base font-bold text-gray-800 flex items-center gap-2">
                      📅 Agenda de Citas
                    </h2>
                    <div className="flex bg-gray-100 p-1 rounded-xl">
                      {(['today', 'week'] as const).map((view) => (
                        <button
                          key={view}
                          onClick={() => setAppointmentView(view)}
                          className={`px-3 py-1 rounded-lg text-xs sm:text-sm font-bold cursor-pointer transition ${
                            appointmentView === view ? 'bg-white text-pink-600 shadow-xs' : 'text-gray-500'
                          }`}
                        >
                          {view === 'today' ? 'Hoy' : 'Esta Semana'}
                        </button>
                      ))}
                    </div>
                  </div>

                  {loadingAppointments ? (
                    <div className="text-center py-8 text-xs text-gray-400">Cargando agenda...</div>
                  ) : appointmentsError ? (
                    <p role="alert" className="bg-red-50 border border-red-200 text-red-600 text-xs font-semibold p-3 rounded-xl">
                      {appointmentsError}
                    </p>
                  ) : (
                    <div className="bg-white rounded-2xl border border-gray-100 shadow-xs overflow-hidden divide-y divide-gray-100 w-full">
                      {appointmentView === 'today' &&
                        (todayAppointments.length === 0 ? (
                          <p className="p-4 text-xs text-gray-400 text-center">No hay citas para hoy.</p>
                        ) : (
                          todayAppointments.map((cita) => (
                            <CitaCard key={cita.id} cita={cita} onVerDetalle={handleViewAppointment} />
                          ))
                        ))}

                      {appointmentView === 'week' &&
                        (weekAppointments.length === 0 ? (
                          <p className="p-4 text-xs text-gray-400 text-center">No hay citas esta semana.</p>
                        ) : (
                          weekAppointments.map((cita) => (
                            <CitaCard
                              key={cita.id}
                              cita={cita}
                              esVistaSemanal={true}
                              onVerDetalle={handleViewAppointment}
                            />
                          ))
                        ))}
                    </div>
                  )}
                </section>
              </>
            )}

            {activeTab === 'customers' && (
              <CustomersTab
                customers={customers}
                onNew={() => setIsNewClientOpen(true)}
                onRefresh={loadCustomers}
              />
            )}

            {activeTab === 'orders' && <OrdersTab orders={orders} onRefresh={loadOrders} />}

            {/* ==================================================
                AUDITORÍA
            ================================================== */}

            {activeTab === 'Auditoría' && (
              <AuditLogTable />
            )}

            {activeTab === 'inventory' && <ProductTable />}
          </main>

          {/* New customer / appointment modal */}
          <NewClientModal
            isOpen={isNewClientOpen}
            onClose={() => setIsNewClientOpen(false)}
            customers={customers}
            onAddAppointment={handleAddAppointment}
          />

          {/* Appointment detail / edit modal */}
          {selectedAppointment && (
            <AppointmentModal
              key={selectedAppointment.id}
              appointment={selectedAppointment}
              onClose={() => setSelectedAppointment(null)}
              onSaved={loadAppointments}
            />
          )}

          {/* Bottom navigation */}
          <nav className="fixed bottom-0 left-0 right-0 w-full bg-white border-t border-gray-200 flex justify-around py-3 z-40 shadow-lg">

            {[
              {
                id: 'Inicio',
                label: 'Inicio',
                icon: '🏠',
              },
              {
                id: 'Clientes',
                label: 'Clientes',
                icon: '👥',
              },
              {
                id: 'Pedidos',
                label: 'Pedidos',
                icon: '🧵',
              },
              {
                id: 'Inventario',
                label: 'Inventario',
                icon: '📦',
              },
              {
                id: 'Auditoría',
                label: 'Auditoría',
                icon: '🧾',
              },
            ].map((item) => {

              const isActive =
                activeTab === item.id;

              return (
                <button
                  key={item.id}
                  onClick={() =>
                    handleTabChange(item.id)
                  }
            {NAV_ITEMS.map((item) => {
              const isActive = activeTab === item.id;
              return (
                <button
                  key={item.id}
                  onClick={() => setActiveTab(item.id)}
                  className={`flex flex-col items-center gap-0.5 bg-transparent border-none cursor-pointer text-[10px] sm:text-xs font-semibold transition ${
                    isActive ? 'text-pink-600 font-bold' : 'text-gray-400 hover:text-pink-600'
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