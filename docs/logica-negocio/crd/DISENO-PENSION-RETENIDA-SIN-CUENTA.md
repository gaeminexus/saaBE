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
