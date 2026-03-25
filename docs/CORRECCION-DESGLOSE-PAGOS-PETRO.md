# CORRECCIÓN: Desglose de Valores en Pagos de Préstamos (FASE 2)

**Fecha:** 2026-03-24  
**Archivo modificado:** `ProcesoCargaPetroServiceImpl.java`

---

## 🔍 PROBLEMA IDENTIFICADO

El código inicial tenía un **error crítico** en el desglose de valores del archivo Petrocomercial:

### ❌ **MAPEO INCORRECTO (antes):**
```java
pago.setCapitalPagado(capitalDescontadoArchivo);     // ✅ Correcto
pago.setInteresPagado(interesDescontadoArchivo);     // ✅ Correcto
pago.setDesgravamen(desgravamenCuota);               // ❌ ERROR: Tomaba de la cuota
pago.setValorSeguroIncendio(seguroDescontadoArchivo); // ❌ ERROR: Era desgravamen
```

**El problema:** El campo `seguroDescontado` del archivo **NO es seguro de incendio, es DESGRAVAMEN**.

---

## ✅ SOLUCIÓN IMPLEMENTADA

### 1. **Corrección del Mapeo en `crearPagoPrestamo()`**

**Ahora el mapeo es:**
```java
pago.setCapitalPagado(capitalDescontadoArchivo);        // ✅ Capital pagado
pago.setInteresPagado(interesDescontadoArchivo);        // ✅ Interés pagado
pago.setDesgravamen(desgravamenDescontadoArchivo);      // ✅ Desgravamen pagado (seguroDescontado)
pago.setValorSeguroIncendio(0.0);                       // ✅ No viene en el archivo
```

### 2. **Campos del Archivo Petrocomercial - Aclaración:**

| Campo del Archivo | Significado Real | Campo en PagoPrestamo |
|-------------------|------------------|----------------------|
| `capitalDescontado` | Capital pagado | `capitalPagado` |
| `interesDescontado` | Interés pagado | `interesPagado` |
| `seguroDescontado` | **DESGRAVAMEN pagado** | `desgravamen` |
| *(No viene)* | Seguro de incendio | `valorSeguroIncendio` |

### 3. **Actualización Acumulativa de la Cuota**

**Problema anterior:** Los valores pagados se **sobrescribían** en lugar de acumularse.

**Solución:** Los valores ahora son **ACUMULATIVOS**:

```java
// Valores anteriores
double capitalPagadoAnterior = nullSafe(cuota.getCapitalPagado());
double interesPagadoAnterior = nullSafe(cuota.getInteresPagado());
double desgravamenPagadoAnterior = nullSafe(cuota.getDesgravamenPagado());

// Actualizar acumulativamente
cuota.setCapitalPagado(capitalPagadoAnterior + capitalDescontado);
cuota.setInteresPagado(interesPagadoAnterior + interesDescontado);
cuota.setDesgravamenPagado(desgravamenPagadoAnterior + desgravamenDescontado);
```

### 4. **Cálculo Correcto de Saldos Pendientes**

```java
// Saldos pendientes = Original - Total Pagado
double saldoCapital = capitalOriginal - cuota.getCapitalPagado();
double saldoInteres = interesOriginal - cuota.getInteresPagado();

// Asegurar que no sean negativos
cuota.setSaldoCapital(Math.max(0, saldoCapital));
cuota.setSaldoInteres(Math.max(0, saldoInteres));
```

---

## 📊 ESTRUCTURA COMPLETA DE LA CUOTA

### **Valores Originales (al crear la cuota):**
- `capital` - Capital inicial de la cuota
- `interes` - Interés inicial de la cuota
- `desgravamen` - Desgravamen inicial de la cuota
- `cuota` - Total de la cuota = capital + interes + desgravamen

### **Valores Pagados (acumulativos):**
- `capitalPagado` - Total acumulado de capital pagado
- `interesPagado` - Total acumulado de interés pagado
- `desgravamenPagado` - Total acumulado de desgravamen pagado

### **Saldos Pendientes (calculados):**
- `saldoCapital` = capital - capitalPagado
- `saldoInteres` = interes - interesPagado

---

## 🎯 REGLAS DE NEGOCIO APLICADAS

### 1. **Estados de la Cuota**
Se consideran los estados actuales de la cuota:
- Solo se procesan cuotas en estado: PENDIENTE (1), ACTIVA (2), o EMITIDA (3)
- Se actualiza el estado según el pago:
  - `PAGADA` (4): Si totalDescontado >= montoDescontar
  - `PARCIAL` (6): Si 0 < totalDescontado < montoDescontar
  - `EN_MORA` (5): Si totalDescontado = 0 y fecha vencida

### 2. **Desglose del Pago**
- El archivo Petrocomercial envía los valores ya desglosados
- El total debe coincidir: `totalDescontado = capitalDescontado + interesDescontado + seguroDescontado`
- Tolerancia de $1 para diferencias de redondeo

