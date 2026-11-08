# Contrato — ciclo de otorgamiento del crédito

**Fecha:** 2026-08-31 · **Equipo:** CRD · EQUIPO B · **Escrito por:** árbitro `omen-saa-1-arb`
**Backend:** en construcción en paralelo, **sin desplegar**. Se implementa contra este contrato.

> Complementa `API-GENERACION-TABLA-AMORTIZACION.md`, que sigue vigente: la generación de la tabla
> no cambia de forma, solo suma una guarda más (§4).

---

## 1. Qué se agrega a la pantalla

`forms/prestamo/prestamo-edit` deja de ser un alta suelta y pasa a tener **ciclo**: la solicitud se
crea, se le genera la tabla, y se **aprueba** o se **rechaza** ahí mismo.

**No hay niveles de aprobación, ni bandeja, ni segundo firmante** (decisión del usuario, 2026-08-31).
Quien opera la pantalla aprueba. No busques una pantalla de autorización: no existe y no se va a
construir.

---

## 2. Los estados

Vienen del catálogo `EstadoPrestamo` y viajan en **`prestamo.idEstado`** (`PRSTIDST`).

| Código | Nombre | Qué significa en la pantalla |
|---|---|---|
| `6` | `PENDIENTE_DE_APROBACION` | Solicitud creada, **todavía sin tabla** |
| `1` | `GENERADO` | Tiene tabla de amortización, **sin aprobar** |
| `2` | `VIGENTE` | Aprobado. La tabla queda **congelada** |
| `7` | `RECHAZADO` | Rechazado. Terminal |

⚠️ **No uses `prestamo.estadoPrestamo` (`ESPSCDGO`) para esto.** Es la FK al catálogo, no el estado
operativo. El campo que manda es `idEstado`.

```
  PENDIENTE_DE_APROBACION (6) ──generar tabla──► GENERADO (1) ──aprobar──► VIGENTE (2)
             │                                        │  ▲                      │
             │                                        │  └─ regenerar           │
             └──────────── rechazar ──────────────────┤                         ▼
                                                      ▼              [ desembolso: NO es
                                               RECHAZADO (7)           de esta pantalla ]
```

---

## 3. Los dos endpoints nuevos

```
POST /SaaBE/rest/prst/aprobar/{id}
POST /SaaBE/rest/prst/rechazar/{id}
```

Cuerpo de `rechazar`:

```json
{ "usuario": "jperez", "observacion": "texto libre, opcional" }
```

Cuerpo de `aprobar` (**corregido 2026-10-13**: desde el desembolso, `7bca1713`, el backend exige
además `idEmpresa` e `idUsuario`; sin ellos responde *«falta idEmpresa»* / *«falta idUsuario»*):

```json
{ "usuario": "jperez", "observacion": "texto libre, opcional", "idEmpresa": 1236, "idUsuario": 15 }
```

- `idEmpresa`: la empresa de la sesión (la misma que mandan las demás pantallas que generan órdenes
  de pago). `idUsuario`: el **código numérico** del usuario de la sesión, no el login.
- **ACTUALIZADO 2026-11-07:** aprobar valida antes capacidad de pago y cupo (§9) y, si hay **niveles de aprobación**
  configurados (§12), puede aprobar solo un nivel: el préstamo sigue GENERADO hasta el último. El campo que cuenta
  como aprobador es **`usuario`** (el código sale de su nombre; `idUsuario` del cuerpo se ignora).
- Aprobar pasa el préstamo a VIGENTE, escribe el asiento de entrega y deja la **orden de desembolso**
  en tesorería con el producto de pago **976** «Desembolso de prestamo a socio» (grupo 79, cuenta
  `2.3.90.90.10 SOCIOS POR PAGAR`, creado con `crd/sql/322` el 2026-10-13).

### idAsoprep — lo asigna «Aprobar» (U6, 2026-10-13)

Diseño: `PLAN-CICLO-OTORGAMIENTO.md` §7.
- `aprobar` asigna `idAsoprep = MAX(PRSTIDAS) + 1` y lo devuelve en el `Prestamo` de la respuesta.
  La pantalla lo muestra en el mensaje de éxito («Préstamo aprobado con N° Asoprep 60124») y en el campo.
