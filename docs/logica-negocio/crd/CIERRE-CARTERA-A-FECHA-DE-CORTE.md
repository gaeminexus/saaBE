# Cierre de cartera a fecha de corte (C15, cuadre Superintendencia)

2026-10-28 · rama `cuadre/c15-cierre-cartera-corte`. Resuelve los huecos H1–H6 de la auditoría C14
(`cuadreSuper/03-trabajo/cartera/CONTABILIDAD-CARTERA.md` §C) con las decisiones D-011, D-015, D-016, D-017 y D-018
del proyecto. Objetivo: re-procesar cada mes desde jun-2025 y que **contabilidad = estructuras G = reportes internos =
módulo** al centavo en cartera, provisión e intereses.

## 1. Diseño

### 1.1 Modo "a fecha de corte" (H1)

`com.saa.ejb.crd.service.ModoCorteCartera` decide, en un solo lugar, cuándo los procesos leen la cartera **como estaba
al corte** (último día del mes) en vez del estado de hoy:

| Cuándo | Cómo |
|---|---|
| Automático | corte **anterior** al último día del mes anterior a hoy (hoy 28-oct-2026: hasta ago-2026 es al corte; sep-2026 y oct-2026 siguen como siempre) |
| Explícito en el cierre | `estadoAlCorte: true/false` en la solicitud |
| Global | propiedad de sistema `saa.cartera.alCorte` = `siempre` / `nunca` / `auto` |

Regla de corte (la de C3 §2.1 y la cuadratura C14), en todas las consultas del cierre (`CierreCarteraDaoService`, variantes
con `corte`), del ⑦ (`selectCuotasProvisionablesAlCorte`, `selectDatosPagosVigentesAlCorte`, MVIC con
`fechaContable <= corte`), de la G48 y del CCPM:

- préstamo en el universo: `PRSTFCHA <= corte` y estado no 0/1/6/7/12 (nunca aprobado o anulado);
- cuota liquidada al corte: `DTPRESTD IN (4,7)` **y** `DTPRFCPG <= corte` (pagada después o sin fecha = impaga);
- pagado al corte: pagos vigentes con `PGPRFCHA <= corte`; capital = `PGPRSLOT` si > 0, si no `PGPRCPPG`
  (regla de la cuadratura; ojo, en la base hay 3.394 pagos DEP y 506 MIGRACION con los dos campos > 0: **sumarlos** como
  pedía el menor 6 duplicaría capital, por eso no se hizo);
- mora: fórmula del proceso diario (`calcularMoraCuota`: capital de la cuota × tasa nominal (9 si falta) / 36000 × días
  desde el vencimiento) menos la mora pagada al corte — no lee `DTPRMRAA`;
- condonación de interés de plazo vencido (D25/D27): solo si la declaración ya existía al corte.

**Convención de vencimiento** (`saa.cartera.corteInclusivo`, o `corteInclusivo` en la solicitud; por defecto `true`):
todas las cuotas vencen el último día del mes. Con `true` una cuota que vence el día del corte e impaga es **vencida**
(1 día) — la convención de la G48 de Delta aceptada en mayo 2025 y de la cuadratura C14 (`vto <= corte`). Con `false`, la
regla del SAA del 4-sep-2026 (el día del vencimiento aún es por vencer). Solo aplica al corte; el modo normal no cambia.
**Decisión pendiente del líder** (ver §5).

Verificado contra la base (corte 31-may-2025): 1.594 operaciones, por vencer 20.160.441,26, vencido 1.337.020,68, interés
vencido 955.703,64 = módulo (b) de la cuadratura C14 al centavo.

### 1.2 Sub-procesos del cierre en modo al corte

| # | Sub-proceso | Fecha del asiento | Cambio |
|---|---|---|---|
| ① | Vencidos | **último día del mes que cierra** (H3) | universo al corte; con la convención inclusiva cruza el rango (corte anterior, corte] |
| ② | Cambio de bandas por vencer | **corte** (H3) | compara corte anterior → corte (el mismo par que ①.1), no corte → fin del mes siguiente |
| ①.1 | Reclasificación vencida | **corte** (H3) | universo al corte |
| ③ | Apertura del mes siguiente | 1.º del mes siguiente (sin cambio) | cobrable medido al corte |
| ④ | Devengo de intereses del mes siguiente | 1.º del mes siguiente (sin cambio) | medido al corte |
| ⑥ | Neteo | corte (sin cambio) | cobrable al corte; sin la transición D27 |
| ⑩ | **Devengo de mora al corte** (D-017, H4) — nuevo | corte | mora bruta al corte de cada cuota vencida impaga − lo ya reconocido en MVIC (tipo 5 − 6): D `1.4.02.xx` / H `5.1.02.xx` por tipo (plantilla 17, papeles 20/40); MVIC tipo 5 origen CIERRE por cuota |
| ⑦ | Provisión de intereses | corte (sin cambio) | cuotas, pagos y libro MVIC al corte |
| ⑧ | **Provisión de cartera** (D-015/D-017, H2) — nuevo | corte | ajuste a saldo: provisión requerida al corte (`CarteraAlCorteService`, regla del SAA, el mismo cálculo de la G48) − saldo acreedor de la cuenta de provisión al corte. Sube: D `4.7.04.05/10/15` / H `1.3.99.05/10/15`. Baja: D `1.3.99.xx` / H `5.3.90.05` REVERSION DE PROVISIONES. Plantilla 37 (`sql/332`); sin plantilla se omite con advertencia |
| ⑨ | **Ajuste de bandas a saldo** (D-011) — nuevo | corte | cada cuenta `1.3.xx.yy` (sin 1.3.99) se lleva al capital del módulo al corte, contando lo que ya movieron ①/②/①.1; por familia (Q 01–04, P 05–08, H 09–12) el asiento suma cero y la **diferencia neta del grupo queda en la banda .25** (`1.3.01.25`, `1.3.05.25`, `1.3.09.25`) con una advertencia que dice el importe |

