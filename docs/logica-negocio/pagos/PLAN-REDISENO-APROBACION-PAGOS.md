# Rediseño del circuito de pagos: solicitud sin cuenta, aprobación con cuenta

**Punto 14 del listado del 2026-08-27** · Tamaño XL · **Fecha:** 2026-08-27
**Rol de este documento:** diseño y plan de fases, escrito por el orquestador. El DDL y los prompts salen de aquí.

---

## 1. Lo que pide el negocio

> *"Pueden llegar solicitudes de pago de CxP, RRHH y créditos. La idea es que desde cualquier módulo se pueda receptar la solicitud de pago y en la pantalla de aprobaciones se escoja una cuenta bancaria y todos los pagos que se van a realizar con esa cuenta; el sistema debe validar si alcanza el saldo. Que se aprueben ahí los pagos y de ahí se generen los archivos para pagar o que se pueda procesar manualmente. También ahí se indicaría si se paga con cheque o transferencia, y en caso de cheque que se genere el cheque. Y que permita seleccionar varios pagos a incluir en una sola transferencia."*

Cuatro cambios, en orden de dependencia:

1. La **cuenta bancaria deja de elegirse al registrar** y se elige **al aprobar**.
2. La aprobación **valida el saldo** de esa cuenta contra el total seleccionado.
3. La **forma de pago se decide al aprobar**, y con cheque se gira el cheque ahí.
4. Varios pagos al **mismo beneficiario y cuenta destino** pueden ir en **una sola transferencia** del archivo del banco.

---

## 2. Lo que ya existe (verificado en el código)

| Pieza | Estado |
|---|---|
| `PagoProgramado` (`PGS.PGTR`) como documento único de salida de dinero | ✅ lo usan CxP (facturas), tesorería (egresos), anticipos a proveedor, caja chica y CRD (devolución de aportes) |
| Origen externo (`OrigenPagoExterno`) para que otros módulos entreguen pagos sin que CxP los conozca | ✅ probado con caja chica |
| Pago con cheque, con giro y anulación por reverso | ✅ probado de punta a punta |
| Lote y archivo del banco (`LotePago`, `generarLote`, `procesarRespuestaBanco`) | ✅ funciona, con un formateador único |
| Saldo de una cuenta a una fecha (`CuentaBancariaServiceImpl.obtieneSaldoFecha`) | ✅ existe: cierre de `SaldoBanco` + movimientos del período |
| Estados: `REGISTRADO(1) · EN_ARCHIVO(2) · CONFIRMADO(3) · RECHAZADO(4) · ANULADO(5)` | ✅ |

**Lo que falta:** un estado anterior a `REGISTRADO` (la solicitud sin cuenta), que `PGTRCNBC` admita nulo, el endpoint de aprobación masiva, la validación de saldo y la agrupación por beneficiario.

**Entidades legadas que NO se usan** (verificado: sin llamadores fuera de su propio ServiceImpl): `AprobacionXProposicionPago`, `ProposicionPagoXCuota`, `AprobacionXMonto`, `MontoAprobacion`, `UsuarioXAprobacion`. **No reutilizarlas**: modelan un circuito de aprobación por montos y usuarios que nadie mantiene. Si más adelante se quiere aprobación por monto y perfil, se rediseña aparte.

---

## 3. Decisiones de diseño

### 3.1 Un estado nuevo: `POR_APROBAR = 0`
La solicitud nace **sin cuenta bancaria y sin forma de pago**. Se usa `0` y no `6` para que el orden numérico siga el ciclo de vida y las consultas por `estado >= 1` (pagos ya con cuenta) sigan valiendo.

Ciclo nuevo:
```
POR_APROBAR(0) --aprobar--> REGISTRADO(1) --lote--> EN_ARCHIVO(2) --respuesta--> CONFIRMADO(3)
                       \--(cheque o débito automático)--> CONFIRMADO(3)
```

### 3.2 Los cheques se confirman al APROBAR
Decisión del usuario. Cambia lo probado en la fase A: hoy el cheque se gira y contabiliza al **registrar**; pasará a girarse y contabilizarse al **aprobar**. Es coherente: el cheque se gira cuando tesorería decide pagarlo, no cuando el área lo solicita.
**Consecuencia:** las tres pantallas de origen (pagos CxP, egresos, anticipos) dejan de mostrar el selector de forma de pago y el aviso "se girará el cheque N° X". Todo eso se muda a la pantalla de aprobación.

