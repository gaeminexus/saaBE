# RESUMEN DE CAMBIOS - Sistema de Novedades Múltiples y Aportes

**Fecha:** 2026-03-24  
**Estado:** ✅ COMPLETADO - Listo para ejecutar script SQL

---

## 🎯 CAMBIOS REALIZADOS

### 1. **Nueva Tabla NVPC (NovedadParticipeCarga)**
Se creó una tabla hija para almacenar **múltiples novedades** por partícipe, ya que un mismo partícipe puede tener varias novedades en una misma carga, incluso más de una por producto.

### 2. **Distinción entre APORTES (AH) y PRÉSTAMOS**
- El código de producto **"AH"** ahora se identifica como **APORTES**
- Los aportes **NO generan novedades financieras** (SIN_DESCUENTOS, DESCUENTOS_INCOMPLETOS)
- Los aportes se procesarán en tablas `Aporte` y `PagoAporte` (pendiente de implementar en FASE 2)

### 3. **Sistema de Novedades en Tabla Hija**
Todas las novedades de FASE 2 (9-16) ahora se registran en la tabla `NVPC` en lugar del campo `novedadesCarga` del partícipe.

---

## 📋 INSTRUCCIONES DE INSTALACIÓN

### PASO 1: Ejecutar el Script SQL

**Ubicación del script:**
```
C:\work\saaBE\v1\saaBE\docs\CREATE_TABLE_NVPC_NovedadParticipeCarga.sql
```

**Ejecutar en Oracle SQL Developer o SQL*Plus:**

```sql
-- 1. Crear la tabla NVPC
CREATE TABLE CRD.NVPC (
    NVPCCDGO NUMBER NOT NULL,
    PXCACDGO NUMBER NOT NULL,
    NVPCTPNV NUMBER NOT NULL,
    NVPCDSCR VARCHAR2(4000),
    NVPCCDPR NUMBER,
    NVPCCDPS NUMBER,
    NVPCCDCT NUMBER,
    NVPCMNES NUMBER,
    NVPCMNRC NUMBER,
    NVPCMNDF NUMBER,
    NVPCESTD NUMBER DEFAULT 1 NOT NULL,
    CONSTRAINT PK_NVPC PRIMARY KEY (NVPCCDGO),
    CONSTRAINT FK_NVPC_PXCA FOREIGN KEY (PXCACDGO) REFERENCES CRD.PXCA(PXCACDGO)
);

-- 2. Crear índices
CREATE INDEX IDX_NVPC_PXCA ON CRD.NVPC (PXCACDGO);
CREATE INDEX IDX_NVPC_TIPO ON CRD.NVPC (NVPCTPNV);

-- 3. Crear secuencia
CREATE SEQUENCE CRD.SEQ_NVPC START WITH 1 INCREMENT BY 1;
```

### PASO 2: Verificar la Instalación

Ejecutar esta consulta para verificar que la tabla fue creada correctamente:

```sql
SELECT table_name, column_name, data_type, nullable
FROM user_tab_columns
WHERE table_name = 'NVPC'
ORDER BY column_id;
```

---

## 🏗️ ESTRUCTURA DE LA TABLA NVPC

| Campo | Descripción | Tipo | Obligatorio |
|-------|-------------|------|-------------|
| NVPCCDGO | Código único (PK) | NUMBER | Sí |
| PXCACDGO | FK a ParticipeXCargaArchivo | NUMBER | Sí |
| NVPCTPNV | Tipo de novedad (9-16) | NUMBER | Sí |
| NVPCDSCR | Descripción de la novedad | VARCHAR2(4000) | No |
| NVPCCDPR | Código del producto relacionado | NUMBER | No |
| NVPCCDPS | Código del préstamo relacionado | NUMBER | No |
| NVPCCDCT | Código de la cuota relacionada | NUMBER | No |
| NVPCMNES | Monto esperado del sistema | NUMBER | No |
| NVPCMNRC | Monto recibido del archivo | NUMBER | No |
| NVPCMNDF | Diferencia entre montos | NUMBER | No |
| NVPCESTD | Estado (1=ACTIVO) | NUMBER | Sí |

---

## 📊 EJEMPLOS DE USO

### Ejemplo 1: Un partícipe con múltiples novedades

```
Partícipe: Juan Pérez (Código Petro: 12345)
Archivo: Marzo 2026

PXCA (ParticipeXCargaArchivo):
└─ Código: 1001
   └─ Nombre: Juan Pérez
   └─ Código Petro: 12345
   └─ novedadesCarga: 0 (OK)

NVPC (NovedadParticipeCarga):
├─ Código: 5001
│  └─ PXCACDGO: 1001
│  └─ NVPCTPNV: 12 (CUOTA_NO_ENCONTRADA)
│  └─ NVPCDSCR: "No se encontró cuota para producto PP"
│  └─ NVPCCDPR: 15 (Producto Préstamo Personal)
│
└─ Código: 5002
   └─ PXCACDGO: 1001
   └─ NVPCTPNV: 13 (MONTO_INCONSISTENTE)
   └─ NVPCDSCR: "Monto inconsistente para producto PH"
   └─ NVPCCDPR: 22 (Producto Préstamo Hipotecario)
   └─ NVPCMNES: 500.00
   └─ NVPCMNRC: 450.00
   └─ NVPCMNDF: 50.00
```

