import { Component, computed, inject } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { Sesion } from '../../nucleo/sesion';
import { REPORTES } from './definiciones';

/** Indice de reportes: solo los del area del usuario (Administracion ve todos). */
@Component({
  selector: 'app-reportes',
  imports: [RouterLink, MatIconModule],
  template: `
    <div class="pagina">
      <div class="encabezado">
        <div>
          <h1>Reportes</h1>
          <p class="subtitulo">Cada reporte se puede filtrar, imprimir y exportar a CSV</p>
        </div>
      </div>
      @for (g of grupos(); track g.area) {
        <h2 class="titulo-grupo">{{ g.area }}</h2>
        <div class="rejilla" style="margin-bottom:24px">
          @for (r of g.reportes; track r.clave) {
            <a class="tarjeta tarjeta-reporte" [routerLink]="['/reportes', r.clave]">
              <mat-icon>{{ r.icono }}</mat-icon>
              <div>
                <strong>{{ r.titulo }}</strong>
                <p class="muted">{{ r.descripcion }}</p>
              </div>
            </a>
          }
        </div>
      }
    </div>
  `,
  styles: `
    .titulo-grupo { font: var(--mat-sys-title-medium); margin: 0 0 10px; color: var(--erp-texto-2); }
    .tarjeta-reporte {
      display: flex; gap: 12px; align-items: flex-start; text-decoration: none; color: inherit;
      transition: border-color .15s, box-shadow .15s;
      mat-icon { color: var(--erp-marca); flex: none; }
      p { margin: 4px 0 0; font: var(--mat-sys-body-small); }
      &:hover, &:focus-visible { border-color: var(--erp-grafica); box-shadow: 0 2px 10px rgba(31,56,100,.08); }
    }
  `,
})
export class Reportes {
  private readonly sesion = inject(Sesion);
  protected readonly grupos = computed(() => {
    const visibles = REPORTES.filter((r) => this.sesion.puede(r.permiso));
    const areas = [...new Set(visibles.map((r) => r.area))];
    return areas.map((area) => ({ area, reportes: visibles.filter((r) => r.area === area) }));
  });
}
