# Auditoría de la contabilidad que generan los motores del SAA en producción

**Corte:** 24-nov-2026. **Producción:** saaBE `origin/main` = `222ead04`.

**Qué no está en producción:**
- el working tree de la rama `cuadre/c39-validaciones-g`, con 16 archivos y +657/−73 líneas **sin commit** (C41, C42, C43, C44, D-154/155/156);
- las ramas `cuadre/c39-validaciones-g` (`0dcb4416`, `acdb7f6e`), `cuadre/c19-balances-oficiales` (`6e85db88`) y `cuadre/c27b-resumen-intereses` (`791bfa66`, `fdebdeb4`, WIP).

**Método.** Todo es de solo lectura:
- el código se leyó con `git show origin/main:` y `git grep … origin/main`;
- el expediente: `DECISIONES-Y-PARTIDAS.md`, `PASE-A-PRODUCCION.md`, `CAMBIOS-SAA.md`, `03-trabajo/cambios-saa/DEFECTOS-SAA-REPROCESO-2.md`, `C27b-INTERES-ANTICIPADO.md`;
- los 393 lotes `03-trabajo/meses/*/lotes/asientos-manuales-*.csv` y las acciones del lector `03-trabajo/meses/corrida-definitiva/acciones/*.json`.

En la base de trabajo (`saa-oracle-23ai`, copia de producción del 15-nov con el re-proceso encima) solo se corrieron SELECT. Están en el §7.

**Convenciones:**
- Las rutas Java cuelgan de `saaBE/src/main/java/com/saa/ejb/`. Las líneas son de `origin/main`.
- Para no confundirlos con las decisiones D-0xx, los defectos de `DEFECTOS-SAA-REPROCESO-2.md` se citan como **DS-01…DS-23**.
- **(inferido)** marca lo que se deduce y no se vio literalmente.
- No hay datos de partícipes.

**Estados:**
- **MAIN**: corregido en `origin/main`.
- **LOCAL**: solo en el working tree, sin commit.
- **RAMA**: en una rama sin integrar.
- **ABIERTO**: sin corrección.
- **DATO**: no es del motor.

**Severidad:**
- **ALTA**: distorsiona el dinero, el B17 o las G en la operación de producción, o en cada mes re-procesado.
- **MEDIA**: obliga a un asiento manual o rodeo recurrente, o bloquea un caso de negocio.
- **BAJA**: robustez.

---

## 1. Resumen ejecutivo

1. **Bien en producción.** Los asientos de cobro se fechan con la fecha de afectación: CBCR (plantillas 19→20→21), Petro (último día del mes) y el cruce con aportes. También están en MAIN:
   - el desembolso con `fechaContable` (C6, C16);
   - la condonación con `fechaAfectacion`;
   - la novación con baja del pagaré por el monto otorgado (C30);
   - la baja de garantía al terminar (C31/C31b/C31c);
   - la pensión retenida (C32/C32b, `8696ed5f`);
   - la prelación de cuotas (C33);
   - D-141, C35 y el complemento de apertura D-024;
   - la provisión ⑧ y el ajuste de bandas ⑨ del cierre de cartera;
   - D-017 (la mora no devengada va a ingreso);
   - los bloqueos C17.
2. **Lo más grave está sin commit.** C42 y C43 viven solo en el working tree:
   - C42: la cuota que vence el día del corte es *por vencer*, en el cierre, la G48, la G49 y el CCPM.
   - C43: no se cobra ni se devenga mora antes de mayo de 2026.

   En producción, el cierre, la G48, la G49 y el CCPM cuentan esa cuota como vencida. Es la causa del rechazo de la G48 de julio v2 (D-156). Además, el motor cobra y devenga la mora de 2025.
3. **C27b sigue abierto.** El paso ④ del cierre resta lo cobrado, excluye la cuota pagada y no devenga la primera cuota del mes de concesión. Solo hay diseño en main; la rama c27b toca el reporte, no el motor. Se cubre con un asiento AJC cada mes: 24 lotes `ajuste-c27b`, de oct-2025 a oct-2026 (D-172).
4. **Plantilla de condonación.** La alterna 25, auxiliar 70, sigue en **4.1.01.30** en producción. En la base de trabajo, producción ya registró condonaciones en esa cuenta en ago, oct y nov 2026, y en **4.8.02.1** en sep-2026. El script §4.18 (D-176) está listo, pero el traslado de esos saldos de producción no está preparado.
5. **D-053 (redondeos) no está programada.** Ningún motor usa 4.8.99.08:
   - el cierre de cartera absorbe hasta 0,50 en una línea de negocio;
   - la nómina, en la línea de cuadre;
   - el crédito, en la última cuota.

   Se cubre cada mes con un asiento RPV (24 lotes `d081-provision-redondeo`) y con AJC de centavos. Solo el redondeo SRI (4.8.90.90.35) cumple.
6. **Defectos de producción que siguen abiertos:**
   - el cobro duplicado (la referencia «9»/«09» está exenta de la unicidad);
   - la precancelación y el acuerdo dejan cuotas en estado 7 con `DTPRFCPG` nulo (la cartera al corte lo neutraliza con C23);
   - el reverso es LIFO;
   - el timer de mora corre con la fecha de hoy y recarga mora pagada;
   - la reapertura revierte novaciones;
   - `/accn` solo acepta préstamos en mora o de plazo vencido y no consume aportes del garante;
   - el tipo de aporte 12 no tiene cuenta;
   - el bien entra a cuentas de orden por `PRSTVLAS` y sale por `PRSTVLGR`.
7. **Fecha del sistema en caminos contables.** Hay 389 ocurrencias candidatas; casi todas son respaldo cuando no llega una fecha. Las que dañan:
   - el reverso de un asiento en un período mayorizado (`reversionAsiento(id)` con `now()`);
   - la re-provisión del reverso directo;
   - el contra-APRT del reverso de aportes y de devoluciones;
   - el contra-movimiento tipo 23 de la pensión rechazada;
   - el asiento de la reestructuración;
   - el evento de sustitución.
8. **No existen como motor:**
   - el castigo;
   - el cierre del ejercicio (el código heredado está huérfano: se hace con C5, D-068);
   - un control de cuentas con saldo contrario (solo en RAMA C39, a nivel B17, y como script del expediente);
   - la regla D-151, que lleva el sobrante Petro devuelto a 2.3.01.15.04 (el motor usa 2.3.90.90.10).
9. **El §4 del pase tiene referencias desactualizadas:**
   - 332 → `346_C15_…` y 335 → `347_…` en main;
   - C41-01 está sin versionar y no figura en el §4;
   - C40-01 tiene cambios sin commit;
   - C39-01 y C40-01 están solo en la rama.
10. **Bloqueantes:**
    - integrar C42 y C43 (commit, merge y EAR);
    - decidir y programar C27b;
    - plantilla 25 a 4.8.90.90.45, más el traslado de las condonaciones de producción;
    - corregir el cobro duplicado;
    - confirmar en el servidor `saa.cartera.alCorte=siempre`; sin ella, ⑧, ⑨ y ⑩ no corren.

    El detalle está en el §5.

---

## 2. Matriz por flujo

### 2.1 Otorgamiento y desembolso

