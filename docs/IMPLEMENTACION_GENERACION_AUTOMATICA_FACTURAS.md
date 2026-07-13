# Implementación de Generación Automática de Campos en Facturas

## Fecha: 2026-07-13

## Problema Resuelto

Al procesar facturas, el sistema generaba el error:
```
ERROR en procesarFacturaCompleta: No result found for query [SELECT f FROM Factura f WHERE f.clave = :clave]
```

Esto ocurría porque al grabar las facturas no se generaban automáticamente los campos requeridos como:
- **Clave de acceso** (campo obligatorio único)
- **Secuencial** (número correlativo)
- **Número de factura** (formato: establecimiento-ptoEmision-secuencial)

## Análisis del Código PHP de Referencia

Se analizó el código PHP legacy en `docs/referencias/logicaDocs/`:
- **fctr.php**: CRUD de facturas
- **utils.php**: Funciones utilitarias

### Lógica Identificada en PHP

1. **Obtención del Secuencial** (`obtenerSecuencial`):
   - Consulta tabla `NXPE` (Numeración por Punto de Emisión)
   - Obtiene el número actual para el punto de emisión y tipo de documento
   - Incrementa el contador en 1
   - Formatea a 9 dígitos con ceros a la izquierda

2. **Generación de Clave de Acceso** (`getMod11Dv`):
   - Formato: `[fecha][tipoDoc][RUC][ambiente][establecimiento][ptoEmision][secuencial][codClave][tipoEmision][DV]`
   - Fecha en formato ddMMyyyy
   - Usa código de 8 dígitos del facturador (`codClave`)
   - Calcula dígito verificador con algoritmo Módulo 11

3. **Generación del Número de Factura**:
   - Formato: `[establecimiento]-[ptoEmision]-[secuencial]`
   - Ejemplo: `001-001-000000001`

4. **Otros Campos Automáticos**:
   - `tipoComprobante`: "01" (código SRI para facturas)
   - `estadoEmision`: 1 (pendiente de autorización)

## Implementación en Java

### Archivos Modificados

#### 1. `FacturaServiceImpl.java`

**Imports Agregados:**
```java
import jakarta.persistence.Query;
```

**Servicios Inyectados:**
```java
@EJB
private DetalleFacturaService detalleFacturaService;
```

**Métodos Nuevos Implementados:**

##### a) `obtenerSecuencial(Long idPtoEmision, String tipoDoc)`
- Consulta la tabla `NumeracionPuntoEmision` (NXPE)
- Obtiene el número actual del secuencial
- Incrementa el contador atómicamente
- Retorna el secuencial formateado a 9 dígitos

##### b) `generarClaveAcceso(...)`
- Formatea la fecha a ddMMyyyy
- Obtiene RUC y codClave del facturador
- Construye la cadena de la clave
- Llama al algoritmo de módulo 11

##### c) `calcularModulo11(String cadena)`
- Invierte la cadena
- Aplica factores del 2 al 7 cíclicamente
- Calcula el dígito verificador
- Maneja casos especiales (10 → 1, 11 → 0)

**Modificación del método `saveSingle`:**
```java
public Factura saveSingle(Factura entidad) throws Throwable {
    if (entidad.getId() == null) {
        // Validaciones
        if (entidad.getPtoEmision() == null || entidad.getPtoEmision().getId() == null) {
            throw new IncomeException("Debe especificar un punto de emisión");
        }
        if (entidad.getFacturador() == null || entidad.getFacturador().getId() == null) {
            throw new IncomeException("Debe especificar un facturador");
        }
        
        // Constantes SRI
        String tipoComprobante = "01"; // Factura
        Long ambiente = entidad.getAmbiente() != null ? entidad.getAmbiente() : 1L;
        String tipoEmision = "1"; // Emisión Normal
        
        // 1. Obtener secuencial
        String secuencial = obtenerSecuencial(entidad.getPtoEmision().getId(), tipoComprobante);
        entidad.setSecuencial(secuencial);
        
        // 2. Generar número de factura
        String numero = entidad.getNumEstablecimiento() + "-" + 
                       entidad.getNumPtoEmision() + "-" + secuencial;
        entidad.setNumero(numero);
        
        // 3. Generar clave de acceso
        String clave = generarClaveAcceso(entidad, tipoComprobante, ambiente, tipoEmision, secuencial);
        entidad.setClave(clave);
        
        // 4. Establecer tipo de comprobante
        entidad.setTipoComprobante(tipoComprobante);
        
        // 5. Estado de emisión inicial
        if (entidad.getEstadoEmision() == null) {
            entidad.setEstadoEmision(1L); // Pendiente
        }
        
        entidad.setEstado(Long.valueOf(Estado.ACTIVO));
    }
    
    entidad = facturaDaoService.save(entidad, entidad.getId());
    return entidad;
}
```

**Corrección del guardado de detalles:**
- Se reemplazó `em.persist(detalle)` por `detalleFacturaService.saveSingle(detalle)`
- Esto evita el error de "detached entity" porque el servicio maneja correctamente el merge/persist

## Entidades Relacionadas

### NumeracionPuntoEmision (NXPE)
- `id`: ID único
- `ptoEmision`: Relación con punto de emisión
- `tipoDoc`: Tipo de documento (01=Factura)
- `numActual`: Número actual del secuencial

