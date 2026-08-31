-- =============================================================================
-- 08 — REUBICACION DEL DEVENGO: la corrección. ESTE SCRIPT SÍ ESCRIBE.
-- =============================================================================
-- FECHA: 2026-08-31 · Equipo omen-saa-2 · Autorizado por el usuario ("hazlo tú")
--
-- Implementa 02_ALGORITMO_REUBICACION_MESES.md y corrige lo que el backfill 63
-- dejó fuera de alcance (README §13).
--
-- ⚠️ NO SE EJECUTA DE CORRIDO SIN LEER. Los bloques 0 son de lectura y hay que
--    mirarlos ANTES de llegar al bloque 2, que es el único que escribe. El
--    reverso (bloque 4) está COMENTADO a propósito.
--
-- ⚠️ AVISAR AL EQUIPO A ANTES DE EJECUTAR. CRD.APRT es su tabla y hay un lector
--    contable colgando de ella. Compromiso tomado el 2026-08-31.
--
-- -----------------------------------------------------------------------------
-- QUÉ ESCRIBE, EXACTAMENTE
-- -----------------------------------------------------------------------------
--   APRTPRDV  el mes al que pertenece el aporte  ← ÚNICO cambio de significado
--   APRTGLSA  se le inserta la traza de la reubicación
--
--   NO TOCA: APRTVLRR, APRTVLPG, APRTSLDO, APRTIDST, APRTFCTR (fecha de CAJA),
--            APRTIDAS, CRARCDGO, ni una sola fila de CRD.PGAP.
--
--   Por lo tanto el asiento de la carga Petro NO cambia:
--   CobroPetroContableServiceImpl.contabilizarAplicacion suma por
--   sumValorPorTipoAporteByCarga, que agrupa por a.idAsoprep y suma a.valor.
--   Ninguna de las dos columnas se toca (verificado con el árbitro del equipo A,
--   que retiró su objeción sobre este punto el 2026-08-31).
--
-- -----------------------------------------------------------------------------
-- QUÉ CORRIGE
-- -----------------------------------------------------------------------------
--   1. Los 651 meses hueco que el 63 no pudo cerrar (solo miraba hacia atrás).
--   2. ⛔ Las 854 filas que el 63 dejó con devengo ANTERIOR a 2025-06-01 —
--      744 partícipes, $55.765,33— invisibles para el cálculo del faltante y por
--      lo tanto un doble cobro esperando a la primera carga con devengo.
--
--      ATRIBUCIÓN, y va escrita a pedido del árbitro del equipo A: **el defecto
--      nació en 63_BACKFILL_DEVENGO_APORTES.sql, que es del frente de devengo
--      (equipo A), no en este frente.** Su regla 2 reparte meses hacia atrás con
--      un CONNECT BY de 24 meses, sin consultar CRD.VGCN y sin piso en 2025-06.
--      Su árbitro lo verificó, lo asumió, y decidió que la corrección se hiciera
--      acá en vez de escribir una segunda versión del 63 — para no tener dos
--      UPDATE compitiendo sobre las mismas filas.
--
--      ⚠️ Y ES UN OBJETIVO EXPLÍCITO DE ESTE SCRIPT, no un efecto colateral de la
--      compactación. Los controles 0.3, 3.3 y 3.3b existen solo para eso: si se
--      cambia el orden o el criterio de cupos y estas filas dejan de corregirse,
--      3.3b lo dice con nombre y apellido.
--
-- -----------------------------------------------------------------------------
-- ALCANCE — y qué queda deliberadamente afuera
-- -----------------------------------------------------------------------------
--   ENTRAN  partícipes con contrato ACTIVO y vigencia útil (1.640 medidos), cuyo
--           dinero cuadra (|registrado - descontado| <= 0.02).
--   NO ENTRAN, y cada uno por su razón:
--     · 404 partícipes SIN contrato ACTIVO (1.972 filas, $132.782,01). Sin grilla
--       no hay mes destino. NO se les fabrica una desde su propio historial de
--       aportes: sería deducir el contrato de los pagos, al revés de como debe
--       leerse. Se corrigen cuando existan los contratos.
--     · 27 partícipes cuyo dinero NO cuadra (16 inflados por $2.832,99, 11 con
--       $8.292,23 de menos). Reubicar un saldo inflado convierte el exceso en
--       historia falsificada: meses impagos que quedan como pagados. Van primero
--       por la depuración del exceso.
--     · Filas manuales, negativas, REVERSO/PAGO PRESTAMO/DEVOLUCION y
--       EXCEDENTE_PETRO (tipoMovimiento 8): no son de la carga y no cubren mes.
--
-- -----------------------------------------------------------------------------
-- ⚠️ DIFERENCIA CON EL 03, Y ES IMPORTANTE
-- -----------------------------------------------------------------------------
--   El 03 filtraba las filas móviles por PERIODO_EFECTIVO BETWEEN piso y techo.
--   Eso DEJABA AFUERA justo las 854 filas con devengo bajo el piso, que son las
--   que más hay que corregir. Acá el universo se define por la FECHA DE CAJA
--   (APRTFCTR >= 2025-06-01), que es la que dice si la fila vino de una carga, y
--   el devengo se toma como está, valga lo que valga.
--
-- ÍNDICE
--   0  Controles PREVIOS — leer antes de nada
--   1  Respaldo
--   2  El UPDATE (MERGE)
--   3  Controles POSTERIORES — revisar ANTES del COMMIT
--   4  Reverso — COMENTADO
-- =============================================================================


