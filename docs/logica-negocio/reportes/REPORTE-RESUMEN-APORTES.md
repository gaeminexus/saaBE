# Reporte — Resumen de aportes al IESS (`RPRT_APRT_RSMN`)

**Módulo:** `rhh` · **Fase:** 5 · **Fecha:** 2026-08-19

Aporte personal, patronal, IECE y SECAP por empleado de un período. **Es el reporte con el que
el cliente cuadra contra la planilla emitida por el IESS**, que es el criterio de aceptación 2
del documento maestro.

## Cómo se pide

```
POST /SaaBE/rest/rprt/generar
{
  "modulo": "rhh",
  "nombreReporte": "RPRT_APRT_RSMN",
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

Una fila por empleado: cédula, código de afiliación, nombre, base imponible, fondos de reserva,
aporte personal, aporte patronal IESS, IECE + SECAP y el total de los tres aportes. Totaliza al
pie.

**Los fondos de reserva van en columna aparte y no dentro de la base imponible**, porque el IESS
los recauda en su propia línea de la planilla. Meterlos en la base haría que el reporte no
cuadrara contra el documento con el que se compara, que es lo único que este reporte tiene que
hacer bien.

Los empleados con `CNTEAPRT = 'N'` —servicios profesionales sin relación de dependencia— salen
marcados «(no aporta)»: aparecen para que nadie los busque, pero no deben constar en la planilla.

## El control de descuadre

La consulta calcula `DIFERENCIA_PATRONAL = NMNATTPT − (NMNAAPPT + NMNAIESC)`, y el pie avisa si
alguna nómina difiere en más de un centavo.

Esa comprobación existe por un motivo concreto: hasta el 2026-08-19 el motor grababa en
`NMNAAPPT` el **total** de renglones patronales —97,20 en vez de 89,20 en el caso de prueba— y
dejaba `NMNAIESC` en nulo. Se corrigió repartiendo los tres campos por `CPNMROLM`
(`sumaPorRol`), pero **las nóminas calculadas antes de esa corrección conservan la cabecera
vieja**. El aviso las delata; se arreglan recalculando el período.

## Decisiones de construcción

- **Lee la cabecera de `NMNA`, no suma los renglones.** Es a propósito: así el reporte comprueba
  que la cabecera está bien escrita, que es justo lo que falló. Sumar renglones daría siempre el
  resultado correcto y ocultaría el defecto.
- `LEFT JOIN` a `CNTE` para la bandera de aporte: una nómina sin contrato enlazado sigue
  apareciendo, marcada como que no aporta.
- `NVL(e.MPLDCDAF, ' ')` — el código de afiliación es opcional en el maestro y no todos lo
  tienen cargado.
