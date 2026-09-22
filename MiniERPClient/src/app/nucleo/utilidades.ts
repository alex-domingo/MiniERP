import { Observable, catchError, of } from 'rxjs';
import { HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Problema } from './modelos';

/** Prefijo de la API. En desarrollo, proxy.conf.json lo redirige a http://localhost:8080. */
export const API = '/api';

/** Date -> "AAAA-MM-DD" con la fecha LOCAL (toISOString usaria UTC y podria correr el dia). */
export function fechaIso(fecha: Date | null | undefined): string | undefined {
  if (!fecha) {
    return undefined;
  }
  const mm = String(fecha.getMonth() + 1).padStart(2, '0');
  const dd = String(fecha.getDate()).padStart(2, '0');
  return `${fecha.getFullYear()}-${mm}-${dd}`;
}

/** Primer dia del mes de la fecha dada. */
export function inicioDeMes(fecha = new Date()): Date {
  return new Date(fecha.getFullYear(), fecha.getMonth(), 1);
}

/** Construye HttpParams omitiendo los valores vacios (null, undefined, ''). */
export function parametros(valores: Record<string, unknown>): HttpParams {
  let p = new HttpParams();
  for (const [clave, valor] of Object.entries(valores)) {
    if (valor === null || valor === undefined || valor === '') {
      continue;
    }
    p = p.set(clave, valor instanceof Date ? fechaIso(valor)! : String(valor));
  }
  return p;
}

/** Texto legible para cualquier error HTTP, usando el "detail" del ProblemDetail del servidor. */
export function mensajeError(error: unknown): string {
  if (!(error instanceof HttpErrorResponse)) {
    return 'Ocurrió un error inesperado';
  }
  if (error.status === 0) {
    return 'No se pudo conectar con el servidor. Verifique que esté en ejecución.';
  }
  const problema = error.error as Problema | null;
  if (problema?.errores) {
    const primero = Object.values(problema.errores)[0];
    if (primero) {
      return primero;
    }
  }
  if (problema?.detail) {
    return problema.detail;
  }
  switch (error.status) {
    case 401: return 'Su sesión no es válida. Inicie sesión de nuevo.';
    case 403: return 'No tiene permiso para realizar esta acción.';
    case 404: return 'El recurso solicitado no existe.';
    default: return 'El servidor no pudo completar la operación.';
  }
}

/** Errores de validacion por campo que devuelve el servidor (400). */
export function erroresDeCampo(error: unknown): Record<string, string> {
  if (error instanceof HttpErrorResponse && error.status === 400) {
    return (error.error as Problema | null)?.errores ?? {};
  }
  return {};
}

/** Descarga un Blob como archivo. */
export function descargar(blob: Blob, nombre: string): void {
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = nombre;
  a.click();
  setTimeout(() => URL.revokeObjectURL(url), 10_000);
}

/**
 * CSV compatible con Excel en espanol: separador ";" (la coma es el
 * separador de listas en Windows con configuracion regional de
 * Latinoamerica) y BOM UTF-8 para que respete los acentos.
 */
export function aCsv(encabezados: string[], filas: unknown[][]): Blob {
  const escapar = (v: unknown) => {
    const t = v === null || v === undefined ? '' : String(v);
    return /[";\n\r]/.test(t) ? `"${t.replace(/"/g, '""')}"` : t;
  };
  const lineas = [encabezados, ...filas].map((f) => f.map(escapar).join(';'));
  return new Blob(['﻿' + lineas.join('\r\n')], { type: 'text/csv;charset=utf-8' });
}

/** Etiquetas legibles de los valores de enumeracion del servidor. */
export const ETIQUETAS: Record<string, string> = {
  ADMINISTRACION: 'Administración',
  COMPRAS: 'Compras',
  INVENTARIO: 'Inventario',
  VENTAS: 'Ventas',
  ENTRADA: 'Entrada',
  SALIDA: 'Salida',
  COMPRA: 'Compra',
  VENTA: 'Venta',
  AJUSTE_ENTRADA: 'Ajuste de entrada',
  AJUSTE_SALIDA: 'Ajuste de salida',
  SIN_EXISTENCIA: 'Sin existencia',
  CRITICO: 'Crítico',
  EN_MINIMO: 'En mínimo',
  NORMAL: 'Normal',
  AUTENTICACION: 'Autenticación',
  USUARIOS: 'Usuarios',
  CATEGORIAS: 'Categorías',
  PRODUCTOS: 'Productos',
  PROVEEDORES: 'Proveedores',
  CLIENTES: 'Clientes',
  REPORTES: 'Reportes',
  LOGIN: 'Inicio de sesión',
  LOGOUT: 'Cierre de sesión',
  LOGIN_FALLIDO: 'Inicio de sesión fallido',
  CREAR: 'Crear',
  ACTUALIZAR: 'Actualizar',
  DESACTIVAR: 'Desactivar',
  ACTIVAR: 'Activar',
  CONSULTAR: 'Consultar',
  EXPORTAR: 'Exportar',
  ANULAR: 'Anular',
  DIA: 'Día',
  SEMANA: 'Semana',
  MES: 'Mes',
  TRIMESTRE: 'Trimestre',
  ANIO: 'Año',
};

export function etiqueta(valor: unknown): string {
  const t = String(valor ?? '');
  return ETIQUETAS[t] ?? t;
}

/**
 * Para las cargas que alimentan un toSignal: si el servidor falla, la
 * senal toma el valor alternativo en lugar de lanzar la excepcion al
 * leerse (lo que romperia el dibujado de toda la pantalla). El aviso al
 * usuario ya lo muestra el interceptor de errores.
 */
export function seguro<T>(fuente: Observable<T>, alternativo: T): Observable<T> {
  return fuente.pipe(catchError(() => of(alternativo)));
}
