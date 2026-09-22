import { Component, OnDestroy, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MonedaPipe } from '../../compartido/formato';
import { Venta } from '../../nucleo/modelos';
import { Facturas } from './factura';

/**
 * Confirmacion de la venta. La factura PDF se pide al servidor en el
 * mismo momento en que se registra la venta, tal como pide el
 * enunciado, y queda lista para abrir o descargar.
 */
@Component({
  selector: 'app-factura-emitida',
  imports: [MatDialogModule, MatButtonModule, MatIconModule, MatProgressBarModule, MonedaPipe],
  template: `
    <h2 mat-dialog-title><mat-icon style="vertical-align:middle; color:var(--erp-ok)">check_circle</mat-icon> Venta registrada</h2>
    <mat-dialog-content>
      <p>Factura <strong>{{ venta.numeroFactura }}</strong> para {{ venta.nombreCliente }}.</p>
      <div class="totales" style="justify-content:start; grid-template-columns: auto auto">
        <span>Subtotal</span><span class="v">{{ venta.subtotal | moneda }}</span>
        <span>IVA</span><span class="v">{{ venta.iva | moneda }}</span>
        <span class="total">Total</span><span class="v total">{{ venta.total | moneda }}</span>
      </div>
      @if (!factura() && !error()) { <mat-progress-bar mode="indeterminate" style="margin-top:16px" /> <p class="muted">Generando la factura…</p> }
      @if (error()) { <p class="mensaje-error">No se pudo generar la factura ahora. Puede emitirla desde el detalle de la venta.</p> }
      @if (factura(); as f) {
        <div style="display:flex; gap:8px; margin-top:16px; flex-wrap:wrap">
          <button mat-flat-button (click)="facturas.abrir(f.url)"><mat-icon>picture_as_pdf</mat-icon>Ver factura</button>
          <button mat-stroked-button (click)="facturas.descargar(f.archivo, f.nombre)"><mat-icon>download</mat-icon>Descargar</button>
        </div>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button [mat-dialog-close]="'detalle'">Ver detalle</button>
      <button mat-button [mat-dialog-close]="'nueva'">Nueva venta</button>
    </mat-dialog-actions>
  `,
})
export class FacturaEmitida implements OnDestroy {
  protected readonly venta = inject<Venta>(MAT_DIALOG_DATA);
  protected readonly facturas = inject(Facturas);
  protected readonly factura = signal<{ url: string; archivo: Blob; nombre: string } | null>(null);
  protected readonly error = signal(false);

  constructor() {
    this.facturas.obtener(this.venta.idVenta).subscribe({
      next: (f) => this.factura.set(f),
      error: () => this.error.set(true),
    });
  }

  ngOnDestroy(): void {
    const f = this.factura();
    if (f) {
      setTimeout(() => URL.revokeObjectURL(f.url), 60_000);
    }
  }
}
