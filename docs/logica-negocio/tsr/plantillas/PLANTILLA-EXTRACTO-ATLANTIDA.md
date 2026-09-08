# Plantilla de extracto bancario — Banco Atlántida

Pedido del usuario, textual: *"dame un formato para llenar valores y subir el extracto"*.
Archivo: [`PLANTILLA-EXTRACTO-ATLANTIDA.xlsx`](PLANTILLA-EXTRACTO-ATLANTIDA.xlsx), generado con Apache
POI el 2026-09-08 y verificado de punta a punta contra `AtlantidaStatementParser` real (parsea sin
ninguna advertencia). Este `.md` repite el mismo contenido en texto, para que quede buscable.

## Estructura de la hoja "Extracto Atlantida"

| Fila | Contenido |
|---|---|
| 1 | `A1="Saldo Inicial :  "`, `B1=<saldo inicial, NÚMERO>` (ej. `12327.46`) |
| 2 | Vacía, a propósito |
| 3 | Encabezado de columnas (ver tabla abajo) |
| 4+ | Una fila por transacción |

**No borre ni mueva la fila 1.** Si la quita, el sistema estima el saldo inicial a partir de la
primera transacción en vez de usar el valor declarado — funciona, pero es menos preciso. Deje la
fila 2 vacía, tal cual está.

**No cambie el orden ni el texto de las columnas de la fila 3 (encabezado).** El sistema identifica
cada columna por su POSICIÓN exacta, no solo por el texto — moverlas hace que el archivo se rechace
o, peor, que los datos se lean desalineados.

## Columnas (fila 3 en adelante)

| Columna | Encabezado | Obligatoria? | Qué es |
|---|---|---|---|
| A | Fecha Mov | **Sí** | Fecha de la transacción. Fecha real de Excel (recomendado, como la fila de ejemplo) o texto `dd/mm/aaaa` (ej. `31/08/2026`) — las dos formas funcionan. |
| B | Sucursal | No | Decorativa, no se usa para el cálculo. |
| C | Asiento | No | Decorativa, se guarda como referencia. |
| D | Transacción | No | Decorativa, se guarda como código de movimiento (ej. `CINT`). |
| E | Concepto | No (recomendada) | Se guarda como descripción — ayuda a identificar el movimiento al conciliar. |
| F | Debito | **Sí** | Valor debitado. Si no aplica, ponga `0` (no lo deje vacío). |
| G | Credito | **Sí** | Valor acreditado. Si no aplica, ponga `0` (no lo deje vacío). |
| H | Cheques | No | Decorativa, no se usa. |
| I | Saldos Disponible | **Sí** | Saldo del banco DESPUÉS de esa transacción — el sistema lo usa para verificar que sus propios cálculos (saldo inicial + créditos − débitos) coincidan con lo que declara el banco, fila por fila. |
| J | Contable | No | Decorativa, no se usa. |

**Regla mínima por fila:** fecha, y al menos uno de Débito/Crédito/Saldos Disponible con un valor —
una fila sin fecha o completamente vacía de esas tres se descarta en silencio, no da error.

## Hoja "INSTRUCCIONES"

El archivo trae una segunda hoja con esta misma tabla y las advertencias, para quien lo llene sin
tener este documento a mano.

## Sobre generalizar esto a los otros 10 bancos

**No implementado todavía — es solo la lectura que pidió el árbitro.** Los 11 parsers difieren en:
formato de fecha (nativa de Excel en 3, texto ISO/DMY/MDY en los otros 8, y ahora Atlántida acepta
las dos), separador de monto (europeo solo en Manabí, US en los otros 10), y sobre todo la POSICIÓN
de cada columna (no hay dos bancos con las columnas en el mismo orden). Una plantilla "genérica" que
sirva para los 11 tendría que ser, en la práctica, 11 plantillas distintas con una portada común —
lo mismo que hacer 11 plantillas individuales, solo que empaquetadas juntas. **Recomendación: una
plantilla por banco, generada igual que esta (mismo generador POI, parametrizado por banco), el día
que otro banco lo pida** — no construir las 10 restantes por anticipado sin un pedido real detrás,
ya que cualquier drift entre la plantilla y el parser real (si alguno cambia) se detecta recién
cuando alguien la usa.
