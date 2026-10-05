-- =====================================================================================
-- 309 — CBCR.CBCRFCAF (fecha de afectacion) + CRD.MVIC (libro de movimientos por cuota)
-- FECHA: 2026-10-05 · EQUIPO: omen-saa-1 (CRD · EQUIPO B)
--
-- DISENO: crd/DISENO-PROVISION-INTERESES-Y-FECHA-AFECTACION.md
--
-- 1. CBCR.CBCRFCAF — FECHA DE AFECTACION del cobro: la fecha con la que se hace TODA su contabilidad.
--    La fecha de pago real sigue siendo CBCRFCHA. Regla: CBCRFCAF >= CBCRFCHA. Las filas que ya
--    existen se rellenan con CBCRFCHA, que es con lo que se contabilizaron.
-- 2. CRD.MVIC — el libro de movimientos por cuota. Guarda lo que el cierre provisiono, devengo y
--    clasifico de cada cuota, y lo que cada cobro reverso. Asi se provisiona una sola vez y se
--    reversa exacto. Autorizada por el usuario el 2026-10-05.
--
-- ⛔ VA ANTES DEL WAR: CobroCredito mapea CBCRFCAF; sin la columna, toda lectura de CBCR revienta.
-- SQL PURO. Correr por bloques.
-- =====================================================================================

-- 0. CONTROLES PREVIOS ------------------------------------------------------------------
-- 0.1 La columna no existe. Esperado: 0 filas.
SELECT c.COLUMN_NAME FROM ALL_TAB_COLUMNS c
 WHERE c.OWNER = 'CRD' AND c.TABLE_NAME = 'CBCR' AND c.COLUMN_NAME = 'CBCRFCAF';
-- 0.2 La tabla y la secuencia libres. Esperado: 0 y 0 filas.
SELECT t.OWNER, t.TABLE_NAME FROM ALL_TABLES t WHERE t.TABLE_NAME = 'MVIC';
SELECT s.SEQUENCE_NAME FROM ALL_SEQUENCES s WHERE s.SEQUENCE_NAME = 'SQ_MVICCDGO';
-- 0.3 Cobros que hay hoy (los que se van a rellenar).
SELECT COUNT(*) AS COBROS FROM CRD.CBCR;


-- 1. FECHA DE AFECTACION ------------------------------------------------------------------
ALTER TABLE CRD.CBCR ADD (CBCRFCAF DATE);
UPDATE CRD.CBCR SET CBCRFCAF = CBCRFCHA WHERE CBCRFCAF IS NULL;
COMMIT;
-- Sin NOT NULL ni DEFAULT a proposito (leccion de H77): el backend la exige y la valida.
-- CHECK de la regla del usuario: nunca menor que la fecha de pago.
ALTER TABLE CRD.CBCR ADD CONSTRAINT CK_CBCR_FECHA_AFECTACION
  CHECK (CBCRFCAF IS NULL OR CBCRFCHA IS NULL OR CBCRFCAF >= CBCRFCHA);
COMMENT ON COLUMN CRD.CBCR.CBCRFCAF IS 'Fecha de AFECTACION contable del cobro (asientos). CBCRFCHA es la fecha de PAGO REAL (pagos de credito y mora). CBCRFCAF >= CBCRFCHA.';


-- 2. CRD.MVIC — libro de movimientos por cuota --------------------------------------------
CREATE TABLE CRD.MVIC (
  MVICCDGO  NUMBER          NOT NULL,   -- PK
  PRSTCDGO  NUMBER          NOT NULL,   -- FK prestamo
  DTPRCDGO  NUMBER          NOT NULL,   -- cuota, SIN FK (H82: el abono y el reverso borran cuotas)
  MVICTPMV  NUMBER          NOT NULL,   -- tipo de movimiento (ver comentario)
  MVICCMPN  NUMBER          NOT NULL,   -- componente: 1 INTERES (ordinario+vencido) · 2 MORA · 3 CAPITAL
  MVICVLOR  NUMBER(18,2)    NOT NULL,   -- SIEMPRE positivo; el signo lo da el tipo
  MVICFCCT  DATE            NOT NULL,   -- fecha contable del movimiento
  CRCTCDGO  NUMBER,                     -- corrida del cierre (tipos 1, 5 y 7)
  PGPRCDGO  NUMBER,                     -- pago que lo origino (tipos 2, 3, 4, 8 y 9)
  MVICORGN  VARCHAR2(30),               -- origen: CIERRE, CBCR, PETRO, CRUCE, PRECANCELACION, CONDONACION
  MVICIDOR  NUMBER,                     -- id del origen (cobro, carga Petro, evento, acuerdo)
  ASNTCDGO  NUMBER,                     -- asiento que lo registro (sin FK: esquema CNT)
  MVICREVR  NUMBER,                     -- tipo 4: el movimiento tipo 2 que deshace
  MVICTPCR  NUMBER,                     -- componente CAPITAL: tipo de cartera 1 POR VENCER · 2 VENCIDO
  BNDPCDGO  NUMBER,                     -- componente CAPITAL: banda (CRD.BNDP)
  MVICANUL  NUMBER          NOT NULL,   -- 0 vigente · 1 anulado (la corrida o la operacion se reverso)
  MVICUSRG  VARCHAR2(50),
  MVICFCRG  TIMESTAMP
);
COMMENT ON TABLE  CRD.MVIC          IS 'Libro de movimientos por cuota: provision, devengo y clasificacion del cierre de cartera, y sus reversos por cobro. Nunca se borra ni se actualiza, salvo MVICANUL.';
COMMENT ON COLUMN CRD.MVIC.MVICTPMV IS '1 PROVISION (cierre 7) · 2 REVERSO PROVISION POR COBRO · 3 REVERSO PROVISION POR COBRO TARDIO · 4 RE-PROVISION (se deshizo el cobro) · 5 DEVENGO DE MORA (cierre 4) · 6 REVERSO DEVENGO POR COBRO TARDIO · 7 CLASIFICACION DE CAPITAL EN EL CIERRE · 8 RECLASIFICACION POR COBRO TARDIO · 9 REVERSO PROVISION POR CONDONACION';

