# C33: pagos registrados antes del corte pero fechados después (incidente préstamo 3492, 2026-11-25)

## Qué pasó
Las cuotas 45-68 del 3492 estaban pagadas con pagos de **MIGRACIÓN** cuya `PGPRFCHA` es el **vencimiento** de la cuota (futuro, hasta 2028), registrados el 22-04-2026. Al corte 2026-11-30 el control C33 y la re-imputación leían solo los pagos con `PGPRFCHA <= corte`: no veían esos pagos, creyeron que la cuota 49 tenía saldo antes de la 69 (pagada por Petro, préstamo adelantado) y movieron los pagos de 69/70/71 a 49/50/51 (pagadas al doble). Corrida: 97 aplicados, 769 anulados, 1256 creados, 38 asientos de provisión (13166 al 13203: 26 de re-provisión y 12 de reverso); el control 15 sin filtro de fecha solo halló pagado de más en el 3492.

## Reverso (solo 3492) — `sql/402_REVERSO_REIMPUTACION_C33_PRESTAMO_3492.sql`
Respaldo (PGPR, DTPR, MVIC) → controles (6 nuevos 312586..312591; 4 anulados 289354, 289355, 292586, 292587) → anula los nuevos y reactiva los anulados → reconstruye las cuotas tocadas (49, 50, 51, 69, 70 y 71, deducidas de los pagos) desde sus pagos vigentes con la fórmula de `recalcularCuotaDesdePagos` → controles → **sin COMMIT**.
- **Contable:** en el JSON `asientoReProvision` y `asientoReversoProvision` son null: no hay asiento que revertir en el 3492; el bloque 0c lo comprueba (debe dar 0 filas; si diera filas no se tocan por SQL).
- Las cuotas se reconstruyen por fórmula, no restaurando valores previos (no había respaldo anterior a la corrida): comparar con una cuota vecina intacta. La mora de 69-71 no se recalcula; el bloque 7 la muestra.

## Medición de toda la cartera — `sql/403_MEDICION_PAGOS_FECHADOS_DESPUES_DE_SU_REGISTRO.sql`
A: pagos vigentes con `PGPRFCHA > PGPRFCRG` por tipo; B: los que importan al corte (fecha después del corte, registrados antes) por tipo y usuario; C: préstamos afectados; D: el control C33 al corte en dos variantes (regla actual vs nueva) con veredicto por préstamo (`SOLO_ACTUAL` = rotura falsa por esto; `SOLO_NUEVA` = a revisar; `AMBAS` = real). **Falta correrlo** (no tengo acceso a la base): su resultado dice en cuántos préstamos el control da la rotura falsa.

## Corrección en el código (programada, sin commit)
1. **Un pago existe al corte si su fecha es <= corte O si se REGISTRÓ (`PGPRFCRG`) hasta el corte.** Método nuevo `PrelacionCuotas.alCorteORegistrado`, usado por la lectura de pagos de la re-imputación y del gancho `asegurarPrelacion` (`pagosAlCortePorCuota`), y la misma condición en el SQL del control del cierre (`selectControlPrelacionAlCorte`: `OR g.PGPRFCRG < :corteSig`). `alCorte` (el del motor de pagos, que aplica a la fecha del pago) **no cambia**.
2. **Un pago con fecha posterior al corte no se mueve**: en `reimputar`, `mueve` exige `cuentaAlCorte(p.getFecha(), corte)`. Cubre la cuota donde está pero no se lleva a otra: la re-imputación no puede apilar pagos sobre una cuota ya cubierta por un pago así.
3. Efecto en el 3492 al 2026-11-30: la 49 queda cubierta por el pago de migración, no hay rotura y no se re-imputa.
4. No cubierto: `saldoPosibleSql` (la condición de «cuota con saldo posible» del control) sigue por fecha; revisar si se usa con corte en el cierre.
5. Riesgo: pagos legítimamente fechados a futuro y registrados antes (anticipos, migración) ahora cuentan en el control; el bloque D del 403 lo mide (`SOLO_NUEVA`).

