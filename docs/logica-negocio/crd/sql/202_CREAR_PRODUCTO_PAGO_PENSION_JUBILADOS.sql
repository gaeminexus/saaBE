-- =====================================================================================
-- 202 - CREAR el grupo y el producto de pago de PENSION A JUBILADOS (H41)
-- FECHA: 2026-09-05 · EQUIPO: CRD / Equipo B (eqB, omen-saa-1)
--
-- ⛔ ESTE SCRIPT SI ESCRIBE. Leer los bloques 0 antes de correr nada.
-- Sin comandos de SQL*Plus: no usa PROMPT, DEFINE, SET ni &variables.
--
-- Complementa al 201, que es el de diagnostico. El 201 ya se corrio y dio:
--   Producto 516 (molde)  -> grupo 52, cuenta 2.3.90.90.06, empresa 1236, todo lo demas 0
--   Grupo 43 "DEVOLUCION PENSIONES COMPLEMENTARIAS" -> YA apunta a 2.3.01.10.03 (id 10364)
--   Producto 411 "DEVOLUCION PENSIONES COMPLEMENTARIAS" -> ya cuelga de ese grupo
--
-- =====================================================================================
-- ⛔⛔ DOS TRAMPAS DE PK, Y SON OPUESTAS. Por eso el bloque 0 no es opcional.
-- =====================================================================================
--
--   PGS.GRPP  ->  @GeneratedValue(SEQUENCE, "PGS.SQ_GRPPCDGO")
--                 HAY que dar el PK, con la secuencia. Si no se da: ORA-01400, que es
--                 exactamente lo que paso el 2026-09-05 con CNT.DTPL.
--
--   PGS.PRDP  ->  @GeneratedValue(IDENTITY)
--                 NO hay que dar el PK. Oracle lo asigna solo. Si se da y la columna es
--                 GENERATED ALWAYS: ORA-32795, "cannot insert into a generated always
--                 identity column".
--
-- Son la misma familia de error en las dos direcciones: dar el PK donde no va y no darlo
-- donde va. El bloque 0.A confirma cual es cual EN ESTA BASE antes de que importe.
--
-- ⚠️ Por eso el producto NO se inserta con un ID fijo aunque nadie mas este trabajando:
-- no es cuestion de concurrencia, es que la columna puede rechazarlo. El ID real se lee
-- despues, en el bloque 3.
--
-- Ningun valor de este script esta escrito a mano: el grupo se copia del 43 (que ya tiene
-- la cuenta correcta) y el producto se copia del 516 (el unico molde que ya funciono en
-- produccion). Solo cambian el nombre y la cuenta/grupo.
-- =====================================================================================


-- ==========================================================================
-- BLOQUE 0.A - (!) CONTROL PREVIO: como se generan los PK en esta base
-- ==========================================================================

SELECT 'PGS.PRDP.ID' AS OBJETO,
       i.GENERATION_TYPE                       AS TIPO_IDENTITY,
       'omitir el ID en el INSERT'             AS QUE_HACER
  FROM ALL_TAB_IDENTITY_COLS i
 WHERE i.OWNER = 'PGS' AND i.TABLE_NAME = 'PRDP' AND i.COLUMN_NAME = 'ID';

SELECT 'PGS.SQ_GRPPCDGO' AS OBJETO,
       s.LAST_NUMBER                           AS PROXIMO_APROX,
       'usar NEXTVAL en el INSERT'             AS QUE_HACER
  FROM ALL_SEQUENCES s
 WHERE s.SEQUENCE_OWNER = 'PGS' AND s.SEQUENCE_NAME = 'SQ_GRPPCDGO';

--
-- COMO SE LEE:
-- (!) La PRIMERA consulta devuelve fila -> PRDP.ID es IDENTITY. Correr el bloque 2 tal
--     como esta (sin ID). Es lo esperado.
-- (!) La PRIMERA consulta NO devuelve fila -> PRDP.ID NO es identity en esta base. PARAR
--     y avisar: el INSERT del bloque 2 fallaria con ORA-01400 y habria que darle un ID.
-- (!) La SEGUNDA no devuelve fila -> la secuencia no existe con ese nombre. PARAR: el
--     bloque 1 fallaria. Avisar antes de improvisar otro nombre.
--

