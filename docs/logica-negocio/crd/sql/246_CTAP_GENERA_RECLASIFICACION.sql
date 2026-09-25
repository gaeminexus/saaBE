-- =====================================================================================
-- 246 - CRD.CTAP: marcar que tipos de aporte generan asiento de reclasificacion
-- FECHA: 2026-09-25 · EQUIPO: omen-saa-1 (CRD · EQUIPO B)
--
-- Sin comandos de SQL*Plus. Comentarios ARRIBA, nunca intercalados.
-- ⛔ VA ANTES DEL WAR que trae el cambio de codigo.
--
-- =====================================================================================
-- QUE PASA HOY, VISTO EN PRODUCCION
-- =====================================================================================
-- Al autorizar por contabilidad el pago de un sepelio, el asiento de reclasificacion
-- (CRE-2026-09-0422) sale asi:
--
--   2.1.01.05.01 APORTES PERSONALES CESANTIA        DEBE  2.755,07
--   2.3.01.05.01 LIQUIDACION APORTES CESANTIA                      HABER 2.755,07
--   2.3.90.90.11 INDEMNIZACIONES ... BENEFICIARIOS  DEBE  1.000,00
--   2.3.90.90.11 INDEMNIZACIONES ... BENEFICIARIOS                 HABER 1.000,00
--
-- ⛔ Las dos ultimas lineas son LA MISMA CUENTA al debe y al haber por el mismo valor:
-- un movimiento que no mueve nada y ensucia el mayor de la cuenta de sepelio.
--
-- Sale asi porque el sql/231 dejo, a proposito, CTAPPLNL = CTAPPLNP = 2.3.90.90.11 para
-- el tipo 27, y generarAsientoReclasificacion arma DEBE cuentaPasivo / HABER
-- cuentaLiquidacion para CADA tipo de la devolucion.
--
-- ⇒ DECISION DEL USUARIO (2026-09-25), textual: "no generemos movimientos en la cuenta
--   de sepelio en esta parte del proceso... para sepelio dejemos solo el asiento al
--   registrar el dinero recibido y al pagarlo. No al autorizar."
--
-- =====================================================================================
-- ⚠️ ESTO REVIERTE UNA DECISION MIA DEL 2026-09-22, Y QUEDA ESCRITO
-- =====================================================================================
-- El diseño original (DISENO-BENEFICIARIOS-Y-VALORES-DE-SEGURO.md §4) pedia exactamente
-- esta columna: "la marca tiene que ser EXPLICITA, no magica: una columna nueva en
-- CRD.CTAP (p.ej. CTAPRCLS) en vez de deducirlo de que las dos cuentas coincidan".
--
-- El 2026-09-22 yo decidi NO agregarla (§9 de ese mismo documento), con el argumento de
-- que el asiento neutro "cuadra, no descuadra nada" y que el ruido en un mayor era mas
-- barato que tocar el motor de reclasificacion.
--
-- ⇒ Me equivoque, y el usuario lo vio en el primer sepelio real. Un asiento que no mueve
--   nada NO es ruido inocuo: es una linea que alguien va a tener que explicar cada vez
--   que concilie esa cuenta, y que hace parecer que el sepelio se movio dos veces. El
--   criterio "cuadra, entonces da igual" era comodo para mi, no correcto para quien lee
--   el mayor.
-- =====================================================================================


-- =====================================================================================
-- 0. CONTROLES PREVIOS
-- =====================================================================================

-- 0.1 La columna no debe existir. Esperado: 0 filas.
SELECT c.COLUMN_NAME FROM ALL_TAB_COLUMNS c
 WHERE c.OWNER = 'CRD' AND c.TABLE_NAME = 'CTAP' AND c.COLUMN_NAME = 'CTAPRCLS';

-- 0.2 Cuantas configuraciones hay hoy. Todas tienen que quedar en 1 (si reclasifican):
--     el cambio NO debe alterar el comportamiento de ningun tipo existente.
SELECT COUNT(*) AS CONFIGURACIONES FROM CRD.CTAP;

-- 0.3 ⭐ La(s) fila(s) del tipo 27, que son las unicas que van a quedar en 0.
--     Esperado: al menos 1 fila, con las dos cuentas iguales (2.3.90.90.11).
SELECT c.CTAPCDGO, c.TPAPCDGO, c.PJRQCDGO,
       np.PLNNCNTA AS CUENTA_PASIVO,
       nl.PLNNCNTA AS CUENTA_LIQUIDACION,
       c.CTAPESTD
  FROM CRD.CTAP c
  LEFT JOIN CNT.PLNN np ON np.PLNNCDGO = c.CTAPPLNP
  LEFT JOIN CNT.PLNN nl ON nl.PLNNCDGO = c.CTAPPLNL
 WHERE c.TPAPCDGO = 27;

