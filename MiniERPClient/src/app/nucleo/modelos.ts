/**
 * Contratos de la API REST del servidor Mini ERP.
 *
 * Cada interfaz refleja un DTO (record) de Spring Boot con los mismos
 * nombres de campo. Las fechas llegan como texto ISO-8601 sin zona
 * ("2026-01-22T15:43:00"), que el navegador interpreta en hora local,
 * la misma del servidor. Los montos llegan como numero.
 */

// ---------------------------------------------------------------------
//  Comunes
// ---------------------------------------------------------------------
export type Rol = 'ADMINISTRACION' | 'COMPRAS' | 'INVENTARIO' | 'VENTAS';
export type TipoMovimiento = 'ENTRADA' | 'SALIDA';
export type OrigenMovimiento = 'COMPRA' | 'VENTA' | 'AJUSTE_ENTRADA' | 'AJUSTE_SALIDA';
export type ModuloBitacora = 'AUTENTICACION' | 'USUARIOS' | 'CATEGORIAS' | 'PRODUCTOS' | 'PROVEEDORES'
  | 'COMPRAS' | 'INVENTARIO' | 'CLIENTES' | 'VENTAS' | 'REPORTES';
export type AccionBitacora = 'LOGIN' | 'LOGOUT' | 'LOGIN_FALLIDO' | 'CREAR' | 'ACTUALIZAR' | 'DESACTIVAR'
  | 'ACTIVAR' | 'CONSULTAR' | 'EXPORTAR' | 'ANULAR';
export type Agrupacion = 'DIA' | 'SEMANA' | 'MES' | 'TRIMESTRE' | 'ANIO';

export interface Pagina<T> {
  contenido: T[];
  pagina: number;
  tamano: number;
  totalElementos: number;
  totalPaginas: number;
  primera: boolean;
  ultima: boolean;
}

/** Error RFC 9457 (ProblemDetail) con los campos propios del servidor. */
export interface Problema {
  title?: string;
  status?: number;
  detail?: string;
  errores?: Record<string, string>;
  codigoProducto?: string;
  solicitado?: number;
  disponible?: number;
}

export interface Salud {
  estado: string;
  aplicacion: string;
  metodoValuacion: 'UEPS' | 'PEPS';
  iva: number;
  productosRegistrados: number;
  usuariosRegistrados: number;
}

// ---------------------------------------------------------------------
//  Autenticacion
// ---------------------------------------------------------------------
export interface RespuestaLogin {
  token: string;
  tipo: string;
  idUsuario: number;
  usuario: string;
  nombreCompleto: string;
  rol: Rol;
  expiraEn: string;
}

// ---------------------------------------------------------------------
//  Catalogos
// ---------------------------------------------------------------------
export interface Categoria {
  idCategoria: number;
  nombre: string;
  descripcion?: string;
  activo: boolean;
}
export interface CategoriaPeticion {
  nombre: string;
  descripcion?: string | null;
}

export interface Producto {
  idProducto: number;
  codigo: string;
  nombre: string;
  descripcion?: string;
  idCategoria: number;
  nombreCategoria: string;
  unidadMedida?: string;
  precioVenta: number;
  stockActual: number;
  stockMinimo: number;
  requiereAtencion: boolean;
  activo: boolean;
}
export interface ProductoPeticion {
  codigo: string;
  nombre: string;
  descripcion?: string | null;
  idCategoria: number;
  unidadMedida?: string | null;
  precioVenta: number;
  stockMinimo: number;
}

export interface Proveedor {
  idProveedor: number;
  nit: string;
  nombre: string;
  contacto?: string;
  telefono?: string;
  correo?: string;
  direccion?: string;
  activo: boolean;
}
export interface ProveedorPeticion {
  nit: string;
  nombre: string;
  contacto?: string | null;
  telefono?: string | null;
  correo?: string | null;
  direccion?: string | null;
}

/** Relacion N:M proveedor-producto con su costo de referencia. */
export interface ProveedorProducto {
  idProveedor: number;
  nombreProveedor: string;
  idProducto: number;
  codigoProducto: string;
  nombreProducto: string;
  costoReferencia: number;
}

