# Pago de préstamo con los aportes del garante (equipo omen-saa-3, 2026-11-11) — URGENTE

Pedido del usuario: *«permitir pagar un préstamo con las cuentas individuales del garante (el garante debe ser
partícipe del fondo SIEMPRE)»*; el 2026-11-11, *«necesitamos hacer urgente el pago de préstamos con aportes de garante»*.

Estado previo verificado en `main` (2026-11-10):
- `CRD.GRPR.ENTDCDGO` es opcional: hoy se admite un **tercero** con nombre y cédula a mano
  (`GarantePrestamoServiceImpl.guardarGarante`, FE `garante-prestamo` con `ModoGarante = 'PARTICIPE' | 'MANUAL'`).
  Era la pregunta P5 de `DISENO-SOLICITUD-V1-…` («por defecto, cualquiera»): **el usuario la cierra: siempre partícipe**.
- `ProcesoPagoPrestamoServiceImpl.pagarConAportes` (`POST /rest/prst/pagarConAportes`, pantalla «Cruce de valores»)
  valida y consume **solo** los aportes de `prestamo.getEntidad()` (el deudor).

## G1 — El garante es siempre partícipe

**BE** (`GarantePrestamoServiceImpl.guardarGarante`): `idEntidad` **obligatorio** en alta y edición; sin él,
`IncomeException("El garante debe ser partícipe del fondo: indique el partícipe (idEntidad)")`. Nombre y cédula se
siguen tomando de `CRD.ENTD` (ya es así). El partícipe garante debe existir; no puede ser el deudor (ya validado).
No hay DDL: la columna queda nullable por los históricos; la regla vive en el servicio.

**FE** (`forms/prestamo/garante-prestamo`): se quita el modo `MANUAL` (tercero). Solo `ParticipeSelector`. Un garante
histórico sin `idEntidad` se muestra en solo lectura con un aviso «Garante registrado como tercero: reemplácelo por un
partícipe» y la opción de anularlo y agregar otro.

**SQL de solo lectura** `crd/sql/e3-16-garantes-sin-participe.sql` (lo escribe el árbitro y lo corre el usuario; el
rango numérico 350–399 es de omen-saa-1): garantes activos sin
`ENTDCDGO`, con su préstamo y estado, y si la cédula del tercero coincide con un partícipe (`ENTDNMID`) — para
corregirlos por UPDATE autorizado si el usuario quiere.

## G2 — Pago con aportes del garante (`POST /rest/prst/pagarConAportes`)

### Contrato
`SolicitudPagoConAportes` gana un campo opcional:

| Campo | Tipo | Regla |
|---|---|---|
| `idGarante` | `Long` (PK de `CRD.GRPR`) | Opcional. Sin él, todo igual que hoy (aportes del deudor). |

Con `idGarante`:
1. El GRPR existe, `GRPRESTD = 1`, `PRSTCDGO = idPrestamo` y `ENTDCDGO` no nulo; si no, `IncomeException` clara
   («el garante X no está activo / no es de este préstamo / no es partícipe»).
2. La **entidad dueña de los aportes** es la del garante (`ENTDCDGO`): `validarDesgloseAportes(aportes, entidadGarante)`
   y `consumirAportes(entidadGarante, …)`. El préstamo y sus cuotas siguen siendo del deudor (`aplicarPago` igual).
3. **`rutaDocumentoRespaldo` obligatoria** (la autorización firmada del garante). *Decidible: por defecto sí.*
4. Mismas reglas de siempre: préstamo operable, no excede la deuda (si no, precancelación), mes abierto.
5. Trazabilidad: la glosa de las filas APRT negativas y de los PGAP dice
   `"PAGO PRESTAMO {id} DEL DEUDOR {CI deudor} CON APORTES DEL GARANTE - Evento {e}"`; la observación del evento y la
   huella agregan «con aportes del garante {nombre} (CI …)»; la glosa del asiento de cruce
   (`construirGlosaCrucePorParticipe`) agrega «- aportes del garante {nombre} (CI …)». Para eso `ContextoPago` lleva
   el `idEntidadAportes` (null = el deudor). **Las cuentas contables no cambian** (plantilla por tipo de aporte, sin
   auxiliar por partícipe — verificado en `lineasCruceAportesConsumidos`).
