-- =====================================================================
-- e2-26 — RHH.VNPG: valores no pagados en nomina
-- Modulo: RHH  ·  Equipo: omen-saa-2  ·  Fecha: 2026-09-08
--
-- ⚠️ NO ES SOLO LECTURA. Crea una tabla, un rubro y conceptos. Correr el
--    BLOQUE 0 primero, LEERLO, y recien despues seguir.
--
-- QUE HACE
--   1. Secuencia RHH.SQ_VNPGCDGO
--   2. Tabla RHH.VNPG — un valor que NO se le paga a un empleado en un periodo
--      y se le devuelve en el pago del periodo siguiente
--   3. Rubro 311 RHH_ESTADO_VALOR_NO_PAGADO + sus 5 detalles (PDTR 1504-1508)
--   4. Detalles 34 y 35 del rubro 221 (rol del concepto en el motor),
--      PDTR 1509-1510
--   5. Dos conceptos INFORMATIVOS por cada empresa que corre nomina
--
--   Diseño completo: docs/logica-negocio/rhh/PLAN-VALORES-NO-PAGADOS.md
--   Reserva: REGISTRO-RESERVAS-EQUIPOS.md §5, fila del 2026-09-08
--
-- POR QUE EXISTE
--   Pedido del usuario: registrar por empleado y periodo un valor que no se
--   pago ese mes. NO afecta la contabilidad del rol ni las provisiones: el rol
--   devenga completo. Solo se resta del PAGO de ese mes y se suma al PAGO del
--   siguiente, asi que lo unico que se mueve es remuneraciones por pagar. En
--   el rol se ve como una linea en negativo el primer mes y en positivo el
--   siguiente. Es un anticipo al reves.
--
-- POR QUE LOS CONCEPTOS TIENEN QUE SER INFORMATIVOS (CPNMTPCN = 5)
--   El motor suma al neto SOLO los renglones INGRESO y EGRESO
--   (ProcesoNominaServiceImpl:1078-1080). Un renglon INFORMATIVO se ve en el
--   rol y no toca neto, bases de IESS/IR, provisiones ni contabilidad. Si
--   alguno de los dos conceptos quedara con otro tipo, el valor se
--   descontaria DOS VECES: una en el rol y otra en la orden de pago. El
--   BLOQUE 7 lo verifica fila por fila.
--
-- ORDEN RESPECTO DEL WAR
--   ESTE SCRIPT VA ANTES DEL WAR. La entidad ValorNoPagado mapea RHH.VNPG y
--   Hibernate valida el mapeo al arrancar: sin la tabla, el despliegue falla.
--
-- CONVENCIONES DE ESTE SCRIPT
--   Sin PROMPT / SET / COLUMN: son comandos de SQL*Plus y el cliente del
--   usuario los muestra como texto suelto. Banners con -- y etiquetas
--   literales en cada SELECT de control.
--
--   Todos los nombres de columna y de PK ajena estan leidos de las entidades,
--   no escritos de memoria: PRDN.PRDNCDGO, RDPG.RDPGCDGO, LQDC.LQDCCDGO,
--   MPLD.MPLDCDGO, PJRQ.PJRQCDGO (verificados el 2026-09-08). Inventar un
--   nombre costo cuatro ORA-00904 en dos dias.
-- =====================================================================


-- =====================================================================
-- BLOQUE 0 — CONTROLES ANTES. Correr esto solo, LEER, y pegar el resultado.
-- =====================================================================

-- 0.1 La tabla NO debe existir. ESPERADO: 0 filas.
--     Si devuelve algo, PARAR: alguien la creo y hay que ver por que.
SELECT '0.1 - VNPG ya existe?' AS control, owner, table_name
  FROM all_tables WHERE table_name = 'VNPG';

-- 0.2 La secuencia NO debe existir. ESPERADO: 0 filas.
SELECT '0.2 - secuencia ya existe?' AS control, sequence_owner, sequence_name
  FROM all_sequences WHERE sequence_name = 'SQ_VNPGCDGO';

-- 0.3 Revalidar el MAX justo antes de ejecutar (regla 2 del registro).
--     ESPERADO: max_prbr < 311 y max_pdtr < 1504. Si alguno los alcanzo,
--     PARAR Y AVISAR. No forzar.
SELECT '0.3 - MAX de los catalogos' AS control,
       (SELECT MAX(PRBRCDGO) FROM SCP.PRBR) AS max_prbr,
       (SELECT MAX(PDTRCDGO) FROM SCP.PDTR) AS max_pdtr
  FROM DUAL;

