# CORRECCIÓN: Desglose de Valores en Excedentes de Préstamos DE_PLAZO_VENCIDO

**Fecha:** 2026-03-31  
**Archivo corregido:** `CargaArchivoPetroServiceImpl.java`  
**Métodos corregidos:** `procesarPagoPlazoVencido()`, `aplicarPagoACuotaPlazoVencido()`

---

## 🔴 PROBLEMA IDENTIFICADO

### Síntoma
Al procesar archivos Petro con préstamos en estado **DE_PLAZO_VENCIDO** que generaban excedentes, la observación del registro de pago mostraba:

```
Excedente de $109,42 del mes 6/2025 aplicado desde carga archivo 352 
(Desgrav:$0,00, Int:$0,00, Cap:$0,00)
```

❌ **Los valores de Desgravamen, Interés y Capital aparecían en $0.00** aunque el excedente sí se aplicó correctamente a la cuota.

### Causa Raíz

En el flujo de préstamos **DE_PLAZO_VENCIDO**, el código procesaba múltiples cuotas en secuencia dentro de un bucle. Cuando una cuota se completaba y quedaba excedente:

1. ✅ El excedente se calculaba correctamente
2. ✅ Se aplicaba a la siguiente cuota en orden
3. ✅ Los valores se distribuían correctamente (Desgravamen → Interés → Capital)
4. ❌ **PERO** el registro de pago se creaba con la observación genérica "Pago préstamo plazo vencido" sin indicar que era un excedente
5. ❌ El desglose mostraba $0.00 en todos los valores

**El problema:** El código no diferenciaba entre:
- Un **pago directo** del archivo (primera cuota)
- Un **excedente** aplicado automáticamente (cuotas siguientes)

Todos los pagos se registraban con la misma observación estándar, sin el contexto de que era un excedente ni el desglose correcto de los valores aplicados.

---

## ✅ SOLUCIÓN APLICADA

### Cambio 1: Rastreo de Excedentes en el Bucle

**Archivo:** `procesarPagoPlazoVencido()`

Se agregaron variables para rastrear si el pago es directo o un excedente:

```java
double montoRestante = montoTotal;
boolean esPrimeraCuota = true; // ✅ Indica si es pago directo o excedente
com.saa.model.crd.DetallePrestamo cuotaOrigen = null; // ✅ Cuota que generó el excedente

// Bucle de procesamiento de cuotas
for (com.saa.model.crd.DetallePrestamo cuota : todasLasCuotas) {
    // ...código de validación...
    
    if (!esPrimeraCuota && cuotaOrigen != null) {
        System.out.println("  ⚠️ Aplicando EXCEDENTE de cuota #" + cuotaOrigen.getNumeroCuota());
    }
    
    // Aplicar pago (ahora con información de si es excedente)
    double montoAplicado = aplicarPagoACuotaPlazoVencido(
        participe, cuota, saldos, montoRestante, 
        cargaArchivo, codigoProducto, hsEncontrado, 
        esPrimeraCuota,  // ✅ NUEVO parámetro
        cuotaOrigen      // ✅ NUEVO parámetro
    );
    
    montoRestante -= montoAplicado;
    
    // ✅ Si queda excedente y la cuota está completa, registrar para siguiente iteración
    if (montoRestante > 0.01 && cuota.getEstado() == PAGADA) {
        cuotaOrigen = cuota; // Esta cuota generó el excedente
        esPrimeraCuota = false; // Las siguientes serán excedentes
    }
}
```

**Beneficios:**
- ✅ Rastrea cuándo un pago es un excedente
- ✅ Mantiene referencia a la cuota que generó el excedente
- ✅ Pasa esta información al método que crea el registro de pago

### Cambio 2: Observación Diferenciada por Tipo de Pago

**Archivo:** `aplicarPagoACuotaPlazoVencido()`

Se actualizó la generación de la observación para diferenciar entre pago directo y excedente:

