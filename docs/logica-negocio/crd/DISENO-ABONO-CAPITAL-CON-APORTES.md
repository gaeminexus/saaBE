# DISEÑO — Abono a capital con aportes del partícipe

**Equipo:** `omen-saa-1` · **Escrito:** 2026-10-12 por `omen-saa-1-arb`, **antes** de despachar (regla 7).
**Pedido:** *«Se solicita poder hacer abono de capital con los aportes de los partícipes. Modifica la
pantalla de cobros personales para que en la opción de abono capital haya como hacerla con aportes.»*
**Contrato:** `crd/API-ABONO-CAPITAL-CON-APORTES.md` (espejado en `saaFE/docs/crd/`).

---

## 1. Decisiones del usuario (2026-10-12) — no re-litigar

| # | Decisión |
|---|---|
| **AA1** | El abono se puede pagar **mixto** (depósito + aportes) **y 100% con aportes**, igual que la precancelación. Con depósito pasa por la bandeja de contabilidad (CBCR); 100% aportes se aplica en el acto, sin aprobación. |
| **AA2** | Tipos de aporte: **los mismos que hoy** en pagar con aportes y precancelar con aportes. Cualquier tipo vigente con saldo, siempre que tenga cuenta en la plantilla 21 (aux1 50/51/52/53). **Sin regla nueva.** |
| **AA3** | Las **dos modalidades** (reducir plazo / reducir cuota), con la misma simulación y comparativa de hoy. |

## 2. Decisiones del árbitro (técnicas)

| # | Decisión | Por qué |
|---|---|---|
| **AA4** | El caso mixto **reutiliza la precancelación mixta**: los aportes viajan en `detalles[0].aportes` del CBCR y se guardan en la tabla existente `CRD.DAPR` (`DetalleAportePrecancelacion`). **Sin DDL.** | La tabla no tiene ninguna columna que la ate a la precancelación: lo que la ata es la validación de `CobroCreditoServiceImpl:1778-1790` y los `if PRECANCELACION`. El nombre de la tabla queda como deuda cosmética. |
| **AA5** | El caso 100% aportes va por un **endpoint nuevo**, `POST /prst/abonarCapitalConAportes`. `POST /prst/abonarCapital` **sigue cerrado con 409** y no se toca. | Ese 409 protege del abono con depósito que no pasa por CBCR. Reabrirlo con un «si trae aportes…» mezcla los dos casos en la misma puerta. |
| **AA6** | El camino directo genera **UN solo asiento**, que junta el cruce de aportes con el re-bandeo, y lo guarda en `EVPR.numeroAsiento`. | Un evento tiene un solo `numeroAsiento`, y `contabilizarReverso` anula solo ese. Con dos asientos, anular el abono dejaría uno vivo. Cada mitad cuadra por separado, así que la suma también cuadra. |
| **AA7** | El consumo del aporte se hace con `procesoPagoPrestamoService.consumirAportes` (el mismo de pagar y precancelar), enlazado al PGPR del abono. | Así la anulación (`anularOperacion` → `revertirAportes`) restituye el aporte **sin tocar la anulación**. |
| **AA8** | Al procesar un abono mixto en CBCR se repite el **pre-chequeo de solo lectura** de la precancelación: saldo de aportes y simulación con el total. Si falla, el cobro queda RECHAZADO por SISTEMA. | El saldo de aportes **no se reserva** entre el registro y el proceso; puede haber cambiado. |

## 3. Lo medido en el código (2026-10-12, `main` = `80a4c86f`)

- **El abono con aportes está bloqueado a propósito** en `CobroCreditoServiceImpl.java:1786`: *«aportes solo aplica a PRECANCELACION»*. El FE lo dice en `abono-capital-dialog.component.ts:30-31`.
- **El abono con depósito** genera estos asientos:
  - el **transitorio** al registrar: Banco / transitoria 2.3.01.15.01;
  - el **reparto** (CBCRASRP) al procesar;
  - el **definitivo** (CBCRASN2) al procesar;
  - y el **re-bandeo**, que lo genera `AbonoCapitalPrestamoServiceImpl.aplicar` (`:328`) y se guarda en `EVPR.numeroAsiento` y `PGPR.asiento`.
