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
