-- =====================================================================================
-- e3-16 — Garantes registrados como TERCERO (sin partícipe) — SOLO LECTURA
-- Equipo omen-saa-3 · 2026-11-11 · Diseño: crd/DISENO-PAGO-CON-APORTES-DEL-GARANTE.md (G1)
-- Desde ahora el garante es SIEMPRE partícipe. Esto lista los garantes activos que quedaron sin
-- ENTDCDGO y si su cédula coincide con un partícipe (para corregirlos con un UPDATE autorizado).
-- Esperado: ninguna fila, o pocas. Mandar el resultado al árbitro.
-- =====================================================================================

SELECT g.GRPRCDGO, g.PRSTCDGO, p.PRSTIDST AS ESTADO_PRESTAMO,
       g.GRPRNMID AS CEDULA_GARANTE, g.GRPRNMBR AS NOMBRE_GARANTE,
       (SELECT COUNT(*) FROM CRD.ENTD e WHERE TRIM(e.ENTDNMID) = TRIM(g.GRPRNMID)) AS PARTICIPES_CON_ESA_CEDULA,
       (SELECT MIN(e.ENTDCDGO) FROM CRD.ENTD e WHERE TRIM(e.ENTDNMID) = TRIM(g.GRPRNMID)) AS ENTDCDGO_SUGERIDO
  FROM CRD.GRPR g
  JOIN CRD.PRST p ON p.PRSTCDGO = g.PRSTCDGO
 WHERE g.GRPRESTD = 1
   AND g.ENTDCDGO IS NULL
 ORDER BY g.GRPRCDGO;

-- Resumen: cuántos garantes activos hay y cuántos son partícipes.
SELECT COUNT(*) AS GARANTES_ACTIVOS,
       SUM(CASE WHEN ENTDCDGO IS NOT NULL THEN 1 ELSE 0 END) AS CON_PARTICIPE,
       SUM(CASE WHEN ENTDCDGO IS NULL THEN 1 ELSE 0 END) AS TERCEROS
  FROM CRD.GRPR
 WHERE GRPRESTD = 1;
