import { HttpClient, HttpContext, HttpResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map, shareReplay } from 'rxjs';
import { SIN_AVISO } from './contexto';
import {
  AjustePeticion, Agrupacion, Categoria, CategoriaPeticion, Cliente, ClientePeticion, Compra,
  CompraPeticion, CompraResumen, Movimiento, Pagina, Producto, ProductoPeticion, Proveedor,
  ProveedorPeticion, ProveedorProducto, Reporte, Salud, Valuacion, Venta, VentaPeticion, VentaResumen,
  Verificacion,
} from './modelos';
import { API, parametros } from './utilidades';

/** Filtros comunes de los listados paginados. */
export interface Consulta {
  busqueda?: string | null;
  activo?: boolean | null;
  pagina?: number;
  tamano?: number;
  orden?: string | null;
  [otro: string]: unknown;
}

/** Operaciones CRUD que comparten los cuatro catalogos. */
class Catalogo<T, P> {
  constructor(private readonly http: HttpClient, private readonly ruta: string) {}

  listar(consulta: Consulta = {}): Observable<Pagina<T>> {
    return this.http.get<Pagina<T>>(`${API}/${this.ruta}`, { params: parametros(consulta) });
  }
  obtener(id: number): Observable<T> {
    return this.http.get<T>(`${API}/${this.ruta}/${id}`);
  }
  /** Los formularios muestran los errores por campo: sin aviso generico. */
  crear(datos: P): Observable<T> {
    return this.http.post<T>(`${API}/${this.ruta}`, datos, { context: silencioso() });
  }
  actualizar(id: number, datos: P): Observable<T> {
    return this.http.put<T>(`${API}/${this.ruta}/${id}`, datos, { context: silencioso() });
  }
  desactivar(id: number): Observable<T> {
    return this.http.delete<T>(`${API}/${this.ruta}/${id}`);
  }
  activar(id: number): Observable<T> {
    return this.http.patch<T>(`${API}/${this.ruta}/${id}/activar`, null);
  }
}

function silencioso(): HttpContext {
  return new HttpContext().set(SIN_AVISO, true);
}

// ---------------------------------------------------------------------

@Injectable({ providedIn: 'root' })
export class SistemaApi {
  private readonly http = inject(HttpClient);
  /** IVA y metodo de valuacion vigentes; se pide una vez por sesion de navegador. */
  readonly salud$ = this.http.get<Salud>(`${API}/salud`).pipe(shareReplay(1));
}

@Injectable({ providedIn: 'root' })
export class CatalogoApi {
  private readonly http = inject(HttpClient);

  readonly categorias = new Catalogo<Categoria, CategoriaPeticion>(this.http, 'categorias');
  readonly productos = new Catalogo<Producto, ProductoPeticion>(this.http, 'productos');
  readonly proveedores = new Catalogo<Proveedor, ProveedorPeticion>(this.http, 'proveedores');
  readonly clientes = new Catalogo<Cliente, ClientePeticion>(this.http, 'clientes');

  categoriasActivas(): Observable<Categoria[]> {
    return this.http.get<Categoria[]>(`${API}/categorias/activas`);
  }
  proveedoresActivos(): Observable<Proveedor[]> {
    return this.http.get<Proveedor[]>(`${API}/proveedores/activos`);
  }
  clientesActivos(): Observable<Cliente[]> {
    return this.http.get<Cliente[]>(`${API}/clientes/activos`);
  }
  alertasDeExistencia(): Observable<Producto[]> {
    return this.http.get<Producto[]>(`${API}/productos/alertas`);
  }
  /** Busqueda para autocompletar (codigo o nombre). */
  buscarProductos(texto: string, soloActivos = true, tamano = 15): Observable<Producto[]> {
    return this.productos
      .listar({ busqueda: texto, activo: soloActivos ? true : null, tamano, orden: 'codigo' })
      .pipe(map((p) => p.contenido));
  }