Fuera del modo al corte el cierre es **idéntico** al de antes (fechas, pares de fechas, universo), salvo que el ⑩ se puede
activar en producción con `-Dsaa.cartera.devengoMoraCierre=true` cuando el contador lo decida.

La mora del ⑩ se cuenta desde `max(vencimiento, saa.cartera.inicioDevengoMora)`. **Recomendado**
`-Dsaa.cartera.inicioDevengoMora=2025-05-31`: la mora anterior al ancla no estaba en el balance de Delta (C14 §2.3); sin
esa fecha el primer ⑩ (junio) devengaría de golpe toda la mora histórica (~595.000 al 31-may).

### 1.3 Mora cobrada (D-017, H4)

`ProvisionInteresService.separarMoraNoDevengada`, llamado desde el ③ de Petro (`CobroPetroContableServiceImpl`) y desde
`ContabilizacionIndividualCreditoService.haberDesdePagos` (CBCR, pago con aportes, abono, precancelación, condonación): por
cuota, la mora ya devengada pendiente (`MVIC 5 − 6` − mora pagada por otros pagos vigentes) se acredita a `1.4.02.xx`; el
**excedente** (mora del mes en curso, no devengada) va a `5.1.02.xx` del tipo y se registra como MVIC tipo 5 origen COBRO
(así el ⑩ no lo vuelve a devengar). Anular el pago anula ese MVIC. `-Dsaa.cartera.moraNoDevengadaAIngreso=false` vuelve al
comportamiento anterior. El motor de pagos (`MotorPagoPrestamoServiceImpl`) no asienta: no se tocó. No se tocó `ejb/asoprep`.

### 1.4 Provisión de cartera, G48 y CCPM (D-015)

`CarteraAlCorteService.calcular(corte, inclusivo)` = una fila por operación con saldo al corte: por vencer, vencido, días de
mora, interés vencido pendiente, mora, interés de la cuota del mes, cuenta individual (aportes **vigentes** `TPAPIDST = 1` con caja
<= corte — §1.7 —, solo en la primera operación del partícipe, 0 si jubilado complementario), VSAP = max(0, saldo − cuenta
individual), calificación y % de `CRD.CFCR/ESCR` vigentes al corte, **requerida = VSAP × %** y **constituida = requerida**
(el ⑧ deja `1.3.99` en ese valor a la misma fecha: B17 = G48).

- **G48** (`GeneracionG48ServiceImpl`): un mes pasado se genera desde ese cálculo (`generarAlCorte`). Interés ordinario =
  pendiente de las cuotas vencidas, o el de la cuota del mes si la operación está al día (convención grupo 2 / grupo 1).
- **CCPM** (`GeneracionCCPMServiceImpl`): universo y bandas con el DAO del cierre al corte; calificación, provisión, interés,
  mora y cuenta individual del mismo cálculo.
- `POST /ejrc/ejecutar` acepta `"regenerar": true`: crea una ejecución nueva aunque el mes tenga una (la anterior queda;
  todas las lecturas y la cuadratura toman la más reciente). Los reportes internos ya tenían `POST /ejcc/regenerar`.

### 1.5 Corrida de arranque (H5, D-016)

`POST /cierrecartera/ejecutar` con `"arranque": true` sobre el **mes del ancla** (mayo 2025):

- ⑨ a nivel de **cuenta madre** (`1.3.01`, `1.3.03`, …): reparte la banda `.25` (D-001) entre las bandas reales al 31-may
  **sin cambiar ninguna cuenta madre**, o sea sin cambiar el B17 aceptado; la diferencia de la madre (las 90 tablas
  distintas, −450.529,49 en total) queda en su `.25`;
