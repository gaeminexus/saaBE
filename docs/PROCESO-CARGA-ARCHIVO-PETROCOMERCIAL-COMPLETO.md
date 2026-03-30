# PROCESO DE CARGA Y PROCESAMIENTO DE ARCHIVOS PETROCOMERCIAL

**Última actualización:** 2026-03-30  
**Estado actual:** ✅ VALIDACIONES COMPLETADAS - ✅ APLICACIÓN DE PAGOS IMPLEMENTADA

## 📚 DOCUMENTOS DE REFERENCIA

- **Estándares de Creación de Tablas Oracle:** `docs/ESTANDARES-CREACION-TABLAS-ORACLE.md` ⭐
- **Corrección de Desglose de Pagos:** `docs/CORRECCION-DESGLOSE-PAGOS-PETRO.md`
- **Consultas de Novedades:** `docs/CONSULTAS-NOVEDADES-PETROCOMERCIAL.sql`
- **Configuración Timeouts WildFly:** `docs/CONFIGURACION_TIMEOUT_WILDFLY38.md`

---

## 🎯 ESTADO ACTUAL DEL PROYECTO

### ✅ COMPLETADO:

#### **FASE 1 - Validaciones Básicas y Carga** ✅
- Lectura y parseo del archivo TXT con codificación ISO-8859-1
- Validación de formato del archivo (encabezados EP, estructura)
- Validación de entidades (búsqueda por código Petro y nombre)
- Actualización automática de códigos Petro cuando se encuentra por nombre
- Detección de duplicados (códigos y nombres)
- Almacenamiento en tablas: CargaArchivo, DetalleCargaArchivo, ParticipeXCargaArchivo
- Carga del archivo físico en carpeta estructurada (aportes/año/mes)

#### **FASE 2 - Validaciones Avanzadas** ✅
- Validación de productos (mapeo con codigoPetro)
- Validación de préstamos activos
- Validación de cuotas pendientes
- Búsqueda de cuotas por mes/año o mínima pendiente
- Validación de montos con tolerancia de $1
- **Manejo especial PH/PP + HS**: Suma correcta de seguros
- Separación correcta: Fase 1 durante inserción, Fase 2 después de todas las inserciones
- Tabla de novedades múltiples: NovedadParticipeCarga (NVPC)

#### **FASE 3 - Aplicación de Pagos** ✅ **(NUEVO 2026-03-30)**
- Verificación de afectaciones manuales (tabla AVPC)
- Aplicación de reglas automáticas de carga
- Orden de afectación: **Desgravamen → Interés → Capital**
- Procesamiento de pagos completos (cuota PAGADA)
- Procesamiento de pagos parciales (cuota PARCIAL)
- Procesamiento de excedentes (aplicación a siguientes cuotas)
- Manejo especial PH/PP sin HS (queda PARCIAL)
- Actualización de estados de cuotas
- Creación de registros en PagoPrestamo
- Pagos acumulativos (se suman a valores previos)
- Endpoint REST: `/api/asgn/aplicarPagosArchivoPetro/{id}`

#### **Infraestructura y Optimizaciones** ✅
- Manejo robusto de errores sin detener el proceso
- Configuración de timeouts (EJB y transacciones: 15 minutos)
- Logs optimizados para debugging
- Tabla de afectaciones manuales: AfectacionValoresParticipeCarga (AVPC)
- API REST completa para todo el flujo

### ⚠️ PENDIENTE:

#### **Validación de Producto AH (Aportes)** 🔄 **(EN PROGRESO)**
- Agregar validaciones específicas para producto AH en Fase 2
- Verificar existencia de registro de aporte para el mes/año
- Validar montos esperados vs recibidos
- *Reglas específicas pendientes de definición*

#### **Generación de Aportes** ⏳
- Crear/actualizar registros en tabla Aportes
- Vincular con entidad correspondiente
- Determinar tipo de aporte (mensual, extraordinario)

#### **Frontend y Reportes** ⏳
- Visualización de resultados de validación
- Interfaz para revisión de novedades
- Panel para afectaciones manuales
- Reportes consolidados por carga

---