-- =============================================================================
-- 0. CONTROLES PREVIOS
-- =============================================================================

-- 0.1 La tabla de respaldo NO debe existir todavía. Esperado: 0 filas.
--     Si existe, este script ya corrió: PARAR y revisar antes de repetir.
SELECT COUNT(*) AS RESPALDO_YA_EXISTE
FROM   ALL_TABLES
WHERE  OWNER = 'CRD' AND TABLE_NAME = 'BKP_APRT_DEVENGO_20260831';

-- 0.2 Parámetros efectivos. TECHO = última carga procesada, NO sysdate.
SELECT DATE '2025-06-01' AS PISO,
       (SELECT MAX(TO_DATE(TO_CHAR(c.CRARANAF) || LPAD(TO_CHAR(c.CRARMSAF), 2, '0'), 'YYYYMM'))
          FROM CRD.CRAR c WHERE c.CRARESTD = 3) AS TECHO
FROM   DUAL;

-- 0.3 El daño a corregir, ANTES. Guardar estos números: el bloque 3.4 los compara.
SELECT COUNT(*)                          AS FILAS_BAJO_EL_PISO,
       COUNT(DISTINCT a.ENTDCDGO)        AS PARTICIPES,
       ROUND(SUM(a.APRTVLRR), 2)         AS VALOR
FROM   CRD.APRT a
WHERE  a.TPAPCDGO IN (9, 11) AND a.APRTVLRR > 0
AND    a.APRTPRDV IS NOT NULL AND a.APRTPRDV < DATE '2025-06-01';


-- =============================================================================
-- 1. RESPALDO — antes de tocar nada
-- =============================================================================
-- Guarda las cuatro columnas que permiten revertir Y comprobar que no se movió
-- dinero: las dos que se escriben, más valor y fecha de caja, que NO se escriben
-- y por eso sirven de testigo en el control 3.1.
CREATE TABLE CRD.BKP_APRT_DEVENGO_20260831 AS
SELECT a.APRTCDGO, a.APRTPRDV, a.APRTGLSA, a.APRTVLRR, a.APRTFCTR
FROM   CRD.APRT a
WHERE  a.TPAPCDGO IN (9, 11)
AND    a.APRTVLRR > 0
AND    a.APRTFCTR >= DATE '2025-06-01';

-- Control: el respaldo tiene que traer el universo completo (~32.312 filas).
SELECT COUNT(*) AS FILAS_RESPALDADAS FROM CRD.BKP_APRT_DEVENGO_20260831;


