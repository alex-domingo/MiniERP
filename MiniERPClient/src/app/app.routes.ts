import { Routes } from '@angular/router';
import { autenticado, conPermiso, invitado } from './nucleo/guardias';

/**
 * Todas las paginas se cargan bajo demanda (loadComponent): el usuario de
 * Ventas nunca descarga el codigo de Compras. El guardia conPermiso lee
 * data.permiso y aplica la misma matriz que el servidor.
 */
export const routes: Routes = [
  { path: 'login', canActivate: [invitado], title: 'Iniciar sesión · Mini ERP',
    loadComponent: () => import('./paginas/login/login').then((m) => m.Login) },
  {
    path: '',
    canActivate: [autenticado],
    loadComponent: () => import('./diseno/marco').then((m) => m.Marco),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'inicio' },
      { path: 'inicio', title: 'Inicio · Mini ERP',
        loadComponent: () => import('./paginas/inicio/inicio').then((m) => m.Inicio) },

      // --- Catalogo ---
      { path: 'categorias', title: 'Categorías · Mini ERP',
        loadComponent: () => import('./paginas/categorias/categorias').then((m) => m.Categorias) },
      { path: 'productos', title: 'Productos · Mini ERP',
        loadComponent: () => import('./paginas/productos/productos').then((m) => m.Productos) },
      { path: 'productos/:id', title: 'Producto · Mini ERP',
        loadComponent: () => import('./paginas/productos/producto-detalle').then((m) => m.ProductoDetalle) },

      // --- Compras ---
      { path: 'proveedores', title: 'Proveedores · Mini ERP', canActivate: [conPermiso], data: { permiso: 'verProveedores' },
        loadComponent: () => import('./paginas/proveedores/proveedores').then((m) => m.Proveedores) },
      { path: 'proveedores/:id', title: 'Proveedor · Mini ERP', canActivate: [conPermiso], data: { permiso: 'verProveedores' },
        loadComponent: () => import('./paginas/proveedores/proveedor-detalle').then((m) => m.ProveedorDetalle) },
      { path: 'compras', title: 'Compras · Mini ERP', canActivate: [conPermiso], data: { permiso: 'verCompras' },
        loadComponent: () => import('./paginas/compras/compras').then((m) => m.Compras) },
      { path: 'compras/nueva', title: 'Nueva compra · Mini ERP', canActivate: [conPermiso], data: { permiso: 'registrarCompra' },
        loadComponent: () => import('./paginas/compras/compra-nueva').then((m) => m.CompraNueva) },
      { path: 'compras/:id', title: 'Compra · Mini ERP', canActivate: [conPermiso], data: { permiso: 'verCompras' },
        loadComponent: () => import('./paginas/compras/compra-detalle').then((m) => m.CompraDetalle) },

      // --- Ventas ---
      { path: 'clientes', title: 'Clientes · Mini ERP', canActivate: [conPermiso], data: { permiso: 'verClientes' },
        loadComponent: () => import('./paginas/clientes/clientes').then((m) => m.Clientes) },
      { path: 'clientes/:id', title: 'Cliente · Mini ERP', canActivate: [conPermiso], data: { permiso: 'verClientes' },
        loadComponent: () => import('./paginas/clientes/cliente-detalle').then((m) => m.ClienteDetalle) },
      { path: 'ventas', title: 'Ventas · Mini ERP', canActivate: [conPermiso], data: { permiso: 'verVentas' },
        loadComponent: () => import('./paginas/ventas/ventas').then((m) => m.Ventas) },
      { path: 'ventas/nueva', title: 'Nueva venta · Mini ERP', canActivate: [conPermiso], data: { permiso: 'registrarVenta' },
        loadComponent: () => import('./paginas/ventas/venta-nueva').then((m) => m.VentaNueva) },
      { path: 'ventas/:id', title: 'Venta · Mini ERP', canActivate: [conPermiso], data: { permiso: 'verVentas' },
        loadComponent: () => import('./paginas/ventas/venta-detalle').then((m) => m.VentaDetalle) },

      // --- Inventario ---
      { path: 'inventario/existencias', title: 'Existencias · Mini ERP', canActivate: [conPermiso], data: { permiso: 'verInventario' },
        loadComponent: () => import('./paginas/inventario/existencias').then((m) => m.Existencias) },
      { path: 'inventario/kardex', title: 'Kardex · Mini ERP', canActivate: [conPermiso], data: { permiso: 'verInventario' },
        loadComponent: () => import('./paginas/inventario/kardex').then((m) => m.Kardex) },
      { path: 'inventario/ajustes', title: 'Ajustes · Mini ERP', canActivate: [conPermiso], data: { permiso: 'ajustarInventario' },
        loadComponent: () => import('./paginas/inventario/ajustes').then((m) => m.Ajustes) },
      { path: 'inventario/verificacion', title: 'Verificación · Mini ERP', canActivate: [conPermiso], data: { permiso: 'verInventario' },
        loadComponent: () => import('./paginas/inventario/verificacion').then((m) => m.Verificacion) },

      // --- Reportes ---
      { path: 'reportes', title: 'Reportes · Mini ERP',
        loadComponent: () => import('./paginas/reportes/reportes').then((m) => m.Reportes) },
      { path: 'reportes/:clave', title: 'Reporte · Mini ERP',
        loadComponent: () => import('./paginas/reportes/reporte').then((m) => m.Reporte) },
    ],
  },
  { path: '**', redirectTo: '' },
];
