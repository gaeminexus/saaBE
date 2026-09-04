-- =====================================================================================
-- 189 - Fechas del pago de pension complementaria: que hay ANTES de correr agosto
-- FECHA: 2026-09-04 · EQUIPO: CRD / Equipo B (eqB, omen-saa-1)
--
-- SOLO SELECT. No modifica una sola fila. Se puede correr en horario laboral.
--
-- PARA QUE: el 2026-09-04 el usuario decidio que todo el circuito de jubilados se
-- registra con fecha de FIN DE MES del periodo. Hasta este cambio el proceso usaba:
--   - PGPC.PGPCFCHA  = dia 1 del mes del periodo
--   - APRT.APRTFCTR   = LocalDateTime.now() (el dia en que se corrio el proceso)
--   - PGAP.PGAPFCCN   = idem
-- Este script contesta tres cosas ANTES de la corrida de agosto:
--   1. Si ya existen pagos de periodos anteriores, y con que fechas quedaron.
--   2. Si agosto 2026 ya se corrio (idempotencia: volver a correrlo NO duplica, pero
--      tampoco reconstruye el informe - ver §1 del contrato).
--   3. Cuanta inconsistencia de fecha hay hoy, para decidir si hay que corregir datos
--      viejos o si alcanza con que el cambio aplique de aca en adelante.
--
-- REFERENCIA: docs/logica-negocio/crd/API-PAGO-PENSION-COMPLEMENTARIA.md §6bis
-- =====================================================================================

PROMPT ==========================================================================
PROMPT BLOQUE 1 - Que periodos de pension complementaria existen y como quedaron
PROMPT ==========================================================================

SELECT p.PGPCANNO                                  AS ANIO,
       p.PGPCMESS                                  AS MES,
       COUNT(*)                                    AS PAGOS,
       MIN(p.PGPCFCHA)                             AS FECHA_MIN,
       MAX(p.PGPCFCHA)                             AS FECHA_MAX,
       -- El dia del mes en que quedo fechado. 1 = convencion vieja.
       -- Igual a LAST_DAY = convencion nueva.
       MIN(TO_CHAR(p.PGPCFCHA, 'DD'))              AS DIA_MIN,
       MAX(TO_CHAR(p.PGPCFCHA, 'DD'))              AS DIA_MAX,
       SUM(p.PGPCVLRR)                             AS TOTAL,
       MIN(p.PGPCFCRG)                             AS REGISTRADO_DESDE,
       MAX(p.PGPCFCRG)                             AS REGISTRADO_HASTA
  FROM CRD.PGPC p
 GROUP BY p.PGPCANNO, p.PGPCMESS
 ORDER BY p.PGPCANNO, p.PGPCMESS;

PROMPT
PROMPT (!) Si DIA_MIN = DIA_MAX = 01, esos periodos usan la convencion VIEJA.
PROMPT (!) Si la tabla vuelve VACIA, no se corrio nunca: agosto es la primera corrida
PROMPT     real y no hay ningun dato historico que corregir. Ese es el mejor caso.
PROMPT

PROMPT ==========================================================================
PROMPT BLOQUE 2 - Agosto 2026: existe ya?
PROMPT ==========================================================================

SELECT COUNT(*)                                    AS PAGOS_AGOSTO_2026,
       SUM(p.PGPCVLRR)                             AS TOTAL,
       COUNT(p.PGPCIDPG)                           AS CON_ORDEN_TESORERIA,
       COUNT(*) - COUNT(p.PGPCIDPG)                AS SIN_ORDEN_CRUZADO_INTEGRO,
       COUNT(p.PGPCNMDV)                           AS CON_ASIENTO_DEVENGO
  FROM CRD.PGPC p
 WHERE p.PGPCANNO = 2026
   AND p.PGPCMESS = 8;

PROMPT
PROMPT (!) PAGOS_AGOSTO_2026 = 0  -> agosto esta limpio, se puede correr.
PROMPT (!) PAGOS_AGOSTO_2026 > 0  -> agosto YA se corrio. Volver a generar no duplica,
PROMPT     pero devuelve renglones YA_EXISTIA sin nombre ni cruce, y los totales del
PROMPT     encabezado dan casi cero. Para ver el informe usar GET /rest/pgpc/porPeriodo.
PROMPT (!) SIN_ORDEN_CRUZADO_INTEGRO NO es un error: es el jubilado cuya deuda se llevo
PROMPT     toda la pension del mes. Queda en estado 1 para siempre y esta bien.
PROMPT

