# 📋 Instrucciones para Consumir Endpoints de Dashboard de Entidades desde Angular

**Fecha:** 2026-04-08  
**Módulo:** Dashboard de Entidades  
**Base URL:** `http://localhost:8080/saaBE/rest/entd`

---

## 🎯 **Endpoints Disponibles**

### **1. Resumen de Entidades por Estado**
### **2. Resumen de Préstamos por Estado**
### **3. Resumen de Aportes por Estado**
### **4. Resumen Consolidado por Estado** ⭐ (Recomendado)

---

## 📦 **1. Interfaces TypeScript**

Crea el archivo `src/app/models/entidad-dashboard.model.ts`:

```typescript
export interface EntidadResumenEstadoDTO {
  estadoId: number;
  totalEntidades: number;
}

export interface EntidadResumenPrestamosDTO {
  estadoId: number;
  totalPrestamos: number;
}

export interface EntidadResumenAportesDTO {
  estadoId: number;
  totalAportes: number;
}

export interface EntidadResumenConsolidadoDTO {
  estadoId: number;
  totalEntidades: number;
  totalPrestamos: number;
  totalAportes: number;
}

export interface EntidadDashboardFiltros {
  estados?: string; // Formato: "10,2,30"
}
```

---

## 🔧 **2. Servicio Angular**

Crea o actualiza `src/app/services/entidad-dashboard.service.ts`:

```typescript
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  EntidadResumenEstadoDTO,
  EntidadResumenPrestamosDTO,
  EntidadResumenAportesDTO,
  EntidadResumenConsolidadoDTO,
  EntidadDashboardFiltros
} from '../models/entidad-dashboard.model';

@Injectable({
  providedIn: 'root'
})
export class EntidadDashboardService {
  
  private apiUrl = `${environment.apiUrl}/entd`;

  constructor(private http: HttpClient) {}

  /**
   * Método auxiliar para construir parámetros de query
   */
  private buildParams(filtros?: EntidadDashboardFiltros): HttpParams {
    let params = new HttpParams();
    if (filtros?.estados) {
      params = params.set('estados', filtros.estados);
    }
    return params;
  }

  /**
   * 1. Obtiene resumen de cantidad de entidades por estado
   */
  getResumenPorEstado(filtros?: EntidadDashboardFiltros): Observable<EntidadResumenEstadoDTO[]> {
    return this.http.get<EntidadResumenEstadoDTO[]>(
      `${this.apiUrl}/resumen-por-estado`,
      { params: this.buildParams(filtros) }
    );
  }

  /**
   * 2. Obtiene resumen de total de préstamos por estado de entidad
   */
  getResumenPrestamosPorEstado(filtros?: EntidadDashboardFiltros): Observable<EntidadResumenPrestamosDTO[]> {
    return this.http.get<EntidadResumenPrestamosDTO[]>(
      `${this.apiUrl}/resumen-prestamos-por-estado`,
      { params: this.buildParams(filtros) }
    );
  }

  /**
   * 3. Obtiene resumen de total de aportes por estado de entidad
   */
  getResumenAportesPorEstado(filtros?: EntidadDashboardFiltros): Observable<EntidadResumenAportesDTO[]> {
    return this.http.get<EntidadResumenAportesDTO[]>(
      `${this.apiUrl}/resumen-aportes-por-estado`,
      { params: this.buildParams(filtros) }
    );
  }

  /**
   * 4. Obtiene resumen consolidado (entidades, préstamos y aportes) por estado
   * ⭐ RECOMENDADO: Un solo endpoint que trae toda la información
   */
  getResumenConsolidadoPorEstado(filtros?: EntidadDashboardFiltros): Observable<EntidadResumenConsolidadoDTO[]> {
    return this.http.get<EntidadResumenConsolidadoDTO[]>(
      `${this.apiUrl}/resumen-consolidado-por-estado`,
      { params: this.buildParams(filtros) }
    );
  }
}
```

