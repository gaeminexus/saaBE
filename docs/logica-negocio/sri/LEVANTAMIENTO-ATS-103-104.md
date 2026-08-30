# ATS, formulario 103 y formulario 104: qué pide el SRI y qué falta en el sistema

**Punto 13 del listado del 2026-08-27** · **Fecha:** 2026-08-27
**Origen:** el usuario no dispone de las casillas que declara hoy, así que se derivaron de la
normativa vigente del SRI. Todas las casillas y campos de este documento salen de documentos
**oficiales del SRI**, descargados y leídos para escribir esto:

| Documento | Uso aquí |
|---|---|
| *Guía para contribuyentes — Elaboración y envío de la declaración del IVA* (sri.gob.ec) | casillas del 104 |
| *Formulario 103 — Instructivo declaración de retenciones en la fuente* (sri.gob.ec) | casillas del 103 |
| *Ficha Técnica Anexo Transaccional Simplificado – ATS*, 93 páginas (sri.gob.ec) | estructura del ATS |
| Resolución **NAC-DGERCGC26-00000009** (27-feb-2026) | porcentajes de retención vigentes |

> **Advertencia de vigencia.** El instructivo del 103 que publica el SRI es antiguo (sus
> referencias más nuevas son de 2013) y **no refleja los porcentajes de 2026**. Las casillas
> siguen siendo válidas; los porcentajes hay que tomarlos de la resolución de 2026. Antes de
> implementar, contabilidad debe confirmar contra una declaración real reciente.

---

## 0. Lo primero: los porcentajes de retención ya están bien

La Resolución **NAC-DGERCGC26-00000009**, vigente desde el **1 de marzo de 2026**, cambió la
tabla de retenciones de renta: eliminó el 2,75%, creó el 5% para servicios profesionales
prestados por sociedades, y subió bienes muebles (1,75% → 2%), mano de obra (2% → 3%),
publicidad (1,75% → 3%) y arrendamiento de inmuebles (8% → 10%).

**Verificado en la base (copia de producción):** las retenciones emitidas desde el 1-mar-2026 ya
usan la tabla nueva.

| Código | % aplicado | Líneas | Concepto |
|---|---|---|---|
| 303 | 10 | 6 | Honorarios profesionales |
| 303A | 5 | 4 | Servicios profesionales de sociedades (**el 5% nuevo**) |
| 304B | 10 | 7 | Predomina el intelecto |
| 312 | 2 | 10 | Bienes muebles (**subió de 1,75%**) |
| 320 | 10 | 5 | Arrendamiento (**subió de 8%**) |
| 3440 | 3 | 16 | Otras retenciones (**subió de 2%**) |

**No hay nada que corregir aquí.** Se deja constatado para que no se abra como incidencia.

---

## 1. Formulario 104 — IVA mensual

### 1.1 Ventas y operaciones (400)

| Casilla | Concepto |
|---|---|
| 401 / 411 / 421 | Ventas locales gravadas **tarifa diferente de cero** — bruto / neto / impuesto generado |
| 402 / 412 / 422 | Ventas de **activos fijos** gravadas tarifa diferente de cero |
| 425 / 435 / 445 | Ventas locales gravadas **tarifa 5%** |
| 410 / 420 / 430 | Ventas locales gravadas con **tarifa variable** diferente de cero |
| 423 / 424 | IVA generado en la diferencia entre ventas y notas de crédito **con distinta tarifa** |
| 403 / 404 | Ventas locales tarifa **0% que NO dan** derecho a crédito tributario |
| 405 / 406 | Ventas locales tarifa **0% que SÍ dan** derecho a crédito tributario |
| 407 | Exportaciones de **bienes** |
| 408 | Exportaciones de **servicios y/o derechos** |
| 409 / 419 / 429 | **Total** operaciones y ventas — bruto / neto / impuesto generado |
| 431 | Transferencias **no objeto o exentas** de IVA |
| 434 | Ingresos por reembolso como intermediario (informativo) |
| 442 / 443 / 453 | Notas de crédito **por compensar el próximo mes** — valor / neto / impuesto |
| 480 / 481 | Total transferencias tarifa ≠ 0 **a contado** / **a crédito** este mes |
| 482 | Total impuesto generado |
| 483 | Impuesto a liquidar **del mes anterior** (traslada el 485 del período anterior) |
| 484 / 499 | Impuesto a liquidar **en este mes** / total |
| 485 | Impuesto a liquidar **el próximo mes** (= 482 − 484; va al 483 del mes siguiente) |
| 486 | Mes en que se pagará el IVA por ventas a crédito |
| 487 | Tamaño COPCI |

### 1.2 Adquisiciones y crédito tributario (500)

| Casilla | Concepto |
|---|---|
| 500 / 501 | Adquisiciones gravadas tarifa ≠ 0 **con derecho a crédito tributario** |
| 502 / 512 / 522 | Otras adquisiciones gravadas tarifa ≠ 0 **sin derecho** a crédito |
| 503 | **Importaciones de servicios** y/o derechos gravados tarifa ≠ 0 |
| 504 / 505 | **Importaciones de bienes** gravadas tarifa ≠ 0 |
| 507 | Adquisiciones (incluye activos fijos) gravadas **tarifa 0%** |
| 508 / 518 | Adquisiciones a contribuyentes **RISE / negocios populares** |
| 526 / 527 | IVA generado en la diferencia entre adquisiciones y NC con distinta tarifa |
| 530 / 533 / 534 | Adquisiciones gravadas **tarifa variable** ≠ 0 |
| 540 / 550 / 560 | Adquisiciones gravadas **tarifa 5%** |
| 531 | Adquisiciones **no objeto** de IVA |
| 532 | Adquisiciones **exentas** del pago de IVA |
| 535 / 545 | Pago neto por **reembolso de gastos** del intermediario |
| 543 / 544 / 554 | Notas de crédito por compensar el próximo mes |
| 563 | **Factor de proporcionalidad** = (411+412+420+415+416+417+418) / 419 |
| 564 | Crédito tributario aplicable en este período |
| 565 | IVA **no** considerado crédito tributario por factor de proporcionalidad |

### 1.3 Resumen impositivo (600)

| Casilla | Concepto |
|---|---|
| 601 / 602 | **Impuesto causado** / crédito tributario aplicable (excluyentes: si uno tiene valor, el otro va vacío) |
| 603 / 604 | Compensación de IVA por ventas con medio electrónico / en zonas afectadas |
| 605 / 606 | Saldo crédito tributario del mes anterior — por adquisiciones / por retenciones |
| 607 / 608 | Saldo del mes anterior por compensaciones (607 **bloqueado**, uso exclusivo del SRI) |
| 609 | **Retenciones de IVA que le han sido efectuadas** en el período |
| 610 / 611 | Ajuste por IVA devuelto o descontado (medio electrónico / zonas afectadas) |
| 612 / 613 / 614 | Ajustes por IVA devuelto o rechazado (devoluciones / retenciones / sector público) |
| 615 / 617 | **Saldo crédito tributario para el próximo mes** — adquisiciones / retenciones |
| 618 / 619 | Saldo próximo mes por compensaciones (618 **bloqueado**) |
| 620 | Subtotal a pagar |
| 621 | Retención de IVA en ventas diferentes porcentajes (**Petrocomercial y comercializadoras de combustibles**, desde ene-2022) |
| 622 | (−) IVA devuelto a personas adultas mayores o con discapacidad |
| 623 | Crédito tributario por procesos de **fusión o absorción** (con la fecha en el 492) |
| 624 | IVA pagado y no compensado que se carga al **gasto de Impuesto a la Renta** |
| 625 | Ajuste del crédito tributario **prescrito a los 5 años** |
| 699 | Total impuesto a pagar por percepción |

