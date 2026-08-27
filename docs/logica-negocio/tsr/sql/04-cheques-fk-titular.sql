-- =====================================================================
-- CHEQUES: repuntar la FK del beneficiario de TSR.PRSN a TSR.TTLR
-- Módulo: TSR
-- Fecha:  2026-08-27
-- Autor:  orquestador
--
-- MOTIVO (defecto del modelo heredado, detectado al probar el primer pago
-- con cheque en el navegador):
--
--   La columna TSR.DTCH.PRSNCDGO tiene la FK FK_DTCH_PRSN apuntando a
--   TSR.PRSN (tabla Persona del sistema legado), pero la entidad JPA
--   com.saa.model.tsr.Cheque la mapea al Titular vigente:
--       @JoinColumn(name = "PRSNCDGO", referencedColumnName = "TTLRCDGO")
--   Al girar un cheque a un beneficiario real la base rechaza el UPDATE con
--   ORA-02291 (restriccion de integridad FK_DTCH_PRSN violada), porque ese
--   codigo existe en TTLR y no en PRSN. Nunca se habia detectado porque la
--   tabla DTCH no se usaba.
--
--   Verificado en la base: TSR.PRSN tiene 2 filas, TSR.TTLR tiene 87, y
--   NINGUN codigo en comun. PRSN es una tabla muerta; el modelo vigente es
--   TTLR. Ningun cheque tiene PRSNCDGO grabado todavia, asi que repuntar la
--   FK no puede romper datos existentes.
--
-- Se aprovecha para dar de alta la FK que falta en DTCHIDBN (beneficiario
-- alterno), que la entidad tambien mapea a Titular y hoy no tiene ninguna.
--
-- SQL puro. Ejecutar por bloques revisando los SELECT de control.
-- =====================================================================

-- ---------------------------------------------------------------------
-- BLOQUE 1: control previo
--   (a) FKs actuales de DTCH: debe verse FK_DTCH_PRSN -> TSR.PRSN
--   (b) filas de DTCH con beneficiario grabado: debe ser 0
-- ---------------------------------------------------------------------
SELECT C.CONSTRAINT_NAME, C.R_OWNER || '.' || RC.TABLE_NAME AS TABLA_REFERENCIADA, CC.COLUMN_NAME
  FROM ALL_CONSTRAINTS C
  JOIN ALL_CONS_COLUMNS CC ON CC.CONSTRAINT_NAME = C.CONSTRAINT_NAME AND CC.OWNER = C.OWNER
  JOIN ALL_CONSTRAINTS RC ON RC.CONSTRAINT_NAME = C.R_CONSTRAINT_NAME AND RC.OWNER = C.R_OWNER
 WHERE C.OWNER = 'TSR' AND C.TABLE_NAME = 'DTCH' AND C.CONSTRAINT_TYPE = 'R'
 ORDER BY C.CONSTRAINT_NAME;

SELECT COUNT(*) AS CHEQUES_CON_BENEFICIARIO
  FROM TSR.DTCH
 WHERE PRSNCDGO IS NOT NULL OR DTCHIDBN IS NOT NULL;

-- Si el conteo anterior NO es 0, detenerse: hay que revisar esos cheques
-- antes de repuntar la FK (sus codigos podrian existir en PRSN y no en TTLR).

-- ---------------------------------------------------------------------
-- BLOQUE 2: repuntar la FK del beneficiario al Titular vigente
-- ---------------------------------------------------------------------
ALTER TABLE TSR.DTCH DROP CONSTRAINT FK_DTCH_PRSN;

ALTER TABLE TSR.DTCH ADD CONSTRAINT FK_DTCH_TTLR
    FOREIGN KEY (PRSNCDGO) REFERENCES TSR.TTLR(TTLRCDGO);

COMMENT ON COLUMN TSR.DTCH.PRSNCDGO IS 'Titular beneficiario del cheque (TSR.TTLR). El nombre de la columna es herencia del modelo antiguo, cuando apuntaba a TSR.PRSN';

-- ---------------------------------------------------------------------
-- BLOQUE 3: FK que faltaba para el beneficiario alterno
-- ---------------------------------------------------------------------
ALTER TABLE TSR.DTCH ADD CONSTRAINT FK_DTCH_IDBN
    FOREIGN KEY (DTCHIDBN) REFERENCES TSR.TTLR(TTLRCDGO);

CREATE INDEX TSR.IDX_DTCH_PRSN ON TSR.DTCH(PRSNCDGO);
CREATE INDEX TSR.IDX_DTCH_IDBN ON TSR.DTCH(DTCHIDBN);

-- ---------------------------------------------------------------------
-- BLOQUE 4: control final — las tres FKs deben apuntar a TTLR, CHQR y ASNT
-- ---------------------------------------------------------------------
SELECT C.CONSTRAINT_NAME, C.R_OWNER || '.' || RC.TABLE_NAME AS TABLA_REFERENCIADA,
       CC.COLUMN_NAME, C.STATUS, C.VALIDATED
  FROM ALL_CONSTRAINTS C
  JOIN ALL_CONS_COLUMNS CC ON CC.CONSTRAINT_NAME = C.CONSTRAINT_NAME AND CC.OWNER = C.OWNER
  JOIN ALL_CONSTRAINTS RC ON RC.CONSTRAINT_NAME = C.R_CONSTRAINT_NAME AND RC.OWNER = C.R_OWNER
 WHERE C.OWNER = 'TSR' AND C.TABLE_NAME = 'DTCH' AND C.CONSTRAINT_TYPE = 'R'
 ORDER BY C.CONSTRAINT_NAME;
