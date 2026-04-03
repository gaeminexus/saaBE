# CORRECCIÓN: Validación Automática de Cuotas Completadas en Préstamos DE_PLAZO_VENCIDO

**Fecha:** 2026-03-31  
**Archivo corregido:** `CargaArchivoPetroServiceImpl.java`  
**Método corregido:** `calcularSaldosRealesCuota()`, `procesarPagoPlazoVencido()`

---

## 🔴 PROBLEMA IDENTIFICADO

### Síntoma
Al procesar archivos Petro con préstamos en estado **DE_PLAZO_VENCIDO**, las cuotas que ya habían sido completadas según la tabla `PagoPrestamo` **NO pasaban automáticamente a estado PAGADA**.

**Consecuencia:** El sistema intentaba aplicar pagos a cuotas que ya estaban completas, dejándolas en estado PARCIAL o sin actualizar su estado correctamente.

### Ejemplo del Problema

**Escenario:**
```
Cuota #5 del préstamo:
- Total esperado: $100 (Desg: $10, Int: $30, Cap: $60)
- Estado en DetallePrestamo: PARCIAL o MORA

Registros en PagoPrestamo:
- Pago 1: $50 (hace 2 meses)
- Pago 2: $50 (hace 1 mes)
- TOTAL PAGADO: $100 ✅ (CUOTA COMPLETA)

Problema:
❌ La cuota seguía en estado PARCIAL/MORA
❌ El sistema intentaba aplicar un nuevo pago
❌ La cuota no avanzaba a PAGADA automáticamente
❌ Se bloqueaba el procesamiento de siguientes cuotas
```

### Causa Raíz

El método `calcularSaldosRealesCuota()`:
1. ✅ Consultaba correctamente la tabla `PagoPrestamo`
2. ✅ Sumaba todos los pagos de la cuota
3. ✅ Calculaba los saldos pendientes
4. ❌ **PERO NO validaba** si el total pagado = total esperado
5. ❌ **PERO NO actualizaba** el estado de la cuota a PAGADA

Esto causaba que cuotas completamente pagadas permanecieran en estados incorrectos (PARCIAL, MORA, PENDIENTE) y bloquearan el procesamiento.

---

## ✅ SOLUCIÓN APLICADA

### Cambio 1: Validación Automática en `calcularSaldosRealesCuota()`

Se agregó lógica para detectar cuotas completadas y actualizarlas automáticamente:

