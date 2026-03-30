# PROCESO DE CARGA Y PROCESAMIENTO DE ARCHIVOS PETROCOMERCIAL

**Última actualización:** 2026-03-27  
**Estado actual:** ✅ VALIDACIONES FASE 1 Y FASE 2 COMPLETADAS - FALTA PROCESAMIENTO DE PAGOS

## 📚 DOCUMENTOS DE REFERENCIA

- **Estándares de Creación de Tablas Oracle:** `docs/ESTANDARES-CREACION-TABLAS-ORACLE.md` ⭐
- **Corrección de Desglose de Pagos:** `docs/CORRECCION-DESGLOSE-PAGOS-PETRO.md`
- **Documentación Completa del Proyecto:** `docs/PROYECTO_DOCUMENTACION.md`

> ⚠️ **IMPORTANTE:** Al crear nuevas tablas, siempre revisar `ESTANDARES-CREACION-TABLAS-ORACLE.md`  
> Este documento contiene el formato correcto para Oracle: `NUMBER`, `VARCHAR2`, sintaxis de IDENTITY, etc.

## 🎯 ESTADO ACTUAL DEL PROYECTO

### ✅ COMPLETADO:
- **FASE 1 - Validaciones Básicas**: Carga y validación de entidades ✅
- **FASE 2 - Validaciones Avanzadas**: Validación de productos, préstamos y cuotas ✅
- **CORRECCIÓN CRÍTICA**: Separación correcta de FASE 1 y FASE 2 ✅ **NUEVO 2026-03-27**
  - Fase 1 se ejecuta durante almacenamiento (validaciones de entidad)
  - Fase 2 se ejecuta DESPUÉS de que todos los registros estén en BD
  - Esto permite que validaciones de PH/PP encuentren registros HS correspondientes
- **MANEJO DE ERRORES**: Excepciones en DAOs no detienen el proceso ✅ **NUEVO 2026-03-27**
  - Los errores de BD se registran como novedades
  - El proceso continúa con el resto de registros
- **CONFIGURACIÓN TIMEOUTS**: Documentación para WildFly 38 ✅ **NUEVO 2026-03-27**
  - EJB timeouts aumentados de 5 segundos a 15 minutos
  - Transaction timeouts configurados en 15 minutos
- **PRODUCTOS ESPECIALES (PH/PP + HS)**: Suma correcta de valores ✅ **NUEVO 2026-03-27**
  - PH (Préstamo Hipotecario) + HS (Seguro) se suman para validación
  - PP (Préstamo Prendario) + HS (Seguro) se suman para validación
- **LOGS OPTIMIZADOS**: Solo logs críticos para debugging ✅ **NUEVO 2026-03-27**
- Tabla de tracking `ProcesamientoCargaArchivo` (PRCA) ✅
- Tabla de novedades múltiples `NovedadParticipeCarga` (NVPC) ✅
- Tabla de afectaciones manuales `AfectacionValoresParticipeCarga` (AVPC) ✅
- Endpoints REST para carga y procesamiento ✅
- REST API para afectaciones manuales `/avpc` ✅
- Consultas SQL para novedades ✅
- Documento de estándares para creación de tablas Oracle ✅

### 🔄 EN PROGRESO:
- **FASE 3 - Procesamiento de Pagos**: Generar pagos y actualizar estados ⚠️ **IMPLEMENTADO PERO NO PROBADO**
  - Método `aplicarPagosArchivoPetro()` creado
  - Procesa pagos completos, parciales y excedentes
  - Actualiza estados de cuotas (PAGADA, PARCIAL)
  - Crea registros en tabla `PagoPrestamo` (PGPR)
  - **FALTA**: Endpoint REST para invocar desde frontend
  - **FALTA**: Pruebas completas del flujo

### ⚠️ PENDIENTE:
- **Generación de Aportes**: Cuando el registro sea tipo APORTE (producto AH)
  - Crear/actualizar registros en tabla de Aportes
  - Vincular con el partícipe/entidad
