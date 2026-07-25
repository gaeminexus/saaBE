# Guía de Manejo de FormData en saaBE

## Resumen Ejecutivo

Esta guía documenta el patrón establecido para manejar formularios multipart (FormData) en el backend de saaBE, específicamente implementado en `CargaArchivoPetroServiceImpl`. El enfoque resuelve problemas comunes de encoding, transacciones y gestión de archivos.

## Contexto del Problema

### Problemas Originales Identificados:
1. **Encoding**: Archivos .txt con caracteres especiales (ñ, acentos) no se almacenaban correctamente en Oracle AL32UTF8
2. **Transacciones**: Si fallaban las inserciones de BD, los archivos físicos se subían innecesariamente al servidor
3. **Dependencias**: Uso de librerías externas (`juniversalchardet`) que no están disponibles en WildFly por defecto
4. **Anotaciones**: Problemas con `@FormDataParam` de RESTEasy que no se resolvían correctamente

## Solución Implementada

### 1. Patrón de REST Endpoint

```java
@POST
@Path("/procesar-archivo-petro")
@Consumes(MediaType.MULTIPART_FORM_DATA)
@Produces(MediaType.APPLICATION_JSON)
public Response procesarArchivoPetro(
        @FormParam("archivo") InputStream archivoInputStream,
        @FormParam("archivoNombre") String archivoNombre,
        @FormParam("cargaArchivo") String cargaArchivoJson,
        @FormParam("detallesCargaArchivos") String detallesCargaArchivosJson,
        @FormParam("participesXCargaArchivo") String participesXCargaArchivoJson) {
```

**Características clave:**
- ✅ Usa `@FormParam` estándar de Jakarta EE en lugar de `@FormDataParam`
- ✅ Recibe el archivo como `InputStream` y el nombre por separado
- ✅ Los datos complejos se envían como JSON strings y se deserializan en el backend

### 2. Patrón de Procesamiento Backend

#### Secuencia de Operaciones (Crítico - mantener este orden):

```java
@Override
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public String procesarArchivoPetro(...) throws Throwable {
    
    // 1. DETECTAR ENCODING del archivo automáticamente
    String encodingDetectado = detectarEncodingArchivo(archivoInputStream, fileName);
    
    // 2. CONVERTIR DATOS a UTF-8 si es necesario
    if (!StandardCharsets.UTF_8.name().equalsIgnoreCase(encodingDetectado)) {
        convertirDatosAUTF8(cargaArchivo, detallesCargaArchivos, participesXCargaArchivo, encodingDetectado);
    }
    
    // 3. PRIMERO: Operaciones de Base de Datos (TRANSACCIONAL)
    CargaArchivo cargaArchivoGuardado = almacenaRegistros(cargaArchivo, detallesCargaArchivos, participesXCargaArchivo);
    
    // 4. AL FINAL: Subir archivo físico (NO TRANSACCIONAL)
    String rutaArchivo = cargarArchivo(archivoInputStream, fileName, cargaArchivo);
    
    return rutaArchivo;
}
```

**Beneficios del orden:**
- 🛡️ **Prevención de archivos huérfanos**: Si falla cualquier operación de BD, el archivo no se sube
- ⚡ **Rollback limpio**: Solo se hace rollback de BD, no hay archivos físicos que limpiar
- 🎯 **Consistencia**: El archivo solo se sube cuando todos los datos están guardados correctamente

### 3. Detección Automática de Encoding (100% Java Nativo)

