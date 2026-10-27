import { useCallback, useEffect, useState } from 'react';

import Login from './components/common/Login';
import ProductTable from './components/tables/ProductTable';
import AuditLogTable from './components/tables/AuditLogTable';
import CitaCard from './components/common/CitaCard';
import NewClientModal from './components/common/NewClientModel';
import AppointmentModal from './components/appointments/AppointmentModal';
import CustomersTab from './components/customers/CustomersTab';
import OrdersTab from './components/orders/OrdersTab';

import { auditEvent, sanitiseErrorName } from './lib/audit';
import {
  appointmentsApi,
  appointmentToCita,
} from './api/appointmentsApi';
import { customersApi } from './api/customersApi';
import { ordersApi } from './api/ordersApi';
import {
  daysUntil,
  formatHeaderDate,
  to24h,
  todayISO,
} from './utils/dates';

import type {
  Appointment,
  AuthUser,
  Customer,
  NewAppointmentData,
  Order,
} from './types';

type Tab = 'home' | 'customers' | 'orders' | 'inventory' | 'audit';

const NAV_ITEMS: { id: Tab; label: string; icon: string }[] = [
  { id: 'home', label: 'Inicio', icon: '🏠' },
  { id: 'customers', label: 'Clientes', icon: '👥' },
  { id: 'orders', label: 'Pedidos', icon: '🧵' },
  { id: 'inventory', label: 'Inventario', icon: '📦' },
  { id: 'audit', label: 'Auditoría', icon: '🧾' },
];

const SECTION_BY_TAB: Record<Tab, string> = {
  home: 'INICIO',
  customers: 'CLIENTES',
  orders: 'PEDIDOS',
  inventory: 'INVENTARIO',
  audit: 'AUDITORIA',
};

