import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Permiso } from './permisos';
import { Sesion } from './sesion';
import { Notificacion } from './notificacion';

/** Solo usuarios autenticados; si no, al login recordando a donde iba. */
export const autenticado: CanActivateFn = (_ruta, estado) => {
  const sesion = inject(Sesion);
  return sesion.autenticado()
    ? true
    : inject(Router).createUrlTree(['/login'], { queryParams: { volver: estado.url } });
};

/** El login no tiene sentido con una sesion abierta. */
export const invitado: CanActivateFn = () =>
  inject(Sesion).autenticado() ? inject(Router).createUrlTree(['/inicio']) : true;

/** Requiere el permiso indicado en data.permiso de la ruta. */
export const conPermiso: CanActivateFn = (ruta) => {
  const permiso = ruta.data['permiso'] as Permiso | undefined;
  if (!permiso || inject(Sesion).puede(permiso)) {
    return true;
  }
  inject(Notificacion).error('Su área no tiene acceso a esa sección.');
  return inject(Router).createUrlTree(['/inicio']);
};