## 📋 ENDPOINTS REST DISPONIBLES

### Base Path: `/api/asgn`

#### 1. **Validar Archivo Petro (Fase 1)**
```
POST /api/asgn/validarArchivoPetro
Content-Type: multipart/form-data

Parámetros:
- archivo: InputStream (archivo .txt)
- archivoNombre: String (nombre del archivo)
- cargaArchivo: JSON con { mesAfectacion, anioAfectacion, filial, usuarioCarga }

Retorna: CargaArchivo completo con todos los registros validados
```

#### 2. **Aplicar Pagos del Archivo (Fase 3)** ⭐ **(NUEVO)**
```
POST /api/asgn/aplicarPagosArchivoPetro/{idCargaArchivo}
Content-Type: application/json

Parámetros:
- idCargaArchivo: Long (ID del CargaArchivo validado)

Retorna: FileResponse con resumen:
{
  "success": true,
  "message": "Total procesados: 150\nExitosos: 140\nOmitidos: 5\nErrores: 5",
  "filePath": null
}
```

#### 3. **Actualizar Código Petro de Entidad**
```
GET /api/asgn/actualizaCodigoPetroEntidad/{codigoPetro}/{idParticipeXCarga}/{idEntidad}

Parámetros:
- codigoPetro: Long
- idParticipeXCarga: Long
- idEntidad: Long

Retorna: ParticipeXCargaArchivo actualizado
```

---

## 🔄 FLUJO COMPLETO DEL PROCESO

### **PASO 1: Validación y Carga (Frontend → Backend)**

```javascript
// Frontend envía archivo con datos básicos
const formData = new FormData();
formData.append('archivo', archivoFile);
formData.append('archivoNombre', archivoFile.name);
formData.append('cargaArchivo', JSON.stringify({
  mesAfectacion: 12,
  anioAfectacion: 2024,
  filial: { codigo: 1 },
  usuarioCarga: { codigo: userId }
}));

const response = await fetch('/api/asgn/validarArchivoPetro', {
  method: 'POST',
  body: formData
});

const cargaArchivo = await response.json();
// cargaArchivo.codigo contiene el ID para el siguiente paso
```

**Backend ejecuta:**
1. Validaciones iniciales (formato, extensión, contenido)
2. Parseo del archivo TXT
3. **FASE 1**: Validaciones de entidades (código Petro, nombres)
4. Almacenamiento en BD (CargaArchivo, DetalleCargaArchivo, ParticipeXCargaArchivo)
5. **FASE 2**: Validaciones avanzadas (productos, préstamos, cuotas)
6. Registro de novedades en NovedadParticipeCarga
7. Retorna CargaArchivo con estado de validaciones

### **PASO 2: Revisión de Novedades (Usuario)**

El usuario revisa en la interfaz:
- Registros con novedades en ParticipeXCargaArchivo
- Detalles en NovedadParticipeCarga
- Puede crear afectaciones manuales en AfectacionValoresParticipeCarga si necesario

### **PASO 3: Aplicación de Pagos (Frontend → Backend)** ⭐

```javascript
// Usuario confirma aplicar pagos
const response = await fetch(`/api/asgn/aplicarPagosArchivoPetro/${cargaArchivo.codigo}`, {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' }
});

const resultado = await response.json();
console.log(resultado.message);
// Muestra: Total procesados, Exitosos, Omitidos, Errores
```

**Backend ejecuta:**
1. Busca todos los registros del CargaArchivo
2. Filtra productos válidos (omite AH, HS)
3. Para cada partícipe:
   - Busca préstamo activo de la entidad
   - Identifica cuota a pagar (mes/año o mínima pendiente)
   - **Si es PH/PP**: Busca y valida registro HS (seguro)
   - **Verifica afectación manual** en tabla AVPC
   - **Aplica reglas según monto**:
     * Monto = Esperado → Cuota PAGADA
     * Monto < Esperado → Cuota PARCIAL (orden: Desgravamen→Interés→Capital)
     * Monto > Esperado → Cuota PAGADA + excedente a siguiente cuota
   - Actualiza DetallePrestamo (valores pagados, saldos, estado)
   - Crea registro en PagoPrestamo
