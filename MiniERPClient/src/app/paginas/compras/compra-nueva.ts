import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormArray, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { filter, startWith } from 'rxjs';
import { confirmar } from '../../compartido/confirmar';
import { MonedaPipe } from '../../compartido/formato';
import { maxDecimales, mensajeCampo } from '../../compartido/formularios';
import { CatalogoApi, OperacionesApi } from '../../nucleo/api';
import { ProveedorProducto } from '../../nucleo/modelos';
import { Notificacion } from '../../nucleo/notificacion';
import { mensajeError, seguro } from '../../nucleo/utilidades';

type Linea = FormGroup<{
  producto: FormControl<ProveedorProducto>;
  cantidad: FormControl<number | null>;
  costoUnitario: FormControl<number | null>;
}>;

/**
 * Registro de una compra. El proveedor determina que productos se pueden
 * elegir (relacion N:M) y propone su costo de referencia, que el usuario
 * puede ajustar al precio real de la factura del proveedor.
 */
@Component({
  selector: 'app-compra-nueva',
  imports: [
    RouterLink, ReactiveFormsModule, MatFormFieldModule, MatSelectModule, MatInputModule, MatButtonModule,
    MatIconModule, MatTooltipModule, MatProgressBarModule, MonedaPipe,
  ],
  templateUrl: './compra-nueva.html',
})
export class CompraNueva implements OnInit {
  private readonly catalogo = inject(CatalogoApi);
  private readonly api = inject(OperacionesApi);
  private readonly router = inject(Router);
  private readonly ruta = inject(ActivatedRoute);
  private readonly dialogo = inject(MatDialog);
  private readonly aviso = inject(Notificacion);

  protected readonly proveedores = toSignal(seguro(this.catalogo.proveedoresActivos(), []), { initialValue: [] });
  protected readonly productos = signal<ProveedorProducto[]>([]);

  protected readonly proveedor = new FormControl<number | null>(null, Validators.required);
  protected readonly observaciones = new FormControl('', { nonNullable: true, validators: Validators.maxLength(300) });
  protected readonly lineas = new FormArray<Linea>([]);
  protected readonly aAgregar = new FormControl<ProveedorProducto | null>(null);

  private readonly valores = toSignal(this.lineas.valueChanges.pipe(startWith([])), { initialValue: [] });
  protected readonly total = computed(() =>
    this.valores().reduce((s, l) => s + (Number(l.cantidad) || 0) * (Number(l.costoUnitario) || 0), 0));

  protected readonly guardando = signal(false);
  protected readonly mensaje = signal<string | null>(null);
  protected readonly mensajeCampo = mensajeCampo;
  private proveedorAnterior: number | null = null;

  ngOnInit(): void {
    const id = Number(this.ruta.snapshot.queryParamMap.get('proveedor'));
    if (id) {
      this.proveedor.setValue(id);
      this.cambiarProveedor(id);
    }
  }

  protected cambiarProveedor(id: number | null): void {
    const aplicar = () => {
      this.proveedorAnterior = id;
      this.lineas.clear();
      this.productos.set([]);
      if (id) {
        this.catalogo.productosDeProveedor(id).subscribe((l) => this.productos.set(l));
      }
    };
    if (this.lineas.length > 0 && id !== this.proveedorAnterior) {
      confirmar(this.dialogo, {
        titulo: 'Cambiar de proveedor',
        mensaje: 'Las líneas agregadas pertenecen al proveedor anterior y se quitarán.',
        aceptar: 'Cambiar',
      }).subscribe((si) => (si ? aplicar() : this.proveedor.setValue(this.proveedorAnterior)));
    } else {
      aplicar();
    }
  }

  protected agregado = (p: ProveedorProducto): boolean =>
    this.lineas.controls.some((l) => l.controls.producto.value.idProducto === p.idProducto);

  protected agregar(p: ProveedorProducto | null): void {
    if (!p || this.agregado(p)) {
      return;
    }
    this.lineas.push(new FormGroup({
      producto: new FormControl(p, { nonNullable: true }),
      cantidad: new FormControl<number | null>(1, [Validators.required, Validators.min(1), Validators.max(100000)]),
      costoUnitario: new FormControl<number | null>(p.costoReferencia,
        [Validators.required, Validators.min(0.01), maxDecimales(2)]),
    }));
    this.aAgregar.setValue(null);
  }

  protected quitar(i: number): void {
    this.lineas.removeAt(i);
  }

  protected subtotal(l: Linea): number {
    return (Number(l.value.cantidad) || 0) * (Number(l.value.costoUnitario) || 0);
  }

  protected registrar(): void {
    if (this.proveedor.invalid || this.lineas.length === 0 || this.lineas.invalid) {
      this.proveedor.markAsTouched();
      this.lineas.markAllAsTouched();
      this.mensaje.set(this.lineas.length === 0 ? 'Agregue al menos un producto.' : null);
      return;
    }
    const proveedor = this.proveedores().find((p) => p.idProveedor === this.proveedor.value);
    confirmar(this.dialogo, {
      titulo: 'Registrar compra',
      mensaje: `Se registrará la compra a ${proveedor?.nombre} por un total de Q ${this.total().toFixed(2)}. ` +
        'Las existencias aumentarán de inmediato y la compra no se puede modificar después.',
      aceptar: 'Registrar',
    }).pipe(filter(Boolean)).subscribe(() => this.enviar());
  }

  private enviar(): void {
    this.guardando.set(true);
    this.mensaje.set(null);
    this.api.registrarCompra({
      idProveedor: this.proveedor.value!,
      observaciones: this.observaciones.value.trim() || null,
      lineas: this.lineas.controls.map((l) => ({
        idProducto: l.controls.producto.value.idProducto,
        cantidad: Number(l.controls.cantidad.value),
        costoUnitario: Number(l.controls.costoUnitario.value),
      })),
    }).subscribe({
      next: (c) => {
        this.aviso.exito(`Compra ${c.numeroDocumento} registrada por Q ${c.total.toFixed(2)}`);
        this.router.navigate(['/compras', c.idCompra]);
      },
      error: (e) => {
        this.guardando.set(false);
        this.mensaje.set(mensajeError(e));
      },
    });
  }
}
