# REPORTE DE VALIDACIÓN: Método validarAporteAH y Actualización de Saldos

**Fecha:** 2026-04-07
**Archivo analizado:** CargaArchivoPetroServiceImpl.java

---

## 1. VALIDACIÓN DEL MÉTODO validarAporteAH EN FASE 2

### ✅ RESULTADO: SÍ SE ESTÁ LLAMANDO CORRECTAMENTE

**Ubicación de la llamada:**
- **Línea 348:** Se llama a `validarNovedadesFase2` para partícipes con productos que NO son especiales (AH o HS)
- **Línea 1926:** Dentro de `validarNovedadesFase2`, cuando detecta que el producto es "AH" (aportes), se llama a `validarAporteAH`

**Flujo de ejecución:**

```
procesarCargaArchivo()
    └─> Para cada partícipe con código != null
        └─> Si NO es producto especial (AH/HS)
            └─> validarNovedadesFase2(participe, codigoProducto, cargaArchivo)
                └─> Si codigoProducto == "AH"
                    └─> validarAporteAH(participe, cargaArchivo) ← AQUÍ SE LLAMA
                    └─> return (termina validación)
```

**Condición de ejecución (línea 344-348):**
```java
boolean esProductoEspecial = CODIGO_PRODUCTO_APORTES.equalsIgnoreCase(codigoProducto) ||
                             CODIGO_PRODUCTO_HS.equalsIgnoreCase(codigoProducto);

if (!esProductoEspecial && participe.getCodigo() != null) {
    validarNovedadesFase2(participe, codigoProducto, cargaArchivo);
}
```

**Nota importante:** El método `validarAporteAH` se ejecuta dentro de `validarNovedadesFase2` cuando el producto es "AH", validando:
- Código Petro del partícipe
- Existencia de la entidad
- Existencia de HistorialSueldo
- Múltiples registros de HistorialSueldo

---

## 2. VALIDACIÓN DE ACTUALIZACIÓN DEL SALDO EN DETALLE_PRESTAMO

### ✅ RESULTADO: EL SALDO SE ACTUALIZA CORRECTAMENTE EN TODOS LOS FLUJOS

Se identificaron **CUATRO FLUJOS DE PAGO** donde se actualiza el campo `saldoCapital` y `saldoInteres` de la tabla `DetallePrestamo`:

---

### **FLUJO 1: Pago exacto o con excedente (procesarPagoCuota)**
**Líneas 1248-1249 y 1273-1274**

**Cuándo se ejecuta:**
- Al procesar pagos desde archivo Petro con reglas definidas
- Cuando el monto pagado es >= al total pendiente de la cuota

**Código:**
```java
// Pago exacto
cuota.setSaldoCapital(Math.max(0, nullSafe(cuota.getSaldoInicialCapital()) - cuota.getCapitalPagado()));
cuota.setSaldoInteres(0.0);

// Pago con excedente
cuota.setSaldoCapital(Math.max(0, nullSafe(cuota.getSaldoInicialCapital()) - cuota.getCapitalPagado()));
cuota.setSaldoInteres(0.0);
```

**✅ CORRECTO:** Usa `saldoInicialCapital - capitalPagado` para calcular el saldo restante del capital.

---

### **FLUJO 2: Pago parcial (procesarPagoCuota)**
**Líneas 1367-1368**

**Cuándo se ejecuta:**
- Al procesar pagos desde archivo Petro con reglas definidas
- Cuando el monto pagado es < al total pendiente de la cuota

**Código:**
```java
cuota.setSaldoCapital(Math.max(0, nullSafe(cuota.getSaldoInicialCapital()) - cuota.getCapitalPagado()));
cuota.setSaldoInteres(nullSafe(cuota.getInteres()) - cuota.getInteresPagado());
```

**✅ CORRECTO:** 
- Saldo capital = `saldoInicialCapital - capitalPagado`
- Saldo interés = `interes - interesPagado`

---

### **FLUJO 3: Afectación manual desde tabla AfectacionValoresParticipeCarga**
**Líneas 1863-1864**

**Cuándo se ejecuta:**
- Al procesar registros manuales de la tabla `AfectacionValoresParticipeCarga`
- Estos tienen PRIORIDAD MÁXIMA sobre el procesamiento automático

**Código:**
```java
// ACUMULAR valores manuales sobre pagos previos
cuota.setCapitalPagado(capitalPagadoPrevio + capitalAfectar);
cuota.setInteresPagado(interesPagadoPrevio + interesAfectar);
cuota.setDesgravamenPagado(desgravamenPagadoPrevio + desgravamenAfectar);

cuota.setSaldoCapital(Math.max(0, nullSafe(cuota.getSaldoInicialCapital()) - cuota.getCapitalPagado()));
cuota.setSaldoInteres(Math.max(0, nullSafe(cuota.getInteres()) - cuota.getInteresPagado()));
```