### 1.4 Agente de retención de IVA (700) y pago (800/900)

| Casilla | Concepto |
|---|---|
| 700 / 701 / 702 | ISD para devolución a exportadores habituales |
| **721 / 723** | Retenciones de IVA efectuadas al **10%** y **20%** |
| **725 / 727 / 729 / 731** | Retenciones de IVA al **30% / 50% / 70% / 100%** |
| 800 | Devolución provisional de IVA por compensación (**bloqueado**) |
| 802 | Retenciones efectuadas y no pagadas — sector público y universidades |
| 859 | Total consolidado de IVA |
| 890 | Pago previo (informativo) |
| 902 | Total impuesto a pagar |

> **La casilla 621 es directamente relevante para este ERP:** desde enero de 2022 recoge el IVA
> retenido por **Petrocomercial y las comercializadoras de combustibles** en ventas de derivados a
> las distribuidoras (Res. NAC-DGERCG21-00000063). Dado el módulo Petro/ASOPREP, hay que
> verificar con contabilidad si la empresa cae en este supuesto.

---

## 2. Formulario 103 — Retenciones en la fuente de Impuesto a la Renta

Cada concepto usa **dos casillas**: base imponible (3xx) y valor retenido (la de la columna
contigua). El instructivo oficial numera así:

### 2.1 Pagos en el país

| Casilla | Concepto |
|---|---|
| 302 / 352 | En relación de dependencia (supere o no la base desgravada) |
| 303 | **Honorarios profesionales** (persona natural, con título, prevalece el intelecto) |
| 304 | Predomina el **intelecto**, distinto del 303 (aquí van los pagos a docentes) |
| 307 | Predomina la **mano de obra** |
| 308 | Utilización o aprovechamiento de la **imagen o renombre** |
| 311 | A través de **liquidaciones de compra** (nivel cultural o rusticidad) |
| 314 | Regalías, derechos de autor, marcas, patentes |
| 322 | Seguros y reaseguros — base = **10% de las primas** facturadas |
| 323 | Rendimientos financieros |
| 324 | Dividendos |
| 325 | Loterías, rifas, apuestas |
| 329 / 330 | Compra local de banano a productor / impuesto actividad bananera productor-exportador |
| 510 / 520 | N.º de cajas de banano transferidas / destinadas a exportación |
| 332 | Pagos **no sujetos** a retención |
| 340 / 341 / 342 | Otras retenciones al **1% / 2% / 8%** |
| 344 | Otras retenciones a **otros porcentajes** |

> Las casillas 340-342 (1%, 2%, 8%) reflejan la tabla **anterior**. Con la resolución de 2026 el
> abanico va de 0% a 10%, con un 5% nuevo y sin 2,75%. **Este es exactamente el punto a
> confirmar contra una declaración real** antes de programar el mapeo.

### 2.2 Pagos al exterior

| Casilla | Concepto |
|---|---|
| 401 | Con convenio de doble tributación |
| 411 / 413 | Intereses por financiamiento de proveedores externos / de créditos externos |
| 415 | Dividendos |
| 429 / 440 | Otros conceptos / pagos al exterior no sujetos a retención |
| 431 / 439 / 433 | A **paraísos fiscales**: intereses / otros conceptos / dividendos |

### 2.3 Identificación y pago

`101` mes · `102` año · `104` n.º de formulario que sustituye · `202` razón social ·
`890` pago previo · `908/910/912` notas de crédito con las que se paga.

---

## 3. ATS — Anexo Transaccional Simplificado

### 3.1 Empaquetado

Un **único XML comprimido en ZIP**, nombrado **`ATmmaaaa.zip`** (ej. `AT082026.zip`).
Máximo **8 MB** por el portal. Montos en dólares, **siempre positivos**. Los campos marcados
obligatorios no pueden ir vacíos ni nulos.

### 3.2 Cabecera — identificación del informante

| Campo XML | Regla |
|---|---|
| `IdInformante` | RUC, 13 dígitos, los tres últimos `001`, con dígito verificador válido |
| `razonSocial` | 5-500 alfanumérico, sin símbolos extraños |
| `anio` / `mes` | `aaaa` / `mm` — **mes en que se contabiliza**, no en que se emite |
| `numEstabRuc` | 3 dígitos, establecimientos **activos** en el RUC, > 000 |
| `totalVentas` | base 0% + base ≠ 0% + base no objeto, consolidado de todos los establecimientos |
| `codigoOperativo` | literal **`IVA`** |
| `RegimenMicroempresa` | solo RIMPE semestral; **si no aplica, se omite el campo — no se pone NO** |

### 3.3 Sección `<compras>` — la más pesada

Identificación: `codSustento` (Tabla 5) · `tpIdProv` (Tabla 2) · `idProv` · `tipoComprobante`
(Tabla 4) · `parteRel` (SI/NO) · `tipoProv` (Tabla 14) · `denopr` · **`fechaRegistro`** (fecha de
**registro contable**) · `establecimiento` · `puntoEmision` · `secuencial` · `fechaEmision` ·
`autorizacion`.

Montos: `baseNoGraIva` · `baseImponible` (0%) · `baseImpGrav` (≠0%) · `baseImpExe` · `montoIce` ·
`montoIva`.

Retenciones de IVA, **una etiqueta por porcentaje**: `valRetBien10` (10%) · `valRetServ20` (20%) ·
`valorRetBienes` (30%) · `valRetServ50` (50%) · `valorRetServicios` (70%) · `valRetServ100` (100%) ·
`valorRetencionNc`.

Pago y exterior: `pagoLocExt` · `formaPago` (Tabla 13) · `paisEfecPago` · `tipoRegi` ·
`aplicConvDobTrib` · `pagoRegFis` · `pagExtSujRetNorLeg` · `denopago`.

Retención de renta: `codRetAir` (Tablas 3.x) · `baseImpAir` · `porcentajeAir` · `valRetAir`.

Comprobante de retención asociado: `estabRetencion1` · `ptoEmiRetencion1` · `secRetencion1` ·
`autRetencion1` · `fechaEmiRet1`.

Documento modificado (NC/ND): `docModificado` · `estabModificado` · `ptoEmiModificado` ·
`secModificado` · `autModificado`.

Reembolsos: `tipoComprobanteReemb` · `tpIdProvReemb` · `idProvReemb` · `establecimientoReemb` …

Banano: `numCajBan` · `precCajBan`. Dividendos: `anioUtDiv`.

### 3.4 Sección `<ventas>`

`tpIdCliente` (Tabla 2) · `idCliente` · `parteRel` · `tipoCliente` (Tabla 14) · `denoCli` ·
`tipoComprobante` (Tabla 4) · `tipoEm` (Tabla 20) · **`numeroComprob`** (cantidad de comprobantes:
las ventas van **agrupadas por cliente y tipo**, no una por una) · `baseNoGraIva` · `baseImponible` ·
`baseImpGrav` · `montoIva` · `montoIce` · `tipoCompe`/`monto` (Tabla 21), más el bloque de
retenciones que le practicaron.

### 3.5 Otras secciones

`<exportaciones>` (Tabla 10, régimen aduanero Tabla 7.1, distrito Tabla 6) ·
**`<anulados>`** (`tipoComprobante`, `establecimiento`, `puntoEmision`, `secuencialInicio`,
`secuencialFin`, `autorizacion` — **excluye los dados de baja por el portal SRI en línea**) ·
`<RECAPS>` tarjetas de crédito · fideicomisos · rendimientos financieros.
Las tres últimas **no aplican** a esta empresa.

### 3.6 Tablas de referencia

