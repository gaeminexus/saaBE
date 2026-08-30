-- =====================================================================================
-- ACTUALIZACION DE LA PLANTILLA 21 — asiento de APLICACION de Petro
-- Version ejecutable de docs/logica-negocio/crd/ACTUALIZACION-PLANTILLA-21-PETRO-APLICACION.md
-- FECHA: 2026-08-29
--
-- =====================================================================================
-- ESTADO: NO EJECUTADO. Revisado y aprobado por el orquestador el 2026-08-29.
--         Va ANTES del despliegue del WAR (el servicio de aplicacion consume estos
--         auxiliares apenas arranca).
-- =====================================================================================
--
-- El .md sigue siendo el documento de referencia: explica el POR QUE de cada cambio y
-- los valores esperados de cada control con su razonamiento. Este .sql es el mismo
-- contenido en el formato que se corre en el plugin JDBC de VS Code, con los controles
-- intercalados en el mismo orden. SQL puro: sin SET, DEFINE ni WHENEVER.
--
-- QUE RESUELVE
--
-- El asiento de aplicacion de Petro necesita de la plantilla alterno 21 estas lineas:
-- aportes por aplicar, prestamos por aplicar, aportes cesantia, aportes JUBILACION,
-- interes ordinario, interes de mora y seguro de desgravamen. Hay tres problemas:
--
--   1. Los DTPLAXL1 son POSICIONALES (1..44 por orden de captura), no semanticos. Igual
--      que lo que la Fase 2 ya corrigio en los alternos 1/17/33.
--   2. FALTA la linea de aportes JUBILACION (2.1.02.05.01). La plantilla solo tiene
--      cesantia (2.1.01.05.01) y aporte adicional (2.1.02.15). Sin esa linea el asiento
--      no se puede armar.
--   3. Las posiciones 36 y 37 DUPLICAN la cuenta 1.4.02.05 (quirografario); una de las
--      dos deberia ser 1.4.02.10 (prendario). Errata ya anotada en §8.3.2 del
--      levantamiento, sin corregir hasta ahora.
--
-- Catalogo semantico destino (com.saa.rubros.CrdLineaAsiento):
--
--   3  APORTES_POR_APLICAR            2.3.02.05
--   4  PRESTAMOS_POR_APLICAR          2.3.02.10
--   10 INTERES_ORDINARIO_POR_COBRAR   1.4.02.xx  — dimension en DTPLAXL2
--   20 INTERES_MORA_POR_COBRAR        1.4.02.xx  — misma cuenta que el ordinario
--   50 APORTES_CESANTIA               2.1.01.05.01
--   51 APORTES_JUBILACION             2.1.02.05.01  <- LINEA NUEVA
--   52 APORTE_ADICIONAL_PERSONAL      2.1.02.15     — solo se renumera para no chocar
--   60 SEGURO_DESGRAVAMEN             1.4.90.90.10
--
-- DTPLAXL2 en las lineas de interes = CRD.TPPR.TPPRCDGO (1 quirografario, 2 hipotecario,
-- 3 prendario), mismo criterio que la Fase 2.
--
-- LO QUE NO SE TOCA:
--   - Las 34 lineas de BANDA (DTPLAXL1 5-35, cuentas 1.3.01.xx-1.3.12.xx). El capital por
--     banda sale de CRD.BNDP via ClasificadorBandaService. Su saneamiento es Fase 4 (§8.3).
--   - Las lineas de seguro de prestamo (42 hipotecario, 43 prendario). El asiento 2 de
--     Petro no las usa; quedan para el pago manual (§3.4).
--   - Las plantillas 19 y 20 (cobro). Son CORRECTAS tal como estan — decision del usuario
--     del 2026-08-28, ver la regla 11 de §5 del levantamiento.
--
-- ⚠ PJRQCDGO = 1236 es la empresa. Si se corre en un ambiente con otra empresa, cambiarlo
--   en TODAS las consultas. El control 1.1 lo detecta: si no devuelve fila, parar.
--
-- IDEMPOTENTE: los UPDATE filtran por el valor VIEJO, que deja de existir apenas se
-- aplican; el INSERT tiene NOT EXISTS. Seguro de repetir. El unico que falla en una
-- segunda corrida es el CREATE TABLE del respaldo, y es deliberado.
--
-- Contenido:
--   1. Controles PREVIOS
--   2. Respaldo + cambios
--   3. Controles POSTERIORES
--   4. Reverso — COMENTADO, no ejecutar
-- =====================================================================================


