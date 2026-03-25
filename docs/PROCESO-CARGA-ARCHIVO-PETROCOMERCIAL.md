# PROCESO DE CARGA Y PROCESAMIENTO DE ARCHIVOS PETROCOMERCIAL

**Última actualización:** 2026-03-25  
**Estado actual:** ✅ FASE 2 IMPLEMENTADA Y CORREGIDA

## 📚 DOCUMENTOS DE REFERENCIA

- **Estándares de Creación de Tablas Oracle:** `docs/ESTANDARES-CREACION-TABLAS-ORACLE.md` ⭐
- **Corrección de Desglose de Pagos:** `docs/CORRECCION-DESGLOSE-PAGOS-PETRO.md`
- **Documentación Completa del Proyecto:** `docs/PROYECTO_DOCUMENTACION.md`

> ⚠️ **IMPORTANTE:** Al crear nuevas tablas, siempre revisar `ESTANDARES-CREACION-TABLAS-ORACLE.md`  
> Este documento contiene el formato correcto para Oracle: `NUMBER`, `VARCHAR2`, sintaxis de IDENTITY, etc.

## 🎯 ESTADO ACTUAL DEL PROYECTO

### ✅ COMPLETADO:
- **FASE 1**: Carga y validación de archivos ✅
- **FASE 2**: Procesamiento y cruce de información ✅
- **CORRECCIÓN CRÍTICA**: Desglose correcto de valores en pagos ✅
- Tabla de tracking `ProcesamientoCargaArchivo` (PRCA) ✅
- Tabla de afectaciones manuales `AfectacionValoresParticipeCarga` (AVPC) ✅ **NUEVO 2026-03-25**
- Endpoints REST para carga y procesamiento ✅
- REST API para afectaciones manuales `/avpc` ✅ **NUEVO 2026-03-25**
- Consultas SQL para novedades ✅
- Documento de estándares para creación de tablas Oracle ✅ **NUEVO 2026-03-25**

### ⚠️ PENDIENTE:
- Generación de aportes (cuando el registro sea tipo APORTE)
- Frontend para visualizar resultados del procesamiento
- Reportes de estadísticas consolidadas

### 🔧 ÚLTIMA CORRECCIÓN REALIZADA (2026-03-24):
Se identificó y corrigió un **error crítico** en el desglose de valores:
- El campo `seguroDescontado` del archivo representa **DESGRAVAMEN**, no seguro de incendio
- Se corrigió el mapeo en `PagoPrestamo`
- Se implementó actualización acumulativa de valores pagados
- Ver detalles en: `docs/CORRECCION-DESGLOSE-PAGOS-PETRO.md`

---

## ⚠️ REGLAS CRÍTICAS DE MODIFICACIÓN DE TABLAS

### 🚫 TABLAS QUE NO SE PUEDEN MODIFICAR
Las siguientes tablas contienen información que viene **directamente del archivo del cliente** y **NO se pueden agregar campos adicionales**:

1. **CargaArchivo (CRAR)** - Información del archivo cargado
2. **DetalleCargaArchivo (DTCA)** - Agrupación por producto del archivo
3. **ParticipeXCargaArchivo (PXCA)** - **CRÍTICO**: Cada línea del archivo TXT

**Razón:** El cliente (PETROCOMERCIAL) nos envía un archivo con formato fijo. No podemos pedirles más información. Debemos trabajar SOLO con los datos que nos proporcionan.

### ✅ TABLAS QUE SÍ PODEMOS MODIFICAR
Podemos agregar campos a tablas que son **de nuestro control interno**:

1. **Prestamo (PRST)** - Podemos agregar campos de control, estados adicionales, etc.
2. **DetallePrestamo (DTPR)** - Podemos agregar saldos, flags de procesamiento, etc.
3. **PagoPrestamo (PGPR)** - Podemos agregar referencias, observaciones, etc.
4. **Aporte (APRT)** - Podemos agregar campos según necesitemos
5. **ProcesamientoCargaArchivo (PRCA)** - **Nueva tabla de tracking** para FASE 2

### 💡 ESTRATEGIA DE TRACKING
Para trackear el procesamiento de la FASE 2 **SIN modificar las tablas del cliente**, creamos la tabla:

**ProcesamientoCargaArchivo (PRCA)** que registra:
- Qué registro del archivo se procesó
- Qué pago o aporte se generó
- Qué errores ocurrieron
- Saldos resultantes
- Estados determinados

Esta tabla es el **puente** entre los datos del cliente y nuestro procesamiento interno.

---

