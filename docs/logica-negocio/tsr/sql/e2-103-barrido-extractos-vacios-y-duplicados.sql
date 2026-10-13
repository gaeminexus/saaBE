-- =====================================================================================
-- e2-103 — Barrido de extractos bancarios problemáticos en TODAS las cuentas.
-- Equipo omen-saa-2 · 2026-10-13. BLOQUES 1-4 SOLO LECTURA; BLOQUE 5 CORRIGE (comentado, se activa después).
--
-- Origen: e2-102 (Pacífico 419). El extracto 42 entró VACÍO (0 filas, saldos distintos), en el período de
-- agosto pero con fechas 01-09 a 01-09, y por eso la pantalla «veía» un extracto en septiembre y solo ofrecía
-- «Recargar», que el backend rechazaba. Las guardas de código (saaBE d830c054, a6c8b518; saaFE 8a7c519)
-- evitan casos NUEVOS, pero no limpian los que ya están en la base. Este script los encuentra.
-- Columnas copiadas de ExtractoBancario (TSR.EXBC), DetalleExtractoBancario (TSR.DEXB),
-- GrupoConciliacionExtracto (TSR.GCEX), CuentaBancaria (TSR.CNBC) y Periodo (CNT.PRDO: PRDOINCO/PRDOFNN).
-- EXBCESTD: 1 = activo, 0 = inactivo.
-- =====================================================================================

-- BLOQUE 1 — Extractos ACTIVOS sin ninguna fila. ESPERADO: idealmente 0 filas.
--   tipo = 'SALDOS DISTINTOS' → el banco sí tuvo movimientos y el parser no los leyó: extracto malo.
--   tipo = 'SALDOS IGUALES'   → un mes sin movimientos; es legítimo SI es el único del período.
--   hay_otro_con_filas = 'SI' → existe otro extracto activo con filas en la misma cuenta y período: éste sobra.
SELECT 'BLOQUE 1 - activos vacios' AS bloque, e.CNBCCDGO, c.CNBCNMRO AS cuenta, e.EXBCCDGO, e.PRDOCDGO,
       p.PRDONMBR AS periodo, e.EXBCARCH, e.EXBCFDSD, e.EXBCFHST, e.EXBCSLIN, e.EXBCSLFN, e.EXBCFCRG,
       CASE WHEN ABS(NVL(e.EXBCSLIN, 0) - NVL(e.EXBCSLFN, 0)) > 0.005 THEN 'SALDOS DISTINTOS'
            ELSE 'SALDOS IGUALES' END AS tipo,
       CASE WHEN EXISTS (SELECT 1 FROM TSR.EXBC o
                          WHERE o.CNBCCDGO = e.CNBCCDGO AND o.PRDOCDGO = e.PRDOCDGO
                            AND o.EXBCESTD = 1 AND o.EXBCCDGO <> e.EXBCCDGO
                            AND EXISTS (SELECT 1 FROM TSR.DEXB d WHERE d.EXBCCDGO = o.EXBCCDGO))
            THEN 'SI' ELSE 'NO' END AS hay_otro_con_filas
  FROM TSR.EXBC e
  JOIN TSR.CNBC c ON c.CNBCCDGO = e.CNBCCDGO
  JOIN CNT.PRDO p ON p.PRDOCDGO = e.PRDOCDGO
 WHERE e.EXBCESTD = 1
   AND NOT EXISTS (SELECT 1 FROM TSR.DEXB d WHERE d.EXBCCDGO = e.EXBCCDGO)
 ORDER BY e.CNBCCDGO, e.PRDOCDGO, e.EXBCCDGO;

-- BLOQUE 2 — Cuentas con MÁS DE UN extracto activo en el mismo período. ESPERADO: 0 filas (desde d830c054
-- la carga normal ya no lo permite). Esperado hoy: al menos la 418 (extractos 1 y 53).
SELECT 'BLOQUE 2 - duplicados' AS bloque, e.CNBCCDGO, c.CNBCNMRO AS cuenta, e.PRDOCDGO, p.PRDONMBR AS periodo,
       e.EXBCCDGO, e.EXBCARCH, e.EXBCFDSD, e.EXBCFHST, e.EXBCSLIN, e.EXBCSLFN, e.EXBCFCRG,
       (SELECT COUNT(*) FROM TSR.DEXB d WHERE d.EXBCCDGO = e.EXBCCDGO) AS filas,
       (SELECT COUNT(*) FROM TSR.GCEX g JOIN TSR.DEXB d ON d.DEXBCDGO = g.DEXBCDGO
         WHERE d.EXBCCDGO = e.EXBCCDGO) AS enlaces_conciliacion
  FROM TSR.EXBC e
  JOIN TSR.CNBC c ON c.CNBCCDGO = e.CNBCCDGO
  JOIN CNT.PRDO p ON p.PRDOCDGO = e.PRDOCDGO
 WHERE e.EXBCESTD = 1
   AND (e.CNBCCDGO, e.PRDOCDGO) IN (SELECT x.CNBCCDGO, x.PRDOCDGO FROM TSR.EXBC x
                                     WHERE x.EXBCESTD = 1
                                     GROUP BY x.CNBCCDGO, x.PRDOCDGO HAVING COUNT(*) > 1)
 ORDER BY e.CNBCCDGO, e.PRDOCDGO, e.EXBCCDGO;

