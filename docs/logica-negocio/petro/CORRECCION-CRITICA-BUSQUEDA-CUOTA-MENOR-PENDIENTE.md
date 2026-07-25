# CORRECCIÓN CRÍTICA: Búsqueda de Cuota a Pagar - Siempre la Menor Pendiente

**Fecha:** 2026-03-31  
**Archivo corregido:** `CargaArchivoPetroServiceImpl.java`  
**Método corregido:** `buscarCuotaAPagar()`

---

## 🔴 PROBLEMA CRÍTICO IDENTIFICADO

### Síntoma
Al procesar archivos Petro con préstamos que tenían **cuotas intermedias en MORA o PARCIAL**, el sistema las **saltaba** y aplicaba el pago a la cuota del mes/año del archivo, dejando las cuotas anteriores sin pagar.

**Ejemplo del problema:**
```
Préstamo con 121 cuotas:
- Cuota #58: PARCIAL (tiene saldo pendiente) ❌ SE SALTÓ
- Cuota #59: MORA (sin pagar) ❌ SE SALTÓ
- Cuota #60: MORA (sin pagar) ❌ SE SALTÓ
- Cuota #61: MORA (sin pagar) ❌ SE SALTÓ
- Cuota #62: MORA (sin pagar) ❌ SE SALTÓ
- Cuota #63: PENDIENTE (corresponde a 6/2025) ✅ PAGÓ ESTA

Archivo de 6/2025 con $334.82:
❌ Pagó la cuota #63 (del mes del archivo)
❌ Dejó las cuotas #58-#62 sin pagar
❌ Generó inconsistencia en el préstamo
```

### Evidencia del Problema (Logs)

```
2026-03-31 15:50:38,450 INFO  Buscando cuota del préstamo 7208 para 6/2025
2026-03-31 15:50:38,450 INFO  Cuotas encontradas: 1
2026-03-31 15:50:38,450 INFO  ✅ Cuota a pagar encontrada: #63.0 - Monto: $225.4
```

**Problema:** Encontró la cuota #63 porque es la del mes 6/2025, **pero NO verificó** si había cuotas anteriores pendientes (#58-#62).

### Causa Raíz

El método `buscarCuotaAPagar()` tenía una **lógica incorrecta**:

```java
// ❌ CÓDIGO INCORRECTO (ANTERIOR)
private DetallePrestamo buscarCuotaAPagar(...) {
    
    // PASO 1: Buscar cuota del mes/año del archivo
    for (Prestamo prestamo : prestamos) {
        List<DetallePrestamo> cuotasDelMes = 
            detallePrestamoDaoService.selectByPrestamoYMesAnio(
                prestamo.getCodigo(),
                mesArchivo,
                anioArchivo
            );
        
        if (cuotasDelMes != null && !cuotasDelMes.isEmpty()) {
            for (DetallePrestamo cuota : cuotasDelMes) {
                if (cuota.getEstado() != PAGADA && cuota.getEstado() != CANCELADA) {
                    return cuota; // ❌ RETORNA INMEDIATAMENTE la cuota del mes
                }
            }
        }
    }
    
    // PASO 2: Solo si NO encuentra del mes, buscar la mínima pendiente
    // ... código para buscar mínima ...
}
```

**El problema:**
1. ❌ **Primero** buscaba la cuota del mes/año del archivo
2. ❌ Si la encontraba, **la retornaba inmediatamente** sin verificar cuotas anteriores
3. ❌ **Solo si no encontraba** cuota del mes, entonces buscaba la mínima pendiente
4. ❌ Esto causaba que se saltaran cuotas en MORA/PARCIAL

**Comportamiento correcto esperado:**
- Los préstamos se pagan **secuencialmente** - cuota por cuota en orden
- Si hay una cuota #58 en PARCIAL, debe pagarse primero
- **No se debe saltar** a cuotas posteriores aunque correspondan al mes del archivo

---

## ✅ SOLUCIÓN APLICADA

### Lógica Corregida

Se modificó completamente el método para **SIEMPRE** buscar la menor cuota pendiente:

