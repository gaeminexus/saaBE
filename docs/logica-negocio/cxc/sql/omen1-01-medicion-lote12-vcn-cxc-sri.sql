-- =====================================================================================
-- omen1-01 — MEDICIÓN (SOLO LECTURA) de lo ya afectado por los defectos #6, #7, #8 y #18 del informe de VCN (lote 12, CXC/SRI)
-- Equipo omen-saa-1 · 2026-11-25. No modifica nada. Se puede correr en producción.
-- Tablas (model/cxc): FCTR factura, NTCR nota de crédito, NTDB nota de débito, LQCS liquidación de compra, RTNC retención (v1), RTV2 retención V2;
--   NXPE numeración por punto de emisión; DRV2 detalle de la retención V2; TSRI tarifas SRI y LSRI su lista; TSR.TTLR titulares.
-- Todos los comprobantes comparten las columnas SECUENCIAL, TIPOCOMPROBANTE, PTOEMISION, AMBIENTE, CLAVE, FECHA, AUTORIZACION, FECHAAUTORIZACION, ESTADO.
-- ESTADO: 3 firmada · 4 enviada · 5 autorizada · 6 NO autorizada. El secuencial de NXPE.NUMACTUAL es «el próximo a usar» (se devuelve el valor previo).
-- Ningún valor individual (nombres ni montos de personas) se devuelve de forma agregada fuera de lo necesario para revisar.
-- =====================================================================================

-- ============================ #6 SECUENCIAL ==========================================
-- 6a. Secuenciales REPETIDOS por punto, tipo y ambiente (la carrera de dos emisiones a la vez). Esperado: 0 filas.
WITH docs AS (
    SELECT 'FCTR' AS doc, ID, PTOEMISION, TIPOCOMPROBANTE, AMBIENTE, SECUENCIAL, CLAVE, ESTADO FROM CBR.FCTR
    UNION ALL SELECT 'NTCR', ID, PTOEMISION, TIPOCOMPROBANTE, AMBIENTE, SECUENCIAL, CLAVE, ESTADO FROM CBR.NTCR
    UNION ALL SELECT 'NTDB', ID, PTOEMISION, TIPOCOMPROBANTE, AMBIENTE, SECUENCIAL, CLAVE, ESTADO FROM CBR.NTDB
    UNION ALL SELECT 'LQCS', ID, PTOEMISION, TIPOCOMPROBANTE, AMBIENTE, SECUENCIAL, CLAVE, ESTADO FROM CBR.LQCS
    UNION ALL SELECT 'RTNC', ID, PTOEMISION, TIPOCOMPROBANTE, AMBIENTE, SECUENCIAL, CLAVE, ESTADO FROM CBR.RTNC
    UNION ALL SELECT 'RTV2', ID, PTOEMISION, TIPOCOMPROBANTE, AMBIENTE, SECUENCIAL, CLAVE, ESTADO FROM CBR.RTV2
)
SELECT d.doc, d.PTOEMISION, d.TIPOCOMPROBANTE, d.AMBIENTE, d.SECUENCIAL, COUNT(*) AS veces, MIN(d.ID) AS primero, MAX(d.ID) AS ultimo
  FROM docs d WHERE d.SECUENCIAL IS NOT NULL
 GROUP BY d.doc, d.PTOEMISION, d.TIPOCOMPROBANTE, d.AMBIENTE, d.SECUENCIAL HAVING COUNT(*) > 1
 ORDER BY d.doc, d.PTOEMISION, d.SECUENCIAL;