6. **Anulación**: `anularOperacion` ya crea el contra-movimiento con `original.getEntidad()` de la fila APRT, o sea
   devuelve el aporte **al garante**. No se toca; solo verificarlo.
7. Las llamadas internas a `pagarConAportes` (pensión complementaria, jubilación) no mandan `idGarante`: sin cambio.

### Decidible (valores por defecto mientras el usuario no diga otra cosa)
- **Cuándo**: en cualquier momento con cuotas pendientes (no se exige mora).
- **Orden**: no se obliga a agotar primero los aportes del deudor; lo decide el operador.
- **Qué aportes del garante**: los mismos tipos vigentes que en el cruce normal del deudor.
- **Cupo del garante**: no se bloquea si el pago deja al garante por debajo de su propio comprometido; la pantalla
  **avisa** (saldo del garante después del pago vs. su capital pendiente).

## G3 — FE «Cruce de valores» (`forms/cruce-de-valores`)

- Al elegir un préstamo con garante activo partícipe (`GET` del garante activo del préstamo — el servicio
  `garante-prestamo.service.ts` ya existe), aparece el selector **«Pagar con: aportes del deudor | aportes del garante
  {nombre}»**.
- Con «garante»: los fondos disponibles se cargan con `saldosPorEntidad(idEntidad del garante)` y el `POST` lleva
  `idGarante`. El respaldo (autorización del garante) es obligatorio: botón deshabilitado sin él.
- Confirmación explícita: «Se descontarán $X de las cuentas individuales de {garante} para pagar el préstamo {id} de
  {deudor}».
- El resultado y el comprobante dicen de quién salieron los aportes.

## Verificación
- BE: `mvn -q compile`. FE: `npx ng build`.
- Sin pruebas en producción (ni pagos de prueba para anular). La primera operación real la revisa el árbitro:
  APRT negativa en el garante, cuotas del deudor pagadas, asiento con la glosa del garante.

## G4 — Garante para préstamos ya otorgados (decisión del usuario 2026-11-11, «opción 1», antes de subir el EAR)

Hasta ahora el garante solo se agrega/cambia/anula con el préstamo en **6 o 1** (`cargarPrestamoEditable`). Los
préstamos antiguos no tienen garante y no pueden usar G2. Se habilita en los **vivos: 2, 8, 10, 11** (misma lista que
«vigentes» del resto del sistema), con la **autorización firmada del garante obligatoria**.

### BE
- **Endpoint nuevo `POST /rest/grpr/conAutorizacion`** (multipart, mismo patrón que
  `POST /rest/cnbp/conCertificado` / `CuentaBancariaParticipeService.crearConCertificado`): `archivo`, `archivoNombre`,
  `idPrestamo`, `idEntidad` (partícipe garante), `usuario`. En **una sola transacción** crea el `GRPR` (mismas reglas de
  `guardarGarante`: partícipe obligatorio, no el deudor, uno activo por préstamo) **y** su adjunto en `CRD.ADJN`:
  `PRSTCDGO` = préstamo, `ENTDCDGO` = el **deudor** (es documento del préstamo), `ADJNIDRF` = `GRPRCDGO`,
  tipo `CRD.TPDJ` **«AUTORIZACION DEL GARANTE»** resuelto por nombre exacto y activo (como «CERTIFICADO BANCARIO»).
  Archivo PDF o imagen; si algo falla no queda ni GRPR ni archivo. Sin el tipo en el catálogo: error claro
  «Falta el tipo de adjunto AUTORIZACION DEL GARANTE (correr crd/sql/e3-17)».
