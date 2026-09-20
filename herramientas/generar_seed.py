#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Generador de la migracion V2 (carga inicial) del Mini ERP.

Simula el motor de inventario UEPS (LIFO) sobre una linea de tiempo de
compras y ventas, de modo que los datos generados sean internamente
consistentes:

  * ninguna venta consume existencias que no existian en esa fecha
  * cada salida consume capas de costo en orden UEPS
  * el kardex cuadra movimiento por movimiento
  * producto.stock_actual coincide con la suma del kardex y con la suma
    de cantidad_disponible de las capas
"""

import random
from decimal import Decimal, ROUND_HALF_UP
from datetime import datetime, timedelta

import bcrypt

random.seed(20260920)          # reproducible: la migracion no cambia entre ejecuciones

D2 = Decimal("0.01")
D4 = Decimal("0.0001")
IVA = Decimal("0.12")          # Guatemala


def q2(x):
    return Decimal(x).quantize(D2, rounding=ROUND_HALF_UP)


def q4(x):
    return Decimal(x).quantize(D4, rounding=ROUND_HALF_UP)


def esc(s):
    """Escapa comillas simples para literales SQL."""
    return s.replace("'", "''")


def hashear(clave):
    return bcrypt.hashpw(clave.encode(), bcrypt.gensalt(rounds=10, prefix=b"2a")).decode()


# =====================================================================
#  1. CATALOGOS BASE
# =====================================================================

ROLES = [
    (1, "ADMINISTRACION", "Supervisa el funcionamiento general del negocio. Acceso total de consulta y a todos los reportes."),
    (2, "COMPRAS",        "Gestiona el abastecimiento de productos y la relacion con proveedores."),
    (3, "INVENTARIO",     "Controla las existencias y los movimientos de productos (valuacion UEPS)."),
    (4, "VENTAS",         "Gestiona la comercializacion de productos a los clientes."),
]

USUARIOS = [
    (1, "admin",      "admin123",      "Ludvin Alexander Domingo Artola", "admin@minierp.gt",      1),
    (2, "jcompras",   "compras123",    "Jorge Estuardo Ramirez Lopez",    "compras@minierp.gt",    2),
    (3, "minventario","inventario123", "Maria Fernanda Coc Batz",         "inventario@minierp.gt", 3),
    (4, "cventas",    "ventas123",     "Carlos Andres Mejia Sandoval",    "ventas@minierp.gt",     4),
]

CATEGORIAS = [
    (1, "Escritorios",           "Escritorios y estaciones de trabajo para hogar y oficina"),
    (2, "Sillas",                "Sillas ejecutivas, secretariales, de visita y bancos"),
    (3, "Mesas",                 "Mesas de comedor, centro, juntas y multiusos"),
    (4, "Estanterias",           "Libreras, estanterias metalicas y repisas"),
    (5, "Sofas",                 "Sofas, sillones y muebles de sala"),
    (6, "Archivadores",          "Archivadores y gabinetes de almacenamiento"),
    (7, "Camas",                 "Camas matrimoniales, individuales y literas"),
    (8, "Accesorios de Oficina", "Complementos y accesorios para estaciones de trabajo"),
]

# (id, codigo, nombre, id_categoria, costo_base, stock_minimo)
PRODUCTOS = [
    ( 1, "ESC-001", "Escritorio Ejecutivo Caoba 1.80m",        1, 1850, 5),
    ( 2, "ESC-002", "Escritorio Secretarial Melamina Blanco",  1,  890, 8),
    ( 3, "ESC-003", "Escritorio en L Gerencial",               1, 2450, 4),
    ( 4, "ESC-004", "Escritorio Compacto para Estudio",        1,  620,10),
    ( 5, "ESC-005", "Estacion de Trabajo Modular 4 Puestos",   1, 3100, 3),
    ( 6, "SIL-001", "Silla Ejecutiva Ergonomica de Malla",     2, 1250,12),
    ( 7, "SIL-002", "Silla Secretarial Giratoria",             2,  480,15),
    ( 8, "SIL-003", "Silla de Visita Tapizada",                2,  390,20),
    ( 9, "SIL-004", "Silla Gamer Reclinable",                  2, 1680, 6),
    (10, "SIL-005", "Banco Alto para Barra",                   2,  340,10),
    (11, "MES-001", "Mesa de Comedor 6 Personas Roble",        3, 2980, 4),
    (12, "MES-002", "Mesa de Centro Cristal Templado",         3,  950, 6),
    (13, "MES-003", "Mesa de Juntas Ovalada 10 Personas",      3, 5400, 2),
    (14, "MES-004", "Mesa Plegable Multiusos 1.80m",           3,  420,12),
    (15, "MES-005", "Mesa de Noche 2 Gavetas",                 3,  560,10),
    (16, "EST-001", "Librera 5 Niveles Melamina",              4,  780, 8),
    (17, "EST-002", "Estanteria Metalica Industrial 5 Niveles",4, 1340, 5),
    (18, "EST-003", "Repisa Flotante Decorativa 90cm",         4,  210,15),
    (19, "EST-004", "Estanteria Modular para Oficina",         4, 1120, 5),
    (20, "SOF-001", "Sofa 3 Plazas Tela Gris",                 5, 3450, 3),
    (21, "SOF-002", "Sofa Cama Matrimonial",                   5, 4200, 2),
    (22, "SOF-003", "Sillon Reclinable Individual",            5, 2350, 4),
    (23, "SOF-004", "Sofa Seccional en L 5 Plazas",            5, 6800, 2),
    (24, "ARC-001", "Archivador Metalico 4 Gavetas",           6, 1580, 5),
    (25, "ARC-002", "Archivador Movil 3 Gavetas",              6,  890, 8),
    (26, "ARC-003", "Gabinete de Archivo Puertas Batientes",   6, 2100, 3),
    (27, "CAM-001", "Cama Matrimonial con Cabecera Tapizada",  7, 3200, 3),
    (28, "CAM-002", "Cama Individual Juvenil",                 7, 1750, 5),
    (29, "CAM-003", "Litera Doble de Madera",                  7, 4100, 2),
    (30, "ACC-001", "Pedestal para CPU con Ruedas",            8,  280,15),
]
assert len(PRODUCTOS) == 30, "El enunciado exige exactamente 30 productos"

MARGEN = Decimal("1.45")       # precio de venta sobre el costo base

PROVEEDORES = [
    (1, "1234567-8",  "Muebles del Valle, S.A.",                 "Ricardo Alvarado",  "2234-5566", "ventas@mueblesdelvalle.gt",  "8a. Avenida 12-45 Zona 9, Guatemala"),
    (2, "2345678-9",  "Industrias Madereras Xelaju",             "Ana Lucia Perez",   "7761-2200", "contacto@maderasxelaju.gt",  "Calzada Independencia 5-30, Quetzaltenango"),
    (3, "3456789-0",  "Importadora Oficina Total",               "Luis Fernando Diaz","2445-8899", "compras@oficinatotal.com.gt","Boulevard Liberacion 18-22 Zona 13, Guatemala"),
    (4, "4567890-1",  "Distribuidora El Roble",                  "Marta Elena Solis", "2298-7744", "info@distelroble.gt",        "Km 15.5 Carretera a El Salvador"),
    (5, "5678901-2",  "Comercial Mobiliaria Centroamericana",    "Oscar Rene Godoy",  "2376-1133", "ventas@comobica.com.gt",     "3a. Calle 7-19 Zona 4, Guatemala"),
    (6, "6789012-3",  "Metalicos y Estructuras, S.A.",           "Julio Cesar Orozco","2489-5050", "pedidos@metalest.gt",        "Anillo Periferico 22-40 Zona 11, Guatemala"),
    (7, "7890123-4",  "Tapiceria y Diseno Moderno",              "Sandra Ivette Cruz","2331-6677", "diseno@tapimoderno.gt",      "Avenida Petapa 34-12 Zona 12, Guatemala"),
    (8, "8901234-5",  "Grupo Importador Asia-GT",                "Kevin Alberto Lam", "2255-9090", "import@asiagt.com",          "Calzada Roosevelt 27-55 Zona 7, Guatemala"),
]

CLIENTES = [
    ( 1, "9876543-2",  "Corporacion Financiera del Istmo, S.A.", "2201-4500", "compras@cfistmo.gt",        "Diagonal 6 10-01 Zona 10, Guatemala"),
    ( 2, "8765432-1",  "Colegio Bilingue Santa Teresa",          "2334-7788", "administracion@cbst.edu.gt","5a. Calle 18-33 Zona 15, Guatemala"),
    ( 3, "7654321-0",  "Hotel Plaza Colonial",                   "7832-4411", "gerencia@plazacolonial.gt", "4a. Avenida Norte 8, Antigua Guatemala"),
    ( 4, "6543210-9",  "Ana Sofia Morales Quinonez",             "5512-3344", "asmorales@gmail.com",       "12 Calle 5-60 Zona 14, Guatemala"),
    ( 5, "5432109-8",  "Constructora Cemento y Acero, S.A.",     "2445-9900", "admin@cemacero.gt",         "Km 9 Carretera al Atlantico"),
    ( 6, "4321098-7",  "Roberto Carlos Aguilar Hernandez",       "4478-2211", "rcaguilar@outlook.com",     "3a. Avenida 15-22 Zona 11, Guatemala"),
    ( 7, "3210987-6",  "Clinica Medica San Rafael",              "2367-1234", "recepcion@sanrafael.gt",    "6a. Avenida 9-18 Zona 9, Guatemala"),
    ( 8, "2109876-5",  "Restaurante La Cocina de Dona Luz",      "2288-5533", "contacto@donaluz.gt",       "2a. Calle 4-15 Zona 1, Guatemala"),
    ( 9, "1098765-4",  "Gabriela Maria Ortiz Lemus",             "5990-7766", "gabyortiz@gmail.com",       "Residenciales El Naranjo casa 42, Mixco"),
    (10, "1987654-3",  "Bufete Juridico Castillo y Asociados",   "2412-6688", "info@castilloasociados.gt", "Torre Empresarial 7o. nivel Zona 10, Guatemala"),
    (11, "2876543-1",  "Universidad Tecnica del Sur",            "7920-3300", "compras@utsur.edu.gt",      "Km 158 Carretera a Retalhuleu"),
    (12, "3765432-0",  "Jose Miguel Recinos Palma",              "4123-8899", "jmrecinos@yahoo.com",       "14 Avenida 3-44 Zona 7, Guatemala"),
    (13, "4654321-9",  "Agencia de Viajes Mundo Maya",           "2356-7711", "reservas@mundomaya.gt",     "7a. Avenida 11-30 Zona 4, Guatemala"),
    (14, "5543210-8",  "Distribuidora Farmaceutica Vida",        "2478-2020", "compras@farmavida.gt",      "Calzada San Juan 22-10 Zona 7, Guatemala"),
    (15, "6432109-7",  "Lucia Fernanda Barrios Tobar",           "5678-1122", "lfbarrios@gmail.com",       "Condominio Las Luces casa 18, Villa Nueva"),
    (16, "7321098-6",  "Cafeteria El Grano Dorado",              "2299-4455", "pedidos@granodorado.gt",    "1a. Calle 9-77 Zona 1, Guatemala"),
    (17, "8210987-5",  "Centro Educativo Nuevo Amanecer",        "2341-9988", "direccion@nuevoamanecer.gt","Colonia Primero de Julio, Mixco"),
    (18, "9109876-4",  "Edgar Alejandro Villatoro Mendez",       "4456-3377", "eavillatoro@hotmail.com",   "Calzada Aguilar Batres 34-12 Zona 12"),
    (19, "1029384-7",  "Inmobiliaria Torres del Bosque",         "2390-5566", "ventas@torresdelbosque.gt", "Boulevard Vista Hermosa 23-80 Zona 15"),
    (20, "2938475-6",  "Patricia Elena Guzman Rivas",            "5234-9900", "peguzman@gmail.com",        "8a. Calle 2-30 Zona 2, Guatemala"),
]

# ---------------------------------------------------------------------
#  Relacion N:M proveedor-producto:
#  cada producto lo suministran entre 2 y 3 proveedores distintos.
# ---------------------------------------------------------------------
proveedor_producto = {}        # (id_prov, id_prod) -> costo_referencia
productos_por_proveedor = {p[0]: [] for p in PROVEEDORES}

for pid, cod, nom, cat, costo, smin in PRODUCTOS:
    n = random.choice([2, 2, 3])
    provs = random.sample([p[0] for p in PROVEEDORES], n)
    for prov in provs:
        factor = Decimal(str(random.uniform(0.94, 1.08)))
        proveedor_producto[(prov, pid)] = q2(Decimal(costo) * factor)
        productos_por_proveedor[prov].append(pid)

# Ningun proveedor puede quedar sin productos asignados
for prov, lista in productos_por_proveedor.items():
    if not lista:
        pid = random.choice([p[0] for p in PRODUCTOS])
        proveedor_producto[(prov, pid)] = q2(Decimal(dict((p[0], p[4]) for p in PRODUCTOS)[pid]))
        lista.append(pid)


# =====================================================================
#  2. SIMULACION DE LA LINEA DE TIEMPO
# =====================================================================

costo_base = {p[0]: Decimal(p[4]) for p in PRODUCTOS}
precio_venta = {p[0]: q2(Decimal(p[4]) * MARGEN) for p in PRODUCTOS}

# Estado simulado
stock = {p[0]: 0 for p in PRODUCTOS}
capas = {p[0]: [] for p in PRODUCTOS}   # lista de dicts {id_capa, fecha, costo, inicial, disponible}

# Acumuladores de salida
compras, detalles_compra, capas_sql = [], [], []
ventas, detalles_venta, consumos = [], [], []
movimientos = []

seq = {"dc": 0, "dv": 0, "capa": 0, "consumo": 0, "mov": 0}

comprados_alguna_vez = set()   # cobertura del catalogo en las compras
vendidos_alguna_vez = set()    # cobertura del catalogo en las ventas


def nuevo(tipo):
    seq[tipo] += 1
    return seq[tipo]


# --- Construccion de la linea de tiempo -------------------------------
# Las 4 primeras compras ocurren antes de cualquier venta, para que el
# inventario tenga existencias con que operar.
INICIO = datetime(2026, 1, 8, 8, 30)

fechas_compra = []
for i in range(15):
    if i < 4:
        f = INICIO + timedelta(days=i * 3, hours=random.randint(0, 6))
    else:
        f = INICIO + timedelta(days=18 + (i - 4) * 17, hours=random.randint(0, 8))
    fechas_compra.append(f)

fechas_venta = []
for i in range(25):
    f = INICIO + timedelta(days=14 + i * 9, hours=random.randint(1, 9), minutes=random.randint(0, 59))
    fechas_venta.append(f)

eventos = ([("C", i + 1, f) for i, f in enumerate(fechas_compra)] +
           [("V", i + 1, f) for i, f in enumerate(fechas_venta)])
eventos.sort(key=lambda e: e[2])


def registrar_movimiento(id_prod, tipo, origen, cant, costo_u, id_compra, id_venta, id_usuario, fecha, obs):
    anterior = stock[id_prod]
    nueva = anterior + cant if tipo == "ENTRADA" else anterior - cant
    assert nueva >= 0, f"Existencia negativa en producto {id_prod}"
    stock[id_prod] = nueva
    movimientos.append({
        "id": nuevo("mov"), "prod": id_prod, "tipo": tipo, "origen": origen,
        "cant": cant, "costo": q4(costo_u), "ant": anterior, "nueva": nueva,
        "compra": id_compra, "venta": id_venta, "usuario": id_usuario,
        "fecha": fecha, "obs": obs,
    })


# --- Procesamiento cronologico ---------------------------------------
for tipo_ev, num, fecha in eventos:

    # ---------------- COMPRA ----------------
    if tipo_ev == "C":
        id_compra = num
        # Se prefiere un proveedor que aun tenga productos sin comprar,
        # de modo que el catalogo completo quede cubierto por el historial.
        con_pendientes = [p for p, l in productos_por_proveedor.items()
                          if set(l) - comprados_alguna_vez]
        if con_pendientes:
            id_prov = random.choice(con_pendientes)
        else:
            id_prov = random.choice([p for p, l in productos_por_proveedor.items() if l])
        disponibles = sorted(set(productos_por_proveedor[id_prov]))
        n_lineas = min(len(disponibles), random.choice([3, 4, 4, 5, 5, 6]))

        # Se da prioridad a los productos que aun no se han comprado nunca,
        # para que los 30 del catalogo tengan historial y los reportes de
        # inventario y compras cubran todo el catalogo.
        pendientes = [p for p in disponibles if p not in comprados_alguna_vez]
        prods = pendientes[:n_lineas]
        if len(prods) < n_lineas:
            resto = [p for p in disponibles if p not in prods]
            prods += random.sample(resto, min(len(resto), n_lineas - len(prods)))
        comprados_alguna_vez.update(prods)

        total = Decimal("0")
        for id_prod in prods:
            cant = random.randint(4, 18)
            base = proveedor_producto[(id_prov, id_prod)]
            # Variacion del costo entre compras: es lo que hace que UEPS
            # produzca un costo de ventas distinto al de PEPS.
            costo_u = q2(base * Decimal(str(random.uniform(0.93, 1.12))))
            id_dc = nuevo("dc")
            detalles_compra.append((id_dc, id_compra, id_prod, cant, costo_u))
            total += cant * costo_u

            id_capa = nuevo("capa")
            capas[id_prod].append({"id": id_capa, "fecha": fecha, "costo": q4(costo_u),
                                   "inicial": cant, "disponible": cant})
            capas_sql.append((id_capa, id_prod, id_dc, fecha, q4(costo_u), cant))

            registrar_movimiento(id_prod, "ENTRADA", "COMPRA", cant, costo_u,
                                 id_compra, None, 2, fecha,
                                 f"Ingreso por compra {id_compra:04d}")

        compras.append((id_compra, f"COM-2026-{id_compra:04d}", id_prov, 2, fecha, q2(total)))

    # ---------------- VENTA ----------------
    else:
        id_venta = num
        id_cliente = random.randint(1, 20)

        # Solo productos con existencia suficiente en ESTE instante:
        # es la regla que impide que una venta consuma stock inexistente.
        con_stock = sorted([p for p in stock if stock[p] >= 2])
        if not con_stock:
            continue
        n_lineas = min(len(con_stock), random.choice([1, 2, 2, 3, 3, 4]))

        # Prioridad a productos aun no vendidos, para dar cobertura al
        # catalogo en los reportes de ventas.
        pendientes = [p for p in con_stock if p not in vendidos_alguna_vez]
        prods = pendientes[:n_lineas]
        if len(prods) < n_lineas:
            resto = [p for p in con_stock if p not in prods]
            prods += random.sample(resto, min(len(resto), n_lineas - len(prods)))
        vendidos_alguna_vez.update(prods)

        subtotal = Decimal("0")
        lineas_validas = 0
        for id_prod in prods:
            maximo = min(stock[id_prod], 6)
            if maximo < 1:
                continue
            cant = random.randint(1, maximo)
            precio = precio_venta[id_prod]
            id_dv = nuevo("dv")

            # ---- CONSUMO DE CAPAS EN ORDEN UEPS (LIFO) ----
            # Se consume primero la capa mas reciente que aun tenga
            # unidades disponibles.
            pendiente = cant
            costo_acumulado = Decimal("0")
            capas_ordenadas = sorted(
                [c for c in capas[id_prod] if c["disponible"] > 0],
                key=lambda c: (c["fecha"], c["id"]),
                reverse=True                      # <-- UEPS. Para PEPS seria reverse=False
            )
            for capa in capas_ordenadas:
                if pendiente == 0:
                    break
                toma = min(pendiente, capa["disponible"])
                capa["disponible"] -= toma
                pendiente -= toma
                costo_acumulado += toma * capa["costo"]
                consumos.append((nuevo("consumo"), capa["id"], id_dv, toma, capa["costo"]))
            assert pendiente == 0, f"Capas insuficientes para producto {id_prod}"

            costo_unit = q4(costo_acumulado / cant)
            detalles_venta.append((id_dv, id_venta, id_prod, cant, precio, costo_unit))
            subtotal += cant * precio
            lineas_validas += 1

            registrar_movimiento(id_prod, "SALIDA", "VENTA", cant, costo_unit,
                                 None, id_venta, 4, fecha,
                                 f"Salida por venta {id_venta:04d} (UEPS)")

        if lineas_validas == 0:
            continue

        sub = q2(subtotal)
        iva = q2(sub * IVA)
        ventas.append((id_venta, f"FAC-2026-{id_venta:05d}", id_cliente, 4, fecha, sub, iva, q2(sub + iva)))


# =====================================================================
#  3. VERIFICACION INTERNA ANTES DE EMITIR EL SQL
# =====================================================================
for id_prod in stock:
    en_capas = sum(c["disponible"] for c in capas[id_prod])
    assert en_capas == stock[id_prod], (
        f"Descuadre producto {id_prod}: capas={en_capas} stock={stock[id_prod]}")

entradas = sum(m["cant"] for m in movimientos if m["tipo"] == "ENTRADA")
salidas = sum(m["cant"] for m in movimientos if m["tipo"] == "SALIDA")
assert entradas - salidas == sum(stock.values())

assert len(compras) == 15, "El enunciado exige 15 compras"
assert len(ventas) == 25,  "El enunciado exige 25 ventas"
assert all(len([d for d in detalles_compra if d[1] == c[0]]) >= 1 for c in compras)
assert all(len([d for d in detalles_venta if d[1] == v[0]]) >= 1 for v in ventas)
assert len(comprados_alguna_vez) == 30, f"Solo {len(comprados_alguna_vez)}/30 productos con compras"
assert len(vendidos_alguna_vez) == 30, f"Solo {len(vendidos_alguna_vez)}/30 productos con ventas"

print(f"Compras generadas : {len(compras)}")
print(f"Ventas generadas  : {len(ventas)}")
print(f"Lineas de compra  : {len(detalles_compra)}")
print(f"Lineas de venta   : {len(detalles_venta)}")
print(f"Movimientos kardex: {len(movimientos)}")
print(f"Capas de costo    : {len(capas_sql)}")
print(f"Consumos de capa  : {len(consumos)}")
print(f"Unidades en stock : {sum(stock.values())}")
bajo_minimo = [p[1] for p in PRODUCTOS if stock[p[0]] <= p[5]]
print(f"Productos en alerta de stock: {len(bajo_minimo)} -> {bajo_minimo}")


# =====================================================================
#  4. EMISION DEL SCRIPT SQL
# =====================================================================
L = []
w = L.append

w("-- =====================================================================")
w("--  MINI ERP - Sistema de Gestion Comercial")
w("--  Migracion V2 : Carga inicial de datos")
w("-- =====================================================================")
w("--  Generado por generar_seed.py (semilla fija: los datos son")
w("--  reproducibles y la migracion nunca cambia de checksum).")
w("--")
w("--  Contenido, segun lo exigido por el enunciado:")
w(f"--    * 4 usuarios, uno por cada area funcional")
w(f"--    * {len(PRODUCTOS)} productos distribuidos en {len(CATEGORIAS)} categorias")
w(f"--    * {len(PROVEEDORES)} proveedores")
w(f"--    * {len(CLIENTES)} clientes")
w(f"--    * {len(compras)} compras, cada una con una o mas lineas de detalle")
w(f"--    * {len(ventas)} ventas, cada una con una o mas lineas de detalle")
w("--")
w("--  Las compras y ventas se generaron simulando la linea de tiempo")
w("--  real del negocio con valuacion UEPS: cada salida consume las")
w("--  capas de costo mas recientes disponibles en esa fecha. Por eso")
w("--  el kardex, las capas de costo y producto.stock_actual cuadran")
w("--  exactamente entre si.")
w("--")
w("--  Credenciales de acceso (hash BCrypt, costo 10):")
for _, u, clave, nombre, _, _ in USUARIOS:
    w(f"--    {u:<12} / {clave:<14} -> {nombre}")
w("-- =====================================================================")
w("")

w("-- ---------------------------------------------------------------------")
w("--  Roles")
w("-- ---------------------------------------------------------------------")
w("INSERT INTO rol (id_rol, nombre, descripcion) VALUES")
w(",\n".join(f"    ({i}, '{n}', '{esc(d)}')" for i, n, d in ROLES) + ";")
w("")

w("-- ---------------------------------------------------------------------")
w("--  Usuarios (uno por cada area funcional)")
w("-- ---------------------------------------------------------------------")
w("INSERT INTO usuario (id_usuario, nombre_usuario, contrasena, nombre_completo, correo, id_rol) VALUES")
filas = []
for i, u, clave, nombre, correo, rol in USUARIOS:
    filas.append(f"    ({i}, '{u}', '{hashear(clave)}', '{esc(nombre)}', '{correo}', {rol})")
w(",\n".join(filas) + ";")
w("")

w("-- ---------------------------------------------------------------------")
w("--  Categorias")
w("-- ---------------------------------------------------------------------")
w("INSERT INTO categoria (id_categoria, nombre, descripcion) VALUES")
w(",\n".join(f"    ({i}, '{esc(n)}', '{esc(d)}')" for i, n, d in CATEGORIAS) + ";")
w("")

w("-- ---------------------------------------------------------------------")
w("--  Productos")
w("--  stock_actual se inicializa en 0: las existencias se construyen")
w("--  exclusivamente a partir de las compras y ventas registradas mas")
w("--  abajo, y se consolidan al final de este script.")
w("-- ---------------------------------------------------------------------")
w("INSERT INTO producto (id_producto, codigo, nombre, descripcion, id_categoria, unidad_medida, precio_venta, stock_actual, stock_minimo) VALUES")
filas = []
cat_nombre = {c[0]: c[1] for c in CATEGORIAS}
for pid, cod, nom, cat, costo, smin in PRODUCTOS:
    desc = f"{nom}. Categoria: {cat_nombre[cat]}."
    filas.append(f"    ({pid}, '{cod}', '{esc(nom)}', '{esc(desc)}', {cat}, 'UNIDAD', {precio_venta[pid]}, 0, {smin})")
w(",\n".join(filas) + ";")
w("")

w("-- ---------------------------------------------------------------------")
w("--  Proveedores")
w("-- ---------------------------------------------------------------------")
w("INSERT INTO proveedor (id_proveedor, nit, nombre, contacto, telefono, correo, direccion) VALUES")
filas = [f"    ({i}, '{nit}', '{esc(n)}', '{esc(c)}', '{t}', '{co}', '{esc(d)}')"
         for i, nit, n, c, t, co, d in PROVEEDORES]
w(",\n".join(filas) + ";")
w("")

w("-- ---------------------------------------------------------------------")
w("--  Relacion proveedor-producto (N:M)")
w("--  Un proveedor suministra varios productos y un mismo producto")
w("--  puede adquirirse a varios proveedores.")
w("-- ---------------------------------------------------------------------")
w("INSERT INTO proveedor_producto (id_proveedor, id_producto, costo_referencia) VALUES")
filas = [f"    ({p}, {pr}, {c})" for (p, pr), c in sorted(proveedor_producto.items())]
w(",\n".join(filas) + ";")
w("")

w("-- ---------------------------------------------------------------------")
w("--  Clientes")
w("-- ---------------------------------------------------------------------")
w("INSERT INTO cliente (id_cliente, nit, nombre, telefono, correo, direccion) VALUES")
filas = [f"    ({i}, '{nit}', '{esc(n)}', '{t}', '{co}', '{esc(d)}')"
         for i, nit, n, t, co, d in CLIENTES]
w(",\n".join(filas) + ";")
w("")

w("-- ---------------------------------------------------------------------")
w("--  Compras")
w("-- ---------------------------------------------------------------------")
w("INSERT INTO compra (id_compra, numero_documento, id_proveedor, id_usuario, fecha_compra, total, observaciones) VALUES")
filas = [f"    ({i}, '{doc}', {prov}, {usr}, TIMESTAMP '{f:%Y-%m-%d %H:%M:%S}', {tot}, 'Compra de abastecimiento')"
         for i, doc, prov, usr, f, tot in sorted(compras)]
w(",\n".join(filas) + ";")
w("")

w("INSERT INTO detalle_compra (id_detalle_compra, id_compra, id_producto, cantidad, costo_unitario) VALUES")
filas = [f"    ({i}, {c}, {p}, {cant}, {costo})" for i, c, p, cant, costo in detalles_compra]
w(",\n".join(filas) + ";")
w("")

w("-- ---------------------------------------------------------------------")
w("--  Ventas")
w("-- ---------------------------------------------------------------------")
w("INSERT INTO venta (id_venta, numero_factura, id_cliente, id_usuario, fecha_venta, subtotal, porcentaje_iva, iva, total) VALUES")
filas = [f"    ({i}, '{doc}', {cli}, {usr}, TIMESTAMP '{f:%Y-%m-%d %H:%M:%S}', {sub}, 0.1200, {iva}, {tot})"
         for i, doc, cli, usr, f, sub, iva, tot in sorted(ventas)]
w(",\n".join(filas) + ";")
w("")

w("INSERT INTO detalle_venta (id_detalle_venta, id_venta, id_producto, cantidad, precio_unitario, costo_unitario) VALUES")
filas = [f"    ({i}, {v}, {p}, {cant}, {precio}, {costo})" for i, v, p, cant, precio, costo in detalles_venta]
w(",\n".join(filas) + ";")
w("")

w("-- ---------------------------------------------------------------------")
w("--  Capas de costo (valuacion UEPS)")
w("--  cantidad_disponible refleja el saldo de cada capa despues de")
w("--  aplicar todas las ventas de la carga inicial.")
w("-- ---------------------------------------------------------------------")
w("INSERT INTO capa_inventario (id_capa, id_producto, id_detalle_compra, fecha_entrada, costo_unitario, cantidad_inicial, cantidad_disponible) VALUES")
saldo_capa = {}
for prod_capas in capas.values():
    for c in prod_capas:
        saldo_capa[c["id"]] = c["disponible"]
filas = [f"    ({i}, {p}, {dc}, TIMESTAMP '{f:%Y-%m-%d %H:%M:%S}', {costo}, {ini}, {saldo_capa[i]})"
         for i, p, dc, f, costo, ini in capas_sql]
w(",\n".join(filas) + ";")
w("")

w("-- ---------------------------------------------------------------------")
w("--  Consumo de capas: trazabilidad de que capa alimento cada venta")
w("-- ---------------------------------------------------------------------")
w("INSERT INTO consumo_capa (id_consumo, id_capa, id_detalle_venta, cantidad, costo_unitario) VALUES")
filas = [f"    ({i}, {capa}, {dv}, {cant}, {costo})" for i, capa, dv, cant, costo in consumos]
w(",\n".join(filas) + ";")
w("")

w("-- ---------------------------------------------------------------------")
w("--  Kardex: movimientos de inventario en orden cronologico")
w("-- ---------------------------------------------------------------------")
w("INSERT INTO movimiento_inventario (id_movimiento, id_producto, tipo_movimiento, origen, cantidad, costo_unitario, existencia_anterior, existencia_nueva, id_compra, id_venta, id_usuario, fecha_movimiento, observaciones) VALUES")
filas = []
for m in movimientos:
    c = m["compra"] if m["compra"] else "NULL"
    v = m["venta"] if m["venta"] else "NULL"
    filas.append(f"    ({m['id']}, {m['prod']}, '{m['tipo']}', '{m['origen']}', {m['cant']}, "
                 f"{m['costo']}, {m['ant']}, {m['nueva']}, {c}, {v}, {m['usuario']}, "
                 f"TIMESTAMP '{m['fecha']:%Y-%m-%d %H:%M:%S}', '{esc(m['obs'])}')")
w(",\n".join(filas) + ";")
w("")

w("-- ---------------------------------------------------------------------")
w("--  Consolidacion de existencias")
w("--  El stock de cada producto se deriva del kardex, no se escribe a")
w("--  mano: si el historial de movimientos es correcto, las existencias")
w("--  tambien lo son.")
w("-- ---------------------------------------------------------------------")
w("UPDATE producto p")
w("SET    stock_actual = COALESCE((")
w("           SELECT SUM(CASE WHEN m.tipo_movimiento = 'ENTRADA' THEN  m.cantidad")
w("                           WHEN m.tipo_movimiento = 'SALIDA'  THEN -m.cantidad")
w("                      END)")
w("           FROM   movimiento_inventario m")
w("           WHERE  m.id_producto = p.id_producto")
w("       ), 0);")
w("")

w("-- ---------------------------------------------------------------------")
w("--  Bitacora: registro del proceso de carga inicial")
w("-- ---------------------------------------------------------------------")
w("INSERT INTO bitacora (id_usuario, nombre_usuario, modulo, accion, entidad_afectada, descripcion) VALUES")
w("    (1, 'admin', 'USUARIOS',    'CREAR', 'usuario',   'Carga inicial: 4 usuarios, uno por area funcional'),")
w("    (1, 'admin', 'CATEGORIAS',  'CREAR', 'categoria', 'Carga inicial: 8 categorias de producto'),")
w("    (3, 'minventario', 'PRODUCTOS', 'CREAR', 'producto', 'Carga inicial: 30 productos del catalogo'),")
w("    (2, 'jcompras', 'PROVEEDORES', 'CREAR', 'proveedor', 'Carga inicial: 8 proveedores'),")
w("    (4, 'cventas', 'CLIENTES',   'CREAR', 'cliente',   'Carga inicial: 20 clientes'),")
w(f"    (2, 'jcompras', 'COMPRAS', 'CREAR', 'compra', 'Carga inicial: {len(compras)} compras registradas'),")
w(f"    (4, 'cventas', 'VENTAS',   'CREAR', 'venta',  'Carga inicial: {len(ventas)} ventas registradas'),")
w(f"    (3, 'minventario', 'INVENTARIO', 'CREAR', 'movimiento_inventario', 'Carga inicial: {len(movimientos)} movimientos de kardex con valuacion UEPS');")
w("")

w("-- ---------------------------------------------------------------------")
w("--  Sincronizacion de las secuencias de identidad")
w("--  Obligatorio: la carga inicial inserto identificadores explicitos,")
w("--  lo que no avanza los generadores IDENTITY. Sin este paso, el")
w("--  primer INSERT desde la aplicacion chocaria con una llave duplicada.")
w("-- ---------------------------------------------------------------------")
tablas_pk = [
    ("rol", "id_rol"), ("usuario", "id_usuario"), ("categoria", "id_categoria"),
    ("producto", "id_producto"), ("proveedor", "id_proveedor"), ("cliente", "id_cliente"),
    ("compra", "id_compra"), ("detalle_compra", "id_detalle_compra"),
    ("venta", "id_venta"), ("detalle_venta", "id_detalle_venta"),
    ("capa_inventario", "id_capa"), ("consumo_capa", "id_consumo"),
    ("movimiento_inventario", "id_movimiento"), ("bitacora", "id_bitacora"),
]
w("DO $$")
w("DECLARE")
w("    r RECORD;")
w("BEGIN")
w("    FOR r IN")
w("        SELECT * FROM (VALUES")
w(",\n".join(f"            ('{t}', '{c}')" for t, c in tablas_pk))
w("        ) AS t(tabla, columna)")
w("    LOOP")
w("        EXECUTE format(")
w("            'SELECT setval(pg_get_serial_sequence(%L, %L), COALESCE((SELECT MAX(%I) FROM %I), 1))',")
w("            r.tabla, r.columna, r.columna, r.tabla);")
w("    END LOOP;")
w("END $$;")
w("")

with open("db/migration/V2__datos_iniciales.sql", "w", encoding="utf-8") as fh:
    fh.write("\n".join(L))

print("\nArchivo generado: db/migration/V2__datos_iniciales.sql")
