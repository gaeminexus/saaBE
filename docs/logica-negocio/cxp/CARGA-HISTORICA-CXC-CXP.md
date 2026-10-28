# Carga histórica de CxC y CxP desde el control del contador (C11 — cuadre Superintendencia)

**Escrito:** 2026-10-28 · **Rama:** `cuadre/c11-cargador-cxc-cxp` · **Expediente:** `cuadreSuper/03-trabajo/cxc-cxp/`
(`ANALISIS-CXC-CXP.md`, `INVENTARIO-SAA-CXC-CXP.md`, `lotes/VALIDACION.md`) · **Decisiones:** D-003 a D-007 y D-013 (saldos iniciales, §10) de
`cuadreSuper/01-seguimiento/DECISIONES-Y-PARTIDAS.md`.

Reconstruye en el SAA, **por los módulos y con sus motores contables**, la operación de compras, retenciones emitidas,
pagos, ventas, retenciones recibidas, cobros y notas de crédito que el SAA no tiene (jun-2025 → jun-2026) y sirve para
cruzar y completar jul–oct 2026. La fuente es `control asoprep.xlsx` del contador (regla del líder: **donde el Excel y la
base difieran, manda el Excel**). La contabilidad que resulta es la definitiva (D-007).

## 1. Piezas

| Capa | Archivo | Qué hace |
|---|---|---|
| Generador (expediente) | `cuadreSuper/03-trabajo/cxc-cxp/scripts/07-genera-lotes.js` + `reglas-carga-historica.json` | De los CSV de `hojas/` produce `lotes/AAAA-MM/lote.json` y `lotes/VALIDACION.md`. Valida contra los catálogos exportados con `sql/c11-00-catalogos.sql` |
| REST | `ws/rest/historico/CargaHistoricaCxcCxpRest.java` | `POST /SaaBE/rest/historico/cargarHistoricoCxcCxp` (carga), `POST /SaaBE/rest/historico/validarHistoricoCxcCxp` (solo valida) y `POST /SaaBE/rest/historico/reasignarSaldoInicial` (D-013, §10) |
| Orquestador | `ejb/historico/service/CargaHistoricaCxcCxpService` + `serviceImpl/CargaHistoricaCxcCxpServiceImpl` | Sin transacción (`NOT_SUPPORTED`); recorre el lote en orden y convierte cada excepción en un renglón RECHAZADO |
| Por documento | `ejb/historico/service/CargaHistoricaDocumentoService` + `serviceImpl/CargaHistoricaDocumentoServiceImpl` | Un método `REQUIRES_NEW` por tipo de documento; ante error marca rollback y relanza |
| DTO | `model/historico/LoteHistoricoCxcCxp.java`, `model/historico/ResultadoCargaHistorica.java` | Lote y resultado (POJOs sin `@Entity`, campos públicos) |
| Catálogo | `docs/logica-negocio/cnt/sql/c11-01-centros-de-costo-asoprep.sql` | Crea en `CNT.CNCS` el árbol `1 ASOPREP` → `1.01 ADMINISTRACION`, `1.02 ARRIENDOS` (D-005). SQL puro, revisable, sin `COMMIT` |

No hay DDL ni tabla nueva. Motores que se reutilizan tal cual (no se modificó ninguno): `AsientoContableService.generarAsientoFacturaCompra / generarAsientoRetencionV2 / generarAsientoFactura / generarAsientoRetencionCompraV2 / generarAsientoNotaCreditoCompra / generarAsientoNotaCredito`, `AplicacionPagoCxpService.aplicarRetencionEmitida / aplicarPagoTransferencia / aplicarNotaCredito / saldoFactura`, `PagoProgramadoService.registrarPago`, `AplicacionPagoCxcService.aplicarCobroTransferencia / aplicarRetencionRecibidaV2 / aplicarNotaCredito / saldoFactura`.

## 2. Contrato del lote (`lote.json`)

Un lote por mes. **D-003:** cada documento va al lote del mes de su **fecha de emisión**; cada pago y cobro al lote del mes de su **fecha de pago** (por eso un lote puede traer pagos de facturas de meses anteriores: `facturaMes`). Los lotes se cargan en orden cronológico. Fechas `yyyy-MM-dd`, importes con punto decimal.

