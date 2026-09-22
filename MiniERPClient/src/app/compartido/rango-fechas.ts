import { Component, effect, input, output } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { debounceTime, filter } from 'rxjs';

export interface Rango {
  desde: Date | null;
  hasta: Date | null;
}

/**
 * Rango de fechas (ambos extremos incluidos, igual que en el servidor).
 * Emite solo cuando el rango queda completo o vacio: elegir la primera
 * fecha no dispara una consulta a medias.
 */
@Component({
  selector: 'app-rango-fechas',
  imports: [ReactiveFormsModule, MatFormFieldModule, MatDatepickerModule, MatButtonModule, MatIconModule],
  template: `
    <mat-form-field [style.width.px]="260">
      <mat-label>{{ etiqueta() }}</mat-label>
      <mat-date-range-input [formGroup]="rango" [rangePicker]="selector">
        <input matStartDate formControlName="desde" placeholder="Desde">
        <input matEndDate formControlName="hasta" placeholder="Hasta">
      </mat-date-range-input>
      <mat-datepicker-toggle matIconSuffix [for]="selector" />
      @if (rango.value.desde || rango.value.hasta) {
        <button mat-icon-button matSuffix (click)="limpiar()" aria-label="Quitar fechas" type="button">
          <mat-icon>close</mat-icon>
        </button>
      }
      <mat-date-range-picker #selector />
      @if (rango.controls.desde.hasError('matDatepickerParse') || rango.controls.hasta.hasError('matDatepickerParse')) {
        <mat-error>Use el formato dd/mm/aaaa</mat-error>
      }
    </mat-form-field>
  `,
})
export class RangoFechas {
  readonly etiqueta = input('Periodo');
  readonly inicial = input<Rango>({ desde: null, hasta: null });
  readonly cambio = output<Rango>();

  protected readonly rango = new FormGroup({
    desde: new FormControl<Date | null>(null),
    hasta: new FormControl<Date | null>(null),
  });

  constructor() {
    effect(() => this.rango.setValue(this.inicial(), { emitEvent: false }));
    this.rango.valueChanges
      .pipe(
        debounceTime(50),
        filter((v) => this.rango.valid && (!!v.desde === !!v.hasta)),
        takeUntilDestroyed(),
      )
      .subscribe((v) => this.cambio.emit({ desde: v.desde ?? null, hasta: v.hasta ?? null }));
  }

  protected limpiar(): void {
    this.rango.setValue({ desde: null, hasta: null });
  }
}