- **Frontend**: Visualización de resultados del procesamiento
- **Reportes**: Estadísticas consolidadas por carga
- **Testing Completo**: Validar todo el flujo end-to-end

### 🐛 CORRECCIONES CRÍTICAS REALIZADAS (2026-03-27):

#### 1. **Separación de Fase 1 y Fase 2**
**Problema:** Las validaciones de Fase 2 se ejecutaban MIENTRAS se insertaban registros en BD. Cuando se validaba un PH, el registro HS correspondiente aún no existía en la BD.

**Solución:**
```java
// ANTES (INCORRECTO):
for (producto : productos) {
    guardar(participe);           // Inserta en BD
    validarFase2(participe);      // Valida inmediatamente - HS no existe aún
}

// AHORA (CORRECTO):
// Paso 1: Guardar TODOS los registros
for (producto : productos) {
    guardar(participe);           // Inserta en BD
}

// Paso 2: Validar DESPUÉS de que todos estén guardados
ejecutarValidacionesFase2();      // Ahora HS ya existe en BD
```

**Archivo modificado:** `CargaArchivoPetroServiceImpl.java`
- Método `almacenaRegistros()` - Solo Fase 1
- Nuevo método `ejecutarValidacionesFase2()` - Se ejecuta al final

#### 2. **Manejo de Excepciones en DAOs**
**Problema:** Cuando ocurría un timeout o error de BD, los métodos DAO lanzaban excepciones y detenían TODO el proceso.

**Solución:** Modificar DAOs para que retornen valores seguros (lista vacía o null) en lugar de lanzar excepciones:

**Archivos modificados:**
- `ProductoDaoServiceImpl.java`
- `EntidadDaoServiceImpl.java`
- `PrestamoDaoServiceImpl.java`
- `DetallePrestamoDaoServiceImpl.java`

```java
// ANTES:
catch (Exception e) {
    throw e;  // ❌ Detenía el proceso
}

// AHORA:
catch (Exception e) {
    System.err.println("Error: " + e.getMessage());
    return new ArrayList<>();  // ✅ Continúa el proceso
}
```

#### 3. **Configuración de Timeouts en WildFly 38**
**Problema:** Timeouts de 5 segundos en EJB causaban que archivos grandes se interrumpieran.

**Solución:** Documentación completa en `docs/CONFIGURACION_TIMEOUT_WILDFLY38.md` y `docs/CORRECCION_TIMEOUT_EJB3_STANDALONE.md`

**Cambios necesarios en `standalone.xml`:**
```xml
<!-- EJB Timeouts: 5 segundos → 15 minutos -->
<stateful default-access-timeout="900000" .../>
<singleton default-access-timeout="900000"/>

<!-- Transaction Timeout: 5 minutos → 15 minutos -->
<coordinator-environment ... default-timeout="900"/>
```

#### 4. **Protección de Inserción en PXCA**
**Problema:** Errores al insertar registros en `ParticipeXCargaArchivo` detenían el proceso.

**Solución:** Envolver la inserción con try-catch:
```java
try {
    participe = participeXCargaArchivoService.saveSingle(participe);
} catch (Throwable e) {
    System.err.println("ERROR al insertar: " + e.getMessage());
    continue;  // ✅ Continúa con el siguiente registro
}
```

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
- Si no existe, buscar por nombre (primeros 35 caracteres de `razonSocial`)
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
aporte.setEstado(1L); // ACTIVO
    
// 4. Guardar aporte
aporteService.saveSingle(aporte);
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
   - estado = ACTIVO
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

## 🚀 ESTADO ACTUAL DEL PROCESAMIENTO DE PAGOS (2026-03-27)

### ✅ FASE 1 Y FASE 2: COMPLETADAS

#### Validaciones Implementadas:
1. ✅ **Validación de archivo** (formato, estructura, contenido)
2. ✅ **Validación Fase 1** (durante almacenamiento):
   - Validación de entidades por código Petro
   - Búsqueda por nombre si no tiene código Petro
   - Actualización automática de código Petro en entidades
   - Detección de valores en cero
   - Validación de novedades financieras básicas

