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
-- (!) ESTE SCRIPT ESTA ESCRITO PARA AGOSTO 2026. Para otro periodo, reemplazar en todo
-- el archivo: 2026 por el anio, 8 por el mes, y '2026-08-01' por el primer dia del mes.
-- =====================================================================================

-- ==========================================================================
-- BLOQUE 1 - El resumen de la corrida
-- ==========================================================================

SELECT COUNT(*)                                          AS PAGOS,
       SUM(p.PGPCVLPN)                                   AS TOTAL_PENSION,
       SUM(p.PGPCVLSG)                                   AS TOTAL_SEGURO,
       SUM(p.PGPCVLRR)                                   AS TOTAL,
       COUNT(p.PGPCIDPG)                                 AS CON_ORDEN_TESORERIA,
       COUNT(*) - COUNT(p.PGPCIDPG)                      AS SIN_ORDEN,
       COUNT(p.PGPCNMDV)                                 AS CON_ASIENTO_DEVENGO,
       COUNT(*) - COUNT(p.PGPCNMDV)                      AS SIN_ASIENTO_DEVENGO
  FROM CRD.PGPC p
 WHERE p.PGPCANNO = 2026
   AND p.PGPCMESS = 8;

--
-- (!) PAGOS debe coincidir con "generados" del resultado de la corrida.
-- (!) SIN_ORDEN NO es un error: es el jubilado cuya deuda se llevo toda la pension.
--     Queda en estado 3 (PAGADA) sin salida de dinero, y esta bien.
-- (!) SIN_ASIENTO_DEVENGO > 0 SI ES UN PROBLEMA salvo que la contabilidad de CRD
--     este inactiva. Cada pago debe tener su asiento de la plantilla alterno 35.
--

-- ==========================================================================
-- BLOQUE 2 - (!) LAS FECHAS: la regla del 2026-09-04
-- ==========================================================================
-- La fecha del hecho es min(ultimo dia del mes del periodo, hoy). Para un periodo
-- ya cerrado tiene que ser el ULTIMO DIA DEL MES. Para el mes en curso, el dia de
-- la corrida. Nunca futura.

SELECT p.PGPCFCHA                                        AS FECHA_DEL_HECHO,
       LAST_DAY(TO_DATE('2026-08-01','YYYY-MM-DD'))   AS FIN_DE_MES_ESPERADO,
       TRUNC(SYSDATE)                                    AS HOY,
       COUNT(*)                                          AS PAGOS,
       CASE WHEN p.PGPCFCHA > TRUNC(SYSDATE)
            THEN '*** FUTURA - NO DEBERIA PASAR ***'
            WHEN p.PGPCFCHA = LAST_DAY(TO_DATE('2026-08-01','YYYY-MM-DD'))
            THEN 'OK - fin de mes (periodo cerrado)'
            WHEN p.PGPCFCHA = TRUNC(SYSDATE)
            THEN 'OK - dia de proceso (mes en curso)'
            ELSE '*** REVISAR ***'
       END                                               AS VEREDICTO
  FROM CRD.PGPC p
 WHERE p.PGPCANNO = 2026
   AND p.PGPCMESS = 8
 GROUP BY p.PGPCFCHA
 ORDER BY 1;

--
-- (!) Cualquier cosa distinta de un OK unico es para parar y avisar.
--

-- ==========================================================================
-- BLOQUE 3 - El movimiento de aporte tiene que caer en el MISMO mes que el pago
-- ==========================================================================
-- Este es el defecto que se corrigio el 2026-09-04: antes APRTFCTR llevaba now() y
-- podia caer en un mes distinto al del pago. Ahora tiene que coincidir.

SELECT TO_CHAR(p.PGPCFCHA,'YYYY-MM')                     AS MES_DEL_PAGO,
       TO_CHAR(a.APRTFCTR,'YYYY-MM')                     AS MES_DEL_APORTE,
       COUNT(*)                                          AS MOVIMIENTOS,
       SUM(a.APRTVLRR)                                   AS VALOR
  FROM CRD.PGPC p
  JOIN CRD.APRT a ON a.APRTCDGO = p.PGPCIDAP
 WHERE p.PGPCANNO = 2026
   AND p.PGPCMESS = 8
 GROUP BY TO_CHAR(p.PGPCFCHA,'YYYY-MM'), TO_CHAR(a.APRTFCTR,'YYYY-MM')
 ORDER BY 1, 2;

--
-- (!) Tiene que devolver UNA SOLA FILA, con MES_DEL_PAGO = MES_DEL_APORTE.
--     Dos filas = hay movimientos fechados en otro mes.
-- (!) El valor del aporte es NEGATIVO a proposito: es una baja de saldo.
--

-- ==========================================================================
-- BLOQUE 4 - Cuadre: el pago contra sus componentes
-- ==========================================================================

SELECT COUNT(*)                                          AS PAGOS_DESCUADRADOS
  FROM CRD.PGPC p
 WHERE p.PGPCANNO = 2026
   AND p.PGPCMESS = 8
   AND ABS(NVL(p.PGPCVLRR,0)
         - (NVL(p.PGPCVLPN,0) + NVL(p.PGPCVLSG,0))) > 0.005;

--
-- (!) Tiene que dar CERO. El total debe ser pension + seguro, al centavo.
--     Cualquier otra cosa es el patron de H24 (el total se graba mas alto que la
--     suma de sus componentes) apareciendo en otro proceso.
--

-- ==========================================================================
-- BLOQUE 5 - Estados, y que ninguno quedo donde no corresponde
-- ==========================================================================

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
 WHERE p.PGPCANNO = 2026
   AND p.PGPCMESS = 8
 GROUP BY p.PGPCESTD
 ORDER BY 1;

--
-- (!) Lo esperable recien corrido: la mayoria en 2 (EN_PAGO, con orden) y los
--     cruzados integros en 3 (PAGADA, sin orden).
-- (!) Un 5 no puede existir: no hay endpoint que anule. Si aparece, avisar.
--

-- ==========================================================================
-- BLOQUE 6 - Quien NO salio, para contrastar contra el prevuelo
-- ==========================================================================

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
                      AND p.PGPCANNO = 2026
                      AND p.PGPCMESS = 8)
 ORDER BY e.ENTDRZNS;

--
-- (!) Cada uno de estos tiene que tener explicacion en el detalle de la corrida:
--     SIN_CUENTA_BANCARIA, SALDO_INSUFICIENTE, SIN_VALOR_PENSION o el estado del
--     participe. Un jubilado aca SIN renglon de error en la corrida es un caso que
--     el proceso ni siquiera evaluo - eso hay que mirarlo.
--

-- =====================================================================================
-- No hay bloque de reverso: este script no escribe nada.
--
-- (!) SI ALGO SALE MAL: no existe revertirPagoPension. Deshacer una corrida significa,
-- a mano: anular las ordenes en tesoreria, revertir los asientos por el proceso normal
-- de contabilidad (NO borrarlos), dar de baja los movimientos de CRD.APRT y borrar las
-- filas de CRD.PGPC. Por eso el 189 se corre ANTES y este DESPUES.
-- =====================================================================================
