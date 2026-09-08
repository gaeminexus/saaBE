-- =====================================================================
-- e2-18 — Conceptos informativos «décimo acumulado pagado»
-- Modulo: RHH  ·  Equipo: omen-saa-2  ·  Fecha: 2026-09-07
--
-- ⚠️ NO ES SOLO LECTURA. Inserta filas. Correr el BLOQUE 0 primero.
--
-- ⚠️ REESCRITO el 2026-09-07. La v1 estaba mal en CUATRO cosas, todas por
--    escribir nombres de memoria en vez de sacarlos de las entidades. Habria
--    fallado en todos los bloques. Lo real, verificado en DetalleRubro.java,
--    Rubro.java, ConceptoNomina.java y Empresa.java:
--
--      1. La columna de nombre de SCP.PDTR es PDTRDSCR, no PDTRNMBR.
--      2. El rubro se identifica por su CODIGO ALTERNO (PRBRALTR), no por la
--         PK. Y no son lo mismo: se midio que el rubro con PK 199 es "ESTADO
--         DEL DESCUENTO RECURRENTE", mientras el de tipo de cuenta tiene PK 200
--         y alterno 199. Filtrar por PK devuelve el catalogo equivocado.
--      3. La tabla de empresas NO es SCP.EMPR: la entidad Empresa mapea
--         SCP.PJRQ, con PK PJRQCDGO y nombre PJRQNMBR. Y ConceptoNomina se une
--         a ella por PJRQCDGO.
--      4. RHH.CPNM.CPNMCDGO es IDENTITY (@GeneratedValue IDENTITY), no una
--         secuencia: no se le pasa valor.
--
-- POR QUE EXISTE
--   El decimo acumulado, pagado a mitad de mes por tesoreria, tiene que quedar
--   como NOVEDAD en el rol de fin de mes. Esa novedad necesita un
--   ConceptoNomina, y tiene que ser INFORMATIVO: si se reusara el concepto del
--   decimo MENSUALIZADO, el valor entraria al neto y el decimo se pagaria DOS
--   VECES. Verificado en ProcesoNominaServiceImpl:1078-1080 y :1493-1494 — el
--   neto suma SOLO renglones INGRESO y EGRESO.
--
-- ORDEN: va ANTES del WAR.
-- =====================================================================


-- =====================================================================
-- BLOQUE 0 — CONTROL PREVIO. Correr esto solo, LEER, y recien despues seguir.
-- =====================================================================

-- 0.1 El rubro 221 por ALTERNO, con su PK al lado para que se vea si difieren.
--     ESPERADO: una fila, y su descripcion tiene que hablar del rol del
--     concepto en el motor. Si no aparece, PARAR.
SELECT 'BLOQUE 0.1 - rubro 221' AS control,
       r.PRBRCDGO AS rubro_pk, r.PRBRALTR AS rubro_alterno, r.PRBRDSCR AS descripcion
  FROM SCP.PRBR r WHERE r.PRBRALTR = 221;

-- 0.2 Sus detalles. ESPERADO: alternos 1..31, ninguno 32 ni 33.
SELECT 'BLOQUE 0.2 - detalles del 221' AS control,
       d.PDTRALTR AS alterno, d.PDTRDSCR AS descripcion
  FROM SCP.PDTR d JOIN SCP.PRBR r ON r.PRBRCDGO = d.PRBRCDGO
 WHERE r.PRBRALTR = 221
 ORDER BY d.PDTRALTR;

-- 0.3 ¿Ya existe el 32 o el 33? Si devuelve filas, NO seguir.
SELECT 'BLOQUE 0.3 - conflicto' AS control, d.PDTRALTR, d.PDTRDSCR
  FROM SCP.PDTR d JOIN SCP.PRBR r ON r.PRBRCDGO = d.PRBRCDGO
 WHERE r.PRBRALTR = 221 AND d.PDTRALTR IN (32, 33);

-- 0.4 🔴 ESTE CONTROL ESTABA MAL PLANTEADO Y CASI CUESTA CARO. Decia "dice cuantos
--     conceptos se van a crear: dos por empresa". SCP.PJRQ NO son las empresas que
--     corren nomina: es la tabla de PERSONAS JURIDICAS -- proveedores, clientes, todo.
--     Medido el 2026-09-07: devuelve 786 filas. El BLOQUE 2 original hacia
--     FROM SCP.PJRQ sin filtro y habria insertado 1572 conceptos SIN DAR NINGUN ERROR.
--     Las empresas que de verdad corren nomina son las que ya tienen conceptos en
--     RHH.CPNM (medido: solo la 1236). Ver e2-18c.
SELECT 'BLOQUE 0.4 - empresas con nomina' AS control, c.PJRQCDGO, e.PJRQNMBR,
       COUNT(*) AS conceptos_existentes
  FROM RHH.CPNM c JOIN SCP.PJRQ e ON e.PJRQCDGO = c.PJRQCDGO
 GROUP BY c.PJRQCDGO, e.PJRQNMBR ORDER BY c.PJRQCDGO;

-- 0.5 ¿CPNMCDGO es realmente IDENTITY? Si NO lo fuera, los INSERT del bloque 2
--     fallan por PK nula y hay que agregarle la secuencia que corresponda.
--     ESPERADO: IDENTITY_COLUMN = YES.
SELECT 'BLOQUE 0.5 - identity' AS control, column_name, identity_column, nullable
  FROM all_tab_columns
 WHERE owner = 'RHH' AND table_name = 'CPNM' AND column_name = 'CPNMCDGO';


