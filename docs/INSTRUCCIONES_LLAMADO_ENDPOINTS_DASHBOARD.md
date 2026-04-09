# 🔌 Instrucciones para Llamar los Endpoints del Dashboard de Aportes

## 📍 URLs de los Endpoints

**Base URL:** `http://localhost:8080/saaBE/rest/aprt`

```typescript
// 1. KPIs Globales
GET /aprt/kpis-globales?fechaDesde=2024-01-01&fechaHasta=2024-12-31&estadoAporte=1

// 2. Resumen por Tipo (Dona)
GET /aprt/resumen-por-tipo?fechaDesde=2024-01-01&fechaHasta=2024-12-31&estadoAporte=1

// 3. Top Entidades
GET /aprt/top-entidades?fechaDesde=2024-01-01&fechaHasta=2024-12-31&estadoAporte=1&tipoAporteId=1&topN=10

// 4. Top Movimientos
GET /aprt/top-movimientos?fechaDesde=2024-01-01&fechaHasta=2024-12-31&estadoAporte=1&tipoAporteId=1&topN=20
```

---

## 🎯 Interfaces TypeScript (models/aporte-dashboard.model.ts)

```typescript
export interface AporteKpiDTO {
  movimientos: number;
  tiposAporte: number;
  montoMas: number;
  montoMenos: number;
  saldoNeto: number;
}

export interface AporteResumenTipoDTO {
  tipoAporteId: number;
  tipoAporteNombre: string;
  movimientos: number;
  montoMas: number;
  montoMenos: number;
  saldoNeto: number;
  magnitudNeta: number;
  porcentajeDona: number;
}

export interface AporteTopEntidadDTO {
  tipoAporteId: number;
  entidadId: number;
  entidadNombre: string;
  movimientos: number;
  montoMas: number;
  montoMenos: number;
  saldoNeto: number;
}

export interface AporteTopMovimientoDTO {
  aporteId: number;
  tipoAporteId: number;
  tipoAporteNombre: string;
  entidadId: number;
  entidadNombre: string;
  fechaTransaccion: string; // ISO 8601: "2024-01-15T10:30:00"
  valor: number;
  magnitud: number;
}
```

---

## 🔧 Métodos en tu Servicio Angular Existente

Agrega estos métodos en tu servicio de aportes:

```typescript
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

// En tu servicio existente (ej: AporteService)

private apiUrl = 'http://localhost:8080/saaBE/rest/aprt';

/**
 * Método auxiliar para construir parámetros de query
 */
private buildQueryParams(filtros?: {
  fechaDesde?: string;
  fechaHasta?: string;
  estadoAporte?: number;
  tipoAporteId?: number;
  topN?: number;
}): HttpParams {
  let params = new HttpParams();
  
  if (filtros) {
    if (filtros.fechaDesde) {
      params = params.set('fechaDesde', filtros.fechaDesde);
    }
    if (filtros.fechaHasta) {
      params = params.set('fechaHasta', filtros.fechaHasta);
    }
    if (filtros.estadoAporte !== undefined && filtros.estadoAporte !== null) {
      params = params.set('estadoAporte', filtros.estadoAporte.toString());
    }
    if (filtros.tipoAporteId !== undefined && filtros.tipoAporteId !== null) {
      params = params.set('tipoAporteId', filtros.tipoAporteId.toString());
    }
    if (filtros.topN !== undefined && filtros.topN !== null) {
      params = params.set('topN', filtros.topN.toString());
    }
  }
  
  return params;
}

/**
 * 1. Obtiene KPIs globales del dashboard
 */
getKpisGlobales(filtros?: {
  fechaDesde?: string;
  fechaHasta?: string;
  estadoAporte?: number;
}): Observable<AporteKpiDTO> {
  const params = this.buildQueryParams(filtros);
  return this.http.get<AporteKpiDTO>(`${this.apiUrl}/kpis-globales`, { params });
}

/**
 * 2. Obtiene resumen por tipo de aporte (para gráfico de dona)
 */
getResumenPorTipo(filtros?: {
  fechaDesde?: string;
  fechaHasta?: string;
  estadoAporte?: number;
}): Observable<AporteResumenTipoDTO[]> {
  const params = this.buildQueryParams(filtros);
  return this.http.get<AporteResumenTipoDTO[]>(`${this.apiUrl}/resumen-por-tipo`, { params });
}

/**
 * 3. Obtiene top entidades con mayor impacto
 */
getTopEntidades(filtros?: {
  fechaDesde?: string;
  fechaHasta?: string;
  estadoAporte?: number;
  tipoAporteId?: number;
  topN?: number;
}): Observable<AporteTopEntidadDTO[]> {
  const params = this.buildQueryParams(filtros);
  return this.http.get<AporteTopEntidadDTO[]>(`${this.apiUrl}/top-entidades`, { params });
}

/**
 * 4. Obtiene top movimientos individuales
 */
getTopMovimientos(filtros?: {
  fechaDesde?: string;
  fechaHasta?: string;
  estadoAporte?: number;
  tipoAporteId?: number;
  topN?: number;
}): Observable<AporteTopMovimientoDTO[]> {
  const params = this.buildQueryParams(filtros);
  return this.http.get<AporteTopMovimientoDTO[]>(`${this.apiUrl}/top-movimientos`, { params });
}
```