- ③ y ④ de junio (abre las planillas que netea el ⑥ de junio);
- semilla del libro MVIC: reparte la provisión de intereses de la apertura (`montoProvisionInteresesApertura`, por defecto
  el saldo acreedor de `1.4.99.05` al corte = 809.747,62) entre las cuotas, en proporción a lo provisionable por el SAA al
  31-may, como MVIC tipo 1 origen APERTURA sin asiento; el residuo de redondeo va a la cuota mayor. El primer ⑦ solo
  provisiona la diferencia;
- la `1.3.99` de la apertura (2.424.206,67) **no necesita semilla**: el ⑧ es ajuste a saldo (en junio asienta
  requerida SAA − 2.424.206,67).

Sin ①/②/①.1/⑥/⑦/⑧/⑩ y sin control de archivo Petro. Se reversa como cualquier corrida
(`POST /cierrecartera/reversar/{id}`: anula asientos y MVIC 1/5/7 de la corrida).

### 1.6 Desembolsos sin asiento de entrega (H6, D-018)

- `GET /prst/sinAsientoEntrega?desde=2025-06-01&hasta=2025-06-30` — préstamos aprobados sin `PRSTASEN`.
- `POST /prst/contabilizarDesembolso` / `POST /prst/contabilizarDesembolsos` (lote, cada préstamo en su transacción):
  asiento de entrega del motor (`contabilizarEntrega`, ahora con fecha explícita) + orden de pago en CxP desde la cuenta del
  extracto como débito ya realizado (nace CONFIRMADA y genera el egreso con su movimiento bancario), todo con la fecha del
  desembolso real. Idempotente: no repite asiento ni orden; rechaza sustituciones y préstamos no aprobados.
- **C16 / D-021 — desembolso neto:** `valor` es el **neto** que transfirió el banco; el ítem admite
  `idPrestamoCancelado` (+ `capitalCancelado`, `interesCancelado`, `seguroCancelado`, opcionales y verificados contra
  `CRD.DTPR`) y `otrosDescuentos`. Regla: bruto = neto + capital + interés + seguro + otros, al centavo. Orden: entrega
  por el bruto → precancelación del préstamo anterior con el motor, pagada contra socios por pagar del nuevo → otros
  descuentos → orden de pago y egreso por el neto. La respuesta del lote trae `items`, `controlPorReferencia` (suma de
  netos por referencia bancaria) y `totales`. Detalle, cuentas y SELECT de control:
  **`DESEMBOLSO-NETO-CON-PRECANCELACION.md`**. Sin precancelación ni descuentos sigue siendo `valor = monto`.
- Lote de junio 2025 (29 préstamos reales): `cuadreSuper/03-trabajo/cartera/resultados/02-lote-desembolsos-2025-06-body.json`.

```json
{ "idEmpresa": 1236, "idUsuario": 38, "usuario": "USUARIO 1",
  "items": [ { "idPrestamo": 7975, "fechaContable": "2025-06-09", "idCuentaBancaria": 418, "valor": 3731.13,
               "referencia": "PACIFICO AHO 2035544", "idPrestamoCancelado": 5429, "capitalCancelado": 21585.65,
               "interesCancelado": 161.89, "seguroCancelado": 21.33 } ] }
```

### 1.7 Lectura única de aportes: solo vigentes (2026-10-28)

El reproceso de carga Petro y la corrida de ancla anulan un aporte dejando un **par** que suma cero: el original
marcado `APRTIDST = 0` (valor intacto) y su contra-movimiento `APRTTPMV = 5` con glosa `… - reverso APRT <id>`
(misma entidad, tipo, fecha de caja y periodo efectivo; `REPROCESO-CARGA-PETRO.md` §4.3). Una suma neta sin filtros
ya da bien, pero toda lectura que filtre por **signo**, por **tipo de movimiento** o que **cuente meses** rompe el
par. Era el caso del cierre: `selectAportesRegistrados` (tipos de movimiento 1 y 8, valor > 0) contaba el esquema
viejo de la carga 352 y en junio 2025 daba **322.509,73** registrados en vez de **161.802,91**.

Regla única, `com.saa.ejb.crd.daoImpl.AporteVigenteSql` (`VIGENTE_SQL` nativo / `VIGENTE_JPQL`):

```sql
NVL(a.APRTIDST, 4) <> 0 AND NOT (NVL(a.APRTTPMV, 0) = 5 AND NVL(a.APRTGLSA, ' ') LIKE '%reverso APRT %')
```

**Se excluyen los dos (marcado y contra) en vez de netear** porque es lo mismo en una suma neta (verificado en la
base: 3.331 pares, suma total y al 30-jun-2025 idénticas con y sin la regla) y además funciona fila a fila, que es lo
que necesitan los conteos y los filtros de signo; netear obligaría a reconstruir el par en cada consulta. Las otras 13
filas tipo 5 (reversos de eventos, devoluciones anuladas, cobros) no llevan la marca y siguen contando. La contra se
reconoce por la glosa porque `CRD.APRT` no tiene columna que apunte al original; el único productor es
`ReprocesoCargaPetroServiceImpl` y el control (C) de §4 verifica que marca y glosa sigan emparejadas.

