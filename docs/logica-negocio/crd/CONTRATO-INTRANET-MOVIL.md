# Contrato de intranet: el path `/movil` de SaaBE

> **Qué es esto.** El WAR de borde (`SaaMovilBE`, en el 192.168.2.3, expuesto a internet) no
> consume `/SaaBE/rest/...`. Consume un application path **aparte**, `/SaaBE/movil/...`, que es
> una **lista blanca cerrada** de consultas de un partícipe sobre sí mismo. Este documento es el
> contrato entre los dos: lo que el borde puede pedir y lo que SaaBE le contesta.
>
> **Fecha:** 2026-09-07 · **Estado:** diseño congelado por el árbitro, pendiente de implementación.
> **Decidido por el usuario el 2026-09-07:** path `/movil` y clave compartida obligatoria.
> Equipo: app móvil ASOPREP (`omen-app-1`). Ejecutor en SaaBE: `omen-app-1-saa-be-app`.

## 1. Por qué un path aparte y no `/rest`

`/rest` tiene ~200 recursos, incluidos `DELETE /rest/prst/{id}`, `pagarCuota`, `aprobar`,
`anularOperacion`. **Nada de eso puede quedar a un error de configuración de distancia de
internet.** Con un path propio, la separación deja de depender de que el borde se porte bien y
pasa a ser estructural: lo que no está en la lista blanca no existe en esa URL.

Efecto lateral buscado: los tres endpoints de oficina de `usap` (`crear`, `resetearClave`,
`reactivar`) **se quedan solo en `/rest`**. No se replican en `/movil`. Ya no hace falta
confiar en que el borde no los enrute — desde el path que el borde ve, no existen.

```
                    /SaaBE/rest/...    ~200 recursos, incluye destructivos  <- intranet, NO lo toca el borde
192.168.2.4  SaaBE
                    /SaaBE/movil/...   lista blanca, solo lectura + 3 auth  <- unico que consume el borde
```

## 2. ⛔ Las dos trampas de JAX-RS que hay que resolver ANTES de escribir el primer recurso

Verificado el 2026-09-07: `com.saa.ws.rest.ApplicationConfig` es `@ApplicationPath("/rest")` y
**está vacío** — no sobreescribe `getClasses()`. Una `Application` vacía toma **todos** los
`@Path` y **todos** los `@Provider` del deployment. De ahí salen dos trampas:

**Trampa 1 — la Application nueva no puede quedar vacía.** Si `MovilApplicationConfig` se
escribe igual de vacía, `/SaaBE/movil/...` expondría exactamente los mismos ~200 recursos que
`/rest`, `DELETE` incluidos, y el path nuevo sería un segundo camino a todo en vez de una lista
blanca. **Es el resultado opuesto al que se busca, y compila y arranca sin un solo aviso.**

  → `MovilApplicationConfig` **debe** sobreescribir `getClasses()` devolviendo el conjunto
    explícito de sus recursos y su filtro. Una clase que no esté en ese `Set` no se sirve por
    `/movil`. Esa lista es *la* lista blanca; revisarla es la revisión de seguridad del frente.

**Trampa 2 — el filtro de la clave no puede ser un `@Provider` suelto.** Un
`ContainerRequestFilter` anotado `@Provider` y sin más se auto-descubre en **todas** las
Applications del deployment: le pediría la clave también a `/rest`, y **rompería el sistema
entero de intranet** (el SaaFE dejaría de funcionar).

  → El filtro va con **`@NameBinding`**: una anotación propia (p. ej. `@ClaveMovilRequerida`)
    que se pone en el filtro y en cada recurso de `/movil`. Así actúa **solo** sobre los
    recursos anotados, y — esto importa — sigue actuando aunque el escaneo automático de
    `/rest` también levante esos recursos: la protección viaja con el recurso, no con el path.

## 3. La clave compartida

- Header **`X-Movil-Key`** en cada request que el borde le hace a `/movil`.
- El valor vive como **propiedad de sistema `saa.movil.key`** en los dos WildFly (el .3 y el .4).
  No se commitea, no va en un `.properties` del WAR, no tiene valor por defecto en el código.
- **Falla cerrado, y eso es deliberado:** si la propiedad no está puesta en el .4, o llega vacía,
  o no coincide, el filtro responde **401** y no ejecuta el recurso. Un despliegue sin configurar
  deja el móvil caído — que es lo correcto: la alternativa (arrancar abierto) es la que no se
  puede permitir. Anotarlo en P4, porque es la causa más probable de "desplegué y no anda".
- El 401 del filtro sale **sin cuerpo descriptivo**: `{"mensaje":"No autorizado"}`. No dice si la
  clave falta, es corta o no coincide.
