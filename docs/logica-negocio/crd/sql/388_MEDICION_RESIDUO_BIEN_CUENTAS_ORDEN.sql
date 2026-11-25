-- =====================================================================================
-- 388 — MEDICIÓN (SOLO LECTURA): residuo del bien en cuentas de orden 7.3/7.4 por préstamo (3.9 del cuadre, DS-08)
-- Equipo omen-saa-1 · 2026-11-24 · No modifica nada. Se puede correr en producción.
--
-- El bien ENTRA al registrar la entrega por PRSTVLAS (valor asegurado; ContabilidadPrestamoServiceImpl, línea «bien en
-- garantía (orden)») y SALE al terminar el préstamo por PRSTVLGR (avalúo) si es > 0, si no por PRSTVLAS
-- (GarantiaOrdenPrestamoServiceImpl.darDeBaja, línea «bien en garantía»). Si el valor cambió entre la entrada y la salida
-- (la suma asegurada se actualiza por póliza, o se cargó un avalúo distinto), queda un residuo en 7.3/7.4.
--
-- No hay una columna que ligue la línea al préstamo: se reconoce por la descripción que arma el código:
--   entrada: «Entrega préstamo {PRSTCDGO} - bien en garantía (orden)»   (HABER)
--   salida : «[C31 PRST {PRSTCDGO}] ... - bien en garantía»              (DEBE)
-- Solo asientos ACTIVOS (ASNTESTD = 1). Un asiento reversado en contabilidad por fuera del reverso del módulo no se cuenta.
-- =====================================================================================


-- BLOQUE 1 — Residuo por préstamo terminado (entrada − salida). Esperado después del arreglo: 0 en todos.
WITH lineas AS (
    SELECT d.DTASDSCR, NVL(d.DTASDBEE, 0) AS debe, NVL(d.DTASHBRR, 0) AS haber
      FROM CNT.DTAS d
      JOIN CNT.ASNT a ON a.ASNTCDGO = d.ASNTCDGO AND a.ASNTESTD = 1
     WHERE d.DTASDSCR LIKE '%bien en garant%a%'
),
entrada AS (
    SELECT TO_NUMBER(REGEXP_SUBSTR(DTASDSCR, '^Entrega pr.stamo ([0-9]+) - bien', 1, 1, 'i', 1)) AS prestamo,
           SUM(haber) - SUM(debe) AS valor
      FROM lineas WHERE REGEXP_LIKE(DTASDSCR, '^Entrega pr.stamo [0-9]+ - bien', 'i')
     GROUP BY TO_NUMBER(REGEXP_SUBSTR(DTASDSCR, '^Entrega pr.stamo ([0-9]+) - bien', 1, 1, 'i', 1))
),
salida AS (
    SELECT TO_NUMBER(REGEXP_SUBSTR(DTASDSCR, '^\[C31 PRST ([0-9]+)\]', 1, 1, 'i', 1)) AS prestamo,
           SUM(debe) - SUM(haber) AS valor
      FROM lineas WHERE REGEXP_LIKE(DTASDSCR, '^\[C31 PRST [0-9]+\]', 'i')
     GROUP BY TO_NUMBER(REGEXP_SUBSTR(DTASDSCR, '^\[C31 PRST ([0-9]+)\]', 1, 1, 'i', 1))
)
SELECT NVL(p.PRSTIDAS, p.PRSTCDGO) AS operacion, p.PRSTIDST AS estado, p.PRSTVLAS AS valor_asegurado_hoy,
       p.PRSTVLGR AS avaluo_hoy, ROUND(NVL(e.valor, 0), 2) AS entro, ROUND(NVL(s.valor, 0), 2) AS salio,
       ROUND(NVL(e.valor, 0) - NVL(s.valor, 0), 2) AS residuo
  FROM CRD.PRST p
  LEFT JOIN entrada e ON e.prestamo = p.PRSTCDGO
  LEFT JOIN salida  s ON s.prestamo = p.PRSTCDGO
 WHERE p.PRSTIDST IN (3, 4, 5, 9, 13)
   AND ABS(NVL(e.valor, 0) - NVL(s.valor, 0)) >= 0.005
 ORDER BY ABS(NVL(e.valor, 0) - NVL(s.valor, 0)) DESC;


-- BLOQUE 2 — Total del residuo, por estado del préstamo, y cuántos son por avalúo distinto de la suma asegurada.
WITH lineas AS (
    SELECT d.DTASDSCR, NVL(d.DTASDBEE, 0) AS debe, NVL(d.DTASHBRR, 0) AS haber
      FROM CNT.DTAS d JOIN CNT.ASNT a ON a.ASNTCDGO = d.ASNTCDGO AND a.ASNTESTD = 1
     WHERE d.DTASDSCR LIKE '%bien en garant%a%'
),
entrada AS (
    SELECT TO_NUMBER(REGEXP_SUBSTR(DTASDSCR, '^Entrega pr.stamo ([0-9]+) - bien', 1, 1, 'i', 1)) AS prestamo,
           SUM(haber) - SUM(debe) AS valor
      FROM lineas WHERE REGEXP_LIKE(DTASDSCR, '^Entrega pr.stamo [0-9]+ - bien', 'i')
     GROUP BY TO_NUMBER(REGEXP_SUBSTR(DTASDSCR, '^Entrega pr.stamo ([0-9]+) - bien', 1, 1, 'i', 1))
),
salida AS (
    SELECT TO_NUMBER(REGEXP_SUBSTR(DTASDSCR, '^\[C31 PRST ([0-9]+)\]', 1, 1, 'i', 1)) AS prestamo,
           SUM(debe) - SUM(haber) AS valor
      FROM lineas WHERE REGEXP_LIKE(DTASDSCR, '^\[C31 PRST [0-9]+\]', 'i')
     GROUP BY TO_NUMBER(REGEXP_SUBSTR(DTASDSCR, '^\[C31 PRST ([0-9]+)\]', 1, 1, 'i', 1))
)
SELECT p.PRSTIDST AS estado, COUNT(*) AS prestamos,
       SUM(CASE WHEN NVL(p.PRSTVLGR, 0) > 0 AND ABS(NVL(p.PRSTVLGR, 0) - NVL(p.PRSTVLAS, 0)) >= 0.005 THEN 1 ELSE 0 END) AS con_avaluo_distinto,
       ROUND(SUM(NVL(e.valor, 0) - NVL(s.valor, 0)), 2) AS residuo_total
  FROM CRD.PRST p
  LEFT JOIN entrada e ON e.prestamo = p.PRSTCDGO
  LEFT JOIN salida  s ON s.prestamo = p.PRSTCDGO
 WHERE p.PRSTIDST IN (3, 4, 5, 9, 13) AND ABS(NVL(e.valor, 0) - NVL(s.valor, 0)) >= 0.005
 GROUP BY p.PRSTIDST ORDER BY 1;


