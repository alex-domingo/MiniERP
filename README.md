# Mini ERP

Sistema de gestión comercial para una empresa que vende y distribuye muebles para hogar y oficina. Integra productos, proveedores, compras, inventario, clientes y ventas, con control de acceso por área, valuación de inventario **UEPS**, factura en PDF y 12 reportes.

Son dos aplicaciones independientes bajo una arquitectura cliente-servidor: un cliente **Angular 21** consume la API REST de un servidor **Spring Boot 4 (Java 21)**, que es el único que accede a la base de datos **PostgreSQL**. La base se crea y se carga con migraciones de **Flyway**.

## Funcionalidades

| Área | Qué hace |
| --- | --- |
| **Administración** | Consulta todo el negocio, los 12 reportes y la bitácora. No registra operaciones. |
| **Compras** | Mantiene proveedores y los productos que cada uno suministra, y registra compras. |
| **Inventario** | Mantiene categorías y productos; consulta existencias, alertas y kardex; registra ajustes. |
| **Ventas** | Mantiene clientes, registra ventas y emite la factura en PDF. |

- Cada usuario entra con sus credenciales y ve solo lo de su área.
- Las salidas de inventario se valúan con UEPS y nunca dejan existencias negativas.
- Cada movimiento del kardex conserva su origen: compra, factura o ajuste.
- Nada se borra: productos, clientes y proveedores se desactivan y su historial se conserva.

## Puesta en marcha

**Requisitos:** JDK 21, Maven 3.9+, PostgreSQL 18 (o 16+) y Node.js 20.19+ / 22.12+ / 24+.

1. Crea la base vacía (en pgAdmin o psql). Las tablas y los datos los crea Flyway al arrancar.
   ```sql
   CREATE DATABASE minierp;
   ```
2. Define la contraseña de PostgreSQL como variable de entorno (Windows, una sola vez; luego reabre la terminal):
   ```bat
   setx DB_PASSWORD "tu_contraseña"
   ```
3. Arranca el servidor. Queda en `http://localhost:8080`.
   ```bash
   cd MiniERPServer
   mvn spring-boot:run
   ```
4. Arranca el cliente y abre `http://localhost:4200`.
   ```bash
   cd MiniERPClient
   npm install
   npm start
   ```

### Usuarios de la carga inicial

| Usuario | Contraseña | Área |
| --- | --- | --- |
| `admin` | `admin123` | Administración |
| `jcompras` | `compras123` | Compras |
| `minventario` | `inventario123` | Inventario |
| `cventas` | `ventas123` | Ventas |

La carga inicial incluye además 30 productos en 8 categorías, 8 proveedores, 20 clientes, 15 compras y 25 ventas.

---

Proyecto académico desarrollado por **Ludvin Alexander Domingo Artola**.