```java
/**
 * Calcula los saldos reales de una cuota consultando la tabla PagoPrestamo
 * 
 * CORRECCIÓN: Ahora valida si la cuota está completamente pagada según
 * PagoPrestamo y la actualiza automáticamente a PAGADA
 */
private SaldosRealesCuota calcularSaldosRealesCuota(DetallePrestamo cuota) throws Throwable {
    SaldosRealesCuota saldos = new SaldosRealesCuota();
    
    // 1. Obtener valores esperados
    double desgravamenEsperado = nullSafe(cuota.getDesgravamen());
    double interesEsperado = nullSafe(cuota.getInteres());
    double capitalEsperado = nullSafe(cuota.getCapital());
    
    // 2. Consultar tabla PagoPrestamo
    List<PagoPrestamo> pagos = pagoPrestamoService.selectAll();
    
    double desgravamenPagadoReal = 0.0;
    double interesPagadoReal = 0.0;
    double capitalPagadoReal = 0.0;
    
    for (PagoPrestamo pago : pagos) {
        if (pago.getDetallePrestamo().getCodigo().equals(cuota.getCodigo())) {
            desgravamenPagadoReal += nullSafe(pago.getDesgravamen());
            interesPagadoReal += nullSafe(pago.getInteresPagado());
            capitalPagadoReal += nullSafe(pago.getCapitalPagado());
        }
    }
    
    // 3. Calcular saldos pendientes
    saldos.saldoDesgravamen = Math.max(0, desgravamenEsperado - desgravamenPagadoReal);
    saldos.saldoInteres = Math.max(0, interesEsperado - interesPagadoReal);
    saldos.saldoCapital = Math.max(0, capitalEsperado - capitalPagadoReal);
    saldos.totalPendiente = saldos.saldoDesgravamen + saldos.saldoInteres + saldos.saldoCapital;
    
    // ✅ CORRECCIÓN CRÍTICA: Validar si la cuota YA está completa según PagoPrestamo
    double totalEsperado = desgravamenEsperado + interesEsperado + capitalEsperado;
    double totalPagado = desgravamenPagadoReal + interesPagadoReal + capitalPagadoReal;
    
    if (Math.abs(totalPagado - totalEsperado) <= TOLERANCIA && 
        saldos.totalPendiente <= TOLERANCIA) {
        
        // La cuota YA está completamente pagada
        if (cuota.getEstado() != PAGADA) {
            System.out.println("    ✅✅ CORRECCIÓN: Cuota ya completada según PagoPrestamo → Actualizando a PAGADA");
            System.out.println("        Total esperado: $" + totalEsperado);
            System.out.println("        Total pagado (PagoPrestamo): $" + totalPagado);
            
            // ✅ Actualizar cuota a PAGADA
            cuota.setEstado(PAGADA);
            cuota.setFechaPagado(LocalDateTime.now());
            cuota.setCapitalPagado(capitalPagadoReal);
            cuota.setInteresPagado(interesPagadoReal);
            cuota.setDesgravamenPagado(desgravamenPagadoReal);
            cuota.setSaldoCapital(0.0);
            cuota.setSaldoInteres(0.0);
            
            // ✅ Guardar cambios
            detallePrestamoService.saveSingle(cuota);
            System.out.println("    ✅✅ Cuota actualizada y guardada como PAGADA");
        }
    }
    
    return saldos;
}
```

**Beneficios:**
- ✅ Detecta automáticamente cuotas completadas
- ✅ Actualiza el estado a PAGADA inmediatamente
- ✅ Sincroniza los valores pagados con PagoPrestamo
- ✅ Establece saldos en 0
- ✅ Registra la fecha de pago

### Cambio 2: Saltar Cuotas Ya Completadas en el Bucle

Se actualizó `procesarPagoPlazoVencido()` para verificar si la cuota fue actualizada:

```java
for (DetallePrestamo cuota : todasLasCuotas) {
    // ...validaciones previas...
    
    // Calcular saldos reales
    // IMPORTANTE: Este método puede actualizar la cuota a PAGADA si ya está completa
    SaldosRealesCuota saldos = calcularSaldosRealesCuota(cuota);
    
    // ✅ CORRECCIÓN: Recargar el estado después de calcular saldos
    Long estadoActualizado = cuota.getEstado();
    
    // Si fue actualizada a PAGADA automáticamente, saltarla
    if (estadoActualizado != null && estadoActualizado == PAGADA) {
        System.out.println("    ⏭️ Cuota ya está PAGADA (actualizada automáticamente) - Continuando con siguiente");
        continue; // ✅ Saltar y continuar con la siguiente cuota
    }
    
    // Validar saldos pendientes
    if (saldos.totalPendiente <= 0.01) {
        // Marcar como PAGADA si no lo está ya
        cuota.setEstado(PAGADA);
        cuota.setFechaPagado(LocalDateTime.now());
        detallePrestamoService.saveSingle(cuota);
        continue;
    }
    
    // Aplicar nuevo pago a esta cuota
    double montoAplicado = aplicarPagoACuotaPlazoVencido(...);
    montoRestante -= montoAplicado;
    
    // ...resto del código...
}
```

**Beneficios:**
- ✅ No intenta aplicar pagos a cuotas ya completadas
- ✅ Continúa automáticamente con la siguiente cuota pendiente
- ✅ Evita crear registros de pago duplicados
- ✅ Optimiza el procesamiento

---

## 🔄 FLUJO COMPLETO CORREGIDO

### Ejemplo Detallado

