# Lote 15 — D-194 (anticipado ④ vs ⑪), D-195 (mora de préstamos cancelados), D-196 (condonación y mora cobrada): diagnóstico y propuesta

*2026-11-25 · omen-saa-1 · SIN programar: espera el OK del árbitro. Los SQL del cuadre se leyeron de `origin/main` (`docs/logica-negocio/cuadre/`) sin hacer pull. Sin nombres ni montos individuales.*
*SQL nuevos (solo lectura, no ejecutados): `crd/sql/399_DIAGNOSTICO_C27B_ANTICIPADO_4_VS_11.sql`, `400_MEDICION_MORA_DE_PRESTAMOS_CANCELADOS.sql`, `401_MEDICION_CONDONACION_Y_MORA_COBRADA.sql`.*

## D-194 · ④ del 1-nov devengó de menos que lo que el ⑪ del 31-oct pasó a 2.9.01.05

### Cómo es la pareja (leído del código, no del SQL del cuadre)
- **⑪ (al 31-oct)** mueve a 2.9.01.05 el interés cobrado por anticipado: `selectInteresAnticipadoPorTipo` = Σ `PGPRINPG` vigente con `ancla <= PGPRFCHA < corte+1` de las cuotas con vencimiento >= 1-nov. Universo `CONCEDIDO_EN_CARTERA` (préstamo concedido antes del corte y fuera de {0,1,6,7,12,14}: **entran cancelados y novados**), cuota no anulada.
- **④ (del 1-nov)** NO toca 2.9.01.05: devenga D 1.4.02 / H ingreso el interés de las cuotas que vencen en el mes que abre. El ⑪ del cierre siguiente libera D 2.9.01.05 / H 1.4.02 lo que ya no es «por vencer». Para que 1.4.02 no quede acreedor, **por cada cuota de noviembre ④ tiene que ser >= lo cobrado por anticipado**.
- Regla del ④ (`selectInteresDevengoPorTipoEnRango`, corte = 31-oct, ancla = 31-may): cuota de préstamo «cancelado terminado al corte» O estado 7 → **solo lo cobrado** (`post`); si no y es de plazo vencido condonado (PLVN) → **0**; si no → `GREATEST(DTPRINTR − pre, 0)` (`pre` = cobrado antes del ancla).

### Comparación cuota por cuota ④ contra ⑪
| | ⑪ | ④ (código) | emulación del cuadre (bloque 04) |
|---|---|---|---|
| Pagos | `PGPRINPG`, ancla <= fecha < corte+1 | `post` igual; `pre` = antes del ancla | `ant`: `INPG + INVP`, ancla <= fecha < 1.º del mes de vencimiento |
| Universo de préstamos | concedido y fuera de {0,1,6,7,12,14}; cancelados entran | igual | `lu.u`: igual **pero excluye** el cancelado terminado (`dev = 0`) |
| Cuota estado 7 | cuenta lo cobrado | solo lo cobrado = ⑪ | sale (`dev = 0` si pagada antes del mes) |
| Plazo vencido condonado | cuenta lo cobrado | **0** | `dev = 0` |
| Cuota cobrada de más (cobrado > interés) | todo lo cobrado | `DTPRINTR − pre` < lo cobrado | `GREATEST(intr − iV, 0)` |
Conclusión de lectura: **las ramas del ④ nunca devengan menos que el ⑪ salvo dos: (i) plazo vencido condonado (0) y (ii) cuotas cuyo interés actual (`DTPRINTR`) es menor que lo cobrado** (cuota reescrita por abono, novación o reestructuración después del cobro). Las cuotas de estado 7 y las de préstamos terminados dan exactamente `post` = lo que mueve el ⑪.

