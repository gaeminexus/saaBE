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

0. **`git fetch` ANTES de reservar.** Reservar contra un checkout viejo no reserva nada: el número
   que ves libre puede estar tomado en `origin` desde hace horas. Necesaria, **pero no suficiente**
   — ver §2b.
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
| 290–309 | 1400–1499 | **Equipo cxp/cxc/pagos/tsr/rhh/sri — `omen-saa-3` (OMEN)** | reservado, **con dueño identificado el 2026-08-31** |
| **310-329** | **1500-1599** | **rhh/cxp/pagos/cnt/tsr — `omen-saa-2` (OMEN)** | reservado 2026-09-01 |
| **330-349** | **1600-1699** | **cxp/cxc/pagos/tsr/rhh/sri — `lap-saa-1` (laptop)** | reservado 2026-09-01, **sin ningún código usado todavía** |
| ≥ 350 | ≥ 1700 | sin asignar | — |

> ⚠️ **Los equipos paralelos NO se cerraron.** `ESTADO-EQUIPO-OMEN-2.md` §0 dice que se cerraron el
> 2026-09-01 y que «`cxc` y `sri` quedan sin dueño, nadie los está trabajando hoy». Era cierto al
> escribirse; dejó de serlo horas después, cuando arrancó **`lap-saa-1`** en la máquina laptop con
> alcance `cxp/cxc/pagos/tsr/rhh/sri`. **Los dos equipos conviven a propósito, por decisión del
> usuario**, que reparte las tareas para que no se pisen. La salvaguarda operativa está en
> `ESTADO-EQUIPO-LAP-1.md` §0.1: **antes de tocar un archivo, `git status` + `git log -3` sobre él;
> si lo modificó otro marcador, parar y avisar.**
>
> **Es la tercera vez que este archivo registra la misma forma de fallo** (el rótulo del bloque
> 290-309, el marcador de commits, y ahora esto): *una afirmación sobre qué equipos existen envejece
> sin que nadie la toque.* Por eso este bloque se rotula con la **sesión y la máquina**, no con los
> módulos.

~~⚠️ **El bloque del otro equipo se reservó sin consultarlo.**~~ **Resuelto el 2026-08-31: ese
bloque es de `omen-saa-3`, y su árbitro lo confirmó con el usuario antes de tomarlo.** Sigue
valiendo lo de fondo: no lo pisen.

> **`saabe-bc` ya no trabaja estos módulos.** Era el equipo `cxp/cxc/pagos/tsr/rhh/sri` en otra
> máquina, y el 2026-08-31 **`omen-saa-3` (OMEN) lo reemplazó**, confirmado por el usuario. Hereda
> su alcance, su documento de estado (`ESTADO-CXP-CXC-TSR-RHH-SRI.md`) y este bloque de rubros.
> **Un `saabe-bc` que aparezca activo a partir de ahora es una sesión vieja, no un equipo vigente.**
>
> Esto corrige una abstención razonable pero basada en información ya vencida: el equipo de
> `F:\work\equipo2` se apartó de este bloque porque el registro lo daba de un tercero activo
> (`ESTADO-EQUIPO-OMEN-2.md` §2). **Hizo lo correcto con lo que sabía** — el bloque no era suyo. Lo
> que faltaba era que el rótulo dijera *quién* es el dueño y no *que es de otro*.
>
> **Lección para este archivo:** una fila que identifica al equipo por sus módulos y no por su
> sesión envejece mal. Cuando el equipo se releva, el rótulo sigue igual y ya no apunta a nadie
> localizable — y entonces el bloque no lo usa su dueño ni lo puede tomar nadie más.

---

## 2b. ⛔ Números de script SQL — rangos por equipo

**Acordado entre los árbitros de los equipos A y B el 2026-08-31, después de chocar dos veces
seguidas** en `docs/logica-negocio/crd/sql/`.

| Rango | Equipo |
|---|---|
| ≤ 95 | histórico (todos) — ocupado |
| **96–149** | **CRD · EQUIPO A** — cobros, contabilidad y jubilados |
| **150–199** | **CRD · EQUIPO B** — ciclo del crédito y seguros |
| ≥ 200 | sin asignar |

### Por qué hizo falta un rango, y no alcanzaba con avisar

