-- =====================================================================================
-- LO QUE HAY QUE CORRER EN PRODUCCION â€” 2026-08-31
--
-- âš ï¸ TODO LO DE ESTE ARCHIVO YA SE EJECUTO Y SE VERIFICO EN LA BASE LOCAL.
-- Corrio el arbitro (`saabe-25`) con permiso puntual del usuario. **En produccion NO se
-- corrio nada de esto.** Este archivo es lo que falta ejecutar alla, en este orden.
--
-- Son dos cosas independientes:
--   BLOQUE 1 â€” un UPDATE urgente que CORRIGE UN ESTADO INCORRECTO que ya esta en produccion.
--   BLOQUE 2 â€” el DDL de CRD.PGPC y su catalogo (equivale al script 97 completo).
--
-- Cada bloque trae sus propios controles. Si un control no da lo esperado, PARAR.
-- =====================================================================================


-- =====================================================================================
-- BLOQUE 1 â€” â›” URGENTE: apagar TPAPPRDP
-- =====================================================================================
--
-- QUE PASO. El 2026-08-31 se corrio en produccion el script 95, que carga
-- CRD.TPAP.TPAPPRDP. Ese script decia que iba "despues del WAR". Era INCOMPLETO: tambien
-- tiene que ir despues de ENCENDER EL FLAG de contabilidad de CRD (rubro 237), que hoy
-- sigue en 0.
--
-- POR QUE IMPORTA. Los dos lados del asiento de la devolucion de aportes tienen gates
-- INDEPENDIENTES:
--   - La reclasificacion de CRD  -> DevolucionAporteServiceImpl:1040, gate contabilidadActiva()
--                                   = rubro 237 = 0  -> NO genera asiento.
--   - El asiento de pago de CXP  -> DevolucionAporteServiceImpl:320, gate
--                                   `contabiliza = tiposSinProducto.isEmpty()`
--                                   = true desde el script 95  -> SI genera asiento.
--
-- O sea: hoy, en produccion, una devolucion de aportes genera el asiento del PAGO
-- (D 2.3.01.xx -> H Banco) SIN que CRD haya reconocido la obligacion. La cuenta de
-- liquidacion queda debitada contra nada.
--
-- âš ï¸ Y los dos asientos, por separado, CUADRAN. Nada lo detecta.
--
-- ESTO NO BORRA NADA. Los 5 grupos y los 5 productos de pago que creo el script 95 quedan
-- creados y listos. Solo se apaga el mapeo: `contabiliza` vuelve a dar false y CXP deja de
-- generar asiento, exactamente como estaba antes del 95. El dia que se encienda el rubro
-- 237, se vuelve a correr el BLOQUE 3 del script 95 y las dos mitades arrancan juntas.

-- 1.1 CONTROL PREVIO. Cuantos tipos tienen producto hoy. Esperado: 11.
--     Si da 0, el 95 no se corrio en produccion y este bloque NO hace falta.
SELECT COUNT(*) AS TIPOS_CON_PRODUCTO FROM CRD.TPAP WHERE TPAPPRDP IS NOT NULL;

-- 1.2 CONTROL PREVIO. El flag de contabilidad de CRD. Esperado: 0 (apagado).
--     Si diera 1, este bloque NO se corre: el flag esta encendido y las dos mitades
--     funcionan. PARAR y avisar al arbitro.
SELECT d.PDTRVLRN AS FLAG_CONTABILIDAD_CRD
FROM   SCP.PDTR d
JOIN   SCP.PRBR r ON r.PRBRCDGO = d.PRBRCDGO
WHERE  r.PRBRALTR = 237 AND d.PDTRALTR = 1;

-- 1.3 EL UPDATE.
UPDATE CRD.TPAP SET TPAPPRDP = NULL
WHERE  TPAPCDGO IN (9,11,12,13,14,15,16,21,22,23,24);

COMMIT;

-- 1.4 CONTROL POSTERIOR. Esperado: 0.
SELECT COUNT(*) AS TIPOS_CON_PRODUCTO FROM CRD.TPAP WHERE TPAPPRDP IS NOT NULL;

-- 1.5 CONTROL POSTERIOR. Los 5 productos siguen existiendo (NO se borraron). Esperado: 5.
SELECT p.ID, p.NOMBRE, p.CODIGO
FROM   PGS.PRDP p
WHERE  p.NOMBRE LIKE 'DEVOLUCION %'
ORDER  BY p.NOMBRE;


