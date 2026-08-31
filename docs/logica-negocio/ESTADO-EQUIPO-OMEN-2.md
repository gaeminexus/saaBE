# ESTADO — equipo `omen-saa-2`

**Árbitro:** `omen-saa-2-arb` (máquina **omen**) · **Agentes:** `omen-saa-2-be`, `omen-saa-2-fe`
**Creado:** 2026-08-31 · **Este documento lo mantiene SOLO este equipo.**

> **No confundir con los otros tres documentos de estado.** Cada equipo lleva el suyo y ya se
> cruzaron una vez por compartir archivo (ver `ESTADO-GENERAL-TRABAJO-EN-CURSO.md`, dado de baja).
>
> | Equipo | Documento |
> |---|---|
> | CRD · EQUIPO A (`saabe-25`, otra máquina) | `ESTADO-CRD.md` |
> | CRD · EQUIPO B (`omen-saa-1`, esta máquina) | `ESTADO-CRD.md` + `crd/PLAN-CICLO-OTORGAMIENTO.md` |
> | `saabe-bc` (otra máquina) | `ESTADO-CXP-CXC-TSR-RHH-SRI.md` |
> | **este equipo** | **este archivo** |

---

## 0. ⛔ DÓNDE TRABAJA ESTE EQUIPO — leer antes de tocar un archivo

**Checkouts propios, creados el 2026-08-31:**

| Repo | Ruta de ESTE equipo | Ruta del equipo `omen-saa-1` |
|---|---|---|
| `saaBE` | **`F:\work\equipo2\saaBE`** | `F:\work\saaBE\v1\saaBE` |
| `saaFE` | **`F:\work\equipo2\saaFE`** | `F:\work\saaFE\v1\saaFE` |

Los dos clones apuntan directo a `https://github.com/gaeminexus/{saaBE,saaFE}.git` (`fetch` y
`push` verificados el 2026-08-31), o sea que este equipo sincroniza por `origin` como cualquier
otra máquina. **No** por el checkout de al lado.

### Por qué hizo falta, y es un hallazgo, no una preferencia

`omen-saa-1` (árbitro + backend + frontend) trabaja en **esta misma máquina y en el mismo
directorio**. Verificado el 2026-08-31: `git rev-parse --show-toplevel` daba la misma ruta para las
dos sesiones, y el `git status` de este árbitro mostraba trabajo ajeno sin commitear —
`PrestamoServiceImpl.java` (+129), `PrestamoRest.java` (+60), `SolicitudDecisionPrestamo.java`
escrito a las 09:18— más cuatro archivos del frontend.

**El §4 del registro de reservas (dueño por archivo) no protege contra esto.** Ese mecanismo evita
conflictos de *merge*; no evita que un `checkout`, un `stash`, un `clean` o dos agentes con
distinto contexto se pisen en el mismo `src/`. Los equipos A y B no comparten disco, y esa
diferencia es justo la que hacía que la regla les alcanzara a ellos y no acá.

⚠️ **Consecuencia diaria:** el despliegue lo hace el usuario por Eclipse **desde
`F:\work\saaBE\v1\saaBE`**. Lo que este equipo escriba se despliega recién después de
`push` (acá) + `pull` (allá). No alcanza con que compile.

⚠️ **Y al revés:** este clon **no ve el trabajo sin commitear del otro equipo.** Al 2026-08-31
quedaron sin commitear en `saaFE` v1: `prestamo-edit.component.{ts,html,scss}`,
`prestamo.service.ts` y `docs/crd/API-CICLO-OTORGAMIENTO.md`. Antes de tocar cualquiera de esos,
`git fetch` y preguntar.

---

## 1. Alcance — decidido por el usuario el 2026-08-31

**`crd` · `cxp` · `cxc` · `pagos` · `tsr` · `rhh` · `sri`** — alcance completo, confirmado por el
usuario después de que el árbitro le presentara el mapa de choques de abajo.

### El mapa de choques — los siete módulos tienen otro equipo encima

| Módulo | Quién más lo trabaja hoy | Naturaleza del choque |
|---|---|---|
| `crd` (cobros, contabilidad, jubilados) | **EQUIPO A** (`saabe-25`) | frente activo suyo |
| `crd` (otorgamiento, reestructuración, seguros) | **EQUIPO B** (`omen-saa-1`) | frente activo + mismo disco (§0) |
| `pagos` | **EQUIPO A** | es un frente entero suyo (`PLAN-REDISENO-APROBACION-PAGOS.md`) |
| `cxp` `cxc` `tsr` `rhh` `sri` | **`saabe-bc`** | equipo **activo**, confirmado por el usuario el 2026-08-31 |

**Regla operativa de este equipo, derivada de lo anterior:** antes de que un agente toque un
archivo de estos módulos, el árbitro verifica `git log`/`git status` sobre él y, si tiene dueño en
el §4 del registro de reservas, **pide permiso al árbitro dueño**. No se edita y se avisa después.

### NO TOCAR — **lista vacía**, definido por el usuario el 2026-08-31

**«Tienes permiso para todos los módulos.»** No hay módulo vedado para este equipo.

**Modo de trabajo, también del 2026-08-31: «contigo solo voy a hacer consultas».** El árbitro
responde preguntas y analiza; **no despacha trabajo a los agentes** salvo que el usuario lo pida
explícitamente. Por eso la coordinación con los otros tres equipos pasa a segundo plano: sin
escrituras no hay nada que pisar. **Vuelve a ser bloqueante en el momento en que este equipo
escriba una sola línea** — ahí aplican de nuevo el §0 (checkouts separados) y el §4 del registro
de reservas (dueños de archivo).

