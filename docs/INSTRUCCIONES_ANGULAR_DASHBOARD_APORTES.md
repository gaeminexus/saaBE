# 📋 Instrucciones para Copilot: Dashboard de Aportes en Angular

## 🎯 Contexto
Necesito implementar un dashboard de aportes en Angular que consuma 4 endpoints REST del backend Java (Jakarta EE).

## 🔗 Endpoints Disponibles

**Base URL:** `http://localhost:8080/saaBE/rest/aprt`

### 1. KPIs Globales
```
GET /aprt/kpis-globales
Query Params: fechaDesde?, fechaHasta?, estadoAporte?
```

**Respuesta:**
```typescript
{
  movimientos: number;
  tiposAporte: number;
  montoMas: number;
  montoMenos: number;
  saldoNeto: number;
}
```

### 2. Resumen por Tipo (Dona + Tarjetas)
```
GET /aprt/resumen-por-tipo
Query Params: fechaDesde?, fechaHasta?, estadoAporte?
```

**Respuesta:**
```typescript
[
  {
    tipoAporteId: number;
    tipoAporteNombre: string;
    movimientos: number;
    montoMas: number;
    montoMenos: number;
    saldoNeto: number;
    magnitudNeta: number;
    porcentajeDona: number;
  }
]
```

### 3. Top Entidades
```
GET /aprt/top-entidades
Query Params: fechaDesde?, fechaHasta?, estadoAporte?, tipoAporteId?, topN?
```

**Respuesta:**
```typescript
[
  {
    tipoAporteId: number;
    entidadId: number;
    entidadNombre: string;
    movimientos: number;
    montoMas: number;
    montoMenos: number;
    saldoNeto: number;
  }
]
```

### 4. Top Movimientos
```
GET /aprt/top-movimientos
Query Params: fechaDesde?, fechaHasta?, estadoAporte?, tipoAporteId?, topN?
```

**Respuesta:**
```typescript
[
  {
    aporteId: number;
    tipoAporteId: number;
    tipoAporteNombre: string;
    entidadId: number;
    entidadNombre: string;
    fechaTransaccion: string; // ISO 8601
    valor: number;
    magnitud: number;
  }
]
```

---

## 🛠️ Tareas a Implementar

### **PASO 1: Crear Interfaces TypeScript**

Crea el archivo `src/app/models/aporte-dashboard.model.ts` con las siguientes interfaces:

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
  fechaTransaccion: string;
  valor: number;
  magnitud: number;
}