PROMPT ==========================================================================
PROMPT BLOQUE 3 - La inconsistencia de fecha, medida
PROMPT ==========================================================================
PROMPT Compara, por cada pago, la fecha del PGPC contra la fecha del movimiento de
PROMPT aporte que lo respalda. Antes del cambio del 2026-09-04 estas dos podian caer
PROMPT en MESES DISTINTOS: el PGPC en el periodo y el APRT en el dia de la corrida.

SELECT p.PGPCANNO                                  AS ANIO,
       p.PGPCMESS                                  AS MES,
       COUNT(*)                                    AS PAGOS,
       SUM(CASE WHEN TO_CHAR(p.PGPCFCHA, 'YYYYMM')
                   = TO_CHAR(a.APRTFCTR, 'YYYYMM')
                THEN 1 ELSE 0 END)                 AS MISMO_MES,
       SUM(CASE WHEN TO_CHAR(p.PGPCFCHA, 'YYYYMM')
                  <> TO_CHAR(a.APRTFCTR, 'YYYYMM')
                THEN 1 ELSE 0 END)                 AS MESES_DISTINTOS
  FROM CRD.PGPC p
  JOIN CRD.APRT a ON a.APRTCDGO = p.PGPCIDAP
 GROUP BY p.PGPCANNO, p.PGPCMESS
 ORDER BY p.PGPCANNO, p.PGPCMESS;

PROMPT
PROMPT (!) MESES_DISTINTOS > 0 es la marca del defecto: la baja del aporte quedo
PROMPT     contabilizada en un mes distinto al del pago que la origino. Es lo que el
PROMPT     cambio del 2026-09-04 corrige de aca en adelante.
PROMPT (!) Este script NO corrige nada. Si MESES_DISTINTOS > 0 y se decide corregir el
PROMPT     historico, eso es un script aparte y una decision del usuario: tocar fechas
PROMPT     contables de periodos ya cerrados no se hace sin saber si estan cerrados.
PROMPT

PROMPT ==========================================================================
PROMPT BLOQUE 4 - Cuantos jubilados espera la corrida de agosto
PROMPT ==========================================================================
PROMPT Prevuelo grueso, para saber si el resultado de la corrida tiene el tamano
PROMPT esperado. El prevuelo fino lo hace la pantalla.

-- ⛔ CORREGIDO EL 2026-09-04. La primera version de este bloque contaba VPPC activas y
-- decia "contrastar contra EVALUADOS". ESO ESTABA MAL y daba un numero enganoso (191).
-- generarPagosDelMes NO parte de VPPC: parte del ESTADO DEL PARTICIPE
-- (ServiceImpl:240 -> entidadDaoService.selectByIdEstado(JUBILADO_COMPLEMENTARIO)).
-- VPPC se consulta DESPUES, por jubilado, y su ausencia es un ERROR de la corrida
-- (SIN_VALOR_PENSION), no un filtro de entrada. Son dos poblaciones distintas.

-- 4.a LA POBLACION REAL que va a evaluar la corrida
SELECT COUNT(*)                                    AS EVALUADOS_ESPERADOS
  FROM CRD.ENTD e
 WHERE e.ENTDIDST = 3;          -- EstadoParticipeEntidad.JUBILADO_COMPLEMENTARIO

PROMPT
PROMPT (!) ESTE es el numero que tiene que coincidir con EVALUADOS de la corrida.
PROMPT

-- 4.b (!!) EL CONTROL QUE DECIDE SI 4.a SIRVE. Leer antes de creerle al 4.a.
-- CLAUDE.md y crd/MIGRACION-ESTADO-PARTICIPE.md documentan que en CRD.ENTD la columna
-- ENTDIDST apuntaba al PK del catalogo mientras el rubro usa el CODIGO ALTERNO. El
-- propio EstadoParticipeEntidad.java lo deja escrito: "3 JUBILADO COMPLEMENTARIO <- PK 30".
-- El codigo Java pasa 3. Si en la base quedaron PKs (30, 41, 42), selectByIdEstado(3)
-- NO DEVUELVE NADA y la corrida evalua CERO jubilados sin fallar.
SELECT e.ENTDIDST                                  AS VALOR_EN_LA_COLUMNA,
       COUNT(*)                                    AS PARTICIPES,
       CASE WHEN e.ENTDIDST BETWEEN 1 AND 9  THEN 'alterno (lo que espera el codigo)'
            WHEN e.ENTDIDST BETWEEN 25 AND 50 THEN '*** PK del catalogo - NO lo encuentra ***'
            ELSE 'revisar'
       END                                         AS INTERPRETACION
  FROM CRD.ENTD e
 GROUP BY e.ENTDIDST
 ORDER BY 1;