-- =====================================================================================
-- 1. CONTROLES PREVIOS — no continuar si algo no cuadra
-- =====================================================================================

-- 1.1 La plantilla existe y esta activa. Esperado: 1 fila con PLNSESTD = 1.
--     En produccion el PLNSCDGO sera distinto al de local: todos los pasos resuelven la
--     plantilla por (PLNSCDAL, PJRQCDGO), nunca por el id. Si NO devuelve fila, PARAR:
--     o la empresa no es 1236 o la plantilla no existe en este ambiente.
SELECT PLNSCDGO, PLNSCDAL, PLNSNMBR, PLNSESTD, PJRQCDGO
FROM   CNT.PLNS WHERE PJRQCDGO = 1236 AND PLNSCDAL = 21;

-- 1.2 Estado actual de las lineas no-banda. Esperado: 10 filas.
--     aux1=1 2.3.02.05 DEBE / aux1=2 2.3.02.10 DEBE / aux1=3 2.1.01.05.01 HABER /
--     aux1=4 2.1.02.15 HABER / aux1=36 1.4.02.05 HABER / aux1=37 1.4.02.05 HABER (el
--     duplicado) / aux1=38 1.4.02.15 HABER / aux1=39 1.4.02.05 HABER MORA /
--     aux1=40 1.4.02.10 HABER MORA / aux1=41 1.4.02.15 HABER MORA / aux1=44 1.4.90.90.10.
--     NINGUNA linea para 2.1.02.05.01 — eso es justo lo que agrega el paso 2.2.
SELECT d.DTPLCDGO, c.PLNNCNTA AS CUENTA, d.DTPLDSCR,
       DECODE(d.DTPLMVMN, 1, 'DEBE', 2, 'HABER') AS MOVIMIENTO,
       d.DTPLAXL1 AS AUX1, d.DTPLAXL2 AS AUX2
FROM   CNT.DTPL d
JOIN   CNT.PLNS s ON s.PLNSCDGO = d.PLNSCDGO
JOIN   CNT.PLNN c ON c.PLNNCDGO = d.PLNNCDGO
WHERE  s.PLNSCDAL = 21 AND s.PJRQCDGO = 1236 AND d.DTPLAXL1 NOT BETWEEN 5 AND 35
ORDER  BY d.DTPLAXL1;

-- 1.3 Las dos cuentas destino existen para la empresa. Esperado: 2 filas.
--     Sin estas, los pasos 2.2 y 2.3 no tienen a donde apuntar. NO CONTINUAR si falta alguna.
SELECT PLNNCDGO, PLNNCNTA, PLNNNMBR FROM CNT.PLNN
WHERE  PJRQCDGO = 1236 AND PLNNCNTA IN ('2.1.02.05.01', '1.4.02.10');

-- 1.4 La secuencia del INSERT del paso 2.2 existe. Esperado: 1 fila.
SELECT SEQUENCE_OWNER, SEQUENCE_NAME FROM ALL_SEQUENCES
WHERE  SEQUENCE_OWNER = 'CNT' AND SEQUENCE_NAME = 'SQ_DTPLCDGO';