## 📋 ÍNDICE
1. [Visión General del Sistema](#visión-general-del-sistema)
2. [Arquitectura de Tablas](#arquitectura-de-tablas)
3. [Flujo del Proceso Completo](#flujo-del-proceso-completo)
4. [Formato del Archivo TXT](#formato-del-archivo-txt)
5. [Fase 1: Carga y Validación del Archivo](#fase-1-carga-y-validación-del-archivo)
6. [Fase 2: Procesamiento y Cruce de Información (PENDIENTE)](#fase-2-procesamiento-y-cruce-de-información-pendiente)
7. [Estados del Sistema](#estados-del-sistema)
8. [Reglas de Negocio Críticas](#reglas-de-negocio-críticas)
9. [Validaciones Implementadas](#validaciones-implementadas)
10. [Casos de Uso y Escenarios](#casos-de-uso-y-escenarios)
11. [Archivos y Componentes del Sistema](#archivos-y-componentes-del-sistema)

---

## 📊 VISIÓN GENERAL DEL SISTEMA

### Propósito
El sistema procesa archivos TXT mensuales enviados por **PETROCOMERCIAL** que contienen información sobre:
- **Pagos realizados** por las entidades (partícipes) a sus préstamos
- **Pagos NO realizados** (mora, pagos parciales)
- **Aportes mensuales** de los partícipes

### Objetivo Final
Cruzar la información del archivo con los datos internos del sistema para:
1. Actualizar el estado de las cuotas de préstamos (PAGADA, PARCIAL, EN MORA, etc.)
2. Generar registros de aportes en la tabla de Aportes
3. Identificar inconsistencias y generar novedades para revisión manual

### Periodicidad
El archivo se carga **mensualmente** y contiene información de un mes y año específico.

---

## 🗄️ ARQUITECTURA DE TABLAS

### Tablas Principales del Proceso

#### 1. **CargaArchivo (CRAR)**
Tabla maestra que representa cada archivo mensual cargado.

**Campos Clave:**
- `codigo`: PK, ID del registro de carga
- `nombre`: Nombre del archivo cargado
- `fechaCarga`: Fecha en que se cargó el archivo
- `usuarioCarga`: Usuario que realizó la carga
- `filial`: Filial asociada
- `rutaArchivo`: Ruta física donde se almacenó el archivo
- `mesAfectacion`: Mes al que corresponden los datos (1-12)
- `anioAfectacion`: Año al que corresponden los datos
- `estado`: Estado del registro (ACTIVO=1, etc.)
- `totalSaldoActual`, `totalInteresAnual`, etc.: Totales consolidados
- `numeroTransferencia`: Número de transferencia bancaria asociada
- `usuarioContabilidadConfirma`: Usuario de contabilidad que aprueba
- `fechaAutorizacionContabilidad`: Fecha de aprobación

**Relaciones:**
- 1:N con `DetalleCargaArchivo`

---

#### 2. **DetalleCargaArchivo (DTCA)**
Agrupa los registros del archivo por **producto** (tipo de préstamo).

**Campos Clave:**
- `codigo`: PK, ID del detalle
- `cargaArchivo`: FK a CargaArchivo
- `codigoPetroProducto`: Código del producto según PETROCOMERCIAL (ej: "0101", "0102")
- `nombreProductoPetro`: Nombre del producto en el archivo (ej: "HIPOTECARIO", "QUIROGRAFARIO")
- `totalParticipes`: Cantidad de partícipes en este producto
- `totalSaldoActual`, `totalInteresAnual`, etc.: Totales del producto
- `estado`: Estado del registro

**Relaciones:**
- N:1 con `CargaArchivo`
- 1:N con `ParticipeXCargaArchivo`

**Ejemplo:**
Si el archivo tiene registros de préstamos HIPOTECARIOS (código 0101) e QUIROGRAFARIOS (código 0102), se generarán 2 registros de `DetalleCargaArchivo`, uno por cada producto.

---

#### 3. **ParticipeXCargaArchivo (PXCA)**
Cada línea individual del archivo representa un partícipe (entidad) con sus datos de pago.

**Campos Clave:**
- `codigo`: PK, ID del registro
- `detalleCargaArchivo`: FK a DetalleCargaArchivo
- `codigoPetro`: Código ROL del partícipe según PETROCOMERCIAL
- `nombre`: Nombre del partícipe (35 caracteres en el archivo)
- `plazoInicial`: Plazo original del préstamo en meses
- `mesesPlazo`: Meses restantes del plazo
- `saldoActual`: Saldo actual del préstamo
- `interesAnual`: Interés acumulado anual
- `valorSeguro`: Valor del seguro
- `montoDescontar`: Monto total a descontar este mes
- `capitalDescontado`: Capital efectivamente descontado
- `interesDescontado`: Interés efectivamente descontado
- `seguroDescontado`: Seguro efectivamente descontado
- `totalDescontado`: Total efectivamente descontado
- `capitalNoDescontado`: Capital que NO se pudo descontar
- `interesNoDescontado`: Interés que NO se pudo descontar
- `desgravamenNoDescontado`: Desgravamen que NO se pudo descontar
- `estadoRevision`: Estado de revisión del registro
- `novedadesCarga`: Código de novedad durante la carga (ver rubros)
- `novedadesFinancieras`: Código de novedad financiera
- `estado`: Estado del registro (ACTIVO=1)

**Relaciones:**
- N:1 con `DetalleCargaArchivo`

**Novedades durante la Carga (campo `novedadesCarga`):**
- `OK (1)`: Registro validado correctamente
- `PARTICIPE_NO_ENCONTRADO (2)`: No existe entidad con ese código Petro ni nombre
- `CODIGO_ROL_DUPLICADO (3)`: Existe más de una entidad con ese código Petro
- `NOMBRE_ENTIDAD_DUPLICADO (4)`: Existen múltiples entidades con ese nombre
- `CODIGO_PETRO_NO_COINCIDE_CON_NOMBRE (5)`: El código Petro existe pero el nombre no coincide

**Novedades Financieras (campo `novedadesFinancieras`):**
- `SIN_DESCUENTOS (1)`: Hay saldos no descontados pero el descuento total es 0
- `DESCUENTOS_INCOMPLETOS (2)`: Hay descuentos parciales (no se descontó todo lo que se debía)

---

#### 4. **ProcesamientoCargaArchivo (PRCA)** 🆕
Tabla de tracking del procesamiento FASE 2. Registra qué sucedió con cada registro del archivo.

**Campos Clave:**
- `codigo`: PK, ID del procesamiento
- `participeXCargaArchivo`: FK a ParticipeXCargaArchivo (registro procesado)
- `fechaProcesamiento`: Fecha en que se procesó
- `procesado`: Estado del procesamiento (0=No procesado, 1=OK, 2=Error)
- `novedadProcesamiento`: Código de novedad (usa ASPNovedadesCargaArchivo)
- `idPagoGenerado`: ID del PagoPrestamo generado (si aplica)
- `idCuotaProcesada`: ID de la cuota (DetallePrestamo) procesada
- `idAporteGenerado`: ID del Aporte generado (si es aporte)
- `idPrestamoProcessado`: ID del Prestamo procesado
- `saldoCapitalPendiente`: Saldo de capital después del pago
- `saldoInteresPendiente`: Saldo de interés después del pago
- `estadoCuotaDeterminado`: Estado determinado para la cuota (códigos EstadoCuotaPrestamo)
- `observaciones`: Observaciones del procesamiento
- `error`: Descripción del error si hubo
- `usuarioRegistro`: Usuario que ejecutó el procesamiento

**Relaciones:**
- N:1 con `ParticipeXCargaArchivo`

**Estados de Procesamiento:**
- `0`: No procesado aún
- `1`: Procesado exitosamente
- `2`: Error durante el procesamiento

**Propósito:**
Esta tabla nos permite:
- ✅ Saber exactamente qué pasó con cada registro del archivo
- ✅ Trackear qué pagos o aportes se generaron
- ✅ Identificar errores y registros pendientes
- ✅ Evitar reprocesamiento duplicado
- ✅ Mantener trazabilidad completa del proceso
- ✅ NO modificar la tabla ParticipeXCargaArchivo que viene del cliente

**⚠️ REGLA CRÍTICA:**
- NO se pueden agregar campos a `ParticipeXCargaArchivo` porque esa información viene directamente del archivo del cliente
- Toda la información de tracking se almacena en esta tabla separada
- Si necesitamos campos adicionales en otras tablas (Prestamo, DetallePrestamo, etc.) SÍ podemos agregarlos

---

#### 4B. **AfectacionValoresParticipeCarga (AVPC)** 🆕 (2026-03-25)
Tabla para registrar **afectaciones manuales** de valores cuando hay novedades que impiden el procesamiento automático.

**Campos Clave:**
- `codigo`: PK, ID de la afectación
- `novedadParticipeCarga`: FK a NovedadParticipeCarga (tabla padre)
- `prestamo`: FK a Prestamo (préstamo al que se afecta)
- `detallePrestamo`: FK a DetallePrestamo (cuota específica a afectar)
- **Valores Originales de la Cuota:**
  - `valorCuotaOriginal`: Valor total de la cuota original
  - `capitalCuotaOriginal`: Capital de la cuota original
  - `interesCuotaOriginal`: Interés de la cuota original
  - `desgravamenCuotaOriginal`: Desgravamen de la cuota original
- **Valores a Afectar (indicados por el usuario):**
  - `valorAfectar`: Valor total a afectar (requerido)
  - `capitalAfectar`: Capital a afectar
  - `interesAfectar`: Interés a afectar
  - `desgravamenAfectar`: Desgravamen a afectar
- **Diferencias Calculadas (automáticas):**
  - `diferenciaTotal`: valor cuota - valor afectar
  - `diferenciaCapital`, `diferenciaInteres`, `diferenciaDesgravamen`
- **Auditoría:**
  - `fechaAfectacion`: Fecha en que se registró
  - `usuarioRegistro`: Usuario que registró
  - `fechaCreacionRegistro`: Timestamp automático
- `observaciones`: Texto libre para notas del usuario
- `estado`: Estado del registro (1=ACTIVO)

**Relaciones:**
- N:1 con `NovedadParticipeCarga`
- N:1 con `Prestamo`
- N:1 con `DetallePrestamo`

**Propósito:**
Cuando el procesamiento automático de FASE 2 detecta novedades (préstamo no encontrado, múltiples préstamos activos, montos inconsistentes, etc.), el sistema NO puede determinar automáticamente cómo distribuir los valores del archivo.

En estos casos:
1. El frontend muestra las novedades al usuario
2. El usuario indica manualmente:
   - A qué préstamo específico afectar
   - A qué cuota específica afectar
   - Qué valores exactos aplicar (capital, interés, desgravamen)
3. El sistema guarda esta decisión en `AfectacionValoresParticipeCarga`
4. Al reprocesar, el sistema usa esta tabla como referencia para aplicar los valores

**Ejemplo de uso:**
```
Novedad: MULTIPLES_PRESTAMOS_ACTIVOS
- El archivo indica un pago de $500 para el ROL 12345
- Existen 2 préstamos activos para esa entidad
- El usuario indica manualmente:
  * Préstamo ID: 1001
  * Cuota: 15
  * Capital a afectar: $400
  * Interés a afectar: $100
- Se guarda en AVPC con esos datos
- Al reprocesar, se aplica el pago a ese préstamo/cuota específica
```

**Script de creación:** `docs/CREATE_TABLE_AVPC_AfectacionValoresParticipeCarga.sql`  
**REST API:** `/avpc` - Ver `AfectacionValoresParticipeCargaRest.java`

---

#### 5. **Entidad (ENTD)**
Tabla que representa a los partícipes (personas/empresas que tienen préstamos y aportes).

**Campos Clave para Petrocomercial:**
- `codigo`: PK
- `razonSocial`: Nombre completo de la entidad
- `rolPetroComercial`: **Código ROL asignado por PETROCOMERCIAL** (se relaciona con `codigoPetro` en ParticipeXCargaArchivo)

**Importante:**
- El campo `rolPetroComercial` es la clave para relacionar los registros del archivo con las entidades del sistema
- Si una entidad no tiene `rolPetroComercial`, se intenta encontrar por nombre (primeros 35 caracteres)
- Si se encuentra por nombre, se actualiza automáticamente el `rolPetroComercial`

---

#### 5. **Producto (PRDC)**
Catálogo de productos (tipos de préstamos) del sistema.

**Campos Clave:**
- `codigo`: PK
- `nombre`: Nombre del producto (ej: "HIPOTECARIO", "QUIROGRAFARIO")
- `codigoSBS`: Código según Superintendencia de Bancos
- `codigoPetro`: **Código del producto según PETROCOMERCIAL** (ej: "0101", "0102")
- `tipoPrestamo`: FK a TipoPrestamo

**Mapeo de Códigos:**
El campo `codigoPetro` permite relacionar el código que viene en el archivo (`codigoPetroProducto` en DetalleCargaArchivo) con el producto interno del sistema.

**Ejemplo:**
```
Archivo TXT:        Sistema Interno:
0101 HIPOTECARIO -> Producto.codigoPetro = "0101" -> Producto.codigo = 5
0102 QUIROGRAFARIO -> Producto.codigoPetro = "0102" -> Producto.codigo = 8
```

---

#### 6. **Prestamo (PRST)**
Tabla de préstamos otorgados a las entidades.

**Campos Clave:**
- `codigo`: PK
- `entidad`: FK a Entidad (¿Quién tiene el préstamo?)
- `producto`: FK a Producto (¿Qué tipo de préstamo es?)
- `montoSolicitado`: Monto original del préstamo
- `plazo`: Plazo en meses
- `valorCuota`: Valor de la cuota mensual
- `tasa`: Tasa de interés anual
- `fechaInicio`: Fecha de inicio del préstamo
- `estadoPrestamo`: Estado del préstamo (GENERADO, VIGENTE, CANCELADO, etc.)
- `saldoTotal`: Saldo pendiente total
- `idAsoprep`: ID del sistema externo ASOPREP (opcional)

**Relaciones:**
- N:1 con `Entidad`
- N:1 con `Producto`
- 1:N con `DetallePrestamo` (cuotas)

---

#### 7. **DetallePrestamo (DTPR)**
Cada cuota individual de un préstamo.

**Campos Clave:**
- `codigo`: PK
- `prestamo`: FK a Prestamo
- `numeroCuota`: Número de la cuota (1, 2, 3, ...)
- `fechaVencimiento`: Fecha de vencimiento de la cuota
- `capital`: Capital a pagar en esta cuota
- `interes`: Interés a pagar en esta cuota
- `cuota`: Valor total de la cuota (capital + interés)
- `saldoCapital`: Saldo de capital después de esta cuota
- `mora`: Valor de mora acumulado
- `interesVencido`: Interés vencido acumulado
- `capitalPagado`: Capital efectivamente pagado
- `interesPagado`: Interés efectivamente pagado
- `abono`: Abonos extraordinarios
- `estado`: **Estado de la cuota** (PENDIENTE, ACTIVA, PAGADA, EN_MORA, PARCIAL, etc.)
- `fechaPagado`: Fecha en que se pagó la cuota
- `desgravamen`: Valor del desgravamen/seguro
- `saldoInteres`, `saldoMora`, etc.: Saldos pendientes

**Relaciones:**
- N:1 con `Prestamo`
- 1:N con `PagoPrestamo` (puede tener múltiples pagos parciales)

---

#### 8. **PagoPrestamo (PGPR)**
Registro de cada pago realizado a una cuota de préstamo.

**Campos Clave:**
- `codigo`: PK
- `prestamo`: FK a Prestamo
- `detallePrestamo`: FK a DetallePrestamo (cuota pagada)
- `fecha`: Fecha del pago
- `valor`: Valor total pagado
- `numeroCuota`: Número de cuota pagada
- `capitalPagado`: Capital pagado
- `interesPagado`: Interés pagado
- `moraPagada`: Mora pagada
- `interesVencidoPagado`: Interés vencido pagado
- `desgravamen`: Desgravamen pagado
- `valorSeguroIncendio`: Seguro de incendio pagado
- `saldoOtros`: Otros valores pagados
- `observacion`: Observaciones del pago
- `tipo`: Tipo de pago (ej: "DESCUENTO_NOMINA", "CAJA", "MIGRACION")
- `estado`: Estado del registro

**Relaciones:**
- N:1 con `Prestamo`
- N:1 con `DetallePrestamo`

---

#### 9. **Aporte (APRT)**
Tabla de aportes mensuales de los partícipes.

**Campos Clave:**
- `codigo`: PK
- `filial`: FK a Filial
- `entidad`: FK a Entidad (¿Quién aporta?)
- `contrato`: FK a Contrato (opcional)
- `tipoAporte`: FK a TipoAporte (ej: APORTE MENSUAL, APORTE EXTRAORDINARIO)
- `fechaTransaccion`: Fecha del aporte
- `glosa`: Descripción/observación
- `valor`: Valor del aporte
- `valorPagado`: Valor efectivamente pagado
- `saldo`: Saldo pendiente del aporte
- `idAsoprep`: ID del sistema externo
- `estado`: Estado del registro

**Relaciones:**
- N:1 con `Entidad`
- N:1 con `TipoAporte`

**Importante:**
Los aportes se generan mensualmente desde el archivo de PETROCOMERCIAL cuando el `codigoPetroProducto` corresponde a un producto tipo "APORTE" (no es un préstamo).

---

## 🔄 FLUJO DEL PROCESO COMPLETO

### FASE 1: CARGA Y VALIDACIÓN ✅ (IMPLEMENTADA)

```
┌─────────────────────────────────────────────────────────────┐
│ 1. USUARIO SUBE ARCHIVO TXT                                 │
│    - Frontend envía: archivo, mes, año, filial, usuario    │
└────────────────┬────────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────────┐
│ 2. VALIDACIONES INICIALES                                   │
│    ✓ Archivo tiene extensión .txt                          │
│    ✓ Archivo no está vacío                                 │
│    ✓ Contenido tiene formato correcto (inicia con "EP")    │
└────────────────┬────────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────────┐
│ 3. LECTURA Y PARSEO DEL ARCHIVO                            │
│    - Lee con codificación ISO-8859-1 (para ñ, tildes)     │
│    - Identifica encabezados "EP"                            │
│    - Extrae bloques de productos                            │
│    - Parsea cada línea según posiciones fijas               │
└────────────────┬────────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────────┐
│ 4. PROCESAMIENTO DE REGISTROS                               │
│    - Crea lista de ParticipeXCargaArchivo                  │
│    - Agrupa por producto (DetalleCargaArchivo)             │
│    - Calcula totales por producto                           │
│    - Calcula totales generales (CargaArchivo)              │
└────────────────┬────────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────────┐
│ 5. VALIDACIÓN DE ENTIDADES (PARTICIPES)                    │
│    Para cada ParticipeXCargaArchivo:                        │
│                                                              │
│    5.1. Buscar Entidad por rolPetroComercial               │
│         ├─ NO ENCONTRADO → Buscar por nombre (35 chars)    │
│         │   ├─ 0 resultados → PARTICIPE_NO_ENCONTRADO      │
│         │   ├─ 1 resultado → Actualizar rolPetroComercial  │
│         │   └─ >1 resultados → NOMBRE_DUPLICADO            │
│         ├─ 1 ENCONTRADO → Validar nombre coincida          │
│         │   ├─ Coincide → OK                                │
│         │   └─ No coincide → CODIGO_NO_COINCIDE_CON_NOMBRE │
│         └─ >1 ENCONTRADOS → CODIGO_ROL_DUPLICADO           │
│                                                              │
│    5.2. Validación Financiera                               │
│         Si (capitalND>0 OR interesND>0 OR desgravamenND>0) │
│         ├─ totalDescontado = 0 → SIN_DESCUENTOS            │
│         └─ totalDescontado > 0 → DESCUENTOS_INCOMPLETOS    │
└────────────────┬────────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────────┐
│ 6. PERSISTENCIA EN BASE DE DATOS (TRANSACCIONAL)           │
│    6.1. Guardar CargaArchivo                                │
│    6.2. Para cada DetalleCargaArchivo:                      │
│         - Guardar DetalleCargaArchivo                       │
│         - Guardar ParticipeXCargaArchivo asociados          │
└────────────────┬────────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────────┐
│ 7. ALMACENAMIENTO FÍSICO DEL ARCHIVO                       │
│    Ruta: aportes/{año}/{mes}/{nombre_archivo}.txt          │
│    Actualiza CargaArchivo.rutaArchivo                       │
└────────────────┬────────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────────┐
│ ✅ FASE 1 COMPLETADA                                        │
│    - Archivo cargado y validado                             │
│    - Entidades identificadas o marcadas con novedades      │
│    - Listo para FASE 2: Procesamiento financiero           │
└─────────────────────────────────────────────────────────────┘
```

---

### FASE 2: PROCESAMIENTO Y CRUCE DE INFORMACIÓN ⚠️ (PENDIENTE DE IMPLEMENTACIÓN)

Esta fase es **CRÍTICA** y aún no está implementada. Aquí se debe cruzar la información del archivo con préstamos y aportes.

```
┌─────────────────────────────────────────────────────────────┐
│ 1. INICIO DEL PROCESAMIENTO                                 │
│    Input: CargaArchivo.codigo (archivo ya validado)        │
└────────────────┬────────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────────┐
│ 2. OBTENER REGISTROS A PROCESAR                             │
│    - Cargar todos los ParticipeXCargaArchivo del archivo   │
│    - Filtrar solo los que tienen novedadesCarga = OK (1)   │
└────────────────┬────────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────────┐
│ 3. CLASIFICAR REGISTROS POR TIPO                            │
│    Para cada ParticipeXCargaArchivo:                        │
│                                                              │
│    3.1. Obtener DetalleCargaArchivo.codigoPetroProducto    │
│    3.2. Buscar Producto por codigoPetro                     │
│         ├─ NO ENCONTRADO → Marcar error                     │
│         └─ ENCONTRADO → Clasificar:                         │
│             ├─ Es APORTE → Lista de Aportes                │
│             └─ Es PRÉSTAMO → Lista de Préstamos            │
└────────────────┬────────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────────┐
│ 4. PROCESAMIENTO DE APORTES                                 │
│    Para cada registro tipo APORTE:                          │
│                                                              │
│    4.1. Obtener Entidad por rolPetroComercial              │
│    4.2. Crear registro de Aporte:                           │
│         - entidad = Entidad encontrada                      │
│         - tipoAporte = según producto                       │
│         - fechaTransaccion = último día del mes/año archivo│
│         - valor = montoDescontar                            │
│         - valorPagado = totalDescontado                     │
│         - saldo = valor - valorPagado                       │
│         - glosa = "Aporte mes {mes}/{año} - Petrocomercial"│
│         - estado = ACTIVO                                   │
│                                                              │
│    4.3. Guardar Aporte en BD                                │
│    4.4. Actualizar ParticipeXCargaArchivo:                  │
│         - Marcar como procesado                             │
│         - Asociar ID del aporte generado (nuevo campo)      │
└────────────────┬────────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────────┐
│ 5. PROCESAMIENTO DE PRÉSTAMOS                               │
│    Para cada registro tipo PRÉSTAMO:                        │
│                                                              │
│    5.1. BUSCAR EL PRÉSTAMO                                  │
│         Buscar Prestamo donde:                              │
│         - entidad.rolPetroComercial = registro.codigoPetro │
│         - producto.codigoPetro = codigoPetroProducto       │
│         - estadoPrestamo IN (VIGENTE, EN_MORA)             │
│                                                              │
│         Resultado:                                          │
│         ├─ 0 préstamos → ERROR: PRESTAMO_NO_ENCONTRADO     │
│         ├─ >1 préstamos → ERROR: MULTIPLES_PRESTAMOS       │
│         └─ 1 préstamo → Continuar                          │
│                                                              │
│    5.2. IDENTIFICAR LA CUOTA CORRESPONDIENTE                │
│         Criterios de búsqueda de cuota:                     │
│         - prestamo = préstamo encontrado                    │
│         - fechaVencimiento.mes = mesAfectacion             │
│         - fechaVencimiento.año = anioAfectacion            │
│         - estado IN (PENDIENTE, ACTIVA, EMITIDA)           │
│                                                              │
│         Resultado:                                          │
│         ├─ 0 cuotas → ERROR: CUOTA_NO_ENCONTRADA           │
│         ├─ >1 cuotas → ADVERTENCIA: Tomar la primera       │
│         └─ 1 cuota → Continuar                             │
│                                                              │
│    5.3. ANÁLISIS DEL PAGO                                   │
│         Obtener del archivo:                                │
│         - montoDescontar = Monto que se debía pagar        │
│         - totalDescontado = Monto que SÍ se pagó           │
│         - capitalDescontado = Capital pagado                │
│         - interesDescontado = Interés pagado                │
│         - seguroDescontado = Seguro pagado                  │
│         - capitalNoDescontado = Capital NO pagado           │
│         - interesNoDescontado = Interés NO pagado           │
│         - desgravamenNoDescontado = Desgravamen NO pagado   │
│                                                              │
│         Obtener de la cuota:                                │
│         - capital = Capital de la cuota                     │
│         - interes = Interés de la cuota                     │
│         - desgravamen = Desgravamen de la cuota             │
│         - cuota = Valor total de la cuota                   │
│                                                              │
│    5.4. DETERMINAR ESTADO DE LA CUOTA                       │
│         Lógica de decisión:                                 │
│                                                              │
│         A) PAGO COMPLETO (totalDescontado >= montoDescontar)│
│            └─> Estado = PAGADA                              │
│                                                              │
│         B) PAGO PARCIAL (totalDescontado > 0 Y < monto)    │
│            └─> Estado = PARCIAL                             │
│                                                              │
│         C) SIN PAGO (totalDescontado = 0)                   │
│            ├─ Fecha vencimiento > hoy → ACTIVA             │
│            └─ Fecha vencimiento <= hoy → EN_MORA           │
│                                                              │
│         D) VALIDAR MONTOS                                   │
│            Si (capitalDescontado > cuota.capital):          │
│            └─> ADVERTENCIA: Pago mayor al capital          │
│                                                              │
│            Si (interesDescontado > cuota.interes):          │
│            └─> ADVERTENCIA: Pago mayor al interés          │
│                                                              │
│    5.5. CREAR REGISTRO DE PAGO                              │
│         Si totalDescontado > 0:                             │
│                                                              │
│         Crear PagoPrestamo:                                 │
│         - prestamo = préstamo encontrado                    │
│         - detallePrestamo = cuota encontrada                │
│         - fecha = último día del mes/año archivo            │
│         - valor = totalDescontado                           │
│         - numeroCuota = cuota.numeroCuota                   │
│         - capitalPagado = capitalDescontado                 │
│         - interesPagado = interesDescontado                 │
│         - desgravamen = seguroDescontado                    │
│         - valorSeguroIncendio = seguroDescontado            │
│         - moraPagada = 0 (o calcular si aplica)            │
│         - interesVencidoPagado = 0 (o calcular si aplica)  │
│         - observacion = "Pago mes {mes}/{año} Petrocomercial"│
│         - tipo = "DESCUENTO_NOMINA"                         │
│         - estado = ACTIVO                                   │
│                                                              │
│         Guardar PagoPrestamo en BD                          │
│                                                              │
│    5.6. ACTUALIZAR LA CUOTA                                 │
│         Actualizar DetallePrestamo:                         │
│         - estado = Estado determinado en 5.4                │
│         - capitalPagado = capitalDescontado                 │
│         - interesPagado = interesDescontado                 │
│         - fechaPagado = último día del mes/año              │
│         - saldoCapital = saldoCapital - capitalDescontado   │
│         - saldoInteres = saldoInteres - interesDescontado   │
│                                                              │
│         Si estado = PAGADA:                                 │
│         - saldoCapital = 0                                  │
│         - saldoInteres = 0                                  │
│                                                              │
│         Guardar DetallePrestamo                             │
│                                                              │
│    5.7. ACTUALIZAR EL PRÉSTAMO                              │
│         Actualizar Prestamo:                                │
│         - totalPagado += totalDescontado                    │
│         - totalCapital actualizado                          │
│         - totalInteres actualizado                          │
│         - saldoTotal = suma de saldos de cuotas pendientes │
│                                                              │
│         Recalcular estado del préstamo:                     │
│         - Si todas las cuotas pagadas → CANCELADO          │
│         - Si tiene cuotas en mora → EN_MORA                │
│         - Si tiene cuotas activas → VIGENTE                │
│                                                              │
│         Guardar Prestamo                                    │
│                                                              │
│    5.8. ACTUALIZAR PARTICIPE EN CARGA                       │
│         Actualizar ParticipeXCargaArchivo:                  │
│         - Marcar como procesado                             │
│         - Asociar ID del pago generado (nuevo campo)        │
│         - Asociar ID de la cuota procesada (nuevo campo)    │
└────────────────┬────────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────────┐
│ 6. MANEJO DE ERRORES Y EXCEPCIONES                          │
│    Para registros con errores:                              │
│    - Marcar ParticipeXCargaArchivo con código de error     │
│    - NO generar pago ni actualizar cuota                    │
│    - Registrar en log para revisión manual                  │
│                                                              │
│    Códigos de error (crear rubros nuevos):                  │
│    - PRESTAMO_NO_ENCONTRADO                                 │
│    - MULTIPLES_PRESTAMOS_ACTIVOS                            │
│    - CUOTA_NO_ENCONTRADA                                    │
│    - MONTO_INCONSISTENTE                                    │
│    - PRODUCTO_NO_MAPEADO                                    │
└────────────────┬────────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────────┐
│ 7. ACTUALIZAR ESTADO DE LA CARGA                            │
│    Actualizar CargaArchivo:                                 │
│    - estado = PROCESADO                                     │
│    - Contar: total procesados OK                            │
│    - Contar: total con errores                              │
│    - Guardar estadísticas de procesamiento                  │
└────────────────┬────────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────────┐
│ 8. GENERAR REPORTE DE PROCESAMIENTO                         │
│    - Total registros procesados                             │
│    - Total aportes generados                                │
│    - Total pagos aplicados                                  │
│    - Total cuotas actualizadas                              │
│    - Total errores por tipo                                 │
│    - Lista de registros pendientes de revisión              │
└─────────────────┬────────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────────┐
│ ✅ FASE 2 COMPLETADA                                        │
│    - Aportes generados                                      │
│    - Cuotas actualizadas                                    │
│    - Pagos registrados                                      │
│    - Préstamos actualizados                                 │
│    - Inconsistencias identificadas para revisión manual     │
└─────────────────────────────────────────────────────────────┘
```

---

## 📄 FORMATO DEL ARCHIVO TXT

### Estructura General

El archivo TXT tiene una estructura de **bloques por producto**:

```
EP                         <-- Encabezado (8 líneas)
...
0101 HIPOTECARIO          <-- Línea de producto
                           <-- Línea en blanco
ROL     NOMBRE...         <-- Encabezado de columnas (ignorar)
12345   PEREZ JUAN...     <-- Datos de partícipe
67890   GOMEZ MARIA...    <-- Datos de partícipe
...
EP                         <-- Nuevo encabezado (siguiente producto)
...
0102 QUIROGRAFARIO
...
```

### Detalle de Posiciones

**Línea de Partícipe (posiciones fijas):**

| Posición | Longitud | Campo | Descripción |
|----------|----------|-------|-------------|
| 0-7 | 7 | Código Petro | ROL del partícipe |
| 7-44 | 37 | Nombre | Nombre del partícipe (35 útiles) |
| 44-50 | 6 | Plazo Inicial | Plazo original en meses |
| 50-61 | 11 | Saldo Actual | Saldo actual del préstamo |
| 61-65 | 4 | Meses Plazo | Meses restantes |
| 65-70 | 5 | Interés Anual | Interés acumulado anual |
| 70-80 | 10 | Valor Seguro | Valor del seguro |
| 80-95 | 15 | Monto Descontar | Total a descontar |
| 95-110 | 15 | Capital Descontado | Capital pagado |
| 110-125 | 15 | Interés Descontado | Interés pagado |
| 125-140 | 15 | Seguro Descontado | Seguro pagado |
| 140-155 | 15 | Total Descontado | Total efectivamente pagado |
| 155-170 | 15 | Capital No Descontado | Capital NO pagado |
| 170-184 | 14 | Interés No Descontado | Interés NO pagado |
| 184-198 | 14 | Desgravamen No Descontado | Desgravamen NO pagado |

### Ejemplo de Registro

```
12345  PEREZ JUAN CARLOS               24    5000.00  12  250.00    50.00      500.00        400.00         100.00          50.00         550.00           0.00            0.00              0.00
```

**Interpretación:**
- ROL: 12345
- Nombre: PEREZ JUAN CARLOS
- Plazo inicial: 24 meses
- Saldo actual: $5,000.00
- Meses restantes: 12
- Interés anual: $250.00
- Valor seguro: $50.00
- Monto a descontar: $500.00
- Capital descontado: $400.00 ✅
- Interés descontado: $100.00 ✅
- Seguro descontado: $50.00 ✅
- Total descontado: $550.00 ✅
- Capital no descontado: $0.00 ✅
- Interés no descontado: $0.00 ✅
- Desgravamen no descontado: $0.00 ✅

**Conclusión:** Pago completo, la cuota debe marcarse como **PAGADA**.

---

## 🎯 ESTADOS DEL SISTEMA

### Estados de Préstamo (EstadoPrestamo - Rubros)

| Código | Constante | Descripción |
|--------|-----------|-------------|
| 1 | GENERADO | Préstamo creado pero no desembolsado |
| 2 | VIGENTE | Préstamo activo y al día |
| 3 | CANCELADO | Préstamo totalmente cancelado |
| 4 | CANCELADO_ANTICIPADO | Cancelado antes del plazo |
| 5 | CANCELADO_POR_NOVACION | Cancelado por novación |
| 6 | PENDIENTE_DE_APROBACION | Esperando aprobación |
| 7 | RECHAZADO | Préstamo rechazado |
| 8 | DE_PLAZO_VENCIDO | Plazo cumplido pero no cancelado |
| 9 | CANCELADO_POR_REVISAR | Cancelado pendiente revisión |
| 10 | VIGENTE_POR_REVISAR | Vigente pendiente revisión |
| 11 | EN_MORA | Préstamo con cuotas vencidas |

### Estados de Cuota (EstadoCuotaPrestamo - Rubros)

| Código | Constante | Descripción |
|--------|-----------|-------------|
| 1 | PENDIENTE | Cuota no vencida, sin pago |
| 2 | ACTIVA | Cuota vigente, próxima a vencer |
| 3 | EMITIDA | Cuota emitida para pago |
| 4 | PAGADA | Cuota totalmente pagada |
| 5 | EN_MORA | Cuota vencida sin pago |
| 6 | PARCIAL | Cuota con pago parcial |
| 7 | CANCELADA_ANTICIPADA | Cancelada antes de vencimiento |
| 8 | VENCIDA | Cuota vencida |

### Estados de Revisión (ASPEstadoRevisionParticipeCarga)

Estados para marcar el progreso de revisión de los partícipes cargados.

| Código | Descripción |
|--------|-------------|
| 1 | PENDIENTE |
| 2 | EN_REVISION |
| 3 | REVISADO_OK |
| 4 | REVISADO_CON_ERRORES |

### Novedades durante Carga (ASPNovedadesCargaArchivo)

| Código | Constante | Descripción |
|--------|-----------|-------------|
| 1 | OK | Registro validado correctamente |
| 2 | PARTICIPE_NO_ENCONTRADO | No existe entidad con ese código ni nombre |
| 3 | CODIGO_ROL_DUPLICADO | Múltiples entidades con mismo código Petro |
| 4 | NOMBRE_ENTIDAD_DUPLICADO | Múltiples entidades con mismo nombre |
| 5 | CODIGO_PETRO_NO_COINCIDE_CON_NOMBRE | Código existe pero nombre no coincide |
| 6 | SIN_DESCUENTOS | Hay saldos pero descuento total es 0 |
| 7 | DESCUENTOS_INCOMPLETOS | Descuento parcial |

**NOTA:** Crear nuevos códigos para FASE 2:
- PRESTAMO_NO_ENCONTRADO
- MULTIPLES_PRESTAMOS_ACTIVOS  
- CUOTA_NO_ENCONTRADA
- MONTO_INCONSISTENTE
- PRODUCTO_NO_MAPEADO

---

## ⚖️ REGLAS DE NEGOCIO CRÍTICAS

### 1. Correspondencia de Códigos Petro

**Entidades:**
- El archivo trae `codigoPetro` (ROL)
- En BD se almacena en `Entidad.rolPetroComercial`
- Si no existe, buscar por nombre (35 primeros caracteres de `razonSocial`)
- Si se encuentra por nombre, actualizar automáticamente el `rolPetroComercial`

**Productos:**
- El archivo trae `codigoPetroProducto` (ej: "0101", "0102")
- En BD debe existir `Producto.codigoPetro` con ese valor
- La relación determina si es préstamo o aporte

### 2. Identificación del Tipo de Registro

```
Si Producto.tipoPrestamo = "APORTE":
    → Es un APORTE, generar en tabla Aporte
Sino:
    → Es un PRÉSTAMO, buscar préstamo y cuota correspondiente
```

### 3. Búsqueda de Préstamo

**Criterios obligatorios:**
```sql
SELECT * FROM Prestamo p
WHERE p.entidad.rolPetroComercial = {codigoPetro}
  AND p.producto.codigoPetro = {codigoPetroProducto}
  AND p.estadoPrestamo IN (VIGENTE, EN_MORA, VIGENTE_POR_REVISAR)
```

**Resultados:**
- **0 registros:** ERROR - No hay préstamo activo
- **1 registro:** OK - Procesar ese préstamo
- **>1 registros:** ERROR - Múltiples préstamos activos (inconsistencia)

### 4. Búsqueda de Cuota

**Criterios obligatorios:**
```sql
SELECT * FROM DetallePrestamo dp
WHERE dp.prestamo.codigo = {prestamoEncontrado}
  AND MONTH(dp.fechaVencimiento) = {mesAfectacion}
  AND YEAR(dp.fechaVencimiento) = {anioAfectacion}
  AND dp.estado IN (PENDIENTE, ACTIVA, EMITIDA)
ORDER BY dp.numeroCuota ASC
LIMIT 1
```

**Nota:** Si hay múltiples cuotas en el mismo mes (raro), tomar la primera.

### 5. Determinación del Estado de la Cuota ⭐

**Algoritmo de decisión:**

```java
// Valores del archivo
double montoDescontar = participe.getMontoDescontar();
double totalDescontado = participe.getTotalDescontado();
double capitalDescontado = participe.getCapitalDescontado();
double interesDescontado = participe.getInteresDescontado();
double desgravamenDescontado = participe.getSeguroDescontado(); // ⚠️ seguroDescontado = DESGRAVAMEN

// Valores de la cuota
double cuotaCapital = detallePrestamo.getCapital();
double cuotaInteres = detallePrestamo.getInteres();
double cuotaDesgravamen = detallePrestamo.getDesgravamen();
double cuotaTotal = cuotaCapital + cuotaInteres + cuotaDesgravamen;

// TOLERANCIA para redondeos
final double TOLERANCIA = 1.0;

// Decisión
if (totalDescontado >= (montoDescontar - TOLERANCIA)) {
    // PAGO COMPLETO
    nuevoEstado = EstadoCuotaPrestamo.PAGADA;
    
} else if (totalDescontado > 0 && totalDescontado < montoDescontar) {
    // PAGO PARCIAL
    nuevoEstado = EstadoCuotaPrestamo.PARCIAL;
    
} else if (totalDescontado == 0) {
    // SIN PAGO
    LocalDateTime hoy = LocalDateTime.now();
    if (detallePrestamo.getFechaVencimiento().isAfter(hoy)) {
        nuevoEstado = EstadoCuotaPrestamo.ACTIVA; // Aún no vence
    } else {
        nuevoEstado = EstadoCuotaPrestamo.EN_MORA; // Ya venció
    }
}

// Validaciones adicionales
if (capitalDescontado > cuotaCapital + TOLERANCIA) {
    // ADVERTENCIA: Se pagó más capital del esperado
    // Puede ser abono extraordinario
}

if (interesDescontado > cuotaInteres + TOLERANCIA) {
    // ADVERTENCIA: Se pagó más interés del esperado
}
```

### 5.1. ⚠️ MAPEO CORRECTO DE CAMPOS DEL ARCHIVO (CRÍTICO)

**IMPORTANTE:** El archivo Petrocomercial NO envía el seguro de incendio desglosado.

| Campo del Archivo | Significado Real | Campo en BD (PagoPrestamo) |
|-------------------|------------------|----------------------------|
| `capitalDescontado` | Capital pagado | `capitalPagado` ✅ |
| `interesDescontado` | Interés pagado | `interesPagado` ✅ |
| `seguroDescontado` | **DESGRAVAMEN pagado** ⚠️ | `desgravamen` ✅ |
| *(No viene)* | Seguro de incendio | `valorSeguroIncendio` = 0 |

**Código correcto para crear PagoPrestamo:**
```java
PagoPrestamo pago = new PagoPrestamo();
pago.setCapitalPagado(participe.getCapitalDescontado());
pago.setInteresPagado(participe.getInteresDescontado());
pago.setDesgravamen(participe.getSeguroDescontado()); // ⚠️ Este campo es DESGRAVAMEN
pago.setValorSeguroIncendio(0.0); // No viene en el archivo
pago.setValor(participe.getTotalDescontado());
```

**Ver detalles completos en:** `docs/CORRECCION-DESGLOSE-PAGOS-PETRO.md`

### 6. Actualización de Saldos ⭐

**En DetallePrestamo (cuota) - VALORES ACUMULATIVOS:**
```java
// ⚠️ IMPORTANTE: Los valores pagados son ACUMULATIVOS
// Si hay pagos anteriores, se SUMAN
double capitalPagadoAnterior = nullSafe(detallePrestamo.getCapitalPagado());
double interesPagadoAnterior = nullSafe(detallePrestamo.getInteresPagado());
double desgravamenPagadoAnterior = nullSafe(detallePrestamo.getDesgravamenPagado());

// El archivo envía el desgravamen en el campo seguroDescontado
double desgravamenDescontado = participe.getSeguroDescontado();

// Actualizar valores pagados (ACUMULATIVO)
detallePrestamo.setCapitalPagado(capitalPagadoAnterior + capitalDescontado);
detallePrestamo.setInteresPagado(interesPagadoAnterior + interesDescontado);
detallePrestamo.setDesgravamenPagado(desgravamenPagadoAnterior + desgravamenDescontado);

// Calcular saldos pendientes
double capitalOriginal = detallePrestamo.getCapital();
double interesOriginal = detallePrestamo.getInteres();

double saldoCapital = capitalOriginal - detallePrestamo.getCapitalPagado();
double saldoInteres = interesOriginal - detallePrestamo.getInteresPagado();

// Asegurar que no sean negativos
detallePrestamo.setSaldoCapital(Math.max(0, saldoCapital));
detallePrestamo.setSaldoInteres(Math.max(0, saldoInteres));

// Si pago completo
if (nuevoEstado == EstadoCuotaPrestamo.PAGADA) {
    detallePrestamo.setSaldoCapital(0.0);
    detallePrestamo.setSaldoInteres(0.0);
    detallePrestamo.setFechaPagado(ultimoDiaDelMes);
}
```

**Esto permite:**
- ✅ Múltiples pagos parciales a una misma cuota
- ✅ Correcta acumulación de valores
- ✅ Saldos siempre correctos

**En Prestamo:**
```java
// Recalcular totales
prestamo.setTotalPagado(prestamo.getTotalPagado() + totalDescontado);

// Recalcular saldo total sumando saldos de todas las cuotas pendientes
double saldoTotal = 0.0;
for (DetallePrestamo cuota : prestamo.getCuotas()) {
    if (cuota.getEstado() != EstadoCuotaPrestamo.PAGADA) {
        saldoTotal += cuota.getSaldoCapital();
    }
}
prestamo.setSaldoTotal(saldoTotal);

// Determinar nuevo estado del préstamo
if (saldoTotal == 0) {
    prestamo.setEstadoPrestamo(EstadoPrestamo.CANCELADO);
} else if (tieneCuotasEnMora()) {
    prestamo.setEstadoPrestamo(EstadoPrestamo.EN_MORA);
} else {
    prestamo.setEstadoPrestamo(EstadoPrestamo.VIGENTE);
}
```

### 7. Generación de Aportes

**Cuando el producto es tipo "APORTE":**

```java
Aporte aporte = new Aporte();
aporte.setEntidad(entidad); // Encontrada por rolPetroComercial
aporte.setFilial(cargaArchivo.getFilial());
aporte.setTipoAporte(determinarTipoAporte(producto));
aporte.setFechaTransaccion(ultimoDiaDelMes(mesAfectacion, anioAfectacion));
aporte.setValor(participe.getMontoDescontar());
aporte.setValorPagado(participe.getTotalDescontado());
aporte.setSaldo(participe.getMontoDescontar() - participe.getTotalDescontado());
aporte.setGlosa("Aporte " + mesAfectacion + "/" + anioAfectacion + " - Petrocomercial");
aporte.setEstado(Estado.ACTIVO);
```

### 8. Manejo de Pagos Parciales

Si una cuota queda en estado PARCIAL:
- El saldo pendiente debe quedar registrado
- En el próximo archivo mensual podría venir el resto del pago
- El sistema debe sumar el nuevo pago al anterior
- Cuando la suma cubra la cuota completa, cambiar a PAGADA

**Importante:** Diseñar lógica para acumular pagos parciales de diferentes meses.

### 9. Tolerancia de Redondeo

```java
// Considerar 1 centavo de tolerancia por redondeos
final double TOLERANCIA = 1.0;

if (Math.abs(totalDescontado - montoDescontar) <= TOLERANCIA) {
    // Considerar como pago completo
}
```

---

## ✅ VALIDACIONES IMPLEMENTADAS

### En Fase 1 (Carga)

1. ✅ Archivo tiene extensión .txt
2. ✅ Archivo no está vacío
3. ✅ Contenido inicia con "EP"
4. ✅ Se procesan registros válidos
5. ✅ Se agrupan por producto
6. ✅ Entidades se buscan por código Petro
7. ✅ Si no se encuentra por código, buscar por nombre
8. ✅ Si se encuentra por nombre, actualizar código Petro
9. ✅ Detectar duplicados de código Petro
10. ✅ Detectar duplicados de nombre
11. ✅ Validar coincidencia nombre-código
12. ✅ Validar novedades financieras (descuentos incompletos)
13. ✅ Almacenamiento transaccional
14. ✅ Archivo físico se guarda en: aportes/{año}/{mes}/

### Pendientes en Fase 2 (Procesamiento)

- ⚠️ Validar que exista el producto con codigoPetro
- ⚠️ Clasificar registros en aportes vs préstamos
- ⚠️ Buscar préstamo correspondiente
- ⚠️ Validar que solo haya 1 préstamo activo
- ⚠️ Buscar cuota del mes correspondiente
- ⚠️ Validar montos contra cuota esperada
- ⚠️ Determinar estado correcto de la cuota
- ⚠️ Crear registro de pago
- ⚠️ Actualizar saldos de cuota
- ⚠️ Actualizar estado de cuota
- ⚠️ Actualizar totales del préstamo
- ⚠️ Actualizar estado del préstamo
- ⚠️ Generar aportes cuando aplique
- ⚠️ Manejar pagos parciales acumulativos
- ⚠️ Generar reporte de procesamiento
- ⚠️ Marcar carga como procesada

---

## 📚 CASOS DE USO Y ESCENARIOS

### Caso 1: Pago Completo de Cuota

**Datos del archivo:**
- montoDescontar: $500.00
- totalDescontado: $500.00
- capitalDescontado: $400.00
- interesDescontado: $100.00
- seguroDescontado: $50.00

**Datos de la cuota:**
- capital: $400.00
- interes: $100.00
- desgravamen: $50.00
- cuota: $550.00

**Procesamiento:**
1. Buscar préstamo y cuota ✓
2. Comparar: $500.00 >= $500.00 → PAGO COMPLETO
3. Crear PagoPrestamo con valor $500.00
4. Actualizar cuota:
   - estado = PAGADA
   - capitalPagado = $400.00
   - interesPagado = $100.00
   - saldoCapital = 0
   - saldoInteres = 0
   - fechaPagado = 31/12/2024
5. Actualizar préstamo:
   - totalPagado += $500.00
   - Recalcular saldoTotal
   - Si todas pagadas → CANCELADO

---

### Caso 2: Pago Parcial de Cuota

**Datos del archivo:**
- montoDescontar: $500.00
- totalDescontado: $300.00
- capitalDescontado: $250.00
- interesDescontado: $50.00
- capitalNoDescontado: $150.00
- interesNoDescontado: $50.00

**Datos de la cuota:**
- capital: $400.00
- interes: $100.00
- cuota: $500.00

**Procesamiento:**
1. Buscar préstamo y cuota ✓
2. Comparar: $300.00 > 0 Y $300.00 < $500.00 → PAGO PARCIAL
3. Crear PagoPrestamo con valor $300.00
4. Actualizar cuota:
   - estado = PARCIAL
   - capitalPagado = $250.00
   - interesPagado = $50.00
   - saldoCapital = $400.00 - $250.00 = $150.00
   - saldoInteres = $100.00 - $50.00 = $50.00
5. Actualizar préstamo:
   - totalPagado += $300.00
   - estadoPrestamo = EN_MORA (porque tiene cuota parcial vencida)

**Mes siguiente:** Si viene otro pago de $200.00, sumar y completar la cuota.

---

### Caso 3: Sin Pago (Mora)

**Datos del archivo:**
- montoDescontar: $500.00
- totalDescontado: $0.00
- capitalNoDescontado: $400.00
- interesNoDescontado: $100.00

**Datos de la cuota:**
- capital: $400.00
- interes: $100.00
- fechaVencimiento: 31/12/2024

**Procesamiento:**
1. Buscar préstamo y cuota ✓
2. Comparar: $0.00 = 0 → SIN PAGO
3. Verificar fecha: 31/12/2024 < Hoy → VENCIDA
4. NO crear PagoPrestamo
5. Actualizar cuota:
   - estado = EN_MORA
   - (No cambiar saldos)
6. Actualizar préstamo:
   - estadoPrestamo = EN_MORA

---

### Caso 4: Aporte Mensual

**Datos del archivo:**
- codigoPetroProducto: "0210" (código de APORTE MENSUAL)
- codigoPetro: 12345
- nombre: "PEREZ JUAN"
- montoDescontar: $50.00
- totalDescontado: $50.00

**Procesamiento:**
1. Buscar Producto con codigoPetro = "0210"
2. Verificar: Producto.tipoPrestamo = "APORTE" → ES APORTE
3. Buscar Entidad con rolPetroComercial = 12345
4. Crear Aporte:
   - entidad = Entidad encontrada
   - tipoAporte = según Producto
   - fechaTransaccion = 31/12/2024
   - valor = $50.00
   - valorPagado = $50.00
   - saldo = 0
   - glosa = "Aporte 12/2024 - Petrocomercial"
5. Guardar Aporte
6. Marcar ParticipeXCargaArchivo como procesado

---

### Caso 5: Entidad No Encontrada

**Datos del archivo:**
- codigoPetro: 99999 (no existe en BD)
- nombre: "GONZALEZ MARIA"

**Procesamiento en Fase 1:**
1. Buscar por código 99999 → NO ENCONTRADO
2. Buscar por nombre "GONZALEZ MARIA" (35 chars)
3. Resultados:
   - 0 resultados → novedadesCarga = PARTICIPE_NO_ENCONTRADO
   - 1 resultado → Actualizar Entidad.rolPetroComercial = 99999, novedadesCarga = OK
   - >1 resultados → novedadesCarga = NOMBRE_ENTIDAD_DUPLICADO

**Procesamiento en Fase 2:**
- Si novedadesCarga != OK → OMITIR, no procesar
- Reporte: "Partícipe no encontrado, requiere revisión manual"

---

### Caso 6: Múltiples Préstamos Activos

**Escenario:**
Una entidad tiene 2 préstamos QUIROGRAFARIOS activos simultáneamente.

**Procesamiento:**
1. Buscar préstamos con:
   - entidad.rolPetroComercial = 12345
   - producto.codigoPetro = "0102" (QUIROGRAFARIO)
   - estadoPrestamo IN (VIGENTE, EN_MORA)
2. Resultado: 2 préstamos encontrados → ERROR
3. Marcar error: MULTIPLES_PRESTAMOS_ACTIVOS
4. NO procesar el pago
5. Reporte: "Entidad con múltiples préstamos activos del mismo producto, requiere revisión manual"

**Solución manual:**
- Revisar préstamos
- Cancelar uno si corresponde
- O asociar manualmente el pago al préstamo correcto

---

## 📁 ARCHIVOS Y COMPONENTES DEL SISTEMA

### Modelos (package: com.saa.model.crd)

| Archivo | Tabla | Descripción |
|---------|-------|-------------|
| `CargaArchivo.java` | CRAR | Registro de carga mensual |
| `DetalleCargaArchivo.java` | DTCA | Detalle por producto |
| `ParticipeXCargaArchivo.java` | PXCA | Registros individuales del archivo |
| `Entidad.java` | ENTD | Partícipes (personas/empresas) |
| `Producto.java` | PRDC | Catálogo de productos (préstamos/aportes) |
| `Prestamo.java` | PRST | Préstamos otorgados |
| `DetallePrestamo.java` | DTPR | Cuotas de préstamos |
| `PagoPrestamo.java` | PGPR | Pagos realizados a cuotas |
| `Aporte.java` | APRT | Aportes de partícipes |
| `TipoPrestamo.java` | TPPR | Tipos de préstamo |
| `TipoAporte.java` | TPAP | Tipos de aporte |

### Servicios (package: com.saa.ejb.asoprep / com.saa.ejb.crd)

| Archivo | Responsabilidad |
|---------|-----------------|
| `CargaArchivoPetroService.java` | Interface del servicio principal |
| `CargaArchivoPetroServiceImpl.java` | Implementación FASE 1 (carga y validación) |
| `CargaArchivoService.java` | CRUD de CargaArchivo |
| `DetalleCargaArchivoService.java` | CRUD de DetalleCargaArchivo |
| `ParticipeXCargaArchivoService.java` | CRUD de ParticipeXCargaArchivo |
| `PrestamoService.java` | Lógica de préstamos |
| `DetallePrestamoService.java` | Lógica de cuotas |
| `PagoPrestamoService.java` | Lógica de pagos |
| `AporteService.java` | Lógica de aportes |

### DAOs (package: com.saa.ejb.crd.dao)

| Archivo | Responsabilidad |
|---------|-----------------|
| `CargaArchivoDaoService.java` | Acceso a datos de CargaArchivo |
| `DetalleCargaArchivoDaoService.java` | Acceso a datos de DetalleCargaArchivo |
| `ParticipeXCargaArchivoDaoService.java` | Acceso a datos de ParticipeXCargaArchivo |
| `EntidadDaoService.java` | Acceso a datos de Entidad |
| `PrestamoDaoService.java` | Acceso a datos de Prestamo |
| `DetallePrestamoDaoService.java` | Acceso a datos de DetallePrestamo |
| `PagoPrestamoDaoService.java` | Acceso a datos de PagoPrestamo |
| `AporteDaoService.java` | Acceso a datos de Aporte |

### REST APIs (package: com.saa.ws.rest)

| Archivo | Endpoints |
|---------|-----------|
| `AsoprepGenerales.java` | POST /asoprep/uploadFile (FASE 1) |
| `CargaArchivoRest.java` | CRUD de CargaArchivo |
| `DetalleCargaArchivoRest.java` | CRUD de DetalleCargaArchivo |
| `ParticipeXCargaArchivoRest.java` | CRUD de ParticipeXCargaArchivo |
| `PrestamoRest.java` | Operaciones de préstamos |
| `AporteRest.java` | Operaciones de aportes |

### Rubros (package: com.saa.rubros)

| Archivo | Constantes |
|---------|------------|
| `EstadoPrestamo.java` | Estados de préstamo |
| `EstadoCuotaPrestamo.java` | Estados de cuota |
| `ASPNovedadesCargaArchivo.java` | Códigos de novedades de carga |
| `ASPEstadoRevisionParticipeCarga.java` | Estados de revisión |
| `ASPEstadoCargaArchivoPetro.java` | Estados de carga |
| `Estado.java` | Estados generales (ACTIVO/INACTIVO) |

### Métodos Clave del Servicio Principal

**CargaArchivoPetroServiceImpl.java:**

```java
// FASE 1 - YA IMPLEMENTADA
public CargaArchivo validarArchivoPetro(
    InputStream archivoInputStream, 
    String fileName, 
    CargaArchivo cargaArchivo
) throws Throwable

// Métodos auxiliares FASE 1
private String leerContenidoArchivo(InputStream inputStream)
private List<ParticipeXCargaArchivo> procesarContenido(String contenido)
private Map<String, DetalleCargaArchivo> agruparPorAporte(List<ParticipeXCargaArchivo>)
private CargaArchivo almacenaRegistros(...)
private String cargarArchivo(InputStream, String, CargaArchivo)

// FASE 2 - POR IMPLEMENTAR
public void procesarCargaPetro(Long idCargaArchivo) throws Throwable {
    // 1. Obtener CargaArchivo y sus registros
    // 2. Filtrar solo registros con novedadesCarga = OK
    // 3. Para cada registro:
    //    - Obtener Producto por codigoPetro
    //    - Si es APORTE → generarAporte()
    //    - Si es PRÉSTAMO → procesarPagoPrestamo()
    // 4. Actualizar estado de la carga
    // 5. Generar reporte
}

private void generarAporte(ParticipeXCargaArchivo participe) {
    // Crear registro en tabla Aporte
}

private void procesarPagoPrestamo(ParticipeXCargaArchivo participe) {
    // 1. Buscar préstamo
    // 2. Buscar cuota del mes
    // 3. Determinar estado según pagos
    // 4. Crear PagoPrestamo si hay pago
    // 5. Actualizar DetallePrestamo
    // 6. Actualizar Prestamo
}
```

---

## 🎯 PRÓXIMOS PASOS PARA IMPLEMENTACIÓN FASE 2

### 1. Crear Nuevos Rubros de Error

Archivo: `ASPNovedadesCargaArchivo.java`

Agregar constantes:
```java
public static final int PRESTAMO_NO_ENCONTRADO = 8;
public static final int MULTIPLES_PRESTAMOS_ACTIVOS = 9;
public static final int CUOTA_NO_ENCONTRADA = 10;
public static final int MONTO_INCONSISTENTE = 11;
public static final int PRODUCTO_NO_MAPEADO = 12;
public static final int PRESTAMO_PROCESADO_OK = 13;
public static final int APORTE_GENERADO_OK = 14;
```

### 2. Agregar Campos a ParticipeXCargaArchivo

```java
// Para tracking de procesamiento
@Column(name = "PXCAIDPG") // ID del pago generado
private Long idPagoGenerado;

@Column(name = "PXCAIDCT") // ID de la cuota procesada
private Long idCuotaProcesada;

@Column(name = "PXCAIDAP") // ID del aporte generado
private Long idAporteGenerado;

@Column(name = "PXCAFCPR") // Fecha de procesamiento
private LocalDateTime fechaProcesamiento;

@Column(name = "PXCAPRCS") // Flag: ya procesado
private Boolean procesado;
```

### 3. Agregar Método en ProductoDaoService

```java
public interface ProductoDaoService {
    /**
     * Busca un producto por su código Petro
     */
    Producto selectByCodigoPetro(String codigoPetro) throws Throwable;
}
```

### 4. Agregar Métodos en PrestamoDaoService

```java
public interface PrestamoDaoService {
    /**
     * Busca préstamos activos de una entidad para un producto específico
     */
    List<Prestamo> selectByEntidadYProductoActivos(
        Long codigoEntidad, 
        Long codigoProducto
    ) throws Throwable;
}
```

### 5. Agregar Métodos en DetallePrestamoDaoService

```java
public interface DetallePrestamoDaoService {
    /**
     * Busca la cuota de un préstamo en un mes/año específico
     */
    List<DetallePrestamo> selectByPrestamoYMesAnio(
        Long codigoPrestamo,
        Integer mes,
        Integer anio
    ) throws Throwable;
}
```

### 6. Crear Servicio de Procesamiento

Archivo nuevo: `ProcesoCargaPetroService.java`

```java
@Stateless
public class ProcesoCargaPetroServiceImpl {
    
    @EJB
    private CargaArchivoService cargaArchivoService;
    
    @EJB
    private ParticipeXCargaArchivoService participeService;
    
    @EJB
    private ProductoDaoService productoDaoService;
    
    @EJB
    private PrestamoService prestamoService;
    
    @EJB
    private DetallePrestamoService detallePrestamoService;
    
    @EJB
    private PagoPrestamoService pagoPrestamoService;
    
    @EJB
    private AporteService aporteService;
    
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public ResultadoProcesamiento procesarCargaPetro(Long idCargaArchivo) {
        // Implementar lógica completa de FASE 2
    }
}
```

### 7. Crear Endpoint REST para Procesamiento

Archivo: `AsoprepGenerales.java`

```java
@POST
@Path("/procesarCargaPetro/{idCargaArchivo}")
@Produces(MediaType.APPLICATION_JSON)
public Response procesarCargaPetro(@PathParam("idCargaArchivo") Long idCargaArchivo) {
    try {
        ResultadoProcesamiento resultado = procesoCargaService.procesarCargaPetro(idCargaArchivo);
        return Response.ok(resultado).build();
    } catch (Exception e) {
        return Response.serverError().entity(e.getMessage()).build();
    }
}
```

### 8. Crear DTO de Resultado

```java
public class ResultadoProcesamiento {
    private Long idCargaArchivo;
    private Integer totalRegistros;
    private Integer registrosProcesados;
    private Integer aportes Generated;
    private Integer pagosAplicados;
    private Integer cuotasActualizadas;
    private Integer errores;
    private List<ErrorProcesamiento> listaErrores;
    private Map<String, Integer> erroresPorTipo;
    
    // Getters y setters
}
```

---

## 🔍 CONSULTAS SQL ÚTILES

### Ver resumen de una carga

```sql
SELECT 
    ca.codigo,
    ca.nombre,
    ca.mesAfectacion,
    ca.anioAfectacion,
    ca.fechaCarga,
    COUNT(DISTINCT dca.codigo) as productos,
    COUNT(pxca.codigo) as participes,
    ca.totalDescontado,
    ca.estado
FROM CRAR ca
LEFT JOIN DTCA dca ON dca.CRARCDGO = ca.codigo
LEFT JOIN PXCA pxca ON pxca.DTCACDGO = dca.codigo
WHERE ca.codigo = ?
GROUP BY ca.codigo;
```

### Ver registros con novedades

```sql
SELECT 
    pxca.codigo,
    pxca.codigoPetro,
    pxca.nombre,
    dca.nombreProductoPetro,
    pxca.novedadesCarga,
    pxca.novedadesFinancieras
FROM PXCA pxca
JOIN DTCA dca ON pxca.DTCACDGO = dca.codigo
JOIN CRAR ca ON dca.CRARCDGO = ca.codigo
WHERE ca.codigo = ?
  AND (pxca.novedadesCarga != 1 OR pxca.novedadesFinancieras IS NOT NULL)
ORDER BY pxca.novedadesCarga;
```

### Buscar préstamos de una entidad

```sql
SELECT 
    p.codigo,
    e.razonSocial,
    pr.nombre as producto,
    pr.codigoPetro,
    p.montoSolicitado,
    p.plazo,
    p.estadoPrestamo,
    p.saldoTotal
FROM PRST p
JOIN ENTD e ON p.ENTDCDGO = e.codigo
JOIN PRDC pr ON p.PRDCCDGO = pr.codigo
WHERE e.rolPetroComercial = ?
  AND p.estadoPrestamo IN (2, 11); -- VIGENTE o EN_MORA
```

### Ver cuotas de un préstamo en un mes/año

```sql
SELECT 
    dp.codigo,
    dp.numeroCuota,
    dp.fechaVencimiento,
    dp.capital,
    dp.interes,
    dp.cuota,
    dp.estado,
    dp.capitalPagado,
    dp.interesPagado,
    dp.saldoCapital
FROM DTPR dp
WHERE dp.PRSTCDGO = ?
  AND MONTH(dp.fechaVencimiento) = ?
  AND YEAR(dp.fechaVencimiento) = ?
ORDER BY dp.numeroCuota;
```

---

## 📝 NOTAS FINALES

### Puntos Críticos a Considerar

1. **Transaccionalidad:** Todo el procesamiento de FASE 2 debe ser transaccional. Si falla cualquier paso, hacer rollback completo.

2. **Idempotencia:** El procesamiento debe poder ejecutarse múltiples veces sin duplicar datos. Validar si ya existe un pago para esa cuota en ese mes/año.

3. **Concurrencia:** Si múltiples usuarios procesan cargas simultáneamente, asegurar que no haya conflictos en las actualizaciones de préstamos/cuotas.

4. **Auditoría:** Registrar en logs cada paso del procesamiento para debugging y auditoría.

5. **Performance:** Para cargas con miles de registros, considerar procesamiento en lotes y optimización de consultas.

6. **Validación de Fechas:** Los pagos siempre deben registrarse al último día del mes/año de afectación de la carga.

7. **Manejo de Moneda:** Todos los cálculos deben manejar precisión decimal correctamente (usar BigDecimal si es necesario).

8. **Testing:** Crear casos de prueba exhaustivos antes de implementar en producción.

### Orden de Implementación Recomendado

1. ✅ Crear rubros de error adicionales
2. ✅ Agregar campos de tracking a ParticipeXCargaArchivo
3. ✅ Implementar queries en DAOs (Producto, Prestamo, DetallePrestamo)
4. ✅ Crear servicio de procesamiento con lógica básica
5. ✅ Implementar generación de aportes
6. ✅ Implementar procesamiento de pagos a préstamos
7. ✅ Implementar actualización de estados de cuotas
8. ✅ Implementar actualización de estados de préstamos
9. ✅ Crear endpoint REST
10. ✅ Testing exhaustivo con datos reales
11. ✅ Implementar reporte de resultados
12. ✅ Documentar API para el frontend

---

**DOCUMENTO CREADO:** 2026-03-23  
**VERSIÓN:** 1.0  
**ESTADO IMPLEMENTACIÓN:** FASE 1 COMPLETA / FASE 2 PENDIENTE  
**PRÓXIMA REVISIÓN:** Al iniciar implementación de FASE 2

---