-- 0.4 ⭐ Y si hay OTROS tipos con las dos cuentas iguales — serian candidatos al mismo
--     problema, y conviene verlos antes de decidir. Esperado: idealmente solo el 27.
SELECT c.TPAPCDGO, t.TPAPNMBR, np.PLNNCNTA AS CUENTA
  FROM CRD.CTAP c
  JOIN CRD.TPAP t ON t.TPAPCDGO = c.TPAPCDGO
  JOIN CNT.PLNN np ON np.PLNNCDGO = c.CTAPPLNP
 WHERE c.CTAPPLNP = c.CTAPPLNL;


-- =====================================================================================
-- 1. LA COLUMNA
-- =====================================================================================
-- 1 = genera asiento de reclasificacion (comportamiento de siempre)
-- 0 = NO lo genera (el tipo se liquida por otro camino, como el sepelio)
--
-- Lleva DEFAULT 1 para que cualquier configuracion nueva reclasifique salvo que alguien
-- diga lo contrario, que es el lado seguro.

ALTER TABLE CRD.CTAP ADD (CTAPRCLS NUMBER DEFAULT 1);

COMMENT ON COLUMN CRD.CTAP.CTAPRCLS IS
  '1 = este tipo de aporte genera asiento de reclasificacion al autorizar la devolucion (D cuenta pasivo / H cuenta liquidacion). 0 = no lo genera, porque se liquida por otro camino (caso sepelio: su asiento va al recibir el dinero y al pagarlo, no al autorizar).';


-- =====================================================================================
-- 2. BACKFILL — todas las filas existentes en 1
-- =====================================================================================
-- ⛔ El DEFAULT NO alcanza y esta es la leccion de H77 (2026-09-22): un DEFAULT de Oracle
--    solo actua cuando la columna se OMITE del INSERT, y Hibernate la manda SIEMPRE, con
--    NULL explicito si el objeto la tiene en null. Ademas, el DEFAULT tampoco rellena las
--    filas que YA existen. Por las dos razones, el backfill es obligatorio.

UPDATE CRD.CTAP SET CTAPRCLS = 1 WHERE CTAPRCLS IS NULL;

COMMIT;


-- =====================================================================================
-- 3. EL TIPO 27 (SEPELIO) PASA A 0
-- =====================================================================================
-- Esperado: tantas filas como devolvio el control 0.3 (una por empresa configurada).

UPDATE CRD.CTAP SET CTAPRCLS = 0 WHERE TPAPCDGO = 27;

COMMIT;


-- =====================================================================================
-- 4. CONTROLES POSTERIORES
-- =====================================================================================

-- 4.1 Ninguna fila puede quedar en NULL. Esperado: 0.
SELECT COUNT(*) AS EN_NULO FROM CRD.CTAP WHERE CTAPRCLS IS NULL;

-- 4.2 El reparto. Esperado: solo las del tipo 27 en 0, todas las demas en 1.
SELECT c.CTAPRCLS, COUNT(*) AS CUANTAS
  FROM CRD.CTAP c
 GROUP BY c.CTAPRCLS
 ORDER BY c.CTAPRCLS;

-- 4.3 Y el detalle de las que quedaron en 0, para confirmarlo a ojo. Esperado: solo 27.
SELECT c.CTAPCDGO, c.TPAPCDGO, t.TPAPNMBR, c.CTAPRCLS
  FROM CRD.CTAP c
  JOIN CRD.TPAP t ON t.TPAPCDGO = c.TPAPCDGO
 WHERE c.CTAPRCLS = 0;


-- =====================================================================================
-- 5. LO QUE TIENE QUE HACER EL CODIGO, Y NO LO RESUELVE ESTE SCRIPT
-- =====================================================================================
-- 1. generarAsientoReclasificacion saltea las lineas de los tipos con CTAPRCLS = 0.
--
-- 2. ⛔ LA VALIDACION DE CUADRE TIENE QUE CAMBIAR, y es el punto que rompe si se olvida:
--    hoy compara el total del asiento contra el valor TOTAL de la devolucion
--    (DevolucionAporteServiceImpl:1678-1682). Si se excluye el sepelio, el asiento suma
--    menos que ese total y la validacion lo rechaza por descuadrado — la devolucion
--    entera fallaria. Debe comparar contra la suma de los tipos que SI reclasifican.
--
-- 3. ⛔ Y EL CASO BORDE, que es justamente el del usuario: una devolucion SOLO de
--    sepelio no tiene ninguna linea que reclasificar. Ahi NO se genera asiento: no se
--    graba uno vacio. La devolucion queda sin numero de asiento de reclasificacion, que
--    es correcto, porque su contabilidad va por los otros dos asientos (el de la
--    recepcion y el del pago).
-- =====================================================================================


-- =====================================================================================
-- 6. REVERSO — comentado
-- =====================================================================================
-- ⛔ Volver atras hace que el sepelio vuelva a generar el asiento neutro.
--
-- ALTER TABLE CRD.CTAP DROP COLUMN CTAPRCLS;
