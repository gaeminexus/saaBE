# Catálogo ATS — referencia extraída del archivo oficial del SRI

**Fuente:** `docs/logica-negocio/cxp/Catalogo_ATS.xls` (entregado por el usuario el 2026-08-28,
descargado de sri.gob.ec → Anexos y guías → Anexo Transaccional Simplificado (ATS) → Catálogo ATS).
Este documento reemplaza a las tablas de la ficha técnica citadas en
`LEVANTAMIENTO-ATS-103-104.md` §3.6, que estaban desactualizadas — **esta es la fuente de verdad**
a partir de ahora. No hace falta volver a abrir el Excel para consultar un código; está todo aquí.

**Alcance:** se transcriben completas las tablas que Fases 4-6 necesitan para `<compras>`,
`<ventas>` y `<anulados>` de esta empresa. Las tablas de exportaciones/tarjetas de crédito/
fideicomisos/países (6, 7, 7.1, 8, 9, 10, 16, 17, 18, 19) se dejan solo referenciadas — el usuario
ya confirmó (`LEVANTAMIENTO-ATS-103-104.md` §7) que exportaciones, RECAPS, fideicomisos y
rendimientos financieros **no aplican** a esta empresa. Si algún día aplican, están completas en el
Excel original, hoja "TABLAS REFERENCIALES".

---

## 1. Clave primaria exacta de cada sección del XML (hoja "CLAVE PRIMARIA (2)")

Los campos marcados `X` en "clave general" son los que identifican de forma única un registro
dentro de su sección — importante para el generador: no puede haber dos filas con la misma
combinación de esos campos, y son los que hay que usar para deduplicar/agrupar.

### `<compras>` — clave: `codSustento`, `tpIdProv`, `idProv`, `tipoComprobante`, `establecimiento`,
`puntoEmision`, `secuencial`, `fechaEmision`, `autorizacion`

Todos los campos, en el orden del catálogo (los de reembolso solo aplican si el comprobante es de
reembolso — sustento 08):

`codSustento` · `tpIdProv` · `idProv` · `tipoComprobante` · `tipoProv` · `parteRel` ·
`DenoProv` · `fechaRegistro` · `establecimiento` · `puntoEmision` · `secuencial` · `fechaEmision` ·
`autorizacion` · `baseNoGraIva` · `baseImponible` (0%) · `baseImpGrav` (≠0%) · `baseImpExe` ·
`montoIce` · `montoIva` · `valRetBien10` (10%) · `valRetServ20` (20%) · `valorRetBienes` (30%) ·
`valRetServ50` (50%) · `valorRetServicios` (70%) · `valRetServ100` (100%) · `pagoLocExt` ·
`TipoRegi` · `paisEfecPagoGen` · `paisEfecPagoParFis` · `DenopagoRegFis` · `paisEfecPago` ·
`aplicConvDobTrib` · `pagExtSujRetNorLeg` · `pagoRegFis` · `formaPag` · `codRetAir` (**clave por
registro** — puede haber más de un concepto de retención de renta por comprobante) · `baseImpAir` ·
`porcentajeAir` · `valRetAir` · `fechaPagoDiv` · `imRentaSoc` · `anioUtDiv` · `NumCajBan` ·
`PrecCajBan` · `estabRetencion1` · `ptoEmiRetencion1` · `secRetencion1` · `autRetencion1` ·
`fechaEmiRet1` · `docModificado` · `estabModificado` · `ptoEmiModificado` · `secModificado` ·
`autModificado` · `tipoComprobanteReemb` · `tpIdProvReemb` · `idProvReemb` ·
`establecimientoReemb`/`puntoEmisionReemb`/`secuencialReemb`/`fechaEmisionReemb`/
`autorizacionReemb` (**clave por registro**) · `baseImponibleReemb` · `baseImpGravReemb` ·
`baseNoGraIvaReemb` · `baseImpExeReemb` · `totbasesImpReemb` · `montoIceReemb` · `montoIvaRemb`.

### `<ventas>` — clave: `tpIdCliente`, `idCliente`, `tipoComprobante`

