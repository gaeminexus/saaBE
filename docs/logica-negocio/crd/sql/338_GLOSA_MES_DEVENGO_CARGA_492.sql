-- =====================================================================================
-- 338 — CORRECCIÓN DE DATOS: la glosa de los aportes de la carga 492 dice el mes de su
-- DEVENGO (al que pertenece el aporte), no solo el mes de la carga.
-- omen-saa-1, 2026-10-31.
--
-- Por qué: CargaArchivoPetroServiceImpl.crearNuevoAporte arma la glosa SIEMPRE con el mes de la
-- carga ("Aporte X - Mes 10/2026 - CargaArchivo: 492"), aunque la fila pague otro mes (atraso o
-- anticipo). El mes real está en APRTPRDV, pero la pantalla de aportes muestra la fecha de caja
-- (APRTFCTR = último día del mes de la carga, por diseño) y la glosa. Así no se distingue de qué
-- mes es cada aporte. Pasa con el reparto automático de toda carga, no solo con las 5 filas del 337.
--
-- Qué hace: en las filas de la carga 492 cuyo devengo NO es octubre 2026, cambia "Mes 10/2026"
-- por "Mes {devengo} (pagado en 10/2026)". Ejemplo:
--   "Aporte CESANTIA PERSONAL - Mes 7/2026 (pagado en 10/2026) - CargaArchivo: 492"
-- Las de devengo octubre quedan igual. No toca montos, fechas ni el asiento.
-- Seguro de correr dos veces: solo cambia glosas que todavía dicen "Mes 10/2026 -".
-- =====================================================================================

-- CONTROL 0 — antes: cuántas filas de la 492 pagan un mes distinto de octubre, por mes.
SELECT TO_CHAR(a.APRTPRDV, 'YYYY-MM') AS DEVENGO, COUNT(*) AS FILAS, SUM(a.APRTVLRR) AS TOTAL
  FROM CRD.APRT a
 WHERE a.APRTIDAS = 492
   AND a.APRTPRDV IS NOT NULL
   AND a.APRTPRDV <> DATE '2026-10-01'
 GROUP BY TO_CHAR(a.APRTPRDV, 'YYYY-MM')
 ORDER BY 1;

-- BLOQUE 1 — glosa con el mes de devengo.
UPDATE CRD.APRT a
   SET a.APRTGLSA = REPLACE(a.APRTGLSA, 'Mes 10/2026 -',
                            'Mes ' || TO_CHAR(a.APRTPRDV, 'FMMM/YYYY') || ' (pagado en 10/2026) -')
 WHERE a.APRTIDAS = 492
   AND a.APRTPRDV IS NOT NULL
   AND a.APRTPRDV <> DATE '2026-10-01'
   AND a.APRTGLSA LIKE '%Mes 10/2026 -%';

COMMIT;

-- CONTROL 1 — después: los 5 partícipes del 337, cada fila con su mes en la glosa.
SELECT e.ENTDRLPC AS ROL, a.APRTPRDV, a.APRTVLRR, a.APRTGLSA
  FROM CRD.APRT a
  JOIN CRD.ENTD e ON e.ENTDCDGO = a.ENTDCDGO
 WHERE a.APRTIDAS = 492
   AND e.ENTDRLPC IN (1625, 60274, 10950, 10036, 6127)
 ORDER BY e.ENTDRLPC, a.APRTPRDV;

-- CONTROL 2 — no queda ninguna fila de devengo distinto de octubre con la glosa vieja (esperado 0).
SELECT COUNT(*) AS PENDIENTES
  FROM CRD.APRT a
 WHERE a.APRTIDAS = 492
   AND a.APRTPRDV IS NOT NULL
   AND a.APRTPRDV <> DATE '2026-10-01'
   AND a.APRTGLSA LIKE '%Mes 10/2026 -%';

-- REVERSO (comentado): vuelve a la glosa original.
-- UPDATE CRD.APRT a
--    SET a.APRTGLSA = REPLACE(a.APRTGLSA,
--          'Mes ' || TO_CHAR(a.APRTPRDV, 'FMMM/YYYY') || ' (pagado en 10/2026) -', 'Mes 10/2026 -')
--  WHERE a.APRTIDAS = 492 AND a.APRTGLSA LIKE '%(pagado en 10/2026)%';
-- COMMIT;
