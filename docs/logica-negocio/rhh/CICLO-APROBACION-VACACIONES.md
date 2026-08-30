# Ciclo de aprobación de solicitudes de vacaciones

**Estado (revisado 2026-08-28):** T1, T2 y T4 **cerrados**; el frontend consume los endpoints
reales, no mocks. **T3 (provisión) sigue deliberadamente SIN implementar** — no es un olvido.

| Tarea | Estado |
|---|---|
| T1 — `aprobar` | ✅ Implementada 2026-08-27, con `RHH.DVAC` enganchada |
| T2 — `rechazar` / `anularAprobacion` | ✅ Implementadas 2026-08-27 |
| T3 — Provisión de vacaciones | ⛔ **Análisis cerrado, implementación diferida a propósito** — ver abajo |
| T4 — Endpoints REST | ✅ Implementados. Frontend verificado el 2026-08-28: `permiso-licencia.service.ts` y `solicitud-vacaciones.service.ts` llaman a `ServiciosRhh.RS_*` reales; los `of([])` que aparecen son *fallback* de `catchError` en error 400, no datos simulados |

**T3 — decisión de diseño tomada (opción 3) y verificación hecha:** las dos cuentas involucradas
son familias distintas (gasto vs. pasivo), así que el pago hoy **sí** duplica el gasto. Corregirlo
es fuera del alcance de este ciclo: toca **el motor de nómina** (`ContabilizacionNominaServiceImpl`,
congelado), no `SolicitudVacacionesServiceImpl`. **Queda pendiente de que el usuario decida cuándo
abordarlo** — requiere además reparametrizar `RHH.CPNM.CPNMROLM=32` para el concepto 12. Ver
"Decisión sobre la provisión" más abajo.

## El problema que esto resuelve

Hasta el 2026-08-27, `SolicitudVacacionesServiceImpl` era CRUD puro (`save`, `remove`,
`selectAll`, `selectById`, `selectByCriteria`, `saveSingle`). Aprobar una solicitud desde
el frontend solo cambiaba `SLCTESTD` a `'APROBADA'`. No pasaba nada más: el saldo de
`RHH.SLDV` no se tocaba y no se creaba ninguna fila en `RHH.NVNM`. Probado en vivo:
una solicitud de 3 días aprobada dejó el saldo 2025 intacto (`asignados=7.71,
usados=0, pendientes=7.71`) y cero novedades para el empleado. Consecuencia real: el
mismo empleado podía pedir sus días disponibles cuantas veces quisiera y el sistema
los aprobaba todos, y el rol de pagos nunca se enteraba de que la persona tomó
vacaciones.

## T1 — `aprobar(idSolicitud, idUsuario, observacion)`

Implementado en `SolicitudVacacionesServiceImpl.aprobar`. Todo en una sola
transacción (`@TransactionAttribute(REQUIRED)`, y los EJB que invoca —
`AcreditacionVacacionesService.consumir`, `NovedadNominaDaoService.save`— también son
`REQUIRED`, así que se unen a la misma transacción del contenedor).

Pasos, en orden:

1. **Guardas de estado.** Solo se aprueba una solicitud cuyo `SLCTESTD` no sea
   `'APROBADA'` ni `'ANULADA'` (comparación case-insensitive). También valida que
   `fechaHasta >= fechaDesde`.
2. **Recalcula los días**, no confía en `SLCTDIAS` grabado al solicitar:
   `ChronoUnit.DAYS.between(fechaDesde, fechaHasta) + 1` (rango inclusivo, días
   calendario — no excluye fines de semana ni feriados porque nada en el saldo
   existente los excluye tampoco; si la regla de negocio real es otra, avisar).
3. **Valida el disponible real** con
   `AcreditacionVacacionesService.diasDisponibles(idEmpleado)`. Si no alcanza, lanza
   `IncomeException` con los dos números: cuántos días hay y cuántos se piden.
