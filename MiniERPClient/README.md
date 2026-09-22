# Mini ERP · Cliente Angular

Aplicación cliente del sistema de gestión comercial Mini ERP. Consume la API REST
del servidor Spring Boot (`MiniERPServer`); nunca se conecta a la base de datos.

- Angular 21 (componentes standalone, signals, sin zone.js) y Angular Material 21
- Idioma y formatos de Guatemala: `es-GT`, fechas `dd/mm/aaaa`, moneda `Q 1,234.50`
- Cada página se carga bajo demanda (lazy loading)

## Requisitos

- Node.js 20.19+, 22.12+ o 24+ (lo que exige Angular 21) y npm
- El servidor corriendo en `http://localhost:8080` (`mvn spring-boot:run` en `MiniERPServer`)

## Ejecutar en desarrollo

```bash
npm install
npm start          # equivale a: ng serve
```

Abrir `http://localhost:4200`. Durante el desarrollo, `proxy.conf.json` redirige
las llamadas a `/api` hacia `http://localhost:8080`, de modo que el navegador
nunca hace peticiones de otro origen.

Usuarios de la carga inicial (uno por área):

| Usuario       | Contraseña      | Área           |
|---------------|-----------------|----------------|
| `admin`       | `admin123`      | Administración |
| `jcompras`    | `compras123`    | Compras        |
| `minventario` | `inventario123` | Inventario     |
| `cventas`     | `ventas123`     | Ventas         |

## Pruebas y compilación

```bash
npm test -- --watch=false   # pruebas unitarias (Vitest)
npm run build               # compilación de producción en dist/MiniERPClient
```

Para publicar el build de producción en otro servidor, la API debe quedar
disponible en la ruta `/api` del mismo origen (proxy inverso), o bien el
servidor debe incluir ese origen en `minierp.cors.origenes`.

## Estructura

```
src/app/
  nucleo/        modelos de la API, servicios HTTP, sesión JWT, interceptores,
                 guardias de ruta y matriz de permisos por área
  compartido/    piezas reutilizables: listados paginados, buscador de productos,
                 rango de fechas, gráficas, formatos, diálogos
  diseno/        marco de la aplicación y menú según el área del usuario
  paginas/       una carpeta por módulo: catálogos, compras, ventas,
                 inventario y reportes
```

### Seguridad en el cliente

- El token JWT se guarda en `localStorage` y la sesión se cierra sola cuando vence
  (se lee el claim `exp` del propio token).
- Un interceptor agrega `Authorization: Bearer ...` y, si el servidor responde 401,
  cierra la sesión y vuelve al inicio de sesión.
- Menús, botones y rutas se muestran según el área (`nucleo/permisos.ts`), pero la
  seguridad real la impone el servidor: aunque se fuerce la interfaz, responde 403.

### Reportes

Los 12 reportes se describen en `paginas/reportes/definiciones.ts` (filtros,
columnas, resumen y gráfica) y un único visor los presenta. Todos se pueden
imprimir (en horizontal) y exportar a CSV compatible con Excel.