-- =============================================================================
-- 2. EL UPDATE — MERGE. ES LO ÚNICO QUE ESCRIBE.
-- =============================================================================
-- Asignación: se compactan las filas móviles del par (entidad, tipo) contra los
-- cupos disponibles, en orden. Cupo i para la fila i.
--
-- ⚠️ ORDEN DE LAS FILAS — decisión D8, es UNA sola línea (marcada abajo).
--    Hoy: por MES DE CAJA y luego por código. O sea, la plata que entró primero
--    cubre el mes adeudado más viejo. Es el orden del dinero.
--    Si el usuario prefiere el otro criterio (por devengo actual), se cambia
--    ORDER BY TRUNC(a.APRTFCTR,'MM') por ORDER BY a.PERIODO_EFECTIVO. Nada más.
--
-- IDEMPOTENTE por dos vías: no toca filas cuya glosa ya lleva '(reubicado desde',
-- y no toca filas cuyo cupo asignado es el mes que ya tenían.
MERGE INTO CRD.APRT dest
USING (
    WITH PARAM AS (
            SELECT DATE '2025-06-01' AS PISO,
                   (SELECT MAX(TO_DATE(TO_CHAR(c.CRARANAF) || LPAD(TO_CHAR(c.CRARMSAF), 2, '0'), 'YYYYMM'))
                      FROM CRD.CRAR c WHERE c.CRARESTD = 3) AS TECHO
            FROM DUAL
    ),
    MESES AS (
            SELECT ADD_MONTHS(p.PISO, LEVEL - 1) AS MES FROM PARAM p
            CONNECT BY LEVEL <= MONTHS_BETWEEN(p.TECHO, p.PISO) + 1
    ),
    -- Universo por FECHA DE CAJA (ver la nota del encabezado), no por devengo.
    APORTES AS (
            SELECT  a.APRTCDGO, a.ENTDCDGO, a.TPAPCDGO, a.APRTVLRR, a.APRTGLSA,
                    a.APRTPRDV, a.APRTFCTR,
                    COALESCE(a.APRTPRDV, TRUNC(a.APRTFCTR, 'MM'))       AS PERIODO_EFECTIVO,
                    CASE WHEN (    a.APRTFCTR >= DATE '2025-06-01'
                               AND (   a.CRARCDGO IS NOT NULL
                                    OR EXISTS (SELECT 1 FROM CRD.CRAR cr WHERE cr.CRARCDGO = a.APRTIDAS)
                                    OR a.APRTGLSA LIKE 'Aporte %CargaArchivo: %'
                                    OR a.APRTGLSA LIKE 'Abono al aporte%'))
                         THEN 'MOVIL' ELSE 'FIJA' END                    AS CLASE
            FROM    CRD.APRT a
            WHERE   a.TPAPCDGO IN (9, 11)
            AND     a.APRTVLRR > 0
            AND     NVL(a.APRTTPMV, 1) <> 8
            AND     a.APRTGLSA NOT LIKE 'REVERSO%'
            AND     a.APRTGLSA NOT LIKE 'PAGO PRESTAMO%'
            AND     a.APRTGLSA NOT LIKE 'DEVOLUCION%'
    ),
    -- Elegibilidad: el dinero del partícipe cuadra contra lo descontado.
    DESCONTADO AS (
            SELECT  e.ENTDCDGO, SUM(NVL(x.PXCADSDO, 0)) AS DESCONTADO
            FROM    CRD.DTCA d
            JOIN    CRD.PXCA x ON x.DTCACDGO = d.DTCACDGO
            JOIN    CRD.CRAR c ON c.CRARCDGO = d.CRARCDGO
            JOIN    CRD.ENTD e ON e.ENTDRLPC = x.PXCACDPT
            WHERE   d.DTCACDPP = 'AH' AND c.CRARESTD = 3
            GROUP BY e.ENTDCDGO
    ),
    ELEGIBLE AS (
            SELECT  a.ENTDCDGO
            FROM    APORTES a
            LEFT    JOIN DESCONTADO d ON d.ENTDCDGO = a.ENTDCDGO
            WHERE   a.CLASE = 'MOVIL'
            GROUP BY a.ENTDCDGO, d.DESCONTADO
            HAVING  ABS(SUM(a.APRTVLRR) - NVL(MAX(d.DESCONTADO), 0)) <= 0.02
    ),
    CONTRATO_ACTIVO AS (
            SELECT c.ENTDCDGO, MAX(c.CNTRCDGO) AS CNTRCDGO
            FROM   CRD.CNTR c WHERE c.CNTRESTD = 1 GROUP BY c.ENTDCDGO
    ),
    -- La grilla: meses en que ESE partícipe debía aportar ESE tipo.
    ESPERADO AS (
            SELECT  DISTINCT ca.ENTDCDGO, v.TPAPCDGO, m.MES
            FROM    MESES m
            CROSS   JOIN CONTRATO_ACTIVO ca
            JOIN    CRD.VGCN v ON v.CNTRCDGO = ca.CNTRCDGO
            WHERE   v.TPAPCDGO IN (9, 11) AND v.VGCNIDST = 1 AND NVL(v.VGCNMNTO, 0) > 0
            AND     v.VGCNFCIN <= LAST_DAY(m.MES)
            AND     (v.VGCNFCFN IS NULL OR v.VGCNFCFN >= LAST_DAY(m.MES))
    ),
    -- Un mes ocupado por una fila FIJA no es cupo.
    OCUPADO_FIJO AS (
            SELECT DISTINCT a.ENTDCDGO, a.TPAPCDGO, a.PERIODO_EFECTIVO AS MES
            FROM   APORTES a WHERE a.CLASE = 'FIJA' AND a.PERIODO_EFECTIVO IS NOT NULL
    ),
    CUPOS AS (
            SELECT  e.ENTDCDGO, e.TPAPCDGO, e.MES,
                    ROW_NUMBER() OVER (PARTITION BY e.ENTDCDGO, e.TPAPCDGO ORDER BY e.MES) AS RN
            FROM    ESPERADO e
            WHERE   NOT EXISTS (SELECT 1 FROM OCUPADO_FIJO o
                                WHERE o.ENTDCDGO = e.ENTDCDGO AND o.TPAPCDGO = e.TPAPCDGO AND o.MES = e.MES)
    ),
    MOVILES AS (
            SELECT  a.APRTCDGO, a.ENTDCDGO, a.TPAPCDGO, a.APRTGLSA, a.PERIODO_EFECTIVO,
                    ROW_NUMBER() OVER (PARTITION BY a.ENTDCDGO, a.TPAPCDGO
                                       -- ◄◄◄ D8: EL ORDEN. Cambiar aquí y en ningún otro lado.
                                       ORDER BY TRUNC(a.APRTFCTR, 'MM'), a.APRTCDGO) AS RN
            FROM    APORTES a
            JOIN    ELEGIBLE el ON el.ENTDCDGO = a.ENTDCDGO
            WHERE   a.CLASE = 'MOVIL'
            AND     a.APRTGLSA NOT LIKE '%(reubicado desde%'
    )
    SELECT  m.APRTCDGO,
            c.MES AS MES_DESTINO,
            CASE
                WHEN m.APRTGLSA LIKE 'Aporte % - Mes %/% - CargaArchivo: %'
                THEN REGEXP_REPLACE(m.APRTGLSA,
                        ' - Mes [0-9]{1,2}/[0-9]{4} - CargaArchivo: ',
                        ' - Mes ' || TO_CHAR(EXTRACT(MONTH FROM c.MES)) || '/' || TO_CHAR(EXTRACT(YEAR FROM c.MES))
                        || ' (reubicado desde ' || TO_CHAR(EXTRACT(MONTH FROM m.PERIODO_EFECTIVO))
                        || '/' || TO_CHAR(EXTRACT(YEAR FROM m.PERIODO_EFECTIVO)) || ')'
                        || ' - CargaArchivo: ')
                ELSE REGEXP_REPLACE(m.APRTGLSA,
                        ' - CargaArchivo: ',
                        ' (reubicado desde ' || TO_CHAR(EXTRACT(MONTH FROM m.PERIODO_EFECTIVO))
                        || '/' || TO_CHAR(EXTRACT(YEAR FROM m.PERIODO_EFECTIVO)) || ')'
                        || ' - CargaArchivo: ')
            END AS GLOSA_NUEVA
    FROM    MOVILES m
    JOIN    CUPOS c ON c.ENTDCDGO = m.ENTDCDGO AND c.TPAPCDGO = m.TPAPCDGO AND c.RN = m.RN
    WHERE   c.MES <> m.PERIODO_EFECTIVO
) src
ON (dest.APRTCDGO = src.APRTCDGO)
WHEN MATCHED THEN UPDATE SET dest.APRTPRDV = src.MES_DESTINO,
                             dest.APRTGLSA = src.GLOSA_NUEVA;

