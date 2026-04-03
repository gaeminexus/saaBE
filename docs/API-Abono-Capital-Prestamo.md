# API - Abono a Capital de Préstamo

**Fecha:** 2026-03-31  
**Estado:** ✅ IMPLEMENTADO Y DISPONIBLE

## 📋 Resumen

El sistema **SÍ cuenta** con funcionalidad completa para aplicar abonos a capital de préstamos, permitiendo elegir entre dos opciones:
1. **Mantener el plazo y reducir el valor de la cuota**
2. **Reducir el plazo y mantener el valor de la cuota**

## 🔧 Componentes Implementados

### 1. Interfaz del Servicio
**Archivo:** `src/main/java/com/saa/ejb/crd/service/PrestamoService.java`

```java
/**
 * Aplica un abono a capital al préstamo y recalcula la tabla de amortización.
 * @param idPrestamo ID del préstamo
 * @param valorAbono Valor del abono a capital
 * @param opcionRecalculo 1=Mantener plazo/reducir cuota, 2=Reducir plazo/mantener cuota
 * @return Préstamo con la tabla de amortización recalculada
 * @throws Throwable Si ocurre algún error durante el proceso
 */
Prestamo aplicarAbonoCapital(Long idPrestamo, Double valorAbono, Integer opcionRecalculo) throws Throwable;
```

### 2. Implementación del Servicio
**Archivo:** `src/main/java/com/saa/ejb/crd/serviceImpl/PrestamoServiceImpl.java`  
**Línea:** ~824

**Funcionalidad implementada:**
- ✅ Valida que el valor del abono sea mayor a cero
- ✅ Valida que la opción de recálculo sea 1 o 2
- ✅ Obtiene el préstamo y su tabla de amortización
- ✅ Identifica la primera cuota pendiente
- ✅ Calcula el saldo actual de capital
- ✅ Valida que el abono no sea mayor al saldo
- ✅ Aplica el abono reduciendo el saldo de capital
- ✅ Recalcula la tabla de amortización según la opción elegida:
  - **Opción 1:** Mantiene el plazo, recalcula cuotas menores
  - **Opción 2:** Reduce el plazo, mantiene cuotas iguales
- ✅ Elimina las cuotas antiguas pendientes
- ✅ Guarda las nuevas cuotas recalculadas
- ✅ Actualiza los campos del préstamo

**Métodos auxiliares:**
- `recalcularMantenPlazoCuotaMenor()` - Mantiene plazo, reduce cuota
- `recalcularReducePlazoCuotaIgual()` - Reduce plazo, mantiene cuota

### 3. Endpoint REST
**Archivo:** `src/main/java/com/saa/ws/rest/crd/PrestamoRest.java`  
**Path Base:** `/prst`

## 🌐 Endpoint REST

### POST - Aplicar Abono a Capital

**URL:**
```
POST /prst/aplicarAbonoCapital/{id}/{valorAbono}/{opcionRecalculo}
```

**Parámetros de Ruta:**
- `id` (Long): Identificador del préstamo
- `valorAbono` (Double): Valor del abono a capital en dólares
- `opcionRecalculo` (Integer): Opción de recálculo
  - `1` = Mantener plazo y reducir valor de la cuota
  - `2` = Reducir plazo y mantener valor de la cuota

**Headers:**
```
Content-Type: application/json
Accept: application/json
```

**Respuesta Exitosa (200 OK):**
```json
{
  "codigo": 123,
  "idAsoprep": "12345",
  "montoSolicitado": 10000.00,
  "plazo": 36,
  "plazoOriginal": 48,
  "tasa": 12.5,
  "cuota": 285.50,
  "tipoAmortizacion": 1,
  "estadoPrestamo": 2,
  "fechaInicio": "2025-01-15T00:00:00",
  "totalInteres": 1500.00,
  "totalCapital": 8500.00,
  ...
}
```

**Respuesta de Error (400/500):**
```json
"Error al aplicar abono a capital: [mensaje de error]"
```

## 📝 Ejemplos de Uso

### Ejemplo 1: Reducir Valor de la Cuota (Mantener Plazo)

**Escenario:**
- Préstamo ID: 456
- Saldo actual: $5,000
- Abono: $1,000
- Opción: Mantener plazo (36 meses restantes)

**Request:**
```bash
curl -X POST "http://localhost:8080/saaBE/api/prst/aplicarAbonoCapital/456/1000.00/1" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json"
```

**Resultado:**
- ✅ Saldo nuevo: $4,000
- ✅ Plazo: 36 meses (sin cambio)
- ✅ Cuota mensual: REDUCIDA (menor que antes)
- ✅ Tabla de amortización recalculada con nuevas cuotas

### Ejemplo 2: Reducir Plazo (Mantener Cuota)

**Escenario:**
- Préstamo ID: 456
- Saldo actual: $5,000
- Abono: $1,000
- Opción: Reducir plazo

**Request:**
```bash
curl -X POST "http://localhost:8080/saaBE/api/prst/aplicarAbonoCapital/456/1000.00/2" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json"
```

