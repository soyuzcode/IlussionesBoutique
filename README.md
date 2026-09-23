# 👗 Ilussions Web App - V1.0.0

Bienvenido al repositorio oficial de **Ilussions**, el sistema de gestión integral desarrollado exclusivamente para **Ilussiones Boutique**.

## 📖 Acerca del Proyecto

**Ilussions** es una aplicación web diseñada para modernizar y optimizar los procesos diarios de Ilussiones Boutique. El objetivo principal de esta versión (V1.0.0) es erradicar el formato de registro manual (en papel) y centralizar toda la información en un entorno digital seguro, organizado y accesible desde cualquier lugar.

Este sistema permite a la administración y a los asistentes de la boutique llevar un control estricto sobre las citas, clientes, inventario de vestidos, materias primas y el progreso de confección, mejorando la experiencia del cliente y la eficiencia interna del negocio.

---

## ✨ Características Principales (Módulos)

El sistema está compuesto por los siguientes módulos clave:

1. 👥 **Manejo de Clientes y Pedidos:** Búsqueda rápida de clientes recurrentes, registro de nuevos clientes y creación de perfiles detallados.
2. 📦 **Gestión de Inventario:** Control en tiempo real de vestidos, accesorios y materias primas disponibles en la tienda. Las existencias se descuentan automáticamente al confirmar pedidos.
3. 📄 **Generación de Documentos:** Creación ágil de cotizaciones y facturas en formato PDF basadas en los pedidos de los clientes.
4. ✂️ **Seguimiento de Confección (Progreso):** Monitoreo del estado de los vestidos a medida, fechas de entrega, materiales faltantes y estado de los accesorios complementarios.
5. 📅 **Integración con Google Calendar:** Sincronización automática de citas y pruebas de vestido directamente con el calendario de Google de la boutique.
6. 🔔 **Actualización y Alertas:** Sistema de recordatorios para mantener los datos de pedidos e inventarios siempre actualizados.
7. 👗 **Catálogo Digital:** Visualización de vestidos y accesorios disponibles, con fotografías y rangos de precio, listo para ser compartido con los clientes o mostrado en tienda.

---

## 👥 Roles de Usuario

El sistema cuenta con un control de acceso basado en roles para garantizar la seguridad de la información:

* **👑 Administradora/Propietaria:** Acceso total. Puede registrar, consultar y cancelar pedidos; gestionar el inventario de manera global y administrar las cuentas de los usuarios auxiliares.
* **🤝 Auxiliar/Asistente:** Puede registrar nuevos clientes y pedidos, consultar la agenda de entregas y visualizar el inventario disponible. No tiene permisos para la gestión de usuarios.

---

## 💻 Entorno Técnico y Arquitectura

* **Tipo de Aplicación:** Web App Multiplataforma (Responsive design para escritorio y dispositivos móviles).
* **Despliegue:** Servidor en la nube (PaaS).
* **Integraciones:** Google Calendar API.
* **Seguridad:** Autenticación cifrada (JWT / Supabase Auth) y protección de rutas según el rol de usuario.
* **Arquitectura:** Diseño modular basado en buenas prácticas (Clean Architecture / MVC) para permitir escalabilidad futura (ej. pasarelas de pago).

---

## 🛠️ Instalación y Desarrollo Local

(Añadir aquí las instrucciones específicas de tu stack tecnológico. Ejemplo:)

```bash
# Clonar el repositorio
git clone https://github.com/tu-usuario/ilussions-webapp.git

# Entrar al directorio
cd ilussions-webapp

# Instalar dependencias
npm install

# Configurar variables de entorno (Crear archivo .env)
# Agregar credenciales de BD, JWT y Google Calendar API

# Iniciar el servidor en modo desarrollo
npm run dev
```

---

## 👨‍💻 Equipo de Desarrollo: KEY CODE

Este proyecto está siendo desarrollado por el **Equipo 7 (KEY CODE)**:

* G. Alas
* S. Alvarado
* B. Amaya
* M. Aquino
* C. Flores

**Fecha de inicio del proyecto:** Agosto de 2026.
