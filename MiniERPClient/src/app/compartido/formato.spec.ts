import { registerLocaleData } from '@angular/common';
import localeEsGt from '@angular/common/locales/es-GT';
import { entero, moneda } from './formato';

describe('formato', () => {
  beforeAll(() => registerLocaleData(localeEsGt));

  it('moneda con el formato de la factura', () => {
    expect(moneda(3865.12)).toBe('Q 3,865.12');
    expect(moneda(1234567.5)).toBe('Q 1,234,567.50');
    expect(moneda(391.17567, 4)).toBe('Q 391.1757');
    expect(moneda(null)).toBe('');
  });

  it('entero con separador de miles', () => {
    expect(entero(12500)).toBe('12,500');
  });
});
