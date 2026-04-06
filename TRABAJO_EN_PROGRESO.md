# Trabajo en Progreso - Correcciones Backend

## Última actualización: 2026-04-06

## Correcciones completadas:

### 1. ✅ Mayorizacion (YA COMPLETADO)
- Corregido método `save()` en MayorizacionServiceImpl
- Problema: No se reasignaba el objeto después de `em.persist()`/`em.merge()`

### 2. ✅ MayorAnalitico (YA COMPLETADO)
- Corregido método `save()` en MayorAnaliticoServiceImpl
- Problema: No se reasignaba el objeto después de grabar

### 3. ✅ TempReportes (COMPLETADO 2026-02-27)
- **Entidad reorganizada**: TempReportes.java
  - Campos ordenados: @Id primero, campos persistidos, luego @Transient
  - Agregadas anotaciones @Transient a campos no persistidos
- **Servicio corregido**: TempReportesServiceImpl.java
  - Método `save(List<TempReportes> lista)` corregido
  - Ahora reasigna correctamente: `tempReportes = tempReportesDaoService.save(...)`

### 4. ✅ ELIMINACIÓN DE TOLERANCIA AL APLICAR PAGOS (COMPLETADO 2026-04-06)

#### 🎯 Objetivo:
Al aplicar pagos, el sistema debe procesar **cualquier cuota con saldo pendiente mayor a $0**, incluso si falta 1 centavo. 
La tolerancia de $1.00 solo debe usarse en **validaciones (validarNovedadesFase2)** para reportar novedades.

#### 📝 Archivo modificado:
`CargaArchivoPetroServiceImpl.java`

#### ✅ Correcciones aplicadas (7 cambios):

**1. Línea 1181 - buscarCuotaAPagar():**
```java
// ANTES: if (saldoPendiente > TOLERANCIA)
// AHORA: if (saldoPendiente > 0)
```
✅ Ahora encuentra cualquier cuota con saldo > 0

**2. Línea 1518 - procesarExcedenteASiguienteCuota():**
```java
// ANTES: if (excedente <= TOLERANCIA)
// AHORA: if (excedente <= 0)
```
✅ Procesa cualquier excedente > 0

**3. Línea 1542 - procesarExcedenteASiguienteCuota():**
```java
// ANTES: if (saldoPendiente > TOLERANCIA)
// AHORA: if (saldoPendiente > 0)
```
✅ Al buscar siguiente cuota, considera cualquier saldo > 0

**4. Línea 2218 - procesarPagoPlazoVencido():**
```java
// ANTES: if (montoRestante <= 0.01)
// AHORA: if (montoRestante <= 0)
```
✅ Procesa préstamos de plazo vencido hasta agotar completamente el monto

**5. Línea 2273 - procesarPagoPlazoVencido():**
```java
// ANTES: if (montoRestante > 0.01)
// AHORA: if (montoRestante > 0)
```
✅ Aplica cualquier excedente a siguiente cuota

**6. Línea 2285 - procesarPagoPlazoVencido():**
```java
// ANTES: if (montoRestante > 0.01)
// AHORA: if (montoRestante > 0)
```
✅ Reporta cualquier monto sobrante

**7. Línea 1069 - Validación HS:**
```java
// ANTES: if (diferenciaHS <= TOLERANCIA)
// AHORA: if (diferenciaHS <= 0.01)
```
✅ Comparación exacta para validar seguro de incendio

### 5. ✅ VALIDACIÓN INTELIGENTE DE SEGURO DE INCENDIO (HS) (COMPLETADO 2026-04-06)

#### 🎯 Problema identificado:
1. El sistema asumía automáticamente error cuando NO encontraba registro HS para productos PH/PP
2. NO validaba si la cuota realmente requiere seguro de incendio (valorSeguroIncendio > 0)
3. NO procesaba montos parciales de HS (si recibía $5 de $10 esperados, perdía los $5)

#### ✅ Solución implementada (Líneas 1044-1105):

