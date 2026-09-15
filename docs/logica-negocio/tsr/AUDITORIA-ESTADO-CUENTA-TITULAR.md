# AUDITORÍA — Estado de cuenta del titular (qué lo alimenta y qué falla)

**Equipo:** `omen-saa-2` · **Creado:** 2026-09-15 · **Origen:** pedido urgente del usuario, derivado por `omen-saa-1-arb`
y confirmado por el usuario.

> *«Sigue teniendo problemas en los estados de cuenta de los titulares: retenciones que no aparecen o que no se aplican a
> las facturas, notas de venta o liquidaciones de compra; pagos que no se reflejan; anticipos que no se muestran. Revisar a
> detalle todas las opciones que deben alimentar el estado de cuenta y validar si lo están alimentando correctamente.»*

**Pantalla:** `saaFE tsr/service/estado-cuenta-titular.service.ts` — `fuentes(rol)` enumera las tablas que se consultan;
el saldo de cada factura sale de las aplicaciones (`/aplc/saldo/{id}` en cliente, `/aplp/saldo/{id}` en proveedor).

---

## A. Rol CLIENTE — medido 2026-09-15 (solo lectura, verificado por el árbitro en los puntos marcados ✔)

Saldo de factura de venta = `total − Σ CBR.APLC.montoAplicado (estado 1)` (`AplicacionPagoCxcServiceImpl.saldoFactura:809`).

| Flujo | ¿Crea `CBR.APLC`? | ¿Se ve? | Veredicto |
|---|---|---|---|
| Factura de venta (`CBR.FCTR`) | — | Sí; oculta 0 y 6 | 🟡 se listan y suman como deuda facturas **no autorizadas** (1/3/4) |
| Cobro CXC por transferencia (`/aplc/cobroTransferencia`) | Sí | Sí | ✅ |
| Cobro de caja TSR (`TSR.CBRO`) / ingreso (`TSR.INGR`) | **No** — ningún servicio de `ejb/tsr` crea `APLC` | No | 🔴 **brecha** si caja cobra facturas — **pregunta al usuario** |
| Retención recibida (`PGS.RCV2`, campo `proveedor` = cliente) | Sí, con `generaConta=1` | Sí | ✅ fuente y campo correctos, **pero** C2 y C3 |
| Retenciones antiguas (`PGS.RTCM`) | Sí | **La línea no** (no es fuente) | 🟡 la factura aparece abonada por algo que no se lista |
| NC/ND de venta | Sí, tras autorizar y con asiento | Sí | 🟡 se listan NC/ND que no rebajaron (no autorizadas o sin asiento) sin decirlo |
| Anticipo de cliente (`CBR.ANTC`) | Sí al cruzar (tipo 4, `anticipoOrigen`) | Sí | 🔴 C4, C5, C9 |

### Defectos, del más grave al menos grave

| # | Defecto | Evidencia | Arreglo | Ítem |
|---|---|---|---|---|
| **C2** | La retención recibida puede quedar en **otro titular** que la factura: `rc.setProveedor(cliente)` con el titular de `buscarTitularPorRuc` (primero por RUC, sin orden), mientras la aplicación se amarra a la factura resuelta **sin titular**. Con titulares duplicados, o factura a cédula y retención con RUC, la factura queda abonada y la retención no se lista para ese titular | ✔ `ProcesoCargaDocumentosServiceImpl:~3480`, ✔ `AplicacionPagoCxcServiceImpl:184` (`resolverFacturaPorNumero(numero, null, idEmpresa)`) | El `proveedor` de la retención = titular de la factura sustento cuando se resuelve | BE-15 |
| **C3** | La retención puede aplicarse a una factura **anulada o no autorizada**: `selectFacturaByNumero` no filtra estado | `AplicacionPagoCxcDaoServiceImpl:140-172` | Filtrar autorizada y no anulada (criterio de `CriterioVentaVigente`) | BE-16 |
| **C4** | **Aviso falso** «No se pudieron consultar: Anticipos de cliente» en todo cliente sin anticipos: el mensaje «no devolvió registros» (con tilde, otra frase) no calza con `esRespuestaVacia` | ✔ `AnticipoClienteServiceImpl:139`, ✔ FE `:228-230` | Detección tolerante a tildes y a las dos frases | FE-5 |
| **C5** | Anticipos **migrados** (estado 4, valor < 0, saldo 0) salen como anticipos fantasma | ✔ FE `estadosAnulados: [3]`; `pagos/MIGRACION-CRUCES-ANTICIPO.md:215-219` | `estadosAnulados: [3, 4]` | FE-5 |
| **C6** | Anticipo en estado 1 (ingresado, sin asiento) se suma como saldo a favor | `AnticipoClienteServiceImpl:185`, componente `:133` | **Pregunta al usuario** | — |
| **C7** | Facturas/NC/ND no autorizadas se listan y suman | FE `:97/:103/:109` | Sólo estado 5 suma; el resto se lista marcado | FE-5 |
| **C8** | Retenciones de `PGS.RTCM` no se listan aunque abonan | — | Agregar la fuente si existe su servicio en el FE | FE-5 |
| **C9** | El abono de un anticipo sale sin número: se lee `abono.anticipo` y el cruce llena `anticipoOrigen` | componente `:399-403` | Leer también `anticipoOrigen` | FE-5 |

**Hallazgo lateral:** `CBR.APLC` también aplica sobre `CBR.LQCS` (liquidación emitida), tabla que ninguna fuente lee; el
rol proveedor usa `PGS.LQCC`. Se revisa en la sección B.