4. **Resuelve el período** de la fecha de inicio con el nuevo
   `PeriodoNominaDaoService.selectByFechaEmpresa(idEmpresa, fecha)` (JPQL
   `fechaInicio <= fecha <= fechaFin` de la empresa del empleado). Si no existe
   período, o existe pero su estado no es `ABIERTO` (`RhhEstadoPeriodoNomina.ABIERTO
   = 1`), rechaza con un mensaje que dice cuál es el estado — **no mueve la novedad
   a otro período por su cuenta**, tal como se pidió.
5. **Resuelve el concepto** "Vacaciones pagadas" vía
   `ConceptoNominaDaoService.selectByCodigoAlterno(12L, idEmpresa)` — ver la nota de
   "12/33/39" más abajo sobre por qué es por código alterno y no por rol del motor.
6. **Consume el saldo FIFO**, inline en `aprobar` (corregido el 2026-08-27 — antes
   llamaba a `AcreditacionVacacionesService.consumir`, que hace el mismo recorrido
   pero no expone qué años tocó). Recorre `SaldoVacacionesDaoService.selectDisponibles`
   (no caducados, `diasPendientes > 0`, orden `anio, codigo` ascendente) subiendo
   `diasUsados` y bajando `diasPendientes` hasta cubrir los días, pudiendo tomar de
   varios años, y **graba una fila en `RHH.DVAC` por cada `SaldoVacaciones` que toca**
   (año, días exactos, `DVACESTD=1`) — es lo que permite que `anularAprobacion`
   devuelva los días exactamente a esos años. Si el saldo cambia entre la validación
   del paso 3 y este bucle (condición de carrera: otra aprobación concurrente del
   mismo empleado), lanza `IncomeException` y aborta toda la transacción en vez de
   dejar la solicitud a medio aprobar.
7. **Crea la novedad** en `RHH.NVNM`: período resuelto en el paso 4, concepto del
   paso 5, `cantidad` = días aprobados, `valor` = días × `valorDiaVacaciones` (mismo
   método que ya usa `GET /sldv/valorDia/{idEmpleado}/{fechaCorte}`), `aprobada='S'`,
   `estado=1` — **son las dos condiciones exactas que exige**
   `NovedadNominaDaoServiceImpl.selectAprobadas` (`Paso 5.c` del motor); sin las dos,
   la novedad es invisible para `calcularPeriodo` aunque exista en la tabla, que es
   el mismo tipo de bug silencioso que se estaba arreglando. La `descripcion` se
   graba como `"Solicitud de vacaciones #{codigo}"` — es el enlace que usa
   `anularAprobacion` para encontrar esta novedad después (no hay FK de SLCT a NVNM).
8. **Marca la solicitud**: `SLCTESTD='APROBADA'`, `SLCTAPRB`=nombre del usuario
   (resuelto de `idUsuario` vía `Usuario.getNombre()`, mismo criterio que
   `ConciliacionCierreServiceImpl.usuarioNombre`), `SLCTFHAP`=hoy, `SLCTDIAS`
   actualizado al valor recalculado.

## T2 — `rechazar` y `anularAprobacion`

### `rechazar(idSolicitud, idUsuario, motivo)`

Transición de estado pura: no toca saldo ni novedad porque una solicitud rechazada
nunca llegó a consumir nada (el consumo solo pasa en `aprobar`). Misma guarda de
estado que `aprobar` (no aprobada ni anulada). Marca `SLCTESTD='RECHAZADA'`.

### `anularAprobacion(idSolicitud, motivo, idUsuario)`

Solo desde `SLCTESTD='APROBADA'`. Motivo obligatorio. Pasos:

1. Ubica la novedad creada en `aprobar` por
   `NovedadNominaDaoService.selectPorDescripcion(idEmpleado, idConceptoVacacionesPagadas,
   "Solicitud de vacaciones #{codigo}")` — método nuevo, exact match.
2. Si la novedad no aparece, rechaza (no debería pasar salvo manipulación directa de
   la BD).
