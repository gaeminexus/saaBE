-- =====================================================================================
-- e3-20 — Pólizas de seguro: la clave de acceso única deja fuera los documentos ANULADOS
-- Equipo omen-saa-3 · 2026-11-11 · Diseño: crd/DISENO-SEGURO-CLAVE-ACCESO-TRAS-ANULAR.md (K1)
-- Se puede correr con el EAR actual (no rompe nada). Hace falta además el EAR con K2.
-- =====================================================================================

-- PASO 0 — Control previo: ninguna clave repetida entre documentos VIVOS (esperado: ninguna fila).
SELECT UPPER(TRIM(POSGCLAC)) AS CLAVE, COUNT(*) AS VECES
  FROM CRD.POSG
 WHERE POSGESTD <> 5 AND POSGCLAC IS NOT NULL
 GROUP BY UPPER(TRIM(POSGCLAC))
HAVING COUNT(*) > 1;

-- PASO 1 — Cambiar el índice.
DROP INDEX CRD.UX_POSG_CLAVE_ACCESO;
CREATE UNIQUE INDEX CRD.UX_POSG_CLAVE_ACCESO
    ON CRD.POSG (CASE WHEN POSGESTD <> 5 THEN UPPER(TRIM(POSGCLAC)) END);

-- PASO 2 — Control final: el índice existe y es único (esperado: 1 fila, UNIQUENESS = UNIQUE, STATUS = VALID).
SELECT INDEX_NAME, UNIQUENESS, STATUS
  FROM ALL_INDEXES
 WHERE OWNER = 'CRD' AND INDEX_NAME = 'UX_POSG_CLAVE_ACCESO';
