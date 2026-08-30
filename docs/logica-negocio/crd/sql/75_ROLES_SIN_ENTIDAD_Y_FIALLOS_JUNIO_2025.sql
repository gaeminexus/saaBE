-- =============================================================================
-- 75 - DOS CASOS SUELTOS DE JUNIO 2025: ROLES SIN ENTIDAD, Y FIALLOS PACHECO
-- =============================================================================
-- FECHA: 2026-08-27
--
-- PARTE A — SIETE ROLES DEL ARCHIVO (carga 352) QUE NO RESUELVEN CONTRA NINGUNA ENTIDAD
--   Petro les descontó $746,74 en junio 2025 (ver control de 66_PRODUCCION_JUNIO_2025_...
--   sql, "Roles del archivo que no resuelven a ninguna entidad", 7 filas). Su PXCACDPT no
--   coincide con ningún ENTD.ENTDRLPC. Puede que existan en CRD.ENTD con el rol vacío o
--   distinto (cambio de rol no reflejado, o el rol nunca se cargó). Este script los busca
--   por NOMBRE (no hay cédula del archivo Petro para cruzar) y, solo para los que resultan
--   en UNA ÚNICA coincidencia inequívoca, deja preparado — COMENTADO — el UPDATE de
--   ENTDRLPC y el INSERT de su aporte de junio 2025, con el mismo formato que usa
--   66_PRODUCCION_JUNIO_2025_Y_NORMALIZACION.sql (bloques A.3/A.5).
--
-- PARTE B — FIALLOS PACHECO (cédula 0603715772)
--   Tiene $154,85 registrados en junio 2025 cuando el archivo descontó $49,16. Ya lo marcó
--   66 como "un ajuste manual anterior a este trabajo, no algo que provoque el script" (ver
--   su bloque A.6, exceso 105,69 medido en local — la cifra de producción puede variar
--   levemente). Esta parte SOLO consulta el detalle de sus filas de junio: no corrige nada.
--   La decisión (¿fue un ajuste deliberado o un error?) es del usuario.
--
-- NADA DE ESTE SCRIPT SE EJECUTA AUTOMÁTICAMENTE. Los INSERT/UPDATE de la parte A están
-- COMENTADOS a propósito y requieren revisar el resultado del bloque A.1 primero. NO
-- EJECUTAR NINGÚN BLOQUE DE ESCRITURA SIN REVISIÓN MANUAL PREVIA.
-- SQL PURO: sin SET / DEFINE / WHENEVER.
--
-- ÍNDICE
--   A.1  Búsqueda de candidatos por nombre (solo lectura)
--   A.2  Candidatos con más de una coincidencia — requieren decisión manual, no se tocan
--   A.3  UPDATE de ENTDRLPC — COMENTADO — solo para coincidencia única
--   A.4  INSERT del aporte de junio 2025 — COMENTADO — depende de A.3
--   A.5  INSERT del pago (PGAP) — COMENTADO — depende de A.4
--   A.6  Verificación — COMENTADO
--   A.7  Reverso — COMENTADO
--   B.1  Fiallos Pacheco — detalle de sus filas de junio 2025 (solo lectura)
-- =============================================================================


