-- =====================================================================================
-- DDL — huella del reverso de un cobro de credito (CRD.CBCR)
-- FECHA: 2026-09-07   EQUIPO: omen-saa-1 (omen1)   SCRIPT: 206 (rango 200-249)
--
-- CONTRATO: docs/logica-negocio/crd/API-REVERSO-COBRO-CREDITO.md  §3
--
-- ESCRIBE: cuatro ALTER TABLE ADD sobre CRD.CBCR. Reverso al final, comentado.
--
-- ⛔ ESTE SCRIPT VA **ANTES** DEL WAR. NO ES OPCIONAL Y NO ES REORDENABLE.
--    Hibernate incluye TODA columna @Column basica en el SELECT que genera. Si el WAR sube
--    con las cuatro columnas mapeadas y la tabla no las tiene, NO se rompe solo el reverso:
--    se rompe **cualquier lectura de CRD.CBCR** con ORA-00904 — la pantalla de cobros
--    entera, la bandeja de contabilidad y la consulta. No se ve al compilar; aparece cuando
--    un usuario abre la pantalla.
--    Precedente exacto: el incidente de CBCRASRP del 2026-08-31.
--
-- ⚠️ SECUENCIAS: este script NO inserta ninguna PK explicita, asi que no hay ninguna
--    secuencia que sincronizar. Se deja dicho para que nadie lo busque.
--
-- ⚠️ GRANT: no hace falta ninguno. Las cuatro columnas son de CRD sobre una tabla de CRD;
--    no hay FK cross-schema, que es lo que en este proyecto exige GRANT REFERENCES y lo que
--    ya fallo en silencio tres veces (ORA-01031 con el resto del script pasando igual).
--
-- COMO DEVOLVER EL RESULTADO: pegar la salida de los bloques 0 y 2.
-- =====================================================================================

SET PAGESIZE 200
SET LINESIZE 240
SET FEEDBACK ON


-- =====================================================================================
-- BLOQUE 0 — CONTROL ANTES
-- Esperado: 0 filas. Si devuelve alguna, el script YA SE CORRIO — no volver a correrlo,
-- los ALTER de abajo fallarian con ORA-01430 (column being added already exists).
-- =====================================================================================

SELECT column_name, data_type, data_length, nullable
  FROM all_tab_columns
 WHERE owner = 'CRD'
   AND table_name = 'CBCR'
   AND column_name IN ('CBCRUSRV', 'CBCRFCRV', 'CBCRMTRV', 'CBCRNMRV')
 ORDER BY column_name;


-- =====================================================================================
-- BLOQUE 1 — EL DDL
-- Las cuatro columnas son NULLABLE a proposito: las 5.664 filas historicas de CBCR nunca
-- se reversaron, y un NOT NULL exigiria un DEFAULT que mentiria sobre ellas.
-- CBCRNMRV se deja NULL en lo historico y el codigo lo lee con nvl(...) = 0.
-- =====================================================================================

ALTER TABLE CRD.CBCR ADD (CBCRUSRV VARCHAR2(50));
COMMENT ON COLUMN CRD.CBCR.CBCRUSRV IS 'Usuario que ejecuto el ultimo reverso del proceso';

ALTER TABLE CRD.CBCR ADD (CBCRFCRV TIMESTAMP);
COMMENT ON COLUMN CRD.CBCR.CBCRFCRV IS 'Fecha y hora del ultimo reverso del proceso';

ALTER TABLE CRD.CBCR ADD (CBCRMTRV VARCHAR2(2000));
COMMENT ON COLUMN CRD.CBCR.CBCRMTRV IS 'Motivo del ultimo reverso del proceso. Obligatorio al reversar';

ALTER TABLE CRD.CBCR ADD (CBCRNMRV NUMBER);
COMMENT ON COLUMN CRD.CBCR.CBCRNMRV IS 'Cuantas veces se reverso el proceso de este cobro. NULL = historico, nunca reversado';


-- =====================================================================================
-- BLOQUE 2 — CONTROL DESPUES
-- Esperado 2.1: exactamente 4 filas.
--   CBCRFCRV TIMESTAMP(6) / CBCRMTRV VARCHAR2 2000 / CBCRNMRV NUMBER / CBCRUSRV VARCHAR2 50
-- Esperado 2.2: 1 fila, con TOTAL = el conteo de CBCR y SIN_REVERSAR igual a TOTAL.
-- Si 2.1 devuelve menos de 4, ALGUN ALTER FALLO y el resto paso igual: NO subir el WAR.
-- =====================================================================================

-- 2.1 Las cuatro columnas existen
SELECT column_name, data_type, data_length, data_precision, nullable
  FROM all_tab_columns
 WHERE owner = 'CRD'
   AND table_name = 'CBCR'
   AND column_name IN ('CBCRUSRV', 'CBCRFCRV', 'CBCRMTRV', 'CBCRNMRV')
 ORDER BY column_name;

-- 2.2 Y se pueden leer sobre los datos reales (esto es lo que hara Hibernate)
SELECT COUNT(*)                                        AS TOTAL,
       COUNT(CBCRNMRV)                                 AS CON_CONTADOR,
       SUM(CASE WHEN CBCRNMRV IS NULL THEN 1 ELSE 0 END) AS SIN_REVERSAR
  FROM CRD.CBCR;


-- =====================================================================================
-- BLOQUE 3 — CONFIRMAR
-- El DDL de Oracle hace COMMIT implicito: cada ALTER ya quedo confirmado al ejecutarse.
-- No hay nada que confirmar aca; se deja el bloque para que nadie busque un COMMIT que
-- no existe y crea que se olvido.
-- =====================================================================================


-- =====================================================================================
-- BLOQUE 4 — REVERSO (comentado)
-- ⛔ Solo si el WAR con el mapeo NO se subio todavia. Si ya esta arriba, borrar estas
--    columnas rompe toda lectura de CRD.CBCR — el mismo ORA-00904 al reves.
-- ⚠️ DROP COLUMN destruye los datos de reverso ya registrados. No hay vuelta atras.
-- =====================================================================================

-- ALTER TABLE CRD.CBCR DROP (CBCRUSRV, CBCRFCRV, CBCRMTRV, CBCRNMRV);
