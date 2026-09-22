import { HttpErrorResponse } from '@angular/common/http';
import { aCsv, erroresDeCampo, etiqueta, fechaIso, mensajeError, parametros } from './utilidades';

describe('utilidades', () => {
  it('fechaIso usa la fecha local, no UTC', () => {
    // 31 de diciembre a las 23:30: en UTC ya seria 1 de enero
    expect(fechaIso(new Date(2026, 11, 31, 23, 30))).toBe('2026-12-31');
    expect(fechaIso(new Date(2026, 0, 5))).toBe('2026-01-05');
    expect(fechaIso(null)).toBeUndefined();
  });

  it('parametros omite los valores vacios y formatea fechas', () => {
    const p = parametros({ a: 1, b: null, c: undefined, d: '', e: false, f: new Date(2026, 2, 9) });
    expect(p.keys().sort()).toEqual(['a', 'e', 'f']);
    expect(p.get('e')).toBe('false');
    expect(p.get('f')).toBe('2026-03-09');
  });

  it('mensajeError toma el detail del ProblemDetail', () => {
    const e = new HttpErrorResponse({ status: 409, error: { detail: 'No hay existencia suficiente' } });
    expect(mensajeError(e)).toBe('No hay existencia suficiente');
  });

  it('mensajeError prioriza el primer error de validacion', () => {
    const e = new HttpErrorResponse({ status: 400, error: { detail: 'Datos invalidos', errores: { nit: 'Formato' } } });
    expect(mensajeError(e)).toBe('Formato');
    expect(erroresDeCampo(e)).toEqual({ nit: 'Formato' });
  });

  it('mensajeError explica un servidor apagado', () => {
    expect(mensajeError(new HttpErrorResponse({ status: 0 }))).toContain('No se pudo conectar');
  });

  it('aCsv separa con punto y coma, escapa comillas y agrega BOM', async () => {
    // jsdom no implementa Blob.text(): se leen los bytes con FileReader
    const bytes = await new Promise<Uint8Array>((ok) => {
      const lector = new FileReader();
      lector.onload = () => ok(new Uint8Array(lector.result as ArrayBuffer));
      lector.readAsArrayBuffer(aCsv(['A', 'B'], [['x;y', 'di "hola"']]));
    });
    expect(Array.from(bytes.slice(0, 3))).toEqual([0xef, 0xbb, 0xbf]);
    expect(new TextDecoder().decode(bytes.slice(3))).toBe('A;B\r\n"x;y";"di ""hola"""');
  });

  it('etiqueta traduce enumeraciones y deja pasar lo desconocido', () => {
    expect(etiqueta('AJUSTE_SALIDA')).toBe('Ajuste de salida');
    expect(etiqueta('ANIO')).toBe('Año');
    expect(etiqueta('OTRO')).toBe('OTRO');
  });
});
