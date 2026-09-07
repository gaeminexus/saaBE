-- =====================================================================
-- e2-18 — Conceptos informativos «décimo acumulado pagado»
-- Modulo: RHH  ·  Equipo: omen-saa-2  ·  Fecha: 2026-09-07
--
-- ⚠️ NO ES SOLO LECTURA. Inserta filas. Leer los controles antes de correr.
--
-- POR QUE EXISTE
--   El usuario pidio que el decimo acumulado, pagado a mitad de mes por
--   tesoreria, quede registrado como NOVEDAD en el rol de fin de mes. Esa
--   novedad necesita un ConceptoNomina, y el concepto tiene que ser
--   INFORMATIVO: si se reusara el concepto del decimo MENSUALIZADO, el valor
--   entraria al neto y el decimo se pagaria DOS VECES.
--
--   Verificado en ProcesoNominaServiceImpl:1078-1080 y :1493-1494 — el neto se
--   arma sumando SOLO los renglones de tipo INGRESO y EGRESO. Un concepto de
--   tipo INFORMATIVO (RhhTipoConceptoNomina = 5) no entra al neto por
--   construccion, no por una validacion que alguien pueda olvidar.
--
-- QUE INSERTA
--   1. Dos detalles del rubro 221 (RhhRolConceptoMotor), alternos 32 y 33.
--      El ultimo alterno usado hoy es el 31 (FINIQUITO_APORTE_PERSONAL).
--   2. Dos conceptos en RHH.CPNM, uno por empresa, con:
--         CPNMTPCN = 5   (INFORMATIVO)
--         CPNMROLM = 32 / 33
--
--   Hacen falta las dos cosas. Una constante en Java cuyo detalle de rubro no
--   existe en la base es exactamente lo que costo el e2-08 y el e2-13.
--
-- ORDEN: este script va ANTES del WAR.
--
-- ⚠️ COLUMNAS: verificadas contra las entidades ConceptoNomina.java y
--    DetalleRubro. Antes de correr, confirmar con el BLOQUE 0 que los nombres
--    coinciden en esta base — es la regla que este equipo aprendio inventando
--    PRBRNMBR dos veces (§28 del estado).
-- =====================================================================


-- =====================================================================
-- BLOQUE 0 — CONTROL PREVIO. Correr esto solo, LEER, y recien despues seguir.
-- =====================================================================

-- 0.1 Las columnas reales de RHH.CPNM y de SCP.PDTR.
--     ESPERADO: ver CPNMTPCN, CPNMROLM, CPNMNMBR, CPNMABRV, CPNMALTR, EMPRCDGO.
SELECT 'CPNM' AS tabla, column_id, column_name, data_type, nullable
  FROM all_tab_columns WHERE owner = 'RHH' AND table_name = 'CPNM'
 ORDER BY column_id;

SELECT 'PDTR' AS tabla, column_id, column_name, data_type, nullable
  FROM all_tab_columns WHERE owner = 'SCP' AND table_name = 'PDTR'
 ORDER BY column_id;

-- 0.2 ¿El rubro 221 existe y cual es su ultimo alterno?
--     ESPERADO: filas del rubro 221, con el maximo alterno en 31.
SELECT 'BLOQUE 0.2 - alternos del rubro 221' AS control,
       MIN(PDTRALTR) AS alterno_min, MAX(PDTRALTR) AS alterno_max, COUNT(*) AS cuantos
  FROM SCP.PDTR WHERE PRBRCDGO = 221;

-- 0.3 ¿Ya existe algun concepto con rol 32 o 33? Si devuelve filas, NO seguir:
--     alguien ya uso esos alternos y hay que elegir otros.
SELECT 'BLOQUE 0.3 - conflicto de alternos' AS control, PDTRCDGO, PDTRALTR, PDTRNMBR
  FROM SCP.PDTR WHERE PRBRCDGO = 221 AND PDTRALTR IN (32, 33);

-- 0.4 Las empresas activas, para saber cuantos conceptos se van a crear.
SELECT 'BLOQUE 0.4 - empresas' AS control, EMPRCDGO, EMPRNMBR FROM SCP.EMPR ORDER BY EMPRCDGO;


-- =====================================================================
-- BLOQUE 1 — Detalles del rubro 221
-- =====================================================================
-- ⚠️ Ajustar PDTRNMBR/PDTRDSCR si el BLOQUE 0.1 muestra otros nombres de columna.

INSERT INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRALTR, PDTRNMBR, PDTRESTD)
SELECT SCP.SQ_PDTRCDGO.NEXTVAL, 221, 32, 'DECIMO_TERCERO_ACUMULADO_PAGADO', 1 FROM DUAL
 WHERE NOT EXISTS (SELECT 1 FROM SCP.PDTR WHERE PRBRCDGO = 221 AND PDTRALTR = 32);

INSERT INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRALTR, PDTRNMBR, PDTRESTD)
SELECT SCP.SQ_PDTRCDGO.NEXTVAL, 221, 33, 'DECIMO_CUARTO_ACUMULADO_PAGADO', 1 FROM DUAL
 WHERE NOT EXISTS (SELECT 1 FROM SCP.PDTR WHERE PRBRCDGO = 221 AND PDTRALTR = 33);


-- =====================================================================
-- BLOQUE 2 — Los dos conceptos, por empresa
-- =====================================================================
-- CPNMTPCN = 5 (INFORMATIVO) es lo que impide que entren al neto.
-- CPNMALTR: codigo alterno del concepto dentro de la empresa. Se toma
-- MAX+1 por empresa para no chocar con la numeracion existente.