4. Retorna resumen con estadísticas

---

## 📊 REGLAS DE APLICACIÓN DE PAGOS

### **Regla 1: Verificar Afectación Manual (Tabla AVPC)**

Si existe un registro en `AfectacionValoresParticipeCarga` para el partícipe/cuota:
- Se aplican los valores exactos indicados en la tabla AVPC
- Se ignoran las reglas automáticas
- Se actualiza la cuota según los valores de afectación manual

### **Regla 2: Aplicación Automática - Orden de Afectación**

**ORDEN ESTRICTO:** Desgravamen → Interés → Capital

```
Ejemplo: Pago parcial de $200

Cuota esperada:
- Desgravamen: $50
- Interés: $100
- Capital: $300
- TOTAL: $450

Aplicación:
1. Desgravamen: $50 (completo) → Queda $150
2. Interés: $100 (completo) → Queda $50
3. Capital: $50 (parcial de $300) → Queda $0

Resultado:
- Desgravamen pagado: $50
- Interés pagado: $100
- Capital pagado: $50
- Estado: PARCIAL
- Saldo capital: $250
- Saldo interés: $0
```

### **Regla 3: Monto Recibido = Monto Esperado**

```java
if (Math.abs(montoPagado - montoCuota) <= TOLERANCIA) {
    // Cuota pasa a PAGADA
    cuota.setCapitalPagado(capital);
    cuota.setInteresPagado(interes);
    cuota.setDesgravamenPagado(desgravamen);
    cuota.setEstado(PAGADA);
    cuota.setFechaPagado(LocalDateTime.now());
    cuota.setSaldoCapital(0.0);
    cuota.setSaldoInteres(0.0);
}
```

### **Regla 4: Monto Recibido < Monto Esperado**

```java
// Distribuir según orden: Desgravamen → Interés → Capital
double montoRestante = montoPagado;

// Paso 1: Pagar desgravamen
if (montoRestante >= desgravamenEsperado) {
    desgravamenPagado = desgravamenEsperado;
    montoRestante -= desgravamenEsperado;
} else {
    desgravamenPagado = montoRestante;
    montoRestante = 0;
}

// Paso 2: Pagar interés
if (montoRestante >= interesEsperado) {
    interesPagado = interesEsperado;
    montoRestante -= interesEsperado;
} else {
    interesPagado = montoRestante;
    montoRestante = 0;
}

// Paso 3: Pagar capital
capitalPagado = Math.min(montoRestante, capitalEsperado);

// Actualizar cuota (valores ACUMULATIVOS)
cuota.setCapitalPagado(pagadoPrevio + capitalPagado);
cuota.setInteresPagado(pagadoPrevio + interesPagado);
cuota.setDesgravamenPagado(pagadoPrevio + desgravamenPagado);
cuota.setEstado(PARCIAL);
```

### **Regla 5: Monto Recibido > Monto Esperado**

```java
// 1. Pagar cuota completa
procesarPagoCompleto(cuota);

// 2. Calcular excedente
double excedente = montoPagado - montoCuota;

// 3. Buscar siguiente cuota pendiente
DetallePrestamo siguienteCuota = buscarSiguienteCuotaPendiente();

// 4. Aplicar excedente a siguiente cuota (orden: Desgravamen→Interés→Capital)
if (siguienteCuota != null) {
    aplicarExcedente(siguienteCuota, excedente);
    
    // 5. Si aún sobra dinero, aplicar recursivamente
    if (excedenteRestante > 0) {
        procesarExcedenteRecursivo(excedenteRestante);
    }
}
```

### **Regla 6: Manejo Especial PH/PP + HS**

Para productos **PH (Préstamo Hipotecario)** y **PP (Préstamo Prendario)**:

```java
// El seguro DEBE venir en un registro separado con código HS
double montoArchivo = participe.getTotalDescontado(); // Monto PH/PP

// Buscar registro HS del mismo partícipe en la misma carga
ParticipeXCargaArchivo participeHS = buscarHS(participe.getCodigoPetro());

if (participeHS != null) {
    double montoHS = participeHS.getTotalDescontado();
    
    // Validar que HS corresponda al desgravamen esperado
    if (Math.abs(montoHS - cuota.getDesgravamen()) <= TOLERANCIA) {
        // ✅ HS válido: Sumar al monto total
        montoArchivo += montoHS;
    } else {
        // ❌ HS no corresponde: Cuota queda PARCIAL
        procesarPagoParcialSinSeguro(cuota, montoArchivo);
        return;
    }
} else {
    // ❌ No se encontró HS: Cuota queda PARCIAL
    procesarPagoParcialSinSeguro(cuota, montoArchivo);
    return;
}

// Aplicar pago normal con el monto total (PH/PP + HS)
procesarPagoCuota(cuota, montoArchivo);
```

### **Regla 7: Pagos Acumulativos**

Los pagos se **SUMAN** a valores previamente pagados:

```java
// INCORRECTO (sobrescribir):
cuota.setCapitalPagado(capitalPagadoAhora);

// CORRECTO (acumular):
double capitalPagadoAnterior = cuota.getCapitalPagado() != null ? 
                                cuota.getCapitalPagado() : 0.0;
cuota.setCapitalPagado(capitalPagadoAnterior + capitalPagadoAhora);
```

Esto permite que una cuota reciba múltiples pagos parciales hasta completarse.

---

## 📁 ARQUITECTURA DE TABLAS

### **Tablas del Cliente (NO MODIFICABLES)**

Contienen información del archivo de PETROCOMERCIAL:

#### **CargaArchivo (CRAR)**
- Registro maestro de cada archivo mensual
- Campos: codigo, nombre, fechaCarga, mesAfectacion, anioAfectacion, usuarioCarga, filial, rutaArchivo
- Totales consolidados de todo el archivo

#### **DetalleCargaArchivo (DTCA)**
- Agrupación por producto/tipo de préstamo
- Campos: codigo, cargaArchivo FK, codigoPetroProducto, nombreProductoPetro
- Totales por producto

#### **ParticipeXCargaArchivo (PXCA)** ⚠️ **CRÍTICO**
- **Cada línea del archivo TXT**
- Campos: codigo, detalleCargaArchivo FK, codigoPetro, nombre, saldoActual, montoDescontar, capitalDescontado, interesDescontado, seguroDescontado, totalDescontado, etc.
- **NO SE PUEDEN AGREGAR CAMPOS** (información fija del cliente)

### **Tablas de Control Interno (MODIFICABLES)**

#### **NovedadParticipeCarga (NVPC)** 🆕
- Tabla HIJA de ParticipeXCargaArchivo
- Permite múltiples novedades por partícipe
- Campos: codigo, participeXCargaArchivo FK, tipoNovedad, descripcion, codigoProducto, codigoPrestamo, montoEsperado, montoRecibido, montoDiferencia
- SQL: `docs/CREATE_TABLE_NVPC_NovedadParticipeCarga.sql`

#### **AfectacionValoresParticipeCarga (AVPC)** 🆕
- Afectaciones manuales cuando hay novedades
- Tabla HIJA de NovedadParticipeCarga
- Campos: codigo, novedadParticipeCarga FK, prestamo FK, detallePrestamo FK, valorAfectar, capitalAfectar, interesAfectar, desgravamenAfectar
- SQL: `docs/CREATE_TABLE_AVPC_AfectacionValoresParticipeCarga.sql`
- REST API: `/avpc`

#### **Prestamo (PRST)**, **DetallePrestamo (DTPR)**, **PagoPrestamo (PGPR)**
- Tablas internas del sistema de crédito
- **SÍ se pueden modificar** según necesidades

---

## 🔍 CÓDIGOS DE NOVEDADES

### **Novedades de Carga (ASPNovedadesCargaArchivo)**

Definidos en: `com.saa.rubros.ASPNovedadesCargaArchivo`

