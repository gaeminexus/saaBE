# Implementación de `CAMBIOS-SAA.md` — plan y hallazgos del equipo `omen-saa-3`

**Árbitro:** `omen-saa-3-arb` · **Iniciado:** 2026-10-02 · Fuente: `docs/regulatorio/CAMBIOS-SAA.md` (commit `22060bc7`)
**Decisiones del usuario (2026-10-02):** lo hace este equipo **coordinando con `omen-saa-1`** para la
parte de crd, **en paralelo** con los frentes de RRHH.

## Condiciones de trabajo

- **La base del proyecto (`saa-oracle-cuadre`, puerto 1522) y la carpeta `cuadreSuper` no están en la
  OMEN.** Se buscaron en todos los discos el 2026-10-02. Todo lo que toca datos sale como script con
  controles, lo corre el usuario y la aceptación vuelve como consulta.
- **Sin el material de `cuadreSuper` no se puede hacer bien:**
  - C2 y C7: necesitan `MAPEO-PLAN-CUENTAS.csv`, `ASIGNACIONES-PENDIENTES.csv`, el B17 de mayo y los mayores;
  - C5: necesita `PLANTILLA-ASIENTOS-MANUALES.csv`;
  - C8: necesita el Excel oficial de la G48, por el orden de columnas.
  Lo que tenga datos por partícipe no entra al repositorio.

## Estado por cambio

| # | Estado | Nota |
|---|---|---|
| C1 | 🔵 **analizado: la premisa del documento no alcanza** (abajo) | Hace falta código en `cnt` + script de periodos |
| C2 | ⏸ espera el mapeo y el B17 de mayo | Necesita el periodo **mayo 2025** (C1) |
| C3 | ⏸ scripts sobre la base del proyecto; coordinar con `omen-saa-1` | |
| C4 | ⏸ código en crd/Petro; coordinar con `omen-saa-1` | Riesgo detectado en `ProcesoCargaPetroServiceImpl:323` (abajo) |
| C5 | ⏸ espera la plantilla | `TipoAsientos` no tiene DIARIO ni APERTURA (abajo) |
| C6 | 🔵 **inventario hecho** (abajo) | |
| C7–C10 | ⏸ | |

## C1 — Los periodos de 2025 NO se pueden crear «con el mismo mecanismo»

Medido en el código el 2026-10-02:

- **Todo el orden de periodos va por `PRDOCDGO` (la secuencia), no por año y mes ni por la fecha de
  inicio.** Los periodos de 2025 creados hoy saldrían con códigos **mayores** que los de 2026, y el
  sistema los trataría como posteriores. Las consultas afectadas son `PeriodoDaoServiceImpl`:
  `selectRangoPeriodos` (:98), `selectPeriodoAnterior` (:292), `selectAnteriorMayorizado` (:271),
  `periodoMayorizacionDesmayorizacion` (:75), `selectByEmpresaMaxFecha` (:149) y
  `selectMaximo/MinimoAnteriorByEstadoEmpresa` (:178, :226).
- **Lo que rompería:**
  - la mayorización y el arrastre de saldos (`MayorizacionServiceImpl:119,129-136`);
  - los saldos bancarios (`SaldoBancoServiceImpl:180`);
  - el saldo inicial del mayor analítico y de **todos los balances** (`PlanCuentaServiceImpl.saldoCuentaFechaEmpresa:620`);
  - la comparación de rangos de la pantalla de mayorización (`mayorizacion-proceso.component.ts:94`).
- **La pantalla de periodos solo deja crear el mes siguiente al último** (`periodo-contable.component.ts:163-207`).
- **El asiento de apertura al 31-may-2025 (C2) necesita el periodo MAYO 2025.** Hay que crear de
  mayo a diciembre, no de junio a diciembre.
- **Solución elegida:** que esas consultas ordenen por la fecha de inicio del periodo (`PRDOINCO`), y
  un script que cree los periodos de mayo a diciembre 2025 en estado ACTIVO. El orden queda correcto
  aunque los códigos estén desordenados, y sirve también para el futuro. La alternativa de insertar
  códigos menores que los de 2026 depende de que haya huecos libres en la secuencia, y es frágil.