Dónde se aplica (todas las lecturas de este frente pasan por la constante):

| Lectura | Uso | Efecto medido |
|---|---|---|
| `CierreCarteraDaoServiceImpl.selectAportesRegistrados` | ⑥ neteo, control Petro, aviso de exceso | **junio 322.509,73 → 161.802,91** |
| `CierreCarteraDaoServiceImpl.selectOperacionesAlCorte` (CTE `ci`) | cuenta individual de `CarteraAlCorteService` (⑧, G48, CCPM) | neutro (suma neta) |
| `AporteDaoServiceImpl.selectCountImposicionesPersonales/PatronalesPorEntidad` | G43 | 18 partícipes con 1 mes de más (personal), 41 (patronal) |
| `AporteDaoServiceImpl.selectSumaAportesNegativosMesPorEntidad` | G43 | dejaba las contras como "negativos" del mes |
| `AporteDaoServiceImpl.selectCountImposicionesJubilacionPorEntidad` | G44 | meses de más, como la G43 |
| `AporteDaoServiceImpl.selectSumaRendimiento/Patronal/PersonalPorEntidad`, `selectTiposAportePorEntidad` | G42 | neutro |
| `AporteDaoServiceImpl.selectSumaSaldoCuentaJubilacionPorEntidad`, `selectSumaAportesTipo23EnRango` | G44, CJBM | neutro |
| `AporteDaoServiceImpl.selectSumaPorEntidadYTipoAporte`, `selectSumaTotalPorTipoAporte` | CPRM, G40 (comentado) | neutro |
| `AporteDaoServiceImpl.sumaAportesPositivosPorTipoYPeriodo` | **producción**: marca de mora en la carga Petro | un aporte anulado hacía ver "aportó" a quien no |
| `AporteDaoServiceImpl.selectUltimaFechaAportePorEntidad` | **producción**: meses a cobrar a morosos en la generación Petro (y el esperado del cierre) | ídem |
| `AporteDaoServiceImpl.selectFechaUltimoMovimientoNegativo` / `selectFechaMovimientoJubilacion` | **producción**: ancla del retroactivo de pensión complementaria | una contra podía pasar por "último negativo" |
| `EntidadDaoServiceImpl.selectPadronParticipes` | **producción**: padrón (número de aportes, último mes) | meses de más |

Las cuatro lecturas de producción tenían el mismo defecto (contar el marcado) y se corrigieron igual: fuera del modo al
corte el cierre y los reportes cambian solo por eso. Quedan sin la constante, a propósito, las sumas netas por
partícipe/carga que ya cuadran por construcción (`SaldoAporteService`, `sumValor*`, `*ByCarga`,
`AportadoGeneracionDaoServiceImpl`) y los tableros con filtro de estado explícito.

## 2. Antes del primer mes (una vez)

1. Correr `docs/logica-negocio/crd/sql/332_C15_PLANTILLA_37_Y_SUBPROCESOS_8_9_10.sql` (bloques 0 → C): plantilla 37 y
   `CK_ANCC_TPOO` 1..10.
2. Propiedades de WildFly del entorno de re-proceso: `-Dsaa.cartera.inicioDevengoMora=2025-05-31` (recomendado);
   `saa.cartera.alCorte` y `saa.cartera.corteInclusivo` por defecto.
3. Mora persistida al ancla: `POST /prst/calcularMora?fecha=2025-05-31&usuario=CUADRE` (no la usa el cierre al corte,
   pero sí el motor de pagos al re-aplicar junio).
4. Arranque:

```
POST /SaaBE/rest/cierrecartera/previsualizar
{ "idEmpresa": 1236, "anio": 2025, "mes": 5, "usuario": "CUADRE", "arranque": true }
POST /SaaBE/rest/cierrecartera/ejecutar      (mismo body)
```

Control: el B17 de mayo no cambia (por cuenta madre); bloque 04 de la cuadratura al 31-may = 0 en toda cuenta salvo las
`.25`; `SELECT SUM(MVICVLOR) FROM CRD.MVIC WHERE MVICORGN = 'APERTURA' AND MVICANUL = 0` = 809.747,62.

## 3. Orden para un mes M (junio 2025: `anio=2025, mes=6`, corte 2025-06-30)

