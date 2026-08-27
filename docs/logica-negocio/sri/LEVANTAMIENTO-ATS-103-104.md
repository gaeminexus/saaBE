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

| Fase | Qué | Tamaño |
|---|---|---|
| **1** | Cargar los catálogos del ATS en `PGS.LSRI`/`PGS.TSRI` (Tablas 2, 4, 5, 11, 12, 13, 14, 20) desde el «Catálogo ATS» del SRI | S |
| **2** | `codSustento` en la factura de compra + defecto por grupo de producto + backfill de lo ya registrado | M |
| **3** | `parteRel` / `tipoProv` en el titular; `fechaRegistro` contable en compras | S |
| **4** | Generador del XML `<compras>` + `<ventas>` + `<anulados>` y empaquetado `ATmmaaaa.zip` | L |
| **5** | Pantalla: generar, previsualizar el cuadre contra el 104, descargar el ZIP | M |
| **6** | Reporte de apoyo al 104 y al 103: los totales por casilla, para que contabilidad transcriba | M |

**El 103 y el 104 no se presentan por archivo**: se llenan en el portal del SRI, que además llega
**prellenado** con los XML de facturación electrónica. Por eso la fase 6 es un **reporte de
cuadre**, no un generador: sirve para que contabilidad contraste lo que el SRI propone contra lo
que dice la contabilidad propia. Ese es el entregable útil, y es mucho más barato que intentar
replicar el formulario.

---

## 5. Confirmaciones del usuario (2026-08-27)

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