- `cnt` es compartido con `omen-saa-2`: se avisa antes de tocar `PeriodoDaoServiceImpl`.

### Hallazgos de C1 que valen para todo el re-proceso

- 🔴 **Un periodo MAYORIZADO no bloquea asientos nuevos.** En `AsientoServiceImpl.saveSingle` el
  periodo se asigna antes del chequeo de MAYORIZADO (:741-743), así que ese chequeo es código muerto.
  `validacionAsiento` (:790) solo bloquea CERRADO. Al re-procesar, un asiento puede caer en un mes ya
  mayorizado sin error y quedar fuera del mayor. **Antes de re-procesar un mes, ese mes y los
  siguientes tienen que estar desmayorizados.**
- Los reversos (`anulaAsiento` → `reversionAsiento(id)`, `AsientoServiceImpl:361`) se fechan **hoy**.
  Ya existe el método `reversionAsiento(id, fecha)` (:367).
- El número de asiento es el máximo por tipo y empresa, **sin año**. Los de 2025 salen con números
  mayores que los de 2026. Es cosmético; `numeroAlterno` sí va por año y mes.
- `TipoAsientos` no tiene APERTURA ni DIARIO. Para C2 y C5 hay que crear o elegir un tipo de asiento
  en `CNT.TPAS` de la empresa 1236.

## C6 — De dónde saca la fecha cada motor

**El centro ya está bien:** `AsientoContableServiceImpl.generarAsiento` recibe la fecha por parámetro
y el periodo se resuelve por mes y año de esa fecha. No existe una «fecha del sistema» configurable.
El trabajo está en los llamadores.

**Ya aceptan una fecha explícita:** pago de cuota y pago con aportes, abono, precancelación, cobros,
devolución de aportes, cierre de cartera, mora (por parámetro), Petro (último día del mes de la
carga, confirmado), nómina (rol, provisiones, pago, liquidación), cxp/cxc/tsr por la fecha del
documento o por parámetro.

**Usan HOY y hay que darles una fecha contable opcional (null = hoy):**

| Motor | Dónde |
|---|---|
| Pensión complementaria: asiento del seguro médico | `PagoPensionComplementariaServiceImpl:2837-2838` |
| Pensión: los pagos (PGPR) y el reverso de APRT | `:2257`, `:2582`, `:3268` |
| Devolución de aportes: reemisión y contra-movimientos | `DevolucionAporteServiceImpl:1414`, `:1815` |
| Desembolso del préstamo (PGPR) | `PrestamoServiceImpl:351` |
| Reverso de un pago de préstamo | `ProcesoPagoPrestamoServiceImpl:1257` → `reversionAsiento(id)` |
| Recálculo de cuota sin fecha de pago | `MotorPagoPrestamoServiceImpl:229`, `:765` (usar la fecha del último pago vigente) |
| Orden de pago de nómina (fecha de emisión) | `GeneracionOrdenPagoServiceImpl:213` |
| Respuesta del banco en Tesorería | `PagoProgramadoServiceImpl:2031` |
| Tesorería legado (cheque, cobro, depósito, transferencia…) | `AsientoServiceImpl.insertarCabeceraAsiento:531` (o dejarlos fuera del re-proceso) |

**Riesgos para el re-proceso:**

- **Petro, `ProcesoCargaPetroServiceImpl:323`:** decide si una cuota está ACTIVA o EN_MORA comparando
  con **hoy**, no con el mes de la carga. Al re-procesar junio 2025 marcaría en mora cuotas que ese mes
  estaban vigentes. Va en C4.
- `parseFecha` en cxp, cxc y tsr cae en silencio a **hoy** si la fecha viene mal formada
  (`PagoProgramadoServiceImpl:3897-3906`). En un re-proceso eso deja asientos con fecha de hoy sin aviso.
- Un mismo hecho queda con dos fechas: en la pensión, la cartera va a fin de mes y el PGPR va hoy; en
  el desembolso, el asiento va en `fechaInicio` y el PGPR va hoy.
- Para re-procesar la mora a fechas pasadas hay que llamar `calcularMoraDiaria(fechaCorte)` a mano:
  el timer siempre usa hoy.
