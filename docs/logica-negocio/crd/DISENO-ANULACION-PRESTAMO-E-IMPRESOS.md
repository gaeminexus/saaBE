# DISEÑO — Anulación del préstamo otorgado, pagaré y tabla de amortización impresos, y la contabilidad sin datos quemados

**Fecha:** 2026-10-13 · **Equipo:** `omen-saa-1` · **Árbitro:** `omen-saa-1-arb`
**Contexto:** el usuario prueba hoy en la tarde el ciclo completo: generación → aprobación → impresos → anulación.
**Relacionado:** `PLAN-CICLO-OTORGAMIENTO.md` (U3–U6), `PLAN-DESEMBOLSO-PRESTAMO.md`, contrato `API-CICLO-OTORGAMIENTO.md` §8.

---

## 1. Pedido del usuario (2026-10-13), textual

> «Si se otorga un préstamo se puede anular siempre y cuando no se haya realizado ningún pago, y los
> asientos generados se deben reversar. Asegúrate de que todo el proceso funcione de principio a fin,
> de que se puedan imprimir el pagaré y la tabla de amortización y de que exista la opción de anular.
> Además necesito saber de dónde estás tomando la información para generar la contabilidad. Todo
> debería ser parametrizable y no debe existir ninguna cuenta contable quemada en el código.»

## 2. Lo que había al 2026-10-13 (verificado contra el código)

| Pieza | Estado |
|---|---|
| Anular un préstamo otorgado | **No existe.** No hay endpoint ni estado ANULADO en `EstadoPrestamo` (1–11). |
| Id del asiento de entrega | **No se guarda** en el préstamo: `aprobar` lo recibe y lo pierde (`PrestamoServiceImpl`, paso 3). Sin él no se puede reversar. |
| Orden de desembolso | Se guarda en `PRSTIDPG`. CXP ya trae `anularPago` con la regla `motivoNoAnular`, que usan devoluciones, rhh y tsr. |
| Reverso de asiento | `AsientoService.reversionAsiento(idAsiento, fechaReverso)` existe y se usa en otros procesos. |
| Pagaré impreso | **No existe** (`rep/crd/` no tiene ninguno). |
| Tabla de amortización impresa de un préstamo real | **Ya existía:** `RPRT_TBLA_ACML` (corregido 2026-10-13; el árbitro lo había dado por inexistente). Se extiende con ANULADO (12) y ANULADA (9). |
| Cuentas contables en el código | **Ninguna** literal en `crd` (barrido por regex del 2026-10-13). Ver §6. |
| Producto de pago del desembolso | **Quemado** como `976L` en `PrestamoServiceImpl`. No es una cuenta, pero es un id de configuración. Se elimina en §6.2. |

## 3. Anulación — reglas

| # | Regla |
|---|---|
| AN1 | Solo se anula un préstamo **VIGENTE (2)** o **EN_MORA (11)**. Cualquier otro estado → 422 con el estado actual. |
| AN2 | **Ningún pago.** Se rechaza si el préstamo tiene **cualquier** dinero aplicado o en curso, con el detalle de qué lo bloquea: (a) un `PagoPrestamo` (CRD.PGPR) no anulado; (b) un detalle de cobro (CRD.DCBC) en un cobro REGISTRADO, APROBADO o PROCESADO; (c) consumo de aportes ligado al préstamo; (d) cualquier otra vía de aplicación que el BE encuentre. El BE tiene que enumerarlas, no suponerlas. |
| AN3 | **La orden de desembolso** (`PRSTIDPG`) se anula con `PagoProgramadoService.anularPago`. Si CXP no deja (ya pagada, en archivo del banco, etc.), su motivo se devuelve tal cual y **no se anula nada**. Si el dinero ya salió, la anulación no corresponde: primero hay que reversar el pago en CXP. Es el mismo criterio que la devolución de aportes. |
| AN4 | **El asiento de entrega se REVERSA** con `reversionAsiento(idAsiento, hoy)`, no se borra: queda el rastro de los dos asientos. El id sale de `PRSTASEN` (columna nueva, §5). Con `PRSTASEN` nulo (préstamo aprobado antes de este cambio) → 422 «no se encuentra el asiento de entrega; reverse manualmente», y no se anula nada. Hoy no hay ninguno en producción, porque `aprobar` nunca funcionó. |
| AN5 | **Cierres de cartera y provisiones:** si un cierre de cartera o una provisión ya procesó este préstamo después de la aprobación, se rechaza con el motivo, porque su asiento ya lo incluye. El BE determina dónde queda ese rastro por préstamo. Si no hay forma de saberlo, lo reporta y no inventa la guarda. |
| AN6 | **Pólizas:** si el préstamo está en un documento de seguro vivo (CRD.PSPR de un POSG no anulado) → rechazo; primero sale por nota de crédito. |
| AN7 | Estado final: **ANULADO (12)**, nuevo en `EstadoPrestamo`. Las cuotas pasan al estado anulado de su propio catálogo, si existe; si no existe, el BE lo reporta antes de inventar uno. |
| AN8 | **El idAsoprep se conserva.** U6: la numeración no pierde ni salta. Un préstamo anulado sigue ocupando su número, y `saveSingle` lo conserva porque 12 no está entre 1, 6 y 7. |
| AN9 | Auditoría: `PRSTUSAN` (usuario), `PRSTFCAN` (fecha y hora), `PRSTMTAN` (motivo, obligatorio) y `PRSTASRV` (id del asiento de reverso). |
| AN10 | **Todo en una transacción**: validaciones → anular orden → reversar asiento → estado. Si algo falla, nada queda a medias. Una excepción atrapada a través de un proxy EJB igual marca el rollback (H83): no se atrapa para seguir. |
| AN11 | Período: el reverso se fecha **hoy**. Si el período de hoy no está abierto, falla con el motivo de contabilidad. |

