# Reporte — Formulario 107 individual (`RPRT_F107_INDV`)

**Módulo:** `rhh` · **Fase:** 9 · **Fecha:** 2026-08-19

Comprobante de retenciones en relación de dependencia, uno por empleado y ejercicio.

## Cómo se pide

```
POST /SaaBE/rest/rprt/generar
{ "modulo": "rhh", "nombreReporte": "RPRT_F107_INDV", "formato": "pdf",
  "parametros": { "P_MPLD_CODIGO": 1, "P_ANIO": 2026, "P_USUARIO": "MIKE" } }
```

| Parámetro | Tipo | Obligatorio |
|---|---|---|
| `P_MPLD_CODIGO` | `java.lang.Long` | Sí |
| `P_ANIO` | `java.lang.Integer` | Sí |
| `P_IMAGEN` / `P_USUARIO` | `Image` / `String` | No |

## De dónde salen los datos

**De `RNGL`, no de una tabla propia.** Agrupa los renglones del ejercicio por el casillero que
cada concepto declara en `CPNMF107`. Es la decisión del §5.1 del orden 5: las salidas no
duplican datos, porque regenerar es determinista y una segunda copia solo sería otra verdad que
mantener.

**Un concepto sin casillero no aparece.** No es un olvido del reporte: es que ese concepto no
forma parte de la declaración. Si algo esperado falta, lo que falta es el `CPNMF107` del
concepto, y el bloque de «sin información» lo dice con esas palabras.

## Registro de la generación

El reporte no escribe en `SLOF` por sí mismo —se pide por el endpoint genérico de reportes—, así
que la pantalla debe llamar a `POST /rest/slof/registrarGeneracion` con
`tipoSalida = 2 (FORMULARIO_107)`, el año y el empleado, para dejar constancia.
