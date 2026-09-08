# Reportes de conciliación bancaria — `RPRT_CNCL_CNTA` y `RPRT_CNCL_GNRL`

**2026-09-08 · equipo `omen-saa-2` · pedido del usuario:** *"No existe un reporte de conciliación de
cuenta bancaria y de conciliación general. Crea el jasper súper completo."*

Dos reportes JasperReports 7.0.3 en `src/main/resources/rep/tsr/`, pedidos por
`POST /rest/rprt/generar` con `modulo: 'tsr'`, igual que `RPRT_ANTC_CLNT`. **Los dos con su
`.jasper` compilado en el mismo commit** — ver §5, que es lo que hasta hoy no se podía hacer en
esta máquina.

Todas las columnas de este documento se leyeron de las entidades el 2026-09-08 (no de memoria).
Las que **no** pude verificar están marcadas `⚠️ VERIFICAR` — el que implemente las confirma
contra la entidad antes de usarlas, y si difieren, corrige acá.

> **Actualización 2026-09-08 (equipo omen-saa-2, al implementar `RPRT_CNCL_CNTA`):** todos los
> `⚠️ VERIFICAR` de este documento se revisaron contra las entidades/DAO reales (no de memoria).
> La mayoría confirmó lo que ya decía el diseño; **dos NO** — corregidos en el `.jrxml` y
> documentados en el §7bis-report, más abajo. Resumen:
>
> - `CuentaBancaria` → empresa: confirmado, **no** tiene `PJRQCDGO` propio; el vínculo es
>   `TSR.CNBC.BNCOCDGO → TSR.BNCO.PJRQCDGO` (`ControlExtractoBancarioDaoService.selectCuentasBancariasActivas`
>   usa exactamente `c.banco.empresa.codigo`). El `.jrxml` de `RPRT_CNCL_CNTA` no necesitaba este
>   filtro (ya viene acotado por `P_CNBC_CODIGO`), pero **`RPRT_CNCL_GNRL` sí** — ver §3.1bis.
> - `CNT.DTAS.PLNNCDGO` (cuenta) y `CNT.ASNT.PJRQCDGO` (empresa): **confirmados tal cual**, las
>   dos FK existen exactamente como las usa §2.5. Sin cambios.
> - `selectCierreVigente` (el "cierre CERRADO más reciente" de una cuenta/período): usa
>   `ORDER BY fechaCierre DESC` + `LIMIT 1`, **no** `MAX(codigo)`. Con `CNCLCDGO` como secuencia
>   normalmente da lo mismo, pero para replicar el criterio real y no solo "probablemente lo
>   mismo", el `.jrxml` usa `MAX(x.CNCLCDGO) KEEP (DENSE_RANK LAST ORDER BY x.CNCLFCCR)` en vez
>   de `MAX(x.CNCLCDGO)` a secas — mismo resultado en el caso normal, correcto también en el
>   caso raro de un cierre cargado fuera de orden.
> - `TSR.EXBC.EXBCESTD`: confirmado simple flag activo/inactivo (1/0, no un rubro). El `.jrxml`
>   agrega `EXBCESTD = 1` al `LEFT JOIN` de §2.1 que antes no lo filtraba.
> - **`EXTRACTO_CARGADO` de §3.1 (la señal Sí/No de la fila por cuenta) estaba MAL.** La regla
>   real (`ExtractoBancarioDaoService.selectCuentasConCobertura`, la que usa el Tablero de
>   Cumplimiento) es *solapamiento de rango*: `EXBCFDSD <= período.hasta AND EXBCFHST >=
>   período.desde`, con `EXBCESTD=1` y `EXBCESTP <> ERROR(4)` — **no** mirar `DEXB.DEXBFTRN`
>   como decía el borrador original de §3.1. Corregido, ver §3.1bis.
> - **El arrastre de §2.5 (pendientes de contabilidad) ancla distinto de lo que decía el
>   modelo.** Este es el hallazgo grande — ver §7bis-report completo más abajo.



---

## 1. Qué modelo hay detrás (para entender las consultas)

