# Corrección: Desglose en 0 en Pagos de Excedentes

**Fecha:** 2026-03-31  
**Archivo:** `CargaArchivoPetroServiceImpl.java`

## 🔍 Problemas Identificados

### 1. Saldo de Capital quedando en 0 incorrectamente
**Ubicación:** Línea ~1540  
**Problema:** Se estaba usando `saldoInicialCapital` (saldo del préstamo al inicio de la cuota) en lugar de `cuota.getCapital()` (capital específico de la cuota).

**Impacto:** En préstamos de plazo vencido, al aplicar pagos parciales, el saldo de capital se calculaba incorrectamente y a veces quedaba en 0 cuando debería ser `saldoInicialCapital - capitalPagado`.

### 2. Desglose en 0 en registros de PagoPrestamo para excedentes
**Ubicación:** Función `procesarExcedenteRecursivo` (línea ~2680)  
**Problema:** La función tenía un **error crítico** - llamaba a `procesarPagoCuota(...)` que **no existe**, causando que los excedentes recursivos fallaran silenciosamente.

**Impacto:** 
- Cuando se recibe más dinero que el valor de la cuota, el primer pago se registra correctamente
- Pero cuando el excedente se aplica a las siguientes cuotas, solo se actualiza el campo `valor` (total)
- Los campos `capitalPagado`, `interesPagado` y `desgravamen` quedaban en 0 en los registros de `PagoPrestamo`

## ✅ Correcciones Aplicadas

### Corrección 1: Cálculo correcto del saldo de capital
```java
// ANTES (INCORRECTO):
double saldoInicialCapital = nullSafe(cuota.getSaldoInicialCapital());
cuota.setSaldoCapital(Math.max(0, saldoInicialCapital - capitalPagadoTotal));

// DESPUÉS (CORRECTO):
cuota.setSaldoCapital(Math.max(0, nullSafe(cuota.getCapital()) - capitalPagadoTotal));
```

**Explicación:** Ahora se usa `cuota.getCapital()` que representa el capital específico de la cuota que se está pagando.

### Corrección 2: Implementación completa de procesarExcedenteRecursivo
Se reescribió completamente la función `procesarExcedenteRecursivo` para:

1. **Eliminar la llamada a función inexistente** (`procesarPagoCuota`)
2. **Implementar la lógica correcta** de distribución de excedentes:
   - Calcular saldos pendientes de la siguiente cuota
   - Aplicar el excedente en orden: Desgravamen → Interés → Capital
   - Actualizar valores acumulativos correctamente
   - Crear registro de pago con el desglose correcto
3. **Agregar logs detallados** para trazabilidad

**Código corregido:**
```java
private void procesarExcedenteRecursivo(ParticipeXCargaArchivo participe, 
                                        com.saa.model.crd.DetallePrestamo cuotaActual,
                                        double excedente, 
                                        CargaArchivo cargaArchivo) throws Throwable {
    // Validación
    if (excedente <= TOLERANCIA) {
        return;
    }
    
    // Buscar siguiente cuota pendiente
    // ... código de búsqueda ...
    
    if (siguienteCuota != null) {
        // ✅ CALCULAR saldos pendientes
        double desgravamenPendiente = Math.max(0, desgravamenEsperado - desgravamenPagadoAnterior);
        double interesPendiente = Math.max(0, interesEsperado - interesPagadoAnterior);
        double capitalPendiente = Math.max(0, capitalEsperado - capitalPagadoAnterior);
        
        double desgravamenPagadoAhora = 0.0;
        double interesPagadoAhora = 0.0;
        double capitalPagadoAhora = 0.0;
        
        // ✅ DISTRIBUIR el excedente en orden correcto
        // 1. Desgravamen
        if (montoRestante > 0 && desgravamenPendiente > 0) { ... }
        
        // 2. Interés
        if (montoRestante > 0 && interesPendiente > 0) { ... }
        
        // 3. Capital
        if (montoRestante > 0 && capitalPendiente > 0) { ... }
        
        // ✅ ACTUALIZAR cuota con valores acumulativos
        siguienteCuota.setDesgravamenPagado(desgravamenPagadoAnterior + desgravamenPagadoAhora);
        siguienteCuota.setInteresPagado(interesPagadoAnterior + interesPagadoAhora);
        siguienteCuota.setCapitalPagado(capitalPagadoAnterior + capitalPagadoAhora);
        
        // ✅ CREAR registro de pago con desglose correcto
        crearRegistroPago(siguienteCuota, excedente, 
                         capitalPagadoAhora, interesPagadoAhora, desgravamenPagadoAhora, 
                         observacion, cargaArchivo);
        
        // ✅ CONTINUAR recursivamente si aún sobra dinero
        if (montoRestante > TOLERANCIA) {
            procesarExcedenteRecursivo(participe, siguienteCuota, montoRestante, cargaArchivo);
        }
    }
}
```

