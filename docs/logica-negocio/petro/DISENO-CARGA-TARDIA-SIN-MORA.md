# Carga Petro/ARCH procesada después del día 1 — sin mora sobre lo que ya se descontó

**Estado: DISEÑO con decisiones abiertas (§5).** omen-saa-1-arb, 2026-10-31, a pedido del usuario:
«que puedan procesar el archivo con fecha posterior al 1; como ese dinero ya se les descontó en el
rol, se asume que pagaron a tiempo esas cuotas, y para esos préstamos se debe eliminar la mora
generada del 1 en adelante, salvo que no se les haya descontado en los archivos».

Fuente: investigación de solo lectura del código + verificación del árbitro (✔).

## 1. Cómo funciona hoy

- **Mora diaria** (`ProcesoMoraPrestamoTimer`, `@Schedule` a las 02:00, ACTIVO desde el 2026-09-09):
  - Entra toda cuota con `DTPRESTD NOT IN (4,7)` y `fechaVencimiento < hoy` de un préstamo en estado
    2, 8 u 11. **Sin días de gracia.**
  - Fórmula: `capital × (tasa/100/360) × días` desde el vencimiento.
  - Se guarda **acumulada** y se recalcula entera cada noche (`mora`/DTPRMRAA, `diasMora`,
    `saldoMora`, `total = totalBase + mora`). No hay filas por día.
  - Pone la cuota en EN_MORA (5) y el préstamo de 2 a 11. **No genera asientos**: la mora llega a
    contabilidad por el cierre de cartera (MVIC tipo 1 provisión, tipo 5 devengo).
- **La carga** ✔ fecha el pago con el último día del mes de afectación a las 23:59:59
  (`crearContextoPagoCarga` ~4070), **pero no recalcula la mora a esa fecha**: el motor
  (`MotorPagoPrestamoServiceImpl`) cobra la mora **guardada** y en este orden: seguro → desgravamen
  → **mora** → interés vencido → interés → capital. No hay ninguna llamada a
  `recalcularMoraALaFecha*` en la carga ✔ (solo la usa la caja, `CobroCreditoServiceImpl` ~946).
- **Efecto si se procesa después de las 02:00 del día 1:** la fase 2 valida contra el total SIN
  mora (no salta novedad), pero el motor gasta parte del descuento en la mora de esos días, al
  capital le falta justo eso, la cuota queda **PARCIAL**, el préstamo sigue EN_MORA (11) y la
  generación del mes siguiente cobra el remanente más la mora que siguió corriendo. Es el «H42» que
  la caja ya resolvió y la carga no.

## 2. Lo que ya existe y se reusa

- `ProcesoMoraPrestamoService.recalcularMoraALaFechaDePagoDetalle(idPrestamo, fechaPago)`:
  - recalcula la mora de cada cuota a `fechaPago` con la misma fórmula;
  - las que vencen en o después de esa fecha quedan con mora 0 y salen de EN_MORA;
  - salta las cuotas con un pago posterior;
  - devuelve `[idCuota, moraAnterior, moraNueva]`.
  - Es idempotente.
- Reversos contables de la caja para «cobro tardío» cuando el cierre de cartera ya corrió:
  - `reversarExcesoProvisionPorCobroTardio` (MVIC tipo 3);
  - `reversarExcesoDevengoPorCobroTardio` (tipo 6);
  - la reclasificación de bandas (tipo 8).

## 3. Diseño

1. **Fecha efectiva** = último día del mes de afectación de la carga (la misma que ya usa el PGPR).
2. **Antes** de aplicar el pago de cada préstamo que la carga va a pagar, si
   `fechaEfectiva < hoy`: `recalcularMoraALaFechaDePagoDetalle(idPrestamo, fechaEfectiva)`. Va en
   las dos rutas: `aplicarPagoNormalConMotor` y `aplicarAfectacionManualConRegistroPago`. Si se
   hace después, la mora ya se comió el dinero.
3. **Lo no descontado conserva su mora sola:** las cuotas que el archivo no cubre, o la parte que
   no alcanzó, vuelven a tener mora la noche siguiente, porque el proceso diario la recalcula desde
   el vencimiento hasta hoy. No hace falta guardar historia.
4. **Estado del préstamo:** sin cambios. Con la mora correcta la cuota queda PAGADA y
   `regularizarPrestamoSiSinMora` lo devuelve a 2.
5. **Contabilidad:** si el cierre de cartera del mes de afectación ya corrió antes de procesar la
   carga, se generan los reversos tipo 3, 6 y 8 con la carga como origen («PETRO_TARDIO», idCarga)
   y fecha del mes de afectación, igual que hace la caja. Si el cierre no corrió, basta con corregir
   la mora.
6. **Concurrencia:** el timer de las 02:00 podría correr en medio de una carga larga y volver a
   escribir la mora de un préstamo ya procesado. A evaluar: saltar en el timer los préstamos de una
   carga EN PROCESO, o recalcular de nuevo al final.

## 4. Defectos encontrados en el camino

- **M1:** la mora se calcula sobre el capital ORIGINAL de la cuota aunque tenga capital pagado
  (`ProcesoMoraPrestamoServiceImpl` ~357): una cuota PARCIAL paga mora sobre capital ya cobrado.
- **M2:** la prueba 8 de `crd/PROCESO-DIARIO-INTERES-MORA.md` contradice sus §3/§4 (si el estado 8
  calcula mora o no).
- **M3:** queda por verificar en la base el día de vencimiento real de las cuotas. Que «la mora
  arranca el día 1» solo es cierto si vencen el último día del mes.

## 5. Decisiones del usuario

- **MA:** fecha efectiva = último día del mes de afectación. *Recomendado: sí.*
- **MB:** la mora posterior al corte de las cuotas viejas NO cubiertas también se recalcula al
  corte (vuelve sola esa noche si siguen impagas). *Recomendado: sí.*
- **MC:** si el cierre de cartera ya corrió, reversos tipo 3, 6 y 8 en un asiento separado por
  carga. *Recomendado: sí.*
- **MD:** sin límite de antigüedad para procesar tarde. *Recomendado: sí, igual que la caja.*
- **ME:** ¿se corrige M1 (mora sobre capital pendiente) en el mismo cambio o aparte? *Recomendado:
  aparte; cambia la mora de toda la cartera.*
