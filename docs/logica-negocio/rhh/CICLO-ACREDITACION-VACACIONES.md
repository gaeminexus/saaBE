# Ciclo de acreditación anual de vacaciones

**Estado:** defecto de producción corregido el 2026-08-27. `revertirAcreditacion` nuevo el
mismo día. Verificado contra el código en esa fecha.

## Cuándo se corre y qué hace

`POST /sldv/acreditar` (`AcreditacionVacacionesServiceImpl.acreditar(idEmpresa, fechaCorte,
usuario)`) es un proceso manual, disparado por RRHH — no hay job automático. Para cada
contrato activo de la empresa entre el 1 de enero del año de `fechaCorte` y `fechaCorte`:

1. Calcula los años de servicio cumplidos a `fechaCorte`. Si es menos de uno, no acredita
   nada: el derecho nace al cumplir el primer año (Art. 69 CT).
2. Calcula los días que corresponden según la escala de `RHH.PRNM` (`PRNMDIVC` días base,
   uno más por cada año desde `PRNMANVC`, tope `PRNMMXVC`).
3. Calcula el arrastre: los días no gozados del saldo del año anterior (ver más abajo).
4. Crea o actualiza el `SaldoVacaciones` del año de `fechaCorte` para ese empleado.

Es **idempotente en los días asignados**: volver a correrlo para el mismo año recalcula
`diasAsignados` sin tocar `diasUsados` — corre una vez por año, pero correrlo dos veces el
mismo día no duplica nada ahí. Antes de acreditar, siempre corre primero `caducarSaldos`
(mismo `idEmpresa`/`fechaCorte`): así el arrastre no incluye días que ya vencieron.

## El defecto (producción, 2026-08-27) y la corrección

`diasPendientes` del saldo nuevo se calculaba como `dias + arrastre − usados`. El arrastre
**se sumaba** al saldo del año nuevo, pero **no se restaba** del saldo del año anterior — que
seguía existiendo, con sus propios `diasPendientes` intactos. Y `diasDisponibles` (y el
consumo FIFO) suman los `diasPendientes` de **todos** los años no caducados de un empleado.
Resultado: los mismos días arrastrados se contaban en el año de origen y otra vez en el año
nuevo.

Caso real, verificado en producción el 2026-08-27:

```
Katherine Pardo (58)   2025: asignados 7.75  arrastrados 0     pendientes  7.75
                        2026: asignados 15    arrastrados 7.75  pendientes 22.75
GET /sldv/disponible/58  →  30.50   (lo correcto: 22.75)
```

7 empleados afectados, 51,63 días duplicados en total. **Los datos de producción ya se
corrigieron por SQL, directamente por el usuario — este documento no toca `RHH.SLDV` con
ningún script.** Lo que faltaba era el código: sin corregirlo, la próxima acreditación anual
vuelve a duplicar.

**Corregido:**

```java
saldo.setDiasArrastrados(RedondeoNomina.redondeaCantidad(arrastre));   // informativo
saldo.setDiasPendientes(RedondeoNomina.redondeaCantidad(Double.valueOf(
        dias.doubleValue() - usados.doubleValue())));                  // SIN arrastre
```

`diasArrastrados` se sigue calculando y guardando — sigue siendo un dato útil, muestra
cuánto viene de atrás — pero deja de sumarse a `diasPendientes`. Los días no gozados del año
anterior **se quedan viviendo en el saldo del año anterior**, no se mudan al año nuevo.

### Por qué es lo correcto, no solo lo simple

La caducidad se marca **por año** (`SLDVCDCD`, una fila por año). Si los días arrastrados se
mudaran físicamente al saldo del año nuevo, perderían su año de origen — y un día que debía
caducar a los `PRNMCDVC` años de **2023** seguiría viviendo, sin caducar nunca, disfrazado de
día del año **2026**. Dejándolos en su propio año:

- **La caducidad sigue funcionando sola**: `caducarSaldos` marca `SLDVCDCD='S'` por año,
  sin tener que rastrear de qué acreditación viene cada día.
- **El consumo FIFO sigue funcionando igual**: `selectDisponibles` ya devuelve los saldos
  ordenados por año ascendente — consume primero el año más viejo, que es exactamente el
  arrastre más antiguo, sin que el código de consumo necesite saber que es "arrastre".
- **`diasDisponibles` no necesita cambiar**: ya sumaba los `diasPendientes` de todos los años
  no caducados de un empleado, que es correcto — el bug nunca estuvo ahí, estuvo en que
  `acreditar` inflaba el `diasPendientes` de un año con los del otro.

### Otros cálculos revisados — ninguno más asume que el arrastre vive dentro de `diasPendientes`

- **`diasQueLeCorresponden`**: no toca `diasPendientes` ni el arrastre, solo la escala de
  `PRNM` contra los años de servicio. No afectado.
- **`arrastreDelPeriodoAnterior`**: lee `anterior.getDiasPendientes()` del año previo. Con la
  corrección, ese valor ya no viene inflado, así que el arrastre calculado para el año
  siguiente es correcto sin cambiar esta función.
- **`consumir` / `revertirConsumo`**: operan sobre `selectDisponibles` (FIFO por año). No
  hacían ninguna suposición sobre arrastre — simplemente sumaban/restaban `diasPendientes`
  del saldo que tocaban. Se benefician de la corrección sin cambiar nada.
- **`caducarSaldos`**: decide la caducidad por `saldo.getAnio()`, nunca por `diasPendientes`.
  No afectado en la lógica; el aviso que imprime ("N día(s) no gozados") sí venía inflado
  por el bug cuando el saldo caducado era uno que había recibido arrastre.
