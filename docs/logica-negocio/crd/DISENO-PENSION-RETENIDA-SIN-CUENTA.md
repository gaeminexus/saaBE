# DISEÑO — Pensión retenida de jubilados sin cuenta bancaria: no se contabiliza hasta pagarse

omen-saa-1-arb · 2026-11-09 · **Aprobado por el usuario (2026-11-09): D1 una orden por mes, D2 corrida + botón, D3 los históricos se dejan como están (opción B).** Ejecutor: omen-saa-1-be (y -fe si
se aprueba el botón de D2).

## 1. Pedido (palabras del usuario)

> «para los jubilados que no tienen cuenta bancaria, igual se está contabilizando el envío a pagar, pero no aparece en el
> módulo de pagos […] a los que no tienen cuenta bancaria registrada, se le puede pagar seguro (si tienen), se puede
> cruzar los valores con préstamos, pero no se debe contabilizar el saldo como que fuera a pagarse, más bien ese saldo se
> debe mantener para que cuando registre la cuenta bancaria, se le paguen todos los saldos acumulados»

## 2. Hoy (verificado en `PagoPensionComplementariaServiceImpl`, `main` 178d75a8)

Corrida de pensiones → `generarMesesRetroactivos` → `registrarPgpcDelMes` (~2112), por jubilado y mes:
- Seguro (débito ya hecho en el proceso de seguro) y cruce con préstamos (`pagarConAportes`): **correctos, no cambian**.
- Remanente de pensión sin «salida» (`resolverCuentaSalida` = null: sin cuenta activa, más de una activa, o sin
  certificado): **no** se crea el negativo del aporte 23 (el saldo queda en el módulo), **no** hay orden en CXP, PGPC queda
  `1 REGISTRADA`…
- …**pero** `generarAsientoDevengoPension(pago, entidad, idEmpresa, remanente)` (~2251) se llama igual con el remanente:
  **D 2.1.02.25.01 / H 2.3.01.10.03**. El mayor baja la cuenta individual y muestra un «por pagar» que tesorería nunca va a
  pagar; el módulo sigue mostrando ese saldo en el aporte 23. **Descuadre módulo-mayor** por cada mes retenido.
- Ese mes queda cubierto (PGPC completo + el ancla avanza con el seguro/cruce): **el retenido nunca se vuelve a pagar**.
- No existe columna con el monto retenido (PGPCVLPN es la pensión nominal; el cruce no se guarda en PGPC).
- Fuera de alcance: el re-proceso C20 (`reprocesarDevengoJubilado`/`reprocesarPagoJubilado`) — sí debita el neto y lo paga
  en un segundo paso; y el jubilado sin préstamo, sin cuenta y sin seguro (hoy no se le genera PGPC y acumula por ancla, que
  ya es el comportamiento que el usuario pide).

## 3. Cambio

### 3.1 Corrida (mes con remanente y sin salida)
- Seguro y cruce: igual que hoy.
- Remanente: **sin negativo, sin orden y sin línea de pensión en el devengo** (si el devengo queda sin líneas, no se graba
  asiento — regla D-034 vigente).
- PGPC `1 REGISTRADA` con el monto retenido en columna nueva **`PGPCVLRT NUMBER(18,2)`** (DDL 374, con respaldo y reverso).
- **Reserva:** `saldoLibre` de los meses siguientes descuenta los retenidos pendientes (como hoy `seguroReservado`), para que
  un cruce posterior no se coma la pensión que se le debe.

### 3.2 Pago de lo acumulado (cuando ya tiene salida: una sola cuenta activa con certificado)
Por cada PGPC `REGISTRADA` con `PGPCVLRT > 0` y sin orden, en orden de período, en la transacción del jubilado:
1. Negativo del aporte 23 por `PGPCVLRT` (revalida saldo con bloqueo, `crearMovimientoNegativo`).
2. Devengo **D 2.1.02.25.01 / H 2.3.01.10.03** por `PGPCVLRT`, fechado el día del pago (mes abierto), glosa «pensión
   retenida m/aaaa».
3. Orden en CXP origen `CRD_PAGO_PENSION_COMPLEMENTARIA`, idOrigen = ese PGPC, producto de pago de pensión → `2 EN_PAGO`.
   Confirmación, rechazo (contra-movimiento) y sincronización: los de siempre, sin cambio.
- **Cuándo (D2, decidido a + b):** (a) en cada corrida de pensiones, para todo jubilado con salida, antes de los meses de
  la corrida; (b) `POST /rest/pgpc/pagarRetenidos?idEntidad&idEmpresa&usuario` (REQUIRES_NEW, mes abierto, usuario
  validado) y botón «Pagar pensiones retenidas» en la pantalla de pago de jubilados, visible cuando el jubilado tiene
  retenidos pendientes. Devuelve lo pagado por mes y lo que no se pudo (sin salida, saldo insuficiente).

