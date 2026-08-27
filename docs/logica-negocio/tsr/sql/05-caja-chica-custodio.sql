-- =====================================================================
-- HISTORICO — NO EJECUTAR EN PRODUCCION
-- El script 02-caja-chica.sql ya crea el custodio apuntando a RHH.MPLD.
-- Este archivo solo reparo la base LOCAL, donde el 02 se corrio en una
-- version anterior que apuntaba a SCP.PJRQ. Ver README-ORDEN-PRODUCCION.md
-- =====================================================================

-- =====================================================================
-- CAJA CHICA: el custodio es un colaborador, no un usuario del sistema
-- Módulo: TSR
-- Fecha:  2026-08-27
-- Autor:  orquestador  (corrige un error propio del script 02)
--
-- MOTIVO:
--   El script 02 definió CJCH.CJCHUSCS como FK a SCP.PJRQ (Usuario del
--   sistema). Es incorrecto: el custodio de una caja chica es la persona
--   que responde por el efectivo, y no tiene por qué tener login. El
--   usuario confirmó que debe ser "un titular o un colaborador tomado de
--   la tabla de RRHH".
--
--   Se repunta a RHH.MPLD (Empleado), que es el caso real: el custodio es
--   personal interno. Para cualquier otro caso queda el campo de texto
--   CJCHRSPN (responsable), que ya existe y no cambia.
--
--   Verificado: TSR.CJCH no tiene ninguna fila con CJCHUSCS grabado (el
--   frontend nunca llegó a enviarlo), así que el cambio no toca datos.
--
-- SQL puro. Ejecutar por bloques revisando los SELECT de control.
-- =====================================================================

-- ---------------------------------------------------------------------
-- BLOQUE 1: control previo — debe devolver 0
-- ---------------------------------------------------------------------
SELECT COUNT(*) AS CAJAS_CON_CUSTODIO FROM TSR.CJCH WHERE CJCHUSCS IS NOT NULL;

-- Si NO es 0, detenerse: hay que traducir esos códigos de usuario a empleado
-- antes de repuntar la FK.

-- ---------------------------------------------------------------------
-- BLOQUE 2: privilegio REFERENCES para la FK cross-schema TSR -> RHH.MPLD
--   Oracle no considera los privilegios heredados por ROL al crear un
--   constraint. Ejecutar conectado como RHH (o como DBA).
--   Si RHH.MPLD ya tiene REFERENCES concedido a PUBLIC, esta línea sobra
--   y da ORA-01720 o similar: se puede saltar.
-- ---------------------------------------------------------------------
SELECT GRANTEE, OWNER, TABLE_NAME FROM DBA_TAB_PRIVS
 WHERE PRIVILEGE = 'REFERENCES' AND OWNER = 'RHH' AND TABLE_NAME = 'MPLD';

GRANT REFERENCES ON RHH.MPLD TO TSR;

-- ---------------------------------------------------------------------
-- BLOQUE 3: repuntar la FK del custodio
-- ---------------------------------------------------------------------
ALTER TABLE TSR.CJCH DROP CONSTRAINT FK_CJCH_USCS;

ALTER TABLE TSR.CJCH ADD CONSTRAINT FK_CJCH_MPLD
    FOREIGN KEY (CJCHUSCS) REFERENCES RHH.MPLD(MPLDCDGO);

CREATE INDEX TSR.IDX_CJCH_USCS ON TSR.CJCH(CJCHUSCS);

COMMENT ON COLUMN TSR.CJCH.CJCHUSCS IS 'Colaborador custodio de la caja (RHH.MPLD). El nombre de la columna es herencia de la primera version, cuando apuntaba a SCP.PJRQ';

-- ---------------------------------------------------------------------
-- BLOQUE 4: control final — FK_CJCH_MPLD apuntando a RHH.MPLD
-- ---------------------------------------------------------------------
SELECT C.CONSTRAINT_NAME, C.R_OWNER || '.' || RC.TABLE_NAME AS TABLA_REFERENCIADA,
       CC.COLUMN_NAME, C.STATUS, C.VALIDATED
  FROM ALL_CONSTRAINTS C
  JOIN ALL_CONS_COLUMNS CC ON CC.CONSTRAINT_NAME = C.CONSTRAINT_NAME AND CC.OWNER = C.OWNER
  JOIN ALL_CONSTRAINTS RC ON RC.CONSTRAINT_NAME = C.R_CONSTRAINT_NAME AND RC.OWNER = C.R_OWNER
 WHERE C.OWNER = 'TSR' AND C.TABLE_NAME = 'CJCH' AND C.CONSTRAINT_TYPE = 'R'
 ORDER BY C.CONSTRAINT_NAME;
