import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginatorModule } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatSortModule } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';
import { Router, RouterLink } from '@angular/router';
import { map } from 'rxjs';
import { MonedaPipe } from '../../compartido/formato';
import { Listado } from '../../compartido/listado';
import { Rango, RangoFechas } from '../../compartido/rango-fechas';
import { CatalogoApi, Consulta, OperacionesApi } from '../../nucleo/api';
import { CompraResumen } from '../../nucleo/modelos';
import { Sesion } from '../../nucleo/sesion';
import { fechaIso, seguro } from '../../nucleo/utilidades';

/** Historial de compras: se consultan, no se editan ni se eliminan. */
@Component({
  selector: 'app-compras',
  imports: [
    RouterLink, DatePipe, MatTableModule, MatSortModule, MatPaginatorModule, MatFormFieldModule, MatSelectModule,
    MatIconModule, MatButtonModule, MatProgressBarModule, MonedaPipe, RangoFechas,
  ],
  templateUrl: './compras.html',
})
export class Compras extends Listado<CompraResumen> {
  private readonly api = inject(OperacionesApi);
  private readonly router = inject(Router);
  protected readonly puedeRegistrar = inject(Sesion).puede('registrarCompra');

  // Incluye proveedores inactivos: sus compras siguen en el historial
  protected readonly proveedores = toSignal(
    seguro(inject(CatalogoApi).proveedores.listar({ tamano: 100, orden: 'nombre' }).pipe(map((p) => p.contenido)), []),
    { initialValue: [] },
  );
  protected readonly idProveedor = signal<number | null>(null);
  private rango: Rango = { desde: null, hasta: null };
  protected readonly columnas = ['numeroDocumento', 'fechaCompra', 'proveedor', 'usuario', 'total'];

  protected consultar({ busqueda: _b, activo: _a, ...resto }: Consulta) {
    return this.api.listarCompras(resto);
  }

  protected override filtrosExtra() {
    return { idProveedor: this.idProveedor(), desde: fechaIso(this.rango.desde), hasta: fechaIso(this.rango.hasta) };
  }

  protected filtrarProveedor(id: number | null): void {
    this.idProveedor.set(id);
    this.buscarDesdeInicio();
  }

  protected filtrarFechas(rango: Rango): void {
    this.rango = rango;
    this.buscarDesdeInicio();
  }

  protected ver(c: CompraResumen): void {
    this.router.navigate(['/compras', c.idCompra]);
  }
}