-- ⛔ NO HACER COMMIT TODAVÍA. Correr el bloque 3 primero.


-- =============================================================================
-- 3. CONTROLES POSTERIORES — revisar ANTES del COMMIT
-- =============================================================================

-- 3.1 ⛔ EL CONTROL QUE MANDA: no se movió ni un centavo, ni una fecha de caja.
--     Esperado: 0 filas. Si sale cualquier cosa, ROLLBACK inmediato.
SELECT COUNT(*) AS FILAS_CON_VALOR_O_CAJA_ALTERADOS
FROM   CRD.APRT a
JOIN   CRD.BKP_APRT_DEVENGO_20260831 b ON b.APRTCDGO = a.APRTCDGO
WHERE  a.APRTVLRR <> b.APRTVLRR
   OR  a.APRTFCTR <> b.APRTFCTR;

-- 3.2 El saldo de cada partícipe es idéntico. Esperado: 0 filas.
SELECT COUNT(*) AS PARTICIPES_CON_SALDO_DISTINTO
FROM ( SELECT a.ENTDCDGO, SUM(a.APRTVLRR) AS AHORA
       FROM   CRD.APRT a JOIN CRD.BKP_APRT_DEVENGO_20260831 b ON b.APRTCDGO = a.APRTCDGO
       GROUP  BY a.ENTDCDGO ) x
