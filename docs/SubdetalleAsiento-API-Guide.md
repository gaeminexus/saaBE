# Documentación de SubdetalleAsiento (Activos Fijos)

## Resumen

Se ha creado el mapeo completo para la tabla `CNT.SDAS` (SubdetalleAsiento) que permite gestionar el detalle de activos fijos asociados a los asientos contables.

## Archivos Creados

### 1. Model
- **Ubicación**: `src/main/java/com/saa/model/cnt/SubdetalleAsiento.java`
- **Descripción**: Entidad JPA que mapea la tabla CNT.SDAS
- **Tabla**: CNT.SDAS
- **Secuencia**: CNT.SQ_SDASCDGO

### 2. DAO Interface
- **Ubicación**: `src/main/java/com/saa/ejb/cnt/dao/SubdetalleAsientoDaoService.java`
- **Descripción**: Interface que define los métodos de acceso a datos

### 3. DAO Implementation
- **Ubicación**: `src/main/java/com/saa/ejb/cnt/daoImpl/SubdetalleAsientoDaoServiceImpl.java`
- **Descripción**: Implementación de los métodos de acceso a datos

### 4. Service Interface
- **Ubicación**: `src/main/java/com/saa/ejb/cnt/service/SubdetalleAsientoService.java`
- **Descripción**: Interface que define la lógica de negocio

### 5. Service Implementation
- **Ubicación**: `src/main/java/com/saa/ejb/cnt/serviceImpl/SubdetalleAsientoServiceImpl.java`
- **Descripción**: Implementación de la lógica de negocio
- **Funcionalidades**:
  - Cálculo automático de base a depreciar
  - Cálculo automático de valor neto en libros

### 6. REST Endpoint
- **Ubicación**: `src/main/java/com/saa/ws/rest/cnt/SubdetalleAsientoRest.java`
- **Path Base**: `/sdas`

### 7. Actualización de Constantes
- **Archivo**: `src/main/java/com/saa/model/cnt/NombreEntidadesContabilidad.java`
- **Constante agregada**: `SUBDETALLE_ASIENTO = "SubdetalleAsiento"`

## Endpoints REST Disponibles

### GET - Obtener todos los subdetalles
```
GET /sdas/getAll
Response: 200 OK - Lista de SubdetalleAsiento
```

### GET - Obtener subdetalle por ID
```
GET /sdas/getId/{id}
Response: 200 OK - SubdetalleAsiento
Response: 404 NOT FOUND - Si no existe
```

### GET - Obtener subdetalles por ID de Detalle Asiento
```
GET /sdas/getByDetalleAsiento/{idDetalleAsiento}
Response: 200 OK - Lista de SubdetalleAsiento
```

### GET - Obtener subdetalles por Código de Activo
```
GET /sdas/getByCodigoActivo/{codigoActivo}
Response: 200 OK - Lista de SubdetalleAsiento
```

### GET - Obtener subdetalles por Categoría
```
GET /sdas/getByCategoria/{categoria}
Response: 200 OK - Lista de SubdetalleAsiento
```

### GET - Obtener subdetalles por Responsable
```
GET /sdas/getByResponsable/{responsable}
Response: 200 OK - Lista de SubdetalleAsiento
```

### POST - Crear nuevo subdetalle
```
POST /sdas
Content-Type: application/json
Body: SubdetalleAsiento (JSON)
Response: 201 CREATED - SubdetalleAsiento creado
```

### PUT - Actualizar subdetalle existente
```
PUT /sdas
Content-Type: application/json
Body: SubdetalleAsiento (JSON)
Response: 200 OK - SubdetalleAsiento actualizado
```

### POST - Búsqueda por criterios
```
POST /sdas/selectByCriteria
Content-Type: application/json
Body: List<DatosBusqueda>
Response: 200 OK - Lista de SubdetalleAsiento
```

### DELETE - Eliminar subdetalle
```
DELETE /sdas/{id}
Response: 204 NO CONTENT - Eliminado exitosamente
```

## Estructura de Datos (JSON)