-- =====================================================================================
-- BLOQUE 2 â€” CRD.PGPC + catalogo PAGO_PENSION (= script 97 completo)
-- =====================================================================================
--
-- Es el script `97_PAGO_PENSION_COMPLEMENTARIA.sql` tal cual. Se replica aca para que este
-- archivo sea autosuficiente, pero **el 97 es la fuente**: si divergen, gana el 97.
--
-- âš ï¸ NO desplegar el WAR con el frente de jubilados sin correr esto antes. La entidad
-- PagoPensionComplementaria mapea 19 columnas de CRD.PGPC; si la tabla no existe, cualquier
-- lectura de esa entidad revienta.
--
-- âš ï¸ LA PK ES IDENTITY, NO SECUENCIA (la entidad declara GenerationType.IDENTITY).
--
-- VERIFICADO EN LOCAL el 2026-08-31: tabla creada, 6 constraints ENABLED/VALIDATED, 4
-- indices VALID con OWNER = CRD, 19 columnas, IDENTITY_COLUMN = YES, y el PDTR 1200
-- insertado (rubro 235 alterno 9 = PAGO_PENSION).

-- 2.1 CONTROLES PREVIOS.
SELECT t.OWNER, t.TABLE_NAME FROM ALL_TABLES t WHERE t.TABLE_NAME = 'PGPC';           -- esperado: 0 filas
SELECT t.OWNER, t.TABLE_NAME FROM ALL_TABLES t
WHERE  t.OWNER = 'CRD' AND t.TABLE_NAME IN ('ENTD','FLLL') ORDER BY t.TABLE_NAME;      -- esperado: 2 filas
SELECT MAX(PDTRCDGO) AS MAX_PDTR FROM SCP.PDTR;                                        -- esperado: 1180

-- 2.2 LA TABLA.
CREATE TABLE CRD.PGPC (
    PGPCCDGO NUMBER GENERATED ALWAYS AS IDENTITY,
    ENTDCDGO NUMBER          NOT NULL,
    FLLLCDGO NUMBER,
    PGPCANNO NUMBER          NOT NULL,
    PGPCMESS NUMBER          NOT NULL,
    PGPCVLPN NUMBER(18,2),
    PGPCVLSG NUMBER(18,2),
    PGPCVLRR NUMBER(18,2)    NOT NULL,
    PGPCFCHA DATE,
    PGPCESTD NUMBER          NOT NULL,
    PGPCIDPG NUMBER,
    PGPCIDAP NUMBER,
    PGPCNMAS NUMBER,
    PGPCUSRG VARCHAR2(50),
    PGPCFCRG TIMESTAMP,
    PGPCFCPG DATE,
    PGPCUSAN VARCHAR2(50),
    PGPCFCAN TIMESTAMP,
    PGPCMTAN VARCHAR2(500)
);

ALTER TABLE CRD.PGPC ADD CONSTRAINT PK_PGPC PRIMARY KEY (PGPCCDGO);
ALTER TABLE CRD.PGPC ADD CONSTRAINT FK_PGPC_ENTD
    FOREIGN KEY (ENTDCDGO) REFERENCES CRD.ENTD(ENTDCDGO);
ALTER TABLE CRD.PGPC ADD CONSTRAINT FK_PGPC_FLLL
    FOREIGN KEY (FLLLCDGO) REFERENCES CRD.FLLL(FLLLCDGO);
ALTER TABLE CRD.PGPC ADD CONSTRAINT UK_PGPC_ENTD_PERIODO
    UNIQUE (ENTDCDGO, PGPCANNO, PGPCMESS);
ALTER TABLE CRD.PGPC ADD CONSTRAINT CK_PGPC_MESS CHECK (PGPCMESS BETWEEN 1 AND 12);
ALTER TABLE CRD.PGPC ADD CONSTRAINT CK_PGPC_VLRR CHECK (PGPCVLRR > 0);

CREATE INDEX CRD.IDX_PGPC_PERIODO ON CRD.PGPC (PGPCANNO, PGPCMESS);
CREATE INDEX CRD.IDX_PGPC_ESTADO  ON CRD.PGPC (PGPCESTD);

