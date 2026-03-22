# API de Abono a Capital

## Descripción

Este servicio permite aplicar abonos extraordinarios al capital de un préstamo y recalcular automáticamente la tabla de amortización según dos opciones:

1. **Mantener el plazo y reducir la cuota** (opción 1)
2. **Reducir el plazo y mantener la cuota** (opción 2)

## Endpoint REST

### Aplicar Abono a Capital

**URL:** `POST /prst/aplicarAbonoCapital/{id}/{valorAbono}/{opcionRecalculo}`

**Parámetros de ruta:**
- `id` (Long): ID del préstamo
- `valorAbono` (Double): Valor del abono a capital
- `opcionRecalculo` (Integer): 
  - `1` = Mantener plazo, reducir cuota
  - `2` = Reducir plazo, mantener cuota

**Respuesta exitosa:**
```json
{
  "codigo": 123,
  "montoSolicitado": 10000.0,
  "plazo": 12,
  "valorCuota": 850.0,
  "totalCapital": 10000.0,
  "totalInteres": 450.0,
  ...
}
```

**Respuesta de error:**
```json
{
  "error": "Mensaje de error descriptivo"
}
```

## Explicación de las Opciones de Recálculo

### Opción 1: Mantener Plazo, Reducir Cuota

Cuando se aplica un abono a capital con esta opción:

1. El **número de cuotas se mantiene igual** al original
2. Se calcula una **nueva cuota fija más pequeña** usando la fórmula de amortización francesa
3. El nuevo saldo de capital es: `saldo_actual - valor_abono`

**Fórmula de la nueva cuota:**

```
nueva_cuota = nuevo_saldo * (i * (1 + i)^n) / ((1 + i)^n - 1)

Donde:
- nuevo_saldo = saldo actual - abono
- i = tasa mensual (tasa_anual / 100 / 12)
- n = número de cuotas restantes (se mantiene igual)
```

**Ejemplo:**
- Préstamo inicial: $10,000 a 12 meses, cuota mensual: $880
- Después de pagar 3 cuotas, saldo: $7,200
- Abono a capital: $1,000
- Nuevo saldo: $6,200
- Resultado: **9 cuotas restantes con nueva cuota de ~$710**

### Opción 2: Reducir Plazo, Mantener Cuota

Cuando se aplica un abono a capital con esta opción:

1. El **valor de la cuota se mantiene igual** al original
2. Se calcula un **nuevo número de cuotas menor** 
3. El nuevo saldo de capital es: `saldo_actual - valor_abono`

**Fórmula del nuevo plazo:**

```
nuevo_plazo = log(cuota / (cuota - nuevo_saldo * i)) / log(1 + i)

Donde:
- cuota = valor de la cuota actual (se mantiene)
- nuevo_saldo = saldo actual - abono
- i = tasa mensual (tasa_anual / 100 / 12)
```

**Ejemplo:**
- Préstamo inicial: $10,000 a 12 meses, cuota mensual: $880
- Después de pagar 3 cuotas, saldo: $7,200
- Abono a capital: $1,000
- Nuevo saldo: $6,200
- Resultado: **~7 cuotas restantes con la misma cuota de $880**

## Proceso Interno del Servicio

1. **Validaciones:**
   - Verificar que el préstamo existe
   - Validar que el valor del abono sea mayor a cero
   - Validar que la opción de recálculo sea 1 o 2
   - Verificar que el préstamo tenga tabla de amortización generada
   - Validar que el abono no sea mayor al saldo actual

2. **Identificación de cuotas:**
   - Buscar la última cuota pagada
   - Identificar la primera cuota pendiente
   - Obtener el saldo actual de capital

3. **Aplicación del abono:**
   - Calcular nuevo saldo: `saldo_actual - valor_abono`

4. **Recálculo según opción:**
   - **Opción 1:** Generar nuevas cuotas con el mismo plazo pero menor valor
   - **Opción 2:** Generar menos cuotas con el mismo valor

