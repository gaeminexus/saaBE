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
| ≥ 310 | ≥ 1500 | sin asignar | — |

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

⚠️ **El equipo A no participó de este acuerdo** y venía commiteando sin marcador. Se le avisó el
2026-08-31. Hasta que confirme, sus commits se identifican como hasta ahora.

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
| 2026-08-31 | cxp/cxc/pagos/tsr/rhh/sri (árbitro `omen-saa-3`) | **Bloque `PRBR` 290-309 / `PDTR` 1400-1499** — ningún código concreto todavía | **Identificación de dueño, no reserva nueva.** El bloque ya estaba reservado desde el 2026-08-30 para «el equipo cxp/cxc/tsr/rhh/sri», sin decir qué sesión era. `omen-saa-3` reemplaza a `saabe-bc` desde el 2026-08-31 (confirmado por el usuario) y hereda alcance, documento de estado y este bloque. Se anota **antes** de usar ningún número, según la regla 1 |
