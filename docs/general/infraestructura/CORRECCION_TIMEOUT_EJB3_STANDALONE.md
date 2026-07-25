# Corrección de Timeout en EJB3 para Procesamiento de Archivos Petro

## Fecha
2027-03-27

## Configuración Actual Identificada

En tu `standalone.xml` tienes:

```xml
<subsystem xmlns="urn:jboss:domain:ejb3:10.0">
    <session-bean>
        <stateless>
            <bean-instance-pool-ref pool-name="slsb-strict-max-pool"/>
        </stateless>
        <stateful default-access-timeout="5000" cache-ref="simple" passivation-disabled-cache-ref="simple"/>
        <singleton default-access-timeout="5000"/>
    </session-bean>
```

## Problema Identificado

- **`default-access-timeout="5000"`** = **5 segundos** (5000 milisegundos)
- Esto es **MUY BAJO** para procesar archivos Petro grandes
- El timeout de acceso controla cuánto tiempo se espera para obtener acceso al bean

## Solución Recomendada

### OPCIÓN 1: Modificación mediante CLI (RECOMENDADO)

```cmd
cd C:\ruta\a\wildfly-38\bin
jboss-cli.bat
connect

# Aumentar timeout de stateful beans a 15 minutos (900000 ms)
/subsystem=ejb3:write-attribute(name=default-stateful-bean-access-timeout,value=900000)

# Aumentar timeout de singleton beans a 15 minutos (900000 ms)
/subsystem=ejb3:write-attribute(name=default-singleton-bean-access-timeout,value=900000)

# Verificar los cambios
/subsystem=ejb3:read-attribute(name=default-stateful-bean-access-timeout)
/subsystem=ejb3:read-attribute(name=default-singleton-bean-access-timeout)

# Recargar el servidor
reload
```

### OPCIÓN 2: Edición Manual del standalone.xml

#### Paso 1: Detener WildFly

```cmd
cd C:\ruta\a\wildfly-38\bin
jboss-cli.bat --connect command=:shutdown
```

#### Paso 2: Hacer Backup

```cmd
copy C:\ruta\a\wildfly-38\standalone\configuration\standalone.xml C:\ruta\a\wildfly-38\standalone\configuration\standalone.xml.backup
```

#### Paso 3: Editar standalone.xml

Buscar la sección `<subsystem xmlns="urn:jboss:domain:ejb3:10.0">` y modificar:

**ANTES:**
```xml
<subsystem xmlns="urn:jboss:domain:ejb3:10.0">
    <session-bean>
        <stateless>
            <bean-instance-pool-ref pool-name="slsb-strict-max-pool"/>
        </stateless>
        <stateful default-access-timeout="5000" cache-ref="simple" passivation-disabled-cache-ref="simple"/>
        <singleton default-access-timeout="5000"/>
    </session-bean>
```

**DESPUÉS (15 minutos = 900000 ms):**
```xml
<subsystem xmlns="urn:jboss:domain:ejb3:10.0">
    <session-bean>
        <stateless>
            <bean-instance-pool-ref pool-name="slsb-strict-max-pool"/>
        </stateless>
        <stateful default-access-timeout="900000" cache-ref="simple" passivation-disabled-cache-ref="simple"/>
        <singleton default-access-timeout="900000"/>
    </session-bean>
```

**DESPUÉS (30 minutos = 1800000 ms):**
```xml
<subsystem xmlns="urn:jboss:domain:ejb3:10.0">
    <session-bean>
        <stateless>
            <bean-instance-pool-ref pool-name="slsb-strict-max-pool"/>
        </stateless>
        <stateful default-access-timeout="1800000" cache-ref="simple" passivation-disabled-cache-ref="simple"/>
        <singleton default-access-timeout="1800000"/>
    </session-bean>
```

#### Paso 4: Reiniciar WildFly

```cmd
cd C:\ruta\a\wildfly-38\bin
standalone.bat
```

## Valores Recomendados por Milisegundos

| Tiempo | Milisegundos | Recomendado Para |
|--------|--------------|------------------|
| 5 segundos | 5000 | ❌ Muy bajo (actual) |
| 1 minuto | 60000 | ⚠️ Aún bajo |
| 5 minutos | 300000 | ✅ Archivos pequeños |
| 10 minutos | 600000 | ✅ Archivos medianos |
| 15 minutos | 900000 | ✅✅ **RECOMENDADO** |
| 30 minutos | 1800000 | ✅ Archivos muy grandes |

## Configuración Completa del Subsistema EJB3

Para una configuración óptima, también considera aumentar otros timeouts:

```xml
<subsystem xmlns="urn:jboss:domain:ejb3:10.0">
    <session-bean>
        <stateless>
            <bean-instance-pool-ref pool-name="slsb-strict-max-pool"/>
        </stateless>
        <!-- Timeout de acceso: 15 minutos -->
        <stateful default-access-timeout="900000" cache-ref="simple" passivation-disabled-cache-ref="simple"/>
        <singleton default-access-timeout="900000"/>
    </session-bean>
    
    <!-- Pools de beans -->
    <pools>
        <bean-instance-pools>
            <bean-instance-pool name="slsb-strict-max-pool" max-pool-size="100" instance-acquisition-timeout="5" instance-acquisition-timeout-unit="MINUTES"/>
        </bean-instance-pools>
    </pools>
    
    <!-- Thread pools -->
    <thread-pools>
        <thread-pool name="default">
            <max-threads count="20"/>
            <keepalive-time time="100" unit="milliseconds"/>
        </thread-pool>
    </thread-pools>
    
    <!-- Configuración de transacciones remotas -->
    <default-security-domain value="other"/>
    <default-missing-method-permissions-deny-access value="true"/>
    <statistics-enabled value="${wildfly.ejb3.statistics-enabled:${wildfly.statistics-enabled:false}}"/>
    <log-system-exceptions value="true"/>
</subsystem>
```

