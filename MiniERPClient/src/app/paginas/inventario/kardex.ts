import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal, viewChild } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { BuscadorProducto } from '../../compartido/buscador-producto';
import { Existencia } from '../../compartido/existencia';
import { MonedaPipe } from '../../compartido/formato';
import { Rango, RangoFechas } from '../../compartido/rango-fechas';
import { CatalogoApi, InventarioApi } from '../../nucleo/api';
import { Movimiento, Producto, Valuacion } from '../../nucleo/modelos';
import { Sesion } from '../../nucleo/sesion';
import { etiqueta, fechaIso } from '../../nucleo/utilidades';

/**
 * Kardex: responde "como y por que cambio la existencia". Cada
 * movimiento muestra su origen (compra, venta o ajuste), el documento,
 * el tercero, la cantidad, el costo con que entro o salio y el saldo.
 * A la derecha, las capas de costo vigentes en el orden en que UEPS
 * las va a consumir.
 */
@Component({
  selector: 'app-kardex',
  imports: [
    RouterLink, DatePipe, MatTableModule, MatPaginatorModule, MatProgressBarModule, MatButtonModule, MatIconModule,
    MatTooltipModule, MonedaPipe, BuscadorProducto, RangoFechas, Existencia,
  ],
  templateUrl: './kardex.html',
})
export class Kardex implements OnInit {
  private readonly api = inject(InventarioApi);
  private readonly catalogo = inject(CatalogoApi);
  private readonly ruta = inject(ActivatedRoute);
  private readonly router = inject(Router);
  protected readonly puedeAjustar = inject(Sesion).puede('ajustarInventario');
  private readonly buscador = viewChild.required(BuscadorProducto);

  protected readonly producto = signal<Producto | null>(null);
  protected readonly movimientos = signal<Movimiento[]>([]);
  protected readonly valuacion = signal<Valuacion | null>(null);
  /** Capas con unidades, en el orden en que el servidor las consumira (ya vienen ordenadas por metodo). */
  protected readonly capasVigentes = computed(() => (this.valuacion()?.capas ?? []).filter((c) => c.cantidadDisponible > 0));
  protected readonly total = signal(0);
  protected readonly pagina = signal(0);
  protected readonly tamano = signal(20);
  protected readonly cargando = signal(false);
  private rango: Rango = { desde: null, hasta: null };
  protected readonly etiqueta = etiqueta;

  protected readonly columnas = ['fecha', 'origen', 'documento', 'tercero', 'entrada', 'salida', 'costo', 'saldo', 'usuario'];

  ngOnInit(): void {
    const id = Number(this.ruta.snapshot.queryParamMap.get('producto'));
    if (id) {
      this.catalogo.productos.obtener(id).subscribe((p) => {
        this.buscador().fijar(p);
        this.elegir(p, false);
      });
    }
  }

  protected elegir(p: Producto, actualizarUrl = true): void {
    this.producto.set(p);
    this.pagina.set(0);
    if (actualizarUrl) {
      this.router.navigate([], { queryParams: { producto: p.idProducto }, replaceUrl: true });
    }
    this.api.valuacion(p.idProducto).subscribe((v) => this.valuacion.set(v));
    this.cargar();
  }

  protected filtrar(rango: Rango): void {
    this.rango = rango;
    this.pagina.set(0);
    this.cargar();
  }

  protected cambiarPagina(e: PageEvent): void {
    this.pagina.set(e.pageIndex);
    this.tamano.set(e.pageSize);
    this.cargar();
  }

  private cargar(): void {
    const p = this.producto();
    if (!p) {
      return;
    }
    this.cargando.set(true);
    this.api.kardex(p.idProducto, {
      desde: fechaIso(this.rango.desde), hasta: fechaIso(this.rango.hasta),
      pagina: this.pagina(), tamano: this.tamano(),
    }).subscribe({
      next: (r) => {
        this.movimientos.set(r.contenido);
        this.total.set(r.totalElementos);
        this.cargando.set(false);
      },
      error: () => this.cargando.set(false),
    });
  }
}
