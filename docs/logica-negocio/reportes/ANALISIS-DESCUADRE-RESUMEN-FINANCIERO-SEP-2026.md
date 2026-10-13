# Por qué el Resumen Financiero de CCPM (sep-2026) no cuadra con los mayores al 30-09

**Equipo:** `lap-saa-1` (árbitro) · **Fecha:** 2026-10-12 · **Pedido del usuario:** contabilidad
reporta que el Resumen Financiero de la pantalla *Informes mensuales de crédito* (`rpr`, CCPM) no
cuadra con los mayores al 30-09 ni en aportes ni en crédito, después de correr el cierre de cartera
de septiembre / apertura de octubre. *(El pedido dice «31 de septiembre»; se asume 30-09.)*

**Base usada:** la copia local de producción (`saa-oracle-23ai`, `FREEPDB1`), fresca al 2026-10-12
(`CNT.ASNT` llega hasta hoy). Sólo `SELECT`. Las consultas clave están en §9.

**Datos del período:** ejecución CCPM de septiembre = `RPR.EJCC` **467** (05-10-2026, 1.286
préstamos, 11.131 filas CPRM); corrida del cierre = `CRD.CRCT` **4** (05-10 21:03, asientos
`11053`–`11059`).

---

## 0. Resumen ejecutivo

1. **Hoy el mayor al 30-09 SÍ cuadra al centavo con el Resumen Financiero — porque el asiento manual
   `11126` lo forzó.** `11126` (JCEVALLOS, fechado 30-09, creado **07-10 15:41**, glosa *«AJUSTES
   MANUALES … Y RESUMEN FINANCIERO SEPTIEMBRE 2026»*) movió 321.716,19 / 342.494,51 entre 53 líneas:
   bandas por vencer, bandas vencidas, intereses por cobrar, provisiones de cartera, provisión de
   intereses, ingresos y **aportes**. Sin ese asiento, el mayor **no** cuadra con el informe en
   ninguna de esas familias. Es el séptimo plug mensual de la serie (feb, may, jun, jul, ago, sep).
2. **Crédito — hay tres definiciones de «banda» conviviendo, y ninguna coincide con las otras:**
   - **CCPM** reparte el capital por vencer por **número ordinal de cuota** (1ª cuota → 1-30, cuotas
     2-4 → 31-90, 5-7 → 91-180, 8-12 → 181-360, 13+ → >360).
   - **El cierre de cartera** reparte **por días al vencimiento** de cada cuota (orden de la
     Superintendencia), y lo guarda en `CRD.BDCC`.
   - **El mayor** no es ni lo uno ni lo otro: es el saldo histórico (migración + plugs manuales
     mensuales) más los *deltas* que el cierre asienta **a cartera constante** — un diseño que, por
     construcción, nunca corrige el nivel de una banda, sólo su envejecimiento.
3. **Crédito — fechas:** cinco de los siete asientos del cierre de septiembre están fechados
   **01-10** (vencido del mes, reclasificación por vencer, reclasificación vencida, apertura,
   devengo). Un mayor al 30-09 **no los contiene**. Y el asiento ② (por vencer) reclasifica hacia la
   foto del **31-10**, no la del 30-09. Las dos cosas son decisiones del usuario del 2026-08-31,
   no defectos — pero explican por qué un mayor «de septiembre» jamás va a parecerse al informe de
   septiembre.
4. **Crédito — vencido:** la pantalla clasifica **el préstamo entero** en la banda de su cuota más
   vieja; el cierre clasifica **cuota por cuota**. En hipotecarios >720 días: 950.631,96 (pantalla)
   contra 660.808,85 (cierre). Mismo capital, dos repartos.
5. **Crédito — universo:** CCPM cuenta como «capital por vencer de 1 a 30 días» **44.221,64 de 11
   préstamos precancelados** (estado 4), que ya no existen para el cierre ni para el mayor. Es un
   defecto de `GeneracionCCPMServiceImpl` (grupo 1 sin filtro de estado de préstamo + el ajuste
   «absorber la diferencia» que manda todo a `cv30` cuando no quedan cuotas futuras).
6. **Aportes — cuadra al centavo después del plug.** Antes del plug había **dos** diferencias
   reales, las dos de origen identificado:
   - **26.739,47 en cesantía personal:** tres cruces de aportes del préstamo 2967 (ORTIZ ESTRELLA,
     eventos 469/470/477) se crearon y reversaron el 07-09. `CRD.APRT` reversa con un
     **contra-movimiento fechado 07-09**; contabilidad reversa **anulando el asiento original del
     31-08**. El informe de agosto (corrido el 09-09) todavía mostraba los cruces, el mayor de agosto
     ya no — y el plug de agosto (`9533`) los «repuso» a mano. En septiembre el informe los suelta y
     el mayor no tiene nada que soltar: reaparece la diferencia, con signo contrario, y se vuelve a
     plugear. **Cada reverso de un pago con aportes va a producir esto.**
   - **1.713,45 en pensión complementaria:** seis pensiones de 9/2026 en estado **REGISTRADA (1)**
     ya tienen asiento de devengo (`PGPCNMDV`) por la pensión neta de cruce, pero `APRT` sólo recibe
     la fila al **procesarse** (`PGPCIDAP` nulo). El informe no las ve; el mayor sí.
