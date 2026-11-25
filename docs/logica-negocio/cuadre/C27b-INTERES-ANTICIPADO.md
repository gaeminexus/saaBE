# C27b — Conciliación de 1.4.02 e interés cobrado antes del devengo

Fecha: 2026-11-11 · Base `saa-oracle-23ai` local (FREEPDB1), **solo SELECT** (octubre 2025 cerrándose en paralelo por
otro agente). Jun–sep 2025: meses cerrados definitivos. **Oct 2025: preliminar** (cartera corrida 106 ejecutada; sus
cobros y acuerdos todavía pueden moverse). Sección madre: `C27-INFORMES-MENSUALES-AL-CORTE.md` §C27b.

> **Propuesta, no aplicada.** Nada de esto se registró en el SAA. Las decisiones del §7.5 son del líder (y de ASOPREP
> las que tocan saldos de meses cerrados); se anotan en `01-seguimiento/DECISIONES-Y-PARTIDAS.md` antes de aplicar.

---

## 1. Resultado en una línea

**1.4.02 contable = interés vencido (G48) + mora contabilizada pendiente + arranque − interés anticipado − primera
cuota sin devengo + residuo**, cerrado **al centavo** en cada mes y cada cuenta (Q 1.4.02.05, P 1.4.02.10, H 1.4.02.15).
El arranque es constante, el anticipado crece, la primera cuota es una sola cohorte (jun-2025) y el residuo que queda
son **9 cuotas identificadas** (Q 24,36 → 43,57; H 156,89 → 241,64; P 0).

## 2. Método (reproducible)

`sql/plantilla-conciliacion-1402.sql` (copias `sql/conciliacion-1402-AAAA-MM.sql`, salidas
`resultados/conciliacion-1402-AAAA-MM-0N.csv`), por **cuota**:

- **libro** = interés vencido al ancla (31-may-2025, cohorte A) + devengo ④ emulado − interés cobrado desde el ancla
  (`PGPRINPG + PGPRINVP`, pagos vigentes) − interés condonado (`MVIC` tipo 9, componente 1).
- **explicado** = interés vencido al corte (cálculo de la G48: el mismo que `plantilla-intereses-cohortes.sql`, que da
  la línea de la G48 al centavo, §3.3 de C27) − anticipado (cobrado antes del día 1 del mes de vencimiento de la cuota).
- **residuo** por cuota = libro − explicado; clasificado en categorías (bloque 02) y listado por operación (bloque 03,
  operación = `PRSTIDAS`; sin nombres). Bloque 04: el anticipado por cuota.

Controles que cierran en todos los meses:

1. **Devengo emulado = asiento ④** ("CRD devengado de interés a ingresos", fechado el día 1 del mes de las cuotas):
   diferencia 0,00 en las 15 combinaciones mes × cuenta, salvo H desde septiembre (+84,75, ver §4, abono a capital).
2. **Movimientos de 1.4.02 = apertura + ④ + mora − cobros − condonado**: diferencia 0,00 salvo Q sep (−0,50) y Q oct
   (−70,89), que son los asientos de condonación de acuerdos (valor del asiento ≠ libro MVIC).
3. Mora contabilizada pendiente (MVIC componente mora: tipo 5 − 6 − 9 − `PGPRMRPG` desde el ancla, **por
   `PRSTCDGO`**, igual que la rama): cierra con la contabilidad al centavo. (La tabla C27b.1 de C27, por `DTPRCDGO`,
   da H 6.373,37 en junio; la que cierra es 6.373,25 — diferencia de 0,12 en la forma de agrupar, no en el asiento.)
4. `sql/linea-conciliada-c27b.sql`: las tres consultas de la rama (mora, anticipado, primera cuota) dan las mismas
   cifras (`resultados/linea-conciliada-c27b-01.csv`).

## 3. Cifras por mes

Valores al corte, acumulados desde el ancla. Anticipado = cobrado antes del mes de vencimiento de su cuota; "por
vencer" = de cuotas cuyo mes todavía no abrió al corte (hoy ⇒ crédito dentro de 1.4.02); "sin ingreso" = de cuotas cuyo
mes ya abrió (ese interés **nunca pasó a 5.1.02**). Comprobación: saldo = vencido + mora + arranque − anticipado −
primera cuota + residuo → **0,00 en todas las filas**.

