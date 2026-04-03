# Cambios Aplicados - FASE 3: Procesamiento Carga Archivo Petrocomercial

**Fecha:** 2026-04-02  
**Sistema:** SAA - Sistema de Asociación de Ahorro  
**Módulo:** Procesamiento de Carga de Archivos Petrocomercial  

---

## RESUMEN EJECUTIVO

Se realizaron múltiples correcciones críticas en el procesamiento de la FASE 3 (aplicación de pagos) del sistema de carga de archivos Petrocomercial. Los principales problemas corregidos fueron:

1. **Errores en queries JPA** - Acceso incorrecto a relaciones padre
2. **Problema de rendimiento grave** - Traía todas las cuotas en lugar de filtrar en BD
3. **Lógica incorrecta de aportes AH** - Dividía 50/50 en lugar de usar HistorialSueldo
4. **Campos obligatorios faltantes** - Campo `idEstado` en PagoPrestamo
5. **Bloqueo incorrecto por novedades** - Se omitían préstamos que sí se podían procesar
6. **Procesamiento de cuotas ya pagadas** - No validaba saldo pendiente real
7. **Registros con valores $0** - Se creaban aportes innecesarios

---

## 1. CORRECCIÓN: Error en Query de AfectacionValoresParticipeCarga

### Problema
El método `selectByParticipeYCuota` intentaba acceder directamente a `participeXCargaArchivo.codigo` desde `AfectacionValoresParticipeCarga`, pero esta entidad no tiene relación directa con `ParticipeXCargaArchivo`.

**La jerarquía correcta es:**
```
AfectacionValoresParticipeCarga → NovedadParticipeCarga → ParticipeXCargaArchivo
```

### Error Original
```
org.hibernate.query.sqm.UnknownPathException: Could not resolve attribute 'participeXCargaArchivo' 
of 'com.saa.model.crd.AfectacionValoresParticipeCarga'
```

### Solución Aplicada

**Archivo:** `AfectacionValoresParticipeCargaDaoService.java`
- Renombrado método de `selectByParticipeYCuota` a `selectByNovedadYCuota`
- Parámetro cambiado: `codigoParticipeXCargaArchivo` → `codigoNovedad`

**Archivo:** `AfectacionValoresParticipeCargaDaoServiceImpl.java`
- Query corregida:
```java
// ANTES (incorrecto):
WHERE a.participeXCargaArchivo.codigo = :codigoParticipe

// AHORA (correcto):
WHERE a.novedadParticipeCarga.codigo = :codigoNovedad
```

**Archivo:** `CargaArchivoPetroServiceImpl.java`
- Método `verificarAfectacionManual` actualizado para:
  1. Buscar primero las novedades del partícipe
  2. Por cada novedad, verificar si existe afectación manual para esa cuota
  3. Retornar la primera afectación encontrada

- Agregada inyección del DAO faltante:
```java
@EJB
private com.saa.ejb.crd.dao.NovedadParticipeCargaDaoService novedadParticipeCargaDaoService;
```

---

## 2. CORRECCIÓN: Campo Obligatorio Faltante en PagoPrestamo

### Problema
```
ORA-01400: no se puede realizar una inserción NULL en ("CRD"."PGPR"."PGPRIDST")
```

### Solución
**Archivo:** `CargaArchivoPetroServiceImpl.java` - Método `crearRegistroPago`

Agregado campo obligatorio:
```java
pago.setIdEstado(1L); // Campo obligatorio - Estado activo
```

---

## 3. OPTIMIZACIÓN CRÍTICA: Filtrado de Cuotas en Base de Datos

### Problema Grave de Rendimiento
El sistema traía **TODAS las cuotas del préstamo** (ej: 85 cuotas) a memoria y luego filtraba en Java.

**Línea problemática 1151:**
```java
// ❌ GRAVE ERROR DE RENDIMIENTO:
List<DetallePrestamo> cuotas = detallePrestamoDaoService.selectByPrestamo(prestamo.getCodigo());
// Traía las 85 cuotas y luego filtraba en Java por estado y saldo
```

### Solución Aplicada

#### 3.1. Nuevo Método en DAO