5. **Actualización de la base de datos:**
   - Eliminar las cuotas pendientes antiguas
   - Guardar las nuevas cuotas recalculadas
   - Actualizar los totales del préstamo (totalCapital, totalInteres, etc.)

## Características Importantes

### Manejo de Cuotas Pagadas
- Las cuotas que ya fueron pagadas **NO se modifican**
- Solo se recalculan las cuotas pendientes a partir del momento del abono

### Manejo de Fechas de Vencimiento
- **Opción 1:** Las fechas de vencimiento se mantienen iguales a las originales
- **Opción 2:** Se mantienen las fechas originales para las primeras N cuotas del nuevo plazo

### Precisión de Cálculos
- Todos los valores se redondean a 2 decimales
- En la última cuota se ajusta cualquier diferencia de redondeo para que el saldo final sea exactamente 0

### Validación de Saldo
El servicio valida que:
```
valor_abono <= saldo_actual_capital
```

Si el abono es mayor al saldo, se rechaza la operación.

## Ejemplo de Uso desde JavaScript

```javascript
// Aplicar abono manteniendo plazo
const response = await fetch(
  `/api/prst/aplicarAbonoCapital/123/1000.00/1`, 
  { method: 'POST' }
);
const prestamo = await response.json();

// Aplicar abono reduciendo plazo
const response2 = await fetch(
  `/api/prst/aplicarAbonoCapital/123/1000.00/2`, 
  { method: 'POST' }
);
const prestamo2 = await response2.json();
```

## Mensajes de Error Comunes

| Error | Causa | Solución |
|-------|-------|----------|
| "El valor del abono debe ser mayor a cero" | valorAbono <= 0 | Enviar un valor positivo |
| "Opción de recálculo inválida" | opcionRecalculo != 1 y != 2 | Usar 1 o 2 |
| "Préstamo con ID X no encontrado" | ID no existe | Verificar el ID del préstamo |
| "El préstamo no tiene tabla de amortización generada" | Sin tabla | Generar tabla primero |
| "No hay cuotas pendientes" | Todas pagadas | No se puede aplicar abono |
| "El abono no puede ser mayor al saldo de capital" | abono > saldo | Reducir el valor del abono |

## Fórmulas Matemáticas Detalladas

### Sistema de Amortización Francés (Cuota Fija)

**Cuota fija:**
```
C = P * (i * (1 + i)^n) / ((1 + i)^n - 1)

Donde:
C = Cuota periódica
P = Principal (saldo de capital)
i = Tasa de interés periódica
n = Número de períodos
```

**Interés de cada cuota:**
```
I_k = S_(k-1) * i

Donde:
I_k = Interés de la cuota k
S_(k-1) = Saldo al inicio del período k
```

**Amortización de capital en cada cuota:**
```
A_k = C - I_k

Donde:
A_k = Amortización de capital en la cuota k
```

**Saldo después de cada cuota:**
```
S_k = S_(k-1) - A_k
```

### Cálculo del Plazo dado la Cuota

```
n = ln(C / (C - P * i)) / ln(1 + i)

Donde:
n = Número de períodos
C = Cuota fija
P = Principal (saldo)
i = Tasa de interés periódica
ln = Logaritmo natural
```

## Consideraciones Técnicas

1. **Transaccionalidad:** El servicio es transaccional (EJB Stateless), si ocurre un error se hace rollback automático

2. **Concurrencia:** Se recomienda implementar bloqueos optimistas si múltiples usuarios pueden modificar el mismo préstamo

3. **Auditoría:** El sistema registra logs de todas las operaciones en la consola del servidor

4. **Performance:** El recálculo elimina y recrea los registros de DetallePrestamo, lo cual puede ser costoso para préstamos con muchas cuotas

## Próximas Mejoras Sugeridas

- [ ] Agregar registro de histórico de abonos a capital
- [ ] Permitir reversión de abonos aplicados
- [ ] Soporte para múltiples abonos acumulativos
- [ ] Recalcular con períodos de gracia
- [ ] Exportar nueva tabla de amortización a PDF/Excel
- [ ] Notificaciones al cliente sobre cambios en su préstamo