export function App() {
  const [currentUser, setCurrentUser] = useState<AuthUser | null>(null);
  const [activeTab, setActiveTab] = useState<Tab>('home');
  const [appointmentView, setAppointmentView] = useState<'today' | 'week'>('today');

  const [appointments, setAppointments] = useState<Appointment[]>([]);
  const [loadingAppointments, setLoadingAppointments] = useState(true);
  const [appointmentsError, setAppointmentsError] = useState('');
  const [selectedAppointment, setSelectedAppointment] = useState<Appointment | null>(null);

  const [customers, setCustomers] = useState<Customer[]>([]);
  const [orders, setOrders] = useState<Order[]>([]);
  const [isNewClientOpen, setIsNewClientOpen] = useState(false);

  const isAuthenticated = currentUser !== null;

  const loadAppointments = useCallback(async () => {
    setLoadingAppointments(true);
    setAppointmentsError('');

    try {
      setAppointments(await appointmentsApi.getAll());
    } catch {
      setAppointments([]);
      setAppointmentsError(
        'No se pudo cargar la agenda. Verifica que el servidor esté activo.',
      );
    } finally {
      setLoadingAppointments(false);
    }
  }, []);

  const loadCustomers = useCallback(async () => {
    try {
      setCustomers(await customersApi.getAll());
    } catch {
      setCustomers([]);
    }
  }, []);

  const loadOrders = useCallback(async () => {
    try {
      setOrders(await ordersApi.getAll());
    } catch {
      setOrders([]);
    }
  }, []);

  // Registra errores inesperados de la interfaz sin guardar mensajes
  // potencialmente sensibles ni detalles internos del error.
  useEffect(() => {
    const handleWindowError = (event: ErrorEvent) => {
      auditEvent({
        eventType: 'UI_UNEXPECTED_ERROR',
        entityType: 'APPLICATION',
        metadata: { ERROR_NAME: sanitiseErrorName(event.error) },
      });
    };

    const handleUnhandledRejection = (event: PromiseRejectionEvent) => {
      auditEvent({
        eventType: 'UI_UNEXPECTED_ERROR',
        entityType: 'APPLICATION',
        metadata: { ERROR_NAME: sanitiseErrorName(event.reason) },
      });
    };

    window.addEventListener('error', handleWindowError);
    window.addEventListener('unhandledrejection', handleUnhandledRejection);

    return () => {
      window.removeEventListener('error', handleWindowError);
      window.removeEventListener('unhandledrejection', handleUnhandledRejection);
    };
  }, []);

  useEffect(() => {
    if (!isAuthenticated) return;

    void loadAppointments();
    void loadCustomers();
    void loadOrders();
  }, [isAuthenticated, loadAppointments, loadCustomers, loadOrders]);

  const handleLoginSuccess = (user: AuthUser) => {
    setCurrentUser(user);

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
    setCurrentUser(null);
    setActiveTab('home');
  };

  const handleTabChange = (tab: Tab) => {
    if (tab === activeTab) return;

    setActiveTab(tab);

    if (tab === 'inventory') {
      auditEvent({
        eventType: 'UI_PRODUCT_CATALOG_VIEWED',
        entityType: 'PRODUCT',
        metadata: { SECTION: SECTION_BY_TAB[tab] },
      });
      return;
    }

    auditEvent({
      eventType: 'UI_SECTION_VIEWED',
      entityType: 'APPLICATION',
      metadata: { SECTION: SECTION_BY_TAB[tab] },
    });
  };

  const handleViewAppointment = (id: number | string) => {
    const appointment = appointments.find(
      (item) => String(item.id) === String(id),
    );

    if (!appointment) return;

    setSelectedAppointment(appointment);
    auditEvent({
      eventType: 'UI_APPOINTMENT_DETAIL_VIEWED',
      entityType: 'APPOINTMENT',
      entityId: String(id),
    });
  };

  const handleAddAppointment = async (data: NewAppointmentData) => {
    try {
      const customerId =
        data.customerId ??
        (
          await customersApi.create({
            name: data.customerName.trim(),
            phone: data.phone.trim() || undefined,
          })
        ).id;

      await appointmentsApi.create({
        customerId,
        reason: data.reason,
        appointmentDate: data.date,
        appointmentTime: to24h(data.time, data.period),
      });

      setIsNewClientOpen(false);
      await Promise.all([loadAppointments(), loadCustomers()]);
    } catch {
      // La capa API registra los errores de transporte/HTTP.
      // Este evento identifica el fallo del flujo de creación.
      auditEvent({
        eventType: 'UI_APPOINTMENT_CREATE_FAILED',
        entityType: 'APPOINTMENT',
        metadata: { REASON: 'WORKFLOW_FAILED' },
      });
      throw new Error('No se pudo guardar la cita. Intenta de nuevo.');
    }
  };

  const today = todayISO();
  const weekLimit = new Date(Date.now() + 7 * 86_400_000)
    .toLocaleDateString('sv-SE');

  const sortedAppointments = [...appointments].sort((a, b) =>
    `${a.appointmentDate} ${a.appointmentTime}`.localeCompare(
      `${b.appointmentDate} ${b.appointmentTime}`,
    ),
  );

  const todayAppointments = sortedAppointments
    .filter((appointment) => appointment.appointmentDate === today)
    .map(appointmentToCita);

  const weekAppointments = sortedAppointments
    .filter(
      (appointment) =>
        appointment.appointmentDate > today &&
        appointment.appointmentDate <= weekLimit,
    )
    .map((appointment) => ({
      ...appointmentToCita(appointment),
      day: new Date(`${appointment.appointmentDate}T00:00:00`).toLocaleDateString(
        'es-SV',
        { weekday: 'long', day: 'numeric' },
      ),
    }));

  const urgentOrder = orders
    .filter((order) => order.status !== 'DELIVERED')
    .map((order) => ({ order, days: daysUntil(order.deliveryDate) }))
    .filter(({ days }) => days >= 0 && days <= 3)
    .sort((a, b) => a.days - b.days)[0];

  if (!isAuthenticated) {
    return <Login onLoginSuccess={handleLoginSuccess} />;
  }

  return (
    <div className="min-h-screen w-full bg-white font-sans text-gray-800 flex flex-col">
      <div className="flex-1 flex flex-col w-full min-h-screen relative pb-20">
        <header className="bg-pink-600 text-white p-4 sm:px-8 sm:py-6 shadow-md flex justify-between items-center w-full">
          <div>
            <h1 className="text-xl sm:text-2xl font-bold tracking-tight">
              Ilussiones Boutique
            </h1>
            <p className="text-pink-200 text-xs sm:text-sm mt-0.5">
              {formatHeaderDate()}
            </p>
          </div>

          <div className="flex items-center gap-3">
            <div
              title={currentUser.email || currentUser.name}
              className="w-9 h-9 sm:w-10 sm:h-10 bg-white rounded-full flex items-center justify-center text-pink-600 font-bold shadow text-xs sm:text-sm"
            >
              {currentUser.name?.charAt(0).toUpperCase() || '?'}
            </div>
            <button
              onClick={handleLogout}
              className="text-xs bg-pink-700 hover:bg-pink-800 text-white px-3 py-1.5 rounded-lg transition cursor-pointer font-medium"
            >
              Salir
            </button>
          </div>
        </header>

        <main className="flex-1 w-full p-4 sm:p-8 space-y-6 max-w-7xl mx-auto">
          {activeTab === 'home' && (
            <>
              {urgentOrder && (
                <div className="bg-red-50 border-l-4 border-red-500 p-4 rounded-r-xl shadow-xs flex items-start justify-between w-full">
                  <div className="flex items-start">
                    <span className="text-red-500 text-lg sm:text-xl mr-3">⚠️</span>
                    <div>
                      <h3 className="text-red-800 font-bold text-xs sm:text-sm">
                        Actualización Pendiente
                      </h3>
                      <p className="text-red-600 text-xs sm:text-sm mt-0.5">
                        El pedido "{urgentOrder.order.eventType}"
                        {urgentOrder.order.customer
                          ? ` de ${urgentOrder.order.customer.name}`
                          : ''}{' '}
                        se entrega{' '}
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
                    onClick={() => handleTabChange('orders')}
                    className="text-xs font-bold text-red-700 hover:underline cursor-pointer ml-2 whitespace-nowrap"
                  >
                    Ver Pedidos
                  </button>
                </div>
              )}

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 w-full">
                <button
                  onClick={() => setIsNewClientOpen(true)}
                  className="bg-gray-50 hover:bg-pink-50 p-5 rounded-2xl border border-gray-100 flex items-center justify-center gap-3 transition active:scale-98 cursor-pointer shadow-xs"
                >
                  <div className="bg-pink-100 p-3 rounded-full text-pink-600 text-2xl">
                    📝
                  </div>
                  <span className="font-semibold text-gray-700 text-sm sm:text-base">
                    Nuevo Cliente / Cita
                  </span>
                </button>

                <button
                  onClick={() => handleTabChange('inventory')}
                  className="bg-gray-50 hover:bg-pink-50 p-5 rounded-2xl border border-gray-100 flex items-center justify-center gap-3 transition active:scale-98 cursor-pointer shadow-xs"
                >
                  <div className="bg-purple-100 p-3 rounded-full text-purple-600 text-2xl">
                    👗
                  </div>
                  <span className="font-semibold text-gray-700 text-sm sm:text-base">
                    Ver Catálogo
                  </span>
                </button>
              </div>

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
                          appointmentView === view
                            ? 'bg-white text-pink-600 shadow-xs'
                            : 'text-gray-500'
                        }`}
                      >
                        {view === 'today' ? 'Hoy' : 'Esta Semana'}
                      </button>
                    ))}
                  </div>
                </div>

                {loadingAppointments ? (
                  <div className="text-center py-8 text-xs text-gray-400">
                    Cargando agenda...
                  </div>
                ) : appointmentsError ? (
                  <p
                    role="alert"
                    className="bg-red-50 border border-red-200 text-red-600 text-xs font-semibold p-3 rounded-xl"
                  >
                    {appointmentsError}
                  </p>
                ) : (
                  <div className="bg-white rounded-2xl border border-gray-100 shadow-xs overflow-hidden divide-y divide-gray-100 w-full">
                    {appointmentView === 'today' &&
                      (todayAppointments.length === 0 ? (
                        <p className="p-4 text-xs text-gray-400 text-center">
                          No hay citas para hoy.
                        </p>
                      ) : (
                        todayAppointments.map((cita) => (
                          <CitaCard
                            key={cita.id}
                            cita={cita}
                            onVerDetalle={handleViewAppointment}
                          />
                        ))
                      ))}

                    {appointmentView === 'week' &&
                      (weekAppointments.length === 0 ? (
                        <p className="p-4 text-xs text-gray-400 text-center">
                          No hay citas esta semana.
                        </p>
                      ) : (
                        weekAppointments.map((cita) => (
                          <CitaCard
                            key={cita.id}
                            cita={cita}
                            esVistaSemanal
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

          {activeTab === 'orders' && (
            <OrdersTab orders={orders} onRefresh={loadOrders} />
          )}

          {activeTab === 'inventory' && <ProductTable />}

          {activeTab === 'audit' && <AuditLogTable />}
        </main>

        <NewClientModal
          isOpen={isNewClientOpen}
          onClose={() => setIsNewClientOpen(false)}
          customers={customers}
          onAddAppointment={handleAddAppointment}
        />

        {selectedAppointment && (
          <AppointmentModal
            key={selectedAppointment.id}
            appointment={selectedAppointment}
            onClose={() => setSelectedAppointment(null)}
            onSaved={loadAppointments}
          />
        )}

        <nav className="fixed bottom-0 left-0 right-0 w-full bg-white border-t border-gray-200 flex justify-around py-3 z-40 shadow-lg">
          {NAV_ITEMS.map((item) => {
            const isActive = activeTab === item.id;

            return (
              <button
                key={item.id}
                onClick={() => handleTabChange(item.id)}
                aria-current={isActive ? 'page' : undefined}
                className={`flex flex-col items-center gap-0.5 bg-transparent border-none cursor-pointer text-[10px] sm:text-xs font-semibold transition ${
                  isActive
                    ? 'text-pink-600 font-bold'
                    : 'text-gray-400 hover:text-pink-600'
                }`}
              >
                <span className="text-lg sm:text-xl">{item.icon}</span>
                {item.label}
              </button>
            );
          })}
        </nav>
      </div>
    </div>
  );
}

export default App;
