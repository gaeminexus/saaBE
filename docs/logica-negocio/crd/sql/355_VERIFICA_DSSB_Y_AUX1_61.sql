-- =====================================================================================
-- 355 — SOLO LECTURA. omen-saa-1, 2026-11-01. Antes del DDL del destino del sobrante (B8).
-- ESPERADO: bloque 1 = 0 filas (el nombre DSSB está libre); bloque 2 = 0 filas (aux1 61 libre en la
-- plantilla 21 de la empresa 1236); bloque 3 = la lista completa, para que el árbitro la revise;
-- bloque 4 = 1 fila (la cuenta existe).
-- =====================================================================================

-- 1. ¿Existe ya una tabla o una secuencia DSSB en algún esquema?
SELECT OWNER, TABLE_NAME AS OBJETO, 'TABLA' AS TIPO FROM ALL_TABLES WHERE TABLE_NAME = 'DSSB'
UNION ALL
SELECT SEQUENCE_OWNER, SEQUENCE_NAME, 'SECUENCIA' FROM ALL_SEQUENCES WHERE SEQUENCE_NAME = 'SQ_DSSBCDGO';

-- 2. ¿El aux1 61 (o el 62, de reserva) ya está ocupado en la plantilla 21?
SELECT d.DTPLCDGO, d.DTPLAXL1 AS AUX1, d.DTPLAXL2 AS AUX2, n.PLNNCNTA AS CUENTA, d.DTPLDSCR, d.DTPLESTD
  FROM CNT.PLNS p JOIN CNT.DTPL d ON d.PLNSCDGO = p.PLNSCDGO
  LEFT JOIN CNT.PLNN n ON n.PLNNCDGO = d.PLNNCDGO
 WHERE p.PLNSCDAL = 21 AND p.PJRQCDGO = 1236 AND d.DTPLAXL1 IN (61, 62);

-- 3. Todas las líneas de la plantilla 21 (para revisar contra CrdLineaAsiento)
SELECT d.DTPLAXL1 AS AUX1, d.DTPLAXL2 AS AUX2, d.DTPLMVMN AS MOV, n.PLNNCNTA AS CUENTA, d.DTPLDSCR, d.DTPLESTD
  FROM CNT.PLNS p JOIN CNT.DTPL d ON d.PLNSCDGO = p.PLNSCDGO
  LEFT JOIN CNT.PLNN n ON n.PLNNCDGO = d.PLNNCDGO
 WHERE p.PLNSCDAL = 21 AND p.PJRQCDGO = 1236
 ORDER BY d.DTPLAXL1, d.DTPLAXL2;

-- 4. ¿Existe la cuenta 2.3.90.90.10? (la que llevará la línea nueva)
SELECT * FROM CNT.PLNN WHERE PLNNCNTA = '2.3.90.90.10';