PROMPT
PROMPT (!) Si aparece un 30 con participes, la migracion de estados quedo a medias y esos
PROMPT     jubilados NO los va a ver la corrida. PARAR y avisar antes de correr agosto.
PROMPT (!) Si todos los valores son de una sola cifra, la columna ya usa el alterno y el
PROMPT     4.a es confiable.
PROMPT

-- 4.c Cuantos de la poblacion real NO tienen VPPC activa: cada uno sera un SIN_VALOR_PENSION
SELECT COUNT(*)                                    AS JUBILADOS_SIN_VPPC_ACTIVA
  FROM CRD.ENTD e
 WHERE e.ENTDIDST = 3
   AND NOT EXISTS (SELECT 1 FROM CRD.VPPC v
                    WHERE v.ENTDCDGO = e.ENTDCDGO AND v.VPPCIDST = 1);

PROMPT
PROMPT (!) Estos van a salir como renglones ERROR con SIN_VALOR_PENSION. Es esperable y no
PROMPT     rompe la corrida, pero conviene saber cuantos ANTES para no asustarse.
PROMPT

-- 4.d Al reves: VPPC activa de alguien que NO es JUBILADO_COMPLEMENTARIO. A estos la
--     corrida NI LOS MIRA, y no dejan rastro en el detalle: desaparecen en silencio.
SELECT COUNT(*)                                    AS VPPC_ACTIVA_FUERA_DE_LA_POBLACION
  FROM CRD.VPPC v
  JOIN CRD.ENTD e ON e.ENTDCDGO = v.ENTDCDGO
 WHERE v.VPPCIDST = 1
   AND NVL(e.ENTDIDST, -1) <> 3;

PROMPT
PROMPT (!) Estos son los peligrosos: alguien les configuro cuanto cobran, pero su estado
PROMPT     dice que no son jubilados complementarios, asi que NO se les paga y NO aparecen
PROMPT     en ningun error. Si el numero no es cero, revisar caso por caso antes de correr.
PROMPT

PROMPT ==========================================================================
PROMPT BLOQUE 5 - (!) APRTPRDV: la via por la que esto llega a los reportes de cartera
PROMPT ==========================================================================
PROMPT Aporte.java:175-187 documenta que APRTPRDV es el mes al que PERTENECE el aporte, y
PROMPT que toda consulta de cartera debe leer NVL(APRTPRDV, TRUNC(APRTFCTR,'MM')), nunca
PROMPT la columna sola.
PROMPT
PROMPT crearMovimientoNegativo hace setPeriodoDevengo(null) para el pago de pension. Con
PROMPT APRTPRDV en NULL, el NVL cae en TRUNC(APRTFCTR,'MM') - que es justo la fecha que
PROMPT hasta el 2026-09-04 era now(). Por ahi la fecha equivocada entra a los reportes.

SELECT TO_CHAR(a.APRTFCTR, 'YYYY-MM')              AS MES_SEGUN_APRTFCTR,
       COUNT(*)                                    AS MOVIMIENTOS,
       SUM(CASE WHEN a.APRTPRDV IS NULL THEN 1 ELSE 0 END) AS SIN_PERIODO_DEVENGO,
       SUM(a.APRTVLRR)                             AS VALOR
  FROM CRD.APRT a
 WHERE a.APRTTPMV = 9          -- CrdTipoMovimientoAporte.PAGO_PENSION
 GROUP BY TO_CHAR(a.APRTFCTR, 'YYYY-MM')
 ORDER BY 1;

PROMPT
PROMPT (!) SIN_PERIODO_DEVENGO = MOVIMIENTOS significa que TODOS caen al fallback, o sea
PROMPT     que el mes que ve cartera es el de APRTFCTR. Con el cambio del 2026-09-04
PROMPT     APRTFCTR pasa a ser fin de mes del periodo, asi que el fallback ya da bien.
PROMPT (!) Llenar APRTPRDV con el primer dia del mes del periodo seria el arreglo de
PROMPT     fondo (no depender del fallback). ES UNA DECISION APARTE: esa columna la
PROMPT     gobierna el plan de devengo de aportes (Fase 2, 2026-08-27), no este frente.
PROMPT     No se toca sin el visto bueno del usuario.
PROMPT

-- =====================================================================================
-- No hay bloque de reverso: este script no escribe nada.
-- =====================================================================================