- Si dos aprobaciones chocan, la segunda responde con el error *«Otro préstamo se aprobó al mismo
  tiempo y tomó el número N; vuelva a aprobar»*. No se graba nada; reintentar.
- **`POST`/`PUT /prst` (saveSingle) ignoran el `idAsoprep` que mande el cliente.** Nuevo o no
  aprobado → `null`. Aprobado → el valor que ya tiene la base. El FE manda `idAsoprep: null`,
  **nunca 0**.
- En «Ingreso» el campo es de **solo lectura** y no obligatorio. Sin número muestra «Se asigna al
  aprobar».

Devuelven el `Prestamo` actualizado, con `idEstado` ya movido y los campos de auditoría llenos
(`usuarioAprobacion`/`fechaAprobacion`, o `usuarioRechazo`/`fechaRechazo`).

**Errores (aprobar, desde 2026-10-13):** los rechazos de negocio (estado, sin tabla, sin suma asegurada, orden de pago rechazada…) salen con **`HTTP 422`** y `{ "mensaje": "..." }`; un error técnico sigue siendo `HTTP 500`. Rechazar sigue con `HTTP 500`. ⚠️ **El cuerpo NO es texto plano, aunque el REST lo escriba así:** hay un `@Provider` global (`com.saa.ws.rest.MensajeErrorJsonFilter`) que envuelve toda respuesta ≥400 cuya entidad sea un `String` y cuyo tipo sea JSON, y la entrega como `{"mensaje": "..."}`. Leé `error.mensaje` primero, con el texto crudo como respaldo — que es lo que ya hace `extraerMensajeError`. Si la transición no
aplica —por ejemplo aprobar un préstamo que todavía no tiene tabla— el mensaje **nombra el estado
actual**. Mostralo tal cual con `extraerMensajeError`; no lo reemplaces por un texto genérico.

⚠️ **El backend valida el estado siempre, no confía en que la pantalla haya deshabilitado el botón.**
Deshabilitar el botón es para que el usuario entienda; la garantía está del otro lado.

---

## 4. Qué habilitar y qué no, según el estado

| Estado | Guardar datos | Generar tabla | Regenerar | Aprobar | Rechazar |
|---|---|---|---|---|---|
| `6` PENDIENTE | Sí | **Sí** | — | **No** (sin tabla no hay qué aprobar) | **Sí** |
| `1` GENERADO | Sí | — | **Sí**, con confirmación | **Sí** | **Sí** |
| `2` VIGENTE | Solo lo no financiero | **No** | **No, nunca** | — | — |
| `7` RECHAZADO | No | **No** | **No** | — | — |

**La regla de congelamiento es nueva y es la que más se va a notar:** una vez aprobado, la tabla no
se regenera **aunque no tenga ningún pago**. Hasta ahora la única guarda era la de pagos; ahora son
dos y se verifican las dos.

Aprobar y rechazar **piden confirmación** (`ConfirmDialogComponent`, el mismo que ya usa el botón de
regenerar). Rechazar es `type: 'danger'` y es terminal: decilo en el mensaje.

---

## 5. Lo que NO entra en esta pantalla

- **El desembolso hacia tesorería.** Es un proceso aparte, posterior a la aprobación, con su propio
  asiento. La pantalla llega hasta dejar el préstamo `VIGENTE`. No agregues botón de desembolsar ni
  toques `fechaAcreditacion`/`usuarioAcreditacion`.
- ~~**Evaluación de capacidad de pago por bandas.**~~ **Corregido 2026-11-07:** las «bandas»
  (`ClasificadorBandaService`, `CRD.BNDP`) **no son capacidad de pago**: clasifican la cartera por días de mora para la
  contabilidad. La capacidad de pago y el cupo del otorgamiento son otra cosa y **ya existen** (§9).
- **La carga de tabla desde Excel.** Es el camino de la cartera migrada y queda **intacto**. No le
  agregues guardas de estado ni lo toques.

---