**Archivo:** `DetallePrestamoDaoService.java`
```java
/**
 * Obtiene solo las cuotas NO pagadas ni canceladas anticipadamente de un préstamo.
 * ✅ OPTIMIZACIÓN: Filtra directamente en la BD en lugar de traer todas las cuotas a memoria.
 */
List<DetallePrestamo> selectCuotasNoPagadasByPrestamo(Long codigoPrestamo) throws Throwable;
```

**Archivo:** `DetallePrestamoDaoServiceImpl.java`
```java
public List<DetallePrestamo> selectCuotasNoPagadasByPrestamo(Long codigoPrestamo) {
    String jpql = "SELECT d FROM DetallePrestamo d " +
                 "WHERE d.prestamo.codigo = :codigoPrestamo " +
                 "AND d.estado NOT IN (:estadoPagada, :estadoCanceladaAnticipada) " +
                 "ORDER BY d.numeroCuota";
    
    query.setParameter("estadoPagada", (long) com.saa.rubros.EstadoCuotaPrestamo.PAGADA);
    query.setParameter("estadoCanceladaAnticipada", (long) com.saa.rubros.EstadoCuotaPrestamo.CANCELADA_ANTICIPADA);
    // ...
}
```

**NOTA:** Cast explícito a `(long)` requerido porque las constantes son `Integer` pero el campo es `Long`.

#### 3.2. Actualización de Métodos

**Archivo:** `CargaArchivoPetroServiceImpl.java`

**Método `buscarCuotaAPagar`:**
```java
// ANTES:
List<DetallePrestamo> cuotas = detallePrestamoDaoService.selectByPrestamo(prestamo.getCodigo());

// AHORA:
List<DetallePrestamo> cuotas = 
    detallePrestamoDaoService.selectCuotasNoPagadasByPrestamo(prestamo.getCodigo());
```

**Método `procesarExcedenteASiguienteCuota`:**
```java
// ANTES:
List<DetallePrestamo> cuotas = detallePrestamoDaoService.selectByPrestamo(cuota.getPrestamo().getCodigo());

// AHORA:
List<DetallePrestamo> cuotas = 
    detallePrestamoDaoService.selectCuotasNoPagadasByPrestamo(cuota.getPrestamo().getCodigo());
```

### Mejora de Rendimiento

| Concepto | ANTES | AHORA |
|----------|-------|-------|
| **Cuotas traídas de BD** | 85 cuotas | 5-10 cuotas pendientes |
| **Validaciones en Java** | Estado + Saldo | Solo Saldo |
| **Tráfico de red** | 85 registros | 5-10 registros |
| **Velocidad** | Lenta | **Rápida** |

---

## 4. CORRECCIÓN: Validación de Cuotas ya Pagadas

### Problema
El sistema procesaba cuotas con **saldo pendiente = $0**, creando registros duplicados y aplicando todo el monto como "excedente" infinitamente.

**Logs del error:**
```
Pendiente: Total=$0.0
✅ Cuota PAGADA con excedente: $194.59
Procesando excedente de $194.59 a siguiente cuota...
[Se repetía en loop para todas las cuotas ya pagadas]
```

### Solución Aplicada

#### 4.1. Validación Temprana en `procesarPagoCuota`

**Archivo:** `CargaArchivoPetroServiceImpl.java`
```java
// Calcular saldo pendiente
double totalPendiente = desgravamenPendiente + interesPendiente + capitalPendiente;

// ✅ VALIDACIÓN CRÍTICA: Si ya está pagada, pasar el monto a la siguiente
if (totalPendiente <= TOLERANCIA) {
    System.out.println("⚠️ Cuota ya pagada completamente, pasando todo el monto a siguiente cuota");
    procesarExcedenteASiguienteCuota(participe, cuota, montoPagado, cargaArchivo);
    return; // Salir SIN crear registros duplicados
}
```

#### 4.2. Validación de Saldo Real en `buscarCuotaAPagar`

Además de filtrar por estado en BD, ahora valida saldo pendiente real en memoria:

