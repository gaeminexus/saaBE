# README - Sistema de Reportes

## ✅ Implementación Completada

Se ha implementado exitosamente el sistema de generación de reportes JasperReports con la siguiente estructura:

## 📁 Estructura Backend

```
com.saa.ws.reportes/
├── model/
│   ├── ReporteRequest.java       - DTO para solicitud de reportes
│   └── ReporteResponse.java      - DTO para respuesta de reportes
├── service/
│   └── ReporteService.java       - Lógica de negocio para generación
└── rest/
    └── ReporteRest.java          - API REST endpoints
```

## 📂 Estructura de Reportes (Resources)

```
src/main/resources/rep/
├── cnt/          - Reportes de Contabilidad
├── tsr/          - Reportes de Tesorería
├── crd/          - Reportes de Créditos
├── cxc/          - Reportes de Cuentas por Cobrar
├── cxp/          - Reportes de Cuentas por Pagar
├── rhh/          - Reportes de Recursos Humanos
├── img/          - Logos e imágenes para reportes
└── test/         - Reportes de prueba
```

## 🚀 Endpoints Disponibles

### 1. Generar Reporte
```
POST /rest/reportes/generar
Content-Type: application/json

{
  "modulo": "cnt",
  "nombreReporte": "balance_general",
  "formato": "PDF",
  "parametros": {
    "empresaId": 1,
    "fechaInicio": "2026-01-01"
  }
}
```

### 2. Listar Módulos
```
GET /rest/reportes/modulos
```

### 3. Health Check
```
GET /rest/reportes/ping
```

## 🧪 Prueba Rápida

### Con cURL (Windows CMD):
```cmd
curl -X POST http://localhost:8080/SaaBE/rest/reportes/generar ^
  -H "Content-Type: application/json" ^
  -d "{\"modulo\":\"test\",\"nombreReporte\":\"reporte_prueba\",\"formato\":\"PDF\",\"parametros\":{\"empresaId\":1}}" ^
  --output reporte_prueba.pdf
```

### Con PowerShell:
```powershell
$body = @{
    modulo = "test"
    nombreReporte = "reporte_prueba"
    formato = "PDF"
    parametros = @{
        empresaId = 1
    }
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/SaaBE/rest/reportes/generar" `
    -Method Post `
    -ContentType "application/json" `
    -Body $body `
    -OutFile "reporte_prueba.pdf"
```

## 📋 Características Implementadas

✅ Generación de reportes en múltiples formatos (PDF, Excel, HTML)
✅ Estructura modular por áreas de negocio
✅ Soporte para parámetros dinámicos
✅ Manejo de imágenes y logos
✅ Manejo de errores y validaciones
✅ Logging detallado
✅ API REST compatible con Angular
✅ Reporte de prueba incluido

## 🔧 Configuración Necesaria

### 1. DataSource JNDI
Asegúrate de tener configurado un DataSource en WildFly:
```xml
<!-- En standalone.xml -->
<datasource jndi-name="java:jboss/datasources/SaaDS" ...>
```

### 2. Inyección del DataSource
El servicio usa `@Inject DataSource` que debe estar configurado en tu proyecto.

### 3. CORS (para Angular)
Aplica la configuración CORS usando el archivo `config/standalone-cors.cli/standalone-cors.cli`

## 📱 Integración con Angular

### Instalar HttpClient
```typescript
// En app.module.ts
import { HttpClientModule } from '@angular/common/http';

@NgModule({
  imports: [
    HttpClientModule,
    // ...
  ]
})
```

### Configurar Environment
```typescript
// environment.ts
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/SaaBE/rest'
};
```

### Usar el Servicio
```typescript
// En tu componente
constructor(private reporteService: ReporteService) {}

generarBalance() {
  this.reporteService.generarYDescargar(
    'cnt',
    'balance_general',
    { empresaId: 1, fecha: '2026-01-29' },
    'PDF'
  );
}
```

## 📝 Próximos Pasos

1. **Agregar tus reportes JRXML** en las carpetas correspondientes:
   - `rep/cnt/` - Reportes de contabilidad
   - `rep/tsr/` - Reportes de tesorería
   - etc.

2. **Agregar logos** en `rep/img/`:
   - logo_empresa.png
   - logo_sistema.png
   - etc.

3. **Crear el servicio Angular** usando el código de `docs/Reportes-API-Guide.md`

4. **Probar el endpoint** usando el reporte de prueba en `rep/test/reporte_prueba.jrxml`

5. **Implementar seguridad** (JWT, roles, permisos por módulo)

## 📚 Documentación Completa

Ver: `docs/Reportes-API-Guide.md` para:
- Ejemplos completos de Angular
- Todos los endpoints disponibles
- Troubleshooting
- Configuración avanzada

## ⚠️ Notas Importantes

1. Los archivos `.jrxml` deben estar en la carpeta correcta según el módulo
2. Los nombres de reportes no deben incluir la extensión `.jrxml`
3. Los parámetros `RUTA_IMAGENES` y `SUBREPORT_DIR` se agregan automáticamente
4. El formato por defecto es PDF si no se especifica

## 🐛 Solución de Problemas

### Reporte no encontrado
- Verifica que el archivo exista en `/rep/{modulo}/{nombreReporte}.jrxml`
- Revisa que el nombre del módulo sea válido (cnt, tsr, crd, cxc, cxp, rhh)

### Error de conexión
- Verifica que el DataSource esté configurado
- Revisa los logs de WildFly

### CORS bloqueado
- Aplica la configuración CORS en WildFly
- Verifica que el origen esté permitido