| Subflujo | clase.método (origin/main) | Plantilla / cuentas | Fecha contable | Defecto | Evidencia | Estado | Sev. |
|---|---|---|---|---|---|---|---|
| Aprobación (orden de pago) | `crd/serviceImpl/PrestamoServiceImpl.aprobarConNiveles` :426 (orden :569) | Orden CxP a 2.3.90.90.10; sin asiento de entrega | `fechaContable` (C6); si no llega, `now()` :440; rechaza fechas futuras | — | C6 `14337d9b` | MAIN | — |
| Desembolso / entrega | `PrestamoServiceImpl.activarPorDesembolso` :672 → `crd/serviceImpl/ContabilidadPrestamoServiceImpl.contabilizarEntrega` :1258 | 34 quirografario / 9 prendario / 13 hipotecario. D cartera por banda (1.3.01/1.3.05/1.3.09) y 7.3.01.05. H 7.4.01.05, 7.4.01.10/15 y 2.3.90.90.10 | Fecha del pago de tesorería (`fechaDesembolso`); `now()` solo si no llega (:683) | — | C15 H6 `022abdc5` | MAIN | — |
| Desembolso histórico neto | `PrestamoServiceImpl.contabilizarDesembolsoHistorico` :847; `ContabilidadPrestamoServiceImpl.contabilizarPrecancelacionConPrestamo` :899; `contabilizarDescuentosDesembolso` :981 | Entrega + precancelación del anterior (21) + otros descuentos (D 2.3.90.90.10 / H cuenta que llega como parámetro) | `fechaContable` obligatoria y no futura (:858-862) | — | C16 / D-021 | MAIN | — |
| Bien en cuentas de orden | `contabilizarEntrega` :1329 | H 7.4.01.10/15 por **`PRSTVLAS`** (valor asegurado) | La de la entrega | Entra por el valor asegurado y la baja (`GarantiaOrdenPrestamoServiceImpl` :301) sale por el avalúo `PRSTVLGR`: queda un residuo en 7.3/7.4 en los hipotecarios con avalúo. Ninguna pantalla captura el avalúo de los préstamos nuevos | DS-08, D-083; asiento C31A de julio 2025 (`asientos-manuales-2025-07-c31-avaluo-julio.csv`); carga 03 de C31b pendiente en producción (§4 1.16) | ABIERTO | MEDIA |
| Novación / refinanciación | `crd/serviceImpl/SustitucionPrestamoServiceImpl.cancelarOriginal` :284 → `ContabilidadPrestamoServiceImpl.contabilizarSustitucion` :1672, `lineasNoBandaSustitucion` :1591 | 2 / 3: bandas del nuevo contra bandas del original. Pagaré: sale el original (auxiliares 11/12) por el monto otorgado y entra el nuevo (13/14). H 2.3.90.90.10 por el neto. Prendario e hipotecario sin plantilla | Asiento: fecha del desembolso. **Evento: `LocalDateTime.now()`** (:304/:308) | Fecha del evento de hoy; cuotas a 7 sin `DTPRFCPG` y sin PGPR (inferido: la cartera al corte la da de baja por su última cuota pagada y no por la novación) | C30 `dbf7195e` | Asiento MAIN; fecha del evento ABIERTO | MEDIA |
| Reestructuración (estado 13) | `PrestamoServiceImpl.aprobarConNiveles` :599 → `contabilizarSustitucion` (sobrecarga **sin fecha**) | 4/5/10/14 (solo bandas) o 6/7/11/15 (con atraso: 1.4.02.05, mora, 1.4.90.90.10). Sin líneas 7.x | **`LocalDate.now()`** (`ContabilidadPrestamoServiceImpl` :1712): ignora la `fechaContable` de C6 | No da de baja el pagaré del original ni registra el nuevo. El estado 13 no cuenta como terminal para la baja (`GarantiaOrdenPrestamoServiceImpl.esTerminado` ≈:105-112) | DS-14; `C31-BAJA-GARANTIA.md` §6 (1); hoy no hay casos | ABIERTO | MEDIA |
| Seguro (desgravamen / incendio) | El cobro acredita 1.4.90.90.10 y 1.4.90.15.02/03 (plantillas 21 y 25); `generarAmortizacion` :1476 genera el incendio en 0 a propósito | — | — | No se verificó dónde se debitan 1.4.90.90.10 y 1.4.90.15.0x | — | Sin verificar | — |
| Complemento de apertura D-024 | `crd/serviceImpl/CobroPetroContableServiceImpl.generarComplementoApertura` :734; CBCR :2616/:2805; `ContabilidadPrestamoServiceImpl.agregaCierreAperturaCaminoDirecto` :390 | 1.4.05.05/10 contra 2.3.02.05/10 | La del asiento madre | Silencioso: no avisa (PASE §2) | `e31e0bb1` | MAIN | BAJA |

### 2.2 Cobros

