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

## 0. Índice — qué reglas contiene este archivo

> ## ⛔ La línea del índice se escribe PRIMERO. Después la sección.
>
> **No al revés, y no «después me acuerdo».** Es la **regla 1 de este archivo aplicada a sí mismo**
> —*«antes de usar un código, reservalo acá; después escribí el script»*— y funciona por el mismo
> motivo: así el índice **deja de ser documentación sobre el archivo y pasa a ser el acto de
> reservar el lugar**. Si escribiste la sección sin la línea, te salteaste el paso 1.
>
> **Por qué se insiste tanto:** un índice mantenido a mano envejece igual que todo lo demás acá
> —este archivo ya registra tres veces la misma forma de fallo—, y **un índice incompleto es peor
> que ninguno**: miente por omisión, y miente en la dirección peor. Quien lo consulte va a concluir
> que la regla no existe y **la va a escribir de nuevo**, que es exactamente el problema que este
> índice viene a resolver.
>
> **Y los títulos nombran el CONCEPTO, no el incidente.** «Lo que pasó con `PGCE` el 03-09» sólo lo
> encuentra quien ya sabe qué pasó; «reservar un nombre ≠ autorizar la tabla» lo encuentra el que
> viene a hacer justamente eso.

| § | Qué regla vive ahí |
|---|---|
| **1** | Las tres reglas: `fetch` primero, reservar acá antes de usar, revalidar el `MAX` antes de ejecutar |
| **1bis** | Las secuencias `SQ_PRBRCDGO` / `SQ_PDTRCDGO` **no existen**: la regla de sincronizarlas era falsa |
| **2** | Qué bloque de `PRBR` / `PDTR` tiene cada equipo |
| **2b** | Números de script `.sql`: rangos dentro de `crd/sql/`, prefijo por equipo fuera de ahí |
| 2b ▸ | **Un archivo sin commitear es invisible para el otro equipo** — reservar el número no alcanza |
| 2b ▸ | **Dos equipos escribiendo la misma idea no colisionan: se duplican**, y git no avisa |
| **2c** | Plantillas contables: cómo se reserva un código alterno `PLNSCDAL` |
| **2d** | Marcador de equipo en el prefijo de **todos** los commits, no sólo los de coordinación |
| **3** | Nombres de tabla de 4 letras: son únicos en **todo el proyecto**, no por esquema |
| 3 ▸ | **Reservar un nombre ≠ autorizar a crear la tabla.** `reservada` no es `autorizada` |
| **4** | Archivos con dueño exclusivo: pedírselo al dueño, no editarlo y avisar después |
| **5** | Bitácora de reservas — una línea cada vez que se reserva algo |
| **6** | El registro reserva `PRBRCDGO`, pero **el código busca por `PRBRALTR`** |
| **7** | **No mergear a `main` un mapeo cuya columna no está en la base** |
| **8** | Deudas transversales sin dueño: `handleError`, el `merge` desnudo, los errores que llegan como JSON, la búsqueda vacía que lanza excepción |
| **9** | Siete pantallas de `cnt` que truncan listados en silencio |

### ✅ Hubo dos §6 y dos §7 — RENUMERADO el 2026-09-03, con acuerdo de los tres árbitros

**Encontrado el 2026-09-03 al escribir este índice** — que es exactamente para lo que sirve: la
duplicación llevaba días en el archivo y nadie la había visto, porque nadie lee 700 líneas de
corrido.

**Resuelto:** *Deudas transversales* pasó de `§6` a **`§8`** (con sus `§8.1`–`§8.4`), y *siete
pantallas de `cnt`* de `§7` a **`§9`**. Las referencias afectadas se arreglaron en el mismo commit.

⚠️ **Los números quedaron únicos, pero el archivo NO está en orden numérico**: el recorrido físico es
`6 → 8 → 7 → 9`. Fue deliberado — mover bloques enteros hace un diff que nadie revisa de verdad, y
lo que importaba era desambiguar, no ordenar. **Usá este índice para saltar, no el scroll.**

### ⛔ Tres afirmaciones sobre el costo, tres veces mal — y la lección está en la tercera

Vale la pena leer esto entero antes de escribir cualquier «lo verifiqué» en este archivo.

1. **`lap-saa-1-arb`**: *«renumerar rompe referencias en silencio; varios citan §6.1 y §6.4»*.
   **Falso** — escrito con seguridad, sin medir.
