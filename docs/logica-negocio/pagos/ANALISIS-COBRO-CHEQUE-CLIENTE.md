# Cobro con cheque de cliente — análisis (sin implementar)

**Fecha:** 2026-08-27. **Alcance de este documento:** responde las 4 preguntas del prompt. No
crea tablas, no escribe DDL, no toca código. El modelo lo decide el usuario a partir de esto.

**Distinción de partida, para que quede escrita:** esto es un cheque que **recibimos** de un
tercero (cliente) como forma de pago de una factura de venta. Es un documento ajeno, con banco y
cuenta ajenos, que **puede rebotar**. No tiene nada que ver con el cheque que **giramos**
nosotros contra nuestra propia chequera (`TSR.CHQR`/`TSR.DTCH`, ya implementado) — ese sale de un
talonario nuestro, lo imprime el sistema, y su riesgo es que un tercero no lo cobre a tiempo, no
que rebote. Son dos ciclos de vida distintos y **no se reutiliza `TSR.DTCH` para esto**.

---

## 0. Lo que ya existe hoy (verificado contra código y contra la base local)

### 0.1 El circuito activo de cobros: `CBR.APLC` (`AplicacionPagoCxc`)

Es el mecanismo real, el que usa `AplicacionPagoCxcServiceImpl` y el que expone
`AplicacionPagoCxcRest`. Ya tiene:

- `APLCFPAG` (`formaPago`): 1=Efectivo, 2=Transferencia, 3=**Cheque**, 4=Tarjeta — el valor 3 ya
  está reservado en el comentario de la entidad, pero **nada lo produce todavía**: el único punto
  que escribe `formaPago` es `aplicarCobroTransferencia`, con `FORMA_PAGO_TRANSFERENCIA = 2L`
  fijo.
- `APLCBANC` (`banco`, `VARCHAR2(200)` libre) y `APLCREFR` (`referencia`, `VARCHAR2(200)` libre):
  genéricos, pensados para anotar "de dónde vino la plata", no para modelar un cheque (no hay
  número de cheque distinguible de la referencia, no hay fecha, no hay estado propio).
- `APLCESTD` (`estado`): sólo 1=Activo / 2=Reversado — binario, sin espacio para "en cartera",
  "depositado", "protestado".

El flujo de referencia es `aplicarCobroTransferencia` (`AplicacionPagoCxcServiceImpl:594-670`):
genera el asiento (`AsientoContableService.generarAsientoCobroTransferenciaCxc`, **DEBE banco
real / HABER cuenta del cliente**, inmediato porque una transferencia no rebota), crea la
`AplicacionPagoCxc`, y crea un `MovimientoBanco` "en tránsito"
(`TipoMovimientoConciliacion.TRANSFERENCIAS_CREDITOS_EN_TRANSITO`) para que la conciliación
bancaria lo empareje después. Un cheque de cliente **no puede copiar el DEBE inmediato a banco
real** — ver §2 — pero sí puede copiar el resto de la forma (asiento + aplicación + movimiento
banco para conciliar).

La reversión ya existe y es genérica: `revierteUnaAplicacion` (`AplicacionPagoCxcServiceImpl:
956+`) marca la aplicación REVERSADA, recalcula el saldo/estado de pago de la factura, anula el
movimiento bancario y anula el asiento. No es específica de transferencia — sirve de base para el
rechazo de un cheque (§3), aunque como se ve ahí, probablemente no baste tal cual.

### 0.2 Un subsistema de cobro con cheque YA EXISTE… y está muerto

`TSR.CBRO` (`Cobro`) tiene detalles hermanos `CobroCheque` (`TSR.CCHQ`), `CobroEfectivo`,
`CobroTarjeta`, `CobroTransferencia`, `CobroRetencion`, más `Deposito`/`DetalleDeposito`
(papeleta de depósito bancario, para agrupar varios cheques en un solo depósito),
`DesgloseDetalleDeposito`, `MotivoCobro`, y una conciliación propia (`DetalleConciliacionServiceImpl`,
`HistDetalleConciliacionServiceImpl`) — con DAO, Service y REST completos para cada uno.

