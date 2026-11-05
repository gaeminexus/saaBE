-- =====================================================================================================
-- c20-08 — CORRECCIÓN POR UPDATE DIRECTO (autorizado): EG-A-2026-10-0011 es un INGRESO → IN-A
-- omen-saa-1-arb · 2026-11-05 · Diagnóstico: c20-07.
--
-- Causa: las cuentas de INVERSIÓN 1.2.01.05.03.xx (TSR.CNBC 440, 441, 444, 445) están registradas como cuentas
-- bancarias de bancos «reales» (BNCOREAL = 1, la marca es por BANCO). La regla del tipo automático ve «banco» a
-- los dos lados del asiento de recuperación de la póliza (DEBE 1.1.02.05.75 ahorros / HABER 1.2.01.05.03.07
-- inversión), neto 0 → lo trata como transferencia entre cuentas propias → EG-A. Es un ingreso: entra dinero a
-- la cuenta de ahorros.
--
-- Qué cambia (solo el asiento 12368): tipo 1258 (EG-A) → 1257 (IN-A) y su numeración por tipo:
--   ASNTNMRO (consecutivo del tipo en la empresa), ASNTNMMS (consecutivo del tipo en el mes) y ASNTNMAL.
-- NO cambia: líneas (DTAS), fecha, valores, numeración por módulo (TSR-2026-10-0032), movimiento bancario.
-- El asiento NO está mayorizado (MYRZCDGO nulo en c20-07 bloque 4).
--
-- Correr bloque por bloque, en orden.
-- =====================================================================================================

-- ---------------------------------------------------------------------------------------------------
-- BLOQUE 0 — CONTROL ANTES. Esperado:
--   (a) una fila: 12368, EG-A-2026-10-0011, PLNTCDGO 1258, ASNTESTD 1, MYRZCDGO nulo;
--   (b) 1257 = IN-A (INGRESO AUTOMATICO) y 1258 = EG-A (EGRESO AUTOMATICO);
--   (c) SIGUIENTE_NMRO y SIGUIENTE_NMMS: los números que va a tomar; NMAL_NUEVO = IN-A-2026-10-NNNN libre;
--   (d) EG-A de octubre con número mayor a 11: si sale alguno, el 0011 deja un hueco en los egresos (avisar).
-- ---------------------------------------------------------------------------------------------------
SELECT a.ASNTCDGO, a.ASNTNMAL, a.PLNTCDGO, a.ASNTESTD, a.MYRZCDGO, a.PJRQCDGO, a.ASNTPRDO, a.ASNTANOO
FROM   CNT.ASNT a WHERE a.ASNTCDGO = 12368;

SELECT p.PLNTCDGO, p.PLNTNMBR FROM CNT.PLNT p WHERE p.PLNTCDGO IN (1257, 1258);

SELECT (SELECT NVL(MAX(x.ASNTNMRO),0) + 1 FROM CNT.ASNT x
         WHERE x.PJRQCDGO = a.PJRQCDGO AND x.PLNTCDGO = 1257)                                   AS SIGUIENTE_NMRO,
       (SELECT NVL(MAX(x.ASNTNMMS),0) + 1 FROM CNT.ASNT x
         WHERE x.PJRQCDGO = a.PJRQCDGO AND x.PLNTCDGO = 1257
           AND x.ASNTPRDO = a.ASNTPRDO AND x.ASNTANOO = a.ASNTANOO)                             AS SIGUIENTE_NMMS,
       'IN-A-' || a.ASNTANOO || '-' || LPAD(a.ASNTPRDO, 2, '0') || '-' ||
       LPAD((SELECT NVL(MAX(x.ASNTNMMS),0) + 1 FROM CNT.ASNT x
              WHERE x.PJRQCDGO = a.PJRQCDGO AND x.PLNTCDGO = 1257
                AND x.ASNTPRDO = a.ASNTPRDO AND x.ASNTANOO = a.ASNTANOO), 4, '0')               AS NMAL_NUEVO,
       (SELECT COUNT(*) FROM CNT.ASNT x WHERE x.ASNTNMAL = 'IN-A-' || a.ASNTANOO || '-' || LPAD(a.ASNTPRDO, 2, '0')
                || '-' || LPAD((SELECT NVL(MAX(y.ASNTNMMS),0) + 1 FROM CNT.ASNT y
                                 WHERE y.PJRQCDGO = a.PJRQCDGO AND y.PLNTCDGO = 1257
                                   AND y.ASNTPRDO = a.ASNTPRDO AND y.ASNTANOO = a.ASNTANOO), 4, '0'))
                                                                                                AS NMAL_YA_EXISTE_DEBE_SER_0
FROM   CNT.ASNT a WHERE a.ASNTCDGO = 12368;

SELECT x.ASNTCDGO, x.ASNTNMAL, x.ASNTESTD FROM CNT.ASNT x
WHERE  x.PLNTCDGO = 1258 AND x.ASNTANOO = 2026 AND x.ASNTPRDO = 10 AND x.ASNTNMMS > 11
ORDER BY x.ASNTNMMS;

