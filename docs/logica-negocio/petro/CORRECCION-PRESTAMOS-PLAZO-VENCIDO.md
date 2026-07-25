# CORRECCIÓN: Procesamiento de Préstamos en Estado DE_PLAZO_VENCIDO

**Fecha:** 2026-03-31  
**Archivo corregido:** `CargaArchivoPetroServiceImpl.java`  
**Métodos corregidos:** `procesarPagoPlazoVencido()`, `aplicarPagoACuotaPlazoVencido()`

---

## 🔴 PROBLEMAS IDENTIFICADOS

### Síntomas
Al procesar archivos Petro con préstamos en estado **DE_PLAZO_VENCIDO**:
1. ❌ **No se actualizaban las cuotas** - Los valores pagados no se reflejaban en DetallePrestamo
2. ❌ **No se aplicaban los pagos** - No se creaban registros en PagoPrestamo
3. ❌ **Estado no pasaba a PAGADA** - Aunque el pago era completo, la cuota quedaba en estado anterior
4. ❌ **No consideraba el seguro HS** - Para PH/PP no buscaba ni sumaba el registro HS
5. ❌ **No usaba afectaciones manuales** - Ignoraba completamente la tabla AVPC

### Causas Raíz

#### 1. **No Buscaba el Registro HS para PH/PP** ❌
```java
// CÓDIGO ANTERIOR (INCORRECTO)
private void procesarPagoPlazoVencido(..., double montoPago, ...) {
    double montoRestante = montoPago; // ⚠️ Solo usa el monto del producto
    // NO buscaba HS, NO lo sumaba
}
```

**Problema:** 
- Para PH/PP, el seguro de incendio viene en un registro separado (código HS)
- El código anterior solo usaba el monto del producto (PH o PP)
- Nunca encontraba ni sumaba el monto HS
- Resultado: Cuota siempre quedaba PARCIAL (faltaba el seguro)

#### 2. **No Verificaba Afectación Manual** ❌
```java
// CÓDIGO ANTERIOR (INCORRECTO)
private double aplicarPagoACuotaPlazoVencido(DetallePrestamo cuota, ...) {
    // Aplicaba directamente reglas automáticas
    // NO verificaba tabla AVPC
}
```

**Problema:**
- Según Regla 1: La tabla AVPC tiene PRIORIDAD sobre reglas automáticas
- El código de plazo vencido omitía completamente esta verificación
- Los usuarios configuraban afectaciones manuales pero no se aplicaban

#### 3. **Validación Incorrecta del Estado PAGADA** ❌
```java
// CÓDIGO ANTERIOR (INCORRECTO)
if (tieneSeguroPendiente) {
    cuota.setEstado(PARCIAL); // ⚠️ Siempre PARCIAL si requiere seguro
} else {
    cuota.setEstado(PAGADA);
}
```

**Problema:**
- Validaba si la cuota requería seguro (`tieneSeguroPendiente`)
- Pero NO verificaba si el seguro HS había sido encontrado y sumado
- Resultado: Aunque el monto incluía HS, marcaba como PARCIAL

---

## ✅ SOLUCIONES APLICADAS

### 1. **Búsqueda y Suma del Registro HS** 🎯

#### Código Corregido:
```java
private void procesarPagoPlazoVencido(..., double montoPago, ..., String codigoProducto) {
    
    System.out.println("Monto archivo (" + codigoProducto + "): $" + montoPago);
    
    // ✅ CORRECCIÓN: Para PH/PP, buscar y sumar el registro HS
    double montoTotal = montoPago;
    boolean hsEncontrado = false;
    
    if (CODIGO_PRODUCTO_PH.equalsIgnoreCase(codigoProducto) || 
        CODIGO_PRODUCTO_PP.equalsIgnoreCase(codigoProducto)) {
        
        System.out.println("⚠️ Producto " + codigoProducto + " requiere validación de seguro HS");
        
        ParticipeXCargaArchivo participeHS = participeXCargaArchivoDaoService
            .selectByCodigoPetroYProductoEnCarga(
                participe.getCodigoPetro(),
                CODIGO_PRODUCTO_HS,
                cargaArchivo.getCodigo()
            );
        
        if (participeHS != null) {
            double montoHS = nullSafe(participeHS.getTotalDescontado());
            System.out.println("✅ Registro HS encontrado: $" + montoHS);
            montoTotal += montoHS; // ✅ SUMA el seguro
            hsEncontrado = true;
        } else {
            System.out.println("⚠️ No se encontró registro HS");
        }
    }
    
    System.out.println("💰 MONTO TOTAL A APLICAR: $" + montoTotal);
    
    double montoRestante = montoTotal; // ✅ Usa el monto total con HS incluido
}
```