| Mes | Cta | Saldo 1.4.02 | Interés vencido | Mora pend. | Arranque | Anticipado | – por vencer | – sin ingreso | Primera cuota | Residuo |
|---|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| 2025-06 | Q | 99.933,73 | 100.012,01 | 3.893,89 | −998,76 | 1.546,29 | 1.546,29 | 0,00 | 1.451,48 | 24,36 |
| | P | 3.192,88 | 3.038,04 | 154,84 | 0,00 | 0,00 | 0,00 | 0,00 | 0,00 | 0,00 |
| | H | 892.627,78 | 895.028,93 | 6.373,25 | −7.479,09 | 1.452,20 | 1.452,20 | 0,00 | 0,00 | 156,89 |
| 2025-07 | Q | 109.823,44 | 108.019,06 | 8.015,46 | −998,76 | 3.785,20 | 2.494,39 | 1.290,81 | 1.451,48 | 24,36 |
| | P | 3.332,43 | 3.038,04 | 314,77 | 0,00 | 20,38 | 20,38 | 0,00 | 0,00 | 0,00 |
| | H | 915.068,10 | 913.963,37 | 13.127,97 | −7.479,09 | 4.701,04 | 3.407,22 | 1.293,82 | 0,00 | 156,89 |
| 2025-08 | Q | 124.391,85 | 121.213,21 | 12.287,52 | −998,76 | 6.683,00 | 3.789,34 | 2.893,66 | 1.451,48 | 24,36 |
| | P | 3.448,44 | 3.038,04 | 474,77 | 0,00 | 64,37 | 43,99 | 20,38 | 0,00 | 0,00 |
| | H | 944.714,27 | 939.235,69 | 20.007,47 | −7.479,09 | 7.206,69 | 3.540,69 | 3.666,00 | 0,00 | 156,89 |
| 2025-09 | Q | 144.582,56 | 139.063,54 | 16.701,46 | −998,76 | 8.775,77 | 3.863,41 | 4.912,36 | 1.451,48 | 43,57 |
| | P | 3.603,24 | 3.038,04 | 629,57 | 0,00 | 64,37 | 26,45 | 37,92 | 0,00 | 0,00 |
| | H | 977.242,41 | 966.178,48 | 26.974,55 | −7.479,09 | 8.673,17 | 2.880,01 | 5.793,16 | 0,00 | 241,64 |
| 2025-10 (prelim.) | Q | 165.805,12 | 157.067,94 | 21.355,00 | −998,76 | 10.194,33 | 3.587,45 | 6.606,88 | 1.451,48 | 26,75 |
| | P | 3.763,23 | 3.038,04 | 789,56 | 0,00 | 64,37 | 11,78 | 52,59 | 0,00 | 0,00 |
| | H | 1.015.914,23 | 999.096,93 | 34.440,98 | −7.479,09 | 10.386,23 | 2.630,01 | 7.756,22 | 0,00 | 241,64 |

- **Arranque** (31-may-2025) = apertura 1.4.02 (B17 de Delta) − interés vencido de la cartera al ancla: Q 95.624,95 −
  96.623,71 = **−998,76**; P 3.066,84 − 3.066,84 = **0,00**; H 878.464,41 − 885.943,50 = **−7.479,09**
  (`resultados/intereses-cohortes-2025-05-01.csv`). Constante en todos los meses: se confirma como arranque puro.
- **Anticipado por origen** (cobros jun–oct): ~91 % **Petro/Arch** (carga del último día del mes aplicada a la cuota del
  mes siguiente), el resto pagos manuales. Por mes de pago: Q 1.546,29 / 2.238,91 / 2.897,80 / 2.092,77 / 1.418,56;
  H 1.452,20 / 3.248,84 / 2.505,65 / 1.466,48 / 1.713,06; P 0 / 20,38 / 43,99 / 0 / 0. Incluye cuotas muy futuras
  (p. ej. op. 67095 pagó en jul-2025 cuotas de nov y dic 2026).
- **Primera cuota sin devengo**: los 29 préstamos quirografarios concedidos del 2 al 11-jun-2025 tienen su primera
  cuota el 30-jun-2025. El ④ de esas cuotas es el de la corrida de arranque (cierre de mayo, universo al 31-may): no los
  ve; ningún otro paso los devenga. 1.134,46 se cobraron (acreditando 1.4.02) y 317,02 quedaron vencidos (en la G48,
  no en la cuenta); al corte de agosto el impago es 69,77. **En la base todos los préstamos nuevos tienen la primera
  cuota en el mismo mes de su concesión (73 de 73 en mayo, 29 de 29 en junio)**: el defecto es sistemático y volverá a
  aparecer con cada préstamo nuevo que se cargue (la base no tiene todavía préstamos concedidos después de jun-2025).

