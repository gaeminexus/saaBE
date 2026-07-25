# ESTRUCTURA DEL ARCHIVO DE DESCUENTOS PETROCOMERCIAL

## DESCRIPCIÓN GENERAL

Este documento especifica la estructura del archivo TXT que se debe generar para enviar a Petrocomercial los descuentos de aportes y cuotas de préstamos de los partícipes de ASOPREP.

**Archivo de ejemplo:** `DESCUENTOS ASOPREP [MES] [AÑO].txt`

**Fecha de especificación:** 2026-04-09

---

## CARACTERÍSTICAS DEL ARCHIVO

- **Formato:** Texto plano (TXT)
- **Codificación:** ASCII / UTF-8
- **Tipo de registro:** Ancho fijo (Fixed-width)
- **Longitud de línea:** 55 caracteres
- **Separador de línea:** CRLF (Windows) o LF (Unix)
- **Un registro por línea:** Cada línea representa un descuento de un partícipe para un tipo de producto específico
- **Múltiples líneas por partícipe:** Un partícipe puede tener varias líneas si tiene descuentos de diferentes productos

---

## ESTRUCTURA DE CADA LÍNEA (55 caracteres)

| Posición | Longitud | Campo | Tipo | Descripción | Ejemplo |
|----------|----------|-------|------|-------------|---------|
| 1-5 | 5 | `codigoParticipe` | Numérico | Código único del partícipe, justificado a la derecha con espacios | ` 9689` |
| 6-9 | 4 | `codigoFijo` | Alfanumérico | Constante "JRNN" | `JRNN` |
| 10-17 | 8 | `rellenoCeros1` | Numérico | Relleno de ceros | `00000000` |
| 18-25 | 8 | `fechaProceso` | Fecha | Fecha del proceso en formato YYYYMMDD | `20250630` |
| 26-38 | 13 | `montoDescuento` | Numérico | **Monto a descontar multiplicado por 10,000** | `0000000450300` |
| 39 | 1 | `codigoUno` | Numérico | Constante "1" | `1` |
| 40-53 | 14 | `rellenoCeros2` | Numérico | Relleno de ceros | `00000000000000` |
| 54-55 | 2 | `tipoProducto` | Alfanumérico | Código del tipo de producto/descuento | `AH` |

---

## DETALLE DE CAMPOS CRÍTICOS

### 1. CÓDIGO PARTÍCIPE (Posiciones 1-5)

- **Formato:** Numérico, justificado a la derecha
- **Relleno:** Espacios a la izquierda
- **Ejemplos:**
  - Código 9689 → `" 9689"` (1 espacio + 4 dígitos)
  - Código 511 → `"  511"` (2 espacios + 3 dígitos)
  - Código 10993 → `"10993"` (5 dígitos, sin espacios)

```java
String codigoParticipe = String.format("%5d", codigoPartInt); // Justifica a derecha con espacios
```

### 2. FECHA PROCESO (Posiciones 18-25)

- **Formato:** YYYYMMDD (Año de 4 dígitos, mes de 2 dígitos, día de 2 dígitos)
- **Sin separadores**
- **Ejemplo:** 30 de junio de 2025 → `20250630`

```java
String fechaProceso = new SimpleDateFormat("yyyyMMdd").format(fechaDescuento);
```

### 3. MONTO DESCUENTO (Posiciones 26-38) - **CRÍTICO**

- **Formato:** Entero de 13 dígitos
- **Conversión:** Monto en dólares × 10,000
- **Justificación:** Derecha con ceros a la izquierda
- **Decimales implícitos:** Los últimos 4 dígitos representan los decimales

**Fórmula de conversión:**
```
Monto Real: $45.03
Monto en archivo: 45.03 × 10,000 = 450,300
Formato: 0000000450300 (13 dígitos)
```

**Ejemplos de conversión:**