7. **El plug mensual empeora el problema de crédito**, no lo resuelve: el cierre reclasifica contra
   **su propio snapshot** (`BDCC` del mes anterior), nunca contra el mayor. El plug es invisible
   para la siguiente corrida, así que el mayor queda como «plug + delta», que no es ni el informe
   ni el snapshot. Medido: tras el cierre, la banda 1 por vencer quedó en 349.661,26 contra un
   snapshot de 22.494,46.

---

## 1. Lo que se midió: la misma cartera, tres fotos

### 1.1 Totales — coinciden; el reparto — no

| Fuente | Por vencer | Vencido | Total |
|---|---|---|---|
| CCPM 467 (pantalla) | 14.491.034,61 | 1.934.167,51 | 16.425.202,12 |
| Mayor al 30-09 (con `11126`) | 14.491.034,61 | 1.934.167,51 | 16.425.202,12 |
| Mayor al 30-09 **sin `11126`** | 14.516.190,37 | 1.908.011,75 | 16.424.202,12 |
| Snapshot del cierre (`BDCC`, CRCT 4) | 14.426.006,30 | 1.921.282,64 | 16.347.288,94 |
| Mayor al 01-10 (tras los asientos del cierre) | 14.431.081,35 | 1.990.542,39 | — |

El snapshot del cierre suma **77.913,18 menos** que la pantalla. De eso, **44.221,64** son los 11
precancelados del §3.5 y **22.943,77** es el préstamo 70613 (cancelado el 08-10, después de ambas
corridas: diferencia de momento, no de regla). El resto (~10.700) es la fórmula de saldo: CCPM usa
`saldoInicialCapital − capital` de la cuota del mes; el cierre usa `GREATEST(DTPRCPTL − pagos
PGPR, 0)` cuota por cuota.

### 1.2 Por vencer, por número de banda (suma de todos los productos)

| Banda | Mayor 30-09 | Mayor 30-09 sin `11126` | Snapshot cierre (días) | CCPM (ordinal) |
|---|---|---|---|---|
| 1 (1-30) | **261.517,12** | 27.629,11 | 63.070,34 | **261.517,12** |
| 2 (31-90) | **654.420,87** | 864.574,68 | 433.375,53 | **654.420,87** |
| 3 (91-180) | **652.210,15** | 656.254,57 | 654.884,89 | **652.210,15** |
| 4 (181-360) | **1.053.558,61** | 1.054.465,04 | 1.277.407,22 | **1.053.558,61** |
| 5 (>360) | **11.869.327,86** | 11.913.266,97 | 11.997.268,32 | **11.869.327,86** |

Las columnas 1 y 4 son idénticas porque `11126` las igualó (banda 1: +143.796,91 en `1.3.01.05`,
+83.099,05 en `1.3.09.05`, etc.). La diferencia entre el snapshot y CCPM en las bandas 1 y 2
(~200.000 y ~220.000) es la diferencia de **definición**: con corte al 30-09, la cuota de
noviembre vence a ~61 días (banda 2 por días) pero es la 2ª cuota (banda 2 por ordinal) — coinciden;
la de **diciembre** vence a ~91 días (banda 3 por días) pero es la 3ª cuota (banda 2 por ordinal).
De ahí en adelante todo corre una banda.

### 1.3 Vencido, por cuenta

| Cuenta | Banda | Mayor 30-09 | sin `11126` | Snapshot cierre (por cuota) | CCPM (por préstamo) |
|---|---|---|---|---|---|
| 1.3.04.05 | 1-30 | 2.970,76 | −4.511,43 | 22.749,91 | 2.970,76 |
| 1.3.04.10 | 31-90 | 720,10 | −2.427,95 | 20.093,06 | 720,10 |
| 1.3.04.15 | 91-180 | 3.540,99 | 9.046,13 | 58.338,22 | 3.540,99 |
| 1.3.04.20 | 181-270 | 9.769,83 | 6.673,34 | 53.715,24 | 9.769,83 |
| 1.3.04.25 | >270 | 713.158,78 | 715.579,39 | 570.192,94 | 713.158,78 |
| 1.3.12.00 | 1-30 | 5.074,09 | −7.670,55 | 33.976,85 | 5.074,09 |
| 1.3.12.05 | 31-90 | 2.313,25 | 5.675,20 | 30.462,75 | 2.313,25 |
| 1.3.12.10 | 91-270 | 24.086,47 | 38.098,94 | 171.941,34 | 24.086,47 |
| 1.3.12.15 | 271-360 | 30.064,52 | 22.448,70 | 73.678,07 | 30.064,52 |
| 1.3.12.20 | 361-720 | 171.194,30 | 187.333,60 | 204.682,95 | 171.194,30 |
| 1.3.12.25 | >720 | 950.631,96 | 921.838,61 | 660.808,85 | 950.631,96 |