-- =============================================================================
-- A.1 BÚSQUEDA DE CANDIDATOS POR NOMBRE — SOLO LECTURA
-- =============================================================================
-- Compara contra ENTDRZNS sin acentos y en mayúsculas, exigiendo que TODOS los tokens del
-- nombre buscado aparezcan (en cualquier orden), para tolerar diferencias de formato entre
-- el archivo Petro y CRD.ENTD. Revisar cada fila a mano: un match de tokens no es una
-- cédula, puede haber falsos positivos con nombres comunes.
-- =============================================================================
WITH BUSCADOS AS (
        SELECT 510  AS ROL, 74.80  AS DESCONTADO, 'GONZALEZ BALSECA LEONARDO GONZALO' AS NOMBRE_BUSCADO FROM DUAL UNION ALL
        SELECT 611  AS ROL, 168.15 AS DESCONTADO, 'GARCIA GONZALEZ YURI IVAN'          AS NOMBRE_BUSCADO FROM DUAL UNION ALL
        SELECT 5950 AS ROL, 109.20 AS DESCONTADO, 'RODRIGUEZ SILVA DIEGO FERNANDO'     AS NOMBRE_BUSCADO FROM DUAL UNION ALL
        SELECT 8752 AS ROL, 72.00  AS DESCONTADO, 'VILLACIS SALAZAR DARWIN KIELFER'    AS NOMBRE_BUSCADO FROM DUAL UNION ALL
        SELECT 1660 AS ROL, 160.05 AS DESCONTADO, 'CEDENO CEDENO WILMER ESNEYDER'      AS NOMBRE_BUSCADO FROM DUAL UNION ALL
        SELECT 2549 AS ROL, 111.79 AS DESCONTADO, 'BRITO MALDONADO ANGEL EDUARDO'      AS NOMBRE_BUSCADO FROM DUAL UNION ALL
        SELECT 8918 AS ROL, 50.75  AS DESCONTADO, 'ERAZO ROMAN CARLOS ANDRES'          AS NOMBRE_BUSCADO FROM DUAL
),
NORMALIZADO AS (
        SELECT  b.ROL, b.DESCONTADO, b.NOMBRE_BUSCADO,
                e.ENTDCDGO, e.ENTDNMID, e.ENTDRZNS, e.ENTDRLPC, e.ENTDIDST, e.FLLLCDGO,
                TRANSLATE(UPPER(e.ENTDRZNS), 'ÁÉÍÓÚÑ', 'AEIOUN') AS RAZON_NORMALIZADA
        FROM    BUSCADOS b
        CROSS   JOIN CRD.ENTD e
)
SELECT  n.ROL, n.DESCONTADO, n.NOMBRE_BUSCADO,
        n.ENTDCDGO, n.ENTDNMID AS CEDULA_ACTUAL, n.ENTDRZNS AS RAZON_SOCIAL_ACTUAL,
        n.ENTDRLPC AS ROL_ACTUAL, n.ENTDIDST AS ESTADO_PARTICIPE, n.FLLLCDGO AS FILIAL,
        CASE WHEN n.ENTDRLPC IS NULL THEN 'SIN ROL' ELSE 'ROL DISTINTO: ' || n.ENTDRLPC END AS OBSERVACION
FROM    NORMALIZADO n
WHERE   n.RAZON_NORMALIZADA LIKE '%' || REGEXP_SUBSTR(n.NOMBRE_BUSCADO, '\S+', 1, 1) || '%'
AND     n.RAZON_NORMALIZADA LIKE '%' || REGEXP_SUBSTR(n.NOMBRE_BUSCADO, '\S+', 1, 2) || '%'
AND     n.RAZON_NORMALIZADA LIKE '%' || REGEXP_SUBSTR(n.NOMBRE_BUSCADO, '\S+', 1, 3) || '%'
AND     n.RAZON_NORMALIZADA LIKE '%' || REGEXP_SUBSTR(n.NOMBRE_BUSCADO, '\S+', 1, 4) || '%'
ORDER BY n.ROL, n.ENTDCDGO;