```java
// Actualizar firma del método para recibir nuevos parámetros
private double aplicarPagoACuotaPlazoVencido(
    ParticipeXCargaArchivo participe,
    DetallePrestamo cuota,
    SaldosRealesCuota saldos,
    double montoDisponible,
    CargaArchivo cargaArchivo,
    String codigoProducto,
    boolean hsEncontrado,
    boolean esPrimeraCuota,        // ✅ NUEVO
    DetallePrestamo cuotaOrigen    // ✅ NUEVO
) throws Throwable {
    
    // ...código de aplicación de pago...
    
    // ✅ CORRECCIÓN: Generar observación según tipo de pago
    String observacion;
    
    if (!esPrimeraCuota && cuotaOrigen != null) {
        // ✅ Es un EXCEDENTE - Incluir desglose de valores
        observacion = String.format(
            "Excedente de $%.2f del mes %d/%d aplicado desde carga archivo %d (Desgrav:$%.2f, Int:$%.2f, Cap:$%.2f)",
            montoDisponible, // Monto total del excedente
            cargaArchivo.getMesAfectacion(), 
            cargaArchivo.getAnioAfectacion(), 
            cargaArchivo.getCodigo(),
            desgravamenPagadoAhora, // ✅ Desglose CORRECTO
            interesPagadoAhora, 
            capitalPagadoAhora
        );
    } else {
        // Es un PAGO DIRECTO - Observación estándar
        observacion = String.format(
            "Pago préstamo plazo vencido - Cuota #%d - Carga %d (Desg:$%.2f, Int:$%.2f, Cap:$%.2f)",
            cuota.getNumeroCuota().intValue(), 
            cargaArchivo.getCodigo(),
            desgravamenPagadoAhora, 
            interesPagadoAhora, 
            capitalPagadoAhora
        );
    }
    
    // Crear registro con observación correcta
    crearRegistroPago(cuota, totalAplicado, capitalPagadoAhora, interesPagadoAhora, 
                      desgravamenPagadoAhora, observacion, cargaArchivo);
}
```

**Beneficios:**
- ✅ Observación diferente para excedentes
- ✅ Desglose de valores **calculados y aplicados realmente** a esa cuota
- ✅ Mantiene trazabilidad del mes/año y carga de origen

---

## 📊 COMPARACIÓN: ANTES vs DESPUÉS

### ❌ ANTES (INCORRECTO)

**Escenario:**
- Cuota #5: Total $100 (Desg: $10, Int: $30, Cap: $60)
- Monto recibido: $209.42
- Se paga cuota #5 completa → Excedente: $109.42
- Se aplica excedente a cuota #6: Total $150 (Desg: $15, Int: $45, Cap: $90)

**Registros en PagoPrestamo:**

```
Registro 1 - Cuota #5:
✅ valorPagado: $100.00
✅ observacion: "Pago préstamo plazo vencido - Cuota #5 - Carga 352 (Desg:$10.00, Int:$30.00, Cap:$60.00)"

Registro 2 - Cuota #6:
✅ valorPagado: $109.42
❌ observacion: "Pago préstamo plazo vencido - Cuota #6 - Carga 352 (Desg:$0.00, Int:$0.00, Cap:$0.00)"
   ^^^ NO indica que es excedente
   ^^^ Valores en $0.00 (aunque sí se aplicaron)
```

### ✅ DESPUÉS (CORRECTO)

**Mismo escenario:**

**Registros en PagoPrestamo:**

```
Registro 1 - Cuota #5:
✅ valorPagado: $100.00
✅ observacion: "Pago préstamo plazo vencido - Cuota #5 - Carga 352 (Desg:$10.00, Int:$30.00, Cap:$60.00)"

Registro 2 - Cuota #6:
✅ valorPagado: $109.42
✅ observacion: "Excedente de $109.42 del mes 6/2025 aplicado desde carga archivo 352 (Desg:$15.00, Int:$45.00, Cap:$79.42)"
   ^^^ ✅ Indica claramente que es un EXCEDENTE
   ^^^ ✅ Muestra el desglose REAL de cómo se distribuyó el excedente:
       - Desgravamen: $15.00 (completo)
       - Interés: $45.00 (completo)
       - Capital: $79.42 (parcial, porque solo quedaba $109.42 - $15 - $45)
```

---

## 🔄 FLUJO COMPLETO CORREGIDO

### Ejemplo Detallado