Dos cosas a la vista: (a) sin el plug, el mayor tenía **saldos negativos** en bandas de activo
(`1.3.04.05`, `1.3.04.10`, `1.3.12.00`) — consecuencia de asentar deltas sobre un nivel que nunca
se niveló; (b) la pantalla concentra en la banda abierta (>270 / >720) **todo** el vencido del
préstamo cuya cuota más vieja supera ese umbral, mientras el cierre deja cada cuota donde le toca.
Ninguna de las dos es «la correcta» por sí sola: es una decisión de negocio (ver §7).

---

## 2. El asiento `11126` y la serie de plugs

| Asiento | Fecha contable | Creado | Qué toca |
|---|---|---|---|
| 5450 | 28-02 | — | asiento inicial (migración) |
| 6775–6812 | 31-05 | — | «REGULARIZACION DE CARTERA – INFORME FCPC-CRE» (10 asientos) |
| 7245–7289 | 30-06 | — | «REGISTRO DE PRESTAMOS … DE ACUERDO AL INFORME» (por familia) |
| 7726–7901 | 31-07 | — | ídem, al 31-07 |
| 9520–9529, **9533** | 31-08 | 08-09 | «DE ACUERDO AL INFORME FCPC-CRE-GR-209-2026» — `9533` toca aportes: **D 138.065,33** `2.1.01.05.01`, H 14.349,15 `2.1.02.25.01` |
| **11126** | 30-09 | **07-10 15:41** | 53 líneas, ver abajo |

Lo que hizo `11126`, agrupado:

| Familia | Cuentas | Efecto |
|---|---|---|
| Bandas por vencer | `1.3.01/03/09/11.xx` | +235.797,19 D / −260.952,95 H — deja cada cuenta = CCPM |
| Bandas vencidas | `1.3.04.xx`, `1.3.12.xx` | ±34.000 — deja cada cuenta = CCPM por préstamo |
| Provisión de cartera | `1.3.99.05` D 23.038,46 · `1.3.99.15` H 40.102,09 (contra `4.7.04.05/15`) | deja `1.3.99.xx` = `provisionRequeridaOriginal` de CCPM |
| Intereses por cobrar | `1.4.02.05/10/15` D 26.755,07 (contra `5.1.02.xx`) | deja `1.4.02.xx` = `interesOrdinario + interesMora` de CCPM |
| Provisión de intereses | `1.4.99.05` D 764.859,26 · `4.7.05.10` H 809.837,62 («REVERSAR LO ANTERIOR Y DEJAR LO CORRECTO») | deja `1.4.99.05` = 2.165.925,14 = total de intereses de CCPM, encima del paso 7 del cierre (`11059`, 2.121.036,78) |
| Aportes | `2.1.01.05.01` H 26.739,47 · `2.1.02.25.01` H 1.713,45 | deja ambas = CPRM |
| Transitoria y apertura | `2.3.01.15.01` D 118.423,82 · `1.4.05.05/2.3.02.05` 825,88 · `1.4.05.10/2.3.02.10` 355,53 | — |

**Conclusión:** la pregunta «¿por qué no cuadra?» es, en los datos, «¿por qué el sistema solo no
llega a lo que el plug tuvo que poner?». Lo que sigue responde eso familia por familia.

---

## 3. Crédito — las causas

### 3.1 CCPM reparte por cuota ordinal, el cierre por días

`GeneracionCCPMServiceImpl:450-495`:

```java
Double cv30  = sumarRango(capitales, 0,  0);    // 1ª cuota futura
Double cv90  = sumarRango(capitales, 1,  3);    // cuotas 2-4
Double cv180 = sumarRango(capitales, 4,  6);    // 5-7
Double cv360 = sumarRango(capitales, 7,  11);   // 8-12
Double cvMas = sumarRango(capitales, 12, …);    // 13+
```

`capitales` es la lista de capitales de las cuotas futuras **en orden de número de cuota**, sin
mirar su fecha. Para el grupo 2 (préstamos con atraso) la lista arranca en la cuota **del mes**.

`CierreCarteraServiceImpl.distribuye:523` → `tipoCarteraYDias` (`vencimiento − corte` días) →
`clasificarEnBandas` contra `CRD.BNDP` (períodos de 30 días: 1/2/3/6/abierta). Es la regla de la
Superintendencia (decisión del usuario del 2026-08-25, registrada en memoria del proyecto) y
**no se cambia**.

Para doce cuotas mensuales, por días caen 1-1-3-6-1 y por ordinal 1-3-3-5-resto. Las dos sumas
dan lo mismo; el reparto, no. La réplica en SQL del algoritmo del cierre reproduce `BDCC` cuenta
por cuenta (diferencias de centavos a 19.059,03, todas por pagos entre el 05-10 y hoy) — §9.4.

### 3.2 El mayor es «nivel histórico + deltas»: el cierre nunca lo nivela