| Tabla | Entidad | Qué es |
|---|---|---|
| `TSR.CNBC` | `CuentaBancaria` | La cuenta. `PLNNCDGO` → cuenta contable. Confirmado 2026-09-08: **no tiene `PJRQCDGO`**; el vínculo a empresa es `CNBC.BNCOCDGO → BNCO.PJRQCDGO` (igual que `ControlExtractoBancarioDaoService.selectCuentasBancariasActivas`). |
| `TSR.CNCL` | `Conciliacion` | **El cierre con partidas en tránsito** de una cuenta/período. `CNCLESTD`: 1 BORRADOR, 2 CERRADO, 3 ANULADO. Guarda la ecuación tal como cuadró: `CNCLSLDF` saldo libros, `CNCLDPTR` depósitos en tránsito, `CNCLCHNC` cheques girados no cobrados, `CNCLNCTR` NC no registradas, `CNCLNDTR` ND no registradas, `CNCLSLDE` saldo según extracto. `CNCLFCCR/CNCLUSCR` cierre; `CNCLMTAN` motivo de anulación. |
| `TSR.DTCN` | `DetalleTransito` | Cada partida declarada. `DTCNTPOO` tipo 1-4; `DTCNESTD` 1 PENDIENTE, 2 SALDADA; `CNCLCDGO` cierre que la declaró; `DTCNCNSL` cierre que la saldó; ancla: `DTCNDTAS` (línea de asiento, tipos 1/2) o `DTCNIDEX` (línea de extracto, tipos 3/4). |
| `TSR.CNCT` | `ConciliacionContable` | La revisión de una cuenta/período. `CNCTESTR`: 1 PENDIENTE, 2 VERIFICADO, 3 CON_DIFERENCIAS. Contadores `CNCTTTGR/CNCTPDEX/CNCTPDAS`; `CNCTUSVR/CNCTFCVR` verificación. |
| `TSR.GRCC` / `GCEX` / `GCAS` | grupos conciliados | Un grupo = N líneas de extracto (`GCEX`→`DEXB`) contra M líneas de asiento (`GCAS`→`DTAS`). `GRCCESTD` 1 activo. |
| `TSR.DEXB` / `EXBC` | extracto | Líneas y cabecera del estado de cuenta cargado. `DEXBESTR`: 1 PENDIENTE_REVISION, 2 CONCILIADA, 3 DESCARTADA. |
| `TSR.CTEB` | `ControlExtractoBancario` | El período de conciliación **de toda la empresa**: `CTEBCRRE` 1 = mes cerrado, `CTEBUSCR/CTEBFCCR`. |
| `CNT.PRDO` | `Periodo` | `PRDONMBR`, `PRDOINCO` primer día, `PRDOFNN` último día, `PJRQCDGO` empresa. |
| `CNT.DTAS` / `ASNT` | asiento | `DTASDBEE/DTASHBRR`, `DTASDSCR`, `ASNTNMAL` número alterno, `ASNTFCHA`, `ASNTESTD` (1 y 3 cuentan como vivos, igual que el DAO). Confirmado 2026-09-08: `DTAS.PLNNCDGO` (cuenta) y `ASNT.PJRQCDGO` (empresa) existen tal cual, sin alias raros. |

**La ecuación** (`ConciliacionCierreServiceImpl.saldoExtractoEsperado`, línea 599):

```
saldo extracto esperado = saldo libros − depósitos en tránsito + cheques girados no cobrados
                          + NC del banco no registradas − ND del banco no registradas
```

Se valida **una sola vez**, al cerrar (§3 de `DISENO-CONCILIACION-PARTIDAS-EN-TRANSITO.md`). El
reporte **no la recalcula**: imprime lo que `CNCL` guardó, y muestra `esperado − CNCLSLDE` como
control (debe dar 0,00 dentro de la tolerancia).

**Pendientes con arrastre** (regla exacta de `GrupoConciliacion{Asiento,Extracto}DaoServiceImpl`):
un movimiento cuenta como pendiente de este período si (a) cae en el período, **o** (b) está
anclado por una partida en tránsito todavía PENDIENTE de un cierre anterior; y no está en ningún
grupo activo. Las consultas de §2.4/§2.5 replican eso — **el arrastre del lado extracto (§2.4,
tipos 3/4) SÍ ancla por `DTCNIDEX`, confirmado igual que el DAO; el del lado asiento (§2.5,
tipos 1/2) NO ancla como decía este borrador — ver §7bis-report, resuelto el 2026-09-08.**

---

## 7bis-report. El arrastre de §2.5 no ancla por `DTCNDTAS`: ancla por `MVCB` (2026-09-08)

**Hallazgo al implementar `RPRT_CNCL_CNTA`.** El modelo de este documento (§1) decía que el
arrastre de asiento debía anclar por `DTCNDTAS` (la corrección de §7bis de
`DISENO-CONCILIACION-PARTIDAS-EN-TRANSITO.md`, que hizo de `DetalleAsiento` el ancla real de
toda partida tipo 1/2 nueva, porque solo el 8% tenía fila en `TSR.MVCB`). Verificado el código
real que corre HOY detrás de la pantalla **Conciliación Contable**
(`ConciliacionContableMatchServiceImpl.obtenerPendientesAsiento` →
`GrupoConciliacionAsientoDaoServiceImpl.selectPendientes`, leído línea por línea, no de memoria):
esa consulta **sigue anclando el arrastre por `dt.movimientoBanco.asiento.codigo`** (`TSR.MVCB
→ CNT.ASNT`), **no** por `DetalleAsiento`/`DTCNDTAS`. El código que sí migró a `DetalleAsiento`
es el que marca una partida como Saldada/Pendiente al conciliar/desconciliar
(`saldarPartidasTransitoDeclaradas`/`reabrirPartidasTransitoDeclaradas`, con el comentario
explícito "se busca por asiento, no por MovimientoBanco") — pero **no** el que busca qué
asiento arrastrar para ofrecerlo como pendiente. Es decir: la migración de §7bis quedó a medias
en este archivo puntual.

**Regla aplicada en el `.jrxml` (gana el código real que corre hoy, no el modelo "ideal"):**
la sección "Pendientes de contabilidad sin conciliar" arrastra un asiento cuando su
`ASNTCDGO` aparece como el asiento de un `TSR.MVCB` que a su vez es el origen de un `TSR.DTCN`
tipo 1/2 todavía Pendiente — exactamente `GrupoConciliacionAsientoDaoServiceImpl.selectPendientes`.
**No** se usa `DTCNDTAS` para esto (sí se sigue usando `DTCNDTAS` en §2.2 para mostrar de qué
línea de asiento salió cada partida declarada — eso es solo lectura/display, no arrastre, y ahí
`DTCNDTAS` es correcto y confiable).

