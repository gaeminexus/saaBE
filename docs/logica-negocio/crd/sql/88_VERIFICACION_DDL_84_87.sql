-- =====================================================================================
-- VERIFICACION DE LOS SCRIPTS 81, 84, 85, 86 Y 87
-- FECHA: 2026-08-31
--
-- Corre DESPUES de haberlos ejecutado y ANTES de desplegar el WAR.
-- Es de SOLO LECTURA: no modifica nada.
--
-- Cada consulta dice el resultado ESPERADO. Si alguna no lo da, PARAR y avisar al arbitro
-- antes del despliegue: una columna mapeada que falta no rompe la funcion nueva, rompe
-- CUALQUIER lectura de esa entidad con ORA-00904, en pantallas sin relacion aparente.
-- =====================================================================================


-- =====================================================================================
-- 1. LAS DOS TABLAS NUEVAS EXISTEN
-- =====================================================================================

-- 1.1 Esperado: 2 filas — CRD.DAAP (script 84) y CRD.DAPR (script 85).
SELECT t.OWNER, t.TABLE_NAME
FROM   ALL_TABLES t
WHERE  t.OWNER = 'CRD'
AND    t.TABLE_NAME IN ('DAAP','DAPR')
ORDER  BY t.TABLE_NAME;


-- =====================================================================================
-- 2. LAS COLUMNAS AGREGADAS A TABLAS QUE YA EXISTIAN
-- =====================================================================================

-- 2.1 Esperado: 4 filas exactas.
--     CRD.ACCN -> ACCNVLAP, ACCNVLDP (script 84) y PJRQCDGO (script 86)
--     CRD.AVPC -> TPAPCDGO (script 87)
--     Estas son las que rompen pantallas YA DESPLEGADAS si faltan: la entidad
--     AcuerdoCondonacion las mapea y CRD.ACCN esta viva en produccion desde el 30-08.
SELECT c.TABLE_NAME, c.COLUMN_NAME, c.DATA_TYPE, c.NULLABLE
FROM   ALL_TAB_COLUMNS c
WHERE  c.OWNER = 'CRD'
AND    ( (c.TABLE_NAME = 'ACCN' AND c.COLUMN_NAME IN ('ACCNVLAP','ACCNVLDP','PJRQCDGO'))
      OR (c.TABLE_NAME = 'AVPC' AND c.COLUMN_NAME = 'TPAPCDGO') )
ORDER  BY c.TABLE_NAME, c.COLUMN_NAME;


-- =====================================================================================
-- 3. EL CATALOGO — rubro 235, los dos alternos
-- =====================================================================================

-- 3.1 Esperado: 2 filas.
--     PDTRCDGO 1178, alterno 7 = JUBILACION      (script 81)
--     PDTRCDGO 1180, alterno 8 = EXCEDENTE_PETRO (script 87)
--     Si el alterno 7 no aparece, el 81 no corrio. Si aparece con otro PDTRCDGO,
--     alguien mas inserto y hay que revisar el registro de reservas antes de seguir.
SELECT d.PDTRCDGO, d.PDTRALTR, d.PDTRDSCR, d.PDTRVLRN, d.PDTRVLRV, d.PDTRESTD
FROM   SCP.PDTR d
JOIN   SCP.PRBR r ON r.PRBRCDGO = d.PRBRCDGO
WHERE  r.PRBRALTR = 235
ORDER  BY d.PDTRALTR;


-- =====================================================================================
-- 4. LA SECUENCIA — el control que evita una PK duplicada en una pantalla sin relacion
-- =====================================================================================

-- 4.1 Esperado: LAST_NUMBER >= 1181.
--     El script 87 inserto el PDTRCDGO 1180 explicitamente. Si la secuencia quedo en
--     1179 o menos (p. ej. porque se corrio el ALTER del 81 DESPUES del 87), el proximo
--     rubro creado DESDE LA APLICACION muere por PK duplicada.
--     Si da menos de 1181, correr:  ALTER SEQUENCE SCP.SQ_PDTRCDGO RESTART START WITH 1181;
SELECT s.SEQUENCE_NAME, s.LAST_NUMBER
FROM   ALL_SEQUENCES s
WHERE  s.SEQUENCE_OWNER = 'SCP'
AND    s.SEQUENCE_NAME IN ('SQ_PDTRCDGO','SQ_PRBRCDGO')
ORDER  BY s.SEQUENCE_NAME;