A tipos de transacción · 1 mes · 2 tipo de identificación · **3.x conceptos de retención de renta**
· **4 tipos de comprobante** · **5 sustento del comprobante** · 6 distritos aduaneros ·
7.1 regímenes aduaneros · 8 tarjetas · 9 fideicomisos · 10 tipos de exportación ·
**11 porcentajes de retención de IVA** · **12 porcentajes de IVA** · **13 formas de pago** ·
14 tipo de identificación proveedor · 15 tipo de pago · 16 país · 17 paraísos fiscales ·
18 ingresos del exterior · 19 régimen fiscal del exterior · 20 tipo de emisión · 21 compensaciones.

La ficha técnica trae las tablas embebidas al final, **pero varias están desactualizadas**: la
Tabla 12 (porcentaje de IVA) sigue diciendo **12% desde 2017**, cuando la tarifa vigente es **15%**,
y la Tabla 13 (formas de pago) viene con las columnas descuadradas en el PDF. **Para poblar los
catálogos hay que descargar el documento «Catálogo ATS» aparte**, en sri.gob.ec →
*Anexos y guías* → *Anexo Transaccional Simplificado (ATS)* → *Catálogo ATS*.

#### Tabla 5 — Sustento del comprobante (sí es fiable y es la que bloquea)

Extraída de la ficha técnica; sus vigencias no se han movido (la última incorporación es de 2020).

| Código | Tipo de sustento |
|---|---|
| **01** | Crédito tributario para declaración de **IVA** (servicios y bienes distintos de inventarios y activos fijos) |
| **02** | Costo o gasto para declaración de **IR** (servicios y bienes distintos de inventarios y activos fijos) |
| 03 | **Activo fijo** — crédito tributario para declaración de IVA |
| 04 | **Activo fijo** — costo o gasto para declaración de IR |
| 05 | Liquidación de gastos de viaje, hospedaje y alimentación (a nombre de empleados) |
| 06 | **Inventario** — crédito tributario para declaración de IVA |
| 07 | **Inventario** — costo o gasto para declaración de IR |
| 08 | Valor pagado para solicitar **reembolso de gasto** (intermediario) |
| 09 | Reembolso por siniestros |
| 10 | Distribución de dividendos, beneficios o utilidades |
| 11 | Convenios de débito o recaudación para IFI's |
| 12 | Impuestos y retenciones presuntivos |
| 13 | Valores reconocidos por entidades del sector público a favor de sujetos pasivos |
| 14 | Valores facturados por socios a operadoras de transporte |
| 15 | Pagos por consumos propios y de terceros de **servicios digitales** |
| 00 | Casos especiales (vigente solo hasta el 28/02/2015) |

Para esta empresa el grueso caerá en **01, 02, 06, 07 y 08**; el 08 conecta directamente con el
trabajo de reembolsos de gastos ya implementado en CxP.

**Actualización 2026-08-27:** el catálogo real ya está cargado en `PGS.TSRI` (`LSRI.TABLA='703'`,
15 filas, `ESTADO=1`) y coincide exactamente con esta tabla (sin el 00 histórico, como corresponde).
Es la fuente de verdad a partir de ahora — **no se redeclaró como lista en Java**; el rubro
`com.saa.rubros.SustentoTributarioSri` sólo trae los 5 códigos que esta empresa usa en la práctica
(01, 02, 06, 07, 08) como constantes, y todo lo demás (validación, catálogo para un combo) se lee
en vivo de `PGS.LSRI`/`PGS.TSRI` — ver §6.

#### Tabla 4 — Tipos de comprobante (confirmada para `<compras>`)

También cargada (`LSRI.TABLA='702'`, 39 códigos). Se usó para resolver una duda pendiente de este
documento: si `PGS.LQCC` (Liquidación de compra), `PGS.NTCC` (Nota de crédito de compra) y
`PGS.NTDC` (Nota de débito de compra) — además de `PGS.FCTC` — necesitan `codSustento` porque el
ATS también las reporta en `<compras>`. Confirmado: la Tabla 4 trae código **3** = "Liquidacion de
compra de Bienes o Prestacion de servicios", **4** = "Nota de credito" y **5** = "Nota de debito"
— los tres son tipos de comprobante válidos para `<compras>`. El DDL de §6 incluyó columnas en las
cuatro tablas por esto.

#### Tabla 11 — Porcentajes de retención de IVA

| Código | % | Casilla del 104 |
|---|---|---|
| 9 | 10% | 721 |
| 10 | 20% | 723 |
| 1 | 30% | 725 |
| 11 | 50% | 727 |
| 2 | 70% | 729 |
| 3 | 100% | 731 |

---

## 4. Qué tiene el sistema y qué falta

### 4.1 Lo que ya está

- **Catálogos SRI**: `PGS.LSRI` (24 catálogos declarados) + `PGS.TSRI` (474 valores). Ya incluye
  `Cat ATS - T13 - Formas Pago`, tipos de comprobante, tipos de identificación, tarifas de IVA e
  ICE y conceptos de retención de renta. **La estructura para cargar las tablas del ATS ya existe**
  — falta poblarla.
- **Compras**: `PGS.FCTC` (FacturaCompra) guarda `TIPOCOMPROBANTE`, `NUMESTABLECIMIENTO`,
  `NUMPTOEMISION`, `SECUENCIAL`, `FECHA`, `AUTORIZACION`, `FORMAPAGO`, y los subtotales
  desglosados por tarifa (`SUBTOTAL`, `SUBCERO`, `SUBTOTAL5`, `SUBTOTAL8`, `VIVA`, `VICE`…).
  Cubre casi todo `<compras>`.
- **Ventas**: `CBR.FCTR` y las notas de crédito/débito, con la misma granularidad.
- **Retenciones**: `CBR.RTV2`/`CBR.DRV2` (emitidas) y las de compra (recibidas), con
  `CODRETENCION`, `PORCENTAJERETEN`, `BASEIMPONIBLE` y los datos del documento sustento.
- **Anulados**: los estados de documento ya distinguen la anulación.

### 4.2 El hueco que hay que tapar primero

> **`codSustento` no existe en ningún lado.** Se buscó en todo el backend: la palabra «sustento»
> aparece solo como *«documento sustento»* de una retención, que es otra cosa. El **código de
> sustento tributario** (Tabla 5: crédito tributario para IVA, costo o gasto para IR, activo fijo…)
> es **campo obligatorio de cada línea de `<compras>`** y hoy no se captura.

Sin ese dato **el ATS no valida**, y no se puede deducir del resto: depende del **destino
tributario** que el contribuyente le da a cada compra. Hay que capturarlo en la factura de compra
—con un valor por defecto según el grupo de producto, para no torturar al usuario— y **rellenar
hacia atrás** las compras ya registradas.

Otros campos ausentes, todos menores comparados con el anterior:
`parteRel` y `tipoProv` (van en el titular, una vez por proveedor, no por documento) ·
`fechaRegistro` **contable** separada de `fechaEmision` · `pagoLocExt` y el bloque de exterior.

### 4.3 Orden propuesto

> **Las seis fases están cerradas (2026-08-28).** El «Catálogo ATS» oficial que faltaba llegó ese
> día y está transcrito en **[`CATALOGO-ATS.md`](CATALOGO-ATS.md) — esa es la fuente de verdad de
> catálogos a partir de ahora**, no las tablas de la ficha técnica (desactualizadas, ver §3.6).
> **Sigue en pie el bloqueante de §10.1: nunca se validó contra el XSD ni el validador oficial del
> SRI.** No presentar un ATS generado por este servicio sin pasar por ahí primero.