El diseño del cierre (`LEVANTAMIENTO-ALIMENTACION-CONTABLE-CREDITOS.md`, `API-CIERRE-CARTERA.md`
§1.3) es deliberado: *«el saldo previo por banda sale de un SNAPSHOT por corrida, NO de la
mayorización contable»* y *«la reclasificación se calcula a cartera constante»*. Cuadra por
construcción, y evita contar dos veces pagos y entregas. El costo: **si el nivel de una banda en el
mayor está mal, el cierre lo arrastra para siempre** — sólo asienta cómo envejeció la cartera.

Evidencia: agosto. Snapshot `BDCC` de CRCT 2 contra mayor al 01-09 (ya con el cierre de agosto):
`1.3.01.10` 110.714,71 vs 444.710,61; `1.3.04.25` 182.764,36 vs 721.136,59; `1.3.12.25`
129.967,58 vs 921.848,58. El propio `cargaDesviaciones` las reporta como advertencia y dice
textualmente: *«no lo corrige este proceso»*.

Y el plug lo agrava: la corrida de septiembre partió de `BDCC` de agosto, no del mayor plugeado al
31-08, y la de octubre partirá de `BDCC` de septiembre, no del mayor plugeado al 30-09. El mayor
queda como `plug + delta`: tras `11054`, `1.3.01.05` quedó en 349.661,26 (snapshot 22.494,46;
CCPM 154.277,87).

### 3.3 Fechas de los asientos del cierre

`CierreCarteraServiceImpl.calcula:436-443`: fecha del asiento = **primer día del mes que se abre**
para ① vencido del mes, ② reclasificación por vencer, ①.1 reclasificación vencida, ③ apertura y ④
devengo; sólo ⑥ neteo y ⑦ provisión van al último día del mes cerrado. Regla confirmada por el
usuario el 2026-08-31. Medido en `CNT.ASNT`: `11053`–`11057` el **01-10** (período 10), `11058`
y `11059` el 30-09.

Consecuencia: un mayor al 30-09 contiene la cartera **envejecida al 31-08** (por el cierre de
agosto) más los movimientos de septiembre, pero **no** el envejecimiento de septiembre. Además, ②
reclasifica hacia `distApertura` = foto al **31-10** (`calcula:400-414`, «valores del mes que se
abre»), así que el mayor del 01-10 muestra la banda 1 con las cuotas de **noviembre** adentro
(réplica al 31-10: banda 1 = 217.004,90 en emergente/quirografario, que es exactamente el débito
de `11054` en `1.3.01.05`: 217.800,12).

Y el devengo de intereses de **septiembre** (`11057`, 91.635,89) también está fechado 01-10: el
ingreso del mes no aparece en el P&L al 30-09.

### 3.4 Vencido: por préstamo (pantalla) vs por cuota (cierre)

`informes-mensuales-credito.component.ts` `vencidoRango(rows, min, max)`: toma `diasMorosidad` del
préstamo (días de su cuota más antigua impaga, `GeneracionCCPMServiceImpl:290`) y suma **todo**
`valorVencido` del préstamo en esa banda. El cierre, cuota por cuota (§3.1). Tabla en §1.3.

Umbrales distintos por familia en la pantalla (quiro 270, hipo 270/360/720) coinciden con `BNDP`;
lo que no coincide es la unidad (préstamo vs cuota).

### 3.5 Universo: 11 préstamos precancelados en «1 a 30 días»

`selectCuotasDelMesGlobal` (`DetallePrestamoDaoServiceImpl:193`) arma el grupo 1 con `d.estado = 4`
y la fecha de vencimiento dentro del mes — **sin filtro de estado del préstamo**. Un préstamo que
pagó su cuota de septiembre y **después se precanceló** entra igual. Para ese préstamo:
`valorPorVencer = saldoInicialCapital − capital` de la cuota pagada (`:300-307`); el desglose
excluye las cuotas en estado 7 → `capitales` vacío → `cv30..cvMas = 0`; el «control de
consistencia» (`:470-486`) absorbe la diferencia en la última banda con saldo, y como no hay
ninguna, **en `cv30`**.

Medido en EJCC 467: 11 préstamos `PRSTIDST = 4` con `por_vencer` 44.221,64, **todo en `cv30`**
(71035: 11.574,95; 70539: 5.640,13; 71152: 5.220,61; …), con cero cuotas pendientes. Más 15 con
`PRSTIDST = 3` en cero (inofensivos; uno, 67749, con vencido 151,63).

### 3.6 Intereses, provisiones, ingresos

