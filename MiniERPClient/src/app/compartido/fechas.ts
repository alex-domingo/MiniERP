import { Injectable } from '@angular/core';
import { MAT_DATE_FORMATS, MatDateFormats, NativeDateAdapter, provideNativeDateAdapter } from '@angular/material/core';
import { MAT_DATE_LOCALE, DateAdapter } from '@angular/material/core';

const FORMATO = 'dd/MM/yyyy';

/**
 * Adaptador de fechas para Guatemala: muestra y ACEPTA dd/mm/aaaa.
 * El adaptador nativo usa Date.parse, que entiende "03/04/2026" como
 * 4 de marzo (formato de EE. UU.); aqui se interpreta como 3 de abril.
 */
@Injectable()
export class AdaptadorFechas extends NativeDateAdapter {
  override parse(valor: unknown, formato: object): Date | null {
    if (typeof valor === 'string') {
      const m = /^\s*(\d{1,2})[/.-](\d{1,2})[/.-](\d{4})\s*$/.exec(valor);
      if (m) {
        const [d, mes, a] = [Number(m[1]), Number(m[2]) - 1, Number(m[3])];
        const fecha = new Date(a, mes, d);
        // Rechaza 31/02/2026 y similares en lugar de "corregirlos" a marzo
        return fecha.getDate() === d && fecha.getMonth() === mes ? fecha : this.invalid();
      }
      return valor.trim() === '' ? null : this.invalid();
    }
    return super.parse(valor, formato);
  }

  override format(fecha: Date, formato: object | string): string {
    if (formato === FORMATO) {
      const dd = String(fecha.getDate()).padStart(2, '0');
      const mm = String(fecha.getMonth() + 1).padStart(2, '0');
      return `${dd}/${mm}/${fecha.getFullYear()}`;
    }
    return super.format(fecha, formato as object);
  }

  override getFirstDayOfWeek(): number {
    return 1; // lunes
  }
}

const FORMATOS: MatDateFormats = {
  parse: { dateInput: FORMATO },
  display: {
    dateInput: FORMATO,
    monthYearLabel: { year: 'numeric', month: 'short' },
    dateA11yLabel: { year: 'numeric', month: 'long', day: 'numeric' },
    monthYearA11yLabel: { year: 'numeric', month: 'long' },
  },
};

export function proveerFechas() {
  return [
    provideNativeDateAdapter(),
    { provide: DateAdapter, useClass: AdaptadorFechas },
    { provide: MAT_DATE_FORMATS, useValue: FORMATOS },
    { provide: MAT_DATE_LOCALE, useValue: 'es-GT' },
  ];
}
