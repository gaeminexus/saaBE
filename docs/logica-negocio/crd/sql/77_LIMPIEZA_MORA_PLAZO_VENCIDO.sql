-- ============================================================================
-- 77_LIMPIEZA_MORA_PLAZO_VENCIDO.sql
-- Frente C — fix del proceso de mora (docs/logica-negocio/crd/PROCESO-DIARIO-INTERES-MORA.md §11,
-- docs/logica-negocio/crd/ESTADO-TRABAJO-EN-CURSO.md §3)
-- Fecha: 2026-08-27
--
-- SQL PURO (sin SET/DEFINE/WHENEVER) para correr en el plugin JDBC de VS Code.
-- NO SE EJECUTA POR EL AGENTE. El usuario lo corre, DESPUÉS de restituir PRSTIDST=8 desde
-- su respaldo (ver ESTADO-TRABAJO-EN-CURSO.md §4, tarea 2) y de confirmar que el fix del
-- código ya está desplegado (tarea 3 de esa misma sección).
--
-- UNIVERSO — SE DEDUCE DEL DATO, NO DE UNA LISTA EXTERNA: las cuotas de préstamos que HOY
-- están en PRSTIDST = 8 (DE_PLAZO_VENCIDO) y tienen DTPRMRAA > 0. Un préstamo legítimamente
-- en 8 nunca debió tener mora calculada (el proceso diario sale antes de tocarlo — ver
-- PROCESO-DIARIO-INTERES-MORA.md §4); si la tiene, es porque el defecto de la §11 lo
-- reclasificó a EN_MORA(11), le calculó mora, y el usuario ya lo restituyó a 8 sin que eso
-- limpiara las cuotas.
--
-- QUÉ SE LIMPIA Y QUÉ NO:
--   - Si la mora de la cuota NO se cobró nunca (SUM(PGPRMRPG) de pagos vigentes = 0): se
--     limpia con UPDATE (Bloque 3).
--   - Si se cobró aunque sea un centavo: NO se toca. Esa plata es real y ya salió de algún
--     lado; decidir qué hacer con ella (devolverla, cruzarla) es del usuario, no de un
--     UPDATE. Bloque 5 la lista y cuantifica, aparte.
--
-- MEDIDO contra la base local el 2026-08-27 (universo real, no hipotético):
--   106 préstamos en estado 8, TODOS con mora escrita — 4.600 cuotas, $598.465,14 de mora
--   escrita, $598.416,86 pendiente. De esas, 16 cuotas tienen $48,28 de mora YA COBRADA
--   (Bloque 5) y quedan FUERA de la limpieza. Las 4.584 restantes se limpian en el Bloque 3.
--   4.580 cuotas estaban en DTPRESTD=5; tras re-derivar por pagos reales (Bloque 4), quedan
--   4.573 en PENDIENTE(1), y el resto se reparte entre PAGADA(4)/PARCIAL(6) según lo que de
--   verdad se pagó. Probado completo en una transacción con ROLLBACK antes de entregar este
--   documento — los números de arriba son el resultado real de esa prueba.
-- ============================================================================


-- ============================================================================
-- BLOQUE 1 — MEDICIÓN (solo lectura)
-- ============================================================================

-- 1.1 Universo: mora escrita por préstamo, en préstamos hoy en estado 8
SELECT d.PRSTCDGO, COUNT(*) AS CUOTAS_CON_MORA,
       SUM(d.DTPRMRAA) AS MORA_ESCRITA, SUM(NVL(d.DTPRSLMR,0)) AS MORA_PENDIENTE
FROM   CRD.DTPR d
JOIN   CRD.PRST p ON p.PRSTCDGO = d.PRSTCDGO
WHERE  p.PRSTIDST = 8
AND    NVL(d.DTPRMRAA,0) > 0
GROUP  BY d.PRSTCDGO
ORDER  BY MORA_ESCRITA DESC;

-- 1.2 Totales del universo completo
SELECT COUNT(DISTINCT d.PRSTCDGO) AS PRESTAMOS_AFECTADOS,
       COUNT(*) AS CUOTAS_AFECTADAS,
       SUM(d.DTPRMRAA) AS TOTAL_MORA_ESCRITA,
       SUM(NVL(d.DTPRSLMR,0)) AS TOTAL_MORA_PENDIENTE
FROM   CRD.DTPR d
JOIN   CRD.PRST p ON p.PRSTCDGO = d.PRSTCDGO
WHERE  p.PRSTIDST = 8
AND    NVL(d.DTPRMRAA,0) > 0;

-- 1.3 De esa mora, cuánta YA SE COBRÓ (pagos vigentes) — esta parte NO se limpia con UPDATE
SELECT d.PRSTCDGO, d.DTPRCDGO, d.DTPRNMCT, d.DTPRMRAA AS MORA_ESCRITA,
       g.mora_pagada AS MORA_YA_COBRADA
