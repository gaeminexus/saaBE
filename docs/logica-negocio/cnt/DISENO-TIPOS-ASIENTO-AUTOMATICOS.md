# Tipos de asiento automáticos IN-A / EG-A / DI-A y numeración por módulo (P5, desde una vigencia parametrizable; por defecto el 01-11-2026)

**Equipo:** `omen-saa-2` · **Escrito:** 2026-10-27 por `omen-saa-2-arb`, **antes** de despachar (regla 7).
**Reemplaza** la regla del P5 de `PEDIDOS-CONTABILIDAD-2026-10-12.md` (que decía «sin banco → tipo del módulo, nunca
Diario»). **Fecha de vigencia: asientos con fecha desde la vigencia de la empresa (§7); sin configurar, el 2026-11-01.**
Donde abajo dice «2026-11-01» léase «la vigencia de la empresa» — el texto de §1–§6 se conserva como se escribió el 27-10.
**Actualizado 2026-10-28 (cuadre Superintendencia, C12):** §7 vigencia parametrizable, §8 re-proceso jun-2025 → nov-2026.

## 1. Decisiones del usuario (2026-10-27), textuales

> *«En los bancos debe existir un check que indique si es banco real o no. Para todos los asientos contables que
> tengan la cuenta contable de una cuenta bancaria de un banco real, en caso de ser ingreso de dinero, entonces se debe
> generar un asiento tipo INGRESO, si es salida de dinero, se debe generar un EGRESO, y si en el asiento no hay una
> cuenta contable de un banco real entonces debe salir tipo diario. Esta numeración debe ser mensual igual que antes por
> tipo asiento sistema. Pero ahora se desea que el módulo sea otro dato en los asientos automáticos. Para conseguir esto
> vamos a crear los tipos de asiento IN-A (INGRESO AUTOMATICO) con código alterno 7, EG-A (EGRESO AUTOMATICO) con código
> alterno 8, DI-A (DIARIO AUTOMATICO) con código alterno 9. Pero además se debe crear un nuevo campo que indique el módulo
> origen del asiento y que también tenga una numeración mensual por módulo, para tener doble control.»*

- Caja chica (no toca bancos): **DI-A**.
- Reversos de asientos anteriores al 01-11: **conservan el tipo del original**.
- **Tipos ya creados por el usuario en Tipos de Asientos del Sistema** (captura del 27-10): `T-INGRESOS` 4, `T-EGRESOS` 5,
  `RECURSOS HUMANOS` 6, `CXC` 2, `CREDITOS` 1, `CXP` 3, **`IN-A (INGRESO AUTOMATICO)` 7, `EG-A (EGRESO AUTOMATICO)` 8,
  `DI-A (DIARIO AUTOMATICO)` 9**, todos activos.

## 2. La regla

Para **todo asiento automático** (el que arma el sistema, no el manual de contabilidad) con **fecha ≥ 2026-11-01**:

1. **Banco real:** una línea es «de banco real» si su cuenta contable es la `planCuenta` de una **cuenta bancaria propia**
   (`TSR.CNBC`) cuyo **banco** (`TSR.BNCO`) tiene el check nuevo **«banco real»** encendido.
2. **Tipo:**
   - hay líneas de banco real y el **neto** de esas líneas (debe − haber) es **> 0** → **IN-A (7)**;
   - neto **< 0** → **EG-A (8)**;
   - neto **= 0** con líneas de banco a los dos lados (transferencia entre cuentas propias) → **EG-A (8)** *(default del
     árbitro; decidible)*;
   - **sin** líneas de banco real → **DI-A (9)**.
3. **Numeración por tipo:** mensual, como hoy (`asignaNumeroAlterno`). ⚠️ Hoy el prefijo son **las 3 primeras letras del
   nombre**: con `IN-A (INGRESO AUTOMATICO)` saldría `IN--2026-11-0001`. Regla nueva: si el nombre tiene `" ("`, el prefijo
   es lo que está antes (`IN-A`, `EG-A`, `DI-A`) → `IN-A-2026-11-0001`. Los nombres viejos no cambian de prefijo.