-- 6b. El contador contra lo emitido: NXPE.NUMACTUAL debería ser MAX(secuencial emitido) + 1. Un contador MENOR que eso es un incremento perdido por la carrera
-- (la siguiente emisión repetiría un número). Un contador mayor deja huecos (normal si hubo emisiones que no llegaron a grabarse).
WITH docs AS (
    SELECT PTOEMISION, TIPOCOMPROBANTE AS tipo, AMBIENTE, TO_NUMBER(SECUENCIAL) AS sec FROM CBR.FCTR WHERE REGEXP_LIKE(SECUENCIAL, '^[0-9]+$')
    UNION ALL SELECT PTOEMISION, TIPOCOMPROBANTE, AMBIENTE, TO_NUMBER(SECUENCIAL) FROM CBR.NTCR WHERE REGEXP_LIKE(SECUENCIAL, '^[0-9]+$')
    UNION ALL SELECT PTOEMISION, TIPOCOMPROBANTE, AMBIENTE, TO_NUMBER(SECUENCIAL) FROM CBR.NTDB WHERE REGEXP_LIKE(SECUENCIAL, '^[0-9]+$')
    UNION ALL SELECT PTOEMISION, TIPOCOMPROBANTE, AMBIENTE, TO_NUMBER(SECUENCIAL) FROM CBR.LQCS WHERE REGEXP_LIKE(SECUENCIAL, '^[0-9]+$')
    UNION ALL SELECT PTOEMISION, TIPOCOMPROBANTE, AMBIENTE, TO_NUMBER(SECUENCIAL) FROM CBR.RTNC WHERE REGEXP_LIKE(SECUENCIAL, '^[0-9]+$')
    UNION ALL SELECT PTOEMISION, TIPOCOMPROBANTE, AMBIENTE, TO_NUMBER(SECUENCIAL) FROM CBR.RTV2 WHERE REGEXP_LIKE(SECUENCIAL, '^[0-9]+$')
)
SELECT n.PTOEMISION, n.TIPODOC, n.NUMACTUAL AS contador, MAX(d.sec) AS maximo_emitido, MAX(d.sec) + 1 AS contador_esperado,
       CASE WHEN n.NUMACTUAL < MAX(d.sec) + 1 THEN 'ATRASADO (repetiría un número)' ELSE 'OK o con huecos' END AS estado
  FROM CBR.NXPE n LEFT JOIN docs d ON d.PTOEMISION = n.PTOEMISION AND d.tipo = n.TIPODOC
 GROUP BY n.PTOEMISION, n.TIPODOC, n.NUMACTUAL
 ORDER BY CASE WHEN n.NUMACTUAL < NVL(MAX(d.sec), 0) + 1 THEN 0 ELSE 1 END, n.PTOEMISION, n.TIPODOC;

