-- =====================================================================================
-- 226 — MEDICIÓN: devoluciones EN_PAGO cuya orden de pago ya está RECHAZADA o ANULADA
-- FECHA: 2026-09-15   EQUIPO: omen-saa-1 (crd)
--
-- ⚠️ ESTE SCRIPT NO ESCRIBE NADA. Un SELECT. Sin parámetros.
--
-- POR QUÉ: el 225 mostró que la devolución 22 (LOPEZ VILLALVA) está EN_PAGO con su orden 325
-- ANULADA por tesorería con motivo «pago antiguo»: una devolución histórica de mayo registrada
-- en el SAA el 2026-09-07, cuyo dinero ya había salido por fuera. Con el WAR que está HOY en
-- producción, la primera vez que alguien abre las devoluciones de ese partícipe la sincronización
-- ve la orden ANULADA y REVIERTE LA DEVOLUCIÓN SOLA: le devuelve el aporte al partícipe como si
-- nunca se le hubiera pagado. Con el WAR nuevo (saaBE 04d96086) ya no revierte, pero la pantalla
-- ofrecería «Reemitir pago» y «Anular», y las DOS cosas serían un error para un pago antiguo.
--
-- Este bloque dice cuántas devoluciones están en esa situación, para decidir cómo cerrarlas.
-- Complementa al 224: su bloque 2 muestra las que YA se revirtieron así (RECHAZADA), y su
-- bloque 3 las PAGADAS con la orden reversada.
--
-- Estados CRD.DVAP: 2 EN_PAGO · 3 PAGADA · 4 RECHAZADA · 5 ANULADA
-- Estados PGS.PGTR: 4 RECHAZADO · 5 ANULADO
-- =====================================================================================

SET PAGESIZE 200
SET LINESIZE 250

SELECT  d.DVAPCDGO            AS devolucion,
        e.ENTDNMID            AS cedula,
        e.ENTDRZNS            AS participe,
        d.DVAPFCHA            AS fecha_devolucion,
        d.DVAPFCRG            AS fecha_registro_saa,
        d.DVAPVLRR            AS valor,
        d.DVAPNMRC            AS asiento_reclasificacion,
        p.PGTRCDGO            AS orden,
        p.PGTRESTD            AS estado_orden,
        p.PGTRBFCT            AS cuenta_en_orden,
        p.PGTRLTPG            AS lote,
        SUBSTR(p.PGTRMTVO, 1, 150) AS motivo_orden
FROM    CRD.DVAP d
JOIN    CRD.ENTD e       ON e.ENTDCDGO = d.ENTDCDGO
JOIN    PGS.PGTR p       ON p.PGTRCDGO = d.DVAPIDPG
WHERE   d.DVAPESTD = 2
AND     p.PGTRESTD IN (4, 5)
ORDER BY d.DVAPFCHA, d.DVAPCDGO;