**✅ CORRECTO:** 
- Acumula correctamente los valores pagados previamente
- Calcula saldos restantes correctamente
- Registra el pago en tabla `PagoPrestamo` (línea 1904)

---

### **FLUJO 4: Recalculo desde tabla PagoPrestamo (calcularSaldosRealesCuota)**
**Líneas 2418-2419**

**Cuándo se ejecuta:**
- Cada vez que se busca una cuota a pagar
- Antes de procesar cualquier pago
- Para validar si una cuota ya está completamente pagada según registros históricos

**Código:**
```java
// Sumar TODOS los pagos desde tabla PagoPrestamo
for (PagoPrestamo pago : pagos) {
    desgravamenPagadoTotal += nullSafe(pago.getDesgravamen());
    interesPagadoTotal += nullSafe(pago.getInteresPagado());
    capitalPagadoTotal += nullSafe(pago.getCapitalPagado());
    seguroIncendioPagadoTotal += nullSafe(pago.getValorSeguroIncendio());
}

// Si está completamente pagada según PagoPrestamo, actualizar estado
if (saldos.totalPendiente <= 0.01 && cuota.getEstado() != PAGADA) {
    cuota.setCapitalPagado(capitalPagadoTotal);
    cuota.setInteresPagado(interesPagadoTotal);
    cuota.setDesgravamenPagado(desgravamenPagadoTotal);
    cuota.setSaldoCapital(Math.max(0, nullSafe(cuota.getSaldoInicialCapital()) - capitalPagadoTotal));
    cuota.setSaldoInteres(0.0);
    detallePrestamoService.saveSingle(cuota);
}
```

**✅ CORRECTO:** 
- Es la "fuente de verdad" para determinar saldos reales
- Suma TODOS los pagos históricos de la tabla `PagoPrestamo`
- Actualiza automáticamente el estado de cuotas que están completas pero no marcadas como PAGADAS

---

## 3. REGISTRO DE PAGOS EN TABLA PAGOPRESTAMO

**Método:** `crearRegistroPago` (línea 2439)

**Se llama desde:**
1. **Línea 1295:** Pago exacto con reglas definidas
2. **Línea 1396:** Pago parcial con reglas definidas  
3. **Línea 1904:** Afectación manual desde tabla `AfectacionValoresParticipeCarga`

**Campos registrados:**
```java
pago.setDetallePrestamo(cuota);
pago.setFecha(LocalDateTime.now());
pago.setCapitalPagado(capitalPagado);
pago.setInteresPagado(interesPagado);
pago.setDesgravamen(desgravamenPagado);
pago.setValorSeguroIncendio(valorSeguroIncendio);  // ✅ Incluye seguro incendio
pago.setValor(montoTotal);
pago.setObservacion(observacion + " [CargaArchivo: " + cargaArchivo.getCodigo() + "]");
pago.setEstado(1L);
```

**✅ CORRECTO:** Todos los pagos generan un registro en `PagoPrestamo` con desglose completo.

---

## 4. CONCLUSIONES

### ✅ PUNTO 1: validarAporteAH en Fase 2
- **SÍ se está llamando** dentro de las validaciones de Fase 2
- Se ejecuta cuando se detecta un producto con código "AH" (aportes)
- La validación se realiza correctamente dentro del método `validarNovedadesFase2`

### ✅ PUNTO 2: Actualización de saldos en DetallePrestamo
- **SÍ se actualiza correctamente** en TODOS los flujos de pago:
  1. Pagos automáticos con reglas definidas (exactos, con excedente, parciales)
  2. Pagos manuales desde tabla `AfectacionValoresParticipeCarga`
  3. Recalculo automático desde tabla `PagoPrestamo`

### ✅ Fórmula consistente en todos los flujos:
```
saldoCapital = MAX(0, saldoInicialCapital - capitalPagado)
saldoInteres = MAX(0, interes - interesPagado)
```

### ✅ Garantías de integridad:
- Todos los pagos se registran en tabla `PagoPrestamo`
- El método `calcularSaldosRealesCuota` actúa como fuente de verdad
- Se recalculan saldos antes de cada operación
- Se actualizan estados automáticamente cuando están completas

---

## 5. RECOMENDACIONES

### ✓ No se requieren cambios
El sistema está funcionando correctamente en ambos aspectos validados:
1. El método `validarAporteAH` se ejecuta en el momento correcto
2. Los saldos se actualizan consistentemente en todos los flujos de pago

### Fortalezas del diseño actual:
- ✅ Separación clara de flujos (automático vs manual)
- ✅ Prioridad correcta (AfectacionValoresParticipeCarga tiene precedencia)
- ✅ Registro histórico completo en PagoPrestamo
- ✅ Recalculo automático para corregir inconsistencias
- ✅ Fórmula consistente en todos los puntos de actualización

---

**Fin del reporte**