---

## 📱 **3. Componente TypeScript**

En tu componente de dashboard (ej: `entidad-dashboard.component.ts`):

### **Opción A: Usar el Endpoint Consolidado (⭐ Recomendado)**

```typescript
import { Component, OnInit } from '@angular/core';
import { EntidadDashboardService } from '../../services/entidad-dashboard.service';
import { EntidadResumenConsolidadoDTO } from '../../models/entidad-dashboard.model';

@Component({
  selector: 'app-entidad-dashboard',
  templateUrl: './entidad-dashboard.component.html',
  styleUrls: ['./entidad-dashboard.component.css']
})
export class EntidadDashboardComponent implements OnInit {
  
  resumenConsolidado: EntidadResumenConsolidadoDTO[] = [];
  loading = false;
  error: string | null = null;

  // Mapeo de IDs de estado a nombres legibles
  estadosMap: { [key: number]: string } = {
    2: 'Inactivo',
    10: 'Activo',
    30: 'Suspendido'
  };

  constructor(private dashboardService: EntidadDashboardService) {}

  ngOnInit(): void {
    this.cargarDashboard();
  }

  /**
   * Carga el dashboard con el endpoint consolidado (una sola llamada)
   */
  cargarDashboard(estados?: string): void {
    this.loading = true;
    this.error = null;

    const filtros = estados ? { estados } : undefined;

    this.dashboardService.getResumenConsolidadoPorEstado(filtros).subscribe({
      next: (data) => {
        this.resumenConsolidado = data;
        console.log('Dashboard consolidado cargado:', data);
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Error al cargar el dashboard';
        console.error('Error:', err);
        this.loading = false;
      }
    });
  }

  /**
   * Obtiene el nombre del estado por ID
   */
  getNombreEstado(estadoId: number): string {
    return this.estadosMap[estadoId] || `Estado ${estadoId}`;
  }

  /**
   * Aplicar filtros personalizados
   */
  aplicarFiltros(): void {
    // Ejemplo: Solo mostrar estados activos y suspendidos
    this.cargarDashboard('10,30');
  }
}
```

### **Opción B: Usar Endpoints Individuales con forkJoin**

```typescript
import { forkJoin } from 'rxjs';
import { finalize } from 'rxjs/operators';

cargarDashboardIndividual(): void {
  this.loading = true;
  this.error = null;

  const filtros = { estados: '10,2,30' };

  forkJoin({
    entidades: this.dashboardService.getResumenPorEstado(filtros),
    prestamos: this.dashboardService.getResumenPrestamosPorEstado(filtros),
    aportes: this.dashboardService.getResumenAportesPorEstado(filtros)
  }).pipe(
    finalize(() => this.loading = false)
  ).subscribe({
    next: (data) => {
      console.log('Entidades:', data.entidades);
      console.log('Préstamos:', data.prestamos);
      console.log('Aportes:', data.aportes);
      
      // Combinar los datos manualmente si es necesario
      this.procesarDatos(data);
    },
    error: (err) => {
      this.error = 'Error al cargar el dashboard';
      console.error('Error:', err);
    }
  });
}
```

---

## 🎨 **4. Template HTML**

En tu archivo `entidad-dashboard.component.html`:

### **Opción con Cards de Bootstrap:**

