# C52 — Saldo contrario a la naturaleza POR CUENTA (D-150, D-222)

Equipo omen-saa-1 · 2026-11-28 · lote 31, punto 2.5 del encargo de controles de producción. Va a `ear/siguiente` por cherry-pick: **no usa el motor de balances de la C19b** (ni DTRP ni TempReportes): lee `CNT.DTAS` / `CNT.ASNT` / `CNT.PLNN`.

## 1. Regla (del cuadre) — `ReglaSaldoContrario`
Cuentas de **movimiento** (`PLNNTPOO = 2`), solo asientos vigentes (estados 1 y 3), saldo = debe − haber, tolerancia 0,005.
| Tipo | Saldo | Clase | Es contrario si |
|---|---|---|---|
| BALANCE | acumulado al corte | 1 activo | saldo **acreedor**, salvo provisión/valuación |
| BALANCE | acumulado al corte | 2 pasivo, 3 patrimonio | saldo **deudor** |
| RESULTADOS | acumulado del año hasta el corte, sin el cierre del ejercicio | 4 gasto | saldo **acreedor** |
| RESULTADOS | idem | 5 ingreso | saldo **deudor** |
Cuentas de orden (6, 7): fuera.

### Cómo se identifican las provisiones/valuación del activo («…99…») — verificado, no adivinado
Contra el export del plan de cuentas del cuadre (`q4-plan.csv`): las contra-cuentas del activo son (1) los nodos de **tercer segmento `99`** (1.2.99, 1.3.99, 1.4.99, 1.5.99, 1.6.99, 1.7.99, 1.9.99 y todo lo que cuelga) y (2) los que el plan marca con un nombre que **empieza con «(»** (incluye 1.5.02.99 DEPRECIACION, 1.9.01.99 y 1.9.02.99 AMORTIZACION ACUMULADA, que tienen `99` en el cuarto segmento). **El `99` como último segmento NO es provisión**: 1.1.02.05.99 es un banco y 1.2.01.05.0x.99 una tarjeta. La regla toma las dos señales (`esProvisionOValuacion`): el tercer segmento o la cuenta/algún ancestro con nombre que empieza con «(». No usé la naturaleza de `CNT.NTRL`: el modelo la documenta al revés del código (`NaturalezaCuenta`: 1 = acreedora; `SaldoCuenta`: 1 = deudora), así que no es confiable sin datos.

### Cómo se excluye el cierre del ejercicio
El motor de cierre (`AsientoServiceImpl.generaAsientoCierre`) **nadie lo llama** (auditoría); el cierre de 2025 lo cargó el cargador de asientos manuales (C5) como `CIE-2025-12-001/002` **con fecha 1-ene del año siguiente**. El cargador guarda el número del cliente en la observación como `[archivo] #CIE-2025-12-001 | glosa`. Por eso el acumulado de resultados excluye los asientos cuya observación contiene `#CIE-` o empieza con `ASIENTO DE CIERRE DEL PERIODO` (el del motor). Para el BALANCE el cierre SÍ cuenta (ya movió los resultados a patrimonio). No se usó el tipo de asiento: el alterno 1 de `PLNT` colisiona (ASIENTO_CIERRE y CREDITOS).

## 2. Lo que se agregó
- **`SaldoContrarioCuentaService.saldosContrarios(idEmpresa, anio, mes)`** (solo lectura): cuenta, nombre, naturaleza esperada, saldo, valor contrario, tipo (BALANCE / RESULTADOS), si la regulariza sola, y el **asiento sugerido**. `GET /prdo/saldosContrarios/{idEmpresa}/{anio}/{mes}`.
- **Control `SALDO_CONTRARIO_CUENTA`** (grupo SALDOS) en `/prdo/controlCierre`: ERROR con la lista acotada a 15 cuentas y el total dicho (cantidad y suma), con el asiento sugerido o la marca «automático». Lo toma el candado C51 como cualquier control (**excusable con motivo**). Corre en `REQUIRES_NEW`; si no se puede medir, ADVERTENCIA con el motivo.
- **Corrección automática solo para redondeos**: `POST /prdo/regularizarRedondeos/{idEmpresa}/{anio}/{mes}?usuario=&simular=true|false` (por defecto simula). Las cuentas de redondeo con saldo acreedor (acumulado del año sin cierre) van a otros ingresos: **D cuenta de redondeo / H otros ingresos** con fecha del último día del mes, observación `C52 REGULARIZACION REDONDEOS AAAA-MM: …` (llave de idempotencia: si ya hay un asiento vigente con esa marca, no se repite). Con el período contable cerrado no graba (`validarContableAbierto`). Usuario validado (U-2).
- **Propuestas para lo demás (NO se graban)**: pasivo/patrimonio deudor → `D <contrapartida del papel 3> / H cuenta` (reclasificar a un activo); activo acreedor → `D cuenta / H <contrapartida del papel 4>`. Gastos acreedores que no son redondeo e ingresos deudores: sin sugerencia (revisar a mano). Las graba el usuario con aprobación y novedad C25.
- **Plantilla alterno 40** `CNT REGULARIZACION SALDOS CONTRARIOS` (semilla `sql/415`, reservada en el registro §2c, constantes en `PlantillasContabilidad`): papel 1 (DEBE, aux2 1 y 2) las cuentas de redondeo, papel 2 (HABER) otros ingresos, papel 3 (DEBE) contrapartida de pasivo deudor, papel 4 (HABER) contrapartida de activo acreedor. **Ningún número de cuenta en el código.** Sin la plantilla el control igual funciona (sin sugerencia ni marca automática) y la regularización se rechaza con el mensaje «correr sql/415».

### Novedad C25 desde aquí (propuesta, no programada)
Crear la novedad C25 en BORRADOR desde el control tocaría `SeguimientoNovedad(V2)ServiceImpl`, con su acta/decisiones, que no conozco al detalle; el riesgo es crear novedades duplicadas cada vez que el semáforo se consulta. Propongo un lote aparte: endpoint explícito `POST /prdo/saldosContrarios/{…}/novedad?cuenta=` que cree UNA novedad por (cuenta, mes), idempotente por clave, con el asiento sugerido en su texto; nunca desde el GET ni desde el semáforo.

## 3. Archivos
- Nuevos: `cnt/service/ReglaSaldoContrario.java`, `SaldoContrarioCuentaService.java`, `dto/SaldoContrarioCuenta.java`, `dto/ResultadoRegularizacionRedondeos.java`, `cnt/serviceImpl/SaldoContrarioCuentaServiceImpl.java`, `rubros/PlantillasContabilidad.java`, `cnt/sql/415_C52_PLANTILLA_REDONDEO_A_OTROS_INGRESOS.sql`, este doc, `src/test/java/com/saa/ejb/cnt/service/ReglaSaldoContrarioPruebas.java`, `tools/pruebas/correr-pruebas-c52.sh`.
- Modificados: `ControlCierreServiceImpl` (un `@EJB` y una línea, junto a los del C48; **fuera de los hunks de la C19b**), `PeriodoRest` (dos endpoints), `REGISTRO-RESERVAS-EQUIPOS.md` (alterno 40).

## 4. Aceptación del cuadre (NO corre aquí; con la plantilla 40 sembrada, sql/415)
1. **Junio**, sin `AJC-2026-06-002` ni `RRH-2026-06-REG1`: `GET /prdo/saldosContrarios/{empresa}/2026/6` trae **4.8.99.08 con saldo −0,11** (RESULTADOS, `regularizableAutomatico = true`) y **2.5.01 deudora 99,77** (BALANCE, sugerencia D 1.4.90.90.05 / H 2.5.01). `POST /prdo/regularizarRedondeos/{empresa}/2026/6?usuario=…&simular=false` asienta D 4.8.99.08 0,11 / H 5.3.90.90 0,11 y deja 4.8.99.08 en 0; la 2.5.01 sigue (no es redondeo).
2. **Julio**, sin `RRH-2026-07-REG1`: detecta **2.5.01 deudora 850,13**.
3. **Con los asientos** (AJC y RRH registrados): 0 filas en los tres meses; `SALDO_CONTRARIO_CUENTA` en OK.
4. Candado C51: con el candado encendido y la 2.5.01 contraria, el cierre se rechaza nombrando `SALDO_CONTRARIO_CUENTA`; con la excepción `{SALDO_CONTRARIO_CUENTA, motivo}` cierra (queda en `CNT.HCPR`).
Nota: la identificación del asiento de cierre (`#CIE-` en la observación) y la marca de provisión («(» / tercer segmento `99`) se verificaron contra el cargador y el export del plan, **no contra la base**: en la copia hay que ver que el RESULTADOS de enero no arrastre el `CIE-2025-12-001` (fechado 1-ene) y que ninguna provisión salga en la lista.

## 5. Compatibilidad con `origin/ear/siguiente`
`PeriodoRest` y los demás tocados son iguales en las dos ramas salvo `ControlCierreServiceImpl`, que difiere en los tres hunks de la C19b; las líneas de este lote (el `@EJB` y el `controles.addAll` junto a los del C48) están fuera de ellos y su contexto es igual. Nada depende de `DTRP`, `TempReportes` ni del motor de balances.

## 6. No verificado
Nada corrió contra base ni WildFly: las consultas nativas (con subconsulta de ancestro «(» y los filtros de observación) usan columnas ya usadas por el repo (`DTASCNTA/DBEE/HBRR`, `ASNTCDGO/FCHA/ESTD/OBSR`, `PLNNCNTA/NMBR/TPOO`) pero no corrieron. Falta confirmar: que `CNT.PLNN` tenga una sola fila por cuenta y empresa (si no, el JOIN duplicaría saldos), que el nombre de los nodos de provisión empiece con «(» en la base de producción (se vio en un export de 2025), el comportamiento de `validarContableAbierto` con el período cerrado, y el tipo de asiento (`TipoAsientos.INGRESOS`: desde la vigencia de los tipos automáticos lo decide la regla IN-A/EG-A/DI-A).