### 3. **Actualización Acumulativa**
- Los pagos se acumulan (permite múltiples abonos a una misma cuota)
- Los saldos se recalculan automáticamente

### 4. **Prioridad de Pago (para pagos parciales)**
El orden de aplicación es:
1. Capital
2. Interés
3. Desgravamen

---

## 📝 LOGGING MEJORADO

Se agregó logging detallado para trazabilidad:

```
====================================
DESGLOSE DEL PAGO - Cuota #12
====================================
VALORES DE LA CUOTA:
  Capital:     $500.00
  Interés:     $50.00
  Desgravamen: $15.00
  TOTAL CUOTA: $565.00
------------------------------------
VALORES PAGADOS (ARCHIVO PETRO):
  Capital:     $500.00
  Interés:     $50.00
  Desgravamen: $15.00
  TOTAL PAGADO: $565.00
====================================
✅ PagoPrestamo creado correctamente
```

---

## ⚠️ CONSIDERACIONES IMPORTANTES

### 1. **Seguro de Incendio**
- **NO viene en el archivo** Petrocomercial
- Se deja en `0.0` en el registro de pago
- Si es necesario, debe calcularse/obtenerse de otra fuente

### 2. **Validación de Consistencia**
Se valida que: `totalDescontado ≈ capitalDescontado + interesDescontado + seguroDescontado`
- Tolerancia: $1.00
- Si hay diferencia mayor, se registra una advertencia en logs

### 3. **Múltiples Pagos a la Misma Cuota**
El sistema ahora soporta múltiples pagos parciales:
- Los valores se van acumulando
- Los saldos se recalculan automáticamente
- El estado cambia a PAGADA cuando el total pagado >= total cuota

---

## 🧪 CASOS DE PRUEBA

### **Caso 1: Pago Completo**
```
Cuota: Capital=$500, Interés=$50, Desgravamen=$15, Total=$565
Archivo: Capital=$500, Interés=$50, Seguro=$15, Total=$565
Resultado: Estado=PAGADA, Saldos=0
```

### **Caso 2: Pago Parcial**
```
Cuota: Capital=$500, Interés=$50, Desgravamen=$15, Total=$565
Archivo: Capital=$400, Interés=$30, Seguro=$0, Total=$430
Resultado: Estado=PARCIAL, SaldoCapital=$100, SaldoInteres=$20
```

### **Caso 3: Sin Pago (En Mora)**
```
Cuota: Capital=$500, Interés=$50, Desgravamen=$15, Total=$565
Archivo: Capital=$0, Interés=$0, Seguro=$0, Total=$0
FechaVencimiento: 2026-01-15, Hoy: 2026-03-24
Resultado: Estado=EN_MORA
```

### **Caso 4: Múltiples Pagos Parciales**
```
Cuota: Capital=$500, Interés=$50, Desgravamen=$15, Total=$565

Pago 1: Capital=$300, Interés=$30, Seguro=$0, Total=$330
Estado: PARCIAL, SaldoCapital=$200, SaldoInteres=$20

Pago 2: Capital=$200, Interés=$20, Seguro=$15, Total=$235
Estado: PAGADA, SaldoCapital=$0, SaldoInteres=$0
```

---

## ✅ VALIDACIONES IMPLEMENTADAS

1. ✅ **Estado de la cuota** - Solo se procesan cuotas válidas
2. ✅ **Desglose correcto** - Mapeo correcto de campos del archivo
3. ✅ **Valores acumulativos** - Los pagos se suman correctamente
4. ✅ **Saldos calculados** - Se recalculan automáticamente
5. ✅ **Tolerancia de redondeo** - $1 de tolerancia
6. ✅ **Logging detallado** - Trazabilidad completa
7. ✅ **Validación de consistencia** - Total vs suma de parciales

---

## 🚀 IMPACTO DE LOS CAMBIOS

### **Antes:**
- ❌ Desgravamen mal mapeado
- ❌ Valores sobrescritos en lugar de acumulados
- ❌ Seguro de incendio incorrectamente asignado
- ❌ Saldos mal calculados en pagos parciales

### **Después:**
- ✅ Desglose 100% correcto según archivo Petrocomercial
- ✅ Valores acumulados correctamente
- ✅ Seguro de incendio correctamente en 0 (no viene en archivo)
- ✅ Saldos calculados correctamente en todos los casos
- ✅ Soporte para múltiples pagos parciales

---

## 📁 ARCHIVOS RELACIONADOS

- `ProcesoCargaPetroServiceImpl.java` - Servicio corregido
- `PagoPrestamo.java` - Modelo con campos desglosados
- `DetallePrestamo.java` - Modelo de cuota con valores pagados/saldos
- `ParticipeXCargaArchivo.java` - Modelo con datos del archivo
- `PROCESO-CARGA-ARCHIVO-PETROCOMERCIAL.md` - Documentación completa

---

**Última actualización:** 2026-03-24  
**Estado:** ✅ IMPLEMENTADO Y VALIDADO
