-- =====================================================================================
-- CRD.ANCP - sub-proceso 4 (COMPLEMENTO_APERTURA) para el asiento de complemento de apertura
-- FECHA: 2026-10-28
-- CAMBIO: cuadre Superintendencia C4 / decision D-024 del lider.
-- DISENO: docs/logica-negocio/crd/CIERRE-CARTERA-A-FECHA-DE-CORTE.md 1.8 y
--         docs/logica-negocio/petro/REPROCESO-CARGA-PETRO.md 4.13
--
-- QUE HACE: amplia el CHECK CK_ANCP_TPOO de (1, 2, 3) a (1, 2, 3, 4). No toca filas.
-- Sin este cambio, el reparto de cualquier carga Petro/Arch que necesite complemento falla al
-- grabar su fila de CRD.ANCP (ORA-02290) y toda la aplicacion de la carga se revierte.
--
-- Correr ANTES de desplegar el saaBE que trae SubProcesoCobroPetro.COMPLEMENTO_APERTURA, en la
-- base de trabajo y en produccion. Orden: 0 (control previo) -> 1 (ALTER) -> 2 (control).
-- Reverso al final, comentado (solo si no hay ninguna fila con ANCPTPOO = 4).
-- =====================================================================================

-- 0. Control previo: definicion actual del CHECK (esperado: ANCPTPOO IN (1, 2, 3))
SELECT constraint_name, search_condition_vc
FROM   all_constraints
WHERE  owner = 'CRD' AND table_name = 'ANCP' AND constraint_name = 'CK_ANCP_TPOO';

-- 1. Cambio
ALTER TABLE CRD.ANCP DROP CONSTRAINT CK_ANCP_TPOO;
ALTER TABLE CRD.ANCP ADD CONSTRAINT CK_ANCP_TPOO CHECK (ANCPTPOO IN (1, 2, 3, 4));

-- 2. Control posterior (esperado: ANCPTPOO IN (1, 2, 3, 4))
SELECT constraint_name, search_condition_vc
FROM   all_constraints
WHERE  owner = 'CRD' AND table_name = 'ANCP' AND constraint_name = 'CK_ANCP_TPOO';

-- Reverso (comentado):
-- SELECT COUNT(*) FROM CRD.ANCP WHERE ANCPTPOO = 4;   -- tiene que dar 0
-- ALTER TABLE CRD.ANCP DROP CONSTRAINT CK_ANCP_TPOO;
-- ALTER TABLE CRD.ANCP ADD CONSTRAINT CK_ANCP_TPOO CHECK (ANCPTPOO IN (1, 2, 3));
