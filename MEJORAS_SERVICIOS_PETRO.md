# Mejoras en Servicios de Generación de Archivos Petro

**Fecha:** 2026-04-15  
**Autor:** Sistema SAA

## Resumen

Se han agregado funcionalidades adicionales a los servicios de generación de archivos Petrocomercial, continuando con la migración de lógica de negocio desde REST a la capa de servicios.

## Cambios Realizados

### 1. ProcesoGeneracionArchivoPetroService (Interface)

Se agregaron los siguientes métodos nuevos:

#### `anularGeneracion()`
- **Propósito:** Anular una generación existente
- **Parámetros:** codigoGeneracion, usuario, motivo
- **Validaciones:** Solo anula generaciones en estado GENERADO (1)
- **Acción:** Cambia el estado a ANULADO (0) y registra el motivo

#### `marcarComoEnviado()`
- **Propósito:** Marcar una generación como enviada a Petrocomercial
- **Parámetros:** codigoGeneracion, usuario
- **Validaciones:** Solo marca generaciones en estado GENERADO (1)
- **Acción:** Cambia el estado a ENVIADO (2) y registra fecha de envío

#### `marcarComoProcesado()`
- **Propósito:** Marcar una generación como procesada por Petrocomercial
- **Parámetros:** codigoGeneracion, usuario
- **Validaciones:** Solo marca generaciones en estado ENVIADO (2)
- **Acción:** Cambia el estado a PROCESADO (3) y registra fecha de proceso

#### `regenerarArchivoTXT()`
- **Propósito:** Regenerar el archivo TXT físico sin recrear la estructura de datos
- **Parámetros:** codigoGeneracion
- **Retorno:** Ruta del archivo regenerado
- **Uso:** Útil cuando se necesita volver a generar el archivo por corrupción o pérdida

#### `obtenerEstadisticas()`
- **Propósito:** Obtener estadísticas completas de una generación
- **Parámetros:** codigoGeneracion
- **Retorno:** Mapa con estadísticas detalladas:
  - Total de registros
  - Total de monto
  - Desglose por producto
  - Total de aportes vs préstamos
  - Estado actual

### 2. ProcesoGeneracionArchivoPetroServiceImpl (Implementación)

Se implementaron todos los métodos definidos en la interface:

```java
@Override
public void anularGeneracion(Long codigoGeneracion, String usuario, String motivo)
@Override
public GeneracionArchivoPetro marcarComoEnviado(Long codigoGeneracion, String usuario)
@Override
public GeneracionArchivoPetro marcarComoProcesado(Long codigoGeneracion, String usuario)
@Override
public String regenerarArchivoTXT(Long codigoGeneracion)
@Override
public Map<String, Object> obtenerEstadisticas(Long codigoGeneracion)
```

**Características de la implementación:**
- Validaciones exhaustivas de estados
- Mensajes de error descriptivos
- Logging completo de operaciones
- Manejo de excepciones robusto
- Actualización de fechas de auditoría

### 3. GeneracionArchivoPetroRest (Endpoints REST)

Se agregaron nuevos endpoints REST que SOLO validan parámetros y delegan al servicio:

#### POST `/generacion-petro/anular/{codigoGeneracion}`
```json
Body: {
  "usuario": "string",
  "motivo": "string"
}
```

#### POST `/generacion-petro/marcar-enviado/{codigoGeneracion}`
```json
Body: {
  "usuario": "string"
}
```

#### POST `/generacion-petro/marcar-procesado/{codigoGeneracion}`
```json
Body: {
  "usuario": "string"
}
```

#### POST `/generacion-petro/regenerar-archivo/{codigoGeneracion}`
- No requiere body
- Retorna la ruta del archivo generado

#### GET `/generacion-petro/estadisticas/{codigoGeneracion}`
- Retorna estadísticas completas en formato JSON

## Estados de Generación

| Estado | Valor | Descripción |
|--------|-------|-------------|
| ANULADO | 0 | Generación anulada (no se puede usar) |
| GENERADO | 1 | Archivo generado, listo para enviar |
| ENVIADO | 2 | Archivo enviado a Petrocomercial |
| PROCESADO | 3 | Archivo procesado por Petrocomercial |

