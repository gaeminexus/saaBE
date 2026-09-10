# Activación del módulo de seguridades para SAA — árbol de permisos

**Equipo:** `lap-saa-1` (laptop) · **Arrancado:** 2026-09-10
**Frente:** generar el catálogo de códigos de permiso de SAA y conectar la validación en los menús.

---

## 0. Qué se pidió, en una línea

Barrer SAA entero, generar el árbol de sus pantallas en `SCP.PJRQ` (jerarquía `PGSPCDGO = 11`),
borrar los sistemas que hoy ocupan esa jerarquía y que no son de SAA, y conectar la verificación de
permisos en los menús con el código que le toca a cada opción.

---

## 1. El mecanismo, verificado contra el código — no contra documentación

| Pieza | Dónde | Qué hace |
|---|---|---|
| Endpoint | `GET /rest/usro/verificaPermiso/{idEmpresa}/{idUsuario}/{idPermiso}` | `UsuarioRest.java:116-125`. Devuelve **`text/plain`**: `'OK'` o el mensaje de negación |
| Motor real | SP `scp.pc_crct_espc.pr_vrfc_prms_susr` | `UsuarioDaoServiceImpl.java:125-137`. Params `pn_empresa`, `pn_cdusro`, `pn_permiso` → `pv_resultado` |
| Modelo del menú | `NavItem` en `saaFE/src/app/shared/basics/menu/model/nav-item.ts` | **ya tiene `idPermiso?: number`** |
| La llamada en el menú | `menu-list.component.ts:73-90` | **ya está escrita y comentada**: compara `result === 'OK'`, navega o muestra snackbar |
| Interruptor | rubro alterno **7** / detalle alterno **1** | `0` = no se validan permisos · `1` = sí |
| Lectura del interruptor desde el FE | `GET /rest/pdtr/getRubros/7` | `DetalleRubroRest.java:55-66` → `selectByCodigoAlternoRubro(7, 1L)` |

> **Consecuencia práctica: este frente NO necesita backend nuevo.** El endpoint de verificación
> existe, y el interruptor se puede leer con un endpoint que también existe. Todo el trabajo de
> código es de `saaFE`, más los `.sql` que escribe el árbitro.

### 1.1 ⚠️ El filtro de errores NO toca esta respuesta, y está dicho a propósito

`MensajeErrorJsonFilter` envuelve en `{"mensaje": "..."}` los cuerpos de texto de las respuestas de
error. **No aplica acá**: su javadoc nombra explícitamente a `verificaPermiso` de `UsuarioRest`
entre los que declaran `TEXT_PLAIN` y devuelven texto a propósito. La comparación `result === 'OK'`
del frontend es correcta tal como está escrita.

Se anota porque desde afuera parece justo el caso que el filtro rompería, y el que lo mire de nuevo
va a querer "arreglarlo".

### 1.0 ⛔⛔ `SCP.PJRQ` ES TAMBIÉN LA TABLA DE EMPRESAS Y DE USUARIOS

**Medido el 2026-09-10 con `lap1-13`, y es lo más importante de todo este documento.**

Verificado en las entidades JPA, no deducido:

```
com.saa.model.scp.Empresa  -> @Table(name = "PJRQ", schema = "SCP")
com.saa.model.scp.Usuario  -> @Table(name = "PJRQ", schema = "SCP")
```

`SCP.PGSP` no es «la lista de jerarquías»: es **el árbol de pantallas del propio módulo de
seguridades**, y cada una de sus hojas define **un tipo de jerarquía** que se guarda en `PJRQ`,
discriminado por `PGSPCDGO`:

| `PGSPCDGO` | Qué guarda |
|---|---|
| 5 | EMPRESA — estructura legal |
| 6 | ESTRUCTURA ORGANIZACIONAL |
| **9** | **USUARIO — los usuarios del sistema viven acá** |
| **11** | **NIVELES DEL SISTEMA — el árbol de permisos. El nuestro** |
| 12 | EMPRESA tal como se ingresó en el sistema |
| 17 / 18 | ÁREA / NIVEL |

786 filas en 8 jerarquías; **742 son del 11**.

> ⛔ **Ningún `DELETE` sobre `SCP.PJRQ` puede escribirse sin `PGSPCDGO = 11`.** Uno más ancho no
> borra «sistemas viejos de permisos»: borra **empresas y usuarios**, y con ellos revienta media
> base por integridad referencial.

