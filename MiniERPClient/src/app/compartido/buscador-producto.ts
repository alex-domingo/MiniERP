import { Component, inject, input, output } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatAutocompleteModule, MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { catchError, debounceTime, distinctUntilChanged, filter, of, startWith, switchMap } from 'rxjs';
import { CatalogoApi } from '../nucleo/api';
import { Producto } from '../nucleo/modelos';
import { MonedaPipe } from './formato';

/**
 * Busca productos en el servidor mientras se escribe (codigo o nombre)
 * y emite el que se elige. Muestra existencia y precio para decidir sin
 * salir de la pantalla.
 */
@Component({
  selector: 'app-buscador-producto',
  imports: [ReactiveFormsModule, MatAutocompleteModule, MatFormFieldModule, MatInputModule, MatIconModule, MonedaPipe],
  template: `
    <mat-form-field [class]="clase()">
      <mat-label>{{ etiqueta() }}</mat-label>
      <input matInput [formControl]="texto" [matAutocomplete]="auto" placeholder="Código o nombre">
      <mat-icon matSuffix>search</mat-icon>
      <mat-autocomplete #auto="matAutocomplete" [displayWith]="mostrar" (optionSelected)="elegir($event)">
        @for (p of opciones(); track p.idProducto) {
          <mat-option [value]="p" [disabled]="deshabilitar()(p)">
            <strong>{{ p.codigo }}</strong> · {{ p.nombre }}
            <span class="muted"> — exist. {{ p.stockActual }} · {{ p.precioVenta | moneda }}</span>
          </mat-option>
        } @empty {
          @if (texto.value && typeof texto.value === 'string') {
            <mat-option disabled>Sin coincidencias</mat-option>
          }
        }
      </mat-autocomplete>
    </mat-form-field>
  `,
})
export class BuscadorProducto {
  private readonly api = inject(CatalogoApi);

  readonly etiqueta = input('Producto');
  readonly clase = input('');
  readonly soloActivos = input(true);
  /** Para marcar opciones no elegibles (p. ej. sin existencia o ya agregadas). */
  readonly deshabilitar = input<(p: Producto) => boolean>(() => false);
  /** Si es true, el campo se vacia despues de elegir (agregar lineas). */
  readonly limpiarAlElegir = input(false);
  readonly elegido = output<Producto>();

  protected readonly texto = new FormControl<string | Producto>('', { nonNullable: true });
  protected readonly opciones = toSignal(
    this.texto.valueChanges.pipe(
      startWith(''),
      filter((v): v is string => typeof v === 'string'),
      debounceTime(250),
      distinctUntilChanged(),
      switchMap((t) => this.api.buscarProductos(t.trim(), this.soloActivos()).pipe(catchError(() => of([])))),
    ),
    { initialValue: [] as Producto[] },
  );

  protected mostrar = (p: Producto | string | null): string =>
    p && typeof p === 'object' ? `${p.codigo} · ${p.nombre}` : (p ?? '');

  protected elegir(evento: MatAutocompleteSelectedEvent): void {
    this.elegido.emit(evento.option.value as Producto);
    if (this.limpiarAlElegir()) {
      this.texto.setValue('');
    }
  }

  /** Permite al padre fijar el producto mostrado (p. ej. al llegar con ?producto=). */
  fijar(p: Producto): void {
    this.texto.setValue(p, { emitEvent: false });
  }
}
