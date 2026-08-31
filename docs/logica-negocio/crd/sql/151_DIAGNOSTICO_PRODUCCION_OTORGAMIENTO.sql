-- =====================================================================================
-- DIAGNOSTICO EN PRODUCCION — ciclo de otorgamiento
-- FECHA: 2026-08-31   EQUIPO: CRD / EQUIPO B (ciclo del credito y seguros)
--
-- ⚠️ ESTE SCRIPT NO ESCRIBE NADA. Son SELECT y nada mas: sin INSERT, sin UPDATE,
--    sin DELETE, sin DDL, sin COMMIT. Se puede correr en produccion en cualquier
--    momento, tambien en horario laboral.
--
-- QUE RESPONDE, y por que hace falta correrlo en PRODUCCION y no en local:
--   Bloque 1 — El GATE del frente de otorgamiento. Si hay cartera viva sentada en
--              PRSTIDST = 1, el ciclo nuevo la trata como solicitud pendiente y le
--              ofrece aprobar/rechazar sobre creditos vivos. NO SE DESPLIEGA HASTA
--              VER ESTO. Ver PLAN-CICLO-OTORGAMIENTO.md §5.b.
--   Bloque 2 — El detalle real de las plantillas 9 y 13, necesario para escribir la
--              plantilla 34 (entrega quirografaria) sin copiar auxiliares a ciegas.
--   Bloque 3 — Que el alterno 34 este libre EN PRODUCCION, no solo en el repositorio.
--   Bloque 4 — El bloque 0 del script 150 (PRSTINNM), medido en produccion. En local
--              el UPDATE resulta no-op; aca puede no serlo.
--
-- COMO DEVOLVER EL RESULTADO: pegar la salida completa de los cuatro bloques.
-- =====================================================================================


-- =====================================================================================
-- BLOQUE 1 — EL GATE: reparto de TODA la cartera por estado
--
-- Como leerlo:
--   * Si el estado 1 sale con 0 prestamos -> no hay riesgo, el ciclo entra tal cual.
--   * Si el estado 1 sale con prestamos que tienen tabla y TOTAL_PAGADO > 0 -> es
--     cartera VIVA mal etiquetada. NO desplegar: hay que separarla antes, porque el
--     ciclo le ofreceria aprobar (empieza a devengar mora) o rechazar (deja de
--     devengar), las dos en silencio.
--   * Si sale con prestamos SIN tabla y sin pagos -> son solicitudes muertas o basura
--     de migracion; se pueden dejar.
-- =====================================================================================
SET PAGESIZE 200
SET LINESIZE 220

SELECT  p.PRSTIDST                                       AS ESTADO,
        COUNT(*)                                         AS PRESTAMOS,
        SUM(CASE WHEN EXISTS (SELECT 1 FROM CRD.DTPR d
                              WHERE d.PRSTCDGO = p.PRSTCDGO)
                 THEN 1 ELSE 0 END)                      AS CON_TABLA,
        SUM(CASE WHEN NVL(p.PRSTTTPG,0) > 0
                 THEN 1 ELSE 0 END)                      AS CON_PAGOS,
        ROUND(SUM(NVL(p.PRSTTTPG,0)), 2)                 AS TOTAL_PAGADO,
        ROUND(SUM(NVL(p.PRSTSLCP,0)), 2)                 AS SALDO_CAPITAL,
        MIN(TO_CHAR(p.PRSTFCIN,'YYYY-MM-DD'))            AS INICIO_MAS_VIEJO,
        MAX(TO_CHAR(p.PRSTFCIN,'YYYY-MM-DD'))            AS INICIO_MAS_NUEVO
FROM    CRD.PRST p
GROUP   BY p.PRSTIDST
ORDER   BY p.PRSTIDST;

-- 1.b Si el estado 1 trajo algo, una muestra para mirarlos de cerca.
SELECT  p.PRSTCDGO, p.PRSTIDST, p.ESPSCDGO,
        p.PRSTMNSL                                       AS MONTO,
        p.PRSTTTPG                                       AS TOTAL_PAGADO,
        p.PRSTSLCP                                       AS SALDO_CAPITAL,
        TO_CHAR(p.PRSTFCIN,'YYYY-MM-DD')                 AS FECHA_INICIO,
        TO_CHAR(p.PRSTFCRG,'YYYY-MM-DD')                 AS FECHA_REGISTRO,
        (SELECT COUNT(*) FROM CRD.DTPR d
         WHERE d.PRSTCDGO = p.PRSTCDGO)                  AS CUOTAS
FROM    CRD.PRST p
WHERE   p.PRSTIDST = 1
ORDER   BY p.PRSTFCRG DESC NULLS LAST
FETCH FIRST 30 ROWS ONLY;


