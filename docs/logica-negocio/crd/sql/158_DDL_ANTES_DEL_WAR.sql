-- =====================================================================================
-- DDL OBLIGATORIO ANTES DE DESPLEGAR EL WAR — equipo CRD / EQUIPO B
-- FECHA: 2026-09-02
--
-- ⛔ ESTO VA ANTES DEL WAR, NO DESPUES.
--
--    La entidad com.saa.model.crd.Prestamo YA MAPEA la columna PRSTIDPG (commit
--    7bca171). Hibernate incluye TODA columna @Column en el SELECT que genera, asi que
--    si el WAR sube y la columna no existe, NO falla la funcion nueva:
--
--        FALLA TODA LECTURA DE CRD.PRST CON ORA-00904.
--
--    Es decir, la cartera entera: prestamos, cobros, mora, reportes, Petro. En pantallas
--    sin ninguna relacion aparente con el otorgamiento.
--
--    Ya paso exactamente esto el 2026-08-31 con CRD.CBCR.CBCRASRP y tumbo la pantalla de
--    cobros completa — ver ORDEN-EJECUCION-DDL-PENDIENTE.md §6. No se detecto de
--    inmediato porque nadie abrio esa pantalla despues del despliegue.
--
-- ⚠️ ESTE SCRIPT SOLO CUBRE EL CODIGO DE ESTE EQUIPO (crd).
--    En el mismo repositorio commitea tambien el equipo omen-saa-2 (rhh, cxp, pagos,
--    cnt, tsr). Si el WAR se arma del mismo checkout, lleva su codigo tambien, y su DDL
--    NO esta aca. PEDIRSELO A SU ARBITRO antes de desplegar, o correr la verificacion
--    completa del paso 3, que los detecta a todos.
-- =====================================================================================

SET PAGESIZE 200
SET LINESIZE 200


-- =====================================================================================
-- PASO 1 — CONTROL PREVIO. Esperado: 0 filas.
--
-- Si devuelve una fila, la columna ya existe: NO correr el ALTER, saltar al paso 3.
-- =====================================================================================
SELECT  c.OWNER, c.TABLE_NAME, c.COLUMN_NAME, c.DATA_TYPE, c.NULLABLE
FROM    ALL_TAB_COLUMNS c
WHERE   c.OWNER = 'CRD'
AND     c.TABLE_NAME = 'PRST'
AND     c.COLUMN_NAME = 'PRSTIDPG';


-- =====================================================================================
-- PASO 2 — EL ALTER. Es lo unico obligatorio para que el WAR no rompa la cartera.
--
-- NUMBER y nullable, igual que CRD.DVAP.DVAPIDPG — verificado en el bloque 1.b del
-- script 157: DVAP / DVAPIDPG / NUMBER / Y.
--
-- SIN FOREIGN KEY, a proposito: CRD no puede tener una FK dura contra el schema de
-- pagos (§1 de PLAN-DEVOLUCION-APORTES.md). Es el mismo criterio de DVAPIDPG.
-- =====================================================================================
ALTER TABLE CRD.PRST ADD (PRSTIDPG NUMBER);

COMMENT ON COLUMN CRD.PRST.PRSTIDPG IS
    'Orden de pago generada en PGS.PGTR al aprobar el prestamo (desembolso hacia tesoreria). NULL mientras no se haya generado. Sin FK: CRD no referencia el schema de pagos.';

COMMIT;


-- =====================================================================================
-- PASO 3 — CONTROL POSTERIOR, Y EL QUE DE VERDAD IMPORTA
--
-- 3.1 La columna quedo. Esperado: 1 fila, NUMBER, nullable Y.
-- =====================================================================================
SELECT  c.OWNER, c.TABLE_NAME, c.COLUMN_NAME, c.DATA_TYPE, c.NULLABLE
FROM    ALL_TAB_COLUMNS c
WHERE   c.OWNER = 'CRD'
AND     c.TABLE_NAME = 'PRST'
AND     c.COLUMN_NAME = 'PRSTIDPG';

-- 3.2 ⛔ ANTES DEL WAR, CORRER LA VERIFICACION COMPLETA ENTIDAD-VS-ESQUEMA:
--
--        docs/logica-negocio/crd/sql/VERIFICACION-ENTIDADES-VS-ESQUEMA-CRD.sql
--
--     Las DOS consultas, no solo la primera. La consulta B es la que encuentra columnas
--     MAPEADAS QUE NO EXISTEN, que es exactamente esta clase de fallo — ya atrapo una
--     caida de produccion el 2026-08-30.
--
--     Dos advertencias al leer el resultado:
--       1. ALL_TAB_COLUMNS muestra solo lo que ve el usuario conectado. Conectarse con
--          el usuario del datasource o con DBA, o salen faltantes falsos.
--       2. Si aparece algo ADEMAS de PRSTIDPG: PARAR Y AVISAR ANTES DEL WAR. Puede ser
--          del otro equipo que trabaja este mismo repositorio, y es un mapeo que nadie
--          previo.


-- =====================================================================================
-- REVERSO — COMENTADO. Solo si hay que deshacer y el WAR NO se desplego.
--
-- ⛔ Con el WAR nuevo ya desplegado, borrar esta columna TUMBA LA CARTERA ENTERA: es el
--    mismo ORA-00904 al reves. Bajar primero el WAR anterior.
-- =====================================================================================
-- ALTER TABLE CRD.PRST DROP COLUMN PRSTIDPG;
-- COMMIT;


-- =====================================================================================
-- LO QUE ESTE SCRIPT NO HACE, Y HAY QUE SABERLO ANTES DE SUBIR
--
-- 1. NO habilita el otorgamiento. Con este WAR, `aprobar` FALLA SIEMPRE, a proposito:
--    el desglose contable espera el producto de pago de SOCIOS POR PAGAR, y el bloque 2
--    del script 157 salio VACIO — ese producto NO EXISTE en la base.
--    El mensaje es explicito ("falta resolver el producto de pago... El desembolso queda
--    bloqueado hasta completar esa configuracion"), no es una caida silenciosa.
--    ⚠️ Esto NO rompe nada que hoy funcione: `aprobar` nunca estuvo en produccion.
--
-- 2. NO crea la plantilla 34. Eso es el script 156, y es independiente: se puede correr
--    antes o despues, y mientras `aprobar` este bloqueado por el punto 1 no hace
--    diferencia. Correrlo igual es inofensivo y adelanta trabajo.
--
-- 3. NO resuelve la linea del bien en garantia. El bloque 6 del 157 lo midio:
--    5.664 prestamos, CERO con valor asegurado, los 5.664 en NULL. Un prendario o un
--    hipotecario que se otorgue va a quedar SIN registrar su garantia en cuentas de
--    orden, y el asiento cuadra igual. No bloquea el quirografario.
-- =====================================================================================
