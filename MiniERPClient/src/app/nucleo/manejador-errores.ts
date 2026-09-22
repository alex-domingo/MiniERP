import { HttpErrorResponse } from '@angular/common/http';
import { ErrorHandler, Injectable } from '@angular/core';

/**
 * Los errores HTTP ya los atendio el interceptor (aviso al usuario o
 * cierre de sesion): no se repiten en la consola como "ERROR". Cualquier
 * otro error si se registra, porque es un defecto del cliente.
 */
@Injectable()
export class ManejadorErrores implements ErrorHandler {
  handleError(error: unknown): void {
    const causa = (error as { rejection?: unknown })?.rejection ?? error;
    if (causa instanceof HttpErrorResponse) {
      return;
    }
    console.error(error);
  }
}
