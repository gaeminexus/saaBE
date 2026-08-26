-- =====================================================================================
-- ACTUALIZACION DE PLANTILLAS CONTABLES PARA EL CIERRE DE CARTERA (CRD)
-- Version ejecutable del runbook
--   docs/logica-negocio/crd/ACTUALIZACION-PLANTILLAS-CIERRE-CARTERA.md
-- FECHA: 2026-08-25 · FASE 2
--
-- EL RUNBOOK MANDA: ahi estan el porque de cada cambio, los resultados esperados de cada
--   control y el bloque de deshacer explicado. Este .sql es el mismo contenido en orden
--   ejecutable, para no tener que copiar bloque por bloque.
--
-- SQL PURO: sin SET / DEFINE / WHENEVER (el plugin JDBC de VS Code los rechaza con
--   ORA-00900). Empresa 1236 (ASOPREP) incrustada; si el nodo de empresa es otro,
--   reemplazar 1236 en todo el archivo.
--
-- REQUIERE: sql/DDL-CIERRE-CARTERA.sql ejecutado antes.
-- ORDEN OBLIGATORIO: 2.2 -> 2.3 -> 2.4 -> 2.5. El paso 2.4 filtra por los auxiliares que
--   fija el 2.3, y el 2.5 inserta el papel 40 que el 2.4 ya no debe tocar.
--
-- MODO DE EJECUCION: correr por bloques, revisando la salida de los controles. El COMMIT
--   esta al final: NO ejecutarlo si algun control posterior no cuadra (en ese caso
--   ROLLBACK; y revisar contra el runbook).
--
-- IDEMPOTENCIA (verificada el 2026-08-25 contra la BD local):
--   2.2 y 2.4 reescriben el mismo valor        -> se pueden repetir sin dano.
--   2.3 filtra por aux1 entre 1 y 9            -> la segunda corrida da 0 filas.
--   2.5 lleva guarda NOT EXISTS anadida en la revision -> la segunda corrida da 0 filas.
--   Sin esa guarda el INSERT duplicaria las tres lineas del papel 40, y eso solo se
--   detectaria en el control 3.3.
-- =====================================================================================


-- =====================================================================================
-- 1. CONTROLES PREVIOS
-- =====================================================================================

-- 1.1 Las tres plantillas existen y estan activas (esperado: 3 filas, PLNSESTD = 1).
--     Los PLNSCDGO seran distintos en cada instalacion: los cambios resuelven la
--     plantilla por (PLNSCDAL, PJRQCDGO) y nunca por id.
SELECT PLNSCDGO, PLNSCDAL, PLNSNMBR, PLNSESTD, PJRQCDGO
FROM CNT.PLNS WHERE PJRQCDGO = 1236 AND PLNSCDAL IN (1, 17, 33) ORDER BY PLNSCDAL;

-- 1.2 Estado actual de los auxiliares
--     (esperado ANTES de los cambios: 4 lineas en el alterno 1, 9 en el 17, 4 en el 33;
--      todas con aux1 posicional 1..N y aux2 = 0).
SELECT s.PLNSCDAL alterno, d.DTPLCDGO, d.DTPLAXL1 aux1, d.DTPLAXL2 aux2,
       DECODE(d.DTPLMVMN, 1, 'DEBE', 2, 'HABER') movimiento,
       c.PLNNCNTA cuenta, d.DTPLDSCR, d.DTPLESTD
FROM CNT.DTPL d
JOIN CNT.PLNS s ON s.PLNSCDGO = d.PLNSCDGO
LEFT JOIN CNT.PLNN c ON c.PLNNCDGO = d.PLNNCDGO
WHERE s.PJRQCDGO = 1236 AND s.PLNSCDAL IN (1, 17, 33)
ORDER BY s.PLNSCDAL, d.DTPLAXL1;

-- 1.3 Tipos de prestamo (esperado: 1 QUIROGRAFARIO, 2 HIPOTECARIO, 3 PRENDARIO)
SELECT TPPRCDGO, TPPRNMBR FROM CRD.TPPR ORDER BY TPPRCDGO;

