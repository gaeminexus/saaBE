# Reglas de Generación de Reportes G40-G51

## Contexto General
- Endpoint: `POST http://{servidor}/saaBE/rest/ejrc/ejecutar`
- Body: `{ "mes": 5, "anio": 2026, "usuario": "jperez" }`
- El orquestador está en: `GeneracionReportesServiceImpl.java`
- Si un G ya está OK no se reprocesa. Si falla queda en estado 2 (Con novedades) y se puede volver a llamar.
- Cada registro insertado en las tablas G lleva el campo `detalleEjecucion` (FK a EJRD).

---

## Estados importantes

### EJRC (Cabecera ejecución)
| Valor | Significado |
|---|---|
| 1 | En proceso |
| 2 | Con novedades |
| 3 | Completo |

### EJRD (Detalle por G)
| Valor | Significado |
|---|---|
| 1 | OK |
| 2 | Con novedades / Error |
| 3 | Pendiente |

> D-038: la G41 ya no usa `ENTDIDST` (ver G41). La marca puede seguir existiendo para otros usos.

### Entidad (ENTD.ENTDIDST)
| Valor | Significado en G41 |
|---|---|
| 1 | Nuevo → incluir en G41 |
| 10 | Procesado → ya fue incluido en G41 |

---

## ✅ G40 — CreditoG40 — D-038/D-039 (29-oct-2026)

**Tabla destino:** `RPR.CG40` · **Fuente única:** `CRD.IGFN` (pantalla Información General del Fondo, un registro).
**Periodicidad:** semestral (junio y diciembre). **Manual:** Catastros FCPC v1.2 §2.1 (campos) y §4.1 (controles).

**Regla:** todos los valores salen de IGFN; el código no fija ninguno (se retiraron provincia `17`, cantón `01`, tipo
FCPC `3` y cambio de nombre `NO`). El generador solo lleva cada valor al tipo, la longitud y la codificación del manual,
y si un valor no cumple, **no genera** y la novedad del EJRD dice qué campo corregir en la pantalla (una estructura con
errores se considera información no recibida, §1).

| # | Campo (manual) | IGFN | Tratamiento |
|---|---|---|---|
| 1 | Tipo identificación C(1), tabla 4 | `IGFNTIDF` | tal cual; debe ser `R` |
| 2 | Identificación C(13) | `IGFNIDFC` | tal cual; 13 dígitos (RUC) |
| 3 | No. resolución C(15) | `IGFNNMRS` | tal cual; ≤ 15 |
| 4 | Fecha resolución | `IGFNFCRS` | obligatoria |
| 5 | Provincia C(2), tabla 6 | `IGFNPRVN` (nombre) | código INEC de `CRD.PRVN.PRVNCDAL` por nombre (o el código si IGFN ya lo trae) |
| 6 | Cantón C(2), tabla 7 | `IGFNCNTN` (nombre) | código de `CRD.CDDD.CDDDCDAL` dentro de la provincia |
| 7 | Dirección C(60) | `IGFNDRCC` | ≤ 60 |
| 8 | Teléfonos C(30) | `IGFNTLFN` | texto tal cual (el 0 inicial es dato de IGFN) |
| 9 | Correo C(60) | `IGFNCEFD` | ≤ 60 |
| 10 | Tipo sistema, tabla 193 | `IGFNTPSS` | D/I/M |
| 11 | Tipo prestación, tabla 194 | `IGFNTPPR` | C/J/M |
| 12 | Tipo aporte, tabla 318 | `IGFNTPAP` | P/V |
| 13 | Tipo administración, tabla 319 | `IGFNTPAD` | B/P |
| 14 | Fecha traspaso (condicional) | `IGFNFCTR` | obligatoria si 13 = B |
| 15 | Tipo FCPC, tabla 320 | `IGFNTPFC` | `III`/`Tipo III` → `3` (1 = tipo I, 2 = II, 3 = III) |
| 16–17 | Resolución y fecha de cambio de estatuto (condicional) | `IGFNNRCE` (≤ 16), `IGFNFRCE` | fecha obligatoria si hay resolución |
| 18 | Cambio de nombre C(2), SI/NO | `IGFNCMNM` | solo `SI`/`NO` (§4.1); otro texto → no genera |
| 19–22 | Porcentajes N(5,2) | `IGFNPAPC`, `IGFNPARC`, `IGFNPAPJ`, `IGFNPARJ` | 0 a 999,99 |
| 23–24 | Valor aporte personal cesantía/jubilación N(5,2) = aporte **mínimo** | `IGFNVARC`, `IGFNVARJ` | 0 a 999,99 |

**Retirada:** la regla "valorAportePersonalCesantia + valorAportePersonalJubilacion = Σ saldoAportePersonal de la G42"
(contradice el manual: el campo es el aporte mínimo, Numérico(5,2), máximo 999,99).

**Archivos:** `GeneracionG40ServiceImpl`, `CatalogoGeograficoDaoService/Impl` (códigos de tablas 6, 7 y 50 desde los
catálogos del SAA, compartido con G45 y G46).

---

## ✅ G41 — ParticipeActivoG41 — D-038 (29-oct-2026)

**Tabla destino:** `RPR.CG41` · **Registro:** `RPR.SG41` (script `sql/337_D038_REGISTRO_G41.sql`).
**Manual:** Catastros §2.2 y §4.2: mensual, solo los partícipes que **ingresan (I)** o cuyo estado **cambia (A)**.

**Regla (GeneracionG41ServiceImpl):**
1. **Universo:** los sujetos de la G42 de la misma ejecución que no están en la G43 ni en la G44 del mes. Orden de
   generación: G42 → G44 → G43 → G41.
2. **Estado del partícipe (tabla 195):** `A` si aportó en el mes (aporte personal del mes de la G42 > 0 = aportes 9/11
   > 0 del período efectivo), `P` si no.
3. **Contra lo último reportado** (`RPR.SG41`, fila con el mayor período < mes): no está → `I`; cambió → `A`;
   no cambió → no va. Cada registro emitido se agrega a SG41 (origen `EJECUCION`). Regenerar el mes borra primero las
   filas `EJECUCION` del mes. Si existen filas de meses posteriores, avisa que hay que regenerarlos.
4. **Mes presentado:** si el período tiene filas de origen `SEMILLA`/`HM41` (mayo 2025), la G41 reproduce lo presentado
   (las 66 filas con estado de registro) y no toca el registro.
