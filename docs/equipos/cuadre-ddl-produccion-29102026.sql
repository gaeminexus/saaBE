-- DDL para PRODUCCION — cuadre Superintendencia, 29-oct-2026 (aprobados por el lider: D-024 / D-030).
-- Correr en el cliente JDBC, en este orden, revisando cada control. SQL puro. Ya aplicado en produccion el bloque 0
-- (CK_ANCP_TPOO a 1..4) el 28-oct. Los bloques 1 y 2 van ANTES de desplegar el WAR que trae C16b y el re-proceso.

-- ===== 1. CK_DCBC_MDLD: el abono sin regenerar tabla (C16b) guarda 0 o -N en DCBCMDLD =====
-- Control antes: restriccion actual (esperado: DCBCMDLD IS NULL OR DCBCMDLD IN (1, 2)) y filas fuera de rango (esperado 0)
SELECT constraint_name, search_condition_vc FROM all_constraints WHERE owner = 'CRD' AND constraint_name = 'CK_DCBC_MDLD';
SELECT COUNT(*) FROM CRD.DCBC WHERE DCBCMDLD IS NOT NULL AND DCBCMDLD NOT IN (1, 2);

ALTER TABLE CRD.DCBC DROP CONSTRAINT CK_DCBC_MDLD;
ALTER TABLE CRD.DCBC ADD CONSTRAINT CK_DCBC_MDLD CHECK (DCBCMDLD IS NULL OR DCBCMDLD IN (1, 2) OR DCBCMDLD <= 0);

-- Control despues
SELECT constraint_name, search_condition_vc FROM all_constraints WHERE owner = 'CRD' AND constraint_name = 'CK_DCBC_MDLD';

-- ===== 2. UK_ANCP_VIGENTE: una carga Petro re-procesada mas de dos veces chocaba con las filas reversadas =====
-- Control antes: definicion actual y duplicados vigentes (esperado 0 filas)
SELECT index_name, column_expression FROM all_ind_expressions WHERE index_owner = 'CRD' AND index_name = 'UK_ANCP_VIGENTE';
SELECT CRARCDGO, ANCPTPOO, COUNT(*) FROM CRD.ANCP WHERE ANCPIDST = 1 GROUP BY CRARCDGO, ANCPTPOO HAVING COUNT(*) > 1;

DROP INDEX CRD.UK_ANCP_VIGENTE;
CREATE UNIQUE INDEX CRD.UK_ANCP_VIGENTE ON CRD.ANCP (
    CASE WHEN ANCPIDST = 1 THEN CRARCDGO END,
    CASE WHEN ANCPIDST = 1 THEN ANCPTPOO END);

-- Control despues
SELECT index_name, column_expression FROM all_ind_expressions WHERE index_owner = 'CRD' AND index_name = 'UK_ANCP_VIGENTE';
SELECT CRARCDGO, ANCPTPOO, COUNT(*) FROM CRD.ANCP WHERE ANCPIDST = 1 GROUP BY CRARCDGO, ANCPTPOO HAVING COUNT(*) > 1;