### Hipótesis sobre el faltante (se confirman con el SQL 399)
1. **(ii) cobrado mayor que la cuota** (rama C del 399): la más probable con «cobros del 31-oct» y préstamos que cambiaron en octubre.
2. **(i) plazo vencido condonado** (rama B).
3. **Estado de HOY, sin fecha:** `DTPRESTD = 7` y `CANCELADO_TERMINADO_AL_CORTE` se evalúan con los datos de hoy. En un **reproceso**, una cuota que un acuerdo, una precancelación, una novación o una reestructuración cerró DESPUÉS del corte se trata como cancelada al corte y el ④ devenga solo `post`; en tiempo real, al 1-nov esa cuota estaba viva y se devengaba completa. El 399 la marca como `A2` (evento vigente posterior al corte). Esto no hace ④ < ⑪ en la partida, pero sí deja sin devengar el interés no cobrado de esas cuotas, y el asiento del evento (`lineasReversoCuotasCanceladas`, Lote 1b) después lo reversa suponiendo que se devengó: es una **diferencia entre reproceso y tiempo real**, no un defecto de producción. Lo mismo vale para el `CANCELADO_TERMINADO_AL_CORTE` con `PGPR.PRSTCDGO` nulo (el guardia C27-C lee pagos por préstamo; ver `sql/396`).
**No pude correr nada contra la base**: la pista del líder (cuotas de préstamos que cambiaron entre el 31-oct y el 1-nov, cruce del 31-oct) cae en las ramas A2, B y C. Primero hay que correr el 399; con su BLOQUE 1 se compara `anticipado_11` contra lo que mide la conciliación y `devengo_4` contra lo que devengó el ④ del 1-nov; el faltante por rama sale del BLOQUE 2/3.

### Propuesta de corrección (independiente de cuál rama pese)
- **Garantía estructural:** en `selectInteresDevengoPorTipoEnRango` (y `selectInteresPrimeraCuotaPorTipo`) el valor por cuota pasa a `GREATEST(regla actual, post)`: el ④ **nunca devenga menos de lo cobrado** de la cuota. Con esa regla, el ⑪ siguiente libera exactamente lo que el ④ devengó y 1.4.02 no queda acreedor, cualquiera que sea la causa (condonado, reescrita, cancelada). Es una línea por consulta; no cambia los casos en que ④ ya era >= lo cobrado, que son todos menos los del faltante.
- **Solo en el reproceso (opcional, aparte):** estado de la cuota «al corte»: tratar como viva (pendiente) la cuota estado 7 cuyo préstamo tiene un evento de cancelación vigente con fecha posterior al corte. Necesita decidir cómo conocer la fecha de cierre de la cuota (no hay columna: se usa el evento del préstamo); riesgo medio; no aplica a producción en tiempo real.
**Efecto sobre lo ya cerrado:** nada se reescribe; el cierre de noviembre ya ejecutado se corrige con el asiento de regularización del cuadre por la diferencia medida (o repitiendo el ④ con la regla nueva si el proceso lo permite). **Tamaño: chica** (2 consultas en `CierreCarteraDaoServiceImpl`) + prueba con el 399 antes y después (faltante_4 = 0). **Riesgo: medio** (cierre contable).

## D-195 · mora devengada y no cobrada de un préstamo que se cancela: ¿quién la reversa?

### Medición en código (hoy)
| Flujo que cancela | ¿Reversa la mora devengada no cobrada de 1.4.02 y de 1.4.99.05? |
|---|---|
| Motor / Petro (la cuota queda pagada) | **Sí, solo por cuota y solo si pagó sin mora** (C43 `reversarMoraNoCobrada`, origen `MORA_NO_COBRADA`: D 5.1.02 / H 1.4.02 y la provisión). |
| Precancelación | **No.** `reversarPorPagos` reversa la provisión de lo PAGADO; `lineasReversoCuotasCanceladas` reversa el devengo del interés ORDINARIO de las cuotas pasadas a 7. La mora devengada pendiente no se toca. Con `saa.cartera.moraNoCobradaHasta` el desglose no cobra mora antes de esa fecha (reproceso: 2026-05-01), así que queda toda sin reversar. |
| Acuerdo de condonación | Solo la mora CONDONADA: `condonarProvision` (provisión, acotada al saldo provisionado) y el asiento del acuerdo (1.4.02 contra el gasto; ver D-196). Lo pagado y lo condonado suman lo adeudado: no queda mora sin tratar, pero el tratamiento de la condonada es el del D-196. |
| Novación / reestructuración | **No.** `SustitucionPrestamoServiceImpl` no tiene ninguna llamada de reverso de mora. El préstamo viejo se cancela por la VS del desglose de precancelación (con las mismas reglas de mora no cobrable). |
| Cobro tardío (CBCR) | Sí, el exceso (`reversarExcesoProvisionPorCobroTardio` / `reversarExcesoDevengoPorCobroTardio`), no la cancelación. |
**Respuesta: hoy nadie reversa la mora devengada y no cobrada al cancelar por precancelación, novación ni reestructuración**, y en el acuerdo solo la condonada. La mide el SQL 400 (BLOQUE 1: por flujo y tipo; BLOQUE 4: qué orígenes de reverso existen en MVIC; BLOQUE 3: las operaciones que el cuadre señaló, que se ponen en la lista).