5. **Campos:** tipo de identificación de la G42; género `HOMBRE→M`, `MUJER→F` (tabla 8); estado civil desde
   `EXTRESCV` (tabla 183: CASADO→C, SOLTERO→S, DIVORCIADO→D, VIUDO→V, `…UNION DE HECHO`→U); fecha de nacimiento
   `EXTRFCNC`; fecha de ingreso `PRTCFCIF`; tipo de sistema = `IGFNTPSS` del fondo; base de cálculo `S` y relación
   laboral `N` (el SAA no guarda esos datos por partícipe, N11); **fecha de actualización = corte en `A` y en blanco en
   `I`** (§4.2: "caso contrario se registrará en blanco"; mayo la llenó también en el I).
6. **No se reportan** cambios de estado civil ni de relación laboral (el SAA no guarda su historia) ni `E` (decisión
   pendiente de ASOPREP). Ya no se usa `ENTDIDST = 9`.

**Semilla de SG41:** estado de mayo 2025 de cada sujeto de la HM42 (`A` si `HM42APPR > 0`) y la HM41 donde existe.
**Junio 2025 esperado:** 2 `I` + 13 `A` (A→P) + 47 `A` (P→A) = **62** (el diagnóstico contaba 64 con 4 `I`: 2 de esos 4
solo estaban en la G42 por el sueldo de HOY de `HSTR`; con el aporte personal del período ya no entran a la G42).
**Mayo:** la regla con el estado de abril de `APRT` reproduce 64 de los 66 presentados (control 4.b del script 337); con
el estado de mayo de `APRT` daría 477, porque el período mayo está contaminado por la migración: por eso mayo es "mes
presentado".

---

## ✅ G42 — SaldoCuentaG42 — D-038 (29-oct-2026)

**Tabla destino:** `RPR.CG42` · **Fuente:** `CRD.APRT` + `CRD.TPAP` (aportes vigentes, `AporteVigenteSql`).
**Manual:** Catastros §2.3 y §4.3.

| Campo | Regla |
|---|---|
| Tipo de prestación (3, tabla 194) | Por el saldo vigente al corte de cada **grupo de prestación** (`CRD.TPAP.TPAPTPPR`, script `338`): `M` si J ≠ 0 y C ≠ 0; `J` si solo J; `C` si solo C o ninguno (tolerancia medio centavo). J = 9, 10, 13, 15, 22, 23, 24, 25; C = 11, 12, 14, 16, 21. Antes: presencia de alguna fila 9/11 (reproducía 1.745 de 4.585 en mayo; ahora 4.564). |
| Aporte patronal (4) | 0 (no hay aporte patronal mensual: IGFN 0 %) |
| Aporte personal (5) | Σ aportes 9/11 > 0 vigentes con **período efectivo = mes** (`AporteDaoServiceImpl.selectAportePersonalDelPeriodoPorEntidad`). Sin filtro de fecha de caja: el aporte de junio que llega en julio es de junio. Antes: `HSTR` vigente hoy (un mes pasado salía con el sueldo de hoy). |
| Saldo patronal (7) | Σ tipos 3, 13, 14 al corte |
| Saldo personal (8) | Σ demás tipos activos no `RE` al corte |
| Rendimiento (10) | Σ tipos `RE` al corte |

Universo: entidades con algún valor > 0 (los negativos van en 0). **Junio 2025:** 4.585 sujetos (antes 4.587: 2 sujetos
sin saldo ni aporte en junio entraban solo por el sueldo de hoy), aporte personal 166.540,22 en 2.051 sujetos (antes
168.412,92 en 2.100), 22 cambios de tipo de prestación frente a mayo (antes 2.838).
Control del manual §4.3 "aporte personal (debe ser mayor a cero)": no se aplica fila a fila (los partícipes pasivos
aportan 0; mayo se aceptó con 0 en 2.570 filas).

**Archivos:** `GeneracionG42ServiceImpl`, `AporteDaoService/Impl` y `AporteService/Impl`
(`selectSumaPorGrupoPrestacionPorEntidad`, `selectAportePersonalDelPeriodoPorEntidad`; se retiró
`selectTiposAportePorEntidad`).

---


## ✅ G43 — ParticipeCesanteG43 — COMPLETADO

> **D-038/D-039 (29-oct-2026), vigente — prevalece sobre lo que sigue.** Manual de catastros §2.4/§4.4.
> - No son cesantes los jubilados: se omiten los sujetos de la G44 del mismo mes (la G44 se genera antes) y las entidades en estado de jubilado (3, 6, 7). Junio 2025: los 2 registros de la ejecución 565 eran jubilados pasivos de la G44 cuyo saldo se agotó con el último pago de pensión → la G43 de junio queda **vacía**.
> - Fecha de liquidación: la del último movimiento negativo del mes (la devolución real); sin movimientos, el corte.
> - Fecha de término de la relación laboral: `PRTCFCSL` si existe y es anterior a la liquidación; si no, el día anterior a la liquidación (criterio de 8 de las 11 filas de mayo 2025). El SAA no guarda la fecha de salida laboral (solo 17 de 7.311 partícipes la tienen).
> - Imposiciones personales (9, 11) y patronales (13, 14): meses de devengo **hasta el corte** (antes contaba también los posteriores).
> - Saldo de la cuenta individual: el de la G42 anterior (patronal + personal + voluntario + rendimiento); si no hay, lo liquidado en el mes. Compensados = cruce con préstamos del mes (`APRTTPMV` 4); pagados = liquidado − compensado. Así se cumple compensados y pagados ≥ 0 y ≤ saldo (§4.4).

**Tabla destino:** `RPR.CG43`  
**Fuente principal:** `RPR.CG42` del mes anterior (o `RPR.HM42` si es primera ejecución)  
**Fuente de comparación:** `RPR.CG42` del mes actual

**Regla:**
- Se compara el universo de entidades que tenían saldo en **G42 del mes anterior** contra las que tienen saldo en el **G42 del mes actual**.
- Toda entidad que **estaba en el mes anterior** pero **ya no aparece en el mes actual** → es un partícipe cesante → se inserta en CG43.
- **Si no existe EJRC del mes anterior** (primera ejecución histórica) → se usa `RPR.HM42` (HistoricoG42) como universo anterior en lugar del CG42 previo.
- Los campos de liquidación (`fechaTerminoRelacionLaboral`, `fechaLiquidacion`, `saldoCuentaIndividual`, `valoresCompensados`, `valoresPagados`) quedan en `null` — provienen de procesos operativos de liquidación externos a esta generación.
- `numeroImposicionesPersonales` (tipoAporte IN 9,11) y `numeroImposicionesPatronales`
  (tipoAporte IN 13,14) **sí se calculan** (nota desactualizada del párrafo anterior,
  corregida 2026-08-27): `AporteDaoServiceImpl.selectCountImposicionesPersonalesPorEntidad`
  / `PatronalesPorEntidad`. Ambos cuentan **MESES DE DEVENGO** (`COUNT(DISTINCT` periodo
  efectivo`)`), no filas — ver la regla general más abajo, "CRD.APRT bajo el modelo de
  devengo — filas ≠ meses".