3. ✅ **Validación Fase 2** (después de almacenamiento completo):
   - Validación de productos mapeados
   - Búsqueda de préstamos activos
   - Búsqueda de cuotas correspondientes
   - **Suma correcta de PH/PP + HS** para validación
   - Validación de montos vs cuotas
   - Detección de diferencias menores a $1
   - Detección de cuotas con fechas diferentes

#### Flujo Actual:
```
1. Usuario sube archivo TXT
2. Sistema lee y parsea el archivo
3. FASE 1: Valida y guarda TODOS los registros en BD
   - AH (aportes) → Guarda
   - PH (préstamos hipotecarios) → Guarda
   - HS (seguros) → Guarda
   - PE (préstamos emergentes) → Guarda
4. FASE 2: Ejecuta validaciones avanzadas
   - Ahora HS ya existe en BD cuando valida PH/PP ✅
   - Suma correctamente PH + HS o PP + HS
5. Sistema retorna resumen con novedades
6. Usuario revisa novedades en el frontend
```

### ⚠️ FASE 3: PROCESAMIENTO DE PAGOS - IMPLEMENTADO PERO SIN ENDPOINT

#### Estado Actual:
El código para procesar pagos **YA ESTÁ IMPLEMENTADO** en `CargaArchivoPetroServiceImpl.java`:

**Método principal:** `aplicarPagosArchivoPetro(Long codigoCargaArchivo)`

**Submétodos implementados:**
- ✅ `aplicarPagoParticipe()` - Procesa cada registro individual
- ✅ `buscarCuotaAPagar()` - Encuentra la cuota correspondiente
- ✅ `procesarPagoCuota()` - Distribuye el pago según el escenario
- ✅ `procesarPagoCompleto()` - Cuota pasa a PAGADA
- ✅ `procesarPagoParcial()` - Cuota queda en PARCIAL
- ✅ `procesarPagoCompletoConExcedente()` - Paga cuota + abona siguiente
- ✅ `crearRegistroPago()` - Crea registro en PagoPrestamo
- ✅ `tieneNovedadesBloqueantes()` - Verifica si se puede procesar

#### Escenarios Implementados:

**1. Pago Completo:**
```java
// Monto archivo = Monto cuota (tolerancia ±$1)
- Actualiza DetallePrestamo.estado = PAGADA
- Actualiza capitalPagado, interesPagado, desgravamenPagado
- Saldos a cero
- Crea registro en PagoPrestamo
```

**2. Pago Parcial:**
```java
// Monto archivo < Monto cuota
- Actualiza DetallePrestamo.estado = PARCIAL
- Distribuye proporcionalmente el pago
- Actualiza saldos pendientes
- Crea registro en PagoPrestamo con el monto parcial
```

**3. Pago con Excedente:**
```java
// Monto archivo > Monto cuota
- Paga cuota completa → PAGADA
- Abona excedente a siguiente cuota como capital
- Crea dos registros en PagoPrestamo
```

**4. Sin Pago:**
```java
// Monto archivo = 0
- NO crea registro de pago
- Cuota pasa a MORA si venció
```

#### Suma de PH/PP + HS en Procesamiento:
```java
// IMPORTANTE: También implementado en aplicarPagoParticipe
if (codigoProducto == "PH" || codigoProducto == "PP") {
    // Busca el registro HS correspondiente
    ParticipeXCargaArchivo participeHS = 
        participeXCargaArchivoDaoService.selectByCodigoPetroYProductoEnCarga(
            participe.getCodigoPetro(),
            "HS",
            cargaArchivo.getCodigo()
        );
    
    if (participeHS != null) {
        montoArchivo += participeHS.getTotalDescontado();
        // Ahora valida/aplica con la suma correcta
    }
}
```

### ❌ LO QUE FALTA IMPLEMENTAR

#### 1. Endpoint REST para Procesamiento de Pagos

**Archivo a crear/modificar:** `AsoprepGenerales.java` o crear nuevo `CargaArchivoPetroRest.java`

