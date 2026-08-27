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

> **CORREGIDO EL 2026-08-27.** La primera versión de este documento agrupaba los signos por
> «está en libros / está en el banco», y ese es el eje equivocado: lo que manda es la **dirección
> de cada partida**. El agente de backend lo detectó al implementarla y tenía razón. De haberse
> aplicado la versión original, **toda conciliación con cheques girados o notas de débito habría
> cuadrado mal**.

```
saldo según libros
  - partidas tipo 1   (depósito en tránsito)
  + partidas tipo 2   (cheque girado y no cobrado)
  + partidas tipo 3   (NC del banco no registrada)
  - partidas tipo 4   (ND del banco no registrada)
  = saldo según el extracto bancario
```

Los valores se guardan **siempre positivos**; el tipo decide el signo. El razonamiento, partida
por partida, sobre un saldo en libros de 1.000:

| Tipo | Qué pasó | Efecto | Banco |
|---|---|---|---|
| **1** Depósito en tránsito (200) | Libros ya sumaron; el banco todavía no | **resta** | 1.000 − 200 = **800** |
| **2** Cheque girado no cobrado (150) | Libros ya restaron; el banco no lo debitó, o sea que **ese dinero sigue en el banco** | **suma** | 1.000 + 150 = **1.150** |
| **3** NC del banco no registrada (50) | El banco ya acreditó; los libros no | **suma** | 1.000 + 50 = **1.050** |
| **4** ND del banco no registrada (30) | El banco ya debitó la comisión; los libros no | **resta** | 1.000 − 30 = **970** |

El par 1/2 y el par 3/4 tienen **signos opuestos dentro de cada par**. Agruparlos por su origen
—libros o banco— es justamente lo que induce al error.

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

## 7bis. CORRECCIÓN DEL 2026-08-27 — `MovimientoBanco` no es el mayor auxiliar

> **Este apartado corrige un supuesto del §5 que era falso, detectado al probar la fase 1 en el
> navegador.** Afecta al modelo, no solo a la redacción.

El §5 exige `MVCBCDGO` para las partidas de tipo 1 y 2 (`CK_DTCN_TIPO_ORIGEN`), dando por
sentado que todo movimiento de banco registrado en libros tiene su fila en `TSR.MVCB`.
**No es así.** Medido sobre la base local:

| | |
|---|---|
| Detalles de asiento sobre cuentas bancarias (`1.1.02%`) | **1.448** |
| de esos, con `MovimientoBanco` asociado | **122 (8 %)** |
| sin `MovimientoBanco` | **1.326 (92 %)** |

Y comparando el saldo contable contra el que arma `MovimientoBanco`:

| Cuenta | Saldo contable | Según `MVCB` | Cobertura |
|---|---|---|---|
| BANCO INTERNACIONAL `1.1.02.05.80` | 2.714.031,22 | 125.452,02 | **4,6 %** |
| BANCO PACIFICO `1.1.02.05.65` | 2.016.302,36 | 22.802,11 | **1,1 %** |
| BANCO AMAZONAS `1.1.02.05.40` | 2.188,75 | 775,50 | 35 % |

`TSR.MVCB` solo lo alimentan ciertos procesos —pagos, cheques, caja chica—. Un asiento hecho
desde contabilidad toca la cuenta del banco **sin** crear movimiento. Ocho de cada cien.

### Consecuencia 1 — el cierre seguía sin poder cerrar

Probado sobre BANCO AMAZONAS / julio 2026: `prepararCierre` devuelve **4 pendientes de asiento y
los 4 con `idMovimientoBanco: null`**, o sea **ninguno declarable**. Como `verificar` exige que
todo pendiente esté declarado, el mes sigue sin cerrar. **El diseño movía el candado en vez de
quitarlo:** antes era «cero pendientes», ahora es «todo pendiente declarado», y una partida que
no se puede declarar reproduce exactamente el mismo bloqueo.

**Corrección del modelo:** una partida en tránsito de tipo 1 o 2 debe colgar del
**`DetalleAsiento`**, que es lo que siempre existe, y `MVCBCDGO` pasa a ser **información
adicional opcional**. `DTCN` necesita entonces una columna `DTCNDTAS` (FK a `CNT.DTAS`), y
`CK_DTCN_TIPO_ORIGEN` pasa a exigir `DTCNDTAS` para los tipos 1 y 2, no `MVCBCDGO`.

### Consecuencia 2 — y esta pega en el punto 14