**Lógica paso a paso:**
1. Obtener el EJRC actual + calcular `mesPrevio/anioPrevio`.
2. Obtener los registros CG42 del EJRD G42 del EJRC actual → `Set<identificacion>` como "entidades activas".
3. Buscar EJRC del mes anterior con `selectByMesAnio(mesPrevio, anioPrevio)`:
   - Si existe → obtener EJRD G42 de ese EJRC → `selectByDetalle(codigoEJRD)` → universo anterior.
   - Si NO existe → `HistoricoG42DaoService.selectAll("HistoricoG42All")` → universo anterior.
4. `cesantes = universoPrevio - entidadesActuales`
5. Por cada cesante → INSERT en CG43 con `identificacion`, `tipoIdentificacion` y `detalleEjecucion`.

**Archivos creados/modificados:**
- `SaldoCuentaG42DaoService.java` → método `selectByDetalle(Long codigoDetalle)` agregado
- `SaldoCuentaG42DaoServiceImpl.java` → implementación del método anterior
- `SaldoCuentaG42Service.java` → método `selectByDetalle` expuesto
- `SaldoCuentaG42ServiceImpl.java` → implementación delegada
- `DetalleEjecucionReporteDaoService.java` → método `selectByEjecucionYTipo(Long, String)` agregado
- `DetalleEjecucionReporteDaoServiceImpl.java` → implementación del método anterior
- `DetalleEjecucionReporteService.java` → método `selectByEjecucionYTipo` expuesto
- `DetalleEjecucionReporteServiceImpl.java` → implementación delegada
- `GeneracionG43Service.java` → interface creada
- `GeneracionG43ServiceImpl.java` → implementación completa con la lógica descrita
- `GeneracionReportesServiceImpl.java` → `@EJB g43Service` agregado + `case "G43"` activado

---

## ✅ G44 — ParticipeJubiladoG44 — COMPLETADO

> **D-039 (29-oct-2026).** Caso 3 (cuotas a descontar > pensión): compensados = pensión y neto = 0 (antes compensados = cuotas − pensión, y pensión ≠ neto + compensados). Sin efecto en junio 2025 (ningún caso). Pendiente, sin cambio: el valor de la pensión sale de `VPPC` sin período (mismo defecto que tenía la G42 con HSTR); fuente alternativa con período: APRT tipo 23 con `APRTTPMV` 9 del mes.

**Tabla destino:** `RPR.CG44`  
**Fuente principal:** `CRD.ENTD` con `idEstado = 30` (jubilados)  
**Fuente imposiciones:** `CRD.APRT` con `tipoAporte.codigo IN (9, 11)` — COUNT agrupado por entidad  
**Fuente valorPension / valorNetoRecibir:** `CRD.VPPC` (ValorPagoPensionComplementaria) — campo `valorPagar`, buscado por entidad  
**Fuente saldoCuenta:** `CRD.APRT` con `tipoAporte.codigo = 23` — SUM del campo `valor`, agrupado por entidad  

**Regla:**
- Se obtienen todas las entidades con `idEstado = 30`.
- Si no hay ninguna → G44 vacío, OK.
- Por cada entidad jubilada:
  - `imposicionesAcumuladas` = COUNT de aportes con `tipoAporte.codigo IN (9, 11)`
  - `valorPension` = `valorPagar` del primer registro de `VPPC` para esa entidad (null si no existe)
  - `valorNetoRecibir` = mismo valor que `valorPension` (mismo origen: VPPC)
  - `saldoCuenta` = SUM de `aportes.valor` con `tipoAporte.codigo = 23` (0 si no hay)
  - `jubilacionIess` = **FIJO: `"S"`**
  - Los campos `tipoJubilacion`, `fechaJubilacion`, `valoresCompensados` quedan en `null`

**Optimización:** Los SELECTs de COUNT e imposiciones y SUM de saldo se ejecutan una sola vez en BD agrupados por entidad (no hay queries individuales por jubilado). Solo `selectByEntidad` de VPPC se ejecuta por entidad.

**Archivos creados/modificados:**
- `AporteDaoService.java` → métodos `selectCountImposicionesJubilacionPorEntidad()` y `selectSumaSaldoCuentaJubilacionPorEntidad()` agregados
- `AporteDaoServiceImpl.java` → implementaciones con JPQL `COUNT`/`SUM GROUP BY`
- `AporteService.java` → mismos 2 métodos expuestos
- `AporteServiceImpl.java` → implementaciones delegadas
- `ValorPagoPensionComplementariaDaoService.java` → método `selectByEntidad(Long)` agregado
- `ValorPagoPensionComplementariaDaoServiceImpl.java` → implementación completa con `EntityManager`, `@PersistenceContext` y `obtieneCampos()`
- `ValorPagoPensionComplementariaService.java` → método `selectByEntidad(Long)` expuesto
- `ValorPagoPensionComplementariaServiceImpl.java` → implementación delegada
- `GeneracionG44Service.java` → interface creada
- `GeneracionG44ServiceImpl.java` → implementación completa con la lógica descrita
- `GeneracionReportesServiceImpl.java` → `@EJB g44Service` agregado + `case "G44"` activado

---

## ✅ G45 — NuevoParticipeG45 — COMPLETADO