-- 0.4 Los codigos concretos deben estar libres. ESPERADO: 0 filas en los tres.
--     OJO: el codigo (PRBRCDGO) y el alterno (PRBRALTR) son cosas distintas y
--     el codigo de la aplicacion busca por ALTERNO. Se verifican los dos.
SELECT '0.4a - PRBR 311 libre?' AS control, PRBRCDGO, PRBRDSCR
  FROM SCP.PRBR WHERE PRBRCDGO = 311 OR PRBRALTR = 311;
SELECT '0.4b - PDTR 1504-1510 libres?' AS control, PDTRCDGO, PDTRDSCR
  FROM SCP.PDTR WHERE PDTRCDGO BETWEEN 1504 AND 1510;

-- 0.5 El rubro 221 (rol del concepto en el motor) existe y NO tiene 34 ni 35.
--     ESPERADO: una fila del rubro y CERO filas de detalles 34/35.
SELECT '0.5a - rubro 221' AS control, PRBRCDGO AS rubro_pk, PRBRALTR AS alterno, PRBRDSCR
  FROM SCP.PRBR WHERE PRBRALTR = 221;
SELECT '0.5b - conflicto 34/35' AS control, d.PDTRALTR, d.PDTRDSCR
  FROM SCP.PDTR d JOIN SCP.PRBR r ON r.PRBRCDGO = d.PRBRCDGO
 WHERE r.PRBRALTR = 221 AND d.PDTRALTR IN (34, 35);

-- 0.6 Cuantas empresas corren nomina, y cuantos conceptos va a crear el
--     BLOQUE 6: DOS por empresa. ESPERADO: un puñado (medido el 2026-09-07:
--     UNA, la 1236, o sea total_a_insertar = 2).
--     🔴 Si dijera cientos, PARAR: seria la confusion SCP.PJRQ (personas
--        juridicas, 786 filas) que casi creo 1.572 conceptos el 2026-09-07.
SELECT '0.6 - conceptos a crear' AS control,
       (SELECT COUNT(DISTINCT PJRQCDGO) FROM RHH.CPNM) AS empresas_con_nomina,
       (SELECT COUNT(DISTINCT PJRQCDGO) FROM RHH.CPNM) * 2 AS total_a_insertar
  FROM DUAL;

-- 0.7 Que ningun concepto use ya los roles 34/35. ESPERADO: 0 filas.
SELECT '0.7 - conceptos con rol 34/35' AS control, CPNMCDGO, PJRQCDGO, CPNMNMBR, CPNMROLM
  FROM RHH.CPNM WHERE CPNMROLM IN (34, 35);


-- =====================================================================
-- BLOQUE 1 — SECUENCIA
-- =====================================================================
CREATE SEQUENCE RHH.SQ_VNPGCDGO START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;


-- =====================================================================
-- BLOQUE 2 — TABLA RHH.VNPG
-- =====================================================================
-- Columnas EXACTAMENTE como las mapea com.saa.model.rhh.ValorNoPagado.
CREATE TABLE RHH.VNPG (
    VNPGCDGO  NUMBER          NOT NULL,   -- PK
    PJRQCDGO  NUMBER          NOT NULL,   -- empresa (SCP.PJRQ)
    MPLDCDGO  NUMBER          NOT NULL,   -- empleado (RHH.MPLD)
    VNPGPRNM  NUMBER          NOT NULL,   -- periodo en que NO se paga (RHH.PRDN)
    VNPGVLOR  NUMBER(18,2)    NOT NULL,   -- valor retenido, siempre positivo
    VNPGMTVO  VARCHAR2(500)   NOT NULL,   -- motivo, obligatorio
    VNPGESTD  NUMBER          NOT NULL,   -- estado (rubro 311)
    VNPGPRRC  NUMBER          NULL,       -- periodo en que se devolvio (RHH.PRDN)
    VNPGORRT  NUMBER          NULL,       -- orden de pago que lo RETUVO (RHH.RDPG)
    VNPGORPG  NUMBER          NULL,       -- orden de pago que lo DEVOLVIO (RHH.RDPG)
    VNPGLQDC  NUMBER          NULL,       -- finiquito que lo absorbio (RHH.LQDC)
    VNPGFCHR  TIMESTAMP       NULL,       -- fecha de registro
    VNPGUSRR  VARCHAR2(60)    NULL,       -- usuario de registro
    VNPGMTAN  VARCHAR2(500)   NULL,       -- motivo de anulacion
    VNPGFCAN  TIMESTAMP       NULL,       -- fecha de anulacion
    VNPGUSAN  VARCHAR2(60)    NULL,       -- usuario de anulacion
    CONSTRAINT PK_VNPG PRIMARY KEY (VNPGCDGO),
    CONSTRAINT CK_VNPG_VLOR CHECK (VNPGVLOR > 0)
);