CREATE SEQUENCE CRD.SQ_MVICCDGO START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

ALTER TABLE CRD.MVIC ADD CONSTRAINT PK_MVIC PRIMARY KEY (MVICCDGO);
ALTER TABLE CRD.MVIC ADD CONSTRAINT FK_MVIC_PRST FOREIGN KEY (PRSTCDGO) REFERENCES CRD.PRST (PRSTCDGO);
ALTER TABLE CRD.MVIC ADD CONSTRAINT FK_MVIC_CRCT FOREIGN KEY (CRCTCDGO) REFERENCES CRD.CRCT (CRCTCDGO);
ALTER TABLE CRD.MVIC ADD CONSTRAINT FK_MVIC_PGPR FOREIGN KEY (PGPRCDGO) REFERENCES CRD.PGPR (PGPRCDGO);
ALTER TABLE CRD.MVIC ADD CONSTRAINT FK_MVIC_REVR FOREIGN KEY (MVICREVR) REFERENCES CRD.MVIC (MVICCDGO);
ALTER TABLE CRD.MVIC ADD CONSTRAINT CK_MVIC_TIPO   CHECK (MVICTPMV BETWEEN 1 AND 9);
ALTER TABLE CRD.MVIC ADD CONSTRAINT CK_MVIC_CMPN   CHECK (MVICCMPN IN (1, 2, 3));
ALTER TABLE CRD.MVIC ADD CONSTRAINT CK_MVIC_VALOR  CHECK (MVICVLOR >= 0);
ALTER TABLE CRD.MVIC ADD CONSTRAINT CK_MVIC_ANUL   CHECK (MVICANUL IN (0, 1));

-- El saldo provisionado por cuota y componente se consulta por (DTPRCDGO, MVICCMPN) todo el tiempo.
CREATE INDEX CRD.IX_MVIC_CUOTA     ON CRD.MVIC (DTPRCDGO, MVICCMPN, MVICANUL);
CREATE INDEX CRD.IX_MVIC_PRESTAMO  ON CRD.MVIC (PRSTCDGO);
CREATE INDEX CRD.IX_MVIC_CORRIDA   ON CRD.MVIC (CRCTCDGO);
CREATE INDEX CRD.IX_MVIC_PAGO      ON CRD.MVIC (PGPRCDGO);


-- 3. CONTROLES POSTERIORES ----------------------------------------------------------------
-- 3.1 CBCRFCAF existe y esta llena. Esperado: SIN_FECHA_AFECTACION = 0.
SELECT COUNT(*) AS COBROS, SUM(CASE WHEN CBCRFCAF IS NULL THEN 1 ELSE 0 END) AS SIN_FECHA_AFECTACION
  FROM CRD.CBCR;
-- 3.2 MVIC: 18 columnas. Esperado: 18.
SELECT COUNT(*) AS COLUMNAS FROM ALL_TAB_COLUMNS c WHERE c.OWNER = 'CRD' AND c.TABLE_NAME = 'MVIC';
-- 3.3 Restricciones con nombre. Esperado: 1 P, 4 R, 4 C en MVIC; 1 C en CBCR (CK_CBCR_FECHA_AFECTACION).
SELECT c.TABLE_NAME, c.CONSTRAINT_TYPE, COUNT(*) AS CUANTAS FROM ALL_CONSTRAINTS c
 WHERE c.OWNER = 'CRD' AND (c.TABLE_NAME = 'MVIC' OR c.CONSTRAINT_NAME = 'CK_CBCR_FECHA_AFECTACION')
   AND c.CONSTRAINT_NAME NOT LIKE 'SYS_%'
 GROUP BY c.TABLE_NAME, c.CONSTRAINT_TYPE ORDER BY 1, 2;
-- 3.4 Secuencia en 1 y tabla vacia. Esperado: 1 y 0.
SELECT s.LAST_NUMBER FROM ALL_SEQUENCES s WHERE s.SEQUENCE_OWNER = 'CRD' AND s.SEQUENCE_NAME = 'SQ_MVICCDGO';
SELECT COUNT(*) AS MVIC FROM CRD.MVIC;


-- 4. REVERSO — COMENTADO. ⛔ Con movimientos cargados, MVIC es lo UNICO que dice que se provisiono.
-- DROP TABLE CRD.MVIC CASCADE CONSTRAINTS; DROP SEQUENCE CRD.SQ_MVICCDGO;
-- ALTER TABLE CRD.CBCR DROP CONSTRAINT CK_CBCR_FECHA_AFECTACION;
-- ALTER TABLE CRD.CBCR DROP COLUMN CBCRFCAF;