> **D-039 (29-oct-2026).** Manual de crédito §3.1/§5.1. Estado civil (tabla 211) desde `EXTRESCV` con el mismo mapeo de
> la G41 (antes `S` fijo). Residencia (tablas 6, 7, 50): provincia y cantón de `EXTR` codificados con los catálogos
> `PRVN`/`CDDD`; si EXTR no da los dos, la dirección vigente `DRCC → PRRQ → CDDD → PRVN` (`resolverUbicacion`; con la
> G46 de mayo presentada EXTR coincide en 47 de 73, DRCC en 39); parroquia `PRRQCDEX` o, mientras no esté cargado,
> `50` (antes cantón `01` y parroquia `50` fijos). Patrimonio: si el sujeto no tiene saldo en la G42, sus ingresos
> (`EXTRSLTT`), como pide el manual ("en caso de que el deudor no posea bienes, se tomará como referencia el valor de sus
> ingresos"; control: > 0). Sin dato en el SAA (se mantienen y se documentan): cargas familiares 0, profesión `O`
> (`EXTRPRFS` es texto libre, falta una tabla de equivalencias a la tabla 216), actividad económica `009`, origen de
> ingresos `B`.

**Tabla destino:** `RPR.CG45`  
**Regla vigente (D-035, 29-oct-2026) — sujetos de riesgo NUEVOS.** El manual de la Superintendencia (Estructuras de
Operaciones de Crédito FCPC v2.4, 18-08-2023, §3.1 y §5.2) define la G45 con los titulares, garantes y codeudores de las
operaciones de crédito, y en los controles de la G46 exige que el sujeto "conste en la base de datos de la estructura G45":
la Superintendencia mantiene una base ACUMULADA de sujetos. Por eso:

- **G45 del mes = sujetos de la G46 del mes (titulares) y de la G50 (garantes/codeudores, hoy stub en 0) que todavía no
  constan en el registro `RPR.SG45`** con un período anterior al que se genera.
- `RPR.SG45` (DDL y carga inicial: `docs/logica-negocio/reportes/sql/336_D035_REGISTRO_SUJETOS_G45.sql`) es la copia local
  de esa base: una fila por sujeto con su primer período reportado y su origen — `SEMILLA` (reportados antes de mayo
  2025, período 202504), `HM45` (G45 presentada en mayo 2025, 202505) o `EJECUCION` (G45 generada por el SAA, con su EJRD).
- Al generar, cada sujeto nuevo se agrega con el período del mes. Al **regenerar** un mes se borran primero las filas
  `EJECUCION` de ese período (no se duplica). Si un sujeto ya estaba registrado con un período POSTERIOR (se generó antes
  un mes siguiente), su registro pasa al mes que se genera y el log pide regenerar ese mes siguiente.
- **Semilla (supuesto a confirmar con ASOPREP):** no se encontró la fecha en que ASOPREP empezó a reportar la G45. Se
  toma como ya reportado a todo sujeto con un préstamo concedido antes de mayo 2025 (estado fuera de 0, 1, 6, 7, 12) cuya
  última cuota vence en 2021-07 o después. Cualquier corte en (2019-08-31, 2021-07-31] reproduce la G45 de mayo; se elige
  el más tardío (semilla más chica: ante la duda se vuelve a reportar un sujeto en vez de omitir uno nuevo, que haría
  rechazar la G46). Con la base del 29-oct-2026: semilla 1.726 sujetos + 10 de la HM45.
- **Aceptación:** mayo 2025 = exactamente los 10 sujetos presentados (de 73 en la G46); junio 2025 = 1 (de 29), igual para
  cualquier corte del rango. Evidencia sin cédulas: `cuadreSuper/03-trabajo/meses/2025-06/sql/33-g45-registro.sql` y
  `resultados/33-g45-registro-01..04.csv`, `33-g45-reglas.txt`.
- Se descartó la regla de `LogicaArchivosSuper.txt` 311-313 (sin otro préstamo vigente/mora/plazo vencido ni cancelado con
  última cuota en el período): daba 13 en mayo (`33-g45-reglas.sql`); los 3 de más ya constaban en la base de sujetos.

**Datos de cada registro** (`LogicaArchivosSuper.txt` 315-337, sin cambios salvo el tipo de partícipe): patrimonio =
saldo aporte patronal + personal + rendimiento de la `CG42` del mismo EJRC; género, fecha de nacimiento y provincia desde
`EXTR`; fijos: estado civil `S`, actividad `009`, cantón `01`, parroquia `50`, profesión `O`, cargas `0`, origen `B`.
**Tipo de partícipe = `P` siempre** (D-035): el manual admite C cesante, J jubilado y P partícipe, pero la G45 de mayo
2025 (aceptada) trae P en los 10 y el área financiera (correo del 5-jun-2026) indica, por directriz de la
Superintendencia, que el jubilado se reporta como P. J y C no se usan (antes: J si la entidad era jubilado complementario).
La tabla de enriquecimiento de abajo (CG41, PerfilEconomico, Participe) es la versión original y ya no rige.

**Orden de generación (D-034/D-035):** la G45 depende de la G46 y de la G50 (y la G49 de la G48). El orquestador
(`GeneracionReportesServiceImpl`) procesa los EJRD en el orden de `GeneracionUnReporteServiceImpl.TIPOS_REPORTE`
(G40, G41, G42, G43, G44, **G46, G50, G45**, G47, G48, G49, G51), no en el `order by d.tipoReporte` del DAO. Desde
4184bb1c (5-oct-2026) la G45 corría antes que la G46, la encontraba vacía y salía con 0 registros sin error.

**Relación con la G48:** el manual pide cuenta individual 0 en la G48 para jubilados reportados en la G45 con `J`. Como
la G45 ya no usa `J`, esa condición no se da; la G48 mantiene su regla acordada (cuenta individual 0 si la entidad es
jubilado complementario, `ENTDIDST` 3, antes PK 30) — pendiente de confirmar con ASOPREP si se conserva.

**Nota histórica:** G41 corre antes que G45 y cambia `idEstado 1→10`, por eso G45 **no puede filtrar por `idEstado=1`**.

**Enriquecimiento por entidad:**

| Campo G45 | Fuente | Campo origen |
|---|---|---|
| `tipoIdentificacion` | `CG41` (viene de Entidad.tipoIdentificacion.nombre) | directo |
| `identificacion` | `CG41` | directo |
| `cargasFamiliares` | `Entidad` | `cargasFamiliares` |
| `genero` | `Exter` | `genero` |
| `estadoCivil` | `Exter` | `estadoCivil` |
| `fechaNacimiento` | `Exter` | `fechaNacimiento.toLocalDate()` |
| `profesion` | `Exter` | `profesion` |
| `provincia` | `Exter` | `provincia` |
| `canton` | `Exter` | `canton` |
| `parroquia` | `Direccion` (primera dirección) | `parroquia.nombre` |
| `patrimonio` | `PerfilEconomico` | `patrimonioNeto` |
| `origenIngresos` | `PerfilEconomico` | `origenOtrosIngresos` |
| `tipoParticipe` | `Participe` | `tipoParticipante.nombre` |
| `actividadEconomica` | `Participe` | `ingresoAdicionalActividad` |

**Regla:**
- Si no hay registros en CG41 del EJRC actual → G45 vacío, OK.
- Por cada registro de CG41 → buscar Entidad por `identificacion` (query en BD), luego enriquecer con Exter, Direccion, PerfilEconomico y Participe.
- Si alguna fuente no existe para la entidad → el campo queda `null` (no bloquea la generación).

**Archivos creados/modificados:**
- `ParticipeActivoG41DaoService.java` → método `selectByDetalle(Long codigoDetalle)` agregado
- `ParticipeActivoG41DaoServiceImpl.java` → implementación con query JPQL
- `EntidadDaoService.java` + `Impl` → método `selectByNumeroIdentificacion(String)` agregado
- `EntidadService.java` + `Impl` → mismo método expuesto
- `PerfilEconomicoDaoService.java` + `Impl` → método `selectByEntidad(Long)` agregado + clase completada con `EntityManager` y `obtieneCampos()`
- `PerfilEconomicoService.java` + `Impl` → mismo método expuesto
- `DireccionService.java` + `Impl` → método `selectByParent(Long)` expuesto (ya existía en el DAO)
- `GeneracionG45Service.java` → interface creada
- `GeneracionG45ServiceImpl.java` → implementación completa
- `GeneracionReportesServiceImpl.java` → `@EJB g45Service` agregado + `case "G45"` activado

---

## ⏳ G46 — NuevoPrestamoG46 — PENDIENTE

> **D-039 (29-oct-2026).** Manual de crédito §3.2/§5.2.
> - Solo operaciones concedidas: se excluyen los préstamos en estado 0, 1, 6, 7 y 12 (raíz, generado, pendiente, rechazado, anulado).
> - Destino geográfico (tablas 6, 7, 50): el SAA no guarda un destino propio del préstamo; se usa la residencia del sujeto (`GeneracionG45ServiceImpl.resolverUbicacion`): provincia y cantón de `EXTR` codificados con `PRVN.PRVNCDAL` y `CDDD.CDDDCDAL` (con la G46 de mayo presentada coinciden 47 de 73 operaciones, provincia 63); si EXTR no da los dos, la dirección vigente `CRD.DRCC` → `PRRQ` → `CDDD` → `PRVN` (39 de 73). Junio 2025: provincia igual en las 29, cantón distinto de 01 en 11. Parroquia: `PRRQCDEX` (código INEC); **mientras no esté cargado** (hoy vacío en las 1.551 parroquias), `50` = cabecera cantonal. Antes: cantón `01` y parroquia `50` fijos. Dato sucio conocido: en Pichincha, POMASQUI, SANGOLQUI y CAPELO llevan `CDDDCDAL` 1.
> - Sin cambio, documentado: estado de la operación `N` (las marcas de novación/reestructuración están en 0 en junio), periodicidad `ME`, frecuencia de revisión 365, garantías `GA`, tipo de crédito por la inicial del producto (E→Q; frágil: mapear desde `TPPR`).

**Tabla destino:** `RPR.CG46`  
**Regla:** pendiente — el usuario la proporcionará

---

## ✅ G47 — NovacionG47 — COMPLETADO

> **D-039 (29-oct-2026), pendiente de decisión — sin cambio de código.** El generador filtra `ESTADO_NOVACION = 9`, que en `EstadoPrestamo` es CANCELADO_POR_REVISAR (novación = 5, reestructuración = 13), no filtra por mes y deja nula la operación anterior (obligatoria). La fuente existe: `CRD.RLPR` (hijo `PRSTCDGH`, padre `PRSTCDGP`, tipo Novacion). Antes de cambiarlo hay que decidir la definición: RLPR da 30 novaciones en mayo 2025 y la G47 aceptada de mayo trae 1 (que no está en RLPR); la G46 de mayo reportó esos casos como `N`. Junio: 0 (correcto con cualquiera de las dos lecturas: ninguna de las 35 cancelaciones tiene novación).

**Tabla destino:** `RPR.CG47`  
**Fuente:** `CRD.PRST` (Prestamo) con `estadoPrestamo = 9`

**Regla:**
- Obtener todos los préstamos con `estadoPrestamo = 9` (novaciones)
- Si no hay ninguno → G47 vacío, OK
- Por cada préstamo mapear a CG47

**Mapeo de campos:**
| Campo CG47 | Fuente | Valor |
|---|---|---|
| `tipoIdentificacion` | — | **FIJO: `"C"`** |
| `identificacion` | `Prestamo.entidad.numeroIdentificacion` | dinámico |
| `numeroOperacion` | `Prestamo.idAsoprep` (como String) | dinámico |
| `fechaNovacion` | `Prestamo.fecha.toLocalDate()` | dinámico |
| `numeroOperacionAnterior` | — | `null` (no disponible) |

**Archivos creados/modificados:**
- `PrestamoDaoService/Impl` → método `selectByEstado(Long estado)` agregado
- `PrestamoService/Impl` → método expuesto
- `GeneracionG47Service.java` + `GeneracionG47ServiceImpl.java` → creados
- Orquestador → `@EJB g47Service` activo, `case "G47"` activado

---

## ✅ G48 — SaldoOperacionG48 — COMPLETADO

> **D-038/D-039 (29-oct-2026).** Manual de crédito §3.4/§5.4.
> - Campo 20 **cuota del crédito = solo el capital** de las cuotas que vencen en el mes de reporte (columna `CAPMES` de `selectOperacionesAlCorte`; en el modo normal, capital de la cuota del mes del grupo 1). Antes: `DTPRCTAA` (capital + interés) de la cuota ancla. Sin cuota que venza en el mes (plazo vencido) = 0.
> - Fecha de exigibilidad (22): **se mantiene** la de la cuota exigible (cuota impaga más antigua o próxima por vencer), lectura literal del manual; mayo 2025 llevaba el corte en todas las filas. Cambio de criterio con constancia en el acta (D-038).
> - Tasa (7): la del préstamo (`PRSTINNM`); las operaciones 54175 (préstamo 8323, 10 %) y 52802 (8331, 13 %), hipotecarias, se reportan con su tasa (mayo llevaba 9 en todas). No se cambia.
> - Control de conteo §5.4 G48(mes) = G48(anterior) + G46 − G49: junio 1.594 + 29 − 35 = 1.588; la 565 trae 1.589 por la operación 64481 (residuo de migración, ver `cuadreSuper/03-trabajo/meses/2025-06/sql/40-depura-64481.md`).
> - Pendientes documentados: dividendo (21) de la cuota ancla y no de la del mes; cuenta individual 0 por estado de entidad 3 y no por el código J de la G45 (hoy no hay J); sistema de amortización `FR` fijo (correcto para la cartera actual); tipo de crédito por inicial del producto.

**Tabla destino:** `RPR.CG48`

**Fuente principal:** `CRD.DTPR` (DetallePrestamo) — dos grupos de cuotas

---

### Grupos de cuotas

**GRUPO 1:** Cuotas con `fechaVencimiento` dentro del mes de ejecución y `estado != 7` (canceladaAnticipada).

**GRUPO 2:** La menor cuota (MIN numeroCuota) por préstamo con `fechaVencimiento < primerDíaMes`, `estado IN (2,3,5,6)` y `prestamo.estadoPrestamo IN (2,8,11)` — préstamos con cuotas vencidas sin pagar.

> Un préstamo que aparece en el Grupo 1 **no** se incluye en el Grupo 2 (se excluye por duplicado).

---

### Mapeo de campos

| Campo CG48 | Fuente | Valor |
|---|---|---|
| `tipoIdentificacion` | — | **FIJO: `"C"`** |
| `identificacion` | `Prestamo.entidad.numeroIdentificacion` | dinámico |
| `numeroOperacion` | `Prestamo.idAsoprep` (como String) | dinámico |
| `tipoCredito` | `Prestamo.producto.nombre.charAt(0)` | dinámico |
| `diasMorosidad` | días entre `fechaVencimiento` y último día del mes (si estado != 4) | calculado |
| `calificacionPropia` | según tabla de días (ver abajo) | calculado |
| `tasaInteres` | `Prestamo.interesNominal` | dinámico |
| `valorPorVencer` | `DetallePrestamo.saldoInicialCapital` | dinámico |
| `valorVencido` | Grupo 1 → `0`. Grupo 2 → `DetallePrestamo.capital` | calculado |
| `interesOrdinario` | `DetallePrestamo.interes` | dinámico |
| `costosOperativos` | — | **FIJO: `0`** |
| `interesMora` | — | **FIJO: `0`** |
| `valorDemandaJudicial` | — | **FIJO: `0`** |
| `carteraCastigada` | — | **FIJO: `0`** |
| `valorTotalCuentaIndividual` | `CG42` del mismo EJRC: `saldoAportePatronal + saldoAportePersonal` de la entidad | calculado |
| `valorSujetoProvision` | `max(0, valorPorVencer - valorTotalCuentaIndividual)` | calculado |
| `provisionRequeridaOriginal` | Si VSAP > 0 → `VSAP × %` según calificación, sino `0` | calculado |
| `provisionConstituida` | `provisionRequeridaOriginal` del G48 del período anterior para esa operación | calculado |
| `tipoSistemaAmortizacion` | — | **FIJO: `"FR"`** |
| `cuotaCredito` | `DetallePrestamo.cuota` | dinámico |
| `dividendo` | — | **FIJO: `0`** |
| `fechaExigibilidad` | `DetallePrestamo.fechaVencimiento.toLocalDate()` | dinámico |

---

### Tabla de calificación propia (por días de morosidad)

| Días | Calificación |
|---|---|
| 0 | A1 |
| 1–8 | A2 |
| 9–15 | A3 |
| 16–30 | B1 |
| 31–45 | B2 |
| 46–70 | C1 |
| 71–90 | C2 |
| 91–120 | D |
| > 120 | E |

---

### Tabla de porcentajes de provisión por calificación

| Calificación | Porcentaje |
|---|---|
| A1 | 0.99% |
| A2 | 1.99% |
| A3 | 2.00% |
| B1 | 5.00% |
| B2 | 10.00% |
| C1 | 20.00% |
| C2 | 40.00% |
| D | 60.00% |
| E | 100.00% |

---

### Provisión Constituida — lógica del período anterior

1. Buscar EJRC del mes/año anterior con `selectByMesAnio(mesPrevio, anioPrevio)`.
2. Si existe → obtener EJRD G48 de ese EJRC → cargar todo el `CG48` en un `Map<numeroOperacion, provisionRequeridaOriginal>` (en bloque, un solo query).
3. Si **no existe** EJRC anterior → consultar `RPR.HM48` (HistoricoG48) **individualmente** por `numeroOperacion` de cada préstamo procesado (con caché en memoria para no repetir la misma consulta). Si tampoco existe en el histórico → `0`.

---

### Archivos creados/modificados

- `HistoricoG48DaoService.java` → método `selectByNumeroOperacion(String)` agregado
- `HistoricoG48DaoServiceImpl.java` → implementación del método anterior
- `HistoricoG48Service.java` → **CREADO** — interface con `selectByNumeroOperacion(String)`
- `HistoricoG48ServiceImpl.java` → **CREADO** — implementación delegada
- `SaldoOperacionG48DaoService.java` → métodos `selectByDetalle(Long)` y `selectByDetalleYOperacion(Long, String)` agregados
- `SaldoOperacionG48DaoServiceImpl.java` → implementaciones de los métodos anteriores
- `SaldoOperacionG48Service.java` → mismos 2 métodos expuestos
- `SaldoOperacionG48ServiceImpl.java` → implementaciones delegadas
- `DetallePrestamoDaoServiceImpl.java` → `JOIN FETCH d.prestamo`, `JOIN FETCH d.prestamo.entidad`, `LEFT JOIN FETCH d.prestamo.producto` en `selectCuotasDelMesGlobal` y `selectMenorCuotaAnteriorAlMesGlobal` (sin alias — JPA spec no lo permite)
- `GeneracionG48Service.java` → **CREADO** — interface
- `GeneracionG48ServiceImpl.java` → **CREADO** — implementación completa
- Orquestador → `@EJB g48Service` activo, `case "G48"` activado

---

## ✅ G49 — CancelacionG49 — COMPLETADO

> **D-038 (29-oct-2026).** Fecha de cancelación **real** dentro del mes (manual de crédito §3.5 campo 4, §5.5): 1) novación en el mes (`CRD.RLPR`, fecha del préstamo hijo) → forma `O`; 2) último evento de pago del préstamo en el mes (`CRD.EVPR`, estado 1: precancelación, pago manual, pago con aportes…); 3) último pago vigente del mes (`CRD.PGPR`); 4) última cuota marcada pagada en el mes (`DTPRFCPG`); 5) sin evento, el fin de mes. `CancelacionG49DaoServiceImpl.selectCancelacionReal`. Junio 2025: las 35 caen en junio (14 el 09-jun y 8 el 16-jun por precancelación, 1 el 10-jun por pago manual, 12 el 30-jun por pago de rol/aportes); forma `N` en las 35 (sin novaciones). Antes: 30/06/2025 en todas.

