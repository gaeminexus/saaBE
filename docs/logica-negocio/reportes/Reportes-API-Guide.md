# Guía de API de Reportes JasperReports

## Estructura Backend Implementada

### Paquetes Creados
```
com.saa.ws.reportes
├── model
│   ├── ReporteRequest.java
│   └── ReporteResponse.java
├── service
│   └── ReporteService.java
└── rest
    └── ReporteRest.java
```

### Estructura de Reportes en Resources
```
src/main/resources/rep/
├── cnt/          # Contabilidad
├── tsr/          # Tesorería
├── crd/          # Créditos
├── cxc/          # Cuentas por Cobrar
├── cxp/          # Cuentas por Pagar
├── rhh/          # Recursos Humanos
└── img/          # Logos e imágenes
```

## Endpoints REST

### 1. Generar Reporte
**POST** `/rest/reportes/generar`

**Content-Type:** `application/json`

**Body:**
```json
{
  "modulo": "cnt",
  "nombreReporte": "balance_general",
  "formato": "PDF",
  "parametros": {
    "empresaId": 1,
    "fechaInicio": "2026-01-01",
    "fechaFin": "2026-01-31",
    "usuario": "admin"
  }
}
```

**Formatos soportados:**
- `PDF` (por defecto)
- `EXCEL`, `XLSX`, `XLS`
- `HTML`

**Módulos válidos:**
- `cnt` - Contabilidad
- `tsr` - Tesorería
- `crd` - Créditos
- `cxc` - Cuentas por Cobrar
- `cxp` - Cuentas por Pagar
- `rhh` - Recursos Humanos

**Response:**
- **200 OK**: Retorna el archivo en el formato solicitado
- **400 Bad Request**: Parámetros inválidos
- **404 Not Found**: Reporte no encontrado
- **500 Internal Server Error**: Error al generar el reporte

### 2. Listar Módulos
**GET** `/rest/reportes/modulos`

**Response:**
```json
["cnt", "tsr", "crd", "cxc", "cxp", "rhh"]
```

### 3. Ping (Health Check)
**GET** `/rest/reportes/ping`

**Response:**
```json
{
  "exito": true,
  "mensaje": "Servicio de reportes activo"
}
```

## Integración con Angular

### 1. Servicio TypeScript

Crear archivo: `src/app/services/reporte.service.ts`

```typescript
import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface ReporteRequest {
  modulo: string;
  nombreReporte: string;
  formato: string;
  parametros: { [key: string]: any };
}

export interface ReporteResponse {
  exito: boolean;
  mensaje: string;
  nombreArchivo?: string;
}

@Injectable({
  providedIn: 'root'
})
export class ReporteService {

  private apiUrl = `${environment.apiUrl}/reportes`;

  constructor(private http: HttpClient) { }

  /**
   * Genera y descarga un reporte
   */
  generarReporte(request: ReporteRequest): Observable<Blob> {
    return this.http.post(`${this.apiUrl}/generar`, request, {
      responseType: 'blob',
      headers: new HttpHeaders({
        'Content-Type': 'application/json'
      })
    });
  }

  /**
   * Descarga el reporte generado
   */
  descargarReporte(blob: Blob, nombreArchivo: string): void {
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = nombreArchivo;
    link.click();
    window.URL.revokeObjectURL(url);
  }

  /**
   * Genera y descarga automáticamente
   */
  generarYDescargar(
    modulo: string, 
    nombreReporte: string, 
    parametros: any, 
    formato: string = 'PDF'
  ): void {
    const request: ReporteRequest = {
      modulo,
      nombreReporte,
      formato,
      parametros
    };

    this.generarReporte(request).subscribe({
      next: (blob) => {
        const extension = formato.toUpperCase() === 'EXCEL' || 
                         formato.toUpperCase() === 'XLSX' ? '.xlsx' : 
                         formato.toUpperCase() === 'HTML' ? '.html' : '.pdf';
        const nombreArchivo = `${modulo}_${nombreReporte}${extension}`;
        this.descargarReporte(blob, nombreArchivo);
      },
      error: (error) => {
        console.error('Error al generar reporte:', error);
        alert('Error al generar el reporte. Por favor, intente nuevamente.');
      }
    });
  }

  /**
   * Obtiene módulos disponibles
   */
  obtenerModulos(): Observable<string[]> {
    return this.http.get<string[]>(`${this.apiUrl}/modulos`);
  }

  /**
   * Verifica estado del servicio
   */
  ping(): Observable<ReporteResponse> {
    return this.http.get<ReporteResponse>(`${this.apiUrl}/ping`);
  }
}
```

### 2. Componente de Ejemplo

Crear archivo: `src/app/components/reportes/reporte-viewer.component.ts`

```typescript
import { Component, OnInit } from '@angular/core';
import { ReporteService } from '../../services/reporte.service';

@Component({
  selector: 'app-reporte-viewer',
  templateUrl: './reporte-viewer.component.html',
  styleUrls: ['./reporte-viewer.component.css']
})
export class ReporteViewerComponent implements OnInit {

  modulos: string[] = [];
  moduloSeleccionado: string = 'cnt';
  nombreReporte: string = '';
  formatoSeleccionado: string = 'PDF';
  parametros: any = {};
  
  cargando: boolean = false;

  constructor(private reporteService: ReporteService) { }

  ngOnInit(): void {
    this.cargarModulos();
  }

  cargarModulos(): void {
    this.reporteService.obtenerModulos().subscribe({
      next: (modulos) => this.modulos = modulos,
      error: (error) => console.error('Error al cargar módulos:', error)
    });
  }

  generarReporte(): void {
    if (!this.nombreReporte) {
      alert('Por favor, ingrese el nombre del reporte');
      return;
    }

    this.cargando = true;
    
    this.reporteService.generarYDescargar(
      this.moduloSeleccionado,
      this.nombreReporte,
      this.parametros,
      this.formatoSeleccionado
    );

    // Simular finalización de carga
    setTimeout(() => this.cargando = false, 2000);
  }

  agregarParametro(key: string, value: any): void {
    this.parametros[key] = value;
  }
}
```