**Verificado en la base local (copia de producción): `TSR.CBRO`, `TSR.CCHQ` y `TSR.DTDP` tienen
0 filas.** Es infraestructura construida y nunca adoptada — el circuito real de cobros es
`CBR.APLC`, no `TSR.CBRO`. No se recomienda revivirla ni extenderla: además de no tener
trayectoria en producción, vive en el schema `TSR` (tesorería/conciliación) en vez de `CBR`
(donde están `APLC`, `FCTR`, `NTCR`, `NTDB` — el resto del ciclo de cobro activo), y su
conciliación es la vieja (`DetalleConciliacionServiceImpl`), no la que se conectó a
`MovimientoBanco` en el trabajo de conciliación bancaria de esta misma sesión
(`ConciliacionContableMatchServiceImpl`).

**Sí vale la pena rescatar su forma**, como referencia de qué campos hacen falta:
`CobroCheque.numero` (número de cheque), `CobroCheque.bancoExterno` (FK a `TSR.BancoExterno`,
**no** texto libre), `CobroCheque.valor`, `CobroCheque.detalleDeposito` (a qué papeleta de
depósito pertenece), `CobroCheque.estado`.

### 0.3 `TSR.BancoExterno` (`TSR.BEXT`) — catálogo real, con datos, para "banco ajeno"

389 filas activas en la base local: bancos y cooperativas ecuatorianas (`BEXTCDGO`, `BEXTNMBR`,
`BEXTESTD`). Es el catálogo correcto para "banco del que viene el cheque" — mejor que el
`APLCBANC` de texto libre de hoy, que ya se usa (mal) para anotar el nombre del banco de la
cuenta bancaria propia en `aplicarCobroTransferencia` (`aplicacion.setBanco(nombreBanco(cuentaBancaria))`).

### 0.4 `MovimientoBanco` ya está preparado para un cheque, a medias

La entidad `MovimientoBanco` (`TSR.MVCB`) ya tiene `numeroCheque` (`MVCBCHQN`) y una FK a `Cheque`
(`TSR.DTCH`, el cheque **girado**) — pensados para el cheque que emitimos, no el que recibimos.
El catálogo de tipos de movimiento para conciliar (`TipoMovimientoConciliacion`, rubro 37) tiene
`CHEQUES_GIRADOS_Y_NO_COBRADOS` (2) y `CHEQUE_COBRADO` (4) — de nuevo, sólo para lo que giramos.
**No existe hoy ningún código de este rubro para un cheque recibido de un cliente** (ver §4).

---

## 1. ¿Dónde debería vivir el cheque recibido?

Tres opciones, de peor a mejor:

**(a) Reusar `TSR.CBRO`/`TSR.CCHQ`.** Descartado: §0.2. Cero trayectoria, schema equivocado,
conciliación vieja y desconectada de la que realmente se usa hoy.

**(b) Campos sueltos en `CBR.APLC`.** Insuficiente: el cheque recibido necesita su **propio**
estado (en cartera → depositado → acreditado, o rechazado en cualquier punto de ese camino),
independiente del estado binario de la aplicación (Activo/Reversado). Meter eso en `APLC` con
columnas nuevas (`APLCCHQESTD`, `APLCCHQNRO`, `APLCCHQFVN`…) infla una tabla que ya sirve a cinco
tipos de documento distintos (Nota de Crédito, Retención, Nota de Débito, Anticipo, cobro
directo) con columnas que sólo aplican a uno.

**(c) Tabla propia, FK opcional desde `CBR.APLC` — recomendado.** Es exactamente el patrón que
`CBR.APLC` **ya usa** para todo lo demás: `APLCNTCR`→`NotaCredito`, `APLCRTCM`→`RetencionCompra`,
`APLCNTDB`→`NotaDebito`, `APLCANTC`→`AnticipoCliente`, cada uno un FK opcional que sólo se llena
cuando `APLCTDPG` corresponde. Un `APLCCHQC` nuevo, apuntando a una tabla nueva (ej.
`CBR.CHQC`, "Cheque de Cliente") sigue el mismo molde, en el mismo schema que el resto del ciclo
de cobro activo (`CBR`, no `TSR`).

