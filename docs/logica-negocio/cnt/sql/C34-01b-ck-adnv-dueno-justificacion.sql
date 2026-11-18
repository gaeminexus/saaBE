-- =====================================================================================
-- C34-01b — Adjuntos de una justificación (CNT.ADNV.JSNVCDGO): la regla del dueño del adjunto no la contemplaba.
--
-- Síntoma (CUADRE, 18-nov-2026): al justificar movimientos con archivos adjuntos en Seguimiento Novedades v2,
-- «ORA-02290: restricción de control (CNT.CK_ADNV_DUENO) violada». C34-01 agregó la columna JSNVCDGO (adjunto de una
-- justificación) pero no amplió CK_ADNV_DUENO (C25), que exige NVDDCDGO, RQNVCDGO, RSNVCDGO o DCSNCDGO.
-- Corrección: la misma regla con JSNVCDGO como dueño válido. Solo amplía lo permitido: ninguna fila existente la viola.
--
-- Requisito: C34-01 aplicado. Incremental y de una sola vez (el bloque 1 dice si ya está aplicado).
-- SQL PURO. Las sentencias DDL confirman solas; no hay datos que confirmar.
-- =====================================================================================

-- 1. Antes. ESPERADO: 1 fila, la condición SIN «JSNVCDGO». Si ya incluye JSNVCDGO, el script ya se aplicó: no seguir.
SELECT CONSTRAINT_NAME, STATUS, SEARCH_CONDITION_VC FROM ALL_CONSTRAINTS
 WHERE OWNER = 'CNT' AND CONSTRAINT_NAME = 'CK_ADNV_DUENO';

-- 1.2 Justificaciones sin adjuntos que no sean de D-120 (por si alguna quedó grabada a medias). ESPERADO: solo las que el
--     usuario haya hecho a propósito sin archivo; un error de CK_ADNV_DUENO deshace toda la justificación.
SELECT j.JSNVUSAR, COUNT(*) JUSTIFICACIONES,
       SUM(CASE WHEN EXISTS (SELECT 1 FROM CNT.ADNV a WHERE a.JSNVCDGO = j.JSNVCDGO) THEN 1 ELSE 0 END) CON_ADJUNTO
  FROM CNT.JSNV j WHERE j.JSNVESTD = 1 GROUP BY j.JSNVUSAR ORDER BY 1;

-- 2. Corrección
ALTER TABLE CNT.ADNV DROP CONSTRAINT CK_ADNV_DUENO;
ALTER TABLE CNT.ADNV ADD CONSTRAINT CK_ADNV_DUENO CHECK (NVDDCDGO IS NOT NULL OR RQNVCDGO IS NOT NULL OR RSNVCDGO IS NOT NULL
    OR DCSNCDGO IS NOT NULL OR JSNVCDGO IS NOT NULL);

-- 3. Después. ESPERADO: 1 fila, ENABLED / VALIDATED, con «OR JSNVCDGO IS NOT NULL».
SELECT CONSTRAINT_NAME, STATUS, VALIDATED, SEARCH_CONDITION_VC FROM ALL_CONSTRAINTS
 WHERE OWNER = 'CNT' AND CONSTRAINT_NAME = 'CK_ADNV_DUENO';

-- REVERSO (comentado; solo con un WAR sin C34):
-- ALTER TABLE CNT.ADNV DROP CONSTRAINT CK_ADNV_DUENO;
-- ALTER TABLE CNT.ADNV ADD CONSTRAINT CK_ADNV_DUENO CHECK (NVDDCDGO IS NOT NULL OR RQNVCDGO IS NOT NULL OR RSNVCDGO IS NOT NULL OR DCSNCDGO IS NOT NULL);
