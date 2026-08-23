-- =============================================================================
-- Script: Actualización de numeración alterna de asientos (ASNTNMAL, ASNTNMMS)
-- Empresa: 1236
-- Tablas:  CNT.ASNT (asientos activos)
--          CNT.ASNH (historico de asientos)
-- Fecha generación: 2026-05-22
--
-- Descripción:
--   Calcula y asigna la numeración alterna legible para todos los asientos
--   existentes de la empresa 1236 que aún no tienen valor en ASNTNMAL.
--   Formato: XXX-AAAA-MM-NNNN
--     XXX  = 3 primeras letras del nombre del tipo de asiento (PLNTNMBR)
--     AAAA = año del asiento (ASNTANOO)
--     MM   = mes del asiento con cero a la izquierda (ASNTPRDO)
--     NNNN = consecutivo mensual por tipo, reseteado cada mes (ASNTNMMS)
--
-- El consecutivo (NNNN) se asigna ordenando por ASNTNMRO (numero de asiento)
-- y por ASNTCDGO como desempate, garantizando consistencia con el orden original.
-- =============================================================================

-- =========================================================
-- PARTE 1: Tabla CNT.ASNT (asientos vigentes)
-- =========================================================

MERGE INTO CNT.ASNT tgt
USING (
    SELECT
        a.ASNTCDGO,
        ROW_NUMBER() OVER (
            PARTITION BY a.PLNTCDGO, a.PJRQCDGO, a.ASNTANOO, a.ASNTPRDO
            ORDER BY a.ASNTNMRO, a.ASNTCDGO
        ) AS nuevo_nmms,
        UPPER(SUBSTR(p.PLNTNMBR, 1, 3))
            || '-' || TO_CHAR(a.ASNTANOO)
            || '-' || LPAD(TO_CHAR(a.ASNTPRDO), 2, '0')
            || '-' || LPAD(
                    TO_CHAR(
                        ROW_NUMBER() OVER (
                            PARTITION BY a.PLNTCDGO, a.PJRQCDGO, a.ASNTANOO, a.ASNTPRDO
                            ORDER BY a.ASNTNMRO, a.ASNTCDGO
                        )
                    ), 4, '0'
                ) AS nuevo_nmal
    FROM CNT.ASNT a
    JOIN CNT.PLNT p ON p.PLNTCDGO = a.PLNTCDGO
    WHERE a.PJRQCDGO = 1236
) src
ON (tgt.ASNTCDGO = src.ASNTCDGO)
WHEN MATCHED THEN UPDATE SET
    tgt.ASNTNMMS = src.nuevo_nmms,
    tgt.ASNTNMAL = src.nuevo_nmal;

-- =========================================================
-- PARTE 2: Tabla CNT.ASNH (histórico de asientos)
-- =========================================================

MERGE INTO CNT.ASNH tgt
USING (
    SELECT
        h.ASNHCDGO,
        ROW_NUMBER() OVER (
            PARTITION BY h.PLNTCDGO, h.PJRQCDGO, h.ASNHANOO, h.ASNHPRDO
            ORDER BY h.ASNHNMRO, h.ASNHCDGO
        ) AS nuevo_nmms,
        UPPER(SUBSTR(p.PLNTNMBR, 1, 3))
            || '-' || TO_CHAR(h.ASNHANOO)
            || '-' || LPAD(TO_CHAR(h.ASNHPRDO), 2, '0')
            || '-' || LPAD(
                    TO_CHAR(
                        ROW_NUMBER() OVER (
                            PARTITION BY h.PLNTCDGO, h.PJRQCDGO, h.ASNHANOO, h.ASNHPRDO
                            ORDER BY h.ASNHNMRO, h.ASNHCDGO
                        )
                    ), 4, '0'
                ) AS nuevo_nmal
    FROM CNT.ASNH h
    JOIN CNT.PLNT p ON p.PLNTCDGO = h.PLNTCDGO
    WHERE h.PJRQCDGO = 1236
) src
ON (tgt.ASNHCDGO = src.ASNHCDGO)
WHEN MATCHED THEN UPDATE SET
    tgt.ASNHNMMS = src.nuevo_nmms,
    tgt.ASNHNMAL = src.nuevo_nmal;

-- =========================================================
-- Verificación previa al COMMIT
-- =========================================================

-- Verifica una muestra del resultado en ASNT
SELECT
    a.ASNTCDGO,
    a.ASNTANOO   AS anio,
    a.ASNTPRDO   AS mes,
    p.PLNTNMBR   AS tipo,
    a.ASNTNMMS   AS numero_mes_tipo,
    a.ASNTNMAL   AS numero_alterno
FROM CNT.ASNT a
JOIN CNT.PLNT p ON p.PLNTCDGO = a.PLNTCDGO
WHERE a.PJRQCDGO = 1236
ORDER BY p.PLNTNMBR, a.ASNTANOO, a.ASNTPRDO, a.ASNTNMMS
FETCH FIRST 50 ROWS ONLY;

-- Verifica que no queden registros sin numero alterno en ASNT
SELECT COUNT(*) AS sin_numero_alterno_asnt
FROM CNT.ASNT
WHERE PJRQCDGO = 1236
  AND ASNTNMAL IS NULL;

-- Verifica que no queden registros sin numero alterno en ASNH
SELECT COUNT(*) AS sin_numero_alterno_asnh
FROM CNT.ASNH
WHERE PJRQCDGO = 1236
  AND ASNHNMAL IS NULL;

-- =========================================================
-- Si la verificación es correcta, ejecutar:
-- =========================================================
COMMIT;

-- En caso de encontrar inconsistencias, ejecutar:
-- ROLLBACK;
