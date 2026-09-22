import { Component, OnInit, computed, inject, signal, viewChild } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, FormGroupDirective, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { filter, startWith } from 'rxjs';
import { BuscadorProducto } from '../../compartido/buscador-producto';
import { confirmar } from '../../compartido/confirmar';
import { Existencia } from '../../compartido/existencia';
import { MonedaPipe } from '../../compartido/formato';
import { aplicarErroresServidor, maxDecimales, mensajeCampo } from '../../compartido/formularios';
import { CatalogoApi, InventarioApi } from '../../nucleo/api';
import { Movimiento, Producto, TipoMovimiento, Valuacion } from '../../nucleo/modelos';

/**
 * Ajustes de inventario: corrigen la existencia cuando el conteo fisico
 * no coincide con el sistema (merma, dano, sobrante). Siempre dejan un
 * movimiento en el kardex con su motivo; nunca se edita el stock "a mano".
 *
 * - Entrada: crea una capa de costo. Si no se indica costo, el servidor
 *   usa el de la entrada mas reciente del producto.
 * - Salida: el costo NO se elige; lo determina el metodo UEPS.
 */
@Component({
  selector: 'app-ajustes',
  imports: [
    RouterLink, ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatButtonToggleModule,
    MatIconModule, MatProgressBarModule, MonedaPipe, BuscadorProducto, Existencia,
  ],
  templateUrl: './ajustes.html',
})
export class Ajustes implements OnInit {
  private readonly api = inject(InventarioApi);
  private readonly catalogo = inject(CatalogoApi);
  private readonly ruta = inject(ActivatedRoute);
  private readonly dialogo = inject(MatDialog);
  private readonly buscador = viewChild.required(BuscadorProducto);
  private readonly directivaFormulario = viewChild(FormGroupDirective);

  protected readonly producto = signal<Producto | null>(null);
  protected readonly valuacion = signal<Valuacion | null>(null);
  protected readonly resultado = signal<Movimiento | null>(null);
  protected readonly guardando = signal(false);
  protected readonly mensaje = signal<string | null>(null);

  protected readonly formulario = inject(FormBuilder).group({
    tipo: ['SALIDA' as TipoMovimiento, Validators.required],
    cantidad: [null as number | null, [Validators.required, Validators.min(1), Validators.max(100000)]],
    costoUnitario: [null as number | null, [Validators.min(0.01), maxDecimales(4)]],
    motivo: ['', [Validators.required, Validators.minLength(5), Validators.maxLength(300)]],
  });
  protected readonly tipo = toSignal(this.formulario.controls.tipo.valueChanges.pipe(startWith('SALIDA' as TipoMovimiento)));
  protected readonly ultimoCosto = computed(() => {
    const capas = this.valuacion()?.capas ?? [];
    // La capa mas reciente, sin importar si ya se agoto: es la regla del servidor
    return [...capas].sort((a, b) => b.fechaEntrada.localeCompare(a.fechaEntrada) || b.idCapa - a.idCapa)[0]?.costoUnitario ?? null;
  });
  protected readonly mensajeCampo = mensajeCampo;

  ngOnInit(): void {
    const id = Number(this.ruta.snapshot.queryParamMap.get('producto'));
    if (id) {
      this.catalogo.productos.obtener(id).subscribe((p) => {
        this.buscador().fijar(p);
        this.elegir(p);
      });
    }
    // En una salida el costo lo decide UEPS: el campo se deshabilita
    this.formulario.controls.tipo.valueChanges.subscribe((t) => {
      const costo = this.formulario.controls.costoUnitario;
      if (t === 'SALIDA') {
        costo.reset(null);
        costo.disable();
      } else {
        costo.enable();
      }
      this.validarMaximo();
    });
    this.formulario.controls.costoUnitario.disable();
  }

  protected elegir(p: Producto): void {
    this.producto.set(p);
    this.resultado.set(null);
    this.mensaje.set(null);
    this.valuacion.set(null);
    this.api.valuacion(p.idProducto).subscribe((v) => this.valuacion.set(v));
    this.validarMaximo();
  }

  /** Una salida no puede superar la existencia (el servidor lo vuelve a comprobar). */
  private validarMaximo(): void {
    const c = this.formulario.controls.cantidad;
    const p = this.producto();
    const tope = this.formulario.controls.tipo.value === 'SALIDA' && p ? p.stockActual : 100000;
    c.setValidators([Validators.required, Validators.min(1), Validators.max(tope)]);
    c.updateValueAndValidity();
  }

  protected errorCantidad(): string {
    const c = this.formulario.controls.cantidad;
    const p = this.producto();
    return c.hasError('max') && this.formulario.controls.tipo.value === 'SALIDA' && p
      ? `Solo hay ${p.stockActual} en existencia`
      : mensajeCampo(c);
  }

  protected aplicar(): void {
    const p = this.producto();
    if (!p || this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }
    const v = this.formulario.getRawValue();
    const nuevo = v.tipo === 'ENTRADA' ? p.stockActual + Number(v.cantidad) : p.stockActual - Number(v.cantidad);
    confirmar(this.dialogo, {
      titulo: v.tipo === 'ENTRADA' ? 'Ajuste de entrada' : 'Ajuste de salida',
      mensaje: `${p.codigo}: la existencia pasará de ${p.stockActual} a ${nuevo}. ` +
        `Motivo: "${v.motivo?.trim()}". El ajuste queda en el kardex y en la bitácora.`,
      aceptar: 'Aplicar ajuste',
      peligro: v.tipo === 'SALIDA',
    }).pipe(filter(Boolean)).subscribe(() => this.enviar(p));
  }

  private enviar(p: Producto): void {
    const v = this.formulario.getRawValue();
    this.guardando.set(true);
    this.mensaje.set(null);
    this.api.ajustar({
      idProducto: p.idProducto,
      tipo: v.tipo!,
      cantidad: Number(v.cantidad),
      costoUnitario: v.tipo === 'ENTRADA' && v.costoUnitario ? Number(v.costoUnitario) : null,
      motivo: v.motivo!.trim(),
    }).subscribe({
      next: (m) => {
        this.guardando.set(false);
        this.resultado.set(m);
        // resetForm limpia tambien el estado "enviado": sin eso los campos
        // vacios aparecerian en rojo como si el usuario hubiera fallado
        this.directivaFormulario()?.resetForm({ tipo: v.tipo, cantidad: null, costoUnitario: null, motivo: '' });
        if (v.tipo === 'SALIDA') {
          this.formulario.controls.costoUnitario.disable();
        }
        // Refresca existencia y capas del producto
        this.catalogo.productos.obtener(p.idProducto).subscribe((act) => {
          this.producto.set(act);
          this.validarMaximo();
        });
        this.api.valuacion(p.idProducto).subscribe((val) => this.valuacion.set(val));
      },
      error: (e) => {
        this.guardando.set(false);
        this.mensaje.set(aplicarErroresServidor(this.formulario, e));
      },
    });
  }
}
