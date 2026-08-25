-- =====================================================================================
-- CARGA INICIAL — MODELO DE BANDAS POR PRODUCTO (CRD.CBPR + CRD.BNDP)
-- Guion para PRODUCCION (y pruebas). Runbook completo:
--   docs/logica-negocio/crd/CARGA-INICIAL-BANDAS-PRODUCTO.md
-- FECHA: 2026-08-25
--
-- SQL PURO: sin comandos SQL*Plus (WHENEVER / SET / DEFINE), apto para el plugin de
--   VS Code / DBeaver / cualquier cliente JDBC, y tambien para SQL*Plus.
-- Valores ya incrustados: EMPRESA = 1236 (ASOPREP), FECHA_DESDE = 2026-09-01,
--   USUARIO = CARGA-INICIAL-BANDAS. Si cambia la fecha de vigencia, editar el
--   TO_DATE('2026-09-01','YYYY-MM-DD') del INSERT de CBPR (unica aparicion).
--
-- REQUIERE: sql/DDL-BANDAS-PRODUCTO.sql ejecutado antes.
-- VALIDADO: en la BD local (copia de la real) el 2026-08-25 → 28 CBPR, 143 BNDP,
--   controles finales en cero. Los IDs coinciden con produccion; aun asi las cuentas
--   se resuelven por CODIGO (PLNNCNTA), no por id.
--
-- MODO DE EJECUCION RECOMENDADO EN EL PLUGIN: correr por bloques en este orden y
--   revisar la salida de cada uno. NO ejecutar el COMMIT final si los controles
--   5.1 / 5.2 devuelven filas — en ese caso ROLLBACK y revisar.
-- =====================================================================================


-- =====================================================================================
-- 2. CONTROLES PREVIOS
-- =====================================================================================

-- 2.1 Nodo de empresa: confirmar que 1236 es ASOPREP (el vigente, no el "ANTERIOR")
SELECT PJRQCDGO, PJRQNMBR FROM SCP.PJRQ WHERE PGSPCDGO = 12 ORDER BY PJRQCDGO;

-- 2.2 Tablas destino vacias (esperado 0 y 0; si no, esta carga ya corrio: ABORTAR)
SELECT (SELECT COUNT(*) FROM CRD.CBPR) CBPR, (SELECT COUNT(*) FROM CRD.BNDP) BNDP FROM dual;

-- 2.3 Cuentas de bandas de la empresa (esperado: 41 subcuentas; faltan las de 1.3.06 y
--     1.3.10 — por eso los productos 21 y 22 no llevan configuracion de por vencer)
SELECT PLNNCNTA, PLNNNMBR FROM CNT.PLNN
WHERE PJRQCDGO = 1236 AND PLNNESTD = 1
  AND REGEXP_LIKE(PLNNCNTA, '^1\.3\.(01|02|03|04|05|06|07|08|09|10|11|12)\.')
ORDER BY PLNNCNTA;


-- =====================================================================================
-- 3. CABECERAS (CRD.CBPR) — esperado: 28 filas insertadas
--    13 configuraciones de POR VENCER + 15 de VENCIDO
-- =====================================================================================

INSERT INTO CRD.CBPR (PRDCCDGO, PJRQCDGO, CBPRTPCR, CBPRFCIN, CBPRUSRG, CBPRESTD)
SELECT m.producto, 1236, m.tpcr, TO_DATE('2026-09-01','YYYY-MM-DD'), 'CARGA-INICIAL-BANDAS', 1
FROM (
    -- ===== POR VENCER (tpcr = 1) =====
    SELECT  2 producto, 1 tpcr FROM dual UNION ALL
    SELECT  4, 1 FROM dual UNION ALL
    SELECT  6, 1 FROM dual UNION ALL
    SELECT 14, 1 FROM dual UNION ALL
    SELECT 15, 1 FROM dual UNION ALL
    SELECT 16, 1 FROM dual UNION ALL
    SELECT 17, 1 FROM dual UNION ALL
    SELECT  3, 1 FROM dual UNION ALL
    SELECT  5, 1 FROM dual UNION ALL
    SELECT  9, 1 FROM dual UNION ALL
    SELECT 10, 1 FROM dual UNION ALL
    SELECT  7, 1 FROM dual UNION ALL
    SELECT  8, 1 FROM dual UNION ALL
    -- ===== VENCIDO (tpcr = 2) — incluye 21 y 22, cuyas familias de vencido SI existen =====
    SELECT  2, 2 FROM dual UNION ALL
    SELECT  3, 2 FROM dual UNION ALL
    SELECT  4, 2 FROM dual UNION ALL
    SELECT  5, 2 FROM dual UNION ALL
    SELECT  6, 2 FROM dual UNION ALL
    SELECT 14, 2 FROM dual UNION ALL
    SELECT 15, 2 FROM dual UNION ALL
    SELECT 16, 2 FROM dual UNION ALL
    SELECT 17, 2 FROM dual UNION ALL
    SELECT  9, 2 FROM dual UNION ALL
    SELECT 10, 2 FROM dual UNION ALL
    SELECT 22, 2 FROM dual UNION ALL
    SELECT  7, 2 FROM dual UNION ALL
    SELECT  8, 2 FROM dual UNION ALL
    SELECT 21, 2 FROM dual
) m;


