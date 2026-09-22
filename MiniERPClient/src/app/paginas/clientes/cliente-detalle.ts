import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, input, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTableModule } from '@angular/material/table';
import { Router, RouterLink } from '@angular/router';
import { filter } from 'rxjs';
import { MonedaPipe } from '../../compartido/formato';
import { CatalogoApi, OperacionesApi } from '../../nucleo/api';
import { Cliente, VentaResumen } from '../../nucleo/modelos';
import { Sesion } from '../../nucleo/sesion';
import { ClienteDialogo } from './cliente-dialogo';

/** Ficha del cliente con el historial completo de sus compras (ventas de la empresa). */
@Component({
  selector: 'app-cliente-detalle',
  imports: [RouterLink, DatePipe, MatButtonModule, MatIconModule, MatTableModule, MatPaginatorModule, MatProgressBarModule, MonedaPipe],
  templateUrl: './cliente-detalle.html',
})
export class ClienteDetalle implements OnInit {
  readonly id = input.required<string>();

  private readonly api = inject(CatalogoApi);
  private readonly operaciones = inject(OperacionesApi);
  private readonly dialogo = inject(MatDialog);
  private readonly router = inject(Router);
  private readonly sesion = inject(Sesion);
  protected readonly puedeEditar = this.sesion.puede('editarClientes');
  protected readonly puedeVender = this.sesion.puede('registrarVenta');

  protected readonly cliente = signal<Cliente | null>(null);
  protected readonly ventas = signal<VentaResumen[]>([]);
  protected readonly total = signal(0);
  protected readonly pagina = signal(0);
  protected readonly cargando = signal(false);
  protected readonly columnas = ['factura', 'fecha', 'subtotal', 'iva', 'total'];

  ngOnInit(): void {
    this.api.clientes.obtener(Number(this.id())).subscribe((c) => this.cliente.set(c));
    this.cargarVentas();
  }

  protected cargarVentas(evento?: PageEvent): void {
    if (evento) {
      this.pagina.set(evento.pageIndex);
    }
    this.cargando.set(true);
    this.operaciones.listarVentas({ idCliente: Number(this.id()), pagina: this.pagina(), tamano: 10 })
      .subscribe({
        next: (r) => {
          this.ventas.set(r.contenido);
          this.total.set(r.totalElementos);
          this.cargando.set(false);
        },
        error: () => this.cargando.set(false),
      });
  }

  protected verVenta(v: VentaResumen): void {
    this.router.navigate(['/ventas', v.idVenta]);
  }

  protected editar(): void {
    this.dialogo.open(ClienteDialogo, { data: this.cliente(), width: '640px' })
      .afterClosed().pipe(filter(Boolean))
      .subscribe((c: Cliente) => this.cliente.set(c));
  }
}
