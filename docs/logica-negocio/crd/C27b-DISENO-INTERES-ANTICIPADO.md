# C27b — interés cobrado antes de su devengo: diagnóstico y diseño (D-172)

**omen-saa-1-arb, 2026-11-24.** Encargo del proyecto de cuadre («PROMPT-ARBITRO-C27b», D-172). Estado: **DISEÑO PARA
APROBACIÓN — sin código todavía.** El encargo pide diagnóstico y diseño aprobados antes de escribir código.

## 1. ¿Ya está hecho? No.

Verificado contra `origin/main` `27d3f835` (fetch del 24-11):

| Punto del encargo | En `main` | Evidencia |
|---|---|---|
| Causa 1: el devengo resta lo ya cobrado | **Sí, el defecto está** | `CierreCarteraDaoServiceImpl.selectInteresPorTipoPrestamoEnRango` (l. 344): `GREATEST(NVL(d.DTPRINTR,0) - NVL(g.intr,0), 0)`. Además, `pendiente(corte)` **saca la cuota pagada**, así que una cuota pagada por anticipado ni siquiera entra al paso ④. Es peor que restar. |
| Causa 2: la primera cuota del mes de concesión | **Sí, el defecto está** | Al corte, `universo(corte)` usa `PRSTFCHA < :corteSig`. En modo normal usa `PRESTAMOS_VIVOS` = estados de **hoy** (ver §3.1: eso da un doble conteo con el arreglo). |
| Causa 4: producción, `PAGOS_VIGENTES` sin fecha | **Sí, el defecto está** | `PAGOS_VIGENTES` (l. 97) no filtra por `PGPRFCHA`. |
| C42: `corteInclusivo(explicito, corte)` y `saa.cartera.corteExclusivoDesde` | **NO está en `main`, ni en ninguna rama remota** | Solo existe `corteInclusivo(Boolean)`, que por defecto toma `true`: la cuota que vence el día del corte cuenta como **vencida**, al revés de la regla del encargo. No hay rama `cuadre/c39-validaciones-g` en `origin`. |
| Commits C27b, C42 o D-172 | Ninguno | `git log --all --grep`. Solo existen C27-B y C27-B2, que son otra cosa: abonos posteriores al corte en las bandas. |
| `C27b-INTERES-ANTICIPADO.md` y `plantilla-conciliacion-1402.sql` | **No están** | Ni en saaBE ni en `F:\work\cuadreSuper`. Los tiene que entregar el cuadre. |

Pasos del cierre hoy (`SubProcesoCierreCartera`): ①=1, ②=2, ①.1=3, ③=4, ④=5 (devengo, plantilla 17), ⑥=6, ⑦=7, ⑧=8,
⑨=9 (ajuste de bandas a saldo) y ⑩=10 (mora al corte).
- El encargo dice «paso 9 de provisión», pero en el código el ⑨ es el **ajuste de bandas**.
- La referencia de estilo para el ajuste a saldo es justamente `armaAjusteBandas` (`CierreCarteraServiceImpl` l. 1338).

## 2. Impacto

- **Contable, en todos los meses futuros.** Sube el ingreso 5.1.02.xx y 1.4.02.xx queda igual a la G48 más la mora.
  Aparece un saldo en 2.9.01.05 (o en la subcuenta que se decida).
- **No cambia:** G48, G49, CCPM, la provisión ⑦ y ⑧, las bandas, los cobros de Petro, Arch y ventanilla, ni los cruces.
- **Doble contabilización.** El reproceso carga un asiento manual mensual desde oct-2025. Si el código nuevo cierra un mes
  que ya lleva ese asiento manual, se contabiliza dos veces. Por eso el plan de regularización (§5) es condición del pase.
- **Lo que se toca:**
  - `CierreCarteraDaoServiceImpl` y su interfaz;
  - `CierreCarteraServiceImpl`: el paso ④ y dos pasos nuevos;
  - `SubProcesoCierreCartera`: los códigos 11 y 12;
  - `PlantillasCredito`: un alterno nuevo;
  - los tres «menores» en `AbonoCapitalPrestamoServiceImpl`, la precancelación y `AcuerdoCondonacionServiceImpl`.

  Todo es `crd`, mi alcance. El «pagos» que no me toca es PGS de tesorería, no los pagos de préstamo.
- **SQL de pase:** solo la semilla de la plantilla nueva (y la subcuenta, si el contador la pide). No hay DDL.

## 3. Diseño («alternativa C», con tres ajustes del árbitro)

