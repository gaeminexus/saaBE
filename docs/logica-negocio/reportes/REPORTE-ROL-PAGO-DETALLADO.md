# Reporte — Rol de pagos detallado (`RPRT_ROLL_DTLL`)

**Módulo:** `rhh` · **Equipo:** omen-saa-3 (frente 23) · **Diseño:** 2026-11-09 · **Estado:** diseño, sin implementar

Pedido del usuario (2026-11-09): un reporte consolidado **con el detalle de valores**, al estilo de la
«Liquidación rol de pagos» que mandó como muestra (tres bloques: INGRESOS, EGRESOS, PROVISIONES
BENEFICIOS; una fila por colaborador, una columna por concepto, fila TOTAL por bloque), **con los
estilos visuales de `RPRT_ROLL_CNSL`**, y que aparezca en la pantalla **RRHH › Reportes de nómina**.

## 1. Cómo se pide (no hay endpoint nuevo)

```
POST /SaaBE/rest/rprt/generar
{ "modulo": "rhh", "nombreReporte": "RPRT_ROLL_DTLL", "formato": "pdf" | "excel",
  "parametros": { "P_PRDN_CODIGO": <PRDNCDGO>, "P_USUARIO": "<usuario de sesión>" } }
```

Mismos parámetros que `RPRT_ROLL_CNSL`: `P_PRDN_CODIGO` (`java.lang.Long`, obligatorio),
`P_IMAGEN` (`java.awt.Image`, lo inyecta `ReporteServiceImpl`), `P_USUARIO` (`java.lang.String`).
El reporte **tiene `<query>`**, así que se llena por la conexión JDBC de `generarReporte` (la trampa
del CLAUDE.md sobre reportes sin `<query>` no aplica).

## 2. Decisión central: columnas por concepto, NO fijas

La muestra trae columnas fijas (horas extra 1.25, consumos, multas…) porque el sistema de donde
salió tiene esos conceptos cableados. **En SAA los conceptos son parametrizables (`RHH.CPNM`)**:
cablear columnas haría que un concepto nuevo no salga en el reporte, o que salga una columna
siempre en cero. Por eso las columnas de INGRESOS y EGRESOS **salen de los datos del período**.

Se descartó un `crosstab` de Jasper: no hay ningún precedente en el repositorio, el control de
estilos y anchos es peor, y con muchos conceptos pagina las columnas en hojas aparte. En su lugar,
**el SQL pivotea los conceptos a 12 «casillas» fijas** (`V1`…`V12`) con su encabezado (`H1`…`H12`)
en cada fila. El layout es uno solo para los tres bloques.

### Reglas de las casillas (bloques 1 y 2)

| Regla | Detalle |
|---|---|
| Qué es un concepto | Clave `CPNMCDGO` del renglón; si el renglón no tiene concepto (`CPNMCDGO` nulo), la clave es `RNGLDSCR`. |
| Qué entra | Bloque 1 = `RHH.RNGL` con `RNGLTPCN = 1` (INGRESO); bloque 2 = `RNGLTPCN = 2` (EGRESO). Constantes en `com.saa.rubros.RhhTipoConceptoNomina`. |
| Qué conceptos tienen columna | Solo los que tienen **algún valor distinto de 0 en el período** (`SUM(ABS(RNGLVLRO)) > 0`). Un concepto sin movimiento no ocupa columna. |
| Orden | `CPNMORDN` (nulo al final), luego la clave. |
| Encabezado | `NVL(CPNMABRV, CPNMNMBR)`; si no hay concepto, `RNGLDSCR`. En mayúsculas. |
| Más de 12 conceptos | Casillas 1–11 = los 11 primeros; la casilla 12 se titula **«OTROS»** y suma el resto. Nunca se pierde un valor. |
| Menos de 12 | Las casillas sobrantes quedan **en blanco** (encabezado nulo, valor nulo, total nulo). No se imprime «0.00» en una casilla sin concepto. |
| Colaborador sin ese concepto | `0` (no blanco) en una casilla que sí tiene concepto. |

