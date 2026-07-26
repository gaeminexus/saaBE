# PLAN DE IMPLEMENTACIÓN - PENDIENTES ASOPREP

**Fecha inicio:** 2026-07-25  
**Responsable:** Equipo de desarrollo  
**Estado general:** 🟡 En progreso

---

## DECISIONES ARQUITECTURALES TOMADAS

- ✅ Caja chica se manejará como cuenta bancaria (banco ficticio "CAJA CHICA" + 2 cuentas, una por cada caja)
- ✅ Trazabilidad contable de documentos CXP: campo `ASNTCDGO` directo en tablas cabecera (igual que CXC, sin tabla intermedia)
- ✅ No se crea tabla `AsientoXDocumentoCompra`
- ✅ Las tablas marcadas para deprecar **no se eliminan de la BD** hasta confirmar que no hay datos activos y que el frontend no las consume

---

## TABLAS MARCADAS PARA DEPRECAR (no eliminar aún)

### CXC (schema: CBR)
- [ ] `DocumentoCobro` (DCMC) y familia:
  - `DetalleDocumentoCobro`
  - `FinanciacionXDocumentoCobro`
  - `CuotaXFinanciacionCobro`
  - `ComposicionCuotaInicialCobro`
  - `PagosArbitrariosXFinanciacionCobro`
  - `ResumenValorDocumentoCobro`
- [ ] `ValorImpuestoDocumentoCobro` / `ValorImpuestoDetalleCobro`
- [ ] `GrupoProductoCobro` / `ImpuestoXGrupoCobro` / `ProductoCobro`
- [ ] Todas las entidades `Temp*` de CXC:
  - `TempDocumentoCobro`, `TempDetalleDocumentoCobro`, `TempFinanciacionXDocumentoCobro`
  - `TempCuotaXFinanciacionCobro`, `TempComposicionCuotaInicialCobro`
  - `TempPagosArbitrariosXFinanciacionCobro`, `TempResumenValorDocumentoCobro`
  - `TempValorImpuestoDetalleCobro`, `TempValorImpuestoDocumentoCobro`

### CXP (schema: PGS)
- [ ] `DocumentoPago` (DCMP) y familia:
  - `DetalleDocumentoPago`
  - `FinanciacionXDocumentoPago`
  - `CuotaXFinanciacionPago`
  - `ComposicionCuotaInicialPago`
  - `PagosArbitrariosXFinanciacionPago`
  - `ResumenValorDocumentoPago`
- [ ] `ValorImpuestoDocumentoPago` / `ValorImpuestoDetallePago`
- [ ] `GrupoProductoPago` / `ImpuestoXGrupoPago` / `ProductoPago`
- [ ] `DocumentoCxp`
- [ ] Familia de aprobaciones:
  - `ProposicionPagoXCuota`, `AprobacionXProposicionPago`
  - `AprobacionXMonto`, `MontoAprobacion`, `UsuarioXAprobacion`
- [ ] Todas las entidades `Temp*` de CXP:
  - `TempDocumentoPago`, `TempDetalleDocumentoPago`, `TempFinanciacionXDocumentoPago`
  - `TempCuotaXFinanciacionPago`, `TempComposicionCuotaInicialPago`
  - `TempPagosArbitrariosXFinanciacionPago`, `TempResumenValorDocumentoPago`
  - `TempValorImpuestoDetallePago`, `TempValorImpuestoDocumentoPago`
  - `TempAprobacionXMonto`, `TempMontoAprobacion`, `TempUsuarioXAprobacion`

### TSR (schema: TSR)
- [ ] Bloque de cajas físicas:
  - `CajaFisica` (CJAA), `CajaLogica` (CJCN), `GrupoCaja` (CJIN)
  - `CierreCaja` (CRCJ), `DetalleCierre`, `AuxDepositoCierre`, `AuxDepositoDesglose`
  - `UsuarioPorCaja` (USXC)
- [ ] Sub-tipos de cobro en caja:
  - `CobroCheque`, `CobroEfectivo`, `CobroTarjeta`, `CobroRetencion`, `CobroTransferencia`
- [ ] Todas las entidades `Temp*` de TSR:
  - `TempCobro`, `TempCobroCheque`, `TempCobroEfectivo`
  - `TempCobroRetencion`, `TempCobroTarjeta`, `TempCobroTransferencia`
  - `TempDebitoCredito`, `TempMotivoCobro`, `TempMotivoPago`, `TempPago`
- [ ] `PersonaCuentaContable` / `PersonaRol` / `DireccionPersona` / `TelefonoDireccion` → mover a CRD

---

## PENDIENTES POR PRIORIDAD

---

### 🔴 PRIORIDAD 1 — CARGA DE ARCHIVOS CXP + GENERACIÓN DE CONTABILIDAD

