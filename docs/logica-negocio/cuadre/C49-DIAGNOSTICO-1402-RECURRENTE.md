# C49 — Diagnóstico: por qué la 1.4.02 se descuadra casi lo mismo cada mes frente al objetivo D-206

Equipo omen-saa-1 · 2026-11-27 · lote 28 (solo análisis; no se tocó código de negocio). SQL de solo lectura: `c49-diagnostico-1402-por-prestamo.sql` (esta misma carpeta).

## 0. Resumen
- **No pude fijar la causa solo leyendo código y los agregados del cuadre**; sí pude descartar varias, acotar el tipo de mecanismo y dejar un SQL que, corrido sobre el respaldo v9 de un mes, decide cuál es.
- Lo que los datos del cuadre ya dicen (sección 1): el hueco es un **flujo mensual** (no un saldo viejo), solo de la **1.4.02** (la provisión sigue al objetivo), **casi igual en PH y PQ y nunca en PP**, y **no crece con la cartera** ni con el interés pendiente.
- La hipótesis más probable, y por qué, está en la sección 4. La regla del cuadre y la del código **no son idénticas en tres puntos** (sección 5); ninguno por sí solo explica ~550 constantes, pero dos de ellos pueden mover la frontera de qué cuotas cuenta cada lado.
- **No encontré un defecto claro que corregir** en este lote. Lo que sí quedó señalado para otro lote: ver sección 6.

## 1. Lo que dicen los datos (CSV `def9-*-control-1402-provision-*` del cuadre, solo agregados)
AJUSTE_1402 = objetivo − saldo contable (signo + = falta interés en la contabilidad). «Antes» = lo que el control dio antes del asiento manual de ese mes; los meses siguientes ya arrastran los asientos anteriores (el «después» de enero y de julio quedó en 0,00), por lo que **cada cifra es el hueco NUEVO de ese mes**.

| Mes 2026 | PH antes | PQ antes | PP | Provisión (ajuste) | ARRANQUE PH / PQ |
|---|---|---|---|---|---|
| enero | 2.880,46 | 1.962,38 | 0 | 0 | −7.479,09 / −998,76 |
| febrero | 555,81 | 448,16 | 0 | 0 | idem |
| marzo | 549,53 | 436,25 | 0 | −44,67 | idem |
| abril | 543,79 | 607,71 | 0 | 109,29 | idem |
| mayo | 528,63 | 543,20 | 0 | 100,90 | idem |
| junio | 542,86 | 554,85 | 0 | 49,98 | idem |
| julio | 516,66 | 601,39 | 0 | 215,39 (dato del cuadre) | PQ −1.028,42 |

Lecturas:
1. **Flujo, no saldo.** Si fuera un faltante viejo, febrero ya habría quedado en 0 después del asiento de enero. Aparece de nuevo, casi igual, cada mes (≈ +550 PH, ≈ +450–600 PQ).
2. **Solo la 1.4.02.** La provisión (⑦) queda a ±100 del objetivo: ⑦ y objetivo miden lo mismo (el interés pendiente por cuota); el que no telescopa es el flujo de la 1.4.02 (④, ④b, ⑩, ⑪ y cobros).
3. **No es proporcional a la cartera.** El interés pendiente crece ~20 mil por mes en PH y ~3 mil en PQ y la mora ~8 mil y ~5 mil; el hueco no se mueve. Es más parecido a «un número fijo de cuotas/préstamos» o a un efecto de calendario que a una tasa.
4. **El arranque no es la causa del flujo**: es constante (−7.479,09 / −998,76). Solo en PQ cambia entre corridas (−1.028,42 en julio, −1.048,48 en agosto), señal de que el interés pendiente **al ancla** (`iv0`) se mueve cuando cambia el estado actual de los préstamos entre una corrida y otra: el objetivo depende del estado de HOY de cada préstamo (`PRSTIDST`), no del de entonces.
5. **PP nunca difiere.** El mecanismo existe en PH y PQ y no en PP (cartera pequeña: ~4–5 mil). Útil para el SQL: en PP todos los grupos de causa salen en 0.
6. **Agosto**: PH +7.249,03 y provisión −12.095,61. No es el mismo fenómeno: es un salto de una vez (un orden de magnitud sobre el flujo) y del lado de la provisión también. El ⑪ avisa objetivo 5.901,34 contra 7.588,47 ya asentado: **el ⑪ de agosto reversa 1.687,13** (D pasivo / H 1.4.02) si la base de transición es 0; eso baja la 1.4.02 y aporta al «+» de agosto. Pide revisar qué cuotas dejaron de ser «anticipadas» entre julio y agosto (el SQL, causa 1 y fila NA2).