| Subflujo | clase.método | Plantilla / cuentas | Fecha contable | Defecto | Evidencia | Estado | Sev. |
|---|---|---|---|---|---|---|---|
| Petro/Arch: recepción | `crd/serviceImpl/CobroPetroContableServiceImpl.confirmarRecepcion` :317 (asiento :489) | 19: banco / H 2.3.01.15.01 | Último día del mes de afectación (:478) | — | — | MAIN | — |
| Petro: reparto | `contabilizarReparto` :619 | 20: D 2.3.01.15.01 / H 1.4.05.05, 1.4.05.10 | Fin de mes (:700) | — | — | MAIN | — |
| Petro: aplicación | `contabilizarAplicacion` :810 (:1348); pagos `asoprep/serviceImpl/CargaArchivoPetroServiceImpl.crearContextoPagoCarga` :5687 → `MotorPagoPrestamoServiceImpl.aplicarPago` :323; re-proceso `ReprocesoCargaPetroServiceImpl.reprocesar` :153 | 21: D 2.3.02.05/10; H bandas 1.3.xx, interés 1.4.02.xx, seguros, aportes 2.1.xx. El excedente va a la cuenta individual por 2.3.02.10 (D-010). La devolución del sobrante va a **H 2.3.90.90.10** (auxiliar 61) con orden CxP (:1428) | Asiento a fin de mes; PGPR al último día del mes a las 23:59:59 (:5692) | **D-151 no implementada**: 2.3.01.15.04 no está ni en el código ni en la plantilla 21; se hace a mano. Si un pago no trae fecha, las bandas caen a `now()` (:988) | D-151; `git grep 2.3.01.15.04` solo encuentra un comentario de DTO | Excedentes MAIN (`e31e0bb1`); D-151 ABIERTO | MEDIA |
| Ventanilla / CBCR | `crd/serviceImpl/CobroCreditoServiceImpl.procesarCobro` :975 → `ProcesoPagoPrestamoServiceImpl.pagarCuota` :205 / `pagarMultiplesCuotas` :272 → `MotorPagoPrestamoServiceImpl.aplicarPago` :323. Asientos: :2258 (19), :2576 (20), :2666 (21, CBCRASN2) | 19 → 20 → 21 (bandas desde `ContabilizacionIndividualCreditoServiceImpl.haberDesdePagos` :749) | `cobro.getFechaAfectacion()` (:2286, :2636, :2840), obligatoria y no futura | — | — | MAIN | — |
| Cobro duplicado | `CobroCreditoServiceImpl.validar` :1783; unicidad de la referencia :1821-1857 | — | — | Las referencias «9»/«09» están **exentas** de la unicidad (:1837). No hay idempotencia por transferencia + préstamo + valor + fecha: el mismo cobro se aplica dos veces | DS-01: cobro aplicado dos veces el 10-nov-2026 en producción (eventos 1151/1152) y antecedente del evento 42 de ago-2026; rodeo B0 + `sql/06m` de `PREPARACION-V6.md` de noviembre | ABIERTO | **ALTA** |
| Fecha de los pagos (motor) | `MotorPagoPrestamoServiceImpl.aplicarPago` :389, `corteDelPago` :430, `aplicarPagoACuotaInterno` :597, `fechaUltimoPago` :1060 | — | `ctx.getFechaPago()`; `now()` solo como respaldo (las antiguas :229/:765 quedaron corregidas) | `recalcularCuotaDesdePagos` :928 decide EN_MORA contra `LocalDate.now()`. Es estado de la cuota, no asiento | C6 `14337d9b`; C33 `d10c0cb0` | MAIN; :928 ABIERTO | BAJA |
| «Vence el día del corte» en los cobros | `ContabilizacionIndividualCreditoServiceImpl` :1059; `CobroPetroContableServiceImpl` :990 | — | — | En los cobros ya rige «por vencer» (`!fechaPago.isAfter(vto)`), conforme a D-156 (1) | D-156 | MAIN | — |
| Cruce con aportes | `ProcesoPagoPrestamoServiceImpl.pagarConAportes` :614 → `ContabilidadPrestamoServiceImpl.contabilizarPagoConAportes` :146 | 21: D 2.1.xx por tipo / H bandas e interés | `fechaPago` (:643); asiento con `fechaCorte` (:198) | — | — | MAIN | — |
| Tipos patronales 13/14 | `ContabilizacionIndividualCreditoServiceImpl.aux1ParaTipoAporte` :1068 | 21: auxiliar 58 → 2.1.02.10.01 y 59 → 2.1.01.10.01 (semilla 378) | Igual que el cruce | En CBCR `esTipoAporteContabilizable` :2125 los sigue rechazando (solo se aceptan por cruce) | C29 `47c953c4`; D-069 | MAIN | — |
| Tipo de aporte 12 | `aux1ParaTipoAporte` :1100 | Sin auxiliar en la plantilla 21 | — | «no tiene cuenta contable parametrizada» (422). `validarDesgloseAportes` :834 no lo filtra antes | DS-09; ago-2025 operación 67670: rodeo CBCR 432 + CRU-2025-08-007 | ABIERTO (falta la semilla de la plantilla 21) | MEDIA |
| Abono a capital con aportes | `AbonoCapitalPrestamoServiceImpl.abonarConAportes` :194 → `contabilizarAbonoCapitalConAportes` :659 | Re-bandeo + 2.1.xx | `evento.getFecha()` (:682) | Ver flujo 3 | — | MAIN | — |
| Débitos de nómina / neteo de planillas | `CierreCarteraServiceImpl` apertura (alterno 1, :1039) y neteo ⑥ (alterno 33, `armaNeteo` :1777) | 33: 1.4.05.05/10 contra 2.3.02.05/10 | Corte | El neteo por préstamo D-032 está en main (`neteoPorPrestamo`, `auto` = sigue a `alCorte`), pero los puentes no cierran solos en los meses re-procesados | 18 lotes `ajuste-cartera-neteo` (nov-2025 a jul-2026), 10 `ajuste-puente-aportes-d010` (sep–nov 2025) y `ajuste-puentes` (ago–oct 2026): AJC sin dinero cada mes (D-032, D-024) | Código MAIN; el síntoma persiste: ABIERTO (inferido: arrastre de datos migrados y del orden de los archivos) | MEDIA |
| Prelación de cuotas C33 | `crd/service/PrelacionCuotas`; `PrelacionCuotasServiceImpl.reimputarPagosEnOrden` :79, `asegurarPrelacion` :303; `MotorPagoPrestamoServiceImpl.cuotaAnteriorConSaldo` :438/:470 | — | Fin del día del pago | — | D-130, D-142 | MAIN (`d10c0cb0`, `0ea9b2e6`, `aae03562`) | — |
| Interés cobrado antes del devengo | El cobro acredita 1.4.02 y el devengo (paso ④) resta lo cobrado | — | — | Ese interés nunca pasa a 5.1.02 y 1.4.02 queda por debajo de la G48 | DS-07, D-172; `C27b-INTERES-ANTICIPADO.md`; diseño en `docs/logica-negocio/crd/C27b-DISENO-INTERES-ANTICIPADO.md` (D1–D6 pendientes) | ABIERTO (solo diseño) | **ALTA** |
| Mora no cobrada antes de 2026 (C43) | `ModoCorteCartera.moraNoCobrable()`, `MotorPagoPrestamoServiceImpl.retirarMoraNoCobrada` / `reversarMoraNoCobrada`, `ProcesoPagoPrestamoServiceImpl.recalcularMoraALaFecha` | Reversa el devengo (17) y la provisión (36), origen `MORA_NO_COBRADA` | `fechaAfectacion ?: fechaPago` | Sin esto, producción cobra y devenga mora de 2025 | D-156 (3); memoria «sin mora antes de 2026» | **LOCAL** | **ALTA** |
| Redondeos | CBCRASN2 :2823 (rechaza si la diferencia pasa de 0,01); Petro :1283 («cuadra por construcción»); condonación :904 | 4.8.99.08 sin uso en ningún código ni plantilla | — | D-053 no implementada | Ver 2.6 | ABIERTO | MEDIA |

### 2.3 Formas de cancelar un préstamo, anulaciones y reversos

