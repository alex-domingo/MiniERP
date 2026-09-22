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
import { Cliente } from '../../nucleo/modelos';
import { Notificacion } from '../../nucleo/notificacion';
import { Sesion } from '../../nucleo/sesion';
import { ClienteDialogo } from './cliente-dialogo';

@Component({
  selector: 'app-clientes',
  imports: [
    ReactiveFormsModule, MatTableModule, MatSortModule, MatPaginatorModule, MatFormFieldModule, MatInputModule,
    MatIconModule, MatButtonModule, MatButtonToggleModule, MatProgressBarModule, MatTooltipModule,
  ],
  templateUrl: './clientes.html',
})
export class Clientes extends Listado<Cliente> {
  private readonly api = inject(CatalogoApi);
  private readonly dialogo = inject(MatDialog);
  private readonly aviso = inject(Notificacion);
  private readonly router = inject(Router);
  protected readonly puedeEditar = inject(Sesion).puede('editarClientes');

  protected readonly columnas = ['nit', 'nombre', 'telefono', 'correo', 'estado', ...(this.puedeEditar ? ['acciones'] : [])];

  protected consultar(consulta: Consulta) {
    return this.api.clientes.listar(consulta);
  }

  protected ver(c: Cliente): void {
    this.router.navigate(['/clientes', c.idCliente]);
  }

  protected abrir(cliente: Cliente | null, evento?: Event): void {
    evento?.stopPropagation();
    this.dialogo.open(ClienteDialogo, { data: cliente, width: '640px' })
      .afterClosed().pipe(filter(Boolean))
      .subscribe((c: Cliente) => {
        this.aviso.exito(cliente ? `Cliente "${c.nombre}" actualizado` : `Cliente "${c.nombre}" creado`);
        this.recargar();
      });
  }

  protected cambiarActivo(c: Cliente, evento: Event): void {
    evento.stopPropagation();
    if (c.activo) {
      confirmar(this.dialogo, {
        titulo: 'Desactivar cliente',
        mensaje: `"${c.nombre}" ya no podrá recibir ventas nuevas. Su historial de compras se conserva.`,
        aceptar: 'Desactivar', peligro: true,
      }).pipe(filter(Boolean)).subscribe(() =>
        this.api.clientes.desactivar(c.idCliente).subscribe(() => {
          this.aviso.exito(`Cliente "${c.nombre}" desactivado`);
          this.recargar();
        }));
    } else {
      this.api.clientes.activar(c.idCliente).subscribe(() => {
        this.aviso.exito(`Cliente "${c.nombre}" activado`);
        this.recargar();
      });
    }
  }
}