### 3. Template HTML de Ejemplo

Crear archivo: `src/app/components/reportes/reporte-viewer.component.html`

```html
<div class="reporte-container">
  <h2>Generador de Reportes</h2>

  <div class="form-group">
    <label>Módulo:</label>
    <select [(ngModel)]="moduloSeleccionado" class="form-control">
      <option *ngFor="let modulo of modulos" [value]="modulo">
        {{ modulo.toUpperCase() }}
      </option>
    </select>
  </div>

  <div class="form-group">
    <label>Nombre del Reporte:</label>
    <input 
      type="text" 
      [(ngModel)]="nombreReporte" 
      class="form-control"
      placeholder="Ej: balance_general">
  </div>

  <div class="form-group">
    <label>Formato:</label>
    <select [(ngModel)]="formatoSeleccionado" class="form-control">
      <option value="PDF">PDF</option>
      <option value="EXCEL">Excel</option>
      <option value="HTML">HTML</option>
    </select>
  </div>

  <div class="form-group">
    <label>Parámetros (JSON):</label>
    <textarea 
      [(ngModel)]="parametros" 
      class="form-control"
      rows="5"
      placeholder='{"empresaId": 1, "fecha": "2026-01-29"}'></textarea>
  </div>

  <button 
    (click)="generarReporte()" 
    [disabled]="cargando"
    class="btn btn-primary">
    <span *ngIf="!cargando">Generar Reporte</span>
    <span *ngIf="cargando">Generando...</span>
  </button>
</div>
```

### 4. Ejemplo de Uso Específico por Módulo

```typescript
// Contabilidad - Balance General
this.reporteService.generarYDescargar(
  'cnt', 
  'balance_general',
  {
    empresaId: 1,
    fechaInicio: '2026-01-01',
    fechaFin: '2026-01-31'
  },
  'PDF'
);

// Tesorería - Flujo de Caja
this.reporteService.generarYDescargar(
  'tsr', 
  'flujo_caja',
  {
    cuentaBancoId: 5,
    mes: 1,
    anio: 2026
  },
  'EXCEL'
);

// CXC - Estado de Cuenta Cliente
this.reporteService.generarYDescargar(
  'cxc', 
  'estado_cuenta_cliente',
  {
    clienteId: 123,
    fechaCorte: '2026-01-29'
  },
  'PDF'
);
```

## Configuración de CORS

Asegúrate de que WildFly tenga configurado CORS para permitir solicitudes desde Angular:

```xml
<!-- En standalone.xml o usar standalone-cors.cli -->
<subsystem xmlns="urn:jboss:domain:undertow:12.0">
    <filters>
        <response-header name="Access-Control-Allow-Origin" header-name="Access-Control-Allow-Origin" header-value="*"/>
        <response-header name="Access-Control-Allow-Methods" header-name="Access-Control-Allow-Methods" header-value="GET, POST, OPTIONS, PUT, DELETE"/>
        <response-header name="Access-Control-Allow-Headers" header-name="Access-Control-Allow-Headers" header-value="accept, content-type"/>
    </filters>
</subsystem>
```

## Consideraciones de Seguridad

1. **Autenticación**: Agrega JWT o sesión para proteger los endpoints
2. **Validación**: Los reportes deben validar permisos por módulo y usuario
3. **Rate Limiting**: Implementa límites de solicitudes para prevenir abuso
4. **Sanitización**: Valida todos los parámetros antes de pasarlos a JasperReports

## Testing

### Test del Endpoint con cURL

```bash
curl -X POST http://localhost:8080/SaaBE/rest/reportes/generar \
  -H "Content-Type: application/json" \
  -d '{
    "modulo": "cnt",
    "nombreReporte": "test",
    "formato": "PDF",
    "parametros": {
      "empresaId": 1
    }
  }' \
  --output reporte.pdf
```

### Test con Postman

1. **Method**: POST
2. **URL**: `http://localhost:8080/SaaBE/rest/reportes/generar`
3. **Headers**: `Content-Type: application/json`
4. **Body** (raw JSON):
```json
{
  "modulo": "cnt",
  "nombreReporte": "balance_general",
  "formato": "PDF",
  "parametros": {
    "empresaId": 1
  }
}
```
5. **Save Response**: Selecciona "Save to file"

## Troubleshooting

### Problema: Reporte no encontrado
- Verifica que el archivo `.jrxml` exista en `/rep/{modulo}/{nombreReporte}.jrxml`
- Revisa los logs de WildFly para ver la ruta exacta buscada

### Problema: Error de conexión a BD
- Verifica que el DataSource esté configurado correctamente
- Revisa que el JNDI del DataSource sea accesible

### Problema: CORS Bloqueado
- Aplica la configuración de CORS en WildFly
- Verifica que el origen del frontend esté permitido
