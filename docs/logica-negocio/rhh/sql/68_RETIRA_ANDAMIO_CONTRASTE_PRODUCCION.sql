-- ============================================================================
-- 68 - RETIRA EL ANDAMIO DEL CONTRASTE, SOLO EN PRODUCCION
-- ==
-- ⚠ ESTE SCRIPT BORRA. Y a diferencia de todo lo demas que hemos corrido hoy,
-- lo que borra NO SE PUEDE VOLVER A GENERAR: RHH.CTRL contiene los roles
-- pagados y las planillas del IESS transcritos a mano desde los libros del
-- cliente. Si se pierden, se pierde el patron contra el que se validaron los
-- siete meses.
-- ==
-- POR ESO EL PASO 1 NO ES OPCIONAL: respalda a una tabla antes de borrar.
-- ==
-- ############ NO CORRER ESTE SCRIPT EN LOCAL. ############
-- El CTRL de local es el banco de pruebas de regresion de las 15 correcciones
-- del motor que quedan pendientes: alli SI se pueden recalcular los siete
-- meses, y sin CTRL no habria contra que compararlos. En produccion ya no
-- sirve --los meses estan cerrados y no se recalculan-- y solo genera
-- preguntas cuando el contador vea tablas con datos que nadie le explico.
-- ==
-- CUANDO SE CORRE: con enero a julio cerrados y antes de que el cliente entre
-- a operar agosto.
-- ============================================================================


-- ============================================================================
-- BLOQUE 1 - CONTROL ANTES: que hay, y que se va a perder.
-- ESPERADO: siete meses en CTRL. Julio sin filas de PLANILLA, porque esa
-- planilla nunca existio.
-- ============================================================================
SELECT CTRLANOO AS ANIO, CTRLMESS AS MES, CTRLFNTE AS FUENTE,
       COUNT(*) AS FILAS, COUNT(DISTINCT CTRLIDNT) AS PERSONAS
  FROM RHH.CTRL
 GROUP BY CTRLANOO, CTRLMESS, CTRLFNTE
 ORDER BY CTRLANOO, CTRLMESS, CTRLFNTE;

SELECT COUNT(*) AS TOTAL_FILAS_CTRL FROM RHH.CTRL;
SELECT ANIO, MES FROM RHH.CTRL_PARAM;


-- ============================================================================
-- BLOQUE 2 - EL RESPALDO. NO SE SALTA.
-- ==
-- Deja una copia con sello de fecha en el nombre. Si algun dia hiciera falta
-- volver a contrastar un mes --por una revision del cliente, por una auditoria,
-- por una discusion sobre un centavo-- esta tabla es la unica fuente.
-- ==
-- Si la tabla ya existiera, el CREATE falla con ORA-00955 y NO se ha borrado
-- nada todavia: eso significa que el script ya se corrio. PARAR y comprobar.
-- ============================================================================
CREATE TABLE RHH.CTRL_RESPALDO_20260825 AS SELECT * FROM RHH.CTRL;

-- Comprobacion del respaldo. Las dos cifras TIENEN que coincidir.
-- Si no coinciden, NO SEGUIR AL BLOQUE 3.
SELECT (SELECT COUNT(*) FROM RHH.CTRL)                    AS EN_ORIGEN,
       (SELECT COUNT(*) FROM RHH.CTRL_RESPALDO_20260825)  AS EN_RESPALDO
  FROM DUAL;


-- ============================================================================
-- BLOQUE 3 - EL BORRADO.
-- Solo despues de que el bloque 2 haya devuelto las dos cifras iguales.
-- ============================================================================
DELETE FROM RHH.CTRL;
DELETE FROM RHH.CTRL_PARAM;

COMMIT;


-- ============================================================================
-- BLOQUE 4 - CONTROL DESPUES.
-- ESPERADO: CTRL en 0, CTRL_PARAM en 0, y el respaldo con todas las filas.
-- ============================================================================
SELECT (SELECT COUNT(*) FROM RHH.CTRL)                    AS CTRL,
       (SELECT COUNT(*) FROM RHH.CTRL_PARAM)              AS CTRL_PARAM,
       (SELECT COUNT(*) FROM RHH.CTRL_RESPALDO_20260825)  AS RESPALDO
  FROM DUAL;


-- ============================================================================
-- BLOQUE 5 - Y QUE NO SE HAYA TOCADO NADA DE NOMINA.
-- ==
-- CTRL es andamio y ningun codigo Java lo lee --verificado con grep sobre
-- src/--, asi que borrarlo no puede mover una nomina. Esto lo comprueba en vez
-- de suponerlo, que es la diferencia entre un control y una creencia.
-- ==
-- ESPERADO: los ocho periodos intactos. Enero a julio en estado 7 con sus
-- totales, y agosto en estado 1 con los totales en nulo.
-- ============================================================================
SELECT PRDNCDGO AS PRDN, PRDNMSEE AS MES, PRDNESTD AS ESTADO, PRDNMODO AS MODO,
       PRDNTTIN AS INGRESOS, PRDNTTDS AS DESCUENTOS, PRDNTTNT AS NETO
  FROM RHH.PRDN
 WHERE PRDNANOO = 2026
 ORDER BY PRDNMSEE;

-- Y los acumulados del ano, que son lo que alimenta los formularios de renta.
-- ESPERADO: 876.
SELECT COUNT(*) AS ACUMULADOS_2026 FROM RHH.ACMN WHERE ACMNANOO = 2026;


-- ============================================================================
-- NOTA SOBRE EL RESPALDO
-- ==
-- RHH.CTRL_RESPALDO_20260825 se queda en la base. No ocupa nada y es la unica
-- copia del patron de validacion de la carga historica. NO la borre nadie sin
-- decision expresa: el dia que haga falta, hara mucha falta.
-- ============================================================================