-- 1.5 Los aux2 de partida de las seis lineas de interes. Esperado: las 6 con DTPLAXL2 = 0
--     (no NULL) — verificado en local el 2026-08-28. El reverso del bloque 4 restaura 0
--     para las seis; si aca saliera NULL, hay que ajustar ese reverso antes de seguir.
SELECT d.DTPLAXL1, d.DTPLAXL2 FROM CNT.DTPL d
WHERE  d.PLNSCDGO = (SELECT s.PLNSCDGO FROM CNT.PLNS s
                     WHERE s.PLNSCDAL = 21 AND s.PJRQCDGO = 1236)
AND    d.DTPLAXL1 IN (36, 37, 38, 39, 40, 41) ORDER BY d.DTPLAXL1;


-- =====================================================================================
-- 2. RESPALDO Y CAMBIOS
-- =====================================================================================

-- 2.0 RESPALDO — antes de tocar nada. Guarda la fila COMPLETA de todas las lineas
--     no-banda, para poder reversar cualquier columna aunque alguien corra el documento
--     por partes: el mapeo inverso del bloque 4 cubre el camino feliz, esto cubre el resto.
--     ⚠ Cambiar la fecha del nombre si se corre otro dia.
CREATE TABLE CNT.BKP_DTPL_P21_20260829 AS
SELECT d.* FROM CNT.DTPL d
WHERE  d.PLNSCDGO = (SELECT s.PLNSCDGO FROM CNT.PLNS s
                     WHERE s.PLNSCDAL = 21 AND s.PJRQCDGO = 1236)
AND    d.DTPLAXL1 NOT BETWEEN 5 AND 35;

-- Esperado: 10 filas, las mismas del control 1.2. NO CONTINUAR si no da 10.
SELECT COUNT(*) AS FILAS_RESPALDADAS FROM CNT.BKP_DTPL_P21_20260829;


-- 2.1 Renumerar por aplicar, cesantia y aporte adicional.
--     Se ancla en la CUENTA, no en el id. Esperado: 4 filas actualizadas.
UPDATE CNT.DTPL d
SET    d.DTPLAXL1 = CASE (SELECT c.PLNNCNTA FROM CNT.PLNN c WHERE c.PLNNCDGO = d.PLNNCDGO)
                      WHEN '2.3.02.05'    THEN 3
                      WHEN '2.3.02.10'    THEN 4
                      WHEN '2.1.01.05.01' THEN 50
                      WHEN '2.1.02.15'    THEN 52
                      ELSE d.DTPLAXL1 END
WHERE  d.PLNSCDGO = (SELECT s.PLNSCDGO FROM CNT.PLNS s
                     WHERE s.PLNSCDAL = 21 AND s.PJRQCDGO = 1236)
AND    d.DTPLAXL1 IN (1, 2, 3, 4);


-- 2.2 Agregar la linea de aportes JUBILACION que falta.
--     Esperado: 1 fila insertada la primera vez, 0 despues (NOT EXISTS).
INSERT INTO CNT.DTPL (DTPLCDGO, PLNSCDGO, PLNNCDGO, DTPLDSCR, DTPLMVMN,
                      DTPLAXL1, DTPLAXL2, DTPLAXL3, DTPLAXL4, DTPLAXL5, DTPLESTD)
SELECT CNT.SQ_DTPLCDGO.NEXTVAL, s.PLNSCDGO, c.PLNNCDGO,
       'APORTES PERSONALES JUBILACION', 2, 51, 0, 0, 0, 0, 1
FROM   CNT.PLNS s, CNT.PLNN c
WHERE  s.PLNSCDAL = 21 AND s.PJRQCDGO = 1236
AND    c.PLNNCNTA = '2.1.02.05.01' AND c.PJRQCDGO = 1236
AND    NOT EXISTS (SELECT 1 FROM CNT.DTPL x
                   WHERE x.PLNSCDGO = s.PLNSCDGO AND x.DTPLAXL1 = 51);


-- 2.3 Interes: corregir el duplicado y renumerar las seis lineas.
--     Esperado: 1 fila por cada UPDATE (6 en total) la primera vez, 0 despues — cada
--     WHERE filtra por el aux1 VIEJO, que deja de existir al aplicarse.