## 6. Cuidado con la cartera migrada

Esta misma pantalla es la que se usa hoy para registrar los préstamos **migrados**, que entran ya en
`VIGENTE` y con su tabla cargada por Excel. Al agregar el ciclo:

- **No rompas ese camino.** Un préstamo que abre en `VIGENTE` tiene que seguir viéndose y editándose
  como hasta ahora, solo que sin poder regenerar la tabla.
- Los botones de aprobar y rechazar **no aparecen** en un préstamo `VIGENTE` o `RECHAZADO`: no hay
  transición válida desde ahí.

---

## 7. Anular un préstamo otorgado, e imprimir pagaré y tabla (NUEVO 2026-10-13)

Diseño: `docs/logica-negocio/crd/DISENO-ANULACION-PRESTAMO-E-IMPRESOS.md` (AN1–AN11), solo en saaBE.

### 7.1 `GET /prst/{id}/puedeAnular`
`{ "puede": true|false, "motivo": "texto o null" }`. El botón «Anular» se muestra solo en VIGENTE (2)
o EN_MORA (11), y se **habilita** con `puede = true`. Con `puede = false`, el `motivo` va como tooltip.

### 7.2 `POST /prst/anular/{id}`
```json
{ "usuario": "jperez", "motivo": "texto obligatorio", "idEmpresa": 1236, "idUsuario": 15 }
```
- Anula la orden de desembolso en tesorería, **reversa** el asiento de entrega con la fecha de hoy y
  deja el préstamo en **ANULADO (12)**. El idAsoprep se conserva.
- **200:** el `Prestamo` actualizado (`idEstado: 12`, `usuarioAnulacion`, `fechaAnulacion`, `motivoAnulacion`,
  `idAsientoEntrega`, `idAsientoReverso`).
- **400** sin `motivo`, `usuario`, `idEmpresa` o `idUsuario`. **404** si el préstamo no existe.
- **422** si no se puede, con `{ "mensaje": "..." }`: estado no anulable, tiene pagos o cobros (el mensaje
  dice cuáles), la orden ya fue pagada o está en archivo del banco (el texto de CXP), un cierre de
  cartera o una provisión ya lo procesó, está en una póliza de seguro viva, falta el asiento de
  entrega, o el período de hoy está cerrado.
- Todo o nada: con un 422 no cambió nada.

### 7.3 Impresos — `POST /rprt/generar` (ACTUALIZADO 2026-10-13: modelos del cliente)

Diseño: `docs/logica-negocio/crd/DISENO-IMPRESOS-OTORGAMIENTO.md` (IM1–IM17), solo en saaBE.

```json
{ "modulo": "crd", "nombreReporte": "RPRT_PRST_SLCT", "formato": "PDF", "parametros": { "P_PRSTCDGO": 5555, "P_USUARIO": "jperez" } }
{ "modulo": "crd", "nombreReporte": "RPRT_PRST_PGRE", "formato": "PDF", "parametros": { "P_PRSTCDGO": 5555 } }
{ "modulo": "crd", "nombreReporte": "RPRT_TBLA_ACML", "formato": "PDF", "parametros": { "P_PRSTCDGO": 5555, "P_USUARIO": "jperez" } }
```
- **Solicitud de crédito** (`RPRT_PRST_SLCT`, nueva): desde GENERADO (1) o PENDIENTE (6), y en
  adelante, salvo RECHAZADO (7).
- **Pagaré** (`RPRT_PRST_PGRE`, ahora con el **modelo V2 del cliente**): necesita la tabla (lleva la cuota) y se bloquea en RECHAZADO (7) y en **ANULADO (12)**: un pagaré de un préstamo anulado no se imprime.
  Antes de aprobar sale marcado **BORRADOR** y sin número, porque el número es el idAsoprep.
- **Autorización de descuento por rol** (`RPRT_PRST_ADRL`, NUEVA 2026-11-07, formato `FOR-CRE-AUT-01`):
  parámetros `P_PRSTCDGO` y `P_USUARIO`. Mismas reglas que el pagaré: se bloquea en RECHAZADO (7) y ANULADO (12) y antes
  de aprobar sale **BORRADOR** (sin número de operación). Botón «Imprimir autorización».
