# Plan de Eliminación de Tolerancia en Procesamiento de Archivo Petro

## Fecha: 2026-04-02
## Última actualización: 2026-04-02 - Corrección crítica calcularSaldosRealesCuota

## Situación Actual

El sistema tiene DOS fases diferentes:

### FASE 1 y 2: VALIDACIÓN DEL ARCHIVO (Frontend)
- **Ubicación**: Método `procesarArchivoPetro()` - líneas de validación
- **Comportamiento CORRECTO (NO CAMBIAR)**:
  - Registra novedades cuando hay diferencias menores a $1.00
  - Usa `TOLERANCIA = 1.0` para identificar diferencias aceptables
  - Genera novedades de tipo `DIFERENCIA_MENOR_UN_DOLAR`
  - El usuario puede revisar estas novedades en el frontend
  - El usuario puede crear registros en `afectacionValoresParticipeCarga` para resolver las novedades

### FASE 3: PROCESAMIENTO/APLICACIÓN DE PAGOS
- **Ubicación**: Método `aplicarPagosArchivoPetro()`
- **Comportamiento A CORREGIR**:
  - Actualmente aplica pagos con tolerancia de $1.00
  - **DEBE CAMBIAR**: NO debe haber tolerancia en la aplicación de pagos
  - Los pagos deben aplicarse por el monto EXACTO sin ajustes de tolerancia

## Problema Identificado

En el método `aplicarPagosArchivoPetro()` se están aplicando pagos con tolerancia, lo que causa que:
1. Se apliquen pagos con montos ajustados por la tolerancia
2. No se respeten los valores exactos del archivo o las afectaciones definidas por el usuario

## Solución a Implementar

### Paso 1: Localizar el método `aplicarPagosArchivoPetro()`
- Buscar línea aproximada 859 donde inicia el método

### Paso 2: Identificar dónde se aplican pagos a préstamos
- Buscar llamadas a métodos de aplicación de pagos
- Identificar dónde se usan comparaciones con `TOLERANCIA`

### Paso 3: Eliminar lógica de tolerancia en aplicación de pagos
- **MANTENER**: Validaciones de existencia de cuotas
- **MANTENER**: Verificaciones de estado de préstamos
- **ELIMINAR**: Cualquier ajuste de montos basado en tolerancia
- **ELIMINAR**: Comparaciones como `if (diferencia <= TOLERANCIA)` en fase de aplicación

### Paso 4: Asegurar aplicación exacta de montos
- Los pagos deben aplicarse por el monto exacto del archivo
- Si existe registro en `afectacionValoresParticipeCarga`, usar esos valores
- Si NO existe afectación, usar el monto del archivo tal cual

### Paso 5: Verificar flujo de aportes (producto AH)
- Los aportes deben crearse con los montos exactos
- Sin ajustes por tolerancia

### Paso 6: Validar que no haya regresiones
- Compilar el proyecto
- Verificar que no se introduzcan errores
- Asegurar que las validaciones de Fase 1 y 2 sigan funcionando

## Archivos Afectados

1. `CargaArchivoPetroServiceImpl.java`
   - Método: `aplicarPagosArchivoPetro()` (línea ~859)
   - Buscar y modificar SOLO la lógica de aplicación de pagos
   - NO modificar métodos de validación

## Reglas Importantes

1. **NO TOCAR** las validaciones de Fase 1 y 2
2. **NO MODIFICAR** el registro de novedades de diferencias menores a $1
3. **ELIMINAR** tolerancia SOLO en la aplicación efectiva de pagos
4. **MANTENER** toda la lógica de afectaciones del usuario
5. **RESPETAR** montos exactos del archivo

## Resultado Esperado

- Validación (Fase 1 y 2): Sigue registrando novedades de diferencias menores a $1
- Procesamiento (Fase 3): Aplica pagos sin tolerancia, montos exactos
- Usuario puede resolver novedades con afectaciones manuales
- Sistema respeta valores exactos en todo momento

## CORRECCIÓN CRÍTICA IMPLEMENTADA - 2026-04-02

### Problema Detectado
El método `procesarPagoCuota()` NO estaba consultando la tabla `PagoPrestamo` para verificar pagos previos. 
Esto causaba que:
1. Si se reprocesaba un archivo del mismo mes, no se detectaban pagos duplicados
2. Los pagos previos registrados en `PagoPrestamo` no se consideraban
3. Podía haber inconsistencias entre `DetallePrestamo` y `PagoPrestamo`

### Solución Implementada
Se integró el método `calcularSaldosRealesCuota()` en el flujo normal de procesamiento de pagos activos:

**Antes:**
```java
// Calculaba saldos basándose SOLO en campos de DetallePrestamo
double desgravamenPagadoPrevio = nullSafe(cuota.getDesgravamenPagado());
double capitalPendiente = Math.max(0, nullSafe(cuota.getCapital()) - capitalPagadoPrevio);
```