`tpIdCliente` · `idCliente` · `parteRel` · `tipoCliente` · `DenoCli` · `tipoComprobante` ·
`numeroComprobantes` (cuenta de comprobantes agrupados, no uno por fila) · `baseNoGraIva` ·
`baseImponible` · `baseImpGrav` · `montoIva` (antes de compensaciones) · `CompSolIVA`
(compensación Ley Solidaridad) · `CompEleIVA` (compensación dinero electrónico) · `montoIce` ·
`valorRetIva` · `valorRetRenta` · `formaCobro`.

### `<ventas>` por establecimiento — clave: `codEstab`

`codEstab` · `ventasEstab` · `IVAComp`.

### `<anulados>` — clave: todos los campos son clave

`tipoComprobante` · `establecimiento` · `puntoEmision` · `secuencialInicio` · `secuencialFin` ·
`autorizacion`.

---

## 2. Tabla 1 — Período (mes)

| Mes | Código |
|---|---|
| Enero | 01 | Febrero | 02 | Marzo | 03 | Abril | 04 | Mayo | 05 |
| Junio / I Semestre (RIMPE) | 06 | Julio | 07 | Agosto | 08 | Septiembre | 09 | Octubre | 10 |
| Noviembre | 11 | Diciembre / II Semestre (RIMPE) | 12 |

## 3. Tabla 2 — Tipo de identificación (por transacción)

| Código | Transacción | Identificación |
|---|---|---|
| 01 | Compra | RUC |
| 02 | Compra | Cédula |
| 03 | Compra | Pasaporte / identificación tributaria del exterior |
| 04 | Venta | RUC |
| 05 | Venta | Cédula |
| 06 | Venta | Pasaporte / identificación tributaria del exterior |
| 07 | Venta | Consumidor final |
| 09 | Exportación | — (vigente 1/1/2000 a 28/2/2015, histórico) |
| 10 | Tarjeta de crédito | RUC |
| 11 | Tarjeta de crédito | Pasaporte |
| 12-14 | Rendimientos financieros | RUC / Cédula / Pasaporte |
| 15-17 | Fondos y fideicomisos | RUC / Cédula / Pasaporte |
| 18 | Comprobantes anulados | — |
| 19 | Venta | Placa o RAMV/CPN |
| 20 | Exportación | RUC |
| 21 | Exportación | Pasaporte |

Para esta empresa (sin exportaciones ni tarjetas de crédito propias): usar **01/02/03** en compras
y **04/05/06/07** en ventas.

## 4. Tabla 4 — Tipos de comprobante (relevantes para esta empresa)

**Formato:** código · nombre · secuenciales válidos para la transacción (1=compra, 2=venta, etc.,
ver Tabla 1) · sustentos tributarios válidos (Tabla 5) para ese tipo de comprobante.

| Código | Comprobante | Transacción | Sustentos válidos (Tabla 5) |
|---|---|---|---|
| **1** | Factura | 01, 09, 20, 21 | 01,02,03,04,05,06,07,08,09,14,15,00 |
| **2** | Nota o boleta de venta | 01 | 02,04,05,07,08,14,15,00 |
| **3** | Liquidación de compra de bienes/servicios | 02, 03 | 01,02,03,04,05,06,07,08,14,15 |
| **4** | Nota de crédito | 01,02,03,04,05,06,07,09,19,20,21 | 01,02,03,04,05,06,07,08,09,14,15,00 |
| **5** | Nota de débito | 01,02,03,04,05,06,07,09,19,20,21 | 01,02,03,04,05,06,07,08,09,14,15,00 |
| 41 | Comprobante de venta por reembolso | 01,02,03,04,05,06,07,09,19,20,21 | 01,02,03,04,05,06,07 |
| 47 | N/C por reembolso emitida por intermediario | 01,04,05,06,09,19,20,21 | 01,02,03,04,06,07 |
| 48 | N/D por reembolso emitida por intermediario | 01,04,05,06,09,19,20,21 | 01,02,03,04,06,07 |
| 294 | Liquidación de compra de bienes muebles usados | 02, 03 | 01-08 |
| 344 | Liquidación de compra de vehículos usados | 02, 03 | 01-08 |
| 375 | Liquidación de compra RISE (vigente 1/12/2020-31/12/2021, histórico) | 01 | 01-08 |