2. **`omen-saa-1-arb`** (yo), corrigiendo lo anterior: *«ningún `§6.x` del repositorio apunta a este
   archivo»*. **También falso**, y escrito *dentro del bloque que corregía una afirmación no
   medida*. Mi `grep` llevaba `--include=*.md`, y la cita que lo desmiente vive en un `.sql`:
   `crd/sql/159_CASO_71177_CONSULTA_QUE_FALLA.sql:23` citaba `§6.1` — subsección del §6 que se iba a
   mover. La habría roto, en silencio, el commit que decía haber medido que no rompía nada.
3. **`omen-saa-2-arb`** la encontró, y con ella **la lección que ninguna de las tres versiones
   muestra sola:**

> ⛔ **No alcanza con exigir que se mida: hay que decir QUÉ se midió.** *«Medí las citas»* y *«medí
> las citas a `§N` y a `§N.x`, **sólo en `.md`, sólo bajo `docs/`**»* se leen igual y no son lo
> mismo. Una medición sin su alcance declarado da la misma confianza que una afirmación sin medir,
> y es **más difícil de auditar** — porque parece verificada.

⚠️ **Y la causa precisa importa, porque la primera explicación también estuvo mal.** Se atribuyó a
haber buscado `§N` y no `§N.x`. **No fue eso**: el patrón `§6.[0-9]` sí estaba. Lo que faltó fue el
**alcance de archivos** — `--include=*.md`, y la cita vive en un `.sql`.

> **El filtro es la parte invisible de una medición.** Un `grep` muestra lo que casa **dentro de lo
> que le dejaste mirar**, y el `--include` **no aparece en el resultado**: se ven los hallazgos,
> nunca lo excluido. Un hueco de filtro no se manifiesta como error — se manifiesta como una lista
> más corta, que es indistinguible de una lista correcta.

⚠️ **Y hubo una quinta vuelta, que generaliza la regla.** La diferencia entre el conteo de cinco y
el de seis **no fue el `--include`**: la medición de cinco exigía que **el nombre del archivo
apareciera en la misma línea que el `§`**, y `ESTADO-EQUIPO-OMEN-1.md:204` dice sólo *«Va al registro
§6»*. Distinto filtro, mismo fallo, cometido otra vez dentro de la corrección del anterior.

> ⛔ **El alcance de una medición no es sólo QUÉ archivos mirás: es también QUÉ FORMA tiene que tener
> el dato para que lo cuentes.** Anclar a una co-ocurrencia en la misma línea es un filtro tan
> invisible como un `--include`, y **peor: ni siquiera aparece como opción del comando** — vive
> dentro del patrón.

**Por eso la práctica correcta no es una regla sobre qué medir, sino pegar el comando.** El comando
lleva el `--include` *y* la forma del patrón; cualquier regla sobre qué mirar deja afuera la próxima
manera de recortar sin darse cuenta.

**Cómo se declara un alcance, en la práctica:** pegando el comando. Si la afirmación viene con el
`grep` que la produjo, cualquiera ve el `--include` y el patrón, y el que sigue no repite el hueco.

*(Nota de conteo: `lap-saa-1-arb` reportó **cinco** citas externas y esta tabla lista **seis**. No es
contradicción, es otro alcance: la medición final corrió sobre **todos** los tipos de archivo del
repo, no sólo `docs/**.md`, y aparecieron `ESTADO-EQUIPO-OMEN-1.md` y `rhh/PLAN-REPORTE-MDT-SUT.md`.
Que dos conteos honestos del mismo hecho difieran es, otra vez, el alcance y no los datos.)*

**Lo medido, ahora sí, y con el alcance dicho** — `grep "§ *[67]"` sobre **todo tipo de archivo**,
en los que nombran este registro:

| Cita | Apunta a | Estado |
|---|---|---|
| `ESTADO-EQUIPO-LAP-1.md:93` | §6 `PRBRALTR` | intacta |
| `ESTADO-EQUIPO-OMEN-2.md:307` | §6 `PRBRALTR`, **citado por título** | intacta |
| `ESTADO-EQUIPO-OMEN-1.md:204` | §6 `PRBRALTR` | intacta |
| `rhh/PLAN-REPORTE-MDT-SUT.md:86` | §6 `PRBRALTR` | intacta |
| `tsr/API-GASTO-CAJA-CHICA.md:143` + scripts `e2-06`/`e2-07` | §7 del mapeo | intacta |
| **`crd/sql/159:23`** | **§6.1 → `§8.1`** | **corregida en el mismo commit** |

