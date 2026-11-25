# C27b — propiedades del servidor y orden del pase (D-172)

Acompaña a `sql/383_C27B_INTERES_ANTICIPADO_PLANTILLA_38_Y_SUBPROCESOS_11_12.sql` y `sql/384_MEDICION_C27B_INTERES_ANTICIPADO.sql`.
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
- **⑪** (subproceso 12, plantilla **38**, fechado al corte): por tipo, `objetivo − (lo que los ⑪ vigentes ya asentaron + base)`.
  Lo asentado se lee de `CNT.DTAS` (debe − haber de la cuenta 1.4.02.xx de cada tipo) de los asientos `CRD.ANCC` subproceso 12 con
  ANCC activo, corrida EJECUTADA, asiento no anulado y fecha anterior al corte. No se ajusta contra el saldo de la cuenta.
- **Reversos** (`DevengoCompletoService`): abono a capital (cuotas historizadas menos nuevas, de meses ya devengados) y
  precancelación (cuotas pasadas a 7 sin cobrar de meses ya devengados), D 5.1.02.xx / H 1.4.02.xx (plantilla 17), dentro del asiento
  del evento. «Mes ya devengado» = vencimiento hasta el último día del mes siguiente al corte de la última corrida EJECUTADA, y solo si ese
  corte es `>= devengoCompletoDesde`.

## Orden del pase

1. SQL 383 (CK_ANCC_TPOO 1..12 y plantilla 38). La cuenta del pasivo la decide el contador: el script se detiene mientras tenga el marcador.
2. Fijar las tres propiedades en el servidor (la de la base de transición la entrega el cuadre).
3. Desplegar el EAR.
4. Cerrar el mes `devengoCompletoDesde`: previsualizar primero y revisar las advertencias del ⑪ (objetivo, asentado, base).

## Pendiente (reportado al árbitro)

- Reverso en la precancelación con cobro de caja (CASO B, el asiento es CBCRASN2 y `EVPR.numeroAsiento` queda nulo) y en el acuerdo de
  condonación (`generarAsientoCondonacion` no guarda su asiento en el evento): no se tocaron, porque anular la operación no
  reversaría esas líneas sin tocar el motor de reverso.

## Condición para activar `saa.cartera.devengoCompletoDesde` (hueco conocido, lote 1b)

El reverso del devengo cubre hoy: el abono a capital (los dos caminos) y la precancelación con aportes o con préstamo nuevo. **NO cubre**
la precancelación con cobro de caja (CBCRASN2, `EVPR.numeroAsiento` nulo) ni la precancelación 100 % efectivo sin cobro, que están en la rama
c45 del cuadre (`CobroCreditoServiceImpl`, `contabilizarPrecancelacion`); quedan para el lote 1b, después del merge de c45.

Efecto: una precancelación por ventanilla hecha **después** de cerrar el mes en que se devengó su cuota deja ese ingreso devengado y no cobrado
sin reverso. Si la precancelación se hace **antes** del cierre, el ④ ya la ve (las cuotas en estado 7 devengan solo lo cobrado) y no hay hueco.
El código es inerte mientras `devengoCompletoDesde` no esté definida; definirla sabiendo este hueco.

## Acuerdo de condonación (medición, sin cambios)

`generarAsientoCondonacion` no guarda su asiento en el evento y ningún otro código lo anula al reversar el cobro (ver el reporte del lote).
Las cuotas del acuerdo en estado 7 hechas antes del cierre quedan bien por la regla del ④ (estado 7 = solo lo cobrado).
