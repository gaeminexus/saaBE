# API de Generación de Tablas de Amortización

## Descripción

Este servicio genera automáticamente las tablas de amortización para los préstamos en el módulo de créditos (CRD). Soporta dos sistemas de amortización:

- **Sistema Francés (Tipo 1)**: Cuota fija con capital creciente e interés decreciente
- **Sistema Alemán (Tipo 2)**: Capital fijo con cuota decreciente

## Endpoint

### Generar Tabla de Amortización

**URL:** `POST /api/prst/generarTablaAmortizacion/{id}`

**Descripción:** Genera la tabla de amortización completa para un préstamo existente, creando todos los registros de detalle en la tabla `DetallePrestamo`.

**Parámetros:**
- `id` (path parameter): ID del préstamo para el cual se generará la tabla

**Response:** Retorna el objeto `Prestamo` actualizado

## Datos Requeridos en el Préstamo

Para generar la tabla de amortización, el préstamo debe tener los siguientes datos configurados:

| Campo | Descripción | Obligatorio |
|-------|-------------|-------------|
| `tipoAmortizacion` | Tipo de sistema: 1=Francesa, 2=Alemana | Sí |
| `plazo` | Número de cuotas (meses) | Sí |
| `tasa` | Tasa de interés anual (%) | Sí |
| `montoSolicitado` | Monto del préstamo | Sí |
| `fechaInicio` | Fecha de inicio del préstamo | Sí |

## Sistema Francés (Tipo 1)

### Características
- Cuota mensual **fija** durante todo el plazo
- Capital pagado **creciente** en cada cuota
- Interés pagado **decreciente** en cada cuota

### Fórmula de la Cuota
```
Cuota = Capital × [i × (1 + i)^n] / [(1 + i)^n - 1]

Donde:
- Capital: Monto del préstamo
- i: Tasa mensual (tasa anual / 12 / 100)
- n: Número de cuotas
```

### Ejemplo
```
Capital: $10,000
Tasa: 12% anual (1% mensual)
Plazo: 12 meses

Cuota fija: $888.49
```

## Sistema Alemán (Tipo 2)

### Características
- Capital **fijo** en cada cuota
- Cuota total **decreciente** (porque el interés disminuye)
- Interés calculado sobre saldo pendiente

### Fórmulas
```
Capital por cuota = Capital total / Número de cuotas
Interés = Saldo pendiente × Tasa mensual
Cuota = Capital + Interés
```

### Ejemplo
```
Capital: $10,000
Tasa: 12% anual (1% mensual)
Plazo: 12 meses

Capital fijo: $833.33
Primera cuota: $933.33 (833.33 + 100)
Última cuota: $841.67 (833.33 + 8.34)
```

## Estructura del DetallePrestamo Generado

Cada cuota generada incluye:

```json
{
  "prestamo": { "codigo": 123 },
  "numeroCuota": 1,
  "fechaVencimiento": "2026-04-04T00:00:00",
  "capital": 850.50,
  "interes": 100.00,
  "cuota": 950.50,
  "saldoCapital": 9149.50,
  "saldo": 9149.50,
  "saldoInteres": 100.00,
  "mora": 0,
  "interesVencido": 0,
  "saldoMora": 0,
  "saldoInteresVencido": 0,
  "abono": 0,
  "capitalPagado": 0,
  "interesPagado": 0,
  "desgravamen": 0,
  "estado": 1
}
```

## Ejemplos de Uso

### Ejemplo 1: Generar tabla para préstamo con sistema francés

**Request:**
```http
POST /api/prst/generarTablaAmortizacion/123
Content-Type: application/json
```

**Response (200 OK):**
```json
{
  "codigo": 123,
  "tipoAmortizacion": 1,
  "plazo": 12,
  "tasa": 12.0,
  "montoSolicitado": 10000.0,
  "fechaInicio": "2026-03-04T00:00:00",
  ...
}
```

### Ejemplo 2: Usando cURL

```bash
curl -X POST http://localhost:8080/saaBE/api/prst/generarTablaAmortizacion/123 \
  -H "Content-Type: application/json"
```

### Ejemplo 3: Usando JavaScript/Fetch

```javascript
fetch('/api/prst/generarTablaAmortizacion/123', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json'
  }
})
.then(response => response.json())
.then(data => {
  console.log('Tabla de amortización generada:', data);
})
.catch(error => {
  console.error('Error:', error);
});
```

## Respuestas de Error

### Error 500: Préstamo no encontrado
```json
"Error al generar tabla de amortización: Préstamo con ID 123 no encontrado"
```

### Error 500: Falta tipo de amortización
```json
"Error al generar tabla de amortización: El préstamo no tiene definido el tipo de amortización"
```

### Error 500: Plazo inválido
```json
"Error al generar tabla de amortización: El préstamo no tiene definido un plazo válido"
```

### Error 500: Tasa inválida
```json
"Error al generar tabla de amortización: El préstamo no tiene definida una tasa válida"
```

### Error 500: Monto inválido
```json
"Error al generar tabla de amortización: El préstamo no tiene definido un monto válido"
```

### Error 500: Falta fecha de inicio
```json
"Error al generar tabla de amortización: El préstamo no tiene definida una fecha de inicio"
```

### Error 500: Tipo de amortización no válido
```json
"Error al generar tabla de amortización: Tipo de amortización no válido. Use 1 para Francesa o 2 para Alemana"
```

## Notas Importantes

1. **Redondeo**: Todos los valores monetarios se redondean a 2 decimales
2. **Última cuota**: Se ajusta automáticamente para cerrar el saldo en cero
3. **Fechas**: Las fechas de vencimiento se calculan sumando un mes a partir de la fecha de inicio
4. **Estado**: Todos los detalles se crean con estado ACTIVO (1)
5. **Sobrescritura**: Si ya existen detalles para el préstamo, estos NO se eliminan automáticamente. Considere eliminarlos manualmente antes de regenerar la tabla si es necesario.

## Integracion con Frontend

Para integrar este servicio en el frontend de Angular/React:

1. Asegúrese de que el préstamo tenga todos los datos requeridos
2. Llame al endpoint POST con el ID del préstamo
3. Muestre un mensaje de éxito al usuario
4. Recargue o actualice la vista de detalles del préstamo para mostrar la tabla generada

## Consultar Tabla Generada

Una vez generada la tabla, puede consultar los detalles usando:

```http
GET /api/dtpr/selectByCriteria
Content-Type: application/json

[
  {
    "campo": "prestamo.codigo",
    "operador": "=",
    "valor": "123"
  }
]
```

## Mantenimiento

- **Servicio**: `PrestamoService.generarTablaAmortizacion(Long idPrestamo)`
- **Implementación**: `PrestamoServiceImpl` en `com.saa.ejb.crd.serviceImpl`
- **REST Controller**: `PrestamoRest.generarTablaAmortizacion(Long id)`
- **Ubicación**: `com.saa.ws.rest.crd`
