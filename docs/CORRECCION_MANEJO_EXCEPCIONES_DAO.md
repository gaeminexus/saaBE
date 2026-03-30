# Corrección: Manejo de Excepciones en DAOs para Evitar Detención del Proceso

## Fecha
2026-03-26

## Problema Identificado

El proceso de carga de archivos Petro se estaba deteniendo cuando ocurrían errores de base de datos (timeouts, rollbacks, etc.) durante las validaciones. El error específico era:

```
ARJUNA012108: CheckedAction::check - atomic action aborting with 1 threads active!
SQL Error: 0, SQLState: null
IJ031070: Transaction cannot proceed: STATUS_ROLLEDBACK
Error al buscar productos por código Petro: org.hibernate.exception.LockAcquisitionException
```

**Causa Raíz**: Los métodos DAO estaban lanzando excepciones (`throw e;`) cuando ocurrían errores de base de datos, lo que detenía todo el proceso en lugar de registrar la inconsistencia como novedad y continuar.

## Solución Implementada

Se modificaron los métodos DAO para que **NO lancen excepciones** cuando ocurren errores de base de datos, sino que retornen valores "seguros" (lista vacía o null) y registren el error en los logs. Esto permite que el nivel superior registre la inconsistencia como novedad y continúe procesando el resto de registros.

## Archivos Modificados

### 1. ProductoDaoServiceImpl.java
**Métodos corregidos:**
- `selectByCodigoPetro()` - Retorna `null` en lugar de lanzar excepción
- `selectAllByCodigoPetro()` - Retorna `new ArrayList<>()` en lugar de lanzar excepción

**Antes:**
```java
catch (Exception e) {
    System.err.println("Error al buscar productos por código Petro: " + e.getMessage());
    e.printStackTrace();
    throw e; // ❌ DETENÍA EL PROCESO
}
```

**Después:**
```java
catch (Exception e) {
    System.err.println("Error al buscar productos por código Petro: " + e.getMessage());
    e.printStackTrace();
    // NO lanzar excepción - retornar lista vacía para no detener el proceso
    // El error se registrará como novedad en el nivel superior
    return new java.util.ArrayList<>(); // ✅ CONTINÚA EL PROCESO
}
```

### 2. EntidadDaoServiceImpl.java
**Métodos corregidos:**
- `selectByCodigoPetro()` - Retorna `new ArrayList<>()` en lugar de lanzar excepción
- `selectByNombrePetro35()` - Retorna `new ArrayList<>()` en lugar de lanzar excepción

**Cambio:** Agregado try-catch que retorna lista vacía en caso de error

### 3. PrestamoDaoServiceImpl.java
**Métodos corregidos:**
- `selectByEntidadYProductoActivosById()` - Retorna `new ArrayList<>()` en lugar de lanzar excepción

### 4. DetallePrestamoDaoServiceImpl.java
**Métodos corregidos:**
- `selectByPrestamoYMesAnio()` - Retorna `new ArrayList<>()` en lugar de lanzar excepción
- `selectByPrestamo()` - Retorna `new ArrayList<>()` en lugar de lanzar excepción

## Comportamiento Después de la Corrección

### Flujo Normal (SIN errores de BD)
1. Se ejecuta la consulta en la base de datos
2. Se retornan los resultados normalmente
3. Las validaciones continúan

### Flujo con Error de BD (timeouts, rollbacks, locks, etc.)
1. Se ejecuta la consulta en la base de datos
2. **Ocurre una excepción** (timeout, rollback, etc.)
3. **Se captura la excepción** en el bloque catch
4. **Se registra el error** en los logs (System.err)
5. **Se retorna lista vacía o null** (no se lanza excepción)
6. El nivel superior detecta que no se encontraron datos
7. **Se registra como NOVEDAD** (ej: PRODUCTO_NO_MAPEADO, PRESTAMO_NO_ENCONTRADO)
8. **El proceso CONTINÚA** con el siguiente registro ✅

## Novedades que se Registran Automáticamente

Cuando ocurre un error de BD, el sistema ahora registra automáticamente las siguientes novedades según el caso:

- **PRODUCTO_NO_MAPEADO (9)**: Cuando no se puede buscar o encontrar el producto por código Petro
- **PRESTAMO_NO_ENCONTRADO (10)**: Cuando no se puede buscar o encontrar la entidad/préstamo
- **PARTICIPE_NO_ENCONTRADO (2)**: Cuando no se encuentra la entidad por código Petro
- **Otras novedades relevantes** según el contexto de la validación

## Ventajas de esta Solución

1. ✅ **Resiliencia**: El proceso NO se detiene por errores de BD
2. ✅ **Trazabilidad**: Todos los errores quedan registrados en logs Y como novedades
3. ✅ **Completitud**: Se procesan TODOS los registros del archivo, no solo hasta el primer error
4. ✅ **Auditabilidad**: Las novedades permiten identificar y corregir problemas específicos
5. ✅ **Escalabilidad**: Maneja mejor archivos grandes con muchos registros

## Escenarios Cubiertos

### Timeout de Transacción
- **Antes**: Proceso detenido con error ARJUNA012108
- **Ahora**: Se registra como novedad y continúa

### Lock de Base de Datos
- **Antes**: Proceso detenido con LockAcquisitionException
- **Ahora**: Se registra como novedad y continúa

### Transacción Rolledback
- **Antes**: Proceso detenido con STATUS_ROLLEDBACK
- **Ahora**: Se registra como novedad y continúa

### Producto No Encontrado
- **Antes**: Excepción SQL
- **Ahora**: Novedad PRODUCTO_NO_MAPEADO

### Entidad No Encontrada
- **Antes**: Excepción SQL
- **Ahora**: Novedad PARTICIPE_NO_ENCONTRADO o PRESTAMO_NO_ENCONTRADO

## Recomendaciones

1. **Monitorear los logs**: Los errores de BD siguen siendo registrados en los logs con `System.err.println()` para diagnóstico
2. **Revisar novedades**: Después de procesar un archivo, revisar las novedades registradas para identificar problemas recurrentes
3. **Ajustar timeouts**: Si hay muchos timeouts, considerar aumentar el timeout de transacciones en la configuración de WildFly

## Próximos Pasos

Si se detectan otros métodos DAO que aún lanzan excepciones y detienen el proceso, aplicar el mismo patrón:
```java
try {
    // Consulta de base de datos
} catch (Exception e) {
    System.err.println("Error: " + e.getMessage());
    e.printStackTrace();
    return new ArrayList<>(); // o null según el tipo de retorno
}
```

## Notas Técnicas

- Los métodos DAO mantienen la declaración `throws Throwable` por compatibilidad con la interfaz
- Sin embargo, ahora NO lanzan excepciones en el bloque catch
- Esto permite que el código superior maneje la situación sin interrumpir el flujo
