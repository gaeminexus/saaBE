-- =====================================================================
-- Carga automatica de documentos CXP desde el SRI
-- Columnas nuevas en PGS.DCXP para trazar el origen del XML y el
-- resultado del ultimo intento de descarga contra el servicio del SRI.
--
-- Ver: docs/logica-negocio/cxp/PLAN-CARGA-AUTOMATICA-SRI.md  (seccion 5)
-- Fecha: 2026-08-22
-- ESTADO: NO EJECUTADO
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. CONTROL PREVIO — que existe hoy
-- ---------------------------------------------------------------------
-- Debe devolver 0 filas. Si devuelve alguna, las columnas ya estan
-- creadas y hay que saltar el paso 2.
SELECT column_name, data_type, data_length, nullable
  FROM all_tab_columns
 WHERE owner = 'PGS'
   AND table_name = 'DCXP'
   AND column_name IN ('DCXPORXM', 'DCXPRSRI', 'DCXPMSRI', 'DCXPFDSC')
 ORDER BY column_name;

-- Cuantos documentos hay hoy, y cuantos tienen XML cargado.
-- Sirve de linea base: despues del ALTER estos numeros no deben cambiar.
SELECT COUNT(*)                                          AS total_documentos,
       COUNT(DCXPPXML)                                   AS con_path_xml,
       SUM(CASE WHEN DCXPESTD = 1 THEN 1 ELSE 0 END)     AS leidos,
       SUM(CASE WHEN DCXPESTD = 2 THEN 1 ELSE 0 END)     AS xml_cargado,
       SUM(CASE WHEN DCXPESTD = 3 THEN 1 ELSE 0 END)     AS registrados
  FROM PGS.DCXP;

-- ---------------------------------------------------------------------
-- 2. DDL
-- ---------------------------------------------------------------------
-- Las cuatro columnas son NULL-ables a proposito: los documentos
-- historicos se cargaron a mano y no tienen resultado del SRI. Un
-- DCXPORXM nulo se lee como "manual, anterior a este cambio".

ALTER TABLE PGS.DCXP ADD (
    DCXPORXM  NUMBER(1),
    DCXPRSRI  VARCHAR2(30),
    DCXPMSRI  VARCHAR2(500),
    DCXPFDSC  TIMESTAMP
);

COMMENT ON COLUMN PGS.DCXP.DCXPORXM IS
    'Origen del XML: 1=Manual (subido por el usuario) 2=SRI (descargado por servicio web). NULL=historico anterior al cambio';

COMMENT ON COLUMN PGS.DCXP.DCXPRSRI IS
    'Resultado del ultimo intento de descarga: DESCARGADO, FUERA_VENTANA, NO_ENCONTRADO, NO_AUTORIZADO, ERROR_CONEXION';

COMMENT ON COLUMN PGS.DCXP.DCXPMSRI IS
    'Mensaje devuelto por el SRI, o el motivo calculado localmente cuando no se llego a llamar';

COMMENT ON COLUMN PGS.DCXP.DCXPFDSC IS
    'Fecha y hora del ultimo intento de descarga del XML desde el SRI';

COMMIT;

-- ---------------------------------------------------------------------
-- 3. CONTROL POSTERIOR
-- ---------------------------------------------------------------------
-- Debe devolver las 4 filas, todas nullable = 'Y'.
SELECT column_name, data_type, data_length, data_precision, nullable
  FROM all_tab_columns
 WHERE owner = 'PGS'
   AND table_name = 'DCXP'
   AND column_name IN ('DCXPORXM', 'DCXPRSRI', 'DCXPMSRI', 'DCXPFDSC')
 ORDER BY column_name;

-- Debe devolver los mismos numeros que el control previo, y las cuatro
-- columnas nuevas en cero.
SELECT COUNT(*)            AS total_documentos,
       COUNT(DCXPPXML)     AS con_path_xml,
       COUNT(DCXPORXM)     AS con_origen_xml,
       COUNT(DCXPRSRI)     AS con_resultado_sri,
       COUNT(DCXPMSRI)     AS con_mensaje_sri,
       COUNT(DCXPFDSC)     AS con_fecha_descarga
  FROM PGS.DCXP;

-- ---------------------------------------------------------------------
-- 4. ROLLBACK
-- ---------------------------------------------------------------------
-- Solo si hay que deshacer. Borra los datos de las cuatro columnas.
--
-- ALTER TABLE PGS.DCXP DROP (DCXPORXM, DCXPRSRI, DCXPMSRI, DCXPFDSC);
-- COMMIT;