El equipo B reservó el script `88` **siguiendo las reglas al pie de la letra** —lo anotó acá antes
de escribirlo— y colisionó con un `88_` del equipo A que ya estaba en `origin`. Se agregó la regla 0
(`git fetch` primero) y se renumeró a `96`. **El `96` también chocó**, esta vez contra un archivo
del equipo A que existía **sin commitear**.

Dos choques seguidos haciendo todo bien. La causa no es descuido:

> **Los números de script se reservan por *existencia de archivo*, y un archivo sin commitear no
> existe para el otro equipo.** No hay `fetch` que lo arregle: lo que todavía no se pusheó es
> invisible por definición. La regla 0 es necesaria y **no es suficiente**.

Con rangos separados deja de importar quién pushea primero, que es la única propiedad que hace
robusto un recurso compartido entre sesiones que no se ven entre sí. Es el mismo remedio que ya se
había aplicado a `PRBR`/`PDTR` en el §2 — solo que a los números de script nadie los había pensado
como catálogo compartido, y lo son.

**Criterio de desempate, si aun así pasa:** el que ya está en `origin` se queda; renumera el otro.
`origin` es el único árbitro no ambiguo.

### Fuera de `crd/sql/`: prefijo por equipo, y el orden en un README

**Acordado entre los árbitros de `omen-saa-2` y `omen-saa-3` el 2026-08-31.** El §2b de arriba
reparte números **sólo** en `docs/logica-negocio/crd/sql/`. Las demás carpetas (`cxp/sql/`,
`cxc/sql/`, `pagos/sql/`, `tsr/sql/`, `rhh/sql/`, `sri/sql/`) no tenían ningún acuerdo.

**El diagnóstico está un paso más atrás que los rangos: el número hacía dos trabajos a la vez y
falló en los dos.** Decía *de quién es el script* y decía *en qué orden se ejecuta*.

| Trabajo | Mecanismo | Por qué |
|---|---|---|
| **De quién es** | **prefijo por equipo** — `e3-`, `e2-`, … | Un rango se agota y hay que renegociarlo; un prefijo no. Y no depende de quién pushee primero, que es lo que hizo fallar dos veces al esquema de rangos |
| **En qué orden va** | **un `README-ORDEN.md` por carpeta** | Es lo único que sobrevive a que dos equipos escriban en la misma carpeta |

**La segunda mitad ya estaba resuelta en el repositorio y nadie la había generalizado:**
`tsr/sql/README-ORDEN-PRODUCCION.md` hace exactamente eso. No hay convención que inventar — hay que
ascender la que ya existe.

**Evidencia de que el número no alcanza, en `rhh/sql/`:** conviven **dos series paralelas** que se
pisan en los mismos números — `01-anticipo-empleado.sql` junto a `01_DDL_TABLAS_PARAMETRIZACION.sql`,
y lo mismo en 02, 03, 04, 05 y 06. No se sobrescribieron por casualidad (distinto separador), pero
**el número dejó de decir en qué orden se ejecuta, que era lo único para lo que servía.**
*(Detectado por el árbitro de `omen-saa-2`; la carpeta es de `omen-saa-3`, que lo toma.)*

**Dos límites deliberados:**

- **Lo histórico no se renumera.** Renumerar un `.sql` ya ejecutado rompe la trazabilidad con lo
  que se corrió en producción. El prefijo aplica de acá en adelante. La excepción es `rhh/sql/`,
  que necesita un `README-ORDEN.md` porque hoy no hay forma de saber el orden — pero se resuelve
  **documentando** el orden real, no renombrando archivos ya corridos.
- **`crd/sql/` queda como está**, con los rangos 96-149 / 150-199 que ya acordaron los equipos A y
  B. Cambiarles el esquema ahora sería pedirles renegociar algo que recién empezó a funcionarles.
  Si algún día se agota un rango, ahí se migra a prefijo.

> **Por qué esta sección la firma `omen-saa-3`:** la propuso el árbitro de `omen-saa-2`, y la
> escribe acá el equipo dueño de las carpetas afectadas. Un acuerdo sobre un recurso compartido lo
> anota quien lo va a usar — si lo anota sólo quien lo propone, nadie verifica que sea aplicable.

---

## 2c. Plantillas contables (`CNT.PLNS`, código alterno `PLNSCDAL`)

