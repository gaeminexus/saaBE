# Configuración Completa de Timeouts en standalone.xml

## Fecha: 2026-03-27

---

## CONFIGURACIÓN COMPLETA DEL SUBSISTEMA EJB3

Busca en tu `standalone.xml` la sección `<subsystem xmlns="urn:jboss:domain:ejb3:10.0">` y cámbiala así:

### ANTES (TU CONFIGURACIÓN ACTUAL):
```xml
<subsystem xmlns="urn:jboss:domain:ejb3:10.0">
    <session-bean>
        <stateless>
            <bean-instance-pool-ref pool-name="slsb-strict-max-pool"/>
        </stateless>
        <stateful default-access-timeout="5000" cache-ref="simple" passivation-disabled-cache-ref="simple"/>
        <singleton default-access-timeout="5000"/>
    </session-bean>
    <!-- resto de configuración... -->
</subsystem>
```

### DESPUÉS (CONFIGURACIÓN CORREGIDA - 15 MINUTOS):
```xml
<subsystem xmlns="urn:jboss:domain:ejb3:10.0">
    <session-bean>
        <stateless>
            <bean-instance-pool-ref pool-name="slsb-strict-max-pool"/>
        </stateless>
        <stateful default-access-timeout="900000" cache-ref="simple" passivation-disabled-cache-ref="simple"/>
        <singleton default-access-timeout="900000"/>
    </session-bean>
    <!-- resto de configuración... -->
</subsystem>
```

**Cambios:**
- `default-access-timeout="5000"` → `default-access-timeout="900000"` (5 seg → 15 min)

---

## CONFIGURACIÓN COMPLETA DEL SUBSISTEMA TRANSACTIONS

Busca en tu `standalone.xml` la sección `<subsystem xmlns="urn:jboss:domain:transactions:...">`:

### EJEMPLO COMÚN (ANTES):
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

### DESPUÉS (CONFIGURACIÓN CORREGIDA - 15 MINUTOS):
```xml
<subsystem xmlns="urn:jboss:domain:transactions:6.0">
    <core-environment node-identifier="${jboss.tx.node.id:1}">
        <process-id>
            <uuid/>
        </process-id>
    </core-environment>
    <recovery-environment socket-binding="txn-recovery-environment" status-socket-binding="txn-status-manager"/>
    <coordinator-environment statistics-enabled="${wildfly.transactions.statistics-enabled:${wildfly.statistics-enabled:false}}" 
                            default-timeout="900"/>
</subsystem>
```

**Cambios:**
- `default-timeout="300"` → `default-timeout="900"` (5 min → 15 min)

---

## RESUMEN DE TODOS LOS CAMBIOS

### 1️⃣ En `<subsystem xmlns="urn:jboss:domain:ejb3:10.0">`:

| Atributo | Valor Actual | Valor Nuevo | Explicación |
|----------|--------------|-------------|-------------|
| `<stateful default-access-timeout>` | `5000` | `900000` | Tiempo máximo para ejecutar un EJB Stateful |
| `<singleton default-access-timeout>` | `5000` | `900000` | Tiempo máximo para ejecutar un EJB Singleton |

**Unidad:** Milisegundos
- 5000 ms = 5 segundos ❌
- 900000 ms = 15 minutos ✅

### 2️⃣ En `<subsystem xmlns="urn:jboss:domain:transactions:...">`:

| Atributo | Valor Actual | Valor Nuevo | Explicación |
|----------|--------------|-------------|-------------|
| `<coordinator-environment default-timeout>` | `300` | `900` | Tiempo máximo de duración de una transacción |

**Unidad:** Segundos
- 300 s = 5 minutos ⚠️
- 900 s = 15 minutos ✅

---

## PASOS PARA APLICAR LOS CAMBIOS

### OPCIÓN A: Usando CLI (MÁS RÁPIDO Y SEGURO)

