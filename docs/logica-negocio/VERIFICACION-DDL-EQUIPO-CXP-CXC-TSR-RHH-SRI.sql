-- ============================================================================
-- VERIFICACION-DDL-EQUIPO-CXP-CXC-TSR-RHH-SRI.sql
-- Escrito por el arbitro del equipo el 2026-08-29
-- ============================================================================
--
-- POR QUE EXISTE
-- El documento de estado del equipo afirmaba "los DDL si estan corridos". El
-- 2026-08-29, al correr el control 3.3 de tsr/sql/08-rubros-partidas-transito.sql,
-- Oracle respondio ORA-00942 sobre TSR.DTCN: la tabla no existe, o sea que
-- tsr/sql/07-conciliacion-transito.sql NUNCA se ejecuto. El frente N figuraba
-- como cerrado. Nadie lo noto porque el codigo aun no esta compilado ni
-- desplegado: la primera senal habria sido un ORA-00942 en produccion.
--
-- Este script NO MODIFICA NADA. Es solo lectura: una fila por objeto que los
-- scripts de este equipo deberian haber creado, con OK o FALTA.
--
-- COMO LEERLO
-- Cada fila es una SONDA: un objeto representativo por script. Si la sonda de
-- un script dice FALTA, ese script no se corrio (o se corrio a medias). Correr
-- el script que nombra la columna SCRIPT y volver a pasar esta verificacion.
--
-- ALCANCE: solo los modulos de este equipo (cxp, cxc, pagos, tsr, sri) mas las
-- dos columnas que este equipo agrego en CNT (schema compartido). NO cubre crd
-- ni el grueso de rhh, que tienen sus propios juegos de scripts.
-- ============================================================================

