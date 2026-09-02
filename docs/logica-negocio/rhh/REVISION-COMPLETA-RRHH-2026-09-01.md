# RRHH — revisión completa contra el esquema de trabajo actual

**Pedido por el usuario el 2026-09-01** · Equipo `omen-saa-2` · **Todo es lectura de código; nada se
modificó.**

Tres preguntas: ¿toda salida de dinero sale por tesorería? ¿la contabilidad se alimenta de todos los
procesos? ¿el módulo cubre lo que piden las entidades de control?

> **Cómo leerlo.** Lo que dice ✅ está verificado archivo:línea. Lo que dice 🟡 **no se verificó** —
> es distinto de «falta», y está separado a propósito para que este listado no se infle con cosas
> que están y no se vieron.

---

## 1. Resumen — qué falta, en orden de costo

| # | Hueco | Por qué duele | Tamaño |
|---|---|---|---|
| **1** | **Liquidación / finiquito no tiene pago** | Se reconoce la deuda con el empleado que sale y **el pago ocurre fuera del sistema**. La cuenta «Liquidaciones por pagar» sólo se acredita: **nadie la debita jamás** | BE M + FE M |
| **2** | **Utilidades: ni pago, ni asiento, ni provisión** | Se calculan y ahí mueren. Es el hueco más limpio del módulo | BE L + FE M |
| **3** | **Reporte del SUT para décimos** | Obligación legal con multa de hasta 20 SBU. **Ya desbloqueado**: ver `PLAN-REPORTE-MDT-SUT.md` | BE S |
| **4** | **Tres reportes construidos y no alcanzables** | Acta de finiquito y formulario 107 individual **existen compilados** y no hay forma de ejecutarlos desde la aplicación | FE S |
| **5** | **Cinco pantallas borran datos al editar** | Un permiso aprobado vuelve a «solicitado»; aprobar vacaciones no graba la fecha | BE S + FE M |

**Y un defecto en lo entregado hoy, que bloquea el despliegue del frente 1:** `RHH.PVNM` no es el
saldo contable de la provisión — ver §4bis de `PLAN-PAGO-BENEFICIOS-Y-SALIDA-POR-TESORERIA.md`.
Corrección ya despachada.

---

## 2. ¿Toda salida de dinero sale por tesorería?

| Proceso | Pago | Bandeja TSR | Evidencia |
|---|---|---|---|
| Nómina | ✅ | ✅ `RHH_NOMINA` | entregado hoy, `bb9bccb` |
| Anticipo de empleado | ✅ | ✅ `RHH_ANTICIPO_EMPLEADO` | preexistente |
| Décimos y fondos de reserva acumulados | ✅ | ✅ `RHH_BENEFICIO_SOCIAL` | entregado hoy, `a820203` |
| Vacaciones pagadas en dinero | ✅ | ✅ vía rol | `SolicitudVacacionesServiceImpl:274` crea una novedad que entra al rol. **No es circuito aparte** |
| **Liquidación / finiquito** | ❌ | ❌ | `Liquidacion` tiene 23 columnas y **ninguna** de pago, cuenta ni egreso. `LiquidacionRest` tiene simular/calcular/aprobar/ejecutarSalida/contabilizar y **cero endpoint de pago** |
| **Utilidades** | ❌ | ❌ | `UtilidadServiceImpl` es CRUD puro. `DTUTVLPG` (valor pagado) existe y **nadie la escribe** |
| Horas extra | 🟡 | 🟡 | Probablemente vía rol, **no verificado** |
| Reverso de cuotas de anticipo | 🟡 | 🟡 | **No se revisó** |

---

## 3. ¿La contabilidad se alimenta de todos los procesos?

| Proceso | Asiento | Evidencia |
|---|---|---|
| Rol de nómina | ✅ | `contabilizarRol` |
| Provisiones (alta) | ✅ | `contabilizarProvisiones` |
| Pago de nómina | ✅ | `contabilizarPago`, plantilla `CFNMPLPG` |
| Liquidación (gasto y pasivo) | ✅ | `contabilizarLiquidacion` |
| Baja de provisión de décimos | ✅ | entregado hoy |
| Baja de jubilación patronal y desahucio | ✅ | entregado hoy, `54c8cdf` — antes **duplicaba el gasto** |
| Aportes al IESS | ✅ | `:786-795` y `:845-846`, desde los renglones del rol. ⚠️ **NO desde `RHH.PRTE`** — esa tabla es dato para la planilla, no alimenta la contabilidad |
| Retención en la fuente | ✅ | `:848-849` |
| **Cancelación del pasivo de liquidación** | ❌ | `LIQUIDACIONES_POR_PAGAR` (línea 70) aparece **dos veces en todo el proyecto**, las dos en el método que la crea |
| **Utilidades — pago** | ❌ | cero menciones de «utilidad» en `ContabilizacionNominaServiceImpl` |
| **Utilidades — provisión** | ❌ | `RhhTipoProvision` tiene 7 tipos y **utilidades no está** |

