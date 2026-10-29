# Abono a capital en re-proceso: "al día" a la fecha del abono (D-048)

Aprobado por el líder del proyecto el 2026-10-29 (decisión D-048, expediente `cuadreSuper`,
`03-trabajo/meses/2025-07/sql/30-detencion-cierre-julio.md` §2). Código:
`AbonoCapitalPrestamoServiceImpl` (`validarAlDiaALaFecha`, `prepararCuotasReutilizadas`,
`validarCuotasReutilizadas`, `reutilizarCuota`).

## 1. El problema

El abono a capital con re-amortización exige que el préstamo esté al día (validación 3 de
`ESPECIFICACION-SERVICIOS-PAGO-PRESTAMOS.md` §7.3): ninguna cuota pendiente vencida antes de la fecha del abono y
**ninguna cuota PARCIAL**. Lo evaluaba con el estado de **hoy**. En el re-proceso de un mes pasado eso rechaza
abonos legítimos: el préstamo 4926 (operación 70473) abonó 32.170,90 el 31-jul-2025, pero el re-proceso Petro de
julio (carga 353) ya imputó a la cuota 17 (vence 31-ago) un excedente de 0,53 con fecha 31-jul, y el motor
respondía `PRESTAMO_NO_AL_DIA: cuota #17 está PARCIAL`. Además, aunque se aceptara, la cuota 17 tiene filas en
`CRD.PGPR` (y en `CRD.DSBN`), así que la re-amortización no podía borrarla (`FK_PGPR_DTPR`).

## 2. La regla (solo con `reproceso: true`)

**Fecha del abono** = la fecha de pago de la solicitud (`fecha`; en CBCR, `CBCRFCHA`); si no viene, la fecha de
afectación.

Misma lectura al corte que el cierre de cartera (`ModoCorteCartera`, `CierreCarteraDaoServiceImpl.pendiente`,
D-019/D-044): una cuota está **liquidada a la fecha** si su estado es PAGADA (4) o CANCELADA_ANTICIPADA (7) y su
`DTPRFCPG` (la fecha del último pago que la completa, D-044) es `<=` fecha.

| Cuota | Condición | Si no se cumple |
|---|---|---|
| Vencimiento `<=` fecha del abono (convención inclusiva) | Liquidada a esa fecha | `PRESTAMO_NO_AL_DIA` (dice qué cuota y por qué: impaga, o liquidada después) |
| Vencimiento `>` fecha, liquidada hoy | Liquidada con fecha `<=` abono | `PAGOS_POSTERIORES_AL_ABONO` |
| Vencimiento `>` fecha, pendiente, con pagos vigentes | Todos con fecha `<=` abono (pagos anticipados) | `PAGOS_POSTERIORES_AL_ABONO` |

Una cuota futura **PARCIAL por un pago anticipado ya no bloquea**. `PAGOS_POSTERIORES_AL_ABONO` significa que una
operación posterior al abono ya está aplicada y la re-amortización no la puede mover: el abono se re-procesa antes
que ella (anular, abonar, volver a aplicar).

### 2.1 Cuotas con pagos anticipados: re-amortización en el lugar

Las cuotas a re-amortizar que tienen filas en `CRD.PGPR` (pagos anticipados vigentes, o pagos anulados) no se
borran:

1. Se historizan en `CRD.HDTP` como todas (mismo evento).
2. La fila de `CRD.DTPR` se **actualiza en el lugar** con la cuota nueva del mismo número (conserva su código, sus
   pagos y su distribución por banda).
3. Sus acumulados se recomponen con sus pagos vigentes, igual que el motor al aplicar un pago parcial: pagado por
   componente, saldo de capital = saldo inicial − capital pagado, saldo = total − pagado; estado PARCIAL (o PAGADA
   con la fecha del último pago, si lo cubren).
4. El capital que esos pagos ya cubrieron vuelve a la tabla nueva (`nuevoCapital = pendiente + prepagado − abono`),
   porque el pago se queda en la cuota.
