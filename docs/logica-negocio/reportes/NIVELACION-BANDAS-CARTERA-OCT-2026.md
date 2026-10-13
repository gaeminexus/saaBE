# Nivelación de las cuentas de banda de cartera — asiento manual de cuadre, octubre 2026

**Equipo:** `lap-saa-1` (árbitro) · **Fecha:** 2026-10-13 · **Script:** `sql/lap1-17-nivelacion-bandas-cartera-oct-2026.sql`
**Origen:** `ANALISIS-DESCUADRE-RESUMEN-FINANCIERO-SEP-2026.md` §3.2 y `DISENO-CCPM-BANDAS-COMO-EL-CIERRE.md` §3.A.
**Decisión del usuario (2026-10-13):** se hace, una vez, como asiento manual de cuadre de bandas, ingresado en octubre.

---

## 1. Qué problema resuelve, en dos párrafos

Una cuenta de banda (`1.3.01.05` «quirografarios por vencer de 1 a 30 días», y las otras 50) debería
ser una **foto**: el capital de las cuotas que, a la fecha de referencia, caen en esa banda. El cierre
de cartera calcula esa foto cada mes (`CRD.BDCC`), pero **no la escribe en el mayor**: asienta sólo la
*diferencia* entre la foto del mes anterior y la de este mes, sobre la misma cartera (reclasificación a
cartera constante). Es correcto para el mes a mes, y tiene una consecuencia: **si el saldo de partida
estaba mal, el cierre lo arrastra para siempre** — sólo mueve lo que envejeció.

Y el saldo de partida está mal desde el origen: la migración (28-02) repartió la cartera con otra
regla, y siete asientos manuales (mayo a septiembre) la fueron igualando cada mes **al informe**, que
reparte por número de cuota y clasifica el vencido por préstamo entero. Medido hoy (§3): el mayor
tiene `1.3.01.05` en 349.235,71 contra una foto de 238.928,53, y tres cuentas de vencido con **saldo
negativo** (`1.3.04.10` −19.604,43; `1.3.04.20` −14.347,02; `1.3.12.05` −29.679,23) — un activo no
puede ser negativo; es el rastro de asentar deltas sobre un nivel equivocado.

La nivelación es **un asiento de reclasificación, una sola vez**, que lleva cada cuenta de banda de su
saldo actual al saldo de la foto. Después, los deltas mensuales del cierre mantienen el mayor igual a
la foto — mientras nadie vuelva a plugear y mientras CCPM informe con la misma regla (frente en curso).

## 2. La foto: exactamente la que dejaron los asientos del cierre de septiembre

El cierre de septiembre (`CRD.CRCT` 4, asientos `11053`–`11059` del 01-10) dejó las cuentas con esta
semántica, y la nivelación la respeta para que el cierre de **octubre** parta de donde espera:

| Cartera | Referencia | Por qué |
|---|---|---|
| **Vencido** (vencimiento anterior al 30-09) | días al **30-09** | el asiento ①.1 reclasificó el vencido a la foto del 30-09 (`distNueva`); el cierre de octubre va a asentar `foto(31-10) − foto(30-09)` sobre las mismas cuotas |
| **Por vencer** (vencimiento 30-09 o posterior) | días al **31-10**, mínimo 1 | el asiento ② reclasificó el por vencer a la foto del **31-10** (`distApertura`, «valores del mes que se abre»); las cuotas que vencen entre el 30-09 y el 31-10 quedaron en banda 1 y las mueve el ① de octubre |

El día del vencimiento todavía es por vencer (`tipoCarteraYDias`: `isBefore`), por eso el corte es
`< 30-09` para vencido. Capital pendiente por cuota = `GREATEST(DTPRCPTL − Σ PGPRCPPG vigentes, 0)`;
universo `PRSTIDST IN (2, 8, 11)`, cuotas no pagadas ni canceladas, producto y vencimiento no nulos;
bandas de `CRD.CBPR`/`BNDP` vigentes (períodos de 30 días, última abierta). Es la réplica del
algoritmo del cierre que reproduce `BDCC` cuenta por cuenta (análisis §3.1 y §9.4).

### 2.1 La trampa de las dos fechas, y cómo la evita el script

La foto se calcula con la **cartera viva del momento en que corre el script** y el mayor con los
asientos **fechados hasta ese día**. Si entre uno y otro entra un pago, se cuenta dos veces. Por eso:
el script se corre **al final del día**, después de registrar todo y antes de registrar nada del día
siguiente, y el asiento se fecha **ese día** (`TRUNC(SYSDATE)`). Y **antes del cierre de octubre**:
`CONTROL 0` no deja correr si ya existe una corrida de 2026-10.

