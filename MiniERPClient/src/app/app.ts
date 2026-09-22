import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

/**
 * Componente raiz: solo aloja el enrutador. El marco con el menu vive en
 * diseno/marco y se carga despues del inicio de sesion.
 */
@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {}