```java
OK = 1                              // ✅ Registro válido
PARTICIPE_NO_ENCONTRADO = 2         // No existe entidad
CODIGO_ROL_DUPLICADO = 3            // Múltiples entidades con ese código
NOMBRE_ENTIDAD_DUPLICADO = 4        // Múltiples entidades con ese nombre
CODIGO_PETRO_NO_COINCIDE_CON_NOMBRE = 5  // Código existe pero nombre no coincide
SIN_DESCUENTOS = 6                  // Hay saldos pero descuento = 0
DESCUENTOS_INCOMPLETOS = 7          // Descuentos parciales
VALORES_CERO = 8                    // Todos los valores son cero
PRODUCTO_NO_MAPEADO = 9             // No existe producto con ese código Petro
PRESTAMO_NO_ENCONTRADO = 10         // No hay préstamo activo
CUOTA_NO_ENCONTRADA = 11            // No hay cuota pendiente
DIFERENCIA_MENOR_UN_DOLAR = 12      // Diferencia < $1 (tolerancia)
CUOTA_FECHA_DIFERENTE = 13          // Cuota de otro mes
MONTO_INCONSISTENTE = 14            // Monto archivo != monto cuota
DESGLOSE_NO_COINCIDE = 15           // Capital/Interés/Seguro no coinciden
```

### **Novedades que NO Bloquean el Pago**

```java
// Estas novedades permiten que el pago se procese:
- OK (1)
- DIFERENCIA_MENOR_UN_DOLAR (12)
- CUOTA_FECHA_DIFERENTE (13)

// Cualquier otra novedad BLOQUEA el procesamiento automático
// Requiere intervención manual (tabla AVPC)
```

---

## 🛠️ ARCHIVOS CLAVE DEL SISTEMA

### **Backend - Java**

#### **Servicios Principales**
- `CargaArchivoPetroServiceImpl.java` - Lógica completa de validación y aplicación
  - `validarArchivoPetro()` - Fase 1 y 2
  - `aplicarPagosArchivoPetro()` - Fase 3
  - `procesarPagoCuota()` - Aplica reglas de pago
  - `procesarPagoParcial()` - Orden de afectación
  - `procesarPagoCompletoConExcedente()` - Manejo de excedentes

#### **REST APIs**
- `AsoprepGenerales.java` - Endpoints principales
  - POST `/validarArchivoPetro` 
  - POST `/aplicarPagosArchivoPetro/{id}` ⭐ **NUEVO**
  - GET `/actualizaCodigoPetroEntidad/{codigoPetro}/{idParticipe}/{idEntidad}`

- `AfectacionValoresParticipeCargaRest.java` - CRUD de afectaciones manuales
  - GET `/avpc/getAll`
  - GET `/avpc/getId/{id}`
  - POST `/avpc` (crear)
  - PUT `/avpc` (actualizar)
  - DELETE `/avpc/{id}`

#### **DAOs Modificados** (manejo robusto de errores)
- `ProductoDaoServiceImpl.java`
- `EntidadDaoServiceImpl.java`
- `PrestamoDaoServiceImpl.java`
- `DetallePrestamoDaoServiceImpl.java`
- `ParticipeXCargaArchivoDaoServiceImpl.java`

### **Base de Datos - Oracle**

#### **Scripts de Creación**
- `CREATE_TABLE_NVPC_NovedadParticipeCarga.sql`
- `CREATE_TABLE_AVPC_AfectacionValoresParticipeCarga.sql`
- `ALTER_TABLE_NVPC_agregar_campos.sql`

#### **Scripts de Consulta**
- `CONSULTAS-NOVEDADES-PETROCOMERCIAL.sql` - Queries para reportes

### **Documentación**
- `PROCESO-CARGA-ARCHIVO-PETROCOMERCIAL-COMPLETO.md` (este archivo)
- `CONFIGURACION_TIMEOUT_WILDFLY38.md` - Configuración de timeouts
- `CORRECCION-DESGLOSE-PAGOS-PETRO.md` - Corrección crítica de valores
- `ESTANDARES-CREACION-TABLAS-ORACLE.md` - Estándares de BD

---

## ⚙️ CONFIGURACIÓN NECESARIA

### **WildFly 38 - Timeouts**

Editar `standalone.xml`:

