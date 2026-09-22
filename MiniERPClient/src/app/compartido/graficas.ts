import { Component, computed, input, signal } from '@angular/core';
import { entero, moneda } from './formato';

export type FormatoValor = 'moneda' | 'entero' | 'porcentaje';

export interface Dato {
  etiqueta: string;
  valor: number;
  /** Linea extra del recuadro emergente (p. ej. "8 ventas"). */
  detalle?: string;
}

function formatear(valor: number, formato: FormatoValor): string {
  switch (formato) {
    case 'moneda': return moneda(valor);
    case 'porcentaje': return `${valor.toFixed(2)} %`;
    default: return entero(valor);
  }
}

/** Redondea hacia arriba a un tope "limpio" (1, 2, 2.5, 5 x 10^n) para el eje. */
function topeLimpio(maximo: number): number {
  if (maximo <= 0) return 1;
  const potencia = 10 ** Math.floor(Math.log10(maximo));
  for (const paso of [1, 2, 2.5, 5, 10]) {
    if (paso * potencia >= maximo) return paso * potencia;
  }
  return 10 * potencia;
}

interface Emergente { x: number; y: number; titulo: string; valor: string; detalle?: string }

/**
 * Barras horizontales de una sola serie, para rankings (top 10...).
 * Un color, sin leyenda: el titulo de la tarjeta dice que se grafica.
 * Barras de 16px, base recta y punta redondeada, valor en la punta y
 * recuadro emergente al pasar el puntero. La tabla del reporte es la
 * vista accesible de los mismos datos.
 */
@Component({
  selector: 'app-grafica-barras',
  template: `
    <div class="grafica-barras" role="img" [attr.aria-label]="descripcion()">
      @for (d of datos(); track $index) {
        <div class="etiqueta" [title]="d.etiqueta">{{ d.etiqueta }}</div>
        <div class="pista"
             (mousemove)="mostrar($event, d)" (mouseleave)="emergente.set(null)">
          <div class="barra" [style.width.%]="ancho(d.valor)"></div>
          <span class="valor">{{ texto(d.valor) }}</span>
        </div>
      } @empty {
        <div class="vacio" style="grid-column: 1 / -1">Sin datos para graficar</div>
      }
    </div>
    @if (emergente(); as e) {
      <div class="info-emergente" [style.left.px]="e.x + 14" [style.top.px]="e.y + 14">
        <strong>{{ e.titulo }}</strong>{{ e.valor }}@if (e.detalle) {<br>{{ e.detalle }}}
      </div>
    }
  `,
})
export class GraficaBarras {
  readonly datos = input.required<Dato[]>();
  readonly formato = input<FormatoValor>('entero');

  protected readonly emergente = signal<Emergente | null>(null);
  // El valor mas largo necesita espacio a la derecha de la barra: 78% del ancho como maximo.
  private readonly maximo = computed(() => Math.max(0, ...this.datos().map((d) => d.valor)));
  protected readonly descripcion = computed(() =>
    this.datos().map((d) => `${d.etiqueta}: ${this.texto(d.valor)}`).join('; '));

  protected ancho(valor: number): number {
    const m = this.maximo();
    return m > 0 ? (valor / m) * 78 : 0;
  }
  protected texto(valor: number): string {
    return formatear(valor, this.formato());
  }
  protected mostrar(evento: MouseEvent, d: Dato): void {
    this.emergente.set({ x: evento.clientX, y: evento.clientY, titulo: d.etiqueta, valor: this.texto(d.valor), detalle: d.detalle });
  }
}

/**
 * Columnas de una sola serie para evoluciones en el tiempo (ventas por
 * mes, semana...). Eje con tres marcas limpias; solo se rotulan el
 * maximo y el ultimo periodo; el resto aparece al pasar el puntero.
 * Los periodos sin datos se ven como hueco en la base: tambien informan.
 */
@Component({
  selector: 'app-grafica-columnas',
  template: `
    <div class="grafica-columnas" role="img" [attr.aria-label]="descripcion()">
      <div style="display:flex; gap:8px">
        <div class="eje-y" style="position:relative; width:72px; height:200px; margin-top:20px">
          @for (m of marcas(); track m.valor) {
            <span class="muted" [style.bottom.%]="m.pos"
                  style="position:absolute; right:0; transform:translateY(50%); font-size:11px; white-space:nowrap">
              {{ m.texto }}
            </span>
          }
        </div>
        <div style="flex:1; min-width:0">
          <div class="area" style="position:relative">
            @for (m of marcas(); track m.valor) {
              @if (m.pos > 0) {
                <div [style.bottom.%]="m.pos * 200 / 220"
                     style="position:absolute; left:0; right:0; border-top:1px solid var(--erp-borde)"></div>
              }
            }
            @for (d of datos(); track $index; let i = $index) {
              <div class="col" (mousemove)="mostrar($event, d)" (mouseleave)="emergente.set(null)">
                @if (rotular(i)) {
                  <span class="cap">{{ texto(d.valor) }}</span>
                }
                <div class="columna" [style.height.%]="alto(d.valor)"></div>
              </div>
            }
          </div>
          <div class="ejes">
            @for (d of datos(); track $index; let i = $index) {
              <span [title]="d.etiqueta">{{ mostrarEtiqueta(i) ? corta(d.etiqueta) : '' }}</span>
            }
          </div>
        </div>
      </div>
    </div>
    @if (emergente(); as e) {
      <div class="info-emergente" [style.left.px]="e.x + 14" [style.top.px]="e.y - 10">
        <strong>{{ e.titulo }}</strong>{{ e.valor }}@if (e.detalle) {<br>{{ e.detalle }}}
      </div>
    }
  `,
})
export class GraficaColumnas {
  readonly datos = input.required<Dato[]>();
  readonly formato = input<FormatoValor>('moneda');

  protected readonly emergente = signal<Emergente | null>(null);
  private readonly tope = computed(() => topeLimpio(Math.max(0, ...this.datos().map((d) => d.valor))));
  private readonly indiceMaximo = computed(() => {
    const valores = this.datos().map((d) => d.valor);
    return valores.indexOf(Math.max(...valores));
  });
  protected readonly marcas = computed(() => {
    const t = this.tope();
    return [0, 0.5, 1].map((f) => ({ valor: f * t, pos: f * 100, texto: formatear(f * t, this.formato()).replace('.00', '') }));
  });
  protected readonly descripcion = computed(() =>
    this.datos().map((d) => `${d.etiqueta}: ${this.texto(d.valor)}`).join('; '));

  protected alto(valor: number): number {
    return (valor / this.tope()) * 100;
  }
  protected texto(valor: number): string {
    return formatear(valor, this.formato());
  }
  /** Solo el maximo y el ultimo periodo llevan cifra; el resto, en el recuadro emergente. */
  protected rotular(i: number): boolean {
    const d = this.datos();
    return d[i].valor > 0 && (i === this.indiceMaximo() || i === d.length - 1);
  }
  /** Con muchos periodos se rotula uno de cada n para que no se encimen. */
  protected mostrarEtiqueta(i: number): boolean {
    const n = this.datos().length;
    const cada = Math.ceil(n / 12);
    return i % cada === 0 || i === n - 1;
  }
  protected corta(etiqueta: string): string {
    return etiqueta.length > 12 ? etiqueta.slice(0, 11) + '…' : etiqueta;
  }
  protected mostrar(evento: MouseEvent, d: Dato): void {
    this.emergente.set({ x: evento.clientX, y: evento.clientY, titulo: d.etiqueta, valor: this.texto(d.valor), detalle: d.detalle });
  }
}
