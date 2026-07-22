# Lógica Compartida — Crédito (G48 y CCPM)

> ⚠️ Este archivo es referenciado por `G48.md` y `CCPM.md`.
> **Si cambia cualquier lógica aquí → actualizar este archivo Y verificar si G48.md o CCPM.md necesitan ajustes en su sección de diferencias.**

---

## Grupos de cuotas

### GRUPO 1 — Cuotas pagadas del mes
Consulta: `selectCuotasDelMesGlobal(fechaInicio, fechaFin)`
- `d.estado = 4` (PAGADA)
- `d.fechaVencimiento BETWEEN fechaInicio AND fechaFin`

### GRUPO 2 — Menor cuota vencida no pagada
Consulta: `selectMenorCuotaAnteriorAlMesGlobal(fechaInicio, fechaFin)`
- `d.estado IN (1, 2, 3, 5, 6, 7)` (cualquier estado excepto pagada)
- `d.fechaVencimiento <= fechaFin`
- `d.prestamo.idEstado IN (2, 8, 11)` (activo, reestructurado, mora judicial)
- `d.numeroCuota = MIN(numeroCuota)` que cumpla lo anterior, por préstamo

Un préstamo del Grupo 1 **no** entra al Grupo 2.

---

## Regla del último día del mes (CRÍTICA)

> Por política del fondo, **todas las cuotas vencen el último día del mes**.
> La cuota del mes de ejecución **NO está en mora** aunque no esté pagada —
> el socio tenía hasta ese día para pagar.
> Esta regla afecta exclusivamente al **Grupo 2**.

---

## VALOR VENCIDO y VALOR POR VENCER

### Grupo 1
| Campo | Valor |
|---|---|
| `valorVencido` | `0` (ya pagadas) |
| `valorPorVencer` | `saldoInicialCapital - capital` de la propia cuota |

### Grupo 2
| Campo | Consulta | Valor |
|---|---|---|
| `valorVencido` | `selectSumaCapitalInteresGrupo2Batch(codigosCuotas, fechaInicio, fechaFin)` | Suma de `capital` de cuotas con `numeroCuota >= cuotaMora` y `fechaVencimiento < fechaInicio` (excluye la cuota del mes) |
| `valorPorVencer` | `selectSaldoInicialCapitalDelMesBatch(codigosPrestamos, fechaInicio, fechaFin)` | `saldoInicialCapital` de la cuota del mes completo (sin restar capital, porque no está en mora) |

```
[mora_ene] [mora_feb] [mora_mar] [cuota_abr*] [may]  [jun]
|←————————— VENCIDO ————————————|
                                 |←—————— POR VENCER ——————————→|
(* cuota del mes: no en mora, va a POR VENCER)
```

> La suma `valorVencido + valorPorVencer` siempre iguala el capital outstanding total. No hay duplicación.

---

## Interés de Mora

Consulta batch: `calcularInteresMoraBatch(codigosCuotas, fechaFin)`
Fórmula por cuota: `capital × (interesNominal / 100 / 360) × diasMora`
- `diasMora = DAYS.between(fechaVencimiento, fechaFin)`
- Si `diasMora <= 0` → no suma (protege automáticamente la cuota del mes: 0 días de mora)
- Grupo 1 → siempre `interesMora = 0`

---

## Días de Morosidad y Calificación

`diasMorosidad = DAYS.between(fechaVencimiento, fechaFinDate)` — si estado = 4 → `diasMorosidad = 0`

### Tabla general (productos no hipotecarios)
| Días | Calificación |
|---|---|
| 0 | A1 |
| 1–15 | A2 |
| 16–30 | A3 |
| 31–60 | B1 |
| 61–90 | B2 |
| 91–120 | C1 |
| 121–180 | C2 |
| 181–270 | D |
| > 270 | E |

### Tabla hipotecaria (productos con código 7, 8 o 21)
| Días | Calificación |
|---|---|
| 0 | A1 |
| 1–30 | A2 |
| 31–60 | A3 |
| 61–120 | B1 |
| 121–180 | B2 |
| 181–210 | C1 |
| 211–270 | C2 |
| 271–450 | D |
| > 450 | E |

---

## Porcentajes de Provisión Requerida Original

`provisionRequeridaOriginal = valorSujetoProvision × porcentaje` (si VSAP > 0, sino 0)

| Calificación | Porcentaje |
|---|---|
| A1 | 0.99% |
| A2 | 1.99% |
| A3 | 2.00% |
| B1 | 5.00% |
| B2 | 10.00% |
| C1 | 20.00% |
| C2 | 40.00% |
| D | 60.00% |
| E | 100.00% |

---

## Valor Total Cuenta Individual

- Si `entidad.idEstado = 30` (jubilado voluntario) → siempre `0`
- Solo se asigna en la **primera cuota** de cada entidad (por `numeroIdentificacion`); el resto → `0`
- **G48:** fuente = `RPR.CG42` del mismo EJRC (`saldoAportePatronal + saldoAportePersonal + rendimiento`)
- **CCPM:** fuente = `RPR.CPRM` del mismo EJCC (suma de todos los `total` por entidad)

## Valor Sujeto a Provisión
```
valorSujetoProvision = max(0, (valorPorVencer + valorVencido) - valorTotalCuentaIndividual)
```