```xml
<!-- EJB Container -->
<session-bean>
    <stateless>
        <bean-instance-pool-ref pool-name="slsb-strict-max-pool"/>
    </stateless>
    <stateful default-access-timeout="900000" cache-ref="simple" passivation-disabled-cache-ref="simple"/>
    <singleton default-access-timeout="900000"/>
</session-bean>

<!-- Transaction Subsystem -->
<coordinator-environment statistics-enabled="${wildfly.transactions.statistics-enabled:${wildfly.statistics-enabled:false}}" default-timeout="900"/>
```

**Valores configurados:**
- EJB timeout: 900,000 ms = 15 minutos
- Transaction timeout: 900 segundos = 15 minutos

---

## 🧪 EJEMPLO DE USO COMPLETO

### **Escenario: Procesar archivo de diciembre 2024**

```javascript
// ============================================
// PASO 1: VALIDAR ARCHIVO
// ============================================
async function procesarArchivoPetro() {
  try {
    // 1.1 Cargar y validar
    const formData = new FormData();
    formData.append('archivo', archivoFile);
    formData.append('archivoNombre', 'PETROCOMERCIAL_2024_12.txt');
    formData.append('cargaArchivo', JSON.stringify({
      mesAfectacion: 12,
      anioAfectacion: 2024,
      filial: { codigo: 1 },
      usuarioCarga: { codigo: currentUserId }
    }));

    const response = await fetch('/api/asgn/validarArchivoPetro', {
      method: 'POST',
      body: formData
    });

    if (!response.ok) throw new Error('Error en validación');
    
    const cargaArchivo = await response.json();
    console.log('✅ Archivo validado - ID:', cargaArchivo.codigo);

    // ============================================
    // PASO 2: REVISAR NOVEDADES
    // ============================================
    // El usuario revisa en la interfaz:
    // - ParticipeXCargaArchivo con novedadesCarga != 1
    // - NovedadParticipeCarga para detalles
    // Si necesario, crea afectaciones en AVPC

    // ============================================
    // PASO 3: APLICAR PAGOS
    // ============================================
    const confirmar = confirm('¿Desea aplicar los pagos del archivo?');
    if (!confirmar) return;

    const response2 = await fetch(
      `/api/asgn/aplicarPagosArchivoPetro/${cargaArchivo.codigo}`,
      {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' }
      }
    );

    if (!response2.ok) throw new Error('Error al aplicar pagos');
    
    const resultado = await response2.json();
    console.log('✅ Pagos aplicados:');
    console.log(resultado.message);
    
    // Ejemplo de salida:
    // === RESUMEN APLICACIÓN DE PAGOS ===
    // Total procesados: 150
    // Exitosos: 140
    // Omitidos (con novedades): 5
    // Errores: 5
    // ================================

  } catch (error) {
    console.error('❌ Error:', error.message);
    alert('Error: ' + error.message);
  }
}
```

---

## 🔐 CASOS DE USO

### **Caso 1: Pago Completo Exitoso**

**Entrada:**
- Archivo indica: ROL 12345, Producto PE, Total $450
- Sistema tiene: Entidad con ROL 12345, Préstamo activo, Cuota pendiente de $450

**Proceso:**
1. Valida entidad ✅
2. Encuentra préstamo activo ✅
3. Encuentra cuota de diciembre 2024 por $450 ✅
4. Monto archivo ($450) = Monto cuota ($450) ✅

**Resultado:**
- Cuota pasa a estado **PAGADA**
- Se crea PagoPrestamo por $450
- Saldos actualizados a 0
- Novedad: OK (1)

---

### **Caso 2: Pago Parcial con Orden de Afectación**

**Entrada:**
- Archivo indica: Total $250

**Cuota esperada:**
- Desgravamen: $50
- Interés: $150
- Capital: $400
- TOTAL: $600

**Proceso:**
1. Monto archivo ($250) < Monto cuota ($600) → PARCIAL
2. Aplica orden de afectación:
   - Desgravamen: $50 (completo) ✅
   - Interés: $150 (completo) ✅
   - Capital: $50 (parcial de $400) ⚠️

**Resultado:**
- Cuota pasa a estado **PARCIAL**
- DesgravamenPagado: $50
- InteresPagado: $150
- CapitalPagado: $50
- SaldoCapital: $350
- Se crea PagoPrestamo por $250
- Novedad: DESCUENTOS_INCOMPLETOS (7)