SELECT SCRIPT, OBJETO, TIPO, ESTADO FROM (

-- ---------------------------------------------------------------- TABLAS ---
SELECT 'cxp/sql/01-create-tables-crtx-dcxp-dctx' AS SCRIPT, 'PGS.CRTX' AS OBJETO, 'TABLA' AS TIPO,
       CASE WHEN (SELECT COUNT(*) FROM ALL_TABLES WHERE OWNER='PGS' AND TABLE_NAME='CRTX')>0 THEN 'OK' ELSE 'FALTA' END AS ESTADO FROM DUAL
UNION ALL SELECT 'cxp/sql/01-create-tables-crtx-dcxp-dctx', 'PGS.DCXP', 'TABLA',
       CASE WHEN (SELECT COUNT(*) FROM ALL_TABLES WHERE OWNER='PGS' AND TABLE_NAME='DCXP')>0 THEN 'OK' ELSE 'FALTA' END FROM DUAL
UNION ALL SELECT 'cxp/sql/01-create-tables-crtx-dcxp-dctx', 'PGS.DCTX', 'TABLA',
       CASE WHEN (SELECT COUNT(*) FROM ALL_TABLES WHERE OWNER='PGS' AND TABLE_NAME='DCTX')>0 THEN 'OK' ELSE 'FALTA' END FROM DUAL
UNION ALL SELECT 'cxp/sql/04-create-tables-negociacion-proveedor', 'PGS.NGCP', 'TABLA',
       CASE WHEN (SELECT COUNT(*) FROM ALL_TABLES WHERE OWNER='PGS' AND TABLE_NAME='NGCP')>0 THEN 'OK' ELSE 'FALTA' END FROM DUAL
UNION ALL SELECT 'cxp/sql/04-create-tables-negociacion-proveedor', 'PGS.SQ_NGCPCDGO', 'SECUENCIA',
       CASE WHEN (SELECT COUNT(*) FROM ALL_SEQUENCES WHERE SEQUENCE_OWNER='PGS' AND SEQUENCE_NAME='SQ_NGCPCDGO')>0 THEN 'OK' ELSE 'FALTA' END FROM DUAL
UNION ALL SELECT 'cxp/sql/07-reembolso-gastos', 'PGS.RMBF', 'TABLA',
       CASE WHEN (SELECT COUNT(*) FROM ALL_TABLES WHERE OWNER='PGS' AND TABLE_NAME='RMBF')>0 THEN 'OK' ELSE 'FALTA' END FROM DUAL
UNION ALL SELECT 'cxc/sql/add-anticipo-cliente', 'CBR.ANTC', 'TABLA',
       CASE WHEN (SELECT COUNT(*) FROM ALL_TABLES WHERE OWNER='CBR' AND TABLE_NAME='ANTC')>0 THEN 'OK' ELSE 'FALTA' END FROM DUAL
UNION ALL SELECT 'cxc/sql/add-anticipo-cliente', 'CBR.SQ_ANTCCDGO', 'SECUENCIA',
       CASE WHEN (SELECT COUNT(*) FROM ALL_SEQUENCES WHERE SEQUENCE_OWNER='CBR' AND SEQUENCE_NAME='SQ_ANTCCDGO')>0 THEN 'OK' ELSE 'FALTA' END FROM DUAL
UNION ALL SELECT 'tsr/sql/02-caja-chica', 'TSR.CJCH', 'TABLA',
       CASE WHEN (SELECT COUNT(*) FROM ALL_TABLES WHERE OWNER='TSR' AND TABLE_NAME='CJCH')>0 THEN 'OK' ELSE 'FALTA' END FROM DUAL
UNION ALL SELECT 'tsr/sql/02-caja-chica', 'TSR.MVCH', 'TABLA',
       CASE WHEN (SELECT COUNT(*) FROM ALL_TABLES WHERE OWNER='TSR' AND TABLE_NAME='MVCH')>0 THEN 'OK' ELSE 'FALTA' END FROM DUAL
UNION ALL SELECT 'tsr/sql/02-caja-chica', 'TSR.CRCH', 'TABLA',
       CASE WHEN (SELECT COUNT(*) FROM ALL_TABLES WHERE OWNER='TSR' AND TABLE_NAME='CRCH')>0 THEN 'OK' ELSE 'FALTA' END FROM DUAL
UNION ALL SELECT 'tsr/sql/02-caja-chica', 'TSR.PTCH', 'TABLA',
       CASE WHEN (SELECT COUNT(*) FROM ALL_TABLES WHERE OWNER='TSR' AND TABLE_NAME='PTCH')>0 THEN 'OK' ELSE 'FALTA' END FROM DUAL

-- El que fallo el 2026-08-29. Si dice FALTA, correr tsr/sql/07 ANTES que nada.
UNION ALL SELECT 'tsr/sql/07-conciliacion-transito', 'TSR.DTCN', 'TABLA',
       CASE WHEN (SELECT COUNT(*) FROM ALL_TABLES WHERE OWNER='TSR' AND TABLE_NAME='DTCN')>0 THEN 'OK' ELSE 'FALTA' END FROM DUAL
UNION ALL SELECT 'tsr/sql/07-conciliacion-transito', 'TSR.SQ_DTCNCDGO', 'SECUENCIA',
       CASE WHEN (SELECT COUNT(*) FROM ALL_SEQUENCES WHERE SEQUENCE_OWNER='TSR' AND SEQUENCE_NAME='SQ_DTCNCDGO')>0 THEN 'OK' ELSE 'FALTA' END FROM DUAL
UNION ALL SELECT 'tsr/sql/07-conciliacion-transito', 'TSR.CNCL.CNCLESTD', 'COLUMNA',
       CASE WHEN (SELECT COUNT(*) FROM ALL_TAB_COLUMNS WHERE OWNER='TSR' AND TABLE_NAME='CNCL' AND COLUMN_NAME='CNCLESTD')>0 THEN 'OK' ELSE 'FALTA' END FROM DUAL

-- -------------------------------------------------------------- COLUMNAS ---
UNION ALL SELECT 'cxp/sql/06-alter-pgtr-debito-automatico', 'PGS.PGTR.PGTRDBAT', 'COLUMNA',
       CASE WHEN (SELECT COUNT(*) FROM ALL_TAB_COLUMNS WHERE OWNER='PGS' AND TABLE_NAME='PGTR' AND COLUMN_NAME='PGTRDBAT')>0 THEN 'OK' ELSE 'FALTA' END FROM DUAL
UNION ALL SELECT 'cxp/sql/08-carga-automatica-sri', 'PGS.DCXP.DCXPRSRI', 'COLUMNA',
       CASE WHEN (SELECT COUNT(*) FROM ALL_TAB_COLUMNS WHERE OWNER='PGS' AND TABLE_NAME='DCXP' AND COLUMN_NAME='DCXPRSRI')>0 THEN 'OK' ELSE 'FALTA' END FROM DUAL
UNION ALL SELECT 'cxp/sql/07-reembolso-gastos', 'PGS.FCTC.FCTCESRM', 'COLUMNA',
       CASE WHEN (SELECT COUNT(*) FROM ALL_TAB_COLUMNS WHERE OWNER='PGS' AND TABLE_NAME='FCTC' AND COLUMN_NAME='FCTCESRM')>0 THEN 'OK' ELSE 'FALTA' END FROM DUAL
UNION ALL SELECT 'cxp/sql/add-anulacion-documentos-compra', 'PGS.FCTC.FCTCMTAN', 'COLUMNA',
       CASE WHEN (SELECT COUNT(*) FROM ALL_TAB_COLUMNS WHERE OWNER='PGS' AND TABLE_NAME='FCTC' AND COLUMN_NAME='FCTCMTAN')>0 THEN 'OK' ELSE 'FALTA' END FROM DUAL
UNION ALL SELECT 'cxp/sql/add-anulacion-documentos-compra', 'PGS.NTDC.NTDCMTAN', 'COLUMNA',
       CASE WHEN (SELECT COUNT(*) FROM ALL_TAB_COLUMNS WHERE OWNER='PGS' AND TABLE_NAME='NTDC' AND COLUMN_NAME='NTDCMTAN')>0 THEN 'OK' ELSE 'FALTA' END FROM DUAL
UNION ALL SELECT 'cxc/sql/add-anulacion-factura', 'CBR.FCTR.MOTIVOANULACION', 'COLUMNA',
       CASE WHEN (SELECT COUNT(*) FROM ALL_TAB_COLUMNS WHERE OWNER='CBR' AND TABLE_NAME='FCTR' AND COLUMN_NAME='MOTIVOANULACION')>0 THEN 'OK' ELSE 'FALTA' END FROM DUAL
UNION ALL SELECT 'cxc/sql/add-anulacion-nc-nd-rtn', 'CBR.NTCR.MOTIVOANULACION', 'COLUMNA',
       CASE WHEN (SELECT COUNT(*) FROM ALL_TAB_COLUMNS WHERE OWNER='CBR' AND TABLE_NAME='NTCR' AND COLUMN_NAME='MOTIVOANULACION')>0 THEN 'OK' ELSE 'FALTA' END FROM DUAL
UNION ALL SELECT 'cxc/sql/add-anulacion-nc-nd-rtn', 'CBR.RTNC.MOTIVOANULACION', 'COLUMNA',
       CASE WHEN (SELECT COUNT(*) FROM ALL_TAB_COLUMNS WHERE OWNER='CBR' AND TABLE_NAME='RTNC' AND COLUMN_NAME='MOTIVOANULACION')>0 THEN 'OK' ELSE 'FALTA' END FROM DUAL
UNION ALL SELECT 'cxc/sql/add-asiento-contable-factura', 'CBR.FCTR.ASIENTO', 'COLUMNA',
       CASE WHEN (SELECT COUNT(*) FROM ALL_TAB_COLUMNS WHERE OWNER='CBR' AND TABLE_NAME='FCTR' AND COLUMN_NAME='ASIENTO')>0 THEN 'OK' ELSE 'FALTA' END FROM DUAL
UNION ALL SELECT 'cxc/sql/add-anulacion-factura (schema CNT compartido)', 'CNT.ASNT.ASNTMTAN', 'COLUMNA',
       CASE WHEN (SELECT COUNT(*) FROM ALL_TAB_COLUMNS WHERE OWNER='CNT' AND TABLE_NAME='ASNT' AND COLUMN_NAME='ASNTMTAN')>0 THEN 'OK' ELSE 'FALTA' END FROM DUAL
UNION ALL SELECT 'cxc/sql/add-liquidacion-compra-emision', 'CBR.LQCS.LQCSLQCC', 'COLUMNA',
       CASE WHEN (SELECT COUNT(*) FROM ALL_TAB_COLUMNS WHERE OWNER='CBR' AND TABLE_NAME='LQCS' AND COLUMN_NAME='LQCSLQCC')>0 THEN 'OK' ELSE 'FALTA' END FROM DUAL
UNION ALL SELECT 'cxc/sql/add-anticipo-cliente-devolucion', 'CBR.ANTC.ANTCIDPG', 'COLUMNA',
       CASE WHEN (SELECT COUNT(*) FROM ALL_TAB_COLUMNS WHERE OWNER='CBR' AND TABLE_NAME='ANTC' AND COLUMN_NAME='ANTCIDPG')>0 THEN 'OK' ELSE 'FALTA' END FROM DUAL
UNION ALL SELECT 'tsr/sql/01-cheques-pago-programado', 'PGS.PGTR.PGTRFPAG', 'COLUMNA',
       CASE WHEN (SELECT COUNT(*) FROM ALL_TAB_COLUMNS WHERE OWNER='PGS' AND TABLE_NAME='PGTR' AND COLUMN_NAME='PGTRFPAG')>0 THEN 'OK' ELSE 'FALTA' END FROM DUAL
UNION ALL SELECT 'tsr/sql/01-cheques-pago-programado', 'TSR.CNBC.CNBCCHQR', 'COLUMNA',
       CASE WHEN (SELECT COUNT(*) FROM ALL_TAB_COLUMNS WHERE OWNER='TSR' AND TABLE_NAME='CNBC' AND COLUMN_NAME='CNBCCHQR')>0 THEN 'OK' ELSE 'FALTA' END FROM DUAL
UNION ALL SELECT 'sri/sql/02-sustento-tributario', 'PGS.FCTC.FCTCCSUS', 'COLUMNA',
       CASE WHEN (SELECT COUNT(*) FROM ALL_TAB_COLUMNS WHERE OWNER='PGS' AND TABLE_NAME='FCTC' AND COLUMN_NAME='FCTCCSUS')>0 THEN 'OK' ELSE 'FALTA' END FROM DUAL
UNION ALL SELECT 'sri/sql/02-sustento-tributario', 'PGS.GRPP.GRPPCSUS', 'COLUMNA',
       CASE WHEN (SELECT COUNT(*) FROM ALL_TAB_COLUMNS WHERE OWNER='PGS' AND TABLE_NAME='GRPP' AND COLUMN_NAME='GRPPCSUS')>0 THEN 'OK' ELSE 'FALTA' END FROM DUAL
UNION ALL SELECT 'sri/sql/03-partereal-tipoprov-fecharegistro', 'PGS.FCTC.FCTCFCRG', 'COLUMNA',
       CASE WHEN (SELECT COUNT(*) FROM ALL_TAB_COLUMNS WHERE OWNER='PGS' AND TABLE_NAME='FCTC' AND COLUMN_NAME='FCTCFCRG')>0 THEN 'OK' ELSE 'FALTA' END FROM DUAL
UNION ALL SELECT 'sri/sql/03-partereal-tipoprov-fecharegistro', 'TSR.TTLR.TTLRPREL', 'COLUMNA',
       CASE WHEN (SELECT COUNT(*) FROM ALL_TAB_COLUMNS WHERE OWNER='TSR' AND TABLE_NAME='TTLR' AND COLUMN_NAME='TTLRPREL')>0 THEN 'OK' ELSE 'FALTA' END FROM DUAL

-- pagos/sql/01: no agrega columna, la vuelve OPCIONAL. La sonda es la
-- nulabilidad: si dice FALTA, registrar un pago revienta con ORA-01400.
UNION ALL SELECT 'pagos/sql/01-aprobacion-pagos', 'PGS.PGTR.PGTRCNBC admite NULL', 'NULABILIDAD',
       CASE WHEN (SELECT COUNT(*) FROM ALL_TAB_COLUMNS WHERE OWNER='PGS' AND TABLE_NAME='PGTR' AND COLUMN_NAME='PGTRCNBC' AND NULLABLE='Y')>0 THEN 'OK' ELSE 'FALTA' END FROM DUAL

-- -------------------------------------------------------------- CATALOGOS ---
UNION ALL SELECT 'sri/sql/01-catalogos-ats', 'PGS.LSRI tabla 703 (sustentos)', 'CATALOGO',
       CASE WHEN (SELECT COUNT(*) FROM PGS.LSRI WHERE TABLA='703')>0 THEN 'OK' ELSE 'FALTA' END FROM DUAL
UNION ALL SELECT 'cxp/sql/05-insert-rubros-proceso-carga', 'SCP.PRBR rubros 173/174', 'CATALOGO',
       CASE WHEN (SELECT COUNT(*) FROM SCP.PRBR WHERE PRBRALTR IN (173,174))=2 THEN 'OK' ELSE 'FALTA' END FROM DUAL
UNION ALL SELECT 'tsr/sql/02-caja-chica', 'SCP.PRBR rubros 232/233', 'CATALOGO',
       CASE WHEN (SELECT COUNT(*) FROM SCP.PRBR WHERE PRBRALTR IN (232,233))=2 THEN 'OK' ELSE 'FALTA' END FROM DUAL
UNION ALL SELECT 'sri/sql/04-reserva-rubro-sustento-tributario', 'SCP.PRBR rubro 238', 'CATALOGO',
       CASE WHEN (SELECT COUNT(*) FROM SCP.PRBR WHERE PRBRALTR=238)>0 THEN 'OK' ELSE 'FALTA' END FROM DUAL
UNION ALL SELECT 'tsr/sql/08-rubros-partidas-transito', 'SCP.PRBR rubros 239/240/241', 'CATALOGO',
       CASE WHEN (SELECT COUNT(*) FROM SCP.PRBR WHERE PRBRALTR IN (239,240,241))=3 THEN 'OK' ELSE 'FALTA' END FROM DUAL
UNION ALL SELECT 'tsr/sql/08-rubros-partidas-transito', 'SCP.PDTR 9 detalles 1151-1159', 'CATALOGO',
       CASE WHEN (SELECT COUNT(*) FROM SCP.PDTR WHERE PDTRCDGO BETWEEN 1151 AND 1159)=9 THEN 'OK' ELSE 'FALTA' END FROM DUAL

-- ------------------------------------------------- SONDA APARTE, NO ES DDL ---
-- TitularDaoServiceImpl.buscarPorNombreSimilar lanza una consulta NATIVA
-- contra TSR.TSRD, no contra TSR.TTLR, que es la tabla de la entidad Titular.
-- Si TSRD no existe, ese metodo revienta con ORA-00942 en tiempo de ejecucion
-- y ningun compilador lo detecta (es un string). Ningun script de este equipo
-- crea TSRD: o es un objeto legado, o es un defecto. Esta sonda lo decide.
UNION ALL SELECT '(no es DDL) TitularDaoServiceImpl:101 consulta nativa', 'TSR.TSRD', 'TABLA O VISTA',
       CASE WHEN (SELECT COUNT(*) FROM ALL_OBJECTS WHERE OWNER='TSR' AND OBJECT_NAME='TSRD'
                  AND OBJECT_TYPE IN ('TABLE','VIEW','SYNONYM'))>0 THEN 'OK' ELSE 'FALTA' END FROM DUAL

)
ORDER BY ESTADO, SCRIPT, OBJETO;