| Fase | Qué | Tamaño | Estado |
|---|---|---|---|
| **1** | Cargar los catálogos del ATS en `PGS.LSRI`/`PGS.TSRI` (Tablas 2, 4, 5, 11, 12, 13, 14, 20) desde el «Catálogo ATS» del SRI | S | ✅ **Hecho** — verificado 2026-08-27: `PGS.LSRI` ya trae las Tablas 701-707 (T2, T4, T5, T11, T13, T14, T15), todas `ESTADO=1`. Confirmado contra la base que la Tabla 5 (`LSRI.TABLA='703'`) trae exactamente los 15 códigos vigentes (01-15; el 00 histórico no está, correctamente). **Ampliado 2026-08-28** con el catálogo oficial completo en `CATALOGO-ATS.md` |
| **2** | `codSustento` en la factura de compra + defecto por grupo de producto + backfill de lo ya registrado | M | ✅ **Cerrada 2026-08-28.** Backfill escrito y **corrido por el usuario** (`sql/BACKFILL-SUSTENTO-TRIBUTARIO-UPDATE.sql`). **Extendida más allá del alcance original** a `LQCC`/`NTCC`/`NTDC` — ver §9; verificado con `sql/VERIFICACION-SUSTENTO-LQCC-NTCC-NTDC.sql` que las tres dan **0 filas pendientes**, sin backfill necesario |
| **3** | `parteRel` / `tipoProv` en el titular; `fechaRegistro` contable en compras | S | ✅ **Cerrada 2026-08-28** — DDL `sql/03-partereal-tipoprov-fecharegistro.sql` **ejecutado por el usuario**. Ver §8 |
| **4** | Generador del XML `<compras>` + `<ventas>` + `<anulados>` y empaquetado `ATmmaaaa.zip` | L | ✅ **Cerrada 2026-08-28** — `com.saa.ejb.sri` (paquete nuevo), StAX + `ZipOutputStream`, `POST /rest/ats/generar`. Ver §10.2-10.5. `<anulados>` cubre compra y venta desde que el frente de anulación (`../ESTADO-CXP-CXC-TSR-RHH-SRI.md` §3) dio el dato que faltaba |
| **5** | Pantalla: generar, previsualizar el cuadre contra el 104, descargar el ZIP | M | ✅ **Cerrada 2026-08-28** — `cxc/reportes/ats/`, ruta **`/menucuentasxcobrar/reportes/ats`**, con entrada de menú. Muestra `avisos` siempre expandido, descarga el ZIP decodificando `contenidoBase64`, y las dos vistas de cuadre con sus `noDisponibles` y motivos visibles |
| **6** | Reporte de apoyo al 104 y al 103: los totales por casilla, para que contabilidad transcriba | M | ✅ **Cerrada 2026-08-28** — `GET /rest/cuadresri/104/{idFacturador}` y `/103/{idFacturador}`. Ver §10.6 |

**El 103 y el 104 no se presentan por archivo**: se llenan en el portal del SRI, que además llega
**prellenado** con los XML de facturación electrónica. Por eso la fase 6 es un **reporte de
cuadre**, no un generador: sirve para que contabilidad contraste lo que el SRI propone contra lo
que dice la contabilidad propia. Ese es el entregable útil, y es mucho más barato que intentar
replicar el formulario.

---

## 6. `codSustento` — modelo, resolución y estado (fase 2, 2026-08-27)

### 6.1 Modelo (DDL aplicado en local; el de producción lo escribe el usuario)

Columna nueva `VARCHAR2(2)` con `CHECK IN ('01'..'15','00')`, en las cuatro tablas de `<compras>`
identificadas en §3.6:

| Tabla | Columna | Uso |
|---|---|---|
| `PGS.GRPP` | `GRPPCSUS` | Sustento **por defecto** del grupo de producto (decisión 1). Nace en `NULL`: hay que parametrizarlo. |
| `PGS.FCTC` | `FCTCCSUS` | Sustento **resuelto** de la factura de compra (decisión 2). Se resuelve automáticamente o se corrige a mano; nunca se recalcula solo. |
| `PGS.LQCC` | `LQCCCSUS` | Misma columna, preparada para liquidación de compra. **Resolución automática NO implementada todavía** (ver 6.5). |
| `PGS.NTCC` | `NTCCCSUS` | Ídem para nota de crédito de compra. **No implementada.** |
| `PGS.NTDC` | `NTDCCSUS` | Ídem para nota de débito de compra. **No implementada.** |

Entidades JPA actualizadas: `GrupoProductoPago.sustentoTributarioDefecto`, `FacturaCompra
.sustentoTributario`, `LiquidacionCompraCompra.sustentoTributario`, `NotaCreditoCompra
.sustentoTributario`, `NotaDebitoCompra.sustentoTributario`.

### 6.2 La regla de resolución (corregida 2026-08-27 — leer antes de tocar el código)

**La primera versión de esta regla estaba mal**, y el síntoma lo dejó claro de inmediato: con
"el sustento por defecto del grupo de producto, y si no hay ninguno configurado gana el de mayor
base imponible" como regla general, **131 de 131 facturas quedaban sin resolver** (§6.4 de la
versión anterior). El diagnóstico del síntoma (que el bloqueo era la falta de parametrización de
`GRPPCSUS`) era razonable pero incompleto — la causa real era otra: **un grupo de producto no es
una unidad fiscal**. "Servicios Básicos" es un solo grupo de producto, pero mezcla 56 líneas
**con IVA** (internet, telefonía) y 96 líneas **sin IVA** (luz y agua van al 0%). No existe un
único código de la Tabla 5 que represente correctamente a la vez el crédito tributario de IVA y
el costo/gasto de IR para el mismo grupo — cualquier valor fijo de `GRPPCSUS` para "Servicios
Básicos" sería correcto para una parte de sus líneas y **incorrecto** para la otra. Por eso una
regla que depende del grupo como fuente principal no podía funcionar, sin importar cuánto se
parametrizara.

**La regla correcta, en dos pasos, en este orden:**

1. **Excepción, por grupo de producto (`GRPPCSUS`), sólo para tres casos que el IVA no puede
   decidir por sí solo:** activo fijo (`03`/`04`), inventario (`06`/`07`), reembolso de gasto
   (`08`). Si el grupo con **mayor base imponible acumulada** en la factura tiene uno de estos
   tres códigos configurado, ese código gana — sigue siendo por grupo, pero ya no es la regla
   general, es la excepción para los casos donde el destino tributario de verdad depende de
   *qué se compró*, no de si la compra gravó IVA.
2. **Regla base, si no aplicó ninguna excepción: el IVA de la *factura*, no de la línea ni del
   grupo.** `FCTC.VIVA > 0` → `"01"` (crédito tributario IVA); si no → `"02"` (costo/gasto IR).
   Es correcta porque **codSustento es un dato por comprobante** (el ATS exige uno solo por
   documento — §3.3), y el IVA total de la factura ya es, por construcción, el resultado neto de
   sus líneas: si la factura generó IVA, aunque sea en una sola línea, hay crédito tributario que
   reclamar; si no generó nada, todo lo que se compró fue costo o gasto puro.

**Por qué no "el grupo con más base gana, sin más" (lo que se probó primero) tampoco sirve como
regla base:** porque el grupo con más base imponible en una factura de "Servicios Básicos" puede
perfectamente ser la línea de agua (0%, sin IVA) mientras la factura en conjunto sí generó IVA por
otra línea menor (internet) — el grupo "ganador" por monto no tiene por qué coincidir con el
sustento correcto del documento. El IVA de la factura sí es inequívoco: es una sola cifra, ya
calculada, que no depende de cuál línea pesa más.

**Verificado el 2026-08-27 contra las 131 facturas de compra activas:** 103 → `"01"`, 28 →
`"02"`, **0 sin resolver**. Ningún grupo tiene todavía una excepción configurada, así que hoy el
100% de la resolución pasa por la regla base del IVA — y aun así resuelve completo, que es
justamente la prueba de que la regla no dependía de la parametrización que se pensó que faltaba.

