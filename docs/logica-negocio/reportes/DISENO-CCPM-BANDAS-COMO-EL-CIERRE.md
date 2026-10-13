# Diseño — CCPM y el Resumen Financiero con la misma regla de bandas que el cierre de cartera

**Equipo:** `lap-saa-1` (árbitro) · **Fecha:** 2026-10-12 · **Estado:** diseño, sin implementar.
**Origen:** `ANALISIS-DESCUADRE-RESUMEN-FINANCIERO-SEP-2026.md` (mismo directorio).

## 0. Decisión del usuario (2026-10-12)

> *«Debemos usar en CCPM la misma lógica de bandas que en el cierre, y debemos asegurarnos que los
> valores cuadren siempre al centavo.»*

Contabilidad comparó **antes** del asiento manual `11126`; ese asiento se hizo sólo para presentar
balances cuadrados. El corte es el 30-09.

## 1. Qué significa «la misma lógica», punto por punto

La regla del cierre no es sólo «por días»; son cinco cosas, y CCPM tiene que adoptar las cinco o
seguirá dando distinto:

| # | Regla del cierre (`CierreCarteraServiceImpl` / `CierreCarteraDaoServiceImpl`) | CCPM hoy | Cambio |
|---|---|---|---|
| 1 | **Universo:** `PRSTIDST IN (2, 8, 11)`, cuotas con `DTPRESTD NOT IN (4, 7)` (o nulo), `DTPRFCVN` no nula, producto no nulo | grupo 1 = cualquier préstamo con una cuota pagada en el mes, **sin filtro de estado** → entran precancelados | mismo universo en los dos grupos |
| 2 | **Saldo por cuota:** `GREATEST(DTPRCPTL − Σ PGPRCPPG vigentes, 0)` | `saldoInicialCapital − capital` de una cuota ancla | misma fórmula, cuota por cuota, desde `CRD.PGPR` |
| 3 | **Tipo y días:** `ContabilizacionIndividualCreditoService.tipoCarteraYDias(vencimiento, corte)` — el día del vencimiento todavía es por vencer | `DAYS.between(vencimiento, fechaFin)` sobre la cuota más vieja; bandas por **número ordinal** de cuota | llamar al mismo método, por cuota |
| 4 | **Banda:** `ClasificadorBandaService.resolverRangos(producto, empresa, tipoCartera, fecha)` + `clasificarEnBandas(bandas, dias)` — períodos de 30 días de `CRD.BNDP`, última abierta | cortes fijos 30/90/180/360 (por vencer) y 30/90/180/270/360/720 (vencido, en el frontend) | clasificar con la configuración vigente del producto; **ninguna constante nueva** |
| 5 | **Corte:** último día del mes que se informa (= `fechaCorte` del cierre) | `fechaFin` = último día del mes a `LocalTime.MAX` | igual; sin cambio |

⛔ **No reimplementar la fórmula.** `API-AUDITORIA-BANDAS.md` ya registra **cinco** copias de
«vencido/por vencer + días» en el sistema y el cierre delegó la suya en 2026-09-05 justamente por
eso. CCPM inyecta `ContabilizacionIndividualCreditoService` y `ClasificadorBandaService`, y llama.

## 2. Qué cambia en cada pieza

### 2.1 `GeneracionCCPMServiceImpl` (rpr)

- Universo y cuotas: una sola consulta nueva en `DetallePrestamoDaoService` (o reutilizar
  `CierreCarteraDaoService.selectCapitalPorProductoYVencimiento` **llevándola a nivel préstamo**:
  hoy agrupa por producto y vencimiento; CCPM necesita `[idPrestamo, idProducto, vencimiento,
  capitalPendiente, nroCuota]`). Misma `WHERE` que el cierre, literalmente las mismas constantes
  (`PRESTAMOS_VIVOS`, `CUOTAS_PENDIENTES`, `PAGOS_VIGENTES`) — moverlas a un lugar común en vez de
  copiarlas.
- Por préstamo: `valorPorVencer` = Σ capital pendiente de cuotas con `tipoCartera = POR_VENCER`;
  `valorVencido` = Σ de las `VENCIDO`; `diasMorosidad` = días de la cuota vencida más antigua
  (`tipoCarteraYDias`), 0 si no hay. `capitalPorVencer1a30 … Mas360` = Σ por **número de banda 1…5
  de la configuración por vencer del producto** (hoy todos los productos tienen 5; si alguno
  tuviera menos, las columnas sobrantes quedan en 0, y si tuviera más, falla fuerte —
  `IncomeException`— en vez de absorber).
- **Se elimina** el bloque «control de consistencia y ajuste final» (`:470-486`): con la misma
  fórmula para el total y para el desglose, cuadran por construcción. Si no cuadran, es un defecto
  y tiene que verse, no absorberse en `cv30`. Se elimina también el reparto de `saldoOtros` a la
  última banda (`:459-467`): el cierre no lo bandea; si `saldoOtros` tiene que informarse, va en su
  columna.
- `estadoDesglose` queda en 1 siempre (o se retira de la pantalla).
- Todo lo demás de CCPM (intereses, mora, seguros, calificación, provisión, cuenta individual) **no
  se toca en esta entrega**.

### 2.2 El Resumen Financiero deja de calcularse en el frontend

Hoy `informes-mensuales-credito.component.ts:393-540` arma el resumen en el navegador con cortes
fijos y nombres de tipo de crédito quemados. Pasa al backend:

`GET /rest/ccpm/resumenBandas/{idEjcc}` → una fila por `(producto, tipoCartera, banda)` con
`cuenta`, `nombreCuenta`, `etiqueta` (de `BandaProductoDetalle`), `capital`, `cantidadCuotas`,
`cantidadPrestamos`. Las categorías de la pantalla salen de la **cuenta contable de la banda**, no de
un `switch` por nombre de producto — así la pantalla ya está en los mismos términos que el mayor y
no hay un tercer catálogo.

**Regla de fuente** (garantiza el centavo contra el cierre):
- Si existe una corrida **EJECUTADA** de `CRD.CRCT` para el mismo `(empresa, año, mes)`, el resumen
  lee `CRD.BDCC` de esa corrida: es la foto **congelada** que se contabilizó. Cuadra al centavo con
  los asientos del cierre por definición, y no se mueve aunque entren pagos después.
- Si no existe, calcula en vivo con el mismo núcleo (`distribuye` del cierre, expuesto como método
  de servicio) y lo rotula «preliminar — sin cierre ejecutado».

Las secciones de intereses, seguros, provisiones, ingresos y aportes del resumen se mantienen como
están (siguen saliendo de CCPM/CPRM) en esta entrega.

### 2.3 Momento de la generación

CCPM de un mes y el cierre de ese mes tienen que correr sobre la **misma cartera**. Si el cierre
corre el 05-10 21:03 y CCPM el 05-10 20:00, un pago registrado entre medio los separa. Regla
operativa: **primero el cierre, después los informes** del mismo mes; y la regla de fuente de §2.2
hace que el resumen tome la foto del cierre aunque CCPM se regenere días después.

## 3. Lo que «al centavo» exige además, y NO es CCPM

Con §1 y §2, **CCPM = cierre** por construcción. Para que además **cierre = mayor**, hacen falta
cuatro cosas que están fuera de CCPM y que son decisiones del usuario (análisis §3.2, §3.3, §4):

| # | Qué | Por qué sin esto no cuadra |
|---|---|---|
| A | **Nivelación** de las cuentas de banda al snapshot absoluto, al menos una vez | El cierre asienta deltas; el nivel del mayor viene de la migración y de siete plugs. Nada lo corrige hoy |
| B | **Fin de los plugs manuales** sobre bandas, intereses, provisiones y aportes (o, si se hacen, después del cierre y registrados) | Cada plug es invisible para la corrida siguiente y produce la diferencia del mes que viene |
| C | **Fecha de comparación:** los asientos ①–④ van al 01 del mes siguiente (decisión del 31-08) | Un mayor al 30-09 nunca va a contener el envejecimiento de septiembre. O contabilidad cuadra al 01-10, o se cambia la decisión |
| D | **Aportes:** reverso de `APRT` fechado hoy vs. anulación contable retroactiva (26.739,47); pensión registrada con asiento y sin `APRT` (1.713,45) | Son diferencias de semántica, se repiten con cada reverso y cada pensión sin procesar |

## 4. Impacto fuera de la pantalla

- **G48 / histórico (`HMCP`):** `GeneracionG48ServiceImpl` toma `provisionRequeridaOriginal` del
  histórico de CCPM para la provisión constituida del mes siguiente. Al cambiar `valorPorVencer` y
  `valorVencido` cambia `valorSujetoProvision` y con él la provisión requerida. **Es el efecto
  deseado** (hoy la provisión se calcula sobre un saldo con precancelados adentro), pero cambia un
  dato que va al regulador: avisar antes del primer mes.
- Las columnas `cv30…cvMas` de CCPM sólo las lee `informes-mensuales-credito`; el G48 no.
- `HistoricoCCPM` conserva los meses viejos con la regla vieja: la serie histórica tiene un
  quiebre en el primer mes con la regla nueva. Documentarlo en la pantalla.

## 5. Propiedad del código

`rpr` y `crd` son de **`omen-saa-1`** (commits `rpr(omen1)`, `crd(omen1)`; archivos con dueño
exclusivo en `REGISTRO-RESERVAS-EQUIPOS.md` §4). Este diseño toca `GeneracionCCPMServiceImpl`,
`DetallePrestamoDaoService`, `CierreCarteraDaoService`/`CierreCarteraServiceImpl` (exponer el
núcleo), `CreditoCuotasPrestamosMensualRest` y la pantalla `rpr` del frontend. **Quién lo
implementa lo decide el usuario**; el otro árbitro tiene que saberlo antes de que se toque una
línea.

## 6. Verificación que se exige para dar por entregado

1. Para el mes de prueba (septiembre 2026, `EJCC` nueva): Σ `capitalPorVencer1a30…Mas360` por
   producto = `BDCC` de `CRCT 4`, **al centavo**, en las 143 bandas. Consulta de control en el
   análisis §9.1 (columna `snap` contra la nueva CCPM).
2. `COUNT` de préstamos de CCPM = `COUNT DISTINCT PRSTCDGO` del universo del cierre.
3. Ningún préstamo con `PRSTIDST IN (3, 4, 5, 9)` en CCPM.
4. `/resumenBandas/{idEjcc}` con cierre ejecutado = `BDCC`; sin cierre = la réplica SQL del
   análisis §9.4 al corte.
5. `mvn -q compile` y `ng build --configuration development` limpios.

## 7. Decisiones pendientes antes de escribir los prompts

1. Alcance de esta entrega: **sólo §1-§2** (CCPM = cierre), o **también §3** (cierre = mayor).
2. Quién implementa (§5) y autorización para avisar a `omen-saa-1-arb`.
3. Regla de fuente §2.2 (leer `BDCC` cuando hay cierre): sí / no.
4. Si se retiran de la pantalla `estadoDesglose` y el ajuste de `saldoOtros`.
