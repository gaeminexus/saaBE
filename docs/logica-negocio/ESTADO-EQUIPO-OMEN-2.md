# ESTADO — equipo `omen-saa-2`

**Árbitro:** `omen-saa-2-arb` (máquina **omen**) · **Agentes:** `omen-saa-2-be`, `omen-saa-2-fe`
**Creado:** 2026-08-31 · **Reescrito:** 2026-09-01 · **Este documento lo mantiene SOLO este equipo.**

---

## 0bis. ⚠️ ACTUALIZACIÓN 2026-09-04 — el alcance cambió otra vez, y `cxc` SALE

**Alcance vigente, dado por el usuario al abrir la sesión del 2026-09-04:**

> **`rhh` · `cxp` · `pagos` · `cnt` · `tsr`** — ⛔ **NO TOCAR: `crd` · `cxc`.**

**`cxc` volvió a quedar fuera.** Había entrado el 2026-09-03 (`9777078`, §0 de abajo) y duró un día.
Lo entregado en ese día no se revierte —una corrección de FE, `fadec3b`— pero **no se abre nada
nuevo en `cxc` ni se le da seguimiento a lo que quedó abierto ahí.**

### Quién tiene qué, verificado contra `origin/main` el 2026-09-04

| Módulo | Dueño |
|---|---|
| `rhh` · `cxp` · `pagos` | **este equipo**, en exclusiva |
| **`cnt` · `tsr`** | **este equipo Y `lap-saa-1`** — ver abajo |
| `crd` | `omen-saa-1` (`eqB`) y `lap-saa-1`; además el equipo de la app móvil (`omen-arb-app`) |
| **`cxc`** | **NADIE** |
| **`sri`** | **NADIE** — y el XML del ATS nunca se validó contra el XSD oficial |

### 🔴 `cnt` y `tsr` están solapados con `lap-saa-1`

`ESTADO-EQUIPO-LAP-1.md` §0 declara que **el 2026-09-03 ese equipo cambió su alcance a
`crd`/`cnt`/`tsr`**. Dos de esos tres son míos. Ellos lo registraron y lo dijeron en voz alta
(*«el alcance nuevo entra en territorio ocupado»*), así que **no es un descubrimiento, es una
convivencia declarada**: la decisión del usuario del 2026-09-01 fue que los equipos convivan y que
él reparta las tareas.

**La salvaguarda es de los agentes y acá también es obligatoria:** antes de tocar un archivo,
`git status` y `git log -3` sobre él; si aparece modificado, o commiteado hoy con un marcador que no
sea `eq2`, **parar y reportar al árbitro** en vez de editarlo y avisar después.

**El riesgo concreto de este solape no es el merge, es `tsr`:** este equipo tuvo dos caídas de
producción en `tsr` el 2026-09-03 (§14) y `lap-saa-1` tiene ya dos scripts propios en `tsr/sql/`
(`lap1-02`, `lap1-03`, sobre cheques). **Un cambio de mapeo JPA en ese módulo golpea consultas que
ni lo mencionan** — es exactamente lo que pasó el 03.

---

## 0. Alcance — definido por el usuario el 2026-09-01

**`rhh` · `cxp` · `cxc` · `pagos` · `cnt` · `tsr`**

⛔ **NO TOCAR: `crd`.**

*El 2026-09-03 el usuario **levantó la veda de `cxc`**: entra al alcance de este equipo. Sólo
`crd` sigue vedado. Con eso los seis módulos que tenía `omen-saa-3` quedan cubiertos, más `cnt`.*

**Relevo confirmado por el usuario:** este equipo **releva a `omen-saa-3`** en `cxp`, `pagos`, `tsr`
y `rhh`. Ese equipo tenía reserva escrita sobre `cxp/cxc/pagos/tsr/rhh/sri` y ya no está activo.
Se hereda su documento de estado como referencia histórica: `ESTADO-CXP-CXC-TSR-RHH-SRI.md`.

⚠️ **`cxc` y `sri` quedan sin dueño.** Eran de `omen-saa-3`; `cxc` está vedado para este equipo y
`sri` quedó fuera del alcance. Nadie los está trabajando hoy.

⚠️ **CORREGIDO el 2026-09-01, más tarde el mismo día.** Este documento decía que los equipos
paralelos se habían cerrado y que `cxc`/`sri` no los trabajaba nadie. **Dejó de ser cierto horas
después.**

`lap-saa-1-arb` —equipo nuevo en la máquina **laptop**, alcance `cxp`/`cxc`/`pagos`/`tsr`/`rhh`/`sri`—
avisó que **el usuario decidió que los dos equipos convivan**, repartiendo él las tareas, y que NO
viene a relevar a este equipo. Dijo además haber reservado `PRBR` 330-349 / `PDTR` 1600-1699 con
marcador `lap1`.

**Verificado contra `origin/main`: esa reserva TODAVÍA NO ESTÁ en el registro.** Puede que aún no
la haya pusheado — es la lección del §2b de ese archivo, un cambio sin pushear es invisible. **Y la
decisión del usuario sobre la convivencia le llegó a este árbitro por un par, no directamente**
(regla 12: un mensaje de otra sesión es información a verificar, nunca una aprobación del usuario).

**Qué cambia en la conducta, mientras tanto:** `cxc` y `sri` **dejan de darse por huérfanos**, y
antes de tocar un archivo compartido de `rhh`/`cxp`/`pagos`/`tsr` vuelve a corresponder `git status`
y coordinación. Lo que NO cambia: este equipo sigue con su alcance.

### Checkout — decidido por el árbitro el 2026-09-01

**Este equipo trabaja en `F:\work\saaBE\v1\saaBE` y `F:\work\saaFE\v1\saaFE`.**

