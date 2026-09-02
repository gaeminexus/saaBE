-- =====================================================================================
-- PREVUELO DEL DESEMBOLSO DE PRESTAMOS — fase 0 de PLAN-DESEMBOLSO-PRESTAMO.md
-- FECHA: 2026-09-01   EQUIPO: CRD / EQUIPO B (ciclo del credito y seguros)
--
-- ⚠️ ESTE SCRIPT NO ESCRIBE NADA. Son SELECT y nada mas. Se puede correr en
--    produccion en cualquier momento, tambien en horario laboral.
--
-- QUE RESPONDE, y las cuatro cosas hay que saberlas ANTES de escribir una linea de Java:
--   Bloque 1 — Que PRSTIDPG no exista todavia en CRD.PRST.
--   Bloque 2 — CUAL es el producto de pago que apunta a 2.3.90.90.10 SOCIOS POR PAGAR.
--              Es el dato que hace que el asiento de bancos salga bien, y no se puede
--              inventar: con el producto equivocado, tesoreria paga y el asiento descarga
--              una cuenta que no es, CUADRADO IGUAL y sin ningun error.
--   Bloque 3 — Como esta armado un pago de origen externo que YA funciona (los de
--              devolucion de aportes), para copiar la forma en vez de deducirla.
--   Bloque 4 — Que productos de credito hay, para saber cuantos quedan sin plantilla.
--
-- ⚠️ NOTA SOBRE LOS NOMBRES DE PGS.PRDP: esa tabla NO sigue la convencion de 8 caracteres
--    del resto del sistema. Sus columnas se llaman ID, NOMBRE, CODIGO, GRUPOPRODUCTO,
--    ESTADO — verificado en com.saa.model.cxp.ProductoPago. No "corregir" esos nombres al
--    leer el script: son asi en la base.
--
-- COMO DEVOLVER EL RESULTADO: pegar la salida completa de los cuatro bloques.
-- =====================================================================================

SET PAGESIZE 200
SET LINESIZE 230


-- =====================================================================================
-- BLOQUE 1 — PRSTIDPG no debe existir todavia
--
-- Esperado: 0 filas. Si aparece, alguien ya agrego la columna y hay que mirar por que
-- antes de correr el ALTER.
-- =====================================================================================
SELECT  c.OWNER, c.TABLE_NAME, c.COLUMN_NAME, c.DATA_TYPE, c.NULLABLE
FROM    ALL_TAB_COLUMNS c
WHERE   c.OWNER = 'CRD'
AND     c.TABLE_NAME = 'PRST'
AND     c.COLUMN_NAME = 'PRSTIDPG';

-- 1.b El patron a copiar: la columna equivalente de la devolucion de aportes, que ya
--     alimenta el mismo circuito. Esperado: 1 fila, NUMBER, nullable.
SELECT  c.TABLE_NAME, c.COLUMN_NAME, c.DATA_TYPE, c.NULLABLE
FROM    ALL_TAB_COLUMNS c
WHERE   c.OWNER = 'CRD'
AND     c.TABLE_NAME = 'DVAP'
AND     c.COLUMN_NAME = 'DVAPIDPG';


-- =====================================================================================
-- BLOQUE 2 — EL DATO QUE IMPORTA: el producto de pago de SOCIOS POR PAGAR
--
-- CXP arma el asiento del pago con UNA LINEA DEBE POR PRODUCTO, usando la cuenta
-- contable del GRUPO del producto, y una linea HABER al banco por el total.
-- Para que el desembolso descargue 2.3.90.90.10 —la misma cuenta que el asiento de
-- entrega acredita— hace falta el producto cuyo grupo apunte a esa cuenta.
--
-- Como leerlo:
--   * UNA fila     -> ese ID es el que va en el desglose. Anotarlo.
--   * VARIAS filas -> hay que elegir, y no lo elige el agente: AVISAR con la lista.
--   * NINGUNA      -> hay que CREAR el producto de pago (y quiza su grupo) antes de
--                     implementar. AVISAR: cambia el alcance de la fase 1.
-- =====================================================================================
SELECT  pp.ID                                      AS ID_PRODUCTO_PAGO,
        pp.NOMBRE                                  AS PRODUCTO,
        pp.CODIGO                                  AS CODIGO_PRODUCTO,
        pp.ESTADO                                  AS ESTADO_PRODUCTO,
        gp.GRPPCDGO                                AS ID_GRUPO,
        gp.GRPPNMBR                                AS GRUPO,
        c.PLNNCDGO                                 AS ID_CUENTA,
        c.PLNNCNTA                                 AS CUENTA,
        c.PLNNNMBR                                 AS NOMBRE_CUENTA
