import { BreakpointObserver } from '@angular/cdk/layout';
import { Component, computed, inject, viewChild } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatDividerModule } from '@angular/material/divider';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatMenuModule } from '@angular/material/menu';
import { MatSidenav, MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter, map } from 'rxjs';
import { permite } from '../nucleo/permisos';
import { Sesion } from '../nucleo/sesion';
import { etiqueta } from '../nucleo/utilidades';
import { MENU } from './menu';

/** Marco de la aplicacion autenticada: barra superior + menu lateral segun el area. */
@Component({
  selector: 'app-marco',
  imports: [
    RouterOutlet, RouterLink, RouterLinkActive, MatSidenavModule, MatToolbarModule, MatListModule,
    MatIconModule, MatButtonModule, MatMenuModule, MatDividerModule,
  ],
  templateUrl: './marco.html',
  styleUrl: './marco.scss',
})
export class Marco {
  protected readonly sesion = inject(Sesion);
  private readonly router = inject(Router);

  protected readonly movil = toSignal(
    inject(BreakpointObserver).observe('(max-width: 900px)').pipe(map((r) => r.matches)),
    { initialValue: false },
  );

  /** Solo los grupos y opciones que el area del usuario puede ver. */
  protected readonly menu = computed(() => {
    const rol = this.sesion.rol();
    return MENU.map((g) => ({
      ...g,
      opciones: g.opciones.filter((o) =>
        o.permiso ? permite(rol, o.permiso) : o.roles ? !!rol && o.roles.includes(rol) : true),
    })).filter((g) => g.opciones.length > 0);
  });

  protected readonly area = computed(() => etiqueta(this.sesion.rol()));

  constructor() {
    // En pantallas pequenas el menu se cierra al navegar
    this.router.events.pipe(filter((e) => e instanceof NavigationEnd)).subscribe(() => this.cerrarSiMovil());
  }

  private readonly lateral = viewChild.required(MatSidenav);

  private cerrarSiMovil(): void {
    if (this.movil()) {
      this.lateral().close();
    }
  }
}
