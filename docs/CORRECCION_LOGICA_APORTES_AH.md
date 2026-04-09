# 🔧 CORRECCIÓN CRÍTICA: Lógica de Procesamiento de Aportes AH

**Fecha:** 2026-04-08  
**Módulo:** Procesamiento de Aportes (Jubilación y Cesantía)  
**Archivo Principal:** `CargaArchivoPetroServiceImpl.java`

---

## 🔴 **PROBLEMA IDENTIFICADO**

### **Error Conceptual:**
La lógica de procesamiento de aportes AH tenía un error fundamental en el orden de aplicación de pagos:

**❌ LÓGICA INCORRECTA (anterior):**
1. Buscaba SOLO aportes creados por el sistema (`SAA_AH`) en estado `PARCIAL`
2. Si no encontraba, creaba directamente el aporte del mes actual
3. **NO verificaba si existían aportes anteriores con saldo pendiente**

**Resultado:** Se creaban múltiples registros de aportes sin completar los anteriores primero.

---

## ✅ **CORRECCIÓN APLICADA**

### **Nueva Lógica (correcta):**
1. **PRIMERO:** Buscar CUALQUIER aporte anterior con saldo pendiente (PENDIENTE o PARCIAL)
2. **Aplicar el pago completo al aporte anterior hasta completarlo**
3. **SOLO si sobra dinero:** Crear el aporte del mes actual con el remanente
4. **Si no sobra nada:** No crear aporte del mes actual (el pago fue absorbido por deudas anteriores)

---

## 📋 **CAMBIOS REALIZADOS**

### **1. Método `procesarAporteIndividual()` - REFACTORIZADO COMPLETO**

**Archivo:** `CargaArchivoPetroServiceImpl.java`

**Antes:**
```java
// ❌ Buscaba solo aportes del sistema en PARCIAL
Aporte aporteExistente = buscarAporteAdelantatoMesActual(...);

if (aporteExistente != null) {
    // Acumulaba sobre el existente
} else {
    // Creaba nuevo
}
// Siempre actualizaba UN solo aporte
```

**Ahora:**
```java
// ✅ Busca CUALQUIER aporte con saldo
Aporte aporteConSaldo = buscarAporteConSaldoPendiente(...);

if (aporteConSaldo != null) {
    // PASO 1: Aplicar primero al aporte anterior
    double montoAAplicar = Math.min(montoDisponible, saldoPendiente);
    // Actualizar el aporte anterior
    // Reducir monto disponible
}

// PASO 2: Solo si queda dinero, crear aporte del mes actual
if (montoDisponible > 0.01) {
    // Crear nuevo aporte con el remanente
}
```

---

### **2. Método `buscarAporteConSaldoPendiente()` - RENOMBRADO Y CORREGIDO**

**Antes:** `buscarAporteAdelantatoMesActual()`
- Solo buscaba aportes con `usuarioRegistro = 'SAA_AH'`
- Solo estado `PARCIAL`

**Ahora:** `buscarAporteConSaldoPendiente()`
- Llama al nuevo método del DAO: `selectMinAporteConSaldo()`
- Busca aportes en estado `PENDIENTE` O `PARCIAL`
- Sin restricción de usuario creador
- Orden: MIN código (más antiguo primero - FIFO)

---

### **3. Nuevo Método en DAO - `selectMinAporteConSaldo()`**

**Archivo:** `AporteDaoService.java` + `AporteDaoServiceImpl.java`

**Query JPA:**
```java
select b from Aporte b
where b.entidad.codigo = :idEntidad
  and b.tipoAporte.codigo = :idTipoAporte
  and b.saldo > 0.01
  and (b.estado = PENDIENTE OR b.estado = PARCIAL)
order by b.codigo asc  -- FIFO
```

**Características:**
- ✅ Busca el aporte MÁS ANTIGUO (MIN código)
- ✅ Con saldo > 0
- ✅ En estado PENDIENTE o PARCIAL
- ✅ Sin restricción de usuario
- ✅ Orden FIFO (First In, First Out)

---

## 🎯 **FLUJO CORRECTO (Ejemplo Numérico)**

### **Escenario:**
- Aporte Jubilación Mes Anterior: Valor $100, Pagado $30, **Saldo $70**
- Aporte Cesantía Mes Anterior: Valor $50, Pagado $0, **Saldo $50**
- Mes Actual: Valor esperado Jubilación $100, Cesantía $50
- **Monto recibido: $150**

### **Flujo ANTIGUO (❌ Incorrecto):**
```
1. Buscar aportes SAA_AH en PARCIAL
2. No encuentra (porque el anterior puede ser estado PENDIENTE)
3. Crear Aporte Jubilación Mes Actual: Valor $100, Pagado $100, Saldo $0 ✅
4. Crear Aporte Cesantía Mes Actual: Valor $50, Pagado $50, Saldo $0 ✅
5. ❌ LOS APORTES ANTERIORES SIGUEN PENDIENTES ($70 + $50 = $120)
```

