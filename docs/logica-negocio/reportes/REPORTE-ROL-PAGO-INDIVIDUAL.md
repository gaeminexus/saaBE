# Reporte — Rol de pago individual (`RPRT_ROLL_INDV`)

**Módulo:** `rhh` · **Fase:** 5 · **Fecha:** 2026-08-19

El documento que el empleado firma. Una hoja por empleado y período, con el desglose de
renglones, los totales y el espacio de firma.

## Cómo se pide

No tiene endpoint propio. Se solicita por el genérico que ya existe:

```
POST /SaaBE/rest/rprt/generar
{
  "modulo": "rhh",
  "nombreReporte": "RPRT_ROLL_INDV",
  "formato": "pdf",
  "parametros": { "P_RLPG_CODIGO": 17, "P_USUARIO": "MIKE" }
}
```

## Parámetros

| Parámetro | Tipo | Obligatorio | De dónde sale |
|---|---|---|---|
| `P_RLPG_CODIGO` | `java.lang.Long` | Sí | `RHH.RLPG.RLPGCDGO`. **No es el código de la nómina ni el del empleado** |
| `P_IMAGEN` | `java.awt.Image` | No | Lo inyecta `ReporteServiceImpl` si el frontend no lo envía |
| `P_USUARIO` | `java.lang.String` | No | Se imprime en la cabecera |

Nombres fijados con el frontend el 2026-08-19. `convertirTiposParametros` coacciona el JSON al
tipo declarado, así que `P_RLPG_CODIGO` puede llegar como número o como texto.

## Qué muestra

- **Cabecera:** empleado, identificación, sueldo base, días trabajados, base imponible del
  IESS, rango del período y fecha de emisión.
- **Detalle:** un renglón por fila de `RHH.RNGL`, ordenado por tipo de concepto y luego por
  `RNGLORDN`. El nombre sale de `CPNM.CPNMNMBR`, con respaldo en `RNGLDSCR` para los renglones
  sin concepto.
- **Totales:** los grabados en `RLPG`, no recalculados. Es deliberado: el rol es un documento
  emitido y debe mostrar lo que se emitió.
- **Aportes patronales:** en un bloque aparte, marcado como informativo. No afectan al neto.
- **Firma:** nombre, identificación y la leyenda de recepción.

## El control de cuadre

El pie compara la suma de los renglones contra los totales grabados en `RLPG`. Si difieren en
más de un centavo imprime un aviso: la nómina cambió después de emitir el rol y hay que
regenerarlo. Es la versión visible de `verificarIntegridad`, que hace lo mismo con el hash.

## Decisiones de construcción

- **Una sola consulta plana, sin subreportes**, según el patrón de `rep/crd/RPRT_CMPB_PGCT.jrxml`.
  La cabecera se repite en cada fila y el detalle la ignora; es lo que permite compilar en
  runtime con `JRJaninoCompiler` sin depender de recursos externos.
- **`LEFT JOIN` a `RNGL`**: un rol sin renglones sigue imprimiendo su cabecera en vez de dar
  «sin información». El `printWhenExpression` del detalle descarta la fila fantasma.
- **`NVL` sistemático** en todo importe, para que un nulo de base no imprima vacío ni rompa una
  expresión aritmética.
- Los literales del tipo de concepto (Ingreso, Descuento, Aporte patronal, Provisión) se
  resuelven en el `CASE` del SQL. **No son valores normativos**: son las etiquetas del rubro 179
  y su código alterno es estable.

## Verificación

Tras generar el rol de un período calculado, contrastar contra el caso de prueba de
`ESTADO-RRHH.md`: ocho renglones, ingresos 973,48, descuentos 75,60, neto 897,88, y en el bloque
patronal 75,60 · 89,20 · 8,00 · 97,20.