```html
<!-- Loading Spinner -->
<div *ngIf="loading" class="text-center my-5">
  <div class="spinner-border text-primary" role="status">
    <span class="visually-hidden">Cargando...</span>
  </div>
</div>

<!-- Mensaje de Error -->
<div *ngIf="error" class="alert alert-danger" role="alert">
  {{ error }}
</div>

<!-- Dashboard Consolidado -->
<div *ngIf="!loading && !error" class="container-fluid">
  
  <!-- Título -->
  <div class="row mb-4">
    <div class="col-12">
      <h2>Dashboard de Entidades por Estado</h2>
    </div>
  </div>

  <!-- Cards por Estado -->
  <div class="row">
    <div class="col-md-4" *ngFor="let item of resumenConsolidado">
      <div class="card mb-4 shadow-sm">
        <div class="card-header bg-primary text-white">
          <h5 class="mb-0">
            <i class="bi bi-building"></i>
            {{ getNombreEstado(item.estadoId) }}
          </h5>
        </div>
        <div class="card-body">
          
          <!-- Total Entidades -->
          <div class="mb-3">
            <div class="d-flex justify-content-between align-items-center">
              <span class="text-muted">Total Entidades:</span>
              <h4 class="mb-0 text-primary">
                {{ item.totalEntidades | number }}
              </h4>
            </div>
          </div>

          <hr>

          <!-- Total Préstamos -->
          <div class="mb-3">
            <div class="d-flex justify-content-between align-items-center">
              <span class="text-muted">
                <i class="bi bi-cash-coin"></i> Préstamos:
              </span>
              <h5 class="mb-0 text-success">
                {{ item.totalPrestamos | currency:'USD':'symbol':'1.2-2' }}
              </h5>
            </div>
          </div>

          <!-- Total Aportes -->
          <div class="mb-3">
            <div class="d-flex justify-content-between align-items-center">
              <span class="text-muted">
                <i class="bi bi-piggy-bank"></i> Aportes:
              </span>
              <h5 class="mb-0 text-info">
                {{ item.totalAportes | currency:'USD':'symbol':'1.2-2' }}
              </h5>
            </div>
          </div>

          <!-- Balance Total -->
          <hr>
          <div class="d-flex justify-content-between align-items-center">
            <strong>Balance Total:</strong>
            <h4 class="mb-0" 
                [ngClass]="(item.totalPrestamos - item.totalAportes) >= 0 ? 'text-success' : 'text-danger'">
              {{ (item.totalPrestamos - item.totalAportes) | currency:'USD':'symbol':'1.2-2' }}
            </h4>
          </div>

        </div>
        <div class="card-footer text-muted">
          <small>Estado ID: {{ item.estadoId }}</small>
        </div>
      </div>
    </div>
  </div>

  <!-- Botón para actualizar -->
  <div class="row mt-3">
    <div class="col-12">
      <button class="btn btn-primary" (click)="cargarDashboard()">
        <i class="bi bi-arrow-clockwise"></i> Actualizar
      </button>
      <button class="btn btn-secondary ms-2" (click)="aplicarFiltros()">
        <i class="bi bi-filter"></i> Filtrar
      </button>
    </div>
  </div>

</div>
```

### **Opción con Tabla:**

```html
<div class="table-responsive" *ngIf="!loading && !error">
  <table class="table table-striped table-hover">
    <thead class="table-dark">
      <tr>
        <th>Estado</th>
        <th class="text-end">Total Entidades</th>
        <th class="text-end">Total Préstamos</th>
        <th class="text-end">Total Aportes</th>
        <th class="text-end">Balance</th>
      </tr>
    </thead>
    <tbody>
      <tr *ngFor="let item of resumenConsolidado">
        <td>
          <span class="badge" 
                [ngClass]="{
                  'bg-success': item.estadoId === 10,
                  'bg-secondary': item.estadoId === 2,
                  'bg-warning': item.estadoId === 30
                }">
            {{ getNombreEstado(item.estadoId) }}
          </span>
        </td>
        <td class="text-end">
          <strong>{{ item.totalEntidades | number }}</strong>
        </td>
        <td class="text-end text-success">
          {{ item.totalPrestamos | currency:'USD':'symbol':'1.2-2' }}
        </td>
        <td class="text-end text-info">
          {{ item.totalAportes | currency:'USD':'symbol':'1.2-2' }}
        </td>
        <td class="text-end">
          <strong [ngClass]="(item.totalPrestamos - item.totalAportes) >= 0 ? 'text-success' : 'text-danger'">
            {{ (item.totalPrestamos - item.totalAportes) | currency:'USD':'symbol':'1.2-2' }}
          </strong>
        </td>
      </tr>
    </tbody>
    <tfoot class="table-light">
      <tr>
        <td><strong>TOTAL</strong></td>
        <td class="text-end">
          <strong>{{ calcularTotalEntidades() | number }}</strong>
        </td>
        <td class="text-end text-success">
          <strong>{{ calcularTotalPrestamos() | currency:'USD':'symbol':'1.2-2' }}</strong>
        </td>
        <td class="text-end text-info">
          <strong>{{ calcularTotalAportes() | currency:'USD':'symbol':'1.2-2' }}</strong>
        </td>
        <td class="text-end">
          <strong [ngClass]="calcularBalance() >= 0 ? 'text-success' : 'text-danger'">
            {{ calcularBalance() | currency:'USD':'symbol':'1.2-2' }}
          </strong>
        </td>
      </tr>
    </tfoot>
  </table>
</div>
```