-- =====================================================================
-- ⛔ EL GRANT. VA ANTES DE LA FK A SCP.PJRQ Y LO CORRE OTRO USUARIO.
--   Oracle no considera privilegios heredados por ROL al crear un constraint:
--   hace falta el GRANT directo del dueño de SCP (o un DBA). En el e2-06 esto
--   quedo comentado, la FK fallo con ORA-01031 en silencio, el script
--   "parecio" correr y se descubrio tres dias despues en produccion (e2-11).
--   Si el usuario ya tiene el privilegio, re-otorgarlo no falla.
-- =====================================================================
GRANT REFERENCES ON SCP.PJRQ TO RHH;

ALTER TABLE RHH.VNPG ADD CONSTRAINT FK_VNPG_PJRQ
    FOREIGN KEY (PJRQCDGO) REFERENCES SCP.PJRQ (PJRQCDGO);
ALTER TABLE RHH.VNPG ADD CONSTRAINT FK_VNPG_MPLD
    FOREIGN KEY (MPLDCDGO) REFERENCES RHH.MPLD (MPLDCDGO);
ALTER TABLE RHH.VNPG ADD CONSTRAINT FK_VNPG_PRNM
    FOREIGN KEY (VNPGPRNM) REFERENCES RHH.PRDN (PRDNCDGO);
ALTER TABLE RHH.VNPG ADD CONSTRAINT FK_VNPG_PRRC
    FOREIGN KEY (VNPGPRRC) REFERENCES RHH.PRDN (PRDNCDGO);
ALTER TABLE RHH.VNPG ADD CONSTRAINT FK_VNPG_ORRT
    FOREIGN KEY (VNPGORRT) REFERENCES RHH.RDPG (RDPGCDGO);
ALTER TABLE RHH.VNPG ADD CONSTRAINT FK_VNPG_ORPG
    FOREIGN KEY (VNPGORPG) REFERENCES RHH.RDPG (RDPGCDGO);
ALTER TABLE RHH.VNPG ADD CONSTRAINT FK_VNPG_LQDC
    FOREIGN KEY (VNPGLQDC) REFERENCES RHH.LQDC (LQDCCDGO);

-- Un solo registro VIVO por (empleado, periodo). NO es un UNIQUE simple: un
-- ANULADO(4) no debe bloquear registrar otro, y un PAGADO(3) o FINIQUITADO(5)
-- tampoco. Indice funcional que solo indexa los vivos: REGISTRADO(1) y
-- RETENIDO(2). Los demas entran como NULL y Oracle no los considera.
-- (Las dos columnas son NOT NULL, asi que aca no hace falta NVL. Y la consulta
-- JPQL que replica esta regla se arma por rama, nunca con nvl: no es JPQL.)
CREATE UNIQUE INDEX UQ_VNPG_VIVO ON RHH.VNPG (
    CASE WHEN VNPGESTD IN (1, 2) THEN MPLDCDGO END,
    CASE WHEN VNPGESTD IN (1, 2) THEN VNPGPRNM END
);

-- Lo que consultan el motor y la orden de pago: por empleado y periodo, y por
-- empresa/periodo/estado para el listado de la pantalla.
CREATE INDEX IX_VNPG_MPLD_PRNM ON RHH.VNPG (MPLDCDGO, VNPGPRNM, VNPGESTD);
CREATE INDEX IX_VNPG_EMPR_PRNM ON RHH.VNPG (PJRQCDGO, VNPGPRNM, VNPGESTD);

COMMENT ON TABLE  RHH.VNPG          IS 'Valor no pagado a un empleado en un periodo, que se retiene del pago de ese mes y se devuelve en el pago del siguiente. No afecta el devengo ni las provisiones: solo mueve remuneraciones por pagar';
COMMENT ON COLUMN RHH.VNPG.VNPGPRNM IS 'Periodo en que NO se paga (se resta de la orden de pago)';
COMMENT ON COLUMN RHH.VNPG.VNPGPRRC IS 'Periodo en que se devolvio (se suma a la orden de pago). Se llena al confirmar ese pago';
COMMENT ON COLUMN RHH.VNPG.VNPGESTD IS 'Codigo alterno del rubro 311 RHH_ESTADO_VALOR_NO_PAGADO: 1 REGISTRADO, 2 RETENIDO, 3 PAGADO, 4 ANULADO, 5 FINIQUITADO';
COMMENT ON COLUMN RHH.VNPG.VNPGORRT IS 'Orden de pago que lo retuvo. Se escribe al generar la orden del periodo VNPGPRNM';
COMMENT ON COLUMN RHH.VNPG.VNPGORPG IS 'Orden de pago que lo devolvio. Se escribe al confirmar el pago del periodo siguiente';
COMMENT ON COLUMN RHH.VNPG.VNPGLQDC IS 'Liquidacion de haberes que lo absorbio cuando el empleado salio antes de cobrarlo';


