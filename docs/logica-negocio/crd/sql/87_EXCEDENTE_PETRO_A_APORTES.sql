-- =====================================================================================
-- EXCEDENTE DE LA CARGA PETRO A UN APORTE — modelo y catalogo
-- FECHA: 2026-08-31
--
-- REQUERIMIENTO (usuario, 2026-08-30): cuando la carga Petro trae mas dinero del esperado,
-- la pantalla hoy solo permite aplicar el excedente a otro prestamo. Debe permitir tambien
-- enviarlo a un aporte de jubilacion o cesantia.
--
-- Es la opcion ③ del §3.7 del levantamiento contable ("se aplica a cuenta individual"),
-- levantada con contabilidad y nunca construida. Ver
-- docs/logica-negocio/petro/PLAN-EXCEDENTE-PETRO-A-APORTES.md
-- =====================================================================================


-- =====================================================================================
-- 0. CONTROLES PREVIOS
-- =====================================================================================

-- 0.1 CRD.AVPC existe. Esperado: 1 fila.
SELECT t.OWNER, t.TABLE_NAME FROM ALL_TABLES t
WHERE  t.OWNER = 'CRD' AND t.TABLE_NAME = 'AVPC';

-- 0.2 La columna nueva NO debe existir. Esperado: 0 filas.
SELECT c.COLUMN_NAME FROM ALL_TAB_COLUMNS c
WHERE  c.OWNER = 'CRD' AND c.TABLE_NAME = 'AVPC' AND c.COLUMN_NAME = 'TPAPCDGO';

-- 0.3 ⚠️ TODA fila existente de AVPC tiene que ser de prestamo, o el CHECK del bloque 1 no
--     va a poder crearse. Esperado: 0 filas problematicas.
SELECT COUNT(*) AS FILAS_SIN_PRESTAMO_O_CUOTA
FROM   CRD.AVPC a
WHERE  a.PRSTCDGO IS NULL OR a.DTPRCDGO IS NULL;

-- 0.4 El rubro 235 y sus detalles. Esperado: 6 filas (alternos 1..6). El alterno 7 esta
--     RESERVADO para JUBILACION por el script 81, que todavia no se corrio — por eso el
--     nuevo va en el 8, no en el 7.
SELECT  d.PDTRCDGO, d.PDTRALTR, d.PDTRDSCR, d.PDTRVLRV, d.PDTRESTD
FROM    SCP.PDTR d
JOIN    SCP.PRBR r ON r.PRBRCDGO = d.PRBRCDGO
WHERE   r.PRBRALTR = 235
ORDER   BY d.PDTRALTR;

-- 0.5 El PDTRCDGO 1180 debe estar libre (1178 = JUBILACION del script 81, 1179 = COBRO_MIXTO
--     del script 83, ya corrido). Esperado: 0 filas.
SELECT PDTRCDGO FROM SCP.PDTR WHERE PDTRCDGO = 1180;


-- =====================================================================================
-- 1. CRD.AVPC — el destino puede ser un aporte
-- =====================================================================================
-- Una fila de afectacion es O de prestamo O de aporte, nunca las dos.
--   - De prestamo: PRSTCDGO + DTPRCDGO presentes, TPAPCDGO NULL, con su desglose
--     (AVPCCPAF/AVPCINAF/AVPCDGAF).
--   - De aporte:   TPAPCDGO presente, PRSTCDGO/DTPRCDGO NULL, SIN desglose — un aporte no
--     tiene capital ni interes. Los campos de desglose quedan en NULL: no se fuerzan a 0,
--     NULL se lee como "no aplica" y 0 se lee como "aplica y vale cero".

ALTER TABLE CRD.AVPC ADD (TPAPCDGO NUMBER);

ALTER TABLE CRD.AVPC ADD CONSTRAINT FK_AVPC_TPAP
    FOREIGN KEY (TPAPCDGO) REFERENCES CRD.TPAP(TPAPCDGO);

ALTER TABLE CRD.AVPC ADD CONSTRAINT CK_AVPC_PRST_XOR_TPAP CHECK (
    (PRSTCDGO IS NOT NULL AND DTPRCDGO IS NOT NULL AND TPAPCDGO IS NULL)
    OR
    (PRSTCDGO IS NULL     AND DTPRCDGO IS NULL     AND TPAPCDGO IS NOT NULL)
);