### Propuesta (aprobada por el líder: en el mismo asiento del evento, como C43)
- Servicio nuevo `ReversoMoraCancelacionService` (CRD, junto a `DevengoCompletoService`): `lineasReversoMoraNoCobrada(idEmpresa, prestamo, fechaEvento, prefijo)` calcula, **después de lo que el evento cobra y condona**, la mora contabilizada pendiente del préstamo (la regla por préstamo del 7b: devengo MVIC 5−6 − mora cobrada − mora condonada de los acuerdos APLICADOS) y el saldo provisionado de mora (MVIC 1,4 − 2,3,9,10), y devuelve las líneas del asiento del evento: **D 5.1.02 / H 1.4.02** (plantilla 17, `INGRESO_INTERES_MORA` y `INTERES_MORA_POR_COBRAR`, por tipo) y **D 1.4.99.05 / H 4.7.05.10** (plantilla 36, papeles 81 y 80, por tipo). «Si cobra mora, solo se reversa lo que queda sin cobrar» sale solo de la fórmula.
- MVIC: al grabar el asiento se registran filas tipo 6 (reverso de devengo, componente mora) y tipo 3 (reverso de provisión) por cuota (acotadas al neto de la cuota, o repartidas del más viejo al más nuevo como el reparto del 7b), con origen `CANCELACION_MORA`, `idOrigen` = el evento y `ASNTCDGO` del asiento; `anularPorEvento(origen, idEvento)` las anula cuando el evento se anula (se engancha en `anularOperacion` / `contabilizarReverso`, que ya reversan el asiento).
- Se llama desde: `contabilizarPrecancelacion` (con y sin aportes, junto a `lineasReversoCuotasCanceladas`), `aplicarAcuerdo` (junto a `lineasReversoDevengo`, después de calcular lo condonado) y los asientos de novación y reestructuración de `SustitucionPrestamoServiceImpl`.
- Los tres préstamos que señaló el cuadre (sus operaciones van en la lista del BLOQUE 3 del SQL 400) se revisan con el BLOQUE 3 del 400 antes de programar (qué flujo los canceló y por qué la 391 los muestra).
**Efecto:** las cancelaciones nuevas dejan 1.4.02 y 1.4.99.05 sin mora del préstamo; el exceso que muestra la 391 por este concepto deja de crecer. Lo ya cancelado (noviembre y antes) se regulariza con un script/asiento del cuadre: el 400 da la base. **Tamaño: media-grande** (servicio nuevo + 4 enganches + anulación + doc). **Riesgo: alto** (asientos de cuatro flujos). Prueba en copia: precancelar un préstamo con mora devengada y no cobrada y comprobar mora = 0 en 1.4.02 y 1.4.99.05; anular el evento y comprobar el reverso.

## D-196 · 3.3(c): la condonación acredita 1.4.02 por mora que nunca se devengó

### Medición en código (hoy)
`generarAsientoCondonacion`: H capital por banda; **H una sola línea de «interés condonado (ordinario y mora)»** por tipo contra 1.4.02 (`lineaInteres` de la plantilla de aplicación) por `interesCondonado` = ordinario + mora completos; D línea 70 (gasto, 4.8.90.90.45) por el total. No separa devengado de no devengado ni en la mora ni en el interés ordinario.
- **Mora:** el problema pedido. La mora condonada no devengada acredita 1.4.02 (queda acreedor) y carga el gasto por mora que nunca fue ingreso.
- **Interés ordinario:** el acuerdo condona el de cuotas exigibles (vencidas), que el ④ del mes en que vencieron ya devengó; las cuotas futuras se cierran en 7 sin condonarse (el desglose solo cuenta su capital). **No tiene el mismo problema salvo** cuotas del mes en curso cuyo ④ aún no corrió o, en un reproceso, cuotas cuyo ④ las vio ya en estado 7 (hipótesis 3 del D-194). El BLOQUE 1b del 401 acota cuánto interés de cuotas de meses posteriores se condonó.