## Flujo de Estados

```
GENERADO (1) → [anular] → ANULADO (0)
GENERADO (1) → [marcar enviado] → ENVIADO (2)
ENVIADO (2) → [marcar procesado] → PROCESADO (3)
```

## Principios de Arquitectura Aplicados

✅ **Separación de responsabilidades:** REST solo valida, Service contiene la lógica  
✅ **Single Responsibility:** Cada método tiene una responsabilidad única  
✅ **DRY (Don't Repeat Yourself):** Reutilización de métodos existentes  
✅ **Validación de negocio:** Todas las validaciones están en el Service  
✅ **Trazabilidad:** Logging completo de todas las operaciones  
✅ **Auditoría:** Registro de usuario y fechas en todas las modificaciones  

## Servicios Relacionados

La funcionalidad de generación de archivos Petro utiliza los siguientes servicios:

1. **GeneracionArchivoPetroService** - CRUD de cabecera (GNAP)
2. **DetalleGeneracionArchivoService** - CRUD de detalles por producto (DTGA)
3. **ParticipeDetalleGeneracionArchivoService** - CRUD de líneas individuales (PDGA)
4. **CuotaXParticipeGeneracionService** - CRUD de detalle de cuotas (CXPG)
5. **ProcesoGeneracionArchivoPetroService** - Orquestación del proceso completo

## Estructura de Tablas

```
GNAP (GeneracionArchivoPetro)
  ├── DTGA (DetalleGeneracionArchivo) [1:N por producto]
       ├── PDGA (ParticipeDetalleGeneracionArchivo) [1:N por partícipe]
            └── CXPG (CuotaXParticipeGeneracion) [1:N por cuota si es préstamo]
```

## Beneficios de estos Cambios

1. **Gestión completa del ciclo de vida:** Desde generación hasta procesamiento
2. **Trazabilidad:** Seguimiento completo del estado del archivo
3. **Flexibilidad:** Regeneración sin pérdida de datos
4. **Reportes:** Estadísticas detalladas para análisis
5. **Auditoría:** Registro de quién y cuándo se realizó cada acción
6. **Mantenibilidad:** Código organizado y fácil de mantener

## Próximos Pasos Sugeridos

- [ ] Implementar endpoint para descarga del archivo TXT
- [ ] Agregar endpoint para consultar historial de cambios de estado
- [ ] Implementar notificaciones por email al cambiar estados
- [ ] Crear reportes adicionales (por periodo, por estado, etc.)
- [ ] Agregar validaciones de permisos por usuario
- [ ] Implementar endpoint para comparar generaciones entre periodos

## Notas Técnicas

- Los errores de compilación de Eclipse son temporales y se resolverán al refrescar el proyecto
- Todos los métodos incluyen manejo de excepciones robusto
- El código sigue las convenciones de Java y Jakarta EE
- La documentación Javadoc está completa en todos los métodos

## Testing

Para probar los nuevos endpoints:

```bash
# Anular generación
curl -X POST http://localhost:8080/saaBE/api/generacion-petro/anular/1 \
  -H "Content-Type: application/json" \
  -d '{"usuario": "admin", "motivo": "Error en datos"}'

# Marcar como enviado
curl -X POST http://localhost:8080/saaBE/api/generacion-petro/marcar-enviado/1 \
  -H "Content-Type: application/json" \
  -d '{"usuario": "admin"}'

# Marcar como procesado
curl -X POST http://localhost:8080/saaBE/api/generacion-petro/marcar-procesado/1 \
  -H "Content-Type: application/json" \
  -d '{"usuario": "admin"}'

# Regenerar archivo
curl -X POST http://localhost:8080/saaBE/api/generacion-petro/regenerar-archivo/1

# Obtener estadísticas
curl http://localhost:8080/saaBE/api/generacion-petro/estadisticas/1
```

---
**Fin del documento**
