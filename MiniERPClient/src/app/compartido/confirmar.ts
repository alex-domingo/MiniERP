import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialog, MatDialogModule } from '@angular/material/dialog';
import { Observable } from 'rxjs';

export interface DatosConfirmacion {
  titulo: string;
  mensaje: string;
  aceptar?: string;
  peligro?: boolean;
}

@Component({
  selector: 'app-confirmar',
  imports: [MatDialogModule, MatButtonModule],
  template: `
    <h2 mat-dialog-title>{{ datos.titulo }}</h2>
    <mat-dialog-content><p>{{ datos.mensaje }}</p></mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cancelar</button>
      <button mat-flat-button [mat-dialog-close]="true" [class.boton-peligro]="datos.peligro">
        {{ datos.aceptar ?? 'Aceptar' }}
      </button>
    </mat-dialog-actions>
  `,
})
export class Confirmar {
  protected readonly datos = inject<DatosConfirmacion>(MAT_DIALOG_DATA);
}

/** Abre la confirmacion y emite true solo si el usuario acepta. */
export function confirmar(dialogo: MatDialog, datos: DatosConfirmacion): Observable<boolean | undefined> {
  return dialogo.open(Confirmar, { data: datos, width: '420px' }).afterClosed();
}
