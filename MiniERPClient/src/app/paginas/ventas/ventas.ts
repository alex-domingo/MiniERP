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
import { VentaResumen } from '../../nucleo/modelos';
import { Sesion } from '../../nucleo/sesion';
import { fechaIso, seguro } from '../../nucleo/utilidades';

/** Historial de ventas: se consultan, no se editan ni se eliminan. */
@Component({
  selector: 'app-ventas',
  imports: [
    RouterLink, DatePipe, MatTableModule, MatSortModule, MatPaginatorModule, MatFormFieldModule, MatSelectModule,
    MatIconModule, MatButtonModule, MatProgressBarModule, MonedaPipe, RangoFechas,
  ],
  templateUrl: './ventas.html',
})
export class Ventas extends Listado<VentaResumen> {
  private readonly api = inject(OperacionesApi);
  private readonly router = inject(Router);
  protected readonly puedeRegistrar = inject(Sesion).puede('registrarVenta');

  // Incluye clientes inactivos: sus ventas siguen en el historial
  protected readonly clientes = toSignal(
    seguro(inject(CatalogoApi).clientes.listar({ tamano: 100, orden: 'nombre' }).pipe(map((p) => p.contenido)), []),
    { initialValue: [] },
  );
  protected readonly idCliente = signal<number | null>(null);
  private rango: Rango = { desde: null, hasta: null };
  protected readonly columnas = ['numeroFactura', 'fechaVenta', 'cliente', 'usuario', 'subtotal', 'iva', 'total'];

  protected consultar({ busqueda: _b, activo: _a, ...resto }: Consulta) {
    return this.api.listarVentas(resto);
  }

  protected override filtrosExtra() {
    return { idCliente: this.idCliente(), desde: fechaIso(this.rango.desde), hasta: fechaIso(this.rango.hasta) };
  }

  protected filtrarCliente(id: number | null): void {
    this.idCliente.set(id);
    this.buscarDesdeInicio();
  }

  protected filtrarFechas(rango: Rango): void {
    this.rango = rango;
    this.buscarDesdeInicio();
  }

  protected ver(c: VentaResumen): void {
    this.router.navigate(['/ventas', c.idVenta]);
  }
}
