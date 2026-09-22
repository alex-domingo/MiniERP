import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { RouterLink } from '@angular/router';
import { catchError, of } from 'rxjs';
import { Existencia } from '../../compartido/existencia';
import { MonedaPipe } from '../../compartido/formato';
import { CatalogoApi, InventarioApi, OperacionesApi, SistemaApi } from '../../nucleo/api';
import { Pagina } from '../../nucleo/modelos';
import { Sesion } from '../../nucleo/sesion';
import { etiqueta, fechaIso, inicioDeMes } from '../../nucleo/utilidades';

interface Acceso { texto: string; icono: string; ruta: string; params?: Record<string, unknown> }

/**
 * Tablero de inicio: lo que cada area necesita ver al entrar. Usa los
 * listados normales (no los reportes) para no llenar la bitacora con
 * una "consulta de reporte" cada vez que alguien abre la aplicacion.
 */
@Component({
  selector: 'app-inicio',
  imports: [RouterLink, DatePipe, DecimalPipe, MatButtonModule, MatIconModule, MatProgressBarModule, MonedaPipe, Existencia],
  templateUrl: './inicio.html',
})
export class Inicio {
  protected readonly sesion = inject(Sesion);
  private readonly catalogo = inject(CatalogoApi);
  private readonly operaciones = inject(OperacionesApi);

  protected readonly hoy = new Date();
  protected readonly area = etiqueta(this.sesion.rol());
  protected readonly saludo = this.hoy.getHours() < 12 ? 'Buenos días' : this.hoy.getHours() < 19 ? 'Buenas tardes' : 'Buenas noches';
  protected readonly nombre = (this.sesion.usuario()?.nombreCompleto ?? '').split(' ')[0];

  protected readonly verCompras = this.sesion.puede('verCompras');
  protected readonly verVentas = this.sesion.puede('verVentas');
  protected readonly verIntegridad = this.sesion.puede('verInventario') && this.sesion.rol() !== 'COMPRAS';

  private readonly mes = { desde: fechaIso(inicioDeMes()), hasta: fechaIso(this.hoy) };

  protected readonly salud = toSignal(inject(SistemaApi).salud$.pipe(catchError(() => of(null))));
  protected readonly alertas = toSignal(this.catalogo.alertasDeExistencia().pipe(catchError(() => of([]))));
  protected readonly ventas = toSignal(
    this.verVentas
      ? this.operaciones.listarVentas({ ...this.mes, tamano: 5 }).pipe(catchError(() => of(null)))
      : of(null),
  );
  protected readonly compras = toSignal(
    this.verCompras
      ? this.operaciones.listarCompras({ ...this.mes, tamano: 5 }).pipe(catchError(() => of(null)))
      : of(null),
  );
  protected readonly integridad = toSignal(
    this.verIntegridad ? inject(InventarioApi).verificar().pipe(catchError(() => of(null))) : of(null),
  );

  protected readonly accesos = computed<Acceso[]>(() => {
    const a: Acceso[] = [];
    const s = this.sesion;
    if (s.puede('registrarVenta')) a.push({ texto: 'Nueva venta', icono: 'point_of_sale', ruta: '/ventas/nueva' });
    if (s.puede('registrarCompra')) a.push({ texto: 'Nueva compra', icono: 'add_shopping_cart', ruta: '/compras/nueva' });
    if (s.puede('ajustarInventario')) a.push({ texto: 'Ajustar inventario', icono: 'tune', ruta: '/inventario/ajustes' });
    if (s.puede('editarClientes')) a.push({ texto: 'Clientes', icono: 'groups', ruta: '/clientes' });
    if (s.puede('editarProveedores')) a.push({ texto: 'Proveedores', icono: 'local_shipping', ruta: '/proveedores' });
    if (s.puede('verInventario')) a.push({ texto: 'Kardex', icono: 'receipt_long', ruta: '/inventario/kardex' });
    a.push({ texto: 'Reportes', icono: 'bar_chart', ruta: '/reportes' });
    if (s.puede('reporteBitacora')) a.push({ texto: 'Bitácora', icono: 'history', ruta: '/reportes/bitacora' });
    return a;
  });

  protected ultimas<T>(p: Pagina<T> | null | undefined): T[] {
    return p?.contenido ?? [];
  }
}