---

## 📱 Llamado desde tu Componente

### **Opción 1: Carga Individual**

```typescript
import { Component, OnInit } from '@angular/core';

export class DashboardComponent implements OnInit {
  
  kpis: AporteKpiDTO | null = null;
  resumenTipos: AporteResumenTipoDTO[] = [];
  topEntidades: AporteTopEntidadDTO[] = [];
  topMovimientos: AporteTopMovimientoDTO[] = [];
  loading = false;

  constructor(private aporteService: AporteService) {}

  ngOnInit(): void {
    this.cargarDatos();
  }

  cargarDatos(): void {
    this.loading = true;

    // Filtros opcionales
    const filtros = {
      fechaDesde: '2024-01-01',
      fechaHasta: '2024-12-31',
      estadoAporte: 1
    };

    // 1. KPIs Globales
    this.aporteService.getKpisGlobales(filtros).subscribe({
      next: (data) => {
        this.kpis = data;
        console.log('KPIs cargados:', data);
      },
      error: (err) => console.error('Error al cargar KPIs:', err)
    });

    // 2. Resumen por Tipo
    this.aporteService.getResumenPorTipo(filtros).subscribe({
      next: (data) => {
        this.resumenTipos = data;
        console.log('Resumen por tipo:', data);
        // Aquí puedes cargar el gráfico de dona
      },
      error: (err) => console.error('Error al cargar resumen:', err)
    });

    // 3. Top Entidades
    this.aporteService.getTopEntidades({ ...filtros, topN: 10 }).subscribe({
      next: (data) => {
        this.topEntidades = data;
        console.log('Top entidades:', data);
      },
      error: (err) => console.error('Error al cargar entidades:', err)
    });

    // 4. Top Movimientos
    this.aporteService.getTopMovimientos({ ...filtros, topN: 20 }).subscribe({
      next: (data) => {
        this.topMovimientos = data;
        console.log('Top movimientos:', data);
        this.loading = false;
      },
      error: (err) => {
        console.error('Error al cargar movimientos:', err);
        this.loading = false;
      }
    });
  }
}
```

### **Opción 2: Carga Paralela con forkJoin (Recomendado)**

```typescript
import { forkJoin } from 'rxjs';
import { finalize } from 'rxjs/operators';

cargarDatos(): void {
  this.loading = true;

  const filtros = {
    fechaDesde: '2024-01-01',
    fechaHasta: '2024-12-31',
    estadoAporte: 1
  };

  forkJoin({
    kpis: this.aporteService.getKpisGlobales(filtros),
    resumen: this.aporteService.getResumenPorTipo(filtros),
    entidades: this.aporteService.getTopEntidades({ ...filtros, topN: 10 }),
    movimientos: this.aporteService.getTopMovimientos({ ...filtros, topN: 20 })
  }).pipe(
    finalize(() => this.loading = false)
  ).subscribe({
    next: (data) => {
      this.kpis = data.kpis;
      this.resumenTipos = data.resumen;
      this.topEntidades = data.entidades;
      this.topMovimientos = data.movimientos;
      console.log('Todos los datos cargados:', data);
    },
    error: (err) => {
      console.error('Error al cargar dashboard:', err);
      // Mostrar mensaje de error al usuario
    }
  });
}
```

---

## 🎨 Uso en el Template (HTML)