FROM   CRD.DTPR d
JOIN   CRD.PRST p ON p.PRSTCDGO = d.PRSTCDGO
JOIN   (SELECT DTPRCDGO, SUM(NVL(PGPRMRPG,0)) mora_pagada
        FROM   CRD.PGPR
        WHERE  PGPRANUL IS NULL OR PGPRANUL = 0
        GROUP  BY DTPRCDGO) g ON g.DTPRCDGO = d.DTPRCDGO
WHERE  p.PRSTIDST = 8
AND    NVL(d.DTPRMRAA,0) > 0
AND    g.mora_pagada > 0.01
ORDER  BY MORA_YA_COBRADA DESC;

-- 1.4 Total de mora ya cobrada (el número que hace falta para decidir qué hacer)
SELECT COUNT(*) AS CUOTAS_CON_MORA_COBRADA, SUM(g.mora_pagada) AS TOTAL_MORA_YA_COBRADA
FROM   CRD.DTPR d
JOIN   CRD.PRST p ON p.PRSTCDGO = d.PRSTCDGO
JOIN   (SELECT DTPRCDGO, SUM(NVL(PGPRMRPG,0)) mora_pagada
        FROM   CRD.PGPR
        WHERE  PGPRANUL IS NULL OR PGPRANUL = 0
        GROUP  BY DTPRCDGO) g ON g.DTPRCDGO = d.DTPRCDGO
WHERE  p.PRSTIDST = 8
AND    NVL(d.DTPRMRAA,0) > 0
AND    g.mora_pagada > 0.01;

-- 1.5 Cuotas que quedaron en DTPRESTD = 5 EN_MORA dentro del universo
SELECT d.PRSTCDGO, COUNT(*) AS CUOTAS_EN_ESTADO_5
FROM   CRD.DTPR d
JOIN   CRD.PRST p ON p.PRSTCDGO = d.PRSTCDGO
WHERE  p.PRSTIDST = 8
AND    NVL(d.DTPRMRAA,0) > 0
AND    d.DTPRESTD = 5
GROUP  BY d.PRSTCDGO
ORDER  BY CUOTAS_EN_ESTADO_5 DESC;


-- ============================================================================
-- BLOQUE 2 — RESPALDO
-- ============================================================================

CREATE TABLE CRD.BKP_DTPR_MORA_PV_20260827 AS
SELECT d.*
FROM   CRD.DTPR d
JOIN   CRD.PRST p ON p.PRSTCDGO = d.PRSTCDGO
WHERE  p.PRSTIDST = 8
AND    NVL(d.DTPRMRAA,0) > 0;

-- Tabla auxiliar con los pagos VIGENTES agregados por cuota (se usa en los bloques 3 y 4).
-- PGPRANUL IS NULL cubre los pagos históricos anteriores al ALTER de CRD.PGPR, mismo
-- criterio que PagoPrestamoDaoServiceImpl.selectVigentesByIdDetallePrestamo.
CREATE TABLE CRD.TMP_VIGENTE_MORA_PV_20260827 AS
SELECT DTPRCDGO,
       SUM(NVL(PGPRCPPG,0)) capital_pagado, SUM(NVL(PGPRINPG,0)) interes_pagado,
       SUM(NVL(PGPRDSGR,0)) desgravamen_pagado, SUM(NVL(PGPRVLSI,0)) seguro_pagado,
       SUM(NVL(PGPRMRPG,0)) mora_pagada
FROM   CRD.PGPR
WHERE  PGPRANUL IS NULL OR PGPRANUL = 0
GROUP  BY DTPRCDGO;


-- ============================================================================
-- BLOQUE 3 — LIMPIEZA (solo cuotas SIN mora ya cobrada)
-- ============================================================================

-- 3.1 DTPRMRAA/DTPRMRCL/DTPRDSMR/DTPRSLMR a cero, y DTPRTTLL recompuesto con la fórmula de
-- idempotencia de PROCESO-DIARIO-INTERES-MORA.md §5: totalBase = TTLL_actual − MRAA_actual.
-- Como la mora nueva es 0, el nuevo total ES la base. NO se recalcula desde cero: se resta
-- exactamente lo que el proceso sumó, preservando la base original de las cuotas cargadas
-- desde Excel (que puede no coincidir con la suma de sus componentes — mismo motivo por el
-- que el proceso diario nunca recalcula desde cero).
-- DTPRTTCS se actualiza igual: es espejo de DTPRTTLL (tabla de la §4 de ese mismo documento).
UPDATE CRD.DTPR d
SET (DTPRMRAA, DTPRMRCL, DTPRDSMR, DTPRSLMR, DTPRTTLL, DTPRTTCS) = (
    SELECT 0, 0, 0, 0,
           NVL(d.DTPRTTLL,0) - NVL(d.DTPRMRAA,0),
           NVL(d.DTPRTTLL,0) - NVL(d.DTPRMRAA,0)
    FROM DUAL
)
WHERE d.PRSTCDGO IN (SELECT PRSTCDGO FROM CRD.PRST WHERE PRSTIDST = 8)
AND   NVL(d.DTPRMRAA,0) > 0
AND   NOT EXISTS (
    SELECT 1 FROM CRD.TMP_VIGENTE_MORA_PV_20260827 v
    WHERE v.DTPRCDGO = d.DTPRCDGO AND v.mora_pagada > 0.01
);

