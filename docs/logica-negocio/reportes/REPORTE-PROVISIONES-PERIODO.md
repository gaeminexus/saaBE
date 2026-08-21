# Reporte — Provisiones del período (`RPRT_PRVS_PRDO`)

**Módulo:** `rhh` · **Fase:** 5 · **Fecha:** 2026-08-19

Las filas de `RHH.PVNM` de un período, agrupadas por tipo de provisión, con subtotal por grupo.
Es el soporte del asiento de provisiones de la fase 6.

## Cómo se pide

```
POST /SaaBE/rest/rprt/generar
{
  "modulo": "rhh",
  "nombreReporte": "RPRT_PRVS_PRDO",
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

Agrupado por `PVNMTPPR` (rubro 206), con subtotal y conteo de empleados por grupo, y total
general al pie. Por fila: cédula, empleado, concepto, base de cálculo y valor.

## Lo que el lector suele preguntar

El pie lo explica en el propio reporte, porque es la duda recurrente:

- **Décimos y fondos de reserva aparecen aquí solo en modalidad ACUMULADO.** En MENSUALIZADO se
  pagan como renglón del rol y no generan provisión. Por eso un período puede tener empleados
  sin ninguna fila en este reporte.
- **Vacaciones se provisionan siempre**, con independencia de cualquier modalidad. Es la única
  provisión sin renglón equivalente en el rol.
- **El aporte patronal no se provisiona.** El tipo 5 del rubro 206 existe pero queda sin uso: el
  asiento de rol ya lo registra completo y provisionarlo contaría el costo dos veces.
- **Jubilación patronal y desahucio** no salen del cálculo mensual sino de
  `ProvisionActuarialService`, y solo si `CFNMAPJP` / `CFNMAPDS` están en `'S'`.

## Decisiones de construcción

- El grupo `G_TIPO` va sobre `PVNMTPPR`, y el `ORDER BY` del SQL empieza por esa misma columna:
  JasperReports agrupa por cambio de valor, no ordena, así que un `ORDER BY` distinto rompería
  el agrupamiento en silencio.
- `LEFT JOIN` a `CPNM`: una provisión cuyo concepto no se localizó por rol imprime «(sin
  concepto asignado)» en vez de desaparecer. Si eso aparece, falta un `CPNMROLM` de los roles
  17 a 22.