| Paso | Qué | Endpoint |
|---|---|---|
| 1 | Desembolsos de M sin asiento (D-018, neto D-021), **antes** del reproceso Petro | `GET /prst/sinAsientoEntrega?desde=2025-06-01&hasta=2025-06-30` → lote neto (`02-lote-desembolsos-2025-06-body.json`) → `POST /prst/contabilizarDesembolsos` |
| 2 | Transferencia Petro, confirmación y re-proceso de la carga (C4, D-010) | `POST /asgn/transferencias`, `POST /asgn/confirmarRecepcion/{id}`, `POST /crar/reprocesar/{idCarga}` |
| 3 | Otros cobros de M (CBCR) con fecha de afectación en M | `POST /cbcr/registrar` → `/aprobar` → `/procesar` |
| 4 | Previsualizar el cierre (sale "Cierre A FECHA DE CORTE" en advertencias; ⑧ y ⑨ dicen requerida/constituida y diferencias por grupo) | `POST /cierrecartera/previsualizar` `{ "idEmpresa": 1236, "anio": 2025, "mes": 6, "usuario": "CUADRE" }` |
| 5 | Ejecutar | `POST /cierrecartera/ejecutar` (mismo body; `estadoAlCorte` explícito si se quiere forzar) |
| 6 | Estructuras G (regenerar) | `POST /ejrc/ejecutar` `{ "mes": 6, "anio": 2025, "usuario": "CUADRE", "regenerar": true }` |
| 7 | Reportes internos | `POST /ejcc/regenerar` `{ "codigoEjecucion": 361, "usuario": "CUADRE" }` (o `/ejcc/ejecutar` si no hay) |
| 8 | B17 | `node 03-trabajo/fase1-balance-arranque/scripts/09-genera-b17-desde-saa.js` |
| 9 | Cuadratura | `node 03-trabajo/cartera/scripts/correr-sql.js 03-trabajo/cartera/sql/01-cuadratura-cartera.sql --corte 2025-06-30` + el SELECT de control (§4) |

Ya no hacen falta los pasos 1 y 6 de mora de `CIERRE-CARTERA-DE-UN-MES.md` para el cierre (lee la fórmula al corte), ni el
asiento manual de provisión del paso 9 (lo hace el ⑧), ni borrar la EJRC vieja (paso 10: `regenerar`).

## 4. SELECT de control (solo lectura; reemplazar la fecha)

```sql
-- Contabilidad vs módulo vs G48 al corte: capital por vencer/vencido, 1.3.99 vs provisión de la G48 (requerida y
-- constituida). Todas las diferencias en 0,00 (salvo la .25 de cada familia, que muestra la diferencia explicada).
WITH P AS (SELECT DATE '2025-06-30' AS CORTE FROM DUAL),
CN AS (SELECT CASE WHEN D.DTASCNTA LIKE '1.3.99%' THEN 'PROVISION'
                   WHEN SUBSTR(D.DTASCNTA,5,2) IN ('04','08','12') THEN 'VENCIDO' ELSE 'POR VENCER' END CONCEPTO,
              SUM(D.DTASDBEE - D.DTASHBRR) SALDO
       FROM CNT.DTAS D JOIN CNT.ASNT A ON A.ASNTCDGO = D.ASNTCDGO, P
       WHERE A.PJRQCDGO = 1236 AND A.ASNTESTD IN (1,3) AND A.ASNTFCHA < P.CORTE + 1 AND D.DTASCNTA LIKE '1.3.%'
       GROUP BY CASE WHEN D.DTASCNTA LIKE '1.3.99%' THEN 'PROVISION'
                     WHEN SUBSTR(D.DTASCNTA,5,2) IN ('04','08','12') THEN 'VENCIDO' ELSE 'POR VENCER' END),
G AS (SELECT SUM(C.CG48VPVN) PV, SUM(C.CG48VLVN) VV, SUM(C.CG48PRRO) PRRO, SUM(C.CG48PRCN) PRCN FROM RPR.CG48 C, P
      WHERE C.CG48EJRD = (SELECT MAX(D.EJRDCDGO) FROM RPR.EJRD D JOIN RPR.EJRC E ON E.EJRCCDGO = D.EJRCCDGO
                          WHERE E.EJRCANOO = EXTRACT(YEAR FROM P.CORTE) AND E.EJRCMESS = EXTRACT(MONTH FROM P.CORTE) AND D.EJRDTPRP = 'G48'))
SELECT 'POR VENCER' CONCEPTO, ROUND((SELECT SALDO FROM CN WHERE CONCEPTO='POR VENCER'),2) CONTABILIDAD, ROUND(G.PV,2) G48,
       ROUND((SELECT SALDO FROM CN WHERE CONCEPTO='POR VENCER') - G.PV,2) DIF FROM G
UNION ALL SELECT 'VENCIDO', ROUND((SELECT SALDO FROM CN WHERE CONCEPTO='VENCIDO'),2), ROUND(G.VV,2),
       ROUND((SELECT SALDO FROM CN WHERE CONCEPTO='VENCIDO') - G.VV,2) FROM G
UNION ALL SELECT 'PROVISION 1.3.99 vs PRRO', ROUND(-(SELECT SALDO FROM CN WHERE CONCEPTO='PROVISION'),2), ROUND(G.PRRO,2),
       ROUND(-(SELECT SALDO FROM CN WHERE CONCEPTO='PROVISION') - G.PRRO,2) FROM G
UNION ALL SELECT 'PROVISION 1.3.99 vs PRCN', ROUND(-(SELECT SALDO FROM CN WHERE CONCEPTO='PROVISION'),2), ROUND(G.PRCN,2),
       ROUND(-(SELECT SALDO FROM CN WHERE CONCEPTO='PROVISION') - G.PRCN,2) FROM G;
```