export interface Cliente {
  idCliente: number;
  nit: string;
  nombre: string;
  telefono?: string;
  correo?: string;
  direccion?: string;
  activo: boolean;
}
export interface ClientePeticion {
  nit: string;
  nombre: string;
  telefono?: string | null;
  correo?: string | null;
  direccion?: string | null;
}

// ---------------------------------------------------------------------
//  Compras y ventas
// ---------------------------------------------------------------------
export interface CompraResumen {
  idCompra: number;
  numeroDocumento: string;
  idProveedor: number;
  nombreProveedor: string;
  usuario: string;
  fechaCompra: string;
  total: number;
}
export interface Compra {
  idCompra: number;
  numeroDocumento: string;
  idProveedor: number;
  nitProveedor: string;
  nombreProveedor: string;
  usuario: string;
  fechaCompra: string;
  total: number;
  observaciones?: string;
  lineas: {
    idDetalleCompra: number;
    idProducto: number;
    codigoProducto: string;
    nombreProducto: string;
    cantidad: number;
    costoUnitario: number;
    subtotal: number;
  }[];
}
export interface CompraPeticion {
  idProveedor: number;
  observaciones?: string | null;
  lineas: { idProducto: number; cantidad: number; costoUnitario: number }[];
}

export interface VentaResumen {
  idVenta: number;
  numeroFactura: string;
  idCliente: number;
  nombreCliente: string;
  usuario: string;
  fechaVenta: string;
  subtotal: number;
  iva: number;
  total: number;
}
export interface Venta {
  idVenta: number;
  numeroFactura: string;
  idCliente: number;
  nitCliente: string;
  nombreCliente: string;
  usuario: string;
  fechaVenta: string;
  subtotal: number;
  porcentajeIva: number;
  iva: number;
  total: number;
  lineas: {
    idDetalleVenta: number;
    idProducto: number;
    codigoProducto: string;
    nombreProducto: string;
    cantidad: number;
    precioUnitario: number;
    subtotal: number;
  }[];
}
export interface VentaPeticion {
  idCliente: number;
  lineas: { idProducto: number; cantidad: number }[];
}

// ---------------------------------------------------------------------
//  Inventario
// ---------------------------------------------------------------------
export interface Movimiento {
  idMovimiento: number;
  idProducto: number;
  codigoProducto: string;
  nombreProducto: string;
  fechaMovimiento: string;
  tipoMovimiento: TipoMovimiento;
  origen: OrigenMovimiento;
  documento: string;
  tercero?: string;
  cantidad: number;
  costoUnitario: number;
  valorMovimiento: number;
  existenciaAnterior: number;
  existenciaNueva: number;
  usuario: string;
  observaciones?: string;
}

export interface Valuacion {
  idProducto: number;
  codigo: string;
  nombre: string;
  metodo: 'UEPS' | 'PEPS';
  stockActual: number;
  unidadesEnCapas: number;
  valorTotal: number;
  costoPromedio: number;
  capas: {
    idCapa: number;
    fechaEntrada: string;
    origen: string;
    costoUnitario: number;
    cantidadInicial: number;
    cantidadDisponible: number;
    valorDisponible: number;
  }[];
}

export interface AjustePeticion {
  idProducto: number;
  tipo: TipoMovimiento;
  cantidad: number;
  costoUnitario?: number | null;
  motivo: string;
}

export interface Verificacion {
  consistente: boolean;
  fecha: string;
  comprobaciones: { nombre: string; descripcion: string; fallos: number }[];
}

// ---------------------------------------------------------------------
//  Reportes
// ---------------------------------------------------------------------
export interface Reporte<R = unknown, F = Record<string, unknown>> {
  titulo: string;
  generado: string;
  generadoPor: string;
  filtros: Record<string, unknown>;
  resumen?: R;
  filas: F[];
  pagina?: { numero: number; tamano: number; totalElementos: number; totalPaginas: number };
}