**Tabla destino:** `RPR.CG49`

**Fuente principal:** `CRD.DTPR` (DetallePrestamo) — tres grupos

---

### Grupos

**GRUPO 1:** Préstamos cuya cuota con **mayor numeroCuota** esté dentro del mes de ejecución y sea de estado **pagada (4)**. Indica que la última cuota fue pagada → cancelación normal.

**GRUPO 2:** Préstamos con `estadoPrestamo = 4` (cancelado anticipado) cuya cuota con **mayor numeroCuota pagada (estado=4)** esté dentro del mes. Excluye los que ya entraron por grupo 1.

**GRUPO 3:** Operaciones que estaban en el **G48 del período anterior** pero ya **no aparecen en el G48 del período actual**. Si no hay EJRC anterior → grupo 3 vacío.

> Se usa un `Set<numeroOperacion>` para evitar duplicados entre los tres grupos.

**Al corte (D-034, 29-oct-2026):** los grupos 1 y 2 leen el estado de HOY (`DTPRESTD`, `PRSTIDST`). Cuando el mes se
genera al corte (`ModoCorteCartera.esAlCorte(fin de mes, null)`), la G49 carga los préstamos con saldo al corte con la
misma lectura de la G48 (`CarteraAlCorteService.calcularPorPrestamo`) y **no** los marca cancelados por los grupos 1/2;
por tanto tampoco los borra de la G48. Caso: préstamo 8179 (operación 69105) en junio 2025: G49 35 (no 36) y G48 1.589.
Fuera del modo al corte (el mes que se cierra normalmente) no cambia nada.