## 3. Lo que saldría hoy (13-10-2026) — para que contabilidad lo vea antes

| Cuenta | Foto | Mayor | Ajuste (D+/H−) |
|---|---|---|---|
| 1.3.01.05 | 238.928,53 | 349.235,71 | −110.307,18 |
| 1.3.01.10 | 108.861,08 | 220.822,93 | −111.961,85 |
| 1.3.01.15 | 324.867,72 | 322.240,66 | +2.627,06 |
| 1.3.01.20 | 614.005,26 | 502.143,97 | +111.861,29 |
| 1.3.01.25 | 3.311.994,69 | 3.260.853,03 | +51.141,66 |
| 1.3.03.05 … 1.3.03.25 | | | +275,61 / −637,90 / −18,94 / +641,33 / +196,16 |
| 1.3.09.05 | 225.463,64 | 265.079,31 | −39.615,67 |
| 1.3.09.10 | 98.432,24 | 197.643,84 | −99.211,60 |
| 1.3.09.15 | 296.450,68 | 297.658,07 | −1.207,39 |
| 1.3.09.20 | 590.991,84 | 492.543,85 | +98.447,99 |
| 1.3.09.25 | 8.121.919,78 | 8.060.681,09 | +61.238,69 |
| 1.3.11.05 … 1.3.11.25 | | | +4.073,38 / −9.735,85 / −254,28 / +9.736,71 / +1.733,26 |
| 1.3.04.05 | 22.351,31 | 24.993,54 | −2.642,23 |
| 1.3.04.10 | 19.736,35 | **−19.604,43** | +39.340,78 |
| 1.3.04.15 | 57.389,85 | 3.103,00 | +54.286,85 |
| 1.3.04.20 | 53.418,27 | **−14.347,02** | +67.765,29 |
| 1.3.04.25 | 570.192,94 | 726.468,73 | −156.275,79 |
| 1.3.08.25 | 20.642,46 | 20.642,46 | 0 |
| 1.3.12.00 | 33.237,22 | 35.324,29 | −2.087,07 |
| 1.3.12.05 | 30.462,75 | **−29.679,23** | +60.141,98 |
| 1.3.12.10 | 171.618,66 | 27.282,82 | +144.335,84 |
| 1.3.12.15 | 73.678,07 | 32.793,70 | +40.884,37 |
| 1.3.12.20 | 205.145,62 | 145.164,07 | +59.981,55 |
| 1.3.12.25 | 660.808,85 | 963.630,16 | −302.821,31 |

| Familia | Foto | Mayor | Foto − mayor |
|---|---|---|---|
| Por vencer | 14.399.235,82 | 14.430.213,34 | −30.977,52 |
| Vencido | 1.918.682,35 | 1.915.772,09 | +2.910,26 |
| **Total cartera** | 16.317.918,17 | 16.345.985,43 | **−28.067,26** |

Los números cambian con cada día que pasa (pagos y entregas): el `CONTROL 1` del script muestra los
del día en que se corra. Las proporciones no cambian.

## 4. El neto entre familias: 28.067,26 que el mayor tiene y el sistema no

Las líneas por cuenta no suman cero: el mayor tiene **28.067,26 más de cartera** que las cuotas
pendientes de `CRD.DTPR`. Esa diferencia **no es reparto**, es cartera contabilizada sin respaldo en
el sistema, y necesita contrapartida. Medido:

- No hay préstamos **fuera** del universo del cierre con capital pendiente (consulta en el análisis): no
  es cartera que el sistema tenga y el cierre ignore.
- El cierre nunca cambia el total de cartera (sus asientos balancean dentro de `1.3`). Los cobros,
  entregas y Petro mueven cartera **y** `DTPR` a la vez. Lo único que movió cartera en el mayor sin
  mover `DTPR` son el asiento inicial (`5450`, 19.490.616,27 a bandas) y los manuales.
- Los manuales usaron **dos contrapartidas**: `1.9.90.10` CARTERA PENDIENTE DE REGULARIZAR (mayo y
  junio, saldo **cero** hoy) y la transitoria `2.3.01.15.01` (junio a septiembre, saldo acreedor
  1.231.193,54 hoy). Parte de esos plugs igualó el mayor a informes CCPM que incluían préstamos ya
  cancelados y saldos sin descontar pagos parciales (análisis §3.5, §4): esa es la cartera «de más».