## ¿La omisión del control con motivo cubre el cierre de noviembre?
**Sí, para el cierre de noviembre alcanza:** con el 3492 revertido, el control al 2026-11-30 lo volvería a ver roto (falso) hasta desplegar la corrección; omitirlo con motivo («pagos de migración fechados después del corte, ver este documento») lo deja fuera sin tocar datos. Condiciones: la omisión debe ser **por préstamo** y solo para los que el bloque D del 403 marque `SOLO_ACTUAL` (omitir todo C33 taparía roturas reales), y **no volver a correr la re-imputación automática ni por lote hasta desplegar la corrección**, o repite el daño en otros préstamos con el mismo patrón.

## Reclasificación del reparto por componente (92 préstamos sin asiento de reclasificación)
La re-imputación cambió el reparto sin cambiar el total: interés +1976,37; capital −1590,33; mora −308,53; seguro −182,40; desgravamen +104,89 (suma 0). La contabilidad conserva el reparto viejo.
- **Medición** — `sql/404_MEDICION_DELTA_COMPONENTES_REIMPUTACION_C33.sql`: delta por componente (A), por tipo de préstamo (B, base del asiento) y los 30 préstamos de mayor cambio (C). Pendiente de correr (excluir el 3492 una vez revertido).
- **Cuánto de la diferencia del ⑨** (H −53.163 → −63.771 = −10.608; Q −78.846 → −82.969 = −4.123): **no se puede atribuir desde el código sin ver la definición del ⑨ del cuadre.** Lo que sí se ve: el reparto reclasificado total es de ±1.976 (interés) y ±1.590 (capital), menor que esas diferencias, así que **a lo sumo explica una parte**. Para cerrarlo: comparar el ⑨ por componente contra el 404-B por tipo de préstamo.
- **Asiento propuesto (uno por corrida, por plantilla, pendiente de decisión del contador):** por cada tipo de préstamo y componente con delta ≠ 0, una línea con la cuenta que resuelve la plantilla de aplicación de cobro para ese componente (el mismo papel que usa el cobro; nunca una cuenta en el código): delta **positivo** → Haber; delta **negativo** → Debe. Como la suma de deltas es 0, cuadra solo. Fecha contable: la de la corrida (2026-11-25) o la que decida el contador; origen `C33_RECLASIFICACION`. **No se programa hasta la decisión del contador** (reclasificar contra las mismas cuentas de cartera, o dejarlo como ajuste del cuadre).

---

## ACTUALIZACIÓN (programado, sin commit): el reproceso de noviembre manda; el reverso del 3492 (sql 402) queda EN PAUSA

### Regla única: «fecha efectiva» del pago = la menor entre PGPRFCHA y PGPRFCRG
Un pago existe al corte si su fecha O su registro caen hasta el corte. Para clasificar un pago en un período (antes/dentro del mes) se usa `LEAST(PGPRFCHA, NVL(PGPRFCRG, PGPRFCHA))`. Un pago normal tiene registro >= fecha, así que su fecha efectiva es su fecha y nada cambia; solo cambian los pagos fechados a futuro y registrados antes (migración).

### Sitios corregidos (archivo, qué leía, efecto del cambio)
| Sitio | Paso | Cambio |
|---|---|---|
| `DetallePrestamoDaoServiceImpl.selectControlPrelacionAlCorte` (CTE `pg`) | control C33 / simulación / re-imputación | `OR g.PGPRFCRG < :corteSig` |
| `PrelacionCuotas.alCorte` (+ alias `alCorteORegistrado`) | motor de saldos al corte, Petro C33b (`CargaArchivoPetroServiceImpl`), lectura de la re-imputación | cuenta también el registrado hasta el corte |
| `PrelacionCuotasServiceImpl.reimputar` (`mueve`) | re-imputación | un pago con fecha posterior al corte cubre su cuota pero NO se mueve |
| `DetallePrestamoDaoServiceImpl.saldoPosibleSql` (conCorte) | cuotas con saldo posible (control y motor) | `OR g.PGPRFCRG <= :corte` |
| `CierreCarteraDaoServiceImpl` función `pagos(corte)` (~167; la usan G48, CCPM, bandas, apertura del neteo, universo de ①/⑥) | pagado al corte por cuota (capital, interés, mora, seguros) | `OR PGPRFCRG < :corteSig` |
| `CierreCarteraDaoServiceImpl` ~1102-1104 (`selectInteresDevengoPorTipoEnRango`), ~1132 (`selectInteresPrimeraCuotaPorTipo`), ~1158 (`selectInteresAnticipadoPorTipo`) | ④ devengo, ④b primera cuota, ⑪ anticipado | `pre`/`post`/rango por fecha efectiva |
| `PagoPrestamoDaoServiceImpl.selectDatosPagosVigentesAlCorte` (~190) | ⑦ provisión de interés y mora, ajuste del tope | `(p.fecha < :corteSig or p.fechaRegistro < :corteSig)` |
| `MovimientoInteresCuotaDaoServiceImpl` ~316 (mora pagada por préstamo) | tope de provisión de mora (lote 7b) | `OR g.PGPRFCRG < :limiteSig` |
| `ModoCorteCartera.CANCELADO_TERMINADO_AL_CORTE_SQL` (~105) y `DetallePrestamoDaoServiceImpl` ~1309 | universo al corte (préstamo cancelado con pagos posteriores sigue vivo) | el «pago posterior» se mide por fecha efectiva: un pago de migración registrado antes del corte no mantiene vivo un cancelado |