- **Solicitud V1** (2026-11-07): `RPRT_PRST_SLCT` cambia de formato (empleador por filial, frecuencia mensual, capacidad de
  pago, saldos, valor líquido, fechas, firma en su propia página). Ya **no recibe** `P_SALDO_CREDITOS_VIGENTES`: lo calcula.
- **Pagaré con garante** (2026-11-07): con garante activo (§11) el pagaré agrega un segundo bloque «FIRMA DEL GARANTE».
- **Tabla de amortización** (`RPRT_TBLA_ACML`): igual que antes. Sale desde GENERADO, incluido ANULADO,
  que se imprime marcado así.
- Botones en Créditos → Préstamos → Ingreso, con el préstamo cargado: «Imprimir solicitud»,
  «Imprimir pagaré», «Imprimir tabla» y «Imprimir documentos» (§7.4).

### 7.4 Los CUATRO juntos — `POST /rprt/generarLote` (NUEVO 2026-10-13; **cuatro desde 2026-11-07**)
```json
{ "modulo": "crd", "formato": "PDF",
  "reportes": [
    { "nombreReporte": "RPRT_PRST_SLCT", "parametros": { "P_PRSTCDGO": 5555, "P_USUARIO": "jperez" } },
    { "nombreReporte": "RPRT_PRST_ADRL", "parametros": { "P_PRSTCDGO": 5555, "P_USUARIO": "jperez" } },
    { "nombreReporte": "RPRT_PRST_PGRE", "parametros": { "P_PRSTCDGO": 5555 } },
    { "nombreReporte": "RPRT_TBLA_ACML", "parametros": { "P_PRSTCDGO": 5555, "P_USUARIO": "jperez" } }
  ] }
```
- **200:** `application/pdf`, un solo documento con los reportes en el orden pedido. Mismo manejo de
  la respuesta binaria que `/rprt/generar`.
- **400:** `formato` distinto de PDF o `reportes` vacío. **500:** falló uno de los reportes. El
  mensaje dice cuál, y no se entrega un PDF a medias.
- **Al aprobar con éxito** (§3), la pantalla llama a este endpoint **automáticamente**, en ese orden
  (**solicitud, autorización, pagaré, tabla**), y abre el PDF para imprimir. Con niveles (§12) el lote sale **solo al
  aprobar el último nivel**. El préstamo ya tiene idAsoprep, así que el
  pagaré sale numerado.
- «Imprimir documentos» hace lo mismo bajo demanda, y solo se habilita con idAsoprep, es decir, ya
  aprobado.
- Si el lote falla después de aprobar, **la aprobación ya quedó hecha**: se muestra el error del
  impreso como aviso, no como fallo de la aprobación, y los botones siguen disponibles para reintentar.

## 8. Suma asegurada del bien en garantía (NUEVO 2026-10-13)

Diseño: `docs/logica-negocio/crd/DISENO-GARANTIA-BIEN-Y-PROVISION-CARTERA.md` (GA1–GA6), solo en saaBE.

- **Campo:** `valorAsegurado` (número > 0, dos decimales) en el `Prestamo` que se envía con `POST/PUT /prst`.
- **Cuándo se muestra:** solo si el producto es de familia **PRENDARIO o HIPOTECARIO**, es decir, si el
  tipo de préstamo del producto se llama así. En ese caso es **obligatorio**. En un quirografario no se
  muestra y no se envía.
- **Etiqueta:** «Suma asegurada del bien». Va con un texto de ayuda: «Valor por el que se registra la
  garantía en cuentas de orden».
- **Al editar** un préstamo cargado, se muestra el valor que ya tiene.
- **Aprobar** (§3) un prendario o hipotecario sin `valorAsegurado > 0` devuelve **422**, con
  `{ "mensaje": "Falta la suma asegurada del bien: sin ella no se registra la garantía en cuentas de orden" }`.
  No se numera ni se contabiliza nada.

---

## 9. Evaluación del otorgamiento: capacidad de pago y cupo (NUEVO 2026-11-07)

