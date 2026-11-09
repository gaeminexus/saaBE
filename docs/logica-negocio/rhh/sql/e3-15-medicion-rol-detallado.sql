-- =====================================================================================
-- e3-15 — Medición para el reporte RPRT_ROLL_DTLL (rol de pagos detallado)
-- Equipo omen-saa-3 · 2026-11-09 · SOLO LECTURA (ningún INSERT/UPDATE/DELETE)
-- Diseño: docs/logica-negocio/reportes/REPORTE-ROL-PAGO-DETALLADO.md
--
-- Se puede correr de corrido en producción. Cada consulta es independiente.
-- El período por defecto es el ÚLTIMO de RHH.PRDN (año/mes más reciente). Para otro,
-- cambiar la línea marcada  >>> PERIODO  en las consultas 4 y 5.
-- =====================================================================================

-- -------------------------------------------------------------------------------------
-- 1. ¿Cuántos conceptos con valor hay por bloque en los últimos 6 períodos?
--    El reporte tiene 12 casillas por bloque; con más de 12, la 12 pasa a «OTROS».
--    Esperado: INGRESOS y EGRESOS con 12 o menos. Si alguno pasa de 12, avisar.
-- -------------------------------------------------------------------------------------
SELECT p.PRDNCDGO, p.PRDNANOO, p.PRDNMSEE,
       CASE r.RNGLTPCN WHEN 1 THEN 'INGRESOS' WHEN 2 THEN 'EGRESOS' END AS BLOQUE,
       COUNT(DISTINCT NVL(TO_CHAR(r.CPNMCDGO), 'D:' || r.RNGLDSCR))  AS CONCEPTOS_CON_VALOR
FROM   RHH.PRDN p
       JOIN RHH.NMNA n ON n.PRDNCDGO = p.PRDNCDGO
       JOIN RHH.RNGL r ON r.NMNACDGO = n.NMNACDGO
WHERE  r.RNGLTPCN IN (1, 2)
  AND  NVL(r.RNGLVLRO, 0) <> 0
  AND  p.PRDNCDGO IN (SELECT PRDNCDGO FROM (SELECT PRDNCDGO FROM RHH.PRDN
                                            ORDER BY PRDNANOO DESC, PRDNMSEE DESC, PRDNCDGO DESC)
                      WHERE ROWNUM <= 6)
GROUP  BY p.PRDNCDGO, p.PRDNANOO, p.PRDNMSEE, r.RNGLTPCN
ORDER  BY p.PRDNANOO DESC, p.PRDNMSEE DESC, BLOQUE DESC;

-- -------------------------------------------------------------------------------------
-- 2. Renglones de valor no pagado (rol de motor 34 retenido / 35 recuperado):
--    ¿con qué tipo de concepto se graban? Si salen con RNGLTPCN 1 o 2 ya están dentro
--    de ingresos/egresos y el NETO A PAGAR los contaría dos veces: avisar.
--    Esperado: ninguna fila, o filas con RNGLTPCN distinto de 1 y 2.
-- -------------------------------------------------------------------------------------
SELECT c.CPNMROLM, r.RNGLTPCN, COUNT(*) AS RENGLONES, SUM(r.RNGLVLRO) AS SUMA
FROM   RHH.RNGL r
       JOIN RHH.CPNM c ON c.CPNMCDGO = r.CPNMCDGO
WHERE  c.CPNMROLM IN (34, 35)
GROUP  BY c.CPNMROLM, r.RNGLTPCN
ORDER  BY c.CPNMROLM, r.RNGLTPCN;

-- -------------------------------------------------------------------------------------
-- 3. Provisiones: tipos y estados presentes en RHH.PVNM.
--    Esperado: PVNMESTD solo 1; tipo 5 (aporte patronal) sin filas. Si hay tipo 5,
--    el reporte NO lo usa (toma NMNAAPPT): avisar para revisar el reporte de provisiones.
-- -------------------------------------------------------------------------------------
SELECT PVNMTPPR AS TIPO, PVNMESTD AS ESTADO, COUNT(*) AS FILAS, SUM(PVNMVLOR) AS SUMA
FROM   RHH.PVNM
GROUP  BY PVNMTPPR, PVNMESTD
ORDER  BY PVNMTPPR, PVNMESTD;