| Concepto | Pantalla (CCPM) | Mayor sin `11126` | Plug | Nota |
|---|---|---|---|---|
| Intereses por cobrar `1.4.02.05/10/15` | 597.537,04 / 14.418,77 / 1.553.969,33 | 594.447,41 / 14.263,95 / 1.530.458,71 | +3.089,63 / +154,82 / +23.510,62 | CCPM = `interesOrdinario + interesMora` acumulados de la tabla |
| Provisión de cartera `1.3.99.05/15` | 496.247,78 / 2.887.111,55 | 519.286,24 / 2.847.009,46 | −23.038,46 / +40.102,09 | CCPM = `provisionRequeridaOriginal` (calificación propia × sujeto a provisión); el mayor viene de plugs anteriores |
| Provisión de intereses `1.4.99.05` | 2.165.925,14 (= total intereses) | 2.930.784,40 (incluye `11059` 2.121.036,78) | −764.859,26 | El paso 7 del cierre (`omen1`, 2026-10-05) y el plug se pisan: el plug «reversa lo anterior y deja lo correcto» |
| Ingresos del mes `5.1.02.xx` | 124.459,42 (`interesOrdinarioDelMes + interesMoraDelMes`) | devengo `11057` = 91.635,89, fechado **01-10** | +23.144,56 al 30-09 | El devengo excluye el interés condonado de plazo vencido (D27) y no incluye mora; además está en octubre |

### 3.7 Lo que NO es defecto

- Bandas por días en el cierre: orden del regulador.
- Asientos ①–④ al 01-10 y ② hacia el 31-10: decisión del usuario del 2026-08-31.
- Que el snapshot no mire el mayor: decisión de diseño para no contar dos veces.

Lo que sí es defecto: §3.5 (precancelados), y la tercera definición (la pantalla) que no existe en
ninguna regla escrita.

---

## 4. Aportes — las causas

`CPRM.total` = `SUM(APRT.valor)` por entidad y tipo con `fechaTransaccion ≤ corte`
(`AporteDaoServiceImpl:905`). El mayor de cada tipo es la cuenta de `CRD.CTAP`. Con `11126` los
11 tipos cuadran a 0,00; **sin** `11126`, septiembre tenía estas diferencias, día por día:

### 4.1 Cesantía personal (`2.1.01.05.01`): 26.739,47

| Día | `APRT` | Mayor | Diferencia | Qué es |
|---|---|---|---|---|
| 01-09 | −8.346,98 | +78,64 | −8.425,62 | Evento 350 (acuerdo de condonación 1, ECOSSIO) — sin asiento |
| 07-09 | +32.362,36 | −2.802,73 | **+35.165,09** | Reversos de los eventos 469, 470, 477 (+26.739,47) y del 350 (+8.425,62) — sin asiento |
| 14-09 / 24-09 | +130 / −130 | — | 0 | registro y reverso de un aporte manual |
| 30-09 | 61.694,41 | 62.142,98 | −448,57 | §4.3 |

Los eventos 469/470/477 son tres `PAGO_APORTES` del préstamo 2967 (ORTIZ ESTRELLA), **creados el
07-09 con fecha 31-08 y anulados dos minutos después** («para regularizar», «por error en cruce»,
«por cuadre»); el 478 (8.449,27) quedó vigente. Sus asientos `9210/9211/9225` (31-08) están en
estado 2 — `contabilizarReverso` (`ContabilidadPrestamoServiceImpl:909-940`) los anuló. Pero
`revertirAportes` (`ProcesoPagoPrestamoServiceImpl:1596-1640`) escribe en `APRT` un
**contra-movimiento con `fechaTransaccion = ahora`** (07-09), porque `APRT` es *append-only*.

Dos semánticas de reverso:

| | Al 31-08 | Al 30-09 |
|---|---|---|
| `APRT` (CPRM) | −26.739,47 (originales) | 0 (originales + contra-movimientos) |
| Mayor | 0 (asientos anulados) | 0 |

El informe de agosto se corrió el 09-09 y **mostró** los cruces; el mayor de agosto ya no los
tenía; `9533` (08-09) debitó 138.065,33 para igualarlo al informe. En septiembre el informe los
suelta, el mayor no tiene nada que soltar: −26.739,47 respecto del informe, y `11126` lo acredita.
**Se va a repetir con cada reverso de un pago con aportes** mientras una fuente reverse hacia atrás
y la otra hacia adelante.

### 4.2 Pensión complementaria (`2.1.02.25.01`): 1.713,45

El 23-09 el mayor tiene 201 líneas de «Pensión complementaria 9/2026» (−130.242,24) y `APRT` 195
filas tipo movimiento 9 (−128.528,79). Las seis de más, una a una (`PGPCNMDV` = asiento):

| PGPC | Pensión | Cruce (`APRT` tpmv 4) | Línea del mayor | `APRT` pensión | Estado |
|---|---|---|---|---|---|
| 793 | 587,24 | −219,35 | −367,89 | — | 1 REGISTRADA |
| 811 | 381,21 (+56,30 seguro) | — | −437,51 | −56,30 | 1 |
| 840 | 73,52 | −28,67 | −44,85 | — | 1 |
| 919 | 452,65 | −325,80 | −126,85 | — | 1 |
| 924 | 10,07 | −3,89 | −6,18 | — | 1 |
| 941 | 786,47 (+56,30) | — | −842,77 | −56,30 | 1 |