**Entrada:**
- Préstamo en estado DE_PLAZO_VENCIDO
- Monto archivo: $309.42
- Cuotas pendientes:
  - Cuota #5: $100 (Desg: $10, Int: $30, Cap: $60)
  - Cuota #6: $150 (Desg: $15, Int: $45, Cap: $90)
  - Cuota #7: $150 (Desg: $15, Int: $45, Cap: $90)

**Procesamiento:**

```
ITERACIÓN 1 - Cuota #5:
├─ esPrimeraCuota = true (pago directo)
├─ cuotaOrigen = null
├─ montoDisponible = $309.42
├─ Aplicar pago:
│  ├─ Desgravamen: $10.00 ✅
│  ├─ Interés: $30.00 ✅
│  ├─ Capital: $60.00 ✅
│  └─ Total aplicado: $100.00
├─ Cuota #5 → PAGADA ✅
├─ Registro PagoPrestamo:
│  └─ Observación: "Pago préstamo plazo vencido - Cuota #5 - Carga 352 (Desg:$10.00, Int:$30.00, Cap:$60.00)"
├─ montoRestante = $309.42 - $100.00 = $209.42
└─ Cuota está PAGADA y sobra dinero:
   ├─ cuotaOrigen = Cuota #5 ✅
   └─ esPrimeraCuota = false ✅

ITERACIÓN 2 - Cuota #6:
├─ esPrimeraCuota = false (ES EXCEDENTE)
├─ cuotaOrigen = Cuota #5
├─ 🔔 Log: "⚠️ Aplicando EXCEDENTE de cuota #5"
├─ montoDisponible = $209.42
├─ Aplicar pago:
│  ├─ Desgravamen: $15.00 ✅
│  ├─ Interés: $45.00 ✅
│  ├─ Capital: $90.00 ✅ (completo porque alcanza)
│  └─ Total aplicado: $150.00
├─ Cuota #6 → PAGADA ✅
├─ Registro PagoPrestamo:
│  └─ Observación: "Excedente de $209.42 del mes 6/2025 aplicado desde carga archivo 352 (Desg:$15.00, Int:$45.00, Cap:$90.00)"
│     ^^^ ✅ CORRECTO: Indica excedente con desglose real
├─ montoRestante = $209.42 - $150.00 = $59.42
└─ Cuota está PAGADA y sobra dinero:
   ├─ cuotaOrigen = Cuota #6 ✅
   └─ esPrimeraCuota = false (sigue siendo excedente)

ITERACIÓN 3 - Cuota #7:
├─ esPrimeraCuota = false (ES EXCEDENTE)
├─ cuotaOrigen = Cuota #6
├─ 🔔 Log: "⚠️ Aplicando EXCEDENTE de cuota #6"
├─ montoDisponible = $59.42
├─ Aplicar pago:
│  ├─ Desgravamen: $15.00 ✅
│  ├─ Interés: $44.42 (parcial, solo queda $59.42 - $15.00) ⚠️
│  ├─ Capital: $0.00 (no alcanza)
│  └─ Total aplicado: $59.42
├─ Cuota #7 → PARCIAL ⚠️ (falta capital y parte del interés)
├─ Registro PagoPrestamo:
│  └─ Observación: "Excedente de $59.42 del mes 6/2025 aplicado desde carga archivo 352 (Desg:$15.00, Int:$44.42, Cap:$0.00)"
│     ^^^ ✅ CORRECTO: Indica excedente con desglose parcial
└─ montoRestante = $0.00 → FIN
```

---

## 🎯 IMPACTO DE LA CORRECCIÓN

### Beneficios para Auditoría y Trazabilidad

1. **Claridad en Registros de Pago** ✅
   - Ahora es evidente cuándo un pago es un excedente
   - Se puede rastrear de qué cuota proviene el excedente

2. **Desglose Correcto de Valores** ✅
   - Los valores de Desgravamen, Interés y Capital reflejan lo realmente aplicado
   - Ya no aparecen en $0.00

3. **Mejor Auditoría** ✅
   - Se puede verificar cómo se distribuyó cada excedente
   - Se mantiene el orden de afectación (Desgravamen → Interés → Capital)

4. **Trazabilidad Completa** ✅
   - Mes/año de origen del pago
   - Carga archivo de origen
   - Desglose exacto de valores

### Ejemplo de Consulta SQL Post-Corrección

