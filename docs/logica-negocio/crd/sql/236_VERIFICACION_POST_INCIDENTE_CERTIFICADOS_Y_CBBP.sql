-- =====================================================================================
-- 236 - VERIFICACION CONSOLIDADA despues de correr 234, 233 y 235
-- FECHA: 2026-09-22 · EQUIPO: omen-saa-1 (CRD · EQUIPO B)
--
-- Sin comandos de SQL*Plus: no usa PROMPT, DEFINE, SET ni &variables.
-- ⭐ ESTE SCRIPT NO ESCRIBE NADA. Es todo SELECT: se puede correr entero, siempre.
--
-- PARA QUE: el usuario corrio los tres scripts el 2026-09-22. Esto confirma que los tres
-- quedaron bien, con evidencia y no con suposiciones. El bloque 0 da el veredicto en UNA
-- sola fila; los bloques 1 a 4 son el detalle para cuando algo diga FALTA.
--
-- ⚠️ Un control que se corre y no se lee no sirve de nada: esta es la tercera vez en
-- este modulo que un problema vivio semanas porque nadie miro una salida. Leer el
-- bloque 0 alcanza; los demas son para diagnosticar.
-- =====================================================================================


-- =====================================================================================
-- 0. VEREDICTO EN UNA SOLA FILA — los seis controles juntos
-- =====================================================================================
-- TODO tiene que decir OK. Cualquier FALTA manda al bloque correspondiente de abajo.

SELECT
  -- 234: el catalogo quedo con un solo tipo activo
  CASE WHEN (SELECT COUNT(*) FROM CRD.TPDJ t
              WHERE UPPER(TRIM(t.TPDJNMBR)) = 'CERTIFICADO BANCARIO'
                AND t.TPDJIDST = 1) = 1
       THEN 'OK' ELSE 'FALTA -> ver bloque 1' END          AS C1_TIPO_UNICO,

  -- 234: y el que quedo activo es el que TIENE los adjuntos (el 4), no el vacio
  CASE WHEN (SELECT COUNT(*) FROM CRD.ADJN a
              WHERE a.TPDJCDGO = (SELECT t.TPDJCDGO FROM CRD.TPDJ t
                                   WHERE UPPER(TRIM(t.TPDJNMBR)) = 'CERTIFICADO BANCARIO'
                                     AND t.TPDJIDST = 1
                                     AND ROWNUM = 1)) > 0
       THEN 'OK' ELSE 'FALTA -> ver bloque 1' END          AS C2_TIPO_CON_ADJUNTOS,

  -- 235: el indice que impide el duplicado
  CASE WHEN (SELECT COUNT(*) FROM ALL_INDEXES i
              WHERE i.OWNER = 'CRD' AND i.INDEX_NAME = 'UX_TPDJ_NOMBRE_ACTIVO'
                AND i.UNIQUENESS = 'UNIQUE') = 1
       THEN 'OK' ELSE 'FALTA -> ver bloque 2' END          AS C3_INDICE_TPDJ,

  -- 233: la tabla existe (esto es lo que causaba el ORA-00942)
  CASE WHEN (SELECT COUNT(*) FROM ALL_TABLES t
              WHERE t.OWNER = 'CRD' AND t.TABLE_NAME = 'CBBP') = 1
       THEN 'OK' ELSE 'FALTA -> ver bloque 3' END          AS C4_TABLA_CBBP,

  -- 233: con sus 11 columnas, ni una menos (Hibernate las pide TODAS en el SELECT)
  CASE WHEN (SELECT COUNT(*) FROM ALL_TAB_COLUMNS c
              WHERE c.OWNER = 'CRD' AND c.TABLE_NAME = 'CBBP') = 11
       THEN 'OK' ELSE 'FALTA -> ver bloque 3' END          AS C5_COLUMNAS_11,

  -- 233: secuencia e indice unico de la tabla nueva
  CASE WHEN (SELECT COUNT(*) FROM ALL_SEQUENCES s
              WHERE s.SEQUENCE_OWNER = 'CRD' AND s.SEQUENCE_NAME = 'SQ_CBBPCDGO') = 1
        AND (SELECT COUNT(*) FROM ALL_INDEXES i
              WHERE i.OWNER = 'CRD' AND i.INDEX_NAME = 'UX_CBBP_PARTICIPE_IDENT'
                AND i.UNIQUENESS = 'UNIQUE') = 1
       THEN 'OK' ELSE 'FALTA -> ver bloque 4' END          AS C6_SECUENCIA_E_INDICE
FROM DUAL;


-- =====================================================================================
-- 1. DETALLE — el catalogo de tipos de adjunto (234)
-- =====================================================================================
-- Esperado: UNA fila activa 'CERTIFICADO BANCARIO', que tiene que ser el id 4 (el que
-- tenia los 410 adjuntos). El 37 y el 38 en estado 0.

SELECT t.TPDJCDGO,
       '[' || t.TPDJNMBR || ']'                            AS NOMBRE,
       t.TPDJIDST                                          AS ESTADO,
       (SELECT COUNT(*) FROM CRD.ADJN a WHERE a.TPDJCDGO = t.TPDJCDGO) AS ADJUNTOS
  FROM CRD.TPDJ t
 WHERE UPPER(t.TPDJNMBR) LIKE '%CERTIFICAD%BANCARIO%'
 ORDER BY t.TPDJIDST DESC, t.TPDJCDGO;