`generarPensionIndividual` emite el asiento de devengo al registrar
(`PagoPensionComplementariaServiceImpl:2202`, por la pensión neta de cruce) y deja el pago en
`REGISTRADA`; la fila `APRT` de la pensión se escribe al procesarla (`PGPCIDAP` nulo en las seis).
Mientras una pensión esté registrada y no procesada, el mayor la tiene y el informe no.

### 4.3 Los 448,57 que nadie plugeó

Cobros 178 y 180 (asientos `11036/11038`, fechados 30-09, creados 05-10 19:38) tienen su `APRT`
con `fechaTransaccion` **31-08** (130,00) y **07-06** (318,57), registradas el 05-10. Al 30-09
ambos lados los contienen (no hay diferencia), pero un informe de agosto re-corrido hoy daría
448,57 más que el de septiembre pasado. Es la misma clase de problema que §4.1: `APRT` fecha por
período de aporte; el asiento, por fecha de registro.

---

## 5. Cómo se encadena todo (lectura corta)

1. El mayor arranca de una migración y de plugs mensuales que lo igualan **al informe** (ordinal
   por cuota, vencido por préstamo).
2. El cierre asienta deltas por días, cuota por cuota, contra su propio snapshot — y los fecha al
   01 del mes siguiente.
3. El informe del mes siguiente vuelve a calcular con su regla propia.
4. Contabilidad compara (1+2) contra (3) y plugea la diferencia, que la siguiente corrida ignora.

No hay un «error de cálculo» en el sentido de una fórmula mal escrita (salvo §3.5): hay **tres
reglas distintas para la misma pregunta**, más dos semánticas de reverso y de fecha en aportes.

---

## 6. Lo que toca territorio de otros equipos

- `GeneracionCCPMServiceImpl` y la pantalla `informes-mensuales-credito` son de `rpr` (commits
  recientes de `omen1`); `CierreCarteraServiceImpl`, `ProcesoPagoPrestamoServiceImpl`,
  `PagoPensionComplementariaServiceImpl`, `ContabilidadPrestamoServiceImpl` son de `crd`
  (`omen-saa-1`, archivos con dueño exclusivo en el registro de reservas §4).
- Cualquier corrección de §3.5, §4.1 o §4.2 la implementa `omen-saa-1`, o se renegocia. **Este
  equipo no toca ese código sin autorización del usuario y aviso al otro árbitro.**

---

## 7. Decisiones que no son mías (para el usuario)

1. **Una sola regla de bandas.** ¿La pantalla adopta la regla del cierre (por días, por cuota),
   o se mantiene la del informe y el cierre cambia? Lo primero es consistente con la orden de la
   Superintendencia y con el mayor; lo segundo cambia lo que el regulador ya aceptó en el cierre.
   Hasta que haya una sola, contabilidad no tiene con qué cuadrar.
2. **Nivelación.** ¿Se agrega al cierre un paso que lleve cada cuenta de banda **al saldo absoluto**
   del snapshot (y no sólo el delta), por única vez o cada mes? Es lo único que absorbe la historia
   de plugs. Requiere diseño: hay que decidir qué pasa con el capital que entró por entregas y
   pagos del mes entre los dos snapshots.
3. **Qué mayor compara contabilidad.** Si los asientos del cierre siguen al 01-10, el cuadre de
   «septiembre» se hace contra el mayor **al 01-10** (o se cambia la fecha, decisión del 31-08).
4. **Plugs manuales.** Mientras el cierre no lea el mayor, cada plug es deuda para la corrida
   siguiente. Si se van a seguir haciendo, que sea después del cierre y con constancia escrita.
5. **CCPM — precancelados (§3.5).** Filtrar el grupo 1 por `prestamo.idEstado in (2, 8, 11)` como
   ya hace el grupo 2, y no absorber en `cv30` cuando no hay cuotas futuras. Corrección chica, de
   `rpr`.
6. **Reverso de aportes (§4.1).** O el contra-movimiento de `APRT` lleva la fecha del original, o
   contabilidad reversa con contra-asiento fechado hoy en vez de anular. Las dos tienen efectos
   colaterales (informes históricos de aportes vs. auditoría contable) y hay que elegir una.
7. **Pensión registrada (§4.2).** O el `APRT` se escribe al registrar, junto con el asiento, o el
   asiento se emite al procesar. Hoy están desfasados.

---

## 8. Lo que queda abierto

- No se verificó qué cifras exactas comparó contabilidad (¿antes o después de `11126`?). Si fue
  antes, este documento explica cada diferencia; si fue después, lo que no cuadra es el mayor al
  01-10 contra el informe, y la explicación es §3.3.
- No se midió el efecto de `11126` sobre la transitoria `2.3.01.15.01` (118.423,82) ni sobre
  `1.4.05/2.3.02`: no entran en el resumen financiero.

---

## 9. Consultas (todas `SELECT`, conectado como `sys` a `FREEPDB1`)