| Monto Real | Cálculo | Formato en Archivo |
|------------|---------|-------------------|
| $45.03 | 45.03 × 10000 = 450300 | `0000000450300` |
| $16.71 | 16.71 × 10000 = 167100 | `0000000167100` |
| $157.63 | 157.63 × 10000 = 1576300 | `0000001576300` |
| $386.74 | 386.74 × 10000 = 3867400 | `0000003867400` |
| $1,133.99 | 1133.99 × 10000 = 11339900 | `0000011339900` |

**Código Java:**
```java
double montoReal = 45.03;
long montoEntero = Math.round(montoReal * 10000);
String montoFormateado = String.format("%013d", montoEntero);
```

**IMPORTANTE:** 
- NO usar punto decimal en el archivo
- NO redondear antes de multiplicar
- Usar `Math.round()` después de multiplicar para evitar problemas de precisión de punto flotante

### 4. TIPO PRODUCTO (Posiciones 54-55)

Códigos identificados y sus significados:

| Código | Descripción | Cantidad Registros | Observaciones |
|--------|-------------|-------------------|---------------|
| `AH` | **Aportes Voluntarios / Ahorro** | 2,097 | Aportes mensuales del partícipe |
| `HS` | **Hipoteca Secundaria** | 258 | Préstamo hipotecario secundario |
| `PE` | **Préstamo Emergente** | 933 | Préstamo de emergencia |
| `PH` | **Préstamo Hipotecario** | 346 | Préstamo hipotecario principal |
| `PQ` | **Préstamo Quirografario** | 47 | Préstamo quirografario |
| `PP` | **Préstamo Personal** | 4 | Préstamo personal |

**TOTAL DE REGISTROS EN EJEMPLO:** 3,685 líneas

---

## EJEMPLOS DE REGISTROS COMPLETOS

### Ejemplo 1: Partícipe 9689 con 4 productos

```
 9689JRNN00000000202506300000000450300100000000000000AH
 9689JRNN00000000202506300000000167100100000000000000HS
 9689JRNN00000000202506300000001576300100000000000000PE
 9689JRNN00000000202506300000003867400100000000000000PH
```

**Desglose línea AH:**
- Código: `" 9689"` (espacio + 9689)
- Fijo: `"JRNN"`
- Ceros: `"00000000"`
- Fecha: `"20250630"` (30-Jun-2025)
- Monto: `"0000000450300"` ($45.03 × 10000)
- Uno: `"1"`
- Ceros: `"00000000000000"`
- Producto: `"AH"`

### Ejemplo 2: Partícipe 511 con 2 productos

```
  511JRNN00000000202506300000000937500100000000000000AH
  511JRNN00000000202506300000002580400100000000000000PE
```

**Montos:**
- AH: $93.75 → 93.75 × 10000 = 937500 → `0000000937500`
- PE: $258.04 → 258.04 × 10000 = 2580400 → `0000002580400`

### Ejemplo 3: Partícipe 2158 con múltiples productos

```
 2158JRNN00000000202506300000001900000100000000000000AH
 2158JRNN00000000202506300000000367600100000000000000HS
 2158JRNN00000000202506300000011339900100000000000000PH
 2158JRNN00000000202506300000011166900100000000000000PQ
```

---

## REGLAS DE NEGOCIO

1. **Un partícipe puede tener múltiples líneas** (una por cada tipo de producto que tenga activo)

2. **No hay línea de encabezado** en el archivo

3. **No hay línea de totales** al final del archivo

4. **El orden de las líneas:**
   - Primero todos los registros de tipo AH
   - Luego todos los registros de tipo HS
   - Luego todos los registros de tipo PE
   - Luego todos los registros de tipo PH
   - Luego todos los registros de tipo PQ
   - Finalmente todos los registros de tipo PP

5. **Dentro de cada tipo de producto**, los registros están ordenados por código de partícipe (ascendente)

6. **Montos en cero:** NO se deben incluir líneas con monto 0

7. **Fecha del proceso:** Debe corresponder al último día del mes de descuento