-- =====================================================================
-- BLOQUE 1 — Los dos detalles del rubro 221
-- =====================================================================
INSERT INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRALTR, PDTRDSCR, PDTRESTD)
SELECT SCP.SQ_PDTRCDGO.NEXTVAL, r.PRBRCDGO, 32, 'DECIMO TERCERO ACUMULADO PAGADO', 1
  FROM SCP.PRBR r
 WHERE r.PRBRALTR = 221
   AND NOT EXISTS (SELECT 1 FROM SCP.PDTR d
                    WHERE d.PRBRCDGO = r.PRBRCDGO AND d.PDTRALTR = 32);

INSERT INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRALTR, PDTRDSCR, PDTRESTD)
SELECT SCP.SQ_PDTRCDGO.NEXTVAL, r.PRBRCDGO, 33, 'DECIMO CUARTO ACUMULADO PAGADO', 1
  FROM SCP.PRBR r
 WHERE r.PRBRALTR = 221
   AND NOT EXISTS (SELECT 1 FROM SCP.PDTR d
                    WHERE d.PRBRCDGO = r.PRBRCDGO AND d.PDTRALTR = 33);


-- =====================================================================
-- BLOQUE 2 — ⛔ ANULADO. NO CORRER. Reemplazado por e2-18c.
-- =====================================================================
-- Este bloque hacia FROM SCP.PJRQ SIN FILTRO, creyendo que esa tabla eran las
-- empresas. NO lo son: SCP.PJRQ es la tabla de PERSONAS JURIDICAS y el
-- 2026-09-07 se midio que tiene 786 filas. Habria insertado 1572 conceptos de
-- nomina, dos por cada proveedor y cada cliente del sistema.
--
-- Y no habria dado ningun error: los INSERT eran validos, el NOT EXISTS se
-- cumplia para las 786 y el COMMIT habria pasado limpio. Se descubrio porque el
-- control 0.4 devolvio 786 donde se esperaba un puñado, y porque los controles
-- 4 y 5 del e2-18b mostraron que TODOS los conceptos que existen son de una
-- sola empresa, la 1236.
--
-- El conjunto correcto son las empresas que ya corren nomina, es decir las que
-- tienen conceptos en RHH.CPNM. Asi ademas se ajusta solo si mañana se da de
-- alta otra. Esta en e2-18c, que es el que hay que correr.
--
-- El BLOQUE 1 de arriba SI es correcto y ya se corrio (los detalles 32 y 33 del
-- rubro 221 existen). Es idempotente por su NOT EXISTS.


-- =====================================================================
-- BLOQUE 3 — CONTROL DE SECUENCIA (regla 8 del equipo)
-- =====================================================================
-- SCP.SQ_PDTRCDGO avanza sola porque el bloque 1 usa NEXTVAL; esto es control.
-- ESPERADO: secuencia POR ENCIMA del maximo de la tabla.
-- RHH.CPNM no lleva secuencia: es IDENTITY, la maneja Oracle.
SELECT 'BLOQUE 3 - secuencia PDTR' AS control,
       (SELECT MAX(PDTRCDGO) FROM SCP.PDTR)                              AS max_tabla,
       (SELECT last_number FROM all_sequences
         WHERE sequence_owner = 'SCP' AND sequence_name = 'SQ_PDTRCDGO') AS secuencia
  FROM DUAL;


-- =====================================================================
-- BLOQUE 4 — CONTROL POSTERIOR
-- =====================================================================
-- 4.2: los dos conceptos POR EMPRESA, los dos con tipo 5. Si alguno saliera con
-- otro tipo, el decimo entraria al neto y se pagaria dos veces: corregir ANTES
-- de desplegar el WAR.
SELECT 'BLOQUE 4.1 - detalles nuevos' AS control, d.PDTRALTR, d.PDTRDSCR
  FROM SCP.PDTR d JOIN SCP.PRBR r ON r.PRBRCDGO = d.PRBRCDGO
 WHERE r.PRBRALTR = 221 AND d.PDTRALTR IN (32, 33) ORDER BY d.PDTRALTR;

SELECT 'BLOQUE 4.2 - conceptos creados' AS control,
       c.CPNMCDGO, c.PJRQCDGO, c.CPNMNMBR, c.CPNMALTR,
       c.CPNMTPCN AS tipo_debe_ser_5, c.CPNMROLM AS rol
  FROM RHH.CPNM c WHERE c.CPNMROLM IN (32, 33)
 ORDER BY c.PJRQCDGO, c.CPNMROLM;


-- =====================================================================
-- BLOQUE 5 — REVERSO. COMENTADO A PROPOSITO.
-- =====================================================================
-- Control antes de borrar (esto SI es lectura). Tiene que dar 0:
--   SELECT COUNT(*) FROM RHH.NVNM n JOIN RHH.CPNM c ON c.CPNMCDGO = n.CPNMCDGO
--    WHERE c.CPNMROLM IN (32, 33);
--
-- DELETE FROM RHH.CPNM WHERE CPNMROLM IN (32, 33);
-- DELETE FROM SCP.PDTR
--  WHERE PDTRALTR IN (32, 33)
--    AND PRBRCDGO IN (SELECT PRBRCDGO FROM SCP.PRBR WHERE PRBRALTR = 221);
-- COMMIT;

-- =====================================================================
-- FIN
-- =====================================================================
