# Resumen Financiero de los Informes Mensuales — 1.4.02 (D-206) y aportes fuera de la 2.1 (lote 26)

Equipo omen-saa-1 · 2026-11-26. Servicio: `ResumenFinancieroCarteraServiceImpl` (`GET /rest/ejcc/resumenFinanciero/{codigo}`). Contexto de las cifras: `ANALISIS-DESCUADRE-RESUMEN-FINANCIERO-SEP-2026.md`.

## 1. Grupo 6 «INTERESES POR COBRAR (INTERES VENCIDO)»
Regla vigente D-206: la 1.4.02 = interés vencido y no cobrado por cuota al corte + mora contabilizada pendiente (la misma base de la provisión 1.4.99.05).
- La línea comparable de cada cuenta 1.4.02.xx (PQ 1.4.02.05; PP y PRE 1.4.02.10; PH 1.4.02.15, de `CUENTAS_POR_TIPO[0]`) vale **interés + mora del tipo** (RPR.CCPB, rubros
  `INTERES_VENCIDO` y `MORA`) contra el saldo de esa cuenta. PP y PRE comparten la 1.4.02.10 y se suman en la misma línea. El concepto lo dice («… - interes + mora»).
- Una mora sin tipo o sin cuenta configurada cae en «(sin cuenta configurada)», como las demás líneas por cuenta.
- Debajo, el desglose informativo (no comparable) por cuenta: «Interes vencido (desglose…)» y «Mora pendiente (desglose…)», con sus registros.
- Grupo 8 (mora calculada al corte): sin cambio de líneas; la nota ahora dice que desde D-206 forma parte de la 1.4.02 (ver grupo 6).
- La fuente de la mora es `opCorte.getMora()` de la G48 al corte, escrita en RPR.CCPB por `GeneracionCCPMServiceImpl` (rubro `MORA`); el interés, `opCorte.getInteresExigible()`.

## 2. Grupo 9 «APORTES (CPRM)»
- La consulta de RPR.CPRM se agrupa por `TPAPCDGO` (no solo por nombre) y se lee la cuenta de pasivo de cada tipo en `CRD.CTAP` (`CTAPPLNP` → `CNT.PLNN.PLNNCNTA`), con el
  mismo filtro que `CuentaTipoAporteDao.selectByTipoAporteYEmpresa`: la empresa de la ejecución y `CTAPESTD = 1`.
- Los tipos cuya cuenta no es la 2.1 ni empieza con `2.1.` (hoy el tipo 27, seguro por entregar a beneficiarios, 2.3.90.90.11) **salen** del «Total aportes (CPRM)» y van en
  su propia línea comparable: valor CPRM contra −saldo de su cuenta (signo acreedor). Dos tipos que comparten cuenta fuera de 2.1 van en una línea. Ningún tipo está
  quemado en el código.
- Un tipo sin fila vigente en CTAP se queda en el total 2.1, como antes, y su nota dice «Sin cuenta en CTAP».
- La pensión complementaria sigue igual, dentro del total 2.1.
- El saldo de las cuentas fuera de 2.1 se lee con una consulta propia (`saldoDeCuentaAlCorte`), porque el mapa de saldos solo trae 1.3, 1.4 y 2.1; usa el mismo filtro de asientos
  (estado 1 y 3, fecha < corte + 1, empresa).

## 3. Criterio de aceptación (marzo 2026, ejecución de informes 669, B17 de marzo) — NO ejecutado
| Línea | Valor del módulo |
|---|---|
| 1.4.02.05 | 206.705,13 (interés 163.202,85 + mora 43.502,28) |
| 1.4.02.10 | 4.654,10 (3.055,82 + 1.598,28) |
| 1.4.02.15 | 1.134.266,86 (1.065.005,69 + 69.261,17) |
| Total aportes 2.1 | 48.700.549,48 (= G42) |
| Tipo 27 contra 2.3.90.90.11 | 2.000,00 |

## 4. Consultas de control (solo lectura) para la base de trabajo
Parámetros: `:ejcc` = 669, `:empresa` = la empresa de la ejecución, `:corte` = último día del mes (2026-03-31).

