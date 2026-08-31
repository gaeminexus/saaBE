# Registro de reservas — códigos y nombres compartidos

**Creado:** 2026-08-30 · **Lo leen y lo escriben TODOS los árbitros, de todos los equipos.**

> **Por qué existe.** Los catálogos (`SCP.PRBR`, `SCP.PDTR`) y los nombres de tabla de 4 letras son
> **recursos globales**. Con varios equipos trabajando a la vez, dos árbitros pueden asignar el
> mismo código sin enterarse — y no se nota hasta que el `INSERT` falla en producción.
>
> **Ya pasó dos veces:**
> - Se dio por libre el `PDTRCDGO` 1151 y estaba tomado por las partidas en tránsito del otro equipo.
> - Se propuso la tabla `CBRO` para cobros de crédito y ya existía `TSR.CBRO`. Terminó siendo `CBCR`.

---

## 1. Las tres reglas

1. **Antes de usar un código, reservalo acá** — editá este archivo primero, después escribí el script.
2. **Volvé a correr el control de `MAX` justo antes de ejecutar.** El rango reservado dice qué te
   corresponde; el `MAX` real dice qué hay. Si no coinciden, **parar y avisar**, nunca forzar.
3. ~~**Después de insertar claves explícitas, sincronizá la secuencia.**~~ **REGLA DEROGADA el
   2026-08-31 — protegía contra algo que no puede pasar. Ver §1bis.**

```sql
-- Control obligatorio antes de ejecutar cualquier script que inserte rubros
SELECT MAX(PRBRCDGO) AS MAX_PRBR FROM SCP.PRBR;
SELECT MAX(PDTRCDGO) AS MAX_PDTR FROM SCP.PDTR;
```

---

## 1bis. ⛔ La regla de la secuencia era falsa — verificado el 2026-08-31

La regla 3 decía que insertar PKs explícitas sin sincronizar `SQ_PRBRCDGO`/`SQ_PDTRCDGO` haría que
*"el próximo rubro creado desde la aplicación muera por PK duplicada"*. **Las dos mitades de esa
frase son falsas**, y se comprobó consultando la base **conectado como `SYS`** (o sea: no es un
problema de visibilidad, es el estado real):

1. **`SCP.SQ_PDTRCDGO` y `SCP.SQ_PRBRCDGO` no existen.** No en `SCP` ni en ningún otro schema. La
   única secuencia que tiene `SCP` es `SQ_PADTUSRO`.
2. **La aplicación no crea rubros ni detalles de rubro.** `DetalleRubroRest` (`@Path("pdtr")`)
   expone **solo dos `@GET`**: `getAll` y `getRubros/{idRubro}`. No hay `@POST`, no hay `@PUT`, y no
   existe un `RubroRest`. Los catálogos se cargan **únicamente por script**.

Las entidades `Rubro` y `DetalleRubro` sí declaran
`@GeneratedValue(strategy = SEQUENCE, generator = "SQ_PRBRCDGO"/"SQ_PDTRCDGO")` — un mapeo que
**nunca se ejercita**, y por eso nadie notó que las secuencias no están.

**Qué hacer con esto: nada.** No crear las secuencias. Serían infraestructura que nadie usa, y
elegirles un valor de arranque equivocado sí introduciría el problema que la regla imaginaba. El
mapeo latente es inofensivo y además **falla ruidoso**: el día que alguien agregue un `@POST` a
`DetalleRubroRest`, revienta con `ORA-02289` en la primera prueba, no en silencio.

**Lo que sí sigue valiendo, y es lo que de verdad importa:** las reglas 1 y 2 — reservar el código
acá antes de escribir el script, y volver a correr el control de `MAX` justo antes de ejecutar. El
riesgo real nunca fue la secuencia: era **dos árbitros asignando el mismo código sin enterarse**, y
eso ya pasó dos veces.

> **La lección, que vale más que el hallazgo:** esta regla venía citada en scripts, en prompts y en
> la bitácora de abajo, y nadie había verificado el mecanismo que decía proteger. Una regla
> documentada no es evidencia de que el mecanismo exista.

---

## 2. Estado al 2026-08-30

**Último usado:** `PRBRCDGO` = **248** · `PDTRCDGO` = **1178**

| Rango PRBR | Rango PDTR | Equipo | Estado |
|---|---|---|---|
| ≤ 248 | ≤ 1178 | histórico (todos) | ocupado |
| 249 | 1179–1199 | **libre — colchón**, no reservar | — |
| 250–269 | 1200–1299 | **CRD · EQUIPO A — Cobros, contabilidad y jubilados** | reservado |
| 270–289 | 1300–1399 | **CRD · EQUIPO B — Ciclo del crédito y seguros** | reservado |
| 290–309 | 1400–1499 | **Equipo cxp/cxc/tsr/rhh/sri** | reservado para el otro equipo |
| ≥ 310 | ≥ 1500 | sin asignar | — |

⚠️ **El bloque del otro equipo se reservó sin consultarlo.** Si ya venían usando otros números,
avisen y se ajusta — pero **no lo pisen**: es el mismo error que este archivo existe para evitar.

---

## 3. Nombres de tabla de 4 letras

**El código de 4 letras es único en TODO el proyecto, no por esquema.** Verificar antes de
proponerlo, contra Java y contra la base:

```sql
SELECT t.OWNER, t.TABLE_NAME FROM ALL_TABLES t WHERE t.TABLE_NAME = 'XXXX';
```
```bash
grep -rn 'name = "XXXX"' src/main/java/com/saa/model/
```

