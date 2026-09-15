-- =====================================================================================
-- 225 — SEGUIMIENTO del 223: devoluciones 42 y 22 (LOPEZ VILLALVA HENRRY ALFONSO),
--       órdenes de pago 339 y 325. Por IDs, SIN parámetros.
-- FECHA: 2026-09-15   EQUIPO: omen-saa-1 (crd)
--
-- ⚠️ ESTE SCRIPT NO ESCRIBE NADA. Cinco SELECT. Se corre de corrido tal cual está.
--
-- POR QUÉ HACE FALTA: en el 223 los bloques 2 (órdenes) y 3 (detalle) salieron VACÍOS, pero
-- el bloque 1 dice que la devolución 42 apunta a la orden 339 y la 22 a la 325. Eso sólo puede
-- ser una de estas tres cosas, y cada una cambia la solución:
--   (a) la cédula se reemplazó sólo en el bloque 1 y el 4 (el reemplazo tenía que ir en las
--       CUATRO apariciones) — este script no depende de la cédula, así que lo descarta;
--   (b) las órdenes 339/325 existen pero con otra etiqueta de origen o de documento;
--   (c) las órdenes o el detalle no existen.
-- =====================================================================================

SET PAGESIZE 200
SET LINESIZE 250


-- =====================================================================================
-- BLOQUE 1 — Las órdenes 339 y 325 por su número, sin filtrar por origen
--
-- Esperado: 2 filas. estado_orden: 0 POR_APROBAR · 1 REGISTRADO · 2 EN_ARCHIVO ·
-- 3 CONFIRMADO · 4 RECHAZADO · 5 ANULADO. Si faltan, es el caso (c).
-- =====================================================================================
SELECT  p.PGTRCDGO            AS orden,
        p.PGTRORGN            AS origen,
        p.PGTRIDOR            AS documento_origen,
        p.PGTRESTD            AS estado_orden,
        p.PGTRVLOR            AS valor,
        p.PGTRBFNM            AS beneficiario,
        p.PGTRBFID            AS identificacion,
        bb.BEXTNMBR           AS banco_en_orden,
        p.PGTRBFTP            AS tipo_cuenta_en_orden,
        p.PGTRBFCT            AS cuenta_en_orden,
        p.PGTRDBAT            AS debito_automatico,
        p.PGTRLTPG            AS lote,
        p.PGTRASNT            AS asiento_orden,
        p.PGTRRFBN            AS referencia_banco,
        p.PGTRFRSP            AS fecha_respuesta,
        p.PGTRFCRG            AS fecha_registro,
        SUBSTR(p.PGTRMTVO, 1, 200) AS motivo,
        SUBSTR(p.PGTROBSR, 1, 200) AS observacion
FROM    PGS.PGTR p
LEFT JOIN TSR.BEXT bb    ON bb.BEXTCDGO = p.PGTRBFBC
WHERE   p.PGTRCDGO IN (339, 325)
ORDER BY p.PGTRCDGO;


-- =====================================================================================
-- BLOQUE 2 — Cualquier otra orden que cuelgue de las devoluciones 42 o 22, con cualquier origen
--
-- Si aparece una orden distinta de 339/325, hubo un segundo intento de pago.
-- =====================================================================================
SELECT  p.PGTRCDGO            AS orden,
        p.PGTRORGN            AS origen,
        p.PGTRIDOR            AS documento_origen,
        p.PGTRESTD            AS estado_orden,
        p.PGTRVLOR            AS valor,
        p.PGTRBFCT            AS cuenta_en_orden,
        p.PGTRFCRG            AS fecha_registro
FROM    PGS.PGTR p
WHERE   p.PGTRIDOR IN (42, 22)
ORDER BY p.PGTRIDOR, p.PGTRCDGO;


-- =====================================================================================
-- BLOQUE 3 — Líneas contables de las órdenes (lo que se copiaría a la orden reemitida)
-- =====================================================================================
SELECT  d.PGTRCDGO            AS orden,
        d.DPGTCDGO            AS linea,
        d.DPGTPRDP            AS producto_pago,
        d.DPGTVLRR            AS valor,
        SUBSTR(d.DPGTCNCP, 1, 150) AS concepto
FROM    PGS.DPGT d
WHERE   d.PGTRCDGO IN (339, 325)
ORDER BY d.PGTRCDGO, d.DPGTCDGO;


-- =====================================================================================
-- BLOQUE 4 — Detalle de las devoluciones 42 y 22 (tipo de aporte, aporte negativo, reverso)
--
-- Si sale vacío también acá, la devolución no tiene detalle: no es un problema de la cédula.
-- =====================================================================================
SELECT  dd.DVAPCDGO           AS devolucion,
        dd.DDVACDGO           AS detalle,
        dd.TPAPCDGO           AS tipo_aporte,
        dd.DDVAVLRR           AS valor,
        dd.DDVAAPRT           AS aporte_negativo,
        dd.DDVAAPRV           AS aporte_reverso,
        dd.DDVAPGAP           AS pago_aporte
FROM    CRD.DDVA dd
WHERE   dd.DVAPCDGO IN (42, 22)
ORDER BY dd.DVAPCDGO, dd.DDVACDGO;


-- =====================================================================================
-- BLOQUE 5 — Los aportes enlazados a esas devoluciones por CRD.APRT.APRTIDDV
--
-- Muestra los negativos de la devolución, y si ya existe algún positivo de reverso.
-- =====================================================================================
SELECT  a.APRTIDDV            AS devolucion,
        a.APRTCDGO            AS aporte,
        a.TPAPCDGO            AS tipo_aporte,
        a.APRTVLRR            AS valor,
        a.APRTTPMV            AS tipo_movimiento,
        a.APRTFCTR            AS fecha,
        SUBSTR(a.APRTGLSA, 1, 120) AS glosa
FROM    CRD.APRT a
WHERE   a.APRTIDDV IN (42, 22)
ORDER BY a.APRTIDDV, a.APRTCDGO;