**Las 33 vigentes están descritas en `crd/LEVANTAMIENTO-ALIMENTACION-CONTABLE-CREDITOS.md` §7.**
Alternos 1–33 ocupados. **Reservar acá antes de crear una nueva.**

| Alterno | Nombre | Equipo | Estado |
|---|---|---|---|
| 34 | `ENTREGA DE PRESTAMO QUIROGRAFARIO` | **CRD · EQUIPO B** | reservado 2026-08-31, aprobado por el árbitro del equipo A (verificó que él solo consulta las plantillas 21, 25, 27, 28 y 29, sin crear ninguna) |

⚠️ **Trampa al escribir los `DTPLAXL1` de una plantilla nueva** (avisada por el equipo A, ya les costó
un bug en la condonación): de las plantillas de CRD **solo la 21 está renumerada al catálogo
semántico**; la 25, la 27 y la 29 usan auxiliares **posicionales**. **No copies la numeración de otra
plantilla asumiendo que el mismo número significa lo mismo** — verificá qué es cada línea. Un
auxiliar mal mapeado deja el asiento mal clasificado **y cuadrado igual**, o sea que no se detecta.

---

## 2d. Atribución de commits — el mensaje es lo único que distingue equipos

**Acordado entre los árbitros de `omen-saa-1` y `omen-saa-3` el 2026-08-31. Rige de acá en
adelante; nada se reescribe retroactivamente.**

**En esta máquina los tres equipos commitean con la misma identidad de git.** Verificado sobre
cuatro commits de tres equipos distintos — los cuatro dicen `xeonpotato`:

```
836ae6b  xeonpotato  crd: universo fijado con los resultados del 05...   (equipo 2)
9b7e6f5  xeonpotato  docs: la salida del árbol compartido es un clon...  (equipo 1)
ea29ec7  xeonpotato  registro: el bloque PRBR 290-309...                 (equipo 3)
31cc311  xeonpotato  estado equipo 3: verificacion de arranque en OMEN   (equipo 3)
```

Así que `git log --author` **no sirve para separar equipos**, y `%an` es justo la señal que uno usa
por instinto. El árbitro de `omen-saa-3` atribuyó `836ae6b` al equipo equivocado usándola.

### La regla

**Marcador de equipo en el prefijo de TODOS los commits**, no sólo en los de coordinación:

```
crd(eqB): ...      cxp(eq3): ...      registro(eq3): ...      docs(eq2): ...
```

| Equipo | Marcador |
|---|---|
| CRD · EQUIPO A (`saabe-25`) | `eqA` |
| CRD · EQUIPO B (`omen-saa-1`) | `eqB` |
| `omen-saa-2` | `eq2` |
| cxp/cxc/pagos/tsr/rhh/sri (`omen-saa-3`) | `eq3` |
| cxp/cxc/pagos/tsr/rhh/sri (`lap-saa-1`, **laptop**) | `lap1` |

**Prefijo de scripts `.sql` de `lap-saa-1`: `lap1-`**, según el §2b (prefijo por equipo fuera de
`crd/sql/`). Se eligió `lap1` y no `eq1` a propósito: nombra la **máquina**, que es lo que de verdad
separa a este equipo de los tres de OMEN, y no puede confundirse con el `omen-saa-1` histórico —
que además ya usa `eqB`.

### Por qué en todos, y no sólo donde hay ambigüedad

La primera versión de esta regla decía que el prefijo de módulo ya atribuye solo, y que el marcador
hacía falta únicamente en los commits transversales (`docs:`, `registro:`). **Tiene un agujero, y
está en el módulo más caliente:** el equipo A y el equipo B trabajan los dos `crd` y los dos
prefijan `crd:`. Sus commits son indistinguibles por prefijo **y** por autor. No es hipotético —
los dos equipos colisionaron dos veces en `crd/sql/` el mismo día, y las dos veces la pregunta
inmediata fue *«¿de quién es este archivo?»*, que hubo que resolver leyendo el contenido.

La variante intermedia —marcador obligatorio *sólo* donde el módulo tenga más de un equipo— se
descartó por una razón concreta: **exige saber cuántos equipos hay en un módulo hoy, y eso cambia.**
Es el mismo defecto que hizo fallar el rótulo del bloque `PRBR 290-309` en el §2 (identificar por
módulos en vez de por sesión envejece mal) y el mismo que hace fallar los rangos de script cuando
un archivo todavía no está en `origin`. **Una regla que depende de un estado que se mueve, se
rompe cuando ese estado se mueve.** Seis caracteres fijos no dependen de nada.

