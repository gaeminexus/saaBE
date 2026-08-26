-- ============================================================================
-- 67 - CREA EL PERIODO DE AGOSTO DE 2026, CONFIGURADO PARA CONTABILIZAR
-- ==
-- ⚠ ESTE SCRIPT ESCRIBE. Crea UNA fila en RHH.PRDN.
-- ==
-- Se corre en PRODUCCION, DESPUES del sql/66 y con sus cinco bloques en verde.
-- Verificado el 2026-08-25: CFNM completa, las tres plantillas activas con sus
-- 32 lineas, todas las cuentas existen y ninguna linea quedo con la marcadora.
-- ==
-- POR QUE POR SCRIPT Y NO POR PANTALLA: agosto lo va a operar el cliente, y el
-- periodo tiene que quedar con PRDNMODO = 2 con total seguridad. La pantalla
-- deja campos en nulo sin avisar --defecto D15-- y aqui un nulo no se nota:
--
--     esHistorico(periodo) -> true cuando PRDNMODO es NULO
--
-- Un periodo sin modo NO CONTABILIZA y NO DA ERROR. El cliente calcularia,
-- pulsaria Contabilizar, los asientos se quedarian en nulo, y nada se lo diria.
-- Por eso se crea aqui y se LEE DE VUELTA.
-- ==
-- VALORES, y de donde salen:
--   PRDNANOO/PRDNMSEE  2026 / 8
--   PRDNFCHI/PRDNFCHF  01-08-2026 al 31-08-2026
--   PRDNFCCN           31-08-2026. FECHA CONTABLE PROVISIONAL, decidida por
--                      Mike el 2026-08-25: fin de mes. Es la fecha con la que
--                      se emitiran los asientos, y PUEDE CAMBIAR: si se cambia,
--                      se cambia ANTES de contabilizar, nunca despues.
--   PRDNESTD           1 ABIERTO
--   PRDNMODO           2 PRODUCTIVO_CONTABILIZA
--   PRDNTPNM           1 MENSUAL
--   PJRQCDGO           1236
-- ============================================================================


-- ============================================================================
-- BLOQUE 0 - QUE COLUMNAS EXIGE LA BASE, antes de escribir el INSERT a ciegas.
-- ==
-- La entidad Java no declara ningun nullable=false, pero la entidad no es la
-- base: esa premisa es la que produjo el ORA-02290 de CNTEESTD y por la que
-- existe REFERENCIA-CHECKS-RHH.md. Si esta consulta devuelve alguna columna
-- que el INSERT del bloque 2 no informa, PARAR y avisar.
-- ============================================================================
SELECT COLUMN_NAME, DATA_TYPE, NULLABLE, DATA_DEFAULT
  FROM ALL_TAB_COLUMNS
 WHERE OWNER = 'RHH' AND TABLE_NAME = 'PRDN' AND NULLABLE = 'N'
 ORDER BY COLUMN_ID;

-- ============================================================================
-- BLOQUE 1 - CONTROL ANTES. ESPERADO: CERO FILAS.
-- Si agosto ya existe, PARAR: hay que mirar quien lo creo y con que modo antes
-- de crear un duplicado.
-- ============================================================================
SELECT PRDNCDGO, PRDNANOO, PRDNMSEE, PRDNESTD, PRDNMODO
  FROM RHH.PRDN
 WHERE PRDNANOO = 2026 AND PRDNMSEE = 8;

-- ============================================================================
-- BLOQUE 2 - LA CREACION.
-- PRDNCDGO es IDENTITY: lo asigna Oracle, no se informa.
-- El WHERE NOT EXISTS hace el script idempotente: correrlo dos veces no crea
-- un segundo agosto.
-- ============================================================================
INSERT INTO RHH.PRDN (PRDNANOO, PRDNMSEE, PRDNFCHI, PRDNFCHF, PRDNFCCN,
                      PRDNESTD, PRDNMODO, PRDNTPNM, PJRQCDGO,
                      PRDNFCHR, PRDNUSRR, PRDNOBSR)
SELECT 2026, 8,
       DATE '2026-08-01', DATE '2026-08-31', DATE '2026-08-31',
       1, 2, 1, 1236,
       SYSDATE, 'SOPORTE',
       'Periodo preparado por soporte. Modo PRODUCTIVO: genera contabilidad.'
  FROM DUAL
 WHERE NOT EXISTS (SELECT 1 FROM RHH.PRDN
                    WHERE PRDNANOO = 2026 AND PRDNMSEE = 8);
-- ESPERADO: 1 fila insertada.

COMMIT;

-- ============================================================================
-- BLOQUE 3 - CONTROL DESPUES, Y ES EL QUE IMPORTA.
-- ==
-- ESPERADO: UNA fila de agosto, con
--   ESTADO 1 . MODO 2 . TIPO 1 . del 01-08 al 31-08 . contable 31-08
--   los tres asientos en NULO y los totales en NULO
-- ==
-- LA COLUMNA MODO TIENE QUE DECIR 2. No basta con haberlo escrito: se lee de
-- vuelta, porque un modo nulo o en 1 hace que agosto NO contabilice y no avise.
-- ============================================================================
SELECT PRDNCDGO AS PRDN, PRDNANOO AS ANIO, PRDNMSEE AS MES,
       PRDNESTD AS ESTADO, PRDNMODO AS MODO, PRDNTPNM AS TIPO,
       PRDNFCHI AS DESDE, PRDNFCHF AS HASTA, PRDNFCCN AS FECHA_CONTABLE,
       PJRQCDGO AS EMPRESA,
       PRDNASNT AS ASIENTO_ROL, PRDNASPR AS ASIENTO_PROV, PRDNASPG AS ASIENTO_PAGO,
       PRDNTTIN AS INGRESOS, PRDNTTDS AS DESCUENTOS, PRDNTTNT AS NETO,
       PRDNOBSR AS OBSERVACION
  FROM RHH.PRDN
 WHERE PRDNANOO = 2026 AND PRDNMSEE = 8;

-- ============================================================================
-- BLOQUE 4 - LOS OCHO PERIODOS DEL ANO, PARA VER AGOSTO EN CONTEXTO.
-- ==
-- ESPERADO: enero a julio en estado 7 y MODO 1, agosto en estado 1 y MODO 2.
-- Agosto es el UNICO con modo 2, y esa es la diferencia que hay que ver de un
-- vistazo: es el primer mes que va a generar asientos de verdad.
-- ============================================================================
SELECT PRDNCDGO AS PRDN, PRDNMSEE AS MES, PRDNESTD AS ESTADO, PRDNMODO AS MODO,
       PRDNFCCN AS FECHA_CONTABLE,
       CASE WHEN PRDNMODO = 2 THEN 'CONTABILIZA' ELSE 'historico' END AS QUE_HACE
  FROM RHH.PRDN
 WHERE PRDNANOO = 2026
 ORDER BY PRDNMSEE;