Y explica de una las 48 FK que apuntan a `PJRQ`: se llaman `FK_..._EMPRESA` y `FK_..._USUARIO`
porque **son la empresa y el usuario** de cada caja chica, cada pago, cada asiento. **No son
asignaciones de permisos.** La pregunta original —«¿qué pasa con los permisos ya asignados?»— tiene
otra respuesta de la que se esperaba: las asignaciones no están en ninguna de esas tablas.

### 1.0bis ⛔ La secuencia SÍ existe, y la premisa de arranque era falsa

Se arrancó bajo el supuesto de que *«la tabla `pjrq` no maneja secuencia en base»*. **No es así:**

```
Empresa.java:19   @SequenceGenerator(sequenceName = "SCP.SQ_PJRQCDGO")
Usuario.java:29   @SequenceGenerator(sequenceName = "SCP.SQ_PJRQCDGO")
```

Los dos con `GenerationType.SEQUENCE`. Cada usuario o empresa que se crea desde SAA saca su id de
`SCP.SQ_PJRQCDGO`.

**Quemar ~700 ids desde el 1253 sin adelantar la secuencia la deja por debajo de lo usado, y
entonces el próximo usuario o empresa que se cree muere con ORA-00001 — en una pantalla que no
tiene nada que ver con permisos.** Es la regla 8 del esquema de trabajo, y acá aplica de lleno.

⚠️ **Y no vale generalizar desde `[[secuencias-rubros-no-existen]]`:** para `PRBR`/`PDTR` las
secuencias efectivamente no existen y la regla de sincronizarlas protegía contra algo imposible.
Acá es al revés. **La misma tabla, la misma pregunta, la respuesta opuesta** — por eso se mide cada
vez en vez de recordar.

**El `lap1-15` tiene que adelantar la secuencia después del `INSERT`.**

### 1.0ter La restricción que condiciona los nombres del árbol

`UN_PJRQ_01` es **UNIQUE sobre `(PGSPCDGO, PJRQNMBR, PJRQCDPD)`**: **dos hermanos no pueden llamarse
igual.**

En SAA se repiten «CONSULTA», «INGRESO», «PARAMETRIZACIÓN» y «REPORTES» entre módulos. Entre padres
distintos no hay problema; lo que hay que evitar al generar el árbol son dos hijos homónimos bajo el
mismo padre.

Además: `PJRQNMBR` es `VARCHAR2(300) NOT NULL`, y `PJRQCDPD` también es `NOT NULL` (la raíz usa `0`).
**No hay ninguna columna oculta**: la tabla tiene exactamente las 6 del CSV, así que el `INSERT` de
6 columnas es seguro. El ORA-01400 que se temía no aplica.

### 1.2 🔴 El interruptor NO se lee de donde se cree — confirmado

El pedido del usuario fue: *«si en la **descripción** tiene un 0 no se manejan permisos»*.

El único lector del backend es `DetalleRubroDaoServiceImpl:100-104`, y su JPQL devuelve
`t.valorAlfanumerico`, que mapea **`PDTRVLRV`** — no `PDTRDSCR`.

**Si el `0`/`1` está escrito en la descripción, el interruptor no funciona y no da ningún error:**
lee vacío y sigue. Es la familia de fallo de siempre — el lector apuntando a donde nadie escribe.

**Medido con el CONTROL 9 del `lap1-13`, y era exactamente así:**

| `PRBRCDGO` | `PRBRALTR` | `PRBRDSCR` | `PDTRCDGO` | `PDTRALTR` | `PDTRDSCR` | `PDTRVLRV` | `PDTRESTD` |
|---|---|---|---|---|---|---|---|
| 7 | 7 | `MANEJA PERMISO (SI = 1, NO = 0)` | 23 | 1 | **`0`** | **(vacío)** | 1 |

El valor vive en **`PDTRDSCR`**; **`PDTRVLRV` está vacío**. El único lector del backend devuelve
`PDTRVLRV`. **El interruptor, tal como está, no se puede leer con lo que existe hoy.**