---

### **Caso 3: Pago con Excedente**

**Entrada:**
- Archivo indica: Total $800

**Cuota 15:**
- Total: $500

**Cuota 16 (siguiente):**
- Desgravamen: $50
- Interés: $100
- Capital: $300
- Total: $450

**Proceso:**
1. Monto archivo ($800) > Monto cuota 15 ($500) → EXCEDENTE
2. Paga cuota 15 completa: $500 → PAGADA ✅
3. Excedente: $300
4. Aplica excedente a cuota 16 con orden:
   - Desgravamen: $50 (completo) ✅
   - Interés: $100 (completo) ✅
   - Capital: $150 (parcial de $300) ⚠️

**Resultado:**
- Cuota 15: **PAGADA**
- Cuota 16: **PARCIAL**
  - DesgravamenPagado: $50
  - InteresPagado: $100
  - CapitalPagado: $150
  - SaldoCapital: $150
- Se crean 2 registros de PagoPrestamo

---

### **Caso 4: PH sin HS - Queda Parcial**

**Entrada:**
- Archivo indica: Producto PH (Hipotecario), Total $400
- **NO se encuentra registro HS** para el mismo partícipe

**Cuota esperada:**
- Desgravamen: $80 (seguro)
- Interés: $150
- Capital: $270
- Total: $500

**Proceso:**
1. Busca registro HS → NO ENCONTRADO ❌
2. Solo aplica a Interés y Capital (sin Desgravamen):
   - Interés: $150 (completo) ✅
   - Capital: $250 (parcial de $270) ⚠️

**Resultado:**
- Cuota queda **PARCIAL**
- InteresPagado: $150
- CapitalPagado: $250
- DesgravamenPagado: $0 ⚠️ (FALTA)
- SaldoInteres: $0
- SaldoCapital: $20
- Observación: "Pago PARCIAL SIN SEGURO - FALTA DESGRAVAMEN: $80"

---

### **Caso 5: Afectación Manual (AVPC)**

**Entrada:**
- Archivo indica: ROL 12345, Total $500
- Existe **múltiples préstamos activos** → NOVEDAD

**Intervención Manual:**
Usuario crea registro en AVPC indicando:
- Préstamo específico: ID 1001
- Cuota específica: 12
- Capital a afectar: $400
- Interés a afectar: $100

**Proceso:**
1. Detecta afectación manual en AVPC ✅
2. Ignora reglas automáticas
3. Aplica valores exactos de AVPC:
   - Capital: $400
   - Interés: $100

**Resultado:**
- Cuota actualizada según AVPC
- Se crea PagoPrestamo con referencia a AVPC
- Observación: "Pago con afectación manual (AVPC ID: 5)"

---

## 📊 CONSULTAS ÚTILES

### **Ver registros con novedades**

```sql
-- Novedades de carga (Fase 1 y 2)
SELECT 
    pxca.CODIGO,
    pxca.CODIGO_PETRO,
    pxca.NOMBRE,
    pxca.NOVEDADES_CARGA,
    nvpc.TIPO_NOVEDAD,
    nvpc.DESCRIPCION,
    nvpc.MONTO_ESPERADO,
    nvpc.MONTO_RECIBIDO,
    nvpc.MONTO_DIFERENCIA
FROM PXCA pxca
LEFT JOIN NVPC nvpc ON nvpc.PARTICIPE_X_CARGA_ARCHIVO = pxca.CODIGO
WHERE pxca.DETALLE_CARGA_ARCHIVO IN (
    SELECT CODIGO FROM DTCA WHERE CARGA_ARCHIVO = :idCargaArchivo
)
AND pxca.NOVEDADES_CARGA != 1
ORDER BY pxca.CODIGO;
```

### **Ver afectaciones manuales pendientes**

