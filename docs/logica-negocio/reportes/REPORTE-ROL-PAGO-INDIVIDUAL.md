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

## Fondos de reserva acumulados en el IESS (e3-06, 2026-09-30)

Un colaborador con `ContratoEmpleado.modalidadFondosReserva = ACUMULADO_EN_EL_IESS` (2,
`CNTEFRMD`, hoy solo Ximena Viteri) no recibe el fondo de reserva mensualizado: el motor
(`ProcesoNominaServiceImpl:988-997`) no genera un renglón de `RHH.RNGL` para ese concepto, genera
una **provisión** en `RHH.PVNM` (`PVNMTPPR = 4`, `RhhTipoProvision.FONDOS_DE_RESERVA`) por
período y empleado. Sin renglón, el rol no tenía ningún rastro del valor.

**El reporte, no el motor, agrega dos filas sintéticas** cuando hay una `PVNM` de tipo 4 con
`PVNMESTD = 1` (activa) para el período y el empleado de la nómina, y el contrato de esa nómina
tiene `CNTEFRMD = 2`:

- **Ingresos:** «Fondos de reserva (acumulados en el IESS)», con `PVNMVLOR`.
- **Descuentos:** «Fondos de reserva depositados en el IESS», el mismo `PVNMVLOR`.

Las dos van al final de su columna (`ORDEN_ORIG = 999999999` en el `ROW_NUMBER()` de los CTE
`ING`/`DSC`). **Se netea a propósito**: deja constancia de que se le pagó (ingreso) y de que se
retuvo para depositarlo en la planilla de fondos de reserva del IESS (descuento), sin mover un
centavo del neto real — es plata que nunca pasa por la cuenta del colaborador. `TOTAL_INGRESOS` y
`TOTAL_DESCUENTOS` del pie (`RLPGTTIN`/`RLPGTTDS`) suman el mismo valor en los dos, para cuadrar
con las filas nuevas; `NETO_A_PAGAR` no cambia.

**Cómo se resuelve el contrato de la nómina**: `RHH.NMNA.CNTECDGO` (FK `NOT NULL` a `RHH.CNTE`,
`Nomina.getContrato()` del lado Java) — join directo, no hace falta ninguna resolución indirecta.

Si no hay provisión de fondo de reserva para el período (sin derecho todavía, causal de salida
antes del aniversario, o modalidad `MENSUALIZADO`, donde el fondo **ya** sale como renglón de
ingreso normal), el reporte sale exactamente igual que antes de este cambio.

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