5. Se rechaza (`PRESTAMO_NO_AL_DIA`) si la cuota ya no existe en la tabla nueva (modalidad 1 acortó antes de ella) o
   si sus pagos no caben en la cuota nueva componente por componente.
6. El ancla del abono (`DTPRSLOT`) sigue siendo la última cuota PAGADA, nunca una cuota re-amortizada en el lugar.

**Anulación del abono** (`POST /prst/anularOperacion`, `ProcesoPagoPrestamoServiceImpl`): la cuota re-amortizada en
el lugar se reconoce porque su código es el `codigoOriginal` de una fila histórica del evento; no se borra, se
restaura en el lugar desde `CRD.HDTP` con sus pagos anticipados. Si tiene un pago vigente con fecha posterior al
abono, se rechaza con `PAGOS_SOBRE_TABLA_RECALCULADA` (anularlo primero), igual que en las demás cuotas nuevas.

## 3. Por dónde entra

| Camino | Cómo |
|---|---|
| `POST /cbcr/{id}/procesar` con `"reproceso": true` (tipo `ABONO_CAPITAL`) | `procesarCobro` pasa la bandera y la fecha de afectación del cobro a `aplicar` |
| `POST /prst/abonarCapitalConAportes` | campo nuevo `reproceso` (opcional) |
| `GET /prst/simularAbonoCapital/{id}?valor&modalidad&fecha=AAAA-MM-DD&reproceso=true` | parámetros nuevos opcionales; sin ellos, igual que antes |

**Sin regenerar tabla (C16b)** no cambia: nunca tuvo la validación "al día" (solo exige que la tabla cuadre con el
monto) y ya ubicaba la cuota a la fecha del abono.

## 4. Fuera de re-proceso

La regla de siempre no cambia (estado de hoy; PARCIAL bloquea). Dos correcciones del mismo defecto (evaluar con
"hoy" lo que es de la fecha del abono), que no cambian nada cuando el cobro es del día:

- El pre-chequeo del abono mixto en CBCR (`motivoAbonoInviable`, AA8) simulaba con la fecha de hoy y el abono se
  aplicaba con la fecha del cobro; ahora simula con la fecha del cobro.
- El abono por CBCR no pasaba la fecha de afectación del cobro, y la validación de período abierto (C17) miraba el
  mes de la fecha de pago. Ahora la pasa, igual que la precancelación desde el 2026-10-05.

## 5. Otros motores revisados

- **Precancelación** (`ProcesoPagoPrestamoServiceImpl.precancelar`): no tiene validación "al día"; liquida lo
  exigible a la fecha y las futuras. No cambia.
- **Precancelación con préstamo nuevo** (C16, desembolso neto): ya trabaja con la fecha de la operación
  (`cuotasPorMes`). No cambia.
- **Abono con aportes** (`abonarConAportes`): usa el mismo cálculo; recibe la bandera `reproceso`.
- **Sustitución / novación** (`SustitucionPrestamoServiceImpl`): usa su propia regla de atraso; fuera del alcance de
  D-048.

## 6. Ejemplo verificado: préstamo 4926, abono del 31-jul-2025

`cuadreSuper/03-trabajo/meses/2025-07/sql/32-abono-4926-al-dia-d048.sql` (solo SELECT, base de trabajo del
29-oct-2026):

- Cuotas 0 a 16 (vencen hasta el 31-jul-2025): todas PAGADA con fecha de pago `<=` 31-jul. Al día.
- Cuotas futuras liquidadas después del abono: ninguna.
- Cuota 17 (vence 31-ago): pago vigente 327852 de 0,53 (desgravamen, carga 353, 31-jul) y dos anulados
  → se re-amortiza en el lugar.
- Capital pendiente 47.855,74 − abono 32.170,90 = 15.684,84; desgravamen de la cuota 17 nueva ≈ 14,12 > 0,53.

Simulación: `GET /prst/simularAbonoCapital/4926?valor=32170.90&modalidad=1&fecha=2025-07-31&reproceso=true`.