## 2. Cómo se arma cada lado (con archivo y línea)
**Objetivo D-206** (plantilla C27b, la copia el SQL): por cuota, `iv1 = GREATEST(DTPRINTR + DTPRINVN − Σ PGPRINPG − Σ PGPRINVP, 0)` de las cuotas con vencimiento ≤ corte, de préstamos «vivos al corte» (`u1`), que no están `estado IN (4,7)` con `DTPRFCPG < corte+1`, más la mora pendiente del libro MVIC (5 − 6 − 9) menos la mora cobrada (`PGPRMRPG`) desde el ancla. Los cobros se ubican por **`PGPRFCHA`**.

**1.4.02 contable** (flujos): ④ `CierreCarteraServiceImpl.armaDevengoIntereses` (CierreCarteraServiceImpl.java:1302): D 1.4.02 / H ingreso del interés de las cuotas que vencen en el mes que se abre, con la consulta por cuota `CierreCarteraDaoServiceImpl.selectInteresDevengoPorCuotaEnRango` (:1119): `GREATEST(DTPRINTR − pre, post)`, donde `pre` = cobros con fecha < ancla (2025-05-31) y `post` = cobros desde el ancla; la fecha del cobro es `LEAST(PGPRFCHA, NVL(PGPRFCRG, PGPRFCHA))` (decisión C33). ④b `armaPrimeraCuota` (:1402, consulta :1151) para préstamos concedidos en el mes. ⑩ `armaDevengoMoraCorte` (:2003). ⑪ `armaInteresAnticipado` (:1442, objetivo `selectInteresAnticipadoPorTipo` :1221). Cobros: `CobroPetroContableServiceImpl` (:1114): H 1.4.02 por `PGPRINPG` (el interés vencido cobrado lanza excepción: hoy es cero) y, para la mora, solo la devengada (:1059, `separarMoraNoDevengada`; el resto va a ingreso 5.1.02).

## 3. Hipótesis, evidencia en el código y qué esperaría ver en el SQL
(Nivel 1 del SQL = diferencia por cuota `iv1 − libro emulado`, agrupada por causa; nivel 2 = lo no atribuible a un préstamo.)

**(a) ⑪ interés anticipado.** `armaInteresAnticipado` asienta por tipo `objetivo − asentado − base de transición`. Lo asentado se lee de los ⑪ de las corridas EJECUTADAS (`selectAnticipadoAsentadoPorCuenta`, :1255, con `ANCCFCHA < corte`), y la base es la propiedad `saa.cartera.anticipadoBaseTransicion` (el asiento manual C27b). Dos detalles: el objetivo del ⑪ cuenta solo `PGPRINPG` y de cuotas con vencimiento ≥ primer día del mes siguiente; por cuenta, no por préstamo. **Explica un monto casi constante?** No por sí mismo: el ⑪ es un stock que se corrige solo cada mes (sube con lo cobrado por adelantado, baja cuando esas cuotas vencen). **Sí puede explicar el salto de PH en agosto** (el reverso de 1.687 y la diferencia entre objetivo y asentado). En el SQL: fila `NA2` (⑪ asentado) contra la causa `1 ANTICIPADO` del nivel 1 (lo que debería restituir). Si `NA2` ≈ `−(causa 1)`, el ⑪ está bien. Si `NA2` queda corto en agosto, el ⑪ no recogió préstamos que salieron del universo.

**(b) ④/④b contra cobros del mismo mes.** El ④ de V se arma al cierre de V−1 con el estado de ESE momento (`CONCEDIDO_EN_CARTERA`, `CANCELADO_TERMINADO_AL_CORTE_SQL`, `condonado(corte)`), mientras el objetivo mide al corte de V con el estado de V (`u1`). Un préstamo que entra o sale entre los dos cierres, o una cuota con condonación posterior, deja una diferencia que el ④ ya no rehace. Esto explica diferencias **por cuota** y por mes, pero un monto fijo solo si se repite el mismo tipo de caso cada mes. En el SQL: causas `2 CANCELADO_EN_EL_MES` y `5 FUERA_DE_CARTERA_ANTES`, y la columna `dev_emulado` contra `dev_libro_mvic` (MVIC 11/12: lo que el ④ realmente grabó por cuota) y fila `NA4` (④ asentado − emulado).