**Objetivo:** Cargar archivos TXT/XML descargados del SRI, clasificarlos en sus tablas específicas y generar contabilidad.  
**Estado:** 🔴 Pendiente

#### 1.1 Agregar campo `ASIENTO` a tablas cabecera CXP

- [x] `FacturaCompra` (PGS.FCTC) — campo `ASIENTO` FK a `CNT.ASNT.ASNTCDGO` ✅
- [x] `NotaCreditoCompra` (PGS.NTCC) — campo `ASIENTO` FK a `CNT.ASNT.ASNTCDGO` ✅
- [x] `LiquidacionCompraCompra` (PGS.LQCC) — campo `ASIENTO` FK a `CNT.ASNT.ASNTCDGO` ✅
- [x] `RetencionCompra` (PGS.RTCM) — campo `ASIENTO` FK a `CNT.ASNT.ASNTCDGO` ✅
- [x] Modelos Java actualizados (`@ManyToOne` + getter/setter en los 4 modelos) ✅
- [x] Script BD creado: `docs/pendientes/scripts/01_ASIENTO_TABLAS_CXP.sql` ✅

#### 1.2 Revisar y completar flujo de carga de archivos

- [x] `CargaArchivoTxt` (CRTX) y `DetalleCargaTxt` (DCTX) — flujo completo ✅
- [x] Parser TXT del SRI — Fase 1 `cargarArchivoTxt` completa con detección de duplicados, novedades y desaparecidos ✅
- [x] Parser XML del SRI — Fase 2+3 unificada `cargarXmlYRegistrar` completa para los 6 tipos de comprobante ✅
- [x] Clasificación automática por tipo: Factura / NC / ND / Liquidación / Retención V1 / Retención V2 ✅
- [x] Manejo de duplicados por `claveAcceso` única ✅
- [x] Manejo de novedades y documentos desaparecidos en el período ✅
- [x] Resolución de novedades (MANTENER / REEMPLAZAR) — Fase 4 ✅
- [x] Reversión de documentos — Fase 5 ✅
- [x] Endpoints REST completos en `ProcesoCargaDocumentosRest` (10 endpoints) ✅

#### 1.3 Generación de contabilidad por documento CXP

- [x] `generarAsientoCxp` implementado en `ProcesoCargaDocumentosServiceImpl` ✅
- [x] `generarAsientoFacturaCompra` implementado en `AsientoContableServiceImpl` ✅
- [x] `generarAsientoNotaCreditoCompra` implementado ✅
- [x] `generarAsientoNotaDebitoCompra` implementado ✅
- [x] `generarAsientoLiquidacionCompraCompra` implementado ✅
- [x] `generarAsientoRetencionCompra` implementado ✅
- [x] `generarAsientoRetencionCompraV2` implementado ✅
- [x] FK `ASIENTO` grabado de vuelta en la tabla específica después de generar el asiento (`grabarAsientoEnDocumento`) ✅
- [x] Modelos Java `NotaDebitoCompra` y `RetencionCompraV2` — campo `asiento` agregado ✅
- [x] Script BD actualizado con 6 tablas: `docs/pendientes/scripts/01_ASIENTO_TABLAS_CXP.sql` ✅
- [ ] Definir `TipoAsiento` en BD (codigoAlterno 9-14) para cada tipo de documento CXP — **requiere configuración manual en BD**
- [ ] Configurar `GrupoProductoPago.planCuenta` para cada grupo de gasto
- [ ] Configurar `PersonaCuentaContable` (tipoCuenta=1) para cada proveedor

---

### 🔴 PRIORIDAD 2 — ORDEN DE PAGO (puente CXP ↔ TSR)

**Objetivo:** Generar órdenes de pago agrupando documentos de CXP para su posterior ejecución de pago en TSR.  
**Estado:** 🔴 Pendiente

#### 2.1 Nuevas tablas a crear

- [ ] **`PGS.ORDP` → `OrdenPago`** (cabecera)
  - `empresa`, `titular` (proveedor), `fechaEmision`, `fechaVencimiento`
  - `valorBruto`, `valorRetenciones`, `valorNeto`
  - `estado` (BORRADOR / APROBADA / PAGADA / ANULADA)
  - `observacion`, `usuario`, `ASNTCDGO`

- [ ] **`PGS.DRDP` → `DetalleOrdenPago`** (documentos incluidos)
  - `ordenPago` (FK), `tipoDocumento` (FACTURA / LIQUIDACION / NOTA_DEBITO)
  - FK opcional por tipo: `facturaCompra` / `liquidacionCompra` / `notaDebitoCompra`
  - `valor`, `valorRetencionIR`, `valorRetencionIVA`, `valorNeto`

