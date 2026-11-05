# Cancelados que el cierre «a fecha de corte» revive como cartera — diseño del arreglo de fondo

**omen-saa-1-arb · 2026-11-05 · estado: DISEÑO, la regla final espera la salida de `sql/365`.**

## 1. Síntoma (cierre de octubre 2026, producción)

- `saa.cartera.alCorte=siempre` desde el pase del 01-11 (`PASE-PRODUCCION-FUSION-2026-11-01.md`): octubre es el
  primer cierre mensual de producción en modo al corte.
- B17 al cierre: «Préstamos prendarios vencidos – más de 360 días» 148.588,38; «Quirografarios novación – más de
  360 días» 125.039,57, con todos los novados cancelados.
- Avisos ⑨ (contabilidad − módulo): Q −14.623.091,45; H −3.518.448,54; P −129.306,93; «69 bandas … 1,80E7». La
  cartera total ronda 16,3 M: el módulo al corte tiene ~18 M de más.
- Prendarios: 20.642,46 (`1.3.08.25` en la nivelación del 13-10) + 129.306,93 = 149.949,39 = subtotal del B17.

## 2. Causa (código)

`CierreCarteraDaoServiceImpl`, modo al corte:

| Fragmento | Regla | Efecto |
|---|---|---|
| `universo(corte)` | `PRSTFCHA <= corte` y `PRSTIDST NOT IN (0,1,6,7,12)` | entran los cancelados 3, 4, 5, 9, 13 |
| `pendiente(corte)` | fuera solo si `DTPRESTD IN (4,7)` **y** `DTPRFCPG <= corte` | cuota de cancelado sin estado/fecha de pago = pendiente |
| `pagos(corte)` | `PGPR` vigentes con `PGPRFCHA <= corte` | cancelado sin `PGPR` (novación, migración, cancelación por dato) = capital entero |

El cancelado vuelve con todo su capital, envejecido al corte → banda «más de 360 días». El ⑨ lleva cada
cuenta de banda al módulo y descarga el neto del grupo en la `.25` (por vencer más de 360) — el total del grupo
no cambia pero el reparto queda deformado.

El mismo patrón (cancelado + cuota sin 4/7 con fecha) está en `PrestamoDaoServiceImpl.selectByEntidadYProductoVigentesAlCorte`.

## 3. Lo que el arreglo tiene que conservar

El universo incluye cancelados a propósito: el reproceso del cuadre cierra meses viejos (2025–2026) y un préstamo
cancelado **después** de ese corte era cartera al corte. Eso no se toca. Lo que se corrige es el cancelado
**antes** del corte.

## 3bis. Medido con el 364 en producción (2026-11-05) — la causa precisa

| Estado préstamo | Préstamos | Cuotas | Capital que el modo al corte cuenta |
|---|---|---|---|
| 2 vigente | 812 | 38.691 | 8.586.728,76 |
| 8 plazo vencido | 237 | 16.006 | 4.510.296,14 |
| 11 en mora | 179 | 10.772 | 2.959.381,30 |
| **3 cancelado** | 70 | 1.150 | **286.862,57** |
| **4 cancelado anticipado** | 2.913 | 106.690 | **17.878.477,99** |

Vivos = 16,06 M (la cartera real); cancelados = **18,17 M revividos** (los ~18 M de los avisos ⑨).

**Todas** las cuotas revividas están en `DTPRESTD` 4 (PAGADA) o 7 (CANCELADA ANTICIPADA) **sin `DTPRFCPG`**. No
son cuotas impagas: son cuotas liquidadas sin fecha de pago (migración, cancelación anticipada que no fecha las
cuotas). El fragmento `pendiente(corte)` hace `NVL(d.DTPRFCPG, :corteSig) < :corteSig`: **una fecha nula se
trata como «pagada después del corte»**, o sea pendiente. Ahí está el defecto; el universo no necesita cambio.
Los 30 mayores son préstamos de 2012–2023 con último vencimiento entre 2017 y 2025.

Asientos del cierre de octubre: `12343` (①), `12344` (②), `12345` (①.1), `12352` (⑨), todos ya en
`ASNTESTD = 2` (ANULADO) al 05-11.

## 4. Regla (corregida con la salida del 364)

Se corrige `pendiente(corte)`: la cuota en 4/7 **sin** `DTPRFCPG` toma como fecha de liquidación la primera
evidencia que exista, en este orden:

1. `MAX(PGPRFCHA)` de los pagos vigentes de **esa cuota**;
2. la fecha de cancelación deducida del **préstamo** (abajo);
3. si no hay ninguna: **liquidada antes del corte** (fuera de la cartera).

Una cuota en 4/7 **con** `DTPRFCPG` sigue la regla de siempre. Lo de abajo (fecha deducida del préstamo) queda
como el paso 2.

### Fecha de cancelación deducida del préstamo (paso 2)

`CRD.PRST` no tiene fecha de cancelación. Se deduce:

```
F_CANCELACION = GREATEST de la evidencia disponible:
  fecha del préstamo sustituto (PRST n con n.PRSTPROR = p.PRSTCDGO, aprobado)   -- novación/reestructuración
  MAX(DTPRFCPG) de sus cuotas
  MAX(PGPRFCHA) de sus pagos vigentes
  MAX(EVPRFCHA) de sus eventos vigentes
```

- Solo se usa para cuotas 4/7 sin `DTPRFCPG` y sin pagos propios (paso 2 de la regla).
- `PRSTFCMD` no se usa: cambia con cualquier edición.
- Vivos (2, 8, 10, 11) y el modo normal (`corte == null`): sin cambio. `universo(corte)` no cambia.

Decisiones que esperan la salida del 365:
- si `F_SUSTITUTO` existe para los novados de producción (si no, la novación migrada solo tiene las otras);
- si algún cancelado revivido tiene evidencia `> 31-10` (debería ser casi nada en producción);
- si conviene materializar la fecha deducida (columna nueva en `PRST` llenada por script + por el código que
  cancela) en vez de calcularla en cada consulta. Recomendación del árbitro: **sí** a mediano plazo; para este
  arreglo, cálculo en la consulta.

## 5. Alcance del cambio de código

- `CierreCarteraDaoServiceImpl.pendiente(corte)` — un solo fragmento, lo usan todas las lecturas al corte.
  Revisar que ninguna otra lectura al corte del mismo DAO (o de los controles C14/G4x) repita
  `NVL(DTPRFCPG, :corteSig)` por su cuenta.
- `PrestamoDaoServiceImpl.selectByEntidadYProductoVigentesAlCorte` — misma condición
  (`d.fechaPagado IS NULL` = vivo); misma regla.
- Doc `CIERRE-CARTERA-A-FECHA-DE-CORTE.md` actualizado en el mismo cambio.
- Dueño del código: el cuadre (C15). Se coordina con su árbitro antes de despachar; el texto lo aprueba el usuario.

## 6. Después del arreglo

1. Revertir los asientos del cierre de octubre (salida del bloque 4 del 364) con la reapertura de cartera (C17).
2. Desplegar el EAR con el arreglo.
3. Volver a correr el cierre de octubre; esperado: avisos ⑨ de miles, no de millones; B17 de novación en ~0.