JOIN ( SELECT a.ENTDCDGO, SUM(b.APRTVLRR) AS ANTES
       FROM   CRD.APRT a JOIN CRD.BKP_APRT_DEVENGO_20260831 b ON b.APRTCDGO = a.APRTCDGO
       GROUP  BY a.ENTDCDGO ) y ON y.ENTDCDGO = x.ENTDCDGO
WHERE  ABS(x.AHORA - y.ANTES) > 0.001;

-- 3.3 ⛔ Ya no queda ninguna fila reubicada con devengo bajo el piso.
--     Esperado: 0. Lo que quede son partícipes fuera del alcance (sin contrato o
--     con el dinero descuadrado): compararlo contra el 0.3 para saber cuántos.
SELECT COUNT(*) AS FILAS_BAJO_EL_PISO_DESPUES,
       COUNT(DISTINCT a.ENTDCDGO) AS PARTICIPES,
       ROUND(SUM(a.APRTVLRR), 2)  AS VALOR
FROM   CRD.APRT a
WHERE  a.TPAPCDGO IN (9, 11) AND a.APRTVLRR > 0
AND    a.APRTPRDV IS NOT NULL AND a.APRTPRDV < DATE '2025-06-01';

-- 3.3b ⛔ OBJETIVO EXPLÍCITO, NO EFECTO COLATERAL — pedido del árbitro del equipo A.
--      Lista, con nombre, las filas que SEGUÍAN bajo el piso y NO se corrigieron.
--      Cada una tiene que caer en una de las dos exclusiones declaradas (partícipe
--      sin contrato ACTIVO, o dinero descuadrado). Si aparece una que no encaja en
--      ninguna, la reubicación no cubrió lo que decía cubrir: PARAR y revisar.
--
--      Este bloque existe para que el arreglo de las 854 no dependa de que alguien
--      recuerde que la compactación también las arreglaba. Si mañana se cambia el
--      orden o el criterio de cupos y estas filas dejan de corregirse, este control
--      lo dice; el 3.3, que solo cuenta, no lo diría con la misma claridad.
SELECT  e.ENTDNMID                                      AS IDENTIFICACION,
        SUBSTR(e.ENTDRZNS, 1, 35)                       AS PARTICIPE,
        a.TPAPCDGO                                      AS TIPO,
        a.APRTCDGO                                      AS ID_APORTE,
        ROUND(a.APRTVLRR, 2)                            AS VALOR,
        TO_CHAR(a.APRTPRDV, 'MM/YYYY')                  AS DEVENGO_ACTUAL,
        CASE WHEN NOT EXISTS (SELECT 1 FROM CRD.CNTR c
                              WHERE c.ENTDCDGO = a.ENTDCDGO AND c.CNTRESTD = 1)
             THEN 'EXCLUSION DECLARADA: sin contrato ACTIVO'
             ELSE 'REVISAR — no encaja en ninguna exclusión declarada'
        END                                             AS POR_QUE_SIGUE_ASI