**Consecuencia práctica, para la verificación de §6:** si una partida tipo 1/2 se declaró en
tránsito DESPUÉS del 2026-08-27 (o cualquier asiento contable directo que nunca generó fila en
`TSR.MVCB`, que es el 92% medido en su momento), esta consulta **no la arrastrará** aunque la
pantalla de conciliación contable —que usa el mismo DAO— tampoco lo haría. Ambas están
igual de "incompletas" respecto al ideal de §7bis, así que el reporte y la pantalla van a
coincidir (que es el criterio de aceptación de §6), aunque ninguna de las dos reglas sea la
más correcta posible. **Arreglar `GrupoConciliacionAsientoDaoServiceImpl` para que ancle por
`DetalleAsiento` como el resto del sistema es un cambio de comportamiento de la conciliación
contable en sí, fuera del alcance de "hacer un reporte que muestre lo que la pantalla muestra"**
— si se decide hacerlo, es un ticket aparte, y este reporte hereda la corrección automáticamente
en cuanto se aplique (usa la misma regla, no una copia congelada).

---

## 2. `RPRT_CNCL_CNTA` — Conciliación de una cuenta bancaria

> **Nota de implementación 2026-09-08 — desviación deliberada del diseño de subdatasets.** Este
> documento pedía subdatasets con `<datasetRun>` para las secciones múltiples. Antes de
> escribirlo se hizo `grep` de todo `src/main/resources/rep/**` buscando `subDataset`,
> `datasetRun`, `kind="table"` y `kind="list"`: **cero resultados** — ningún `.jrxml` de este
> repo usa esos componentes, todos los reportes multi-fila existentes (p. ej.
> `RPRT_ASNT_CNTB.jrxml`) resuelven "cabecera + N filas repetidas" con un único `<group>` y una
> sola banda de `<detail>`. Escribir a mano un componente sin ningún ejemplo local, en un dialecto
> de JRXML "compacto" (`kind="..."`) que tampoco tiene ejemplos de `datasetRun` en internet para
> esa sintaxis puntual, era el riesgo estructural más alto de esta entrega — y a diferencia del
> riesgo de la consulta SQL (que de cualquier forma solo se valida en producción, elegir
> subdatasets o no ELIMINA ese riesgo), el riesgo de sintaxis de JRXML SÍ es evitable si se usa
> solo lo que ya está probado en este repo.
>
> **Solución aplicada:** una única consulta SQL con `UNION ALL` (`ORDEN_SECCION` discrimina la
> sección del reporte, `TIPO_FILA` discrimina qué banda de `<detail>` renderiza cada fila —
> `<detail>` admite varias `<band>` con `printWhenExpression`, mecanismo estándar de JRXML), más
> dos `<group>` anidados (`Seccion` por `ORDEN_SECCION` para los títulos de sección y
> subtotales; `TipoPartida` por `R_TIPO` para el subtotal por tipo de partida en tránsito). La
> fila con `ORDEN_SECCION=0` (cabecera/estado/ecuación) es siempre la primera por el `ORDER BY`,
> y se lee en el `<title>`, que JasperReports evalúa una sola vez con los valores del primer
> registro — el mismo patrón de "1 fila = 1 reporte" que ya usa `RPRT_ANTC_CLNT.jrxml`, aplicado
> aquí solo a esa primera fila en vez de a todo el reporte. Sin subreportes, sin `.jasper`
> sueltos, sin `SUBREPORT_DIR` — el objetivo original de evitar subreportes se cumple igual.

**Parámetros:** `P_CNBC_CODIGO` (Long), `P_PRDO_CODIGO` (Long), `P_USUARIO` (String), más
`P_PATH`, `P_REPORTE`, `P_IMAGEN` como los demás (el backend inyecta el logo si no viene).

**Formato:** A4 vertical, PDF (Excel opcional). Una cuenta, un período.

### Secciones, en este orden

1. **Cabecera.** Empresa, banco, número de cuenta, cuenta contable (número y nombre), período
   (nombre y rango de fechas), "Mes de conciliación: CERRADO por X el Y / ABIERTO", usuario que
   imprime, fecha/hora de impresión.
2. **Estado.** Tres tarjetas: *Cierre con partidas en tránsito* (CERRADO el … por … / SIN CIERRE /
   ANULADO: motivo), *Revisión contable* (Verificado por … el … / Pendiente / Con diferencias),
   *Extracto* (archivo, cargado el … por …, saldo inicial/final del banco / NO CARGADO).
3. **La ecuación** — el cuadro que el auditor quiere ver, con signos:

   | | |
   |---|---:|
   | Saldo según libros al {último día} | CNCLSLDF |
   | (−) Depósitos en tránsito | CNCLDPTR |
   | (+) Cheques girados no cobrados | CNCLCHNC |
   | (+) Notas de crédito del banco no registradas | CNCLNCTR |
   | (−) Notas de débito del banco no registradas | CNCLNDTR |
   | **Saldo según extracto esperado** | fórmula |
   | Saldo según extracto (declarado) | CNCLSLDE |
   | **Diferencia** | esperado − declarado (resaltar si ≠ 0,00) |

   Sin cierre: imprimir el cuadro con guiones y la leyenda "Cierre pendiente".
4. **Partidas en tránsito**, agrupadas por tipo (1→4), con subtotal por tipo. Columnas: origen
   (asiento nº/fecha/detalle **o** extracto fecha/detalle/referencia), valor, declarada el,
   período de origen (si es arrastrada, marcarlo: *"arrastrada de {período}"*), estado
   (Pendiente / Saldada en {período}), observación.
5. **Movimientos conciliados del período** — un bloque por grupo: nº grupo, conciliado por/el,
   valor extracto, valor asiento, diferencia, rango de fechas; debajo sus líneas, dos columnas
   lado a lado o dos listas (Extracto: fecha, descripción, referencia, débito, crédito ·
   Contabilidad: asiento, fecha, descripción, debe, haber). Totales del bloque.