CREATE INDEX CRD.IDX_AVPC_TIPOAPORTE ON CRD.AVPC (TPAPCDGO);

COMMENT ON COLUMN CRD.AVPC.TPAPCDGO IS
    'Tipo de aporte destino cuando el excedente NO va a un prestamo. Excluyente con PRSTCDGO/DTPRCDGO (CK_AVPC_PRST_XOR_TPAP). Que tipos puede recibir un participe lo decide VigenciaContratoService.esperadoPorEntidad — la MISMA regla que decide a quien se incluye en el archivo.';


-- =====================================================================================
-- 2. RUBRO 235 — tipo de movimiento del aporte generado por un excedente
-- =====================================================================================
-- ⚠️ VA EN EL ALTERNO 8, NO EN EL 7. El 7 esta reservado para JUBILACION por el script 81
-- (PDTR 1178), que todavia no se corrio pero ya esta anotado en
-- docs/logica-negocio/REGISTRO-RESERVAS-EQUIPOS.md. Usar el 7 aca haria que uno de los dos
-- scripts falle por PK/alterno duplicado el dia que se corran los dos.
--
-- Ninguno de los 6 existentes sirve: APORTE_MENSUAL es el descuento normal de rol,
-- AJUSTE_MANUAL es ventanilla sin carga. Sin un tipo propio, un aporte nacido de un
-- excedente seria indistinguible de uno cargado a mano por un cajero.

INSERT INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD)
SELECT 1180, r.PRBRCDGO, 'EXCEDENTE PETRO', 8, 'EXCEDENTE_PETRO', 8, 1
FROM   SCP.PRBR r
WHERE  r.PRBRALTR = 235
AND    NOT EXISTS (
         SELECT 1 FROM SCP.PDTR x
         WHERE  x.PRBRCDGO = r.PRBRCDGO AND x.PDTRALTR = 8
       );

COMMIT;


-- =====================================================================================
-- 3. SINCRONIZAR LA SECUENCIA
-- =====================================================================================
-- Correr la consulta y ejecutar el ALTER SOLO si la secuencia quedo en 1180 o por debajo.

SELECT  s.SEQUENCE_NAME, s.LAST_NUMBER AS SIGUIENTE, 1180 AS PK_USADO
FROM    ALL_SEQUENCES s
WHERE   s.SEQUENCE_OWNER = 'SCP' AND s.SEQUENCE_NAME = 'SQ_PDTRCDGO';

-- ALTER SEQUENCE SCP.SQ_PDTRCDGO RESTART START WITH 1181;


-- =====================================================================================
-- 4. CONTROLES POSTERIORES
-- =====================================================================================

-- 4.1 La columna, la FK, el CHECK y el indice. Esperado: 1 fila cada uno, ENABLED/VALID.
SELECT c.COLUMN_NAME, c.DATA_TYPE, c.NULLABLE FROM ALL_TAB_COLUMNS c
WHERE  c.OWNER = 'CRD' AND c.TABLE_NAME = 'AVPC' AND c.COLUMN_NAME = 'TPAPCDGO';

SELECT c.CONSTRAINT_NAME, c.CONSTRAINT_TYPE, c.STATUS, c.SEARCH_CONDITION
FROM   ALL_CONSTRAINTS c
WHERE  c.OWNER = 'CRD' AND c.TABLE_NAME = 'AVPC'
AND    c.CONSTRAINT_NAME IN ('FK_AVPC_TPAP', 'CK_AVPC_PRST_XOR_TPAP');

-- 4.2 El rubro 235 con su detalle nuevo. Esperado: 7 filas si el 81 no se corrio (1..6 + 8),
--     u 8 filas si ya se corrio (1..8).
SELECT  d.PDTRCDGO, d.PDTRALTR, d.PDTRDSCR, d.PDTRVLRV, d.PDTRESTD
FROM    SCP.PDTR d
JOIN    SCP.PRBR r ON r.PRBRCDGO = d.PRBRCDGO
WHERE   r.PRBRALTR = 235
ORDER   BY d.PDTRALTR;