Nota clave para el generador: **cada tipo de comprobante restringe qué `codSustento` puede llevar.**
Antes de emitir una fila de `<compras>`, validar que el sustento resuelto (`FCTCCSUS`/`LQCCCSUS`/
`NTCCCSUS`/`NTDCCSUS`) sea uno de los válidos para ese tipo de comprobante — si no, es una señal de
que la resolución automática (o una corrección manual) produjo un valor imposible para ese
documento, y hay que reportarlo, no enviarlo igual.

- Factura de compra → código **1**.
- Liquidación de compra (`LQCC`) → código **3**.
- Nota de crédito de compra (`NTCC`) → código **4**.
- Nota de débito de compra (`NTDC`) → código **5**.

## 5. Tabla 5 — Sustento del comprobante (ya en `CrdSustentoTributarioSri`/`SustentoTributarioSri`)

Igual que `LEVANTAMIENTO-ATS-103-104.md` §3.6, con el detalle de **qué tipos de comprobante acepta
cada sustento** (columna "Código Tipo Comprobante" del catálogo, referida a la Tabla 4):

| Código | Sustento | Comprobantes válidos (Tabla 4) |
|---|---|---|
| **01** | Crédito tributario IVA (no inventario/activo fijo) | 1,3,4,5,11,12,21,41,43,47,48,294,344 |
| **02** | Costo/gasto IR (no inventario/activo fijo) | 1,2,3,4,5,9,11,12,15,19,20,21,41,43,47,48,294,344,364 |
| 03 | Activo fijo — crédito tributario IVA | 1,3,4,5,41,47,48,294,344 |
| 04 | Activo fijo — costo/gasto IR | 1,2,3,4,5,15,41,47,48,294,344 |
| 05 | Liquidación gastos de viaje/hospedaje/alimentación (a nombre de empleados) | 1,2,3,4,5,11,15,41,294,344 |
| **06** | Inventario — crédito tributario IVA | 1,3,4,5,41,43,47,48,294,344 |
| **07** | Inventario — costo/gasto IR | 1,2,3,4,5,15,41,43,47,48,294,344,364 |
| **08** | Reembolso de gasto (intermediario) | 1,2,3,4,5,21,294,344 |
| 09 | Reembolso por siniestros | 1,4,5,45 |
| 00 | Casos especiales (histórico, hasta 28/2/2015) | 1,2,4,5,19,42 |

Para esta empresa el grueso cae en 01, 02, 06, 07, 08 (ya identificado en `LEVANTAMIENTO-ATS-103-104.md`).

## 6. Tabla 11 — Porcentajes de retención de IVA

| Código | % | Vigente desde |
|---|---|---|
| 9 | 10% | 1/6/2015 |
| 10 | 20% | 1/6/2015 |
| 1 | 30% | 1/1/2002 |
| 11 | 50% | 1/1/2016 |
| 2 | 70% | 1/1/2002 |
| 3 | 100% | 1/1/2002 |

## 7. Tabla 12 — Porcentaje de IVA vigente por fecha

| % | Desde | Hasta |
|---|---|---|
| 12% | 01/01/2000 | 31/05/2001 |
| 14% | 01/06/2001 | 31/08/2001 |
| 12% | 01/09/2001 | 31/05/2016 |
| 14% | 01/06/2016 | 31/05/2017 |
| **12%** | 01/06/2017 | *(el catálogo no refleja el alza a 15% vigente hoy — confirmado por el
usuario 2026-08-28. No es un problema para el generador: no hace falta corregir esta tabla ni
consultarla en vivo, ver la nota de abajo.)* |