export interface DashboardFiltros {
  fechaDesde?: string;  // formato: yyyy-MM-dd
  fechaHasta?: string;  // formato: yyyy-MM-dd
  estadoAporte?: number;
  tipoAporteId?: number;
  topN?: number;
}
```

---

### **PASO 2: Crear Servicio Angular**

Crea el archivo `src/app/services/aporte-dashboard.service.ts` con:

- Inyección de `HttpClient`
- Base URL configurable (usar environment)
- 4 métodos observables para cada endpoint
- Manejo de parámetros opcionales con `HttpParams`
- Manejo de errores con `catchError`

**Métodos requeridos:**
```typescript
getKpisGlobales(filtros?: DashboardFiltros): Observable<AporteKpiDTO>
getResumenPorTipo(filtros?: DashboardFiltros): Observable<AporteResumenTipoDTO[]>
getTopEntidades(filtros?: DashboardFiltros): Observable<AporteTopEntidadDTO[]>
getTopMovimientos(filtros?: DashboardFiltros): Observable<AporteTopMovimientoDTO[]>
```

**Ejemplo de implementación esperada:**
```typescript
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class AporteDashboardService {
  private apiUrl = `${environment.apiUrl}/aprt`;

  constructor(private http: HttpClient) {}

  private buildParams(filtros?: DashboardFiltros): HttpParams {
    let params = new HttpParams();
    if (filtros?.fechaDesde) params = params.set('fechaDesde', filtros.fechaDesde);
    if (filtros?.fechaHasta) params = params.set('fechaHasta', filtros.fechaHasta);
    if (filtros?.estadoAporte !== undefined) params = params.set('estadoAporte', filtros.estadoAporte.toString());
    if (filtros?.tipoAporteId !== undefined) params = params.set('tipoAporteId', filtros.tipoAporteId.toString());
    if (filtros?.topN !== undefined) params = params.set('topN', filtros.topN.toString());
    return params;
  }

  getKpisGlobales(filtros?: DashboardFiltros): Observable<AporteKpiDTO> {
    return this.http.get<AporteKpiDTO>(`${this.apiUrl}/kpis-globales`, {
      params: this.buildParams(filtros)
    });
  }

  // ... implementar los otros 3 métodos igual
}
```

---

### **PASO 3: Crear Componente Dashboard**

Genera el componente `src/app/components/aporte-dashboard/aporte-dashboard.component.ts` con:

**Propiedades:**
- `kpis: AporteKpiDTO | null`
- `resumenTipos: AporteResumenTipoDTO[]`
- `topEntidades: AporteTopEntidadDTO[]`
- `topMovimientos: AporteTopMovimientoDTO[]`
- `filtros: DashboardFiltros`
- `loading: boolean`
- `error: string | null`

**Métodos:**
- `ngOnInit()`: Cargar datos iniciales
- `cargarDatos()`: Llamar a los 4 servicios en paralelo con `forkJoin`
- `aplicarFiltros()`: Recargar con filtros
- `limpiarFiltros()`: Reset

**Ejemplo de carga inicial:**
```typescript
cargarDatos(): void {
  this.loading = true;
  this.error = null;

  forkJoin({
    kpis: this.dashboardService.getKpisGlobales(this.filtros),
    resumen: this.dashboardService.getResumenPorTipo(this.filtros),
    entidades: this.dashboardService.getTopEntidades(this.filtros),
    movimientos: this.dashboardService.getTopMovimientos(this.filtros)
  }).pipe(
    finalize(() => this.loading = false)
  ).subscribe({
    next: (data) => {
      this.kpis = data.kpis;
      this.resumenTipos = data.resumen;
      this.topEntidades = data.entidades;
      this.topMovimientos = data.movimientos;
    },
    error: (err) => {
      this.error = 'Error al cargar el dashboard';
      console.error(err);
    }
  });
}
```

---

### **PASO 4: Crear Template HTML**

En `aporte-dashboard.component.html`, crear estructura con:

**Sección 1: Filtros**
- DatePickers para fechaDesde/fechaHasta
- Select para estadoAporte
- Select para tipoAporteId
- Input numérico para topN
- Botones: Aplicar, Limpiar

**Sección 2: Cards de KPIs** (usar Bootstrap Cards o Material Cards)
```html
<div class="row" *ngIf="kpis">
  <div class="col-md-2">
    <div class="card kpi-card">
      <div class="card-body">
        <h6>Movimientos</h6>
        <h3>{{ kpis.movimientos | number }}</h3>
      </div>
    </div>
  </div>
  <!-- Repetir para: tiposAporte, montoMas, montoMenos, saldoNeto -->
</div>
```

**Sección 3: Gráfico de Dona**
- Usar `resumenTipos` (campo `porcentajeDona`)
- Librería sugerida: `ng2-charts` o `ngx-charts`
- Mostrar leyenda con `tipoAporteNombre` y valores

**Sección 4: Tabla Top Entidades**
```html
<table class="table table-striped">
  <thead>
    <tr>
      <th>Entidad</th>
      <th>Tipo Aporte</th>
      <th>Movimientos</th>
      <th>Monto +</th>
      <th>Monto -</th>
      <th>Saldo Neto</th>
    </tr>
  </thead>
  <tbody>
    <tr *ngFor="let item of topEntidades">
      <td>{{ item.entidadNombre }}</td>
      <td>{{ item.tipoAporteId }}</td>
      <td>{{ item.movimientos }}</td>
      <td>{{ item.montoMas | currency }}</td>
      <td>{{ item.montoMenos | currency }}</td>
      <td>{{ item.saldoNeto | currency }}</td>
    </tr>
  </tbody>
</table>
```

**Sección 5: Tabla Top Movimientos**
- Similar a la anterior
- Agregar columna `fechaTransaccion` con pipe `date`

---

### **PASO 5: Configuración Environment**

En `src/environments/environment.ts`:
```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/saaBE/rest'
};
```

---

### **PASO 6: Módulo (si no usa standalone)**

En `app.module.ts` o en el módulo correspondiente:
```typescript
import { HttpClientModule } from '@angular/common/http';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