-- =====================================================================================
-- 4. BANDAS (CRD.BNDP) — esperado: 143 filas insertadas
--    Por vencer 13x5 = 65; vencido quirografario 9x5 = 45; vencido prendario 3x5 = 15;
--    vencido hipotecario 3x6 = 18.
--    Patrones (periodos de 30 dias; NULL = banda abierta/"resto"):
--      PV360 por vencer:            1, 2, 3, 6, NULL
--      V270  quirografario vencido: 1, 2, 3, 3, NULL
--      V360  prendario vencido:     1, 2, 3, 6, NULL
--      VHIP  hipotecario vencido:   1, 2, 6, 3, 12, NULL
-- =====================================================================================

INSERT INTO CRD.BNDP (CBPRCDGO, BNDPNMRO, BNDPCNTD, PLNNCDGO, BNDPUSRG, BNDPESTD)
SELECT c.CBPRCDGO, b.numero, b.periodos, pl.PLNNCDGO, 'CARGA-INICIAL-BANDAS', 1
FROM (
    -- producto, tipo cartera, familia contable, patron de bandas
    SELECT  2 producto, 1 tpcr, '1.3.01' familia, 'PV360' patron FROM dual UNION ALL
    SELECT  4, 1, '1.3.01', 'PV360' FROM dual UNION ALL
    SELECT  6, 1, '1.3.01', 'PV360' FROM dual UNION ALL
    SELECT 14, 1, '1.3.01', 'PV360' FROM dual UNION ALL
    SELECT 15, 1, '1.3.01', 'PV360' FROM dual UNION ALL
    SELECT 16, 1, '1.3.01', 'PV360' FROM dual UNION ALL
    SELECT 17, 1, '1.3.02', 'PV360' FROM dual UNION ALL
    SELECT  3, 1, '1.3.03', 'PV360' FROM dual UNION ALL
    SELECT  5, 1, '1.3.03', 'PV360' FROM dual UNION ALL
    SELECT  9, 1, '1.3.05', 'PV360' FROM dual UNION ALL
    SELECT 10, 1, '1.3.07', 'PV360' FROM dual UNION ALL
    SELECT  7, 1, '1.3.09', 'PV360' FROM dual UNION ALL
    SELECT  8, 1, '1.3.11', 'PV360' FROM dual UNION ALL
    SELECT  2, 2, '1.3.04', 'V270' FROM dual UNION ALL
    SELECT  3, 2, '1.3.04', 'V270' FROM dual UNION ALL
    SELECT  4, 2, '1.3.04', 'V270' FROM dual UNION ALL
    SELECT  5, 2, '1.3.04', 'V270' FROM dual UNION ALL
    SELECT  6, 2, '1.3.04', 'V270' FROM dual UNION ALL
    SELECT 14, 2, '1.3.04', 'V270' FROM dual UNION ALL
    SELECT 15, 2, '1.3.04', 'V270' FROM dual UNION ALL
    SELECT 16, 2, '1.3.04', 'V270' FROM dual UNION ALL
    SELECT 17, 2, '1.3.04', 'V270' FROM dual UNION ALL
    SELECT  9, 2, '1.3.08', 'V360' FROM dual UNION ALL
    SELECT 10, 2, '1.3.08', 'V360' FROM dual UNION ALL
    SELECT 22, 2, '1.3.08', 'V360' FROM dual UNION ALL
    SELECT  7, 2, '1.3.12', 'VHIP' FROM dual UNION ALL
    SELECT  8, 2, '1.3.12', 'VHIP' FROM dual UNION ALL
    SELECT 21, 2, '1.3.12', 'VHIP' FROM dual
) f
JOIN (
    -- patron, sufijo de cuenta, numero de banda, periodos de 30 dias (NULL = abierta)
    SELECT 'PV360' patron, '05' sufijo, 1 numero, 1 periodos FROM dual UNION ALL
    SELECT 'PV360', '10', 2, 2    FROM dual UNION ALL
    SELECT 'PV360', '15', 3, 3    FROM dual UNION ALL
    SELECT 'PV360', '20', 4, 6    FROM dual UNION ALL
    SELECT 'PV360', '25', 5, NULL FROM dual UNION ALL
    SELECT 'V270',  '05', 1, 1    FROM dual UNION ALL
    SELECT 'V270',  '10', 2, 2    FROM dual UNION ALL
    SELECT 'V270',  '15', 3, 3    FROM dual UNION ALL
    SELECT 'V270',  '20', 4, 3    FROM dual UNION ALL
    SELECT 'V270',  '25', 5, NULL FROM dual UNION ALL
    SELECT 'V360',  '05', 1, 1    FROM dual UNION ALL
    SELECT 'V360',  '10', 2, 2    FROM dual UNION ALL
    SELECT 'V360',  '15', 3, 3    FROM dual UNION ALL
    SELECT 'V360',  '20', 4, 6    FROM dual UNION ALL
    SELECT 'V360',  '25', 5, NULL FROM dual UNION ALL
    SELECT 'VHIP',  '00', 1, 1    FROM dual UNION ALL
    SELECT 'VHIP',  '05', 2, 2    FROM dual UNION ALL
    SELECT 'VHIP',  '10', 3, 6    FROM dual UNION ALL
    SELECT 'VHIP',  '15', 4, 3    FROM dual UNION ALL
    SELECT 'VHIP',  '20', 5, 12   FROM dual UNION ALL
    SELECT 'VHIP',  '25', 6, NULL FROM dual
) b ON b.patron = f.patron
JOIN CRD.CBPR c
  ON c.PRDCCDGO = f.producto AND c.CBPRTPCR = f.tpcr
 AND c.PJRQCDGO = 1236 AND c.CBPRFCFN IS NULL AND c.CBPRESTD = 1