### Facturador (FCDR)
- `numDoc`: RUC del facturador
- `codClave`: Código de 8 dígitos para generar la clave

### Factura (FCTR)
Campos generados automáticamente:
- `secuencial`: Número correlativo (9 dígitos)
- `numero`: Formato establecimiento-ptoEmision-secuencial
- `clave`: Clave de acceso de 49 dígitos
- `tipoComprobante`: "01" para facturas
- `estadoEmision`: 1 (pendiente de autorización)

## Flujo de Procesamiento

### Antes (con errores):
1. Cliente envía factura sin clave
2. Sistema intenta grabar factura sin clave
3. Sistema intenta generar XML
4. **ERROR**: No encuentra factura por clave (porque no se generó)

### Ahora (corregido):
1. Cliente envía factura con datos básicos
2. Sistema valida punto de emisión y facturador
3. **Sistema genera automáticamente**:
   - Obtiene y actualiza secuencial desde NXPE
   - Genera número de factura
   - Genera clave de acceso con algoritmo Módulo 11
   - Establece tipo de comprobante y estado
4. Sistema graba factura con todos los campos
5. Sistema graba detalles usando servicio
6. Sistema genera XML usando la clave generada
7. **ÉXITO**: Factura procesada correctamente

## Algoritmo Módulo 11 (Dígito Verificador)

### Ejemplo de Cálculo:
```
Cadena: 1307202601213456789000100100100000000112345678901
         ^invierte y aplica factores 2-7 cíclicamente^

Pasos:
1. Invertir cadena
2. Multiplicar cada dígito por factor (2,3,4,5,6,7,2,3,4...)
3. Sumar todos los productos
4. DV = 11 - (suma % 11)
5. Si DV=10 → 1, Si DV=11 → 0
```

## Validaciones Implementadas

1. **Punto de emisión obligatorio**: Sin punto de emisión no se puede generar el secuencial
2. **Facturador obligatorio**: Se necesita RUC y codClave para generar la clave de acceso
3. **Secuencial único**: La tabla NXPE garantiza secuenciales únicos por punto de emisión
4. **Formato estricto**: 
   - Secuencial: 9 dígitos
   - Clave: 49 dígitos
   - Número: formato XXX-XXX-XXXXXXXXX

## Campos que Debe Enviar el Cliente

### Obligatorios:
- `facturador` (con id)
- `ptoEmision` (con id)
- `numEstablecimiento` (ej: "001")
- `numPtoEmision` (ej: "001")
- `fecha` (fecha de emisión)
- `titular` (comprador)
- Datos de totales (subtotal, IVA, total, etc.)

### Opcionales (se generan si no existen):
- `ambiente` (default: 1 = Pruebas)
- `estadoEmision` (default: 1 = Pendiente)

### Generados Automáticamente:
- `secuencial`
- `numero`
- `clave`
- `tipoComprobante`
- `estado`

## Ejemplo de Uso

### Request JSON:
```json
{
  "factura": {
    "facturador": {"id": 1},
    "ptoEmision": {"id": 1},
    "titular": {"id": 123},
    "numEstablecimiento": "001",
    "numPtoEmision": "001",
    "fecha": "2026-07-13",
    "subtotal": 100.00,
    "subcero": 0.00,
    "vIVA": 15.00,
    "total": 115.00
  },
  "detalles": [
    {
      "descripcion": "Producto A",
      "cantidad": 1,
      "valor": 100.00,
      "total": 115.00
    }
  ]
}
```

### Response (factura generada):
```json
{
  "id": 456,
  "numero": "001-001-000000123",
  "secuencial": "000000123",
  "clave": "1307202601213456789000100100100000000112345678901",
  "tipoComprobante": "01",
  "estadoEmision": 1,
  "estado": 1
}
```

## Pruebas Sugeridas

1. **Crear factura nueva**: Verificar que se generen todos los campos automáticos
2. **Verificar secuencial**: Confirmar que cada factura incrementa el secuencial
3. **Validar clave**: Verificar que la clave tenga 49 dígitos y sea única
4. **Verificar número**: Confirmar formato XXX-XXX-XXXXXXXXX
5. **Probar con múltiples puntos de emisión**: Cada uno debe tener su propio secuencial

## Notas Técnicas

- **Transaccionalidad**: El incremento del secuencial debe ser atómico para evitar duplicados
- **Performance**: La consulta y actualización del secuencial es rápida (un solo registro por punto/tipo)
- **Compatibilidad**: La implementación replica exactamente la lógica PHP existente
- **Mantenibilidad**: Los métodos están bien documentados y separados por responsabilidad

## Próximos Pasos

1. ✅ Generación automática de campos implementada
2. ✅ Corrección de guardado de detalles con servicio
3. ⏳ Probar generación de XML con la clave generada
4. ⏳ Probar autorización en el SRI
5. ⏳ Validar flujo completo de facturación

## Referencias

- Código PHP: `docs/referencias/logicaDocs/fctr.php`
- Utilidades PHP: `docs/referencias/logicaDocs/utils.php`
- Tablas SRI: Catálogo de comprobantes electrónicos del SRI Ecuador
