# Conciliación bancaria: modelar las partidas en tránsito

**Punto 12 del listado del 2026-08-27** · **Fecha:** 2026-08-27
**Decisión del usuario:** *"Debemos modelar el tránsito explícitamente, que es lo correcto."*
**Rol de este documento:** diseño del modelo, escrito por el orquestador. El DDL definitivo sale de aquí después de que el agente valide el modelo en local.

---

## 1. El problema, en una frase

**Una sola partida en tránsito impide cerrar el mes para siempre.**

Verificado en el código:

- `GrupoConciliacionExtractoDaoServiceImpl.selectPendientes` filtra `d.periodo.codigo = :idPeriodo`, y el de asientos filtra `fechaAsiento between primerDia and ultimoDia`.
- `verificar` exige **cero** pendientes de ambos lados.
- `cerrarMes` exige todas las cuentas verificadas.

Un depósito registrado en libros el 30/abr y acreditado por el banco el 02/may **no puede conciliarse nunca**: cada lado vive en un período distinto y no existe pantalla donde coexistan. Y como no se puede conciliar, queda pendiente; y como queda pendiente, abril no cierra. Nunca.

Esto no es un defecto de la conciliación N:M, que funciona bien. Es que **falta el concepto**: hoy un movimiento solo puede estar *conciliado* o *pendiente*, y la realidad tiene un tercer estado.

## 2. Qué es una partida en tránsito

Es una diferencia **legítima y esperada** entre los libros y el banco al cortar el mes. Son cuatro, y son las cuatro esquinas de la conciliación clásica:

| # | Situación | Nombre contable |
|---|---|---|
| 1 | Está en **libros**, no en el banco, y **suma** | **Depósito en tránsito** |
| 2 | Está en **libros**, no en el banco, y **resta** | **Cheque girado y no cobrado** |
| 3 | Está en el **banco**, no en libros, y **suma** | Nota de crédito del banco no registrada |
| 4 | Está en el **banco**, no en libros, y **resta** | Nota de débito del banco no registrada (comisiones) |

Una partida en tránsito **no es un error**: es un mes que cierra bien. Lo que la vuelve un problema es no poder declararla.

## 3. La ecuación que hace cerrar el mes

En vez de exigir cero pendientes, el cierre exige que **cuadre la ecuación clásica**:

```
saldo según libros
  + partidas tipo 3 y 4   (están en el banco, no en libros)
  - partidas tipo 1 y 2   (están en libros, no en el banco)
  = saldo según el extracto bancario
```

Con la misma tolerancia de 0,01 que ya usa `conciliarGrupo`. Si cuadra, **el mes cierra aunque queden partidas sin conciliar**, porque están declaradas y justificadas. Si no cuadra, hay una diferencia real y el mes no debe cerrar — que es exactamente lo que se quiere.

## 4. Lo que ya existe y no hay que construir

- **La conciliación N:M funciona completa**, backend y frontend. No se toca.
- **`TSR.MVCB` ya identifica las partidas una a una**: `MVCBCNCL` (conciliado), `MVCBFCCN`, `MVCBMSSS`/`MVCBANOO`. El dato a nivel de ítem ya está.
- **`TSR.CNCL` (entidad `Conciliacion`) ya modela el cuadre clásico completo**, con `depositoTransito`, `chequeTransito`, `creditoTransito`, `debitoTransito`, `saldoBanco`, `saldoEstadoCuenta`. Y `ConciliacionServiceImpl.insertaConciliacion` **ya lo calcula**, apoyándose en `MovimientoBancoServiceImpl.saldosSegunBancos`, que suma los `MVCB` no conciliados del período.
  **El diseño correcto se pensó una vez y quedó huérfano: sin REST y sin pantalla.**

Su límite: `CNCL` guarda solo **totales**. Sirve para cuadrar, no para saber *cuáles* partidas están en tránsito ni para saldarlas cuando el banco las acredite. Eso es lo que falta.

### Estado de los datos (base local, copia de producción)

| | |
|---|---|
| `TSR.MVCB` total | 121 |
| conciliados | **0** |
| sin conciliar | **121**, repartidos en 2 períodos |
| `TSR.CNCL` | **0** |

**Nunca se ha conciliado en esta base.** No hay nada que migrar: el modelo nuevo arranca limpio. Es la mejor circunstancia posible para hacerlo bien.

## 5. El modelo propuesto

Se **reutiliza `TSR.CNCL`** como cabecera del cierre —ya tiene los campos del cuadre y está vacía, así que ampliarla no arriesga datos— y se agrega **una tabla de detalle** que es la pieza que falta: qué partida concreta quedó en tránsito y cuándo se saldó.

### `TSR.DTCN` — Detalle de partidas en tránsito

