-- =====================================================================================
-- CRD.PGPC — SEGUNDA COLUMNA DE ASIENTO PARA EL PAGO DE PENSION COMPLEMENTARIA
-- FECHA: 2026-09-02   EQUIPO: CRD / EQUIPO B
--
-- ⚠️ ESTE SCRIPT ESCRIBE (ALTER TABLE). Correrlo ANTES de desplegar el WAR que la use.
--
-- QUE LO ORIGINA — decision del usuario, 2026-09-02: «sí quiero los dos asientos».
--
-- EL PROBLEMA: PGPC tiene UNA sola columna de asiento (PGPCNMAS). El pago de pension
-- genera DOS asientos:
--     1. DEVENGO      -- lo genera CRD (plantilla 35): D cuenta del jubilado -> H por pagar
--     2. PAGO         -- lo genera CXP/TSR con la orden: D por pagar -> H banco
-- Con una sola columna hay que elegir cual se guarda y el otro se pierde de vista. Hoy el
-- codigo guarda el del DEVENGO y dejo de tomar prestado el de CXP — o sea, el numero del
-- asiento bancario no queda referenciado en ningun lado del lado de CRD.
--
-- LA SOLUCION, y por que esta y no otra: se copia EXACTAMENTE el precedente de
-- CRD.DVAP (DevolucionAporte), que es el MISMO circuito (dinero saliendo a un tercero via
-- CXP) y ya resolvio esto:
--     DVAPNMAS -> asiento del PAGO           (el de CXP)
--     DVAPNMRC -> asiento de RECLASIFICACION (el de CRD)
-- Aca queda igual:
--     PGPCNMAS -> asiento del PAGO           (el de CXP)   [ya existe]
--     PGPCNMDV -> asiento del DEVENGO        (el de CRD)   [se agrega]
--
-- ⚠️ OJO AL SENTIDO: hoy el codigo escribe el DEVENGO en PGPCNMAS. Al agregar la columna
--    hay que MOVERLO a PGPCNMDV y devolver PGPCNMAS a su significado original (el de CXP,
--    que lo escribe el reconciliador al confirmar el pago). Si no se hace ese movimiento,
--    las dos columnas terminan significando lo contrario que en DVAP y nadie va a poder
--    leer las dos tablas con el mismo criterio. Eso es trabajo de codigo, no de este
--    script — pero sin el, esta columna queda vacia.
--
-- ⛔ POR QUE NO SE HIZO UNA TABLA DE REGISTRO como CRD.ASNTCRAR (AsientoCargaPetro):
--    esa tabla existe porque la carga Petro genera TRES asientos (transitorio, reparto,
--    aplicacion) y podria generar mas. La pension genera dos, fijos, y ya hay un
--    precedente de dos columnas en el mismo circuito. Generalizar el registro a todos los
--    procesos es una mejora razonable, pero es un proyecto aparte que deberia absorber
--    tambien a DVAP — no se cuela en este cambio.
-- =====================================================================================

SET PAGESIZE 100
SET LINESIZE 200


-- =====================================================================================
-- CONTROL ANTES
-- =====================================================================================

-- A.1 — La columna NO debe existir. Debe salir 0 filas.
SELECT  c.COLUMN_NAME
FROM    ALL_TAB_COLUMNS c
WHERE   c.OWNER = 'CRD' AND c.TABLE_NAME = 'PGPC' AND c.COLUMN_NAME = 'PGPCNMDV';

-- A.2 — El precedente de DVAP, para confirmar el criterio que se esta copiando.
--       Deben salir las dos: DVAPNMAS y DVAPNMRC.
SELECT  c.COLUMN_NAME, c.DATA_TYPE, c.NULLABLE
FROM    ALL_TAB_COLUMNS c
WHERE   c.OWNER = 'CRD' AND c.TABLE_NAME = 'DVAP'
AND     c.COLUMN_NAME IN ('DVAPNMAS', 'DVAPNMRC')
ORDER   BY c.COLUMN_NAME;

-- A.3 — Cuantos pagos de pension ya existen y cuantos tienen asiento hoy.
--       Es lo que va a haber que migrar a mano si ya se genero alguno.
SELECT  COUNT(*)                                                AS PAGOS,
        SUM(CASE WHEN p.PGPCNMAS IS NOT NULL THEN 1 ELSE 0 END) AS CON_ASIENTO
FROM    CRD.PGPC p;


-- =====================================================================================
-- EJECUCION
--
-- NULLABLE a proposito: un pago cuya orden todavia no se confirmo no tiene asiento de
-- CXP, y un pago generado con contabilidad desconectada no tiene ninguno de los dos.
-- Ausencia de dato legitima, no un hueco a rellenar.
-- =====================================================================================

ALTER TABLE CRD.PGPC ADD (PGPCNMDV NUMBER);

COMMENT ON COLUMN CRD.PGPC.PGPCNMDV IS 'Codigo del asiento de DEVENGO de la pension y su seguro de salud, generado por CRD con la plantilla alterno 35. Distinto de PGPCNMAS, que guarda el asiento del PAGO generado por CXP. Mismo criterio que DVAP.DVAPNMRC / DVAP.DVAPNMAS.';

COMMIT;


-- =====================================================================================
-- CONTROL DESPUES — ⛔ SI ESTO NO DA LO ESPERADO, EL DESPLIEGUE NO VA
-- =====================================================================================

-- D.1 — La columna existe, es NUMBER y admite null. Debe salir 1 fila con NULLABLE = 'Y'.
SELECT  c.COLUMN_NAME, c.DATA_TYPE, c.NULLABLE
FROM    ALL_TAB_COLUMNS c
WHERE   c.OWNER = 'CRD' AND c.TABLE_NAME = 'PGPC' AND c.COLUMN_NAME = 'PGPCNMDV';

-- D.2 — Las dos columnas de asiento de PGPC, juntas. Deben salir 2 filas.
SELECT  c.COLUMN_NAME, c.DATA_TYPE, c.NULLABLE
FROM    ALL_TAB_COLUMNS c
WHERE   c.OWNER = 'CRD' AND c.TABLE_NAME = 'PGPC'
AND     c.COLUMN_NAME IN ('PGPCNMAS', 'PGPCNMDV')
ORDER   BY c.COLUMN_NAME;


-- =====================================================================================
-- REVERSO — comentado a proposito. Descomentar SOLO si hay que deshacer este script.
-- =====================================================================================
-- ALTER TABLE CRD.PGPC DROP COLUMN PGPCNMDV;