| Subflujo | clase.método | Plantilla / cuentas | Fecha contable | Defecto | Evidencia | Estado | Sev. |
|---|---|---|---|---|---|---|---|
| Pago de la última cuota | `MotorPagoPrestamoServiceImpl.aplicarPago` :323 (cierre del préstamo :377-390); `ContabilidadPrestamoServiceImpl.contabilizarPagoCuota` :525 devuelve `null` a propósito | El asiento lo da el canal (CBCR 19/20/21, Petro 21 o aportes 21) | La del canal. La baja de garantía usa afectación → pago → `now()` (`GarantiaOrdenPrestamoServiceImpl` :333) | — | — | MAIN | — |
| Precancelación (`/prst/precancelar` y CBCR PRECANCELACION) | `ProcesoPagoPrestamoServiceImpl.precancelar` :1300; `ContabilidadPrestamoServiceImpl.contabilizarPrecancelacion` :771; CBCR `procesarCobro` :1189 | 21 (D aportes / H `haberDesdePagos`) + cierre de apertura; reverso de la provisión (36) | `fechaAfectacion` o la del pago (:874) | **(a)** Las cuotas futuras pasan a 7 con `setFechaPagado(null)` (:1515-1517, verificado). **(b)** Una precancelación directa 100 % en efectivo **no genera asiento** y no avisa (:794-806, «no es un caso contemplado hoy»). El endpoint sigue abierto (inferido: la cartera baja sin el asiento del dinero) | DS-02 (jul-2025, 438 cuotas; rodeo `sql/66` mes a mes); D-052 / C23 | (a) ABIERTO en el motor; la cartera al corte lo neutraliza con C23 (`a560cdfe`, `ModoCorteCartera` :92-102). (b) ABIERTO | ALTA (a, en otros consumidores de `DTPRFCPG`); MEDIA (b) |
| D-141: cuota vencida con saldo cobrada completa | `fijarMoraMarcaExigible` :1154 | — | — | — | — | MAIN (`aae03562`) | — |
| C35: `DTPRSLOT` = lo respaldado por los pagos | `MotorPagoPrestamoServiceImpl.controlarSaldoOtrosSinRespaldo` / `normalizarSaldoOtros` :1000/:1014 | — | — | Requiere el DDL 380 | §4 1.19 | MAIN (`a2032e37`) | — |
| Abono a capital | `AbonoCapitalPrestamoServiceImpl.aplicar` :182 / `aplicarConMovimientos` :270 / `aplicarSinRegenerarTabla` :701; `ContabilidadPrestamoServiceImpl.contabilizarAbonoCapital` :605 (re-bandeo; el dinero va por CBCRASN2) | Bandas BNDP; 21 | `evento.fecha` (:621) | **(e)** rechaza con `PRESTAMO_NO_AL_DIA` (:354, :964, :1434), `PAGOS_POSTERIORES_AL_ABONO` (:1443, :1478), `DESGLOSE_INVALIDO` (:318) y con tipo de aporte repetido. **(h)** no reversa el interés ya devengado de las cuotas re-amortizadas | DS-13 (D-086: 7.716,25 a 2.3.90.90.09 en 2025); C27b §3.4 | (e) vigente por diseño, ABIERTO; (h) ABIERTO (solo diseño) | MEDIA / ALTA (h, dentro de C27b) |
| Acuerdo de condonación `/accn` | `crd/serviceImpl/AcuerdoCondonacionServiceImpl.aplicarAcuerdo` :501; `generarAsientoCondonacion` :728; validación del estado :1080 | **25**: D auxiliar 70 = **4.1.01.30 en producción** (`PLNNCDGO` 9743; en la base de trabajo ya es 4.8.90.90.45 por D-176). H capital por banda e interés y mora en 1.4.02. `condonarProvision` (36) | `fechaAfectacion ?: acuerdo.fecha`, nunca `now()` (:522-531) | **(i)** Cuenta equivocada (inversiones, sin renglón propio). **(a)** Las pendientes pasan a 7 con `DTPRFCPG` nulo (:636-638, verificado). **(d)** Solo acepta EN_MORA y DE_PLAZO_VENCIDO (:1080, verificado) y `consumirAportes(acuerdo.getEntidad())` no admite garante. **(j)** El interés y la mora condonados van todos a gasto contra 1.4.02, sin reversar su devengo en 5.1.02; la mora nunca devengada también se acredita a 1.4.02 (inferido: 1.4.02 queda corta; falta el equivalente de D-017). No reversa el devengo de la cuota en curso (C27b) | D-176; RCL-2026-09-002 (`asientos-manuales-2026-09-reclasificacion-condonaciones.csv`); DS-12; acción `ASIENTO_MANUAL/PAGO_GARANTE_CESANTIA` dic-2025; consulta §7 | (i) ABIERTO en producción, script listo (§4 4.18); (a), (d), (j) ABIERTO | **ALTA** (i); MEDIA (a, d, j) |
| Liquidación / cesantía / jubilación con cruce | `crd/serviceImpl/PagoPensionComplementariaServiceImpl` :1991, :2867, :3456 → `pagarConAportes` | 21 | `solicitud.fechaPago` | `DevolucionAporteServiceImpl` no cruza préstamos (inferido: la liquidación con deuda se hace a mano, con precancelar con aportes) | 49 acciones `LOTE_CREDITO/PRECANCELACION` y 6 `ASIENTO_MANUAL/EXCEDENTE_A_INTERES` (diferencia entre lo que cobró Delta y el simulador del SAA) | DATO / sin motor de liquidación | MEDIA |
| Castigo | — | — | — | **No existe** ningún flujo de castigo ni de baja de cartera | grep «castig»: solo comentarios | ABIERTO (sin motor) | BAJA (hoy no hay casos) |
| Anular un evento de préstamo | `ProcesoPagoPrestamoServiceImpl.anularOperacion` :2078; `ContabilidadPrestamoServiceImpl.contabilizarReverso` :1127 | Anula el asiento; re-provisión (36) | Re-provisión con **`LocalDate.now()`** (:1159); contra-APRT `revertirAportes` :2446/:2475 con `now()`; anulación de la baja de garantía con `now()` (:2425) | **(b)** LIFO: `ERR_EVENTO_POSTERIOR_VIGENTE` (:2111-2117). Novación y reestructuración no se anulan (:2120) | DS-03: rodeo `scripts/63e-*` (oculta eventos posteriores), ago–oct 2026 | ABIERTO | **ALTA** (re-proceso) / MEDIA (operación) |
| Anular / reversar un cobro CBCR | `CobroCreditoServiceImpl.anularCobro` :518, `reversarProceso` :708, `reversarLineasProcesadas` :653 | Anula los asientos 21, 20 y 19 (`reversarProceso` regenera el 19) | Re-provisión con `fechaReProvision` :1754: la de afectación si el período está abierto; si no, hoy | Hereda el LIFO. Las líneas REGISTRO_APORTE y COBRO_MIXTO llaman a `AporteService.reversarAporte` (:662): APRT negativo con fecha **`now()`** (`AporteServiceImpl` :496-507) | DS-05: aporte duplicado en ago y sep 2026 tras el rodeo 63e (inferido: el síntoma lo produjo el rodeo por SQL, no el reverso del motor) | Contra-movimiento MAIN; fecha ABIERTO | MEDIA |
| Baja de garantía al terminar | `crd/serviceImpl/GarantiaOrdenPrestamoServiceImpl.sincronizar` :117, `darDeBaja` :291, `sincronizarExplicito` :155 (`/prst/sincronizarGarantia`) | 26: H 7.3.01.05 / D 7.4.01.05, 7.4.01.15, 7.4.01.10 | La de la operación; `now()` si es nula (:333) | No cubre el estado 13 (ver 2.1) | C31/C31b/C31c; lotes `c31-baja-junio` y `c31-avaluo-julio` (jul-2025) | MAIN (`de5f884c`) | — |
| Reapertura al corte / Petro que reabre | `crd/serviceImpl/ReaperturaVigenteAlCorteServiceImpl.reabrirPrestamoVigenteAlCorte` :161 (desde `CargaArchivoPetroServiceImpl` :2855/:3038) | Anula la baja de la 26 | Regla estándar de CNT | `esPrestamoTerminal` incluye 3, 4, **5**, 9 y **13** (:66-73): los pasa a VIGENTE (:210) y anula la baja (:225). En un novado (5) el asiento de sustitución queda vivo (inferido) | DS-15: novaciones de 5 a 2 el 15-nov; rodeos `15e` y `68r`/`68` | ABIERTO | **ALTA** (re-proceso) |
| Fecha de los contra-asientos | `cnt/serviceImpl/AsientoServiceImpl.anulaAsiento` :642 → `reversionAsiento(id)` :410 = `reversionAsiento(id, LocalDate.now())` (verificado); `EstadoCierreServiceImpl.fechaReversoEnPeriodoAbierto` :197 | — | Período abierto: se anula en su sitio. **Mayorizado sin cierre contable: reverso fechado hoy.** Cerrado (C17): primer período abierto (su día 1, u hoy si cae en el mes en curso) | Al re-procesar meses pasados, el reverso cae en el mes de hoy | DS-16 | ABIERTO | **ALTA** (re-proceso) |

### 2.4 Mora e intereses

| Subflujo | clase.método | Plantilla / cuentas | Fecha | Defecto | Evidencia | Estado | Sev. |
|---|---|---|---|---|---|---|---|
| Timer diario (02:00) | `crd/serviceImpl/ProcesoMoraPrestamoTimer.ejecutarCalculoDiario` → `ProcesoMoraPrestamoServiceImpl` :265-294 | No contabiliza; persiste `DTPRMRAA` | **`LocalDate.now()`** | No hay propiedad para apagarlo. Recalcula toda cuota vencida que no esté en 4 ni 7, **incluidas PARCIAL y EN_MORA con mora pagada**. Alimenta el paso ④ del cierre (inferido: la mora devengada depende del día en que corrió el timer) | DS-06: jul-2025, 2.865 cuotas, A1 +613,76 y +0,75 de capital en la G48; foto de partida `CRD.BKP_DTPR_PARTIDA_*` cada mes | ABIERTO | **ALTA** |
| `/prst/calcularMora?fecha=` | `calcularMoraDiaria` :77 / `calcularMoraPrestamo` :221 / `calcularMoraCuota` :431 | — | `fecha` = corte | No admite fecha de partida. El retiro a fecha pasada **salta toda cuota con mora pagada** (:334), PARCIAL incluida | DS-06; rodeo `sql/def6-a1*` | ABIERTO | ALTA |
| Mora sobre el capital pendiente (D-130) | `calcularMoraCuota` :440; `PrelacionCuotas` :70/:123 | — | — | Por defecto `false` (decisión D-130) | D-130 | MAIN | — |
| Devengo de mora en el cierre | Paso ④ (17); ⑩ `CierreCarteraServiceImpl.armaDevengoMoraCorte` :1555 (activo en :638) | 17: D 1.4.02.05/10/15 (auxiliares 10/20) / H 5.1.02.05/10/15 (auxiliares 30/40) | ④ día 1 del mes que abre; ⑩ corte | ⑩ solo corre en modo al corte o con `saa.cartera.devengoMoraCierre`. `saa.cartera.inicioDevengoMora` es el único piso de la mora devengable; no impide cobrarla ni acumularla | PASE §1.2 | MAIN | — |
| Provisión de intereses ⑦ y sus reversos | `crd/serviceImpl/ProvisionInteresServiceImpl` (`reversarPorPagos` :334, `reProvisionarPorAnulacion` :443, `condonarProvision` :528, `reversarExceso*PorCobroTardio` :611/:742) | 36: D 4.7.05.10 (auxiliar 80) / H 1.4.99.05 (auxiliar 81) | La del llamador | Reverso directo con `now()` (ver 2.3) | — | MAIN | — |
| D-017: mora cobrada no devengada a ingreso | `ProvisionInteresServiceImpl.separarMoraNoDevengada` :140 (usado en `ContabilizacionIndividualCreditoServiceImpl` :845 y `CobroPetroContableServiceImpl` :1067); `saa.cartera.moraNoDevengadaAIngreso` (por defecto `true`) | 5.1.02 directo; MVIC tipo 5 | Fecha del pago | — | D-017 | MAIN (`66ea491f`) | — |
| «Sin mora antes de 2026» | C43 (ver 2.2) | — | — | ASOPREP no cobró mora hasta mayo/junio de 2026. El hecho es **DATO**; el motor que lo respeta (C43) está solo en LOCAL | D-156 (3) | DATO + LOCAL | ALTA |