-- =====================================================================
-- BLOQUE 3 — RUBRO 311 Y SUS 5 DETALLES (PDTR 1504-1508)
-- =====================================================================
-- Reservado en REGISTRO-RESERVAS-EQUIPOS.md el 2026-09-08. Segundo rubro del
-- bloque 310-329 / 1500-1599 de este equipo. PRBRALTR = PRBRCDGO = 311 (§6).
INSERT INTO SCP.PRBR (PRBRCDGO, PRBRDSCR, PRBRFCHA, PRBRALTR, PRBRTPOO)
VALUES (311, 'RHH ESTADO DE VALOR NO PAGADO', SYSDATE, 311, 1);

INSERT INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRALTR, PDTRESTD) VALUES (1504, 311, 'REGISTRADO',  1, 1);
INSERT INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRALTR, PDTRESTD) VALUES (1505, 311, 'RETENIDO',    2, 1);
INSERT INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRALTR, PDTRESTD) VALUES (1506, 311, 'PAGADO',      3, 1);
INSERT INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRALTR, PDTRESTD) VALUES (1507, 311, 'ANULADO',     4, 1);
INSERT INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRALTR, PDTRESTD) VALUES (1508, 311, 'FINIQUITADO', 5, 1);


-- =====================================================================
-- BLOQUE 4 — DETALLES 34 Y 35 DEL RUBRO 221 (PDTR 1509-1510)
-- =====================================================================
-- Los roles nuevos del motor: RhhRolConceptoMotor.VALOR_NO_PAGADO_RETENIDO = 34
-- y VALOR_NO_PAGADO_RECUPERADO = 35. Con codigos fijos del bloque de este
-- equipo, no con una secuencia: determinista, y el 0.4b ya verifico que estan
-- libres. El rubro 221 se ubica por su ALTERNO (la PK es otra).
INSERT INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRALTR, PDTRESTD)
SELECT 1509, r.PRBRCDGO, 'VALOR NO PAGADO RETENIDO', 34, 1
  FROM SCP.PRBR r WHERE r.PRBRALTR = 221;
INSERT INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRALTR, PDTRESTD)
SELECT 1510, r.PRBRCDGO, 'VALOR NO PAGADO RECUPERADO', 35, 1
  FROM SCP.PRBR r WHERE r.PRBRALTR = 221;


-- =====================================================================
-- BLOQUE 5 — LOS DOS CONCEPTOS, uno por empresa que corre nomina
-- =====================================================================
-- ⛔ FROM (SELECT DISTINCT PJRQCDGO FROM RHH.CPNM), NUNCA FROM SCP.PJRQ: esa
--    tabla son las personas juridicas (786 filas), no las empresas.
-- CPNMTPCN = 5 (INFORMATIVO) es lo que impide que entren al neto.
-- CPNMESTD = 1 es obligatorio: selectByRolMotor filtra por estado = 1.
-- CPNMCDGO no se pasa: es IDENTITY (verificado en e2-18, bloque 0.5).
-- Los NOT EXISTS hacen que re-correr esto no duplique.
INSERT INTO RHH.CPNM (PJRQCDGO, CPNMNMBR, CPNMABRV, CPNMALTR, CPNMTPCN, CPNMROLM, CPNMESTD)
SELECT emp.PJRQCDGO,
       'Valor no pagado',
       'VNPGRT',
       NVL((SELECT MAX(c.CPNMALTR) FROM RHH.CPNM c WHERE c.PJRQCDGO = emp.PJRQCDGO), 0) + 1,
       5,    -- INFORMATIVO
       34,   -- RhhRolConceptoMotor.VALOR_NO_PAGADO_RETENIDO
       1
  FROM (SELECT DISTINCT PJRQCDGO FROM RHH.CPNM) emp
 WHERE NOT EXISTS (SELECT 1 FROM RHH.CPNM c2
                    WHERE c2.PJRQCDGO = emp.PJRQCDGO AND c2.CPNMROLM = 34);