8. **Validaciones obligatorias:**
   - Código de partícipe debe existir y estar activo
   - Monto debe ser mayor a 0
   - Tipo de producto debe ser válido (AH, HS, PE, PH, PQ, PP)
   - Longitud de línea debe ser exactamente 55 caracteres

---

## ARCHIVO DE RESPUESTA (CONSOLIDADO)

Petrocomercial devuelve un archivo con el formato:

```
APELLIDOS Y NOMBRES              PLAZO   SALDO ACTUAL   MESES %INTER    %SEGURO   TOT.PROPUES
INICIAL                PLAZO  ANUAL  DESGRAVAM   A DESCONTAR        CAPITAL     INTERESES   SEG.DESGRAV    T O T A L        CAPITAL    INTERESES  SEG.DESGRAV
```

Ejemplo:
```
9689 ABARCA GUERRA CESAR SALOMON         1           45,03  1  0,00  0,000000          45,03          45,03           0,00           0,00          45,03           0,00          0,00          0,00
```

Este archivo de respuesta debe ser procesado para:
- Validar que los montos descontados coincidan con los enviados
- Identificar partícipes con descuentos no realizados
- Generar reportes de inconsistencias

---

## GENERACIÓN DEL ARCHIVO

### Pasos a seguir:

1. **Obtener fecha de proceso** (último día del mes)

2. **Consultar aportes voluntarios (AH)**
   - Extraer de tabla de aportes mensuales
   - Filtrar por partícipes activos
   - Monto > 0

3. **Consultar cuotas de préstamos activos**
   - Por cada tipo: HS, PE, PH, PQ, PP
   - Solo préstamos en estado VIGENTE
   - Solo cuotas pendientes del mes

4. **Formatear cada registro** según la estructura

5. **Ordenar registros:**
   - Primero por tipo de producto (AH, HS, PE, PH, PQ, PP)
   - Luego por código de partícipe (ascendente)

6. **Generar archivo TXT** con el nombre: `DESCUENTOS ASOPREP [MES] [AÑO].txt`

7. **Validar archivo generado:**
   - Longitud de cada línea = 55 caracteres
   - Formato de montos correcto
   - Sin líneas duplicadas para el mismo partícipe-producto

---

## TABLAS DE BASE DE DATOS INVOLUCRADAS

### PENDIENTE DE DEFINIR:

- Tabla de partícipes
- Tabla de aportes mensuales
- Tabla de préstamos
- Tabla de cuotas de préstamos
- Tabla de tipos de préstamo

*(Completar después con el usuario)*

---

## CONSIDERACIONES TÉCNICAS

### Manejo de Precisión Decimal

```java
// CORRECTO - Usar BigDecimal para precisión
BigDecimal monto = new BigDecimal("45.03");
BigDecimal multiplicador = new BigDecimal("10000");
BigDecimal resultado = monto.multiply(multiplicador);
long montoEntero = resultado.longValue();
String montoFormateado = String.format("%013d", montoEntero);
```

### Validación de Longitud

```java
if (linea.length() != 55) {
    throw new ValidationException("Longitud de línea incorrecta: " + linea.length());
}
```

### Generación del Archivo

```java
try (BufferedWriter writer = new BufferedWriter(
        new OutputStreamWriter(
            new FileOutputStream(archivo), 
            StandardCharsets.UTF_8))) {
    
    for (DescuentoDTO descuento : descuentos) {
        String linea = generarLineaDescuento(descuento);
        writer.write(linea);
        writer.newLine();
    }
}
```

---

## EJEMPLO COMPLETO DE GENERACIÓN