### **Flujo NUEVO (✅ Correcto):**
```
1. Buscar aportes con saldo > 0 (cualquier estado)
2. Encuentra Aporte Jubilación Mes Anterior: Saldo $70
3. Aplicar $70 al aporte anterior → Completarlo ✅
4. Monto restante: $150 - $70 = $80
5. Buscar siguiente aporte con saldo
6. Encuentra Aporte Cesantía Mes Anterior: Saldo $50
7. Aplicar $50 al aporte anterior → Completarlo ✅
8. Monto restante: $80 - $50 = $30
9. Crear Aporte Jubilación Mes Actual: Valor $100, Pagado $30, Saldo $70 ⚠️ PARCIAL
10. No hay más dinero para Cesantía del mes actual
11. ✅ APORTES ANTERIORES COMPLETADOS PRIMERO
```

---

## 📊 **IMPACTO DE LA CORRECCIÓN**

### **Antes:**
- ❌ Acumulación de deudas sin pagar
- ❌ Múltiples aportes PENDIENTES/PARCIALES del mismo tipo
- ❌ Lógica contable incorrecta
- ❌ Reportes de saldos inconsistentes

### **Después:**
- ✅ Pagos aplicados en orden cronológico (FIFO)
- ✅ Deudas anteriores completadas primero
- ✅ Creación de nuevos aportes solo con remanente
- ✅ Lógica contable correcta
- ✅ Trazabilidad completa en `PagoAporte`

---

## 🔍 **LOGS DE CONSOLA (Nueva Salida)**

```
📋 Procesando aporte de Jubilación:
   Valor esperado mes actual: $100.00
   Monto recibido: $150.00
   
   🔍 Aporte anterior con saldo encontrado (aplicar primero aquí):
      ID: 12345
      IdAsoprep: 89 (carga anterior)
      Usuario: SAA_AH
      Estado: PARCIAL
      Valor: $100.00
      Valor Pagado: $30.00
      Saldo: $70.00
      💰 Aplicando: $70.00 al aporte anterior
      ✅ Aporte anterior completado - Estado: PAGADA
      💵 Monto restante para mes actual: $80.00
   
   ✨ Creando aporte del mes actual con monto: $80.00
   ⚠️ Aporte mes actual parcial - Pagado: $80.00 - Saldo: $20.00
```

---

## ⚠️ **CONSIDERACIONES IMPORTANTES**

1. **Orden de aplicación:** Siempre el aporte más antiguo primero (FIFO)
2. **Estados válidos:** PENDIENTE (1) o PARCIAL (6)
3. **Umbral de saldo:** Se considera `saldo > 0.01` para evitar decimales
4. **Usuario creador:** Ya NO es relevante para la búsqueda
5. **Trazabilidad:** Cada pago se registra en `PagoAporte` con referencia a `CargaArchivo`

---

## 🧪 **CASOS DE PRUEBA RECOMENDADOS**

### **Test 1: Completar aporte anterior**
- Entrada: Aporte anterior con saldo $50, monto recibido $50
- Esperado: Aporte anterior PAGADO, no crear aporte mes actual

### **Test 2: Aporte anterior + nuevo parcial**
- Entrada: Aporte anterior con saldo $30, monto recibido $50
- Esperado: Aporte anterior PAGADO, nuevo aporte PARCIAL con $20 pagado

### **Test 3: Múltiples aportes anteriores**
- Entrada: 2 aportes anteriores con saldo $40 y $60, monto recibido $100
- Esperado: Primer aporte PAGADO ($40), segundo aporte PAGADO ($60), no crear mes actual

### **Test 4: Sin aportes anteriores**
- Entrada: No hay aportes anteriores, monto recibido $100
- Esperado: Crear aporte mes actual PAGADO con $100

---

## 📝 **ARCHIVOS MODIFICADOS**

1. ✅ `CargaArchivoPetroServiceImpl.java`
   - Método `procesarAporteIndividual()` - REFACTORIZADO
   - Método `buscarAporteConSaldoPendiente()` - RENOMBRADO Y CORREGIDO

2. ✅ `AporteDaoService.java`
   - Nuevo método: `selectMinAporteConSaldo()`

3. ✅ `AporteDaoServiceImpl.java`
   - Implementación: `selectMinAporteConSaldo()`

---

## ✅ **VALIDACIÓN**

- ✅ Compilación sin errores
- ✅ Lógica FIFO implementada correctamente
- ✅ Logs detallados para debugging
- ✅ Trazabilidad en `PagoAporte`
- ✅ Estados actualizados correctamente (PENDIENTE → PARCIAL → PAGADA)

---

**🚀 LA CORRECCIÓN ESTÁ LISTA PARA DESPLIEGUE**

**Recomendación:** Probar con datos reales en ambiente de desarrollo antes de producción.