## 4. Residuo (lo no explicado), cuota por cuota

| Cta | Operación | Desde | Valor | Causa |
|---|---|---|---:|---|
| Q | 71111 | jun | 12,96 | Cuota vto 31-may (pendiente al ancla) marcada pagada el 30-jun con 3,04 de interés en `PGPR` de 16,00 |
| Q | 67857 | jun | 11,40 | Cuota vto 31-may marcada pagada el 30-jun sin fila de interés en `PGPR` |
| Q | 70882 | sep | 19,71 | Acuerdo de condonación 26-sep: cuota vto 30-sep devengada y cancelada anticipada sin cobro ni reverso |
| Q | (asientos de condonación) | sep | −0,50 | Asiento de condonación ≠ libro MVIC (interés + mora) |
| Q | 68781, 70474, 69818, 71024, 70752 | oct (prelim.) | −16,82 | Acuerdos del 31-oct: neto por operación 0,00 / 0,00 / 14,02 / 14,11 / 25,44 y −70,39 de asiento ≠ MVIC |
| H | 68489 | jun | 37,41 | Precancelada el 3-jun: la cuota vto 30-jun se devengó el 1-jun y no se cobró ni se reversó |
| H | 69891 | jun | 98,45 | Cuota vto 31-may marcada pagada el 30-jun sin fila de interés en `PGPR` |
| H | 65906 | jun | 21,03 | Cuota vto 2018 (pendiente al ancla, 145,00) marcada pagada el 30-jun con 123,97 de interés |
| H | 66940 | sep | 84,75 | Abono a capital del 29-sep (11.300,00): la tabla re-amortizada bajó el interés de la cuota vto 30-sep de 324,12 a 239,37 (= 11.300,00 × 0,75 %) **después** de su devengo del 1-sep; la diferencia quedó en 1.4.02 |

Q: 24,36 (jun–ago) → 43,57 (sep) → 26,75 (oct). H: 156,89 (jun–ago) → 241,64 (sep, oct). P: 0.
Las de cohorte A (71111, 67857, 69891, 65906) son cuotas que la migración marcó pagadas sin el cobro completo
(ver CLAUDE.md: los cobros del SAA no son fuente de verdad); van a la revisión de cobros del frente A.1.

## 5. Dónde está el defecto en el código (saaBE `main` = rama c27b, sin cambios de motor)

1. **④ resta lo cobrado.** `CierreCarteraDaoServiceImpl.selectInteresPorTipoPrestamoEnRango`:
   `GREATEST(NVL(d.DTPRINTR,0) - NVL(g.intr,0), 0)` con `pagos(corte)` = todo pago vigente anterior al día 1 del mes
   de la cuota. Correcto solo si el cobro anticipado hubiera ido a ingreso o a un diferido.
2. **Todo cobro de interés acredita 1.4.02.** Petro/Arch (`CobroPetroContableServiceImpl`, línea
   `INTERES_ORDINARIO_POR_COBRAR` de la plantilla `APLICACION_PETRO`), ventanilla/manual
   (`ContabilizacionIndividualCreditoServiceImpl`), cruces, precancelación y condonación: no distinguen si la cuota ya
   se devengó. (La mora sí lo distingue desde D-017: `ProvisionInteresServiceImpl.separarMoraNoDevengada`.)
3. **El universo del ④ es el del corte anterior** (`universo(corte)`, `PRSTFCHA < :corteSig`): la primera cuota de
   un préstamo concedido en el mes de su vencimiento no la devenga nadie.
4. (Menores) El abono a capital re-amortiza el interés de la cuota del mes ya devengada sin reversar la diferencia; la
   precancelación y el acuerdo de condonación no reversan el devengo de la cuota en curso que no cobran.

Normal vs al corte: en producción (`corte == null`) `PAGOS_VIGENTES` toma **todos** los pagos a la fecha de la
corrida, así que también se resta lo cobrado entre el día 1 y el día en que se corre el cierre: el defecto es igual o
mayor fuera del re-proceso.

