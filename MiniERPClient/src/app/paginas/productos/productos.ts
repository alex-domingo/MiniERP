import { Component, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatSortModule } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Router } from '@angular/router';
import { filter } from 'rxjs';
import { confirmar } from '../../compartido/confirmar';
import { Existencia } from '../../compartido/existencia';
import { MonedaPipe } from '../../compartido/formato';
import { Listado } from '../../compartido/listado';
import { CatalogoApi, Consulta } from '../../nucleo/api';
import { Producto } from '../../nucleo/modelos';
import { Notificacion } from '../../nucleo/notificacion';
import { Sesion } from '../../nucleo/sesion';
import { ProductoDialogo } from './producto-dialogo';
import { seguro } from '../../nucleo/utilidades';

@Component({
  selector: 'app-productos',
  imports: [
    ReactiveFormsModule, MatTableModule, MatSortModule, MatPaginatorModule, MatFormFieldModule, MatInputModule,
    MatSelectModule, MatIconModule, MatButtonModule, MatButtonToggleModule, MatProgressBarModule, MatTooltipModule,
    MonedaPipe, Existencia,
  ],
  templateUrl: './productos.html',
})
export class Productos extends Listado<Producto> {
  private readonly api = inject(CatalogoApi);
  private readonly dialogo = inject(MatDialog);
  private readonly aviso = inject(Notificacion);
  private readonly router = inject(Router);
  protected readonly puedeEditar = inject(Sesion).puede('editarCatalogo');

  protected readonly categorias = toSignal(seguro(this.api.categoriasActivas(), []), { initialValue: [] });
  protected readonly idCategoria = signal<number | null>(null);
  protected readonly columnas = ['codigo', 'nombre', 'categoria', 'precioVenta', 'stockActual', 'nivel',
    ...(this.puedeEditar ? ['acciones'] : [])];

  protected consultar(consulta: Consulta) {
    return this.api.productos.listar(consulta);
  }

  protected override filtrosExtra() {
    return { idCategoria: this.idCategoria() };
  }

  protected filtrarCategoria(id: number | null): void {
    this.idCategoria.set(id);
    this.buscarDesdeInicio();
  }

  protected ver(p: Producto): void {
    this.router.navigate(['/productos', p.idProducto]);
  }

  protected abrir(producto: Producto | null, evento?: Event): void {
    evento?.stopPropagation();
    this.dialogo.open(ProductoDialogo, { data: producto, width: '640px' })
      .afterClosed().pipe(filter(Boolean))
      .subscribe((p: Producto) => {
        this.aviso.exito(producto ? `Producto ${p.codigo} actualizado` : `Producto ${p.codigo} creado`);
        this.recargar();
      });
  }

  protected cambiarActivo(p: Producto, evento: Event): void {
    evento.stopPropagation();
    if (p.activo) {
      confirmar(this.dialogo, {
        titulo: 'Desactivar producto',
        mensaje: `${p.codigo} - ${p.nombre} dejará de venderse y comprarse. Su historial se conserva. ` +
          'El servidor no permite desactivar un producto que todavía tiene existencias.',
        aceptar: 'Desactivar', peligro: true,
      }).pipe(filter(Boolean)).subscribe(() =>
        this.api.productos.desactivar(p.idProducto).subscribe(() => {
          this.aviso.exito(`Producto ${p.codigo} desactivado`);
          this.recargar();
        }));
    } else {
      this.api.productos.activar(p.idProducto).subscribe(() => {
        this.aviso.exito(`Producto ${p.codigo} activado`);
        this.recargar();
      });
    }
  }
}