-- ==========================================================================
-- BLOQUE 0.B - (!) No exista ya lo que vamos a crear
-- ==========================================================================
-- Correr el script dos veces por error crearia un duplicado silencioso, y despues habria
-- dos productos igual de validos apuntando a la misma cuenta.

SELECT g.GRPPCDGO AS ID_GRUPO, g.GRPPNMBR AS NOMBRE, 'GRUPO' AS QUE_ES
  FROM PGS.GRPP g
 WHERE UPPER(g.GRPPNMBR) LIKE '%PENSION COMPLEMENTARIA JUBILADOS%'
UNION ALL
SELECT p.ID, p.NOMBRE, 'PRODUCTO'
  FROM PGS.PRDP p
 WHERE UPPER(p.NOMBRE) LIKE '%PENSION COMPLEMENTARIA JUBILADOS%';

--
-- (!) Se espera CERO filas. Si devuelve algo, ya se creo antes: no volver a correr, ir
--     directo al bloque 3 y tomar ese ID.
--

-- ==========================================================================
-- BLOQUE 1 - Crear el GRUPO, copiando el 43 (que ya tiene la cuenta correcta)
-- ==========================================================================
-- Se copian TODAS las columnas del grupo 43 —rubros, sustento tributario, empresa,
-- estado— y solo se cambian el PK y el nombre. Asi no hay ningun valor adivinado.
-- La cuenta (PLNNCDGO = 10364 = 2.3.01.10.03) viene heredada del 43, que es justo la
-- que queremos.

INSERT INTO PGS.GRPP (GRPPCDGO, GRPPNMBR, GRPPRYYA, GRPPRZZA, PLNNCDGO,
                      GRPPCSUS, GRPPESTD, PJRQCDGO)
SELECT PGS.SQ_GRPPCDGO.NEXTVAL,
       'PENSION COMPLEMENTARIA JUBILADOS',
       g.GRPPRYYA,
       g.GRPPRZZA,
       g.PLNNCDGO,
       g.GRPPCSUS,
       g.GRPPESTD,
       g.PJRQCDGO
  FROM PGS.GRPP g
 WHERE g.GRPPCDGO = 43;

-- ==========================================================================
-- BLOQUE 2 - Crear el PRODUCTO, copiando el 516 (el molde probado)
-- ==========================================================================
-- Se copian todas las columnas del 516 —precio, IVA, ICE, descuento, subsidio, IRBPNR,
-- stock, unidad, codigo, empresa, estado— y solo cambian el nombre y el grupo.
-- ⛔ El ID NO se lista: lo asigna Oracle (IDENTITY). Ver bloque 0.A.

INSERT INTO PGS.PRDP (EMPRESA, GRUPOPRODUCTO, NOMBRE, CODIGO, CODIGOAUX,
                      PRECIOUNITARIO, DESCUENTO, TIPODESCUENTO, INCLUYEIVA, TIPOIVA,
                      TIPOICE, ICE, DESCRIPCION, SUBSIDIO, PRECIOSINSUB, IRBPNR,
                      MULTIPRECIO, STOCK, MANEJAUNIDAD, UNIDAD, ESTADO)
SELECT p.EMPRESA,
       (SELECT g.GRPPCDGO FROM PGS.GRPP g
         WHERE g.GRPPNMBR = 'PENSION COMPLEMENTARIA JUBILADOS'),
       'Pension Complementaria Jubilados',
       p.CODIGO,
       p.CODIGOAUX,
       p.PRECIOUNITARIO,
       p.DESCUENTO,
       p.TIPODESCUENTO,
       p.INCLUYEIVA,
       p.TIPOIVA,
       p.TIPOICE,
       p.ICE,
       'Pago mensual de pension complementaria a jubilados - corrida CRD',
       p.SUBSIDIO,
       p.PRECIOSINSUB,
       p.IRBPNR,
       p.MULTIPRECIO,
       p.STOCK,
       p.MANEJAUNIDAD,
       p.UNIDAD,
       p.ESTADO
  FROM PGS.PRDP p
 WHERE p.ID = 516;