Esa tabla nueva necesitaría, como mínimo (sin proponer DDL, sólo el contenido):
número de cheque, banco emisor (FK a `TSR.BancoExterno`, §0.3), número de cuenta del girador
(texto — no hay catálogo de cuentas de terceros), titular del cheque si es distinto del cliente
de la factura (frecuente: un cheque puede venir a nombre de un tercero relacionado), fecha del
cheque, fecha de vencimiento/cobro si el cheque es posfechado, la `AplicacionPagoCxc` a la que
pertenece, y un **estado propio** (ver §2-§3) con su fecha de cada transición y el motivo cuando
se rechaza.

---

## 2. ¿Qué pasa contablemente al recibirlo?

**No se puede copiar el DEBE inmediato a banco real de `aplicarCobroTransferencia`.** Una
transferencia ya es dinero confirmado en la cuenta; un cheque no — puede rebotar. El plan de
cuentas de esta empresa **ya tiene la cuenta puente para esto, sin usar**:

```
1.1.02.15  REMESAS EN TRANSITO      (bajo 1.1.02 BANCOS Y OTRAS INSTITUCIONES FINANCIERAS)
```

Es una cuenta de tesorería (no de cuentas por cobrar), consistente con la práctica contable
ecuatoriana estándar: un cheque recibido y depositado, antes de que el banco lo acredite, es
"casi efectivo", se muestra junto a bancos, no mezclado con la cartera de clientes.

**Asiento propuesto al recibir (o al depositar — ver la pregunta abierta más abajo):**

```
DEBE   1.1.02.15  Remesas en Tránsito         valor del cheque
HABER  <cuenta CxC del cliente>               valor del cheque   (misma cuenta que ya
                                                                    resuelve obtenerCuentaCliente,
                                                                    rol Cliente, tipoCuenta=1)
```

La factura se da por cobrada de inmediato en el sentido de que `AplicacionPagoCxc` se crea
activa y el saldo de la factura baja — igual que con la transferencia. Lo que cambia es **contra
qué cuenta** se contabiliza: no es Banco real todavía, es la cuenta puente. Cuando el cheque se
acredita de verdad en el banco, un segundo movimiento (o el mismo asiento ajustado, según cómo se
modele — es parte de la decisión de DDL) reclasifica de Remesas en Tránsito a la cuenta bancaria
real, sin tocar de nuevo la factura ni al cliente.

**Pregunta abierta que no se puede responder sin decisión de negocio:** ¿"recibido" y
"depositado" son el mismo momento para esta empresa (el cheque se deposita el mismo día que
llega), o hay un lapso en que el cheque está físicamente en caja antes de ir al banco? El diseño
muerto de §0.2 (`Deposito`/`DetalleDeposito` agrupando varios `CobroCheque`) sugiere que en algún
momento se pensó en **dos** estados separados — "en cartera" (recibido, sin depositar) y "en
tránsito" (ya en el banco, sin acreditar) —, no uno. Si la operación real deposita el mismo día,
un solo estado/cuenta alcanza y se simplifica todo lo de abajo; si no, hace falta una cuenta o
sub-estado adicional para "cheques en cartera" antes de Remesas en Tránsito. Esto lo decide el
usuario con contabilidad, no el código.

---

## 3. ¿Cómo se modela el rechazo del cheque?

Dos piezas, no una sola:

**(1) Reversar el cobro** — reutilizar `revierteUnaAplicacion` (§0.1), que ya hace exactamente lo
que hace falta desde el punto de vista de la factura: la marca REVERSADA, recalcula el saldo de
la factura (vuelve a quedar pendiente de cobro — correcto, el cliente sigue debiendo), anula el
movimiento bancario si ya se había creado uno, anula el asiento original. No hace falta
reinventar esto, sólo verificar que no dependa de nada específico de `tipoDocPago=ANTICIPO` para
el camino de cheque (ya está separado por `if`, §0.1).

**(2) Registrar el motivo y dejar rastro contable del rechazo, no sólo deshacerlo.** Aquí es
donde entra la otra cuenta que el plan ya tiene lista y sin usar:

```
1.4.90.05  CHEQUES PROTESTADOS Y RECHAZADOS   (bajo cuentas y documentos por cobrar)
```