---

### Mapeo de campos

| Campo CG49 | Fuente | Valor |
|---|---|---|
| `tipoIdentificacion` | — | **FIJO: `"C"`** |
| `identificacion` | `Prestamo.entidad.numeroIdentificacion` (grupos 1 y 2) / `CG48anterior.identificacion` (grupo 3) | dinámico |
| `numeroOperacion` | `Prestamo.idAsoprep` (como String) | dinámico |
| `fechaCancelacion` | — | **último día del mes de ejecución** |
| `formaCancelacion` | — | **FIJO: `"N"`** |

---

### Archivos creados/modificados

- `DetallePrestamoDaoService.java` → métodos `selectMaxCuotaPagadaDelMesGlobal` y `selectMaxCuotaPagadaCanceladoAnticipadoDelMesGlobal` agregados
- `DetallePrestamoDaoServiceImpl.java` → implementaciones con `JOIN FETCH d.prestamo` y `JOIN FETCH d.prestamo.entidad`
- `DetallePrestamoService.java` → mismos 2 métodos expuestos
- `DetallePrestamoServiceImpl.java` → implementaciones delegadas
- `GeneracionG49Service.java` → **CREADO** — interface
- `GeneracionG49ServiceImpl.java` → **CREADO** — implementación completa con los 3 grupos
- Orquestador → `@EJB g49Service` activo, `case "G49"` activado