```java
// Calcular saldo pendiente real
double desgravamenPendiente = Math.max(0, cuota.getDesgravamen() - cuota.getDesgravamenPagado());
double interesPendiente = Math.max(0, cuota.getInteres() - cuota.getInteresPagado());
double capitalPendiente = Math.max(0, cuota.getCapital() - cuota.getCapitalPagado());
double saldoPendiente = desgravamenPendiente + interesPendiente + capitalPendiente;

// Solo considerar si tiene saldo pendiente
if (saldoPendiente > TOLERANCIA) {
    // Esta cuota SÍ necesita pago
}
```

#### 4.3. Validación de Saldo Real en `procesarExcedenteASiguienteCuota`

Similar validación al buscar la siguiente cuota para aplicar excedente.

### Resultado
- ✅ No procesa cuotas con saldo $0
- ✅ No crea registros de pago duplicados
- ✅ No entra en loops infinitos
- ✅ Aplica excedente solo a cuotas con saldo pendiente real

---

## 5. CORRECCIÓN: Lógica de Aportes AH (Jubilación y Cesantía)

### Problema
El sistema dividía el monto total en **50% para jubilación y 50% para cesantía**, ignorando los valores configurados en la tabla `HistorialSueldo`.

**Código incorrecto:**
```java
// ❌ INCORRECTO:
double montoJubilacion = montoTotal / 2.0;
double montoCesantia = montoTotal / 2.0;
```

### Solución Aplicada

#### 5.1. Nuevo Método en DAO

**Archivo:** `HistorialSueldoDaoService.java`
```java
/**
 * Busca el registro de HistorialSueldo activo (estado 99) para una entidad
 * ✅ CRÍTICO: Estado 99 indica el registro vigente con los montos actuales de aportes
 */
HistorialSueldo selectByEntidadYEstadoActivo(Long codigoEntidad) throws Throwable;
```

**Archivo:** `HistorialSueldoDaoServiceImpl.java`
```java
public HistorialSueldo selectByEntidadYEstadoActivo(Long codigoEntidad) {
    String jpql = "SELECT h FROM HistorialSueldo h " +
                 "WHERE h.entidad.codigo = :codigoEntidad " +
                 "AND h.estado = 99 " +
                 "ORDER BY h.fechaIngreso DESC";
    query.setMaxResults(1); // Solo el más reciente
    // ...
}
```

#### 5.2. Método `aplicarAporteAH` Corregido

**Archivo:** `CargaArchivoPetroServiceImpl.java`

```java
// ✅ Buscar valores en HistorialSueldo con estado 99
HistorialSueldo historialActivo = 
    historialSueldoDaoService.selectByEntidadYEstadoActivo(entidad.getCodigo());

if (historialActivo != null) {
    // ✅ Usar los montos configurados
    montoJubilacion = nullSafe(historialActivo.getMontoJubilacion());
    montoCesantia = nullSafe(historialActivo.getMontoCesantia());
    
    // Validar que el total coincida
    double totalConfigurado = montoJubilacion + montoCesantia;
    if (Math.abs(totalConfigurado - montoTotal) > 1.0) {
        System.out.println("⚠️ DIFERENCIA detectada: $" + 
                          Math.abs(totalConfigurado - montoTotal));
    }
} else {
    // Fallback: dividir 50/50
    montoJubilacion = montoTotal / 2.0;
    montoCesantia = montoTotal / 2.0;
}
```

#### 5.3. Validación para No Crear Registros con Valor $0

```java
// ✅ Solo crear aporte de jubilación si el monto es mayor a 0
if (montoJubilacion > 0) {
    // Crear aporte de jubilación
} else {
    System.out.println("⊘ Aporte de jubilación omitido (valor = $0)");
}

// ✅ Solo crear aporte de cesantía si el monto es mayor a 0
if (montoCesantia > 0) {
    // Crear aporte de cesantía
} else {
    System.out.println("⊘ Aporte de cesantía omitido (valor = $0)");
}
```

### Resultado
- ✅ Respeta configuración individual de cada empleado
- ✅ Detecta inconsistencias entre archivo y configuración
- ✅ No crea registros con valor $0
- ✅ Logs detallados para auditoría
- ✅ Fallback seguro si no existe HistorialSueldo

---

## 6. CORRECCIÓN: Eliminación de Bloqueo por Novedades