### Lo que renumerar NO arregla, y es lo que de verdad importa

**El argumento de `omen-saa-2`, que conviene no perder:** numeramos por
secuencia. Cada uno abre su copia, ve que la última es la 7 y escribe «## 8». **Dos equipos que
hacen eso el mismo día producen dos «## 8», y git los fusiona sin avisar.** Escribir primero la
línea del §0 ayuda sólo si el otro fetcheó *después* de que la subiste; en la misma ventana, vuelve
a pasar. Es el mismo mecanismo que el §2b ya resolvió para los scripts `.sql` **repartiendo rangos
por equipo**, y la misma familia de fallo que el proyecto viene encontrando toda la semana: *dos
caminos que producen el mismo valor sin compararse nunca*. Un conflicto de git es un mecanismo de
aviso, y numerar por secuencia lo desactiva.

⛔ **La regla que queda, y es la principal: CITAR POR TÍTULO, no sólo por número.** Renumerar
arregló la ambigüedad de hoy; esto evita la próxima. **Si las citas usan el título, el número deja
de ser un recurso compartido** y una colisión futura pasa a ser cosmética en vez de peligrosa.

Escribí `§8 «Deudas transversales»`, no `§8` a secas. Cuesta cinco palabras y sobrevive a que
alguien renumere.

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
| **150–199** | **CRD · EQUIPO B** — ciclo del crédito y seguros · **agotado el 2026-09-05** |
| **200–249** | **CRD · EQUIPO B** — continuación del anterior · **reservado 2026-09-05 por `omen-saa-1-arb`** |
| ≥ 250 | sin asignar |

**Nota de la reserva del 200–249 (2026-09-05).** El rango 150–199 se agotó durante la corrida de
jubilados de agosto 2026: el 199 fue el último. Se toma el bloque inmediato siguiente **porque el
`≥ 200` figuraba explícitamente como «sin asignar»** — no estaba reservado por el equipo A ni por
nadie. Se anota acá **antes de usarlo**, que es lo que el §2b pide, y se avisó a `lap-saa-1-arb`
en el mismo acto por ser la contraparte del acuerdo original del 2026-08-31. Si el equipo A ya lo
tenía tomado sin haberlo escrito, aplica el criterio de desempate del §2b: **el que ya está en
`origin` se queda; renumera el otro** — y el que estaría en `origin` primero es esta reserva.

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

### ⛔ El mismo mecanismo al revés: dos equipos escribiendo la misma idea **no colisionan, se duplican** — 2026-09-03

Arriba el problema es que **lo no pusheado es invisible**. Acá es el inverso, y es más difícil de
ver: **lo pusheado por dos se suma en silencio.**

Hallazgo de `lap-saa-1-arb`, sobre este mismo archivo. Los dos árbitros escribimos la distinción
entre *reservar un nombre* y *autorizar una tabla* (§3) al mismo tiempo, cada uno en su copia.
**El rebase no dio conflicto**: los bloques cayeron en líneas distintas del mismo archivo y git los
fusionó sin decir nada. Quedaron las dos versiones conviviendo, redactadas distinto.

**Por qué importa más que un conflicto:** un conflicto **avisa** y no se puede ignorar. Una
duplicación no avisa a nadie. Y un registro con la misma regla dos veces, en dos redacciones,
**envejece hacia la contradicción** — una se corrige, la otra no, y el día que alguien las lea
juntas no va a saber cuál manda. Que es exactamente el fallo que este archivo existe para prevenir.

**Qué hacer:**

**Qué hacer, en este orden — y el orden es el arreglo:**

1. **`git fetch` y buscá el concepto en el §0 antes de escribir.** Es la **regla 0 extendida**: no
   sólo antes de reservar un código, **antes de escribir cualquier regla**. Es el mecanismo
   principal porque **no depende de que nadie más haga nada**, escala a los cuatro equipos que
   escriben este archivo, y funciona para el que arranque mañana sin que nadie lo agregue a una
   lista.
2. **Escribí la línea del §0 primero, la sección después** (ver el recuadro del §0).
3. **Avisá a los árbitros activos al subirla.** Es la **red**, no el mecanismo: atrapa lo que la
   búsqueda no ve. Y va a **los activos**, en plural — este archivo lo escriben cuatro equipos, no
   dos.