4. **Módulo origen (campo nuevo) y su numeración mensual (doble control):** cada asiento automático guarda el módulo que
   lo generó y un consecutivo mensual **por módulo y empresa**, con su número alterno (`CXP-2026-11-0001`). Módulos:
   `CRE` créditos · `CXC` · `CXP` · `TSR` tesorería · `RRH` RR.HH. · `CNT` contabilidad. De dónde sale el módulo de cada
   asiento lo decide el inventario del ítem 0 (§4): hoy los llamadores pasan un `codigoAlterno` (1–6) que es casi el
   módulo, pero con cruces (p. ej. el anticipo de proveedor de CxP pasa 5 = T-EGRESOS).
5. **Reversos** de asientos con fecha < 2026-11-01 → tipo del original (como hoy). Reversos de asientos nuevos → mismo tipo
   que el original (IN-A/EG-A/DI-A) y mismo módulo.
6. **Asientos manuales** de contabilidad: no cambian.
7. **Antes del 2026-11-01:** todo sigue como hoy.

## 3. Datos (script `e2-111`, a escribir con el inventario en la mano)

- `TSR.BNCO`: columna nueva «banco real» (NUMBER(1), 0/1). El default y qué bancos van en 0 se deciden con el listado de
  bancos y sus cuentas (el script lo muestra primero).
- `CNT.ASNT`: columnas nuevas para módulo origen, consecutivo mensual por módulo y número alterno por módulo.
- Los tipos 7/8/9 ya existen (los creó el usuario); el script solo los **verifica** por empresa.

## 4. Lo que falta medir (ítem 0 del BE, antes de diseñar el resto)

- Todos los llamadores de `AsientoContableServiceImpl.generarAsiento` (6 y 7 argumentos), con el `codigoAlterno` y el
  `moduloSistema` que pasan, y su módulo real.
- Todo lugar que crea un asiento **sin** pasar por `generarAsiento` (legacy de tsr `insertarCabeceraAsiento`, cierres,
  reversos, procesos de `crd`, copia de asiento, etc.).
- Cómo se crean los asientos manuales (para excluirlos).
- La relación `CNBC` → `BNCO` → `planCuenta`, y qué filas de `BNCO` existen.
- Dónde se calcula `numeroMesTipo` y si el consecutivo por módulo puede seguir el mismo patrón (MAX+1) sin colisiones.

## 5. Impacto en otros equipos

El cambio es **central** (`generarAsiento`): cambia el tipo de **todos** los asientos automáticos, también los de `crd`
(`omen-saa-1`, que hoy usa `CREDITOS`) y los que toca `lap-saa-1`. Hay que avisarles con autorización del usuario antes de
subirlo. `omen-saa-1` además está cambiando validaciones de usuario en `cnt`.

## 6. Lo que midió el inventario (omen-saa-2-be, 2026-10-27) y lo que define

- **H1 — el tipo se busca solo por empresa + alterno** (`TipoAsientoDaoService.selectByAlterno`; `PLNTSSTM` no lo lee
  nadie). El alterno **7 ya lo usaba `TipoAsientos.TRANSFERENCIAS`** (`generaCabeceraTransferencia`, legacy de tsr). Si
  en una empresa hay otra fila con alterno 7, IN-A choca con ella. → El `e2-111` BLOQUE 0a lo mide. **La búsqueda de
  7/8/9 se hace con un método nuevo que exige `PLNTSSTM = 1`**; el método viejo no se toca.
- **H2 — los `codigoAlterno` que pasan los llamadores no sirven para saber el módulo** (3 = todas las compras y la
  retención V2 emitida; 4 y 5 mezclan tesorería, cxc y cxp). **H3 — el módulo que se graba hoy es CXC por defecto**
  (todo crd, todas las compras). Por eso el módulo origen **no** se deduce de lo que se graba hoy.
