import { Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginatorModule } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSortModule } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ReactiveFormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { RouterLink } from '@angular/router';
import { Existencia } from '../../compartido/existencia';
import { Listado } from '../../compartido/listado';
import { CatalogoApi, Consulta } from '../../nucleo/api';
import { Producto } from '../../nucleo/modelos';
import { Sesion } from '../../nucleo/sesion';
import { seguro } from '../../nucleo/utilidades';

/**
 * Existencias actuales y productos que requieren atencion (en o por
 * debajo de su stock minimo), con acceso directo al kardex y a los ajustes.
 */
@Component({
  selector: 'app-existencias',
  imports: [
    RouterLink, ReactiveFormsModule, MatTableModule, MatSortModule, MatPaginatorModule, MatButtonModule, MatIconModule,
    MatProgressBarModule, MatTooltipModule, MatFormFieldModule, MatInputModule, Existencia,
  ],
  templateUrl: './existencias.html',
})
export class Existencias extends Listado<Producto> {
  private readonly api = inject(CatalogoApi);
  protected readonly puedeAjustar = inject(Sesion).puede('ajustarInventario');

  protected readonly alertas = toSignal(seguro(this.api.alertasDeExistencia(), []), { initialValue: null });
  protected readonly sinExistencia = computed(() => (this.alertas() ?? []).filter((p) => p.stockActual === 0).length);
  protected readonly criticos = computed(() =>
    (this.alertas() ?? []).filter((p) => p.stockActual > 0 && p.stockActual < p.stockMinimo).length);
  protected readonly enMinimo = computed(() =>
    (this.alertas() ?? []).filter((p) => p.stockActual > 0 && p.stockActual === p.stockMinimo).length);

  protected readonly columnasAlerta = ['codigo', 'nombre', 'stock', 'faltante', 'nivel', 'acciones'];
  protected readonly columnas = ['codigo', 'nombre', 'categoria', 'stockActual', 'stockMinimo', 'nivel', 'acciones'];

  constructor() {
    super();
    this.orden = 'stockActual,asc';
  }

  protected consultar(consulta: Consulta) {
    return this.api.productos.listar(consulta);
  }
}