El valor se **guarda** en `FCTCCSUS`, no se recalcula al generar el ATS (fase 4): si el grupo de
una línea cambia después, o si contabilidad parametriza una excepción nueva, la factura ya emitida
conserva el sustento con el que se resolvió — y sigue corregible a mano en cualquier momento vía
`PUT /fctc/sustento/{id}`.

**Advertencia para quien toque esto después:** si en seis meses alguien ve esta regla y piensa
"esto se simplifica volviendo al sustento por grupo", que relea este párrafo primero — es
exactamente el diseño que se probó, midió y descartó en esta misma fecha por la razón de
"Servicios Básicos" de arriba.

### 6.3 Dónde vive (implementado)

- `com.saa.rubros.SustentoTributarioSri` — sólo 5 constantes (01, 02, 06, 07, 08) + la clave del
  catálogo (`LSRI_TABLA="703"`). El catálogo completo **no se redeclara**: vive en `PGS.LSRI`/
  `PGS.TSRI`, ya cargado (§3.6).
- `SustentoTributarioService`/`Impl` (`ejb/cxp/service(Impl)`): `calcularSustento` (la regla de
  6.2, pura, sin guardar), `resolverSiFalta` (guarda sólo si `FCTCCSUS` sigue `NULL` — nunca pisa
  una resolución ni una corrección manual), `corregirSustento` (valida contra el catálogo real vía
  SQL nativo — ver 6.6 — y guarda), `listarPendientes`, `catalogoVigente`.
- Enganchado en los dos caminos por los que nace una factura de compra manual:
  `FacturaCompraServiceImpl.saveSingle` (cabecera) y, sobre todo, `DetalleFacturaCompraServiceImpl
  .saveSingle` (líneas) — en el flujo REST manual la cabecera nace **antes** que sus líneas (dos
  llamadas separadas, `POST /fctc` y luego varios `POST /dfcc`), así que el momento real en que
  hay algo que resolver es al grabar cada línea, no al grabar la cabecera vacía.
- Enganchado en la carga automática: `ProcesoCargaDocumentosServiceImpl.registrarFacturaCompra`,
  después de grabar todas las líneas (incluidas las de valores de terceros). **No bloquea el
  lote**: si no se puede resolver, el documento igual se registra (`ESTADO_REGISTRADO_BD`) y se
  marca con una observación en `DocumentoCxp.observacion`, siguiendo el mismo mecanismo no
  bloqueante que ya usaba `advertenciaReembolso`.
- REST (`FacturaCompraRest`, `@Path("fctc")`): `GET /sustento/{id}` (consultar), `PUT
  /sustento/{id}?sustento=02` (corregir a mano), `GET /sustentoPendiente?idEmpresa=` (listado para
  repasar antes del primer ATS), `GET /sustentoCatalogo` (código→descripción, para un combo en el
  frontend sin duplicar la lista).

### 6.4 Backfill (análisis entregado, UPDATE pendiente de escribir)

`docs/logica-negocio/sri/sql/BACKFILL-SUSTENTO-TRIBUTARIO.sql` — sólo `SELECT`, reescrito con la
regla corregida de 6.2 y verificado corriendo contra la base local el 2026-08-27:

- **131 de 131 facturas de compra activas resuelven.** 103 → `"01"` (con IVA), 28 → `"02"` (sin
  IVA), 0 sin resolver. No hace falta parametrizar ningún grupo antes del backfill — la regla
  base no depende de eso. La consulta 2 del script sigue disponible para verificar excepciones
  por grupo (activo fijo/inventario/reembolso) si contabilidad decide configurar alguna después;
  hoy da 0 filas porque ningún grupo la tiene.
- **Hallazgo aparte, no relacionado con la resolución del sustento** (con la regla corregida ya
  no bloquea nada — ver 6.7): 5 facturas (ids 122, 159, 189, 190, 191 en la base local) tienen
  líneas apuntando a un `ProductoPago` que ya no existe en `PGS.PRDP`. Detalle completo, estado
  de contabilización/pago y recomendación sobre la FK en 6.7.

### 6.5 Lo que falta para cerrar la fase 2

1. **Escribir y ejecutar el `UPDATE` de backfill real**, a partir de los números de la consulta 1
   (103 → "01", 28 → "02") — el usuario lo escribe, según el patrón ya establecido en este
   proyecto de "cambios de datos como .md revisable". Ya no depende de parametrizar grupos primero.
2. ~~**Resolución automática para `LQCC`/`NTCC`/`NTDC`** — no implementada en esta ronda~~
   **Hecho el 2026-08-28, ver §9.**
3. **Corregir las 5 facturas con producto huérfano y decidir sobre la FK** — ver 6.7.

### 6.6 Nota técnica: mapeo JPA roto entre `TsriCompra` y `LsriCompra`

Descubierto al implementar la validación de `corregirSustento` contra el catálogo real — **no se
tocó, se reportó y se rodeó con SQL nativo**: `Tsri.java`/`TsriCompra` declara
`@JoinColumn(name="LSRI", referencedColumnName="TABLA")`, pero los datos reales en
`PGS.TSRI.LSRI` guardan el **id numérico** de `PGS.LSRI` (ej. `27`), no el texto de `LSRI.TABLA`
(ej. `"703"`) — verificado contra la base local. Navegar `t.lsri.tabla` en JPQL usa esa metadata y
no encuentra nada; `SustentoTributarioServiceImpl` evita la asociación mapeada y usa SQL nativo
sobre el join real (`TSRI.LSRI = LSRI.ID`). Si algún otro módulo navega esa relación en JPQL
(`TsriCompra.getLsri()` o similar), probablemente tiene el mismo problema — no se auditó el resto
del código por estar fuera del alcance de esta tarea.

### 6.7 `DetalleFacturaCompra.producto` sin FK — hallazgo, no arreglado

`PGS.DFCC` declara una sola FK real: la de `FACTURA` (`DFCC` → `FCTC`). La columna `PRODUCTO`
(`DetalleFacturaCompra.producto`, `java.lang.Long` plano, sin `@ManyToOne`/`@JoinColumn` — ver
`DetalleFacturaCompra.java`) no tiene ninguna restricción que la ate a `PGS.PRDP.ID`. Eso permitió
borrar un `ProductoPago` que 16 líneas de 5 facturas ya usaban, dejándolas huérfanas.

**Detalle de las 5 facturas** (verificado 2026-08-27):

| Factura | Número | Fecha | Proveedor | Líneas huérfanas (producto inexistente) |
|---|---|---|---|---|
| 122 | 002-022-000000321 | 2026-07-09 | PILLAJO GUACHO JOSE JUAN | 133 (Tinta Epson Yellow T504), 136 (Tinta Epson Black 504) |
| 159 | 002-022-000000322 | 2026-07-09 | PILLAJO GUACHO JOSE JUAN | 133, 134 (Magenta 504), 135 (Cyan T504), 136 |
| 189 | 004-100-003556736 | 2026-07-03 | EQUISUIZA | 8 (Prima Vida Grupo), 9 (Contribución Super Cías. 3.5%), 10 (Seguro Campesino 0.5%) |
| 190 | 001-006-058190562 | 2026-07-17 | AIG-METROPOLITANA | 78 (Hogar), 79 (Super de Bancos), 80 (Seguro Campesino), 81 (Derechos de Emisión) |
| 191 | 004-100-003713029 | 2026-07-22 | EQUISUIZA | 8, 9, 10 (mismos conceptos que la 189) |

Nótese que 189/191 (mismo proveedor, mismos tres productos huérfanos) y 122/159 (mismo proveedor,
solapan en 133/136) sugieren que los productos 8-10 y 133-136 eran líneas recurrentes de esos dos
proveedores (pólizas de seguro, insumos de impresora) que en algún momento se depuraron del
catálogo — consistente con una limpieza de `PGS.PRDP`, no con una carga de datos corrupta.