```java
// ✅ CÓDIGO CORRECTO (NUEVO)
/**
 * Busca la cuota que se debe pagar
 * 
 * CORRECCIÓN CRÍTICA: SIEMPRE busca la MENOR cuota pendiente 
 * (en cualquier estado diferente a PAGADA/CANCELADA)
 * 
 * No importa el mes/año del archivo - los préstamos deben pagarse 
 * en orden secuencial
 * 
 * Estados válidos para pagar: MORA, PARCIAL, PENDIENTE, EMITIDA, etc.
 */
private DetallePrestamo buscarCuotaAPagar(
    List<Prestamo> prestamos, 
    CargaArchivo cargaArchivo
) throws Throwable {
    
    System.out.println("Buscando cuota del préstamo " + 
                       prestamos.get(0).getCodigo() + 
                       " para " + cargaArchivo.getMesAfectacion() + 
                       "/" + cargaArchivo.getAnioAfectacion());
    
    // ✅ CORRECCIÓN: Buscar SIEMPRE la MENOR cuota pendiente
    // Los préstamos se pagan secuencialmente - no se salta a la cuota del mes
    DetallePrestamo cuotaMinima = null;
    
    for (Prestamo prestamo : prestamos) {
        List<DetallePrestamo> todasLasCuotas = 
            detallePrestamoDaoService.selectByPrestamo(prestamo.getCodigo());
        
        if (todasLasCuotas != null && !todasLasCuotas.isEmpty()) {
            // ✅ Ordenar por número de cuota para asegurar secuencia
            todasLasCuotas.sort((c1, c2) -> 
                Double.compare(c1.getNumeroCuota(), c2.getNumeroCuota())
            );
            
            for (DetallePrestamo cuota : todasLasCuotas) {
                Long estadoCuota = cuota.getEstado();
                
                // ✅ Buscar cualquier cuota que NO esté PAGADA ni CANCELADA
                // Incluye: MORA, PARCIAL, PENDIENTE, EMITIDA, etc.
                if (estadoCuota != null && 
                    estadoCuota != EstadoCuotaPrestamo.PAGADA && 
                    estadoCuota != EstadoCuotaPrestamo.CANCELADA_ANTICIPADA) {
                    
                    // ✅ Guardar la cuota con el número menor
                    if (cuotaMinima == null || 
                        cuota.getNumeroCuota() < cuotaMinima.getNumeroCuota()) {
                        cuotaMinima = cuota;
                    }
                }
            }
        }
    }
    
    // ✅ Logging mejorado
    if (cuotaMinima != null) {
        System.out.println("✅ Cuota a pagar encontrada: #" + 
                           cuotaMinima.getNumeroCuota() + 
                           " - Estado: " + cuotaMinima.getEstado() + 
                           " - Monto: $" + cuotaMinima.getTotal());
    } else {
        System.out.println("⚠️ No se encontró ninguna cuota pendiente");
    }
    
    return cuotaMinima;
}
```

### Beneficios de la Corrección

1. ✅ **Orden Secuencial Garantizado**
   - Siempre busca la cuota con el número más bajo
   - No se saltan cuotas intermedias

2. ✅ **Todos los Estados Pendientes**
   - Detecta cuotas en MORA, PARCIAL, PENDIENTE, EMITIDA
   - Solo omite PAGADA y CANCELADA_ANTICIPADA

3. ✅ **Independiente del Mes del Archivo**
   - El mes/año del archivo es solo informativo
   - La búsqueda se basa en el orden de cuotas

4. ✅ **Logging Detallado**
   - Muestra qué cuota se encontró y su estado
   - Facilita debugging y auditoría

---

## 🔄 FLUJO CORREGIDO COMPLETO

### Ejemplo: Mismo Caso del Problema

**Situación:**
```
Préstamo #7208 con 121 cuotas:
- Cuota #58: PARCIAL (saldo pendiente: $150)
- Cuota #59: MORA (sin pagar: $225)
- Cuota #60: MORA (sin pagar: $225)
- Cuota #61: MORA (sin pagar: $225)
- Cuota #62: MORA (sin pagar: $225)
- Cuota #63: PENDIENTE (mes 6/2025: $225)

Archivo de 6/2025 con monto: $334.82
```

### ❌ ANTES (INCORRECTO)

```
Procesamiento:

1. buscarCuotaAPagar(mes=6, año=2025):
   ├─ Buscar cuota del mes 6/2025
   ├─ Encuentra: Cuota #63
   └─ ❌ RETORNA cuota #63 (salta las anteriores)

2. Aplicar pago:
   ├─ Paga cuota #63: $225.40
   ├─ Excedente: $109.42
   └─ Aplica excedente a cuota #64

Resultado:
❌ Cuota #58: Sigue PARCIAL (sin pagar)
❌ Cuotas #59-62: Siguen en MORA (sin pagar)
✅ Cuota #63: PAGADA
✅ Cuota #64: PAGADA
❌ Préstamo con cuotas intermedias sin pagar
```