COMMENT ON TABLE  CRD.PGPC          IS 'Bitacora del pago mensual de pension complementaria a un jubilado. Una fila por jubilado y periodo. El UNIQUE (ENTDCDGO, ANNO, MESS) es lo que hace idempotente al proceso de generacion: correrlo dos veces no puede duplicar el pago, aunque el chequeo en Java se saltee.';
COMMENT ON COLUMN CRD.PGPC.PGPCVLPN IS 'Valor de la pension del periodo, de CRD.VPPC.';
COMMENT ON COLUMN CRD.PGPC.PGPCVLSG IS 'Valor del seguro del periodo, de CRD.VPPC.';
COMMENT ON COLUMN CRD.PGPC.PGPCVLRR IS 'Total pagado = VLPN + VLSG. Se guarda calculado a proposito: es el monto que efectivamente se mando a pagar, y tiene que sobrevivir a un cambio posterior de la configuracion en VPPC.';
COMMENT ON COLUMN CRD.PGPC.PGPCIDPG IS 'Orden de pago en CXP (PGS.PGTR). Sin FK, mismo criterio que CRD.DVAP.DVAPIDPG.';
COMMENT ON COLUMN CRD.PGPC.PGPCIDAP IS 'Aporte NEGATIVO generado en CRD.APRT por este pago. Sin FK, mismo criterio.';
COMMENT ON COLUMN CRD.PGPC.PGPCNMAS IS 'Asiento (ASNTCDGO, la PK, no el correlativo ASNTNMRO). Hoy lo puebla el reconciliador con el asiento que genera CXP al confirmar el pago; CRD no genera uno propio para el pago mensual.';

-- 2.3 EL CATALOGO â€” rubro 235 alterno 9, PDTR 1200 (rango del equipo A).
INSERT INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD)
SELECT 1200, r.PRBRCDGO, 'PAGO PENSION', 9, 'PAGO_PENSION', 9, 1
FROM   SCP.PRBR r
WHERE  r.PRBRALTR = 235
AND    NOT EXISTS (
         SELECT 1 FROM SCP.PDTR x
         WHERE  x.PRBRCDGO = r.PRBRCDGO AND x.PDTRALTR = 9
       );

COMMIT;

-- 2.4 CONTROLES POSTERIORES.
SELECT c.COLUMN_NAME, c.DATA_TYPE, c.NULLABLE FROM ALL_TAB_COLUMNS c
WHERE  c.OWNER = 'CRD' AND c.TABLE_NAME = 'PGPC' ORDER BY c.COLUMN_ID;                  -- esperado: 19 filas

SELECT c.COLUMN_NAME, c.IDENTITY_COLUMN FROM ALL_TAB_COLUMNS c
WHERE  c.OWNER = 'CRD' AND c.TABLE_NAME = 'PGPC' AND c.COLUMN_NAME = 'PGPCCDGO';        -- esperado: YES

SELECT c.CONSTRAINT_NAME, c.CONSTRAINT_TYPE, c.STATUS, c.VALIDATED FROM ALL_CONSTRAINTS c
WHERE  c.OWNER = 'CRD' AND c.TABLE_NAME = 'PGPC'
ORDER  BY c.CONSTRAINT_TYPE;                                                            -- esperado: 6, ENABLED/VALIDATED

SELECT i.OWNER, i.INDEX_NAME, i.STATUS FROM ALL_INDEXES i
WHERE  i.TABLE_OWNER = 'CRD' AND i.TABLE_NAME = 'PGPC' ORDER BY i.INDEX_NAME;            -- esperado: 4, OWNER=CRD, VALID

SELECT d.PDTRCDGO, d.PDTRALTR, d.PDTRDSCR, d.PDTRVLRV FROM SCP.PDTR d
JOIN   SCP.PRBR r ON r.PRBRCDGO = d.PRBRCDGO
WHERE  r.PRBRALTR = 235 ORDER BY d.PDTRALTR;                                             -- esperado: 9 filas, la 9 = PAGO PENSION


-- =====================================================================================
-- 3. REVERSO â€” comentado a proposito.
-- =====================================================================================
-- Del BLOQUE 1: volver a encender el mapeo es correr el BLOQUE 3 del script 95, y SOLO
-- despues de encender el rubro 237.
--
-- Del BLOQUE 2, solo si el WAR con jubilados NO se desplego:
-- DELETE FROM SCP.PDTR WHERE PDTRCDGO = 1200;
-- DROP TABLE CRD.PGPC CASCADE CONSTRAINTS;
-- COMMIT;
-- =====================================================================================