- **Módulo origen, regla definitiva:**
  - los métodos de negocio de `AsientoContableServiceImpl` llaman a una sobrecarga nueva de `generarAsiento` **con el
    módulo explícito** (tabla de abajo);
  - los llamadores externos que usan `generarAsiento` directo (6/7 args) se resuelven así, sin tocar su código:
    alterno **1 → CRE** (todo crd), alterno **6 → RRH** (rhh), y si no, por el `moduloSistema` que pasan
    (TESORERIA → TSR, CXP → CXP, RRHH → RRH, CXC → CXC). Así **no se toca código de `crd`**.

  | Método | Módulo |
  |---|---|
  | factura, NC, ND, retención v1/v2 emitidas, anticipo de cliente, aplicación de anticipo de cliente, cobro por transferencia, retención recibida (v1/v2) | CXC |
  | factura/NC/ND/liquidación de compra (incl. intermediario y reembolso), anticipo de proveedor, devolución de saldo a favor, aplicación de anticipo de proveedor, pago por transferencia a proveedor | CXP |
  | egreso e ingreso de tesorería, egreso contra anticipo, gasto/aplicación/reposición/ajuste de caja chica | TSR |
  | anticipo de empleado | RRH |
  | liquidación de compra emitida por cxc | CXC |

- **Asientos que no pasan por `generarAsiento`:** el reverso conserva tipo **y módulo** del original y toma **su propio**
  consecutivo de módulo; la copia (`generaCopia`) y los manuales (`POST/PUT /asnt`) quedan **sin módulo**; el cierre de
  período no cambia. El legacy de tsr (`insertarCabeceraAsiento`: cheques, cobros, depósitos, débitos/créditos, pagos,
  transferencias) crea la cabecera **antes** que las líneas: en esta pasada **no** se le aplica la regla; se mide si se usa
  todavía (asientos de octubre de esos tipos) y se decide aparte.
- **No hay nada que distinga un asiento manual de uno automático** en la entidad: la regla se aplica solo en el camino de
  `generarAsiento`, que es por definición automático.
- **Consecutivos sin protección:** `numeroMesTipo` es MAX+1 sin bloqueo ni UK; el de módulo copia el mismo patrón (mismo
  riesgo de carrera, ya existente). No se agrega UK en esta pasada.

## 7. Vigencia parametrizable (C12, 2026-10-28)

**Por qué.** El cuadre con la Superintendencia (decisión **D-006** del líder del proyecto, 28-oct) regenera la contabilidad
de **jun-2025 → nov-2026** con esta misma regla (IN-A / EG-A / DI-A, módulo origen y numeración paralela por módulo y
mes). Con la fecha fija `2026-11-01` del commit `e7103b60` ningún asiento de 2025 la tomaba: la fecha pasó a ser un dato.

**Cómo (rubro de configuración, el patrón de la casa).** Se eligió un **detalle de rubro en `SCP.PRBR`/`SCP.PDTR`**
porque es como el repo guarda ya sus banderas de proceso (`CRD_PARAMETROS_CONTABILIDAD` 237, `CRD_GENERACION_POR_FALTANTE`
242): se lee con el DAO que existe, se cambia con un `UPDATE` revisable sin redesplegar y se puede desactivar
(`PDTRESTD = 0`). Se descartó una propiedad de sistema de WildFly (no es por empresa, exige reiniciar y no queda en la
base que se respalda) y una columna nueva en la empresa (DDL por un solo dato).