---**Regla:** pendiente — el usuario la proporcionará

---

## ✅ G50 — GaranteG50 — COMPLETADO (sin registros)

**Tabla destino:** `RPR.CG50`

**Regla:** Por el momento no se generan registros. El service retorna `0` registros y el EJRD queda en estado OK. La lógica real se implementará posteriormente cuando el usuario la proporcione.

**Archivos creados:**
- `GeneracionG50Service.java` → **CREADO** — interface
- `GeneracionG50ServiceImpl.java` → **CREADO** — retorna 0, OK
- Orquestador → `@EJB g50Service` activo, `case "G50"` activado

---

## ✅ G51 — GarantiaRealG51 — COMPLETADO

**Tabla destino:** `RPR.CG51`

**Fuente:** `RPR.CG46` (NuevoPrestamoG46) del mismo EJRC — misma cantidad de registros que G46.

**Regla:** Por cada registro del G46 del período actual → insertar un registro en G51. Si G46 está vacío → G51 vacío, OK.

**Mapeo de campos:**

| Campo CG51 | Fuente | Valor |
|---|---|---|
| `tipoIdentificacion` | `CG46.tipoIdentificacion` | dinámico |
| `identificacion` | `CG46.identificacion` | dinámico |
| `numeroOperacion` | `CG46.numeroOperacion` | dinámico |
| `numeroGarantia` | `CG46.numeroOperacion` | igual al número de operación |
| `tipoGarantia` | — | **FIJO: `"A21"`** (TODO: cambiar a otro código para productos cuyo nombre empiece con H) |
| `descripcionGarantia` | — | **FIJO: `"Cuenta Individual"`** |
| `valorAvaluo` | — | **FIJO: `0`** |
| `fechaAvaluo` | — | **FIJO: `null`** |
| `numeroRegistroGarantia` | — | **FIJO: `null`** |
| `fechaContabilizacion` | — | **FIJO: `null`** |
| `porcentajeCubre` | — | **FIJO: `100`** |
| `estadoRegistro` | — | **FIJO: `"N"`** |

**Archivos creados/modificados:**
- `NuevoPrestamoG46DaoService.java` → método `selectByDetalle(Long)` agregado
- `NuevoPrestamoG46DaoServiceImpl.java` → implementación del método anterior
- `NuevoPrestamoG46Service.java` → método `selectByDetalle(Long)` expuesto
- `NuevoPrestamoG46ServiceImpl.java` → implementación delegada
- `GeneracionG51Service.java` → **CREADO** — interface
- `GeneracionG51ServiceImpl.java` → **CREADO** — implementación completa
- Orquestador → `@EJB g51Service` activo, `case "G51"` activado

---

## Patrón estándar para implementar un G nuevo

Cada G nuevo sigue este patrón:

### 1. Crear interface
```
src/main/java/com/saa/ejb/rpr/service/GeneracionGxxService.java
→ método: long generar(DetalleEjecucionReporte detalle) throws Throwable;
```

### 2. Crear implementación
```
src/main/java/com/saa/ejb/rpr/serviceImpl/GeneracionGxxServiceImpl.java
→ @EJB de los services que necesite
→ lógica de consulta + mapeo + persistencia + actualización de estados fuente
```