### SubdetalleAsiento
```json
{
  "codigo": 1,
  "detalleAsiento": {
    "codigo": 100
  },
  "codigoActivo": "ACT-001",
  "nombreBien": "Computadora Dell Optiplex",
  "categoria": "Equipos de Computación",
  "tipo": "Hardware",
  "fechaAdquisicion": "2024-01-15",
  "costoAdquisicion": 1500.00,
  "mejorasCapitalizadas": 0.00,
  "valorResidual": 150.00,
  "baseDepreciar": 1350.00,
  "vidaUtilTotal": 36,
  "vidaUtilRemanente": 30,
  "porcentajeDepreciacion": 33.33,
  "cuotaDepreciacion": 37.50,
  "depreciacionAcumulada": 225.00,
  "valorNetoLibros": 1275.00,
  "ubicacionGeneral": "Oficina Principal",
  "ubicacionEspecifica": "Departamento de Contabilidad",
  "responsable": "Juan Pérez",
  "estadoFisico": "Bueno",
  "factura": "FAC-2024-001 / Proveedor XYZ",
  "observaciones": "Activo en uso"
}
```

## Cálculos Automáticos

El servicio realiza los siguientes cálculos automáticos al guardar:

### Base a Depreciar
```
Base a Depreciar = (Costo Adquisición + Mejoras Capitalizadas) - Valor Residual
```

### Valor Neto en Libros
```
Valor Neto en Libros = (Costo Adquisición + Mejoras Capitalizadas) - Depreciación Acumulada
```

## Relaciones

- **DetalleAsiento** (ManyToOne): Relación con la tabla DTAS (Detalle de Asiento)
  - Permite asociar uno o más activos fijos a un detalle de asiento contable

## Campos Principales

| Campo | Tipo | Descripción |
|-------|------|-------------|
| codigo | Long | ID único (PK) |
| detalleAsiento | DetalleAsiento | Referencia al detalle de asiento (FK) |
| codigoActivo | String(50) | Código del activo fijo |
| nombreBien | String(200) | Nombre del bien (NOT NULL) |
| categoria | String(100) | Categoría del activo |
| tipo | String(100) | Tipo de activo |
| fechaAdquisicion | LocalDate | Fecha de adquisición |
| costoAdquisicion | Double | Costo de adquisición (NOT NULL) |
| mejorasCapitalizadas | Double | Mejoras capitalizadas (Default: 0) |
| valorResidual | Double | Valor residual |
| baseDepreciar | Double | Base a depreciar (calculado) |
| vidaUtilTotal | Integer | Vida útil total en meses |
| vidaUtilRemanente | Integer | Vida útil remanente en meses |
| porcentajeDepreciacion | Double | Porcentaje de depreciación anual |
| cuotaDepreciacion | Double | Cuota de depreciación mensual |
| depreciacionAcumulada | Double | Depreciación acumulada (Default: 0) |
| valorNetoLibros | Double | Valor neto en libros (calculado) |
| ubicacionGeneral | String(150) | Ubicación general |
| ubicacionEspecifica | String(150) | Ubicación específica |
| responsable | String(150) | Responsable/custodio |
| estadoFisico | String(50) | Estado físico del activo |
| factura | String(150) | No. de factura / proveedor |
| observaciones | String(200) | Observaciones |

## Ejemplo de Uso

### Crear un nuevo activo fijo
```bash
curl -X POST http://localhost:8080/saaBE/api/sdas \
  -H "Content-Type: application/json" \
  -d '{
    "detalleAsiento": {"codigo": 100},
    "codigoActivo": "ACT-001",
    "nombreBien": "Laptop HP",
    "categoria": "Equipos de Computación",
    "costoAdquisicion": 2000.00,
    "valorResidual": 200.00,
    "vidaUtilTotal": 36,
    "responsable": "María García"
  }'
```

### Obtener activos de un detalle de asiento
```bash
curl -X GET http://localhost:8080/saaBE/api/sdas/getByDetalleAsiento/100
```

### Buscar activos por responsable
```bash
curl -X GET http://localhost:8080/saaBE/api/sdas/getByResponsable/Juan%20Pérez
```

## Notas Importantes

1. Los campos calculados (baseDepreciar y valorNetoLibros) se calculan automáticamente al guardar
2. La relación con DetalleAsiento permite asociar múltiples activos a un mismo asiento contable
3. Todos los servicios REST devuelven respuestas en formato JSON
4. Los errores se manejan con códigos HTTP estándar y mensajes descriptivos

## Estado

✅ **Completado** - Todo el mapeo está funcional y listo para usar
- Model: ✅
- DAO: ✅
- Service: ✅
- REST: ✅
- Sin errores de compilación: ✅