Diseño: `docs/logica-negocio/crd/DISENO-SOLICITUD-V1-AUTORIZACION-Y-REGLAS-OTORGAMIENTO.md` (B1, B2), solo en saaBE.
Un solo cálculo (`EvaluacionOtorgamientoService`) para guardar, generar la tabla, aprobar, el simulador y la pantalla.

- **Capacidad:** cuota nueva + cuotas vigentes ≤ **50 %** de la remuneración unificada (`CRD.PRTC.PRTCRMUN`).
- **Cupo:** saldo de capital de los vigentes + monto ≤ **100 %** de la cuenta individual.
- «Vigentes» = estados 2, 8, 10 y 11, **sin** el préstamo evaluado y **sin** el original que se nova (se cancela con el nuevo).
  El capital pendiente usa `PagoPrestamoService.calcularSaldoCapitalPendiente`, nunca `PRSTSLCP` (columna muerta).
- No pasan por estas reglas: la carga Petro, la migración y la **reestructuración** de un préstamo en mora.

| Momento | Capacidad | Cupo |
|---|---|---|
| `POST/PUT /prst` de un préstamo nuevo o en 6/1 | no (todavía no hay cuota) | **422** |
| `POST /prst/generarTablaAmortizacion/{id}/{cuotaCero}` | **no bloquea**; informa (ver abajo) | informa |
| `POST /prst/aprobar/{id}` | **422** | **422** |
| Sin remuneración registrada | al aprobar, **422**: «Registre la remuneración unificada del partícipe en Actualización de datos → Datos del partícipe» | — |

Los 422 llevan `{ "exito": false, "etapa": "VALIDACION", "mensaje": "…", "codigo": "" }`. Ejemplo de mensaje: «La cuota 128,71
más las cuotas vigentes 310,00 superan la capacidad de pago 400,00 (50 % de 800,00)».

**`GET /prst/evaluarOtorgamiento/{id}`** → 200 con `EvaluacionOtorgamiento` (sin sobre). Informa siempre, aunque el préstamo
no esté sujeto (`aplicaReglas = false` y `motivoNoAplica`).
**`POST /prst/evaluarOtorgamiento`** con `{ "idEntidad", "monto", "cuotaNueva", "idPrestamoOriginal" }` → el mismo DTO (para el simulador).

```json
{ "idEntidad": 3947, "idPrestamo": 5555, "idPrestamoOriginal": null, "monto": 15000.0,
  "aplicaReglas": true, "motivoNoAplica": null,
  "remuneracion": 3363.0, "capacidadPago": 1681.5, "cuotasVigentes": 482.38, "cuotaNueva": 255.69, "capacidadOk": true,
  "saldoCuentaIndividual": 30171.87, "saldoCapitalVigentes": 20180.0, "cupoOk": true,
  "mensajes": [] }
```
`capacidadOk` es `null` si no se pudo evaluar (sin cuota o sin remuneración); `cupoOk` es `null` sin monto.

**`generarTablaAmortizacion` con evaluación:** con `?conEvaluacion=true` la respuesta es `{ "prestamo": {…}, "evaluacion": {…} }`
(para mostrar el aviso de capacidad). **Sin el parámetro, la respuesta es el `Prestamo`, igual que siempre.**

## 10. Tasa del 9 % y simulador de novación (NUEVO 2026-11-07)

- **Tasa (B5):** constante `ParametroOtorgamiento.TASA_OTORGAMIENTO = 9.0`. En `POST/PUT /prst` de un préstamo nuevo o en
  6/1 (no reestructuración): tasa nula o 0 → el servidor graba **9**; **otra tasa → 422** «La tasa de interés de los créditos es 9 %».
  El FE arranca el campo en 9,00 y de solo lectura. (`tasaNominal`/`tasaEfectiva` las escribe, como siempre, la generación de la tabla.)
- **`POST /prst/simularNovacion`** `{ "idPrestamoOriginal", "monto", "plazo" }` → sobre de los simuladores
  `{ exito, etapa: "SIMULACION", mensaje, resultado }` con `resultado` = `ResultadoSimulacionNovacion`:

