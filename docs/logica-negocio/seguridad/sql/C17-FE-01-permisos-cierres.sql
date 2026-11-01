--------------------------------------------------------------------------------
-- C17-FE-01 — Permisos de las pantallas de cierres (saaFE rama cuadre/c17-pantallas-cierres)
--
-- Agrega 4 nodos al arbol de permisos de SAA (SCP.PJRQ, PGSPCDGO = 11), a continuacion
-- de lap1-15 (ids 1253..1557):
--
--   1558 CIERRE DE PERIODOS   nivel 4, padre 1277 SAA > CONTABILIDAD > PROCESOS (pantalla)
--   1559 CERRAR PERIODO       nivel 5, padre 1558 (accion)
--   1560 REABRIR PERIODO      nivel 5, padre 1558 (accion)
--   1561 REABRIR CARTERA      nivel 5, padre 1412 SAA > CREDITOS > PARAMETRIZACION > CIERRE DE CARTERA (accion)
--
-- En el frontend: Permisos.CNT_CIERRE_DE_PERIODOS, CNT_CERRAR_PERIODO, CNT_REABRIR_PERIODO y
-- CRD_REABRIR_CARTERA (shared/model/permisos.ts).
--
-- Con el interruptor de permisos en 0 (rubro 7 / detalle 1) estos codigos no se validan. Con el
-- interruptor en 1 y estos nodos ausentes, el SP niega la accion: correr ESTE script antes de subir
-- el frontend si el interruptor esta en 1.
--
-- SCP.PJRQ es TAMBIEN la tabla de empresas y usuarios: el rango tiene que estar libre en TODA la tabla.
--------------------------------------------------------------------------------

-- CONTROL 1 (antes) — el rango 1558..1561 libre en toda la tabla. Esperado: 0 filas.
SELECT PJRQCDGO, PGSPCDGO, PJRQNMBR
  FROM SCP.PJRQ
 WHERE PJRQCDGO BETWEEN 1558 AND 1561;

-- CONTROL 2 (antes) — los padres existen en el arbol 11. Esperado: 2 filas (1277 PROCESOS, 1412 CIERRE DE CARTERA).
SELECT PJRQCDGO, PGSPCDGO, PJRQNMBR, PJRQNVLL, PJRQCDPD
  FROM SCP.PJRQ
 WHERE PJRQCDGO IN (1277, 1412)
   AND PGSPCDGO = 11;

-- INSERTS (solo si CONTROL 1 = 0 filas y CONTROL 2 = 2 filas)
INSERT INTO SCP.PJRQ (PJRQCDGO, PGSPCDGO, PJRQNMBR, PJRQNVLL, PJRQCDPD, PJRQINGR) VALUES (1558, 11, 'CIERRE DE PERIODOS', 4, 1277, 1);
INSERT INTO SCP.PJRQ (PJRQCDGO, PGSPCDGO, PJRQNMBR, PJRQNVLL, PJRQCDPD, PJRQINGR) VALUES (1559, 11, 'CERRAR PERIODO', 5, 1558, 1);
INSERT INTO SCP.PJRQ (PJRQCDGO, PGSPCDGO, PJRQNMBR, PJRQNVLL, PJRQCDPD, PJRQINGR) VALUES (1560, 11, 'REABRIR PERIODO', 5, 1558, 1);
INSERT INTO SCP.PJRQ (PJRQCDGO, PGSPCDGO, PJRQNMBR, PJRQNVLL, PJRQCDPD, PJRQINGR) VALUES (1561, 11, 'REABRIR CARTERA', 5, 1412, 1);

-- CONTROL 3 (despues) — Esperado: 4 filas con los padres de arriba.
SELECT PJRQCDGO, PGSPCDGO, PJRQNMBR, PJRQNVLL, PJRQCDPD
  FROM SCP.PJRQ
 WHERE PJRQCDGO BETWEEN 1558 AND 1561
 ORDER BY PJRQCDGO;

-- COMMIT;

-- REVERSO (comentado):
-- DELETE FROM SCP.PJRQ WHERE PGSPCDGO = 11 AND PJRQCDGO BETWEEN 1558 AND 1561;
