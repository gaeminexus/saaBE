-- =====================================================================================
-- e2-76 — Los «saldos a favor» de proveedores que nacen de una retención emitida DESPUÉS de
--         pagar la factura completa. SOLO LECTURA. Equipo omen-saa-2, 2026-09-30.
--
-- Lo medido en el código:
--   RetencionV2ServiceImpl.cerrarContabilidadYCruceRetencionV2 (:1367-1408) CONFIRMA primero el
--   asiento de la retención (DEBE CxP del proveedor / HABER retenciones) en su propia transacción,
--   y DESPUÉS intenta el cruce contra la factura (PGS.APLP tipo 3). Si la factura ya está pagada,
--   el cruce se RECHAZA («supera el saldo pendiente») y la retención queda con cruce pendiente.
--   Resultado: la CxP del proveedor queda con saldo DEUDOR en contabilidad, pero la factura sigue
--   en saldo 0 y no hay anticipo. Ese es el dinero que el proveedor devuelve.
--
-- Nombres de columna copiados de RetencionV2 (CBR.RTV2) y AplicacionPagoCxp (PGS.APLP).
-- «Vigente» = ESTADO 5 (autorizada) y ESTADOEMISION <> 3 (anulada): CriterioVentaVigente, que
-- también aplica a RetencionV2.
-- =====================================================================================

-- BLOQUE 1 — Retenciones emitidas, vigentes, CONTABILIZADAS y SIN cruce activo contra ningún
-- documento. Cada fila es un candidato a «saldo a favor». Buscar aquí el caso que reportó el usuario.
SELECT 'BLOQUE 1 - retenciones sin cruce' AS bloque,
       r.ID AS id_retencion, r.NUMERO, r.FECHA, r.TOTAL,
       r.PROVEEDOR AS id_titular, t.TTLRIDNT, NVL(t.TTLRRZSC, t.TTLRNMBR) AS proveedor,
       r.ASIENTO AS id_asiento, r.OBSERVACION
  FROM CBR.RTV2 r
  JOIN TSR.TTLR t ON t.TTLRCDGO = r.PROVEEDOR
 WHERE r.ESTADO = 5
   AND NVL(r.ESTADOEMISION, 0) <> 3
   AND r.ASIENTO IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM PGS.APLP a WHERE a.APLPRTV2 = r.ID AND a.APLPESTD = 1)
 ORDER BY r.FECHA DESC;

-- BLOQUE 2 — Lo mismo, sumado por proveedor: el «saldo a favor» que cada uno tendría que
-- devolver (si la causa es la del encabezado).
SELECT 'BLOQUE 2 - por proveedor' AS bloque,
       r.PROVEEDOR AS id_titular, NVL(t.TTLRRZSC, t.TTLRNMBR) AS proveedor,
       COUNT(*) AS retenciones, SUM(r.TOTAL) AS total_retenido_sin_cruce
  FROM CBR.RTV2 r
  JOIN TSR.TTLR t ON t.TTLRCDGO = r.PROVEEDOR
 WHERE r.ESTADO = 5
   AND NVL(r.ESTADOEMISION, 0) <> 3
   AND r.ASIENTO IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM PGS.APLP a WHERE a.APLPRTV2 = r.ID AND a.APLPESTD = 1)
 GROUP BY r.PROVEEDOR, NVL(t.TTLRRZSC, t.TTLRNMBR)
 ORDER BY total_retenido_sin_cruce DESC;
