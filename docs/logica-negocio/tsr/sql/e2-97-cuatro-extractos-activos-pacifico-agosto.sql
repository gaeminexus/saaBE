-- =====================================================================================
-- e2-97 — Pacífico 7713878 (cuenta 419): CUATRO extractos activos en agosto y NINGUNO en septiembre.
-- SOLO LECTURA. Equipo omen-saa-2 · 2026-10-12. Sigue al e2-95.
--
-- Lo que dio el e2-95 (corrido por el usuario el 2026-10-12):
--   - G1 vacío: NO existe extracto de la cuenta 419 en el período 4519 (septiembre). «Recargar» exige uno
--     y por eso falla; desde f3473cab dice «Use la carga normal (confirmar), no la recarga».
--   - G4/G5: septiembre no está cerrado (CTEBCRRE nulo) ni la conciliación 105 verificada (CNCTESTR 1).
--   - G6: la cuenta tiene 4 extractos, TODOS en el período 4518 (agosto) y TODOS activos: 42, 56, 57, 59.
--     Los tres últimos con el mismo nombre de archivo y hash distinto.
--
-- Defecto confirmado en el código (ImportacionExtractoBancarioServiceImpl.ejecutarImportacion, 2026-10-12):
-- la carga normal SOLO controla el hash del archivo, no que la cuenta/período ya tenga un extracto activo.
-- Un archivo re-exportado del banco (otro hash) entra como un extracto más del mismo mes. Y recargar() solo
-- reemplaza el de código mayor (selectByCuentaYPeriodo, order by codigo desc).
--
-- Este script mide qué hay en cada uno para saber si son el mismo agosto repetido, si alguno es en realidad
-- septiembre cargado en el período equivocado, y cuál está conciliado. NO corrige nada.
-- Columnas copiadas de ExtractoBancario (EXBC), DetalleExtractoBancario (DEXB), GrupoConciliacionExtracto
-- (GCEX), GrupoConciliacionContable (GRCC), DetalleTransito (DTCN) y ConciliacionContable (CNCT).
-- =====================================================================================

-- BLOQUE 1 — Cabecera de cada extracto. ESPERADO: decide el caso.
--   · Si 56/57/59 tienen las mismas fechas (01-08 a 31-08) y los mismos saldos que 42 → agosto repetido.
--   · Si alguno va del 01-09 al 30-09 → es septiembre, cargado en el período de agosto.
SELECT 'BLOQUE 1 - cabeceras' AS bloque, e.EXBCCDGO, e.PRDOCDGO, e.EXBCARCH, e.EXBCFDSD, e.EXBCFHST,
       e.EXBCSLIN, e.EXBCSLFN, e.EXBCESTP, e.EXBCESTD, e.EXBCFCRG, e.EXBCUSAR,
       (SELECT COUNT(*) FROM TSR.DEXB d WHERE d.EXBCCDGO = e.EXBCCDGO) AS filas,
       (SELECT MIN(d.DEXBFTRN) FROM TSR.DEXB d WHERE d.EXBCCDGO = e.EXBCCDGO) AS primera_trx,
       (SELECT MAX(d.DEXBFTRN) FROM TSR.DEXB d WHERE d.EXBCCDGO = e.EXBCCDGO) AS ultima_trx,
       (SELECT SUM(d.DEXBDBTO) FROM TSR.DEXB d WHERE d.EXBCCDGO = e.EXBCCDGO) AS total_debito,
       (SELECT SUM(d.DEXBCRDT) FROM TSR.DEXB d WHERE d.EXBCCDGO = e.EXBCCDGO) AS total_credito,
       e.EXBCOBSR
  FROM TSR.EXBC e
 WHERE e.CNBCCDGO = 419
 ORDER BY e.EXBCCDGO;

-- BLOQUE 2 — Filas de cada extracto por mes de la transacción. ESPERADO: todo en 2026-08. Filas de 2026-09
-- confirman que septiembre se cargó en el período de agosto.
SELECT 'BLOQUE 2 - filas por mes' AS bloque, d.EXBCCDGO, TO_CHAR(d.DEXBFTRN, 'YYYY-MM') AS mes, COUNT(*) AS filas
  FROM TSR.DEXB d
  JOIN TSR.EXBC e ON e.EXBCCDGO = d.EXBCCDGO
 WHERE e.CNBCCDGO = 419
 GROUP BY d.EXBCCDGO, TO_CHAR(d.DEXBFTRN, 'YYYY-MM')
 ORDER BY d.EXBCCDGO, mes;

-- BLOQUE 3 — Qué extracto está conciliado. ESPERADO: uno solo con grupos activos. Los extractos sin ningún
-- grupo activo ni partida en tránsito son los candidatos a inactivarse; los conciliados NO se tocan.
SELECT 'BLOQUE 3 - conciliados' AS bloque, d.EXBCCDGO, r.GRCCESTD, COUNT(DISTINCT g.DEXBCDGO) AS filas_enlazadas,
       COUNT(DISTINCT r.GRCCCDGO) AS grupos
  FROM TSR.GCEX g
  JOIN TSR.GRCC r ON r.GRCCCDGO = g.GRCCCDGO
  JOIN TSR.DEXB d ON d.DEXBCDGO = g.DEXBCDGO
  JOIN TSR.EXBC e ON e.EXBCCDGO = d.EXBCCDGO
 WHERE e.CNBCCDGO = 419
 GROUP BY d.EXBCCDGO, r.GRCCESTD
 ORDER BY d.EXBCCDGO, r.GRCCESTD;

SELECT 'BLOQUE 3b - partidas en transito' AS bloque, d.EXBCCDGO, dt.DTCNESTD, COUNT(*) AS filas
  FROM TSR.DTCN dt
  JOIN TSR.DEXB d ON d.DEXBCDGO = dt.DTCNIDEX
  JOIN TSR.EXBC e ON e.EXBCCDGO = d.EXBCCDGO
 WHERE e.CNBCCDGO = 419
 GROUP BY d.EXBCCDGO, dt.DTCNESTD
 ORDER BY d.EXBCCDGO;

-- BLOQUE 4 — Estado de la conciliación contable de agosto. ESPERADO: si está VERIFICADA (CNCTESTR = 2),
-- agosto se dio por cerrado con estos extractos y cualquier limpieza pasa primero por el usuario.
SELECT 'BLOQUE 4 - conciliacion agosto' AS bloque, n.CNCTCDGO, n.PRDOCDGO, n.CNCTESTR
  FROM TSR.CNCT n WHERE n.CNBCCDGO = 419 AND n.PRDOCDGO IN (4518, 4519);

-- BLOQUE 5 — ¿Otras cuentas con el mismo problema? ESPERADO: ninguna fila. Cada fila es una cuenta/período
-- con más de un extracto activo.
SELECT 'BLOQUE 5 - otras cuentas duplicadas' AS bloque, e.CNBCCDGO, e.PRDOCDGO, COUNT(*) AS extractos_activos,
       LISTAGG(e.EXBCCDGO, ',') WITHIN GROUP (ORDER BY e.EXBCCDGO) AS codigos
  FROM TSR.EXBC e
 WHERE e.EXBCESTD = 1
 GROUP BY e.CNBCCDGO, e.PRDOCDGO
HAVING COUNT(*) > 1
 ORDER BY e.CNBCCDGO, e.PRDOCDGO;
