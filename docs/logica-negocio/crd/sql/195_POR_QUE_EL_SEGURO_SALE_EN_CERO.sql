-- =====================================================================================
-- 195 - Por que el prevuelo de la corrida muestra SEGURO MEDICO en $0,00
-- FECHA: 2026-09-04 · EQUIPO: CRD / Equipo B (eqB, omen-saa-1)
--
-- SOLO SELECT. No modifica una sola fila.
-- Sin comandos de SQL*Plus: no usa PROMPT, DEFINE, SET ni &variables.
--
-- =====================================================================================
-- EL SINTOMA
-- =====================================================================================
-- Prevuelo de agosto 2026: 180 evaluados, 136 aptos, 44 bloqueados.
--   A PRESTAMOS  $16.231,60
--   A DINERO    $113.278,63
--   TOTAL       $129.510,23   <- el usuario VALIDO estos numeros: estan bien
--   SEGURO           $0,00    <- esto es lo unico que no cuadra
--
-- =====================================================================================
-- LO QUE YA ESTA DESCARTADO (revisado en el codigo, no hace falta volver a mirarlo)
-- =====================================================================================
-- La cadena entera esta bien escrita, punta a punta:
--
--   VPPC.VPPCVLSR -> vppc.getValorSeguro()                     (entidad JPA, mapeo correcto)
--   PagoPensionComplementariaServiceImpl:366  valorSeguroMensual = vppc.getValorSeguro()
--   PagoPensionComplementariaServiceImpl:470  totalPension = total * (valorPension/valorTotal)
--   PagoPensionComplementariaServiceImpl:471  totalSeguro  = total - totalPension
--   PagoPensionComplementariaServiceImpl:295  totalSeguroGeneral += fila.getTotalSeguro()
--   ResultadoPrevisualizacionCorrida.totalSeguroGeneral        (DTO)
--   corrida-mes-pago-jubilados.component.html:134  res.totalSeguroGeneral     (pantalla)
--
-- Los nombres coinciden en los siete puntos. No hay un campo perdido en el camino.
--
-- =====================================================================================
-- (!) POR LO TANTO SOLO QUEDAN DOS EXPLICACIONES, Y ESTE SCRIPT LAS SEPARA
-- =====================================================================================
-- La suma acumula el seguro UNICAMENTE de las filas APTAS (linea 294: dentro de
-- "if (fila.isApto())"). Y totalSeguro sale de "total", que es cero cuando no hay meses
-- adeudados. Entonces $0,00 exacto solo puede venir de una de estas:
--
--   A. Los que tienen seguro cargado estan entre los 44 BLOQUEADOS.
--      Su seguro nunca entra a la suma. El $0 es correcto; falta pagarles por otra razon.
--
--   B. Los que tienen seguro cargado estan AL DIA (cero meses adeudados).
--      Son aptos, pero su "total" es 0, y cero pension implica cero seguro.
--      El $0 es CORRECTO y no hay nada que arreglar.
--
--   C. Ninguna de las dos: hay un apto con seguro cuyo total NO entro. ESO SI ES UN
--      DEFECTO y hay que ir a buscarlo. El bloque 2 lo dice con nombre y apellido.
--
-- =====================================================================================

-- ==========================================================================
-- BLOQUE 1 - Cuantos tienen seguro cargado, y cuanto suma
-- ==========================================================================
-- Confirma el punto de partida antes de interpretar nada mas.

SELECT COUNT(*)                                                    AS CONFIGS_ACTIVAS,
       SUM(CASE WHEN NVL(v.VPPCVLSR,0) > 0 THEN 1 ELSE 0 END)      AS CON_SEGURO,
       SUM(CASE WHEN NVL(v.VPPCVLSR,0) = 0 THEN 1 ELSE 0 END)      AS SIN_SEGURO,
       ROUND(SUM(NVL(v.VPPCVLSR,0)), 2)                            AS SEGURO_MENSUAL_TOTAL
  FROM CRD.ENTD e
  JOIN CRD.VPPC v ON v.ENTDCDGO = e.ENTDCDGO AND v.VPPCIDST = 1
 WHERE e.ENTDIDST = 3;

--
-- (!) Se espera CON_SEGURO = 8 y SEGURO_MENSUAL_TOTAL = 450,40 (lo que ya midio el
--     usuario el 2026-09-04). Si da otra cosa, el resto de este script hay que releerlo
--     con ese numero nuevo en la mano.
--

-- ==========================================================================
-- BLOQUE 2 - (!) EL BLOQUE QUE DECIDE: los que tienen seguro, uno por uno
-- ==========================================================================
-- Reproduce, en SQL, las mismas puertas que aplica previsualizarJubilado, en el mismo
-- orden. La columna DIAGNOSTICO dice en cual de ellas cae cada uno.