-- BLOQUE 3 — Extractos activos cuyas FECHAS se salen de su período. Son los que hacen que la pantalla vea
-- «ya hay extracto» en otro mes (el caso del 42). ESPERADO: 0 filas.
SELECT 'BLOQUE 3 - fechas fuera del periodo' AS bloque, e.CNBCCDGO, c.CNBCNMRO AS cuenta, e.EXBCCDGO,
       e.PRDOCDGO, p.PRDONMBR AS periodo, p.PRDOINCO, p.PRDOFNN, e.EXBCFDSD, e.EXBCFHST,
       (SELECT COUNT(*) FROM TSR.DEXB d WHERE d.EXBCCDGO = e.EXBCCDGO) AS filas
  FROM TSR.EXBC e
  JOIN TSR.CNBC c ON c.CNBCCDGO = e.CNBCCDGO
  JOIN CNT.PRDO p ON p.PRDOCDGO = e.PRDOCDGO
 WHERE e.EXBCESTD = 1
   AND (TRUNC(e.EXBCFDSD) < p.PRDOINCO OR TRUNC(e.EXBCFHST) > p.PRDOFNN)
 ORDER BY e.CNBCCDGO, e.PRDOCDGO, e.EXBCCDGO;

-- BLOQUE 4 — Resumen. ESPERADO: todo en 0, salvo lo que ya se vio arriba.
SELECT 'BLOQUE 4 - resumen' AS bloque,
       (SELECT COUNT(*) FROM TSR.EXBC e WHERE e.EXBCESTD = 1
           AND NOT EXISTS (SELECT 1 FROM TSR.DEXB d WHERE d.EXBCCDGO = e.EXBCCDGO)) AS activos_vacios,
       (SELECT COUNT(*) FROM (SELECT 1 FROM TSR.EXBC x WHERE x.EXBCESTD = 1
           GROUP BY x.CNBCCDGO, x.PRDOCDGO HAVING COUNT(*) > 1)) AS cuenta_periodo_duplicados,
       (SELECT COUNT(*) FROM TSR.EXBC e JOIN CNT.PRDO p ON p.PRDOCDGO = e.PRDOCDGO
         WHERE e.EXBCESTD = 1
           AND (TRUNC(e.EXBCFDSD) < p.PRDOINCO OR TRUNC(e.EXBCFHST) > p.PRDOFNN)) AS fechas_fuera
  FROM DUAL;

-- =====================================================================================
-- BLOQUE 5 — CORRECCIÓN. **NO correr todavía.** Pásame primero los bloques 1 a 4.
-- Inactiva SOLO extractos activos, VACÍOS (0 filas, así que no hay conciliación ni tránsito que perder) y que
-- además cumplan una de dos:
--   a) saldos distintos (el parser no leyó los movimientos: el extracto no sirve), o
--   b) hay otro extracto activo CON filas en la misma cuenta y período (éste sobra).
-- Un extracto vacío con saldos iguales que es el único de su período NO se toca (mes sin movimientos).
-- Los duplicados CON filas (p. ej. 418: 1 y 53) NO los toca este bloque: se deciden uno por uno.
-- =====================================================================================

-- Control ANTES: cuántos y cuáles tocaría. Debe coincidir con lo que acordemos del BLOQUE 1.
-- SELECT 'ANTES' AS bloque, e.EXBCCDGO, e.CNBCCDGO, e.PRDOCDGO, e.EXBCSLIN, e.EXBCSLFN
--   FROM TSR.EXBC e
--  WHERE e.EXBCESTD = 1
--    AND NOT EXISTS (SELECT 1 FROM TSR.DEXB d WHERE d.EXBCCDGO = e.EXBCCDGO)
--    AND (   ABS(NVL(e.EXBCSLIN, 0) - NVL(e.EXBCSLFN, 0)) > 0.005
--         OR EXISTS (SELECT 1 FROM TSR.EXBC o
--                     WHERE o.CNBCCDGO = e.CNBCCDGO AND o.PRDOCDGO = e.PRDOCDGO
--                       AND o.EXBCESTD = 1 AND o.EXBCCDGO <> e.EXBCCDGO
--                       AND EXISTS (SELECT 1 FROM TSR.DEXB d WHERE d.EXBCCDGO = o.EXBCCDGO)))
--  ORDER BY e.EXBCCDGO;

-- UPDATE TSR.EXBC e
--    SET e.EXBCESTD = 0,
--        e.EXBCOBSR = SUBSTR(NVL(e.EXBCOBSR, '') || ' | e2-103 2026-10-13: inactivado, extracto vacio', 1, 1000)
--  WHERE e.EXBCESTD = 1
--    AND NOT EXISTS (SELECT 1 FROM TSR.DEXB d WHERE d.EXBCCDGO = e.EXBCCDGO)
--    AND (   ABS(NVL(e.EXBCSLIN, 0) - NVL(e.EXBCSLFN, 0)) > 0.005
--         OR EXISTS (SELECT 1 FROM TSR.EXBC o
--                     WHERE o.CNBCCDGO = e.CNBCCDGO AND o.PRDOCDGO = e.PRDOCDGO
--                       AND o.EXBCESTD = 1 AND o.EXBCCDGO <> e.EXBCCDGO
--                       AND EXISTS (SELECT 1 FROM TSR.DEXB d WHERE d.EXBCCDGO = o.EXBCCDGO)));
-- ESPERADO: tantas filas actualizadas como dio el control ANTES.

-- Control DESPUÉS: volver a correr el BLOQUE 4. ESPERADO: activos_vacios solo con meses legítimos sin
-- movimientos; fechas_fuera debería bajar si los que se salían eran vacíos.
-- Si todo cuadra: COMMIT;   Si no: ROLLBACK;

-- REVERSO (comentado): reactiva solo lo que marcó este script.
-- UPDATE TSR.EXBC SET EXBCESTD = 1 WHERE EXBCOBSR LIKE '%e2-103 2026-10-13: inactivado%';
-- COMMIT;