-- 36 -> ordinario QUIROGRAFARIO (cuenta ya correcta, solo se renumera)
UPDATE CNT.DTPL d SET d.DTPLAXL1 = 10, d.DTPLAXL2 = 1
WHERE  d.PLNSCDGO = (SELECT s.PLNSCDGO FROM CNT.PLNS s
                     WHERE s.PLNSCDAL = 21 AND s.PJRQCDGO = 1236)
AND    d.DTPLAXL1 = 36;

-- 37 -> ordinario PRENDARIO. ERRATA CORREGIDA: la cuenta pasa de 1.4.02.05 a 1.4.02.10.
UPDATE CNT.DTPL d
SET    d.PLNNCDGO = (SELECT c.PLNNCDGO FROM CNT.PLNN c
                     WHERE c.PLNNCNTA = '1.4.02.10' AND c.PJRQCDGO = 1236),
       d.DTPLAXL1 = 10, d.DTPLAXL2 = 3
WHERE  d.PLNSCDGO = (SELECT s.PLNSCDGO FROM CNT.PLNS s
                     WHERE s.PLNSCDAL = 21 AND s.PJRQCDGO = 1236)
AND    d.DTPLAXL1 = 37;

-- 38 -> ordinario HIPOTECARIO (cuenta ya correcta)
UPDATE CNT.DTPL d SET d.DTPLAXL1 = 10, d.DTPLAXL2 = 2
WHERE  d.PLNSCDGO = (SELECT s.PLNSCDGO FROM CNT.PLNS s
                     WHERE s.PLNSCDAL = 21 AND s.PJRQCDGO = 1236)
AND    d.DTPLAXL1 = 38;

-- 39 -> mora QUIROGRAFARIO
UPDATE CNT.DTPL d SET d.DTPLAXL1 = 20, d.DTPLAXL2 = 1
WHERE  d.PLNSCDGO = (SELECT s.PLNSCDGO FROM CNT.PLNS s
                     WHERE s.PLNSCDAL = 21 AND s.PJRQCDGO = 1236)
AND    d.DTPLAXL1 = 39;

-- 40 -> mora PRENDARIO (cuenta ya correcta, 1.4.02.10)
UPDATE CNT.DTPL d SET d.DTPLAXL1 = 20, d.DTPLAXL2 = 3
WHERE  d.PLNSCDGO = (SELECT s.PLNSCDGO FROM CNT.PLNS s
                     WHERE s.PLNSCDAL = 21 AND s.PJRQCDGO = 1236)
AND    d.DTPLAXL1 = 40;

-- 41 -> mora HIPOTECARIO
UPDATE CNT.DTPL d SET d.DTPLAXL1 = 20, d.DTPLAXL2 = 2
WHERE  d.PLNSCDGO = (SELECT s.PLNSCDGO FROM CNT.PLNS s
                     WHERE s.PLNSCDAL = 21 AND s.PJRQCDGO = 1236)
AND    d.DTPLAXL1 = 41;


-- 2.4 Renumerar el seguro de desgravamen. Esperado: 1 fila.
UPDATE CNT.DTPL d SET d.DTPLAXL1 = 60
WHERE  d.PLNSCDGO = (SELECT s.PLNSCDGO FROM CNT.PLNS s
                     WHERE s.PLNSCDAL = 21 AND s.PJRQCDGO = 1236)
AND    d.DTPLAXL1 = 44;


-- =====================================================================================
-- 3. CONTROLES POSTERIORES — revisar los cuatro ANTES del COMMIT
-- =====================================================================================

