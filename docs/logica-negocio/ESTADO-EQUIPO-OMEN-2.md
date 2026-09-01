# ESTADO — equipo `omen-saa-2`

**Árbitro:** `omen-saa-2-arb` (máquina **omen**) · **Agentes:** `omen-saa-2-be`, `omen-saa-2-fe`
**Creado:** 2026-08-31 · **Reescrito:** 2026-09-01 · **Este documento lo mantiene SOLO este equipo.**

---

## 0. Alcance — definido por el usuario el 2026-09-01

**`rhh` · `cxp` · `pagos` · `cnt` · `tsr`**

⛔ **NO TOCAR: `crd`, `cxc`.**

**Relevo confirmado por el usuario:** este equipo **releva a `omen-saa-3`** en `cxp`, `pagos`, `tsr`
y `rhh`. Ese equipo tenía reserva escrita sobre `cxp/cxc/pagos/tsr/rhh/sri` y ya no está activo.
Se hereda su documento de estado como referencia histórica: `ESTADO-CXP-CXC-TSR-RHH-SRI.md`.

⚠️ **`cxc` y `sri` quedan sin dueño.** Eran de `omen-saa-3`; `cxc` está vedado para este equipo y
`sri` quedó fuera del alcance. Nadie los está trabajando hoy.

**Los equipos paralelos se cerraron el 2026-09-01.** Las reservas de alcance por archivo del §4 de
`REGISTRO-RESERVAS-EQUIPOS.md` dejaron de regir. Verificar qué sesiones siguen vivas antes de
asumir que un archivo tiene dueño ajeno.

### Checkout — decidido por el árbitro el 2026-09-01

**Este equipo trabaja en `F:\work\saaBE\v1\saaBE` y `F:\work\saaFE\v1\saaFE`.**

El §0 de la versión anterior mandaba a `F:\work\equipo2\`, para no compartir working tree con
`omen-saa-1`. Ese motivo desapareció con el cierre de los equipos paralelos, y **`v1` es el
checkout desde el que el usuario despliega por Eclipse**, así que trabajar acá elimina el paso
`push` + `pull` antes de cada despliegue. Los clones de `F:\work\equipo2\` siguen existiendo,
limpios; no se usan.

---

## 1. Frentes activos

| # | Módulo | Frente | Estado |
|---|---|---|---|
| **1** | rhh/tsr | **Pago de décimos acumulados** | 🟠 **FE entregado, BE bloqueado por el V1** (falta la tabla `ODBS`) |
| **1bis** | rhh | Los décimos se generaban también para los mensualizados | ✅ **entregado** (`e3b53ab`) |
| **2** | rhh/tsr | **La nómina pasa por la bandeja de aprobación de TSR** | ✅ **entregado BE+FE** (`bb9bccb`, `7081a8c`) |
| **3-A** | rhh/cnt | Baja de provisión de décimos y fondos de reserva al pagar | 🟠 depende del frente 1 |
| **3-B** | rhh | Baja de provisión de **vacaciones** | ⚪ levantado; **no se implementa** hasta diseñar la marca de lo ya descargado |
| **3-C** | rhh | **Baja de provisión de jubilación patronal y desahucio** | ✅ **entregado** (`54c8cdf`) |
| **4** | rhh | Reporte del Ministerio de Trabajo (SUT) | 🔴 bloqueado — falta el CSV de ejemplo |

**Nada de lo entregado está desplegado.** Todo compila (`mvn -q clean compile` y `ng build`, los dos
exit 0) y está en `origin/main`.

⛔ **Orden de despliegue del frente 2: el frontend PRIMERO, el WAR después.** FE nuevo con WAR viejo
es inofensivo (el REST lee el body como `Map` e ignora la clave de más); WAR nuevo con FE viejo
**rompe `generar()` de nómina**, porque no llegaría el `idUsuario`. Ver §4.2 del diseño.

**Documentos:**
- Diseño: `rhh/PLAN-PAGO-BENEFICIOS-Y-SALIDA-POR-TESORERIA.md`
- Contrato de API: `rhh/API-PAGO-BENEFICIOS-SOCIALES.md`, espejado a `saaFE/docs/rrh/`
- Verificaciones previas: `rhh/sql/e2-01-verificacion-previa-beneficios.sql`

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

## 6. Pendientes del usuario

### 🔴 Bloqueante
1. **Correr `rhh/sql/e2-01-verificacion-previa-beneficios.sql`** y devolver los resultados. Es solo
   lectura. Sin V1 no se puede crear la tabla; sin V5 no se sabe si el frente 1 es preventivo o si
   hay obligación vencida.
2. **Descargar del SUT el CSV de ejemplo** del formulario de decimotercera (y decimocuarta si
   difiere) y dejarlo en `docs/logica-negocio/rhh/muestras/`. El frente 4 está bloqueado sin él.

### 🟡 Decidible
3. Autorizar o no el **modo directo** (despacho por `SendMessage` a `omen-saa-2-be`/`-fe`).
4. Qué se hace con **`cxc` y `sri`**, que quedaron sin dueño.

### ⚪ Sin prisa
5. Confirmar contra la base si `tsr/sql/07` y `08` se ejecutaron.
6. `README-ORDEN.md` para `rhh/sql/` y `cxp/sql/`.
