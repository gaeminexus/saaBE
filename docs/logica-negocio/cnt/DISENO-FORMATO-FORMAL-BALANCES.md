# Formato formal de los balances contables — `rep/cnt/`

**2026-09-08 · equipo `omen-saa-2` · pedido del usuario:** *"necesito imprimir los reportes de los
balances contables con un formato formal con el logo del fondo… generame uno muy completo y formal
ajustado a la normativa"*.

---

## 0. La norma, leída — no supuesta

ASOPREP es un **FCPC** (Fondo Complementario Previsional Cerrado) bajo control de la
**Superintendencia de Bancos**. Lo confirma el propio repositorio: el módulo `rpr` genera las
estructuras G40–G51 y `G40.md` fija `tipoFcpc = "3"`.

La norma contable aplicable es el **Catálogo de Cuentas para uso de los Fondos Complementarios
Previsionales Cerrados, Resolución No. SBS-2013-0507**. Se descargó el marco conceptual y se leyó
entero (24 páginas). Todo lo que sigue entre comillas es **cita textual** de ese documento; lo que
no está entrecomillado es decisión de diseño nuestra.

> ⚠️ **Lo que este documento NO resuelve.** El marco conceptual fija el juego de estados, los
> responsables, la periodicidad y el nivel de apertura, pero **no dicta un diseño gráfico**. El
> layout de §3 es nuestro, construido para cumplir lo que la norma sí exige. Y la propia resolución
> remite a la Superintendencia para las **estructuras de envío** ("Las características técnicas de
> los archivos, así como el medio de transmisión y reporte, serán definidos por la Superintendencia
> de Bancos y Seguros"): eso es el módulo `rpr` (los G), **no** estos reportes. Los G son estructuras
> de datos para transmisión; esto es presentación impresa. **No se mezclan.**

### 0.1 Lo que la norma sí fija, y que este formato debe cumplir

| Punto | Cita textual (SBS-2013-0507) |
|---|---|
| **Juego completo** | "(a) un estado de situación financiera al final del periodo; (b) un estado del resultado integral del periodo; (c) un estado de cambios en el patrimonio del periodo; (d) un estado de flujos de efectivo del periodo; (e) notas, que incluyan un resumen de las políticas contables más significativas y otra información explicativa; y (f) un estado de situación financiera al principio del primer periodo comparativo…" |
| **Moneda** | "Un juego completo de estados financieros expresados en **moneda de curso legal en el Ecuador**". Y: "La unidad monetaria de medida para la contabilidad y para la información financiera en la República del Ecuador, es el dólar." |
| **Quién responde** | "**Los miembros del Consejo de Administración de un Fondo son responsables de la preparación y presentación de sus estados financieros.**" |
| **Contador** | "Es responsabilidad legal del **contador general** … hasta la formulación de estados financieros… El contador general deberá ser **contador público autorizado (CPA)**". |
| **Otros responsables** | "…sin perjuicio de la responsabilidad que le compete al **Consejo de Administración, al representante legal, al auditor interno y al contador general**." |
| **Periodicidad** | "…deberán presentar, obligatoriamente, **estados financieros mensuales**, que deberán entregar… **dentro de los ocho días siguientes a la fecha del balance** que se reporte, hasta las 12H00." |
| **Nivel de apertura** | "…respetando las dinámicas, descripciones y cuentas contempladas en el Catálogo… **hasta un nivel de seis (6) dígitos**. Para propósitos administrativos y manejo interno… existe libertad en cuanto al uso de **subcuentas auxiliares (7 dígitos en adelante)**". |
| **Supletorio** | "…en lo no previsto por dichos catálogos, ni por la citada codificación, se aplicarán las **Normas Internacionales de Información Financiera NIIF**". |
| **Uniformidad** | "Los principios de contabilidad deben ser aplicados uniformemente de un período a otro. Cuando… se presenten cambios… deberá dejarse constancia expresa de tal situación". |

### 0.2 Consecuencias directas para el diseño

1. **El nombre es "ESTADO DE SITUACIÓN FINANCIERA"** — está en la norma, no es una preferencia.
2. **Las firmas no son decorativas.** La norma nombra cuatro responsables. El pie debe reflejarlos.
3. **El corte es mensual.** El encabezado debe expresar el mes con claridad de cierre.
4. **Seis dígitos es la frontera regulatoria.** Un balance "formal" no debería mezclar en el mismo
   cuerpo las subcuentas auxiliares internas de 7+ dígitos. Ver §3.4 — esto es lo más importante
   de todo este documento y es lo que ningún reporte actual contempla.
5. **NIIF como supletorio** ⇒ presentación comparativa y negativos entre paréntesis son lo esperable.

---

## 1. Qué existe hoy (no hay que crear reportes)

`rep/cnt/` ya tiene **seis** variantes, todas leyendo `CNT.DTMT` filtrado por `P_DTMTSCRP` (el
`idEjecucion` de `generarBalance`, guardado en `DTMTSCRP`). Los doce `.jasper` de `cnt` pasan el
fill: `OK: 12 | FALLIDOS: 0`. Los seis **ya declaran `P_IMAGEN`**: el logo lo inyecta
`ReporteServiceImpl` solo.

| Reporte | Corte | Debe/Haber | ¿Alcanzable desde la pantalla? |
|---|---|---|---|
| `RPRTCNTB_ACUM_CNFI` | acumulado con fecha inicial | no | sí |
| `RPRT_ACUM_CNFI_DBHB` | ídem | sí | sí |
| `RPRTCNTB_RNGO_FIFF` | por rango de fechas | no | sí |
| `RPRT_RNGO_FIFF_DBHB` | ídem | sí | sí |
| `RPRTCNTB_ACUM_SNFI` | a fecha de corte | no | **no** — ver §5 |
| `RPRT_ACUM_SNFI_DBHB` | ídem | sí | **no** — ver §5 |

---

## 2. Qué datos hay — la restricción que manda

`CNT.DTMT` (`TempReportes`), una fila por cuenta de la ejecución: `DTMTCTCN` código ·
`PLNNNMBR` nombre · `PLNNNVLL` **nivel** · `PLNNTPOO` movimiento/grupo · `PLNNCDPD` padre ·
`DTMTSLAN` **saldo anterior** · `DTMTDBEE`/`DTMTHBRR` debe/haber · `DTMTSLAC` **saldo actual** ·
centro de costo.

La agrupación Activo/Pasivo/Patrimonio **no está cableada**: sale de `CNT.RPRT` + `CNT.DTRP`
(rangos de cuentas con signo, por reporte configurable, elegido con `codigoAlterno`). Correcto que
sea así: el fondo parametriza su estructura y el Jasper no debe imponer otra.

---

## 3. El formato — qué se agrega a los seis

### 3.1 Cabecera

```
[LOGO]     <RAZÓN SOCIAL COMPLETA>
           RUC: <ruc>
           ESTADO DE SITUACIÓN FINANCIERA        ← literal por variante, §3.2
           AL 31 DE AGOSTO DE 2026               ← §3.3
           (Expresado en dólares de los Estados Unidos de América)
```

Razón social y RUC **de la base, no de un literal del FE**: nuevo `P_PJRQ_CODIGO` + join a
`SCP.PJRQ`. ⚠️ **VERIFICAR qué columna de `SCP.PJRQ` tiene el RUC** antes de escribir el join. Si
no existe, omitir el RUC y avisar — mejor sin RUC que con uno inventado.

### 3.2 Títulos literales

Los seis imprimen hoy `$P{P_REPORTE}` y `$P{P_PATH}`, **que nadie manda**: van a mostrar `null`,
el mismo defecto corregido en `3d9b56e9`. Se reemplazan por literal y los parámetros quedan
declarados (no se rompe el contrato).

| Reporte | Título |
|---|---|
| `RPRTCNTB_ACUM_CNFI` / `ACUM_SNFI` | ESTADO DE SITUACIÓN FINANCIERA |
| `RPRT_ACUM_CNFI_DBHB` / `SNFI_DBHB` | ESTADO DE SITUACIÓN FINANCIERA — CON MOVIMIENTO DEL PERÍODO |
| `RPRTCNTB_RNGO_FIFF` | BALANCE DE COMPROBACIÓN |
| `RPRT_RNGO_FIFF_DBHB` | BALANCE DE COMPROBACIÓN — SUMAS Y SALDOS |

"Estado de situación financiera" es el nombre **de la norma**. "Balance de comprobación" es un
documento de control interno, no del juego regulatorio: por eso lleva otro nombre a propósito.

### 3.3 Expresión del corte

- **SNFI**: `AL <31 DE AGOSTO DE 2026>` — en letras, mayúsculas, de `$P{P_FECHAFINAL}`.
- **CNFI** y **RNGO**: `DEL <1 DE AGOSTO DE 2026> AL <31 DE AGOSTO DE 2026>`.

### 3.4 🔴 Nivel de apertura — lo que hoy no contempla ningún reporte

La norma separa **hasta 6 dígitos** (catálogo obligatorio, lo que se reporta) de **7+ dígitos**
(auxiliares internas, libres). Un estado de situación financiera formal no debería imprimir las
auxiliares internas mezcladas con las cuentas del catálogo.

**Qué pido:** un parámetro nuevo `P_NIVEL_MAXIMO` (Integer, por defecto **6**) que corte por
longitud del código de cuenta (`LENGTH(REPLACE(DTMTCTCN,'.',''))` o el criterio que corresponda
según cómo esté formado `DTMTCTCN` — ⚠️ **VERIFICAR** si trae puntos o no antes de escribir la
condición). Con 6 sale el balance regulatorio; subiéndolo, el analítico interno.

⚠️ **Y una advertencia que hay que medir antes de decidir:** si las auxiliares de 7+ dígitos se
excluyen, **los totales tienen que seguir cuadrando** — los saldos de las auxiliares deben estar ya
sumados en su cuenta padre de 6 dígitos. Si en `DTMT` no fuera así, cortar por nivel produciría un
balance que no suma. **Medirlo con datos reales antes de dar esto por bueno**, y si no cuadra,
decirlo y no implementar el corte.

### 3.5 Cuerpo

- **Sangría por nivel** (`PLNNNVLL`), 8 pt por nivel.
- Cuentas de grupo en **negrita**; de movimiento en redonda.
- Importes con separador de miles, dos decimales, alineados a la derecha, **negativos entre
  paréntesis** (convención NIIF, supletoria por norma).
- **Cero se imprime `-`**, no `0,00`.
- Encabezado de columnas repetido en cada página.

### 3.6 Totales

- Total por cada nivel 1 (Activo, Pasivo, Patrimonio… según el reporte configurado).
- **Línea de comprobación**: `ACTIVO = PASIVO + PATRIMONIO` con la diferencia, resaltada si ≠ 0.
  Un balance que no cuadra tiene que gritarlo. ⚠️ Depende de que el reporte configurado tenga esos
  tres grupos; si no se puede determinar genéricamente, imprimir el total general y decirlo — **no
  inventar una igualdad que la parametrización no garantiza**.

### 3.7 Pie de firmas — según los cuatro responsables que nombra la norma

```
  ____________________   ____________________   ____________________   ____________________
   Presidente del          Representante Legal    Contador General       Auditor Interno
   Consejo de Administración                      Reg. CPA N° ________
```

La norma responsabiliza al **Consejo de Administración** de "la preparación y presentación", y
nombra además a **representante legal, auditor interno y contador general**. El "Reg. CPA" no es
adorno: "El contador general deberá ser contador público autorizado (CPA)".

Van como **texto del `.jrxml`**, no como parámetros — se cambian en un solo lugar. **Nombres en
blanco: se firman a mano.** No poner el usuario que imprime en la línea del contador: quien imprime
no es quien certifica.

**Precedente a copiar:** buscar primero un bloque de firmas ya existente en `rep/` (empezar por
`rhh/RPRT_ACTA_FNQT`). El usuario reconoce el formato que ya recibe.

⚠️ **A confirmar con el fondo:** si su estatuto nombra los cargos de otro modo (p. ej. "Presidente
del Directorio"), se ajusta el texto.

### 3.8 Pie de página

`Página X de Y` · fecha y hora de impresión · `Impreso por: $P{P_USUARIO}` · y el `idEjecucion`
(`$P{P_DTMTSCRP}`) en letra chica. Ese último dato es lo que permite reproducir exactamente el
mismo balance después: es lo que lo hace auditable.

---

## 4. Lo que este formato NO da, y hay que decirlo

La norma exige un **juego completo** de cinco componentes. Esto cubre **uno**.

| Componente exigido | Estado |
|---|---|
| Estado de situación financiera | ✅ es esto |
| Estado del resultado integral | ⚠️ depende de que exista un `ReporteContable` configurado de resultados; **verificar**. Si existe, el mismo Jasper sirve cambiando el título |
| Estado de cambios en el patrimonio | ❌ no existe fuente ni reporte |
| Estado de flujos de efectivo | ❌ no existe fuente ni reporte |
| Notas | ❌ no hay dónde guardarlas |

- **Comparativo del ejercicio anterior**: `DTMT` guarda una sola ejecución y `DTMTSLAN` es el saldo
  anterior *del mismo corte*, no el cierre del año pasado. Un comparativo real exige dos
  ejecuciones cruzadas — frente aparte.
- **Los tres faltantes son desarrollos nuevos**, no variantes de esto. Decisión del usuario si se
  abren.

---

## 5. Hallazgo del FE: dos de los seis no son alcanzables

`ACUM_SNFI` y `ACUM_SNFI_DBHB` **ni declaran `P_FECHAINICIAL`**: son "a fecha de corte" de verdad.
Pero el formulario tiene `fechaInicio` como `Validators.required` siempre, así que no hay estado de
la pantalla que los pida. Para habilitarlos hace falta una tercera opción en el combo de
acumulación ("A fecha de corte"). **Decisión del usuario**, es cambio de UI.

Y para la norma **el estado de situación financiera es a una fecha** — o sea que las variantes SNFI
son, en rigor, las que corresponden al entregable regulatorio. Vale la pena habilitarlas.

## 5.1 `P_FILTRO` y `P_MAYORIZADO`

Verificado por el FE: **no entran en la consulta SQL**, son solo texto de cabecera. Hoy se mandan
vacíos. Propuesta: `P_FILTRO` = resumen legible de los checkboxes aplicados (centros de costo,
distribuido, elimina saldos cero); `P_MAYORIZADO` = si el período está mayorizado, dato que **sí
importa en un estado formal** — un balance sobre un período no mayorizado es provisional y debería
decirlo. ⚠️ Verificar de dónde sale ese estado antes de cablearlo.

---

## 6. Verificación antes de dar por entregado

1. `compilar-jasper.bat` y `verificar-fill-jasper.bat`: los doce de `cnt` en OK.
2. 🔴 **Con datos, no con `JREmptyDataSource`**: un `JRDataSource` con cuentas de varios niveles y
   saldos que sumen. En los SEIS: ningún `null`, ningún total en blanco, sangría por nivel,
   negativos entre paréntesis, firmas en la última página. El fill vacío **no** atrapa un `null` —
   se aprendió el 2026-09-08 con los reportes de conciliación.
3. El corte por `P_NIVEL_MAXIMO` (§3.4) **medido contra datos reales**: que los totales cuadren.
4. Generar uno real desde la pantalla y mirarlo.

---

## Fuente

Superintendencia de Bancos y Seguros — *Marco Conceptual, Catálogo de Cuentas Fondos
Complementarios Previsionales Cerrados*, **Resolución No. SBS-2013-0507**, 24 pp.
`https://www.superbancos.gob.ec/bancos/wp-content/uploads/downloads/2017/07/CUC_marco_conceptual_FCPC_11_jul_13.pdf`
(descargado y leído el 2026-09-08; el sitio tiene el certificado TLS vencido, hubo que bajarlo con
`curl -k`).
