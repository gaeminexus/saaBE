-- =====================================================================================
-- 396 — MEDICIÓN (SOLO LECTURA): PGPR vigentes con PRSTCDGO nulo o distinto del préstamo de su cuota
-- Equipo omen-saa-1 · 2026-11-25. No modifica nada. Se puede correr en producción.
--
-- Por qué importa: todo PGPR tiene una cuota (DTPRCDGO) y toda cuota tiene su préstamo, pero PGPR.PRSTCDGO puede venir nulo (el propio DAO lo
-- documenta: «esa FK directa en CRD.PGPR puede venir NULL en pagos viejos»). Lo que lee por PGPR.PRSTCDGO no ve esos pagos:
--   · PagoPrestamoDaoService.selectVigentesByPrestamo (p.prestamo.codigo) → PrelacionCuotasServiceImpl (C33), PrestamoServiceImpl:764 (revertir
--     el desembolso: «ya tiene pagos») y :1163 (anular un préstamo: «tiene N pago(s)»);
--   · SQL nativo con g.PRSTCDGO: CierreCarteraDaoServiceImpl (cuadre de cargas Petro, líneas ~512 y ~545) y PrestamoCanceladoDaoServiceImpl
--     (último pago, cantidad de pagos y total de un préstamo cancelado).
-- =====================================================================================

-- BLOQUE 1 — Totales (PGPR vigentes).
SELECT CASE WHEN g.PRSTCDGO IS NULL THEN 'PRSTCDGO NULO'
            WHEN g.PRSTCDGO <> d.PRSTCDGO THEN 'PRSTCDGO DE OTRO PRESTAMO'
            ELSE 'CORRECTO' END AS situacion,
       COUNT(*) AS pagos, COUNT(DISTINCT d.PRSTCDGO) AS prestamos,
       ROUND(SUM(NVL(g.PGPRVLRR, 0)), 2) AS valor, ROUND(SUM(NVL(g.PGPRCPPG, 0) + NVL(g.PGPRINPG, 0)), 2) AS capital_interes,
       MIN(g.PGPRFCHA) AS desde, MAX(g.PGPRFCHA) AS hasta
  FROM CRD.PGPR g JOIN CRD.DTPR d ON d.DTPRCDGO = g.DTPRCDGO
 WHERE NVL(g.PGPRANUL, 0) = 0
 GROUP BY CASE WHEN g.PRSTCDGO IS NULL THEN 'PRSTCDGO NULO' WHEN g.PRSTCDGO <> d.PRSTCDGO THEN 'PRSTCDGO DE OTRO PRESTAMO' ELSE 'CORRECTO' END
 ORDER BY 1;

-- BLOQUE 2 — ¿De dónde vienen? Por año-mes de registro, tipo y si traen carga Petro (CRARCDGO).
SELECT TO_CHAR(g.PGPRFCRG, 'YYYY-MM') AS registrado_en, NVL(g.PGPRTPOO, '(sin tipo)') AS tipo,
       CASE WHEN g.CRARCDGO IS NULL THEN 'sin carga' ELSE 'con carga' END AS carga, SUBSTR(g.PGPRUSRG, 1, 30) AS usuario,
       COUNT(*) AS pagos, COUNT(DISTINCT d.PRSTCDGO) AS prestamos
  FROM CRD.PGPR g JOIN CRD.DTPR d ON d.DTPRCDGO = g.DTPRCDGO
 WHERE NVL(g.PGPRANUL, 0) = 0 AND (g.PRSTCDGO IS NULL OR g.PRSTCDGO <> d.PRSTCDGO)
 GROUP BY TO_CHAR(g.PGPRFCRG, 'YYYY-MM'), NVL(g.PGPRTPOO, '(sin tipo)'), CASE WHEN g.CRARCDGO IS NULL THEN 'sin carga' ELSE 'con carga' END, SUBSTR(g.PGPRUSRG, 1, 30)
 ORDER BY 1, pagos DESC;

-- BLOQUE 3 — Los 50 préstamos con más capital + interés escondido, y el estado del préstamo.
SELECT * FROM (
    SELECT d.PRSTCDGO AS prestamo, MAX(p.PRSTIDAS) AS operacion, MAX(p.PRSTIDST) AS estado_prestamo, COUNT(*) AS pagos_invisibles,
           ROUND(SUM(NVL(g.PGPRCPPG, 0) + NVL(g.PGPRINPG, 0)), 2) AS capital_interes_invisible, MIN(g.PGPRFCHA) AS desde, MAX(g.PGPRFCHA) AS hasta,
           (SELECT COUNT(*) FROM CRD.PGPR h JOIN CRD.DTPR e ON e.DTPRCDGO = h.DTPRCDGO WHERE e.PRSTCDGO = d.PRSTCDGO AND NVL(h.PGPRANUL, 0) = 0
               AND h.PRSTCDGO = e.PRSTCDGO) AS pagos_visibles
      FROM CRD.PGPR g JOIN CRD.DTPR d ON d.DTPRCDGO = g.DTPRCDGO JOIN CRD.PRST p ON p.PRSTCDGO = d.PRSTCDGO
     WHERE NVL(g.PGPRANUL, 0) = 0 AND (g.PRSTCDGO IS NULL OR g.PRSTCDGO <> d.PRSTCDGO)
     GROUP BY d.PRSTCDGO
     ORDER BY SUM(NVL(g.PGPRCPPG, 0) + NVL(g.PGPRINPG, 0)) DESC
) WHERE ROWNUM <= 50;

-- BLOQUE 4 — Préstamos cuyos pagos vigentes son TODOS invisibles (selectVigentesByPrestamo los da por «sin pagos»: PrestamoServiceImpl dejaría
-- revertir el desembolso o anular el préstamo aunque tenga pagos).
SELECT COUNT(*) AS prestamos_con_pagos_pero_ninguno_visible
  FROM (SELECT d.PRSTCDGO FROM CRD.PGPR g JOIN CRD.DTPR d ON d.DTPRCDGO = g.DTPRCDGO WHERE NVL(g.PGPRANUL, 0) = 0
         GROUP BY d.PRSTCDGO HAVING SUM(CASE WHEN g.PRSTCDGO = d.PRSTCDGO THEN 1 ELSE 0 END) = 0);

-- BLOQUE 5 — Las cuotas con PGPR invisibles de los préstamos que el control C33 del cierre tiene hoy en la lista: ver el bloque 3 de sql/394
-- (columnas pagos_invisibles e invisible_cap_int).