- [ ] **`PGS.RNOP` → `RetencionXOrdenPago`** (retenciones emitidas asociadas)
  - `ordenPago` (FK), `retencionCompra` (FK → RetencionCompra)
  - `tipo` (IR / IVA), `valor`

#### 2.2 Modelos Java a crear

- [ ] `OrdenPago.java`
- [ ] `DetalleOrdenPago.java`
- [ ] `RetencionXOrdenPago.java`

#### 2.3 Servicios a implementar

- [ ] Crear / editar `OrdenPago`
- [ ] Agregar / quitar documentos del detalle
- [ ] Aprobar `OrdenPago` (cambio de estado + generación de asiento)
- [ ] Anular `OrdenPago` (reversión de asiento)
- [ ] Endpoints REST (`OrdenPagoRest`)

---

### 🔴 PRIORIDAD 3 — PAGOS Y ANTICIPOS EN TSR

**Objetivo:** Registrar pagos a proveedores y anticipos, conectados con las órdenes de pago.  
**Estado:** 🔴 Pendiente

#### 3.1 Refactorización de `Pago` (TSR.PGSS)

- [ ] Limpiar referencia a `CierreCaja` en el modelo `Pago`
- [ ] Limpiar referencia a `CajaLogica` en el modelo `Pago`
- [ ] Verificar que `Cobro` (CBRO) también se limpie de referencias a caja

#### 3.2 Nuevas tablas a crear

- [ ] **`TSR.PGOP` → `PagoXOrdenPago`** (aplicación de pago a orden)
  - `pago` (FK → Pago), `ordenPago` (FK → OrdenPago), `valorAplicado`, `fecha`

- [ ] **`TSR.ANTC` → `AnticipoProveedor`** (anticipos a proveedores o clientes)
  - `empresa`, `titular`, `tipoAnticipo` (PROVEEDOR / CLIENTE)
  - `valor`, `saldoDisponible`, `fecha`, `estado`
  - `ASNTCDGO`, `movimientoBanco` (FK → MovimientoBanco)

- [ ] **`TSR.ANTD` → `AplicacionAnticipo`** (uso del anticipo)
  - `anticipo` (FK), `ordenPago` (FK, opcional), `cobro` (FK, opcional)
  - `valorAplicado`, `fecha`

#### 3.3 Modelos Java a crear

- [ ] `PagoXOrdenPago.java`
- [ ] `AnticipoProveedor.java`
- [ ] `AplicacionAnticipo.java`

#### 3.4 Servicios a implementar

- [ ] Registrar pago asociado a `OrdenPago`
- [ ] Registrar anticipo a proveedor / cliente
- [ ] Aplicar anticipo a `OrdenPago` o cobro
- [ ] Generación de `MovimientoBanco` y asiento contable
- [ ] Endpoints REST

---

### 🔴 PRIORIDAD 4 — CONCILIACIÓN BANCARIA EN TSR

**Objetivo:** Conciliar los movimientos bancarios del sistema contra los extractos bancarios reales.  
**Estado:** 🔴 Pendiente

#### 4.1 Verificar trazabilidad en `MovimientoBanco` (TSR.MVCB)

- [ ] Confirmar si existe campo de tipo de origen del movimiento (Pago / Cobro / Transferencia / DebitoCredito)
- [ ] Si no existe: agregar `MVCBORGEN` (tipo origen) y `MVCBIDORG` (id del registro origen)
- [ ] Confirmar que `MVCBCNCL` (conciliado) y `MVCBFCCN` (fecha conciliación) ya existen ✅

#### 4.2 Revisar y completar flujo existente

- [ ] Revisar estado actual de `Conciliacion` y `DetalleConciliacion`
- [ ] Revisar `HistConciliacion` y `HistDetalleConciliacion`
- [ ] Completar servicio de carga de extracto bancario
- [ ] Completar servicio de matching automático (movimiento sistema ↔ línea extracto)
- [ ] Completar servicio de matching manual
- [ ] Endpoint REST de conciliación

---

### 🟡 PRIORIDAD 5 — EMISIÓN DE LIQUIDACIÓN EN COMPRAS Y RETENCIONES EN CXC

**Objetivo:** Completar emisión de Liquidaciones de Compra y Retenciones en CXC con documento electrónico y contabilidad.  
**Estado:** 🟡 En análisis

#### 5.1 Liquidación de Compra (CXC)

- [ ] Revisar estado actual del modelo `LiquidacionCompra` y `DetalleLiquidacionCompra`
- [ ] Flujo de emisión y firma electrónica (XML + envío SRI)
- [ ] Generación de contabilidad

#### 5.2 Retenciones en CXC

