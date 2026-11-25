# C27b — propiedades del servidor y orden del pase (D-172)

Acompaña a `sql/383_C27B_INTERES_ANTICIPADO_PLANTILLA_39_Y_SUBPROCESOS_11_12.sql` y `sql/384_MEDICION_C27B_INTERES_ANTICIPADO.sql`.
Diseño: `C27b-DISENO-INTERES-ANTICIPADO.md`. Javadoc de las propiedades: `ModoCorteCartera`.

## Las tres propiedades (propiedades de sistema de WildFly, jboss-cli)

| Propiedad | Formato | Qué hace |
|---|---|---|
| `saa.cartera.devengoCompletoDesde` | `AAAA-MM-DD` | Primer corte que se cierra con el código nuevo. **Sin ella, o con un corte anterior, el ④ es el de siempre y ④b y ⑪ no corren, y los reversos de abono y precancelación no se emiten.** Es obligatoria en la práctica: el reproceso carga un asiento manual C27b, y sin esta propiedad el código nuevo lo contabilizaría dos veces. |
| `saa.cartera.anclaArranque` | `AAAA-MM-DD` | `fechaCorte` de la corrida de arranque EJECUTADA de la empresa. Si el devengo completo está activo para el corte y la propiedad falta, o no coincide con una corrida EJECUTADA, **previsualizar y ejecutar fallan** con un mensaje. |
| `saa.cartera.anticipadoBaseTransicion` | `TPPRCDGO:valor;TPPRCDGO:valor`, punto decimal (ej. `1:123.45;2:0;3:67.80`) | Lo que el asiento manual C27b dejó en la cuenta al corte anterior a `devengoCompletoDesde`, por tipo de préstamo. Lo entrega el cuadre. Si el devengo completo está activo y falta, el ⑪ falla: no hay cero por defecto. El SQL 384 (bloque 3b) calcula el objetivo a ese corte para que el cuadre lo contraste con su asiento. |

Ejemplo (jboss-cli):

```
/system-property=saa.cartera.devengoCompletoDesde:add(value=2026-10-31)
/system-property=saa.cartera.anclaArranque:add(value=2025-05-31)
/system-property=saa.cartera.anticipadoBaseTransicion:add(value="1:0;2:0;3:0")
```

(Los valores del ejemplo son de forma; los reales los fija el cuadre.)

## Qué cambia con el devengo completo activo

- **④** (`SubProcesoCierreCartera` 5, plantilla 17): el interés ordinario sale del método DAO nuevo
  `selectInteresDevengoPorTipoEnRango`. Cuota completa (también la ya pagada), menos lo cobrado antes de la ancla; si el
  préstamo salió de cartera, solo lo cobrado con `ancla <= PGPRFCHA < corte + 1`. Universo por fecha (estados 9 y 10
  entran) también en modo normal. La mora sigue saliendo del método de siempre.
- **④b** (subproceso 11, plantilla 17, fechado al corte): primera cuota de los préstamos concedidos en el mes que se cierra.
- **⑪** (subproceso 12, plantilla **39**, fechado al corte): por tipo, `objetivo − (lo que los ⑪ vigentes ya asentaron + base)`.
  Lo asentado se lee de `CNT.DTAS` (debe − haber de la cuenta 1.4.02.xx de cada tipo) de los asientos `CRD.ANCC` subproceso 12 con
  ANCC activo, corrida EJECUTADA, asiento no anulado y fecha anterior al corte. No se ajusta contra el saldo de la cuenta.
- **Reversos** (`DevengoCompletoService`): abono a capital (cuotas historizadas menos nuevas, de meses ya devengados) y
  precancelación (cuotas pasadas a 7 sin cobrar de meses ya devengados), D 5.1.02.xx / H 1.4.02.xx (plantilla 17), dentro del asiento
  del evento. «Mes ya devengado» = vencimiento hasta el último día del mes siguiente al corte de la última corrida EJECUTADA, y solo si ese
  corte es `>= devengoCompletoDesde`.

## Orden del pase

1. SQL 383 (CK_ANCC_TPOO 1..12 y plantilla 39). La cuenta del pasivo es 2.9.01.05 (D2 decidido), buscada por su número; el script se detiene si el alterno 39 ya existe en la empresa o si la cuenta no existe.
2. Fijar las tres propiedades en el servidor (la de la base de transición la entrega el cuadre).
3. Desplegar el EAR.
4. Cerrar el mes `devengoCompletoDesde`: previsualizar primero y revisar las advertencias del ⑪ (objetivo, asentado, base).

## Reversos del devengo — cobertura (lotes 1, 1b y 1c)

Con el devengo completo activo, las operaciones que dejan interés devengado y no cobrado lo reversan (D 5.1.02.xx / H 1.4.02.xx, plantilla 17,
cuentas por plantilla), **dentro del asiento de la operación**, para que anularla lo anule:

| Operación | Dónde va el reverso | Cuotas |
|---|---|---|
| Abono a capital (con o sin aportes) | asiento del evento (re-bandeo) | historizadas menos nuevas |
| Precancelación con aportes / con préstamo nuevo | asiento del evento | cuotas en 7 que cerró |
| Precancelación por ventanilla (CBCR) | asiento definitivo del cobro (CBCRASN2), `CobroCreditoServiceImpl.generarAsientoDefinitivo` | cuotas en 7 con vencimiento posterior a la fecha de pago (`cobro.getFecha()`, la misma de `precancelar`) |
| Acuerdo de condonación (con o sin depósito) | asiento de la condonación, grabado en `EVPR.numeroAsiento` (lote 1c) | futuras en 7 (el desglose solo cuenta su capital); las exigibles se pagan o se condonan |

La precancelación directa en efectivo sin CBCR ya no existe (c45-2 la rechaza).

Cotas de las cuotas que se reversan: (a) las que ESTA operación pasó a 7 (vencimiento posterior a su fecha), (b) vencimiento hasta el
límite (último día del mes siguiente al corte de la última corrida EJECUTADA) y (c) desde el 1.º del mes siguiente al corte de
`devengoCompletoDesde`.

## Restricción operativa: reversar un cierre ya ejecutado

Lo que decide si un mes está devengado no es la fecha de negocio de la operación sino si la corrida ya corrió cuando la cuota pasó a 7. Por
eso, **con la propiedad activa, reversar un cierre ya ejecutado después de cancelaciones del mes siguiente exige revisarlo con el SQL 384
(bloque de detección de doble reverso) antes de re-ejecutar**: el ④ nuevo ve la cuota en 7 y devenga solo lo cobrado, mientras que el reverso
de la cancelación (hecho contra el ④ viejo) queda vivo, y el resultado es un doble reverso. No se resuelve en código.

## Acuerdo de condonación

Medición histórica (lote 1c): el asiento de la condonación no se grababa en el evento y quedaba vivo al reversar el cobro; ver
`REGULARIZACION-ASIENTOS-CONDONACION-1C.md` y `sql/387`.