- **La precancelación mixta** consume los aportes en `ProcesoPagoPrestamoServiceImpl.consumirAportes` (`:717`):
  - bloquea al partícipe (H61);
  - escribe un APRT negativo con estado CONSUMIDO y un `PagoAporte`;
  - pone las líneas de aportes en el **Debe** del CBCRASN2 con `lineasCruceAportesConsumidos` (`CobroCreditoServiceImpl:2264`).
- **`anularOperacion`** maneja ABONO_CAPITAL (`ProcesoPagoPrestamoServiceImpl:1396`) y `revertirAportes` restituye cada `PagoAporte` del evento.
- **`AbonoCapitalPrestamoServiceImpl`** ya inyecta `ProcesoPagoPrestamoService` (`:61`), así que no se crea una dependencia nueva.
- **La invariante «cobrado = aplicado»** (`CobroCreditoServiceImpl:1214-1255`) excluye solo PRECANCELACION y ACUERDO_CONDONACION. Un abono mixto la rompería: lo aplicado es depósito + aportes y lo cobrado es solo el depósito.

## 4. ⛔ Riesgo sin medir: el capital futuro en los asientos 2 y 3

`calcularCapitalFuturoDelCobro` (`CobroCreditoServiceImpl`) suma `capitalFuturoPosteriorACorte` sobre **todos** los PGPR del evento, sin distinguir qué parte se pagó con depósito y qué parte con aportes («lo que decide es QUÉ se pagó, no CÓMO»). El asiento de reparto debita la transitoria por `cobro.valor − capitalFuturo` (`:2188`), y `cobro.valor` es **solo el depósito**.

En un abono, casi todo el capital es futuro. Con depósito 300 + aportes 200, el capital futuro puede rondar 500 y el Debe del reparto saldría **negativo** (300 − 500). El cuadre de `:2204` lo rechazaría y el cobro no se podría procesar.

**La precancelación mixta tiene la misma forma** y nadie reportó el problema, así que puede haber un detalle que lo evita y no se ve leyendo. **No se programa el asiento a ciegas:** el ítem 0 del BE lo mide con un caso concreto y lo reporta antes de escribir la parte contable.

### 4.1 ✅ Medido por el ítem 0 (`omen-saa-1-be`, 2026-10-12): el riesgo es REAL, y ya existe en producción

- **Abono mixto con depósito 300 + aportes 200:** el capital futuro ronda los 492. El Debe del reparto sale −192 y el cuadre de `:2204` revienta. **El cobro no se puede procesar.** El asiento definitivo tampoco cuadraría.
- **Precancelación mixta: tiene el MISMO defecto, y está hoy en producción.** Nada lo evita. Se salva solo si no hay corrida de apertura previa o si los aportes son chicos.
  - El impacto en producción lo mide `sql/321` (solo lectura). Una precancelación atascada debería quedar en estado APROBADO, porque la excepción hace rollback.
  - ✅ **`sql/321` corrido en producción el 2026-10-12: los tres bloques vacíos.** Nunca se registró una precancelación mixta, así que el defecto no tocó producción. Por eso nadie lo había reportado: el camino nunca se usó. No hay nada que regularizar.
- **El camino directo:** el cruce y el re-bandeo se pueden juntar en un asiento. Además necesita `agregaCierreAperturaCaminoDirecto`, por `T − capitalFuturo`.

### 4.2 Decisiones del árbitro sobre la corrección

| # | Decisión |
|---|---|
| **AA9** | **En CBCR, el capital futuro que se descuenta del reparto y del definitivo es el que NO cubrieron los aportes.** `capitalFuturoEfectivo = max(0, capitalFuturo − Σ DAPR del cobro)`, calculado una sola vez en `calcularCapitalFuturoDelCobro`, para que el reparto y el definitivo sigan saliendo del mismo número. Es el principio que el camino directo ya aplica: lo que se cierra contra la apertura es `T − capitalFuturo`, sin importar quién lo pagó (`agregaCierreAperturaCaminoDirecto:376`). Con 100% depósito el resultado es idéntico al de hoy. **Corrige también la precancelación mixta.** |
| **AA10** | **Caso borde: los aportes superan al capital futuro (`A > capitalFuturo`).** Los aportes pagaron también parte de lo que estaba abierto, y ese tramo (`A − capitalFuturo`) no lo puede cerrar el reparto, que solo mueve el depósito. Se cierra en el **asiento definitivo** con las mismas dos líneas que usa el camino directo (D `2.3.02.10` / H `1.4.05.10`, plantilla 1, aux 4 y 2, `agregaCierreAperturaCaminoDirecto`), por `A − capitalFuturo`. **Regla única: el tramo abierto que se pagó con aportes se cierra igual, venga del camino directo o del CBCR.** Se puede revisar si el contador dice otra cosa. |