-- ============================ #8 ESTADOS DEL SRI =====================================
-- 8a. Comprobantes marcados NO AUTORIZADOS (ESTADO 6) por mes y tipo. Algunos pueden estar autorizados en el SRI (una respuesta «en proceso» se trataba como rechazo):
-- el usuario consulta la autorización real de las claves de 8b en el portal del SRI.
WITH docs AS (
    SELECT 'FCTR' AS doc, ID, CLAVE, FECHA, AMBIENTE, ESTADO FROM CBR.FCTR UNION ALL SELECT 'NTCR', ID, CLAVE, FECHA, AMBIENTE, ESTADO FROM CBR.NTCR
    UNION ALL SELECT 'NTDB', ID, CLAVE, FECHA, AMBIENTE, ESTADO FROM CBR.NTDB UNION ALL SELECT 'LQCS', ID, CLAVE, FECHA, AMBIENTE, ESTADO FROM CBR.LQCS
    UNION ALL SELECT 'RTNC', ID, CLAVE, FECHA, AMBIENTE, ESTADO FROM CBR.RTNC UNION ALL SELECT 'RTV2', ID, CLAVE, FECHA, AMBIENTE, ESTADO FROM CBR.RTV2
)
SELECT doc, TO_CHAR(FECHA, 'YYYY-MM') AS mes, AMBIENTE, COUNT(*) AS no_autorizados FROM docs WHERE ESTADO = 6 GROUP BY doc, TO_CHAR(FECHA, 'YYYY-MM'), AMBIENTE ORDER BY 2 DESC, 1;
-- 8b. Las claves de acceso de los NO AUTORIZADOS de producción (AMBIENTE 2) de los últimos 90 días: para consultarlas una por una en el SRI.
SELECT 'FCTR' AS doc, ID, CLAVE, FECHA FROM CBR.FCTR WHERE ESTADO = 6 AND AMBIENTE = 2 AND FECHA >= TRUNC(SYSDATE) - 90
UNION ALL SELECT 'NTCR', ID, CLAVE, FECHA FROM CBR.NTCR WHERE ESTADO = 6 AND AMBIENTE = 2 AND FECHA >= TRUNC(SYSDATE) - 90
UNION ALL SELECT 'NTDB', ID, CLAVE, FECHA FROM CBR.NTDB WHERE ESTADO = 6 AND AMBIENTE = 2 AND FECHA >= TRUNC(SYSDATE) - 90
UNION ALL SELECT 'LQCS', ID, CLAVE, FECHA FROM CBR.LQCS WHERE ESTADO = 6 AND AMBIENTE = 2 AND FECHA >= TRUNC(SYSDATE) - 90
UNION ALL SELECT 'RTV2', ID, CLAVE, FECHA FROM CBR.RTV2 WHERE ESTADO = 6 AND AMBIENTE = 2 AND FECHA >= TRUNC(SYSDATE) - 90
ORDER BY 4 DESC, 1;
-- 8c. Autorizados con la FECHA DE AUTORIZACIÓN INVENTADA por la rama «clave de acceso registrada» (fecha del documento + 1 min 15 s): su fecha real de
-- autorización se desconoce hasta consultarla al SRI.
SELECT 'FCTR' AS doc, ID, CLAVE, FECHA, FECHAAUTORIZACION FROM CBR.FCTR WHERE ESTADO = 5 AND ABS(CAST(FECHAAUTORIZACION AS DATE) - (FECHA + 75 / 86400)) < 1 / 86400
UNION ALL SELECT 'NTCR', ID, CLAVE, FECHA, FECHAAUTORIZACION FROM CBR.NTCR WHERE ESTADO = 5 AND ABS(CAST(FECHAAUTORIZACION AS DATE) - (FECHA + 75 / 86400)) < 1 / 86400
UNION ALL SELECT 'NTDB', ID, CLAVE, FECHA, FECHAAUTORIZACION FROM CBR.NTDB WHERE ESTADO = 5 AND ABS(CAST(FECHAAUTORIZACION AS DATE) - (FECHA + 75 / 86400)) < 1 / 86400
UNION ALL SELECT 'LQCS', ID, CLAVE, FECHA, FECHAAUTORIZACION FROM CBR.LQCS WHERE ESTADO = 5 AND ABS(CAST(FECHAAUTORIZACION AS DATE) - (FECHA + 75 / 86400)) < 1 / 86400
UNION ALL SELECT 'RTV2', ID, CLAVE, FECHA, FECHAAUTORIZACION FROM CBR.RTV2 WHERE ESTADO = 5 AND ABS(CAST(FECHAAUTORIZACION AS DATE) - (FECHA + 75 / 86400)) < 1 / 86400
ORDER BY 4 DESC, 1;

-- ============================ #7 RETENCIÓN CON TARIFA DEL SERVIDOR ===================
-- CORRECCION 2026-11-25: la version anterior de 7a/7b/7c enlazaba TSRI con LSRI por ID (t.LSRI = l.ID) y estaba MAL: CBR.TSRI.LSRI guarda el CODIGO de TABLA, no el id de LSRI
-- (medido con el control 13: LSRI=17 son las tarifas de IVA, la 608 tiene 130 filas y la 20 tiene 15). Los resultados de 6/2026-11-25 de 7a/7b/7c no valen; repetir.
-- 7a. Qué tablas hay en LSRI y cuántas tarifas activas tiene cada una. Se buscan las de retención en la fuente de renta (608) y de IVA (20).
SELECT l.ID AS lsri, l.TABLA, SUBSTR(l.DETALLE, 1, 60) AS detalle, l.ESTADO AS estado_lsri, COUNT(t.ID) AS tarifas, SUM(CASE WHEN t.ESTADO = 1 THEN 1 ELSE 0 END) AS tarifas_activas
  FROM CBR.LSRI l LEFT JOIN CBR.TSRI t ON TO_CHAR(t.LSRI) = l.TABLA WHERE l.TABLA IN ('608', '20') GROUP BY l.ID, l.TABLA, SUBSTR(l.DETALLE, 1, 60), l.ESTADO ORDER BY l.TABLA;