- [ ] Definir si se usa `Retencion` o `RetencionV2` — deprecar la que no se use
- [ ] Verificar relación `Retencion` ↔ `Factura` (si no existe, crear el vínculo)
- [ ] Flujo de emisión electrónica al SRI
- [ ] Generación de contabilidad

---

### 🟡 PRIORIDAD 6 — CAJA CHICA EN TSR

**Objetivo:** Manejar las 2 cajas chicas del fondo como cuentas bancarias de un banco ficticio.  
**Estado:** 🟡 En análisis

#### 6.1 Configuración inicial en BD

- [ ] Crear registro en `TSR.Banco`: nombre = "CAJA CHICA"
- [ ] Crear 2 registros en `TSR.CuentaBancaria` (Caja Chica 1 y Caja Chica 2)
- [ ] Asignar la cuenta contable correcta a cada una

#### 6.2 Operación diaria (reutiliza lo existente)

- [ ] Gastos: `MovimientoBanco` tipo SALIDA en la cuenta de caja chica correspondiente
- [ ] Reposición: `Transferencia` desde cuenta bancaria real → cuenta caja chica

#### 6.3 Arqueo de caja chica (tabla nueva liviana)

- [ ] Crear **`TSR.ARCJ` → `ArqueoCajaChica`**
  - `cuentaBancaria` (FK → CuentaBancaria), `fecha`
  - `saldoLibros` (calculado del sistema), `saldoFisico` (ingresado por el responsable)
  - `diferencia` (saldoFisico - saldoLibros), `observacion`, `usuario`, `estado` (BORRADOR / CERRADO)
- [ ] Modelo Java `ArqueoCajaChica.java`
- [ ] Servicio de cierre / arqueo
- [ ] Endpoint REST

---

### 🟡 PRIORIDAD 7 — RELACIÓN FACTURAS CXP ↔ NEGOCIACIÓN CON PROVEEDOR

**Objetivo:** Vincular facturas de compra con el contrato / negociación del proveedor correspondiente.  
**Estado:** 🟡 Pendiente

#### 7.1 Nueva tabla a crear

- [ ] **`PGS.FCTNG` → `FacturaXNegociacion`**
  - `facturaCompra` (FK → FacturaCompra)
  - `negociacion` (FK → NegociacionProveedor)
  - `valorImputado`, `fecha`, `observacion`
  - **Regla:** suma de `valorImputado` ≤ `valorTotal` de la negociación

#### 7.2 Servicios a implementar

- [ ] Asociar factura a negociación con validación de montos
- [ ] Reporte de consumo por negociación (valor contratado vs. facturado)
- [ ] Endpoint REST

---

### 🟡 PRIORIDAD 8 — CONTABILIDAD DEL MÓDULO CRD (CRÉDITOS)

**Objetivo:** Generar asientos contables para aportaciones y créditos de los afiliados.  
**Estado:** 🟡 Pendiente — se detallará en la siguiente iteración de planificación

---

## REGISTRO DE AVANCE

| Fecha | Tarea completada | Responsable | Notas |
|---|---|---|---|
| 2026-07-25 | Documento de plan creado | Equipo | Análisis arquitectural completado |
| 2026-07-25 | Prioridad 1.1 — Campo ASIENTO en tablas cabecera CXP | Equipo | 4 modelos Java + script SQL `01_ASIENTO_TABLAS_CXP.sql` |
| 2026-07-25 | Prioridad 1.2 — Flujo de carga TXT/XML ya completo | Equipo | `ProcesoCargaDocumentosServiceImpl` + `ProcesoCargaDocumentosRest` — 10 endpoints operativos |
| 2026-07-25 | Prioridad 1.3 — Generación de contabilidad CXP | Equipo | 5 métodos implementados en `AsientoContableServiceImpl`, `grabarAsientoEnDocumento` en impl, `NotaDebitoCompra` y `RetencionCompraV2` actualizados. Script SQL ampliado a 6 tablas. Pendiente: configurar TipoAsiento en BD (codigoAlterno 9-14). |

---

## NOTAS TÉCNICAS GENERALES

- El patrón de campo `ASNTCDGO` directo en la tabla cabecera (usado exitosamente en CXC) se replica en CXP.
- Toda nueva tabla sigue el estándar de códigos de **4 letras mayúsculas** del sistema (ej: ORDP, DRDP, ANTC).
- Las tablas marcadas para deprecar se dejan en BD hasta validar que ningún proceso activo las consume.
- La caja chica se maneja 100% con la infraestructura de `Banco` + `CuentaBancaria` + `MovimientoBanco` existente, sin crear tablas nuevas salvo `ArqueoCajaChica`.
- Al limpiar `Pago` (PGSS) y `Cobro` (CBRO) de referencias a caja, verificar primero que no haya datos históricos usando esos campos.