## 6. Ejemplo al centavo — operación 65860 (hipotecario), cuota 94 (vto 31-ago-2025)

Interés de la cuota 382,37. Carga Petro/Arch 353 fechada 31-jul-2025 23:59: capital 119,57 + interés 382,37 (cuota
pagada completa un mes antes).

| Fecha | Asiento | Hoy | Propuesto (§7, alternativa C) |
|---|---|---|---|
| 31-jul | Aplicación Petro carga 353 | H 1.4.02.15 382,37 | igual |
| 31-jul | ⑪ ajuste a saldo de intereses recibidos por anticipado (nuevo, del cierre de julio) | — | D 1.4.02.15 382,37 / H 2.9.01.05(.xx) 382,37 |
| 1-ago | ④ devengo (cierre de julio) | 0,00 (resta lo cobrado) | D 1.4.02.15 382,37 / H 5.1.02.15 382,37 |
| 31-ago | ⑪ del cierre de agosto (la cuota ya no está por vencer) | — | D 2.9.01.05(.xx) 382,37 / H 1.4.02.15 382,37 |
| **Al 31-jul** | 1.4.02.15 / 2.9.01.05 / 5.1.02.15 de esta cuota | −382,37 / 0 / 0 | **0 / −382,37 / 0** |
| **Al 31-ago** | ídem | −382,37 / 0 / 0 | **0 / 0 / −382,37** (ingreso de agosto) |

Hoy la cuenta queda 382,37 por debajo de la cartera para siempre y ese interés nunca llega a ingresos.

## 7. Propuesta de corrección

### 7.1 Alternativas

| | Qué hace | A favor | En contra |
|---|---|---|---|
| **A. Ingreso directo en el cobro** (análogo a D-017) | El cobro del interés de una cuota no devengada acredita 5.1.02 en vez de 1.4.02 | Mismo criterio que la mora; 1.4.02 = cartera | Reconoce ingreso antes de su mes (no es devengo); toca **todos** los motores de cobro (Petro, ventanilla, cruce, precancelación, condonación, anulaciones); necesita saber por cuota si ya se devengó |
| **B. Diferido en el cobro** | El cobro anticipado acredita 2.9.01.05; el ④ lo reclasifica a 5.1.02 | Contablemente exacto en cada fecha | Toca todos los motores de cobro y sus reversos; la anulación de un cobro anticipado tiene que deshacer el diferido |
| **C. Corrección en el cierre** (recomendada) | ④ devenga el interés **completo** de la cuota (sin restar cobros desde el ancla) y un paso nuevo ⑪ ajusta al corte el saldo de 2.9.01.05 al anticipado de cuotas por vencer; ④b devenga la primera cuota de los préstamos del mes | Exacto en cada corte (B17); **un solo módulo** (cierre de cartera), cobros intactos; robusto a anulaciones (⑪ es ajuste a saldo, como ⑨); reproducible al corte | Una línea más en ④ y un asiento más por cierre; entre cobro y cierre la cuenta tiene el anticipado neteado (solo dentro del mes) |
| **D. Solo informe** | El motor no cambia; la línea conciliada lo muestra y cada mes se carga un asiento manual | Sin cambio de código | 5.1.02 subestimado; trabajo manual mensual; no corrige la causa |

### 7.2 Recomendada: C — cambios en `CierreCarteraServiceImpl` / `CierreCarteraDaoServiceImpl`

1. **④ (devengo, fechado día 1 del mes que abre):** interés = `DTPRINTR − interés cobrado ANTES DEL ANCLA` (lo de Delta,
   que ya está en el arranque), sin restar los cobros desde el ancla; condonado de plazo vencido igual que hoy; mora sin
   cambio. **Ojo con el universo:** hoy `pendiente(corte)` excluye la cuota ya pagada completa antes del mes (caso del
   §6) y `universo(corte)` excluye el préstamo cancelado después de pagar cuotas futuras; para el interés, el ④ tiene que
   incluir también las cuotas del mes que abre **con interés cobrado desde el ancla**, estén pagadas o no y siga o no el
   préstamo en el universo (excepto préstamos anulados, `ESTADOS_FUERA_DE_CARTERA`). Asiento tipo, por tipo de
   préstamo: D 1.4.02.xx / H 5.1.02.xx (plantilla 17, sin cambio de plantilla).