| Pieza | Valor |
|---|---|
| Rubro | `Rubros.CNT_PARAMETROS_ASIENTOS_AUTOMATICOS = 249` (`PRBRCDGO = PRBRALTR = 249`) |
| Detalle | **uno por empresa**: `PDTRALTR = PJRQCDGO` de la empresa; `PDTRALTR = 0` = todas las que no tengan detalle propio (`CntParametroAsientoAutomatico.VIGENCIA_TODAS_LAS_EMPRESAS`) |
| Valor | `PDTRVLRV` = fecha `yyyy-MM-dd` (p. ej. `2025-06-01`); `PDTRESTD = 1` |
| Lectura | `AsientoContableService.fechaVigenciaTiposAutomaticos(idEmpresa)`: detalle de la empresa → detalle 0 → `TipoAsientoAutomatico.FECHA_VIGENCIA_DEFECTO` (**2026-11-01**). Un valor ilegible se registra en el log y cae al defecto. **Nunca lanza**: lee por lista (`selectByCodigoAlternoRubro`, solo activos), no con `getSingleResult`, para que la falta de fila no marque la transacción del asiento para rollback. |
| Comparación | `fechaAsiento >= vigencia` → regla. Siempre contra la **fecha del asiento**, nunca contra `now()`. |
| Sin configurar | comportamiento **idéntico** al de `e7103b60`. |
| Script | `sql/c12-01-vigencia-tipos-automaticos.sql`: crea el rubro 249 y el detalle de la **1236 con `2025-06-01`**, con controles antes/después, sin `COMMIT`, **no ejecutado**. Solo para la base de trabajo del proyecto; en producción no se corre hasta que el líder lo decida. |

**Qué cambia en el código** (`AsientoContableServiceImpl.generarAsiento(…, moduloOrigen)`): `FECHA_VIGENCIA` (constante)
→ `fechaVigenciaTiposAutomaticos(idEmpresa)`; el resto de la regla (§2) no se toca. Los manuales
(`generarAsientoManual`, cargador C5, APERTURA) siguen fuera con cualquier fecha.

### 7.1 Módulos dentro de la regla — todos (decisión del 2026-10-28)

**Ningún módulo queda fuera.** El líder del proyecto del cuadre decidió el 28-oct (cierra el pendiente de D-006) que
**Créditos (`CRE`) y Nómina (`RRH`) también** salen IN-A / EG-A / DI-A, con su numeración paralela por módulo
(`CRE-AAAA-MM-NNNN`, `RRH-AAAA-MM-NNNN`). El **único criterio** de ingreso/egreso es si el asiento tiene la cuenta
contable de una cuenta bancaria de un banco real (§2.1–2.2); si no la tiene, DI-A. Es exactamente lo que implementó
`e7103b60` (§5): la regla reclasifica el tipo de **todos** los asientos automáticos que pasan por `generarAsiento`; CRE y
RRH solo se distinguen en que su módulo origen sale del alterno del llamador (1 → CRE, 6 → RRH) sin tocar su código.
No existe en el código ningún conjunto de módulos excluidos (se retiró el que C12 había dejado preparado).

Siguen fuera de la regla por diseño (§6), con cualquier fecha: asientos manuales, copia (`generaCopia`), cierre de
período y el legacy de tsr (`insertarCabeceraAsiento`). Los dos asientos de **apertura** del 31-may-2025 (ASNT 12043 y
12044) los pasó el líder a DI-A por SQL (`DI-A-2025-05-0001/0002`, módulo `CNT`); no los toca ningún proceso.

## 8. Re-proceso jun-2025 → nov-2026 (cuadre Superintendencia)

1. **Orden de aplicación en la base de trabajo:** `e2-111` (ya aplicado: columnas `BNCOREAL`, `ASNTMDOR`, `ASNTNMMD`,
   `ASNTNAMD` presentes) → WAR con este cambio → `c12-01` (vigencia 2025-06-01 para la 1236, confirmada por el líder;
   pendiente de correr) → `c12-02` (solo verificación de bancos reales; la marca ya la puso el líder).
