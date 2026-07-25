# Documentación: Carga de Tabla de Amortización desde Excel

## Endpoint

```
POST /api/prst/cargarTablaExcel/{idPrestamo}
```

## Parámetros

- **idPrestamo** (Path): ID del préstamo (Long)
- **fileData** (Form): Archivo Excel (.xls o .xlsx)

## Content-Type

```
multipart/form-data
```

## Formato del Archivo Excel

### Estructura esperada:

| Columna | Nombre | Tipo | Mapea a |
|---------|--------|------|---------|
| A (0) | NroCuota | Numérico | numeroCuota |
| B (1) | FECHA VENCE | Fecha (DD/MM/YYYY) | fechaVencimiento |
| C (2) | PAGO EXTRA | Numérico | saldoOtros |
| D (3) | SALDO DE CAPITAL | Numérico | saldoCapital |
| E (4) | PAGO DE CAPITAL | Numérico | capital |
| F (5) | VALOR DEL INTERÉS | Numérico | interes |
| G (6) | DESGRAVAMEN | Numérico | desgravamen |
| H (7) | SEGURO | Numérico | valorSeguroIncendio |
| I (8) | CUOTA A PAGAR | Numérico | total |
| J (9) | ESTADO | Numérico | estado |

### Notas:
- **Fila 1**: Encabezados (se omite en la lectura)
- **Fila 2 en adelante**: Datos de las cuotas
- **Formato de fecha**: DD/MM/YYYY (ejemplo: 31/07/2026)
- Las fechas también pueden estar en formato nativo de Excel

## Ejemplo de Excel

```
NroCuota | FECHA VENCE | PAGO EXTRA | SALDO DE CAPITAL | PAGO DE CAPITAL | VALOR DEL INTERÉS | DESGRAVAMEN | SEGURO | CUOTA A PAGAR | ESTADO
1        | 31/07/2026  | 0.00       | 9500.00          | 500.00          | 50.00             | 5.00        | 2.00   | 557.00        | 1
2        | 31/08/2026  | 0.00       | 9000.00          | 500.00          | 47.50             | 5.00        | 2.00   | 554.50        | 1
3        | 30/09/2026  | 0.00       | 8500.00          | 500.00          | 45.00             | 5.00        | 2.00   | 552.00        | 1
```

## Cálculos Automáticos

El sistema calcula automáticamente:

1. **cuota** = capital + interes
2. **saldoInicialCapital** = capital + saldoCapital + saldoOtros (si existe)
3. **totalCapital** = Suma de todos los valores de "PAGO DE CAPITAL"
4. **totalInteres** = Suma de todos los valores de "VALOR DEL INTERÉS"
5. **valorCuota** = Cuota de la primera fila con NroCuota > 0
6. **fechaFin** = Fecha de la última fila procesada

## Actualización del Préstamo

Después de cargar la tabla, se actualizan automáticamente estos campos en el préstamo:

- ✅ `valorCuota`
- ✅ `fechaFin`
- ✅ `totalCapital`
- ✅ `totalInteres`
- ✅ `tasaNominal` (usa la tasa del préstamo)
- ✅ `tasaEfectiva` (calculada)
- ✅ `totalPrestamo` (totalCapital + totalInteres)

## Campos Inicializados

Los siguientes campos se inicializan automáticamente:

### En cero:
- mora
- interesVencido
- saldoMora
- saldoInteresVencido
- abono
- capitalPagado
- interesPagado
- desgravamenDiferido
- valorDiferido

### Copiados del Excel:
- saldoInteres = interes (del Excel)
- saldoCapital = SALDO DE CAPITAL (del Excel)

## Ejemplo de uso desde Frontend

### JavaScript (Fetch API)

```javascript
async function cargarTablaExcel(idPrestamo, archivo) {
    const formData = new FormData();
    formData.append('fileData', archivo);
    
    try {
        const response = await fetch(`/api/prst/cargarTablaExcel/${idPrestamo}`, {
            method: 'POST',
            body: formData,
            headers: {
                // NO incluir Content-Type, el browser lo añade automáticamente
            }
        });
        
        if (!response.ok) {
            const error = await response.text();
            throw new Error(error);
        }
        
        const prestamo = await response.json();
        console.log('Tabla cargada exitosamente:', prestamo);
        return prestamo;
        
    } catch (error) {
        console.error('Error al cargar tabla:', error);
        throw error;
    }
}

// Uso:
const inputFile = document.getElementById('archivoExcel');
inputFile.addEventListener('change', async (e) => {
    const archivo = e.target.files[0];
    const idPrestamo = 123; // ID del préstamo
    
    if (archivo) {
        await cargarTablaExcel(idPrestamo, archivo);
    }
});
```

### Angular (HttpClient)

```typescript
cargarTablaExcel(idPrestamo: number, archivo: File): Observable<Prestamo> {
    const formData = new FormData();
    formData.append('fileData', archivo, archivo.name);
    
    return this.http.post<Prestamo>(
        `/api/prst/cargarTablaExcel/${idPrestamo}`,
        formData
    );
}
```

### cURL (para pruebas)

```bash
curl -X POST \
  http://localhost:8080/api/prst/cargarTablaExcel/123 \
  -H "Content-Type: multipart/form-data" \
  -F "fileData=@tabla_amortizacion.xlsx"
```

## Respuesta Exitosa

```json
{
    "codigo": 123,
    "montoSolicitado": 10000.00,
    "plazo": 12,
    "tasa": 12.0,
    "valorCuota": 550.00,
    "fechaInicio": "2026-06-20T00:00:00",
    "fechaFin": "2027-06-30T00:00:00",
    "totalCapital": 10000.00,
    "totalInteres": 600.00,
    "totalPrestamo": 10600.00,
    "tasaNominal": 12.0,
    "tasaEfectiva": 12.68,
    "estado": 1,
    ...
}
```

## Respuesta de Error

```json
"Error al procesar el archivo Excel: [mensaje de error]"
```

## Códigos de Estado HTTP

- **200 OK**: Tabla cargada exitosamente
- **400 BAD REQUEST**: No se recibió archivo o archivo inválido
- **500 INTERNAL SERVER ERROR**: Error al procesar el archivo

## Validaciones

- ✅ El préstamo debe existir (se valida por ID)
- ✅ El archivo debe contener al menos una fila de datos
- ✅ Las celdas vacías o nulas se tratan como 0
- ✅ Se omiten filas completamente vacías
- ✅ Las fechas pueden estar en formato DD/MM/YYYY o formato nativo de Excel

## Notas Importantes

1. **Sin validación de estado**: El proceso NO valida el estado del préstamo, permite cargar en cualquier estado.

2. **Sobrescritura**: Si el préstamo ya tiene una tabla de amortización, esta se SOBRESCRIBIRÁ con los nuevos datos del Excel.

3. **Primera hoja**: El proceso lee ÚNICAMENTE la primera hoja del archivo Excel.

4. **Fila de encabezados**: La fila 1 se considera encabezado y se omite. Los datos se leen desde la fila 2.

5. **Orden de columnas**: El orden de las columnas DEBE ser exactamente el especificado. No se soporta lectura por nombre de columna.

6. **Extensiones soportadas**: .xls (Excel 97-2003) y .xlsx (Excel 2007+)