-- ---------------------------------------------------------------------------------------------------
-- BLOQUE 1 — RESPALDO. Esperado: 1 fila.
-- ---------------------------------------------------------------------------------------------------
CREATE TABLE CNT.BK_ASNT_12368_EGA_NOV26 AS
SELECT * FROM CNT.ASNT WHERE ASNTCDGO = 12368;

SELECT COUNT(*) AS FILAS_RESPALDO FROM CNT.BK_ASNT_12368_EGA_NOV26;

-- ---------------------------------------------------------------------------------------------------
-- BLOQUE 2 — UPDATE. Esperado: 1 fila actualizada. Luego COMMIT.
-- (Los tres números se calculan en la misma sentencia, con los valores que mostró el BLOQUE 0.)
-- ---------------------------------------------------------------------------------------------------
UPDATE CNT.ASNT a
SET    a.PLNTCDGO = 1257,
       a.ASNTNMRO = (SELECT NVL(MAX(x.ASNTNMRO),0) + 1 FROM CNT.ASNT x
                      WHERE x.PJRQCDGO = a.PJRQCDGO AND x.PLNTCDGO = 1257),
       a.ASNTNMMS = (SELECT NVL(MAX(x.ASNTNMMS),0) + 1 FROM CNT.ASNT x
                      WHERE x.PJRQCDGO = a.PJRQCDGO AND x.PLNTCDGO = 1257
                        AND x.ASNTPRDO = a.ASNTPRDO AND x.ASNTANOO = a.ASNTANOO),
       a.ASNTNMAL = 'IN-A-' || a.ASNTANOO || '-' || LPAD(a.ASNTPRDO, 2, '0') || '-' ||
                    LPAD((SELECT NVL(MAX(x.ASNTNMMS),0) + 1 FROM CNT.ASNT x
                           WHERE x.PJRQCDGO = a.PJRQCDGO AND x.PLNTCDGO = 1257
                             AND x.ASNTPRDO = a.ASNTPRDO AND x.ASNTANOO = a.ASNTANOO), 4, '0')
WHERE  a.ASNTCDGO = 12368
AND    a.PLNTCDGO = 1258
AND    a.ASNTNMAL = 'EG-A-2026-10-0011'
AND    a.ASNTESTD = 1;

COMMIT;

-- ---------------------------------------------------------------------------------------------------
-- BLOQUE 3 — CONTROL DESPUÉS. Esperado: 12368 con PLNTCDGO 1257 y el NMAL_NUEVO del BLOQUE 0;
-- y cero duplicados de numeración IN-A en octubre.
-- ---------------------------------------------------------------------------------------------------
SELECT a.ASNTCDGO, a.ASNTNMAL, a.ASNTNMRO, a.ASNTNMMS, a.PLNTCDGO, a.ASNTMDOR, a.ASNTNMMD, a.ASNTESTD
FROM   CNT.ASNT a WHERE a.ASNTCDGO = 12368;

SELECT x.ASNTNMAL, COUNT(*) FROM CNT.ASNT x
WHERE  x.PLNTCDGO = 1257 AND x.ASNTANOO = 2026 AND x.ASNTPRDO = 10
GROUP BY x.ASNTNMAL HAVING COUNT(*) > 1;

-- ---------------------------------------------------------------------------------------------------
-- REVERSO (solo si hace falta volver atrás; descomentar):
-- UPDATE CNT.ASNT a SET (a.PLNTCDGO, a.ASNTNMRO, a.ASNTNMMS, a.ASNTNMAL) =
--   (SELECT b.PLNTCDGO, b.ASNTNMRO, b.ASNTNMMS, b.ASNTNMAL FROM CNT.BK_ASNT_12368_EGA_NOV26 b WHERE b.ASNTCDGO = a.ASNTCDGO)
-- WHERE a.ASNTCDGO = 12368;
-- COMMIT;
-- ---------------------------------------------------------------------------------------------------

-- ---------------------------------------------------------------------------------------------------
-- BLOQUE 4 — LECTURA: las líneas de los otros cuatro EG-A de pólizas (0002, 0003, 0004, 0008), para confirmar
-- que son egresos de verdad (sale dinero de la cuenta 1.1.02 hacia la inversión 1.2.01) y no se tocan.
-- Esperado en cada uno: la 1.1.02.xx en HABER y la 1.2.01.xx en DEBE.
-- ---------------------------------------------------------------------------------------------------
SELECT a.ASNTNMAL, d.DTASCNTA, d.DTASNMCT, d.DTASDBEE, d.DTASHBRR
FROM   CNT.ASNT a JOIN CNT.DTAS d ON d.ASNTCDGO = a.ASNTCDGO
WHERE  a.ASNTCDGO IN (12295, 12296, 12297, 12355)
ORDER BY a.ASNTNMAL, d.DTASCDGO;