## Configuración Adicional: Subsistema de Transacciones

También es importante ajustar el timeout de transacciones:

```xml
<subsystem xmlns="urn:jboss:domain:transactions:6.0">
    <core-environment node-identifier="${jboss.tx.node.id:1}">
        <process-id>
            <uuid/>
        </process-id>
    </core-environment>
    <recovery-environment socket-binding="txn-recovery-environment" status-socket-binding="txn-status-manager"/>
    <!-- Timeout de transacciones: 15 minutos (900 segundos) -->
    <coordinator-environment statistics-enabled="${wildfly.transactions.statistics-enabled:${wildfly.statistics-enabled:false}}" 
                            default-timeout="900"/>
</subsystem>
```

## Script de Configuración Completa

Crear archivo `configure-ejb-timeouts.cli`:

```
connect

# EJB Timeouts (15 minutos)
/subsystem=ejb3:write-attribute(name=default-stateful-bean-access-timeout,value=900000)
/subsystem=ejb3:write-attribute(name=default-singleton-bean-access-timeout,value=900000)

# Transaction Timeout (15 minutos)
/subsystem=transactions:write-attribute(name=default-timeout,value=900)

# Verificar cambios
echo "=== EJB Stateful Timeout ==="
/subsystem=ejb3:read-attribute(name=default-stateful-bean-access-timeout)

echo "=== EJB Singleton Timeout ==="
/subsystem=ejb3:read-attribute(name=default-singleton-bean-access-timeout)

echo "=== Transaction Timeout ==="
/subsystem=transactions:read-attribute(name=default-timeout)

# Recargar servidor
reload
```

Ejecutar:
```cmd
cd C:\ruta\a\wildfly-38\bin
jboss-cli.bat --file=configure-ejb-timeouts.cli
```

## Verificación Post-Configuración

### Verificar EJB Timeouts:
```cmd
jboss-cli.bat
connect
/subsystem=ejb3:read-resource(recursive=true)
```

### Verificar Transaction Timeout:
```cmd
/subsystem=transactions:read-attribute(name=default-timeout)
```

### Verificar en Logs:
Después de reiniciar, verificar en:
```
C:\ruta\a\wildfly-38\standalone\log\server.log
```

Buscar líneas como:
```
INFO  [org.jboss.as.ejb3] (MSC service thread 1-2) WFLYEJB0482: Strict pool slsb-strict-max-pool is using a max instance size of 100...
```

## Comportamiento Esperado

### Con timeout de 5000 ms (5 segundos) - ANTES:
- ❌ Proceso se interrumpe si tarda más de 5 segundos
- ❌ Error: `EJB access timeout`
- ❌ Archivos grandes SIEMPRE fallan

### Con timeout de 900000 ms (15 minutos) - DESPUÉS:
- ✅ Proceso puede ejecutarse hasta 15 minutos
- ✅ Archivos grandes se procesan completamente
- ✅ Errores individuales se registran como novedades
- ✅ El proceso continúa hasta completar todos los registros

## Troubleshooting

### Si después de los cambios aún hay timeouts:

1. **Verificar que los cambios se aplicaron:**
   ```cmd
   jboss-cli.bat
   connect
   /subsystem=ejb3:read-resource
   ```

2. **Verificar logs del servidor:**
   ```
   tail -f C:\ruta\a\wildfly-38\standalone\log\server.log
   ```

3. **Aumentar aún más los timeouts** (30 minutos):
   - EJB: `1800000` ms
   - Transaction: `1800` segundos

4. **Verificar memoria JVM:**
   En `standalone.conf.bat` (Windows) o `standalone.conf` (Linux):
   ```
   set "JAVA_OPTS=-Xms2048m -Xmx4096m -XX:MetaspaceSize=512m -XX:MaxMetaspaceSize=1024m"
   ```

5. **Monitorear uso de memoria durante el proceso:**
   ```cmd
   jboss-cli.bat
   connect
   /core-service=platform-mbean/type=memory:read-resource(include-runtime=true)
   ```

## Resumen de Cambios Necesarios

### ✅ CRÍTICO - DEBE CAMBIAR:
1. **EJB Stateful timeout**: 5000 → 900000 ms (5 seg → 15 min)
2. **EJB Singleton timeout**: 5000 → 900000 ms (5 seg → 15 min)
3. **Transaction timeout**: 300 → 900 segundos (5 min → 15 min)

### ⚠️ OPCIONAL - RECOMENDADO:
4. **DataSource blocking timeout**: Aumentar a 600000 ms (10 min)
5. **JVM Heap**: Aumentar a `-Xmx4096m` si procesa archivos muy grandes
6. **Pool de conexiones**: Aumentar `max-pool-size` a 50 o más

## Notas Finales

- ⚠️ **SIEMPRE hacer backup** antes de modificar standalone.xml
- ⚠️ **Reiniciar WildFly** después de cambios manuales en XML
- ✅ Con CLI los cambios son persistentes y no requieren edición manual
- ✅ Los cambios en **EJB timeout** son **CRÍTICOS** para tu caso
- ✅ El timeout de **5 segundos actual** es el que está causando las interrupciones
- ✅ Con **15 minutos** deberías poder procesar archivos de 5000+ registros sin problema
