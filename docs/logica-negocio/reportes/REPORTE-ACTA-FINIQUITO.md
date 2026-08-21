# Reporte — Acta de finiquito (`RPRT_ACTA_FNQT`)

**Módulo:** `rhh` · **Fase:** 9 · **Fecha:** 2026-08-19

El documento que las dos partes firman y que se registra en el SUT del Ministerio del Trabajo.

## Cómo se pide

```
POST /SaaBE/rest/rprt/generar
{ "modulo": "rhh", "nombreReporte": "RPRT_ACTA_FNQT", "formato": "pdf",
  "parametros": { "P_LQDC_CODIGO": 1, "P_USUARIO": "MIKE" } }
```

| Parámetro | Tipo | Obligatorio |
|---|---|---|
| `P_LQDC_CODIGO` | `java.lang.Long` | Sí — `RHH.LQDC.LQDCCDGO` |
| `P_IMAGEN` / `P_USUARIO` | `Image` / `String` | No |

## Qué muestra

Cabecera con trabajador, cédula, rango de la relación laboral, años de servicio, causal con su
artículo y última remuneración. Detalle: un rubro por fila de `TMLQ`, con base, días o años y
valor, en el orden de presentación. Totales y las dos firmas.

## Tres decisiones

- **Un rubro en cero no se imprime.** El finiquito lista lo que corresponde, y una línea de
  desahucio en cero solo confunde a quien firma.
- **Un neto negativo se muestra en naranja y con su importe**, no se esconde: significa que el
  trabajador adeuda a la empresa, y el acta tiene que decirlo.
- **El número de acta del SUT sale si existe**; si no, la cabecera dice «Pendiente de registro en
  el SUT», que es el estado normal antes de presentarla.