## 5. Plan de trabajo

### Backend (`omen-saa-1-be`) — solo `crd`
- **Ítem 0 — medición, sin código.**
  - (a) Calcular a mano, desde el código, los asientos 1, 2 y 3 y el re-bandeo para un abono mixto (depósito 300 + aportes 200, préstamo al día, mes con corrida de apertura). Decir si el reparto sale negativo.
  - (b) El mismo cálculo para una precancelación mixta ya procesada, si existe alguna.
  - (c) Confirmar que `lineasReclasificacionAbonoCapital` y `lineasCruceAportesConsumidos` se pueden juntar en un asiento.
  - Reporta y **se detiene**.
- **Ítem 1 — mixto por CBCR.**
  - Permitir `aportes` en ABONO_CAPITAL, en `validar` y al persistir DAPR.
  - En `procesarCobro` (rama ABONO_CAPITAL): leer DAPR, hacer el pre-chequeo de AA8, y pasar a `SolicitudAbonoCapital` el `valor` total y los `aportes`.
  - En `aplicar`: si hay aportes, `consumirAportes` sobre el PGPR del abono.
  - Invariante: lo cobrado es el depósito más DAPR.
  - En CBCRASN2: agregar la rama de aportes para ABONO_CAPITAL. La parte de capital futuro se programa según lo que salga del ítem 0.
- **Ítem 2 — 100% aportes directo.**
  - `POST /prst/abonarCapitalConAportes`.
  - Método de servicio en `ProcesoPagoPrestamoService`.
  - En `aplicar`, el asiento único de AA6 cuando no hay cobro.
  - Fecha de afectación con las mismas reglas que la precancelación directa (`API-FECHA-AFECTACION-COBRO.md` §2bis).
- **Ítem 3 — defecto previo.**
  - `editarYReenviarCobro` (`CobroCreditoServiceImpl:427-458`) borra las DCBC sin borrar sus DAPR y no vuelve a persistir `aportes`. Hoy ya afecta a la precancelación mixta, y con este cambio afectaría también al abono.
- **Ítem 4 — la bandeja ve los aportes.**
  - `GET /cbcr/getId` devuelve los aportes de cada línea. Hoy contabilidad aprueba un depósito sin ver que el abono es mayor.

### Frontend (`omen-saa-1-fe`) — solo `crd`
- **Diálogo de abono a capital:**
  - Agregar el reparto efectivo/aportes copiado de `precancelacion-dialog`.
  - Hacer el respaldo condicional.
  - Ruteo: con efectivo, CBCR ABONO_CAPITAL; si es 100% aportes, el endpoint nuevo.
  - Recibo para el caso directo.
  - Corregir el comentario `:30-31`.
- **Modelo:** actualizar el comentario de `DetalleCobroCredito.aportes` (`cobro-credito.ts`).
- **Bandeja de contabilidad:** mostrar los aportes consumidos cuando el BE los devuelva (ítem 4).

## 6. Fuera de alcance
- Renombrar `CRD.DAPR`.
- Reservar el saldo de aportes al registrar (AA8 cubre ese caso con el rechazo automático).
- Pagar cuotas con aportes mixto. Hoy `pagarConAportes` es 100% aportes y no cambia.

---

## 7. Implementación — 2026-10-12

