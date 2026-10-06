-- =====================================================================================
-- 315 — MEDICION (solo lectura): cuotas futuras con interes de los prestamos DE PLAZO VENCIDO (8)
-- FECHA: 2026-10-05 · EQUIPO: omen-saa-1 (CRD · EQUIPO B)
--
-- D27: el cierre de septiembre / apertura de octubre ya incluye al estado 8. El interes de las cuotas
-- que vencen DESPUES de la declaracion (PLVNFCCR) se excluye del devengo, de la provision y del cobrable
-- (D25: el interes va solo hasta la fecha de corte de la declaracion). PERO esa exclusion necesita una
-- declaracion viva en CRD.PLVN. Los historicos (grupo B) pueden no tenerla, y entonces su interes
-- futuro entraria al devengo de octubre. Este script mide cuanto es.
-- =====================================================================================

-- 1. Por grupo: prestamos en 8 con y sin declaracion viva, y su interes en cuotas no pagadas que
--    vencen desde el 01-10-2026 (lo que el devengo de octubre y los cierres siguientes tomarian).
SELECT CASE WHEN EXISTS (SELECT 1 FROM CRD.PLVN v WHERE v.PRSTCDGO = p.PRSTCDGO AND v.PLVNESTD IN (1, 2))
            THEN 'A_CON_DECLARACION' ELSE 'B_SIN_DECLARACION' END                AS GRUPO,
       COUNT(DISTINCT p.PRSTCDGO)                                                 AS PRESTAMOS,
       COUNT(d.DTPRCDGO)                                                          AS CUOTAS_FUTURAS,
       ROUND(SUM(NVL(d.DTPRINTR, 0)), 2)                                          AS INTERES_FUTURO,
       ROUND(SUM(CASE WHEN d.DTPRFCVN < DATE '2026-11-01' THEN NVL(d.DTPRINTR,0) ELSE 0 END), 2) AS INTERES_OCTUBRE
  FROM CRD.PRST p
  LEFT JOIN CRD.DTPR d ON d.PRSTCDGO = p.PRSTCDGO
                      AND NVL(d.DTPRESTD, 0) NOT IN (4, 7)
                      AND d.DTPRFCVN >= DATE '2026-10-01'
 WHERE p.PRSTIDST = 8
 GROUP BY CASE WHEN EXISTS (SELECT 1 FROM CRD.PLVN v WHERE v.PRSTCDGO = p.PRSTCDGO AND v.PLVNESTD IN (1, 2))
               THEN 'A_CON_DECLARACION' ELSE 'B_SIN_DECLARACION' END
 ORDER BY 1;

-- 2. Los estados de PLVN que existen (para confirmar que 1 y 2 son DECLARADA y LIQUIDADA).
SELECT v.PLVNESTD, COUNT(*) FROM CRD.PLVN v GROUP BY v.PLVNESTD ORDER BY 1;

-- 3. Prestamos con MAS de una declaracion viva (deberia dar 0 filas).
SELECT v.PRSTCDGO, COUNT(*) FROM CRD.PLVN v WHERE v.PLVNESTD IN (1, 2)
 GROUP BY v.PRSTCDGO HAVING COUNT(*) > 1;
