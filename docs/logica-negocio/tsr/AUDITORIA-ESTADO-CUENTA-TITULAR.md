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

## B. Rol PROVEEDOR

*Pendiente — la medición está en curso.*
