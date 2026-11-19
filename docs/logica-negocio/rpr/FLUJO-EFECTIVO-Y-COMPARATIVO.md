# Flujo de efectivo, flujo de caja y comparativo con los formatos de ASOPREP (C38)

**2026-11-19 · tarea C38 del cuadre con la Superintendencia.** Inventario en
[REPORTES-SUPERINTENDENCIA.md](REPORTES-SUPERINTENDENCIA.md); motor de estructuras en
[ESTRUCTURAS-DE-RENGLONES.md](ESTRUCTURAS-DE-RENGLONES.md). Documento del expediente: cuadreSuper
`03-trabajo/cambios-saa/C38-FLUJO-Y-COMPARATIVO.md` (pruebas, comparación con los formatos de Contabilidad).

## 1. Qué genera

| Reporte | Formato de ASOPREP | Endpoint |
|---|---|---|
| Estado de flujos de efectivo, método indirecto, **del mes** | `FLUJO DE EFECTIVO_ENE_2026 FORMATO.xlsx` (filas 7–61, columna G de códigos) | `POST /flef/generar` (JSON) · `POST /flef/excel` |
| Ídem, **acumulado del año** | `FLUJO EFECTIVO_12_2025.xlsx` (mismas filas dos más arriba, sin columna G) | ídem con `periodo: "ACUMULADO"` |
| Flujo de caja real, método directo (BIESS) | `FLUJO DE CAJA_12_2025_FORMATO.xlsx` (una columna por mes) | `POST /flef/generar` con `incluirDirecto: true` · `POST /flef/excel` con `reporte: "DIRECTO"` |
| Balance comparativo 31-dic anterior vs mes | `COMPARATIVO_01_2026_FORMATO.xlsx` | `POST /flef/comparativo` (JSON) · `POST /flef/comparativoExcel` |

Cuerpo: `{empresa, anio, mes, periodo: "MES" | "ACUMULADO", preliminar?, incluirDirecto?, reporte?: "INDIRECTO" | "DIRECTO"}`.
El Excel sale con `Content-Disposition` (`FLUJO_EFECTIVO_2025-12_ACUMULADO.xlsx`, `FLUJO_CAJA_…`, `COMPARATIVO_2026-01.xlsx`,
con `_PRELIMINAR` si corresponde). Errores: 400 `{mensaje}`.

## 2. Fechas, cierre y PRELIMINAR

- **Corte** = último día del mes. **Base** = cierre del mes anterior (`MES`) o 31-dic del año anterior (`ACUMULADO`, y la
  columna E del comparativo). Si la contabilidad de la empresa empieza después de la base (primer asiento; en ASOPREP el
  asiento de apertura del 31-may-2025), la base es esa fecha: el acumulado de 2025 es **1-jun a 31-dic** desde el balance de
  arranque, y lo dice una nota.
- **Mes cerrado:** el B17 de los dos cortes se pide a `/esrp` (`EstructuraReporteService.generar` con `fechaComparacion` =
  base), que exige el mes de corte cerrado salvo `preliminar: true` (`REPORTE_MES_NO_CERRADO`, igual que el B17). Con
  `preliminar` la respuesta trae `preliminar: true`, `leyenda: "PRELIMINAR"`, el Excel lo rotula y el archivo lleva `_PRELIMINAR`.
- **Cierre del ejercicio** (asiento del 1-ene, D-068): si la base es de un año anterior al corte, la utilidad (FE10) es el
  resultado del año en curso y el del año anterior se descuenta de la fila de resultados (FE44), que lo recibió en el cierre.

## 3. Definiciones (semilla `sql/C38-01-semilla-flujo-efectivo-comparativo.sql`)

Tres reportes nuevos de la 1236 en `CNT.RPRT` (tipo 2, `RPRTFRAR = 2` «derivado del B17»; `/esrp` los rechaza):
**FLE** (alterno 8), **FCD** (alterno 9) y **CMP** (alterno 10). No hay códigos de cuenta en Java.

