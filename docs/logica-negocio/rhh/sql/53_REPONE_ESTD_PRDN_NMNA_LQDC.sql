-- =====================================================
-- MODULO: RHH - REPONE LAS COLUMNAS DE ESTADO PERDIDAS POR REEJECUTAR EL 05
-- FECHA: 2026-08-21
-- DESTINO: PRODUCCION (y cualquier base donde el 05 se haya corrido dos veces)
-- =====================================================
-- QUE PASO
--   En produccion, RHH.PRDN, RHH.NMNA y RHH.LQDC se quedaron SIN su columna
--   de estado. Los tres endpoints REST mueren con ORA-00904 y ninguna de las
--   tres pantallas -periodos, nomina, finiquitos- puede ni listar.
--
--   Detectado el 2026-08-21 al abrir la pantalla de finiquitos en produccion:
--     GET /SaaBE/rest/lqdc/getAll -> 500
--     ORA-00904: "L1_0"."LQDCESTD": identificador no valido
--   y confirmado despues en prdn y nmna con el mismo barrido.
--
-- POR QUE PASO, QUE ES LO QUE IMPORTA
--   El 05 convierte los estados de VARCHAR2 a NUMBER con DROP + ADD. Usa dos
--   formas distintas, y solo una sobrevive a una segunda ejecucion:
--
--     MPLD (05:37) -- dos sentencias sueltas -> REEJECUTABLE
--       ALTER TABLE RHH.MPLD DROP COLUMN MPLDESTD;
--       ALTER TABLE RHH.MPLD ADD (MPLDESTD NUMBER DEFAULT 1);
--
--     PRDN (05:144), NMNA (05:193), LQDC (05:288) -- DROP suelto + ADD en
--     bloque junto a otras columnas -> NO REEJECUTABLE
--       ALTER TABLE RHH.LQDC DROP COLUMN LQDCESTD;
--       ALTER TABLE RHH.LQDC ADD (LQDCESTD ..., CSTRCDGO, ... 15 columnas);
--
--   En la segunda pasada el DROP se confirma solo y el ADD en bloque muere
--   ENTERO con ORA-01430 porque las otras 14 ya existian. La columna del
--   estado desaparece; las demas quedan intactas. Verificado en produccion:
--   LQDC tiene 22 de las 23 columnas que mapea la entidad, y la unica ausente
--   es LQDCESTD.
--
--   Las tres tablas afectadas son EXACTAMENTE las tres que usan la forma en
--   bloque. MPLD se salva por usar la otra.
--
-- POR QUE NADIE LO HABIA VISTO
--   Apertura, MPLD, CNTE, ACMN, SLDV y los anticipos no tocan ninguna de las
--   tres. PRDN y NMNA se estrenan al crear el primer periodo; LQDC, al primer
--   finiquito. En produccion las tres estaban todavia VACIAS.
--
-- POR QUE ESTE SCRIPT SI ES REEJECUTABLE
--   Cada ADD va guardado por un NOT EXISTS contra USER_TAB_COLUMNS. Correrlo
--   dos veces no hace nada la segunda vez. Es la propiedad que le faltaba al
--   05 y la razon de que exista este fichero.
--
-- NO REEJECUTAR EL 05 PARA ARREGLAR ESTO: una tercera pasada volveria a
-- tirar las tres columnas.
--
-- PRECONDICION VERIFICADA EN PRODUCCION EL 2026-08-21
--   SELECT (SELECT COUNT(*) FROM RHH.PRDN) PRDN,
--          (SELECT COUNT(*) FROM RHH.NMNA) NMNA,
--          (SELECT COUNT(*) FROM RHH.LQDC) LQDC FROM DUAL;
--   -> 0 · 0 · 0
--
--   Las tres VACIAS es lo que hace inofensivo el DEFAULT 1. Si alguna tuviera
--   filas, el DEFAULT les pondria un estado inventado -en PRDN el 1 es CREADO,
--   en LQDC es SIMULADA- y este script NO sirve tal cual: habria que reponer
--   la columna sin DEFAULT y decidir el estado fila por fila.
--   COMPROBAR ANTES DE CORRERLO EN CUALQUIER OTRA BASE.
--
-- Tipos y comentarios copiados literalmente del 05 (lineas 148, 196, 291).
-- =====================================================