SELECT x.CEDULA,
       x.NOMBRE,
       x.VALOR_PAGAR,
       x.VALOR_SEGURO,
       x.SALDO,
       x.TIENE_CERTIFICADO,
       x.PRESTAMOS_VIGENTES,
       x.ANCLA,
       x.MESES_ADEUDADOS,
       CASE
         WHEN x.VALOR_PAGAR <= 0.01
              THEN 'BLOQUEADO - valorPagar en 0'
         WHEN x.ANCLA IS NULL
              THEN 'BLOQUEADO - SIN_ANCLA'
         WHEN x.MESES_ADEUDADOS <= 0
              THEN 'AL DIA - apto pero total 0, seguro 0 CORRECTO'
         WHEN x.PRESTAMOS_VIGENTES = 0 AND x.TIENE_CERTIFICADO = 'NO'
              THEN 'BLOQUEADO - sin prestamo y sin certificado'
         WHEN x.SALDO <= 0.005
              THEN 'APTO pero saldo 0 - total 0, seguro 0'
         ELSE '(!) APTO CON MESES Y SALDO - su seguro DEBERIA sumar'
       END                                                         AS DIAGNOSTICO
  FROM (
    SELECT e.ENTDNMID                                              AS CEDULA,
           SUBSTR(e.ENTDRZNS,1,32)                                 AS NOMBRE,
           NVL(v.VPPCVLRR,0)                                       AS VALOR_PAGAR,
           NVL(v.VPPCVLSR,0)                                       AS VALOR_SEGURO,
           NVL((SELECT SUM(a3.APRTVLRR) FROM CRD.APRT a3
                 WHERE a3.ENTDCDGO = e.ENTDCDGO
                   AND a3.TPAPCDGO = 23), 0)                       AS SALDO,
           CASE WHEN EXISTS (
                  SELECT 1 FROM CRD.CNBP c
                    JOIN CRD.ADJN d ON d.ADJNIDRF = c.CNBPCDGO
                    JOIN CRD.TPDJ t ON t.TPDJCDGO = d.TPDJCDGO
                   WHERE c.ENTDCDGO = e.ENTDCDGO
                     AND c.CNBPIDST = 1
                     AND d.ADJNIDST = 1
                     AND UPPER(TRIM(t.TPDJNMBR)) = 'CERTIFICADO BANCARIO')
                THEN 'SI' ELSE 'NO' END                            AS TIENE_CERTIFICADO,
           (SELECT COUNT(*) FROM CRD.PRST p
             WHERE p.ENTDCDGO = e.ENTDCDGO
               AND p.PRSTIDST IN (2,3))                            AS PRESTAMOS_VIGENTES,
           TRUNC(NVL((SELECT MAX(a.APRTFCTR) FROM CRD.APRT a
                       WHERE a.ENTDCDGO = e.ENTDCDGO
                         AND a.TPAPCDGO = 23
                         AND a.APRTVLRR < 0),
                     (SELECT MAX(a2.APRTFCTR) FROM CRD.APRT a2
                       WHERE a2.ENTDCDGO = e.ENTDCDGO
                         AND a2.TPAPCDGO = 23
                         AND a2.APRTTPMV = 7
                         AND a2.APRTVLRR > 0)), 'MM')              AS ANCLA,
           NVL(MONTHS_BETWEEN(
               TO_DATE('2026-08-01','YYYY-MM-DD'),
               TRUNC(NVL((SELECT MAX(a.APRTFCTR) FROM CRD.APRT a
                           WHERE a.ENTDCDGO = e.ENTDCDGO
                             AND a.TPAPCDGO = 23
                             AND a.APRTVLRR < 0),
                         (SELECT MAX(a2.APRTFCTR) FROM CRD.APRT a2
                           WHERE a2.ENTDCDGO = e.ENTDCDGO
                             AND a2.TPAPCDGO = 23
                             AND a2.APRTTPMV = 7
                             AND a2.APRTVLRR > 0)), 'MM')), -1)    AS MESES_ADEUDADOS
      FROM CRD.ENTD e
      JOIN CRD.VPPC v ON v.ENTDCDGO = e.ENTDCDGO AND v.VPPCIDST = 1
     WHERE e.ENTDIDST = 3
       AND NVL(v.VPPCVLSR,0) > 0
  ) x
 ORDER BY x.MESES_ADEUDADOS DESC, x.VALOR_SEGURO DESC;