### 2.5 Cierre de cartera, período contable y ejercicio

| Proceso / paso | clase.método (`crd/serviceImpl/CierreCarteraServiceImpl`, salvo que se indique otra) | Plantilla / cuentas | Fecha contable | Defecto | Evidencia | Estado | Sev. |
|---|---|---|---|---|---|---|---|
| Modo al corte | `calcula()` :511-517 → `crd/service/ModoCorteCartera.esAlCorte` :313 | — | — | Con `auto`, el cierre del mes recién terminado **no** es al corte, así que no corren ⑧ ni ⑨ (:644/:648); ⑩ solo corre si `devengoMoraCierre`. Solo `saa.cartera.alCorte=siempre` lo resuelve. Según el PASE (cabecera), se configuró el 1-nov; no se verificó en el servidor | Brecha I-3 | Código MAIN; configuración sin verificar | **ALTA** si falta la propiedad |
| ① ② ①.1 vencidos y bandas | `armaVencidos` :822, `armaCambioBandas` :891, :614 | Bandas BNDP 1.3.xx | Normal: día 1 del mes que abre. Al corte: el corte | **C42**: en modo al corte, `tipoYDiasInclusivo` :352 (`corteInclusivo` = `true` por defecto, :338) marca VENCIDA la cuota que vence el día del corte. Afecta al cierre, `CarteraAlCorteServiceImpl` :42, la G48 :483, la G49 :295 y el CCPM :172 | D-156 (rechazo de la G48 de julio v2, 58,97 en 1301/1304) | **LOCAL** (`corteExclusivoDesde`, por defecto 2025-08-01; +113 líneas en `ModoCorteCartera`) | **ALTA** |
| ③ apertura | `armaApertura` :1006 | Alterno 1 (puentes 1.4.05.05/10 contra 2.3.02.05/10) | Día 1 del mes siguiente | Los aportes salen de `calcularAportesEsperados` | — | MAIN | — |
| ④ devengo de intereses | `armaDevengoIntereses` :1083; DAO `crd/daoImpl/CierreCarteraDaoServiceImpl.selectInteresPorTipoPrestamoEnRango` :344, `pendiente(corte)` :124 | 17: 1.4.02.xx / 5.1.02.xx | Día 1 del mes que abre | **C27b**: resta lo cobrado (`GREATEST(DTPRINTR - g.intr, 0)`), excluye la cuota pagada, no devenga la primera cuota del mes de concesión y en modo normal no corta los pagos por fecha. Los pasos 4b y 11 solo existen en el diseño | D-172; 24 lotes `ajuste-c27b` (AJC-2025-10-008 … AJC-2026-10-004) | ABIERTO (la rama c27b solo toca `rpr/ResumenFinancieroCarteraServiceImpl`) | **ALTA** |
| ⑥ neteo de planillas | `armaNeteo` :1777 (tope D-024 `topeNeteoSaldoAbierto`) | 33 | Corte | Ver 2.2 (puentes con AJC mensual) | D-032 | MAIN; síntoma ABIERTO | MEDIA |
| ⑦ provisión de intereses | `armaProvisionIntereses` :1132 | 36: 4.7.05.10 / 1.4.99.05 | Corte | — | — | MAIN | — |
| ⑧ provisión de cartera | `armaProvisionCartera` :1211 | 37: auxiliar 82 gasto 4.7.04.05/15/10; 83 1.3.99.05/15/10; 84 5.3.90.05 | Corte | Ajusta a saldo contra la G48. El aviso :1220 remite a «sql/332», pero en main el archivo es `crd/sql/346_C15_PLANTILLA_37_Y_SUBPROCESOS_8_9_10.sql`. No lleva a 4.8.99.08 la diferencia de redondeo entre 1399 y la G48 | D-081 (1): 24 lotes `d081-provision-redondeo` (RPV, de 0,01 a 0,05 al mes) | MAIN; redondeo ABIERTO | MEDIA |
| ⑨ ajuste de bandas | `armaAjusteBandas` :1337 | 1.3.xx contra el módulo; auxiliar 85 de la 37 (semilla `356_SEMILLAS_P3_P5…`) | Corte | Sin la plantilla 37, su auxiliar 83 o su auxiliar 85, lanza excepción (:1371, :1541). El comentario de la 346 que dice lo contrario está desactualizado | Lotes `ajuste-cartera-residuo` (ago–oct 2026, D-064 abonos de producción) y `ajuste-cartera-centavos` (abr–jul 2026) | MAIN | MEDIA |
| ⑩ mora al corte | `armaDevengoMoraCorte` :1555 | 17 | Corte | Sin piso, devenga mora de 2025 (choca con D-156 (3)); C43 solo LOCAL | — | MAIN + LOCAL | ALTA |
| Cuadre del asiento | `cuadra` :2304 | — | — | Absorbe hasta 0,50 en una línea de negocio, no en 4.8.99.08 | D-053; lotes `ajuste-cartera-d053-*` | ABIERTO | MEDIA |
| Reverso de la corrida | `reversar` :358 → `anulaAsiento(id, usuario, motivo)` :413 | — | Mayorizado sin cierre contable: hoy (ver 2.3) | — | — | ABIERTO | MEDIA |
| Configuración de bandas | `cnt/serviceImpl/ParametroContableServiceImpl` :125 (`CBPRFCFN >= TRUNC(SYSDATE)`) | — | — | Usa la configuración vigente hoy, no la vigente al corte (inferido: riesgo si cambia la configuración) | — | ABIERTO (inferido) | BAJA |
| Cierre del período contable | `cnt/serviceImpl/CierrePeriodoContableServiceImpl.cerrar` :141, `validarOrdenCierre` :94 | — | — | Exige MAYORIZADO y la cartera cerrada; deja bitácora en `CNT.HCPR` | C17 | MAIN | — |
| Reabrir período / desmayorizar | `CierrePeriodoContableServiceImpl.reabrir` :174 (:183); `cnt/serviceImpl/MayorizacionServiceImpl.desmayorizacion` :208 (`selectById(PRDOMYRZ)` :225) | — | — | `reabrir` no sirve para un período mayorizado sin cierre contable. Con un `PRDOMYRZ` colgante, la desmayorización falla. No hay salida por REST | DS-21, D-067 (período 4511; mayorización en rango y desmayorización, §4 4.14) | ABIERTO | BAJA |
| Barrera C17 | `cnt/serviceImpl/EstadoCierreServiceImpl` :144-190, :197, :228 | — | — | `validarContableAbierto` mira el cierre contable, no la mayorización | — | MAIN | — |
| Cierre del ejercicio | `cnt/serviceImpl/AsientoServiceImpl.generaAsientoCierre` :345 / `DetalleAsientoServiceImpl.generaDetalleCierre` :159 | `PlantillasSistema.CUENTA_CIERRE` | Último día del período | **Código huérfano**: nadie lo llama (ni REST ni servicios). El cierre de 2025 se hizo con el cargador C5 (CIE-2025-12-001/002), con un residuo de 0,06 por importes de más de dos decimales en líneas de motor | D-068; lotes `cierre-resultados` y `cierre-resultados-residuo` (dic-2025) | ABIERTO (sin motor; procedimiento manual) | MEDIA |
| Control de cierre del mes | `cnt/serviceImpl/ControlCierreServiceImpl.controlar` (`GET /prdo/controlCierre`) | — | — | Controla puentes, ecuación, cartera por grupo, bancos, asientos, 21 = G42, 13 − 1399 = G48, 1399 = provisión de la G48 y novedades. `B17_OFICIAL` queda como advertencia «pendiente C19b». **Sin control de saldos contrarios** | — | MAIN; saldos negativos del B17 en RAMA C39 (`0dcb4416`) | MEDIA |