```json
{ "mes": "2025-06", "etiqueta": "control asoprep 27-oct-2026", "idEmpresa": 1236, "idUsuario": 38, "usuario": null,
  "observacion": "CARGA HISTORICA [control asoprep 27-oct-2026] mes 2025-06", "soloValidar": false, "aceptarInferidos": false,
  "titulares":            [ { "identificacion": "1791847652001", "tipoIdentificacion": "RUC", "tipoPersona": "JURIDICA", "razonSocial": "...", "esProveedor": true, "esCliente": false, "existeEnSaa": true } ],
  "compras":              [ { "ref": "C-2025-06-0002", "claveAcceso": "49 dígitos", "numero": "003-003-000174424", "fechaEmision": "2025-06-02", "fechaAutorizacion": "2025-06-02",
                              "proveedor": "0400985149001", "razonSocial": "...", "baseCero": 0, "base5": 0, "base15": 1.34, "baseNoObjeto": 0, "iva": 0.2, "total": 1.54, "diferenciaRedondeo": 0,
                              "cuenta": "4.4.05.26", "nombreCuenta": "...", "origenCuenta": "excel | D-004 (...) | regla:proveedor (...) | regla:listas (...)", "cuentaInferida": false, "requiereConfirmacion": false,
                              "centroCosto": "1.01", "centroInferido": false,
                              "lineas": [ { "descripcion": "GASTOS NOTARIO - 003-003-000174424", "base": 1.34, "tarifa": 15, "codigoIvaSri": 4, "iva": 0.2, "cuenta": "4.4.05.26", "centroCosto": "1.01" } ],
                              "estadoPagoExcel": "PAGADO", "formaPagoExcel": "ORDEN PROCESADA - PACÍFICO", "tipoGasto": "GASTOS NOTARIO",
                              "retencionEmitida": { "numero": "001-002-000006926", "fecha": "2025-06-05", "total": 0.33, "lote": "2025-06" }, "pagoSinFecha": true, "fechaPagoSugerida": "2025-06-05" } ],
  "retencionesEmitidas":  [ { "ref": "R-2025-06-0001", "numero": "001-002-000006926", "claveAcceso": "...", "fechaEmision": "2025-06-05", "fechaAutorizacion": "2025-06-05", "proveedor": "...", "facturaNumero": "003-003-000174424", "facturaClave": "...", "facturaFecha": "2025-06-02", "periodoFiscal": "06/2025",
                              "lineas": [ { "impuesto": "RENTA", "codigoSri": "304B", "base": 1.34, "porcentaje": 10, "valor": 0.13, "codigoInferido": true }, { "impuesto": "IVA", "codigoSri": "3", "base": 0.2, "porcentaje": 100, "valor": 0.2, "codigoInferido": false } ], "total": 0.33, "fuente": "hoja RETENCIONES EMITIDAS" } ],
  "pagos":                [ { "ref": "P-2025-06-0001", "facturaClave": "...", "facturaNumero": "001-001-032168621", "facturaMes": "2025-06", "proveedor": "...", "fechaPago": "2025-06-04", "formaPago": "DEBITO_AUTOMATICO | TRANSFERENCIA", "formaPagoExcel": "DÉBITO AUTOMÁTICO - PACIFICO",
                              "cuentaBancaria": "1048142735", "idCuentaBancaria": 418, "banco": "BANCO PACIFICO", "valor": 143.42, "totalFactura": 143.42, "retenido": 0, "referencia": "...", "retencionEnOtroMes": null } ],
  "ventas":               [ { "ref": "V-2025-06-0001", "claveAcceso": "...", "numero": "001-002-000000544", "fechaEmision": "2025-06-11", "cliente": "...", "base15": 1258.62, "base0": 0, "iva": 188.79, "total": 1447.41, "cuentaIngreso": "5.3.01", "lineas": [ ... ], "estadoCobroExcel": "PAGADA - REVISAR REGISTRO", "cobroSinFecha": true } ],
  "cobros":               [ { "ref": "CB-2025-06-0001", "facturaClave": "...", "facturaNumero": "001-002-000000545", "facturaMes": "2025-06", "cliente": "...", "fechaCobro": "2025-06-23", "cuentaBancaria": "1048142735", "idCuentaBancaria": 418, "valor": 1504.46, "totalFactura": 1504.46, "retenido": 0, "referencia": "..." } ],
  "retencionesRecibidas": [ { "ref": "RR-2025-06-0001", "numero": "001-100-000019640", "claveAcceso": "...", "fechaEmision": "2025-06-13", "cliente": "...", "facturaNumero": "001-002-000000552", "facturaClave": "...", "periodoFiscal": "06/2025", "lineas": [ { "impuesto": "IVA", "codigoSri": "3", "base": 461.41, "porcentaje": 100, "valor": 461.41 } ], "total": 461.41 } ],
  "ncCompras":            [ { "ref": "NCC-...", "claveAcceso": "...", "numero": "...", "fechaEmision": "...", "proveedor": "...", "facturaAfectadaNumero": "...", "facturaAfectadaClave": "...", "facturaAfectadaMes": "...", "baseCero": 0, "base15": 0, "iva": 0, "total": 0, "cuenta": "4.x", "origenCuenta": "excel | factura afectada (NO ENCONTRADA)", "centroCosto": "1.01", "lineas": [ ... ] } ],
  "ncVentas":             [ { "ref": "NCV-...", "claveAcceso": "...", "numero": "...", "fechaEmision": "...", "cliente": "...", "facturaAfectadaNumero": "...", "facturaAfectadaClave": "...", "base15": 50, "base0": 0, "iva": 7.5, "total": 57.5, "cuentaIngreso": "5.3.01", "lineas": [ ... ] } ] }
```

Campos de control de la petición: `idUsuario` (por defecto **38**, "usuario 1" de `SCP.PJRQ`, decisión del coordinador 28-oct), `usuario` (nombre que queda en los asientos; si va vacío se toma el del `idUsuario`), `soloValidar` (no graba nada; cada ítem responde VALIDADO/RECHAZADO con lo que haría), `aceptarInferidos` (carga también las compras con `requiereConfirmacion = true`; por defecto se rechazan).

## 3. Mapeo Excel → SAA

### 3.1 Titulares (`TSR.TTLR`, `PRRL`, `PRCC`)

Se buscan por identificación (el generador repone el cero inicial que el Excel pierde: `400985149001` → `0400985149001`). Si no existe se crea: `TTLRRYYA/RZZA = 35 / 1 natural · 2 jurídica` (jurídica = RUC con tercer dígito 6 o 9), `TTLRRYYB/RZZB = 36 / 2 RUC · 1 cédula · 3 pasaporte`, `TTLRCLNT/TTLRPRVD` según rol, `TTLRAPIV = TTLRAPRT = 1`, `TTLRTPAT` 01/02, estado 1, nombre = razón social del Excel (y `TTLRRZSC` si es jurídica). **Además** se garantiza lo que la creación automática desde el XML del SRI no hace y los motores exigen (`existeCuentaConRolEstricto`): rol `PRRL` (rubro 55: 2 proveedor / 1 cliente, empresa 1236) y cuenta `PRCC` tipo 1 con **`2.3.90.10`** (proveedores) o **`1.4.90.10`** (clientes), que es la práctica actual del SAA. Un titular existente solo recibe la bandera y el rol/cuenta que le falten.

### 3.2 Compras (`PGS.FCTC` + `DFCC`)

| Excel | SAA |
|---|---|
| CLAVE DE ACCESO | `CLAVE` y `AUTORIZACION` (ya autorizada; no hay XML ni bandeja `DCXP`) |
| N DE FACTURA | `NUMERO`, `NUMESTABLECIMIENTO`, `NUMPTOEMISION`, `SECUENCIAL` |
| FECHA EMISION | `FECHA` (fecha del documento = fecha contable) y `FCTCFCRG` |
| RUC / PROVEEDOR | `TITULAR` |
| BASE CERO TOTAL (= BASE CERO + BASE CERO DIFERENCIADA), BASE 5%, BASE 15, IVA2, IMPORTE TOTAL | una línea `DFCC` por tarifa (`PORCENTAJEIVA`, `CODIGOIVASRI` 0/5/4, `BASEIMPONIBLE`, `VALORIVA`); cabecera `SUBTOTAL/SUBCERO/SUBTOTAL5/PIVA/VIVA/VIVA5/TOTAL`. Diferencia `total − bases − IVA` hasta 0,50 la absorbe el motor en `4.8.90.90.35` (más de 0,50 el generador rechaza) |
| CUENTA SUGERIDA PLAN ASOPREP (`440105 - …`) | cuenta de movimiento del plan (`CNT.PLNN`) por dígitos; va en cada línea → **producto histórico** del grupo con esa cuenta (§4.1) |
| CENTRO DE COSTOS (`01 ASOPREP ADMINISTRACION` / `02 ASOPREP ARRIENDOS`) | `CNT.CNCS.CNCSNMRO` `1.01` / `1.02` → `CNT.DTAS.CNCSCDGO` de las líneas de gasto (§4.2). Sin centro: `4.4.03.xx` → 1.02, el resto → 1.01, marcado `centroInferido` (D-005) |
| ESTADO DE FACTURA / FORMA DE PAGO / FECHA DE PAGO / referencia | pago (§3.4) o saldo por pagar |
| N.º RETENCIÓN EMITIDA / FECHA RETENCIÓN | retención emitida (§3.3), solo 2025 |
| TIPO DE GASTO, OBSERVACION | `OBSERVACION` de la factura, junto con la observación base |