Crear un archivo `aplicar-timeouts.cli` con este contenido:

```
connect

# Cambiar EJB Stateful timeout a 15 minutos
/subsystem=ejb3:write-attribute(name=default-stateful-bean-access-timeout,value=900000)

# Cambiar EJB Singleton timeout a 15 minutos
/subsystem=ejb3:write-attribute(name=default-singleton-bean-access-timeout,value=900000)

# Cambiar Transaction timeout a 15 minutos
/subsystem=transactions:write-attribute(name=default-timeout,value=900)

# Mostrar configuración actualizada
echo ======================================
echo CONFIGURACIÓN ACTUALIZADA:
echo ======================================
echo 
echo EJB Stateful Access Timeout:
/subsystem=ejb3:read-attribute(name=default-stateful-bean-access-timeout)
echo 
echo EJB Singleton Access Timeout:
/subsystem=ejb3:read-attribute(name=default-singleton-bean-access-timeout)
echo 
echo Transaction Default Timeout:
/subsystem=transactions:read-attribute(name=default-timeout)
echo 
echo ======================================

# Recargar el servidor para aplicar cambios
reload
```

Ejecutar:
```cmd
cd C:\ruta\a\wildfly-38\bin
jboss-cli.bat --file=aplicar-timeouts.cli
```

### OPCIÓN B: Edición Manual de standalone.xml

#### Paso 1: Detener WildFly
```cmd
cd C:\ruta\a\wildfly-38\bin
jboss-cli.bat --connect command=:shutdown
```

#### Paso 2: Hacer Backup
```cmd
copy "C:\ruta\a\wildfly-38\standalone\configuration\standalone.xml" "C:\ruta\a\wildfly-38\standalone\configuration\standalone.xml.backup-%date:~-4,4%%date:~-10,2%%date:~-7,2%"
```

#### Paso 3: Abrir standalone.xml
```
Ubicación: C:\ruta\a\wildfly-38\standalone\configuration\standalone.xml
```

#### Paso 4: Buscar y Cambiar - SECCIÓN EJB3

**Buscar esto:**
```xml
<stateful default-access-timeout="5000"
```

**Cambiar a:**
```xml
<stateful default-access-timeout="900000"
```

**Buscar esto:**
```xml
<singleton default-access-timeout="5000"/>
```

**Cambiar a:**
```xml
<singleton default-access-timeout="900000"/>
```

#### Paso 5: Buscar y Cambiar - SECCIÓN TRANSACTIONS

**Buscar esto (puede estar en múltiples líneas):**
```xml
<coordinator-environment statistics-enabled="..." 
                        default-timeout="300"/>
```

**Cambiar a:**
```xml
<coordinator-environment statistics-enabled="..." 
                        default-timeout="900"/>
```

O si está en una sola línea:

**Buscar:**
```xml
<coordinator-environment ... default-timeout="300"/>
```

**Cambiar a:**
```xml
<coordinator-environment ... default-timeout="900"/>
```

#### Paso 6: Guardar el archivo

#### Paso 7: Reiniciar WildFly
```cmd
cd C:\ruta\a\wildfly-38\bin
standalone.bat
```

---

## VERIFICACIÓN POST-CAMBIOS

### 1. Verificar que WildFly inició correctamente

Revisar el log:
```
C:\ruta\a\wildfly-38\standalone\log\server.log
```

Buscar líneas como:
```
INFO  [org.jboss.as] (Controller Boot Thread) WFLYSRV0025: WildFly Full 38.x.x (WildFly Core x.x.x) started in xxxms
```

### 2. Verificar configuración con CLI

```cmd
cd C:\ruta\a\wildfly-38\bin
jboss-cli.bat
connect

# Verificar EJB timeouts
/subsystem=ejb3:read-attribute(name=default-stateful-bean-access-timeout)
/subsystem=ejb3:read-attribute(name=default-singleton-bean-access-timeout)

# Verificar transaction timeout
/subsystem=transactions:read-attribute(name=default-timeout)

exit
```

