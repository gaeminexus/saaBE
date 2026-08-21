# Reporte — Planilla del IESS, control (`RPRT_IESS_CNTR`)

**Módulo:** `rhh` · **Fase:** 9 · **Fecha:** 2026-08-19

**Es el reporte de control, no el archivo de carga.** El formato del archivo del IESS sigue
pendiente del cliente (insumo 4 del §9 del maestro); esto es lo que sí se puede construir
mientras tanto, y sirve para lo que hace falta: cuadrar contra la planilla emitida.

## Cómo se pide

```
POST /SaaBE/rest/rprt/generar
{ "modulo": "rhh", "nombreReporte": "RPRT_IESS_CNTR", "formato": "pdf",
  "parametros": { "P_PRDN_CODIGO": 1, "P_USUARIO": "MIKE" } }
```

## Qué muestra

Agrupado por empleado —con su cédula, código de afiliación y base imponible— y dentro de cada
uno, una fila por código IESS del concepto (`CPNMIESS`), con subtotal por empleado y total del
período. Un concepto sin código IESS no aparece: no forma parte de la planilla.

## Su relación con el resumen de aportes

Los dos cuadran contra el IESS pero por vías distintas, y conviene usarlos juntos:

| Reporte | Cuadra por | Sirve para |
|---|---|---|
| `RPRT_APRT_RSMN` | Totales de aporte personal, patronal, IECE y SECAP | El importe global de la planilla |
| `RPRT_IESS_CNTR` | Código IESS de cada concepto | Encontrar **qué** concepto descuadra |

El primero dice si el total está bien; el segundo, dónde está la diferencia cuando no lo está.