2. **④b (primera cuota, fechado al corte del mes que se cierra):** cuotas con vencimiento en el mes cerrado de
   préstamos concedidos en ese mismo mes: D 1.4.02.xx / H 5.1.02.xx por `DTPRINTR` completo (sus cobros ya acreditaron
   1.4.02). Misma plantilla 17.
3. **⑪ (intereses recibidos por anticipado, fechado al corte):** objetivo = Σ interés cobrado desde el ancla y hasta el
   corte de cuotas con vencimiento ≥ día 1 del mes siguiente (cuotas por vencer; mismo criterio de universo del punto 1);
   asiento por la diferencia contra el saldo de la cuenta: si sube, D 1.4.02.xx / H 2.9.01.05(.xx); si baja, al revés.
   Plantilla nueva (alterno a definir, líneas por tipo de préstamo como la 17). Semilla de plantilla → §4 de
   `PASE-A-PRODUCCION.md` al integrar.
4. Las tres consultas usan los fragmentos al corte existentes (`universo`, `pendiente`, `pagos(corte)`); en modo
   normal el mismo código con `corte == null`.
5. (Aparte, menores) abono a capital: al re-amortizar, reversar D 5.1.02 / H 1.4.02 la baja de interés de cuotas cuyo
   mes ya abrió; precancelación y acuerdo: reversar el devengo de la cuota en curso no cobrada.

**Cuentas verificadas en `CNT.PLNN`** (empresa en uso): 1.4.02.05 / .10 / .15 (nivel 4, movimiento), 5.1.02.05 / .10
/ .15 (nivel 4, movimiento; las de la plantilla 17), 2.9.01.05 "INTERESES RECIBIDOS POR ANTICIPADO" (nivel 4,
movimiento). **Ojo: 2.9.01.05 ya se usa** para los intereses de inversiones no privativas (apertura 412.215,04 de
Delta, cargas manuales jun/ago/sep-2025 y 2026). Para no mezclar la conciliación de inversiones: subcuenta nivel 5 bajo
2.9.01.05 (exige convertir la cuenta en grupo y mover su saldo) o usar 2.9.01.05 compartida separando por plantilla y
descripción (el B17 suma al nivel 4 igual). 2.9.01.90 "OTROS" (sin movimientos) es la salida sin tocar el plan, pero el
nombre del catálogo que corresponde es 2.9.01.05. Decide el líder con el contador.

### 7.3 Regularización de los meses ya cerrados

Objetivo en cada corte: 1.4.02 = vencido + mora + arranque (el arranque tiene su propia decisión); 2.9.01.05 =
anticipado por vencer; 5.1.02 = + anticipado de cuotas ya devengadas + primera cuota.

**Opción 1 — un asiento por mes** (reabrir y recerrar cada mes; deja cada B17 correcto). Incremento del mes, por
cuenta (D 1.4.02 = H 2.9.01.05 + H 5.1.02):

| Mes | Cta | D 1.4.02.xx | H 2.9.01.05 | H 5.1.02.xx |
|---|---|---:|---:|---:|
| 2025-06 | Q | 2.997,77 | 1.546,29 | 1.451,48 (primera cuota) |
| | H | 1.452,20 | 1.452,20 | 0,00 |
| 2025-07 | Q | 2.238,91 | 948,10 | 1.290,81 |
| | P | 20,38 | 20,38 | 0,00 |
| | H | 3.248,84 | 1.955,02 | 1.293,82 |
| 2025-08 | Q | 2.897,80 | 1.294,95 | 1.602,85 |
| | P | 43,99 | 23,61 | 20,38 |
| | H | 2.505,65 | 133,47 | 2.372,18 |
| 2025-09 | Q | 2.092,77 | 74,07 | 2.018,70 |
| | P | 0,00 | −17,54 (D) | 17,54 |
| | H | 1.466,48 | −660,68 (D) | 2.127,16 |
| 2025-10 (prelim.) | Q / P / H | 1.418,56 / 0,00 / 1.713,06 | −275,96 / −14,67 / −250,00 (D) | 1.694,52 / 14,67 / 1.963,06 |

**Opción 2 — un asiento acumulado** en el primer mes abierto (si no se reabren jun–sep): al 30-sep, Q D 10.227,25 /
H 2.9.01.05 3.863,41 / H 5.1.02.05 6.363,84; P D 64,37 / H 26,45 / H 37,92; H D 8.673,17 / H 2.880,01 / H 5.793,16.
Los B17 de jun–sep quedan como están (1.4.02 y 5.1.02 subestimados en las cifras de §3).

