-- =====================================================================
-- e2-45 — El Tipo de Persona (rubro 35) tiene el TEXTO 'null' en el valor
--          alfanumerico, y por eso el ATS no escribe <tipoCliente>
-- Modulo: sri (catalogo SCP compartido)  ·  Equipo: omen-saa-2  ·  2026-09-15
--
-- ⚠️ NO ES SOLO LECTURA: el BLOQUE 2 hace UPDATE sobre SCP.PDTR (2 filas).
--    Correr los bloques 0 y 1 primero y leerlos. Seguir solo si el
--    BLOQUE 0 da lo ESPERADO.
--
-- POR QUE EXISTE
--   Al generar el ATS de agosto la pantalla avisa:
--     "Titular 65 (AGHAYAR SEYIDOV): el catalogo del rubro 35 (Tipo de
--      Persona) devolvio 'null', que no es '01' ni '02'..."
--   Ese aviso SOLO sale si el valor NO es nulo en Java
--   (GeneradorAtsServiceImpl:914-926). O sea: la columna no esta vacia,
--   tiene escritas las cuatro letras  n-u-l-l. La consulta del 2026-09-11
--   lo leyo como NULL porque el cliente SQL muestra igual un nulo y el
--   texto 'null'. Con ese texto el codigo se niega a adivinar, no escribe
--   <tipoCliente>, y el SRI rechaza el pasaporte C05580508.
--
--   Salida: poner en el catalogo el codigo del SRI que corresponde a cada
--   detalle — Tabla 14 del ATS: 01 = persona natural, 02 = sociedad —
--   que es exactamente la correspondencia que el codigo ya usa como
--   respaldo por alterno (NATURAL=1 -> 01, JURIDICO=2 -> 02).
--
--   Unico consumidor en el backend de PDTRVLRV del rubro 35: el ATS
--   (grep de TIPO_PERSONA y de ByRubAltDetAlt, 2026-09-15).
--
-- Columnas copiadas de las entidades:
--   DetalleRubro (SCP.PDTR: PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN,
--                 PDTRVLRV, PDTRALTR, PDTRESTD)
--   Rubro (SCP.PRBR: PRBRCDGO, PRBRDSCR, PRBRALTR)
-- =====================================================================


-- ---------------------------------------------------------------------
-- BLOQUE 0 — Control ANTES: que hay de verdad en el rubro 35
-- ESPERADO: 2 filas, ALTERNO 1 NATURAL y 2 JURIDICO, con VALOR_TEXTO =
--           'null', LARGO = 4 y ES_NULO = 'NO'.
--   Si ES_NULO = 'SI' en las dos -> el problema es otro: NO correr el
--   bloque 2, avisar al arbitro.
--   Si hay mas de 2 filas o otros alternos -> NO correr el bloque 2.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 0 - rubro 35 antes' AS bloque,
       d.PDTRCDGO, d.PDTRALTR AS alterno, d.PDTRDSCR AS descripcion,
       '[' || d.PDTRVLRV || ']' AS valor_texto,
       LENGTH(d.PDTRVLRV)       AS largo,
       CASE WHEN d.PDTRVLRV IS NULL THEN 'SI' ELSE 'NO' END AS es_nulo,
       d.PDTRESTD
  FROM SCP.PDTR d
  JOIN SCP.PRBR r ON r.PRBRCDGO = d.PRBRCDGO
 WHERE r.PRBRALTR = 35
 ORDER BY d.PDTRALTR;


-- ---------------------------------------------------------------------
-- BLOQUE 1 — La familia: en que otros rubros hay el texto 'null' guardado
-- ESPERADO: idealmente solo el rubro 35. Cada fila de otro rubro es un
--           catalogo que devuelve 'null' a quien lo lea como texto.
--           Este bloque NO se corrige aqui: pegar la salida al arbitro.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 1 - texto null en PDTR' AS bloque,
       r.PRBRALTR AS rubro_alterno, r.PRBRDSCR AS rubro,
       COUNT(*)   AS detalles_con_texto_null
  FROM SCP.PDTR d
  JOIN SCP.PRBR r ON r.PRBRCDGO = d.PRBRCDGO
 WHERE LOWER(TRIM(d.PDTRVLRV)) = 'null'
 GROUP BY r.PRBRALTR, r.PRBRDSCR
 ORDER BY r.PRBRALTR;


-- ---------------------------------------------------------------------
-- BLOQUE 2 — ⚠️ UPDATE: el codigo SRI (Tabla 14) en los dos detalles
-- ESPERADO: "2 filas actualizadas". Si dice otro numero -> ROLLBACK;
--           y avisar al arbitro. Si dice 2 -> COMMIT.
-- ---------------------------------------------------------------------
UPDATE SCP.PDTR d
   SET d.PDTRVLRV = CASE d.PDTRALTR WHEN 1 THEN '01' WHEN 2 THEN '02' END
 WHERE d.PRBRCDGO IN (SELECT r.PRBRCDGO FROM SCP.PRBR r WHERE r.PRBRALTR = 35)
   AND d.PDTRALTR IN (1, 2)
   AND (d.PDTRVLRV IS NULL OR LOWER(TRIM(d.PDTRVLRV)) = 'null');

-- COMMIT;   <- quitar el comentario y ejecutar SOLO si el UPDATE dijo 2 filas


-- ---------------------------------------------------------------------
-- BLOQUE 3 — Control DESPUES
-- ESPERADO: alterno 1 -> [01], alterno 2 -> [02], LARGO = 2.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 3 - rubro 35 despues' AS bloque,
       d.PDTRCDGO, d.PDTRALTR AS alterno, d.PDTRDSCR AS descripcion,
       '[' || d.PDTRVLRV || ']' AS valor_texto,
       LENGTH(d.PDTRVLRV)       AS largo
  FROM SCP.PDTR d
  JOIN SCP.PRBR r ON r.PRBRCDGO = d.PRBRCDGO
 WHERE r.PRBRALTR = 35
 ORDER BY d.PDTRALTR;


-- ---------------------------------------------------------------------
-- REVERSO — comentado. Deja el valor como estaba ANTES (el texto 'null').
-- ---------------------------------------------------------------------
-- UPDATE SCP.PDTR d
--    SET d.PDTRVLRV = 'null'
--  WHERE d.PRBRCDGO IN (SELECT r.PRBRCDGO FROM SCP.PRBR r WHERE r.PRBRALTR = 35)
--    AND d.PDTRALTR IN (1, 2)
--    AND d.PDTRVLRV IN ('01', '02');
-- COMMIT;