INSERT INTO RHH.CPNM (CPNMCDGO, EMPRCDGO, CPNMNMBR, CPNMABRV, CPNMALTR,
                      CPNMTPCN, CPNMROLM, CPNMESTD)
SELECT RHH.SQ_CPNMCDGO.NEXTVAL,
       e.EMPRCDGO,
       'Decimo tercero acumulado pagado',
       'D3ACPG',
       NVL((SELECT MAX(c.CPNMALTR) FROM RHH.CPNM c WHERE c.EMPRCDGO = e.EMPRCDGO), 0) + 1,
       5,    -- INFORMATIVO
       32,   -- RhhRolConceptoMotor.DECIMO_TERCERO_ACUMULADO_PAGADO
       1
  FROM SCP.EMPR e
 WHERE NOT EXISTS (SELECT 1 FROM RHH.CPNM c2
                    WHERE c2.EMPRCDGO = e.EMPRCDGO AND c2.CPNMROLM = 32);

INSERT INTO RHH.CPNM (CPNMCDGO, EMPRCDGO, CPNMNMBR, CPNMABRV, CPNMALTR,
                      CPNMTPCN, CPNMROLM, CPNMESTD)
SELECT RHH.SQ_CPNMCDGO.NEXTVAL,
       e.EMPRCDGO,
       'Decimo cuarto acumulado pagado',
       'D4ACPG',
       NVL((SELECT MAX(c.CPNMALTR) FROM RHH.CPNM c WHERE c.EMPRCDGO = e.EMPRCDGO), 0) + 1,
       5,    -- INFORMATIVO
       33,   -- RhhRolConceptoMotor.DECIMO_CUARTO_ACUMULADO_PAGADO
       1
  FROM SCP.EMPR e
 WHERE NOT EXISTS (SELECT 1 FROM RHH.CPNM c2
                    WHERE c2.EMPRCDGO = e.EMPRCDGO AND c2.CPNMROLM = 33);

COMMIT;


-- =====================================================================
-- BLOQUE 3 — SINCRONIZAR LAS SECUENCIAS
-- =====================================================================
-- Es la regla 8 del esquema de trabajo de este equipo, y ya tuvo su ejemplar:
-- SQ_BEXTCDGO habia quedado en 95 con la tabla en 389 y el alta de bancos
-- externos estaba rota (§30.11 y §33 del estado).
--
-- Aca los INSERT usan NEXTVAL, asi que las secuencias avanzan solas y NO
-- deberian quedar atrasadas. Este bloque es de CONTROL: si el maximo de la
-- tabla supera el last_number de su secuencia, hay que corregirla.
--
-- ESPERADO: last_number POR ENCIMA del maximo en las dos filas.

SELECT 'BLOQUE 3 - PDTR' AS control,
       (SELECT MAX(PDTRCDGO) FROM SCP.PDTR)                                  AS max_tabla,
       (SELECT last_number FROM all_sequences
         WHERE sequence_owner = 'SCP' AND sequence_name = 'SQ_PDTRCDGO')     AS secuencia
  FROM DUAL;

SELECT 'BLOQUE 3 - CPNM' AS control,
       (SELECT MAX(CPNMCDGO) FROM RHH.CPNM)                                  AS max_tabla,
       (SELECT last_number FROM all_sequences
         WHERE sequence_owner = 'RHH' AND sequence_name = 'SQ_CPNMCDGO')     AS secuencia
  FROM DUAL;


-- =====================================================================
-- BLOQUE 4 — CONTROL POSTERIOR. Que quedo.
-- =====================================================================
-- ESPERADO: dos filas en 4.1, y en 4.2 dos conceptos POR EMPRESA, los dos con
-- tipo 5 (INFORMATIVO). Si alguno saliera con otro tipo, el decimo entraria al
-- neto y se pagaria dos veces: corregir ANTES de desplegar el WAR.

SELECT 'BLOQUE 4.1 - detalles del rubro 221' AS control, PDTRCDGO, PDTRALTR, PDTRNMBR
  FROM SCP.PDTR WHERE PRBRCDGO = 221 AND PDTRALTR IN (32, 33) ORDER BY PDTRALTR;

SELECT 'BLOQUE 4.2 - conceptos creados' AS control,
       c.CPNMCDGO, c.EMPRCDGO, c.CPNMNMBR, c.CPNMALTR,
       c.CPNMTPCN AS tipo_debe_ser_5, c.CPNMROLM AS rol
  FROM RHH.CPNM c WHERE c.CPNMROLM IN (32, 33)
 ORDER BY c.EMPRCDGO, c.CPNMROLM;


-- =====================================================================
-- BLOQUE 5 — REVERSO. COMENTADO A PROPOSITO.
-- =====================================================================
-- Descomentar SOLO si hay que deshacer, y unicamente si el BLOQUE 4.2 muestra
-- que NINGUNA novedad se creo todavia con estos conceptos: si ya hay novedades
-- colgando, borrar el concepto deja RHH.NVNM con una FK rota.
--
-- Control antes de borrar (esto SI se puede correr, es lectura):
--   SELECT COUNT(*) FROM RHH.NVNM n JOIN RHH.CPNM c ON c.CPNMCDGO = n.CPNMCDGO
--    WHERE c.CPNMROLM IN (32, 33);
--   -- Tiene que dar 0.
--
-- DELETE FROM RHH.CPNM WHERE CPNMROLM IN (32, 33);
-- DELETE FROM SCP.PDTR WHERE PRBRCDGO = 221 AND PDTRALTR IN (32, 33);
-- COMMIT;

-- =====================================================================
-- FIN
-- =====================================================================