```json
{ "original": { "idPrestamo": 3984, "idAsoprep": 61839, "producto": "QUIROGRAFARIO", "cuotaActual": 482.38, "vs": 20180.0,
                "desgloseVs": { "capital": 20180.0, "interes": 0.0, "mora": 0.0, "seguros": 0.0 } },
  "simulacion": { "tablaProyectada": [], "totalCapital": 0, "totalInteres": 0, "totalDesgravamen": 0, "totalSeguro": 0, "totalAPagar": 0, "valorCuota": 0 },
  "valorLiquido": 6000.0,
  "evaluacion": { "…": "EvaluacionOtorgamiento, SIN el original" } }
```
  422 si: el original no es quirografario ni emergente (`PRDC.PRDCCDPT` PQ/PE), no es sustituible (mismo motivo que
  `/sustituibles`: solo estados 2 y 11, sin mora, sin cobros en trámite), o el monto no supera el valor de sustitución.
  Simula a 9 %, con el tipo de amortización del original, desde hoy y sin cuota 0. **No graba nada.**
- **`GET /prst/sustituibles/{idEntidad}?tipo=`** agrega a cada fila **`codigoPetro`** (`PRDC.PRDCCDPT`, p. ej. `PQ`/`PE`) para
  filtrar la novación sin depender del nombre de la familia.

## 11. Garante del préstamo (NUEVO 2026-11-07)

Tabla `CRD.GRPR` (`crd/sql/369`, **antes del EAR**). Es **opcional**, uno solo **activo** por préstamo, y solo se agrega, cambia o
anula con el préstamo en **6 o 1** (después, 422). La cédula del garante no puede ser la del deudor. Si viene `idEntidad`
(garante partícipe), nombre y cédula salen de `CRD.ENTD`, no de la pantalla.

| Endpoint | Qué hace |
|---|---|
| `GET /grpr/prestamo/{idPrestamo}` | el garante **activo**: 200 con él, **204** si no tiene |
| `POST /grpr?usuario=` | crea (201). Cuerpo `{ idPrestamo, idEntidad?, nombre, cedula, direccion?, telefono?, correo? }`. Si ya hay uno activo, 422 |
| `PUT /grpr?usuario=` | cambia; el cuerpo trae `codigo` |
| `DELETE /grpr/{id}?usuario=` | **anula** (`estado = 0`), no borra |
| `GET /grpr/getAll`, `GET /grpr/getId/{id}`, `POST /grpr/selectByCriteria` | estándar |

`GarantePrestamo`: `{ codigo, idPrestamo, idEntidad, nombre, cedula, direccion, telefono, correo, estado, usuarioRegistro,
fechaRegistro, usuarioModificacion, fechaModificacion }`. Errores de regla: 422 `{ exito:false, etapa:"VALIDACION", mensaje }`;
usuario inválido: 400. **Pagaré (`RPRT_PRST_PGRE`):** con garante activo agrega «FIRMA DEL GARANTE» (nombre y C.C.); el texto legal no
cambia. La solicitud no cambia.

## 12. Niveles de aprobación configurables (NUEVO 2026-11-07)

Tablas `CRD.RGAP`, `CRD.NVAP`, `CRD.UNAP`, `CRD.APPR` (`crd/sql/370`, **antes del EAR**). Una **regla** (`RGAP`) aplica según tipo de
préstamo (nulo = cualquiera), monto y plazo (rangos inclusivos, nulo = abierto) y tiene una **prioridad** (gana la menor; empate = 422).
Sus **niveles** (`NVAP`, orden 1..n; el número de niveles es cuántos activos tiene) se aprueban **en orden**; en cada nivel aprueba
**cualquiera** de sus usuarios (`UNAP`, usuario = `SCP.PJRQ`). Cada decisión queda en `APPR`.

**Transición:** mientras **no exista ninguna regla activa** en toda la tabla, el préstamo se aprueba **como hoy**, en un paso. En
cuanto existe una, un préstamo sin regla aplicable **no se aprueba** (422 «No hay regla de aprobación para tipo T, monto M, plazo P»).
Una regla sin niveles activos, o un nivel sin usuarios, es error de configuración (422 con su nombre).