6. **Pendientes del extracto sin conciliar** (con la columna *Situación*: "Declarada en tránsito"
   / **"SIN DECLARAR"** en rojo) y total.
7. **Pendientes de contabilidad sin conciliar**, ídem.
8. **Pie:** totales de control — nº de líneas de extracto del período, nº conciliadas, nº
   pendientes; nº de líneas de libro, ídem. Firmas: Elaborado / Revisado / Aprobado.

### 2.1 Consulta principal (una fila)

```sql
SELECT e.PJRQNMBR                                   AS EMPRESA,
       p.PRDONMBR                                   AS PERIODO,
       p.PRDOINCO                                   AS DESDE,
       p.PRDOFNN                                    AS HASTA,
       b.BNCONMBR                                   AS BANCO,
       c.CNBCNMRO                                   AS NUMERO_CUENTA,
       pc.PLNNCNTA                                  AS CUENTA_CONTABLE,
       pc.PLNNNMBR                                  AS NOMBRE_CUENTA,
       ci.CNCLCDGO                                  AS ID_CIERRE,
       ci.CNCLESTD                                  AS ESTADO_CIERRE,
       ci.CNCLFCCR                                  AS FECHA_CIERRE,
       ci.CNCLUSCR                                  AS USUARIO_CIERRE,
       ci.CNCLMTAN                                  AS MOTIVO_ANULACION,
       ci.CNCLSLDF                                  AS SALDO_LIBROS,
       ci.CNCLDPTR                                  AS DEPOSITOS_TRANSITO,
       ci.CNCLCHNC                                  AS CHEQUES_NO_COBRADOS,
       ci.CNCLNCTR                                  AS NC_NO_REGISTRADAS,
       ci.CNCLNDTR                                  AS ND_NO_REGISTRADAS,
       ci.CNCLSLDE                                  AS SALDO_EXTRACTO,
       (NVL(ci.CNCLSLDF,0) - NVL(ci.CNCLDPTR,0) + NVL(ci.CNCLCHNC,0)
        + NVL(ci.CNCLNCTR,0) - NVL(ci.CNCLNDTR,0)) AS SALDO_EXTRACTO_ESPERADO,
       cc.CNCTESTR                                  AS ESTADO_REVISION,
       cc.CNCTUSVR                                  AS VERIFICADO_POR,
       cc.CNCTFCVR                                  AS FECHA_VERIFICACION,
       cc.CNCTTTGR                                  AS TOTAL_GRUPOS,
       cc.CNCTPDEX                                  AS PEND_EXTRACTO,
       cc.CNCTPDAS                                  AS PEND_ASIENTO,
       ex.EXBCARCH                                  AS EXTRACTO_ARCHIVO,
       ex.EXBCSLIN                                  AS EXTRACTO_SALDO_INICIAL,
       ex.EXBCSLFN                                  AS EXTRACTO_SALDO_FINAL,
       ex.EXBCFCRG                                  AS EXTRACTO_CARGADO_EL,
       ex.EXBCUSAR                                  AS EXTRACTO_CARGADO_POR,
       ct.CTEBCRRE                                  AS MES_CERRADO,
       ct.CTEBUSCR                                  AS MES_CERRADO_POR,
       ct.CTEBFCCR                                  AS MES_CERRADO_EL
  FROM TSR.CNBC c
  JOIN TSR.BNCO b   ON b.BNCOCDGO = c.BNCOCDGO
  JOIN CNT.PLNN pc  ON pc.PLNNCDGO = c.PLNNCDGO
  JOIN CNT.PRDO p   ON p.PRDOCDGO = $P{P_PRDO_CODIGO}
  JOIN SCP.PJRQ e   ON e.PJRQCDGO = p.PJRQCDGO
  LEFT JOIN TSR.CNCL ci ON ci.CNCLCDGO = (SELECT MAX(x.CNCLCDGO) KEEP (DENSE_RANK LAST ORDER BY x.CNCLFCCR)
                                          FROM TSR.CNCL x
                                          WHERE x.CNBCCDGO = c.CNBCCDGO AND x.CNCLPRDO = p.PRDOCDGO
                                            AND x.CNCLESTD = 2)
  LEFT JOIN TSR.CNCT cc ON cc.CNBCCDGO = c.CNBCCDGO AND cc.PRDOCDGO = p.PRDOCDGO
  LEFT JOIN TSR.EXBC ex ON ex.EXBCCDGO = (SELECT MAX(y.EXBCCDGO) FROM TSR.EXBC y
                                          WHERE y.CNBCCDGO = c.CNBCCDGO AND y.PRDOCDGO = p.PRDOCDGO
                                            AND y.EXBCESTD = 1)
  LEFT JOIN TSR.CTEB ct ON ct.PJRQCDGO = p.PJRQCDGO AND ct.PRDOCDGO = p.PRDOCDGO
 WHERE c.CNBCCDGO = $P{P_CNBC_CODIGO}
```

Los `MAX(...)`/`KEEP (DENSE_RANK LAST ...)` en los `LEFT JOIN` son a propósito: garantizan **una
sola fila** aunque haya un cierre anulado y otro vigente, o dos archivos de extracto cargados.
Confirmado 2026-09-08: `ConciliacionDaoServiceImpl.selectCierreVigente` usa `ORDER BY
fechaCierre DESC` + `LIMIT 1` (no `MAX(codigo)`), replicado arriba con
`KEEP (DENSE_RANK LAST ORDER BY x.CNCLFCCR)`. `EXBCESTD` confirmado: flag simple activo(1)/
inactivo(0), no un rubro — agregado el filtro `EXBCESTD = 1` arriba.