-- 1.4 Productos por tipo. Esperado: solo los tipos 1, 2 y 3. Si en produccion hubiera
--     productos en los tipos 4 o 5, HAY QUE anadirles sus lineas o el devengo fallara.
SELECT TPPRCDGO, COUNT(*) productos FROM CRD.PRDC GROUP BY TPPRCDGO ORDER BY 1;


-- =====================================================================================
-- 2. CAMBIOS
-- 2.1 Alterno 1 (apertura): SIN CAMBIOS, sus auxiliares ya coinciden con el catalogo.
-- =====================================================================================

-- 2.2 Alterno 33 (neteo): intercambiar los papeles 1<->3 y 2<->4.
--     Se ancla en la CUENTA, que es lo estable entre instalaciones, no en los ids.
--     Esperado: 4 filas actualizadas.
UPDATE CNT.DTPL d
SET d.DTPLAXL1 = CASE (SELECT c.PLNNCNTA FROM CNT.PLNN c WHERE c.PLNNCDGO = d.PLNNCDGO)
                   WHEN '2.3.02.05' THEN 3
                   WHEN '2.3.02.10' THEN 4
                   WHEN '1.4.05.05' THEN 1
                   WHEN '1.4.05.10' THEN 2
                   ELSE d.DTPLAXL1 END
WHERE d.PLNSCDGO = (SELECT s.PLNSCDGO FROM CNT.PLNS s
                    WHERE s.PLNSCDAL = 33 AND s.PJRQCDGO = 1236);

-- 2.3 Alterno 17 (devengo): fijar aux1 = papel y aux2 = tipo de prestamo.
--     Se ancla en el aux1 posicional actual (1..9), unico discriminante fiable mientras
--     las cuentas se repiten entre el bloque de ordinario y el de mora.
--     Esperado: 9 filas actualizadas (0 si ya se aplico).
UPDATE CNT.DTPL d
SET d.DTPLAXL1 = CASE d.DTPLAXL1
                   WHEN 1 THEN 10 WHEN 2 THEN 10 WHEN 3 THEN 10
                   WHEN 4 THEN 20 WHEN 5 THEN 20 WHEN 6 THEN 20
                   WHEN 7 THEN 30 WHEN 8 THEN 30 WHEN 9 THEN 30
                   ELSE d.DTPLAXL1 END,
    d.DTPLAXL2 = CASE d.DTPLAXL1
                   WHEN 1 THEN 1 WHEN 2 THEN 3 WHEN 3 THEN 2
                   WHEN 4 THEN 1 WHEN 5 THEN 3 WHEN 6 THEN 2
                   WHEN 7 THEN 1 WHEN 8 THEN 3 WHEN 9 THEN 2
                   ELSE d.DTPLAXL2 END
WHERE d.PLNSCDGO = (SELECT s.PLNSCDGO FROM CNT.PLNS s
                    WHERE s.PLNSCDAL = 17 AND s.PJRQCDGO = 1236)
  AND d.DTPLAXL1 BETWEEN 1 AND 9;

-- 2.4 Alterno 17: corregir la errata de descripcion de la linea de mora prendaria.
--     La descripcion NO es cosmetica: distingue mora de ordinario en el mayor, porque
--     las dos comparten cuenta (decision D3 del levantamiento).
--     Esperado: 1 fila actualizada.
UPDATE CNT.DTPL d
SET d.DTPLDSCR = 'INTERESES POR MORA'
WHERE d.PLNSCDGO = (SELECT s.PLNSCDGO FROM CNT.PLNS s
                    WHERE s.PLNSCDAL = 17 AND s.PJRQCDGO = 1236)
  AND d.DTPLAXL1 = 20 AND d.DTPLAXL2 = 3;

