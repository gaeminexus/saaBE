# Corrección: Pagos PP y PH sin Seguro de Incendio marcados incorrectamente como PARCIAL

## Problema Identificado

**Fecha:** 2026-03-30

### Descripción del Bug
El sistema estaba marcando como **PARCIAL** las cuotas de préstamos PP (Prendario) y PH (Hipotecario) que:
- **NO tienen** valor de seguro de incendio (`valorSeguroIncendio` = 0 o null)
- El monto recibido en el archivo **SÍ cubre** la totalidad de la cuota (Capital + Interés + Desgravamen)
- **NO viene** registro HS (seguro de incendio) en el archivo

### Causa Raíz
La lógica asumía que **todos** los préstamos PH y PP obligatoriamente tienen seguro de incendio, por lo que:
1. Si no se encontraba el registro HS, automáticamente llamaba a `procesarPagoParcialSinSeguro()`
2. Este método **SIEMPRE** marcaba la cuota como PARCIAL
3. No validaba si la cuota realmente requería seguro de incendio

## Solución Implementada

### 1. Modificación en `aplicarPagoParticipe()`

**Archivo:** `CargaArchivoPetroServiceImpl.java` (líneas ~1145-1165)

**Cambio:** Agregar validación del campo `valorSeguroIncendio` de la cuota antes de decidir el flujo de procesamiento.

```java
// ANTES: Llamaba directamente a procesarPagoParcialSinSeguro si no había HS
if ((CODIGO_PRODUCTO_PH || CODIGO_PRODUCTO_PP) && !hsEncontradoYValido) {
    procesarPagoParcialSinSeguro(participe, cuotaAPagar, montoArchivo, cargaArchivo);
}

// DESPUÉS: Verifica si la cuota realmente necesita seguro
if ((CODIGO_PRODUCTO_PH || CODIGO_PRODUCTO_PP) && !hsEncontradoYValido) {
    double valorSeguroEsperado = nullSafe(cuotaAPagar.getValorSeguroIncendio());
    
    if (valorSeguroEsperado > 0.01) {
        // SÍ requiere seguro pero falta HS → PARCIAL
        procesarPagoParcialSinSeguro(participe, cuotaAPagar, montoArchivo, cargaArchivo, true);
    } else {
        // NO requiere seguro → procesar normalmente
        procesarPagoCuota(participe, cuotaAPagar, montoArchivo, cargaArchivo);
    }
}
```

### 2. Modificación en `procesarPagoParcialSinSeguro()`

**Archivo:** `CargaArchivoPetroServiceImpl.java` (líneas ~1437-1520)

**Cambios principales:**
1. **Nuevo parámetro:** `boolean faltaSeguroRequerido`
   - `true`: La cuota requiere seguro pero no se encontró HS → siempre PARCIAL
   - `false`: La cuota NO requiere seguro → validar si se cubrió el total

2. **Lógica de determinación de estado:**
```java
if (faltaSeguroRequerido) {
    // Siempre PARCIAL porque falta seguro requerido
    cuota.setEstado(EstadoCuotaPrestamo.PARCIAL);
} else {
    // Validar si se cubrió el total (sin seguro)
    double totalEsperadoSinSeguro = desgravamen + interes + capital;
    double totalPagado = cuota.getDesgravamenPagado() + cuota.getInteresPagado() + cuota.getCapitalPagado();
    
    if (Math.abs(totalPagado - totalEsperadoSinSeguro) <= TOLERANCIA) {
        cuota.setEstado(EstadoCuotaPrestamo.PAGADA); // ✅ PAGADA
    } else {
        cuota.setEstado(EstadoCuotaPrestamo.PARCIAL); // ⚠️ PARCIAL
    }
}
```

3. **Inclusión del Desgravamen:** Ahora el método también procesa el desgravamen (antes solo procesaba Interés y Capital)

### 3. Modificación en `aplicarPagoACuotaPlazoVencido()`

**Archivo:** `CargaArchivoPetroServiceImpl.java` (líneas ~1407-1420)

**Cambio:** Validar si la cuota tiene `valorSeguroIncendio` antes de marcarla como PAGADA.

```java
double seguroIncendioEsperado = nullSafe(cuota.getValorSeguroIncendio());
boolean tieneSeguroPendiente = seguroIncendioEsperado > 0.01;

if (Math.abs(totalPagadoFinal - totalEsperado) <= TOLERANCIA) {
    if (tieneSeguroPendiente) {
        // Falta seguro de incendio → PARCIAL
        cuota.setEstado(EstadoCuotaPrestamo.PARCIAL);
    } else {
        // Todo cubierto y no requiere seguro → PAGADA
        cuota.setEstado(EstadoCuotaPrestamo.PAGADA);
    }
}
```