```java
public String generarLineaDescuento(
    int codigoParticipe, 
    Date fechaProceso, 
    double monto, 
    String tipoProducto) {
    
    // 1. Código partícipe (5 caracteres, justificado derecha)
    String codigo = String.format("%5d", codigoParticipe);
    
    // 2. Código fijo
    String codigoFijo = "JRNN";
    
    // 3. Ceros relleno 1
    String ceros1 = "00000000";
    
    // 4. Fecha proceso (YYYYMMDD)
    String fecha = new SimpleDateFormat("yyyyMMdd").format(fechaProceso);
    
    // 5. Monto (13 dígitos, multiplicado por 10000)
    BigDecimal montoBD = new BigDecimal(String.valueOf(monto));
    BigDecimal multiplicador = new BigDecimal("10000");
    long montoEntero = montoBD.multiply(multiplicador).longValue();
    String montoStr = String.format("%013d", montoEntero);
    
    // 6. Código uno
    String uno = "1";
    
    // 7. Ceros relleno 2
    String ceros2 = "00000000000000";
    
    // 8. Tipo producto
    String tipo = tipoProducto;
    
    // Concatenar todo
    String linea = codigo + codigoFijo + ceros1 + fecha + 
                   montoStr + uno + ceros2 + tipo;
    
    // Validar longitud
    if (linea.length() != 55) {
        throw new RuntimeException("Error: longitud incorrecta " + linea.length());
    }
    
    return linea;
}
```

---

## CASOS DE PRUEBA

### Test 1: Monto con decimales
```
Input: codigo=9689, fecha=2025-06-30, monto=45.03, tipo=AH
Expected: " 9689JRNN00000000202506300000000450300100000000000000AH"
```

### Test 2: Código de 3 dígitos
```
Input: codigo=511, fecha=2025-06-30, monto=93.75, tipo=AH
Expected: "  511JRNN00000000202506300000000937500100000000000000AH"
```

### Test 3: Monto grande
```
Input: codigo=2158, fecha=2025-06-30, monto=1133.99, tipo=PH
Expected: " 2158JRNN00000000202506300000011339900100000000000000PH"
```

### Test 4: Monto pequeño
```
Input: codigo=10295, fecha=2025-06-30, monto=1.90, tipo=HS
Expected: "10295JRNN00000000202506300000000019000100000000000000HS"
```

---

## ERRORES COMUNES A EVITAR

1. ❌ **Multiplicar por 100 en lugar de 10000**
   - Incorrecto: $45.03 × 100 = 4503
   - Correcto: $45.03 × 10000 = 450300

2. ❌ **No justificar el código de partícipe**
   - Incorrecto: `"511  "` (justificado izquierda)
   - Correcto: `"  511"` (justificado derecha)

3. ❌ **Usar punto decimal en el monto**
   - Incorrecto: `"000000045.03"`
   - Correcto: `"0000000450300"`

4. ❌ **Longitud incorrecta**
   - Debe ser exactamente 55 caracteres por línea

5. ❌ **Incluir líneas con monto 0**
   - Solo incluir descuentos con monto > 0

6. ❌ **Fecha en formato incorrecto**
   - Incorrecto: `"30/06/2025"` o `"2025-06-30"`
   - Correcto: `"20250630"`

---

## PRÓXIMOS PASOS

1. ✅ Estructura del archivo documentada
2. ⏳ Definir tablas de base de datos
3. ⏳ Implementar clase DAO para extraer datos
4. ⏳ Implementar servicio de generación de archivo
5. ⏳ Implementar endpoint REST
6. ⏳ Crear pruebas unitarias
7. ⏳ Crear pruebas de integración
8. ⏳ Implementar procesamiento de archivo de respuesta

---

## NOTAS ADICIONALES

- El archivo debe generarse mensualmente, típicamente antes del cierre del mes
- Se recomienda generar el archivo 2-3 días antes de la fecha de descuento para dar tiempo a correcciones
- Mantener histórico de archivos generados (respaldo)
- Validar totales antes de enviar el archivo
- Coordinar con Petrocomercial para confirmar recepción del archivo

---

**Última actualización:** 2026-04-09
**Versión:** 1.0
**Autor:** Sistema ASOPREP