@NgModule({
  imports: [
    HttpClientModule,
    FormsModule,
    ReactiveFormsModule,
    // ... otros módulos
  ]
})
```

---

## 📦 Dependencias Opcionales

Si quieres gráficos más avanzados:

```bash
npm install ng2-charts chart.js
npm install @angular/material @angular/cdk
npm install ngx-charts @swimlane/ngx-charts
```

---

## 🔥 Ejemplo de Uso de los Filtros

```typescript
// Sin filtros
this.dashboardService.getKpisGlobales().subscribe(data => { ... });

// Con rango de fechas
const filtros: DashboardFiltros = {
  fechaDesde: '2024-01-01',
  fechaHasta: '2024-12-31',
  estadoAporte: 1
};
this.dashboardService.getKpisGlobales(filtros).subscribe(data => { ... });

// Top 5 entidades de un tipo específico
const filtros: DashboardFiltros = {
  tipoAporteId: 1,
  topN: 5
};
this.dashboardService.getTopEntidades(filtros).subscribe(data => { ... });
```

---

## ✅ Checklist Final

- [ ] Interfaces creadas en `models/aporte-dashboard.model.ts`
- [ ] Servicio creado en `services/aporte-dashboard.service.ts`
- [ ] Componente generado `aporte-dashboard.component.ts`
- [ ] Template HTML con filtros, KPIs, gráfico y tablas
- [ ] Environment configurado con URL base
- [ ] HttpClientModule importado
- [ ] Pipes de formato (number, currency, date) aplicados
- [ ] Manejo de loading y errores implementado
- [ ] CORS habilitado en backend (si aplica)

---

## 🚨 Notas Importantes

1. **Fechas:** El backend espera formato `yyyy-MM-dd` (sin hora)
2. **Parámetros opcionales:** Todos son opcionales, enviar solo los necesarios
3. **topN por defecto:** El backend usa 10 si no se envía
4. **CORS:** Verificar que el backend tenga configurado CORS para Angular
5. **LocalDateTime en respuestas:** Angular lo recibe como string ISO 8601

---

## 🎨 Mejoras Opcionales

- Implementar caché de datos con RxJS `shareReplay()`
- Agregar botón de "Exportar a Excel"
- Implementar paginación en las tablas
- Agregar tooltips con información adicional
- Modo oscuro/claro
- Animaciones de carga con Skeleton Loaders
- Gráficos interactivos con drill-down

---

## 📝 Comando para Copilot

**Copia y pega esto en VS Code:**

```
Implementa un dashboard de aportes en Angular siguiendo estas especificaciones:

1. Crea el archivo src/app/models/aporte-dashboard.model.ts con las interfaces:
   - AporteKpiDTO
   - AporteResumenTipoDTO
   - AporteTopEntidadDTO
   - AporteTopMovimientoDTO
   - DashboardFiltros

2. Crea el servicio src/app/services/aporte-dashboard.service.ts que consuma estos endpoints:
   - GET /aprt/kpis-globales
   - GET /aprt/resumen-por-tipo
   - GET /aprt/top-entidades
   - GET /aprt/top-movimientos
   
   Base URL: environment.apiUrl = 'http://localhost:8080/saaBE/rest'
   
   Parámetros query opcionales: fechaDesde, fechaHasta, estadoAporte, tipoAporteId, topN

3. Crea el componente src/app/components/aporte-dashboard con:
   - Formulario de filtros (fechas, estados, tipo, topN)
   - 5 cards de KPIs (movimientos, tipos, montoMas, montoMenos, saldoNeto)
   - Gráfico de dona con resumen por tipo (usa ng2-charts)
   - Tabla de top entidades
   - Tabla de top movimientos
   - Loading spinner y manejo de errores
   - Usa forkJoin para cargar los 4 endpoints en paralelo

4. Estilos con Bootstrap 5 o Angular Material

5. Pipes para formateo: number, currency:'USD', date:'short'
```

---

## 🔧 Troubleshooting

**Error de CORS:**
```typescript
// Agregar en standalone-cors.cli de WildFly:
/subsystem=undertow/server=default-server/host=default-host/filter-ref=cors-filter:add
```

**Error 404:**
- Verificar que la base URL esté correcta
- Verificar que WildFly esté corriendo
- Verificar el contexto `/saaBE/rest`

**Fechas incorrectas:**
- Usar formato `yyyy-MM-dd` exacto
- No enviar hora ni zona horaria

---

**¡Todo listo para implementar! 🚀**
