# Configuración de Timeout de Transacciones en WildFly 38

## Fecha
2026-03-27

## Objetivo
Aumentar el timeout de transacciones en WildFly 38 para evitar errores `ARJUNA012108` (transaction timeout) durante el procesamiento de archivos Petro grandes.

---

## OPCIÓN 1: Configuración mediante CLI (RECOMENDADO)

### Paso 1: Acceder al CLI de WildFly

```cmd
cd C:\ruta\a\wildfly-38\bin
jboss-cli.bat
```

### Paso 2: Conectarse al servidor

Si el servidor está corriendo:
```
connect
```

Si el servidor está apagado (modo standalone):
```
embed-server --server-config=standalone.xml
```

### Paso 3: Aumentar el timeout de transacciones

El timeout por defecto es **300 segundos (5 minutos)**. Para aumentarlo a **600 segundos (10 minutos)**:

```
/subsystem=transactions:write-attribute(name=default-timeout,value=600)
```

Para 15 minutos (900 segundos):
```
/subsystem=transactions:write-attribute(name=default-timeout,value=900)
```

Para 30 minutos (1800 segundos):
```
/subsystem=transactions:write-attribute(name=default-timeout,value=1800)
```

### Paso 4: Verificar el cambio

```
/subsystem=transactions:read-attribute(name=default-timeout)
```

### Paso 5: Recargar el servidor

```
reload
```

---

## OPCIÓN 2: Edición Manual del archivo standalone.xml

### Paso 1: Detener WildFly

```cmd
cd C:\ruta\a\wildfly-38\bin
jboss-cli.bat --connect command=:shutdown
```

### Paso 2: Editar standalone.xml

Abrir el archivo:
```
C:\ruta\a\wildfly-38\standalone\configuration\standalone.xml
```

### Paso 3: Buscar el subsystem de transacciones

Buscar la sección `<subsystem xmlns="urn:jboss:domain:transactions:...">`:

```xml
<subsystem xmlns="urn:jboss:domain:transactions:6.0">
    <core-environment node-identifier="${jboss.tx.node.id:1}">
        <process-id>
            <uuid/>
        </process-id>
    </core-environment>
    <recovery-environment socket-binding="txn-recovery-environment" status-socket-binding="txn-status-manager"/>
    <coordinator-environment statistics-enabled="${wildfly.transactions.statistics-enabled:${wildfly.statistics-enabled:false}}" 
                            default-timeout="300"/>
</subsystem>
```

### Paso 4: Modificar el default-timeout

Cambiar el valor de `default-timeout` en la línea `<coordinator-environment>`:

**Antes:**
```xml
<coordinator-environment statistics-enabled="..." default-timeout="300"/>
```

**Después (10 minutos):**
```xml
<coordinator-environment statistics-enabled="..." default-timeout="600"/>
```

**Después (15 minutos):**
```xml
<coordinator-environment statistics-enabled="..." default-timeout="900"/>
```

**Después (30 minutos):**
```xml
<coordinator-environment statistics-enabled="..." default-timeout="1800"/>
```

### Paso 5: Guardar y reiniciar WildFly

```cmd
cd C:\ruta\a\wildfly-38\bin
standalone.bat
```

---

## OPCIÓN 3: Timeout a nivel de EJB (Programático)

Si solo necesitas aumentar el timeout para operaciones específicas, puedes usar anotaciones en el código:

### En la clase CargaArchivoPetroServiceImpl.java

```java
import jakarta.ejb.TransactionTimeout;
import java.util.concurrent.TimeUnit;

@Stateful
public class CargaArchivoPetroServiceImpl implements CargaArchivoPetroService {
    
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    @TransactionTimeout(value = 15, unit = TimeUnit.MINUTES) // 15 minutos
    public String procesarArchivoPetro(InputStream archivoInputStream, ...) throws Throwable {
        // ... código existente
    }
}
```

