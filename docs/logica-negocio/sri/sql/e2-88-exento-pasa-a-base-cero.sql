-- =====================================================================================
-- e2-88 — Lo cargado como EXENTO de IVA (código 7) pasa a BASE 0%, desde agosto 2026.
-- ⚠️ EL BLOQUE 1 ESCRIBE. Equipo omen-saa-2 · 2026-10-05.
--
-- DECISIÓN DEL USUARIO, 2026-10-05: «opción B». Contabilidad reportó una diferencia de 0,39 en el ATS de
-- agosto: los intereses por mora de las planillas de E.E. Quito y CNEL (0,02 + 0,16 + 0,18 + 0,03) vienen
-- en el XML con código 7 (exento), el ATS los declara en baseImpExe, y la grilla de compras del DIMM NO
-- muestra la base exenta (solo base 0%, base distinta de 0% y no objeto). En el DIMM la factura
-- 001-999-134308072 suma 17,24 + 7,23 = 24,47 en vez de 24,63.
--   (A) lo ya cargado: SUBCERO += SUBEXENT, SUBEXENT = 0 → ESTE script.
--   (B) desde ahora, la carga manda el código 7 a base 0% → cambio de código (BasesPorTarifa).
-- El IVA no cambia: exento y 0% no generan impuesto. Solo cambia la columna donde se declara.
--
-- ALCANCE: los cuatro documentos de compra (FCTC, NTCC, NTDC, LQCC) con FECHA >= 2026-08-01. JULIO NO SE
-- TOCA: ya está declarado (misma decisión del usuario que en el e2-66).
-- NO toca el detalle (CODIGOIVASRI sigue en 7, que es lo que dice el XML del proveedor): el ATS declara
-- con las columnas de cabecera.
-- DESPUÉS: regenerar el ATS de agosto (y el de septiembre, si se generó antes de correr esto).
-- =====================================================================================

-- BLOQUE 0 — CONTROL ANTES. Documentos desde agosto con base exenta.
-- ESPERADO para agosto: 4 facturas (CNEL 092-999-011117120 0,02 · E.E.Q. 134308072 0,16 · 134470375 0,18 ·
-- 134482033 0,03), total 0,39. Septiembre puede traer más: también se pasan.
SELECT 'BLOQUE 0 - antes' AS bloque, x.tabla, x.ID, x.NUMERO, TRUNC(x.FECHA) AS fecha, x.SUBCERO, x.SUBEXENT
  FROM (SELECT 'FCTC' AS tabla, ID, NUMERO, FECHA, SUBCERO, SUBEXENT FROM PGS.FCTC WHERE NVL(SUBEXENT, 0) > 0
        UNION ALL SELECT 'NTCC', ID, NUMERO, FECHA, SUBCERO, SUBEXENT FROM PGS.NTCC WHERE NVL(SUBEXENT, 0) > 0
        UNION ALL SELECT 'NTDC', ID, NUMERO, FECHA, SUBCERO, SUBEXENT FROM PGS.NTDC WHERE NVL(SUBEXENT, 0) > 0
        UNION ALL SELECT 'LQCC', ID, NUMERO, FECHA, SUBCERO, SUBEXENT FROM PGS.LQCC WHERE NVL(SUBEXENT, 0) > 0) x
 WHERE x.FECHA >= DATE '2026-08-01'
 ORDER BY x.FECHA, x.tabla, x.ID;

SELECT 'BLOQUE 0 - totales por mes' AS bloque, TO_CHAR(x.FECHA, 'YYYY-MM') AS mes, COUNT(*) AS documentos,
       SUM(x.SUBEXENT) AS exento
  FROM (SELECT FECHA, SUBEXENT FROM PGS.FCTC WHERE NVL(SUBEXENT, 0) > 0
        UNION ALL SELECT FECHA, SUBEXENT FROM PGS.NTCC WHERE NVL(SUBEXENT, 0) > 0
        UNION ALL SELECT FECHA, SUBEXENT FROM PGS.NTDC WHERE NVL(SUBEXENT, 0) > 0
        UNION ALL SELECT FECHA, SUBEXENT FROM PGS.LQCC WHERE NVL(SUBEXENT, 0) > 0) x
 GROUP BY TO_CHAR(x.FECHA, 'YYYY-MM')
 ORDER BY mes;

-- BLOQUE 1 — El paso a base 0%. Idempotente: si ya corrió, no encuentra nada que mover.
BEGIN
    UPDATE PGS.FCTC SET SUBCERO = NVL(SUBCERO, 0) + SUBEXENT, SUBEXENT = 0
     WHERE NVL(SUBEXENT, 0) > 0 AND FECHA >= DATE '2026-08-01';
    UPDATE PGS.NTCC SET SUBCERO = NVL(SUBCERO, 0) + SUBEXENT, SUBEXENT = 0
     WHERE NVL(SUBEXENT, 0) > 0 AND FECHA >= DATE '2026-08-01';
    UPDATE PGS.NTDC SET SUBCERO = NVL(SUBCERO, 0) + SUBEXENT, SUBEXENT = 0
     WHERE NVL(SUBEXENT, 0) > 0 AND FECHA >= DATE '2026-08-01';
    UPDATE PGS.LQCC SET SUBCERO = NVL(SUBCERO, 0) + SUBEXENT, SUBEXENT = 0
     WHERE NVL(SUBEXENT, 0) > 0 AND FECHA >= DATE '2026-08-01';
END;
/

COMMIT;

-- BLOQUE 2 — CONTROL DESPUÉS.
-- 2.1 ESPERADO: CERO filas desde agosto con base exenta. Si aparecen, el COMMIT no corrió.
SELECT 'BLOQUE 2.1 - exento restante' AS bloque, COUNT(*) AS documentos
  FROM (SELECT FECHA FROM PGS.FCTC WHERE NVL(SUBEXENT, 0) > 0
        UNION ALL SELECT FECHA FROM PGS.NTCC WHERE NVL(SUBEXENT, 0) > 0
        UNION ALL SELECT FECHA FROM PGS.NTDC WHERE NVL(SUBEXENT, 0) > 0
        UNION ALL SELECT FECHA FROM PGS.LQCC WHERE NVL(SUBEXENT, 0) > 0) x
 WHERE x.FECHA >= DATE '2026-08-01';
-- 2.2 La factura del ejemplo. ESPERADO: SUBCERO 17,40 (17,24 + 0,16), SUBEXENT 0. En el DIMM: 17,40 + 7,23 = 24,63.
SELECT 'BLOQUE 2.2 - 134308072' AS bloque, f.ID, f.NUMERO, f.SUBCERO, f.SUBNOOBJ, f.SUBEXENT, f.TOTAL
  FROM PGS.FCTC f WHERE f.NUMERO LIKE '%134308072';

-- REVERSO (comentado): no es automático, porque después del UPDATE ya no se distingue qué parte de
-- SUBCERO era exenta. Si hiciera falta, se reconstruye desde el detalle (CODIGOIVASRI = 7), como hacía
-- el e2-66.