-- =============================================================================
-- A.2 CANDIDATOS CON MÁS DE UNA COINCIDENCIA — no se tocan, requieren decisión manual
-- =============================================================================
WITH BUSCADOS AS (
        SELECT 510  AS ROL, 'GONZALEZ BALSECA LEONARDO GONZALO' AS NOMBRE_BUSCADO FROM DUAL UNION ALL
        SELECT 611  AS ROL, 'GARCIA GONZALEZ YURI IVAN'          AS NOMBRE_BUSCADO FROM DUAL UNION ALL
        SELECT 5950 AS ROL, 'RODRIGUEZ SILVA DIEGO FERNANDO'     AS NOMBRE_BUSCADO FROM DUAL UNION ALL
        SELECT 8752 AS ROL, 'VILLACIS SALAZAR DARWIN KIELFER'    AS NOMBRE_BUSCADO FROM DUAL UNION ALL
        SELECT 1660 AS ROL, 'CEDENO CEDENO WILMER ESNEYDER'      AS NOMBRE_BUSCADO FROM DUAL UNION ALL
        SELECT 2549 AS ROL, 'BRITO MALDONADO ANGEL EDUARDO'      AS NOMBRE_BUSCADO FROM DUAL UNION ALL
        SELECT 8918 AS ROL, 'ERAZO ROMAN CARLOS ANDRES'          AS NOMBRE_BUSCADO FROM DUAL
),
NORMALIZADO AS (
        SELECT  b.ROL, b.NOMBRE_BUSCADO, e.ENTDCDGO,
                TRANSLATE(UPPER(e.ENTDRZNS), 'ÁÉÍÓÚÑ', 'AEIOUN') AS RAZON_NORMALIZADA
        FROM    BUSCADOS b
        CROSS   JOIN CRD.ENTD e
),
COINCIDENCIAS AS (
        SELECT  n.ROL, n.NOMBRE_BUSCADO, n.ENTDCDGO
        FROM    NORMALIZADO n
        WHERE   n.RAZON_NORMALIZADA LIKE '%' || REGEXP_SUBSTR(n.NOMBRE_BUSCADO, '\S+', 1, 1) || '%'
        AND     n.RAZON_NORMALIZADA LIKE '%' || REGEXP_SUBSTR(n.NOMBRE_BUSCADO, '\S+', 1, 2) || '%'
        AND     n.RAZON_NORMALIZADA LIKE '%' || REGEXP_SUBSTR(n.NOMBRE_BUSCADO, '\S+', 1, 3) || '%'
        AND     n.RAZON_NORMALIZADA LIKE '%' || REGEXP_SUBSTR(n.NOMBRE_BUSCADO, '\S+', 1, 4) || '%'
)
SELECT  c.ROL, c.NOMBRE_BUSCADO, COUNT(*) AS COINCIDENCIAS
FROM    COINCIDENCIAS c
GROUP BY c.ROL, c.NOMBRE_BUSCADO
HAVING  COUNT(*) <> 1
ORDER BY 1;
-- Esperado: 0 o pocas filas. Cualquier ROL que aparezca aquí (0 coincidencias o más de 1)
-- queda FUERA de los bloques A.3/A.4/A.5: esos exigen coincidencia única.