**Recomendación:** la contrapartida del neto es `1.9.90.10` CARTERA PENDIENTE DE REGULARIZAR — es la
cuenta que contabilidad creó para exactamente esto, está en cero, y deja el ajuste **visible y
rastreable** en vez de diluirlo en la transitoria. Queda como parámetro del script (`CUENTA_CUADRE`,
dos apariciones en el PASO 2); si contabilidad decide otra, se reemplaza y listo. Lo que queda en
`1.9.90.10` es un saldo a explicar y depurar por contabilidad, no un saldo a esconder.

## 5. El asiento que se genera

- **Plantilla `1097` DIARIO**, empresa 1236, período 10/2026 (`PRDOCDGO` 4520), fecha = el día de
  corrida, usuario `JCEVALLOS` (parámetro), estado 1, rubros 15/1 como cualquier DIARIO manual;
  numeración `ASNTNMRO` (correlativo de la plantilla en el año), `ASNTNMMS` (correlativo del mes) y
  `ASNTNMAL = DIA-2026-10-NNNN`, calculados igual que los que digita contabilidad.
- **Glosa** que empieza con la marca `NIVELACION BANDAS CARTERA 2026-10` (identifica el asiento en los
  controles y en el reverso) y explica la regla y la fecha de la cartera.
- **Líneas:** una por cuenta de banda con ajuste ≥ 0,01 (`DEBE` si la foto es mayor, `HABER` si es
  menor; descripción «Nivelación bandas 2026-10 – nombre de la cuenta») y una línea de cuadre contra
  `1.9.90.10` por el neto. `DTASNMCT`/`DTASCNTA` llevan nombre y número de cuenta como en los DIARIO.
- Es una reclasificación **activo contra activo**: no toca resultados, pasivos ni provisiones.
- No hay tablas de saldos que un `INSERT` directo deje desactualizadas (`CNT.MYRZ` y `CNT.SDAS` están
  vacías; el mayor se calcula desde `DTAS`). No hay triggers sobre `ASNT`/`DTAS`. Las PK salen de
  `CNT.SQ_ASNTCDGO` / `CNT.SQ_DTASCDGO`, que son las que usa la aplicación.

## 6. Cómo correrlo

Cliente JDBC (VS Code), sentencia por sentencia, **en este orden y sin saltarse ninguno**:

| Paso | Qué tiene que dar | Si no |
|---|---|---|
| `CONTROL 0` | una fila con `PUEDE_CORRER = SI` | parar; `MOTIVO` dice qué condición falla |
| `CONTROL 1` | las líneas del asiento; revisarlas con contabilidad, en especial la fila `NETO` | parar si no se entiende alguna |
| `CONTROL 2` | número que va a recibir el asiento (informativo) | — |
| `PASO 1` | 1 fila insertada | si inserta 0, volver a `CONTROL 0` |
| `PASO 2` | N filas insertadas (≈ 30 + 1) | si revienta con **ORA-01476 (división por cero)**: la `CUENTA_CUADRE` no existe en el plan; corregirla y repetir el PASO 2 — no insertó nada |
| `CONTROL 3` | (a) `DESCUADRE = 0`; (b) la segunda consulta devuelve **cero filas** | correr el REVERSO (opción A) y avisar |

Guardas que el propio script impone: sólo en octubre 2026, sólo si existe el cierre de septiembre
ejecutado y **no** existe corrida de octubre, y sólo una vez (si ya hay un asiento con la marca, el
PASO 1 inserta 0 filas).

## 7. Después de la nivelación — lo que la mantiene

1. **No más plugs manuales** sobre `1.3.0x`. Si contabilidad ve una diferencia, se mide con la
   consulta del `CONTROL 3` (foto vs mayor por cuenta) y se busca la causa; no se tapa.
2. **El cierre de octubre** parte de donde debe: su `cargaDesviaciones` va a reportar sólo lo que los
   pagos y entregas de octubre movieron, que es lo que el diseño previó.
3. **CCPM** (frente en curso) va a informar con la misma regla: el informe de octubre y el mayor al
   01-11 van a ser la misma foto.
4. **Control mensual sugerido** (reemplaza al plug): después de cada cierre, correr la segunda consulta
   del `CONTROL 3` con la referencia del mes; lo que aparezca es deriva de los asientos individuales
   del mes (clasifican al día del pago, no a la referencia) y debería ser chico. Si no es chico, hay
   algo que medir, no que ajustar.

## 8. Lo que este asiento NO arregla

Intereses por cobrar, provisión de intereses, ingresos del mes y aportes tienen sus propias causas
(análisis §3.6 y §4) y no entran acá. Tampoco la decisión de a qué fecha cuadra contabilidad (los
asientos ①–④ del cierre van al 01 del mes siguiente).