🟡 **Decisión pendiente del usuario:** mover el `0` a `PDTRVLRV` (un `UPDATE` de una fila, no toca
código), o leer `PDTRDSCR` desde el frontend con `GET /rest/pdtr/getRubros/7`, que devuelve la
entidad completa y por lo tanto **ya trae las dos columnas**. La segunda no necesita ni `UPDATE` ni
backend nuevo, y es la que recomiendo.

---

## 2. Decisiones del usuario — 2026-09-10

Tomadas por el usuario respondiendo a las ocho preguntas de arranque. **No se re-preguntan.**

| # | Decisión |
|---|---|
| **1** | **No es tabla rasa.** Se conservan `RAIZ SISTEMAS` (93) y todo el subárbol de `SEGURIDAD Y AUDITORIA` (42–166). Se borran **`PCC` (482), `CID` (315) y `ACT` (979)** con sus subárboles |
| **2** | **Los permisos ya asignados a esos tres sistemas se borran** junto con sus nodos |
| **3** | **Un solo sistema `SAA`** de nivel 1 bajo la raíz, y los módulos como nivel 2. No un sistema por módulo |
| **4** | `PJRQINGR` se genera con **`1`** en todos los nodos nuevos |
| **5** | `PJRQNVLL` se genera con **profundidad real** (`SAA` = 1, módulo = 2, grupo = 3, pantalla = 4…), no copiando la incoherencia del árbol actual |
| **6** | `PJRQNMBR` admite hasta **300 caracteres**, pero los nombres van **lo más cortos posible** |
| **7** | **Las opciones de menú que no tengan pantalla programada no se muestran: se dejan comentadas** en el componente del menú |
| **8** | **Validación al hacer clic**, con el endpoint de a uno. **No** se construye el endpoint batch por ahora |

### 2.1 Numeración

- Los `INSERT` llevan **ids quemados**, sin secuencia, arrancando en **1253**.
- El id quemado no es una comodidad: es lo que permite que un nodo referencie a su padre
  (`PJRQCDPD`) dentro del mismo script.
- ✅ **El 1253 está confirmado.** El CONTROL 6 del `lap1-13` midió `MAX(PJRQCDGO) = 1252` sobre
  **toda** la tabla (no sólo `PGSPCDGO = 11`, cuyo máximo es 1220). El número que dio el usuario es
  el correcto.
- ⛔ **Pero hay que adelantar `SCP.SQ_PJRQCDGO` después del `INSERT`** — ver §1.0bis. Ese paso no
  estaba en el pedido original y es el que evita un ORA-00001 al crear el próximo usuario.

### 2.2 Granularidad — la regla que gobierna todo el árbol

**Un nodo por pantalla. Nunca por botón.**

La única excepción es un **botón que abre otra pantalla** (un `MatDialog` con formulario propio, o
un `router.navigate` a otra ruta): esa pantalla hija es un nodo y cuelga de la pantalla que la abre.

Los botones que sí requieran permiso propio se van agregando al árbol **a pedido**, cuando el
usuario los reporte. No se generan por adelantado.

---

## 3. Lo que hay que medir antes de escribir el `DELETE` y el `INSERT`

`sql/lap1-13-diagnostico-arbol-permisos.sql` — **solo lectura, cero DML**, diez controles.

Los tres que pueden cambiar el plan:

| Control | Qué pasa si sale distinto de lo esperado |
|---|---|
| **2** — columnas reales de `PJRQ` | El CSV exportado trae 6 columnas. Si la tabla tiene una séptima `NOT NULL` sin default, **todos los `INSERT` mueren con ORA-01400**. Es exactamente el ORA-01400 de `CPNMTPCL` que le costó una corrección en producción a `omen-saa-2`: nombraron 7 columnas de 29 |
| **4 / 5** — quién referencia `PJRQCDGO` | Si el 4 **no devuelve nada**, las asignaciones de permisos referencian el árbol **sin FK declarada**. Entonces el `DELETE` no falla, y quedan permisos huérfanos apuntando a códigos que el `INSERT` nuevo reutiliza para otras pantallas: **un permiso viejo concediendo algo nuevo, en silencio.** El 5 las encuentra igual, por nombre de columna |
| **6** — `MAX(PJRQCDGO)` global | Confirma o desmiente el 1253 |

---

## 4. 🔴 Dos trampas encontradas al relevar