-- 3.1 Las lineas que necesita el asiento, cada una con su cuenta. Esperado: 11 filas.
--       3  -   DEBE  2.3.02.05
--       4  -   DEBE  2.3.02.10
--      10  1  HABER  1.4.02.05      10  2  HABER  1.4.02.15      10  3  HABER  1.4.02.10
--      20  1  HABER  1.4.02.05      20  2  HABER  1.4.02.15      20  3  HABER  1.4.02.10
--      50  -  HABER  2.1.01.05.01
--      51  -  HABER  2.1.02.05.01
--      60  -  HABER  1.4.90.90.10
SELECT d.DTPLAXL1 AS PAPEL, d.DTPLAXL2 AS TIPO_PRESTAMO,
       DECODE(d.DTPLMVMN, 1, 'DEBE', 2, 'HABER') AS MOV,
       c.PLNNCNTA AS CUENTA, d.DTPLDSCR
FROM   CNT.DTPL d
JOIN   CNT.PLNS s ON s.PLNSCDGO = d.PLNSCDGO
JOIN   CNT.PLNN c ON c.PLNNCDGO = d.PLNNCDGO
WHERE  s.PLNSCDAL = 21 AND s.PJRQCDGO = 1236 AND d.DTPLESTD = 1
AND    d.DTPLAXL1 IN (3, 4, 10, 20, 50, 51, 60)
ORDER  BY d.DTPLAXL1, d.DTPLAXL2;

-- 3.2 Ninguna combinacion (papel, tipo) repetida en toda la plantilla. Esperado: 0 filas.
--     Si sale alguna, dos lineas compiten por el mismo papel y el servicio tomaria una
--     arbitrariamente, con la cuenta equivocada.
SELECT d.DTPLAXL1, d.DTPLAXL2, COUNT(*) AS VECES
FROM   CNT.DTPL d
JOIN   CNT.PLNS s ON s.PLNSCDGO = d.PLNSCDGO
WHERE  s.PLNSCDAL = 21 AND s.PJRQCDGO = 1236 AND d.DTPLESTD = 1
GROUP  BY d.DTPLAXL1, d.DTPLAXL2 HAVING COUNT(*) > 1;

-- 3.3 Ninguna linea sin cuenta. Esperado: 0 filas.
--     Atrapa el caso de que el subquery del paso 2.3 (linea 37) no haya encontrado
--     1.4.02.10 y hubiera dejado PLNNCDGO en NULL.
SELECT d.DTPLCDGO, d.DTPLAXL1
FROM   CNT.DTPL d
JOIN   CNT.PLNS s ON s.PLNSCDGO = d.PLNSCDGO
WHERE  s.PLNSCDAL = 21 AND s.PJRQCDGO = 1236 AND d.DTPLESTD = 1 AND d.PLNNCDGO IS NULL;

-- 3.4 Total de lineas. Esperado: 45 (las 44 originales + la de jubilacion del paso 2.2).
SELECT COUNT(*) AS TOTAL_LINEAS FROM CNT.DTPL d
JOIN   CNT.PLNS s ON s.PLNSCDGO = d.PLNSCDGO
WHERE  s.PLNSCDAL = 21 AND s.PJRQCDGO = 1236 AND d.DTPLESTD = 1;

-- Si los cuatro controles pasan:  COMMIT;