### Ejemplo 2: Producto AH (Aportes)

```
Partícipe: María González
Producto: AH (Aportes)

RESULTADO:
✅ NO se generan novedades financieras (SIN_DESCUENTOS, DESCUENTOS_INCOMPLETOS)
✅ Se validan las novedades de FASE 2 normalmente
✅ En FASE 2 se procesará en tablas Aporte y PagoAporte
```

---

## 🔧 ARCHIVOS JAVA CREADOS/MODIFICADOS

### Nuevos Archivos Creados:

1. **NovedadParticipeCarga.java** - Modelo de la entidad
2. **NovedadParticipeCargaDaoService.java** - Interface DAO
3. **NovedadParticipeCargaDaoServiceImpl.java** - Implementación DAO
4. **NovedadParticipeCargaService.java** - Interface Service
5. **NovedadParticipeCargaServiceImpl.java** - Implementación Service

### Archivos Modificados:

1. **CargaArchivoPetroServiceImpl.java**
   - Inyección de `NovedadParticipeCargaService`
   - Constante `CODIGO_PRODUCTO_APORTES = "AH"`
   - Método `registrarNovedad()` para guardar en tabla hija
   - Modificación de `validarNovedadesFase2()` para usar `registrarNovedad()`
   - Lógica para distinguir entre aportes y préstamos

2. **NombreEntidadesCredito.java**
   - Agregada constante `NOVEDAD_PARTICIPE_CARGA`

3. **ProductoDaoService.java**
   - Agregado método `selectAllByCodigoPetro()`

4. **ProductoDaoServiceImpl.java**
   - Implementado método `selectAllByCodigoPetro()`

---

## 🎮 FLUJO ACTUALIZADO

### FASE 1: Carga del Archivo

```
1. Leer archivo Petrocomercial
2. Por cada partícipe:
   ├─ Validaciones 1-7: Existencia de entidad
   ├─ Validación 8: VALORES_CERO
   │
   ├─ Identificar si es APORTE (código AH)
   │  ├─ Si es AH: NO validar novedades financieras
   │  └─ Si NO es AH: Validar novedades financieras
   │
   ├─ INSERT del partícipe en PXCA
   │
   └─ Validaciones FASE 2 (9-16):
      ├─ Buscar TODOS los productos con código Petro
      ├─ Buscar préstamos en todos los productos
      ├─ Buscar cuotas (coincidencia exacta o suma)
      └─ Si hay novedades → INSERT en NVPC (tabla hija)
```

### Ventajas del Nuevo Sistema:

✅ **Un partícipe puede tener múltiples novedades**
✅ **Cada novedad tiene información detallada** (descripción, montos, diferencias)
✅ **Trazabilidad completa** (producto, préstamo, cuota relacionados)
✅ **Aportes y préstamos se manejan diferente**
✅ **No se sobrescribe información** (cada novedad se guarda individualmente)

---

## 📝 CONSULTAS ÚTILES

### Ver todas las novedades de un partícipe:

```sql
SELECT 
    n.NVPCCDGO AS codigo_novedad,
    n.NVPCTPNV AS tipo_novedad,
    n.NVPCDSCR AS descripcion,
    n.NVPCMNES AS monto_esperado,
    n.NVPCMNRC AS monto_recibido,
    n.NVPCMNDF AS diferencia
FROM CRD.NVPC n
WHERE n.PXCACDGO = :codigo_participe
ORDER BY n.NVPCCDGO;
```

### Ver partícipes con múltiples novedades:

```sql
SELECT 
    p.PXCACDGO,
    p.PXCANMBR AS nombre,
    p.PXCACDPT AS codigo_petro,
    COUNT(n.NVPCCDGO) AS total_novedades
FROM CRD.PXCA p
INNER JOIN CRD.NVPC n ON p.PXCACDGO = n.PXCACDGO
GROUP BY p.PXCACDGO, p.PXCANMBR, p.PXCACDPT
HAVING COUNT(n.NVPCCDGO) > 1
ORDER BY total_novedades DESC;
```

### Ver novedades por tipo:

```sql
SELECT 
    n.NVPCTPNV AS tipo_novedad,
    COUNT(*) AS cantidad
FROM CRD.NVPC n
GROUP BY n.NVPCTPNV
ORDER BY n.NVPCTPNV;
```

---

## ✅ ESTADO DEL PROYECTO

- ✅ Modelo Java creado
- ✅ DAO implementado
- ✅ Service implementado
- ✅ Lógica de validación actualizada
- ✅ Distinción entre aportes y préstamos
- ✅ Sistema de múltiples novedades implementado
- ✅ Sin errores de compilación
- ⏳ **PENDIENTE: Ejecutar script SQL en base de datos**
- ⏳ **PENDIENTE: Implementar procesamiento de APORTES en FASE 2**

---

## 🚀 PRÓXIMOS PASOS

1. **EJECUTAR el script SQL** para crear la tabla NVPC
2. **PROBAR la carga de un archivo** Petrocomercial
3. **VERIFICAR** que las novedades se guardan en la tabla NVPC
4. **IMPLEMENTAR** el procesamiento de APORTES (código AH) en FASE 2:
   - Buscar/crear registros en tabla `Aporte`
   - Crear registros en tabla `PagoAporte`
   - Actualizar saldos de aportes

---

**¡El sistema está listo para usarse una vez ejecutado el script SQL!** 🎉