### Reservados

| Código | Tabla | Equipo | Estado |
|---|---|---|---|
| `ACCN` | Acuerdo de condonación | CRD | creada |
| `DACC` | Detalle de acuerdo | CRD | creada |
| `CBCR` | Cobro de crédito | CRD | creada |
| `DCBC` | Detalle de cobro | CRD | creada |
| `TRCR` | Transferencia de carga | CRD | creada |
| `ANCP` | Asiento por sub-proceso Petro | CRD | creada |
| `CRTF` | Certificado de crédito | CRD | creada |

### Propuestos para los frentes nuevos — **verificar antes de usar**

Ninguno está confirmado. Cada árbitro corre las dos verificaciones de arriba antes de fijarlo.

| Frente | Idea de códigos |
|---|---|
| Jubilados | jubilación del partícipe, detalle de la liquidación |
| Seguros | póliza, inscripción de préstamo en póliza (solo incendio), aseguradora |
| Ciclo del crédito | solicitud de crédito, otorgamiento/desembolso, reestructuración aplicada |
| Contabilidad | probablemente ninguna nueva: reusa `CBCR`/`DCBC` |

---

## 4. Archivos con dueño exclusivo

Un archivo que dos equipos editan a la vez es un conflicto silencioso. Estos tienen dueño:

| Archivo | Dueño | El otro equipo |
|---|---|---|
| `CobroCreditoServiceImpl`, `ProcesoPagoPrestamoServiceImpl`, `AporteServiceImpl`, `DevolucionAporteServiceImpl` | **EQUIPO A** | solo lectura |
| FE: `forms/cobros-personales/*`, `forms/cruce-de-valores/*`, `dialog/pagos/*`, `forms/entidad-participe/jubilados/*` | **EQUIPO A** | solo lectura |
| `CalculadoraAmortizacionServiceImpl`, `SimulacionPrestamoServiceImpl`, `PrestamoServiceImpl` | **EQUIPO B** | solo lectura |
| FE: `forms/simulador-*`, `forms/asignacion-seguros/*` | **EQUIPO B** | solo lectura |
| FE: `forms/prestamo/*` | **EQUIPO B** (desde 2026-08-31) | solo lectura. Es la pantalla del otorgamiento: alta del préstamo y generación de la tabla de amortización |
| FE: `service/prestamo.service.ts` | **compartido** | lo usan las pantallas de los dos equipos. `git status` antes de tocar, y **modificar solo el método propio**, nunca el archivo entero |
| `com.saa.ejb.cnt`, `com.saa.model.cnt`, `docs/logica-negocio/cnt/` | **compartido, también con el equipo cxp/tsr** | `git status` antes de tocar, y avisar |

**Si necesitás un cambio en un archivo ajeno: pedíselo a su dueño.** No lo edites y avises después.

---

## 5. Bitácora de reservas

Agregá una línea cada vez que reserves algo. Fecha, equipo, qué, para qué.

| Fecha | Equipo | Reservado | Para |
|---|---|---|---|
| 2026-08-30 | CRD (árbitro `saabe-4b`) | PDTR 1178 | `JUBILACION` en el rubro 235 (tipo de movimiento de aporte) — script `crd/sql/81` |
| 2026-08-30 | CRD (árbitro `saabe-4b`) | PDTR **1179** — del colchón, no del rango del equipo 4 | `COBRO_MIXTO` en el rubro 245 (tipo de operación de cobro) — script `crd/sql/83`. Un depósito que se reparte entre aportes y varios préstamos: **un depósito = un cobro = una aprobación = un reverso** |
| 2026-08-31 | CRD · EQUIPO B (`omen-saa-1`) | **Número de script `crd/sql/88`** — ningún `PRBR`/`PDTR` | `88_BACKFILL_PRSTINNM_DESDE_PRSTTSAA.sql` (defecto D10). ~~Cambia la mora nocturna~~ — **medido contra la base el 2026-08-31: el `UPDATE` no toca ninguna fila** (5.657 de 5.664 préstamos ya tienen `PRSTINNM` correcto). Aviso al equipo A **rebajado**. Y **el frente de otorgamiento NO va a necesitar tabla nueva de 4 letras**: `CRD.PRST` ya trae todo el ciclo (aprobación, rechazo, legalización, acreditación) y `CRD.CRDT` los rangos de aprobación — ver `crd/REVISION-MOTOR-ANTES-DE-OTORGAMIENTO.md` §4. El rango 270-289 / 1300-1399 **queda libre** para el frente de seguros |
| 2026-08-31 | CRD · EQUIPO A (`saabe-4b`) | PDTR **1180**, rubro 235 **alterno 8** | `EXCEDENTE_PETRO` — script `crd/sql/87`. ⚠️ **Va en el alterno 8 porque el 7 ya está tomado por `JUBILACION`** (script 81, PDTR 1178, escrito y **sin correr**). El agente lo propuso en el 7 sin saberlo: el 81 no está en la base todavía, así que consultarla no alcanzaba — **este registro es la única fuente que lo evitaba** |
| 2026-08-31 | CRD · EQUIPO A (`saabe-25`) | Tabla **`CTAP`** — cuentas contables por tipo de aporte | Devolución de aportes, opción C. Verificado libre en `src/main/java/com/saa/model/`; **falta confirmarlo contra `ALL_TABLES`** antes de ejecutar. DDL en `crd/sql/94` |