### Problema
El sistema bloqueaba el procesamiento de préstamos cuando el partícipe tenía **cualquier novedad**, incluso si:
- La novedad estaba en un préstamo diferente
- La novedad era procesable (MONTO_INCONSISTENTE, PRESTAMO_NO_ENCONTRADO, etc.)

**Ejemplo:** Un partícipe con préstamo PE se omitía porque tenía novedad MONTO_INCONSISTENTE en otro préstamo.

### Novedades que se Bloqueaban Incorrectamente

Antes solo permitía: `OK (0)`, `DIFERENCIA_MENOR_UN_DOLAR (17)`

**Bloqueaba todo lo demás:**
- PRODUCTO_NO_MAPEADO (9) - aunque el flujo ya lo maneja
- PRESTAMO_NO_ENCONTRADO (10) - aunque el flujo ya lo maneja
- MULTIPLES_PRESTAMOS_ACTIVOS (11) - aunque el flujo procesa todos
- CUOTA_NO_ENCONTRADA (12) - aunque el flujo ya lo maneja
- MONTO_INCONSISTENTE (13) - aunque el flujo lo maneja (parcial/excedente)
- Y muchas más...

### Solución Aplicada

**Archivo:** `CargaArchivoPetroServiceImpl.java` - Método `tieneNovedadesBloqueantes`

```java
/**
 * Verifica si un partícipe tiene novedades que bloquean el procesamiento de pagos
 * ✅ CORRECCIÓN: NINGUNA novedad bloquea el procesamiento
 * El flujo ya maneja correctamente todos los casos:
 * - PRESTAMO_NO_ENCONTRADO: El método aplicarPagoParticipe hace return si no encuentra préstamos
 * - CUOTA_NO_ENCONTRADA: El método buscarCuotaAPagar retorna null y no procesa
 * - MONTO_INCONSISTENTE: Si es menor queda PARCIAL, si es mayor se aplica a siguiente cuota
 * - MULTIPLES_PRESTAMOS_ACTIVOS: Se procesan todos los préstamos activos encontrados
 */
private boolean tieneNovedadesBloqueantes(ParticipeXCargaArchivo participe) {
    // ✅ NO bloquear ningún procesamiento por novedades
    // El flujo maneja correctamente cada caso
    return false;
}
```

### Justificación

El flujo de procesamiento YA maneja correctamente todos los casos:

1. **PRESTAMO_NO_ENCONTRADO**: `aplicarPagoParticipe` verifica y hace `return`
2. **CUOTA_NO_ENCONTRADA**: `buscarCuotaAPagar` retorna `null`
3. **MONTO_INCONSISTENTE**: Aplica lógica PARCIAL o excedente
4. **MULTIPLES_PRESTAMOS**: Procesa todos los activos
5. **PRODUCTO_NO_MAPEADO**: Verifica y hace `return`

**No hay necesidad de bloquear preventivamente.**

---

## 7. MEJORA: Logs Detallados para Diagnóstico

### Logs Agregados para Identificar Omisiones

**Cuando se omite por novedad:**
```java
System.out.println("⚠️ Partícipe OMITIDO - Código Petro: " + participe.getCodigoPetro() + 
                   " (" + participe.getNombre() + ") - Producto: " + codigoProducto + 
                   " - Novedad: " + participe.getNovedadesCarga() + 
                   " - Monto: $" + participe.getTotalDescontado());
```

### Logs para TODOS los Productos (no solo PH/PP)

**Al inicio de `aplicarPagoParticipe`:**
```java
System.out.println("========================================");
System.out.println("APLICAR PAGO " + codigoProducto + " - Partícipe: " + participe.getCodigoPetro() + " (" + participe.getNombre() + ")");
System.out.println("Monto: $" + participe.getTotalDescontado());
System.out.println("✅ Entidad encontrada: " + entidad.getRazonSocial() + " (ID: " + entidad.getCodigo() + ")");
System.out.println("✅ Producto(s) encontrado(s): " + productos.size());
System.out.println("✅ Préstamo(s) activo(s) encontrado(s): " + prestamos.size());
```