--
-- ⛔ SI EL ACTIVO NO ES EL QUE TIENE LOS ADJUNTOS: se desactivo el equivocado. El
--    sintoma sigue igual (certificados invisibles) aunque el error haya desaparecido.
--    Reactivar el bueno y desactivar el otro; nada se perdio.
--


-- =====================================================================================
-- 2. DETALLE — el indice que impide el duplicado (235)
-- =====================================================================================

SELECT i.INDEX_NAME, i.UNIQUENESS, i.STATUS
  FROM ALL_INDEXES i
 WHERE i.OWNER = 'CRD' AND i.TABLE_NAME = 'TPDJ';

-- La prueba real, opcional y sin dejar rastro. Esperado: ORA-00001. El ERROR es el exito.
-- INSERT INTO CRD.TPDJ (TPDJNMBR, TPDJIDST) VALUES ('certificado bancario  ', 1);
-- ROLLBACK;


-- =====================================================================================
-- 3. DETALLE — la tabla CBBP y sus columnas (233)
-- =====================================================================================
-- Esperado: 11 filas. Las 9 primeras NOT NULL ('N'), y CBBPUSRG/CBBPFCRG nullables ('Y').
-- El mapeo JPA NO dice si una columna es obligatoria: esto si (leccion de H68).

SELECT c.COLUMN_NAME, c.DATA_TYPE, c.DATA_LENGTH, c.DATA_PRECISION, c.DATA_SCALE, c.NULLABLE
  FROM ALL_TAB_COLUMNS c
 WHERE c.OWNER = 'CRD' AND c.TABLE_NAME = 'CBBP'
 ORDER BY c.COLUMN_ID;

-- Contraste contra lo que Hibernate va a pedir. Esperado: 0 filas (ninguna faltante).
-- Si alguna aparece, la lectura de esa entidad revienta con ORA-00904 en cuanto un
-- usuario abra la ficha de un participe.
SELECT columna AS COLUMNA_QUE_LA_ENTIDAD_MAPEA_Y_NO_EXISTE
  FROM (SELECT 'CBBPCDGO' AS columna FROM DUAL UNION ALL
        SELECT 'ENTDCDGO' FROM DUAL UNION ALL
        SELECT 'CBBPNMBR' FROM DUAL UNION ALL
        SELECT 'CBBPIDNT' FROM DUAL UNION ALL
        SELECT 'BEXTCDGO' FROM DUAL UNION ALL
        SELECT 'CBBPTPCN' FROM DUAL UNION ALL
        SELECT 'CBBPNMRO' FROM DUAL UNION ALL
        SELECT 'CBBPPRCN' FROM DUAL UNION ALL
        SELECT 'CBBPIDST' FROM DUAL UNION ALL
        SELECT 'CBBPUSRG' FROM DUAL UNION ALL
        SELECT 'CBBPFCRG' FROM DUAL) esperadas
 WHERE columna NOT IN (SELECT c.COLUMN_NAME FROM ALL_TAB_COLUMNS c
                        WHERE c.OWNER = 'CRD' AND c.TABLE_NAME = 'CBBP');


-- =====================================================================================
-- 4. DETALLE — secuencia, indice y restricciones de CBBP (233)
-- =====================================================================================

SELECT s.SEQUENCE_NAME, s.LAST_NUMBER, s.INCREMENT_BY
  FROM ALL_SEQUENCES s
 WHERE s.SEQUENCE_OWNER = 'CRD' AND s.SEQUENCE_NAME = 'SQ_CBBPCDGO';

-- El indice unico es por (ENTDCDGO, CBBPIDNT), NO por la cedula sola: la misma persona
-- puede ser beneficiaria de varios participes. Esperado: 2 filas, UNIQUE.
SELECT i.INDEX_NAME, i.UNIQUENESS, c.COLUMN_NAME, c.COLUMN_POSITION
  FROM ALL_INDEXES i
  JOIN ALL_IND_COLUMNS c ON c.INDEX_OWNER = i.OWNER AND c.INDEX_NAME = i.INDEX_NAME
 WHERE i.OWNER = 'CRD' AND i.TABLE_NAME = 'CBBP'
 ORDER BY c.COLUMN_POSITION;

-- Esperado: PK_CBBP (P), FK_CBBP_ENTD (R), FK_CBBP_BEXT (R), los dos CK_, y 9 SYS_C de
-- NOT NULL. ⚠️ Si FK_CBBP_BEXT no esta, el control 0.4 del 233 fallo por el GRANT
-- REFERENCES sobre TSR.BEXT: la tabla funciona igual, pero queda sin esa integridad.
SELECT c.CONSTRAINT_NAME, c.CONSTRAINT_TYPE, c.SEARCH_CONDITION, c.STATUS
  FROM ALL_CONSTRAINTS c
 WHERE c.OWNER = 'CRD' AND c.TABLE_NAME = 'CBBP'
 ORDER BY c.CONSTRAINT_TYPE, c.CONSTRAINT_NAME;


-- =====================================================================================
-- 5. LO QUE ESTE SCRIPT NO PUEDE CONFIRMAR, Y HAY QUE MIRAR EN LA PANTALLA
-- =====================================================================================
-- 1. Que los certificados VUELVAN A VERSE: abrir la ficha de un participe que tenga uno
--    cargado. El bloque 0 dice que el catalogo quedo bien; solo la pantalla dice que el
--    usuario recupero lo suyo.
-- 2. Que la pestaña Beneficiarios abra sin error (ya no hay ORA-00942).
-- 3. Que el proceso de seguros medicos de jubilados corra: dependia del certificado.
-- =====================================================================================