COMMIT;


-- ============================================================================
-- BLOQUE 4 — RE-DERIVAR EL ESTADO DE LAS CUOTAS QUE QUEDARON EN 5 (dentro de lo ya limpiado)
-- ============================================================================

-- 4.1 Con pagos vigentes registrados: PAGADA si lo pagado cubre el nuevo total (ya sin
-- mora, con tolerancia $0.01), PARCIAL si pagó algo pero no todo. DTPRESTD y DTPRIDST
-- siempre juntos (nunca deben quedar desfasados — CLAUDE.md).
UPDATE CRD.DTPR d
SET (DTPRESTD, DTPRIDST) = (
    SELECT CASE
             WHEN ABS( (NVL(v.capital_pagado,0)+NVL(v.interes_pagado,0)+NVL(v.desgravamen_pagado,0)+NVL(v.seguro_pagado,0))
                       - NVL(d.DTPRTTLL,0) ) <= 0.01 THEN 4   -- PAGADA
             WHEN (NVL(v.capital_pagado,0)+NVL(v.interes_pagado,0)+NVL(v.desgravamen_pagado,0)+NVL(v.seguro_pagado,0)) > 0.01 THEN 6  -- PARCIAL
             ELSE 1                                                                                                                     -- PENDIENTE
           END,
           CASE
             WHEN ABS( (NVL(v.capital_pagado,0)+NVL(v.interes_pagado,0)+NVL(v.desgravamen_pagado,0)+NVL(v.seguro_pagado,0))
                       - NVL(d.DTPRTTLL,0) ) <= 0.01 THEN 4
             WHEN (NVL(v.capital_pagado,0)+NVL(v.interes_pagado,0)+NVL(v.desgravamen_pagado,0)+NVL(v.seguro_pagado,0)) > 0.01 THEN 6
             ELSE 1
           END
    FROM CRD.TMP_VIGENTE_MORA_PV_20260827 v
    WHERE v.DTPRCDGO = d.DTPRCDGO
)
WHERE d.DTPRESTD = 5
AND   d.PRSTCDGO IN (SELECT PRSTCDGO FROM CRD.PRST WHERE PRSTIDST = 8)
AND   NVL(d.DTPRMRAA,0) = 0
AND   EXISTS (SELECT 1 FROM CRD.TMP_VIGENTE_MORA_PV_20260827 v WHERE v.DTPRCDGO = d.DTPRCDGO);

-- 4.2 Sin ninguna fila en PGPR (nunca se le aplicó pago): PENDIENTE directo.
UPDATE CRD.DTPR d
SET (DTPRESTD, DTPRIDST) = (SELECT 1, 1 FROM DUAL)
WHERE d.DTPRESTD = 5
AND   d.PRSTCDGO IN (SELECT PRSTCDGO FROM CRD.PRST WHERE PRSTIDST = 8)
AND   NVL(d.DTPRMRAA,0) = 0
AND   NOT EXISTS (SELECT 1 FROM CRD.TMP_VIGENTE_MORA_PV_20260827 v WHERE v.DTPRCDGO = d.DTPRCDGO);

COMMIT;


-- ============================================================================
-- BLOQUE 5 — MORA YA COBRADA — SEPARADA, CUANTIFICADA, SIN TOCAR
-- ============================================================================

-- Repite el 1.3/1.4 DESPUÉS de la limpieza, para que quede como registro de lo que sigue
-- pendiente de decisión. Estas cuotas NO fueron tocadas por el Bloque 3 ni por el 4: siguen
-- exactamente como estaban antes de correr este script.
SELECT d.PRSTCDGO, d.DTPRCDGO, d.DTPRNMCT, d.DTPRESTD, d.DTPRMRAA AS MORA_ESCRITA,
       g.mora_pagada AS MORA_YA_COBRADA
FROM   CRD.DTPR d
JOIN   CRD.PRST p ON p.PRSTCDGO = d.PRSTCDGO
JOIN   (SELECT DTPRCDGO, SUM(NVL(PGPRMRPG,0)) mora_pagada
        FROM   CRD.PGPR
        WHERE  PGPRANUL IS NULL OR PGPRANUL = 0
        GROUP  BY DTPRCDGO) g ON g.DTPRCDGO = d.DTPRCDGO
