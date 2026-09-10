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

### 1.2 🔴 El interruptor puede no leerse de donde se cree

El pedido del usuario fue: *«si en la **descripción** tiene un 0 no se manejan permisos»*.

El único lector del backend es `DetalleRubroDaoServiceImpl:100-104`, y su JPQL devuelve
`t.valorAlfanumerico`, que mapea **`PDTRVLRV`** — no `PDTRDSCR`.

**Si el `0`/`1` está escrito en la descripción, el interruptor no funciona y no da ningún error:**
lee vacío y sigue. Es la familia de fallo de siempre — el lector apuntando a donde nadie escribe.

Lo mide el **CONTROL 9** de `sql/lap1-13`, que trae las dos columnas. **Decisión pendiente** según
el resultado: mover el dato a `PDTRVLRV`, o cambiar qué columna lee el frontend.

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
- **El 1253 lo dio el usuario y hay que confirmarlo**: el CSV exportado llega a 1220, pero es sólo
  `PGSPCDGO = 11`. Si otra jerarquía tiene códigos más altos, arrancar en 1253 pisa filas ajenas.
  Lo mide el **CONTROL 6** de `sql/lap1-13`.

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

### 4.2 El permiso de ingreso quemado en un rubro — trampa dormida

`UsuarioDaoServiceImpl:151`, dentro de `validaUsuarioSucursal`, lee el rubro `MODULO_SISTEMA`
(alterno **15**) / detalle `INGRESO` (alterno **99**) y usa ese valor **como `PJRQCDGO`** para
preguntarle al SP si el usuario tiene permiso de ingreso al sistema.

**Hoy no rompe nada**: el frontend hace login con `validaUsuario` (el simple), no con
`validaUsuarioSucursal` — verificado en `login.component.ts:190` y `usuario.service.ts:90-91`.

**Pero es una trampa dormida.** El día que se active el login por sucursal —que es exactamente el
tipo de cosa que se activa junto con el módulo de seguridades—, si ese código apunta a un nodo que
borramos, **nadie entra al sistema**. Lo mide el CONTROL 9.

---

## 5. Estado del frente

| Paso | Estado |
|---|---|
| Preguntas de arranque respondidas (§2) | ✅ 2026-09-10 |
| `sql/lap1-13` — diagnóstico | ✅ escrito y commiteado (`a9069fea`). **Pendiente de que el usuario lo corra** |
| Barrido de pantallas del frontend (4 ítems) | 🔵 despachado a `lap-saa-1-fe` el 2026-09-10 |
| `lap1-14` — `DELETE` de PCC/CID/ACT y sus permisos | ⛔ bloqueado por los CONTROLES 2, 4/5 y 6 |
| `lap1-15` — `INSERT` del árbol de SAA desde el 1253 | ⛔ bloqueado por lo anterior + el barrido |
| Conexión en los menús (descomentar + códigos reales + interruptor) | ⛔ bloqueado por `lap1-15` |
| Comentar las opciones sin pantalla programada (decisión 7) | ⛔ bloqueado por el ÍTEM 2 del barrido |

---

## 6. ⚠️ Alcance compartido — esto toca territorio de los otros dos equipos

El frente va a modificar **los 9 menús, `app.routes.ts` (archivo único) y los 9 módulos de `saaFE`**.
`omen-saa-1` y `omen-saa-2` estaban commiteando sobre `crd`, `rpr`, `cxc`, `sri`, `tsr`, `cnt` y
`rhh` el mismo 2026-09-10.

`app.routes.ts` en particular es **un solo archivo que los tres equipos van a querer tocar**.

**Pendiente:** el usuario tiene que autorizar el aviso a los otros dos árbitros. Un árbitro no le
escribe a otro por su cuenta en este esquema.
