# C48 — Las cuotas en CANCELADA_ANTICIPADA (7) quedan con fecha de pago

Equipo omen-saa-1 · 2026-11-27 · lote 27, punto 4.1 del encargo de controles de producción (acordado con el cuadre el 27-11). Va también al EAR de producción
(`ear/prod-1127`, sin C19b) por cherry-pick: no depende de la C19b ni de los lotes VCN.

## 1. El defecto
`AcuerdoCondonacionServiceImpl` y `ProcesoPagoPrestamoServiceImpl.precancelar` ponían `DTPRFCPG = NULL` al pasar una cuota a 7. Las lecturas al corte
(`CierreCarteraDaoServiceImpl.pendiente`: `NOT (NVL(DTPRESTD,0) IN (4,7) AND NVL(DTPRFCPG, :corteSig) < :corteSig)`, y `ModoCorteCartera`) tratan una cuota 7 sin
fecha como **no liquidada al corte, para siempre**. Efecto: el libro de devengo y la provisión por cuota, las bandas y la G48 leen vivas cuotas ya canceladas. En la v9 de
julio eran 5 préstamos y 156 cuotas; el cuadre las fechaba a mano antes de cada cierre.

## 2. Qué cambia
### 2.1 Los motores fechan la cuota (caminos que dejan una cuota en 7)
| Camino | Archivo | Resultado |
|---|---|---|
| Acuerdo de condonación (`aplicarAcuerdo`) | `AcuerdoCondonacionServiceImpl` | **Cambiado**: `setFechaPagado(fechaHora)`, la misma del evento y del pago (`fecha.atStartOfDay()`). |
| Precancelación, con y sin préstamo nuevo (`precancelar`) | `ProcesoPagoPrestamoServiceImpl` | **Cambiado**: `setFechaPagado(fechaHora)`, la fecha del evento y del pago. |
| Cancelación del préstamo original en la sustitución (novación / reestructuración, `cancelarOriginal`) | `SustitucionPrestamoServiceImpl` | **Cambiado**: ni ponía `null` ni ponía fecha, así que quedaba nula. Ahora lleva la misma fecha de negocio del evento (`fechaEvento`; hoy si no llega o es la de hoy). |
| Cancelación de las futuras en el pago que salda un préstamo (`ProcesoPagoPrestamoServiceImpl`, ~2008) | idem | **No aplica**: ya ponía `futura.setFechaPagado(fechaHora)` (C45). |
| Abono a capital, motor de pago, Petro, reproceso | `AbonoCapitalPrestamoServiceImpl`, `MotorPagoPrestamoServiceImpl`, `CargaArchivoPetroServiceImpl`, `ReaperturaVigenteAlCorteServiceImpl` | **No aplican**: ninguno deja una cuota en 7. Sus `setFechaPagado(null)` son de cuotas que **vuelven** a pendiente/parcial; Petro y la reapertura solo leen 7 (y tratan igual una 7 con fecha). |
| Carga de cuotas desde Excel (`PrestamoServiceImpl`) | idem | **No aplica**: las cuotas 4/7 ya entran con `fechaPagado = fechaVencimiento`. |

### 2.2 Anulación o reverso
- **Acuerdo y precancelación** (`ProcesoPagoPrestamoServiceImpl.anular…`): la cuota 7 se reconstruía con `motorPagoPrestamoService.recalcularCuotaDesdePagos`, que deja
  `DTPRFCPG = NULL` si la cuota vuelve a pendiente/en mora/parcial, pero **conservaba** la fecha si la reconstruía como pagada. Con la 7 ya fechada eso último sería la
  fecha de la operación que se reversa, y no la del último pago. Se agregó `cuota.setFechaPagado(null)` antes de recalcular: la cuota vuelve a vigente **sin fecha** (que
  era como estaba antes de la operación, porque era pendiente), o con la fecha del último pago si el motor la reconstruye pagada. No se inventa una fecha ni hace falta HDTP:
  estas cuotas no pasan por el historial, se reconstruyen desde sus pagos.
- **Sustitución**: no tiene reverso desde el historial (`"una reestructuración o novación no se anula desde el historial"`), así que no hay cuota que restaurar.