Valores recomendados:
- **5 minutos**: `@TransactionTimeout(value = 5, unit = TimeUnit.MINUTES)`
- **10 minutos**: `@TransactionTimeout(value = 10, unit = TimeUnit.MINUTES)`
- **15 minutos**: `@TransactionTimeout(value = 15, unit = TimeUnit.MINUTES)`
- **30 minutos**: `@TransactionTimeout(value = 30, unit = TimeUnit.MINUTES)`

---

## OPCIÓN 4: Timeout del DataSource (Pool de Conexiones)

Si los timeouts son por conexiones a la base de datos, también ajustar el datasource:

### Mediante CLI:

```
/subsystem=datasources/data-source=TuDataSource:write-attribute(name=blocking-timeout-wait-millis,value=300000)
```

Esto configura 5 minutos (300000 ms) para esperar una conexión del pool.

### En standalone.xml:

```xml
<datasource jndi-name="java:jboss/datasources/TuDataSource" pool-name="TuDataSource">
    <!-- ... otras configuraciones ... -->
    <timeout>
        <blocking-timeout-millis>300000</blocking-timeout-millis>
        <idle-timeout-minutes>30</idle-timeout-minutes>
    </timeout>
</datasource>
```

---

## Verificar Configuración Actual

### Ver timeout de transacciones actual:

```cmd
cd C:\ruta\a\wildfly-38\bin
jboss-cli.bat
connect
/subsystem=transactions:read-attribute(name=default-timeout)
```

### Ver configuración del datasource:

```
/subsystem=datasources/data-source=TuDataSource:read-resource(recursive=true)
```

---

## Recomendaciones

### Para archivos Petro pequeños (< 1000 registros):
- **Timeout transacciones**: 600 segundos (10 minutos)
- **Timeout datasource**: 300000 ms (5 minutos)

### Para archivos Petro medianos (1000-5000 registros):
- **Timeout transacciones**: 900 segundos (15 minutos)
- **Timeout datasource**: 600000 ms (10 minutos)

### Para archivos Petro grandes (> 5000 registros):
- **Timeout transacciones**: 1800 segundos (30 minutos)
- **Timeout datasource**: 900000 ms (15 minutos)

### Consideración Importante:
Con las correcciones realizadas en los DAOs, los timeouts deberían ser **menos frecuentes** porque:
- ✅ Los errores de BD no detienen el proceso
- ✅ Se registran como novedades y continúa
- ✅ No se acumulan transacciones fallidas

---

## Monitoreo

### Ver transacciones activas:

```
/subsystem=transactions:read-attribute(name=number-of-transactions)
```

### Ver estadísticas de transacciones:

```
/subsystem=transactions:read-resource(include-runtime=true)
```

---

## Troubleshooting

### Si después de aumentar el timeout siguen los errores:

1. **Verificar locks en la BD**: Puede haber queries bloqueadas
   ```sql
   -- Oracle
   SELECT * FROM v$locked_object;
   ```

2. **Verificar conexiones del pool**: Puede estar agotado
   ```
   /subsystem=datasources/data-source=TuDataSource/statistics=pool:read-resource(include-runtime=true)
   ```

3. **Aumentar el pool de conexiones**:
   ```
   /subsystem=datasources/data-source=TuDataSource:write-attribute(name=max-pool-size,value=50)
   ```

4. **Revisar logs de WildFly**:
   ```
   C:\ruta\a\wildfly-38\standalone\log\server.log
   ```

---

## Script de Configuración Rápida

Crear un archivo `config-timeout.cli` con:

```
connect
/subsystem=transactions:write-attribute(name=default-timeout,value=900)
/subsystem=datasources/data-source=OracleDS:write-attribute(name=blocking-timeout-wait-millis,value=600000)
reload
```

Ejecutar:
```cmd
jboss-cli.bat --file=config-timeout.cli
```

---

## Notas Importantes

- ⚠️ **Siempre hacer backup** del `standalone.xml` antes de modificar
- ⚠️ **Reiniciar WildFly** después de cambios en archivos XML
- ⚠️ **No usar timeouts muy altos** en producción (máximo 30 minutos recomendado)
- ✅ **Con las correcciones de DAOs**, los timeouts deberían reducirse significativamente
- ✅ **Monitorear logs** para identificar la causa raíz de timeouts persistentes