-- 4.2 Control cruzado: el maximo realmente usado. Esperado: 1180.
SELECT MAX(PDTRCDGO) AS MAX_PDTR FROM SCP.PDTR;


-- =====================================================================================
-- 5. LAS RESTRICCIONES — que el DDL haya entrado COMPLETO, no a medias
-- =====================================================================================

-- 5.1 Esperado: al menos 7 filas.
--     PK_DAAP, FK_DAAP_ACCN, PK_DAPR, FK_DAPR_DCBC, FK_DAPR_TPAP, UK_DAPR_DCBC_TPAP,
--     FK_ACCN_PJRQ, CK_ACCN_VLAP, CK_ACCN_VLDP, FK_AVPC_TPAP, CK_AVPC_PRST_XOR_TPAP.
--     Un ALTER que fallo a mitad deja la columna sin su constraint y no avisa despues.
SELECT c.TABLE_NAME, c.CONSTRAINT_NAME, c.CONSTRAINT_TYPE, c.STATUS
FROM   ALL_CONSTRAINTS c
WHERE  c.OWNER = 'CRD'
AND    c.CONSTRAINT_NAME IN ('PK_DAAP','FK_DAAP_ACCN','PK_DAPR','FK_DAPR_DCBC',
                             'FK_DAPR_TPAP','UK_DAPR_DCBC_TPAP','FK_ACCN_PJRQ',
                             'CK_ACCN_VLAP','CK_ACCN_VLDP','FK_AVPC_TPAP',
                             'CK_AVPC_PRST_XOR_TPAP')
ORDER  BY c.TABLE_NAME, c.CONSTRAINT_NAME;

-- 5.2 Ninguna constraint deshabilitada o no validada. Esperado: 0 filas.
SELECT c.TABLE_NAME, c.CONSTRAINT_NAME, c.STATUS, c.VALIDATED
FROM   ALL_CONSTRAINTS c
WHERE  c.OWNER = 'CRD'
AND    c.TABLE_NAME IN ('DAAP','DAPR','ACCN','AVPC')
AND    (c.STATUS <> 'ENABLED' OR c.VALIDATED <> 'VALIDATED');


-- =====================================================================================
-- 6. LOS INDICES — que hayan quedado en el schema CRD y no en el de la sesion
-- =====================================================================================

-- 6.1 Un CREATE INDEX sin prefijo de schema queda en el schema del usuario conectado, no
--     en CRD, y la tabla se queda sin el indice sin ningun error. Esperado: los indices
--     de estas tablas con OWNER = 'CRD'.
SELECT i.OWNER, i.INDEX_NAME, i.TABLE_NAME, i.STATUS
FROM   ALL_INDEXES i
WHERE  i.TABLE_NAME IN ('DAAP','DAPR','ACCN','AVPC')
AND    i.TABLE_OWNER = 'CRD'
ORDER  BY i.TABLE_NAME, i.INDEX_NAME;


-- =====================================================================================
-- 7. DESPUES DE ESTO
-- =====================================================================================
-- Si las siete consultas dieron lo esperado, corre igual el barrido completo
--   docs/logica-negocio/crd/sql/VERIFICACION-ENTIDADES-VS-ESQUEMA-CRD.sql
-- LAS DOS CONSULTAS. Este script mira solo lo que estos cinco tocaron; el completo
-- compara TODAS las columnas mapeadas de TODAS las entidades de crd, y es el que
-- encuentra lo que nadie previo.
--
-- Advertencia al leerlo: ALL_TAB_COLUMNS muestra solo lo que ve el usuario conectado.
-- Conectarse con el mismo usuario del datasource o con DBA, o salen faltantes falsos.
-- Y CRD.PRCA va a salir como ausente: es una entidad superseded que nadie llama, ya
-- registrada, no es un hallazgo nuevo.
-- =====================================================================================