### Propuesta (aprobada)
- En `generarAsientoCondonacion`: mora devengada = `min(moraCondonada, max(0, pendienteContabilizadaDelPrestamo(fechaAcuerdo, excluyendo este acuerdo)))` con la regla del 7b. **H 1.4.02 = interés ordinario condonado + mora devengada; D línea 70 = ese total.** La parte no devengada **no genera ninguna línea**.
- Hace falta un método de DAO por préstamo y fecha (`selectMoraContabilizadaPendienteDelPrestamo(idPrestamo, fecha, excluirAcuerdo)`): el acuerdo ya está APLICADO cuando se arma el asiento (paso 5 antes del 6), así que su propio DACC hay que excluirlo.
- `condonarProvision` (provisión) no cambia. MVIC tipo 9 y la regla del 7b (DACC) quedan igual: lo condonado no devengado nunca estuvo en el devengo y no resta nada.
**Efecto:** los acuerdos nuevos dejan de acreditar 1.4.02 por mora no devengada; el asiento manual mensual del cuadre (D 1.4.02 / H 4.8.90.90.45) deja de hacer falta para ese concepto. Lo ya aplicado se regulariza por el cuadre con la base del 401 (BLOQUE 1a: «parte no devengada acreditada de más»). **Tamaño: media** (1 método de DAO + `AcuerdoCondonacionServiceImpl`). **Riesgo: medio-alto** (asiento del acuerdo).

### La parte 5.1.02 del asiento manual: mora COBRADA por encima de lo devengado
El líder dijo que no sale de devengo y condonación en el mismo mes sino de mora cobrada por encima del devengo del préstamo que acreditó 1.4.02 en lugar de ir a ingreso, y que habría un camino de cobro que no pasa por `separarMoraNoDevengada`.
**Lo que dice el código:** *todos* los caminos de contabilización de un cobro llaman a `separarMoraNoDevengada`: `haberDesdePagos` (CBCR/`CobroCreditoServiceImpl`, `contabilizarPagoConAportes`, abono a capital, precancelación y precancelación con aportes de `ContabilidadPrestamoServiceImpl`, el cruce de aportes del acuerdo) y, directamente, `CobroPetroContableServiceImpl`. **No encontré un camino que acredite mora a 1.4.02 sin pasar por ahí**, así que la sospecha del cuadre (cruce con aportes y precancelación con aportes) no se confirma por lectura. Dos debilidades de la propia función sí explican un excedente sin separar:
1. **Sin límite de fecha:** el «reconocido» de la cuota (`selectSaldoDevengadoPorCuotas`) suma TODO el MVIC 5−6 de la cuota, también el devengo posterior a la fecha del pago. En un reproceso (o con un cobro tardío) la mora «parece» devengada cuando al pagar no lo estaba, y va a 1.4.02 en lugar de a ingreso.
2. **Por cuota, no por préstamo:** compara cada pago contra el devengo de SU cuota; si la mora de una cuota se cobra en otra, o si el devengo de la cuota se reversó después, el exceso no se separa.
**Medición (BLOQUE 2a/2b del 401):** por pago de mora del mes, el excedente esperado y si existe el MVIC 5 de origen `COBRO` que `separarMoraNoDevengada` graba; los que tienen excedente y no tienen ese MVIC son los que no pasaron por la separación, agrupados por tipo de pago y camino (Petro, evento, CBCR). Con eso se sabe si es un camino faltante o una de las dos debilidades.
**Propuesta para que todo cobro de mora pase por la separación (cuando el 401 confirme):** (a) limitar el reconocido a `MVICFCCT <= fecha del pago` (`selectSaldoDevengadoPorCuotasAl(fecha)`); (b) si el 401 muestra un camino concreto, llamar a `separarMoraNoDevengada` desde él; (c) un chequeo de cierre: «mora cobrada > devengada» por préstamo debe ser 0 o avisar.

## Orden sugerido y qué necesito
1. **Correr los SQL** (el usuario o el cuadre): el 399 (BLOQUES 1–3) en el reproceso, el 400 y el 401. Con eso se escoge entre las ramas del D-194 y se confirma el origen del cobro sin separar.
2. D-194 (chica) y D-196 condonación (media) pueden ir en paralelo; D-195 (grande) después del 400.
3. **OK del árbitro** a: la garantía `GREATEST(regla, post)` del ④; el servicio de reverso de mora y su enganche en los cuatro flujos; la separación devengada/no devengada del asiento de condonación; y el límite de fecha en `separarMoraNoDevengada`.