-- =====================================================================================
-- BLOQUE 2 — Detalle real de las plantillas 9 (prendario) y 13 (hipotecario)
--
-- Para escribir la 34 (entrega quirografaria) hay que ver que significa cada linea y
-- que auxiliar lleva. AVISO del equipo A, verificado por ellos: de las plantillas de
-- CRD solo la 21 esta renumerada al catalogo semantico; la 25, 27 y 29 usan auxiliares
-- POSICIONALES. Si la 9 y la 13 tambien lo son, copiar sus numeros a la 34 dejaria el
-- asiento mal clasificado Y CUADRADO IGUAL, o sea sin ninguna senal de error.
-- =====================================================================================
SELECT  p.PLNSCDAL                                       AS ALTERNO,
        p.PLNSNMBR                                       AS PLANTILLA,
        d.DTPLAXL1                                       AS AUX1,
        d.DTPLMVMN                                       AS MVMN_1DEBE_2HABER,
        c.PLNNCDGO                                       AS ID_CUENTA,
        c.PLNNCDCT                                       AS CUENTA,
        c.PLNNNMBR                                       AS NOMBRE_CUENTA,
        d.DTPLDSCR                                       AS DESCRIPCION_LINEA,
        d.DTPLESTD                                       AS ESTADO
FROM    CNT.PLNS p
JOIN    CNT.DTPL d ON d.PLNSCDGO = p.PLNSCDGO
JOIN    CNT.PLNN c ON c.PLNNCDGO = d.PLNNCDGO
WHERE   p.PLNSCDAL IN (9, 13)
ORDER   BY p.PLNSCDAL, d.DTPLAXL1, d.DTPLMVMN;

-- 2.b Las cuentas de la familia quirografaria por vencer, para confirmar los IDs que
--     va a usar la plantilla 34. Esperado: 5 filas (1.3.01.05/.10/.15/.20/.25).
SELECT  c.PLNNCDGO AS ID_CUENTA, c.PLNNCDCT AS CUENTA, c.PLNNNMBR AS NOMBRE
FROM    CNT.PLNN c
WHERE   c.PLNNCDCT LIKE '1.3.01.%'
   OR   c.PLNNCDCT IN ('7.3.01.05','7.4.01.05','2.3.90.90.10')
ORDER   BY c.PLNNCDCT;


-- =====================================================================================
-- BLOQUE 3 — El alterno 34 tiene que estar LIBRE en produccion
--
-- El repositorio dice que hay 33 plantillas; esto lo confirma contra la base real.
-- Esperado: 0 filas en la primera consulta.
-- =====================================================================================
SELECT  p.PLNSCDAL AS ALTERNO, p.PLNSNMBR AS NOMBRE, p.PLNSESTD AS ESTADO
FROM    CNT.PLNS p
WHERE   p.PLNSCDAL = 34;

SELECT  MAX(p.PLNSCDAL) AS MAX_ALTERNO_USADO, COUNT(*) AS TOTAL_PLANTILLAS
FROM    CNT.PLNS p;


-- =====================================================================================
-- BLOQUE 4 — PRSTINNM en produccion (bloque 0 del script 150, resumido)
--
-- En LOCAL: 7 de 5.664 prestamos caen al default silencioso del 9% del proceso de mora,
-- y los 7 tienen tambien PRSTTSAA = 0, asi que el UPDATE del 150 no toca ninguna fila.
-- Produccion puede ser otra cosa. Si aca los numeros son distintos, el 150 deja de ser
-- una red de seguridad y pasa a ser algo que hay que correr.
-- =====================================================================================
SELECT  COUNT(*)                                                  AS TOTAL_PRESTAMOS,
        SUM(CASE WHEN p.PRSTINNM IS NULL OR p.PRSTINNM <= 0
                 THEN 1 ELSE 0 END)                               AS CAEN_DEFAULT_9,
        SUM(CASE WHEN (p.PRSTINNM IS NULL OR p.PRSTINNM <= 0)
                  AND (p.PRSTTSAA IS NULL OR p.PRSTTSAA <= 0)
                 THEN 1 ELSE 0 END)                               AS SIN_TASA_NI_NOMINAL,
        SUM(CASE WHEN (p.PRSTINNM IS NULL OR p.PRSTINNM <= 0)
                  AND  p.PRSTTSAA > 0
                 THEN 1 ELSE 0 END)                               AS ARREGLABLES_POR_EL_150
FROM    CRD.PRST p;

-- 4.b Solo los vivos, que son los unicos a los que el proceso de mora les toca algo.
SELECT  COUNT(*)                                                  AS VIVOS,
        SUM(CASE WHEN p.PRSTINNM IS NULL OR p.PRSTINNM <= 0
                 THEN 1 ELSE 0 END)                               AS VIVOS_EN_DEFAULT
FROM    CRD.PRST p
WHERE   p.PRSTIDST IN (2, 8, 10, 11);

-- 4.c Distribucion de la tasa real en la cartera viva. En local: 9% en 1.305 de 1.311.
SELECT  p.PRSTTSAA AS TASA, COUNT(*) AS PRESTAMOS
FROM    CRD.PRST p
WHERE   p.PRSTIDST IN (2, 8, 10, 11)
GROUP   BY p.PRSTTSAA
ORDER   BY p.PRSTTSAA;

-- =====================================================================================
-- FIN. Recordatorio: este script no escribio nada. No hay COMMIT que hacer ni
-- ROLLBACK que temer.
-- =====================================================================================