Estados: `ESTADO = 1`, `ESTADOEMISION = 2`, `FCTCEPAG = 1` (lo recalculan las aplicaciones), `FCTCESIN = 0`, `FCTCESSG = 0`, `FCTCESRM = 0`, `AMBIENTE` el de la clave. Asiento: `generarAsientoFacturaCompra(id, 1236, TipoAsientos.FACTURAS_COMPRA, fechaEmision, …)` (Debe gasto por grupo + `1.9.01.90.15` IVA, Haber `2.3.90.10`).

**Cuenta cuando el Excel no la trae** (orden del generador): (1) **D-004** si el proveedor es asegurador (Equisuiza, Generali → `1.4.90.90.10` desgravamen; AIG → `1.4.90.15.02` incendio hipotecarios) y la fila está marcada INTERMEDIARIO / REVISAR CLASIFICACION o la referencia dice DESGRAVAMEN/INCENDIO → se carga sin confirmación (decisión registrada); (2) cuenta sugerida del Excel; (3) regla por proveedor de `reglas-carga-historica.json` (hoy solo SETEL → 4.4.01.05) y (4) tabla CLASIFICACION→CODIGO de la hoja `Listas` del propio contador por TIPO DE GASTO: estas dos quedan `cuentaInferida + requiereConfirmacion` y el cargador las omite salvo `aceptarInferidos = true`. Sin ninguna → rechazo en `VALIDACION.md` (A1/A2 del análisis).

### 3.3 Retenciones emitidas (`CBR.RTV2` + `DRV2`) — solo 2025

Las de 2026 las emitió el SAA (`CBR.RTV2` desde jul-2026): el lote las marca `verificarEnSaa` en la compra y no las crea. Para 2025 la fuente es la hoja `RETENCIONES EMITIDAS` (detalle: clave, número, fecha, bases y %), con la fila de compras como respaldo.

| Excel | SAA |
|---|---|
| N.º RETENCIÓN (`001-002-0000069xx`) | `NUMERO`/estab/punto/secuencial; el punto `002` no existe en `CBR.PTEM`: **se crea** la primera vez (`Punto 002 (historico, creado por la carga C11)`), sin `NXPE` |
| CLAVE / AUTORIZACIÓN | `CLAVE` y `AUTORIZACION`; `ESTADO = 5` autorizada, `ESTADOEMISION = 1`, `FECHAAUTORIZACION` |
| FECHA EMISIÓN RETENCIÓN | `FECHA` (= fecha del asiento), `PERIODOFISCAL MM/AAAA` |
| BASE RENTA / % / VALOR | `DRV2` con `CODIMPUESTO = 1`, `CODRETENCION` inferido del % y la cuenta de la factura (`reglas…retencionRenta`: 10 % → 303, o 304B notarías, o 320 arriendos; 5 % → 303A; 3 %, 2,75 %, 1,75 % → 3440; 2 % → 312 o 322 seguros; 1 % → 343 o 310 movilización; 8 % → 304A), `codigoInferido = true` |
| BASE IVA / % / VALOR | `DRV2` con `CODIMPUESTO = 2`, `CODRETENCION` 9/10/1/11/2/3 = 10/20/30/50/70/100 % |
| documento afectado | `TIPODOCRETEN = 01`, `NUMDOCRETEN` = número de la factura, `DOCRES*` de la cabecera de la factura |

La cuenta sale de `CBR.TSRI` (`lsri.tabla` 608 renta / 20 IVA) como en el motor; si el código no tiene cuenta, se rechaza. El % que se graba es el del Excel aunque el catálogo tenga otro (3440 al 3 %). Asiento: `generarAsientoRetencionV2(…, TipoAsientos.RETENCIONES_EMITIDAS_V2, fecha, …)` (Debe `2.3.90.10` proveedor, Haber `2.3.04.90.xx` por código); cruce: `aplicarRetencionEmitida` → `PGS.APLP` tipo 3 con `APLPFAPL` = fecha de la retención. Si el saldo de la factura es menor que la retención (porque ya se pagó completa) se rechaza: el orden correcto es retención antes que pago, y el pago del lote va por el **líquido** (total − retención).

**Se graba por JPA directo, no por `RetencionV2Service.saveSingle`**, porque ese método sobreescribe siempre número, secuencial y clave desde `CBR.NXPE` (consumiría el secuencial vigente 7412 y perdería la clave real). Lo mismo para ventas y NC de venta.

### 3.4 Pagos a proveedores (`PGS.PGTR` + `APLP`, motor de CxP)

Solo para compras `PAGADO` con fecha de pago y cuenta bancaria identificables (`reglas…formasPagoExcel` y `cuentasBancarias`): DÉBITO AUTOMÁTICO / PAGO DESDE CUENTA PROPIA / DÉBITO PROPIA CUENTA → `DEBITO_AUTOMATICO`; TRANSFERENCIA / ORDEN PROCESADA → `TRANSFERENCIA`. Banco de origen: INTERNACIONAL → 520612384 (CNBC 417), PACÍFICO → 1048142735 (418; 7713878 si la referencia lo dice), COOP POLICÍA → 423, JEP → 421, AMAZONAS → 413; "PAGO DESDE CUENTA PROPIA" por el nombre del banco proveedor. **Valor = LIQUIDO A RECIBIR** (total − retención) o el total si no hay retención.

- Débito automático: `registrarPago(idFactura, idCuentaBancaria, null, valor, fechaPago, 1236, idUsuario, obs, true, referencia, 4)` → el motor lo crea CONFIRMADO con `PGTRFPRG = PGTRFRSP = fechaPago`, aplica a la factura (`APLP` tipo 1, `APLPFAPL = fechaPago`), genera el asiento (`generarAsientoPagoTransferenciaCxp`: Debe `2.3.90.10`, Haber cuenta del banco) y el `TSR.MVCB`.
- Transferencia: `registrarPago(…, false, referencia, 2)` deja el pago REGISTRADO (esperando al banco); como la orden de 2025 ya se ejecutó, en la **misma transacción** se hace lo que `confirmarUnPagoManual` haría (`PGTRFRSP = fechaPago`, referencia, `aplicarPagoTransferencia`, estado CONFIRMADO). No se llama a `confirmarUnPagoManual` porque es `REQUIRES_NEW` y no vería el pago aún no confirmado.
- Pagado sin fecha (`pagoSinFecha`, 130 casos; el generador sugiere `fechaPagoSugerida` cuando la referencia trae "transmisión dd/mm/aaaa"), DÉBITO AUTOMÁTICO - NO LOCALIZADO, ABONO PARCIAL, ABONO POSIBLE, REEMBOLSO, pago anterior a la emisión y los PENDIENTE: **no se registra pago**; la factura queda con saldo de proveedor y el caso en `VALIDACION.md` §3 (se resuelve con las órdenes de transferencia / extracto).

### 3.5 Ventas (`CBR.FCTR` + `DTFC`)