3. **Si el período de esa novedad está en estado `PAGADO` o posterior**
   (`RhhEstadoPeriodoNomina.PAGADO = 6`, o `CERRADO = 7`), rechaza con mensaje claro:
   "la novedad ya entró en un rol pagado". No bloquea en `CONTABILIZADO` (5) porque
   ese estado no implica que ya se pagó, solo que se generó el asiento.
4. Devuelve el saldo y anula la novedad (`aprobada='N'`, `estado=0`,
   `descripcion` con el sufijo `" | ANULADA: {motivo}"`).
5. Marca la solicitud `SLCTESTD='ANULADA'`.

### La devolución del saldo (corregido el 2026-08-27: ahora es exacta)

**`RHH.DVAC` ya está enganchada** (DDL de `docs/logica-negocio/rhh/sql/03-detalle-consumo-vacaciones.sql`
ejecutado en local, modelo sin cambios de fondo salvo un índice único `(SLCTCDGO, SLDVCDGO)`:
una solicitud no puede tomar dos veces del mismo año — si necesita más días de ese año, es la
misma fila con más días, no una fila nueva).

`anularAprobacion` lee `DetalleConsumoVacacionesDaoService.selectVigentesPorSolicitud(idSolicitud)`.
Si hay filas (`DVACESTD=1`): por cada una, devuelve exactamente esos días al `SaldoVacaciones`
exacto del que salieron (`diasPendientes += dias`, `diasUsados -= dias`) y marca la fila
`DVACESTD=0` — sin importar qué se haya consumido después, sin la heurística de antes.

**Si no hay filas** (la solicitud se aprobó antes de que existiera `RHH.DVAC`), cae al
respaldo: `AcreditacionVacacionesService.revertirConsumo`, que reversa en orden **inverso** al
de consumo — del año más reciente al más antiguo — sobre el estado *actual* de `RHH.SLDV`, sin
saber qué años consumió esta solicitud en particular. **Es a propósito, no hay backfill**: el
dato de qué año se consumió solo existía en el instante de aprobar, y no hay de dónde
reconstruirlo para lo aprobado antes de esta tabla sin inventarlo. El riesgo que motivó crear
`DVAC` sigue documentado para ese caso residual: si la solicitud A (pre-`DVAC`) consumió de
2023+2024, y después se aprobó B (con `DVAC`) que consumió de 2024+2025, anular A con
`revertirConsumo` puede devolver días a 2025 en vez de a 2023/2024 — el total de días cuadra,
el año no. De las solicitudes aprobadas desde el 2026-08-27 en adelante, todas son exactas.

## T3 — La provisión de vacaciones (análisis, sin implementar)

Preguntas del usuario y respuesta, verificada contra el código:

**¿Se calcula al vuelo o se acumula en una tabla?** Se **acumula**, pero no como un
saldo corriente: `ProcesoNominaServiceImpl.calcularPeriodo`, en el "Paso 10b" (línea
~1003), genera una fila **nueva en `RHH.PVNM` (`ProvisionNomina`) cada período**, para
cada empleado, con `tipoProvision = RhhTipoProvision.VACACIONES (3)`:

```java
Double provisionVacaciones = RedondeoNomina.redondea(
    baseVac * prnm.getDiasVacaciones() / prnm.getDiasAnio());
generaProvision(periodo, empleado,
    conceptoPorRol(conceptos, RhhRolConceptoMotor.PROVISION_VACACIONES),
    RhhTipoProvision.VACACIONES, baseVac, provisionVacaciones, usuario, persistir);
```

Es un **flujo mensual**, no un saldo: cada fila de `PVNM` es "lo que se provisionó
ESTE período", calculado como `baseVacaciones × PRNMDIVC / PRNMDANO` (con la
parametría 2026 eso es la base entre 24). El comentario del código dice
explícitamente que las vacaciones **no admiten mensualización** — a diferencia de
décimos y fondos de reserva — así que esta provisión se genera siempre, sin depender
de ninguna modalidad de contrato, y es la única sin renglón equivalente en el rol de
pago (el trabajador nunca "recibe" esta provisión como ingreso mensual: la recibe
como el pago puntual de vacaciones que ahora genera este mismo cambio, vía
`NovedadNomina`).