**Estado de contabilización y pago** (verificado 2026-08-27, `PGS.FCTC.ASIENTO`/`FCTCEPAG`):

| Factura | Contabilizada (tiene asiento) | Estado de pago |
|---|---|---|
| 122 | Sí (asiento 7492) | 3 = Pagada totalmente |
| 159 | Sí (asiento 7529) | 3 = Pagada totalmente |
| 189 | Sí (asiento 7559) | 3 = Pagada totalmente |
| 190 | Sí (asiento 7560) | `NULL` (sin pago aplicado — nunca se le aplicó nada, no es un dato faltante) |
| 191 | Sí (asiento 7561) | 3 = Pagada totalmente |

**Las 5 ya están contabilizadas, y 4 de las 5 ya están pagadas totalmente.** Esto importa para la
corrección: el asiento de cada una ya se generó cuando el producto **todavía existía** (si no,
`generarAsientoFacturaCompra` no habría podido resolver el grupo/cuenta contable para su línea del
DEBE) — el borrado del producto pasó **después**, no antes, de contabilizar. Corregir
`DFCC.PRODUCTO` para que apunte a un producto vigente de vuelta es, en principio, una corrección
de catálogo sobre datos históricos, no una operación que deba tocar el asiento ya posteado ni
requerir reversar el pago — pero **no se puede afirmar sin decidirlo con contabilidad si el grupo
del producto de reemplazo coincide con el grupo (y por tanto la cuenta) que el asiento original
usó**; si no coincide, ahí sí habría un desajuste entre lo contabilizado y lo que la línea
"corregida" diría a partir de ahora, y eso sí ameritaría revisar si reversar. No se investigó esto
último — es justamente la pieza que decide si hace falta reversar, y no toca resolverla en este
análisis.

**Recomendación sobre la FK `DFCC.PRODUCTO → PRDP.ID`:**

1. **Sí conviene declararla** — es la única forma de que esto no se repita.
2. **Hay que limpiar las 16 filas antes de un `ADD CONSTRAINT ... VALIDATE`** (el default):
   Oracle rechaza crear una FK que ya tiene violaciones existentes (`ORA-02298`). Alternativa que
   no toca las 16 filas ahora mismo: crearla con `ENABLE NOVALIDATE` — Oracle exige la FK para
   filas **nuevas** desde ese momento, pero no valida (ni bloquea) las 16 que ya existen. Permite
   cerrar el hueco de inmediato sin esperar a que contabilidad decida cómo corregir el histórico.
   La corrección definitiva de las 16 filas sigue pendiente en cualquiera de los dos caminos.
3. **Dos procesos insertan `DFCC.PRODUCTO` hoy, y uno de los dos no valida que el producto
   exista:**
   - `ProcesoCargaDocumentosServiceImpl` (carga automática): crea el `ProductoPago` primero
     (`obtenerOAutoCrearProducto`) y usa su id recién generado — no puede insertar un huérfano por
     construcción.
   - `DetalleFacturaCompraServiceImpl.saveSingle` (manual, `POST/PUT /dfcc`): recibe `producto`
     como un `Long` suelto del cliente REST y lo graba tal cual, **sin verificar que exista** en
     `PGS.PRDP`. Hoy nada te impide mandar un id borrado, inventado, o de otra empresa. Con la FK
     puesta, Oracle rechazaría el insert (`ORA-02291`) en vez de crear una fila huérfana nueva —
     recomendable envolver ese `INSERT` en un mensaje de negocio más claro que el `ORA-02291` crudo,
     pero eso ya es implementación, no este análisis.
4. **La causa real de las 16 huérfanas de hoy es la otra punta: `ProductoPagoServiceImpl.remove`**
   (`DELETE /rest/prdp/{id}`, `ProductoPagoRest.delete`) borra el `ProductoPago` sin comprobar si
   algún `DetalleFacturaCompra` lo referencia todavía. Con la FK puesta, Oracle también bloquearía
   ese borrado (`ORA-02292`, "child record found") mientras exista al menos una línea que lo use —
   eso es lo que habría evitado exactamente este caso. La FK resuelve las dos puntas (inserción y
   borrado) sin necesitar un chequeo aparte en `ProductoPagoServiceImpl`, aunque un mensaje de
   negocio ahí también sería más claro que el `ORA-02292` crudo.

---

## 7. Confirmaciones del usuario (2026-08-27)

| Pregunta | Respuesta |
|---|---|
| ¿La empresa está obligada a presentar el ATS? | **Sí.** La fase 4 se justifica. |
| ¿Aplica la casilla 621 (retención de IVA de Petrocomercial)? | **No.** Queda fuera del alcance; no modelar nada para ella. |

Sigue pendiente de contabilidad, y es lo que más riesgo quita:

1. **Una declaración 103 y una 104 reales y recientes**, para contrastar el mapeo de casillas
   contra lo que efectivamente se declara.
2. **El «Catálogo ATS» vigente** descargado del SRI, porque las tablas embebidas en la ficha
   técnica están desactualizadas (ver §3.6). Sin él, la fase 1 no se puede cerrar con códigos
   exactos — salvo las Tablas 5 y 11, que sí quedaron verificadas aquí.

## 8. `parteRel` / `tipoProv` / `fechaRegistro` contable — modelo (fase 3, 2026-08-28)

### 8.1 Qué se agregó

Tres campos de §4.2, todos capturados a mano (no hay regla derivable como la de `codSustento`
en la fase 2):

| Campo ATS | Dónde vive | Entidad / columna | Catálogo |
|---|---|---|---|
| `parteRel` (SI/NO) | `TSR.Titular`, una vez por proveedor | `Titular.parteRelacionada` / `TTLRPREL VARCHAR2(2)` | Sin catálogo — CHECK `('SI','NO')` |
| `tipoProv` (Tabla 14) | `TSR.Titular`, una vez por proveedor | `Titular.tipoProveedorAts` / `TTLRTPAT VARCHAR2(2)` | `PGS.LSRI.TABLA='706'` — **"01" Persona natural, "02" Sociedad**, ya cargado en la fase 1 (`01-catalogos-ats.sql:214-216`). CHECK `('01','02')` |
| `fechaRegistro` (registro contable, por documento) | `PGS.FCTC`/`LQCC`/`NTCC`/`NTDC` — las mismas cuatro tablas de `<compras>` de la fase 2 | `*.fechaRegistroContable` / `*FCRG DATE` | Fecha libre, sin catálogo |

**`tipoProveedorAts` no es lo mismo que el `tipoProveedor` (`TTLRPRVD`) que ya existía**: ese es
un flag binario "es proveedor sí/no" de uso general del sistema, no la clasificación de la Tabla
14 del SRI. Se mantuvieron los dos, con javadoc cruzado para que no se confundan.

**`fechaRegistro` es distinta de `fecha`** (fecha de emisión del comprobante, columna ya
existente en las cuatro tablas): el ATS pide la fecha en que el documento se **registró
contablemente**, que puede no coincidir con la de emisión.

### 8.2 Estado

- **Mapeo JPA hecho**: `Titular.java` (+2 campos), `FacturaCompra.java`, `LiquidacionCompraCompra
  .java`, `NotaCreditoCompra.java`, `NotaDebitoCompra.java` (+1 campo cada una). `obtieneCampos()`
  actualizado en los cinco `*DaoServiceImpl` correspondientes.
- **DDL propuesto, sin ejecutar**: `docs/logica-negocio/sri/sql/03-partereal-tipoprov-fecharegistro.sql`
  — mismo patrón que la fase 2 (columnas nullable, CHECK donde hay catálogo, comentarios). Falta
  que el usuario lo corra primero en local.
