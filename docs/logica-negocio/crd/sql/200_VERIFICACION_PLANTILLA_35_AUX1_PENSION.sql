-- =====================================================================================
-- 200 - Verificacion de aux1=1/2 de la plantilla 35 (devengo pago pension complementaria)
-- FECHA: 2026-09-05 - EQUIPO: CRD / Equipo B (eqB, omen-saa-1)
--
-- SOLO SELECT. No modifica una sola fila.
-- Sin comandos de SQL*Plus: no usa PROMPT, DEFINE, SET ni &variables.
--
-- Contexto: H43 (hallazgo real de produccion, 2026-09-05) -- el asiento de DEVENGO
-- (generarAsientoDevengoPension) y el asiento del CRUCE contra prestamo debitan LA MISMA
-- cuenta individual del jubilado (2.1.02.25.01), y el devengo lo hace por el NOMINAL
-- completo de la pension en vez de por el remanente neto de cruce -- sobredebita esa cuenta
-- exactamente por lo que se cruzo. Confirmado con un caso real (PGPC 4, prestamo 7747):
-- devengo D 589,17 + cruce D 481,78 = 1.070,95 debitados de una pension de 589,17.
--
-- Este script SOLO verifica el catalogo (que aux1=1 sea DEBE 2.1.02.25.01 y aux1=2 sea
-- HABER 2.3.01.10.03, tal como documenta docs/logica-negocio/crd/sql/173_PLANTILLA_PAGO_PENSIONES.sql,
-- escrito el 2026-09-02, ANTES de esta corrida) -- no corrige nada por si solo. El fix real
-- es de codigo (PagoPensionComplementariaServiceImpl.generarAsientoDevengoPension: la linea
-- de pension del devengo debe ir por `remanente`, no por `pago.getValorPension()` nominal) y
-- no se implementa desde este script.
-- =====================================================================================

-- =====================================================================================
-- BLOQUE 1 - aux1=1 y aux1=2 de la plantilla 35, contra la BD real
-- =====================================================================================
SELECT p.PLNSCDAL AS ALTERNO, d.DTPLAXL1 AS AUX1, d.DTPLMVMN AS MOVIMIENTO,
       n.PLNNCNTA AS CUENTA, n.PLNNNMBR AS CUENTA_NOMBRE, d.DTPLDSCR AS DESCRIPCION
FROM   CNT.PLNS p
JOIN   CNT.DTPL d ON d.PLNSCDGO = p.PLNSCDGO
LEFT   JOIN CNT.PLNN n ON n.PLNNCDGO = d.PLNNCDGO
WHERE  p.PLNSCDAL = 35
AND    d.DTPLAXL1 IN (1, 2)
ORDER  BY d.DTPLAXL1;

-- ESPERADO (segun docs/logica-negocio/crd/sql/173, control A.1/EJECUCION):
--   AUX1=1   MOVIMIENTO=1 (DEBE)     CUENTA=2.1.02.25.01  CTA INDIVIDUAL DE PENSIONES COMPLEMENTARIAS
--   AUX1=2   MOVIMIENTO<>1 (HABER)   CUENTA=2.3.01.10.03  PENSIONES COMPLEMENTARIAS POR PAGAR
--
-- Si no da esto: PARAR. El fix de generarAsientoDevengoPension asume esta parametrizacion
-- (aux1=2 es la cuenta que el devengo acredita y el pago al banco tendria que debitar) y si
-- el catalogo real dice otra cosa, el diseño de H41/H43 hay que revisarlo antes de escribir
-- una sola linea de Java.

-- =====================================================================================
-- BLOQUE 2 - Referencia cruzada: la misma cuenta 2.1.02.25.01 aparece en la plantilla 21
--            (aux1=53, el cruce contra prestamo -- ver sql/199) Y en la 35 (aux1=1, el
--            devengo). Confirma que es la MISMA cuenta individual tocada desde los dos
--            lados del ciclo -- coherente con el diseño, no una casualidad de nombres.
-- =====================================================================================
SELECT p.PLNSCDAL AS ALTERNO, d.DTPLAXL1 AS AUX1, d.DTPLMVMN AS MOVIMIENTO,
       n.PLNNCNTA AS CUENTA, n.PLNNNMBR AS CUENTA_NOMBRE
FROM   CNT.PLNS p
JOIN   CNT.DTPL d ON d.PLNSCDGO = p.PLNSCDGO
LEFT   JOIN CNT.PLNN n ON n.PLNNCDGO = d.PLNNCDGO
WHERE  n.PLNNCNTA = '2.1.02.25.01'
ORDER  BY p.PLNSCDAL, d.DTPLAXL1;
