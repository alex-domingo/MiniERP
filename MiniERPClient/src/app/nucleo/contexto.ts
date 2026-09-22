import { HttpContextToken } from '@angular/common/http';

/**
 * Marca una peticion cuyo error va a manejar el propio componente (por
 * ejemplo un formulario que muestra los errores junto a cada campo), para
 * que el interceptor no muestre ademas un aviso generico.
 */
export const SIN_AVISO = new HttpContextToken<boolean>(() => false);