**¿`ProvisionActuarialServiceImpl` es el dueño?** **No.** Su propio Javadoc lo dice
sin ambigüedad: `cargarProvisionActuarial`/`cargarEstudioActuarial` **solo** aceptan
`JUBILACION_PATRONAL (6)` y `DESAHUCIO (7)` — provisiones que dependen de un estudio
actuarial externo con tablas de mortalidad y rotación, que entran como un dato, no
como una fórmula. El propio código lo rechaza explícitamente para cualquier otro
tipo:

```java
throw new IncomeException("El tipo de provision " + tipoProvision + " no es actuarial."
    + " Solo la jubilacion patronal y el desahucio se cargan desde un estudio externo;"
    + " las demas las genera calcularPeriodo a partir de las bases del periodo.");
```

La provisión de vacaciones (tipo 3) la genera **el motor de nómina**
(`ProcesoNominaServiceImpl`), no este servicio.

**¿Bajarla es una novedad negativa, un asiento, o se recalcula sola?** Ninguna de las
tres existe hoy. `PVNM` no tiene ninguna noción de "saldo pendiente de provisión que
se reduce cuando se paga" — cada fila es un hecho histórico del período en que se
generó, igual que un renglón de rol. No hay ningún código que reste de `PVNM` cuando
se crea una `NovedadNomina` de vacaciones pagadas (ni antes de este cambio, ni con
`aprobar` nuevo). Verificado con `grep`: nada en el código escribe en `PVNM` salvo
`ProcesoNominaServiceImpl.generaProvision` (motor) y
`ProvisionActuarialServiceImpl.cargarProvisionActuarial` (actuariales).

Tres formas honestas de resolverlo, ninguna implementada:

1. **Novedad negativa de concepto 33** en el período de la aprobación, para que el
   asiento de provisiones del período la compense — simétrico a cómo ya se maneja el
   pago vía concepto 12, pero exige decidir si el motor debe leer novedades negativas
   de un concepto de provisión (hoy `generaProvision` no consulta `NovedadNomina` en
   absoluto, es un camino nuevo).
2. **Recalcular la base de provisión** hacia adelante: como cada período genera su
   propia fila `PVNM` a partir de `baseVac` (que sale de qué conceptos tienen
   `CPNMBSVC='S'` en el período), si tomar vacaciones reduce esa base de algún modo,
   la provisión del período siguiente bajaría sola sin tocar nada retroactivo — pero
   hoy `baseVac` no parece derivarse del saldo de vacaciones pendiente, sino de
   conceptos remunerativos del propio período (no confirmado a fondo: requeriría leer
   de dónde sale `baseVac` en `calcularPeriodo`, fuera del alcance de este análisis).
3. **No tocar `PVNM` y ajustar solo en el asiento de provisiones**, dejando que la
   provisión acumulada (suma de `PVNM` histórico) y lo efectivamente pagado (suma de
   novedades de concepto 12) se reconcilien en el asiento contable en vez de en la
   tabla de origen — más simple de implementar, pero desplaza la reconciliación a
   contabilidad en vez de resolverla en el dato.

Esto es contabilidad de fondo (Art. 71 CT + NIC 19 de beneficios a empleados) y la
decisión de cuál de las tres — o ninguna — se implementa queda para el usuario, con
este análisis delante.

## Decisión sobre la provisión (2026-08-27) — opción 3, y una reparametrización pendiente

