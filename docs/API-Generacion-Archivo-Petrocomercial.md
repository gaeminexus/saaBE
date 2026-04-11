# API - Generación Archivo Petrocomercial

## Descripción General

Sistema completo para generar archivos de descuentos mensuales para Petrocomercial. Procesa aportes personales (jubilación + cesantía) y cuotas de préstamos activos, generando un archivo TXT con el formato requerido.

## Fecha de Implementación
**9 de abril de 2026**

---

## 📋 MODELO DE DATOS

### Tabla GNAP - Generación Archivo Petrocomercial (Cabecera)
```sql
-- Almacena la información de cada generación mensual
GNAPCDGO   NUMBER(19,0)    -- Código único (PK)
GNAPMSPE   NUMBER(10,0)    -- Mes del periodo (1-12)
GNAPANPE   NUMBER(10,0)    -- Año del periodo
GNAPFCGN   TIMESTAMP       -- Fecha de generación
GNAPUSGN   VARCHAR2(50)    -- Usuario que generó
GNAPTRGN   NUMBER(19,0)    -- Total de registros
GNAPTMEN   NUMBER(19,2)    -- Total monto enviado
GNAPESTA   VARCHAR2(20)    -- Estado (GENERADO, ENVIADO, PROCESADO)
GNAPRTAA   VARCHAR2(500)   -- Ruta del archivo
GNAPNMAR   VARCHAR2(200)   -- Nombre del archivo
GNAPFCEN   TIMESTAMP       -- Fecha de envío
GNAPFCPR   TIMESTAMP       -- Fecha de procesamiento
GNAPOBSR   VARCHAR2(4000)  -- Observaciones
FLLLCDGO   NUMBER(19,0)    -- FK: Filial
```

### Tabla DTGA - Detalle Generación Archivo (Por Producto)
```sql
-- Agrupa los totales por tipo de producto
DTGACDGO   NUMBER(19,0)    -- Código único (PK)
GNAPCDGO   NUMBER(19,0)    -- FK: Generación
DTGACDPT   VARCHAR2(2)     -- Código producto Petro (AH, HS, PE, etc.)
DTGATRRG   NUMBER(19,0)    -- Total registros del producto
DTGATMTO   NUMBER(19,2)    -- Total monto del producto
DTGADSCP   VARCHAR2(200)   -- Descripción del producto
```

### Tabla PDTG - Partícipe Detalle Generación (Líneas Individuales)
```sql
-- Detalla cada línea del archivo por partícipe
PDTGCDGO   NUMBER(19,0)    -- Código único (PK)
DTGACDGO   NUMBER(19,0)    -- FK: Detalle generación
ENTDCDGO   NUMBER(19,0)    -- FK: Entidad (partícipe)
PRSTCDGO   NUMBER(19,0)    -- FK: Préstamo (si aplica)
PDTGRLPC   NUMBER(19,0)    -- Rol Petrocomercial
PDTGCDPT   VARCHAR2(2)     -- Código producto
PDTGMNEN   NUMBER(19,2)    -- Monto enviado
PDTGNMLN   NUMBER(19,0)    -- Número de línea en el archivo
PDTGOBSR   VARCHAR2(500)   -- Observaciones
```

---

## 🎯 LÓGICA DE NEGOCIO

### 1. APORTES PERSONALES (Producto: AH)

**Criterios de selección:**
- Tabla: `HSTR` (HistorialSueldo)
- Estado: `10` (ACTIVO para descuentos Petrocomercial)
- Se toma el registro más reciente por entidad
- Solo entidades con `rolPetroComercial` definido

**Cálculo del monto:**
```
Monto Aporte = HSTRMNAJ (montoJubilacion) + HSTRMNAC (montoCesantia)
```

**Ejemplo:**
```
Entidad: Juan Pérez
Jubilación: $150.00
Cesantía: $50.00
TOTAL A DESCONTAR: $200.00
```

### 2. CUOTAS DE PRÉSTAMOS

**Criterios de selección:**
- Tabla: `DTPR` (DetallePrestamo) JOIN `PRST` (Prestamo)
- Préstamos con estado activo (`estadoPrestamo` IN (1, 2))
- Cuotas que vencen en el mes/año del periodo
- Solo préstamos con `codigoPetro` definido en su producto
- Solo cuotas con `saldoCapital > 0`

**Obtención del código de producto:**
- Se obtiene de `PRDC.PRDCCDPT` (codigoPetro)
- Mediante la FK del préstamo al producto

**Monto a descontar:**
```
Monto = DTPRCTAA (cuota completa)
```

---

## 📁 FORMATO DEL ARCHIVO TXT

