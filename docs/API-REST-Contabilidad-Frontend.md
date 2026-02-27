# API REST - Módulo de Contabilidad
## Documentación para Frontend Angular

**Fecha:** 2026-02-27  
**Versión:** 1.0  
**Base URL:** `http://localhost:8080/saaBE/api/cnt`

---

## Índice

1. [Configuración en Angular](#configuración-en-angular)
2. [Mayor Analítico (Libro Mayor)](#mayor-analítico-libro-mayor)
3. [Balance Contable](#balance-contable-reportes-temporales)
4. [Modelos TypeScript](#modelos-typescript)
5. [Ejemplos de Uso](#ejemplos-de-uso)

---

## Configuración en Angular

### 1. Crear el servicio base para HTTP

```typescript
// src/app/core/services/api.service.ts
import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  
  private baseUrl = environment.apiUrl; // http://localhost:8080/saaBE/api
  
  constructor(private http: HttpClient) {}
  
  private getHeaders(): HttpHeaders {
    return new HttpHeaders({
      'Content-Type': 'application/json',
      'Accept': 'application/json'
    });
  }
  
  get<T>(path: string): Observable<T> {
    return this.http.get<T>(`${this.baseUrl}${path}`, { headers: this.getHeaders() });
  }
  
  post<T>(path: string, body: any): Observable<T> {
    return this.http.post<T>(`${this.baseUrl}${path}`, body, { headers: this.getHeaders() });
  }
  
  put<T>(path: string, body: any): Observable<T> {
    return this.http.put<T>(`${this.baseUrl}${path}`, body, { headers: this.getHeaders() });
  }
  
  delete<T>(path: string): Observable<T> {
    return this.http.delete<T>(`${this.baseUrl}${path}`, { headers: this.getHeaders() });
  }
}
```

### 2. Configurar environment

```typescript
// src/environments/environment.ts
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/saaBE/api'
};
```

---

## Mayor Analítico (Libro Mayor)

**Base Path:** `/cnt/myan`

### 📌 Endpoints Disponibles

#### 1. **Generar Reporte de Mayor Analítico**

Genera un reporte detallado línea por línea de los movimientos contables (Libro Mayor).

**Endpoint:**
```
POST /cnt/myan/generarReporte
```

**Request Body (ParametrosMayorAnalitico):**
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
- `fechaInicio` (string, requerido): Fecha inicial del periodo en formato YYYY-MM-DD
- `fechaFin` (string, requerido): Fecha final del periodo en formato YYYY-MM-DD
- `empresa` (number, requerido): ID de la empresa
- `cuentaInicio` (string, opcional): Cuenta contable inicial
- `cuentaFin` (string, opcional): Cuenta contable final
- `tipoDistribucion` (number, default: 0):
  - `0` = Sin centro de costo
  - `1` = Centro de costo por cuenta contable
  - `2` = Cuenta contable por centro de costo
- `centroInicio` (string, opcional): Centro de costo inicial
- `centroFin` (string, opcional): Centro de costo final
- `tipoAcumulacion` (number, default: 0):
  - `0` = Sin acumular
  - `1` = Acumulado

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
  "mensaje": "Error al generar mayor analítico: [descripción del error]",
  "exitoso": false
}
```

#### 2. **Consultar Cabeceras del Reporte**

**Endpoint:**
```
GET /cnt/myan/resultado/{secuencialReporte}
```

**Response:**
```json
[
  {
    "codigo": 1,
    "secuencia": 12345,
    "planCuenta": { "codigo": 10, "cuentaContable": "1101", "nombre": "Caja General" },
    "centroCosto": null,
    "saldoAnterior": 1000.00,
    "debe": 5000.00,
    "haber": 3000.00,
    "saldoFinal": 3000.00
  }
]
```

#### 3. **Consultar Detalles de una Cabecera**

**Endpoint:**
```
GET /cnt/myan/detalle/{idMayorAnalitico}
```

**Response:**
```json
[
  {
    "codigo": 1,
    "mayorAnalitico": { "codigo": 1 },
    "fecha": "2026-01-15",
    "documento": "F001-123",
    "descripcion": "Venta de mercadería",
    "debe": 500.00,
    "haber": 0.00,
    "saldo": 1500.00
  }
]
```

#### 4. **Listar Todos los Mayores Analíticos**

**Endpoint:**
```
GET /cnt/myan/getAll
```

#### 5. **Obtener Mayor Analítico por ID**

**Endpoint:**
```
GET /cnt/myan/getId/{id}
```

#### 6. **Búsqueda por Criterios**

**Endpoint:**
```
POST /cnt/myan/selectByCriteria
```

**Request Body:**
```json
[
  {
    "campo": "secuencia",
    "operador": "=",
    "valor": "12345"
  }
]
```

---

## Balance Contable (Reportes Temporales)

**Base Path:** `/cnt/tempReportes`

### 📌 Endpoints Disponibles

#### 1. **Generar Balance Contable**

Genera un balance contable con saldos por cuenta para un periodo específico.

**Endpoint:**
```
POST /cnt/tempReportes/generarBalance
```

**Request Body (ParametrosBalance):**
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
- `fechaInicio` (string, requerido): Fecha inicial en formato YYYY-MM-DD
- `fechaFin` (string, requerido): Fecha final en formato YYYY-MM-DD
- `empresa` (number, requerido): ID de la empresa
- `codigoAlterno` (number, requerido): ID de la parametrización del reporte
- `acumulacion` (number, default: 0):
  - `0` = Periodo
  - `1` = Acumulado
- `incluyeCentrosCosto` (boolean, default: false): Incluir centros de costo
- `reporteDistribuido` (boolean, default: false): Reporte plan por centros
- `eliminarSaldosCero` (boolean, default: true): Eliminar cuentas con saldo cero

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
  "mensaje": "Error: [descripción del error]",
  "exitoso": false
}
```

#### 2. **Consultar Datos del Balance Generado**

**Endpoint:**
```
GET /cnt/tempReportes/resultado/{idEjecucion}
```

**Response:**
```json
[
  {
    "codigo": 1,
    "secuencia": 54321,
    "planCuenta": { "codigo": 10, "cuentaContable": "1101", "nombre": "Caja General" },
    "saldoCuenta": 1000.00,
    "valorDebe": 5000.00,
    "valorHaber": 3000.00,
    "saldoFinal": 3000.00,
    "valorActual": 3000.00,
    "nivel": 4,
    "tipo": 2,
    "cuentaContable": "1101",
    "nombreCuenta": "Caja General"
  }
]
```

#### 3. **Listar Todos los Reportes Temporales**

**Endpoint:**
```
GET /cnt/tempReportes/getAll
```

#### 4. **Obtener Reporte por ID**

**Endpoint:**
```
GET /cnt/tempReportes/getId/{id}
```

#### 5. **Búsqueda por Criterios**

**Endpoint:**
```
POST /cnt/tempReportes/selectByCriteria
```

#### 6. **Eliminar Balance Generado**

**Endpoint:**
```
DELETE /cnt/tempReportes/eliminar/{idEjecucion}
```

---

## Modelos TypeScript

### Para Mayor Analítico

```typescript
// src/app/models/mayor-analitico.model.ts

export interface ParametrosMayorAnalitico {
  fechaInicio: string; // formato: 'YYYY-MM-DD'
  fechaFin: string;
  empresa: number;
  cuentaInicio?: string;
  cuentaFin?: string;
  tipoDistribucion?: number; // 0, 1, 2
  centroInicio?: string;
  centroFin?: string;
  tipoAcumulacion?: number; // 0, 1
}

export interface RespuestaMayorAnalitico {
  secuencialReporte: number;
  totalCabeceras: number;
  totalDetalles: number;
  fechaProceso: string;
  mensaje: string;
  exitoso: boolean;
}

export interface MayorAnalitico {
  codigo: number;
  secuencia: number;
  planCuenta: {
    codigo: number;
    cuentaContable: string;
    nombre: string;
  };
  centroCosto?: {
    codigo: number;
    numero: string;
    nombre: string;
  };
  saldoAnterior: number;
  debe: number;
  haber: number;
  saldoFinal: number;
}

export interface DetalleMayorAnalitico {
  codigo: number;
  mayorAnalitico: { codigo: number };
  fecha: string;
  documento: string;
  descripcion: string;
  debe: number;
  haber: number;
  saldo: number;
}
```

### Para Balance

```typescript
// src/app/models/balance.model.ts

export interface ParametrosBalance {
  fechaInicio: string; // formato: 'YYYY-MM-DD'
  fechaFin: string;
  empresa: number;
  codigoAlterno: number;
  acumulacion?: number; // 0 o 1
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

export interface TempReportes {
  codigo: number;
  secuencia: number;
  planCuenta: {
    codigo: number;
    cuentaContable: string;
    nombre: string;
  };
  saldoCuenta: number;
  valorDebe: number;
  valorHaber: number;
  saldoFinal: number;
  valorActual: number;
  nivel: number;
  tipo: number; // 1 = Acumulación, 2 = Movimiento
  cuentaContable: string;
  nombreCuenta: string;
  codigoCuentaPadre?: number;
  centroCosto?: {
    codigo: number;
    numero: string;
    nombre: string;
  };
}
```

---

## Ejemplos de Uso

### Servicio Angular para Mayor Analítico

```typescript
// src/app/services/mayor-analitico.service.ts
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../core/services/api.service';
import { 
  ParametrosMayorAnalitico, 
  RespuestaMayorAnalitico,
  MayorAnalitico,
  DetalleMayorAnalitico 
} from '../models/mayor-analitico.model';

@Injectable({
  providedIn: 'root'
})
export class MayorAnaliticoService {
  
  private basePath = '/cnt/myan';
  
  constructor(private api: ApiService) {}
  
  /**
   * Genera un reporte de Mayor Analítico
   */
  generarReporte(parametros: ParametrosMayorAnalitico): Observable<RespuestaMayorAnalitico> {
    return this.api.post<RespuestaMayorAnalitico>(`${this.basePath}/generarReporte`, parametros);
  }
  
  /**
   * Obtiene las cabeceras del reporte generado
   */
  obtenerCabeceras(secuencialReporte: number): Observable<MayorAnalitico[]> {
    return this.api.get<MayorAnalitico[]>(`${this.basePath}/resultado/${secuencialReporte}`);
  }
  
  /**
   * Obtiene los detalles de una cabecera específica
   */
  obtenerDetalles(idMayorAnalitico: number): Observable<DetalleMayorAnalitico[]> {
    return this.api.get<DetalleMayorAnalitico[]>(`${this.basePath}/detalle/${idMayorAnalitico}`);
  }
  
  /**
   * Lista todos los mayores analíticos
   */
  listarTodos(): Observable<MayorAnalitico[]> {
    return this.api.get<MayorAnalitico[]>(`${this.basePath}/getAll`);
  }
  
  /**
   * Obtiene un mayor analítico por ID
   */
  obtenerPorId(id: number): Observable<MayorAnalitico> {
    return this.api.get<MayorAnalitico>(`${this.basePath}/getId/${id}`);
  }
}
```

### Servicio Angular para Balance

```typescript
// src/app/services/balance.service.ts
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../core/services/api.service';
import { 
  ParametrosBalance, 
  RespuestaBalance,
  TempReportes 
} from '../models/balance.model';

@Injectable({
  providedIn: 'root'
})
export class BalanceService {
  
  private basePath = '/cnt/tempReportes';
  
  constructor(private api: ApiService) {}
  
  /**
   * Genera un balance contable
   */
  generarBalance(parametros: ParametrosBalance): Observable<RespuestaBalance> {
    return this.api.post<RespuestaBalance>(`${this.basePath}/generarBalance`, parametros);
  }
  
  /**
   * Obtiene los datos del balance generado
   */
  obtenerDatosBalance(idEjecucion: number): Observable<TempReportes[]> {
    return this.api.get<TempReportes[]>(`${this.basePath}/resultado/${idEjecucion}`);
  }
  
  /**
   * Elimina un balance generado
   */
  eliminarBalance(idEjecucion: number): Observable<any> {
    return this.api.delete(`${this.basePath}/eliminar/${idEjecucion}`);
  }
  
  /**
   * Lista todos los reportes temporales
   */
  listarTodos(): Observable<TempReportes[]> {
    return this.api.get<TempReportes[]>(`${this.basePath}/getAll`);
  }
}
```

### Ejemplo de Componente Angular

```typescript
// src/app/components/mayor-analitico/mayor-analitico.component.ts
import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MayorAnaliticoService } from '../../services/mayor-analitico.service';
import { ParametrosMayorAnalitico, MayorAnalitico, DetalleMayorAnalitico } from '../../models/mayor-analitico.model';

@Component({
  selector: 'app-mayor-analitico',
  templateUrl: './mayor-analitico.component.html',
  styleUrls: ['./mayor-analitico.component.scss']
})
export class MayorAnaliticoComponent implements OnInit {
  
  formulario: FormGroup;
  cargando = false;
  cabeceras: MayorAnalitico[] = [];
  detalles: DetalleMayorAnalitico[] = [];
  secuencialReporte: number | null = null;
  
  constructor(
    private fb: FormBuilder,
    private mayorAnaliticoService: MayorAnaliticoService
  ) {
    this.formulario = this.fb.group({
      fechaInicio: ['', Validators.required],
      fechaFin: ['', Validators.required],
      empresa: [1, Validators.required],
      cuentaInicio: [''],
      cuentaFin: [''],
      tipoDistribucion: [0],
      centroInicio: [''],
      centroFin: [''],
      tipoAcumulacion: [0]
    });
  }
  
  ngOnInit(): void {}
  
  generarReporte(): void {
    if (this.formulario.invalid) {
      alert('Por favor complete los campos requeridos');
      return;
    }
    
    this.cargando = true;
    const parametros: ParametrosMayorAnalitico = this.formulario.value;
    
    this.mayorAnaliticoService.generarReporte(parametros).subscribe({
      next: (respuesta) => {
        if (respuesta.exitoso) {
          this.secuencialReporte = respuesta.secuencialReporte;
          alert(`Reporte generado exitosamente. ID: ${respuesta.secuencialReporte}`);
          this.cargarCabeceras(respuesta.secuencialReporte);
        } else {
          alert(`Error: ${respuesta.mensaje}`);
        }
        this.cargando = false;
      },
      error: (error) => {
        console.error('Error al generar reporte:', error);
        alert('Error al generar el reporte');
        this.cargando = false;
      }
    });
  }
  
  cargarCabeceras(secuencial: number): void {
    this.mayorAnaliticoService.obtenerCabeceras(secuencial).subscribe({
      next: (cabeceras) => {
        this.cabeceras = cabeceras;
      },
      error: (error) => {
        console.error('Error al cargar cabeceras:', error);
      }
    });
  }
  
  verDetalles(idMayorAnalitico: number): void {
    this.mayorAnaliticoService.obtenerDetalles(idMayorAnalitico).subscribe({
      next: (detalles) => {
        this.detalles = detalles;
      },
      error: (error) => {
        console.error('Error al cargar detalles:', error);
      }
    });
  }
}
```

### Ejemplo de Template HTML

```html
<!-- mayor-analitico.component.html -->
<div class="container">
  <h2>Generar Mayor Analítico (Libro Mayor)</h2>
  
  <form [formGroup]="formulario" (ngSubmit)="generarReporte()">
    <div class="row">
      <div class="col-md-6">
        <label>Fecha Inicio:</label>
        <input type="date" class="form-control" formControlName="fechaInicio">
      </div>
      <div class="col-md-6">
        <label>Fecha Fin:</label>
        <input type="date" class="form-control" formControlName="fechaFin">
      </div>
    </div>
    
    <div class="row">
      <div class="col-md-6">
        <label>Cuenta Inicio:</label>
        <input type="text" class="form-control" formControlName="cuentaInicio">
      </div>
      <div class="col-md-6">
        <label>Cuenta Fin:</label>
        <input type="text" class="form-control" formControlName="cuentaFin">
      </div>
    </div>
    
    <div class="row">
      <div class="col-md-6">
        <label>Tipo Distribución:</label>
        <select class="form-control" formControlName="tipoDistribucion">
          <option value="0">Sin centro de costo</option>
          <option value="1">Centro por cuenta</option>
          <option value="2">Cuenta por centro</option>
        </select>
      </div>
      <div class="col-md-6">
        <label>Tipo Acumulación:</label>
        <select class="form-control" formControlName="tipoAcumulacion">
          <option value="0">Sin acumular</option>
          <option value="1">Acumulado</option>
        </select>
      </div>
    </div>
    
    <button type="submit" class="btn btn-primary" [disabled]="cargando">
      {{ cargando ? 'Generando...' : 'Generar Reporte' }}
    </button>
  </form>
  
  <!-- Tabla de cabeceras -->
  <div *ngIf="cabeceras.length > 0" class="mt-4">
    <h3>Resultados</h3>
    <table class="table table-striped">
      <thead>
        <tr>
          <th>Cuenta</th>
          <th>Nombre</th>
          <th>Saldo Anterior</th>
          <th>Debe</th>
          <th>Haber</th>
          <th>Saldo Final</th>
          <th>Acciones</th>
        </tr>
      </thead>
      <tbody>
        <tr *ngFor="let cabecera of cabeceras">
          <td>{{ cabecera.planCuenta.cuentaContable }}</td>
          <td>{{ cabecera.planCuenta.nombre }}</td>
          <td>{{ cabecera.saldoAnterior | number:'1.2-2' }}</td>
          <td>{{ cabecera.debe | number:'1.2-2' }}</td>
          <td>{{ cabecera.haber | number:'1.2-2' }}</td>
          <td>{{ cabecera.saldoFinal | number:'1.2-2' }}</td>
          <td>
            <button class="btn btn-sm btn-info" (click)="verDetalles(cabecera.codigo)">
              Ver Detalles
            </button>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</div>
```

---

## Manejo de Errores

### En el Servicio

```typescript
import { catchError } from 'rxjs/operators';
import { throwError } from 'rxjs';

generarReporte(parametros: ParametrosMayorAnalitico): Observable<RespuestaMayorAnalitico> {
  return this.api.post<RespuestaMayorAnalitico>(`${this.basePath}/generarReporte`, parametros)
    .pipe(
      catchError(error => {
        console.error('Error en generarReporte:', error);
        // Aquí puedes mostrar un mensaje de error global
        return throwError(() => new Error(error.error?.mensaje || 'Error al generar reporte'));
      })
    );
}
```

### En el Componente

```typescript
this.mayorAnaliticoService.generarReporte(parametros).subscribe({
  next: (respuesta) => {
    if (respuesta.exitoso) {
      // Manejo de éxito
    } else {
      // Manejo de error de negocio
      this.mostrarError(respuesta.mensaje);
    }
  },
  error: (error) => {
    // Manejo de error técnico
    this.mostrarError(error.message);
  }
});
```

---

## Notas Importantes

1. **Fechas**: Las fechas deben enviarse en formato ISO 8601 (`YYYY-MM-DD`)
2. **Content-Type**: Siempre usar `application/json`
3. **CORS**: Asegúrate de que el backend tenga configurado CORS para permitir peticiones desde tu frontend
4. **Seguridad**: Implementa autenticación/autorización con tokens JWT si es necesario
5. **IDs de Ejecución**: Guarda los `idEjecucion` o `secuencialReporte` para consultas posteriores
6. **Manejo de Estados**: Usa loading states para mejorar la UX durante las peticiones

---

## Testing

### Ejemplo con HTTPClientTestingModule

```typescript
import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { MayorAnaliticoService } from './mayor-analitico.service';

describe('MayorAnaliticoService', () => {
  let service: MayorAnaliticoService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [MayorAnaliticoService]
    });
    service = TestBed.inject(MayorAnaliticoService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  it('debe generar reporte correctamente', () => {
    const mockParametros = {
      fechaInicio: '2026-01-01',
      fechaFin: '2026-01-31',
      empresa: 1
    };
    
    const mockRespuesta = {
      secuencialReporte: 12345,
      totalCabeceras: 25,
      totalDetalles: 150,
      fechaProceso: '2026-02-27T14:30:00',
      mensaje: 'Mayor analítico generado exitosamente',
      exitoso: true
    };

    service.generarReporte(mockParametros).subscribe(respuesta => {
      expect(respuesta.exitoso).toBe(true);
      expect(respuesta.secuencialReporte).toBe(12345);
    });

    const req = httpMock.expectOne('/cnt/myan/generarReporte');
    expect(req.request.method).toBe('POST');
    req.flush(mockRespuesta);
  });

  afterEach(() => {
    httpMock.verify();
  });
});
```

---

## Preguntas Frecuentes

**Q: ¿Qué hago si la fecha de inicio es mayor que la fecha fin?**  
A: El backend validará esto y retornará un error 400 con el mensaje apropiado.

**Q: ¿Puedo consultar los datos de un reporte antiguo?**  
A: Sí, usa el `idEjecucion` o `secuencialReporte` para consultar datos previamente generados.

**Q: ¿Los reportes se guardan permanentemente?**  
A: Sí, los datos se almacenan en las tablas DTMT (balance) y MYAN/DMYA (mayor analítico).

**Q: ¿Cómo elimino un reporte?**  
A: Usa el endpoint DELETE correspondiente pasando el ID de ejecución.

---

## Soporte

Para más información o soporte, contacta al equipo de backend.

**Actualizado:** 2026-02-27
