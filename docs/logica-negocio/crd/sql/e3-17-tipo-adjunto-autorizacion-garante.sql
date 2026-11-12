-- =====================================================================================
-- e3-17 — Tipo de adjunto «AUTORIZACION DEL GARANTE» en CRD.TPDJ
-- Equipo omen-saa-3 · 2026-11-11 · Diseño: crd/DISENO-PAGO-CON-APORTES-DEL-GARANTE.md (G4)
-- Correr ANTES del EAR garante-1111. Idempotente: si ya existe no inserta nada.
-- Sin esta fila el EAR arranca igual; solo el alta de garante en préstamos ya otorgados da error.
-- =====================================================================================

-- 1. Control previo. ESPERADO: 0 filas (o 1 si ya se corrió).
SELECT TPDJCDGO, TPDJNMBR, TPDJIDST FROM CRD.TPDJ WHERE UPPER(TRIM(TPDJNMBR)) = 'AUTORIZACION DEL GARANTE';

-- 2. Alta (mismo patrón que crd/sql/376, bloque 3).
INSERT INTO CRD.TPDJ (TPDJNMBR, TPDJIDST)
SELECT 'AUTORIZACION DEL GARANTE', 1 FROM DUAL
 WHERE NOT EXISTS (SELECT 1 FROM CRD.TPDJ t WHERE UPPER(TRIM(t.TPDJNMBR)) = 'AUTORIZACION DEL GARANTE');
COMMIT;

-- 3. Control después. ESPERADO: exactamente 1 fila, TPDJIDST = 1.
SELECT TPDJCDGO, TPDJNMBR, TPDJIDST FROM CRD.TPDJ WHERE UPPER(TRIM(TPDJNMBR)) = 'AUTORIZACION DEL GARANTE';