```sql
-- A) 1.4.02 por cuenta: interes + mora del módulo (RPR.CCPB) contra el saldo contable.
WITH modulo AS (
  SELECT CASE t.TPPRCSPB WHEN 'PQ' THEN '1.4.02.05' WHEN 'PH' THEN '1.4.02.15'
                         WHEN 'PP' THEN '1.4.02.10' WHEN 'PRE' THEN '1.4.02.10' END AS cuenta,
         SUM(CASE WHEN c.CCPBRBRO = 'INTERES_VENCIDO' THEN c.CCPBVLOR ELSE 0 END) AS interes,
         SUM(CASE WHEN c.CCPBRBRO = 'MORA'            THEN c.CCPBVLOR ELSE 0 END) AS mora
    FROM RPR.CCPB c LEFT JOIN CRD.TPPR t ON t.TPPRCDGO = c.CCPBTPPR
   WHERE c.CCPBEJCC = :ejcc AND c.CCPBRBRO IN ('INTERES_VENCIDO', 'MORA')
   GROUP BY CASE t.TPPRCSPB WHEN 'PQ' THEN '1.4.02.05' WHEN 'PH' THEN '1.4.02.15'
                            WHEN 'PP' THEN '1.4.02.10' WHEN 'PRE' THEN '1.4.02.10' END),
saldo AS (
  SELECT d.DTASCNTA AS cuenta, SUM(NVL(d.DTASDBEE,0)) - SUM(NVL(d.DTASHBRR,0)) AS saldo
    FROM CNT.DTAS d JOIN CNT.ASNT a ON a.ASNTCDGO = d.ASNTCDGO
   WHERE a.ASNTESTD IN (1, 3) AND a.ASNTFCHA < :corte + 1 AND a.PJRQCDGO = :empresa
     AND (d.DTASCNTA LIKE '1.4.02.05%' OR d.DTASCNTA LIKE '1.4.02.10%' OR d.DTASCNTA LIKE '1.4.02.15%')
   GROUP BY d.DTASCNTA)
SELECT NVL(m.cuenta, s.cuenta) AS cuenta, m.interes, m.mora, NVL(m.interes,0) + NVL(m.mora,0) AS modulo_interes_mas_mora,
       s.saldo, NVL(m.interes,0) + NVL(m.mora,0) - NVL(s.saldo,0) AS diferencia
  FROM modulo m FULL OUTER JOIN saldo s ON s.cuenta = m.cuenta
 ORDER BY 1;

-- B) Aportes: CPRM por tipo con la cuenta de pasivo de CTAP (misma regla del servicio: empresa y CTAPESTD = 1).
--    Si un tipo sale repetido, tiene más de una fila CTAP vigente en la empresa.
SELECT c.TPAPCDGO, MAX(tp.TPAPNMBR) AS tipo, MIN(pl.PLNNCNTA) AS cuenta_ctap, SUM(NVL(c.CPRMTTAL,0)) AS valor_cprm, COUNT(*) AS registros
  FROM RPR.CPRM c
  LEFT JOIN CRD.TPAP tp ON tp.TPAPCDGO = c.TPAPCDGO
  LEFT JOIN CRD.CTAP ct ON ct.TPAPCDGO = c.TPAPCDGO AND ct.CTAPESTD = 1 AND ct.PJRQCDGO = :empresa
  LEFT JOIN CNT.PLNN pl ON pl.PLNNCDGO = ct.CTAPPLNP
 WHERE c.CPRMEJCC = :ejcc
 GROUP BY c.TPAPCDGO
 ORDER BY 3, 1;

-- C) Saldo acreedor (haber - debe) de la 2.1 en total y de las cuentas fuera de 2.1 que salgan en B (p. ej. 2.3.90.90.11).
SELECT CASE WHEN d.DTASCNTA LIKE '2.1.%' THEN '2.1 (total)' ELSE d.DTASCNTA END AS cuenta,
       SUM(NVL(d.DTASHBRR,0)) - SUM(NVL(d.DTASDBEE,0)) AS saldo_acreedor
  FROM CNT.DTAS d JOIN CNT.ASNT a ON a.ASNTCDGO = d.ASNTCDGO
 WHERE a.ASNTESTD IN (1, 3) AND a.ASNTFCHA < :corte + 1 AND a.PJRQCDGO = :empresa
   AND (d.DTASCNTA LIKE '2.1.%' OR d.DTASCNTA LIKE '2.3.90.90.11%')
 GROUP BY CASE WHEN d.DTASCNTA LIKE '2.1.%' THEN '2.1 (total)' ELSE d.DTASCNTA END;
```
Lectura: en A, `modulo_interes_mas_mora` debe igualar `saldo` por cuenta (aceptación de la sección 3). En B, la suma de `valor_cprm` de los tipos con cuenta en 2.1 (más los sin fila CTAP) = el total 2.1
de C = G42; el tipo con cuenta fuera de 2.1 = su fila de C.

## 5. No cambiado
Los demás grupos, la contabilidad y el DTO (`LineaResumenFinanciero` ya tenía todo lo necesario: concepto, cuenta, comparable, nota).