### Especificaciones
- **Nombre:** `PETRO_YYYYMM_YYYYMMDD_HHMMSS.txt`
- **Ubicación:** `C:/saa/archivos/petrocomercial/`
- **Codificación:** UTF-8
- **Separador:** Pipe (|)
- **Formato de línea:** `ROL|PRODUCTO|MONTO`

### Ejemplo de archivo generado:
```
12345|AH|200.00
12345|PE|350.00
67890|AH|180.00
67890|HS|420.00
```

### Códigos de Producto Petrocomercial:

Los códigos de productos se obtienen de la tabla **PRDC (Productos)** existente, campo **PRDCCDPT (codigoPetro)**:

- **AH**: Aporte Habitacional (Aportes personales - fijo en código)
- **HS**: Hipotecario de Salud
- **PE**: Préstamo de Emergencia
- **PH**: Préstamo Hipotecario
- **PQ**: Préstamo Quirografario
- **PP**: Préstamo Prendario

**Nota:** Para aportes personales, el sistema usa el código "AH" fijo. Para préstamos, obtiene el código del campo `PRDCCDPT` de la tabla `PRDC`.

---

## 🌐 REST API ENDPOINTS

### Base URL
```
http://localhost:8080/saaBE/api/petrocomercial
```

---

### 1. Generar Archivo de Descuentos

**Endpoint:** `POST /generar`

**Request Body:**
```json
{
  "mesPeriodo": 4,
  "anioPeriodo": 2026,
  "filialId": 1,
  "usuario": "admin"
}
```

**Response Success (200):**
```json
{
  "success": true,
  "message": "Archivo generado exitosamente",
  "data": {
    "codigo": 1,
    "mesPeriodo": 4,
    "anioPeriodo": 2026,
    "fechaGeneracion": "2026-04-09T10:30:00",
    "usuarioGeneracion": "admin",
    "totalRegistros": 85,
    "totalMontoEnviado": 42750.50,
    "estado": "GENERADO",
    "rutaArchivo": "C:/saa/archivos/petrocomercial/PETRO_202604_20260409_103000.txt",
    "nombreArchivo": "PETRO_202604_20260409_103000.txt",
    "filial": {
      "codigo": 1,
      "nombre": "Filial Principal"
    }
  }
}
```

**Response Error (400 - Periodo duplicado):**
```json
{
  "success": false,
  "message": "Ya existe una generación para el periodo 4/2026"
}
```

**Response Error (400 - Parámetros inválidos):**
```json
{
  "success": false,
  "message": "El mes debe estar entre 1 y 12"
}
```

**Ejemplo con cURL:**
```bash
curl -X POST http://localhost:8080/saaBE/api/petrocomercial/generar \
  -H "Content-Type: application/json" \
  -d '{
    "mesPeriodo": 4,
    "anioPeriodo": 2026,
    "filialId": 1,
    "usuario": "admin"
  }'
```

---

### 2. Listar Todas las Generaciones

**Endpoint:** `GET /getAll`

**Response (200):**
```json
[
  {
    "codigo": 2,
    "mesPeriodo": 4,
    "anioPeriodo": 2026,
    "fechaGeneracion": "2026-04-09T10:30:00",
    "totalRegistros": 85,
    "totalMontoEnviado": 42750.50,
    "estado": "GENERADO",
    "nombreArchivo": "PETRO_202604_20260409_103000.txt"
  },
  {
    "codigo": 1,
    "mesPeriodo": 3,
    "anioPeriodo": 2026,
    "fechaGeneracion": "2026-03-08T15:20:00",
    "totalRegistros": 78,
    "totalMontoEnviado": 39200.00,
    "estado": "PROCESADO",
    "nombreArchivo": "PETRO_202603_20260308_152000.txt"
  }
]
```

---

### 3. Obtener Generación por ID

**Endpoint:** `GET /getId/{id}`

**Response (200):**
```json
{
  "codigo": 1,
  "mesPeriodo": 4,
  "anioPeriodo": 2026,
  "fechaGeneracion": "2026-04-09T10:30:00",
  "usuarioGeneracion": "admin",
  "totalRegistros": 85,
  "totalMontoEnviado": 42750.50,
  "estado": "GENERADO",
  "rutaArchivo": "C:/saa/archivos/petrocomercial/PETRO_202604_20260409_103000.txt",
  "nombreArchivo": "PETRO_202604_20260409_103000.txt",
  "observaciones": null,
  "filial": {
    "codigo": 1,
    "nombre": "Filial Principal"
  }
}
```

