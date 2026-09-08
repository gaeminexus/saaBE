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

---

## 1. Qué modelo hay detrás (para entender las consultas)

| Tabla | Entidad | Qué es |
|---|---|---|
| `TSR.CNBC` | `CuentaBancaria` | La cuenta. `PLNNCDGO` → cuenta contable. `⚠️ VERIFICAR` cómo se liga a la empresa: **no tiene `PJRQCDGO`** — replicar la regla de `ControlExtractoBancarioDaoService.selectCuentasBancariasActivas(idEmpresa)`. |
| `TSR.CNCL` | `Conciliacion` | **El cierre con partidas en tránsito** de una cuenta/período. `CNCLESTD`: 1 BORRADOR, 2 CERRADO, 3 ANULADO. Guarda la ecuación tal como cuadró: `CNCLSLDF` saldo libros, `CNCLDPTR` depósitos en tránsito, `CNCLCHNC` cheques girados no cobrados, `CNCLNCTR` NC no registradas, `CNCLNDTR` ND no registradas, `CNCLSLDE` saldo según extracto. `CNCLFCCR/CNCLUSCR` cierre; `CNCLMTAN` motivo de anulación. |
| `TSR.DTCN` | `DetalleTransito` | Cada partida declarada. `DTCNTPOO` tipo 1-4; `DTCNESTD` 1 PENDIENTE, 2 SALDADA; `CNCLCDGO` cierre que la declaró; `DTCNCNSL` cierre que la saldó; ancla: `DTCNDTAS` (línea de asiento, tipos 1/2) o `DTCNIDEX` (línea de extracto, tipos 3/4). |
| `TSR.CNCT` | `ConciliacionContable` | La revisión de una cuenta/período. `CNCTESTR`: 1 PENDIENTE, 2 VERIFICADO, 3 CON_DIFERENCIAS. Contadores `CNCTTTGR/CNCTPDEX/CNCTPDAS`; `CNCTUSVR/CNCTFCVR` verificación. |
| `TSR.GRCC` / `GCEX` / `GCAS` | grupos conciliados | Un grupo = N líneas de extracto (`GCEX`→`DEXB`) contra M líneas de asiento (`GCAS`→`DTAS`). `GRCCESTD` 1 activo. |
| `TSR.DEXB` / `EXBC` | extracto | Líneas y cabecera del estado de cuenta cargado. `DEXBESTR`: 1 PENDIENTE_REVISION, 2 CONCILIADA, 3 DESCARTADA. |
| `TSR.CTEB` | `ControlExtractoBancario` | El período de conciliación **de toda la empresa**: `CTEBCRRE` 1 = mes cerrado, `CTEBUSCR/CTEBFCCR`. |
| `CNT.PRDO` | `Periodo` | `PRDONMBR`, `PRDOINCO` primer día, `PRDOFNN` último día, `PJRQCDGO` empresa. |
| `CNT.DTAS` / `ASNT` | asiento | `DTASDBEE/DTASHBRR`, `DTASDSCR`, `ASNTNMAL` número alterno, `ASNTFCHA`, `ASNTESTD` (1 y 3 cuentan como vivos, igual que el DAO). `⚠️ VERIFICAR` el nombre de la FK cuenta en `DTAS` (`d.planCuenta` → `PLNNCDGO`?) y de la FK empresa en `ASNT` (`d.asiento.empresa` → `PJRQCDGO`?). |

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
grupo activo. Las consultas de §3.3/§3.4 replican eso. `⚠️` El DAO de asiento ancla el arrastre por
`dt.movimientoBanco.asiento`; este reporte ancla por `DTCNDTAS` (§7bis del diseño, la línea exacta).
Si en producción hay partidas viejas con `DTCNDTAS` NULL y `MVCBCDGO` lleno, el reporte no las
arrastra — decidir entonces si se agrega el `OR` por `MVCB`.

---