-- =============================================================================
-- A.3 UPDATE DE ENTDRLPC — ⛔ COMENTADO. Revisar A.1/A.2 antes de descomentar.
-- =============================================================================
-- Solo actualiza el rol de los candidatos con EXACTAMENTE una coincidencia (repite el
-- filtro de A.2, invertido). Guardarraíl adicional: no toca una entidad cuyo ENTDRLPC ya
-- esté ocupado por otro rol activo — revisar a mano si CANDIDATOS_UNICOS trae menos de 7.
--
-- WITH BUSCADOS AS (
--         SELECT 510  AS ROL, 'GONZALEZ BALSECA LEONARDO GONZALO' AS NOMBRE_BUSCADO FROM DUAL UNION ALL
--         SELECT 611  AS ROL, 'GARCIA GONZALEZ YURI IVAN'          AS NOMBRE_BUSCADO FROM DUAL UNION ALL
--         SELECT 5950 AS ROL, 'RODRIGUEZ SILVA DIEGO FERNANDO'     AS NOMBRE_BUSCADO FROM DUAL UNION ALL
--         SELECT 8752 AS ROL, 'VILLACIS SALAZAR DARWIN KIELFER'    AS NOMBRE_BUSCADO FROM DUAL UNION ALL
--         SELECT 1660 AS ROL, 'CEDENO CEDENO WILMER ESNEYDER'      AS NOMBRE_BUSCADO FROM DUAL UNION ALL
--         SELECT 2549 AS ROL, 'BRITO MALDONADO ANGEL EDUARDO'      AS NOMBRE_BUSCADO FROM DUAL UNION ALL
--         SELECT 8918 AS ROL, 'ERAZO ROMAN CARLOS ANDRES'          AS NOMBRE_BUSCADO FROM DUAL
-- ),
-- NORMALIZADO AS (
--         SELECT  b.ROL, b.NOMBRE_BUSCADO, e.ENTDCDGO,
--                 TRANSLATE(UPPER(e.ENTDRZNS), 'ÁÉÍÓÚÑ', 'AEIOUN') AS RAZON_NORMALIZADA
--         FROM    BUSCADOS b CROSS JOIN CRD.ENTD e
-- ),
-- COINCIDENCIAS AS (
--         SELECT  n.ROL, n.NOMBRE_BUSCADO, n.ENTDCDGO
--         FROM    NORMALIZADO n
--         WHERE   n.RAZON_NORMALIZADA LIKE '%' || REGEXP_SUBSTR(n.NOMBRE_BUSCADO, '\S+', 1, 1) || '%'
--         AND     n.RAZON_NORMALIZADA LIKE '%' || REGEXP_SUBSTR(n.NOMBRE_BUSCADO, '\S+', 1, 2) || '%'
--         AND     n.RAZON_NORMALIZADA LIKE '%' || REGEXP_SUBSTR(n.NOMBRE_BUSCADO, '\S+', 1, 3) || '%'
--         AND     n.RAZON_NORMALIZADA LIKE '%' || REGEXP_SUBSTR(n.NOMBRE_BUSCADO, '\S+', 1, 4) || '%'
-- ),
-- CANDIDATOS_UNICOS AS (
--         SELECT  c.ROL, MIN(c.ENTDCDGO) AS ENTDCDGO
--         FROM    COINCIDENCIAS c
--         GROUP BY c.ROL
--         HAVING  COUNT(*) = 1
-- )
-- UPDATE  CRD.ENTD e
-- SET     e.ENTDRLPC = (SELECT cu.ROL FROM CANDIDATOS_UNICOS cu WHERE cu.ENTDCDGO = e.ENTDCDGO)
-- WHERE   e.ENTDCDGO IN (SELECT cu.ENTDCDGO FROM CANDIDATOS_UNICOS cu);
--
-- COMMIT;


