import { FormatoValor } from '../../compartido/graficas';
import { Permiso } from '../../nucleo/permisos';

/**
 * Como se muestra el valor de una celda o de una cifra del resumen.
 * "clave" es texto corto que no debe partirse (codigo, NIT, documento).
 */
export type TipoCampo =
  | 'texto' | 'clave' | 'entero' | 'moneda' | 'moneda4' | 'porcentaje' | 'fecha' | 'fechaHora' | 'etiqueta' | 'nivel' | 'booleano';

export type Filtro =
  | 'fechas'              // rango opcional
  | 'fechasObligatorias'  // el reporte es "dentro de un rango de fechas"
  | 'categoria' | 'proveedor' | 'cliente' | 'producto'
  | 'limite' | 'soloAlertas' | 'agrupacion'
  | 'tipoMovimiento' | 'origen'
  | 'usuario' | 'modulo' | 'accion' | 'exitoso' | 'texto';

export interface Columna {
  campo: string;
  titulo: string;
  tipo?: TipoCampo;
}

export interface Grafica {
  tipo: 'barras' | 'columnas';
  titulo: string;
  etiqueta: (fila: Record<string, unknown>) => string;
  valor: string;
  formato: FormatoValor;
  detalle?: (fila: Record<string, unknown>) => string;
}

export interface DefinicionReporte {
  clave: string;
  area: 'Productos e inventario' | 'Compras y proveedores' | 'Ventas y clientes' | 'Logs';
  permiso: Permiso;
  icono: string;
  titulo: string;
  descripcion: string;
  /** Ruta bajo /api/reportes (puede llevar :idProducto). */
  ruta: string;
  filtros: Filtro[];
  columnas: Columna[];
  resumen?: Columna[];
  grafica?: Grafica;
  limite?: number;
  paginado?: boolean;
}

const nombreProducto = (f: Record<string, unknown>) => `${f['codigo']} · ${f['nombre']}`;