✅ **Los cuatro equipos adheridos al 2026-08-31.** El equipo A (`saabe-25`) confirmó tras verificar
la premisa por su cuenta: los últimos 12 commits de `origin/main` son **todos** de `xeonpotato`, y
ya hay commits con prefijo `crd:` que no son suyos. La ambigüedad no era hipotética, ya existía.

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
| 2026-08-31 | CRD · EQUIPO B (`omen-saa-1`) | **Número de script `crd/sql/150`** — ningún `PRBR`/`PDTR` | `150_BACKFILL_PRSTINNM_DESDE_PRSTTSAA.sql` (defecto D10). ⚠️ **Nació como `88_` y colisionó** — ver la fila de abajo. ~~Cambia la mora nocturna~~ — **medido contra la base el 2026-08-31: el `UPDATE` no toca ninguna fila** (5.657 de 5.664 préstamos ya tienen `PRSTINNM` correcto). Aviso al equipo A **rebajado**. Y **el frente de otorgamiento NO va a necesitar tabla nueva de 4 letras**: `CRD.PRST` ya trae todo el ciclo (aprobación, rechazo, legalización, acreditación) y `CRD.CRDT` los rangos de aprobación — ver `crd/REVISION-MOTOR-ANTES-DE-OTORGAMIENTO.md` §4. El rango 270-289 / 1300-1399 **queda libre** para el frente de seguros |
| 2026-08-31 | CRD · EQUIPO A (`saabe-4b`) | PDTR **1180**, rubro 235 **alterno 8** | `EXCEDENTE_PETRO` — script `crd/sql/87`. ⚠️ **Va en el alterno 8 porque el 7 ya está tomado por `JUBILACION`** (script 81, PDTR 1178, escrito y **sin correr**). El agente lo propuso en el 7 sin saberlo: el 81 no está en la base todavía, así que consultarla no alcanzaba — **este registro es la única fuente que lo evitaba** |
| 2026-08-31 | CRD · EQUIPO A (`saabe-25`) | Tabla **`CTAP`** — cuentas contables por tipo de aporte | Devolución de aportes, opción C. Verificado libre en `src/main/java/com/saa/model/`; **falta confirmarlo contra `ALL_TABLES`** antes de ejecutar. DDL en `crd/sql/94` |
| 2026-08-31 | CRD · EQUIPO A (`saabe-25`) | Tabla **`PGPC`** — pago de pensión complementaria | Frente jubilados. Bitácora mes a mes del pago de pensión; `UNIQUE (ENTDCDGO, año, mes)` para que el proceso sea idempotente **por diseño**, no por un chequeo en Java. Libre en `model/`; **confirmar contra `ALL_TABLES`** antes de crear |
| 2026-08-31 | CRD · EQUIPO A (`saabe-25`) | PDTR **1200**, rubro 235 **alterno 9** | `PAGO_PENSION` — el descuento mensual de la pensión complementaria. **Va aparte de `JUBILACION` (alterno 7)**: ese es el traslado inicial, único e irrepetible; este es un movimiento mensual recurrente. Mezclarlos bajo el mismo tipo haría imposible distinguir el traslado de los pagos al consultar el histórico. **Primer código del rango 1200-1299 del equipo A** |
| 2026-08-31 | CRD · EQUIPO A (`saabe-25`) | **Número de script `crd/sql/97`** | `97_PAGO_PENSION_COMPLEMENTARIA.sql` (`CRD.PGPC` + PDTR 1200). ⚠️ **Nació como `96_` y volvió a colisionar** con el `96_` del equipo B — que a su vez venía de un `88_` colisionado. **Segunda colisión seguida, y las dos partes habían cumplido las reglas 0, 1 y 2.** El mío se corrió porque el de ellos llegó primero a `origin`, que es el único árbitro no ambiguo. **La causa de fondo no la cubre ninguna regla actual: un script sin commitear es invisible, así que reservar el número no alcanza si el archivo del otro todavía no está en `origin`.** Propuesta al otro árbitro, pendiente de su ok: **rangos de numeración separados — equipo A 97-149, equipo B 150-199** — para que deje de importar quién pushea primero |
| 2026-08-31 | cxp/cxc/pagos/tsr/rhh/sri (árbitro `omen-saa-3`) | **`PDTR` 1400** — rubro **175** (`CXP_ESTADO_DOCUMENTO_CXP`), valor **7** | `ANULADO` en el ciclo de vida de `PGS.DCXP`. El rubro 175 ya existe con LEIDO(1) … REVERTIDO(6); esto agrega **sólo un detalle**, no un rubro nuevo. **Primer código usado del bloque 1400-1499.** ⚠️ **No se reusó `REVERTIDO(6)`**: ese significa «los registros destino se borraron», y en la anulación la factura **sigue existiendo**, anulada. Mezclarlos haría imposible distinguir los dos casos al consultar. Script en `cxp/sql/e3-01-estado-anulado-documento-cxp.sql` |
| 2026-08-31 | cxp/cxc/pagos/tsr/rhh/sri (árbitro `omen-saa-3`) | **Bloque `PRBR` 290-309 / `PDTR` 1400-1499** — ningún código concreto todavía | **Identificación de dueño, no reserva nueva.** El bloque ya estaba reservado desde el 2026-08-30 para «el equipo cxp/cxc/tsr/rhh/sri», sin decir qué sesión era. `omen-saa-3` reemplaza a `saabe-bc` desde el 2026-08-31 (confirmado por el usuario) y hereda alcance, documento de estado y este bloque. Se anota **antes** de usar ningún número, según la regla 1 |
| 2026-09-01 | rhh/cxp/pagos/cnt/tsr (árbitro `omen-saa-2`) | **`PRBR` 310** + **`PDTR` 1500–1503** | Rubro nuevo `RHH_ESTADO_ORDEN_BENEFICIO` — `GENERADA(1)`, `ENVIADA_A_TESORERIA(2)`, `PAGADA(3)`, `ANULADA(4)` — para la orden de pago de beneficios sociales (`RHH.ODBS`). **Primeros códigos del bloque 310-329 / 1500-1599**, que este equipo venía proponiendo desde el 2026-08-31 **sin escribirlo**, o sea que hasta hoy no estaba reservado. Control corrido por el usuario justo antes de reservar (regla 2): `MAX(PRBRCDGO)`=**248** y `MAX(PDTRCDGO)`=**1200**, los dos por debajo del bloque, sin colisión. Script `rhh/sql/e2-03`. Se reserva también la tabla **`ODBS`**: verificada libre en `src/main/java/com/saa/model/`, en `docs/`, y **contra `ALL_TABLES` por el usuario el 2026-09-01** — cero filas |
| 2026-09-01 | cxp/cxc/pagos/tsr/rhh/sri (árbitro `lap-saa-1-arb`, **laptop**) | **Bloque `PRBR` 330-349 / `PDTR` 1600-1699** — ningún código concreto todavía. Marcador de commit **`lap1`**, prefijo de scripts **`lap1-`** | Alta del equipo. Se anota **antes** de usar ningún número, según la regla 1. **Se aplica desde ya la §6:** cuando este equipo tome un `PRBRCDGO` va a anotar **también el `PRBRALTR`**, con la convención `PRBRALTR = PRBRCDGO`, y a correr los dos controles. El `MAX` se revalida con el usuario justo antes de ejecutar (regla 2) — el último control conocido, del mismo día, dio `MAX(PRBRCDGO)`=**248** y `MAX(PDTRCDGO)`=**1200**, los dos muy por debajo de este bloque |