### 3.1 Paso ④ — devengo del mes que abre (fechado el día 1 del mes que abre, plantilla 17)

Se crea un método DAO nuevo: `selectInteresDevengoPorTipoEnRango(desde, hasta, corte, ancla)`. El viejo queda solo para
la mora.

Por cada cuota con `DTPRFCVN` en el mes que abre:
- **Préstamo en el universo al corte** (`PRSTFCHA < :corteSig`, sin anulados y sin cancelados antes del corte):
  `DTPRINTR` − condonado − interés cobrado con `PGPRFCHA < ancla`.
- **Préstamo que salió del universo** (cancelado, precancelado o novado; no anulado): solo el interés **cobrado** vigente
  de esa cuota, con `PGPRFCHA ≥ ancla`. ⚠️ **Ajuste 1 al encargo:** devengar el `DTPRINTR` completo de un préstamo
  precancelado devengaría interés que nunca se va a cobrar.
- **Sin el filtro `pendiente`:** la cuota pagada entra.
- **Ajuste 2:** el universo es **por fecha también en modo normal** (`PRSTFCHA < :corteSig`). Con `PRESTAMOS_VIVOS`, un
  préstamo concedido entre el día 1 y el día en que se corre el cierre devengaría su primera cuota en ④ y otra vez en
  ④b del mes siguiente.
- **Ancla:** el `fechaCorte` de la corrida de **arranque** EJECUTADA de la empresa. En la corrida de arranque se sigue
  restando todo lo cobrado antes del corte, que ya está en el saldo de apertura.
- La mora del paso ④ no cambia.

### 3.2 Paso ④b nuevo — primera cuota (código 11, fechado al corte del mes que se cierra, plantilla 17)

- Toma las cuotas que vencen en el mes que se cierra, de préstamos con `PRSTFCHA` dentro de ese mismo mes, sin anulados.
- El asiento es D 1.4.02.xx / H 5.1.02.xx por `DTPRINTR` completo, por tipo de préstamo.
- Si el préstamo se canceló ese mismo mes, se usa la regla de §3.1: solo lo cobrado.

### 3.3 Paso ⑪ nuevo — intereses recibidos por anticipado (código 12, fechado al corte, plantilla nueva)

- **Objetivo por tipo de préstamo:** Σ `PGPRINPG` vigente con `ancla ≤ PGPRFCHA < :corteSig`, de cuotas con
  `DTPRFCVN ≥` día 1 del mes siguiente. El universo es el de §3.1, sin anulados.
- **Asiento:** objetivo − saldo actual de la cuenta de la plantilla, como ajuste a saldo al estilo de `armaAjusteBandas`.
  - Si el objetivo sube: D 1.4.02.xx / H 2.9.01.05(.xx).
  - Si baja: al revés.
- **Anulación de un cobro anticipado después del cierre:** el reverso vuelve a debitar 1.4.02 y el ⑪ del mes siguiente
  baja el objetivo. Se corrige solo, por saldo.
- **Ajuste 3:** las cuentas salen de **líneas de plantilla** por tipo de préstamo, nunca del código. Está pendiente la
  decisión del contador (§6-D2).

### 3.4 Menores (en el motor, al momento del hecho)

- **Abono a capital:** al re-amortizar, por la baja de `DTPRINTR` de cuotas con vencimiento en un mes **ya abierto**
  (devengado en ④), se registra D 5.1.02.xx / H 1.4.02.xx en el mismo asiento del abono. Caso 66940, 84,75.
- **Precancelación y acuerdo de condonación:** por el interés de la cuota del mes en curso que ya se devengó y no se cobra
  ni se condona contra ingreso, se registra D 5.1.02 / H 1.4.02.
- Se hace con líneas de plantilla nuevas o existentes de esos procesos. El BE mide primero cuáles hay.

### 3.5 Lo que **no** depende de C42

- ④, ④b y ⑪ trabajan con **rangos de mes** (`DTPRFCVN` entre el día 1 y el último día, y `≥` día 1 del siguiente). No
  usan `corteInclusivo`.
- La cuota que vence el último día del mes es del mes y se devenga en el ④ de ese mes, con cualquier convención.
- **C42 solo afecta la prueba de aceptación 1** (comparar contra el interés vencido de la G48). Por eso el código puede
  avanzar sin C42, pero la conciliación al centavo se valida con C42 integrado.

### 3.6 Re-ejecución

- `ejecutar` exige reversar la corrida EJECUTADA anterior (l. 378, C17), y el reverso anula sus asientos.
- La idempotencia de ④b y ⑪ viene de ahí. Además, ⑪ es ajuste a saldo.
- El BE verifica que no se pueda tener dos corridas EJECUTADAS del mismo mes.