4. **Al recibir ese aviso, buscá si vos también la escribiste** antes de seguir.
5. **Desempate:** la que está en `origin` se queda; la otra se borra, **no se fusiona** — dos
   redacciones de la misma regla no se mejoran mezclándose.

> ⚠️ **Por qué el `fetch` va primero y el aviso segundo, y no al revés.** La primera versión de esta
> subsección ponía el aviso como mecanismo principal. **Estaba mal, y se escribió justo después de
> que este caso se salvara por casualidad:** se detectó *porque* un árbitro avisó, y eso es
> evidencia de que esa vez hubo suerte, no de que avisar funcione. Un remedio que depende del
> comportamiento ajeno falla exactamente en el caso que más lo necesita — el del equipo nuevo que
> todavía no sabe que la convención existe.
>
> **Con su propio límite declarado:** *un `grep` recorta por definición — muestra lo que casa, no lo
> que decide*. Dos redacciones de la misma idea pueden no compartir ninguna palabra. Por eso el
> paso 3 sigue existiendo, y por eso los títulos del §0 nombran el concepto y no el incidente: para
> que la búsqueda tenga contra qué casar.

> Y una nota sobre el desempate real de este caso: la que sobrevivió no fue la primera en llegar
> sino la mejor ubicada (arriba de la tabla de Reservados, apoyada en su columna **Estado**). El
> criterio de `origin` es para cuando no hay una razón mejor — cuando la hay, se usa la razón.

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

### ⛔ Reservar un nombre y autorizar una tabla son DOS actos distintos — 2026-09-03

Esta sección los confundía en una sola fila, y la confusión salió a la luz cuando el equipo de la
laptop reservó `CRD.PGCE` (detalle del cruce de un pago de pensión contra préstamos) para un frente
cuyo backend es de CRD/Equipo B.

- **Reservar el nombre** evita una colisión de código de 4 letras. Lo puede hacer **cualquier
  equipo**, en cualquier momento, y es barato: apartar `PGCE` no le cuesta nada a nadie.
- **Crear la tabla** es **cambiar el modelo de datos del módulo dueño**. Eso lo decide el **dueño
  del módulo**, no el equipo que la propone — aunque el diseño sea correcto y el nombre ya esté
  apartado. Y el DDL lo termina corriendo un usuario sobre una base que comparte `main` con las
  demás.

**En la práctica:** una fila puede estar en «reservada» sin que exista ningún permiso para escribir
su DDL. La columna **Estado** lo dice; **`reservada` no es `autorizada`**.

