# Plantilla de extracto bancario — Cooperativa Policía Nacional

Pedido del usuario, el mismo que motivó la de Atlántida, esta vez explícitamente en formato
**`.xls`** (no `.xlsx`) — `HSSFWorkbook`, no XSSF. El parser acepta las dos formas
(`WorkbookFactory` detecta el formato por firma de bytes), pero el entregable pedido es `.xls`.

Archivo: [`PLANTILLA-EXTRACTO-POLICIA-NACIONAL.xls`](PLANTILLA-EXTRACTO-POLICIA-NACIONAL.xls),
generado con Apache POI el 2026-09-08 y verificado de punta a punta contra
`PoliciaNacionalStatementParser` real:

```
saldoInicial=5000.0
detalles=2
  fecha=2026-08-15 debito=0.0 credito=200.0 saldo=5200.0 causa=DEP  desc=Deposito en efectivo
  fecha=2026-08-20 debito=150.0 credito=0.0 saldo=5050.0 causa=RET  desc=Retiro cajero
saldoFinal=5050.0
advertencias=[]
```

Sin ninguna advertencia de balance replay (5000 + 200 − 150 = 5050, cuadra fila por fila contra el
saldo contable declarado).

## Estructura de la hoja "Extracto Policia Nacional"

| Fila | Contenido |
|---|---|
| 1 | Decorativa (título del extracto), no la lee el parser |
| 2 | `A2="Saldo Anterior"`, `C2=<saldo inicial, NÚMERO>` — **la columna es la C, no la B** (a diferencia de la plantilla de Atlántida, donde el valor va en B). Es el primer error al copiar una plantilla de otro banco. |
| 3-4 | Vacías, tal como en el archivo real |
| 5 | Encabezado de columnas (ver tabla abajo) |
| 6+ | Una fila por transacción — la plantilla trae dos filas de ejemplo, una de crédito y una de débito |

**No borre ni mueva la fila 2.** Si el valor no está en la columna C, el sistema no lo va a
encontrar y va a estimar el saldo inicial a partir de la primera transacción en vez de usar el
valor declarado.

**No cambie el orden ni el texto de las columnas de la fila 5 (encabezado).** El sistema identifica
cada columna por su POSICIÓN exacta, no solo por el texto.

## 🔴 La columna E (`+/-`) es la que decide débito o crédito

Esta es la trampa de este banco, y por eso va primero:

- `-` (un guion) en la columna E = **DÉBITO**.
- Columna E **VACÍA** (o cualquier otro valor que no sea `-`) = **CRÉDITO**.

Si deja la columna E vacía por error en una fila que en realidad era un débito, esa fila entra
como crédito **sin ningún aviso**, y el extracto va a cuadrar mal. La plantilla trae dos filas de
ejemplo justamente para mostrar la diferencia: la del 15/08 es un crédito (columna E vacía) y la
del 20/08 es un débito (columna E = `-`).

## Columnas (fila 5 en adelante)

| Columna | Encabezado | Obligatoria? | Qué es |
|---|---|---|---|
| A | Fecha | **Sí** | Fecha de la transacción, como fecha real de Excel. |
| B | Lugar | No | Decorativa, no se usa para el cálculo. |
| C | Causa | No | Decorativa, se guarda como código de movimiento (ej. `DEP`, `RET`). |
| D | Descripción | No (recomendada) | Se guarda como descripción del movimiento. |
| E | +/- | **Sí** | `-` = débito, vacía = crédito. Ver la advertencia de arriba. |
| F | Efectivo | No | Decorativa, no se usa. |
| G | Cheques | No | Decorativa, no se usa. |
| H | Valor | **Sí** | Monto de la transacción, siempre positivo — el signo lo da la columna E. **Debe ser NUMÉRICO**, no texto, aunque el banco a veces lo muestre con "$". |
| I | Intereses | No | Decorativa, no se usa. |
| J | Saldo Contable | **Sí** | Saldo del banco DESPUÉS de esa transacción — el sistema lo usa para verificar que sus propios cálculos (saldo anterior + créditos − débitos) coincidan con lo que declara el banco, fila por fila. |
| K | Saldo Disponibles | No | Decorativa, no se usa. |

**Regla mínima por fila:** fecha y valor con un dato — una fila sin fecha se descarta en silencio,
no da error.

## Hoja "INSTRUCCIONES"

El archivo trae una segunda hoja con esta misma información (incluida la advertencia de la columna
E en primer plano, no como nota al pie), para quien lo llene sin tener este documento a mano.