**Situación Inicial:**
```
Préstamo DE_PLAZO_VENCIDO con 3 cuotas:

Cuota #1:
- Total esperado: $100 (Desg: $10, Int: $30, Cap: $60)
- Estado en DTPR: MORA
- Pagos en PagoPrestamo:
  * Pago hace 2 meses: $60
  * Pago hace 1 mes: $40
  * TOTAL PAGADO: $100 ✅ (COMPLETA pero estado no actualizado)

Cuota #2:
- Total esperado: $100
- Estado en DTPR: PARCIAL
- Pagos en PagoPrestamo:
  * Pago hace 1 mes: $50
  * TOTAL PAGADO: $50 (pendiente $50)

Cuota #3:
- Total esperado: $100
- Estado en DTPR: PENDIENTE
- Pagos en PagoPrestamo: NINGUNO

Archivo nuevo:
- Monto a aplicar: $150
```

### ❌ ANTES (INCORRECTO)

```
Procesamiento:

1. Procesar Cuota #1:
   - Calcular saldos → $0 pendiente
   - ❌ Estado sigue en MORA (no se actualiza)
   - ❌ Intenta aplicar pago de $150
   - ❌ Crea registro de pago duplicado
   - ❌ Confusión en el sistema

2. Resultado: FALLO en procesamiento
```

### ✅ DESPUÉS (CORRECTO)

```
Procesamiento:

1. Procesar Cuota #1:
   ├─ Consultar PagoPrestamo:
   │  └─ Total pagado: $100
   ├─ Calcular saldos:
   │  └─ Pendiente: $0
   ├─ ✅ VALIDACIÓN AUTOMÁTICA:
   │  ├─ Total pagado ($100) = Total esperado ($100)
   │  ├─ Saldos = $0
   │  └─ ✅ Actualizar a PAGADA
   ├─ ✅ Guardar cuota actualizada
   └─ ⏭️ SALTAR - Continuar con siguiente
   
   Logs:
   "✅✅ CORRECCIÓN: Cuota ya completada según PagoPrestamo → Actualizando a PAGADA"
   "    Total esperado: $100.00"
   "    Total pagado (PagoPrestamo): $100.00"
   "    ✅✅ Cuota actualizada y guardada como PAGADA"
   "    ⏭️ Cuota ya está PAGADA (actualizada automáticamente) - Continuando con siguiente"

2. Procesar Cuota #2:
   ├─ Consultar PagoPrestamo:
   │  └─ Total pagado: $50
   ├─ Calcular saldos:
   │  └─ Pendiente: $50 (Desg:$0, Int:$15, Cap:$35)
   ├─ Estado: PARCIAL (correcto, no se actualiza)
   ├─ Aplicar pago de $150:
   │  ├─ Aplicar $50 a esta cuota → Completa
   │  ├─ Cuota #2 → PAGADA ✅
   │  └─ Excedente: $100
   └─ Crear registro de pago: $50
   
   Logs:
   "Pago préstamo plazo vencido - Cuota #2 - Carga 352 (Desg:$0, Int:$15, Cap:$35)"

3. Procesar Cuota #3:
   ├─ Consultar PagoPrestamo:
   │  └─ Total pagado: $0
   ├─ Calcular saldos:
   │  └─ Pendiente: $100
   ├─ Estado: PENDIENTE (correcto)
   ├─ Aplicar excedente de $100:
   │  ├─ Aplicar $100 a esta cuota → Completa
   │  └─ Cuota #3 → PAGADA ✅
   └─ Crear registro de pago: $100
   
   Logs:
   "⚠️ Aplicando EXCEDENTE de cuota #2"
   "Excedente de $100.00 del mes 3/2026 aplicado desde carga archivo 352 (Desg:$10, Int:$30, Cap:$60)"

RESULTADO FINAL:
✅ Cuota #1: PAGADA (actualizada automáticamente)
✅ Cuota #2: PAGADA (completada con archivo actual)
✅ Cuota #3: PAGADA (completada con excedente)
✅ Monto restante: $0
✅ Proceso exitoso
```

---

## 📊 CASOS DE USO RESUELTOS

### Caso 1: Cuota Completada en Cargas Anteriores