## 4. Pruebas

Son las 8 del encargo. Las que corren sobre la base van **sobre una copia de producción**; nunca en producción.
- `src/test` está vacío y no hay framework de pruebas.
- La prueba 8 (unitarias) se cubre con un SQL de medición de solo lectura por caso borde:
  - cuota que vence el día del corte;
  - cuota pagada dos meses antes;
  - préstamo cancelado después de pagar cuotas futuras;
  - cobro anticipado anulado.

  El SQL compara el objetivo de ④, ④b y ⑪ contra el cálculo a mano. Si el cuadre exige JUnit, es una decisión aparte
  (§6-D5).

## 5. Regularización (coordinar con el líder del cuadre)

- **Opción A (recomendada):** un asiento acumulado en el primer mes que se cierre con el código nuevo, por la diferencia
  1.4.02 − (G48 + mora + arranque + residuo) **neta de los asientos manuales ya cargados**. Desde ese mes se dejan de
  cargar los manuales.
- **Opción B:** re-ejecutar los cierres desde jun-2025. Hay que reversar cada corrida y su asiento manual. Es más limpio,
  pero arrastra los cuadres 21=G42 y 13−1399=G48 ya conciliados.

## 6. Decisiones antes de despachar al BE

| # | Decisión | Quién |
|---|---|---|
| D1 | Aprobar el diseño, con los ajustes 1 y 2 | usuario / cuadre |
| D2 | Cuenta del ⑪: subcuenta propia de 2.9.01.05 por tipo (Q, P, H) o 2.9.01.05 compartida con las inversiones, separada por plantilla | contador |
| D3 | Regularización A o B | usuario con el líder del cuadre |
| D4 | C42: entregar la rama o el commit. Sin ella, la prueba 1 no se puede cerrar al centavo | cuadre |
| D5 | Aceptar el SQL de medición como prueba 8, o exigir JUnit (habría que montar el framework, que hoy no hay) | usuario |
| D6 | Entregar `plantilla-conciliacion-1402.sql` y `C27b-INTERES-ANTICIPADO.md` | cuadre |

## 7. Lote 16: el límite del devengo se mide al día de la operación
- **Defecto.** `DevengoCompletoServiceImpl.limiteDevengado` tomaba el corte máximo de todas las corridas EJECUTADAS de la empresa. En una base con cierres posteriores a la operación (la copia de trabajo con los cierres de jul a sep de 2026, o producción tras el reemplazo), el límite quedaba en el fin del mes siguiente a ese último corte, y los reversos de abono, precancelación y acuerdo reversaban interés de cuotas futuras que nunca se devengaron.
- **Regla nueva.** `limiteDevengado(idEmpresa, fechaOperacion)` solo considera corridas EJECUTADAS con `fechaCorte` estrictamente anterior a `fechaOperacion`. El resto no cambia: el corte tiene que ser `>= devengoCompletoDesde` y el límite es el último día del mes siguiente a ese corte. Con `devengoCompletoDesde = 2025-10-31`, una operación de octubre de 2025 no tiene límite (no reversa nada), y una de noviembre o diciembre solo alcanza las cuotas del mes de la operación.
- **Llamadores y fecha que usan.** `lineasReversoAbono` recibe ahora la fecha de la operación; `lineasReversoCuotasCanceladas` ya traía `fechaEvento`.
  - Abono a capital (`ContabilidadPrestamoServiceImpl.lineasReBandeoAbono`, usada por `contabilizarAbonoCapital` y `contabilizarAbonoCapitalConAportes`): fecha del evento (`evento.getFecha()`).
  - Precancelación por evento directo (`ContabilidadPrestamoServiceImpl`, dos métodos): fecha de pago del contexto (`ctx.getFechaPago()`).
  - Precancelación por cobro (`CobroCreditoServiceImpl`): fecha del cobro (`cobro.getFecha()`).
  - Acuerdo (`AcuerdoCondonacionServiceImpl`): fecha del acuerdo (`fecha`).
- **Otras lecturas del «último cierre ejecutado».** No hay otra que decida qué se devengó sin acotar por fecha: `selectUltimaEjecutadaAntesDe` (bandas, reclasificación) ya acota por período anterior; `validaAnclaArranque` compara con el corte exacto del ancla; la guarda de anulación de préstamos en `PrestamoServiceImpl` usa el período de la aprobación; los reportes consultan por período.

