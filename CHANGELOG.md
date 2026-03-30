# Changelog - Sistema SAA Backend

Todos los cambios notables en este proyecto serán documentados en este archivo.

El formato está basado en [Keep a Changelog](https://keepachangelog.com/es-ES/1.0.0/),
y este proyecto adhiere a [Versionado Semántico](https://semver.org/lang/es/).

---

## [1.1.0] - 2026-03-27

### 🎉 Características Nuevas
- ✅ Implementación completa de **Fase 2 de Validaciones Avanzadas** para archivos Petrocomercial
  - Validación de productos mapeados
  - Búsqueda de préstamos activos
  - Búsqueda de cuotas correspondientes
  - Detección de diferencias menores a $1
  - Detección de cuotas con fechas diferentes
  - Tabla de novedades múltiples `NovedadParticipeCarga` (NVPC)

- ✅ **Procesamiento de Pagos (Fase 3)** - Backend completo
  - Método `aplicarPagosArchivoPetro()` implementado
  - Procesamiento de pagos completos, parciales y con excedente
  - Actualización automática de estados de cuotas
  - Creación de registros en `PagoPrestamo`
  - Recalculación de saldos de préstamos

- ✅ **Productos Especiales PH/PP + HS**
  - Suma automática de Préstamo Hipotecario (PH) + Seguro (HS)
  - Suma automática de Préstamo Prendario (PP) + Seguro (HS)
  - Implementado en validaciones y procesamiento

### 🔧 Correcciones Críticas

#### 1. Separación Correcta de Fase 1 y Fase 2
- **Problema:** Fase 2 se ejecutaba mientras se insertaban registros, causando que no se encontraran registros HS cuando se validaban PH/PP
- **Solución:** 
  - Fase 1 ahora solo valida entidades y guarda TODOS los registros
  - Fase 2 se ejecuta DESPUÉS mediante método `ejecutarValidacionesFase2()`
  - Ahora los registros HS existen en BD cuando se validan PH/PP
- **Archivos:** `CargaArchivoPetroServiceImpl.java`

#### 2. Manejo de Excepciones en DAOs
- **Problema:** Errores de BD (timeouts, rollbacks) detenían TODO el proceso
- **Solución:** DAOs retornan valores seguros (lista vacía o null) en lugar de lanzar excepciones
- **Archivos modificados:**
  - `ProductoDaoServiceImpl.java`
  - `EntidadDaoServiceImpl.java`
  - `PrestamoDaoServiceImpl.java`
  - `DetallePrestamoDaoServiceImpl.java`
- **Beneficio:** El proceso continúa con todos los registros, errores se registran como novedades

#### 3. Protección de Inserción en PXCA
- **Problema:** Errores al insertar registros en `ParticipeXCargaArchivo` detenían el proceso
- **Solución:** Try-catch alrededor de la inserción con `continue` para siguiente registro
- **Archivo:** `CargaArchivoPetroServiceImpl.java`

#### 4. Configuración de Timeouts
- **Problema:** Timeouts de 5 segundos en EJB causaban interrupciones en archivos grandes
- **Solución:** Documentación completa para configurar timeouts en WildFly 38
- **Documentos creados:**
  - `docs/CONFIGURACION_TIMEOUT_WILDFLY38.md`
  - `docs/CORRECCION_TIMEOUT_EJB3_STANDALONE.md`
- **Configuración recomendada:**
  - EJB Stateful/Singleton: 900000 ms (15 minutos)
  - Transaction timeout: 900 segundos (15 minutos)

### 🎨 Mejoras

- ✅ **Logs Optimizados**
  - Eliminados logs innecesarios del proceso
  - Solo logs críticos para debugging de PH/PP + HS
  - Logs con formato claro y separadores visuales

- ✅ **Resiliencia del Sistema**
  - Errores de BD no detienen el proceso completo
  - Cada error se registra como novedad individual
  - Proceso continúa con resto de registros

- ✅ **Documentación Completa**
  - `PROCESO-CARGA-ARCHIVO-PETROCOMERCIAL.md` actualizado
  - `CORRECCION_MANEJO_EXCEPCIONES_DAO.md` creado
  - `CONFIGURACION_COMPLETA_TIMEOUTS.md` creado
  - Checklist de implementación para Fase 3

### 📊 Validaciones Implementadas

#### Fase 1 (Durante Almacenamiento):
1. Validación de formato de archivo
2. Búsqueda de entidades por código Petro
3. Búsqueda alternativa por nombre (35 caracteres)
4. Actualización automática de código Petro en entidades
5. Detección de valores en cero
6. Detección de descuentos incompletos

#### Fase 2 (Después de Almacenamiento):
9. Validación de productos mapeados
10. Validación de préstamos activos
11. Búsqueda de cuotas correspondientes
12. Validación de montos vs cuotas
13. Detección de diferencias menores a $1
14. Detección de cuotas con fechas diferentes
15. Suma correcta de PH/PP + HS

### 🏗️ Arquitectura

- **Nuevo Método:** `ejecutarValidacionesFase2()` en `CargaArchivoPetroServiceImpl`
  - Se ejecuta después de almacenar TODOS los registros
  - Valida solo préstamos (omite AH y HS)
  - Muestra estadísticas de validación

- **Métodos de Procesamiento de Pagos:**
  - `aplicarPagosArchivoPetro()` - Método principal
  - `aplicarPagoParticipe()` - Procesa cada registro
  - `buscarCuotaAPagar()` - Encuentra cuota correspondiente
  - `procesarPagoCuota()` - Distribuye el pago
  - `procesarPagoCompleto()` - Cuota → PAGADA
  - `procesarPagoParcial()` - Cuota → PARCIAL
  - `procesarPagoCompletoConExcedente()` - Paga + abona siguiente
  - `crearRegistroPago()` - Crea PagoPrestamo
  - `tieneNovedadesBloqueantes()` - Verifica si procesar

### ⚠️ Pendiente de Implementar

- [ ] Endpoint REST para procesamiento de pagos (Fase 3)
- [ ] Método `procesarAporte()` para productos AH
- [ ] Frontend: Botón "Procesar Pagos"
- [ ] Frontend: Pantalla de resumen de procesamiento
- [ ] Testing completo end-to-end

### 🔗 Documentación

- `docs/PROCESO-CARGA-ARCHIVO-PETROCOMERCIAL.md` - Documentación completa actualizada
- `docs/CORRECCION_MANEJO_EXCEPCIONES_DAO.md` - Detalles de corrección de DAOs
- `docs/CONFIGURACION_TIMEOUT_WILDFLY38.md` - Guía de configuración de timeouts
- `docs/CORRECCION_TIMEOUT_EJB3_STANDALONE.md` - Configuración específica EJB3
- `docs/CONFIGURACION_COMPLETA_TIMEOUTS.md` - Configuración consolidada

### 📈 Estadísticas

- **Archivos modificados:** 8 archivos
- **Nuevos documentos:** 4 documentos técnicos
- **Líneas de código agregadas:** ~500 líneas
- **Correcciones críticas:** 4 correcciones mayores
- **Tiempo estimado pendiente:** ~7 horas para completar Fase 3

---

## [1.0-SNAPSHOT] - 2026-03-24

### Características Iniciales
- Implementación de Fase 1: Carga y validación de archivos Petrocomercial
- Tabla `CargaArchivo` (CRAR)
- Tabla `DetalleCargaArchivo` (DTCA)
- Tabla `ParticipeXCargaArchivo` (PXCA)
- Tabla `NovedadParticipeCarga` (NVPC)
- Tabla `AfectacionValoresParticipeCarga` (AVPC)
- REST API para carga de archivos
- Validaciones básicas de entidades

### Corrección Crítica
- Campo `seguroDescontado` del archivo = DESGRAVAMEN (no seguro de incendio)
- Mapeo correcto en `PagoPrestamo`

---

## Tipos de Cambios

- **Added** (Agregado) - para funcionalidades nuevas
- **Changed** (Cambiado) - para cambios en funcionalidades existentes
- **Deprecated** (Obsoleto) - para funcionalidades que serán removidas
- **Removed** (Removido) - para funcionalidades removidas
- **Fixed** (Corregido) - para corrección de bugs
- **Security** (Seguridad) - para vulnerabilidades

---

**Versión actual:** 1.1.0  
**Última actualización:** 2026-03-27
