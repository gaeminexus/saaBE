# CCPM — CreditoCuotasPrestamosMensual

> ⚠️ Si se modifica la lógica de este reporte, actualizar este archivo.
> **La lógica base de CCPM es idéntica a G48.** Si cambia G48, revisar si este archivo necesita ajustes.
> La lógica de grupos, calificación y provisión está en `_LOGICA_COMPARTIDA_CREDITO.md`.

**Tabla destino:** `RPR.CCPM`
**Servicio:** `GeneracionCCPMServiceImpl.java`
**G equivalente:** G48 (`GeneracionG48ServiceImpl.java`)

## Lógica base
Idéntica a G48. Ver `G48.md` y `_LOGICA_COMPARTIDA_CREDITO.md`.

## Diferencias respecto a G48

| Aspecto | G48 | CCPM |
|---|---|---|
| Tabla control ejecución | `RPR.EJRC` / `RPR.EJRD` | `RPR.EJCC` |
| Tabla destino | `RPR.CG48` | `RPR.CCPM` |
| `valorDesgravamen` | No existe | `DetallePrestamo.desgravamen` |
| `valorIncendio` | No existe | `DetallePrestamo.valorSeguroIncendio` |
| `interesOrdinarioDelMes` | No existe | Interés de la cuota del mes (no acumulado) |
| `interesMoraDelMes` | No existe | `max(0, interesMoraActual - interesMoraEjecucionAnterior)` |
| `fechaPrestamo` | No existe | `Prestamo.fecha.toLocalDate()` |
| Desglose de capital | No existe | 5 bandas temporales (ver abajo) |
| `provisionConstituida` | Lógica compleja (mes anterior + HM48) | Siempre `0` |
| `tipoCredito` | Primera letra del producto (E→Q) | Nombre completo del producto |
| Fuente `valorTotalCuentaIndividual` | `RPR.CG42` del mismo EJRC | `RPR.CPRM` del mismo EJCC (suma de todos los `total` por entidad) |

## Desglose de capital por vencer en 5 bandas

El `valorPorVencer` se distribuye usando cuotas futuras ordenadas por `numeroCuota ASC` (índice base 0):

| Banda | Campo | Índices |
|---|---|---|
| 1–30 días | `capitalPorVencer1a30` | 0 |
| 31–90 días | `capitalPorVencer31a90` | 1 a 3 |
| 91–180 días | `capitalPorVencer91a180` | 4 a 6 |
| 181–360 días | `capitalPorVencer181a360` | 7 a 11 |
| Más de 360 días | `capitalPorVencerMas360` | 12 en adelante |

### Origen de cuotas para el desglose — diferenciado por grupo

> ⚠️ El desglose usa batches diferentes según el grupo.

| Grupo | Método batch | Condición | Razón |
|---|---|---|---|
| **Grupo 1** | `selectCapitalCuotasFuturasBatch` | `fechaVencimiento > fechaFin` | La cuota del mes ya fue pagada; el desglose parte de la siguiente |
| **Grupo 2** | `selectCapitalCuotasDesdeInicioMesBatch` | `fechaVencimiento >= fechaInicio` | La cuota del mes no está en mora; va al `cv30` (índice 0) |

```
Grupo 2:
[mora_ene][mora_feb][mora_mar] | [abr→cv30] [may→cv90] [jun→cv180] ...
                                ↑ fechaInicio
```

### Ajuste de cuadre
Si `suma(bandas) ≠ valorPorVencer` → la diferencia se absorbe en la banda de mayor plazo con saldo > 0. Se registra `estadoDesglose = 1`.

## interesMoraDelMes
- Se carga la mora de la ejecución anterior del CCPM en `Map<numeroOperacion, interesMora>` al inicio.
- `interesMoraDelMes = max(0, interesMoraActual - interesMoraAnterior)`
- Si es la primera ejecución → `0`.

## Historial de cambios
| Fecha | Cambio |
|---|---|
| 2026-07 | Corrección Grupo 2: `valorVencido` excluye la cuota del mes (`fechaVencimiento < fechaInicio`). `valorPorVencer` usa `saldoInicialCapital` completo. Ver `_LOGICA_COMPARTIDA_CREDITO.md`. |
| 2026-07 | Corrección bandas: Grupo 1 usa `selectCapitalCuotasFuturasBatch` (> fechaFin). Grupo 2 usa `selectCapitalCuotasDesdeInicioMesBatch` (>= fechaInicio) para que la cuota del mes caiga en `cv30`. |
| 2026-07 | `selectSumaCapitalInteresGrupo2Batch` recibe `fechaInicio` además de `fechaFin`. |