- La comparación se hace en **tiempo constante** (`MessageDigest.isEqual` sobre los bytes), no
  con `String.equals`.
- Esto es **defensa en profundidad, no la defensa principal**: la principal sigue siendo el
  firewall (el .4 solo acepta 8080 desde el .3 y la intranet). Son dos capas, no una en lugar
  de la otra.

## 4. Reglas transversales del path `/movil`

1. **Solo `GET`, y los 3 `POST` de `/auth`.** Ni un `PUT`, ni un `DELETE`, ni un `POST` de
   escritura de negocio. Si un frente futuro necesita escribir, se discute antes; no se agrega.
2. **Nunca se serializa una entidad JPA cruda.** Todo sale como DTO plano del paquete móvil.
   Motivo concreto y ya medido: `GET /rest/prst/porEntidad/{id}` devuelve `Prestamo` con sus
   `@ManyToOne` (`Entidad`, `Producto`, `Filial`, `MotivoPrestamo`) y **arrastra los datos
   personales completos del partícipe dentro de cada préstamo**. Al servidor expuesto no se le
   manda nada que no vaya a usar la app.
3. **Las fechas salen como texto, no como arreglo Jackson.** Los DTO de `/movil` declaran
   `@JsonFormat(shape = STRING, pattern = ...)`: `yyyy-MM-dd` para `LocalDate` y el ISO local
   sin zona para `LocalDateTime`. **Esto cambia la decisión §3.4 del plan**, que ponía la
   conversión en el borde: se hace acá, en el origen, una sola vez y sin que nadie tenga que
   adivinar qué campo era fecha. El borde igual valida a la salida, por si algún campo nuevo se
   escapa sin anotar.
4. **Pertenencia validada también acá.** Todo recurso que reciba un id que no sea el `idEntidad`
   (un `idPrestamo`, por ejemplo) verifica que ese recurso sea de esa entidad y devuelve **404**
   si no lo es — 404, no 403, para no confirmar que el id existe. El borde ya valida
   token↔recurso; esta es la segunda capa, y es la que sigue en pie si el borde tiene un bug.
5. **Una lista vacía es 200, no error.** Un partícipe sin préstamos, sin aportes o sin
   movimientos recibe `[]`. Nada de `IncomeException` en este path.
6. **Errores genéricos hacia afuera.** `{"mensaje": "..."}` sin `e.getMessage()`, sin stack, sin
   nombres de tabla. El detalle va al log del servidor, que se queda en la intranet.
7. **Sin paginación no se sirve una colección que pueda crecer sin techo.** Movimientos y kardex
   llevan rango de fechas o límite. `getAll` no se usa desde acá para nada: `CRD.APRT` tiene
   ~980.000 filas y ya provocó `OutOfMemoryError` (ver el JavaDoc de `AporteRest`).
8. **Cero lógica de negocio nueva.** Este path compone y recorta lo que ya calculan los
   `*Service`/`*DaoService` existentes. Si hace falta una regla nueva, no va acá.

## 5. Los endpoints

Base: `http://192.168.2.4:8080/SaaBE/movil`

### 5.1 Autenticación — delegan en `UsuarioAppService`, sin duplicar una línea de lógica

| Método | Ruta | Body | Respuesta |
|---|---|---|---|
| POST | `/auth/validarCredencial` | `{identificacion, clave}` | `ValidarCredencialResponse` · 401 genérico |
| POST | `/auth/cambiarClave` | `{identificacion, claveActual, claveNueva}` | 200 · **401** clave actual mala · **400** clave nueva débil |
| POST | `/auth/desactivar` | `{identificacion, clave}` | 200 · 401 |

⚠️ **El 400 de `cambiarClave` se propaga tal cual, no se colapsa a 401.** La app trata cualquier
401 como sesión vencida y hace logout; si una clave nueva débil devolviera 401, todo partícipe
recién enrolado (`debeCambiarClave = true`) quedaría en un bucle de logout sin poder activar su
cuenta nunca. Es la razón por la que `ValidacionException` existe. Ver
`API-USUARIO-APP-MOVIL.md`.

⛔ **`crear`, `resetearClave` y `reactivar` NO se replican en `/movil`.** Son de oficina y viven
solo en `/rest/usap`.

### 5.2 Consulta — todos `GET`, todos anclados a un `{idEntidad}`

