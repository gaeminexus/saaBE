-- =====================================================================================
-- C45-01 · Medición de cobros duplicados (DS-01) — SOLO LECTURA
-- FECHA: 2026-11-24 · rama cuadre/c45-bloqueantes-produccion
--
-- No cambia nada: mide lo que los controles nuevos de C45 habrían rechazado.
--   (A) CobroCreditoServiceImpl.procesarCobro bloquea CRD.CBCR (FOR UPDATE NOWAIT) antes de
--       leer el estado: dos procesos del MISMO cobro ya no pueden correr a la vez.
--   (B) procesarCobro rechaza si el préstamo de una línea ya tiene un evento VIGENTE de la
--       misma fecha y valor que no pertenece a ningún cobro (CRD.DCBC.EVPRCDGO).
--   (C) validar (registro/corrección) rechaza un cobro '9'/'09' si ya hay otro no anulado de la
--       misma entidad, cuenta, fecha de depósito y valor. Igual en CRD.RVSG.
-- Resultado en la copia del 15-nov-2026: (B) 3 filas (eventos 1, 42 y 1151), (C) 0 filas.
-- =====================================================================================

-- (B) Eventos sueltos que coinciden con una línea de un cobro no anulado
SELECT e.EVPRCDGO, e.EVPRTPOO, e.PRSTCDGO, e.EVPRVLRR, TO_CHAR(e.EVPRFCHA, 'YYYY-MM-DD') AS FECHA,
       c.CBCRCDGO, c.CBCRESTD, d.EVPRCDGO AS EVENTO_DEL_COBRO
  FROM CRD.EVPR e
  JOIN CRD.DCBC d ON d.PRSTCDGO = e.PRSTCDGO AND ABS(d.DCBCVLRR - e.EVPRVLRR) < 0.005
  JOIN CRD.CBCR c ON c.CBCRCDGO = d.CBCRCDGO AND TRUNC(c.CBCRFCHA) = TRUNC(e.EVPRFCHA) AND c.CBCRESTD <> 5
 WHERE e.EVPRESTD = 1
   AND NOT EXISTS (SELECT 1 FROM CRD.DCBC x WHERE x.EVPRCDGO = e.EVPRCDGO)
 ORDER BY e.EVPRCDGO;


-- (C) Cobros '9'/'09' no anulados que comparten entidad, cuenta, fecha y valor
SELECT a.CBCRCDGO, b.CBCRCDGO AS CBCRCDGO_2, a.CBCRVLRR, TO_CHAR(a.CBCRFCHA, 'YYYY-MM-DD') AS FECHA
  FROM CRD.CBCR a
  JOIN CRD.CBCR b ON b.CBCRCDGO > a.CBCRCDGO AND b.ENTDCDGO = a.ENTDCDGO AND b.CNBCCDGO = a.CNBCCDGO
                 AND TRUNC(b.CBCRFCHA) = TRUNC(a.CBCRFCHA) AND ABS(b.CBCRVLRR - a.CBCRVLRR) < 0.005
 WHERE TRIM(a.CBCRRFRN) IN ('9', '09') AND TRIM(b.CBCRRFRN) IN ('9', '09')
   AND NVL(a.CBCRESTD, 0) <> 5 AND NVL(b.CBCRESTD, 0) <> 5;

-- (C) Recepciones de valor de seguro '9'/'09' vigentes (ni 3 rechazada ni 4 anulada, CrdEstadoRecepcionSeguro)
SELECT a.RVSGCDGO, b.RVSGCDGO AS RVSGCDGO_2, a.RVSGVLRR, TO_CHAR(a.RVSGFCHA, 'YYYY-MM-DD') AS FECHA
  FROM CRD.RVSG a
  JOIN CRD.RVSG b ON b.RVSGCDGO > a.RVSGCDGO AND b.ENTDCDGO = a.ENTDCDGO AND b.TPAPCDGO = a.TPAPCDGO
                 AND b.CNBCCDGO = a.CNBCCDGO AND b.RVSGFCHA = a.RVSGFCHA AND ABS(b.RVSGVLRR - a.RVSGVLRR) < 0.005
 WHERE TRIM(a.RVSGRFRN) IN ('9', '09') AND TRIM(b.RVSGRFRN) IN ('9', '09')
   AND NVL(a.RVSGESTD, 0) NOT IN (3, 4) AND NVL(b.RVSGESTD, 0) NOT IN (3, 4);