WHERE  p.PRSTIDST = 8
AND    NVL(d.DTPRMRAA,0) > 0
AND    g.mora_pagada > 0.01
ORDER  BY MORA_YA_COBRADA DESC;


-- ============================================================================
-- BLOQUE 6 — CONTROL DESPUÉS
-- ============================================================================

-- 6.1 Ninguna cuota limpiada debe seguir en estado 5 (deben ser solo las de mora cobrada)
SELECT COUNT(*) AS DEBE_SER_CERO
FROM   CRD.DTPR d
WHERE  d.PRSTCDGO IN (SELECT PRSTCDGO FROM CRD.PRST WHERE PRSTIDST = 8)
AND    d.DTPRESTD = 5
AND    NVL(d.DTPRMRAA,0) = 0;

-- 6.2 Espejo de estados intacto dentro de todo el universo tocado (debe ser 0, igual que el
-- control de PROCESO-DIARIO-INTERES-MORA.md §9)
SELECT COUNT(*) AS DEBE_SER_CERO
FROM   CRD.DTPR d
WHERE  d.DTPRCDGO IN (SELECT DTPRCDGO FROM CRD.BKP_DTPR_MORA_PV_20260827)
AND    NVL(d.DTPRIDST,-1) <> NVL(d.DTPRESTD,-1);

-- 6.3 Distribución final de estados dentro del universo tocado
SELECT DTPRESTD, COUNT(*) FROM CRD.DTPR d
WHERE  d.DTPRCDGO IN (SELECT DTPRCDGO FROM CRD.BKP_DTPR_MORA_PV_20260827)
GROUP  BY DTPRESTD ORDER BY 1;

-- 6.4 Ninguna cuota limpiada debe conservar mora (debe ser 0 filas)
SELECT COUNT(*) AS DEBE_SER_CERO
FROM   CRD.DTPR d
WHERE  d.DTPRCDGO IN (SELECT DTPRCDGO FROM CRD.BKP_DTPR_MORA_PV_20260827)
AND    NOT EXISTS (
    SELECT 1 FROM CRD.PGPR g
    WHERE g.DTPRCDGO = d.DTPRCDGO AND (g.PGPRANUL IS NULL OR g.PGPRANUL = 0) AND NVL(g.PGPRMRPG,0) > 0.01
)
AND    (NVL(d.DTPRMRAA,0) <> 0 OR NVL(d.DTPRMRCL,0) <> 0 OR NVL(d.DTPRDSMR,0) <> 0 OR NVL(d.DTPRSLMR,0) <> 0);

-- 6.5 Idempotencia: correr el Bloque 3 otra vez sobre lo ya limpiado no debe cambiar nada
-- (DTPRMRAA ya es 0, así que "TTLL - MRAA" da el mismo TTLL). Foto de control:
SELECT SUM(DTPRTTLL), SUM(DTPRMRAA) FROM CRD.DTPR
WHERE DTPRCDGO IN (SELECT DTPRCDGO FROM CRD.BKP_DTPR_MORA_PV_20260827);


-- ============================================================================
-- BLOQUE 7 — REVERSO
-- ============================================================================

-- ⛔⛔ TODO ESTE BLOQUE VA COMENTADO A PROPÓSITO. NO LO DESCOMENTES "por si acaso".
--     Corre SOLO si hay que deshacer la limpieza, y descomentando línea por línea.
--     Contiene un DELETE FROM CRD.DTPR con subquery: si este script se ejecuta de corrido
--     con esto activo, sobreescribe de nuevo lo que el Bloque 3/4 acaban de corregir.
--
-- 7.1 Restaurar las columnas tocadas desde el respaldo (Bloque 2), fila por fila.
-- MERGE INTO CRD.DTPR d
-- USING CRD.BKP_DTPR_MORA_PV_20260827 b
-- ON (d.DTPRCDGO = b.DTPRCDGO)
-- WHEN MATCHED THEN UPDATE SET
--     d.DTPRMRAA = b.DTPRMRAA, d.DTPRMRCL = b.DTPRMRCL, d.DTPRDSMR = b.DTPRDSMR,
--     d.DTPRSLMR = b.DTPRSLMR, d.DTPRTTLL = b.DTPRTTLL, d.DTPRTTCS = b.DTPRTTCS,
--     d.DTPRESTD = b.DTPRESTD, d.DTPRIDST = b.DTPRIDST;
-- COMMIT;
--
-- 7.2 Limpieza de las tablas de trabajo (correr solo cuando el usuario confirme que ya no
--     las necesita; conservarlas un tiempo sirve de evidencia de la corrida).
-- DROP TABLE CRD.BKP_DTPR_MORA_PV_20260827 PURGE;
-- DROP TABLE CRD.TMP_VIGENTE_MORA_PV_20260827 PURGE;
