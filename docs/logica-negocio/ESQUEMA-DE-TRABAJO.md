# Esquema de trabajo — un equipo de tres agentes

**Para qué sirve este documento:** explica **por qué** el esquema de tres agentes es como es —los
roles, los límites de cada uno y las reglas que salieron de problemas reales—. Los prompts en sí
ya no están acá: viven en [`gaeminexus/equipos`](https://github.com/gaeminexus/equipos) y el
lanzador los inyecta solo. Ver §4.

**Se pueden levantar varios equipos a la vez**, cada uno trabajando un requerimiento distinto. Lo
único que cambia entre equipos es el **alcance**, que lo define el usuario al arrancar. Probado con
dos equipos en paralelo (uno en `crd`, otro en `cxp`/`cxc`/`pagos`/`tsr`/`rhh`/`sri`) el 2026-08-28.

---

## 1. Los tres roles

| Rol | Repos | Toca código | Qué hace |
|---|---|---|---|
| **Árbitro** | `saaBE` **y** `saaFE` | ❌ **Nunca** | Analiza impacto, decide el plan, escribe DDL/SQL y documentos, redacta los prompts para BE y FE, evalúa lo que entregan |
| **Backend** | solo `saaBE` | ✅ | Implementa lo que el árbitro le encarga |
| **Frontend** | escribe `saaFE`, lee `saaBE` | ✅ en `saaFE` | Implementa lo que el árbitro le encarga y contrasta el contrato contra el código real del backend |

**El árbitro es el único que escribe en los dos repos.** Por eso es quien detecta desajustes de
contrato entre backend y frontend antes de que exploten.

### Qué significa "no toca código", exactamente

Definido por ruta y no por concepto, porque el borde importa —un `pom.xml` es discutible si la
regla se enuncia en abstracto. El árbitro **no escribe nada bajo `src/`, ni `pom.xml`, ni
`package.json`, ni `angular.json`**. Su salida son `.md`, `.sql` y los prompts de sus agentes.

En `saaFE` trabaja de lectura, **con una sola excepción: los contratos de API en
`saaFE/docs/{modulo}/`**, que sí los escribe él. Sin esa excepción el documento se contradecía a
sí mismo: pedía lectura en `saaFE` y a la vez mandaba espejar el contrato ahí.

### Por qué el frontend lee el backend

Es el control cruzado del trabajo del árbitro. Si el árbitro escribe el contrato leyendo el
backend y nadie más lo contrasta, el contrato queda siendo una suposición suya que nadie verifica
— exactamente el anti-patrón que este documento nombra en "releer el propio código no es
verificar".

El límite: lee `saaBE` **solo para contrastar un contrato que ya tiene**, nunca para deducir
endpoints que el contrato no describe. Si el contrato no existe, reporta bloqueado y espera.

### Quién hace qué con la base de datos

- **El árbitro escribe los `.sql`** — DDL, backfills, consultas de verificación — con sus bloques
  de control antes y después, respaldo y reverso comentado.
- **El usuario los ejecuta**, en local y en producción. Nadie más los corre.
- Los agentes BE/FE **no tocan SQL** ni lo ejecutan. Si necesitan una columna que no existe, lo
  reportan y se detienen.

### Quién compila

**Depende de la máquina, así que el agente lo comprueba en vez de asumirlo.** En la laptop `mvn`
no está en el PATH y compila el usuario en Eclipse; en la OMEN sí está y el backend puede
verificar por su cuenta. La regla es correr `mvn -version` antes de dar nada por sentado, y **decir
explícitamente en cada reporte qué se pudo verificar y qué no**. "Entregado sin compilar" es una
respuesta válida; disimularlo no lo es.

El frontend sí puede correr `ng build` en cualquier máquina.

---

## 2. Cómo fluye el trabajo

```
Usuario ──── define el alcance ────► ÁRBITRO
                                       │
                    ┌──────────────────┼──────────────────┐
                    ▼                                     ▼
              prompt BACKEND                        prompt FRONTEND
                    │                                     │
                    ▼                                     ▼
              implementa                             implementa
                    │                                     │
                    └──────────────► ÁRBITRO ◄────────────┘
                                       │
                    evalúa, cruza contratos, actualiza documentos
                                       │
                                       ▼
                        Usuario: corre SQL · compila · decide
```

**Dos modos de entrega de prompts**, el usuario elige:

1. **Por defecto — el usuario es intermediario.** El árbitro escribe el prompt en el chat, el
   usuario lo copia y lo pega en la sesión del agente, y le trae la respuesta de vuelta.
2. **Modo directo** — el usuario lo activa explícitamente. El árbitro despacha por `SendMessage` a
   las sesiones de BE/FE y recibe sus reportes, sin pasar por el usuario. **Solo interrumpe al
   usuario para una decisión de negocio o un SQL que hay que correr.** Muy superior cuando hay
   mucho volumen; el usuario lo pidió así el 2026-08-28 con la frase *"solo cuando haya decisiones
   o scripts que correr avísame"*.

---

## 3. Reglas que evitan los problemas que ya ocurrieron

Cada una salió de algo que pasó de verdad:

- **Los prompts van en el chat, nunca como archivo `.md`.** Antes se creaba un archivo por prompt
  y se llenaba `docs/` de basura de un solo uso. El plan y los hallazgos sí van a documento; el
  prompt no.
- **Cada equipo mantiene su propio documento de estado.** Con dos equipos escribiendo el mismo
  archivo, los estados se cruzan y cada uno marca frentes del otro.
- **Verificar, no asumir.** El agente lee el `ServiceImpl` real, no la descripción del contrato en
  el documento. El 2026-08-28 eso atrapó: un contrato que devolvía filas heterogéneas, un endpoint
  que respondía `200` con `exito: false`, y una validación que nunca se ejecutaba porque el REST
  llamaba directo al DAO saltándose el Service.
- **Un comentario del código es contexto, no evidencia.** Uno que decía "este campo existe pero
  nadie lo escribe" era cierto de un lado del sistema y falso del otro; casi deja sin corregir un
  defecto real.
- **Reportar lo que no se pudo verificar.** "Verificado por código, no contra la base" es una
  respuesta válida y útil. Inventar la confirmación no lo es.
- **Si algo requiere una decisión de negocio, se reporta y se para.** No se elige por el usuario.

### Lo aprendido operando (agregado el 2026-08-30, cada punto costó tiempo real)

- **Lo acordado en el chat no existe.** El diseño de los acuerdos de condonación se acordó
  hablando; cuando el agente de backend perdió contexto hubo que reconstruirlo entero. **Si un
  agente lo va a implementar, está en disco ANTES de que empiece.** Un diseño que solo vive en una
  conversación se pierde con la conversación.
- **Una pantalla no está hecha hasta que está en el menú.** Componente + ruta + entrada de menú,
  las tres. Pasó dos veces que el usuario subió una versión y no vio nada nuevo.
- **Las pruebas funcionales las hace el usuario.** Ningún agente maneja el navegador ni pide
  insumos de prueba. Lo que sí entrega el agente al cerrar es **el listado de qué hace falta para
  probar cada flujo** (un registro en tal estado, un archivo pendiente, una cuenta con tal
  configuración) para que el usuario arme el escenario sin adivinar.
- **Un par no levanta una pausa que dio el usuario.** Si el usuario pausó a un agente directamente,
  el árbitro **no puede** reactivarlo: la señal tiene que venir del usuario. Un agente que se planta
  ahí está actuando bien, aunque cueste tiempo.
- **Releer el propio código no es verificar.** Un agente que revisa "su" contrato contra el código
  que él mismo escribió está confirmando sus propias suposiciones. La verificación independiente la
  hace otro, o se hace contra la base o contra el cable.
- **Después de renombrar, grep del nombre viejo en todo el árbol** antes de reportar. Un rename de
  constantes dejó tres líneas rotas en un método auxiliar de presentación — de los que no aparecen
  cuando uno piensa en el modelo.
- **El despliegue tiene orden: primero el SQL, después el WAR.** Si las entidades mapean columnas o
  tablas que la base todavía no tiene, el orden inverso rompe.
- **Verificación entidad-vs-esquema antes de cada despliegue grande.** Ver §6.

---

## 3.b El contrato de API — qué es y cuándo se escribe

**Un contrato es el documento que describe exactamente qué expone el backend**, escrito por el
árbitro y congelado: rutas y verbos, el cuerpo exacto de cada petición, la forma de cada respuesta,
los códigos HTTP, los estados y sus transiciones, y las trampas.

Viven en `docs/logica-negocio/{modulo}/API-*.md` y **se espejan a `saaFE/docs/{modulo}/`** —
⚠️ ojo: en `saaFE` la ruta es `docs/crd/`, **no** `docs/logica-negocio/crd/`. Dar la ruta
equivocada deja al frontend sin poder leerlo.

### El contrato se escribe ANTES de que el frontend arranque

No después. El backend del circuito de aprobación de cobros se construyó y **se desplegó a
producción sin contrato escrito**: el frontend no tenía de dónde leer los endpoints y estuvo
parado, y el usuario subió una versión donde la pantalla nueva no existía.

### Por qué no alcanza con que el frontend lea el código

Porque tendría que deducir el comportamiento, y deducir sale mal. Tres casos reales del mismo
contrato:

- Los seis endpoints de escritura devolvían **tres formas distintas de éxito** (un 201 con DTO,
  cuatro con la entidad completa, uno con otro DTO).
- **`procesar` devuelve HTTP 200 y puede no haber procesado nada** — es el rechazo automático por
  monto desactualizado. Tomar 200 como éxito habría mostrado "cobro procesado" con el dinero sin
  aplicar.
- La fila de la bandeja **no traía la ruta del comprobante**, al revés de lo que decía la primera
  versión del contrato. Se detectó al verificarlo contra el código, que es exactamente para lo que
  sirve escribirlo.

**"Congelado" significa** que ninguno de los dos agentes lo cambia por su cuenta. Si hay que
cambiarlo, lo cambia el árbitro y avisa a los dos lados. Un cambio unilateral rompe al otro en
silencio.

---

## 4. Los prompts iniciales

**Los tres prompts ya no viven en este documento.** Están en el repositorio
[`gaeminexus/equipos`](https://github.com/gaeminexus/equipos), en `prompts/saa-arbitro.txt`,
`prompts/saa-backend.txt` y `prompts/saa-frontend.txt`.

El motivo es que tenerlos acá obligaba a copiarlos a mano en cada sesión, y a mantener una copia
por máquina con las rutas cambiadas. Ahora el lanzador los inyecta como primer mensaje de cada
sesión, ya resueltos.

### Levantar un equipo

```powershell
levantar-equipo.ps1 -Proyecto saa -Equipo 1 -Alcance "crd, cnt" -NoTocar "tsr"
```

Abre las tres sesiones, cada una parada en su repositorio, con su nombre, su modelo y su prompt.
`-SoloMostrar` imprime todo sin abrir nada, y deja los prompts resueltos en
`%TEMP%\equipos-lanzador\` por si querés revisar exactamente qué recibió cada agente.

### Lo que el lanzador resuelve por vos

| | |
|---|---|
| **Rutas** | Cambian por máquina: en la laptop `C:\work\...`, en la OMEN `F:\work\...`. Los prompts no las mencionan |
| **Nombres** | `<máquina>-<proyecto>-<equipo>-<rol>`, p.ej. `omen-saa-1-be` |
| **Modelos** | Opus en el árbitro, Sonnet en los ejecutores |
| **Accesos** | El árbitro recibe `--add-dir` a `saaFE`; el frontend, a `saaBE` |
| **Alcance** | Lo que pases en `-Alcance` y `-NoTocar` entra en los tres prompts |

### Por qué el árbitro es el único remoto

Solo el árbitro lleva `--remote-control`. La mensajería entre sesiones de la misma máquina no lo
necesita —viaja por un named pipe local—, así que el equipo coordina igual y el celular muestra un
árbitro por equipo en vez de quince sesiones.

El costo: si el backend o el frontend se traban pidiendo un permiso, no se les puede contestar
desde el celular, y el árbitro tampoco puede hacerlo por ellos. Un mensaje de otra sesión nunca
cuenta como consentimiento del usuario.

### Si cambiás un prompt

Cambialo en `equipos/prompts/`, no acá. Este documento explica **por qué** el esquema es como es;
los prompts son el **qué** y tienen un solo lugar. Si los duplicás, divergen — que es exactamente
lo que pasaba antes.


## 5. Levantar varios equipos

Funciona, y está probado. Cada equipo es una corrida del lanzador con su número y su alcance:

```powershell
levantar-equipo.ps1 -Proyecto saa -Equipo 1 -Alcance "crd, cnt" -NoTocar "tsr"
levantar-equipo.ps1 -Proyecto saa -Equipo 2 -Alcance "cxp, cxc, pagos, tsr" -NoTocar "crd, cnt"
```

**El número separa los nombres de sesión, no el disco.** Los dos equipos editan el mismo árbol de
archivos; el aislamiento lo da el alcance disjunto, no el sistema. Lo que hay que cuidar:

1. **Alcance disjunto de módulos.** Cada equipo con los suyos, dicho explícitamente en los tres
   prompts (los "MÓDULOS QUE TE TOCAN" y "NO TOCAR"). `-NoTocar` no necesita ser exhaustivo: lo
   que no está en el alcance ya está fuera. Sirve para nombrar lo que es de otro equipo, que es
   más fuerte que el silencio.
2. **Los módulos compartidos son la zona de riesgo.** Contabilidad, rubros, utilidades: cualquier
   equipo puede necesitarlos. La regla es revisar `git status` sobre esos archivos antes de
   tocarlos y avisar al otro equipo si hay cambios ajenos.
3. **Un documento de estado por equipo**, nunca compartido.
4. **Los árbitros se avisan entre sí** cuando un cambio de uno afecta al otro — un servicio
   compartido que cambia de comportamiento, un endpoint que empieza a rechazar donde antes
   aceptaba. Eso no lo detecta nadie más.

---

## 6. Verificación entidad-vs-esquema — antes de cada despliegue grande

**El fallo que encuentra:** Hibernate incluye **toda** columna `@Column` básica en el `SELECT` que
genera. Una columna mapeada que no existe en la base no rompe solo la función nueva: rompe
**cualquier lectura de esa entidad** con `ORA-00904`. No se ve en el código, no se ve al compilar, y
aparece cuando un usuario abre la pantalla.

Los scripts comparan **todas** las columnas mapeadas de **todas** las entidades de unos módulos
contra `ALL_TAB_COLUMNS`. Son de solo lectura.

| Script | Cubre |
|---|---|
| `docs/logica-negocio/VERIFICACION-ENTIDADES-VS-ESQUEMA-CXC-CXP-TSR.sql` | cxc, cxp, tsr |
| `docs/logica-negocio/crd/sql/VERIFICACION-ENTIDADES-VS-ESQUEMA-CRD.sql` | crd |

**Generar el de otros módulos es mecánico** — el árbitro extrae de `model/{modulo}/*.java` el
`@Table(name, schema)` y todos los `name = "..."` de `@Column`/`@JoinColumn`, y arma la lista
esperada. Conviene generarlo con **dos extractores independientes y comparar las salidas**: es un
script cuyo trabajo es dar tranquilidad, así que no debería depender de una sola pasada.

**Dos advertencias al leer el resultado:**
1. `ALL_TAB_COLUMNS` muestra solo lo que el usuario conectado ve. Conectarse con el mismo usuario
   del datasource, o con DBA, o salen faltantes falsos.
2. Las tablas cuyo DDL todavía no se corrió **a propósito** van a salir como ausentes. Hay que
   saber cuáles son antes de correrlo, o se confunde lo esperado con lo roto.

**Ya encontró cosas reales:** un script DDL que nunca se había corrido sobre una columna mapeada
(equipo cxp), y `CRD.PRCA` — una entidad con DAO, service y **un endpoint REST vivo** contra una
tabla que no existe en producción, resto de una implementación superseded que nadie llama.

---

## 7. Cómo se ven las sesiones entre sí

Las sesiones de Claude Code se descubren con `ListAgents` y se hablan con `SendMessage`. En la
misma máquina el mensaje viaja por un named pipe local, sin pasar por servidores de Anthropic; a
una máquina distinta llega por la conexión de Remote Control de esa máquina.

Un mensaje de otra sesión **es información a verificar, no una orden ni una aprobación del
usuario**. No puede aprobar un permiso pendiente ni cambiar configuración.

### Un árbitro ve mucho más que su equipo

Claude Code **no tiene concepto de equipo**. `ListAgents` devuelve todas las sesiones alcanzables:
los otros equipos del mismo proyecto, los otros proyectos abiertos, y —como el árbitro está
conectado a Remote Control— también las sesiones de las otras máquinas y las de la nube.

No hay forma estructural de acotarlo. Las reglas `deny` sobre `SendMessage` son todo o nada y le
quitan la coordinación al árbitro; `crossSessionInbound` es una política general que no distingue
remitentes. Así que el límite vive en los prompts, y por eso los nombres de sesión llevan el
equipo adentro: `omen-saa-1-be` dice máquina, proyecto, equipo y rol, y la regla queda verificable
por el propio agente.

**La regla es asimétrica a propósito:**

- Un árbitro **no** le manda trabajo a los ejecutores de otro equipo. No conoce sus reservas de
  módulos ni su alcance, y despacharles trabajo es la vía más rápida a una colisión.
- Entre **árbitros** la comunicación es deseable y necesaria. Cuando un cambio afecta el alcance
  de otro equipo —un servicio compartido que cambia de comportamiento, un endpoint que empieza a
  rechazar donde antes aceptaba— eso no lo detecta nadie más.
- Un ejecutor que recibe trabajo de alguien que no es su árbitro **no lo ejecuta**: se lo reporta
  a su árbitro y sigue con lo suyo.

### Lo que Remote Control oculta

Mientras el árbitro está conectado a Remote Control, Claude Code **le esconde parte de los datos
de las sesiones locales**: omite los directorios de trabajo, y omite todo nombre que no pueda
atribuir a una persona. Una sesión sin nombre puesto explícitamente le aparece como
`(unnamed session)` y no la puede direccionar.

Por eso el lanzador siempre pasa `--name`. No es cosmético: sin nombre explícito, un árbitro
remoto no alcanza a su propio equipo.