### 2.2 Partidas en tránsito (subdataset)

```sql
SELECT dt.DTCNTPOO AS TIPO,
       CASE dt.DTCNTPOO WHEN 1 THEN 'Depósitos en tránsito'
                        WHEN 2 THEN 'Cheques girados no cobrados'
                        WHEN 3 THEN 'Notas de crédito del banco no registradas'
                        WHEN 4 THEN 'Notas de débito del banco no registradas' END AS TIPO_NOMBRE,
       dt.DTCNVLOR AS VALOR,
       dt.DTCNESTD AS ESTADO,
       dt.DTCNOBSR AS OBSERVACION,
       dt.DTCNFCRG AS DECLARADA_EL,
       po.PRDONMBR AS PERIODO_ORIGEN,
       CASE WHEN ci.CNCLPRDO = $P{P_PRDO_CODIGO} THEN 'N' ELSE 'S' END AS ARRASTRADA,
       ps.PRDONMBR AS SALDADA_EN,
       a.ASNTNMAL  AS ASIENTO,
       a.ASNTFCHA  AS FECHA_ASIENTO,
       da.DTASDSCR AS DETALLE_ASIENTO,
       de.DEXBFTRN AS FECHA_EXTRACTO,
       de.DEXBDSCR AS DETALLE_EXTRACTO,
       de.DEXBREFR AS REFERENCIA_EXTRACTO
  FROM TSR.DTCN dt
  JOIN TSR.CNCL ci ON ci.CNCLCDGO = dt.CNCLCDGO
  JOIN CNT.PRDO po ON po.PRDOCDGO = ci.CNCLPRDO
  LEFT JOIN TSR.CNCL cs ON cs.CNCLCDGO = dt.DTCNCNSL
  LEFT JOIN CNT.PRDO ps ON ps.PRDOCDGO = cs.CNCLPRDO
  LEFT JOIN CNT.DTAS da ON da.DTASCDGO = dt.DTCNDTAS
  LEFT JOIN CNT.ASNT a  ON a.ASNTCDGO = da.ASNTCDGO
  LEFT JOIN TSR.DEXB de ON de.DEXBCDGO = dt.DTCNIDEX
 WHERE ci.CNBCCDGO = $P{P_CNBC_CODIGO}
   AND ci.CNCLESTD = 2
   AND (   ci.CNCLPRDO = $P{P_PRDO_CODIGO}
        OR (dt.DTCNESTD = 1
            AND po.PRDOFNN < (SELECT q.PRDOINCO FROM CNT.PRDO q WHERE q.PRDOCDGO = $P{P_PRDO_CODIGO})))
 ORDER BY dt.DTCNTPOO, po.PRDOINCO, dt.DTCNFCRG
```

### 2.3 Grupos conciliados del período (subdataset) y sus líneas

```sql
SELECT g.GRCCCDGO AS GRUPO, g.GRCCVLEX AS VALOR_EXTRACTO, g.GRCCVLAS AS VALOR_ASIENTO,
       g.GRCCDIFF AS DIFERENCIA, g.GRCCFCMN AS FECHA_MIN, g.GRCCFCMX AS FECHA_MAX,
       g.GRCCTOLD AS TOLERANCIA_DIAS, g.GRCCUSCN AS CONCILIADO_POR, g.GRCCFCCN AS CONCILIADO_EL,
       g.GRCCOBSR AS OBSERVACION,
       (SELECT COUNT(*) FROM TSR.GCEX x WHERE x.GRCCCDGO = g.GRCCCDGO) AS LINEAS_EXTRACTO,
       (SELECT COUNT(*) FROM TSR.GCAS y WHERE y.GRCCCDGO = g.GRCCCDGO) AS LINEAS_ASIENTO
  FROM TSR.GRCC g
  JOIN TSR.CNCT cc ON cc.CNCTCDGO = g.CNCTCDGO
 WHERE cc.CNBCCDGO = $P{P_CNBC_CODIGO} AND cc.PRDOCDGO = $P{P_PRDO_CODIGO} AND g.GRCCESTD = 1
 ORDER BY g.GRCCFCMN, g.GRCCCDGO
```

Líneas de **un** grupo (parámetro `P_GRCC_CODIGO`), extracto y libro en una sola lista con `ORIGEN`:

```sql
SELECT 'EXTRACTO' AS ORIGEN, de.DEXBFTRN AS FECHA, de.DEXBDSCR AS DESCRIPCION, de.DEXBREFR AS REFERENCIA,
       de.DEXBDBTO AS DEBITO, de.DEXBCRDT AS CREDITO, NULL AS ASIENTO
  FROM TSR.GCEX x JOIN TSR.DEXB de ON de.DEXBCDGO = x.DEXBCDGO
 WHERE x.GRCCCDGO = $P{P_GRCC_CODIGO}
UNION ALL
SELECT 'LIBRO', a.ASNTFCHA, da.DTASDSCR, NULL, da.DTASDBEE, da.DTASHBRR, a.ASNTNMAL
  FROM TSR.GCAS y JOIN CNT.DTAS da ON da.DTASCDGO = y.DTASCDGO JOIN CNT.ASNT a ON a.ASNTCDGO = da.ASNTCDGO
 WHERE y.GRCCCDGO = $P{P_GRCC_CODIGO}
 ORDER BY 1, 2
```

### 2.4 Pendientes del extracto sin conciliar (subdataset) — misma regla que el DAO

