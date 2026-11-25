-- C41-01-ddl-sg45-tipo-participe.sql — C41 (24-nov-2026): tipo de partícipe con que la Superintendencia tiene a cada
-- sujeto en su base G45 (RPR.SG45.SG45TPPR).
--
-- Por qué: la Super rechazó la G48 de junio y julio 2025 (error 12092, «En la estructura G45 con el código J, la cuenta
-- individual deberá ser cero») para tres sujetos que el SAA tiene como partícipes y la Super como jubilados. La G48
-- ahora pone la cuenta individual en cero a los sujetos con «J» en este registro (GeneracionG48ServiceImpl), y la
-- validación previa lo controla. La G45 del SAA guarda aquí el tipo con que reporta a cada sujeto nuevo.
--
-- Se aplica UNA vez, ANTES de desplegar el WAR que trae C41 (después de C39-01 y C40-01). SQL puro (cliente JDBC).
-- Termina SIN COMMIT (el DDL confirma solo; el COMMIT es por costumbre).

-- 1. DDL
ALTER TABLE RPR.SG45 ADD (SG45TPPR VARCHAR2(1));
COMMENT ON COLUMN RPR.SG45.SG45TPPR IS 'C41: tipo de partícipe con que la Super tiene al sujeto en su base G45 (C cesante, J jubilado, P partícipe); vacío = no se sabe';

-- 2. Control. ESPERADO: la columna existe.
SELECT COLUMN_NAME, DATA_TYPE, DATA_LENGTH FROM ALL_TAB_COLUMNS WHERE OWNER = 'RPR' AND TABLE_NAME = 'SG45' AND COLUMN_NAME = 'SG45TPPR';