### 2.6 Fecha del sistema, redondeos y saldos contrarios

**Fecha del sistema.** En `ejb/` hay 651 ocurrencias de `now()` / `new Date()` / `Calendar.getInstance()`. 389 son candidatas en caminos de negocio. La gran mayoría son respaldos `fecha != null ? fecha : now()` o validaciones de «no futura».

Las que fechan contabilidad con el día de hoy aunque se procese otro mes, todas **ABIERTO**:

| Archivo:línea | Qué queda con fecha de hoy | Sev. |
|---|---|---|
| `cnt/serviceImpl/AsientoServiceImpl.reversionAsiento(id)` :410, desde `anulaAsiento` :672 | Reverso de cualquier asiento de un período mayorizado sin cierre contable. Lo heredan el reverso del cierre de cartera, `ContabilidadPrestamoServiceImpl.contabilizarReverso` :1127 y CxP: `AplicacionPagoCxp` :1967, `PagoProgramado` :3169, `AnticipoProveedor` :464 | ALTA (re-proceso) |
| `crd/serviceImpl/ContabilidadPrestamoServiceImpl.contabilizarReverso` :1159 | Asiento de re-provisión (36) | MEDIA |
| `crd/serviceImpl/PagoPensionComplementariaServiceImpl.generarContraMovimiento` :4474 (:4496, :4509), desde `dejarPendientePorPagoNoRecibido` :4205 | Contra-APRT tipo 23 y `PagoAporte.fechaContable`. Además el devengo queda vivo con `PGPCDVRT = 1` (DS-04, huecos 2 y 3) | ALTA |
| `crd/serviceImpl/AporteServiceImpl.reversarAporte` :471/:507/:521 | Reverso de un aporte (por diseño C17) | MEDIA |
| `crd/serviceImpl/DevolucionAporteServiceImpl` :1833/:1873 | Contra-APRT del reverso de una devolución | MEDIA |
| `crd/serviceImpl/ProcesoPagoPrestamoServiceImpl` :2119 → `revertirAportes` :2475; `sincronizar(…, LocalDate.now())` :2425 | APRT de reverso y anulación de la baja de garantía | MEDIA |
| `ContabilidadPrestamoServiceImpl` :1712 (reestructuración) y `SustitucionPrestamoServiceImpl` :304/:308 (evento) | Ver 2.1 | MEDIA |
| `CobroCreditoServiceImpl.fechaReProvision` :1755 | Hoy si el período de afectación está mayorizado o cerrado | BAJA (parcial) |
| `AsientoServiceImpl.insertarCabeceraAsiento` :580/:591, `recuperaDatosAsiento` :559, `generaCabeceraTransferencia` :741, `copiaAsiento` :958; tsr `CajaChica` :234, `Cobro` :330, `DebitoCredito` :196, `Transferencia` :175 | Documentos y asientos de tesorería heredados con fecha de hoy (inferido: no se verificó si tienen pantalla activa) | MEDIA (inferido) |

C6 corrigió cinco puntos: seguro médico y órdenes de la pensión, desembolso y orden CxP, `procesarRespuestaBanco` y MVCB. Están en MAIN (`docs/logica-negocio/cnt/FECHA-CONTABLE-REPROCESO.md`). `ContabilizacionIndividualCreditoServiceImpl` no tiene ningún `now()`.

**Redondeos (D-053).** `4.8.99.08` existe en `CNT.PLNN` («GND POR AJUSTES»). Ningún código ni plantilla de main la usa, y no hay ningún commit ni rama de C24 (que está solo especificado). Cómo se trata hoy cada caso:

| Dónde | Qué hace hoy | ¿Cumple D-053? |
|---|---|---|
| SRI: `AsientoContableServiceImpl.agregarDiferenciaRedondeoSri` :3111 (alterno 38, 4.8.90.90.35, de 0,01 a 0,50) | Lleva la diferencia a 4.8.90.90.35 | Sí (es la excepción) |
| Cierre de cartera: `cuadra` :2304 | Absorbe en la línea de negocio | No |
| Nómina: `rhh/serviceImpl/ContabilizacionNominaServiceImpl.comprobarCuadre` (sueldos :293, vacaciones :351, banco :560, liquidaciones :754) | Absorbe en la línea de cuadre | No |
| Crédito: `ContabilizacionIndividualCreditoServiceImpl` :291/:638 | La última cuota absorbe el residuo | No |
| CBCRASN2 | Rechaza el asiento si la diferencia pasa de 0,01 | No |
| C34 v2 (`SeguimientoNovedadV2ServiceImpl` :1236) | Reparte el centavo en la última fila | No |

Evidencia de que se compensa a mano:
- 24 lotes `d081-provision-redondeo` (jul-2025 → oct-2026);
- `ajuste-cartera-d053-*` (sep–oct 2025);
- `centavo-354-d053` (ago-2025);
- `pension-jja-d053-*`;
- el residuo de 0,06 del cierre del ejercicio 2025.

Estado: **ABIERTO**. Severidad: **MEDIA** (son centavos, pero hay un asiento manual cada mes).

**Cuentas con saldo contrario.** Main no tiene ningún control de naturaleza de cuenta:
- en RAMA C39 (`0dcb4416`) solo hay un aviso de saldo negativo a nivel de renglón del B17;
- el control del expediente es `03-trabajo/revision/cuentas-saldo-contrario.sql`, que vio 2.5.15 escondida dentro del 2501 de septiembre 2025.

| Cuenta | Saldo al 30-sep-2025 | Cómo llegó ahí | Clasificación | Sev. |
|---|---|---|---|---|
| **2.3.04.90.01** | Deudor de 1.234,24 | La usa el grupo de pagos `PGS.GRPP` 58 (retenciones en la fuente de renta). No está en ninguna plantilla. Las retenciones se constituyen en las subcuentas por porcentaje y el pago al SRI debita la .01 (inferido) | DATO / parametrización del grupo de pagos | MEDIA |
| **2.5.15** (liquidación de haberes por pagar) | Deudor de 9.871,49 | Plantillas 161/166 (HABER) y 162 (DEBE). Haber de 3.168,52 en junio y debe de 13.040,01 en agosto. Se pagaron liquidaciones cuya constitución no está en el SAA (inferido) | DATO / parametrización | MEDIA |
| 2.3.04.10 y 2.5.07 | Negativos en el B17 de jun–jul 2025, rechazado por la Super | Mismo patrón: pagos cargados contra un pasivo sin su constitución | DATO; se reclasificó con D-150 | MEDIA |

---

## 3. Síntomas en el expediente y su causa en el motor

Hay 393 lotes de asientos manuales (jun-2025 → nov-2026). La mayoría son lo que el objetivo 4 prevé y **no** indican un hueco del motor:
- regularización bancaria (26 de ingresos y 26 de egresos);
- depreciación (14), diferidos (14 + 6) e inversiones;
- SRI (15) y caja chica;
- traspasos entre bancos propios (`pension-banco`, 16).

Los que sí delatan un hueco:

| Lote o acción recurrente | Meses | Causa en el motor | Fila de la matriz |
|---|---|---|---|
| `ajuste-c27b` (AJC) | oct-2025 → oct-2026 (24) | Devengo ④ (C27b) | 2.5 ④ |
| `d081-provision-redondeo` (RPV) | jul-2025 → oct-2026 (24) | Redondeo de ⑧ fuera de 4.8.99.08 | 2.5 ⑧ / 2.6 |
| `ajuste-cartera-neteo`, `ajuste-puente-aportes(-d010)`, `ajuste-puentes` | sep-2025 → oct-2026 (≈36) | Puentes que no cierran con el neteo ⑥ | 2.2 / 2.5 ⑥ |
| `ajuste-cartera-centavos`, `-residuo`, `-d053-*` | sep-2025 → oct-2026 (≈18) | Redondeo del cuadre del cierre; abonos de producción posteriores (D-064) | 2.5 ⑨ |
| `c31-baja-junio`, `c31-avaluo-julio` | jul-2025 | Terminados sin baja antes de C31; bien por `PRSTVLAS` | 2.1 / 2.3 |
| `pension-retenida` (RTN) | ago–sep 2026 | Devengo de pensión retenida (antes de C32) y rechazo sin reversar el devengo | 2.6 |
| `reclasificacion-condonaciones` (RCL-2026-09-002), D176-2025-09-001 | sep-2025, sep-2026 | Plantilla 25/70 | 2.3 acuerdo |
| `cierre-resultados(-residuo)` | dic-2025 | No hay motor de cierre del ejercicio | 2.5 |
| `d150-reclasificaciones` | jun–jul 2025 | Pagos contra pasivos sin constitución (DATO) | 2.6 |
| Acción `ASIENTO_MANUAL/PAGO_GARANTE_CESANTIA` | dic-2025 | `/accn` no admite aportes del garante | 2.3 acuerdo (d) |
| Acción `ASIENTO_MANUAL/EXCEDENTE_A_INTERES` (6) | nov-2025 → jul-2026 | Lo que cobró Delta difiere del simulador de precancelación del SAA | 2.3 liquidación (DATO) |
| Acción `RECLASIFICACION/NEUTRALIZAR_APORTE_MIGRADO` (123; 928.569,67) | jul-2025 → jul-2026 | Cruces migrados de Delta como APRT sin aplicación al préstamo | DATO (migración, DS-23) |
| Rodeos `sql/66`, `63e`, `15e`, `68r`, `def6-a1*`, `15p`, `06m` | Todos los meses del re-proceso 2 | DS-02, DS-03, DS-15, DS-06, DS-05 y DS-01 | 2.3 / 2.4 |

---

## 4. Lo que falta llevar a producción

### 4.1 Código

| Qué | Dónde está | Qué hace falta |
|---|---|---|
| **C42 + C43** (+ C41, C44, D-154/155/156) | Working tree de `cuadre/c39-validaciones-g`, 16 archivos, +657/−73, **sin commit**: `ModoCorteCartera`, `CierreCarteraServiceImpl`, `CierreCarteraDaoServiceImpl`, `CarteraAlCorteServiceImpl`, `MotorPagoPrestamoServiceImpl` (+129), `ProcesoPagoPrestamoServiceImpl` (+12), `DetalleAplicacionCuota`, `OperacionCarteraCorte`, `GeneracionCCPM`, `G45`, `G46`, `G48`, `G49`, `ValidacionEstructurasGServiceImpl`, `SujetoReportadoG45` | Commit, revisión, merge a main y EAR. Propiedades: `saa.cartera.corteExclusivoDesde` con su valor por defecto (2025-08-01) y **sin** `saa.cartera.corteInclusivo`; `saa.cartera.moraNoCobradaHasta=2026-05-01` solo mientras se re-procesa, y quitarla al terminar (PASE §1.2) |
| C39 + C40 (validaciones previas de las G, B17 sin saldos negativos, sustituciones de las G) | RAMA `cuadre/c39-validaciones-g` (`0dcb4416`, `acdb7f6e`) | Merge junto con lo anterior; DDL C39-01 y C40-01 antes del WAR |
| C19b (B17 y flujo dentro del motor de balances) | RAMA `cuadre/c19-balances-oficiales` (`6e85db88`) | Pendiente desde el pase del 1-nov. Choca con C38 (`CNT.CRRP`, `/esrp`) |
| C27b (motor) | Solo el diseño en main (`docs/logica-negocio/crd/C27b-DISENO-INTERES-ANTICIPADO.md`); la rama `cuadre/c27b-resumen-intereses` es WIP del reporte | Decisiones D1–D6 del líder; programar los pasos 4b y 11 y los reversos en el abono, la precancelación y la condonación. Inferido: hará falta ampliar `CK_ANCC_TPOO` a 11 y 12 |
| Sin código todavía | — | Idempotencia de cobros (DS-01, y quitar la exención de «9»/«09»); `DTPRFCPG` = fecha del cobro o acuerdo al pasar a 4/7 (DS-02, en precancelación :1517, acuerdo :638, abono :1611/:1705 y motor :612/:924/:931); fecha contable en `reversionAsiento(id)`, `contabilizarReverso` :1159, `generarContraMovimiento`, `reversarAporte`, `revertirAportes`, reestructuración y sustitución; reverso del devengo al rechazar una pensión (DS-04 D3-A); timer y `calcularMora` con fecha de partida que no recargue mora pagada (DS-06); reapertura que no toque los estados 5 ni 13 (DS-15); reverso en cascada (DS-03); `/accn` para vigentes y con aportes del garante (DS-12); asiento o rechazo de la precancelación directa en efectivo; D-151 (línea de la 21 a 2.3.01.15.04 cuando hay devolución documentada); C24 / D-053 (diferencias a 4.8.99.08 en el cierre de cartera, nómina, crédito y C34); captura de `PRSTVLGR` y alta del bien por el avalúo (DS-08); reestructuración con baja y alta de pagaré (DS-14); control de saldos contrarios por subcuenta en `GET /prdo/controlCierre` |

### 4.2 Scripts SQL de PASE-A-PRODUCCION §4

| Script | Estado |
|---|---|
| §4 **4.18** `03-trabajo/meses/2025-09/sql/def9-07-cuenta-4.8.90.90.45-perdidas-condonaciones.sql` (D-176) | **Listo**. **Falta** el asiento de traslado de lo que producción ya registró en 4.1.01.30 y 4.8.02.1 en 2026 (ver §7). El §4.18 lo prevé («trasladarlas con un asiento como D176-2025-09-001»), pero no hay lote preparado |
| §4 1.4 | Cita `332_C15_PLANTILLA_37_Y_SUBPROCESOS_8_9_10.sql`; en main es **`346_C15_…`**. El aviso del código (:1220) también dice 332 |
| §4 4.2 | Cita `335_LINEAS_RENDIMIENTOS_PLANTILLA_21.sql`; en main es **`347_…`**. En main, `332_` y `335_` son otros scripts (medición de la carga 492 y puente del Arch de octubre 2026). Ejecutar por el número viejo correría el script equivocado |
| C39-01 (`docs/logica-negocio/reportes/sql/C39-01-ddl-sg45-origenes.sql`) | En la rama, no en main |
| C40-01 | En la rama, con **cambios sin commit** en el working tree (texto de las sustituciones de julio y agosto) |
| **C41-01** (`docs/logica-negocio/reportes/sql/C41-01-ddl-sg45-tipo-participe.sql`) | **Sin versionar y no figura en el §4**. Faltante: debe ir antes del WAR con C41 |
| §4 1.16 carga 03 de avalúos C31b | Pendiente en producción |
| §4 1.19 (380), 1.15 (378), 1.14 (C27-01), 1.10 (D043-01) | En main. Producción ya corrió U1–U7 según el §4, sin verificar en esta auditoría |
| Faltantes sin escribir | Semilla de la línea del tipo 12 en la plantilla 21 (DS-09); línea o cuenta de 2.3.01.15.04 en la 21 (D-151); uso de 4.8.99.08 en las plantillas (C24); ampliación de `CK_ANCC_TPOO` para C27b (inferido) |

### 4.3 Plantillas

