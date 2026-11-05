-- =====================================================================================================
-- 367 — CONTROL PREVIO (SOLO LECTURA) antes de ejecutar el cierre de cartera de octubre 2026
-- omen-saa-1-arb · 2026-11-05 · No modifica nada.
-- =====================================================================================================

-- ---------------------------------------------------------------------------------------------------
-- BLOQUE 1 — Asientos de la corrida fallida de octubre (numeración DI-A-2026-10-0036 a 0050).
-- Esperado: los del cierre (capital vencido, bandas, ajuste de bandas, provisión, devengo de mora) en
-- ASNTESTD = 2 (ANULADO). Si alguno del cierre sale en 1 (ACTIVO): NO ejecutar, avisar.
-- Los asientos que no son del cierre (otra glosa) pueden estar en 1: son movimientos normales.
-- ---------------------------------------------------------------------------------------------------
SELECT a.ASNTCDGO, a.ASNTNMAL, TRUNC(a.ASNTFCHA) AS FECHA, a.ASNTESTD,
       CASE a.ASNTESTD WHEN 1 THEN 'ACTIVO' WHEN 2 THEN 'ANULADO' WHEN 3 THEN 'REVERSADO'
                       WHEN 4 THEN 'INCOMPLETO' ELSE TO_CHAR(a.ASNTESTD) END AS ESTADO,
       a.ASNTUSRO AS USUARIO, SUBSTR(a.ASNTOBSR, 1, 100) AS GLOSA
FROM   CNT.ASNT a
WHERE  a.ASNTNMAL BETWEEN 'DI-A-2026-10-0036' AND 'DI-A-2026-10-0050'
ORDER BY a.ASNTNMAL;

-- ---------------------------------------------------------------------------------------------------
-- BLOQUE 2 — Cualquier asiento ACTIVO de cierre de cartera / provisión / devengo de octubre, con
-- cualquier numeración. Esperado: CERO filas.
-- ---------------------------------------------------------------------------------------------------
SELECT a.ASNTCDGO, a.ASNTNMAL, TRUNC(a.ASNTFCHA) AS FECHA, a.ASNTUSRO AS USUARIO,
       SUBSTR(a.ASNTOBSR, 1, 100) AS GLOSA
FROM   CNT.ASNT a
WHERE  a.ASNTESTD = 1
AND    a.ASNTFCHA >= DATE '2026-10-01'
AND    UPPER(a.ASNTOBSR) LIKE '%2026-10%'
AND   (UPPER(a.ASNTOBSR) LIKE '%CIERRE DE CARTERA%'
    OR UPPER(a.ASNTOBSR) LIKE '%AJUSTE DE BANDAS%'
    OR UPPER(a.ASNTOBSR) LIKE '%PROVISION DE CARTERA%'
    OR UPPER(a.ASNTOBSR) LIKE '%DEVENGO DE MORA%'
    OR UPPER(a.ASNTOBSR) LIKE '%NETEO%')
ORDER BY a.ASNTCDGO;

-- ---------------------------------------------------------------------------------------------------
-- BLOQUE 3 — Saldo de cartera en el mayor al 31-10 (1.3 sin la provisión 1.3.99), para el jefe de
-- cartera. Esperado: del orden de 16 millones (la previsualización da 15.923.286,37).
-- ---------------------------------------------------------------------------------------------------
SELECT ROUND(SUM(NVL(d.DTASDBEE,0) - NVL(d.DTASHBRR,0)), 2) AS SALDO_CARTERA_1_3
FROM   CNT.DTAS d
JOIN   CNT.ASNT a ON a.ASNTCDGO = d.ASNTCDGO
WHERE  a.ASNTESTD = 1
AND    a.ASNTFCHA < DATE '2026-11-01'
AND    d.DTASCNTA LIKE '1.3.%'
AND    d.DTASCNTA NOT LIKE '1.3.99%';
