import { DatePipe, LowerCasePipe } from '@angular/common';
import { Component, OnInit, inject, input, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTableModule } from '@angular/material/table';
import { RouterLink } from '@angular/router';
import { filter } from 'rxjs';
import { Existencia } from '../../compartido/existencia';
import { MonedaPipe } from '../../compartido/formato';
import { CatalogoApi, InventarioApi } from '../../nucleo/api';
import { Movimiento, Producto, ProveedorProducto } from '../../nucleo/modelos';
import { Sesion } from '../../nucleo/sesion';
import { etiqueta } from '../../nucleo/utilidades';
import { ProductoDialogo } from './producto-dialogo';

@Component({
  selector: 'app-producto-detalle',
  imports: [RouterLink, DatePipe, LowerCasePipe, MatButtonModule, MatIconModule, MatTableModule, MatProgressBarModule, MonedaPipe, Existencia],
  templateUrl: './producto-detalle.html',
})
export class ProductoDetalle implements OnInit {
  /** Parametro :id de la ruta (withComponentInputBinding). */
  readonly id = input.required<string>();

  private readonly api = inject(CatalogoApi);
  private readonly inventario = inject(InventarioApi);
  private readonly dialogo = inject(MatDialog);
  private readonly sesion = inject(Sesion);

  protected readonly puedeEditar = this.sesion.puede('editarCatalogo');
  protected readonly verProveedores = this.sesion.puede('verProveedores');
  protected readonly verInventario = this.sesion.puede('verInventario');
  protected readonly verReporte = this.sesion.puede('reportesInventario');

  protected readonly producto = signal<Producto | null>(null);
  protected readonly proveedores = signal<ProveedorProducto[]>([]);
  protected readonly movimientos = signal<Movimiento[]>([]);
  protected readonly etiqueta = etiqueta;

  ngOnInit(): void {
    const id = Number(this.id());
    this.api.productos.obtener(id).subscribe((p) => this.producto.set(p));
    if (this.verProveedores) {
      this.api.proveedoresDeProducto(id).subscribe((l) => this.proveedores.set(l));
    }
    if (this.verInventario) {
      this.inventario.kardex(id, { tamano: 8 }).subscribe((k) => this.movimientos.set(k.contenido));
    }
  }

  protected editar(): void {
    this.dialogo.open(ProductoDialogo, { data: this.producto(), width: '640px' })
      .afterClosed().pipe(filter(Boolean))
      .subscribe((p: Producto) => this.producto.set(p));
  }
}