COMMIT;

-- ==========================================================================
-- BLOQUE 3 - (!) CONTROL POSTERIOR: leer el ID que asigno Oracle
-- ==========================================================================
-- Este es el numero que hay que pasarle al equipo: va como constante
-- ID_PRODUCTO_PAGO_PENSION_JUBILADOS en el codigo, igual que el 516.

SELECT p.ID                     AS ID_PRODUCTO_NUEVO,
       p.NOMBRE                 AS NOMBRE,
       p.CODIGO                 AS CODIGO,
       p.EMPRESA                AS ID_EMPRESA,
       p.ESTADO                 AS ESTADO,
       p.GRUPOPRODUCTO          AS ID_GRUPO,
       g.GRPPNMBR               AS GRUPO_NOMBRE,
       n.PLNNCNTA               AS CUENTA_CONTABLE,
       n.PLNNNMBR               AS CUENTA_NOMBRE,
       p.PRECIOUNITARIO         AS PRECIO,
       p.INCLUYEIVA             AS INCLUYE_IVA,
       p.TIPOIVA                AS TIPO_IVA,
       p.TIPOICE                AS TIPO_ICE
  FROM PGS.PRDP p
  JOIN PGS.GRPP g ON g.GRPPCDGO = p.GRUPOPRODUCTO
  LEFT JOIN CNT.PLNN n ON n.PLNNCDGO = g.PLNNCDGO
 WHERE p.NOMBRE = 'Pension Complementaria Jubilados';

--
-- (!) TIENE que decir CUENTA_CONTABLE = 2.3.01.10.03 y CUENTA_NOMBRE = PENSIONES
--     COMPLEMENTARIAS POR PAGAR. Si dice otra cosa, el producto quedo colgado del grupo
--     equivocado: correr el bloque 4 y avisar.
-- (!) ID_EMPRESA tiene que ser 1236 y ESTADO 1, igual que el 516.
-- (!) Precio, IVA e ICE en 0, igual que el 516.
-- (!) ⭐ ANOTAR ID_PRODUCTO_NUEVO y pasarlo al equipo. Sin ese numero la corrida no
--     puede salir: la constante quedo en null a proposito y el guard #5 frena el proceso
--     antes de tocar al primer jubilado.
--

-- ==========================================================================
-- BLOQUE 4 - REVERSO, por si algo salio mal
-- ==========================================================================
-- Solo si el bloque 3 mostro algo distinto a lo esperado. Borra en el orden correcto:
-- primero el producto, despues el grupo (el producto tiene FK al grupo).
--
-- ⛔ NO correr si la corrida de jubilados ya uso el producto: habria pagos apuntando a
--    un producto inexistente. En ese caso avisar en vez de borrar.
--
-- DELETE FROM PGS.PRDP WHERE NOMBRE = 'Pension Complementaria Jubilados';
-- DELETE FROM PGS.GRPP WHERE GRPPNMBR = 'PENSION COMPLEMENTARIA JUBILADOS';
-- COMMIT;

-- =====================================================================================
-- Nota sobre por que se crea un producto nuevo y no se reusa el 411:
-- El 411 "DEVOLUCION PENSIONES COMPLEMENTARIAS" apunta a la misma cuenta y
-- contablemente serviria igual. Pero pertenece al proceso de DEVOLUCION DE APORTES
-- (DevolucionAporteServiceImpl:506-515 arma su desglose con el). Compartirlo dejaria el
-- pago mensual a jubilados y las devoluciones indistinguibles en cualquier reporte
-- agrupado por producto: el asiento saldria bien y la etiqueta mentiria.
--
-- El GRUPO 43 en cambio si se reusa como molde de la cuenta, pero se crea uno propio
-- para que el nombre tampoco mienta un nivel mas arriba.
-- =====================================================================================