**(c) Cuotas pagadas parcialmente.** El objetivo cuenta lo impago de una PARCIAL como `intr − Σ PGPRINPG`; el ④ devenga la cuota entera (menos lo anterior al ancla) y el cobro baja la 1.4.02 por lo cobrado: telescopa. **No lo explica por regla**; solo si algún cobro parcial cae en un mes distinto del que lo ve el objetivo (`LEAST(FCHA, FCRG)` contra `FCHA`, sección 5). En el SQL: causa `3 PARCIAL`. Si el valor es ≈ 0, descartada.

**(d) Préstamos cancelados en el mes.** El objetivo pone en 0 el `iv1` de un préstamo no vivo al corte; si la contabilidad conserva el interés pendiente de un préstamo que se canceló con interés impago (sin condonación ni cobro), la 1.4.02 queda **por encima** (signo −), no por debajo; para el signo + haría falta lo contrario (interés cobrado de un préstamo al que el ④ no devengó porque ya no estaba en cartera). En el SQL: causa `2 CANCELADO_EN_EL_MES`. Un valor > 0 aquí sería el caso del signo +.

**(e) Redondeos por cuota.** El ④/④b redondean cada cuota a centavos (`redondeaDouble`, :1325) y suman por tipo; el objetivo suma los valores del libro. Los intereses ya están en centavos: la diferencia máxima es de centavos, no de cientos. **Descartada** (en el SQL, la fila `CHECK` mostrará los centavos).

**(f) Lo que se ve al leer ④, ⑦, ⑩, ⑪, el cobro y la condonación.**
1. `armaDevengoIntereses`: con el devengo completo activo, la mora del ④ sigue saliendo «del método de siempre» (:1311) y ⑩ devenga mora al corte por el libro (:2003): dos asientos distintos de mora a la misma cuenta. Si el ④ de V ya devengó parte de la mora que ⑩ también devenga, el exceso quedaría **por encima** del objetivo (signo −). Aun así lo señalo porque cae en la fila `NA5` (mora asentada) del SQL.
2. El objetivo de mora resta **toda** la mora cobrada desde el ancla (`PGPRMRPG`), incluida la que el cobro llevó **directo a ingreso** (H4, `separarMoraNoDevengada`): el objetivo de mora puede quedar por debajo de lo que tiene la 1.4.02 (signo −).
3. El cobro Petro acredita `PGPRINPG`, y el objetivo resta `PGPRINPG + PGPRINVP`; el interés vencido cobrado hoy es 0 (el cobro lanza excepción si no), así que son iguales.
4. La condonación: el objetivo resta `MVIC 9` (componente 1) por cuota; el ④ pone devengo 0 a las condonadas (`condonado(corte)`, :1100). Si una condonación no tiene `MVIC 9`, la 1.4.02 queda con interés que el objetivo ya no cuenta (signo −); al revés (MVIC 9 sin asiento) da signo +. En el SQL: columna `cond` dentro del libro emulado; compare `NA5`.
5. Las cuotas 7 sin fecha (C48): no explican estos números en la v9 (el cuadre las fecha antes del cierre). La columna `cuotas7_sin_fecha` del SQL lo confirma o lo descarta mes a mes, y en producción sí puede pesar.

## 4. Hipótesis más probable
**Un mecanismo de flujo, de pocas cuotas y repetido cada mes, que vive en la frontera entre «lo que el ④ ve al cierre de V−1» y «lo que el objetivo ve al corte de V».** Razones: (1) es un flujo y no un stock (sección 1.1); (2) solo toca la 1.4.02 y no la provisión, es decir, no es un error de datos de las cuotas sino de **qué cuotas devenga el ④** (el ⑦ lee el estado al corte); (3) no escala con la cartera, así que no es de tasa; (4) PP no lo tiene, y PH y PQ lo tienen casi igual: parecido a «unos pocos préstamos por mes por tipo» (por ejemplo, préstamos concedidos o desembolsados el mes en que se cierra, cuya primera cuota cae fuera del ④ y del ④b por la fecha de concesión `PRSTFCHA` o por su estado en cada cierre).
- La causa concreta que mejor encaja con el código es el **④b** (`selectInteresPrimeraCuotaPorCuota`, :1151): solo cubre préstamos con `PRSTFCHA` dentro del mes que se cierra y cuotas con vencimiento dentro del mismo mes. Un préstamo cuya primera cuota vence en el mes de su concesión **pero cuyo `PRSTFCHA` es anterior al día 1** (fecha de la solicitud anterior al desembolso) no entra al ④ del cierre anterior (porque no estaba en cartera entonces) ni al ④b (porque `PRSTFCHA < inicio`): su interés nunca se devenga. El objetivo sí lo cuenta (`lu.u = 1` por estado actual). Es la categoría `1 primera cuota de prestamo del mes, sin devengo` de la propia plantilla del cuadre. **Confianza media-baja: no lo pude confirmar sin los datos.**
- En el SQL se confirma si la causa `4 SIN_CAUSA` (o `cat_plantilla = '1 …'`) concentra el hueco en pocas operaciones nuevas del mes, y si `dev_libro_mvic` de esas cuotas es 0 mientras `dev_emulado` no lo es.