```sql
SELECT de.DEXBFTRN AS FECHA, de.DEXBDSCR AS DESCRIPCION, de.DEXBREFR AS REFERENCIA,
       de.DEXBDBTO AS DEBITO, de.DEXBCRDT AS CREDITO, de.DEXBSLDO AS SALDO,
       pe.PRDONMBR AS PERIODO_EXTRACTO,
       CASE WHEN EXISTS (SELECT 1 FROM TSR.DTCN t WHERE t.DTCNIDEX = de.DEXBCDGO AND t.DTCNESTD = 1)
            THEN 'Declarada en tránsito' ELSE 'SIN DECLARAR' END AS SITUACION
  FROM TSR.DEXB de
  JOIN CNT.PRDO pe ON pe.PRDOCDGO = de.PRDOCDGO
 WHERE de.CNBCCDGO = $P{P_CNBC_CODIGO}
   AND de.DEXBESTD = 1
   AND (   de.PRDOCDGO = $P{P_PRDO_CODIGO}
        OR de.DEXBCDGO IN (SELECT t.DTCNIDEX FROM TSR.DTCN t WHERE t.DTCNTPOO IN (3,4) AND t.DTCNESTD = 1))
   AND de.DEXBCDGO NOT IN (SELECT x.DEXBCDGO FROM TSR.GCEX x JOIN TSR.GRCC g ON g.GRCCCDGO = x.GRCCCDGO
                            WHERE g.GRCCESTD = 1)
 ORDER BY de.DEXBFTRN, de.DEXBCDGO
```

### 2.5 Pendientes de contabilidad sin conciliar (subdataset) — misma regla que el DAO

```sql
SELECT a.ASNTNMAL AS ASIENTO, a.ASNTFCHA AS FECHA, da.DTASDSCR AS DESCRIPCION,
       da.DTASDBEE AS DEBE, da.DTASHBRR AS HABER, a.ASNTOBSR AS OBSERVACION_ASIENTO,
       CASE WHEN EXISTS (SELECT 1 FROM TSR.DTCN t WHERE t.DTCNDTAS = da.DTASCDGO AND t.DTCNESTD = 1)
            THEN 'Declarada en tránsito' ELSE 'SIN DECLARAR' END AS SITUACION
  FROM CNT.DTAS da
  JOIN CNT.ASNT a ON a.ASNTCDGO = da.ASNTCDGO
  JOIN CNT.PRDO p ON p.PRDOCDGO = $P{P_PRDO_CODIGO}
 WHERE da.PLNNCDGO = (SELECT c.PLNNCDGO FROM TSR.CNBC c WHERE c.CNBCCDGO = $P{P_CNBC_CODIGO})  -- FK confirmada
   AND a.PJRQCDGO = p.PJRQCDGO                                                                   -- FK confirmada
   AND a.ASNTESTD IN (1, 3)
   AND (   a.ASNTFCHA BETWEEN p.PRDOINCO AND p.PRDOFNN
        -- Arrastre: NO por DTCNDTAS -- ver §7bis-report. La regla real
        -- (GrupoConciliacionAsientoDaoServiceImpl.selectPendientes, la que usa hoy la pantalla
        -- de Conciliación Contable) ancla por TSR.MVCB -> CNT.ASNT.
        OR a.ASNTCDGO IN (SELECT m.ASNTCDGO FROM TSR.MVCB m JOIN TSR.DTCN t ON t.MVCBCDGO = m.MVCBCDGO
                           WHERE t.DTCNTPOO IN (1,2) AND t.DTCNESTD = 1))
   AND da.DTASCDGO NOT IN (SELECT y.DTASCDGO FROM TSR.GCAS y JOIN TSR.GRCC g ON g.GRCCCDGO = y.GRCCCDGO
                            WHERE g.GRCCESTD = 1)
 ORDER BY a.ASNTFCHA, a.ASNTNMAL, da.DTASCDGO
```

---

## 3. `RPRT_CNCL_GNRL` — Conciliación general del período (todas las cuentas)

**Parámetros:** `P_PJRQ_CODIGO` (Long, empresa), `P_PRDO_CODIGO` (Long), `P_USUARIO`, más los
comunes. **Formato:** A4 **horizontal** — es una tabla ancha. PDF y **Excel** (es el que van a
querer en planilla).

### Secciones

1. **Cabecera.** Empresa, período, "Mes de conciliación: CERRADO por … el … / ABIERTO", cuentas
   activas, cuántas verificadas, cuántas con extracto, usuario/fecha de impresión.
2. **Una fila por cuenta bancaria activa**, ordenadas por banco y número:
   banco · cuenta · cuenta contable · extracto (Sí/No) · saldo libros · (−) dep. tránsito ·
   (+) cheques no cobrados · (+) NC no reg. · (−) ND no reg. · saldo extracto esperado · saldo
   extracto declarado · **diferencia** · pend. extracto · pend. libro · grupos · revisión
   (Verificado/Pendiente/Con diferencias/Sin extracto) · verificado por · fecha · cierre tránsito
   (fecha) · Situación: **"OK" / "SIN CIERRE" / "SIN EXTRACTO" / "PENDIENTES SIN DECLARAR"**.
   Resaltar en rojo: diferencia ≠ 0, "SIN CIERRE", pendientes > 0 sin verificar.
3. **Totales** de las columnas de valor.
4. **Resumen de partidas en tránsito pendientes de toda la empresa** al cierre del período, por
   tipo (cantidad y valor) — es el "arrastre" que el auditor pregunta primero.
5. Firmas.

### 3.1 Consulta principal (una fila por cuenta)

