-- =====================================================================
--  MINI ERP - Migracion V3 : Ajustes de inventario y numeracion
-- =====================================================================
--  1. Las capas de costo y sus consumos pueden originarse en un ajuste
--     de inventario, no solo en una compra o una venta.
--  2. Secuencias para numerar compras y facturas sin choques entre
--     operaciones simultaneas.
--
--  Nota: V1 y V2 no se modifican. Una migracion ya aplicada es
--  inmutable; Flyway detectaria el cambio de checksum y se negaria a
--  arrancar. Todo cambio de esquema posterior va en una version nueva.
-- =====================================================================


-- ---------------------------------------------------------------------
--  1a. Capas originadas por un AJUSTE DE ENTRADA
--
--  Un ajuste de entrada (unidades que aparecen en un conteo fisico)
--  debe crear su propia capa de costo. Si solo sumara al stock, dejaria
--  de cumplirse el invariante central del inventario:
--
--      producto.stock_actual = SUM(capa_inventario.cantidad_disponible)
--
--  Cada capa tiene exactamente UN origen: una linea de compra o un
--  movimiento de ajuste. num_nonnulls() lo exige a nivel de motor.
-- ---------------------------------------------------------------------
ALTER TABLE capa_inventario
    ALTER COLUMN id_detalle_compra DROP NOT NULL;

ALTER TABLE capa_inventario
    ADD COLUMN id_movimiento_ajuste BIGINT;

ALTER TABLE capa_inventario
    ADD CONSTRAINT fk_capa_mov_ajuste
        FOREIGN KEY (id_movimiento_ajuste) REFERENCES movimiento_inventario (id_movimiento);

ALTER TABLE capa_inventario
    ADD CONSTRAINT uq_capa_mov_ajuste UNIQUE (id_movimiento_ajuste);

ALTER TABLE capa_inventario
    ADD CONSTRAINT ck_capa_origen
        CHECK (num_nonnulls(id_detalle_compra, id_movimiento_ajuste) = 1);

COMMENT ON COLUMN capa_inventario.id_movimiento_ajuste
    IS 'Movimiento de ajuste de entrada que origino la capa. Excluyente con id_detalle_compra.';


-- ---------------------------------------------------------------------
--  1b. Consumos originados por un AJUSTE DE SALIDA
--
--  Un ajuste de salida (merma, dano, retiro antes de descontinuar un
--  producto) consume capas en el mismo orden UEPS que una venta, y ese
--  consumo debe quedar registrado igual, para que se siga cumpliendo:
--
--      capa.cantidad_inicial - SUM(consumos) = capa.cantidad_disponible
-- ---------------------------------------------------------------------
ALTER TABLE consumo_capa
    ALTER COLUMN id_detalle_venta DROP NOT NULL;

ALTER TABLE consumo_capa
    ADD COLUMN id_movimiento_ajuste BIGINT;

ALTER TABLE consumo_capa
    ADD CONSTRAINT fk_consumo_mov_ajuste
        FOREIGN KEY (id_movimiento_ajuste) REFERENCES movimiento_inventario (id_movimiento);

ALTER TABLE consumo_capa
    ADD CONSTRAINT uq_consumo_ajuste UNIQUE (id_capa, id_movimiento_ajuste);

ALTER TABLE consumo_capa
    ADD CONSTRAINT ck_consumo_origen
        CHECK (num_nonnulls(id_detalle_venta, id_movimiento_ajuste) = 1);

CREATE INDEX ix_consumo_mov_ajuste ON consumo_capa (id_movimiento_ajuste);


-- ---------------------------------------------------------------------
--  1c. Todo ajuste debe explicar su motivo
--
--  El enunciado exige poder determinar "como y por que" cambio una
--  existencia. En una compra o venta el porque es el documento; en un
--  ajuste no hay documento, asi que el motivo escrito es obligatorio.
-- ---------------------------------------------------------------------
ALTER TABLE movimiento_inventario
    ADD CONSTRAINT ck_mov_ajuste_motivo CHECK (
        origen NOT IN ('AJUSTE_ENTRADA', 'AJUSTE_SALIDA')
        OR (observaciones IS NOT NULL AND LENGTH(TRIM(observaciones)) >= 5)
    );


-- ---------------------------------------------------------------------
--  2. Numeracion de documentos
--
--  Calcular el siguiente numero con MAX()+1 falla en cuanto dos
--  usuarios registran a la vez: ambos leen el mismo maximo y el segundo
--  choca contra la restriccion UNIQUE. Una secuencia entrega numeros
--  distintos a cada transaccion sin bloquear a nadie.
--
--  Se inicializan a partir de los documentos existentes, para continuar
--  la numeracion de la carga inicial sin huecos ni repeticiones.
-- ---------------------------------------------------------------------
CREATE SEQUENCE seq_numero_compra AS BIGINT START WITH 1 MINVALUE 1;
CREATE SEQUENCE seq_numero_factura AS BIGINT START WITH 1 MINVALUE 1;

SELECT setval('seq_numero_compra',
              COALESCE((SELECT MAX(CAST(SUBSTRING(numero_documento FROM '[0-9]+$') AS BIGINT))
                        FROM compra), 0) + 1,
              false);

SELECT setval('seq_numero_factura',
              COALESCE((SELECT MAX(CAST(SUBSTRING(numero_factura FROM '[0-9]+$') AS BIGINT))
                        FROM venta), 0) + 1,
              false);


-- ---------------------------------------------------------------------
--  3. El kardex legible ahora identifica los ajustes
--
--  Se elimina y se vuelve a crear en lugar de usar CREATE OR REPLACE:
--  este ultimo no permite cambiar el tipo de una columna existente, y
--  agregar el literal 'AJUSTE' cambia "documento" de varchar(20) a
--  varchar. Ningun otro objeto depende de esta vista.
-- ---------------------------------------------------------------------
DROP VIEW vw_kardex;

CREATE VIEW vw_kardex AS
SELECT m.id_movimiento,
       m.id_producto,
       p.codigo           AS codigo_producto,
       p.nombre           AS nombre_producto,
       m.fecha_movimiento,
       m.tipo_movimiento,
       m.origen,
       COALESCE(co.numero_documento, v.numero_factura, 'AJUSTE') AS documento,
       COALESCE(pr.nombre, cl.nombre)                           AS tercero,
       m.cantidad,
       m.costo_unitario,
       (m.cantidad * m.costo_unitario) AS valor_movimiento,
       m.existencia_anterior,
       m.existencia_nueva,
       u.nombre_usuario   AS usuario,
       m.observaciones
FROM   movimiento_inventario m
JOIN   producto  p  ON p.id_producto  = m.id_producto
JOIN   usuario   u  ON u.id_usuario   = m.id_usuario
LEFT   JOIN compra    co ON co.id_compra    = m.id_compra
LEFT   JOIN proveedor pr ON pr.id_proveedor = co.id_proveedor
LEFT   JOIN venta     v  ON v.id_venta      = m.id_venta
LEFT   JOIN cliente   cl ON cl.id_cliente   = v.id_cliente;