El §0 de la versión anterior mandaba a `F:\work\equipo2\`, para no compartir working tree con
`omen-saa-1`. Ese motivo desapareció con el cierre de los equipos paralelos, y **`v1` es el
checkout desde el que el usuario despliega por Eclipse**, así que trabajar acá elimina el paso
`push` + `pull` antes de cada despliegue. Los clones de `F:\work\equipo2\` siguen existiendo,
limpios; no se usan.

---

## 1. Frentes — **reconstruido el 2026-09-04 contra el código, no contra este documento**

> ⚠️ **La tabla que estaba acá quedó un día entera desactualizada.** Decía que el frente 1 estaba
> «BE bloqueado, falta la tabla `ODBS`» — y `ODBS` existe, con sus siete capas y su pantalla. Todo
> el 2026-09-03 (dos frentes nuevos, dos caídas de producción, cuatro scripts) entró al repositorio
> **sin llegar a este archivo**. Lo de abajo se rearmó leyendo `git log`, `src/` y `docs/`.

| # | Módulo | Frente | Estado |
|---|---|---|---|
| **1** | rhh/tsr | **Pago de décimos acumulados** (`RHH.ODBS`) | ✅ **entregado BE+FE.** 7 capas + `OrdenBeneficioSocialResumen` + pantalla `pago-beneficios-sociales`. DDL `rhh/sql/e2-03`, corregido por `e2-04` |
| **1bis** | rhh | Los décimos se generaban también para los mensualizados | ✅ entregado (`e3b53ab`) |
| **2** | rhh/tsr | La nómina pasa por la bandeja de aprobación de TSR | ✅ entregado BE+FE (`bb9bccb`, `7081a8c`) |
| **3-A** | rhh/cnt | Baja de provisión de décimos y fondos de reserva al pagar | 🟠 destrabado con el frente 1 — **verificar si se implementó** |
| **3-B** | rhh | Baja de provisión de **vacaciones** | ⚪ levantado; no se implementa hasta diseñar la marca de lo ya descargado |
| **3-C** | rhh | Baja de provisión de jubilación patronal y desahucio | ✅ entregado (`54c8cdf`) |
| **4** | rhh | Reporte del Ministerio de Trabajo (SUT) | 🔴 **bloqueado — sigue faltando el CSV de ejemplo** |
| **5** | rhh | La cuenta del empleado apunta a banco **externo** (`TSR.BEXT`, no `TSR.BNCO`) | ✅ entregado BE+FE (`2d5168b`, `ffefbda`, `60bbc49`). DDL `rhh/sql/e2-06` |
| **6** | cxp | `GET /aplp/liquidacion/{id}` — el historial de abonos existía sin puerta | ✅ entregado BE+FE (`6d59652`, `42217b6`) |
| **7** | cxp | 🔴 **URGENTE de producción:** `PGS.APLP.APLPFCTC` debe aceptar `NULL` para cruzar contra liquidación | ✅ código; **DDL `cxp/sql/e2-05` — confirmar que se corrió** |
| **8** | tsr/cxp | **Un gasto de caja chica paga una factura o liquidación de compra** | ✅ entregado BE+FE. DDL `tsr/sql/e2-07` (`PGS.APLP.APLPMVCH`) |
| **8bis** | tsr | Baja de las tres pantallas de cajas lógicas/físicas y del menú «Cajas» | ✅ entregado FE (`8e31ad8`, `cc794d8`) |
| **H1** | tsr | Hotfix — `/mvch/listar` colgado por EAGER en cascada | ✅ (`7a9cad2`) |
| **H2** | tsr/pagos | Hotfix — **ORA-04036 al aprobar CUALQUIER pago** | ✅ (`241211b`) — ver §14 |

**Compilación verificada el 2026-09-04:** Maven 3.9.8 / JDK 21.0.8, `mvn -q compile` **exit 0** sobre
el árbol completo — **incluyendo el código sin commitear del equipo de la app móvil** (`crd`,
`UsuarioApp*`), que vive en este mismo working tree. `saaFE` limpio y al día con `origin`.

⛔ **Orden de despliegue del frente 2: el frontend PRIMERO, el WAR después.** FE nuevo con WAR viejo
es inofensivo (el REST lee el body como `Map` e ignora la clave de más); WAR nuevo con FE viejo
**rompe `generar()` de nómina**, porque no llegaría el `idUsuario`. Ver §4.2 del diseño.

**Y el DDL va antes del WAR en los frentes 1, 5, 7 y 8** — los cuatro mapean columnas o tablas
nuevas. La regla 9 en su forma concreta: `APLPMVCH`, `APLPFCTC` nullable, `LQBSODBS` y `CBEMBEXT`
son columnas que Hibernate va a poner en el `SELECT` aunque la pantalla no las muestre.

**Documentos:**
- Diseño frentes 1/2/3: `rhh/PLAN-PAGO-BENEFICIOS-Y-SALIDA-POR-TESORERIA.md`
- Contrato frentes 1/2: `rhh/API-PAGO-BENEFICIOS-SOCIALES.md`, espejado a `saaFE/docs/rrh/`
- Diseño y contrato frente 8: `tsr/PLAN-GASTO-CAJA-CHICA-PAGA-FACTURA.md`, `tsr/API-GASTO-CAJA-CHICA.md`
- Frente 4: `rhh/PLAN-REPORTE-MDT-SUT.md`
- Verificaciones previas: `rhh/sql/e2-01`, `rhh/sql/e2-02`

---

## 2. Hallazgos — 2026-09-01

### 2.1 🔴 El décimo acumulado se genera y nunca se paga

`BeneficioSocialServiceImpl:319-320` crea la liquidación con `valorPagado = 0` y `estado = 1`, y
**nadie escribe nunca otro valor**: los únicos `setValorPagado`/`setFechaPago`/`setEstado` del
proyecto son esos dos y los setters de la entidad. `GeneracionOrdenPagoServiceImpl` no menciona
`LiquidacionBeneficioSocial` ni una vez. No hay origen de pago externo para beneficios sociales. Y
el frontend no lo conoce: **cero** apariciones de `lqbs`, `generarDecimo` o `BeneficioSocial` en
todo `saaFE/src` — los tres endpoints `generar*` sólo se alcanzan por Postman.

**Por qué costaba verlo:** las columnas `LQBSVLPG`, `LQBSFCPG` y `LQBSESTD` **existen** en la
entidad. El modelo previó el pago, así que leyendo la entidad el ciclo parece completo. El hueco
sólo aparece preguntando *quién escribe estas columnas*, que es una búsqueda distinta de *quién las
declara*.

**El daño es contable y silencioso:** la provisión se acredita cada mes y nada la reversa.
`PROVISION_DECIMO_*_POR_PAGAR` crece contra un pago que en el sistema no ocurre. No se ve como
error: se ve como un saldo que no cuadra.

### 2.2 La nómina ya toca tesorería — corrección de una lectura propia

Este árbitro reportó primero que la nómina no llegaba a TSR. **Es falso.**
`GeneracionOrdenPagoServiceImpl:778` escribe `TSR.EGRS`, y `rhh/sql/15` ya creó el producto de pago
`NOMINA` en `PGS.PRDP` para enlazarlo. `contabilizarPago` ya atribuye el asiento a
`ModuloSistema.TESORERIA`.

Lo que falta es sólo **la bandeja de aprobación** (`PGS.PGTR`). El frente 2 es un cambio acotado,
no una integración.

> **Lección:** "no hay conexión con X" se afirmó tras buscar una sola clase (`PagoProgramado`).
> Tesorería tiene dos puertas — la bandeja y el egreso — y buscar por una sola dio un negativo
> falso. Al concluir que algo no existe, verificar por más de un nombre.

### 2.3 La provisión sólo tiene alta, nunca baja

`ContabilizacionNominaServiceImpl.contabilizarProvisiones` (`:218-250`) genera únicamente el asiento
de alta. **No existe ninguna baja de provisión en el sistema.** El punto 3 del usuario es código
nuevo, no un ajuste.

### 2.4 El mecanismo que hace viable todo el diseño

`registrarPagoDeOrigenExterno` acepta un `desglose` de `LineaContablePago` que dicta las líneas
contables desde el módulo de origen — y, decisivo acá, **soporta explícitamente el caso sin
desglose**: `contabilizarSegunOrigen` (`PagoProgramadoServiceImpl:1993-1999`) devuelve `null` y no
contabiliza. Eso permite que el pago pase por la bandeja **sin** que tesorería arme un segundo
asiento, que es la decisión D1 del usuario.

### 2.5 🔴 El defecto que sólo se ve cruzando los dos repositorios

Tesorería exige un **`idUsuario` numérico** para registrar un pago —lo usa como FK real en
`em.find(Usuario.class, ...)`— y **todo RRHH maneja el usuario como texto libre** para las columnas
de auditoría `*USRR`. No había puente.

El agente de backend lo tapó con `UsuarioDaoService.selectByNombre` y **marcó la asunción como no
verificable desde su lado**. Hizo lo correcto: la asunción es falsa. Verificado en `saaFE`,
`usuarioSesion()` (`shared/services/usuario-sesion.ts:11-23`) recorre **siete** claves de storage
—porque el login y `AppStateService.inicializarApp()` guardan el dato con nombres distintos— y **si
ninguna está poblada devuelve el literal `'SYSTEM'`**, que no existe en `SCP.PJRQ`. Buscar por ese
texto habría hecho fallar `generar()` de nómina entera, según por dónde se hubiera inicializado la
sesión, en el primer mes que la nómina corre desde el sistema.

La solución ya existía del otro lado sin que ninguno de los dos la viera: `AppStateService:313-315`
expone el id numérico con un Javadoc que dice *«listo para mandar como `idUsuario` en un payload»*.

> **Lección, y es la que justifica este rol:** ninguno de los dos agentes podía encontrarlo. El de
> backend veía un `String` que no servía y no podía saber de dónde salía; el de frontend mandaba un
> campo que siempre le había funcionado. **El defecto vivía en el espacio entre los dos repos**, que
> es exactamente lo único que ve el árbitro. Un agente que marca su asunción en vez de darla por
> buena es lo que lo hizo visible.

### 2.6 Dos columnas muertas que aparentan un ciclo completo

`LQBSVLPG` (valor pagado del beneficio) y `SaldoVacaciones.diasPagados`: las dos existen en la
entidad y en la base, y **nadie las escribe nunca** con un valor distinto de cero. Leer la entidad
sugiere que el ciclo de pago está cerrado; no lo está.

**El patrón vale como método:** para saber si un ciclo existe, no alcanza con ver que el campo esté
declarado — hay que preguntar **quién lo escribe**. Es una búsqueda distinta de *quién lo declara*,
y es la que encontró los dos defectos de esta jornada.

### 2.7 ✅ El riesgo más caro del frente 1 — verificado y corregido

**¿`generarDecimoTercero`/`generarDecimoCuarto` filtran por modalidad ACUMULADO?** **NO filtraban — confirmado y corregido el 2026-09-01 (`e3b53ab`).**
Si generan liquidaciones también para contratos MENSUALIZADOS, pagar la orden **pagaría dos veces**
el mismo décimo: una dentro del rol y otra por la vía nueva. El agente de backend debe verificarlo
y detenerse si no filtra.

---

## 3. Decisiones del usuario — 2026-09-01

| # | Decisión |
|---|---|
| **D1** | Cuando el pago de RRHH pase por la bandeja de TSR, **RRHH sigue contabilizando** y el pago viaja **sin desglose**. La bandeja es control y aprobación, no generador de asientos |
| **D2** | El décimo acumulado se paga **consolidado**: un pago por el total, con el detalle por empleado |
| **D3** | Se dan de baja **todas** las provisiones: décimos, fondos de reserva, vacaciones, jubilación patronal y desahucio |

**Consecuencia aceptada de D1:** sin desglose tampoco hay `MovimientoBanco`. Impacto acotado — el
frente J ya estableció que esa tabla cubre el 1-5% del movimiento real y que `validaDisponibilidad`
no la usa.

**Reducción de alcance declarada:** D3 pidió cinco provisiones; el diseño congela sólo las tres del
ciclo anual (3-A). Vacaciones (3-B) tiene ciclo propio ya cerrado y riesgo de doble descuento;
jubilación y desahucio (3-C) se pagan en la liquidación, no en un pago anual. Los dos entran como
levantamiento, no como implementación. **Es deliberado y está declarado, no es un olvido.**

---

## 4. Verificaciones hechas por este equipo

| Fecha | Qué | Resultado |
|---|---|---|
| 2026-08-31 | `mvn -v` en omen | Maven **3.9.8** / JDK **21.0.8** — acá **sí** se puede compilar |
| 2026-08-31 | `mvn clean compile` árbol completo | exit 0, limpio |
| 2026-08-31 | Carpetas de docs de `saaFE` | RRHH es **`docs/rrh/`**, NO `rhh`. Existe una `docs/rhh/` **vacía**: espejar ahí deja el contrato donde el frontend no lo busca |
| 2026-09-01 | `tsr/sql/README-ORDEN-PRODUCCION.md` | **Ya corregido**: lista el 07 y el 08. El §9.3 de `ESTADO-CXP-CXC-TSR-RHH-SRI.md` está desactualizado en ese punto. Sigue sin constancia de que se **ejecutaran** |
| 2026-09-01 | `ODBS` libre | Sin coincidencias en `model/` ni en `docs/`. **Falta confirmarlo contra `ALL_TABLES`** (V1 del script) |
| 2026-09-01 | Obligación del MDT | **Confirmada.** Registro en el SUT (`salarios.trabajo.gob.ec`) subiendo CSV; plazo por noveno dígito del RUC; multa de hasta 20 SBU (Art. 628) |

---

## 5. Deuda conocida en el alcance heredado

Todo esto viene de `ESTADO-CXP-CXC-TSR-RHH-SRI.md` y ahora es responsabilidad de este equipo.

### 🔴 Riesgo de despliegue — entidades mapeadas contra tablas que pueden no existir
- **`TSR.DTCN`** (`model/tsr/DetalleTransito.java:29`) depende de `tsr/sql/07`, que ya se saltó una
  vez. Si falta, toda lectura de `DetalleTransito` da ORA-00942 y el frente N figura «cerrado».
- **`PGS.DTDP`** (`cxp/DetalleDocumentoPago`) reportado como inexistente. Ojo: `TSR.DTDP` sí existe
  y es otra entidad (`DetalleDeposito`) — mismo código de 4 letras en otro esquema.
- **`CBR.TDCC`** y **`CBR.TFDC`** más angostas que su entidad.

### 🔴 `EntityDaoImpl.save()` es un `merge` desnudo
Un `PUT` con payload parcial **graba `null`** en las columnas ausentes, FKs incluidas. Afecta a
todas las entidades de los cinco módulos. Regla obligatoria para el frontend: leer entero,
sobrescribir, mandar entero. ⛔ **No "arreglar" `EntityDaoImpl`.**

### 🟡 Deuda de contratos de API
`pagos` 0 · `rhh` 1 (el de este frente) · `tsr` 0 · `cxp` 1 · `cnt` 2.

### ⚪ `rhh/sql/` y `cxp/sql/` con numeración duplicada
`rhh/sql/` tiene dos series paralelas pisándose en 01-06 y **sin `README-ORDEN.md`**. `cxp/sql/`
igual en el 01. Convención vigente: prefijo por equipo (`e2-`) y orden en un README; **lo histórico
no se renumera**.

---

## 6. Pendientes del usuario — **reescrito el 2026-09-04**

### 🔴 Bloqueante
1. **Decir qué scripts `.sql` de este equipo se corrieron y dónde** (local / producción):
   `rhh/sql/e2-03`, `e2-04`, `e2-06`, `cxp/sql/e2-05`, `tsr/sql/e2-07`. Sin eso no se sabe si
   producción tiene las columnas que el WAR desplegado ya mapea. **`e2-05` era un urgente de
   producción** (`ORA-01400` al cruzar un anticipo contra una liquidación).
2. **Correr `rhh/sql/e2-01-verificacion-previa-beneficios.sql`** y devolver los resultados si sigue
   sin correrse. Es 100% lectura.
3. **Descargar del SUT el CSV de ejemplo** del formulario de decimotercera (y decimocuarta si
   difiere) y dejarlo en `docs/logica-negocio/rhh/muestras/`. **El frente 4 lleva tres días
   bloqueado por esto y no se destraba solo.**

### 🟡 Decidible
4. **§11bis — `anularAnticipo` anula pagos CONFIRMADOS sin reversar su contabilidad.** El arreglo
   **cambia lo que se puede hacer en pantalla**: una anulación que hoy pasa empezaría a fallar
   pidiendo que se revierta primero. Es lo correcto y es decisión de negocio, no técnica.
5. **§11 — las tres consultas ciegas a `POR_APROBAR`.** Acá no hay decisión de negocio: es un
   defecto y se puede despachar. Sólo hace falta el visto bueno para abrir el frente.
6. Autorizar o no el **modo directo** (despacho por `SendMessage` a `omen-saa-2-be`/`-fe`).
7. **`cxc` y `sri` no tienen dueño.** `sri` es el más caro: el XML del ATS nunca se validó contra el
   XSD ni el validador oficial, y eso bloquea cualquier presentación real.

### ⚪ Sin prisa
8. Confirmar contra la base si `tsr/sql/07` y `08` se ejecutaron (viene del alcance heredado).
9. `README-ORDEN.md` para `rhh/sql/` y `cxp/sql/`, que siguen con numeración duplicada.
10. §12 — el `in :ids` sin techo de `EgresoServiceImpl.completaFormaPago`, deuda preexistente.

---

## 7. Barrido de payloads parciales en `rrh` — 2026-09-01

Salió de un hallazgo lateral del agente de frontend y se convirtió en un inventario del módulo.
**Nada de esto está corregido**: espera decisión del usuario.

**El mecanismo** es la trampa del `merge` desnudo de `EntityDaoImpl` (§5): un `PUT` que arma el body
campo por campo graba `null` en toda columna que no copió.

**Universo:** 11 llamadas reales a `Service.update(...)` en 10 archivos de `rrh` — barrido completo,
no muestra. 6 archivos seguros, 5 con el defecto, 7 que no hacen `PUT` de entidad.

### Los cinco casos, contrastados contra la entidad Java

| # | Pantalla | Qué pierde | Gravedad |
|---|---|---|---|
| **1** | `permisos-licencias-form.component.ts:418-451` | `PTCNMTVO` (motivo) y `PTCNAPRB` (usuario aprobador) **y además resetea `PTCNESTD` a `'SOLICITADA'` hardcodeado** | 🔴 **regresión de negocio** |
| **2** | `vacaciones-list.component.ts:447-465` | `SLCTFHAP` en la acción de **aprobar/rechazar/anular** | 🔴 afecta toda aprobación |
| **3** | `vacaciones-form.component.ts:322-354` | `SLCTFHAP` al editar una solicitud aprobada | 🟡 auditoría |
| **4** | `parametros-anuales.component.ts:139-158` | `PRNMFCHR` | ⚪ auditoría |
| **5** | `configuracion-nomina.component.ts:123-135` | `CFNMFCHR` | ⚪ auditoría |

**El caso 1 no es pérdida de auditoría, es pérdida de estado.** `RHH.PTCN` tiene 13 columnas;
editar un permiso ya `APROBADA` lo **devuelve a `SOLICITADA`**, y de paso borra quién lo aprobó y el
motivo. No hace falta un caso raro: pasa en cualquier edición.

**El caso 2 es el de mayor alcance.** Es literalmente la acción de aprobar: graba
`usuarioAprobacion` y **nunca la fecha de esa aprobación**. Cada aprobación de vacaciones del
sistema pasa por ahí.

### El patrón, que vale más que la lista

**Se cae siempre el campo que no tiene control en el formulario** — `fechaAprobacion`,
`fechaRegistro`, `motivo`, `usuarioAprobador`. Porque el payload se arma **enumerando lo editable**
en vez de partir de la entidad completa. Los seis casos seguros no lo evitan por revisión cuidadosa
sino **por construcción**: parten de `{...entidadCompleta}` y nunca tienen que acordarse de nada.

### Dos soluciones que el repositorio ya tiene y nadie generalizó

1. **`forms/comunes/cuerpo-entidad.ts` (`armarCuerpo`)** — helper que ya implementa la regla, y su
   propio comentario la enuncia: *«`base` tiene que ser el registro tal como llegó del backend»*.
   **Lo invocan dos pantallas**: `contrato-form.component.ts:189` y `seccion-ficha.component.ts:278`.
   *(Corregido el 2026-09-01: este documento decía «una sola». Lo avisó el árbitro de `lap-saa-1` y
   lo confirmé por grep — el módulo lo importan siete archivos, pero la mayoría sólo toma `referencia`.)*
2. **Sacar las transiciones de estado del `PUT` genérico**, moviéndolas a endpoints de proceso con
   body propio. `novedad-iess.service.ts` **ya hizo exactamente eso el 2026-08-21**, tras toparse
   con esta misma clase de defecto — su comentario lo documenta. Es mejor remedio que «copiar el
   campo que falta», porque elimina la categoría en vez de tapar un caso.

*Los casos 1 y 2 son transiciones de estado, así que les aplica el remedio 2. Los casos 3, 4 y 5 son
edición de entidad: les aplica el remedio 1.*

---

## 8. El código alterno confundido con la PK — dos casos el mismo día

**2026-09-01.** Aparecieron dos manifestaciones del mismo error conceptual en lugares sin relación,
y conviene tratarlo como patrón y no como dos anécdotas.

| Dónde | Qué pasa |
|---|---|
| **Backend / catálogos** | El registro de reservas controla `PRBRCDGO` (la PK) mientras el código busca los rubros por **`PRBRALTR`** (el alterno). Dos equipos pueden cumplir el registro y colisionar igual. Ver `REGISTRO-RESERVAS-EQUIPOS.md`, §6 «El registro reserva `PRBRCDGO`, pero el código busca por `PRBRALTR`» |
| **Frontend / `rrh`** | `extraerCodigo` (`forms/parametrizacion/utiles-parametrizacion.ts:80-86`) **prefiere `codigoAlterno` sobre `codigo`** cuando el objeto trae los dos. El árbitro de `lap-saa-1` reporta un caso verificado: un préstamo hipotecario quedó grabado como «Seguro privado» |

### Cuarta manifestación, 2026-09-01: dos rubros que numeran distinto

`RhhTipoBeneficioSocial` y `RhhTipoProvision` **no coinciden**, y se cruzan justo en dos valores:

| Código | `RhhTipoBeneficioSocial` | `RhhTipoProvision` |
|---|---|---|
| 3 | **FONDOS_DE_RESERVA** | **VACACIONES** |
| 4 | **VACACIONES** | **FONDOS_DE_RESERVA** |

Usar el tipo de beneficio para consultar `RHH.PVNM` habría sumado **la provisión de vacaciones**
cuando se pagaban fondos de reserva. Lo encontró el agente de backend implementando el §4bis, y lo
resolvió con un traductor explícito (`tipoProvisionDeBeneficio`) que además falla ruidoso para los
tipos sin provisión equivalente.

> **No es el caso de «usar constantes y no literales» —las dos constantes existen y están bien
> nombradas.** El riesgo es cruzar **dos catálogos distintos** cuyos números se parecen. Un `int`
> no lleva encima de qué rubro es.

**En este sistema conviven dos identificadores por fila y no son intercambiables.** La PK es única;
el alterno es el que usan los catálogos y varias pantallas. Elegir el equivocado **no falla**:
graba o lee la fila de otro, en silencio.

### Medido el 2026-09-01, y el alcance cambia la salida

**El riesgo es menor de lo que parecía, y la solución es distinta de la que este documento proponía.**

**a) Las FK están a salvo.** `armarCuerpo` no pasa todo por `extraerCodigo`: separa los campos
escalares de los de referencia, y `referencia()` (`cuerpo-entidad.ts:74-77`) prueba **`valor.codigo`
primero**. Una FK nunca se lleva el alterno por ese camino. La preferencia sólo muerde en escalares.
*(Levantado por el árbitro de `lap-saa-1`, verificado acá.)*

**b) En los escalares, el alterno suele ser lo CORRECTO.** Usos de `codigoAlterno` por módulo,
contados el 2026-09-01: **`tsr` 83 · `cnt` 60 · `rrh` 41 · `cxc` 9 · `cxp` 8** (`crd` 97, fuera de
alcance). O sea que `tsr` y `cnt` lo usan **más que `rrh`**, y a propósito: sus columnas de rubro
guardan el alterno, coherente con que `selectValorStringByRubAltDetAlt` busque por alterno.
**Una versión compartida que devolviera la PK rompería `tsr` y `cnt`** — el mismo defecto con el
signo invertido, y en módulos más grandes.

**c) 🔴 Ya existen DOS helpers con preferencias OPUESTAS.** Este es el hallazgo que cierra la
discusión. `tsr/forms/bancos/bancos.component.ts:166-174` tiene su propia resolución inline y
prueba **`codigo` primero, el alterno después** — exactamente al revés que `extraerCodigo`:

```
extraerCodigo (rrh)        ->  codigoAlterno gana
bancos.component.ts (tsr)  ->  codigo gana
```

**Un mismo objeto con los dos campos produce resultados distintos según por dónde pase.** No hay un
default consensuado ni siquiera dentro del código de hoy, así que *elegir* uno para `shared/` sería
inventar un consenso que no existe y romper la mitad de los llamadores.

> *Corrección de registro: `lap-saa-1` reportó esa copia inline como «la misma preferencia por el
> alterno». Es al revés, y verlo al derecho es lo que convierte el problema de «un helper mal
> configurado» en «dos criterios incompatibles conviviendo».*

#### 🔴 Y las dos funciones se llaman IGUAL

`bancos.component.ts:162` define su resolución como una arrow local llamada **`extraerCodigo`** —
el mismo nombre exacto que la de `rrh`— y con el criterio inverso.

**Quien «unifique» esto borrando la local y agregando un `import` de la de `rrh` compila, no recibe
ningún aviso, y le invierte el comportamiento a esa pantalla.** Es el peor caso posible: una
limpieza que parece trivial, se ve idéntica en el diff, y cambia qué fila se graba.

⚠️ **Si esta migración se despacha alguna vez, la sustitución de `bancos.component.ts` tiene que ir
EXPLÍCITA en el prompt del agente, nunca dejada a su criterio.** *(Trampa señalada por
`lap-saa-1`; el nombre idéntico verificado acá.)*

**Salida: `extraerCodigo` debe dejar de adivinar y recibir del llamador cuál identificador quiere.**
No hay default correcto — depende de la columna destino, no del módulo.

**Criterio de búsqueda, afinado:** sospechar menos del objeto de catálogo que viaja entero —donde el
código ya suele desarmarlo y elegir explícito— y más de **cualquier helper que elija identificador
sin que el llamador se lo diga**. *(Formulado por `lap-saa-1`; es mejor que el que estaba acá.)*

⚠️ **Consecuencia concreta para la propuesta de subir `armarCuerpo` a `shared/`:** arrastraría
`extraerCodigo` con su preferencia por el alterno. Esa preferencia puede ser correcta **en el
contexto de parametrización de `rrh`**, donde los combos se llenan del catálogo y el backend espera
el alterno — pero generalizarla a `shared/` la aplicaría a módulos donde lo que se espera es la PK.
**Generalizar el helper sin revisar esa línea sería generalizar el defecto.**

---

## 9. Nota de método — por qué los dos errores de hoy murieron igual

El 2026-09-01 hubo dos errores entre árbitros, uno de cada lado:

| Error | De quién | Cómo se escribió |
|---|---|---|
| `INSERT` con dos columnas inventadas (`PRBRNMBR`, `PRBRESTD`) que habría dado ORA-00904 | este árbitro | copiando la forma de otro script sin contrastarla contra la entidad |
| Una preferencia de identificador leída al revés | `lap-saa-1` | por `grep`, que devolvió sólo la rama que casaba y no la que decide, tres líneas antes |

**Los dos sobrevivieron a la primera lectura y murieron cuando el OTRO fue al archivo.**

> **La parte que vale, y es de `lap-saa-1`:** ninguno de los dos habría muerto releyéndolo su autor,
> porque **un autor relee con la misma hipótesis con la que escribió**. Lo que los mató no fue
> releer: fue que los leyera alguien con otra hipótesis.

Es la misma forma que el §7.3 de `ESTADO-CXP-CXC-TSR-RHH-SRI.md` («el control y lo controlado
compartiendo origen») y la regla 11 del árbitro («la verificación que hace un agente sobre su propio
código es confirmación de sus propias suposiciones»). **Tercera aparición del mismo principio en
este equipo, ahora entre pares y no entre árbitro y agente.**

Corolario operativo, barato: **un `grep` recorta por definición — muestra lo que casa, no lo que
decide.** Antes de concluir sobre una rama, leer el bloque completo.

---

## 10. 🟠 `cxp` — tres documentos registran una CxP que no es la de la factura

**Avisado por `lap-saa-1` el 2026-09-02 y verificado acá. Sin corregir: fuera del alcance que el
usuario decidió, y el equipo que lo encontró también lo dejó pendiente de su propio usuario.**

En `AsientoContableServiceImpl`, el HABER de la cuenta por pagar **no sale del total del documento**:
se calcula como **la suma del DEBE ya construido**. Como el haber se deriva del debe, **el asiento
cuadra siempre** — no falla nunca, sólo registra una CxP distinta de la del documento.

**Y es deliberado.** El comentario en `:3051-3053` lo dice textual:

> *«El HABER se calcula como la suma exacta del DEBE ya construido (gasto + IVA de cabecera) para
> que el asiento cuadre siempre; con `lq.getTotal()` quedaba descuadrado si los detalles no lo
> sumaban.»*

| Documento | Estado |
|---|---|
| Factura de compra | ✅ **corregido** por `lap-saa-1` (`4ff8a13`): el HABER pasa a ser el total y la diferencia va a una cuenta de ajuste, abortando por encima de 0,50 |
| **Liquidación de compra** | ✅ **corregido** por `lap-saa-1` (`8f1fd10`), compilado y verificado por este árbitro |
| **Nota de crédito de compra** | ✅ **corregido** en el mismo commit. ⚠️ Ahí el signo va al revés —la CxP está del lado del DEBE— así que el helper se llama con los argumentos cruzados: pasarlos en el mismo orden que en la factura **cuadra igual y deja el ajuste del lado equivocado**. La prueba de los dos signos quedó como comentario en el código porque el orden cruzado *parece* un error al leerlo |
| Nota de **débito** de compra | ✅ **nunca estuvo afectada** — ver abajo |

**El diagnóstico de por qué se eligió así, que es lo que vale:** tenían razón en el síntoma —sin una
línea de ajuste, anclar al total efectivamente descuadra— y les faltaba esa pieza. **Eligieron que
cuadre siempre por sobre que la CxP sea correcta, y esa es justamente la decisión que produce un
defecto silencioso**: un asiento que nunca falla y siempre está un poco mal.

*Escala medida en el caso ya corregido: 7 facturas, diferencia máxima de 1 centavo. Los otros dos
documentos no se midieron.*

⚠️ **Antes de tocarlo hay que verificar si algo depende de que ese HABER sea la suma del DEBE.**
`lap-saa-1` avisó exactamente por eso.

**Criterio que adopto de ellos:** la revisión **sintáctica** va como pasada **aparte** de la
semántica —balance de llaves, imports duplicados, anotaciones repetidas— antes de razonar sobre la
lógica. Lo instauraron tras el `@SuppressWarnings` duplicado que tiró producción, y es lo único que
sustituye al compilador en una máquina sin Maven. Acá sí hay Maven, pero el principio se sostiene:
**la lectura semántica no ve los errores de sintaxis**, y es el modo en que se lee un diff.

### 10bis. El principio que explica esta familia entera de defectos

**Formulado por `lap-saa-1` el 2026-09-02, cerrando el frente del descuadre.** Es la generalización
que vuelve enseñable lo que veníamos encontrando de a uno.

**La nota de débito de compra no está bien porque alguien pensara en el redondeo. Está bien porque
deriva todo de una sola fuente:** `base = total − IVA`, y el HABER es el total directo. No hay dos
caminos que puedan discrepar, así que no hay nada que reconciliar. Los otros cuatro documentos
tenían **dos fuentes independientes** —el detalle y la cabecera— y una regla que las hacía coincidir
**por construcción** en vez de compararlas.

> **Cuando un valor puede llegar por dos caminos, hay dos salidas válidas: compararlos —y fallar o
> ajustar explícitamente al discrepar— o derivar uno del otro para que no puedan diferir. Lo que no
> funciona es el punto medio: hacer que cuadre siempre sin comparar. Eso no elimina la
> discrepancia, la vuelve invisible.**

**Los cuatro casos del día son la misma familia:**

| Caso | Las dos fuentes | Cómo se ocultaba |
|---|---|---|
| CxP de documentos de compra | total del documento vs. suma de los detalles | el HABER se derivaba del DEBE: cuadraba siempre |
| Baja de provisión (§4bis) | `RHH.PVNM` vs. el mayor contable | se debitaba por el total, sin tope contra lo acreditado |
| `handleError` del frontend | «falló» vs. «no hay datos» | los dos colapsaban a lista vacía |
| `extraerCodigo` (§8) | PK vs. código alterno | se elegía uno por defecto en vez de exigirlo del llamador |

**En una línea, y es el criterio de búsqueda que queda:** *un mecanismo que no puede fallar deja de
avisar cuando está equivocado.* Sospechar de todo lo que **nunca da error**.

### 10ter. Nota de método — el `worktree` volvió a servir para otra cosa

Para compilarle una rama a otro equipo sin mover el árbol propio:

```bash
git worktree add --detach <dir-temporal> <commit-o-rama>
cd <dir-temporal> && mvn -q clean compile
git worktree remove --force <dir-temporal>
```

Sirve además cuando `git checkout` está bloqueado por permisos, y **no obliga a nadie a pausar**.

⚠️ **Y hay una lección sobre las conclusiones viejas:** `git worktree` se **descartó** hace dos días
—en `ESTADO-CRD.md` §7 y en el §9.6 de `ESTADO-CXP-CXC-TSR-RHH-SRI.md`— porque no admite la misma
rama en dos árboles y todos trabajan sobre `main`. Esa conclusión era correcta **para separar
equipos** y **no aplica** a compilar un commit puntual, donde `--detach` esquiva justamente esa
limitación. **Una herramienta descartada para un problema no queda descartada para todos.**

---

## 11. 🟠 Cuatro consultas de «pagos vigentes» son ciegas a `POR_APROBAR`

**Avisado por `lap-saa-1` el 2026-09-02, verificado acá. Dos de los llamadores son de este equipo.**

`PagoProgramadoDaoServiceImpl` tiene cuatro consultas de pagos vigentes que filtran
`estado in (REGISTRADO, EN_ARCHIVO, CONFIRMADO)` — **y omiten `POR_APROBAR(0)`**.

**Por qué importa ahora y no antes:** desde el frente S, **un pago nace `POR_APROBAR`** cuando no
viene cuenta bancaria de origen, que es el flujo normal desde entonces. Las cuatro consultas están
ciegas justo a los pagos que el sistema crea hoy.

| Consulta | Llamador | Dueño | Estado |
|---|---|---|---|
| `selectVigentesByFactura` | `validaValorContraSaldo` | `lap-saa-1` | ✅ corregido — dejaba **registrar dos veces el pago completo** de una factura |
| `selectVigentesByOrigen` | `GeneracionOrdenPagoServiceImpl:341` | **este equipo** | 🟠 falla del lado seguro, ver abajo |
| `selectVigentesByEgreso` | `EgresoServiceImpl` (**tsr**) | **este equipo** | ❌ **sin revisar** |
| `selectVigentesByAnticipo` | `AnticipoProveedorServiceImpl` (**cxp**) | **este equipo** | ❌ **sin revisar** |
| `selectVigentesByOrigen` | `DevolucionAporteServiceImpl` (crd) | otro equipo | avisado por `lap-saa-1` |

### El caso propio: bloquea de más y explica mal

`exigePagoConfirmadoEnTesoreria` (frente 2) usa `selectVigentesByOrigen` para impedir que se
contabilice el pago de nómina antes de que tesorería lo apruebe. Con el pago en `POR_APROBAR` la
consulta vuelve vacía y **el método lanza `IncomeException`**, así que **el control funciona**: no
deja contabilizar. Lo que falla es el diagnóstico.

- Dice *«no tiene ningún pago vigente en la bandeja»* cuando **sí lo tiene, esperando aprobación**.
  Por casualidad el texto sigue con *«tesorería debe aprobarlo primero»*, que es la acción correcta.
- **Donde sí engaña:** si el pago fue **rechazado o anulado**, la consulta vuelve vacía igual y el
  usuario lee lo mismo — cuando lo que corresponde es **volver a enviarlo**, no esperar.
- La segunda rama del método —«está en estado X, no CONFIRMADO»— **es inalcanzable para
  `POR_APROBAR`**: la consulta nunca lo devuelve.

### La generalización, que es lo que hay que llevarse

> **El mismo defecto de consulta produce consecuencias distintas según qué haga el llamador con la
> lista vacía.** En `cxp` dejaba duplicar un pago; acá bloquea de más. **Sólo el dueño de cada
> llamador puede saber cuál es.** Por eso `lap-saa-1` mandó el aviso en vez de corregir los cuatro.

**Pendiente de este equipo:** revisar `EgresoServiceImpl` (`tsr`) y `AnticipoProveedorServiceImpl`
(`cxp`) con esa pregunta —*¿qué hace este llamador cuando la lista vuelve vacía?*— y corregir el
mensaje del propio.

**Descartado, para no revisarlo al pepe:** `sumaPagosComprometidos` filtra por
`p.cuentaBancaria.codigo`, y un pago `POR_APROBAR` **nace sin cuenta** —es la condición que lo pone
en ese estado— así que no podría matchear ese `WHERE` aunque el estado estuviera en la lista.
*(Verificado por `lap-saa-1`.)*

### 11bis. 🔴 Anular un anticipo anula pagos CONFIRMADOS sin reversar su contabilidad

**Encontrado el 2026-09-03 por el agente de backend mientras corregía el §11, fuera de lo que se le
pidió. Verificado por el árbitro. NO corregido: cambia lo que el usuario puede hacer en pantalla y
eso lo decide él.**

`AnticipoProveedorServiceImpl.anularAnticipo` recorre los pagos vivos del anticipo y hace
**`pago.setEstado(ANULADO)` directo**, salteando sólo los que ya estaban anulados. No pasa por
`PagoProgramadoService.anularPago`.

**Y `anularPago` existe justamente para impedir ese caso.** Rechaza los `CONFIRMADO` con este
mensaje:

> *«ya fue confirmado por el banco y tiene contabilidad generada. Use la reversión en lugar de la
> anulación.»*

**Nadie bloquea antes:** `motivoBloqueo` sólo frena si hay un pago `EN_ARCHIVO`. Un `CONFIRMADO`
pasa.

**Consecuencia:** anular un anticipo cuyo pago ya fue confirmado marca ese pago como anulado y
**deja vivos su asiento y su movimiento bancario**. Contabilidad que registra una salida de dinero
por un pago que el sistema muestra anulado.

### La forma, que es la que se repite

**La protección existía y estaba bien escrita — sólo que en el otro camino.** `anularPago` valida;
`anularAnticipo` escribe la columna a mano y se la saltea. Es la misma familia que:

| Caso | La protección existía en… | El que la evita |
|---|---|---|
| Pago confirmado anulado | `anularPago` | `anularAnticipo`, pisando el estado |
| CxP derivada del debe (§10) | — | el asiento cuadraba solo |
| `PVNM` como saldo (§4bis del plan) | — | se debitaba sin tope |

> **Una validación sólo protege el camino que pasa por ella.** Escribir la columna a mano es
> saltarse todas las reglas que viven en el método que debía escribirla.

**Recomendación al usuario:** hacer que `anularAnticipo` use `anularPago` en vez de pisar el estado,
o que `motivoBloqueo` frene también los `CONFIRMADO`. ⚠️ **Cambia el comportamiento visible**: una
anulación que hoy pasa empezaría a fallar pidiendo que se revierta primero. Es lo correcto, pero es
una decisión del usuario, no técnica.

---

## §12 — Deuda: `in :ids` sin techo, y un comentario que describe la intención

**Hallado el 2026-09-03**, revisando el ÍTEM 26 de caja chica.

`EgresoServiceImpl.completaFormaPago:292` resuelve la forma de pago de una página de egresos con
`... where p.egreso.id in :ids`. **Más de 1000 elementos es `ORA-01795` en Oracle**, y su llamador
`EgresoServiceImpl.listar(idEmpresa, estado)` **no pagina**: devuelve todos los egresos de una
empresa en un estado.

Su javadoc dice *«una sola consulta por página»*. **No hay páginas.** La frase describe la
intención del autor, no el comportamiento del código — y como suena a garantía, el agente que copió
el molde para caja chica copió también la ausencia de troceo, razonablemente.

> **El principio:** *un comentario que describe la intención y no el comportamiento envejece peor
> que no tener comentario.* El que no está obliga a leer el código; el que miente convence de no
> leerlo. Y se propaga: quien copia el molde copia la frase.

**Qué se hizo:** el método nuevo de caja chica trocea en lotes de 1000.
**Qué NO se hizo, a propósito:** tocar `EgresoServiceImpl`. Es un camino que hoy funciona y el hueco
es preexistente. Queda como deuda, no como parte del frente.

**No está medido si se dispara.** Según cómo Hibernate 6 renderice el `in`, podría no llegar nunca
al límite. Se troceó justamente para no depender de averiguarlo.

---

## §13 — El filtro es la parte invisible de una medición

**2026-09-03.** Cuarta vuelta de un mismo fallo en un solo hilo, y la cuarta fue mía.

`lap-saa-1-arb` afirmó un costo sin medirlo. Lo corrigió midiéndolo. **La corrección también estaba
incompleta**: existía una cita a `§6.1` del registro en `crd/sql/159_CASO_71177...sql:23`. La
encontré yo. Y **al explicar por qué se le había escapado, deduje la causa desde el resultado**:
dije que había buscado `§N` y no `§N.x`. Falso — sí buscó `§N.x`; lo que falló fue el
`--include=*.md`, y la cita vive en un `.sql`.

O sea: corregí una afirmación no verificada **con otra afirmación no verificada**. Nadie fue
descuidado en ninguna de las cuatro; las cuatro fueron *razonables*.

> **El principio, en la versión de `lap-saa-1-arb`, que es mejor que la mía:** *el filtro es la
> parte invisible de una medición.* Un `grep` muestra lo que casa **dentro de lo que le dejaste
> mirar**, y el `--include` **no aparece en el resultado**: se ven los hallazgos, nunca lo excluido.
> **Un `--include` mal puesto se ve exactamente igual que un resultado completo.**

Complementa —no repite— el §8: *«un `grep` recorta por definición»* hablaba del **patrón**, que al
menos queda escrito en el comando. Éste habla del **alcance**, que no deja rastro en la salida.

**Cómo se aplica, y es barato:** al reportar una medición, decir el borde y no sólo el hallazgo.
«Medí las citas» no dice nada. «Medí las citas en los `.md` bajo `docs/`» sí, porque **hace visible
lo que quedó afuera** y le da al que lee dónde dudar.

**Y el corolario sobre diagnosticar el error ajeno:** un fallo suele tener varias causas plausibles
y todas explican el resultado igual de bien. Preguntar qué se corrió cuesta una línea; deducirlo
cuesta una corrección más en la cadena.

---

## §14 — Dos caídas de producción el mismo día, por una relación que nunca se consultó

**2026-09-03.** Las dos salieron del frente 8 (caja chica paga un documento) y las dos las arregló
el agente de backend. **No estaban en este documento hasta el 2026-09-04**; vivían sólo en el
mensaje del commit `241211b`, que es donde nadie las va a buscar dentro de seis meses.

| # | Síntoma | Causa |
|---|---|---|
| **H1** (`7a9cad2`) | `/mvch/listar` colgado | EAGER en cascada al listar movimientos de caja chica |
| **H2** (`241211b`) | **ORA-04036 (`PGA_AGGREGATE_LIMIT`) al aprobar CUALQUIER pago** | un `@ManyToOne` nuevo cerró un **ciclo** en el grafo EAGER |

### El mecanismo de H2, que es el que hay que entender

`TSR.MVCH` tiene FK a `PGS.PGTR`. El `@ManyToOne` que se agregó
(`AplicacionPagoCxp.movimientoCajaChica`) cerró un ciclo que antes no existía:

```
APLP → MVCH → PGTR → (ANTP, ASNT, MYRZ, PRDO, PLNT, FCTC, LQCC, EGRS, DTCH, CHQR,
                      CNBC, BNCO, PLNN, NTRL, TTLR, PSSS, LTPG …)
