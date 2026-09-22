import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { CatalogoApi } from '../../nucleo/api';
import { Categoria } from '../../nucleo/modelos';
import { aplicarErroresServidor, limpiar, mensajeCampo } from '../../compartido/formularios';

@Component({
  selector: 'app-categoria-dialogo',
  imports: [ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatIconModule],
  template: `
    <h2 mat-dialog-title>{{ datos ? 'Editar categoría' : 'Nueva categoría' }}</h2>
    <form [formGroup]="formulario" (ngSubmit)="guardar()" novalidate>
      <mat-dialog-content>
        <mat-form-field>
          <mat-label>Nombre</mat-label>
          <input matInput formControlName="nombre" maxlength="60" cdkFocusInitial>
          <mat-error>{{ error('nombre') }}</mat-error>
        </mat-form-field>
        <mat-form-field>
          <mat-label>Descripción</mat-label>
          <textarea matInput formControlName="descripcion" rows="3" maxlength="200"></textarea>
          <mat-error>{{ error('descripcion') }}</mat-error>
        </mat-form-field>
        @if (mensaje()) { <p class="mensaje-error" role="alert"><mat-icon>error</mat-icon>{{ mensaje() }}</p> }
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" mat-dialog-close>Cancelar</button>
        <button mat-flat-button type="submit" [disabled]="guardando()">Guardar</button>
      </mat-dialog-actions>
    </form>
  `,
})
export class CategoriaDialogo {
  protected readonly datos = inject<Categoria | null>(MAT_DIALOG_DATA);
  private readonly api = inject(CatalogoApi);
  private readonly ref = inject(MatDialogRef<CategoriaDialogo, Categoria>);

  protected readonly formulario = inject(FormBuilder).nonNullable.group({
    nombre: [this.datos?.nombre ?? '', [Validators.required, Validators.maxLength(60)]],
    descripcion: [this.datos?.descripcion ?? '', [Validators.maxLength(200)]],
  });
  protected readonly guardando = signal(false);
  protected readonly mensaje = signal<string | null>(null);

  protected error(campo: string): string {
    return mensajeCampo(this.formulario.get(campo));
  }

  protected guardar(): void {
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }
    this.guardando.set(true);
    this.mensaje.set(null);
    const valores = limpiar(this.formulario.getRawValue());
    const peticion = this.datos
      ? this.api.categorias.actualizar(this.datos.idCategoria, valores)
      : this.api.categorias.crear(valores);
    peticion.subscribe({
      next: (c) => this.ref.close(c),
      error: (e) => {
        this.guardando.set(false);
        this.mensaje.set(aplicarErroresServidor(this.formulario, e));
      },
    });
  }
}
