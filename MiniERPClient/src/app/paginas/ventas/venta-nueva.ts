import { DecimalPipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormArray, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { filter, map, startWith } from 'rxjs';
import { BuscadorProducto } from '../../compartido/buscador-producto';
import { confirmar } from '../../compartido/confirmar';
import { MonedaPipe } from '../../compartido/formato';
import { mensajeCampo } from '../../compartido/formularios';
import { CatalogoApi, OperacionesApi, SistemaApi } from '../../nucleo/api';
import { Cliente, Producto } from '../../nucleo/modelos';
import { mensajeError, seguro } from '../../nucleo/utilidades';
import { FacturaEmitida } from './factura-emitida';

type Linea = FormGroup<{
  producto: FormControl<Producto>;
  cantidad: FormControl<number | null>;
}>;

/**
 * Registro de una venta.
 *
 * La pantalla ya impide pedir mas de lo que hay en existencia, pero la
 * verificacion que cuenta es la del servidor, que bloquea las filas del
 * producto y vuelve a comprobar: si otra venta se llevo las unidades en
 * el intervalo, responde 409 y aqui se muestra el detalle.
 *
 * Los totales mostrados son una vista previa con el precio del catalogo
 * y la tasa de IVA vigente; los definitivos los calcula el servidor.
 */
@Component({
  selector: 'app-venta-nueva',
  imports: [
    RouterLink, ReactiveFormsModule, MatFormFieldModule, MatAutocompleteModule, MatInputModule, MatButtonModule,
    MatIconModule, MatTooltipModule, MatProgressBarModule, MonedaPipe, BuscadorProducto, DecimalPipe,
  ],
  templateUrl: './venta-nueva.html',
})
export class VentaNueva implements OnInit {
  private readonly catalogo = inject(CatalogoApi);
  private readonly api = inject(OperacionesApi);
  private readonly router = inject(Router);
  private readonly ruta = inject(ActivatedRoute);
  private readonly dialogo = inject(MatDialog);

  private readonly clientes = toSignal(seguro(this.catalogo.clientesActivos(), [] as Cliente[]), { initialValue: [] as Cliente[] });
  protected readonly iva = toSignal(seguro(inject(SistemaApi).salud$.pipe(map((s) => s.iva)), 0.12), { initialValue: 0.12 });

  protected readonly cliente = new FormControl<Cliente | string | null>(null, Validators.required);
  private readonly textoCliente = toSignal(this.cliente.valueChanges.pipe(startWith(null)));
  protected readonly clientesFiltrados = computed(() => {
    const t = this.textoCliente();
    const busca = typeof t === 'string' ? t.trim().toLowerCase() : '';
    return this.clientes()
      .filter((c) => !busca || c.nombre.toLowerCase().includes(busca) || c.nit.toLowerCase().includes(busca))
      .slice(0, 30);
  });
  protected mostrarCliente = (c: Cliente | string | null): string =>
    c && typeof c === 'object' ? `${c.nombre} · NIT ${c.nit}` : (c ?? '');

  protected readonly lineas = new FormArray<Linea>([]);
  private readonly valores = toSignal(this.lineas.valueChanges.pipe(startWith([])), { initialValue: [] });
  protected readonly subtotal = computed(() =>
    this.valores().reduce((s, l) => s + (Number(l.cantidad) || 0) * (l.producto?.precioVenta ?? 0), 0));
  protected readonly montoIva = computed(() => Math.round(this.subtotal() * this.iva() * 100) / 100);
  protected readonly total = computed(() => this.subtotal() + this.montoIva());

  protected readonly guardando = signal(false);
  protected readonly mensaje = signal<string | null>(null);
  protected readonly mensajeCampo = mensajeCampo;

  ngOnInit(): void {
    const id = Number(this.ruta.snapshot.queryParamMap.get('cliente'));
    if (id) {
      this.catalogo.clientes.obtener(id).subscribe((c) => this.cliente.setValue(c));
    }
  }

  protected noVendible = (p: Producto): boolean =>
    p.stockActual <= 0 || this.lineas.controls.some((l) => l.controls.producto.value.idProducto === p.idProducto);

  protected agregar(p: Producto): void {
    if (this.noVendible(p)) {
      return;
    }
    this.lineas.push(new FormGroup({
      producto: new FormControl(p, { nonNullable: true }),
      cantidad: new FormControl<number | null>(1, [Validators.required, Validators.min(1), Validators.max(p.stockActual)]),
    }));
    this.mensaje.set(null);
  }

  protected quitar(i: number): void {
    this.lineas.removeAt(i);
  }

  protected importe(l: Linea): number {
    return (Number(l.value.cantidad) || 0) * l.controls.producto.value.precioVenta;
  }

  protected errorCantidad(l: Linea): string {
    const c = l.controls.cantidad;
    return c.hasError('max') ? `Solo hay ${l.controls.producto.value.stockActual} en existencia` : mensajeCampo(c);
  }

  protected registrar(): void {
    const cliente = this.cliente.value;
    if (!cliente || typeof cliente === 'string') {
      this.cliente.setErrors({ required: true });
      this.cliente.markAsTouched();
      return;
    }
    if (this.lineas.length === 0 || this.lineas.invalid) {
      this.lineas.markAllAsTouched();
      this.mensaje.set(this.lineas.length === 0 ? 'Agregue al menos un producto.' : null);
      return;
    }
    confirmar(this.dialogo, {
      titulo: 'Registrar venta',
      mensaje: `Venta a ${cliente.nombre} por Q ${this.total().toFixed(2)} (IVA incluido). ` +
        'Se descontarán las existencias y se emitirá la factura. La venta no se puede modificar después.',
      aceptar: 'Registrar y facturar',
    }).pipe(filter(Boolean)).subscribe(() => this.enviar(cliente));
  }

  private enviar(cliente: Cliente): void {
    this.guardando.set(true);
    this.mensaje.set(null);
    this.api.registrarVenta({
      idCliente: cliente.idCliente,
      lineas: this.lineas.controls.map((l) => ({
        idProducto: l.controls.producto.value.idProducto,
        cantidad: Number(l.controls.cantidad.value),
      })),
    }).subscribe({
      next: (venta) => {
        this.dialogo.open(FacturaEmitida, { data: venta, width: '480px', disableClose: true })
          .afterClosed()
          .subscribe((accion: 'nueva' | 'detalle') => {
            if (accion === 'nueva') {
              this.reiniciar();
            } else {
              this.router.navigate(['/ventas', venta.idVenta]);
            }
          });
      },
      error: (e) => {
        this.guardando.set(false);
        this.mensaje.set(mensajeError(e));
      },
    });
  }

  private reiniciar(): void {
    this.guardando.set(false);
    this.cliente.reset(null);
    this.lineas.clear();
    this.mensaje.set(null);
  }
}