### 9.1 Mayor por cuenta de banda con y sin `11126`, contra snapshot y CCPM
```sql
WITH b AS (SELECT PLNNCDGO, BDCCTPCR tpcr, MIN(BDCCNMRO) banda, SUM(BDCCCPTL) snap
           FROM CRD.BDCC WHERE CRCTCDGO=4 GROUP BY PLNNCDGO, BDCCTPCR),
m AS (SELECT d.PLNNCDGO,
        SUM(CASE WHEN a.ASNTFCHA <= DATE '2026-09-30' THEN NVL(d.DTASDBEE,0)-NVL(d.DTASHBRR,0) ELSE 0 END) s3009,
        SUM(CASE WHEN a.ASNTCDGO = 11126 THEN NVL(d.DTASDBEE,0)-NVL(d.DTASHBRR,0) ELSE 0 END) ef11126,
        SUM(CASE WHEN a.ASNTFCHA <= DATE '2026-10-01' THEN NVL(d.DTASDBEE,0)-NVL(d.DTASHBRR,0) ELSE 0 END) s0110
      FROM CNT.DTAS d JOIN CNT.ASNT a ON a.ASNTCDGO=d.ASNTCDGO
      WHERE a.ASNTESTD=1 AND a.PJRQCDGO=1236 GROUP BY d.PLNNCDGO)
SELECT b.tpcr, b.banda, p.PLNNCNTA, m.s3009, m.ef11126, m.s3009-m.ef11126 sin11126, b.snap, m.s0110
FROM b JOIN CNT.PLNN p ON p.PLNNCDGO=b.PLNNCDGO LEFT JOIN m ON m.PLNNCDGO=b.PLNNCDGO
ORDER BY b.tpcr, p.PLNNCNTA;
```
CCPM por banda: `SUM(CCPMCV30|CV90|CV180|CV360|CVMAS) FROM RPR.CCPM WHERE CCPMEJCC=467`;
vencido por préstamo: `SUM(CCPMVLVN)` agrupando `CCPMDDMR` en los tramos de la pantalla.

### 9.2 Detalle de `11126`
```sql
SELECT p.PLNNCNTA, d.DTASDBEE, d.DTASHBRR, d.DTASDSCR
FROM CNT.DTAS d JOIN CNT.PLNN p ON p.PLNNCDGO=d.PLNNCDGO WHERE d.ASNTCDGO=11126 ORDER BY 1;
```

### 9.3 Asientos manuales sobre cartera y aportes
```sql
SELECT a.ASNTCDGO, a.ASNTFCHA, a.ASNTFCPR, a.ASNTESTD, a.PLNTCDGO, a.ASNTOBSR, SUM(d.DTASDBEE), SUM(d.DTASHBRR)
FROM CNT.ASNT a JOIN CNT.DTAS d ON d.ASNTCDGO=a.ASNTCDGO JOIN CNT.PLNN p ON p.PLNNCDGO=d.PLNNCDGO
WHERE a.PJRQCDGO=1236 AND a.ASNTFCHA >= DATE '2026-01-01'
  AND (p.PLNNCNTA LIKE '1.3.%' OR p.PLNNCNTA LIKE '2.1.01.%' OR p.PLNNCNTA LIKE '2.1.02.%')
  AND a.PLNTCDGO IN (1097,1117,1118)
GROUP BY a.ASNTCDGO, a.ASNTFCHA, a.ASNTFCPR, a.ASNTESTD, a.PLNTCDGO, a.ASNTOBSR ORDER BY a.ASNTFCHA;
```

### 9.4 Réplica del reparto por días del cierre (valida `BDCC` y da la foto al 31-10)
```sql
WITH cuotas AS (
  SELECT p.PRDCCDGO prd, TRUNC(d.DTPRFCVN) fv, GREATEST(NVL(d.DTPRCPTL,0)-NVL(g.cap,0),0) cap
  FROM CRD.DTPR d JOIN CRD.PRST p ON p.PRSTCDGO=d.PRSTCDGO
  LEFT JOIN (SELECT DTPRCDGO, SUM(NVL(PGPRCPPG,0)) cap FROM CRD.PGPR
             WHERE PGPRANUL IS NULL OR PGPRANUL=0 GROUP BY DTPRCDGO) g ON g.DTPRCDGO=d.DTPRCDGO
  WHERE p.PRSTIDST IN (2,8,11) AND (d.DTPRESTD IS NULL OR d.DTPRESTD NOT IN (4,7))
    AND d.DTPRFCVN IS NOT NULL AND p.PRDCCDGO IS NOT NULL),
agr AS (SELECT prd, fv, SUM(cap) cap FROM cuotas GROUP BY prd, fv HAVING SUM(cap) > 0),
bandas AS (
  SELECT c.PRDCCDGO prd, c.CBPRTPCR tpcr, b.BNDPNMRO nro, b.PLNNCDGO,
         NVL(SUM(b.BNDPCNTD) OVER (PARTITION BY c.CBPRCDGO ORDER BY b.BNDPNMRO
             ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING),0)*30+1 d_ini,
         CASE WHEN b.BNDPCNTD IS NULL THEN NULL ELSE SUM(b.BNDPCNTD) OVER (PARTITION BY c.CBPRCDGO
             ORDER BY b.BNDPNMRO ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW)*30 END d_fin
  FROM CRD.CBPR c JOIN CRD.BNDP b ON b.CBPRCDGO=c.CBPRCDGO
  WHERE c.CBPRESTD=1 AND b.BNDPESTD=1 AND c.PJRQCDGO=1236),
clas AS (
  SELECT a.prd, a.cap, corte.etq,
         CASE WHEN a.fv < corte.f THEN 2 ELSE 1 END tpcr,
         CASE WHEN a.fv < corte.f THEN GREATEST(1, corte.f - a.fv) ELSE GREATEST(1, a.fv - corte.f) END dias
  FROM agr a CROSS JOIN (SELECT DATE '2026-09-30' f, 's3009' etq FROM dual
                         UNION ALL SELECT DATE '2026-10-31', 's3110' FROM dual) corte)
SELECT cl.etq, bd.tpcr, bd.nro, p.PLNNCNTA, SUM(cl.cap)
FROM clas cl JOIN bandas bd ON bd.prd=cl.prd AND bd.tpcr=cl.tpcr
     AND cl.dias >= bd.d_ini AND (bd.d_fin IS NULL OR cl.dias <= bd.d_fin)
JOIN CNT.PLNN p ON p.PLNNCDGO=bd.PLNNCDGO
GROUP BY cl.etq, bd.tpcr, bd.nro, p.PLNNCNTA ORDER BY 1,2,4;
```