⚠️ **Períodos históricos:** escriben provisiones en `RHH.PVNM` **sin generar asiento**, a propósito
(es lo que permitió cargar enero-julio sin plan de cuentas). Cualquier reporte que sume `PVNM`
esperando cuadrar contra el mayor **va a descuadrar** si no los excluye.

---

## 4. ¿Cubre lo que piden las entidades de control?

**El módulo está mucho mejor de lo esperado con IESS y SRI** — los dos organismos con mayor
exigencia técnica ya tienen generadores reales de archivo, no sólo cálculo interno.

### IESS

| Obligación | Estado | Evidencia |
|---|---|---|
| Novedades (11 tipos: ingresos, salidas, modificación de sueldo, fondos de reserva…) | ✅ | `ExportacionNovedadesIessServiceImpl.generarArchivo:115` — archivo batch del anexo oficial. **Se niega a generar si falta un dato**, en vez de emitir algo inválido |
| Fondos de reserva reportados | ✅ | mismo generador, tipo 4 |
| Planilla de control | ✅ como control interno | Su Javadoc lo aclara: no es documento oficial. Nació de un caso real de marzo 2026, $208,22 de más |
| Declaración de aportes mensuales | 🟡 | No hay generador. **Probablemente no aplica** — se declara en línea contra la nómina del portal. Sin confirmar |

### SRI

| Obligación | Estado | Evidencia |
|---|---|---|
| Retención en la fuente | ✅ | `RetencionRentaServiceImpl` + contabilización |
| **Formulario 107 / RDEP** | ✅ | `GeneracionSalidasOficialesServiceImpl.generarRdep:72` — XML real con hash SHA-256 para saber si lo presentado sigue vigente. Filtra por acumulado del ejercicio, no por empleado activo: **corrige un defecto real de marzo 2026 donde 2 de 22 quedaban fuera** |
| Proyección de gastos personales | ✅ backend | `RetencionRentaServiceImpl:149,311`. 🟡 No se verificó si hay autoservicio del empleado |
| Tabla de impuesto a la renta | 🟠 carga manual | CRUD puro, sin aviso de actualización anual. Esperable: el SRI la publica una vez al año |

### Ministerio de Trabajo — **el más débil de los tres**

| Obligación | Estado | Evidencia |
|---|---|---|
| **Décimos — registro en el SUT** | ❌ | Ver `PLAN-REPORTE-MDT-SUT.md`. Ya desbloqueado |
| **Acta de finiquito** | 🟠 **existe y no es alcanzable** | ver §5 |
| Registro de contratos al SUT | 🟡 | **No investigado a fondo** |
| Declaración de salarios al SUT | 🟡 | **No investigado a fondo** |

---

## 5. 🟠 Tres reportes construidos, compilados, y sin puerta de entrada

**Corrige un falso faltante.** El primer análisis reportó el acta de finiquito como inexistente,
buscándola en `LiquidacionHaberesServiceImpl` y en `LiquidacionRest`. **Está**, pero los reportes no
viven en el módulo: van por el servicio transversal de Jasper (`POST /rest/rprt/generar`). Buscar el
generador dentro del módulo daba un negativo falso.

`src/main/resources/rep/rhh/` tiene **7 reportes, todos con su `.jasper` compilado**:

| Reporte | ¿Lo ofrece la pantalla? |
|---|---|
| `RPRT_ROLL_CNSL` rol consolidado | ✅ |
| `RPRT_PRVS_PRDO` provisiones | ✅ |
| `RPRT_APRT_RSMN` resumen de aportes | ✅ |
| `RPRT_ROLL_INDV` rol individual | 🟠 declarado en el frontend, **no ofrecido** en la pantalla |
| **`RPRT_ACTA_FNQT` acta de finiquito** | ❌ **ninguna referencia en todo el frontend** |
| **`RPRT_F107_INDV` formulario 107 individual** | ❌ **ninguna referencia** |
| **`RPRT_IESS_CNTR` control IESS** | ❌ **ninguna referencia** |

**Es el hueco más barato de cerrar de todo el listado**: el trabajo está hecho y compilado; falta
conectarlo. Y el acta de finiquito deja de ser un faltante del Ministerio para pasar a ser un botón.

> **Tercera aparición del mismo patrón en el día**, y conviene tratarlo como criterio: *los endpoints
> `generarDecimo*` sin pantalla · estos tres reportes sin pantalla · las columnas `LQBSVLPG` y
> `diasPagados` que nadie escribe.* **En este sistema hay bastante construido que no es alcanzable
> desde la aplicación.** Antes de estimar algo como nuevo, conviene buscar si ya está y sólo le
> falta la puerta.

---

## 6. Lo que NO se verificó

Para que no se lea como cobertura:

- Horas extra: si `RHH.HREX` alimenta el motor o tiene camino propio.
- Reverso de cuotas de anticipo.
- Registro de contratos y declaración de salarios al SUT.
- Autoservicio del empleado para declarar gastos personales.
- Si existe algún reporte que sume `RHH.PVNM` esperando cuadrar contra el mayor.
