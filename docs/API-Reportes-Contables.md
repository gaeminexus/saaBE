# API REST - Reportes Contables (Balance y Mayor Analítico)
## Documentación para Frontend Angular

**Fecha:** 2026-02-27  
**Versión:** 1.0  
**Base URL:** `http://localhost:8080/saaBE/api/cnt`

---

## 📋 Índice

1. [Mayor Analítico - Generar Reporte](#mayor-analítico---generar-reporte)
2. [Balance Contable - Generar Balance](#balance-contable---generar-balance)
3. [Modelos TypeScript](#modelos-typescript)
4. [Servicios Angular](#servicios-angular)
5. [Ejemplo de Uso](#ejemplo-de-uso)

---

## 🔷 Mayor Analítico - Generar Reporte

**Base Path:** `/cnt/myan`

### Endpoint: Generar Reporte de Mayor Analítico

Genera un reporte detallado línea por línea de los movimientos contables (Libro Mayor).

**URL:**
```
POST /cnt/myan/generarReporte
```

**Request Body:**
```json
{
  "fechaInicio": "2026-01-01",
  "fechaFin": "2026-01-31",
  "empresa": 1,
  "cuentaInicio": "1101",
  "cuentaFin": "1199",
  "tipoDistribucion": 0,
  "centroInicio": null,
  "centroFin": null,
  "tipoAcumulacion": 0
}
```

**Parámetros:**

| Campo | Tipo | Requerido | Descripción | Valores |
|-------|------|-----------|-------------|---------|
| `fechaInicio` | string | ✅ Sí | Fecha inicial (YYYY-MM-DD) | Ej: "2026-01-01" |
| `fechaFin` | string | ✅ Sí | Fecha final (YYYY-MM-DD) | Ej: "2026-01-31" |
| `empresa` | number | ✅ Sí | ID de la empresa | Ej: 1 |
| `cuentaInicio` | string | ❌ No | Cuenta contable inicial | Ej: "1101" |
| `cuentaFin` | string | ❌ No | Cuenta contable final | Ej: "1199" |
| `tipoDistribucion` | number | ❌ No | Tipo de distribución | `0`=Sin centro, `1`=Centro por cuenta, `2`=Cuenta por centro |
| `centroInicio` | string | ❌ No | Centro de costo inicial | Ej: "CC001" |
| `centroFin` | string | ❌ No | Centro de costo final | Ej: "CC999" |
| `tipoAcumulacion` | number | ❌ No | Tipo de acumulación | `0`=Sin acumular, `1`=Acumulado |

**Response Success (201):**
```json
{
  "secuencialReporte": 12345,
  "totalCabeceras": 25,
  "totalDetalles": 150,
  "fechaProceso": "2026-02-27T14:30:00",
  "mensaje": "Mayor analítico generado exitosamente",
  "exitoso": true
}
```

**Response Error (400/500):**
```json
{
  "secuencialReporte": null,
  "totalCabeceras": null,
  "totalDetalles": null,
  "fechaProceso": "2026-02-27T14:30:00",
  "mensaje": "Error al generar mayor analítico: Las fechas de inicio y fin son obligatorias",
  "exitoso": false
}
```

**Errores Comunes:**
- `400` - Fechas no proporcionadas
- `400` - Fecha inicio mayor que fecha fin
- `400` - Empresa no proporcionada
- `500` - Error interno del servidor

---

## 🔶 Balance Contable - Generar Balance

**Base Path:** `/cnt/tempReportes`

### Endpoint: Generar Balance Contable

Genera un balance contable con saldos por cuenta para un periodo específico.

**URL:**
```
POST /cnt/tempReportes/generarBalance
```

**Request Body:**
```json
{
  "fechaInicio": "2026-01-01",
  "fechaFin": "2026-01-31",
  "empresa": 1,
  "codigoAlterno": 100,
  "acumulacion": 1,
  "incluyeCentrosCosto": false,
  "reporteDistribuido": false,
  "eliminarSaldosCero": true
}
```

**Parámetros:**

| Campo | Tipo | Requerido | Descripción | Valores |
|-------|------|-----------|-------------|---------|
| `fechaInicio` | string | ✅ Sí | Fecha inicial (YYYY-MM-DD) | Ej: "2026-01-01" |
| `fechaFin` | string | ✅ Sí | Fecha final (YYYY-MM-DD) | Ej: "2026-01-31" |
| `empresa` | number | ✅ Sí | ID de la empresa | Ej: 1 |
| `codigoAlterno` | number | ✅ Sí | ID de parametrización del reporte | Ej: 100 |
| `acumulacion` | number | ❌ No | Tipo de acumulación | `0`=Periodo, `1`=Acumulado (default: 0) |
| `incluyeCentrosCosto` | boolean | ❌ No | Incluir centros de costo | `true`/`false` (default: false) |
| `reporteDistribuido` | boolean | ❌ No | Reporte plan por centros | `true`/`false` (default: false) |
| `eliminarSaldosCero` | boolean | ❌ No | Eliminar cuentas sin saldo | `true`/`false` (default: true) |

**Response Success (201):**
```json
{
  "idEjecucion": 54321,
  "totalRegistros": 85,
  "fechaProceso": "2026-02-27T14:30:00",
  "mensaje": "Balance generado exitosamente",
  "exitoso": true
}
```

**Response Error (400/500):**
```json
{
  "idEjecucion": null,
  "totalRegistros": null,
  "fechaProceso": "2026-02-27T14:30:00",
  "mensaje": "Error: El ID de empresa es obligatorio",
  "exitoso": false
}
```

**Errores Comunes:**
- `400` - Fechas no proporcionadas
- `400` - Fecha inicio mayor que fecha fin
- `400` - Empresa no proporcionada
- `400` - Código alterno no proporcionado
- `500` - Error interno del servidor

---

## 📦 Modelos TypeScript

### Mayor Analítico

```typescript
// src/app/models/mayor-analitico.model.ts

export interface ParametrosMayorAnalitico {
  fechaInicio: string;          // formato: 'YYYY-MM-DD'
  fechaFin: string;              // formato: 'YYYY-MM-DD'
  empresa: number;
  cuentaInicio?: string;
  cuentaFin?: string;
  tipoDistribucion?: number;     // 0, 1, 2
  centroInicio?: string;
  centroFin?: string;
  tipoAcumulacion?: number;      // 0, 1
}

export interface RespuestaMayorAnalitico {
  secuencialReporte: number;
  totalCabeceras: number;
  totalDetalles: number;
  fechaProceso: string;
  mensaje: string;
  exitoso: boolean;
}
```

### Balance Contable

```typescript
// src/app/models/balance.model.ts

export interface ParametrosBalance {
  fechaInicio: string;           // formato: 'YYYY-MM-DD'
  fechaFin: string;               // formato: 'YYYY-MM-DD'
  empresa: number;
  codigoAlterno: number;
  acumulacion?: number;           // 0 o 1
  incluyeCentrosCosto?: boolean;
  reporteDistribuido?: boolean;
  eliminarSaldosCero?: boolean;
}

export interface RespuestaBalance {
  idEjecucion: number;
  totalRegistros: number;
  fechaProceso: string;
  mensaje: string;
  exitoso: boolean;
}
```

---

## 🔧 Servicios Angular

### Mayor Analítico Service

```typescript
// src/app/services/mayor-analitico.service.ts
import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class MayorAnaliticoService {
  
  private apiUrl = `${environment.apiUrl}/cnt/myan`;
  
  constructor(private http: HttpClient) {}
  
  private getHeaders(): HttpHeaders {
    return new HttpHeaders({
      'Content-Type': 'application/json',
      'Accept': 'application/json'
    });
  }
  
  /**
   * Genera un reporte de Mayor Analítico
   * @param parametros Parámetros para generar el reporte
   * @returns Observable con la respuesta del servidor
   */
  generarReporte(parametros: ParametrosMayorAnalitico): Observable<RespuestaMayorAnalitico> {
    return this.http.post<RespuestaMayorAnalitico>(
      `${this.apiUrl}/generarReporte`,
      parametros,
      { headers: this.getHeaders() }
    );
  }
}

// Interfaces
export interface ParametrosMayorAnalitico {
  fechaInicio: string;
  fechaFin: string;
  empresa: number;
  cuentaInicio?: string;
  cuentaFin?: string;
  tipoDistribucion?: number;
  centroInicio?: string;
  centroFin?: string;
  tipoAcumulacion?: number;
}

export interface RespuestaMayorAnalitico {
  secuencialReporte: number;
  totalCabeceras: number;
  totalDetalles: number;
  fechaProceso: string;
  mensaje: string;
  exitoso: boolean;
}
```

### Balance Service

```typescript
// src/app/services/balance.service.ts
import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class BalanceService {
  
  private apiUrl = `${environment.apiUrl}/cnt/tempReportes`;
  
  constructor(private http: HttpClient) {}
  
  private getHeaders(): HttpHeaders {
    return new HttpHeaders({
      'Content-Type': 'application/json',
      'Accept': 'application/json'
    });
  }
  
  /**
   * Genera un balance contable
   * @param parametros Parámetros para generar el balance
   * @returns Observable con la respuesta del servidor
   */
  generarBalance(parametros: ParametrosBalance): Observable<RespuestaBalance> {
    return this.http.post<RespuestaBalance>(
      `${this.apiUrl}/generarBalance`,
      parametros,
      { headers: this.getHeaders() }
    );
  }
}

// Interfaces
export interface ParametrosBalance {
  fechaInicio: string;
  fechaFin: string;
  empresa: number;
  codigoAlterno: number;
  acumulacion?: number;
  incluyeCentrosCosto?: boolean;
  reporteDistribuido?: boolean;
  eliminarSaldosCero?: boolean;
}

export interface RespuestaBalance {
  idEjecucion: number;
  totalRegistros: number;
  fechaProceso: string;
  mensaje: string;
  exitoso: boolean;
}
```

---

## 💡 Ejemplo de Uso

### Componente TypeScript

```typescript
// mayor-analitico.component.ts
import { Component } from '@angular/core';
import { MayorAnaliticoService, ParametrosMayorAnalitico } from './services/mayor-analitico.service';

@Component({
  selector: 'app-mayor-analitico',
  templateUrl: './mayor-analitico.component.html'
})
export class MayorAnaliticoComponent {
  
  cargando = false;
  resultado: any = null;
  
  constructor(private mayorAnaliticoService: MayorAnaliticoService) {}
  
  generarReporte() {
    const parametros: ParametrosMayorAnalitico = {
      fechaInicio: '2026-01-01',
      fechaFin: '2026-01-31',
      empresa: 1,
      cuentaInicio: '1101',
      cuentaFin: '1199',
      tipoDistribucion: 0,
      tipoAcumulacion: 0
    };
    
    this.cargando = true;
    
    this.mayorAnaliticoService.generarReporte(parametros).subscribe({
      next: (respuesta) => {
        this.cargando = false;
        
        if (respuesta.exitoso) {
          this.resultado = respuesta;
          console.log('Reporte generado con ID:', respuesta.secuencialReporte);
          console.log('Total cabeceras:', respuesta.totalCabeceras);
          console.log('Total detalles:', respuesta.totalDetalles);
          
          alert(`✅ ${respuesta.mensaje}\nID Reporte: ${respuesta.secuencialReporte}`);
        } else {
          alert(`❌ ${respuesta.mensaje}`);
        }
      },
      error: (error) => {
        this.cargando = false;
        console.error('Error al generar reporte:', error);
        alert('❌ Error al generar el reporte. Por favor intente nuevamente.');
      }
    });
  }
}
```

```typescript
// balance.component.ts
import { Component } from '@angular/core';
import { BalanceService, ParametrosBalance } from './services/balance.service';

@Component({
  selector: 'app-balance',
  templateUrl: './balance.component.html'
})
export class BalanceComponent {
  
  cargando = false;
  resultado: any = null;
  
  constructor(private balanceService: BalanceService) {}
  
  generarBalance() {
    const parametros: ParametrosBalance = {
      fechaInicio: '2026-01-01',
      fechaFin: '2026-01-31',
      empresa: 1,
      codigoAlterno: 100,
      acumulacion: 1,                    // Acumulado
      incluyeCentrosCosto: false,
      reporteDistribuido: false,
      eliminarSaldosCero: true
    };
    
    this.cargando = true;
    
    this.balanceService.generarBalance(parametros).subscribe({
      next: (respuesta) => {
        this.cargando = false;
        
        if (respuesta.exitoso) {
          this.resultado = respuesta;
          console.log('Balance generado con ID:', respuesta.idEjecucion);
          console.log('Total registros:', respuesta.totalRegistros);
          
          alert(`✅ ${respuesta.mensaje}\nID Ejecución: ${respuesta.idEjecucion}`);
        } else {
          alert(`❌ ${respuesta.mensaje}`);
        }
      },
      error: (error) => {
        this.cargando = false;
        console.error('Error al generar balance:', error);
        alert('❌ Error al generar el balance. Por favor intente nuevamente.');
      }
    });
  }
}
```

### Template HTML

```html
<!-- mayor-analitico.component.html -->
<div class="container">
  <h2>Generar Mayor Analítico</h2>
  
  <button 
    (click)="generarReporte()" 
    [disabled]="cargando"
    class="btn btn-primary">
    {{ cargando ? 'Generando...' : 'Generar Reporte' }}
  </button>
  
  <div *ngIf="resultado && resultado.exitoso" class="alert alert-success mt-3">
    <h4>✅ Reporte Generado Exitosamente</h4>
    <p><strong>ID Reporte:</strong> {{ resultado.secuencialReporte }}</p>
    <p><strong>Total Cabeceras:</strong> {{ resultado.totalCabeceras }}</p>
    <p><strong>Total Detalles:</strong> {{ resultado.totalDetalles }}</p>
    <p><strong>Fecha Proceso:</strong> {{ resultado.fechaProceso | date:'dd/MM/yyyy HH:mm' }}</p>
  </div>
</div>
```

```html
<!-- balance.component.html -->
<div class="container">
  <h2>Generar Balance Contable</h2>
  
  <button 
    (click)="generarBalance()" 
    [disabled]="cargando"
    class="btn btn-primary">
    {{ cargando ? 'Generando...' : 'Generar Balance' }}
  </button>
  
  <div *ngIf="resultado && resultado.exitoso" class="alert alert-success mt-3">
    <h4>✅ Balance Generado Exitosamente</h4>
    <p><strong>ID Ejecución:</strong> {{ resultado.idEjecucion }}</p>
    <p><strong>Total Registros:</strong> {{ resultado.totalRegistros }}</p>
    <p><strong>Fecha Proceso:</strong> {{ resultado.fechaProceso | date:'dd/MM/yyyy HH:mm' }}</p>
  </div>
</div>
```

---

## 🔍 Consultar Resultados

Después de generar los reportes, puedes consultar los datos usando los endpoints existentes:

### Mayor Analítico - Consultar Resultados

```typescript
// Obtener cabeceras del reporte generado
this.http.get(`${apiUrl}/cnt/myan/resultado/${secuencialReporte}`).subscribe(cabeceras => {
  console.log('Cabeceras:', cabeceras);
});

// Obtener detalles de una cabecera específica
this.http.get(`${apiUrl}/cnt/myan/detalle/${idMayorAnalitico}`).subscribe(detalles => {
  console.log('Detalles:', detalles);
});
```

### Balance - Consultar Resultados

```typescript
// Obtener datos del balance generado
this.http.get(`${apiUrl}/cnt/tempReportes/resultado/${idEjecucion}`).subscribe(datos => {
  console.log('Datos Balance:', datos);
});
```

---

## ⚠️ Notas Importantes

1. **Formato de Fechas:** Siempre usar formato ISO 8601: `YYYY-MM-DD` (ejemplo: `"2026-02-27"`)

2. **Headers HTTP:** 
   ```typescript
   'Content-Type': 'application/json'
   'Accept': 'application/json'
   ```

3. **Códigos HTTP:**
   - `201` - Reporte creado exitosamente
   - `200` - Consulta exitosa
   - `400` - Error de validación (revisar el campo `mensaje`)
   - `500` - Error interno del servidor

4. **IDs de Ejecución:**
   - Mayor Analítico: Guardar `secuencialReporte` para consultas posteriores
   - Balance: Guardar `idEjecucion` para consultas posteriores

5. **Validaciones del Backend:**
   - Fechas obligatorias
   - Fecha inicio no puede ser mayor que fecha fin
   - Empresa obligatoria
   - Código alterno obligatorio (solo para balance)

6. **Environment Configuration:**
   ```typescript
   // environment.ts
   export const environment = {
     production: false,
     apiUrl: 'http://localhost:8080/saaBE/api'
   };
   ```

---

## 🐛 Troubleshooting

### Error: CORS

Si obtienes errores de CORS, verifica que el backend tenga configurado:
```
Access-Control-Allow-Origin: *
Access-Control-Allow-Methods: GET, POST, PUT, DELETE
Access-Control-Allow-Headers: Content-Type, Accept
```

### Error: 400 - Bad Request

Verifica que:
- Las fechas estén en formato `YYYY-MM-DD`
- Todos los campos requeridos estén presentes
- Los tipos de datos sean correctos (números como números, no strings)

### Error: 500 - Internal Server Error

Revisa:
- Los logs del servidor backend
- La consola del navegador para más detalles
- Que la base de datos esté accesible

---

## 📞 Soporte

Para soporte adicional, contacta al equipo de backend.

**Última actualización:** 2026-02-27  
**Versión:** 1.0
