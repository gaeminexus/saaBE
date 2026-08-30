# LIMPIEZA — Mora residual en préstamos de plazo vencido

**Fecha: 2026-08-27.** Documento de corrección de datos. Complementa
`PROCESO-DIARIO-INTERES-MORA.md` §11 (historial del defecto) y
`ESTADO-TRABAJO-EN-CURSO.md` §3 (Frente C). Script: `sql/77_LIMPIEZA_MORA_PLAZO_VENCIDO.sql`.

**No se ejecuta nada de este documento ni del script.** El usuario los corre, después de
restituir `PRSTIDST = 8` desde su respaldo y de confirmar que el fix del código (universo del
lote + guarda por préstamo, ver `PROCESO-DIARIO-INTERES-MORA.md` §11) ya está desplegado.

---

## 1. Por qué hace falta esto

Entre el 2026-08-14 y el 2026-08-24 (y en cada corrida de las 02:00 durante esos diez días,
más lo que corrió después hasta el despliegue del fix), el proceso diario de mora reclasificó
**todos** los préstamos en `8 DE_PLAZO_VENCIDO` a `11 EN_MORA` y les calculó y persistió mora en
sus cuotas — algo que, por definición, un préstamo de plazo vencido nunca debió tener.

El fix (ya entregado, verificado y desplegado) cierra la causa: el universo del lote excluye el
8, y una guarda en `calcularMoraPrestamo` corta el cálculo antes de tocar nada si el préstamo
está en 8, incluso desde el endpoint manual. **Pero no limpia lo que ya se escribió.**

El usuario restituye `PRSTIDST` a 8 desde su propio respaldo (fuera del alcance de este
documento). Una vez restituido, el proceso diario **ya no vuelve a tocar esas cuotas** — la mora
que quedó escrita se congela ahí, y sigue siendo cobrada:

- `GeneracionArchivoPetroServiceImpl.calcularSaldoCuota` la suma al archivo de descuentos que se
  envía a Petro.
- La prelación de seis componentes del motor de pagos (`MotorPagoPrestamoServiceImpl`) la cobra
  en cualquier pago manual sobre esas cuotas.

No es un dato feo en una pantalla: es plata que se le sigue cobrando a un partícipe por una mora
que el sistema nunca debió calcularle.

---

## 2. Universo — se deduce del dato, no de una lista externa

Las consultas de `ESTADO-TRABAJO-EN-CURSO.md` §3 dependían de una "lista de respaldo" que el
usuario tiene aparte, para restituir `PRSTIDST`. Ese respaldo sirve para la restitución, pero
**no hace falta para medir ni limpiar la mora**: el conjunto afectado se deduce solo con dos
condiciones sobre el dato actual:

```
préstamo.PRSTIDST = 8                    -- ya restituido por el usuario
   Y
cuota.DTPRMRAA > 0                       -- tiene mora escrita
```

Un préstamo legítimamente en 8 nunca tiene mora escrita (el proceso sale antes de calcularla).
Si la tiene, es exactamente el rastro del defecto — sin importar si el agente tiene o no acceso a
la lista de respaldo original.

### Medido contra la base local el 2026-08-27

| Métrica | Valor |
|---|---|
| Préstamos en estado 8 (total) | 106 |
| Préstamos en estado 8 con mora escrita | **106 de 106** — el 100 % |
| Cuotas afectadas | 4.600 |
| Mora escrita (total) | $598.465,14 |
| Mora pendiente (total) | $598.416,86 |
| Cuotas que quedaron en `DTPRESTD = 5` | 4.580 |
| Cuotas con mora **ya cobrada** (pagos vigentes) | 16 |
| Monto de mora ya cobrada | $48,28 |

**Los 106 préstamos que hoy están en 8 tienen, sin excepción, mora escrita.** Confirma que el
universo "estado 8 + mora > 0" no es una aproximación: en esta base, hoy, es exactamente el
100 % de los préstamos restituidos. La mora ya cobrada es una fracción mínima (~0,008 % del
total escrito) y queda separada, sin tocar (§4).

---

## 3. Qué se limpia y cómo (Bloques 2–4 y 6 del script)

### 3.1 Respaldo

`CRD.BKP_DTPR_MORA_PV_20260827` — copia completa de las 4.600 filas afectadas, antes de tocar
nada. `CRD.TMP_VIGENTE_MORA_PV_20260827` — pagos vigentes (`PGPRANUL IS NULL OR PGPRANUL = 0`)
agregados por cuota, para las dos operaciones de abajo.

### 3.2 Limpieza de los campos de mora

Solo en las cuotas **sin mora ya cobrada** (ver §4): `DTPRMRAA`, `DTPRMRCL`, `DTPRDSMR`,
`DTPRSLMR` a cero.

`DTPRTTLL` se recompone con la **misma fórmula de idempotencia** que usa el propio proceso
diario (`PROCESO-DIARIO-INTERES-MORA.md` §5), no un recálculo desde cero:

```
totalBase   = DTPRTTLL_actual − DTPRMRAA_actual
DTPRTTLL_nuevo = totalBase                    (la mora nueva es 0)
```