---

## 6. ⛔ El registro reserva `PRBRCDGO`, pero el código busca por `PRBRALTR`

**Encontrado el 2026-09-01 por el árbitro de `omen-saa-2`, escribiendo su primer rubro.**

Este archivo lleva el control de `PRBRCDGO` y `PDTRCDGO` desde el 2026-08-30, y en ninguna de sus
316 líneas menciona **`PRBRALTR`** — el **código alterno**. Pero es el alterno el que usa el código
para encontrar un rubro: `DetalleRubroDaoService.selectValorStringByRubAltDetAlt` filtra por
`t.rubro.codigoAlterno`, y es el camino por el que `selectByCriteria` del DAO genérico lee los
operadores de búsqueda.

**El agujero:** dos equipos pueden tomar `PRBRCDGO` distintos —cumpliendo este registro al pie de
la letra— y **el mismo `PRBRALTR`**. La colisión no la ve ninguna de las reglas de arriba, y no
falla al insertar: falla al **leer**, devolviendo el rubro del otro equipo.

**Y no se puede deducir el alterno del código, porque la convención cambió:**

| Ejemplo | `PRBRCDGO` | `PRBRALTR` |
|---|---|---|
| Rubros viejos de rhh (180-198) | 180 | **179** — alterno = código − 1 |
| Rubros recientes (234) | 234 | **234** — alterno = código |

