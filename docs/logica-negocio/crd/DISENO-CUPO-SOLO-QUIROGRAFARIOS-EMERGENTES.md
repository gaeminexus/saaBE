# DISEÑO — El cupo de la cuenta individual cuenta solo quirografarios y emergentes

omen-saa-1-arb · 2026-11-08 · Pedido del usuario (reporte de crédito). Ejecutor: omen-saa-1-be.

## Pedido
> «en la novación de un crédito no se por qué dice que no cumple la cuenta individual, no se si está tomando en cuenta
> el saldo capital del hipotecario, si es así no debería tomar en cuenta eso ya que las garantías de cuentas
> individuales es únicamente para los quirografarios y emergentes»

## Hoy (verificado, `EvaluacionOtorgamientoServiceImpl.evaluar` ~76-128)
`saldoCapitalVigentes` suma el capital pendiente de TODOS los vigentes (2, 8, 10, 11) del partícipe — también
hipotecarios y prendarios — y el cupo exige `saldoCapitalVigentes + monto ≤ cuenta individual`. El cupo se evalúa
para cualquier producto nuevo.

## Nuevo
1. **El cupo solo suma el capital pendiente de préstamos quirografarios y emergentes** (código Petro del producto =
   `ParametroOtorgamiento.CODIGO_PETRO_QUIROGRAFARIO` / `CODIGO_PETRO_EMERGENTE`; el ejecutor verifica que los
   productos de novación/renovación de quirografarios tengan ese código o define la familia por `TPPR`, y lo reporta).
2. **El cupo solo se evalúa si el préstamo nuevo es quirografario o emergente.** Si el producto no se conoce (simulador
   de crédito nuevo sin producto), se evalúa como hoy pero con la suma del punto 1. Hipotecario/prendario nuevo →
   `cupoOk = null` («no aplica»), no bloquea.
3. **Capacidad de pago no cambia:** las cuotas vigentes siguen sumando todos los préstamos (todo descuenta del rol).
4. El DTO separa los dos saldos: `saldoCapitalVigentes` (todos, informativo, como lo muestra el panel y la solicitud
   V1) y uno nuevo `saldoCapitalCupo` (solo PQ/PE, el que usa la regla); el mensaje del cupo cita el nuevo. Avisar al
   FE el campo nuevo (panel «Evaluación del otorgamiento» y simulador de novación).
5. La solicitud V1 (`RPRT_PRST_SLCT`) no cambia (sus cifras son informativas).
6. Docs: `crd/API-CICLO-OTORGAMIENTO.md` (§ evaluación) y el diseño `DISENO-SOLICITUD-V1-...` §B2.