Que exista ya, con ese nombre exacto, es una señal fuerte de que el criterio de la empresa es
**no** limpiar el rechazo como si nunca hubiera pasado: el valor se reclasifica a esta cuenta
específica en vez de simplemente devolverlo a la cuenta genérica de CxC del cliente, para que
quede visible en el balance como cartera problemática hasta que se resuelva (cobro en efectivo,
canje, o castigo). Dos formas de combinarlo con (1), a decidir:

- **(a)** Reversar tal cual (factura vuelve a "pendiente" en el sentido normal), y el asiento de
  reversión usa como contrapartida `1.4.90.05` en vez de volver a la cuenta CxC normal del
  cliente — es decir, el asiento de rechazo es `DEBE Cheques Protestados / HABER Remesas en
  Tránsito`, no una reversión que vuelva literalmente al asiento original. La factura queda
  "impaga" de nuevo pero el sistema sabe que la razón es un cheque rechazado, no un cobro
  simplemente deshecho.
- **(b)** Dejar la factura como pagada (no reversar la aplicación) y mover sólo el valor de
  Remesas en Tránsito a Cheques Protestados como una cuenta por cobrar aparte, fuera de la
  cartera normal de facturas — la factura ya no se ve como pendiente en los reportes de cartera
  por vencer, pero hay $X en una cuenta de "cheques rechazados" para gestión de cobranza. Esto es
  más fiel a como suelen llevarlo los contadores (la factura *sí* se pagó según lo acordado, lo
  que falló fue el instrumento de pago), pero requiere que los reportes de cartera consulten
  también esta cuenta para no subestimar el riesgo.

La opción **(a)** es más simple de implementar (una sola fuente de verdad: si la factura está
pendiente, está pendiente) y más coherente con `revierteUnaAplicacion` tal como existe hoy. La
**(b)** es contablemente más purista pero pide más trabajo (nuevo reporte de cheques protestados,
o extender el de cartera). Recomendación: (a) para la primera versión, dejando (b) como mejora
si contabilidad la pide después de ver el reporte de cartera con rechazos mezclados.

En cualquiera de las dos, el **motivo del rechazo** (protesto por fondos insuficientes, cuenta
cerrada, firma disconforme, etc.) necesita guardarse — un campo de texto o, mejor, un rubro nuevo
si contabilidad maneja una lista cerrada de motivos (paralelo a `MotivoAnulacionCheque`, que ya
existe para el cheque girado — otra pieza reusable como referencia de forma, no de datos).

---

## 4. ¿Esto toca la conciliación bancaria? Sí, en dos puntos

**(1) El catálogo de tipos de movimiento no tiene código para esto.** `TipoMovimientoConciliacion`
(rubro 37) tiene el par `CHEQUES_GIRADOS_Y_NO_COBRADOS`(2)/`CHEQUE_COBRADO`(4) para el cheque que
giramos, y `TRANSFERENCIAS_CREDITOS_EN_TRANSITO`(10)/`TRANSFERENCIAS_CREDITOS`(12) para
transferencias recibidas — pero nada para "cheque de cliente en tránsito" ni "cheque de cliente
acreditado". Hace falta un par nuevo, simétrico al de transferencias, para que
`MovimientoBanco` pueda representar el estado del cheque recibido de la misma forma en que ya
representa el de una transferencia.

