import { Rol } from './modelos';

/**
 * Espejo, en el cliente, de la matriz de ConfiguracionSeguridad del
 * servidor. Sirve para mostrar u ocultar menus y botones; la seguridad
 * real la impone el servidor, que responde 403 aunque alguien fuerce la
 * interfaz.
 */
export const PERMISOS = {
  // Lectura
  verProveedores: ['ADMINISTRACION', 'COMPRAS', 'INVENTARIO'],
  verCompras: ['ADMINISTRACION', 'COMPRAS', 'INVENTARIO'],
  verClientes: ['ADMINISTRACION', 'VENTAS'],
  verVentas: ['ADMINISTRACION', 'VENTAS', 'INVENTARIO'],
  verInventario: ['ADMINISTRACION', 'COMPRAS', 'INVENTARIO'],

  // Escritura: solo el area duena del dato
  editarCatalogo: ['INVENTARIO'],
  editarProveedores: ['COMPRAS'],
  editarClientes: ['VENTAS'],
  registrarCompra: ['COMPRAS'],
  registrarVenta: ['VENTAS'],
  ajustarInventario: ['INVENTARIO'],
  emitirFactura: ['ADMINISTRACION', 'VENTAS'],

  // Reportes
  reportesInventario: ['ADMINISTRACION', 'INVENTARIO'],
  reportesCompras: ['ADMINISTRACION', 'COMPRAS'],
  reportesVentas: ['ADMINISTRACION', 'VENTAS'],
  reporteBitacora: ['ADMINISTRACION'],
} as const satisfies Record<string, readonly Rol[]>;

export type Permiso = keyof typeof PERMISOS;

export function permite(rol: Rol | null | undefined, permiso: Permiso): boolean {
  return !!rol && (PERMISOS[permiso] as readonly Rol[]).includes(rol);
}