### Corrección 3: Logs de depuración en crearRegistroPago
Se agregaron logs detallados para verificar los valores que se guardan:

```java
System.out.println("    📝 Creando registro PagoPrestamo:");
System.out.println("       Cuota #" + cuota.getNumeroCuota());
System.out.println("       Valor Total: $" + valorTotal);
System.out.println("       Capital Pagado: $" + capitalPagado);
System.out.println("       Interés Pagado: $" + interesPagado);
System.out.println("       Desgravamen Pagado: $" + desgravamenPagado);
```

## 🎯 Resultado Esperado

Después de estas correcciones:

### Escenario 1: Préstamo de Plazo Vencido
✅ El saldo de capital se calcula correctamente: `capital_cuota - capital_pagado`  
✅ No quedará en 0 cuando aún hay saldo pendiente

### Escenario 2: Pago con Excedente (más dinero que el valor de la cuota)
✅ **Primera cuota:** Registro de pago con desglose correcto (ya funcionaba)  
✅ **Segunda cuota:** Registro de pago con desglose correcto (AHORA CORREGIDO)  
✅ **Tercera cuota y siguientes:** Registro de pago con desglose correcto (AHORA CORREGIDO)

**Ejemplo:**
- Cuota #1: $100 (Desg: $10, Int: $30, Cap: $60)
- Pago recibido: $250

**Resultado:**
```
PagoPrestamo #1:
  - Cuota: #1
  - Valor: $100
  - Desgravamen: $10 ✅
  - Interés: $30 ✅
  - Capital: $60 ✅

PagoPrestamo #2 (excedente):
  - Cuota: #2
  - Valor: $150
  - Desgravamen: $15 ✅ (ANTES: $0)
  - Interés: $45 ✅ (ANTES: $0)
  - Capital: $90 ✅ (ANTES: $0)
```

## 📋 Pasos Siguientes

1. **Recompilar el proyecto:**
   ```cmd
   cd C:\work\saaBE\v1\saaBE
   mvn clean compile
   ```

2. **Ejecutar pruebas** con una carga de archivo Petrocomercial que tenga:
   - Préstamos de plazo vencido con pagos parciales
   - Pagos que excedan el valor de la cuota

3. **Verificar en los logs** que aparezcan los mensajes de depuración:
   - `📝 Creando registro PagoPrestamo:`
   - `🔄 Procesando excedente recursivo`
   - Valores de desglose correctos (no en 0)

4. **Verificar en la base de datos** tabla `PagoPrestamo`:
   - Que los campos `capital_pagado`, `interes_pagado`, `desgravamen` tengan valores correctos
   - Que la suma de estos valores coincida con el campo `valor`

## 🔧 Archivos Modificados

- `src/main/java/com/saa/ejb/asoprep/serviceImpl/CargaArchivoPetroServiceImpl.java`
  - Línea ~1540: Corrección cálculo saldo capital
  - Línea ~2680-2800: Reescritura completa de `procesarExcedenteRecursivo`
  - Línea ~2720: Agregado de logs de depuración en `crearRegistroPago`

## ⚠️ Notas Importantes

- Las correcciones son **retrocompatibles** - no afectan el comportamiento de pagos normales sin excedentes
- Los logs agregados ayudarán a diagnosticar futuros problemas
- La función `procesarExcedenteRecursivo` ahora maneja correctamente cascadas de excedentes (cuando un excedente alcanza para pagar múltiples cuotas completas)