**Fondo de reserva acumulado en el IESS** (`CNTEFRMD = 2`, criterio e3-06 del consolidado): no es
renglón, está en `RHH.PVNM` tipo 4 estado 1. Igual que `RPRT_ROLL_CNSL`/`RPRT_ROLL_INDV`, se agrega
como **un concepto más al final** de los dos bloques: «FONDOS RESERVA IESS» en ingresos y
«FONDOS RESERVA DEPOSITADOS IESS» en egresos (se netea, no mueve el neto). Si nadie del período lo
tiene, no ocupa columna. Copiar la subconsulta `FR_IESS_VALOR` tal cual de `RPRT_ROLL_CNSL.jrxml`.

### Columnas finales de cada bloque

| Bloque | `TOT` | `NETO` |
|---|---|---|
| 1 INGRESOS | «TOTAL INGRESOS» = suma de las casillas de la fila | vacío |
| 2 EGRESOS | «TOTAL EGRESOS» = suma de las casillas de la fila | «NETO A PAGAR» = `NMNANETO` + `SUM(RNGLVLRO)` de los renglones cuyo concepto tiene `CPNMROLM IN (34, 35)` (mismo cálculo que `NETO_A_PAGAR` de `RPRT_ROLL_INDV`, pero con `NMNANETO` en vez de `RLPGNETO`: un período sin rol emitido igual tiene nómina) |
| 3 PROVISIONES | «TOTAL» = casillas 2 a 9 (la 1 es referencia, no provisión) | vacío |

`TOT` es la suma **de lo que se imprime**, para que la fila cuadre a la vista. Cuadre esperado (lo
verifica `e3-15`, consulta 4): `TOT(1) = NMNATING + FR_IESS`, `TOT(2) = NMNATDSC + FR_IESS`,
`TOT(1) − TOT(2) = NMNANETO`. Si no cuadra, **no se corrige en el reporte**: se reporta al árbitro.

## 3. Bloque 3 — Provisiones y aportes patronales (casillas fijas)

| Casilla | Encabezado | Origen |
|---|---|---|
| 1 | TOTAL INGRESOS | `TOT` del bloque 1 de esa fila (referencia) |
| 2 | DÉCIMO TERCERO | `PVNM` tipo 1 |
| 3 | DÉCIMO CUARTO | `PVNM` tipo 2 |
| 4 | VACACIONES | `PVNM` tipo 3 |
| 5 | FONDOS DE RESERVA | `PVNM` tipo 4 |
| 6 | APORTE PATRONAL IESS | `NMNA.NMNAAPPT` |
| 7 | IECE / SECAP | `NMNA.NMNAIESC` |
| 8 | JUBILACIÓN PATRONAL | `PVNM` tipo 6 |
| 9 | DESAHUCIO | `PVNM` tipo 7 |
| 10–12 | (en blanco) | — |

- `PVNM` siempre con `PVNMESTD = 1` y `PRDNCDGO` del período, sumado por empleado (constantes en
  `com.saa.rubros.RhhTipoProvision`).
- **`PVNM` tipo 5 (aporte patronal) NO se usa**: el aporte sale de `NMNAAPPT`. Si existieran filas
  tipo 5 se contaría dos veces. `e3-15` consulta 3 mide si hay.
- El fondo de reserva acumulado (`CNTEFRMD = 2`) aparece en el bloque 3 **y** en ingresos/egresos:
  en el 3 como costo provisionado, en 1/2 como paso que se netea. Es el mismo valor visto desde dos
  lados, no un doble conteo de ningún total.
- El título del bloque es **«PROVISIONES Y APORTES PATRONALES»** (la muestra dice «PROVISIONES
  BENEFICIOS» pero mete el IESS patronal, que no es provisión).

## 4. Filas

- Una fila por nómina del período (`RHH.NMNA` del `PRDNCDGO`), **en los tres bloques**, aunque el
  colaborador no tenga renglones en un bloque. Mismas filas y mismo orden que `RPRT_ROLL_CNSL`:
  apellidos, nombres, `NMNACDGO`. No se filtra por estado de la nómina (igual que el consolidado).
- Columnas fijas a la izquierda: `#` (número de fila dentro del bloque), IDENTIFICACIÓN, EMPLEADO,
  DÍAS (`NMNADITR`, sin totalizar).