-- =============================================================================
-- A.4 INSERT DEL APORTE DE JUNIO 2025 — ⛔ COMENTADO. Corre DESPUÉS de A.3.
-- =============================================================================
-- Mismo formato y misma prelación (jubilación primero hasta su monto esperado en HSTR,
-- luego cesantía) que 66_PRODUCCION_JUNIO_2025_Y_NORMALIZACION.sql bloque A.3. Si el
-- partícipe no tiene HSTR estado 99, no se inserta nada para él (igual que en 66) — queda
-- para revisión aparte, no se le inventa un reparto.
--
-- INSERT INTO CRD.APRT
--        (ENTDCDGO, FLLLCDGO, TPAPCDGO, APRTVLRR, APRTVLPG, APRTSLDO, APRTIDST,
--         APRTIDAS, APRTFCTR, APRTPRDV, APRTTPMV, APRTGLSA, APRTUSRG, APRTFCRG)
-- WITH BUSCADOS AS (
--         SELECT 510  AS ROL, 74.80  AS DESCONTADO FROM DUAL UNION ALL
--         SELECT 611  AS ROL, 168.15 AS DESCONTADO FROM DUAL UNION ALL
--         SELECT 5950 AS ROL, 109.20 AS DESCONTADO FROM DUAL UNION ALL
--         SELECT 8752 AS ROL, 72.00  AS DESCONTADO FROM DUAL UNION ALL
--         SELECT 1660 AS ROL, 160.05 AS DESCONTADO FROM DUAL UNION ALL
--         SELECT 2549 AS ROL, 111.79 AS DESCONTADO FROM DUAL UNION ALL
--         SELECT 8918 AS ROL, 50.75  AS DESCONTADO FROM DUAL
-- ),
-- HS AS ( SELECT h.ENTDCDGO, NVL(h.HSTRMNAJ,0) AS MONTO_JUB, NVL(h.HSTRMNAC,0) AS MONTO_CES
--         FROM ( SELECT h.*, ROW_NUMBER() OVER (PARTITION BY h.ENTDCDGO
--                       ORDER BY h.HSTRFCIN DESC, h.HSTRCDGO DESC) rn
--                FROM CRD.HSTR h WHERE h.HSTRESTD = 99 ) h WHERE h.rn = 1),
-- BASE AS (
--         SELECT  e.ENTDCDGO, e.FLLLCDGO,
--                 ROUND(LEAST(b.DESCONTADO, hs.MONTO_JUB), 2) AS ASIG_JUB,
--                 ROUND(LEAST(b.DESCONTADO - LEAST(b.DESCONTADO, hs.MONTO_JUB),
--                             hs.MONTO_CES), 2)                AS ASIG_CES
--         FROM    BUSCADOS b
--         JOIN    CRD.ENTD e  ON e.ENTDRLPC = b.ROL     -- exige que A.3 ya haya corrido
--         JOIN    HS hs        ON hs.ENTDCDGO = e.ENTDCDGO
--         WHERE   NOT EXISTS (SELECT 1 FROM CRD.APRT a
--                             WHERE a.ENTDCDGO = e.ENTDCDGO
--                             AND   a.TPAPCDGO IN (9,11) AND a.APRTVLRR > 0
--                             AND   TRUNC(a.APRTFCTR,'MM') = DATE '2025-06-01'))
-- SELECT  b.ENTDCDGO, b.FLLLCDGO, 9, b.ASIG_JUB, b.ASIG_JUB, 0, 4, 352,
--         TO_TIMESTAMP('2025-06-30 23:59:59', 'YYYY-MM-DD HH24:MI:SS'),
--         DATE '2025-06-01', 1,
--         'Aporte Jubilación - Mes 6/2025 - CargaArchivo: 352', 'SAA_AH', SYSTIMESTAMP
-- FROM    BASE b WHERE b.ASIG_JUB > 0.01
-- UNION ALL
-- SELECT  b.ENTDCDGO, b.FLLLCDGO, 11, b.ASIG_CES, b.ASIG_CES, 0, 4, 352,
--         TO_TIMESTAMP('2025-06-30 23:59:59', 'YYYY-MM-DD HH24:MI:SS'),
--         DATE '2025-06-01', 1,
--         'Aporte Cesantía - Mes 6/2025 - CargaArchivo: 352', 'SAA_AH', SYSTIMESTAMP
-- FROM    BASE b WHERE b.ASIG_CES > 0.01;
--
-- COMMIT;


-- =============================================================================
-- A.5 INSERT DEL PAGO (PGAP) — ⛔ COMENTADO. Corre DESPUÉS de A.4.
-- =============================================================================
-- Mismo formato que 66 bloque A.5, para que las consultas del 61/69 (que extraen el id de
-- carga del concepto con REGEXP) sigan cuadrando.
--
-- INSERT INTO CRD.PGAP
--        (APRTCDGO, FLLLCDGO, PGAPVLRR, PGAPFCCN, PGAPCNCP, PGAPFCRG, PGAPUSRG, PGAPIDST)
-- SELECT  a.APRTCDGO, a.FLLLCDGO, a.APRTVLRR,
--         TO_TIMESTAMP('2025-06-30 23:59:59', 'YYYY-MM-DD HH24:MI:SS'),
--         'Pago aporte mes 6/2025 - Partícipe: ' || e.ENTDRLPC
--             || ' (' || SUBSTR(e.ENTDRZNS, 1, 60) || ') - CargaArchivo: 352',
--         SYSTIMESTAMP, 'SISTEMA', 1
-- FROM    CRD.APRT a
-- JOIN    CRD.ENTD e ON e.ENTDCDGO = a.ENTDCDGO
-- WHERE   a.APRTIDAS = 352
-- AND     e.ENTDRLPC IN (510, 611, 5950, 8752, 1660, 2549, 8918)
-- AND     NOT EXISTS (SELECT 1 FROM CRD.PGAP p WHERE p.APRTCDGO = a.APRTCDGO);
--
-- COMMIT;