-- -------------------------------------------------------------------------------------
-- 4. Cuadre por colaborador del período: suma de renglones vs. totales de la nómina.
--    Esperado: las columnas DIF_* en 0 en todas las filas. La última fila (TOTAL) es la
--    que se compara contra el pie de cada bloque del PDF.
-- -------------------------------------------------------------------------------------
WITH PER AS (
    SELECT PRDNCDGO FROM (SELECT PRDNCDGO FROM RHH.PRDN
                          ORDER BY PRDNANOO DESC, PRDNMSEE DESC, PRDNCDGO DESC)
    WHERE ROWNUM = 1                                                  -- >>> PERIODO
    -- para otro período:  SELECT <PRDNCDGO> AS PRDNCDGO FROM DUAL
), LIN AS (
    SELECT r.NMNACDGO,
           SUM(CASE WHEN r.RNGLTPCN = 1 THEN r.RNGLVLRO ELSE 0 END) AS ING_RENGLONES,
           SUM(CASE WHEN r.RNGLTPCN = 2 THEN r.RNGLVLRO ELSE 0 END) AS EGR_RENGLONES
    FROM   RHH.RNGL r
    GROUP  BY r.NMNACDGO
)
SELECT NVL(e.MPLDAPLL, ' ') || ' ' || NVL(e.MPLDNMBR, ' ')        AS EMPLEADO,
       SUM(NVL(n.NMNATING, 0))                                    AS NMNATING,
       SUM(NVL(l.ING_RENGLONES, 0))                               AS ING_RENGLONES,
       SUM(NVL(l.ING_RENGLONES, 0) - NVL(n.NMNATING, 0))          AS DIF_ING,
       SUM(NVL(n.NMNATDSC, 0))                                    AS NMNATDSC,
       SUM(NVL(l.EGR_RENGLONES, 0))                               AS EGR_RENGLONES,
       SUM(NVL(l.EGR_RENGLONES, 0) - NVL(n.NMNATDSC, 0))          AS DIF_EGR,
       SUM(NVL(n.NMNANETO, 0))                                    AS NMNANETO,
       SUM(NVL(l.ING_RENGLONES, 0) - NVL(l.EGR_RENGLONES, 0)
           - NVL(n.NMNANETO, 0))                                  AS DIF_NETO
FROM   RHH.NMNA n
       JOIN PER ON PER.PRDNCDGO = n.PRDNCDGO
       JOIN RHH.MPLD e ON e.MPLDCDGO = n.MPLDCDGO
       LEFT JOIN LIN l ON l.NMNACDGO = n.NMNACDGO
GROUP  BY ROLLUP (NVL(e.MPLDAPLL, ' ') || ' ' || NVL(e.MPLDNMBR, ' '))
ORDER  BY GROUPING(NVL(e.MPLDAPLL, ' ') || ' ' || NVL(e.MPLDNMBR, ' ')), EMPLEADO;

-- -------------------------------------------------------------------------------------
-- 5. El asiento de la nómina del período (para el «N° asiento» del título).
--    Esperado: una fila; ASNTNMRO con valor si la nómina ya se contabilizó.
-- -------------------------------------------------------------------------------------
SELECT p.PRDNCDGO, p.PRDNANOO, p.PRDNMSEE, p.PRDNASNT, a.ASNTNMRO, a.ASNTESTD
FROM   RHH.PRDN p
       LEFT JOIN CNT.ASNT a ON a.ASNTCDGO = p.PRDNASNT
WHERE  p.PRDNCDGO = (SELECT PRDNCDGO FROM (SELECT PRDNCDGO FROM RHH.PRDN
                     ORDER BY PRDNANOO DESC, PRDNMSEE DESC, PRDNCDGO DESC)
                     WHERE ROWNUM = 1);                                -- >>> PERIODO
