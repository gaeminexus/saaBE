# Reporte — Rol de pagos consolidado (`RPRT_ROLL_CNSL`)

**Módulo:** `rhh` · **Fase:** 5 · **Fecha:** 2026-08-19

Todos los empleados de un período en una sola tabla, con totales. Es el reporte de control del
responsable de nómina: el que se revisa antes de aprobar y el que se archiva con el período.

## Cómo se pide

```
POST /SaaBE/rest/rprt/generar
{
  "modulo": "rhh",
  "nombreReporte": "RPRT_ROLL_CNSL",
  "formato": "pdf",
  "parametros": { "P_PRDN_CODIGO": 1, "P_USUARIO": "MIKE" }
}
```

## Parámetros

| Parámetro | Tipo | Obligatorio | De dónde sale |
|---|---|---|---|
| `P_PRDN_CODIGO` | `java.lang.Long` | Sí | `RHH.PRDN.PRDNCDGO` |
| `P_IMAGEN` | `java.awt.Image` | No | Lo inyecta `ReporteServiceImpl` |
| `P_USUARIO` | `java.lang.String` | No | Se imprime en la cabecera |

## Qué muestra

Una fila por nómina del período: cédula, empleado, días trabajados, sueldo base, total de
ingresos, aporte personal, retención de IR, total de descuentos, **neto**, total patronal y
costo del empleador. Ordenado por apellidos.

El pie totaliza ingresos, descuentos, neto, patronal y costo, y cierra con una frase que resume
el neto pagado y el costo total.

**`FONDOS_RESERVA` (e3-06, 2026-09-30)**: para un colaborador `MENSUALIZADO` sale de
`NMNA.NMNAFNRS` (un renglón normal); para uno `ACUMULADO_EN_EL_IESS` (`ContratoEmpleado.
modalidadFondosReserva`, `CNTEFRMD=2` — hoy solo Ximena Viteri) `NMNAFNRS` es 0 porque el motor
no genera renglón para ese caso (`ProcesoNominaServiceImpl:988-997`), así que la columna trae el
valor de `RHH.PVNM` (tipo 4, activa) para ese período y empleado. `TOTAL_INGRESOS` y
`TOTAL_DESCUENTOS` suman ese mismo valor en los dos (se netea, igual criterio que el rol
individual, `REPORTE-ROL-PAGO-INDIVIDUAL.md`); `NETO` no cambia.

## Una columna que conviene entender

**`COSTO_EMPLEADOR` = total de ingresos + total patronal.** No es el neto más los aportes: el
neto ya descuenta el aporte personal y la retención, que son dinero del empleado retenido por la
empresa, no ahorro. La empresa desembolsa los ingresos completos —parte al empleado, parte al
IESS y al SRI por su cuenta— más su propio aporte patronal.

**Las provisiones no entran en esta columna.** Son costo del mes pero no desembolso del período,
y van en su propio reporte. Sumarlas aquí haría que el consolidado no cuadrara contra la orden
de pago.

**Excepción deliberada, e3-06 (2026-09-30): el fondo de reserva `ACUMULADO_EN_EL_IESS` sí entra,
aunque nace como provisión (`RHH.PVNM`, igual mecanismo que un décimo no mensualizado).** No es
una inconsistencia con el párrafo de arriba: a diferencia de un décimo provisionado —que no
mueve caja hasta que se paga, meses después—, el fondo de reserva acumulado **sí es un
desembolso real del período**: la empresa lo declara y lo paga mensualmente al IESS por la
planilla de fondos de reserva (`RhhTipoPlanillaIess.FONDOS_DE_RESERVA`,
`PlanillaIessServiceImpl`), igual que paga el aporte patronal. Es dinero que sale de la empresa
ese mismo mes, solo que no pasa por la cuenta del colaborador — por eso sí cuadra contra la orden
de pago, y por eso entra en `COSTO_EMPLEADOR`.

## Decisiones de construcción

- **Lee `NMNA`, no `RLPG`.** El consolidado sirve para revisar antes de aprobar, y antes de
  aprobar todavía no hay roles emitidos. El número de rol se trae con `LEFT JOIN` y muestra
  «(sin rol emitido)» cuando falta, que es a la vez el estado normal antes de aprobar y la
  señal de un rol que no se generó después.
- Una sola consulta plana, `NVL` sistemático, alias en `MAYUSCULA_SNAKE`.
- Apaisado: son once columnas numéricas y en vertical no entran sin reducir la fuente por
  debajo de lo legible.
