-- =====================================================================================
-- CRD.ACCN — la empresa del acuerdo de condonacion
-- FECHA: 2026-08-30
--
-- POR QUE
-- `generarAsientoCondonacion` resuelve hoy la empresa navegando
-- acuerdo -> cobroCredito -> cuentaBancaria -> planCuenta -> empresa.
-- Pero un acuerdo cubierto 100% con aportes NO TIENE cobroCredito (por diseño: no hay
-- deposito que contabilidad deba verificar, K13/§5 del plan). Con el flag de contabilidad
-- encendido, ese acuerdo fallaria con "no se pudo determinar la empresa" al condonar.
--
-- Es un agujero preexistente que se hizo visible recien al construir CBCRASN2: hoy no
-- explota solo porque el flag esta apagado.
--
-- NO HAY DE DONDE DERIVARLA. Verificado: ni Prestamo, ni Producto, ni Entidad, ni Filial
-- tienen empresa. En crd solo la guardan CRD.CRCT (cierre de cartera) y CRD.CBPR (bandas),
-- las dos recibiendola por parametro — que es el patron de la casa y el que se sigue aca.
--
-- ⚠️ LA EMPRESA ES UN NODO DE JERARQUIA: la columna se llama PJRQCDGO y apunta a
-- SCP.PJRQ, igual que en CRD.CRCT y en CNT.PLNS. No es "EMPRCDGO".
-- =====================================================================================


-- =====================================================================================
-- 0. CONTROLES PREVIOS
-- =====================================================================================

-- 0.1 CRD.ACCN debe existir. Esperado: 1 fila.
SELECT t.OWNER, t.TABLE_NAME FROM ALL_TABLES t
WHERE  t.OWNER = 'CRD' AND t.TABLE_NAME = 'ACCN';

-- 0.2 La columna NO debe existir. Esperado: 0 filas.
SELECT c.COLUMN_NAME FROM ALL_TAB_COLUMNS c
WHERE  c.OWNER = 'CRD' AND c.TABLE_NAME = 'ACCN' AND c.COLUMN_NAME = 'PJRQCDGO';

-- 0.3 Cuantos acuerdos hay ya. Si hay filas, la columna NO puede ser NOT NULL de entrada.
--     Esperado en produccion al 2026-08-30: 0 (la funcionalidad no se ha usado).
SELECT COUNT(*) AS ACUERDOS_EXISTENTES FROM CRD.ACCN;

-- 0.4 Que empresas existen, para saber con cual llenar si el control 0.3 dio > 0.
SELECT j.PJRQCDGO, j.PJRQNMBR FROM SCP.PJRQ j ORDER BY j.PJRQCDGO;


-- =====================================================================================
-- 1. LA COLUMNA
-- =====================================================================================
-- Nullable a proposito: si el control 0.3 dio 0 filas igual conviene dejarla nullable, y
-- que sea el SERVICIO el que la exija al registrar. Un NOT NULL aca obligaria a inventar
-- un valor para cualquier fila historica que apareciera, y ese valor equivocado seria
-- indistinguible de uno correcto.

ALTER TABLE CRD.ACCN ADD (PJRQCDGO NUMBER);

ALTER TABLE CRD.ACCN ADD CONSTRAINT FK_ACCN_PJRQ
    FOREIGN KEY (PJRQCDGO) REFERENCES SCP.PJRQ(PJRQCDGO);

CREATE INDEX CRD.IDX_ACCN_EMPRESA ON CRD.ACCN (PJRQCDGO);

COMMENT ON COLUMN CRD.ACCN.PJRQCDGO IS
    'Empresa (nodo de jerarquia SCP.PJRQ) del acuerdo. UNICA fuente de la empresa para su contabilizacion, en los dos caminos: con deposito y 100% aportes. NO derivarla del cobro: un acuerdo 100% aportes no tiene cobro.';


-- =====================================================================================
-- 2. CONTROL POSTERIOR
-- =====================================================================================

-- 2.1 La columna, la FK y el indice. Esperado: 1 fila cada consulta.
SELECT c.COLUMN_NAME, c.DATA_TYPE, c.NULLABLE
FROM   ALL_TAB_COLUMNS c
WHERE  c.OWNER = 'CRD' AND c.TABLE_NAME = 'ACCN' AND c.COLUMN_NAME = 'PJRQCDGO';

SELECT c.CONSTRAINT_NAME, c.CONSTRAINT_TYPE, c.STATUS
FROM   ALL_CONSTRAINTS c
WHERE  c.OWNER = 'CRD' AND c.CONSTRAINT_NAME = 'FK_ACCN_PJRQ';

SELECT i.INDEX_NAME, i.STATUS FROM ALL_INDEXES i
WHERE  i.OWNER = 'CRD' AND i.INDEX_NAME = 'IDX_ACCN_EMPRESA';

-- 2.2 Si el control 0.3 dio > 0, esas filas quedaron con la empresa en NULL y hay que
--     llenarlas antes de encender la contabilidad. Esperado: 0.
SELECT COUNT(*) AS ACUERDOS_SIN_EMPRESA FROM CRD.ACCN a WHERE a.PJRQCDGO IS NULL;
