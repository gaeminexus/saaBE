# Plantilla de extracto bancario — Cooperativa JEP

Pedido del usuario, mismo generador parametrizado que Atlántida y Policía Nacional, en formato
**`.xls`** (`HSSFWorkbook`). Antes de generarla se releyó `JepStatementParser` completo (no solo el
pedido) — ver la corrección de layout más abajo, es importante.

Archivo: [`PLANTILLA-EXTRACTO-JEP.xls`](PLANTILLA-EXTRACTO-JEP.xls), generado con Apache POI el
2026-09-08 y verificado de punta a punta contra `JepStatementParser` real:

```
saldoInicial=1000.0
detalles=2
  fecha=2026-08-05 debito=200.0 credito=0.0 saldo=800.0 desc=Compra en establecimiento
  fecha=2026-08-10 debito=0.0 credito=350.0 saldo=1150.0 desc=Deposito
saldoFinal=1150.0
advertencias=[]
```

Sin ninguna advertencia de balance replay (1000 − 200 + 350 = 1150, cuadra fila por fila). Dos
detalles, no cuatro: las filas "Saldo inicial" y "Saldo Final" no cuentan como transacción.

## 🔴 Corrección sobre el pedido original: el encabezado va en la fila 2, no en la fila 1

El pedido decía "Fila 1: encabezados. Fila 2 en adelante: datos". **Eso no es lo que hace el
parser.** `JepStatementParser.getPrimeraFilaDatos()` devuelve `2` (índice 0-based de la primera
fila de datos), lo que ubica el encabezado esperado en el índice `1` — **fila 2 de Excel
(1-indexada)** — y los datos desde la **fila 3**. El propio javadoc de la clase lo dice
explícitamente: *"encabezado en fila 2 (1-indexado) = fila 1 (0-indexado)"*, verificado contra un
archivo real ("COOP. JEP.xlsx"). La fila 1 queda libre/decorativa (en la plantilla lleva el nombre
de la cooperativa, pero el parser no la lee).

Esto importa porque el buscador de encabezado (`buscarFilaEncabezado`) tolera un desplazamiento
pequeño (±1/±2 filas) antes de rendirse, así que un archivo con el encabezado en la fila 1 en vez
de la 2 **puede** llegar a funcionar igual por esa tolerancia — pero la plantilla existe para
mostrar el formato *real* del banco, no para probar los límites de esa tolerancia. Se generó
respetando la posición exacta confirmada en el código.

## Estructura de la hoja "Extracto JEP"

| Fila | Contenido |
|---|---|
| 1 | Decorativa (nombre de la cooperativa), no la lee el parser |
| 2 | Encabezado de columnas (ver tabla abajo) — posición exacta, no solo texto |
| 3 | `A3=<fecha del período, FECHA>`, `B3="Saldo inicial"`, `F3=<saldo inicial, NÚMERO>` |
| 4-5 | Movimientos de ejemplo — uno de débito, uno de crédito, saldos encadenados |
| 6 | `B6="Saldo Final"` — el parser la descarta entera, ni mira su columna F |

**No cambie el orden ni el texto de la fila 2 (encabezado).** El sistema identifica cada columna
por su posición exacta, no solo por el texto.

**No deje filas vacías de más entre los movimientos.** El parser ya descarta solo, sin avisar, las
celdas de fecha en blanco al final del archivo (formato residual de Excel más allá del último dato
real) — pero eso es para *después* de la última fila real, no para huecos en medio de la tabla.

## 🔴 El texto exacto de las filas "Saldo inicial" y "Saldo Final" es lo que hay que cuidar

Esta es la trampa propia de este banco, y por eso va primero: JEP no trae el saldo inicial en una
celda aparte del encabezado (como Policía Nacional) ni lo deja implícito (como la mayoría de los
bancos) — lo mete como **una fila más dentro de la misma tabla de movimientos**, junto con una fila
de "Saldo Final" al cierre. El sistema las reconoce por el **texto exacto** de la columna B
(`trim().toLowerCase()`, así que mayúscula/minúscula no importa, pero la redacción sí):

- `Saldo inicial` (columna B) → el sistema toma el valor de la columna F de **esa misma fila** como
  saldo inicial del período. Si escribe otra cosa (ej. "Saldo Inicial del período" o lo traduce),
  el sistema **no lo reconoce**, no da ningún error, y en vez de eso **deriva** el saldo inicial de
  la primera transacción real — puede no coincidir con lo que el banco declaró.
- `Saldo Final` (columna B) → el sistema **descarta la fila entera**, ni siquiera mira su columna
  F. El saldo final real queda dado por la última transacción real ya procesada. La JEP manda esta
  fila con datos que no cierran a propósito (crédito duplicado de la última transacción, saldo en
  0.00) — la plantilla replica ese patrón real para que se reconozca.

⚠️ **Y la trampa dentro de la trampa:** el parser descarta la fila entera si la columna A (Fecha)
viene vacía, **antes** de mirar el texto de la columna B. Si la fila "Saldo inicial" se deja sin
fecha (parecería razonable, no es una transacción), el sistema la descarta sin leer su texto ni su
saldo — el valor declarado se pierde en silencio y se vuelve a caer en la derivación automática.
Por eso la plantilla lleva una fecha real (la del inicio del período) también en las filas de saldo
inicial y final.

## Columnas (fila 2 = encabezado, fila 3 en adelante = datos)

| Columna | Encabezado | Obligatoria? | Qué es |
|---|---|---|---|
| A | Fecha | **Sí** | Fecha de la transacción (o del período, en las filas de saldo), fecha real de Excel. Sin fecha, la fila entera se descarta sin mirar el resto. |
| B | Descripción/Referencia | **Sí** para "Saldo inicial"/"Saldo Final" (texto exacto); recomendada en el resto | Descripción del movimiento, o el texto que activa el reconocimiento de saldo inicial/final. |
| C | Oficina Transacción | No | Decorativa, no se usa. |
| D | Débito(s) | **Sí** (una de D o E) | Monto de débito, numérico. Vacía si la fila es un crédito. |
| E | Crédito(s) | **Sí** (una de D o E) | Monto de crédito, numérico. Vacía si la fila es un débito. |
| F | Saldos | **Sí** | Saldo después de esa fila (o el saldo inicial declarado, en la fila "Saldo inicial"). El sistema lo usa para verificar sus propios cálculos fila por fila. |

## Hoja "INSTRUCCIONES"

El archivo trae una segunda hoja con esta misma información — el asunto del texto exacto de
"Saldo inicial"/"Saldo Final" y la trampa de la fecha vacía van en primer plano, no como nota al
pie — para quien lo llene sin tener este documento a mano.