**Por qué esto no bloquea nada:** el ATS no necesita "saber" la tarifa vigente por fecha — cada
línea de `FCTC`/`LQCC`/`NTCC`/`NTDC` ya guarda su propio `porcentajeIVA`/`montoIva`, capturado al
momento del documento (12% en un documento de 2018, 15% en uno de hoy). El generador usa siempre el
valor grabado en el documento, nunca esta tabla — esta tabla es solo referencia informativa del
catálogo del SRI, desactualizada en este punto y sin ningún consumidor en el sistema.

## 8. Tabla 13 — Formas de pago/cobro (código vigente, sin fecha de fin)

| Código | Forma de pago |
|---|---|
| 01 | Sin utilización del sistema financiero |
| 15 | Compensación de deudas |
| 16 | Tarjeta de débito |
| 17 | Dinero electrónico |
| 18 | Tarjeta prepago |
| 19 | Tarjeta de crédito |
| 20 | Otros con utilización del sistema financiero |
| 21 | Endoso de títulos |

*(Los códigos 02-14 —cheque, débito de cuenta, transferencias, giro, etc.— tienen fecha de fin
31/08/2016 y ya no aplican a comprobantes actuales; se dejan fuera de esta tabla resumida porque un
documento de hoy no debería usarlos. Están completos en el Excel original si hace falta consultar
un histórico.)*

## 9. Tabla 14 — Tipo de identificación del proveedor (ya en `TTLRTPAT`)

| Código | Tipo |
|---|---|
| 01 | Persona natural |
| 02 | Sociedad |

## 10. Tabla 15 — Tipo de pago

| Código | Tipo |
|---|---|
| 01 | Pago a residente / establecimiento permanente |
| 02 | Pago a no residente |

## 11. Tabla 20 — Tipo de emisión de facturación

| Código | Tipo |
|---|---|
| F | Facturación física |
| E | Facturación electrónica |

Esta empresa factura electrónicamente — usar **E** como default salvo que exista algún comprobante
físico real que capturar distinto.

## 12. Tabla 21 — Tipo de compensación (IVA en ventas)

| Código | Tipo | Vigente desde |
|---|---|---|
| 01 | Ley Solidaridad — Zonas afectadas | 6/1/2016 (histórico, terminó 31/5/2017) |
| 02 | Medios electrónicos | 5/1/2016 |

## 13. Tablas que NO aplican a esta empresa (confirmado, no las modeles)

| Tabla | Qué es | Por qué no aplica |
|---|---|---|
| 6 | Distritos aduaneros | Sin exportaciones (§7 del levantamiento) |
| 7 / 7.1 | Código de régimen aduanero | Sin exportaciones |
| 8 | Tarjetas de crédito (emisoras) | Sin operación de tarjetas propia |
| 9 | Tipos de fideicomiso | Sin fideicomisos (§3.5 del levantamiento) |
| 10 | Tipo de exportación/ingreso del exterior | Sin exportaciones |
| 16 | Países | Solo hace falta si hay pagos/ingresos al exterior — no es el caso hoy |
| 17 | Paraísos fiscales | Ídem |
| 18 | Tipos de ingresos del exterior | Ídem |
| 19 | Régimen fiscal del exterior | Ídem |

Si en el futuro la empresa exporta o tiene operaciones con el exterior, estas tablas están
completas en el Excel original (`docs/logica-negocio/cxp/Catalogo_ATS.xls`, hoja "TABLAS
REFERENCIALES") — no hace falta volver a pedir el catálogo.

---

## 14. Reembolso de gastos — signo de cada comprobante en el reporte (hoja "REEMBOLSO DE GASTOS")

Al totalizar bases/IVA para un comprobante de reembolso, casi todos los tipos **suman** (`+`);
**solo dos restan** (`-`): las notas de crédito de compra (código 04 de esta tabla — no confundir
con el código 4 de la Tabla 4 de tipos de comprobante) y las notas de crédito por reembolso emitidas
por intermediario (código 47). El resto (factura, nota/boleta de venta, liquidación de compra, nota
de débito, boletos, tiquetes, pasajes, documentos financieros/seguros/telecomunicaciones, cartas de
porte, comprobantes de reembolso, retención presuntiva, hidrocarburos, reclamos de aseguradoras,
liquidaciones de bienes/vehículos usados, actas PET) suma.