El módulo (b) y el CCPM (d) se comparan con los bloques 03/04/05/10/11 de la cuadratura C14, que ya aplica la misma regla
de corte y la convención inclusiva (`vto <= corte`). Diferencias esperadas mientras no se decidan: las 90 tablas distintas
a mayo (en la `.25`, C3 P2) y los desembolsos de H6 aún sin asiento (en la `.25` y en el total). Matiz conocido: la mora del
bloque 03 de la cuadratura usa capital **pendiente** × días; la del SAA (y la G48 al corte) capital **de la cuota** × días
(`calcularMoraCuota`): al 31-may 580.951,26 vs 595.273,80. Si se quiere igualdad exacta en esa fila, alinear la consulta
de la cuadratura (o decidir la fórmula).

**Aportes del mes (§1.7):** vigentes netos por tipo de movimiento contra lo que el cierre reporta como registrados,
la contabilidad de las cuentas de aportes y la integridad de la marca.

```sql
-- (A) Aportes 9/11 con caja en el mes, por tipo de movimiento: vigente neto, par anulado excluido y lo que el
--     cierre cuenta como "registrados" (fila total = desglose.registrado / registradoJubilacion / registradoCesantia).
WITH P AS (SELECT DATE '2025-06-01' AS DESDE, DATE '2025-06-30' AS HASTA FROM DUAL),
A AS (SELECT a.APRTTPMV TPMV, a.TPAPCDGO TIPO, a.APRTVLRR VALOR,
             CASE WHEN NVL(a.APRTIDST, 4) <> 0
                   AND NOT (NVL(a.APRTTPMV, 0) = 5 AND NVL(a.APRTGLSA, ' ') LIKE '%reverso APRT %') THEN 1 ELSE 0 END VIGENTE
      FROM CRD.APRT a, P
      WHERE a.TPAPCDGO IN (9, 11) AND TRUNC(a.APRTFCTR) BETWEEN P.DESDE AND P.HASTA)
SELECT TPMV, COUNT(*) FILAS,
       ROUND(SUM(CASE WHEN VIGENTE = 1 THEN VALOR ELSE 0 END), 2) VIGENTE_NETO,
       ROUND(SUM(CASE WHEN VIGENTE = 0 THEN VALOR ELSE 0 END), 2) EXCLUIDO_PAR_ANULADO,
       ROUND(SUM(CASE WHEN VIGENTE = 1 AND TPMV IN (1, 8) AND VALOR > 0 THEN VALOR ELSE 0 END), 2) REGISTRADO_CIERRE,
       ROUND(SUM(CASE WHEN VIGENTE = 1 AND TPMV IN (1, 8) AND VALOR > 0 AND TIPO = 9 THEN VALOR ELSE 0 END), 2) REG_JUBILACION,
       ROUND(SUM(CASE WHEN VIGENTE = 1 AND TPMV IN (1, 8) AND VALOR > 0 AND TIPO = 11 THEN VALOR ELSE 0 END), 2) REG_CESANTIA
FROM A GROUP BY ROLLUP(TPMV) ORDER BY TPMV NULLS LAST;

-- (B) Contabilidad: movimiento del mes y saldo al corte de aportes por cobrar (1.4.05.05), aportes por aplicar
--     (2.3.02.05) y aportes personales (2.1.01.05.01 cesantía, 2.1.02.05.01 jubilación). Saldo DEUDOR.
WITH P AS (SELECT DATE '2025-06-01' AS DESDE, DATE '2025-06-30' AS HASTA FROM DUAL)
SELECT D.DTASCNTA CUENTA,
       ROUND(SUM(CASE WHEN A.ASNTFCHA >= P.DESDE THEN NVL(D.DTASDBEE,0) - NVL(D.DTASHBRR,0) ELSE 0 END), 2) MOVIMIENTO_MES,
       ROUND(SUM(NVL(D.DTASDBEE,0) - NVL(D.DTASHBRR,0)), 2) SALDO_AL_CORTE
FROM CNT.DTAS D JOIN CNT.ASNT A ON A.ASNTCDGO = D.ASNTCDGO, P
WHERE A.PJRQCDGO = 1236 AND A.ASNTESTD IN (1, 3) AND A.ASNTFCHA < P.HASTA + 1
AND (D.DTASCNTA LIKE '1.4.05.05%' OR D.DTASCNTA LIKE '2.3.02.05%'
     OR D.DTASCNTA LIKE '2.1.01.05.01%' OR D.DTASCNTA LIKE '2.1.02.05.01%')
GROUP BY D.DTASCNTA ORDER BY D.DTASCNTA;

-- (C) Integridad de la marca (los dos en 0; si no, la regla de §1.7 deja de ser equivalente a netear).
SELECT (SELECT COUNT(*) FROM CRD.APRT o WHERE o.APRTIDST = 0 AND NOT EXISTS (
          SELECT 1 FROM CRD.APRT c WHERE c.APRTTPMV = 5 AND c.APRTGLSA LIKE '%reverso APRT ' || o.APRTCDGO || '%'
            AND c.ENTDCDGO = o.ENTDCDGO AND c.TPAPCDGO = o.TPAPCDGO AND c.APRTVLRR = -o.APRTVLRR)) MARCADOS_SIN_CONTRA,
       (SELECT COUNT(*) FROM CRD.APRT c WHERE c.APRTTPMV = 5 AND c.APRTGLSA LIKE '%reverso APRT %' AND NOT EXISTS (
          SELECT 1 FROM CRD.APRT o WHERE o.APRTIDST = 0
            AND o.APRTCDGO = TO_NUMBER(REGEXP_SUBSTR(c.APRTGLSA, 'reverso APRT ([0-9]+)', 1, 1, NULL, 1)))) CONTRAS_SIN_MARCADO
FROM DUAL;
```