- **`valorDiaVacaciones`** (rama de saldo de apertura, cuando la ventana de doce meses está
  incompleta): suma `diasPendientes` de `selectDisponibles` para ponderar la tarifa. Mismo
  caso que `diasDisponibles` — se corrige solo, sin cambiar esta función.

**Ninguna otra función necesitó cambios.** El defecto estaba enteramente contenido en la
línea de `acreditar` que sumaba el arrastre.

## `revertirAcreditacion` — el reverso que no existía

Hasta el 2026-08-27, los únicos endpoints de este ciclo eran `acreditar`, `disponible`,
`valorDia` y `caducar` — ninguno deshacía una acreditación. Cuando la de este defecto se
corrió con el cálculo equivocado, la única salida fue un `UPDATE` a mano sobre producción.
Ahora existe `POST /sldv/revertirAcreditacion`:

```json
{ "idEmpresa": 1, "anio": 2026, "usuarioRegistro": "jperez" }
```
Responde `200` con el número de saldos borrados, o `500` con el motivo si se rechaza (mismo
estilo que el resto de este módulo — texto plano, no JSON estructurado).

> **Conectado en el frontend el 2026-08-28.** `acreditar-vacaciones.component.ts` ya expone el
> botón **Revertir**, con un `ConfirmDialog` de tipo *danger* (mismo patrón que Acreditar) y un
> input explícito **"Año a revertir"** — el año se pide, no se infiere de `fechaCorte`, porque el
> contrato es por `(idEmpresa, anio)` y deducirlo habría sido inventar un mapeo.
>
> Nota para quien lea el código del frontend: hasta esa fecha `saldo-vacaciones.service.ts` traía
> un comentario diciendo que *"el backend todavía no lo tiene construido"* y el botón estaba
> deshabilitado. **Ese comentario quedó desactualizado el mismo día en que se escribió** — el
> backend lo entregó después. Ya no aplica.

### Todo o nada

Recorre `SaldoVacaciones` de esa empresa y ese año. Si **cualquiera** tiene `diasUsados > 0`
o `diasPagados > 0`, o viene de una apertura de migración (`SLDVAPRT='S'` — no lo creó esta
acreditación, no le corresponde borrarlo), **se rechaza la reversión completa**, nombrando a
cada empleado bloqueante y su motivo. Nunca un reverso parcial: dejar algunos saldos del año
borrados y otros no rompería la relación entre lo que un empleado ve disponible y lo que
realmente tiene registrado.

### Deshacer la caducidad — el punto delicado

`acreditar` llama a `caducarSaldos` **antes** de acreditar. Revertir la acreditación sin
desmarcar esa caducidad dejaría caducados unos días que nadie decidió caducar — el reverso
quedaría incompleto en silencio.

**El problema: no hay ninguna columna que diga "qué corrida caducó este saldo".** No se creó
una a propósito — restricción del usuario, no se toca `RHH.SLDV` sin que el usuario escriba
el DDL. La solución no necesita columna nueva: se puede **recalcular** cuál fue el año
límite de esa corrida, con los mismos parámetros que usó `acreditar`.

`caducarSaldos(idEmpresa, fechaCorte, usuario)` marca caducado todo saldo con
`anio <= (fechaCorte.getYear() − PRNMCDVC)`. Como `PRNMCDVC` no cambia de un año a otro salvo
que alguien reparametrice, cada valor de `anio` cruza ese umbral **exactamente una vez**: el
año en que `anioLimite` llega a coincidir con él por primera vez. Los años anteriores a ese
umbral ya estaban caducados de corridas previas (`caducarSaldos` es idempotente sobre ellos:
volver a marcar `SI` un saldo que ya es `SI` no cambia nada observable). Así que:

```
anioLimite = anio − PRNMCDVC   (con el PRNM del mismo anio que se esta revirtiendo)

Saldos que ESTA corrida caducó = SaldoVacaciones de esa empresa
                                  con anio = anioLimite y SLDVCDCD = 'S'
```

`revertirAcreditacion` recalcula `anioLimite` y les pone `SLDVCDCD='N'` a esos saldos, antes
de borrar los del año que se revierte.

**Es una inferencia, no un registro exacto** — y es honesto decirlo así. Si alguien llamó a
`POST /sldv/caducar` suelto (existe como endpoint independiente, no solo dentro de
`acreditar`) con `fechaCorte` del mismo año, es indistinguible de la caducidad que produjo
`acreditar`: los dos calculan el mismo `anioLimite` con los mismos parámetros. Pero esto no
es un defecto de la solución — revertir "la caducidad que corresponde a este año" es lo
correcto en los dos casos por igual, sea cual haya sido el disparador exacto.

### Qué SÍ pide una columna, si hace falta más precisión después

Si en el futuro hace falta saber con certeza qué acreditación caducó qué saldo (por ejemplo,
para deshacer una caducidad sin deshacer la acreditación completa), la vía limpia es una
columna en `RHH.SLDV` (por ejemplo `SLDVCDAN`, el año de la acreditación que marcó la
caducidad) o una tabla de auditoría de corridas de `acreditar`/`caducarSaldos`. No se creó
ahora porque no hace falta para lo que pide `revertirAcreditacion` hoy — la inferencia por
`anioLimite` alcanza — y porque crearla sin necesidad concreta sería la misma clase de
sobre-construcción que este proyecto evita en otros módulos.