-- 2.5 Alterno 17: crear las tres lineas de INGRESO POR MORA que faltan (papel 40).
--     La guarda NOT EXISTS evita duplicarlas si el guion se corre dos veces.
--     Esperado: 3 filas insertadas (0 si ya se aplico).
INSERT INTO CNT.DTPL (DTPLCDGO, PLNSCDGO, PLNNCDGO, DTPLDSCR, DTPLMVMN,
                      DTPLAXL1, DTPLAXL2, DTPLAXL3, DTPLAXL4, DTPLAXL5, DTPLESTD)
SELECT CNT.SQ_DTPLCDGO.NEXTVAL, m.plns, m.plnn, 'INGRESO POR INTERES DE MORA', 2,
       40, m.tipo, 0, 0, 0, 1
FROM (
  SELECT s.PLNSCDGO plns, c.PLNNCDGO plnn, 1 tipo
  FROM CNT.PLNS s, CNT.PLNN c
  WHERE s.PLNSCDAL = 17 AND s.PJRQCDGO = 1236 AND c.PLNNCNTA = '5.1.02.05' AND c.PJRQCDGO = 1236
  UNION ALL
  SELECT s.PLNSCDGO, c.PLNNCDGO, 3
  FROM CNT.PLNS s, CNT.PLNN c
  WHERE s.PLNSCDAL = 17 AND s.PJRQCDGO = 1236 AND c.PLNNCNTA = '5.1.02.10' AND c.PJRQCDGO = 1236
  UNION ALL
  SELECT s.PLNSCDGO, c.PLNNCDGO, 2
  FROM CNT.PLNS s, CNT.PLNN c
  WHERE s.PLNSCDAL = 17 AND s.PJRQCDGO = 1236 AND c.PLNNCNTA = '5.1.02.15' AND c.PJRQCDGO = 1236
) m
WHERE NOT EXISTS (
  SELECT 1 FROM CNT.DTPL x
  WHERE x.PLNSCDGO = m.plns AND x.DTPLAXL1 = 40 AND x.DTPLAXL2 = m.tipo
);


-- =====================================================================================
-- 3. CONTROLES POSTERIORES — los cuatro deben cuadrar ANTES del COMMIT
-- =====================================================================================

-- 3.1 Los cuatro papeles de apertura y neteo, cada uno con su cuenta.
--     Esperado exactamente 8 filas: el MISMO papel con la MISMA cuenta y el lado
--     contrario entre el alterno 1 y el 33.
--       1 |1|DEBE |1.4.05.05    33|1|HABER|1.4.05.05
--       1 |2|DEBE |1.4.05.10    33|2|HABER|1.4.05.10
--       1 |3|HABER|2.3.02.05    33|3|DEBE |2.3.02.05
--       1 |4|HABER|2.3.02.10    33|4|DEBE |2.3.02.10
SELECT s.PLNSCDAL alterno, d.DTPLAXL1 papel,
       DECODE(d.DTPLMVMN, 1, 'DEBE', 2, 'HABER') mov, c.PLNNCNTA cuenta
FROM CNT.DTPL d
JOIN CNT.PLNS s ON s.PLNSCDGO = d.PLNSCDGO
JOIN CNT.PLNN c ON c.PLNNCDGO = d.PLNNCDGO
WHERE s.PJRQCDGO = 1236 AND s.PLNSCDAL IN (1, 33) AND d.DTPLESTD = 1
ORDER BY s.PLNSCDAL, d.DTPLAXL1;

-- 3.2 Las doce lineas del devengo. Esperado: 12 filas — papeles 10, 20, 30 y 40, cada
--     uno con los tipos 1, 2 y 3. Los 10 y 20 al DEBE sobre 1.4.02.xx; los 30 y 40 al
--     HABER sobre 5.1.02.xx.
SELECT d.DTPLAXL1 papel, d.DTPLAXL2 tipo_prestamo,
       DECODE(d.DTPLMVMN, 1, 'DEBE', 2, 'HABER') mov, c.PLNNCNTA cuenta, d.DTPLDSCR