```sql
SELECT 
    avpc.CODIGO,
    nvpc.PARTICIPE_X_CARGA_ARCHIVO,
    avpc.PRESTAMO,
    avpc.DETALLE_PRESTAMO,
    avpc.VALOR_AFECTAR,
    avpc.CAPITAL_AFECTAR,
    avpc.INTERES_AFECTAR,
    avpc.DESGRAVAMEN_AFECTAR,
    avpc.OBSERVACIONES
FROM AVPC avpc
INNER JOIN NVPC nvpc ON nvpc.CODIGO = avpc.NOVEDAD_PARTICIPE_CARGA
WHERE nvpc.CODIGO_CARGA_ARCHIVO = :idCargaArchivo
AND avpc.ESTADO = 1;
```

### **Resumen de aplicación de pagos**

```sql
-- Cuotas actualizadas por la carga
SELECT 
    dtpr.CODIGO AS CODIGO_CUOTA,
    dtpr.PRESTAMO,
    dtpr.NUMERO_CUOTA,
    dtpr.ESTADO,
    dtpr.CAPITAL_PAGADO,
    dtpr.INTERES_PAGADO,
    dtpr.DESGRAVAMEN_PAGADO,
    dtpr.SALDO_CAPITAL,
    dtpr.FECHA_PAGADO
FROM PGPR pgpr
INNER JOIN DTPR dtpr ON dtpr.CODIGO = pgpr.DETALLE_PRESTAMO
WHERE pgpr.TIPO = 'CARGA_ARCHIVO_PETRO'
AND pgpr.OBSERVACION LIKE '%carga archivo ' || :idCargaArchivo || '%';
```

---

## 🚨 TROUBLESHOOTING

### **Problema: Timeout al validar archivo grande**

**Síntoma:** Error después de 5 segundos con archivo de 5000+ líneas

**Solución:**
1. Verificar configuración en `standalone.xml`
2. Confirmar timeouts de EJB y transacciones en 15 minutos
3. Reiniciar WildFly

**Referencia:** `docs/CONFIGURACION_TIMEOUT_WILDFLY38.md`

---

### **Problema: PH/PP no encuentra HS**

**Síntoma:** Cuotas de productos PH/PP quedan como PARCIAL, falta desgravamen

**Causa:** El archivo no incluyó registros HS (seguros) o tiene error en código Petro

**Solución:**
1. Verificar que el archivo incluya registros con producto "HS"
2. Verificar que el código Petro del HS coincida con el del PH/PP
3. Si HS tiene código diferente, corregir en entidad
4. Reprocesar aplicación de pagos

---

### **Problema: Registros omitidos al aplicar pagos**

**Síntoma:** Algunos registros no se procesan, aparecen en "Omitidos"

**Causa:** Tienen novedades que bloquean el procesamiento automático

**Verificar:**
```sql
SELECT * FROM PXCA 
WHERE NOVEDADES_CARGA NOT IN (1, 12, 13)
AND DETALLE_CARGA_ARCHIVO IN (
    SELECT CODIGO FROM DTCA WHERE CARGA_ARCHIVO = :id
);
```

**Solución:**
1. Revisar novedades en tabla NVPC
2. Crear afectaciones manuales en AVPC si necesario
3. Volver a ejecutar aplicación de pagos

---

## 📝 PRÓXIMOS PASOS

### **1. Validaciones de Producto AH (Aportes)** 🔄 *EN PROGRESO*

Agregar en Fase 2:
- Verificar existencia de aporte para el mes/año
- Validar montos esperados vs recibidos
- Reglas específicas de aportes (pendiente definición)

### **2. Generación de Aportes**

Cuando el producto sea AH:
- Crear/actualizar registro en tabla Aportes
- Vincular con entidad
- Determinar tipo de aporte

### **3. Testing Completo**

- Casos de prueba con archivos reales
- Validación end-to-end
- Performance con archivos grandes (10,000+ líneas)

### **4. Frontend**

- Interfaz de revisión de novedades
- Panel de afectaciones manuales
- Reportes consolidados

---

## 📞 CONTACTO Y SOPORTE

Para dudas o problemas, revisar:
1. Este documento completo
2. Archivos en carpeta `docs/`
3. Consultas SQL en `CONSULTAS-NOVEDADES-PETROCOMERCIAL.sql`
4. Logs del servidor WildFly

---

**FIN DEL DOCUMENTO**