**Octubre:** su ④ (1-oct, corrida 86) y el del 1-nov (corrida 106) ya corrieron con la regla vieja; si el cambio se
integra antes de cerrar octubre, octubre lleva su asiento de la opción 1 (o se recorre su cierre de cartera); desde el
primer mes cerrado con el código nuevo no hace falta regularizar. **Arranque y residuo** (§3, §4) son partidas aparte,
para decisión de ASOPREP con constancia (regla 6).

### 7.4 Efecto en B17, G y cuadraturas

- **B17:** sube 1.4.02.xx, aparece 2.9.01.05 (pasivo) y sube 5.1.02.xx (resultado del ejercicio). Al 30-sep: 1.4.02
  +18.964,79, 2.9.01.05 +6.769,87, 5.1.02 +12.194,92 (con primera cuota).
- **G40–G51:** no cambian (la cartera, el interés vencido de la G48 y los aportes no se tocan).
- **Cuadraturas:** 21 = G42 y 13 − 1399 = G48 **no se afectan** (ni 1.4.02 ni 2.9 ni 5.1 entran).
- **Provisión de intereses ⑦** (1.4.99): no cambia (se calcula sobre interés y mora no cobrados de las cuotas).
- **Resumen Financiero (rama C27b):** con C aplicada, las partidas "anticipado" y "primera cuota" de la línea
  conciliada pasan a 0 para los meses cerrados con el código nuevo; quedan arranque y residuo.

### 7.5 Decisiones que necesita el líder

1. **Alternativa de motor** (recomendada C) y si entra antes del cierre de octubre o después.
2. **Cuenta del diferido**: 2.9.01.05 compartida con inversiones, subcuenta nivel 5, o 2.9.01.90.
3. **Meses cerrados**: opción 1 (asiento por mes, reabrir jun–sep) u opción 2 (acumulado en el primer mes abierto).
4. **Arranque** (Q −998,76, H −7.479,09): contra qué cuenta y con qué fecha (ancla), con constancia de ASOPREP.
5. **Residuo de §4** (9 cuotas): revisar contra cobros reales (frente A.1) antes de ajustar; los de acuerdos y
   precancelación, si se reversa el devengo (D 5.1.02 / H 1.4.02).
6. **Abono a capital** (66940): si la baja del interés de la cuota en curso por un abono es la regla de ASOPREP (si
   lo es, el motor debe reversar el devengo de esa diferencia).
7. **Petro/Arch al último día del mes aplicada a la cuota del mes siguiente** (≈ 91 % del anticipado): confirmar que
   es lo real y no un efecto de la fecha o del orden de aplicación de la carga.

## 8. Rama `cuadre/c27b-resumen-intereses`

- saaBE `C:\work\saaBE\wt\c27b`: `791bfa66` (WIP línea conciliada) + **`fdebdeb4`** (WIP: partida "primera cuota de
  préstamos concedidos en el mes, sin devengo"). Compila (`mvn -q -o package -DskipTests`). Con la partida nueva, lo
  no explicado de la línea es exactamente el residuo de §4.
- saaFE `C:\work\saaFE\wt\c27b`: `06f3a76` (sangría); la partida nueva usa el mismo prefijo, sin cambios.
- No toca motores (ni ④ ni cobros): la corrección del §7 es otro cambio (C29 sugerido), que requiere decisión.
- Sin merge, sin push, sin despliegue.

## 9. Reproducir

```
node 03-trabajo/meses/2025-10/scripts/correr-sql.js <abs>/sql/conciliacion-1402-AAAA-MM.sql --out <abs>/resultados
node 03-trabajo/meses/2025-10/scripts/correr-sql.js <abs>/sql/intereses-cohortes-AAAA-MM.sql --out <abs>/resultados
node 03-trabajo/meses/2025-10/scripts/correr-sql.js <abs>/sql/linea-conciliada-c27b.sql --out <abs>/resultados
```

Nuevos meses: copiar `plantilla-conciliacion-1402.sql` reemplazando `__CORTE__` por `DATE 'AAAA-MM-DD'` (el corte tiene
que ser fin de mes) y la primera línea por un comentario; el ancla está fija en 2025-05-31.