- Estados: el endpoint acepta **6, 1, 2, 8, 10, 11**. En 2/8/10/11 el `POST`/`PUT` genéricos de `grpr` siguen
  rechazando (alta solo con autorización; para cambiar de garante: anular y agregar otro). **Anular**
  (`DELETE /grpr/{id}?usuario=`) se permite también en 2/8/10/11. En 6/1 todo queda como hoy.
- `GET /rest/grpr/{id}/autorizacion` → el `Adjunto` activo de la autorización (o 204), para verlo desde la pantalla.
- Nada de pagaré: un préstamo ya otorgado no reimprime el pagaré.

### Catálogo — `crd/sql/e3-17-tipo-adjunto-autorizacion-garante.sql` (lo corre el usuario ANTES del EAR)
`INSERT` idempotente en `CRD.TPDJ` de «AUTORIZACION DEL GARANTE» (activo), con su control. Sin él el EAR arranca igual;
solo el alta de garante en préstamos vivos responde el error de arriba.

### FE (`forms/prestamo/garante-prestamo`, embebido en `prestamo-edit`)
- `editable` pasa a: 6/1 como hoy; **2/8/10/11 → modo «préstamo otorgado»**: elegir partícipe + **adjuntar la
  autorización firmada (obligatoria)** → `POST /grpr/conAutorizacion`. Sin edición: «Anular» y agregar otro.
- Aviso en ese modo: «El préstamo ya está otorgado: el garante se registra con su autorización firmada; el pagaré no se
  reimprime».
- Con garante existente: enlace «Ver autorización» (`GET /grpr/{id}/autorizacion`) si la tiene.
- Revisar que `prestamo-edit` muestre la sección «Garante» también en esos estados (hoy puede estar oculta fuera de 6/1).

## G5 — Decisiones del usuario sobre los «decidibles» (2026-11-11)

| # | Tema | Decisión |
|---|---|---|
| 1 | Cuándo se puede pagar con aportes del garante | **Libre**: en cualquier momento con cuotas pendientes (no se exige mora). |
| 2 | Primero los aportes del deudor | **Como hoy**: no se obliga; lo decide el operador. |
| 3 | Autorización firmada | **Como hoy**: obligatoria en **cada** pago con aportes del garante (y al registrar el garante en un préstamo otorgado). |
| 4 | Garante por debajo de su comprometido | **Aviso, sin detener** (ver abajo). |
| 5 | Diálogo «Pagar con aportes» por fila (`pago-prestamo-dialog`) | **Se deja como está** (solo aportes del deudor); el garante va solo por Cruce de valores. |
| 6 | Aviso a omen-saa-1-arb | **Sí** (enviado el 2026-11-11). |

### Aviso de comprometido del garante (decisión 4)
**BE — `GET /rest/prst/compromisoGarante/{idGarante}?monto={valor}`** (solo lectura). Devuelve:
`saldoAportes` (Σ del saldo disponible de los tipos de aporte del garante, misma fuente que `saldosPorEntidad`),
`capitalComprometido` (capital pendiente de los préstamos **vigentes PQ/PE del garante** — **el mismo cálculo que
`saldoCapitalCupo` de `EvaluacionOtorgamientoServiceImpl`**, reutilizado, no reescrito), `saldoDespues` = saldoAportes −
monto, `quedaDescubierto` = saldoDespues < capitalComprometido, y `mensaje` listo para mostrar
(«Después de este pago {garante} quedaría con $X en aportes y tiene $Y comprometidos en sus propios préstamos») o null.
Errores: garante inexistente/inactivo/sin partícipe → 422 con el mismo texto que `pagarConAportes`.
**`pagarConAportes` no cambia**: el aviso no detiene nada.

**FE — Cruce de valores**: con «aportes del garante», al abrir la confirmación se consulta el endpoint con el monto a
cruzar; si `quedaDescubierto`, el diálogo de confirmación muestra el `mensaje` en ámbar **además** del texto de §G3. El
botón sigue habilitado. Si el endpoint falla, se confirma igual (sin aviso).