```html
<!-- KPIs -->
<div class="row" *ngIf="kpis">
  <div class="col-md-2">
    <div class="card">
      <div class="card-body text-center">
        <h6 class="text-muted">Movimientos</h6>
        <h3>{{ kpis.movimientos | number }}</h3>
      </div>
    </div>
  </div>
  <div class="col-md-3">
    <div class="card">
      <div class="card-body text-center">
        <h6 class="text-muted">Monto Positivo</h6>
        <h3 class="text-success">{{ kpis.montoMas | currency:'USD':'symbol':'1.2-2' }}</h3>
      </div>
    </div>
  </div>
  <div class="col-md-3">
    <div class="card">
      <div class="card-body text-center">
        <h6 class="text-muted">Monto Negativo</h6>
        <h3 class="text-danger">{{ kpis.montoMenos | currency:'USD':'symbol':'1.2-2' }}</h3>
      </div>
    </div>
  </div>
  <div class="col-md-2">
    <div class="card">
      <div class="card-body text-center">
        <h6 class="text-muted">Saldo Neto</h6>
        <h3>{{ kpis.saldoNeto | currency:'USD':'symbol':'1.2-2' }}</h3>
      </div>
    </div>
  </div>
  <div class="col-md-2">
    <div class="card">
      <div class="card-body text-center">
        <h6 class="text-muted">Tipos</h6>
        <h3>{{ kpis.tiposAporte }}</h3>
      </div>
    </div>
  </div>
</div>

<!-- Gráfico de Dona (usa resumenTipos[].porcentajeDona) -->
<div class="row mt-4">
  <div class="col-md-6">
    <div class="card">
      <div class="card-header">Distribución por Tipo de Aporte</div>
      <div class="card-body">
        <!-- Aquí tu componente de gráfico de dona -->
        <!-- Data: resumenTipos -->
        <!-- Labels: resumenTipos[].tipoAporteNombre -->
        <!-- Values: resumenTipos[].porcentajeDona -->
      </div>
    </div>
  </div>
</div>

<!-- Tabla Top Entidades -->
<div class="row mt-4">
  <div class="col-12">
    <div class="card">
      <div class="card-header">Top Entidades</div>
      <div class="card-body">
        <table class="table table-striped">
          <thead>
            <tr>
              <th>Entidad</th>
              <th>Movimientos</th>
              <th>Monto +</th>
              <th>Monto -</th>
              <th>Saldo Neto</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let item of topEntidades">
              <td>{{ item.entidadNombre }}</td>
              <td>{{ item.movimientos | number }}</td>
              <td>{{ item.montoMas | currency:'USD':'symbol':'1.2-2' }}</td>
              <td>{{ item.montoMenos | currency:'USD':'symbol':'1.2-2' }}</td>
              <td [ngClass]="item.saldoNeto >= 0 ? 'text-success' : 'text-danger'">
                {{ item.saldoNeto | currency:'USD':'symbol':'1.2-2' }}
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</div>

<!-- Tabla Top Movimientos -->
<div class="row mt-4">
  <div class="col-12">
    <div class="card">
      <div class="card-header">Top Movimientos</div>
      <div class="card-body">
        <table class="table table-striped">
          <thead>
            <tr>
              <th>Fecha</th>
              <th>Tipo Aporte</th>
              <th>Entidad</th>
              <th>Valor</th>
              <th>Magnitud</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let item of topMovimientos">
              <td>{{ item.fechaTransaccion | date:'short' }}</td>
              <td>{{ item.tipoAporteNombre }}</td>
              <td>{{ item.entidadNombre }}</td>
              <td [ngClass]="item.valor >= 0 ? 'text-success' : 'text-danger'">
                {{ item.valor | currency:'USD':'symbol':'1.2-2' }}
              </td>
              <td>{{ item.magnitud | currency:'USD':'symbol':'1.2-2' }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</div>

<!-- Loading spinner -->
<div *ngIf="loading" class="text-center my-5">
  <div class="spinner-border" role="status">
    <span class="visually-hidden">Cargando...</span>
  </div>
</div>
```

---

## 🔍 Con Filtros Dinámicos

