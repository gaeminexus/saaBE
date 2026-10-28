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
mora, interés vencido pendiente, mora, interés de la cuota del mes, cuenta individual (aportes `TPAPIDST = 1` con caja
<= corte, solo en la primera operación del partícipe, 0 si jubilado complementario), VSAP = max(0, saldo − cuenta
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
  desembolso real. Idempotente: no repite asiento ni orden; rechaza sustituciones y préstamos no aprobados; exige
  `valor = monto`.
- Lote del mes: `cuadreSuper/03-trabajo/cartera/scripts/02-lote-desembolsos.js --mes 2025-06` (cruce con el extracto).

```json
{ "idEmpresa": 1236, "idUsuario": 1, "usuario": "CUADRE",
  "items": [ { "idPrestamo": 7974, "fechaContable": "2025-06-09", "idCuentaBancaria": 418, "valor": 10000.00,
               "referencia": "PACIFICO AHO 2035544" } ] }
```

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
| 1 | Desembolsos de M sin asiento (D-018) | `GET /prst/sinAsientoEntrega?desde=2025-06-01&hasta=2025-06-30` → script `02-lote-desembolsos.js` → `POST /prst/contabilizarDesembolsos` |
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

## 5. Pendiente / decisiones

1. **Convención de vencimiento al corte** (`corteInclusivo`): por defecto la de Delta/C14 (vence el día del corte = vencida),
   contraria a la regla del SAA del 4-sep-2026. Confirmar con el líder; cambiarla es una propiedad.
2. **H6 junio — desembolsos netos**: 40 préstamos (319.000,00) contra 4 débitos de desembolso de Pacífico en la ventana
   (29-may 65.107,19 «Pq Cre»; 9-jun 72.799,95 y 16-jun 8.258,34 «Pq Pre»; 16-jun 27.098,71 «Pc Cre»): ningún cruce por valor
   exacto. El banco pagó montos NETOS agregados (la diferencia son saldos de préstamos anteriores cancelados con el nuevo y
   descuentos) y 11 préstamos en estado 4 sin número de operación (87.500,00, inicio 30-jun) no parecen desembolsos reales.
   El endpoint exige `valor = monto`: hace falta decidir cómo se contabiliza el neto (orden por el neto + cruce de la
   cancelación del anterior, que es el circuito de sustitución) antes de cargar el lote. Detalle:
   `cuadreSuper/03-trabajo/cartera/resultados/02-lote-desembolsos-2025-06.md`.
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
