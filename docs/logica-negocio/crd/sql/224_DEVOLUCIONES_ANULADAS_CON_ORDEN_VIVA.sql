-- =====================================================================================
-- 224 — MEDICIÓN: devoluciones de aportes ANULADAS cuya orden de pago sigue viva, y las que
--       la sincronización revirtió o dejó cerradas como pagadas con la orden reversada
-- FECHA: 2026-09-15   EQUIPO: omen-saa-1 (crd)
--
-- ⚠️ ESTE SCRIPT NO ESCRIBE NADA. Tres SELECT. Se puede correr en producción en cualquier
--    momento. No lleva parámetros.
--
-- POR QUÉ: DevolucionAporteServiceImpl.anularDevolucion sólo anula la orden de pago cuando
-- está REGISTRADA(1). Desde el 2026-08-29 toda orden de devolución nace POR_APROBAR(0), así que
-- anular una devolución antes de que tesorería apruebe su orden deja la devolución ANULADA, el
-- aporte devuelto al partícipe y el asiento de reclasificación anulado... y la ORDEN VIVA.
-- Tesorería la puede aprobar y pagar: el partícipe cobra dinero que ya volvió a su saldo.
-- Contrato de la corrección: crd/API-REEMITIR-PAGO-DEVOLUCION.md §5.
--
-- Estados PGS.PGTR: 0 POR_APROBAR · 1 REGISTRADO · 2 EN_ARCHIVO · 3 CONFIRMADO · 4 RECHAZADO · 5 ANULADO
-- Estados CRD.DVAP: 1 REGISTRADA · 2 EN_PAGO · 3 PAGADA · 4 RECHAZADA · 5 ANULADA
-- =====================================================================================

SET PAGESIZE 200
SET LINESIZE 250


-- =====================================================================================
-- BLOQUE 1 — ⛔ Devoluciones ANULADAS con una orden de pago que NO está anulada ni rechazada
--
-- Esperado: 0 filas.
--   estado_orden 0/1/2 → la orden todavía se puede pagar. Hay que anularla en tesorería YA.
--   estado_orden 3     → YA SE PAGÓ: el partícipe cobró y además tiene el aporte devuelto.
-- =====================================================================================
SELECT  d.DVAPCDGO            AS devolucion,
        e.ENTDNMID            AS cedula,
        e.ENTDRZNS            AS participe,
        d.DVAPVLRR            AS valor_devolucion,
        d.DVAPFCAN            AS fecha_anulacion,
        d.DVAPUSAN            AS usuario_anulacion,
        p.PGTRCDGO            AS orden,
        p.PGTRESTD            AS estado_orden,
        p.PGTRVLOR            AS valor_orden,
        p.PGTRFCRG            AS fecha_orden,
        p.PGTRFRSP            AS fecha_respuesta,
        p.PGTRRFBN            AS referencia_banco
FROM    CRD.DVAP d
JOIN    CRD.ENTD e       ON e.ENTDCDGO = d.ENTDCDGO
JOIN    PGS.PGTR p       ON p.PGTRORGN = 'CRD_DEVOLUCION_APORTE'
                        AND p.PGTRIDOR = d.DVAPCDGO
WHERE   d.DVAPESTD = 5
AND     p.PGTRESTD IN (0, 1, 2, 3)
ORDER BY p.PGTRESTD DESC, d.DVAPCDGO;


-- =====================================================================================
-- BLOQUE 2 — Devoluciones RECHAZADAS por la sincronización automática
--
-- Informativo: son las que el reconciliador revirtió solo porque la orden volvió rechazada o
-- anulada. Si alguna era un rebote que se quería reemitir, ya perdió el aporte negativo.
-- =====================================================================================
SELECT  d.DVAPCDGO            AS devolucion,
        e.ENTDNMID            AS cedula,
        e.ENTDRZNS            AS participe,
        d.DVAPVLRR            AS valor,
        d.DVAPFCHA            AS fecha_devolucion,
        p.PGTRCDGO            AS orden,
        p.PGTRESTD            AS estado_orden,
        SUBSTR(p.PGTRMTVO, 1, 150) AS motivo_orden
FROM    CRD.DVAP d
JOIN    CRD.ENTD e       ON e.ENTDCDGO = d.ENTDCDGO
LEFT JOIN PGS.PGTR p     ON p.PGTRCDGO = d.DVAPIDPG
WHERE   d.DVAPESTD = 4
ORDER BY d.DVAPCDGO DESC;


-- =====================================================================================
-- BLOQUE 3 — Devoluciones PAGADAS cuya orden enlazada está RECHAZADA o ANULADA
--
-- Esperado: 0 filas. Cada fila es un partícipe que figura pagado y cuyo dinero NO llegó
-- (tesorería confirmó y después reversó). Ningún proceso actual las vuelve a mirar.
-- =====================================================================================
SELECT  d.DVAPCDGO            AS devolucion,
        e.ENTDNMID            AS cedula,
        e.ENTDRZNS            AS participe,
        d.DVAPVLRR            AS valor,
        d.DVAPFCPG            AS fecha_pago_registrada,
        d.DVAPNMAS            AS asiento_pago,
        p.PGTRCDGO            AS orden,
        p.PGTRESTD            AS estado_orden,
        SUBSTR(p.PGTRMTVO, 1, 150) AS motivo_orden
FROM    CRD.DVAP d
JOIN    CRD.ENTD e       ON e.ENTDCDGO = d.ENTDCDGO
JOIN    PGS.PGTR p       ON p.PGTRCDGO = d.DVAPIDPG
WHERE   d.DVAPESTD = 3
AND     p.PGTRESTD IN (4, 5)
ORDER BY d.DVAPCDGO DESC;