- La muestra trae DEPARTAMENTO; aquí **no** va (ASOPREP usa un solo centro de costo y el ancho se
  necesita para los conceptos). Si el usuario lo pide, es `MPLD → CNT.CNCS`.

## 5. Diseño visual

- Copiar de `RPRT_ROLL_CNSL.jrxml`: página A4 horizontal (842×595, márgenes 25/25/20/20), **los
  mismos `<style name=…>`**, banda de título con logo `P_IMAGEN`, colores de encabezado, formato
  numérico `#,##0.00`, pie de página. Sintaxis **compacta** de JR 7 (`<element kind=…>`). **Sin
  `fontName="Tahoma"`** (pierde la negrita en el PDF).
- Título: «ROL DE PAGOS DETALLADO — {MES EN LETRAS} {AÑO}» y, debajo, «N° asiento: {ASNTNMRO}» del
  asiento de la nómina (`PRDN.PRDNASNT` → `CNT.ASNT.ASNTCDGO`, imprimir `ASNTNMRO`; si es nulo,
  «sin contabilizar»). Empresa, período desde–hasta, usuario y fecha de impresión como el consolidado.
- Un **grupo de Jasper por `SECCION`** (1, 2, 3): el encabezado de grupo lleva la franja con el
  nombre del bloque y la fila de encabezados de columna (`H1`…`H12`, «TOTAL…», «NETO A PAGAR»),
  con `isReprintHeaderOnEachPage` y altura mínima para no dejar un encabezado huérfano al pie. El
  pie de grupo es la fila TOTAL: suma de cada casilla, `TOT` y `NETO` (variables con `resetType` del
  grupo). Una casilla sin concepto suma nulo y queda en blanco (`blankWhenNull`).
- Anchos (`columnWidth` 792): `#` 14, IDENTIFICACIÓN 52, EMPLEADO 120, DÍAS 26 → 212; quedan 580
  para 12 casillas + `TOT` + `NETO` ≈ 41 c/u. Detalle a 6–6,5 pt; encabezados hasta 3 líneas.
- Formato EXCEL: debe salir legible en una sola hoja (lo usa el mismo selector de formato).

## 6. Frontend

Pantalla `saaFE/src/app/modules/rrh/forms/procesos/reportes-nomina/`. No cambia nada del
componente salvo el catálogo:

- `procesos/descarga-reporte.ts`: `ReportesNomina.ROL_DETALLADO = 'RPRT_ROLL_DTLL'` (y su
  expectativa en `descarga-reporte.spec.ts`).
- `reportes-nomina.component.ts`, `REPORTES[]`: tarjeta nueva **inmediatamente después de «Rol
  consolidado»**: título «Rol detallado», descripción «Cada colaborador con todos sus ingresos,
  egresos y provisiones del período, concepto por concepto.», icono `view_list`, archivo
  `rol-detallado`. Pide período, como las otras cuatro.

## 7. Verificación exigida

1. `compilar-jasper.bat src/main/resources/rep/rhh/RPRT_ROLL_DTLL.jrxml` y
   `verificar-fill-jasper.bat src/main/resources/rep/rhh/RPRT_ROLL_DTLL.jasper`; los dos en el commit.
2. Si hay una base con datos de RRHH alcanzable: PDF real de un período por `generarReporte`, y
   comparar los TOTAL de cada bloque contra `RPRT_ROLL_CNSL` del mismo período (ingresos, descuentos,
   neto) y mirar el PDF (negritas, encabezados, casillas en blanco).
3. Si no hay base: decirlo. El usuario corre `rhh/sql/e3-15` y compara contra el PDF.

## 8. Hallazgos al diseñar

- `RHH.PVNM.PVNMESTD` no tiene rubro de estados; el motor solo escribe 1
  (`ProcesoNominaServiceImpl:1809`). El consolidado filtra `= 1` y el reporte de provisiones no
  filtra nada: dos criterios distintos para la misma tabla. Aquí se usa `= 1`.
- El reporte de provisiones existente (`RPRT_PRVS_PRDO`) lista `PVNM` tipo 5 si existiera; la
  tarjeta del FE dice «el aporte patronal no aparece: no se provisiona». `e3-15` lo mide.