| Columna | Tipo | Qué guarda |
|---|---|---|
| `DTCNCDGO` | PK identity | |
| `CNCLCDGO` | FK → `TSR.CNCL` | El cierre que **declaró** la partida |
| `MVCBCDGO` | FK → `TSR.MVCB`, nullable | El movimiento de libros no acreditado (tipos 1 y 2) |
| `DTCNIDEX` | FK a la línea de extracto, nullable | La línea del banco no registrada (tipos 3 y 4) |
| `DTCNTPOO` | NUMBER | 1 Depósito en tránsito · 2 Cheque girado no cobrado · 3 NC del banco · 4 ND del banco |
| `DTCNVLOR` | NUMBER(18,2) | Valor, siempre positivo; el tipo dice si suma o resta |
| `DTCNESTD` | NUMBER | 1 Pendiente · 2 Saldada |
| `DTCNCNSL` | FK → `TSR.CNCL`, nullable | El cierre en que **se saldó** |
| `DTCNOBSR` | VARCHAR2(1000) | Por qué quedó en tránsito |

Dos restricciones que evitan basura:

- `CK_DTCN_ORIGEN`: exactamente uno de `MVCBCDGO` / `DTCNIDEX` no nulo. Una partida es de libros **o** del banco, nunca las dos.
- `CK_DTCN_TIPO_ORIGEN`: los tipos 1 y 2 exigen `MVCBCDGO`; los tipos 3 y 4 exigen `DTCNIDEX`.

Y en `TSR.CNCL`, tres columnas nuevas: estado del cierre (1 borrador, 2 cerrado, 3 anulado), fecha de cierre y usuario.

### Por qué una tabla y no una columna en `MVCB`

Porque una partida puede quedar en tránsito, saldarse, y el mes siguiente entrar otra distinta. Una columna guarda un solo estado; la tabla guarda **la historia**: qué cierre la declaró, cuál la saldó y cuánto tiempo estuvo en el aire. Eso último es justamente lo que un auditor pregunta.

## 6. Cómo cambia el flujo

**Al preparar el cierre**, la pantalla deja de pedir cero pendientes y muestra tres bloques:
1. Lo conciliado del mes (informativo).
2. **Lo pendiente, para clasificar**: cada partida sin conciliar se marca como en tránsito con su tipo, o se deja sin declarar. El tipo se puede proponer solo (un `MVCB` de ingreso → tipo 1; de egreso → tipo 2; una línea de extracto no registrada → 3 o 4 según el signo).
3. **La ecuación en vivo**: saldo libros, ± las partidas declaradas, saldo banco, y la diferencia. Igual que el cierre de caja chica, que ya funciona así y el usuario ya conoce.

**Al confirmar**, se exige que la diferencia esté dentro de 0,01. Se crea el `CNCL` con sus totales y las filas de `DTCN`. El mes queda cerrado.

**Al abrir el mes siguiente**, los pendientes **incluyen las partidas en tránsito de meses anteriores**, marcadas visiblemente como arrastradas y con la fecha original. Ahí es donde se resuelve el bloqueante: el depósito del 30/abr y el crédito del banco del 02/may **por fin coexisten en la misma pantalla** y se pueden conciliar con el N:M que ya funciona.

**Al conciliarse una partida arrastrada**, su fila de `DTCN` pasa a Saldada con el cierre que la saldó.

## 7. Las dos consultas que hay que cambiar

Es el corazón del cambio, y es pequeño:

- `GrupoConciliacionExtractoDaoServiceImpl.selectPendientes`: hoy filtra `d.periodo.codigo = :idPeriodo`. Debe devolver **los del período pedido más los declarados en tránsito y todavía pendientes**, vengan del período que vengan.
- El equivalente de asientos, que filtra `fechaAsiento between primerDia and ultimoDia`: mismo tratamiento.
- `verificar`: deja de exigir cero pendientes y pasa a exigir que **todo pendiente esté declarado en tránsito** y que la ecuación cuadre.

## 8. Riesgos

1. **Una partida en tránsito que nunca se salda es un síntoma, no un dato.** Un cheque girado hace ocho meses y no cobrado casi siempre significa que se perdió, se anuló por fuera, o el asiento estaba mal. **Hace falta un aviso por antigüedad** — 60 días es el umbral habitual — o el tránsito se convierte en el basurero donde se esconden los errores. Sin eso, este diseño empeora las cosas en vez de mejorarlas.
2. **No confundir el tránsito con el descuadre.** Declarar una partida no la justifica: la ecuación tiene que cerrar igual. Si el usuario puede declarar cualquier cosa hasta que cuadre, el control desaparece. Por eso la tolerancia se mantiene en 0,01 y no se hace configurable.
3. **`obtieneSaldoFecha` y el rediseño de pagos.** Los 121 movimientos sin conciliar hoy inflan el saldo disponible. Cerrar este trabajo es lo que vuelve confiable la validación de disponibilidad del punto 14 — por eso conciliación va primero.

## 9. Fases

| Fase | Qué | Tamaño |
|---|---|---|
| **1** | DDL (`TSR.DTCN` + columnas de estado en `TSR.CNCL` + rubro de tipos) y modelo JPA | S |
| **2** | Servicio de cierre: declarar partidas, calcular la ecuación, cerrar, anular; y el arrastre en las dos consultas de pendientes | M |
| **3** | Pantalla de cierre con los tres bloques y la ecuación en vivo | M |
| **4** | Aviso de partidas en tránsito con más de 60 días, en el tablero de Tesorería | S |
