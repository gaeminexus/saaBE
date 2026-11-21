-- C37-01 · D-126 (19-nov-2026): rubro 251 «centros de costo con crédito tributario de IVA» (sustento 01 del ATS).
-- SustentoTributarioServiceImpl lee el rubro por su ALTERNO (PRBRALTR = 251) y toma los CNCSCDGO de los detalles
-- vigentes (PDTRVLRN, PDTRESTD = 1). Con IVA:
--   centro en el rubro (02 ASOPREP ARRIENDOS)          → '01' con derecho a crédito tributario
--   cualquier otro centro (01 ASOPREP ADMINISTRACION)  → '02' sin crédito
--   sin centro en el asiento                            → '01' (regla del IVA de antes) con aviso
-- Sin IVA → '02'. SIN este rubro el código aplica la regla del IVA de antes (01 con IVA) y lo avisa.
--
-- El centro se busca por NÚMERO (CNCSNMRO '1.02') y empresa (1236), no por CNCSCDGO fijo: en la base de trabajo es
-- 1705, pero en otra base puede ser otro. PRBRCDGO = 251 si está libre; si no, MAX + 1 (en producción el 249 quedó
-- como PRBR 312). PDTRCDGO: no hay secuencia; MAX + 1, como c20-02 y C19-04.
-- Los NOT EXISTS impiden duplicar: se puede correr dos veces.
-- SQL puro (regla 8): correr los bloques 01 a 05 en orden y dar COMMIT a mano si el 05 está bien.
-- En la base de trabajo: node 03-trabajo/meses/2025-07/scripts/correr-sql.js <ruta a este archivo> --out <dir> [--commit]

-- 01 Antes: el alterno 251 no existe. ESPERADO: 0 filas (si sale 1, ya se corrió: el resto no inserta nada)
SELECT r.PRBRCDGO, r.PRBRALTR, r.PRBRDSCR FROM SCP.PRBR r WHERE r.PRBRALTR = 251 OR r.PRBRCDGO = 251;

-- 02 Antes: centros de costo de la empresa 1236. ESPERADO: 1.01 ADMINISTRACION y 1.02 ARRIENDOS activos
SELECT c.CNCSCDGO, c.CNCSNMRO, c.CNCSNMBR, c.CNCSESTD FROM CNT.CNCS c WHERE c.PJRQCDGO = 1236 ORDER BY c.CNCSNMRO;

-- 03 Cambio: rubro
INSERT INTO SCP.PRBR (PRBRCDGO, PRBRDSCR, PRBRFCHA, PRBRALTR, PRBRTPOO)
SELECT CASE WHEN NOT EXISTS (SELECT 1 FROM SCP.PRBR x WHERE x.PRBRCDGO = 251) THEN 251
            ELSE (SELECT MAX(x.PRBRCDGO) + 1 FROM SCP.PRBR x) END,
       'CXP CENTROS DE COSTO CON CREDITO TRIBUTARIO DE IVA (D-126, sustento 01 del ATS)', SYSDATE, 251, 1
  FROM DUAL
 WHERE NOT EXISTS (SELECT 1 FROM SCP.PRBR r WHERE r.PRBRALTR = 251);

-- 04 Cambio: detalle 1 = centro 1.02 ASOPREP ARRIENDOS de la empresa 1236
INSERT INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD)
SELECT (SELECT NVL(MAX(PDTRCDGO), 0) + 1 FROM SCP.PDTR), r.PRBRCDGO, c.CNCSNMBR, c.CNCSCDGO, c.CNCSNMRO, 1, 1
  FROM SCP.PRBR r, CNT.CNCS c
 WHERE r.PRBRALTR = 251
   AND c.PJRQCDGO = 1236 AND c.CNCSNMRO = '1.02'
   AND NOT EXISTS (SELECT 1 FROM SCP.PDTR d WHERE d.PRBRCDGO = r.PRBRCDGO AND d.PDTRVLRN = c.CNCSCDGO);

-- 05 Después. ESPERADO: 1 fila, alterno 251, detalle 1, centro de ARRIENDOS (CNCSNMRO 1.02), estado 1
SELECT r.PRBRCDGO, r.PRBRALTR, d.PDTRCDGO, d.PDTRALTR, d.PDTRVLRN AS cncscdgo, d.PDTRVLRV AS numero, d.PDTRDSCR, d.PDTRESTD
  FROM SCP.PRBR r JOIN SCP.PDTR d ON d.PRBRCDGO = r.PRBRCDGO
 WHERE r.PRBRALTR = 251;

-- COMMIT;   <- a mano, si el bloque 05 está bien

-- REVERSO (comentado; sin el rubro vuelve la regla del IVA de antes, con aviso):
-- DELETE FROM SCP.PDTR WHERE PRBRCDGO = (SELECT PRBRCDGO FROM SCP.PRBR WHERE PRBRALTR = 251);
-- DELETE FROM SCP.PRBR WHERE PRBRALTR = 251;
-- COMMIT;