FROM    CRD.APRT a
JOIN    CRD.ENTD e ON e.ENTDCDGO = a.ENTDCDGO
WHERE   a.TPAPCDGO IN (9, 11) AND a.APRTVLRR > 0
AND     a.APRTPRDV IS NOT NULL AND a.APRTPRDV < DATE '2025-06-01'
ORDER BY 7, 1;

-- 3.4 Cuántas filas se movieron, y cuánto valor representan.
SELECT COUNT(*)                                     AS FILAS_REUBICADAS,
       COUNT(DISTINCT a.ENTDCDGO)                   AS PARTICIPES,
       ROUND(SUM(a.APRTVLRR), 2)                    AS VALOR_INVOLUCRADO,
       MIN(a.APRTPRDV)                              AS MIN_DEVENGO_NUEVO,
       MAX(a.APRTPRDV)                              AS MAX_DEVENGO_NUEVO
FROM   CRD.APRT a
WHERE  a.APRTGLSA LIKE '%(reubicado desde%';

-- 3.5 Ningún par (entidad, tipo, mes) quedó con más de una fila móvil.
--     Esperado: 0 filas.
SELECT COUNT(*) AS MESES_CON_MAS_DE_UNA_FILA
FROM ( SELECT a.ENTDCDGO, a.TPAPCDGO, a.APRTPRDV
       FROM   CRD.APRT a
       WHERE  a.TPAPCDGO IN (9, 11) AND a.APRTVLRR > 0
       AND    a.APRTFCTR >= DATE '2025-06-01'
       AND    a.APRTPRDV IS NOT NULL
       AND    (   a.CRARCDGO IS NOT NULL
               OR EXISTS (SELECT 1 FROM CRD.CRAR cr WHERE cr.CRARCDGO = a.APRTIDAS)
               OR a.APRTGLSA LIKE 'Aporte %CargaArchivo: %')
       GROUP  BY a.ENTDCDGO, a.TPAPCDGO, a.APRTPRDV
       HAVING COUNT(*) > 1 );

-- 3.6 La glosa sigue siendo legible por los dos patrones que la leen: el LIKE que
--     clasifica la fila y el regex ANCLADO que extrae el id de carga.
--     Esperado: FILAS_REUBICADAS = CON_ID_DE_CARGA_LEGIBLE.
SELECT COUNT(*) AS FILAS_REUBICADAS,
       SUM(CASE WHEN REGEXP_SUBSTR(a.APRTGLSA, 'CargaArchivo: ([0-9]+)\s*$', 1, 1, NULL, 1) IS NOT NULL
                THEN 1 ELSE 0 END) AS CON_ID_DE_CARGA_LEGIBLE
FROM   CRD.APRT a
WHERE  a.APRTGLSA LIKE '%(reubicado desde%';

-- Si 3.1, 3.2, 3.3 y 3.5 dan 0 y 3.6 coincide:  COMMIT;
-- Si algo no da:                                ROLLBACK;


-- =============================================================================
-- 4. ⛔ REVERSO — NO EJECUTAR salvo que haya que deshacer la reubicación
-- =============================================================================
-- Devuelve devengo y glosa a como estaban. No toca nada más porque nada más se
-- tocó. Sirve incluso después del COMMIT, mientras exista la tabla de respaldo.
--
-- UPDATE CRD.APRT a
-- SET   (a.APRTPRDV, a.APRTGLSA) =
--       ( SELECT b.APRTPRDV, b.APRTGLSA
--         FROM   CRD.BKP_APRT_DEVENGO_20260831 b
--         WHERE  b.APRTCDGO = a.APRTCDGO )
-- WHERE EXISTS ( SELECT 1 FROM CRD.BKP_APRT_DEVENGO_20260831 b
--                WHERE b.APRTCDGO = a.APRTCDGO
--                AND   (b.APRTPRDV <> a.APRTPRDV OR b.APRTGLSA <> a.APRTGLSA) );
-- COMMIT;
--
-- La tabla de respaldo NO se borra en este script. Se elimina a mano cuando el
-- usuario dé por buena la corrección:
-- DROP TABLE CRD.BKP_APRT_DEVENGO_20260831;
-- =============================================================================