-- 7b. Códigos con MÁS DE UNA tarifa activa en las tablas 608 y 20: el servidor no podría elegir sin una vigencia. TSRI no tiene columnas de vigencia (solo ESTADO).
SELECT l.TABLA, t.CODIGO, COUNT(*) AS filas_activas, MIN(t.PORCENTAJE) AS porcentaje_min, MAX(t.PORCENTAJE) AS porcentaje_max
  FROM CBR.TSRI t JOIN CBR.LSRI l ON TO_CHAR(t.LSRI) = l.TABLA WHERE l.TABLA IN ('608', '20') AND t.ESTADO = 1 GROUP BY l.TABLA, t.CODIGO HAVING COUNT(*) > 1 ORDER BY 1, 2;
-- 7c. Retenciones V2 ya emitidas cuyo valor NO es base × porcentaje (diferencia > 1 centavo) o cuyo porcentaje difiere de la tarifa activa de ese código: lo que el navegador mandó
-- y el servidor no comprobó. Solo informativo: no se corrige lo ya emitido.
SELECT d.ID, d.CODIMPUESTO, d.CODRETENCION, d.BASEIMPONIBLE, d.PORCENTAJERETEN, d.VALORRETEN,
       ROUND(d.BASEIMPONIBLE * d.PORCENTAJERETEN / 100, 2) AS valor_recalculado, ROUND(d.VALORRETEN - d.BASEIMPONIBLE * d.PORCENTAJERETEN / 100, 2) AS diferencia,
       (SELECT MIN(t.PORCENTAJE) FROM CBR.TSRI t JOIN CBR.LSRI l ON TO_CHAR(t.LSRI) = l.TABLA WHERE l.TABLA IN ('608', '20') AND t.ESTADO = 1 AND t.CODIGO = d.CODRETENCION) AS porcentaje_tarifa_activa
  FROM CBR.DRV2 d
 WHERE ABS(d.VALORRETEN - ROUND(d.BASEIMPONIBLE * d.PORCENTAJERETEN / 100, 2)) > 0.01
    OR d.PORCENTAJERETEN <> NVL((SELECT MIN(t.PORCENTAJE) FROM CBR.TSRI t JOIN CBR.LSRI l ON TO_CHAR(t.LSRI) = l.TABLA WHERE l.TABLA IN ('608', '20') AND t.ESTADO = 1 AND t.CODIGO = d.CODRETENCION), d.PORCENTAJERETEN)
 ORDER BY ABS(d.VALORRETEN - d.BASEIMPONIBLE * d.PORCENTAJERETEN / 100) DESC;

-- ============================ #18 RAZÓN SOCIAL =======================================
-- 18a. Titulares cuyo NOMBRE está vacío y tienen RAZÓN SOCIAL: con el código actual salen con la razón social vacía en el XML y el SRI los rechaza.
SELECT COUNT(*) AS titulares_solo_razon_social FROM TSR.TTLR t
 WHERE (t.TTLRNMBR IS NULL OR TRIM(t.TTLRNMBR) IS NULL) AND t.TTLRRZSC IS NOT NULL AND TRIM(t.TTLRRZSC) IS NOT NULL;
-- 18b. Titulares sin nombre ni razón social (no se pueden emitir a su nombre).
SELECT COUNT(*) AS titulares_sin_nombre_ni_razon FROM TSR.TTLR t
 WHERE (t.TTLRNMBR IS NULL OR TRIM(t.TTLRNMBR) IS NULL) AND (t.TTLRRZSC IS NULL OR TRIM(t.TTLRRZSC) IS NULL);
