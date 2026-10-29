# API — Cobro de un mes pasado en modo re-proceso (D-019 en el motor de cobros, `CRD.CBCR`)

Aprobado por el líder del proyecto el 2026-10-28 (decisión D-019 ampliada al motor de cobros; expediente
`cuadreSuper`, `03-trabajo/meses/2025-06/sql/13-borrado-pagos-migrados-junio.md` §4.4, camino (a)).
Contrato base del cobro: `API-COBROS-APROBACION-CONTABILIDAD.md`. Fecha de afectación: `API-FECHA-AFECTACION-COBRO.md`.

## 1. El problema

El re-proceso de jun-2025 → nov-2026 registra con el motor de cobros (`/cbcr/registrar` → `aprobar` → `procesar`)
depósitos del extracto de meses pasados. Algunos son pagos de préstamos que **en ese mes estaban vigentes** y **hoy
están cancelados** (en el lote de junio 2025: 2937, 2959, 6201, 6949, 8069, 8085, 8140 y 8183). El motor de pagos
(`MotorPagoPrestamoServiceImpl`, `ProcesoPagoPrestamoServiceImpl.validarPrestamoOperable`,
`AbonoCapitalPrestamoServiceImpl`) rechaza cualquier préstamo en estado terminal (3, 4, 5, 9, 13): el estado de HOY
decide, y el pago de junio no entra.

Es el mismo caso que D-019 resolvió para el re-proceso de cargas Petro (`petro/REPROCESO-CARGA-PETRO.md` §4.8–4.9).

## 2. La regla

Con `reproceso: true` en `/cbcr/{id}/procesar`, **antes** de la corrección de mora H42 y de cualquier motor, cada
préstamo del cobro se evalúa **vigente al corte**:

- **Corte** = último día del mes de la **fecha de pago** del cobro (`CBCRFCHA`), inclusive.
- **¿Estaba vigente al corte?** Mismo criterio que Petro (`PrestamoDaoService.selectByEntidadYProductoVigentesAlCorte`):
  activo hoy (1, 2, 8, 10, 11), o terminal hoy (3, 4, 5, 9, 13) que empezó a más tardar en el corte, tiene alguna cuota
  vencida a más tardar en el corte y alguna cuota no saldada al corte. Si **no** lo estaba, el proceso se rechaza con
  `PRESTAMO_NO_VIGENTE_AL_CORTE` y no se toca nada.
- **Si hoy es terminal** (D-019): las cuotas saldadas sin fecha de pago o con fecha posterior al corte se reconstruyen
  desde sus pagos vigentes (`recalcularCuotaDesdePagos`) con su mora al corte; el préstamo pasa a VIGENTE (2) con la
  marca `REPROCESO CBCR <id> (corte AAAA-MM-DD): cancelación posterior pendiente de re-aplicar (estado anterior N)` en
  `PRSTOBSR`; los eventos no anulados posteriores al corte reciben la misma marca (no se anulan: se recalculan al
  re-procesar su mes).
- **Siempre** (D-019 ampliada): las cuotas con vencimiento en el mes del corte en PAGADA/CANCELADA_ANTICIPADA sin
  ningún pago vigente vuelven a pendiente.

Después, el cobro se aplica **exactamente como en el proceso normal** (mismos motores, mismos tres asientos, mismo
invariante cobrado = aplicado). Si el pago salda el préstamo, el motor lo deja cancelado como siempre; si no, queda
VIGENTE con la marca hasta re-procesar el mes de su cancelación real.

La regla vive en un solo lugar, `ReaperturaVigenteAlCorteService` (`prepararPrestamoAlCorte`), que también usan el
re-proceso Petro y la pensión de un mes pasado (C20, `API-REPROCESO-PENSION-PERIODO.md`). Nada se borra.

### 2.1 Cuándo aplica (lo más seguro)

| Condición | Si no se cumple |
|---|---|
| El cuerpo de `procesar` trae `"reproceso": true` | Proceso normal, sin ningún cambio de comportamiento |
| Fecha de pago **y** fecha de afectación en un mes **anterior** al mes en curso | `REPROCESO_NO_APLICA` |
| Tipo con préstamo: `PAGO_CUOTA`, `PAGO_MULTIPLE`, `ABONO_CAPITAL`, `PRECANCELACION`, `COBRO_MIXTO` | `REPROCESO_NO_APLICA` (`REGISTRO_APORTE` y `ACUERDO_CONDONACION` no lo usan) |
| Período contable de la fecha de afectación **abierto** (se verificó al registrar y se repite al procesar) | `PERIODO_CERRADO` |