### NO tocado a propósito (decidir)
- `ProcesoMoraPrestamoServiceImpl` ~471-477 y ~517 / `PrelacionCuotas.moraSobreCapitalPendiente` (~577): la mora de ① se detiene en la FECHA del pago; con un pago de migración de fecha futura la mora podría seguir corriendo sobre capital ya cobrado (solo si la cuota no está PAGADA y con la propiedad `moraSobreCapitalPendiente` activa). Tocar la mora es más delicado; medir primero (403-D no lo cubre).
- `ProvisionInteresServiceImpl` ~815 (`topeTransicionDevengo`, solo cierres previos a sep-2026 sin MVIC).
- Correctos por diseño (semántica «a la fecha de la operación»): `ProvisionInteresServiceImpl` ~178/196, `MovimientoInteresCuotaDaoServiceImpl` ~191/212, `ProcesoMoraPrestamoServiceImpl` ~625.
- Efecto de método: `PrelacionCuotas.alCorte` cambia para el motor y Petro: un pago registrado antes de la fecha de aplicación pero fechado después ahora cuenta; probar un reproceso de carga Petro en copia con un préstamo adelantado por migración.

### Reclasificación de la corrida (programada; pendiente de decisión del contador para ASENTAR)
`POST /rest/prelacioncuotas/reclasificar {fecha (día de la corrida), usuario, idEmpresa, simular}` → `PrelacionCuotasService.reclasificarCorrida`:
- Toma los pagos anulados por la corrida (motivo `C33-REIMPUTADO`, anulados desde ese día) y sus copias vigentes (observación «[C33-REIMPUTADO de PGPR <id>…», con <id> entre los anulados).
- Para cada conjunto, por tipo de préstamo, arma las líneas de Haber con `ContabilizacionIndividualCreditoService.haberReclasificacion` = la MISMA lógica de `haberDesdePagos` (banda de capital por vencimiento, interés, mora, seguros, por la plantilla de aplicación de cobro) **sin efectos** (no separa mora no devengada ni escribe MVIC). Ninguna cuenta en el código.
- Asienta la **diferencia por cuenta**: neto > 0 al Haber, neto < 0 al Debe. Debe = Haber o no se asienta (error con el detalle). Un asiento por corrida (tipo CRÉDITOS, módulo cuentas por cobrar), con fecha = la de la corrida.
- `simular=true` no graba y devuelve las líneas (`detalle`). **Simular primero y pasarlo al contador.** No es idempotente: no repetirlo con la misma corrida.
- Si el reverso del 3492 (402) se corre ANTES, sus pagos reactivados/anulados ya no aparecen en la corrida y quedan fuera solos; si se corre DESPUÉS de reclasificar, habría que ajustar a mano ese préstamo.
- Los pagos con capital en `saldoOtros` (abono, precancelación) no son movibles por la re-imputación, así que no entran.

### Notas del árbitro (2026-11-25), sin cambio de código
- **Mora de ①** (`ProcesoMoraPrestamoServiceImpl` ~471/517, `moraSobreCapitalPendiente` ~577) se queda como está: un pago de migración lleva fecha = vencimiento de su cuota; si la cuota ya venció al corte su fecha es <= corte y la mora se detiene bien; si no venció, no corre mora. Supuesto no medido: que ningún pago de migración tenga fecha distinta de su vencimiento (el 403-B lista los pagos con fecha > corte y registro anterior; compararlos con `DTPRFCVN` de su cuota lo confirma).
- **`topeTransicionDevengo`** (solo cierres previos a sep-2026): sin cambio.
- **Cortes anteriores al 22-04-2026** (fecha de registro de la migración): esos pagos no existen en la base de ese momento ni por fecha ni por registro. Es correcto para esa fecha, pero un reproceso de esos meses daría las mismas roturas falsas del C33. Hoy los reprocesos parten del ancla 2026-07-31.
- La reclasificación se corre primero en simulación, cuando el contador dé el OK.

### Lote 15: la re-imputación no salta cuotas que no toca (incidente del 2026-11-25)
- **Incidente.** La corrida de `reimputarAlCorte` del 25-11 en producción (97 préstamos) se deshizo con un restore (`crd/sql/405`, commit 40074738). En al menos 31 préstamos dejó cuotas PAGADAS en medio de cuotas EN MORA. Caso tipo (préstamo 6741): la cuota 31 tenía pendiente y la 41 era la primera posterior con pagos; la herramienta tomó los pagos de las cuotas 41 y 43-46 y llenó con ellos las 31-34. No tocó las 35-40 ni la 42 (pagos exentos —tipo exento o `PGPRSLOT > 0`— o cuotas cerradas por regla, que `planificar()` se salta). Resultado: 31-33 pagadas, 34 parcial, 35-40 pagadas, 41 en mora, 42 pagada, 43-46 en mora. El control posterior (`detectarRotura` / `selectControlPrelacionAlCorte`) no lo vio porque solo mira pagos no exentos. Mismo patrón en 7694, 7757, 7841, 7896, 2904, 4952 y 6907.
- **Regla** (`PrelacionCuotas.cuotasSaltadas`, estática y pura, junto a `planificar`): una cuota es NO movible si está cerrada por regla (`estaCerradaPorRegla`) o conserva pagos exentos o fijos (con capital o interés) que el plan no mueve. Si alguna asignación lleva plata a una cuota de número MENOR que una NO movible, y la plata sale de un pago de una cuota de número MAYOR que esa misma, no se re-imputa.
- **Efecto.** En `PrelacionCuotasServiceImpl.reimputar`, después de `planificar()` y antes de la validación en memoria, de simular y de grabar: mensaje «No se re-imputa: re-aplicar en orden saltaría la(s) cuota(s) #… (cerradas o con pagos exentos) y las dejaría pagadas entre cuotas en mora. Revisar con crédito.», sin ninguna escritura, igual en simulación y en ejecución. `planificar()` y `detectarRotura()` no cambian (los usa el cierre de cartera).
- **6741 con la regla.** Las asignaciones llevan plata de las cuotas 41 y 43-46 a las 31-34; las 35-40 (exentas o cerradas) y la 42 son NO movibles y quedan entre el destino y el origen: se rechaza nombrando #35, #36, #37, #38, #39, #40 y #42, y no se escribe nada. Razonado sobre el código, no probado contra la base.
- **Ajuste: cuota PAGADA sin respaldo («marca»).** Una cuota en estado PAGADA (4) o CANCELADA_ANTICIPADA (7), desde el inicio de la rotura, que no cede ningún pago movible (no es origen de ningún pago del plan) también es NO movible, aunque no tenga pagos ni esté cerrada por regla (vencimiento posterior a `desde`). Motivo: `planificar()` la ve con saldo pendiente, pero si la plata se acaba antes de llegar a ella, el dinero de las cuotas posteriores pasa por encima y la cuota sigue pagada entre cuotas en mora. Una cuota en estado 4 que SÍ cede pagos es origen y no entra. En sentido contrario, una cuota a la que el plan sí le da plata (destino de alguna asignación) no cuenta como saltada, aunque estuviera marcada.
- **6741 con el ajuste** (razonado sobre el código): (a) cuotas 35-40 con pagos exentos: NO movibles por tener pago fijo; se rechaza nombrándolas, y la 42 igual. (b) cuotas 35-40 en estado 4 sin pagos: NO movibles por marca y sin ser destino (la plata se acaba en la 34); se rechaza nombrando #35 a #40. En los dos casos no se escribe nada.