**Beneficios:**
- ✅ Busca el registro HS automáticamente para PH/PP
- ✅ Suma el monto HS al monto total a aplicar
- ✅ Registra si se encontró o no el HS (para validación posterior)
- ✅ Logs claros del proceso

### 2. **Verificación de Afectación Manual (Tabla AVPC)** 🔍

#### Código Corregido:
```java
private double aplicarPagoACuotaPlazoVencido(ParticipeXCargaArchivo participe,
                                             DetallePrestamo cuota, ...) {
    
    // ✅ CORRECCIÓN: PASO 1 - Verificar afectación manual (PRIORIDAD)
    AfectacionValoresParticipeCarga afectacionManual = verificarAfectacionManual(participe, cuota);
    
    if (afectacionManual != null) {
        System.out.println("✅ Afectación MANUAL encontrada - Aplicando valores de tabla AVPC");
        
        // Usar valores exactos de AVPC
        double capitalAfectar = nullSafe(afectacionManual.getCapitalAfectar());
        double interesAfectar = nullSafe(afectacionManual.getInteresAfectar());
        double desgravamenAfectar = nullSafe(afectacionManual.getDesgravamenAfectar());
        
        // Actualizar cuota con valores manuales
        cuota.setCapitalPagado(cuota.getCapitalPagado() + capitalAfectar);
        cuota.setInteresPagado(cuota.getInteresPagado() + interesAfectar);
        cuota.setDesgravamenPagado(cuota.getDesgravamenPagado() + desgravamenAfectar);
        
        // Validar si se completó
        if (pagoCompleto) {
            cuota.setEstado(PAGADA);
            cuota.setFechaPagado(LocalDateTime.now());
        } else {
            cuota.setEstado(PARCIAL);
        }
        
        detallePrestamoService.saveSingle(cuota); // ✅ GUARDAR
        crearRegistroPago(...); // ✅ CREAR REGISTRO PAGO
        
        return valorTotalAfectar;
    }
    
    // Si no hay afectación manual, aplicar reglas automáticas
    System.out.println("⚙️ Afectación AUTOMÁTICA (sin AVPC)");
    // ... resto del código
}
```

**Beneficios:**
- ✅ Verifica PRIMERO si existe afectación manual
- ✅ Si existe, aplica valores exactos de AVPC
- ✅ Guarda la cuota actualizada
- ✅ Crea el registro de pago
- ✅ Cumple la Regla 1 de prioridad de AVPC

### 3. **Validación Correcta del Estado PAGADA con HS** ✅

#### Código Corregido:
```java
// Calcular total esperado SIN seguro de incendio (el seguro viene en HS)
double totalEsperadoSinSeguro = desgravamenEsperado + interesEsperado + capitalEsperado;
double totalPagadoFinal = desgravamenPagadoTotal + interesPagadoTotal + capitalPagadoTotal;

System.out.println("Total esperado (sin seguro): $" + totalEsperadoSinSeguro);
System.out.println("Total pagado: $" + totalPagadoFinal);
System.out.println("Seguro incendio esperado: $" + seguroIncendioEsperado);

// ✅ Verificar si se cubrió desgravamen+interés+capital
boolean pagoCompletoSinSeguro = Math.abs(totalPagadoFinal - totalEsperadoSinSeguro) <= TOLERANCIA;

if (pagoCompletoSinSeguro) {
    // Se cubrió desgravamen+interés+capital
    if (seguroIncendioEsperado > 0.01) {
        // Requiere seguro de incendio
        if ((CODIGO_PRODUCTO_PH.equalsIgnoreCase(codigoProducto) || 
             CODIGO_PRODUCTO_PP.equalsIgnoreCase(codigoProducto)) && hsEncontrado) {
            // ✅ Es PH/PP Y se encontró HS → Pago COMPLETO
            cuota.setEstado(PAGADA);
            cuota.setFechaPagado(LocalDateTime.now());
            cuota.setSaldoCapital(0.0);
            cuota.setSaldoInteres(0.0);
            System.out.println("✅ Cuota COMPLETADA → PAGADA (incluye HS de $" + seguroIncendioEsperado + ")");
        } else {
            // ❌ Requiere seguro pero NO se encontró HS → PARCIAL
            cuota.setEstado(PARCIAL);
            System.out.println("⚠️ Cuota PARCIAL (falta seguro HS: $" + seguroIncendioEsperado + ")");
        }
    } else {
        // No requiere seguro → PAGADA
        cuota.setEstado(PAGADA);
        cuota.setFechaPagado(LocalDateTime.now());
        System.out.println("✅ Cuota COMPLETADA → PAGADA (sin seguro)");
    }
}

// ✅ GUARDAR CUOTA ACTUALIZADA
detallePrestamoService.saveSingle(cuota);

// ✅ CREAR REGISTRO DE PAGO
crearRegistroPago(cuota, totalAplicado, ...);
```