**Situación:**
- Cuota pagada en 2 o más archivos anteriores
- Estado en BD: PARCIAL o MORA (desactualizado)
- PagoPrestamo: Suma completa

**Solución:**
```
✅ calcularSaldosRealesCuota() detecta que está completa
✅ Actualiza a PAGADA automáticamente
✅ El bucle la salta y continúa con la siguiente
✅ No se crean pagos duplicados
```

### Caso 2: Cuota Parcialmente Pagada

**Situación:**
- Cuota con pagos parciales en archivos anteriores
- Estado en BD: PARCIAL (correcto)
- PagoPrestamo: Suma parcial

**Solución:**
```
✅ calcularSaldosRealesCuota() calcula saldo pendiente correcto
✅ NO actualiza el estado (no está completa)
✅ Aplica el nuevo pago al saldo pendiente
✅ Si se completa ahora → PAGADA
✅ Si no se completa → Sigue PARCIAL
```

### Caso 3: Cuota Sin Pagos Previos

**Situación:**
- Cuota sin pagos anteriores
- Estado en BD: PENDIENTE o EMITIDA
- PagoPrestamo: Sin registros

**Solución:**
```
✅ calcularSaldosRealesCuota() detecta saldo completo pendiente
✅ NO actualiza el estado (no hay pagos previos)
✅ Aplica el nuevo pago normalmente
✅ Proceso estándar
```

---

## 🎯 BENEFICIOS DE LA CORRECCIÓN

### 1. **Sincronización Automática** ✅
- La tabla `DetallePrestamo` se mantiene sincronizada con `PagoPrestamo`
- Los estados reflejan la realidad de los pagos acumulados
- No hay inconsistencias entre tablas

### 2. **Procesamiento Eficiente** ✅
- No se pierda tiempo procesando cuotas ya completadas
- El monto disponible se aplica solo a cuotas pendientes
- Mejor aprovechamiento de excedentes

### 3. **Trazabilidad Correcta** ✅
- Los registros de pago son precisos
- No hay duplicados ni confusiones
- Auditoría clara y confiable

### 4. **Prevención de Errores** ✅
- No se intentan aplicar pagos a cuotas completas
- No se crean registros incorrectos
- El flujo es predecible y robusto

---

## 🧪 PRUEBAS RECOMENDADAS

### Test 1: Cuota Completada en Archivos Anteriores

**Setup:**
```sql
-- Crear cuota
INSERT INTO detalle_prestamo (codigo, numero_cuota, capital, interes, desgravamen, estado)
VALUES (100, 1, 60, 30, 10, 4); -- Estado 4 = MORA

-- Crear pagos previos que suman el total
INSERT INTO pago_prestamo (detalle_prestamo_id, capital_pagado, interes_pagado, desgravamen, valor_pagado)
VALUES (100, 40, 20, 5, 65);  -- Primer pago

INSERT INTO pago_prestamo (detalle_prestamo_id, capital_pagado, interes_pagado, desgravamen, valor_pagado)
VALUES (100, 20, 10, 5, 35);  -- Segundo pago

-- Total pagado: $100 (completo)
```

**Procesar archivo con monto $200**

**Verificar:**
```sql
-- La cuota debe estar PAGADA (estado 2)
SELECT estado, capital_pagado, interes_pagado, desgravamen_pagado, fecha_pagado
FROM detalle_prestamo
WHERE codigo = 100;
-- Esperado: estado = 2, valores actualizados, fecha_pagado NOT NULL

-- NO debe haber nuevo registro de pago para esta cuota
SELECT COUNT(*) 
FROM pago_prestamo 
WHERE detalle_prestamo_id = 100;
-- Esperado: 2 (los 2 previos, no uno nuevo)

-- El monto debe haberse aplicado a la siguiente cuota
```

### Test 2: Cuota Parcial que se Completa Ahora

**Setup:**
```sql
-- Cuota parcialmente pagada
INSERT INTO detalle_prestamo (codigo, numero_cuota, capital, interes, desgravamen, estado)
VALUES (101, 2, 60, 30, 10, 5); -- Estado 5 = PARCIAL

-- Pago previo parcial
INSERT INTO pago_prestamo (detalle_prestamo_id, capital_pagado, interes_pagado, desgravamen, valor_pagado)
VALUES (101, 30, 15, 5, 50);  -- Falta $50

-- Total pagado: $50, falta: $50
```

