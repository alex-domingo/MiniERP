import { DestroyRef, Directive, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl } from '@angular/forms';
import { PageEvent } from '@angular/material/paginator';
import { Sort } from '@angular/material/sort';
import { Observable, Subject, catchError, debounceTime, distinctUntilChanged, of, switchMap, tap } from 'rxjs';
import { Consulta } from '../nucleo/api';
import { Pagina } from '../nucleo/modelos';

export type FiltroEstado = 'activos' | 'inactivos' | 'todos';

/**
 * Comportamiento comun de los listados paginados EN EL SERVIDOR:
 * busqueda con espera de 300 ms, filtro activos/inactivos, orden por
 * columna y paginacion. switchMap descarta la respuesta de una consulta
 * vieja si el usuario ya pidio otra (escribir rapido no mezcla resultados).
 */
@Directive()
export abstract class Listado<T> implements OnInit {
  protected readonly filas = signal<T[]>([]);
  protected readonly total = signal(0);
  protected readonly cargando = signal(false);
  protected readonly pagina = signal(0);
  protected readonly tamano = signal(20);
  protected readonly estado = signal<FiltroEstado>('activos');
  protected readonly busqueda = new FormControl('', { nonNullable: true });
  protected orden: string | null = null;

  private readonly solicitudes = new Subject<void>();

  /** Llamada concreta al servidor con la consulta armada. */
  protected abstract consultar(consulta: Consulta): Observable<Pagina<T>>;

  /** Filtros propios de la pantalla (categoria, fechas...). */
  protected filtrosExtra(): Record<string, unknown> {
    return {};
  }

  constructor() {
    const destruir = inject(DestroyRef);
    this.busqueda.valueChanges
      .pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed(destruir))
      .subscribe(() => this.buscarDesdeInicio());

    this.solicitudes
      .pipe(
        tap(() => this.cargando.set(true)),
        switchMap(() =>
          this.consultar({
            busqueda: this.busqueda.value.trim() || null,
            activo: this.estado() === 'todos' ? null : this.estado() === 'activos',
            pagina: this.pagina(),
            tamano: this.tamano(),
            orden: this.orden,
            ...this.filtrosExtra(),
          }).pipe(catchError(() => of(null))),
        ),
        takeUntilDestroyed(destruir),
      )
      .subscribe((respuesta) => {
        this.cargando.set(false);
        if (respuesta) {
          this.filas.set(respuesta.contenido);
          this.total.set(respuesta.totalElementos);
        }
      });
  }

  ngOnInit(): void {
    this.recargar();
  }

  recargar(): void {
    this.solicitudes.next();
  }

  protected buscarDesdeInicio(): void {
    this.pagina.set(0);
    this.recargar();
  }

  protected cambiarPagina(evento: PageEvent): void {
    this.pagina.set(evento.pageIndex);
    this.tamano.set(evento.pageSize);
    this.recargar();
  }

  protected ordenar(orden: Sort): void {
    this.orden = orden.direction ? `${orden.active},${orden.direction}` : null;
    this.buscarDesdeInicio();
  }

  protected cambiarEstado(estado: FiltroEstado): void {
    this.estado.set(estado);
    this.buscarDesdeInicio();
  }
}
