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
import { filter } from 'rxjs';
import { Listado } from '../../compartido/listado';
import { confirmar } from '../../compartido/confirmar';
import { CatalogoApi, Consulta } from '../../nucleo/api';
import { Categoria } from '../../nucleo/modelos';
import { Notificacion } from '../../nucleo/notificacion';
import { Sesion } from '../../nucleo/sesion';
import { CategoriaDialogo } from './categoria-dialogo';

@Component({
  selector: 'app-categorias',
  imports: [
    ReactiveFormsModule, MatTableModule, MatSortModule, MatPaginatorModule, MatFormFieldModule, MatInputModule,
    MatIconModule, MatButtonModule, MatButtonToggleModule, MatProgressBarModule, MatTooltipModule,
  ],
  templateUrl: './categorias.html',
})
export class Categorias extends Listado<Categoria> {
  private readonly api = inject(CatalogoApi);
  private readonly dialogo = inject(MatDialog);
  private readonly aviso = inject(Notificacion);
  protected readonly puedeEditar = inject(Sesion).puede('editarCatalogo');

  protected readonly columnas = ['nombre', 'descripcion', 'estado', ...(this.puedeEditar ? ['acciones'] : [])];

  protected consultar(consulta: Consulta) {
    return this.api.categorias.listar(consulta);
  }

  protected abrir(categoria: Categoria | null = null): void {
    this.dialogo.open(CategoriaDialogo, { data: categoria, width: '480px' })
      .afterClosed().pipe(filter(Boolean))
      .subscribe((c: Categoria) => {
        this.aviso.exito(categoria ? `Categoría "${c.nombre}" actualizada` : `Categoría "${c.nombre}" creada`);
        this.recargar();
      });
  }

  protected cambiarActivo(c: Categoria): void {
    if (c.activo) {
      confirmar(this.dialogo, {
        titulo: 'Desactivar categoría',
        mensaje: `"${c.nombre}" dejará de ofrecerse para productos nuevos. Su historial se conserva. ` +
          'El servidor no permite desactivarla si tiene productos activos.',
        aceptar: 'Desactivar', peligro: true,
      }).pipe(filter(Boolean)).subscribe(() =>
        this.api.categorias.desactivar(c.idCategoria).subscribe(() => {
          this.aviso.exito(`Categoría "${c.nombre}" desactivada`);
          this.recargar();
        }));
    } else {
      this.api.categorias.activar(c.idCategoria).subscribe(() => {
        this.aviso.exito(`Categoría "${c.nombre}" activada`);
        this.recargar();
      });
    }
  }
}