**Procesar archivo con monto $50**

**Verificar:**
```sql
-- La cuota debe pasar a PAGADA
SELECT estado, capital_pagado, interes_pagado 
FROM detalle_prestamo
WHERE codigo = 101;
-- Esperado: estado = 2, capital_pagado = 60, interes_pagado = 30

-- Debe tener 2 registros de pago
SELECT COUNT(*) 
FROM pago_prestamo 
WHERE detalle_prestamo_id = 101;
-- Esperado: 2 (el previo + el nuevo)
```

### Test 3: Múltiples Cuotas - Primera Completa, Siguientes Pendientes

**Setup:**
```sql
-- Cuota 1: Completa (pagada anteriormente)
INSERT INTO pago_prestamo VALUES (...); -- Total $100

-- Cuota 2: Pendiente
-- Sin pagos

-- Cuota 3: Pendiente
-- Sin pagos
```

**Procesar archivo con monto $250**

**Verificar:**
```
Cuota 1: PAGADA (sin nuevo pago aplicado)
Cuota 2: PAGADA (pagada con archivo actual: $100)
Cuota 3: PAGADA (pagada con excedente: $150)
```

---

## 📝 LOGS ESPERADOS

### Cuota Ya Completa (Actualización Automática)

```
Procesando cuota #1 - Estado: 4
  Pagos encontrados en tabla PagoPrestamo:
    Desgravamen pagado: $10.00
    Interés pagado: $30.00
    Capital pagado: $60.00
  ✅✅ CORRECCIÓN: Cuota ya completada según PagoPrestamo → Actualizando a PAGADA
      Total esperado: $100.00
      Total pagado (PagoPrestamo): $100.00
  ✅✅ Cuota actualizada y guardada como PAGADA
  ⏭️ Cuota ya está PAGADA (actualizada automáticamente) - Continuando con siguiente
```

### Cuota Pendiente (Procesamiento Normal)

```
Procesando cuota #2 - Estado: 6
  Pagos encontrados en tabla PagoPrestamo:
    Desgravamen pagado: $0.00
    Interés pagado: $0.00
    Capital pagado: $0.00
  Saldos reales calculados:
    Desgravamen pendiente: $10.00
    Interés pendiente: $30.00
    Capital pendiente: $60.00
    TOTAL pendiente: $100.00
  ⚙️ Afectación AUTOMÁTICA (sin AVPC)
  ✅ Cuota COMPLETADA → PAGADA (sin seguro)
  ✅ Aplicado: $100.00 | Restante: $150.00
```

---

## ✅ RESULTADO FINAL

Con esta corrección crítica, el sistema ahora:

1. ✅ **Valida automáticamente** si una cuota está completa según `PagoPrestamo`
2. ✅ **Actualiza el estado a PAGADA** cuando corresponde
3. ✅ **Salta cuotas completadas** y continúa con las pendientes
4. ✅ **Sincroniza** `DetallePrestamo` con `PagoPrestamo`
5. ✅ **Evita pagos duplicados** a cuotas ya completadas
6. ✅ **Optimiza el procesamiento** de archivos con préstamos de plazo vencido
7. ✅ **Mantiene consistencia** entre las tablas del sistema

**El problema de cuotas completadas que no pasaban a PAGADA está RESUELTO.**

---

## 📚 ARCHIVOS RELACIONADOS

- **Implementación:** `CargaArchivoPetroServiceImpl.java`
- **Documentación previa:** 
  - `CORRECCION-PRESTAMOS-PLAZO-VENCIDO.md`
  - `CORRECCION-DESGLOSE-EXCEDENTES-PLAZO-VENCIDO.md`
- **Documentación general:** `PROCESO-CARGA-ARCHIVO-PETROCOMERCIAL-COMPLETO.md`

---

**FIN DEL DOCUMENTO**