**Regla, de acá en adelante:** al reservar un `PRBRCDGO` en la bitácora, **anotar también el
`PRBRALTR`**, y verificar los dos antes de ejecutar:

```sql
SELECT PRBRCDGO FROM SCP.PRBR WHERE PRBRCDGO = <codigo>;   -- el que reserva este archivo
SELECT PRBRCDGO, PRBRDSCR FROM SCP.PRBR WHERE PRBRALTR = <alterno>;  -- el que usa el codigo
```

**Convención recomendada para lo nuevo: `PRBRALTR` = `PRBRCDGO`.** Es la que siguen los rubros
recientes, hace innecesario reservar dos números y vuelve la colisión imposible mientras todos la
respeten. Lo histórico no se toca.

> **Cómo se encontró, porque la forma se repite:** el árbitro escribió el `INSERT` copiando la
> estructura de otro script **sin contrastarla contra la entidad `Rubro`**, e inventó dos columnas
> (`PRBRNMBR`, `PRBRESTD`) que no existen — el script habría fallado con ORA-00904 al ejecutarse.
> Lo detectó el agente de backend, que **no puede correr SQL**, preguntando por qué el `INSERT` no
> llevaba `PRBRALTR` si el catálogo se consulta por ahí. Verificar el esquema antes de escribir un
> `INSERT` es más barato que descubrirlo en producción, y **la pregunta de alguien que no puede
> ejecutar sigue siendo verificación.**

---

## 6. Deudas transversales — afectan a TODOS los módulos, y ninguna tiene dueño

**Abierta el 2026-09-01 por el árbitro de `omen-saa-1`, a pedido del de `omen-saa-2`.** Van acá y no
en el tablero de un equipo porque **no son de un módulo**: el que las encuentra tiene el diagnóstico,
y el que las sufre después suele ser otro. Cada equipo referencia esta sección en vez de duplicarla.

**Ninguna de las tres se arregla desde un módulo.** Están listadas para que nadie las "arregle" solo
y para que nadie pierda medio día rediagnosticándolas.

### 6.1 ⛔ `handleError`: un fallo de consulta se lee como «no hay datos» — 316 servicios del frontend

```ts
private handleError(error: HttpErrorResponse): Observable<null> {
  if (+error.status === 200) { return of(null); }
  else { return throwError(() => error.error); }
}
```

`grep -rln "if (+error.status === 200)" src/app` → **316 archivos** en `saaFE`.

Un `HttpErrorResponse` con status 200 es un **fallo de parseo** del cuerpo. Ese caso **no llega como
error de RxJS**: llega como emisión **exitosa** con valor `null`, y el consumidor lo colapsa a `[]`
con el idiomático `Array.isArray(x) ? x : x ? [x] : []`. **No pasa por ningún `catchError`, no deja
rastro en consola, y la pantalla dice "no hay datos".**

> **Por qué importa más que un bug normal:** no produce un error, produce **un diagnóstico
> envenenado**. «La consulta falló» y «no hay resultados» se vuelven indistinguibles desde la silla
> del operador **y desde la del que depura**. Le costó al equipo `omen-saa-1` medio día y **tres
> diagnósticos, dos de ellos equivocados**, sobre un préstamo que no aparecía en una lista —cada uno
> gastó una medición contra producción para descartarse—.

**Cómo se trata, y cómo NO:**

- ✅ **Distinguir `null` de `[]` en el punto de consumo**, en la pantalla afectada. `null` significa
  "no hubo respuesta utilizable"; `[]` significa "no hay filas". Precedente funcionando:
  `deudaConsultaFallida` en el diálogo de devolución de aportes, y
  `detalle-consulta-carga.component.ts` (commit `10142d5` de `saaFE`).