- **Sin lógica de negocio nueva, a propósito**: al ser las tres entidades genéricas (JPA plano,
  sin DTO — ver `CLAUDE.md`), quedan expuestas automáticamente por los endpoints CRUD existentes
  de `Titular`/`FacturaCompra`/`LiquidacionCompraCompra`/`NotaCreditoCompra`/`NotaDebitoCompra` en
  cuanto la columna exista en la base — no hizo falta tocar REST ni Service. Si más adelante se
  quiere un valor por defecto (p. ej. `fechaRegistro = fecha` si no se captura, o un default de
  `tipoProv` por tipo de identificación), es una decisión de negocio nueva, no implementada aquí.
- **No hay backfill**: a diferencia de `codSustento`, no hay ninguna regla para inferir
  retroactivamente si un proveedor existente es parte relacionada o persona natural/sociedad —
  quedan en `NULL` hasta que alguien los capture a mano.

## 9. `codSustento` extendido a LQCC/NTCC/NTDC (2026-08-28)

Decisión del usuario: extender la resolución automática de §6 (excepción por grupo de producto,
si no la regla base por IVA del documento) a las tres tablas de `<compras>` que la fase 2 dejó
sin resolución automática — `PGS.LQCC`, `PGS.NTCC`, `PGS.NTDC`. Las columnas `*CSUS` ya existían
desde §6.1; esto solo agrega el código que las resuelve.

### 9.1 Diferencias entre las tres, verificadas antes de escribir código

| Documento | `producto` en el detalle | Paso de excepción por grupo |
|---|---|---|
| `LQCC` (`DetalleLiquidacionCompraCompra`) | `@ManyToOne ProductoPago` (ya migrado, ver §4.3 fase C) | Sí — join directo, pesa por `subTotal` (no tiene `baseImponible`) |
| `NTCC` (`DetalleNotaCreditoCompra`) | `Long` plano, igual que `DetalleFacturaCompra` | Sí — mismo patrón de join manual `p.id = df.producto`, pesa por `baseImponible` |
| `NTDC` (`DetalleNotaDebitoCompra`) | **No existe ninguna columna de producto** (verificado) | **No** — siempre resuelve por la regla base del IVA del documento. Si se agrega la columna en el futuro, hace falta el mismo paso que los otros dos. |

En los tres, la carga automática desde XML (`ProcesoCargaDocumentosServiceImpl`) no asigna
`producto` a las líneas (el SRI no lo declara), así que en la práctica esas líneas siempre caen
a la regla base — no es un error, es el mismo comportamiento que ya tenían las líneas de
"valores de terceros" de `FacturaCompra`. `LQCC` sí puede resolver por excepción cuando el
documento nace desde la emisión propia (`LiquidacionCompraServiceImpl.crearDocumentoCxp`, que
copia el `producto` clasificado del lado CXC).

### 9.2 Dónde vive

`SustentoTributarioService`/`Impl` — mismos métodos que factura, con sufijo por documento:
`calcularSustentoLiquidacion`/`NotaCredito`/`NotaDebito`, `resolverSiFalta*`, `corregirSustento*`,
`listarPendientes*`. `catalogoVigente()` es compartido (mismo catálogo, Tabla 5, para los cuatro
documentos) — no se duplicó por tipo.

Enganchado en:
- `LiquidacionCompraCompraServiceImpl`/`NotaCreditoCompraServiceImpl`/`NotaDebitoCompraServiceImpl`
  `.saveSingle` (caminos manuales genéricos).
- `LiquidacionCompraServiceImpl.crearDocumentoCxp` (cxc → cxp, emisión propia de liquidación).
- `ProcesoCargaDocumentosServiceImpl.registrarLiquidacionCompraCompra`/`registrarNotaCreditoCompra`/
  `registrarNotaDebitoCompra` (carga automática desde el SRI) — no bloquea el registro si falla,
  mismo criterio que factura, con la misma advertencia `sustentoTributarioPendiente` en la
  respuesta.

REST: `LiquidacionCompraCompraRest` (`lqcc`), `NotaCreditoCompraRest` (`ntcc`),
`NotaDebitoCompraRest` (`ntdc`) — cada una con `GET /sustento/{id}`, `PUT /sustento/{id}?sustento=`,
`GET /sustentoPendiente?idEmpresa=`, mismo contrato que `fctc`.

DAO: `selectPendientesSustento(idEmpresa)` nuevo en `LiquidacionCompraCompraDaoService`,
`NotaCreditoCompraDaoService`, `NotaDebitoCompraDaoService` — mismo patrón que
`FacturaCompraDaoService`.

Reutiliza `FacturaSustentoPendiente` como proyección para los tres tipos nuevos (mismo shape:
id, numero, fecha, proveedor, identificación, total, iva, sustentoSugerido) en vez de crear tres
DTOs casi idénticos — nombre heredado de la implementación original de factura, no renombrado
para no romper el contrato ya usado por el frontend en `/fctc/sustentoPendiente`.

### 9.3 Backfill — verificado, no hace falta

El árbitro corrió la verificación contra la base (este agente no tiene acceso directo a la BD):
`LQCC`, `NTCC` y `NTDC` dan **0 filas** pendientes de sustento tributario hoy. Sin volumen
histórico que resolver retroactivamente, la resolución hacia adelante (§9.2) alcanza sola —
a diferencia de `FCTC`, que sí necesitó el backfill de §6.4/§6.5.

## 10. Generador del ATS (Fase 4) + reporte de cuadre 103/104 (Fase 6) — 2026-08-28

Implementados en un paquete nuevo, `com.saa.ejb.sri` (no existía ninguno antes — todo lo de SRI
vivía repartido en `cxc`/`cxp`, ver la investigación previa a este ítem). Empaquetado con
`java.util.zip.ZipOutputStream` (tampoco existía precedente en el repo). XML con StAX
(`XMLStreamWriter`), mismo patrón que `LiquidacionCompraServiceImpl.writeElement`.

**Verificado contra `CATALOGO-ATS.md`** (el catálogo oficial del SRI que reemplazó a la ficha
técnica desactualizada) — no contra el instructivo viejo. Tablas 2, 4, 5, 11, 12, 13, 14, 15, 20,
21 confirmadas; 6/7/7.1/8/9/10/16-19 no modeladas (no aplican a esta empresa, confirmado).

### 10.1 Advertencia que no cambia con el catálogo nuevo

**Nunca se validó contra el XSD oficial del ATS ni contra el validador del SRI.** La estructura
raíz (`&lt;iva&gt;`, `&lt;compras&gt;/&lt;detalleCompras&gt;`, etc.) es la del esquema público del
ATS, estable desde hace años, pero no viene confirmada por ningún documento de este levantamiento
— no enviar un ATS real generado por este servicio sin probarlo primero contra el validador del
SRI o una herramienta equivalente.

### 10.2 `POST /rest/ats/generar` — Fase 4

```json
{ "idFacturador": 1, "anio": 2026, "mes": 8 }
```
`idFacturador`, no `idEmpresa`: el RUC/razón social del ATS salen de `Facturador`
(`CBR.FCDR.NUMDOC`/`RAZONSOCIAL`), no de `Empresa` (`SCP.PJRQ`, que es solo jerarquía
organizacional, sin RUC) — verificado, no hay campo de RUC en `Empresa`.

Respuesta 200:
```json
{
  "nombreArchivo": "AT082026.zip",
  "contenidoBase64": "UEsDBBQA...",
  "tamanoBytes": 48213,
  "totalCompras": 42,
  "totalVentas": 15,
  "totalAnulados": 2,
  "totalVentasDeclarado": 18500.00,
  "avisos": [ "..." ]
}
```
El ZIP viaja en `contenidoBase64` (no octet-stream): mismo criterio que
`PagoProgramadoService.obtenerArchivoLote`, que ya responde el contenido de un archivo dentro de
JSON en vez de introducir un segundo patrón de descarga en el sistema.