-- BLOQUE 3 — Préstamos VIVOS prendarios/hipotecarios cuyo valor asegurado de hoy ya no es lo que entró (el residuo futuro:
-- la baja saldrá por el valor de hoy y no por el de la entrada).
WITH lineas AS (
    SELECT d.DTASDSCR, NVL(d.DTASDBEE, 0) AS debe, NVL(d.DTASHBRR, 0) AS haber
      FROM CNT.DTAS d JOIN CNT.ASNT a ON a.ASNTCDGO = d.ASNTCDGO AND a.ASNTESTD = 1
     WHERE d.DTASDSCR LIKE 'Entrega pr%stamo % - bien en garant%'
),
entrada AS (
    SELECT TO_NUMBER(REGEXP_SUBSTR(DTASDSCR, '^Entrega pr.stamo ([0-9]+) - bien', 1, 1, 'i', 1)) AS prestamo,
           SUM(haber) - SUM(debe) AS valor
      FROM lineas GROUP BY TO_NUMBER(REGEXP_SUBSTR(DTASDSCR, '^Entrega pr.stamo ([0-9]+) - bien', 1, 1, 'i', 1))
)
SELECT NVL(p.PRSTIDAS, p.PRSTCDGO) AS operacion, p.PRSTIDST AS estado, ROUND(e.valor, 2) AS entro,
       p.PRSTVLAS AS valor_asegurado_hoy, p.PRSTVLGR AS avaluo_hoy,
       ROUND(CASE WHEN NVL(p.PRSTVLGR, 0) > 0 THEN p.PRSTVLGR ELSE NVL(p.PRSTVLAS, 0) END - e.valor, 2) AS saldria_de_mas
  FROM CRD.PRST p JOIN entrada e ON e.prestamo = p.PRSTCDGO
 WHERE p.PRSTIDST IN (2, 8, 10, 11)
   AND ABS(CASE WHEN NVL(p.PRSTVLGR, 0) > 0 THEN p.PRSTVLGR ELSE NVL(p.PRSTVLAS, 0) END - e.valor) >= 0.005
 ORDER BY ABS(CASE WHEN NVL(p.PRSTVLGR, 0) > 0 THEN p.PRSTVLGR ELSE NVL(p.PRSTVLAS, 0) END - e.valor) DESC;


-- BLOQUE 4 — Préstamos prendarios/hipotecarios nuevos sin avalúo capturado (PRSTVLGR nulo o 0): son los que hoy saldrían
-- por la suma asegurada. Cuenta por estado y por año de concesión.
SELECT p.PRSTIDST AS estado, TO_CHAR(p.PRSTFCHA, 'YYYY') AS anio, COUNT(*) AS prestamos,
       SUM(CASE WHEN NVL(p.PRSTVLAS, 0) <= 0 THEN 1 ELSE 0 END) AS sin_valor_asegurado
  FROM CRD.PRST p
  JOIN CRD.PRDC pr ON pr.PRDCCDGO = p.PRDCCDGO
  JOIN CRD.TPPR t ON t.TPPRCDGO = pr.TPPRCDGO AND UPPER(t.TPPRNMBR) IN ('PRENDARIO', 'HIPOTECARIO')
 WHERE NVL(p.PRSTVLGR, 0) <= 0 AND p.PRSTIDST NOT IN (0, 1, 6, 7, 12)
 GROUP BY p.PRSTIDST, TO_CHAR(p.PRSTFCHA, 'YYYY') ORDER BY 2, 1;


-- BLOQUE 0 (CONTROL PREVIO) — ¿Existe la columna PRSTVLGR (avalúo) en CRD.PRST? Esperado: 1 fila (NUMBER(15,2)).
-- Si devuelve 0 filas, la entidad Prestamo la mapea igual y TODA lectura de CRD.PRST revienta con ORA-00904: hay que
-- correr antes ALTER TABLE CRD.PRST ADD (PRSTVLGR NUMBER(15,2)) (ver el javadoc de Prestamo.valorGarantia). Los bloques 1-4
-- de arriba leen esta columna.
SELECT c.OWNER, c.TABLE_NAME, c.COLUMN_NAME, c.DATA_TYPE, c.DATA_PRECISION, c.DATA_SCALE, c.NULLABLE
  FROM ALL_TAB_COLUMNS c
 WHERE c.OWNER = 'CRD' AND c.TABLE_NAME = 'PRST' AND c.COLUMN_NAME = 'PRSTVLGR';