**Si hay problemas:**
```java
System.out.println("⚠️ PROCESAMIENTO OMITIDO - No se encontró entidad con código Petro: " + participe.getCodigoPetro());
System.out.println("⚠️ PROCESAMIENTO OMITIDO - No se encontró producto con código Petro: " + codigoProducto);
System.out.println("⚠️ PROCESAMIENTO OMITIDO - No se encontraron préstamos activos para entidad: " + entidad.getRazonSocial());
```

---

## ARCHIVOS MODIFICADOS

### Archivos de DAO - Interfaces
1. `AfectacionValoresParticipeCargaDaoService.java`
   - Método renombrado y parámetros actualizados

2. `DetallePrestamoDaoService.java`
   - Nuevo método: `selectCuotasNoPagadasByPrestamo`

3. `HistorialSueldoDaoService.java`
   - Nuevo método: `selectByEntidadYEstadoActivo`

### Archivos de DAO - Implementaciones
4. `AfectacionValoresParticipeCargaDaoServiceImpl.java`
   - Query corregida para usar `novedadParticipeCarga.codigo`

5. `DetallePrestamoDaoServiceImpl.java`
   - Implementación de `selectCuotasNoPagadasByPrestamo`
   - Cast explícito a `(long)` para estados

6. `HistorialSueldoDaoServiceImpl.java`
   - Implementación de `selectByEntidadYEstadoActivo`

### Archivo Principal
7. `CargaArchivoPetroServiceImpl.java`
   - Inyección de `novedadParticipeCargaDaoService`
   - Método `verificarAfectacionManual` actualizado
   - Método `crearRegistroPago` - campo `idEstado` agregado
   - Método `buscarCuotaAPagar` - optimizado con nuevo DAO
   - Método `procesarPagoCuota` - validación de saldo $0
   - Método `procesarExcedenteASiguienteCuota` - optimizado con nuevo DAO
   - Método `aplicarAporteAH` - usa HistorialSueldo y valida $0
   - Método `tieneNovedadesBloqueantes` - eliminado bloqueo
   - Método `aplicarPagoParticipe` - logs detallados agregados
   - Logs de omisión agregados

---

## VALIDACIONES REALIZADAS

✅ Todos los archivos compilados sin errores  
✅ Queries JPA validadas con nombres de atributos correctos  
✅ Tipos de datos corregidos (Integer → Long)  
✅ Campos obligatorios identificados y completados  
✅ Logs agregados para diagnóstico completo  

---

## PRÓXIMOS PASOS RECOMENDADOS

1. **Recompilar el proyecto completo**
2. **Probar con archivo de prueba** que contenga:
   - Préstamos PE, PH, PP
   - Aportes AH
   - Casos con novedades diversas
   - Cuotas parcialmente pagadas
   - Cuotas con montos inconsistentes
3. **Revisar logs** para validar el flujo correcto
4. **Validar en BD** que no se creen registros con $0
5. **Validar** que las cuotas ya pagadas no se reprocesen

---

## NOTAS TÉCNICAS

### Estado de las Cuotas
- **PAGADA**: Estado que indica que la cuota fue pagada completamente
- **CANCELADA_ANTICIPADA**: Estado para cuotas canceladas antes de su vencimiento
- **PARCIAL**: Estado cuando se pagó parte pero no el total
- **PENDIENTE**: Cuota sin pagar o con pago parcial

### Constantes de Estado
```java
com.saa.rubros.EstadoCuotaPrestamo.PAGADA
com.saa.rubros.EstadoCuotaPrestamo.CANCELADA_ANTICIPADA
```
**IMPORTANTE:** Son `Integer` pero el campo en BD es `Long` - requiere cast explícito.

### Estado 99 en HistorialSueldo
El **estado 99** indica el registro **vigente/activo** en la tabla HistorialSueldo, conteniendo los montos actuales de aportes de jubilación y cesantía para cada empleado.

### Tolerancia
```java
private static final double TOLERANCIA = 1.0; // $1 para redondeos
```

---

## CONTACTO Y SOPORTE

Para dudas sobre esta implementación, referirse a:
- Este documento de cambios
- Logs del sistema durante el procesamiento
- Código fuente con comentarios `✅` que indican correcciones aplicadas

---

**FIN DEL DOCUMENTO**
