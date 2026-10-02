-- =====================================================================================
-- 307 — CRD.DPLV: quitar la FK a CRD.DTPR (bloquea el abono a capital y el reverso de operaciones)
-- FECHA: 2026-10-02 · EQUIPO: omen-saa-1 (CRD · EQUIPO B)
--
-- EL DEFECTO (H82, encontrado por el arbitro al disenar polizas): el 247 creo FK_DPLV_DTPR. Pero el
-- abono a capital y el reverso de operaciones BORRAN cuotas de CRD.DTPR y las vuelven a crear
-- (AbonoCapitalPrestamoServiceImpl:194-203, ProcesoPagoPrestamoServiceImpl:1336). Si un prestamo tiene
-- filas en DPLV (fue declarado de plazo vencido y se le anulo el seguro de cuotas futuras), un abono
-- o un reverso sobre el falla con ORA-02292 «se ha encontrado un registro secundario».
--
-- DPLV es HISTORIA (el seguro original de una cuota para el reverso de la declaracion): tiene que
-- poder conservar el DTPRCDGO aunque la cuota ya no exista. Se quita la FK y queda el indice.
--
-- SQL PURO. Correr por bloques. No borra datos.
-- =====================================================================================

-- 0. La FK existe. Esperado: 1 fila, FK_DPLV_DTPR, ENABLED.
SELECT c.CONSTRAINT_NAME, c.STATUS FROM ALL_CONSTRAINTS c
 WHERE c.OWNER = 'CRD' AND c.TABLE_NAME = 'DPLV' AND c.CONSTRAINT_NAME = 'FK_DPLV_DTPR';

-- 0.1 Informativo: cuantas filas de DPLV hay y cuantas apuntan a prestamos hoy.
SELECT COUNT(*) AS FILAS_DPLV, COUNT(DISTINCT v.PRSTCDGO) AS PRESTAMOS
  FROM CRD.DPLV x JOIN CRD.PLVN v ON v.PLVNCDGO = x.PLVNCDGO;

-- 1. Quitar la FK.
ALTER TABLE CRD.DPLV DROP CONSTRAINT FK_DPLV_DTPR;

-- 2. Indice para seguir buscando por cuota sin la FK (la UX_DPLV_DECLARACION_CUOTA empieza por PLVNCDGO).
CREATE INDEX CRD.IX_DPLV_DTPR ON CRD.DPLV (DTPRCDGO);

-- 3. Control. Esperado: 0 filas con FK_DPLV_DTPR; 1 fila con IX_DPLV_DTPR.
SELECT c.CONSTRAINT_NAME FROM ALL_CONSTRAINTS c
 WHERE c.OWNER = 'CRD' AND c.TABLE_NAME = 'DPLV' AND c.CONSTRAINT_NAME = 'FK_DPLV_DTPR';
SELECT i.INDEX_NAME FROM ALL_INDEXES i WHERE i.OWNER = 'CRD' AND i.INDEX_NAME = 'IX_DPLV_DTPR';

-- 4. REVERSO — COMENTADO (sólo si no se borro ninguna cuota referenciada en el medio)
-- DROP INDEX CRD.IX_DPLV_DTPR;
-- ALTER TABLE CRD.DPLV ADD CONSTRAINT FK_DPLV_DTPR FOREIGN KEY (DTPRCDGO) REFERENCES CRD.DTPR (DTPRCDGO);