### 4.1 El choque de `PermisosRrh` — 47 códigos ya inventados que apuntan a otro sistema

`saaFE/src/app/modules/rrh/model/permisos-rrh.ts` declara **47 constantes de permiso** en el rango
**840–9xx**, ya cableadas en el menú de RRHH, con un comentario que dice *«estos códigos deben
existir en el catálogo de permisos del backend»*.

**No existen para RRHH: ese rango está ocupado en `PGSPCDGO = 11` por nodos de `PCC`** — el 900 es
hoy un botón «MODIFICAR» de promociones, el 840 un grupo de reportes.

Dos consecuencias, y la segunda es peor:

- Si se activara la validación **hoy**, el menú de RRHH preguntaría por permisos de otro sistema.
- Después del `DELETE` de `PCC`, preguntaría por códigos que **ya no existen**.

**Hay que reasignar las 47 al árbol nuevo en el mismo movimiento.** Lo inventaría el ÍTEM 4 del
barrido de frontend.

### 4.2 El permiso de ingreso quemado en un rubro — y el rubro NO EXISTE

`UsuarioDaoServiceImpl:151`, dentro de `validaUsuarioSucursal`, lee el rubro `MODULO_SISTEMA`
(alterno **15**) / detalle `INGRESO` (alterno **99**) y usa ese valor **como `PJRQCDGO`** para
preguntarle al SP si el usuario tiene permiso de ingreso al sistema.

**Hoy no rompe nada**: el frontend hace login con `validaUsuario` (el simple), no con
`validaUsuarioSucursal` — verificado en `login.component.ts:190` y `usuario.service.ts:90-91`.

**Y el CONTROL 9 del `lap1-13` devolvió una sola fila: la del rubro 7. La del rubro alterno 15 /
detalle alterno 99 NO EXISTE.**

Eso cambia el diagnóstico y lo empeora un poco: `selectValorStringByRubAltDetAlt` termina en
`getSingleResult()`, así que sin fila **lanza `NoResultException`**. `validaUsuarioSucursal` no está
«esperando a que le borren el nodo»: **ya está roto hoy**, y falla antes de llegar al SP.

Nuestro `DELETE` no lo empeora ni lo mejora — no lo toca. Pero queda escrito acá porque **el día que
alguien active el login por sucursal se va a encontrar con esto y va a creer que lo rompimos
nosotros.** Si se quiere usar ese camino, primero hay que crear el detalle rubro 15/99 apuntando al
nodo `SAA` del árbol nuevo.

---

## 5. Estado del frente

| Paso | Estado |
|---|---|
| Preguntas de arranque respondidas (§2) | ✅ 2026-09-10 |
| `sql/lap1-13` — diagnóstico | ✅ **corrido por el usuario el 2026-09-10.** Resultados analizados en §1.0, §1.0bis, §1.0ter, §1.2 y §4.2 |
| `sql/lap1-14` — chequeo referencial consolidado + secuencia | ✅ escrito. **Pendiente de que el usuario lo corra** |
| Barrido del frontend — ÍTEM 1 (los 9 menús) | ✅ 2026-09-10, en `saaFE/docs/seguridad/INVENTARIO-PANTALLAS-SAA.md` |
| Barrido del frontend — ÍTEM 2 (rutas) e ÍTEM 4 (`PermisosRrh`) | ✅ 2026-09-10 |
| Barrido del frontend — ÍTEM 3 (pantallas hijas) | 🔵 en curso |
| Barrido del frontend — ÍTEM 3 (149 relaciones pantalla-a-pantalla) | ✅ 2026-09-10 |
| `sql/lap1-15` — `DELETE` de 666 nodos + `INSERT` de 305 | ✅ escrito. **Pendiente de que el usuario lo corra**, después del `lap1-14` |
| `CODIGOS-PERMISOS-SAA.md` y `API-PERMISOS-FRONTEND.md` | ✅ escritos y espejados a `saaFE/docs/seguridad/` |
| Cableado del frontend — menús **y botones** | 🔵 despachado a `lap-saa-1-fe` el 2026-09-10, en 7 ítems con dos puntos de control |
| Adelanto de la secuencia | ⛔ **no aplica, y está medido.** `SCP.SQ_PJRQCDGO` no existe, y SAA no podría insertar en `PJRQ` aunque existiera: `UsuarioRest` tiene siete métodos y los siete son `@GET`, y `EmpresaRest` no existe |

