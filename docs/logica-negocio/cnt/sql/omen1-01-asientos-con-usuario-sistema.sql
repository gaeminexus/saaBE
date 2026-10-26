-- =====================================================================================
-- omen1-01 — Asientos grabados con el usuario SISTEMA / SYSTEM (solo consulta)
-- FECHA: 2026-10-26 · EQUIPO: omen-saa-1 (cnt)
--
-- Por que: cuando el frontend pierde el usuario de la sesion (F5, incognito), varias
-- pantallas mandan 'sistema' o 'SYSTEM' como usuario, y el backend graba "SISTEMA" si el
-- usuario llega nulo. Esta consulta mide cuantos asientos quedaron asi y de que tipo, para
-- separar los que si son de procesos automaticos (carga Petro, cobros) de los que hizo una
-- persona con la sesion perdida.
--
-- SOLO LECTURA. No modifica nada.
-- =====================================================================================

-- 1. Asientos por usuario "de sistema", tipo de asiento y mes
SELECT UPPER(TRIM(a.ASNTUSRO))           AS usuario,
       t.PLNTNMBR                        AS tipo_asiento,
       TO_CHAR(a.ASNTFCHA, 'YYYY-MM')    AS mes,
       COUNT(*)                          AS asientos,
       MIN(a.ASNTFCHA)                   AS primero,
       MAX(a.ASNTFCHA)                   AS ultimo
  FROM CNT.ASNT a
  LEFT JOIN CNT.PLNT t ON t.PLNTCDGO = a.PLNTCDGO
 WHERE UPPER(TRIM(a.ASNTUSRO)) IN ('SISTEMA', 'SYSTEM')
    OR a.ASNTUSRO IS NULL
 GROUP BY UPPER(TRIM(a.ASNTUSRO)), t.PLNTNMBR, TO_CHAR(a.ASNTFCHA, 'YYYY-MM')
 ORDER BY mes DESC, asientos DESC;

-- 2. Anulaciones registradas con usuario "de sistema"
SELECT UPPER(TRIM(a.ASNTUSAN))           AS usuario_anulacion,
       t.PLNTNMBR                        AS tipo_asiento,
       TO_CHAR(a.ASNTFCAN, 'YYYY-MM')    AS mes,
       COUNT(*)                          AS asientos
  FROM CNT.ASNT a
  LEFT JOIN CNT.PLNT t ON t.PLNTCDGO = a.PLNTCDGO
 WHERE a.ASNTFCAN IS NOT NULL
   AND (UPPER(TRIM(a.ASNTUSAN)) IN ('SISTEMA', 'SYSTEM') OR a.ASNTUSAN IS NULL)
 GROUP BY UPPER(TRIM(a.ASNTUSAN)), t.PLNTNMBR, TO_CHAR(a.ASNTFCAN, 'YYYY-MM')
 ORDER BY mes DESC, asientos DESC;

-- 3. Detalle de los ultimos 200 (para reconocer cuales hizo una persona)
SELECT a.ASNTCDGO, a.ASNTNMRO, a.ASNTFCHA, t.PLNTNMBR AS tipo_asiento,
       a.ASNTUSRO, SUBSTR(a.ASNTOBSR, 1, 120) AS observacion
  FROM CNT.ASNT a
  LEFT JOIN CNT.PLNT t ON t.PLNTCDGO = a.PLNTCDGO
 WHERE UPPER(TRIM(a.ASNTUSRO)) IN ('SISTEMA', 'SYSTEM')
    OR a.ASNTUSRO IS NULL
 ORDER BY a.ASNTFCHA DESC, a.ASNTCDGO DESC
 FETCH FIRST 200 ROWS ONLY;