### 9.5 Precancelados dentro de CCPM
```sql
SELECT c.CCPMNMOP, p.PRSTIDST, c.CCPMTPCR, c.CCPMVPVN, c.CCPMCV30
FROM RPR.CCPM c JOIN CRD.PRST p ON TO_CHAR(p.PRSTIDAS)=c.CCPMNMOP
WHERE c.CCPMEJCC=467 AND p.PRSTIDST IN (3,4) ORDER BY c.CCPMVPVN DESC;
```

### 9.6 Aportes día por día, `APRT` contra mayor (tipo 11; cambiar tipo y cuenta para 23)
```sql
WITH ap AS (SELECT TRUNC(APRTFCTR) d, SUM(APRTVLRR) aprt FROM CRD.APRT
            WHERE TPAPCDGO=11 AND APRTFCTR >= DATE '2026-09-01' AND APRTFCTR < DATE '2026-10-01' GROUP BY TRUNC(APRTFCTR)),
my AS (SELECT TRUNC(a.ASNTFCHA) d, SUM(NVL(x.DTASHBRR,0)-NVL(x.DTASDBEE,0)) mayor
       FROM CNT.ASNT a JOIN CNT.DTAS x ON x.ASNTCDGO=a.ASNTCDGO JOIN CNT.PLNN p ON p.PLNNCDGO=x.PLNNCDGO
       WHERE a.PJRQCDGO=1236 AND a.ASNTESTD=1 AND a.ASNTCDGO<>11126 AND p.PLNNCNTA='2.1.01.05.01'
         AND a.ASNTFCHA BETWEEN DATE '2026-09-01' AND DATE '2026-09-30' GROUP BY TRUNC(a.ASNTFCHA))
SELECT NVL(ap.d,my.d) dia, ap.aprt, my.mayor, NVL(ap.aprt,0)-NVL(my.mayor,0) dif
FROM ap FULL OUTER JOIN my ON my.d=ap.d ORDER BY 1;
```

### 9.7 Las seis pensiones registradas sin `APRT`
```sql
SELECT g.PGPCCDGO, g.ENTDCDGO, g.PGPCVLPN, g.PGPCVLSG, g.PGPCESTD, g.PGPCIDAP, g.PGPCNMDV,
  (SELECT SUM(APRTVLRR) FROM CRD.APRT a WHERE a.ENTDCDGO=g.ENTDCDGO AND a.TPAPCDGO=23
     AND a.APRTTPMV=9 AND TRUNC(a.APRTFCTR)=DATE '2026-09-23') aprt_pension
FROM CRD.PGPC g WHERE g.PGPCNMDV IN (10299,10127,10112,10277,10164,10282);
```

### 9.8 Eventos reversados del préstamo 2967
```sql
SELECT EVPRCDGO, EVPRTPOO, EVPRVLRR, EVPRFCHA, EVPRFCRG, EVPRESTD, EVPRNMAS, EVPRFCAN
FROM CRD.EVPR WHERE PRSTCDGO=2967 ORDER BY 1;
SELECT ASNTCDGO, ASNTFCHA, ASNTESTD, ASNTFCPR, ASNTFCAN FROM CNT.ASNT WHERE ASNTCDGO IN (9210,9211,9225,9226);
```

> Trampa encontrada al consultar: `ASNTOBSR` lleva acentos («Pensión», «Cobro crédito») que la
> consola muestra sin ellos; `LIKE 'Pension%'` no casa. Usar `REGEXP_LIKE(col, '^Pensi.n')`.