--
-- (!) NOTA SOBRE EL ANCLA: la fecha se calcula con MAX del movimiento de JUBILACION
--     (APRTTPMV = 7), que es lo que hace resolverAnclaRetroactivo en el codigo.
--     El sql/194 usaba MIN de TODOS los movimientos positivos, que NO es lo mismo -
--     por eso aquel media un retroactivo mas largo. Este script sigue al codigo.
--
-- COMO SE LEE:
-- (!) Si TODAS las filas dicen "AL DIA" o "BLOQUEADO" -> el $0,00 es CORRECTO. A esas 8
--     personas no se les debe seguro de agosto porque no se les debe agosto. No hay nada
--     que corregir en el codigo y la corrida se puede ejecutar.
-- (!) Si alguna dice "(!) APTO CON MESES Y SALDO" -> ESO es un defecto real y hay que
--     pararse ahi antes de ejecutar. Pasar esa cedula y voy directo a buscarlo.
--

-- ==========================================================================
-- BLOQUE 3 - El contraste: los mismos numeros del prevuelo, desde la base
-- ==========================================================================
-- Cuenta cuantos jubilados caen en cada rama, para cuadrar contra el "136 aptos /
-- 44 bloqueados" que muestra la pantalla. Si los numeros coinciden, el prevuelo esta
-- leyendo la misma realidad que este script y su seguro en 0 es de fiar.

SELECT SUM(CASE WHEN y.VALOR_PAGAR <= 0.01                    THEN 1 ELSE 0 END) AS BLQ_SIN_VALOR,
       SUM(CASE WHEN y.VALOR_PAGAR >  0.01
                 AND y.MESES_ADEUDADOS <= 0                   THEN 1 ELSE 0 END) AS AL_DIA,
       SUM(CASE WHEN y.VALOR_PAGAR >  0.01
                 AND y.MESES_ADEUDADOS >  0
                 AND y.PRESTAMOS_VIGENTES = 0
                 AND y.TIENE_CERTIFICADO = 'NO'               THEN 1 ELSE 0 END) AS BLQ_SIN_SALIDA,
       SUM(CASE WHEN y.VALOR_PAGAR >  0.01
                 AND y.MESES_ADEUDADOS >  0
                 AND (y.PRESTAMOS_VIGENTES > 0
                      OR y.TIENE_CERTIFICADO = 'SI')          THEN 1 ELSE 0 END) AS APTOS_CON_MONTO
  FROM (
    SELECT NVL(v.VPPCVLRR,0)                                       AS VALOR_PAGAR,
           CASE WHEN EXISTS (
                  SELECT 1 FROM CRD.CNBP c
                    JOIN CRD.ADJN d ON d.ADJNIDRF = c.CNBPCDGO
                    JOIN CRD.TPDJ t ON t.TPDJCDGO = d.TPDJCDGO
                   WHERE c.ENTDCDGO = e.ENTDCDGO
                     AND c.CNBPIDST = 1
                     AND d.ADJNIDST = 1
                     AND UPPER(TRIM(t.TPDJNMBR)) = 'CERTIFICADO BANCARIO')
                THEN 'SI' ELSE 'NO' END                            AS TIENE_CERTIFICADO,
           (SELECT COUNT(*) FROM CRD.PRST p
             WHERE p.ENTDCDGO = e.ENTDCDGO
               AND p.PRSTIDST IN (2,3))                            AS PRESTAMOS_VIGENTES,
           NVL(MONTHS_BETWEEN(
               TO_DATE('2026-08-01','YYYY-MM-DD'),
               TRUNC(NVL((SELECT MAX(a.APRTFCTR) FROM CRD.APRT a
                           WHERE a.ENTDCDGO = e.ENTDCDGO
                             AND a.TPAPCDGO = 23
                             AND a.APRTVLRR < 0),
                         (SELECT MAX(a2.APRTFCTR) FROM CRD.APRT a2
                           WHERE a2.ENTDCDGO = e.ENTDCDGO
                             AND a2.TPAPCDGO = 23
                             AND a2.APRTTPMV = 7
                             AND a2.APRTVLRR > 0)), 'MM')), -1)    AS MESES_ADEUDADOS
      FROM CRD.ENTD e
      JOIN CRD.VPPC v ON v.ENTDCDGO = e.ENTDCDGO AND v.VPPCIDST = 1
     WHERE e.ENTDIDST = 3
  ) y;

--
-- (!) AL_DIA + APTOS_CON_MONTO deberia acercarse a 136. No tiene por que dar EXACTO: este
--     script no reproduce el tope por saldo ni la deuda exigible por cuota, y ademas los
--     que tienen DOS VPPC activas revientan en el codigo y salen bloqueados, mientras que
--     aca el JOIN los duplica. Una diferencia de pocas unidades es normal; una de decenas
--     significa que estoy midiendo otra poblacion y hay que avisar antes de ejecutar.
--

-- =====================================================================================
-- No hay bloque de reverso: este script no escribe nada.
-- =====================================================================================
