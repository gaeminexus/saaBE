-- =====================================================================================
-- 345 — CORRECCIÓN DE DATOS: mes real de los aportes de ARCH (filial 2).
-- omen-saa-1, 2026-10-31. Insumo: salida del crd/sql/344.
--
-- Lo que mostró el 344, en los 7 partícipes de ARCH:
--   - Las filas «PAGO APORTE JUNIO 2026» y «PAGO APORTE JULIO 2026» (APRTUSRG = 'SAA_JUL_FIN',
--     APRTIDAS = -98, registradas el 2026-08-04) NO tienen mes de devengo (APRTPRDV nulo) y las
--     DOS tienen fecha de caja 2026-07-31. Por «período efectivo» (devengo, o si es nulo el mes de
--     la fecha de caja) las dos cuentan como JULIO: junio queda vacío y julio doble.
--   - Agosto y septiembre (cobros personales, GROBAYO) sí tienen su mes.
--   - Por eso el 343 mandó el aporte de octubre de la carga 510 a JUNIO, el primer mes «incompleto».
--     En realidad junio-septiembre están pagados y lo de la 510 es OCTUBRE.
--
-- Qué hace (solo filial 2):
--   1. Pone APRTPRDV = 2026-06-01 a «PAGO APORTE JUNIO 2026» y 2026-07-01 a «PAGO APORTE JULIO 2026»
--      (solo las de SAA_JUL_FIN sin devengo).
--   2. Pasa los 9 aportes de la carga 510 de junio a OCTUBRE 2026 y corrige su glosa.
-- No cambia montos ni fechas de caja: el asiento no se toca. Seguro de correr dos veces.
-- =====================================================================================

-- CONTROL 0 — antes: lo que se va a tocar.
SELECT e.ENTDRLPC AS ROL, a.APRTCDGO, a.TPAPCDGO, a.APRTVLRR, a.APRTPRDV, a.APRTFCTR, a.APRTUSRG,
       a.APRTIDAS, a.APRTGLSA
  FROM CRD.APRT a
  JOIN CRD.ENTD e ON e.ENTDCDGO = a.ENTDCDGO
 WHERE e.FLLLCDGO = 2
   AND ((a.APRTUSRG = 'SAA_JUL_FIN' AND a.APRTPRDV IS NULL
         AND a.APRTGLSA IN ('PAGO APORTE JUNIO 2026', 'PAGO APORTE JULIO 2026'))
        OR a.APRTIDAS = 510)
 ORDER BY e.ENTDRLPC, a.TPAPCDGO, a.APRTGLSA;

-- BLOQUE 1 — mes real de junio y julio (SAA_JUL_FIN, filial 2)
UPDATE CRD.APRT a
   SET a.APRTPRDV = DATE '2026-06-01'
 WHERE a.APRTUSRG = 'SAA_JUL_FIN' AND a.APRTPRDV IS NULL
   AND a.APRTGLSA = 'PAGO APORTE JUNIO 2026'
   AND a.ENTDCDGO IN (SELECT e.ENTDCDGO FROM CRD.ENTD e WHERE e.FLLLCDGO = 2);

UPDATE CRD.APRT a
   SET a.APRTPRDV = DATE '2026-07-01'
 WHERE a.APRTUSRG = 'SAA_JUL_FIN' AND a.APRTPRDV IS NULL
   AND a.APRTGLSA = 'PAGO APORTE JULIO 2026'
   AND a.ENTDCDGO IN (SELECT e.ENTDCDGO FROM CRD.ENTD e WHERE e.FLLLCDGO = 2);

-- BLOQUE 2 — los aportes de la carga 510 son de OCTUBRE
UPDATE CRD.APRT a
   SET a.APRTPRDV = DATE '2026-10-01',
       a.APRTGLSA = REPLACE(a.APRTGLSA, 'Mes 6/2026 (pagado en 10/2026) -', 'Mes 10/2026 (pagado en 10/2026) -')
 WHERE a.APRTIDAS = 510
   AND a.APRTPRDV = DATE '2026-06-01';

COMMIT;

-- CONTROL 1 — después: por partícipe y tipo, UNA fila por mes de jun a oct 2026 (julio ya no doble).
SELECT e.ENTDRLPC AS ROL, a.TPAPCDGO,
       TO_CHAR(CASE WHEN a.APRTPRDV IS NOT NULL THEN a.APRTPRDV
                    WHEN a.APRTVLRR > 0 THEN TRUNC(a.APRTFCTR, 'MM') END, 'YYYY-MM') AS PERIODO,
       COUNT(*) AS FILAS, SUM(a.APRTVLRR) AS TOTAL
  FROM CRD.APRT a
  JOIN CRD.ENTD e ON e.ENTDCDGO = a.ENTDCDGO
 WHERE e.FLLLCDGO = 2
   AND a.TPAPCDGO IN (9, 11)
   AND (a.APRTPRDV >= DATE '2026-06-01' OR a.APRTFCTR >= DATE '2026-06-01')
 GROUP BY e.ENTDRLPC, a.TPAPCDGO,
          TO_CHAR(CASE WHEN a.APRTPRDV IS NOT NULL THEN a.APRTPRDV
                       WHEN a.APRTVLRR > 0 THEN TRUNC(a.APRTFCTR, 'MM') END, 'YYYY-MM')
 ORDER BY 1, 2, 3;

-- CONTROL 2 — MEDICIÓN para mañana (no corrige nada): ¿el mismo problema en TODA la cartera?
--   a) filas SAA_JUL_FIN sin devengo, por glosa, en todas las filiales
SELECT e.FLLLCDGO, a.APRTGLSA, COUNT(*) AS FILAS, SUM(a.APRTVLRR) AS TOTAL
  FROM CRD.APRT a
  JOIN CRD.ENTD e ON e.ENTDCDGO = a.ENTDCDGO
 WHERE a.APRTUSRG = 'SAA_JUL_FIN' AND a.APRTPRDV IS NULL
 GROUP BY e.FLLLCDGO, a.APRTGLSA
 ORDER BY 1, 2;
--   b) aportes que las cargas (SAA_AH) mandaron a junio 2026 después de agosto: candidatos a
--      haber caído en «junio vacío» por el mismo motivo
SELECT a.APRTIDAS AS CARGA, COUNT(*) AS FILAS, SUM(a.APRTVLRR) AS TOTAL
  FROM CRD.APRT a
 WHERE a.APRTUSRG = 'SAA_AH'
   AND a.APRTPRDV = DATE '2026-06-01'
   AND a.APRTFCRG >= DATE '2026-08-05'
 GROUP BY a.APRTIDAS
 ORDER BY 1;

-- REVERSO (comentado)
-- UPDATE CRD.APRT a SET a.APRTPRDV = DATE '2026-06-01',
--        a.APRTGLSA = REPLACE(a.APRTGLSA, 'Mes 10/2026 (pagado en 10/2026) -', 'Mes 6/2026 (pagado en 10/2026) -')
--  WHERE a.APRTIDAS = 510 AND a.APRTPRDV = DATE '2026-10-01';
-- UPDATE CRD.APRT a SET a.APRTPRDV = NULL
--  WHERE a.APRTUSRG = 'SAA_JUL_FIN' AND a.APRTGLSA IN ('PAGO APORTE JUNIO 2026','PAGO APORTE JULIO 2026')
--    AND a.ENTDCDGO IN (SELECT e.ENTDCDGO FROM CRD.ENTD e WHERE e.FLLLCDGO = 2);
-- COMMIT;