### 3.2bis Reglas fijadas al implementar (2026-11-09, aprobadas por el árbitro)
- **Ancla:** el negativo del retenido pagado lleva fecha del mes de pago; si contara para el ancla del retroactivo (último negativo del aporte 23), la corrida del mes en curso quedaría AL_DIA y su pensión no se generaría. `selectFechaUltimoMovimientoNegativo` lo excluye (PGPC con `PGPCVLRT` > 0 y negativo que no sea de seguro). Siguen contando el negativo del seguro de un mes retenido (también el de los históricos `PGPCDVRT = 1`) y, como antes, se excluye el del stub de seguro.
- **Glosa:** «PAGO PENSION RETENIDA m/aaaa - Entidad n», sin el sufijo de seguro.
- **Enlace:** al pagar, `PGPCIDAP` se re-apunta al negativo del retenido, así el rechazo (contra-movimiento) y `netoDelPgpc` devuelven `PGPCVLRT` y no el seguro. El negativo del seguro de ese mes queda en APRT sin enlace desde la fila.
- **Reserva en el bucle:** el remanente retenido de un mes resta de `saldoLibre` para los meses siguientes de la misma corrida.
- **Rechazo:** ~~sale de los pendientes y no se reintenta~~ — **reemplazado por §3.4** (decisión del usuario 2026-11-09).
- **PGPCNMDV:** el devengo del retenido se guarda ahí solo si está libre (puede traer el asiento del seguro); el código del asiento va siempre en la respuesta y el PGPC en la glosa.
- **Saldo insuficiente al pagar:** se corta en el primer mes que no alcanza (se paga en orden de período, no se salta un mes) y el resto va como no pagado con su motivo.


### 3.4 Pago rechazado y proceso de pendientes (decisión del usuario 2026-11-09)

> «El pago rechazado se queda pendiente, pero se debe poder procesar el mismo mes solo los pendientes no todo el padrón.
> El de pensiones retenidas que sea con cada fila del listado»

- **Reemplaza la regla de rechazo de §3.2bis.** Toda pensión de jubilado cuyo pago termina RECHAZADA (la del mes, una
  retenida pagada o un re-proceso C20) queda **pendiente**: tras el contra-movimiento de siempre, `PGPCVLRT` = el valor
  devuelto, `PGPCDVRT = 1` (su «por pagar» ya está en el mayor: el pago nunca se confirmó, no hay asiento de pago que
  revertir — el ejecutor lo verifica en CXP) y vuelve a contar como pendiente (el ejecutor elige: volver a 1 REGISTRADA
  sin `PGPCIDPG`, o incluir RECHAZADA en los pendientes; la orden rechazada sigue trazable por idOrigen = PGPC).
- **Ancla:** la exclusión pasa a ser por la glosa propia del pago del retenido («PAGO PENSION RETENIDA …»), no por
  `PGPCVLRT > 0`: el negativo ORIGINAL de una pensión rechazada sí debe seguir contando (si no, el ancla retrocedería).
- **Proceso de pendientes:** `POST /rest/pgpc/procesarPendientes?idEmpresa&anio&mes&usuario` paga, para todo jubilado con
  pendientes y salida válida, sus pendientes (la misma lógica de §3.2, una orden por mes). No recorre el padrón ni toca
  seguro, cruce ni pensión del mes; se puede correr las veces que haga falta, también con las pensiones del mes ya
  cerradas en la corrida (CRJB). Mes abierto (C17), usuario validado, una transacción por jubilado; devuelve el resumen
  pagado / no pagado con motivo.
- **Listado:** lectura en bloque `GET /rest/pgpc/retenidos?idEmpresa` (por jubilado: total pendiente, meses, tieneSalida,
  motivo) para mostrar el botón «Pagar pendientes» en cada fila sin una consulta por jubilado. El botón por fila usa el
  `POST pgpc/pagarRetenidos` individual que ya existe.

### 3.3 Históricos (los que ya se devengaron) — `crd/sql/373` los mide
- Backfill de `PGPCVLRT` = HABER 2.3.01.10.03 de su asiento de devengo y **`PGPCDVRT = 1`** (columna nueva
  `NUMBER(1)`, «el retenido ya está devengado»), en el DDL 374, con control.
- **D3-B (decisión):** no se reversan. Al pagarlos (3.2) se hace el negativo y la orden, **sin el paso 2 (devengo)**: el
  «por pagar» ya existe y lo cierra la confirmación de tesorería (D 2.3.01.10.03 / H banco). Las filas nuevas nacen con
  `PGPCDVRT = 0` y sí llevan el devengo del paso 2.

## 4. Decisiones del usuario
- **D1 — una orden por mes retenido** (recomendado: reusa el idOrigen = PGPC, rechazo y conciliación por mes) **o una sola
  orden por el total acumulado**.
- **D2 — cuándo se paga lo acumulado:** (a) automático en la siguiente corrida de pensiones; (b) además un botón
  «Pagar pensiones retenidas» en el jubilado para pagarlo apenas registre la cuenta (recomendado a + b).
- **D3 — históricos ya devengados:** (A) asiento de reverso **D 2.3.01.10.03 / H 2.1.02.25.01** fechado en el mes abierto,
  así el mayor cuadra con el módulo hoy y al pagar se devenga como en 3.2 (recomendado); (B) dejarlos y, al pagar, no
  volver a devengar (el mayor sigue descuadrado hasta que se paguen).

## 5. Riesgos
- Contabilidad: hasta que se paguen, los históricos siguen como «por pagar» en el mayor (decisión D3-B); el reporte de
  `crd/sql/373` bloque 2 es la lista para contabilidad.
- Pantalla/previsualización de la corrida: mostrar «retenido» y «retenido acumulado pagado» (el ejecutor revisa
  `previsualizarCorrida` para que diga lo mismo que la corrida).
- Nada de esto se prueba en producción con pagos de prueba: humo solo de lectura.