```typescript
// En tu componente
filtros = {
  fechaDesde: '',
  fechaHasta: '',
  estadoAporte: null as number | null,
  tipoAporteId: null as number | null,
  topN: 10
};

aplicarFiltros(): void {
  // Formatear fechas a yyyy-MM-dd
  const filtrosFormateados = {
    fechaDesde: this.filtros.fechaDesde || undefined,
    fechaHasta: this.filtros.fechaHasta || undefined,
    estadoAporte: this.filtros.estadoAporte || undefined,
    tipoAporteId: this.filtros.tipoAporteId || undefined,
    topN: this.filtros.topN || 10
  };

  this.cargarDatos(filtrosFormateados);
}

limpiarFiltros(): void {
  this.filtros = {
    fechaDesde: '',
    fechaHasta: '',
    estadoAporte: null,
    tipoAporteId: null,
    topN: 10
  };
  this.cargarDatos();
}
```

```html
<!-- Formulario de filtros -->
<div class="card mb-4">
  <div class="card-body">
    <div class="row g-3">
      <div class="col-md-3">
        <label>Fecha Desde</label>
        <input type="date" class="form-control" [(ngModel)]="filtros.fechaDesde">
      </div>
      <div class="col-md-3">
        <label>Fecha Hasta</label>
        <input type="date" class="form-control" [(ngModel)]="filtros.fechaHasta">
      </div>
      <div class="col-md-2">
        <label>Estado</label>
        <select class="form-control" [(ngModel)]="filtros.estadoAporte">
          <option [ngValue]="null">Todos</option>
          <option [ngValue]="1">Activo</option>
          <option [ngValue]="0">Inactivo</option>
        </select>
      </div>
      <div class="col-md-2">
        <label>Top N</label>
        <input type="number" class="form-control" [(ngModel)]="filtros.topN" min="1" max="100">
      </div>
      <div class="col-md-2 d-flex align-items-end">
        <button class="btn btn-primary me-2" (click)="aplicarFiltros()">
          <i class="bi bi-search"></i> Buscar
        </button>
        <button class="btn btn-secondary" (click)="limpiarFiltros()">
          <i class="bi bi-x-circle"></i> Limpiar
        </button>
      </div>
    </div>
  </div>
</div>
```

---

## 📋 Ejemplos de Llamadas

### Sin filtros (todos los datos):
```typescript
this.aporteService.getKpisGlobales().subscribe(data => {
  console.log(data);
});
```

### Con rango de fechas:
```typescript
this.aporteService.getKpisGlobales({
  fechaDesde: '2024-01-01',
  fechaHasta: '2024-12-31'
}).subscribe(data => {
  console.log(data);
});
```

### Top 5 entidades de un tipo específico:
```typescript
this.aporteService.getTopEntidades({
  tipoAporteId: 1,
  topN: 5
}).subscribe(data => {
  console.log(data);
});
```

### Con todos los filtros:
```typescript
this.aporteService.getTopMovimientos({
  fechaDesde: '2024-01-01',
  fechaHasta: '2024-12-31',
  estadoAporte: 1,
  tipoAporteId: 2,
  topN: 20
}).subscribe(data => {
  console.log(data);
});
```

---

## ⚠️ Notas Importantes

1. **Formato de fechas:** Usar `yyyy-MM-dd` (ej: `2024-01-15`)
2. **Parámetros opcionales:** No enviar si son `null` o `undefined`
3. **fechaTransaccion en respuesta:** Viene como string ISO 8601, usar pipe `date`
4. **CORS:** Verificar que esté habilitado en WildFly
5. **Manejo de errores:** Implementar try-catch o catchError de RxJS

---

## ✅ Checklist

- [ ] Interfaces agregadas en models
- [ ] Métodos agregados en el servicio
- [ ] HttpClient inyectado en el servicio
- [ ] Llamadas implementadas en el componente
- [ ] Template actualizado con los datos
- [ ] Pipes aplicados (number, currency, date)
- [ ] Loading spinner implementado
- [ ] Manejo de errores implementado
- [ ] Pruebas en el navegador (F12 → Network)

---

## 🚀 Testing Rápido

En la consola del navegador (F12), ejecuta:

```javascript
// Verificar que las llamadas se hagan
// Deberías ver 4 requests a:
// - /aprt/kpis-globales
// - /aprt/resumen-por-tipo
// - /aprt/top-entidades
// - /aprt/top-movimientos
```

**¡Listo para implementar! 🎉**