```sql
SELECT e.PJRQNMBR   AS EMPRESA,
       p.PRDONMBR   AS PERIODO, p.PRDOINCO AS DESDE, p.PRDOFNN AS HASTA,
       ct.CTEBCRRE  AS MES_CERRADO, ct.CTEBUSCR AS MES_CERRADO_POR, ct.CTEBFCCR AS MES_CERRADO_EL,
       b.BNCONMBR   AS BANCO, c.CNBCNMRO AS NUMERO_CUENTA,
       pc.PLNNCNTA  AS CUENTA_CONTABLE, pc.PLNNNMBR AS NOMBRE_CUENTA,
       CASE WHEN EXISTS (SELECT 1 FROM TSR.EXBC x WHERE x.CNBCCDGO = c.CNBCCDGO
                            AND x.EXBCESTD = 1 AND x.EXBCESTP <> 4
                            AND x.EXBCFDSD <= p.PRDOFNN AND x.EXBCFHST >= p.PRDOINCO)
            THEN 'S' ELSE 'N' END                    AS EXTRACTO_CARGADO,
       ci.CNCLSLDF  AS SALDO_LIBROS,
       ci.CNCLDPTR  AS DEPOSITOS_TRANSITO,
       ci.CNCLCHNC  AS CHEQUES_NO_COBRADOS,
       ci.CNCLNCTR  AS NC_NO_REGISTRADAS,
       ci.CNCLNDTR  AS ND_NO_REGISTRADAS,
       (NVL(ci.CNCLSLDF,0) - NVL(ci.CNCLDPTR,0) + NVL(ci.CNCLCHNC,0)
        + NVL(ci.CNCLNCTR,0) - NVL(ci.CNCLNDTR,0)) AS SALDO_EXTRACTO_ESPERADO,
       ci.CNCLSLDE  AS SALDO_EXTRACTO,
       ci.CNCLFCCR  AS FECHA_CIERRE, ci.CNCLUSCR AS USUARIO_CIERRE,
       cc.CNCTESTR  AS ESTADO_REVISION, cc.CNCTUSVR AS VERIFICADO_POR, cc.CNCTFCVR AS FECHA_VERIFICACION,
       cc.CNCTPDEX  AS PEND_EXTRACTO, cc.CNCTPDAS AS PEND_ASIENTO, cc.CNCTTTGR AS TOTAL_GRUPOS
  FROM CNT.PRDO p
  JOIN SCP.PJRQ e  ON e.PJRQCDGO = p.PJRQCDGO
  JOIN TSR.BNCO b  ON b.PJRQCDGO = e.PJRQCDGO        -- filtro de empresa (faltaba, ver #3.1bis)
  JOIN TSR.CNBC c  ON c.BNCOCDGO = b.BNCOCDGO AND c.CNBCESTD = 1
  JOIN CNT.PLNN pc ON pc.PLNNCDGO = c.PLNNCDGO
  LEFT JOIN TSR.CNCL ci ON ci.CNCLCDGO = (SELECT MAX(x.CNCLCDGO) KEEP (DENSE_RANK LAST ORDER BY x.CNCLFCCR)
                                          FROM TSR.CNCL x
                                          WHERE x.CNBCCDGO = c.CNBCCDGO AND x.CNCLPRDO = p.PRDOCDGO
                                            AND x.CNCLESTD = 2)
  LEFT JOIN TSR.CNCT cc ON cc.CNBCCDGO = c.CNBCCDGO AND cc.PRDOCDGO = p.PRDOCDGO
  LEFT JOIN TSR.CTEB ct ON ct.PJRQCDGO = p.PJRQCDGO AND ct.PRDOCDGO = p.PRDOCDGO
 WHERE p.PRDOCDGO = $P{P_PRDO_CODIGO}
   AND p.PJRQCDGO = $P{P_PJRQ_CODIGO}
 ORDER BY b.BNCONMBR, c.CNBCNMRO
```

#### 3.1bis. Correcciones aplicadas (2026-09-08, al implementar)

1. **Bug real, no solo `⚠️ VERIFICAR`: el `JOIN TSR.CNBC c ON c.CNBCESTD = 1` original no
   filtraba por empresa en absoluto.** `TSR.CNBC` no tiene `PJRQCDGO` propio (confirmado, ver
   §1); el vínculo real es `CNBC.BNCOCDGO → BNCO.PJRQCDGO`
   (`ControlExtractoBancarioDaoService.selectCuentasBancariasActivas` usa exactamente
   `c.banco.empresa.codigo`). Sin el `JOIN TSR.BNCO b ON b.PJRQCDGO = e.PJRQCDGO` agregado
   arriba, este reporte habría mostrado **las cuentas bancarias de TODAS las empresas**, no solo
   la de `P_PJRQ_CODIGO`. Corregido antes de compilar nada — no llegó a producción.
2. **`EXTRACTO_CARGADO` usaba el criterio equivocado.** El original miraba si existía una fila de
   `TSR.DEXB` con `DEXBFTRN` dentro del rango del período. La regla real
   (`ExtractoBancarioDaoService.selectCuentasConCobertura`, la que usa el Tablero de
   Cumplimiento) es *solapamiento de rango de la cabecera del archivo*:
   `EXBCFDSD <= período.hasta AND EXBCFHST >= período.desde`, con `EXBCESTD = 1` (activo) y
   `EXBCESTP <> 4` (no en estado ERROR). Corregido arriba.
3. **`CNCLCDGO` vigente:** mismo ajuste que en §2.1 (`KEEP (DENSE_RANK LAST ORDER BY
   fechaCierre)` en vez de `MAX(codigo)` a secas), por la misma razón: replicar
   `selectCierreVigente` con exactitud.