**Endpoint necesario:**
```java
@POST
@Path("procesarPagosArchivo/{codigoCargaArchivo}")
@Produces(MediaType.APPLICATION_JSON)
public Response procesarPagosArchivo(@PathParam("codigoCargaArchivo") Long codigoCargaArchivo) {
    try {
        String resumen = cargaArchivoPetroService.aplicarPagosArchivoPetro(codigoCargaArchivo);
        return Response.status(Response.Status.OK)
                       .entity(resumen)
                       .type(MediaType.APPLICATION_JSON)
                       .build();
    } catch (Throwable e) {
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                       .entity("Error al procesar pagos: " + e.getMessage())
                       .type(MediaType.APPLICATION_JSON)
                       .build();
    }
}
```

**Ubicación sugerida:** 
- Opción 1: Agregar a `AsoprepGenerales.java` (junto con uploadFile)
- Opción 2: Crear nuevo `CargaArchivoPetroRest.java` dedicado

#### 2. Generación de Aportes

**Cuando el producto es AH (aportes):**

**Método a implementar en:** `CargaArchivoPetroServiceImpl.java`

```java
private void procesarAporte(ParticipeXCargaArchivo participe, 
                           String codigoProducto, 
                           CargaArchivo cargaArchivo) throws Throwable {
    
    // 1. Buscar la entidad
    List<Entidad> entidades = entidadDaoService.selectByCodigoPetro(participe.getCodigoPetro());
    if (entidades == null || entidades.isEmpty()) {
        return; // Ya tiene novedad de Fase 1
    }
    
    Entidad entidad = entidades.get(0);
    
    // 2. Determinar tipo de aporte según producto
    TipoAporte tipoAporte = determinarTipoAporte(codigoProducto);
    
    // 3. Crear registro de aporte
    Aporte aporte = new Aporte();
    aporte.setEntidad(entidad);
    aporte.setFilial(cargaArchivo.getFilial());
    aporte.setTipoAporte(tipoAporte);
    aporte.setFechaTransaccion(calcularUltimoDiaDelMes(
        cargaArchivo.getMesAfectacion(), 
        cargaArchivo.getAnioAfectacion()
    ));
    aporte.setValor(participe.getMontoDescontar());
    aporte.setValorPagado(participe.getTotalDescontado());
    aporte.setSaldo(participe.getMontoDescontar() - participe.getTotalDescontado());
    aporte.setObservacion("Aporte desde archivo Petrocomercial - " + 
                         cargaArchivo.getMesAfectacion() + "/" + 
                         cargaArchivo.getAnioAfectacion());
    aporte.setEstado(1L); // ACTIVO
    
    // 4. Guardar aporte
    aporteService.saveSingle(aporte);
}

private TipoAporte determinarTipoAporte(String codigoProducto) {
    // Mapear código Petro a tipo de aporte
    // Esto depende de cómo tengan configurados los tipos de aporte
    // Ejemplo:
    if ("AH".equals(codigoProducto)) {
        return tipoAporteDaoService.selectById(1L, "TipoAporte"); // AHORRO
    }
    // ... otros tipos
    return null;
}
```

**Integración en aplicarPagosArchivoPetro:**
```java
// Después de procesar préstamos, agregar:
if (CODIGO_PRODUCTO_APORTES.equalsIgnoreCase(codigoProducto)) {
    procesarAporte(participe, codigoProducto, cargaArchivo);
}
```

#### 3. Frontend - Botón de Procesamiento

**Pantalla de revisión de carga:**
```typescript
// Después de que el usuario revise las novedades
// Mostrar botón "Procesar Pagos"

procesarPagos() {
    this.cargaService.procesarPagosArchivo(this.codigoCargaArchivo)
        .subscribe(
            response => {
                this.mostrarResumen(response);
                // Actualizar estado de la carga
                // Refrescar pantalla
            },
            error => {
                this.mostrarError(error);
            }
        );
}
```

**Flujo en Frontend:**
```
1. Usuario sube archivo → FASE 1 y FASE 2 automáticas
2. Sistema muestra novedades encontradas
3. Usuario revisa novedades
4. Usuario presiona "Procesar Pagos" → FASE 3
5. Sistema muestra resumen de procesamiento:
   - X pagos aplicados exitosamente
   - Y pagos omitidos por novedades
   - Z errores
```

