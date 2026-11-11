--------------------------------------------------------------------------------
-- C25-FE-01 — Permisos de la pantalla Seguimiento Novedades (saaFE rama cuadre/c25-seguimiento-novedades)
--
-- Agrega 9 nodos al arbol de permisos de SAA (SCP.PJRQ, PGSPCDGO = 11), despues de C17-FE-01 (1558..1561)
-- y del usuario REPROCESO (1562, PGSPCDGO 9: SCP.PJRQ guarda tambien empresas y usuarios):
--
--   1563 SEGUIMIENTO NOVEDADES    nivel 4, padre 1277 SAA > CONTABILIDAD > PROCESOS (pantalla)
--   1564 CARGAR NOVEDADES         nivel 5, padre 1563 (cargar lote y decisiones, nueva version)
--   1565 ENVIAR NOVEDADES         nivel 5, padre 1563
--   1566 RESPONDER REQUERIMIENTO  nivel 5, padre 1563 (respuesta, no existe, adjuntos)
--   1567 REVISAR RESPUESTA        nivel 5, padre 1563 (aceptar / observar)
--   1568 REGISTRAR DECISION       nivel 5, padre 1563 (Contabilidad de ASOPREP, D-056 (4))
--   1569 REGISTRAR AJUSTE         nivel 5, padre 1563 (asientos manuales de ajuste, vinculos, aplicar)
--   1570 CERRAR NOVEDAD           nivel 5, padre 1563 (cerrar, reabrir, anular, no aplica, informativas)
--   1571 VER TRAZABILIDAD         nivel 5, padre 1563 (archivos del expediente; copia al expediente; solo GAEMII)
--
-- En el frontend: Permisos.CNT_SEGUIMIENTO_NOVEDADES y CNT_SN_* (shared/model/permisos.ts).
-- D-056 (5): no hay relacion usuario -> area; el area es dato del requerimiento y se filtra en la bandeja.
--
-- Con el interruptor de permisos en 0 (rubro 7 / detalle 1) estos codigos no se validan. Con el
-- interruptor en 1 y estos nodos ausentes, el SP niega la accion: correr ESTE script antes de subir
-- el frontend si el interruptor esta en 1.
--
-- SCP.PJRQ es TAMBIEN la tabla de empresas y usuarios: el rango tiene que estar libre en TODA la tabla, y la
-- secuencia SCP.SQ_PJRQCDGO (si existe) no puede estar por debajo del rango (CONTROL 3).
-- SQL PURO (sin SET/DEFINE/WHENEVER). NO SE HA EJECUTADO en ninguna base.
--------------------------------------------------------------------------------

-- CONTROL 1 (antes) — el rango 1563..1571 libre en toda la tabla. Esperado: 0 filas.
SELECT PJRQCDGO, PGSPCDGO, PJRQNMBR
  FROM SCP.PJRQ
 WHERE PJRQCDGO BETWEEN 1563 AND 1571;

-- CONTROL 2 (antes) — el padre existe en el arbol 11. Esperado: 1 fila (1277 PROCESOS, numeracion de lap1-15).
-- OJO (verificado el 2026-11-10 en la copia de produccion del 9-nov): ahi lap1-15 NO esta aplicado (1253 es un
-- usuario, CONTABILIDAD > PROCESOS es el 620) y 1558..1561 de C17-FE-01 ya cuelgan de un 1277 inexistente. Si
-- este control da 0 filas, NO correr: el lider decide si se aplica lap1-15 antes o si se cuelga de otro padre.
SELECT PJRQCDGO, PGSPCDGO, PJRQNMBR, PJRQNVLL, PJRQCDPD
  FROM SCP.PJRQ
 WHERE PJRQCDGO = 1277
   AND PGSPCDGO = 11;

-- CONTROL 3 (antes) — la secuencia de PJRQ, si existe, no entrega numeros del rango. Esperado: 0 filas o
-- LAST_NUMBER > 1571 (si da un numero <= 1571, NO correr: avisar, hay que mover el rango o adelantar la secuencia).
SELECT SEQUENCE_OWNER, SEQUENCE_NAME, LAST_NUMBER
  FROM ALL_SEQUENCES
 WHERE SEQUENCE_OWNER = 'SCP' AND SEQUENCE_NAME = 'SQ_PJRQCDGO';

-- INSERTS (solo si CONTROL 1 = 0 filas, CONTROL 2 = 1 fila y CONTROL 3 sin conflicto)
INSERT INTO SCP.PJRQ (PJRQCDGO, PGSPCDGO, PJRQNMBR, PJRQNVLL, PJRQCDPD, PJRQINGR) VALUES (1563, 11, 'SEGUIMIENTO NOVEDADES', 4, 1277, 1);
INSERT INTO SCP.PJRQ (PJRQCDGO, PGSPCDGO, PJRQNMBR, PJRQNVLL, PJRQCDPD, PJRQINGR) VALUES (1564, 11, 'CARGAR NOVEDADES', 5, 1563, 1);
INSERT INTO SCP.PJRQ (PJRQCDGO, PGSPCDGO, PJRQNMBR, PJRQNVLL, PJRQCDPD, PJRQINGR) VALUES (1565, 11, 'ENVIAR NOVEDADES', 5, 1563, 1);
INSERT INTO SCP.PJRQ (PJRQCDGO, PGSPCDGO, PJRQNMBR, PJRQNVLL, PJRQCDPD, PJRQINGR) VALUES (1566, 11, 'RESPONDER REQUERIMIENTO', 5, 1563, 1);
INSERT INTO SCP.PJRQ (PJRQCDGO, PGSPCDGO, PJRQNMBR, PJRQNVLL, PJRQCDPD, PJRQINGR) VALUES (1567, 11, 'REVISAR RESPUESTA', 5, 1563, 1);
INSERT INTO SCP.PJRQ (PJRQCDGO, PGSPCDGO, PJRQNMBR, PJRQNVLL, PJRQCDPD, PJRQINGR) VALUES (1568, 11, 'REGISTRAR DECISION', 5, 1563, 1);
INSERT INTO SCP.PJRQ (PJRQCDGO, PGSPCDGO, PJRQNMBR, PJRQNVLL, PJRQCDPD, PJRQINGR) VALUES (1569, 11, 'REGISTRAR AJUSTE', 5, 1563, 1);
INSERT INTO SCP.PJRQ (PJRQCDGO, PGSPCDGO, PJRQNMBR, PJRQNVLL, PJRQCDPD, PJRQINGR) VALUES (1570, 11, 'CERRAR NOVEDAD', 5, 1563, 1);
INSERT INTO SCP.PJRQ (PJRQCDGO, PGSPCDGO, PJRQNMBR, PJRQNVLL, PJRQCDPD, PJRQINGR) VALUES (1571, 11, 'VER TRAZABILIDAD', 5, 1563, 1);

-- CONTROL 4 (despues, DECISIVO) — Esperado: 9 filas, 1563 con padre 1277 y las otras 8 con padre 1563.
SELECT PJRQCDGO, PGSPCDGO, PJRQNMBR, PJRQNVLL, PJRQCDPD
  FROM SCP.PJRQ
 WHERE PJRQCDGO BETWEEN 1563 AND 1571
 ORDER BY PJRQCDGO;

-- COMMIT;

-- REVERSO (comentado):
-- DELETE FROM SCP.PJRQ WHERE PGSPCDGO = 11 AND PJRQCDGO BETWEEN 1563 AND 1571;