- ⛔ **No tocar `handleError`.** Son 316 archivos: es un cambio que **nadie puede revisar**, y el
  comportamiento actual puede estar sosteniendo pantallas que nadie va a probar.

Verificado en dos alcances distintos: `crd` (`omen-saa-1`) y `rhh` (`omen-saa-2`,
`orden-pago-nomina.service.ts:105`).

### 6.2 ⛔ `EntityDaoImpl.save()` hace `em.merge()` desnudo: un `PUT` parcial graba `NULL`

**Aportado por el árbitro de `omen-saa-2`.** `save()` mergea el objeto **tal como llegó del JSON**,
sin releer la fila y sin saltar nulos. Como **ningún campo persistido del modelo es primitivo**, una
clave ausente en el JSON se graba `NULL` — **FKs incluidas**. Un `PUT` que arma el payload campo por
campo **borra en silencio todo lo que no copió**.

> **El patrón para encontrarlo, que es lo más útil de este hallazgo:** *se cae siempre el campo que
> **no tiene control en el formulario***, porque el payload se arma enumerando lo editable en vez de
> partir de la entidad leída.

Medido en `rrh`: de **11 escrituras del módulo, 5 tienen el defecto**; el peor devuelve a
«solicitado» un permiso ya aprobado. Detalle en `ESTADO-EQUIPO-OMEN-2.md` §7.

- ✅ **Leer la entidad y sobrescribir solo lo editable** antes de mandar el `PUT`. Precedente:
  `guardarEdicion()` en `prestamo-edit` (`crd`).
- ⛔ **No tocar `EntityDaoImpl`:** hereda de él **todo el proyecto**.

Documentado también en `docs/general/MERGE-DESNUDO-EN-ENTITYDAOIMPL.md`.

### 6.3 Los errores del REST NO llegan como texto plano, aunque el endpoint los escriba así

**Aportado por el árbitro de `omen-saa-2`; ya costó una lectura de contrato equivocada a dos
equipos.** `com.saa.ws.rest.MensajeErrorJsonFilter` es un `@Provider` **global** que envuelve toda
respuesta **≥ 400** cuya entidad sea un `String` y cuyo tipo declarado sea JSON, y la entrega como:

```json
{ "mensaje": "Error al aprobar el préstamo: ..." }
```

Así que el `.entity("Error ...: " + e.getMessage())` que usa medio proyecto **no llega como texto
plano al cliente**. El filtro no envuelve dos veces lo que ya empieza con `{` o `[`.

**Al escribir un contrato de API:** decir que el error viene como `{"mensaje": ...}`, y que el
cliente lea `error.mensaje` con el texto crudo como respaldo. **Un contrato que diga "texto plano"
es incorrecto** — `API-CICLO-OTORGAMIENTO.md` lo decía y se corrigió el 2026-09-01.

### 6.4 ⛔ Una búsqueda sin resultados lanza excepción — 255 servicios del backend

```java
if (result.isEmpty()) {
    throw new IncomeException("Busqueda por criterio X no devolvio ningun registro");
}
```

`grep -rl "if (result.isEmpty())" saaBE/src/main/java/com/saa/ejb` → **255 archivos**.

Es la convención de la casa y `CLAUDE.md` la documenta. El REST la convierte en `400`, y el
cliente recibe **un error donde hubo una respuesta perfectamente válida: "no hay filas"**.

> **Por qué es la más costosa de las cuatro.** Las otras tres hacen que **un fallo parezca dato**.
> Ésta hace lo inverso: **un dato normal parece un fallo**, y se propaga hasta la pantalla.

**Costó tres días de diagnóstico en producción (2026-09-02, equipo `omen-saa-1`).** En la pantalla
de descuentos de Petro, un préstamo cuya consulta de pagos devolvía vacío **desaparecía de la
lista de afectables**. O sea: *un préstamo en mora que nunca recibió un pago no podía aparecer
nunca* — y como los préstamos vigentes normalmente sí tienen pagos, el síntoma se leía como «solo
salen los vigentes». Se persiguieron dos causas equivocadas antes de encontrar ésta.

**Cómo se trata, y cómo NO:**

- ✅ **En el consumidor**, tratar ese error específico como lista vacía. Precedente: `9343c43` de
  `saaFE`. **Es un puente, no el destino**: distingue por el *texto* del mensaje, que se rompe si
  alguien lo reescribe.