**`avisos` no es opcional de leer — revisarlo siempre antes de enviar el ZIP al SRI.**

### 10.3 Qué genera `&lt;compras&gt;` (FCTC + LQCC + NTCC + NTDC)

Un `&lt;detalleCompras&gt;` por documento ACTIVO del período (`fechaRegistroContable` si está
capturada, si no `fecha` de emisión — mismo criterio de fallback que ya usan `codSustento` y el
resto de fase 3). Campos que sí se escriben: `codSustento` (ya resuelto en las 4 tablas, fase 6),
`tpIdProv` (Tabla 2, **traducido** del rubro interno `TipoIdentificacion` — ver 10.5), `idProv`,
`tipoComprobante` (tal como está grabado), `parteRel`/`tipoProv` (del titular, pueden venir vacíos
si no se capturaron — fase 3), `denopr`, `fechaRegistro`, `establecimiento`/`puntoEmision`/
`secuencial`/`fechaEmision`/`autorizacion`, y las bases/impuestos (`baseImponible` 0%,
`baseImpGrav` ≠0%, `montoIce`, `montoIva`).

**Antes de escribir cada fila se valida que el `codSustento` sea compatible con el
`tipoComprobante`** contra la Tabla 4/5 del catálogo nuevo (ej. Liquidación de compra no admite
sustento "09"/"00") — si no encaja, no se corrige solo: se agrega a `avisos` para revisión manual.

**Actualización 2026-08-28 (ítem 12) — la limitación de `&lt;anulados&gt;` de compra ya no
existe.** `FacturaCompra`/`LiquidacionCompraCompra`/`NotaCreditoCompra`/`NotaDebitoCompra` ahora
tienen auditoría de anulación completa (`motivoAnulacion`/`fechaAnulacion`/`usuarioAnulacion`,
columnas `*MTAN`/`*FCAN`/`*USAN`) más un método `anular&lt;Tipo&gt;`/endpoint
`POST /&lt;ruta&gt;/anular/{id}` por cada una — mismo patrón que `FacturaServiceImpl.anularFactura`
del lado venta, `estadoEmision=3` = ANULADA (verificado por precedente de código —
`LiquidacionCompra.anular`, lado CXC, ya usa ese mismo código — pendiente de confirmar contra la
base real, ver el ítem 12). **`GeneradorAtsServiceImpl.anuladosDe` ya se conectó a las cuatro** —
`&lt;anulados&gt;` cubre compra y venta por igual. La limitación que sigue en pie es otra, ya
documentada más abajo: no se puede distinguir una anulación interna de una baja hecha en el
portal del SRI (ningún lado, ni compra ni venta, tiene ese dato) — sigue generándose la lista
completa de anulados internos, con el aviso correspondiente para revisión manual antes de enviar.

**No se escriben** (fuera de alcance de esta ronda, con la razón exacta):
- `baseNoGraIva`/`baseImpExe`: quedan en `0.00`. El modelo no distingue "no objeto de IVA" ni
  "exento" del resto de la base 0% — no hay columna para eso hoy.
- Retenciones de IVA/renta por documento (`valRetBien10`...`valRetAir`), pago/exterior (Tabla
  13, `formaPag` — `FacturaCompra.formaPago` existe pero es el rubro interno
  `FormaPagoProgramado` de este sistema, no confirmado que mapee a la Tabla 13 del SRI, así que
  no se usa), reembolsos detallados, banano, dividendos: sin fuente de datos vinculada al
  documento individual en el modelo actual.

### 10.4 Qué genera `&lt;ventas&gt;` (FCTR + NTCR + NTDB)

Agrupado por `(titular, tipoComprobante)`, `numeroComprob` = cantidad de documentos agrupados
(§3.4). `tpIdCliente` traducido igual que compras pero al rango de venta (04-07, ver 10.5).
`tipoEm` = `"E"` siempre (Tabla 20 — toda la emisión de esta empresa es electrónica, confirmado
en el catálogo). Notas de crédito **restan** de la base/IVA del cliente, notas de débito suman —
mismo criterio de netiado que usa el reporte de cuadre (§10.6).

**No se escribe**: `CompSolIVA`/`CompEleIVA` (Tabla 21, compensaciones — sin dato por documento),
`valorRetIva`/`valorRetRenta` (retenciones que le practicaron al cliente, no modeladas por
documento de venta), `formaCobro` (mismo problema de `formaPag` de compras).

### 10.5 Corrección importante: traducción de tipo de identificación (Tabla 2)

**Antes de este ítem, el plan era reusar `Titular.rubroTipoIdentificacionH` tal cual** (como ya
hace `LiquidacionCompraServiceImpl` para el XML del comprobante electrónico). **No sirve para el
ATS**: `rubroTipoIdentificacionH` guarda el numeral interno de
`com.saa.rubros.TipoIdentificacion` (Cédula=1, RUC=2, Pasaporte=3 — orden y numeración propios
del sistema), mientras que la Tabla 2 del ATS usa **dos rangos distintos según la dirección**:
compras 01=RUC/02=Cédula/03=Pasaporte, ventas 04=RUC/05=Cédula/06=Pasaporte/07=Consumidor final.
Emitir el rubro interno tal cual habría mandado "1"/"2"/"3" en vez de "01"-"07", con el orden
además invertido entre RUC y Cédula. Se corrigió con dos traductores (`tipoIdentificacionCompra`/
`tipoIdentificacionVenta`) — ninguno puede resolver "Consumidor final" (07): `TipoIdentificacion`
no tiene ese valor, así que un titular sin rubro reconocido queda con el campo vacío, no con un
07 asumido.

### 10.6 `GET /rest/cuadresri/104/{idFacturador}?anio=&mes=` y `/103/{idFacturador}?anio=&mes=` — Fase 6

No generan los formularios — dan los totales que el sistema puede derivar, para que contabilidad
los contraste contra lo que el SRI prellena.

**104**: casillas 401/411/421 (ventas gravadas ≠0%), 425/435/445 (tarifa 5%), 409/419/429
(totales), 500/501 (compras con derecho a crédito, por `codSustento` ya resuelto), 502/512/522
(sin derecho), 507 (tarifa 0%), 601 **o** 602 (impuesto causado / crédito, simplificado — sin
saldos de meses anteriores ni ajustes). Respuesta con dos listas: `casillas` (lo calculado) y
`noDisponibles` (609, 620, 621, 605/606/615/617 — cada una con el motivo exacto de por qué no se
calcula, no se pone en cero).

**103**: agrupado por `codRetencion` tal como se registra en `RTV2`/`DRV2` hoy, **no** traducido a
número de casilla salvo coincidencia literal sin sufijo (303, 304, 312, 320, 340-342, 344...).
Los códigos con sufijo de la resolución 2026 (`303A` = 5% servicios profesionales de sociedades,
`304B`) quedan con `casillaSugerida: null` a propósito — el propio levantamiento (§0, §2.1) pide
confirmar ese mapeo contra una declaración real antes de programarlo, y no llegó esa confirmación
en esta ronda. Solo cubre "pagos en el país" (§2.1); pagos al exterior (§2.2) no verificados.

### 10.7 Pendiente para el frontend (Fase 5, fuera de este ítem)

Pantalla de: elegir facturador/período, llamar `/ats/generar`, mostrar `avisos` de forma visible
(no como detalle colapsado — son la lista de lo que hay que revisar antes de enviar), botón de
descarga del ZIP (decodificar `contenidoBase64`), y una vista de los dos cuadres con `casillas`/
`noDisponibles`/`porCodigo` en tablas simples.
