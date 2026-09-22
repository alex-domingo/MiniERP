import { Component, inject } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSortModule } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Router } from '@angular/router';
import { filter } from 'rxjs';
import { confirmar } from '../../compartido/confirmar';
import { Listado } from '../../compartido/listado';
import { CatalogoApi, Consulta } from '../../nucleo/api';
import { Proveedor } from '../../nucleo/modelos';
import { Notificacion } from '../../nucleo/notificacion';
import { Sesion } from '../../nucleo/sesion';
import { ProveedorDialogo } from './proveedor-dialogo';

@Component({
  selector: 'app-proveedores',
  imports: [
    ReactiveFormsModule, MatTableModule, MatSortModule, MatPaginatorModule, MatFormFieldModule, MatInputModule,
    MatIconModule, MatButtonModule, MatButtonToggleModule, MatProgressBarModule, MatTooltipModule,
  ],
  templateUrl: './proveedores.html',
})
export class Proveedores extends Listado<Proveedor> {
  private readonly api = inject(CatalogoApi);
  private readonly dialogo = inject(MatDialog);
  private readonly aviso = inject(Notificacion);
  private readonly router = inject(Router);
  protected readonly puedeEditar = inject(Sesion).puede('editarProveedores');

  protected readonly columnas = ['nit', 'nombre', 'contacto', 'telefono', 'estado', ...(this.puedeEditar ? ['acciones'] : [])];

  protected consultar(consulta: Consulta) {
    return this.api.proveedores.listar(consulta);
  }

  protected ver(p: Proveedor): void {
    this.router.navigate(['/proveedores', p.idProveedor]);
  }

  protected abrir(proveedor: Proveedor | null, evento?: Event): void {
    evento?.stopPropagation();
    this.dialogo.open(ProveedorDialogo, { data: proveedor, width: '640px' })
      .afterClosed().pipe(filter(Boolean))
      .subscribe((p: Proveedor) => {
        this.aviso.exito(proveedor ? `Proveedor "${p.nombre}" actualizado` : `Proveedor "${p.nombre}" creado`);
        this.recargar();
      });
  }

  protected cambiarActivo(p: Proveedor, evento: Event): void {
    evento.stopPropagation();
    if (p.activo) {
      confirmar(this.dialogo, {
        titulo: 'Desactivar proveedor',
        mensaje: `"${p.nombre}" ya no podrá recibir compras nuevas. Sus compras anteriores se conservan.`,
        aceptar: 'Desactivar', peligro: true,
      }).pipe(filter(Boolean)).subscribe(() =>
        this.api.proveedores.desactivar(p.idProveedor).subscribe(() => {
          this.aviso.exito(`Proveedor "${p.nombre}" desactivado`);
          this.recargar();
        }));
    } else {
      this.api.proveedores.activar(p.idProveedor).subscribe(() => {
        this.aviso.exito(`Proveedor "${p.nombre}" activado`);
        this.recargar();
      });
    }
  }
}
