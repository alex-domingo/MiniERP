import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { CatalogoApi } from '../../nucleo/api';
import { Cliente, ClientePeticion } from '../../nucleo/modelos';
import { PATRON_NIT, aplicarErroresServidor, limpiar, mensajeCampo } from '../../compartido/formularios';

@Component({
  selector: 'app-cliente-dialogo',
  imports: [ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatIconModule],
  template: `
    <h2 mat-dialog-title>{{ datos ? 'Editar cliente' : 'Nuevo cliente' }}</h2>
    <form [formGroup]="formulario" (ngSubmit)="guardar()" novalidate>
      <mat-dialog-content>
        <div class="formulario">
          <mat-form-field>
            <mat-label>NIT</mat-label>
            <input matInput formControlName="nit" maxlength="20" placeholder="1234567-8" cdkFocusInitial>
            <mat-hint>1234567-8, o CF para consumidor final</mat-hint>
            <mat-error>{{ error('nit') }}</mat-error>
          </mat-form-field>
          <mat-form-field>
            <mat-label>Teléfono</mat-label>
            <input matInput formControlName="telefono" maxlength="20">
            <mat-error>{{ error('telefono') }}</mat-error>
          </mat-form-field>
          <mat-form-field class="completo">
            <mat-label>Nombre o razón social</mat-label>
            <input matInput formControlName="nombre" maxlength="120">
            <mat-error>{{ error('nombre') }}</mat-error>
          </mat-form-field>
          <mat-form-field class="completo">
            <mat-label>Correo</mat-label>
            <input matInput type="email" formControlName="correo" maxlength="120">
            <mat-error>{{ error('correo') }}</mat-error>
          </mat-form-field>
          <mat-form-field class="completo">
            <mat-label>Dirección</mat-label>
            <input matInput formControlName="direccion" maxlength="200">
            <mat-error>{{ error('direccion') }}</mat-error>
          </mat-form-field>
        </div>
        @if (mensaje()) { <p class="mensaje-error" role="alert"><mat-icon>error</mat-icon>{{ mensaje() }}</p> }
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" mat-dialog-close>Cancelar</button>
        <button mat-flat-button type="submit" [disabled]="guardando()">Guardar</button>
      </mat-dialog-actions>
    </form>
  `,
})
export class ClienteDialogo {
  protected readonly datos = inject<Cliente | null>(MAT_DIALOG_DATA);
  private readonly api = inject(CatalogoApi);
  private readonly ref = inject(MatDialogRef<ClienteDialogo, Cliente>);

  protected readonly formulario = inject(FormBuilder).nonNullable.group({
    nit: [this.datos?.nit ?? '', [Validators.required, Validators.maxLength(20), Validators.pattern(PATRON_NIT)]],
    nombre: [this.datos?.nombre ?? '', [Validators.required, Validators.maxLength(120)]],
    telefono: [this.datos?.telefono ?? '', [Validators.maxLength(20)]],
    correo: [this.datos?.correo ?? '', [Validators.email, Validators.maxLength(120)]],
    direccion: [this.datos?.direccion ?? '', [Validators.maxLength(200)]],
  });
  protected readonly guardando = signal(false);
  protected readonly mensaje = signal<string | null>(null);

  protected error(campo: string): string {
    const c = this.formulario.get(campo);
    return c?.hasError('pattern') && campo === 'nit' ? 'Formato 1234567-8, o CF' : mensajeCampo(c);
  }

  protected guardar(): void {
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }
    this.guardando.set(true);
    this.mensaje.set(null);
    const valores = limpiar(this.formulario.getRawValue()) as ClientePeticion;
    const peticion = this.datos
      ? this.api.clientes.actualizar(this.datos.idCliente, valores)
      : this.api.clientes.crear(valores);
    peticion.subscribe({
      next: (p) => this.ref.close(p),
      error: (e) => {
        this.guardando.set(false);
        this.mensaje.set(aplicarErroresServidor(this.formulario, e));
      },
    });
  }
}
