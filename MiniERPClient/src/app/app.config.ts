import { registerLocaleData } from '@angular/common';
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import localeEsGt from '@angular/common/locales/es-GT';
import { ApplicationConfig, ErrorHandler, LOCALE_ID, provideBrowserGlobalErrorListeners } from '@angular/core';
import { MAT_FORM_FIELD_DEFAULT_OPTIONS } from '@angular/material/form-field';
import { MatPaginatorIntl } from '@angular/material/paginator';
import { provideRouter, withComponentInputBinding, withInMemoryScrolling } from '@angular/router';
import { routes } from './app.routes';
import { proveerFechas } from './compartido/fechas';
import { PaginadorEspanol } from './compartido/paginador';
import { autenticacionInterceptor, erroresInterceptor } from './nucleo/http';
import { ManejadorErrores } from './nucleo/manejador-errores';

registerLocaleData(localeEsGt);

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes, withComponentInputBinding(), withInMemoryScrolling({ scrollPositionRestoration: 'top' })),
    provideHttpClient(withFetch(), withInterceptors([autenticacionInterceptor, erroresInterceptor])),
    { provide: ErrorHandler, useClass: ManejadorErrores },
    { provide: LOCALE_ID, useValue: 'es-GT' },
    { provide: MAT_FORM_FIELD_DEFAULT_OPTIONS, useValue: { appearance: 'outline', subscriptSizing: 'dynamic' } },
    { provide: MatPaginatorIntl, useClass: PaginadorEspanol },
    ...proveerFechas(),
  ],
};
