-- =====================================================================================
-- 190 - Verificacion DESPUES de correr el pago mensual a jubilados
-- FECHA: 2026-09-04 · EQUIPO: CRD / Equipo B (eqB, omen-saa-1)
--
-- SOLO SELECT. No modifica una sola fila.
--
-- PARA QUE: la corrida de generarPagosDelMes devuelve un 200 aunque falle la mitad de
-- los jubilados - los fallos viajan como renglones del detalle, no como codigo HTTP.
-- Este script confirma contra la BASE lo que la corrida dijo, que es lo unico que vale.
--
-- CUANDO: inmediatamente despues de correr el mes, por pantalla o por API. Sirve igual
-- en los dos casos, y es la unica verificacion posible si se corre por API cruda.
--
-- ANTES de correr el mes va el 189.
--
-- (!) EDITAR ESTAS DOS LINEAS con el periodo que se acaba de correr:
DEFINE ANIO = 2026
DEFINE MES  = 8
-- =====================================================================================

PROMPT ==========================================================================
PROMPT BLOQUE 1 - El resumen de la corrida
PROMPT ==========================================================================

SELECT COUNT(*)                                          AS PAGOS,
       SUM(p.PGPCVLPN)                                   AS TOTAL_PENSION,
       SUM(p.PGPCVLSG)                                   AS TOTAL_SEGURO,
       SUM(p.PGPCVLRR)                                   AS TOTAL,
       COUNT(p.PGPCIDPG)                                 AS CON_ORDEN_TESORERIA,
       COUNT(*) - COUNT(p.PGPCIDPG)                      AS SIN_ORDEN,
       COUNT(p.PGPCNMDV)                                 AS CON_ASIENTO_DEVENGO,
       COUNT(*) - COUNT(p.PGPCNMDV)                      AS SIN_ASIENTO_DEVENGO
  FROM CRD.PGPC p
 WHERE p.PGPCANNO = &ANIO
   AND p.PGPCMESS = &MES;

PROMPT
PROMPT (!) PAGOS debe coincidir con "generados" del resultado de la corrida.
PROMPT (!) SIN_ORDEN NO es un error: es el jubilado cuya deuda se llevo toda la pension.
PROMPT     Queda en estado 3 (PAGADA) sin salida de dinero, y esta bien.
PROMPT (!) SIN_ASIENTO_DEVENGO > 0 SI ES UN PROBLEMA salvo que la contabilidad de CRD
PROMPT     este inactiva. Cada pago debe tener su asiento de la plantilla alterno 35.
PROMPT

PROMPT ==========================================================================
PROMPT BLOQUE 2 - (!) LAS FECHAS: la regla del 2026-09-04
PROMPT ==========================================================================
PROMPT La fecha del hecho es min(ultimo dia del mes del periodo, hoy). Para un periodo
PROMPT ya cerrado tiene que ser el ULTIMO DIA DEL MES. Para el mes en curso, el dia de
PROMPT la corrida. Nunca futura.

SELECT p.PGPCFCHA                                        AS FECHA_DEL_HECHO,
       LAST_DAY(TO_DATE('&ANIO-&MES-01','YYYY-MM-DD'))   AS FIN_DE_MES_ESPERADO,
       TRUNC(SYSDATE)                                    AS HOY,
       COUNT(*)                                          AS PAGOS,
       CASE WHEN p.PGPCFCHA > TRUNC(SYSDATE)
            THEN '*** FUTURA - NO DEBERIA PASAR ***'
            WHEN p.PGPCFCHA = LAST_DAY(TO_DATE('&ANIO-&MES-01','YYYY-MM-DD'))
            THEN 'OK - fin de mes (periodo cerrado)'
            WHEN p.PGPCFCHA = TRUNC(SYSDATE)
            THEN 'OK - dia de proceso (mes en curso)'
            ELSE '*** REVISAR ***'
       END                                               AS VEREDICTO
  FROM CRD.PGPC p
 WHERE p.PGPCANNO = &ANIO
   AND p.PGPCMESS = &MES
 GROUP BY p.PGPCFCHA
 ORDER BY 1;

PROMPT
PROMPT (!) Cualquier cosa distinta de un OK unico es para parar y avisar.
PROMPT

PROMPT ==========================================================================
PROMPT BLOQUE 3 - El movimiento de aporte tiene que caer en el MISMO mes que el pago
PROMPT ==========================================================================
PROMPT Este es el defecto que se corrigio el 2026-09-04: antes APRTFCTR llevaba now() y
PROMPT podia caer en un mes distinto al del pago. Ahora tiene que coincidir.

