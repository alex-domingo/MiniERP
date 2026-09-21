-- =====================================================================
--  MINI ERP - V4: bitacora historica de la carga inicial
-- ---------------------------------------------------------------------
--  V2 dejo en la bitacora solo ocho filas de resumen ("Carga inicial:
--  15 compras registradas"), todas con la hora en que corrio la
--  migracion. Con eso el reporte de logs no tiene nada que analizar.
--
--  Esta migracion reconstruye, a partir de los propios documentos, la
--  traza que la aplicacion habria dejado si las operaciones de la carga
--  inicial se hubieran registrado desde la interfaz:
--
--    * una fila CREAR por cada compra y cada venta, con la fecha, el
--      usuario y la descripcion que genera el aspecto de auditoria;
--    * un LOGIN del usuario 15 minutos antes de su primera operacion
--      de cada dia.
--
--  Solo toma documentos que todavia no tienen su fila en la bitacora
--  (id_entidad nulo en V2), de modo que no duplica las operaciones que
--  ya se hicieron desde la API en una base que esta en uso.
-- =====================================================================

-- ---------------------------------------------------------------------
--  1. Compras
-- ---------------------------------------------------------------------
INSERT INTO bitacora (id_usuario, nombre_usuario, modulo, accion, entidad_afectada,
                      id_entidad, descripcion, exitoso, fecha_hora)
SELECT c.id_usuario, u.nombre_usuario, 'COMPRAS', 'CREAR', 'compra', c.id_compra,
       LEFT('Registro de una compra: ' || c.numero_documento || ' - ' || p.nombre, 400),
       TRUE, c.fecha_compra
FROM   compra    c
JOIN   usuario   u ON u.id_usuario   = c.id_usuario
JOIN   proveedor p ON p.id_proveedor = c.id_proveedor
WHERE  NOT EXISTS (SELECT 1 FROM bitacora b
                   WHERE b.entidad_afectada = 'compra' AND b.id_entidad = c.id_compra)
ORDER  BY c.fecha_compra, c.id_compra;

-- ---------------------------------------------------------------------
--  2. Ventas
-- ---------------------------------------------------------------------
INSERT INTO bitacora (id_usuario, nombre_usuario, modulo, accion, entidad_afectada,
                      id_entidad, descripcion, exitoso, fecha_hora)
SELECT v.id_usuario, u.nombre_usuario, 'VENTAS', 'CREAR', 'venta', v.id_venta,
       LEFT('Registro de una venta: ' || v.numero_factura || ' - ' || cl.nombre, 400),
       TRUE, v.fecha_venta
FROM   venta   v
JOIN   usuario u  ON u.id_usuario  = v.id_usuario
JOIN   cliente cl ON cl.id_cliente = v.id_cliente
WHERE  NOT EXISTS (SELECT 1 FROM bitacora b
                   WHERE b.entidad_afectada = 'venta' AND b.id_entidad = v.id_venta)
ORDER  BY v.fecha_venta, v.id_venta;

-- ---------------------------------------------------------------------
--  3. Inicio de sesion previo a la primera operacion de cada dia
--     (solo para los dias de la carga inicial: los posteriores ya
--     tienen su LOGIN real registrado por la aplicacion)
-- ---------------------------------------------------------------------
INSERT INTO bitacora (id_usuario, nombre_usuario, modulo, accion, entidad_afectada,
                      id_entidad, descripcion, exitoso, fecha_hora)
SELECT d.id_usuario, u.nombre_usuario, 'AUTENTICACION', 'LOGIN', 'usuario', d.id_usuario,
       'Inicio de sesion exitoso del area ' || r.nombre, TRUE,
       d.primera_operacion - INTERVAL '15 minutes'
FROM  (SELECT o.id_usuario, CAST(o.fecha AS DATE) AS dia, MIN(o.fecha) AS primera_operacion
       FROM  (SELECT id_usuario, fecha_compra AS fecha FROM compra
              UNION ALL
              SELECT id_usuario, fecha_venta FROM venta) o
       GROUP  BY o.id_usuario, CAST(o.fecha AS DATE)) d
JOIN   usuario u ON u.id_usuario = d.id_usuario
JOIN   rol     r ON r.id_rol     = u.id_rol
WHERE  NOT EXISTS (SELECT 1 FROM bitacora b
                   WHERE b.accion = 'LOGIN'
                     AND b.id_usuario = d.id_usuario
                     AND CAST(b.fecha_hora AS DATE) = d.dia)
ORDER  BY d.primera_operacion;