## 8. Lote 19 — libro por cuota del devengo de INTERÉS (nivel 1.4 y A2): medición, diseño y lo entregado

### 8.1 Medición (2026-11-25, por lectura del código)
- **Tipos de MVIC** (`TipoMovimientoInteresCuota`): 1 PROVISION, 2 REVERSO_POR_COBRO, 3 REVERSO_POR_COBRO_TARDIO, 4 RE_PROVISION, 5 DEVENGO_MORA, 6 REVERSO_DEVENGO_POR_COBRO_TARDIO, 7 CLASIFICACION_CAPITAL, 8 RECLASIFICACION_POR_COBRO_TARDIO, 9 REVERSO_POR_CONDONACION, 10 AJUSTE_TOPE_PROVISION. Componentes: 1 INTERES, 2 MORA, 3 CAPITAL. Admiten INTERES hoy los tipos 1, 2, 4 y 9 (la provisión y sus reversos); los 5 y 6 son solo MORA y los 7 y 8 solo CAPITAL. Ningún tipo registra el DEVENGO de interés: el interés devengado solo existe como asiento.
- **Dónde se devenga el interés:** ④ (`armaDevengoIntereses`), un asiento por tipo de préstamo con la plantilla 17 (INTERES_ORDINARIO_POR_COBRAR / INGRESO_INTERES_ORDINARIO), fechado al primer día del mes que abre, con dos consultas agregadas por tipo según el modo: `selectInteresPorTipoPrestamoEnRango` (de siempre) y `selectInteresDevengoPorTipoEnRango` (devengo completo, con ancla, pagos antes y después de ella). ④b (`armaPrimeraCuota`, `selectInteresPrimeraCuotaPorTipo`), fechado al corte. ⑪ (`armaInteresAnticipado`) no devenga: pasa a un pasivo el interés cobrado antes de que abra su cuota, y su asiento es la diferencia contra lo ya asentado, sin cuota. De los tres, ④ y ④b se graban por tipo de préstamo y el único detalle por cuota que se guarda de ④ es el de la mora (tipo 5).
- **Reversos del devengo de interés (1.4.02):**
  - Abono: `DevengoCompletoService.lineasReversoAbono` da UN total por préstamo (Σ interés de las cuotas historizadas menos Σ de las nuevas, de los meses ya devengados); las cuotas viejas y las nuevas son filas distintas, así que no hay una cuota «de origen» por cuota.
  - Precancelación (tres llamadores) y acuerdo: `lineasReversoCuotasCanceladas`, que calcula el no cobrado cuota por cuota (sí hay cuota) y devuelve solo las dos líneas del asiento.
  - Anulación: anular el asiento reversa la contabilidad; el libro por cuota (que hoy no existe para el interés) no tiene nada que anular.

### 8.2 Diseño
- Tipos 11 DEVENGO_INTERES y 12 REVERSO_DEVENGO_INTERES, componente 1, valor positivo, con el `ASNTCDGO` del asiento que mueve la 1.4.02 (misma regla del libro de la mora y de la provisión). Anulación igual a la de los MVIC de provisión: por corrida (`anularByCorrida`) para los del ④, y por evento/origen (`anularByOrigenYIdOrigen`) para los reversos, que hoy ya anulan los del reverso de mora.
- ④/④b: grabar, desde la misma lista por cuota de la que sale el asiento (como el ⑦ del lote 18), un tipo 11 por cuota. Los ④ y ④b de hoy son SQL agregado por tipo con ramas de condonado, cancelado al corte y cuota cerrada; hay que replicar CADA rama por cuota. Y el asiento debe armarse como la suma redondeada de las cuotas, no como el SQL agregado de hoy, para que sea «al centavo».
- Reversos: precancelación y acuerdo graban un tipo 12 por cuota (ya calculan por cuota); el abono necesita una regla de reparto del total entre las cuotas del préstamo (propuesta: por cuota nueva con el mismo número que la historizada).
- Regla de oro y su control: `sql/407_CONTROL_LIBRO_DEVENGO_VS_1402.sql` (solo SELECT) compara por asiento Σ(11 − 12) + Σ(5 − 6) del libro contra el movimiento de la 1.4.02 de ese asiento, con el cruce mensual por tipo y el saldo al corte.
- A2 (semilla al 30-nov-2025): `POST /rest/prov/semillaDevengoInteres` con `grabar=false` para previsualizar e idempotente por el asiento de referencia (como `/prov/ajusteTopeMora`). «Devengado y no cobrado al corte» por cuota = `interés de la cuota − interés cobrado con fecha efectiva ≤ corte`, mínimo 0, para las cuotas vencidas impagas y la que vence el día del corte y no se pagó; se excluyen las cerradas (estado 7) y las anuladas. Graba un tipo 11 por cuota, sin asiento contable, ligado al asiento D206. La previsualización devuelve la suma por tipo de préstamo.