-- =====================================================================================
-- 4. ⛔ REVERSO — NO EJECUTAR salvo que haya que deshacer los cambios
-- =====================================================================================
-- Descomentar SOLO si los controles del bloque 3 fallan. Si el estado real no coincide
-- con lo que dejo este script, usar el respaldo CNT.BKP_DTPL_P21_20260829 en vez de este
-- mapeo inverso.
--
-- UPDATE CNT.DTPL d
-- SET    d.DTPLAXL1 = CASE (SELECT c.PLNNCNTA FROM CNT.PLNN c WHERE c.PLNNCDGO = d.PLNNCDGO)
--                       WHEN '2.3.02.05'    THEN 1
--                       WHEN '2.3.02.10'    THEN 2
--                       WHEN '2.1.01.05.01' THEN 3
--                       WHEN '2.1.02.15'    THEN 4
--                       ELSE d.DTPLAXL1 END
-- WHERE  d.PLNSCDGO = (SELECT s.PLNSCDGO FROM CNT.PLNS s
--                      WHERE s.PLNSCDAL = 21 AND s.PJRQCDGO = 1236)
-- AND    d.DTPLAXL1 IN (3, 4, 50, 52);
--
-- DELETE FROM CNT.DTPL d
-- WHERE  d.PLNSCDGO = (SELECT s.PLNSCDGO FROM CNT.PLNS s
--                      WHERE s.PLNSCDAL = 21 AND s.PJRQCDGO = 1236)
-- AND    d.DTPLAXL1 = 51;
--
-- UPDATE CNT.DTPL d SET d.DTPLAXL1 = 36, d.DTPLAXL2 = 0
-- WHERE  d.PLNSCDGO = (SELECT s.PLNSCDGO FROM CNT.PLNS s WHERE s.PLNSCDAL = 21 AND s.PJRQCDGO = 1236)
-- AND    d.DTPLAXL1 = 10 AND d.DTPLAXL2 = 1;
--
-- UPDATE CNT.DTPL d
-- SET    d.PLNNCDGO = (SELECT c.PLNNCDGO FROM CNT.PLNN c
--                      WHERE c.PLNNCNTA = '1.4.02.05' AND c.PJRQCDGO = 1236),
--        d.DTPLAXL1 = 37, d.DTPLAXL2 = 0
-- WHERE  d.PLNSCDGO = (SELECT s.PLNSCDGO FROM CNT.PLNS s WHERE s.PLNSCDAL = 21 AND s.PJRQCDGO = 1236)
-- AND    d.DTPLAXL1 = 10 AND d.DTPLAXL2 = 3;
--
-- UPDATE CNT.DTPL d SET d.DTPLAXL1 = 38, d.DTPLAXL2 = 0
-- WHERE  d.PLNSCDGO = (SELECT s.PLNSCDGO FROM CNT.PLNS s WHERE s.PLNSCDAL = 21 AND s.PJRQCDGO = 1236)
-- AND    d.DTPLAXL1 = 10 AND d.DTPLAXL2 = 2;
--
-- UPDATE CNT.DTPL d SET d.DTPLAXL1 = 39, d.DTPLAXL2 = 0
-- WHERE  d.PLNSCDGO = (SELECT s.PLNSCDGO FROM CNT.PLNS s WHERE s.PLNSCDAL = 21 AND s.PJRQCDGO = 1236)
-- AND    d.DTPLAXL1 = 20 AND d.DTPLAXL2 = 1;
--
-- UPDATE CNT.DTPL d SET d.DTPLAXL1 = 40, d.DTPLAXL2 = 0
-- WHERE  d.PLNSCDGO = (SELECT s.PLNSCDGO FROM CNT.PLNS s WHERE s.PLNSCDAL = 21 AND s.PJRQCDGO = 1236)
-- AND    d.DTPLAXL1 = 20 AND d.DTPLAXL2 = 3;
--
-- UPDATE CNT.DTPL d SET d.DTPLAXL1 = 41, d.DTPLAXL2 = 0
-- WHERE  d.PLNSCDGO = (SELECT s.PLNSCDGO FROM CNT.PLNS s WHERE s.PLNSCDAL = 21 AND s.PJRQCDGO = 1236)
-- AND    d.DTPLAXL1 = 20 AND d.DTPLAXL2 = 2;
--
-- UPDATE CNT.DTPL d SET d.DTPLAXL1 = 44
-- WHERE  d.PLNSCDGO = (SELECT s.PLNSCDGO FROM CNT.PLNS s WHERE s.PLNSCDAL = 21 AND s.PJRQCDGO = 1236)
-- AND    d.DTPLAXL1 = 60;
--
-- COMMIT;
--
-- El respaldo se conserva hasta que el cambio este verificado en produccion. Despues:
-- DROP TABLE CNT.BKP_DTPL_P21_20260829;
