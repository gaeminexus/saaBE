-- =====================================================================
-- CRD.GNAP — Marca de descarga del archivo Petrocomercial
-- =====================================================================
-- Necesario para el proceso de eliminacion de una generacion de archivo
-- Petrocomercial (DELETE /rest/gnap/eliminar/{codigo}).
--
-- La regla es: una generacion se puede borrar mientras su TXT no haya
-- salido del sistema. Hasta ahora no habia forma de saberlo — el front
-- bajaba el archivo por /rest/files/download, que no registra nada.
--
-- Estas dos columnas las estampa GET /rest/gnap/descargarArchivo/{codigo}
-- la primera vez que se descarga el archivo. Con GNAPFCDS distinto de
-- NULL, el proceso de eliminacion responde 409 y no borra nada.
--
-- Ejecutar como CRD, o con un usuario que pueda alterar objetos en CRD.
-- Ejecute sentencia por sentencia (Ctrl+Enter).
-- =====================================================================


ALTER TABLE CRD.GNAP ADD (
    GNAPFCDS DATE,
    GNAPUSDS VARCHAR2(50)
);

COMMENT ON COLUMN CRD.GNAP.GNAPFCDS IS 'Fecha en que se descargo el archivo TXT. NULL = aun no descargado, la generacion se puede eliminar';
COMMENT ON COLUMN CRD.GNAP.GNAPUSDS IS 'Usuario que descargo el archivo TXT';

COMMIT;


-- ---------------------------------------------------------------------
-- VERIFICACION
-- ---------------------------------------------------------------------
-- Las dos columnas deben aparecer con NULLABLE = 'Y'.

SELECT COLUMN_NAME, DATA_TYPE, DATA_LENGTH, NULLABLE
  FROM ALL_TAB_COLUMNS
 WHERE OWNER = 'CRD'
   AND TABLE_NAME = 'GNAP'
   AND COLUMN_NAME IN ('GNAPFCDS', 'GNAPUSDS');


-- ---------------------------------------------------------------------
-- GENERACIONES YA EXISTENTES
-- ---------------------------------------------------------------------
-- Quedan con GNAPFCDS en NULL, o sea "no descargadas", y por lo tanto
-- eliminables. Las que ya se entregaron a Petrocomercial estan en estado
-- 2 (ENVIADO) o 3 (PROCESADO) y el proceso las bloquea igual por estado.
--
-- Si hay generaciones en estado 1 (GENERADO) cuyo TXT ya se entrego,
-- marquelas a mano para protegerlas — ajuste la lista de codigos:
--
-- UPDATE CRD.GNAP
--    SET GNAPFCDS = SYSDATE,
--        GNAPUSDS = 'MIGRACION'
--  WHERE GNAPCDGO IN (/* codigos */);
-- COMMIT;

SELECT GNAPCDGO, GNAPMSPE, GNAPANPE, GNAPESTD, GNAPNMAR, GNAPFCDS
  FROM CRD.GNAP
 ORDER BY GNAPANPE DESC, GNAPMSPE DESC;