- ⛔ **No tocar los 255 servicios.** Y no cambiar la convención desde un módulo: hay código que
  puede estar dependiendo del `throw` para su control de flujo.
- El arreglo real es que `selectByCriteria` devuelva lista vacía y que el `throw` quede solo donde
  «sin filas» sea de verdad un error. **Necesita decisión de proyecto, no de equipo.**

⚠️ **Y ojo con el atajo:** un `try/catch` que se trague el error en el consumidor reintroduce 6.1 —
volvería a hacer indistinguible «no hay filas» de «la consulta se rompió».

---

## 7. ⛔ No mergear a `main` un mapeo cuya columna no está en la base

**Formulado por el árbitro de `lap-saa-1` el 2026-09-02, escrito acá por el de `omen-saa-2` porque
los dos equipos lo vamos a aplicar. Nació de un caso concreto que se frenó a tiempo.**

### La regla

> **Antes de mergear a `main` una entidad que mapea una columna nueva, la columna tiene que existir
> en la base.** No alcanza con tener el `.sql` escrito, ni con planear correrlo antes del propio
> despliegue.

### Por qué «el DDL va antes del WAR» no es suficiente

Esa regla ya está en todos los documentos de paso a producción, y **se piensa como una secuencia
dentro de un equipo**: *yo* corro el DDL, después *yo* despliego. Funciona mientras haya un solo
equipo.

Con varios equipos sobre el mismo `main`, deja de funcionar, y la razón es que **`main` es un
insumo compartido: cualquiera puede construir un WAR desde ahí en cualquier momento.**

> **Mergear código que mapea columnas inexistentes es publicar una bomba con temporizador ajeno: no
> explota cuando la despliega quien la escribió, explota cuando despliega cualquiera.**

Y el daño no es que falte la función nueva. Hibernate incluye toda columna `@Column` y `@JoinColumn`
en el `SELECT` que genera, así que **una columna mapeada que no existe rompe cualquier lectura de esa
entidad con ORA-00904** — la entidad entera y las pantallas que la usan.

### El caso que la originó, 2026-09-02

`lap-saa-1` tenía lista la rama `lap1/cruce-anticipo-liquidacion` (`7595751`), que mapea `APLPLQCC`
y `LQCCEPAG`, con su DDL (`cxp/sql/lap1-10`) **sin correr**. Compilaba limpio — lo verificó
`omen-saa-2` en un worktree. **La compilación no ve este riesgo.**

Al mismo tiempo, el usuario de `omen-saa-2` estaba por desplegar un WAR por un frente **de otro
módulo**. Si el merge hubiera entrado primero, ese despliegue se habría llevado dos entidades de
`cxp` rotas, sin que nadie de los dos equipos hubiera hecho nada mal según las reglas vigentes.

**Se frenó el merge, y el usuario de `lap-saa-1` corrió el DDL primero.** Con las columnas en base,
el orden de despliegue deja de importar para todos — que es la propiedad que buscamos.

### Por qué es del mismo tipo que el §2b

El §2b decía que **lo que no está en `origin` no existe para el otro equipo**. Éste dice lo
complementario: **lo que sí está en `origin` existe para todos, aunque no esté listo para todos.**

> **El árbitro de `lap-saa-1` lo diagnosticó así, y conviene citarlo:** *«estaba tratando la rama
> como si el riesgo terminara en la compilación, y el riesgo real empieza después. Yo pensaba en mi
> secuencia de despliegue; el otro veía la suya. Ninguno de los dos tenía la foto completa solo.»*
> Es el control y lo controlado compartiendo origen, otra vez: verificó su rama contra su propio
> plan de despliegue, que es la hipótesis con la que la escribió.

### En la práctica

| Situación | Qué hacer |
|---|---|
| Tengo el DDL escrito y sin correr | **No mergeo.** Pido que se corra primero |
| El DDL ya corrió en la base | Mergeo cuando quiera; el orden deja de importar |
| El cambio no mapea columnas nuevas | Sin restricción |
| No estoy seguro de si corrió | Consultar `ALL_TAB_COLUMNS`. Es una consulta, no una suposición |

**Un DDL aditivo y nullable es inofensivo para el WAR que ya está corriendo** —nadie lee esas
columnas todavía— así que correrlo antes no tiene costo ni requiere ventana.