FROM CNT.DTPL d
JOIN CNT.PLNS s ON s.PLNSCDGO = d.PLNSCDGO
JOIN CNT.PLNN c ON c.PLNNCDGO = d.PLNNCDGO
WHERE s.PJRQCDGO = 1236 AND s.PLNSCDAL = 17 AND d.DTPLESTD = 1
ORDER BY d.DTPLAXL1, d.DTPLAXL2;

-- 3.3 Ninguna combinacion (plantilla, papel, tipo) repetida. Esperado: 0 filas.
--     Si sale alguna, el servicio tomaria la primera por codigo y el asiento saldria
--     con una cuenta arbitraria.
SELECT s.PLNSCDAL, d.DTPLAXL1, d.DTPLAXL2, COUNT(*) veces
FROM CNT.DTPL d JOIN CNT.PLNS s ON s.PLNSCDGO = d.PLNSCDGO
WHERE s.PJRQCDGO = 1236 AND s.PLNSCDAL IN (1, 17, 33) AND d.DTPLESTD = 1
GROUP BY s.PLNSCDAL, d.DTPLAXL1, d.DTPLAXL2
HAVING COUNT(*) > 1;

-- 3.4 Ninguna linea activa sin cuenta. Esperado: 0 filas.
SELECT s.PLNSCDAL, d.DTPLCDGO, d.DTPLAXL1
FROM CNT.DTPL d JOIN CNT.PLNS s ON s.PLNSCDGO = d.PLNSCDGO
WHERE s.PJRQCDGO = 1236 AND s.PLNSCDAL IN (1, 17, 33)
  AND d.DTPLESTD = 1 AND d.PLNNCDGO IS NULL;


-- =====================================================================================
-- 4. COMMIT — solo con los cuatro controles cuadrando. Si no: ROLLBACK;
-- =====================================================================================

COMMIT;


-- =====================================================================================
-- 5. DESHACER (NO ejecutar salvo que haya que revertir; explicado en §5 del runbook)
--    El DELETE va ANTES del ultimo UPDATE: si se invierte el orden, las filas del papel
--    40 ya no se distinguen y el borrado se lleva lo que no debe.
--
-- UPDATE CNT.DTPL d
-- SET d.DTPLAXL1 = CASE (SELECT c.PLNNCNTA FROM CNT.PLNN c WHERE c.PLNNCDGO = d.PLNNCDGO)
--                    WHEN '2.3.02.05' THEN 1 WHEN '2.3.02.10' THEN 2
--                    WHEN '1.4.05.05' THEN 3 WHEN '1.4.05.10' THEN 4
--                    ELSE d.DTPLAXL1 END
-- WHERE d.PLNSCDGO = (SELECT s.PLNSCDGO FROM CNT.PLNS s
--                     WHERE s.PLNSCDAL = 33 AND s.PJRQCDGO = 1236);
--
-- DELETE FROM CNT.DTPL d
-- WHERE d.PLNSCDGO = (SELECT s.PLNSCDGO FROM CNT.PLNS s
--                     WHERE s.PLNSCDAL = 17 AND s.PJRQCDGO = 1236)
--   AND d.DTPLAXL1 = 40;
--
-- UPDATE CNT.DTPL d
-- SET d.DTPLAXL1 = CASE d.DTPLAXL1 * 10 + d.DTPLAXL2
--                    WHEN 101 THEN 1 WHEN 103 THEN 2 WHEN 102 THEN 3
--                    WHEN 201 THEN 4 WHEN 203 THEN 5 WHEN 202 THEN 6
--                    WHEN 301 THEN 7 WHEN 303 THEN 8 WHEN 302 THEN 9
--                    ELSE d.DTPLAXL1 END,
--     d.DTPLAXL2 = 0
-- WHERE d.PLNSCDGO = (SELECT s.PLNSCDGO FROM CNT.PLNS s
--                     WHERE s.PLNSCDAL = 17 AND s.PJRQCDGO = 1236)
--   AND d.DTPLAXL1 IN (10, 20, 30);
--
-- COMMIT;
-- =====================================================================================