**Salida esperada:**
```json
{
    "outcome" => "success",
    "result" => 900000L
}

{
    "outcome" => "success",
    "result" => 900000L
}

{
    "outcome" => "success",
    "result" => 900
}
```

---

## VALORES ALTERNATIVOS (SI 15 MINUTOS NO ES SUFICIENTE)

### Para archivos MUY grandes (30 minutos):

**EJB (en milisegundos):**
```xml
<stateful default-access-timeout="1800000" .../>
<singleton default-access-timeout="1800000"/>
```

**Transactions (en segundos):**
```xml
<coordinator-environment ... default-timeout="1800"/>
```

### Para archivos gigantes (60 minutos):

**EJB (en milisegundos):**
```xml
<stateful default-access-timeout="3600000" .../>
<singleton default-access-timeout="3600000"/>
```

**Transactions (en segundos):**
```xml
<coordinator-environment ... default-timeout="3600"/>
```

---

## TABLA DE CONVERSIÓN RÁPIDA

| Tiempo deseado | EJB timeout (ms) | Transaction timeout (s) |
|----------------|------------------|-------------------------|
| 5 minutos | 300000 | 300 |
| 10 minutos | 600000 | 600 |
| **15 minutos** | **900000** | **900** |
| 20 minutos | 1200000 | 1200 |
| 30 minutos | 1800000 | 1800 |
| 60 minutos | 3600000 | 3600 |

---

## TROUBLESHOOTING

### ❌ Error: "Failed to connect to the controller"
**Solución:** El servidor no está corriendo. Iniciarlo primero.

### ❌ Error al parsear standalone.xml
**Solución:** Verificar que no falten comillas o haya errores de sintaxis XML. Restaurar desde backup.

### ❌ Los cambios no se aplican
**Solución:** Asegurarse de reiniciar WildFly después de cambios manuales en XML.

### ❌ Sigue habiendo timeouts después de los cambios
**Solución:** 
1. Verificar que los cambios se aplicaron (ver sección Verificación)
2. Aumentar aún más los timeouts (30 o 60 minutos)
3. Revisar logs para ver si hay otros problemas (memoria, base de datos)

---

## COMANDOS ÚTILES DE VERIFICACIÓN

```cmd
# Ver toda la configuración EJB3
/subsystem=ejb3:read-resource(recursive=true)

# Ver toda la configuración de Transactions
/subsystem=transactions:read-resource(recursive=true)

# Ver uso de memoria actual
/core-service=platform-mbean/type=memory:read-resource(include-runtime=true)

# Ver threads activos
/core-service=platform-mbean/type=threading:read-resource(include-runtime=true)
```

---

## NOTAS IMPORTANTES

⚠️ **CRÍTICO:** Los valores están en unidades DIFERENTES:
- **EJB timeouts** → Milisegundos (ms)
- **Transaction timeout** → Segundos (s)

✅ **RECOMENDACIÓN:** Usar CLI en lugar de edición manual para evitar errores

✅ **BACKUP:** Siempre hacer backup antes de modificar standalone.xml

✅ **CONSISTENCIA:** Ambos timeouts deben ser similares (EJB ≈ Transaction)

---

## RESULTADO ESPERADO

Con estos cambios aplicados:

✅ Timeout de EJB Stateful: **5 segundos** → **15 minutos**
✅ Timeout de EJB Singleton: **5 segundos** → **15 minutos**
✅ Timeout de Transacciones: **5 minutos** → **15 minutos**

**Beneficios:**
- ✅ Procesar archivos de 5000+ registros sin interrupciones
- ✅ Tiempo suficiente para validaciones de base de datos
- ✅ Menos errores de timeout en operaciones largas
- ✅ Mejor experiencia al cargar archivos grandes de Petrocomercial

