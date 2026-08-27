-- =====================================================================================
-- CORRECCION — ALTERNOS DE LOS RUBROS NUEVOS DE CRD (solo LOCAL)
-- =====================================================================================
-- FECHA: 2026-08-27
--
-- QUE PASO
--   La primera version de DDL-APORTES-DEVENGO-CONTRATOS.sql inserto los tres rubros
--   nuevos con PRBRCDGO 235/236/237 pero PRBRALTR 231/232/233. Al correr el control 7.3
--   aparecio el choque:
--
--     alterno 232 -> ya lo usa "TSR - Tipo de movimiento de caja chica (MVCH)"
--     alterno 233 -> ya lo usa "TSR - Estado del cierre de caja chica (CRCH)"
--
--   Es decir, DOS rubros distintos comparten el mismo codigo alterno. Como la aplicacion
--   resuelve los rubros POR ALTERNO (selectValorNumericoByRubAltDetAlt), ese estado es
--   peligroso: una lectura del rubro 233 puede devolver el detalle de caja chica de TSR
--   en vez del flag de contabilidad de CRD, sin ningun error visible.
--
--   Decision del usuario: PRBRCDGO y PRBRALTR coinciden. Los rubros de CRD pasan a
--   alterno 235, 236 y 237. El alterno 231 quedaba libre pero se mueve igual, para que
--   los tres sigan la misma regla.
--
-- ⚠ ESTE SCRIPT ES SOLO PARA LOCAL, donde ya se ejecuto la version equivocada.
--   EN PRODUCCION NO SE USA: alli se corre DDL-APORTES-DEVENGO-CONTRATOS.sql, que ya
--   trae los alternos correctos desde el inicio.
--
-- NO toca los detalles (PDTR): cuelgan de PRBRCDGO, que no cambia, y sus PDTRALTR
-- (1..6, 1..2, 1) son correctos.
-- =====================================================================================


-- =====================================================================================
-- 1. CONTROL PREVIO — la foto del problema
-- =====================================================================================

-- 1.1 Los rubros de CRD tal como quedaron. Esperado: 3 filas con alterno 231/232/233.
SELECT r.PRBRCDGO, r.PRBRALTR, r.PRBRDSCR
FROM   SCP.PRBR r
WHERE  r.PRBRCDGO IN (235, 236, 237)
ORDER BY r.PRBRCDGO;

-- 1.2 El choque: alternos con MAS DE UN rubro. Deben salir el 232 y el 233.
--     Si aparece algun otro par, es un choque PREEXISTENTE ajeno a este cambio:
--     anotarlo y reportarlo, pero NO arreglarlo aqui.
SELECT  r.PRBRALTR,
        COUNT(*) AS RUBROS,
        LISTAGG(r.PRBRCDGO || ' = ' || r.PRBRDSCR, ' | ')
            WITHIN GROUP (ORDER BY r.PRBRCDGO) AS DETALLE
FROM    SCP.PRBR r
GROUP BY r.PRBRALTR
HAVING  COUNT(*) > 1
ORDER BY 1;

-- 1.3 Los alternos destino deben estar libres. Esperado: 0 filas.
--     Si sale algo, PARAR: hay que elegir otro rango y avisar, porque tambien cambia
--     com.saa.rubros.Rubros.
SELECT r.PRBRCDGO, r.PRBRALTR, r.PRBRDSCR
FROM   SCP.PRBR r
WHERE  r.PRBRALTR IN (235, 236, 237)
AND    r.PRBRCDGO NOT IN (235, 236, 237);


-- =====================================================================================
-- 2. RESPALDO
-- =====================================================================================
CREATE TABLE SCP.BKP_PRBR_ALTERNOS_20260827 AS
SELECT r.* FROM SCP.PRBR r WHERE r.PRBRCDGO IN (235, 236, 237);

SELECT COUNT(*) AS FILAS_RESPALDADAS FROM SCP.BKP_PRBR_ALTERNOS_20260827;


-- =====================================================================================
-- 3. LA CORRECCION
-- =====================================================================================
-- El alterno queda igual al PK. Se filtra ademas por el alterno viejo para que, si el
-- script se corre dos veces, la segunda no afecte ninguna fila.

UPDATE SCP.PRBR SET PRBRALTR = 235 WHERE PRBRCDGO = 235 AND PRBRALTR = 231;
UPDATE SCP.PRBR SET PRBRALTR = 236 WHERE PRBRCDGO = 236 AND PRBRALTR = 232;
UPDATE SCP.PRBR SET PRBRALTR = 237 WHERE PRBRCDGO = 237 AND PRBRALTR = 233;

COMMIT;


-- =====================================================================================
-- 4. CONTROLES POSTERIORES
-- =====================================================================================

-- 4.1 Los tres rubros con PK = alterno. Esperado: 3 filas, 235/236/237 en ambas columnas.
SELECT r.PRBRCDGO, r.PRBRALTR, r.PRBRDSCR
FROM   SCP.PRBR r
WHERE  r.PRBRCDGO IN (235, 236, 237)
ORDER BY r.PRBRCDGO;

-- 4.2 Ya no hay alternos duplicados por culpa de este cambio.
--     El 232 y el 233 deben desaparecer de este resultado.
SELECT  r.PRBRALTR, COUNT(*) AS RUBROS,
        LISTAGG(r.PRBRCDGO || ' = ' || r.PRBRDSCR, ' | ')
            WITHIN GROUP (ORDER BY r.PRBRCDGO) AS DETALLE
FROM    SCP.PRBR r
GROUP BY r.PRBRALTR
HAVING  COUNT(*) > 1
ORDER BY 1;

-- 4.3 El resultado final, igual al control 7.3 del DDL.
--     Esperado: 235 -> 6 detalles, 236 -> 2, 237 -> 1.
SELECT  r.PRBRALTR AS RUBRO, r.PRBRDSCR, d.PDTRALTR AS DETALLE, d.PDTRDSCR,
        d.PDTRVLRN, d.PDTRESTD
FROM    SCP.PRBR r
LEFT    JOIN SCP.PDTR d ON d.PRBRCDGO = r.PRBRCDGO
WHERE   r.PRBRALTR IN (235, 236, 237)
ORDER BY r.PRBRALTR, d.PDTRALTR;

-- 4.4 Los rubros de TSR quedaron intactos con sus alternos originales.
--     Esperado: los dos rubros de caja chica, con alterno 232 y 233.
SELECT r.PRBRCDGO, r.PRBRALTR, r.PRBRDSCR
FROM   SCP.PRBR r
WHERE  r.PRBRALTR IN (232, 233)
ORDER BY r.PRBRALTR;


-- =====================================================================================
-- 5. REVERSO (si hiciera falta)
-- =====================================================================================
-- UPDATE SCP.PRBR r
-- SET    r.PRBRALTR = (SELECT b.PRBRALTR FROM SCP.BKP_PRBR_ALTERNOS_20260827 b
--                      WHERE b.PRBRCDGO = r.PRBRCDGO)
-- WHERE  EXISTS (SELECT 1 FROM SCP.BKP_PRBR_ALTERNOS_20260827 b
--                WHERE b.PRBRCDGO = r.PRBRCDGO);
-- COMMIT;
--
-- Limpieza del respaldo, una vez validado:
-- DROP TABLE SCP.BKP_PRBR_ALTERNOS_20260827 PURGE;
