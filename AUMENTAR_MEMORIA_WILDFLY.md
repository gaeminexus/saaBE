# Cómo Aumentar la Memoria de WildFly para Resolver OutOfMemoryError

## Error Actual
```
UT005023: Exception handling request to /SaaBE/rest/aprt/getAll: 
java.lang.OutOfMemoryError: Java heap space
```

## Solución: Aumentar Memoria Heap

### Archivo a Editar
```
C:\wildfly38\bin\standalone.conf.bat
```

### Cambios a Realizar

**Buscar la línea 49-51 que dice:**
```bat
if "x%JBOSS_JAVA_SIZING%" == "x" (
    rem # JVM memory allocation pool parameters - modify as appropriate.
    set "JBOSS_JAVA_SIZING=-Xms4096m -Xmx4096m -XX:MetaspaceSize=2048M -XX:MaxMetaspaceSize=2048m"
)
```

**Reemplazar con:**
```bat
if "x%JBOSS_JAVA_SIZING%" == "x" (
    rem # JVM memory allocation pool parameters - modify as appropriate.
    rem # AUMENTADO: Xms/Xmx de 4GB a 8GB, Metaspace de 2GB a 4GB para resolver OutOfMemoryError
    set "JBOSS_JAVA_SIZING=-Xms8192m -Xmx8192m -XX:MetaspaceSize=4096M -XX:MaxMetaspaceSize=4096m"
)
```

### Explicación de los Parámetros

- **-Xms8192m**: Memoria heap inicial = 8 GB (antes 4 GB)
- **-Xmx8192m**: Memoria heap máxima = 8 GB (antes 4 GB)
- **-XX:MetaspaceSize=4096M**: Metaspace inicial = 4 GB (antes 2 GB)
- **-XX:MaxMetaspaceSize=4096m**: Metaspace máximo = 4 GB (antes 2 GB)

### Pasos para Aplicar el Cambio

1. **Detener WildFly** (si está corriendo)
   ```cmd
   Ctrl+C en la consola donde corre WildFly
   ```

2. **Editar el archivo**
   ```cmd
   notepad C:\wildfly38\bin\standalone.conf.bat
   ```

3. **Hacer el cambio** descrito arriba

4. **Guardar el archivo** (Ctrl+S)

5. **Reiniciar WildFly**
   ```cmd
   C:\wildfly38\bin\standalone.bat
   ```

6. **Verificar que el cambio se aplicó**
   - En la consola de WildFly, al iniciar debería mostrar los nuevos valores de memoria
   - Buscar líneas que digan: `-Xms8192m -Xmx8192m`

### Verificación de Memoria

Después de aplicar el cambio, puedes verificar que WildFly tenga suficiente memoria:

```cmd
jps -v | findstr wildfly
```

Deberías ver `-Xms8192m -Xmx8192m` en la salida.

---

## ⚠️ IMPORTANTE - Problema de Fondo

El endpoint `/rest/aprt/getAll` está causando OutOfMemoryError porque probablemente está trayendo **TODOS los registros de aportes** de la base de datos sin paginación.

### Solución Recomendada a Largo Plazo

Deberías **implementar paginación** en el endpoint `getAll()` de aportes:

```java
// En lugar de:
@GET
@Path("/getAll")
public List<Aporte> getAll() {
    return aporteService.selectAll(); // ❌ Trae TODOS los registros
}

// Implementar:
@GET
@Path("/getAll")
public List<Aporte> getAll(
    @QueryParam("page") @DefaultValue("0") int page,
    @QueryParam("size") @DefaultValue("100") int size) {
    return aporteService.selectPaginated(page, size); // ✅ Trae solo una página
}
```

### Archivo a Modificar
El endpoint probablemente está en:
```
C:\work\saaBE\v1\saaBE\src\main\java\com\saa\rest\...\AporteRest.java
```

---

## Resumen

1. **Solución Inmediata**: Aumentar memoria de 4GB a 8GB ✅
2. **Solución Permanente**: Implementar paginación en el endpoint `/rest/aprt/getAll` 
3. **Reiniciar WildFly** después del cambio

Si el problema persiste después de aumentar la memoria, necesitarás implementar la paginación.
