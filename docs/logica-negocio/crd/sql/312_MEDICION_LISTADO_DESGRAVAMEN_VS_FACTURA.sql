-- =====================================================================================
-- 312 — MEDICION: diferencias entre el listado de desgravamen del sistema y lo facturado (solo lectura)
-- FECHA: 2026-10-05 · EQUIPO: omen-saa-1 (CRD · EQUIPO B)
--
-- El usuario cruzo el listado del sistema contra la factura de la aseguradora (Excel
-- «valicaion_de_seguro_desgravamen»):
--   HOJA 1 — en el listado y NO facturados (12): casi todos con una BASE minuscula (0,14 · 0,56 · 0,15…)
--            frente a un «saldo capital» del sistema mucho mayor (0,31 · 72,82 · 575,27…).
--   HOJA 2 — facturados y NO en el listado (23): casi todos EMERGENTE, en mora o vigentes.
--
-- La base del listado es Σ(capital − capital pagado en PGPR) de las cuotas NO pagadas
-- (DocumentoSeguroServiceImpl:192-215). Este script la reconstruye y la contrasta con otras dos
-- medidas de «saldo» para ver cual usa la aseguradora y donde esta la diferencia.
-- =====================================================================================

-- 1. HOJA 1: por que la base sale casi en cero. Por prestamo, tres medidas del saldo de capital.
WITH pg AS (
    SELECT x.DTPRCDGO, SUM(NVL(x.PGPRCPPG, 0)) AS CAP
      FROM CRD.PGPR x WHERE NVL(x.PGPRANUL, 0) = 0 GROUP BY x.DTPRCDGO
)
SELECT NVL(p.PRSTIDAS, p.PRSTCDGO)                                                       AS PRESTAMO,
       p.PRSTIDST                                                                        AS ESTADO,
       COUNT(*)                                                                          AS CUOTAS,
       SUM(CASE WHEN d.DTPRESTD = 4 THEN 1 ELSE 0 END)                                   AS PAGADAS,
       SUM(CASE WHEN NVL(d.DTPRESTD,0) NOT IN (4,7) THEN 1 ELSE 0 END)                   AS NO_PAGADAS,
       -- (a) la base del listado: no pagadas, capital − PGPR, con piso por cuota
       ROUND(SUM(CASE WHEN NVL(d.DTPRESTD,0) NOT IN (4,7)
                      THEN GREATEST(0, NVL(d.DTPRCPTL,0) - NVL(g.CAP,0)) ELSE 0 END), 2)  AS BASE_LISTADO,
       -- (b) capital de las no pagadas, sin restar pagos
       ROUND(SUM(CASE WHEN NVL(d.DTPRESTD,0) NOT IN (4,7) THEN NVL(d.DTPRCPTL,0) ELSE 0 END), 2) AS CAPITAL_NO_PAGADAS,
       -- (c) capital pagado por PGPR sobre cuotas NO pagadas (lo que hace bajar la base)
       ROUND(SUM(CASE WHEN NVL(d.DTPRESTD,0) NOT IN (4,7) THEN NVL(g.CAP,0) ELSE 0 END), 2) AS PAGADO_EN_NO_PAGADAS,
       -- (d) saldo inicial de la primera cuota no pagada (lo que muestra «saldo insoluto»)
       (SELECT d2.DTPRSICP FROM CRD.DTPR d2 WHERE d2.PRSTCDGO = p.PRSTCDGO
          AND NVL(d2.DTPRESTD,0) NOT IN (4,7)
          ORDER BY d2.DTPRNMCT FETCH FIRST 1 ROW ONLY)                                   AS SALDO_INICIAL_PRIMERA_NO_PAGADA
  FROM CRD.PRST p
  JOIN CRD.DTPR d ON d.PRSTCDGO = p.PRSTCDGO
  LEFT JOIN pg g ON g.DTPRCDGO = d.DTPRCDGO
 WHERE NVL(p.PRSTIDAS, p.PRSTCDGO) IN (69663, 70858, 70003, 69814, 66787, 68989, 70002, 71029, 57598, 62953, 68986, 69728)
 GROUP BY NVL(p.PRSTIDAS, p.PRSTCDGO), p.PRSTIDST, p.PRSTCDGO
 ORDER BY 1;

-- 1b. HOJA 1, cuota por cuota del caso mas raro (57598: base 0,15 contra 575,27).
WITH pg AS (
    SELECT x.DTPRCDGO, SUM(NVL(x.PGPRCPPG, 0)) AS CAP, COUNT(*) AS PAGOS
      FROM CRD.PGPR x WHERE NVL(x.PGPRANUL, 0) = 0 GROUP BY x.DTPRCDGO
)
SELECT d.DTPRNMCT AS CUOTA, d.DTPRESTD AS ESTADO, d.DTPRFCVN AS VENCE, d.DTPRCPTL AS CAPITAL,
       NVL(g.CAP,0) AS CAPITAL_PAGADO_PGPR, NVL(g.PAGOS,0) AS PAGOS, d.DTPRSICP AS SALDO_INICIAL
  FROM CRD.PRST p JOIN CRD.DTPR d ON d.PRSTCDGO = p.PRSTCDGO
  LEFT JOIN pg g ON g.DTPRCDGO = d.DTPRCDGO
 WHERE NVL(p.PRSTIDAS, p.PRSTCDGO) = 57598
   AND NVL(d.DTPRESTD,0) NOT IN (4,7)
 ORDER BY d.DTPRNMCT;


-- 2. HOJA 2: por que no estan en el listado. Estado HOY, producto, y si estan en algun listado de desgravamen.
SELECT NVL(p.PRSTIDAS, p.PRSTCDGO) AS PRESTAMO, p.PRSTCDGO, p.PRSTIDST AS ESTADO_HOY,
       pr.PRDCNMBR AS PRODUCTO, p.PRSTFCMD AS ULTIMA_MODIFICACION,
       (SELECT LISTAGG(s.POSGCDGO || ':' || s.POSGESTD || ':' || TO_CHAR(s.POSGFCCT,'YYYY-MM-DD'), ', ')
               WITHIN GROUP (ORDER BY s.POSGCDGO)
          FROM CRD.PSPR x JOIN CRD.POSG s ON s.POSGCDGO = x.POSGCDGO
         WHERE x.PRSTCDGO = p.PRSTCDGO AND s.POSGTPSG = 1)       AS EN_LISTADOS_DESGRAVAMEN
  FROM CRD.PRST p
  LEFT JOIN CRD.PRDC pr ON pr.PRDCCDGO = p.PRDCCDGO
 WHERE NVL(p.PRSTIDAS, p.PRSTCDGO) IN (69864, 68773, 70494, 71035, 70481, 71152, 70900, 70777, 69286, 71161,
                                       69346, 69808, 70709, 70539, 70447, 69288, 68509, 67728, 70717, 70724,
                                       70997, 69304, 70262)
 ORDER BY 1;

-- 2b. Los listados de desgravamen que existen: cuantos prestamos tiene cada uno y su fecha de corte.
SELECT s.POSGCDGO, s.POSGESTD, s.POSGFCCT, s.POSGFCRG, COUNT(x.PSPRCDGO) AS PRESTAMOS
  FROM CRD.POSG s LEFT JOIN CRD.PSPR x ON x.POSGCDGO = s.POSGCDGO
 WHERE s.POSGTPSG = 1
 GROUP BY s.POSGCDGO, s.POSGESTD, s.POSGFCCT, s.POSGFCRG
 ORDER BY s.POSGCDGO;
