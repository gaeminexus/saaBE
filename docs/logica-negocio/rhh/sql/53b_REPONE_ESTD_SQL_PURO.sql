-- =====================================================
-- MODULO: RHH - REPONE LAS COLUMNAS DE ESTADO  (VERSION SQL PURO)
-- FECHA: 2026-08-21
-- DESTINO: PRODUCCION
-- =====================================================
-- ES EL MISMO ARREGLO QUE EL sql/53, SIN PL/SQL.
--
--   El 53 empieza con SET SERVEROUTPUT ON, que es un comando de SQL*Plus, no
--   de Oracle. DBeaver, SQL Developer y cualquier cliente JDBC lo mandan al
--   servidor como si fuera SQL y devuelve ORA-00922 en la primera linea.
--   Ocurrio en produccion el 2026-08-21.
--
--   USAR EL 53  -> desde sqlplus.
--   USAR ESTE   -> desde DBeaver, SQL Developer o cualquier cliente JDBC.
--
--   El 53 lleva la guarda dentro del bloque; aqui va como paso 1 manual y hay
--   que MIRARLA antes de seguir. Es la misma condicion y por la misma razon.
--
-- POR QUE HIZO FALTA: el sql/05 corrio dos veces. Convierte los estados de
-- VARCHAR2 a NUMBER con DROP + ADD, y en PRDN, NMNA y LQDC mete la columna de
-- estado en un ADD de quince columnas: en la segunda pasada el DROP se
-- confirma solo y el ADD entero muere con ORA-01430 porque las otras catorce
-- ya existian. MPLD se salva porque usa la forma suelta.
-- Leer la cabecera del 53 para el detalle completo.
--
-- NO REEJECUTAR EL 05 PARA ARREGLAR ESTO: una tercera pasada las tiraria otra vez.
-- =====================================================

-- -----------------------------------------------------
-- PASO 1 - LA GUARDA. Tiene que devolver 0 · 0 · 0.
-- Si alguna trae filas, PARAR: el DEFAULT 1 les inventaria un estado
-- (en PRDN el 1 es CREADO, en LQDC es SIMULADA) y habria que decidirlo
-- fila por fila, reponiendo la columna SIN default.
-- -----------------------------------------------------
SELECT (SELECT COUNT(*) FROM RHH.PRDN) AS PRDN,
       (SELECT COUNT(*) FROM RHH.NMNA) AS NMNA,
       (SELECT COUNT(*) FROM RHH.LQDC) AS LQDC FROM DUAL;


-- -----------------------------------------------------
-- PASO 2 - LAS TRES COLUMNAS. Una sentencia a la vez.
-- Si alguna da ORA-01430 (la columna ya existe), esa ya estaba: seguir con
-- las otras. El DDL se confirma solo, no hace falta COMMIT.
-- Tipos y comentarios copiados del 05 (lineas 148, 196, 291).
-- -----------------------------------------------------
ALTER TABLE RHH.PRDN ADD (PRDNESTD NUMBER DEFAULT 1);
ALTER TABLE RHH.NMNA ADD (NMNAESTD NUMBER DEFAULT 1);
ALTER TABLE RHH.LQDC ADD (LQDCESTD NUMBER DEFAULT 1);

COMMENT ON COLUMN RHH.PRDN.PRDNESTD IS 'Estado del periodo: detalle del rubro RHH_ESTADO_PERIODO_NOMINA';
COMMENT ON COLUMN RHH.NMNA.NMNAESTD IS 'Estado de la nomina: detalle del rubro RHH_ESTADO_NOMINA';
COMMENT ON COLUMN RHH.LQDC.LQDCESTD IS 'Estado de la liquidacion: detalle del rubro RHH_ESTADO_LIQUIDACION';


-- -----------------------------------------------------
-- PASO 3 - COMPROBACION. Las tres tienen que decir OK.
-- -----------------------------------------------------
SELECT t.TABLA, t.COLUMNA, NVL(c.DATA_TYPE,'-') AS TIPO,
       CASE WHEN c.COLUMN_NAME IS NULL THEN '*** SIGUE FALTANDO: PARAR ***' ELSE 'OK' END AS VEREDICTO
  FROM (SELECT 'PRDN' TABLA,'PRDNESTD' COLUMNA FROM DUAL UNION ALL
        SELECT 'NMNA','NMNAESTD' FROM DUAL UNION ALL
        SELECT 'LQDC','LQDCESTD' FROM DUAL) t
  LEFT JOIN ALL_TAB_COLUMNS c
    ON c.OWNER='RHH' AND c.TABLE_NAME=t.TABLA AND c.COLUMN_NAME=t.COLUMNA
 ORDER BY t.TABLA;

-- Y desde la consola del navegador, los tres endpoints en 200:
--   for (const e of ['prdn','nmna','lqdc'])
--     console.log(e, (await fetch(`/SaaBE/rest/${e}/getAll`)).status);