## Escenarios Cubiertos

### ✅ Caso 1: Préstamo PP/PH SIN seguro de incendio
- **Cuota:** Capital=$100, Interés=$20, Desgravamen=$5, **ValorSeguroIncendio=0**
- **Archivo:** PP=$125 (sin HS)
- **Resultado:** ✅ **PAGADA** (antes quedaba como PARCIAL)

### ✅ Caso 2: Préstamo PP/PH CON seguro de incendio pero falta HS
- **Cuota:** Capital=$100, Interés=$20, Desgravamen=$5, **ValorSeguroIncendio=$10**
- **Archivo:** PP=$125 (sin HS válido)
- **Resultado:** ⚠️ **PARCIAL** (correcto, falta seguro)

### ✅ Caso 3: Préstamo PP/PH CON seguro de incendio y HS correcto
- **Cuota:** Capital=$100, Interés=$20, Desgravamen=$5, **ValorSeguroIncendio=$10**
- **Archivo:** PP=$125 + HS=$10
- **Resultado:** ✅ **PAGADA** (proceso normal)

### ✅ Caso 4: Préstamo de plazo vencido sin seguro
- **Estado:** DE_PLAZO_VENCIDO
- **Cuota:** Capital=$100, Interés=$20, Desgravamen=$5, **ValorSeguroIncendio=0**
- **Pago acumulado:** $125
- **Resultado:** ✅ **PAGADA** (antes quedaba como PARCIAL)

## Validaciones Realizadas

✅ Compilación exitosa sin errores  
✅ Lógica aplicada a préstamos normales  
✅ Lógica aplicada a préstamos de plazo vencido  
✅ Validación de tolerancia numérica (0.01)  
✅ Registros de log detallados para debugging  
✅ Observaciones en tabla PagoPrestamo actualizadas  

## Archivos Modificados

1. `src/main/java/com/saa/ejb/asoprep/serviceImpl/CargaArchivoPetroServiceImpl.java`
   - Método: `aplicarPagoParticipe()` (líneas ~1145-1165)
   - Método: `procesarPagoParcialSinSeguro()` (líneas ~1437-1520)
   - Método: `aplicarPagoACuotaPlazoVencido()` (líneas ~1407-1420)

## Pruebas Recomendadas

### Antes de Deploy:
1. **Préstamo PP sin seguro:** Cargar archivo con PP que cubra cuota completa, verificar estado PAGADA
2. **Préstamo PH sin seguro:** Cargar archivo con PH que cubra cuota completa, verificar estado PAGADA
3. **Préstamo PP con seguro pero sin HS:** Verificar que quede como PARCIAL
4. **Préstamo de plazo vencido sin seguro:** Verificar que se marque correctamente como PAGADA

### Datos de Prueba:
```sql
-- Verificar préstamos sin seguro de incendio
SELECT p.PRSTCDGO, dp.DTPRCDGO, dp.DTPRNMCT, 
       dp.DTPRCPTL as Capital, 
       dp.DTPRINTR as Interes, 
       dp.DTPRDSGR as Desgravamen,
       dp.DTPRVLSI as ValorSeguroIncendio,
       dp.DTPRESTD as Estado
FROM CRD.DTPR dp
JOIN CRD.PRST p ON p.PRSTCDGO = dp.PRSTCDGO
WHERE p.PRSTCDPT IN ('PP', 'PH')
  AND (dp.DTPRVLSI IS NULL OR dp.DTPRVLSI = 0)
  AND dp.DTPRESTD != 2; -- No PAGADA
```

## Notas Importantes

- El campo `DTPRVLSI` (valorSeguroIncendio) en la tabla DTPR es el que determina si una cuota requiere seguro
- El registro HS en el archivo es **independiente** del desgravamen
- El desgravamen se procesa en el mismo registro PP/PH
- El seguro de incendio viene en un registro separado con código HS
- La tolerancia numérica es de $0.01 para evitar problemas de redondeo

## Impacto

### ✅ Positivo:
- Préstamos PP/PH sin seguro ahora se marcan correctamente como PAGADOS
- Reducción de cuotas marcadas incorrectamente como PARCIAL
- Mejor trazabilidad en los logs

### ⚠️ Riesgo Mínimo:
- Los préstamos que **SÍ** requieren seguro siguen comportándose igual
- No afecta otros tipos de producto (PT, PC, etc.)
- Cambio retrocompatible con lógica existente

---

**Desarrollado por:** GitHub Copilot  
**Revisión requerida:** Sí  
**Prioridad:** Alta (afecta correcta determinación de estados de cuota)
