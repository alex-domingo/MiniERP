import { formatNumber } from '@angular/common';
import { Pipe, PipeTransform } from '@angular/core';

/** Q 1,234.50 - el mismo formato de la factura PDF. */
export function moneda(valor: number | null | undefined, decimales = 2): string {
  if (valor === null || valor === undefined || Number.isNaN(valor)) {
    return '';
  }
  return 'Q ' + formatNumber(valor, 'es-GT', `1.${decimales}-${decimales}`);
}

export function entero(valor: number | null | undefined): string {
  return valor === null || valor === undefined ? '' : formatNumber(valor, 'es-GT', '1.0-0');
}

@Pipe({ name: 'moneda' })
export class MonedaPipe implements PipeTransform {
  transform(valor: number | null | undefined, decimales = 2): string {
    return moneda(valor, decimales);
  }
}

@Pipe({ name: 'entero' })
export class EnteroPipe implements PipeTransform {
  transform(valor: number | null | undefined): string {
    return entero(valor);
  }
}