SELECT TO_CHAR(p.PGPCFCHA,'YYYY-MM')                     AS MES_DEL_PAGO,
       TO_CHAR(a.APRTFCTR,'YYYY-MM')                     AS MES_DEL_APORTE,
       COUNT(*)                                          AS MOVIMIENTOS,
       SUM(a.APRTVLRR)                                   AS VALOR
  FROM CRD.PGPC p
  JOIN CRD.APRT a ON a.APRTCDGO = p.PGPCIDAP
 WHERE p.PGPCANNO = &ANIO
   AND p.PGPCMESS = &MES
 GROUP BY TO_CHAR(p.PGPCFCHA,'YYYY-MM'), TO_CHAR(a.APRTFCTR,'YYYY-MM')
 ORDER BY 1, 2;

PROMPT
PROMPT (!) Tiene que devolver UNA SOLA FILA, con MES_DEL_PAGO = MES_DEL_APORTE.
PROMPT     Dos filas = hay movimientos fechados en otro mes.
PROMPT (!) El valor del aporte es NEGATIVO a proposito: es una baja de saldo.
PROMPT

PROMPT ==========================================================================
PROMPT BLOQUE 4 - Cuadre: el pago contra sus componentes
PROMPT ==========================================================================

SELECT COUNT(*)                                          AS PAGOS_DESCUADRADOS
  FROM CRD.PGPC p
 WHERE p.PGPCANNO = &ANIO
   AND p.PGPCMESS = &MES
   AND ABS(NVL(p.PGPCVLRR,0)
         - (NVL(p.PGPCVLPN,0) + NVL(p.PGPCVLSG,0))) > 0.005;

PROMPT
PROMPT (!) Tiene que dar CERO. El total debe ser pension + seguro, al centavo.
PROMPT     Cualquier otra cosa es el patron de H24 (el total se graba mas alto que la
PROMPT     suma de sus componentes) apareciendo en otro proceso.
PROMPT

PROMPT ==========================================================================
PROMPT BLOQUE 5 - Estados, y que ninguno quedo donde no corresponde
PROMPT ==========================================================================

SELECT p.PGPCESTD                                        AS ESTADO,
       CASE p.PGPCESTD
            WHEN 1 THEN 'REGISTRADA - contabilizada, sin confirmar en tesoreria'
            WHEN 2 THEN 'EN_PAGO - orden creada, esperando el pago'
            WHEN 3 THEN 'PAGADA'
            WHEN 4 THEN 'RECHAZADA'
            WHEN 5 THEN 'ANULADA - NO DEBERIA EXISTIR, no hay endpoint de anulacion'
            ELSE '*** ESTADO DESCONOCIDO ***'
       END                                               AS SIGNIFICADO,
       COUNT(*)                                          AS PAGOS,
       COUNT(p.PGPCIDPG)                                 AS CON_ORDEN
  FROM CRD.PGPC p
 WHERE p.PGPCANNO = &ANIO
   AND p.PGPCMESS = &MES
 GROUP BY p.PGPCESTD
 ORDER BY 1;

PROMPT
PROMPT (!) Lo esperable recien corrido: la mayoria en 2 (EN_PAGO, con orden) y los
PROMPT     cruzados integros en 3 (PAGADA, sin orden).
PROMPT (!) Un 5 no puede existir: no hay endpoint que anule. Si aparece, avisar.
PROMPT

PROMPT ==========================================================================
PROMPT BLOQUE 6 - Quien NO salio, para contrastar contra el prevuelo
PROMPT ==========================================================================

SELECT e.ENTDCDGO                                        AS ID_ENTIDAD,
       e.ENTDNMID                                        AS IDENTIFICACION,
       e.ENTDRZNS                                        AS NOMBRE,
       v.VPPCVLRR                                        AS VALOR_PENSION
  FROM CRD.VPPC v
  JOIN CRD.ENTD e ON e.ENTDCDGO = v.ENTDCDGO
 WHERE v.VPPCIDST = 1
   AND NOT EXISTS (SELECT 1
                     FROM CRD.PGPC p
                    WHERE p.ENTDCDGO = v.ENTDCDGO
                      AND p.PGPCANNO = &ANIO
                      AND p.PGPCMESS = &MES)
 ORDER BY e.ENTDRZNS;

PROMPT
PROMPT (!) Cada uno de estos tiene que tener explicacion en el detalle de la corrida:
PROMPT     SIN_CUENTA_BANCARIA, SALDO_INSUFICIENTE, SIN_VALOR_PENSION o el estado del
PROMPT     participe. Un jubilado aca SIN renglon de error en la corrida es un caso que
PROMPT     el proceso ni siquiera evaluo - eso hay que mirarlo.
PROMPT

-- =====================================================================================
-- No hay bloque de reverso: este script no escribe nada.
--
-- (!) SI ALGO SALE MAL: no existe revertirPagoPension. Deshacer una corrida significa,
-- a mano: anular las ordenes en tesoreria, revertir los asientos por el proceso normal
-- de contabilidad (NO borrarlos), dar de baja los movimientos de CRD.APRT y borrar las
-- filas de CRD.PGPC. Por eso el 189 se corre ANTES y este DESPUES.
-- =====================================================================================