Factura con `NUMERO`/`CLAVE`/`AUTORIZACION` del Excel, `ESTADO = 5`, `ESTADOEMISION = 1`, `FCTREPAG = 1`, `FECHA` = emisión, `EMPRESA = 1236` (obligatorio para que `resolverFacturaPorNumero` la encuentre), punto `001-002` (se crea), `AMBIENTE` del facturador. Líneas: una por tarifa (`SUBTOTAL 15%` → código 4, `SUBTOTAL 0%` → 0) con el **producto histórico de la cuenta de ingreso `5.3.01`** (práctica actual: todas las facturas CXC de ago-2026 acreditan `5.3.01 ARRIENDOS` + `2.3.04.90.02 IVA` y debitan `1.4.90.10`). Asiento: `generarAsientoFactura(…, TipoAsientos.FACTURAS_VENTA, fecha, …)`. Las ventas no llevan centro de costo (el Excel no lo trae).

### 3.6 Cobros (`CBR.APLC`)

Para `PAGADA` y `PAGADA - REVISAR REGISTRO` con fecha y medio de pago con cuenta (PACÍFICO 1048142735 → 418; AMAZONAS 450-2-42597-3 → 413): `aplicarCobroTransferencia(idFactura, valor, fechaCobro, referencia, idCuentaBancaria, 1236, idUsuario, obs)` → asiento T-I (Debe banco, Haber `1.4.90.10`), `APLC` tipo 1 con `APLCFAPL = fechaCobro`, `MVCB`. Valor = VALOR CONCILIADO (o total − retención). `SIN REGISTRO`, `PENDIENTE`, `PAGO PARCIAL`, vacío, "NO IDENTIFICADO", sin fecha, o cobro anterior a la emisión (anticipo) → **saldo de cliente** + `VALIDACION.md` §3. Las cobradas con NC (`NOTA DE CRÉDITO 001-001-00000000x`) se cierran con la NC de venta.

### 3.7 Retenciones recibidas (`PGS.RCV2` + `DRC2`)

Hoja `RETENCIONES RECIBIDAS` (detalle) ligada a la venta por N.º DOCUMENTO AFECTADO. Cabecera como la crea la bandeja (`ESTADO = 1`, `ESTADOEMISION = 2`, `PROVEEDOR` = el cliente que retiene, `CLAVE`/`AUTORIZACION`, `FECHA`), `DRC2` con `NUMDOCRETEN` sin guiones, códigos renta/IVA como en 3.3 pero contra `PGS.TSRI` (cuentas `1.9.01.90.xx`). Asiento `generarAsientoRetencionCompraV2(…, TipoAsientos.RETENCIONES_RECIBIDAS_V2, fecha, …)` y cruce `aplicarRetencionRecibidaV2` (`APLC` tipo 3). Las de ago-2026 en adelante ya están en el SAA (`verificarEnSaa`). Las "NO LOCALIZADA EN VENTAS" (retenciones de IFIs sobre rendimientos, 29) no son de CxC: quedan en §3 de `VALIDACION.md`.

### 3.8 Notas de crédito

- **De compra (`PGS.NTCC` + `DTCC`)**: cabecera como la factura, `NUMDOCMODIFICADO` = número de la factura afectada (debe estar cargada), cuenta = la sugerida o, si "NO ENCONTRADA", **la de la factura afectada**; centro = el de la factura. Asiento `generarAsientoNotaCreditoCompra(…, TipoAsientos.NOTAS_CREDITO_COMPRA, fecha, …)`; como ese motor acredita un "gasto genérico" (primer grupo con cuenta), el cargador **reemplaza la cuenta de esa línea HABER** (descripción `NC Compra reverso gasto`) por la cuenta de la NC y le pone el centro. Cruce `aplicarNotaCredito(…, resultado)`: aplica hasta el saldo de la factura; el resto queda como saldo de la NC (`NC-EXCEDE-SALDO-FACTURA.md`), se informa en el mensaje.
- **De venta (`CBR.NTCR` + `DTNC`)**: cabecera con número/clave del Excel, `ESTADO = 5`, `FACTURA` = la afectada; líneas con el producto histórico de `5.3.01`; asiento `generarAsientoNotaCredito(…, TipoAsientos.FACTURAS_VENTA, …)` (mismo alterno que usa `NotaCreditoServiceImpl`); cruce `aplicarNotaCredito` (`APLC` tipo 2).

## 4. Las dos decisiones de diseño

### 4.1 Cómo se impone la cuenta del Excel por línea

El motor de CxP decide la cuenta de gasto **solo** por `PGS.PRDP → PGS.GRPP.PLNNCDGO` (no hay plantilla ni cuenta en la línea; la búsqueda normal del producto es por descripción del XML). El de CxC, por `CBR.PRDC → CBR.GRPC.PLNNCDGO`. Para no tocar los motores ni la operación normal, cada línea del lote se graba con un **producto histórico `CARGA HISTORICA <cuenta>`** que vive en el grupo activo de la empresa cuya cuenta es exactamente la de la línea (el que más productos tenga, nunca POR CLASIFICAR); si ninguna agrupación tiene esa cuenta se crea el grupo `CARGA HISTORICA <cuenta> <nombre>` (`GRPPRYYA/RZZA = 2/2` como los grupos creados a mano, sustento `01` para cuentas 4.x, estado 1). Son productos y grupos normales: el frontend los ve, se pueden renombrar o reclasificar. Según `VALIDACION.md` §7 hoy faltan grupos para `4.3.01.45`, `4.4.05.05`, `4.4.05.25`, `1.9.01.15` y alguna más.

### 4.2 Cómo se impone el centro de costo

Ningún motor llena `CNT.DTAS.CNCSCDGO`. El Excel trae **un** centro por factura, así que después de que el motor genera el asiento el cargador recorre sus líneas y pone el centro en las que tienen la cuenta de gasto de la factura (no en IVA, proveedor ni redondeo). Requiere el catálogo del script `c11-01` (`1.01`, `1.02`); sin él, `validarLote` rechaza el lote entero. Es el único punto donde la carga escribe sobre un asiento ya generado (además de la cuenta de la línea de gasto de la NC de compra, §3.8).

## 5. Asientos que genera cada documento

Todos con **la fecha del documento** (o del pago/cobro) y por el camino `generarAsiento(..., moduloOrigen)`: con la regla de tipos automáticos vigente desde el 1-jun-2025 (C12) salen como **IN-A / EG-A / DI-A** con numeración mensual por tipo y numeración paralela por módulo (`CXP-2025-06-0001`, `CXC-…`), sin nada especial en este código (D-006). El alterno que se pasa (`FACTURAS_COMPRA = 3`, `FACTURAS_VENTA = 2`, `PAGO_TRANSFERENCIA_CXP = 5`, `COBRO_TRANSFERENCIA_CXC = 4`, `RETENCIONES_*_V2 = 3`) solo se usaría si la regla no estuviera vigente.

