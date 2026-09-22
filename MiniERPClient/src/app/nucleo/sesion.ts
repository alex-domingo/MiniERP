import { HttpClient, HttpContext } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { RespuestaLogin, Rol } from './modelos';
import { Permiso, permite } from './permisos';
import { SIN_AVISO } from './contexto';
import { API } from './utilidades';

const CLAVE = 'minierp.sesion';

/**
 * Sesion del usuario autenticado.
 *
 * El token JWT se guarda en localStorage para sobrevivir a un refresco
 * de pagina. La expiracion se toma del propio token (claim "exp"), no
 * del reloj del usuario frente a un texto: al vencer, la sesion se
 * cierra sola y se lleva al usuario al inicio de sesion.
 */
@Injectable({ providedIn: 'root' })
export class Sesion {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly datos = signal<RespuestaLogin | null>(null);
  private temporizador: ReturnType<typeof setTimeout> | undefined;

  readonly usuario = this.datos.asReadonly();
  readonly autenticado = computed(() => this.datos() !== null);
  readonly rol = computed<Rol | null>(() => this.datos()?.rol ?? null);

  constructor() {
    const guardada = this.leer();
    if (guardada && this.vigente(guardada.token)) {
      this.establecer(guardada);
    } else {
      localStorage.removeItem(CLAVE);
    }
  }

  iniciar(usuario: string, contrasena: string): Observable<RespuestaLogin> {
    return this.http
      .post<RespuestaLogin>(`${API}/auth/login`, { usuario, contrasena },
        { context: new HttpContext().set(SIN_AVISO, true) })
      .pipe(tap((r) => {
        localStorage.setItem(CLAVE, JSON.stringify(r));
        this.establecer(r);
      }));
  }

  /** Cierra la sesion; si el token sigue vigente, avisa al servidor para la bitacora. */
  cerrar(motivo?: 'expirada'): void {
    const actual = this.datos();
    if (actual && !motivo) {
      this.http.post(`${API}/auth/logout`, null, { context: new HttpContext().set(SIN_AVISO, true) })
        .subscribe({ error: () => undefined });
    }
    this.limpiar();
    this.router.navigate(['/login'], motivo ? { queryParams: { motivo } } : {});
  }

  /** Llamado por el interceptor cuando el servidor rechaza el token (401). */
  expirada(): void {
    if (this.datos()) {
      this.cerrar('expirada');
    }
  }

  token(): string | null {
    return this.datos()?.token ?? null;
  }

  puede(permiso: Permiso): boolean {
    return permite(this.rol(), permiso);
  }

  // -------------------------------------------------------------------

  private establecer(r: RespuestaLogin): void {
    this.datos.set(r);
    clearTimeout(this.temporizador);
    const restante = this.expiracion(r.token) - Date.now();
    // setTimeout admite hasta ~24.8 dias; el token dura 8 horas.
    this.temporizador = setTimeout(() => this.cerrar('expirada'), Math.max(0, Math.min(restante, 2 ** 31 - 1)));
  }

  private limpiar(): void {
    clearTimeout(this.temporizador);
    localStorage.removeItem(CLAVE);
    this.datos.set(null);
  }

  private leer(): RespuestaLogin | null {
    try {
      const texto = localStorage.getItem(CLAVE);
      return texto ? (JSON.parse(texto) as RespuestaLogin) : null;
    } catch {
      return null;
    }
  }

  private vigente(token: string): boolean {
    return this.expiracion(token) > Date.now() + 5_000;
  }

  /** Milisegundos epoch del claim "exp" del JWT (0 si el token es ilegible). */
  private expiracion(token: string): number {
    try {
      const carga = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
      const json = JSON.parse(atob(carga.padEnd(carga.length + ((4 - (carga.length % 4)) % 4), '=')));
      return typeof json.exp === 'number' ? json.exp * 1000 : 0;
    } catch {
      return 0;
    }
  }
}
