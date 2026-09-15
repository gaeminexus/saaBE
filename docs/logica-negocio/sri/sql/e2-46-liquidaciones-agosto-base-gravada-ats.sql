-- =====================================================================
-- e2-46 — ¿El ATS de agosto declara bien la base gravada de las
--          liquidaciones de compra?
-- Modulo: sri / cxp  ·  Equipo: omen-saa-2  ·  Fecha: 2026-09-15
--
-- ✅ SOLO LECTURA. Pegar la salida con la columna BLOQUE.
--
-- POR QUE EXISTE — hallazgo del 2026-09-15 al leer los avisos del ATS
--   PGS.LQCC.SUBTOTAL significa DOS cosas segun quien creo la fila:
--   - Carga del XML (ProcesoCargaDocumentosServiceImpl): SUBTOTAL =
--     totalSinImpuestos, que YA incluye la parte al 0%.
--   - Liquidacion EMITIDA por ASOPREP (LiquidacionCompraServiceImpl:813,
--     copia de CBR.LQCS): SUBTOTAL = base GRAVADA sola — la pantalla manda
--     subtotal = subtotalGravado (liquidaciones.component.ts:661) y el XML
--     arma totalSinImpuestos = SUBTOTAL + SUBCERO (:1035).
--   El ATS calcula gravada = SUBTOTAL - SUBCERO (GeneradorAtsServiceImpl,
--   baseGravadaCompra), correcto para la primera y MAL para la segunda.
--   Las liquidaciones 2 y 3 (SUBTOTAL 0) salen bien de casualidad.
--   La que importa es cualquiera con SUBTOTAL > 0 y SUBCERO > 0: el ATS
--   le declara SUBCERO de menos en base gravada.
--
-- Columnas copiadas de LiquidacionCompraCompra (PGS.LQCC: ID, TITULAR,
--   NUMERO, CLAVE, FECHA, SUBTOTAL, SUBCERO, VIVA, TOTAL, ESTADO).
-- =====================================================================


-- ---------------------------------------------------------------------
-- BLOQUE 1 — Liquidaciones de agosto 2026 y que convencion siguen
-- ESPERADO: CONVENCION = 'EMITIDA (SUBTOTAL=GRAVADA)' en las que emitio
--   ASOPREP. Para cada una:
--     GRAVADA_QUE_DECLARA_ATS = GRAVADA_CORRECTA  -> bien declarada.
--     distintas                                   -> 🔴 mal declarada.
--   CONVENCION = 'NO CUADRA' -> pegar la fila, hay que mirarla a mano.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 1 - LQCC agosto' AS bloque,
       l.ID, l.NUMERO, l.FECHA, l.ESTADO,
       NVL(l.SUBTOTAL, 0) AS subtotal,
       NVL(l.SUBCERO, 0)  AS subcero,
       NVL(l.VIVA, 0)     AS viva,
       NVL(l.TOTAL, 0)    AS total,
       CASE
         WHEN ABS(NVL(l.SUBTOTAL,0) + NVL(l.SUBCERO,0) + NVL(l.VIVA,0) - NVL(l.TOTAL,0)) < 0.02
              AND NVL(l.SUBCERO,0) > 0 THEN 'EMITIDA (SUBTOTAL=GRAVADA)'
         WHEN ABS(NVL(l.SUBTOTAL,0) + NVL(l.VIVA,0) - NVL(l.TOTAL,0)) < 0.02
              THEN 'XML (SUBTOTAL=TOTAL SIN IMPUESTOS) o sin 0%'
         ELSE 'NO CUADRA'
       END AS convencion,
       GREATEST(NVL(l.SUBTOTAL,0) - NVL(l.SUBCERO,0), 0) AS gravada_que_declara_ats,
       CASE
         WHEN ABS(NVL(l.SUBTOTAL,0) + NVL(l.SUBCERO,0) + NVL(l.VIVA,0) - NVL(l.TOTAL,0)) < 0.02
              AND NVL(l.SUBCERO,0) > 0 THEN NVL(l.SUBTOTAL,0)
         ELSE GREATEST(NVL(l.SUBTOTAL,0) - NVL(l.SUBCERO,0), 0)
       END AS gravada_correcta
  FROM PGS.LQCC l
 WHERE l.FECHA >= DATE '2026-08-01'
   AND l.FECHA <  DATE '2026-09-01'
 ORDER BY l.ID;