`CuentaBancariaServiceImpl.obtieneSaldoFecha` arma el saldo desde `MovimientoBanco`. Con esa
cobertura, **el «saldo disponible» de cada cuenta es una fracción del real** — para BANCO
PACIFICO, el 1 %.

El rediseño de pagos (punto 14) iba a **validar la disponibilidad contra ese número**. De
haberlo construido sobre este supuesto, el sistema habría rechazado pagos legítimos por «saldo
insuficiente» teniendo la cuenta dos millones. Ver
[`../pagos/PLAN-REDISENO-APROBACION-PAGOS.md`](../pagos/PLAN-REDISENO-APROBACION-PAGOS.md) §5,
riesgo 4: decía que el saldo sería «optimista» por los movimientos en tránsito sin cerrar. Era
al revés y por otro motivo: es **pesimista**, porque casi nada llega a `MVCB`.

**Antes de tocar el punto 14 hay que decidir de dónde sale el saldo de una cuenta bancaria:**
de la contabilidad (`DetalleAsiento` sobre la cuenta del plan) o de `MovimientoBanco`. Hoy el
sistema tiene dos respuestas distintas para la misma pregunta y difieren en dos órdenes de
magnitud. Esa decisión es previa a cualquier validación de disponibilidad.

## 8. Riesgos

1. **Una partida en tránsito que nunca se salda es un síntoma, no un dato.** Un cheque girado hace ocho meses y no cobrado casi siempre significa que se perdió, se anuló por fuera, o el asiento estaba mal. **Hace falta un aviso por antigüedad** — 60 días es el umbral habitual — o el tránsito se convierte en el basurero donde se esconden los errores. Sin eso, este diseño empeora las cosas en vez de mejorarlas.
2. **No confundir el tránsito con el descuadre.** Declarar una partida no la justifica: la ecuación tiene que cerrar igual. Si el usuario puede declarar cualquier cosa hasta que cuadre, el control desaparece. Por eso la tolerancia se mantiene en 0,01 y no se hace configurable.
3. **`obtieneSaldoFecha` y el rediseño de pagos.** Los 121 movimientos sin conciliar hoy inflan el saldo disponible. Cerrar este trabajo es lo que vuelve confiable la validación de disponibilidad del punto 14 — por eso conciliación va primero.

## 9. Fases

| Fase | Qué | Tamaño | Estado |
|---|---|---|---|
| **1** | DDL (`TSR.DTCN` + columnas de estado en `TSR.CNCL` + rubro de tipos) y modelo JPA | S | **Hecho** (2026-08-27) — aplicado en local. Ver §10. |
| **2** | Servicio de cierre: declarar partidas, calcular la ecuación, cerrar, anular; y el arrastre en las dos consultas de pendientes | M | **Hecho** (2026-08-27) — ver §10.2. |
| **3** | Pantalla de cierre con los tres bloques y la ecuación en vivo | M | Pendiente (frontend) |
| **4** | Aviso de partidas en tránsito con más de 60 días, en el tablero de Tesorería | S | Backend hecho (`partidasEnTransitoAntiguas`/`GET /cnct/transito/antiguas/{idEmpresa}`); falta engancharlo al tablero (frontend) |

---

## 10. Estado de implementación (2026-08-27)

### 10.1 T1 — Modelo

DDL aplicado en la base local (no en producción — lo escribe el usuario a partir de esto):

- `TSR.DTCN`: exactamente como §5, más `DTCNFCRG` (fecha de registro, auditoría — no estaba en
  el diseño original, adición mínima consistente con el resto de tablas de este módulo).
- `TSR.CNCL`: `CNCLESTD`, `CNCLFCCR`, `CNCLUSCR` como en §5, **más `CNCLMTAN`** (motivo de
  anulación, `VARCHAR2(500)`) — cuarta columna no listada en §5, necesaria para
  `anularCierre(idCierre, motivo, usuario)`.
- Rubros nuevos (interfaces Java planas, no `SCP.PRBR` — mismo criterio que otros rubros de un
  solo uso interno de este proyecto): `TipoPartidaTransito` (239), `EstadoPartidaTransito` (240),
  `EstadoCierreConciliacion` (241).
- Entidad `DetalleTransito` (`com.saa.model.tsr`), y tres columnas nuevas en la entidad
  `Conciliacion` existente (`estadoCierre`, `fechaCierre`, `usuarioCierre`, `motivoAnulacion`).
- `DetalleAsiento` y `DetalleExtractoBancario` ganaron un campo `@Transient boolean esArrastrada`
  (no persistido) — lo pone `selectPendientes` cuando la fila viene del arrastre. La fecha
  "original" de una partida arrastrada no necesitó campo nuevo: ya está en
  `fechaTransaccion`/`asiento.getFechaAsiento()`.

