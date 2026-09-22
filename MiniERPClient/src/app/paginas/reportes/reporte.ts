import { DatePipe, formatDate } from '@angular/common';
import { Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { Router, RouterLink } from '@angular/router';
import { BuscadorProducto } from '../../compartido/buscador-producto';
import { entero, moneda } from '../../compartido/formato';
import { Dato, GraficaBarras, GraficaColumnas } from '../../compartido/graficas';
import { CatalogoApi, ReportesApi } from '../../nucleo/api';
import { Producto, Reporte as RespuestaReporte } from '../../nucleo/modelos';
import { Notificacion } from '../../nucleo/notificacion';
import { Sesion } from '../../nucleo/sesion';
import { aCsv, descargar, etiqueta, fechaIso } from '../../nucleo/utilidades';
import { Columna, DefinicionReporte, Filtro, TipoCampo, buscarReporte } from './definiciones';

type Fila = Record<string, unknown>;
interface Opcion { id: number; nombre: string }
interface Conteo { clave: string; cantidad: number }

/**
 * Visor unico de los 12 reportes. Toma la definicion (filtros, columnas,
 * resumen y grafica) y arma la pantalla; los datos vienen ya calculados
 * del servidor, que es quien ejecuta el SQL.
 *
 * Los reportes se generan con el boton, no a cada cambio de filtro: cada
 * consulta queda en la bitacora y no tendria sentido registrar diez
 * consultas intermedias mientras el usuario elige fechas.
 */
@Component({
  selector: 'app-reporte',
  imports: [
    RouterLink, DatePipe, ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatDatepickerModule,
    MatCheckboxModule, MatButtonModule, MatIconModule, MatTableModule, MatPaginatorModule, MatProgressBarModule,
    GraficaBarras, GraficaColumnas, BuscadorProducto,
  ],
  templateUrl: './reporte.html',
})
export class Reporte implements OnInit {
  readonly clave = input.required<string>();

  private readonly api = inject(ReportesApi);
  private readonly catalogo = inject(CatalogoApi);
  private readonly sesion = inject(Sesion);
  private readonly router = inject(Router);
  private readonly aviso = inject(Notificacion);

  protected def!: DefinicionReporte;
  protected readonly resultado = signal<RespuestaReporte<Fila, Fila> | null>(null);
  protected readonly cargando = signal(false);
  protected readonly producto = signal<Producto | null>(null);
  protected readonly pagina = signal(0);
  protected readonly tamano = signal(50);
  protected readonly descripcionFiltros = signal('');

  protected readonly filtros = new FormGroup({
    desde: new FormControl<Date | null>(null),
    hasta: new FormControl<Date | null>(null),
    idCategoria: new FormControl<number | null>(null),
    idProveedor: new FormControl<number | null>(null),
    idCliente: new FormControl<number | null>(null),
    limite: new FormControl<number | null>(10, [Validators.min(1), Validators.max(100)]),
    soloAlertas: new FormControl(false, { nonNullable: true }),
    agrupacion: new FormControl('MES', { nonNullable: true }),
    tipo: new FormControl<string | null>(null),
    origen: new FormControl<string | null>(null),
    usuario: new FormControl('', { nonNullable: true }),
    modulo: new FormControl<string | null>(null),
    accion: new FormControl<string | null>(null),
    exitoso: new FormControl<boolean | null>(null),
    texto: new FormControl('', { nonNullable: true }),
  });

  protected readonly categorias = signal<Opcion[]>([]);
  protected readonly proveedores = signal<Opcion[]>([]);
  protected readonly clientes = signal<Opcion[]>([]);

  protected readonly modulos = ['AUTENTICACION', 'CATEGORIAS', 'PRODUCTOS', 'PROVEEDORES', 'COMPRAS', 'INVENTARIO',
    'CLIENTES', 'VENTAS', 'REPORTES'];
  protected readonly acciones = ['LOGIN', 'LOGOUT', 'LOGIN_FALLIDO', 'CREAR', 'ACTUALIZAR', 'DESACTIVAR', 'ACTIVAR',
    'CONSULTAR', 'EXPORTAR'];
  protected readonly origenes = ['COMPRA', 'VENTA', 'AJUSTE_ENTRADA', 'AJUSTE_SALIDA'];
  protected readonly agrupaciones = ['DIA', 'SEMANA', 'MES', 'TRIMESTRE', 'ANIO'];
  protected readonly etiqueta = etiqueta;

  protected readonly columnasVisibles = computed(() => this.def?.columnas.map((c) => c.campo) ?? []);
  protected readonly datosGrafica = computed<Dato[]>(() => {
    const r = this.resultado();
    const g = this.def?.grafica;
    if (!r || !g) return [];
    return r.filas.map((f) => ({ etiqueta: g.etiqueta(f), valor: Number(f[g.valor]) || 0, detalle: g.detalle?.(f) }));
  });
  /** La bitacora trae en el resumen los conteos por modulo, accion y usuario. */
  protected readonly conteos = computed(() => {
    const r = this.resultado()?.resumen as Record<string, Conteo[]> | undefined;
    const aDatos = (lista?: Conteo[], etiquetar = true): Dato[] =>
      (lista ?? []).map((c) => ({ etiqueta: etiquetar ? etiqueta(c.clave) : c.clave, valor: c.cantidad }));
    return r && this.def?.clave === 'bitacora'
      ? { modulo: aDatos(r['porModulo']), accion: aDatos(r['porAccion']), usuario: aDatos(r['porUsuario'], false) }
      : null;
  });

  ngOnInit(): void {
    const def = buscarReporte(this.clave());
    if (!def || !this.sesion.puede(def.permiso)) {
      this.aviso.error(def ? 'Su área no tiene acceso a ese reporte.' : 'El reporte no existe.');
      this.router.navigate(['/reportes']);
      return;
    }
    this.def = def;
    this.filtros.controls.limite.setValue(def.limite ?? 10);
    if (this.tiene('fechasObligatorias')) {
      const hoy = new Date();
      this.filtros.patchValue({ desde: new Date(hoy.getFullYear(), 0, 1), hasta: hoy });
    }
    // Incluye elementos inactivos: el historial se puede consultar aunque ya no se usen
    if (this.tiene('categoria')) {
      this.catalogo.categorias.listar({ tamano: 100, orden: 'nombre' })
        .subscribe((p) => this.categorias.set(p.contenido.map((c) => ({ id: c.idCategoria, nombre: c.nombre }))));
    }
    if (this.tiene('proveedor')) {
      this.catalogo.proveedores.listar({ tamano: 100, orden: 'nombre' })
        .subscribe((p) => this.proveedores.set(p.contenido.map((c) => ({ id: c.idProveedor, nombre: c.nombre }))));
    }
    if (this.tiene('cliente')) {
      this.catalogo.clientes.listar({ tamano: 100, orden: 'nombre' })
        .subscribe((p) => this.clientes.set(p.contenido.map((c) => ({ id: c.idCliente, nombre: c.nombre }))));
    }
    // El historial necesita un producto antes de poder generarse
    if (!this.tiene('producto')) {
      this.generar();
    }
  }

  protected tiene(f: Filtro): boolean {
    return this.def.filtros.includes(f);
  }

  protected elegirProducto(p: Producto): void {
    this.producto.set(p);
    this.generar();
  }

  protected generar(pagina = 0): void {
    if (this.filtros.invalid) {
      this.filtros.markAllAsTouched();
      return;
    }
    if (this.tiene('producto') && !this.producto()) {
      this.aviso.error('Elija un producto.');
      return;
    }
    const f = this.filtros.getRawValue();
    const obligatorias = this.tiene('fechasObligatorias');
    if (obligatorias && (!f.desde || !f.hasta)) {
      this.aviso.error('Este reporte requiere el rango de fechas completo.');
      return;
    }
    const p: Record<string, unknown> = {};
    if (this.tiene('fechas') || obligatorias) { p['desde'] = fechaIso(f.desde); p['hasta'] = fechaIso(f.hasta); }
    if (this.tiene('categoria')) p['idCategoria'] = f.idCategoria;
    if (this.tiene('proveedor')) p['idProveedor'] = f.idProveedor;
    if (this.tiene('cliente')) p['idCliente'] = f.idCliente;
    if (this.tiene('limite')) p['limite'] = f.limite;
    if (this.tiene('soloAlertas')) p['soloAlertas'] = f.soloAlertas;
    if (this.tiene('agrupacion')) p['agrupacion'] = f.agrupacion;
    if (this.tiene('tipoMovimiento')) p['tipo'] = f.tipo;
    if (this.tiene('origen')) p['origen'] = f.origen;
    if (this.tiene('usuario')) p['usuario'] = f.usuario.trim();
    if (this.tiene('modulo')) p['modulo'] = f.modulo;
    if (this.tiene('accion')) p['accion'] = f.accion;
    if (this.tiene('exitoso')) p['exitoso'] = f.exitoso;
    if (this.tiene('texto')) p['texto'] = f.texto.trim();
    if (this.def.paginado) { p['pagina'] = pagina; p['tamano'] = this.tamano(); }

    const ruta = this.def.ruta.replace(':idProducto', String(this.producto()?.idProducto ?? ''));
    this.cargando.set(true);
    this.pagina.set(pagina);
    this.api.consultar<Fila, Fila>(ruta, p).subscribe({
      next: (r) => {
        this.resultado.set(r);
        this.cargando.set(false);
        this.descripcionFiltros.set(this.describir(r.filtros));
      },
      error: () => this.cargando.set(false),
    });
  }

  protected cambiarPagina(e: PageEvent): void {
    this.tamano.set(e.pageSize);
    this.generar(e.pageIndex);
  }

  protected imprimir(): void {
    window.print();
  }

  protected exportar(): void {
    const r = this.resultado();
    if (!r) return;
    const cols = this.def.columnas;
    const filas = r.filas.map((f) => cols.map((c) => this.valorCsv(f[c.campo], c.tipo)));
    const nombre = `${this.def.clave}-${formatDate(new Date(), 'yyyyMMdd-HHmm', 'es-GT')}.csv`;
    descargar(aCsv(cols.map((c) => c.titulo), filas), nombre);
  }

  // --- presentacion de valores ------------------------------------------

  protected esNumero(tipo?: TipoCampo): boolean {
    return tipo === 'entero' || tipo === 'moneda' || tipo === 'moneda4' || tipo === 'porcentaje';
  }

  protected formatear(valor: unknown, tipo?: TipoCampo): string {
    if (valor === null || valor === undefined || valor === '') return '';
    switch (tipo) {
      case 'entero': return entero(Number(valor));
      case 'moneda': return moneda(Number(valor));
      case 'moneda4': return moneda(Number(valor), 4);
      case 'porcentaje': return `${Number(valor).toFixed(2)} %`;
      case 'fecha': return formatDate(String(valor), 'dd/MM/yyyy', 'es-GT');
      case 'fechaHora': return formatDate(String(valor), 'dd/MM/yyyy HH:mm', 'es-GT');
      case 'etiqueta': case 'nivel': return etiqueta(valor);
      case 'booleano': return valor ? 'Exitoso' : 'Fallido';
      default: return String(valor);
    }
  }

  /** En el CSV los numeros van sin formato para que Excel los sume. */
  private valorCsv(valor: unknown, tipo?: TipoCampo): unknown {
    if (this.esNumero(tipo)) return valor === null || valor === undefined ? '' : String(valor).replace('.', ',');
    return this.formatear(valor, tipo);
  }

  protected claseNivel(valor: unknown): string {
    switch (valor) {
      case 'SIN_EXISTENCIA': case 'CRITICO': return 'estado critico';
      case 'EN_MINIMO': return 'estado aviso';
      default: return 'estado ok';
    }
  }

  protected valorResumen(c: Columna): string {
    const r = this.resultado()?.resumen as Fila | undefined;
    return r ? this.formatear(r[c.campo], c.tipo) : '';
  }

  /** Texto de los filtros aplicados, con nombres en vez de identificadores. */
  private describir(filtros: Record<string, unknown>): string {
    const partes: string[] = [];
    const fecha = (v: unknown) => formatDate(String(v), 'dd/MM/yyyy', 'es-GT');
    if (filtros['desde'] || filtros['hasta']) {
      partes.push(`Del ${filtros['desde'] ? fecha(filtros['desde']) : 'inicio'} al ${filtros['hasta'] ? fecha(filtros['hasta']) : 'hoy'}`);
    } else if (this.tiene('fechas')) {
      partes.push('Todo el historial');
    }
    const nombres: Record<string, string> = {
      idCategoria: 'Categoría', idProveedor: 'Proveedor', idCliente: 'Cliente', limite: 'Límite',
      soloAlertas: 'Solo alertas', agrupacion: 'Agrupación', tipo: 'Tipo', origen: 'Origen', usuario: 'Usuario',
      modulo: 'Módulo', accion: 'Acción', exitoso: 'Resultado', texto: 'Texto',
    };
    for (const [k, v] of Object.entries(filtros)) {
      if (k === 'desde' || k === 'hasta' || k === 'idProducto' || v === null || v === false) continue;
      let texto = String(v);
      if (k === 'idCategoria' || k === 'idProveedor' || k === 'idCliente') {
        texto = this.nombreDe(k, Number(v)) ?? texto;
      } else if (k === 'exitoso') {
        texto = v ? 'Exitosos' : 'Fallidos';
      } else if (k === 'soloAlertas') {
        texto = 'Sí';
      } else {
        texto = etiqueta(v);
      }
      partes.push(`${nombres[k] ?? k}: ${texto}`);
    }
    return partes.join(' · ');
  }

  private nombreDe(tipo: string, id: number): string | undefined {
    const lista = tipo === 'idCategoria' ? this.categorias() : tipo === 'idProveedor' ? this.proveedores() : this.clientes();
    return lista.find((o) => o.id === id)?.nombre;
  }
}