> Precedente: `PGCE` quedó reservada el 2026-09-03 con el DDL explícitamente detenido hasta que el
> usuario de CRD/Equipo B decida sobre el frente de pago a jubilados completo. El equipo que la
> propuso aceptó la corrección sin discusión — el registro era lo que estaba mal, no ellos.

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
| `USAP` | Usuario de app móvil (credenciales del partícipe) | CRD | **autorizada por el usuario**; creada en local, DDL de producción escrito y sin correr |

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
| 2026-09-03 | `lap-saa-1` (**laptop**) | **Números de script `crd/sql/` 200-249** | El §2b reparte 96-149 (equipo A) y 150-199 (equipo B) y deja **≥200 sin asignar**; este equipo entró a `crd` el 2026-09-03 por el frente de pago a jubilados y necesita rango propio. Verificado el mismo día: el `MAX` real en la carpeta es **188**, así que el bloque está libre con margen. **Los equipos A y B no pierden nada**: su rango no se toca |
| 2026-09-03 | `lap-saa-1` (**laptop**) | **Tabla `CRD.PGCE`** — cruce de un pago de pensión contra los eventos de préstamo que generó | Anulación de un pago de pensión **con** cruce (decisión del usuario, 2026-09-03). Hace falta tabla y no columna porque `cruzarContraPrestamos` llama a `pagarConAportes` **una vez por préstamo vigente**: son N eventos por pago, no uno. Verificada libre contra `src/main/java/com/saa/model/` y contra las 100 tablas `CRD` ya mapeadas — **falta confirmarla contra `ALL_TABLES`** antes de crearla. ⚠️ **La implementa `eqB`, no este equipo**: por el acuerdo del 2026-09-03 el `saaBE` del frente de jubilados es de ellos y el `saaFE` de `lap-saa-1`. Se reserva acá porque el nombre es un recurso global y lo reserva quien lo propone. Diseño en `crd/DISENO-PANTALLA-PAGO-JUBILADOS.md` §6bis. ⛔ **NOMBRE APARTADO, DDL NO AUTORIZADO** (ver el recuadro del §3): `CRD` es el esquema de `omen-saa-1` y **crear la tabla lo decide su usuario, no este equipo** — aunque el frente termine no haciéndose. El DDL no se escribe ni se corre hasta ese visto bueno |
| 2026-09-03 | app móvil ASOPREP (árbitro `omen-arb-app`) | **Tabla `CRD.USAP`** — credenciales de acceso del partícipe a la app móvil | La app "Asoprep Contigo" deja Azure y pasa a consultar el SAA por un WAR de borde (`SaaMovilBE`) expuesto a internet; `SaaBE` queda en intranet. `USAP` es lo único que le faltaba a `SaaBE`: el almacén de credenciales. **No es la tabla de usuarios del sistema** — es 1:1 con `CRD.ENTD` (`ENTDCDGO` UNIQUE, no sólo FK) y sólo dice «este partícipe puede entrar a la app». Verificada libre contra `src/main/java/com/saa/model/` (grep, cero hits) — **falta confirmarla contra `ALL_TABLES`**, y ese control es el **0.1 del propio DDL**, que detiene el script si devuelve filas. **No consume ningún `PRBR`/`PDTR`**: los estados (activo/bloqueado/eliminado) son constantes planas en `com.saa.rubros.EstadoUsuarioApp`, no catálogo `Rubro`/`DetalleRubro` — es estado técnico de cuenta, no parametría de oficina. DDL en `crd/sql/DDL-USUARIO-APP-MOVIL.sql` (sin número: sigue la convención `DDL-*` y **no toca los rangos numerados del §2b**). API en `crd/API-USUARIO-APP-MOVIL.md`. ⚠️ **`CRD` es el esquema de `omen-saa-1`**: según el recuadro del §3, crear la tabla lo decide su usuario. Acá **la creación la ordenó el usuario directamente** (encargo del 2026-09-03), así que está **autorizada**, no sólo reservada — pero se anota para que `omen-saa-1` lo sepa sin tener que descubrirlo en un merge |

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

## 8. Deudas transversales — afectan a TODOS los módulos, y ninguna tiene dueño

**Abierta el 2026-09-01 por el árbitro de `omen-saa-1`, a pedido del de `omen-saa-2`.** Van acá y no
en el tablero de un equipo porque **no son de un módulo**: el que las encuentra tiene el diagnóstico,
y el que las sufre después suele ser otro. Cada equipo referencia esta sección en vez de duplicarla.

**Ninguna de las tres se arregla desde un módulo.** Están listadas para que nadie las "arregle" solo
y para que nadie pierda medio día rediagnosticándolas.

### 8.1 ⛔ `handleError`: un fallo de consulta se lee como «no hay datos» — 316 servicios del frontend

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

### 8.2 ⛔ `EntityDaoImpl.save()` hace `em.merge()` desnudo: un `PUT` parcial graba `NULL`

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

### 8.3 Los errores del REST NO llegan como texto plano, aunque el endpoint los escriba así

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

### 8.4 ⛔ Una búsqueda sin resultados lanza excepción — 255 servicios del backend

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

---

## 9. ⛔ Hallazgo transversal en `cnt` (frontend): siete pantallas pueden truncar listados en silencio

**Medido el 2026-09-02 por el equipo B de crd**, al corregir un «0 of 0» que el usuario reportó en la
pantalla de plantillas. **Ninguna de las pantallas de `cnt` fue modificada salvo esa** — el resto es
una investigación de sólo lectura que se deja acá para quien tome contabilidad.

### El defecto

El paginador (y a veces el `matSort`) se conecta al `MatTableDataSource` **una sola vez**, en
`ngAfterViewInit`. Pero vive dentro de un `*ngIf`/`@if` que **arranca en falso** —porque hay un panel
de bienvenida, o porque `loading` se pone en `true` de forma síncrona en `ngOnInit`, que corre
**antes** que `ngAfterViewInit`—. En ese momento el `@ViewChild` resuelve `undefined`, la asignación
no ocurre, y **nunca se vuelve a intentar**: después el paginador aparece de verdad en el DOM y ya
nadie lo engancha.