```java
/**
 * Detecta automáticamente el encoding usando solo Java nativo
 * NO requiere librerías externas como juniversalchardet
 */
private String detectarEncodingNativo(byte[] buffer, int length) {
    String[] encodingsAProbar = {"UTF-8", "ISO-8859-1", "Windows-1252", "CP1252"};
    
    String mejorEncoding = StandardCharsets.UTF_8.name();
    int mejorScore = 0;
    
    for (String encoding : encodingsAProbar) {
        String contenido = new String(buffer, 0, length, Charset.forName(encoding));
        int score = calcularScoreEncoding(contenido, encoding);
        
        if (score > mejorScore) {
            mejorScore = score;
            mejorEncoding = encoding;
        }
    }
    
    return mejorEncoding;
}

/**
 * Sistema de scoring inteligente 0-100
 */
private int calcularScoreEncoding(String contenido, String encoding) {
    int score = 0;
    
    // +20: Sin caracteres de control extraños
    if (contenido.chars().filter(c -> c < 32 && c != 9 && c != 10 && c != 13).count() == 0) {
        score += 20;
    }
    
    // +30: Contiene ñ, acentos válidos en español
    if (contenido.matches(".*[ñáéíóúüÑÁÉÍÓÚÜ].*")) {
        score += 30;
    }
    
    // -50: Por cada secuencia mal interpretada (Ã±, Ã¡, etc.)
    String[] secuenciasMalas = {"Ã±", "Ã¡", "Ã©", "Ã­", "Ã³", "Ãº", "Ã¼", "Â "};
    for (String secuencia : secuenciasMalas) {
        if (contenido.contains(secuencia)) score -= 50;
    }
    
    // +20: >70% caracteres ASCII normales
    long caracteresASCII = contenido.chars().filter(c -> c >= 32 && c <= 126).count();
    if ((double) caracteresASCII / contenido.length() > 0.7) score += 20;
    
    // +10: Bonus para UTF-8
    if ("UTF-8".equals(encoding)) score += 10;
    
    return Math.max(0, Math.min(100, score));
}
```

### 4. Conversión Inteligente a UTF-8

```java
/**
 * Solo convierte si el archivo NO está en UTF-8
 * Procesa únicamente los campos de texto que existen en las entidades
 */
private void convertirDatosAUTF8(CargaArchivo cargaArchivo,
                               List<DetalleCargaArchivo> detallesCargaArchivos,
                               List<ParticipeXCargaArchivo> participesXCargaArchivo,
                               String encodingOrigen) throws Exception {
    
    Charset charsetOrigen = Charset.forName(encodingOrigen);
    Charset charsetDestino = StandardCharsets.UTF_8;
    
    // CargaArchivo: Solo campo 'nombre'
    if (cargaArchivo.getNombre() != null) {
        cargaArchivo.setNombre(convertirTextoAUTF8(cargaArchivo.getNombre(), charsetOrigen, charsetDestino));
    }
    
    // DetalleCargaArchivo: Solo campos que realmente existen
    for (DetalleCargaArchivo detalle : detallesCargaArchivos) {
        if (detalle.getCodigoPetroProducto() != null) {
            detalle.setCodigoPetroProducto(convertirTextoAUTF8(detalle.getCodigoPetroProducto(), charsetOrigen, charsetDestino));
        }
        if (detalle.getNombreProductoPetro() != null) {
            detalle.setNombreProductoPetro(convertirTextoAUTF8(detalle.getNombreProductoPetro(), charsetOrigen, charsetDestino));
        }
    }
}

/**
 * Convierte texto con validación y normalización Unicode
 */
private String convertirTextoAUTF8(String textoOriginal, Charset charsetOrigen, Charset charsetDestino) {
    if (textoOriginal == null || textoOriginal.trim().isEmpty()) return textoOriginal;
    
    try {
        // Paso 1: Convertir usando el charset origen
        byte[] bytesOrigen = textoOriginal.getBytes(charsetOrigen);
        
        // Paso 2: Crear string con charset destino
        String textoConvertido = new String(bytesOrigen, charsetDestino);
        
        // Paso 3: Normalizar caracteres Unicode
        String textoNormalizado = Normalizer.normalize(textoConvertido, Normalizer.Form.NFC);
        
        // Paso 4: Validar conversión
        return validarConversionUTF8(textoNormalizado) ? textoNormalizado : textoOriginal;
        
    } catch (Exception e) {
        return textoOriginal; // Fallback seguro
    }
}
```

### 5. Gestión Correcta de IDs en Transacciones

```java
/**
 * CRÍTICO: Limpiar códigos antes de insert para evitar errores de clave primaria
 */
private CargaArchivo almacenaRegistros(CargaArchivo cargaArchivo,
                                    List<DetalleCargaArchivo> detallesCargaArchivos,
                                    List<ParticipeXCargaArchivo> participesXCargaArchivo) throws Throwable {
    
    CargaArchivo cargaArchivoGuardado = almacenarCargaArchivo(cargaArchivo);
    
    for (DetalleCargaArchivo detalle : detallesCargaArchivos) {
        Long codigoDetalleOriginal = detalle.getCodigo(); // Guardar para filtrar partícipes
        
        detalle.setCodigo(null); // ← CRÍTICO: Limpiar para que JPA genere nuevo ID
        detalle.setCargaArchivo(cargaArchivoGuardado);
        detalle = detalleCargaArchivoService.saveSingle(detalle);
        
        for (ParticipeXCargaArchivo participe : filtrarPorCodigoDetalle(participesXCargaArchivo, codigoDetalleOriginal)) {
            participe.setCodigo(null); // ← CRÍTICO: También limpiar código del partícipe
            participe.setDetalleCargaArchivo(detalle);
            participe = participeXCargaArchivoService.saveSingle(participe);
        }
    }
    return cargaArchivoGuardado;
}
```