SET SERVEROUTPUT ON

DECLARE
  v_existe NUMBER;

  PROCEDURE repone (p_tabla VARCHAR2, p_columna VARCHAR2, p_comentario VARCHAR2) IS
    v_n NUMBER;
  BEGIN
    SELECT COUNT(*) INTO v_n
      FROM ALL_TAB_COLUMNS
     WHERE OWNER = 'RHH' AND TABLE_NAME = p_tabla AND COLUMN_NAME = p_columna;

    IF v_n = 0 THEN
      EXECUTE IMMEDIATE 'ALTER TABLE RHH.' || p_tabla ||
                        ' ADD (' || p_columna || ' NUMBER DEFAULT 1)';
      EXECUTE IMMEDIATE 'COMMENT ON COLUMN RHH.' || p_tabla || '.' || p_columna ||
                        ' IS ''' || p_comentario || '''';
      DBMS_OUTPUT.PUT_LINE('REPUESTA  RHH.' || p_tabla || '.' || p_columna);
    ELSE
      DBMS_OUTPUT.PUT_LINE('YA EXISTE RHH.' || p_tabla || '.' || p_columna ||
                           ' - no se toca');
    END IF;
  END repone;

BEGIN
  -- Guarda dura: si alguna de las tres tiene filas, no se toca nada.
  SELECT (SELECT COUNT(*) FROM RHH.PRDN) +
         (SELECT COUNT(*) FROM RHH.NMNA) +
         (SELECT COUNT(*) FROM RHH.LQDC)
    INTO v_existe FROM DUAL;

  IF v_existe > 0 THEN
    RAISE_APPLICATION_ERROR(-20001,
      'PARAR: PRDN/NMNA/LQDC no estan vacias (' || v_existe || ' filas en total). ' ||
      'El DEFAULT 1 les pondria un estado inventado. Leer la cabecera de este script.');
  END IF;

  repone('PRDN', 'PRDNESTD',
         'Estado del periodo: detalle del rubro RHH_ESTADO_PERIODO_NOMINA');
  repone('NMNA', 'NMNAESTD',
         'Estado de la nomina: detalle del rubro RHH_ESTADO_NOMINA');
  repone('LQDC', 'LQDCESTD',
         'Estado de la liquidacion: detalle del rubro RHH_ESTADO_LIQUIDACION');
END;
/

COMMIT;


-- =====================================================
-- COMPROBACION: las tres deben aparecer, y el veredicto decir OK
-- =====================================================
SET PAGESIZE 100
SET LINESIZE 160
COLUMN TABLA     FORMAT A8
COLUMN COLUMNA   FORMAT A12
COLUMN TIPO      FORMAT A10
COLUMN DEFECTO   FORMAT A10
COLUMN VEREDICTO FORMAT A34

SELECT t.TABLA,
       t.COLUMNA,
       NVL(c.DATA_TYPE, '-')               AS TIPO,
       NVL(TO_CHAR(c.DATA_DEFAULT), '-')   AS DEFECTO,
       CASE WHEN c.COLUMN_NAME IS NULL
            THEN '*** SIGUE FALTANDO: PARAR ***'
            ELSE 'OK'
       END                                 AS VEREDICTO
  FROM (SELECT 'PRDN' AS TABLA, 'PRDNESTD' AS COLUMNA FROM DUAL UNION ALL
        SELECT 'NMNA',          'NMNAESTD'            FROM DUAL UNION ALL
        SELECT 'LQDC',          'LQDCESTD'            FROM DUAL) t
  LEFT JOIN ALL_TAB_COLUMNS c
         ON c.OWNER = 'RHH' AND c.TABLE_NAME = t.TABLA AND c.COLUMN_NAME = t.COLUMNA
 ORDER BY t.TABLA;


-- =====================================================
-- DESPUES DE CORRERLO, DESDE LA PANTALLA (consola del navegador)
-- =====================================================
--   for (const e of ['prdn','nmna','lqdc'])
--     console.log(e, (await fetch(`/SaaBE/rest/${e}/getAll`)).status);
--   -> los tres deben dar 200. Con 500, leer el ORA-00904: hay mas columnas
--      caidas del mismo ADD en bloque y toca correr DIAGNOSTICO_PRDN_NMNA_LQDC.sql
-- =====================================================