**Beneficios:**
- ✅ Valida primero si se cubrió desgravamen+interés+capital
- ✅ Si requiere seguro, verifica si el HS fue encontrado
- ✅ Si HS fue encontrado → Estado PAGADA
- ✅ Si HS NO fue encontrado → Estado PARCIAL
- ✅ Actualiza fechaPagado cuando corresponde
- ✅ Guarda la cuota en BD
- ✅ Crea el registro en PagoPrestamo

---

## 🔄 FLUJO CORRECTO COMPLETO

### Escenario 1: PH con Seguro HS Completo

**Entrada:**
- Partícipe: 12345 - Juan Pérez
- Préstamo PH en estado DE_PLAZO_VENCIDO
- Cuota #3:
  - Desgravamen: $50
  - Interés: $100
  - Capital: $300
  - Seguro Incendio: $20
  - **Total esperado: $450** (sin contar seguro en el total)
- Archivo:
  - Registro PH: $450
  - Registro HS: $20

**Proceso:**
```
1. Detectar préstamo DE_PLAZO_VENCIDO
2. Buscar registro HS para código 12345
   ✅ Encontrado: $20
3. Calcular monto total: $450 + $20 = $470
4. Buscar cuota #3 pendiente
5. Calcular saldos reales (consultar PagoPrestamo)
6. Verificar afectación manual en AVPC
   ❌ No existe
7. Aplicar reglas automáticas:
   - Desgravamen: $50 ✅
   - Interés: $100 ✅
   - Capital: $300 ✅
   - Restante: $20 (corresponde al seguro HS)
8. Validar pago completo:
   - Total pagado: $450 = Total esperado: $450 ✅
   - Requiere seguro: SÍ ($20)
   - HS encontrado: SÍ ✅
9. ✅ Estado: PAGADA
10. ✅ Guardar cuota actualizada
11. ✅ Crear registro PagoPrestamo
```

**Resultado:**
```
Cuota #3:
- capitalPagado: $300
- interesPagado: $100
- desgravamenPagado: $50
- saldoCapital: $0
- saldoInteres: $0
- estado: PAGADA (2)
- fechaPagado: 2026-03-31 10:30:00
- codigoExterno: 123 (ID de CargaArchivo)

PagoPrestamo (nuevo registro):
- detallePrestamo: FK a Cuota #3
- capitalPagado: $300
- interesPagado: $100
- desgravamen: $50
- valorPagado: $450
- observacion: "Pago préstamo plazo vencido - Cuota #3 - Carga 123..."
```

### Escenario 2: PH sin Seguro HS (Parcial)

**Entrada:**
- Cuota PH que requiere seguro: $20
- Archivo: Solo registro PH ($450), NO hay registro HS

**Proceso:**
```
1. Buscar registro HS
   ❌ No encontrado
2. Monto total: $450 (sin HS)
3. Aplicar pago: $450 cubre desgravamen+interés+capital
4. Validar:
   - Requiere seguro: SÍ ($20)
   - HS encontrado: NO ❌
5. ⚠️ Estado: PARCIAL (falta seguro)
```

**Resultado:**
- Estado: PARCIAL
- Faltan: $20 (seguro de incendio)

### Escenario 3: Con Afectación Manual (AVPC)

**Entrada:**
- Cuota con novedad registrada
- Registro en AVPC:
  - capitalAfectar: $200
  - interesAfectar: $100
  - desgravamenAfectar: $50
  - valorAfectar: $350

**Proceso:**
```
1. Buscar registro HS (si aplica)
2. Verificar afectación manual
   ✅ Encontrada en AVPC
3. Aplicar valores EXACTOS de AVPC:
   - Capital: $200 (en lugar de reglas automáticas)
   - Interés: $100
   - Desgravamen: $50
4. ✅ Guardar cuota
5. ✅ Crear registro pago
6. Observación: "...Afectación manual (AVPC ID: 5)"
```