| Documento | Debe | Haber | Módulo origen | Tipo esperado |
|---|---|---|---|---|
| Factura de compra | gasto (cuenta del Excel, con centro) + `1.9.01.90.15` IVA | `2.3.90.10` proveedor | CXP | DI-A |
| Retención emitida | `2.3.90.10` proveedor | `2.3.04.90.xx` por código | CXC (así lo graba el motor de RTV2) | DI-A |
| Pago (débito/transferencia) | `2.3.90.10` proveedor | `1.1.02.05.xx` banco | CXP | **EG-A** |
| Factura de venta | `1.4.90.10` clientes | `5.3.01` arriendos + `2.3.04.90.02` IVA | CXC | DI-A |
| Retención recibida | `1.9.01.90.xx` retenciones a favor | `1.4.90.10` clientes | CXC | DI-A |
| Cobro | `1.1.02.05.xx` banco | `1.4.90.10` clientes | CXC | **IN-A** |
| NC de compra | `2.3.90.10` proveedor | gasto (cuenta impuesta, con centro) + IVA | CXP | DI-A |
| NC de venta | `5.3.01` + `2.3.04.90.02` | `1.4.90.10` | CXC | DI-A |

Trazabilidad: la observación del documento y del asiento empiezan o contienen `CARGA HISTORICA [control asoprep 27-oct-2026] mes AAAA-MM | <ref del lote> | …`; los pagos y cobros llevan además la referencia bancaria del Excel.

## 6. Idempotencia y transacciones

- **Clave de acceso**: compra (`FCTC.CLAVE`, estado 1), retención emitida (`RTV2.CLAVE` o número), venta (`FCTR.CLAVE` o número), retención recibida (`RCV2.CLAVE`), NC (`NTCC.CLAVE`, `NTCR.CLAVE` o número) → si ya existe, **OMITIDO** con el id y el asiento existentes.
- **Pagos y cobros**: una aplicación activa tipo 1 sobre la misma factura con la misma fecha y el mismo valor (±0,01) → OMITIDO. Además se valida contra el saldo (`saldoFactura`).
- **Titulares**: por identificación; solo se completa lo que falte.
- Cada documento corre en su `REQUIRES_NEW`; un error deja RECHAZADO con la causa raíz y no toca lo demás. Volver a enviar el mismo lote repite solo lo que falló. Los catálogos (producto/grupo histórico, punto de emisión 002) se crean dentro de la transacción del primer documento que los necesita.
- `validarLote` (antes de tocar nada): períodos de todas las fechas del lote existen y no están CERRADOS ni MAYORIZADOS (ene-2026 está mayorizado: desmayorizar antes de cargar), facturador con `GENERACONTA = 1`, usuario, cuentas de las líneas (existen, activas, de movimiento), centros `1.01/1.02`, cuentas bancarias de pagos y cobros (de la empresa, con cuenta contable). Cualquier error devuelve 400 sin procesar.

## 7. Cómo se corre

```powershell
# 1. (una vez) catálogo de centros de costo, revisado y con COMMIT manual
#    docs/logica-negocio/cnt/sql/c11-01-centros-de-costo-asoprep.sql
# 2. generar los lotes (expediente)
node 03-trabajo/cxc-cxp/scripts/03-correr-inventario.js sql/c11-00-catalogos.sql
node 03-trabajo/cxc-cxp/scripts/07-genera-lotes.js
# 3. validar y cargar junio 2025
$lote = Get-Content -Raw -Encoding UTF8 03-trabajo/cxc-cxp/lotes/2025-06/lote.json
Invoke-RestMethod -Method Post -Uri http://localhost:8080/SaaBE/rest/historico/validarHistoricoCxcCxp -ContentType 'application/json; charset=utf-8' -Body $lote
Invoke-RestMethod -Method Post -Uri http://localhost:8080/SaaBE/rest/historico/cargarHistoricoCxcCxp  -ContentType 'application/json; charset=utf-8' -Body $lote | ConvertTo-Json -Depth 4 > lotes/2025-06/resultado.json
# 4. saldos iniciales (D-013, §10): una sola vez, en cualquier momento (no depende de junio)
node 03-trabajo/cxc-cxp/scripts/03-correr-inventario.js sql/c11-02-saldos-iniciales.sql
node 03-trabajo/cxc-cxp/scripts/09-genera-saldos-iniciales.js
$si = Get-Content -Raw -Encoding UTF8 03-trabajo/cxc-cxp/lotes/2025-05/lote.json
Invoke-RestMethod -Method Post -Uri http://localhost:8080/SaaBE/rest/historico/validarHistoricoCxcCxp -ContentType 'application/json; charset=utf-8' -Body $si
Invoke-RestMethod -Method Post -Uri http://localhost:8080/SaaBE/rest/historico/cargarHistoricoCxcCxp  -ContentType 'application/json; charset=utf-8' -Body $si | ConvertTo-Json -Depth 4 > lotes/2025-05/resultado.json
```

Respuesta: `{ mes, exito, soloValidar, erroresLote[], creados, omitidos, rechazados, validados, items[ { tipo, ref, documento, estado, mensaje, id, idAsiento, asiento } ] }`. 200 con `items` aunque haya rechazados; 400 si `erroresLote` no está vacío.

## 8. SELECT de control por mes (solo lectura; `:mes` = 'YYYY-MM')