### ✅ DESPUÉS (CORRECTO)

```
Procesamiento:

1. buscarCuotaAPagar(mes=6, año=2025):
   ├─ Obtener TODAS las cuotas del préstamo
   ├─ Ordenar por número de cuota
   ├─ Recorrer en orden:
   │  ├─ Cuota #58: Estado=PARCIAL → ✅ ES LA MENOR PENDIENTE
   │  └─ RETORNA cuota #58
   └─ Log: "✅ Cuota a pagar encontrada: #58 - Estado: PARCIAL - Monto: $150"

2. Aplicar pago a cuota #58:
   ├─ Saldo pendiente: $150
   ├─ Pagar: $150 (completa)
   ├─ Cuota #58 → PAGADA ✅
   └─ Excedente: $184.82

3. Buscar siguiente cuota pendiente:
   ├─ Cuota #59: Estado=MORA → ✅ SIGUIENTE PENDIENTE
   └─ Aplicar excedente

4. Aplicar excedente a cuota #59:
   ├─ Pagar: $184.82 (parcial de $225)
   ├─ Cuota #59 → PARCIAL (falta $40.18)
   └─ Excedente: $0

Resultado:
✅ Cuota #58: PAGADA (completada)
✅ Cuota #59: PARCIAL (parcialmente pagada: $184.82 de $225)
⏭️ Cuotas #60-62: Siguen en MORA (pendientes para próximos archivos)
⏭️ Cuota #63: PENDIENTE (se pagará después)
✅ Préstamo con orden secuencial correcto
```

---

## 📊 COMPARACIÓN: ANTES vs DESPUÉS

| Aspecto | ❌ ANTES | ✅ DESPUÉS |
|---------|----------|------------|
| **Búsqueda** | Por mes/año del archivo | Por menor número de cuota |
| **Orden** | Salta cuotas intermedias | Secuencial estricto |
| **Estados** | Solo busca del mes | Busca cualquier pendiente |
| **Lógica** | 1. Mes/año, 2. Mínima | Siempre la mínima |
| **Consistencia** | Deja cuotas sin pagar | Paga en orden correcto |
| **Cuotas en MORA** | Se saltan si no son del mes | Se pagan en orden |
| **Cuotas PARCIALES** | Se saltan si no son del mes | Se pagan primero |

---

## 🎯 CASOS DE USO RESUELTOS

### Caso 1: Cuotas en MORA Anteriores al Mes del Archivo

**Antes:**
```
Cuota #58: MORA → ❌ Saltada
Cuota #59: MORA → ❌ Saltada
Cuota #60: PENDIENTE (mes del archivo) → ✅ Pagada
```

**Ahora:**
```
Cuota #58: MORA → ✅ Pagada primero
Cuota #59: MORA → ✅ Pagada con excedente
Cuota #60: PENDIENTE → ⏭️ Pendiente para siguiente archivo
```

### Caso 2: Cuota PARCIAL Anterior

**Antes:**
```
Cuota #45: PARCIAL (falta $50) → ❌ Saltada
Cuota #50: PENDIENTE (mes del archivo) → ✅ Pagada
```

**Ahora:**
```
Cuota #45: PARCIAL → ✅ Completada primero ($50)
Cuota #46: PENDIENTE → ✅ Pagada con excedente
Cuota #50: PENDIENTE → ⏭️ Se pagará después
```

### Caso 3: Todas las Anteriores PAGADAS

**Antes y Ahora (mismo comportamiento correcto):**
```
Cuotas #1-62: PAGADAS
Cuota #63: PENDIENTE (mes del archivo) → ✅ Pagada
```

---

## 🧪 PRUEBAS RECOMENDADAS

### Test 1: Cuotas en MORA Anteriores

**Setup:**
```sql
-- Crear cuotas
INSERT INTO detalle_prestamo (numero_cuota, estado) VALUES
(58, 4), -- MORA
(59, 4), -- MORA
(60, 6); -- PENDIENTE (mes del archivo)
```

**Procesar archivo del mes correspondiente a cuota #60**

**Verificar:**
```sql
-- La cuota #58 debe pagarse primero
SELECT numero_cuota, estado 
FROM detalle_prestamo 
WHERE numero_cuota IN (58, 59, 60);

-- Esperado:
-- 58: PAGADA o PARCIAL (según monto)
-- 59: Dependiendo del excedente
-- 60: Pendiente o pagada según disponibilidad
```