Resultado en la base de trabajo, junio 2025 (28-oct-2026):

| (A) tpmv | filas | vigente neto | par anulado excluido | registrado del cierre |
|---|---|---|---|---|
| 1 aporte mensual | 5.283 | 161.257,56 | 160.706,82 | 161.257,56 |
| 5 contra-movimiento | 2.640 | 0,00 | −160.706,82 | — |
| 6 migrado/otros | 42 | −39.520,67 | 0 | — |
| 8 excedente Petro (D-010) | 3 | 545,35 | 0 | 545,35 |
| **total** | 7.968 | 122.282,24 | 0 | **161.802,91** (jub. 61.843,74 · ces. 99.959,17) |

(B) aplicación de la carga 352 (asiento 12277): D 2.3.02.05 **161.802,91** / H 2.1.01.05.01 99.959,17 + H 2.1.02.05.01
61.843,74 — **igual al registrado del cierre, por tipo, al centavo**. (C): 0 y 0.

Fila 6: −42.029,89 de cruce aportes→préstamos ("POR MIGRACION Y DEPURACION JUN … PAGO PRESTAMOS", re-fechados por la
corrida de ancla) y +2.509,22 de aportes manuales de junio, **sin asiento en junio** todavía: son del paso 3 (otros
cobros / pago con aportes), no del cierre. Mientras no se asienten, la cuenta individual del B17 (movimiento de junio
−161.802,91) y la de la G42 (vigente neto −122.282,24 de movimiento) difieren en esos 39.520,67.

### 4.1 Registrado mayor que el esperado: qué hace hoy el cierre y qué se propone

**De dónde sale el esperado.** No de `VigenciaContratoService.esperadoEnLotePorFilial`: el rubro 242 (generación
por faltante) está en **0** en la base, así que `calcularAportesEsperados` usa el camino viejo — `HistorialSueldo`
estado 99 **vigente hoy** de los partícipes **hoy** ACTIVO/ACTIVO_EN_MORA, × meses adeudados de los morosos (último
aporte vigente antes del 1.º del mes). Las vigencias que la migración dejó empezando en jul-2025 (D-022) no
intervienen. Es el mismo cálculo con el que el ③ de la corrida de arranque abrió junio (asiento 12193:
**122.487,88**), así que ③ y ⑥ coinciden, que es la invariante del diseño; pero no es la planilla de junio 2025, es la
de hoy proyectada hacia atrás. **No encender el rubro 242 durante el re-proceso**: con él el esperado de un mes ya
cobrado es el *faltante acumulado a hoy*, casi cero después del reproceso, y el aviso de exceso crecería.

**Lo que hace el ⑥ hoy** (decisión del 2026-08-31, opción b): `noCobrado = max(esperado − registrado, 0)`. Con la
lectura corregida junio da 122.487,88 − 161.802,91 = −39.315,03 → **el ⑥ no lleva aportes** (antes del arreglo
tampoco: −200.021,85 también se iba a 0). **El total del ⑥ de la previsualización (3.066.240,81) no cambia**: es todo
préstamos no cobrados. Lo único que cambia es el aviso: registrados **161.802,91** contra esperados **122.487,88**,
exceso **39.315,03** (antes 322.509,73 / 200.021,85).