4. **Decisión tomada (el diseño la dejaba abierta):** `PEND_EXTRACTO`/`PEND_ASIENTO`/
   `TOTAL_GRUPOS` usan los contadores **guardados** en `TSR.CNCT` (los que escribe
   `recalcularContadores`), no el recálculo en vivo de las consultas §2.4/§2.5 replicado para
   cada una de las N cuentas del período — sería mucho más lento y este reporte es "todas las
   cuentas de un vistazo", no el detalle por cuenta (para eso está `RPRT_CNCL_CNTA`). Si algún
   contador guardado queda desactualizado (no se corrió `recalcularContadores` después de un
   cambio), la señal "Situación" de la fila puede no reflejar el estado más reciente — es un
   trade-off aceptado, no un bug.
5. **La etiqueta "PENDIENTES SIN DECLARAR" del diseño original se simplificó a "PENDIENTES"
   en el `.jrxml`.** Con los contadores agregados de `CNCT` no se puede distinguir "declarada en
   tránsito pero aún sin saldar" de "nunca declarada" (esa distinción solo existe a nivel de
   línea, en las consultas §2.4/§2.5 de `RPRT_CNCL_CNTA`) — decir "SIN DECLARAR" en este reporte
   sería una afirmación que la consulta no puede respaldar. "PENDIENTES", más general (con la
   misma marca en rojo), evita reclamar una precisión que la fuente de datos no tiene a este
   nivel de agregación.

`CNCTPDEX/CNCTPDAS` son los contadores **guardados** en `CNCT` (los recalcula
`recalcularContadores`). Si se quiere el dato vivo en vez del guardado, reemplazarlos por los
`COUNT` de las consultas §2.4/§2.5 — más lento, más exacto. Decisión del implementador; anotarla.

### 3.2 Resumen de partidas pendientes de la empresa (subdataset)

```sql
SELECT dt.DTCNTPOO AS TIPO, COUNT(*) AS CANTIDAD, SUM(dt.DTCNVLOR) AS VALOR
  FROM TSR.DTCN dt
  JOIN TSR.CNCL ci ON ci.CNCLCDGO = dt.CNCLCDGO
  JOIN CNT.PRDO po ON po.PRDOCDGO = ci.CNCLPRDO
 WHERE ci.PJRQCDGO = $P{P_PJRQ_CODIGO}
   AND ci.CNCLESTD = 2
   AND dt.DTCNESTD = 1
   AND po.PRDOINCO <= (SELECT q.PRDOFNN FROM CNT.PRDO q WHERE q.PRDOCDGO = $P{P_PRDO_CODIGO})
 GROUP BY dt.DTCNTPOO
 ORDER BY dt.DTCNTPOO
```

---

## 4. Frontend

- **Conciliación · Cierre con partidas en tránsito** (`conciliacion-cierre`): botón **"Imprimir
  conciliación"** junto al histórico de cierres, para la cuenta/período seleccionados. Siempre
  habilitado con cuenta y período elegidos (sin cierre imprime el estado "pendiente", que también
  sirve). `JasperReportesService.generar('tsr', 'RPRT_CNCL_CNTA', {P_CNBC_CODIGO, P_PRDO_CODIGO,
  P_USUARIO})` + `guardarArchivo` de `shared/services/descarga-reporte.ts`.
- **Conciliación Contable** (`conciliacion-contable`): en la vista de resumen del período, botón
  **"Conciliación general"** (PDF y Excel — dos opciones o un menú), con `P_PJRQ_CODIGO` de la
  sesión y `P_PRDO_CODIGO`. Y en cada fila de cuenta, un icono de impresión que llama al de cuenta.

---

## 5. El `.jasper` — cómo se compila en esta máquina (y por qué el CLAUDE.md estaba incompleto)

El CLAUDE.md dice, con razón, que **dentro de WildFly** no hay compilación que funcione y que el
`.jasper` es obligatorio. Lo que no dice es cómo producirlo sin Jaspersoft Studio. Medido el
2026-09-08: **no hay Studio en la OMEN**, pero `mvn dependency:get
-Dartifact=net.sf.jasperreports:jasperreports-jdt:7.0.3` **baja sin problema**, y el `pom` ya
trae `ecj`. O sea que fuera del servidor, con el classpath de Maven + `jasperreports-jdt`, un
`JasperCompileManager.compileReportToFile` compila el `.jrxml` igual que Studio.

Lo que hay que dejar en el repo (BE): un harness reproducible — un `main` chico fuera del WAR
(p. ej. `tools/jasper/CompilarJasper.java`) más un `compilar-jasper.bat` que arme el classpath con
`mvn dependency:build-classpath` y compile todos los `.jrxml` de `rep/**` que no tengan `.jasper`
al día. **Sin tocar las dependencias del WAR** (`jasperreports-jdt` no entra al `pom` de
producción; se usa solo en el harness). Y corregir la nota del CLAUDE.md: sigue siendo cierto que
en runtime no compila; ya no es cierto que haga falta Studio.

---

## 6. Verificación antes de dar por entregado

1. Correr la consulta §2.1 y §3.1 a mano contra la base con una cuenta/período real: **una fila
   por cuenta**, ninguna duplicada por cierres anulados ni por dos extractos.
2. Contrastar §2.4/§2.5 con lo que muestra la pantalla de Conciliación Contable para la misma
   cuenta: mismos pendientes, mismo conteo. Si difieren, gana el DAO y se corrige la consulta.
3. `mvn -q compile` + los dos `.jasper` generados por el harness, commiteados junto al `.jrxml`.
4. Generar los dos PDF por `POST /rest/rprt/generar` desde Postman o la pantalla.
