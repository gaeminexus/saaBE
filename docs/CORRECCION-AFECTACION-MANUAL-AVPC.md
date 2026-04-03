# CORRECCIÓN: Afectación Manual de Pagos (Tabla AVPC)

**Fecha:** 2026-03-31  
**Archivo corregido:** `CargaArchivoPetroServiceImpl.java`  
**Método corregido:** `verificarAfectacionManual()`

---

## 🔍 PROBLEMA IDENTIFICADO

### Síntoma
Al procesar archivos Petro, el sistema **NO estaba aplicando** las afectaciones manuales definidas en la tabla `AfectacionValoresParticipeCarga` (AVPC), a pesar de que la documentación indica claramente que esta tabla tiene **PRIORIDAD** sobre las reglas automáticas.

### Regla Documentada (No se estaba cumpliendo)
Según `PROCESO-CARGA-ARCHIVO-PETROCOMERCIAL-COMPLETO.md`:

**Regla 1: Verificar Afectación Manual (Tabla AVPC)**
> Si existe un registro en `AfectacionValoresParticipeCarga` para el partícipe/cuota:
> - Se aplican los valores exactos indicados en la tabla AVPC
> - Se ignoran las reglas automáticas
> - Se actualiza la cuota según los valores de afectación manual

### Causa Raíz

El método `verificarAfectacionManual()` tenía **dos problemas críticos**:

#### 1. **Búsqueda Ineficiente** ❌
```java
// CÓDIGO ANTERIOR (PROBLEMÁTICO)
List<AfectacionValoresParticipeCarga> afectaciones = 
    afectacionValoresParticipeCargaService.selectAll(); // ⚠️ Trae TODAS las afectaciones

if (afectaciones != null) {
    for (AfectacionValoresParticipeCarga afectacion : afectaciones) {
        // Filtrar en memoria...
        if (afectacion.getNovedadParticipeCarga() != null &&
            afectacion.getNovedadParticipeCarga().getParticipeXCargaArchivo() != null &&
            // ...más validaciones
        ) {
            return afectacion;
        }
    }
}
```

**Problemas:**
- Trae TODAS las afectaciones de la BD en memoria
- Las relaciones JPA pueden no estar cargadas (lazy loading)
- Si `getParticipeXCargaArchivo()` retorna null por lazy loading, nunca encuentra la afectación
- Ineficiente: O(n) con n = total de afectaciones en toda la BD

#### 2. **Sin Logging para Debugging** ❌
No había forma de saber si:
- Se estaba buscando la afectación
- Se encontró alguna afectación
- Por qué no se aplicó si existía

---

## ✅ SOLUCIÓN APLICADA

### Cambios Realizados

#### 1. **Búsqueda Directa por Cuota** 🎯
```java
// CÓDIGO NUEVO (CORRECTO)
com.saa.ejb.crd.serviceImpl.AfectacionValoresParticipeCargaServiceImpl serviceImpl = 
    (com.saa.ejb.crd.serviceImpl.AfectacionValoresParticipeCargaServiceImpl) afectacionValoresParticipeCargaService;

// ✅ Busca SOLO las afectaciones de esta cuota específica
List<com.saa.model.crd.AfectacionValoresParticipeCarga> afectacionesCuota = 
    serviceImpl.selectByCuota(cuota.getCodigo());
```

**Ventajas:**
- Búsqueda optimizada: Solo trae las afectaciones de esa cuota
- Usa método específico del DAO que hace JOIN correcto
- Las relaciones están correctamente cargadas
- Más eficiente: O(k) con k = afectaciones de esta cuota (típicamente 0 o 1)

#### 2. **Logging Detallado para Debugging** 📋
```java
System.out.println("    🔍 Verificando afectación manual (tabla AVPC)...");
System.out.println("       Partícipe ID: " + participe.getCodigo());
System.out.println("       Cuota ID: " + cuota.getCodigo());
System.out.println("       Afectaciones encontradas: " + afectacionesCuota.size());

if (codigoParticipeNovedad.equals(participe.getCodigo())) {
    System.out.println("       ✅ AFECTACIÓN MANUAL ENCONTRADA!");
    System.out.println("         - Capital a afectar: $" + afectacion.getCapitalAfectar());
    System.out.println("         - Interés a afectar: $" + afectacion.getInteresAfectar());
    System.out.println("         - Desgravamen a afectar: $" + afectacion.getDesgravamenAfectar());
    return afectacion;
}
```

**Ventajas:**
- Permite ver en logs si se buscó la afectación
- Muestra cuántas afectaciones se encontraron
- Indica claramente cuando se aplica una afectación manual
- Ayuda a detectar problemas de configuración

#### 3. **Validación Robusta de Relaciones** 🛡️
```java
for (com.saa.model.crd.AfectacionValoresParticipeCarga afectacion : afectacionesCuota) {
    if (afectacion.getNovedadParticipeCarga() != null) {
        com.saa.model.crd.NovedadParticipeCarga novedad = afectacion.getNovedadParticipeCarga();
        
        if (novedad.getParticipeXCargaArchivo() != null) {
            Long codigoParticipeNovedad = novedad.getParticipeXCargaArchivo().getCodigo();
            
            if (codigoParticipeNovedad.equals(participe.getCodigo())) {
                return afectacion; // ✅ Encontrada y validada
            }
        }
    }
}
```

**Ventajas:**
- Valida cada nivel de la relación (afectacion → novedad → participe)
- No falla si alguna relación es null
- Compara por IDs (más robusto que comparar objetos completos)