### 3. Conectar en el orquestador
```
GeneracionReportesServiceImpl.java
→ Agregar: @EJB private GeneracionGxxService gxxService;
→ Descomentar: case "Gxx": return gxxService.generar(ejrd);
```

### 4. Actualizar este documento con las reglas del G implementado

---

## ⚠ REGLA OBLIGATORIA: contar `CRD.APRT` bajo el modelo de devengo — FILAS ≠ MESES

**Aplica a cualquier G, nuevo o existente, que cuente "número de aportes", "imposiciones
acumuladas/personales/patronales", "meses aportados" o cualquier variante de esa idea sobre
`CRD.APRT` / la entidad `Aporte`.**

Desde el plan de devengo de aportes (`docs/logica-negocio/crd/PLAN-APORTES-DEVENGO-CONTRATOS.md`),
**una fila de `CRD.APRT` ya no equivale a un mes aportado**:

- Un partícipe puede tener **varias filas del mismo mes**: un pago parcial completado
  después, un anticipo, un ajuste manual.
- Una **sola fila** puede cubrir un mes de devengo **distinto** al de su fecha de caja
  (`APRTFCTR`): un partícipe que se pone al día pagando 6 meses atrasados en una sola carga
  genera varias filas con la misma fecha de caja pero devengo repartido en 6 meses
  distintos; un anticipo genera una fila cuyo devengo es un mes *futuro* respecto de su
  fecha de caja.

**Consecuencia práctica:** `COUNT(*)` (o `COUNT(a)`, `COUNT(a.codigo)`, `.size()` de una
lista de `Aporte`) sobre filas de `CRD.APRT` **no es lo mismo** que "cuántos meses aportó el
partícipe". Si la cifra que se está calculando significa **meses aportados**, hay que contar
`COUNT(DISTINCT` periodo efectivo`)`, usando la expresión ya centralizada en
`com.saa.ejb.crd.daoImpl.PeriodoEfectivoAporteSql.PERIODO_EFECTIVO_SQL`:

```sql
(CASE WHEN APRTPRDV IS NOT NULL THEN APRTPRDV
      WHEN APRTVLRR > 0         THEN TRUNC(APRTFCTR, 'MM')
      ELSE NULL END)
```

**No la redefinas.** Si la necesitas fuera de `com.saa.ejb.crd.daoImpl`, extráela a un sitio
compartido y haz que el original la use — nunca dupliques la expresión con otra lógica.

**Si la cifra significa cuántos MOVIMIENTOS hubo** (filas, no meses — por ejemplo un
dashboard de actividad, o un detalle de transacciones), `COUNT(*)` sigue siendo correcto:
**pero decilo explícitamente en el JavaDoc del método**, para que el siguiente que lo lea no
tenga que adivinar cuál de los dos significados es. No dejes un `COUNT` ambiguo.

**Métodos ya corregidos con este criterio** (2026-08-27): `AporteDaoServiceImpl.selectCountImposicionesJubilacionPorEntidad`
(G44), `selectCountImposicionesPersonalesPorEntidad` y `selectCountImposicionesPatronalesPorEntidad`
(G43), `EntidadDaoServiceImpl.selectPadronParticipes` (padrón), `AporteDaoServiceImpl.sumaAportesPositivosPorTipoYPeriodo`
(mora del partícipe en la carga). Comparación antes/después de los tres primeros:
`docs/logica-negocio/crd/sql/68_COMPARACION_REPORTES_G_IMPOSICIONES.sql`.

**Casos ya revisados y correctos tal cual** (NO tocar): `AporteDaoServiceImpl.selectKpisGlobales`,
`selectResumenPorTipo`, `selectTopEntidades` cuentan **movimientos**, están etiquetados como
tales (`AS movimientos`) y así deben quedarse. `RPRT_ESCT_APRT.jrxml` (reporte "estado de
cuenta de aportes") trae en su SQL nativo un campo `NUMERO_MOVIMIENTOS` con `COUNT(A.APRTCDGO)`
— está bien etiquetado (cuenta movimientos, no meses) y además **el campo no se imprime en
ningún lado del layout del reporte**: no se modificó porque cualquier cambio ahí obliga a
recompilar el `.jasper` a mano en Jaspersoft Studio y no lo amerita.

**Código muerto retirado por este mismo motivo** (2026-08-27): `AporteDaoService.selectCountByEntidad`
(`COUNT(a)` genérico sobre `Aporte`, sin usar `PERIODO_EFECTIVO_SQL`) y su único llamador,
`CargaArchivoServiceImpl.melyTest` / `GET /crd/cargaArchivo/melyTest/{idEntidad}` — código de
depuración expuesto como endpoint público sin autenticación real, cuyo nombre ("cantidad de
aportes por entidad") invitaba a reusarlo como "meses aportados". Se eliminaron ambos en vez
de corregirlos: no tenían ningún consumidor real.

---

## Corrección aplicada al orquestador

### Bug: re-ejecución no procesaba ningún G
**Causa:** `DetalleEjecucionReporteServiceImpl.selectPendientesYNovedadesByEjecucion` lanzaba `IncomeException` cuando la lista estaba vacía (patrón genérico). En el orquestador esa llamada estaba en `try-catch` que silenciaba la excepción y dejaba `ejrdsAProcesar` vacío → el bucle no procesaba nada en re-ejecuciones.

**Archivos corregidos:**
- `DetalleEjecucionReporteServiceImpl.java` → ya **no lanza excepción** cuando no hay pendientes; retorna lista vacía (resultado válido: todos OK).
- `GeneracionReportesServiceImpl.java` → eliminados los `try-catch` silenciosos en:
  - Paso 3: obtener EJRDs pendientes en re-ejecución
  - Paso 7: evaluación final del estado del EJRC

---

## Instrucciones para retomar en nueva conversación

1. Leer: `REGLAS_GENERACION_REPORTES_G.md` (este archivo)
2. Leer: `PLAN_GENERACION_REPORTES_G.md` (estado técnico detallado)
3. **Toda la implementación G40–G51 está completa y funcional**
4. Pendientes menores conocidos:
   - **G50** — sin registros por ahora (retorna 0, OK). Lógica real a definir.
   - **G51** — `tipoGarantia = "A21"` fijo. TODO: préstamos con producto cuyo nombre empiece con H → cambiar a otro código.

---

## NOTA FINAL
> ⚠️ Una vez que no se necesite más referencia, **eliminar los siguientes archivos**:
> - `REGLAS_GENERACION_REPORTES_G.md` (este archivo)
> - `PLAN_GENERACION_REPORTES_G.md`