| Plantilla | En producción | Debe quedar |
|---|---|---|
| 25 / auxiliar 70 (condonación) | 4.1.01.30 (`PLNNCDGO` 9743) | 4.8.90.90.45 (§4.18) |
| 21, tipo de aporte 12 | Sin línea | Línea con cuenta |
| 21, devolución por cobro en exceso (D-151) | Sin línea (auxiliar 61 → 2.3.90.90.10) | Decidir si se agrega una línea para 2.3.01.15.04 o sigue siendo manual |
| 4–7, 10, 11, 14, 15 (reestructuración) | Sin líneas 7.x | Pendiente de decisión (DS-14) |
| Grupos de pagos `PGS.GRPP` 58 y plantillas 161/162/166 | Generan los saldos contrarios de 2.3.04.90.01 y 2.5.15 | Revisar con Contabilidad (inferido) |

---

## 5. Bloqueantes

1. **C42 y C43 sin commit.** Sin ellos, producción vuelve a contar como vencida la cuota que vence el día del corte, la causa del rechazo de la G48 de julio v2. También cobra y devenga mora anterior a 2026. Son 657 líneas de un único working tree: si se pierden, no hay copia (ALTA).
2. **C27b (paso ④ y reversos de abono, precancelación y condonación).** Sin el motor, cada mes necesita un AJC manual para que 1.4.02 cuadre con el interés de la G48 (D-172), y en producción no lo hace nadie (ALTA).
3. **Plantilla 25/70 en 4.1.01.30.** Producción sigue registrando condonaciones en una cuenta de inversiones y ya tiene saldos de 2026 en 4.1.01.30 y 4.8.02.1. Hace falta el §4.18 más un asiento de traslado, que no está preparado (ALTA).
4. **Cobro duplicado (DS-01).** Ya ocurrió en producción en agosto y noviembre de 2026 y paga capital y provisión dos veces (ALTA).
5. **`saa.cartera.alCorte=siempre` sin verificar en el servidor.** Sin ella, el cierre mensual no corre ⑧ ni ⑨ y la provisión 1399 no se ajusta a la G48 (ALTA si falta).
6. **Para el re-proceso, aunque no para la operación diaria:**
   - fecha de hoy en los reversos de períodos mayorizados y en los contra-APRT;
   - timer de mora (DS-06);
   - reapertura que revierte novaciones (DS-15);
   - reverso LIFO (DS-03).

   Cada uno obliga a un rodeo por SQL en cada mes (ALTA en el re-proceso).

---

## 6. Verificaciones que no se pudieron cerrar

- **Propiedades JVM del WildFly de producción:** `saa.cartera.alCorte`, `devengoMoraCierre`, `inicioDevengoMora`, `neteoPorPrestamo`, `moraNoDevengadaAIngreso`. Solo se leyó la cabecera del PASE, que dice que el 1-nov se configuraron `alCorte=siempre` e `inicioDevengoMora=2025-05-31`.
- **Plantillas en la base de producción.** Se consultó la base de trabajo, que es copia de producción del 15-nov con cambios locales (D-176). El estado real de la 25/70, la 37 y `CK_ANCC_TPOO` en producción no se vio.
- **`AsientoContableServiceImpl.generarAsiento`:** no se leyó si tolera un descuadre de 0,01 o lo ajusta a alguna cuenta. No se sabe si hoy entran redondeos en silencio.
- **Seguros:** dónde se debitan 1.4.90.90.10 (desgravamen) y 1.4.90.15.0x antes de que el cobro los acredite.
- **Débitos de nómina:** no se auditaron a fondo (plantilla 33, fecha) ni la contabilización de nómina más allá del cuadre.
- **Precancelación directa con efectivo y aportes:** que lance excepción por D ≠ H se dedujo del código, no se ejecutó.
- **Novados frente a C23:** el efecto en cortes intermedios es inferido, sin SQL.
- **Mora condonada nunca devengada:** que deje 1.4.02 corta es inferido.
- **DS-05:** que el aporte duplicado venga del rodeo 63e y no del reverso del motor es inferido. En main, el reverso CBCR sí inserta el APRT negativo.
- **Referencias «9»/«09»:** si la UI o el REST impiden usarlas en transferencias reales. No se buscaron cobros duplicados en la base.
- **Tesorería heredada:** si los servicios que usan `insertarCabeceraAsiento` o `generaCabeceraTransferencia` tienen pantalla activa.
- **Baja de garantía:** que `garantiaOrdenPrestamoService.sincronizar` genere el asiento con la fecha que recibe.
- **Saldos contrarios:** la causa documental de 2.5.15 y 2.3.04.90.01 se infirió de los movimientos.
- **DS-10 (G49 repite canceladas) y DS-11 (factura por número sin filtrar el estado, CxP):** quedan fuera de la contabilidad de los motores y no se re-verificaron. `selectFacturaByNumero` sigue en `cxp/daoImpl/AplicacionPagoCxpDaoServiceImpl` :153; no se leyó su filtro.
- **Producción por lotes:** no se verificó que producción haya corrido U1–U7 del §4.

---

## 7. Consultas de esta auditoría (reproducibles, solo SELECT, base de trabajo `FREEPDB1`)

```sql
-- Línea de condonación (alterno 25, auxiliar 70) antes de D-176: respaldo local
SELECT DTPLCDGO, PLNSCDGO, PLNNCDGO, DTPLDSCR, DTPLAXL1 FROM CNT.BKP_DTPL_D176_CONDONACION;
--   1790 | 1111 | 9743 | GASTO POR CONDONACION DE PRESTAMOS | 70
SELECT PLNNCDGO, PLNNCNTA, PLNNNMBR FROM CNT.PLNN WHERE PLNNCDGO IN (9743, 11109);
--   9743 = 4.1.01.30 INVERSIONES RENTA VARIABLE SECTOR NO FINANCIERO PRIVADO; 11109 = 4.8.90.90.45 (D-176, local)

-- Uso de las cuentas de condonación por mes (asientos vigentes)
SELECT TO_CHAR(a.ASNTFCHA,'YYYY-MM') mes, d.DTASCNTA, COUNT(*), SUM(d.DTASDBEE) debe, SUM(d.DTASHBRR) haber
  FROM CNT.DTAS d JOIN CNT.ASNT a ON a.ASNTCDGO = d.ASNTCDGO
 WHERE d.DTASCNTA IN ('4.1.01.30','4.8.02.1','4.8.90.90.45') AND a.ASNTESTD IN (1,3)
 GROUP BY TO_CHAR(a.ASNTFCHA,'YYYY-MM'), d.DTASCNTA ORDER BY 1, 2;
```

Resultado del 24-nov-2026:

| Mes | Cuenta | Debe | Haber | Nota |
|---|---|---:|---:|---|
| 2025-06 | 4.1.01.30 | 40.222,94 | 40.222,94 | Neto 0 |
| 2025-09 | 4.1.01.30 | 177,96 | 177,96 | Neto 0, trasladado por D176-2025-09-001 |
| 2025-09 | 4.8.90.90.45 | 440,47 | — | |
| 2026-08 | 4.1.01.30 | 11.977,85 | — | |
| 2026-09 | 4.8.02.1 | 12.751,99 | — | Cuatro líneas «gasto por condonación» de acuerdos de producción |
| 2026-10 | 4.1.01.30 | 28.495,65 | — | |
| 2026-11 | 4.1.01.30 | 969,44 | — | |

**Qué muestra:**
- En 2026 la línea 70 de producción apuntó primero a **4.8.02.1** y después a **4.1.01.30**.
- En 2026 quedan **41.442,94** en 4.1.01.30 y **12.751,99** en 4.8.02.1 por trasladar. Uno de los asientos de 4.8.02.1 es la condonación huérfana que D-107 anuló en la v6; hay que confirmarlo antes de trasladar.

Las otras consultas de soporte (saldos contrarios, plantillas 17/21/26/36/37) están en el scratchpad de la sesión y no se versionan. Sus cifras se citan arriba como «base de trabajo».
