import { Permiso } from '../nucleo/permisos';
import { Rol } from '../nucleo/modelos';

export interface OpcionMenu {
  texto: string;
  icono: string;
  ruta: string;
  /** Visible si el rol tiene este permiso... */
  permiso?: Permiso;
  /** ...o si el rol esta en esta lista. Sin ninguno de los dos: visible para todos. */
  roles?: Rol[];
}

export interface GrupoMenu {
  titulo?: string;
  opciones: OpcionMenu[];
}

export const MENU: GrupoMenu[] = [
  { opciones: [{ texto: 'Inicio', icono: 'dashboard', ruta: '/inicio' }] },
  {
    titulo: 'Catálogo',
    opciones: [
      { texto: 'Categorías', icono: 'category', ruta: '/categorias', roles: ['ADMINISTRACION', 'INVENTARIO'] },
      { texto: 'Productos', icono: 'chair', ruta: '/productos' },
    ],
  },
  {
    titulo: 'Compras',
    opciones: [
      { texto: 'Proveedores', icono: 'local_shipping', ruta: '/proveedores', permiso: 'verProveedores' },
      { texto: 'Compras', icono: 'shopping_cart', ruta: '/compras', permiso: 'verCompras' },
    ],
  },
  {
    titulo: 'Ventas',
    opciones: [
      { texto: 'Clientes', icono: 'groups', ruta: '/clientes', permiso: 'verClientes' },
      { texto: 'Ventas', icono: 'point_of_sale', ruta: '/ventas', permiso: 'verVentas' },
    ],
  },
  {
    titulo: 'Inventario',
    opciones: [
      { texto: 'Existencias y alertas', icono: 'inventory_2', ruta: '/inventario/existencias', permiso: 'verInventario' },
      { texto: 'Kardex', icono: 'receipt_long', ruta: '/inventario/kardex', permiso: 'verInventario' },
      { texto: 'Ajustes', icono: 'tune', ruta: '/inventario/ajustes', permiso: 'ajustarInventario' },
      { texto: 'Verificación', icono: 'fact_check', ruta: '/inventario/verificacion', permiso: 'verInventario' },
    ],
  },
  {
    titulo: 'Análisis',
    opciones: [
      { texto: 'Reportes', icono: 'bar_chart', ruta: '/reportes',
        roles: ['ADMINISTRACION', 'COMPRAS', 'INVENTARIO', 'VENTAS'] },
      { texto: 'Bitácora', icono: 'history', ruta: '/reportes/bitacora', permiso: 'reporteBitacora' },
    ],
  },
];