FROM    PGS.PRDP pp
JOIN    PGS.GRPP gp ON gp.GRPPCDGO = pp.GRUPOPRODUCTO
JOIN    CNT.PLNN c  ON c.PLNNCDGO  = gp.PLNNCDGO
WHERE   c.PLNNCNTA = '2.3.90.90.10'
ORDER   BY pp.ID;

-- 2.b Si el bloque 2 vino vacio: todos los productos de pago activos con su cuenta, para
--     ver si hay alguno equivalente con otro nombre. Puede ser una lista larga.
SELECT  pp.ID                                      AS ID_PRODUCTO_PAGO,
        pp.NOMBRE                                  AS PRODUCTO,
        gp.GRPPNMBR                                AS GRUPO,
        c.PLNNCNTA                                 AS CUENTA,
        c.PLNNNMBR                                 AS NOMBRE_CUENTA
FROM    PGS.PRDP pp
LEFT    JOIN PGS.GRPP gp ON gp.GRPPCDGO = pp.GRUPOPRODUCTO
LEFT    JOIN CNT.PLNN c  ON c.PLNNCDGO  = gp.PLNNCDGO
WHERE   pp.ESTADO = 1
ORDER   BY c.PLNNCNTA NULLS LAST, pp.ID;


-- =====================================================================================
-- BLOQUE 3 — Un pago de origen externo que YA funciona, con su desglose
--
-- Sirve para copiar la forma real en vez de deducirla del codigo: que valores llevan
-- PGTRORGN / PGTRIDOR, con que estado nacen, y como se ve el desglose en PGS.DPGT.
--
-- Como leerlo: si no hay ninguna fila, todavia no se registro ningun pago de origen
-- externo en esta base y no hay ejemplo que copiar. No bloquea, pero conviene saberlo.
-- =====================================================================================
SELECT  t.PGTRCDGO                                 AS ID_PAGO,
        t.PGTRORGN                                 AS ORIGEN,
        t.PGTRIDOR                                 AS ID_ORIGEN,
        t.PGTRESTD                                 AS ESTADO_PAGO,
        t.PGTRVLOR                                 AS VALOR,
        t.PGTRASNT                                 AS ASIENTO,
        TO_CHAR(t.PGTRFPRG, 'YYYY-MM-DD')          AS FECHA_PROGRAMADA
FROM    PGS.PGTR t
WHERE   t.PGTRORGN IS NOT NULL
ORDER   BY t.PGTRCDGO DESC
FETCH FIRST 10 ROWS ONLY;

-- 3.b El desglose de esos pagos: los pares (producto, valor) que CXP convierte en el
--     asiento. Es la forma exacta que tiene que armar el desembolso.
SELECT  d.PGTRCDGO                                 AS ID_PAGO,
        d.DPGTPRDP                                 AS ID_PRODUCTO_PAGO,
        pp.NOMBRE                                  AS PRODUCTO,
        d.DPGTVLRR                                 AS VALOR,
        d.DPGTCNCP                                 AS CONCEPTO
FROM    PGS.DPGT d
LEFT    JOIN PGS.PRDP pp ON pp.ID = d.DPGTPRDP
WHERE   d.PGTRCDGO IN (SELECT t.PGTRCDGO FROM PGS.PGTR t
                        WHERE t.PGTRORGN IS NOT NULL
                        ORDER BY t.PGTRCDGO DESC
                        FETCH FIRST 10 ROWS ONLY)
ORDER   BY d.PGTRCDGO DESC, d.DPGTPRDP;


-- =====================================================================================
-- BLOQUE 4 — Productos de credito, para saber cuantos quedan sin plantilla de entrega
--
-- Hoy hay plantilla para prendario (alterno 9) e hipotecario (13), y se creo la de
-- quirografario (34, script 156, sin ejecutar).
--
-- Como leerlo: si aparece un producto de credito que no sea de esos tres, el desembolso
-- lo va a RECHAZAR con un mensaje — por diseno, no se elige una plantilla por defecto.
-- Si esos productos tienen CARTERA_VIVA > 0, hay que decidir que plantilla les
-- corresponde ANTES de que alguien intente otorgar uno.
-- =====================================================================================
SELECT  p.PRDCCDGO                                 AS ID_PRODUCTO,
        p.PRDCNMBR                                 AS PRODUCTO,
        p.PRDCCDPT                                 AS CODIGO_PETRO,
        p.PRDCESTD                                 AS ESTADO,
        (SELECT COUNT(*) FROM CRD.PRST pr
          WHERE pr.PRDCCDGO = p.PRDCCDGO
            AND pr.PRSTIDST IN (2, 8, 11))         AS CARTERA_VIVA
FROM    CRD.PRDC p
ORDER   BY p.PRDCCDGO;


-- =====================================================================================
-- FIN. Pegar la salida de los cuatro bloques.
-- =====================================================================================