```sql
-- 8.1 Documentos creados por la carga en el mes (por la observación de trazabilidad)
SELECT 'FCTC' tabla, COUNT(*) n, SUM(TOTAL) total FROM PGS.FCTC WHERE OBSERVACION LIKE 'CARGA HISTORICA [%] mes ' || :mes || '%' AND ESTADO = 1
UNION ALL SELECT 'RTV2', COUNT(*), SUM(TOTAL) FROM CBR.RTV2 WHERE OBSERVACION LIKE 'CARGA HISTORICA [%] mes ' || :mes || '%' AND ESTADOEMISION <> 3
UNION ALL SELECT 'FCTR', COUNT(*), SUM(TOTAL) FROM CBR.FCTR WHERE OBSERVACION LIKE 'CARGA HISTORICA [%] mes ' || :mes || '%' AND ESTADO = 5
UNION ALL SELECT 'RCV2', COUNT(*), SUM(TOTAL) FROM PGS.RCV2 WHERE OBSERVACION LIKE 'CARGA HISTORICA [%] mes ' || :mes || '%' AND ESTADO = 1
UNION ALL SELECT 'NTCC', COUNT(*), SUM(TOTAL) FROM PGS.NTCC WHERE OBSERVACION LIKE 'CARGA HISTORICA [%] mes ' || :mes || '%' AND ESTADO = 1
UNION ALL SELECT 'NTCR', COUNT(*), SUM(TOTAL) FROM CBR.NTCR WHERE OBSERVACION LIKE 'CARGA HISTORICA [%] mes ' || :mes || '%' AND ESTADO = 5
UNION ALL SELECT 'PGTR pagos', COUNT(*), SUM(PGTRVLOR) FROM PGS.PGTR WHERE PGTROBSR LIKE 'CARGA HISTORICA [%] mes ' || :mes || '%' AND PGTRESTD = 3
UNION ALL SELECT 'APLC cobros', COUNT(*), SUM(APLCMAPL) FROM CBR.APLC WHERE APLCOBSR LIKE 'CARGA HISTORICA [%] mes ' || :mes || '%' AND APLCESTD = 1;
-- Esperado: igual al resumen del lote (compras/comprasTotal, retencionesEmitidas, ventas, pagos/pagosTotal, cobros/cobrosTotal...) menos los RECHAZADOS del resultado.

-- 8.2 Asientos del mes por tipo y por módulo origen (todos deben tener ASNTMDOR y numero por modulo)
SELECT t.PLNTNMBR tipo, a.ASNTMDOR modulo, COUNT(*) n, SUM(d.DTASDBEE) debe, MIN(a.ASNTNAMD) primero, MAX(a.ASNTNAMD) ultimo
FROM CNT.ASNT a JOIN CNT.PLNT t ON t.PLNTCDGO = a.PLNTCDGO JOIN CNT.DTAS d ON d.ASNTCDGO = a.ASNTCDGO
WHERE a.PJRQCDGO = 1236 AND a.ASNTESTD = 1 AND TO_CHAR(a.ASNTFCHA,'YYYY-MM') = :mes
GROUP BY t.PLNTNMBR, a.ASNTMDOR ORDER BY 1, 2;

-- 8.3 Líneas de gasto con centro de costo (todas las compras del mes deben tenerlo; ventas no)
SELECT c.CNCSNMRO centro, COUNT(*) lineas, SUM(d.DTASDBEE) debe
FROM CNT.DTAS d JOIN CNT.ASNT a ON a.ASNTCDGO = d.ASNTCDGO LEFT JOIN CNT.CNCS c ON c.CNCSCDGO = d.CNCSCDGO
WHERE a.PJRQCDGO = 1236 AND a.ASNTESTD = 1 AND TO_CHAR(a.ASNTFCHA,'YYYY-MM') = :mes AND d.DTASDBEE > 0
  AND d.DTASCNTA NOT LIKE '1.9.01.90%' AND d.DTASCNTA NOT IN ('2.3.90.10','1.4.90.10') AND d.DTASCNTA NOT LIKE '1.1.02.05%'
GROUP BY c.CNCSNMRO;

-- 8.4 Saldo de proveedores al cierre del mes (facturas activas − aplicaciones activas hasta esa fecha) vs Excel (PENDIENTE + pagadas sin fecha)
SELECT t.TTLRIDNT identificacion, CASE WHEN t.TTLRRZZA = 2 THEN NVL(t.TTLRRZSC, t.TTLRNMBR) END razon_social,
       SUM(f.TOTAL) facturado, SUM(NVL(ap.aplicado,0)) aplicado, SUM(f.TOTAL - NVL(ap.aplicado,0)) saldo
FROM PGS.FCTC f JOIN TSR.TTLR t ON t.TTLRCDGO = f.TITULAR
LEFT JOIN (SELECT APLPFCTC, SUM(APLPMAPL) aplicado FROM PGS.APLP WHERE APLPESTD = 1 AND APLPFAPL <= LAST_DAY(TO_DATE(:mes,'YYYY-MM')) GROUP BY APLPFCTC) ap ON ap.APLPFCTC = f.ID
WHERE f.ESTADO = 1 AND f.EMPRESA = 1236 AND f.FECHA <= LAST_DAY(TO_DATE(:mes,'YYYY-MM'))
GROUP BY t.TTLRIDNT, t.TTLRRZZA, t.TTLRRZSC, t.TTLRNMBR HAVING SUM(f.TOTAL - NVL(ap.aplicado,0)) > 0.01 ORDER BY saldo DESC;
-- Contraste: en el Excel, compras con FECHA EMISION <= fin de mes cuyo pago no esté fechado dentro del mes (lotes/VALIDACION.md §3 lista las pagadas sin fecha).

-- 8.5 Saldo de clientes al cierre del mes vs Excel (SALDO PENDIENTE SEGÚN ARCHIVO + cobros sin fecha)
SELECT t.TTLRIDNT identificacion, CASE WHEN t.TTLRRZZA = 2 THEN NVL(t.TTLRRZSC, t.TTLRNMBR) END razon_social,
       SUM(f.TOTAL) facturado, SUM(NVL(ap.aplicado,0)) aplicado, SUM(f.TOTAL - NVL(ap.aplicado,0)) saldo
FROM CBR.FCTR f JOIN TSR.TTLR t ON t.TTLRCDGO = f.COMPRADOR
LEFT JOIN (SELECT APLCFCTR, SUM(APLCMAPL) aplicado FROM CBR.APLC WHERE APLCESTD = 1 AND APLCFAPL <= LAST_DAY(TO_DATE(:mes,'YYYY-MM')) GROUP BY APLCFCTR) ap ON ap.APLCFCTR = f.ID
WHERE f.ESTADO = 5 AND f.ESTADOEMISION <> 3 AND f.EMPRESA = 1236 AND f.FECHA <= LAST_DAY(TO_DATE(:mes,'YYYY-MM'))
GROUP BY t.TTLRIDNT, t.TTLRRZZA, t.TTLRRZSC, t.TTLRNMBR HAVING SUM(f.TOTAL - NVL(ap.aplicado,0)) > 0.01 ORDER BY saldo DESC;

-- 8.6 Cuadre con contabilidad: saldo de 2.3.90.10 y 1.4.90.10 al cierre del mes (debe igualar 8.4/8.5 + saldo de apertura)
SELECT p.PLNNCNTA, SUM(NVL(d.DTASDBEE,0)) - SUM(NVL(d.DTASHBRR,0)) saldo
FROM CNT.DTAS d JOIN CNT.ASNT a ON a.ASNTCDGO = d.ASNTCDGO JOIN CNT.PLNN p ON p.PLNNCDGO = d.PLNNCDGO
WHERE a.PJRQCDGO = 1236 AND a.ASNTESTD = 1 AND a.ASNTFCHA <= LAST_DAY(TO_DATE(:mes,'YYYY-MM')) AND p.PLNNCNTA IN ('2.3.90.10','1.4.90.10','1.9.01.90.15','2.3.04.90.02')
GROUP BY p.PLNNCNTA;

-- 8.7 Catálogos que la carga creó (para revisarlos y, si se quiere, renombrarlos)
SELECT 'GRPP' t, GRPPCDGO id, GRPPNMBR nombre FROM PGS.GRPP WHERE PJRQCDGO = 1236 AND GRPPNMBR LIKE 'CARGA HISTORICA%'
UNION ALL SELECT 'PRDP', ID, NOMBRE FROM PGS.PRDP WHERE EMPRESA = 1236 AND NOMBRE LIKE 'CARGA HISTORICA%'
UNION ALL SELECT 'PRDC', ID, NOMBRE FROM CBR.PRDC WHERE EMPRESA = 1236 AND NOMBRE LIKE 'CARGA HISTORICA%'
UNION ALL SELECT 'PTEM', ID, CODIGO || ' ' || NOMBRE FROM CBR.PTEM WHERE NOMBRE LIKE '%carga C11%';
```

## 9. Lo que queda a decisión humana (resumen; detalle en `lotes/VALIDACION.md`)