-- =============================================================================
-- A.6 VERIFICACIÓN — ⛔ COMENTADO
-- =============================================================================
-- SELECT  e.ENTDRLPC AS ROL, e.ENTDNMID AS CEDULA, e.ENTDRZNS AS NOMBRE,
--         COUNT(*) AS FILAS, SUM(a.APRTVLRR) AS TOTAL_REGISTRADO
-- FROM    CRD.APRT a JOIN CRD.ENTD e ON e.ENTDCDGO = a.ENTDCDGO
-- WHERE   e.ENTDRLPC IN (510, 611, 5950, 8752, 1660, 2549, 8918)
-- AND     a.APRTIDAS = 352
-- GROUP BY e.ENTDRLPC, e.ENTDNMID, e.ENTDRZNS
-- ORDER BY 1;
-- -- Esperado: una fila por rol resuelto, TOTAL_REGISTRADO = el DESCONTADO de A.1.


-- =============================================================================
-- A.7 REVERSO — ⛔ COMENTADO. No hace falta respaldo aparte: nada de esto existía antes.
-- =============================================================================
-- DELETE FROM CRD.PGAP WHERE APRTCDGO IN (
--     SELECT a.APRTCDGO FROM CRD.APRT a JOIN CRD.ENTD e ON e.ENTDCDGO = a.ENTDCDGO
--     WHERE a.APRTIDAS = 352 AND e.ENTDRLPC IN (510, 611, 5950, 8752, 1660, 2549, 8918));
-- DELETE FROM CRD.APRT a WHERE a.APRTIDAS = 352 AND EXISTS (
--     SELECT 1 FROM CRD.ENTD e WHERE e.ENTDCDGO = a.ENTDCDGO
--     AND e.ENTDRLPC IN (510, 611, 5950, 8752, 1660, 2549, 8918));
-- -- El ENTDRLPC asignado en A.3 NO se revierte aquí a propósito: si se decidió que la
-- -- coincidencia era correcta, el rol se queda. Revertirlo es una decisión aparte.
-- COMMIT;


-- =============================================================================
-- B.1 FIALLOS PACHECO (cédula 0603715772) — DETALLE DE JUNIO 2025. SOLO LECTURA.
-- =============================================================================
-- No corrige nada: solo muestra glosa, usuario, fecha de registro y APRTIDAS de cada fila
-- de junio 2025, para que el usuario decida si el exceso (registrado 154,85 vs
-- descontado 49,16 en el archivo) fue un ajuste deliberado o un error.
-- =============================================================================
SELECT  a.APRTCDGO, a.TPAPCDGO, tp.TPAPNMBR AS TIPO_APORTE,
        a.APRTVLRR AS VALOR, a.APRTVLPG AS VALOR_PAGADO, a.APRTSLDO AS SALDO,
        a.APRTIDST AS ESTADO, a.APRTIDAS AS ID_CARGA,
        a.APRTFCTR AS FECHA_TRANSACCION, a.APRTFCRG AS FECHA_REGISTRO,
        NVL(a.APRTUSRG, '(null)') AS USUARIO, a.APRTGLSA AS GLOSA
FROM    CRD.APRT a
JOIN    CRD.ENTD e  ON e.ENTDCDGO = a.ENTDCDGO
JOIN    CRD.TPAP tp ON tp.TPAPCDGO = a.TPAPCDGO
WHERE   e.ENTDNMID = '0603715772'
AND     TRUNC(a.APRTFCTR, 'MM') = DATE '2025-06-01'
ORDER BY a.APRTFCTR, a.APRTCDGO;

-- Contexto: lo que el archivo de Petro descontó realmente ese mes, para comparar.
SELECT  x.PXCACDPT AS ROL, x.PXCANMBR AS NOMBRE_ARCHIVO, NVL(x.PXCADSDO, 0) AS DESCONTADO,
        d.CRARCDGO AS ID_CARGA
FROM    CRD.DTCA d
JOIN    CRD.PXCA x ON x.DTCACDGO = d.DTCACDGO
JOIN    CRD.ENTD e ON e.ENTDRLPC = x.PXCACDPT
WHERE   e.ENTDNMID = '0603715772'
AND     d.DTCACDPP = 'AH'
AND     d.CRARCDGO IN (SELECT c.CRARCDGO FROM CRD.CRAR c
                       WHERE c.CRARANAF = 2025 AND c.CRARMSAF = 6);