---

## B. Rol PROVEEDOR — medido 2026-09-15 (verificado por el árbitro en los puntos marcados ✔)

Saldo de factura de compra = `total − Σ PGS.APLP activas` (`AplicacionPagoCxpServiceImpl:1065-1084`). El resumen de la
pantalla toma el saldo pendiente **sólo** de las filas tipo FACTURA (FCTC y LQCC).

| Flujo | ¿Crea `PGS.APLP`? | ¿Se ve? | Veredicto |
|---|---|---|---|
| Factura (FCTC 01) y nota de venta (FCTC 02) | Recibe APLP en `APLPFCTC` | Sí | ✅ (la nota de venta rotulada «Factura») |
| **Liquidación (LQCC)** | **Sí** — ✔ `APLPLQCC` existe (`AplicacionPagoCxp.java:111`), ✔ `/aplp/liquidacion/{id}` y `/aplp/saldoLiquidacion/{id}` existen | **No**: ✔ FE fuerza `saldo = total`, `aplicado = 0` (`:300-309`), con un comentario viejo («APLP no tiene FK a LQCC»). Al expandir, pide `/aplp/factura/{idLQCC}` → **abonos de una FCTC ajena con el mismo id** | 🔴 **P1** |
| Retención V2 sobre factura/nota de venta | APLP tipo 3 contra FCTC | Sí | ✅ con condiciones: exige asiento (`generaConta=1`) y saldo; si el cruce falla queda `cruceFacturaPendiente` y **la factura no baja** (P3) |
| **Retención V2 sobre liquidación (03), NC (04), ND (05)** | **Nunca**: PASO 0.1 y el cruce buscan sólo en FCTC | Con `generaConta=1` la emisión se corta («No existe la factura de compra»); con 0, sale sin cruce | 🔴 **P2** |
| Pago programado CONFIRMADO | Factura/NV: APLP tipo 1; liquidación: APLP contra LQCC (`cd800803`); anticipo y egreso: sin APLP | Factura sí; **liquidación no** (P1); **POR_APROBAR/REGISTRADO/EN_ARCHIVO en ninguna parte** | P1 · **P5** |
| Caja chica que paga un documento | APLP tipo 6 contra FCTC o LQCC | Factura sí; liquidación no | P1 |
| NC/ND de compra | APLP tipo 2/5 contra FCTC | Sí | ✅ sobre factura; sobre liquidación falla (búsqueda sólo FCTC) |
| Anticipo a proveedor (`PGS.ANTP`) | Cruce: APLP tipo 4 con `anticipoOrigen` | Sí; nombres de campo correctos | 🔴 **P4**, **P7**, P8 |
| Retención V1 (`CBR.RTNC`), pago legacy `TSR.PGSS` | No | No | ⚪ sólo históricos; sin FK para enlazar |

### Defectos, del que más plata mueve al que menos

| # | Defecto | Arreglo | Ítem |
|---|---|---|---|
| **P1** | **La liquidación de compra está ciega en el estado de cuenta**: ni pagos, ni cruces de anticipo, ni caja chica bajan su saldo; y al expandirla muestra abonos de otra factura | FE: para LQCC, `/aplp/saldoLiquidacion/{id}` y `/aplp/liquidacion/{id}`; el discriminador es la fuente, no `consultaSaldo` | FE-6 |
| **P2** | **Retención V2 sobre liquidación no se puede emitir o nunca se cruza** | BE: resolver el documento sustento según `tipoDocReten` — 01/02 en FCTC, 03 en LQCC — en PASO 0.1 y en `aplicarRetencionEmitida` | BE-17 |
| **P3** | Retención autorizada con **cruce pendiente**: la retención aparece restando y la factura no baja. Pasa sin asiento, con `generaConta=0`, o si el pago se programó por el total antes de la retención | Medir con `e2-49`; reproceso con el botón Contabilizar ya existente | `e2-49` |
| **P4** | Anticipo **INGRESADO sin pagar** suma como saldo a favor (nace con `saldo = valor`) | Mismo punto que C6 del cliente → **pregunta al usuario** | — |
| **P5** | **Pagos no confirmados no se ven** en ninguna parte | Fuente informativa «Pagos en proceso» que no toca el saldo — o la pantalla de seguimiento de pagos (pedido 1) | FE-6 |
| **P6** | **Anular la liquidación emitida (`CBR.LQCS`) no revierte las APLP de su LQCC**: pagos y cruces de anticipo quedan vivos y el saldo del anticipo sigue consumido | ✔ `LiquidacionCompraServiceImpl:~2187-2210` sólo mira `AplicacionPagoCxc`. BE: revertir también las de `consultarPorLiquidacion(lqcc.id)`, como `LiquidacionCompraCompraServiceImpl:141` | BE-18 |
| **P7** | Aviso falso «No se pudieron consultar: Anticipos a proveedor» (`AnticipoProveedorServiceImpl:136`, cuerpo `{error}`) | Cubierto por FE-5a si la detección lee también `error` | FE-5/6 |
| **P8** | Abono por anticipo sin número; nota de venta rotulada «Factura» | Cubierto por FE-5c; rótulo por `tipoComprobante` | FE-6 |

## C. Preguntas al usuario

1. **C6/P4:** un anticipo **ingresado pero no pagado**, ¿debe contar como saldo a favor del titular, o sólo cuando se confirma?
2. **C1:** ¿la caja de tesorería (`TSR.CBRO`) se usa para cobrar facturas de venta? Si sí, hoy esos cobros no abonan ninguna factura.