---

## 🔄 FLUJO CORRECTO DE APLICACIÓN DE PAGOS

### Proceso Actualizado:

```
1. Usuario procesa archivo Petro
   └─> POST /api/asgn/aplicarPagosArchivoPetro/{id}

2. Para cada partícipe con préstamo:
   ├─> Buscar préstamo activo
   ├─> Identificar cuota a pagar
   │
   ├─> 🔍 PASO CRÍTICO: Verificar afectación manual
   │   ├─> Buscar en tabla AVPC por ID de cuota
   │   ├─> Filtrar por ID de partícipe
   │   └─> Si existe:
   │       ├─> ✅ Aplicar valores de AVPC (prioridad)
   │       ├─> Actualizar cuota con valores manuales
   │       ├─> Crear registro en PagoPrestamo
   │       └─> ✅ FIN (omitir reglas automáticas)
   │
   └─> Si NO existe afectación manual:
       ├─> ⚙️ Aplicar reglas automáticas
       ├─> Orden: Desgravamen → Interés → Capital
       └─> Estado según monto: PAGADA / PARCIAL / EXCEDENTE
```

---

## 📊 EJEMPLO DE USO

### Escenario: Pago con Afectación Manual

**Situación:**
- Partícipe: 12345 - Juan Pérez
- Préstamo: PH (Hipotecario)
- Cuota #5: Total esperado = $450
  - Desgravamen: $50
  - Interés: $100
  - Capital: $300
- Monto archivo: $200 (insuficiente)

**SIN afectación manual:**
- Aplicaría reglas automáticas:
  - Desgravamen: $50
  - Interés: $100
  - Capital: $50
  - Estado: PARCIAL

**CON afectación manual en AVPC:**
```sql
INSERT INTO AFECTACION_VALORES_PARTICIPE_CARGA 
(CODIGO, NOVEDAD_PARTICIPE_CARGA, DETALLE_PRESTAMO, VALOR_AFECTAR, 
 CAPITAL_AFECTAR, INTERES_AFECTAR, DESGRAVAMEN_AFECTAR)
VALUES 
(1, 100, 500, 200, 150, 50, 0);
```

**Resultado con corrección aplicada:**
- ✅ Sistema detecta afectación manual
- ✅ Aplica valores de AVPC:
  - Capital: $150 (en lugar de $50)
  - Interés: $50 (en lugar de $100)
  - Desgravamen: $0 (en lugar de $50)
- ✅ Estado: PARCIAL
- ✅ Observación en PagoPrestamo: "Pago con afectación manual (AVPC ID: 1)"

---

## 🧪 PRUEBAS RECOMENDADAS

### Test 1: Afectación Manual Existe
1. Crear registro en NVPC (NovedadParticipeCarga) para un partícipe
2. Crear registro en AVPC con valores específicos
3. Procesar archivo con ese partícipe
4. **Verificar:** Logs muestran "✅ AFECTACIÓN MANUAL ENCONTRADA!"
5. **Verificar:** Cuota tiene los valores exactos de AVPC

### Test 2: Sin Afectación Manual
1. Procesar partícipe sin registro en AVPC
2. **Verificar:** Logs muestran "❌ No se encontró afectación manual"
3. **Verificar:** Se aplicaron reglas automáticas
4. **Verificar:** Orden correcto: Desgravamen → Interés → Capital

### Test 3: Afectación de Cuota Incorrecta
1. Crear AVPC para cuota #5
2. Procesar pago que corresponde a cuota #3
3. **Verificar:** No se aplica la afectación (cuotas diferentes)
4. **Verificar:** Se usan reglas automáticas

---

## 📝 NOTAS IMPORTANTES

### Relaciones de la Tabla AVPC

```
AfectacionValoresParticipeCarga (AVPC)
├─> novedadParticipeCarga (FK)
│   └─> NovedadParticipeCarga (NVPC)
│       └─> participeXCargaArchivo (FK)
│           └─> ParticipeXCargaArchivo (PXCA)
│
├─> detallePrestamo (FK)
│   └─> DetallePrestamo (DTPR)
│
└─> prestamo (FK)
    └─> Prestamo (PRST)
```

### Campos de Afectación

- `valorAfectar`: Valor total a aplicar
- `capitalAfectar`: Monto específico para capital
- `interesAfectar`: Monto específico para interés
- `desgravamenAfectar`: Monto específico para desgravamen

**IMPORTANTE:** Los valores deben sumar correctamente según la lógica de negocio.

---

## ✅ RESULTADO

Con esta corrección, el sistema ahora:
1. ✅ **Busca eficientemente** afectaciones manuales por cuota
2. ✅ **Prioriza** la tabla AVPC sobre reglas automáticas
3. ✅ **Registra en logs** todo el proceso de verificación
4. ✅ **Aplica valores exactos** indicados en AVPC
5. ✅ **Cumple la Regla 1** de la documentación

---

## 📚 ARCHIVOS RELACIONADOS

- **Implementación:** `CargaArchivoPetroServiceImpl.java`
- **Documentación:** `PROCESO-CARGA-ARCHIVO-PETROCOMERCIAL-COMPLETO.md`
- **SQL Tabla:** `CREATE_TABLE_AVPC_AfectacionValoresParticipeCarga.sql`
- **REST API:** `AfectacionValoresParticipeCargaRest.java` (endpoint: `/api/avpc`)
- **Servicio:** `AfectacionValoresParticipeCargaServiceImpl.java`

---

**FIN DEL DOCUMENTO**