**Después:**
```java
// Consulta tabla PagoPrestamo para obtener saldos REALES
SaldosRealesCuota saldos = calcularSaldosRealesCuota(cuota);
double capitalPendiente = saldos.saldoCapital;
```

### Beneficios
1. ✅ **Fuente única de verdad:** `PagoPrestamo` es la tabla que registra TODOS los pagos
2. ✅ **Detección de pagos completos:** Si una cuota ya está pagada según `PagoPrestamo`, se detecta automáticamente
3. ✅ **Prevención de duplicados:** Si se reprocesa un archivo, el sistema detecta pagos previos
4. ✅ **Consistencia de datos:** `DetallePrestamo` se actualiza según la realidad de `PagoPrestamo`
5. ✅ **Cuotas se marcan correctamente como PAGADAS:** Cuando los pagos en `PagoPrestamo` suman el total de la cuota

### Flujo Mejorado
1. Al procesar una cuota, primero consulta `PagoPrestamo`
2. Suma todos los pagos previos registrados
3. Calcula el saldo pendiente REAL
4. Si el saldo es ≤ $0.01, actualiza la cuota a PAGADA automáticamente
5. Si la cuota ya está PAGADA, pasa el monto completo a la siguiente cuota
6. Solo aplica el nuevo pago si hay saldo pendiente

## CORRECCIÓN CRÍTICA 2 - Inclusión del Seguro de Incendio

### Problema Detectado
El método `calcularSaldosRealesCuota()` **NO estaba considerando el seguro de incendio** en los cálculos de saldos reales.

Solo calculaba:
- ✅ Desgravamen
- ✅ Interés
- ✅ Capital
- ❌ **Seguro de Incendio (FALTABA)**

Esto causaba que:
1. Las cuotas de PH/PP (que tienen seguro de incendio) no se marcaran correctamente como PAGADAS
2. El saldo pendiente calculado era incorrecto
3. Los pagos no se distribuían correctamente cuando incluían seguro de incendio

### Solución Implementada

**1. Actualizada la clase `SaldosRealesCuota`:**
```java
private static class SaldosRealesCuota {
    double saldoDesgravamen = 0.0;
    double saldoInteres = 0.0;
    double saldoCapital = 0.0;
    double saldoSeguroIncendio = 0.0;  // ✅ AGREGADO
    double totalPendiente = 0.0;
}
```

**2. Actualizado `calcularSaldosRealesCuota()` para incluir seguro de incendio:**
```java
// Consulta PagoPrestamo y suma TODOS los componentes incluyendo seguro de incendio
seguroIncendioPagadoTotal += nullSafe(pago.getValorSeguroIncendio());
saldos.saldoSeguroIncendio = Math.max(0, nullSafe(cuota.getValorSeguroIncendio()) - seguroIncendioPagadoTotal);
saldos.totalPendiente = saldos.saldoDesgravamen + saldos.saldoInteres + saldos.saldoCapital + saldos.saldoSeguroIncendio;
```

**3. Actualizado el orden de prelación de pagos:**
- Orden anterior: Desgravamen → Interés → Capital
- **Orden nuevo:** Desgravamen → Interés → Capital → **Seguro Incendio**

**4. Actualizado el registro en `PagoPrestamo`:**
```java
// Ahora registra correctamente el seguro de incendio pagado
crearRegistroPago(cuota, montoRegistrar, 
    capitalRegistrar, interesRegistrar, desgravamenRegistrar,
    seguroIncendioRegistrar,  // ✅ Valor calculado correctamente
    observacion, cargaArchivo);
```

### Beneficios
1. ✅ **Cálculo correcto del saldo pendiente:** Incluye TODOS los componentes de la cuota
2. ✅ **Cuotas PH/PP se marcan correctamente como PAGADAS:** Solo cuando se paga TODO incluyendo seguro
3. ✅ **Distribución correcta de pagos:** El seguro de incendio se aplica en el orden correcto
4. ✅ **Registro completo en PagoPrestamo:** Se guarda el monto exacto del seguro pagado
5. ✅ **Consistencia con archivo Petro:** El seguro HS se procesa y registra correctamente

### Ejemplo del Impacto

**Cuota PH #5:** 
- Capital: $80
- Interés: $15
- Desgravamen: $3
- **Seguro Incendio: $2**
- **TOTAL: $100**

**Escenario:** Se paga $98 (falta el seguro de incendio)

**ANTES de la corrección:**
- Saldo calculado: Capital + Interés + Desgravamen = $98
- Comparación: $98 == $98 → ✅ Marcaba como PAGADA ❌
- **Problema:** Faltaban $2 del seguro pero la cuota se cerraba

**DESPUÉS de la corrección:**
- Saldo calculado: Capital + Interés + Desgravamen + Seguro = $100 ✅
- Comparación: $98 < $100 → Cuota PARCIAL ✅
- **Resultado:** La cuota queda pendiente hasta que se pague el seguro completo