**Resultado:**
- ✅ Saldo nuevo: $4,000
- ✅ Plazo: REDUCIDO (menos meses que antes)
- ✅ Cuota mensual: sin cambio (igual que antes)
- ✅ Tabla de amortización recalculada con menos cuotas

## ⚙️ Validaciones Implementadas

El sistema realiza las siguientes validaciones:

1. ✅ **Valor del abono > 0:** El abono debe ser mayor a cero
2. ✅ **Opción válida:** Solo acepta 1 o 2
3. ✅ **Préstamo existe:** Valida que el ID del préstamo exista
4. ✅ **Tiene tipo de amortización:** El préstamo debe tener tipo definido
5. ✅ **Tiene tabla de amortización:** Debe tener cuotas generadas
6. ✅ **Tiene cuotas pendientes:** Debe haber al menos una cuota pendiente
7. ✅ **Abono <= Saldo:** El abono no puede ser mayor al saldo de capital

## 🔄 Proceso de Recálculo

### Opción 1: Mantener Plazo, Reducir Cuota

1. Se toma el nuevo saldo de capital (saldo actual - abono)
2. Se mantiene el número de cuotas pendientes
3. Se recalcula cada cuota con el nuevo saldo:
   - **Sistema Francés:** Cuota fija menor con nuevo capital
   - **Sistema Alemán:** Capital fijo menor + intereses
4. Se eliminan las cuotas antiguas pendientes
5. Se guardan las nuevas cuotas recalculadas

### Opción 2: Reducir Plazo, Mantener Cuota

1. Se toma el nuevo saldo de capital (saldo actual - abono)
2. Se calcula cuántas cuotas se necesitan con el nuevo saldo manteniendo la cuota original
3. Se reduce el número de cuotas según el cálculo
4. Se recalcula la tabla con menos cuotas
5. Se eliminan las cuotas antiguas pendientes
6. Se guardan las nuevas cuotas recalculadas

## 📊 Impacto en el Préstamo

Después de aplicar un abono a capital, se actualizan los siguientes campos del préstamo:

- `totalCapital`: Total de capital de todas las cuotas
- `totalInteres`: Total de intereses de todas las cuotas
- `cuota`: Valor de la cuota (si cambió)
- `plazo`: Número de cuotas (si cambió)

## 🚨 Mensajes de Error

| Error | Causa | Solución |
|-------|-------|----------|
| "El valor del abono debe ser mayor a cero" | Abono <= 0 | Enviar un valor positivo |
| "Opción de recálculo inválida. Use 1 para mantener plazo o 2 para reducir plazo" | Opción != 1 y != 2 | Usar 1 o 2 |
| "Préstamo con ID X no encontrado" | ID no existe | Verificar el ID del préstamo |
| "El préstamo no tiene definido el tipo de amortización" | Sin tipo | Definir tipo antes de aplicar abono |
| "El préstamo no tiene tabla de amortización generada" | Sin cuotas | Generar tabla antes de aplicar abono |
| "No hay cuotas pendientes para aplicar el abono a capital" | Todas pagadas | No se puede aplicar abono |
| "El abono (X) no puede ser mayor al saldo de capital (Y)" | Abono > saldo | Reducir el monto del abono |

## 🔗 APIs Relacionadas

### Generar Tabla de Amortización
```
POST /prst/generarTablaAmortizacion/{id}/{tieneCuotaCero}
```
Genera la tabla de amortización inicial del préstamo.

### Cargar Tabla desde Excel
```
POST /prst/cargarTablaExcel/{id}
Content-Type: multipart/form-data
```
Carga una tabla de amortización personalizada desde un archivo Excel.

## 📖 Documentos Relacionados

- `Abono-Capital-API.md` - Documentación anterior sobre la API
- `Carga-Tabla-Amortizacion-Excel.md` - Carga de tablas desde Excel
- `ARQUITECTURA_SISTEMA.md` - Arquitectura general del sistema

## ✅ Estado de Implementación

| Componente | Estado | Observaciones |
|------------|--------|---------------|
| Interfaz de Servicio | ✅ Implementado | Método definido en `PrestamoService` |
| Implementación de Servicio | ✅ Implementado | Lógica completa en `PrestamoServiceImpl` |
| Endpoint REST | ✅ Implementado | Disponible en `/prst/aplicarAbonoCapital` |
| Validaciones | ✅ Implementado | Todas las validaciones necesarias |
| Recálculo Opción 1 | ✅ Implementado | Mantiene plazo, reduce cuota |
| Recálculo Opción 2 | ✅ Implementado | Reduce plazo, mantiene cuota |
| Manejo de Errores | ✅ Implementado | Excepciones y mensajes claros |
| Logs de Depuración | ✅ Implementado | Trazabilidad completa |

## 🎯 Conclusión

**El sistema cuenta con funcionalidad COMPLETA y LISTA PARA USAR** para aplicar abonos a capital de préstamos con las dos opciones estándar:

1. ✅ **Reducir cuota manteniendo plazo**
2. ✅ **Reducir plazo manteniendo cuota**

La implementación incluye todas las validaciones necesarias, manejo de errores robusto, y endpoint REST documentado y funcional.

No se requiere desarrollo adicional, la funcionalidad está completamente implementada y disponible.
