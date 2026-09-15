-- =====================================================================
-- e2-49 — Retenciones emitidas AUTORIZADAS que no rebajaron el documento
--          (aparecen en el estado de cuenta, pero la factura no baja)
-- Modulo: tsr / cxc / cxp  ·  Equipo: omen-saa-2  ·  Fecha: 2026-09-15
--
-- ✅ SOLO LECTURA. Pegar la salida con la columna BLOQUE.
--
-- POR QUE EXISTE — tsr/AUDITORIA-ESTADO-CUENTA-TITULAR.md, defecto P3.
--   Una retencion V2 autorizada crea su cruce (PGS.APLP con APLPRTV2) solo
--   si tiene asiento, el facturador genera contabilidad, el documento se
--   encuentra y hay saldo. Si algo falla, queda "cruce pendiente": la
--   retencion se lista restando en el estado de cuenta y la factura NO baja.
--
-- Columnas copiadas de las entidades:
--   RetencionV2 (CBR.RTV2: ID, NUMERO, PROVEEDOR, FECHA, TOTAL, ESTADO,
--                ESTADOEMISION, ASIENTO, FACTURADOR)
--   DetalleRetencionV2 (CBR.DRV2: RETENCIONV2, TIPODOCRETEN, NUMDOCRETEN)
--   AplicacionPagoCxp (PGS.APLP: APLPCDGO, APLPRTV2, APLPESTD)
--   Titular (TSR.TTLR: TTLRCDGO, TTLRNMBR)
-- =====================================================================


-- ---------------------------------------------------------------------
-- BLOQUE 1 — Resumen: autorizadas con total > 0 sin cruce activo
-- ESPERADO: CANTIDAD = 0. Cada una es una factura (o liquidacion) cuyo
--           saldo en el estado de cuenta esta inflado en ese valor.
--   SIN_ASIENTO = 'SI' -> el cruce ni se intento (falta contabilidad).
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 1 - resumen' AS bloque,
       CASE WHEN r.ASIENTO IS NULL THEN 'SI' ELSE 'NO' END AS sin_asiento,
       COUNT(*)     AS cantidad,
       SUM(r.TOTAL) AS total_retenido
  FROM CBR.RTV2 r
 WHERE r.ESTADO = 5
   AND r.ESTADOEMISION <> 3
   AND NVL(r.TOTAL, 0) > 0.005
   AND NOT EXISTS (SELECT 1 FROM PGS.APLP a
                    WHERE a.APLPRTV2 = r.ID AND a.APLPESTD = 1)
 GROUP BY CASE WHEN r.ASIENTO IS NULL THEN 'SI' ELSE 'NO' END;


-- ---------------------------------------------------------------------
-- BLOQUE 2 — El detalle, con el tipo de documento sustento
-- ESPERADO: vacio si el bloque 1 dio 0.
--   TIPODOCRETEN = '03' (liquidacion) -> defecto P2: hoy nunca cruza.
--   '01'/'02' con asiento -> revisar saldo del documento o reprocesar con
--   el boton Contabilizar de la retencion.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 2 - detalle' AS bloque,
       r.ID AS id_retencion, r.NUMERO, r.FECHA, r.TOTAL,
       CASE WHEN r.ASIENTO IS NULL THEN 'SI' ELSE 'NO' END AS sin_asiento,
       t.TTLRNMBR AS proveedor,
       LISTAGG(DISTINCT d.TIPODOCRETEN || ':' || d.NUMDOCRETEN, ', ')
         WITHIN GROUP (ORDER BY d.NUMDOCRETEN) AS documentos_sustento
  FROM CBR.RTV2 r
  LEFT JOIN TSR.TTLR t ON t.TTLRCDGO = r.PROVEEDOR
  LEFT JOIN CBR.DRV2 d ON d.RETENCIONV2 = r.ID
 WHERE r.ESTADO = 5
   AND r.ESTADOEMISION <> 3
   AND NVL(r.TOTAL, 0) > 0.005
   AND NOT EXISTS (SELECT 1 FROM PGS.APLP a
                    WHERE a.APLPRTV2 = r.ID AND a.APLPESTD = 1)
 GROUP BY r.ID, r.NUMERO, r.FECHA, r.TOTAL, r.ASIENTO, t.TTLRNMBR
 ORDER BY r.FECHA;
