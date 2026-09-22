import { PERMISOS, permite } from './permisos';

describe('permisos (espejo de ConfiguracionSeguridad)', () => {
  it('solo el area duena escribe', () => {
    expect(permite('VENTAS', 'registrarVenta')).toBe(true);
    expect(permite('ADMINISTRACION', 'registrarVenta')).toBe(false);
    expect(permite('COMPRAS', 'registrarCompra')).toBe(true);
    expect(permite('INVENTARIO', 'ajustarInventario')).toBe(true);
    expect(permite('ADMINISTRACION', 'ajustarInventario')).toBe(false);
  });

  it('Ventas no ve el kardex (expone costos)', () => {
    expect(permite('VENTAS', 'verInventario')).toBe(false);
  });

  it('Administracion ve los cuatro grupos de reportes', () => {
    for (const p of ['reportesInventario', 'reportesCompras', 'reportesVentas', 'reporteBitacora'] as const) {
      expect(permite('ADMINISTRACION', p)).toBe(true);
    }
    expect(PERMISOS.reporteBitacora).toEqual(['ADMINISTRACION']);
  });

  it('sin rol no hay permisos', () => {
    expect(permite(null, 'verVentas')).toBe(false);
  });
});