INSERT INTO RHH.CPNM (PJRQCDGO, CPNMNMBR, CPNMABRV, CPNMALTR, CPNMTPCN, CPNMROLM, CPNMESTD)
SELECT emp.PJRQCDGO,
       'Valor no pagado del mes anterior',
       'VNPGRC',
       NVL((SELECT MAX(c.CPNMALTR) FROM RHH.CPNM c WHERE c.PJRQCDGO = emp.PJRQCDGO), 0) + 1,
       5,    -- INFORMATIVO
       35,   -- RhhRolConceptoMotor.VALOR_NO_PAGADO_RECUPERADO
       1
  FROM (SELECT DISTINCT PJRQCDGO FROM RHH.CPNM) emp
 WHERE NOT EXISTS (SELECT 1 FROM RHH.CPNM c2
                    WHERE c2.PJRQCDGO = emp.PJRQCDGO AND c2.CPNMROLM = 35);

COMMIT;


-- =====================================================================
-- BLOQUE 6 — CONTROLES DESPUES. Correr y LEER.
-- =====================================================================

-- 6.1 La tabla existe con sus 16 columnas. ESPERADO: 16.
SELECT '6.1 - columnas de VNPG' AS control, COUNT(*) AS debe_ser_16
  FROM all_tab_columns WHERE owner = 'RHH' AND table_name = 'VNPG';

-- 6.2 Las 7 FK estan creadas. ESPERADO: 7. Si son 6, la de PJRQ fallo por el
--     GRANT (ORA-01031): correr el GRANT como dueño de SCP y repetir ese ALTER.
SELECT '6.2 - FKs de VNPG' AS control, COUNT(*) AS debe_ser_7
  FROM all_constraints WHERE owner = 'RHH' AND table_name = 'VNPG' AND constraint_type = 'R';

-- 6.3 El rubro 311 y sus 5 detalles. ESPERADO: 5 filas, alternos 1..5.
SELECT '6.3 - estados del rubro 311' AS control, d.PDTRCDGO, d.PDTRALTR, d.PDTRDSCR
  FROM SCP.PDTR d JOIN SCP.PRBR r ON r.PRBRCDGO = d.PRBRCDGO
 WHERE r.PRBRALTR = 311 ORDER BY d.PDTRALTR;

-- 6.4 Los detalles 34 y 35 del rubro 221. ESPERADO: 2 filas.
SELECT '6.4 - roles 34/35 del rubro 221' AS control, d.PDTRCDGO, d.PDTRALTR, d.PDTRDSCR
  FROM SCP.PDTR d JOIN SCP.PRBR r ON r.PRBRCDGO = d.PRBRCDGO
 WHERE r.PRBRALTR = 221 AND d.PDTRALTR IN (34, 35) ORDER BY d.PDTRALTR;


-- =====================================================================
-- BLOQUE 7 — 🔴 EL CONTROL QUE EVITA EL DOBLE DESCUENTO
-- =====================================================================
-- ESPERADO: DOS filas por empresa del 0.6, con tipo_debe_ser_5 = 5 y
-- estado_debe_ser_1 = 1 en TODAS.
-- 🔴 Si alguna trae tipo distinto de 5, PARAR Y AVISAR antes del WAR: ese
--    concepto entraria al neto y el valor se descontaria dos veces.
SELECT '7 - conceptos creados' AS control,
       c.CPNMCDGO, c.PJRQCDGO, c.CPNMNMBR, c.CPNMALTR,
       c.CPNMTPCN AS tipo_debe_ser_5, c.CPNMROLM AS rol, c.CPNMESTD AS estado_debe_ser_1
  FROM RHH.CPNM c WHERE c.CPNMROLM IN (34, 35)
 ORDER BY c.PJRQCDGO, c.CPNMROLM;


-- =====================================================================
-- BLOQUE 8 — REVERSO. COMENTADO A PROPOSITO.
-- =====================================================================
-- Control antes de borrar (lectura): tiene que dar 0 en los dos.
--   SELECT COUNT(*) FROM RHH.VNPG;
--   SELECT COUNT(*) FROM RHH.NVNM n JOIN RHH.CPNM c ON c.CPNMCDGO = n.CPNMCDGO
--    WHERE c.CPNMROLM IN (34, 35);
--
-- DELETE FROM RHH.CPNM WHERE CPNMROLM IN (34, 35);
-- DELETE FROM SCP.PDTR WHERE PDTRCDGO BETWEEN 1504 AND 1510;
-- DELETE FROM SCP.PRBR WHERE PRBRCDGO = 311;
-- DROP TABLE RHH.VNPG;
-- DROP SEQUENCE RHH.SQ_VNPGCDGO;
-- COMMIT;

-- =====================================================================
-- FIN
-- =====================================================================
