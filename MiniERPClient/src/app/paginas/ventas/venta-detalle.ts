import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, inject, input, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTableModule } from '@angular/material/table';
import { RouterLink } from '@angular/router';
import { MonedaPipe } from '../../compartido/formato';
import { OperacionesApi } from '../../nucleo/api';
import { Venta } from '../../nucleo/modelos';
import { Sesion } from '../../nucleo/sesion';
import { Facturas } from './factura';

@Component({
  selector: 'app-venta-detalle',
  imports: [RouterLink, DatePipe, DecimalPipe, MatButtonModule, MatIconModule, MatTableModule, MatProgressBarModule, MonedaPipe],
  template: `
    <div class="pagina">
      @if (venta(); as v) {
        <div class="encabezado">
          <button mat-icon-button routerLink="/ventas" aria-label="Volver a ventas"><mat-icon>arrow_back</mat-icon></button>
          <div>
            <h1>Venta {{ v.numeroFactura }}</h1>
            <p class="subtitulo">{{ v.fechaVenta | date: "dd/MM/yyyy 'a las' HH:mm" }} · vendió {{ v.usuario }}</p>
          </div>
          <span class="espacio"></span>
          @if (puedeFacturar) {
            <button mat-stroked-button (click)="descargar(v)" [disabled]="generando()"><mat-icon>download</mat-icon>Descargar factura</button>
            <button mat-flat-button (click)="ver(v)" [disabled]="generando()"><mat-icon>picture_as_pdf</mat-icon>Ver factura</button>
          }
        </div>
        @if (generando()) { <mat-progress-bar mode="indeterminate" style="margin-bottom:8px" /> }

        <div class="tarjeta" style="margin-bottom:16px">
          <div class="rejilla">
            <div><div class="muted">Cliente</div>
              @if (verClientes) { <a [routerLink]="['/clientes', v.idCliente]">{{ v.nombreCliente }}</a> } @else { {{ v.nombreCliente }} }
            </div>
            <div><div class="muted">NIT</div>{{ v.nitCliente }}</div>
            <div><div class="muted">Tasa de IVA aplicada</div>{{ v.porcentajeIva * 100 | number: '1.0-2' }} %</div>
          </div>
        </div>

        <div class="tabla-contenedor">
          <table mat-table [dataSource]="v.lineas">
            <ng-container matColumnDef="codigo">
              <th mat-header-cell *matHeaderCellDef>Código</th>
              <td mat-cell *matCellDef="let l" class="nowrap"><a [routerLink]="['/productos', l.idProducto]">{{ l.codigoProducto }}</a></td>
            </ng-container>
            <ng-container matColumnDef="producto">
              <th mat-header-cell *matHeaderCellDef>Producto</th>
              <td mat-cell *matCellDef="let l">{{ l.nombreProducto }}</td>
            </ng-container>
            <ng-container matColumnDef="cantidad">
              <th mat-header-cell *matHeaderCellDef class="num">Cantidad</th>
              <td mat-cell *matCellDef="let l" class="num">{{ l.cantidad }}</td>
            </ng-container>
            <ng-container matColumnDef="precio">
              <th mat-header-cell *matHeaderCellDef class="num">Precio unitario</th>
              <td mat-cell *matCellDef="let l" class="num">{{ l.precioUnitario | moneda }}</td>
            </ng-container>
            <ng-container matColumnDef="subtotal">
              <th mat-header-cell *matHeaderCellDef class="num">Subtotal</th>
              <td mat-cell *matCellDef="let l" class="num">{{ l.subtotal | moneda }}</td>
            </ng-container>
            <tr mat-header-row *matHeaderRowDef="columnas"></tr>
            <tr mat-row *matRowDef="let l; columns: columnas"></tr>
          </table>
          <div class="totales" style="padding: 0 16px 16px">
            <span>Subtotal</span><span class="v">{{ v.subtotal | moneda }}</span>
            <span>IVA</span><span class="v">{{ v.iva | moneda }}</span>
            <span class="total">Total</span><span class="v total">{{ v.total | moneda }}</span>
          </div>
        </div>
      } @else {
        <mat-progress-bar mode="indeterminate" />
      }
    </div>
  `,
})
export class VentaDetalle implements OnInit {
  readonly id = input.required<string>();
  private readonly api = inject(OperacionesApi);
  private readonly facturas = inject(Facturas);
  private readonly sesion = inject(Sesion);
  protected readonly puedeFacturar = this.sesion.puede('emitirFactura');
  protected readonly verClientes = this.sesion.puede('verClientes');
  protected readonly venta = signal<Venta | null>(null);
  protected readonly generando = signal(false);
  protected readonly columnas = ['codigo', 'producto', 'cantidad', 'precio', 'subtotal'];

  ngOnInit(): void {
    this.api.obtenerVenta(Number(this.id())).subscribe((v) => this.venta.set(v));
  }

  /**
   * Se abre una pestana en blanco ANTES de pedir el PDF (dentro del clic
   * del usuario) y luego se le asigna la direccion: si se abriera
   * despues de la respuesta, el navegador lo trataria como ventana
   * emergente no solicitada y la bloquearia.
   */
  protected ver(v: Venta): void {
    const pestana = window.open('', '_blank');
    this.generando.set(true);
    this.facturas.obtener(v.idVenta).subscribe({
      next: (f) => {
        this.generando.set(false);
        if (pestana) {
          pestana.location.href = f.url;
        } else {
          this.facturas.descargar(f.archivo, f.nombre);
        }
      },
      error: () => {
        this.generando.set(false);
        pestana?.close();
      },
    });
  }

  protected descargar(v: Venta): void {
    this.generando.set(true);
    this.facturas.obtener(v.idVenta).subscribe({
      next: (f) => {
        this.generando.set(false);
        this.facturas.descargar(f.archivo, f.nombre);
      },
      error: () => this.generando.set(false),
    });
  }
}