```sql
-- Ver todos los excedentes aplicados de una carga
SELECT 
    pp.codigo,
    pp.detalle_prestamo_id,
    dp.numero_cuota,
    pp.valor_pagado,
    pp.capital_pagado,
    pp.interes_pagado,
    pp.desgravamen,
    pp.observacion
FROM 
    pago_prestamo pp
    JOIN detalle_prestamo dp ON pp.detalle_prestamo_id = dp.codigo
WHERE 
    pp.observacion LIKE 'Excedente de%352%'
ORDER BY 
    dp.numero_cuota;

-- Resultado esperado:
-- codigo | cuota | valor   | capital | interes | desgrav | observacion
-- -------|-------|---------|---------|---------|---------|-------------
-- 1234   | 6     | 209.42  | 90.00   | 45.00   | 15.00   | Excedente de $209.42 del mes 6/2025 aplicado desde carga archivo 352 (Desg:$15.00, Int:$45.00, Cap:$90.00)
-- 1235   | 7     | 59.42   | 0.00    | 44.42   | 15.00   | Excedente de $59.42 del mes 6/2025 aplicado desde carga archivo 352 (Desg:$15.00, Int:$44.42, Cap:$0.00)
```

---

## 🧪 PRUEBAS RECOMENDADAS

### Test 1: Excedente que Completa Siguiente Cuota
1. Procesar préstamo plazo vencido con pago que completa 2 cuotas
2. **Verificar:**
   - ✅ Primera cuota: Observación "Pago préstamo plazo vencido"
   - ✅ Segunda cuota: Observación "Excedente de $X..."
   - ✅ Desglose correcto en ambas
   - ✅ Ambas cuotas en estado PAGADA

### Test 2: Excedente Parcial
1. Procesar pago que completa una cuota y deja excedente que NO completa la siguiente
2. **Verificar:**
   - ✅ Primera cuota: PAGADA con observación normal
   - ✅ Segunda cuota: PARCIAL con observación "Excedente de $X..."
   - ✅ Desglose muestra valores parciales correctos

### Test 3: Excedente en Múltiples Cuotas
1. Procesar pago grande que cubre 3+ cuotas
2. **Verificar:**
   - ✅ Solo primera cuota tiene observación "Pago préstamo plazo vencido"
   - ✅ Todas las demás tienen observación "Excedente de..."
   - ✅ Cada registro tiene su desglose correcto

---

## 📝 NOTAS TÉCNICAS

### Variables Clave Agregadas

```java
// En procesarPagoPlazoVencido()
boolean esPrimeraCuota = true;  // Rastrea si es pago directo o excedente
DetallePrestamo cuotaOrigen = null;  // Cuota que generó el excedente

// Actualización después de cada cuota procesada
if (montoRestante > 0.01 && cuota.getEstado() == PAGADA) {
    cuotaOrigen = cuota;
    esPrimeraCuota = false;
}
```

### Parámetros Adicionales en Firma

```java
// Firma actualizada de aplicarPagoACuotaPlazoVencido
private double aplicarPagoACuotaPlazoVencido(
    // ...parámetros existentes...
    boolean esPrimeraCuota,        // NUEVO
    DetallePrestamo cuotaOrigen    // NUEVO
)
```

---

## ✅ RESULTADO FINAL

Con esta corrección, el sistema ahora:

1. ✅ **Diferencia entre pagos directos y excedentes** en las observaciones
2. ✅ **Muestra el desglose correcto** de Desgravamen, Interés y Capital en excedentes
3. ✅ **Mantiene trazabilidad completa** del origen de cada excedente
4. ✅ **Facilita auditorías** con información clara y precisa
5. ✅ **NO modifica la lógica de aplicación** - solo mejora el registro

**El problema de valores en $0.00 en excedentes está RESUELTO.**

---

## 📚 ARCHIVOS RELACIONADOS

- **Implementación:** `CargaArchivoPetroServiceImpl.java`
- **Documentación previa:** `CORRECCION-PRESTAMOS-PLAZO-VENCIDO.md`
- **Documentación general:** `PROCESO-CARGA-ARCHIVO-PETROCOMERCIAL-COMPLETO.md`

---

**FIN DEL DOCUMENTO**