- **FLE** — `RNRPORDN` = fila del formato de enero 2026; `RNRPNVLL` 0 título/nota, 1 columna F, 2 columna D;
  `RNRPTPCL` 1 = fórmula sobre **códigos del B17** con efecto en caja (activo «−», pasivo y patrimonio «+»), con base
  2 variación / 1 al corte / 3 a la base; `RNRPTPCL` 3 = suma de renglones (los subtotales, que el Excel escribe como fórmula).
  Mapeo = cuadreSuper `03-trabajo/flujo-efectivo/README.md` §2.
- **FCD** — filas del formato BIESS y seis cuentas puente (`RNRPNVLL` 9). Reglas con `CRRPMDSG` 1 sobre cuentas del plan:
  caja (`1.1` → FC10), puentes (transitoria, planillas, socios por pagar, prestaciones, proveedores, nómina) y cuentas de
  orden excluidas; reglas con `CRRPMDSG` 3 (entra dinero) y 2 (sale) clasifican por **código del B17** de la cuenta final
  (prefijo, gana el más largo; las de un dígito son «toda otra cuenta»). README §3.
- **CMP** — un renglón por fila del formato (código, nombre, fórmula de las columnas E y G, nivel 0 = título) más la fila
  66 «resultado del período (5 − 4)», que el formato no trae y sin la cual la columna H no suma cero. Columna H: activo =
  E − G, pasivo y patrimonio = G − E (grupos del rubro 250).

## 4. Código

`com.saa.ejb.cnt.flujo` (puro, sin EJB ni JPA): `GeneradorFlujoEfectivo` (fechas, B17, armado), `CalculadorFlujoIndirecto`,
`CalculadorFlujoDirecto` (port de la sección 5 de `flujo-efectivo.js`: contrapartidas de la caja, neteo por pareja de
filas dentro del asiento, reparto por puentes en proporción a lo que liquidaron en asientos sin caja, resto mayor al
centavo), `CalculadorComparativo`, `ExcelFormatosAsoprep` (POI), `FuenteDatosFlujo` (la interfaz de datos) y
`ConsultasFlujo` (SQL). `FlujoEfectivoServiceImpl` implementa la fuente con `EstructuraReporteService` y consultas nativas
de solo lectura, y pone razón social, RUC y firmas del rubro 250. `FlujoEfectivoRest` (`/flef`).
Cambios en clases existentes: `EstructuraReporteService` expone `definicionEstructura`, `renglonesVigentes` y
`reglasVigentes`; `/esrp/generar` rechaza `RPRTFRAR = 2`; `CalculadorEstructura.renglonDeCuenta` (renglón del B17 de una
cuenta: regla más específica o mapeo automático).

## 5. Controles (en el JSON y en la hoja «Controles» de cada Excel)

Indirecto: caja inicial + flujo neto = 11 del B17 al corte; flujo neto = Δ11; utilidad = (5 − 4) del período; ecuación
del B17 a los dos cortes; códigos del B17 con variación sin fila. Directo, por mes: saldo inicial + ingresos − egresos =
caja al corte; neto de contrapartidas = movimiento de la caja; advertencia por lo que pasó por un puente y no se pudo
seguir. Directo vs indirecto: saldo inicial = caja inicial y Σ meses = flujo neto. Comparativo: cada título = suma de sus
filas (advierte saldos en códigos que el formato no trae) y Σ columna H = 0.

## 6. Validación (base de trabajo, 2026-11-19)

Arnés fuera del servidor (cuadreSuper `03-trabajo/cambios-saa/c38-flujo-comparativo/prueba/PruebaFlujoC38.java`): corre
estas mismas clases con los datos de `saa-oracle-23ai` (solo SELECT) y la semilla C38-01, y compara con el script
`flujo-efectivo.js`: dic-2025 acumulado y mes, ene-2026 mes y acumulado — **iguales al centavo** en el indirecto, en las
filas del directo de cada mes (jun–dic 2025, ene 2026) y en el detalle por fila y cuenta; controles sin errores.

## 7. Pendiente

Correr la semilla C38-01 (PASE-A-PRODUCCION §4), desplegar y repetir la prueba por REST; historia laboral del BIESS
(filas 37–39) y tipo de fondo (C3) no salen de la contabilidad; mora (fila 16) y novaciones (17) en 0 como el script;
retirar el FLC provisional (C19-03) cuando el directo de `/flef` quede aceptado.
