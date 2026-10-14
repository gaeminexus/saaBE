-- =====================================================================================
-- 328 — MEDICION: cuentas, plantillas y saldos de la PROVISION DE CARTERA
-- Equipo omen-saa-1 · 2026-10-13 · SOLO LECTURA (ningun bloque escribe)
--
-- Por que: DISENO-GARANTIA-BIEN-Y-PROVISION-CARTERA.md §4 (PV1-PV7). La provision de cartera
-- (por calificacion de riesgo) se calcula para el G48 pero ningun asiento la registra. Antes
-- de disenar el paso 8 del cierre hace falta saber:
--   - que cuentas de provision / gasto / recuperacion existen en el plan de cuentas;
--   - si ya hay alguna plantilla de provision de cartera;
--   - si hoy se provisiona a mano, cuanto y cada cuanto (saldo y movimientos);
--   - la escala de calificacion vigente con sus porcentajes.
-- Empresa 1236. Correr de corrido y devolver al arbitro la salida de los 5 bloques.
-- =====================================================================================


-- BLOQUE 1 — Cuentas candidatas del plan de cuentas.
SELECT c.PLNNCDGO  AS ID_CUENTA,
       c.PLNNCNTA  AS CUENTA,
       c.PLNNNMBR  AS NOMBRE,
       c.PLNNNVLL  AS NIVEL,
       c.PLNNTPOO  AS TIPO,
       c.PLNNESTD  AS ESTADO
  FROM CNT.PLNN c
 WHERE c.PJRQCDGO = 1236
   AND (   c.PLNNCNTA LIKE '1.3.99%'
        OR UPPER(c.PLNNNMBR) LIKE '%PROVISI%'
        OR UPPER(c.PLNNNMBR) LIKE '%RECUPERACI%'
        OR UPPER(c.PLNNNMBR) LIKE '%INCOBRAB%')
 ORDER BY c.PLNNCNTA;


-- BLOQUE 2 — Plantillas que ya hablan de provision (por nombre o por apuntar a esas cuentas).
SELECT p.PLNSCDAL   AS ALTERNO,
       p.PLNSNMBR   AS PLANTILLA,
       p.PJRQCDGO   AS EMPRESA,
       p.PLNSESTD   AS ESTADO_PLANTILLA,
       d.DTPLAXL1   AS AUX1,
       d.DTPLAXL2   AS AUX2,
       DECODE(d.DTPLMVMN, 1, 'DEBE', 2, 'HABER', d.DTPLMVMN) AS MOVIMIENTO,
       c.PLNNCNTA   AS CUENTA,
       c.PLNNNMBR   AS NOMBRE_CUENTA,
       d.DTPLESTD   AS ESTADO_LINEA
  FROM CNT.PLNS p
  JOIN CNT.DTPL d ON d.PLNSCDGO = p.PLNSCDGO
  LEFT JOIN CNT.PLNN c ON c.PLNNCDGO = d.PLNNCDGO
 WHERE UPPER(p.PLNSNMBR) LIKE '%PROVISI%'
    OR c.PLNNCNTA LIKE '1.3.99%'
    OR UPPER(c.PLNNNMBR) LIKE '%PROVISI%'
 ORDER BY p.PLNSCDAL, d.DTPLAXL1, d.DTPLAXL2, d.DTPLMVMN;


-- BLOQUE 3 — Saldo contable actual de las cuentas de provision (asientos no anulados ni incompletos).
--   Si hay saldo, hoy se provisiona a mano: la primera corrida del paso 8 solo ajusta la diferencia.
SELECT c.PLNNCNTA                         AS CUENTA,
       c.PLNNNMBR                         AS NOMBRE,
       ROUND(SUM(NVL(d.DTASDBEE, 0)), 2)  AS TOTAL_DEBE,
       ROUND(SUM(NVL(d.DTASHBRR, 0)), 2)  AS TOTAL_HABER,
       ROUND(SUM(NVL(d.DTASDBEE, 0) - NVL(d.DTASHBRR, 0)), 2) AS SALDO_DEUDOR,
       COUNT(DISTINCT a.ASNTCDGO)         AS ASIENTOS,
       MIN(a.ASNTFCHA)                    AS PRIMER_MOVIMIENTO,
       MAX(a.ASNTFCHA)                    AS ULTIMO_MOVIMIENTO
  FROM CNT.DTAS d
  JOIN CNT.ASNT a ON a.ASNTCDGO = d.ASNTCDGO
  JOIN CNT.PLNN c ON c.PLNNCDGO = d.PLNNCDGO
 WHERE a.PJRQCDGO = 1236
   AND a.ASNTESTD NOT IN (2, 4)
   AND (c.PLNNCNTA LIKE '1.3.99%' OR UPPER(c.PLNNNMBR) LIKE '%PROVISI%')
 GROUP BY c.PLNNCNTA, c.PLNNNMBR
 ORDER BY c.PLNNCNTA;


-- BLOQUE 4 — Como se movieron esas cuentas en los ultimos 24 meses, por mes
--   (dice si hoy se provisiona mensual, trimestral o nunca).
SELECT TO_CHAR(a.ASNTFCHA, 'YYYY-MM')     AS MES,
       c.PLNNCNTA                         AS CUENTA,
       ROUND(SUM(NVL(d.DTASDBEE, 0)), 2)  AS DEBE,
       ROUND(SUM(NVL(d.DTASHBRR, 0)), 2)  AS HABER,
       COUNT(DISTINCT a.ASNTCDGO)         AS ASIENTOS,
       MIN(a.ASNTUSRO)                    AS UN_USUARIO
  FROM CNT.DTAS d
  JOIN CNT.ASNT a ON a.ASNTCDGO = d.ASNTCDGO
  JOIN CNT.PLNN c ON c.PLNNCDGO = d.PLNNCDGO
 WHERE a.PJRQCDGO = 1236
   AND a.ASNTESTD NOT IN (2, 4)
   AND a.ASNTFCHA >= ADD_MONTHS(TRUNC(SYSDATE, 'MM'), -24)
   AND (c.PLNNCNTA LIKE '1.3.99%' OR UPPER(c.PLNNNMBR) LIKE '%PROVISI%')
 GROUP BY TO_CHAR(a.ASNTFCHA, 'YYYY-MM'), c.PLNNCNTA
 ORDER BY 1, 2;


-- BLOQUE 5 — Escala de calificacion vigente (la que usa el G48) con su % de provision.
SELECT f.CFCRCDGO  AS ID_CONFIG,
       f.CFCRNMBR  AS CONFIGURACION,
       f.CFCRFCIN  AS VIGENTE_DESDE,
       f.CFCRFCFN  AS VIGENTE_HASTA,
       e.ESCRORDN  AS ORDEN,
       e.ESCRCLFC  AS CALIFICACION,
       e.ESCRDSDE  AS DIAS_DESDE,
       e.ESCRHSTA  AS DIAS_HASTA,
       e.ESCRPRVS  AS PORC_PROVISION
  FROM CRD.CFCR f
  JOIN CRD.ESCR e ON e.CFCRCDGO = f.CFCRCDGO
 WHERE NVL(f.CFCRESTD, 1) = 1
   AND NVL(e.ESCRESTD, 1) = 1
 ORDER BY f.CFCRCDGO, e.ESCRORDN;
-- Si este bloque falla por una columna (ESCR.CFCRCDGO), devolver el error tal cual:
-- el enlace ESCR -> CFCR se toma de la entidad y no se ha medido contra la base.
