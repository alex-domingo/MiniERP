import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, input, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterLink } from '@angular/router';
import { filter } from 'rxjs';
import { BuscadorProducto } from '../../compartido/buscador-producto';
import { confirmar } from '../../compartido/confirmar';
import { MonedaPipe } from '../../compartido/formato';
import { maxDecimales } from '../../compartido/formularios';
import { CatalogoApi, OperacionesApi } from '../../nucleo/api';
import { CompraResumen, Producto, Proveedor, ProveedorProducto } from '../../nucleo/modelos';
import { Notificacion } from '../../nucleo/notificacion';
import { Sesion } from '../../nucleo/sesion';
import { ProveedorDialogo } from './proveedor-dialogo';

/**
 * Ficha del proveedor. Aqui se administra el lado "proveedor" de la
 * relacion N:M: que productos suministra y a que costo de referencia.
 * Ese costo es el que se propone al registrar una compra.
 */
@Component({
  selector: 'app-proveedor-detalle',
  imports: [
    RouterLink, DatePipe, ReactiveFormsModule, MatButtonModule, MatIconModule, MatTableModule, MatFormFieldModule,
    MatInputModule, MatProgressBarModule, MatTooltipModule, MonedaPipe, BuscadorProducto,
  ],
  templateUrl: './proveedor-detalle.html',
})
export class ProveedorDetalle implements OnInit {
  readonly id = input.required<string>();

  private readonly api = inject(CatalogoApi);
  private readonly operaciones = inject(OperacionesApi);
  private readonly dialogo = inject(MatDialog);
  private readonly aviso = inject(Notificacion);
  protected readonly puedeEditar = inject(Sesion).puede('editarProveedores');

  protected readonly proveedor = signal<Proveedor | null>(null);
  protected readonly productos = signal<ProveedorProducto[]>([]);
  protected readonly compras = signal<CompraResumen[]>([]);
  protected readonly totalCompras = signal(0);

  // Asociar un producto nuevo
  protected readonly nuevo = signal<Producto | null>(null);
  protected readonly costoNuevo = new FormControl<number | null>(null, [Validators.required, Validators.min(0.01), maxDecimales(2)]);

  // Editar el costo en la misma fila
  protected readonly editando = signal<number | null>(null);
  protected readonly costoEditado = new FormControl<number | null>(null, [Validators.required, Validators.min(0.01), maxDecimales(2)]);

  protected readonly columnas = ['codigo', 'nombre', 'costo', ...(this.puedeEditar ? ['acciones'] : [])];

  private get idProveedor(): number {
    return Number(this.id());
  }

  ngOnInit(): void {
    this.api.proveedores.obtener(this.idProveedor).subscribe((p) => this.proveedor.set(p));
    this.cargarProductos();
    this.operaciones.listarCompras({ idProveedor: this.idProveedor, tamano: 8 }).subscribe((r) => {
      this.compras.set(r.contenido);
      this.totalCompras.set(r.totalElementos);
    });
  }

  private cargarProductos(): void {
    this.api.productosDeProveedor(this.idProveedor).subscribe((l) => this.productos.set(l));
  }

  protected yaAsociado = (p: Producto): boolean => this.productos().some((a) => a.idProducto === p.idProducto);

  protected elegirNuevo(p: Producto): void {
    this.nuevo.set(p);
    this.costoNuevo.reset(null);
  }

  protected asociar(): void {
    const p = this.nuevo();
    if (!p || this.costoNuevo.invalid) {
      this.costoNuevo.markAsTouched();
      return;
    }
    this.api.asociar(this.idProveedor, p.idProducto, this.costoNuevo.value!).subscribe(() => {
      this.aviso.exito(`${p.codigo} asociado al proveedor`);
      this.nuevo.set(null);
      this.cargarProductos();
    });
  }

  protected editar(fila: ProveedorProducto): void {
    this.editando.set(fila.idProducto);
    this.costoEditado.setValue(fila.costoReferencia);
  }

  protected guardarCosto(fila: ProveedorProducto): void {
    if (this.costoEditado.invalid) {
      return;
    }
    this.api.actualizarCosto(this.idProveedor, fila.idProducto, this.costoEditado.value!).subscribe(() => {
      this.aviso.exito(`Costo de ${fila.codigoProducto} actualizado`);
      this.editando.set(null);
      this.cargarProductos();
    });
  }

  protected quitar(fila: ProveedorProducto): void {
    confirmar(this.dialogo, {
      titulo: 'Quitar producto',
      mensaje: `${fila.codigoProducto} - ${fila.nombreProducto} ya no se podrá comprar a este proveedor. ` +
        'Las compras anteriores no se modifican.',
      aceptar: 'Quitar', peligro: true,
    }).pipe(filter(Boolean)).subscribe(() =>
      this.api.desasociar(this.idProveedor, fila.idProducto).subscribe(() => {
        this.aviso.exito(`${fila.codigoProducto} quitado del proveedor`);
        this.cargarProductos();
      }));
  }

  protected editarProveedor(): void {
    this.dialogo.open(ProveedorDialogo, { data: this.proveedor(), width: '640px' })
      .afterClosed().pipe(filter(Boolean))
      .subscribe((p: Proveedor) => this.proveedor.set(p));
  }
}
