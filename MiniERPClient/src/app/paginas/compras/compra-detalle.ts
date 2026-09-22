import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, input, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTableModule } from '@angular/material/table';
import { RouterLink } from '@angular/router';
import { MonedaPipe } from '../../compartido/formato';
import { OperacionesApi } from '../../nucleo/api';
import { Compra } from '../../nucleo/modelos';
import { Sesion } from '../../nucleo/sesion';

@Component({
  selector: 'app-compra-detalle',
  imports: [RouterLink, DatePipe, MatButtonModule, MatIconModule, MatTableModule, MatProgressBarModule, MonedaPipe],
  template: `
    <div class="pagina">
      @if (compra(); as c) {
        <div class="encabezado">
          <button mat-icon-button routerLink="/compras" aria-label="Volver a compras" class="no-imprimir"><mat-icon>arrow_back</mat-icon></button>
          <div>
            <h1>Compra {{ c.numeroDocumento }}</h1>
            <p class="subtitulo">{{ c.fechaCompra | date: "dd/MM/yyyy 'a las' HH:mm" }} · registró {{ c.usuario }}</p>
          </div>
          <span class="espacio"></span>
          <button mat-stroked-button (click)="imprimir()" class="no-imprimir"><mat-icon>print</mat-icon>Imprimir</button>
        </div>

        <div class="tarjeta" style="margin-bottom:16px">
          <div class="rejilla">
            <div><div class="muted">Proveedor</div>
              @if (verProveedores) { <a [routerLink]="['/proveedores', c.idProveedor]">{{ c.nombreProveedor }}</a> } @else { {{ c.nombreProveedor }} }
            </div>
            <div><div class="muted">NIT</div>{{ c.nitProveedor }}</div>
            <div><div class="muted">Observaciones</div>{{ c.observaciones || '—' }}</div>
          </div>
        </div>

        <div class="tabla-contenedor">
          <table mat-table [dataSource]="c.lineas">
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
            <ng-container matColumnDef="costo">
              <th mat-header-cell *matHeaderCellDef class="num">Costo unitario</th>
              <td mat-cell *matCellDef="let l" class="num">{{ l.costoUnitario | moneda }}</td>
            </ng-container>
            <ng-container matColumnDef="subtotal">
              <th mat-header-cell *matHeaderCellDef class="num">Subtotal</th>
              <td mat-cell *matCellDef="let l" class="num">{{ l.subtotal | moneda }}</td>
            </ng-container>
            <tr mat-header-row *matHeaderRowDef="columnas"></tr>
            <tr mat-row *matRowDef="let l; columns: columnas"></tr>
          </table>
          <div class="totales" style="padding: 0 16px 16px">
            <span class="total">Total</span><span class="v total">{{ c.total | moneda }}</span>
          </div>
        </div>
      } @else {
        <mat-progress-bar mode="indeterminate" />
      }
    </div>
  `,
})
export class CompraDetalle implements OnInit {
  readonly id = input.required<string>();
  private readonly api = inject(OperacionesApi);
  protected readonly verProveedores = inject(Sesion).puede('verProveedores');
  protected readonly compra = signal<Compra | null>(null);
  protected readonly columnas = ['codigo', 'producto', 'cantidad', 'costo', 'subtotal'];

  ngOnInit(): void {
    this.api.obtenerCompra(Number(this.id())).subscribe((c) => this.compra.set(c));
  }

  protected imprimir(): void {
    window.print();
  }
}