### 5.1 Lo que midió el barrido del frontend — 2026-09-10

- **Opciones de menú sin pantalla programada: sólo 2**, de ~140 opciones activas en los 9 menús, y
  las dos en `crd` («Archivos Descuentos», sin `route`; «Dash» bajo Cobros, con el `route`
  comentado aunque el destino exista). `cnt`, `cxc`, `cxp`, `rpr`, `rrh` y `tsr` no tienen ninguna.
  **La decisión 7 resultó mucho más chica de lo que parecía.**
- **~35 rutas a las que ningún menú llega**, repartidas en `crd`(11), `tsr`(14), `rrh`(6),
  `cnt`(4), `cxc`(4), `cxp`(3). Dos de `tsr` son placeholders de verdad
  (`TsrPlaceholderComponent` en `parametrizacion/bancos` y `parametrizacion/cajas/logicas`). Buena
  parte llevan `:id`/`:codigo`, así que son destinos de `router.navigate` y no huérfanas — lo
  confirma el ÍTEM 3.
- **Tres componentes completos sin ninguna ruta**: `tsr/forms/movimientos-bancarios/{creditos,
  debitos,transferencias}`. No aparecen en el cruce porque no hay `path:` que cruzar. **Inalcanzables
  hoy por cualquier camino.**
- **`PermisosRrh`**: de las 47 constantes, 44 están en un nodo visible del menú, 1
  (`APORTES_RETENCIONES` = 882) en un nodo comentado, y **2 no se usan en ningún lado**
  (`FICHA_COLABORADOR` = 871, `NOMINA` = 880).
- **`tsr/menu/menucreditos.component.ts` es un archivo muerto**: clase vacía, nadie lo importa, su
  selector no aparece en ningún template, y la única ruta a `MenucreditosComponent` apunta al de
  `crd`. **No se borra** — se deja anotado.
- **`dash/menu/menu.component.ts` NO es un `NavItem[]`**: son **7 botones hardcodeados en el HTML**
  que llaman `navigate('...')`, sin modelo de datos y sin `idPermiso`. Son justo los **nodos de
  nivel 2 del árbol** (los 7 módulos). ⚠️ **No hay que migrarlo a `NavItem[]`** —eso lo pasaría a
  renderizarse por `menu-list` y le cambiaría la cara a la pantalla de entrada—: alcanza con meter
  la verificación dentro de `navigate(ruta)`, con un mapa `ruta → idPermiso`. Es lo mismo que hace
  el menú lateral y no toca el diseño.
- **`asoprep` no tiene ni un componente ni una ruta en `saaFE`.** No entra al árbol: es backend puro
  (la carga de archivos Petro), operado desde otras pantallas.

---

## 6. El árbol: cómo se armó y qué quedó afuera — 2026-09-10

**305 nodos, ids 1253 a 1557.** Script: `sql/lap1-15-arbol-permisos-saa.sql`.
Mapa legible de códigos: `CODIGOS-PERMISOS-SAA.md`, espejado a `saaFE/docs/seguridad/`.

| Nivel | Qué es | Nodos |
|---|---|---|
| 1 | `SAA` (padre = 93, la raíz que se conserva) | 1 |
| 2 | Los 7 módulos | 7 |
| 3 | Grupos del menú (Parametrización, Procesos, Reportes…) | 28 |
| 4 | Pantallas y subgrupos | 142 |
| 5 | Pantallas hijas y hojas profundas | 90 |
| 6–7 | Diálogos de diálogos | 37 |

### 6.1 Las cuatro reglas del árbol

1. **Un nodo por pantalla, nunca por botón.** Excepción: un botón que **abre otra
   pantalla** (diálogo con formulario, o `router.navigate` a otra ruta) — esa pantalla hija es nodo y
   cuelga de la que la abre.
2. **Una pantalla que ya está en el menú tiene UN solo nodo**, en su lugar del menú. Que otra
   pantalla navegue hacia ella **no le crea un segundo nodo**. Sin esta regla el árbol tendría ~40
   nodos duplicados: el inventario encontró 149 relaciones pantalla-a-pantalla y buena parte son
   navegaciones entre dos opciones de menú (`PrestamoConsulta ⇄ PrestamoEdit`, `Marcaciones ⇄
   ResumenDiario`).
