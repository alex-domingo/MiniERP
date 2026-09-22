import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { InventarioApi } from '../../nucleo/api';
import { Verificacion as Resultado } from '../../nucleo/modelos';

/**
 * Ejecuta en el servidor las 10 comprobaciones de consistencia entre
 * existencias, kardex, capas de costo, documentos y totales. En un
 * sistema sano todas dan cero.
 */
@Component({
  selector: 'app-verificacion',
  imports: [DatePipe, MatButtonModule, MatIconModule, MatProgressBarModule],
  template: `
    <div class="pagina">
      <div class="encabezado">
        <div>
          <h1>Verificación de integridad</h1>
          <p class="subtitulo">Comprueba que existencias, kardex, capas de costo y documentos cuadran entre sí</p>
        </div>
        <span class="espacio"></span>
        <button mat-flat-button (click)="verificar()" [disabled]="cargando()"><mat-icon>refresh</mat-icon>Verificar de nuevo</button>
      </div>
      @if (cargando()) { <mat-progress-bar mode="indeterminate" style="margin-bottom:12px" /> }
      @if (resultado(); as r) {
        <div class="tarjeta" style="margin-bottom:16px; display:flex; gap:12px; align-items:center">
          <mat-icon [style.color]="r.consistente ? 'var(--erp-ok)' : 'var(--erp-critico)'" style="font-size:36px; width:36px; height:36px">
            {{ r.consistente ? 'verified' : 'report' }}</mat-icon>
          <div>
            <div style="font: var(--mat-sys-title-large)">{{ r.consistente ? 'Todo cuadra' : 'Se encontraron inconsistencias' }}</div>
            <div class="muted">Verificado el {{ r.fecha | date: "dd/MM/yyyy 'a las' HH:mm:ss" }}</div>
          </div>
        </div>
        <div class="tabla-contenedor">
          @for (c of r.comprobaciones; track c.nombre) {
            <div style="display:flex; gap:12px; align-items:flex-start; padding:12px 16px; border-top:1px solid var(--erp-borde)">
              <span class="estado" [class.ok]="c.fallos === 0" [class.critico]="c.fallos > 0" style="flex:none">
                <mat-icon>{{ c.fallos === 0 ? 'check' : 'close' }}</mat-icon>{{ c.fallos === 0 ? 'Correcto' : c.fallos + ' con error' }}
              </span>
              <div>
                <div><strong>{{ c.nombre }}</strong></div>
                <div class="muted">{{ c.descripcion }}</div>
              </div>
            </div>
          }
        </div>
      }
    </div>
  `,
})
export class Verificacion {
  private readonly api = inject(InventarioApi);
  protected readonly resultado = signal<Resultado | null>(null);
  protected readonly cargando = signal(false);

  constructor() {
    this.verificar();
  }

  protected verificar(): void {
    this.cargando.set(true);
    this.api.verificar().subscribe({
      next: (r) => {
        this.resultado.set(r);
        this.cargando.set(false);
      },
      error: () => this.cargando.set(false),
    });
  }
}
