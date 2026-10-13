-- =====================================================================================
-- e2-102 — Pacífico 7713878 (cuenta 419): la pantalla dice «ya hay extracto» y «Recargar» dice «no existe».
-- Equipo omen-saa-2 · 2026-10-13. BLOQUES 1-2 SOLO LECTURA; BLOQUE 3 CORRIGE (inactiva extractos vacíos).
--
-- Causa (verificada en el código el 2026-10-13):
--   · La pantalla decide «ya existe extracto» con /cnct/resumenPorPeriodo → extractoCargado, que mira
--     COBERTURA DE FECHAS (EXBCFDSD <= último día y EXBCFHST >= primer día), sin importar el período
--     (ExtractoBancarioDaoServiceImpl.selectCuentasConCobertura). Con eso oculta «Confirmar» y deja solo
--     «Recargar».
--   · «Recargar» busca el extracto por PERÍODO (PRDOCDGO = septiembre, selectByCuentaYPeriodo). No hay
--     ninguno → 400 «No existe un extracto cargado…».
--   → Algún extracto ACTIVO de la cuenta 419, registrado en OTRO período, tiene fechas que llegan a
--     septiembre. Este script dice cuál.
-- El arreglo de código (los dos lados usan el criterio por período) está en GUARDA-CARGA-EXTRACTO.md §Recarga.
-- Columnas copiadas de ExtractoBancario (EXBC), DetalleExtractoBancario (DEXB), GrupoConciliacionExtracto (GCEX).
-- =====================================================================================

-- BLOQUE 1 — Extractos activos de la 419 cuyas fechas tocan septiembre. ESPERADO: al menos una fila, con
-- PRDOCDGO distinto de 4519. Ésa es la que hace aparecer «Recargar». Anotar EXBCCDGO, PRDOCDGO y filas.
SELECT 'BLOQUE 1 - cubren septiembre' AS bloque, e.EXBCCDGO, e.PRDOCDGO, e.EXBCARCH, e.EXBCFDSD, e.EXBCFHST,
       e.EXBCSLIN, e.EXBCSLFN, e.EXBCESTP, e.EXBCESTD, e.EXBCFCRG,
       (SELECT COUNT(*) FROM TSR.DEXB d WHERE d.EXBCCDGO = e.EXBCCDGO) AS filas
  FROM TSR.EXBC e
 WHERE e.CNBCCDGO = 419
   AND e.EXBCESTD = 1
   AND e.EXBCFDSD <= DATE '2026-09-30'
   AND e.EXBCFHST >= DATE '2026-09-01'
 ORDER BY e.EXBCCDGO;

-- BLOQUE 2 — Control ANTES del bloque 3. ESPERADO: 42, 56 y 57 activos, período 4518, 0 filas, 0 enlaces de
-- conciliación. Si alguno tiene filas o enlaces, NO correr el bloque 3: pasarme el resultado.
SELECT 'BLOQUE 2 - antes' AS bloque, e.EXBCCDGO, e.PRDOCDGO, e.EXBCESTD,
       (SELECT COUNT(*) FROM TSR.DEXB d WHERE d.EXBCCDGO = e.EXBCCDGO) AS filas,
       (SELECT COUNT(*) FROM TSR.GCEX g JOIN TSR.DEXB d ON d.DEXBCDGO = g.DEXBCDGO
         WHERE d.EXBCCDGO = e.EXBCCDGO) AS enlaces_conciliacion
  FROM TSR.EXBC e
 WHERE e.EXBCCDGO IN (42, 56, 57);

-- BLOQUE 3 — CORRECCIÓN. Inactiva los tres extractos VACÍOS de agosto (42, 56, 57). Ya estaban en la lista
-- de limpieza (e2-97): no tienen filas, el extracto bueno de agosto es el 59 (conciliado) y no se toca.
-- Si el BLOQUE 1 mostró que el que tapa septiembre es uno de estos tres, después de esto la pantalla deja
-- de ofrecer «Recargar» y Ana puede usar la carga normal (Validar → Confirmar) sin esperar el despliegue.
-- Si el que tapa septiembre es OTRO (p. ej. el 59), NO lo inactives: pásame el BLOQUE 1.
-- ESPERADO: 3 filas actualizadas.
UPDATE TSR.EXBC
   SET EXBCESTD = 0,
       EXBCOBSR = SUBSTR(NVL(EXBCOBSR, '') || ' | e2-102 2026-10-13: inactivado, extracto vacio duplicado de agosto', 1, 1000)
 WHERE EXBCCDGO IN (42, 56, 57)
   AND CNBCCDGO = 419
   AND EXBCESTD = 1
   AND NOT EXISTS (SELECT 1 FROM TSR.DEXB d WHERE d.EXBCCDGO = TSR.EXBC.EXBCCDGO);

-- Control DESPUÉS. ESPERADO: 42, 56, 57 con EXBCESTD 0; el 59 sigue en 1.
SELECT 'DESPUES' AS bloque, e.EXBCCDGO, e.PRDOCDGO, e.EXBCESTD
  FROM TSR.EXBC e
 WHERE e.CNBCCDGO = 419
 ORDER BY e.EXBCCDGO;

-- Si todo cuadra: COMMIT;   Si no: ROLLBACK;

-- REVERSO (comentado):
-- UPDATE TSR.EXBC SET EXBCESTD = 1 WHERE EXBCCDGO IN (42, 56, 57) AND CNBCCDGO = 419;
-- COMMIT;