**Response (404):**
```json
{
  "success": false,
  "message": "Generación con ID 999 no encontrada"
}
```

---

### 4. Obtener Líneas de una Generación

**Endpoint:** `GET /getLineas/{id}`

**Response (200):**
```json
[
  {
    "codigo": 1,
    "numeroLinea": 1,
    "rolPetrocomercial": 12345,
    "codigoProducto": "AH",
    "montoEnviado": 200.00,
    "observaciones": "Aporte personal (J:150.0 + C:50.0)",
    "entidad": {
      "codigo": 100,
      "razonSocial": "Juan Pérez Gómez",
      "numeroIdentificacion": "1234567890"
    },
    "prestamo": null
  },
  {
    "codigo": 2,
    "numeroLinea": 2,
    "rolPetrocomercial": 12345,
    "codigoProducto": "PE",
    "montoEnviado": 350.00,
    "observaciones": "Cuota #5 Préstamo 200",
    "entidad": {
      "codigo": 100,
      "razonSocial": "Juan Pérez Gómez",
      "numeroIdentificacion": "1234567890"
    },
    "prestamo": {
      "codigo": 200,
      "montoSolicitado": 5000.00
    }
  }
]
```

---

## 🔧 CONFIGURACIÓN DEL SISTEMA

### Variables de Configuración

En `GeneracionArchivoPetroService.java`:

```java
// Ruta donde se generan los archivos
private static final String RUTA_ARCHIVOS = "C:/saa/archivos/petrocomercial/";

// Código de producto para aportes personales
private static final String PRODUCTO_APORTE_PERSONAL = "AH";
```

### Permisos Requeridos
- Escritura en `C:/saa/archivos/petrocomercial/`
- Acceso a base de datos CRD schema
- Usuario con permisos de ejecución de transacciones

---

## 📊 FLUJO DE PROCESO

```
1. Frontend solicita generación
   ↓
2. Validar que no exista periodo duplicado
   ↓
3. Crear registro cabecera (GNAP)
   ↓
4. Obtener aportes personales (HSTR estado=10)
   ↓
5. Obtener cuotas de préstamos del mes
   ↓
6. Generar archivo TXT físico
   ↓
7. Actualizar totales en cabecera
   ↓
8. Crear detalles por producto (DTGA)
   ↓
9. Crear líneas individuales (PDTG)
   ↓
10. Retornar resultado al frontend
```

---

## ✅ VALIDACIONES IMPLEMENTADAS

1. **Periodo único por filial:** No permite generar dos veces para el mismo mes/año
2. **Mes válido:** Entre 1 y 12
3. **Rol Petrocomercial:** Solo procesa entidades con rol definido
4. **Código de Producto:** Solo procesa préstamos con código Petro definido
5. **Estado activo:** Solo historial con estado 10 y préstamos activos
6. **Saldo pendiente:** Solo cuotas con saldo de capital > 0

---

## 🧪 CASOS DE PRUEBA

### Test 1: Generación Exitosa
```bash
curl -X POST http://localhost:8080/saaBE/api/petrocomercial/generar \
  -H "Content-Type: application/json" \
  -d '{"mesPeriodo": 4, "anioPeriodo": 2026, "filialId": 1, "usuario": "test"}'
```

**Resultado esperado:** Status 200, archivo generado

### Test 2: Periodo Duplicado
```bash
# Ejecutar dos veces el mismo comando
```

**Resultado esperado:** Status 400, mensaje de error

### Test 3: Mes Inválido
```bash
curl -X POST http://localhost:8080/saaBE/api/petrocomercial/generar \
  -H "Content-Type: application/json" \
  -d '{"mesPeriodo": 13, "anioPeriodo": 2026, "filialId": 1, "usuario": "test"}'
```

**Resultado esperado:** Status 400, mensaje "El mes debe estar entre 1 y 12"

---

## 📝 NOTAS TÉCNICAS

### Transaccionalidad
- Toda la operación es transaccional (@Stateless)
- Si falla alguna parte, se hace rollback completo
- El archivo físico se genera antes de persistir en BD

### Performance
- Usa queries optimizadas con filtros en BD
- Procesa en memoria sin cargas masivas
- Índices recomendados en HSTRESTD, ENTDRLPC, PRDCCDPT

### Mantenimiento
- Logs detallados en System.out
- Mensajes de error descriptivos
- Trazabilidad completa de usuarios y fechas

---

## 👥 CONTACTO Y SOPORTE

**Desarrollado por:** GitHub Copilot  
**Fecha:** 9 de abril de 2026  
**Versión:** 1.0

Para reportar problemas o mejoras, contactar al equipo de desarrollo.