## Patrones de Frontend (Cliente)

### Estructura FormData Recomendada:

```javascript
const formData = new FormData();

// Archivo
formData.append('archivo', file);
formData.append('archivoNombre', file.name);

// Datos complejos como JSON strings
formData.append('cargaArchivo', JSON.stringify({
    nombre: "Archivo Petro Ejemplo",
    anioAfectacion: 2024,
    mesAfectacion: 11,
    filial: { codigo: 1, nombre: "Filial Principal" },
    usuarioCarga: { codigo: 123 }
}));

formData.append('detallesCargaArchivos', JSON.stringify([
    {
        codigo: 1, // ID temporal para relacionar con partícipes
        codigoPetroProducto: "PROD001",
        nombreProductoPetro: "Producto con ñ y acentos"
    }
]));

formData.append('participesXCargaArchivo', JSON.stringify([
    {
        codigo: null,
        detalleCargaArchivo: { codigo: 1 }, // Relación con detalle temporal
        nombreParticipe: "José María Peña"
    }
]));
```

## Verificaciones de Calidad

### Testing de Encoding:
```java
// Test con caracteres problemáticos
String testData = "José María Peña, niño, corazón, Ñuñoa";
// Debe almacenarse correctamente sin convertirse a: JosÃ© MarÃ­a PeÃ±a
```

### Logs Esperados:
```
Detectando encoding del archivo con métodos nativos de Java: archivo.txt
Encoding UTF-8 - Score: 75 - Muestra: José María Peña...
Encoding ISO-8859-1 - Score: 45 - Muestra: JosÃ© MarÃ­a...
Mejor encoding encontrado: UTF-8 (Score: 75)
El archivo ya está en UTF-8, no se requiere conversión
CargaArchivo guardado con código: 123
Archivo cargado en: /path/to/aportes/2024/11/archivo.txt
```

## Consideraciones Técnicas

### Stack Tecnológico:
- **Backend**: Jakarta EE 10, WildFly 38, Oracle 23ai (AL32UTF8)
- **Dependencias**: Solo librerías estándar de Java (sin `juniversalchardet`)
- **Transacciones**: JTA con rollback automático

### Performance:
- **Detección encoding**: Lee solo 4KB del archivo (eficiente)
- **Conversión**: Solo si es necesario (no UTF-8 → UTF-8)
- **Memoria**: Procesa streams sin cargar archivo completo en memoria

### Seguridad:
- **Validación**: Charset válido antes de conversión
- **Fallback**: Siempre devuelve un encoding válido
- **Sanitización**: Normalización Unicode contra ataques

## Aplicabilidad Futura

### Usar este patrón cuando:
- ✅ Necesites subir archivos con datos relacionados
- ✅ Los archivos puedan tener encoding variable
- ✅ Requieras consistencia transaccional entre BD y archivos
- ✅ Trabajes con caracteres especiales (ñ, acentos)

### Adaptaciones necesarias:
1. **Campos específicos**: Ajustar `convertirDatosAUTF8()` para los campos de texto de tus entidades
2. **Rutas de archivo**: Modificar `cargarArchivo()` para tu estructura de carpetas
3. **Validaciones**: Agregar validaciones específicas de tu dominio

## Troubleshooting

### Problema: ñ aparecen como Ã±
**Solución**: El encoding se detectó mal. Verificar que el archivo tenga suficiente texto para scoring.

### Problema: Error de clave primaria
**Solución**: Verificar que se llame `setCodigo(null)` antes de `saveSingle()`.

### Problema: Archivo se sube pero BD falla
**Solución**: Verificar orden de operaciones (BD primero, archivo después).

---

**Fecha de documentación**: 2024-11-25  
**Versión**: 1.0  
**Implementado en**: `CargaArchivoPetroServiceImpl.java`  
**Autor**: GitHub Copilot & Equipo de Desarrollo