1. **Compras sin cuenta** (A1/A2): "REVISAR CLASIFICACION" no asegurador (Varfiles, El Universo, menores) y sin cuenta sugerida ni tipo de gasto (65 → quedan rechazadas hasta que el contador asigne cuenta; SETEL propuesta 4.4.01.05 y las de la hoja Listas se cargan solo con `aceptarInferidos`).
2. **Pagadas sin fecha de pago** (A5, 130) y **pagos sin cuenta bancaria identificable**: la factura queda por pagar; se resuelve con las órdenes de transferencia del 27-oct y se carga después como lote de pagos (mismo formato, solo `pagos`).
3. **Abonos parciales, abonos posibles, reembolsos, débitos no localizados**: por pagar + decisión.
4. **Cobros/pagos anteriores a la emisión** (anticipos): fuera de esta carga; el módulo de anticipos (CBR.ANTC / PGS.ANTP) con fecha dentro del período re-procesado, si el líder lo decide.
5. **Ventas PAGADA - REVISAR REGISTRO sin fecha / SIN REGISTRO** (A9): saldo de cliente.
6. **Retenciones recibidas de IFIs** (29, "NO LOCALIZADA EN VENTAS"): no son de CxC; van con la contabilidad de inversiones (C5).
7. **Códigos de retención inferidos** (todos los de renta): confirmar la tabla `retencionRenta` de las reglas con el contador antes de la carga definitiva; el código define la cuenta `2.3.04.90.xx`.
8. **Aseguradores con cuenta de gasto explícita** (Equisuiza jun-2025 "PAGO DIF SEGURO", AIG 480110): el Excel manda hoy; revisar contra D-004.
9. **Enero 2026 mayorizado** (`CNT.PRDO` estado 2 sin asientos): desmayorizar antes del lote 2026-01.
10. **Retenciones emitidas y recibidas de 2026 que ya existen en el SAA**: el lote las marca `verificarEnSaa`; el cruce número a número contra `CBR.RTV2` / `PGS.RCV2` es el paso siguiente (A7).

## 10. Saldos iniciales por tercero al 31-may-2025 (D-013)

**Decisión D-013 (líder, 28-oct):** los saldos de clientes y proveedores del balance de arranque se cargan en los módulos a un titular **SALDO INICIAL**; cuando aparezca el origen de un saldo se reasigna sacándolo de ese titular. La contabilidad de esos saldos **ya está** en el asiento de apertura (id 12043, `DI-A-2025-05-0001` / `CNT-2025-05-0001`): `2.3.90.10` haber 44.019,59 y `1.4.90.10` debe 45.373,99. Lo que se carga en los módulos no genera asiento.

### 10.1 Cómo se representa

| Pieza | Valor |
|---|---|
| Titular genérico | `TSR.TTLR` identificación **`9999999999001`**, RUC, jurídica, nombre/razón social `SALDO INICIAL`, cliente y proveedor, rol `PRRL` 1 y 2 con `PRCC` tipo 1 a `1.4.90.10` y `2.3.90.10` (mismo camino que cualquier titular del lote, §3.1). Los genéricos de producción (`9999999999` "PROVEEDORES JULIO 2026", cédula; `99999999` "CLIENTES A JULIO 2026") son de la salida en vivo de jul-2026 y no se reutilizan; el `001` final distingue al nuevo y le da forma de RUC para ATS y frontend. Va en `saldosIniciales.titularGenerico` del lote: se puede cambiar sin tocar código |
| Documento de proveedor | `PGS.FCTC` estado 1, `ESTADOEMISION` 2, `TOTAL = SUBTOTAL = SUBCERO` = saldo, IVA 0, **sin `DFCC`**, `ASIENTO` nulo, sin clave (salvo que se conozca), `FECHA = FECHAREGISTROCONTABLE` = emisión ≤ corte |
| Documento de cliente | `CBR.FCTR` estado 5, `ESTADOEMISION` 1, mismos importes, **sin `DTFC`**, `ASIENTO` nulo, `EMPRESA` 1236, punto de emisión solo si el número es de un punto real (`001-002`); el `999-999` queda sin punto |
| Número | real si se identificó (`001-002-000000531`); el genérico `999-999-000000001`; los reasignados sin comprobante conocido `999-998-NNNNNNNNN` por titular |
| Trazabilidad | observación `CARGA HISTORICA [D-013 saldos iniciales] mes 2025-05 \| <ref> \| SALDO INICIAL D-013 al 2025-05-31, sin asiento: la contabilidad esta en el asiento de apertura … \| monto original … \| origen: …` |

**Por qué sin líneas y sin asiento.** Revisado en el código (28-oct): `saldoFactura` de CxP y CxC es total − aplicaciones activas (`AplicacionPagoCxpServiceImpl` ~1387, `AplicacionPagoCxcServiceImpl` ~826); `registrarPago`, `aplicarPagoTransferencia`, `aplicarRetencionEmitida`, `aplicarNotaCredito`, `aplicarCobroTransferencia`, `aplicarRetencionRecibidaV2` y `resolverFactura(Compra)PorNumero` no leen el asiento de la factura; la cuenta del asiento del pago/cobro sale de la **PRCC del titular** (`obtenerCuentaProveedorPorTipo` / `obtenerCuentaCliente`), por eso el cargador exige que el titular la tenga; la cartera de CxP (`/cartera`) y de CxC no hacen JOIN al asiento; anular una factura sin asiento salta el paso del asiento. Y como `generarAsientoFactura` y `generarAsientoFacturaCompra` lanzan "no tiene detalles activos" sin líneas, **"contabilizar" / "consultar estado SRI" / recontabilizar no pueden generarle por error un asiento que duplicaría la apertura**. Riesgos que quedan: (1) el **ATS** de mayo 2025 (ya presentado) incluiría estos documentos si se regenerara: no regenerarlo desde el SAA; (2) la FCTC aparece en `/sustentoPendiente` (sustento nulo, igual que las compras de la carga); (3) el frontend puede mostrar una factura de venta sin punto de emisión y sin XML.

### 10.2 Lote `2025-05` (bloque `saldosIniciales`)

```json
{ "mes": "2025-05", "observacion": "CARGA HISTORICA [D-013 saldos iniciales] mes 2025-05", "idEmpresa": 1236, "idUsuario": 38, "titulares": [ …titulares reales de las partidas identificadas… ],
  "saldosIniciales": { "fecha": "2025-05-31", "idAsientoApertura": 12043, "asientoApertura": "DI-A-2025-05-0001 (CNT-2025-05-0001)",
    "titularGenerico": { "identificacion": "9999999999001", "tipoIdentificacion": "RUC", "tipoPersona": "JURIDICA", "razonSocial": "SALDO INICIAL" },
    "proveedores": [ { "ref": "SI-P-GEN", "esGenerico": true, "numero": "999-999-000000001", "fechaEmision": "2025-05-31", "monto": 44019.59, "origen": "…" } ],
    "clientes":    [ { "ref": "SI-C-0001", "esGenerico": false, "identificacion": "1793114504001", "numero": "001-002-000000531", "fechaEmision": "2025-05-29", "monto": 1504.46, "origen": "mayor 149010 …" }, …,
                     { "ref": "SI-C-GEN", "esGenerico": true, "numero": "999-999-000000001", "monto": 39449.61, … } ] } }
```