### 3.3 Validación de saldo: informativa y bloqueante
Al elegir la cuenta, la pantalla muestra: **saldo disponible** (`obtieneSaldoFecha` a hoy), **comprometido** (suma de pagos ya aprobados de esa cuenta que aún no se confirman) y **disponible real** = saldo − comprometido. Si el total seleccionado lo supera, **la aprobación se rechaza** con el detalle de los tres números.
El comprometido es imprescindible: sin él, dos aprobaciones seguidas pasan la validación cada una por su lado y juntas sobregiran la cuenta.

### 3.4 Agrupación en una sola transferencia
Al generar el lote, los pagos con **el mismo beneficiario y la misma cuenta destino** se emiten como **una línea** del archivo, sumando los valores. El `LotePago` sigue teniendo N pagos; lo que se agrupa es la línea del archivo. Cada pago conserva su asiento y su aplicación: **no se fusionan documentos**, solo la instrucción al banco.
Requiere que el formateador reciba las líneas ya agrupadas y que la conciliación posterior sepa repartir un débito bancario entre N pagos — por eso va en la última fase.

### 3.5 Qué NO cambia
- El asiento y el movimiento bancario de cada pago siguen generándose igual, por pago.
- El reverso sigue siendo por pago.
- Los orígenes externos siguen entregando pagos con `registrarPagoDeOrigenExterno`; solo dejan de pasar cuenta.

---

## 4. Fases

### Fase 1 — DDL + backend del ciclo nuevo (M)
- `PGTRCNBC` pasa a nullable (columna y entidad).
- Estado `POR_APROBAR = 0` en `EstadoPagoProgramado` y en el rubro correspondiente.
- Los cuatro `registrar*` aceptan cuenta nula → nace `POR_APROBAR`. Las firmas actuales (con cuenta) siguen funcionando y nacen `REGISTRADO`, para no romper nada mientras el frontend migra.
- `POST /pgtr/aprobar` con `{idsPagos, idCuentaBancaria, formaPago, fechaPago, idUsuario}`: valida que todos estén `POR_APROBAR`, valida saldo, asigna cuenta y forma de pago, gira cheque si corresponde, y deja cada pago en `REGISTRADO` (transferencia) o `CONFIRMADO` (cheque o débito automático, contabilizando en el acto).
- `GET /pgtr/porAprobar?idEmpresa&origen&desde&hasta`: la bandeja, con origen (CxP, RRHH, CRD, caja chica), beneficiario, concepto, valor y fecha solicitada.
- `GET /pgtr/disponibilidad/{idCuenta}?fecha`: saldo, comprometido y disponible real.

### Fase 2 — Pantalla de aprobación (M)
Bandeja única con filtros por origen y fecha, selección múltiple, total seleccionado en vivo, selector de cuenta con los tres números de disponibilidad, selector de forma de pago (cheque solo si la cuenta maneja chequera) y botón aprobar. Al aprobar con cheque, muestra los números girados.
Las pantallas de origen dejan de pedir cuenta y forma de pago.

### Fase 3 — Agrupación en el archivo (S)
`generarLote` agrupa por beneficiario + cuenta destino al construir el archivo. Ajustar el formateador y documentar cómo se concilia un débito agrupado.

### Fase 4 — Orígenes nuevos (S cada uno)
RRHH entrega al circuito: nómina y anticipos a trabajadores (este último ya diseñado, ver `rhh/prompts/08`). CRD ya entrega devolución de aportes.

---

## 5. Riesgos

1. **Toca el archivo más caliente del sistema.** `PagoProgramadoServiceImpl` concentra cheques, caja chica, anticipos y liquidaciones. **No empezar hasta que la tanda actual esté en producción y estable.**
2. **Cambia un flujo recién probado** (cheque al registrar → al aprobar). Hay que volver a probar la fase A completa después.
3. **Pagos en vuelo durante el despliegue**: los que estén en `REGISTRADO` con cuenta ya asignada siguen su curso normal; el estado nuevo solo aplica a los que nazcan después. No hace falta migrar datos.
4. **La validación de saldo depende de `MovimientoBanco`**, que hoy acumula movimientos en tránsito que nadie cierra. **Conviene cerrar antes el trabajo de conciliación** (conectar el cierre de movimientos), o el "disponible" será optimista.

---

## 6. Dependencia con conciliación

El punto 12 (conciliación) y este comparten `MovimientoBanco`. El orden correcto es:

```
conciliación: conectar el cierre de movimientos  →  saldo confiable
                     ↓
rediseño de pagos: validación de disponibilidad
```

Hacerlo al revés significa validar saldo contra una cifra que arrastra tránsitos de meses.
