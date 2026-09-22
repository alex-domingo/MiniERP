import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { Notificacion } from './notificacion';
import { Sesion } from './sesion';
import { SIN_AVISO } from './contexto';
import { API, mensajeError } from './utilidades';

/** Agrega "Authorization: Bearer <token>" a las llamadas a la API. */
export const autenticacionInterceptor: HttpInterceptorFn = (req, next) => {
  const token = inject(Sesion).token();
  if (token && req.url.startsWith(API) && !req.url.endsWith('/auth/login')) {
    req = req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
  }
  return next(req);
};

/**
 * Manejo central de errores:
 *   401 -> la sesion ya no es valida: se cierra y se vuelve al login.
 *   resto -> aviso con el "detail" del ProblemDetail del servidor,
 *            salvo que la peticion lleve SIN_AVISO.
 * El error se propaga igual, para que el componente pueda reaccionar.
 */
export const erroresInterceptor: HttpInterceptorFn = (req, next) => {
  const sesion = inject(Sesion);
  const aviso = inject(Notificacion);
  return next(req).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse) {
        if (error.status === 401 && !req.url.endsWith('/auth/login')) {
          sesion.expirada();
        } else if (!req.context.get(SIN_AVISO)) {
          aviso.error(mensajeError(error));
        }
      }
      return throwError(() => error);
    }),
  );
};