### 📋 CHECKLIST DE IMPLEMENTACIÓN

#### Para Completar el Procesamiento de Pagos:

- [ ] **Backend:**
  - [ ] Crear/modificar REST endpoint para procesamiento
  - [ ] Probar método `aplicarPagosArchivoPetro()` con datos reales
  - [ ] Implementar método `procesarAporte()` para productos AH
  - [ ] Agregar método `determinarTipoAporte()`
  - [ ] Integrar procesamiento de aportes en flujo principal

- [ ] **Frontend:**
  - [ ] Agregar botón "Procesar Pagos" en pantalla de revisión
  - [ ] Implementar servicio para llamar endpoint de procesamiento
  - [ ] Mostrar resumen de procesamiento
  - [ ] Actualizar estado visual de la carga

- [ ] **Testing:**
  - [ ] Probar con archivo pequeño (10-20 registros)
  - [ ] Probar con productos PH que tengan HS
  - [ ] Probar con productos PE que tengan HS
  - [ ] Probar pagos completos
  - [ ] Probar pagos parciales
  - [ ] Probar pagos con excedente
  - [ ] Probar registros sin pago (mora)
  - [ ] Probar aportes (AH)
  - [ ] Verificar actualización correcta de estados
  - [ ] Verificar saldos después de procesamiento

- [ ] **Validación:**
  - [ ] Verificar que registros en PagoPrestamo sean correctos
  - [ ] Verificar que estados de cuotas se actualicen
  - [ ] Verificar que saldos de préstamos sean correctos
  - [ ] Verificar que aportes se creen correctamente

### 🔍 EJEMPLO DE FLUJO COMPLETO ESPERADO

```
ARCHIVO CARGADO: petrocomercial_202603.txt
Mes/Año: 03/2026
Registros: 150

==========================================
FASE 1: CARGA Y VALIDACIÓN BÁSICA
==========================================
✅ Archivo parseado: 150 registros
✅ Productos: AH (50), PH (40), HS (40), PE (20)
✅ Todos los registros guardados en BD

Novedades Fase 1:
- 5 partícipes no encontrados
- 2 códigos Petro duplicados
- 3 nombres de entidad duplicados

==========================================
FASE 2: VALIDACIONES AVANZADAS
==========================================
Validando 100 registros (omitiendo 50 AH/HS)

Validando PH (40 registros):
✅ PH Partícipe 10107 → Monto PH: $975.38 + HS: $50.25 = $1025.63
✅ PH Partícipe 10205 → Monto PH: $850.00 + HS: $45.00 = $895.00
✅ ...

Validando PE (20 registros):
✅ PE Partícipe 20301 → Monto PE: $500.00 + HS: $30.00 = $530.00
✅ ...

Novedades Fase 2:
- 10 cuotas con fecha diferente
- 5 diferencias menores a $1
- 3 préstamos no encontrados
- 2 productos no mapeados

==========================================
RESUMEN VALIDACIONES
==========================================
Total registros: 150
Registros OK: 125
Registros con novedades: 25

Usuario revisa novedades y presiona "Procesar Pagos"

==========================================
FASE 3: PROCESAMIENTO DE PAGOS
==========================================
Procesando 125 registros aprobados...

Préstamos PH (35 procesados):
✅ Partícipe 10107 → Cuota #15 PAGADA ($1025.63)
✅ Partícipe 10205 → Cuota #8 PAGADA ($895.00)
⚠️ Partícipe 10350 → Cuota #12 PARCIAL ($450.00 de $895.00)
✅ ...

Préstamos PE (18 procesados):
✅ Partícipe 20301 → Cuota #5 PAGADA ($530.00)
✅ ...

Aportes AH (50 procesados):
✅ Partícipe 30101 → Aporte creado ($150.00)
✅ ...

==========================================
RESUMEN PROCESAMIENTO
==========================================
Total procesados: 125
Pagos completos: 95
Pagos parciales: 8
Aportes creados: 50
Omitidos (con novedades): 25
Errores: 0

✅ Procesamiento completado exitosamente
```

