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

## Una columna que conviene entender

**`COSTO_EMPLEADOR` = total de ingresos + total patronal.** No es el neto más los aportes: el
neto ya descuenta el aporte personal y la retención, que son dinero del empleado retenido por la
empresa, no ahorro. La empresa desembolsa los ingresos completos —parte al empleado, parte al
IESS y al SRI por su cuenta— más su propio aporte patronal.

**Las provisiones no entran en esta columna.** Son costo del mes pero no desembolso del período,
y van en su propio reporte. Sumarlas aquí haría que el consolidado no cuadrara contra la orden
de pago.

## Decisiones de construcción

- **Lee `NMNA`, no `RLPG`.** El consolidado sirve para revisar antes de aprobar, y antes de
  aprobar todavía no hay roles emitidos. El número de rol se trae con `LEFT JOIN` y muestra
  «(sin rol emitido)» cuando falta, que es a la vez el estado normal antes de aprobar y la
  señal de un rol que no se generó después.
- Una sola consulta plana, `NVL` sistemático, alias en `MAYUSCULA_SNAKE`.
- Apaisado: son once columnas numéricas y en vertical no entran sin reducir la fuente por
  debajo de lo legible.