| Ruta | Se apoya en | Notas |
|---|---|---|
| `/participe/{idEntidad}` | `entd/getId` + `ParticipeDaoService.selectByEntidad(...)` | Perfil consolidado. El DAO ya tiene el `selectByEntidad`; **falta exponerlo**. DTO recortado: identificación, nombres, apellidos, contacto. Nada de columnas internas |
| `/prestamos/{idEntidad}` | `PrestamoDaoService.selectByEntidad(...)` | ⚠️ **El estado vigente es `PRSTIDST` (`idEstado`), NO `ESPSCDGO`** — trampa documentada en `CLAUDE.md`. El filtro por estado y el mapeo contra el rubro real se documentan acá cuando se implementen |
| `/prestamos/{idEntidad}/{idPrestamo}` | `prst` por id | Valida pertenencia → 404 |
| `/prestamos/{idEntidad}/{idPrestamo}/cuotas` | `DetallePrestamoDaoService.selectByPrestamo(...)` | El DAO ya lo tiene; **falta exponerlo**. `Prestamo` **no** trae las cuotas anidadas: no hay `@OneToMany`. Valida pertenencia |
| `/aportes/{idEntidad}/resumen` | `aprt/saldosPorEntidad` | El endpoint de `/rest` envuelve en `{exito, resultado}`; acá se devuelve el contenido plano |
| `/aportes/{idEntidad}/movimientos?desde=&hasta=` | `aprt/estadoCuenta` | ⚠️ **`desde`/`hasta` son obligatorios aguas abajo** (`yyyy-MM`, 400 si faltan). Si el borde no los manda, `/movil` aplica un **rango por defecto de los últimos 24 meses** y lo dice en la respuesta |
| `/cuenta-individual/{idEntidad}` | composición | Saldos de aportes + préstamos con saldo + kardex CXC del partícipe |
| `/simulador/productos` | `prdc` | Solo los vigentes que la app puede simular, recortados |
| `/simulador/simular` (POST) | `prst/simularCreditoNuevo` | No persiste nada. Único POST de datos, porque el cálculo necesita cuerpo |

### 5.3 ⛔ Lo que falta en `saaBE` y hay que construir

Tres de los apoyos de arriba **no existen hoy** (verificado contra el código el 2026-09-07):

- **`prtc` por entidad** y **`dtpr` por préstamo**: el método ya está en el DAO
  (`ParticipeDaoService.selectByEntidad`, `DetallePrestamoDaoService.selectByPrestamo`). Solo
  falta consumirlo desde el recurso móvil. **No se toca `ParticipeRest` ni `DetallePrestamoRest`**
  — son de otros equipos y `saaBE` es árbol compartido.
- **CXC del partícipe**: `CxcParticipeDaoService` y `CxcKardexParticipeDaoService` están
  **vacíos**, sin una sola consulta propia. Hay que agregar ahí las consultas por entidad
  (`CxcParticipe.entidad` es FK a `ENTD`; `CxcKardexParticipe.cxcParticipe` es FK a `CXCP`).
  Se agregan **métodos nuevos** a esas dos interfaces y sus `Impl`, sin tocar sus `Rest` ni sus
  `Service`. Es el único archivo de otro equipo que este frente modifica, y se declara en el commit.

**Lo que NO se hace, y el motivo:** ni `getAll` ni `selectByCriteria` desde el borde. `getAll`
traería el kardex y las cuotas de *todos* los partícipes al servidor expuesto para devolver las
de uno. `selectByCriteria` arma el JPQL leyendo los operadores **desde la base**
(`Rubro`/`DetalleRubro`, ver `CLAUDE.md`): es una dependencia silenciosa de datos de catálogo
para algo que tiene que funcionar siempre, y le da al borde un constructor de consultas genérico
justo donde la regla es lista blanca.

## 6. Cómo se verifica que la lista blanca sigue siendo blanca

Antes de cada despliegue, tres controles — dos se leen y uno se corre:

1. `MovilApplicationConfig.getClasses()` no devuelve `null`, no está vacío, y cada clase de la
   lista es del paquete móvil. Una clase de fuera del paquete es un defecto grave.
2. Cada recurso de esa lista lleva `@ClaveMovilRequerida` y no tiene ningún método
   `@PUT`/`@DELETE`, ni un `@POST` que no sea de `/auth` o el simulador.
3. Con el WAR desplegado: `curl` sin el header → **401**; `curl` a
   `/SaaBE/movil/prst/getAll` (un recurso de `/rest` que NO está en la lista) → **404**. Si eso
   devuelve 200, la Application quedó vacía: es la trampa 1.

## 7. Registro de cambios

| Fecha | Cambio |
|---|---|
| 2026-09-07 | Creación. Path `/movil` y clave compartida decididos por el usuario; las dos trampas de JAX-RS y el cambio de la §3.4 del plan (fechas en el origen) los aporta el árbitro |
