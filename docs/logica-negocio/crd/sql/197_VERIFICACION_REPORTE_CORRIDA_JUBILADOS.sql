-- =====================================================================================
-- 197 - Verificacion de la consulta del reporte RPRT_PGPC_CRRD (corrida mensual jubilados)
-- FECHA: 2026-09-05 - EQUIPO: CRD / Equipo B (eqB, omen-saa-1)
--
-- SOLO SELECT. No modifica una sola fila.
-- Sin comandos de SQL*Plus: no usa PROMPT, DEFINE, SET ni &variables.
--
-- Correr esto ANTES de dar el .jrxml por bueno: confirma que la query que usa el
-- reporte devuelve lo esperado contra datos reales, sin necesidad de abrir Jaspersoft
-- Studio. Cambiar los literales 2026 / 8 / 1 de mas abajo por el anio/mes/empresa reales
-- de la corrida que se quiera revisar.
-- =====================================================================================

-- =====================================================================================
-- BLOQUE 1 - Seccion 1 del reporte: pagos generados en la corrida del mes indicado
--            (filtro por PGPCFCRG, fecha de EJECUCION, no por PGPCANNO/PGPCMESS)
-- =====================================================================================
SELECT
    e.ENTDCDGO,
    e.ENTDNMID,
    e.ENTDRZNS,
    p.PGPCCDGO,
    p.PGPCANNO,
    p.PGPCMESS,
    p.PGPCVLPN,
    p.PGPCVLSG,
    p.PGPCVLRR,
    p.PGPCESTD,
    p.PGPCFCHA,
    p.PGPCFCRG,
    p.PGPCFCPG,
    CASE WHEN p.PGPCESTD IN (2,3) THEN p.PGPCVLPN ELSE 0 END AS DINERO_BANCO,
    NVL(cr.CRZ_VALOR, 0) AS CRUZADO_PRESTAMOS,
    NVL(cr.CRZ_DETALLE, 'Sin cruce a prestamo este mes') AS DETALLE_CRUCE_PRESTAMOS
FROM CRD.PGPC p
JOIN CRD.ENTD e ON e.ENTDCDGO = p.ENTDCDGO
LEFT JOIN (
    SELECT a.ENTDCDGO AS ENTDCDGO,
           TRUNC(a.APRTFCTR) AS FECHA,
           SUM(ABS(a.APRTVLRR)) AS CRZ_VALOR,
           LISTAGG(
               'Prestamo ' || REGEXP_SUBSTR(a.APRTGLSA, 'PAGO PRESTAMO (\d+)', 1, 1, NULL, 1)
               || ': $' || TO_CHAR(ABS(a.APRTVLRR), 'FM999999990.00'),
               '; '
           ) WITHIN GROUP (ORDER BY a.APRTCDGO) AS CRZ_DETALLE
    FROM CRD.APRT a
    WHERE a.TPAPCDGO = 23
      AND a.APRTTPMV = 4
    GROUP BY a.ENTDCDGO, TRUNC(a.APRTFCTR)
) cr ON cr.ENTDCDGO = p.ENTDCDGO AND cr.FECHA = p.PGPCFCHA
WHERE p.PGPCFCRG >= TO_DATE('2026-08-01', 'YYYY-MM-DD')
  AND p.PGPCFCRG <  ADD_MONTHS(TO_DATE('2026-08-01', 'YYYY-MM-DD'), 1)
ORDER BY e.ENTDRZNS, p.PGPCANNO, p.PGPCMESS;

-- QUE MIRAR:
--   - Si un jubilado tuvo retroactivo, tiene que aparecer con VARIAS filas (una por
--     PGPCANNO/PGPCMESS distinto), todas con la MISMA PGPCFCRG (mismo dia de corrida).
--   - CRUZADO_PRESTAMOS y DETALLE_CRUCE_PRESTAMOS son una heuristica (entidad + fecha
--     contra CRD.APRT tipo 23/movimiento 4) -- contrastar manualmente un par de casos
--     contra CRD.APRT directamente si hay dudas (bloque 3 mas abajo).

-- =====================================================================================
-- BLOQUE 2 - Seccion 2 del reporte: jubilados JUBILADO_COMPLEMENTARIO sin ninguna fila
--            de PGPC en el rango. La ausencia NO distingue bloqueado de "al dia".
-- =====================================================================================
SELECT
    e.ENTDCDGO,
    e.ENTDNMID,
    e.ENTDRZNS
FROM CRD.ENTD e
WHERE e.ENTDIDST = 3
  AND NOT EXISTS (
      SELECT 1 FROM CRD.PGPC p2
      WHERE p2.ENTDCDGO = e.ENTDCDGO
        AND p2.PGPCFCRG >= TO_DATE('2026-08-01', 'YYYY-MM-DD')
        AND p2.PGPCFCRG <  ADD_MONTHS(TO_DATE('2026-08-01', 'YYYY-MM-DD'), 1)
  )
ORDER BY e.ENTDRZNS;

-- =====================================================================================
-- BLOQUE 3 - Contraste manual del cruce a prestamos de UN jubilado puntual
--            (cambiar el ENTDCDGO de ejemplo, 1, por el que se quiera revisar)
-- =====================================================================================
SELECT
    a.APRTCDGO,
    a.APRTFCTR,
    a.APRTGLSA,
    a.APRTVLRR,
    REGEXP_SUBSTR(a.APRTGLSA, 'PAGO PRESTAMO (\d+)', 1, 1, NULL, 1) AS PRESTAMO_EXTRAIDO
FROM CRD.APRT a
WHERE a.ENTDCDGO = 1
  AND a.TPAPCDGO = 23
  AND a.APRTTPMV = 4
ORDER BY a.APRTFCTR;

-- =====================================================================================
-- BLOQUE 4 - Orden de pago al proveedor del seguro medico del periodo (pie del reporte)
-- =====================================================================================
SELECT
    g.PGTRCDGO,
    g.PGTRORGN,
    g.PGTRIDOR,
    g.PGTRVLOR,
    g.PGTRESTD,
    g.PGTRPJRQ
FROM PGS.PGTR g
WHERE g.PGTRORGN = 'CRD_SEGURO_JUBILADOS'
  AND g.PGTRIDOR = (2026 * 100 + 8)
  AND g.PGTRPJRQ = 1;
