-- =====================================================================================
-- REPARACION EXPLICITA de las 4 lineas de banda sin cuenta — reemplaza al bloque 2 del 209
-- FECHA: 2026-09-07   EQUIPO: omen-saa-1 (omen1)   SCRIPT: 210 (rango 200-249)
--
-- ESCRIBE. Dos UPDATE de dos filas cada uno. Controles antes y despues; COMMIT y reverso
-- comentados.
--
-- POR QUE ESTE SCRIPT EXISTE:
--   El bloque 2 del sql/209 fallo con ORA-30926 ("la operacion ha intentado actualizar la
--   misma fila dos veces"). Su UPDATE llevaba un JOIN dentro de la subconsulta correlacionada
--   (CNT.PLNN con CNT.ASNT), y Oracle no acepta esa forma. Es un defecto de MI SQL, no de los
--   datos: el bloque 1 del 209 ya habia verificado que las cuatro filas resuelven a UNA sola
--   cuenta de su propia empresa.
--
--   En vez de reescribir la version generica con subconsultas escalares anidadas —mas astucia
--   sobre un caso de CUATRO filas ya verificadas— se hace explicito. Menos elegante, imposible
--   de malinterpretar.
--
-- DE DONDE SALEN LOS NUMEROS — de la salida del bloque 1 del 209, corrida por el usuario:
--
--   LINEA   EMPRESA  CUENTA       COINCIDENCIAS  RESUELVE A  NOMBRE
--   18860   1236     1.3.12.20    1              10552       DE 361 A 720 DIAS
--   18861   1236     1.3.12.15    1              10551       DE 271 A 360 DIAS
--   21217   1236     1.3.12.20    1              10552       DE 361 A 720 DIAS
--   21218   1236     1.3.12.15    1              10551       DE 271 A 360 DIAS
--
--   Asientos: 8527 (CRE-2026-09-0003, acuerdo 1) y 9245 (CRE-2026-08-0459, acuerdo 7).
--   Total involucrado: $2.691,94. Ninguno de los dos periodos esta mayorizado.
--
-- ⚠️ SECUENCIAS: no se inserta nada. Solo se completa una FK. Nada que sincronizar.
-- =====================================================================================

SET PAGESIZE 200
SET LINESIZE 260
SET FEEDBACK ON


-- =====================================================================================
-- BLOQUE 0 — CONTROL ANTES
-- Esperado: 4 filas, las cuatro con FK_PLAN_CUENTA en NULL y su CUENTA_TEXTO con valor.
-- Si alguna ya tiene FK, es que se reparo antes: no pasa nada, los UPDATE la ignoran.
-- =====================================================================================

SELECT l.DTASCDGO AS LINEA, l.ASNTCDGO AS ASIENTO, l.DTASCNTA AS CUENTA_TEXTO,
       l.DTASNMCT AS NOMBRE_TEXTO, l.PLNNCDGO AS FK_PLAN_CUENTA, l.DTASHBRR AS HABER
  FROM CNT.DTAS l
 WHERE l.DTASCDGO IN (18860, 18861, 21217, 21218)
 ORDER BY l.DTASCDGO;

-- 0.2 Reconfirmar que las dos cuentas destino son las correctas, de la empresa 1236
SELECT pc.PLNNCDGO, pc.PLNNCNTA, pc.PLNNNMBR, pc.PJRQCDGO AS EMPRESA
  FROM CNT.PLNN pc
 WHERE pc.PLNNCDGO IN (10551, 10552)
 ORDER BY pc.PLNNCDGO;


-- =====================================================================================
-- BLOQUE 1 — LA REPARACION
-- El `AND PLNNCDGO IS NULL` lo hace idempotente: correrlo dos veces no rompe nada.
-- El `AND DTASCNTA = ...` es un cinturon extra: si la fila no es la que se cree, no se toca.
-- Esperado: 2 filas actualizadas en cada UPDATE.
-- =====================================================================================

UPDATE CNT.DTAS
   SET PLNNCDGO = 10552
 WHERE DTASCDGO IN (18860, 21217)
   AND PLNNCDGO IS NULL
   AND DTASCNTA = '1.3.12.20';

UPDATE CNT.DTAS
   SET PLNNCDGO = 10551
 WHERE DTASCDGO IN (18861, 21218)
   AND PLNNCDGO IS NULL
   AND DTASCNTA = '1.3.12.15';


-- =====================================================================================
-- BLOQUE 2 — CONTROL DESPUES (mirar ANTES de confirmar)
-- Esperado 2.1: las 4 filas con su FK puesta y CONTROL = 'OK' en todas.
-- Esperado 2.2: 0 filas.
-- Si no da eso: ROLLBACK y avisar.
-- =====================================================================================

-- 2.1 Como quedaron
SELECT l.DTASCDGO AS LINEA, a.ASNTNMAL AS ASIENTO,
       l.DTASCNTA AS CUENTA_TEXTO, l.PLNNCDGO AS FK_PUESTA,
       pc.PLNNCNTA AS CUENTA_FK, pc.PLNNNMBR AS NOMBRE_FK,
       CASE WHEN pc.PLNNCNTA = l.DTASCNTA THEN 'OK' ELSE '⛔ NO COINCIDE' END AS CONTROL
  FROM CNT.DTAS l
  JOIN CNT.ASNT a  ON a.ASNTCDGO = l.ASNTCDGO
  JOIN CNT.PLNN pc ON pc.PLNNCDGO = l.PLNNCDGO
 WHERE l.DTASCDGO IN (18860, 18861, 21217, 21218)
 ORDER BY l.DTASCDGO;

-- 2.2 No debe quedar NINGUNA linea de asiento sin cuenta en toda la base
SELECT l.DTASCDGO, l.ASNTCDGO, l.DTASCNTA, l.DTASDSCR
  FROM CNT.DTAS l
 WHERE l.PLNNCDGO IS NULL
 ORDER BY l.DTASCDGO;

-- 2.3 Y que los dos asientos sigan cuadrando (no se tocaron valores, pero se controla igual)
SELECT l.ASNTCDGO AS ASIENTO,
       ROUND(SUM(NVL(l.DTASDBEE,0)), 2) AS TOTAL_DEBE,
       ROUND(SUM(NVL(l.DTASHBRR,0)), 2) AS TOTAL_HABER
  FROM CNT.DTAS l
 WHERE l.ASNTCDGO IN (8527, 9245)
 GROUP BY l.ASNTCDGO
 ORDER BY l.ASNTCDGO;


-- =====================================================================================
-- BLOQUE 3 — CONFIRMAR
-- Descomentar recien despues de leer el BLOQUE 2.
-- =====================================================================================

-- COMMIT;


-- =====================================================================================
-- BLOQUE 4 — REVERSO (comentado)
-- Devuelve las cuatro filas a como estaban. Sirve ANTES del COMMIT (ahi alcanza ROLLBACK)
-- o para deshacerlo despues.
-- =====================================================================================

-- UPDATE CNT.DTAS SET PLNNCDGO = NULL WHERE DTASCDGO IN (18860, 18861, 21217, 21218);
-- COMMIT;


-- =====================================================================================
-- DESPUES DE CONFIRMAR
--   Recien ahi mayorizar agosto y septiembre. Con la FK puesta, los $2.691,94 entran al
--   mayor en la cuenta que corresponde desde la primera mayorizacion: no hay que remayorizar.
-- =====================================================================================
