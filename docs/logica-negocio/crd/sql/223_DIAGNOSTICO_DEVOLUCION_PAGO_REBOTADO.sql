-- =====================================================================================
-- 223 — DIAGNÓSTICO: devolución de aportes cuyo pago rebotó por cuenta bancaria errada
-- FECHA: 2026-09-15   EQUIPO: omen-saa-1 (crd)
--
-- ⚠️ ESTE SCRIPT NO ESCRIBE NADA. Son cuatro SELECT. Se puede correr en producción en
--    cualquier momento.
--
-- CÓMO USARLO: reemplazar las CUATRO apariciones de  CEDULA_DEL_PARTICIPE  por la cédula
-- (ENTDNMID) del partícipe y correr de corrido. Pegar la salida completa.
--
-- POR QUÉ HACE FALTA ANTES DE DECIDIR NADA: la corrección posible depende de en qué punto
-- quedó el caso, y hay cuatro puntos distintos que en pantalla se ven parecidos:
--
--   (A) Orden EN_ARCHIVO (2) o REGISTRADA (1) y devolución EN_PAGO (2): el rebote todavía no
--       se registró en tesorería. Es el mejor caso: el aporte negativo sigue intacto.
--   (B) Orden RECHAZADA (4) o ANULADA (5) y devolución todavía EN_PAGO (2): ⛔ EN RIESGO.
--       La próxima vez que alguien abra las devoluciones de ese partícipe (o se sincronice),
--       CRD revierte TODO sola: contra-movimiento del aporte y anulación del asiento de
--       reclasificación (DevolucionAporteServiceImpl.sincronizarDevolucion).
--   (C) Orden RECHAZADA/ANULADA y devolución RECHAZADA (4): la reversión completa YA ocurrió.
--       El bloque 3 lo confirma con DDVAAPRV lleno.
--   (D) Orden RECHAZADA y devolución PAGADA (3): tesorería confirmó a mano antes del rebote,
--       CRD la marcó pagada, y después se reversó el pago. La devolución quedó cerrada como
--       pagada y NINGÚN proceso actual la vuelve a mirar.
--
-- Estados PGS.PGTR (EstadoPagoProgramado): 0 POR_APROBAR · 1 REGISTRADO · 2 EN_ARCHIVO ·
--   3 CONFIRMADO · 4 RECHAZADO · 5 ANULADO
-- Estados CRD.DVAP (EstadoDevolucionAporte): 1 REGISTRADA · 2 EN_PAGO · 3 PAGADA ·
--   4 RECHAZADA · 5 ANULADA
-- =====================================================================================

SET PAGESIZE 200
SET LINESIZE 250


-- =====================================================================================
-- BLOQUE 1 — Devoluciones del partícipe, y la cuenta bancaria con la que se registraron
-- =====================================================================================
SELECT  d.DVAPCDGO            AS devolucion,
        d.DVAPFCHA            AS fecha,
        d.DVAPVLRR            AS valor,
        d.DVAPESTD            AS estado_devolucion,
        d.DVAPIDPG            AS orden_pago_enlazada,
        d.DVAPNMRC            AS asiento_reclasificacion,
        d.DVAPNMAS            AS asiento_pago,
        d.DVAPFCPG            AS fecha_pago,
        d.CNBPCDGO            AS cuenta_participe_usada,
        c.CNBPNMRO            AS numero_cuenta_usada,
        b.BEXTNMBR            AS banco_cuenta_usada,
        e.ENTDRZNS            AS participe
FROM    CRD.DVAP d
JOIN    CRD.ENTD e       ON e.ENTDCDGO = d.ENTDCDGO
LEFT JOIN CRD.CNBP c     ON c.CNBPCDGO = d.CNBPCDGO
LEFT JOIN TSR.BEXT b     ON b.BEXTCDGO = c.BEXTCDGO
WHERE   e.ENTDNMID = 'CEDULA_DEL_PARTICIPE'
ORDER BY d.DVAPCDGO DESC;