**(2) El mecanismo de emparejamiento ya construido esta misma sesión aplica igual.**
`ConciliacionContableMatchServiceImpl.conciliarGrupo`/`deshacerGrupo` (el trabajo de "Conciliación:
conectar los movimientos bancarios al cierre del grupo" de este mismo hilo) ya sabe mover un
`MovimientoBanco` de "en tránsito" a "definitivo" cuando aparece en el extracto bancario, y
viceversa al deshacer. Un `MovimientoBanco` creado para el cheque de cliente (igual que
`creaMovimientoPorTransferencia` crea uno para la transferencia, ver §0.1) entra en ese mismo
flujo sin cambios adicionales — **siempre que** se le dé el tipo de movimiento correcto del punto
(1) y, si aplica la reclasificación de Remesas en Tránsito a Banco real (§2), que ese segundo
asiento sea el que efectivamente aparece en el extracto y se concilia, no el primero.

**Lo que NO toca:** el cheque **girado** (`TSR.DTCH`/`CHQR`) y su conciliación
(`CHEQUES_GIRADOS_Y_NO_COBRADOS`/`CHEQUE_COBRADO`) — circuito totalmente aparte, sin cambios.

---

## 5. Resumen para decidir

| Pregunta | Recomendación | Alternativa considerada y por qué no |
|---|---|---|
| ¿Dónde vive? | Tabla nueva `CBR.CHQC` (o similar), FK opcional desde `CBR.APLC` (`APLCCHQC`), mismo patrón que NC/retención/ND/anticipo | `TSR.CBRO`/`CCHQ`: muerto, 0 filas, schema y conciliación equivocados. Columnas sueltas en `APLC`: infla una tabla ya multipropósito |
| ¿Cuenta al recibir? | `1.1.02.15 Remesas en Tránsito` (ya existe) como puente, en vez de Banco real | DEBE directo a Banco real (como transferencia): ignora el riesgo de rebote que el propio plan de cuentas ya contempla |
| ¿Rechazo? | Reversar con `revierteUnaAplicacion` + asiento de rechazo contra `1.4.90.05 Cheques Protestados y Rechazados` (ya existe) en vez de volver sin más a la cuenta CxC genérica | Reversión "silenciosa" sin dejar rastro en una cuenta específica: contradice que esa cuenta ya exista con ese nombre exacto en el plan |
| ¿Conciliación? | Nuevo par en `TipoMovimientoConciliacion` (rubro 37) + reutilizar `MovimientoBanco` y `ConciliacionContableMatchServiceImpl` tal cual | — |

**Pendiente de decisión del usuario, no resuelto aquí:** si "recibido" y "depositado" son un solo
momento o dos (§2, pregunta abierta) — de eso depende si hace falta una cuenta/estado adicional
para "cheques en cartera" antes de Remesas en Tránsito, y por lo tanto cuántas columnas de fecha
lleva la tabla nueva.

---

## Respuesta del usuario (2026-08-27) — la pregunta abierta queda cerrada

> *"Actualmente el cliente no recibe pagos en cheque, pero si lo hiciera alguna vez puede pasar
> que recibe hoy y deposita en 2 o 3 días."*

**Son dos momentos distintos, no uno.** Por lo tanto el modelo necesita **dos estados puente**, no
uno:

| Estado | Cuándo | Cuenta contable | Fecha que lo marca |
|---|---|---|---|
| **1 · En cartera** | Se recibió el cheque, todavía no se deposita | *Cheques en cartera* — **hay que crearla**, no existe en el plan | `fechaRecepcion` |
| **2 · Depositado** | Se llevó al banco, el banco aún no lo acredita | `1.1.02.15 Remesas en Tránsito` — ya existe | `fechaDeposito` |
| **3 · Acreditado** | El banco lo acreditó | Banco real | `fechaAcreditacion` |
| **4 · Rechazado** | Rebotó | `1.4.90.05 Cheques Protestados y Rechazados` — ya existe | `fechaRechazo` |

Consecuencias para el modelo propuesto:

1. **Cuatro fechas en la tabla nueva**, no dos. Cada transición se sella con la suya.
2. **Falta una cuenta contable en el plan**: *Cheques en cartera*. Las otras tres ya están. Cuando
   se implemente, hay que crearla antes — es un dato de parametrización, no de código.
3. **El cheque en cartera no es un activo bancario**: no genera `MovimientoBanco` todavía. El
   movimiento bancario nace al **depositar**, no al recibir. Esto importa: si se creara al recibir,
   el saldo disponible de la cuenta se inflaría con dinero que el banco no tiene, que es
   exactamente el problema que ya arrastran los 121 movimientos sin conciliar.
4. **La conciliación solo ve el estado 2 en adelante.** Un cheque en cartera no es una partida en
   tránsito bancaria: no está en el banco ni debería estarlo. Ver
   [`DISENO-CONCILIACION-PARTIDAS-EN-TRANSITO.md`](../tsr/DISENO-CONCILIACION-PARTIDAS-EN-TRANSITO.md).

**Sigue diferido**, no descartado: el cliente actual no cobra con cheque, pero el producto se va a
vender a otros que sí. Cuando se retome, este análisis y esta decisión ya están tomados.
