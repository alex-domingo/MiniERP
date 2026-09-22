import { Injectable, inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';

/** Avisos breves al pie de la pantalla. */
@Injectable({ providedIn: 'root' })
export class Notificacion {
  private readonly snack = inject(MatSnackBar);

  exito(mensaje: string): void {
    this.snack.open(mensaje, 'Cerrar', { duration: 4000, panelClass: 'aviso-exito' });
  }

  error(mensaje: string): void {
    this.snack.open(mensaje, 'Cerrar', { duration: 7000, panelClass: 'aviso-error' });
  }
}