-- =====================================================================================
-- BLOQUE 2 — TODAS las órdenes de pago de esas devoluciones (puede haber más de una)
--
-- La cuenta que viaja al banco es la COPIA grabada en la orden (PGTRBFBC/PGTRBFTP/PGTRBFCT),
-- no la de CRD.CNBP: corregir la cuenta del partícipe NO corrige una orden ya emitida.
-- =====================================================================================
SELECT  p.PGTRCDGO            AS orden,
        p.PGTRIDOR            AS devolucion,
        p.PGTRESTD            AS estado_orden,
        p.PGTRVLOR            AS valor,
        p.PGTRBFNM            AS beneficiario,
        p.PGTRBFID            AS identificacion,
        bb.BEXTNMBR           AS banco_en_orden,
        p.PGTRBFTP            AS tipo_cuenta_en_orden,
        p.PGTRBFCT            AS cuenta_en_orden,
        p.PGTRLTPG            AS lote,
        p.PGTRASNT            AS asiento_orden,
        p.PGTRRFBN            AS referencia_banco,
        p.PGTRFRSP            AS fecha_respuesta,
        p.PGTRFCRG            AS fecha_registro,
        SUBSTR(p.PGTRMTVO, 1, 200) AS motivo,
        SUBSTR(p.PGTROBSR, 1, 200) AS observacion
FROM    PGS.PGTR p
LEFT JOIN TSR.BEXT bb    ON bb.BEXTCDGO = p.PGTRBFBC
WHERE   p.PGTRORGN = 'CRD_DEVOLUCION_APORTE'
AND     p.PGTRIDOR IN (SELECT d.DVAPCDGO
                       FROM   CRD.DVAP d
                       JOIN   CRD.ENTD e ON e.ENTDCDGO = d.ENTDCDGO
                       WHERE  e.ENTDNMID = 'CEDULA_DEL_PARTICIPE')
ORDER BY p.PGTRIDOR DESC, p.PGTRCDGO;


-- =====================================================================================
-- BLOQUE 3 — Detalle: el aporte negativo de la devolución y si ya tiene contra-movimiento
--
-- aporte_reverso (DDVAAPRV) lleno = CRD ya revirtió ese tipo de aporte (caso C).
-- =====================================================================================
SELECT  dd.DVAPCDGO           AS devolucion,
        dd.DDVACDGO           AS detalle,
        dd.TPAPCDGO           AS tipo_aporte,
        dd.DDVAVLRR           AS valor,
        dd.DDVAAPRT           AS aporte_negativo,
        an.APRTVLRR           AS valor_aporte_negativo,
        dd.DDVAAPRV           AS aporte_reverso,
        ar.APRTVLRR           AS valor_aporte_reverso,
        ar.APRTFCTR           AS fecha_reverso,
        dd.DDVAPGAP           AS pago_aporte
FROM    CRD.DDVA dd
LEFT JOIN CRD.APRT an    ON an.APRTCDGO = dd.DDVAAPRT
LEFT JOIN CRD.APRT ar    ON ar.APRTCDGO = dd.DDVAAPRV
WHERE   dd.DVAPCDGO IN (SELECT d.DVAPCDGO
                        FROM   CRD.DVAP d
                        JOIN   CRD.ENTD e ON e.ENTDCDGO = d.ENTDCDGO
                        WHERE  e.ENTDNMID = 'CEDULA_DEL_PARTICIPE')
ORDER BY dd.DVAPCDGO DESC, dd.DDVACDGO;


-- =====================================================================================
-- BLOQUE 4 — Cuentas bancarias registradas del partícipe
--
-- Para saber si la cuenta CORRECTA ya existe como fila propia, o si la errada se editó
-- encima (en ese caso bloque 1 y bloque 2 muestran números distintos para la misma cuenta).
-- =====================================================================================
SELECT  c.CNBPCDGO            AS cuenta,
        b.BEXTNMBR            AS banco,
        c.CNBPTPCN            AS tipo_cuenta,
        c.CNBPNMRO            AS numero,
        c.CNBPIDST            AS estado
FROM    CRD.CNBP c
JOIN    CRD.ENTD e       ON e.ENTDCDGO = c.ENTDCDGO
LEFT JOIN TSR.BEXT b     ON b.BEXTCDGO = c.BEXTCDGO
WHERE   e.ENTDNMID = 'CEDULA_DEL_PARTICIPE'
ORDER BY c.CNBPCDGO;
