import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { OperacionesApi } from '../../nucleo/api';
import { descargar } from '../../nucleo/utilidades';

/** Pide la factura PDF al servidor y la abre o descarga en el navegador. */
@Injectable({ providedIn: 'root' })
export class Facturas {
  private readonly api = inject(OperacionesApi);

  obtener(idVenta: number): Observable<{ url: string; archivo: Blob; nombre: string }> {
    return this.api.factura(idVenta).pipe(map((f) => ({ ...f, url: URL.createObjectURL(f.archivo) })));
  }

  abrir(url: string): void {
    window.open(url, '_blank', 'noopener');
  }

  descargar(archivo: Blob, nombre: string): void {
    descargar(archivo, nombre);
  }
}