### ⛔ Por qué no es cosmético

Una tabla así **muestra las primeras filas de un conjunto más grande sin ningún indicio de que hay
más**. El contador dice «0 of 0» y los controles no responden. Alguien mira un plan de cuentas, un
listado de asientos o una mayorización, ve lo que hay en pantalla, y **concluye que eso es todo**.

### Estado

| Pantalla | Estado |
|---|---|
| `plantilla-general` | ✅ **Corregido** (`a3e2bf4`) — reportado por el usuario |
| `asientos-contables-dinamico` | ✅ **Corregido** (`saaFE` `762a4b2`) |
| `listado-asientos` | ✅ **Corregido** (`saaFE` `762a4b2`) |
| `mayorizacion` | ✅ **Corregido** (`saaFE` `762a4b2`) |
| `parametrizacion/centro-grid` | ✅ **Corregido** (`saaFE` `762a4b2`) — además el orden por defecto dependía del mismo `ViewChild` |
| `parametrizacion/periodo-contable` | ✅ **Corregido** (`saaFE` `762a4b2`) |
| `parametrizacion/plan-grid` | ✅ **Corregido** (`saaFE` `762a4b2`) |
| `detalle-mayorizacion` | ✔ no aplica — pagina por *slicing* manual, otra arquitectura |
| `plan-arbol` | ✔ no aplica — es un árbol, no tiene paginador ni sort |
| `tipo-asiento-general-grid` · `tipo-asiento-sistema-grid` | ✔ sin gating, siempre en el DOM |

### La corrección, ya probada

Factorizar el enganche en un método **idempotente** (que sólo reasigna si la referencia cambió) y
llamarlo también desde `ngAfterViewChecked`. Aplicado y verificado en `plantilla-general`; es
mecánico y el mismo para las seis.

**No se tocan sin que su dueño lo decida.** Queda acá para que la decisión se tome con la lista
completa y no pantalla por pantalla.

> Nota menor, sin tocar: `plantilla-general` declara un `@ViewChild('maestroPaginator')` que no tiene
> `<mat-paginator #maestroPaginator>` en el HTML ni asignación en el `.ts`. Es una referencia
> huérfana, no el mismo defecto.

### Actualización 2026-09-02 — las seis corregidas, y una séptima que faltaba

Las seis quedaron corregidas en `saaFE` `762a4b2`, con el mismo método idempotente en todas. En
`centro-grid` hubo que mover además el orden por defecto al mismo bloque: dependía del mismo
`ViewChild` y con el contenedor oculto operaba sobre `undefined`. No es una variante de diseño — es
la misma corrección alcanzando la parte que dependía de lo mismo.

✅ **La séptima, corregida** (`saaFE` `ceb65b2`) — se había escapado del barrido inicial:
`dialog/mayor-analitico-asiento-dialog` tiene el mismo defecto (sólo paginador, no usa `matSort`):
`loading` arranca en `true`, así que la tabla y su paginador están ocultos en el primer render.

⚠️ **Y una trampa de coordinación, sin resolver:** `saaFE/docs/REGISTRO-RESERVAS-EQUIPOS.md` es un
**espejo desactualizado** de este documento — 113 líneas contra 615, sin este §9 ni buena parte de lo
anterior. Quien lo lea creyendo que está al día va a tomar decisiones con información vieja. **Hay
que sincronizarlo o borrarlo**; dejarlo así es peor que no tenerlo. No se toca desde acá porque está
fuera del alcance de escritura de este equipo en `saaFE` (que se limita a `docs/{modulo}/`).

**Cerrado el 2026-09-02: las siete pantallas de `cnt` quedaron corregidas** (`saaFE` `762a4b2` las
seis, `ceb65b2` el diálogo del mayor analítico). Falta la verificación visual, que sólo puede hacer
alguien con navegador: abrir cada una y confirmar que el paginador dice «X of Y» y que ordenar y
cambiar de página responden.

Aparte, y **no es el mismo defecto**: la pantalla `reporte-mayor-analitico` cortaba las columnas
derechas del panel de movimientos **sin barra de scroll horizontal**. Causa: `.rm-panel` declara
`overflow: hidden` —que fija los **dos** ejes— y `.rm-panel--detail` sólo pisaba el vertical con
`overflow-y: auto`. Corregido con `overflow: auto` (`saaFE` `449d209`).