JOIN CNT.PLNN pl
  ON pl.PLNNCNTA = f.familia || '.' || b.sufijo
 AND pl.PJRQCDGO = 1236 AND pl.PLNNESTD = 1;


-- =====================================================================================
-- 5. CONTROLES POSTERIORES — 5.1 y 5.2 deben devolver CERO filas.
--    Si alguno devuelve filas: ROLLBACK; y revisar (NO ejecutar el COMMIT).
-- =====================================================================================

-- 5.1 Bandas consecutivas y exactamente una abierta, al final (esperado: 0 filas)
SELECT c.CBPRCDGO, c.PRDCCDGO, c.CBPRTPCR,
       COUNT(*) bandas,
       SUM(CASE WHEN b.BNDPCNTD IS NULL THEN 1 ELSE 0 END) abiertas
FROM CRD.CBPR c JOIN CRD.BNDP b ON b.CBPRCDGO = c.CBPRCDGO
GROUP BY c.CBPRCDGO, c.PRDCCDGO, c.CBPRTPCR
HAVING COUNT(*) <> MAX(b.BNDPNMRO)
    OR SUM(CASE WHEN b.BNDPCNTD IS NULL THEN 1 ELSE 0 END) <> 1
    OR MAX(CASE WHEN b.BNDPCNTD IS NULL THEN b.BNDPNMRO END) <> MAX(b.BNDPNMRO);

-- 5.2 Configuraciones incompletas o vacias (esperado: 0 filas)
SELECT c.CBPRCDGO, c.PRDCCDGO, c.CBPRTPCR, COUNT(b.BNDPCDGO) bandas
FROM CRD.CBPR c LEFT JOIN CRD.BNDP b ON b.CBPRCDGO = c.CBPRCDGO
GROUP BY c.CBPRCDGO, c.PRDCCDGO, c.CBPRTPCR
HAVING COUNT(b.BNDPCDGO) NOT IN (5, 6)
ORDER BY c.PRDCCDGO;

-- 5.3 Conteos (esperado: 28 y 143)
SELECT (SELECT COUNT(*) FROM CRD.CBPR) CBPR, (SELECT COUNT(*) FROM CRD.BNDP) BNDP FROM dual;

-- 5.4 Vista completa para revision de contabilidad (143 filas: producto, cartera,
--     banda, periodos, rango derivado en dias y cuenta)
SELECT p.PRDCNMBR producto,
       DECODE(c.CBPRTPCR, 1, 'POR VENCER', 2, 'VENCIDO') cartera,
       b.BNDPNMRO banda, NVL(TO_CHAR(b.BNDPCNTD), 'RESTO') periodos,
       30 * NVL(SUM(b.BNDPCNTD) OVER (PARTITION BY c.CBPRCDGO ORDER BY b.BNDPNMRO
            ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING), 0) + 1 dia_ini,
       30 * SUM(b.BNDPCNTD) OVER (PARTITION BY c.CBPRCDGO ORDER BY b.BNDPNMRO) dia_fin,
       pl.PLNNCNTA cuenta, pl.PLNNNMBR nombre_cuenta
FROM CRD.CBPR c
JOIN CRD.PRDC p ON p.PRDCCDGO = c.PRDCCDGO
JOIN CRD.BNDP b ON b.CBPRCDGO = c.CBPRCDGO
JOIN CNT.PLNN pl ON pl.PLNNCDGO = b.PLNNCDGO
ORDER BY p.PRDCCDGO, c.CBPRTPCR, b.BNDPNMRO;


-- =====================================================================================
-- 6. COMMIT — ejecutar SOLO si 5.1 y 5.2 dieron 0 filas y 5.3 dio 28 / 143.
--    Si no: ROLLBACK;
-- =====================================================================================

COMMIT;


-- NOTA PENDIENTE (no corre en esta carga): los productos 21 HIPOTECARIO NOVACION y
-- 22 PRENDARIO NOVACION quedan SIN configuracion de POR VENCER porque 1.3.10 y 1.3.06
-- no tienen subcuentas de bandas en CNT.PLNN. Cuando contabilidad las cree, ejecutar el
-- bloque del paso 4 de CARGA-INICIAL-BANDAS-PRODUCTO.md.