### 10.2 T2 — Servicio de cierre

`ConciliacionCierreService`/`Impl` implementa `prepararCierre`, `cerrar`, `anularCierre`,
`partidasEnTransitoAntiguas` tal como se especificó.

**La ecuación quedó confirmada el 2026-08-27** (el usuario la verificó partida por partida y
corrigió §3 con el mismo ejemplo numérico usado durante la implementación — el punto que se
había señalado ahí sigue siendo válido como registro de por qué el signo de cada tipo es el que
es, no como algo pendiente):

```
saldoExtracto = saldoLibros − sumaTipo1 + sumaTipo2 + sumaTipo3 − sumaTipo4
```

Implementada en `ConciliacionCierreServiceImpl.saldoExtractoEsperado`, aislada a propósito en un
único método — de él dependen tanto `cerrar()` como la vista previa de `prepararCierre()`.

Otras decisiones tomadas para poder implementar sin más preguntas, documentadas en el código:

- **`DTCN.DTCNCNSL` (cierre que saldó) queda `null`** cuando una partida arrastrada se salda vía
  la conciliación N:M ordinaria (`conciliarGrupo`), porque ese flujo no crea ni conoce ningún
  `TSR.CNCL` — sólo `cerrar()` los crea. `DTCNESTD=Saldada` ya es suficiente para que `verificar`
  no vuelva a exigir esa partida.
- **Una línea de asiento pendiente sin ningún `MVCB` asociado no se puede declarar en tránsito**
  (`PendienteAsientoTransito.idMovimientoBanco`/`tipoSugerido` vienen `null`): `TSR.DTCN` exige
  `MVCBCDGO` para tipo 1/2 por diseño (`CK_DTCN_TIPO_ORIGEN`), y esa línea no tiene de dónde
  colgarlo. Sigue siendo pendiente sin declarar hasta que se concilie por el N:M o se investigue
  — es, a propósito, exactamente el mismo tipo de caso que el riesgo #1 quiere que no se pueda
  esconder.
- **`anularCierre` rechaza si alguna de las partidas que declaró ya está Saldada** (se conciliadó
  de verdad después del cierre): anular igual desharía una conciliación real sin que el usuario lo
  pidiera. Hay que deshacer esa conciliación primero (`POST /cnct/deshacer/{idGrupo}`).
- **`anularCierre` sólo el último cierre vigente** de la cuenta/período (verificado contra
  `ConciliacionDaoService.selectCierreVigente`, que filtra por `CNCLESTD` — no confundir con
  `selectByPeriodoCuentaEstado`, que filtra por `CNCLRZZA`/rubro 43, un estado *distinto* en la
  misma tabla, del mecanismo viejo `insertaConciliacion` que nunca llegó a producción).
- **`ConciliacionContableService.verificar` ya no exige cero pendientes** (T3): exige que todo
  pendiente actual esté cubierto por una `TSR.DTCN` Pendiente, y que exista un cierre `CNCL`
  vigente (`CNCLESTD=Cerrado`) para esa cuenta/período — no re-deriva la ecuación (eso ya lo validó
  `cerrar()` al crear ese cierre; repetirla ahí sería duplicar la fórmula en dos sitios).
- `GrupoConciliacionExtracto/AsientoDaoService.contarPendientes` se amplió igual que
  `selectPendientes` (no estaba pedido explícitamente, pero es necesario: si no,
  `resumenPorPeriodo` y `verificar` habrían quedado viendo un número de pendientes distinto al
  que `selectPendientes` realmente devuelve).

### 10.3 Contrato JSON exacto de cada endpoint nuevo

Todos bajo `@Path("cnct")`, el mismo que ya usa el resto de la conciliación contable.

#### `GET /cnct/transito/preparar/{idCuentaBancaria}/{idPeriodo}`