**No existe un "proceso de cobro en exceso" que recoja esa diferencia.** El §3.7 del levantamiento es la devolución a
un partícipe que pagó de más (D 2.3.02.05 / H 2.3.01.15.04 y luego al banco); aquí lo registrado ya se aplicó a la
cuenta individual (asiento de aplicación), no hay nada que devolver, y los excedentes reales por partícipe ya los
resolvió la carga (D-010, tipo 8, 545,35). Lo que queda son las dos transitorias con signo contrario a su
naturaleza:

| Cuenta | ③ 01-jun (arranque) | Reparto 352 (30-jun) | Aplicación 352 (30-jun) | ⑥ hoy | Saldo 30-jun |
|---|---|---|---|---|---|
| 1.4.05.05 aportes por cobrar | D 122.487,88 | H 162.004,30 | | — | **−39.516,42 (acreedor)** |
| 2.3.02.05 aportes por aplicar | H 122.487,88 | | D 161.802,91 | — | **+39.315,03 (deudor)** |
| 2.1.0x.05.01 aportes personales | | | H 161.802,91 | — | = registrado |

**¿Rompe B17 = G42?** No en la cuenta individual: 2.1.0x.05.01 recibe exactamente el registrado vigente (control B),
haga lo que haga el ⑥. Lo que queda mal es el B17 en las transitorias: un activo con saldo acreedor y un pasivo con
saldo deudor por 39,3 mil que ningún módulo explica, y que se arrastra (cada mes el ③ abre el esperado de hoy y el ⑥
solo baja lo que falta, nunca sube lo que sobra).

**Propuesta (sin implementar: contradice la decisión del 2026-08-31, la decide el líder/contador):** que el ⑥ lleve la
diferencia **con signo**. Si registrado > esperado, las mismas dos líneas invertidas: **D 1.4.05.05 / H 2.3.02.05 por
39.315,03** (glosa "Aportes cobrados sobre lo esperado"). Junio quedaría 2.3.02.05 = **0,00** y 1.4.05.05 = **−201,39**,
que es la diferencia real entre lo descontado por Petro en AH (162.004,30) y lo aplicado (161.802,91): 746,74 de roles AH
sin partícipe (novedades) − 545,35 de excedentes que nacieron en préstamos y se aplicaron a aportes (el "desfase del
reparto" ya documentado); se va cuando se resuelvan esas novedades. El cambio es local (`armaNeteo` y el piso de
`DesgloseAportesCierre.noCobrado`), pero no lo hice porque revierte una decisión registrada. Alternativa sin tocar el
⑥: abrir el ③ de los meses re-procesados con lo descontado por Petro en AH (`CRD.DTCA` de la carga del mes) en vez del
esperado de hoy — deja las dos transitorias en el desfase de 201,39 desde el origen, pero cambia la fuente del ③, que
también es decisión del 2026-08-31.

## 5. Pendiente / decisiones

1. **Convención de vencimiento al corte** (`corteInclusivo`): por defecto la de Delta/C14 (vence el día del corte = vencida),
   contraria a la regla del SAA del 4-sep-2026. Confirmar con el líder; cambiarla es una propiedad.
2. **H6 junio — desembolsos netos**: resuelto en C16 (D-021, `DESEMBOLSO-NETO-CON-PRECANCELACION.md`): 29 préstamos
   reales netos de la precancelación del anterior cuadran con los 3 débitos de junio (72.799,95; 8.258,34; 27.098,71).
   Los 11 del 30-jun (7958–7965, 7968, 7987, 7988) no son desembolsos y quedan fuera del lote (anularlos es decisión
   aparte).
3. **Mora anterior al ancla**: con `inicioDevengoMora=2025-05-31` no se devenga; el ⑦ sin embargo provisiona la mora
   pendiente bruta (calcularMoraCuota desde el vencimiento). Revisar con el contador si la provisión de mora también debe
   contarse desde el ancla.
4. La pantalla de auditoría de bandas (`DistribucionBandaServiceImpl`, CRD.DSBN) sigue mostrando toda la mora cobrada
   contra `1.4.02`: no es contabilidad, solo la pantalla.
5. Rastro por préstamo de la provisión de cartera (PV6) no implementado: el ⑧ es por cuenta; el detalle por operación está
   en la G48 del mes.
6. Reestructuras/novaciones sin asiento (sustituciones) quedan fuera del lote H6.
7. Probar de punta a punta en `saa-oracle-23ai` con el WAR desplegado (solo se compiló y se validó la consulta por
   operación contra la base).
8. **Aportes cobrados sobre lo esperado** (§4.1): decidir si el ⑥ lleva la diferencia con signo (propuesta) o si el ③
   de los meses re-procesados abre con lo descontado por Petro. Sin decisión, junio deja 1.4.05.05 en −39.516,42 y
   2.3.02.05 en +39.315,03.
9. Movimientos de aportes tipo 6 de junio sin asiento (−42.029,89 cruce aportes→préstamos, +2.509,22 manuales;
   §4, fila 6): sin ellos la cuenta individual del B17 y la G42 difieren en 39.520,67.