## 4. Impresos

| Reporte | Contenido | Parámetro |
|---|---|---|
| `RPRT_TBLA_ACML` — tabla de amortización | Cabecera: idAsoprep, partícipe, cédula, producto, monto, tasa, plazo, fecha de inicio, estado. Detalle por cuota: número, vencimiento, capital, interés, seguros u otros rubros si existen, cuota total y saldo de capital. Totales al pie. | `P_PRSTCDGO` |
| `RPRT_PRST_PGRE` — pagaré | Pagaré a la orden por el monto del préstamo: lugar y fecha, monto en números y en letras, deudor con cédula, plazo, tasa, número de cuotas, cuota, interés de mora según la ley, y las firmas del deudor. ⚠️ **TEXTO LEGAL PROVISIONAL** hasta que el usuario entregue el modelo de pagaré del cliente: lleva al pie «MODELO PROVISIONAL — validar con asesoría legal». | `P_PRSTCDGO` |

Los dos se generan con `POST /rest/rprt/generar` (`modulo: "crd"`) y llevan `.jrxml` en sintaxis
**compacta** más `.jasper` compilado y verificado con `verificar-fill-jasper.bat`. Si el reporte se
alimenta por parámetros y no por `<query>`, va por `generarReporteDesdeColeccion` (CLAUDE.md). Sin
`fontName="Tahoma"` donde haga falta negrita. Se pueden imprimir en cualquier estado desde GENERADO,
y la tabla también en ANULADO, con la marca «ANULADO».

## 5. DDL — `crd/sql/325` (va ANTES del WAR)

`ALTER TABLE CRD.PRST ADD (PRSTASEN NUMBER, PRSTASRV NUMBER, PRSTUSAN VARCHAR2(50), PRSTFCAN TIMESTAMP, PRSTMTAN VARCHAR2(1000))`,
más el estado **12 ANULADO** en el catálogo de donde salgan los nombres de estado del préstamo. El BE
dice cuál es ese catálogo, y el árbitro escribe el SQL. Lleva bloques de control y reverso comentado.

`aprobar` empieza a guardar `PRSTASEN` con el id del asiento de entrega.

## 6. De dónde sale la contabilidad (respuesta al usuario)

### 6.1 Asiento de entrega, al aprobar (`ContabilidadPrestamoServiceImpl.contabilizarEntrega`)

| Línea | De dónde sale la cuenta | Parametrizable en |
|---|---|---|
| **DEBE cartera por vencer, una línea por banda de plazo** | La banda de cada cuota según los días entre el inicio y su vencimiento. La cuenta es la que tenga esa banda del producto en **CRD.BNDP** (`ClasificadorBandaService`). | Créditos → parametrización de bandas por producto |
| DEBE cuenta de orden «cartera de créditos» | Línea `aux1=6` de la **plantilla contable** de la familia del producto | Contabilidad → Plantillas: alterno **9** (prendario), **13** (hipotecario), **34** (quirografario) |
| HABER documentos en garantía (el pagaré) | Línea `aux1=7` de la misma plantilla | ídem |
| HABER bien en garantía (solo prendario e hipotecario, con suma asegurada) | Línea `aux1=8` de 9 o 13; valor = `PRSTVLAS` | ídem |
| HABER **SOCIOS POR PAGAR** | Línea `aux1=9` (9 y 13) o `aux1=8` (34) | ídem |

La **familia** sale del **tipo de préstamo** del producto (CRD.TPPR → PRENDARIO, HIPOTECARIO o
QUIROGRAFARIO). Es un dato del catálogo, no del código.

### 6.2 Orden de desembolso (CXP) y asiento contra bancos

CXP arma el asiento del pago así: **DEBE la cuenta del grupo del producto de pago** y **HABER el
banco** que tesorería elige al pagar. Hoy el producto (976) está **quemado** en el código. **Cambio:**
el producto se **deduce** de la plantilla. Se busca el producto de pago vigente cuyo grupo tenga la
**misma cuenta** que la línea SOCIOS POR PAGAR de la plantilla de entrega. Así el puente cuadra por
construcción:
- si alguien cambia la cuenta en la plantilla, el desembolso la sigue solo;
- cero productos → 422 «no hay producto de pago para la cuenta X»;
- más de uno → 422 con la lista, y no se elige uno al azar.

### 6.3 Lo que sí queda fijo en el código, y por qué no es una cuenta

- Los **números alternos de plantilla** 9, 13 y 34, y las **posiciones `aux1`** de cada línea. Son la
  *llave* con la que el código encuentra la plantilla, igual que en todo el resto de `crd` y `cnt`
  (`PlantillasCredito`). La cuenta de cada posición la decide la plantilla, que se edita en pantalla.
- Los tres nombres de familia: PRENDARIO, HIPOTECARIO y QUIROGRAFARIO.
- El **tipo de asiento** CREDITOS: alterno de `TipoAsientos`.
