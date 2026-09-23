# Ilussions - Web App (Frontend)

Módulo Frontend de la aplicación web **Ilussions** para **Ilussiones Boutique**, desarrollada por el equipo **KEY CODE**.

Este módulo está enfocado en proporcionar una interfaz que sea intuitiva, moderna y adaptada a dispositivos móviles y de escritorio para digitalizar la gestión de clientes, citas, inventario, pedidos y catálogo para la tienda Ilussiones Boutique, la cual es una tienda especializada en vestidos de boda y de 15 años.

---

## Tecnologías Utilizadas

- **Framework / Bundler:** Vite
- **Librería UI:** React
- **Lenguaje:** TypeScript (`.ts`, `.tsx`)
- **Gestor de paquetes:** Node Package Manager (`npm`)

---

## Módulos y Funcionalidades Incluidas

La interfaz tendrá los siguientes módulos principales:

1. **Formulario de Clientes y Citas (`Pantalla_01_FormularioCliente`):** Registro de datos del cliente, agendamiento de primera cita e integración con Google Calendar.
2. **Gestión de Inventario (`Pantalla_02_Inventario`):** Consulta y actualización de prendas, productos disponibles y materia prima.
3. **Generador de Documentos (`Pantalla_03_Documentos`):** Emisión de cotizaciones temporales y facturas en PDF.
4. **Seguimiento de Pedidos (`Pantalla_04_EstadoPedidos`):** Control del estado de confección del vestido y accesorios.
5. **Catálogo Digital (`Pantalla_05_Catalogo`):** Visualización de prendas y accesorios disponibles con fotos y precios orientativos.

---

## Roles de Usuario Soportados en UI

- **Administradora:** Acceso total a gestión de pedidos, clientes, inventario y cuentas auxiliares.
- **Auxiliar** Registro de clientes/pedidos, consulta de agenda e inventario.

---

## Estructura del Módulo Frontend

```text
Frontend/
├── public/              # Archivos estáticos e imágenes
├── src/                 # Código fuente de la aplicación
│   ├── assets/          # Recursos gráficos y estilos
│   ├── components/      # Componentes reutilizables de UI
│   └── ...
├── index.html           # Punto de entrada HTML
├── package.json         # Dependencias y scripts
├── tsconfig.json        # Configuración de TypeScript
└── vite.config.ts       # Configuración del servidor Vite