---

## 📊 COMPARACIÓN: ANTES vs DESPUÉS

| Aspecto | ❌ ANTES | ✅ DESPUÉS |
|---------|----------|------------|
| **Búsqueda HS** | No buscaba | Busca y suma automáticamente |
| **Monto Total** | Solo monto producto | Monto producto + HS (si aplica) |
| **Tabla AVPC** | Ignorada completamente | Verificada primero (prioridad) |
| **Estado PAGADA** | Nunca se aplicaba | Se aplica cuando pago es completo |
| **Actualización Cuota** | No se guardaba | Se guarda con saveSingle() |
| **Registro PagoPrestamo** | No se creaba | Se crea siempre |
| **Validación Seguro** | Incorrecta | Valida si HS fue encontrado |
| **Logs** | Básicos | Detallados y claros |

---

## 🧪 PRUEBAS RECOMENDADAS

### Test 1: PH con HS Completo
1. Crear préstamo PH en estado DE_PLAZO_VENCIDO
2. Generar cuota con seguro de incendio ($20)
3. Procesar archivo con:
   - Registro PH: monto que cubre la cuota
   - Registro HS: $20
4. **Verificar:**
   - ✅ HS fue encontrado y sumado
   - ✅ Cuota estado = PAGADA
   - ✅ fechaPagado actualizada
   - ✅ Saldos en 0
   - ✅ Registro en PagoPrestamo creado

### Test 2: PH sin HS (Parcial)
1. Procesar PH sin registro HS en archivo
2. **Verificar:**
   - ⚠️ Logs muestran "No se encontró registro HS"
   - ⚠️ Cuota estado = PARCIAL
   - ✅ Valores aplicados correctamente
   - ✅ Registro en PagoPrestamo creado

### Test 3: Con Afectación Manual
1. Crear registro en NVPC
2. Crear afectación en AVPC con valores específicos
3. Procesar archivo
4. **Verificar:**
   - ✅ Logs muestran "Afectación MANUAL encontrada"
   - ✅ Se usan valores de AVPC (no automáticos)
   - ✅ Observación indica "AVPC ID: X"

### Test 4: Múltiples Cuotas con Excedente
1. Préstamo con 3 cuotas pendientes
2. Procesar pago que cubre 2 cuotas completas
3. **Verificar:**
   - ✅ Cuota 1: PAGADA
   - ✅ Cuota 2: PAGADA
   - ✅ Cuota 3: PARCIAL (con excedente aplicado)

---

## 📝 CAMBIOS EN PARÁMETROS

### Método `aplicarPagoACuotaPlazoVencido`

**Antes:**
```java
private double aplicarPagoACuotaPlazoVencido(
    DetallePrestamo cuota,
    SaldosRealesCuota saldos,
    double montoDisponible,
    CargaArchivo cargaArchivo
)
```

**Después:**
```java
private double aplicarPagoACuotaPlazoVencido(
    ParticipeXCargaArchivo participe,  // ✅ NUEVO - Para verificar AVPC
    DetallePrestamo cuota,
    SaldosRealesCuota saldos,
    double montoDisponible,
    CargaArchivo cargaArchivo,
    String codigoProducto,             // ✅ NUEVO - Para validar PH/PP
    boolean hsEncontrado               // ✅ NUEVO - Para validar seguro
)
```

---

## ✅ RESULTADO FINAL

Con estas correcciones, el procesamiento de préstamos DE_PLAZO_VENCIDO ahora:

1. ✅ **Busca y suma el registro HS** para PH/PP automáticamente
2. ✅ **Verifica afectación manual** (tabla AVPC) con prioridad
3. ✅ **Actualiza correctamente las cuotas** en DetallePrestamo
4. ✅ **Crea registros en PagoPrestamo** siempre
5. ✅ **Cambia estado a PAGADA** cuando el pago está completo
6. ✅ **Valida el seguro HS correctamente** antes de marcar PAGADA
7. ✅ **Logs detallados** para debugging y seguimiento
8. ✅ **Maneja excedentes** aplicándolos a siguientes cuotas

---

## 📚 ARCHIVOS RELACIONADOS

- **Implementación:** `CargaArchivoPetroServiceImpl.java`
- **Documentación:** `PROCESO-CARGA-ARCHIVO-PETROCOMERCIAL-COMPLETO.md`
- **Corrección AVPC:** `CORRECCION-AFECTACION-MANUAL-AVPC.md`

---

**FIN DEL DOCUMENTO**