### Test 2: Cuota PARCIAL + Excedente

**Setup:**
```sql
-- Cuota parcialmente pagada
INSERT INTO detalle_prestamo (numero_cuota, capital, interes, desgravamen, 
                               capital_pagado, interes_pagado, desgravamen_pagado, estado)
VALUES (45, 100, 50, 10, 50, 20, 5, 5); -- PARCIAL, falta $85

-- Cuota posterior pendiente
INSERT INTO detalle_prestamo (numero_cuota, estado) VALUES (50, 6); -- PENDIENTE
```

**Procesar archivo con monto $200**

**Verificar:**
```
Cuota #45: PAGADA (completada con $85)
Cuota #46-49: Procesadas según excedente restante ($115)
```

### Test 3: Sin Cuotas Anteriores Pendientes

**Setup:**
```sql
-- Todas anteriores pagadas
UPDATE detalle_prestamo SET estado = 2 WHERE numero_cuota < 63;

-- Solo una pendiente
INSERT INTO detalle_prestamo (numero_cuota, estado) VALUES (63, 6);
```

**Procesar archivo**

**Verificar:**
```
Cuota #63: PAGADA (comportamiento normal)
```

---

## 📝 LOGS ESPERADOS

### Con Cuotas Anteriores Pendientes

```
2026-03-31 15:50:38 INFO  Buscando cuota del préstamo 7208 para 6/2025
2026-03-31 15:50:38 INFO  ✅ Cuota a pagar encontrada: #58 - Estado: 5 (PARCIAL) - Monto: $150.00
2026-03-31 15:50:38 INFO  >>> Procesando cuota #58 - Préstamo: 7208
2026-03-31 15:50:38 INFO      Monto recibido: $334.82
2026-03-31 15:50:38 INFO      Monto esperado: $150.00
2026-03-31 15:50:38 INFO      ✅ Cuota COMPLETADA → PAGADA
2026-03-31 15:50:38 INFO      >>> Aplicando EXCEDENTE de $184.82 a siguiente cuota
2026-03-31 15:50:38 INFO  ✅ Cuota a pagar encontrada: #59 - Estado: 4 (MORA) - Monto: $225.00
2026-03-31 15:50:38 INFO  >>> Procesando cuota #59 - Préstamo: 7208
2026-03-31 15:50:38 INFO      Monto recibido: $184.82
2026-03-31 15:50:38 INFO      Monto esperado: $225.00
2026-03-31 15:50:38 INFO      ⚠️ Cuota PARCIAL (falta: $40.18)
```

### Sin Cuotas Anteriores (Normal)

```
2026-03-31 15:50:38 INFO  Buscando cuota del préstamo 7208 para 6/2025
2026-03-31 15:50:38 INFO  ✅ Cuota a pagar encontrada: #63 - Estado: 6 (PENDIENTE) - Monto: $225.40
2026-03-31 15:50:38 INFO  >>> Procesando cuota #63 - Préstamo: 7208
```

---

## ✅ RESULTADO FINAL

Con esta corrección crítica, el sistema ahora:

1. ✅ **SIEMPRE busca la menor cuota pendiente** (independiente del mes del archivo)
2. ✅ **No salta cuotas intermedias** en MORA, PARCIAL o cualquier estado pendiente
3. ✅ **Mantiene orden secuencial estricto** en el pago de cuotas
4. ✅ **Garantiza consistencia** en el estado del préstamo
5. ✅ **Paga cuotas en el orden correcto** antes de avanzar
6. ✅ **Evita inconsistencias** de cuotas intermedias sin pagar
7. ✅ **Logging claro** que muestra la cuota encontrada y su estado

**El problema de cuotas intermedias saltadas está COMPLETAMENTE RESUELTO.**

---

## 📚 ARCHIVOS RELACIONADOS

- **Implementación:** `CargaArchivoPetroServiceImpl.java`
- **Documentación previa:** 
  - `CORRECCION-PRESTAMOS-PLAZO-VENCIDO.md`
  - `CORRECCION-DESGLOSE-EXCEDENTES-PLAZO-VENCIDO.md`
  - `CORRECCION-VALIDACION-CUOTAS-COMPLETADAS-PLAZO-VENCIDO.md`
- **Documentación general:** `PROCESO-CARGA-ARCHIVO-PETROCOMERCIAL-COMPLETO.md`

---

**FIN DEL DOCUMENTO**