Respuesta 200:
```json
{
  "idCuentaBancaria": 4,
  "idPeriodo": 87,
  "conciliadosDelMes": [
    { "idGrupo": 12, "valorExtracto": 500.00, "valorAsiento": 500.00,
      "fechaConciliacion": "2026-08-05T10:00:00", "usuarioConcilia": "jperez" }
  ],
  "pendientesExtracto": [
    { "idDetalleExtracto": 201, "fecha": "2026-04-30", "descripcion": "DEPOSITO",
      "valor": 100.00, "esArrastrada": false, "tipoSugerido": 3 }
  ],
  "pendientesAsiento": [
    { "idDetalleAsiento": 550, "idAsiento": 9001, "idMovimientoBanco": 771,
      "fecha": "2026-04-30", "descripcion": "Cheque #123", "valor": 50.00,
      "esArrastrada": false, "tipoSugerido": 2 },
    { "idDetalleAsiento": 551, "idAsiento": 9002, "idMovimientoBanco": null,
      "fecha": "2026-04-28", "descripcion": "Ajuste manual", "valor": 15.00,
      "esArrastrada": false, "tipoSugerido": null }
  ],
  "saldoLibros": 12345.67,
  "saldoExtractoSugerido": 12300.00,
  "diferenciaSugerida": 45.67
}
```
`idMovimientoBanco`/`tipoSugerido` en `null` (segunda fila de `pendientesAsiento` arriba) =
esa línea no se puede declarar en tránsito, ver §10.2. `saldoExtractoSugerido`/
`diferenciaSugerida` pueden venir `null` si no hay ninguna fila de extracto en el período (nada
de qué tomar el último saldo).

#### `POST /cnct/transito/cerrar`

Solicitud:
```json
{
  "idCuentaBancaria": 4,
  "idPeriodo": 87,
  "partidas": [
    { "idMovimientoBanco": 771, "idDetalleExtracto": null, "tipo": 2,
      "observacion": "Cheque #123, proveedor todavia no lo cobra" },
    { "idMovimientoBanco": null, "idDetalleExtracto": 201, "tipo": 3,
      "observacion": "NC del banco, revisar con contabilidad" }
  ],
  "saldoExtracto": 12300.00,
  "idUsuario": 5
}
```
Nota: `valor` **no** se envía por partida — lo calcula el backend del `MovimientoBanco`/
`DetalleExtractoBancario` referenciado, para que un número mal copiado en el frontend no pueda
descuadrar la ecuación. `idUsuario` es numérico (SCP.PJRQ), igual que el resto del sistema —
corregido 2026-08-27, la primera versión de este endpoint recibía `usuario` como texto, distinto
del resto de la API; se resuelve internamente a su nombre sólo para guardarlo en los campos de
texto de `TSR.CNCL` (`CNCLUSCR`), que sí son `VARCHAR2` (mismo criterio que
`ControlExtractoBancario.usuarioCierre`).

Respuesta 201:
```json
{
  "idCierre": 77,
  "idCuentaBancaria": 4,
  "idPeriodo": 87,
  "saldoLibros": 12345.67,
  "saldoExtracto": 12300.00,
  "diferencia": 0.00,
  "estado": 2,
  "fechaCierre": "2026-08-27T15:00:00",
  "usuarioCierre": "jperez",
  "partidasDeclaradas": 2
}
```
Respuesta 400 (ecuación no cuadra, o queda algún pendiente sin declarar) — texto plano, no JSON
estructurado (mismo estilo que el resto de este módulo):
```
La ecuacion no cuadra: saldo segun libros 12345.67, saldo segun extracto 12200.00, diferencia 145.67 (tolerancia 0.01). Revise las partidas declaradas.
```

#### `POST /cnct/transito/anular/{idCierre}`

Solicitud:
```json
{ "motivo": "Se declaro con el tipo equivocado", "idUsuario": 5 }
```
`idUsuario` numérico, mismo criterio que en `cerrar` (ver nota arriba). `TSR.CNCL` no tiene una
columna dedicada a "quién anuló" (no estaba en el modelo del §5) — el nombre resuelto se anexa al
texto de `motivoAnulacion` (`"{motivo} (anulado por {nombre})"`), mismo patrón que `ANTEMTAN` en
`AnticipoEmpleado` reutilizando un solo campo de texto para motivo + autor.

Respuesta 200: la `Conciliacion` (CNCL) actualizada — **entidad completa, sin proyección**. Es
una sola fila (no el problema de payload de 536 KB de una lista), pero es el mismo patrón
estructural: incluido en el barrido de endpoints pesados de la sección siguiente.

Respuesta 400 si no es el último cierre, o si alguna partida que declaró ya está Saldada — texto
plano con el motivo exacto.

#### `GET /cnct/transito/antiguas/{idEmpresa}?dias=60`

`dias` opcional, default 60. Respuesta 200:
```json
[
  { "idPartida": 33, "tipo": 2, "valor": 50.00, "diasEnTransito": 75,
    "cuentaBancaria": "BANCO PICHINCHA - CTA CTE 34217424-04",
    "declaradaEn": "2026-06-13T09:00:00", "observacion": "Cheque #123..." }
]
```