Lo genera `cuadreSuper/03-trabajo/cxc-cxp/scripts/09-genera-saldos-iniciales.js` desde `sql/c11-02-saldos-iniciales.sql` (líneas del asiento 12043) y `scripts/saldos-iniciales-identificados.json` (partidas con titular real; el genérico es la diferencia). Orden en el cargador: titulares del lote → titular genérico → documentos de proveedores → de clientes → (si los hubiera) el resto del lote.

`validarLote` añade: el asiento `idAsientoApertura` existe, es de la empresa y está activo; **la suma de los documentos de cada lado es exactamente el saldo de `2.3.90.10` (haber − debe) y de `1.4.90.10` (debe − haber) en `CNT.DTAS` de ese asiento** (si no, 400); montos > 0, número de 15 dígitos, fecha ≤ corte, sin repetidos. Un lote que solo trae saldos iniciales **no exige el período de mayo abierto** (no genera asientos). Idempotencia: empresa + titular + número (OMITIDO si ya existe). Cada documento en su `REQUIRES_NEW`.

**Identificado al 28-oct** (mayor de mayo 2025 del cliente, hoja 149010): facturas de arriendo de mayo 531, 534 y 537 (1.504,46 + 1.406,56 + 3.013,36 = **5.924,38**), sin cobro al 31-may, a sus titulares reales (una sociedad y dos personas naturales, identificación comprobada contra la hoja de ventas del control). Queda en SALDO INICIAL: **clientes 39.449,61** (bloque anterior a 2025) y **proveedores 44.019,59** completo (todo lo registrado en 239010 en 2025 se pagó en 2025 salvo 2,90 de residuos; el control empieza en junio 2025 y ningún pago de junio es de una factura de mayo, así que el lote de junio no cambia).

### 10.3 Reasignación (`POST /SaaBE/rest/historico/reasignarSaldoInicial`)

Cuando aparece el origen de una parte del saldo (un pago de jun-2025 a una factura de mayo, un extracto, una conciliación), se mueve del genérico al titular real **sin tocar la contabilidad de mayo**:

```json
{ "idEmpresa": 1236, "idUsuario": 38, "soloValidar": true, "lado": "PROVEEDOR", "identificacionDestino": "1790000000001",
  "razonSocialDestino": "…", "tipoIdentificacionDestino": "RUC", "tipoPersonaDestino": "JURIDICA",
  "monto": 1234.56, "fecha": "2026-11-05", "decision": "D-014", "motivo": "pago de 12-jun-2025 orden 123 a la factura 001-001-000000999 de mayo",
  "numeroDocumento": "001-001-000000999", "fechaEmisionDocumento": "2025-05-20", "claveAcceso": null }
```

Qué hace, en una transacción: (1) busca el documento genérico (`identificacionGenerico`/`numeroGenerico`, por defecto `9999999999001` / `999-999-000000001`) del lado y lo bloquea (`PESSIMISTIC_WRITE`); (2) exige `monto ≤ saldo` del genérico, fecha de emisión ≤ fecha del genérico (un documento posterior al corte no es saldo inicial: va por la carga del mes), `decision` y `motivo` (regla 4 del expediente: sin decisión registrada no se aplica); (3) crea o completa el titular destino (rol + PRCC como §3.1); (4) crea el documento del titular real como en 10.1 (sin líneas ni asiento) con `numeroDocumento` o `999-998-NNNNNNNNN`; (5) **rebaja el total del genérico** en el monto y le anexa a la observación `REASIG <decision> <fecha>: <monto> de 999-999-000000001 (id …) -> <número> (id …)`; el destino lleva la misma traza y el motivo. La suma de documentos D-013 de cada lado sigue igual a la apertura (control `c11-02` bloque 04). Idempotente: si el documento destino ya existe (por número) o ya hay una reasignación con la misma `decision` al mismo titular, OMITIDO y no mueve nada. `soloValidar` informa sin grabar. Respuesta: el mismo `ResultadoCargaHistorica` con un ítem (CREADO/OMITIDO/RECHAZADO/VALIDADO); 400 si faltan campos o se rechaza.

Después de reasignar, el pago o cobro real se carga contra el documento destino con el lote del mes del pago (mismo formato, `facturaNumero` = número destino): el motor contabiliza Debe `2.3.90.10` / Haber banco (o Debe banco / Haber `1.4.90.10`) con la PRCC del titular, y el saldo de la cuenta sigue cuadrando con la cartera.

### 10.4 Control (solo SELECT)

`cuadreSuper/03-trabajo/cxc-cxp/sql/c11-02-saldos-iniciales.sql`: 01 saldos de la apertura, 02 titulares y su cuenta de cartera, 03 documentos D-013 con líneas (debe ser 0), asiento (debe ser nulo) y saldo, 04 suma de documentos vs apertura (diferencia 0 antes y después de cada reasignación).

## 11. Pendiente de probar en WildFly (criterio de aceptación de C11)

1. `c11-01` aplicado (4 filas en `CNT.CNCS`), `validarHistoricoCxcCxp` con `lotes/2025-06/lote.json` → 200, `erroresLote = []`, todos VALIDADO salvo la compra con `requiereConfirmacion`.
2. `cargarHistoricoCxcCxp` del mismo lote → 55 compras, 14 retenciones, 20 pagos, 11 ventas, 2 retenciones recibidas, 5 cobros creados (36 titulares, 13 nuevos); 8.1 coincide con el resumen del lote; 8.2 muestra DI-A/EG-A/IN-A con `ASNTMDOR` CXP/CXC y numeración mensual; 8.3 todas las líneas de gasto con centro 1.01/1.02.
3. Repetir la carga → todo OMITIDO, cero filas nuevas.
4. 8.4/8.5 contra el Excel de junio: proveedores con saldo = PENDIENTE (18) + VALIDAR DÉBITO (2) + pagadas sin fecha (9) + abono posible (1) + anulada con NC (1); clientes con saldo = 544 (sin fecha), 547 y 550 (cobro anterior a la emisión), 548 (sin registro) y las cobradas en meses siguientes (543 en 2026-01, 552 en 2025-12). Con D-013 cargado, 8.4/8.5 muestran además los documentos de saldo inicial y 8.6 cuadra sin sumar aparte la apertura.
5. Saldos iniciales: `validarHistoricoCxcCxp` con `lotes/2025-05/lote.json` → 200; cargarlo → 1 titular creado (SALDO INICIAL), 5 documentos creados; `c11-02` bloque 03 con 0 líneas y asiento nulo, bloque 04 con diferencia 0; repetir → todo OMITIDO; `reasignarSaldoInicial` con `soloValidar` y luego real (monto pequeño, decisión de prueba) → bloque 04 sigue en 0; `/cartera` de CxP y CxC muestra los documentos.
6. Lotes de junio en adelante: sin cambios por D-013 (ningún pago/cobro de junio va contra facturas de mayo según el control).