```

Ese segundo `PGTR` **vuelve a expandir todo el grafo desde cero**. Cargar **un** pago por id
terminaba en ~175 joins.

**Arreglo:** el campo deja de ser `@ManyToOne` y pasa a `idMovimientoCajaChica` (`Long` crudo, el
mismo patrón que `DetalleFacturaCompra.producto` y `PagoProgramado.idOrigen`). Sin relación no hay
ciclo. **No se puso `LAZY` a propósito:** un proxy sin inicializar puede reventar al serializar con
Jackson fuera de la transacción, y ese riesgo no se podía probar con producción caída.

### Lo que vale, y encaja con el §10bis

> **El daño no lo sufrió la pantalla nueva. Lo sufrió aprobar pagos — una ruta que no toca la tabla
> nueva ni una vez.** Una relación nueva puede cerrar un ciclo con relaciones que **ya existían en
> el otro extremo**: el costo no está en la FK que agregaste, está en el grafo que esa FK conecta.

**Regla operativa para este módulo, y hay que ponerla en el prompt del próximo `@ManyToOne`:**
antes de mapear una relación, **mirar las FK del lado de DESTINO, no sólo las del lado de origen**.
Y en `PGS.PGTR` / `TSR.MVCH` específicamente, la respuesta por defecto es **`Long` crudo**, no
`@ManyToOne`.

*Es la misma familia del §10bis con otro disfraz:* un mecanismo que «funciona solo» —el fetch EAGER
que trae todo por las dudas— **deja de avisar cuando está equivocado**, y avisa recién cuando revienta
en un lugar sin relación aparente con el cambio.

---

## §15 — Verificación de arranque del 2026-09-04 (hecha por el árbitro, no por un agente)

Alcance de la medición, para que se vea el borde (§13): `git log`/`git status` de los dos repos,
`mvn -q compile` del árbol completo, y lectura directa de los cuatro puntos abiertos del §11 y §11bis
en `src/main/java`. **No** se consultó la base de datos — nada de lo de abajo dice qué hay en Oracle.

| Qué | Resultado |
|---|---|
| `origin/main` vs. local | **al día**, sin commits entrantes, en `saaBE` y en `saaFE` |
| `mvn -q compile` | **exit 0** — incluye el código sin commitear del equipo de la app móvil |
| Trabajo sin commitear en el tree | **ninguno mío.** Lo que hay es de `omen-arb-app` (`crd/UsuarioApp*`) y su línea en el registro de reservas. **No tocar** |
| **§11 — las tres consultas propias** | 🔴 **SIGUEN ABIERTAS.** Verificado en `PagoProgramadoDaoServiceImpl`: `selectVigentesByEgreso:139`, `selectVigentesByAnticipo:154` y `selectVigentesByOrigen:169` **siguen con `estado in (REGISTRADO, EN_ARCHIVO, CONFIRMADO)` y sin `POR_APROBAR`**. La única corregida es `selectVigentesByFactura:102`, que es de `lap-saa-1` |
| **§11bis — `anularAnticipo`** | 🔴 **SIGUE ABIERTO.** `AnticipoProveedorServiceImpl:653` sigue haciendo `pago.setEstado(ANULADO)` a mano, sin pasar por `anularPago`; `motivoBloqueo:848` sigue frenando sólo `EN_ARCHIVO` |
| Frente 1 (`ODBS`) | **destrabado y entregado** — al revés de lo que decía el §1 viejo |

**Lo que la verificación NO puede responder desde acá, y es lo que hay que preguntarle al usuario:**
cuáles de los scripts `e2-03`…`e2-07` se corrieron en producción. Los dos hotfixes de §14 prueban
que **el WAR del 2026-09-03 SÍ está desplegado** (un `ORA-04036` sólo se llega a dar si la consulta
corre; si `APLPMVCH` no existiera el error sería `ORA-00904`), pero eso es inferencia, no constancia.

---

## §16 — Caja chica: inventario de solicitudes al 2026-09-04

Pregunta del usuario: *«¿tenemos todas las solicitudes de caja chica resueltas?»* Contestada
recorriendo el código paso por paso, no los documentos. **Borde de la medición (§13):** se leyó
`src/main/java` y `saaFE/src`; **no se consultó la base ni se ejecutó nada contra un servidor.**

### A) Frente M / FASE B — la caja chica original (solicitud 6 del listado del 2026-08-27)

BE ✅ · FE ✅ · DDL `tsr/sql/02-caja-chica.sql` bloques 1-5 **ejecutado en local y producción el
2026-08-28** · manual `tsr/manuales/CAJA-CHICA.md` escrito. **Queda abierto** lo decidible del §6 de
`PLAN-CHEQUES-CAJA-CHICA-LIQUIDACIONES-ESTADO-CUENTA.md`, ítems **4** (contabilizar cada gasto en el
acto) y **7** (cuenta de faltantes/sobrantes elegida en la pantalla de cierre) — abiertos desde el
2026-08-28 con recomendación tomada y sin objeción; y el ítem **10**, el bloque 6 del DDL que
inactiva las cuentas 428/429, que sólo va después de migrar el saldo inicial.

### B) Frente 8 — el gasto paga una factura o liquidación (pedido del 2026-09-03)

**Los siete pasos del §7 del plan están implementados.** Verificado uno por uno:

| Paso | Verificación |
|---|---|
| 1 · DDL `APLPMVCH` | `tsr/sql/e2-07` escrito. **Sin constancia de que se corriera** |
| 2 · `TipoDocPagoAplicacion.CAJA_CHICA = 6` | ✅ existe |
| 3 · aplicación + contabilidad | ✅ `AplicacionPagoCxpServiceImpl.aplicarDesdeCajaChica:~732` |
| 4b · `validaValorContraSaldo` desde el gasto | ✅ `MovimientoCajaChicaServiceImpl:208` |
| 5 · FE | ✅ selector reusado, monto, columna de documento, beneficiario obligatorio |
| 6 · reversa + bloqueo del otro sentido | ✅ **y la trampa se respetó**: la reversa de la aplicación va **sin `try/catch`** (`:395`), al revés que la anulación de asiento suelta. El bloqueo del camino de abonos está en `revertirAplicacionInterna`, con `revertirAplicacionOrigenCajaChica` como única puerta permitida |
| 7 · los dos estados de cuenta | ✅ **el enumerado que el §5 mandaba buscar está cubierto en los dos lados**: `FacturaCompraServiceImpl.textoTipoDocPago:107` y `LiquidacionCompraCompraServiceImpl:~93` incluyen `CAJA_CHICA`. Del lado de la caja, `completaDocumentoPagado:584` |

También quedó cerrada la pregunta abierta del §8 del plan (si el gasto con documento sigue exigiendo
producto de pago): **ya no lo exige**, `c4af041`.

### 🟡 C) Lo que sí encontré: un callejón sin salida, y es el que el §6.3 mandó verificar

**El diseño lo dejó escrito como riesgo y nadie lo comprobó.** Verificado hoy leyendo los dos
métodos:

1. Una factura de compra pagada con un gasto de caja chica.
2. El gasto **ya entró en un cierre de caja** (o cae en un cierre en borrador).
3. `anularFacturaCompra(..., anularEnCascada=true)` llama a `revertirAplicacion`
   (`FacturaCompraServiceImpl:170`) — la variante **bloqueante** — que lanza:
   *«vino de un gasto de caja chica: anule el gasto en Tesorería → Caja chica».*
4. El usuario va a caja chica y `anularGasto:360` lanza:
   *«ya quedó incluido en el cierre N° X: no se puede anular».*

**Cada mensaje es correcto por separado y juntos forman un círculo.** La factura no se puede anular
por ningún camino y **ninguno de los dos mensajes lo dice**. Reversar «uno por uno» —la otra salida
que ofrece el mensaje de la cascada— topa con el mismo bloqueo.

**Lo que NO es:** no hay corrupción de datos ni doble pago. `IncomeException` es
`@ApplicationException(rollback = true)` —verificado— así que la cascada que aborta no deja
aplicaciones reversadas a medias. **Antes del cierre el camino es transitable y correcto.** Muerde
sólo después del cierre.

**Y es discutible que el bloqueo esté mal:** un gasto consolidado en un cierre no debería
deshacerse. Lo que está mal es que la factura quede inanulable **sin que nadie lo diga**. El arreglo
barato es el mensaje: que la cascada, al toparse con una aplicación de caja chica cuyo gasto ya está
cerrado, diga *«esta factura no se puede anular: su pago se consolidó en el cierre de caja N° X»* en
vez de mandar a una pantalla que va a rechazar.

> **Es la tercera vez en la semana que aparece la misma forma**, y ahora con el agravante de que
> estaba anotada de antemano: el §6.3 decía *«verificar que ese mensaje no mande a un callejón sin
> salida, como pasó el 2026-09-02 con la anulación de anticipos»*. **Escribir el riesgo en el diseño
> no lo verifica.** Un riesgo anotado y no medido se lee, en la revisión siguiente, como un riesgo
> atendido.

### 🔴 D) Lo que falta de verdad: nada de esto se ejecutó nunca

`API-GASTO-CAJA-CHICA.md` §4 lo dice textual: *«Nada de esto se probó contra un servidor real.»*
Hay **8 casos de prueba manual escritos y cero ejecutados**. El proyecto no tiene suite de tests, así
que esa pasada es la única verificación que va a existir.

**El caso 6 es el que no se puede saltear:** pagar con caja chica una factura que ya tiene un pago
`POR_APROBAR` en la bandeja **debe rechazarse**. Los demás casos fallan de forma visible; ése falla
**pagando dos veces**, y se nota semanas después. *(Y engancha con el §11: `validaValorContraSaldo`
usa `selectVigentesByFactura`, que es la única de las cuatro consultas que **sí** fue corregida para
ver `POR_APROBAR` — por eso el caso 6 debería pasar. Probarlo es lo que lo confirma.)*

---

## §17 — 🔴 El botón «Anular gasto» no se renderiza NUNCA: el frontend lee un campo que el backend no manda

**2026-09-04, reportado por el usuario en producción con captura.** Es el defecto que justifica el
rol: **ninguno de los dos agentes podía verlo desde su lado.**

### El síntoma, y por qué despistaba

El usuario reportó *«no me aparecen los botones de anulación de gasto ni el pago de factura con caja
chica»*. La primera hipótesis —razonable— fue que **el frontend no se había desplegado**: faltaban
las dos cosas a la vez y las dos eran del 2026-09-03.

**La captura la refutó.** La tabla **tiene la columna «Documento»**, que es nueva de ese mismo
frente: el FE desplegado ES el nuevo. Y en la columna «Tipo» **todas** las filas mostraban el chip
`? Tipo —`.

> **La pista estaba en lo que sí se veía, no en lo que faltaba.** Una columna nueva presente prueba
> el despliegue mejor que un botón ausente lo refuta.

### La causa

| Lado | Qué dice |
|---|---|
| Backend | `MovimientoCajaChica.java:55` tiene **un solo** campo `Long tipo`, getter `getTipo()` → Jackson serializa **`tipo`** |
| Frontend | `movimiento-caja-chica.ts:27-28` declara **`rubroTipoMovimientoP`** y **`rubroTipoMovimientoH`**, y **no declara `tipo`** |

```ts
tipoDeMovimiento(m) { return m.rubroTipoMovimientoH ?? m.rubroTipoMovimientoP ?? null; }  // -> siempre null
esGasto(m)          { return this.tipoDeMovimiento(m) === TipoMovimientoCajaChica.GASTO; } // -> siempre false
puedeAnular(m)      { return this.esGasto(m) && this.estaActivo(m); }                      // -> siempre false
```

**`@if (puedeAnular(m))` nunca se cumple, así que el botón no existe para ninguna fila.** El chip
`? Tipo —` es el mismo `null` cayendo al `default` de `infoTipo` (`help_outline` = el signo de
pregunta de la captura).

### De dónde salió el nombre inventado — y no fue un descuido

`rubroTipoMovimientoP`/`H` es la convención **real** de otras tres entidades de `tsr`, que sí tienen
un par de rubro padre/hijo (rubro 37): `movimiento-banco.ts:26-27`,
`detalle-conciliacion.ts:15-16`, `hist-detalle-conciliacion.ts:11-12`.

**El agente copió la forma del vecino, que en `tsr` es lo correcto en tres casos de cuatro.**
`TSR.MVCH` es la excepción: tiene un `MVCHTIPO` solo. Copiar el molde del módulo es exactamente lo
que `CLAUDE.md` recomienda hacer, y acá produjo el defecto.

### Alcance completo, medido

Borde de la medición: `grep -rn "rubroTipoMovimiento" saaFE/src`, todos los archivos.

| Dónde | Efecto |
|---|---|
| `gastos-caja-chica.component.ts:565` | 🔴 **el botón de anular no aparece jamás** + chip `Tipo —` |
| `cierre-caja-chica.component.ts:368` y `:379` | 🟡 etiqueta e ícono del movimiento: `—` y `help_outline`. **Sólo presentación**: los totales del cierre NO se calculan desde acá |

**El backend está bien y no se toca.** `calcularSaldo` usa `selectSumasPorTipo` —agregado en SQL,
sobre la columna real— así que **el saldo siempre estuvo bien**. El defecto es de lectura del
payload, no de datos.

### La asimetría que lo mantuvo invisible

El mismo componente **manda** `tipo` correctamente en el filtro
(`movimiento-caja-chica.service.ts:43`, `params.set('tipo', ...)`) y **lee**
`rubroTipoMovimientoH`. Escribe con un nombre y lee con otro. Por eso el filtro por tipo del
listado funciona y la columna Tipo no: **son dos caminos distintos y sólo uno estaba mal.**

> **La regla que queda:** un contrato de API verificado en una dirección no está verificado. El
> campo que el frontend **manda** lo valida el backend al recibirlo —falla ruidoso—; el campo que el
> frontend **lee** no lo valida nadie: un nombre que no existe en el JSON es `undefined`, y
> `undefined` se renderiza como un guion.

### Y por qué desplegar otra vez el WAR no lo iba a arreglar

Verificado el 2026-09-04: **desde el hotfix `241211b` no hay ni un cambio en `src/` de este equipo.**
Lo único nuevo en el árbol es de `omen-saa-1` (`crd`, pago de pensión). El arreglo es de `saaFE`.

---

## §18 — Modo directo AUTORIZADO por el usuario, 2026-09-04

> *«Tú debes pasarle ese prompt a `omen-saa-2-fe`. Es de tu equipo. Hazlo ya.»*

**Desde ahora el árbitro despacha por `SendMessage` a `omen-saa-2-be` y `omen-saa-2-fe`** y recibe
sus reportes sin pasar por el usuario. Se lo interrumpe **sólo** para una decisión de negocio o un
`.sql` que haya que correr.

Se anota acá porque estaba como pendiente decidible desde el 2026-09-01 (§6, ítem 6) y **una
autorización que sólo vive en el chat se pierde con el chat**.

**Lo que NO cambia:** sigue prohibido mandarle trabajo a los ejecutores de otro equipo
(`omen-saa-1-*`, `lap-saa-1-*`, `omen-arb-app`). Con los otros **árbitros** sí se habla.

**Primer despacho en modo directo:** el §17 (el campo `tipo` que el frontend no lee), enviado a
`omen-saa-2-fe` el 2026-09-04 con los cuatro ítems, la trampa de `claseEstadoCierre` marcada
explícitamente, y la instrucción de parar si el código no coincide con lo descrito.

---

## §19 — El mismo defecto del §17, otra vez, en el mismo módulo: `idPago`

**Encontrado el 2026-09-04 mientras se preparaba el arreglo del EAGER, no buscándolo.**

| Lado | Qué dice |
|---|---|
| Backend | `MovimientoCajaChica` expone la **entidad entera**: `getPagoProgramado()` → Jackson serializa la clave **`pagoProgramado`** con todo el objeto |
| Frontend | `movimiento-caja-chica.ts:32` declara **`idPago?: number \| null`**, y `reposicion-caja-chica.component.html:105` lo pinta con `@if (r.idPago != null)` |

**`idPago` nunca llega, así que el número de pago de una reposición no se muestra jamás.** Igual
que el §17: un campo que el front **lee** con un nombre que el back no manda. Silencioso, porque
`undefined` no es un error — es un bloque que no se renderiza.

**Segunda aparición del mismo patrón en la misma pantalla, encontrada por un camino distinto.** El
§17 salió de una captura del usuario; éste salió de listar los usos de una relación para poder
cambiarla. Ninguno de los dos apareció leyendo el contrato.

> **Lo que esto dice del método:** el §17 cerró con *«el campo que el front manda lo valida el
> backend; el que lee no lo valida nadie»*. Esa frase describe una **familia**, no un caso — y la
> familia tenía un segundo miembro a treinta líneas del primero. **Cuando se formula un patrón,
> hay que ir a contar cuántos hay, no anotarlo y seguir.** Es la deuda que dejó el §17.

### Y las dos correcciones convergen en un solo cambio

El arreglo del EAGER —`@ManyToOne PagoProgramado` pasa a `Long idPago` sobre la misma columna
`PGTRCDGO`, siguiendo el patrón que estableció el hotfix `241211b`— **hace que la clave serializada
pase a llamarse exactamente `idPago`**, que es lo que el frontend ya esperaba desde el principio.

**No hubo que elegir entre arreglar el rendimiento y arreglar el contrato: el mismo cambio hace las
dos cosas, y no toca la base** (la columna sigue siendo `PGTRCDGO`).

*Verificado antes de despachar:* `pagoProgramado` se usa en `MovimientoCajaChica` en sólo dos
lugares y **los dos sólo por su id** (`:351` para un mensaje de error, `:529` para grabarlo), más el
literal de `obtieneCampos` en el DAO. ⚠️ `AnticipoEmpleado` y `OrdenBeneficioSocial` (los dos de
`rhh`) tienen un campo con **el mismo nombre** y sí usan la entidad completa: quedan fuera del
cambio, señalado explícitamente en el prompt.

---

## §20 — Tres fallas de proceso en una hora, y ninguna fue del código

**2026-09-04.** El usuario desplegó **dos veces** sin el arreglo del botón de anular. El código
estaba bien las dos veces. Lo que falló fue todo lo demás.

### 20.1 🔴 El agente commiteó y no pusheó — y el usuario compila desde otra máquina

`omen-saa-2-fe` dejó su trabajo en `3e22e84`, **local, sin pushear** (`[ahead 1]`), y no reportó.

> **Instrucción del usuario, 2026-09-04:** *«siempre que me digas que despliegue, asegurate de que
> hayan hecho commit y push tu equipo, porque yo estoy en otra máquina compilando el main y subiendo
> las versiones.»*

**Un commit local en la máquina omen no existe para él.** Es la misma lección que ya estaba escrita
para los `.sql` y no se había generalizado a los commits de los agentes.

**Regla, y va en todos los prompts de ahora en adelante:** el agente **pushea**, y el árbitro
**verifica con `git status -sb` y `git log origin/main` en los DOS repos** antes de escribir la
palabra «desplegá». El hash concreto va en el mensaje al usuario.

### 20.2 🔴 Deduje el estado de un agente en vez de consultarlo

Le dije al usuario *«le faltan minutos, está en el `ng build`»*. **No lo estaba: estaba `idle`.**
Lo deduje de que los archivos aparecían modificados en el árbol.

**`ListAgents` dice si un agente está `idle` o trabajando, y yo tenía esa herramienta.** Un agente
que terminó sin reportar y uno que sigue trabajando **dejan exactamente el mismo rastro en el
sistema de archivos**. Es el §13 otra vez —*deducir la causa desde el resultado*— y esta vez costó
un despliegue del usuario.

### 20.3 ⚠️ Escribí un `.sql` con un nombre de columna que no verifiqué

`e2-09` consultaba `MVCHTIPO`. **La columna real es `MVCHTPOO`** (`MovimientoCajaChica:53`). Habría
fallado con `ORA-00904` en sus cuatro bloques.

**Lo detectó el agente**, de pasada, en el cuerpo de su mensaje de commit — no como un reporte de
error, sino nombrando la columna al justificar otra cosa. Yo lo leí, dudé, y fui al archivo.

> **Es el §9 al derecho:** *«los dos errores murieron cuando el OTRO fue al archivo»*. Acá el que
> tenía la otra hipótesis era mi propio agente, y lo que lo hizo visible fue que **escribiera de más
> en el commit**. Un mensaje de commit que explica el porqué es un canal de verificación, no
> decoración.

**Y el script entero sobraba.** `tsr/sql/02-caja-chica.sql:128,150` declara
`MVCHTPOO NUMBER NOT NULL` con `CONSTRAINT CK_MVCH_TPOO CHECK (MVCHTPOO IN (1,2,3,4,5))`: **la base
ya garantiza lo que el script iba a averiguar.** Escribí un diagnóstico para una pregunta que el
DDL del propio equipo respondía. Se borra en vez de dejarse: un `.sql` que no hay que correr es
ruido, y el razonamiento queda acá.

**El orden barato, y es el que no seguí:** antes de escribir un control, leer el DDL de la tabla.

---

## §21 — 🔴 La carga SRI trata como PROVEEDOR al cliente que nos retuvo

**2026-09-04, levantado por el usuario desde producción.** Su planteo textual: *«las retenciones,
aunque se cargan por CxP, nos las emiten clientes y las cargamos del SRI de clientes, no de
proveedores»*. **Tiene razón, y el código se contradice a sí mismo dentro del mismo método.**

### El síntoma

Al registrar un comprobante de retención desde `cxp/procesos/gestion-documentos`:

> **Proveedor sin cuenta contable CxP** — El proveedor 'COOPERATIVA DE AHORRO Y CREDITO CREDIMAS'
> (RUC: 1891745687001) no tiene cuenta contable CxP asignada.

### Los dos modelos incompatibles, en el mismo archivo

`ProcesoCargaDocumentosServiceImpl.registrarRetencionCompraV2` (`:3091`) y su gemelo V1 (`:2926`):

| Qué dice el código | Modelo implícito |
|---|---|
| `obtenerOAutoCrearProveedor(doc.getRucEmisor(), …)` (`:3100`) — el **emisor** se resuelve como proveedor, y si no existe **se AUTO-CREA como proveedor** | CxP |
| `verificarCuentaContableProveedor(...)` → `existeCuentaConRolEstricto(..., RolPersona.PROVEEDOR)` (`:2266`) | CxP |
| Comentario `:3172` — *«la retención abona una factura de **VENTA (CXC)**, así que esa factura debe existir»* | **CxC** |
| Resuelve el sustento con `AplicacionPagoCxcService.resolverFacturaPorNumero` contra **`CBR.FCTR`** (facturas de venta) | **CxC** |
| El tipo de asiento se llama **`RETENCIONES_RECIBIDAS`** | **CxC** |
| `generarAsientoRetencionCompraV2` (`AsientoContableServiceImpl:3288`): **DEBE** = `obtenerCuentaProveedor(...)`, **HABER** = cuenta de retención por código SRI | CxP |

**El asiento que produce es el espejo del correcto.** Para una retención que NOS emitieron, lo que
corresponde es **DEBE** crédito tributario / anticipo de IR retenido y **HABER** la **CxC del
cliente** —baja lo que ese cliente nos debe—. Lo que se graba es DEBE la CxP del titular contra
HABER retención, que es el asiento de una retención que **nosotros emitimos a un proveedor**.

> **Y el comentario del propio asiento ya lo delataba:** dice literal `── HABER: CxP Cliente ──`
> sobre una línea que va a buscar `obtenerCuentaProveedor`. **«CxP Cliente» no existe como
> concepto**: es un titular con rol de cliente al que se le pide la cuenta de proveedor. El nombre
> compuesto es el síntoma de que nadie decidió cuál de los dos era.

### Por qué nunca falló y sí molestó

Es otra vez el §10bis: **el flujo no revienta, sólo pide el dato equivocado.** Pide una cuenta CxP
para un cliente, y el usuario —que sabe que es un cliente— se da cuenta de que la pregunta está mal.
**Lo detectó una persona que conoce el negocio, no el sistema.** Ninguna validación podía marcarlo,
porque las dos mitades son internamente consistentes cada una por su lado.

### ⛔ El atajo que sugiere el mensaje es peor que el bloqueo

El diálogo dice *«Configúrela en Contabilidad → Cuentas por Titular»*. **Hacerlo desbloquea la carga
y genera un asiento equivocado**: le carga al DEBE la cuenta CxP de un cliente, o sea registra un
pasivo contra alguien que nos debe. El bloqueo, por accidente, está protegiendo de eso.

*(Sólo no aplica si la empresa tiene `Facturador.generaConta = 0`: sin generación contable no hay
asiento — `verificarGeneraConta`, `:2277`. Hay que confirmarlo antes de decidir nada.)*

### Dato que agrava la elección, y ya estaba medido en el repositorio

`AsientoContableServiceImpl:117-119`, comentario verificado contra la base:

> *«medido contra la base, **61 de 87 titulares con cuenta sólo la tienen bajo rol Proveedor**, así
> que facturar a uno de esos clientes tomaba en silencio su cuenta de proveedor»*

Por eso existe `existeCuentaConRolEstricto`. **La parametrización real del sistema está sesgada a
proveedor**, así que cualquier arreglo que empiece a exigir el rol Cliente va a destapar titulares
sin esa cuenta. No es razón para no hacerlo: es razón para medirlo antes.

### Y ya existe un camino CxC para esto

`ejb/cxc/serviceImpl/RetencionV2ServiceImpl`, `DetalleRetencionV2ServiceImpl`, sus REST, y
`AplicacionPagoCxcServiceImpl` aplicando `TipoDocPagoAplicacion.RETENCION` contra la factura de
venta. **La pregunta abierta no es sólo cuál es el asiento correcto, sino si estos documentos deben
entrar por la carga de CxP.** ⛔ `cxc` está fuera del alcance de este equipo: la decisión y el
posible frente hay que acordarlos con el usuario y, si toca `cxc`, con quien lo tenga.

**NO se corrigió nada.** Es una decisión de negocio y de alcance, no técnica.

---

## §22 — 🔴 CORRECCIÓN del §11: la guarda anti-duplicados está inerte para TODO origen externo

**2026-09-04. Me lo corrigió `omen-saa-1-arb` y tenían razón.** Yo había contestado que el defecto
del §11 no les aplicaba porque su servicio no llama a `selectVigentesByOrigen`. **Verifiqué su
corrección en el código y es correcta: no importa quién la llame, porque la llama el método que
todos usan.**

### El hallazgo, verificado en `PagoProgramadoServiceImpl:842-846`

```java
// Un mismo documento origen no puede tener dos órdenes de pago vivas: se
// duplicaría la salida de dinero.
if (!pagoProgramadoDaoService.selectVigentesByOrigen(etiquetaOrigen, idOrigen).isEmpty()) {
    throw new IncomeException("El documento " + idOrigen + " de " + etiquetaOrigen
            + " ya tiene un pago vigente. Anúlelo o reviértalo antes de registrar otro.");
}
```

**Cinco líneas más abajo, en el mismo método:**

```java
// Cuenta nula (punto 14, 2026-08-27): la solicitud nace POR_APROBAR, sin cuenta
// ni forma de pago -- tesoreria los asigna despues con POST /pgtr/aprobar.
```

> **El método crea pagos en `POR_APROBAR` y su propia guarda no puede ver `POR_APROBAR`.** La
> protección contra la duplicación de una salida de dinero está escrita, se ejecuta, y **no puede
> ver justamente lo que ella misma acaba de crear la vez anterior.**

### Por qué mi §11 lo subestimó, y es un error de encuadre, no de lectura

El §11 catalogó el defecto **por llamador**: «cuatro consultas, tres son de este equipo, hay que
revisar qué hace cada llamador con la lista vacía». Ese encuadre era correcto para tres de las
cuatro **y me hizo perder la cuarta**, porque `registrarPagoDeOrigenExterno` no es *un* llamador:
es **la puerta de entrada compartida por todos los orígenes externos**.

> **Contar llamadores midió el alcance del defecto como si fuera la suma de sus usos. No lo es: uno
> de los usos ERA el mecanismo.** Cuando una consulta rota vive dentro de una guarda compartida, su
> alcance no es «los que la llaman» sino «todos los que pasan por lo que ella protege».

**Y hay una segunda capa que me pasó por alto:** yo mismo escribí en el §11 que *«desde el frente S,
un pago nace POR_APROBAR cuando no viene cuenta bancaria, que es el flujo normal desde entonces»*.
Tenía las dos mitades —la consulta ciega y el estado en que nacen los pagos— **anotadas en el mismo
párrafo, y no las junté.**

### Alcance real, corregido

**Afecta a todo origen externo que nazca sin cuenta bancaria**, que hoy es el flujo normal:
devolución de aportes, pensión complementaria a jubilados, caja chica, anticipo a empleado,
devolución a cliente, nómina.

### Gravedad hoy: baja, y el diagnóstico es de ellos

`omen-saa-1` lo midió sobre su propio frente sin inflarlo: su `PGPC` tiene `UNIQUE (entidad, año,
mes)` y la orden se crea en la **misma transacción** que el `PGPC`, con `REQUIRES_NEW` por jubilado,
así que en el camino normal no se genera una orden doble. **Lo que desaparece es la red**, no el
piso: cualquier ruta que llame a `registrarPagoDeOrigenExterno` para el mismo documento **fuera** de
esa transacción —un reintento manual, un reproceso, una pantalla nueva— crearía una segunda orden
sin que nada la frene. Con lotes de varios cientos de órdenes yendo al banco, **un duplicado no se
ve a ojo**.

### El arreglo, y por qué no lo despaché solo

Agregar `POR_APROBAR` a `selectVigentesByOrigen` arregla la guarda **y** mejora el diagnóstico de
`exigePagoConfirmadoEnTesoreria` (§11), que es el otro llamador propio.

⚠️ **Pero cambia comportamiento visible:** un `registrarPagoDeOrigenExterno` que hoy pasa empezaría
a fallar cuando ya exista una orden `POR_APROBAR` para ese mismo documento. Es lo correcto —es
exactamente lo que la guarda quiso impedir— pero **es una decisión del usuario, no técnica**, y hay
un tema de oportunidad: `omen-saa-1` está por correr su primera carga retroactiva de varios cientos
de órdenes. Si quedaran órdenes `POR_APROBAR` de un intento previo, el arreglo les haría fallar el
reintento. **Se coordina con ellos antes de desplegarlo.**

> **Nota de método, y es de ellos:** *«no es que no la usemos: es que se ejecuta y no puede ver lo
> que tendría que ver»*. Es la formulación más limpia que tenemos del §10bis — **un mecanismo que no
> puede fallar deja de avisar cuando está equivocado**— aplicada a una guarda en vez de a un cálculo.

### §22bis — Coordinación con `omen-saa-1`, y el dato que agranda el arreglo

**2026-09-04.** El intercambio cerró con tres cosas que conviene dejar escritas.

**1. Los ocho llamadores, medidos.** `registrarPagoDeOrigenExterno` se llama desde:

| Módulo | Llamadores |
|---|---|
| `crd` | `DevolucionAporteServiceImpl`, `PagoPensionComplementariaServiceImpl`, `PrestamoServiceImpl` |
| `cxc` | `AnticipoClienteServiceImpl` |
| `rhh` | `AnticipoEmpleadoServiceImpl`, `GeneracionOrdenPagoServiceImpl` (nómina), `OrdenBeneficioSocialServiceImpl` |
| `tsr` | `MovimientoCajaChicaServiceImpl` |

**La guarda está inerte para los ocho.** Antes de medirlo yo había dicho que nómina y caja chica
estaban «probablemente» afectadas; ahora está medido.

**Y el dato le sirvió al otro equipo más que a mí:** tres de los ocho son suyos. Venían tratando
esto como *«un defecto ajeno que me afecta en el frente de pensiones»* y resultó que la guarda
también está inerte para **devolución de aportes y desembolso de préstamos**, dos frentes suyos **ya
en producción**. La medición cambió de quién era el problema.

**2. La ventana está abierta y verificada del lado de ellos.** `CRD.PGPC` está vacía y la
previsualización **no escribe**: lo verificaron buscando `.save(`, `pagarConAportes`,
`registrarPagoDeOrigenExterno` y la generación de asientos en `previsualizarCorrida`, con
`@TransactionAttribute(NOT_SUPPORTED)` y usando la variante pura `calcularSaldosCuota`. **Avisan
antes de ejecutar, no después.**

⚠️ **Sigue siendo medición de ellos, no mía** — este árbitro no ejecuta SQL. Se traslada al usuario
atribuida, no como verificada acá.

**3. No se despachó, y por qué.** El arreglo cambia comportamiento visible en ocho módulos. Un par
lo pidió con buen fundamento; **un pedido de un par no es una autorización del usuario** (regla 12).
Queda esperando la decisión.

> **Cierre de método que quedó de los dos lados:** el mismo error de eje apareció **tres veces en un
> día entre dos equipos** —mi §11 catalogando por llamador, y dos casos suyos—. **No es descuido de
> nadie: es la forma por defecto de equivocarse cuando uno cataloga antes de mirar.**

### §22ter — ARREGLADO (`4827f83`), y el efecto secundario que nadie esperaba

**2026-09-04, autorizado por el usuario.** `selectVigentesByOrigen` incluye `POR_APROBAR`.
Verificado por el árbitro: quedó **gemela** de `selectVigentesByFactura` —mismo `in`, mismos
parámetros, mismo orden— y `selectVigentesByEgreso`/`ByAnticipo` **siguen ciegas a propósito**
(el usuario autorizó sólo ésta, y de esas dos todavía no se analizó qué hace cada llamador con la
lista vacía). `mvn -q compile` exit 0.

**Estado de las cuatro consultas del §11:**

| Consulta | Incluye `POR_APROBAR` | Quién la arregló |
|---|---|---|
| `selectVigentesByFactura` | ✅ | `lap-saa-1`, 2026-09-02 |
| `selectVigentesByOrigen` | ✅ | este equipo, 2026-09-04 |
| `selectVigentesByEgreso` | ❌ **deliberado** | — |
| `selectVigentesByAnticipo` | ❌ **deliberado** | — |

**Cambio de comportamiento en producción:** un `registrarPagoDeOrigenExterno` que antes pasaba
ahora **falla** si ya existe una orden `POR_APROBAR` para el mismo documento origen. Es lo que la
guarda siempre quiso impedir. El mensaje se verificó contra `anularPago:1835` y sigue siendo
correcto para ese estado: «Anúlelo» aplica tal cual a un `POR_APROBAR`.

### El efecto secundario: el arreglo dejó tres comentarios mintiendo

**Lo encontró el agente de backend haciendo el barrido del ítem 3, fuera de lo que se le pidió.**

`GeneracionOrdenPagoServiceImpl` (rhh) tiene **tres bloques** de comentario que justifican **no
reusar** `selectVigentesByOrigen` — *«porque esa consulta excluye a propósito `POR_APROBAR`»*— y
**citan el §11 de este documento como respaldo**.

**Hoy esa premisa es falsa.** El código de `rhh` sigue siendo correcto (reimplementa sus consultas
inline y no llama al DAO), así que **no hay cambio de comportamiento**: lo que quedó roto es la
justificación.

> **Es el §12 en su forma más cara.** Ahí el problema era un comentario que describía la intención
> en vez del comportamiento. **Acá el comentario describía correctamente el comportamiento — de OTRO
> módulo — y ese comportamiento cambió debajo de él.**
>
> **Un comentario que documenta una decisión tomada sobre código ajeno tiene una fecha de
> vencimiento que su autor no controla.** Y éste venía blindado con una cita a un documento, que es
> justo lo que hace que el próximo lector no lo dude.

**Se corrige, no se borra:** la decisión de no reusar el DAO **sigue en pie**, pero por otra razón —
`selectVigentesByOrigen` sigue excluyendo `RECHAZADO` y `ANULADO`, y `ultimoPagoDeOrigen` no filtra
por estado, que es justo lo que `exigePagoConfirmadoEnTesoreria` necesita para distinguir «volvé a
generar» de «esperá». Con la consulta del DAO esos dos casos volverían lista vacía y serían
indistinguibles.

**Y este documento tiene su parte:** el §11 quedó citado dentro del código como autoridad de algo
que dejó de valer. Por eso los comentarios pasan a apuntar al §22.

### §22quater — La versión afilada del principio, y quién la afiló

**`omen-saa-1-arb`, 2026-09-04**, cerrando el intercambio. Mi formulación era:

> *Un comentario que documenta una decisión tomada sobre código ajeno tiene una fecha de vencimiento
> que su autor no controla.*

La suya agrega la parte que explica **por qué el nuestro sobrevivió tanto**:

> **Un comentario mal fundado se cuestiona; uno bien citado se cree.**

Los tres comentarios de `GeneracionOrdenPagoServiceImpl` no eran vagos: **citaban el §11 de este
documento**. Esa cita es exactamente lo que hacía que el lector siguiente no los dudara. **El
respaldo documental no protege de envejecer — protege de que lo revisen.**

*Corolario operativo, y es barato:* cuando un comentario cite un documento propio, la cita tiene que
poder envejecer con él. Los tres pasaron a apuntar al **§22**, que es donde vive la corrección.

**Y su barrido salió limpio**, lo cual también informa: grepearon `selectVigentesByOrigen` y
`POR_APROBAR` sobre todo `ejb/crd` y ninguno de sus seis comentarios se apoyaba en el borde que
movimos —cinco describen su propio lado y siguen siendo ciertos, y el de
`DevolucionAporteServiceImpl:389` no afirma nada sobre qué estados ve la consulta, así que hoy es
**más** verdadero que antes—. **Salieron bien por casualidad, no por diseño, y lo dicen ellos: no lo
sabían hasta hacer el barrido.**

### §22quinquies — Nota de proceso: esta vez consulté en vez de deducir

Al revisar si el ítem de los comentarios había entrado, el archivo aparecía **modificado y sin
commitear**. Esta mañana (§20.2) deduje de un rastro idéntico que el agente «estaba en el `ng
build`», y estaba **parado** — y le costó un despliegue al usuario.

Hoy usé `ListAgents`: `omen-saa-2-be` figuraba **`busy`**. Sigue trabajando de verdad, así que no
hay nada que reactivar; se espera el reporte.

> **El rastro en el disco es idéntico en los dos casos.** Un agente que abandonó y uno que sigue
> escribiendo dejan exactamente los mismos archivos modificados. **La diferencia sólo se ve
> preguntándole al sistema, no mirando el árbol.** Costó un despliegue aprenderlo y una llamada
> aplicarlo.

---

## §23 — Cuándo duplicar es correcto, y por qué hoy dije lo contrario tres veces

**2026-09-04.** El agente de backend, al reescribir los comentarios del §22ter, encontró que mi
instrucción trataba los tres de forma uniforme **y no lo son**. Tenía razón, y de ahí salió el
matiz que faltaba.

### El hallazgo, verificado

`EstadoPagoProgramado` tiene **exactamente seis** estados (0..5).

| Consulta | Conjunto que devuelve |
|---|---|
| `tienePagoVivoEnBandeja` (rhh, inline): `estado <> RECHAZADO and estado <> ANULADO` | `{0,1,2,3}` |
| `selectVigentesByOrigen` (cxp), **desde hoy**: `in (POR_APROBAR, REGISTRADO, EN_ARCHIVO, CONFIRMADO)` | `{0,1,2,3}` |

**Son idénticos.** La reimplementación de `rhh` quedó **redundante, no divergente** — y el agente no
inventó una distinción que ya no existía, que era el riesgo de mi instrucción.

Los otros dos (`ultimoPagoDeOrigen`, `exigePagoConfirmadoEnTesoreria`) **sí** conservan una razón
real: necesitan ver `RECHAZADO`/`ANULADO`, que la consulta de `cxp` sigue excluyendo.

### La propuesta, y por qué la rechazo

El agente ofreció unificar `tienePagoVivoEnBandeja` para que llame al DAO de `cxp` — un cambio de
una línea. **No se hace.**

Los dos conjuntos son iguales **hoy, por efecto de un cambio que hice yo esta misma tarde**. Lo que
expresan es distinto:

- `selectVigentesByOrigen` es la noción de **`cxp`** de «pago vigente». `cxp` la puede cambiar, y
  **acaba de hacerlo**.
- `tienePagoVivoEnBandeja` es la noción de **`rhh`** de «esta orden ya tiene un pago vivo».

**Si `rhh` llamara al DAO de `cxp`, el próximo ajuste de esa consulta cambiaría el comportamiento de
la nómina en silencio.** Es la misma falla que produjo los comentarios rancios del §22ter, pero un
escalón peor: allá envejeció una explicación, acá envejecería una decisión de negocio.

### El criterio que faltaba, y contradice lo que exigí tres veces hoy

Hoy insistí **tres veces** en lo contrario —«un solo helper compartido, no tres copias»— con
`extraerCodigo`, con el mapa de bloqueantes y con la etiqueta de tipo de comprobante. **Las tres
veces era correcto y ésta también, y no se contradicen: son categorías distintas.**

| | Duplicar es **defecto** | Duplicar es **desacople** |
|---|---|---|
| **Qué es** | Presentación: etiquetas, íconos, formato, resolución de identificador | Un criterio de negocio que **pertenece a otro módulo** |
| **Ejemplos de hoy** | los dos `extraerCodigo`, el mapa de bloqueantes, la etiqueta de `tipoComprobante` | `tienePagoVivoEnBandeja` vs. `selectVigentesByOrigen` |
| **Si las dos copias divergen** | es un **bug**: lo mismo se ve distinto según por dónde pase | es **legítimo**: cada módulo define lo suyo |

> **La prueba, y se hace en una pregunta:** *si estas dos copias empezaran a dar resultados
> distintos, ¿sería un error o una diferencia legítima?* Si es error, hay que unificarlas. Si es
> legítima, unificarlas crea un acoplamiento que va a morder cuando el otro módulo cambie **su**
> definición sin saber que alguien depende de ella.

**Que dos consultas coincidan hoy no las hace la misma consulta.** Coinciden porque nadie las
separó todavía.

**Se deja constancia en el código, no sólo acá:** el comentario nuevo de `tienePagoVivoEnBandeja`
dice explícitamente que hoy el criterio coincide con el del DAO **y que la reimplementación es
deliberada**, para que el próximo que note la redundancia no la «limpie».
