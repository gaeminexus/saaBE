-- =====================================================================================
-- e2-95 — «Recargar Extracto» de Banco Pacífico 7713878, septiembre 2026, da «500 Internal Server Error».
-- SOLO LECTURA. Equipo omen-saa-2 · 2026-10-08.
--
-- POST /exbc/recargar/419/4519 (cuenta 419, período 4519). El REST devuelve 500 ante CUALQUIER error, aunque
-- sea una validación con mensaje claro, y la pantalla muestra solo «Http failure response … 500», sin el
-- mensaje. Este script reproduce, en orden, las guardas de ImportacionExtractoBancarioServiceImpl.recargar
-- para saber cuál frenó. La PRIMERA que dé distinto de lo esperado es la causa.
-- Columnas copiadas de ExtractoBancario (EXBC), DetalleExtractoBancario (DEXB), GrupoConciliacionExtracto
-- (GCEX), GrupoConciliacionContable (GRCC), DetalleTransito (DTCN), ConciliacionContable (CNCT),
-- ControlExtractoBancario (CTEB) y Periodo (PRDO).
-- =====================================================================================

-- G1 — Tiene que existir el extracto anterior. ESPERADO: 1 fila.
SELECT 'G1 - extracto actual' AS guarda, e.EXBCCDGO, e.EXBCARCH, e.EXBCESTD, e.EXBCHASH,
       (SELECT COUNT(*) FROM TSR.DEXB d WHERE d.EXBCCDGO = e.EXBCCDGO) AS filas
  FROM TSR.EXBC e WHERE e.CNBCCDGO = 419 AND e.PRDOCDGO = 4519;

-- G2 — Ninguna fila del extracto en un grupo de conciliación ACTIVO. ESPERADO: 0.
-- Si da > 0: hay que DESHACER esas conciliaciones en Conciliación Contable antes de recargar.
SELECT 'G2 - filas conciliadas' AS guarda, COUNT(*) AS filas
  FROM TSR.GCEX g
  JOIN TSR.GRCC r ON r.GRCCCDGO = g.GRCCCDGO
  JOIN TSR.DEXB d ON d.DEXBCDGO = g.DEXBCDGO
  JOIN TSR.EXBC e ON e.EXBCCDGO = d.EXBCCDGO
 WHERE e.CNBCCDGO = 419 AND e.PRDOCDGO = 4519 AND r.GRCCESTD = 1;

-- G3 + red de seguridad — Ninguna fila declarada como partida en tránsito (pendiente o saldada). ESPERADO: 0.
SELECT 'G3 - partidas en transito' AS guarda, dt.DTCNESTD, COUNT(*) AS filas
  FROM TSR.DTCN dt
  JOIN TSR.DEXB d ON d.DEXBCDGO = dt.DTCNIDEX
  JOIN TSR.EXBC e ON e.EXBCCDGO = d.EXBCCDGO
 WHERE e.CNBCCDGO = 419 AND e.PRDOCDGO = 4519
 GROUP BY dt.DTCNESTD;

-- G4 — El período no puede estar cerrado para conciliación bancaria. ESPERADO: CTEBCRRE distinto de 1.
SELECT 'G4 - periodo cerrado' AS guarda, c.CTEBCDGO, c.CTEBMSSS, c.CTEBANOO, c.CTEBCRRE, c.CTEBESTD
  FROM TSR.CTEB c
  JOIN CNT.PRDO p ON p.PJRQCDGO = c.PJRQCDGO AND p.PRDOMSSS = c.CTEBMSSS AND p.PRDOANNN = c.CTEBANOO
 WHERE p.PRDOCDGO = 4519;

-- G5 — La conciliación contable de la cuenta/período no puede estar VERIFICADA. ESPERADO: CNCTESTR distinto de 2.
SELECT 'G5 - conciliacion verificada' AS guarda, n.CNCTCDGO, n.CNCTESTR
  FROM TSR.CNCT n WHERE n.CNBCCDGO = 419 AND n.PRDOCDGO = 4519;

-- G6 — El MISMO archivo no puede estar cargado en otro extracto (control por hash del archivo). No se puede
-- calcular el hash del archivo nuevo desde SQL: esta lista muestra los extractos de la cuenta, por si el
-- archivo «Plantilla banco pacifico cte.xlsx» ya se subió en otro período.
SELECT 'G6 - extractos de la cuenta' AS guarda, e.EXBCCDGO, e.EXBCARCH, e.PRDOCDGO, e.EXBCESTD, e.EXBCHASH
  FROM TSR.EXBC e WHERE e.CNBCCDGO = 419 ORDER BY e.EXBCCDGO;