  // --- Relacion N:M proveedor-producto ---
  productosDeProveedor(idProveedor: number): Observable<ProveedorProducto[]> {
    return this.http.get<ProveedorProducto[]>(`${API}/proveedores/${idProveedor}/productos`);
  }
  proveedoresDeProducto(idProducto: number): Observable<ProveedorProducto[]> {
    return this.http.get<ProveedorProducto[]>(`${API}/proveedores/por-producto/${idProducto}`);
  }
  asociar(idProveedor: number, idProducto: number, costoReferencia: number): Observable<ProveedorProducto> {
    return this.http.post<ProveedorProducto>(`${API}/proveedores/${idProveedor}/productos`,
      { idProducto, costoReferencia });
  }
  actualizarCosto(idProveedor: number, idProducto: number, costoReferencia: number): Observable<ProveedorProducto> {
    return this.http.put<ProveedorProducto>(`${API}/proveedores/${idProveedor}/productos/${idProducto}`,
      { costoReferencia });
  }
  desasociar(idProveedor: number, idProducto: number): Observable<ProveedorProducto> {
    return this.http.delete<ProveedorProducto>(`${API}/proveedores/${idProveedor}/productos/${idProducto}`);
  }
}

@Injectable({ providedIn: 'root' })
export class OperacionesApi {
  private readonly http = inject(HttpClient);

  listarCompras(consulta: Consulta): Observable<Pagina<CompraResumen>> {
    return this.http.get<Pagina<CompraResumen>>(`${API}/compras`, { params: parametros(consulta) });
  }
  obtenerCompra(id: number): Observable<Compra> {
    return this.http.get<Compra>(`${API}/compras/${id}`);
  }
  registrarCompra(datos: CompraPeticion): Observable<Compra> {
    return this.http.post<Compra>(`${API}/compras`, datos, { context: silencioso() });
  }

  listarVentas(consulta: Consulta): Observable<Pagina<VentaResumen>> {
    return this.http.get<Pagina<VentaResumen>>(`${API}/ventas`, { params: parametros(consulta) });
  }
  obtenerVenta(id: number): Observable<Venta> {
    return this.http.get<Venta>(`${API}/ventas/${id}`);
  }
  registrarVenta(datos: VentaPeticion): Observable<Venta> {
    return this.http.post<Venta>(`${API}/ventas`, datos, { context: silencioso() });
  }

  /** Factura PDF como Blob, con el nombre de archivo que propone el servidor. */
  factura(idVenta: number): Observable<{ archivo: Blob; nombre: string }> {
    return this.http
      .get(`${API}/ventas/${idVenta}/factura`, { responseType: 'blob', observe: 'response' })
      .pipe(map((r: HttpResponse<Blob>) => ({
        archivo: r.body!,
        nombre: /filename="?([^";]+)"?/.exec(r.headers.get('Content-Disposition') ?? '')?.[1] ?? `factura-${idVenta}.pdf`,
      })));
  }
}

@Injectable({ providedIn: 'root' })
export class InventarioApi {
  private readonly http = inject(HttpClient);

  kardex(idProducto: number, consulta: Consulta): Observable<Pagina<Movimiento>> {
    return this.http.get<Pagina<Movimiento>>(`${API}/inventario/kardex/${idProducto}`, { params: parametros(consulta) });
  }
  valuacion(idProducto: number): Observable<Valuacion> {
    return this.http.get<Valuacion>(`${API}/inventario/valuacion/${idProducto}`);
  }
  ajustar(datos: AjustePeticion): Observable<Movimiento> {
    return this.http.post<Movimiento>(`${API}/inventario/ajustes`, datos, { context: silencioso() });
  }
  verificar(): Observable<Verificacion> {
    return this.http.get<Verificacion>(`${API}/inventario/verificacion`);
  }
}

@Injectable({ providedIn: 'root' })
export class ReportesApi {
  private readonly http = inject(HttpClient);

  /** ruta relativa a /api/reportes, p. ej. "ventas/top-clientes". */
  consultar<R = unknown, F = Record<string, unknown>>(ruta: string, filtros: Record<string, unknown>): Observable<Reporte<R, F>> {
    return this.http.get<Reporte<R, F>>(`${API}/reportes/${ruta}`, { params: parametros(filtros) });
  }
}

export type { Agrupacion };