**Flujo correcto:**
1. **Busca la cuota primero** para obtener `valorSeguroIncendio`
2. **Si `valorSeguroIncendio > 0.01`**: La cuota SÍ requiere seguro
   - Busca registro HS
   - Si encuentra HS: **SIEMPRE procesa el monto recibido** (aunque no corresponda exactamente)
   - Si NO encuentra HS: Marca error y la cuota queda PARCIAL
3. **Si `valorSeguroIncendio <= 0.01`**: La cuota NO requiere seguro
   - NO busca HS (no es necesario)
   - Procesa normalmente sin seguro

**Ejemplos de comportamiento:**

**Escenario 1: Cuota requiere $10, recibe $5 de HS**
```
✅ Suma montoArchivo += $5
✅ Procesa los $5 recibidos
✅ La cuota queda PARCIAL (falta $5 de seguro)
```

**Escenario 2: Cuota requiere $10, NO encuentra HS**
```
❌ hsEncontradoYValido = false
❌ NO suma nada
❌ La cuota queda PARCIAL (falta todo el seguro)
```

**Escenario 3: Cuota NO requiere seguro (valorSeguroIncendio = $0)**
```
✅ NO busca HS (no es necesario)
✅ hsEncontradoYValido = true
✅ Procesa normalmente
```

#### 🔑 Código clave (Líneas 1044-1105):
```java
// Buscar cuota para validar si requiere seguro
com.saa.model.crd.DetallePrestamo cuotaValidar = buscarCuotaAPagar(prestamos, cargaArchivo);
double seguroIncendioEsperado = nullSafe(cuotaValidar.getValorSeguroIncendio());

if (seguroIncendioEsperado > 0.01) {
    // La cuota SÍ requiere seguro - Buscar HS
    ParticipeXCargaArchivo participeHS = ...;
    
    if (participeHS != null) {
        montoHS = nullSafe(participeHS.getTotalDescontado());
        double diferenciaHS = Math.abs(seguroIncendioEsperado - montoHS);
        
        // ✅ SIEMPRE sumar el montoHS recibido
        if (diferenciaHS <= 0.01) {
            // Monto corresponde exactamente
            montoArchivo += montoHS;
        } else {
            // Monto NO corresponde pero SÍ se procesa
            montoArchivo += montoHS;
            hsEncontradoYValido = true;
            // La cuota quedará PARCIAL automáticamente por el faltante
        }
    } else {
        // NO se encontró HS cuando SÍ se requiere
        hsEncontradoYValido = false;
    }
} else {
    // La cuota NO requiere seguro
    hsEncontradoYValido = true;
}
```

### 📊 Resumen de impacto:

**ANTES:**
- ❌ No procesaba cuotas con saldo < $1.00
- ❌ Asumía error automático si no encontraba HS para PH/PP
- ❌ No procesaba montos parciales de HS

**AHORA:**
- ✅ Procesa cualquier cuota con saldo > $0 (incluso 1 centavo)
- ✅ Valida si realmente se requiere seguro antes de asumir error
- ✅ Procesa montos parciales de HS correctamente
- ✅ Mantiene tolerancia de $1.00 solo en validaciones para reportar novedades

## Correcciones completadas:

## Problema identificado:
En varios servicios, después de llamar a `daoService.save()`, no se reasignaba el resultado a la variable.
Esto causa problemas con:
- IDs generados que no se actualizan
- Estado de entidades desincronizado
- Referencias a objetos detached

## Patrón de corrección:

### ❌ ANTES (Incorrecto):
```java
for (Entidad entidad : lista) {
    daoService.save(entidad, entidad.getCodigo());
}
```

### ✅ DESPUÉS (Correcto):
```java
for (int i = 0; i < lista.size(); i++) {
    Entidad entidad = lista.get(i);
    entidad = daoService.save(entidad, entidad.getCodigo());
    lista.set(i, entidad);
}
```

## Pendientes:
- [ ] Revisar otros servicios que puedan tener el mismo problema
- [ ] Verificar si hay más entidades con campos sin @Transient

## Notas:
- Los métodos individuales que ya reasignan (`entidad = daoService.save()`) están correctos
- Solo revisar métodos que iteran sobre listas o que no reasignan