Igual que el proceso diario, esto respeta la base original de las cuotas cargadas desde Excel
—donde `DTPRTTLL` viene de la columna "CUOTA A PAGAR" y puede no coincidir con la suma de sus
componentes— en vez de reconstruirla e introducir una diferencia nueva. `DTPRTTCS` se actualiza
igual, porque es espejo exacto de `DTPRTTLL` (tabla de la §4 de ese documento); el pedido no lo
mencionó explícitamente pero dejarlo desalineado habría creado una inconsistencia nueva.

### 3.3 Estado de las cuotas que quedaron en `DTPRESTD = 5`

Se deriva de `CRD.PGPR` con pagos **vigentes** — nunca de `DTPRCPPG` (mismo criterio ya aplicado
en la segunda ola para el saldo de capital de la reestructuración, y el mismo patrón de
`MotorPagoPrestamoServiceImpl.calcularSaldosRealesCuota`):

```
pagado = capitalPagado + interesPagado + desgravamenPagado + seguroPagado   (todo vigente)

pagado ≈ DTPRTTLL (ya sin mora), tolerancia $0.01   → PAGADA (4)
pagado > 0 pero no cubre el total                    → PARCIAL (6)
pagado = 0 (o sin ninguna fila en PGPR)              → PENDIENTE (1)
```

`PENDIENTE(1)` como destino de "nada pagado" replica el mismo criterio que usa el sistema al
reconstruir una cuota desde cero (`AbonoCapitalPrestamoServiceImpl`, al re-amortizar: cuota
nueva sin pagos → `PENDIENTE`). `DTPRESTD` y `DTPRIDST` se escriben siempre juntos, para no
dejar el espejo desfasado (trampa documentada en `CLAUDE.md`).

**Solo se tocan cuotas que hoy están en `DTPRESTD = 5`.** Las que ya estaban en `PAGADA` o
`PARCIAL` por su propia historia de pagos no se tocan — el defecto no las afectó ahí, la mora
sí (limpiada igual en 3.2), pero no su clasificación.

### Resultado de la prueba (transacción con `ROLLBACK`, no aplicado)

| Antes | Después |
|---|---|
| 4.580 cuotas en `DTPRESTD = 5` | 4.573 → `PENDIENTE(1)`, 15 → `PAGADA(4)`, 12 → `PARCIAL(6)` |

Los 15 `PAGADA` y 12 `PARCIAL` no son error: son cuotas que sí recibieron pagos reales (sin
mora) mientras estaban mal clasificadas — el defecto les puso mora y estado 5 encima de una
cuota que en realidad ya se estaba pagando con normalidad.

Controles después (Bloque 6 del script, todos en 0 en la prueba): ninguna cuota limpiada quedó
en estado 5, el espejo `DTPRIDST = DTPRESTD` sigue intacto, y ninguna cuota limpiada conserva
alguno de los cuatro campos de mora distinto de cero.

---

## 4. Lo que NO se puede arreglar con un `UPDATE`

**16 cuotas, $48,28 en total**, tienen mora que **ya se cobró** (`PGPRMRPG` de pagos vigentes
> 0). Ese dinero salió del bolsillo de un partícipe por una mora que el sistema no debió
calcularle — no es un dato a corregir, es plata a resolver.

El script las **lista y cuantifica** (Bloque 1.3/1.4 antes de limpiar, Bloque 5 después, para
que quede registro de que se dejaron intactas) y **no las toca**: ni sus columnas de mora, ni su
`DTPRTTLL`, ni su estado. Decidir si se devuelve, se cruza contra otro concepto, o se deja
constancia y no se hace nada, es una decisión del usuario — este documento no la toma.

Dado el monto total ($48,28 sobre 16 cuotas, ~$3 promedio), es plausible que sea operativamente
más simple resolverlo caso por caso que diseñar un mecanismo automático — pero esa también es
una decisión del usuario, no una recomendación de este documento.

---

## 5. Reverso

El Bloque 7 del script está **completamente comentado**. Restaura las columnas tocadas desde
`CRD.BKP_DTPR_MORA_PV_20260827` fila por fila (`MERGE`, no `DELETE + INSERT`, para no depender
del generador de `DTPRCDGO`). Se descomenta a mano, línea por línea, solo si hace falta
deshacer la limpieza.

---

## 6. Orden de ejecución recomendado

1. Confirmar que el fix del código (`PROCESO-DIARIO-INTERES-MORA.md` §11) está desplegado y que
   el timer de las 02:00 ya no reclasifica préstamos en 8 (prueba 8 de la §10 de ese documento).
2. Confirmar que la restitución de `PRSTIDST = 8` ya se hizo (`ESTADO-TRABAJO-EN-CURSO.md` §4,
   tarea 2), y que un día después ningún préstamo restituido se movió (tarea 3 de esa sección).
3. Correr el Bloque 1 de `77_LIMPIEZA_MORA_PLAZO_VENCIDO.sql` y comparar contra la tabla de la
   §2 de este documento — si los números no se parecen, algo cambió y hay que revisar antes de
   seguir.
4. Correr los Bloques 2, 3 y 4 (respaldo, limpieza, re-derivación de estado).
5. Correr el Bloque 6 (control después) y confirmar que las cuatro consultas dan 0 donde se
   espera 0.
6. Revisar el Bloque 5 (mora ya cobrada) y decidir qué hacer con esas 16 cuotas — sin apuro,
   ese dato no se degrada con el tiempo.