### 2.3 Control previo al cierre de cartera
`ControlCuotasSinFechaService` (crd): cuotas con `DTPRESTD = 7` y `DTPRFCPG IS NULL`, en todos los préstamos (`CRD.PRST` no tiene empresa; como las demás lecturas de
cartera no se filtra por ella). Lista préstamo (operación) y cuotas, acotada a 30 préstamos con el total siempre dicho.
- `/cierrecartera/previsualizar`: una **advertencia** «C48 Hay N cuota(s) … ». Nunca corta.
- `/prdo/controlCierre/{idEmpresa}/{anio}/{mes}`: un control `CARTERA_CUOTAS7_SIN_FECHA`, **ERROR** si hay (valor = cuántas, esperado 0), OK si no.
- `/cierrecartera/ejecutar`: **no bloquea** por defecto. Con la propiedad `saa.cartera.bloqueoCuotas7SinFecha=true` se vuelve precondición bloqueante
  (`IncomeException «CUOTAS_7_SIN_FECHA: no se puede cerrar …»`), junto a las del C35 y la C33, y no en la corrida de arranque. Apagada por defecto.

**Compatibilidad main / prod (semáforo y cierre).** El gancho en `ControlCierreServiceImpl` son 2 bloques (un `@EJB` y una línea `controles.addAll(…)`) junto a la llamada
a `seguimientoNovedadService.controlesCierre`, que existe igual en las dos ramas, sin tocar nada de la C19b (`controlSaldosNegativosB17` queda como está). En
`CierreCarteraServiceImpl` son un `@EJB`, dos llamadas (junto a `avisaPrelacionCuotas` y `exigePrelacionCuotas`, que existen en las dos) y dos métodos privados. La
propiedad vive en la propia interfaz del control, no en `ModoCorteCartera`, que no está en prod. No se tocó `CierreCarteraDaoServiceImpl`.

## 3. Script de datos para producción
`docs/logica-negocio/crd/sql/412_C48_DTPRFCPG_CUOTAS_7_SIN_FECHA.sql` (revisable, **no ejecutado**). Controles antes (por préstamo y mes del evento, y la clasificación
SE TOCA / FUERA), respaldo `CRD.BK_DTPR_FCPG_C48_20261127`, `UPDATE DTPRFCPG` = fecha del evento vigente `PRECANCELACION` o `ACUERDO_CONDONACION`, solo si el préstamo tiene
**exactamente uno** y ningún `NOVACION`/`REESTRUCTURACION` vigente; los demás (más de un evento, ninguno, o cerrados por una sustitución) no se tocan y salen en la lista 0c
para que el cuadre decida. Controles después, `COMMIT` comentado y reverso desde el respaldo comentado.
**Se corre solo con el visto bueno del cuadre**: no es una operación, pero cambia la lectura al corte de meses pasados; no se regeneran las G ya presentadas.

## 4. Archivos
- `AcuerdoCondonacionServiceImpl`, `ProcesoPagoPrestamoServiceImpl`, `SustitucionPrestamoServiceImpl` (los tres motores y el reverso).
- `ControlCuotasSinFechaService` y `ControlCuotasSinFechaServiceImpl` (nuevos); ganchos en `ControlCierreServiceImpl` y `CierreCarteraServiceImpl`.
- `docs/logica-negocio/crd/sql/412_C48_DTPRFCPG_CUOTAS_7_SIN_FECHA.sql`; este doc.
- `src/test/java/com/saa/ejb/crd/service/ControlCuotasSinFechaPruebas.java` y `tools/pruebas/correr-pruebas-c48.sh`.

## 5. Prueba
`mvn -o -q compile`; `sh tools/pruebas/correr-pruebas-c48.sh` (11 verificaciones: texto y acotado de la lista, propiedad apagada por defecto); C33 (109/0) y C46 (30/0) siguen
pasando. La regla de los motores es una asignación, sin lógica que probar aparte.
**Aceptación del cuadre (en su contenedor, no corrida aquí):** en julio el control lista 5 préstamos y 156 cuotas; un acuerdo o una precancelación nuevos dejan 0; anular el
acuerdo devuelve la cuota a vigente con `DTPRFCPG` nulo.

## 6. Pendiente / no verificado
- Nada se corrió contra base. El SQL 412 no está ejecutado.
- Al anular, una cuota 7 que el motor reconstruya como pagada toma la fecha del último pago (comportamiento de siempre); si en la base real hubiera cuotas 7 con pagos
  vigentes, conviene verlas en la aceptación.