2. **Lo que la regla necesita para fechas de 2025, verificado el 2026-10-28 en la base de trabajo (dump del 28-oct):**
   - Tipos del sistema de la 1236: `IN-A (INGRESO AUTOMATICO)` 7 = PLNT 1257, `EG-A (EGRESO AUTOMATICO)` 8 = 1258,
     `DI-A (DIARIO AUTOMATICO)` 9 = 1259, todos `PLNTSSTM = 1`, `PLNTESTD = 1`; ninguna otra fila con alterno 7/8/9 en
     ninguna empresa (no hay choque con TRANSFERENCIAS). APERTURA quedó en el 10 (C5).
   - Períodos: 4523 (may-2025) a 4530 (dic-2025) y 4511–4522 (2026), todos abiertos salvo ene-2026 (estado 2).
   - Bancos (marca puesta por el líder el 28-oct, verificada con `c12-02`): **12 reales** (`BNCOREAL = 1`): 393 Pacífico
     (ahorros 1048142735 → `1.1.02.05.65`, corriente 7713878 → `.70`), 396 Internacional (corriente 520612384 → `.80`),
     398 Austro, 400 Amazonas (ahorros 4502425973 → `.40`), 401 Guayaquil, 402 Atlántida, 403 Comercial de Manabí,
     404 Alianza del Valle, 405 JEP, 406 Policía Nacional, 407 Oscus, 408 Mutualista Pichincha (2 cuentas); los 14
     planes que la regla toma como banco real son todos `1.1.02.05.xx`. **6 no reales** (`= 0`): 425 y 427 caja chica,
     432 cuenta transitoria (`2.3.01.15.01`), 433 arriendos anticipados, 435 anticipo a proveedores, 438 depósitos por
     confirmar. Las 21 cuentas bancarias de la 1236 tienen plan de cuenta enlazado; ninguna de ASOPREP sin él.
   - Detalle: `clasificarTipoAutomatico` no filtra por `CNBCESTD`; la cnbc 431 (inactiva, banco 425) apunta a la
     transitoria, pero con 425 en 0 ya no cuenta. Sin efecto hoy.
3. **Numeración por módulo en meses pasados, confirmada en el código:** `asignaNumeroModulo` usa
   `asiento.getNumeroMes()/getNumeroAnio()`, que `saveSingle` sincroniza **siempre** con `fechaAsiento`
   (`AsientoServiceImpl` 743-757) y `selectMaxNumeroMesModulo` filtra por empresa + módulo + mes + año. El número
   alterno por tipo (`asignaNumeroAlterno`) sale de los mismos campos. **No hay `LocalDate.now()` en la numeración.**
   Los `now()` que quedan en `AsientoServiceImpl` son fechas de negocio de caminos fuera de la regla (reverso sin fecha
   = anulación de hoy, copia, legacy de tsr, cierre) y están inventariados en `FECHA-CONTABLE-REPROCESO.md`.
4. **Reversos:** `generaCabeceraReversion(original, fechaReverso)` copia `tipoAsiento` y `moduloOrigen` del original y
   numera (tipo y módulo) en el mes/año de `fechaReverso`. Para anular dentro del re-proceso hay que pasar la fecha
   contable del mes (la sobrecarga sin fecha reversa hoy).
5. **Asientos ya existentes** con fecha entre 2025-06-01 y 2026-10-31 (4.723 de 2026 en la base: CREDITOS 1.495,
   T-EGRESOS 1.038, DIARIO 555, CXP 539, INGRESO 512, …) **no cambian** por configurar la vigencia: la regla solo actúa
   al generar. Los re-procesos los anulan y regeneran por su propio camino; lo que no se regenere conserva tipo y
   numeración viejos y queda sin módulo origen.
6. **Efecto del consecutivo mensual mezclado:** en un mes que mezcle asientos viejos (tipo CXP, INGRESO…) y regenerados
   (IN-A/EG-A/DI-A) los consecutivos por tipo no colisionan (son tipos distintos), pero el mes tendrá dos numeraciones.
   Es lo esperado hasta que el mes se regenere completo.
