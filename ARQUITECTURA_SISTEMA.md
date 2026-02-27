# Arquitectura y Alcance del Sistema SaaBE
## Sistema de Administración y Automatización - Backend Enterprise

**Versión:** 1.0  
**Fecha de Creación:** 25 de Febrero de 2026  
**Tecnología Base:** Jakarta EE 10, WildFly  
**Lenguaje:** Java 21

---

## 📋 TABLA DE CONTENIDOS

1. [Descripción General](#descripción-general)
2. [Arquitectura del Sistema](#arquitectura-del-sistema)
3. [Módulos del Sistema](#módulos-del-sistema)
4. [Stack Tecnológico](#stack-tecnológico)
5. [Estructura de Capas](#estructura-de-capas)
6. [Convenciones y Estándares](#convenciones-y-estándares)

---

## 🎯 DESCRIPCIÓN GENERAL

SaaBE es un sistema empresarial integral desarrollado con Jakarta EE que proporciona soluciones para la gestión financiera, contable, recursos humanos y administración de créditos. El sistema está diseñado bajo una arquitectura multicapa que garantiza escalabilidad, mantenibilidad y rendimiento.

### Objetivos del Sistema

- **Automatización** de procesos financieros y administrativos
- **Integración** de múltiples módulos de negocio
- **Generación** de reportes contables y financieros
- **Gestión** de documentos, pagos, cobros y nómina
- **Control** de créditos, aportes y préstamos
- **Trazabilidad** completa de operaciones

---

## 🏗️ ARQUITECTURA DEL SISTEMA

### Arquitectura de Capas

```
┌─────────────────────────────────────────────────────────┐
│                    CAPA PRESENTACIÓN                    │
│              (REST API - JAX-RS Endpoints)              │
│                  /ws/rest/{module}                      │
└─────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────┐
│                   CAPA DE SERVICIOS                     │
│              (EJB Services - Lógica de Negocio)         │
│                  /ejb/{module}/service                  │
└─────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────┐
│               CAPA DE ACCESO A DATOS (DAO)              │
│              (EJB DAO - Persistencia)                   │
│                  /ejb/{module}/dao                      │
└─────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────┐
│                    CAPA DE MODELO                       │
│              (JPA Entities - Modelo de Datos)           │
│                  /model/{module}                        │
└─────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────┐
│                    BASE DE DATOS                        │
│                  (Oracle Database)                      │
│           Schemas: CRD, CNT, CXC, CXP, TSR, RHH        │
└─────────────────────────────────────────────────────────┘
```

### Principios de Diseño

- **Separación de Responsabilidades:** Cada capa tiene un propósito específico
- **Inyección de Dependencias:** Uso de `@EJB` para desacoplamiento
- **Transaccionalidad:** Gestión automática de transacciones con EJB
- **RESTful API:** Endpoints REST siguiendo convenciones estándar
- **Persistencia JPA:** ORM con JPA para mapeo objeto-relacional

---

## 📦 MÓDULOS DEL SISTEMA

El sistema SaaBE está dividido en **8 módulos principales**, cada uno con su propio dominio de negocio:

### 1. 💳 **CRD - Módulo de Créditos**

**Esquema de Base de Datos:** `CRD`  
**Paquete Base:** `com.saa.*.crd`  
**Path REST:** `/crd/*`

#### Descripción
Gestiona todo el ciclo de vida de créditos, préstamos, aportes y participes. Incluye la administración de cesantías, jubilaciones y productos crediticios.

#### Entidades Principales (67 entidades)
- **Participes y Personas**
  - `Participe` - Información de los participantes del sistema
  - `PersonaNatural` - Datos personales de personas naturales
  - `BioProfile` - Perfil biométrico
  - `PerfilEconomico` - Información económica del partícipe
  - `HistorialSueldo` - Historial de sueldos
  - `BaseInicialParticipes` - Base inicial de aportes de partícipes

- **Préstamos y Créditos**
  - `Prestamo` - Información de préstamos
  - `DetallePrestamo` - Desglose de préstamos
  - `PagoPrestamo` - Registro de pagos
  - `MoraPrestamo` - Control de moras
  - `TasaPrestamo` - Tasas de interés
  - `DatosPrestamo` - Datos adicionales de préstamos
  - `DocumentoCredito` - Documentación del crédito
  - `RequisitosPrestamo` - Requisitos para aprobación
  - `RelacionPrestamo` - Relaciones entre préstamos

- **Aportes y Cesantías**
  - `Aporte` - Registro de aportes
  - `PagoAporte` - Pagos de aportes
  - `CambioAporte` - Modificaciones de aportes
  - `Cesantia` - Información de cesantías
  - `TipoCesantia` - Tipos de cesantía

- **Cuentas por Cobrar de Créditos**
  - `CxcParticipe` - Cuentas por cobrar de partícipes
  - `CxcKardexParticipe` - Kardex de movimientos

- **Catálogos y Configuración**
  - `TipoPrestamo` - Tipos de préstamos
  - `Producto` - Productos crediticios
  - `Entidad` - Entidades financieras
  - `Filial` - Filiales
  - `EstadoPrestamo` - Estados de préstamos
  - `EstadoParticipe` - Estados de partícipes
  - `EstadoCesantia` - Estados de cesantías
  - `MotivoPrestamo` - Motivos de préstamos
  - `CreditoMontoAprobacion` - Montos aprobados

- **Geográficos**
  - `Pais` - Países
  - `Provincia` - Provincias
  - `Canton` - Cantones
  - `Ciudad` - Ciudades
  - `Parroquia` - Parroquias
  - `Direccion` - Direcciones
  - `DireccionTrabajo` - Direcciones de trabajo

- **Generales**
  - `EstadoCivil` - Estados civiles
  - `NivelEstudio` - Niveles de estudio
  - `Profesion` - Profesiones
  - `TipoGenero` - Tipos de género
  - `TipoIdentificacion` - Tipos de identificación
  - `TipoVivienda` - Tipos de vivienda
  - `TipoContrato` - Tipos de contrato
  - `Contrato` - Contratos
  - `MetodoPago` - Métodos de pago
  - `TipoPago` - Tipos de pago

- **Integración ASOPREP**
  - `ParticipeAsoprep` - Partícipes de ASOPREP
  - `AporteAsoprep` - Aportes ASOPREP
  - `TransaccionesAsoprep` - Transacciones ASOPREP

- **Carga Masiva**
  - `CargaArchivo` - Carga de archivos
  - `DetalleCargaArchivo` - Detalle de archivos cargados
  - `ParticipeXCargaArchivo` - Partícipes por carga

- **Auditoría**
  - `Auditoria` - Registro de auditoría
  - `Comentario` - Comentarios del sistema
  - `Adjunto` - Adjuntos de documentos
  - `TipoAdjunto` - Tipos de adjuntos

#### Funcionalidades Clave
- ✅ Gestión completa de partícipes
- ✅ Originación y seguimiento de préstamos
- ✅ Cálculo de moras y pagos
- ✅ Control de aportes y cesantías
- ✅ Gestión de garantías y requisitos
- ✅ Integración con ASOPREP
- ✅ Carga masiva de datos
- ✅ Auditoría de operaciones

---

### 2. 📊 **CNT - Módulo de Contabilidad**

**Esquema de Base de Datos:** `CNT`  
**Paquete Base:** `com.saa.*.cnt`  
**Path REST:** `/cnt/*`

#### Descripción
Administra el plan contable, asientos contables, mayorizaciones, periodos fiscales y generación de reportes contables. Es el núcleo financiero del sistema.

#### Entidades Principales (29 entidades)
- **Plan Contable**
  - `PlanCuenta` - Plan de cuentas contables
  - `NaturalezaCuenta` - Naturaleza de las cuentas (deudor/acreedor)
  - `CentroCosto` - Centros de costo
  - `MatchCuenta` - Coincidencias de cuentas

- **Asientos Contables**
  - `Asiento` - Asientos contables
  - `DetalleAsiento` - Detalle de asientos (debe/haber)
  - `TipoAsiento` - Tipos de asientos
  - `Plantilla` - Plantillas de asientos
  - `DetallePlantilla` - Detalle de plantillas
  - `HistAsiento` - Historial de asientos
  - `HistDetalleAsiento` - Historial de detalles

- **Mayorizaciones**
  - `Mayorizacion` - Proceso de mayorización
  - `DetalleMayorizacion` - Detalle de mayorización
  - `MayorizacionCC` - Mayorización por centro de costo
  - `DetalleMayorizacionCC` - Detalle por centro de costo
  - `DesgloseMayorizacionCC` - Desglose por centro
  - `HistMayorizacion` - Historial de mayorizaciones
  - `HistDetalleMayorizacion` - Historial de detalles
  - `MayorAnalitico` - Mayor analítico
  - `DetalleMayorAnalitico` - Detalle mayor analítico

- **Saldos y Reportes**
  - `Saldos` - Saldos contables
  - `ReporteContable` - Configuración de reportes
  - `DetalleReporteContable` - Detalle de reportes
  - `ReporteCuentaCC` - Reportes por centro de costo
  - `DetalleReporteCuentaCC` - Detalle por centro
  - `TempReportes` - Tabla temporal para reportes

- **Configuración Temporal**
  - `Periodo` - Periodos contables
  - `AnioMotor` - Año del motor contable

#### Funcionalidades Clave
- ✅ Gestión del plan de cuentas
- ✅ Registro y reversión de asientos contables
- ✅ Mayorización automática
- ✅ Control de centros de costo
- ✅ Cierre de periodos contables
- ✅ Generación de reportes (Balance General, Estado de Resultados, etc.)
- ✅ Auditoría de cambios contables
- ✅ Plantillas de asientos recurrentes

---

### 3. 💰 **CXC - Módulo de Cuentas por Cobrar**

**Esquema de Base de Datos:** `CXC`  
**Paquete Base:** `com.saa.*.cxc`  
**Path REST:** `/cxc/*`

#### Descripción
Gestiona la facturación, documentos por cobrar, financiamientos, productos y control de impuestos para ventas y servicios.

#### Entidades Principales (22 entidades)
- **Documentos por Cobrar**
  - `DocumentoCobro` - Documentos por cobrar
  - `DetalleDocumentoCobro` - Detalle de documentos
  - `ResumenValorDocumentoCobro` - Resumen de valores
  - `ComposicionCuotaInicialCobro` - Composición de cuota inicial

- **Financiamientos**
  - `FinanciacionXDocumentoCobro` - Financiaciones por documento
  - `CuotaXFinanciacionCobro` - Cuotas de financiamiento
  - `PagosArbitrariosXFinanciacionCobro` - Pagos extraordinarios

- **Productos e Impuestos**
  - `ProductoCobro` - Productos para cobro
  - `GrupoProductoCobro` - Grupos de productos
  - `ImpuestoXGrupoCobro` - Impuestos por grupo
  - `ValorImpuestoDocumentoCobro` - Valor de impuestos del documento
  - `ValorImpuestoDetalleCobro` - Valor de impuestos del detalle

- **Tablas Temporales**
  - `TempDocumentoCobro` - Temporal de documentos
  - `TempDetalleDocumentoCobro` - Temporal de detalles
  - `TempFinanciacionXDocumentoCobro` - Temporal de financiaciones
  - `TempCuotaXFinanciacionCobro` - Temporal de cuotas
  - `TempComposicionCuotaInicialCobro` - Temporal de composición
  - `TempPagosArbitrariosXFinanciacionCobro` - Temporal de pagos
  - `TempResumenValorDocumentoCobro` - Temporal de resumen
  - `TempValorImpuestoDocumentoCobro` - Temporal de impuestos documento
  - `TempValorImpuestoDetalleCobro` - Temporal de impuestos detalle

#### Funcionalidades Clave
- ✅ Emisión de documentos por cobrar
- ✅ Gestión de financiamientos y cuotas
- ✅ Cálculo automático de impuestos
- ✅ Control de pagos parciales y totales
- ✅ Productos y grupos de productos
- ✅ Generación de estados de cuenta
- ✅ Integración con contabilidad

---

### 4. 💸 **CXP - Módulo de Cuentas por Pagar**

**Esquema de Base de Datos:** `CXP`  
**Paquete Base:** `com.saa.*.cxp`  
**Path REST:** `/cxp/*`

#### Descripción
Administra las obligaciones con proveedores, documentos por pagar, flujo de aprobaciones, financiamientos y control de impuestos en compras.

#### Entidades Principales (30 entidades)
- **Documentos por Pagar**
  - `DocumentoPago` - Documentos por pagar
  - `DetalleDocumentoPago` - Detalle de documentos
  - `ResumenValorDocumentoPago` - Resumen de valores
  - `ComposicionCuotaInicialPago` - Composición de cuota inicial

- **Financiamientos**
  - `FinanciacionXDocumentoPago` - Financiaciones por documento
  - `CuotaXFinanciacionPago` - Cuotas de financiamiento
  - `PagosArbitrariosXFinanciacionPago` - Pagos extraordinarios

- **Productos e Impuestos**
  - `ProductoPago` - Productos para pago
  - `GrupoProductoPago` - Grupos de productos
  - `ImpuestoXGrupoPago` - Impuestos por grupo
  - `ValorImpuestoDocumentoPago` - Valor de impuestos del documento
  - `ValorImpuestoDetallePago` - Valor de impuestos del detalle

- **Aprobaciones**
  - `MontoAprobacion` - Montos de aprobación
  - `AprobacionXMonto` - Aprobaciones por monto
  - `UsuarioXAprobacion` - Usuarios aprobadores
  - `ProposicionPagoXCuota` - Proposiciones de pago

- **Tablas Temporales**
  - `TempDocumentoPago` - Temporal de documentos
  - `TempDetalleDocumentoPago` - Temporal de detalles
  - `TempFinanciacionXDocumentoPago` - Temporal de financiaciones
  - `TempCuotaXFinanciacionPago` - Temporal de cuotas
  - `TempComposicionCuotaInicialPago` - Temporal de composición
  - `TempPagosArbitrariosXFinanciacionPago` - Temporal de pagos
  - `TempResumenValorDocumentoPago` - Temporal de resumen
  - `TempValorImpuestoDocumentoPago` - Temporal de impuestos documento
  - `TempValorImpuestoDetallePago` - Temporal de impuestos detalle
  - `TempMontoAprobacion` - Temporal de montos
  - `TempAprobacionXMonto` - Temporal de aprobaciones
  - `TempUsuarioXAprobacion` - Temporal de usuarios

#### Funcionalidades Clave
- ✅ Registro de documentos por pagar
- ✅ Flujo de aprobaciones multinivel
- ✅ Gestión de financiamientos y cuotas
- ✅ Cálculo automático de retenciones e impuestos
- ✅ Control de pagos parciales y totales
- ✅ Proposiciones de pago
- ✅ Productos y grupos de productos
- ✅ Integración con tesorería y contabilidad

---

### 5. 🏦 **TSR - Módulo de Tesorería**

**Esquema de Base de Datos:** `TSR`  
**Paquete Base:** `com.saa.*.tsr`  
**Path REST:** `/tsr/*`

#### Descripción
Gestiona el flujo de caja, cuentas bancarias, cobros, pagos, conciliaciones bancarias, cheques y movimientos de tesorería.

#### Entidades Principales (57 entidades)
- **Bancos y Cuentas**
  - `Banco` - Bancos del sistema
  - `BancoExterno` - Bancos externos
  - `CuentaBancaria` - Cuentas bancarias
  - `SaldoBanco` - Saldos bancarios
  - `MovimientoBanco` - Movimientos bancarios
  - `Titular` - Titulares de cuentas

- **Cheques**
  - `Chequera` - Chequeras
  - `Cheque` - Cheques emitidos

- **Cajas**
  - `CajaFisica` - Cajas físicas
  - `CajaLogica` - Cajas lógicas
  - `CajaLogicaPorCajaFisica` - Relación cajas
  - `GrupoCaja` - Grupos de cajas
  - `UsuarioPorCaja` - Usuarios por caja
  - `CierreCaja` - Cierres de caja
  - `DetalleCierre` - Detalle de cierres

- **Cobros**
  - `Cobro` - Registro de cobros
  - `CobroEfectivo` - Cobros en efectivo
  - `CobroCheque` - Cobros con cheque
  - `CobroTarjeta` - Cobros con tarjeta
  - `CobroTransferencia` - Cobros por transferencia
  - `CobroRetencion` - Retenciones en cobros
  - `MotivoCobro` - Motivos de cobro

- **Pagos**
  - `Pago` - Registro de pagos
  - `MotivoPago` - Motivos de pago

- **Depósitos**
  - `Deposito` - Depósitos bancarios
  - `DetalleDeposito` - Detalle de depósitos
  - `DesgloseDetalleDeposito` - Desglose de depósitos
  - `AuxDepositoBanco` - Auxiliar de depósitos
  - `AuxDepositoCierre` - Auxiliar de cierres
  - `AuxDepositoDesglose` - Auxiliar de desgloses

- **Débitos y Créditos**
  - `DebitoCredito` - Notas de débito/crédito
  - `DetalleDebitoCredito` - Detalle de notas

- **Conciliaciones**
  - `Conciliacion` - Conciliaciones bancarias
  - `DetalleConciliacion` - Detalle de conciliaciones
  - `HistConciliacion` - Historial de conciliaciones
  - `HistDetalleConciliacion` - Historial de detalles

- **Transferencias**
  - `Transferencia` - Transferencias bancarias

- **Personas y Direcciones**
  - `PersonaRol` - Roles de personas
  - `PersonaCuentaContable` - Cuentas contables de personas
  - `DireccionPersona` - Direcciones de personas
  - `TelefonoDireccion` - Teléfonos de direcciones

- **Tablas Temporales**
  - `TempCobro` - Temporal de cobros
  - `TempCobroEfectivo` - Temporal de efectivo
  - `TempCobroCheque` - Temporal de cheques
  - `TempCobroTarjeta` - Temporal de tarjetas
  - `TempCobroTransferencia` - Temporal de transferencias
  - `TempCobroRetencion` - Temporal de retenciones
  - `TempPago` - Temporal de pagos
  - `TempMotivoCobro` - Temporal de motivos cobro
  - `TempMotivoPago` - Temporal de motivos pago
  - `TempDebitoCredito` - Temporal de débitos/créditos

#### Funcionalidades Clave
- ✅ Gestión de cuentas bancarias
- ✅ Control de cajas físicas y lógicas
- ✅ Registro de cobros (efectivo, cheque, tarjeta, transferencia)
- ✅ Registro de pagos y cheques
- ✅ Depósitos bancarios
- ✅ Conciliaciones bancarias automáticas
- ✅ Control de saldos en tiempo real
- ✅ Cierre de cajas
- ✅ Notas de débito y crédito
- ✅ Integración con CXC, CXP y Contabilidad

---

### 6. 👥 **RHH - Módulo de Recursos Humanos**

**Esquema de Base de Datos:** `RHH`  
**Paquete Base:** `com.saa.*.rhh`  
**Path REST:** `/rhh/*`

#### Descripción
Administra la información de empleados, contratos, nómina, liquidaciones, vacaciones, marcaciones y estructura organizacional.

#### Entidades Principales (25 entidades)
- **Empleados**
  - `Empleado` - Información de empleados
  - `ContratoEmpleado` - Contratos de trabajo
  - `TipoContratoEmpleado` - Tipos de contrato
  - `AnexoContrato` - Anexos de contratos
  - `Historial` - Historial laboral

- **Estructura Organizacional**
  - `Departamento` - Departamentos
  - `Cargo` - Cargos
  - `DepartamentoCargo` - Relación departamento-cargo

- **Nómina**
  - `Nomina` - Nóminas procesadas
  - `PeriodoNomina` - Periodos de nómina
  - `ReglonNomina` - Renglones de nómina
  - `ResumenNomina` - Resumen de nóminas
  - `RolPago` - Roles de pago

- **Liquidaciones**
  - `Liquidacion` - Liquidaciones
  - `DetalleLiquidacion` - Detalle de liquidaciones

- **Vacaciones**
  - `SolicitudVacaciones` - Solicitudes de vacaciones
  - `SaldoVacaciones` - Saldo de vacaciones

- **Control de Asistencia**
  - `Marcaciones` - Marcaciones de entrada/salida
  - `Turno` - Turnos de trabajo
  - `DetalleTurno` - Detalle de turnos

- **Aportes y Retenciones**
  - `AportesRetenciones` - Aportes y retenciones

- **Peticiones**
  - `Peticiones` - Peticiones de empleados

- **Catálogos**
  - `Catalogo` - Catálogos generales

#### Funcionalidades Clave
- ✅ Gestión de información de empleados
- ✅ Control de contratos y anexos
- ✅ Procesamiento de nómina
- ✅ Cálculo de liquidaciones
- ✅ Gestión de vacaciones
- ✅ Control de marcaciones y turnos
- ✅ Estructura organizacional
- ✅ Aportes y retenciones
- ✅ Roles de pago
- ✅ Integración con contabilidad

---

### 7. 📄 **REPORTE - Módulo de Reportes**

**Esquema de Base de Datos:** N/A (Transversal)  
**Paquete Base:** `com.saa.*.reporte`  
**Path REST:** `/reporte/*`

#### Descripción
Módulo transversal que genera reportes en múltiples formatos (PDF, Excel, Word) utilizando JasperReports. Accede a datos de todos los módulos.

#### Entidades Principales (2 entidades)
- `ReporteRequest` - Solicitud de generación de reporte
- `ReporteResponse` - Respuesta con el reporte generado

#### Tecnología
- **JasperReports 7.0.3** - Motor de reportes
- **Apache POI 5.2.3** - Exportación a Excel
- **Eclipse JDT Compiler** - Compilación de plantillas

#### Reportes Disponibles
- 📊 Balance General
- 📈 Estado de Resultados
- 📋 Mayor Analítico
- 🧾 Libro Diario
- 💰 Estado de Flujo de Efectivo
- 📊 Reportes de CXC y CXP
- 💳 Reportes de Préstamos
- 👥 Reportes de Nómina
- 🏦 Reportes de Tesorería

#### Funcionalidades Clave
- ✅ Generación dinámica de reportes
- ✅ Múltiples formatos de salida (PDF, XLS, XLSX, DOCX)
- ✅ Plantillas personalizables (.jrxml)
- ✅ Reportes parametrizados
- ✅ Exportación masiva
- ✅ Integración con todos los módulos

---

### 8. 🏢 **SCP - Módulo del Sistema (Core)**

**Esquema de Base de Datos:** `SCP` / `BASICO`  
**Paquete Base:** `com.saa.*.scp` y `com.saa.rubros`  
**Path REST:** `/basico/*`

#### Descripción
Módulo central que gestiona la configuración base del sistema, empresas, usuarios, jerarquías organizacionales y rubros contables.

#### Entidades Principales (6 entidades)
- **Empresa**
  - `Empresa` - Información de la empresa

- **Usuarios y Jerarquía**
  - `Usuario` - Usuarios del sistema
  - `Jerarquia` - Estructura jerárquica organizacional

- **Rubros Contables**
  - `Rubro` - Rubros contables del sistema
  - `DetalleRubro` - Detalle de rubros

#### Catálogos del Sistema (120+ constantes)
El módulo `rubros` contiene todas las constantes y enumeraciones del sistema:

**Estados Generales**
- `Estado` - Estados generales (Activo, Inactivo)
- `EstadoAsiento` - Estados de asientos contables
- `EstadoCheque` - Estados de cheques
- `EstadoCobro` - Estados de cobros
- `EstadoPago` - Estados de pagos
- `EstadoDocumentoPago` - Estados de documentos por pagar
- `EstadoConciliacion` - Estados de conciliaciones
- `EstadoPeriodos` - Estados de periodos contables

**Tipos y Clasificaciones**
- `TipoAsientos` - Tipos de asientos
- `TipoBancos` - Tipos de bancos
- `TipoCuentasBancarias` - Tipos de cuentas bancarias
- `TipoFormaPago` - Formas de pago
- `TipoImpuesto` - Tipos de impuestos
- `TipoMoneda` - Tipos de moneda
- `TipoPersona` - Tipos de persona
- `TipoIdentificacion` - Tipos de identificación
- `TipoTransaccion` - Tipos de transacción
- `TipoDireccion` - Tipos de dirección
- `TipoTelefono` - Tipos de teléfono

**Configuraciones del Sistema**
- `ModuloSistema` - Módulos del sistema
- `NivelesPermisos` - Niveles de permisos
- `RolPersona` - Roles de personas
- `FormatoFecha` - Formatos de fecha
- `FormatoNumero` - Formatos de número
- `Periodicidad` - Periodicidades

**Configuraciones Específicas ASOPREP**
- `ASPEstadoCargaArchivoPetro` - Estados de carga de archivos Petro
- `ASPEstadoCuotasPrestamoAportes` - Estados de cuotas
- `ASPEstadoRevisionParticipeCarga` - Estados de revisión
- `ASPNovedadesCargaArchivo` - Novedades de carga
- `ASPSensibilidadBusquedaCoincidencias` - Sensibilidad de búsqueda
- `ASPTimeOutSessionUsuario` - Timeout de sesiones

**Reportes**
- `ReportesBasicos` - Reportes básicos del sistema
- `ReportesBalances` - Reportes de balances
- `ReporteTipoAcumulacion` - Tipos de acumulación
- `ReporteTipoDistribucion` - Tipos de distribución
- `SignosReportesContables` - Signos para reportes

#### Funcionalidades Clave
- ✅ Configuración de empresa
- ✅ Gestión de usuarios y permisos
- ✅ Estructura organizacional jerárquica
- ✅ Catálogos centralizados
- ✅ Constantes del sistema
- ✅ Rubros contables
- ✅ Configuración de módulos

---

## 🛠️ STACK TECNOLÓGICO

### Backend
- **Java:** 21 (LTS)
- **Jakarta EE:** 10.0.0
- **Application Server:** WildFly
- **ORM:** JPA (Jakarta Persistence)
- **Servicios:** EJB 4.0 (Stateless/Stateful)
- **API REST:** JAX-RS (Jakarta RESTful Web Services)
- **Dependency Injection:** CDI 4.0

### Base de Datos
- **DBMS:** Oracle Database
- **Driver:** Oracle JDBC

### Reportes
- **Motor:** JasperReports 7.0.3
- **Compilador:** Eclipse JDT (ECJ) 3.33.0
- **Exportación Excel:** Apache POI 5.2.3
- **Formatos:** PDF, XLS, XLSX, DOCX

### Build & Deployment
- **Build Tool:** Apache Maven 3.x
- **Plugins:**
  - maven-compiler-plugin 3.11.0
  - maven-war-plugin 3.3.2
  - maven-resources-plugin 3.3.0
  - maven-enforcer-plugin 3.2.1

### Multipart
- **Provider:** RESTEasy Multipart Provider 6.2.9.Final

---

## 📐 ESTRUCTURA DE CAPAS

### 1. Capa de Modelo (Model Layer)
**Ubicación:** `com.saa.model.{module}`

Entidades JPA que representan las tablas de la base de datos.

**Características:**
- Anotaciones JPA (`@Entity`, `@Table`, `@Column`)
- Named Queries para consultas comunes
- Relaciones entre entidades (`@ManyToOne`, `@OneToMany`)
- Implementan `Serializable`

**Ejemplo:**
```java
@Entity
@Table(name = "BIPR", schema = "CRD")
@NamedQueries({
    @NamedQuery(name = "BaseInicialParticipesAll", 
                query = "select e from BaseInicialParticipes e")
})
public class BaseInicialParticipes implements Serializable {
    @Id
    @Column(name = "BIPRNMRO")
    private Long numero;
    // ...
}
```

### 2. Capa DAO (Data Access Object)
**Ubicación:** `com.saa.ejb.{module}.dao` y `com.saa.ejb.{module}.daoImpl`

Acceso a datos mediante EJBs que extienden `EntityDao`.

**Características:**
- Interfaces locales (`@Local`)
- Implementaciones Stateless (`@Stateless`)
- Operaciones CRUD genéricas
- Consultas personalizadas

**Ejemplo:**
```java
@Local
public interface BaseInicialParticipesDaoService 
    extends EntityDao<BaseInicialParticipes> {
}

@Stateless
public class BaseInicialParticipesDaoServiceImpl 
    extends EntityDaoImpl<BaseInicialParticipes> 
    implements BaseInicialParticipesDaoService {
}
```

### 3. Capa de Servicio (Service Layer)
**Ubicación:** `com.saa.ejb.{module}.service` y `com.saa.ejb.{module}.serviceImpl`

Lógica de negocio mediante EJBs con gestión transaccional.

**Características:**
- Interfaces locales (`@Local`)
- Implementaciones Stateless (`@Stateless`)
- Inyección de DAOs (`@EJB`)
- Validaciones de negocio
- Gestión de transacciones

**Ejemplo:**
```java
@Stateless
public class BaseInicialParticipesServiceImpl 
    implements BaseInicialParticipesService {
    
    @EJB
    private BaseInicialParticipesDaoService dao;
    
    @Override
    public BaseInicialParticipes saveSingle(BaseInicialParticipes entity) 
        throws Throwable {
        // Lógica de negocio
        return dao.save(entity, entity.getNumero());
    }
}
```

### 4. Capa REST (Presentation Layer)
**Ubicación:** `com.saa.ws.rest.{module}`

Endpoints REST para comunicación con clientes.

**Características:**
- Anotación `@Path` para rutas
- Métodos HTTP (`@GET`, `@POST`, `@PUT`, `@DELETE`)
- Producción/Consumo JSON (`@Produces`, `@Consumes`)
- Inyección de servicios (`@EJB`)
- Manejo de respuestas HTTP

**Ejemplo:**
```java
@Path("bipr")
public class BaseInicialParticipesRest {
    @EJB
    private BaseInicialParticipesService service;
    
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        // Retorna lista de entidades
    }
}
```

---

## 📋 CONVENCIONES Y ESTÁNDARES

### Nomenclatura de Archivos

#### Entidades (Model)
- **Patrón:** `{NombreEntidad}.java`
- **Ejemplos:** `Asiento.java`, `Participe.java`, `DocumentoCobro.java`

#### DAOs
- **Interface:** `{NombreEntidad}DaoService.java`
- **Implementación:** `{NombreEntidad}DaoServiceImpl.java`
- **Ejemplos:** `AsientoDaoService.java`, `AsientoDaoServiceImpl.java`

#### Services
- **Interface:** `{NombreEntidad}Service.java`
- **Implementación:** `{NombreEntidad}ServiceImpl.java`
- **Ejemplos:** `AsientoService.java`, `AsientoServiceImpl.java`

#### REST Controllers
- **Patrón:** `{NombreEntidad}Rest.java`
- **Ejemplos:** `AsientoRest.java`, `ParticipeRest.java`

### Nomenclatura de Base de Datos

#### Tablas
- **Formato:** 4 letras mayúsculas (código nemotécnico)
- **Ejemplos:** 
  - `ASNT` → Asiento
  - `BIPR` → Base Inicial Partícipes
  - `PRDC` → Producto

#### Columnas
- **Formato:** 8 caracteres (4 letras tabla + 4 letras campo)
- **Ejemplos:**
  - `ASNTCDGO` → Asiento.Código
  - `BIPRNMRO` → BaseInicialParticipes.Número
  - `PRDCNMBR` → Producto.Nombre

#### Schemas
- `CRD` - Créditos
- `CNT` - Contabilidad
- `CXC` - Cuentas por Cobrar
- `CXP` - Cuentas por Pagar
- `TSR` - Tesorería
- `RHH` - Recursos Humanos
- `SCP` - Sistema Core

### Rutas REST

#### Patrón Base
```
/api/{codigo-tabla}/{operacion}
```

#### Operaciones Estándar
- `GET /api/{tabla}/getAll` - Obtener todos
- `GET /api/{tabla}/getId/{id}` - Obtener por ID
- `POST /api/{tabla}` - Crear nuevo
- `PUT /api/{tabla}` - Actualizar
- `DELETE /api/{tabla}/{id}` - Eliminar
- `POST /api/{tabla}/selectByCriteria` - Búsqueda avanzada

#### Ejemplos
```
GET    /api/asnt/getAll
GET    /api/asnt/getId/123
POST   /api/asnt
PUT    /api/asnt
DELETE /api/asnt/123
POST   /api/asnt/selectByCriteria
```

### Named Queries

#### Patrón
- **Todos:** `{NombreEntidad}All`
- **Por ID:** `{NombreEntidad}Id`

#### Ejemplos
```java
@NamedQueries({
    @NamedQuery(name = "AsientoAll", 
                query = "select e from Asiento e"),
    @NamedQuery(name = "AsientoId", 
                query = "select e from Asiento e where e.codigo = :id")
})
```

### Constantes de Entidades

Definidas en interfaces `NombreEntidades{Modulo}`:
- `NombreEntidadesCredito`
- `NombreEntidadesContabilidad`
- `NombreEntidadesCobro`
- `NombreEntidadesPago`
- `NombreEntidadesTesoreria`
- `NombreEntidadesRhh`
- `NombreEntidadesSistema`

**Ejemplo:**
```java
public interface NombreEntidadesCredito {
    String PARTICIPE = "Participe";
    String PRESTAMO = "Prestamo";
    String BASE_INICIAL_PARTICIPES = "BaseInicialParticipes";
}
```

---

## 🔄 FLUJO DE OPERACIONES

### Flujo Típico de una Operación CRUD

```
Cliente (Frontend)
      ↓ HTTP Request (JSON)
REST Controller (@Path)
      ↓ @EJB injection
Service Layer (Business Logic)
      ↓ @EJB injection
DAO Layer (Data Access)
      ↓ JPA/EntityManager
Base de Datos (Oracle)
      ↓
DAO Layer (Entities)
      ↓
Service Layer (Processing)
      ↓
REST Controller (Response)
      ↓ HTTP Response (JSON)
Cliente (Frontend)
```

### Gestión de Transacciones

- **Transacciones automáticas** mediante EJB
- **Container-Managed Transactions** (CMT)
- **Rollback automático** en caso de excepciones
- **Propagación de transacciones** entre EJBs

### Manejo de Excepciones

```java
try {
    resultado = service.saveSingle(entidad);
    return Response.status(Response.Status.OK)
            .entity(resultado)
            .build();
} catch (Throwable e) {
    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
            .entity("Error: " + e.getMessage())
            .build();
}
```

---

## 📊 INTEGRACIÓN ENTRE MÓDULOS

### Relaciones Principales

```
┌─────────────┐
│     CRD     │◄──────┐
│  (Créditos) │       │
└─────────────┘       │
       ↓              │
┌─────────────┐       │
│     CXC     │       │
│  (Cobros)   │       │
└─────────────┘       │
       ↓              │
┌─────────────┐       │
│     TSR     │◄──────┤
│ (Tesorería) │       │
└─────────────┘       │
       ↓              │
┌─────────────┐       │
│     CNT     │◄──────┤
│(Contabilidad)│      │
└─────────────┘       │
       ▲              │
       │              │
┌─────────────┐       │
│     CXP     │───────┘
│   (Pagos)   │
└─────────────┘
       ▲
       │
┌─────────────┐
│     RHH     │
│ (RR.HH.)    │
└─────────────┘
```

### Flujos de Integración

**1. Préstamo → Cobro → Tesorería → Contabilidad**
- Se crea un préstamo en CRD
- Se genera un documento por cobrar en CXC
- Se registra el cobro en TSR
- Se contabiliza en CNT

**2. Nómina → Pago → Tesorería → Contabilidad**
- Se procesa nómina en RHH
- Se generan documentos por pagar en CXP
- Se realizan pagos en TSR
- Se contabiliza en CNT

**3. Todas las operaciones → Contabilidad**
- Cada módulo genera asientos contables
- CNT centraliza toda la información financiera

---

## 🔐 SEGURIDAD

### Autenticación y Autorización
- Gestión mediante `Usuario` y `Jerarquia`
- Control de permisos por módulo
- Niveles de acceso definidos en `NivelesPermisos`

### Auditoría
- Tabla `Auditoria` en módulo CRD
- Registro de todas las operaciones críticas
- Historial de cambios en tablas sensibles (Hist*)

---

## 📝 CONFIGURACIÓN

### Archivo de Configuración Principal
**Ubicación:** `src/main/resources/META-INF/microprofile-config.properties`

### Persistencia
**Ubicación:** `src/main/resources/META-INF/persistence.xml`

### Plantillas de Reportes
**Ubicación:** `src/main/resources/rep/{modulo}/`

---

## 🚀 DESPLIEGUE

### Proceso de Build
```bash
mvn clean package
```

### Artefacto Generado
- **Nombre:** `SaaBE.war`
- **Tipo:** Web Application Archive
- **Ubicación:** `target/SaaBE.war`

### Servidor de Aplicaciones
- **WildFly** (Jakarta EE 10 compatible)
- Despliegue automático o manual

---

## 📚 DOCUMENTACIÓN ADICIONAL

- [Guía de Reportes](docs/Reportes-README.md)
- [API de Reportes](docs/Reportes-API-Guide.md)
- [Configuración de Archivos](docs/FileService-Config.md)
- [Manejo de FormData](docs/FormData-Handling-Guide.md)
- [Implementación de Reportes](IMPLEMENTACION_REPORTES.md)
- [Actualización REST CXC/CXP](ACTUALIZACION_REST_RESUMEN.md)

---

## 📈 ESTADÍSTICAS DEL PROYECTO

### Por Módulo

| Módulo | Entidades | DAOs | Services | REST Controllers | Schemas |
|--------|-----------|------|----------|------------------|---------|
| CRD | 67 | 67 | 67 | 67 | CRD |
| CNT | 29 | 29 | 29 | 29 | CNT |
| CXC | 22 | 22 | 22 | 22 | CXC |
| CXP | 30 | 30 | 30 | 30 | CXP |
| TSR | 57 | 57 | 57 | 57 | TSR |
| RHH | 25 | 25 | 25 | 25 | RHH |
| SCP | 6 | 6 | 6 | 6 | SCP |
| REPORTE | 2 | N/A | N/A | N/A | N/A |
| **TOTAL** | **238** | **236** | **236** | **236** | **7** |

### Archivos del Proyecto
- **Entidades JPA:** 238
- **Interfaces DAO:** 236
- **Implementaciones DAO:** 236
- **Interfaces Service:** 236
- **Implementaciones Service:** 236
- **REST Controllers:** 236
- **Catálogos/Enums:** 120+
- **Plantillas Jasper:** Variable

---

## 🎯 CONCLUSIÓN

SaaBE es un sistema empresarial robusto y escalable que integra múltiples módulos de negocio bajo una arquitectura multicapa estándar de Jakarta EE. Su diseño modular permite la independencia funcional de cada área mientras mantiene la integración necesaria para el flujo completo de información financiera y administrativa.

La separación clara de responsabilidades en capas (Modelo, DAO, Servicio, REST) facilita el mantenimiento, las pruebas y la evolución del sistema. El uso de estándares empresariales como EJB, JPA y JAX-RS garantiza compatibilidad, rendimiento y escalabilidad a largo plazo.

---

**Desarrollado con Jakarta EE 10 y Java 21**  
**© 2026 SaaBE - Sistema de Administración y Automatización Backend**