Se eligió la bandera explícita, y no "cualquier cobro con fecha de un mes pasado", para que un cobro tardío normal
de la operación diaria nunca reabra un préstamo cancelado sin que alguien lo pida. El registro (`/cbcr/registrar`) no
cambia: ya aceptaba préstamos en 3 y 4 (solo rechaza 5, 12 y 13); el rechazo era del motor al procesar. Por eso
los préstamos cancelados por novación (5) o reestructuración (13) siguen sin poder recibir cobros.

### 2.2 Transacción

Todo corre en la transacción de `procesar`. Si el motor rechaza el pago, la reapertura se revierte con él (el cobro
queda APROBADO). Si el cobro termina en **RECHAZADO automático** (staleness de precancelación o de abono con aportes),
la reapertura **queda** —igual que la mora H42— y se informa en la respuesta: el préstamo queda VIGENTE con la marca,
y el cobro corregido que se registre después la encuentra lista (la preparación es idempotente).

## 3. Endpoint

```http
POST /SaaBE/rest/cbcr/{id}/procesar
{ "usuario": "CUADRE-SB", "reproceso": true }
```

`registrar` y `aprobar` no cambian. Respuesta (`ResultadoProcesoCobro`), dos campos nuevos:

| Campo | Qué es |
|---|---|
| `reproceso` | `true` si se procesó en este modo |
| `reaperturaAlCorte` | Líneas de lo que D-019 cambió: `"Préstamo 8085 (estado anterior 3): N cuota(s) reabiertas, 4 evento(s) posteriores marcados"` y una por cada cuota del mes reabierta. Vacía si no hizo falta reabrir nada |

`mensaje` dice `"Cobro procesado en modo re-proceso (D-019, corte 2025-06-30)."`. Errores nuevos (HTTP 500 del
endpoint, con el texto): `REPROCESO_NO_APLICA`, `PRESTAMO_NO_VIGENTE_AL_CORTE`, `PERIODO_CERRADO`.

## 4. Ejemplo: lote de cobros de junio 2025

Lote `cuadreSuper/03-trabajo/cartera/resultados/13-lote-cobros-junio-2025.json` (62 filas). Las filas en estado
`REQUIERE_4.4` (préstamos 2937, 2959 ×3, 6201, 6949, 8069, 8085, 8140, 8183) se procesan con `"reproceso": true`;
las demás pueden ir con o sin la bandera (con ella, además se reabren las cuotas de junio saldadas sin pago).

Medido el 2026-10-28 en la base de trabajo, **los 8 estaban vigentes al 30-jun-2025** según el criterio:

| Préstamo | Estado hoy | Inicio | Cuotas vencidas al corte | Cuotas no saldadas al corte | Eventos posteriores no anulados |
|---|---|---|---|---|---|
| 2937 | 4 | 2024-09-04 | 10 | 50 | 0 |
| 2959 | 4 | 2023-03-22 | 28 | 58 | 0 |
| 6201 | 4 | 2020-09-23 | 58 | 14 | 0 |
| 6949 | 3 | 2021-06-16 | 49 | 13 | 0 |
| 8069 | 3 | 2015-02-24 | 124 | 134 | 0 |
| 8085 | 3 | 2012-06-29 | 156 | 162 | 4 (más 1 anulado) |
| 8140 | 3 | 2020-08-26 | 59 | 61 | 0 |
| 8183 | 4 | 2024-07-24 | 12 | 107 | 3 |

8183 tiene una precancelación real del 22-oct-2026 (sus 3 eventos posteriores quedan marcados, no anulados). 6201 es
la precancelación del 3-jun: el motor recalcula el valor con las 14 cuotas que restituye el SQL de §4.3 del borrado.

## 5. Control

`sql/333_CONTROL_REPROCESO_COBRO_D019.sql`: antes (los préstamos del lote vigentes al corte), después (cobros del
mes, préstamo, estado de hoy, valor aplicado y marca D-019; préstamos y eventos marcados; cuotas del mes; fecha de
los tres asientos = fecha de afectación).
