-- =====================================================================================
-- ¿QUE cobros dejaron la transitoria cargada, y de cuando son?
-- FECHA: 2026-08-31 · Equipo A de crd
--
-- ⛔ SOLO LECTURA.
--
-- LA PREGUNTA. El usuario reporto que "todos los cobros deben dar de baja las cuentas
-- transitorias, no solo pago de cuotas o abono de capital". El saldo de la transitoria es
-- de **-$2.973.328,49 sobre 521 lineas**.
--
-- LO QUE EL CODIGO DICE (verificado hoy, linea por linea en `validar()`): los SIETE tipos
-- de operacion suman en el asiento de reparto. `idPrestamo` es obligatorio para todos los
-- que no son aporte — la condonacion incluida, que exige prestamo Y acuerdo — y los de
-- aporte llevan `idTipoAporte`. **No hay ningun tipo que hoy deje la transitoria cargada.**
--
-- LA HIPOTESIS ALTERNATIVA, que estas consultas confirman o desmienten: **el asiento de
-- reparto es de HOY.** Antes de este despliegue el circuito tenia DOS asientos y ninguno
-- cerraba la transitoria — por eso se construyo el tercero. Si es asi, esos -$2,97 M son
-- historial acumulado, no un tipo de operacion con un hueco.
--
-- La diferencia importa: si es historial, se arregla con un ajuste puntual de una vez; si
-- es un hueco vigente, sigue creciendo con cada cobro.
-- =====================================================================================


-- 1. LA TRANSITORIA POR MES. Si el saldo deja de crecer a partir del despliegue de hoy,
--    es historial. Si sigue creciendo despues, hay un hueco vigente.
SELECT TO_CHAR(a.ASNTFCHA, 'YYYY-MM')                AS PERIODO,
       COUNT(*)                                      AS LINEAS,
       SUM(NVL(d.DTASDBEE,0))                        AS DEBE,
       SUM(NVL(d.DTASHBRR,0))                        AS HABER,
       SUM(NVL(d.DTASDBEE,0)) - SUM(NVL(d.DTASHBRR,0)) AS SALDO_DEL_MES
FROM   CNT.DTAS d
JOIN   CNT.ASNT a ON a.ASNTCDGO = d.ASNTCDGO
WHERE  d.DTASCNTA = '2.3.01.15.01'
GROUP  BY TO_CHAR(a.ASNTFCHA, 'YYYY-MM')
ORDER  BY 1;


-- 2. LOS COBROS PROCESADOS SIN ASIENTO DE REPARTO, POR TIPO DE OPERACION.
--    Un cobro PROCESADO con CBCRASRP nulo es uno que cargo la transitoria y no la descargo.
--    Si todos son anteriores al despliegue de hoy, confirma la hipotesis del historial.
SELECT c.CBCRTPOO                          AS TIPO_OPERACION,
       COUNT(*)                            AS CUANTOS,
       SUM(NVL(c.CBCRVLRR,0))              AS VALOR_TOTAL,
       MIN(c.CBCRFCHA)                     AS DESDE,
       MAX(c.CBCRFCHA)                     AS HASTA
FROM   CRD.CBCR c
WHERE  c.CBCRASRP IS NULL
AND    c.CBCRASN1 IS NOT NULL      -- cargo la transitoria
GROUP  BY c.CBCRTPOO
ORDER  BY 2 DESC;


-- 3. LOS QUE SI TIENEN LOS TRES ASIENTOS — deberian ser solo los de hoy en adelante.
--    Si esta vacia, el asiento de reparto todavia no se genero NUNCA: mirar el log del
--    servidor antes de seguir buscando en la base.
SELECT c.CBCRCDGO, c.CBCRTPOO, c.CBCRFCHA, c.CBCRVLRR,
       c.CBCRASN1 AS TRANSITORIO, c.CBCRASRP AS REPARTO, c.CBCRASN2 AS DEFINITIVO
FROM   CRD.CBCR c
WHERE  c.CBCRASRP IS NOT NULL
ORDER  BY c.CBCRCDGO DESC
FETCH  FIRST 30 ROWS ONLY;


-- 4. EL DETALLE DE UN COBRO PUNTUAL — cambiar el codigo por uno que se sospeche mal.
--    Muestra los tres asientos con sus lineas y cuentas, para ver exactamente que se
--    contabilizo y contra que.
-- SELECT c.CBCRCDGO, c.CBCRTPOO, a.ASNTCDGO, a.ASNTFCHA, a.ASNTOBSR,
--        d.DTASCNTA, d.DTASNMCT, d.DTASDBEE, d.DTASHBRR, d.DTASDSCR
-- FROM   CRD.CBCR c
-- JOIN   CNT.ASNT a ON a.ASNTCDGO IN (c.CBCRASN1, c.CBCRASRP, c.CBCRASN2)
-- JOIN   CNT.DTAS d ON d.ASNTCDGO = a.ASNTCDGO
-- WHERE  c.CBCRCDGO = 9999
-- ORDER  BY a.ASNTCDGO, d.DTASCDGO;