**Decisión tomada: opción 3**, y no como compromiso. El planteo original ("bajar la
provisión cuando alguien toma vacaciones") partía de un supuesto equivocado: una
provisión de este tipo no es un saldo que se "baja", es una cuenta de pasivo que se
**acredita** mensualmente (la provisión) y se **debita** al pagar (la novedad) — el
saldo de esa cuenta de pasivo *es* la provisión pendiente, sin que `PVNM` necesite
ninguna noción nueva de saldo. La opción 1 (novedad negativa de concepto 33) habría
distorsionado el gasto del mes en que se paga, y la 2 dependía de una base
(`baseVac`) que no se confirmó que se derive del saldo pendiente.

Para que la opción 3 funcione, una sola condición tiene que cumplirse: **el pago de
vacaciones (concepto 12) tiene que debitar la misma cuenta de pasivo que acredita la
provisión mensual (concepto 33)**, no una cuenta de gasto — si va a gasto, el gasto
se cuenta dos veces (una vez al provisionar, otra vez al pagar) y el pasivo nunca baja.

### Verificación: las dos cuentas, y no son la misma familia

Rastreado hasta la plantilla contable real (`RHH.CFNM` de la empresa 1236 → `CNT.PLNS`
→ `CNT.DTPL` → `CNT.PLNN`), verificado en la base local:

| Concepto | Vía | `RhhLineaAsiento` (rubro 214) | Plantilla | Cuenta |
|---|---|---|---|---|
| 12 Vacaciones pagadas | `ReglonNomina` (tipo INGRESO, sin `rolMotor`) → `acumulaRenglon` → `lineaDeIngreso(null)` | `GASTO_SUELDOS_Y_SALARIOS` (1), el genérico de "cualquier ingreso sin línea propia" | ROL DE PAGOS (`PLNSCDAL=163`) | **`4.3.01.05 REMUNERACIONES O SALARIO`** — gasto |
| 33 Provisión vacaciones | `ProvisionNomina.tipoProvision=VACACIONES` → `importesDeProvisiones` → `lineaPorPagarProvision` | `PROVISION_VACACIONES_POR_PAGAR` (42) | PROVISIONES (`PLNSCDAL=164`) | **`2.5.14 VACACIONES POR PAGAR`** — pasivo |

**No son la misma familia — 4.x (gasto) contra 2.x (pasivo).** El pago de vacaciones
hoy **sí** duplica el gasto, exactamente como se sospechaba: al aprobar, la
`NovedadNomina` de concepto 12 se convierte en un `ReglonNomina` normal del rol, y
como `CPNMROLM` de concepto 12 está en blanco (nunca tuvo rol del motor: nadie lo
generaba automáticamente antes de este ciclo), `lineaDeIngreso` no tiene ninguna
regla especial para él y cae al `return` genérico de la función — el mismo que usa
el sueldo base, los bonos y cualquier ingreso sin clasificar.

### Por qué no es reparametrización pura

La expectativa era que esto se resolviera solo tocando `RHH.CFNM`/`CNT.DTPL` (datos).
No es así: `ConceptoNomina.planCuenta`/`detallePlantilla` (los campos que en teoría
permitirían apuntar un concepto directo a una cuenta) **no los lee ningún código de
contabilización** — verificado con `grep`, la única fuente real de la cuenta es la
cadena `rolMotor → RhhLineaAsiento → DTPL`. Como concepto 12 no tiene `rolMotor`, no
hay ninguna fila de `CNT.DTPL` que se pueda reapuntar para arreglarlo: la línea 1
(`GASTO_SUELDOS_Y_SALARIOS`) es compartida con el sueldo base y no se puede
redirigir sin romper eso.

**Es reparametrización + una línea de código, no una implementación nueva** — el mismo
patrón que ya usan fondos de reserva, décimos y horas extra en `lineaDeIngreso`:

1. Nuevo `RhhRolConceptoMotor.VACACIONES_PAGADAS` (siguiente libre: 32).
2. `UPDATE RHH.CPNM SET CPNMROLM = 32 WHERE CPNMCDGO = 12` (empresa 1236) — dato.
3. Una rama en `lineaDeIngreso`: `if (esRol(rol, RhhRolConceptoMotor.VACACIONES_PAGADAS))
   return RhhLineaAsiento.PROVISION_VACACIONES_POR_PAGAR;` — código, en
   `ContabilizacionNominaServiceImpl`, que es el motor de nómina.

**No implementado.** Toca `ContabilizacionNominaServiceImpl`, fuera del alcance de
este ciclo (`SolicitudVacacionesServiceImpl`) y de la restricción "no tocar el motor
de nómina" con la que se vino trabajando en todo este documento. Queda para que el
usuario decida cuándo abordarlo — con las dos cuentas y el mecanismo exacto ya
verificados, no hace falta researchear de nuevo.

## T4 — Endpoints REST

Base `@Path("slct")`, ya existente.

```
POST /rest/slct/aprobar/{id}
  body: { "idUsuario": <Long>, "observacion": "<String opcional>" }
  200: SolicitudVacaciones actualizada
  400: IncomeException (estado inválido, saldo insuficiente con los dos números,
       período inexistente o no abierto, concepto no encontrado)

POST /rest/slct/rechazar/{id}
  body: { "idUsuario": <Long>, "motivo": "<String opcional>" }
  200: SolicitudVacaciones actualizada
  400: IncomeException (estado inválido)

POST /rest/slct/anularAprobacion/{id}
  body: { "idUsuario": <Long>, "motivo": "<String obligatorio>" }
  200: SolicitudVacaciones actualizada
  400: IncomeException (estado inválido, motivo faltante, novedad no encontrada,
       novedad ya en rol pagado)
```

Mismo patrón de body (`Map<String,Object>` con `idUsuario`/`motivo` opcional) que
`ChequeRest.anular`, `ChequeraRest.anular`, `CierreCajaChicaRest.anular`.

## Nota sobre "concepto 12 / 33 / 39"

Verificado contra la base local (copia de producción):

| Lo que el usuario llamó | `CPNMCDGO` (PK) | `CPNMALTR` (código alterno) | `CPNMROLM` (rol motor) |
|---|---|---|---|
| 12 Vacaciones pagadas | 12 | **12** | *(vacío)* |
| 33 Provisión vacaciones | 33 | 52 | 19 (`PROVISION_VACACIONES`) |
| 39 Vacaciones no gozadas | 39 | 62 | 25 (`FINIQUITO_VACACIONES`) |

Los números "12/33/39" del enunciado son el `CPNMCDGO` (código interno) del catálogo
de **esta** instalación, no una constante portable — coincide con el código alterno
solo para el concepto 12, por casualidad. `aprobar` resuelve el concepto de
"Vacaciones pagadas" por **código alterno** (`ConceptoNominaDaoService.
selectByCodigoAlterno(12L, idEmpresa)`), que es la clave estable entre instalaciones
según su propio Javadoc — **no** por `CPNMROLM`, porque el concepto 12 no tiene rol
del motor asignado (está en blanco): el motor nunca lo genera solo, así que nunca
necesitó uno. No fue necesario ningún cambio de catálogo para esto.

## Nota sobre `SLCTESTD` (reporte, sin cambiar nada)

Pedido explícito: reportar qué valores toma hoy y si algo compara ese texto, sin
tocarlo.

- **Valores actuales en la base local:** uno solo, `'APROBADA'` (1 fila — la misma
  solicitud de prueba del enunciado).
- **Comparaciones en el código:** ninguna, antes de este cambio — `grep` de
  `SLCTESTD` en todo `src/main/java` solo encuentra la anotación `@Column` en la
  propia entidad `SolicitudVacaciones.java`. Nada más leía ni comparaba ese campo.
- **Después de este cambio:** las comparaciones que introduce
  `SolicitudVacacionesServiceImpl` (`aprobar`/`rechazar`/`anularAprobacion`) contra
  las constantes `"APROBADA"`, `"RECHAZADA"`, `"ANULADA"`, todas case-insensitive.

Es un campo de texto libre sin ningún consumidor previo — normalizarlo a un rubro
numérico (como el resto del sistema) es viable sin romper nada existente, porque no
hay comparaciones legadas que migrar; solo las tres que se acaban de agregar. Queda
la decisión en manos del usuario.