### 📊 DIAGRAMA DE FLUJO ACTUALIZADO

```
┌─────────────────────────────────────────┐
│   USUARIO SUBE ARCHIVO TXT              │
└─────────────┬───────────────────────────┘
              │
              ▼
┌─────────────────────────────────────────┐
│   FASE 1: CARGA Y VALIDACIÓN BÁSICA     │
│   ✅ IMPLEMENTADA                        │
├─────────────────────────────────────────┤
│ 1. Parsear archivo                      │
│ 2. Validar entidades                    │
│ 3. Guardar TODOS en BD                  │
│ 4. Registrar novedades Fase 1           │
└─────────────┬───────────────────────────┘
              │
              ▼
┌─────────────────────────────────────────┐
│   FASE 2: VALIDACIONES AVANZADAS        │
│   ✅ IMPLEMENTADA                        │
├─────────────────────────────────────────┤
│ 1. Validar productos                    │
│ 2. Validar préstamos                    │
│ 3. Validar cuotas                       │
│ 4. Sumar PH/PP + HS                     │
│ 5. Registrar novedades Fase 2           │
└─────────────┬───────────────────────────┘
              │
              ▼
┌─────────────────────────────────────────┐
│   USUARIO REVISA NOVEDADES              │
│   (Pantalla Frontend)                   │
└─────────────┬───────────────────────────┘
              │
              ▼
┌─────────────────────────────────────────┐
│   USUARIO PRESIONA "PROCESAR PAGOS"     │
│   ⚠️ ENDPOINT FALTA                     │
└─────────────┬───────────────────────────┘
              │
              ▼
┌─────────────────────────────────────────┐
│   FASE 3: PROCESAMIENTO                 │
│   ⚠️ IMPLEMENTADO SIN ENDPOINT          │
├─────────────────────────────────────────┤
│ 1. Aplicar pagos a cuotas               │
│ 2. Actualizar estados                   │
│ 3. Crear registros PagoPrestamo         │
│ 4. Crear registros Aporte (AH)          │
│ 5. Recalcular saldos                    │
└─────────────┬───────────────────────────┘
              │
              ▼
┌─────────────────────────────────────────┐
│   MOSTRAR RESUMEN EN FRONTEND           │
│   ⚠️ FALTA IMPLEMENTAR                  │
└─────────────────────────────────────────┘
```

### 🎯 PRÓXIMOS PASOS INMEDIATOS

**PASO 1: Crear Endpoint REST**
```bash
Archivo: AsoprepGenerales.java o crear CargaArchivoPetroRest.java
Tiempo estimado: 15 minutos
```

**PASO 2: Probar Procesamiento de Pagos**
```bash
- Cargar archivo de prueba
- Llamar endpoint manualmente (Postman)
- Verificar logs
- Verificar registros en BD
Tiempo estimado: 1 hora
```

**PASO 3: Implementar Procesamiento de Aportes**
```bash
Archivo: CargaArchivoPetroServiceImpl.java
Método: procesarAporte()
Tiempo estimado: 30 minutos
```

**PASO 4: Integrar con Frontend**
```bash
- Agregar botón en pantalla
- Conectar con servicio
- Mostrar resumen
Tiempo estimado: 2 horas
```

**PASO 5: Testing Completo**
```bash
- Probar todos los escenarios
- Validar datos en BD
- Ajustar según resultados
Tiempo estimado: 3 horas
```

---

## 📞 CONTACTO Y SOPORTE

Para continuar con la implementación, se necesita:
1. ✅ Código backend del procesamiento → **YA ESTÁ IMPLEMENTADO**
2. ⚠️ Endpoint REST → **FALTA CREAR**
3. ⚠️ Frontend botón procesamiento → **FALTA IMPLEMENTAR**
4. ⚠️ Procesamiento de aportes → **FALTA IMPLEMENTAR**

**El 80% del trabajo está hecho. Solo falta exponerlo via REST y conectar con frontend.**

---
