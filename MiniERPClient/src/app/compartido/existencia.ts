import { Component, computed, input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

/**
 * Nivel de existencia de un producto, con las mismas reglas que la
 * vista vw_alerta_stock del servidor. Icono + texto: el color nunca va solo.
 */
@Component({
  selector: 'app-existencia',
  imports: [MatIconModule],
  template: `
    <span class="estado" [class]="'estado ' + nivel().clase">
      <mat-icon aria-hidden="true">{{ nivel().icono }}</mat-icon>{{ nivel().texto }}
    </span>
  `,
})
export class Existencia {
  readonly stock = input.required<number>();
  readonly minimo = input.required<number>();

  protected readonly nivel = computed(() => {
    const s = this.stock();
    const m = this.minimo();
    if (s === 0) return { clase: 'critico', icono: 'block', texto: 'Sin existencia' };
    if (s < m) return { clase: 'critico', icono: 'error', texto: 'Crítico' };
    if (s === m) return { clase: 'aviso', icono: 'warning', texto: 'En mínimo' };
    return { clase: 'ok', icono: 'check_circle', texto: 'Normal' };
  });
}
