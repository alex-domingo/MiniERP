import { AdaptadorFechas } from './fechas';
import { TestBed } from '@angular/core/testing';
import { MAT_DATE_LOCALE } from '@angular/material/core';

describe('AdaptadorFechas (dd/mm/aaaa)', () => {
  let adaptador: AdaptadorFechas;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [AdaptadorFechas, { provide: MAT_DATE_LOCALE, useValue: 'es-GT' }],
    });
    adaptador = TestBed.inject(AdaptadorFechas);
  });

  it('interpreta dia/mes/anio, no mes/dia', () => {
    const f = adaptador.parse('03/04/2026', {})!;
    expect([f.getDate(), f.getMonth(), f.getFullYear()]).toEqual([3, 3, 2026]);
  });

  it('acepta guiones y puntos', () => {
    expect(adaptador.parse('15-07-2026', {})!.getMonth()).toBe(6);
    expect(adaptador.parse('1.2.2026', {})!.getDate()).toBe(1);
  });

  it('rechaza fechas imposibles en vez de corregirlas', () => {
    expect(adaptador.isValid(adaptador.parse('31/02/2026', {})!)).toBe(false);
    expect(adaptador.isValid(adaptador.parse('texto', {})!)).toBe(false);
  });

  it('vacio es null', () => {
    expect(adaptador.parse('', {})).toBeNull();
  });

  it('muestra dd/mm/aaaa', () => {
    expect(adaptador.format(new Date(2026, 0, 5), 'dd/MM/yyyy')).toBe('05/01/2026');
  });
});