### 8.3 Entregado en este lote y lo que falta
- Entregado: las constantes 11 y 12, `sql/406` (CK_MVIC_TIPO hasta 12; no ejecutado), `sql/407` (control, solo SELECT; no ejecutado) y este diseño.
- **No** se tocaron el ④, el ④b ni los reversos, y A2 no está: el ④ diario es el paso más delicado del cierre (SQL agregado con cuatro ramas de interés, que hay que reescribir por cuota y reconciliar al centavo con el asiento). Se entrega primero la medición y el diseño para decidirlo antes de escribir.

## 9. Lote 20 — el ④, el ④b y los reversos de la 1.4.02 graban el libro de interés (MVIC 11 y 12)

> ### ⚠ El `sql/406` va ANTES del EAR
> Con el devengo completo activo, el cierre (④ y ④b) y cada reverso (abono, precancelación, acuerdo) graban filas con `MVICTPMV` 11 o 12. Sin el `406`
> (que amplía `CK_MVIC_TIPO` a 1..12) el INSERT revienta con ORA-02290 y, en el cierre, aborta la ejecución. Sin devengo completo activo no se graba nada
> de esto y el cierre es el de siempre.

### 9.1 ④ y ④b (devengo)
- Consulta por cuota por variante, copia de la agregada sin `SUM` ni `GROUP BY` (mismas ramas: condonado, cancelado al corte, cuota cerrada, pagos antes y después del ancla): `CierreCarteraDao.selectInteresDevengoPorCuotaEnRango` (④ con el devengo completo) y `selectInteresPrimeraCuotaPorCuota` (④b). La variante «de siempre» (`selectInteresPorTipoPrestamoEnRango`) solo rige sin devengo completo, donde no se graba el libro de interés y no se toca; la mora del ④ sigue saliendo de ella.
- El asiento por tipo es la suma de los valores por cuota redondeados a 2 decimales (se aceptan centavos de diferencia contra el agregado histórico). De esa misma lista, `generaAsiento` graba un MVIC tipo 11 por cuota (componente interés, fecha contable del asiento, corrida, origen CIERRE, mismo asiento).
- Anular el cierre: `anularByCorrida` ahora incluye el tipo 11 (antes 1, 5 y 7).
- Comprobación de equivalencia: `sql/408` (solo SELECT, en una copia) compara por tipo el agregado contra la suma por cuota redondeada, con las constantes expandidas, y después de un cierre compara el libro grabado contra el DEBE de la 1.4.02 del asiento.

### 9.2 Reversos (MVIC 12, componente interés)
- `DevengoCompletoService.calcularReversoCuotasCanceladas` (precancelación en sus tres llamadores y acuerdo) y `calcularReversoAbono` devuelven `ResultadoReversoDevengo`: las líneas del asiento y el detalle por cuota. Después de generar el asiento, cada llamador llama a `registrarReverso(resultado, idAsiento, ...)`, con el patrón de `ReversoMoraCancelacion.registrar`.
- Cuotas canceladas: valor por cuota = `max(interés − cobrado, 0)` redondeado por cuota; el asiento es la suma de esos valores. Origen `CANCELACION_DEVENGO_INTERES`, idOrigen = el evento (el cobro de la precancelación pasa el evento de cada préstamo).
- Abono: por número de cuota, interés historizado − interés nuevo cuando da positivo (sobre cuotas de meses ya devengados); el residuo de las cuotas que desaparecen va a la cuota nueva de número mayor que queda dentro del límite; el total del libro se ajusta a esa cuota para ser igual, al centavo, al reverso del asiento. Origen `ABONO_DEVENGO_INTERES`, idOrigen = el evento del abono.
- Anular: `DevengoCompletoService.anularPorEvento(idEvento)` (por origen e idOrigen) se llama donde ya se anulaba el reverso de mora: la anulación del cobro (dos sitios) y `contabilizarReverso`.
- Todo sigue condicionado a `devengoCompletoActivo` (a través del límite del lote 16): sin él, ninguna lista tiene cuotas y no se graba nada.

### 9.3 Pendiente
A2 (semilla al 30-nov) y A1 van juntos en el lote 21. Sin probar contra la base; `sql/407` y `sql/408` sin ejecutar.