3. **Las rutas sin puerta de entrada no reciben nodo.** Ni menú, ni `navigate`: nadie puede llegar,
   así que un permiso sobre ellas no protege nada. Están listadas en el §6.2 para que se les pueda
   dar nodo el día que se les dé puerta.
4. **Excluidos** los diálogos de confirmación, los selectores de un valor y los visores de PDF — el
   mismo filtro que aplicó el barrido del frontend, de forma consistente en los cuatro módulos.

### 6.2 Lo que deliberadamente NO entró, y por qué

| Qué | Por qué |
|---|---|
| `PCC`, `CID`, `ACT` | Se borran: son sistemas ajenos a SAA |
| Los 11 bloques de **Cobros** de `tsr` | Retirados del menú a propósito el 2026-09-07. Rutas y componentes vivos — si se reactivan, se agregan al árbol |
| `/menutesoreria/parametrizacion/bancos` y `.../cajas/logicas` | `TsrPlaceholderComponent`: no hay pantalla detrás |
| `/menurecursoshumanos/procesos/aportes` | Retirada el 2026-08-26: pantalla a medio construir, sin entidad en el backend |
| `/menucuentaxpagar/pagos/transferencias-legacy` | Alcanzable sólo escribiendo la URL |
| `/menucreditos/entidad` | Huérfana real: sin menú y sin nadie que navegue hacia ella |
| `tsr/forms/movimientos-bancarios/{creditos,debitos,transferencias}` | Tres componentes completos **sin ninguna entrada en `app.routes.ts`** |
| `asoprep` | No tiene ni un componente ni una ruta en `saaFE` |
| «Archivos Descuentos» y «Dash» de `crd` | Las 2 opciones sin pantalla programada — **se comentan en el menú** (decisión 7) |

### 6.3 Dos decisiones de modelado que conviene poder revertir

- **Los diálogos repetidos son un solo nodo.** `ParticipeDashComponent` abre `AuditoriaDialog` desde
  4 lugares distintos y `DetalleConsultaCarga` abre `CoincidenciasEntidad` desde 2. El `UNIQUE`
  obligaba a elegir: cuatro nombres inventados, o uno solo. **Se eligió uno**, porque no sabemos si
  las 4 llamadas son la misma acción o cuatro secciones distintas, y un nombre inventado sobre algo
  que no se entendió es peor que un nodo de menos. Si resulta que hacen falta cuatro, se agregan.
- **Una pantalla alcanzable desde varios padres cuelga del grupo donde conceptualmente pertenece**,
  no del primero que la abre. `ParticipeDash` la abren `entidad-consulta`, `participe-inicial` y
  `pago-jubilados`; queda como hija de `PARTICIPES`.

### 6.4 Cómo se generó, y por qué no a mano

El árbol se escribió como texto indentado y un generador emitió los `INSERT`, **validando antes de
emitir**: que ningún padre falte, que el nivel del padre sea exactamente uno menos, que el padre
tenga id menor que el hijo (para que el orden del `INSERT` funcione), que ningún nombre pase de 300
caracteres, y que **no haya dos hermanos con el mismo nombre** — lo que exige `UN_PJRQ_01`.

305 nodos con referencias `PJRQCDPD` cruzadas escritos a mano tienen una probabilidad muy alta de
llevar al menos un padre mal. **La validación mecánica elimina esa clase de error entera**, y es
barata: el generador está en el scratchpad, no en el repositorio, porque es de un solo uso.

---

## 7. ⚠️ Alcance compartido — esto toca territorio de los otros dos equipos

El frente va a modificar **los 9 menús, `app.routes.ts` (archivo único) y los 9 módulos de `saaFE`**.
`omen-saa-1` y `omen-saa-2` estaban commiteando sobre `crd`, `rpr`, `cxc`, `sri`, `tsr`, `cnt` y
`rhh` el mismo 2026-09-10.

`app.routes.ts` en particular es **un solo archivo que los tres equipos van a querer tocar**.

**Pendiente:** el usuario tiene que autorizar el aviso a los otros dos árbitros. Un árbitro no le
escribe a otro por su cuenta en este esquema.
