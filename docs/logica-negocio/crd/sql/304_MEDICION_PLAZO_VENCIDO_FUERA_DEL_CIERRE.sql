-- =====================================================================================
-- 304 — MEDICION: cuanto pesan los prestamos DE PLAZO VENCIDO (8) que el cierre de cartera no ve
-- FECHA: 2026-09-30 · EQUIPO: omen-saa-1 (CRD · EQUIPO B) · solo lectura
--
-- CONTEXTO (H80, decision D27 del usuario): el cierre de cartera solo toma PRSTIDST IN (2, 11)
-- (CierreCarteraDaoServiceImpl:44-45). El usuario decidio que los de plazo vencido TAMBIEN entren.
-- El cambio de codigo es de una linea; lo delicado es la TRANSICION, porque hay dos grupos de 8:
--
--   A. Los DECLARADOS en la pantalla nueva (CRD.PLVN vivas). Eran 11 cuando corrio el ultimo cierre
--      (agosto, que abrio septiembre): su apertura de septiembre SI se hizo. Si el cierre de
--      septiembre los excluye, quedan abiertos y nunca neteados. Incluirlos CORRIGE eso.
--   B. Los HISTORICOS en 8 (puestos a mano / migracion, sin fila viva en PLVN). El cierre NUNCA los
--      vio: nunca se abrieron, nunca se bandearon. Incluirlos de golpe hace que el neteo de
--      septiembre cierre algo que nunca se abrio, y que el pase de bandas mueva capital que nunca
--      se cargo a ninguna banda. Eso pide un tratamiento de transicion que decide el CONTADOR.
--
-- Este script dice cuantos hay en cada grupo y cuanto suman. SQL PURO. Pasar la salida al arbitro.
-- =====================================================================================

-- 0. Las ultimas corridas del cierre (para saber que mes abrio cada una).
SELECT c.CRCTCDGO, c.CRCTANOO, c.CRCTMESS, c.CRCTFCCR AS CORTE, c.CRCTFCPR AS PROCESO, c.CRCTESTD, c.CRCTIDST
  FROM CRD.CRCT c
 ORDER BY c.CRCTFCCR DESC
 FETCH FIRST 4 ROWS ONLY;


-- 1. Los prestamos en 8, separados en A (declarados con la pantalla, PLVN viva) y B (historicos),
--    con su capital pendiente y su mora pendiente al dia de hoy.
WITH pg AS (
    SELECT x.DTPRCDGO,
           SUM(NVL(x.PGPRCPPG, 0)) AS CAP,
           SUM(NVL(x.PGPRMRPG, 0)) AS MOR,
           SUM(NVL(x.PGPRINPG, 0)) AS INTR
      FROM CRD.PGPR x WHERE NVL(x.PGPRANUL, 0) = 0 GROUP BY x.DTPRCDGO
),
ocho AS (
    SELECT p.PRSTCDGO,
           CASE WHEN EXISTS (SELECT 1 FROM CRD.PLVN v WHERE v.PRSTCDGO = p.PRSTCDGO AND v.PLVNESTD IN (1, 2))
                THEN 'A DECLARADO EN PANTALLA' ELSE 'B HISTORICO' END AS GRUPO,
           (SELECT MIN(v.PLVNFCDC) FROM CRD.PLVN v WHERE v.PRSTCDGO = p.PRSTCDGO AND v.PLVNESTD IN (1, 2)) AS DECLARADO_EL
      FROM CRD.PRST p
     WHERE p.PRSTIDST = 8
)
SELECT o.GRUPO,
       COUNT(DISTINCT o.PRSTCDGO)                                                    AS PRESTAMOS,
       MIN(o.DECLARADO_EL)                                                           AS PRIMERA_DECLARACION,
       MAX(o.DECLARADO_EL)                                                           AS ULTIMA_DECLARACION,
       ROUND(SUM(GREATEST(0, NVL(d.DTPRCPTL,0) - NVL(g.CAP,0))), 2)                  AS CAPITAL_PENDIENTE,
       ROUND(SUM(CASE WHEN d.DTPRFCVN < TRUNC(SYSDATE)
                      THEN GREATEST(0, NVL(d.DTPRCPTL,0) - NVL(g.CAP,0)) ELSE 0 END), 2) AS CAPITAL_YA_VENCIDO,
       ROUND(SUM(GREATEST(0, NVL(d.DTPRINTR,0) - NVL(g.INTR,0))), 2)                 AS INTERES_PENDIENTE,
       ROUND(SUM(GREATEST(0, NVL(d.DTPRMRAA,0) - NVL(g.MOR,0))), 2)                  AS MORA_PENDIENTE
  FROM ocho o
  JOIN CRD.DTPR d ON d.PRSTCDGO = o.PRSTCDGO
  LEFT JOIN pg g  ON g.DTPRCDGO = d.DTPRCDGO
 WHERE (d.DTPRESTD IS NULL OR d.DTPRESTD NOT IN (4, 7))
 GROUP BY o.GRUPO
 ORDER BY o.GRUPO;


-- 2. Detalle del grupo B (historicos): uno por prestamo, para que el contador vea de que se trata.
WITH pg AS (
    SELECT x.DTPRCDGO, SUM(NVL(x.PGPRCPPG, 0)) AS CAP
      FROM CRD.PGPR x WHERE NVL(x.PGPRANUL, 0) = 0 GROUP BY x.DTPRCDGO
)
SELECT NVL(p.PRSTIDAS, p.PRSTCDGO) AS PRESTAMO, p.PRSTFCMD AS ULTIMA_MODIFICACION,
       ROUND(SUM(GREATEST(0, NVL(d.DTPRCPTL,0) - NVL(g.CAP,0))), 2) AS CAPITAL_PENDIENTE,
       MIN(CASE WHEN d.DTPRESTD IS NULL OR d.DTPRESTD NOT IN (4, 7) THEN d.DTPRFCVN END) AS PRIMER_VENCIMIENTO_IMPAGO
  FROM CRD.PRST p
  JOIN CRD.DTPR d ON d.PRSTCDGO = p.PRSTCDGO
  LEFT JOIN pg g  ON g.DTPRCDGO = d.DTPRCDGO
 WHERE p.PRSTIDST = 8
   AND NOT EXISTS (SELECT 1 FROM CRD.PLVN v WHERE v.PRSTCDGO = p.PRSTCDGO AND v.PLVNESTD IN (1, 2))
   AND (d.DTPRESTD IS NULL OR d.DTPRESTD NOT IN (4, 7))
 GROUP BY NVL(p.PRSTIDAS, p.PRSTCDGO), p.PRSTFCMD
 ORDER BY 3 DESC;