Alternativa si no es esa: el caso (d) en su versión «interés cobrado de un préstamo al que el ④ ya no devengó» (cuotas de préstamos que cambian de estado el mismo mes), que el SQL aísla en la causa 2.

## 5. Donde la regla del cuadre y la del código NO son la misma
1. **Fecha del cobro.** Código: `LEAST(PGPRFCHA, NVL(PGPRFCRG, PGPRFCHA))` (C33: «un pago registrado antes del corte existe al corte»). Plantilla: `PGPRFCHA`. Un pago de migración fechado a un vencimiento futuro y registrado antes cuenta como cobrado para el ④, el ⑪ y las lecturas al corte, y como NO cobrado para el objetivo. Afecta a cuotas con cobros de migración (poca gente); el SQL de este lote usa la fecha de la plantilla a propósito, para ver esa diferencia en `NA4/NA5`. Si el cuadre quiere usar la regla del código, cambia `PGPRFCHA` por `LEAST(...)` en la CTE `pgc`.
2. **Estado del préstamo.** Código: el estado de V−1 para el ④ y el de V para ⑦/⑪; ambos son el **estado actual** (`PRSTIDST`), no el histórico. Objetivo: el estado actual evaluado en cada fecha (`lu`). Sin historia de estados, un préstamo reabierto o cancelado después se ve distinto según el día en que se corre la corrida.
3. **Mora pagada.** Ver (f).2: el objetivo resta la mora cobrada completa; el código solo cancela la devengada contra la 1.4.02.
Ninguno de los tres hace «mal» a un lado: son decisiones distintas. Para D-206, la regla que debe mandar es la que el contador firmó (objetivo del cuadre); si el código usa otra, el código es el que se ajusta.

## 6. Arreglo propuesto (sin programarlo)
Según lo que muestre el SQL:
- Si domina `SIN_CAUSA`/`cat 1` con préstamos del mes: **`CierreCarteraDaoServiceImpl.selectInteresPrimeraCuotaPorCuota`** (:1151) debe incluir préstamos cuya primera cuota vence en el mes **aunque su `PRSTFCHA` sea anterior al mes**, siempre que no estuvieran en el universo del ④ del cierre anterior. Regla: «toda cuota con vencimiento en el mes que no devengó el ④ del mes anterior por no estar su préstamo en cartera entonces».
- Si domina `ANTICIPADO` o `NA2`: **`armaInteresAnticipado`** (:1442) debe calcular el objetivo con el mismo universo del objetivo D-206 (préstamos vivos al corte) y no con `CONCEDIDO_EN_CARTERA` actual; y revisar la base de transición.
- Si domina `CANCELADO_EN_EL_MES`: decidir con el contador qué pasa con el interés devengado y no cobrado de un préstamo que sale de cartera (cobro, condonación o castigo): hoy no hay asiento que lo saque de la 1.4.02 y el objetivo sí lo saca.
- Si domina `PARCIAL`: igualar la fecha del cobro de una de las dos reglas (sección 5.1).
- **Último recurso**: el ajuste automático (`saa.cartera.ajusteUltimoRecurso`, lote 22/24) solo después de que el SQL deje un residuo que no se pueda atribuir a una causa del código.

## 7. Qué necesito que corra el cuadre
1. `c49-diagnostico-1402-por-prestamo.sql` en el respaldo v9 de **mayo, junio y julio** (cambiando la fecha de la primera línea, `DATE '2026-07-31'`), y de agosto si el respaldo trae los asientos del cierre de agosto. Es un solo SELECT.
2. Devolver el resultado completo como CSV (sin nombres: la operación es el número de operación). Con eso se ve: (1) que el `CHECK` por tipo es 0,00; (2) por tipo, qué causa suma ≈ 550; (3) qué operaciones la forman y si se repiten de un mes a otro; (4) si `NA2` cubre lo `ANTICIPADO`.
3. Si el respaldo no tiene el libro MVIC 11/12 (corrida sin los lotes 19/20), las columnas `dev_libro_mvic` salen en 0: es normal, el resto no depende de ellas.
4. Si ninguna causa concentra el hueco, devolver también la línea `NA5` de cada mes: es lo que no se puede atribuir a un préstamo, y llevaría la investigación al lado de los asientos manuales vigentes y de la mora.
