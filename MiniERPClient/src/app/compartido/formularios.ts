import { AbstractControl, FormGroup } from '@angular/forms';
import { erroresDeCampo, mensajeError } from '../nucleo/utilidades';

/**
 * Coloca los errores de validacion del servidor (400 con "errores") en
 * el campo correspondiente del formulario, y devuelve el mensaje general
 * para lo que no corresponda a ningun campo (409, 404...).
 */
export function aplicarErroresServidor(formulario: FormGroup, error: unknown): string | null {
  const porCampo = erroresDeCampo(error);
  let sinCampo: string | null = null;
  for (const [campo, mensaje] of Object.entries(porCampo)) {
    const control = formulario.get(campo);
    if (control) {
      control.setErrors({ ...(control.errors ?? {}), servidor: mensaje });
      control.markAsTouched();
    } else {
      sinCampo ??= mensaje;
    }
  }
  if (Object.keys(porCampo).length === 0) {
    return mensajeError(error);
  }
  return sinCampo;
}

/**
 * Primer mensaje de error de un control, en espanol. Cubre los
 * validadores de Angular que usan los formularios y el error del servidor.
 */
export function mensajeCampo(control: AbstractControl | null): string {
  const e = control?.errors;
  if (!e) return '';
  if (e['servidor']) return e['servidor'];
  if (e['required']) return 'Este campo es obligatorio';
  if (e['minlength']) return `Debe tener al menos ${e['minlength'].requiredLength} caracteres`;
  if (e['maxlength']) return `No puede exceder ${e['maxlength'].requiredLength} caracteres`;
  if (e['min']) return `El valor mínimo es ${e['min'].min}`;
  if (e['max']) return `El valor máximo es ${e['max'].max}`;
  if (e['email']) return 'El correo no tiene un formato válido';
  if (e['pattern']) return 'El formato no es válido';
  if (e['matDatepickerParse']) return 'Use el formato dd/mm/aaaa';
  if (e['decimales']) return `Admite hasta ${e['decimales']} decimales`;
  return 'Valor no válido';
}

/** Mismo patron de NIT que el servidor (Texto.PATRON_NIT): 1234567-8, 12345678 o CF. */
export const PATRON_NIT = /^\s*(?:[Cc][Ff]|[0-9]{1,15}\s*-?\s*[0-9Kk])\s*$/;

/** Validador de cantidad maxima de decimales (el servidor admite 2 en precios y costos). */
export function maxDecimales(n: number) {
  return (control: AbstractControl) => {
    const v = control.value;
    if (v === null || v === undefined || v === '') return null;
    const partes = String(v).split('.');
    return partes.length > 1 && partes[1].length > n ? { decimales: n } : null;
  };
}

/** Convierte cadenas vacias en null para no mandar "" al servidor. */
export function limpiar<T extends Record<string, unknown>>(valores: T): T {
  const salida: Record<string, unknown> = {};
  for (const [k, v] of Object.entries(valores)) {
    salida[k] = typeof v === 'string' ? (v.trim() === '' ? null : v.trim()) : v;
  }
  return salida as T;
}
