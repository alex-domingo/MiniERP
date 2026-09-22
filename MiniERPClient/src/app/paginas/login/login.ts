import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { ActivatedRoute, Router } from '@angular/router';
import { Sesion } from '../../nucleo/sesion';
import { mensajeError } from '../../nucleo/utilidades';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatIconModule, MatProgressBarModule],
  templateUrl: './login.html',
  styleUrl: './login.scss',
})
export class Login {
  private readonly sesion = inject(Sesion);
  private readonly router = inject(Router);
  private readonly ruta = inject(ActivatedRoute);

  protected readonly formulario = inject(FormBuilder).nonNullable.group({
    usuario: ['', [Validators.required, Validators.minLength(4), Validators.maxLength(40)]],
    contrasena: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(100)]],
  });
  protected readonly enviando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly ocultar = signal(true);
  protected readonly expirada = this.ruta.snapshot.queryParamMap.get('motivo') === 'expirada';

  protected entrar(): void {
    if (this.formulario.invalid || this.enviando()) {
      this.formulario.markAllAsTouched();
      return;
    }
    const { usuario, contrasena } = this.formulario.getRawValue();
    this.enviando.set(true);
    this.error.set(null);
    this.sesion.iniciar(usuario.trim(), contrasena).subscribe({
      next: () => {
        const volver = this.ruta.snapshot.queryParamMap.get('volver');
        this.router.navigateByUrl(volver && volver.startsWith('/') ? volver : '/inicio');
      },
      error: (e: unknown) => {
        this.enviando.set(false);
        // El servidor responde igual a usuario inexistente y a contrasena erronea
        this.error.set(e instanceof HttpErrorResponse && e.status === 401
          ? 'Usuario o contraseña incorrectos.'
          : mensajeError(e));
      },
    });
  }
}