---

## 2. Reservas de recursos compartidos — PROPUESTAS, todavía sin escribir en el registro

**Nada de esto está en `REGISTRO-RESERVAS-EQUIPOS.md` al momento de escribir este documento**, y
mientras no esté ahí **y pusheado**, no está reservado. Un cambio sin commitear es invisible para
el otro equipo: es la lección del §2b de ese archivo, y aplica también a este equipo.

| Recurso | Propuesta | Estado |
|---|---|---|
| Números de script `crd/sql/` | **200-249** | propuesto por `omen-saa-1-arb`, aceptado por este árbitro, **sin escribir** |
| `SCP.PRBR` | **310-329** | el registro marca `≥310` sin asignar. **Sin escribir** |
| `SCP.PDTR` | **1500-1599** | el registro marca `≥1500` sin asignar. **Sin escribir** |

⛔ **`PRBR 290-309` / `PDTR 1400-1499` NO son de este equipo.** El registro los rotula "equipo
cxp/cxc/tsr/rhh/sri", que es `saabe-bc` — y el usuario confirmó que **sigue activo**. Tomarlos
porque los módulos coinciden sería exactamente el error que ese archivo existe para evitar.

### Hueco encontrado en el esquema de rangos: solo cubre `crd/sql/`

El §2b del registro reparte números **únicamente en `docs/logica-negocio/crd/sql/`**. Los otros
módulos tienen su propia carpeta `sql/` y **ningún acuerdo de rangos**, aunque `saabe-bc` y este
equipo van a escribir en las mismas.

**Y el problema ya se materializó en `rhh/sql/`**, sin que nadie lo registrara: hay **dos series de
numeración paralelas** que se pisan en los mismos números —`01-anticipo-empleado.sql` junto a
`01_DDL_TABLAS_PARAMETRIZACION.sql`, y lo mismo en 02, 03, 04, 05 y 06—. No se sobrescribieron por
casualidad (distinto separador y sufijo), pero **el número dejó de decir en qué orden se ejecuta**,
que es lo único para lo que servía. `tsr/sql/` tiene un `README-ORDEN-PRODUCCION.md` justamente
porque ahí el riesgo se vio venir.

**Acción pendiente del árbitro:** proponerle a `saabe-bc` un reparto por carpeta, o pasar a un
prefijo por equipo en vez de un número global.

---

## 3. Verificaciones hechas por este equipo

| Fecha | Qué | Resultado |
|---|---|---|
| 2026-08-31 | `mvn -v` en omen | **Maven 3.9.8 / JDK 21.0.8** — coincide con `CLAUDE.md`; acá **sí** se puede compilar |
| 2026-08-31 | `mvn clean compile` sobre el árbol completo | **exit 0, limpio.** Cierra el pendiente 🟡3 de `ESTADO-CXP-CXC-TSR-RHH-SRI.md` §4: los ~15 archivos del frente R (anulación con cascada) **sí compilan**, cosa que ese equipo no había podido verificar porque en su máquina no hay Maven |
| 2026-08-31 | Muestra de `ESTADO-CXP-CXC-TSR-RHH-SRI.md` contra el código | **el documento es fiel.** `POST /rest/ats/generar` y `/cuadresri/{103,104}` existen; `movimientosRelacionados` está en los 9 documentos que declara; `anularEnCascada` aparece 82 veces; `GET /aplc/listar` está en `AplicacionPagoCxcRest:294` (el doc dice 293) |
| 2026-08-31 | Carpetas de docs de `saaFE` | Son `docs/{cnt,crd,cxc,cxp,pagos,rrh,transversal,tsr}`. **RRHH es `rrh`, NO `rhh`, y `sri` NO existe.** Espejar un contrato a `docs/rhh/` o `docs/sri/` lo deja donde el frontend no lo busca (regla 6 del árbitro) |

---

## 4. Frentes de este equipo

| Frente | Documento | Estado |
|---|---|---|
| **Corrección de aportes duplicados — cargas Petro desde junio 2025** | `crd/correccion-duplicados/README.md` | 🔵 **diagnóstico.** Script `01` listo para correr; ningún dato corregido |

Sin agentes despachados: el árbitro trabaja en modo consulta. **El código lo coordina el usuario
con el equipo A** (decisión del 2026-08-31), porque tres de los cuatro archivos que el registro le
asigna a ese equipo leen el saldo de aportes que este frente corrige.

---

## 5. Pendientes

### 🔴 Bloqueante para que este equipo despache trabajo
1. **Lista `NO TOCAR`** — sin definir por el usuario.
2. **Arrancar `omen-saa-2-be` y `omen-saa-2-fe` en los checkouts nuevos** (§0). Si arrancan en las
   rutas `v1`, todo lo de §0 se pierde.
3. **Avisar a los tres árbitros** que este equipo entra en sus módulos. `omen-saa-1-arb`: avisado.
   Equipo A y `saabe-bc`: pendiente.

### 🟡 Decidible
4. Escribir las reservas del §2 en el registro y pushearlas (hoy no están reservadas).
5. Proponer el reparto de números de script fuera de `crd/sql/` (§2, hueco encontrado).

### ⚪ Sin prisa
6. Renumerar o documentar el orden real de `rhh/sql/` (§2).