| Parte | Commit | Verificado |
|---|---|---|
| Contrato y diseño | saaBE `616cae9d`, `c550fb0c`, `9623168c`; espejo saaFE `4f5a460` | — |
| BE, ítems 1–4 | saaBE `8e6766dd` | `mvn -q compile` (árbitro) |
| BE, ajustes 1 y 2 | ver el commit que sigue a `8e6766dd` | `mvn -q compile` (árbitro) |
| FE, ítems 1–3 | saaFE `5bf4376` | `ng build --configuration development` (árbitro) |

**Nada se probó en ejecución.** Los asientos se cuadraron a mano para cuatro casos: 300+200; borde 50+450; 100+300 sin capital futuro; y 100% depósito, que queda idéntico a hoy.

### 7.1 Desvíos aprobados respecto del §5
- El servicio del camino directo vive en `AbonoCapitalPrestamoService.abonarConAportes`, no en `ProcesoPagoPrestamoService`. `AbonoCapital` ya depende de `ProcesoPago`, y ponerlo al revés creaba un ciclo de EJB.
- El pre-chequeo de AA8 usa métodos que **no lanzan** (`motivoDesgloseAportesInvalido`, `motivoAbonoInviable`). Ver H83.
- `calcularCapitalFuturoDelCobro` devuelve `{efectivo, tramoAbiertoPagadoConAportes}`. El bruto dice explícitamente si hubo corrida (ajuste 1).
- `lineasCierreApertura` quedó expuesto en `ContabilidadPrestamoService`, como la única copia de la resolución de D 2.3.02.10 / H 1.4.05.10. El camino directo y el CBCR la comparten.

### 7.2 Hallazgos
- **H83 — Un rechazo «automático» que atrapa un `IncomeException` cruzando el proxy EJB no persiste.**
  - `IncomeException` es `@ApplicationException(rollback = true)`. Al cruzar el proxy marca la transacción como rollback-only **aunque se atrape**.
  - El RECHAZADO que se graba después se pierde al hacer commit: el cobro queda APROBADO y el REST da error.
  - La precancelación mixta lo tenía así desde el 2026-08-30. Se corrigió en el ajuste 2.
  - **Por qué costaba verlo:** el código «se lee» correcto (try/catch → rechazar) y nunca se ejecutó, porque no hubo precancelaciones mixtas (`sql/321`).
  - **Regla:** dentro de un método transaccional, una validación que debe terminar en un estado grabado se consulta con un método que devuelve el motivo, nunca con uno que lanza.
- **H84 — Un 0,0 centinela que significaba dos cosas.**
  - En el cálculo del capital futuro, «0» significaba a la vez «no hay corrida de apertura» y «no queda capital futuro».
  - En el segundo caso, la apertura pagada con aportes quedaba sin cerrar.
  - Lo encontró el árbitro al revisar la entrega y se corrigió en el ajuste 1.
- **El reenvío de un cobro no copia `idAcuerdo`** de sus líneas, y eso afecta a ACUERDO_CONDONACION. Lo vio el BE y no se tocó porque está fuera de este frente. Queda como deuda.

### 7.3 Pruebas que faltan (local, con el WAR y el FE desplegados juntos)
1. **Abono mixto** con depósito 300 + aportes {11: 200}, en un mes con corrida previa. Después de procesar: reparto con Debe positivo y chico; aportes en el Debe del definitivo; transitoria neta en 0; saldo de aportes −200.
2. **Borde:** depósito 50 + aportes 450, con capital futuro < 450. En el definitivo debe aparecer el par D 2.3.02.10 / H 1.4.05.10 por la diferencia.
3. **AA8:** registrar y aprobar un abono mixto, gastar el aporte por otra vía y procesar. El cobro debe quedar RECHAZADO por SISTEMA y **seguir así al recargar**.
4. **100% aportes directo:** un solo asiento. Luego `POST /prst/anularOperacion`: asiento anulado, aporte restituido y tabla restaurada. Probar también sin fecha de afectación (400) y con saldo insuficiente (422).
5. **Reenvío** de un abono mixto rechazado, con los aportes corregidos.
6. **`GET /cbcr/getId`** de un cobro mixto y de uno sin aportes (`[]`), y la tabla «Aportes consumidos» en la bandeja de contabilidad.
7. **Precancelación mixta** con aportes mayores que lo exigible, en un mes con corrida. Antes no se podía procesar.