**`POST /prst/aprobar/{id}[?conDetalle=true]`** (mismo cuerpo; el aprobador es `usuario`):
- Se valida B2 (§9) **en cada nivel**. Se usa la regla de las aprobaciones vigentes del préstamo (no se re-resuelve).
- El `usuario` tiene que ser de un usuario del **nivel pendiente** (si no, 422 «El nivel N (nombre) lo aprueba: u1, u2») y **no puede aprobar dos
  niveles** del mismo préstamo.
- Si no era el último nivel, el préstamo **sigue GENERADO (1)**. Si era el último, corre el aprobar completo en la **misma transacción**
  (si falla, la aprobación del nivel se revierte).
- Respuesta: sin `conDetalle` es el `Prestamo` (su `idEstado` dice si quedó VIGENTE). Con `?conDetalle=true`:
  `{ "prestamo": {…}, "aprobacionCompleta": false, "nivelAprobado": 1, "totalNiveles": 2, "siguienteNivel": "Gerencia", "mensaje": "Nivel 1 de 2 aprobado; falta: Gerencia" }`.
- **El lote de impresos (§7.4) se imprime solo cuando `aprobacionCompleta` es `true`.**

**`GET /prst/estadoAprobacion/{id}?idUsuario=`** (`idUsuario` = código `SCP.PJRQ`):
```json
{ "regla": { "id": 1, "nombre": "Quirografarios hasta 30.000" }, "sinNiveles": false,
  "niveles": [ { "idNivel": 1, "orden": 1, "nombre": "Jefe de Crédito", "usuarios": ["jperez"], "aprobadoPor": "jperez", "fecha": "2026-11-07T10:15:00", "decision": 1 },
               { "idNivel": 2, "orden": 2, "nombre": "Gerencia", "usuarios": ["mlopez"], "aprobadoPor": null, "fecha": null, "decision": null } ],
  "nivelPendiente": 2, "totalNiveles": 2, "puedeAprobar": false, "mensaje": null }
```
`sinNiveles = true`: no hay reglas activas (se aprueba en un paso). Si la configuración falla, `regla` es nulo, `sinNiveles` es `false` y
`mensaje` dice por qué (no es un error HTTP).

**Rechazar** (`POST /prst/rechazar/{id}`): con regla, solo puede negar un usuario de algún nivel de ella y queda la negación del nivel
pendiente; sin niveles aplicables, como hoy.
**Invalidación:** si el préstamo cambia después de una aprobación parcial (guardar con otro monto, plazo, tasa o producto, o regenerar
la tabla), las aprobaciones vigentes se invalidan y la aprobación **vuelve al nivel 1**. La pantalla lo avisa antes de guardar.

**Configuración** (pantalla «Niveles de aprobación»): CRUD de `rgap`, `nvap` y `unap` — `GET /getAll`, `GET /getId/{id}`,
`POST ?usuario=`, `PUT ?usuario=`, `DELETE /{id}?usuario=` (deja **inactivo**, no borra), `POST /selectByCriteria` — más
`GET /rgap/completa/{id}` (regla con niveles y usuarios), `GET /rgap/simular?idTipo=&monto=&plazo=` (qué regla aplicaría; no falla por
la configuración, la explica en `mensaje`), `GET /nvap/porRegla/{idRegla}` y `GET /unap/porNivel/{idNivel}`.
Campos: `ReglaAprobacionPrestamo { codigo, idTipoPrestamo, nombre, montoMinimo, montoMaximo, plazoMinimo, plazoMaximo, prioridad (def. 100), estado }`,
`NivelAprobacionPrestamo { codigo, idRegla, orden, nombre, estado }`, `UsuarioNivelAprobacion { codigo, idNivel, idUsuario, estado }`.

**Solicitud, sección 7:** «Nivel de aprobación» = nombre del último nivel y «(N niveles)»; «Revisado por» = quien aprobó el nivel anterior
al último; «Aprobado por» = quien aprobó el último.