## 2. `RPRT_CNCL_CNTA` — Conciliación de una cuenta bancaria

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
  LEFT JOIN TSR.CNCL ci ON ci.CNCLCDGO = (SELECT MAX(x.CNCLCDGO) FROM TSR.CNCL x
                                          WHERE x.CNBCCDGO = c.CNBCCDGO AND x.CNCLPRDO = p.PRDOCDGO
                                            AND x.CNCLESTD = 2)
  LEFT JOIN TSR.CNCT cc ON cc.CNBCCDGO = c.CNBCCDGO AND cc.PRDOCDGO = p.PRDOCDGO
  LEFT JOIN TSR.EXBC ex ON ex.EXBCCDGO = (SELECT MAX(y.EXBCCDGO) FROM TSR.EXBC y
                                          WHERE y.CNBCCDGO = c.CNBCCDGO AND y.PRDOCDGO = p.PRDOCDGO)
  LEFT JOIN TSR.CTEB ct ON ct.PJRQCDGO = p.PJRQCDGO AND ct.PRDOCDGO = p.PRDOCDGO
 WHERE c.CNBCCDGO = $P{P_CNBC_CODIGO}
```

Los `MAX(...)` en los `LEFT JOIN` son a propósito: garantizan **una sola fila** aunque haya un
cierre anulado y otro vigente, o dos archivos de extracto cargados. `⚠️ VERIFICAR` que
`selectCierreVigente` use el mismo criterio (CERRADO más reciente); si toma otro, replicarlo.
`⚠️ VERIFICAR` si `EXBC` debe filtrarse por `EXBCESTD` (no sé qué valores toma).

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
 WHERE da.PLNNCDGO = (SELECT c.PLNNCDGO FROM TSR.CNBC c WHERE c.CNBCCDGO = $P{P_CNBC_CODIGO})  -- ⚠️ VERIFICAR FK
   AND a.PJRQCDGO = p.PJRQCDGO                                                                   -- ⚠️ VERIFICAR FK
   AND a.ASNTESTD IN (1, 3)
   AND (   a.ASNTFCHA BETWEEN p.PRDOINCO AND p.PRDOFNN
        OR da.DTASCDGO IN (SELECT t.DTCNDTAS FROM TSR.DTCN t WHERE t.DTCNTPOO IN (1,2) AND t.DTCNESTD = 1))
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
       CASE WHEN EXISTS (SELECT 1 FROM TSR.DEXB d WHERE d.CNBCCDGO = c.CNBCCDGO AND d.DEXBESTD = 1
                            AND d.DEXBFTRN BETWEEN p.PRDOINCO AND p.PRDOFNN)
            THEN 'S' ELSE 'N' END                    AS EXTRACTO_CARGADO,   -- ⚠️ VERIFICAR contra selectCuentasConCobertura
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
  JOIN TSR.CNBC c  ON c.CNBCESTD = 1                 -- ⚠️ VERIFICAR: replicar selectCuentasBancariasActivas(idEmpresa)
  JOIN TSR.BNCO b  ON b.BNCOCDGO = c.BNCOCDGO
  JOIN CNT.PLNN pc ON pc.PLNNCDGO = c.PLNNCDGO
  LEFT JOIN TSR.CNCL ci ON ci.CNCLCDGO = (SELECT MAX(x.CNCLCDGO) FROM TSR.CNCL x
                                          WHERE x.CNBCCDGO = c.CNBCCDGO AND x.CNCLPRDO = p.PRDOCDGO
                                            AND x.CNCLESTD = 2)
  LEFT JOIN TSR.CNCT cc ON cc.CNBCCDGO = c.CNBCCDGO AND cc.PRDOCDGO = p.PRDOCDGO
  LEFT JOIN TSR.CTEB ct ON ct.PJRQCDGO = p.PJRQCDGO AND ct.PRDOCDGO = p.PRDOCDGO
 WHERE p.PRDOCDGO = $P{P_PRDO_CODIGO}
   AND p.PJRQCDGO = $P{P_PJRQ_CODIGO}
 ORDER BY b.BNCONMBR, c.CNBCNMRO
```

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