/** Los 12 reportes que pide el enunciado, en su mismo orden. */
export const REPORTES: DefinicionReporte[] = [
  // ------------------------------------------------ Productos e inventario
  {
    clave: 'mas-vendidos', area: 'Productos e inventario', permiso: 'reportesInventario', icono: 'trending_up',
    titulo: 'Top productos más vendidos',
    descripcion: 'Productos ordenados por unidades vendidas en el periodo.',
    ruta: 'inventario/mas-vendidos', filtros: ['fechas', 'categoria', 'limite'], limite: 10,
    columnas: [
      { campo: 'posicion', titulo: '#', tipo: 'entero' },
      { campo: 'codigo', titulo: 'Código', tipo: 'clave' },
      { campo: 'nombre', titulo: 'Producto' },
      { campo: 'categoria', titulo: 'Categoría' },
      { campo: 'unidadesVendidas', titulo: 'Unidades', tipo: 'entero' },
      { campo: 'numeroVentas', titulo: 'Ventas', tipo: 'entero' },
      { campo: 'ingresos', titulo: 'Ingresos (sin IVA)', tipo: 'moneda' },
      { campo: 'stockActual', titulo: 'Existencia', tipo: 'entero' },
    ],
    grafica: { tipo: 'barras', titulo: 'Unidades vendidas', etiqueta: nombreProducto, valor: 'unidadesVendidas',
      formato: 'entero', detalle: (f) => `${f['numeroVentas']} ventas` },
  },
  {
    clave: 'menor-existencia', area: 'Productos e inventario', permiso: 'reportesInventario', icono: 'inventory',
    titulo: 'Productos con menor existencia',
    descripcion: 'Productos activos ordenados de menor a mayor existencia, con su nivel respecto al mínimo.',
    ruta: 'inventario/menor-existencia', filtros: ['categoria', 'soloAlertas', 'limite'], limite: 10,
    columnas: [
      { campo: 'posicion', titulo: '#', tipo: 'entero' },
      { campo: 'codigo', titulo: 'Código', tipo: 'clave' },
      { campo: 'nombre', titulo: 'Producto' },
      { campo: 'categoria', titulo: 'Categoría' },
      { campo: 'stockActual', titulo: 'Existencia', tipo: 'entero' },
      { campo: 'stockMinimo', titulo: 'Mínimo', tipo: 'entero' },
      { campo: 'diferencia', titulo: 'Sobre el mínimo', tipo: 'entero' },
      { campo: 'nivel', titulo: 'Nivel', tipo: 'nivel' },
      { campo: 'valorInventario', titulo: 'Valor en inventario', tipo: 'moneda' },
      { campo: 'ultimaEntrada', titulo: 'Última entrada', tipo: 'fecha' },
    ],
    grafica: { tipo: 'barras', titulo: 'Existencia actual', etiqueta: nombreProducto, valor: 'stockActual',
      formato: 'entero', detalle: (f) => `Mínimo ${f['stockMinimo']}` },
  },
  {
    clave: 'mas-movimientos', area: 'Productos e inventario', permiso: 'reportesInventario', icono: 'swap_vert',
    titulo: 'Productos con más movimientos',
    descripcion: 'Productos con mayor cantidad de entradas y salidas en el kardex.',
    ruta: 'inventario/mas-movimientos', filtros: ['fechas', 'categoria', 'limite'], limite: 10,
    columnas: [
      { campo: 'posicion', titulo: '#', tipo: 'entero' },
      { campo: 'codigo', titulo: 'Código', tipo: 'clave' },
      { campo: 'nombre', titulo: 'Producto' },
      { campo: 'totalMovimientos', titulo: 'Movimientos', tipo: 'entero' },
      { campo: 'entradas', titulo: 'Entradas', tipo: 'entero' },
      { campo: 'salidas', titulo: 'Salidas', tipo: 'entero' },
      { campo: 'ajustes', titulo: 'Ajustes', tipo: 'entero' },
      { campo: 'unidadesEntrada', titulo: 'Unid. entrada', tipo: 'entero' },
      { campo: 'unidadesSalida', titulo: 'Unid. salida', tipo: 'entero' },
      { campo: 'ultimoMovimiento', titulo: 'Último', tipo: 'fecha' },
    ],
    grafica: { tipo: 'barras', titulo: 'Cantidad de movimientos', etiqueta: nombreProducto, valor: 'totalMovimientos',
      formato: 'entero', detalle: (f) => `${f['entradas']} entradas · ${f['salidas']} salidas` },
  },
  {
    clave: 'historial', area: 'Productos e inventario', permiso: 'reportesInventario', icono: 'receipt_long',
    titulo: 'Historial de movimientos de un producto',
    descripcion: 'Kardex cronológico de un producto: existencia inicial, entradas, salidas y existencia final del periodo.',
    ruta: 'inventario/historial/:idProducto', filtros: ['producto', 'fechas', 'tipoMovimiento', 'origen'], paginado: true,
    resumen: [
      { campo: 'existenciaInicial', titulo: 'Existencia inicial', tipo: 'entero' },
      { campo: 'unidadesEntrada', titulo: 'Entradas', tipo: 'entero' },
      { campo: 'unidadesSalida', titulo: 'Salidas', tipo: 'entero' },
      { campo: 'existenciaFinal', titulo: 'Existencia final', tipo: 'entero' },
      { campo: 'valorEntradas', titulo: 'Valor de entradas', tipo: 'moneda' },
      { campo: 'valorSalidas', titulo: 'Valor de salidas', tipo: 'moneda' },
    ],
    columnas: [
      { campo: 'fechaMovimiento', titulo: 'Fecha', tipo: 'fechaHora' },
      { campo: 'tipoMovimiento', titulo: 'Tipo', tipo: 'etiqueta' },
      { campo: 'origen', titulo: 'Origen', tipo: 'etiqueta' },
      { campo: 'documento', titulo: 'Documento', tipo: 'clave' },
      { campo: 'tercero', titulo: 'Proveedor / cliente' },
      { campo: 'existenciaAnterior', titulo: 'Anterior', tipo: 'entero' },
      { campo: 'cantidad', titulo: 'Cantidad', tipo: 'entero' },
      { campo: 'existenciaNueva', titulo: 'Saldo', tipo: 'entero' },
      { campo: 'costoUnitario', titulo: 'Costo unit.', tipo: 'moneda4' },
      { campo: 'valorMovimiento', titulo: 'Valor', tipo: 'moneda' },
      { campo: 'usuario', titulo: 'Usuario', tipo: 'clave' },
      { campo: 'observaciones', titulo: 'Observaciones' },
    ],
  },

  // ------------------------------------------------ Compras y proveedores
  {
    clave: 'compras-por-fechas', area: 'Compras y proveedores', permiso: 'reportesCompras', icono: 'date_range',
    titulo: 'Compras por rango de fechas',
    descripcion: 'Todas las compras realizadas dentro del periodo, con sus totales.',
    ruta: 'compras/por-fechas', filtros: ['fechasObligatorias', 'proveedor'],
    resumen: [
      { campo: 'numeroCompras', titulo: 'Compras', tipo: 'entero' },
      { campo: 'proveedoresDistintos', titulo: 'Proveedores', tipo: 'entero' },
      { campo: 'unidades', titulo: 'Unidades', tipo: 'entero' },
      { campo: 'montoTotal', titulo: 'Monto total', tipo: 'moneda' },
      { campo: 'promedioPorCompra', titulo: 'Promedio por compra', tipo: 'moneda' },
    ],
    columnas: [
      { campo: 'numeroDocumento', titulo: 'Documento', tipo: 'clave' },
      { campo: 'fechaCompra', titulo: 'Fecha', tipo: 'fechaHora' },
      { campo: 'proveedor', titulo: 'Proveedor' },
      { campo: 'nitProveedor', titulo: 'NIT', tipo: 'clave' },
      { campo: 'usuario', titulo: 'Registró', tipo: 'clave' },
      { campo: 'lineas', titulo: 'Líneas', tipo: 'entero' },
      { campo: 'unidades', titulo: 'Unidades', tipo: 'entero' },
      { campo: 'total', titulo: 'Total', tipo: 'moneda' },
    ],
  },
  {
    clave: 'top-proveedores', area: 'Compras y proveedores', permiso: 'reportesCompras', icono: 'local_shipping',
    titulo: 'Top proveedores por monto de compras',
    descripcion: 'Proveedores con mayor monto acumulado de compras y su participación en el total.',
    ruta: 'compras/top-proveedores', filtros: ['fechas', 'limite'], limite: 5,
    columnas: [
      { campo: 'posicion', titulo: '#', tipo: 'entero' },
      { campo: 'nombre', titulo: 'Proveedor' },
      { campo: 'nit', titulo: 'NIT', tipo: 'clave' },
      { campo: 'numeroCompras', titulo: 'Compras', tipo: 'entero' },
      { campo: 'productosDistintos', titulo: 'Productos', tipo: 'entero' },
      { campo: 'unidades', titulo: 'Unidades', tipo: 'entero' },
      { campo: 'montoTotal', titulo: 'Monto acumulado', tipo: 'moneda' },
      { campo: 'participacion', titulo: 'Participación', tipo: 'porcentaje' },
      { campo: 'ultimaCompra', titulo: 'Última compra', tipo: 'fecha' },
    ],
    grafica: { tipo: 'barras', titulo: 'Monto acumulado de compras', etiqueta: (f) => String(f['nombre']),
      valor: 'montoTotal', formato: 'moneda', detalle: (f) => `${f['participacion']} % del total` },
  },
  {
    clave: 'productos-frecuentes', area: 'Compras y proveedores', permiso: 'reportesCompras', icono: 'repeat',
    titulo: 'Productos adquiridos con mayor frecuencia',
    descripcion: 'Productos que aparecen en más compras distintas, con unidades, monto y costo promedio.',
    ruta: 'compras/productos-frecuentes', filtros: ['fechas', 'proveedor', 'categoria', 'limite'], limite: 10,
    columnas: [
      { campo: 'posicion', titulo: '#', tipo: 'entero' },
      { campo: 'codigo', titulo: 'Código', tipo: 'clave' },
      { campo: 'nombre', titulo: 'Producto' },
      { campo: 'categoria', titulo: 'Categoría' },
      { campo: 'numeroCompras', titulo: 'Compras', tipo: 'entero' },
      { campo: 'proveedoresDistintos', titulo: 'Proveedores', tipo: 'entero' },
      { campo: 'unidades', titulo: 'Unidades', tipo: 'entero' },
      { campo: 'montoTotal', titulo: 'Monto', tipo: 'moneda' },
      { campo: 'costoPromedio', titulo: 'Costo promedio', tipo: 'moneda4' },
      { campo: 'ultimaCompra', titulo: 'Última compra', tipo: 'fecha' },
    ],
    grafica: { tipo: 'barras', titulo: 'Compras en que aparece', etiqueta: nombreProducto, valor: 'numeroCompras',
      formato: 'entero', detalle: (f) => `${f['unidades']} unidades` },
  },

  // ------------------------------------------------ Ventas y clientes
  {
    clave: 'ventas-por-fechas', area: 'Ventas y clientes', permiso: 'reportesVentas', icono: 'date_range',
    titulo: 'Ventas por rango de fechas',
    descripcion: 'Todas las ventas realizadas dentro del periodo, con subtotal, IVA y total.',
    ruta: 'ventas/por-fechas', filtros: ['fechasObligatorias', 'cliente'],
    resumen: [
      { campo: 'numeroVentas', titulo: 'Ventas', tipo: 'entero' },
      { campo: 'clientesDistintos', titulo: 'Clientes', tipo: 'entero' },
      { campo: 'subtotal', titulo: 'Subtotal', tipo: 'moneda' },
      { campo: 'iva', titulo: 'IVA', tipo: 'moneda' },
      { campo: 'total', titulo: 'Total', tipo: 'moneda' },
      { campo: 'ticketPromedio', titulo: 'Ticket promedio', tipo: 'moneda' },
    ],
    columnas: [
      { campo: 'numeroFactura', titulo: 'Factura', tipo: 'clave' },
      { campo: 'fechaVenta', titulo: 'Fecha', tipo: 'fechaHora' },
      { campo: 'cliente', titulo: 'Cliente' },
      { campo: 'nitCliente', titulo: 'NIT', tipo: 'clave' },
      { campo: 'usuario', titulo: 'Vendió', tipo: 'clave' },
      { campo: 'unidades', titulo: 'Unidades', tipo: 'entero' },
      { campo: 'subtotal', titulo: 'Subtotal', tipo: 'moneda' },
      { campo: 'iva', titulo: 'IVA', tipo: 'moneda' },
      { campo: 'total', titulo: 'Total', tipo: 'moneda' },
    ],
  },
  {
    clave: 'top-clientes', area: 'Ventas y clientes', permiso: 'reportesVentas', icono: 'workspace_premium',
    titulo: 'Top clientes por monto de compras',
    descripcion: 'Clientes con mayor monto acumulado (IVA incluido) y su participación en el total vendido.',
    ruta: 'ventas/top-clientes', filtros: ['fechas', 'limite'], limite: 10,
    columnas: [
      { campo: 'posicion', titulo: '#', tipo: 'entero' },
      { campo: 'nombre', titulo: 'Cliente' },
      { campo: 'nit', titulo: 'NIT', tipo: 'clave' },
      { campo: 'numeroCompras', titulo: 'Compras', tipo: 'entero' },
      { campo: 'unidades', titulo: 'Unidades', tipo: 'entero' },
      { campo: 'montoTotal', titulo: 'Monto acumulado', tipo: 'moneda' },
      { campo: 'ticketPromedio', titulo: 'Ticket promedio', tipo: 'moneda' },
      { campo: 'participacion', titulo: 'Participación', tipo: 'porcentaje' },
      { campo: 'ultimaCompra', titulo: 'Última compra', tipo: 'fecha' },
    ],
    grafica: { tipo: 'barras', titulo: 'Monto acumulado por cliente', etiqueta: (f) => String(f['nombre']),
      valor: 'montoTotal', formato: 'moneda', detalle: (f) => `${f['numeroCompras']} compras · ${f['participacion']} %` },
  },
  {
    clave: 'top-productos', area: 'Ventas y clientes', permiso: 'reportesVentas', icono: 'paid',
    titulo: 'Top productos por ingresos',
    descripcion: 'Productos que más ingresos generaron (subtotal sin IVA).',
    ruta: 'ventas/top-productos', filtros: ['fechas', 'categoria', 'limite'], limite: 10,
    columnas: [
      { campo: 'posicion', titulo: '#', tipo: 'entero' },
      { campo: 'codigo', titulo: 'Código', tipo: 'clave' },
      { campo: 'nombre', titulo: 'Producto' },
      { campo: 'categoria', titulo: 'Categoría' },
      { campo: 'unidades', titulo: 'Unidades', tipo: 'entero' },
      { campo: 'numeroVentas', titulo: 'Ventas', tipo: 'entero' },
      { campo: 'ingresos', titulo: 'Ingresos', tipo: 'moneda' },
      { campo: 'precioPromedio', titulo: 'Precio promedio', tipo: 'moneda' },
      { campo: 'participacion', titulo: 'Participación', tipo: 'porcentaje' },
    ],
    grafica: { tipo: 'barras', titulo: 'Ingresos por producto (sin IVA)', etiqueta: nombreProducto, valor: 'ingresos',
      formato: 'moneda', detalle: (f) => `${f['unidades']} unidades · ${f['participacion']} %` },
  },
  {
    clave: 'ventas-por-periodo', area: 'Ventas y clientes', permiso: 'reportesVentas', icono: 'calendar_month',
    titulo: 'Resumen de ventas por periodo',
    descripcion: 'Ventas agrupadas por día, semana, mes, trimestre o año, incluidos los periodos sin ventas.',
    ruta: 'ventas/por-periodo', filtros: ['fechas', 'agrupacion'],
    resumen: [
      { campo: 'numeroVentas', titulo: 'Ventas', tipo: 'entero' },
      { campo: 'unidades', titulo: 'Unidades', tipo: 'entero' },
      { campo: 'subtotal', titulo: 'Subtotal', tipo: 'moneda' },
      { campo: 'iva', titulo: 'IVA', tipo: 'moneda' },
      { campo: 'total', titulo: 'Total', tipo: 'moneda' },
      { campo: 'ticketPromedio', titulo: 'Ticket promedio', tipo: 'moneda' },
    ],
    columnas: [
      { campo: 'etiqueta', titulo: 'Periodo' },
      { campo: 'numeroVentas', titulo: 'Ventas', tipo: 'entero' },
      { campo: 'clientesDistintos', titulo: 'Clientes', tipo: 'entero' },
      { campo: 'unidades', titulo: 'Unidades', tipo: 'entero' },
      { campo: 'subtotal', titulo: 'Subtotal', tipo: 'moneda' },
      { campo: 'iva', titulo: 'IVA', tipo: 'moneda' },
      { campo: 'total', titulo: 'Total', tipo: 'moneda' },
      { campo: 'ticketPromedio', titulo: 'Ticket promedio', tipo: 'moneda' },
    ],
    grafica: { tipo: 'columnas', titulo: 'Total vendido por periodo (IVA incluido)', etiqueta: (f) => String(f['etiqueta']),
      valor: 'total', formato: 'moneda', detalle: (f) => `${f['numeroVentas']} ventas` },
  },

  // ------------------------------------------------ Logs
  {
    clave: 'bitacora', area: 'Logs', permiso: 'reporteBitacora', icono: 'history',
    titulo: 'Bitácora de interacciones',
    descripcion: 'Acciones de los usuarios en cada módulo: inicios de sesión, creaciones, actualizaciones, consultas y exportaciones.',
    ruta: 'bitacora', filtros: ['fechas', 'usuario', 'modulo', 'accion', 'exitoso', 'texto'], paginado: true,
    resumen: [
      { campo: 'total', titulo: 'Registros', tipo: 'entero' },
      { campo: 'exitosos', titulo: 'Exitosos', tipo: 'entero' },
      { campo: 'fallidos', titulo: 'Fallidos', tipo: 'entero' },
      { campo: 'usuariosDistintos', titulo: 'Usuarios', tipo: 'entero' },
    ],
    columnas: [
      { campo: 'fechaHora', titulo: 'Fecha y hora', tipo: 'fechaHora' },
      { campo: 'usuario', titulo: 'Usuario', tipo: 'clave' },
      { campo: 'modulo', titulo: 'Módulo', tipo: 'etiqueta' },
      { campo: 'accion', titulo: 'Acción', tipo: 'etiqueta' },
      { campo: 'descripcion', titulo: 'Descripción' },
      { campo: 'exitoso', titulo: 'Resultado', tipo: 'booleano' },
    ],
  },
];

export function buscarReporte(clave: string): DefinicionReporte | undefined {
  return REPORTES.find((r) => r.clave === clave);
}
