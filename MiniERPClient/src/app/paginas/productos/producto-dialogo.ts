import { Component, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { CatalogoApi } from '../../nucleo/api';
import { Producto, ProductoPeticion } from '../../nucleo/modelos';
import { aplicarErroresServidor, limpiar, maxDecimales, mensajeCampo } from '../../compartido/formularios';
import { seguro } from '../../nucleo/utilidades';

@Component({
  selector: 'app-producto-dialogo',
  imports: [ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatButtonModule, MatIconModule],
  template: `
    <h2 mat-dialog-title>{{ datos ? 'Editar producto' : 'Nuevo producto' }}</h2>
    <form [formGroup]="formulario" (ngSubmit)="guardar()" novalidate>
      <mat-dialog-content>
        <div class="formulario">
          <mat-form-field>
            <mat-label>Código</mat-label>
            <input matInput formControlName="codigo" maxlength="20" placeholder="ESC-006" cdkFocusInitial>
            <mat-hint>Letras, números y guiones</mat-hint>
            <mat-error>{{ error('codigo') }}</mat-error>
          </mat-form-field>
          <mat-form-field>
            <mat-label>Categoría</mat-label>
            <mat-select formControlName="idCategoria">
              @for (c of categorias(); track c.idCategoria) {
                <mat-option [value]="c.idCategoria">{{ c.nombre }}</mat-option>
              }
            </mat-select>
            <mat-error>{{ error('idCategoria') }}</mat-error>
          </mat-form-field>
          <mat-form-field class="completo">
            <mat-label>Nombre</mat-label>
            <input matInput formControlName="nombre" maxlength="120">
            <mat-error>{{ error('nombre') }}</mat-error>
          </mat-form-field>
          <mat-form-field class="completo">
            <mat-label>Descripción</mat-label>
            <textarea matInput formControlName="descripcion" rows="2" maxlength="300"></textarea>
            <mat-error>{{ error('descripcion') }}</mat-error>
          </mat-form-field>
          <mat-form-field>
            <mat-label>Precio de venta</mat-label>
            <span matTextPrefix>Q&nbsp;</span>
            <input matInput type="number" formControlName="precioVenta" min="0.01" step="0.01">
            <mat-error>{{ error('precioVenta') }}</mat-error>
          </mat-form-field>
          <mat-form-field>
            <mat-label>Unidad de medida</mat-label>
            <input matInput formControlName="unidadMedida" maxlength="20" placeholder="UNIDAD">
            <mat-error>{{ error('unidadMedida') }}</mat-error>
          </mat-form-field>
          <mat-form-field>
            <mat-label>Stock mínimo</mat-label>
            <input matInput type="number" formControlName="stockMinimo" min="0" step="1">
            <mat-hint>Por debajo de este nivel el producto aparece en alertas</mat-hint>
            <mat-error>{{ error('stockMinimo') }}</mat-error>
          </mat-form-field>
          @if (datos) {
            <div class="muted" style="align-self:center">
              Existencia actual: <strong>{{ datos.stockActual }}</strong><br>
              <small>Solo cambia con compras, ventas y ajustes</small>
            </div>
          }
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
export class ProductoDialogo {
  protected readonly datos = inject<Producto | null>(MAT_DIALOG_DATA);
  private readonly api = inject(CatalogoApi);
  private readonly ref = inject(MatDialogRef<ProductoDialogo, Producto>);

  protected readonly categorias = toSignal(seguro(this.api.categoriasActivas(), []), { initialValue: [] });
  protected readonly formulario = inject(FormBuilder).group({
    codigo: [this.datos?.codigo ?? '', [Validators.required, Validators.maxLength(20), Validators.pattern(/^[A-Za-z0-9-]+$/)]],
    nombre: [this.datos?.nombre ?? '', [Validators.required, Validators.maxLength(120)]],
    descripcion: [this.datos?.descripcion ?? '', [Validators.maxLength(300)]],
    idCategoria: [this.datos?.idCategoria ?? (null as number | null), [Validators.required]],
    unidadMedida: [this.datos?.unidadMedida ?? 'UNIDAD', [Validators.maxLength(20)]],
    precioVenta: [this.datos?.precioVenta ?? (null as number | null), [Validators.required, Validators.min(0.01), maxDecimales(2)]],
    stockMinimo: [this.datos?.stockMinimo ?? 0, [Validators.required, Validators.min(0)]],
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
    const valores = limpiar(this.formulario.getRawValue()) as unknown as ProductoPeticion;
    const peticion = this.datos
      ? this.api.productos.actualizar(this.datos.idProducto, valores)
      : this.api.productos.crear(valores);
    peticion.subscribe({
      next: (p) => this.ref.close(p),
      error: (e) => {
        this.guardando.set(false);
        this.mensaje.set(aplicarErroresServidor(this.formulario, e));
      },
    });
  }
}