**Métodos para calcular totales:**

```typescript
calcularTotalEntidades(): number {
  return this.resumenConsolidado.reduce((sum, item) => sum + item.totalEntidades, 0);
}

calcularTotalPrestamos(): number {
  return this.resumenConsolidado.reduce((sum, item) => sum + item.totalPrestamos, 0);
}

calcularTotalAportes(): number {
  return this.resumenConsolidado.reduce((sum, item) => sum + item.totalAportes, 0);
}

calcularBalance(): number {
  return this.calcularTotalPrestamos() - this.calcularTotalAportes();
}
```

---

## 📊 **5. Gráficos con Chart.js (Opcional)**

Si quieres mostrar gráficos, instala:

```bash
npm install chart.js ng2-charts
```

**En el componente:**

```typescript
import { ChartConfiguration } from 'chart.js';

// Configuración del gráfico de barras
public barChartData: ChartConfiguration<'bar'>['data'] = {
  labels: [],
  datasets: [
    { data: [], label: 'Préstamos', backgroundColor: '#28a745' },
    { data: [], label: 'Aportes', backgroundColor: '#17a2b8' }
  ]
};

cargarDashboard(): void {
  this.dashboardService.getResumenConsolidadoPorEstado().subscribe({
    next: (data) => {
      this.resumenConsolidado = data;
      
      // Actualizar datos del gráfico
      this.barChartData.labels = data.map(item => this.getNombreEstado(item.estadoId));
      this.barChartData.datasets[0].data = data.map(item => item.totalPrestamos);
      this.barChartData.datasets[1].data = data.map(item => item.totalAportes);
    }
  });
}
```

**En el HTML:**

```html
<div class="card">
  <div class="card-header">Gráfico Comparativo</div>
  <div class="card-body">
    <canvas baseChart
            [data]="barChartData"
            [type]="'bar'">
    </canvas>
  </div>
</div>
```

---

## 🔍 **6. Ejemplos de Llamadas HTTP**

### **Sin filtros (usa estados por defecto: 10, 2, 30):**

```typescript
this.dashboardService.getResumenConsolidadoPorEstado().subscribe(data => {
  console.log(data);
});
```

**URL generada:**
```
GET http://localhost:8080/saaBE/rest/entd/resumen-consolidado-por-estado
```

### **Con estados personalizados:**

```typescript
const filtros = { estados: '10,30' }; // Solo activos y suspendidos
this.dashboardService.getResumenConsolidadoPorEstado(filtros).subscribe(data => {
  console.log(data);
});
```

**URL generada:**
```
GET http://localhost:8080/saaBE/rest/entd/resumen-consolidado-por-estado?estados=10,30
```

---

## 📋 **7. Mapeo de Estados Comunes**

Según tu sistema, los estados típicos son:

```typescript
export const ESTADOS_ENTIDAD = {
  INACTIVO: 2,
  ACTIVO: 10,
  SUSPENDIDO: 30
};

export const ESTADOS_NOMBRES = {
  2: 'Inactivo',
  10: 'Activo',
  30: 'Suspendido'
};

// Colores para badges/chips
export const ESTADOS_COLORES = {
  2: 'secondary',   // Gris
  10: 'success',    // Verde
  30: 'warning'     // Amarillo
};
```

---

## 🎯 **8. Casos de Uso Completos**

### **Caso 1: Dashboard Simple (Solo totales)**

```typescript
cargarDashboardSimple(): void {
  this.dashboardService.getResumenConsolidadoPorEstado().subscribe({
    next: (data) => {
      // Mostrar en 3 cards
      this.totales = {
        entidades: data.reduce((sum, item) => sum + item.totalEntidades, 0),
        prestamos: data.reduce((sum, item) => sum + item.totalPrestamos, 0),
        aportes: data.reduce((sum, item) => sum + item.totalAportes, 0)
      };
    }
  });
}
```

### **Caso 2: Dashboard por Estado con Filtros**

```typescript
filtrarPorEstado(estadoId: number): void {
  const filtros = { estados: estadoId.toString() };
  
  this.dashboardService.getResumenConsolidadoPorEstado(filtros).subscribe({
    next: (data) => {
      this.resumenFiltrado = data[0]; // Solo un estado
    }
  });
}
```

### **Caso 3: Comparativa Múltiples Estados**

```typescript
compararEstados(): void {
  const filtros = { estados: '10,2,30' };
  
  this.dashboardService.getResumenConsolidadoPorEstado(filtros).subscribe({
    next: (data) => {
      // Crear gráfico comparativo
      this.crearGraficoComparativo(data);
    }
  });
}
```

---

## ⚠️ **9. Notas Importantes**

1. **Estados por defecto:** Si no envías parámetros, el backend usa `10, 2, 30`
2. **Formato de estados:** String separado por comas: `"10,2,30"`
3. **Valores NULL:** El backend retorna `0` si no hay datos
4. **CORS:** Verificar configuración en WildFly si Angular corre en otro puerto
5. **Environment:** Configurar `apiUrl` en `environment.ts`

---

## ✅ **10. Checklist de Implementación**

- [ ] Crear interfaces en `models/entidad-dashboard.model.ts`
- [ ] Crear servicio en `services/entidad-dashboard.service.ts`
- [ ] Configurar `environment.apiUrl`
- [ ] Importar `HttpClientModule` en tu módulo
- [ ] Crear componente de dashboard
- [ ] Implementar template HTML con cards o tabla
- [ ] Aplicar pipes de formato (number, currency)
- [ ] Implementar manejo de loading y errores
- [ ] Probar en el navegador (F12 → Network)
- [ ] Verificar respuestas JSON

---

## 🚀 **11. URLs Completas de los Endpoints**

```javascript
// 1. Solo entidades
GET http://localhost:8080/saaBE/rest/entd/resumen-por-estado

// 2. Solo préstamos
GET http://localhost:8080/saaBE/rest/entd/resumen-prestamos-por-estado

// 3. Solo aportes
GET http://localhost:8080/saaBE/rest/entd/resumen-aportes-por-estado

// 4. Consolidado (⭐ Recomendado)
GET http://localhost:8080/saaBE/rest/entd/resumen-consolidado-por-estado

// Con parámetros
GET http://localhost:8080/saaBE/rest/entd/resumen-consolidado-por-estado?estados=10,30
```

---

## 💡 **12. Recomendación Final**

Para el dashboard, **usa el endpoint consolidado** (`/resumen-consolidado-por-estado`) porque:

✅ **Una sola llamada HTTP** en lugar de 3  
✅ **Reduce latencia** y carga del servidor  
✅ **Datos consistentes** (misma transacción)  
✅ **Más fácil de mantener** en el frontend  

---

**¡Todo listo para implementar el dashboard de entidades en Angular!** 🎉
